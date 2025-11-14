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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tourbook.data.Equipment;
import net.tourbook.data.TourData;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;

public class EquipmentMenuManager {

   private ITourProvider                 _tourProvider;

   private ActionAddEquipment_SubMenu    _actionAddEquipment;
   private ActionRemoveEquipment_SubMenu _actionRemoveEquipment;

   public EquipmentMenuManager(final ITourProvider tourProvider) {

      setTourProvider(tourProvider);

      createActions();

   }

   private void createActions() {

      _actionAddEquipment = new ActionAddEquipment_SubMenu(this);
      _actionRemoveEquipment = new ActionRemoveEquipment_SubMenu(this);
   }

   private void enableActions() {

      final List<Equipment> allAvailableEquipments = EquipmentManager.getAllEquipment_Name();
      final Map<Long, Equipment> allUseEquipments = getAllUseEquipments();

      final boolean isEquipmentAvailable = allAvailableEquipments.size() > 0;
      final boolean isEquipmentInTour = allUseEquipments.size() > 0;

      _actionAddEquipment.setEnabled(isEquipmentAvailable);
      _actionRemoveEquipment.setEnabled(isEquipmentInTour);
   }

   /**
    * Add all tour equipment actions
    *
    * @param menuMgr
    */
   public void fillEquipmentMenu(final IMenuManager menuMgr) {

      menuMgr.add(new Separator());
      {
         menuMgr.add(_actionAddEquipment);
         menuMgr.add(_actionRemoveEquipment);
      }

      enableActions();
   }

   private Map<Long, Equipment> getAllUseEquipments() {

      final Map<Long, Equipment> allUsedEquipment = new HashMap<>();

      final List<TourData> allSelectedTours = _tourProvider.getSelectedTours();
      for (final TourData tourData : allSelectedTours) {

         final Set<Equipment> allTourEquipments = tourData.getTourEquipment();

         for (final Equipment tourEquipment : allTourEquipments) {
            allUsedEquipment.put(tourEquipment.getEquipmentId(), tourEquipment);
         }
      }

      return allUsedEquipment;
   }

   public ITourProvider getTourProvider() {
      return _tourProvider;
   }

   public void setTourProvider(final ITourProvider tourProvider) {
      _tourProvider = tourProvider;
   }

}
