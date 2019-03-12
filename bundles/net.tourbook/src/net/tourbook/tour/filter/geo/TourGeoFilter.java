/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.filter.geo;

import de.byteholder.geoclipse.map.MapGridData;

import java.time.ZonedDateTime;

import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.time.TimeTools;

import org.eclipse.swt.graphics.Point;

/**
 * A tour geo filter is created in the map by selecting an area. This geo filter is displayed (and
 * can be selected) in the tour geo filter slideout.
 */
public class TourGeoFilter {

   String                       id = Long.toString(System.nanoTime());

   public Point                 geo_TopLeft_E2;
   public Point                 geo_BottomRight_E2;

   public int                   mapZoomLevel;
   public GeoPosition           mapGeoCenter;

   public GeoPosition           geo_TopLeft;
   public GeoPosition           geo_BottomRight;

   ZonedDateTime                created;
   long                         createdMS;

   int                          geoParts_Width;
   int                          geoParts_Height;
   int                          numGeoParts;

   public MapGridData mapGridData;

   public TourGeoFilter() {}

   public TourGeoFilter(final Point geoTopLeftE2,
                        final Point geoBottomRightE2,
                        final int mapZoomLevel,
                        final GeoPosition mapGeoCenter,
                        final MapGridData mapGridData) {

      geo_TopLeft_E2 = geoTopLeftE2;
      geo_BottomRight_E2 = geoBottomRightE2;

      geo_TopLeft = new GeoPosition(geoTopLeftE2.y / 100.0d, geoTopLeftE2.x / 100.0d);
      geo_BottomRight = new GeoPosition(geoBottomRightE2.y / 100.0d, geoBottomRightE2.x / 100.0d);

      geoParts_Width = geoBottomRightE2.x - geoTopLeftE2.x;
      geoParts_Height = geoTopLeftE2.y - geoBottomRightE2.y;

      numGeoParts = geoParts_Width * geoParts_Height;

      this.mapZoomLevel = mapZoomLevel;
      this.mapGeoCenter = mapGeoCenter;

      this.mapGridData = mapGridData;

      created = TimeTools.now();
      createdMS = TimeTools.toEpochMilli(created);

   }

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
      final TourGeoFilter other = (TourGeoFilter) obj;
      if (id == null) {
         if (other.id != null) {
            return false;
         }
      } else if (!id.equals(other.id)) {
         return false;
      }
      return true;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      return result;
   }

   @Override
   public String toString() {

      return getClass().getName()

            + "\n"

            + " topLeftE2      = " + geo_TopLeft_E2 + "\n"
            + " bottomRightE2  = " + geo_BottomRight_E2 + "\n"
            + " mapZoomLevel   = " + mapZoomLevel + "\n"

            + " created        = " + created + "\n"
            + " numGeoParts    = " + numGeoParts + "\n"

            + "\n";
   }

}
