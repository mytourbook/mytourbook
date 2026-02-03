/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalField;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.AppFilter;

public class TVITourBookYear extends TVITourBookItem {

   private static final String YEAR_WEEK_FORMAT = "[%02d] %s"; //$NON-NLS-1$

   private TourBookViewLayout  _viewLayout;

   boolean                     isRowSummary;

   public TVITourBookYear(final TourBookView view, final TVITourBookItem parentItem) {

      super(view);

      _viewLayout = view.getViewLayout();

      setParentItem(parentItem);
   }

   @Override
   protected void fetchChildren() {

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final boolean isWeekDisplayed = _viewLayout == TourBookViewLayout.CATEGORY_WEEK;

         final ArrayList<TreeViewerItem> children = new ArrayList<>();
         setChildren(children);

         final ZonedDateTime tourWeek = calendar8.with(
               TimeTools.calendarWeek.dayOfWeek(),
               TimeTools.calendarWeek.getFirstDayOfWeek().getValue());

         String sqlSumYearField = UI.EMPTY_STRING;
         String sqlSumYearFieldSub = UI.EMPTY_STRING;

         if (isWeekDisplayed) {

            // show weeks

            sqlSumYearField = "StartWeekYear"; //$NON-NLS-1$
            sqlSumYearFieldSub = "StartWeek"; //$NON-NLS-1$

         } else {

            // show months

            sqlSumYearField = "StartYear"; //$NON-NLS-1$
            sqlSumYearFieldSub = "StartMonth"; //$NON-NLS-1$
         }

         final AppFilter appFilter = new AppFilter(AppFilter.ANY_APP_FILTERS);

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                      //$NON-NLS-1$

               + sqlSumYearField + "," + NL //                                         //$NON-NLS-1$
               + sqlSumYearFieldSub + "," + NL //                                      //$NON-NLS-1$
               
               + SQL_SUM_COLUMNS

               + "FROM TOURDATA" + NL //                                               //$NON-NLS-1$

               + "WHERE" + NL //                                                       //$NON-NLS-1$
               + "	" + sqlSumYearField + " = ?" + NL //                                //$NON-NLS-1$ //$NON-NLS-2$

               + appFilter.getWhereClause()

               + "GROUP BY " + sqlSumYearField + "," + sqlSumYearFieldSub + NL //      //$NON-NLS-1$ //$NON-NLS-2$
               + "ORDER BY " + sqlSumYearFieldSub + NL //                              //$NON-NLS-1$
         ;

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int nextIndex = 1;

         // set WHERE parameters
         prepStmt.setInt(nextIndex++, tourYear);

         // set filter parameters
         nextIndex = appFilter.setParameters(prepStmt, nextIndex);

         final ResultSet result = prepStmt.executeQuery();
         while (result.next()) {

            final TVITourBookItem tourItem = new TVITourBookYearCategorized(tourBookView, this, _viewLayout);

            children.add(tourItem);

            final int dbYear = result.getInt(1);
            final int dbYearSub = result.getInt(2);

            String columnText;

            /*
             * Fixed "java.time.LocalDate cannot be cast to java.time.ZonedDateTime" exception with
             * this special date/time construction
             */
            LocalDate categoryDate;
            final LocalDate tourWeekLocal = tourWeek.toLocalDate();

            if (isWeekDisplayed) {

               // week

               final TemporalField weekBasedYear = TimeTools.calendarWeek.weekBasedYear();
               final TemporalField weekOfYear = TimeTools.calendarWeek.weekOfYear();

               categoryDate = tourWeekLocal
                     .with(weekBasedYear, dbYear)
                     .with(weekOfYear, dbYearSub);

               columnText = String.format(
                     YEAR_WEEK_FORMAT,
                     dbYearSub,
                     categoryDate.format(TimeTools.Formatter_Week_Month));

            } else {

               // month

               categoryDate = tourWeekLocal
                     .withYear(dbYear)
                     .withMonth(dbYearSub);

               columnText = categoryDate.format(TimeTools.Formatter_Month);
            }

            final ZonedDateTime zonedWeek = ZonedDateTime.from(tourWeek);
            final ZonedDateTime zonedWeekDate = zonedWeek.with(categoryDate);

            tourItem.treeColumn = columnText;

            tourItem.tourYear = dbYear;
            tourItem.tourYearSub = dbYearSub;
            tourItem.colTourDateTime = new TourDateTime(zonedWeekDate);

            tourItem.addSumColumns(result, 3);

            if (UI.IS_SCRAMBLE_DATA) {

               tourItem.scrambleData();

               tourItem.treeColumn = UI.scrambleText(tourItem.treeColumn);
            }
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }
   }

   @Override
   public boolean hasChildren() {

      if (isRowSummary) {

         // row summary has no children

         return false;
      }

      return super.hasChildren();
   }

   @Override
   public String toString() {

      boolean isShortInfo = true;
      isShortInfo = isShortInfo == true;

      if (isShortInfo) {

         return "TVITourBookYear  tourYear = " + tourYear; //     //$NON-NLS-1$

      } else {

         return NL

               + "TVITourBookYear" + NL //                        //$NON-NLS-1$

               + "[" + NL //                                      //$NON-NLS-1$

               + " tourYear     = " + tourYear + NL //            //$NON-NLS-1$
               + " isRowSummary = " + isRowSummary + NL //        //$NON-NLS-1$
               + " _viewLayout  = " + _viewLayout + NL //         //$NON-NLS-1$

               + "]" + NL //                                      //$NON-NLS-1$
         ;
      }
   }

}
