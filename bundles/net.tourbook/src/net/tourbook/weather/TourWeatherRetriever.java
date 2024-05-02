/*******************************************************************************
 * Copyright (C) 2022, 2024 Frédéric Bard
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

   /**
    * Check if the weather retrieval can be performed.
    * For example: the user could have reached the maximum number of requests
    * per day. In this case, we notify them and abort the retrieval
    *
    */
   public static boolean canRetrieveWeather(final String weatherProvider) {

      final HistoricalWeatherRetriever historicalWeatherRetriever = getHistoricalWeatherRetriever(weatherProvider);

      return historicalWeatherRetriever != null && historicalWeatherRetriever.canMakeRequest();
   }

   private static HistoricalWeatherRetriever getHistoricalWeatherRetriever(final String weatherProvider) {

      HistoricalWeatherRetriever historicalWeatherRetriever = null;
      switch (weatherProvider) {

      case IWeatherProvider.WEATHER_PROVIDER_OPENWEATHERMAP_ID:

         historicalWeatherRetriever = new OpenWeatherMapRetriever();
         break;

      case IWeatherProvider.WEATHER_PROVIDER_WEATHERAPI_ID:

         historicalWeatherRetriever = new WeatherApiRetriever();
         break;

      case IWeatherProvider.WEATHER_PROVIDER_WORLDWEATHERONLINE_ID:

         historicalWeatherRetriever = new WorldWeatherOnlineRetriever();
         break;
      }

      return historicalWeatherRetriever;
   }

   /**
    * Retrieves, for each weather vendor, a log message explaining why a weather
    * request can't be made at this time.
    *
    * @param weatherProvider
    */
   public static String getWeatherRetrievalFailureLogMessage(final String weatherProvider) {

      final HistoricalWeatherRetriever historicalWeatherRetriever = getHistoricalWeatherRetriever(weatherProvider);

      return historicalWeatherRetriever != null
            ? historicalWeatherRetriever.getWeatherRetrievalFailureLogMessage()
            : UI.EMPTY_STRING;
   }

   public static boolean retrieveWeatherData(final TourData tourData,
                                             final String weatherProvider) {

      final HistoricalWeatherRetriever historicalWeatherRetriever = getHistoricalWeatherRetriever(weatherProvider);
      if (historicalWeatherRetriever == null) {
         return false;
      }
      historicalWeatherRetriever.setTourData(tourData);

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

            /*
             * Complicated, the weather title is already set
             */
            String currentTourDataWeather = UI.EMPTY_STRING;

            final boolean isReplaceWeather = _prefStore.getBoolean(ITourbookPreferences.WEATHER_IS_APPEND_WEATHER_DESCRIPTION) == false;

            if (isReplaceWeather) {

               currentTourDataWeather = tourData.getWeather();

               if (StringUtils.hasContent(currentTourDataWeather)) {
                  currentTourDataWeather += UI.SYSTEM_NEW_LINE;
               }
            }

            final String detailedWeatherLog = historicalWeatherRetriever.buildDetailedWeatherLog(true);

            tourData.appendOrReplaceWeather(currentTourDataWeather + detailedWeatherLog);
         }

      } else {

         TourLogManager.subLog_INFO(String.format(
               Messages.Log_RetrieveWeatherData_003_NoWeatherData,
               TourManager.getTourDateTimeShort(tourData)));
      }

      return isWeatherRetrieved;
   }
}
