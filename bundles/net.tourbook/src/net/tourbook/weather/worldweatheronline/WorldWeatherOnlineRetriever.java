/*******************************************************************************
 * Copyright (C) 2019, 2023 Frédéric Bard
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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.views.calendar.CalendarProfile;
import net.tourbook.weather.HistoricalWeatherRetriever;
import net.tourbook.weather.WeatherUtils;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;

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

   private static final String    baseApiUrl   = "http://api.worldweatheronline.com/premium/v1/past-weather.ashx"; //$NON-NLS-1$
   private static final String    keyParameter = "?key=";                                                          //$NON-NLS-1$

   private String                 endDate;
   private String                 startDate;

   private final IPreferenceStore prefStore    = TourbookPlugin.getPrefStore();
   private Data                   weatherData  = null;

   /*
    * @param tour
    * The tour for which we need to retrieve the weather data.
    */
   public WorldWeatherOnlineRetriever(final TourData tourData) {

      super(tourData);

      startDate = TimeTools.Formatter_YearMonthDay.format(tour.getTourStartTime());
      endDate = TimeTools.Formatter_YearMonthDay.format(tour.getTourStartTime()
            .plusSeconds(tour.getTourDeviceTime_Elapsed()));
   }

   public static String convertWeatherCodeToMTWeatherClouds(final String weatherCode) {

      String weatherType;

      // Codes : http://www.worldweatheronline.com/feed/wwoConditionCodes.xml
      //https://www.worldweatheronline.com/weather-api/api/docs/weather-icons.aspx
      switch (weatherCode) {
      case "122": //$NON-NLS-1$
      case "119": //$NON-NLS-1$
      case "143": //$NON-NLS-1$
      case "248": //$NON-NLS-1$
         weatherType = IWeather.WEATHER_ID_OVERCAST;
         break;
      case "113": //$NON-NLS-1$
         weatherType = IWeather.WEATHER_ID_CLEAR;
         break;
      case "116": //$NON-NLS-1$
      case "260": //$NON-NLS-1$
         weatherType = IWeather.WEATHER_ID_PART_CLOUDS;
         break;
      case "299": //$NON-NLS-1$
      case "302": //$NON-NLS-1$
      case "305": //$NON-NLS-1$
      case "308": //$NON-NLS-1$
      case "314": //$NON-NLS-1$
      case "356": //$NON-NLS-1$
      case "359": //$NON-NLS-1$
      case "377": //$NON-NLS-1$
      case "365": //$NON-NLS-1$
      case "389": //$NON-NLS-1$
         weatherType = IWeather.WEATHER_ID_RAIN;
         break;
      case "332": //$NON-NLS-1$
      case "335": //$NON-NLS-1$
      case "338": //$NON-NLS-1$
      case "329": //$NON-NLS-1$
      case "326": //$NON-NLS-1$
      case "323": //$NON-NLS-1$
      case "320": //$NON-NLS-1$
      case "371": //$NON-NLS-1$
      case "368": //$NON-NLS-1$
      case "230": //$NON-NLS-1$
      case "227": //$NON-NLS-1$
      case "179": //$NON-NLS-1$
      case "392": //$NON-NLS-1$
      case "395": //$NON-NLS-1$
         weatherType = IWeather.WEATHER_ID_SNOW;
         break;
      case "200": //$NON-NLS-1$
         weatherType = IWeather.WEATHER_ID_SEVERE_WEATHER_ALERT;
         break;
      case "374": //$NON-NLS-1$
      case "362": //$NON-NLS-1$
      case "350": //$NON-NLS-1$
      case "317": //$NON-NLS-1$
      case "182": //$NON-NLS-1$
      case "176": //$NON-NLS-1$
      case "386": //$NON-NLS-1$
         weatherType = IWeather.WEATHER_ID_SCATTERED_SHOWERS;
         break;
      case "311": //$NON-NLS-1$
      case "353": //$NON-NLS-1$
      case "185": //$NON-NLS-1$
      case "263": //$NON-NLS-1$
      case "266": //$NON-NLS-1$
      case "281": //$NON-NLS-1$
      case "284": //$NON-NLS-1$
      case "293": //$NON-NLS-1$
      case "296": //$NON-NLS-1$
         weatherType = IWeather.WEATHER_ID_DRIZZLE;
         break;
      default:
         weatherType = UI.EMPTY_STRING;
         break;
      }

      return weatherType;
   }

   public static String getApiUrl() {
      return baseApiUrl + keyParameter;
   }

   @Override
   protected String buildDetailedWeatherLog(final boolean isCompressed) {

      final List<String> fullWeatherDataList = new ArrayList<>();

      for (final Hourly hourly : weatherData.getFilteredHourly()) {

         final long hourlyEpochSeconds = hourly.getEpochSeconds();
         final TourDateTime tourDateTime = TimeTools.createTourDateTime(
               hourlyEpochSeconds * 1000L,
               tour.getTimeZoneId());

         final boolean isDisplayEmptyValues = !isCompressed;
         String fullWeatherData = WeatherUtils.buildFullWeatherDataString(
               hourly.getTempC(),
               WeatherUtils.getWeatherIcon(
                     WeatherUtils.getWeatherIndex(
                           convertWeatherCodeToMTWeatherClouds(
                                 hourly.getWeatherCode()))),
               hourly.getWeatherDescription(),
               hourly.getFeelsLikeC(),
               hourly.getWindspeedKmph(),
               hourly.getWinddirDegree(),
               hourly.getHumidity(),
               hourly.getPressure(),
               hourly.getPrecipMM(),
               0,
               0,
               tourDateTime,
               isDisplayEmptyValues);

         if (isCompressed) {
            fullWeatherData = fullWeatherData.replaceAll("\\s+", UI.SPACE1); //$NON-NLS-1$
         }

         fullWeatherDataList.add(fullWeatherData);
      }

      if (!isCompressed) {

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
      }

      final String fullWeatherData = String.join(
            UI.SYSTEM_NEW_LINE,
            fullWeatherDataList);

      return fullWeatherData;
   }

   private String buildWeatherApiRequest() {

      final StringBuilder weatherRequestWithParameters = new StringBuilder(baseApiUrl + UI.SYMBOL_QUESTION_MARK);

   // SET_FORMATTING_OFF

      weatherRequestWithParameters.append(      "key"             + "=" + prefStore.getString(ITourbookPreferences.WEATHER_API_KEY).trim()); //$NON-NLS-1$ //$NON-NLS-2$
      weatherRequestWithParameters.append("&" + "q"               + "=" + searchAreaCenter.getLatitude() + "," + searchAreaCenter.getLongitude()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      weatherRequestWithParameters.append("&" + "date"            + "=" + startDate); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      //tp=1 : Specifies the weather forecast time interval in hours. Here, every 1 hour
      weatherRequestWithParameters.append("&" + "tp"              + "=" + "1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      weatherRequestWithParameters.append("&" + "format"          + "=" + "json"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      weatherRequestWithParameters.append("&" + "includelocation" + "=" + "yes"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      weatherRequestWithParameters.append("&" + "extra"           + "=" + "utcDateTime"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      weatherRequestWithParameters.append("&" + "lang"            + "=" + Locale.getDefault().getLanguage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

// SET_FORMATTING_ON

      //If the tour finishes a different day, we need to specify the ending date
      if (!endDate.equals(startDate)) {
         weatherRequestWithParameters.append("&" + "enddate" + "=" + endDate); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }

      return weatherRequestWithParameters.toString();
   }

   /**
    * Deserialize a JSON weather data object into a WeatherData object.
    *
    * @param weatherDataResponse
    *           A string containing a historical weather data JSON object.
    * @return The serialized weather data.
    */
   private Data deserializeWeatherData(final String weatherDataResponse) {

      if (_isLogWeatherData) {

         final long elapsedTime = tour.getTourDeviceTime_Elapsed();
         final ZonedDateTime zdtTourStart = tour.getTourStartTime();
         final ZonedDateTime zdtTourEnd = zdtTourStart.plusSeconds(elapsedTime);
         final String tourTitle = tour.getTourTitle();

         System.out.println();

         if (tourTitle.length() > 0) {
            System.out.println(tourTitle);
         }

         System.out.println(String.format(OtherMessages.TOUR_TOOLTIP_FORMAT_DATE_WEEK_TIME,
               zdtTourStart.format(TimeTools.Formatter_Date_F),
               zdtTourStart.format(TimeTools.Formatter_Time_M),
               zdtTourEnd.format(TimeTools.Formatter_Time_M),
               zdtTourStart.get(TimeTools.calendarWeek.weekOfWeekBasedYear())));

         System.out.println(weatherDataResponse);
      }

      Data serializedWeatherData = null;
      try {

         //weather
         final ObjectMapper mapper = new ObjectMapper();
         final String weatherResults = mapper.readValue(weatherDataResponse, JsonNode.class)
               .get("data") //$NON-NLS-1$
               .toString();

         serializedWeatherData = mapper.readValue(weatherResults, new TypeReference<Data>() {});

      } catch (final Exception e) {

         StatusUtil.logError(
               "WorldWeatherOnlineRetriever.deserializeWeatherData : Error while " + //$NON-NLS-1$
                     "deserializing the historical weather JSON object :" //$NON-NLS-1$
                     + weatherDataResponse + UI.SYSTEM_NEW_LINE + e.getMessage());
      }

      return serializedWeatherData;
   }

   @Override
   public boolean retrieveHistoricalWeatherData() {

      final String weatherRequestWithParameters = buildWeatherApiRequest();

      final String rawWeatherData = sendWeatherApiRequest(weatherRequestWithParameters);
      if (StringUtils.isNullOrEmpty(rawWeatherData)) {
         return false;
      }

      weatherData = deserializeWeatherData(rawWeatherData);
      if (weatherData == null) {
         return false;
      }

      final List<Weather> weather = weatherData.getWeather();
      if (weather == null ||
            weather.isEmpty() ||
            weather.get(0).getHourly() == null ||
            weather.get(0).getHourly().isEmpty()) {
         return false;
      }

   // SET_FORMATTING_OFF

      final boolean hourlyDataExists = weatherData.filterHourlyData(tourStartTime, tourEndTime);
      if(!hourlyDataExists)
      {
         return false;
      }

      //We look for the weather data in the middle of the tour to populate the weather conditions
      weatherData.findMiddleHourly(tourMiddleTime);
      tour.setWeather(                       weatherData.getWeatherDescription());
      tour.setWeather_Clouds(                weatherData.getWeatherType());

      tour.setWeather_Temperature_Average(   weatherData.getTemperatureAverage());
      tour.setWeather_Humidity(              weatherData.getAverageHumidity());
      tour.setWeather_Precipitation(         weatherData.getTotalPrecipitation());
      tour.setWeather_Pressure(              weatherData.getAveragePressure());
      tour.setWeather_Temperature_Max(       weatherData.getTemperatureMax());
      tour.setWeather_Temperature_Min(       weatherData.getTemperatureMin());
      tour.setWeather_Temperature_WindChill( weatherData.getAverageWindChill());

      weatherData.computeAverageWindSpeedAndDirection();
      tour.setWeather_Wind_Speed(            weatherData.getAverageWindSpeed());
      tour.setWeather_Wind_Direction(        weatherData.getAverageWindDirection());

// SET_FORMATTING_ON

      return true;
   }

}
