/*******************************************************************************
 * Copyright (C) 2024 Frédéric Bard
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
package weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.ui.views.IWeatherProvider;
import net.tourbook.weather.TourWeatherRetriever;

import org.junit.jupiter.api.Test;

public class TourWeatherRetrieverTests {

   @Test
   void testCanRetrieveWeather() {

      // OpenWeatherMap
      assertTrue(TourWeatherRetriever.canRetrieveWeather(IWeatherProvider.WEATHER_PROVIDER_OPENWEATHERMAP_ID));

      // WeatherApi
      assertTrue(TourWeatherRetriever.canRetrieveWeather(IWeatherProvider.WEATHER_PROVIDER_WEATHERAPI_ID));

      // WorldWeatherOnline
      assertTrue(TourWeatherRetriever.canRetrieveWeather(IWeatherProvider.WEATHER_PROVIDER_WORLDWEATHERONLINE_ID));

      // No provider
      assertFalse(TourWeatherRetriever.canRetrieveWeather(UI.EMPTY_STRING));
   }

   @Test
   void testGetWeatherRetrievalFailureLogMessage() {

      // OpenWeatherMap
      assertEquals(Messages.Log_HistoricalWeatherRetriever_003_RetrievalLimitReached,
            TourWeatherRetriever.getWeatherRetrievalFailureLogMessage(IWeatherProvider.WEATHER_PROVIDER_OPENWEATHERMAP_ID));

      // WeatherApi
      assertEquals(UI.EMPTY_STRING, TourWeatherRetriever.getWeatherRetrievalFailureLogMessage(IWeatherProvider.WEATHER_PROVIDER_WEATHERAPI_ID));

      // WorldWeatherOnline
      assertEquals(UI.EMPTY_STRING,
            TourWeatherRetriever.getWeatherRetrievalFailureLogMessage(IWeatherProvider.WEATHER_PROVIDER_WORLDWEATHERONLINE_ID));

      // No provider
      assertEquals(UI.EMPTY_STRING, TourWeatherRetriever.getWeatherRetrievalFailureLogMessage(UI.EMPTY_STRING));
   }

}
