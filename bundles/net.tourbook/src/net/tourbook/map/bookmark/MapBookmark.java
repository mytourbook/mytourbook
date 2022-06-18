/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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

import java.util.UUID;

public class MapBookmark {

   public String                           id = UUID.randomUUID().toString();

   public String                           name;

   private double                          _latitude;
   private double                          _longitude;

   private double                          _mapPosition_MarkerLatitude;
   private double                          _mapPosition_MarkerLongitude;

   private MapPosition_with_MarkerPosition _mapPositionProjected;

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (getClass() != obj.getClass()) {
         return false;
      }

      final MapBookmark other = (MapBookmark) obj;

      if (id == null) {
         if (other.id != null) {
            return false;
         }
      } else if (!id.equals(other.id)) {
         return false;
      }

      return true;
   }

   public double getLatitude() {
      return _latitude;
   }

   public double getLongitude() {
      return _longitude;
   }

   public double getMapPosition_MarkerLatitude() {
      return _mapPosition_MarkerLatitude;
   }

   public double getMapPosition_MarkerLongitude() {
      return _mapPosition_MarkerLongitude;
   }

   public MapPosition_with_MarkerPosition getMapPositionProjected() {
      return _mapPositionProjected;
   }

   @Override
   public int hashCode() {

      final int prime = 31;
      int result = 1;

      result = prime * result + ((id == null) ? 0 : id.hashCode());

      return result;
   }

   public void setMapPosition(final MapPosition_with_MarkerPosition mapPosition) {

      _mapPositionProjected = mapPosition;

      _latitude = mapPosition.getLatitude();
      _longitude = mapPosition.getLongitude();

      //this should be in a seperate methode like "setMapMarkerPosition():
      //_mapPositionMarkerLatitude = mapPosition.getMarkerLatitude();
      //_mapPositionMarkerLongitude = mapPosition.getMarkerLongitude();

   }

   public void setMapPositionMarker(final MapPosition_with_MarkerPosition mapPosition) {

      _mapPositionProjected = mapPosition;

      _mapPosition_MarkerLatitude = mapPosition.getMarkerLatitude();
      _mapPosition_MarkerLongitude = mapPosition.getMarkerLongitude();

   }

   @Override
   public String toString() {
      return "\n" //$NON-NLS-1$

            + "MapBookmark " //$NON-NLS-1$

            + "[" //$NON-NLS-1$

            + "id=" + id + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "name=" + name + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "latitude=" + _latitude + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "longitude=" + _longitude + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "marker lat=" + _mapPosition_MarkerLatitude + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "marker long=" + _mapPosition_MarkerLongitude + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "bearing=" + _mapPositionProjected.bearing + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "]"; //$NON-NLS-1$
   }

}
