/*******************************************************************************
 * Copyright (C) 2020, 2022 Frédéric Bard
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
package data.weather.worldweatheronline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pgssoft.httpclient.HttpClientMock;

import java.lang.reflect.Field;

import net.tourbook.data.TourData;
import net.tourbook.weather.worldweatheronline.WorldWeatherOnlineRetriever;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;
import utils.Initializer;

public class WorldWeatherOnlineRetrieverTests {

   private static final String WORLDWEATHERONLINE_FILE_PATH =
         FilesUtils.rootPath + "data/weather/worldweatheronline/files/"; //$NON-NLS-1$

   static HttpClientMock       httpClientMock;
   WorldWeatherOnlineRetriever historicalWeatherRetriever;

   @BeforeAll
   static void initAll() {

      httpClientMock = new HttpClientMock();
   }

   /**
    * Regression test for the weather retrieval from World Weather Online.
    */
   @Test
   void testWeatherRetrieval() throws IllegalAccessException, NoSuchFieldException {

      final String worldWeatherOnlineResponse = Comparison.readFileContent(WORLDWEATHERONLINE_FILE_PATH
            + "LongsPeak-Manual-WorldWeatherOnlineResponse.json"); //$NON-NLS-1$
      final String url =
            "http://api.worldweatheronline.com/premium/v1/past-weather.ashx?key=&q=40.263996,-105.58854099999999&date=2020-07-04&tp=1&format=json&includelocation=yes&extra=utcDateTime"; //$NON-NLS-1$
      httpClientMock.onGet(url)
            .doReturn(worldWeatherOnlineResponse);
      final Field field = WorldWeatherOnlineRetriever.class
            .getSuperclass()
            .getDeclaredField("httpClient"); //$NON-NLS-1$
      field.setAccessible(true);
      field.set(null, httpClientMock);

      final TourData tour = Initializer.importTour();
      historicalWeatherRetriever = new WorldWeatherOnlineRetriever(tour);

      assertTrue(historicalWeatherRetriever.retrieveHistoricalWeatherData());
      httpClientMock.verify().get(url).called();

// SET_FORMATTING_OFF

      assertEquals(12,               tour.getWeather_Temperature_Average());
      assertEquals(9,                tour.getWeather_Wind_Speed());
      assertEquals(185,              tour.getWeather_Wind_Direction());
      assertEquals("Partly cloudy",  tour.getWeather()); //$NON-NLS-1$
      assertEquals("weather-cloudy", tour.getWeather_Clouds()); //$NON-NLS-1$
      assertEquals(64,               tour.getWeather_Humidity());
      assertEquals(1.2,              Math.round(tour.getWeather_Precipitation() * 10.0) / 10.0);
      assertEquals(1019,             tour.getWeather_Pressure());
      assertEquals(19,               tour.getWeather_Temperature_Max());
      assertEquals(8,                tour.getWeather_Temperature_Min());
      assertEquals(11,               tour.getWeather_Temperature_WindChill());

// SET_FORMATTING_ON

   }
}
