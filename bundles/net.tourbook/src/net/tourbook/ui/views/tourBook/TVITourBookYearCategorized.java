/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
import java.sql.SQLException;

import net.tourbook.database.TourDatabase;
import net.tourbook.tag.tour.filter.TourTagFilterSqlJoinBuilder;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.UI;

public class TVITourBookYearCategorized extends TVITourBookItem {

   private TourBookViewLayout _category;

   public TVITourBookYearCategorized(final TourBookView view,
                                     final TVITourBookItem parentItem,
                                     final TourBookViewLayout itemType) {

      super(view);

      _category = itemType;

      setParentItem(parentItem);
   }

   /**
    * Fetch all tour data within a month/week category.
    */
   @Override
   protected void fetchChildren() {

      /*
       * Set the children for the yearSub (month,week,...) item, these are tour items
       */
      String sumYear = UI.EMPTY_STRING;
      String sumYearSub = UI.EMPTY_STRING;

      if (_category == TourBookViewLayout.CATEGORY_WEEK) {

         // categorize by week

         sumYear = "startWeekYear"; //$NON-NLS-1$
         sumYearSub = "startWeek"; //$NON-NLS-1$

      } else {

         // categorize by month (default)

         sumYear = "startYear"; //$NON-NLS-1$
         sumYearSub = "startMonth"; //$NON-NLS-1$
      }

      final SQLFilter sqlAppFilter = new SQLFilter(SQLFilter.TAG_FILTER);

      final TourTagFilterSqlJoinBuilder tagFilterSqlJoinBuilder = new TourTagFilterSqlJoinBuilder();

      final String sql = NL

            + "SELECT" + NL //                                                      //$NON-NLS-1$

            + SQL_ALL_TOUR_FIELDS + UI.COMMA_SPACE + NL
            + SQL_ALL_OTHER_FIELDS + NL

            + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                        //$NON-NLS-1$

            // get/filter tag's
            + tagFilterSqlJoinBuilder.getSqlTagJoinTable() + " jTdataTtag" //       //$NON-NLS-1$
            + " ON tourID = jTdataTtag.TourData_tourId" + NL //                     //$NON-NLS-1$

            // get marker id's
            + "LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_MARKER + " Tmarker" //   //$NON-NLS-1$ //$NON-NLS-2$
            + " ON TourData.tourId = Tmarker.TourData_tourId" + NL //               //$NON-NLS-1$

            + "WHERE  " + sumYear + "=?" + NL //                                    //$NON-NLS-1$ //$NON-NLS-2$
            + "   AND " + sumYearSub + "=?" + NL //                                 //$NON-NLS-1$ //$NON-NLS-2$
            + "   " + sqlAppFilter.getWhereClause() //$NON-NLS-1$

            + "ORDER BY TourStartTime" + NL; //                                     //$NON-NLS-1$

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

//         TourDatabase.enableRuntimeStatistics(conn);

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int paramIndex = 1;

         // set sql tag parameters
         paramIndex = tagFilterSqlJoinBuilder.setParameters(prepStmt, paramIndex);
//         }

         // set sql other parameters
         prepStmt.setInt(paramIndex++, tourYear);
         prepStmt.setInt(paramIndex++, tourYearSub);
         sqlAppFilter.setParameters(prepStmt, paramIndex++);

         fetchTourItems(prepStmt);

//       TourDatabase.disableRuntimeStatistic(conn);

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }
   }

   public TourBookViewLayout getCategory() {
      return _category;
   }

}
