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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
   private LatLng                 searchAreaCenter;
   private String                 startDate;
   private String                 startTime;
   private String                 endTime;

   private WeatherData            historicalWeatherData;

   private final String           apiUrl     = "https://api.worldweatheronline.com/premium/v1/past-weather.ashx?key="; //$NON-NLS-1$

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   /*
    * @param tour
    * The tour for which we need to retrieve the weather data.
    */
   public HistoricalWeatherRetriever(final TourData tour) {
      this.tour = tour;

      searchAreaCenter = determineWeatherSearchArea();
      startDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(tour.getTourStartTime()); //$NON-NLS-1$

      final double roundedStartTime = tour.getTourStartTime().getHour();
      startTime = (int) roundedStartTime + "00"; //$NON-NLS-1$


      int roundedEndHour = Instant.ofEpochMilli(tour.getTourEndTimeMS()).atZone(tour.getTimeZoneIdWithDefault()).getHour();
      final int roundedEndMinutes = Instant.ofEpochMilli(tour.getTourEndTimeMS()).atZone(tour.getTimeZoneIdWithDefault()).getMinute();
      if (roundedEndMinutes >= 30) {
         ++roundedEndHour;
      }
      endTime = roundedEndHour + "00"; //$NON-NLS-1$
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
         final String weatherResults = mapper.readValue(weatherDataResponse, JsonNode.class)
               .get("data") //$NON-NLS-1$
               .get("weather") //$NON-NLS-1$
               .get(0)
               .get("hourly") //$NON-NLS-1$
               .toString();

         final List<WWOHourlyResults> rawWeatherData = mapper.readValue(weatherResults, new TypeReference<List<WWOHourlyResults>>() {});

         float totalPrecipitation = 0f;
         boolean isTourStartData = false;
         boolean isTourEndData = false;
         int numHourlyDatasets = 0;
         int sumHumidity = 0;
         int sumPressure = 0;
         int sumWindChill = 0;
         int sumTemperature = 0;
         int maxTemperature = Integer.MIN_VALUE;
         int minTemperature = Integer.MAX_VALUE;

         for (final WWOHourlyResults hourlyData : rawWeatherData) {
            // Within the hourly data, find the times that corresponds to the tour time
            // and extract all the weather data.
            if (hourlyData.gettime().equals(startTime)) {
               isTourStartData = true;
               weatherData.setWeatherDescription(hourlyData.getWeatherDescription());
            }
            if (hourlyData.gettime().equals(endTime)) {
               isTourEndData = true;
            }

            if (isTourStartData || isTourEndData) {
               weatherData.setWindDirection(Integer.parseInt(hourlyData.getWinddirDegree()));
               weatherData.setWindSpeed(Integer.parseInt(hourlyData.getWindspeedKmph()));
               sumHumidity += hourlyData.getHumidity();
               totalPrecipitation += hourlyData.getPrecipMM();
               sumPressure += hourlyData.getPressure();
               sumWindChill += hourlyData.getFeelsLikeC();
               weatherData.setWeatherType(hourlyData.getWeatherCode());
               sumTemperature += hourlyData.getTempC();

               if (hourlyData.getTempC() < minTemperature) {
                  minTemperature  = hourlyData.getTempC();
               }

               if (hourlyData.getTempC() > maxTemperature) {
                  maxTemperature = hourlyData.getTempC();
               }

               ++numHourlyDatasets;
               if (isTourEndData) {
                  break;
               }
            }
         }

         weatherData.setTemperatureMax(maxTemperature);
         weatherData.setTemperatureMin(minTemperature);
         weatherData.setTemperatureAverage((int) Math.ceil((double) sumTemperature / (double) numHourlyDatasets));
         weatherData.setWindChill((int) Math.ceil((double) sumWindChill / (double) numHourlyDatasets));
         weatherData.setAverageHumidity((int) Math.ceil((double) sumHumidity / (double) numHourlyDatasets));
         weatherData.setAveragePressure((int) Math.ceil((double) sumPressure / (double) numHourlyDatasets));
         weatherData.setPrecipitation(totalPrecipitation);

      } catch (final IOException e) {
         StatusUtil.log(
               "WeatherHistoryRetriever.parseWeatherData : Error while parsing the historical weather JSON object :" //$NON-NLS-1$
                     + weatherData + "\n" + e.getMessage()); //$NON-NLS-1$
      }

      return weatherData;
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

      final String rawWeatherData = sendWeatherApiRequest();
      if (!rawWeatherData.contains("weather")) { //$NON-NLS-1$
         return null;
      }
      final WeatherData historicalWeatherData = parseWeatherData(rawWeatherData);
      return historicalWeatherData;
   }

   /**
    * Processes a query against the weather API.
    *
    * @return The result of the weather API query.
    */
   private String sendWeatherApiRequest() {
      final String weatherRequestWithParameters = apiUrl + _prefStore.getString(ITourbookPreferences.API_KEY) + "&q=" + searchAreaCenter.getLatitude() //$NON-NLS-1$
            + "," + searchAreaCenter.getLongitude() //$NON-NLS-1$
            + "&date=" + startDate + "&tp=1&format=json"; //$NON-NLS-1$ //$NON-NLS-2$
      //tp=1 : Specifies the weather forecast time interval in hours. Here, every 1 hour

      BufferedReader rd = null;
      InputStreamReader isr = null;
      final StringBuffer weatherHistory = new StringBuffer();
      try {
         // NOTE :
         // This error below keeps popping up RANDOMLY and as of today, I haven't found a solution:
         // java.lang.NoClassDefFoundError: Could not initialize class sun.security.ssl.SSLContextImpl$CustomizedTLSContext

         final HttpClientBuilder clientBuilder = HttpClientBuilder.create();
         final HttpClient client = clientBuilder.build();
         final HttpGet request = new HttpGet(weatherRequestWithParameters);
         final HttpResponse response = client.execute(request);
         isr = new InputStreamReader(response.getEntity().getContent());
         rd = new BufferedReader(isr);

         String line = ""; //$NON-NLS-1$
         while ((line = rd.readLine()) != null) {
            weatherHistory.append(line);
         }
      } catch (final Exception ex) {
         StatusUtil.log(
               "WeatherHistoryRetriever.processRequest : Error while executing the historical weather request with the parameters " //$NON-NLS-1$
                     + weatherRequestWithParameters + "\n" + ex.getMessage()); //$NON-NLS-1$
         return ""; //$NON-NLS-1$
      } finally {
         try {
            // close resources
            if (rd != null) {
               rd.close();
            }
            if (isr != null) {
               isr.close();
            }
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }

      return weatherHistory.toString();
   }
}
