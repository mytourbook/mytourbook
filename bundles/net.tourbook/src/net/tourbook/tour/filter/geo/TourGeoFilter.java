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
 * <p>
 * Geo Locations vs. Geo Parts
 * <p>
 * <b>Geo Location</b></br>
 * Geo location is used to show the geo filter area in the map
 * <p>
 * <b>Geo Parts</b></br>
 * Geo parts are used to get tours from the sql database
 * <p>
 */
public class TourGeoFilter {

   String             id = Long.toString(System.nanoTime());

   public GeoPosition geoLocation_TopLeft;
   public GeoPosition geoLocation_BottomRight;
   public GeoPosition geoParts_TopLeft;
   public GeoPosition geoParts_BottomRight;

   public Point       geoLocation_TopLeft_E2;
   public Point       geoLocation_BottomRight_E2;
   public Point       geoParts_TopLeft_E2;
   public Point       geoParts_BottomRight_E2;

   public int         mapZoomLevel;
   public GeoPosition mapGeoCenter;

   ZonedDateTime      created;
   long               createdMS;

   int                geoParts_Width;
   int                geoParts_Height;
   int                numGeoParts;

   public MapGridData mapGridData;

   public TourGeoFilter() {}

   public TourGeoFilter(final int mapZoomLevel,
                        final GeoPosition mapGeoCenter,
                        final MapGridData mapGridData) {
// SET_FORMATTING_OFF

      geoLocation_TopLeft_E2     = mapGridData.geoLocation_TopLeft_E2;
      geoLocation_BottomRight_E2 = mapGridData.geoLocation_BottomRight_E2;

      geoParts_TopLeft_E2        = mapGridData.geoParts_TopLeft_E2;
      geoParts_BottomRight_E2    = mapGridData.geoParts_BottomRight_E2;

      geoLocation_TopLeft        = new GeoPosition(geoLocation_TopLeft_E2.y / 100.0d,        geoLocation_TopLeft_E2.x / 100.0d);
      geoLocation_BottomRight    = new GeoPosition(geoLocation_BottomRight_E2.y / 100.0d,    geoLocation_BottomRight_E2.x / 100.0d);
      geoParts_TopLeft           = new GeoPosition(geoParts_TopLeft_E2.y / 100.0d,           geoParts_TopLeft_E2.x / 100.0d);
      geoParts_BottomRight       = new GeoPosition(geoParts_BottomRight_E2.y / 100.0d,       geoParts_BottomRight_E2.x / 100.0d);

      geoParts_Width = geoParts_BottomRight_E2.x - geoParts_TopLeft_E2.x;
      geoParts_Height = geoParts_TopLeft_E2.y - geoParts_BottomRight_E2.y;

// SET_FORMATTING_ON

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

      return ""

            + "TourGeoFilter\n"

            + "[\n"

            + "geoLocation_TopLeft           =" + geoLocation_TopLeft + "\n"
            + "geoLocation_BottomRight       =" + geoLocation_BottomRight + "\n"
            + "geoParts_TopLeft              =" + geoParts_TopLeft + "\n"
            + "geoParts_BottomRight          =" + geoParts_BottomRight + "\n"

            + "geoLocation_TopLeft_E2        =" + geoLocation_TopLeft_E2 + "\n"
            + "geoLocation_BottomRight_E2    =" + geoLocation_BottomRight_E2 + "\n"
            + "geoParts_TopLeft_E2           =" + geoParts_TopLeft_E2 + "\n"
            + "geoParts_BottomRight_E2       =" + geoParts_BottomRight_E2 + "\n"

            + "geoParts_Width       =" + geoParts_Width + "\n"
            + "geoParts_Height      =" + geoParts_Height + "\n"
            + "numGeoParts          =" + numGeoParts + "\n"

//            + "mapZoomLevel         =" + mapZoomLevel + "\n"
//            + "mapGeoCenter         =" + mapGeoCenter + "\n"
//
//            + "id                   =" + id + "\n"
//            + "created              =" + created + "\n"
//            + "createdMS            =" + createdMS + "\n"

            + "mapGridData          =" + mapGridData + "\n"

            + "]";
   }

}
