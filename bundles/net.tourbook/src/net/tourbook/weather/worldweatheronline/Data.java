/*******************************************************************************
 * Copyright (C) 2019, 2023 Frédéric Bard
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
package net.tourbook.weather.worldweatheronline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import net.tourbook.common.UI;
import net.tourbook.weather.WeatherUtils;

/**
 * Class to store data from the WorldWeatherOnline API.
 * Documentation : https://www.worldweatheronline.com/developer/api/docs/historical-weather-api.aspx
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Data {

   @JsonProperty("nearest_area")
   private List<NearestArea> nearestArea;
   private List<Weather>     weather;

   private List<Hourly>      filteredHourly;
   private Hourly            middleHourly;

   private int               averageWindSpeed;
   private int               averageWindDirection;

   void computeAverageWindSpeedAndDirection() {

      final double[] windSpeeds = filteredHourly
            .stream()
            .mapToDouble(Hourly::getWindspeedKmph)
            .toArray();

      final int[] windDirections = filteredHourly
            .stream()
            .mapToInt(Hourly::getWinddirDegree)
            .toArray();

      final int[] averageWindSpeedAndDirection =
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections);

      averageWindSpeed = averageWindSpeedAndDirection[0];
      averageWindDirection = averageWindSpeedAndDirection[1];
   }

   /**
    * Filters and keeps only the values included between the tour start and end times.
    *
    * @return
    */
   boolean filterHourlyData(final long tourStartTime, final long tourEndTime) {

      //If the returned data is for more than 1 day, we combine the multiple hourly
      //lists into 1
      final List<Hourly> combinedHourly = new ArrayList<>();
      weather.forEach(currentWeather -> combinedHourly.addAll(currentWeather.getHourly()));

      filteredHourly = new ArrayList<>();
      for (final Hourly currentHourly : combinedHourly) {

         //The current data is not kept if its measured time is:
         // - more than 30 mins before the tour start time
         // OR
         // - more than 30 mins after the tour end time

         final long hourlyEpochSeconds = currentHourly.getEpochSeconds();
         if (hourlyEpochSeconds < tourStartTime - WeatherUtils.SECONDS_PER_THIRTY_MINUTE ||
               hourlyEpochSeconds > tourEndTime + WeatherUtils.SECONDS_PER_THIRTY_MINUTE) {
            continue;
         }

         filteredHourly.add(currentHourly);
      }

      return filteredHourly.size() > 0;
   }

   /**
    * Finds the hourly that is closest to the middle of the tour. This will be used
    * to determine the weather description of the tour.
    */
   void findMiddleHourly(final long tourMiddleTime) {

      if (filteredHourly == null) {
         return;
      }

      middleHourly = null;

      long timeDifference = Long.MAX_VALUE;
      for (final Hourly currentHourly : filteredHourly) {

         final long hourlyEpochSeconds = currentHourly.getEpochSeconds();
         final long currentTimeDifference = Math.abs(hourlyEpochSeconds - tourMiddleTime);

         if (currentTimeDifference < timeDifference) {

            middleHourly = currentHourly;
            timeDifference = currentTimeDifference;
         }
      }

   }

   public short getAverageHumidity() {

      final OptionalDouble averageHumidity =
            filteredHourly.stream().mapToDouble(Hourly::getHumidity).average();

      if (averageHumidity.isPresent()) {
         return (short) (averageHumidity.getAsDouble());
      }

      return 0;
   }

   public float getAveragePressure() {

      final OptionalDouble averagePressure =
            filteredHourly.stream().mapToDouble(Hourly::getPressure).average();

      if (averagePressure.isPresent()) {
         return WeatherUtils.roundDoubleToFloat(averagePressure.getAsDouble());
      }

      return 0;
   }

   public float getAverageWindChill() {

      final OptionalDouble averageWindChill =
            filteredHourly.stream().mapToDouble(Hourly::getFeelsLikeC).average();

      if (averageWindChill.isPresent()) {
         return WeatherUtils.roundDoubleToFloat(averageWindChill.getAsDouble());
      }

      return 0;
   }

   public int getAverageWindDirection() {

      return averageWindDirection;
   }

   public int getAverageWindSpeed() {

      return averageWindSpeed;
   }

   public List<Hourly> getFilteredHourly() {
      return filteredHourly;
   }

   public List<NearestArea> getNearestArea() {
      return nearestArea;
   }

   public float getTemperatureAverage() {

      final OptionalDouble averageTemperature =
            filteredHourly.stream().mapToDouble(Hourly::getTempC).average();

      if (averageTemperature.isPresent()) {
         return WeatherUtils.roundDoubleToFloat(averageTemperature.getAsDouble());
      }

      return 0;
   }

   public float getTemperatureMax() {

      final OptionalDouble maxTemperature =
            filteredHourly.stream().mapToDouble(Hourly::getTempC).max();

      if (maxTemperature.isPresent()) {
         return WeatherUtils.roundDoubleToFloat(maxTemperature.getAsDouble());
      }

      return 0;
   }

   public float getTemperatureMin() {

      final OptionalDouble minTemperature =
            filteredHourly.stream().mapToDouble(Hourly::getTempC).min();

      if (minTemperature.isPresent()) {
         return WeatherUtils.roundDoubleToFloat(minTemperature.getAsDouble());
      }

      return 0;
   }

   public float getTotalPrecipitation() {

      return WeatherUtils.roundDoubleToFloat(filteredHourly.stream().mapToDouble(Hourly::getPrecipMM).sum());
   }

   public List<Weather> getWeather() {
      return weather;
   }

   public String getWeatherDescription() {

      return middleHourly != null ? middleHourly.getWeatherDescription() : UI.EMPTY_STRING;
   }

   public String getWeatherType() {

      if (middleHourly == null) {
         return UI.EMPTY_STRING;
      }

      return WorldWeatherOnlineRetriever.convertWeatherCodeToMTWeatherClouds(middleHourly.getWeatherCode());
   }

}
