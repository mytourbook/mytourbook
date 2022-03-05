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
import java.net.URISyntaxException;

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

   //todo fb maybe this should be the middle date of the middle of the activity = (end - start ) /2 + start
   //but that still wouldnt work for long activities (100milers)
   //BUT that would give the hourly data correct (i.e.: sun type in the middle of the run instead of the beginning only, which
   //is kind of an average
   //if tour > 12 hours (11? 13?), then we need >= 2 API calls
   // startDate = tour.getTourStartTimeMS() / 1000;

   private static final String HEROKU_APP_URL = "https://passeur-mytourbook-oauthapps.herokuapp.com"; //$NON-NLS-1$
   private static final String baseApiUrl     = HEROKU_APP_URL + "/openweathermap/timemachine";       //$NON-NLS-1$

   private LatLng              searchAreaCenter;
   private long                startDate;

   public OpenWeatherMapRetriever(final TourData tourData) {

      super(tourData);

      searchAreaCenter = WeatherUtils.determineWeatherSearchAreaCenter(tour);

      startDate = tour.getTourStartTimeMS() / 1000;
   }

   public static String getBaseApiUrl() {
      return baseApiUrl;
   }

   private String buildWeatherApiRequest() {

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
         uriBuilder.setParameter("dt", String.valueOf(startDate)); //$NON-NLS-1$
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

   @Override
   public boolean retrieveHistoricalWeatherData() {

      final String weatherRequestWithParameters = buildWeatherApiRequest();

      final String rawWeatherData = super.sendWeatherApiRequest(weatherRequestWithParameters);
      if (StringUtils.isNullOrEmpty(rawWeatherData)) {
         return false;
      }

      final TimeMachineResult timeMachineResult = serializeWeatherData(rawWeatherData);

      if (timeMachineResult == null) {
         return false;
      }

      tour.setIsWeatherDataFromApi(true);
      tour.setWeather(timeMachineResult.getWeatherDescription());
      tour.setWeather_Clouds(timeMachineResult.getWeatherType());
      tour.setWeather_Temperature_Average(timeMachineResult.getTemperatureAverage());
      tour.setWeather_Wind_Speed(timeMachineResult.getWindSpeed());
      tour.setWeather_Wind_Direction(timeMachineResult.getWindDirection());
      tour.setWeather_Humidity((short) timeMachineResult.getAverageHumidity());
      tour.setWeather_Precipitation(timeMachineResult.getPrecipitation());
      tour.setWeather_Pressure((short) timeMachineResult.getAveragePressure());
      tour.setWeather_Temperature_Max(timeMachineResult.getTemperatureMax());
      tour.setWeather_Temperature_Min(timeMachineResult.getTemperatureMin());
      tour.setWeather_Temperature_WindChill(timeMachineResult.getWindChill());

      return true;
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
