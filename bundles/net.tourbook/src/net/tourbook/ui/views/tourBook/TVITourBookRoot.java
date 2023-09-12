/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
import net.tourbook.tag.tour.filter.TourTagFilterManager;
import net.tourbook.tag.tour.filter.TourTagFilterSqlJoinBuilder;
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
         final ArrayList<TreeViewerItem> children = new ArrayList<>();
         setChildren(children);

         final SQLFilter sqlAppFilter = new SQLFilter(SQLFilter.ANY_APP_FILTERS);
         String sqlFromTourData;

         final String sqlFilterWhereClause = sqlAppFilter.getWhereClause().trim();
         final boolean isSqlWhereClause = sqlFilterWhereClause.length() > 0;

         final String sqlWhereClause = isSqlWhereClause
               ? "WHERE 1=1 " + NL + sqlFilterWhereClause + NL //$NON-NLS-1$
               : UI.EMPTY_STRING;

         final TourTagFilterSqlJoinBuilder tagFilterSqlJoinBuilder = new TourTagFilterSqlJoinBuilder();

         if (TourTagFilterManager.isTourTagFilterEnabled()) {

            // with tag filter

            sqlFromTourData = UI.EMPTY_STRING

                  + "FROM (" + NL //                                                         //$NON-NLS-1$

                  + "   SELECT" + NL //                                                      //$NON-NLS-1$

                  // this is necessary otherwise tours can occur multiple times when a tour contains multiple tags !!!
                  + "      DISTINCT TourId," + NL //                                         //$NON-NLS-1$

                  + "      StartYear," + NL //                                               //$NON-NLS-1$
                  + "      " + SQL_SUM_FIELDS //$NON-NLS-1$

                  + "   FROM TOURDATA" + NL //                                               //$NON-NLS-1$

                  + "   " + tagFilterSqlJoinBuilder.getSqlTagJoinTable() + " jTdataTtag" //  //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //             //$NON-NLS-1$

                  + "   " + sqlWhereClause //$NON-NLS-1$

                  + ") NecessaryNameOtherwiseItDoNotWork" + NL //                            //$NON-NLS-1$
            ;

         } else {

            // without tag filter

            sqlFromTourData = UI.EMPTY_STRING

                  + " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //$NON-NLS-1$

                  + sqlWhereClause;
         }

         final boolean isShowSummaryRow = tourBookView.isShowSummaryRow();

         final String sqlGroupBy = isShowSummaryRow

               // show a summary row
               ? "GROUP BY ROLLUP(StartYear)" + NL //          //$NON-NLS-1$
               : "GROUP BY StartYear" + NL; //                 //$NON-NLS-1$

         sql = NL +

               "SELECT" + NL //                                //$NON-NLS-1$

               + "   StartYear," + NL //                       //$NON-NLS-1$
               + "   " + SQL_SUM_COLUMNS //$NON-NLS-1$

               + sqlFromTourData
               + sqlGroupBy

               + "ORDER BY StartYear" + NL //                  //$NON-NLS-1$
         ;

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int paramIndex = 1;

         paramIndex = tagFilterSqlJoinBuilder.setParameters(prepStmt, paramIndex);

         sqlAppFilter.setParameters(prepStmt, paramIndex);

         TVITourBookYear yearItem = null;

         int yearIndex = 0;
         int summaryIndex = 0;

         final ResultSet result = prepStmt.executeQuery();
         while (result.next()) {

            final int dbYear = result.getInt(1);

            yearItem = new TVITourBookYear(tourBookView, this);
            children.add(yearItem);

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
         final int numChildren = children.size();

         if (isShowSummaryRow

               // ensure there are items
               && numChildren > 0

               // check last position
               && summaryIndex != numChildren - 1) {

            // wrong summary row detected

            // move summary to the end
            final TreeViewerItem summarytYearItem = children.remove(summaryIndex);

            children.add(summarytYearItem);
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }
   }

}
