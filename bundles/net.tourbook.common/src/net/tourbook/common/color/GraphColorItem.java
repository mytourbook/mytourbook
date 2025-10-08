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
package net.tourbook.common.color;

import net.tourbook.common.UI;

import org.eclipse.swt.graphics.RGB;

public class GraphColorItem {

   private static final char NL = UI.NEW_LINE;

   private ColorDefinition   _colorDefinition;

   private String            _colorPrefName;
   private String            _visibleName;

   private String            _colorId;

   /**
    * Is <code>true</code> when this {@link GraphColorItem} is used as for a map color.
    */
   private boolean           _isMapColor;

   public GraphColorItem(final ColorDefinition colorDefinition,
                         final String colorPrefName,
                         final String visibleName,
                         final boolean isMapColor) {

      _colorDefinition = colorDefinition;

      _colorPrefName = colorPrefName;
      _visibleName = visibleName;

      _isMapColor = isMapColor;

      _colorId = _colorDefinition.getColorDefinitionId() + UI.SYMBOL_DOT + _colorPrefName;
   }

   public ColorDefinition getColorDefinition() {
      return _colorDefinition;
   }

   public String getColorId() {
      return _colorId;
   }

   public String getName() {
      return _visibleName;
   }

   public RGB getRGB() {

      if (GraphColorManager.PREF_COLOR_LINE_LIGHT.equals(_colorPrefName)) {

         return _colorDefinition.getLineColor_New_Light();

      } else if (GraphColorManager.PREF_COLOR_LINE_DARK.equals(_colorPrefName)) {

         return _colorDefinition.getLineColor_New_Dark();

      } else if (GraphColorManager.PREF_COLOR_TEXT_LIGHT.equals(_colorPrefName)) {

         return _colorDefinition.getTextColor_New_Light();

      } else if (GraphColorManager.PREF_COLOR_TEXT_DARK.equals(_colorPrefName)) {

         return _colorDefinition.getTextColor_New_Dark();

      } else if (GraphColorManager.PREF_COLOR_GRADIENT_BRIGHT.equals(_colorPrefName)) {

         return _colorDefinition.getGradientBright_New();

      } else {

         // use as default color

         return _colorDefinition.getGradientDark_New();
      }
   }

   /**
    * @return Returns <code>true</code> when this {@link GraphColorItem} represents a
    *         {@link Map2ColorProfile}
    */
   public boolean isMapColor() {
      return _isMapColor;
   }

   /**
    * @param fromOneGraphColor
    *
    * @return Returns <code>true</code> when the graph color could be set, otherwise
    *         <code>false</code>
    */
   public boolean setGraphColor(final GraphColorItem fromOneGraphColor) {

      final String fromGraphColorId = fromOneGraphColor.getColorId();
      final ColorDefinition fromColorDefinition = fromOneGraphColor.getColorDefinition();

      final GraphColorItem[] allFromGraphColor = fromColorDefinition.getGraphColorItems();

      for (final GraphColorItem fromGraphColor : allFromGraphColor) {

         if (fromGraphColor.getColorId().equals(fromGraphColorId)) {

            final RGB pastedRGB = fromGraphColor.getRGB();

            // clone and set new color
            setRGB(new RGB(pastedRGB.red, pastedRGB.green, pastedRGB.blue));

            return true;
         }
      }

      return false;
   }

   public void setName(final String name) {
      _visibleName = name;
   }

   public void setRGB(final RGB rgb) {

      if (GraphColorManager.PREF_COLOR_LINE_LIGHT.equals(_colorPrefName)) {

         _colorDefinition.setLineColor_New_LightTheme(rgb);

      } else if (GraphColorManager.PREF_COLOR_LINE_DARK.equals(_colorPrefName)) {

         _colorDefinition.setLineColor_New_DarkTheme(rgb);

      } else if (GraphColorManager.PREF_COLOR_TEXT_LIGHT.equals(_colorPrefName)) {

         _colorDefinition.setTextColor_New_LightTheme(rgb);

      } else if (GraphColorManager.PREF_COLOR_TEXT_DARK.equals(_colorPrefName)) {

         _colorDefinition.setTextColor_New_DarkTheme(rgb);

      } else if (GraphColorManager.PREF_COLOR_GRADIENT_BRIGHT.equals(_colorPrefName)) {

         _colorDefinition.setGradientBright_New(rgb);

      } else {

         // use as default color

         _colorDefinition.setGradientDark_New(rgb);
      }
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "GraphColorItem" + NL //                                  //$NON-NLS-1$

            + "  _visibleName       = " + _visibleName + NL //          //$NON-NLS-1$
            + "  _colorId           = " + _colorId + NL //              //$NON-NLS-1$
            + "  _colorPrefName     = " + _colorPrefName + NL //        //$NON-NLS-1$
            + "  _isMapColor        = " + _isMapColor + NL //           //$NON-NLS-1$

// THIS MAY CAUSE AN ENDLESS LOOP -> com.sun.jdi.InvocationException: Exception occurred in target VM occurred invoking method.
//          + "  _colorDefinition   = " + _colorDefinition + NL //      //$NON-NLS-1$

      ;

   }
}
