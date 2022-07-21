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

import net.tourbook.common.UI;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.map.MapUI.LegendUnitLayout;
import net.tourbook.map25.Map25ConfigManager;

import org.eclipse.swt.graphics.RGB;

public class Map25TrackConfig {

   private static final char NL = UI.NEW_LINE;

   /*
    * Set default values also here to ensure that a valid value is set. A default value would not
    * be set when an xml tag is not available.
    */

// SET_FORMATTING_OFF

   public String  id                         = Long.toString(System.nanoTime());
   public String  defaultId                  = Map25ConfigManager.CONFIG_DEFAULT_ID_1;
   public String  name                       = Map25ConfigManager.CONFIG_DEFAULT_ID_1;

   // line
   public boolean isShowDirectionArrow       = Map25ConfigManager.LINE_IS_SHOW_DIRECTION_ARROW_DEFAULT;
   public boolean isTrackVerticalOffset      = Map25ConfigManager.LINE_IS_TRACK_VERTICAL_OFFSET_DEFAULT;

   public LineColorMode lineColorMode        = Map25ConfigManager.LINE_COLOR_MODE_DEFAULT;
   public MapGraphId    gradientColorGraphID = Map25ConfigManager.LINE_GRADIENT_COLOR_GRAPH_ID_DEFAULT;
   public RGB           lineColor            = Map25ConfigManager.LINE_COLOR_DEFAULT;
   public int           lineOpacity          = Map25ConfigManager.LINE_OPACITY_DEFAULT;
   public float         lineWidth            = Map25ConfigManager.LINE_WIDTH_DEFAULT;
   public int           trackVerticalOffset  = Map25ConfigManager.LINE_TRACK_VERTICAL_OFFSET_DEFAULT;

   // outline
   public boolean isShowOutline              = Map25ConfigManager.OUTLINE_IS_SHOW_OUTLINE_DEFAULT;
   public float   outlineBrighness           = Map25ConfigManager.OUTLINE_BRIGHTNESS_DEFAULT;
   public float   outlineWidth               = Map25ConfigManager.OUTLINE_WIDTH_DEFAULT;

   // slider location
   public boolean isShowSliderLocation       = Map25ConfigManager.SLIDER_IS_SHOW_CHART_SLIDER_DEFAULT;
   public RGB     sliderLocation_Left_Color  = Map25ConfigManager.SLIDER_LOCATION_LEFT_COLOR_DEFAULT;
   public RGB     sliderLocation_Right_Color = Map25ConfigManager.SLIDER_LOCATION_RIGHT_COLOR_DEFAULT;
   public int     sliderLocation_Opacity     = Map25ConfigManager.SLIDER_LOCATION_OPACITY_DEFAULT;
   public int     sliderLocation_Size        = Map25ConfigManager.SLIDER_LOCATION_SIZE_DEFAULT;

   // slider path
   public boolean isShowSliderPath           = Map25ConfigManager.SLIDER_IS_SHOW_SLIDER_PATH_DEFAULT;
   public RGB     sliderPath_Color           = Map25ConfigManager.SLIDER_PATH_COLOR_DEFAULT;
   public float   sliderPath_LineWidth       = Map25ConfigManager.SLIDER_PATH_LINE_WIDTH_DEFAULT;
   public int     sliderPath_Opacity         = Map25ConfigManager.SLIDER_PATH_OPACITY_DEFAULT;
   public int     testValue                  = 50;

   // legend
   public LegendUnitLayout legendUnitLayout  = Map25ConfigManager.LEGEND_UNIT_LAYOUT_DEFAULT;

// SET_FORMATTING_ON

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

   @Override
   public int hashCode() {

      final int prime = 31;
      int result = 1;
      result = prime * result + ((id == null) ? 0 : id.hashCode());

      return result;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "Map25TrackConfig" + NL //                                         //$NON-NLS-1$

            + "[" + NL //                                                        //$NON-NLS-1$

            + "id                         =" + id + NL //                        //$NON-NLS-1$
            + "defaultId                  =" + defaultId + NL //                 //$NON-NLS-1$
            + "name                       =" + name + NL //                      //$NON-NLS-1$
            + "isShowDirectionArrow       =" + isShowDirectionArrow + NL //      //$NON-NLS-1$
            + "isTrackVerticalOffset      =" + isTrackVerticalOffset + NL //     //$NON-NLS-1$
            + "lineColor                  =" + lineColor + NL //                 //$NON-NLS-1$
            + "lineColorMode              =" + lineColorMode + NL //             //$NON-NLS-1$
            + "lineOpacity                =" + lineOpacity + NL //               //$NON-NLS-1$
            + "lineWidth                  =" + lineWidth + NL //                 //$NON-NLS-1$
            + "trackVerticalOffset        =" + trackVerticalOffset + NL //       //$NON-NLS-1$

            + "isShowOutline              =" + isShowOutline + NL //             //$NON-NLS-1$
            + "outlineBrighness           =" + outlineBrighness + NL //          //$NON-NLS-1$
            + "outlineWidth               =" + outlineWidth + NL //              //$NON-NLS-1$

            + "isShowSliderLocation       =" + isShowSliderLocation + NL //      //$NON-NLS-1$
            + "sliderLocation_Left_Color  =" + sliderLocation_Left_Color + NL // //$NON-NLS-1$
            + "sliderLocation_Right_Color =" + sliderLocation_Right_Color + NL ////$NON-NLS-1$
            + "sliderLocation_Opacity     =" + sliderLocation_Opacity + NL //    //$NON-NLS-1$
            + "sliderLocation_Size        =" + sliderLocation_Size + NL //       //$NON-NLS-1$

            + "isShowSliderPath           =" + isShowSliderPath + NL //          //$NON-NLS-1$
            + "sliderPath_Color           =" + sliderPath_Color + NL //          //$NON-NLS-1$
            + "sliderPath_LineWidth       =" + sliderPath_LineWidth + NL //      //$NON-NLS-1$
            + "sliderPath_Opacity         =" + sliderPath_Opacity + NL //        //$NON-NLS-1$

            + "testValue                  =" + testValue + NL //                 //$NON-NLS-1$

            + "]" + NL //                                                        //$NON-NLS-1$
      ;
   }

}
