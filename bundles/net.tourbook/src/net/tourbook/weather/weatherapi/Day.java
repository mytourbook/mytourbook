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
package net.tourbook.weather.weatherapi;

public class Day {
   public double    maxtemp_c;
   public double    maxtemp_f;
   public double    mintemp_c;
   public double    mintemp_f;
   public double    avgtemp_c;
   public double    avgtemp_f;
   public double    maxwind_mph;
   public double    maxwind_kph;
   public double    totalprecip_mm;
   public double    totalprecip_in;
   public double    avgvis_km;
   public double    avgvis_miles;
   public double    avghumidity;
   public Condition condition;
   public double    uv;

}
