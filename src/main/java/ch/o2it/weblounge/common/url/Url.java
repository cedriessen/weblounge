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

package ch.o2it.weblounge.common.url;

import java.io.Serializable;

/**
 * A <code>Url</code> is what defines a path to a page, a binary resource or
 * an action in the system.
 */
public interface Url extends Serializable {

  /**
   * Returns the url separator character, like '/' in web urls.
   * 
   * @return the separator character
   */
  char getPathSeparator();

  /**
   * Returns the path for this url.
   * 
   * @return the url path
   */
  String getPath();

  /**
   * Returns <code>true</code> if the url starts with the given path.
   * 
   * @param path
   *          the path
   * @return <code>true</code> if the url starts with <code>path</code>
   */
  boolean startsWith(String path);

  /**
   * Returns <code>true</code> if the url ends with the given path.
   * 
   * @param path
   *          the path
   * @return <code>true</code> if the url ends with <code>path</code>
   */
  boolean endsWith(String path);

  /**
   * Returns <code>true</code> if this url is a prefix of <code>url</code> by
   * means of the implementation of <code>equals</code>. Note that this url is
   * also a prefix if it is identical to <code>url</code>.
   * 
   * @param url
   *          the url
   * @return <code>true</code> if this url is a prefix
   */
  boolean isPrefixOf(Url url);

  /**
   * Returns <code>true</code> if this url is an extension of <code>url</code>
   * by means of the implementation of <code>equals</code>. Note that this url
   * is also an extension if it is identical to <code>url</code>.
   * 
   * @param url
   *          the url
   * @return <code>true</code> if this url is a prefix
   */
  boolean isExtensionOf(Url url);

}