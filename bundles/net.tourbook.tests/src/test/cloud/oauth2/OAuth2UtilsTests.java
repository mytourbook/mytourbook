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
package cloud.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.tourbook.cloud.oauth2.OAuth2Utils;

import org.junit.jupiter.api.Test;

public class OAuth2UtilsTests {

   @Test
   public void testComputeAccessTokenExpirationDate() {

      assertEquals(
            OAuth2Utils.computeAccessTokenExpirationDate(974935587000L, 0),
            "2000-11-22T23:26:27Z[UTC]");

      assertEquals(
            OAuth2Utils.computeAccessTokenExpirationDate(974935587000L, 300000),
            "2000-11-22T23:31:27Z[UTC]");

      assertEquals(OAuth2Utils.computeAccessTokenExpirationDate(0, 0), "");
   }
}
