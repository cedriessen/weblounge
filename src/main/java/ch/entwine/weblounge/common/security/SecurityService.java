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

package ch.entwine.weblounge.common.security;

import ch.entwine.weblounge.common.site.Site;

/**
 * Provides access to the current user's username and roles, if any.
 */
public interface SecurityService {

  /** Login of the generic anonymous user */
  String ANONYMOUS_USER = "anonymous";

  /** Name of the generic anonymous user */
  String ANONYMOUS_NAME = "Anonymous";

  /** Login of the generic admin user */
  String ADMIN_USER = "admin";

  /** Name of the generic admin user */
  String ADMIN_NAME = "Weblounge System Administrator";

  /**
   * Gets the current user, or the local organization's anonymous user if the
   * user has not been authenticated.
   * 
   * @return the user
   */
  User getUser();

  /**
   * Gets the site associated with the current thread context.
   * 
   * @return the site
   */
  Site getSite();

  /**
   * Sets the site for the calling thread.
   * 
   * @param site
   *          the site
   */
  void setSite(Site organization);

  /**
   * Sets the current thread's user context to another user. This is useful when
   * spawning new threads that must contain the parent thread's user context.
   * 
   * @param user
   *          the user to set for the current user context
   */
  void setUser(User user);

  /**
   * Returns <code>true</code> if there is a security policy in place,
   * <code>false</code> otherwise.
   * 
   * @return <code>true</code> if security is enabled
   */
  boolean isEnabled();

}
