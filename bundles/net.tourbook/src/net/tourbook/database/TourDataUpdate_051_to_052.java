/*******************************************************************************
 * Copyright (C) 2023, 2025 Frédéric Bard
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

import java.util.List;

import net.tourbook.common.util.StringUtils;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;

public class TourDataUpdate_051_to_052 implements ITourDataUpdate {

   @Override
   public int getDatabaseVersion() {

      return 52;
   }

   @Override
   public List<Long> getTourIDs() {
      return null;
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

      case "Gut": //$NON-NLS-1$
      case "Bon": //$NON-NLS-1$
      case "Buona": //$NON-NLS-1$
      case "Goed": //$NON-NLS-1$
      case "Good": //$NON-NLS-1$
         airQualityDatabaseValue = IWeather.AIRQUALITY_ID_GOOD;
         break;

      case "Schön": //23.5 //$NON-NLS-1$
      case "Mittelmässig": // 23.8 //$NON-NLS-1$
      case "Acceptable": //$NON-NLS-1$
      case "Discreta": //$NON-NLS-1$
      case "Redelijk": //$NON-NLS-1$
      case "Fair": //$NON-NLS-1$
         airQualityDatabaseValue = IWeather.AIRQUALITY_ID_FAIR;
         break;

      case "Moderat": //23.5 //$NON-NLS-1$
      case "Ungesund": // 23.8 //$NON-NLS-1$
      case "Moyen": //$NON-NLS-1$
      case "Sufficiente": //$NON-NLS-1$
      case "Gemiddeld": //$NON-NLS-1$
      case "Moderate": //$NON-NLS-1$
         airQualityDatabaseValue = IWeather.AIRQUALITY_ID_MODERATE;
         break;

      case "Schlecht": //23.5 //$NON-NLS-1$
      case "Äusserst Ungesund": // 23.8 //$NON-NLS-1$
      case "Mauvais": //$NON-NLS-1$
      case "Scarsa": //$NON-NLS-1$
      case "Slecht": //$NON-NLS-1$
      case "Poor": //$NON-NLS-1$
         airQualityDatabaseValue = IWeather.AIRQUALITY_ID_POOR;
         break;

      case "Sehr schlecht": //23.5 //$NON-NLS-1$
      case "Gefährlich": // 23.8 //$NON-NLS-1$
      case "Très mauvais": //$NON-NLS-1$
      case "Molto scarsa": //$NON-NLS-1$
      case "Zeer slecht": //$NON-NLS-1$
      case "Very poor": //$NON-NLS-1$
         airQualityDatabaseValue = IWeather.AIRQUALITY_ID_VERYPOOR;
         break;

      default:
         return false;
      }

      tourData.setWeather_AirQuality(airQualityDatabaseValue);

      return true;
   }

}
