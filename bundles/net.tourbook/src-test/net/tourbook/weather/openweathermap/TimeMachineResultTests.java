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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.tourbook.common.weather.IWeather;

import org.junit.jupiter.api.Test;

public class TimeMachineResultTests {

   @Test
   void openWeatherMap_WeatherTypeMapping_AllValues() {

      assertAll(
            () -> assertEquals(IWeather.WEATHER_ID_LIGHTNING,
                  TimeMachineResult.convertWeatherTypeToMTWeatherClouds(210)),
            () -> assertEquals(IWeather.WEATHER_ID_SCATTERED_SHOWERS,
                  TimeMachineResult.convertWeatherTypeToMTWeatherClouds(522)),
            () -> assertEquals(IWeather.WEATHER_ID_SEVERE_WEATHER_ALERT,
                  TimeMachineResult.convertWeatherTypeToMTWeatherClouds(771)),
            () -> assertEquals(IWeather.WEATHER_ID_CLEAR,
                  TimeMachineResult.convertWeatherTypeToMTWeatherClouds(800)));
   }
}
