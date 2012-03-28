/*
 *  Weblounge: Web Content Management System
 *  Copyright (c) 2003 - 2011 The Weblounge Team
 *  http://entwinemedia.com/weblounge
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

package ch.entwine.weblounge.contentrepository.impl;

import static ch.entwine.weblounge.contentrepository.impl.index.solr.SolrSchema.HEADER_XML;
import static ch.entwine.weblounge.contentrepository.impl.index.solr.SolrSchema.PATH;
import static ch.entwine.weblounge.contentrepository.impl.index.solr.SolrSchema.RESOURCE_ID;
import static ch.entwine.weblounge.contentrepository.impl.index.solr.SolrSchema.VERSION;
import static ch.entwine.weblounge.contentrepository.impl.index.solr.SolrSchema.XML;

import ch.entwine.weblounge.common.content.PreviewGenerator;
import ch.entwine.weblounge.common.content.Resource;
import ch.entwine.weblounge.common.content.ResourceContentReader;
import ch.entwine.weblounge.common.content.ResourceMetadata;
import ch.entwine.weblounge.common.content.ResourceReader;
import ch.entwine.weblounge.common.content.ResourceURI;
import ch.entwine.weblounge.common.content.SearchResultItem;
import ch.entwine.weblounge.common.content.movie.MovieContent;
import ch.entwine.weblounge.common.content.movie.MovieResource;
import ch.entwine.weblounge.common.impl.content.movie.MovieContentReader;
import ch.entwine.weblounge.common.impl.content.movie.MovieResourceImpl;
import ch.entwine.weblounge.common.impl.content.movie.MovieResourceReader;
import ch.entwine.weblounge.common.impl.content.movie.MovieResourceSearchResultItemImpl;
import ch.entwine.weblounge.common.impl.content.movie.MovieResourceURIImpl;
import ch.entwine.weblounge.common.impl.url.WebUrlImpl;
import ch.entwine.weblounge.common.language.Language;
import ch.entwine.weblounge.common.security.User;
import ch.entwine.weblounge.common.site.Site;
import ch.entwine.weblounge.common.url.WebUrl;
import ch.entwine.weblounge.contentrepository.impl.index.solr.MovieInputDocument;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Implementation of a serializer for image resources.
 */
public class MovieResourceSerializer extends AbstractResourceSerializer<MovieContent, MovieResource> {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(MovieResourceSerializer.class);

  /** Alternate uri prefix */
  protected static final String URI_PREFIX = "/weblounge-movies/";

  /** The preview generator */
  protected PreviewGenerator previewGenerator = null;

  /**
   * Creates a new image resource serializer.
   */
  public MovieResourceSerializer() {
    super(MovieResource.TYPE);
    // TODO: Initialize preview generator
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.entwine.weblounge.contentrepository.ResourceSerializer#getMimeType(ch.entwine.weblounge.common.content.ResourceContent)
   */
  public String getMimeType(MovieContent resourceContent) {
    return resourceContent.getMimetype();
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.entwine.weblounge.contentrepository.ResourceSerializer#supports(java.lang.String)
   */
  public boolean supports(String mimeType) {
    if (mimeType == null)
      return false;
    if (mimeType.toLowerCase().startsWith("audio/"))
      return true;
    if (mimeType.toLowerCase().startsWith("video/"))
      return true;
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.entwine.weblounge.contentrepository.ResourceSerializer#newResource(ch.entwine.weblounge.common.site.Site)
   */
  public Resource<MovieContent> newResource(Site site) {
    return new MovieResourceImpl(new MovieResourceURIImpl(site));
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.entwine.weblounge.contentrepository.ResourceSerializer#newResource(ch.entwine.weblounge.common.site.Site,
   *      java.io.InputStream, ch.entwine.weblounge.common.security.User,
   *      ch.entwine.weblounge.common.language.Language)
   */
  public Resource<MovieContent> newResource(Site site, InputStream is,
      User user, Language language) {

    // TODO: Extract av metadata

    MovieResource avResource = new MovieResourceImpl(new MovieResourceURIImpl(site));
    avResource.setCreated(user, new Date());

    return avResource;
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.entwine.weblounge.contentrepository.impl.AbstractResourceSerializer#createNewReader()
   */
  @Override
  protected MovieResourceReader createNewReader()
      throws ParserConfigurationException, SAXException {
    return new MovieResourceReader();
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.entwine.weblounge.contentrepository.ResourceSerializer#toMetadata(ch.entwine.weblounge.common.content.Resource)
   */
  public List<ResourceMetadata<?>> toMetadata(Resource<?> resource) {
    if (resource != null) {
      return new MovieInputDocument((MovieResource) resource).getMetadata();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.entwine.weblounge.contentrepository.ResourceSerializer#toResource(ch.entwine.weblounge.common.site.Site,
   *      java.util.List)
   */
  public Resource<?> toResource(Site site, List<ResourceMetadata<?>> metadata) {
    for (ResourceMetadata<?> metadataItem : metadata) {
      if (XML.equals(metadataItem.getName())) {
        String resourceXml = (String) metadataItem.getValues().get(0);
        try {
          ResourceReader<MovieContent, MovieResource> reader = getReader();
          MovieResource audioVisual = reader.read(IOUtils.toInputStream(resourceXml, "UTF-8"), site);
          return audioVisual;
        } catch (SAXException e) {
          logger.warn("Error parsing audio visual resource from metadata", e);
          return null;
        } catch (IOException e) {
          logger.warn("Error parsing audio visual resource from metadata", e);
          return null;
        } catch (ParserConfigurationException e) {
          logger.warn("Error parsing audio visual resource from metadata", e);
          return null;
        }
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.entwine.weblounge.contentrepository.ResourceSerializer#toSearchResultItem(ch.entwine.weblounge.common.site.Site,
   *      double, List)
   */
  public SearchResultItem toSearchResultItem(Site site, double relevance,
      List<ResourceMetadata<?>> metadata) {

    Map<String, ResourceMetadata<?>> metadataMap = new HashMap<String, ResourceMetadata<?>>();
    for (ResourceMetadata<?> metadataItem : metadata) {
      metadataMap.put(metadataItem.getName(), metadataItem);
    }

    // resource id
    String id = (String) metadataMap.get(RESOURCE_ID).getValues().get(0);

    // resource version
    long version = (Long) metadataMap.get(VERSION).getValues().get(0);

    // path
    String path = null;
    if (metadataMap.get(PATH) != null)
      path = (String) metadataMap.get(PATH).getValues().get(0);
    else {
      path = URI_PREFIX + "/" + id;
    }

    ResourceURI uri = new MovieResourceURIImpl(site, path, id, version);
    WebUrl url = new WebUrlImpl(site, path);

    MovieResourceSearchResultItemImpl result = new MovieResourceSearchResultItemImpl(uri, url, relevance, site, metadata);

    if (metadataMap.get(XML) != null)
      result.setResourceXml((String) metadataMap.get(XML).getValues().get(0));
    if (metadataMap.get(HEADER_XML) != null)
      result.setAudioVisualHeaderXml((String) metadataMap.get(HEADER_XML).getValues().get(0));
    // TODO: Add remaining metadata

    return result;
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.entwine.weblounge.contentrepository.ResourceSerializer#getContentReader()
   */
  public ResourceContentReader<MovieContent> getContentReader()
      throws ParserConfigurationException, SAXException {
    return new MovieContentReader();
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.entwine.weblounge.contentrepository.ResourceSerializer#getPreviewGenerator()
   */
  public PreviewGenerator getPreviewGenerator() {
    return previewGenerator;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Audio visual serializer";
  }

}