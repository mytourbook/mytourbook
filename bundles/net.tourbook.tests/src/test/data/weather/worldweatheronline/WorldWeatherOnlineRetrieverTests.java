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

/**
 * Regression tests for the weather retrieval from World Weather Online.
 */
public class WorldWeatherOnlineRetrieverTests {

   private static final String WORLDWEATHERONLINE_FILE_PATH =
         FilesUtils.rootPath + "data/weather/worldweatheronline/files/"; //$NON-NLS-1$

   static HttpClientMock       httpClientMock;
   WorldWeatherOnlineRetriever historicalWeatherRetriever;

   @BeforeAll
   static void initAll() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

      httpClientMock = new HttpClientMock();
      final Field field = WorldWeatherOnlineRetriever.class
            .getSuperclass()
            .getDeclaredField("httpClient"); //$NON-NLS-1$
      field.setAccessible(true);
      field.set(null, httpClientMock);
   }

   @Test
   void testWeatherRetrieval_July2020() {

      final String worldWeatherOnlineResponse = Comparison.readFileContent(WORLDWEATHERONLINE_FILE_PATH
            + "LongsPeak-Manual-WorldWeatherOnlineResponse-2020-07-04.json"); //$NON-NLS-1$
      final String url =
            "http://api.worldweatheronline.com/premium/v1/past-weather.ashx?key=&q=40.263996,-105.58854099999999&date=2020-07-04&tp=1&format=json&includelocation=yes&extra=utcDateTime&lang=en"; //$NON-NLS-1$
      httpClientMock.onGet(url)
            .doReturn(worldWeatherOnlineResponse);

      final TourData tour = Initializer.importTour();
      historicalWeatherRetriever = new WorldWeatherOnlineRetriever(tour);

      assertTrue(historicalWeatherRetriever.retrieveHistoricalWeatherData());
      httpClientMock.verify().get(url).called();

// SET_FORMATTING_OFF

      assertEquals(15.92f,                      tour.getWeather_Temperature_Average());
      assertEquals(2,                           tour.getWeather_Wind_Speed());
      assertEquals(120,                         tour.getWeather_Wind_Direction());
      assertEquals("Patchy rain possible",      tour.getWeather()); //$NON-NLS-1$
      assertEquals("weather-showers-scattered", tour.getWeather_Clouds()); //$NON-NLS-1$
      assertEquals(49,                          tour.getWeather_Humidity());
      assertEquals(1.6f,                        tour.getWeather_Precipitation());
      assertEquals(1016.54f,                    tour.getWeather_Pressure());
      assertEquals(19,                          tour.getWeather_Temperature_Max());
      assertEquals(8,                           tour.getWeather_Temperature_Min());
      assertEquals(15.62f,                      tour.getWeather_Temperature_WindChill());

// SET_FORMATTING_ON

   }

   @Test
   void testWeatherRetrieval_July2022() {

      final String worldWeatherOnlineResponse = Comparison.readFileContent(WORLDWEATHERONLINE_FILE_PATH
            + "LongsPeak-Manual-WorldWeatherOnlineResponse-2022-07-02.json"); //$NON-NLS-1$
      final String url =
            "http://api.worldweatheronline.com/premium/v1/past-weather.ashx?key=&q=40.263996,-105.58854099999999&date=2022-07-02&tp=1&format=json&includelocation=yes&extra=utcDateTime&lang=en"; //$NON-NLS-1$
      httpClientMock.onGet(url)
            .doReturn(worldWeatherOnlineResponse);

      final TourData tour = Initializer.importTour();
      //Tuesday, July 2, 2022 12:00:00 AM
      tour.setTourStartTime(2022, 7, 2, 0, 0, 0);
      //We set the current time elapsed to trigger the computation of the new end time
      tour.setTourDeviceTime_Elapsed(tour.getTourDeviceTime_Elapsed());

      historicalWeatherRetriever = new WorldWeatherOnlineRetriever(tour);

      assertTrue(historicalWeatherRetriever.retrieveHistoricalWeatherData());
      httpClientMock.verify().get(url).called();

// SET_FORMATTING_OFF

      assertEquals(8,                      tour.getWeather_Temperature_Average());
      assertEquals(12,                     tour.getWeather_Wind_Speed());
      assertEquals(267,                    tour.getWeather_Wind_Direction());
      assertEquals("Patchy rain possible", tour.getWeather()); //$NON-NLS-1$
      assertEquals("weather-drizzle",      tour.getWeather_Clouds()); //$NON-NLS-1$
      assertEquals(79,                     tour.getWeather_Humidity());
      assertEquals(1.1f,                   tour.getWeather_Precipitation());
      assertEquals(1019f,                  tour.getWeather_Pressure());
      assertEquals(8,                      tour.getWeather_Temperature_Max());
      assertEquals(8,                      tour.getWeather_Temperature_Min());
      assertEquals(5.57f,                  tour.getWeather_Temperature_WindChill());

// SET_FORMATTING_ON

   }
}
