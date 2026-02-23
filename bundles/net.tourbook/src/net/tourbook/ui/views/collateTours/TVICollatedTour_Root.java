/*******************************************************************************
 * Copyright (C) 2015, 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.collateTours;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.AppFilter;
import net.tourbook.ui.TourTypeSQLData;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;

public class TVICollatedTour_Root extends TVICollatedTour {

   TVICollatedTour_Root(final CollatedToursView view) {
      super(view);
   }

   @Override
   protected void fetchChildren() {

      collateToursView.setIsInUIUpdate(true);
      {
         // set the children for the root item
         final ArrayList<TreeViewerItem> children = new ArrayList<>();
         setChildren(children);

         final TourTypeSQLData sqlData = collateToursView.getCollatedSQL();
         if (sqlData == null) {
            return;
         }

         final AppFilter sqlFilter = new AppFilter();

         final ArrayList<TVICollatedTour_Event> collateEvents = getCollateEvents(sqlData);

         children.addAll(collateEvents);

         getCollateSums(collateEvents, sqlFilter);
      }
      collateToursView.setIsInUIUpdate(false);
   }

   /**
    * Get all events/tours for the selected tour type filter
    *
    * @param sqlData
    *
    * @return
    */
   private ArrayList<TVICollatedTour_Event> getCollateEvents(final TourTypeSQLData sqlData) {

      final ArrayList<TVICollatedTour_Event> collateEvents = new ArrayList<>();

      final String sql = UI.EMPTY_STRING //

            + "--" + NL //                                                    //$NON-NLS-1$
            + NL
            + "---------------------" + NL //                                 //$NON-NLS-1$
            + "-- collated - events" + NL //                                  //$NON-NLS-1$
            + "---------------------" + NL //                                 //$NON-NLS-1$
            + NL

            + "SELECT" + NL //                                                //$NON-NLS-1$

            + "   tourID," + NL //                                         1  //$NON-NLS-1$

            + "   jTdataTtag.TourTag_tagId," + NL//                        2  //$NON-NLS-1$
            + "   jTdataEq.Equipment_EquipmentID," + NL //                 3  //$NON-NLS-1$

            + "   tourStartTime," + NL //                                  4  //$NON-NLS-1$
            + "   tourTitle" + NL //                                       5  //$NON-NLS-1$

            + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                  //$NON-NLS-1$

            // get tag ids
            + "LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag" //   //$NON-NLS-1$ //$NON-NLS-2$
            + " ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //                           //$NON-NLS-1$

            // get all equipment ids
            + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS jTdataEq" //       //$NON-NLS-1$ //$NON-NLS-2$
            + "  ON TourData.TOURID = jTdataEq.TOURDATA_TOURID" + NL //                            //$NON-NLS-1$

            + "WHERE 1=1" + NL //                                             //$NON-NLS-1$

            + sqlData.getWhereString()

            + "ORDER BY tourStartTime" + NL //                                //$NON-NLS-1$

            + NL
            + "--;" + NL //                                                   //$NON-NLS-1$
      ;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         int eventCounter = 0;

         final PreparedStatement statement = conn.prepareStatement(sql);

         sqlData.setParameters(statement, 1);

         long prevTourId = -1;

         HashSet<Long> allEquipmentIDs = null;
         HashSet<Long> allTagIDs = null;

         final ResultSet result = statement.executeQuery();
         while (result.next()) {

            final long dbTourId = result.getLong(1);

            final Object dbTagId = result.getObject(2);
            final Object dbEquipmentId = result.getObject(3);

            if (dbTourId == prevTourId) {

               // additional result set's for the same tour

               // get tags from outer join
               if (dbTagId instanceof Long) {
                  allTagIDs.add((Long) dbTagId);
               }

               // get equipment from outer join
               if (dbEquipmentId instanceof Long) {
                  allEquipmentIDs.add((Long) dbEquipmentId);
               }

            } else {

               final long dbTourStartTime = result.getLong(4);
               final String dbTourTitle = result.getString(5);

               final TVICollatedTour_Event collateEvent = new TVICollatedTour_Event(collateToursView, this);
               collateEvents.add(collateEvent);

               final ZonedDateTime eventStart = TimeTools.getZonedDateTime(dbTourStartTime);

               collateEvent.treeColumn = dbTourTitle == null ? UI.EMPTY_STRING : dbTourTitle;

               collateEvent.tourId = dbTourId;
               collateEvent.eventStart = eventStart;

               collateEvent.isFirstEvent = eventCounter++ == 0;

               collateEvent.colTourTitle = dbTourTitle;

               // get first tag id
               if (dbTagId instanceof Long) {

                  allTagIDs = new HashSet<>();
                  allTagIDs.add((Long) dbTagId);

                  collateEvent.setTagIds(allTagIDs);
               }

               // get first equipment id
               if (dbEquipmentId instanceof Long) {

                  allEquipmentIDs = new HashSet<>();
                  allEquipmentIDs.add((Long) dbEquipmentId);

                  collateEvent.setEquipmentIds(allEquipmentIDs);
               }
            }

            prevTourId = dbTourId;
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      /*
       * Add an additional event which shows the tours from the last event until today
       */
      final TVICollatedTour_Event collateEvent = new TVICollatedTour_Event(collateToursView, this);
      collateEvents.add(collateEvent);

      final ZonedDateTime eventStart = TimeTools.now();

      collateEvent.treeColumn = UI.EMPTY_STRING;
      collateEvent.eventStart = eventStart;
      collateEvent.isLastEvent = true;

      return collateEvents;
   }

   private void getCollateSums(final ArrayList<TVICollatedTour_Event> collatedEvents, final AppFilter sqlFilter) {

      final int eventSize = collatedEvents.size();

      final String sql = UI.EMPTY_STRING

            + "--" + NL //                                                 //$NON-NLS-1$
            + NL
            + "------------------------" + NL //                           //$NON-NLS-1$
            + "-- collated - event sums" + NL //                           //$NON-NLS-1$
            + "------------------------" + NL //                           //$NON-NLS-1$
            + NL

            + "SELECT" + NL //                                             //$NON-NLS-1$

            + SQL_SUM_COLUMNS

            + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //               //$NON-NLS-1$

            + "WHERE TourStartTime >= ? AND TourStartTime < ?" + NL //     //$NON-NLS-1$

            + sqlFilter.getWhereClause()

            + NL
            + "--;" + NL //                                                //$NON-NLS-1$
      ;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final PreparedStatement statement = conn.prepareStatement(sql);
         sqlFilter.setParameters(statement, 3);

         final long[] prevStart = { 0 };
         final int[] eventCounter = { 0 };
         final int[] eventIndex = { 0 };

         final long start = System.currentTimeMillis();

         boolean isLongDuration = false;

         for (; eventIndex[0] < eventSize;) {

            final int currentEventIndex = eventIndex[0]++;

            final boolean isFirstEvent = currentEventIndex == 0;
            final TVICollatedTour_Event collateEvent = collatedEvents.get(currentEventIndex);

            final long eventStart = isFirstEvent ? Long.MIN_VALUE : prevStart[0];
            final long eventEnd = collateEvent.eventStart.toInstant().toEpochMilli();

            prevStart[0] = eventEnd;

            /*
             * This is a highly complicated algorithm that the eventStart is overwritten again
             */
            collateEvent.eventStart = TimeTools.getZonedDateTime(eventStart);
            collateEvent.eventEnd = TimeTools.getZonedDateTime(eventEnd);
            collateEvent.isFirstEvent = isFirstEvent;

            statement.setLong(1, eventStart);
            statement.setLong(2, eventEnd);

            final ResultSet result = statement.executeQuery();

            while (result.next()) {
               collateEvent.addSumColumns(result, 1);
            }

            /*
             * Check if this is a long duration, run in progress monitor
             */
            final long runDuration = System.currentTimeMillis() - start;
            if (runDuration > 500) {
               isLongDuration = true;
               break;
            }

            ++eventCounter[0];
         }

         if (isLongDuration) {

            try {

               /*
                * Run with a monitor because it can take a longer time until all is computed.
                */

               final IRunnableWithProgress runnable = new IRunnableWithProgress() {
                  @Override
                  public void run(final IProgressMonitor monitor) throws InvocationTargetException,
                        InterruptedException {

                     try {
                        monitor.beginTask(Messages.Tour_Book_Monitor_CollateTask, eventSize);
                        monitor.worked(eventCounter[0]);

                        for (; eventIndex[0] < eventSize;) {

                           if (monitor.isCanceled()) {
                              break;
                           }

                           final int currentEventIndex = eventIndex[0]++;

                           final boolean isFirstEvent = currentEventIndex == 0;
                           final TVICollatedTour_Event collateEvent = collatedEvents.get(currentEventIndex);

                           final long eventStart = isFirstEvent ? Long.MIN_VALUE : prevStart[0];
                           final long eventEnd = collateEvent.eventStart.toInstant().toEpochMilli();

                           prevStart[0] = eventEnd;

                           /*
                            * This is a highly complicated algorithm that the eventStart
                            * is overwritten again
                            */
                           collateEvent.eventStart = TimeTools.getZonedDateTime(eventStart);
                           collateEvent.eventEnd = TimeTools.getZonedDateTime(eventEnd);
                           collateEvent.isFirstEvent = isFirstEvent;

                           statement.setLong(1, eventStart);
                           statement.setLong(2, eventEnd);

                           final ResultSet result = statement.executeQuery();

                           while (result.next()) {
                              collateEvent.addSumColumns(result, 1);
                           }

                           monitor.subTask(NLS.bind(
                                 Messages.Tour_Book_Monitor_CollateSubtask,
                                 ++eventCounter[0],
                                 eventSize));
                           monitor.worked(1);
                        }
                     } catch (final SQLException e) {
                        SQL.showException(e, sql);
                     }
                  }
               };

               /*
                * Use the shell of the main app that the tooltip is not covered with the
                * monitor, otherwise it would be centered of the active shell.
                */
               new ProgressMonitorDialog(collateToursView.getShell()).run(true, true, runnable);

            } catch (final InvocationTargetException | InterruptedException e) {
               StatusUtil.showStatus(e);
            }
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }
   }
}
