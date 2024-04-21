/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import net.tourbook.common.UI;

import org.eclipse.swt.graphics.RGB;

/**
 * Contains data which are needed to paint a marker into the 2D map
 */
public class Map2MarkerConfig {

   private static final char NL = UI.NEW_LINE;

   /*
    * Set default values also here to ensure that a valid value is set. A default value would not be
    * set when an xml tag is not available.
    */

   public String  id        = Long.toString(System.nanoTime());
   public String  defaultId = Map2ConfigManager.CONFIG_DEFAULT_ID_1;
   public String  name      = Map2ConfigManager.CONFIG_DEFAULT_ID_1;

   public boolean isMarkerAntialiasPainting;

   /*
    * Marker
    */
   public boolean isShowTourMarker      = true;

   public RGB     markerFill_Color      = Map2ConfigManager.DEFAULT_MARKER_FILL_COLOR;
   public int     markerFill_Opacity    = Map2ConfigManager.DEFAULT_MARKER_FILL_OPACITY;
   public RGB     markerOutline_Color   = Map2ConfigManager.DEFAULT_MARKER_OUTLINE_COLOR;
   public int     markerOutline_Opacity = Map2ConfigManager.DEFAULT_MARKER_OUTLINE_OPACITY;
   public float   markerOutline_Size    = Map2ConfigManager.DEFAULT_MARKER_OUTLINE_SIZE;
   public int     markerSymbol_Size     = Map2ConfigManager.DEFAULT_MARKER_SYMBOL_SIZE;

   /*
    * Cluster
    */
   public boolean isMarkerClustered      = true;

   public int     clusterGridSize        = Map2ConfigManager.DEFAULT_CLUSTER_GRID_SIZE;
   public RGB     clusterFill_Color      = Map2ConfigManager.DEFAULT_CLUSTER_FILL_COLOR;
   public int     clusterFill_Opacity    = Map2ConfigManager.DEFAULT_CLUSTER_FILL_OPACITY;

   public RGB     clusterOutline_Color   = Map2ConfigManager.DEFAULT_CLUSTER_OUTLINE_COLOR;
   public int     clusterOutline_Opacity = Map2ConfigManager.DEFAULT_CLUSTER_OUTLINE_OPACITY;

   public int     clusterOutline_Width   = Map2ConfigManager.DEFAULT_CLUSTER_OUTLINE_WIDTH;
   public int     clusterSymbol_Size     = Map2ConfigManager.DEFAULT_CLUSTER_SYMBOL_SIZE;

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "Map2MarkerConfig" + NL //                                            //$NON-NLS-1$

            + " name                       = " + name + NL //                       //$NON-NLS-1$
            + " defaultId                  = " + defaultId + NL //                  //$NON-NLS-1$
            + " id                         = " + id + NL //                         //$NON-NLS-1$

            + " isShowTourMarker           = " + isShowTourMarker + NL //           //$NON-NLS-1$
            + " isMarkerClustered          = " + isMarkerClustered + NL //          //$NON-NLS-1$
            + " isMarkerAntialiasPainting  = " + isMarkerAntialiasPainting + NL //  //$NON-NLS-1$

            + " markerSymbol_Size          = " + markerSymbol_Size + NL //          //$NON-NLS-1$
            + " markerOutline_Opacity      = " + markerOutline_Opacity + NL //      //$NON-NLS-1$
            + " markerOutline_Size         = " + markerOutline_Size + NL //         //$NON-NLS-1$
            + " markerOutline_Color        = " + markerOutline_Color + NL //        //$NON-NLS-1$
            + " markerFill_Color           = " + markerFill_Color + NL //           //$NON-NLS-1$
            + " markerFill_Opacity         = " + markerFill_Opacity + NL //         //$NON-NLS-1$

            + " clusterGridSize            = " + clusterGridSize + NL //            //$NON-NLS-1$
            + " clusterSymbol_Size         = " + clusterSymbol_Size + NL //         //$NON-NLS-1$
            + " clusterOutline_Width       = " + clusterOutline_Width + NL //       //$NON-NLS-1$
            + " clusterOutline_Color       = " + clusterOutline_Color + NL //       //$NON-NLS-1$
            + " clusterOutline_Opacity     = " + clusterOutline_Opacity + NL //     //$NON-NLS-1$
            + " clusterFill_Color          = " + clusterFill_Color + NL //          //$NON-NLS-1$
            + " clusterFill_Opacity        = " + clusterFill_Opacity + NL //        //$NON-NLS-1$

      ;
   }

}
