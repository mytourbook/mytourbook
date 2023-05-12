/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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
import java.util.OptionalDouble;
import java.util.function.ToDoubleFunction;

@JsonIgnoreProperties(ignoreUnknown = true)
class AirPollutionResult {

   private List<net.tourbook.weather.openweathermap.List> list;

   public int getAirQualityIndexAverage() {

      final ToDoubleFunction<net.tourbook.weather.openweathermap.List> listFunction =
            listElement -> listElement.getMain().getAqi();

      final OptionalDouble averageAirQualityIndex =
            getList().stream().mapToDouble(listFunction).average();

      if (averageAirQualityIndex.isPresent()) {
         return (int) Math.round(averageAirQualityIndex.getAsDouble());
      }

      return 0;
   }

   public List<net.tourbook.weather.openweathermap.List> getList() {
      return list;
   }
}
