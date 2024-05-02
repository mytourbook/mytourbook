/*******************************************************************************
 * Copyright (C) 2022, 2024 Frédéric Bard
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
package net.tourbook.weather.openweathermap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.common.weather.IWeather;
import net.tourbook.weather.HistoricalWeatherRetriever;
import net.tourbook.weather.WeatherUtils;

import org.apache.commons.lang3.time.DateUtils;
import org.eclipse.jface.dialogs.IDialogSettings;

public class OpenWeatherMapRetriever extends HistoricalWeatherRetriever {

   private static final int    MAX_REQUESTS_PER_DAY     = 50;
   private static final String STATE_LAST_REQUEST_DAY   = "lastRequestDay";      //$NON-NLS-1$
   private static final String STATE_REQUEST_COUNT      = "requestCount";        //$NON-NLS-1$

   private static final String OPENWEATHERMAP3_BASEPATH = "/openweathermap/3.0"; //$NON-NLS-1$

   //https://openweathermap.org/api/one-call-3
   private static final String BASE_TIME_MACHINE_API_URL = WeatherUtils.OAUTH_PASSEUR_APP_URL + OPENWEATHERMAP3_BASEPATH + "/timemachine"; //$NON-NLS-1$
   private static final String BASE_CURRENT_API_URL      = WeatherUtils.OAUTH_PASSEUR_APP_URL + OPENWEATHERMAP3_BASEPATH + "/current";     //$NON-NLS-1$

   // https://openweathermap.org/api/air-pollution
   private static final String          BASE_AIR_POLLUTION_API_URL = WeatherUtils.OAUTH_PASSEUR_APP_URL + "/openweathermap/air_pollution"; //$NON-NLS-1$

   private static final IDialogSettings _state                     = TourbookPlugin.getState("OpenWeatherMapRetriever");                   //$NON-NLS-1$
   private TimeMachineResult            timeMachineResult          = null;

   private AirPollutionResult           airPollutionResult         = null;

   /**
    * Codes : https://openweathermap.org/weather-conditions#Icon-list
    *
    * @param weatherIcon
    *
    * @return
    */
   public static String convertWeatherIconToMTWeatherClouds(final String weatherIcon) {

      String weatherType = UI.EMPTY_STRING;

      if (StringUtils.isNullOrEmpty(weatherIcon)) {
         return weatherType;
      }

      if (weatherIcon.startsWith("01")) { //$NON-NLS-1$
         weatherType = IWeather.WEATHER_ID_CLEAR;
      } else if (weatherIcon.startsWith("02") || //$NON-NLS-1$
            weatherIcon.startsWith("03")) { //$NON-NLS-1$
         weatherType = IWeather.WEATHER_ID_PART_CLOUDS;
      } else if (weatherIcon.startsWith("04")) { //$NON-NLS-1$
         weatherType = IWeather.WEATHER_ID_OVERCAST;
      } else if (weatherIcon.startsWith("09")) { //$NON-NLS-1$
         weatherType = IWeather.WEATHER_ID_SCATTERED_SHOWERS;
      } else if (weatherIcon.startsWith("10")) { //$NON-NLS-1$
         weatherType = IWeather.WEATHER_ID_RAIN;
      } else if (weatherIcon.startsWith("11")) { //$NON-NLS-1$
         weatherType = IWeather.WEATHER_ID_LIGHTNING;
      } else if (weatherIcon.startsWith("13")) { //$NON-NLS-1$
         weatherType = IWeather.WEATHER_ID_SNOW;
      } else if (weatherIcon.startsWith("50")) { //$NON-NLS-1$
         weatherType = IWeather.WEATHER_ID_DRIZZLE;
      }

      return weatherType;
   }

   public static String getBaseTimeMachineApiUrl() {
      return BASE_TIME_MACHINE_API_URL;
   }

   private String buildAirPollutionApiRequest() {

      final long tourStart = DateUtils.round(Date.from(tour.getTourStartTime().toInstant()), Calendar.HOUR).toInstant().getEpochSecond();
      final long tourEnd = DateUtils.round(Date.from(tour.getTourStartTime().plus(tour.getTourDeviceTime_Elapsed(), ChronoUnit.SECONDS).toInstant()),
            Calendar.HOUR)
            .toInstant().getEpochSecond();

      final StringBuilder weatherRequestWithParameters = new StringBuilder(BASE_AIR_POLLUTION_API_URL + UI.SYMBOL_QUESTION_MARK);

   // SET_FORMATTING_OFF

         weatherRequestWithParameters.append(      "lat"   + "=" + searchAreaCenter.getLatitude()); //$NON-NLS-1$ //$NON-NLS-2$
         weatherRequestWithParameters.append("&" + "lon"   + "=" + searchAreaCenter.getLongitude()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         weatherRequestWithParameters.append("&" + "start" + "=" + tourStart); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         weatherRequestWithParameters.append("&" + "end"   + "=" + tourEnd); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

   // SET_FORMATTING_ON

      return weatherRequestWithParameters.toString();
   }

   @Override
   protected String buildDetailedWeatherLog(final boolean isCompressed) {

      final List<String> fullWeatherDataList = new ArrayList<>();

      final List<WeatherData> hourlyList = timeMachineResult.getData();

      if (hourlyList.size() > 0) {

         for (final WeatherData hourly : hourlyList) {

            final int hourlyDateTime = hourly.dt();

            final TourDateTime tourDateTime = TimeTools.createTourDateTime(
                  hourlyDateTime * 1000L,
                  tour.getTimeZoneId());

            final net.tourbook.weather.openweathermap.List currentAirPollutionResult = airPollutionResult.list().stream()
                  .filter(list -> list.dt() == hourlyDateTime)
                  .findAny()
                  .orElse(null);

            final int airQualityIndex = currentAirPollutionResult == null
                  ? 0 : currentAirPollutionResult.main().aqi();

            final boolean isDisplayEmptyValues = !isCompressed;
            String fullWeatherData = WeatherUtils.buildFullWeatherDataString(
                  (float) hourly.temp(),
                  WeatherUtils.getWeatherIcon(
                        WeatherUtils.getWeatherIndex(
                              convertWeatherIconToMTWeatherClouds(
                                    hourly.weather().get(0).icon()))),
                  hourly.weather().get(0).description(),
                  (float) hourly.feels_like(),
                  (float) hourly.getWind_speedKmph(),
                  hourly.wind_deg(),
                  hourly.humidity(),
                  hourly.pressure(),
                  hourly.getRain(),
                  hourly.getSnow(),
                  airQualityIndex,
                  tourDateTime,
                  isDisplayEmptyValues);

            if (isCompressed) {
               fullWeatherData = fullWeatherData.replaceAll("\\s+", UI.SPACE1); //$NON-NLS-1$
            }

            fullWeatherDataList.add(fullWeatherData);
         }
      } else if (airPollutionResult.getAirQualityIndexAverage() != 0) {

         for (final net.tourbook.weather.openweathermap.List currentAirPollutionResult : airPollutionResult.list()) {

            final int airQualityIndex = currentAirPollutionResult.main().aqi();

            final TourDateTime tourDateTime = TimeTools.createTourDateTime(
                  currentAirPollutionResult.dt() * 1000L,
                  tour.getTimeZoneId());

            final boolean isDisplayEmptyValues = !isCompressed;

            final String tourTime = String.format("%3s", tourDateTime.tourZonedDateTime.getHour() + UI.UNIT_LABEL_TIME); //$NON-NLS-1$

            String airQuality = UI.EMPTY_STRING;
            if (airQualityIndex > 0 || isDisplayEmptyValues) {

               airQuality = tourTime + UI.SPACE3
                     + Messages.Log_HistoricalWeatherRetriever_001_WeatherData_AirQuality
                     + UI.SPACE
                     + airQualityIndex;
            }

            if (isCompressed) {
               airQuality = airQuality.replaceAll("\\s+", UI.SPACE1); //$NON-NLS-1$
            }

            fullWeatherDataList.add(airQuality);
         }
      }

      final String fullWeatherData = String.join(
            UI.SYSTEM_NEW_LINE,
            fullWeatherDataList);

      return fullWeatherData;
   }

   private String buildWeatherApiRequest(final long date, final String baseUrl) {

      final StringBuilder weatherRequestWithParameters = new StringBuilder(baseUrl + UI.SYMBOL_QUESTION_MARK);

// SET_FORMATTING_OFF

      weatherRequestWithParameters.append(      "units" + "=" + "metric"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      weatherRequestWithParameters.append("&" + "lat"   + "=" + searchAreaCenter.getLatitude()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      weatherRequestWithParameters.append("&" + "lon"   + "=" + searchAreaCenter.getLongitude()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      weatherRequestWithParameters.append("&" + "lang"  + "=" + Locale.getDefault().getLanguage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      weatherRequestWithParameters.append("&" + "dt"    + "=" + date); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

// SET_FORMATTING_ON

      return weatherRequestWithParameters.toString();
   }

   @Override
   protected boolean canMakeRequest() {

      final LocalDate lastRequestDay = Util.getStateDate(_state, STATE_LAST_REQUEST_DAY, LocalDate.of(2000, 1, 1));
      int requestCount = getRequestCount();

      if (!LocalDate.now().equals(lastRequestDay)) {

         _state.put(STATE_LAST_REQUEST_DAY, LocalDate.now().toString());
         requestCount = 0;
         _state.put(STATE_REQUEST_COUNT, requestCount);
      }

      return requestCount < MAX_REQUESTS_PER_DAY;
   }

   private AirPollutionResult deserializeAirPollutionData(final String airPollutionDataResponse) {

      airPollutionResult = null;
      try {

         final ObjectMapper mapper = new ObjectMapper();
         final String weatherResults = mapper
               .readValue(airPollutionDataResponse, JsonNode.class)
               .toString();

         airPollutionResult = mapper.readValue(
               weatherResults,
               new TypeReference<AirPollutionResult>() {});

      } catch (final Exception e) {

         StatusUtil.logError(
               "OpenWeatherMapRetriever.deserializeAirPollutionData : Error while " + //$NON-NLS-1$
                     "deserializing the historical air quality JSON object :" //$NON-NLS-1$
                     + airPollutionDataResponse + UI.SYSTEM_NEW_LINE + e.getMessage());
      }

      return airPollutionResult;
   }

   private CurrentResult deserializeCurrentWeatherData(final String weatherDataResponse) {

      CurrentResult currentResult = null;
      try {

         final ObjectMapper mapper = new ObjectMapper();
         final String weatherResults = mapper
               .readValue(weatherDataResponse, JsonNode.class)
               .toString();

         currentResult = mapper.readValue(
               weatherResults,
               new TypeReference<CurrentResult>() {});

      } catch (final Exception e) {

         StatusUtil.logError(
               "OpenWeatherMapRetriever.deserializeCurrentWeatherData : Error while " + //$NON-NLS-1$
                     "deserializing the historical weather JSON object :" //$NON-NLS-1$
                     + weatherDataResponse + UI.SYSTEM_NEW_LINE + e.getMessage());
      }

      return currentResult;
   }

   private TimeMachineResult deserializeHistoricalWeatherData(final String weatherDataResponse) {

      TimeMachineResult newTimeMachineResult = null;
      try {

         final ObjectMapper mapper = new ObjectMapper();
         final String weatherResults = mapper
               .readValue(weatherDataResponse, JsonNode.class)
               .toString();

         newTimeMachineResult = mapper.readValue(
               weatherResults,
               new TypeReference<TimeMachineResult>() {});

      } catch (final Exception e) {

         StatusUtil.logError(
               "OpenWeatherMapRetriever.deserializeHistoricalWeatherData : Error while " + //$NON-NLS-1$
                     "deserializing the historical weather JSON object :" //$NON-NLS-1$
                     + weatherDataResponse + UI.SYSTEM_NEW_LINE + e.getMessage());
      }

      return newTimeMachineResult;
   }

   private int getRequestCount() {

      return Util.getStateInt(_state, STATE_REQUEST_COUNT, 0);
   }

   @Override
   protected String getWeatherRetrievalFailureLogMessage() {
      return Messages.Log_HistoricalWeatherRetriever_003_RetrievalLimitReached;
   }

   /**
    * Determines if the tour start time is within the current hour
    *
    * @param tourStartTime
    *
    * @return
    */
   private boolean isTourStartTimeCurrent(final long tourStartTime, final String tourTimeZoneId) {

      final GregorianCalendar tourStartTimeCalendar = new GregorianCalendar();
      tourStartTimeCalendar.setTimeInMillis(tourStartTime * 1000L);
      tourStartTimeCalendar.setTimeZone(TimeZone.getTimeZone(tourTimeZoneId));
      tourStartTimeCalendar.set(Calendar.MINUTE, 0);
      tourStartTimeCalendar.set(Calendar.SECOND, 0);
      tourStartTimeCalendar.set(Calendar.MILLISECOND, 0);

      final Instant instant = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant();
      final long timeInMillis = instant.toEpochMilli();
      final GregorianCalendar currentTimeCalendar = new GregorianCalendar();
      currentTimeCalendar.setTimeInMillis(timeInMillis);
      currentTimeCalendar.set(Calendar.MINUTE, 0);
      currentTimeCalendar.set(Calendar.SECOND, 0);
      currentTimeCalendar.set(Calendar.MILLISECOND, 0);

      return tourStartTimeCalendar.compareTo(currentTimeCalendar) == 0;
   }

   private AirPollutionResult retrieveAirPollutionData() {

      final String airPollutionRequestWithParameters = buildAirPollutionApiRequest();

      final String rawAirPollutionData = sendWeatherApiRequest(airPollutionRequestWithParameters);
      if (StringUtils.isNullOrEmpty(rawAirPollutionData)) {
         return null;
      }

      return deserializeAirPollutionData(rawAirPollutionData);
   }

   private boolean retrieveAirQuality() {

      //Send an API request as long as we don't have the results covering the entire duration of the tour
      airPollutionResult = retrieveAirPollutionData();
      if (airPollutionResult == null || airPollutionResult.getAirQualityIndexAverage() == 0) {
         return false;
      }

      setTourWeatherAirQuality();

      return true;
   }

   private CurrentResult retrieveCurrentWeatherData(final long requestedTime) {

      final String weatherRequestWithParameters = buildWeatherApiRequest(requestedTime, BASE_CURRENT_API_URL);

      final String rawWeatherData = sendWeatherApiRequest(weatherRequestWithParameters);
      if (StringUtils.isNullOrEmpty(rawWeatherData)) {
         return null;
      }

      return deserializeCurrentWeatherData(rawWeatherData);
   }

   private boolean retrieveHistoricalWeather() {

      timeMachineResult = new TimeMachineResult();
      long requestedTime = tourStartTime;

      final boolean isTourStartWithinTheCurrentHour = isTourStartTimeCurrent(
            tourStartTime,
            tour.getTimeZoneId());

      // If the tour start time is within the current hour, we use the
      // current weather data instead of the historical one.
      if (isTourStartWithinTheCurrentHour) {

         final CurrentResult currentResult = retrieveCurrentWeatherData(requestedTime);
         if (currentResult != null) {

            setTourWeatherWithCurrentWeather(currentResult.current());

            return true;
         }
      }

      while (true) {

         //Send an API request as long as we don't have the results covering the entire duration of the tour
         final TimeMachineResult newTimeMachineResult = retrieveHistoricalWeatherData(requestedTime);
         if (newTimeMachineResult == null) {
            return false;
         }

         final int requestCount = getRequestCount();
         _state.put(STATE_REQUEST_COUNT, requestCount + 1);

         timeMachineResult.addAllData(newTimeMachineResult.getData());
         final List<WeatherData> hourly = timeMachineResult.getData();

         final int lastWeatherDataHour = hourly.get(hourly.size() - 1).dt();
         if (WeatherUtils.isTourWeatherDataComplete(
               hourly.get(0).dt(),
               lastWeatherDataHour,
               tourStartTime,
               tourEndTime)) {
            break;
         }

         //Setting the requested time to the next hour to retrieve the next set of weather data
         requestedTime = lastWeatherDataHour + 3600L;

         // We avoid requesting data in the future
         if (requestedTime > TimeTools.nowInMilliseconds() / 1000) {

            if (hourly.isEmpty()) {
               return false;
            } else {
               break;
            }
         }
      }

      final boolean hourlyDataExists = timeMachineResult.filterWeatherData(tourStartTime, tourEndTime);
      if (!hourlyDataExists) {
         return false;
      }

      setTourWeatherWithHistoricalWeather();

      return true;
   }

   @Override
   public boolean retrieveHistoricalWeatherData() {

      final boolean isHistoricalWeatherRetrieved = retrieveHistoricalWeather();

      final boolean isAirQualityRetrieved = retrieveAirQuality();

      return isHistoricalWeatherRetrieved || isAirQualityRetrieved;
   }

   private TimeMachineResult retrieveHistoricalWeatherData(final long requestedTime) {

      final String weatherRequestWithParameters = buildWeatherApiRequest(requestedTime, BASE_TIME_MACHINE_API_URL);

      final String rawWeatherData = sendWeatherApiRequest(weatherRequestWithParameters);
      if (StringUtils.isNullOrEmpty(rawWeatherData)) {
         return null;
      }

      return deserializeHistoricalWeatherData(rawWeatherData);
   }

   private void setTourWeatherAirQuality() {

      final int airQualityIndexAverage = airPollutionResult.getAirQualityIndexAverage();

      tour.setWeather_AirQuality(IWeather.AIR_QUALITY_IDS[airQualityIndexAverage]);
   }

   private void setTourWeatherWithCurrentWeather(final Current currentWeather) {

// SET_FORMATTING_OFF

      tour.appendOrReplaceWeather(           currentWeather.getWeatherDescription());
      tour.setWeather_Clouds(                currentWeather.getWeatherClouds());
      tour.setWeather_Temperature_Average(   currentWeather.temp());
      tour.setWeather_Temperature_Max(       currentWeather.temp());
      tour.setWeather_Temperature_Min(       currentWeather.temp());
      tour.setWeather_Humidity((short)       currentWeather.humidity());
      tour.setWeather_Precipitation(         currentWeather.getPrecipitation());
      tour.setWeather_Pressure((short)       currentWeather.pressure());
      tour.setWeather_Snowfall(              currentWeather.getSnowfall());
      tour.setWeather_Temperature_WindChill( currentWeather.feels_like());
      tour.setWeather_Wind_Speed(            currentWeather.getWind_speed());
      tour.setWeather_Wind_Direction(        currentWeather.wind_deg());

// SET_FORMATTING_ON
   }

   private void setTourWeatherWithHistoricalWeather() {

// SET_FORMATTING_OFF

      //We look for the weather data in the middle of the tour to populate the weather conditions
      timeMachineResult.findMiddleWeatherData(    tourMiddleTime);
      tour.appendOrReplaceWeather(           timeMachineResult.getWeatherDescription());
      tour.setWeather_Clouds(                timeMachineResult.getWeatherClouds());

      tour.setWeather_Temperature_Average(   timeMachineResult.getTemperatureAverage());
      tour.setWeather_Humidity((short)       timeMachineResult.getAverageHumidity());
      tour.setWeather_Precipitation(         timeMachineResult.getTotalPrecipitation());
      tour.setWeather_Pressure((short)       timeMachineResult.getAveragePressure());
      tour.setWeather_Snowfall(              timeMachineResult.getTotalSnowfall());
      tour.setWeather_Temperature_Max(       timeMachineResult.getTemperatureMax());
      tour.setWeather_Temperature_Min(       timeMachineResult.getTemperatureMin());
      tour.setWeather_Temperature_WindChill( timeMachineResult.getAverageWindChill());

      timeMachineResult.computeAverageWindSpeedAndDirection();
      tour.setWeather_Wind_Speed(            timeMachineResult.getAverageWindSpeed());
      tour.setWeather_Wind_Direction(        timeMachineResult.getAverageWindDirection());

// SET_FORMATTING_ON
   }
}
