/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.cloud.oauth2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class OAuth2UtilsTests {

   @Test
   public void testIsAccessTokenValid() {

      // Token expiring date: Friday, November 22, 2999 11:26:27 PM
      assertTrue(OAuth2Utils.isAccessTokenValid(32500308387000L));
      // Token expiring date: Wednesday, November 22, 2000 11:26:27 PM
      assertFalse(OAuth2Utils.isAccessTokenValid(974935587000L));

      // Token expiring date: Current time + 5min & 1ms
      assertTrue(OAuth2Utils.isAccessTokenValid(System.currentTimeMillis() + 300001));
   }
}
