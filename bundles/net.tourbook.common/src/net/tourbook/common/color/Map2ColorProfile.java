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
package net.tourbook.common.color;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

/**
 * Contains all colors and data to paint a tour or legend in a map.
 */
public class Map2ColorProfile extends MapColorProfile implements Cloneable {

   private static final char NL          = UI.NEW_LINE;

   protected boolean         isMinValueOverwrite;
   protected boolean         isMaxValueOverwrite;

   protected int             minValueOverwrite;
   protected int             maxValueOverwrite;

   private ColorValue[]      colorValues = new ColorValue[] {

         new ColorValue(10, 255, 0, 0),
         new ColorValue(50, 100, 100, 0),
         new ColorValue(100, 0, 255, 0),
         new ColorValue(150, 0, 100, 100),
         new ColorValue(190, 0, 0, 255)

   };

   public Map2ColorProfile() {}

   /**
    * @param valueColors
    * @param minBrightness
    * @param minBrightnessFactor
    * @param maxBrightness
    * @param maxBrightnessFactor
    */
   public Map2ColorProfile(final ColorValue[] valueColors,

                           final int minBrightness,
                           final int minBrightnessFactor,
                           final int maxBrightness,
                           final int maxBrightnessFactor) {

      this.colorValues = valueColors;

      this.minBrightness = minBrightness;
      this.minBrightnessFactor = minBrightnessFactor;
      this.maxBrightness = maxBrightness;
      this.maxBrightnessFactor = maxBrightnessFactor;
   }

   /**
    * @param valueColors
    * @param minBrightness
    * @param minBrightnessFactor
    * @param maxBrightness
    * @param maxBrightnessFactor
    * @param isMinOverwrite
    * @param minOverwrite
    * @param isMaxOverwrite
    * @param maxOverwrite
    */
   public Map2ColorProfile(final ColorValue[] valueColors,

                           final int minBrightness,
                           final int minBrightnessFactor,
                           final int maxBrightness,
                           final int maxBrightnessFactor,

                           final boolean isMinOverwrite,
                           final int minOverwrite,
                           final boolean isMaxOverwrite,
                           final int maxOverwrite) {

      this(valueColors, minBrightness, minBrightnessFactor, maxBrightness, maxBrightnessFactor);

      this.isMinValueOverwrite = isMinOverwrite;
      this.minValueOverwrite = minOverwrite;
      this.isMaxValueOverwrite = isMaxOverwrite;
      this.maxValueOverwrite = maxOverwrite;
   }

   @Override
   public Map2ColorProfile clone() {

      Map2ColorProfile clonedObject = null;

      try {

         clonedObject = (Map2ColorProfile) super.clone();

         clonedObject.colorValues = new ColorValue[colorValues.length];

         for (int colorIndex = 0; colorIndex < colorValues.length; colorIndex++) {
            clonedObject.colorValues[colorIndex] = colorValues[colorIndex].clone();
         }

      } catch (final CloneNotSupportedException e) {
         StatusUtil.log(e);
      }

      return clonedObject;
   }

   private String getBrightnessText(final int brightnessValue) {

//    public static final int      BRIGHTNESS_DEFAULT        = 0;
//    public static final int      BRIGHTNESS_DIMMING        = 1;
//    public static final int      BRIGHTNESS_LIGHTNING      = 2;

      if (brightnessValue == 1) {
         return "MapColorProfile.BRIGHTNESS_DIMMING"; //$NON-NLS-1$
      } else if (brightnessValue == 2) {
         return "MapColorProfile.BRIGHTNESS_LIGHTNING"; //$NON-NLS-1$
      } else {
         return "MapColorProfile.BRIGHTNESS_DEFAULT"; //$NON-NLS-1$
      }
   }

   public ColorValue[] getColorValues() {
      return colorValues;
   }

   public int getMaxValueOverwrite() {
      return maxValueOverwrite;
   }

   public int getMinValueOverwrite() {
      return minValueOverwrite;
   }

   public boolean isMaxValueOverwrite() {
      return isMaxValueOverwrite;
   }

   public boolean isMinValueOverwrite() {
      return isMinValueOverwrite;
   }

   public void setColorValues(final ColorValue[] colorValues) {
      this.colorValues = colorValues;
   }

   public void setIsMaxValueOverwrite(final boolean isMaxValueOverwrite) {
      this.isMaxValueOverwrite = isMaxValueOverwrite;
   }

   public void setIsMinValueOverwrite(final boolean isMinValueOverwrite) {
      this.isMinValueOverwrite = isMinValueOverwrite;
   }

   public void setMaxValueOverwrite(final int overwriteMaxValue) {
      this.maxValueOverwrite = overwriteMaxValue;
   }

   public void setMinValueOverwrite(final int overwriteMinValue) {
      this.minValueOverwrite = overwriteMinValue;
   }

   @Override
   public String toString() {

//      new Map2ColorProfile(
//
//            new ColorValue[] {
//
//                  new ColorValue(10, 0, 0, 255),
//                  new ColorValue(50, 0, 255, 255),
//                  new ColorValue(100, 0, 237, 0),
//                  new ColorValue(150, 255, 255, 0),
//                  new ColorValue(190, 255, 0, 0)
//
//            },
//
//            MapColorProfile.BRIGHTNESS_DIMMING,
//            23,
//            MapColorProfile.BRIGHTNESS_DIMMING,
//            10,
//
//            // overwrite min/max values
//            true,
//            -10,
//            true,
//            10
//            )

      final int numColorValues = colorValues.length;

      final String minBrightness_Text = getBrightnessText(minBrightness);
      final String maxBrightness_Text = getBrightnessText(maxBrightness);

      final StringBuilder sb = new StringBuilder();

      sb.append(NL);
      sb.append(NL);

      sb.append("new Map2ColorProfile(" + NL); //$NON-NLS-1$
      sb.append(NL);
      sb.append("   new ColorValue[] {" + NL); //$NON-NLS-1$
      sb.append(NL);

      for (int valueIndex = 0; valueIndex < numColorValues; valueIndex++) {
         final ColorValue colorValue = colorValues[valueIndex];
         final String komma = valueIndex < numColorValues - 1 ? "," : ""; //$NON-NLS-1$ //$NON-NLS-2$
         sb.append("      " + colorValue + komma + NL); //$NON-NLS-1$
      }
      sb.append("   }," + NL); //$NON-NLS-1$

      sb.append(NL);

      sb.append("   " + minBrightness_Text + "," + NL); //$NON-NLS-1$ //$NON-NLS-2$
      sb.append("   " + minBrightnessFactor + "," + NL); //$NON-NLS-1$ //$NON-NLS-2$
      sb.append("   " + maxBrightness_Text + "," + NL); //$NON-NLS-1$ //$NON-NLS-2$
      sb.append("   " + maxBrightnessFactor + "" + NL); //$NON-NLS-1$ //$NON-NLS-2$

      if (isMinValueOverwrite || isMaxValueOverwrite) {
         sb.append("   " + "," + NL); //$NON-NLS-1$ //$NON-NLS-2$
         sb.append(NL);

         sb.append("   // overwrite min/max values" + NL); //$NON-NLS-1$

         sb.append("   " + isMinValueOverwrite + "," + NL); //$NON-NLS-1$ //$NON-NLS-2$
         sb.append("   " + minValueOverwrite + "," + NL); //$NON-NLS-1$ //$NON-NLS-2$
         sb.append("   " + isMaxValueOverwrite + "," + NL); //$NON-NLS-1$ //$NON-NLS-2$
         sb.append("   " + maxValueOverwrite + NL); //$NON-NLS-1$
      }

      sb.append("   );"); //$NON-NLS-1$

      return sb.toString();
   }

}
