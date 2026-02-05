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
import net.tourbook.equipment.tour.filter.TourEquipmentFilterManager;
import net.tourbook.equipment.tour.filter.TourEquipmentFilterProfile;

public class EquipmentFilter {

   private static final char   NL                  = UI.NEW_LINE;

   private static final String PARAMETER_FIRST     = " ?";       //$NON-NLS-1$
   private static final String PARAMETER_FOLLOWING = ", ?";      //$NON-NLS-1$

   private SQLData             _sqlData;

   public EquipmentFilter() {

      _sqlData = createSQL();
   }

   private SQLData createSQL() {

      String sql = UI.EMPTY_STRING;

      final List<Object> allSQLParameters = new ArrayList<>();

      if (TourEquipmentFilterManager.isFilterEnabled()) {

         final TourEquipmentFilterProfile selectedProfile = TourEquipmentFilterManager.getSelectedProfile();

         final String joinTable = TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT;

         if (selectedProfile.isOrOperator) {

            // combine equipment with OR

            final StringBuilder sqlParameters = createSQL_OR_Parameters(allSQLParameters);

            /* require tour to have at least one of these equipment (index-friendly) */
            sql = UI.EMPTY_STRING

                  + "AND EXISTS" + NL //                                                     //$NON-NLS-1$
                  + "(" + NL //                                                              //$NON-NLS-1$
                  + "  SELECT 1" + NL //                                                     //$NON-NLS-1$
                  + "  FROM " + joinTable + " AS tt" + NL //                                 //$NON-NLS-1$ //$NON-NLS-2$
                  + "  WHERE tt.TOURDATA_TOURID = TourData.tourID" + NL //                   //$NON-NLS-1$
                  + "    AND tt.Equipment_EquipmentId IN (" + sqlParameters + ")" + NL //            //$NON-NLS-1$ //$NON-NLS-2$
                  + ")" + NL //                                                              //$NON-NLS-1$
            ;

         } else {

            // combine equipment with AND

            // AND EXISTS (SELECT 1 FROM TOURDATA_Equipment AS tt WHERE tt.TOURDATA_TOURID = TourData.tourId AND tt.Equipment_EquipmentId = 9)
            // AND EXISTS (SELECT 1 FROM TOURDATA_Equipment AS tt WHERE tt.TOURDATA_TOURID = TourData.tourId AND tt.Equipment_EquipmentId = 12    )
            // AND EXISTS (SELECT 1 FROM TOURDATA_Equipment AS tt WHERE tt.TOURDATA_TOURID = TourData.tourId AND tt.Equipment_EquipmentId = 20    )

            final long[] allEquipmentIDs = selectedProfile.allAssetFilterIDs.toArray();

            final StringBuilder sb = new StringBuilder();

            for (final long equipmentID : allEquipmentIDs) {

               sb.append(UI.EMPTY_STRING

                     + "AND EXISTS " //                                                //$NON-NLS-1$
                     + "(" //                                                          //$NON-NLS-1$
                     + "   SELECT 1" //                                                //$NON-NLS-1$
                     + "   FROM " + joinTable + " AS tt " //                           //$NON-NLS-1$ //$NON-NLS-2$
                     + "   WHERE tt.TOURDATA_TOURID = TourData.tourId " //             //$NON-NLS-1$
                     + "     AND tt.Equipment_EquipmentId = ?" //                      //$NON-NLS-1$
                     + ")" + NL //                                                     //$NON-NLS-1$
               );

               allSQLParameters.add(equipmentID);
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
