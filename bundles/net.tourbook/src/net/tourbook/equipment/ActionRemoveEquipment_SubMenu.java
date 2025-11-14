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

import java.util.List;

import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.Equipment;
import net.tourbook.data.TourData;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Menu;

/**
 * Add equipments from the selected tours
 */
public class ActionRemoveEquipment_SubMenu extends SubMenu {

   private EquipmentMenuManager _equipmentMenuManager;

   private List<TourData>       _allSelectedTours;

   private class ActionEquipment extends Action {

      private final Equipment __equipment;

      public ActionEquipment(final Equipment equipment) {

         super(equipment.getName(), AS_CHECK_BOX);

         __equipment = equipment;
      }

      @Override
      public void run() {

//         setTourTag(isChecked(), __equipment);
      }
   }

   protected ActionRemoveEquipment_SubMenu(final EquipmentMenuManager equipmentMenuManager) {

      super("&Remove Equipment", AS_DROP_DOWN_MENU);

      _equipmentMenuManager = equipmentMenuManager;
   }

   @Override
   public void enableActions() {

   }

   @Override
   public void fillMenu(final Menu menu) {

      final List<Equipment> allEquipments = EquipmentManager.getAllEquipment_Name();

      for (final Equipment equipment : allEquipments) {

         addActionToMenu(new ActionEquipment(equipment));
      }

   }

}
