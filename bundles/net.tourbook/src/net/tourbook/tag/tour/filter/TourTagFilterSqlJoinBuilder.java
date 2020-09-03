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
package net.tourbook.tag.tour.filter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.util.SQLData;
import net.tourbook.database.TourDatabase;

public class TourTagFilterSqlJoinBuilder {

   private static final char   NL                  = UI.NEW_LINE;

   private static final String PARAMETER_FIRST     = " ?";         //$NON-NLS-1$
   private static final String PARAMETER_FOLLOWING = ", ?";        //$NON-NLS-1$

   private boolean             _isNoTagFilter_Or_CombineTagsWithOr;
   private String              _sqlTagJoinTable;

   private SQLData             _sqlCombineTagsWithAnd;

   public TourTagFilterSqlJoinBuilder() {

      _isNoTagFilter_Or_CombineTagsWithOr = TourTagFilterManager.isNoTagsFilter_Or_CombineTagsWithOr();

      if (_isNoTagFilter_Or_CombineTagsWithOr) {

         // combine tags with OR

         _sqlTagJoinTable = "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG;

      } else {

         // combine tags with AND

         _sqlCombineTagsWithAnd = createSql_CombineTagsWithAnd();
         _sqlTagJoinTable = _sqlCombineTagsWithAnd.getSqlString();
      }
   }

   public static SQLData createSql_CombineTagsWithAnd() {

      final SQLData sqlJoinPartForAndOperator = createSQL_JoinPartForAndOperator();

      // the name "Count_TourId" is required to prevent duplicated names !!!

      final String sql = NL

            + " INNER JOIN" + NL //                                                                      //$NON-NLS-1$
            + " (" + NL //                                                                               //$NON-NLS-1$
            + "    SELECT *" + NL //                                                                     //$NON-NLS-1$
            + "    FROM TOURDATA_TOURTAG" + NL //                                                        //$NON-NLS-1$
            + "    INNER JOIN" + NL //                                                                   //$NON-NLS-1$
            + "    (" + NL //                                                                            //$NON-NLS-1$
            + "       SELECT TOURDATA_TOURID AS Count_TourId, COUNT(*) AS NumTagIds" + NL //             //$NON-NLS-1$
            + "       FROM TOURDATA_TOURTAG" + NL //                                                     //$NON-NLS-1$
            + "       WHERE " + sqlJoinPartForAndOperator.getSqlString() + NL //                         //$NON-NLS-1$
            + "       GROUP BY TOURDATA_TOURID" + NL //                                                  //$NON-NLS-1$
            + "       HAVING COUNT(TOURDATA_TOURID) = ?" + NL //                                         //$NON-NLS-1$
            + "    )" + NL //                                                                            //$NON-NLS-1$
            + "    AS jTdataTtag " + NL //                                                               //$NON-NLS-1$
            + "    ON TOURDATA_TOURTAG.TOURDATA_TOURID = jTdataTtag.Count_TourId" + NL //                //$NON-NLS-1$
            + " ) " + NL //                                                                              //$NON-NLS-1$
      ;

      final ArrayList<Object> sqlParameters = sqlJoinPartForAndOperator.getParameters();

      // add number of tags that only tours with ALL tags are available in the join table
      sqlParameters.add(sqlParameters.size());

      return new SQLData(sql, sqlParameters);
   }

   /**
    * @return Returns a SQL part for the tag filter when the tags are AND'ed.
    */
   private static SQLData createSQL_JoinPartForAndOperator() {

      final long[] tagIds = TourTagFilterManager.getSelectedProfile().tagFilterIds.toArray();
      final ArrayList<Object> sqlParameters = new ArrayList<>();

      final StringBuilder tagIdsAsParameters = new StringBuilder();

      for (int tagIndex = 0; tagIndex < tagIds.length; tagIndex++) {
         final long tagId = tagIds[tagIndex];
         if (tagIndex == 0) {
            tagIdsAsParameters.append(PARAMETER_FIRST);
         } else {
            tagIdsAsParameters.append(PARAMETER_FOLLOWING);
         }

         sqlParameters.add(tagId);
      }

      final String sql = " TOURTAG_TAGID IN (" + tagIdsAsParameters.toString() + ")" + NL; //$NON-NLS-1$ //$NON-NLS-2$

      return new SQLData(sql, sqlParameters);
   }

   /**
    * @return the sqlTagJoinTable
    */
   public String getSqlTagJoinTable() {
      return _sqlTagJoinTable;
   }

   /**
    * Sets the SQL tag parameters when necessary
    *
    * @param prepStmt
    * @param paramIndexFrom
    * @return Returns the last parameter index +1 which was used for setting parameters
    * @throws SQLException
    */
   public int setParameters(final PreparedStatement prepStmt, final int paramIndexFrom) throws SQLException {

      int paramIndex = paramIndexFrom;

      // set sql tag parameters
      if (_isNoTagFilter_Or_CombineTagsWithOr) {

         // nothing more to do

      } else {

         // combine tags with AND

         // set join parameters
         _sqlCombineTagsWithAnd.setParameters(prepStmt, paramIndex);
         paramIndex = _sqlCombineTagsWithAnd.getLastParameterIndex();
      }

      return paramIndex;
   }

}
