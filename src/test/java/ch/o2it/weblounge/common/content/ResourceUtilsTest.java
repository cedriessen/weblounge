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

package ch.o2it.weblounge.common.content;

import ch.o2it.weblounge.common.impl.content.ResourceUtils;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Test cases for class {@link ResourceUtilsTest}.
 */
public class ResourceUtilsTest {
  
  /** The file size */
  protected double fileSize = 1265389524L;

  /**
   * Test method for {@link ch.o2it.weblounge.common.impl.content.ResourceUtils#formatFileSize(long)}.
   */
  @Test
  public void testFormatFileSizeLong() {
    // Test special values
    assertEquals("0B", ResourceUtils.formatFileSize(0));
    assertEquals("1B", ResourceUtils.formatFileSize(1));
    assertEquals("1KB", ResourceUtils.formatFileSize(1024));
    
    assertEquals("1.2GB", ResourceUtils.formatFileSize((long)fileSize));
    fileSize /= 1024.0d;
    assertEquals("1.2MB", ResourceUtils.formatFileSize((long)fileSize));
    fileSize /= 1024.0d;
    assertEquals("1.2KB", ResourceUtils.formatFileSize((long)fileSize));
    fileSize /= 1024.0d;
    assertEquals("1B", ResourceUtils.formatFileSize((long)fileSize));
  }

}
