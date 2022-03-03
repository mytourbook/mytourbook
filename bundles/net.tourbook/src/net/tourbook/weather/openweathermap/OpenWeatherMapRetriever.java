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
import com.javadocmd.simplelatlng.LatLng;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.weather.HistoricalWeatherRetriever;
import net.tourbook.weather.WeatherUtils;

import org.apache.http.client.utils.URIBuilder;

public class OpenWeatherMapRetriever extends HistoricalWeatherRetriever {

   //todo fb i might have to do more than 1 call as the results only ocntains the 24 hours around the given start time
   // that will work for most of the activities but not 100milers ....

   //todo fb this will be replaced by HEROKU
   private static final String baseApiUrl = "https://api.openweathermap.org/data/2.5/onecall/timemachine?"; //$NON-NLS-1$
   private LatLng              searchAreaCenter;
   private long                startDate;

   public OpenWeatherMapRetriever(final TourData tourData) {

      super(tourData);

      searchAreaCenter = WeatherUtils.determineWeatherSearchAreaCenter(_tour);

      //todo fb maybe this should be the middle date of the middle of the activity = (end - start ) /2 + start
      //but that still wouldnt work for long activities (100milers)
      //BUT that would give the hourly data correct (i.e.: sun type in the middle of the run instead of the beginning only, which
      //is kind of an average
      startDate = _tour.getTourStartTimeMS() / 1000;
   }

   private void logVendorError(final String weatherRequestWithParameters,
                               final int statusCode,
                               final String exceptionMessage) {

      final StringBuilder error = new StringBuilder();

      error.append(
            "WeatherHistoryRetriever.processRequest : Error while executing the historical weather request with the parameters " + //$NON-NLS-1$
                  weatherRequestWithParameters);

      if (statusCode > 0) {
         error.append(UI.NEW_LINE + "Status Code: " + statusCode);//$NON-NLS-1$
      }
      if (StringUtils.hasContent(exceptionMessage)) {
         error.append(exceptionMessage);
      }

      StatusUtil.logError(error.toString());
   }

   @Override
   public boolean retrieveHistoricalWeatherData() {

      final String rawWeatherData = sendWeatherApiRequest();
      if (StringUtils.isNullOrEmpty(rawWeatherData)) {
         return false;
      }

      final TimeMachineResult timeMachineResult = serializeWeatherData(rawWeatherData);

      if (timeMachineResult == null) {
         return false;
      }

      _tour.setIsWeatherDataFromApi(true);
      _tour.setWeather(timeMachineResult.getWeatherDescription());
      _tour.setWeatherClouds(timeMachineResult.getWeatherType());
      _tour.setWeather_Temperature_Average(timeMachineResult.getTemperatureAverage());
      _tour.setWeatherWindSpeed(timeMachineResult.getWindSpeed());
      _tour.setWeatherWindDir(timeMachineResult.getWindDirection());
      _tour.setWeather_Humidity((short) timeMachineResult.getAverageHumidity());
      _tour.setWeather_Precipitation(timeMachineResult.getPrecipitation());
      _tour.setWeather_Pressure((short) timeMachineResult.getAveragePressure());
      _tour.setWeather_Temperature_Max(timeMachineResult.getTemperatureMax());
      _tour.setWeather_Temperature_Min(timeMachineResult.getTemperatureMin());
      _tour.setWeather_Temperature_WindChill(timeMachineResult.getWindChill());

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

         //TODO FB heroku always use and expect units in metrics
         uriBuilder.setParameter("units", "metric");
         uriBuilder.setParameter("lat", String.valueOf(searchAreaCenter.getLatitude()));
         uriBuilder.setParameter("lon", String.valueOf(searchAreaCenter.getLongitude()));
         uriBuilder.setParameter("dt", String.valueOf(startDate));

         weatherRequestWithParameters = uriBuilder.build().toString();

         // NOTE :
         // This error below keeps popping up RANDOMLY and as of today, I haven't found a solution:
         // java.lang.NoClassDefFoundError: Could not initialize class sun.security.ssl.SSLContextImpl$CustomizedTLSContext
         // 2019/06/20 : To avoid this issue, we are using the HTTP address of WWO and not the HTTPS.

         final HttpRequest request = HttpRequest.newBuilder(
               URI.create(weatherRequestWithParameters)).GET().build();

         final HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

         weatherHistory = response.body();

         if (response.statusCode() != 200) {
            logVendorError(weatherRequestWithParameters, response.statusCode(), response.body());
            return UI.EMPTY_STRING;
         }

      } catch (final Exception ex) {

         logVendorError(weatherRequestWithParameters, 0, ex.getMessage());
         Thread.currentThread().interrupt();

         return UI.EMPTY_STRING;
      }

      return weatherHistory;
   }

   private TimeMachineResult serializeWeatherData(final String weatherDataResponse) {

      TimeMachineResult timeMachineResult = new TimeMachineResult();
      try {
         final ObjectMapper mapper = new ObjectMapper();
         final String weatherResults = mapper
               .readValue(weatherDataResponse, JsonNode.class)
               .toString();

         timeMachineResult = mapper.readValue(
               weatherResults,
               new TypeReference<TimeMachineResult>() {});

      } catch (final Exception e) {

         StatusUtil.logError(
               "WeatherHistoryRetriever.parseWeatherData : Error while parsing the historical weather JSON object :" //$NON-NLS-1$
                     + weatherDataResponse + "\n" + e.getMessage()); //$NON-NLS-1$
         return timeMachineResult;
      }

      return timeMachineResult;
   }
}
