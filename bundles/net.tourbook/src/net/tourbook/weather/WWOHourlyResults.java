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
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WWOHourlyResults {
   private String                 time;

   private String                 windspeedKmph;

   private String                 winddirDegree;

   private List<WWOValuesResults> weatherDesc;

   private String                 weatherCode;

   @JsonProperty("FeelsLikeC")
   private String                 FeelsLikeC;

   @JsonProperty("FeelsLikeF")
   private String                 FeelsLikeF;

   private String                 tempC;
   private String                 pressure;
   private int                    humidity;

   private String                 precipMM;

   public int getFeelsLikeC() {
      return Integer.parseInt(FeelsLikeC);
   }

   public String getFeelsLikeF() {
      return FeelsLikeF;
   }

   public int getHumidity() {
      return humidity;
   }

   public float getPrecipMM() {
      return Float.parseFloat(precipMM);
   }

   public int getPressure() {
      return Integer.parseInt(pressure);
   }

   public int getTempC() {
      return Integer.parseInt(tempC);
   }

   public String gettime() {
      return time;
   }

   public String getWeatherCode() {
      return weatherCode;
   }

   public List<WWOValuesResults> getWeatherDesc() {
      return weatherDesc;
   }

   public String getWeatherDescription(final WWOWeatherResults weatherResults) {
      return weatherDesc.get(0).getValue();
   }

   public String getWinddirDegree() {
      return winddirDegree;
   }

   public String getWindspeedKmph() {
      return windspeedKmph;
   }
}
