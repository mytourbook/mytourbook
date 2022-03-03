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

      final String worldWeatherOnlineResponse = Comparison.readFileContent(OPENWEATHERMAP_FILE_PATH
            + "LongsPeak-Manual-OpenWeatherMapResponse.json"); //$NON-NLS-1$
      httpClientMock.onGet(
            "https://api.openweathermap.org/data/2.5/onecall/timemachine?units=metric&lat=40.263996&lon=-105.58854099999999&dt=1646136000") //$NON-NLS-1$
            .doReturn(worldWeatherOnlineResponse);
      final Field field = OpenWeatherMapRetriever.class.getDeclaredField("httpClient"); //$NON-NLS-1$
      field.setAccessible(true);
      field.set(null, httpClientMock);

      final TourData tour = Initializer.importTour();
      //Tuesday, March 1, 2022 12:00:00 PM
      tour.setTourStartTime(2022, 3, 1, 12, 0, 0);

      openWeatherMapRetriever = new OpenWeatherMapRetriever(tour);

      assertTrue(openWeatherMapRetriever.retrieveHistoricalWeatherData());
//      httpClientMock.verify().post(HEROKU_APP_URL_TOKEN).called();

      //todo fb is that normal to have lots of decimals???
      assertEquals("clear sky", tour.getWeather()); //$NON-NLS-1$
      assertEquals("weather-sunny", tour.getWeather_Clouds()); //$NON-NLS-1$
      assertEquals(-5.788750171661377, tour.getWeather_Temperature_Average());
      assertEquals(4.0, tour.getWeather_Wind_Speed());
      assertEquals(268.0, tour.getWeather_Wind_Direction());
      assertEquals(56.0, tour.getWeather_Humidity());
      assertEquals(0, tour.getWeather_Precipitation());
      assertEquals(1025.0, tour.getWeather_Pressure());
      assertEquals(5.78000020980835, tour.getWeather_Temperature_Max());
      assertEquals(-19.639999389648438, tour.getWeather_Temperature_Min());
      assertEquals(-10.65291690826416, tour.getWeather_Temperature_WindChill());
   }
}
