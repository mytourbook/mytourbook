/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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

import net.tourbook.common.util.StringUtils;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;

public class TourDataUpdate_051_to_052 implements ITourDataUpdate {

   @Override
   public int getDatabaseVersion() {

      return 52;
   }

   /**
    * For each tour containing translated strings for the air quality
    * (as of 23.10), this function updates the database by saving a unique air
    * quality identifier instead of the existing translated string.
    *
    * See issue: https://github.com/mytourbook/mytourbook/issues/1193
    *
    * Note that because the German translations were changed in 23.8, the function
    * can have 2 german translations for each air quality ids.
    */
   @Override
   public boolean updateTourData(final TourData tourData) {

      final String translatedAirQuality = tourData.getWeather_AirQuality();
      if (StringUtils.isNullOrEmpty(translatedAirQuality)) {
         return false;
      }

      String airQualityDatabaseValue;
      switch (translatedAirQuality) {

      case "Gut":
      case "Bon":
      case "Buona":
      case "Goed":
      case "Good":
         airQualityDatabaseValue = IWeather.AIRQUALITY_ID_GOOD;
         break;

      case "Schön": //23.5
      case "Mittelmässig": // 23.8
      case "Acceptable":
      case "Discreta":
      case "Redelijk":
      case "Fair":
         airQualityDatabaseValue = IWeather.AIRQUALITY_ID_FAIR;
         break;

      case "Moderat": //23.5
      case "Ungesund": // 23.8
      case "Moyen":
      case "Sufficiente":
      case "Gemiddeld":
      case "Moderate":
         airQualityDatabaseValue = IWeather.AIRQUALITY_ID_MODERATE;
         break;

      case "Schlecht": //23.5
      case "Äusserst Ungesund": // 23.8
      case "Mauvais":
      case "Scarsa":
      case "Slecht":
      case "Poor":
         airQualityDatabaseValue = IWeather.AIRQUALITY_ID_POOR;
         break;

      case "Sehr schlecht": //23.5
      case "Gefährlich": // 23.8
      case "Très mauvais":
      case "Molto scarsa":
      case "Zeer slecht":
      case "Very poor":
         airQualityDatabaseValue = IWeather.AIRQUALITY_ID_VERYPOOR;
         break;

      default:
         return false;
      }

      tourData.setWeather_AirQuality(airQualityDatabaseValue);

      return true;
   }

}
