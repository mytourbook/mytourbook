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
package data.weather.openweathermap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pgssoft.httpclient.HttpClientMock;

import java.lang.reflect.Field;

import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.data.TourData;
import net.tourbook.weather.openweathermap.OpenWeatherMapRetriever;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;
import utils.Initializer;

public class OpenWeatherMapRetrieverTests {

   private static final String OPENWEATHERMAP_FILE_PATH =
         FilesUtils.rootPath + "data/weather/openweathermap/files/"; //$NON-NLS-1$

   static HttpClientMock       httpClientMock;
   OpenWeatherMapRetriever     openWeatherMapRetriever;

   @BeforeAll
   static void initAll() {

      httpClientMock = new HttpClientMock();
   }

   /**
    * Regression test for the weather retrieval from OpenWeatherMap.
    */
   @Test
   void testWeatherRetrieval() throws IllegalAccessException, NoSuchFieldException {

      final String urlBase = OAuth2Constants.HEROKU_APP_URL
            + "/openweathermap/timemachine?units=metric&lat=40.263996&lon=-105.58854099999999&lang=en&dt="; //$NON-NLS-1$

      final String openWeatherMapResponse1 = Comparison.readFileContent(OPENWEATHERMAP_FILE_PATH
            + "LongsPeak-Manual-OpenWeatherMapResponse-1647086400.json"); //$NON-NLS-1$

      //First call configuration
      final String url1 = urlBase + "1647086400"; //$NON-NLS-1$
      httpClientMock.onGet(url1)
            .doReturn(openWeatherMapResponse1);

      final String openWeatherMapResponse2 = Comparison.readFileContent(OPENWEATHERMAP_FILE_PATH
            + "LongsPeak-Manual-OpenWeatherMapResponse-1647129600.json"); //$NON-NLS-1$
      final String url2 = urlBase + "1647129600"; //$NON-NLS-1$
      httpClientMock.onGet(url2)
            .doReturn(openWeatherMapResponse2);

      //Second call configuration
      final Field field = OpenWeatherMapRetriever.class
            .getSuperclass()
            .getDeclaredField("httpClient"); //$NON-NLS-1$
      field.setAccessible(true);
      field.set(null, httpClientMock);

      final TourData tour = Initializer.importTour();
      //Tuesday, March 12, 2022 12:00:00 PM
      tour.setTourStartTime(2022, 3, 12, 12, 0, 0);
      //We set the current time elapsed to trigger the computation of the new end time
      tour.setTourDeviceTime_Elapsed(tour.getTourDeviceTime_Elapsed());

      openWeatherMapRetriever = new OpenWeatherMapRetriever(tour);

      assertTrue(openWeatherMapRetriever.retrieveHistoricalWeatherData());
      httpClientMock.verify().get(url1).called();
      httpClientMock.verify().get(url2).called();

// SET_FORMATTING_OFF

      assertEquals("scattered clouds", tour.getWeather()); //$NON-NLS-1$
      assertEquals("weather-cloudy",   tour.getWeather_Clouds()); //$NON-NLS-1$
      assertEquals(-5.91f,             tour.getWeather_Temperature_Average());
      assertEquals(15,                 tour.getWeather_Wind_Speed());
      assertEquals(266,                tour.getWeather_Wind_Direction());
      assertEquals(54,                 tour.getWeather_Humidity());
      assertEquals(0.76f,              tour.getWeather_Precipitation());
      assertEquals(0,                  tour.getWeather_Snowfall());
      assertEquals(1024,               tour.getWeather_Pressure());
      assertEquals(-0.87f,             tour.getWeather_Temperature_Max());
      assertEquals(-15.96f,            tour.getWeather_Temperature_Min());
      assertEquals(-11.07f,            tour.getWeather_Temperature_WindChill());

// SET_FORMATTING_ON
   }
}
