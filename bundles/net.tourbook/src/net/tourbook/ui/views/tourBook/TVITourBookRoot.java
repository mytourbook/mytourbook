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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.SQLData;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.tour.filter.TourTagFilterManager;
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

      getItemsHierarchical();
   }

   private void getItemsHierarchical() {

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         /*
          * set the children for the root item, these are year items
          */
         final ArrayList<TreeViewerItem> children = new ArrayList<>();
         setChildren(children);

         final SQLFilter sqlAppFilter = new SQLFilter(SQLFilter.TAG_FILTER);
         String sqlFromTourData;

         final String sqlFilterWhereClause = sqlAppFilter.getWhereClause().trim();
         final boolean isSqlWhereClause = sqlFilterWhereClause.length() > 0;

         final String sqlWhereClause = isSqlWhereClause
               ? "WHERE 1=1 " + NL + sqlFilterWhereClause + NL //$NON-NLS-1$
               : UI.EMPTY_STRING;

         final boolean isTourTagFilterEnabled = TourTagFilterManager.isTourTagFilterEnabled();
         boolean isNoTagFilter_Or_CombineTagsWithOr = false;
         SQLData sqlCombineTagsWithAnd = null;

         if (isTourTagFilterEnabled) {

            // with tag filter

            isNoTagFilter_Or_CombineTagsWithOr = TourTagFilterManager.isNoTagsFilter_Or_CombineTagsWithOr();

            String sqlTagJoinTable;

            if (isNoTagFilter_Or_CombineTagsWithOr) {

               /**
                * <code>
                *
                * SELECT
                *    StartYear,
                *    SUM( CAST(TourDistance AS BIGINT)),
                *    ...
                * 	MAX(CASE WHEN weather_Temperature_Max = 0 THEN NULL ELSE weather_Temperature_Max END)
                * FROM (
                *    SELECT
                *       DISTINCT TourId,
                *       StartYear,
                *       ...
                * 		weather_Temperature_Max
                * 	FROM TOURDATA
                * 	LEFT JOIN TOURDATA_TOURTAG AS jTdataTtag ON TourData.tourId = jTdataTtag.TourData_tourId
                *    WHERE 1=1
                * 		AND TourData.tourPerson_personId = 0
                * 		AND TourData.tourType_typeId IN ( 0, 1, 34)
                * 		AND jTdataTtag.TourTag_tagId IN ( 22, 9)
                * ) NecessaryNameOtherwiseItDoNotWork
                * GROUP BY ROLLUP(StartYear)
                * ORDER BY StartYear
                *
                * </code>
                */

               sqlTagJoinTable = "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG;

            } else {

               /**
                * <code>
                *
                * SELECT
                *    StartYear,
                *    SUM( CAST(TourDistance AS BIGINT)),
                * 	...
                * 	MAX(CASE WHEN weather_Temperature_Max = 0 THEN NULL ELSE weather_Temperature_Max END)
                * FROM (
                *    SELECT
                *       DISTINCT TourId,
                *       StartYear,
                *       TourDistance,
                * 		...
                * 		weather_Temperature_Max
                * 	FROM TOURDATA
                * 	INNER JOIN
                * 	(
                * 	 SELECT *
                * 	 FROM TOURDATA_TOURTAG
                * 	 INNER JOIN
                * 	 (
                * 		 SELECT TOURDATA_TOURID AS Count_TourId, COUNT(*) AS NumTagIds
                * 		 FROM TOURDATA_TOURTAG
                * 		 WHERE  TOURTAG_TAGID IN ( 22, 9)
                * 		 GROUP BY TOURDATA_TOURID
                * 		 HAVING COUNT(TOURDATA_TOURID) = 2
                * 	 )
                * 	 AS jTdataTtag
                * 	 ON TOURDATA_TOURTAG.TOURDATA_TOURID = jTdataTtag.Count_TourId
                * 	)
                *    AS jTdataTtag ON TourData.tourId = jTdataTtag.TourData_tourId
                *    WHERE 1=1
                * 		AND TourData.tourPerson_personId = 0
                * 		AND TourData.tourType_typeId IN ( 0, 1, 34)
                * ) NecessaryNameOtherwiseItDoNotWork
                * GROUP BY ROLLUP(StartYear)
                * ORDER BY StartYear
                *
                * </code>
                */

               sqlCombineTagsWithAnd = TourTagFilterManager.createSql_CombineTagsWithAnd();
               sqlTagJoinTable = sqlCombineTagsWithAnd.getSqlString();
            }

            sqlFromTourData = UI.EMPTY_STRING

                  + "FROM (" + NL //                                                   //$NON-NLS-1$

                  + "   SELECT" + NL //                                                //$NON-NLS-1$

                  // this is necessary otherwise tours can occure multiple times when a tour contains multiple tags !!!
                  + "      DISTINCT TourId," + NL //                                   //$NON-NLS-1$

                  + "      StartYear," + NL //                                         //$NON-NLS-1$
                  + "      " + SQL_SUM_FIELDS

                  + "   FROM TOURDATA" + NL //                                         //$NON-NLS-1$

                  + "   " + sqlTagJoinTable

                  + "   AS jTdataTtag" //                                              //$NON-NLS-1$
                  + "   ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //       //$NON-NLS-1$

                  + "   " + sqlWhereClause

                  + ") NecessaryNameOtherwiseItDoNotWork" + NL //                      //$NON-NLS-1$
            ;

         } else {

            // without tag filter

            /**
             * <code>
             *
             *  SELECT
             *     StartYear,
             *     SUM( CAST(TourDistance AS BIGINT)),
             *     ...
             *     MAX(CASE WHEN weather_Temperature_Max = 0 THEN NULL ELSE weather_Temperature_Max END)
             *  FROM TOURDATA
             *  WHERE 1=1
             *     AND TourData.tourPerson_personId = 0
             *     AND TourData.tourType_typeId IN ( 0, 1, 34)
             *  GROUP BY ROLLUP(StartYear)
             *  ORDER BY StartYear
             *
             * </code>
             */

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
               + "   " + SQL_SUM_COLUMNS

               + sqlFromTourData
               + sqlGroupBy

               + "ORDER BY StartYear" + NL //                  //$NON-NLS-1$
         ;

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int paramIndex = 1;

         if (isTourTagFilterEnabled == false || isNoTagFilter_Or_CombineTagsWithOr) {

            // nothing more to do

         } else {

            // combine tags with AND

            // set join parameters
            sqlCombineTagsWithAnd.setParameters(prepStmt, paramIndex);
            paramIndex = sqlCombineTagsWithAnd.getLastParameterIndex();
         }

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
