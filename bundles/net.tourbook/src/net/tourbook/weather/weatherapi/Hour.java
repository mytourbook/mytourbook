/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.weather.weatherapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Hour {

   private int       time_epoch;
   private String    time;
   private double    temp_c;
   private int       is_day;
   private Condition condition;
   private double    wind_kph;
   private int       wind_degree;
   private String    wind_dir;
   private double    pressure_mb;
   private double    precip_mm;
   private int       humidity;
   private int       cloud;
   private double    feelslike_c;
   private double    windchill_c;
   private double    dewpoint_c;
   private double    gust_kph;

   public int getCloud() {
      return cloud;
   }

   public Condition getCondition() {
      return condition;
   }

   public double getDewpoint_c() {
      return dewpoint_c;
   }

   public double getFeelslike_c() {
      return feelslike_c;
   }

   public double getGust_kph() {
      return gust_kph;
   }

   public int getHumidity() {
      return humidity;
   }

   public int getIs_day() {
      return is_day;
   }

   public double getPrecip_mm() {
      return precip_mm;
   }

   public double getPressure_mb() {
      return pressure_mb;
   }

   public double getTemp_c() {
      return temp_c;
   }

   public String getTime() {
      return time;
   }

   public int getTime_epoch() {
      return time_epoch;
   }

   public int getWind_degree() {
      return wind_degree;
   }

   public String getWind_dir() {
      return wind_dir;
   }

   public double getWind_kph() {
      return wind_kph;
   }

   public double getWindchill_c() {
      return windchill_c;
   }

}
