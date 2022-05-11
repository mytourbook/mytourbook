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

@JsonIgnoreProperties(ignoreUnknown = true)
public class Payload {

   public int    activityId;
   public long   startTime;
   public int    timeOffsetInMinutes;
   public String workoutKey;

   /**
    * Gets a sport name from an activity Id.
    * The mapping is available here (cf. column "SuuntoApp"):
    * https://apimgmtstfbqznm5nc6zmvgx.blob.core.windows.net/content/MediaLibrary/docs/Suunto%20Watches-%20SuuntoApp%20-Movescount-FIT-Activities.pdf
    *
    * @return
    */
   public String getSportNameFromActivityId() {

      String sportName;

      switch (activityId) {

      case 80:
         sportName = "adventure racing";//$NON-NLS-1$
         break;
      case 69:
         sportName = "aerobics";//$NON-NLS-1$
         break;
      case 13:
         sportName = "downhill skiing";//$NON-NLS-1$
         break;
      case 94:
         sportName = "aquathlon";//$NON-NLS-1$
         break;
      case 36:
         sportName = "badminton";//$NON-NLS-1$
         break;
      case 37:
         sportName = "baseball";//$NON-NLS-1$
         break;
      case 35:
         sportName = "basketball";//$NON-NLS-1$
         break;
      case 46:
         sportName = "bowling";//$NON-NLS-1$
         break;
      case 77:
         sportName = "boxing";//$NON-NLS-1$
         break;
      case 82:
         sportName = "canoeing";//$NON-NLS-1$
         break;
      case 76:
         sportName = "cheerleading";//$NON-NLS-1$
         break;
      case 73:
         sportName = "circuit training";//$NON-NLS-1$
         break;
      case 29:
         sportName = "climbing";//$NON-NLS-1$
         break;
      case 62:
         sportName = "combat sport";//$NON-NLS-1$
         break;
      case 47:
         sportName = "cricket";//$NON-NLS-1$
         break;
      case 54:
         sportName = "crossfit";//$NON-NLS-1$
         break;
      case 3:
         sportName = "cross country skiing";//$NON-NLS-1$
         break;
      case 55:
         sportName = "crosstrainer";//$NON-NLS-1$
         break;
      case 2:
         sportName = "cycling";//$NON-NLS-1$
         break;
      case 64:
         sportName = "dancing";//$NON-NLS-1$
         break;
      case 93:
         sportName = "duathlon";//$NON-NLS-1$
         break;
      case 96:
         sportName = "fishing";//$NON-NLS-1$
         break;
      case 43:
         sportName = "floorball";//$NON-NLS-1$
         break;
      case 39:
         sportName = "american football";//$NON-NLS-1$
         break;
      case 79:
         sportName = "freediving";//$NON-NLS-1$
         break;
      case 66:
         sportName = "frisbee golf";//$NON-NLS-1$
         break;
      case 16:
         sportName = "golf";//$NON-NLS-1$
         break;
      case 81:
         sportName = "gymnastics";//$NON-NLS-1$
         break;
      case 44:
         sportName = "handball";//$NON-NLS-1$
         break;
      case 11:
         sportName = "hiking";//$NON-NLS-1$
         break;
      case 25:
         sportName = "horseback riding";//$NON-NLS-1$
         break;
      case 97:
         sportName = "hunting";//$NON-NLS-1$
         break;
      case 50:
         sportName = "ice hockey";//$NON-NLS-1$
         break;
      case 49:
         sportName = "ice skating";//$NON-NLS-1$
         break;
      case 57:
         sportName = "indoor rowing";//$NON-NLS-1$
         break;
      case 17:
         sportName = "indoor";//$NON-NLS-1$
         break;
      case 52:
         sportName = "indoor cycling";//$NON-NLS-1$
         break;
      case 72:
         sportName = "kayaking";//$NON-NLS-1$
         break;
      case 63:
         sportName = "kettlebell";//$NON-NLS-1$
         break;
      case 87:
         sportName = "kitesurfing kiting";//$NON-NLS-1$
         break;
      case 26:
         sportName = "motorsports";//$NON-NLS-1$
         break;
      case 10:
         sportName = "mountain biking";//$NON-NLS-1$
         break;
      case 83:
         sportName = "mountaineering";//$NON-NLS-1$
         break;
      case 68:
         sportName = "multisport";//$NON-NLS-1$
         break;
      case 24:
         sportName = "nordic walking";//$NON-NLS-1$
         break;
      case 4:
         sportName = "other 1";//$NON-NLS-1$
         break;
      case 95:
         sportName = "obstacle racing";//$NON-NLS-1$
         break;
      case 85:
         sportName = "openwater swimming";//$NON-NLS-1$
         break;
      case 60:
         sportName = "orienteering";//$NON-NLS-1$
         break;
      case 88:
         sportName = "paragliding";//$NON-NLS-1$
         break;
      case 41:
         sportName = "racquet ball";//$NON-NLS-1$
         break;
      case 56:
         sportName = "roller skiing";//$NON-NLS-1$
         break;
      case 15:
         sportName = "rowing";//$NON-NLS-1$
         break;
      case 48:
         sportName = "rugby";//$NON-NLS-1$
         break;
      case 1:
         sportName = "running";//$NON-NLS-1$
         break;
      case 71:
         sportName = "sailing";//$NON-NLS-1$
         break;
      case 78:
         sportName = "scubadiving";//$NON-NLS-1$
         break;
      case 12:
         sportName = "roller skating";//$NON-NLS-1$
         break;
      case 31:
         sportName = "ski touring";//$NON-NLS-1$
         break;
      case 90:
         sportName = "snorkeling";//$NON-NLS-1$
         break;
      case 65:
         sportName = "snow shoeing";//$NON-NLS-1$
         break;
      case 30:
         sportName = "snowboarding";//$NON-NLS-1$
         break;
      case 33:
         sportName = "soccer";//$NON-NLS-1$
         break;
      case 45:
         sportName = "softball";//$NON-NLS-1$
         break;
      case 42:
         sportName = "squash";//$NON-NLS-1$
         break;
      case 61:
         sportName = "standup paddling";//$NON-NLS-1$
         break;
      case 58:
         sportName = "stretching";//$NON-NLS-1$
         break;
      case 91:
         sportName = "surfing";//$NON-NLS-1$
         break;
      case 21:
         sportName = "swimming";//$NON-NLS-1$
         break;
      case 92:
         sportName = "swimrun";//$NON-NLS-1$
         break;
      case 40:
         sportName = "table tennis";//$NON-NLS-1$
         break;
      case 84:
         sportName = "telemarkskiing";//$NON-NLS-1$
         break;
      case 34:
         sportName = "tennis";//$NON-NLS-1$
         break;
      case 59:
         sportName = "track and field";//$NON-NLS-1$
         break;
      case 22:
         sportName = "trail running";//$NON-NLS-1$
         break;
      case 98:
         sportName = "transition";//$NON-NLS-1$
         break;
      case 53:
         sportName = "treadmill";//$NON-NLS-1$
         break;
      case 70:
         sportName = "trekking";//$NON-NLS-1$
         break;
      case 74:
         sportName = "triathlon";//$NON-NLS-1$
         break;
      case 38:
         sportName = "volleyball";//$NON-NLS-1$
         break;
      case 0:
         sportName = "walking";//$NON-NLS-1$
         break;
      case 23:
         sportName = "gym";//$NON-NLS-1$
         break;
      case 86:
         sportName = "windsurfing";//$NON-NLS-1$
         break;
      case 51:
         sportName = "yoga";//$NON-NLS-1$
         break;
      case 19:
         sportName = "ball games";//$NON-NLS-1$
         break;
      case 32:
         sportName = "fitness class";//$NON-NLS-1$
         break;
      case 67:
         sportName = "futsal";//$NON-NLS-1$
         break;
      case 5:
         sportName = "other 2";//$NON-NLS-1$
         break;
      case 6:
         sportName = "other 3";//$NON-NLS-1$
         break;
      case 7:
         sportName = "other 4";//$NON-NLS-1$
         break;
      case 8:
         sportName = "other 5";//$NON-NLS-1$
         break;
      case 9:
         sportName = "other 6";//$NON-NLS-1$
         break;
      case 20:
         sportName = "outdoor gym";//$NON-NLS-1$
         break;
      case 14:
         sportName = "paddling";//$NON-NLS-1$
         break;
      case 18:
         sportName = "parkour";//$NON-NLS-1$
         break;
      case 27:
         sportName = "skateboarding";//$NON-NLS-1$
         break;
      case 28:
         sportName = "water sports";//$NON-NLS-1$
         break;

      default:
         sportName = String.valueOf(activityId);
         break;
      }

      return sportName;
   }
}
