/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import net.tourbook.common.color.ColorDefinition;
import net.tourbook.data.TourType;

import org.eclipse.swt.graphics.RGB;

public class TourTypeColorDefinition extends ColorDefinition implements Comparable<Object> {

   public static final RGB DEFAULT_GRADIENT_BRIGHT = new RGB(255, 255, 255);
   public static final RGB DEFAULT_GRADIENT_DARK   = new RGB(255, 167, 199);
   public static final RGB DEFAULT_LINE_COLOR      = new RGB(232, 152, 180);
   public static final RGB DEFAULT_TEXT_COLOR      = new RGB(98, 23, 49);

   private TourType         _tourType;

   /**
    * Create tour type color definition with a default ugly color
    *
    * @param tourType
    * @param colorDefinitionId
    * @param visibleName
    */
   public TourTypeColorDefinition(final TourType tourType, final String colorDefinitionId, final String visibleName) {

      // rgb values must be cloned that each tour type has it's own color

      super(colorDefinitionId,
            visibleName,

            new RGB(DEFAULT_GRADIENT_BRIGHT.red, DEFAULT_GRADIENT_BRIGHT.green, DEFAULT_GRADIENT_BRIGHT.blue),
            new RGB(DEFAULT_GRADIENT_DARK.red, DEFAULT_GRADIENT_DARK.green, DEFAULT_GRADIENT_DARK.blue),

            new RGB(DEFAULT_LINE_COLOR.red, DEFAULT_LINE_COLOR.green, DEFAULT_LINE_COLOR.blue),
            new RGB(DEFAULT_TEXT_COLOR.red, DEFAULT_TEXT_COLOR.green, DEFAULT_TEXT_COLOR.blue),

            null);

      _tourType = tourType;
   }

   /**
    * @param tourType
    * @param colorDefinitionId
    * @param visibleName
    * @param defaultGradientBright
    * @param defaultGradientDark
    * @param defaultLineColor_Light
    * @param defaultTextColor_Light
    * @param rgb2
    * @param rgb
    */
   public TourTypeColorDefinition(final TourType tourType,
                                  final String colorDefinitionId,
                                  final String visibleName,
                                  final RGB defaultGradientBright,
                                  final RGB defaultGradientDark,
                                  final RGB defaultLineColor_Light,
                                  final RGB defaultLineColor_Dark,
                                  final RGB defaultTextColor_Light,
                                  final RGB defaultTextColor_Dark) {

      super(colorDefinitionId,
            visibleName,

            defaultGradientBright,
            defaultGradientDark,

            defaultLineColor_Light,
            defaultLineColor_Dark,

            defaultTextColor_Light,
            defaultTextColor_Dark,

            null);

      _tourType = tourType;
   }

   @Override
   public int compareTo(final Object obj) {

      if (obj instanceof TourTypeColorDefinition) {

         final TourTypeColorDefinition otherColorDefinition = (TourTypeColorDefinition) obj;

         return _tourType.compareTo(otherColorDefinition.getTourType());
      }

      return 0;
   }

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }
      if (!super.equals(obj)) {
         return false;
      }
      if (!(obj instanceof TourTypeColorDefinition)) {
         return false;
      }
      final TourTypeColorDefinition other = (TourTypeColorDefinition) obj;
      if (_tourType == null) {
         if (other._tourType != null) {
            return false;
         }
      } else if (!_tourType.equals(other._tourType)) {
         return false;
      }
      return true;
   }

   public TourType getTourType() {
      return _tourType;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((_tourType == null) ? 0 : _tourType.hashCode());
      return result;
   }

   public void setTourType(final TourType tourType) {

      _tourType = tourType;
   }
}
