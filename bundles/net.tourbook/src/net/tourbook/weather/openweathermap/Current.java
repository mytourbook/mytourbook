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

import java.util.List;

import net.tourbook.common.UI;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Current(float temp,
                      float feels_like,
                      int pressure,
                      int humidity,
                      float wind_speed,
                      int wind_deg,
                      Volume rain,
                      Volume snow,
                      List<Weather> weather) {

   public float getPrecipitation() {

      if (rain() == null) {
         return 0;
      }

      return (float) rain().oneHour();
   }

   public float getSnowfall() {

      if (snow == null) {
         return 0;
      }

      return (float) snow.oneHour();
   }

   public String getWeatherClouds() {

      if (weather() == null || weather().isEmpty()) {
         return UI.EMPTY_STRING;
      }

      return OpenWeatherMapRetriever.convertWeatherIconToMTWeatherClouds(weather().get(0).icon());
   }

   public String getWeatherDescription() {

      if (weather() == null || weather().isEmpty()) {
         return UI.EMPTY_STRING;
      }

      return weather().get(0).description();
   }

   public int getWind_speed() {
      return Math.round(wind_speed);
   }
}
