package net.tourbook.common.weather;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import net.tourbook.weather.WeatherUtils;

import org.junit.jupiter.api.Test;

public class WeatherUtilsTests {

   @Test
   void testComputeAverageWindSpeedAndDirection() {

      final Double[] windSpeeds = new Double[] { 1.0 };
      final Integer[] windDirections = new Integer[] { 0 };

      final int[] expectedAverageWindSpeedAndDirection = new int[] { 1, 179 };

      assertArrayEquals(
            expectedAverageWindSpeedAndDirection,
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections));
   }

}
