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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A Java representation of a World Weather Online query result "weather" element.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WWOWeatherResults {

   private String                 maxtempC;

   private String                 mintempC;

   private String                 avgtempC;

   private List<WWOHourlyResults> hourly;

   public String getavgtempC() {
      return avgtempC;
   }

   public List<WWOHourlyResults> gethourly() {
      return hourly;
   }

   public String getmaxtempC() {
      return maxtempC;
   }

   public String getmintempC() {
      return mintempC;
   }
}
