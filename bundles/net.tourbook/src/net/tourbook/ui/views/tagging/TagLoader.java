/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tagging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.AppFilter;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;

public class TagLoader {

   protected static final char                         NL                 = net.tourbook.common.UI.NEW_LINE;

   private static ThreadPoolExecutor                   _sqlExecutor;
   private static final LinkedBlockingDeque<SQLLoader> _sqlWaitingQueue   = new LinkedBlockingDeque<>();

   private static int                                  _loadCounter;
   private static AtomicInteger                        _loadValuesCounter = new AtomicInteger();
   private static int                                  _viewUpdateCounter;

   /**
    * This is a concurrent set
    */
   private static Set<TVITaggingView_Item>             _allUpdateItems    = ConcurrentHashMap.newKeySet();

   private static long                                 _lastUpdateTime;

   private static int                                  _minUpdateDiff;

   static {

      final ThreadFactory sqlThread = new ThreadFactory() {

         private int __threadNumber = 0;

         @Override
         public Thread newThread(final Runnable r) {

            final String threadName = "TagSQLLoader-" + __threadNumber++; //$NON-NLS-1$

            final Thread thread = new Thread(r, threadName);

            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);

            return thread;
         }
      };

      final int numProcessors = Runtime.getRuntime().availableProcessors();

      _sqlExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numProcessors, sqlThread);
   }

   public static class SQLLoader {

      private TVITaggingView_Item __taggingItem;
      private TagLoaderID         __tagLoaderID;

      private int                 __loadCounter;

      public SQLLoader(final TVITaggingView_Item taggingItem, final TagLoaderID tagLoaderID, final int loadCounter) {

         __taggingItem = taggingItem;
         __loadCounter = loadCounter;
         __tagLoaderID = tagLoaderID;
      }
   }

   private static void checkLoadingQueue() {

      if (_loadCounter != _viewUpdateCounter) {

         // clear waiting queue

         final BlockingQueue<Runnable> taskQueue = _sqlExecutor.getQueue();

         for (final Runnable runnable : taskQueue) {

            final FutureTask<?> task = (FutureTask<?>) runnable;

            task.cancel(false);
         }

         _sqlWaitingQueue.clear();

         _loadCounter = _viewUpdateCounter;
      }
   }

   public static AtomicInteger getItemUpdateCounter() {
      return _loadValuesCounter;
   }

   /**
    * @return Returns all items which needs to be updated
    */
   private static TVITaggingView_Item[] getUpdateItems() {

      TVITaggingView_Item[] allUpdateItems;

      synchronized (_allUpdateItems) {

         final int numItems = _allUpdateItems.size();

         allUpdateItems = (TVITaggingView_Item[]) _allUpdateItems.toArray(new TVITaggingView_Item[numItems]);

         _allUpdateItems.clear();
      }

      return allUpdateItems;
   }

   /**
    * @param taggingItem
    * @param tagLoaderID
    */
   static void loadValues(final TVITaggingView_Item taggingItem, final TagLoaderID tagLoaderID) {

      _loadValuesCounter.incrementAndGet();

      checkLoadingQueue();

      final SQLLoader sqlLoader = new SQLLoader(taggingItem, tagLoaderID, _viewUpdateCounter);

      _sqlWaitingQueue.add(sqlLoader);

      final Runnable executorTask = new Runnable() {
         @Override
         public void run() {

            // get last added loader item
            final SQLLoader loaderItem = _sqlWaitingQueue.pollFirst();

            if (loaderItem == null) {
               return;
            }

            final int loaderCounter = loaderItem.__loadCounter;

            if (loaderCounter != _viewUpdateCounter) {
               return;
            }

            loadValues_Runnable(loaderItem);
         }
      };

      _sqlExecutor.submit(executorTask);
   }

   private static void loadValues_Runnable(final SQLLoader loaderItem) {

      final TagLoaderID loaderID = loaderItem.__tagLoaderID;
      final TVITaggingView_Item runnableTaggingItem = loaderItem.__taggingItem;

      /*
       * Load values
       */
      if (TagLoaderID.TAG__TOURS.equals(loaderID)) {

         if (runnableTaggingItem instanceof final TVITaggingView_Tag tagItem) {

            loadValues_Runnable_Tag__Tours(tagItem);
         }

      } else if (TagLoaderID.TAG__TOTALS.equals(loaderID)) {

         if (runnableTaggingItem instanceof final TVITaggingView_Tag tagItem) {

            loadValues_Runnable_Tag__Totals(tagItem);
         }

      } else if (TagLoaderID.YEAR__TOURS.equals(loaderID)) {

         if (runnableTaggingItem instanceof final TVITaggingView_Year yearItem) {

            loadValues_Runnable_Year__Tours(yearItem);
         }

      } else if (TagLoaderID.YEAR__MONTHS.equals(loaderID)) {

         if (runnableTaggingItem instanceof final TVITaggingView_Year yearItem) {

            loadValues_Runnable_Year__Months(yearItem);
         }

      } else if (TagLoaderID.MONTH__TOURS.equals(loaderID)) {

         if (runnableTaggingItem instanceof final TVITaggingView_Month monthItem) {

            loadValues_Runnable_Month__Tours(monthItem);
         }
      }

      _loadValuesCounter.decrementAndGet();

      /*
       * Update UI
       */
      final TreeViewer tagViewer = runnableTaggingItem.getTagViewer();

      if (_loadValuesCounter.get() == 0) {

         // this is needed to run the view filter when ALL is loaded

         Display.getDefault().syncExec(() -> {

            if (tagViewer.getTree().isDisposed()) {
               return;
            }

            tagViewer.refresh(false);
         });

      } else {

         final int numUpdateItems = _allUpdateItems.size();

         if (numUpdateItems > 0) {

            Display.getDefault().syncExec(() -> {

               updateUI(tagViewer);
            });
         }
      }
   }

   private static void loadValues_Runnable_Month__Tours(final TVITaggingView_Month monthItem) {

      final ArrayList<TreeViewerItem> allTourItems = new ArrayList<>();

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter appFilter = new AppFilter();

         sql = UI.EMPTY_STRING

               + "--" + NL //                                                                         //$NON-NLS-1$
               + NL
               + "--" + NL //                                                                         //$NON-NLS-1$
               + "-- month - tours (concurrent)" + NL //                                              //$NON-NLS-1$
               + "--" + NL //                                                                         //$NON-NLS-1$
               + NL

               + "SELECT" + NL //                                                                     //$NON-NLS-1$

               + "   tourID," + NL //                                                              1  //$NON-NLS-1$
               + "   jTdataTtag.TourTag_tagId," + NL //                                            2  //$NON-NLS-1$

               + TVITaggingView_Tour.SQL_TOUR_COLUMNS + NL //                                      3

               + "FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " AS jTdataTtag" + NL //       //$NON-NLS-1$ //$NON-NLS-2$

               // get all tours for current tag and year/month
               + "LEFT JOIN " + TourDatabase.TABLE_TOUR_DATA + " AS TourData" //                      //$NON-NLS-1$ //$NON-NLS-2$
               + " ON jTdataTtag.TourData_tourId=TourData.tourId " + NL //                            //$NON-NLS-1$

               // get all equipment ids
               + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS jTdataEq" //       //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.TOURID = jTdataEq.TOURDATA_TOURID" + NL //                            //$NON-NLS-1$

               // get all tag ids for one tour
               + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " AS jTdataTtag_2" //     //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourID = jTdataTtag_2.TourData_tourId" + NL //                        //$NON-NLS-1$

               // get marker ids
               + "LEFT JOIN " + TourDatabase.TABLE_TOUR_MARKER + " AS Tmarker" //                     //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourId = Tmarker.TourData_tourId" + NL //                             //$NON-NLS-1$

               + "WHERE jTdataTtag.TourTag_TagId = ?" + NL //                                         //$NON-NLS-1$
               + "  AND startYear                = ?" + NL //                                         //$NON-NLS-1$
               + "  AND startMonth               = ?" + NL //                                         //$NON-NLS-1$
               + appFilter.getWhereClause() + NL

               + "ORDER BY TourStartTime" + NL //                                                     //$NON-NLS-1$

               + NL;

         final PreparedStatement statement = conn.prepareStatement(sql);

         final TVITaggingView_Year yearItem = monthItem.getYearItem();

         statement.setLong(1, yearItem.getTagId());
         statement.setInt(2, yearItem.getYear());
         statement.setInt(3, monthItem.getMonth());

         appFilter.setParameters(statement, 4);

         long lastTourId = -1;
         TVITaggingView_Tour tourItem = null;

         Set<Long> allTagIDs = null;
         Set<Long> allEquipmentIDs = null;

         final ResultSet result = statement.executeQuery();
         while (result.next()) {

            final long tourId = result.getLong(1);
            final Object dbTagID = result.getObject(11);
            final Object dbEquipmentID = result.getObject(12);

            if (tourId == lastTourId) {

               // get tags from left join
               if (dbTagID instanceof Long) {
                  tourItem.allTagIDs.add((Long) dbTagID);
               }

               // get equipment from left join
               if (dbEquipmentID instanceof final Long equipmentID) {
                  allEquipmentIDs.add(equipmentID);
               }

            } else {

               // new tour is in the resultset
               tourItem = new TVITaggingView_Tour(monthItem, monthItem.getTagViewer());

               allTourItems.add(tourItem);

               tourItem.tourId = tourId;
               tourItem.readTourColumnValues(result, 3);

               tourItem.firstColumn = Integer.toString(tourItem.tourDay);

               // get first tag id
               if (dbTagID instanceof Long) {

                  allTagIDs = new HashSet<>();
                  allTagIDs.add((Long) dbTagID);

                  tourItem.allTagIDs = allTagIDs;
               }

               // get first equipment id
               if (dbEquipmentID instanceof final Long equipmentID) {

                  allEquipmentIDs = new HashSet<>();
                  allEquipmentIDs.add(equipmentID);

                  tourItem.allEquipmentIDs = allEquipmentIDs;
               }

               if (UI.IS_SCRAMBLE_DATA) {
                  tourItem.firstColumn = UI.scrambleText(tourItem.firstColumn);
               }
            }

            lastTourId = tourId;
         }

         final int numTours = allTourItems.size();
         final int numNoTours = numTours == 0 ? 1 : 0;

         monthItem.setChildren(allTourItems);

         monthItem.numTours.addAndGet(numTours);
         monthItem.numNoTours.addAndGet(numNoTours);

         monthItem.updateParent_NumToursAndNoTours(numTours, numNoTours, _allUpdateItems);

         monthItem.updateNumLoadedItems_Decrement();

         _allUpdateItems.add(monthItem);

      } catch (final SQLException e) {

         SQL.showException(e, sql);
      }
   }

   /**
    * Read sum totals from the database for the tagItem
    *
    * @param tagItem
    */
   private static void loadValues_Runnable_Tag__Totals(final TVITaggingView_Tag tagItem) {

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter appFilter = new AppFilter();

         /*
          * Get tags
          */
         sql = UI.EMPTY_STRING

               + "--" + NL //                                                                      //$NON-NLS-1$
               + NL
               + "--" + NL //                                                                      //$NON-NLS-1$
               + "-- tag - totals (concurrent)" + NL //                                            //$NON-NLS-1$
               + "--" + NL //                                                                      //$NON-NLS-1$
               + NL

               + "SELECT " + TVITaggingView_Item.SQL_SUM_COLUMNS + NL //                           //$NON-NLS-1$

               + "FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " AS jtblTagData" + NL //   //$NON-NLS-1$ //$NON-NLS-2$

               // get data for a tour
               + "LEFT JOIN " + TourDatabase.TABLE_TOUR_DATA + " AS TourData" //                   //$NON-NLS-1$ //$NON-NLS-2$
               + " ON jtblTagData.TourData_tourId = TourData.tourId" + NL //                       //$NON-NLS-1$

               + " WHERE jtblTagData.TourTag_TagId = ?" + NL //                                    //$NON-NLS-1$

               + appFilter.getWhereClause();

         final PreparedStatement statement = conn.prepareStatement(sql);

         statement.setLong(1, tagItem.getTagId());
         appFilter.setParameters(statement, 2);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {
            tagItem.readSumColumnData(result, 1);
         }

      } catch (final SQLException e) {

         SQL.showException(e, sql);
      }
   }

   /**
    * Create all tours for the tag item
    *
    * This is the same as {@link TVITaggingView_Tag#loadTag_Tours_Refresh(String)} but without
    * custom sql WHERE
    *
    * @param tagItem
    * @param allUpdatedItems
    */
   private static void loadValues_Runnable_Tag__Tours(final TVITaggingView_Tag tagItem) {

      final ArrayList<TreeViewerItem> allTourItems = new ArrayList<>();

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter appFilter = new AppFilter();

         sql = UI.EMPTY_STRING

               + "--" + NL //                                                                      //$NON-NLS-1$
               + NL
               + "--" + NL //                                                                      //$NON-NLS-1$
               + "-- tag - tours (concurrent)" + NL //                                             //$NON-NLS-1$
               + "--" + NL //                                                                      //$NON-NLS-1$
               + NL

               + "SELECT" + NL //                                                                  //$NON-NLS-1$

               + "   TourData.tourId," + NL //                                                  1  //$NON-NLS-1$
               + "   jTdataTtag.TourTag_tagId," + NL //                                         2  //$NON-NLS-1$

               + TVITaggingView_Tour.SQL_TOUR_COLUMNS + NL //                                   3

               + "FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " AS jTdataTtag" + NL //    //$NON-NLS-1$ //$NON-NLS-2$

               // get all tours for current tag
               + "LEFT JOIN " + TourDatabase.TABLE_TOUR_DATA + " AS TourData" //                   //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON jTdataTtag.TourData_tourId = TourData.tourId " + NL //                      //$NON-NLS-1$

               // get all equipment ids
               + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS jTdataEq" //    //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.TOURID = jTdataEq.TOURDATA_TOURID" + NL //                         //$NON-NLS-1$

               // get all tag ids for one tour
               + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " AS jTdataTtag_2" //  //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourID = jTdataTtag_2.TourData_tourId" + NL //                     //$NON-NLS-1$

               // get marker ids
               + "LEFT JOIN " + TourDatabase.TABLE_TOUR_MARKER + " AS Tmarker" //                  //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourId = Tmarker.TourData_tourId" + NL //                          //$NON-NLS-1$

               + "WHERE jTdataTtag.TourTag_TagId = ?" + NL //                                      //$NON-NLS-1$
               + appFilter.getWhereClause() + NL

               + "ORDER BY TourStartTime" + NL //                                                  //$NON-NLS-1$

               + NL;

         long previousTourId = -1;
         TVITaggingView_Tour tourItem = null;

         final PreparedStatement statement = conn.prepareStatement(sql);
         statement.setLong(1, tagItem.getTagId());
         appFilter.setParameters(statement, 2);

         Set<Long> allTagIDs = null;
         Set<Long> allEquipmentIDs = null;

         final ResultSet result = statement.executeQuery();
         while (result.next()) {

            final long tourId = result.getLong(1);

            final Object dbTagID = result.getObject(11);
            final Object dbEquipmentID = result.getObject(12);

            if (tourId == previousTourId) {

               // get tags from left join
               if (dbTagID instanceof Long) {
                  tourItem.allTagIDs.add((Long) dbTagID);
               }

               // get equipment from left join
               if (dbEquipmentID instanceof final Long equipmentID) {
                  allEquipmentIDs.add(equipmentID);
               }

            } else {

               tourItem = new TVITaggingView_Tour(tagItem, tagItem.getTagViewer());
               allTourItems.add(tourItem);

               tourItem.tourId = tourId;

               tourItem.readTourColumnValues(result, 3);

               tourItem.firstColumn = tourItem.tourDate.format(TimeTools.Formatter_Date_S);

               // get first tag id
               if (dbTagID instanceof Long) {

                  allTagIDs = new HashSet<>();
                  allTagIDs.add((Long) dbTagID);

                  tourItem.allTagIDs = allTagIDs;
               }

               // get first equipment id
               if (dbEquipmentID instanceof final Long equipmentID) {

                  allEquipmentIDs = new HashSet<>();
                  allEquipmentIDs.add(equipmentID);

                  tourItem.allEquipmentIDs = allEquipmentIDs;
               }

               if (UI.IS_SCRAMBLE_DATA) {
                  tourItem.firstColumn = UI.scrambleText(tourItem.firstColumn);
               }
            }

            previousTourId = tourId;
         }

         final int numTours = allTourItems.size();
         final int numNoTours = numTours == 0 ? 1 : 0;

         tagItem.setChildren(allTourItems);

         tagItem.numTours.addAndGet(numTours);
         tagItem.numNoTours.addAndGet(numNoTours);

         tagItem.updateParent_NumToursAndNoTours(numTours, numNoTours, _allUpdateItems);

         tagItem.updateNumLoadedItems_Decrement();

         _allUpdateItems.add(tagItem);

      } catch (final SQLException e) {

         SQL.showException(e, sql);
      }
   }

   private static void loadValues_Runnable_Year__Months(final TVITaggingView_Year yearItem) {

      final ArrayList<TreeViewerItem> allMonthItems = new ArrayList<>();

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter sqlFilter = new AppFilter();

         sql = UI.EMPTY_STRING

               + "--" + NL //                                                                            //$NON-NLS-1$
               + NL
               + "--" + NL //                                                                            //$NON-NLS-1$
               + "-- year - months (concurrent)" + NL //                                                 //$NON-NLS-1$
               + "--" + NL //                                                                            //$NON-NLS-1$
               + NL

               + "SELECT" + NL //               //$NON-NLS-1$
               + " startYear," + NL //       1  //$NON-NLS-1$
               + " startMonth," + NL //      2  //$NON-NLS-1$

               + TVITaggingView_Item.SQL_SUM_COLUMNS

               + " FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag" + NL //   //$NON-NLS-1$ //$NON-NLS-2$

               // get all tours for current tag and year
               + " LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData" + NL //       //$NON-NLS-1$ //$NON-NLS-2$
               + " ON jTdataTtag.TourData_tourId=TourData.tourId " + NL //                      //$NON-NLS-1$

               + " WHERE jTdataTtag.TourTag_TagId = ?" + NL //                                  //$NON-NLS-1$
               + " AND startYear = ?" + NL //                                                   //$NON-NLS-1$
               + sqlFilter.getWhereClause() + NL

               + " GROUP BY startYear, startMonth" + NL //                                      //$NON-NLS-1$
               + " ORDER BY startYear" + NL //                                                  //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);
         statement.setLong(1, yearItem.getTagId());
         statement.setInt(2, yearItem.getYear());
         sqlFilter.setParameters(statement, 3);

         final ResultSet result = statement.executeQuery();
         while (result.next()) {

            final int dbYear = result.getInt(1);
            final int dbMonth = result.getInt(2);

            final TVITaggingView_Month monthItem = new TVITaggingView_Month(yearItem, dbYear, dbMonth, yearItem.getTagViewer());

            monthItem.firstColumn = LocalDate.of(dbYear, dbMonth, 1).format(TimeTools.Formatter_Month);
            monthItem.readSumColumnData(result, 3);

            if (UI.IS_SCRAMBLE_DATA) {
               monthItem.firstColumn = UI.scrambleText(monthItem.firstColumn);
            }

            allMonthItems.add(monthItem);

            // load all tours for a month
            monthItem.numNotLoadedItems.incrementAndGet();
            loadValues(monthItem, TagLoaderID.MONTH__TOURS);
         }

         yearItem.setChildren(allMonthItems);

         yearItem.updateNumLoadedItems_Add(allMonthItems.size());

      } catch (final SQLException e) {

         SQL.showException(e, sql);
      }
   }

   private static void loadValues_Runnable_Year__Tours(final TVITaggingView_Year yearItem) {

      final ArrayList<TreeViewerItem> allTourItems = new ArrayList<>();

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         /*
          * Get all tours for the tag Id of this tree item
          */

         final AppFilter appFilter = new AppFilter();

         sql = UI.EMPTY_STRING

               + "--" + NL //                                                                            //$NON-NLS-1$
               + NL
               + "--" + NL //                                                                            //$NON-NLS-1$
               + "-- year - tours (concurrent)" + NL //                                                  //$NON-NLS-1$
               + "--" + NL //                                                                            //$NON-NLS-1$
               + NL

               + "SELECT" + NL //                                                                        //$NON-NLS-1$

               + "   tourID," + NL //                                                                 1  //$NON-NLS-1$
               + "   jTdataTtag.TourTag_tagId," + NL //                                               2  //$NON-NLS-1$

               + TVITaggingView_Tour.SQL_TOUR_COLUMNS + NL //                                         3

               + "FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " AS jTdataTtag" + NL //          //$NON-NLS-1$ //$NON-NLS-2$

               // get all tours for current tag and year/month
               + "LEFT JOIN " + TourDatabase.TABLE_TOUR_DATA + " AS TourData" //                         //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON jTdataTtag.TourData_tourId=TourData.tourId " + NL //                              //$NON-NLS-1$

               // get all equipment ids
               + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS jTdataEq" //          //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.TOURID = jTdataEq.TOURDATA_TOURID" + NL //                               //$NON-NLS-1$

               // get all tag ids for one tour
               + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " AS jTdataTtag_2" //        //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourID = jTdataTtag_2.TourData_tourId" + NL //                           //$NON-NLS-1$

               // get marker ids
               + "LEFT JOIN " + TourDatabase.TABLE_TOUR_MARKER + " AS Tmarker" //                        //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourId = Tmarker.TourData_tourId" + NL //                                //$NON-NLS-1$

               + "WHERE jTdataTtag.TourTag_TagId = ?" + NL //                                            //$NON-NLS-1$
               + "  AND startYear                = ?" + NL //                                            //$NON-NLS-1$

               + appFilter.getWhereClause() + NL

               + "ORDER BY TourStartTime" + NL //                                                        //$NON-NLS-1$

               + NL;

         final PreparedStatement statement = conn.prepareStatement(sql);

         statement.setLong(1, yearItem.getTagId());
         statement.setInt(2, yearItem.getYear());
         appFilter.setParameters(statement, 3);

         long lastTourId = -1;
         TVITaggingView_Tour tourItem = null;

         Set<Long> allTagIDs = null;
         Set<Long> allEquipmentIDs = null;

         final ResultSet result = statement.executeQuery();
         while (result.next()) {

            final long tourId = result.getLong(1);
            final Object dbTagID = result.getObject(11);
            final Object dbEquipmentID = result.getObject(12);

            if (tourId == lastTourId) {

               // get tags from left join
               if (dbTagID instanceof Long) {
                  tourItem.allTagIDs.add((Long) dbTagID);
               }

               // get equipment from left join
               if (dbEquipmentID instanceof final Long equipmentID) {
                  allEquipmentIDs.add(equipmentID);
               }

            } else {

               // resultset contains a new tour

               tourItem = new TVITaggingView_Tour(yearItem, yearItem.getTagViewer());

               allTourItems.add(tourItem);

               tourItem.tourId = tourId;
               tourItem.readTourColumnValues(result, 3);

               tourItem.firstColumn = tourItem.tourDate.format(TimeTools.Formatter_Date_S);

               // get first tag id
               if (dbTagID instanceof Long) {

                  allTagIDs = new HashSet<>();
                  allTagIDs.add((Long) dbTagID);

                  tourItem.allTagIDs = allTagIDs;
               }

               // get first equipment id
               if (dbEquipmentID instanceof final Long equipmentID) {

                  allEquipmentIDs = new HashSet<>();
                  allEquipmentIDs.add(equipmentID);

                  tourItem.allEquipmentIDs = allEquipmentIDs;
               }

               if (UI.IS_SCRAMBLE_DATA) {
                  tourItem.firstColumn = UI.scrambleText(tourItem.firstColumn);
               }
            }

            lastTourId = tourId;
         }

         final int numTours = allTourItems.size();
         final int numNoTours = numTours == 0 ? 1 : 0;

         yearItem.setChildren(allTourItems);

         yearItem.numTours.addAndGet(numTours);
         yearItem.numNoTours.addAndGet(numNoTours);

         yearItem.updateParent_NumToursAndNoTours(numTours, numNoTours, _allUpdateItems);

         yearItem.updateNumLoadedItems_Decrement();

         _allUpdateItems.add(yearItem);

      } catch (final SQLException e) {

         SQL.showException(e, sql);
      }
   }

   public static void startUpdate() {

      _loadValuesCounter.set(0);
      _viewUpdateCounter++;

      _allUpdateItems.clear();
   }

   private static void updateUI(final TreeViewer tagViewer) {

      _minUpdateDiff = 300;

      final Tree tree = tagViewer.getTree();

      if (tree.isDisposed()) {
         return;
      }

      final int numItems = _allUpdateItems.size();

      if (numItems == 0) {
         return;
      }

      final long now = System.currentTimeMillis();
      final long lastUpdateTimeDiff = now - _lastUpdateTime;

      if (lastUpdateTimeDiff < _minUpdateDiff) {

         tree.getDisplay().timerExec(_minUpdateDiff, () -> {
            updateUI_CheckLatestUpdates(tagViewer);
         });

         return;
      }

      _lastUpdateTime = now;

      final TVITaggingView_Item[] allUpdateItems = getUpdateItems();

      for (final TVITaggingView_Item taggingItem : allUpdateItems) {

         // refresh MUST be called to update the category twisties
         tagViewer.refresh(taggingItem);

         // update() is about 20% faster but do not display the twisties
//       tagViewer.update(taggingItem, null);
      }
   }

   private static void updateUI_CheckLatestUpdates(final TreeViewer tagViewer) {
      
      if (tagViewer.getTree().isDisposed()) {
         return;
      }

      final long timeDiff = System.currentTimeMillis() - _lastUpdateTime;

      if (timeDiff > _minUpdateDiff) {

         final TVITaggingView_Item[] allUpdateItems = getUpdateItems();

         for (final TVITaggingView_Item taggingItem : allUpdateItems) {

            // refresh MUST be called to update the category twisties
            tagViewer.refresh(taggingItem);

            // update() is about 20% faster but do not display the twisties
//          tagViewer.update(taggingItem, null);
         }
      }
   }
}
