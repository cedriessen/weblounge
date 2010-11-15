/*
 *  Weblounge: Web Content Management System
 *  Copyright (c) 2010 The Weblounge Team
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

package ch.o2it.weblounge.contentrepository.impl.fs;

import ch.o2it.weblounge.common.content.MalformedResourceURIException;
import ch.o2it.weblounge.common.content.Resource;
import ch.o2it.weblounge.common.content.ResourceContent;
import ch.o2it.weblounge.common.content.ResourceReader;
import ch.o2it.weblounge.common.content.ResourceURI;
import ch.o2it.weblounge.common.impl.content.ResourceUtils;
import ch.o2it.weblounge.common.impl.url.UrlUtils;
import ch.o2it.weblounge.common.impl.util.config.ConfigurationUtils;
import ch.o2it.weblounge.common.language.Language;
import ch.o2it.weblounge.contentrepository.ContentRepositoryException;
import ch.o2it.weblounge.contentrepository.ResourceSerializer;
import ch.o2it.weblounge.contentrepository.ResourceSerializerFactory;
import ch.o2it.weblounge.contentrepository.VersionedContentRepositoryIndex;
import ch.o2it.weblounge.contentrepository.impl.AbstractWritableContentRepository;
import ch.o2it.weblounge.contentrepository.impl.ContentRepositoryServiceImpl;
import ch.o2it.weblounge.contentrepository.impl.index.ContentRepositoryIndex;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;

/**
 * Implementation of a content repository that lives on a filesystem.
 */
public class FileSystemContentRepository extends AbstractWritableContentRepository {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(FileSystemContentRepository.class);

  /** Prefix for repository configuration keys */
  private static final String CONF_PREFIX = ContentRepositoryServiceImpl.OPT_PREFIX + ".fs.";

  /** Configuration key for the repository's root directory */
  public static final String OPT_ROOT_DIR = CONF_PREFIX + "root";

  /** Name of the index path element right below the repository root */
  public static final String INDEX_PATH = "index";

  /** The repository root directory */
  protected File repositoryRoot = null;

  /** The root directory for the temporary bundle index */
  protected File idxRootDir = null;

  /** The document builder factory */
  protected final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

  /** The xml transformer factory */
  protected final TransformerFactory transformerFactory = TransformerFactory.newInstance();

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#connect(java.util.Dictionary)
   */
  public void connect(Dictionary<?, ?> properties)
      throws ContentRepositoryException {

    // Call the super implementation
    super.connect(properties);

    // Detect the filesystem root directory
    String fsRootDir = (String) properties.get(OPT_ROOT_DIR);
    if (fsRootDir == null) {
      fsRootDir = UrlUtils.concat(System.getProperty("java.io.tmpdir"), "weblounge", "repository");
    }
    repositoryRoot = new File(fsRootDir, site.getIdentifier());
    logger.debug("Content repository root is located at {}", repositoryRoot);

    // Make sure we can create a temporary index
    idxRootDir = new File(repositoryRoot, INDEX_PATH);
    try {
      FileUtils.forceMkdir(idxRootDir);
    } catch (IOException e) {
      throw new ContentRepositoryException("Unable to create site index at " + idxRootDir, e);
    }

    logger.info("Content repository for site '{}' connected at {}", site, repositoryRoot);
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.WritableContentRepository#index()
   */
  public void index() throws ContentRepositoryException {
    if (!connected)
      throw new IllegalStateException("Repository is not connected");

    // Temporary path for rebuilt site
    boolean success = true;

    try {
      // Clear the current index, which might be null if the site has not been
      // started yet.
      if (index == null)
        index = loadIndex();
      index.clear();

      logger.info("Creating site index '{}'...", site);
      long time = System.currentTimeMillis();
      long resourceCount = 0;

      // Index each and every known resource type
      Set<ResourceSerializer<?, ?>> serializers = ResourceSerializerFactory.getSerializers();
      if (serializers == null) {
        logger.warn("Unable to index {} while no resource serializers are registered", this);
        return;
      }
      for (ResourceSerializer<?, ?> serializer : serializers) {
        long added = index(serializer.getType());
        if (added > 0)
          logger.info("Added {} {}s to index", added, serializer.getType().toLowerCase());
        resourceCount += added;
      }

      if (resourceCount > 0) {
        time = System.currentTimeMillis() - time;
        logger.info("Site index populated in {} ms", ConfigurationUtils.toHumanReadableDuration(time));
        logger.info("{} resources added to index", resourceCount);
      }
    } catch (IOException e) {
      success = false;
      throw new ContentRepositoryException("Error while writing to index", e);
    } catch (MalformedResourceURIException e) {
      success = false;
      throw new ContentRepositoryException("Error while reading resource uri for index", e);
    } finally {
      if (!success) {
        try {
          index.clear();
        } catch (IOException e) {
          logger.error("Error while trying to cleanup after failed indexing operation", e);
        }
      }
    }
  }

  /**
   * This method indexes a certain type of resources and expects the resources
   * to be located in a subdirectory of the site directory named
   * <tt>&lt;resourceType&gt;s<tt>.
   * 
   * @param resourceType
   *          the resource type
   * @return the number of resources that were indexed
   * @throws IOException
   *           if accessing a file fails
   */
  protected long index(String resourceType) throws IOException {
    if (!connected)
      throw new IllegalStateException("Repository is not connected");

    // Temporary path for rebuilt site
    String resourceDirectory = resourceType + "s";
    String homePath = UrlUtils.concat(repositoryRoot.getAbsolutePath(), resourceDirectory);
    File resourcesRootDirectory = new File(homePath);
    FileUtils.forceMkdir(resourcesRootDirectory);
    if (resourcesRootDirectory.list().length == 0) {
      logger.debug("No {}s found to index", resourceType);
      return 0;
    }

    logger.info("Populating site index '{}' with {}s...", site, resourceType);

    ResourceSerializer<?, ?> serializer = ResourceSerializerFactory.getSerializer(resourceType);
    if (serializer == null) {
      logger.warn("Unable to index resources of type '{}': no resource serializer found", resourceType);
      return 0;
    }

    File restructuredResources = new File(repositoryRoot, "." + resourceDirectory);
    long resourceCount = 0;
    long resourceVersionCount = 0;
    boolean restructured = false;

    try {
      Stack<File> uris = new Stack<File>();
      uris.push(resourcesRootDirectory);
      while (!uris.empty()) {
        File dir = uris.pop();
        File[] files = dir.listFiles(new FileFilter() {
          public boolean accept(File path) {
            if (path.getName().startsWith("."))
              return false;
            return path.isDirectory() || path.getName().endsWith(".xml");
          }
        });
        if (files == null || files.length == 0)
          continue;
        boolean foundResource = false;
        for (File f : files) {
          if (f.isDirectory()) {
            uris.push(f);
          } else {
            try {
              Resource<?> resource = null;
              ResourceURI uri = null;
              ResourceReader<?, ?> reader = serializer.getReader();
              InputStream is = null;
              try {
                is = new FileInputStream(f);
                resource = reader.read(is, site);
                if (resource == null) {
                  logger.warn("Unkown error loading {}", f);
                  continue;
                }
                uri = resource.getURI();
              } catch (Exception e) {
                logger.error("Error loading {}: {}", f, e.getMessage());
                continue;
              } finally {
                IOUtils.closeQuietly(is);
              }

              index.add(resource);
              resourceVersionCount++;
              if (!foundResource) {
                String resourceName = uri.toString();
                if (resource.contents().size() > 0)
                  resourceName += resource.contents().iterator().next();
                logger.info("Adding {} {} to site index", resourceType, resourceName);
                resourceCount++;
                foundResource = true;
              }

              // Make sure the resource is in the correct place
              File expectedFile = uriToFile(uri);
              String tempPath = expectedFile.getAbsolutePath().substring(homePath.length());
              FileUtils.copyFile(f, new File(restructuredResources, tempPath));
              if (!f.equals(expectedFile)) {
                restructured = true;
              }
            } catch (Throwable t) {
              logger.error("Error indexing {} {}: {}", new Object[] {
                  resourceType,
                  f,
                  t.getMessage() });
            }
          }
        }
      }

      // Move restructured resources in place
      if (restructured) {
        String oldResourcesDirectory = resourceDirectory + "-old";
        File movedOldResources = new File(repositoryRoot, oldResourcesDirectory);
        if (movedOldResources.exists()) {
          for (int i = 1; i < Integer.MAX_VALUE; i++) {
            movedOldResources = new File(repositoryRoot, oldResourcesDirectory + " " + i);
            if (!movedOldResources.exists())
              break;
          }
        }
        FileUtils.moveDirectory(resourcesRootDirectory, movedOldResources);
        FileUtils.moveDirectory(restructuredResources, resourcesRootDirectory);
      }

      // Log the work
      if (resourceCount > 0) {
        logger.info("{} {}s and {} revisions added to index", new Object[] {
            resourceCount,
            resourceType,
            resourceVersionCount - resourceCount });
      }

    } finally {
      if (restructuredResources.exists()) {
        FileUtils.deleteQuietly(restructuredResources);
      }
    }

    return resourceCount;
  }

  /**
   * Returns the root directory for this repository.
   * <p>
   * The root is either equal to the repository's filesystem root or, in case
   * this repository hosts multiple sites, to the filesystem root + a uri.
   * 
   * @return the repository root directory
   */
  public File getRootDirectory() {
    return repositoryRoot;
  }

  /**
   * Returns the <code>File</code> object that is represented by
   * <code>uri</code> or <code>null</code> if the resource does not exist on the
   * filesystem.
   * 
   * @param uri
   *          the resource uri
   * @return the file
   */
  protected File uriToFile(ResourceURI uri) throws IOException {
    StringBuffer path = new StringBuffer(repositoryRoot.getAbsolutePath());
    if (uri.getType() == null)
      throw new IllegalArgumentException("Resource uri has no type");
    path.append("/").append(uri.getType()).append("s");
    String id = null;
    if (uri.getId() != null) {
      id = uri.getId();
    } else {
      id = index.getIdentifier(uri);
      if (id == null) {
        logger.debug("Uri '{}' is not part of the repository index", uri);
        return null;
      }
    }
    if (uri.getVersion() < 0) {
      logger.warn("Resource {} has no version");
    }

    // Build the path
    path = appendIdToPath(id, path);
    path.append(File.separatorChar);
    path.append(uri.getVersion());
    path.append(File.separatorChar);

    // Add the document name
    path.append(ResourceUtils.getDocument(Resource.LIVE));
    return new File(path.toString());
  }

  /**
   * Returns the <code>File</code> object that is represented by
   * <code>uri</code> and <code>content</code> or <code>null</code> if the
   * resource or the resource content does not exist on the filesystem.
   * 
   * @param uri
   *          the resource uri
   * @param content
   *          the resource content
   * @return the content file
   * @throws IOException
   *           if the file cannot be accessed not exist
   */
  protected File uriToContentFile(ResourceURI uri, ResourceContent content)
      throws IOException {
    File resourceDirectory = uriToDirectory(uri);
    File resourceRevisionDirectory = new File(resourceDirectory, Long.toString(uri.getVersion()));

    // Construct the filename
    String fileName = content.getLanguage().getIdentifier();
    String fileExtension = FilenameUtils.getExtension(content.getFilename());
    if (!"".equals(fileExtension)) {
      fileName += "." + fileExtension;
    }
    File contentFile = new File(resourceRevisionDirectory, fileName);
    return contentFile;
  }

  /**
   * Returns the resource uri's parent directory or <code>null</code> if the
   * directory does not exist on the filesystem.
   * 
   * @param uri
   *          the resource uri
   * @return the parent directory
   */
  protected File uriToDirectory(ResourceURI uri) throws IOException {
    StringBuffer path = new StringBuffer(repositoryRoot.getAbsolutePath());
    if (uri.getType() == null)
      throw new IllegalArgumentException("Resource uri has no type");
    path.append("/").append(uri.getType()).append("s");
    String id = null;
    if (uri.getId() != null) {
      id = uri.getId();
    } else {
      id = index.getIdentifier(uri);
      if (id == null) {
        logger.warn("Uri '{}' is not part of the repository index", uri);
        return null;
      }
    }
    path = appendIdToPath(uri.getId(), path);
    return new File(path.toString());
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    if (repositoryRoot != null)
      return repositoryRoot.hashCode();
    else
      return super.hashCode();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof FileSystemContentRepository) {
      FileSystemContentRepository repo = (FileSystemContentRepository) obj;
      if (repositoryRoot != null) {
        return repositoryRoot.equals(repo.getRootDirectory());
      } else {
        return super.equals(obj);
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "filesystem content repository " + repositoryRoot;
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.impl.AbstractContentRepository#loadResource()
   */
  @Override
  protected InputStream openStreamToResource(ResourceURI uri)
      throws IOException {
    if (uri.getType() == null) {
      uri.setType(index.getType(uri));
    }
    File resourceFile = uriToFile(uri);
    if (resourceFile == null || !resourceFile.isFile())
      return null;
    return new FileInputStream(resourceFile);
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.impl.AbstractContentRepository#openStreamToResourceContent(ch.o2it.weblounge.common.content.ResourceURI,
   *      ch.o2it.weblounge.common.language.Language)
   */
  @Override
  protected InputStream openStreamToResourceContent(ResourceURI uri,
      Language language) throws IOException {
    File resourceFile = uriToFile(uri);
    if (resourceFile == null)
      return null;

    // Look for the localized file
    File resourceDirectory = resourceFile.getParentFile();
    final String filenamePrefix = language.getIdentifier() + ".";
    File[] localizedFiles = resourceDirectory.listFiles(new FileFilter() {
      public boolean accept(File f) {
        return f.isFile() && f.getName().startsWith(filenamePrefix);
      }
    });

    // Make sure everything looks consistent
    if (localizedFiles.length == 0)
      return null;
    else if (localizedFiles.length > 1)
      logger.warn("Inconsistencies found in resource {} content {}", language, uri);

    // Finally return the content
    return new FileInputStream(localizedFiles[0]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.impl.AbstractWritableContentRepository#deleteResource(ch.o2it.weblounge.common.content.ResourceURI,
   *      long[])
   */
  @Override
  protected void deleteResource(ResourceURI uri, long[] revisions)
      throws IOException {

    // Remove the resources
    File resourceDir = uriToDirectory(uri);
    for (long r : revisions) {
      File f = new File(resourceDir, Long.toString(r));
      if (f.exists()) {
        try {
          FileUtils.deleteDirectory(f);
        } catch (IOException e) {
          throw new IOException("Unable to delete revision " + r + " of resource " + uri + " located at " + f + " from repository");
        }
      }
    }

    // Remove the resource directory itself if there are no more resources
    try {
      File f = resourceDir;
      while (!uri.getType().equals(f.getName()) && f.listFiles().length == 0) {
        FileUtils.deleteDirectory(f);
        f = f.getParentFile();
      }
    } catch (IOException e) {
      throw new IOException("Unable to delete directory for resource " + uri, e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.impl.AbstractWritableContentRepository#storeResource(ch.o2it.weblounge.common.content.resource.Resource)
   */
  @Override
  protected <T extends ResourceContent, R extends Resource<T>> R storeResource(
      R resource) throws IOException {
    File resourceUrl = uriToFile(resource.getURI());
    InputStream is = null;
    OutputStream os = null;
    try {
      FileUtils.forceMkdir(resourceUrl.getParentFile());
      if (!resourceUrl.exists())
        resourceUrl.createNewFile();
      is = new ByteArrayInputStream(resource.toXml().getBytes("UTF-8"));
      os = new FileOutputStream(resourceUrl);
      IOUtils.copy(is, os);
    } finally {
      IOUtils.closeQuietly(is);
      IOUtils.closeQuietly(os);
    }
    return resource;
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.impl.AbstractWritableContentRepository#storeResourceContent(ch.o2it.weblounge.common.content.ResourceURI,
   *      ch.o2it.weblounge.common.content.ResourceContent, java.io.InputStream)
   */
  @Override
  protected <T extends ResourceContent> T storeResourceContent(ResourceURI uri,
      T content, InputStream is) throws IOException {

    File contentFile = uriToContentFile(uri, content);
    OutputStream os = null;
    try {
      FileUtils.forceMkdir(contentFile.getParentFile());
      if (!contentFile.exists())
        contentFile.createNewFile();
      os = new FileOutputStream(contentFile);
      IOUtils.copyLarge(is, os);
    } finally {
      IOUtils.closeQuietly(is);
      IOUtils.closeQuietly(os);
    }

    // Set the size
    content.setSize(contentFile.length());

    return content;
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.impl.AbstractWritableContentRepository#deleteResourceContent(ch.o2it.weblounge.common.content.ResourceURI,
   *      ch.o2it.weblounge.common.content.ResourceContent)
   */
  protected <T extends ResourceContent> void deleteResourceContent(
      ResourceURI uri, T content) throws IOException {
    File contentFile = uriToContentFile(uri, content);
    if (contentFile == null)
      throw new IOException("Resource content " + contentFile + " does not exist");
    FileUtils.deleteQuietly(contentFile);
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.impl.AbstractContentRepository#loadIndex()
   */
  @Override
  protected ContentRepositoryIndex loadIndex() throws IOException,
      ContentRepositoryException {

    logger.debug("Trying to load site index from {}", idxRootDir);

    // Is this a new index?
    boolean created = !idxRootDir.exists() || idxRootDir.list().length == 0;
    FileUtils.forceMkdir(idxRootDir);

    // Add content if there is any
    index = new FileSystemContentRepositoryIndex(idxRootDir);

    // Create the index if there is nothing in place so far
    if (index.getResourceCount() <= 0) {
      index();
    }

    // Make sure the version matches the implementation
    else if (index.getIndexVersion() != VersionedContentRepositoryIndex.INDEX_VERSION) {
      logger.warn("Index version does not match implementation, triggering reindex");
      index();
    }

    // Is there an existing index?
    if (created) {
      logger.info("Created site index at {}", idxRootDir);
    } else {
      long resourceCount = index.getResourceCount();
      long resourceVersionCount = index.getRevisionCount();
      logger.info("Loaded site index from {}", idxRootDir);
      logger.info("Index contains {} resources and {} revisions", resourceCount, resourceVersionCount - resourceCount);
    }

    return index;
  }

}
