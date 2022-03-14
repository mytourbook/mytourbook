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

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Hourly {

   private int           dt;
   private double        temp;
   private double        feels_like;
   private int           pressure;
   private int           humidity;
   private List<Weather> weather;
   private double        wind_speed;
   private int           wind_deg;
   private Volume        rain;
   private Volume        snow;

   public int getDt() {
      return dt;
   }

   public double getFeels_like() {
      return feels_like;
   }

   public int getHumidity() {
      return humidity;
   }

   public int getPressure() {
      return pressure;
   }

   public float getRain() {

      if (rain == null) {
         return 0f;
      }

      return (float) rain.getOneHour();
   }

   public float getSnow() {

      if (snow == null) {
         return 0f;
      }

      return (float) snow.getOneHour();
   }

   public double getTemp() {
      return temp;
   }

   public List<Weather> getWeather() {
      return weather;
   }

   public int getWind_deg() {
      return wind_deg;
   }

   public double getWind_speed() {
      return wind_speed;
   }

   public double getWind_speedKmph() {
      return wind_speed * 3.6;
   }
}
