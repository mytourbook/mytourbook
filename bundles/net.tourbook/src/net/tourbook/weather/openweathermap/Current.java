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

@JsonIgnoreProperties(ignoreUnknown = true)
class Current {

   private float         temp;
   private float         feels_like;
   private int           pressure;
   private int           humidity;
   private float         wind_speed;
   private int           wind_deg;
   private Volume        rain;
   private Volume        snow;
   private List<Weather> weather;

   public float getFeels_like() {
      return feels_like;
   }

   public int getHumidity() {
      return humidity;
   }

   public float getPrecipitation() {

      if (getRain() == null) {
         return 0;
      }

      return (float) getRain().getOneHour();
   }

   public int getPressure() {
      return pressure;
   }

   public Volume getRain() {
      return rain;
   }

   public float getSnowfall() {

      if (snow == null) {
         return 0;
      }

      return (float) snow.getOneHour();
   }

   public float getTemp() {
      return temp;
   }

   public List<Weather> getWeather() {
      return weather;
   }

   public String getWeatherClouds() {

      if (getWeather() == null || getWeather().isEmpty()) {
         return UI.EMPTY_STRING;
      }

      return TimeMachineResult.convertWeatherTypeToMTWeatherClouds(getWeather().get(0).getId());
   }

   public String getWeatherDescription() {

      if (getWeather() == null || getWeather().isEmpty()) {
         return UI.EMPTY_STRING;
      }

      return getWeather().get(0).getDescription();
   }

   public int getWind_deg() {
      return wind_deg;
   }

   public int getWind_speed() {
      return Math.round(wind_speed);
   }
}
