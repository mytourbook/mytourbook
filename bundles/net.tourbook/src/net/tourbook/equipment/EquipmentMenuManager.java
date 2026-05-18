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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.ICommandIds;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.common.util.AdvancedMenuForActions;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.ToolTip;
import net.tourbook.data.Equipment;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageEquipment;
import net.tourbook.preferences.PrefPageEquipmentGroups;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.IActionProvider;
import net.tourbook.ui.action.TourActionCategory;
import net.tourbook.ui.action.TourActionManager;
import net.tourbook.ui.views.referenceTour.TVIRefTour_ComparedTour;
import net.tourbook.ui.views.tagging.TVITaggingView_Tour;
import net.tourbook.ui.views.tourBook.TVITourBookTour;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

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
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PlatformUI;

public class EquipmentMenuManager implements IActionProvider {

   private static final char NL = UI.NEW_LINE;

// SET_FORMATTING_OFF

   public static final String ACTION_KEY__ADD_EQUIPMENT_AUTO_OPEN_DEFAULT = ActionAddEquipment_SubMenu.class.getName() + TourActionManager.AUTO_OPEN_DEFAULT;
   public static final String ACTION_KEY__ADD_EQUIPMENT_AUTO_OPEN_FLAT    = ActionAddEquipment_SubMenu.class.getName() + TourActionManager.AUTO_OPEN_FLAT;
   public static final String ACTION_KEY__ADD_EQUIPMENT_AUTO_OPEN_TREE    = ActionAddEquipment_SubMenu.class.getName() + TourActionManager.AUTO_OPEN_TREE;

   private static final IPreferenceStore         _prefStore    = TourbookPlugin.getPrefStore();
   private static final IDialogSettings          _state        = TourbookPlugin.getState("net.tourbook.equipment.EquipmentMenuManager");                                                             //$NON-NLS-1$

// SET_FORMATTING_ON

   private static final String                   STATE_PREVIOUS_EQUIPMENT = "STATE_PREVIOUS_EQUIPMENT"; //$NON-NLS-1$
   private static final String                   STATE_RECENT_EQUIPMENT   = "STATE_RECENT_EQUIPMENT";   //$NON-NLS-1$

   private static IPropertyChangeListener        _prefChangeListener;

   private static EquipmentMenuManager           _currentInstance;
   private static boolean                        _isAdvMenu;

   private static ActionRecentEquipment[]        _allActions_RecentEquipment;
   private static ActionWithAllPreviousEquipment _actionWithAllPreviousEquipment;

   /**
    * Number of equipment which are displayed in the context menu or saved in the dialog settings,
    * it's max number is 9 to have a unique accelerator key
    */
   private static int                            _maxRecentEquipment      = -1;

   private static LinkedList<Equipment>          _allRecentEquipment      = new LinkedList<>();

   /**
    * Contains all equipment which were added by the last add action in the advanced menu
    */
   private static LinkedList<Equipment>          _allPreviousEquipment    = new LinkedList<>();

   /**
    * Contains all equipment ids when only one tour is selected
    */
   private static HashSet<Long>                  _allEquipmentIds_OneTour = new HashSet<>();

   private static Map<Long, Equipment>           _allEquipment_WhenCopied;

   private static boolean                        _isTaggingAutoOpen;
   private static boolean                        _isTaggingAnimation;
   private static int                            _taggingAutoOpenDelay;

   private ITourProvider                         _tourProvider;

   private EquipmentTransfer                     _equipmentTransfer       = new EquipmentTransfer();

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

   private ActionContributionItem                _actionContribItem_AddEquipment_AutoOpen_Current;
   private ActionContributionItem                _actionContribItem_AddEquipment_AutoOpen_Default;
   private ActionContributionItem                _actionContribItem_AddEquipment_AutoOpen_Flat;
   private ActionContributionItem                _actionContribItem_AddEquipment_AutoOpen_Tree;

   private AdvancedMenuForActions                _advancedMenuToAddEquipment_Current;
   private AdvancedMenuForActions                _advancedMenuToAddEquipment_Default;
   private AdvancedMenuForActions                _advancedMenuToAddEquipment_Flat;
   private AdvancedMenuForActions                _advancedMenuToAddEquipment_Tree;

   public class ActionAddEquipmentGroups_SubMenu extends SubMenu {

      List<ActionEquipmentGroup> __allEquipmentGroupActions = new ArrayList<>();

      public ActionAddEquipmentGroups_SubMenu() {

         super(Messages.Equipment_Action_AddEquipment_Groups, AS_DROP_DOWN_MENU);
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

         super(Messages.Equipment_Action_ClipboardCopy, AS_PUSH_BUTTON);

         setToolTipText(Messages.Equipment_Action_ClipboardCopy_Tooltip);
      }

      @Override
      public void run() {

         clipboard_CopyEquipment();
      }
   }

   public class ActionClipboard_PasteEquipment extends Action {

      public ActionClipboard_PasteEquipment() {

         super(Messages.Equipment_Action_ClipboardPaste, AS_PUSH_BUTTON);
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

   private static class ActionRecentEquipment extends Action {

      private Equipment equipment;

      public ActionRecentEquipment() {
         super(net.tourbook.ui.UI.IS_NOT_INITIALIZED, AS_CHECK_BOX);
      }

      @Override
      public void run() {

         if (_isAdvMenu) {

            final ActionAddEquipment_SubMenu actionAddEquipmentAdvanced =
                  (ActionAddEquipment_SubMenu) _currentInstance._actionContribItem_AddEquipment_AutoOpen_Current.getAction();

            final boolean isAdd = isChecked();

            actionAddEquipmentAdvanced.setEquipment(isAdd, equipment);

         } else {

            _currentInstance.addEquipment(equipment);
         }
      }

      @Override
      public String toString() {

         return UI.EMPTY_STRING

               + "ActionRecentEquipment" + NL //            //$NON-NLS-1$
               + " __equipment = " + equipment + NL //      //$NON-NLS-1$
         ;
      }

      private void updateEquipmentAction(final Equipment equipment, final String equipmentText) {

         setText(equipmentText);

         this.equipment = equipment;
      }
   }

   /**
    * Removes all equipment
    */
   public class ActionRemoveEquipmentAll extends Action {

      public ActionRemoveEquipmentAll() {

         super(Messages.Equipment_Action_RemoveEquipment_All, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {

         EquipmentManager.equipment_RemoveAll(_tourProvider, _isSaveTour, _isCheckTourEditor);
      }
   }

   /**
   *
   */
   private static class ActionWithAllPreviousEquipment extends Action {

      public ActionWithAllPreviousEquipment() {

         super(net.tourbook.ui.UI.IS_NOT_INITIALIZED, AS_CHECK_BOX);
      }

      @Override
      public void run() {

         final boolean isChecked = isChecked();

         // when this action is selected then it is also checked but this can be irritating
         setChecked(false);

         if (_isAdvMenu) {

            final ActionAddEquipment_SubMenu action = (ActionAddEquipment_SubMenu) _currentInstance._actionContribItem_AddEquipment_AutoOpen_Current
                  .getAction();

            action.setEquipment(true, _allPreviousEquipment);

         } else {

            if (isChecked) {

               // add equipment

               EquipmentManager.equipment_Add(_allPreviousEquipment,

                     _currentInstance._tourProvider,
                     _currentInstance.isSaveTour(),
                     _currentInstance.isCheckTourEditor());

            } else {

               // remove equipment

               EquipmentManager.equipment_Remove(_allPreviousEquipment,

                     _currentInstance._tourProvider,
                     _currentInstance.isSaveTour(),
                     _currentInstance.isCheckTourEditor());
            }
         }
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
         restoreAutoOpen();
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

         } else if (property.equals(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN)
               || property.equals(ITourbookPreferences.APPEARANCE_IS_TAGGING_ANIMATION)
               || property.equals(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY)) {

            restoreAutoOpen();
         }
      };

      // add pref listener which is never removed because it is lasting as long as the app is running
      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   public static void clearPreviousAndRecentEquipment() {

      _allPreviousEquipment.clear();
      _allRecentEquipment.clear();
   }

   /**
    * @param isEnabled_AddEquipment
    * @param numSelectedTours
    * @param allEquipmentIDs_InTours
    * @param allModifiedEquipmentIDs
    *           Is <code>null</code> when its a normal menu. In the advanced menu it contains all
    *           selected equipment before they are applied to the tours
    */
   static void enableActions_Recent(final boolean isEnabled_AddEquipment,
                                    final int numSelectedTours,
                                    final Set<Long> allEquipmentIDs_InTours,
                                    final Set<Long> allModifiedEquipmentIDs) {


      if (_allActions_RecentEquipment.length == 0) {
         return;
      }

      if (isEnabled_AddEquipment) {

         final boolean isOneTourWithEquipment = numSelectedTours == 1

               && allEquipmentIDs_InTours.size() > 0;

         for (final ActionRecentEquipment actionRecentEquipment : _allActions_RecentEquipment) {

            final Equipment recentEquipment = actionRecentEquipment.equipment;

            if (recentEquipment == null) {

               // this happens when the recent actions are not yet used

               actionRecentEquipment.setEnabled(false);
               actionRecentEquipment.setChecked(false);

               continue;
            }

            final long recentEquipmentID = recentEquipment.getEquipmentId();

            boolean isEquipmentEnabled = true;
            boolean isEquipmentChecked = false;

            final boolean isEquipmentInModified = allModifiedEquipmentIDs != null && allModifiedEquipmentIDs.contains(recentEquipmentID);
            final boolean isEquipmentInTour = allEquipmentIDs_InTours.contains(recentEquipmentID);

            if (isEquipmentInModified) {

               isEquipmentEnabled = true;
               isEquipmentChecked = true;

            } else if (isOneTourWithEquipment) {

               // one tour is selected

               // disable action when its tour equipment id is selected
               isEquipmentEnabled = isEquipmentInTour == false;
               isEquipmentChecked = isEquipmentInTour;

            } else {

               // multiple tours are selected

               isEquipmentEnabled = true;

               if (isEquipmentInTour) {
                  isEquipmentChecked = true;
               }
            }

            actionRecentEquipment.setEnabled(isEquipmentEnabled);
            actionRecentEquipment.setChecked(isEquipmentChecked);
         }

      } else {

         // disable all recent actions, this is applied when no tours are selected

         for (final ActionRecentEquipment recentAction : _allActions_RecentEquipment) {

            recentAction.setEnabled(false);
         }
      }

      if (_allPreviousEquipment != null && isEnabled_AddEquipment) {

         boolean isEquipmentChecked = true;

         for (final Equipment previousEquipment : _allPreviousEquipment) {

            final long previousEquipmentId = previousEquipment.getEquipmentId();

            if (_allEquipmentIds_OneTour.contains(previousEquipmentId) == false) {
               isEquipmentChecked = false;
               break;
            }
         }

         _actionWithAllPreviousEquipment.setChecked(isEquipmentChecked);
         _actionWithAllPreviousEquipment.setEnabled(isEquipmentChecked == false);

      } else {

         _actionWithAllPreviousEquipment.setChecked(false);
         _actionWithAllPreviousEquipment.setEnabled(false);
      }
   }

   private static void restoreAutoOpen() {

      _isTaggingAutoOpen = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN);
      _isTaggingAnimation = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_ANIMATION);
      _taggingAutoOpenDelay = _prefStore.getInt(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY);
   }

   public static void restoreState() {

      final Map<Long, Equipment> allAvailableEquipment = EquipmentManager.getAllEquipment_ByID();

      final String[] allRecentEquipmentIDs = _state.getArray(STATE_RECENT_EQUIPMENT);

      if (allRecentEquipmentIDs != null) {

         for (final String equipmentIDText : allRecentEquipmentIDs) {

            try {

               final Long equipmentID = Long.valueOf(equipmentIDText);

               final Equipment equipment = allAvailableEquipment.get(equipmentID);

               if (equipment != null) {

                  // remove duplicates which were created during the development
                  _allRecentEquipment.remove(equipment);

                  _allRecentEquipment.add(equipment);
               }

            } catch (final NumberFormatException e) {
               // ignore
            }
         }
      }

      final String[] previousEquipmentIds = _state.getArray(STATE_PREVIOUS_EQUIPMENT);
      if (previousEquipmentIds != null) {

         for (final String equipmentIdText : previousEquipmentIds) {

            try {

               final Long equipmentID = Long.valueOf(equipmentIdText);

               final Equipment equipment = allAvailableEquipment.get(equipmentID);

               if (equipment != null) {
                  _allPreviousEquipment.add(equipment);
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

         for (final Equipment equipment : _allRecentEquipment) {

            allEquipmentIDs[equipmentIndex++] = Long.toString(equipment.getEquipmentId());

            if (equipmentIndex == _maxRecentEquipment) {
               break;
            }
         }

         _state.put(STATE_RECENT_EQUIPMENT, allEquipmentIDs);
      }

      if (_allPreviousEquipment.size() > 0) {

         final String[] allEquipmentIds = new String[_allPreviousEquipment.size()];
         int equipmentIndex = 0;

         for (final Equipment equipment : _allPreviousEquipment) {
            allEquipmentIds[equipmentIndex++] = Long.toString(equipment.getEquipmentId());
         }

         _state.put(STATE_PREVIOUS_EQUIPMENT, allEquipmentIds);
      }
   }

   /**
    * Create actions for recent equipment
    */
   private static void setupRecentActions() {

      _maxRecentEquipment = _prefStore.getInt(ITourbookPreferences.EQUIPMENT_NUMBER_OF_RECENT_EQUIPMENT);

      _allActions_RecentEquipment = new ActionRecentEquipment[_maxRecentEquipment];

      for (int actionIndex = 0; actionIndex < _allActions_RecentEquipment.length; actionIndex++) {
         _allActions_RecentEquipment[actionIndex] = new ActionRecentEquipment();
      }

      _actionWithAllPreviousEquipment = new ActionWithAllPreviousEquipment();
   }

   /**
    * Check if all previous equipment are contained in the modified equipment
    *
    * @param allModifiedEquipment
    */
   static void updatePreviousEquipmentState(final HashMap<Long, Equipment> allModifiedEquipment) {

      for (final Equipment previousEquipment : _allPreviousEquipment) {

         if (allModifiedEquipment.containsKey(previousEquipment.getEquipmentId()) == false) {

            _actionWithAllPreviousEquipment.setChecked(false);

            return;
         }
      }
   }

   /**
    * Replace equipment in recent equipment with freshly loaded equipment instance
    */
   public static void updateRecentEquipment() {

      final Map<Long, Equipment> allAvailableEquipment = EquipmentManager.getAllEquipment_ByID();

      final LinkedList<Equipment> allNewRecentEquipment = new LinkedList<>();

      for (final Equipment recentEquipment : _allRecentEquipment) {

         final long recentEquipmentID = recentEquipment.getEquipmentId();
         final Equipment availableEquipment = allAvailableEquipment.get(recentEquipmentID);

         allNewRecentEquipment.add(availableEquipment);
      }

      _allRecentEquipment = allNewRecentEquipment;
   }

   private void addEquipment(final Equipment equipment) {

      EquipmentManager.equipment_Add(

            equipment,
            _tourProvider,

            _isSaveTour,
            _isCheckTourEditor);

      replaceRecentEquipment(equipment);
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

      UI.showStatusLineMessage(Messages.Equipment_StatusLine_EquipmentCopiedToClipboard.formatted(_allEquipment_WhenCopied.size()));
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

      final ActionAddEquipment_SubMenu actionAddEquipment_AutoOpen_Default   = new ActionAddEquipment_SubMenu(this, null);
      final ActionAddEquipment_SubMenu actionAddEquipment_AutoOpen_Flat      = new ActionAddEquipment_SubMenu(this, null);
      final ActionAddEquipment_SubMenu actionAddEquipment_AutoOpen_Tree      = new ActionAddEquipment_SubMenu(this, null);

      _actionContribItem_AddEquipment_AutoOpen_Default   = new ActionContributionItem(actionAddEquipment_AutoOpen_Default);
      _actionContribItem_AddEquipment_AutoOpen_Flat      = new ActionContributionItem(actionAddEquipment_AutoOpen_Flat);
      _actionContribItem_AddEquipment_AutoOpen_Tree      = new ActionContributionItem(actionAddEquipment_AutoOpen_Tree);

      /**
       * VERY IMPORTANT: Without an ID, the auto open do NOT work
       */
      _actionContribItem_AddEquipment_AutoOpen_Default   .setId(ICommandIds.ACTION_ADD_EQUIPMENT_AUTO_OPEN_DEFAULT);
      _actionContribItem_AddEquipment_AutoOpen_Flat      .setId(ICommandIds.ACTION_ADD_EQUIPMENT_AUTO_OPEN_FLAT);
      _actionContribItem_AddEquipment_AutoOpen_Tree      .setId(ICommandIds.ACTION_ADD_EQUIPMENT_AUTO_OPEN_TREE);

      _advancedMenuToAddEquipment_Default   = new AdvancedMenuForActions(_actionContribItem_AddEquipment_AutoOpen_Default);
      _advancedMenuToAddEquipment_Flat      = new AdvancedMenuForActions(_actionContribItem_AddEquipment_AutoOpen_Flat);
      _advancedMenuToAddEquipment_Tree      = new AdvancedMenuForActions(_actionContribItem_AddEquipment_AutoOpen_Tree);

      _actionAddEquipment              = new ActionAddEquipment_SubMenu(this);
      _actionAddEquipment_Groups       = new ActionAddEquipmentGroups_SubMenu();
      _actionAddRecentEquipment        = new ActionAddRecentEquipment(this);
      _actionRemoveEquipment           = new ActionRemoveEquipment_SubMenu(this);
      _actionRemoveAllEquipment        = new ActionRemoveEquipmentAll();

      _actionClipboard_CopyEquipment   = new ActionClipboard_CopyEquipment();
      _actionClipboard_PasteEquipment  = new ActionClipboard_PasteEquipment();

      _actionEquipmentPreferences      = new ActionOpenPrefDialog(Messages.Equipment_Action_EquipmentPreferences, PrefPageEquipment.ID);
      _actionEquipmentGroupPreferences = new ActionOpenPrefDialog(Messages.Equipment_Action_ManageEquipmentGroups, PrefPageEquipmentGroups.ID);

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
    * @param allSelectedItems
    */
   public void enableActions(final List<Object> allSelectedItems) {


      final Set<Long> allSelectedEquipmentIDs_FromAllTours = new HashSet<>();

      boolean isEnabled_RemoveEquipment = false;

      int numTours = 0;

      if (allSelectedItems != null && allSelectedItems.size() > 0) {

         for (final Object selectedItem : allSelectedItems) {

            if (selectedItem instanceof final TVITourBookTour tourItem) {

               numTours++;

               final List<Long> allTourEquipmentIDs = tourItem.getEquipmentIds();

               if (allTourEquipmentIDs != null && allTourEquipmentIDs.size() > 0) {

                  allSelectedEquipmentIDs_FromAllTours.addAll(allTourEquipmentIDs);

                  isEnabled_RemoveEquipment = true;
               }

            } else if (selectedItem instanceof final TourData tourData) {

               // tourData are from the import view, collated view

               numTours++;

               final Set<Equipment> allEquipment = tourData.getEquipment();

               if (allEquipment != null && allEquipment.size() > 0) {

                  final List<Long> allEquipmentIDs = new ArrayList<>();

                  for (final Equipment equipment : allEquipment) {
                     allEquipmentIDs.add(equipment.getEquipmentId());
                  }

                  allSelectedEquipmentIDs_FromAllTours.addAll(allEquipmentIDs);

                  isEnabled_RemoveEquipment = true;
               }

            } else if (selectedItem instanceof final TVITaggingView_Tour tourItem) {

               final Set<Long> allEquipmentIDs = tourItem.allEquipmentIDs;

               if (allEquipmentIDs != null && allEquipmentIDs.size() > 0) {

                  allSelectedEquipmentIDs_FromAllTours.addAll(new ArrayList<>(allEquipmentIDs));

                  isEnabled_RemoveEquipment = true;
               }

               numTours++;

            } else if (selectedItem instanceof final TVIRefTour_ComparedTour tourItem) {

               final Set<Long> allEquipmentIDs = tourItem.allEquipmentIDs;

               if (allEquipmentIDs != null && allEquipmentIDs.size() > 0) {

                  allSelectedEquipmentIDs_FromAllTours.addAll(new ArrayList<>(allEquipmentIDs));

                  isEnabled_RemoveEquipment = true;
               }

               numTours++;

            } else if (selectedItem instanceof final TVIEquipmentView_Tour tourItem) {

               numTours++;

               final List<Long> allTourEquipmentIDs = tourItem.getEquipmentIds();

               if (allTourEquipmentIDs != null && allTourEquipmentIDs.size() > 0) {

                  allSelectedEquipmentIDs_FromAllTours.addAll(allTourEquipmentIDs);

                  isEnabled_RemoveEquipment = true;
               }
            }
         }
      }

      final boolean isEnabled_AddEquipment = numTours > 0;

      final int numClipboardEquipment = setupPasteAction();
      final int numSelectedItems = allSelectedItems.size();

      _allEquipmentIds_OneTour.clear();

      if (numTours == 1) {
         _allEquipmentIds_OneTour.addAll(allSelectedEquipmentIDs_FromAllTours);
      }

      enableActions_Equipment(isEnabled_AddEquipment, isEnabled_RemoveEquipment, numClipboardEquipment);

      enableActions_Recent(
            isEnabled_AddEquipment,
            numSelectedItems,
            allSelectedEquipmentIDs_FromAllTours,
            null);
   }

   private void enableActions_Equipment(final boolean isEnabled_AddEquipment,
                                        final boolean isEnabled_RemoveEquipment,
                                        final int numClipboardEquipment) {
// SET_FORMATTING_OFF

      _actionAddEquipment              .setEnabled(isEnabled_AddEquipment);
      _actionAddEquipment_Groups       .setEnabled(isEnabled_AddEquipment);

      _actionRemoveEquipment           .setEnabled(isEnabled_RemoveEquipment);
      _actionRemoveAllEquipment        .setEnabled(isEnabled_RemoveEquipment);

      _actionClipboard_CopyEquipment   .setEnabled(isEnabled_RemoveEquipment);
      _actionClipboard_PasteEquipment  .setEnabled(numClipboardEquipment > 0);

// SET_FORMATTING_ON
   }

   private void enableActions_OneTour() {

      final Map<Long, Equipment> allUsedEquipments = getAllUsedEquipments();

      final int numClipboardEquipment = setupPasteAction();

      final boolean isEnabled_RemoveEquipment = allUsedEquipments.size() > 0;

      enableActions_Equipment(true, isEnabled_RemoveEquipment, numClipboardEquipment);
      enableActions_Recent();
   }

   private void enableActions_Recent() {

      // get all equipment from all tours
      final HashSet<Equipment> allUsedEquipment = new HashSet<>();
      final List<TourData> allSelectedTours = getSelectedTours();

      for (final TourData tourData : allSelectedTours) {
         allUsedEquipment.addAll(tourData.getEquipment());
      }

      for (final ActionRecentEquipment actionRecentEquipment : _allActions_RecentEquipment) {

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

   /**
    * Add and save multiple equipment in the selected tours
    *
    * @param allModifiedEquipment
    */
   void equipment_Add(final Collection<Equipment> allModifiedEquipment) {

      EquipmentManager.equipment_Add(

            allModifiedEquipment,
            _tourProvider,

            _isSaveTour,
            _isCheckTourEditor);

      updateRecentEquipment(allModifiedEquipment);
      updatePreviousEquipment(allModifiedEquipment);
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
   public void fillActions(final IMenuManager menuManager,
                           final ITourProvider tourProvider) {

      _currentInstance = this;

      fillEquipmentMenu_WithRecentEquipment(menuManager, null);
   }

   /**
    * Add all equipment actions, this is called from the {@link TourDataEditorView}
    *
    * @param menuManager
    */
   public void fillEquipmentMenu(final IMenuManager menuManager) {

      _currentInstance = this;

      menuManager.add(new Separator());
      {
         menuManager.add(_actionAddEquipment);
         menuManager.add(_actionAddEquipment_Groups);

         fillEquipmentMenu_WithRecentEquipment(menuManager, null);

         menuManager.add(_actionRemoveEquipment);
         menuManager.add(_actionRemoveAllEquipment);

         menuManager.add(_actionClipboard_CopyEquipment);
         menuManager.add(_actionClipboard_PasteEquipment);

         menuManager.add(new Separator());

         menuManager.add(_actionEquipmentPreferences);
      }

      enableActions_OneTour();

      _isAdvMenu = false;
   }

   public void fillEquipmentMenu_WithActiveActions(final IMenuManager menuMgr,
                                                   final ITourProvider tourProvider) {

      fillEquipmentMenu_WithActiveActions(menuMgr, tourProvider, null);
   }

   public void fillEquipmentMenu_WithActiveActions(final IMenuManager menuMgr,
                                                   final ITourProvider tourProvider,
                                                   final Boolean isFlatView) {

      _currentInstance = this;

      updateEquipmentAutoOpenAction(isFlatView);

      menuMgr.add(new Separator());

      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.EQUIPMENT, _allEquipmentActions, tourProvider);

      _isAdvMenu = false;
   }

   /**
    * Fill recent equipment into the menu, either with a {@link IMenuManager} or with a {@link Menu}
    *
    * @param menuManager
    * @param menu
    */
   void fillEquipmentMenu_WithRecentEquipment(final IMenuManager menuManager, final Menu menu) {

      if (_allRecentEquipment.isEmpty()) {
         return;
      }

      if (_maxRecentEquipment < 1) {
         return;
      }

      // add all previous equipment
      final int numPreviousEquipment = _allPreviousEquipment.size();
      if (numPreviousEquipment > 0) {

         final Collection<Equipment> allPreviousEquipment = _allPreviousEquipment;

         // check if the first previous equipment is the same as the first recent equipment
         if (numPreviousEquipment > 1
               || allPreviousEquipment.iterator().next().equals(_allRecentEquipment.getFirst()) == false) {

            final StringBuilder sb = new StringBuilder();
            final StringBuilder sbTooltip = new StringBuilder();

            boolean isFirst = true;

            for (final Equipment recentEquipment : allPreviousEquipment) {

               if (isFirst) {
                  isFirst = false;
               } else {
                  sb.append(UI.COMMA_SPACE);
                  sbTooltip.append(UI.NEW_LINE);
               }

               sb.append(recentEquipment.getName());
               sbTooltip.append(recentEquipment.getName());
            }

            String equipmentText = sb.toString();
            String equipmentTextTooltip = sbTooltip.toString();

            if (UI.IS_SCRAMBLE_DATA) {

               equipmentText = UI.scrambleText(equipmentText);
               equipmentTextTooltip = UI.scrambleText(equipmentTextTooltip);
            }

            final int maxTextWidth = 40;

            if (equipmentText.length() > maxTextWidth) {

               equipmentText = UI.shortenText(equipmentText, maxTextWidth, true);
            }

            final ActionContributionItem actionContributionItem = new ActionContributionItem(_actionWithAllPreviousEquipment);

            if (menu == null) {

               _actionWithAllPreviousEquipment.setText(UI.SPACE4 + UI.MNEMONIC + 0 + UI.SPACE2 + equipmentText);
               _actionWithAllPreviousEquipment.setToolTipText(equipmentTextTooltip);

               menuManager.add(actionContributionItem);

            } else {

               _actionWithAllPreviousEquipment.setText(UI.MNEMONIC + 0 + UI.SPACE2 + equipmentText);
               _actionWithAllPreviousEquipment.setToolTipText(equipmentTextTooltip);

               actionContributionItem.fill(menu, -1);
            }
         }
      }

      // add all recent equipment
      int equipmentIndex = 0;
      for (final ActionRecentEquipment actionRecentEquipment : _allActions_RecentEquipment) {

         if (equipmentIndex >= _allRecentEquipment.size()) {

            // there are no more recent equipment

            break;
         }

         final Equipment equipment = _allRecentEquipment.get(equipmentIndex);

         String equipmentText = equipment.getName();

         if (UI.IS_SCRAMBLE_DATA) {

            equipmentText = UI.scrambleText(equipmentText);
         }

         if (menu == null) {

            actionRecentEquipment.updateEquipmentAction(equipment, (UI.SPACE4 + UI.MNEMONIC + (equipmentIndex + 1) + UI.SPACE2 + equipmentText));

            menuManager.add(new ActionContributionItem(actionRecentEquipment));

         } else {

            actionRecentEquipment.updateEquipmentAction(equipment, (UI.MNEMONIC + (equipmentIndex + 1) + UI.SPACE2 + equipmentText));

            new ActionContributionItem(actionRecentEquipment).fill(menu, -1);
         }

         equipmentIndex++;
      }
   }

   public Map<String, Object> getAllEquipmentActions() {

      return getAllEquipmentActions(null);
   }

   public Map<String, Object> getAllEquipmentActions(final Boolean isFlatView) {

      updateEquipmentAutoOpenAction(isFlatView);

      return _allEquipmentActions;
   }

   /**
    * @return Returns all equipment from all selected tours
    */
   private Map<Long, Equipment> getAllUsedEquipments() {

      final Map<Long, Equipment> allUsedEquipment = new HashMap<>();

      final List<TourData> allSelectedTours = getSelectedTours();

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

   public LinkedList<Equipment> getRecentEquipment() {

      return _allRecentEquipment;
   }

   private Map<Long, Equipment> getSelectedEquipment() {

      final Map<Long, Equipment> allEquipment_Selected = new HashMap<>();

      final List<TourData> allSelectedTours = getSelectedTours();

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

   private List<TourData> getSelectedTours() {

      final List<TourData> allSelectedTours = _tourProvider.getSelectedTours();

      _allEquipmentIds_OneTour.clear();

      if (allSelectedTours.size() == 1) {

         final TourData tourData = allSelectedTours.get(0);

         for (final Long equipmentID : tourData.getEquipmentIds()) {
            _allEquipmentIds_OneTour.add(equipmentID);
         }
      }

      return allSelectedTours;
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

   /**
    * This is called when the menu is hidden which contains the equipment actions
    */
   public void onHideMenu() {

      _advancedMenuToAddEquipment_Current.onHideParentMenu();
   }

   /**
    * This is called when the menu is displayed which contains the equipment actions
    *
    * @param menuEvent
    * @param menuParentControl
    * @param menuPosition
    * @param toolTip
    */
   public void onShowMenu(final MenuEvent menuEvent,
                          final Control menuParentControl,
                          final Point menuPosition,
                          final ToolTip toolTip) {

      onShowMenu(

            menuEvent,
            menuParentControl,
            menuPosition,

            toolTip,
            null);
   }

   /**
    * This is called when the menu is displayed which contains the equipment actions
    *
    * @param menuEvent
    * @param menuParentControl
    * @param menuPosition
    * @param toolTip
    * @param isFlatView
    */
   public void onShowMenu(final MenuEvent menuEvent,
                          final Control menuParentControl,
                          final Point menuPosition,
                          final ToolTip toolTip,
                          final Boolean isFlatView) {

      updateEquipmentAutoOpenAction(isFlatView);

      _advancedMenuToAddEquipment_Current.onShowParentMenu(

            menuEvent,
            menuParentControl,

            _isTaggingAutoOpen,
            _isTaggingAnimation,
            _taggingAutoOpenDelay,

            menuPosition,
            toolTip);
   }

   /**
    * Replace an equipment with an updated equipment
    *
    * @param equipment
    */
   void replaceRecentEquipment(final Equipment equipment) {

      _allRecentEquipment.remove(equipment);
      _allRecentEquipment.addFirst(equipment);
   }

   void setIsAdvanceMenu() {

      _isAdvMenu = true;
   }

   /**
    * @return Returns number of equipment in the clipboard
    */
   private int setupPasteAction() {

      final List<Equipment> allEquipmentInClipboard = getEquipmentFromClipboard();
      final int numEquipment = allEquipmentInClipboard != null ? allEquipmentInClipboard.size() : 0;

      if (numEquipment > 0) {

         _actionClipboard_PasteEquipment.setToolTipText(Messages.Equipment_Action_Paste_Tooltip
               .formatted(EquipmentGroupManager.createEquipmentSortedList(null, allEquipmentInClipboard)));
      }

      return numEquipment;
   }

   /**
    * Replace the action "add equipment auto open" with the default, flat or categorized action
    * <p>
    * This is a fix for https://github.com/mytourbook/mytourbook/issues/1603
    *
    * @param isFlatView
    */
   private void updateEquipmentAutoOpenAction(final Boolean isFlatView) {

      _allEquipmentActions.remove(ACTION_KEY__ADD_EQUIPMENT_AUTO_OPEN_DEFAULT);
      _allEquipmentActions.remove(ACTION_KEY__ADD_EQUIPMENT_AUTO_OPEN_FLAT);
      _allEquipmentActions.remove(ACTION_KEY__ADD_EQUIPMENT_AUTO_OPEN_TREE);

// SET_FORMATTING_OFF

      if (isFlatView == null) {

         // context menu in dialogs

         _allEquipmentActions.put(ACTION_KEY__ADD_EQUIPMENT_AUTO_OPEN_DEFAULT, _actionContribItem_AddEquipment_AutoOpen_Default);

         _actionContribItem_AddEquipment_AutoOpen_Current   = _actionContribItem_AddEquipment_AutoOpen_Default;
         _advancedMenuToAddEquipment_Current                = _advancedMenuToAddEquipment_Default;

      } else if (isFlatView) {

         // flat tourbook view

         _allEquipmentActions.put(ACTION_KEY__ADD_EQUIPMENT_AUTO_OPEN_FLAT, _actionContribItem_AddEquipment_AutoOpen_Flat);

         _actionContribItem_AddEquipment_AutoOpen_Current   = _actionContribItem_AddEquipment_AutoOpen_Flat;
         _advancedMenuToAddEquipment_Current                = _advancedMenuToAddEquipment_Flat;

      } else {

         // tree views

         _allEquipmentActions.put(ACTION_KEY__ADD_EQUIPMENT_AUTO_OPEN_TREE, _actionContribItem_AddEquipment_AutoOpen_Tree);

         _actionContribItem_AddEquipment_AutoOpen_Current   = _actionContribItem_AddEquipment_AutoOpen_Tree;
         _advancedMenuToAddEquipment_Current                = _advancedMenuToAddEquipment_Tree;
      }

// SET_FORMATTING_ON
   }

   private void updatePreviousEquipment(final Collection<Equipment> allModifiedEquipment) {

      _allPreviousEquipment.clear();

      for (final Equipment equipment : allModifiedEquipment) {
         _allPreviousEquipment.add(equipment);
      }
   }

   /**
    * Replace all recent equipment
    *
    * @param allModifiedEquipment
    */
   private void updateRecentEquipment(final Collection<Equipment> allModifiedEquipment) {

      for (final Equipment equipment : allModifiedEquipment) {
         replaceRecentEquipment(equipment);
      }
   }
}
