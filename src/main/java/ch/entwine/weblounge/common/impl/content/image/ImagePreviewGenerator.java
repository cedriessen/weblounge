/*
 *  Weblounge: Web Content Management System
 *  Copyright (c) 2011 The Weblounge Team
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

package ch.entwine.weblounge.common.impl.content.image;

import ch.entwine.weblounge.common.content.PreviewGenerator;
import ch.entwine.weblounge.common.content.image.ImageStyle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A <code>PreviewGenerator</code> that will generate previews for image
 * resources.
 */
public class ImagePreviewGenerator implements PreviewGenerator {

  /**
   * {@inheritDoc}
   * 
   * @see ch.entwine.weblounge.common.content.PreviewGenerator#createPreview(java.io.InputStream,
   *      ch.entwine.weblounge.common.content.image.ImageStyle,
   *      java.lang.String, java.io.OutputStream)
   */
  public void createPreview(InputStream content, ImageStyle style,
      String format, OutputStream os) throws IOException {
    ImageStyleUtils.style(content, os, format, style);
  }

}
