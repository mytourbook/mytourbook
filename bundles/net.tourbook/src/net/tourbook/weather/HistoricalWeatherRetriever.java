/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.format.DateTimeFormatter;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * A class that retrieves, for a given track, the historical weather data.
 */
public class HistoricalWeatherRetriever {

   private TourData               tour;

   private WeatherData            historicalWeatherData;

   private final String           apiUrl     = "https://api.worldweatheronline.com/premium/v1/past-weather.ashx?key="; //$NON-NLS-1$

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   /*
    * @param tour
    * The tour for which we need to retrieve the weather data.
    */
   public HistoricalWeatherRetriever(final TourData tour) {
      this.tour = tour;
   }

   /**
    * Determines the geographic area covered by a GPS track. The goal is to
    * encompass most of the track to search a weather station as close as possible
    * to the overall course and not just to a specific point.
    */
   private LatLng determineWeatherSearchArea() {
      // Looking for the farthest point of the track
      double maxDistance = Double.MIN_VALUE;
      LatLng furthestPoint = null;
      final LatLng startPoint = new LatLng(tour.latitudeSerie[0], tour.longitudeSerie[0]);
      for (int index = 1; index < tour.latitudeSerie.length && index < tour.longitudeSerie.length; ++index) {
         final LatLng currentPoint = new LatLng(tour.latitudeSerie[index], tour.longitudeSerie[index]);

         final double distanceFromStart = LatLngTool.distance(startPoint, currentPoint, LengthUnit.METER);

         if (distanceFromStart > maxDistance) {
            maxDistance = distanceFromStart;
            furthestPoint = currentPoint;
         }
      }

      final double distanceFromStart = LatLngTool.distance(startPoint, furthestPoint, LengthUnit.METER);
      final double bearingBetweenPoint = LatLngTool.initialBearing(startPoint, furthestPoint);

      // We find the center of the circle formed by the starting point and the farthest point
      final LatLng searchAreaCenter = LatLngTool.travel(startPoint, bearingBetweenPoint, distanceFromStart / 2, LengthUnit.METER);

      return searchAreaCenter;
   }

   public WeatherData getHistoricalWeatherData() {
      return historicalWeatherData;
   }

   /**
    * Parses a JSON weather data object into a WeatherData object.
    *
    * @param weatherDataResponse
    *           A string containing a historical weather data JSON object.
    * @return The parsed weather data.
    */
   private WeatherData parseWeatherData(final String weatherDataResponse) {

      final WeatherData weatherData = new WeatherData();
      try {
         final ObjectMapper mapper = new ObjectMapper();
         final String weatherResults = mapper.readValue(weatherDataResponse, JsonNode.class).get("data").get("weather").get(0).toString(); //$NON-NLS-1$ //$NON-NLS-2$

         final WWOWeatherResults rawWeatherData = mapper.readValue(weatherResults, WWOWeatherResults.class);
         final double roundedStartTimeOfDay = Math.ceil(tour.getStartTimeOfDay() / 3600.0);
         final String startTimeOfDay = (int) roundedStartTimeOfDay + "00"; //$NON-NLS-1$

         // Within the hourly data, find the time that corresponds to the tour start time
         // and extract the weather data.
         for (final WWOHourlyResults hourlyData : rawWeatherData.gethourly()) {
            if (hourlyData.gettime().equals(startTimeOfDay)) {
               weatherData.setWindDirection(Integer.parseInt(hourlyData.getWinddirDegree()));
               weatherData.setWindSpeed(Integer.parseInt(hourlyData.getWindspeedKmph()));
               weatherData.setWeatherDescription(hourlyData.getWeatherDescription(rawWeatherData));
               weatherData.setWeatherType(hourlyData.getWeatherCode());
               break;
            }
         }

         //TODO
         // Request data for the beginning AND the end of the tour in order to generate the below data
         weatherData.setTemperatureMax(rawWeatherData.getmaxtempC());
         weatherData.setTemperatureMin(rawWeatherData.getmintempC());
         weatherData.setTemperatureAverage(rawWeatherData.getavgtempC());

      } catch (final IOException e) {
         StatusUtil.log(
               "WeatherHistoryRetriever.parseWeatherData : Error while parsing the historical weather JSON object :" //$NON-NLS-1$
                     + weatherData + "\n" + e.getMessage()); //$NON-NLS-1$
      }

      return weatherData;
   }

   /**
    * Processes a query against the weather API.
    *
    * @return The result of the weather API query.
    */
   private String processRequest() {
      final LatLng searchAreaCenter = determineWeatherSearchArea();
      final String startDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(tour.getTourStartTime()); //$NON-NLS-1$

      final String weatherRequestParameters = apiUrl + _prefStore.getString(ITourbookPreferences.API_KEY) + "&q=" + searchAreaCenter.getLatitude() //$NON-NLS-1$
            + "," + searchAreaCenter.getLongitude() //$NON-NLS-1$
            + "&date=" + startDate + "&tp=1&format=json"; //$NON-NLS-1$ //$NON-NLS-2$
      //tp=1 : Specifies the weather forecast time interval in hours. Here, every 1 hour

      BufferedReader rd = null;
      final StringBuffer weatherHistory = new StringBuffer();
      try {
         final HttpClient client = HttpClientBuilder.create().build();
         final HttpGet request = new HttpGet(weatherRequestParameters);
         final HttpResponse response = client.execute(request);

         rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

         String line = ""; //$NON-NLS-1$
         while ((line = rd.readLine()) != null) {
            weatherHistory.append(line);
         }
      } catch (final Exception ex) {
         StatusUtil.log(
               "WeatherHistoryRetriever.processRequest : Error while executing the historical weather request with the parameters " //$NON-NLS-1$
                     + weatherRequestParameters + "\n" + ex.getMessage()); //$NON-NLS-1$
         return ""; //$NON-NLS-1$
      } finally {
         try {
            // close resources
            if (rd != null) {
               rd.close();
            }

         } catch (final IOException e) {
            e.printStackTrace();
         }
      }

      return weatherHistory.toString();
   }

   /**
    * This method retrieves, if found, the weather data.
    */
   public HistoricalWeatherRetriever retrieve() {
      historicalWeatherData = retrieveHistoricalWeatherData();

      return this;
   }

   /**
    * Retrieves the historical weather data
    *
    * @return The weather data, if found.
    */
   private WeatherData retrieveHistoricalWeatherData() {

      final String rawWeatherData = processRequest();
      if (!rawWeatherData.contains("weather")) { //$NON-NLS-1$
         return null;
      }
      final WeatherData historicalWeatherData = parseWeatherData(rawWeatherData);
      return historicalWeatherData;
   }
}
