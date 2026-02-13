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
import net.tourbook.equipment.tour.filter.EquipmentFilterType;
import net.tourbook.equipment.tour.filter.TourEquipmentFilterManager;
import net.tourbook.equipment.tour.filter.TourEquipmentFilterProfile;

public class EquipmentPartFilter {

   private static final char   NL                  = UI.NEW_LINE;

   private static final String PARAMETER_FIRST     = " ?";       //$NON-NLS-1$
   private static final String PARAMETER_FOLLOWING = ", ?";      //$NON-NLS-1$

   private SQLData             _sqlData;

   public EquipmentPartFilter() {

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

            if (selectedProfile.isOrOperator) {

               // combine parts with OR

               final StringBuilder sqlParameters = createSQL_OR_Parameters(allSQLParameters);

               /* require tour to have at least one of these equipment (index-friendly) */
               sql = UI.EMPTY_STRING

                     + "JOIN " + jTdEq + " AS jTdEqOR"
                     + "   ON jTdEqOR.TOURDATA_TOURID = TourData.TOURID" + NL //                         //$NON-NLS-1$

                     + "JOIN " + tEqPart + " AS partOR" //                 //$NON-NLS-1$
                     + "   ON partOR.EQUIPMENT_EQUIPMENTID = jTdEqOR.EQUIPMENT_EQUIPMENTID" + NL //      //$NON-NLS-1$

                     + "   AND partOR.ISCOLLATE = TRUE" + NL //                                          //$NON-NLS-1$
                     + "   AND partOR.PARTID IN (" + sqlParameters + ")" + NL //                         //$NON-NLS-1$ //$NON-NLS-2$

                     + "   AND TourData.TourStartTime >= partOR.dateFrom" + NL //                        //$NON-NLS-1$
                     + "   AND TourData.TourStartTime < partOR.dateUntil" + NL //                        //$NON-NLS-1$
               ;

            } else {

               // combine parts with AND

               final StringBuilder allSqlPartIDParameters = createSQL_OR_Parameters(allSQLParameters);

               final long[] allPartIDs = TourEquipmentFilterManager.getSelectedProfile().allAssetFilterIDs.toArray();
               final int numParts = allPartIDs.length;

               // sequence is important when the parameters are set !!!
               allSQLParameters.add(numParts);

               final StringBuilder sbAllParts = new StringBuilder();

               for (int partIndex = 0; partIndex < allPartIDs.length; partIndex++) {

                  final long partID = allPartIDs[partIndex];

                  allSQLParameters.add(partID);

                  final String partName = "part_" + partIndex;

                  final String joinPart = UI.EMPTY_STRING

                        + "JOIN " + tEqPart + " AS " + partName + NL //                                           //$NON-NLS-1$
                        + "  ON " + partName + ".EQUIPMENT_EQUIPMENTID = jTdEq.EQUIPMENT_EQUIPMENTID" + NL //     //$NON-NLS-1$
                        + "    AND " + partName + ".PARTID = ?" + NL //                                           //$NON-NLS-1$
                        + "    AND " + partName + ".ISCOLLATE = TRUE" + NL //                                     //$NON-NLS-1$
                        + "    AND TourData.TourStartTime >= " + partName + ".dateFrom" + NL //                   //$NON-NLS-1$
                        + "    AND TourData.TourStartTime < " + partName + ".dateUntil" + NL //                   //$NON-NLS-1$
                  ;

                  sbAllParts.append(joinPart);
               }

               final String allJoinedParts = sbAllParts.toString();

               sql = UI.EMPTY_STRING

                     // require tour to have at least one of these equipment (index-friendly)

                     + "JOIN " + jTdEq + " jTdEq" + NL //                                                         //$NON-NLS-1$
                     + "   ON jTdEq.TOURDATA_TOURID = TourData.TOURID" + NL //                                    //$NON-NLS-1$

                     + "JOIN" + NL //                                                                             //$NON-NLS-1$
                     + "(" + NL //                                                                                //$NON-NLS-1$

                     // Pre-filter: equipment that has ALL n parts with ISCOLLATE=TRUE
                     + "   SELECT EQUIPMENT_EQUIPMENTID" + NL //                                                  //$NON-NLS-1$
                     + "   FROM " + tEqPart + NL //                                                               //$NON-NLS-1$
                     + "   WHERE ISCOLLATE = TRUE" + NL //                                                        //$NON-NLS-1$
                     + "     AND PARTID IN (" + allSqlPartIDParameters + ")" + NL //                              //$NON-NLS-1$
                     + "   GROUP BY EQUIPMENT_EQUIPMENTID" + NL //                                                //$NON-NLS-1$
                     + "   HAVING COUNT(DISTINCT PARTID) = ?" + NL //                                             //$NON-NLS-1$

                     + ") AS Eq_With_All_Parts" + NL //                                                           //$NON-NLS-1$
                     + "  ON Eq_With_All_Parts.EQUIPMENT_EQUIPMENTID = jTdEq.EQUIPMENT_EQUIPMENTID" + NL //       //$NON-NLS-1$

                     + allJoinedParts;
            }
         }
      }

      final String sqlFinal = UI.EMPTY_STRING

            + "-- part filter - START" + NL //$NON-NLS-1$
            + sql
            + "-- part filter - END" + NL //$NON-NLS-1$
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
