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

import org.eclipse.swt.graphics.Color;
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

   public String id        = Long.toString(System.nanoTime());
   public String defaultId = Map2ConfigManager.CONFIG_DEFAULT_ID_1;
   public String name      = Map2ConfigManager.CONFIG_DEFAULT_ID_1;

   /*
    * Marker
    */
   public boolean        isShowTourMarker          = true;
   public boolean        isMarkerLabelAntialiased  = true;

   public MapLabelLayout markerLabelLayout         = Map2ConfigManager.MARKER_LABEL_LAYOUT_DEFAULT;

   public RGB            markerFill_RGB            = Map2ConfigManager.DEFAULT_MARKER_FILL_RGB;
   public RGB            markerOutline_RGB         = Map2ConfigManager.DEFAULT_MARKER_OUTLINE_RGB;

   public Color          markerFill_Color;
   public Color          markerOutline_Color;

   public int            labelDistributorMaxLabels = Map2ConfigManager.LABEL_DISTRIBUTOR_MAX_LABELS_DEFAULT;
   public int            labelDistributorRadius    = Map2ConfigManager.LABEL_DISTRIBUTOR_RADIUS_DEFAULT;

   /*
    * Cluster
    */
   public boolean isMarkerClustered          = true;
   public boolean isClusterSymbolAntialiased = true;
   public boolean isClusterTextAntialiased   = true;
   public boolean isShowClusterMarker        = true;

   public boolean isFillClusterSymbol        = Map2ConfigManager.DEFAULT_IS_FILL_CLUSTER_SYMBOL;
   public int     clusterGridSize            = Map2ConfigManager.DEFAULT_CLUSTER_GRID_SIZE;
   public RGB     clusterFill_RGB            = Map2ConfigManager.DEFAULT_CLUSTER_FILL_RGB;
   public RGB     clusterOutline_RGB         = Map2ConfigManager.DEFAULT_CLUSTER_OUTLINE_RGB;
   public int     clusterOutline_Width       = Map2ConfigManager.DEFAULT_CLUSTER_OUTLINE_WIDTH;
   public int     clusterSymbol_Size         = Map2ConfigManager.DEFAULT_CLUSTER_SYMBOL_SIZE;

   public Color   clusterFill_Color;
   public Color   clusterOutline_Color;

   public void setupColors() {

      clusterFill_Color = new Color(clusterFill_RGB);
      clusterOutline_Color = new Color(clusterOutline_RGB);
      markerFill_Color = new Color(markerFill_RGB);
      markerOutline_Color = new Color(markerOutline_RGB);
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "Map2MarkerConfig" + NL //                                            //$NON-NLS-1$

            + " name                      = " + name + NL //                        //$NON-NLS-1$
            + " defaultId                 = " + defaultId + NL //                   //$NON-NLS-1$
            + " id                        = " + id + NL //                          //$NON-NLS-1$

            + " isShowTourMarker          = " + isShowTourMarker + NL //            //$NON-NLS-1$
            + " markerOutline_RGB         = " + markerOutline_RGB + NL //           //$NON-NLS-1$
            + " markerFill_RGB            = " + markerFill_RGB + NL //              //$NON-NLS-1$

            + " isMarkerClustered            = " + isMarkerClustered + NL //           //$NON-NLS-1$
            + " isClusterSymbolAntialiased   = " + isClusterSymbolAntialiased + NL //  //$NON-NLS-1$
            + " isClusterTextAntialiased     = " + isClusterTextAntialiased + NL //    //$NON-NLS-1$
            + " isFillClusterSymbol          = " + isFillClusterSymbol + NL //         //$NON-NLS-1$
            + " clusterGridSize              = " + clusterGridSize + NL //             //$NON-NLS-1$
            + " clusterSymbol_Size           = " + clusterSymbol_Size + NL //          //$NON-NLS-1$
            + " clusterOutline_Width         = " + clusterOutline_Width + NL //        //$NON-NLS-1$
            + " clusterOutline_RGB           = " + clusterOutline_RGB + NL //          //$NON-NLS-1$
            + " clusterFill_RGB              = " + clusterFill_RGB + NL //             //$NON-NLS-1$

      ;
   }

}