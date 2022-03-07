/*******************************************************************************
 * Copyright (C) 2019, 2022 Frédéric Bard
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
package net.tourbook.weather.worldweatheronline;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javadocmd.simplelatlng.LatLng;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.views.calendar.CalendarProfile;
import net.tourbook.weather.HistoricalWeatherRetriever;
import net.tourbook.weather.WeatherUtils;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * A class that retrieves, for a given track, the historical weather data.
 */
public class WorldWeatherOnlineRetriever extends HistoricalWeatherRetriever {

   private static final String  SYS_PROP__LOG_WEATHER_DATA = "logWeatherData";                                      //$NON-NLS-1$
   private static final boolean _isLogWeatherData          = System.getProperty(SYS_PROP__LOG_WEATHER_DATA) != null;

   static {

      if (_isLogWeatherData) {

         Util.logSystemProperty_IsEnabled(CalendarProfile.class,
               SYS_PROP__LOG_WEATHER_DATA,
               "Weather data is logged"); //$NON-NLS-1$
      }
   }

   private static final String    baseApiUrl      = "http://api.worldweatheronline.com/premium/v1/past-weather.ashx"; //$NON-NLS-1$
   private static final String    keyParameter    = "?key=";                                                          //$NON-NLS-1$
   private LatLng                 searchAreaCenter;
   private String                 startDate;
   private String                 startTime;

   private String                 endTime;

   private WeatherData            historicalWeatherData;

   private final IPreferenceStore _prefStore      = TourbookPlugin.getPrefStore();
   private List<WWOHourlyResults> _rawWeatherData = null;

   /*
    * @param tour
    * The tour for which we need to retrieve the weather data.
    */
   public WorldWeatherOnlineRetriever(final TourData tourData) {

      super(tourData);

      searchAreaCenter = WeatherUtils.determineWeatherSearchAreaCenter(tour);
      startDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(tour.getTourStartTime()); //$NON-NLS-1$

      final double roundedStartTime = tour.getTourStartTime().getHour();
      startTime = (int) roundedStartTime + "00"; //$NON-NLS-1$

      int roundedEndHour = Instant.ofEpochMilli(tour.getTourEndTimeMS()).atZone(tour.getTimeZoneIdWithDefault()).getHour();
      final int roundedEndMinutes = Instant.ofEpochMilli(tour.getTourEndTimeMS()).atZone(tour.getTimeZoneIdWithDefault()).getMinute();
      if (roundedEndMinutes >= 30) {
         ++roundedEndHour;
      }
      endTime = roundedEndHour + "00"; //$NON-NLS-1$
   }

   public static String getApiUrl() {
      return baseApiUrl + keyParameter;
   }

   public static String getBaseApiUrl() {
      return baseApiUrl;
   }

   @Override
   protected String buildFullWeatherDataString() {

      final long tourStartTime = tour.getTourStartTimeMS() / 1000;
      final long tourEndTime = tour.getTourEndTimeMS() / 1000;
      final long thirtyMinutes = 1800;

      final List<String> fullWeatherDataList = new ArrayList<>();

      for (final WWOHourlyResults hourly : _rawWeatherData) {

         final long hourlyEpochSeconds = hourly.getEpochSeconds(tour.getTimeZoneIdWithDefault());
         if (hourlyEpochSeconds < tourStartTime - thirtyMinutes ||
               hourlyEpochSeconds > tourEndTime + thirtyMinutes) {
            continue;
         }

         final TourDateTime tourDateTime = TimeTools.createTourDateTime(hourlyEpochSeconds * 1000L, tour.getTimeZoneId());
         final String fullWeatherData = WeatherUtils.buildFullWeatherDataString(
               hourly.getTempC(),
               hourly.getFeelsLikeC(),
               hourly.getWindspeedKmph(),
               hourly.getWinddirDegree(),
               hourly.getHumidity(),
               hourly.getPrecipMM(),
               tourDateTime.tourZonedDateTime.getHour(),
               tour.getTimeZoneId());

         fullWeatherDataList.add(fullWeatherData);
      }

      final String fullWeatherData = String.join(UI.COMMA_SPACE, fullWeatherDataList);

      return fullWeatherData;
   }

   private String buildWeatherApiRequest() {

      String weatherRequestWithParameters = UI.EMPTY_STRING;

      try {
         final URI apiUri = new URI(baseApiUrl);

         final URIBuilder uriBuilder = new URIBuilder()
               .setScheme(apiUri.getScheme())
               .setHost(apiUri.getHost())
               .setPath(apiUri.getPath());

         uriBuilder.setParameter("key", _prefStore.getString(ITourbookPreferences.WEATHER_API_KEY)); //$NON-NLS-1$
         uriBuilder.setParameter("q", searchAreaCenter.getLatitude() + "," + searchAreaCenter.getLongitude()); //$NON-NLS-1$ //$NON-NLS-2$
         uriBuilder.setParameter("date", startDate); //$NON-NLS-1$
         //tp=1 : Specifies the weather forecast time interval in hours. Here, every 1 hour
         uriBuilder.setParameter("tp", "1"); //$NON-NLS-1$ //$NON-NLS-2$
         uriBuilder.setParameter("format", "json"); //$NON-NLS-1$ //$NON-NLS-2$
         uriBuilder.setParameter("includelocation", "yes"); //$NON-NLS-1$ //$NON-NLS-2$
         uriBuilder.setParameter("extra", "utcDateTime"); //$NON-NLS-1$ //$NON-NLS-2$

         weatherRequestWithParameters = uriBuilder.build().toString();

         return weatherRequestWithParameters;

      } catch (final URISyntaxException e) {

         StatusUtil.logError(
               "OpenWeatherMapRetriever.buildWeatherApiRequest : Error while " + //$NON-NLS-1$
                     "building the historical weather request :" //$NON-NLS-1$
                     + e.getMessage());
         return UI.EMPTY_STRING;
      }
   }

   private void computeFinalWeatherData(final WeatherData weatherData,
                                        final List<WWOHourlyResults> rawWeatherData) {

      boolean isTourStartData = false;
      boolean isTourEndData = false;
      int numHourlyDatasets = 0;
      int sumHumidity = 0;
      int sumPressure = 0;
      float sumPrecipitation = 0f;
      int sumWindChill = 0;
      int sumWindDirection = 0;
      int sumWindSpeed = 0;
      int sumTemperature = 0;
      int maxTemperature = Integer.MIN_VALUE;
      int minTemperature = Integer.MAX_VALUE;

      for (int index = 0; index < rawWeatherData.size() && !isTourEndData; index++) {

         final WWOHourlyResults hourlyData = rawWeatherData.get(index);

         // Within the hourly data, find the times that corresponds to the tour time
         // and extract all the weather data.
         if (hourlyData.gettime().equals(startTime)) {
            isTourStartData = true;
            weatherData.setWeatherDescription(hourlyData.getWeatherDescription());
            weatherData.setWeatherType(hourlyData.getWeatherCode());
         }
         if (hourlyData.gettime().equals(endTime)) {
            isTourEndData = true;
         }

         if (isTourStartData || isTourEndData) {

            sumWindDirection += hourlyData.getWinddirDegree();
            sumWindSpeed += hourlyData.getWindspeedKmph();
            sumHumidity += hourlyData.getHumidity();
            sumPrecipitation += hourlyData.getPrecipMM();
            sumPressure += hourlyData.getPressure();
            sumWindChill += hourlyData.getFeelsLikeC();
            sumTemperature += hourlyData.getTempC();

            if (hourlyData.getTempC() < minTemperature) {
               minTemperature = hourlyData.getTempC();
            }

            if (hourlyData.getTempC() > maxTemperature) {
               maxTemperature = hourlyData.getTempC();
            }

            ++numHourlyDatasets;
         }
      }

      weatherData.setWindDirection((int) Math.ceil((double) sumWindDirection / (double) numHourlyDatasets));
      weatherData.setWindSpeed((int) Math.ceil((double) sumWindSpeed / (double) numHourlyDatasets));
      weatherData.setTemperatureMax(maxTemperature);
      weatherData.setTemperatureMin(minTemperature);
      weatherData.setTemperatureAverage((int) Math.ceil((double) sumTemperature / (double) numHourlyDatasets));
      weatherData.setWindChill((int) Math.ceil((double) sumWindChill / (double) numHourlyDatasets));
      weatherData.setAverageHumidity((int) Math.ceil((double) sumHumidity / (double) numHourlyDatasets));
      weatherData.setAveragePressure((int) Math.ceil((double) sumPressure / (double) numHourlyDatasets));
      weatherData.setPrecipitation(sumPrecipitation);
   }

   /**
    * Parses a JSON weather data object into a WeatherData object.
    *
    * @param weatherDataResponse
    *           A string containing a historical weather data JSON object.
    * @return The parsed weather data.
    */
   private WeatherData parseWeatherData(final String weatherDataResponse) {

      if (_isLogWeatherData) {

         final long elapsedTime = tour.getTourDeviceTime_Elapsed();
         final ZonedDateTime zdtTourStart = tour.getTourStartTime();
         final ZonedDateTime zdtTourEnd = zdtTourStart.plusSeconds(elapsedTime);
         final String tourTitle = tour.getTourTitle();

         System.out.println();

         if (tourTitle.length() > 0) {
            System.out.println(tourTitle);
         }

         System.out.println(String.format(net.tourbook.ui.Messages.Tour_Tooltip_Format_DateWeekTime,
               zdtTourStart.format(TimeTools.Formatter_Date_F),
               zdtTourStart.format(TimeTools.Formatter_Time_M),
               zdtTourEnd.format(TimeTools.Formatter_Time_M),
               zdtTourStart.get(TimeTools.calendarWeek.weekOfWeekBasedYear())));

         System.out.println(weatherDataResponse);
      }

      final WeatherData weatherData = new WeatherData();
      try {
         final ObjectMapper mapper = new ObjectMapper();
         final String weatherResults = mapper.readValue(weatherDataResponse, JsonNode.class)
               .get("data") //$NON-NLS-1$
               .get("weather") //$NON-NLS-1$
               .get(0)
               .get("hourly") //$NON-NLS-1$
               .toString();

         _rawWeatherData = mapper.readValue(weatherResults, new TypeReference<List<WWOHourlyResults>>() {});

         computeFinalWeatherData(weatherData, _rawWeatherData);

      } catch (final Exception e) {
         StatusUtil.logError(
               "WeatherHistoryRetriever.parseWeatherData : Error while parsing the historical weather JSON object :" //$NON-NLS-1$
                     + weatherDataResponse + "\n" + e.getMessage()); //$NON-NLS-1$
         return null;
      }

      return weatherData;
   }

   /**
    * Retrieves the historical weather data
    *
    * @return The weather data, if found.
    */
   @Override
   public boolean retrieveHistoricalWeatherData() {

      final String weatherRequestWithParameters = buildWeatherApiRequest();

      final String rawWeatherData = super.sendWeatherApiRequest(weatherRequestWithParameters);
      if (!rawWeatherData.contains("weather")) { //$NON-NLS-1$
         return false;
      }

      historicalWeatherData = parseWeatherData(rawWeatherData);

      if (historicalWeatherData == null) {
         return false;
      }

      tour.setIsWeatherDataFromProvider(true);
      tour.setWeather_Temperature_Average(historicalWeatherData.getTemperatureAverage());
      tour.setWeather_Wind_Speed(historicalWeatherData.getWindSpeed());
      tour.setWeather_Wind_Direction(historicalWeatherData.getWindDirection());
      tour.setWeather(historicalWeatherData.getWeatherDescription());
      tour.setWeather_Clouds(historicalWeatherData.getWeatherType());

      tour.setWeather_Humidity((short) historicalWeatherData.getAverageHumidity());
      tour.setWeather_Precipitation(historicalWeatherData.getPrecipitation());
      tour.setWeather_Pressure((short) historicalWeatherData.getAveragePressure());
      tour.setWeather_Temperature_Max(historicalWeatherData.getTemperatureMax());
      tour.setWeather_Temperature_Min(historicalWeatherData.getTemperatureMin());
      tour.setWeather_Temperature_WindChill(historicalWeatherData.getWindChill());

      return true;
   }

}
