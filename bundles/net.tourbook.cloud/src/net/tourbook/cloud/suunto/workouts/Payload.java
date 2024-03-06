/*******************************************************************************
 * Copyright (C) 2021, 2024 Frédéric Bard
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

@JsonIgnoreProperties(ignoreUnknown = true)
public record Payload(int activityId,
                      long startTime,
                      int timeOffsetInMinutes,
                      String workoutKey) {

   /**
    * Gets a sport name from an activity Id.
    * The mapping is available here (cf. column "SuuntoApp"):
    * https://apimgmtstfbqznm5nc6zmvgx.blob.core.windows.net/content/MediaLibrary/docs/Suunto%20Watches-%20SuuntoApp%20-Movescount-FIT-Activities.pdf
    *
    * @return
    */
   public String getSportNameFromActivityId() {

      switch (activityId) {

      case 80:
         return "adventure racing";//$NON-NLS-1$

      case 69:
         return "aerobics";//$NON-NLS-1$

      case 13:
         return "downhill skiing";//$NON-NLS-1$

      case 94:
         return "aquathlon";//$NON-NLS-1$

      case 36:
         return "badminton";//$NON-NLS-1$

      case 37:
         return "baseball";//$NON-NLS-1$

      case 35:
         return "basketball";//$NON-NLS-1$

      case 46:
         return "bowling";//$NON-NLS-1$

      case 77:
         return "boxing";//$NON-NLS-1$

      case 82:
         return "canoeing";//$NON-NLS-1$

      case 76:
         return "cheerleading";//$NON-NLS-1$

      case 73:
         return "circuit training";//$NON-NLS-1$

      case 29:
         return "climbing";//$NON-NLS-1$

      case 62:
         return "combat sport";//$NON-NLS-1$

      case 47:
         return "cricket";//$NON-NLS-1$

      case 54:
         return "crossfit";//$NON-NLS-1$

      case 3:
         return "cross country skiing";//$NON-NLS-1$

      case 55:
         return "crosstrainer";//$NON-NLS-1$

      case 2:
         return "cycling";//$NON-NLS-1$

      case 64:
         return "dancing";//$NON-NLS-1$

      case 93:
         return "duathlon";//$NON-NLS-1$

      case 96:
         return "fishing";//$NON-NLS-1$

      case 43:
         return "floorball";//$NON-NLS-1$

      case 39:
         return "american football";//$NON-NLS-1$

      case 79:
         return "freediving";//$NON-NLS-1$

      case 66:
         return "frisbee golf";//$NON-NLS-1$

      case 16:
         return "golf";//$NON-NLS-1$

      case 81:
         return "gymnastics";//$NON-NLS-1$

      case 44:
         return "handball";//$NON-NLS-1$

      case 11:
         return "hiking";//$NON-NLS-1$

      case 25:
         return "horseback riding";//$NON-NLS-1$

      case 97:
         return "hunting";//$NON-NLS-1$

      case 50:
         return "ice hockey";//$NON-NLS-1$

      case 49:
         return "ice skating";//$NON-NLS-1$

      case 57:
         return "indoor rowing";//$NON-NLS-1$

      case 17:
         return "indoor";//$NON-NLS-1$

      case 52:
         return "indoor cycling";//$NON-NLS-1$

      case 72:
         return "kayaking";//$NON-NLS-1$

      case 63:
         return "kettlebell";//$NON-NLS-1$

      case 87:
         return "kitesurfing kiting";//$NON-NLS-1$

      case 26:
         return "motorsports";//$NON-NLS-1$

      case 10:
         return "mountain biking";//$NON-NLS-1$

      case 83:
         return "mountaineering";//$NON-NLS-1$

      case 68:
         return "multisport";//$NON-NLS-1$

      case 24:
         return "nordic walking";//$NON-NLS-1$

      case 4:
         return "other 1";//$NON-NLS-1$

      case 95:
         return "obstacle racing";//$NON-NLS-1$

      case 85:
         return "openwater swimming";//$NON-NLS-1$

      case 60:
         return "orienteering";//$NON-NLS-1$

      case 88:
         return "paragliding";//$NON-NLS-1$

      case 41:
         return "racquet ball";//$NON-NLS-1$

      case 56:
         return "roller skiing";//$NON-NLS-1$

      case 15:
         return "rowing";//$NON-NLS-1$

      case 48:
         return "rugby";//$NON-NLS-1$

      case 1:
         return "running";//$NON-NLS-1$

      case 71:
         return "sailing";//$NON-NLS-1$

      case 78:
         return "scubadiving";//$NON-NLS-1$

      case 12:
         return "roller skating";//$NON-NLS-1$

      case 31:
         return "ski touring";//$NON-NLS-1$

      case 90:
         return "snorkeling";//$NON-NLS-1$

      case 65:
         return "snow shoeing";//$NON-NLS-1$

      case 30:
         return "snowboarding";//$NON-NLS-1$

      case 33:
         return "soccer";//$NON-NLS-1$

      case 45:
         return "softball";//$NON-NLS-1$

      case 42:
         return "squash";//$NON-NLS-1$

      case 61:
         return "standup paddling";//$NON-NLS-1$

      case 58:
         return "stretching";//$NON-NLS-1$

      case 91:
         return "surfing";//$NON-NLS-1$

      case 21:
         return "swimming";//$NON-NLS-1$

      case 92:
         return "swimrun";//$NON-NLS-1$

      case 40:
         return "table tennis";//$NON-NLS-1$

      case 84:
         return "telemarkskiing";//$NON-NLS-1$

      case 34:
         return "tennis";//$NON-NLS-1$

      case 59:
         return "track and field";//$NON-NLS-1$

      case 22:
         return "trail running";//$NON-NLS-1$

      case 98:
         return "transition";//$NON-NLS-1$

      case 53:
         return "treadmill";//$NON-NLS-1$

      case 70:
         return "trekking";//$NON-NLS-1$

      case 74:
         return "triathlon";//$NON-NLS-1$

      case 38:
         return "volleyball";//$NON-NLS-1$

      case 0:
         return "walking";//$NON-NLS-1$

      case 23:
         return "gym";//$NON-NLS-1$

      case 86:
         return "windsurfing";//$NON-NLS-1$

      case 51:
         return "yoga";//$NON-NLS-1$

      case 19:
         return "ball games";//$NON-NLS-1$

      case 32:
         return "fitness class";//$NON-NLS-1$

      case 67:
         return "futsal";//$NON-NLS-1$

      case 5:
         return "other 2";//$NON-NLS-1$

      case 6:
         return "other 3";//$NON-NLS-1$

      case 7:
         return "other 4";//$NON-NLS-1$

      case 8:
         return "other 5";//$NON-NLS-1$

      case 9:
         return "other 6";//$NON-NLS-1$

      case 20:
         return "outdoor gym";//$NON-NLS-1$

      case 14:
         return "paddling";//$NON-NLS-1$

      case 18:
         return "parkour";//$NON-NLS-1$

      case 27:
         return "skateboarding";//$NON-NLS-1$

      case 28:
         return "water sports";//$NON-NLS-1$

      default:
         return String.valueOf(activityId);
      }
   }
}
