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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.IWeatherProvider;
import net.tourbook.weather.openweathermap.OpenWeatherMapRetriever;
import net.tourbook.weather.worldweatheronline.WorldWeatherOnlineRetriever;

import org.eclipse.jface.preference.IPreferenceStore;

public final class TourWeatherRetriever {

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

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

      case IWeatherProvider.Pref_Weather_Provider_None:
      default:
         break;
      }

      if (historicalWeatherRetriever == null) {
         return false;
      }

      final boolean retrievalStatus = historicalWeatherRetriever.retrieveHistoricalWeatherData();

      if (retrievalStatus) {

         TourLogManager.subLog_OK(TourManager.getTourDateTimeShort(tourData) +
               UI.SYMBOL_COLON + UI.SPACE +
               WeatherUtils.buildWeatherDataString(tourData, true, true, true));

         if (_prefStore.getBoolean(ITourbookPreferences.WEATHER_DISPLAY_FULL_LOG)) {

            TourLogManager.subLog_INFO(historicalWeatherRetriever.buildFullWeatherDataString());
         }
      } else {
         TourLogManager.subLog_INFO(String.format(
               Messages.Log_RetrieveWeatherData_003_NoWeatherData,
               TourManager.getTourDateTimeShort(tourData)));
      }

      return retrievalStatus;
   }
}
