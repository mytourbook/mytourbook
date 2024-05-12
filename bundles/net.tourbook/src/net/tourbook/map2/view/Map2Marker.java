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
import net.tourbook.data.TourMarker;
import net.tourbook.map25.layer.marker.algorithm.distance.ClusterItem;

import org.oscim.core.GeoPoint;

/**
 * Immutable class describing a GeoPoint with a Title and a Description
 */
public class Map2Marker implements ClusterItem {

   public GeoPoint   geoPoint;

   /**
    * Geo position in device pixel for the marker position
    */
   public int        geoPointDevX;
   public int        geoPointDevY;

   public TourMarker tourMarker;

   /**
    * @param tourMarker
    * @param geoPoint
    */
   public Map2Marker(final TourMarker tourMarker,
                     final GeoPoint geoPoint) {

      this.geoPoint = geoPoint;
      this.tourMarker = tourMarker;
   }

   @Override
   public GeoPoint getPosition() {
      return geoPoint;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "Map2Marker" //$NON-NLS-1$

            + " geoPoint =" + geoPoint + ", " //$NON-NLS-1$ //$NON-NLS-2$

            + UI.NEW_LINE;
   }

}
