/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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

import java.awt.Color;

import org.eclipse.swt.graphics.RGB;

public class ColorUtil {

   /**
    * Converts SWT color into AWT color
    *
    * @param swtColor
    * @return
    */
   public static Color convertSWTColor_into_AWTColor(final org.eclipse.swt.graphics.Color swtColor) {

      return new Color(swtColor.getRed(), swtColor.getGreen(), swtColor.getBlue());
   }

   /**
    * Splits an integer color values in it's red, green and blue components.
    *
    * @param rgbValue
    * @return Returns a {@link RGB} from an integer color value
    */
   public static RGB createRGB(final int rgbValue) {

      final int red = (rgbValue & 0xFF0000) >>> 16;
      final int green = (rgbValue & 0xFF00) >>> 8;
      final int blue = (rgbValue & 0xFF) >>> 0;

      return new RGB(red, green, blue);
   }

   /**
    * @param color
    * @param alpha
    *           0xff is opaque, 0 is transparent
    * @return
    */
   public static int getARGB(final RGB color, final int alpha) {

      final int graphColor = ((color.blue & 0xFF) << 0)
            | ((color.green & 0xFF) << 8)
            | ((color.red & 0xFF) << 16)
            | ((alpha) << 24);

      return graphColor;
   }

   /**
    * @param rgb
    * @return Returns an integer value from a {@link RGB}
    */
   public static int getColorValue(final RGB rgb) {

      return ((rgb.blue & 0xFF) << 0)
            | ((rgb.green & 0xFF) << 8)
            | ((rgb.red & 0xFF) << 16);
   }

   public static org.eclipse.swt.graphics.Color getComplimentColor(final org.eclipse.swt.graphics.Color color) {

      final RGB complimentColor = getComplimentColor(color.getRGB());

      return new org.eclipse.swt.graphics.Color(complimentColor);
   }

   public static RGB getComplimentColor(final RGB color) {

      // get compliment color
      final int red = (~color.red) & 0xff;
      final int blue = (~color.blue) & 0xff;
      final int green = (~color.green) & 0xff;

      final double darker = 0.8;

      return new RGB(
            (int) (red * darker),
            (int) (green * darker),
            (int) (blue * darker));
   }

   public static org.eclipse.swt.graphics.Color getContrastColor(final int rgbValue) {

      final byte blue = (byte) ((rgbValue & 0xFF0000) >> 16);
      final byte green = (byte) ((rgbValue & 0xFF00) >> 8);
      final byte red = (byte) ((rgbValue & 0xFF) >> 0);

      return getContrastColor(red & 0xFF, green & 0xFF, blue & 0xFF);
   }

   /**
    * Compute a color that contrasts with the given color.
    *
    * @param red
    * @param green
    * @param blue
    * @return Returns white or black that contrasts with the background color.
    */
   public static org.eclipse.swt.graphics.Color getContrastColor(final int red,
                                                                 final int green,
                                                                 final int blue) {

      final int yiq = ((red * 299) + (green * 587) + (blue * 114)) / 1000;

      if (yiq >= 128) {
         return new org.eclipse.swt.graphics.Color(0, 0, 0);
      } else {
         return new org.eclipse.swt.graphics.Color(0xff, 0xff, 0xff);
      }
   }

   public static org.eclipse.swt.graphics.Color getContrastColor(final RGB rgb) {

      return getContrastColor(rgb.red, rgb.green, rgb.blue);
   }

   /**
    * Compute a color that contrasts with the given color.
    *
    * @param red
    * @param green
    * @param blue
    * @return Returns white or black that contrasts with the background color.
    */
   public static Color getContrastColorAWT(final int red, final int green, final int blue, final int alpha) {

      final int yiq = ((red * 299) + (green * 587) + (blue * 114)) / 1000;

      final float newAlpha = (float) alpha / 0xff;

      if (yiq >= 128) {
         return new Color(0, 0, 0, newAlpha);
      } else {
         return new Color(1, 1, 1, newAlpha);
      }
   }

   /**
    * Compute a color that contrasts with the given color.
    *
    * @param red
    * @param green
    * @param blue
    * @return Returns white or black that contrasts with the background color.
    */
   public static RGB getContrastRGB(
                                    final int red,
                                    final int green,
                                    final int blue) {

      final int yiq = ((red * 299) + (green * 587) + (blue * 114)) / 1000;

      if (yiq >= 128) {
         return new RGB(0, 0, 0);
      } else {
         return new RGB(0xff, 0xff, 0xff);
      }
   }

   public static RGB getContrastRGB(final RGB rgb) {

      return getContrastRGB(rgb.red, rgb.green, rgb.blue);
   }

}
