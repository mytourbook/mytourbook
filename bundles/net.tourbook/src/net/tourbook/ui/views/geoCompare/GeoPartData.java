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

import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.data.NormalizedGeoData;
import net.tourbook.data.TourReference;

/**
 * Contains all data for a tour geo comparison
 */
public class GeoPartData {

   private static final char         NL            = UI.NEW_LINE;

   /**
    * Entity ID of the {@link TourReference}, is -1 when not available
    */
   public long                       refId         = -1;

   long                              executorId;

   /**
    * When <code>true</code> then the loading/comparing of tours in this loader is canceled.
    */
   boolean                           isCanceled;

   boolean                           isUseAppFilter;

   /**
    * Geo part which should be compared
    */
   public NormalizedGeoData          normalizedTourPart;

   /**
    * Geo parts which are affected
    */
   int[]                             geoParts;

   /**
    * Tour id's which are having at least one of the {@link #geoParts}
    */
   long[]                            tourIds;

   /**
    * Results of the compared tours
    */
   public ArrayList<GeoComparedTour> comparedTours = new ArrayList<>();

   public ArrayList<GeoComparedTour> comparedTours_Filtered;

   /**
    * Time in ms to calculate sql data
    */
   long                              sqlRunningTime;

   /**
    *
    */
   boolean                           isReselectedInUI;

   @SuppressWarnings("unused")
   private GeoPartData() {}

   public GeoPartData(final long executorId,
                      final int[] geoParts,
                      final NormalizedGeoData normalizedTourPart,
                      final boolean useAppFilter) {

      this.executorId = executorId;

      this.geoParts = geoParts;
      this.normalizedTourPart = normalizedTourPart;

      this.isUseAppFilter = useAppFilter;
   }

   @Override
   public String toString() {

//      final int maxLen = 5;

//      final String logGeoPart = geoParts != null ? Arrays.toString(Arrays.copyOf(geoParts, Math.min(geoParts.length, maxLen))) : null;
//      final String logTourIds = tourIds != null ? Arrays.toString(Arrays.copyOf(tourIds, Math.min(tourIds.length, maxLen))) : null;
//      final List<GeoComparedTour> logComparedTours = comparedTours != null ? comparedTours.subList(0, Math.min(comparedTours.size(), maxLen)) : null;

      return UI.EMPTY_STRING

            + "GeoPartData" + NL //                                     //$NON-NLS-1$

            + "[" + NL //                                               //$NON-NLS-1$

            + "refId                = " + refId + NL //                 //$NON-NLS-1$
            + "executorId           = " + executorId + NL //            //$NON-NLS-1$
            + "isUseAppFilter       = " + isUseAppFilter + NL //        //$NON-NLS-1$
            + "isReselectedInUI     = " + isReselectedInUI + NL //      //$NON-NLS-1$
            + "isCanceled           = " + isCanceled + NL //            //$NON-NLS-1$
            + "sqlRunningTime       = " + sqlRunningTime + NL //        //$NON-NLS-1$
            + "normalizedTourPart   = " + normalizedTourPart + NL //    //$NON-NLS-1$

//          + "geoParts             = " + logGeoPart + NL //            //$NON-NLS-1$
//          + "tourIds              = " + logTourIds + NL //            //$NON-NLS-1$
//          + "comparedTours        = " + logComparedTours + NL //      //$NON-NLS-1$

            + "]" + NL //                                               //$NON-NLS-1$

      ;
   }

}
