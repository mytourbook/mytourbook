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

import java.util.Arrays;
import java.util.List;

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

   private static final String    NL               = UI.NEW_LINE1;

   private final IPreferenceStore _commonPrefStore = CommonActivator.getPrefStore();

   private String                 _colorDefinitionId;
   private String                 _visibleName;
   private String                 _graphPrefNamePrefix;

   /**
    * These are children in the tree viewer
    */
   private GraphColorItem[]       _allGraphColorItems;

   private RGB                    _gradient_Active_Bright;
   private RGB                    _gradientBright_Default;
   private RGB                    _gradient_New_Bright;

   private RGB                    _gradient_Active_Dark;
   private RGB                    _gradientDark_Default;
   private RGB                    _gradient_New_Dark;

   private RGB                    _lineColor_Active_LightTheme;
   private RGB                    _lineColor_Active_DarkTheme;
   private RGB                    _lineColor_Default_LightTheme;
   private RGB                    _lineColor_Default_DarkTheme;
   private RGB                    _lineColor_New_LightTheme;
   private RGB                    _lineColor_New_DarkTheme;

   private RGB                    _textColor_Active_LightTheme;
   private RGB                    _textColor_Active_DarkTheme;
   private RGB                    _textColor_Default_LightTheme;
   private RGB                    _textColor_Default_DarkTheme;
   private RGB                    _textColor_New_LightTheme;
   private RGB                    _textColor_New_DarkTheme;

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

                             final RGB defaultLineColor_LightTheme,
                             final RGB defaultTextColor_LightTheme,

                             final Map2ColorProfile defaultMapColorProfile) {

      this(

            colorDefinitionId,
            visibleName,

            defaultGradientBright,
            defaultGradientDark,

            defaultLineColor_LightTheme,
            defaultLineColor_LightTheme,

            defaultTextColor_LightTheme,
            defaultTextColor_LightTheme,

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
    * @param defaultLineColor_LightTheme
    * @param defaultLineColor_DarkTheme
    * @param defaultTextColor_LightTheme
    * @param defaultTextColor_DarkTheme
    * @param defaultMapColorProfile
    *           Map color configuration or <code>null</code> when not available
    */
   protected ColorDefinition(final String colorDefinitionId,
                             final String visibleName,

                             final RGB defaultGradientBright,
                             final RGB defaultGradientDark,

                             final RGB defaultLineColor_LightTheme,
                             final RGB defaultLineColor_DarkTheme,

                             final RGB defaultTextColor_LightTheme,
                             final RGB defaultTextColor_DarkTheme,

                             final Map2ColorProfile defaultMapColorProfile) {

      _colorDefinitionId = colorDefinitionId;
      _visibleName = visibleName;

      _gradientBright_Default = defaultGradientBright;
      _gradientDark_Default = defaultGradientDark;

      _lineColor_Default_LightTheme = defaultLineColor_LightTheme;
      _lineColor_Default_DarkTheme = defaultLineColor_DarkTheme;

      _textColor_Default_LightTheme = defaultTextColor_LightTheme;
      _textColor_Default_DarkTheme = defaultTextColor_DarkTheme;

      _map2ColorProfile_Default = defaultMapColorProfile;

      _graphPrefNamePrefix = ICommonPreferences.GRAPH_COLORS + _colorDefinitionId + UI.SYMBOL_DOT;

      /*
       * Gradient color
       */
      final String prefColorGradientBright = getGraphPrefName(GraphColorManager.PREF_COLOR_GRADIENT_BRIGHT);
      final String prefColorGradientDark = getGraphPrefName(GraphColorManager.PREF_COLOR_GRADIENT_DARK);

      if (_commonPrefStore.contains(prefColorGradientBright)) {
         _gradient_Active_Bright = PreferenceConverter.getColor(_commonPrefStore, prefColorGradientBright);
      } else {
         _gradient_Active_Bright = _gradientBright_Default;
      }

      if (_commonPrefStore.contains(prefColorGradientDark)) {
         _gradient_Active_Dark = PreferenceConverter.getColor(_commonPrefStore, prefColorGradientDark);
      } else {
         _gradient_Active_Dark = _gradientDark_Default;
      }

      _gradient_New_Bright = _gradient_Active_Bright;
      _gradient_New_Dark = _gradient_Active_Dark;

      /*
       * Line color
       */
      final String prefColorLine_Light = getGraphPrefName(GraphColorManager.PREF_COLOR_LINE_LIGHT);
      final String prefColorLine_Dark = getGraphPrefName(GraphColorManager.PREF_COLOR_LINE_DARK);

      if (_commonPrefStore.contains(prefColorLine_Light)) {
         _lineColor_Active_LightTheme = PreferenceConverter.getColor(_commonPrefStore, prefColorLine_Light);
      } else {
         _lineColor_Active_LightTheme = _lineColor_Default_LightTheme;
      }
      if (_commonPrefStore.contains(prefColorLine_Dark)) {
         _lineColor_Active_DarkTheme = PreferenceConverter.getColor(_commonPrefStore, prefColorLine_Dark);
      } else {
         _lineColor_Active_DarkTheme = _lineColor_Default_DarkTheme;
      }

      _lineColor_New_LightTheme = _lineColor_Active_LightTheme;
      _lineColor_New_DarkTheme = _lineColor_Active_DarkTheme;

      /*
       * Text color
       */
      final String prefColorText_Light = getGraphPrefName(GraphColorManager.PREF_COLOR_TEXT_LIGHT);
      final String prefColorText_Dark = getGraphPrefName(GraphColorManager.PREF_COLOR_TEXT_DARK);

      if (_commonPrefStore.contains(prefColorText_Light)) {
         _textColor_Active_LightTheme = PreferenceConverter.getColor(_commonPrefStore, prefColorText_Light);
      } else {
         _textColor_Active_LightTheme = _textColor_Default_LightTheme;
      }
      if (_commonPrefStore.contains(prefColorText_Dark)) {
         _textColor_Active_DarkTheme = PreferenceConverter.getColor(_commonPrefStore, prefColorText_Dark);
      } else {
         _textColor_Active_DarkTheme = _textColor_Default_DarkTheme;
      }

      _textColor_New_LightTheme = _textColor_Active_LightTheme;
      _textColor_New_DarkTheme = _textColor_Active_DarkTheme;
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
      return _gradient_Active_Bright;
   }

   public RGB getGradientBright_Default() {
      return _gradientBright_Default;
   }

   public RGB getGradientBright_New() {
      return _gradient_New_Bright;
   }

   public RGB getGradientDark_Active() {
      return _gradient_Active_Dark;
   }

   public RGB getGradientDark_Default() {
      return _gradientDark_Default;
   }

   public RGB getGradientDark_New() {
      return _gradient_New_Dark;
   }

   public GraphColorItem[] getGraphColorItems() {
      return _allGraphColorItems;
   }

   public String getGraphPrefName(final String graphColorName) {

      final String graphPrefName = _graphPrefNamePrefix + graphColorName;

      return graphPrefName;
   }

   public RGB getLineColor_Active_Dark() {
      return _lineColor_Active_DarkTheme;
   }

   public RGB getLineColor_Active_Light() {
      return _lineColor_Active_LightTheme;
   }

   public RGB getLineColor_Active_Themed() {

      return UI.IS_DARK_THEME ? _lineColor_Active_DarkTheme : _lineColor_Active_LightTheme;
   }

   public RGB getLineColor_Default_Dark() {
      return _lineColor_Default_DarkTheme;
   }

   public RGB getLineColor_Default_Light() {
      return _lineColor_Default_LightTheme;
   }

   public RGB getLineColor_New_Dark() {
      return _lineColor_New_DarkTheme;
   }

   public RGB getLineColor_New_Light() {
      return _lineColor_New_LightTheme;
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
      return _textColor_Active_DarkTheme;
   }

   public RGB getTextColor_Active_Light() {
      return _textColor_Active_LightTheme;
   }

   public RGB getTextColor_Active_Themed() {

      return UI.IS_DARK_THEME ? _textColor_Active_DarkTheme : _textColor_Active_LightTheme;
   }

   public RGB getTextColor_Default_Dark() {
      return _textColor_Default_DarkTheme;
   }

   public RGB getTextColor_Default_Light() {
      return _textColor_Default_LightTheme;
   }

   public RGB getTextColor_New_Dark() {
      return _textColor_New_DarkTheme;
   }

   public RGB getTextColor_New_Light() {
      return _textColor_New_LightTheme;
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

   public String logColorCode() {

      return NL + NL

            + "_colorDefinitionId = " + _colorDefinitionId + NL + NL //$NON-NLS-1$

            + UI.logRGB(_gradient_New_Bright) + NL
            + UI.logRGB(_gradient_New_Dark) + NL

            + UI.logRGB(_lineColor_New_LightTheme) + NL
            + UI.logRGB(_lineColor_New_DarkTheme) + NL

            + UI.logRGB(_textColor_New_LightTheme) + NL
            + UI.logRGB(_textColor_New_DarkTheme) + NL

      ;
   }

   public void resetToDefaultColors() {

//      _map2ColorProfile_Default = defaultMapColorProfile;

// SET_FORMATTING_OFF

      _gradient_Active_Bright       = _gradientBright_Default;
      _gradient_Active_Dark         = _gradientDark_Default;
      _gradient_New_Bright          = _gradientBright_Default;
      _gradient_New_Dark            = _gradientDark_Default;

      _lineColor_Active_LightTheme  = _lineColor_Default_LightTheme;
      _lineColor_Active_DarkTheme   = _lineColor_Default_DarkTheme;
      _lineColor_New_LightTheme     = _lineColor_Default_LightTheme;
      _lineColor_New_DarkTheme      = _lineColor_Default_DarkTheme;

      _textColor_Active_LightTheme  = _textColor_Default_LightTheme;
      _textColor_Active_DarkTheme   = _textColor_Default_DarkTheme;
      _textColor_New_LightTheme     = _textColor_Default_LightTheme;
      _textColor_New_DarkTheme      = _textColor_Default_DarkTheme;

// SET_FORMATTING_ON
   }

   /**
    * Set color items for this color definition, the color items are children in the tree.
    *
    * @param children
    */
   public void setColorItems(final GraphColorItem[] children) {
      _allGraphColorItems = children;
   }

   /**
    * Set color from another {@link ColorDefinition}
    *
    * @param colorDefinition
    */
   public void setColors(final ColorDefinition colorDefinition) {

// SET_FORMATTING_OFF

      _gradient_Active_Bright       = colorDefinition.getGradientBright_New();
      _gradient_Active_Dark         = colorDefinition.getGradientDark_New();
      _gradient_New_Bright          = colorDefinition.getGradientBright_New();
      _gradient_New_Dark            = colorDefinition.getGradientDark_New();

      _lineColor_Active_LightTheme  = colorDefinition.getLineColor_New_Light();
      _lineColor_Active_DarkTheme   = colorDefinition.getLineColor_New_Dark();
      _lineColor_New_LightTheme     = colorDefinition.getLineColor_New_Light();
      _lineColor_New_DarkTheme      = colorDefinition.getLineColor_New_Dark();

      _textColor_Active_LightTheme  = colorDefinition.getTextColor_New_Light();
      _textColor_Active_DarkTheme   = colorDefinition.getTextColor_New_Dark();
      _textColor_New_LightTheme     = colorDefinition.getTextColor_New_Light();
      _textColor_New_DarkTheme      = colorDefinition.getTextColor_New_Dark();

// SET_FORMATTING_ON
   }

   public void setGradientBright_Active(final RGB gradientBright) {
      _gradient_Active_Bright = gradientBright;
   }

   public void setGradientBright_New(final RGB newGradientBright) {
      _gradient_New_Bright = newGradientBright;
   }

   public void setGradientDark_Active(final RGB gradientDark) {
      _gradient_Active_Dark = gradientDark;
   }

   public void setGradientDark_New(final RGB newGradientDark) {
      _gradient_New_Dark = newGradientDark;
   }

   public void setLineColor_Active_DarkTheme(final RGB lineColor) {
      _lineColor_Active_DarkTheme = lineColor;
   }

   public void setLineColor_Active_LightTheme(final RGB lineColor) {
      _lineColor_Active_LightTheme = lineColor;
   }

   public void setLineColor_New_DarkTheme(final RGB newLineColor) {
      _lineColor_New_DarkTheme = newLineColor;
   }

   public void setLineColor_New_LightTheme(final RGB newLineColor) {
      _lineColor_New_LightTheme = newLineColor;
   }

   public void setMap2Color_Active(final Map2ColorProfile mapColor) {
      _map2ColorProfile_Active = mapColor;
   }

   public void setMap2Color_New(final Map2ColorProfile newMapColor) {
      _map2ColorProfile_New = newMapColor;
   }

   public void setTextColor_Active_DarkTheme(final RGB textColor) {
      _textColor_Active_DarkTheme = textColor;
   }

   public void setTextColor_Active_LightTheme(final RGB textColor) {
      _textColor_Active_LightTheme = textColor;
   }

   public void setTextColor_New_DarkTheme(final RGB textColorNew) {
      _textColor_New_DarkTheme = textColorNew;
   }

   public void setTextColor_New_LightTheme(final RGB textColorNew) {
      _textColor_New_LightTheme = textColorNew;
   }

   public void setVisibleName(final String visibleName) {
      _visibleName = visibleName;
   }

   @Override
   public String toString() {

      final int maxLen = 5;

      final List<GraphColorItem> allGraphColorItems = _allGraphColorItems != null
            ? Arrays.asList(_allGraphColorItems).subList(0, Math.min(_allGraphColorItems.length, maxLen))
            : null;

      return UI.EMPTY_STRING

            + "ColorDefinition" + NL //                                                      //$NON-NLS-1$

            + "  _colorDefinitionId             =" + _colorDefinitionId + NL //              //$NON-NLS-1$
            + "  _visibleName                   =" + _visibleName + NL //                    //$NON-NLS-1$
            + "  _graphPrefNamePrefix           =" + _graphPrefNamePrefix + NL //            //$NON-NLS-1$

//          + "  _gradientBright_Active         =" + _gradientBright_Active + NL //          //$NON-NLS-1$
//          + "  _gradientBright_Default        =" + _gradientBright_Default + NL //         //$NON-NLS-1$
            + "  _gradient_New_Bright           =" + _gradient_New_Bright + NL //             //$NON-NLS-1$
//          + "  _gradientDark_Active           =" + _gradientDark_Active + NL //            //$NON-NLS-1$
//          + "  _gradientDark_Default          =" + _gradientDark_Default + NL //           //$NON-NLS-1$
            + "  _gradient_New_Dark             =" + _gradient_New_Dark + NL //               //$NON-NLS-1$
//
//          + "  _lineColor_Active_LightTheme   =" + _lineColor_Active_LightTheme + NL //    //$NON-NLS-1$
//          + "  _lineColor_Active_DarkTheme    =" + _lineColor_Active_DarkTheme + NL //     //$NON-NLS-1$
//          + "  _lineColor_Default_LightTheme  =" + _lineColor_Default_LightTheme + NL //   //$NON-NLS-1$
//          + "  _lineColor_Default_DarkTheme   =" + _lineColor_Default_DarkTheme + NL //    //$NON-NLS-1$
            + "  _lineColor_New_LightTheme      =" + _lineColor_New_LightTheme + NL //       //$NON-NLS-1$
            + "  _lineColor_New_DarkTheme       =" + _lineColor_New_DarkTheme + NL //        //$NON-NLS-1$
//
//          + "  _textColor_Active_LightTheme   =" + _textColor_Active_LightTheme + NL //    //$NON-NLS-1$
//          + "  _textColor_Active_DarkTheme    =" + _textColor_Active_DarkTheme + NL //     //$NON-NLS-1$
//          + "  _textColor_Default_LightTheme  =" + _textColor_Default_LightTheme + NL //   //$NON-NLS-1$
//          + "  _textColor_Default_DarkTheme   =" + _textColor_Default_DarkTheme + NL //    //$NON-NLS-1$
            + "  _textColor_New_LightTheme      =" + _textColor_New_LightTheme + NL //       //$NON-NLS-1$
            + "  _textColor_New_DarkTheme       =" + _textColor_New_DarkTheme + NL //        //$NON-NLS-1$

            + "  _graphColorItems               =" + NL + allGraphColorItems + NL //         //$NON-NLS-1$

//          + "  _map2ColorProfile_Active       =" + _map2ColorProfile_Active + NL //        //$NON-NLS-1$
//          + "  _map2ColorProfile_Default      =" + _map2ColorProfile_Default + NL //       //$NON-NLS-1$
//          + "  _map2ColorProfile_New          =" + _map2ColorProfile_New + NL //           //$NON-NLS-1$
      ;
   }

}
