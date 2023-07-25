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
import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.data.NormalizedGeoData;
import net.tourbook.data.TourReference;

/**
 * Contains all data for a tour geo comparison
 */
public class GeoCompareData {

   private static final char    NL                  = UI.NEW_LINE;

   /**
    * Entity ID of the {@link TourReference}, is -1 when not available
    */
   public long                  refTour_RefId       = -1;

   /**
    * Reference tour ID which is compared
    */
   public long                  refTour_TourId      = -1;

   public int                   refTour_FirstIndex;
   public int                   refTour_LastIndex;

   /**
    * Title of the original reference tour
    */
   String                       refTour_OriginalTitle;

   public String                comparedTour_TourTitle;

   long                         executorId;

   /**
    * When <code>true</code> then the loading/comparing of tours in this loader is canceled.
    */
   boolean                      isCanceled;

   boolean                      isUseAppFilter;

   /**
    * Geo part which should be compared
    */
   public NormalizedGeoData     normalizedTourPart;

   /**
    * Geo parts which are affected
    */
   int[]                        geoParts;

   /**
    * Tour id's which are having at least one of the {@link #geoParts}
    */
   long[]                       tourIds;

   /**
    * All geo compared tours
    */
   public List<GeoComparedTour> allGeoComparedTours = new ArrayList<>();

   public List<GeoComparedTour> allGeoComparedTours_Filtered;

   /**
    * Time in ms to calculate sql data
    */
   long                         sqlRunningTime;

   /**
    *
    */
   boolean                      isReselectedInUI;

   @SuppressWarnings("unused")
   private GeoCompareData() {}

   public GeoCompareData(final long executorId,
                         final int[] geoParts,
                         final NormalizedGeoData normalizedTourPart,
                         final boolean useAppFilter,
                         final String comparedTour_TourTitle,
                         final long refTour_TourId) {

      this.executorId = executorId;

      this.geoParts = geoParts;
      this.normalizedTourPart = normalizedTourPart;

      this.isUseAppFilter = useAppFilter;

      this.refTour_TourId = refTour_TourId;
      this.comparedTour_TourTitle = comparedTour_TourTitle;
   }

   @Override
   public String toString() {

//      final int maxLen = 5;

//      final String logGeoPart = geoParts != null ? Arrays.toString(Arrays.copyOf(geoParts, Math.min(geoParts.length, maxLen))) : null;
//      final String logTourIds = tourIds != null ? Arrays.toString(Arrays.copyOf(tourIds, Math.min(tourIds.length, maxLen))) : null;
//      final List<GeoComparedTour> logComparedTours = comparedTours != null ? comparedTours.subList(0, Math.min(comparedTours.size(), maxLen)) : null;

      return UI.EMPTY_STRING

            + "GeoCompareData" + NL //                                  //$NON-NLS-1$

            + "[" + NL //                                               //$NON-NLS-1$

            + "refId                = " + refTour_RefId + NL //                 //$NON-NLS-1$
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
