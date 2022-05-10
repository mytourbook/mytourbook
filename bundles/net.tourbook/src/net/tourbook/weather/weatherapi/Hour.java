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

public class Hour{
    public int time_epoch;
    public String time;
    public double temp_c;
    public double temp_f;
    public int is_day;
    public Condition condition;
    public double wind_mph;
    public double wind_kph;
    public int wind_degree;
    public String wind_dir;
    public double pressure_mb;
    public double pressure_in;
    public double precip_mm;
    public double precip_in;
    public int humidity;
    public int cloud;
    public double feelslike_c;
    public double feelslike_f;
    public double windchill_c;
    public double windchill_f;
    public double heatindex_c;
    public double heatindex_f;
    public double dewpoint_c;
    public double dewpoint_f;
    public int will_it_rain;
    public int chance_of_rain;
    public int will_it_snow;
    public int chance_of_snow;
    public double vis_km;
    public double vis_miles;
    public double gust_mph;
    public double gust_kph;
}
