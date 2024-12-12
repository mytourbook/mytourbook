/*******************************************************************************
 * Copyright (C) 2022, 2024 Frédéric Bard
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import net.tourbook.common.UI;
import net.tourbook.weather.WeatherUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
class TimeMachineResult {

   /*
    * Datapoints coming from the API response
    */
   private List<WeatherData> data;

   /*
    * Internal properties
    */
   private int         averageWindSpeed;
   private int         averageWindDirection;
   private WeatherData middleWeatherData;

   public TimeMachineResult() {
      data = new ArrayList<>();
   }

   /**
    * This adds new data manually to ensure only new data is added to the
    * current dataset.
    *
    * @param newData
    */
   void addAllData(final List<WeatherData> newData) {

      for (final WeatherData currentData : newData) {
         if (!data.contains(currentData)) {
            data.add(currentData);
         }
      }
   }

   void computeAverageWindSpeedAndDirection() {

      final double[] windSpeeds = data
            .stream()
            .mapToDouble(h -> h.getWind_speedKmph())
            .toArray();

      final int[] windDirections = data
            .stream()
            .mapToInt(h -> h.wind_deg())
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
   boolean filterWeatherData(final long tourStartTime, final long tourEndTime) {

      final List<WeatherData> filteredWeatherData = new ArrayList<>();

      for (final WeatherData currentData : data) {

         //The current data is not kept if its measured time is:
         // - more than 30 mins before the tour start time
         // OR
         // - more than 30 mins after the tour end time

         if (currentData.dt() < tourStartTime - WeatherUtils.SECONDS_PER_THIRTY_MINUTE ||
               currentData.dt() > tourEndTime + WeatherUtils.SECONDS_PER_THIRTY_MINUTE) {
            continue;
         }

         filteredWeatherData.add(currentData);
      }

      data = filteredWeatherData;

      return data.size() > 0;
   }

   /**
    * Finds the data that is closest to the middle of the tour. This will be used
    * to determine the weather description of the tour.
    */
   void findMiddleWeatherData(final long tourMiddleTime) {

      middleWeatherData = null;

      long timeDifference = Long.MAX_VALUE;
      for (final WeatherData currentData : data) {

         final long currentTimeDifference = Math.abs(currentData.dt() - tourMiddleTime);
         if (currentTimeDifference < timeDifference) {
            middleWeatherData = currentData;
            timeDifference = currentTimeDifference;
         }
      }

   }

   public float getAverageHumidity() {

      final OptionalDouble averageHumidity =
            data.stream().mapToDouble(h -> h.humidity()).average();

      if (averageHumidity.isPresent()) {
         return WeatherUtils.roundDoubleToFloat(averageHumidity.getAsDouble());
      }

      return 0;
   }

   public float getAveragePressure() {

      final OptionalDouble averagePressure =
            data.stream().mapToDouble(h -> h.pressure()).average();

      if (averagePressure.isPresent()) {
         return WeatherUtils.roundDoubleToFloat(averagePressure.getAsDouble());
      }

      return 0;
   }

   public float getAverageWindChill() {

      final OptionalDouble averageWindChill =
            data.stream().mapToDouble(h -> h.feels_like()).average();

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

   public List<WeatherData> getData() {
      return data;
   }

   private Weather getMiddleWeatherData() {

      final List<Weather> middleWeather = middleWeatherData.weather();
      if (middleWeather == null || middleWeather.isEmpty()) {
         return null;
      }

      return middleWeather.get(0);
   }

   public float getTemperatureAverage() {

      final OptionalDouble averageTemperature =
            data.stream().mapToDouble(h -> h.temp()).average();

      if (averageTemperature.isPresent()) {
         return WeatherUtils.roundDoubleToFloat(averageTemperature.getAsDouble());
      }

      return 0;
   }

   public float getTemperatureMax() {

      final OptionalDouble maxTemperature =
            data.stream().mapToDouble(h -> h.temp()).max();

      if (maxTemperature.isPresent()) {
         return WeatherUtils.roundDoubleToFloat(maxTemperature.getAsDouble());
      }

      return 0;
   }

   public float getTemperatureMin() {

      final OptionalDouble minTemperature =
            data.stream().mapToDouble(h -> h.temp()).min();

      if (minTemperature.isPresent()) {
         return WeatherUtils.roundDoubleToFloat(minTemperature.getAsDouble());
      }

      return 0;
   }

   public float getTotalPrecipitation() {

      return WeatherUtils.roundDoubleToFloat(data.stream().mapToDouble(h -> h.getRain()).sum());
   }

   public float getTotalSnowfall() {

      return WeatherUtils.roundDoubleToFloat(data.stream().mapToDouble(h -> h.getSnow()).sum());
   }

   public String getWeatherClouds() {

      final String weatherType = UI.EMPTY_STRING;

      final Weather middleWeather = getMiddleWeatherData();
      if (middleWeather == null) {
         return weatherType;
      }

      return OpenWeatherMapRetriever.convertWeatherIconToMTWeatherClouds(middleWeather.icon(), middleWeather.id());
   }

   public String getWeatherDescription() {

      final String weatherDescription = UI.EMPTY_STRING;

      final Weather middleWeather = getMiddleWeatherData();
      if (middleWeather == null) {
         return weatherDescription;
      }

      return middleWeather.description();
   }
}
