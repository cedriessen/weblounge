/*
 *  Weblounge: Web Content Management System
 *  Copyright (c) 2009 The Weblounge Team
 *  http://weblounge.o2it.ch
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software Foundation
 *  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package ch.o2it.weblounge.dispatcher.impl.handler;

import ch.o2it.weblounge.common.content.ResourceURI;
import ch.o2it.weblounge.common.content.file.FileContent;
import ch.o2it.weblounge.common.content.file.FileResource;
import ch.o2it.weblounge.common.impl.content.ResourceUtils;
import ch.o2it.weblounge.common.impl.content.file.FileResourceURIImpl;
import ch.o2it.weblounge.common.impl.language.LanguageUtils;
import ch.o2it.weblounge.common.language.Language;
import ch.o2it.weblounge.common.request.WebloungeRequest;
import ch.o2it.weblounge.common.request.WebloungeResponse;
import ch.o2it.weblounge.common.site.Site;
import ch.o2it.weblounge.common.url.WebUrl;
import ch.o2it.weblounge.common.user.User;
import ch.o2it.weblounge.contentrepository.ContentRepository;
import ch.o2it.weblounge.contentrepository.ContentRepositoryException;
import ch.o2it.weblounge.contentrepository.ContentRepositoryFactory;
import ch.o2it.weblounge.dispatcher.RequestHandler;
import ch.o2it.weblounge.dispatcher.impl.DispatchUtils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;

/**
 * This request handler is used to handle requests to files in the repository.
 */
public final class FileRequestHandlerImpl implements RequestHandler {

  /** Alternate uri prefix */
  protected static final String ALT_URI_PREFIX = "/files";

  /** Logging facility */
  protected static final Logger logger = LoggerFactory.getLogger(FileRequestHandlerImpl.class);

  /**
   * Handles the request for a file resource that is believed to be in the
   * content repository. The handler sets the response headers and the writes
   * the file contents to the response.
   * <p>
   * This method returns <code>true</code> if the handler is decided to handle
   * the request, <code>false</code> otherwise.
   * 
   * @param request
   *          the weblounge request
   * @param response
   *          the weblounge response
   */
  public boolean service(WebloungeRequest request, WebloungeResponse response) {

    WebUrl url = request.getUrl();
    Site site = request.getSite();
    String path = url.getPath();

    // Check the request method. Only GET is supported right now.
    String requestMethod = request.getMethod();
    if (!"GET".equals(requestMethod)) {
      logger.debug("File request handler does not support {} requests", requestMethod);
      return false;
    }

    // Get hold of the content repository
    ContentRepository contentRepository = ContentRepositoryFactory.getRepository(site);
    if (contentRepository == null) {
      logger.warn("No content repository found for site '{}'", site);
      return false;
    }

    // Check if the request uri matches the special uri for files. If so, try
    // to extract the id from the last part of the path. If not, check if there
    // is a file with the current path.
    ResourceURI fileURI = null;
    FileResource fileResource = null;
    try {
      if (path.startsWith(ALT_URI_PREFIX)) {
        String id = FilenameUtils.getBaseName(StringUtils.chomp(path, "/"));
        fileURI = new FileResourceURIImpl(site, null, id);
      } else {
        fileURI = new FileResourceURIImpl(site, path);
      }
      fileResource = (FileResource) contentRepository.get(fileURI);
      if (fileResource == null) {
        logger.debug("No file found at {}", fileURI);
        return false;
      }
    } catch (ContentRepositoryException e) {
      logger.error("Error loading file from {}: {}", contentRepository, e);
      DispatchUtils.sendInternalError(request, response);
      return true;
    }

    // Try to serve the file
    logger.debug("File handler agrees to handle {}", path);

    // Is it published?
    if (!fileResource.isPublished()) {
      logger.debug("Access to unpublished file {}", fileURI);
      DispatchUtils.sendNotFound(request, response);
      return true;
    }

    // Can the page be accessed by the current user?
    User user = request.getUser();
    try {
      // TODO: Check permission
      // PagePermission p = new PagePermission(page, user);
      // AccessController.checkPermission(p);
    } catch (SecurityException e) {
      logger.warn("Access to file {} denied for user {}", fileURI, user);
      DispatchUtils.sendAccessDenied(request, response);
      return true;
    }

    // Determine the response language
    Language language = LanguageUtils.getPreferredLanguage(fileResource, request, site);
    if (language == null) {
      logger.warn("File {} does not exist in any supported language", fileURI);
      DispatchUtils.sendNotFound(request, response);
      return true;
    } else {
      fileResource.switchTo(request.getLanguage());
    }

    // Check the ETag value
    if (!ResourceUtils.isMismatch(fileResource, language, request)) {
      logger.debug("File {} was not modified", fileURI);
      DispatchUtils.sendNotModified(request, response);
      return true;
    }

    // Check the modified headers
    if (!ResourceUtils.isModified(fileResource, request)) {
      logger.debug("File {} was not modified", fileURI);
      DispatchUtils.sendNotModified(request, response);
      return true;
    }

    // Add mime type header
    FileContent content = fileResource.getContent(language);
    String contentType = content.getMimetype();
    if (contentType == null)
      contentType = MediaType.APPLICATION_OCTET_STREAM;
    response.setContentType(contentType);

    // Add last modified header
    response.setDateHeader("Last-Modified", fileResource.getModificationDate().getTime());

    // Add content size
    response.setHeader("Content-Length", Long.toString(content.getSize()));

    // Add ETag header
    String eTag = ResourceUtils.getETagValue(fileResource, language);
    response.setHeader("ETag", "\"" + eTag + "\"");

    // Add content disposition header
    response.setHeader("Content-Disposition", "inline; filename=" + content.getFilename());

    // Write the file back to the response
    InputStream fileContents = null;
    try {
      fileContents = contentRepository.getContent(fileURI, language);
      IOUtils.copy(fileContents, response.getOutputStream());
      response.getOutputStream().flush();
      return true;
    } catch (ContentRepositoryException e) {
      logger.error("Unable to load file {}: {}", new Object[] {
          fileURI,
          e.getMessage(),
          e });
      DispatchUtils.sendInternalError(request, response);
      return true;
    } catch (IOException e) {
      logger.error("Error sending file {} to the client: {}", fileURI, e.getMessage());
      DispatchUtils.sendInternalError(request, response);
      return true;
    } finally {
      IOUtils.closeQuietly(fileContents);
    }
  }

  /**
   * @see ch.o2it.weblounge.dispatcher.api.request.RequestHandler#getName()
   */
  public String getName() {
    return "file request handler";
  }

  /**
   * Returns a string representation of this request handler.
   * 
   * @return the handler name
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getName();
  }

}