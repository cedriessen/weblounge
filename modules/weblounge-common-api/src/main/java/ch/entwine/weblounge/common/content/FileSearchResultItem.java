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
package ch.entwine.weblounge.common.content;

import ch.entwine.weblounge.common.content.file.FileResource;

/**
 * This extended <code>SearchResultResourceItem</code> interface is intended for
 * files that were found in the weblounge search index.
 * 
 * @see ch.entwine.weblounge.common.content.ResourceSearchResultItem
 */
public interface FileSearchResultItem extends ResourceSearchResultItem {
  
  /**
   * Returns the file resource.
   * 
   * @return the file resource
   */
  FileResource getFileResource();

}
