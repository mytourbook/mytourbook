/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

public class MapPosition_with_MarkerPosition extends org.oscim.core.MapPosition{
   
   /** Projected position x 0..1 */
   public double mapPositionMarkerX;
   
   public double getMapPositionMarkerX() {
      return mapPositionMarkerX;
   }

   public void setMapPositionMarkerX(double mapPositionMarkerX) {
      this.mapPositionMarkerX = mapPositionMarkerX;
   }


   
   /** Projected position x 0..1 */
   public double mapPositionMarkerY;  
   
   public double getMapPositionMarkerY() {
      return mapPositionMarkerY;
   }  
   
   public void setMapPositionMarkerY(double mapPositionMarkerY) {
      this.mapPositionMarkerY = mapPositionMarkerY;
   }

   
   public double getMarkerLatitude() {
      return MercatorProjection.toLatitude(mapPositionMarkerY);
  }

  public double getMarkerLongitude() {
      return MercatorProjection.toLongitude(mapPositionMarkerX);
  }
   
   
   
   
   public MapPosition_with_MarkerPosition() {
      super();
      this.mapPositionMarkerX = 0.5;
      this.mapPositionMarkerY = 0.5;
   }


   public MapPosition_with_MarkerPosition(double latitude, double longitude, int scale) {
      super(latitude, longitude, scale);
   }

   
   //TODO: this constuctor is not used yet. lat/long must be converted to mercador first, or?
   public MapPosition_with_MarkerPosition(double latitude, double longitude, int scale, double markerLatitude,  double markerLongitude) {
      super(latitude, longitude, scale);
      //this.mapPositionMarkerY = markerLatitude;
      //this.mapPositionMarkerX = markerLongitude;
   } 
   
   public MapPosition_with_MarkerPosition(MapPosition mapPosition) {
      super(mapPosition.getLatitude(), mapPosition.getLongitude(), mapPosition.getScale());
      this.mapPositionMarkerX = mapPosition.x;
      this.mapPositionMarkerY = mapPosition.y;
      System.out.println("constructor: " + mapPosition.getLatitude() + " lon: " + mapPosition.getLongitude());
      System.out.println("constructor: " + mapPositionMarkerY + " lon: " + mapPositionMarkerX);
   }  
   
}
