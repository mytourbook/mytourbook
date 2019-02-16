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

import de.byteholder.geoclipse.map.GridBoxItem;

import java.time.ZonedDateTime;

import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.time.TimeTools;

import org.eclipse.swt.graphics.Point;

public class TourGeoFilterItem {

   String             id = Long.toString(System.nanoTime());

   Point              topLeftE2;
   Point              bottomRightE2;

   public int         mapZoomLevel;
   public GeoPosition mapGeoCenter;

   public double      latitude1;
   public double      longitude1;

   public double      latitude2;
   public double      longitude2;

   ZonedDateTime      created;
   long               createdMS;

   int                numGeoParts;

   GridBoxItem        gridBoxItem;

   public TourGeoFilterItem() {}

   public TourGeoFilterItem(final Point topLeftE2,
                            final Point bottomRightE2,
                            final int mapZoomLevel,
                            final GeoPosition mapGeoCenter,
                            final GridBoxItem gridBoxItem) {

      this.topLeftE2 = topLeftE2;
      this.bottomRightE2 = bottomRightE2;

      this.mapZoomLevel = mapZoomLevel;
      this.mapGeoCenter = mapGeoCenter;

      this.gridBoxItem = gridBoxItem;

      latitude1 = topLeftE2.y / 100.0d;
      longitude1 = topLeftE2.x / 100.0d;

      latitude2 = bottomRightE2.y / 100.0d;
      longitude2 = bottomRightE2.x / 100.0d;

      created = TimeTools.now();
      createdMS = TimeTools.toEpochMilli(created);

      final int width = bottomRightE2.x - topLeftE2.x;
      final int height = topLeftE2.y - bottomRightE2.y;

      numGeoParts = width * height;
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
      final TourGeoFilterItem other = (TourGeoFilterItem) obj;
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

            + " topLeftE2      = " + topLeftE2 + "\n"
            + " bottomRightE2  = " + bottomRightE2 + "\n"
            + " mapZoomLevel   = " + mapZoomLevel + "\n"

            + " latitude1      = " + latitude1 + "\n"
            + " longitude1     = " + longitude1 + "\n"
            + " latitude2      = " + latitude2 + "\n"
            + " longitude2     = " + longitude2 + "\n"

            + " created        = " + created + "\n"
            + " numGeoParts    = " + numGeoParts + "\n"

            + "\n";
   }

}
