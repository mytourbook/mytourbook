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
package net.tourbook.weather.weatherapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
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

public class WeatherApiRetriever extends HistoricalWeatherRetriever {

   private static final String baseApiUrl    = WeatherUtils.OAUTH_PASSEUR_APP_URL + "/weatherapi"; //$NON-NLS-1$

   private HistoryResult       historyResult = null;

   public WeatherApiRetriever(final TourData tourData) {

      super(tourData);
   }

   public static String getBaseApiUrl() {
      return baseApiUrl;
   }

   @Override
   protected String buildDetailedWeatherLog(final boolean isCompressed) {

      final List<String> fullWeatherDataList = new ArrayList<>();

      for (final Hour hour : historyResult.getHourList()) {

         final TourDateTime tourDateTime = TimeTools.createTourDateTime(
               hour.getTime_epoch() * 1000L,
               tour.getTimeZoneId());

         final boolean isDisplayEmptyValues = !isCompressed;
         String fullWeatherData = WeatherUtils.buildFullWeatherDataString(
               (float) hour.getTemp_c(),
               (float) hour.getFeelslike_c(),
               (float) hour.getWind_kph(),
               hour.getWind_degree(),
               hour.getHumidity(),
               (int) hour.getPressure_mb(),
               (float) hour.getPrecip_mm(),
               0,
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

   private String buildWeatherApiRequest(final LocalDate requestedDate) {

      String weatherRequestWithParameters = UI.EMPTY_STRING;

      try {
         final URI apiUri = new URI(baseApiUrl);

         final URIBuilder uriBuilder = new URIBuilder()
               .setScheme(apiUri.getScheme())
               .setHost(apiUri.getHost())
               .setPath(apiUri.getPath());

         uriBuilder.setParameter("lat", String.valueOf(searchAreaCenter.getLatitude())); //$NON-NLS-1$
         uriBuilder.setParameter("lon", String.valueOf(searchAreaCenter.getLongitude())); //$NON-NLS-1$
         uriBuilder.setParameter("lang", Locale.getDefault().getLanguage()); //$NON-NLS-1$

         final String date = requestedDate.format(TimeTools.Formatter_YearMonthDay);

         uriBuilder.setParameter("dt", date); //$NON-NLS-1$
         weatherRequestWithParameters = uriBuilder.build().toString();

         return weatherRequestWithParameters;

      } catch (final URISyntaxException e) {

         StatusUtil.logError(
               "WeatherApiRetriever.buildWeatherApiRequest : Error while " + //$NON-NLS-1$
                     "building the historical weather request:" //$NON-NLS-1$
                     + e.getMessage());
         return UI.EMPTY_STRING;
      }
   }

   private HistoryResult deserializeWeatherData(final String weatherDataResponse) {

      HistoryResult newHistoryResult = null;
      try {

         final ObjectMapper mapper = new ObjectMapper();
         final String weatherResults = mapper
               .readValue(weatherDataResponse, JsonNode.class)
               .toString();

         newHistoryResult = mapper.readValue(
               weatherResults,
               new TypeReference<HistoryResult>() {});

      } catch (final Exception e) {

         StatusUtil.logError(
               "WeatherApiRetriever.deserializeWeatherData : Error while " + //$NON-NLS-1$
                     "deserializing the historical weather JSON object :" //$NON-NLS-1$
                     + weatherDataResponse + UI.SYSTEM_NEW_LINE + e.getMessage());
      }

      return newHistoryResult;
   }

   @Override
   public boolean retrieveHistoricalWeatherData() {

      historyResult = new HistoryResult();

      LocalDate requestedDate = tour.getTourStartTime().toLocalDate();

      final LocalDate tomorrow = LocalDate.now().plusDays(1);

      //Send an API request as long as we don't have the results covering the
      //entire duration of the tour
      while (true) {

         final String weatherRequestWithParameters = buildWeatherApiRequest(requestedDate);

         final String rawWeatherData = sendWeatherApiRequest(weatherRequestWithParameters);
         if (StringUtils.isNullOrEmpty(rawWeatherData)) {
            return false;
         }

         final HistoryResult newHistoryResult = deserializeWeatherData(rawWeatherData);
         if (newHistoryResult == null) {
            return false;
         }

         historyResult.addHourList(newHistoryResult.getForecastdayHourList());
         final List<Hour> hourList = historyResult.getHourList();

         final int lastWeatherDataHour = hourList.get(hourList.size() - 1).getTime_epoch();
         if (WeatherUtils.isTourWeatherDataComplete(
               hourList.get(0).getTime_epoch(),
               lastWeatherDataHour,
               tourStartTime,
               tourEndTime)) {
            break;
         }

         //Setting the requested time to the next day to retrieve the next set of weather data
         requestedDate = requestedDate.plusDays(1);

         // We avoid requesting data in the future
         if (requestedDate.compareTo(tomorrow) > 0) {

            if (hourList.isEmpty()) {
               return false;
            } else {
               break;
            }
         }
      }

// SET_FORMATTING_OFF

      final boolean hourlyDataExists = historyResult.filterHourData(tourStartTime, tourEndTime);
      if(!hourlyDataExists)
      {
         return false;
      }

      tour.setIsWeatherDataFromProvider(true);

      //We look for the weather data in the middle of the tour to populate the weather conditions
      historyResult.findMiddleHour(tourMiddleTime);
      tour.setWeather(                       historyResult.getWeatherDescription());
      tour.setWeather_Clouds(                historyResult.getWeatherType());

      tour.setWeather_Temperature_Average(   historyResult.getTemperatureAverage());
      tour.setWeather_Humidity((short)       historyResult.getAverageHumidity());
      tour.setWeather_Precipitation(         historyResult.getTotalPrecipitation());
      tour.setWeather_Pressure((short)       historyResult.getAveragePressure());
      tour.setWeather_Temperature_Max(       historyResult.getTemperatureMax());
      tour.setWeather_Temperature_Min(       historyResult.getTemperatureMin());
      tour.setWeather_Temperature_WindChill( historyResult.getAverageWindChill());

      historyResult.computeAverageWindSpeedAndDirection();
      tour.setWeather_Wind_Speed(            historyResult.getAverageWindSpeed());
      tour.setWeather_Wind_Direction(        historyResult.getAverageWindDirection());

// SET_FORMATTING_ON

      return true;
   }
}
