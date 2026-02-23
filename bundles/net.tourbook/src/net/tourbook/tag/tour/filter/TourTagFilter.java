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

import java.util.ArrayList;
import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.common.util.SQLData;
import net.tourbook.database.TourDatabase;

/**
 * The SQL EXISTS statement is from the github KI copilot optimizations
 */
public class TourTagFilter {

   private static final char   NL                  = UI.NEW_LINE;

   private static final String PARAMETER_FIRST     = " ?";       //$NON-NLS-1$
   private static final String PARAMETER_FOLLOWING = ", ?";      //$NON-NLS-1$

   private SQLData             _sqlData;

   public TourTagFilter() {

      _sqlData = createSQL();
   }

   private SQLData createSQL() {

      String sql = UI.EMPTY_STRING;

      final List<Object> allSQLParameters = new ArrayList<>();

      if (TourTagFilterManager.isFilterEnabled()) {

         final TourTagFilterProfile selectedProfile = TourTagFilterManager.getSelectedProfile();

         final String joinTable = TourDatabase.JOINTABLE__TOURDATA__TOURTAG;

         if (selectedProfile.isOrOperator) {

            // combine tags with OR

            final StringBuilder sqlParameters = createSQL_OR_Parameters(allSQLParameters);

            /* require tour to have at least one of these tags (index-friendly) */
            sql = UI.EMPTY_STRING

                  + "AND EXISTS" + NL //                                                     //$NON-NLS-1$
                  + "(" + NL //                                                              //$NON-NLS-1$
                  + "  SELECT 1" + NL //                                                     //$NON-NLS-1$
                  + "  FROM " + joinTable + " AS tt" + NL //                                 //$NON-NLS-1$ //$NON-NLS-2$
                  + "  WHERE tt.TOURDATA_TOURID = TourData.tourID" + NL //                   //$NON-NLS-1$
                  + "    AND tt.TOURTAG_TAGID IN (" + sqlParameters + ")" + NL //            //$NON-NLS-1$ //$NON-NLS-2$
                  + ")" + NL //                                                              //$NON-NLS-1$
            ;

         } else {

            // combine tags with AND

            // AND EXISTS (SELECT 1 FROM TOURDATA_TOURTAG AS tt WHERE tt.TOURDATA_TOURID = TourData.tourId AND tt.TOURTAG_TAGID = 9)
            // AND EXISTS (SELECT 1 FROM TOURDATA_TOURTAG AS tt WHERE tt.TOURDATA_TOURID = TourData.tourId AND tt.TOURTAG_TAGID = 12    )
            // AND EXISTS (SELECT 1 FROM TOURDATA_TOURTAG AS tt WHERE tt.TOURDATA_TOURID = TourData.tourId AND tt.TOURTAG_TAGID = 20    )

            final long[] allTagIDs = selectedProfile.tagFilterIds.toArray();

            final StringBuilder sb = new StringBuilder();

            for (final long tagID : allTagIDs) {

               sb.append("AND EXISTS (SELECT 1 FROM " + joinTable + " AS tt WHERE tt.TOURDATA_TOURID = TourData.tourId AND tt.TOURTAG_TAGID = ?)" //$NON-NLS-1$ //$NON-NLS-2$
                     + NL);

               allSQLParameters.add(tagID);
            }

            sql = sb.toString();
         }
      }

      return new SQLData(sql, allSQLParameters);
   }

   /**
    * Create "?, ?, ?, ?, ?, ?, ?, ?, ?, ?" <br>
    * for "9, 12, 20, 21, 22, 24, 32, 1141, 841, 641"
    *
    * @param allSQLParameters
    *
    * @return
    */
   private StringBuilder createSQL_OR_Parameters(final List<Object> allSQLParameters) {

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

   public SQLData getSqlData() {

      return _sqlData;
   }
}
