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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.common.util.LRUMap;
import net.tourbook.data.Equipment;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageEquipment;
import net.tourbook.preferences.PrefPageEquipmentGroups;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.action.TourActionCategory;
import net.tourbook.ui.action.TourActionManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

public class EquipmentMenuManager {

// SET_FORMATTING_OFF

   private static final IPreferenceStore         _prefStore    = TourbookPlugin.getPrefStore();
   private static final IDialogSettings          _state        = TourbookPlugin.getState("net.tourbook.equipment.EquipmentMenuManager");                                                             //$NON-NLS-1$

// SET_FORMATTING_ON

   private static final String                   STATE_RECENT_EQUIPMENT              = "STATE_RECENT_EQUIPMENT"; //$NON-NLS-1$

   private static IPropertyChangeListener        _prefChangeListener;

   /**
    * Number of equipment which are displayed in the context menu or saved in the dialog settings,
    * it's
    * max number is 9 to have a unique accelerator key
    */
   private static int                            _maxRecentEquipment;

   private static LinkedHashMap<Long, Equipment> _allRecentEquipment                 = new LRUMap<>(10);

   private static ActionRecentEquipment[]        _allActions_RecentEquipment;
   private static List<ActionRecentEquipment>    _allActions_RecentEquipment_Visible = new ArrayList<>();
   private static Map<String, Object>            _allEquipmentActions;

   private static EquipmentMenuManager           _currentInstance;

   private ITourProvider                         _tourProvider;

   private boolean                               _isCheckTourEditor;
   private boolean                               _isSaveTour;

   private ActionAddEquipment_SubMenu            _actionAddEquipment;
   private ActionAddEquipmentGroups_SubMenu      _actionAddEquipment_Groups;
   private ActionOpenPrefDialog                  _actionEquipmentGroupPreferences;
   private ActionOpenPrefDialog                  _actionEquipmentPreferences;
   private ActionRemoveEquipment_SubMenu         _actionRemoveEquipment;
   private ActionRemoveEquipmentAll              _actionRemoveAllEquipment;

   public class ActionAddEquipmentGroups_SubMenu extends SubMenu {

      List<ActionEquipmentGroup> __allEquipmentGroupActions = new ArrayList<>();

      public ActionAddEquipmentGroups_SubMenu() {

         super(Messages.Action_Equipment_AddEquipment_Groups, AS_DROP_DOWN_MENU);
      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         __allEquipmentGroupActions.clear();

         final List<EquipmentGroup> allEquipmentGroups = EquipmentGroupManager.getEquipmentGroups();

         // create actions for each equipment group
         for (final EquipmentGroup equipmentGroup : allEquipmentGroups) {

            final Set<Equipment> allEquipment = equipmentGroup.allEquipment;
            final boolean hasEquipment = allEquipment.size() > 0;

            final ActionEquipmentGroup equipmentGroupAction = new ActionEquipmentGroup(equipmentGroup);

            equipmentGroupAction.setEnabled(hasEquipment);

            __allEquipmentGroupActions.add(equipmentGroupAction);

            addActionToMenu(equipmentGroupAction);
         }

         if (allEquipmentGroups.size() > 0) {

            addSeparatorToMenu();
         }

         addActionToMenu(_actionEquipmentGroupPreferences);
      }
   }

   private class ActionEquipmentGroup extends Action {

      private final EquipmentGroup __equipmentGroup;

      public ActionEquipmentGroup(final EquipmentGroup equipmentGroup) {

         super("%s  %d".formatted(equipmentGroup.name, equipmentGroup.allEquipment.size()), AS_PUSH_BUTTON); //$NON-NLS-1$

         setToolTipText(EquipmentGroupManager.createEquipmentSortedList(equipmentGroup));

         __equipmentGroup = equipmentGroup;
      }

      @Override
      public void run() {

         saveTourEquipment(__equipmentGroup);
      }
   }

   private static class ActionRecentEquipment extends Action {

      private Equipment __equipment;

      public ActionRecentEquipment() {
         super(net.tourbook.ui.UI.IS_NOT_INITIALIZED, AS_CHECK_BOX);
      }

      @Override
      public void run() {

         _currentInstance.addEquipment(__equipment);
      }

      private void updateAction(final Equipment equipment, final String equipmentText) {

         setText(equipmentText);

         __equipment = equipment;
      }
   }

   /**
    * Removes all equipment
    */
   public class ActionRemoveEquipmentAll extends Action {

      public ActionRemoveEquipmentAll() {

         super(Messages.Action_Equipment_RemoveEquipment_All, AS_PUSH_BUTTON);
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

      if (_prefChangeListener == null) {

         // static fields are not yet initialized

         addPrefListener();
         setupRecentActions();
      }

      createActions();
   }

   private static void addPrefListener() {

      // create pref listener
      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         // check if the number of recent equipment has changed
         if (property.equals(ITourbookPreferences.EQUIPMENT_NUMBER_OF_RECENT_EQUIPMENT)) {

            setupRecentActions();
         }
      };

      // add pref listener which is never removed because it is lasting as long as the app is running
      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   public static void clearRecentEquipment() {

      _allRecentEquipment.clear();
   }

   public static void restoreState() {

      final Map<Long, Equipment> allAvailableEquipment = EquipmentManager.getAllEquipment_ByID();

      final String[] allRecentEquipmentIDs = _state.getArray(STATE_RECENT_EQUIPMENT);

      if (allRecentEquipmentIDs != null) {

         for (final String equipmentIDString : allRecentEquipmentIDs) {

            try {

               final Long equipmentID = Long.valueOf(equipmentIDString);

               final Equipment equipment = allAvailableEquipment.get(equipmentID);

               if (equipment != null) {
                  _allRecentEquipment.put(equipmentID, equipment);
               }

            } catch (final NumberFormatException e) {
               // ignore
            }
         }
      }
   }

   public static void saveState() {

      if (_maxRecentEquipment > 0) {

         final String[] allEquipmentIDs = new String[Math.min(_maxRecentEquipment, _allRecentEquipment.size())];

         int equipmentIndex = 0;

         for (final Equipment equipment : _allRecentEquipment.values()) {

            allEquipmentIDs[equipmentIndex++] = Long.toString(equipment.getEquipmentId());

            if (equipmentIndex == _maxRecentEquipment) {
               break;
            }
         }

         _state.put(STATE_RECENT_EQUIPMENT, allEquipmentIDs);
      }
   }

   /**
    * create actions for recent equipment
    */
   private static void setupRecentActions() {

      _maxRecentEquipment = _prefStore.getInt(ITourbookPreferences.EQUIPMENT_NUMBER_OF_RECENT_EQUIPMENT);

      _allActions_RecentEquipment = new ActionRecentEquipment[_maxRecentEquipment];

      for (int actionIndex = 0; actionIndex < _allActions_RecentEquipment.length; actionIndex++) {
         _allActions_RecentEquipment[actionIndex] = new ActionRecentEquipment();
      }
   }

   private void addEquipment(final Equipment equipment) {

      EquipmentManager.equipment_Add(

            equipment,
            _tourProvider,

            _isSaveTour,
            _isCheckTourEditor);

      updateRecentEquipment(equipment);
   }

   private void createActions() {

// SET_FORMATTING_OFF

      _actionAddEquipment              = new ActionAddEquipment_SubMenu(this);
      _actionAddEquipment_Groups       = new ActionAddEquipmentGroups_SubMenu();
      _actionRemoveEquipment           = new ActionRemoveEquipment_SubMenu(this);
      _actionRemoveAllEquipment        = new ActionRemoveEquipmentAll();

      _actionEquipmentPreferences      = new ActionOpenPrefDialog(Messages.Action_Equipment_EquipmentPreferences, PrefPageEquipment.ID);
      _actionEquipmentGroupPreferences = new ActionOpenPrefDialog(Messages.Action_Equipment_ManageEquipmentGroups, PrefPageEquipmentGroups.ID);

      _allEquipmentActions             = new HashMap<>();

      _allEquipmentActions.put(_actionAddEquipment          .getClass().getName(),  _actionAddEquipment);
      _allEquipmentActions.put(_actionAddEquipment_Groups   .getClass().getName(),  _actionAddEquipment_Groups);
      _allEquipmentActions.put(_actionRemoveEquipment       .getClass().getName(),  _actionRemoveEquipment);
      _allEquipmentActions.put(_actionRemoveAllEquipment    .getClass().getName(),  _actionRemoveAllEquipment);

// SET_FORMATTING_ON

   }

   private void enableActions() {

      final List<Equipment> allAvailableEquipments = EquipmentManager.getAllEquipment_Name();
      final Map<Long, Equipment> allUseEquipments = getAllUseEquipments();

      final boolean isEquipmentAvailable = allAvailableEquipments.size() > 0;
      final boolean isEquipmentUsedInTour = allUseEquipments.size() > 0;

      _actionAddEquipment.setEnabled(isEquipmentAvailable);
      _actionAddEquipment_Groups.setEnabled(isEquipmentAvailable);
      _actionRemoveEquipment.setEnabled(isEquipmentUsedInTour);

      enableRecentActions();
   }

   private void enableRecentActions() {

      // get all equipment from all tours
      final HashSet<Equipment> allUsedEquipment = new HashSet<>();
      final List<TourData> allSelectedTours = _tourProvider.getSelectedTours();

      for (final TourData tourData : allSelectedTours) {
         allUsedEquipment.addAll(tourData.getEquipment());
      }

      for (final ActionRecentEquipment actionRecentEquipment : _allActions_RecentEquipment_Visible) {

         final Equipment equipment = actionRecentEquipment.__equipment;

         if (allUsedEquipment.contains(equipment)) {

            actionRecentEquipment.setEnabled(false);

         } else {

            // this action could be disabled because it is a shared action
            actionRecentEquipment.setEnabled(true);
         }

         // this shared action could be checked
         actionRecentEquipment.setChecked(false);
      }
   }

   /**
    * Add all tour equipment actions
    *
    * @param menuMgr
    */
   public void fillEquipmentMenu(final IMenuManager menuMgr) {

      _currentInstance = this;

      menuMgr.add(new Separator());
      {
         menuMgr.add(_actionAddEquipment);
         menuMgr.add(_actionAddEquipment_Groups);
         fillMenuWithRecentEquipment(menuMgr);

         menuMgr.add(_actionRemoveEquipment);
         menuMgr.add(_actionRemoveAllEquipment);

         menuMgr.add(new Separator());

         menuMgr.add(_actionEquipmentPreferences);
      }

      enableActions();
   }

   public void fillEquipmentMenu_WithActiveActions(final IMenuManager menuMgr,
                                                   final ITourProvider tourProvider) {

      menuMgr.add(new Separator());

      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.EQUIPMENT, _allEquipmentActions, tourProvider);
   }

   /**
    * @param menuMgr
    */
   private void fillMenuWithRecentEquipment(final IMenuManager menuMgr) {

      if (_maxRecentEquipment < 1) {

         // recent actions are disabled

         return;
      }

      final int numRecentEquipments = _allRecentEquipment.size();

      if (numRecentEquipments == 0) {
         return;
      }

      final Collection<Equipment> allRecentEquipment = _allRecentEquipment.values();

      final int numVisibleActions = _allActions_RecentEquipment.length;

      int equipmentIndex = 0;

      _allActions_RecentEquipment_Visible.clear();

      /*
       * loop: all recent equipment which is limited by
       * the number of visible actions and number of available recent equipment
       */
      for (final Equipment equipment : allRecentEquipment) {

         if (equipmentIndex >= numVisibleActions) {
            break;
         }

         String equipmentText = equipment.getName();

         if (UI.IS_SCRAMBLE_DATA) {
            equipmentText = UI.scrambleText(equipmentText);
         }

         final String equipmentTextFinal = UI.SPACE4 + UI.MNEMONIC + (equipmentIndex + 1) + UI.SPACE2 + equipmentText;

         // update action
         final ActionRecentEquipment actionRecentEquipment = _allActions_RecentEquipment[equipmentIndex];
         actionRecentEquipment.updateAction(equipment, equipmentTextFinal);

         // add to menu
         menuMgr.add(new ActionContributionItem(actionRecentEquipment));

         _allActions_RecentEquipment_Visible.add(actionRecentEquipment);

         // advance to the next equipment
         equipmentIndex++;
      }
   }

   public Map<String, Object> getAllEquipmentActions() {
      return _allEquipmentActions;
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

      // remove equipment in all tours (without tours from an editor)
      for (final TourData tourData : allModifiedTours) {

         // get all equipment which will be removed
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

         // save all tours with the removed equipment

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

   /**
    * Set and save all tour equipment from a group
    *
    * @param equipmentGroup
    */
   private void saveTourEquipment(final EquipmentGroup equipmentGroup) {

      final HashMap<Long, Equipment> allEquipment = new HashMap<>();

      for (final Equipment equipment : equipmentGroup.allEquipment) {

         allEquipment.put(equipment.getEquipmentId(), equipment);
      }

      saveTourEquipment_All(allEquipment, true);
   }

   /**
    * Add/remove and save for multiple equipment
    *
    * @param mapWithAllModifiedEquipment
    * @param isAddMode
    *           When <code>true</code> then equipment are added otherwise they are removed
    */
   private void saveTourEquipment_All(final HashMap<Long, Equipment> mapWithAllModifiedEquipment,
                                      final boolean isAddMode) {

      final Runnable runnable = () -> {

         final ArrayList<TourData> allSelectedTours = _tourProvider.getSelectedTours();

         // get tours which equipment should be changed
         if (allSelectedTours == null || allSelectedTours.isEmpty()) {
            return;
         }

         final Collection<Equipment> allModifiedEquipment = mapWithAllModifiedEquipment.values();

         // add the equipment into all selected tours
         for (final TourData tourData : allSelectedTours) {

            // set equipment into a tour
            final Set<Equipment> allEquipment = tourData.getEquipment();

            if (isAddMode) {

               // add equipment to the tour
               allEquipment.addAll(allModifiedEquipment);

            } else {

               // remove equipment from tour
               allEquipment.removeAll(allModifiedEquipment);
            }
         }

         // update recent equipment
         for (final Equipment equipment : allModifiedEquipment) {

            _allRecentEquipment.putFirst(equipment.getEquipmentId(), equipment);
         }

         saveAndNotify(allSelectedTours, mapWithAllModifiedEquipment);
      };

      BusyIndicator.showWhile(Display.getCurrent(), runnable);
   }

   public void updateRecentEquipment(final Equipment equipment) {

      _allRecentEquipment.putFirst(equipment.getEquipmentId(), equipment);
   }

}
