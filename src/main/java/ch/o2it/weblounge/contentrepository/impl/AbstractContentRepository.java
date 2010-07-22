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

package ch.o2it.weblounge.contentrepository.impl;

import ch.o2it.weblounge.common.content.Page;
import ch.o2it.weblounge.common.content.PageURI;
import ch.o2it.weblounge.common.content.SearchQuery;
import ch.o2it.weblounge.common.content.SearchResult;
import ch.o2it.weblounge.common.impl.page.PageReader;
import ch.o2it.weblounge.common.impl.page.PageURIImpl;
import ch.o2it.weblounge.common.impl.page.PageUtils;
import ch.o2it.weblounge.common.security.Permission;
import ch.o2it.weblounge.common.site.Site;
import ch.o2it.weblounge.common.user.User;
import ch.o2it.weblounge.contentrepository.ContentRepository;
import ch.o2it.weblounge.contentrepository.ContentRepositoryException;
import ch.o2it.weblounge.contentrepository.impl.index.ContentRepositoryIndex;

import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;

/**
 * Abstract implementation for read-only content repositories.
 */
public abstract class AbstractContentRepository implements ContentRepository {

  /** Index into this repository */
  protected ContentRepositoryIndex index = null;

  /** The site */
  protected Site site = null;

  /** The root uri, which identifies a sub part of the overall repository */
  protected String rootURI = null;

  /** Flag indicating the connected state */
  protected boolean connected = false;

  /** The document builder factory */
  protected final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

  /** The xml transformer factory */
  protected final TransformerFactory transformerFactory = TransformerFactory.newInstance();

  /** Regular expression to match the page id, path and version */
  protected final static Pattern pageHeaderRegex = Pattern.compile(".*id=\"([a-z0-9-]*)\".*path=\"([^\"]*)\".*version=\"([^\"]*)\".*");
  
  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#connect(java.util.Dictionary)
   */
  public void connect(Dictionary<?, ?> properties)
      throws ContentRepositoryException {

    site = (Site) properties.get(Site.class.getName());
    if (site == null)
      throw new IllegalStateException("Cannot connect without site");

    connected = true;
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#disconnect()
   */
  public void disconnect() throws ContentRepositoryException {
    connected = false;
  }

  /**
   * {@inheritDoc}
   * 
   * This default implementation triggers loading of the index, so when
   * overwriting, make sure to invoke by calling <code>super.start()</code>.
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#start()
   */
  public void start() throws ContentRepositoryException {
    try {
      index = loadIndex();
    } catch (IOException e) {
      throw new ContentRepositoryException("Error loading repository index", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * This implementation closes the index, so when overwriting, make sure to
   * invoke by calling <code>super.stop()</code>.
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#stop()
   */
  public void stop() throws ContentRepositoryException {
    try {
      index.close();
    } catch (IOException e) {
      throw new ContentRepositoryException("Error closing repository index", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#exists(ch.o2it.weblounge.common.content.PageURI)
   */
  public boolean exists(PageURI uri) throws ContentRepositoryException {
    if (!connected)
      throw new IllegalStateException("Content repository is not connected");
    try {
      return index.exists(uri);
    } catch (IOException e) {
      throw new ContentRepositoryException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#exists(ch.o2it.weblounge.common.content.PageURI,
   *      ch.o2it.weblounge.common.user.User,
   *      ch.o2it.weblounge.common.security.Permission)
   */
  public boolean exists(PageURI uri, User user, Permission p)
      throws ContentRepositoryException, SecurityException {
    if (!connected)
      throw new IllegalStateException("Content repository is not connected");

    // TODO: Implement security
    try {
      return index.exists(uri);
    } catch (IOException e) {
      throw new ContentRepositoryException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#findPages(ch.o2it.weblounge.common.content.SearchQuery)
   */
  public SearchResult findPages(SearchQuery query)
      throws ContentRepositoryException {
    if (!connected)
      throw new IllegalStateException("Content repository is not connected");

    try {
      return index.findPages(query);
    } catch (IOException e) {
      throw new ContentRepositoryException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#getPage(ch.o2it.weblounge.common.content.PageURI)
   */
  public Page getPage(PageURI uri) throws ContentRepositoryException {
    if (!connected)
      throw new IllegalStateException("Content repository is not connected");
    try {
      return loadPage(uri);
    } catch (IOException e) {
      throw new ContentRepositoryException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#getPage(ch.o2it.weblounge.common.content.PageURI,
   *      ch.o2it.weblounge.common.user.User,
   *      ch.o2it.weblounge.common.security.Permission)
   */
  public Page getPage(PageURI uri, User user, Permission p)
      throws ContentRepositoryException, SecurityException {
    if (!connected)
      throw new IllegalStateException("Content repository is not connected");

    Page page = getPage(uri);

    // TODO: Check permissions

    return page;
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#getVersions(ch.o2it.weblounge.common.content.PageURI)
   */
  public PageURI[] getVersions(PageURI uri) throws ContentRepositoryException {
    if (!connected)
      throw new IllegalStateException("Content repository is not connected");

    try {
      long[] revisions = index.getRevisions(uri);
      PageURI[] uris = new PageURI[revisions.length];
      int i = 0;
      for (long r : revisions) {
        uris[i++] = new PageURIImpl(uri, r);
      }
      return uris;
    } catch (IOException e) {
      throw new ContentRepositoryException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#listPages(ch.o2it.weblounge.common.content.PageURI)
   */
  public Iterator<PageURI> listPages(PageURI uri)
      throws ContentRepositoryException {
    if (!connected)
      throw new IllegalStateException("Content repository is not connected");
    return listPages(uri, Integer.MAX_VALUE, -1);
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#listPages(ch.o2it.weblounge.common.content.PageURI,
   *      long[])
   */
  public Iterator<PageURI> listPages(PageURI uri, long version)
      throws ContentRepositoryException {
    return listPages(uri, Integer.MAX_VALUE, version);
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#listPages(ch.o2it.weblounge.common.content.PageURI,
   *      int)
   */
  public Iterator<PageURI> listPages(PageURI uri, int level)
      throws ContentRepositoryException {
    return listPages(uri, level, -1);
  }

  /**
   * {@inheritDoc}
   * 
   * This implementation uses the index to get the list.
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#listPages(ch.o2it.weblounge.common.content.PageURI,
   *      int, long)
   */
  public Iterator<PageURI> listPages(PageURI uri, int level, long version)
      throws ContentRepositoryException {
    if (!connected)
      throw new IllegalStateException("Content repository is not connected");

    return index.list(uri, level, version);
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#setURI(java.lang.String)
   */
  public void setURI(String repositoryURI) {
    if (connected)
      throw new IllegalStateException("Unable to change the repository uri while still connected");
    if (repositoryURI == null)
      throw new IllegalArgumentException("Repository uri cannot be null");
    if (repositoryURI.startsWith("/"))
      throw new IllegalArgumentException("Repository uri Must be absolute");
    rootURI = repositoryURI;
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#getURI()
   */
  public String getURI() {
    return rootURI;
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#getPages()
   */
  public long getPages() {
    return index != null ? index.getPages() : -1;
  }

  /**
   * {@inheritDoc}
   * 
   * @see ch.o2it.weblounge.contentrepository.ContentRepository#getVersions()
   */
  public long getVersions() {
    return index != null ? index.getVersions() : -1;
  }

  /**
   * Appends the identifier of the form <code>x-y-z-u-v</code> to
   * <code>path</code> as in <code>/&lt;int&gt;/&lt;int&gt;/id</code>, with the
   * "/" being the platform's file separator.
   * 
   * @param id
   *          the identifier
   * @param path
   *          the root path
   * @return the path
   */
  protected StringBuffer appendIdToPath(String id, StringBuffer path) {
    if (id == null)
      throw new IllegalArgumentException("Identifier must not be null");
    path.append(idToDirectory(id));
    return path;
  }

  /**
   * Returns the identifier of the form <code>x-y-z-u-v</code> as a path as in
   * <code>/&lt;int&gt;/&lt;int&gt;/id</code>, with the "/" being the platform's
   * file separator.
   * 
   * @param id
   *          the identifier
   * @return the path
   */
  protected String idToDirectory(String id) {
    if (id == null)
      throw new IllegalArgumentException("Identifier must not be null");
    String[] elements = id.split("-");
    StringBuffer path = new StringBuffer();

    // convert first part of uuid to long and apply modulo 100
    path.append(File.separatorChar);
    path.append(String.valueOf(Long.parseLong(elements[0], 16) % 100));

    // convert second part of uuid to long and apply modulo 10
    path.append(File.separatorChar);
    path.append(String.valueOf(Long.parseLong(elements[1], 16) % 10));

    // append the full uuid as the actual directory
    path.append(File.separatorChar);
    path.append(id);

    return path.toString();
  }

  /**
   * Returns the site that is associated with this repository.
   * 
   * @return the site
   */
  protected Site getSite() {
    return site;
  }

  /**
   * Returns <code>true</code> if the repository is connected.
   * 
   * @return <code>true</code> if the repository is connected
   */
  protected boolean isConnected() {
    return connected;
  }

  /**
   * Loads and returns the page from the repository.
   * 
   * @param uri
   *          the page uri
   * @return the page
   * @throws IOException
   *           if the page could not be loaded
   */
  protected abstract Page loadPage(PageURI uri) throws IOException;

  /**
   * Loads the repository index. Depending on the concrete implementation, the
   * index might be located in the repository itself or at any other storage
   * location. It might even be an in-memory index, in which case the repository
   * implementation is in charge of populating the index.
   * 
   * @return the index
   * @throws IOException
   *           if reading or creating the index fails
   * @throws ContentRepositoryException
   *           if populating the index fails
   */
  protected abstract ContentRepositoryIndex loadIndex() throws IOException,
      ContentRepositoryException;

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    if (site != null)
      return site.hashCode();
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
    if (obj instanceof AbstractContentRepository) {
      AbstractContentRepository repo = (AbstractContentRepository) obj;
      if (site != null) {
        return site.equals(repo.getSite());
      } else {
        return super.equals(obj);
      }
    }
    return false;
  }

  /**
   * Returns the page that is located at the indicated url.
   * 
   * @param url
   *          location of the page file
   * @return the page
   */
  protected Page loadPage(Site site, URL url) throws IOException {
    BufferedInputStream is = new BufferedInputStream(url.openStream());
    try {
      PageReader reader = new PageReader();
      return reader.read(is, new PageURIImpl(site));
    } catch (SAXException e) {
      throw new IOException("Error reading page from " + url);
    } catch (ParserConfigurationException e) {
      throw new IOException("Error parsing page at " + url);
    }
  }

  /**
   * Returns the page uri or <code>null</code> if no page id and/or path could
   * be found on the specified document. This method is intended to serve as a
   * utility method when importing pages.
   * 
   * @param url
   *          location of the page file
   * @return the page uri
   */
  protected PageURI loadPageURI(Site site, URL url) throws IOException {
    BufferedInputStream is = new BufferedInputStream(url.openStream());
    InputStreamReader reader = new InputStreamReader(is);
    CharBuffer buf = CharBuffer.allocate(1024);
    reader.read(buf);
    String s = new String(buf.array());
    s = s.replace('\n', ' ');
    Matcher m = pageHeaderRegex.matcher(s);
    if (m.matches()) {
      long version = PageUtils.getVersion(m.group(3));
      return new PageURIImpl(site, m.group(2), version, m.group(1));
    }
    return null;
  }

}
