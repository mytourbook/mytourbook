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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Root extends TVIEquipmentView_Item {

   /**
    * @param equipmentViewer
    */
   public TVIEquipmentView_Root(final TreeViewer equipmentViewer) {

      super(equipmentViewer);

   }

   @Override
   @SuppressWarnings("unchecked")
   protected void fetchChildren() {

      final Map<Long, TVIEquipmentView_Equipment> allEquipmentItems = new HashMap<>();

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      {
         if (em == null) {
            return;
         }

         final Query query = em.createQuery(UI.EMPTY_STRING

               + "SELECT" + NL //                                                      //$NON-NLS-1$
               + " Equipment" + NL //                                                  //$NON-NLS-1$
               + " FROM " + Equipment.class.getSimpleName() + " AS Equipment" //       //$NON-NLS-1$ //$NON-NLS-2$
         );

         final TreeViewer equipmentViewer = getEquipmentViewer();
         final List<Equipment> allEquipments = query.getResultList();

         /*
          * Create all equipment top items
          */
         for (final Equipment equipment : allEquipments) {

            final TVIEquipmentView_Equipment equipmentItem = new TVIEquipmentView_Equipment(equipmentViewer, equipment);

            addChild(equipmentItem);

            allEquipmentItems.put(equipment.getEquipmentId(), equipmentItem);
         }
      }
      em.close();

      loadNumberOfTours(allEquipmentItems);
   }

   private void loadNumberOfTours(final Map<Long, TVIEquipmentView_Equipment> allEquipmentItems) {

      String sql = null;
      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                            //$NON-NLS-1$

               + "   j_TD_EQ.EQUIPMENT_EQUIPMENTID," + NL //                              1  //$NON-NLS-1$
               + "   COUNT(*)" + NL //                                                    2  //$NON-NLS-1$

               + "FROM EQUIPMENT" + NL //                                                    //$NON-NLS-1$

               + "JOIN TOURDATA_EQUIPMENT AS j_TD_EQ" + NL //                                //$NON-NLS-1$
               + "   ON j_TD_EQ.EQUIPMENT_EQUIPMENTID = EQUIPMENT.EQUIPMENTID" + NL //       //$NON-NLS-1$

               + "GROUP BY " + NL //                                                         //$NON-NLS-1$
               + "   j_TD_EQ.EQUIPMENT_EQUIPMENTID" + NL //                                  //$NON-NLS-1$
         ;

         statement = conn.prepareStatement(sql);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final long equipmentID = result.getLong(1);
            final int numTours = result.getInt(2);

            final TVIEquipmentView_Equipment equipmentItem = allEquipmentItems.get(equipmentID);

            if (equipmentItem != null) {

               equipmentItem.numTours = numTours;
            }
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      } finally {
         Util.closeSql(statement);
      }
   }
}
