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
package common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.measurement_system.MeasurementSystem_Manager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class UITests {

   @AfterAll
   static void cleanUp() {
      setMetricSystem();
   }

   private static void setMetricSystem() {

      MeasurementSystem_Manager.setActiveSystemProfileIndex(0, true);
      UI.updateUnits();
   }

   private void setDistanceNauticalMile() {

      MeasurementSystem_Manager.setActiveSystemProfileIndex(2, true);
      UI.updateUnits();
   }

   private void setImperialSystem() {

      MeasurementSystem_Manager.setActiveSystemProfileIndex(1, true);
      UI.updateUnits();
   }

   @Test
   void testComputeBodyMassIndex() {

      setMetricSystem();
      //70kg and 1.80m => 21.6 BMI
      assertEquals(21.6f, UI.computeBodyMassIndex(70, 1.80));

      setImperialSystem();
      //154.3lbs and 70.9in => 21.6 BMI
      assertEquals(21.6f, UI.computeBodyMassIndex(154.3, 70.9));

      //100lbs and 4ft10in (58in) => 20.9 BMI
      assertEquals(20.9f, UI.computeBodyMassIndex(100, 58));

      //100lbs and 0in => 0 BMI
      assertEquals(0, UI.computeBodyMassIndex(100, 0));
   }

   @Test
   void testConvertAverageElevationChangeFromMetric() {

      setMetricSystem();
      //250m/km -> 250m/km
      assertEquals(250, UI.convertAverageElevationChangeFromMetric(250));

      setImperialSystem();
      //250m/km -> 1320ft/mi
      assertEquals(1320.0f, UI.convertAverageElevationChangeFromMetric(250));
   }

   @Test
   void testConvertBodyHeightFromMetric() {

      setMetricSystem();
      //184cm -> 184cm
      assertEquals(1.84f, UI.convertBodyHeightFromMetric(1.84f));

      setImperialSystem();
      //184cm -> 72.4409in
      assertEquals(72.44095f, UI.convertBodyHeightFromMetric(1.84f));
   }

   @Test
   void testConvertBodyHeightToMetric() {

      setMetricSystem();
      //184cm -> 184cm
      assertEquals(184f, UI.convertBodyHeightToMetric(184f, 0));

      setImperialSystem();
      //5ft11 -> 180.34cm
      assertEquals(180.34f, UI.convertBodyHeightToMetric(5, 11));
   }

   @Test
   void testConvertBodyWeightFromMetric() {

      setMetricSystem();
      //70kg -> 70kg
      assertEquals(70f, UI.convertBodyWeightFromMetric(70));

      setImperialSystem();
      //70kg -> 154.3lbs
      assertEquals(154.32361f, UI.convertBodyWeightFromMetric(70));
   }

   @Test
   void testConvertBodyWeightToMetric() {

      setMetricSystem();
      //70kg -> 70kg
      assertEquals(70f, UI.convertBodyWeightToMetric(70));

      setImperialSystem();
      //154.3lbs -> 70kg
      assertEquals(70, UI.convertBodyWeightToMetric(154.32361f));
   }

   @Test
   void testConvertPrecipitation_FromMetric() {

      setMetricSystem();
      //1mm -> 1mm
      assertEquals(1.0f, UI.convertPrecipitation_FromMetric(1.0f));

      setImperialSystem();
      //1mm -> 0.03938in
      assertEquals(0.03937007874f, UI.convertPrecipitation_FromMetric(1.0f));
   }

   @Test
   void testConvertPrecipitation_ToMetric() {

      setMetricSystem();
      //1mm -> 1mm
      assertEquals(1.0f, UI.convertPrecipitation_ToMetric(1.0f));

      setImperialSystem();
      //1in -> 25.4mm
      assertEquals(25.4f, UI.convertPrecipitation_ToMetric(1.0f));
   }

   @Test
   void testConvertPressure_FromMetric() {

      setMetricSystem();
      //1026mbar -> 1026mbar
      assertEquals(1026, UI.convertPressure_FromMetric(1026));

      setImperialSystem();
      //1026mbar -> 30.3in
      assertEquals(30.29778f, UI.convertPressure_FromMetric(1026));
   }

   @Test
   void testConvertPressure_ToMetric() {

      setMetricSystem();
      //1026mbar -> 1026mbar
      assertEquals(1026, UI.convertPressure_ToMetric(1026));

      setImperialSystem();
      //30.3in -> 1026mbar
      assertEquals(1026, UI.convertPressure_ToMetric(30.29778f));
   }

   @Test
   void testConvertSpeed_FromMetric() {

      setMetricSystem();
      //10km/h -> 10km/h
      assertEquals(10, UI.convertSpeed_FromMetric(10));

      setImperialSystem();
      //10km/h -> 6.2mph
      assertEquals(6.2137117f, UI.convertSpeed_FromMetric(10));

      setDistanceNauticalMile();
      //10km/h -> 5.4knots
      assertEquals(5.399568f, UI.convertSpeed_FromMetric(10));
   }

   @Test
   void testConvertTemperatureFromMetric() {

      setMetricSystem();
      //10C -> 10C
      assertEquals(10, UI.convertTemperatureFromMetric(10));

      setImperialSystem();
      //10C -> 50F
      assertEquals(50f, UI.convertTemperatureFromMetric(10));
   }

   @Test
   void testConvertTemperatureToMetric() {

      setMetricSystem();
      //10C -> 10C
      assertEquals(10, UI.convertTemperatureToMetric(10));

      setImperialSystem();
      //50F -> 10C
      assertEquals(10f, UI.convertTemperatureToMetric(50));
   }

   @Test
   void testGetCardinalDirectionText() {

      //0° -> N
      assertEquals(Messages.Weather_WindDirection_N, UI.getCardinalDirectionText(0));

      //20° -> NNE
      assertEquals(Messages.Weather_WindDirection_NNE, UI.getCardinalDirectionText(20));

      //45° -> NE
      assertEquals(Messages.Weather_WindDirection_NE, UI.getCardinalDirectionText(45));

      //65° -> ENE
      assertEquals(Messages.Weather_WindDirection_ENE, UI.getCardinalDirectionText(65));

      //90° -> E
      assertEquals(Messages.Weather_WindDirection_E, UI.getCardinalDirectionText(90));

      //105° -> ESE
      assertEquals(Messages.Weather_WindDirection_ESE, UI.getCardinalDirectionText(105));

      //130° -> SE
      assertEquals(Messages.Weather_WindDirection_SE, UI.getCardinalDirectionText(130));

      //150° -> SSE
      assertEquals(Messages.Weather_WindDirection_SSE, UI.getCardinalDirectionText(150));

      //180° -> S
      assertEquals(Messages.Weather_WindDirection_S, UI.getCardinalDirectionText(180));

      //200° -> SSW
      assertEquals(Messages.Weather_WindDirection_SSW, UI.getCardinalDirectionText(200));

      //220° -> SW
      assertEquals(Messages.Weather_WindDirection_SW, UI.getCardinalDirectionText(220));

      //240° -> WSW
      assertEquals(Messages.Weather_WindDirection_WSW, UI.getCardinalDirectionText(240));

      //270° -> W
      assertEquals(Messages.Weather_WindDirection_W, UI.getCardinalDirectionText(270));

      //290° -> WNW
      assertEquals(Messages.Weather_WindDirection_WNW, UI.getCardinalDirectionText(290));

      //310° -> NW
      assertEquals(Messages.Weather_WindDirection_NW, UI.getCardinalDirectionText(310));

      //330° -> NNW
      assertEquals(Messages.Weather_WindDirection_NNW, UI.getCardinalDirectionText(330));
   }
}
