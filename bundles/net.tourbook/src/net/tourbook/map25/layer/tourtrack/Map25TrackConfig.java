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
package net.tourbook.map25.layer.tourtrack;

import static net.tourbook.common.formatter.CodeFormatter.RGB;
import static net.tourbook.common.formatter.CodeFormatter.RGBA;

import net.tourbook.common.UI;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.map.MapUI.DirectionArrowDesign;
import net.tourbook.common.map.MapUI.LegendUnitLayout;
import net.tourbook.map25.Map25ConfigManager;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;

public class Map25TrackConfig {

   private static final char NL = UI.NEW_LINE;

   /*
    * Set default values also here to ensure that a valid value is set. A default value would not
    * be set when an xml tag is not available.
    */

// SET_FORMATTING_OFF

   public String  id                            = Long.toString(System.nanoTime());
   public String  defaultId                     = Map25ConfigManager.CONFIG_DEFAULT_ID_1;
   public String  name                          = Map25ConfigManager.CONFIG_DEFAULT_ID_1;

   // line

   public LineColorMode lineColorMode           = Map25ConfigManager.LINE_COLOR_MODE_DEFAULT;
   public MapGraphId    gradientColorGraphID    = Map25ConfigManager.LINE_GRADIENT_COLOR_GRAPH_ID_DEFAULT;
   public RGB           lineColor               = Map25ConfigManager.LINE_COLOR_DEFAULT;
   public int           lineOpacity             = Map25ConfigManager.LINE_OPACITY_DEFAULT;
   public float         lineWidth               = Map25ConfigManager.LINE_WIDTH_DEFAULT;

   public boolean       isTrackVerticalOffset   = Map25ConfigManager.TRACK_IS_VERTICAL_OFFSET_DEFAULT;
   public int           trackVerticalOffset     = Map25ConfigManager.TRACK_VERTICAL_OFFSET_DEFAULT;

   // outline
   public boolean isShowOutline                 = Map25ConfigManager.OUTLINE_IS_SHOW_OUTLINE_DEFAULT;
   public float   outlineBrighness              = Map25ConfigManager.OUTLINE_BRIGHTNESS_DEFAULT;
   public float   outlineWidth                  = Map25ConfigManager.OUTLINE_WIDTH_DEFAULT;

   // direction arrow
   public boolean isShowDirectionArrow          = Map25ConfigManager.ARROW_IS_SHOW_ARROW_DEFAULT;
   public int     arrow_MinimumDistance         = Map25ConfigManager.ARROW_MIN_DISTANCE_DEFAULT;
   public int     arrow_MinimumDistanceAnimated = Map25ConfigManager.ARROW_MIN_DISTANCE_ANIMATED_DEFAULT;
   public int     arrow_VerticalOffset          = Map25ConfigManager.ARROW_VERTICAL_OFFSET_DEFAULT;
   public DirectionArrowDesign arrow_Design     = Map25ConfigManager.ARROW_DESIGN_DEFAULT;

   public int     arrow_Scale                   = Map25ConfigManager.ARROW_SCALE_DEFAULT;
   public int     arrow_Length                  = Map25ConfigManager.ARROW_LENGTH_DEFAULT;
   public int     arrow_LengthCenter            = Map25ConfigManager.ARROW_LENGTH_CENTER_DEFAULT;
   public int     arrow_Width                   = Map25ConfigManager.ARROW_WIDTH_DEFAULT;
   public int     arrow_Height                  = Map25ConfigManager.ARROW_HEIGHT_DEFAULT;

   public int     arrowFin_OutlineWidth         = Map25ConfigManager.ARROW_FIN_OUTLINE_WIDTH_DEFAULT;
   public int     arrowWing_OutlineWidth        = Map25ConfigManager.ARROW_WING_OUTLINE_WIDTH_DEFAULT;

   public RGBA    arrowFin_InsideColor          = Map25ConfigManager.ARROW_FIN_INSIDE_COLOR_DEFAULT;
   public RGBA    arrowFin_OutlineColor         = Map25ConfigManager.ARROW_FIN_OUTLINE_COLOR_DEFAULT;
   public RGBA    arrowWing_InsideColor         = Map25ConfigManager.ARROW_WING_INSIDE_COLOR_DEFAULT;
   public RGBA    arrowWing_OutlineColor        = Map25ConfigManager.ARROW_WING_OUTLINE_COLOR_DEFAULT;


   // slider location
   public boolean isShowSliderLocation          = Map25ConfigManager.SLIDER_IS_SHOW_CHART_SLIDER_DEFAULT;
   public RGB     sliderLocation_Left_Color     = Map25ConfigManager.SLIDER_LOCATION_LEFT_COLOR_DEFAULT;
   public RGB     sliderLocation_Right_Color    = Map25ConfigManager.SLIDER_LOCATION_RIGHT_COLOR_DEFAULT;
   public int     sliderLocation_Opacity        = Map25ConfigManager.SLIDER_LOCATION_OPACITY_DEFAULT;
   public int     sliderLocation_Size           = Map25ConfigManager.SLIDER_LOCATION_SIZE_DEFAULT;

   // slider path
   public boolean isShowSliderPath              = Map25ConfigManager.SLIDER_IS_SHOW_SLIDER_PATH_DEFAULT;
   public RGB     sliderPath_Color              = Map25ConfigManager.SLIDER_PATH_COLOR_DEFAULT;
   public float   sliderPath_LineWidth          = Map25ConfigManager.SLIDER_PATH_LINE_WIDTH_DEFAULT;
   public int     sliderPath_Opacity            = Map25ConfigManager.SLIDER_PATH_OPACITY_DEFAULT;

   // legend
   public LegendUnitLayout legendUnitLayout     = Map25ConfigManager.LEGEND_UNIT_LAYOUT_DEFAULT;

// SET_FORMATTING_ON

   /**
    * Arrow colors used in the shader
    */
   private float[] arrowColors;

   public enum LineColorMode {

      GRADIENT, //
      SOLID
   }

   /**
    * Animation time in milli seconds when a tour is synched with the map, default is 2000 ms
    */
//      public int            animationTime                              = Map25ConfigManager.DEFAULT_ANIMATION_TIME;

   public Map25TrackConfig() {}

   /**
    * Create a copy of this object.
    *
    * @return a copy of this <code>Insets</code> object.
    */
   @Override
   public Object clone() {
      try {
         return super.clone();
      } catch (final CloneNotSupportedException e) {
         // this shouldn't happen, since we are Cloneable
         throw new InternalError();
      }
   }

   public String createFormattedCode() {

      final String CONFIG = "config.";//$NON-NLS-1$
      final String EOL = UI.EMPTY_STRING + UI.SYMBOL_SEMICOLON + NL;

      // used for floating point values
      final String fEOL = "f" + EOL;//$NON-NLS-1$

      return UI.EMPTY_STRING

            + "Map25TrackConfig" + NL //                                                                 //$NON-NLS-1$

            + "[" + NL //                                                                                //$NON-NLS-1$

            + CONFIG + "id                            = " + id + EOL //                                  //$NON-NLS-1$
            + CONFIG + "defaultId                     = " + defaultId + EOL //                           //$NON-NLS-1$
            + CONFIG + "name                          = " + name + EOL //                                //$NON-NLS-1$
            + CONFIG + "isTrackVerticalOffset         = " + isTrackVerticalOffset + EOL //               //$NON-NLS-1$
            + CONFIG + "lineColor                     = " + RGB(lineColor) + EOL //                      //$NON-NLS-1$
            + CONFIG + "lineColorMode                 = " + lineColorMode + EOL //                       //$NON-NLS-1$
            + CONFIG + "lineOpacity                   = " + lineOpacity + EOL //                         //$NON-NLS-1$
            + CONFIG + "lineWidth                     = " + lineWidth + EOL //                           //$NON-NLS-1$
            + CONFIG + "trackVerticalOffset           = " + trackVerticalOffset + EOL //                 //$NON-NLS-1$
            + NL
            + CONFIG + "isShowDirectionArrow          = " + isShowDirectionArrow + EOL //                //$NON-NLS-1$
            + CONFIG + "arrow_MinimumDistance         = " + arrow_MinimumDistance + EOL //               //$NON-NLS-1$
            + CONFIG + "arrow_VerticalOffset          = " + arrow_VerticalOffset + EOL //                //$NON-NLS-1$
            + CONFIG + "arrow_Design                  = DirectionArrowDesign." + arrow_Design + EOL //   //$NON-NLS-1$

            + CONFIG + "arrow_Scale                   = " + arrow_Scale + EOL //                         //$NON-NLS-1$
            + CONFIG + "arrow_Length                  = " + arrow_Length + EOL //                        //$NON-NLS-1$
            + CONFIG + "arrow_LengthCenter            = " + arrow_LengthCenter + EOL //                  //$NON-NLS-1$
            + CONFIG + "arrow_Width                   = " + arrow_Width + EOL //                         //$NON-NLS-1$
            + CONFIG + "arrow_Height                  = " + arrow_Height + EOL //                        //$NON-NLS-1$

            + CONFIG + "arrowFin_OutlineWidth         = " + arrowFin_OutlineWidth + EOL //               //$NON-NLS-1$
            + CONFIG + "arrowWing_OutlineWidth        = " + arrowWing_OutlineWidth + EOL //              //$NON-NLS-1$

            + CONFIG + "arrowFin_InsideColor          = " + RGBA(arrowFin_InsideColor) + EOL //          //$NON-NLS-1$
            + CONFIG + "arrowFin_OutlineColor         = " + RGBA(arrowFin_OutlineColor) + EOL //         //$NON-NLS-1$
            + CONFIG + "arrowWing_InsideColor         = " + RGBA(arrowWing_InsideColor) + EOL //         //$NON-NLS-1$
            + CONFIG + "arrowWing_OutlineColor        = " + RGBA(arrowWing_OutlineColor) + EOL //        //$NON-NLS-1$
            + NL
            + CONFIG + "isShowOutline                 = " + isShowOutline + EOL //                       //$NON-NLS-1$
            + CONFIG + "outlineBrighness              = " + outlineBrighness + fEOL //                   //$NON-NLS-1$
            + CONFIG + "outlineWidth                  = " + outlineWidth + fEOL //                       //$NON-NLS-1$
            + NL
            + CONFIG + "isShowSliderLocation          = " + isShowSliderLocation + EOL //                //$NON-NLS-1$
            + CONFIG + "sliderLocation_Left_Color     = " + RGB(sliderLocation_Left_Color) + EOL //      //$NON-NLS-1$
            + CONFIG + "sliderLocation_Right_Color    = " + RGB(sliderLocation_Right_Color) + EOL //     //$NON-NLS-1$
            + CONFIG + "sliderLocation_Opacity        = " + sliderLocation_Opacity + EOL //              //$NON-NLS-1$
            + CONFIG + "sliderLocation_Size           = " + sliderLocation_Size + EOL //                 //$NON-NLS-1$
            + NL
            + CONFIG + "isShowSliderPath              = " + isShowSliderPath + EOL //                    //$NON-NLS-1$
            + CONFIG + "sliderPath_Color              = " + RGB(sliderPath_Color) + EOL //               //$NON-NLS-1$
            + CONFIG + "sliderPath_LineWidth          = " + sliderPath_LineWidth + EOL //                //$NON-NLS-1$
            + CONFIG + "sliderPath_Opacity            = " + sliderPath_Opacity + EOL //                  //$NON-NLS-1$

            + "]" + NL //                                                                                //$NON-NLS-1$
      ;
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

      final Map25TrackConfig other = (Map25TrackConfig) obj;

      if (id == null) {
         if (other.id != null) {
            return false;
         }
      } else if (!id.equals(other.id)) {
         return false;
      }

      return true;
   }

   public float[] getArrowColors() {

      if (arrowColors != null) {
         return arrowColors;
      }

      updateShaderArrowColors();

      return arrowColors;
   }

   @Override
   public int hashCode() {

      final int prime = 31;
      int result = 1;
      result = prime * result + ((id == null) ? 0 : id.hashCode());

      return result;
   }

   @Override
   public String toString() {

      return createFormattedCode();
   }

   public void updateShaderArrowColors() {

      /**
       * <code>
       *
       *  uniform  vec4  uni_ArrowColors[4];  // 1:wing inside,
       *                                      // 2:wing outline,
       *                                      // 3:fin inside,
       *                                      // 4:fin outline
       * </code>
       */

// SET_FORMATTING_OFF

      arrowColors = new float[] {

            arrowWing_InsideColor.rgb.red    / 255f,
            arrowWing_InsideColor.rgb.green  / 255f,
            arrowWing_InsideColor.rgb.blue   / 255f,
            arrowWing_InsideColor.alpha      / 255f,

            arrowWing_OutlineColor.rgb.red   / 255f,
            arrowWing_OutlineColor.rgb.green / 255f,
            arrowWing_OutlineColor.rgb.blue  / 255f,
            arrowWing_OutlineColor.alpha     / 255f,

            arrowFin_InsideColor.rgb.red     / 255f,
            arrowFin_InsideColor.rgb.green   / 255f,
            arrowFin_InsideColor.rgb.blue    / 255f,
            arrowFin_InsideColor.alpha       / 255f,

            arrowFin_OutlineColor.rgb.red    / 255f,
            arrowFin_OutlineColor.rgb.green  / 255f,
            arrowFin_OutlineColor.rgb.blue   / 255f,
            arrowFin_OutlineColor.alpha      / 255f,
      };

// SET_FORMATTING_ON
   }

}
