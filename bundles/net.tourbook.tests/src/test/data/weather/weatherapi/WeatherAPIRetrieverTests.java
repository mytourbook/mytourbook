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
package data.weather.weatherapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pgssoft.httpclient.HttpClientMock;

import java.lang.reflect.Field;

import net.tourbook.data.TourData;
import net.tourbook.weather.WeatherUtils;
import net.tourbook.weather.weatherapi.WeatherApiRetriever;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;
import utils.Initializer;

public class WeatherAPIRetrieverTests {

   private static final String WEATHERAPI_FILE_PATH =
         FilesUtils.rootPath + "data/weather/weatherapi/files/"; //$NON-NLS-1$

   static HttpClientMock       httpClientMock;
   WeatherApiRetriever         weatherApiRetriever;

   @BeforeAll
   static void initAll() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {

      httpClientMock = new HttpClientMock();
      final Field field = WeatherApiRetriever.class
            .getSuperclass()
            .getDeclaredField("httpClient"); //$NON-NLS-1$
      field.setAccessible(true);
      field.set(null, httpClientMock);
   }

   /**
    * Regression test for the weather retrieval from WeatherAPI.
    */
   @Test
   void testWeatherRetrieval() {

      final String weatherApiResponse = Comparison.readFileContent(WEATHERAPI_FILE_PATH
            + "LongsPeak-Manual-WeatherApiResponse.json"); //$NON-NLS-1$
      final String url = WeatherUtils.HEROKU_APP_URL
            + "/weatherapi?lat=40.263996&lon=-105.58854099999999&lang=en&dt=2022-05-10"; //$NON-NLS-1$
      httpClientMock.onGet(url)
            .doReturn(weatherApiResponse);

      final TourData tour = Initializer.importTour();
      //Tuesday, May 10, 2022 12:00:00 PM
      tour.setTourStartTime(2022, 5, 10, 12, 0, 0);
      //We set the current time elapsed to trigger the computation of the new end time
      tour.setTourDeviceTime_Elapsed(tour.getTourDeviceTime_Elapsed());

      weatherApiRetriever = new WeatherApiRetriever(tour);

      assertTrue(weatherApiRetriever.retrieveHistoricalWeatherData());
      httpClientMock.verify().get(url).called();

// SET_FORMATTING_OFF

      assertEquals(13.92f,          tour.getWeather_Temperature_Average());
      assertEquals(11,              tour.getWeather_Wind_Speed());
      assertEquals(121,             tour.getWeather_Wind_Direction());
      assertEquals("Ensoleille",    tour.getWeather()); //$NON-NLS-1$
      assertEquals("weather-sunny", tour.getWeather_Clouds()); //$NON-NLS-1$
      assertEquals(29,              tour.getWeather_Humidity());
      assertEquals(0.0,             tour.getWeather_Precipitation());
      assertEquals(1012.0,          tour.getWeather_Pressure());
      assertEquals(22.1f,           tour.getWeather_Temperature_Max());
      assertEquals(3.4f,            tour.getWeather_Temperature_Min());
      assertEquals(13.32f,          tour.getWeather_Temperature_WindChill());

// SET_FORMATTING_ON
   }
}
