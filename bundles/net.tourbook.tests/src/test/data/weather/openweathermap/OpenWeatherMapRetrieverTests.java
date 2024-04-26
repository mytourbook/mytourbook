/*******************************************************************************
 * Copyright (C) 2020, 2024 Frédéric Bard
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pgssoft.httpclient.HttpClientMock;

import java.lang.reflect.Field;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.weather.IWeather;
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

   private static final String OPENWEATHERMAP_BASE_URL                = WeatherUtils.OAUTH_PASSEUR_APP_URL
         + "/openweathermap/3.0/timemachine?units=metric&lat=40.263996&lon=-105.58854099999999&lang=en&dt="; //$NON-NLS-1$

   private static final String OPENWEATHERMAP_FILE_PATH               =
         FilesUtils.rootPath + "data/weather/openweathermap/files/";                                         //$NON-NLS-1$

   private static final String OPENWEATHERMAP_RESPONSE_BASE_FILE_PATH =
         OPENWEATHERMAP_FILE_PATH + "LongsPeak-Manual-OpenWeatherMapResponse-%s.json";                       //$NON-NLS-1$

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

      final List<String> timeStamps = Arrays.asList("1656720000");
      final List<String> urls = new ArrayList<>();

      timeStamps.forEach(timeStamp -> {

         final String openWeatherMapResponse = Comparison.readFileContent(String.format(OPENWEATHERMAP_RESPONSE_BASE_FILE_PATH, timeStamp));

         final String url = OPENWEATHERMAP_BASE_URL + "1656720000"; //$NON-NLS-1$
         urls.add(url);
         httpClientMock.onGet(url).doReturn(openWeatherMapResponse);
      });

      final TourData tour = Initializer.importTour();
      //Tuesday, July 2, 2022 12:00:00 AM
      final ZonedDateTime zonedDateTime = ZonedDateTime.of(
            2022,
            7,
            2,
            0,
            0,
            0,
            0,
            TimeTools.UTC);
      tour.setTourStartTime(zonedDateTime);
      //We set the current time elapsed to trigger the computation of the new end time
      tour.setTourDeviceTime_Elapsed(tour.getTourDeviceTime_Elapsed());

      openWeatherMapRetriever = new OpenWeatherMapRetriever(tour);

      assertTrue(openWeatherMapRetriever.retrieveHistoricalWeatherData(), "The weather was retrieved"); //$NON-NLS-1$

      urls.forEach(url -> httpClientMock.verify().get(url).called());

// SET_FORMATTING_OFF

      assertAll(
            () ->  assertEquals("scattered clouds",              tour.getWeather()), //$NON-NLS-1$
            () ->  assertEquals(IWeather.WEATHER_ID_PART_CLOUDS, tour.getWeather_Clouds()),
            () ->  assertEquals(7.51f,                           tour.getWeather_Temperature_Average()),
            () ->  assertEquals(2,                               tour.getWeather_Wind_Speed()),
            () ->  assertEquals(228,                             tour.getWeather_Wind_Direction()),
            () ->  assertEquals(72,                              tour.getWeather_Humidity()),
            () ->  assertEquals(2.29f,                           tour.getWeather_Precipitation()),
            () ->  assertEquals(0,                               tour.getWeather_Snowfall()),
            () ->  assertEquals(1007,                            tour.getWeather_Pressure()),
            () ->  assertEquals(14.04f,                          tour.getWeather_Temperature_Max()),
            () ->  assertEquals(2.84f,                           tour.getWeather_Temperature_Min()),
            () ->  assertEquals(6.57f,                           tour.getWeather_Temperature_WindChill()));

// SET_FORMATTING_ON
   }

   @Test
   void testWeatherRetrieval_JulySixth2022() {

      final TourData tour = Initializer.importTour();
      //Tuesday, July 6, 2022 12:00:00 AM
      final ZonedDateTime zonedDateTime = ZonedDateTime.of(
            2022,
            7,
            6,
            0,
            0,
            0,
            0,
            TimeTools.UTC);
      tour.setTourStartTime(zonedDateTime);
      //We set the current time elapsed to trigger the computation of the new end time
      tour.setTourDeviceTime_Elapsed(tour.getTourDeviceTime_Elapsed());

      openWeatherMapRetriever = new OpenWeatherMapRetriever(tour);

      assertTrue(openWeatherMapRetriever.retrieveHistoricalWeatherData());

// SET_FORMATTING_OFF

      assertAll(
            () ->  assertEquals("light rain",             tour.getWeather()), //$NON-NLS-1$
            () ->  assertEquals(IWeather.WEATHER_ID_RAIN, tour.getWeather_Clouds()),
            () ->  assertEquals(7.92f,                    tour.getWeather_Temperature_Average()),
            () ->  assertEquals(2,                        tour.getWeather_Wind_Speed()),
            () ->  assertEquals(295,                      tour.getWeather_Wind_Direction()),
            () ->  assertEquals(78,                       tour.getWeather_Humidity()),
            () ->  assertEquals(2.45f,                    tour.getWeather_Precipitation()),
            () ->  assertEquals(0,                        tour.getWeather_Snowfall()),
            () ->  assertEquals(1007,                     tour.getWeather_Pressure()),
            () ->  assertEquals(11.79f,                   tour.getWeather_Temperature_Max()),
            () ->  assertEquals(5.89f,                    tour.getWeather_Temperature_Min()),
            () ->  assertEquals(7.75f,                    tour.getWeather_Temperature_WindChill()));

// SET_FORMATTING_ON
   }

   @Test
   void testWeatherRetrieval_March2022() {

      final TourData tour = Initializer.importTour();
      //Tuesday, March 12, 2022 12:00:00 PM
      final ZonedDateTime zonedDateTime = ZonedDateTime.of(
            2022,
            3,
            12,
            12,
            0,
            0,
            0,
            TimeTools.UTC);
      tour.setTourStartTime(zonedDateTime);
      //We set the current time elapsed to trigger the computation of the new end time
      tour.setTourDeviceTime_Elapsed(tour.getTourDeviceTime_Elapsed());

      openWeatherMapRetriever = new OpenWeatherMapRetriever(tour);

      assertTrue(openWeatherMapRetriever.retrieveHistoricalWeatherData());

// SET_FORMATTING_OFF

      assertAll(
            () ->  assertEquals("scattered clouds",              tour.getWeather()), //$NON-NLS-1$
            () ->  assertEquals(IWeather.WEATHER_ID_PART_CLOUDS, tour.getWeather_Clouds()),
            () ->  assertEquals(-5.06f,                          tour.getWeather_Temperature_Average()),
            () ->  assertEquals(9,                               tour.getWeather_Wind_Speed()),
            () ->  assertEquals(282,                             tour.getWeather_Wind_Direction()),
            () ->  assertEquals(52,                              tour.getWeather_Humidity()),
            () ->  assertEquals(1.51f,                           tour.getWeather_Precipitation()),
            () ->  assertEquals(0,                               tour.getWeather_Snowfall()),
            () ->  assertEquals(1023,                            tour.getWeather_Pressure()),
            () ->  assertEquals(-0.36f,                          tour.getWeather_Temperature_Max()),
            () ->  assertEquals(-10.77f,                         tour.getWeather_Temperature_Min()),
            () ->  assertEquals(-9.96f,                          tour.getWeather_Temperature_WindChill()));

// SET_FORMATTING_ON
   }

   @Test
   void weatherData_CurrentWeather() {

      final TourData tour = Initializer.importTour();
      //Set the tour start time to be within the current hour
      final ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.systemDefault());
      tour.setTourStartTime(zonedDateTime);
      tour.setTimeZoneId(ZoneId.systemDefault().getId());
      //We set the current time elapsed to trigger the computation of the new end time
      tour.setTourDeviceTime_Elapsed(tour.getTourDeviceTime_Elapsed());

      openWeatherMapRetriever = new OpenWeatherMapRetriever(tour);

      assertTrue(openWeatherMapRetriever.retrieveHistoricalWeatherData(), "The weather should have been retrieved"); //$NON-NLS-1$
   }

   @Test
   void weatherIconMapping_AllValues() {

      assertAll(
            () -> assertEquals(UI.EMPTY_STRING,
                  OpenWeatherMapRetriever.convertWeatherIconToMTWeatherClouds(UI.SPACE1)),
            () -> assertEquals(IWeather.WEATHER_ID_LIGHTNING,
                  OpenWeatherMapRetriever.convertWeatherIconToMTWeatherClouds("11d")), //$NON-NLS-1$
            () -> assertEquals(IWeather.WEATHER_ID_SCATTERED_SHOWERS,
                  OpenWeatherMapRetriever.convertWeatherIconToMTWeatherClouds("09d")), //$NON-NLS-1$
            () -> assertEquals(IWeather.WEATHER_ID_DRIZZLE,
                  OpenWeatherMapRetriever.convertWeatherIconToMTWeatherClouds("50d")), //$NON-NLS-1$
            () -> assertEquals(IWeather.WEATHER_ID_RAIN,
                  OpenWeatherMapRetriever.convertWeatherIconToMTWeatherClouds("10d")), //$NON-NLS-1$
            () -> assertEquals(IWeather.WEATHER_ID_SNOW,
                  OpenWeatherMapRetriever.convertWeatherIconToMTWeatherClouds("13d")), //$NON-NLS-1$
            () -> assertEquals(IWeather.WEATHER_ID_CLEAR,
                  OpenWeatherMapRetriever.convertWeatherIconToMTWeatherClouds("01d"))); //$NON-NLS-1$
   }
}
