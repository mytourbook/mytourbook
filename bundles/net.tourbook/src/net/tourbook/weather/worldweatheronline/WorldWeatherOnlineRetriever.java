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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.Messages;
import net.tourbook.ui.views.calendar.CalendarProfile;
import net.tourbook.weather.HistoricalWeatherRetriever;
import net.tourbook.weather.WeatherUtils;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * A class that retrieves, for a given track, the historical weather data.
 */
public class WorldWeatherOnlineRetriever extends HistoricalWeatherRetriever {

   private static final String  SYS_PROP__LOG_WEATHER_DATA = "logWeatherData";                                                      //$NON-NLS-1$
   private static final boolean _isLogWeatherData          = System.getProperty(SYS_PROP__LOG_WEATHER_DATA) != null;

   public static HttpClient     httpClient                 = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

   static {

      if (_isLogWeatherData) {

         Util.logSystemProperty_IsEnabled(CalendarProfile.class,
               SYS_PROP__LOG_WEATHER_DATA,
               "Weather data is logged"); //$NON-NLS-1$
      }
   }

   private static final String    baseApiUrl   = "http://api.worldweatheronline.com/premium/v1/past-weather.ashx"; //$NON-NLS-1$
   private static final String    keyParameter = "?key=";                                                          //$NON-NLS-1$
   private LatLng                 searchAreaCenter;
   private String                 startDate;
   private String                 startTime;

   private String                 endTime;

   private WeatherData            historicalWeatherData;

   private final IPreferenceStore _prefStore   = TourbookPlugin.getPrefStore();

   /*
    * @param tour
    * The tour for which we need to retrieve the weather data.
    */
   public WorldWeatherOnlineRetriever(final TourData tourData) {

      super(tourData);

      searchAreaCenter = WeatherUtils.determineWeatherSearchAreaCenter(_tour);
      startDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(_tour.getTourStartTime()); //$NON-NLS-1$

      final double roundedStartTime = _tour.getTourStartTime().getHour();
      startTime = (int) roundedStartTime + "00"; //$NON-NLS-1$

      int roundedEndHour = Instant.ofEpochMilli(_tour.getTourEndTimeMS()).atZone(_tour.getTimeZoneIdWithDefault()).getHour();
      final int roundedEndMinutes = Instant.ofEpochMilli(_tour.getTourEndTimeMS()).atZone(_tour.getTimeZoneIdWithDefault()).getMinute();
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

         final long elapsedTime = _tour.getTourDeviceTime_Elapsed();
         final ZonedDateTime zdtTourStart = _tour.getTourStartTime();
         final ZonedDateTime zdtTourEnd = zdtTourStart.plusSeconds(elapsedTime);
         final String tourTitle = _tour.getTourTitle();

         System.out.println();

         if (tourTitle.length() > 0) {
            System.out.println(tourTitle);
         }

         System.out.println(String.format(Messages.Tour_Tooltip_Format_DateWeekTime,
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

         final List<WWOHourlyResults> rawWeatherData = mapper.readValue(weatherResults, new TypeReference<List<WWOHourlyResults>>() {});

         computeFinalWeatherData(weatherData, rawWeatherData);

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

      final String rawWeatherData = sendWeatherApiRequest();
      if (!rawWeatherData.contains("weather")) { //$NON-NLS-1$
         return false;
      }

      historicalWeatherData = parseWeatherData(rawWeatherData);

      if (historicalWeatherData == null) {
         return false;
      }

      _tour.setIsWeatherDataFromApi(true);
      _tour.setWeather_Temperature_Average(historicalWeatherData.getTemperatureAverage());
      _tour.setWeatherWindSpeed(historicalWeatherData.getWindSpeed());
      _tour.setWeatherWindDir(historicalWeatherData.getWindDirection());
      _tour.setWeather(historicalWeatherData.getWeatherDescription());
      _tour.setWeatherClouds(historicalWeatherData.getWeatherType());

      _tour.setWeather_Humidity((short) historicalWeatherData.getAverageHumidity());
      _tour.setWeather_Precipitation(historicalWeatherData.getPrecipitation());
      _tour.setWeather_Pressure((short) historicalWeatherData.getAveragePressure());
      _tour.setWeather_Temperature_Max(historicalWeatherData.getTemperatureMax());
      _tour.setWeather_Temperature_Min(historicalWeatherData.getTemperatureMin());
      _tour.setWeather_Temperature_WindChill(historicalWeatherData.getWindChill());

      return true;
   }

   /**
    * Processes a query against the weather API.
    *
    * @return The result of the weather API query.
    */
   private String sendWeatherApiRequest() {

      String weatherRequestWithParameters = UI.EMPTY_STRING;
      String weatherHistory = UI.EMPTY_STRING;

      try {
         final URI apiUri = new URI(baseApiUrl);

         final URIBuilder uriBuilder = new URIBuilder()
               .setScheme(apiUri.getScheme())
               .setHost(apiUri.getHost())
               .setPath(apiUri.getPath());

         uriBuilder.setParameter("key", _prefStore.getString(ITourbookPreferences.WEATHER_API_KEY));
         uriBuilder.setParameter("q", searchAreaCenter.getLatitude() + "," + searchAreaCenter.getLongitude());
         uriBuilder.setParameter("date", startDate);
         //tp=1 : Specifies the weather forecast time interval in hours. Here, every 1 hour
         uriBuilder.setParameter("tp", "1");
         uriBuilder.setParameter("format", "json");
         uriBuilder.setParameter("includelocation", "yes");
         uriBuilder.setParameter("extra", "utcDateTime");

         weatherRequestWithParameters = uriBuilder.build().toString();

         // NOTE :
         // This error below keeps popping up RANDOMLY and as of today, I haven't found a solution:
         // java.lang.NoClassDefFoundError: Could not initialize class sun.security.ssl.SSLContextImpl$CustomizedTLSContext
         // 2019/06/20 : To avoid this issue, we are using the HTTP address of WWO and not the HTTPS.

         final HttpRequest request = HttpRequest.newBuilder(URI.create(weatherRequestWithParameters)).GET().build();

         final HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

         weatherHistory = response.body();

      } catch (final Exception ex) {
         StatusUtil.logError(
               "WeatherHistoryRetriever.processRequest : Error while executing the historical weather request with the parameters " //$NON-NLS-1$
                     + weatherRequestWithParameters + "\n" + ex.getMessage()); //$NON-NLS-1$
         Thread.currentThread().interrupt();
         return UI.EMPTY_STRING;
      }

      return weatherHistory;
   }

}
