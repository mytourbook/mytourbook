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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tourbook.data.Equipment;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProvider2;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class EquipmentMenuManager {

   private ITourProvider                 _tourProvider;

   private boolean                       _isCheckTourEditor;
   private boolean                       _isSaveTour;

   private ActionAddEquipment_SubMenu    _actionAddEquipment;
   private ActionRemoveEquipment_SubMenu _actionRemoveEquipment;
   private ActionRemoveAllEquipment      _actionRemoveAllEquipment;

   /**
    * Removes all equipment
    */
   public class ActionRemoveAllEquipment extends Action {

      public ActionRemoveAllEquipment() {

         super("Remove A&ll Equipment", AS_PUSH_BUTTON);
      }

      @Override
      public void run() {

         BusyIndicator.showWhile(Display.getCurrent(), () -> removeAllEquipment());
      }
   }

   public EquipmentMenuManager(final ITourProvider tourProvider,
                               final boolean isSaveTour,
                               final boolean isCheckTourEditor) {

      _tourProvider = tourProvider;

      _isSaveTour = isSaveTour;
      _isCheckTourEditor = isCheckTourEditor;

      createActions();

   }

   private void createActions() {

      _actionAddEquipment = new ActionAddEquipment_SubMenu(this);
      _actionRemoveEquipment = new ActionRemoveEquipment_SubMenu(this);
      _actionRemoveAllEquipment = new ActionRemoveAllEquipment();
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
         menuMgr.add(_actionRemoveAllEquipment);
      }

      enableActions();
   }

   private Map<Long, Equipment> getAllUseEquipments() {

      final Map<Long, Equipment> allUsedEquipment = new HashMap<>();

      final List<TourData> allSelectedTours = _tourProvider.getSelectedTours();
      for (final TourData tourData : allSelectedTours) {

         final Set<Equipment> allTourEquipments = tourData.getEquipment();

         for (final Equipment tourEquipment : allTourEquipments) {
            allUsedEquipment.put(tourEquipment.getEquipmentId(), tourEquipment);
         }
      }

      return allUsedEquipment;
   }

   public ITourProvider getTourProvider() {
      return _tourProvider;
   }

   public boolean isCheckTourEditor() {
      return _isCheckTourEditor;
   }

   public boolean isSaveTour() {
      return _isSaveTour;
   }

   private void removeAllEquipment() {

      // get tours which tour type should be changed
      final ArrayList<TourData> allModifiedTours = _tourProvider.getSelectedTours();

      if (allModifiedTours == null || allModifiedTours.isEmpty()) {
         return;
      }

      final HashMap<Long, Equipment> allModifiedEquipment = new HashMap<>();

      // remove tag in all tours (without tours from an editor)
      for (final TourData tourData : allModifiedTours) {

         // get all tag's which will be removed
         final Set<Equipment> allEquipment = tourData.getEquipment();

         for (final Equipment equipment : allEquipment) {
            allModifiedEquipment.put(equipment.getEquipmentId(), equipment);
         }

         // remove all equipment
         allEquipment.clear();
      }

      saveAndNotify(allModifiedTours, allModifiedEquipment);
   }

   /**
    * Save modified tours and notify tour provider
    *
    * @param allModifiedTours
    * @param allModifiedEquipment
    */
   private void saveAndNotify(ArrayList<TourData> allModifiedTours, final Map<Long, Equipment> allModifiedEquipment) {

      if (_isSaveTour) {

         // save all tours with the removed tags

         allModifiedTours = TourManager.saveModifiedTours(allModifiedTours);

      } else {

         // tours are not saved but the tour provider must be notified that tours has changed

         if (_tourProvider instanceof ITourProvider2) {

            ((ITourProvider2) _tourProvider).toursAreModified(allModifiedTours);

         } else {

            TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(allModifiedTours));
         }
      }

//      TourManager.fireEventWithCustomData(TourEventId.NOTIFY_TAG_VIEW,
//            new ChangedTags(modifiedTags, allModifiedTours, false),
//            null);
   }

}
