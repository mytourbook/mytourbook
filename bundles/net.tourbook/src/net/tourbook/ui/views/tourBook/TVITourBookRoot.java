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
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.tour.filter.TourTagFilter_WithExists;
import net.tourbook.ui.SQLFilter;

public class TVITourBookRoot extends TVITourBookItem {

   /**
    * @param view
    * @param viewLayout
    */
   TVITourBookRoot(final TourBookView view) {

      super(view);
   }

   @Override
   protected void fetchChildren() {

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         /*
          * set the children for the root item, these are year items
          */
         final ArrayList<TreeViewerItem> allYearItems = new ArrayList<>();
         setChildren(allYearItems);

         final boolean isShowSummaryRow = tourBookView.isShowSummaryRow();
         final String sqlGroupBy = isShowSummaryRow

               // show a summary row
               ? "GROUP BY ROLLUP(StartYear)" + NL //          //$NON-NLS-1$

               // hide summary row
               : "GROUP BY StartYear" + NL; //                 //$NON-NLS-1$

         final SQLFilter appFilter = new SQLFilter(SQLFilter.ANY_APP_FILTERS_NO_TAG);
         final TourTagFilter_WithExists tagFilter = new TourTagFilter_WithExists();

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                              //$NON-NLS-1$

               + "	StartYear," + NL //                       //$NON-NLS-1$
               + SQL_SUM_COLUMNS //

               + "FROM TOURDATA" + NL //                       //$NON-NLS-1$

               + "WHERE 1=1" + NL //                           //$NON-NLS-1$

               + appFilter.getWhereClause() + NL //
               + tagFilter.getSql()

               + sqlGroupBy

               + "ORDER BY StartYear" + NL //                  //$NON-NLS-1$
         ;

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int nextIndex = 1;

         // set filter parameters
         nextIndex = appFilter.setParameters(prepStmt, nextIndex);
         nextIndex = tagFilter.setParameters(prepStmt, nextIndex);

         int yearIndex = 0;
         int summaryIndex = 0;

         final ResultSet result = prepStmt.executeQuery();

         while (result.next()) {

            final int dbYear = result.getInt(1);

            final TVITourBookYear yearItem = new TVITourBookYear(tourBookView, this);
            allYearItems.add(yearItem);

            yearItem.treeColumn = Integer.toString(dbYear);
            yearItem.tourYear = dbYear;

            yearItem.colTourDateTime = new TourDateTime(calendar8.withYear(dbYear));
            yearItem.addSumColumns(result, 2);

            if (isShowSummaryRow && dbYear == 0) {

               // this should be the summary row
               summaryIndex = yearIndex;

               // add summary flag to the last row
               yearItem.isRowSummary = true;
            }

            if (UI.IS_SCRAMBLE_DATA) {

               yearItem.scrambleData();

               yearItem.treeColumn = UI.scrambleText(yearItem.treeColumn);
            }

            yearIndex++;
         }

         /**
          * It can happen that the summary row is not the last row (seems to be a bug in derby)
          */
         final int numChildren = allYearItems.size();

         if (isShowSummaryRow

               // ensure there are items
               && numChildren > 0

               // check last position
               && summaryIndex != numChildren - 1) {

            // wrong summary row detected

            // move summary to the end
            final TreeViewerItem summarytYearItem = allYearItems.remove(summaryIndex);

            allYearItems.add(summarytYearItem);
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }
   }
}
