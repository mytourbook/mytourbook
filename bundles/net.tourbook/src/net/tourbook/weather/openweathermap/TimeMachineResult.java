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

import java.util.List;
import java.util.OptionalDouble;

import net.tourbook.common.weather.IWeather;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeMachineResult {

//TODO FB maybe useful to display in the log distance from tour to weather measurement ???   public double       lat;
//   public double       lon;
//   public String       timezone;
//   public int          timezone_offset;
   public Current      current;
   public List<Hourly> hourly;

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

   private Weather getCurrentWeather() {
      final Weather currentWeather = null;

      if (current == null) {
         return currentWeather;
      }

      final List<Weather> weather = current.weather;
      if (weather == null || weather.size() == 0) {
         return currentWeather;
      }

      return weather.get(0);
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

      return currentWeather.description;
   }

   public String getWeatherType() {

      String weatherType = UI.EMPTY_STRING;

      final Weather currentWeather = getCurrentWeather();
      if (currentWeather == null) {
         return weatherType;
      }

      // Codes : https://openweathermap.org/weather-conditions#Weather-Condition-Codes-2

      final int currentWeatherId = currentWeather.id;

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
         weatherType = "id: '" + currentWeather.id + "'," + //$NON-NLS-1$ //$NON-NLS-2$
               "main: '" + currentWeather.main + "'"; //$NON-NLS-1$ //$NON-NLS-2$
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
