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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tourbook.Messages;
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

   private class ActionEquipment extends Action {

      private final Equipment __equipment;

      public ActionEquipment(final Equipment equipment) {

         super(equipment.getName(), AS_CHECK_BOX);

         __equipment = equipment;
      }

      @Override
      public void run() {

         EquipmentManager.equipment_Remove(

               __equipment,
               _equipmentMenuManager.getTourProvider(),

               _equipmentMenuManager.isSaveTour(),
               _equipmentMenuManager.isCheckTourEditor());
      }
   }

   protected ActionRemoveEquipment_SubMenu(final EquipmentMenuManager equipmentMenuManager) {

      super(Messages.Action_Equipment_RemoveEquipment, AS_DROP_DOWN_MENU);

      _equipmentMenuManager = equipmentMenuManager;
   }

   @Override
   public void enableActions() {

   }

   @Override
   public void fillMenu(final Menu menu) {

      // get all equipment from all tours
      final List<TourData> allSelectedTours = _equipmentMenuManager.getTourProvider().getSelectedTours();

      final Set<Equipment> allUsedEquipment = new HashSet<>();

      for (final TourData tourData : allSelectedTours) {
         allUsedEquipment.addAll(tourData.getEquipment());
      }

      // sort equipment
      final List<Equipment> allUsedAndSortedEquipment = new ArrayList<>();
      allUsedAndSortedEquipment.addAll(allUsedEquipment);
      Collections.sort(allUsedAndSortedEquipment);

      for (final Equipment equipment : allUsedAndSortedEquipment) {

         final ActionEquipment action = new ActionEquipment(equipment);
         addActionToMenu(action);

         // make the equipment more visible
         action.setChecked(true);
      }

      setEnabled(allUsedEquipment.size() > 0);

//      System.out.println(UI.timeStamp() + " ActionRemoveEquipment_SubMenu " + isEnabled());
//// TODO remove SYSTEM.OUT.PRINTLN
   }

}
