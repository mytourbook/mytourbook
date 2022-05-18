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
package net.tourbook.weather.openweathermap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.weather.HistoricalWeatherRetriever;
import net.tourbook.weather.WeatherUtils;

import org.apache.http.client.utils.URIBuilder;

public class OpenWeatherMapRetriever extends HistoricalWeatherRetriever {

   private static final String baseApiUrl        = WeatherUtils.HEROKU_APP_URL + "/openweathermap/timemachine"; //$NON-NLS-1$

   private TimeMachineResult   timeMachineResult = null;

   public OpenWeatherMapRetriever(final TourData tourData) {

      super(tourData);
   }

   public static String getBaseApiUrl() {
      return baseApiUrl;
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

      String weatherRequestWithParameters = UI.EMPTY_STRING;

      try {
         final URI apiUri = new URI(baseApiUrl);

         final URIBuilder uriBuilder = new URIBuilder()
               .setScheme(apiUri.getScheme())
               .setHost(apiUri.getHost())
               .setPath(apiUri.getPath());

         uriBuilder.setParameter("units", "metric"); //$NON-NLS-1$ //$NON-NLS-2$
         uriBuilder.setParameter("lat", String.valueOf(searchAreaCenter.getLatitude())); //$NON-NLS-1$
         uriBuilder.setParameter("lon", String.valueOf(searchAreaCenter.getLongitude())); //$NON-NLS-1$
         uriBuilder.setParameter("lang", Locale.getDefault().getLanguage()); //$NON-NLS-1$
         uriBuilder.setParameter("dt", String.valueOf(date)); //$NON-NLS-1$
         weatherRequestWithParameters = uriBuilder.build().toString();

         return weatherRequestWithParameters;

      } catch (final URISyntaxException e) {

         StatusUtil.logError(
               "OpenWeatherMapRetriever.buildWeatherApiRequest : Error while " + //$NON-NLS-1$
                     "building the historical weather request:" //$NON-NLS-1$
                     + e.getMessage());
         return UI.EMPTY_STRING;
      }
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

   @Override
   public boolean retrieveHistoricalWeatherData() {

      timeMachineResult = new TimeMachineResult();

      long requestedTime = tourStartTime;

      while (true) {

         //Send an API request as long as we don't have the results covering the entire duration of the tour
         final String weatherRequestWithParameters = buildWeatherApiRequest(requestedTime);

         final String rawWeatherData = sendWeatherApiRequest(weatherRequestWithParameters);
         if (StringUtils.isNullOrEmpty(rawWeatherData)) {
            return false;
         }

         final TimeMachineResult newTimeMachineResult = deserializeWeatherData(rawWeatherData);
         if (newTimeMachineResult == null) {
            return false;
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

// SET_FORMATTING_OFF

      final boolean hourlyDataExists = timeMachineResult.filterHourlyData(tourStartTime, tourEndTime);
      if(!hourlyDataExists)
      {
         return false;
      }

      tour.setIsWeatherDataFromProvider(true);

      //We look for the weather data in the middle of the tour to populate the weather conditions
      timeMachineResult.findMiddleHourly(tourMiddleTime);
      tour.setWeather(                       timeMachineResult.getWeatherDescription());
      tour.setWeather_Clouds(                timeMachineResult.getWeatherType());

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

      return true;
   }
}
