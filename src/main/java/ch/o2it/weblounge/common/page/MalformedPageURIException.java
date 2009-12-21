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

package ch.o2it.weblounge.common.page;

/**
 * This exception is thrown if an invalid <code>PageURI</code> is about to
 * be created.
 */
public class MalformedPageURIException extends RuntimeException {

  /** The serial version UID */
  private static final long serialVersionUID = -4385183355893667958L;

  /**
   * Creates an exception stating that <code>path</code> is malformed.
   * 
   * @param path the path to the page
   */
  public MalformedPageURIException(String path) {
    super("The page uri path '" + path + "' is invalid!");
  }

}