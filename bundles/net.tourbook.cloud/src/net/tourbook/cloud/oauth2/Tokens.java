/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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

public abstract class Tokens {

   private long   expires_at;
   private long   expires_in;
   private String refresh_token;
   private String access_token;

   public String getAccess_token() {
      return access_token;
   }

   public long getExpires_at() {
      return expires_at;
   }

   public long getExpires_in() {
      return expires_in;
   }

   public String getRefresh_token() {
      return refresh_token;
   }
}
