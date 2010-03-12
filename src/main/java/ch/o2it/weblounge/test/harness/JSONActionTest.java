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

package ch.o2it.weblounge.test.harness;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test to test JSON action output.
 */
public class JSONActionTest extends IntegrationTestBase {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JSONActionTest.class);

  /**
   * Creates a new instance of the json action test.
   */
  public JSONActionTest() {
    super("JSON Action Test");
  }

  /**
   * {@inheritDoc}
   *
   * @see ch.o2it.weblounge.test.harness.IntegrationTest#execute()
   */
  public void execute() throws Exception {
    logger.info("Preparing test of greeter action's json output");
  }
  
}
