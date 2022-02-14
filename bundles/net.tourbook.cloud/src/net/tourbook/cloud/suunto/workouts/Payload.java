/*******************************************************************************
 * Copyright (C) 2021, 2022 Frédéric Bard
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
package net.tourbook.cloud.suunto.workouts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.byteholder.geoclipse.map.UI;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Payload {

   public int    activityId;
   public long   startTime;
   public int    timeOffsetInMinutes;
   public String workoutKey;

   //TODO fb add the necessary properties here

   /**
    * Gets a sport name from an activity Id.
    * The mapping is available here (cf. column "FIT file"):
    * https://apimgmtstfbqznm5nc6zmvgx.blob.core.windows.net/content/MediaLibrary/docs/Suunto%20Watches-%20SuuntoApp%20-Movescount-FIT-Activities.pdf
    *
    * @return
    */
   public String getSportNameFromActivityId() {

      switch (activityId) {
      case 0:
         return "GENERIC";
      case 1:
         return "RUNNING";
      case 2:
         return "CYCLING";
      }

      return UI.EMPTY_STRING;
   }
}
