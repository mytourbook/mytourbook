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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.database.TourDatabase;
import net.tourbook.map2.view.Map2View;
import net.tourbook.ui.SQLFilter;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.graphics.Point;

public class TourGeoFilter_Loader {

   private static final char                                      NL                  = UI.NEW_LINE;

   private final static IDialogSettings                           _state              = TourGeoFilter_Manager.getState();

   private static final AtomicLong                                _loaderExecuterId   = new AtomicLong();
   private static final LinkedBlockingDeque<GeoFilter_LoaderData> _loaderWaitingQueue = new LinkedBlockingDeque<>();
   private static ExecutorService                                 _loadingExecutor;

   static {

      final ThreadFactory threadFactory = new ThreadFactory() {

         @Override
         public Thread newThread(final Runnable r) {

            final Thread thread = new Thread(r, "Loading tours for geo parts");//$NON-NLS-1$

            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);

            return thread;
         }
      };

      _loadingExecutor = Executors.newSingleThreadExecutor(threadFactory);
   }

   /**
    * @param mapGridData
    * @param map2View
    * @param tourGeoFilter
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
   public static GeoFilter_LoaderData loadToursFromGeoParts(final Point geoParts_TopLeft_E2,
                                                            final Point geoParts_BottomRight_E2,
                                                            final GeoFilter_LoaderData previousGeoFilterItem,
                                                            final MapGridData mapGridData,
                                                            final Map2View map2View,
                                                            final TourGeoFilter tourGeoFilter) {

      stopLoading(previousGeoFilterItem);

      // invalidate old requests
      final long executerId = _loaderExecuterId.incrementAndGet();

      final GeoFilter_LoaderData loaderItem = new GeoFilter_LoaderData(executerId);

      loaderItem.geoParts_TopLeft_E2 = geoParts_TopLeft_E2;
      loaderItem.geoParts_BottomRight_E2 = geoParts_BottomRight_E2;

      loaderItem.mapGridData = mapGridData;

      _loaderWaitingQueue.add(loaderItem);

      final Runnable executorTask = new Runnable() {
         @Override
         public void run() {

            try {

               // get last added loader item
               final GeoFilter_LoaderData geoLoaderData = _loaderWaitingQueue.pollFirst();

               if (geoLoaderData == null) {
                  return;
               }

               // show loading state
               final String loadingMessage;
               if (mapGridData == null || mapGridData.numWidth == -1) {
                  loadingMessage = "Loading ...";
               } else {
                  final int numParts = mapGridData.numWidth * mapGridData.numHeight;
                  loadingMessage = MessageFormat.format("Loading {0} parts...", numParts);
               }
               geoLoaderData.mapGridData.gridBox_Text = loadingMessage;
               map2View.redrawMap();

               if (loadToursFromGeoParts_FromDB(geoLoaderData, tourGeoFilter)) {

                  map2View.geoFilter_20_ShowLoadedTours(geoLoaderData, tourGeoFilter);

                  // sometimes the loading state is not updated
                  map2View.redrawMap();

               } else {

                  // update loading state - this should not occure but is helpfull for testing

                  // show loading state
                  geoLoaderData.mapGridData.gridBox_Text = "Loading error: " + TimeTools.Formatter_DateTime_SM.format(LocalDateTime.now());
                  map2View.redrawMap();
               }

            } catch (final Exception e) {
               StatusUtil.log(e);
            }
         }
      };

      _loadingExecutor.submit(executorTask);

      return loaderItem;
   }

   private static boolean loadToursFromGeoParts_FromDB(final GeoFilter_LoaderData geoLoaderData, final TourGeoFilter tourGeoFilter) {

      if (geoLoaderData.isCanceled) {
         return false;
      }

      final long timerStart = System.currentTimeMillis();

      final ArrayList<Integer> allLatLonParts = new ArrayList<>();

      final String selectTourIdsFromGeoParts = TourGeoFilter_Manager.createSelectStmtForGeoParts(
            geoLoaderData.geoParts_TopLeft_E2,
            geoLoaderData.geoParts_BottomRight_E2,
            allLatLonParts);

      if (selectTourIdsFromGeoParts == null) {

         // this can occure when there are no geo parts, this would cause a sql exception

         return false;
      }

      final boolean isUseAppFilter = Util.getStateBoolean(_state,
            TourGeoFilter_Manager.STATE_IS_USE_APP_FILTERS,
            TourGeoFilter_Manager.STATE_IS_USE_APP_FILTERS_DEFAULT);

      final boolean isIncludeGeoParts = Util.getStateBoolean(_state,
            TourGeoFilter_Manager.STATE_IS_INCLUDE_GEO_PARTS,
            TourGeoFilter_Manager.STATE_IS_INCLUDE_GEO_PARTS_DEFAULT);

      String sqlSelect;
      SQLFilter appFilter = null;

      if (isUseAppFilter) {

         // with app filter

         final String sqlIncludeExcludeGeoParts = isIncludeGeoParts ? UI.EMPTY_STRING : "NOT"; //$NON-NLS-1$

         // get app filter without geo location, this is added here
         appFilter = new SQLFilter(SQLFilter.NO_GEO_LOCATION);

         sqlSelect = ""

               + "SELECT" + NL //                                       //$NON-NLS-1$

               + " TourId" + NL //                                      //$NON-NLS-1$
               + " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //        //$NON-NLS-1$

               + " WHERE 1=1 " + appFilter.getWhereClause() + NL//      //$NON-NLS-1$

               + " AND TourId " + sqlIncludeExcludeGeoParts + " IN (" + selectTourIdsFromGeoParts + ")"; //         //$NON-NLS-1$ //$NON-NLS-2$

      } else {

         // no app filter

         if (isIncludeGeoParts) {

            sqlSelect = selectTourIdsFromGeoParts;

         } else {

            // exclude geo parts

            sqlSelect = ""

                  + "SELECT" + NL //                                       //$NON-NLS-1$

                  + " TourId" + NL //                                      //$NON-NLS-1$
                  + " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //        //$NON-NLS-1$

                  + " WHERE TourId NOT IN (" + selectTourIdsFromGeoParts + ")"; //         //$NON-NLS-1$ //$NON-NLS-2$
         }
      }

      final int numGeoParts = allLatLonParts.size();
      Connection conn = null;

      final ArrayList<Long> allTourIds = new ArrayList<>();

      try {

         conn = TourDatabase.getInstance().getConnection();

         final PreparedStatement stmtSelect = conn.prepareStatement(sqlSelect);

         /*
          * Fillup parameters
          */

         int lastAppFilterParamIndex = 1;

         // app filter parameters
         if (isUseAppFilter) {
            appFilter.setParameters(stmtSelect, 1);
            lastAppFilterParamIndex = appFilter.getLastParameterIndex();
         }

         // geo part parameter
         for (int partIndex = 0; partIndex < numGeoParts; partIndex++) {

            final int paramIndex = lastAppFilterParamIndex + partIndex;

            stmtSelect.setInt(paramIndex, allLatLonParts.get(partIndex));
         }

         final ResultSet result = stmtSelect.executeQuery();

         while (result.next()) {

            if (geoLoaderData.isCanceled) {
               return false;
            }

            allTourIds.add(result.getLong(1));
         }

      } catch (final SQLException e) {

         StatusUtil.log(sqlSelect);
         net.tourbook.ui.UI.showSQLException(e);

      } finally {

         Util.closeSql(conn);
      }

      final long timeDiff = System.currentTimeMillis() - timerStart;

      geoLoaderData.sqlRunningTime = timeDiff;

      final String title = tourGeoFilter != null && tourGeoFilter.filterName.length() > 0
            ? tourGeoFilter.filterName
            : "Tours";

      final int numTours = allTourIds.size();
      final String timeInMs = timeDiff > 0 ? " - " + Long.toString(timeDiff) + " ms" : "";

      geoLoaderData.mapGridData.gridBox_Text = String.format("%s: %d %s", title, numTours, timeInMs);

      geoLoaderData.allLoadedTourIds = allTourIds;

      return true;
   }

   /**
    * Stop loading the tours in the waiting queue.
    *
    * @param loaderItem
    */
   public static void stopLoading(final GeoFilter_LoaderData loaderItem) {

      if (loaderItem != null) {
         loaderItem.isCanceled = true;
      }
   }

}
