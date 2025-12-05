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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;
import net.tourbook.data.TourData;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Menu;

/**
 * Add equipment into the selected tours
 */
public class ActionAddEquipment_SubMenu extends SubMenu {

   private EquipmentMenuManager    _equipmentMenuManager;

   private ActionShowEquipmentView _actionManageEquipment;

   private class ActionEquipment extends Action {

      private final Equipment __equipment;

      public ActionEquipment(final Equipment equipment) {

         super(equipment.getName(), AS_CHECK_BOX);

         __equipment = equipment;
      }

      @Override
      public void run() {

         EquipmentManager.equipment_Add(

               __equipment,
               _equipmentMenuManager.getTourProvider(),

               _equipmentMenuManager.isSaveTour(),
               _equipmentMenuManager.isCheckTourEditor());

         _equipmentMenuManager.updateRecentEquipment(__equipment);
      }
   }

   private class ActionShowEquipmentView extends Action {

      public ActionShowEquipmentView() {

         super("Manage E&quipment", AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.Equipment));
      }

      @Override
      public void run() {

         Util.showView(EquipmentView.ID, true);
      }
   }

   protected ActionAddEquipment_SubMenu(final EquipmentMenuManager equipmentMenuManager) {

      super(Messages.Action_Equipment_AddEquipment, AS_DROP_DOWN_MENU);

      _equipmentMenuManager = equipmentMenuManager;

      _actionManageEquipment = new ActionShowEquipmentView();
   }

   @Override
   public void enableActions() {

   }

   @Override
   public void fillMenu(final Menu menu) {

      // get all equipment from all tours
      final Set<Long> allUsedEquipmentIDs = new HashSet<>();
      final List<TourData> allSelectedTours = _equipmentMenuManager.getTourProvider().getSelectedTours();

      for (final TourData tourData : allSelectedTours) {

         final Set<Equipment> allEquipment = tourData.getEquipment();

         for (final Equipment equipment : allEquipment) {
            allUsedEquipmentIDs.add(equipment.getEquipmentId());
         }
      }

      final List<Equipment> allEquipments = EquipmentManager.getAllEquipment_Name();

      for (final Equipment equipment : allEquipments) {

         final ActionEquipment action = new ActionEquipment(equipment);

         final boolean isEquipmentAlreadySet = allUsedEquipmentIDs.contains(equipment.getEquipmentId());

         if (isEquipmentAlreadySet) {
            action.setEnabled(false);
         }

         addActionToMenu(action);
      }

      addSeparatorToMenu();

      addActionToMenu(_actionManageEquipment);
   }
}
