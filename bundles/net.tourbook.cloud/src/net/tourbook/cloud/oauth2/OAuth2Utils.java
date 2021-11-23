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

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;

public class OAuth2Utils {

   public static String computeAccessTokenExpirationDate(final long accessTokenIssueDateTime,
                                                         final long accessTokenExpiresIn) {

      final long expireAt = accessTokenIssueDateTime + accessTokenExpiresIn;

      return (expireAt == 0) ? UI.EMPTY_STRING : TimeTools.getUTCISODateTime(expireAt);
   }

   /**
    * We consider that an access token is valid (non expired) if there are more
    * than 5 mins remaining until the actual expiration
    *
    * @return
    */
   public static boolean isAccessTokenValid(final long tokenExpirationDate) {

      return tokenExpirationDate - System.currentTimeMillis() - 300000 > 0;
   }
}
