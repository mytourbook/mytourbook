/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.map.bookmark;

import net.tourbook.common.map.GeoPosition;

/**
 */
public class MapLocation {

   private MapPosition_with_MarkerPosition _mapPosition;

   public MapLocation(final GeoPosition geoPosition, final int mapZoomLevel) {

      _mapPosition = new MapPosition_with_MarkerPosition(

            geoPosition.latitude,
            geoPosition.longitude,

            /* set dummy */
            1);

      _mapPosition.setZoomLevel(mapZoomLevel);
   }

   public MapLocation(final MapPosition_with_MarkerPosition mapPosition) {

      _mapPosition = mapPosition;
   }

   public MapPosition_with_MarkerPosition getMapPosition() {
      return _mapPosition;
   }

}
