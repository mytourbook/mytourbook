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
package net.tourbook.cloud.strava;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ActivityUpload {

   private String id;
   private String error;
   private String name;
   private String status;

   private String tourDate;

   ActivityUpload() {}

   public String getError() {
      return error;
   }

   public String getId() {
      return id;
   }

   public String getName() {
      return name;
   }

   public String getStatus() {
      return status;
   }

   public String getTourDate() {
      return tourDate;
   }

   public void setError(final String error) {
      this.error = error;
   }

   public void setTourDate(final String tourDate) {
      this.tourDate = tourDate;
   }
}
