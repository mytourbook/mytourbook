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
package net.tourbook.weather;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

public class WeatherUtilsTests {

   @Test
   void testComputeAverageWindSpeedAndDirection_Basic() {

      final double[] windSpeeds = new double[] { 30.0, 36.0 };
      final int[] windDirections = new int[] { 360, 350 };

      final int[] expectedAverageWindSpeedAndDirection = new int[] { 33, 355 };

      assertArrayEquals(
            expectedAverageWindSpeedAndDirection,
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections));
   }

   @Test
   void testComputeAverageWindSpeedAndDirection_Basics() {

      final double[] windSpeeds = new double[] { 1.0 };

      // Wind Coming From the North
      int[] windDirections = new int[] { 0 };
      int[] expectedAverageWindSpeedAndDirection = new int[] { 1, 0 };

      assertArrayEquals(
            expectedAverageWindSpeedAndDirection,
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections));

      // Wind Coming From the East
      windDirections = new int[] { 90 };
      expectedAverageWindSpeedAndDirection = new int[] { 1, 90 };
      assertArrayEquals(
            expectedAverageWindSpeedAndDirection,
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections));

      // Wind Coming From the South
      windDirections = new int[] { 180 };
      expectedAverageWindSpeedAndDirection = new int[] { 1, 180 };
      assertArrayEquals(
            expectedAverageWindSpeedAndDirection,
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections));

      // Wind Coming From the West
      windDirections = new int[] { 270 };
      expectedAverageWindSpeedAndDirection = new int[] { 1, 270 };
      assertArrayEquals(
            expectedAverageWindSpeedAndDirection,
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections));

      // Wind Coming From the NE
      windDirections = new int[] { 45 };
      expectedAverageWindSpeedAndDirection = new int[] { 1, 45 };
      assertArrayEquals(
            expectedAverageWindSpeedAndDirection,
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections));

      // Wind Coming From the SE
      windDirections = new int[] { 135 };
      expectedAverageWindSpeedAndDirection = new int[] { 1, 135 };
      assertArrayEquals(
            expectedAverageWindSpeedAndDirection,
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections));

      // Wind Coming From the SW
      windDirections = new int[] { 225 };
      expectedAverageWindSpeedAndDirection = new int[] { 1, 225 };
      assertArrayEquals(
            expectedAverageWindSpeedAndDirection,
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections));

      // Wind Coming From the NW
      windDirections = new int[] { 315 };
      expectedAverageWindSpeedAndDirection = new int[] { 1, 315 };
      assertArrayEquals(
            expectedAverageWindSpeedAndDirection,
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections));
   }

   @Test
   void testComputeAverageWindSpeedAndDirection_Empty() {

      double[] windSpeeds = new double[0];
      int[] windDirections = new int[0];

      final int[] expectedAverageWindSpeedAndDirection = new int[2];

      assertArrayEquals(
            expectedAverageWindSpeedAndDirection,
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections));

      windSpeeds = new double[1];

      assertArrayEquals(
            expectedAverageWindSpeedAndDirection,
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections));

      windSpeeds = new double[] { 13 };
      windDirections = new int[] { 12, 12 };

      assertArrayEquals(
            expectedAverageWindSpeedAndDirection,
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections));
   }

   /**
    * Testing and comparing the computation of the average speed and direction
    * with the data provided below:
    * https://www.itron.com/na/blog/forecasting/computing-a-weighted-average-wind-speed-and-wind-direction-across-multiple-weather-stations
    */
   @Test
   void testComputeAverageWindSpeedAndDirection_Itron() {

      final double[] windSpeeds = new double[] { 5.0, 10.0, 15.0, 20.0 };
      final int[] windDirections = new int[] { 87, 122, 157, 192 };

      final int[] expectedAverageWindSpeedAndDirection = new int[] { 10, 158 };

      assertArrayEquals(
            expectedAverageWindSpeedAndDirection,
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections));
   }

   /**
    * Testing and comparing the computation of the average speed and direction
    * with the data provided below:
    * https://www.scadacore.com/2014/12/19/average-wind-direction-and-wind-speed/
    */
   @Test
   void testComputeAverageWindSpeedAndDirection_ScadaCore() {

      final double[] windSpeeds = new double[] { 1.0, 1.0, 1.0, 1.0, 1.0, 1.0 };
      final int[] windDirections = new int[] { 270, 270, 270, 180, 180, 180 };

      final int[] expectedAverageWindSpeedAndDirection = new int[] { 1, 225 };

      assertArrayEquals(
            expectedAverageWindSpeedAndDirection,
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections));
   }
}
