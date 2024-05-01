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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;
import net.tourbook.weather.WeatherUtils;
import net.tourbook.weather.openweathermap.OpenWeatherMapRetriever;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;
import utils.Initializer;

/**
 * Regression tests for the weather retrieval from OpenWeatherMap.
 */
public class OpenWeatherMapRetrieverTests {

   private static final String OPENWEATHERMAP_CURRENT_BASE_URL                     = WeatherUtils.OAUTH_PASSEUR_APP_URL
         + "/openweathermap/3.0/current?units=metric&lat=40.263996&lon=-105.58854099999999&lang=en&dt=";               //$NON-NLS-1$
   private static final String OPENWEATHERMAP_TIMEMACHINE_BASE_URL                 = WeatherUtils.OAUTH_PASSEUR_APP_URL
         + "/openweathermap/3.0/timemachine?units=metric&lat=40.263996&lon=-105.58854099999999&lang=en&dt=";           //$NON-NLS-1$
   private static final String OPENWEATHERMAP_AIRPOLLUTION_BASE_URL                = WeatherUtils.OAUTH_PASSEUR_APP_URL
         + "/openweathermap/air_pollution?lat=40.263996&lon=-105.58854099999999&start=%s&end=%s";                      //$NON-NLS-1$

   private static final String OPENWEATHERMAP_FILE_PATH                            =
         FilesUtils.rootPath + "data/weather/openweathermap/files/";                                                   //$NON-NLS-1$

   private static final String OPENWEATHERMAP_RESPONSE_BASE_FILE_PATH              =
         OPENWEATHERMAP_FILE_PATH + "LongsPeak-Manual-OpenWeatherMapResponse-%s.json";                                 //$NON-NLS-1$
   private static final String OPENWEATHERMAP_CURRENT_RESPONSE_BASE_FILE_PATH      =
         OPENWEATHERMAP_FILE_PATH + "LongsPeak-Manual-OpenWeatherMapCurrentResponse-%s.json";                          //$NON-NLS-1$
   private static final String OPENWEATHERMAP_AIRPOLLUTION_RESPONSE_BASE_FILE_PATH =
         OPENWEATHERMAP_FILE_PATH + "LongsPeak-Manual-OpenWeatherMapPollutionResponse-%sto%s.json";                    //$NON-NLS-1$

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

   private List<String> arrangeUrls(final List<String> timeStamps) {

      final List<String> urls = new ArrayList<>();

      timeStamps.forEach(timeStamp -> {

         final String openWeatherMapResponse = Comparison.readFileContent(String.format(OPENWEATHERMAP_RESPONSE_BASE_FILE_PATH, timeStamp));

         final String timeMachineUrl = OPENWEATHERMAP_TIMEMACHINE_BASE_URL + timeStamp;
         urls.add(timeMachineUrl);
         httpClientMock.onGet(timeMachineUrl).doReturn(openWeatherMapResponse);
      });

      final String openWeatherMapAirPollutionResponse = Comparison.readFileContent(String.format(OPENWEATHERMAP_AIRPOLLUTION_RESPONSE_BASE_FILE_PATH,
            timeStamps.get(0),
            timeStamps.get(timeStamps.size() - 1)));
      final String airPollutionUrl = String.format(OPENWEATHERMAP_AIRPOLLUTION_BASE_URL, timeStamps.get(0), timeStamps.get(timeStamps.size() - 1));
      urls.add(airPollutionUrl);
      httpClientMock.onGet(airPollutionUrl).doReturn(openWeatherMapAirPollutionResponse);

      return urls;
   }

   @Test
   void testWeatherRetrieval_JulySecond2022() {

      final List<String> timeStamps = Arrays.asList("1656720000", //$NON-NLS-1$
            "1656723600", //$NON-NLS-1$
            "1656727200", //$NON-NLS-1$
            "1656730800", //$NON-NLS-1$
            "1656734400", //$NON-NLS-1$
            "1656738000", //$NON-NLS-1$
            "1656741600", //$NON-NLS-1$
            "1656745200", //$NON-NLS-1$
            "1656748800", //$NON-NLS-1$
            "1656752400", //$NON-NLS-1$
            "1656756000", //$NON-NLS-1$
            "1656759600", //$NON-NLS-1$
            "1656763200"); //$NON-NLS-1$

      final List<String> urls = arrangeUrls(timeStamps);

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

      openWeatherMapRetriever = new OpenWeatherMapRetriever();
      openWeatherMapRetriever.setTourData(tour);

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
            () ->  assertEquals(6.57f,                           tour.getWeather_Temperature_WindChill()),
            () ->  assertEquals(IWeather.AIRQUALITY_ID_GOOD,     tour.getWeather_AirQuality()));

// SET_FORMATTING_ON
   }

   @Test
   void testWeatherRetrieval_JulySixth2022() {

      final List<String> timeStamps = Arrays.asList("1657065600", //$NON-NLS-1$
            "1657069200", //$NON-NLS-1$
            "1657072800", //$NON-NLS-1$
            "1657076400", //$NON-NLS-1$
            "1657080000", //$NON-NLS-1$
            "1657083600", //$NON-NLS-1$
            "1657087200", //$NON-NLS-1$
            "1657090800", //$NON-NLS-1$
            "1657094400", //$NON-NLS-1$
            "1657098000", //$NON-NLS-1$
            "1657101600", //$NON-NLS-1$
            "1657105200", //$NON-NLS-1$
            "1657108800"//$NON-NLS-1$
      );
      final List<String> urls = arrangeUrls(timeStamps);

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

      openWeatherMapRetriever = new OpenWeatherMapRetriever();
      openWeatherMapRetriever.setTourData(tour);
      assertTrue(openWeatherMapRetriever.retrieveHistoricalWeatherData());

      urls.forEach(url -> httpClientMock.verify().get(url).called());

// SET_FORMATTING_OFF

      assertAll(
            () ->  assertEquals("light rain",                tour.getWeather()), //$NON-NLS-1$
            () ->  assertEquals(IWeather.WEATHER_ID_RAIN,    tour.getWeather_Clouds()),
            () ->  assertEquals(7.92f,                       tour.getWeather_Temperature_Average()),
            () ->  assertEquals(2,                           tour.getWeather_Wind_Speed()),
            () ->  assertEquals(295,                         tour.getWeather_Wind_Direction()),
            () ->  assertEquals(78,                          tour.getWeather_Humidity()),
            () ->  assertEquals(2.45f,                       tour.getWeather_Precipitation()),
            () ->  assertEquals(0,                           tour.getWeather_Snowfall()),
            () ->  assertEquals(1007,                        tour.getWeather_Pressure()),
            () ->  assertEquals(11.79f,                      tour.getWeather_Temperature_Max()),
            () ->  assertEquals(5.89f,                       tour.getWeather_Temperature_Min()),
            () ->  assertEquals(7.75f,                       tour.getWeather_Temperature_WindChill()),
            () ->  assertEquals(IWeather.AIRQUALITY_ID_GOOD, tour.getWeather_AirQuality()));

// SET_FORMATTING_ON
   }

   @Test
   void testWeatherRetrieval_March2022() {

      final List<String> timeStamps = Arrays.asList("1647086400", //$NON-NLS-1$
            "1647090000", //$NON-NLS-1$
            "1647093600", //$NON-NLS-1$
            "1647097200", //$NON-NLS-1$
            "1647100800", //$NON-NLS-1$
            "1647104400", //$NON-NLS-1$
            "1647108000", //$NON-NLS-1$
            "1647111600", //$NON-NLS-1$
            "1647115200", //$NON-NLS-1$
            "1647118800", //$NON-NLS-1$
            "1647122400", //$NON-NLS-1$
            "1647126000", //$NON-NLS-1$
            "1647129600"//$NON-NLS-1$
      );
      final List<String> urls = arrangeUrls(timeStamps);

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

      openWeatherMapRetriever = new OpenWeatherMapRetriever();
      openWeatherMapRetriever.setTourData(tour);
      assertTrue(openWeatherMapRetriever.retrieveHistoricalWeatherData());

      urls.forEach(url -> httpClientMock.verify().get(url).called());

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
            () ->  assertEquals(-9.96f,                          tour.getWeather_Temperature_WindChill()),
            () ->  assertEquals(IWeather.AIRQUALITY_ID_FAIR,     tour.getWeather_AirQuality()));

// SET_FORMATTING_ON
   }

   @Test
   void weatherData_CurrentWeather() {

      final String openWeatherMapResponse = Comparison.readFileContent(String.format(OPENWEATHERMAP_CURRENT_RESPONSE_BASE_FILE_PATH, 1656720000));

      final TourData tour = Initializer.importTour();
      //Set the tour start time to be within the current hour
      final ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.systemDefault());
      tour.setTourStartTime(zonedDateTime);
      tour.setTimeZoneId(ZoneId.systemDefault().getId());
      //We set the current time elapsed to trigger the computation of the new end time
      tour.setTourDeviceTime_Elapsed(tour.getTourDeviceTime_Elapsed());

      final String currentUrl = OPENWEATHERMAP_CURRENT_BASE_URL + tour.getTourStartTimeMS() / 1000;
      httpClientMock.onGet(currentUrl).doReturn(openWeatherMapResponse);
      final List<String> urls = new ArrayList<>();
      urls.add(currentUrl);
      final String openWeatherMapAirPollutionResponse = Comparison.readFileContent(String.format(OPENWEATHERMAP_AIRPOLLUTION_RESPONSE_BASE_FILE_PATH,
            1714150800,
            1714190400));
      final long tourStart = DateUtils.round(Date.from(tour.getTourStartTime().toInstant()), Calendar.HOUR).toInstant().getEpochSecond();
      final long tourEnd = DateUtils.round(Date.from(tour.getTourStartTime().plus(tour.getTourDeviceTime_Elapsed(), ChronoUnit.SECONDS).toInstant()),
            Calendar.HOUR)
            .toInstant().getEpochSecond();
      final String airPollutionUrl = String.format(OPENWEATHERMAP_AIRPOLLUTION_BASE_URL,
            tourStart,
            tourEnd);
      urls.add(airPollutionUrl);
      httpClientMock.onGet(airPollutionUrl).doReturn(openWeatherMapAirPollutionResponse);

      openWeatherMapRetriever = new OpenWeatherMapRetriever();
      openWeatherMapRetriever.setTourData(tour);
      assertTrue(openWeatherMapRetriever.retrieveHistoricalWeatherData(), "The weather should have been retrieved"); //$NON-NLS-1$
      urls.forEach(url -> httpClientMock.verify().get(url).called());
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
