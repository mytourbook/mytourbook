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
package de.byteholder.geoclipse.map;

import net.tourbook.map25.layer.marker.algorithm.distance.StaticCluster;

import org.eclipse.swt.graphics.Rectangle;

public class PaintedMarkerCluster {

   public StaticCluster<?> markerCluster;

   public Rectangle        clusterRectangle;

   public String           clusterLabel;
   public int              clusterLabelDevX;
   public int              clusterLabelDevY;

   public PaintedMarkerCluster(final StaticCluster<?> staticCluster,
                               final Rectangle locationRectangle,
                               final String clusterLabel,
                               final int clusterLabelDevX,
                               final int clusterLabelDevY) {

      this.markerCluster = staticCluster;
      this.clusterRectangle = locationRectangle;

      this.clusterLabel = clusterLabel;
      this.clusterLabelDevX = clusterLabelDevX;
      this.clusterLabelDevY = clusterLabelDevY;
   }
}
