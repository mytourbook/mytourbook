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
package net.tourbook.tag.tour.filter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.common.util.SQLData;

/**
 * The SQL EXISTS statement is from the github KI copilot optimizations
 */
public class TourTagFilter_WithExists {

   private static final char   NL                  = UI.NEW_LINE;

   private static final String PARAMETER_FIRST     = " ?";       //$NON-NLS-1$
   private static final String PARAMETER_FOLLOWING = ", ?";      //$NON-NLS-1$

   private boolean             _isEnabled;

   private SQLData             _sqlData;

   public TourTagFilter_WithExists() {

      _isEnabled = TourTagFilterManager.isTourTagFilterEnabled();

      setupSQL();
   }

   /**
    * Create "?, ?, ?, ?, ?, ?, ?, ?, ?, ?" <br>
    * for "9, 12, 20, 21, 22, 24, 32, 1141, 841, 641"
    *
    * @param allSQLParameters
    *
    * @return
    */
   private StringBuilder createOR_Parameters(final List<Object> allSQLParameters) {

      final StringBuilder sb = new StringBuilder();

      final long[] allTagIDs = TourTagFilterManager.getSelectedProfile().tagFilterIds.toArray();

      for (int paramIndex = 0; paramIndex < allTagIDs.length; paramIndex++) {

         final long tagId = allTagIDs[paramIndex];

         if (paramIndex == 0) {
            sb.append(PARAMETER_FIRST);
         } else {
            sb.append(PARAMETER_FOLLOWING);
         }

         allSQLParameters.add(tagId);
      }

      return sb;
   }

   /**
    * @return Returns a SQL WHERE AND statement
    */
   public String getSql() {

      return _sqlData.getSqlString();
   }

   /**
    * Sets the SQL tag parameters when necessary
    *
    * @param prepStmt
    * @param paramIndex_From
    *
    * @return Returns the next parameter index which can be used for setting the next parameter
    *
    * @throws SQLException
    */
   public int setParameters(final PreparedStatement prepStmt, final int paramIndex_From) throws SQLException {

      int paramIndex = paramIndex_From;

      // set sql tag parameters
      if (_isEnabled) {

         _sqlData.setParameters(prepStmt, paramIndex);

         paramIndex = _sqlData.getLastParameterIndex();
      }

      return paramIndex;
   }

   private void setupSQL() {

      String sql = UI.EMPTY_STRING;

      final List<Object> allSQLParameters = new ArrayList<>();

      if (_isEnabled) {

         if (TourTagFilterManager.isOrOperator()) {

            // combine tags with OR

            final StringBuilder sqlParameterString = createOR_Parameters(allSQLParameters);

            /* require tour to have at least one of these tags (index-friendly) */
            sql = UI.EMPTY_STRING

                  + "AND EXISTS" + NL //                                                     //$NON-NLS-1$
                  + "(" + NL //                                                              //$NON-NLS-1$
                  + "  SELECT 1" + NL //                                                     //$NON-NLS-1$
                  + "  FROM TOURDATA_TOURTAG AS tt" + NL //                                  //$NON-NLS-1$
                  + "  WHERE tt.TOURDATA_TOURID = TourData.tourID" + NL //                   //$NON-NLS-1$
                  + "    AND tt.TOURTAG_TAGID IN (" + sqlParameterString + ")" + NL //       //$NON-NLS-1$ //$NON-NLS-2$
                  + ")" + NL //                                                              //$NON-NLS-1$
            ;

         } else {

            // combine tags with AND

            // AND EXISTS (SELECT 1 FROM TOURDATA_TOURTAG AS tt WHERE tt.TOURDATA_TOURID = TourData.tourId AND tt.TOURTAG_TAGID = 9)
            // AND EXISTS (SELECT 1 FROM TOURDATA_TOURTAG AS tt WHERE tt.TOURDATA_TOURID = TourData.tourId AND tt.TOURTAG_TAGID = 12    )
            // AND EXISTS (SELECT 1 FROM TOURDATA_TOURTAG AS tt WHERE tt.TOURDATA_TOURID = TourData.tourId AND tt.TOURTAG_TAGID = 20    )

            final long[] allTagIDs = TourTagFilterManager.getSelectedProfile().tagFilterIds.toArray();

            final StringBuilder sb = new StringBuilder();

            for (final long tagID : allTagIDs) {

               sb.append("AND EXISTS (SELECT 1 FROM TOURDATA_TOURTAG AS tt WHERE tt.TOURDATA_TOURID = TourData.tourId AND tt.TOURTAG_TAGID = ?)" //$NON-NLS-1$
                     + NL);

               allSQLParameters.add(tagID);
            }

            sql = sb.toString();
         }
      }

      _sqlData = new SQLData(sql, allSQLParameters);
   }
}
