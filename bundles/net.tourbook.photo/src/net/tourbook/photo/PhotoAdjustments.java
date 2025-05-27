/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Arrays;

import net.tourbook.common.UI;

/**
 * Adjustments for a photo, e.g. cropping, tonality...
 */
@JsonIgnoreProperties(ignoreUnknown = true) // ignore fields which are not defined, this is helpful when field names are changed to prevent exceptions
public class PhotoAdjustments {

   private static final char NL = UI.NEW_LINE;

   public boolean            isPhotoCropped;

   /**
    * Relative position 0...1 of the crop area top left x position
    */
   public float              cropAreaX1;
   public float              cropAreaY1;

   public float              cropAreaX2;
   public float              cropAreaY2;

   public boolean            isSetTonality;

   /**
    * X values 0...1
    */
   public float              curveValuesX[];

   /**
    * Y values 0...1
    */
   public float              curveValuesY[];

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "PhotoAdjustments" + NL //                                                                       //$NON-NLS-1$

//            + " isPhotoCropped   = " + isPhotoCropped + NL //                                                  //$NON-NLS-1$
//            + " cropAreaX1       = " + cropAreaX1 + NL //                                                      //$NON-NLS-1$
//            + " cropAreaY1       = " + cropAreaY1 + NL //                                                      //$NON-NLS-1$
//            + " cropAreaX2       = " + cropAreaX2 + NL //                                                      //$NON-NLS-1$
//            + " cropAreaY2       = " + cropAreaY2 + NL //                                                      //$NON-NLS-1$

            + " isSetTonality    = " + isSetTonality + NL //                                                   //$NON-NLS-1$
            + " curveValuesX     = " + (curveValuesX != null ? Arrays.toString(curveValuesX) : null) + NL //   //$NON-NLS-1$
            + " curveValuesY     = " + (curveValuesY != null ? Arrays.toString(curveValuesY) : null) + NL //   //$NON-NLS-1$

      ;
   }

}
