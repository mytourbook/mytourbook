/*******************************************************************************
 * Copyright (C) 2024, 2025 Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.map;

import java.util.Collection;

import net.tourbook.common.UI;
import net.tourbook.map2.view.Map2Point;
import net.tourbook.map25.layer.marker.algorithm.distance.StaticCluster;

import org.eclipse.swt.graphics.Rectangle;

public class PaintedMarkerCluster {

   private static final char NL = UI.NEW_LINE;

   public Map2Point[]        allClusterMarker;

   public Rectangle          clusterSymbolRectangle;
   public Rectangle          clusterSymbolRectangle_Unscaled;

   public String             clusterLabel;
   public int                clusterLabelDevX;
   public int                clusterLabelDevY;

   public PaintedMarkerCluster(final StaticCluster<?> staticCluster,
                               final Rectangle clusterSymbolRectangle,
                               final Rectangle clusterSymbolRectangle_Unscaled,

                               final String clusterLabel,
                               final int clusterLabelDevX,
                               final int clusterLabelDevY) {

      this.clusterSymbolRectangle = clusterSymbolRectangle;
      this.clusterSymbolRectangle_Unscaled = clusterSymbolRectangle_Unscaled;

      this.clusterLabel = clusterLabel;
      this.clusterLabelDevX = clusterLabelDevX;
      this.clusterLabelDevY = clusterLabelDevY;

      // speedup access to the cluster items and convert type
      final Collection<?> allItems = staticCluster.getItems();
      allClusterMarker = allItems.toArray(new Map2Point[allItems.size()]);
   }

   @Override
   public String toString() {

//    final int maxLen = 5;

      return UI.EMPTY_STRING

            + "PaintedMarkerCluster" + NL //                                  //$NON-NLS-1$

            + " clusterLabel           = " + clusterLabel + NL //             //$NON-NLS-1$
            + " clusterLabelDevX       = " + clusterLabelDevX + NL //         //$NON-NLS-1$
            + " clusterLabelDevY       = " + clusterLabelDevY + NL //         //$NON-NLS-1$
            + " clusterSymbolRectangle = " + clusterSymbolRectangle + NL //   //$NON-NLS-1$

//            + " markerCluster    = " + markerCluster + NL //          //$NON-NLS-1$

//            + " allClusterItemsAsArray=" + (allClusterItemsAsArray != null
//                  ? Arrays.asList(allClusterItemsAsArray).subList(0, Math.min(allClusterItemsAsArray.length, maxLen))
//                  : null) + UI.NEW_LINE //
      ;
   }
}
