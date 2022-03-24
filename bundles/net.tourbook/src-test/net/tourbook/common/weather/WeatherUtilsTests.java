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

   //testNorthSouthVectorReversed
   //18:58:36 [main - 1] 7  23/03/2022 16:43: ? ciel dégagé, 13°C, max. 13°C, min. 13°C, ressenti 11°C, 36km/h depuis S, 16% humidité, 1018.0mbar pression atm.
//   18:58:36 [main - 1] 8  17h 13°C ressenti 11°C 39km/h depuis 360° humidité 15% pression atm. 1018.0mbar précipitations 0.0mm enneigement 0.0mm
   // 18h 13°C ressenti 11°C 33km/h depuis 10° humidité 17% pression atm. 1018.0mbar précipitations 0.0mm enneigement 0.0mm
   //18:58:36 [main - 1] 9  Données récupérées en 0,9 s 

}
