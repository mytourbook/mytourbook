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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.byteholder.geoclipse.map.UI;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import net.tourbook.common.weather.IWeather;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeMachineResult {

   private List<Hourly> hourly;

   private Hourly       middleHourly;

   public TimeMachineResult() {
      hourly = new ArrayList<>();
   }

   /**
    * This adds new hourly data manually to ensure only new data is added to the
    * current dataset.
    *
    * @param newHourly
    */
   public void addAllHourly(final List<Hourly> newHourly) {

      for (final Hourly currentHourly : newHourly) {
         if (!hourly.contains(currentHourly)) {
            hourly.add(currentHourly);
         }
      }
   }

   /**
    * Filters and keeps only the values included between the tour start and end times.
    *
    * @return
    */
   public void filterHourlyData(final long tourStartTime, final long tourEndTime) {

      final List<Hourly> filteredHourlyData = new ArrayList<>();

      for (final Hourly currentHourly : hourly) {

         //The current data is not kept if its measured time is:
         // - more than 30 mins before the tour start time
         // OR
         // - more than 30 mins after the tour end time

         final long thirtyMinutes = 1800;

         if (currentHourly.getDt() < tourStartTime - thirtyMinutes ||
               currentHourly.getDt() > tourEndTime + thirtyMinutes) {
            continue;
         }

         filteredHourlyData.add(currentHourly);
      }

      hourly = filteredHourlyData;

   }

   /**
    * Finds the hourly that is closest to the middle of the tour. This will be used
    * to determine the weather description of the tour.
    */
   public void findMiddleHourly(final long tourMiddleTime) {

      middleHourly = null;

      long timeDifference = Long.MAX_VALUE;
      for (final Hourly currentHourly : hourly) {

         final long currentTimeDifference = Math.abs(currentHourly.getDt() - tourMiddleTime);
         if (currentTimeDifference < timeDifference) {
            middleHourly = currentHourly;
            timeDifference = currentTimeDifference;
         }
      }

   }

   public float getAverageHumidity() {

      final OptionalDouble averageHumidity =
            hourly.stream().mapToDouble(Hourly::getHumidity).average();

      if (averageHumidity.isPresent()) {
         return roundDoubleToFloat(averageHumidity.getAsDouble());
      }

      return 0;
   }

   public float getAveragePressure() {

      final OptionalDouble averagePressure =
            hourly.stream().mapToDouble(Hourly::getPressure).average();

      if (averagePressure.isPresent()) {
         return roundDoubleToFloat(averagePressure.getAsDouble());
      }

      return 0;
   }

   public float getAverageSnowfall() {

      final OptionalDouble averageSnowfall =
            hourly.stream().mapToDouble(Hourly::getSnow).average();

      if (averageSnowfall.isPresent()) {
         return roundDoubleToFloat(averageSnowfall.getAsDouble());
      }

      return 0;
   }

   private Weather getCurrentWeather() {

      final List<Weather> currentWeather = middleHourly.getWeather();
      if (currentWeather == null || currentWeather.size() == 0) {
         return null;
      }

      return currentWeather.get(0);
   }

   public List<Hourly> getHourly() {
      return hourly;
   }

   public float getPrecipitation() {

      final OptionalDouble averagePrecipitation =
            hourly.stream().mapToDouble(Hourly::getRain).average();

      if (averagePrecipitation.isPresent()) {
         return roundDoubleToFloat(averagePrecipitation.getAsDouble());
      }

      return 0;
   }

   public float getTemperatureAverage() {

      final OptionalDouble averageTemperature =
            hourly.stream().mapToDouble(Hourly::getTemp).average();

      if (averageTemperature.isPresent()) {
         return roundDoubleToFloat(averageTemperature.getAsDouble());
      }

      return 0;
   }

   public float getTemperatureMax() {

      final OptionalDouble maxTemperature =
            hourly.stream().mapToDouble(Hourly::getTemp).max();

      if (maxTemperature.isPresent()) {
         return roundDoubleToFloat(maxTemperature.getAsDouble());
      }

      return 0;
   }

   public float getTemperatureMin() {

      final OptionalDouble minTemperature =
            hourly.stream().mapToDouble(Hourly::getTemp).min();

      if (minTemperature.isPresent()) {
         return roundDoubleToFloat(minTemperature.getAsDouble());
      }

      return 0;
   }

   public String getWeatherDescription() {

      final String weatherDescription = UI.EMPTY_STRING;

      final Weather currentWeather = getCurrentWeather();
      if (currentWeather == null) {
         return weatherDescription;
      }

      return currentWeather.getDescription();
   }

   public String getWeatherType() {

      String weatherType = IWeather.cloudIsNotDefined;

      final Weather currentWeather = getCurrentWeather();
      if (currentWeather == null) {
         return weatherType;
      }

      // Codes : https://openweathermap.org/weather-conditions#Weather-Condition-Codes-2

      final int currentWeatherId = currentWeather.getId();

      if (currentWeatherId >= 200 && currentWeatherId < 300) {
         weatherType = IWeather.WEATHER_ID_LIGHTNING;
      } else if (currentWeatherId >= 300 && currentWeatherId < 400) {
         weatherType = IWeather.WEATHER_ID_SCATTERED_SHOWERS;
      } else if (currentWeatherId >= 500 && currentWeatherId < 600) {
         weatherType = IWeather.WEATHER_ID_RAIN;
      } else if (currentWeatherId >= 600 && currentWeatherId < 700) {
         weatherType = IWeather.WEATHER_ID_SNOW;
      } else if (currentWeatherId == 800) {
         weatherType = IWeather.WEATHER_ID_CLEAR;
      } else if (currentWeatherId == 801 || currentWeatherId == 802) {
         weatherType = IWeather.WEATHER_ID_PART_CLOUDS;
      } else if (currentWeatherId == 803 || currentWeatherId == 804) {
         weatherType = IWeather.WEATHER_ID_OVERCAST;
      } else if (currentWeatherId == 711 || currentWeatherId == 762 ||
            currentWeatherId == 771 || currentWeatherId == 781) {
         weatherType = IWeather.WEATHER_ID_SEVERE_WEATHER_ALERT;
      } else {
         weatherType = "id: '" + currentWeatherId + "'," + //$NON-NLS-1$ //$NON-NLS-2$
               "main: '" + currentWeather.getMain() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
      }

      return weatherType;
   }

   public float getWindChill() {

      final OptionalDouble averageWindChill =
            hourly.stream().mapToDouble(Hourly::getFeels_like).average();

      if (averageWindChill.isPresent()) {
         return roundDoubleToFloat(averageWindChill.getAsDouble());
      }

      return 0;
   }

   public int getWindDirection() {

      final OptionalDouble averageWindDirection =
            hourly.stream().mapToDouble(Hourly::getWind_deg).average();

      if (averageWindDirection.isPresent()) {
         return (int) Math.round(averageWindDirection.getAsDouble());
      }

      return 0;
   }

   public int getWindSpeed() {

      final OptionalDouble averageWindSpeed =
            hourly.stream().mapToDouble(Hourly::getWind_speed).average();

      if (averageWindSpeed.isPresent()) {
         return (int) Math.round(averageWindSpeed.getAsDouble());
      }

      return 0;
   }

   private float roundDoubleToFloat(final double value) {

      return Math.round(value * 100.0) / 100.0f;
   }

}
