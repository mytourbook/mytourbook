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

import net.tourbook.common.UI;
import net.tourbook.data.TourType;

/**
 * Contains data for one compared tour
 */
public class GeoComparedTour implements Comparable<Object> {

   private static final char NL           = UI.NEW_LINE;

   /**
    * When <code>true</code> then the geo comparison is performed
    */
   public boolean            isGeoCompared;

   public long               tourId;
   public GeoPartData        geoPartData;

   public float[]            tourLatLonDiff;

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
   float                     avgPace;
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
   float                     elevationGain;
   float                     elevationLoss;

   /**
    * Ensure title it is set for sorting
    */
   String                    tourTitle    = UI.EMPTY_STRING;

   TourType                  tourType;

   public GeoComparedTour(final long tourId, final GeoPartData geoPartItem) {

      this.tourId = tourId;
      this.geoPartData = geoPartItem;
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
   public String toString() {

      return UI.EMPTY_STRING

            + "GeoComparedTour" + NL //                  //$NON-NLS-1$

            + "[" + NL //                                //$NON-NLS-1$

            + "tourId      =" + tourId + NL //           //$NON-NLS-1$
            + "geoPartItem =" + geoPartData + NL //      //$NON-NLS-1$

            + "]" + NL //                                //$NON-NLS-1$
      ;
   }

}
