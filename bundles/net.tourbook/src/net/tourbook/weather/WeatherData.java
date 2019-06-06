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

/**
 * Class to store data from the WorldWeatherOnline API.
 * Documentation : https://www.worldweatheronline.com/developer/api/docs/historical-weather-api.aspx
 */
public class WeatherData {

   private float maxTemperature;
   private float minTemperature;
   private float AverageTemperature;

   public WeatherData() {
      maxTemperature = Float.MIN_VALUE;
      minTemperature = Float.MIN_VALUE;
      AverageTemperature = Float.MIN_VALUE;
   }

   public float getTemperatureAverage() {
      return AverageTemperature;
   }

   public float getTemperatureMax() {
      return maxTemperature;
   }

   public float getTemperatureMin() {
      return minTemperature;
   }

   public void setTemperatureAverage(final String temperatureAverage) {
      if (!temperatureAverage.equals("")) {
         AverageTemperature = Float.parseFloat(temperatureAverage);
      }
   }

   public void setTemperatureMax(final String temperatureMax) {
      if (!temperatureMax.equals("")) {
         maxTemperature = Float.parseFloat(temperatureMax);
      }
   }

   public void setTemperatureMin(final String temperatureMin) {
      if (!temperatureMin.equals("")) {
         minTemperature = Float.parseFloat(temperatureMin);
      }
   }
}
