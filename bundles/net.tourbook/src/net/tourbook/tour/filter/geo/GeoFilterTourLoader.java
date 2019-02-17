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

import de.byteholder.geoclipse.map.MapGridBoxItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.map2.view.Map2View;
import net.tourbook.ui.SQLFilter;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.graphics.Point;

public class GeoFilterTourLoader {

   private static final char                                     NL                  = UI.NEW_LINE;

   private final static IDialogSettings                          _state              = TourGeoFilterManager.getState();

   private static final AtomicLong                               _loaderExecuterId   = new AtomicLong();
   private static final LinkedBlockingDeque<GeoFilterLoaderItem> _loaderWaitingQueue = new LinkedBlockingDeque<>();
   private static ExecutorService                                _loadingExecutor;

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

   /**
    * @param gridBoxItem
    * @param map2View
    * @param geoParts
    *           Requested geo parts
    * @param lonPartNormalized
    * @param latPartNormalized
    * @param useAppFilter
    * @param previousLoaderItem
    * @param geoPartView
    * @return
    * @return
    */
   public static GeoFilterLoaderItem loadToursFromGeoParts(final Point topLeftE2,
                                                           final Point bottomRightE2,
                                                           final GeoFilterLoaderItem previousGeoFilterItem,
                                                           final MapGridBoxItem gridBoxItem,
                                                           final Map2View map2View) {

      stopLoading(previousGeoFilterItem);

      // invalidate old requests
      final long executerId = _loaderExecuterId.incrementAndGet();

      final GeoFilterLoaderItem loaderItem = new GeoFilterLoaderItem(executerId);

      loaderItem.topLeftE2 = topLeftE2;
      loaderItem.bottomRightE2 = bottomRightE2;

      loaderItem.mapGridBoxItem = gridBoxItem;

      _loaderWaitingQueue.add(loaderItem);

      final Runnable executorTask = new Runnable() {
         @Override
         public void run() {

            // get last added loader item
            final GeoFilterLoaderItem loaderItem = _loaderWaitingQueue.pollFirst();

            if (loaderItem == null) {
               return;
            }

            if (loadToursFromGeoParts_FromDB(loaderItem)) {
               map2View.geoFilter_20_Result(loaderItem);
            }
         }
      };

      _loadingExecutor.submit(executorTask);

      return loaderItem;
   }

   private static boolean loadToursFromGeoParts_FromDB(final GeoFilterLoaderItem loaderItem) {

      if (loaderItem.isCanceled) {
         return false;
      }

      final long start = System.currentTimeMillis();

      final ArrayList<Integer> allLatLonParts = new ArrayList<>();

      //         int latPart = (int) (latitude * 100);
      //         int lonPart = (int) (longitude * 100);
      //
      //         lat      ( -90 ... + 90) * 100 =  -9_000 +  9_000 = 18_000
      //         lon      (-180 ... +180) * 100 = -18_000 + 18_000 = 36_000
      //
      //         max      (9_000 + 9_000) * 100_000 = 18_000 * 100_000  = 1_800_000_000
      //
      //                                    Integer.MAX_VALUE = 2_147_483_647

      // x: longitude
      // y: latitude

      final int normalizedLat1 = loaderItem.topLeftE2.y + TourData.NORMALIZED_LATITUDE_OFFSET_E2;
      final int normalizedLat2 = loaderItem.bottomRightE2.y + TourData.NORMALIZED_LATITUDE_OFFSET_E2;

      final int normalizedLon1 = loaderItem.topLeftE2.x + TourData.NORMALIZED_LONGITUDE_OFFSET_E2;
      final int normalizedLon2 = loaderItem.bottomRightE2.x + TourData.NORMALIZED_LONGITUDE_OFFSET_E2;

      final double gridSize_E2 = 1; // 0.01°

      for (int normalizedLon = normalizedLon1; normalizedLon < normalizedLon2; normalizedLon += gridSize_E2) {

         for (int normalizedLat = normalizedLat2; normalizedLat < normalizedLat1; normalizedLat += gridSize_E2) {

            final int latitudeE2 = normalizedLat - TourData.NORMALIZED_LATITUDE_OFFSET_E2;
            final int longitudeE2 = normalizedLon - TourData.NORMALIZED_LONGITUDE_OFFSET_E2;

            final int latLonPart = (latitudeE2 + 9_000) * 100_000 + (longitudeE2 + 18_000);

            allLatLonParts.add(latLonPart);

//            System.out.println(String.format("lon(x) %d  lat(y) %d  %s",
//
//                  longitudeE2,
//                  latitudeE2,
//
//                  Integer.toString(latLonPart)
//
//            ));
//// TODO remove SYSTEM.OUT.PRINTLN
         }
      }

      final boolean isAppFilter = Util.getStateBoolean(_state,
            TourGeoFilterManager.STATE_IS_USE_APP_FILTERS,
            TourGeoFilterManager.STATE_IS_USE_APP_FILTERS_DEFAULT);

      /*
       * Create geo part sql parameters
       */
      final int numGeoParts = allLatLonParts.size();
      final StringBuilder sqlGeoPartParameters = new StringBuilder();
      for (int partIndex = 0; partIndex < numGeoParts; partIndex++) {
         if (partIndex == 0) {
            sqlGeoPartParameters.append(" ?"); //                             //$NON-NLS-1$
         } else {
            sqlGeoPartParameters.append(", ?"); //                            //$NON-NLS-1$
         }
      }

      final String selectGeoPartTourIds = ""

            + "SELECT" + NL //                                                //$NON-NLS-1$

            + " DISTINCT TourId " + NL //                                     //$NON-NLS-1$

            + (" FROM " + TourDatabase.TABLE_TOUR_GEO_PARTS + NL) //          //$NON-NLS-1$
            + (" WHERE GeoPart IN (" + sqlGeoPartParameters + ")") + NL //    //$NON-NLS-1$ //$NON-NLS-2$
      ;

      String select;
      SQLFilter appFilter = null;

      if (isAppFilter) {

         // get app filter without geo location, this is added here
         appFilter = new SQLFilter(SQLFilter.NO_GEO_LOCATION);

         final String selectAppFilter = ""

               + "SELECT" + NL //                                       //$NON-NLS-1$

               + " TourId" + NL //                                      //$NON-NLS-1$
               + " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //        //$NON-NLS-1$

               + " WHERE 1=1 " + appFilter.getWhereClause() + NL//      //$NON-NLS-1$
         ;

         select = selectGeoPartTourIds

               + " AND TourId IN (" + selectAppFilter + ")"; //         //$NON-NLS-1$ //$NON-NLS-2$
      } else {

         select = selectGeoPartTourIds;
      }

      Connection conn = null;

      final ArrayList<Long> allTourIds = new ArrayList<>();

      try {

         conn = TourDatabase.getInstance().getConnection();

         final PreparedStatement stmtSelect = conn.prepareStatement(select);

            // fillup parameters
            for (int partIndex = 0; partIndex < numGeoParts; partIndex++) {
               stmtSelect.setInt(partIndex + 1, allLatLonParts.get(partIndex));
            }

         if (isAppFilter) {
            appFilter.setParameters(stmtSelect, 1 + numGeoParts);
         }

         final ResultSet result = stmtSelect.executeQuery();

         while (result.next()) {

            if (loaderItem.isCanceled) {
               return false;
            }

            allTourIds.add(result.getLong(1));
         }

      } catch (final SQLException e) {

         StatusUtil.log(select);
         net.tourbook.ui.UI.showSQLException(e);

      } finally {

         Util.closeSql(conn);
      }

      final long timeDiff = System.currentTimeMillis() - start;
      loaderItem.sqlRunningTime = timeDiff;

      final String timeInMs = timeDiff > 50 ? " - " + Long.toString(timeDiff) + " ms" : "";

      loaderItem.mapGridBoxItem.gridBoxText = "Tours: " + Integer.toString(allTourIds.size()) + timeInMs;

      loaderItem.allLoadedTourIds = allTourIds;

//      System.out.println(""
////            (UI.timeStampNano() + " [" + GeoFilterTourLoader.class.getSimpleName() + "] ")
//            + "load\t" + timeDiff + " ms");
//// TODO remove SYSTEM.OUT.PRINTLN

      return true;
   }

   /**
    * Stop loading the tours in the waiting queue.
    *
    * @param loaderItem
    */
   public static void stopLoading(final GeoFilterLoaderItem loaderItem) {

      if (loaderItem != null) {
         loaderItem.isCanceled = true;
      }
   }

}
