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

            final String joinTable = TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT;

            if (selectedProfile.isOrOperator) {

               // combine parts with OR

               final StringBuilder sqlParameters = createSQL_OR_Parameters(allSQLParameters);

               /* require tour to have at least one of these equipment (index-friendly) */
               sql = UI.EMPTY_STRING

                     + "JOIN " + joinTable + " AS jTdEq"
                     + "   ON jTdEq.TOURDATA_TOURID = TourData.TOURID" + NL //                     //$NON-NLS-1$

                     + "JOIN " + TourDatabase.TABLE_EQUIPMENT_PART + " AS part" //                 //$NON-NLS-1$
                     + "   ON part.EQUIPMENT_EQUIPMENTID = jTdEq.EQUIPMENT_EQUIPMENTID" + NL //    //$NON-NLS-1$

                     + "   AND part.ISCOLLATE = TRUE" + NL //                                      //$NON-NLS-1$
                     + "   AND part.PARTID IN (" + sqlParameters + ")" + NL //                     //$NON-NLS-1$ //$NON-NLS-2$

                     + "   AND TourData.TourStartTime >= part.dateFrom" + NL //                    //$NON-NLS-1$
                     + "   AND TourData.TourStartTime < part.dateUntil" + NL //                    //$NON-NLS-1$
               ;

            } else {

               // combine parts with AND

//               TBD
            }
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
