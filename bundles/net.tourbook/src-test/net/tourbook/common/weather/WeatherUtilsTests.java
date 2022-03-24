package net.tourbook.common.weather;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import net.tourbook.weather.WeatherUtils;

import org.junit.jupiter.api.Test;

public class WeatherUtilsTests {

   @Test
   void testComputeAverageWindSpeedAndDirection() {

      final Double[] windSpeeds = new Double[] { 1.0, 1.0, 1.0, 1.0, 1.0, 1.0 };
      final Integer[] windDirections = new Integer[] { 270, 270, 270, 180, 180, 180 };

      final int[] expectedAverageWindSpeedAndDirection = new int[] { 1, 225 };

      assertArrayEquals(
            expectedAverageWindSpeedAndDirection,
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections));
   }

}
