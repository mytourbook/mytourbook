/*******************************************************************************
 * Copyright (C) 2022, 2023 Frédéric Bard
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.IWeatherProvider;
import net.tourbook.weather.openweathermap.OpenWeatherMapRetriever;
import net.tourbook.weather.weatherapi.WeatherApiRetriever;
import net.tourbook.weather.worldweatheronline.WorldWeatherOnlineRetriever;

import org.eclipse.jface.preference.IPreferenceStore;

public final class TourWeatherRetriever {

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   public static boolean retrieveWeatherData(final TourData tourData,
                                             final String weatherProvider) {

      HistoricalWeatherRetriever historicalWeatherRetriever = null;

      switch (weatherProvider) {

      case IWeatherProvider.WEATHER_PROVIDER_OPENWEATHERMAP_ID:

         historicalWeatherRetriever = new OpenWeatherMapRetriever(tourData);
         break;

      case IWeatherProvider.WEATHER_PROVIDER_WEATHERAPI_ID:

         historicalWeatherRetriever = new WeatherApiRetriever(tourData);
         break;

      case IWeatherProvider.WEATHER_PROVIDER_WORLDWEATHERONLINE_ID:

         historicalWeatherRetriever = new WorldWeatherOnlineRetriever(tourData);
         break;

      case IWeatherProvider.Pref_Weather_Provider_None:
      default:
         return false;
      }

      final boolean isWeatherRetrieved = historicalWeatherRetriever.retrieveHistoricalWeatherData();
      if (isWeatherRetrieved) {

         tourData.setIsWeatherDataFromProvider(true);

         TourLogManager.subLog_OK(TourManager.getTourDateTimeShort(tourData) +
               UI.SYMBOL_COLON + UI.SPACE +
               WeatherUtils.buildWeatherDataString(tourData, true, true, false));

         if (_prefStore.getBoolean(ITourbookPreferences.WEATHER_DISPLAY_FULL_LOG)) {

            final String detailedWeatherLog = historicalWeatherRetriever.buildDetailedWeatherLog(false);
            TourLogManager.subLog_INFO(detailedWeatherLog);
         }
         if (_prefStore.getBoolean(ITourbookPreferences.WEATHER_SAVE_LOG_IN_TOUR_WEATHER_DESCRIPTION)) {

            String tourDataWeather = tourData.getWeather();
            if (StringUtils.hasContent(tourDataWeather)) {
               tourDataWeather += UI.SYSTEM_NEW_LINE;
            }

            final String detailedWeatherLog = historicalWeatherRetriever.buildDetailedWeatherLog(true);
            tourData.setWeather(tourDataWeather + detailedWeatherLog);
         }
      } else {
         TourLogManager.subLog_INFO(String.format(
               Messages.Log_RetrieveWeatherData_003_NoWeatherData,
               TourManager.getTourDateTimeShort(tourData)));
      }

      return isWeatherRetrieved;
   }
}
