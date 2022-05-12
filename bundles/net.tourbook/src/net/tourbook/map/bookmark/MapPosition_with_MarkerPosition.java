/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
 * Copyright 2012 Hannes Janetzek
 * Copyright (C) 2019 Thomas Theussing
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

import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;

public class MapPosition_with_MarkerPosition extends org.oscim.core.MapPosition {

   /** Projected position x 0..1 */
   public double mapPositionMarkerX;

   /** Projected position x 0..1 */
   public double mapPositionMarkerY;

   public MapPosition_with_MarkerPosition() {
      
      super();
      
      this.mapPositionMarkerX = 0.5;
      this.mapPositionMarkerY = 0.5;
   }

   public MapPosition_with_MarkerPosition(final double latitude, final double longitude, final int scale) {
      
      super(latitude, longitude, scale);
   }

   //TODO: this constuctor is not used yet. lat/long must be converted to mercador first, or?
   public MapPosition_with_MarkerPosition(final double latitude, final double longitude, final int scale, final double markerLatitude, final double markerLongitude) {
      
      super(latitude, longitude, scale);
      //this.mapPositionMarkerY = markerLatitude;
      //this.mapPositionMarkerX = markerLongitude;
   }

   public MapPosition_with_MarkerPosition(final MapPosition mapPosition) {

      super(mapPosition.getLatitude(), mapPosition.getLongitude(), mapPosition.scale);

      this.bearing = mapPosition.bearing;
      this.roll = mapPosition.roll;
      this.zoomLevel = mapPosition.zoomLevel;
      this.tilt = mapPosition.tilt;
      this.roll = mapPosition.roll;
      this.x = mapPosition.x;
      this.y = mapPosition.y;

      this.mapPositionMarkerX = mapPosition.x;
      this.mapPositionMarkerY = mapPosition.y;
   }

   public double getMapPositionMarkerX() {
      return mapPositionMarkerX;
   }

   public double getMapPositionMarkerY() {
      return mapPositionMarkerY;
   }

   public double getMarkerLatitude() {
      return MercatorProjection.toLatitude(mapPositionMarkerY);
   }

   public double getMarkerLongitude() {
      return MercatorProjection.toLongitude(mapPositionMarkerX);
   }

   public void setMapPositionMarkerX(final double mapPositionMarkerX) {
      this.mapPositionMarkerX = mapPositionMarkerX;
   }

   public void setMapPositionMarkerY(final double mapPositionMarkerY) {
      this.mapPositionMarkerY = mapPositionMarkerY;
   }

}
