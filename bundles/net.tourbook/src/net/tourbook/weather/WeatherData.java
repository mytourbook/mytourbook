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

import net.tourbook.common.weather.IWeather;

/**
 * Class to store data from the WorldWeatherOnline API.
 * Documentation : https://www.worldweatheronline.com/developer/api/docs/historical-weather-api.aspx
 */
public class WeatherData {

   private int    maxTemperature;
   private int    minTemperature;
   private int    averageTemperature;
   private int    WindDirection;
   private int    WindSpeed;
   private float  precipitation;
   private String WeatherDescription;
   private String WeatherType;
   private int    averageHumidity;
   //TODO TourWeatherData class name
   // or average{Property} name ?
   private int    averagePressure;
   private int windChill;

   public int getAverageHumidity() {
      return averageHumidity;
   }

   public int getAveragePressure() {
      return averagePressure;
   }

   public float getPrecipitation() {
      return precipitation;
   }

   public int getTemperatureAverage() {
      return averageTemperature;
   }

   public int getTemperatureMax() {
      return maxTemperature;
   }

   public int getTemperatureMin() {
      return minTemperature;
   }

   public String getWeatherDescription() {
      return WeatherDescription;
   }

   public String getWeatherType() {
      return WeatherType;
   }

   public int getWindChill() {
      return windChill;
   }

   public int getWindDirection() {
      return WindDirection;
   }

   public int getWindSpeed() {
      return WindSpeed;
   }

   public void setAverageHumidity(final int averageHumidity) {
      this.averageHumidity = averageHumidity;
   }

   public void setAveragePressure(final int averagePressure) {
      this.averagePressure = averagePressure;
   }

   public void setPrecipitation(final float precipitation) {
      this.precipitation = precipitation;
   }

   public void setTemperatureAverage(final int temperatureAverage) {
      averageTemperature = temperatureAverage;
   }

   public void setTemperatureMax(final int temperatureMax) {
         maxTemperature = temperatureMax;
   }

   public void setTemperatureMin(final int temperatureMin) {
         minTemperature = temperatureMin;
   }

   public void setWeatherDescription(final String weatherDescription) {
      WeatherDescription = weatherDescription;
   }

   public void setWeatherType(final String weatherCode) {
      // Codes : http://www.worldweatheronline.com/feed/wwoConditionCodes.xml
      switch (weatherCode) {
      case "122":
         WeatherType = IWeather.WEATHER_ID_OVERCAST;
         break;
      case "113":
         WeatherType = IWeather.WEATHER_ID_CLEAR;
         break;
      case "116":
         WeatherType = IWeather.WEATHER_ID_PART_CLOUDS;
         break;
      //case "200":
      //    WeatherType = IWeather.WEATHER_ID_LIGHTNING;
      //   break;
      case "293":
      case "296":
      case "299":
      case "302":
      case "305":
      case "308":
      case "356":
      case "359":
         WeatherType = IWeather.WEATHER_ID_RAIN;
         break;
      case "332":
      case "335":
      case "329":
      case "326":
      case "323":
      case "320":
         WeatherType = IWeather.WEATHER_ID_SNOW;
         break;
      case "200":
         WeatherType = IWeather.WEATHER_ID_SEVERE_WEATHER_ALERT;
         break;
      case "353":
         WeatherType = IWeather.WEATHER_ID_SCATTERED_SHOWERS;
         break;
      }
   }

   public void setWindChill(final int windChill) {
      this.windChill = windChill;
   }

   public void setWindDirection(final int windDirection) {
      WindDirection = windDirection;
   }

   public void setWindSpeed(final int windSpeed) {
      WindSpeed = windSpeed;
   }
}
