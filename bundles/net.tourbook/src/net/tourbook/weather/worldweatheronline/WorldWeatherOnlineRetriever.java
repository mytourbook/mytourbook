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
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.views.calendar.CalendarProfile;
import net.tourbook.weather.HistoricalWeatherRetriever;
import net.tourbook.weather.WeatherUtils;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;

/**
 * A class that retrieves, for a given track, the historical weather data.
 */
public class WorldWeatherOnlineRetriever extends HistoricalWeatherRetriever {

   private static final String  TOUR_TOOLTIP_FORMAT_DATEWEEKTIME = net.tourbook.ui.Messages.Tour_Tooltip_Format_DateWeekTime;

   private static final String  SYS_PROP__LOG_WEATHER_DATA       = "logWeatherData";                                         //$NON-NLS-1$
   private static final boolean _isLogWeatherData                = System.getProperty(SYS_PROP__LOG_WEATHER_DATA) != null;

   static {

      if (_isLogWeatherData) {

         Util.logSystemProperty_IsEnabled(CalendarProfile.class,
               SYS_PROP__LOG_WEATHER_DATA,
               "Weather data is logged"); //$NON-NLS-1$
      }
   }

   private static final String    baseApiUrl   = "http://api.worldweatheronline.com/premium/v1/past-weather.ashx"; //$NON-NLS-1$
   private static final String    keyParameter = "?key=";                                                          //$NON-NLS-1$

   private String                 endDate;
   private LatLng                 searchAreaCenter;
   private String                 startDate;
   private long                   tourEndTime;
   private long                   tourMiddleTime;
   private long                   tourStartTime;

   private final IPreferenceStore prefStore    = TourbookPlugin.getPrefStore();
   private Data                   weatherData  = null;

   /*
    * @param tour
    * The tour for which we need to retrieve the weather data.
    */
   public WorldWeatherOnlineRetriever(final TourData tourData) {

      super(tourData);

      searchAreaCenter = WeatherUtils.determineWeatherSearchAreaCenter(tour);
      startDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(tour.getTourStartTime()); //$NON-NLS-1$
      endDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(tour.getTourStartTime().plusSeconds(tour.getTourDeviceTime_Elapsed())); //$NON-NLS-1$

      tourStartTime = tour.getTourStartTimeMS() / 1000;
      tourEndTime = tour.getTourEndTimeMS() / 1000;
      tourMiddleTime = tourStartTime + ((tourEndTime - tourStartTime) / 2);
   }

   public static String getApiUrl() {
      return baseApiUrl + keyParameter;
   }

   public static String getBaseApiUrl() {
      return baseApiUrl;
   }

   @Override
   protected String buildFullWeatherDataString() {

      final List<String> fullWeatherDataList = new ArrayList<>();

      for (final Hourly hourly : weatherData.getFilteredHourly()) {

         final long hourlyEpochSeconds = hourly.getEpochSeconds();
         final TourDateTime tourDateTime = TimeTools.createTourDateTime(hourlyEpochSeconds * 1000L, tour.getTimeZoneId());

         final String fullWeatherData = WeatherUtils.buildFullWeatherDataString(
               hourly.getTempC(),
               hourly.getFeelsLikeC(),
               hourly.getWindspeedKmph(),
               hourly.getWinddirDegree(),
               hourly.getHumidity(),
               hourly.getPrecipMM(),
               0,
               tourDateTime);

         fullWeatherDataList.add(fullWeatherData);
      }

      //Adding the weather station information
      final List<NearestArea> nearestArea = weatherData.getNearestArea();
      if (nearestArea != null && nearestArea.size() > 0) {

         final NearestArea firstNearestArea = nearestArea.get(0);

         String weatherStationName = UI.EMPTY_STRING;
         if (firstNearestArea.getAreaName() != null && firstNearestArea.getAreaName().size() > 0) {
            weatherStationName = firstNearestArea.getAreaName().get(0).getValue();
         }

         final LatLng weatherStationCoordinates = new LatLng(
               Double.valueOf(firstNearestArea.getLatitude()),
               Double.valueOf(firstNearestArea.getLongitude()));

         final float distanceFromTour = Math.round(
               LatLngTool.distance(
                     searchAreaCenter,
                     weatherStationCoordinates,
                     LengthUnit.METER)
                     / UI.UNIT_VALUE_DISTANCE / 1000);

         String weatherStationLink = UI.EMPTY_STRING;
         if (firstNearestArea.getWeatherUrl() != null && firstNearestArea.getWeatherUrl().size() > 0) {
            weatherStationLink = firstNearestArea.getWeatherUrl().get(0).getValue();
         }

         fullWeatherDataList.add(NLS.bind(
               Messages.Log_HistoricalWeatherRetriever_001_WeatherData_WeatherStation_Link,
               new Object[] { weatherStationLink,
                     weatherStationName,
                     distanceFromTour + UI.UNIT_LABEL_DISTANCE }));
      }

      final String fullWeatherData = String.join(
            net.tourbook.ui.UI.SYSTEM_NEW_LINE,
            fullWeatherDataList);

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

         uriBuilder.setParameter("key", prefStore.getString(ITourbookPreferences.WEATHER_API_KEY)); //$NON-NLS-1$
         uriBuilder.setParameter("q", searchAreaCenter.getLatitude() + "," + searchAreaCenter.getLongitude()); //$NON-NLS-1$ //$NON-NLS-2$
         uriBuilder.setParameter("date", startDate); //$NON-NLS-1$
         //tp=1 : Specifies the weather forecast time interval in hours. Here, every 1 hour
         uriBuilder.setParameter("tp", "1"); //$NON-NLS-1$ //$NON-NLS-2$
         uriBuilder.setParameter("format", "json"); //$NON-NLS-1$ //$NON-NLS-2$
         uriBuilder.setParameter("includelocation", "yes"); //$NON-NLS-1$ //$NON-NLS-2$
         uriBuilder.setParameter("extra", "utcDateTime"); //$NON-NLS-1$ //$NON-NLS-2$

         //If the tour finishes a different day, we need to specify the ending date
         if (!endDate.equals(startDate)) {
            uriBuilder.setParameter("enddate", endDate); //$NON-NLS-1$
         }

         weatherRequestWithParameters = uriBuilder.build().toString();

         return weatherRequestWithParameters;

      } catch (final URISyntaxException e) {

         StatusUtil.logError(
               "WorldWeatherOnlineRetriever.buildWeatherApiRequest : Error while " + //$NON-NLS-1$
                     "building the historical weather request :" //$NON-NLS-1$
                     + e.getMessage());
         return UI.EMPTY_STRING;
      }
   }

   /**
    * Retrieves the historical weather data
    *
    * @return The weather data, if found.
    */
   @Override
   public boolean retrieveHistoricalWeatherData() {

      final String weatherRequestWithParameters = buildWeatherApiRequest();

      final String rawWeatherData = sendWeatherApiRequest(weatherRequestWithParameters);
      if (StringUtils.isNullOrEmpty(rawWeatherData)) {
         return false;
      }

      weatherData = serializeWeatherData(rawWeatherData);
      if (weatherData == null) {
         return false;
      }

      final List<Weather> weather = weatherData.getWeather();
      if (weather == null ||
            weather.size() == 0 ||
            weather.get(0).getHourly() == null ||
            weather.get(0).getHourly().size() == 0) {
         return false;
      }

   // SET_FORMATTING_OFF

      weatherData.filterHourlyData(tourStartTime, tourEndTime);

      tour.setIsWeatherDataFromProvider(true);

      //We look for the weather data in the middle of the tour to populate the weather conditions
      weatherData.findMiddleHourly(tourMiddleTime);
      tour.setWeather(                       weatherData.getWeatherDescription());
      tour.setWeather_Clouds(                weatherData.getWeatherType());

      tour.setWeather_Temperature_Average(   weatherData.getTemperatureAverage());
      tour.setWeather_Wind_Speed(            weatherData.getAverageWindSpeed());
      tour.setWeather_Wind_Direction(        weatherData.getAverageWindDirection());
      tour.setWeather_Humidity(              weatherData.getAverageHumidity());
      tour.setWeather_Precipitation(         weatherData.getAveragePrecipitation());
      tour.setWeather_Pressure(              weatherData.getAveragePressure());
      tour.setWeather_Temperature_Max(       weatherData.getTemperatureMax());
      tour.setWeather_Temperature_Min(       weatherData.getTemperatureMin());
      tour.setWeather_Temperature_WindChill( weatherData.getAverageWindChill());

// SET_FORMATTING_ON

      return true;
   }

   /**
    * Serialized a JSON weather data object into a WeatherData object.
    *
    * @param weatherDataResponse
    *           A string containing a historical weather data JSON object.
    * @return The serialized weather data.
    */
   private Data serializeWeatherData(final String weatherDataResponse) {

      if (_isLogWeatherData) {

         final long elapsedTime = tour.getTourDeviceTime_Elapsed();
         final ZonedDateTime zdtTourStart = tour.getTourStartTime();
         final ZonedDateTime zdtTourEnd = zdtTourStart.plusSeconds(elapsedTime);
         final String tourTitle = tour.getTourTitle();

         System.out.println();

         if (tourTitle.length() > 0) {
            System.out.println(tourTitle);
         }

         System.out.println(String.format(TOUR_TOOLTIP_FORMAT_DATEWEEKTIME,
               zdtTourStart.format(TimeTools.Formatter_Date_F),
               zdtTourStart.format(TimeTools.Formatter_Time_M),
               zdtTourEnd.format(TimeTools.Formatter_Time_M),
               zdtTourStart.get(TimeTools.calendarWeek.weekOfWeekBasedYear())));

         System.out.println(weatherDataResponse);
      }

      Data serializedWeatherData = new Data();
      try {

         //weather
         final ObjectMapper mapper = new ObjectMapper();
         final String weatherResults = mapper.readValue(weatherDataResponse, JsonNode.class)
               .get("data") //$NON-NLS-1$
               .toString();

         serializedWeatherData = mapper.readValue(weatherResults, new TypeReference<Data>() {});

      } catch (final Exception e) {
         StatusUtil.logError(
               "WorldWeatherOnlineRetriever.serializeWeatherData : Error while parsing the historical weather JSON object :" //$NON-NLS-1$
                     + weatherDataResponse + "\n" + e.getMessage()); //$NON-NLS-1$
         return null;
      }

      return serializedWeatherData;
   }

}
