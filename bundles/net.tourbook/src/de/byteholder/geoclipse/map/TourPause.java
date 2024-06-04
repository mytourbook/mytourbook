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

import net.tourbook.common.map.GeoPosition;
import net.tourbook.data.TourData;

import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

public class TourPause {

   public long                              startTime;
   public long                              duration;

   public boolean                           isAutoPause;

   public GeoPosition                       geoPosition;

   public TourData                          tourData;

   /**
    * Caches the world positions for the pause lat/long values for each zoom level
    */
   private IntObjectHashMap<java.awt.Point> _worldPixelPositions;


   public TourPause() {}

   public java.awt.Point getWorldPixelPosition(final int zoomLevel) {

      if (_worldPixelPositions != null) {

         return _worldPixelPositions.get(zoomLevel);
      }

      return null;
   }

   /**
    * @param geoPosition
    * @param worldPixelPosition
    * @param zoomLevel
    */
   public void setPosition(final GeoPosition geoPosition,
                           final java.awt.Point worldPixelPosition,
                           final int zoomLevel) {

      this.geoPosition = geoPosition;

      if (_worldPixelPositions == null) {

         _worldPixelPositions = new IntObjectHashMap<>();
      }

      _worldPixelPositions.put(zoomLevel, worldPixelPosition);
   }

}
