/*******************************************************************************
 * Copyright (C) 2022, 2023 Frédéric Bard
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.weather.HistoricalWeatherRetriever;
import net.tourbook.weather.WeatherUtils;

public class OpenWeatherMapRetriever extends HistoricalWeatherRetriever {

   private static final String BASE_API_URL      = WeatherUtils.OAUTH_PASSEUR_APP_URL + "/openweathermap/timemachine"; //$NON-NLS-1$

   private TimeMachineResult   timeMachineResult = null;

   public OpenWeatherMapRetriever(final TourData tourData) {

      super(tourData);
   }

   public static String getBaseApiUrl() {
      return BASE_API_URL;
   }

   @Override
   protected String buildDetailedWeatherLog(final boolean isCompressed) {

      final List<String> fullWeatherDataList = new ArrayList<>();

      for (final Hourly hourly : timeMachineResult.getHourly()) {

         final TourDateTime tourDateTime = TimeTools.createTourDateTime(
               hourly.getDt() * 1000L,
               tour.getTimeZoneId());

         final boolean isDisplayEmptyValues = !isCompressed;
         String fullWeatherData = WeatherUtils.buildFullWeatherDataString(
               (float) hourly.getTemp(),
               (float) hourly.getFeels_like(),
               (float) hourly.getWind_speedKmph(),
               hourly.getWind_deg(),
               hourly.getHumidity(),
               hourly.getPressure(),
               hourly.getRain(),
               hourly.getSnow(),
               tourDateTime,
               isDisplayEmptyValues);

         if (isCompressed) {
            fullWeatherData = fullWeatherData.replaceAll("\\s+", UI.SPACE1); //$NON-NLS-1$
         }

         fullWeatherDataList.add(fullWeatherData);
      }

      final String fullWeatherData = String.join(
            UI.SYSTEM_NEW_LINE,
            fullWeatherDataList);

      return fullWeatherData;
   }

   private String buildWeatherApiRequest(final long date) {

      final StringBuilder weatherRequestWithParameters = new StringBuilder(BASE_API_URL + UI.SYMBOL_QUESTION_MARK);

// SET_FORMATTING_OFF

      weatherRequestWithParameters.append(      "units" + "=" + "metric"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      weatherRequestWithParameters.append("&" + "lat"   + "=" + searchAreaCenter.getLatitude()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      weatherRequestWithParameters.append("&" + "lon"   + "=" + searchAreaCenter.getLongitude()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      weatherRequestWithParameters.append("&" + "lang"  + "=" + Locale.getDefault().getLanguage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      weatherRequestWithParameters.append("&" + "dt"    + "=" + date); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

// SET_FORMATTING_ON

      return weatherRequestWithParameters.toString();
   }

   private TimeMachineResult deserializeWeatherData(final String weatherDataResponse) {

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
               "OpenWeatherMapRetriever.deserializeWeatherData : Error while " + //$NON-NLS-1$
                     "deserializing the historical weather JSON object :" //$NON-NLS-1$
                     + weatherDataResponse + UI.SYSTEM_NEW_LINE + e.getMessage());
      }

      return newTimeMachineResult;
   }

   /**
    * Determines if the tour start time is within the current hour
    *
    * @param tourStartTime
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

   @Override
   public boolean retrieveHistoricalWeatherData() {

      timeMachineResult = new TimeMachineResult();

      long requestedTime = tourStartTime;

      while (true) {

         //Send an API request as long as we don't have the results covering the entire duration of the tour
         final TimeMachineResult newTimeMachineResult = retrieveWeatherData(requestedTime);
         if (newTimeMachineResult == null) {
            return false;
         }

         final boolean isTourStartWithinTheCurrentHour = isTourStartTimeCurrent(tourStartTime, tour.getTimeZoneId());

         // If the tour start time is within the current hour, we use the
         // current weather data instead of the historical one.
         final Current currentWeather = newTimeMachineResult.getCurrent();
         if (isTourStartWithinTheCurrentHour && currentWeather != null) {

            setTourWeatherWithCurrentWeather(currentWeather);
            return true;
         }

         timeMachineResult.addAllHourly(newTimeMachineResult.getHourly());
         final List<Hourly> hourly = timeMachineResult.getHourly();

         final int lastWeatherDataHour = hourly.get(hourly.size() - 1).getDt();
         if (WeatherUtils.isTourWeatherDataComplete(
               hourly.get(0).getDt(),
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

      final boolean hourlyDataExists = timeMachineResult.filterHourlyData(tourStartTime, tourEndTime);
      if (!hourlyDataExists) {
         return false;
      }

      setTourWeatherWithHistoricalWeather();

      return true;
   }

   private TimeMachineResult retrieveWeatherData(final long requestedTime) {

      final String weatherRequestWithParameters = buildWeatherApiRequest(requestedTime);

      final String rawWeatherData = sendWeatherApiRequest(weatherRequestWithParameters);
      if (StringUtils.isNullOrEmpty(rawWeatherData)) {
         return null;
      }

      return deserializeWeatherData(rawWeatherData);
   }

   private void setTourWeatherWithCurrentWeather(final Current currentWeather) {

// SET_FORMATTING_OFF

      tour.setWeather(                       currentWeather.getWeatherDescription());
      tour.setWeather_Clouds(                currentWeather.getWeatherClouds());
      tour.setWeather_Temperature_Average(   currentWeather.getTemp());
      tour.setWeather_Humidity((short)       currentWeather.getHumidity());
      tour.setWeather_Precipitation(         currentWeather.getPrecipitation());
      tour.setWeather_Pressure((short)       currentWeather.getPressure());
      tour.setWeather_Snowfall(              currentWeather.getSnowfall());
      tour.setWeather_Temperature_WindChill( currentWeather.getFeels_like());
      tour.setWeather_Wind_Speed(            currentWeather.getWind_speed());
      tour.setWeather_Wind_Direction(        currentWeather.getWind_deg());

// SET_FORMATTING_ON
   }

   private void setTourWeatherWithHistoricalWeather() {

// SET_FORMATTING_OFF

      tour.setIsWeatherDataFromProvider(true);

      //We look for the weather data in the middle of the tour to populate the weather conditions
      timeMachineResult.findMiddleHourly(    tourMiddleTime);
      tour.setWeather(                       timeMachineResult.getWeatherDescription());
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
