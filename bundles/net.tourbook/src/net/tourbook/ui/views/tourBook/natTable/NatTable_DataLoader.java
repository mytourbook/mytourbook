/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook.natTable;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;

import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.SQL;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.views.tourBook.LazyTourLoaderItem;
import net.tourbook.ui.views.tourBook.TVITourBookItem;
import net.tourbook.ui.views.tourBook.TVITourBookTour;
import net.tourbook.ui.views.tourBook.TourBookView;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.widgets.Display;

public class NatTable_DataLoader {

   private static final char NL = net.tourbook.common.UI.NEW_LINE;

   // TODO fix fetch size
   private static final int                               FETCH_SIZE           = 10;

   private ConcurrentHashMap<Integer, Integer>            _pageNumbers_Fetched = new ConcurrentHashMap<>();
   private ConcurrentHashMap<Integer, LazyTourLoaderItem> _pageNumbers_Loading = new ConcurrentHashMap<>();

   /**
    * Relation between row position and tour item
    * <p>
    * Key: Row position<br>
    * Value: {@link TVITourBookTour}
    */
   private ConcurrentHashMap<Integer, TVITourBookTour>    _fetchedTourItems    = new ConcurrentHashMap<>();
 
   /**
    * Relation between tour id and row position
    * <p>
    * Key: Tour ID <br>
    * Value: Row position
    */
   private ConcurrentHashMap<Long, Integer>               _fetchedTourIndex    = new ConcurrentHashMap<>();

   private final LinkedBlockingDeque<LazyTourLoaderItem>  _loaderWaitingQueue  = new LinkedBlockingDeque<>();

   private String                                         _sqlSortField;
   private String                                         _sqlSortDirection;

   /**
    * Contains all columns (also hidden columns), sorted in the order how they are displayed in the
    * UI.
    */
   public ArrayList<ColumnDefinition>                     allSortedColumns;

   /**
    * Number of columns which are visible in the natTable
    */
   int                                                    numVisibleColumns;

   /**
    * Number of all tours for the current lazy loader
    */
   private int                                            _numAllTourItems     = -1;

   private TourBookView                                   _tourBookView;
   private ColumnManager                                  _columnManager;

   /**
    * Contains all tour id's for the current tour filter and tour sorting, this is used
    * to get the row index for a tour.
    */
   private long[]                                         _allLoadedTourIds;

   private ExecutorService                                _loadingExecutor;
   private ExecutorService                                _rowIndexExecutor;
   {

      final ThreadFactory threadFactory = new ThreadFactory() {

         @Override
         public Thread newThread(final Runnable r) {

            final Thread thread = new Thread(r, "NatTable_DataLoader: Loading tours/row indices");//$NON-NLS-1$

            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);

            return thread;
         }
      };

      _loadingExecutor = Executors.newSingleThreadExecutor(threadFactory);
      _rowIndexExecutor = Executors.newSingleThreadExecutor(threadFactory);
   }

   public NatTable_DataLoader(final TourBookView tourBookView, final ColumnManager columnManager) {

      _tourBookView = tourBookView;
      _columnManager = columnManager;

      createColumnHeaderData();
   }

   private void createColumnHeaderData() {

      allSortedColumns = _columnManager.getVisibleAndSortedColumns();

      numVisibleColumns = 0;

      for (final ColumnDefinition colDef : allSortedColumns) {

         if (colDef.isColumnDisplayed()) {
            numVisibleColumns++;
         }
      }
   }

   private int[] createRowIndicesFromTourIds(final ArrayList<Long> allRequestedTourIds) {

      final int numRequestedTourIds = allRequestedTourIds.size();
      if (numRequestedTourIds == 0) {

         // nothing more to do

         return new int[0];
      }

      final TIntArrayList allRowIndices = new TIntArrayList();
      final int numAllAvailableTourIds = _allLoadedTourIds.length;

      // loop: all requested tour id's
      for (final Long requestedTourId : allRequestedTourIds) {

         // loop: all available tour id's
         for (int rowPosition = 0; rowPosition < numAllAvailableTourIds; rowPosition++) {

            final long loadedTourId = _allLoadedTourIds[rowPosition];

            if (loadedTourId == requestedTourId) {

               allRowIndices.add(rowPosition);

               // keep relation also for already fetched tour id's
               _fetchedTourIndex.put(requestedTourId, rowPosition);

               break;
            }
         }
      }

      return allRowIndices.toArray();
   }

   private int fetchNumberOfTours() {

      final SQLFilter sqlFilter = new SQLFilter(SQLFilter.TAG_FILTER);

      // get number of tours
      final String sql = NL

            + "SELECT COUNT(*)"
            + " FROM " + TourDatabase.TABLE_TOUR_DATA

            + " WHERE 1=1" + NL //
            + sqlFilter.getWhereClause() + NL;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

//       TourDatabase.enableRuntimeStatistics(conn);

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         // set filter parameters
         sqlFilter.setParameters(prepStmt, 1);

         final ResultSet result = prepStmt.executeQuery();

         // get first result
         result.next();

         // get first value
         return result.getInt(1);

//       TourDatabase.disableRuntimeStatistic(conn);

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      return 0;
   }

   /**
    * @param hoveredRow
    * @return Returns the fetched tour by it's row index or <code>null</code> when tour is not yet
    *         fetched from the backend.
    */
   public TVITourBookTour getFetchedTour(final int hoveredRow) {

      return _fetchedTourItems.get(hoveredRow);
   }

   /**
    * @param tourId
    * @return Returns the tour index but only when it was already fetched (which was done for
    *         displayed tour), otherwise -1.
    */
   public int getFetchedTourIndex(final long tourId) {

      final Integer rowIndex = _fetchedTourIndex.get(tourId);
      if (rowIndex == null) {
         return -1;
      } else {
         return rowIndex;
      }
   }

   /**
    * @return Returns number of all tours for the current lazy loader
    */
   int getNumberOfTours() {

      if (_numAllTourItems == -1) {
         _numAllTourItems = fetchNumberOfTours();
      }

      return _numAllTourItems;
   }

   public CompletableFuture<int[]> getRowIndexFromTourId(final ArrayList<Long> allRequestedTourIds) {

      if (_allLoadedTourIds == null) {

         // firstly load all tour id's

         return CompletableFuture.supplyAsync(() -> {

            loadAllTourIds();

            final int[] rowIndicesFromTourIds = createRowIndicesFromTourIds(allRequestedTourIds);

            return rowIndicesFromTourIds;

         }, _rowIndexExecutor);

      } else {

         return CompletableFuture.supplyAsync(() -> {

            final int[] rowIndices = createRowIndicesFromTourIds(allRequestedTourIds);

            return rowIndices;

         }, _rowIndexExecutor);

      }
   }

   /**
    * @param index
    * @return Returns tour item at the requested row index or <code>null</code> when not yet
    *         available. When tour is not yet loaded then the data will be fetched from the backend.
    */
   TVITourBookTour getTour(final int index) {

      final TVITourBookTour loadedTourItem = _fetchedTourItems.get(index);

      if (loadedTourItem != null) {

         // tour is loaded

         return loadedTourItem;
      }

      /*
       * Check if the tour is currently fetched
       */
      final int fetchKey = index / FETCH_SIZE;

      LazyTourLoaderItem loaderItem = _pageNumbers_Loading.get(fetchKey);

      if (loaderItem != null) {

         // tour is currently being loading -> wait until finished loading

         return null;
      }

      /*
       * Tour is not yet loaded or not yet loading -> load it now
       */

      loaderItem = new LazyTourLoaderItem();

      loaderItem.sqlOffset = fetchKey * FETCH_SIZE;
      loaderItem.fetchKey = fetchKey;

      _pageNumbers_Loading.put(fetchKey, loaderItem);

      _loaderWaitingQueue.add(loaderItem);

      _loadingExecutor.submit(new Runnable() {
         @Override
         public void run() {

            // get last added loader item
            final LazyTourLoaderItem loaderItem = _loaderWaitingQueue.pollFirst();

            if (loaderItem == null) {
               return;
            }

            if (loadPagedTourItems(loaderItem)) {

               // update UI

               final NatTable tourViewer_NatTable = _tourBookView.getTourViewer_NatTable();
               final Display display = tourViewer_NatTable.getDisplay();

               display.asyncExec(() -> {

                  // do a simple redraw, would not work with table/tree widget
                  tourViewer_NatTable.redraw();
               });
            }

            final int loaderItemFetchKey = loaderItem.fetchKey;

            _pageNumbers_Fetched.put(loaderItemFetchKey, loaderItemFetchKey);
            _pageNumbers_Loading.remove(loaderItemFetchKey);
         }
      });

      return null;
   }

   long getTourId(final int rowIndex) {

      if (_allLoadedTourIds == null) {

         return -1;

      } else {

         return _allLoadedTourIds[rowIndex];
      }
   }

   /**
    * Loads all tour id's for the current sort and tour filter
    *
    * @return
    */
   private void loadAllTourIds() {

      final SQLFilter sqlFilter = new SQLFilter(SQLFilter.TAG_FILTER);

      final String sql = NL

            + "SELECT TOURID" //                                                       //$NON-NLS-1$

            + " FROM " + TourDatabase.TABLE_TOUR_DATA + " TourData" + NL //            //$NON-NLS-1$ //$NON-NLS-2$

            + " WHERE 1=1" + NL //
            + sqlFilter.getWhereClause() + NL

            + " ORDER BY " + _sqlSortField + UI.SPACE + _sqlSortDirection + NL //      //$NON-NLS-1$
      ;

      final TLongArrayList allTourIds = new TLongArrayList();

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         // set filter parameters
         sqlFilter.setParameters(prepStmt, 1);

         int rowIndex = 0;

         final ResultSet result = prepStmt.executeQuery();
         while (result.next()) {

            final long tourId = result.getLong(1);

            allTourIds.add(tourId);

            _fetchedTourIndex.put(tourId, rowIndex++);
         }

      } catch (final SQLException e) {

         SQL.showException(e, sql);
      }

      _allLoadedTourIds = allTourIds.toArray();
   }

   private boolean loadPagedTourItems(final LazyTourLoaderItem loaderItem) {

      final SQLFilter sqlFilter = new SQLFilter(SQLFilter.TAG_FILTER);

      final String sql = NL

            + "SELECT " //                                                             //$NON-NLS-1$

            + TVITourBookItem.SQL_ALL_TOUR_FIELDS + NL

            + " FROM " + TourDatabase.TABLE_TOUR_DATA + " TourData" + NL //            //$NON-NLS-1$ //$NON-NLS-2$

            + " WHERE 1=1" + NL //
            + sqlFilter.getWhereClause() + NL

            + " ORDER BY " + _sqlSortField + UI.SPACE + _sqlSortDirection + NL //      //$NON-NLS-1$

            + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY" + NL //                          //$NON-NLS-1$
      ;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         int rowIndex = loaderItem.sqlOffset;

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         // set filter parameters
         sqlFilter.setParameters(prepStmt, 1);

         // set other parameters
         int paramIndex = sqlFilter.getLastParameterIndex();

         prepStmt.setInt(paramIndex++, loaderItem.sqlOffset);
         prepStmt.setInt(paramIndex++, FETCH_SIZE);

         final ResultSet result = prepStmt.executeQuery();
         while (result.next()) {

            final TVITourBookTour tourItem = new TVITourBookTour(null, null);

            tourItem.tourId = result.getLong(1);

            TVITourBookItem.getTourDataFields(result, tourItem);

            final int natTableRowIndex = rowIndex++;

            _fetchedTourItems.put(natTableRowIndex, tourItem);
            _fetchedTourIndex.put(tourItem.tourId, natTableRowIndex);
         }

      } catch (final SQLException e) {

         SQL.showException(e, sql);

         return false;
      }

      return true;
   }

   /**
    * Cleanup all loaded data that the next time they are newly fetched when requested.
    */
   public void resetTourItems() {

      for (final TVITourBookTour tourItem : _fetchedTourItems.values()) {
         tourItem.clearChildren();
      }

      _fetchedTourItems.clear();
      _fetchedTourIndex.clear();

      _pageNumbers_Fetched.clear();
      _pageNumbers_Loading.clear();

      _allLoadedTourIds = null;

      _numAllTourItems = -1;
   }

   public void setSortColumn(final String sortColumnId, final int sortDirection) {

      // cleanup old fetched tours
      resetTourItems();

      /*
       * Set sort order
       */
      if (sortDirection == TourBookView.ItemComparator_Table.ASCENDING) {
         _sqlSortDirection = "ASC"; //$NON-NLS-1$
      } else {
         _sqlSortDirection = "DESC"; //$NON-NLS-1$
      }

      _sqlSortDirection = "ASC";

      /*
       * Set sort direction
       */

// SET_FORMATTING_OFF

      switch (sortColumnId) {

      // tour date
      case TableColumnFactory.TIME_DATE_ID:                 _sqlSortField = "TourStartTime";                         break; //$NON-NLS-1$

      // tour time
      case TableColumnFactory.TIME_TOUR_START_TIME_ID:      _sqlSortField = "TourStartTime";                         break; //$NON-NLS-1$

      case TableColumnFactory.TOUR_TITLE_ID:                _sqlSortField = "TourTitle,         TourStartTime";      break; //$NON-NLS-1$

      case TableColumnFactory.DATA_IMPORT_FILE_NAME_ID:     _sqlSortField = "TourImportFileName";                    break; //$NON-NLS-1$

      case TableColumnFactory.TIME_WEEK_NO_ID:              _sqlSortField = "StartWeek,         TourStartTime";      break; //$NON-NLS-1$
      case TableColumnFactory.TIME_WEEKYEAR_ID:             _sqlSortField = "StartWeekYear,     TourStartTime";      break; //$NON-NLS-1$

// these fields are not yet displayed in tourbook view but are available in tour data indicies
//
//      case TableColumnFactory.:      _sqlSortField = "TourEndTime";          break; //$NON-NLS-1$
//      case TableColumnFactory.:      _sqlSortField = "HasGeoData";           break; //$NON-NLS-1$

      default:
         _sqlSortField = "TourStartTime"; //$NON-NLS-1$
         break;
      }

// SET_FORMATTING_ON

   }
}
