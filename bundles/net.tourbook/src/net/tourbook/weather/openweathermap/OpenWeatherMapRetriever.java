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
import java.util.ArrayList;
import java.util.List;

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

   private static final String HEROKU_APP_URL     = "https://passeur-mytourbook-oauthapps.herokuapp.com"; //$NON-NLS-1$
   private static final String baseApiUrl         = HEROKU_APP_URL + "/openweathermap/timemachine";       //$NON-NLS-1$

   private LatLng              searchAreaCenter;
   private long                date;

   private TimeMachineResult   _timeMachineResult = null;

   public OpenWeatherMapRetriever(final TourData tourData) {

      super(tourData);

      searchAreaCenter = WeatherUtils.determineWeatherSearchAreaCenter(tour);

      final long tourStartTime = tour.getTourStartTimeMS();
      final long tourMiddleTime = tourStartTime + ((tour.getTourEndTimeMS() - tourStartTime) / 2);
      date = tourMiddleTime / 1000;
   }

   public static String getBaseApiUrl() {
      return baseApiUrl;
   }

   @Override
   protected String buildFullWeatherDataString() {

      final long tourStartTime = tour.getTourStartTimeMS() / 1000;
      final long tourEndTime = tour.getTourEndTimeMS() / 1000;
      final long thirtyMinutes = 1800;

      final List<String> fullWeatherDataList = new ArrayList<>();

      for (final Hourly hourly : _timeMachineResult.getHourly()) {

         if (hourly.getDt() < tourStartTime - thirtyMinutes ||
               hourly.getDt() > tourEndTime + thirtyMinutes) {
            continue;
         }

         final TourDateTime tourDateTime = TimeTools.createTourDateTime(hourly.getDt() * 1000L, tour.getTimeZoneId());

         final String fullWeatherData = WeatherUtils.buildFullWeatherDataString(
               (float) hourly.getTemp(),
               (float) hourly.getFeels_like(),
               (float) hourly.getWind_speed(),
               hourly.getWind_deg(),
               hourly.getHumidity(),
               hourly.getRain(),
               hourly.getSnow(),
               tourDateTime.tourZonedDateTime.toEpochSecond(),
               tour.getTimeZoneId());

         fullWeatherDataList.add(fullWeatherData);
      }

      final String fullWeatherData = String.join(
            UI.SYSTEM_NEW_LINE,
            fullWeatherDataList)

            //  separate visually
            + UI.SYSTEM_NEW_LINE;

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

         uriBuilder.setParameter("units", "metric"); //$NON-NLS-1$ //$NON-NLS-2$
         uriBuilder.setParameter("lat", String.valueOf(searchAreaCenter.getLatitude())); //$NON-NLS-1$
         uriBuilder.setParameter("lon", String.valueOf(searchAreaCenter.getLongitude())); //$NON-NLS-1$
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

   @Override
   public boolean retrieveHistoricalWeatherData() {

      final String weatherRequestWithParameters = buildWeatherApiRequest();

      final String rawWeatherData = super.sendWeatherApiRequest(weatherRequestWithParameters);
      if (StringUtils.isNullOrEmpty(rawWeatherData)) {
         return false;
      }

      _timeMachineResult = serializeWeatherData(rawWeatherData);

      if (_timeMachineResult == null) {
         return false;
      }

// SET_FORMATTING_OFF

      tour.setIsWeatherDataFromProvider(true);
      tour.setWeather(                       _timeMachineResult.getWeatherDescription());
      tour.setWeather_Clouds(                _timeMachineResult.getWeatherType());
      tour.setWeather_Temperature_Average(   _timeMachineResult.getTemperatureAverage(tour));
      tour.setWeather_Wind_Speed(            _timeMachineResult.getWindSpeed(tour));
      tour.setWeather_Wind_Direction(        _timeMachineResult.getWindDirection(tour));
      tour.setWeather_Humidity((short)       _timeMachineResult.getAverageHumidity(tour));
      tour.setWeather_Precipitation(         _timeMachineResult.getPrecipitation(tour));
      tour.setWeather_Pressure((short)       _timeMachineResult.getAveragePressure(tour));
      tour.setWeather_Snowfall(              _timeMachineResult.getAverageSnowfall(tour));
      tour.setWeather_Temperature_Max(       _timeMachineResult.getTemperatureMax(tour));
      tour.setWeather_Temperature_Min(       _timeMachineResult.getTemperatureMin(tour));
      tour.setWeather_Temperature_WindChill( _timeMachineResult.getWindChill(tour));

// SET_FORMATTING_ON

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
               "OpenWeatherMapRetriever.serializeWeatherData : Error while serializing the historical weather JSON object :" //$NON-NLS-1$
                     + weatherDataResponse + "\n" + e.getMessage()); //$NON-NLS-1$
         return timeMachineResult;
      }

      return timeMachineResult;
   }
}
