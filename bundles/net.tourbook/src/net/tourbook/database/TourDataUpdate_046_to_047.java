/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package net.tourbook.database;

import net.tourbook.Messages;
import net.tourbook.data.TourData;

public class TourDataUpdate_046_to_047 implements ITourDataUpdateConcurrent {

   @Override
   public int getDatabaseVersion() {

      return 47;
   }

   @Override
   public String getSplashManagerMessage() {

      // Data update 47: Converting weather data - {0} of {1} - {2} % - {3}
      return Messages.Tour_Database_PostUpdate_047_Weather;
   }

   /**
    * If the previous average, max, min temperatures were retrieved by
    * a weather provider, they are copied into the new fields.
    * If necessary, the average, max, min temperatures measured from the device
    * are recomputed
    *
    * @param tourData
    */
   @Override
   public void updateTourData(final TourData tourData) {

      /*
       * Temperature Migration
       */
      if (tourData.temperatureSerie == null || tourData.isWeatherDataFromProvider()) {

         /**
          * If the device has NO temperature data or the weather was retrieved from WWO:
          * - copy the temperatures (DB 46) to the new non-device fields (DB 47)
          */
         tourData.setWeather_Temperature_Average(tourData.getWeather_Temperature_Average_Device());
         tourData.setWeather_Temperature_Max(tourData.getWeather_Temperature_Max_Device());
         tourData.setWeather_Temperature_Min(tourData.getWeather_Temperature_Min_Device());
      }

      /**
       * If the device has NO temperature data:
       * - set the device temperatures to 0
       */
      if (tourData.temperatureSerie == null) {

         tourData.setWeather_Temperature_Average_Device(0);
         tourData.setWeather_Temperature_Max_Device(0);
         tourData.setWeather_Temperature_Min_Device(0);

      } else {

         /**
          * If the device has temperature data:
          * - recalculate the device temperatures
          */
         tourData.computeAvg_Temperature();
      }
   }

}
