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
import com.javadocmd.simplelatlng.LatLng;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.weather.HistoricalWeatherRetriever;
import net.tourbook.weather.WeatherUtils;

import org.apache.http.client.utils.URIBuilder;

public class WeatherApiRetriever extends HistoricalWeatherRetriever {

   private static final String baseApiUrl    = WeatherUtils.HEROKU_APP_URL + "/weatherapi"; //$NON-NLS-1$

   private LatLng              searchAreaCenter;
   private long                tourEndTime;
   private long                tourMiddleTime;
   private long                tourStartTime;

   private HistoryResult       historyResult = null;

   public WeatherApiRetriever(final TourData tourData) {

      super(tourData);

      searchAreaCenter = WeatherUtils.determineWeatherSearchAreaCenter(tour);

      tourStartTime = tour.getTourStartTimeMS() / 1000;
      tourEndTime = tour.getTourEndTimeMS() / 1000;
      tourMiddleTime = tourStartTime + ((tourEndTime - tourStartTime) / 2);
   }

   public static String getBaseApiUrl() {
      return baseApiUrl;
   }

   @Override
   protected String buildFullWeatherDataString(final boolean displayStationInformation) {

      return "";
//      final List<String> fullWeatherDataList = new ArrayList<>();
//
//      for (final Hourly hourly : timeMachineResult.getHourly()) {
//
//         final TourDateTime tourDateTime = TimeTools.createTourDateTime(hourly.getDt() * 1000L, tour.getTimeZoneId());
//
//         final String fullWeatherData = WeatherUtils.buildFullWeatherDataString(
//               (float) hourly.getTemp(),
//               (float) hourly.getFeels_like(),
//               (float) hourly.getWind_speedKmph(),
//               hourly.getWind_deg(),
//               hourly.getHumidity(),
//               hourly.getPressure(),
//               hourly.getRain(),
//               hourly.getSnow(),
//               tourDateTime);
//
//         fullWeatherDataList.add(fullWeatherData);
//      }
//
//      final String fullWeatherData = String.join(
//            net.tourbook.ui.UI.SYSTEM_NEW_LINE,
//            fullWeatherDataList);
//
//      return fullWeatherData;
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

         final String date = requestedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")); //$NON-NLS-1$

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

   //todo fb isn't it a deserialization ???? if yes, rename also in other classes
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
               "WeatherApiRetriever.deserializeWeatherData : Error while deserializing the historical weather JSON object :" //$NON-NLS-1$
                     + weatherDataResponse + "\n" + e.getMessage()); //$NON-NLS-1$
      }

      return newHistoryResult;
   }

   @Override
   public boolean retrieveHistoricalWeatherData() {

      historyResult = new HistoryResult();

      LocalDate requestedDate = tour.getTourStartTime().toLocalDate();

      final LocalDate tomorrow = LocalDate.now().plusDays(1);

      while (true) {

         //Send an API request as long as we don't have the results covering the entire duration of the tour
         final String weatherRequestWithParameters = buildWeatherApiRequest(requestedDate);

         final String rawWeatherData = sendWeatherApiRequest(weatherRequestWithParameters);
         if (StringUtils.isNullOrEmpty(rawWeatherData)) {
            return false;
         }

         final HistoryResult newHistoryResult = deserializeWeatherData(rawWeatherData);
         if (newHistoryResult == null) {
            return false;
         }

         historyResult.addAllHour(newHistoryResult.getForecastdayHourList());
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
