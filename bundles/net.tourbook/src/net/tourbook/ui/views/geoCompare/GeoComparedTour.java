/*******************************************************************************
 * Copyright (C) 2018, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.geoCompare;

import java.time.ZonedDateTime;
import java.util.Objects;

import net.tourbook.common.UI;
import net.tourbook.data.TourType;

/**
 * Contains data for ONE geo compared tour
 */
public class GeoComparedTour implements Comparable<Object> {

   private static final char NL           = UI.NEW_LINE;

   /**
    * When <code>true</code> then the geo comparison is performed
    */
   public boolean            isGeoCompareDone;

   public long               tourId;

   /**
    * Reference to the root geo compare data
    */
   public GeoCompareData     geoCompareData;

   public float[]            tourLatLonDiff;

   /**
    * Original tour index
    */
   public int                tourFirstIndex;
   public int                tourLastIndex;

   /**
    * <ul>
    * <li>-2 : Value is not yet set</li>
    * <li>-1 : Value is invalid</li>
    * <li>0...max : A Valid value is set</li>
    * </ul>
    */
   long                      minDiffValue = -2;

   public float              avgPulse;
   public float              avgPace;
   public float              avgSpeed;
   public float              avgAltimeter;
   public float              maxPulse;

   public ZonedDateTime      tourStartTime;
   public long               tourStartTimeMS;
   public int                tourYear;

   int                       elapsedTime;
   long                      recordedTime;
   long                      movingTime;

   float                     distance;

   float                     elevationGainAbsolute;
   float                     elevationGainDiff;
   float                     elevationLossAbsolute;
   float                     elevationLossDiff;

   /**
    * Ensure that the title is set for sorting
    */
   public String             tourTitle    = UI.EMPTY_STRING;

   TourType                  tourType;

   /**
    * @param tourId
    * @param geoCompareData
    */
   public GeoComparedTour(final long tourId, final GeoCompareData geoCompareData) {

      this.tourId = tourId;
      this.geoCompareData = geoCompareData;
   }

   @Override
   public int compareTo(final Object o) {

      final long otherValue = ((GeoComparedTour) o).tourStartTimeMS;

      return tourStartTimeMS < otherValue

            ? -1
            : tourStartTimeMS == otherValue

                  ? 0
                  : 1;
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

      final GeoComparedTour other = (GeoComparedTour) obj;

      return tourId == other.tourId;
   }

   @Override
   public int hashCode() {

      return Objects.hash(tourId);
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "GeoComparedTour" + NL //                     //$NON-NLS-1$

            + "[" + NL //                                   //$NON-NLS-1$

            + "tourId         =" + tourId + NL //           //$NON-NLS-1$
            + "geoCompareData =" + geoCompareData + NL //   //$NON-NLS-1$

            + "]" + NL //                                   //$NON-NLS-1$
      ;
   }

}
