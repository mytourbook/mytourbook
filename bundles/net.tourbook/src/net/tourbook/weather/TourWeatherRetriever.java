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
package net.tourbook.weather;

import net.tourbook.data.TourData;
import net.tourbook.ui.views.IWeatherProvider;
import net.tourbook.weather.openweathermap.OpenWeatherMapRetriever;
import net.tourbook.weather.worldweatheronline.WorldWeatherOnlineRetriever;;

public final class TourWeatherRetriever {

   public static boolean retrieveWeatherData(final TourData tourData,
                                             final String weatherProvider) {

      HistoricalWeatherRetriever historicalWeatherRetriever = null;

      switch (weatherProvider) {
      case IWeatherProvider.WEATHER_PROVIDER_OPENWEATHERMAP:
         historicalWeatherRetriever = new OpenWeatherMapRetriever(tourData);
         break;
      case IWeatherProvider.WEATHER_PROVIDER_WORLDWEATHERONLINE:
         historicalWeatherRetriever = new WorldWeatherOnlineRetriever(tourData);
         break;
      case IWeatherProvider.WEATHER_PROVIDER_NONE:
      default:
         break;
      }

      if (historicalWeatherRetriever == null) {
         return false;
      }

      return historicalWeatherRetriever.retrieveHistoricalWeatherData();
   }

}
