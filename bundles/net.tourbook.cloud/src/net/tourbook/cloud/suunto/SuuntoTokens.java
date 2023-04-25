/*******************************************************************************
 * Copyright (C) 2021, 2023 Frédéric Bard
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
package net.tourbook.cloud.suunto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.tourbook.cloud.oauth2.Tokens;

@JsonIgnoreProperties(ignoreUnknown = true)
class SuuntoTokens extends Tokens {

   private String token_type;

   private String scope;

   private String ukv;

   private String uk;

   private String user;

   private String jti;

   public String getJti() {
      return jti;
   }

   public String getScope() {
      return scope;
   }

   public String getToken_type() {
      return token_type;
   }

   public String getUk() {
      return uk;
   }

   public String getUkv() {
      return ukv;
   }

   public String getUser() {
      return user;
   }

}
