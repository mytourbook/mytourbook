/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import net.tourbook.chart.ISliderLabelProvider;
import net.tourbook.common.UI;
import net.tourbook.data.GearDataType;

public class SliderLabelProvider_Gear implements ISliderLabelProvider {

   private float[][]    _gearValues;
   private GearDataType _gearType;

   public SliderLabelProvider_Gear(final float[][] gearValues, final GearDataType gearType) {

      _gearValues = gearValues;
      _gearType = gearType;
   }

   @Override
   public String getLabel(final int sliderValueIndex) {

      final float gearRatio = _gearValues[0][sliderValueIndex];

      final float frontTeeth = _gearValues[1][sliderValueIndex];
      final float rearTeeth = _gearValues[2][sliderValueIndex];

      final float rearGearNo = _gearValues[4][sliderValueIndex];

      if (_gearType.equals(GearDataType.REAR_GEAR)) {

         return Integer.toString((int) rearGearNo);

      } else {

         if (Float.isNaN(gearRatio)) {
            return UI.DASH_WITH_SPACE;
         }

         return TourManager.GEAR_VALUE_FORMAT.formatted(
               (int) frontTeeth,
               (int) rearTeeth,
               gearRatio);
      }
   }
}
