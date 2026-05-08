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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.util.AdvancedMenuForActions;
import net.tourbook.common.util.IAdvancedMenuForActions;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;
import net.tourbook.data.TourData;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Add equipment into the selected tours
 */
public class ActionAddEquipment_SubMenu extends Action implements IMenuCreator, IAdvancedMenuForActions {

   private static final char        NL                     = UI.NEW_LINE;

   private static final String      SPACE_PRE_EQUIPMENT    = "   ";                        //$NON-NLS-1$

   private EquipmentMenuManager     _equipmentMenuManager;
   private Menu                     _menu;

   private List<TourData>           _allSelectedTours;
   private final Set<Long>          _allUsedEquipmentIDs   = new HashSet<>();

   private ActionShowEquipmentView  _actionManageEquipment = new ActionShowEquipmentView();

   /**
    * Contains all equipment which will be added
    */
   private HashMap<Long, Equipment> _allModifiedEquipment  = new HashMap<>();

   private boolean                  _isAdvancedMenu;
   private AdvancedMenuForActions   _advancedMenuProvider;

   private ActionOK                 _actionOK;

   private Action                   _actionTitle_AddEquipment;
   private Action                   _actionTitle_ModifiedEquipment;
   private Action                   _actionTitle_RecentEquipment;

   private final class ActionCancel extends Action {

      private ActionCancel() {

         super(Messages.Action_Tag_AutoOpenCancel);

         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Close));
      }

      @Override
      public void run() {
         resetData();
      }
   }

   private class ActionEquipment extends Action {

      private final Equipment __equipment;

      public ActionEquipment(final Equipment equipment) {

         super(equipment.getName(), AS_CHECK_BOX);

         __equipment = equipment;
      }

      @Override
      public void run() {

         if (_isAdvancedMenu) {

            setEquipment(isChecked(), __equipment);

         } else {

            EquipmentManager.equipment_Add(

                  __equipment,
                  _equipmentMenuManager.getTourProvider(),

                  _equipmentMenuManager.isSaveTour(),
                  _equipmentMenuManager.isCheckTourEditor());

            _equipmentMenuManager.updateRecentEquipment(__equipment);
         }
      }

      @Override
      public String toString() {

         return UI.EMPTY_STRING

               + "ActionEquipment" + NL //                     //$NON-NLS-1$
               + " __equipment = " + __equipment + NL //       //$NON-NLS-1$
         ;
      }
   }

   private class ActionModifyEquipment extends Action {

      private final Equipment __equipment;

      public ActionModifyEquipment(final Equipment equipment) {

         super(SPACE_PRE_EQUIPMENT + equipment.getName(), AS_CHECK_BOX);

         __equipment = equipment;

         // this equipment is always checked, unchecking it will also remove it
         setChecked(true);
      }

      @Override
      public void run() {

         // uncheck/remove this equipment
         _allModifiedEquipment.remove(__equipment.getEquipmentId());

         // reopen action menu
         _advancedMenuProvider.openAdvancedMenu();
      }
   }

   private final class ActionOK extends Action {

      private ActionOK() {

         super(Messages.Action_Tag_AutoOpenOK);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.State_OK));
      }

      @Override
      public void run() {

         saveEquipment();
      }
   }

   private class ActionShowEquipmentView extends Action {

      public ActionShowEquipmentView() {

         super(Messages.Equipment_Action_ManageEquipment, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.Equipment));
      }

      @Override
      public void run() {

         Util.showView(EquipmentView.ID, true);
      }
   }

   protected ActionAddEquipment_SubMenu(final EquipmentMenuManager equipmentMenuManager) {

      super(Messages.Equipment_Action_AddEquipment, AS_DROP_DOWN_MENU);

      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.Equipment_Only));

      _equipmentMenuManager = equipmentMenuManager;

      setMenuCreator(this);
   }

   /**
    * This constructor creates a push button action without a drop down menu
    *
    * @param equipmentMenuManager
    * @param isForAutoOpen
    *           This parameter is not used but it indicates that the menu auto open behavior is
    *           used and a menuCreator is not set
    */
   ActionAddEquipment_SubMenu(final EquipmentMenuManager equipmentMenuManager, final Object isForAutoOpen) {

      super("&Add Equipment...", AS_PUSH_BUTTON);

      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.Equipment_New));

      _equipmentMenuManager = equipmentMenuManager;

      createDefaultActions();
   }

   private void addActionToMenu(final Menu menu, final Action action) {

      final ActionContributionItem item = new ActionContributionItem(action);
      item.fill(menu, -1);
   }

   private void addSeparatorToMenu(final Menu menu) {

//      new MenuItem(menu, SWT.SEPARATOR);

      (new Separator()).fill(menu, -1);
   }

   private void createDefaultActions() {

      _actionTitle_AddEquipment = new Action("» Add Equipment «") {};
      _actionTitle_AddEquipment.setEnabled(false);

      _actionTitle_ModifiedEquipment = new Action("» Equipment which will be added with OK «") {};
      _actionTitle_ModifiedEquipment.setEnabled(false);

      _actionTitle_RecentEquipment = new Action("» Recently added Equipment «") {};
      _actionTitle_RecentEquipment.setEnabled(false);

      _actionOK = new ActionOK();
   }

   @Override
   public void dispose() {

      if (_menu != null) {
         _menu.dispose();
         _menu = null;
      }
   }

   private void enableActions() {

      final boolean isModifiedEquipment = _allModifiedEquipment.size() > 0;

      _actionOK.setEnabled(isModifiedEquipment);
      _actionOK.setImageDescriptor(isModifiedEquipment
            ? TourbookPlugin.getImageDescriptor(Images.State_OK)
            : null);
   }

   public void fillMenu(final Menu menu) {

      // dispose old items
      final MenuItem[] allMenuItems = menu.getItems();
      for (final MenuItem item : allMenuItems) {
         item.dispose();
      }

      // check if a tour is selected
      _allSelectedTours = _equipmentMenuManager.getTourProvider().getSelectedTours();
      if (_allSelectedTours == null || _allSelectedTours.isEmpty()) {
         // a tour is not selected
         return;
      }

      // get all equipment from all tours
      _allUsedEquipmentIDs.clear();

      for (final TourData tourData : _allSelectedTours) {

         final Set<Equipment> allEquipment = tourData.getEquipment();

         for (final Equipment equipment : allEquipment) {
            _allUsedEquipmentIDs.add(equipment.getEquipmentId());
         }
      }

      fillMenu_AllActions(menu);
   }

   /**
    * Display all newly added equipment which are not yet set in the tours
    *
    * @param menu
    */
   private void fillMenu_AddedEquipmentButNotYetSaved(final Menu menu) {

      final boolean isModifiedEquipment = _allModifiedEquipment.size() > 0;

      if (isModifiedEquipment == false) {
         return;
      }

      addSeparatorToMenu(menu);
      {

         addActionToMenu(menu, _actionTitle_ModifiedEquipment);

         // create actions
         final ArrayList<Equipment> allModifiedEquipment = new ArrayList<>(_allModifiedEquipment.values());
         Collections.sort(allModifiedEquipment);

         for (final Equipment equipment : allModifiedEquipment) {
            addActionToMenu(menu, new ActionModifyEquipment(equipment));
         }
      }
   }

   /**
    * Fill all actions
    *
    * @param menu
    */
   private void fillMenu_AllActions(final Menu menu) {

      if (_isAdvancedMenu == false) {

         fillMenu_EquipmentActions(menu);

         addSeparatorToMenu(menu);
         {
            addActionToMenu(menu, _actionManageEquipment);
         }

      } else {

         /*
          * These actions are managed by the advanced menu provider
          */

         // create title menu items

         addActionToMenu(menu, _actionTitle_AddEquipment);

         addSeparatorToMenu(menu);
         {
            fillMenu_EquipmentActions(menu);
         }

         fillMenu_RecentEquipment(menu);

         fillMenu_AddedEquipmentButNotYetSaved(menu);

         addSeparatorToMenu(menu);
         {
            addActionToMenu(menu, _actionOK);
            addActionToMenu(menu, new ActionCancel());
         }

         addSeparatorToMenu(menu);
         {
            addActionToMenu(menu, _actionManageEquipment);
         }

         enableActions();
      }
   }

   private void fillMenu_EquipmentActions(final Menu menu) {

      final int numSelectedTour = _allSelectedTours.size();

      final List<Equipment> allEquipment = EquipmentManager.getAllEquipment_Name();

      for (final Equipment equipment : allEquipment) {

         if (equipment.isRetired()) {

            // skip retired equipment https://github.com/mytourbook/mytourbook/issues/1660
            continue;
         }

         final ActionEquipment action = new ActionEquipment(equipment);

         if (numSelectedTour == 1) {

            // disable action only when one tour is selected

            final boolean isEquipmentAlreadySet = _allUsedEquipmentIDs.contains(equipment.getEquipmentId());

            if (isEquipmentAlreadySet) {
               action.setEnabled(false);
            }
         }

         addActionToMenu(menu, action);
      }
   }

   /**
    * @param menu
    */
   private void fillMenu_RecentEquipment(final Menu menu) {

      if (_equipmentMenuManager.getRecentEquipment().size() > 0) {

         // recent equipment are available

         addSeparatorToMenu(menu);
         {
            addActionToMenu(menu, _actionTitle_RecentEquipment);

            _equipmentMenuManager.fillEquipmentMenu_WithRecentEquipment(null, menu);
         }
      }
   }

   @Override
   public Menu getMenu(final Control parent) {

      // fix: https://github.com/mytourbook/mytourbook/issues/1154
      if (parent == null) {
         return null;
      }

      _isAdvancedMenu = true;

      dispose();

      _menu = new Menu(parent);

      // Add listener to repopulate the menu each time
      _menu.addMenuListener(MenuListener.menuShownAdapter(menuEvent -> fillMenu((Menu) menuEvent.widget)));

      return _menu;
   }

   @Override
   public Menu getMenu(final Menu parent) {

      _isAdvancedMenu = false;

      dispose();

      _menu = new Menu(parent);

      // Add listener to repopulate the menu each time
      _menu.addMenuListener(MenuListener.menuShownAdapter(menuEvent -> {

         resetData();

         fillMenu((Menu) menuEvent.widget);
      }));

      return _menu;
   }

   @Override
   public void onShowMenu() {

      _equipmentMenuManager.setIsAdvanceMenu();

      final int unknown = 1;
      EquipmentMenuManager.enableActions_Recent(true, _allModifiedEquipment.keySet(), unknown);
   }

   @Override
   public void resetData() {

      _allModifiedEquipment.clear();
   }

   @Override
   public void run() {

      _advancedMenuProvider.openAdvancedMenu();
   }

   private void saveEquipment() {

      if (_allModifiedEquipment.size() > 0) {

         _equipmentMenuManager.equipment_Add(_allModifiedEquipment.values());
      }
   }

   private void saveOrReopenEquipmentMenu(final boolean isAddEquipment) {

      if (_isAdvancedMenu) {

         if (isAddEquipment == false) {

            /*
             * It is possible that a equipment was removed which is contained within the previous
             * equipment, uncheck this action that is displays the correct checked equipment
             */
            EquipmentMenuManager.updatePreviousEquipmentState(_allModifiedEquipment);
         }

         // reopen action menu
         _advancedMenuProvider.openAdvancedMenu();

      } else {

         saveEquipment();
      }
   }

   @Override
   public void setAdvancedMenuProvider(final AdvancedMenuForActions advancedMenuProvider) {

      _advancedMenuProvider = advancedMenuProvider;
   }

   /**
    * Add or remove an equipment
    *
    * @param isAddEquipment
    * @param equipment
    */
   void setEquipment(final boolean isAddEquipment, final Equipment equipment) {

      if (isAddEquipment) {

         // add equipment

         _allModifiedEquipment.put(equipment.getEquipmentId(), equipment);

      } else {

         // remove equipment

         _allModifiedEquipment.remove(equipment.getEquipmentId());
      }

      saveOrReopenEquipmentMenu(isAddEquipment);
   }

   /**
    * Add/Remove all previous equipment
    *
    * @param isAddEquipment
    * @param allPreviousEquipment
    */
   void setEquipment(final boolean isAddEquipment, final Map<Long, Equipment> allPreviousEquipment) {

      for (final Equipment equipment : allPreviousEquipment.values()) {

         if (isAddEquipment) {

            // add equipment

            _allModifiedEquipment.put(equipment.getEquipmentId(), equipment);

         } else {

            // remove equipment

            _allModifiedEquipment.remove(equipment.getEquipmentId());
         }
      }

      saveOrReopenEquipmentMenu(isAddEquipment);
   }
}
