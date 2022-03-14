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

import org.junit.jupiter.api.Test;

public class UITests {

   @Test
   void testConvertPrecipitation_FromMetric() {

      UI.UNIT_IS_LENGTH_SMALL_MILLIMETER = true;
      //1mm -> 1mm
      assertEquals(1.0f, UI.convertPrecipitation_FromMetric(1.0f));

      //1mm -> 0.03938in
      UI.UNIT_IS_LENGTH_SMALL_MILLIMETER = false;
      assertEquals(0.03937007874f, UI.convertPrecipitation_FromMetric(1.0f));
   }

   @Test
   void testConvertPrecipitation_ToMetric() {

      UI.UNIT_IS_LENGTH_SMALL_MILLIMETER = true;
      //1mm -> 1mm
      assertEquals(1.0f, UI.convertPrecipitation_ToMetric(1.0f));

      //1in -> 25.4mm
      UI.UNIT_IS_LENGTH_SMALL_MILLIMETER = false;
      assertEquals(25.4f, UI.convertPrecipitation_ToMetric(1.0f));
   }
}
