/*******************************************************************************
 * Copyright (C) 2022, 2025 Frédéric Bard
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

import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;

public class TourDataUpdate_047_to_048 implements ITourDataUpdate {

   @Override
   public int getDatabaseVersion() {

      return 48;
   }

   @Override
   public List<Long> getTourIDs() {
      return null;
   }

   @Override
   public boolean updateTourData(final TourData tourData) {

      // convert weather-showers-scatterd
      //    into weather-showers-scattered

      if ("weather-showers-scatterd".equalsIgnoreCase(tourData.getWeather_Clouds())) { //$NON-NLS-1$

         /**
          * If the weather cloud has the old value (with the typo) for the scattered showers,
          * it is updated to the new value
          */
         tourData.setWeather_Clouds(IWeather.WEATHER_ID_SCATTERED_SHOWERS);

         TourDatabase.logDbUpdate("Converted weather field '%s' in %s".formatted( //$NON-NLS-1$
               IWeather.WEATHER_ID_SCATTERED_SHOWERS,
               TourManager.getTourDateTimeShort(tourData)));

         return true;
      }

      return false;
   }

}
