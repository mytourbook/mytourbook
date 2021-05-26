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

import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.preferences.ICommonPreferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

/**
 * Contains all colors for one graph type.
 */
public class ColorDefinition {

   private static final char      NL               = UI.NEW_LINE;

   private final IPreferenceStore _commonPrefStore = CommonActivator.getPrefStore();

   private String                 _colorDefinitionId;
   private String                 _visibleName;
   private String                 _graphPrefNamePrefix;

   /**
    * These are children in the tree viewer.
    */
   private GraphColorItem[]       _graphColorItems;

   private RGB                    _gradientBright_Active;
   private RGB                    _gradientBright_Default;
   private RGB                    _gradientBright_New;

   private RGB                    _gradientDark_Active;
   private RGB                    _gradientDark_Default;
   private RGB                    _gradientDark_New;

   private RGB                    _lineColor_Active_Light;
   private RGB                    _lineColor_Active_Dark;
   private RGB                    _lineColor_Default_Light;
   private RGB                    _lineColor_Default_Dark;
   private RGB                    _lineColor_New_Light;
   private RGB                    _lineColor_New_Dark;

   private RGB                    _textColor_Active_Light;
   private RGB                    _textColor_Active_Dark;
   private RGB                    _textColor_Default_Light;
   private RGB                    _textColor_Default_Dark;
   private RGB                    _textColor_New_Light;
   private RGB                    _textColor_New_Dark;

   /*
    * One color definition contains different profiles which are used depending on the current
    * situation.
    */
   private Map2ColorProfile _map2ColorProfile_Active;
   private Map2ColorProfile _map2ColorProfile_Default;
   private Map2ColorProfile _map2ColorProfile_New;

   protected ColorDefinition(final String colorDefinitionId,
                             final String visibleName,

                             final RGB defaultGradientBright,
                             final RGB defaultGradientDark,

                             final RGB defaultLineColor_Light,
                             final RGB defaultTextColor_Light,

                             final Map2ColorProfile defaultMapColorProfile) {

      this(

            colorDefinitionId,
            visibleName,

            defaultGradientBright,
            defaultGradientDark,

            defaultLineColor_Light,
            defaultLineColor_Light,

            defaultTextColor_Light,
            defaultTextColor_Light,

            defaultMapColorProfile

      );
   }

   /**
    * Sets the color for the default, current and changes
    *
    * @param colorDefinitionId
    *           Unique id
    * @param visibleName
    * @param defaultGradientBright
    * @param defaultGradientDark
    * @param defaultLineColor_Light
    * @param defaultLineColor_Dark
    * @param defaultTextColor_Light
    * @param defaultTextColor_Dark
    * @param defaultMapColorProfile
    *           Map color configuration or <code>null</code> when not available
    */
   protected ColorDefinition(final String colorDefinitionId,
                             final String visibleName,

                             final RGB defaultGradientBright,
                             final RGB defaultGradientDark,

                             final RGB defaultLineColor_Light,
                             final RGB defaultLineColor_Dark,

                             final RGB defaultTextColor_Light,
                             final RGB defaultTextColor_Dark,

                             final Map2ColorProfile defaultMapColorProfile) {

      _colorDefinitionId = colorDefinitionId;
      _visibleName = visibleName;

      _gradientBright_Default = defaultGradientBright;
      _gradientDark_Default = defaultGradientDark;

      _lineColor_Default_Light = defaultLineColor_Light;
      _lineColor_Default_Dark = defaultLineColor_Dark;

      _textColor_Default_Light = defaultTextColor_Light;
      _textColor_Default_Dark = defaultTextColor_Dark;

      _map2ColorProfile_Default = defaultMapColorProfile;

      _graphPrefNamePrefix = ICommonPreferences.GRAPH_COLORS + _colorDefinitionId + UI.SYMBOL_DOT;

      /*
       * Set gradient bright from pref store or default
       */
      final String prefColorGradientBright = getGraphPrefName(GraphColorManager.PREF_COLOR_GRADIENT_BRIGHT);

      if (_commonPrefStore.contains(prefColorGradientBright)) {
         _gradientBright_Active = PreferenceConverter.getColor(_commonPrefStore, prefColorGradientBright);
      } else {
         _gradientBright_Active = _gradientBright_Default;
      }
      _gradientBright_New = _gradientBright_Active;

      /*
       * Gradient dark
       */
      final String prefColorGradientDark = getGraphPrefName(GraphColorManager.PREF_COLOR_GRADIENT_DARK);

      if (_commonPrefStore.contains(prefColorGradientDark)) {
         _gradientDark_Active = PreferenceConverter.getColor(_commonPrefStore, prefColorGradientDark);
      } else {
         _gradientDark_Active = _gradientDark_Default;
      }
      _gradientDark_New = _gradientDark_Active;

      /*
       * Line color
       */
      final String prefColorLine_Light = getGraphPrefName(GraphColorManager.PREF_COLOR_LINE_LIGHT);
      final String prefColorLine_Dark = getGraphPrefName(GraphColorManager.PREF_COLOR_LINE_DARK);

      if (_commonPrefStore.contains(prefColorLine_Light)) {
         _lineColor_Active_Light = PreferenceConverter.getColor(_commonPrefStore, prefColorLine_Light);
      } else {
         _lineColor_Active_Light = _lineColor_Default_Light;
      }
      if (_commonPrefStore.contains(prefColorLine_Dark)) {
         _lineColor_Active_Dark = PreferenceConverter.getColor(_commonPrefStore, prefColorLine_Dark);
      } else {
         _lineColor_Active_Dark = _lineColor_Default_Dark;
      }

      _lineColor_New_Light = _lineColor_Active_Light;
      _lineColor_New_Dark = _lineColor_Active_Dark;

      /*
       * Text color
       */
      final String prefColorText_Light = getGraphPrefName(GraphColorManager.PREF_COLOR_TEXT_LIGHT);
      final String prefColorText_Dark = getGraphPrefName(GraphColorManager.PREF_COLOR_TEXT_DARK);

      if (_commonPrefStore.contains(prefColorText_Light)) {
         _textColor_Active_Light = PreferenceConverter.getColor(_commonPrefStore, prefColorText_Light);
      } else {
         _textColor_Active_Light = _textColor_Default_Light;
      }
      if (_commonPrefStore.contains(prefColorText_Dark)) {
         _textColor_Active_Dark = PreferenceConverter.getColor(_commonPrefStore, prefColorText_Dark);
      } else {
         _textColor_Active_Dark = _textColor_Default_Dark;
      }

      _textColor_New_Light = _textColor_Active_Light;
      _textColor_New_Dark = _textColor_Active_Dark;
   }

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }

      final ColorDefinition other = (ColorDefinition) obj;
      if (_colorDefinitionId == null) {
         if (other._colorDefinitionId != null) {
            return false;
         }
      } else if (!_colorDefinitionId.equals(other._colorDefinitionId)) {
         return false;
      }

      return true;
   }

   public String getColorDefinitionId() {
      return _colorDefinitionId;
   }

   public RGB getGradientBright_Active() {
      return _gradientBright_Active;
   }

   public RGB getGradientBright_Default() {
      return _gradientBright_Default;
   }

   public RGB getGradientBright_New() {
      return _gradientBright_New;
   }

   public RGB getGradientDark_Active() {
      return _gradientDark_Active;
   }

   public RGB getGradientDark_Default() {
      return _gradientDark_Default;
   }

   public RGB getGradientDark_New() {
      return _gradientDark_New;
   }

   public GraphColorItem[] getGraphColorItems() {
      return _graphColorItems;
   }

   public String getGraphPrefName(final String graphColorName) {

      final String graphPrefName = _graphPrefNamePrefix + graphColorName;

      return graphPrefName;
   }

   public RGB getLineColor_Active_Dark() {
      return _lineColor_Active_Dark;
   }

   public RGB getLineColor_Active_Light() {
      return _lineColor_Active_Light;
   }

   public RGB getLineColor_Active_Themed() {

      return UI.IS_DARK_THEME ? _lineColor_Active_Dark : _lineColor_Active_Light;
   }

   public RGB getLineColor_Default_Dark() {
      return _lineColor_Default_Dark;
   }

   public RGB getLineColor_Default_Light() {
      return _lineColor_Default_Light;
   }

   public RGB getLineColor_New_Dark() {
      return _lineColor_New_Dark;
   }

   public RGB getLineColor_New_Light() {
      return _lineColor_New_Light;
   }

   public Map2ColorProfile getMap2Color_Active() {
      return _map2ColorProfile_Active;
   }

   public Map2ColorProfile getMap2Color_Default() {
      return _map2ColorProfile_Default;
   }

   public Map2ColorProfile getMap2Color_New() {
      return _map2ColorProfile_New;
   }

   public RGB getTextColor_Active_Dark() {
      return _textColor_Active_Dark;
   }

   public RGB getTextColor_Active_Light() {
      return _textColor_Active_Light;
   }

   public RGB getTextColor_Active_Themed() {

      return UI.IS_DARK_THEME ? _textColor_Active_Dark : _textColor_Active_Light;
   }

   public RGB getTextColor_Default_Dark() {
      return _textColor_Default_Dark;
   }

   public RGB getTextColor_Default_Light() {
      return _textColor_Default_Light;
   }

   public RGB getTextColor_New_Dark() {
      return _textColor_New_Dark;
   }

   public RGB getTextColor_New_Light() {
      return _textColor_New_Light;
   }

   public String getVisibleName() {
      return _visibleName;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((_colorDefinitionId == null) ? 0 : _colorDefinitionId.hashCode());
      return result;
   }

   /**
    * Log RGB values as Java code:
    * <p>
    * <code>
    *  new RGB(0x5B, 0x5B, 0x5B),
    * </code>
    *
    * @param rgb
    * @return
    */
   private String logRGB(final RGB rgb) {

      if (rgb == null) {
         return "null"; //$NON-NLS-1$
      }

      return "new RGB(" //$NON-NLS-1$
            + "0x" + Integer.toHexString(rgb.red) + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "0x" + Integer.toHexString(rgb.green) + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "0x" + Integer.toHexString(rgb.blue) //$NON-NLS-1$
            + "),"; //$NON-NLS-1$
   }

   /**
    * Set color items for this color definition, the color items are children in the tree.
    *
    * @param children
    */
   public void setColorItems(final GraphColorItem[] children) {
      _graphColorItems = children;
   }

   public void setGradientBright_Active(final RGB gradientBright) {
      _gradientBright_Active = gradientBright;
   }

   public void setGradientBright_New(final RGB newGradientBright) {
      _gradientBright_New = newGradientBright;
   }

   public void setGradientDark_Active(final RGB gradientDark) {
      _gradientDark_Active = gradientDark;
   }

   public void setGradientDark_New(final RGB newGradientDark) {
      _gradientDark_New = newGradientDark;
   }

   public void setLineColor_Active_Dark(final RGB lineColor) {
      _lineColor_Active_Dark = lineColor;
   }

   public void setLineColor_Active_Light(final RGB lineColor) {
      _lineColor_Active_Light = lineColor;
   }

   public void setLineColor_New_Dark(final RGB newLineColor) {
      _lineColor_New_Dark = newLineColor;
   }

   public void setLineColor_New_Light(final RGB newLineColor) {
      _lineColor_New_Light = newLineColor;
   }

   public void setMap2Color_Active(final Map2ColorProfile mapColor) {
      _map2ColorProfile_Active = mapColor;
   }

   public void setMap2Color_New(final Map2ColorProfile newMapColor) {
      _map2ColorProfile_New = newMapColor;
   }

   public void setTextColor_Active_Dark(final RGB textColor) {
      _textColor_Active_Dark = textColor;
   }

   public void setTextColor_Active_Light(final RGB textColor) {
      _textColor_Active_Light = textColor;
   }

   public void setTextColor_New_Dark(final RGB textColorNew) {
      _textColor_New_Dark = textColorNew;
   }

   public void setTextColor_New_Light(final RGB textColorNew) {
      _textColor_New_Light = textColorNew;
   }

   public void setVisibleName(final String visibleName) {
      _visibleName = visibleName;
   }

   @Override
   public String toString() {

// SET_FORMATTING_OFF

      return "\nColorDefinition"             + NL + NL //$NON-NLS-1$

            + "_colorDefinitionId = " + _colorDefinitionId  + NL + NL //$NON-NLS-1$

            + logRGB(_gradientBright_New)    + NL
            + logRGB(_gradientDark_New)      + NL

            + logRGB(_lineColor_New_Light)   + NL
            + logRGB(_lineColor_New_Dark)    + NL

            + logRGB(_textColor_New_Light)   + NL
            + logRGB(_textColor_New_Dark)    + NL

      ;
// SET_FORMATTING_ON
   }
}
