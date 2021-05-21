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

import gnu.trove.map.hash.TIntObjectHashMap;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

/**
 * Cache for SWT {@link Color}'s which do <b>not</b> contain transparency.
 */
public class ColorCacheSWT {

   private TIntObjectHashMap<Color> _colors = new TIntObjectHashMap<>();

   public ColorCacheSWT() {}

   /**
    * Remove all colors from the color cache
    */
   public void dispose() {

      _colors.clear();
   }

   /**
    * @param colorValue
    * @return Returns the color for the <code>colorValue</code> from the color cache, color is
    *         created when it is not available.
    */
   public Color getColor(final int colorValue) {

      Color color = _colors.get(colorValue);
      if (color != null) {
         return color;
      }

      final int red = (colorValue & 0xFF) >>> 0;
      final int green = (colorValue & 0xFF00) >>> 8;
      final int blue = (colorValue & 0xFF0000) >>> 16;

      color = new Color(red, green, blue);

      _colors.put(colorValue, color);

      return color;
   }

   /**
    * @param colorValue
    * @return Returns the color for the <code>colorValue</code> from the color cache, color is
    *         created when it is not available.
    */
   public Color getColor(final RGB rgb) {

      final int colorValue = ((rgb.red & 0xFF) << 0) | ((rgb.green & 0xFF) << 8) | ((rgb.blue & 0xFF) << 16);

      Color color = _colors.get(colorValue);

      if (color != null) {
         return color;
      }

      color = new Color(rgb);

      _colors.put(colorValue, color);

      return color;
   }

   /**
    * Discovered after a century that rgb values are in the wrong order with {@link #getColor(int)}
    *
    * @param colorValue
    * @return Returns the color for the <code>colorValue</code> from the color cache, color is
    *         created when it is not available.
    */
   public Color getColorRGB(final int colorValue) {

      Color color = _colors.get(colorValue);
      if (color != null) {
         return color;
      }

      final int red = (colorValue & 0xFF0000) >>> 16;
      final int green = (colorValue & 0xFF00) >>> 8;
      final int blue = (colorValue & 0xFF) >>> 0;

      color = new Color(red, green, blue);

      _colors.put(colorValue, color);

      return color;
   }

   /**
    * Discovered after a century that rgb values are in the wrong order with {@link #getColor(RGB)}
    *
    * @param rgb
    * @return Returns the color for the <code>colorValue</code> from the color cache, color is
    *         created when it is not available.
    */
   public Color getColorRGB(final RGB rgb) {

      final int colorValue = ((rgb.red & 0xFF) << 16) | ((rgb.green & 0xFF) << 8) | ((rgb.blue & 0xFF) << 8);

      Color color = _colors.get(colorValue);

      if (color != null) {
         return color;
      }

      color = new Color(rgb);

      _colors.put(colorValue, color);

      return color;
   }

}
