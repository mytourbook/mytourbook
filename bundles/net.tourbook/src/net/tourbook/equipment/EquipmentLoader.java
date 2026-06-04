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
package net.tourbook.equipment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.SQLData;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.AppFilter;

import org.eclipse.swt.widgets.Display;

public class EquipmentLoader {

   protected static final char                                  NL                    = net.tourbook.common.UI.NEW_LINE;

   private static ThreadPoolExecutor                            _sqlEquipExecutor;
   private static ThreadPoolExecutor                            _sqlPartExecutor;

   private static final LinkedBlockingDeque<SQLEquipmentLoader> _sqlEquipWaitingQueue = new LinkedBlockingDeque<>();
   private static final LinkedBlockingDeque<SQLPartLoader>      _sqlPartWaitingQueue  = new LinkedBlockingDeque<>();

   private static int                                           _equipLoadCounter;
   private static int                                           _partLoadCounter;

   /**
    * This counter is set (incremented) when a new update starts
    */
   private static int                                           _updateCounter;

   static {

      final ThreadFactory sqlThreadEquip = new ThreadFactory() {

         private int __threadNumber = 0;

         @Override
         public Thread newThread(final Runnable r) {

            final String threadName = "EquipmentSQLLoader-Equipment" + __threadNumber++; //$NON-NLS-1$

            final Thread thread = new Thread(r, threadName);

            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);

            return thread;
         }
      };

      final ThreadFactory sqlThreadPart = new ThreadFactory() {

         private int __threadNumber = 0;

         @Override
         public Thread newThread(final Runnable r) {

            final String threadName = "EquipmentSQLLoader-Part-" + __threadNumber++; //$NON-NLS-1$

            final Thread thread = new Thread(r, threadName);

            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);

            return thread;
         }
      };

      final int numProcessors = Runtime.getRuntime().availableProcessors();

      _sqlEquipExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numProcessors, sqlThreadEquip);
      _sqlPartExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numProcessors, sqlThreadPart);
   }

   public static class SQLEquipmentLoader {

      private TVIEquipmentView_Equipment __equipmentItem;
      private int                        __loadCounter;

      public SQLEquipmentLoader(final TVIEquipmentView_Equipment equipmentItem, final int loadCounter) {

         __equipmentItem = equipmentItem;
         __loadCounter = loadCounter;
      }
   }

   public static class SQLPartLoader {

      private TVIEquipmentView_Part __partItem;
      private int                   __loadCounter;

      public SQLPartLoader(final TVIEquipmentView_Part partItem, final int loadCounter) {

         __partItem = partItem;
         __loadCounter = loadCounter;
      }
   }

   /**
    * @param rootItem
    *           This is the parent item for all children
    * @param allEquipmentItems
    *           All children items
    */
   static void loadSummarizedValues_Equipment(final TVIEquipmentView_Item rootItem,
                                              final Map<Long, TVIEquipmentView_Equipment> allEquipmentItems) {

      loadSummarizedValues_Equipment_AllTours(rootItem, allEquipmentItems);
      loadSummarizedValues_Equipment_CollateTours(rootItem, allEquipmentItems);
      loadSummarizedValues_Equipment_WithParts(rootItem, allEquipmentItems);
   }

   /**
    * Get number of all tours for each equipment
    *
    * @param rootItem
    * @param allEquipmentItems
    */
   private static void loadSummarizedValues_Equipment_AllTours(final TVIEquipmentView_Item rootItem,
                                                               final Map<Long, TVIEquipmentView_Equipment> allEquipmentItems) {

      String sql = null;
      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter appFilter = rootItem.createAppFilter();

         sql = UI.EMPTY_STRING

               + "--" + NL //                                                                   //$NON-NLS-1$
               + NL
               + "----------------------------" + NL //                                         //$NON-NLS-1$
               + "-- equipment sum - all tours" + NL //                                         //$NON-NLS-1$
               + "----------------------------" + NL //                                         //$NON-NLS-1$
               + NL

               + "SELECT" + NL //                                                               //$NON-NLS-1$

               + "   j_Td_Eq.EQUIPMENT_EQUIPMENTID," + NL //                                 1  //$NON-NLS-1$
               + "   COUNT(*) AS numTours" + NL //                                           2  //$NON-NLS-1$

               + "FROM " + TourDatabase.TABLE_EQUIPMENT + NL //                                 //$NON-NLS-1$

               + "JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS j_Td_Eq" //       //$NON-NLS-1$ //$NON-NLS-2$
               + "   ON j_Td_Eq.EQUIPMENT_EQUIPMENTID = EQUIPMENT.EQUIPMENTID" + NL //          //$NON-NLS-1$

               + "JOIN " + TourDatabase.TABLE_TOUR_DATA + " AS TourData" //                     //$NON-NLS-1$ //$NON-NLS-2$
               + "   ON TourData.tourid = j_Td_Eq.tourdata_tourid" + NL //                      //$NON-NLS-1$

               + appFilter.getWhereClause()

               + "GROUP BY " + NL //                                                            //$NON-NLS-1$
               + "   j_Td_Eq.EQUIPMENT_EQUIPMENTID" + NL //                                     //$NON-NLS-1$

               + NL;

         statement = conn.prepareStatement(sql);

         int nextIndex = 1;

         nextIndex = appFilter.setParameters(statement, nextIndex);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final long equipmentID = result.getLong(1);
            final int numTours = result.getInt(2);

            final TVIEquipmentView_Equipment equipmentItem = allEquipmentItems.get(equipmentID);

            if (equipmentItem != null) {

               equipmentItem.numTours_All = numTours;
            }
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      } finally {
         Util.closeSql(statement);
      }
   }

   /**
    * Collate tours for equipment which has no parts/services
    *
    * @param rootItem
    * @param allEquipmentItems
    */
   private static void loadSummarizedValues_Equipment_CollateTours(final TVIEquipmentView_Item rootItem,
                                                                   final Map<Long, TVIEquipmentView_Equipment> allEquipmentItems) {

      // clone map
      final Map<Long, TVIEquipmentView_Equipment> allEquipmentItemsWithoutTours = new HashMap<>(allEquipmentItems);

      String sql = null;
      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter appFilter = rootItem.createAppFilter();

         sql = UI.EMPTY_STRING

               + "--" + NL //                                                                      //$NON-NLS-1$
               + NL
               + "---------------------------------" + NL //                                       //$NON-NLS-1$
               + "-- equipment sum - collated tours" + NL //                                       //$NON-NLS-1$
               + "---------------------------------" + NL //                                       //$NON-NLS-1$
               + NL

               + "SELECT" + NL //                                                                  //$NON-NLS-1$
               + "   equip.EQUIPMENTID," + NL //                                                1  //$NON-NLS-1$
               + "   COUNT(*) AS num_Tours," + NL //                                            2  //$NON-NLS-1$

               + TVIEquipmentView_Item.getSQL_SUM_COLUMNS("TourData", 3) //                     3  //$NON-NLS-1$

               + "FROM " + TourDatabase.TABLE_EQUIPMENT + " AS equip" + NL //                      //$NON-NLS-1$ //$NON-NLS-2$

               + "JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS j_Td_Eq" //          //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON j_Td_Eq.equipment_equipmentid = equip.EQUIPMENTID" + NL //                  //$NON-NLS-1$

               // the alias "TourData" is needed that the app filter is working
               + "JOIN " + TourDatabase.TABLE_TOUR_DATA + " AS TourData" //                        //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourid = j_Td_Eq.tourdata_tourid" + NL //                          //$NON-NLS-1$
               + "  AND TourData.tourstarttime >= equip.dateCollateFrom" + NL //                   //$NON-NLS-1$
               + "  AND TourData.tourstarttime <  equip.dateCollateUntil" + NL //                  //$NON-NLS-1$

               + appFilter.getWhereClause()

               + "WHERE equip.isCollate = true" + NL //                                            //$NON-NLS-1$

               + "GROUP BY equip.EQUIPMENTID" + NL //                                              //$NON-NLS-1$

               + NL;

         statement = conn.prepareStatement(sql);

         int nextIndex = 1;

         nextIndex = appFilter.setParameters(statement, nextIndex);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final long equipmentID = result.getLong(1);
            final int numTours = result.getInt(2);

            final TVIEquipmentView_Equipment equipmentItem = allEquipmentItems.get(equipmentID);

            if (equipmentItem != null) {

               equipmentItem.numTours_IsCollated = numTours;

               equipmentItem.readCommonValues(result, 3);

               // this equipment has a tour -> remove from list
               allEquipmentItemsWithoutTours.remove(equipmentID);
            }
         }

         for (final TVIEquipmentView_Equipment equipmentItem : allEquipmentItemsWithoutTours.values()) {

            if (equipmentItem.getEquipment().isCollate()) {

               // set 0 children that the expand icon in the view is not displayed
               equipmentItem.setChildren(new ArrayList<>());
            }
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      } finally {
         Util.closeSql(statement);
      }
   }

   /**
    * Collate tours for equipment which has parts/services
    *
    * @param rootItem
    * @param allEquipmentItems
    */
   private static void loadSummarizedValues_Equipment_WithParts(final TVIEquipmentView_Item rootItem,
                                                                final Map<Long, TVIEquipmentView_Equipment> allEquipmentItems) {

      for (final TVIEquipmentView_Equipment eqItem : allEquipmentItems.values()) {

         final Equipment equipment = eqItem.getEquipment();

         if (equipment.isCollate() == false) {

            // equipment has parts/services

            loadSummarizedValues_Equipment_WithParts_Equipment(eqItem);
         }
      }
   }

   private static void loadSummarizedValues_Equipment_WithParts_Equipment(final TVIEquipmentView_Equipment eqItem) {

      if (_equipLoadCounter != _updateCounter) {

         // clear waiting queue

         final BlockingQueue<Runnable> taskQueue = _sqlEquipExecutor.getQueue();

         for (final Runnable runnable : taskQueue) {

            final FutureTask<?> task = (FutureTask<?>) runnable;

            task.cancel(false);
         }

         _sqlEquipWaitingQueue.clear();

         _equipLoadCounter = _updateCounter;
      }

      final SQLEquipmentLoader sqlLoader = new SQLEquipmentLoader(eqItem, _updateCounter);

      _sqlEquipWaitingQueue.add(sqlLoader);

      final Runnable executorTask = new Runnable() {
         @Override
         public void run() {

            // get last added loader item
            final SQLEquipmentLoader loaderItem = _sqlEquipWaitingQueue.pollFirst();

            if (loaderItem == null) {
               return;
            }

            final int loaderCounter = loaderItem.__loadCounter;

            if (loaderCounter != _updateCounter) {
               return;
            }

            final TVIEquipmentView_Equipment loaderEqItem = loaderItem.__equipmentItem;

            loadSummarizedValues_Equipment_WithParts_Equipment_Runnable(loaderEqItem);

            if (loaderCounter != _updateCounter) {
               return;
            }

            Display.getDefault().asyncExec(() -> {

               eqItem.getEquipmentViewer().update(loaderEqItem, null);
            });
         }
      };

      _sqlEquipExecutor.submit(executorTask);
   }

   private static void loadSummarizedValues_Equipment_WithParts_Equipment_Runnable(final TVIEquipmentView_Equipment eqItem) {

      final long equipmentID = eqItem.getEquipmentID();

      final EquipmentViewerType viewerType = eqItem.getViewerType();

      String sql = null;
      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter appFilter = eqItem.createAppFilter();
         final SQLData partFilter_AND_OR = new EquipmentPartFilter_AND_OR(viewerType, equipmentID).getSqlData();
         final SQLData partFilter_NOT = new EquipmentPartFilter_NOT(viewerType).getSqlData();

         sql = UI.EMPTY_STRING

               + "--" + NL //                                                                      //$NON-NLS-1$
               + NL
               + "-----------------------------" + NL //                                           //$NON-NLS-1$
               + "-- equipment sum - from parts" + NL //                                           //$NON-NLS-1$
               + "-----------------------------" + NL //                                           //$NON-NLS-1$
               + NL

               + "SELECT" + NL //                                                                  //$NON-NLS-1$

               + "   COUNT(*) AS Num_Tours," + NL //                                               //$NON-NLS-1$

               + TVIEquipmentView_Item.getSQL_SUM_COLUMNS("tdFields", 3) //                                              //$NON-NLS-1$

               + "FROM" + NL //                                                                    //$NON-NLS-1$
               + "(" + NL //                                                                       //$NON-NLS-1$

               + "   SELECT" + NL //                                                               //$NON-NLS-1$

               // the part filter can create duplicated tour ids
               + "      DISTINCT TourData.TourID," + NL //                                         //$NON-NLS-1$

               + TVIEquipmentView_Item.getSQL_SUM_TOUR_COLUMNS("TourData", 6) //                                         //$NON-NLS-1$

               + "   FROM " + TourDatabase.TABLE_EQUIPMENT_PART + " AS part" + NL //               //$NON-NLS-1$ //$NON-NLS-2$

               + "   JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS j_Td_Eq" //       //$NON-NLS-1$ //$NON-NLS-2$
               + "      ON j_Td_Eq.equipment_equipmentid = part.equipment_equipmentid" + NL //     //$NON-NLS-1$

               + "   JOIN " + TourDatabase.TABLE_TOUR_DATA + " AS TourData" //                     //$NON-NLS-1$ //$NON-NLS-2$
               + "      ON TourData.tourid = j_Td_Eq.tourdata_tourid" + NL //                      //$NON-NLS-1$
               + "      AND TourData.tourstarttime >= part.dateCollateFrom" + NL //                //$NON-NLS-1$
               + "      AND TourData.tourstarttime <  part.dateCollateUntil" + NL //               //$NON-NLS-1$

               + appFilter.getWhereClause()
               + partFilter_AND_OR.getSqlString()

               + "   WHERE part.EQUIPMENT_EQUIPMENTID = ?" + NL //                                 //$NON-NLS-1$

               + partFilter_NOT.getSqlString()

               + ") AS tdFields" + NL //                                                           //$NON-NLS-1$

               + NL;

         statement = conn.prepareStatement(sql);

         int nextIndex = 1;

         nextIndex = appFilter.setParameters(statement, nextIndex);
         nextIndex = partFilter_AND_OR.setParameters(statement, nextIndex);

         statement.setLong(nextIndex++, equipmentID);

         nextIndex = partFilter_NOT.setParameters(statement, nextIndex);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final int numTours = result.getInt(1);

            eqItem.numTours_IsNotCollated = numTours;

            eqItem.readCommonValues(result, 2);

            // there should be only one part
            break;
         }

         final long numTours = eqItem.numTours_IsNotCollated;

         if (numTours == 0) {

            // hide expand UI icon when there are no children

            eqItem.setChildren(new ArrayList<>());
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      } finally {
         Util.closeSql(statement);
      }
   }

   static void loadSummarizedValues_Part(final TVIEquipmentView_Part partItem) {

      if (_partLoadCounter != _updateCounter) {

         // clear waiting queue

         final BlockingQueue<Runnable> taskQueue = _sqlPartExecutor.getQueue();

         for (final Runnable runnable : taskQueue) {

            final FutureTask<?> task = (FutureTask<?>) runnable;

            task.cancel(false);
         }

         _sqlPartWaitingQueue.clear();

         _partLoadCounter = _updateCounter;
      }

      final SQLPartLoader sqlLoader = new SQLPartLoader(partItem, _updateCounter);

      _sqlPartWaitingQueue.add(sqlLoader);

      final Runnable executorTask = new Runnable() {
         @Override
         public void run() {

            // get last added loader item
            final SQLPartLoader loaderItem = _sqlPartWaitingQueue.pollFirst();

            if (loaderItem == null) {
               return;
            }

            final int loaderCounter = loaderItem.__loadCounter;

            if (loaderCounter != _updateCounter) {
               return;
            }

            final TVIEquipmentView_Part loaderPartItem = loaderItem.__partItem;

            loadSummarizedValues_Part_Runnable(loaderPartItem);

            if (loaderCounter != _updateCounter) {
               return;
            }

            Display.getDefault().asyncExec(() -> {

               partItem.getEquipmentViewer().update(loaderPartItem, null);
            });
         }
      };

      _sqlPartExecutor.submit(executorTask);
   }

   /**
    * Summarizes all tour values for each part and type
    *
    * @return
    */
   private static void loadSummarizedValues_Part_Runnable(final TVIEquipmentView_Part partItem) {

      final EquipmentViewerType viewerType = partItem.getViewerType();

      String sql = null;
      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter appFilter = partItem.createAppFilter();
         final SQLData partFilter_AND_OR = new EquipmentPartFilter_AND_OR(viewerType).getSqlData();
         final SQLData partFilter_NOT = new EquipmentPartFilter_NOT(viewerType).getSqlData();

         sql = UI.EMPTY_STRING

               + "--" + NL //                                                                      //$NON-NLS-1$
               + NL
               + "-----------" + NL //                                                             //$NON-NLS-1$
               + "-- part sum" + NL //                                                             //$NON-NLS-1$
               + "-----------" + NL //                                                             //$NON-NLS-1$
               + NL

               + "SELECT" + NL //                                                                  //$NON-NLS-1$

               + "   COUNT(*) AS Num_Tours," + NL //                                               //$NON-NLS-1$

               + TVIEquipmentView_Item.getSQL_SUM_COLUMNS("tdFields", 3) //                                              //$NON-NLS-1$

               + "FROM" + NL //                                                                    //$NON-NLS-1$
               + "(" + NL //                                                                       //$NON-NLS-1$

               + "   SELECT" + NL //                                                               //$NON-NLS-1$

               // the part filter can create duplicated tour ids
               + "      DISTINCT TourData.TourID," + NL //                                         //$NON-NLS-1$

               + TVIEquipmentView_Item.getSQL_SUM_TOUR_COLUMNS("TourData", 6) //                                         //$NON-NLS-1$

               + "   FROM " + TourDatabase.TABLE_EQUIPMENT_PART + " AS part" + NL //               //$NON-NLS-1$ //$NON-NLS-2$

               + "   JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS j_Td_Eq" //       //$NON-NLS-1$ //$NON-NLS-2$
               + "      ON j_Td_Eq.equipment_equipmentid = part.equipment_equipmentid" + NL //     //$NON-NLS-1$

               + "   JOIN " + TourDatabase.TABLE_TOUR_DATA + " AS TourData" //                     //$NON-NLS-1$ //$NON-NLS-2$
               + "      ON TourData.tourid = j_Td_Eq.tourdata_tourid" + NL //                      //$NON-NLS-1$
               + "      AND TourData.tourstarttime >= part.dateCollateFrom" + NL //                //$NON-NLS-1$
               + "      AND TourData.tourstarttime <  part.dateCollateUntil" + NL //               //$NON-NLS-1$

               + appFilter.getWhereClause()
               + partFilter_AND_OR.getSqlString()

               + "   WHERE part.isCollate = TRUE" + NL //                                          //$NON-NLS-1$
               + "     AND part.partID    = ?" + NL //                                             //$NON-NLS-1$

               + partFilter_NOT.getSqlString()

               + ") AS tdFields" + NL //                                                           //$NON-NLS-1$

               + NL;

         statement = conn.prepareStatement(sql);

         int nextIndex = 1;

         nextIndex = appFilter.setParameters(statement, nextIndex);
         nextIndex = partFilter_AND_OR.setParameters(statement, nextIndex);

         statement.setLong(nextIndex++, partItem.getPartID());

         nextIndex = partFilter_NOT.setParameters(statement, nextIndex);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final int numTours = result.getInt(1);

            partItem.numTours_IsCollated = numTours;

            partItem.readCommonValues(result, 2);

            // there should be only one part
            break;
         }

         final long numTours = partItem.numTours_IsCollated;

         if (numTours == 0) {

            // hide expand UI icon when there are no children

            partItem.setChildren(new ArrayList<>());
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      } finally {
         Util.closeSql(statement);
      }
   }

   public static void startAnUpdate() {

      _updateCounter++;
   }
}
