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

import java.time.LocalDateTime;

import org.eclipse.swt.graphics.Point;

public class TourGeoFilterItem {

   final Point   topLeftE2;
   final Point   bottomRightE2;

   final int     mapZoomLevel;

   final double  latitude1;
   final double  longitude1;

   final double  latitude2;
   final double  longitude2;

   LocalDateTime created;
   int           numGeoParts;

   public TourGeoFilterItem(final Point topLeftE2, final Point bottomRightE2, final int mapZoomLevel) {

      this.topLeftE2 = topLeftE2;
      this.bottomRightE2 = bottomRightE2;

      this.mapZoomLevel = mapZoomLevel;

      latitude1 = topLeftE2.y / 100.0d;
      longitude1 = topLeftE2.x / 100.0d;

      latitude2 = bottomRightE2.y / 100.0d;
      longitude2 = bottomRightE2.x / 100.0d;

      created = LocalDateTime.now();

      final int width = topLeftE2.x - bottomRightE2.x;
      final int height = topLeftE2.y - bottomRightE2.y;

      numGeoParts = width * height;
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
