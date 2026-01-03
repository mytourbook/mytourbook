/*******************************************************************************
 * Copyright (C) 2025, 2026 Wolfgang Schramm and Contributors
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
import net.tourbook.data.EquipmentPart;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Part extends TVIEquipmentView_Item {

   private Equipment     _equipment;
   private EquipmentPart _part;

   private long          _partID;

   public TVIEquipmentView_Part(final TVIEquipmentView_Equipment tviParent,
                                final EquipmentPart equipmentPart,
                                final TreeViewer treeViewer) {

      super(treeViewer);

      setParentItem(tviParent);

      _equipment = tviParent.getEquipment();

      _part = equipmentPart;
      _partID = equipmentPart.getPartId();
   }

   /**
    * Get all tours for this part
    */
   @Override
   protected void fetchChildren() {

      final ArrayList<TreeViewerItem> allTourItems = new ArrayList<>();

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final SQLFilter sqlFilter = new SQLFilter();

         final String sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                               //$NON-NLS-1$

               + TVIEquipmentView_Tour.SQL_TOUR_COLUMNS

               + "FROM equipmentpart part" + NL //                                              //$NON-NLS-1$

               + "JOIN tourdata_equipment AS j_td_eq" + NL //                                   //$NON-NLS-1$
               + "   ON j_td_eq.equipment_equipmentid = part.equipment_equipmentid" + NL //     //$NON-NLS-1$

               // The alias "TourData" is needed that the tour filter is working
               + "JOIN tourdata AS TourData" + NL //                                            //$NON-NLS-1$
               + "   ON TourData.tourID = j_td_eq.tourdata_tourID" + NL //                      //$NON-NLS-1$
               + "   AND TourData.tourstarttime >= part.\"DATE\"" + NL //                       //$NON-NLS-1$
               + "   AND TourData.tourstarttime <  part.dateUntil" + NL //                      //$NON-NLS-1$

               + "WHERE part.isCollate = TRUE" + NL //                                          //$NON-NLS-1$
               + "   AND part.partID = ?" + NL //                                               //$NON-NLS-1$

               + sqlFilter.getWhereClause() + NL

               + "ORDER BY TourData.tourstarttime" + NL //                                      //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);

         statement.setLong(1, _partID);
         sqlFilter.setParameters(statement, 2);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final TVIEquipmentView_Tour tourItem = new TVIEquipmentView_Tour(this, getEquipmentViewer());

            allTourItems.add(tourItem);

            tourItem.readColumnValues_Tour(result);

            if (UI.IS_SCRAMBLE_DATA) {
               tourItem.firstColumn = UI.scrambleText(tourItem.firstColumn);
            }
         }

      } catch (final SQLException e) {

         net.tourbook.ui.UI.showSQLException(e);
      }

      setChildren(allTourItems);
   }

   public Equipment getEquipment() {
      return _equipment;
   }

   public EquipmentPart getPart() {
      return _part;
   }

   public long getPartID() {
      return _partID;
   }

   public String getTourValuesKey() {

      return getTourValuesKey(_equipment.getEquipmentId(), _partID, _part.getType());
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVIEquipmentView_Part" + NL

            + " _part   = " + _part + NL

      ;
   }

}
