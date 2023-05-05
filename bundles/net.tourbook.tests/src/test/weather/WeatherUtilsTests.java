/*******************************************************************************
 * Copyright (C) 2022, 2023 Frédéric Bard
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
package weather;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import net.tourbook.common.UI;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.weather.WeatherUtils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class WeatherUtilsTests {
   @BeforeAll
   static void initAll() {

      FormatManager.updateDisplayFormats();
   }

   @Test
   void testBuildFullWeatherDataString() {

      final String andorraZoneId = "Europe/Andorra"; //$NON-NLS-1$
      final ZonedDateTime zonedDateTime = ZonedDateTime.of(
            2022,
            05,
            16,
            17,
            46,
            10,
            0,
            ZoneId.of(andorraZoneId));

      final String fullWeatherDataString = WeatherUtils.buildFullWeatherDataString(6.09f,
            "⛅", //$NON-NLS-1$
            "Partly cloudy", //$NON-NLS-1$
            8.52f,
            9.216f,
            156,
            68,
            1023,
            0.01f,
            0.02f,
            1,
            new TourDateTime(zonedDateTime),
            true);

      assertEquals(
            "17h   ⛅   Partly cloudy       6°C   feels like     9°C       9km/h from 156°   humidity  68%   pressure 1023.0mbar   precipitation  0.01mm   snowfall  0.02mm   air quality 1", //$NON-NLS-1$
            fullWeatherDataString);
   }

   @Test
   void testBuildFullWeatherDataString_ShouldReturnEmptyString() {

      final String fullWeatherDataString = WeatherUtils.buildFullWeatherDataString(6.09f,
            UI.EMPTY_STRING,
            UI.EMPTY_STRING,
            8.52f,
            9.216f,
            156,
            68,
            1023,
            0.01f,
            0.02f,
            0,
            null,
            true);

      assertEquals(UI.EMPTY_STRING, fullWeatherDataString);
   }
}
