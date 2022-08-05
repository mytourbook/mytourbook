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

import net.tourbook.data.TourData;
import net.tourbook.weather.WeatherUtils;
import net.tourbook.weather.openweathermap.OpenWeatherMapRetriever;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;
import utils.Initializer;

/**
 * Regression tests for the weather retrieval from OpenWeatherMap.
 */
public class OpenWeatherMapRetrieverTests {

   private static final String OPENWEATHERMAP_BASE_URL  = WeatherUtils.HEROKU_APP_URL
         + "/openweathermap/timemachine?units=metric&lat=40.263996&lon=-105.58854099999999&lang=en&dt="; //$NON-NLS-1$

   private static final String OPENWEATHERMAP_FILE_PATH =
         FilesUtils.rootPath + "data/weather/openweathermap/files/";                                    //$NON-NLS-1$

   static HttpClientMock       httpClientMock;
   OpenWeatherMapRetriever     openWeatherMapRetriever;

   @BeforeAll
   static void initAll() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

      httpClientMock = new HttpClientMock();
      final Field field = OpenWeatherMapRetriever.class
            .getSuperclass()
            .getDeclaredField("httpClient"); //$NON-NLS-1$
      field.setAccessible(true);
      field.set(null, httpClientMock);
   }

   @Test
   void testWeatherRetrieval_JulySecond2022() {

      final String openWeatherMapResponse = Comparison.readFileContent(OPENWEATHERMAP_FILE_PATH
            + "LongsPeak-Manual-OpenWeatherMapResponse-1656720000.json"); //$NON-NLS-1$

      final String url = OPENWEATHERMAP_BASE_URL + "1656720000"; //$NON-NLS-1$
      httpClientMock.onGet(url).doReturn(openWeatherMapResponse);

      final TourData tour = Initializer.importTour();
      //Tuesday, July 2, 2022 12:00:00 AM
      tour.setTourStartTime(2022, 7, 2, 0, 0, 0);
      //We set the current time elapsed to trigger the computation of the new end time
      tour.setTourDeviceTime_Elapsed(tour.getTourDeviceTime_Elapsed());

      openWeatherMapRetriever = new OpenWeatherMapRetriever(tour);

      assertTrue(openWeatherMapRetriever.retrieveHistoricalWeatherData());
      httpClientMock.verify().get(url).called();

// SET_FORMATTING_OFF

      assertEquals("broken clouds",   tour.getWeather()); //$NON-NLS-1$
      assertEquals("weather-drizzle", tour.getWeather_Clouds()); //$NON-NLS-1$
      assertEquals(7.58f,             tour.getWeather_Temperature_Average());
      assertEquals(3,                 tour.getWeather_Wind_Speed());
      assertEquals(240,               tour.getWeather_Wind_Direction());
      assertEquals(70,                tour.getWeather_Humidity());
      assertEquals(0,                 tour.getWeather_Precipitation());
      assertEquals(0,                 tour.getWeather_Snowfall());
      assertEquals(1007,              tour.getWeather_Pressure());
      assertEquals(14.15f,            tour.getWeather_Temperature_Max());
      assertEquals(3.93f,             tour.getWeather_Temperature_Min());
      assertEquals(6.33f,             tour.getWeather_Temperature_WindChill());

// SET_FORMATTING_ON
   }

   @Test
   void testWeatherRetrieval_JulySixth2022() {

      final String openWeatherMapResponse = Comparison.readFileContent(OPENWEATHERMAP_FILE_PATH
            + "LongsPeak-Manual-OpenWeatherMapResponse-1657065600.json"); //$NON-NLS-1$

      final String url = OPENWEATHERMAP_BASE_URL + "1657065600"; //$NON-NLS-1$
      httpClientMock.onGet(url).doReturn(openWeatherMapResponse);

      final TourData tour = Initializer.importTour();
      //Tuesday, July 6, 2022 12:00:00 AM
      tour.setTourStartTime(2022, 7, 6, 0, 0, 0);
      //We set the current time elapsed to trigger the computation of the new end time
      tour.setTourDeviceTime_Elapsed(tour.getTourDeviceTime_Elapsed());

      openWeatherMapRetriever = new OpenWeatherMapRetriever(tour);

      assertTrue(openWeatherMapRetriever.retrieveHistoricalWeatherData());
      httpClientMock.verify().get(url).called();

// SET_FORMATTING_OFF

      assertEquals("overcast clouds", tour.getWeather()); //$NON-NLS-1$
      assertEquals("weather-rain",    tour.getWeather_Clouds()); //$NON-NLS-1$
      assertEquals(8.35f,             tour.getWeather_Temperature_Average());
      assertEquals(3,                 tour.getWeather_Wind_Speed());
      assertEquals(268,               tour.getWeather_Wind_Direction());
      assertEquals(72,                tour.getWeather_Humidity());
      assertEquals(0.42f,             tour.getWeather_Precipitation());
      assertEquals(0,                 tour.getWeather_Snowfall());
      assertEquals(1008,              tour.getWeather_Pressure());
      assertEquals(14.13f,            tour.getWeather_Temperature_Max());
      assertEquals(5.95f,             tour.getWeather_Temperature_Min());
      assertEquals(7.66f,             tour.getWeather_Temperature_WindChill());

// SET_FORMATTING_ON
   }

   @Test
   void testWeatherRetrieval_March2022() {

      final String openWeatherMapResponse1 = Comparison.readFileContent(OPENWEATHERMAP_FILE_PATH
            + "LongsPeak-Manual-OpenWeatherMapResponse-1647086400.json"); //$NON-NLS-1$

      final String url1 = OPENWEATHERMAP_BASE_URL + "1647086400"; //$NON-NLS-1$
      httpClientMock.onGet(url1).doReturn(openWeatherMapResponse1);

      final String openWeatherMapResponse2 = Comparison.readFileContent(OPENWEATHERMAP_FILE_PATH
            + "LongsPeak-Manual-OpenWeatherMapResponse-1647129600.json"); //$NON-NLS-1$
      final String url2 = OPENWEATHERMAP_BASE_URL + "1647129600"; //$NON-NLS-1$
      httpClientMock.onGet(url2).doReturn(openWeatherMapResponse2);

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
      assertEquals(11,                 tour.getWeather_Wind_Speed());
      assertEquals(280,                tour.getWeather_Wind_Direction());
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
