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

public class MapPosition_with_MarkerPosition extends org.oscim.core.MapPosition{
   
   /** Projected position x 0..1 */
   public double mapPositionMarkerX;
   
   public double getMapPositionMarkerX() {
      return mapPositionMarkerX;
   }


   public void setMapPositionMarkerX(double mapPositionMarkerX) {
      this.mapPositionMarkerX = mapPositionMarkerX;
   }


   public double getMapPositionMarkerY() {
      return mapPositionMarkerY;
   }


   public void setMapPositionMarkerY(double mapPositionMarkerY) {
      this.mapPositionMarkerY = mapPositionMarkerY;
   }


   /** Projected position x 0..1 */
   public double mapPositionMarkerY;  
   
   
   public MapPosition_with_MarkerPosition() {
      super();
      this.mapPositionMarkerX = 0.5;
      this.mapPositionMarkerY = 0.5;
      // TODO Auto-generated constructor stub
   }


   public MapPosition_with_MarkerPosition(double latitude, double longitude, int scale) {
      super(latitude, longitude, scale);
      // TODO Auto-generated constructor stub
   }

}
