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

   /**
    * Gets a sport name from an activity Id.
    * The mapping is available here (cf. column "FIT file"):
    * https://apimgmtstfbqznm5nc6zmvgx.blob.core.windows.net/content/MediaLibrary/docs/Suunto%20Watches-%20SuuntoApp%20-Movescount-FIT-Activities.pdf
    *
    * @return
    */
   public String getSportNameFromActivityId() {

      String sportName;

      switch (activityId) {

      case 0:
         sportName = "GENERIC";//$NON-NLS-1$
         break;
      case 1:
         sportName = "RUNNING";//$NON-NLS-1$
         break;
      case 2:
         sportName = "CYCLING";//$NON-NLS-1$
         break;
      case 18:
         sportName = "MULTISPORT";//$NON-NLS-1$
         break;
      case 6:
         sportName = "BASKETBALL";//$NON-NLS-1$
         break;
      case 47:
         sportName = "BOXING";//$NON-NLS-1$
         break;
      case 10:
         sportName = "TRAINING";//$NON-NLS-1$
         break;
      case 48:
         sportName = "FLOOR_CLIMBING";//$NON-NLS-1$
         break;
      case 29:
         sportName = "FISHING";//$NON-NLS-1$
         break;
      case 9:
         sportName = "AMERICAN_FOOTBALL";//$NON-NLS-1$
         break;
      case 25:
         sportName = "GOLF";//$NON-NLS-1$
         break;
      case 27:
         sportName = "HORSEBACK_RIDING";//$NON-NLS-1$
         break;
      case 28:
         sportName = "HUNTING";//$NON-NLS-1$
         break;
      case 33:
         sportName = "ICE_SKATING";//$NON-NLS-1$
         break;
      case 4:
         sportName = "FITNESS_EQUIPMENT";//$NON-NLS-1$
         break;
      case 44:
         sportName = "KITESURFING";//$NON-NLS-1$
         break;
      case 24:
         sportName = "DRIVING";//$NON-NLS-1$
         break;
      case 16:
         sportName = "MOUNTAINEERING";//$NON-NLS-1$
         break;
      case 26:
         sportName = "HANG_GLIDING";//$NON-NLS-1$
         break;
      case 15:
         sportName = "ROWING";//$NON-NLS-1$
         break;
      case 32:
         sportName = "SAILING";//$NON-NLS-1$
         break;
      case 12:
         sportName = "CROSS_COUNTRY_SKIING";//$NON-NLS-1$
         break;
      case 5:
         sportName = "SWIMMING";//$NON-NLS-1$
         break;
      case 35:
         sportName = "SNOWSHOEING";//$NON-NLS-1$
         break;
      case 14:
         sportName = "SNOWBOARDING";//$NON-NLS-1$
         break;
      case 7:
         sportName = "SOCCER";//$NON-NLS-1$
         break;
      case 37:
         sportName = "STAND_UP_PADDLEBOARDING";//$NON-NLS-1$
         break;
      case 38:
         sportName = "SURFING";//$NON-NLS-1$
         break;
      case 13:
         sportName = "ALPINE_SKIING";//$NON-NLS-1$
         break;
      case 8:
         sportName = "TENNIS";//$NON-NLS-1$
         break;
      case 3:
         sportName = "TRANSITION";//$NON-NLS-1$
         break;
      case 17:
         sportName = "HIKING";//$NON-NLS-1$
         break;
      case 11:
         sportName = "WALKING";//$NON-NLS-1$
         break;
      case 43:
         sportName = "WINDSURFING";//$NON-NLS-1$
         break;
      case 19:
         sportName = "PADDLING";//$NON-NLS-1$
         break;

      default:
         sportName = UI.EMPTY_STRING;
         break;
      }

      return sportName;
   }
}
