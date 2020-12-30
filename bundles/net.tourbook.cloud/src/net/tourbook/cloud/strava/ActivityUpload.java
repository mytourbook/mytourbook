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
package net.tourbook.cloud.strava;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ActivityUpload {
   private String id;
   private String error;
   private String status;
   private String activityId;

   private String tourDate;

   ActivityUpload() {}

   @JsonProperty("activity_id")
   public String getActivityId() {
      return activityId;
   }

   public String getError() {
      return error;
   }

   @JsonProperty("id_str")
   public String getId() {
      return id;
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
