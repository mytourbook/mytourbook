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
package net.tourbook.equipment;

import java.util.ArrayList;
import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.common.util.SQLData;
import net.tourbook.database.TourDatabase;
import net.tourbook.equipment.tour.filter.EquipmentFilterOperator;
import net.tourbook.equipment.tour.filter.EquipmentFilterType;
import net.tourbook.equipment.tour.filter.TourEquipmentFilterManager;
import net.tourbook.equipment.tour.filter.TourEquipmentFilterProfile;

public class EquipmentPartFilter_NOT {

   private static final char   NL                  = UI.NEW_LINE;

   private static final String PARAMETER_FIRST     = " ?";       //$NON-NLS-1$
   private static final String PARAMETER_FOLLOWING = ", ?";      //$NON-NLS-1$

   private SQLData             _sqlData;

   public EquipmentPartFilter_NOT() {

      _sqlData = createSQL();
   }

   private SQLData createSQL() {

      String sql = UI.EMPTY_STRING;

      final List<Object> allSQLParameters = new ArrayList<>();

      if (TourEquipmentFilterManager.isFilterEnabled()) {

         final TourEquipmentFilterProfile selectedProfile = TourEquipmentFilterManager.getSelectedProfile();

         if (EquipmentFilterType.PART.equals(selectedProfile.filterType) == false) {

            // equipment filter

            /**
             * The equipment filter is implemented differently in {@link EquipmentFilter}
             */

         } else {

            // part filter

            final String tEqPart = TourDatabase.TABLE_EQUIPMENT_PART;
            final String jTdEq = TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT;

            final EquipmentFilterOperator filterOperator = selectedProfile.filterOperator;
            final boolean isNotOperator = filterOperator.equals(EquipmentFilterOperator.NOT);

            if (isNotOperator) {

               // combine parts with NOT

               final StringBuilder sqlParameters = createSQL_OR_Parameters(allSQLParameters);

               sql = UI.EMPTY_STRING

                     + "   AND NOT EXISTS (" + NL //                                                        //$NON-NLS-1$

                     + "      SELECT 1" + NL //                                                             //$NON-NLS-1$
                     + "      FROM " + jTdEq + " AS jTdEqOR" + NL //                                   //$NON-NLS-1$

                     + "      JOIN " + tEqPart + " AS eqPart " + NL //                                        //$NON-NLS-1$
                     + "			ON eqPart.EQUIPMENT_EQUIPMENTID = jTdEqOR.EQUIPMENT_EQUIPMENTID" + NL //   //$NON-NLS-1$

                     + "      WHERE jTdEqOR.TOURDATA_TOURID = TourData.TOURID" + NL //                      //$NON-NLS-1$
                     + "         AND eqPart.ISCOLLATE = TRUE" + NL //                                       //$NON-NLS-1$
                     + "         AND eqPart.PARTID IN (" + sqlParameters + ")" + NL //                      //$NON-NLS-1$
                     + "         AND TourData.TourStartTime >= eqPart.dateCollateFrom" + NL //              //$NON-NLS-1$
                     + "         AND TourData.TourStartTime < eqPart.dateCollateUntil" + NL //              //$NON-NLS-1$
                     + "   )" + NL //                                                                       //$NON-NLS-1$
               ;
            }
         }
      }

      final String sqlFinal = UI.EMPTY_STRING

            + "-- part filter NOT - START" + NL //$NON-NLS-1$
            + sql
            + "-- part filter NOT - END" + NL //$NON-NLS-1$
      ;

      return new SQLData(sqlFinal, allSQLParameters);
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

      final long[] allEquipmentIDs = TourEquipmentFilterManager.getSelectedProfile().allAssetFilterIDs.toArray();

      for (int paramIndex = 0; paramIndex < allEquipmentIDs.length; paramIndex++) {

         final long equipmentID = allEquipmentIDs[paramIndex];

         if (paramIndex == 0) {
            sb.append(PARAMETER_FIRST);
         } else {
            sb.append(PARAMETER_FOLLOWING);
         }

         allSQLParameters.add(equipmentID);
      }

      return sb;
   }

   public SQLData getSqlData() {

      return _sqlData;
   }
}
