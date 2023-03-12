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

import static org.eclipse.swt.events.MenuListener.menuShownAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
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
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Add tag(s) from the selected tours
 */
class Action_AddTourTag_SubMenu extends Action implements IMenuCreator, IAdvancedMenuForActions {

   private static final String    SPACE_PRE_TAG     = "   ";          //$NON-NLS-1$

   private TagMenuManager         _tagMenuMgr;
   private Menu                   _menu;

   /**
    * Contains all tags for all selected tours in the viewer
    */
   private Set<TourTag>           _selectedTourTags = new HashSet<>();
   private ArrayList<TourData>    _selectedTours;

   /**
    * Contains all tags which will be added
    */
   private HashMap<Long, TourTag> _modifiedTags     = new HashMap<>();

   private AdvancedMenuForActions _advancedMenuProvider;

   private Action                 _actionAddTagTitle;
   private ActionOK               _actionOK;
   private ActionOpenPrefDialog   _actionOpenTagPrefs;
   private Action                 _actionRecentTagsTitle;

   private boolean                _isAdvancedMenu;

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

   private class ActionModifiedTag extends Action {

      private final TourTag __tourTag;

      public ActionModifiedTag(final TourTag tourTag) {

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

   private class ActionModifiedTags extends Action {

      public ActionModifiedTags() {

         super(Messages.Action_Tag_Add_AutoOpen_ModifiedTags);

         // this action is only for info
         super.setEnabled(false);
      }
   }

   private final class ActionOK extends Action {

      private ActionOK() {

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
      }

      @Override
      public void run() {

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

         setMenuCreator(this);
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
         __categoryMenu.addMenuListener(menuShownAdapter(menuEvent -> {

            final Menu menu = (Menu) menuEvent.widget;

            // dispose old items
            final MenuItem[] items = menu.getItems();
            for (final MenuItem item : items) {
               item.dispose();
            }

            final TagCollection tagCollection = TourDatabase.getTagEntries(__tagCategory.getCategoryId());

            // add actions
            __actionAddTourTag.createCategoryActions(tagCollection, __categoryMenu);
            __actionAddTourTag.createTagActions(tagCollection, __categoryMenu);
         }));

         return __categoryMenu;
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

   private void createCategoryActions(final TagCollection tagCollection, final Menu menu) {

      // add tag categories
      for (final TourTagCategory tagCategory : tagCollection.tourTagCategories) {
         addActionToMenu(menu, new ActionTourTagCategory(this, tagCategory));
      }
   }

   private void createDefaultAction(final TagMenuManager tagMenuMgr) {

      _tagMenuMgr = tagMenuMgr;

      _actionAddTagTitle = new Action(Messages.Action_Tag_Add_AutoOpen_Title) {};
      _actionAddTagTitle.setEnabled(false);

      _actionRecentTagsTitle = new Action(Messages.Action_Tag_Add_RecentTags) {};
      _actionRecentTagsTitle.setEnabled(false);

      _actionOK = new ActionOK();

      _actionOpenTagPrefs = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure, PrefPageTags.ID);
   }

   private void createTagActions(final TagCollection tagCollection, final Menu menu) {

      final ArrayList<TourTag> allTourTags = tagCollection.tourTags;
      if (allTourTags == null) {
         return;
      }

      //Preload the tag images
      //Note that the hourglass is only displayed on Windows (it doesn't seem
      //to work on Linux)
      BusyIndicator.showWhile(Display.getCurrent(), () -> allTourTags.forEach(tourTag -> TagManager.getTagImage(tourTag)));

      // add tag items
      for (final TourTag menuTourTag : allTourTags) {

         // check the tag when it's set in the tour
         final ActionTourTag actionTourTag = new ActionTourTag(menuTourTag);

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

   @Override
   public void dispose() {

      if (_menu != null) {
         _menu.dispose();
         _menu = null;
      }
   }

   /**
    * Fill the context menu and check/disable tags for the selected tours
    *
    * @param menu
    */
   private void fillMenu(final Menu menu) {

      // dispose old items
      final MenuItem[] items = menu.getItems();
      for (final MenuItem item : items) {
         item.dispose();
      }

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

         createCategoryActions(rootTagCollection, menu);
         createTagActions(rootTagCollection, menu);

      } else {

         /*
          * this action is managed by the advanced menu provider
          */

         // create title menu items

         addActionToMenu(menu, _actionAddTagTitle);

         (new Separator()).fill(menu, -1);
         {
            createCategoryActions(rootTagCollection, menu);
            createTagActions(rootTagCollection, menu);
         }

         fillRecentTags(menu);

         final boolean isModifiedTags = _modifiedTags.size() > 0;

         (new Separator()).fill(menu, -1);
         {
            // show newly added tags

            addActionToMenu(menu, new ActionModifiedTags());

            if (isModifiedTags) {

               // create actions
               final ArrayList<TourTag> modifiedTags = new ArrayList<>(_modifiedTags.values());
               Collections.sort(modifiedTags);

               for (final TourTag tourTag : modifiedTags) {
                  addActionToMenu(menu, new ActionModifiedTag(tourTag));
               }
            }
         }

         (new Separator()).fill(menu, -1);
         {
            addActionToMenu(menu, _actionOK);
            addActionToMenu(menu, new ActionCancel());
         }

         (new Separator()).fill(menu, -1);
         {
            addActionToMenu(menu, _actionOpenTagPrefs);
         }

         /*
          * enable actions
          */
         _actionOK.setEnabled(isModifiedTags);
         _actionOK.setImageDescriptor(isModifiedTags
               ? TourbookPlugin.getImageDescriptor(Images.App_OK)
               : null);
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
            addActionToMenu(menu, _actionRecentTagsTitle);
            _tagMenuMgr.fillMenuWithRecentTags(null, menu);
         }
      }
   }

   @Override
   public Menu getMenu(final Control parent) {

      _isAdvancedMenu = true;

      dispose();

      _menu = new Menu(parent);

      // Add listener to repopulate the menu each time
      _menu.addMenuListener(menuShownAdapter(menuEvent -> fillMenu((Menu) menuEvent.widget)));

      return _menu;
   }

   @Override
   public Menu getMenu(final Menu parent) {

      _isAdvancedMenu = false;

      dispose();

      _menu = new Menu(parent);

      // Add listener to repopulate the menu each time
      _menu.addMenuListener(menuShownAdapter(menuEvent -> {

         resetData();

         fillMenu((Menu) menuEvent.widget);
      }));

      return _menu;
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
