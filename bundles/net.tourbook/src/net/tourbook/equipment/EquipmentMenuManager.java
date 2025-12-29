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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.common.util.LRUMap;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.Equipment;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageEquipment;
import net.tourbook.preferences.PrefPageEquipmentGroups;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.IActionProvider;
import net.tourbook.ui.action.TourActionCategory;
import net.tourbook.ui.action.TourActionManager;
import net.tourbook.ui.views.tourBook.TVITourBookTour;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PlatformUI;

public class EquipmentMenuManager implements IActionProvider {

   private static final char NL = UI.NEW_LINE;

// SET_FORMATTING_OFF

   private static final IPreferenceStore         _prefStore    = TourbookPlugin.getPrefStore();
   private static final IDialogSettings          _state        = TourbookPlugin.getState("net.tourbook.equipment.EquipmentMenuManager");                                                             //$NON-NLS-1$

// SET_FORMATTING_ON

   private static final String                   STATE_RECENT_EQUIPMENT              = "STATE_RECENT_EQUIPMENT"; //$NON-NLS-1$

   private static IPropertyChangeListener        _prefChangeListener;

   /**
    * Number of equipment which are displayed in the context menu or saved in the dialog settings,
    * it's max number is 9 to have a unique accelerator key
    */
   private static int                            _maxRecentEquipment;

   private static LinkedHashMap<Long, Equipment> _allRecentEquipment                 = new LRUMap<>(10);

   private static ActionRecentEquipment[]        _allActions_RecentEquipment;
   private static List<ActionRecentEquipment>    _allActions_RecentEquipment_Visible = new ArrayList<>();

   private static EquipmentMenuManager           _currentInstance;

   private static Map<Long, Equipment>           _allEquipment_WhenCopied;

   private ITourProvider                         _tourProvider;

   private EquipmentTransfer                     _equipmentTransfer                  = new EquipmentTransfer();

   private boolean                               _isCheckTourEditor;
   private boolean                               _isSaveTour;

   private Map<String, Object>                   _allEquipmentActions;

   private ActionAddEquipment_SubMenu            _actionAddEquipment;
   private ActionAddEquipmentGroups_SubMenu      _actionAddEquipment_Groups;
   private ActionAddRecentEquipment              _actionAddRecentEquipment;
   private ActionClipboard_CopyEquipment         _actionClipboard_CopyEquipment;
   private ActionClipboard_PasteEquipment        _actionClipboard_PasteEquipment;
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

   public class ActionClipboard_CopyEquipment extends Action {

      public ActionClipboard_CopyEquipment() {

         super(Messages.Action_Equipment_ClipboardCopy, AS_PUSH_BUTTON);

         setToolTipText(Messages.Action_Equipment_ClipboardCopy_Tooltip);
      }

      @Override
      public void run() {

         clipboard_CopyEquipment();
      }
   }

   public class ActionClipboard_PasteEquipment extends Action {

      public ActionClipboard_PasteEquipment() {

         super(Messages.Action_Equipment_ClipboardPaste, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {

         clipboard_PasteEquipment();
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

         equipment_Add(__equipmentGroup);
      }
   }

   public static class ActionRecentEquipment extends Action {

      Equipment equipment;

      public ActionRecentEquipment() {
         super(net.tourbook.ui.UI.IS_NOT_INITIALIZED, AS_CHECK_BOX);
      }

      @Override
      public void run() {

         _currentInstance.addEquipment(equipment);
      }

      @Override
      public String toString() {

         return UI.EMPTY_STRING

               + "ActionRecentEquipment" + NL //            //$NON-NLS-1$
               + " __equipment = " + equipment + NL //      //$NON-NLS-1$
         ;
      }

      private void updateAction(final Equipment equipment, final String equipmentText) {

         setText(equipmentText);

         this.equipment = equipment;
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

         EquipmentManager.equipment_RemoveAll(_tourProvider, _isSaveTour, _isCheckTourEditor);
      }
   }

   private class EquipmentTransfer extends ByteArrayTransfer {

      private final String TYPE_NAME = "net.tourbook.equipment.EquipmentMenuManager.EquipmentTransfer"; //$NON-NLS-1$
      private final int    TYPE_ID   = registerType(TYPE_NAME);

      private EquipmentTransfer() {}

      @Override
      protected int[] getTypeIds() {
         return new int[] { TYPE_ID };
      }

      @Override
      protected String[] getTypeNames() {
         return new String[] { TYPE_NAME };
      }

      @Override
      protected void javaToNative(final Object data, final TransferData transferData) {

         try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
               final DataOutputStream dataOut = new DataOutputStream(out)) {

            if (_allEquipment_WhenCopied != null) {

               // write number of equipment
               dataOut.writeInt(_allEquipment_WhenCopied.size());

               // write all equipment ID's
               for (final Entry<Long, Equipment> entry : _allEquipment_WhenCopied.entrySet()) {
                  dataOut.writeLong(entry.getKey());
               }
            }

            super.javaToNative(out.toByteArray(), transferData);

         } catch (final IOException e) {

            StatusUtil.log(e);
         }
      }

      @Override
      protected Object nativeToJava(final TransferData transferData) {

         final byte[] bytes = (byte[]) super.nativeToJava(transferData);

         try (final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
               final DataInputStream dataIn = new DataInputStream(in)) {

            final HashSet<Long> allEquipmentIDs = new HashSet<>();

            // read number of equipment
            final int numEquipment = dataIn.readInt();

            for (int equipmentIndex = 0; equipmentIndex < numEquipment; equipmentIndex++) {

               // read equipment ID
               final long equipmentID = dataIn.readLong();

               allEquipmentIDs.add(equipmentID);
            }

            return allEquipmentIDs;

         } catch (final IOException e) {

            StatusUtil.log(e);
         }

         return null;
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

   /**
    * Replace equipment in recent equipment with freshly loaded equipment instance
    */
   public static void updateRecentEquipment() {

      final Map<Long, Equipment> allAvailableEquipment = EquipmentManager.getAllEquipment_ByID();
      final Collection<Equipment> allRecentEquipment = _allRecentEquipment.values();

      final LinkedHashMap<Long, Equipment> allNewRecentEquipment = new LRUMap<>(10);

      for (final Equipment recentEquipment : allRecentEquipment) {

         final long recentEquipmentID = recentEquipment.getEquipmentId();
         final Equipment availableEquipment = allAvailableEquipment.get(recentEquipmentID);

         allNewRecentEquipment.put(recentEquipmentID, availableEquipment);
      }

      _allRecentEquipment = allNewRecentEquipment;
   }

   private void addEquipment(final Equipment equipment) {

      EquipmentManager.equipment_Add(

            equipment,
            _tourProvider,

            _isSaveTour,
            _isCheckTourEditor);

      updateRecentEquipment(equipment);
   }

   private void clipboard_CopyEquipment() {

      _allEquipment_WhenCopied = getSelectedEquipment();

      final Clipboard clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
      {
         clipboard.setContents(

               new Object[] { new Object() },
               new Transfer[] { _equipmentTransfer });
      }
      clipboard.dispose();

      UI.showStatusLineMessage("%d equipment were copied to the clipboard".formatted(_allEquipment_WhenCopied.size()));
   }

   private void clipboard_PasteEquipment() {

      Object contents;

      final Clipboard clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
      {
         contents = clipboard.getContents(_equipmentTransfer);
      }
      clipboard.dispose();

      if (contents instanceof final HashSet allEquipmentIDs) {

         // get all equipment from the equipment ID's

         final Map<Long, Equipment> allEquipment = EquipmentManager.getAllEquipment_ByID();
         final Map<Long, Equipment> allClipboardEquipment = new HashMap<>();

         for (final Object equipmentID : allEquipmentIDs) {

            final Equipment equipment = allEquipment.get(equipmentID);

            if (equipment != null) {

               allClipboardEquipment.put(equipment.getEquipmentId(), equipment);
            }
         }

         if (allClipboardEquipment.size() > 0) {

            equipment_Add(allClipboardEquipment.values());
         }
      }
   }

   private void createActions() {

// SET_FORMATTING_OFF

      _actionAddEquipment              = new ActionAddEquipment_SubMenu(this);
      _actionAddEquipment_Groups       = new ActionAddEquipmentGroups_SubMenu();
      _actionAddRecentEquipment        = new ActionAddRecentEquipment(this);
      _actionRemoveEquipment           = new ActionRemoveEquipment_SubMenu(this);
      _actionRemoveAllEquipment        = new ActionRemoveEquipmentAll();

      _actionClipboard_CopyEquipment   = new ActionClipboard_CopyEquipment();
      _actionClipboard_PasteEquipment  = new ActionClipboard_PasteEquipment();

      _actionEquipmentPreferences      = new ActionOpenPrefDialog(Messages.Action_Equipment_EquipmentPreferences, PrefPageEquipment.ID);
      _actionEquipmentGroupPreferences = new ActionOpenPrefDialog(Messages.Action_Equipment_ManageEquipmentGroups, PrefPageEquipmentGroups.ID);

      _allEquipmentActions             = new HashMap<>();

      _allEquipmentActions.put(_actionAddEquipment             .getClass().getName(),  _actionAddEquipment);
      _allEquipmentActions.put(_actionAddEquipment_Groups      .getClass().getName(),  _actionAddEquipment_Groups);
      _allEquipmentActions.put(_actionAddRecentEquipment       .getClass().getName(),  _actionAddRecentEquipment);
      _allEquipmentActions.put(_actionClipboard_CopyEquipment  .getClass().getName(),  _actionClipboard_CopyEquipment);
      _allEquipmentActions.put(_actionClipboard_PasteEquipment .getClass().getName(),  _actionClipboard_PasteEquipment);
      _allEquipmentActions.put(_actionRemoveEquipment          .getClass().getName(),  _actionRemoveEquipment);
      _allEquipmentActions.put(_actionRemoveAllEquipment       .getClass().getName(),  _actionRemoveAllEquipment);

// SET_FORMATTING_ON

   }

   /**
    * Enable actions from selected tours
    *
    * @param allSelectedTourItems
    */
   public void enableActions(final List<TVITourBookTour> allSelectedTourItems) {

      if (allSelectedTourItems == null) {
         return;
      }

      final int numSelectedTours = allSelectedTourItems.size();

      boolean isEnabled_RemoveEquipment = false;

      final HashSet<Long> allSelectedEquipmentIDs_AllTours = new HashSet<>();

      for (final TVITourBookTour tviTourBookTour : allSelectedTourItems) {

         final List<Long> allTourEquipmentIDs = tviTourBookTour.getEquipmentIds();

         if (allTourEquipmentIDs != null && allTourEquipmentIDs.size() > 0) {

            allSelectedEquipmentIDs_AllTours.addAll(allTourEquipmentIDs);

            isEnabled_RemoveEquipment = true;
         }
      }

      final int numClipboardEquipment = updateUI_PasteAction();

      enableActions_Equipment(isEnabled_RemoveEquipment, numClipboardEquipment);

      enableActions_Recent(allSelectedEquipmentIDs_AllTours, numSelectedTours);
   }

   private void enableActions_Equipment(final boolean isEnabled_RemoveEquipment,
                                        final int numClipboardEquipment) {
// SET_FORMATTING_OFF

      _actionAddEquipment              .setEnabled(true);
      _actionAddEquipment_Groups       .setEnabled(true);

      _actionRemoveEquipment           .setEnabled(isEnabled_RemoveEquipment);
      _actionRemoveAllEquipment        .setEnabled(isEnabled_RemoveEquipment);

      _actionClipboard_CopyEquipment   .setEnabled(isEnabled_RemoveEquipment);
      _actionClipboard_PasteEquipment  .setEnabled(numClipboardEquipment > 0);

// SET_FORMATTING_ON
   }

   private void enableActions_OneTour() {

      final Map<Long, Equipment> allUsedEquipments = getAllUsedEquipments();

      final int numClipboardEquipment = updateUI_PasteAction();

      final boolean isEnabled_RemoveEquipment = allUsedEquipments.size() > 0;

      enableActions_Equipment(isEnabled_RemoveEquipment, numClipboardEquipment);

      enableActions_Recent();
   }

   private void enableActions_Recent() {

      // get all equipment from all tours
      final HashSet<Equipment> allUsedEquipment = new HashSet<>();
      final List<TourData> allSelectedTours = _tourProvider.getSelectedTours();

      for (final TourData tourData : allSelectedTours) {
         allUsedEquipment.addAll(tourData.getEquipment());
      }

      for (final ActionRecentEquipment actionRecentEquipment : _allActions_RecentEquipment_Visible) {

         final Equipment equipment = actionRecentEquipment.equipment;

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

   private void enableActions_Recent(final Set<Long> allSelectedEquipmentIDs_AllTours,
                                     final int numSelectedTours) {

      if (_allActions_RecentEquipment_Visible.size() == 0) {
         return;
      }

      final boolean isOneTourWithEquipment = allSelectedEquipmentIDs_AllTours.size() > 0 && numSelectedTours == 1;

      for (final ActionRecentEquipment actionRecentEquipment : _allActions_RecentEquipment_Visible) {

         final Equipment recentEquipment = actionRecentEquipment.equipment;

         if (recentEquipment == null) {

            actionRecentEquipment.setEnabled(false);
            actionRecentEquipment.setChecked(false);

            continue;
         }

         final long recentEquipmentID = recentEquipment.getEquipmentId();

         boolean isEquipmentEnabled;
         boolean isEquipmentChecked = false;

         final boolean isRecentEquipmentSelected = allSelectedEquipmentIDs_AllTours.contains(recentEquipmentID);

         if (isOneTourWithEquipment) {

            // one tour is selected

            // disable action when its tour equipment id is selected
            isEquipmentEnabled = isRecentEquipmentSelected == false;
            isEquipmentChecked = isRecentEquipmentSelected;

         } else {

            // multiple tours are selected

            isEquipmentEnabled = true;

            if (isRecentEquipmentSelected) {
               isEquipmentChecked = true;
            }
         }

         actionRecentEquipment.setEnabled(isEquipmentEnabled);
         actionRecentEquipment.setChecked(isEquipmentChecked);
      }
   }

   /**
    * Add and save multiple equipment in the selected tours
    *
    * @param allModifiedEquipment
    */
   private void equipment_Add(final Collection<Equipment> allModifiedEquipment) {

      EquipmentManager.equipment_Add(

            allModifiedEquipment,
            _tourProvider,

            _isSaveTour,
            _isCheckTourEditor);

      // update recent equipment
      for (final Equipment equipment : allModifiedEquipment) {
         updateRecentEquipment(equipment);
      }
   }

   /**
    * Set and save all tour equipment from a group
    *
    * @param equipmentGroup
    */
   private void equipment_Add(final EquipmentGroup equipmentGroup) {

      final HashMap<Long, Equipment> allEquipment = new HashMap<>();

      for (final Equipment equipment : equipmentGroup.allEquipment) {

         allEquipment.put(equipment.getEquipmentId(), equipment);
      }

      equipment_Add(allEquipment.values());
   }

   @Override
   public void fillActions(final IMenuManager menuMgr,
                           final ITourProvider tourProvider) {

      _currentInstance = this;

      fillMenuWithRecentEquipment(menuMgr);
   }

   /**
    * Add all equipment actions
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

         menuMgr.add(_actionClipboard_CopyEquipment);
         menuMgr.add(_actionClipboard_PasteEquipment);

         menuMgr.add(new Separator());

         menuMgr.add(_actionEquipmentPreferences);
      }

      enableActions_OneTour();
   }

   public void fillEquipmentMenu_WithActiveActions(final IMenuManager menuMgr,
                                                   final ITourProvider tourProvider) {

      _currentInstance = this;

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

   /**
    * @return Returns all equipment from all selected tours
    */
   private Map<Long, Equipment> getAllUsedEquipments() {

      final Map<Long, Equipment> allUsedEquipment = new HashMap<>();

      final List<TourData> allSelectedTours = _tourProvider.getSelectedTours();

      for (final TourData tourData : allSelectedTours) {

         final Set<Equipment> allTourEquipment = tourData.getEquipment();

         for (final Equipment tourEquipment : allTourEquipment) {
            allUsedEquipment.put(tourEquipment.getEquipmentId(), tourEquipment);
         }
      }

      return allUsedEquipment;
   }

   private List<Equipment> getEquipmentFromClipboard() {

      Object clipboardContent;

      final Clipboard clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
      {
         clipboardContent = clipboard.getContents(_equipmentTransfer);
      }
      clipboard.dispose();

      if (clipboardContent instanceof final HashSet allEquipmentIDs) {

         // get all equipment from the equipment ID's

         final Map<Long, Equipment> allTourEquipment = EquipmentManager.getAllEquipment_ByID();
         final List<Equipment> allClipboardEquipment = new ArrayList<>();

         for (final Object equipmentID : allEquipmentIDs) {

            final Equipment equipment = allTourEquipment.get(equipmentID);

            if (equipment != null) {

               allClipboardEquipment.add(equipment);
            }
         }

         return allClipboardEquipment;
      }

      return null;
   }

   private Map<Long, Equipment> getSelectedEquipment() {

      final Map<Long, Equipment> allEquipment_Selected = new HashMap<>();

      final List<TourData> allSelectedTours = _tourProvider.getSelectedTours();

      if (allSelectedTours != null) {

         // get all equipment from all tours
         for (final TourData tourData : allSelectedTours) {

            final Set<Equipment> allTourEquipment = tourData.getEquipment();

            for (final Equipment equipment : allTourEquipment) {

               allEquipment_Selected.put(equipment.getEquipmentId(), equipment);
            }
         }
      }

      return allEquipment_Selected;
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

   public void updateRecentEquipment(final Equipment equipment) {

      _allRecentEquipment.putFirst(equipment.getEquipmentId(), equipment);
   }

   /**
    * @return Returns number of equipment in the clipboard
    */
   private int updateUI_PasteAction() {

      final List<Equipment> allEquipmentInClipboard = getEquipmentFromClipboard();
      final int numEquipment = allEquipmentInClipboard != null ? allEquipmentInClipboard.size() : 0;

      if (numEquipment > 0) {

         _actionClipboard_PasteEquipment.setToolTipText("Paste equipment from the clipboard into the selected tours\n\n%s"
               .formatted(EquipmentGroupManager.createEquipmentSortedList(null, allEquipmentInClipboard)));
      }

      return numEquipment;
   }

}
