/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
import net.tourbook.tag.tour.filter.TourTagFilterManager;
import net.tourbook.tag.tour.filter.TourTagFilterSqlJoinBuilder;
import net.tourbook.ui.SQLFilter;

public class TVITourBookYear extends TVITourBookItem {

   private static final String YEAR_WEEK_FORMAT = "[%02d] %s"; //$NON-NLS-1$

   private TourBookViewLayout  _subCategory;

   boolean                     isRowSummary;

   public TVITourBookYear(final TourBookView view, final TVITourBookItem parentItem) {

      super(view);

      _subCategory = view.getViewLayout();

      setParentItem(parentItem);
   }

   @Override
   protected void fetchChildren() {

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final boolean isWeekDisplayed = _subCategory == TourBookViewLayout.CATEGORY_WEEK;

         final ArrayList<TreeViewerItem> children = new ArrayList<>();
         setChildren(children);

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

         final SQLFilter sqlAppFilter = new SQLFilter(SQLFilter.TAG_FILTER);
         String sqlFromTourData;

         final TourTagFilterSqlJoinBuilder tagFilterSqlJoinBuilder = new TourTagFilterSqlJoinBuilder();

         if (TourTagFilterManager.isTourTagFilterEnabled()) {

            // with tag filter

            sqlFromTourData = NL

                  + "FROM (" + NL //                                                   //$NON-NLS-1$

                  + "   SELECT" + NL //                                                //$NON-NLS-1$

                  // this is necessary otherwise tours can occur multiple times when a tour contains multiple tags !!!
                  + "      DISTINCT TourId," + NL //                                   //$NON-NLS-1$

                  + "      " + sqlSumYearField + "," + NL //                           //$NON-NLS-1$ //$NON-NLS-2$
                  + "      " + sqlSumYearFieldSub + "," + NL //                        //$NON-NLS-1$ //$NON-NLS-2$
                  + "      " + SQL_SUM_FIELDS + NL //$NON-NLS-1$

                  + "   FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                  //$NON-NLS-1$

                  // get tag id's
                  + "       " + tagFilterSqlJoinBuilder.getSqlTagJoinTable() //$NON-NLS-1$

                  + "   AS jTdataTtag" + NL //$NON-NLS-1$
                  + "   ON tourID = jTdataTtag.TourData_tourId" + NL //                //$NON-NLS-1$

                  + "   WHERE " + sqlSumYearField + "=?" + NL //                       //$NON-NLS-1$ //$NON-NLS-2$
                  + "      " + sqlAppFilter.getWhereClause() //$NON-NLS-1$

                  + ") NecessaryNameOtherwiseItDoNotWork" + NL //                      //$NON-NLS-1$
            ;

         } else {

            // without tag filter

            sqlFromTourData = NL

                  + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                     //$NON-NLS-1$

                  + "WHERE " + sqlSumYearField + "=?" + NL //                          //$NON-NLS-1$ //$NON-NLS-2$
                  + "   " + sqlAppFilter.getWhereClause() + NL; //$NON-NLS-1$
         }

         sql = NL +

               "SELECT" + NL //                                                        //$NON-NLS-1$

               + sqlSumYearField + "," + NL //                                         //$NON-NLS-1$
               + sqlSumYearFieldSub + "," + NL //                                      //$NON-NLS-1$
               + SQL_SUM_COLUMNS

               + sqlFromTourData

               + "GROUP BY " + sqlSumYearField + "," + sqlSumYearFieldSub + NL //      //$NON-NLS-1$ //$NON-NLS-2$
               + "ORDER BY " + sqlSumYearFieldSub + NL //                              //$NON-NLS-1$
         ;

         final ZonedDateTime tourWeek = calendar8.with(
               TimeTools.calendarWeek.dayOfWeek(),
               TimeTools.calendarWeek.getFirstDayOfWeek().getValue());

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int paramIndex = 1;

         paramIndex = tagFilterSqlJoinBuilder.setParameters(prepStmt, paramIndex);

         // set sql parameters
         prepStmt.setInt(paramIndex++, tourYear);
         sqlAppFilter.setParameters(prepStmt, paramIndex++);

         final ResultSet result = prepStmt.executeQuery();
         while (result.next()) {

            final TVITourBookItem tourItem = new TVITourBookYearCategorized(tourBookView, this, _subCategory);

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
      return

      UI.EMPTY_STRING

//      getClass().getName() + "\n"

//          + "_subCategory = " + _subCategory + "\n" //$NON-NLS-1$ //$NON-NLS-2$
//          + "isRowSummary = " + isRowSummary + "\n" //$NON-NLS-1$ //$NON-NLS-2$

            + "tourYear     = " + tourYear + "  colCounter   = " + colCounter + "\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      ;
   }

}
