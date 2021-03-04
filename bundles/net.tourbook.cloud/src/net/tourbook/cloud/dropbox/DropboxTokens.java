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
package net.tourbook.cloud.dropbox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.tourbook.cloud.oauth2.Tokens;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DropboxTokens extends Tokens {

   private String uid;
   private String token_type;
   private String account_id;

   public String getAccount_id() {
      return account_id;
   }

   public String getToken_type() {
      return token_type;
   }

   public String getUid() {
      return uid;
   }

}
