/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.Equipment;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Equipment extends TVIEquipmentView_Item {

   private Equipment _equipment;

   public TVIEquipmentView_Equipment(final TreeViewer equipViewer, final Equipment equipment) {

      super(equipViewer);

      _equipment = equipment;
   }

   @Override
   protected void fetchChildren() {

      final ArrayList<TreeViewerItem> children_Tours = readChildren_Tours(UI.EMPTY_STRING);

      setChildren(children_Tours);
   }

   public Equipment getEquipment() {
      return _equipment;
   }

   /**
    * Get all tours for the equipment Id of this tree item
    */
   private ArrayList<TreeViewerItem> readChildren_Tours(final String whereClause) {

      final ArrayList<TreeViewerItem> children = new ArrayList<>();

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final SQLFilter sqlFilter = new SQLFilter();

         final String sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                             //$NON-NLS-1$

               + " TourData.tourId," + NL //                               1  //$NON-NLS-1$
               + " JTdataTequipment2.Equipment_EquipmentId," + NL //       2  //$NON-NLS-1$

               + TVIEquipmentView_Tour.SQL_TOUR_COLUMNS + NL //            3

               + " FROM " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " JTdataTequipment1" + NL //               //$NON-NLS-1$ //$NON-NLS-2$

               // get all tours for current equipment
               + " LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData" + NL //                            //$NON-NLS-1$ //$NON-NLS-2$
               + " ON JTdataTequipment1.TourData_tourId = TourData.tourId " + NL //                                  //$NON-NLS-1$

               // get all equipment id's for one tour
               + " LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " JTdataTequipment2" + NL //    //$NON-NLS-1$ //$NON-NLS-2$
               + " ON TourData.tourID = JTdataTequipment2.TourData_tourId" + NL //                                   //$NON-NLS-1$

               + " WHERE JTdataTequipment1.Equipment_EquipmentId = ?" + NL //                                        //$NON-NLS-1$
               + whereClause + NL
               + sqlFilter.getWhereClause() + NL

               + " ORDER BY startYear, startMonth, startDay, startHour, startMinute" + NL //                   //$NON-NLS-1$
         ;

         long previousTourId = -1;
         TVIEquipmentView_Tour tourItem = null;

         final PreparedStatement statement = conn.prepareStatement(sql);
         statement.setLong(1, _equipment.getEquipmentId());
         sqlFilter.setParameters(statement, 2);

         final ResultSet result = statement.executeQuery();
         while (result.next()) {

            final long tourId = result.getLong(1);
            final Object resultEquipmentID = result.getObject(2);

            if (tourId == previousTourId) {

               // get equipment from outer join

               if (resultEquipmentID instanceof Long) {
                  tourItem.allEquipmentIDs.add((Long) resultEquipmentID);
               }

            } else {

               tourItem = new TVIEquipmentView_Tour(this, getEquipmentViewer());
               children.add(tourItem);

               tourItem.tourId = tourId;
               tourItem.getTourColumnData(result, resultEquipmentID, 3);

//               tourItem.firstColumn = tourItem.tourDate.format(TimeTools.Formatter_Date_S);
//
//               if (UI.IS_SCRAMBLE_DATA) {
//                  tourItem.firstColumn = UI.scrambleText(tourItem.firstColumn);
//               }
            }

            previousTourId = tourId;
         }

      } catch (final SQLException e) {
         net.tourbook.ui.UI.showSQLException(e);
      }

      return children;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVIEquipmentView_Equipment" + NL //       //$NON-NLS-1$

            + " _equipment = " + _equipment + NL //      //$NON-NLS-1$
      ;
   }

}
