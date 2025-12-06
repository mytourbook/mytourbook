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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.common.UI;
import net.tourbook.data.Equipment;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_EquipRoot extends TVIEquipmentView_Item {

   /**
    * @param equipmentViewer
    */
   public TVIEquipmentView_EquipRoot(final TreeViewer equipmentViewer) {

      super(equipmentViewer);
   }

   @Override
   @SuppressWarnings("unchecked")
   protected void fetchChildren() {

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
          * Create equipment tree items
          */
         for (final Equipment equipment : allEquipments) {

            final TVIEquipmentView_Equipment equipmentItem = new TVIEquipmentView_Equipment(equipmentViewer, equipment);

            addChild(equipmentItem);
         }
      }
      em.close();
   }

}
