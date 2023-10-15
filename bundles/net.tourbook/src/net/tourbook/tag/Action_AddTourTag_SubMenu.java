/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.AdvancedMenuForActions;
import net.tourbook.common.util.IAdvancedMenuForActions;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.PrefPageTags;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ArmEvent;
import org.eclipse.swt.events.ArmListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TypedListener;

/**
 * Add tag(s) from the selected tours
 */
class Action_AddTourTag_SubMenu extends Action implements IMenuCreator, IAdvancedMenuForActions {

   private static final String      SPACE_PRE_TAG       = "   ";                   //$NON-NLS-1$

   private TagMenuManager           _tagMenuMgr;

   /**
    * Contains all tags for all selected tours in the viewer
    */
   private Set<TourTag>             _selectedTourTags   = new HashSet<>();
   private ArrayList<TourData>      _selectedTours;

   /**
    * Contains all tags which will be added
    */
   private HashMap<Long, TourTag>   _modifiedTags       = new HashMap<>();

   private boolean                  _isAdvancedMenu;

   private AdvancedMenuForActions   _advancedMenuProvider;

   private final ContextArmListener _contextArmListener = new ContextArmListener();

   private Action                   _actionAdvanced_AddTagTitle;
   private ActionAdvanced_OK        _actionAdvanced_OK;
   private ActionOpenPrefDialog     _actionAdvanced_OpenTagPrefs;
   private Action                   _actionAdvanced_RecentTagsTitle;

   /*
    * UI controls
    */
   private Menu _rootMenu;

   private class ActionAdvanced_Cancel extends Action {

      private ActionAdvanced_Cancel() {

         super(Messages.Action_Tag_AutoOpenCancel);

         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Close));
      }

      @Override
      public void run() {
         resetData();
      }
   }

   private class ActionAdvanced_ModifiedTag extends Action {

      private final TourTag __tourTag;

      public ActionAdvanced_ModifiedTag(final TourTag tourTag) {

         super(SPACE_PRE_TAG + tourTag.getTagName(), AS_CHECK_BOX);

         __tourTag = tourTag;

         // this tag is always checked, unchecking it will also remove it
         setChecked(true);
      }

      @Override
      public void run() {

         // uncheck/remove this tag
         _modifiedTags.remove(__tourTag.getTagId());

         // reopen action menu
         _advancedMenuProvider.openAdvancedMenu();
      }
   }

   private class ActionAdvanced_ModifiedTags extends Action {

      public ActionAdvanced_ModifiedTags() {

         super(Messages.Action_Tag_Add_AutoOpen_ModifiedTags);

         // this action is only for info
         super.setEnabled(false);
      }
   }

   private class ActionAdvanced_OK extends Action {

      private ActionAdvanced_OK() {

         super(Messages.Action_Tag_AutoOpenOK);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_OK));
      }

      @Override
      public void run() {
         saveTags();
      }
   }

   private class ActionTourTag extends Action {

      private final TourTag __tourTag;

      public ActionTourTag(final TourTag tourTag) {

         super(tourTag.getTagName(), AS_CHECK_BOX);

         final Image tagImage = TagManager.getTagImage(tourTag);

         if (tagImage != null) {
            setImageDescriptor(ImageDescriptor.createFromImage(tagImage));
         }

         __tourTag = tourTag;

         System.out.println(UI.timeStamp() + " create Tag: " + tourTag);
// TODO remove SYSTEM.OUT.PRINTLN

      }

      @Override
      public void run() {

         System.out.println(UI.timeStamp() + " run()");
// TODO remove SYSTEM.OUT.PRINTLN

         if (_isAdvancedMenu == false) {

            // add tag
            _modifiedTags.put(__tourTag.getTagId(), __tourTag);

            saveTags();

         } else {

            setTourTag(isChecked(), __tourTag);
         }
      }
   }

   /**
    *
    */
   private class ActionTourTagCategory extends Action implements IMenuCreator {

      private Menu                            __categoryMenu;

      private final Action_AddTourTag_SubMenu __actionAddTourTag;
      private final TourTagCategory           __tagCategory;

      public ActionTourTagCategory(final Action_AddTourTag_SubMenu actionAddTourTag, final TourTagCategory tagCategory) {

         super(tagCategory.getCategoryName(), AS_DROP_DOWN_MENU);

         __actionAddTourTag = actionAddTourTag;
         __tagCategory = tagCategory;

         System.out.println(UI.timeStamp() + " create Category: " + tagCategory.getCategoryName());
// TODO remove SYSTEM.OUT.PRINTLN

         setMenuCreator(this);
      }

      private void addArmListener(final Menu menu) {
         // TODO Auto-generated method stub

         // add arm listener to each menu item
         for (final MenuItem menuItem : menu.getItems()) {

            /*
             * check if an arm listener is already set
             */
            final Listener[] itemArmListeners = menuItem.getListeners(SWT.Arm);
            boolean isArmAvailable = false;

            for (final Listener listener : itemArmListeners) {
               if (listener instanceof TypedListener) {
                  if (((TypedListener) listener).getEventListener() instanceof ContextArmListener) {
                     isArmAvailable = true;
                     break;
                  }
               }
            }

            if (isArmAvailable == false) {
               menuItem.addArmListener(_contextArmListener);
            }
         }
      }

      @Override
      public void dispose() {

         if (__categoryMenu != null) {

            __categoryMenu.dispose();
            __categoryMenu = null;
         }
      }

      @Override
      public Menu getMenu(final Control parent) {
         return null;
      }

      @Override
      public Menu getMenu(final Menu parent) {

         dispose();

         __categoryMenu = new Menu(parent);

         // Add listener to repopulate the menu each time
         __categoryMenu.addMenuListener(new MenuListener() {

            @Override
            public void menuHidden(final MenuEvent menuEvent) {
               // TODO Auto-generated method stub

               final Menu menu = (Menu) menuEvent.widget;

               System.out.println(UI.NEW_LINE + UI.timeStamp() + " menuHidden: " + menu);
// TODO remove SYSTEM.OUT.PRINTLN
            }

            @Override
            public void menuShown(final MenuEvent menuEvent) {
               // TODO Auto-generated method stub

               final Menu menu = (Menu) menuEvent.widget;

               System.out.println(UI.NEW_LINE + UI.timeStamp() + " menuShown: " + menu);
// TODO remove SYSTEM.OUT.PRINTLN

               // dispose old menu items
               Arrays.stream(menu.getItems()).forEach(menuItem -> menuItem.dispose());

               final TagCollection tagCollection = TourDatabase.getTagEntries(__tagCategory.getCategoryId());

               // add actions
               __actionAddTourTag.createTagCategoryActions(tagCollection, __categoryMenu);
               __actionAddTourTag.createTagActions(tagCollection, __categoryMenu);

               addArmListener(menu);
            }
         });

         return __categoryMenu;
      }
   }

   private class ContextArmListener implements ArmListener {

      @Override
      public void widgetArmed(final ArmEvent event) {
         onArmEvent(event);
      }
   }

   /**
    * @param tagMenuManager
    */
   Action_AddTourTag_SubMenu(final TagMenuManager tagMenuManager) {

      super(Messages.action_tag_add, AS_DROP_DOWN_MENU);

      createDefaultAction(tagMenuManager);

      setMenuCreator(this);
   }

   /**
    * This constructor creates a push button action without a drop down menu
    *
    * @param tagMenuMgr
    * @param isAutoOpen
    *           This parameter is ignored but it indicates that the menu auto open behavior is
    *           used.
    */
   Action_AddTourTag_SubMenu(final TagMenuManager tagMenuMgr, final Object isAutoOpen) {

      super(Messages.Action_Tag_Add_AutoOpen, AS_PUSH_BUTTON);

      createDefaultAction(tagMenuMgr);
   }

   private void addActionToMenu(final Menu menu, final Action action) {

      final ActionContributionItem item = new ActionContributionItem(action);
      item.fill(menu, -1);
   }

   private void createDefaultAction(final TagMenuManager tagMenuMgr) {

      _tagMenuMgr = tagMenuMgr;

      _actionAdvanced_AddTagTitle = new Action(Messages.Action_Tag_Add_AutoOpen_Title) {};
      _actionAdvanced_AddTagTitle.setEnabled(false);

      _actionAdvanced_RecentTagsTitle = new Action(Messages.Action_Tag_Add_RecentTags) {};
      _actionAdvanced_RecentTagsTitle.setEnabled(false);

      _actionAdvanced_OK = new ActionAdvanced_OK();

      _actionAdvanced_OpenTagPrefs = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure, PrefPageTags.ID);
   }

   private void createTagActions(final TagCollection tagCollection, final Menu menu) {

      final ArrayList<TourTag> allTourTags = tagCollection.tourTags;
      if (allTourTags == null) {
         return;
      }

      //Preload the tag images
      //Note that the hourglass is only displayed on Windows (it doesn't seem
      //to work on Linux)
//      BusyIndicator.showWhile(Display.getCurrent(), () -> allTourTags.forEach(tourTag -> TagManager.getTagImage(tourTag)));

      // add tag items
      for (final TourTag menuTourTag : allTourTags) {

         final boolean isModifiedTags = _modifiedTags.size() > 0;
         final boolean isSelectedTags = _selectedTourTags != null;

         boolean isTagChecked = false;
         final boolean isOneTour = _selectedTours != null && _selectedTours.size() == 1;
         boolean isModifiedTag = false;

         if (isSelectedTags && isOneTour) {

            /*
             * only when one tour is selected check the tag otherwise it's confusing, a
             * three-state check could solve this problem but is not available
             */

            final long tagId = menuTourTag.getTagId();

            if (isSelectedTags) {

               for (final TourTag selectedTourTag : _selectedTourTags) {
                  if (selectedTourTag.getTagId() == tagId) {
                     isTagChecked = true;
                     break;
                  }
               }
            }

         }

         if (isModifiedTags) {

            if (isTagChecked == false && _modifiedTags.containsValue(menuTourTag)) {
               isTagChecked = true;
               isModifiedTag = true;
            }

         }

         final ActionTourTag actionTourTag = new ActionTourTag(menuTourTag);

         // check the tag when it's set in the tour
         actionTourTag.setChecked(isTagChecked);

         // disable tags which are not tagged
         if (isModifiedTag) {

            // modified tags are always enabled

         } else if (isOneTour) {
            actionTourTag.setEnabled(!isTagChecked);
         }

         addActionToMenu(menu, actionTourTag);
      }
   }

   private void createTagCategoryActions(final TagCollection tagCollection, final Menu menu) {

      // add tag categories
      for (final TourTagCategory tagCategory : tagCollection.tourTagCategories) {
         addActionToMenu(menu, new ActionTourTagCategory(this, tagCategory));
      }
   }

   @Override
   public void dispose() {

      disposeRootMenu();
   }

   private void disposeRootMenu() {

      if (_rootMenu != null) {
         _rootMenu.dispose();
         _rootMenu = null;
      }
   }

   /**
    * @param menu
    */
   private void fillRecentTags(final Menu menu) {

      if ((TourDatabase.getAllTourTags().size() > 0)) {

         // tags are available

         (new Separator()).fill(menu, -1);
         {
            addActionToMenu(menu, _actionAdvanced_RecentTagsTitle);
            _tagMenuMgr.fillMenuWithRecentTags(null, menu);
         }
      }
   }

   /**
    * Fill the root context menu and check/disable tags for the selected tours
    *
    * @param menu
    */
   private void fillRootMenu(final Menu menu) {

      // dispose old menu items
      Arrays.stream(menu.getItems()).forEach(menuItem -> menuItem.dispose());

      // check if a tour is selected
      _selectedTours = _tagMenuMgr.getTourProvider().getSelectedTours();
      if (_selectedTours == null || _selectedTours.isEmpty()) {
         // a tour is not selected
         return;
      }

      // get all tags for all tours
      _selectedTourTags.clear();
      for (final TourData tourData : _selectedTours) {
         final Set<TourTag> tags = tourData.getTourTags();
         if (tags != null) {
            _selectedTourTags.addAll(tags);
         }
      }

      // add tags, create actions for the root tags

      final TagCollection rootTagCollection = TourDatabase.getRootTags();

      if (_isAdvancedMenu == false) {

         createTagCategoryActions(rootTagCollection, menu);
         createTagActions(rootTagCollection, menu);

      } else {

         /*
          * this action is managed by the advanced menu provider
          */

         // create title menu items

         addActionToMenu(menu, _actionAdvanced_AddTagTitle);

         (new Separator()).fill(menu, -1);
         {
            createTagCategoryActions(rootTagCollection, menu);
            createTagActions(rootTagCollection, menu);
         }

         fillRecentTags(menu);

         final boolean isModifiedTags = _modifiedTags.size() > 0;

         (new Separator()).fill(menu, -1);
         {
            // show newly added tags

            addActionToMenu(menu, new ActionAdvanced_ModifiedTags());

            if (isModifiedTags) {

               // create actions
               final ArrayList<TourTag> modifiedTags = new ArrayList<>(_modifiedTags.values());
               Collections.sort(modifiedTags);

               for (final TourTag tourTag : modifiedTags) {
                  addActionToMenu(menu, new ActionAdvanced_ModifiedTag(tourTag));
               }
            }
         }

         (new Separator()).fill(menu, -1);
         {
            addActionToMenu(menu, _actionAdvanced_OK);
            addActionToMenu(menu, new ActionAdvanced_Cancel());
         }

         (new Separator()).fill(menu, -1);
         {
            addActionToMenu(menu, _actionAdvanced_OpenTagPrefs);
         }

         /*
          * enable actions
          */
         _actionAdvanced_OK.setEnabled(isModifiedTags);
         _actionAdvanced_OK.setImageDescriptor(isModifiedTags
               ? TourbookPlugin.getImageDescriptor(Images.App_OK)
               : null);
      }
   }

   @Override
   public Menu getMenu(final Control parentControl) {

      // fix: https://github.com/mytourbook/mytourbook/issues/1154
      if (parentControl == null) {
         return null;
      }

      _isAdvancedMenu = true;

      disposeRootMenu();

      _rootMenu = new Menu(parentControl);

      // Add listener to repopulate the menu each time
      _rootMenu.addMenuListener(MenuListener.menuShownAdapter(menuEvent -> fillRootMenu((Menu) menuEvent.widget)));

      return _rootMenu;
   }

   @Override
   public Menu getMenu(final Menu parentMenu) {

      _isAdvancedMenu = false;

      disposeRootMenu();

      _rootMenu = new Menu(parentMenu);

      // Add listener to repopulate the menu each time
      _rootMenu.addMenuListener(MenuListener.menuShownAdapter(menuEvent -> {

         resetData();

         fillRootMenu((Menu) menuEvent.widget);
      }));

      return _rootMenu;
   }

   private void onArmEvent(final ArmEvent event) {

      final MenuItem menuItem = (MenuItem) event.widget;

      System.out.println(UI.timeStamp() + " armEvent: " + menuItem.getText());
// TODO remove SYSTEM.OUT.PRINTLN

   }

   @Override
   public void onShowMenu() {

      _tagMenuMgr.setIsAdvanceMenu();

      TagMenuManager.enableRecentTagActions(true, _modifiedTags.keySet());
   }

   @Override
   public void resetData() {

      _modifiedTags.clear();
   }

   @Override
   public void run() {

      _advancedMenuProvider.openAdvancedMenu();
   }

   private void saveOrReopenTagMenu(final boolean isAddTag) {

      if (_isAdvancedMenu == false) {

         saveTags();

      } else {

         if (isAddTag == false) {
            /*
             * it is possible that a tag was remove which is contained within the previous tags,
             * uncheck this action that is displays the correct checked tags
             */
            TagMenuManager.updatePreviousTagState(_modifiedTags);
         }

         // reopen action menu
         _advancedMenuProvider.openAdvancedMenu();
      }
   }

   private void saveTags() {

      System.out.println(UI.timeStamp() + " saveTags() " + _modifiedTags);
// TODO remove SYSTEM.OUT.PRINTLN

      if (_modifiedTags.size() > 0) {
         _tagMenuMgr.saveTourTags(_modifiedTags, true);
      }
   }

   @Override
   public void setAdvancedMenuProvider(final AdvancedMenuForActions advancedMenuProvider) {

      _advancedMenuProvider = advancedMenuProvider;
   }

   @Override
   public void setEnabled(final boolean enabled) {

      if (_isAdvancedMenu == false) {

         // ensure tags are available
         final HashMap<Long, TourTag> allTags = TourDatabase.getAllTourTags();

         super.setEnabled(enabled && allTags.size() > 0);

      } else {

         super.setEnabled(enabled);
      }
   }

   /**
    * Add/Remove all previous tags
    *
    * @param isAddTag
    * @param _allPreviousTags
    */
   void setTourTag(final boolean isAddTag, final HashMap<Long, TourTag> _allPreviousTags) {

      for (final TourTag tourTag : _allPreviousTags.values()) {

         if (isAddTag) {
            // add tag
            _modifiedTags.put(tourTag.getTagId(), tourTag);
         } else {
            // remove tag
            _modifiedTags.remove(tourTag.getTagId());
         }
      }

      saveOrReopenTagMenu(isAddTag);
   }

   /**
    * Add or remove a tour tag
    *
    * @param isAddTag
    * @param tourTag
    */
   void setTourTag(final boolean isAddTag, final TourTag tourTag) {

      if (isAddTag) {
         // add tag
         _modifiedTags.put(tourTag.getTagId(), tourTag);
      } else {
         // remove tag
         _modifiedTags.remove(tourTag.getTagId());
      }

      saveOrReopenTagMenu(isAddTag);
   }

   @Override
   public String toString() {

      return "ActionAddTourTag [getText()=" + getText() + ", hashCode()=" + hashCode() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
   }

}
