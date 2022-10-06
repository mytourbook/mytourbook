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

   private void setImperialSystem() {

      MeasurementSystem_Manager.setActiveSystemProfileIndex(1, true);
      UI.updateUnits();
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
      assertEquals(1f, UI.convertBodyHeightToMetric(1f, 0));

      setImperialSystem();
      //184cm -> 72.4409in
      assertEquals(1.84f, UI.convertBodyHeightToMetric(72f, 44095));
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
}
