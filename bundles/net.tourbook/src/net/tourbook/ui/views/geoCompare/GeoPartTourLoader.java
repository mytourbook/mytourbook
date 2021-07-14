/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import gnu.trove.list.array.TLongArrayList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.NormalizedGeoData;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

public class GeoPartTourLoader {

   private static final char                             NL                  = UI.NEW_LINE;

   private static final AtomicLong                       _loaderExecuterId   = new AtomicLong();
   private static final LinkedBlockingDeque<GeoPartItem> _loaderWaitingQueue = new LinkedBlockingDeque<>();
   private static ExecutorService                        _loadingExecutor;

   static {

      final ThreadFactory threadFactory = new ThreadFactory() {

         @Override
         public Thread newThread(final Runnable r) {

            final Thread thread = new Thread(r, "Loading geo part tours");//$NON-NLS-1$

            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);

            return thread;
         }
      };

      _loadingExecutor = Executors.newSingleThreadExecutor(threadFactory);
   }

   private static boolean loadTourGeoPartsFromDB(final GeoPartItem loaderItem) {

      if (loaderItem.isCanceled) {
         return false;
      }

      final long start = System.currentTimeMillis();

      final int[] requestedGeoParts = loaderItem.geoParts;
      final int numGeoParts = requestedGeoParts.length;

      if (numGeoParts == 0) {

         // there are no geoparts, set empty list to have valid data

         loaderItem.tourIds = new long[] {};

         return true;
      }

      final boolean isAppFilter = loaderItem.isUseAppFilter;

      /*
       * Create sql parameters
       */
      final StringBuilder sqlInParameters = new StringBuilder();
      for (int partIndex = 0; partIndex < numGeoParts; partIndex++) {
         if (partIndex == 0) {
            sqlInParameters.append(" ?"); //$NON-NLS-1$
         } else {
            sqlInParameters.append(", ?"); //$NON-NLS-1$
         }
      }

      String select = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

// this is very slow
//       final SQLFilter appFilter = new SQLFilter(SQLFilter.TAG_FILTER);
         final SQLFilter appFilter = new SQLFilter();

         final String selectGeoPart = UI.EMPTY_STRING

               + "SELECT" + NL //                                          //$NON-NLS-1$

               + " DISTINCT TourId " + NL //                               //$NON-NLS-1$

               + " FROM " + TourDatabase.TABLE_TOUR_GEO_PARTS + NL //      //$NON-NLS-1$
               + " WHERE GeoPart IN (" + sqlInParameters + ")" + NL //     //$NON-NLS-1$ //$NON-NLS-2$
         ;

         if (isAppFilter) {

            final String selectAppFilter = UI.EMPTY_STRING

                  + "SELECT" + NL //                                       //$NON-NLS-1$

                  + " TourId" + NL //                                      //$NON-NLS-1$
                  + " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //        //$NON-NLS-1$

// this is very slow
//                // get tag id's
//                + (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag") + NL //$NON-NLS-1$ //$NON-NLS-2$
//                + (" ON tourID = jTdataTtag.TourData_tourId") + NL //$NON-NLS-1$

                  + " WHERE 1=1 " + appFilter.getWhereClause() + NL //     //$NON-NLS-1$
            ;

            select = selectGeoPart

                  + " AND TourId IN (" + selectAppFilter + ")"; //         //$NON-NLS-1$ //$NON-NLS-2$

         } else {

            select = selectGeoPart;
         }

         /*
          * Fill parameters
          */
         final PreparedStatement statement = conn.prepareStatement(select);

         for (int partIndex = 0; partIndex < numGeoParts; partIndex++) {
            statement.setInt(partIndex + 1, requestedGeoParts[partIndex]);
         }

         if (isAppFilter) {
            appFilter.setParameters(statement, 1 + numGeoParts);
         }

         /*
          * Get tour id's
          */
         final ResultSet result = statement.executeQuery();

         final TLongArrayList tourIds = new TLongArrayList();

         while (result.next()) {
            tourIds.add(result.getLong(1));
         }

         loaderItem.tourIds = tourIds.toArray();

      } catch (final SQLException e) {

         StatusUtil.log(select);
         net.tourbook.ui.UI.showSQLException(e);

      }

      final long timeDiff = System.currentTimeMillis() - start;

      loaderItem.sqlRunningTime = timeDiff;

//      System.out.println(
//            (UI.timeStampNano() + " [" + GeoPart_TourLoader.class.getSimpleName() + "] ")
//                  + "loadTourGeoPartsFromDB\t" + timeDiff + " ms");
//// TODO remove SYSTEM.OUT.PRINTLN

      if (loaderItem.isCanceled) {
         return false;
      }

      return true;
   }

   /**
    * @param geoParts
    *           Requested geo parts
    * @param lonPartNormalized
    * @param latPartNormalized
    * @param useAppFilter
    * @param previousLoaderItem
    * @param geoPartView
    * @return
    */
   static GeoPartItem loadToursFromGeoParts(final int[] geoParts,
                                            final NormalizedGeoData normalizedTourPart,
                                            final boolean useAppFilter,
                                            final GeoPartItem previousLoaderItem,
                                            final GeoCompareView geoPartView) {

      stopLoading(previousLoaderItem);

      // invalidate old requests
      final long executerId = _loaderExecuterId.incrementAndGet();

//      System.out.println(
//            ("[" + GeoPartTourLoader.class.getSimpleName() + "] loadToursFromGeoParts()")
//                 + ("\texecuterId: " + executerId));
//// TODO remove SYSTEM.OUT.PRINTLN

      final GeoPartItem loaderItem = new GeoPartItem(
            executerId,
            geoParts,
            normalizedTourPart,
            useAppFilter);

      _loaderWaitingQueue.add(loaderItem);

      final Runnable executorTask = new Runnable() {
         @Override
         public void run() {

            // get last added loader item
            final GeoPartItem loaderItem = _loaderWaitingQueue.pollFirst();

            if (loaderItem == null) {
               return;
            }

            if (loadTourGeoPartsFromDB(loaderItem)) {
               geoPartView.compare_40_CompareTours(loaderItem);
            }
         }
      };

      _loadingExecutor.submit(executorTask);

      return loaderItem;
   }

   /**
    * Stop loading and comparing of the tours in the waiting queue.
    *
    * @param loaderItem
    */
   static void stopLoading(final GeoPartItem loaderItem) {

      if (loaderItem != null) {
         loaderItem.isCanceled = true;
      }
   }

}
