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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.common.UI;
import net.tourbook.data.Equipment;
import net.tourbook.data.EquipmentPart;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Root extends TVIEquipmentView_Item {

   /**
    * @param equipmentViewer
    * @param isShowTours
    *           When <code>true</code> then the years/months and tours are displayed, otherwise
    *           then just the equipment structure is displayed, e.g. in the equipment tour filter
    */
   public TVIEquipmentView_Root(final TreeViewer equipmentViewer, final EquipmentViewerType equipmentType) {

      super(equipmentViewer, equipmentType);
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

         final boolean isFilterEnabled = EquipmentManager.isEquipmentFilterEnabled();
         final int equipmentFilter_Retired = EquipmentManager.getEquipmentFilter_Retired();

         final boolean eqFilter_IsShowActive = equipmentFilter_Retired == EquipmentManager.FILTER_RETIRED_IS_ACTIVE;
         final boolean eqFilter_IsShowRetired = equipmentFilter_Retired == EquipmentManager.FILTER_RETIRED_IS_RETIRED;

         final boolean useFilter = isFilterEnabled && (eqFilter_IsShowRetired || eqFilter_IsShowActive);

         final String sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                      //$NON-NLS-1$
               + " Equipment" + NL //                                                  //$NON-NLS-1$
               + " FROM " + Equipment.class.getSimpleName() + " AS Equipment" + NL//   //$NON-NLS-1$ //$NON-NLS-2$
         ;

         final Query query = em.createQuery(sql);

         final TreeViewer equipmentViewer = getEquipmentViewer();
         final List<Equipment> allEquipments = query.getResultList();

         /*
          * Create all equipment top items
          */
         for (final Equipment equipment : allEquipments) {

            if (useFilter) {

               final boolean isEquipmentCollate = equipment.isCollate();
               final boolean isEquipmentRetired = equipment.isRetired();

               if (isEquipmentCollate) {

                  // collated equipment

                  if (eqFilter_IsShowRetired && isEquipmentRetired) {

                     // display this equipment

                  } else if (eqFilter_IsShowActive && isEquipmentRetired == false) {

                     // display this equipment

                  } else {

                     // filter out equipment

                     continue;
                  }

               } else {

                  // collated parts

                  final Set<EquipmentPart> allParts = equipment.getParts();

                  int numRetiredParts = 0;

                  for (final EquipmentPart part : allParts) {
                     if (part.isRetired()) {
                        numRetiredParts++;
                     }
                  }

                  final int numParts = allParts.size();
                  final int numActiveParts = numParts - numRetiredParts;

                  if (eqFilter_IsShowRetired) {

                     if (isEquipmentRetired) {

                        // display this equipment

                     } else {

                        // equipment is active -> check parts

                        if (numRetiredParts > 0) {

                           // display this equipment

                        } else {

                           // no retired parts -> hide equipment

                           continue;
                        }
                     }

                  } else if (eqFilter_IsShowActive) {

                     if (isEquipmentRetired) {

                        // check parts

                        if (numActiveParts > 0) {

                           // display this equipment

                        } else {

                           // no active parts -> hide equipment

                           continue;
                        }

                     } else {

                        // display this equipment
                     }
                  }
               }
            }

            final TVIEquipmentView_Equipment equipmentItem = new TVIEquipmentView_Equipment(

                  equipmentViewer,
                  equipment,
                  getViewerType());

            addChild(equipmentItem);

            allEquipmentItems.put(equipment.getEquipmentId(), equipmentItem);
         }
      }
      em.close();

      loadSummarizedValues_Equipment(allEquipmentItems);
   }
}
