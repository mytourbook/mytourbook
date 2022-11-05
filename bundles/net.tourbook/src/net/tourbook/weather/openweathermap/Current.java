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
class Current {

   private int           dt;
   private int           sunrise;
   private int           sunset;
   private double        temp;
   private double        feels_like;
   private int           pressure;
   private int           humidity;
   private double        dew_point;
   private int           uvi;
   private int           clouds;
   private int           visibility;
   private double        wind_speed;
   private int           wind_deg;
   private int           wind_gust;
   private List<Weather> weather;

   public List<Weather> getWeather() {
      return weather;
   }
}
