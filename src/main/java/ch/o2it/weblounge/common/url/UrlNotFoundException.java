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

/**
 * This exception will be thrown if the requested url could not be found.
 */
public class UrlNotFoundException extends RuntimeException {

  /** The serial version id */
  private static final long serialVersionUID = 1L;

  /** The url */
  private String url = null;

  /**
   * Creates an exception for the given url.
   * 
   * @param url
   *          the url that had been looked up
   */
  public UrlNotFoundException(String url) {
    super("Url " + url + " was not found!");
    this.url = url;
  }

  /**
   * Returns the url that had been looked up.
   * 
   * @return the url
   */
  public String getUrl() {
    return url;
  }

}