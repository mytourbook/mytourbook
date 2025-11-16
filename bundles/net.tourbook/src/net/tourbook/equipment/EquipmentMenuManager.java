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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.LRUMap;
import net.tourbook.data.Equipment;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageEquipment;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProvider2;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class EquipmentMenuManager {

// SET_FORMATTING_OFF

   private static final IPreferenceStore         _prefStore    = TourbookPlugin.getPrefStore();
   private static final IDialogSettings          _state        = TourbookPlugin.getState("net.tourbook.equipment.EquipmentMenuManager");                                                             //$NON-NLS-1$

// SET_FORMATTING_ON

   private static final String                   STATE_RECENT_EQUIPMENT              = "STATE_RECENT_EQUIPMENT";

   private static IPropertyChangeListener        _prefChangeListener;

   /**
    * Number of tags which are displayed in the context menu or saved in the dialog settings, it's
    * max number is 9 to have a unique accelerator key
    */
   private static int                            _maxRecentEquipment;

   private static LinkedHashMap<Long, Equipment> _allRecentEquipment                 = new LRUMap<>(10);

   private static ActionRecentEquipment[]        _allActions_RecentEquipment;
   private static List<ActionRecentEquipment>    _allActions_RecentEquipment_Visible = new ArrayList<>();

   private static EquipmentMenuManager           _currentInstance;

   private ITourProvider                         _tourProvider;

   private boolean                               _isCheckTourEditor;
   private boolean                               _isSaveTour;

   private ActionAddEquipment_SubMenu            _actionAddEquipment;
   private ActionOpenPrefDialog                  _actionEquipmentPreferences;
   private ActionRemoveEquipment_SubMenu         _actionRemoveEquipment;
   private ActionRemoveAllEquipment              _actionRemoveAllEquipment;

   /**
   *
   */
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

         // check if the number of recent tags has changed
         if (property.equals(ITourbookPreferences.EQUIPMENT_NUMBER_OF_RECENT_EQUIPMENT)) {

            setupRecentActions();
         }
      };

      // add pref listener which is never removed because it is lasting as long as the app is running
      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   public static void clearRecentTags() {

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
    * create actions for recent tags
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

      _actionAddEquipment           = new ActionAddEquipment_SubMenu(this);
      _actionEquipmentPreferences   = new ActionOpenPrefDialog("Equipment &Preferences", PrefPageEquipment.ID);
      _actionRemoveEquipment        = new ActionRemoveEquipment_SubMenu(this);
      _actionRemoveAllEquipment     = new ActionRemoveAllEquipment();

// SET_FORMATTING_ON

   }

   private void enableActions() {

      final List<Equipment> allAvailableEquipments = EquipmentManager.getAllEquipment_Name();
      final Map<Long, Equipment> allUseEquipments = getAllUseEquipments();

      final boolean isEquipmentAvailable = allAvailableEquipments.size() > 0;
      final boolean isEquipmentInTour = allUseEquipments.size() > 0;

      _actionAddEquipment.setEnabled(isEquipmentAvailable);
      _actionRemoveEquipment.setEnabled(isEquipmentInTour);

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
         fillMenuWithRecentEquipment(menuMgr);

         menuMgr.add(_actionRemoveEquipment);
         menuMgr.add(_actionRemoveAllEquipment);

         menuMgr.add(new Separator());

         menuMgr.add(_actionEquipmentPreferences);
      }

      enableActions();
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

   public void updateRecentEquipment(final Equipment equipment) {

      _allRecentEquipment.putFirst(equipment.getEquipmentId(), equipment);
   }

}
