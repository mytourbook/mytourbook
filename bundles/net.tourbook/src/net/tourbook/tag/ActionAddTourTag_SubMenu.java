/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Add tag(s) from the selected tours
 */
public class ActionAddTourTag_SubMenu extends Action implements IMenuCreator, IAdvancedMenuForActions {

   private static final String    SPACE_PRE_TAG    = "   ";          //$NON-NLS-1$

   private TagMenuManager         _tagMenuManager;
   private Menu                   _menu;

   private ArrayList<TourData>    _allSelectedTours;

   /**
    * Contains all tags for all selected tours in the viewer
    */
   private Set<TourTag>           _allUsedTags     = new HashSet<>();

   /**
    * Contains all tags which will be added
    */
   private HashMap<Long, TourTag> _allModifiedTags = new HashMap<>();

   private AdvancedMenuForActions _advancedMenuProvider;

   private ActionOK               _actionOK;
   private ActionOpenPrefDialog   _actionOpenTagPrefs;

   private Action                 _actionTitle_AddTag;
   private Action                 _actionTitle_ModifiedTags;
   private Action                 _actionTitle_RecentTags;

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

   private class ActionModifyTag extends Action {

      private final TourTag __tourTag;

      public ActionModifyTag(final TourTag tourTag) {

         super(SPACE_PRE_TAG + tourTag.getTagName(), AS_CHECK_BOX);

         __tourTag = tourTag;

         // this tag is always checked, unchecking it will also remove it
         setChecked(true);
      }

      @Override
      public void run() {

         // uncheck/remove this tag
         _allModifiedTags.remove(__tourTag.getTagId());

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
         saveTags();
      }
   }

   private class ActionTourTag extends Action {

      private final TourTag __tourTag;

      public ActionTourTag(final TourTag tourTag) {

         super(getTagName(tourTag), AS_CHECK_BOX);

         final Image tagImage = TagManager.getTagImage(tourTag);

         if (tagImage != null) {
            setImageDescriptor(ImageDescriptor.createFromImage(tagImage));
         }

         __tourTag = tourTag;
      }

      private static String getTagName(final TourTag tourTag) {

         String tagName = tourTag.getTagName();

         if (UI.IS_SCRAMBLE_DATA) {
            tagName = UI.scrambleText(tagName);
         }

         return tagName;
      }

      @Override
      public void run() {

         if (_isAdvancedMenu) {

            setTourTag(isChecked(), __tourTag);

         } else {

            // add tag
            _allModifiedTags.put(__tourTag.getTagId(), __tourTag);

            saveTags();
         }
      }
   }

   /**
    *
    */
   private class ActionTourTagCategory extends Action implements IMenuCreator {

      private Menu                           __categoryMenu;

      private final ActionAddTourTag_SubMenu __actionAddTourTag;
      private final TourTagCategory          __tagCategory;

      public ActionTourTagCategory(final ActionAddTourTag_SubMenu actionAddTourTag, final TourTagCategory tagCategory) {

         super(getCategoryName(tagCategory), AS_DROP_DOWN_MENU);

         __actionAddTourTag = actionAddTourTag;
         __tagCategory = tagCategory;

         setMenuCreator(this);
      }

      private static String getCategoryName(final TourTagCategory tagCategory) {

         String categoryName = tagCategory.getCategoryName();

         if (UI.IS_SCRAMBLE_DATA) {
            categoryName = UI.scrambleText(categoryName);
         }

         return categoryName;
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
         __categoryMenu.addMenuListener(MenuListener.menuShownAdapter(menuEvent -> {

            final Menu menu = (Menu) menuEvent.widget;

            // dispose old items
            final MenuItem[] items = menu.getItems();
            for (final MenuItem item : items) {
               item.dispose();
            }

            final TagCollection tagCollection = TourDatabase.getTagEntries(__tagCategory.getCategoryId());

            // add actions
            __actionAddTourTag.fillMenu_10_TagCategoryActions(tagCollection, __categoryMenu);
            __actionAddTourTag.fillMenu_20_TagActions(tagCollection, __categoryMenu);
         }));

         return __categoryMenu;
      }
   }

   /**
    * @param tagMenuManager
    */
   ActionAddTourTag_SubMenu(final TagMenuManager tagMenuManager) {

      super(Messages.action_tag_add, AS_DROP_DOWN_MENU);

      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.Tag));

      createDefaultAction(tagMenuManager);

      setMenuCreator(this);
   }

   /**
    * This constructor creates a push button action without a drop down menu
    *
    * @param tagMenuMgr
    * @param isAutoOpen
    *           This parameter is ignored but it indicates that the menu auto open behavior is
    *           used and a menu creator is not set
    */
   ActionAddTourTag_SubMenu(final TagMenuManager tagMenuMgr, final Object isAutoOpen) {

      super(Messages.Action_Tag_Add_AutoOpen, AS_PUSH_BUTTON);

      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.Tag));

      createDefaultAction(tagMenuMgr);
   }

   private void addActionToMenu(final Menu menu, final Action action) {

      final ActionContributionItem item = new ActionContributionItem(action);
      item.fill(menu, -1);
   }

   private void addSeparatorToMenu(final Menu menu) {

      (new Separator()).fill(menu, -1);
   }

   private void createDefaultAction(final TagMenuManager tagMenuManager) {

      _tagMenuManager = tagMenuManager;

      _actionTitle_AddTag = new Action(Messages.Action_Tag_Add_AutoOpen_Title) {};
      _actionTitle_AddTag.setEnabled(false);

      _actionTitle_ModifiedTags = new Action(Messages.Action_Tag_Add_AutoOpen_ModifiedTags) {};
      _actionTitle_ModifiedTags.setEnabled(false);

      _actionTitle_RecentTags = new Action(Messages.Action_Tag_Add_RecentTags) {};
      _actionTitle_RecentTags.setEnabled(false);

      _actionOK = new ActionOK();

      _actionOpenTagPrefs = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure, PrefPageTags.ID);
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
      final MenuItem[] allMenuItems = menu.getItems();
      for (final MenuItem item : allMenuItems) {
         item.dispose();
      }

      // check if a tour is selected
      _allSelectedTours = _tagMenuManager.getTourProvider().getSelectedTours();
      if (_allSelectedTours == null || _allSelectedTours.isEmpty()) {
         // a tour is not selected
         return;
      }

      // get all tags which are used in all tours
      _allUsedTags.clear();
      for (final TourData tourData : _allSelectedTours) {
         final Set<TourTag> tags = tourData.getTourTags();
         if (tags != null) {
            _allUsedTags.addAll(tags);
         }
      }

      fillMenu_00_AllActions(menu);
   }

   /**
    * Fill all actions
    *
    * @param menu
    */
   private void fillMenu_00_AllActions(final Menu menu) {

      final TagCollection rootTagCollection = TourDatabase.getRootTags();

      if (_isAdvancedMenu) {

         /*
          * This action is managed by the advanced menu provider
          */

         // create title menu items

         addActionToMenu(menu, _actionTitle_AddTag);

         addSeparatorToMenu(menu);
         {
            fillMenu_10_TagCategoryActions(rootTagCollection, menu);
            fillMenu_20_TagActions(rootTagCollection, menu);
         }

         fillMenu_30_RecentTags(menu);

         final boolean isModifiedTags = _allModifiedTags.size() > 0;

         addSeparatorToMenu(menu);
         {
            // show newly added tags

            addActionToMenu(menu, _actionTitle_ModifiedTags);

            if (isModifiedTags) {

               // create actions
               final ArrayList<TourTag> allModifiedTags = new ArrayList<>(_allModifiedTags.values());
               Collections.sort(allModifiedTags);

               for (final TourTag tourTag : allModifiedTags) {
                  addActionToMenu(menu, new ActionModifyTag(tourTag));
               }
            }
         }

         addSeparatorToMenu(menu);
         {
            addActionToMenu(menu, _actionOK);
            addActionToMenu(menu, new ActionCancel());
         }

         addSeparatorToMenu(menu);
         {
            addActionToMenu(menu, _actionOpenTagPrefs);
         }

         /*
          * Enable actions
          */
         _actionOK.setEnabled(isModifiedTags);
         _actionOK.setImageDescriptor(isModifiedTags
               ? TourbookPlugin.getImageDescriptor(Images.State_OK)
               : null);
      } else {

         fillMenu_10_TagCategoryActions(rootTagCollection, menu);
         fillMenu_20_TagActions(rootTagCollection, menu);
      }
   }

   private void fillMenu_10_TagCategoryActions(final TagCollection tagCollection, final Menu menu) {

      // add tag categories
      for (final TourTagCategory tagCategory : tagCollection.tourTagCategories) {
         addActionToMenu(menu, new ActionTourTagCategory(this, tagCategory));
      }
   }

   private void fillMenu_20_TagActions(final TagCollection tagCollection, final Menu menu) {

      final ArrayList<TourTag> allTourTags = tagCollection.tourTags;
      if (allTourTags == null) {
         return;
      }

      // Preload the tag images
      // Note that the hourglass is only displayed on Windows (it doesn't seem
      // to work on Linux)
      BusyIndicator.showWhile(Display.getCurrent(), () -> allTourTags.forEach(tourTag -> TagManager.getTagImage(tourTag)));

      // add tag items
      for (final TourTag menuTourTag : allTourTags) {

         // check the tag when it's set in the tour
         final ActionTourTag actionTourTag = new ActionTourTag(menuTourTag);

         final boolean isModifiedTags = _allModifiedTags.size() > 0;
         final boolean isSelectedTags = _allUsedTags != null;

         boolean isTagChecked = false;
         final boolean isOneTour = _allSelectedTours != null && _allSelectedTours.size() == 1;
         boolean isModifiedTag = false;

         if (isSelectedTags && isOneTour) {

            /*
             * only when one tour is selected check the tag otherwise it's confusing, a
             * three-state check could solve this problem but is not available
             */

            final long tagId = menuTourTag.getTagId();

            if (isSelectedTags) {

               for (final TourTag selectedTourTag : _allUsedTags) {
                  if (selectedTourTag.getTagId() == tagId) {
                     isTagChecked = true;
                     break;
                  }
               }
            }

         }

         if (isModifiedTags) {

            if (isTagChecked == false && _allModifiedTags.containsValue(menuTourTag)) {
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

   /**
    * @param menu
    */
   private void fillMenu_30_RecentTags(final Menu menu) {

      if ((TourDatabase.getAllTourTags().size() > 0)) {

         // tags are available

         addSeparatorToMenu(menu);
         {
            addActionToMenu(menu, _actionTitle_RecentTags);
            _tagMenuManager.fillTagMenu_WithRecentTags(null, menu);
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
   public void onShowAdvancedMenu() {

      _tagMenuManager.setIsAdvanceMenu();

      TagMenuManager.enableActions_Recent(true, _allModifiedTags.keySet());
   }

   @Override
   public void resetData() {

      _allModifiedTags.clear();
   }

   @Override
   public void run() {

      _advancedMenuProvider.openAdvancedMenu();
   }

   private void saveOrReopenTagMenu(final boolean isAddTag) {

      if (_isAdvancedMenu) {

         if (isAddTag == false) {

            /*
             * It is possible that a tag was removed which is contained within the previous tags,
             * uncheck this action that it displays the correct checked tags
             */
            TagMenuManager.updatePreviousTagState(_allModifiedTags);
         }

         // reopen action menu
         _advancedMenuProvider.openAdvancedMenu();

      } else {

         saveTags();
      }
   }

   private void saveTags() {

      if (_allModifiedTags.size() > 0) {

         _tagMenuManager.saveTourTags(_allModifiedTags, true);
      }
   }

   @Override
   public void setAdvancedMenuProvider(final AdvancedMenuForActions advancedMenuProvider) {

      _advancedMenuProvider = advancedMenuProvider;
   }

   @Override
   public void setEnabled(final boolean enabled) {

      if (_isAdvancedMenu) {

         super.setEnabled(enabled);

      } else {

         // ensure tags are available
         final HashMap<Long, TourTag> allTags = TourDatabase.getAllTourTags();

         super.setEnabled(enabled && allTags.size() > 0);
      }
   }

   /**
    * Add/Remove all previous tags
    *
    * @param isAddTag
    * @param allPreviousTags
    */
   void setTourTag(final boolean isAddTag, final HashMap<Long, TourTag> allPreviousTags) {

      for (final TourTag tourTag : allPreviousTags.values()) {

         if (isAddTag) {

            // add tag

            _allModifiedTags.put(tourTag.getTagId(), tourTag);

         } else {

            // remove tag

            _allModifiedTags.remove(tourTag.getTagId());
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

         _allModifiedTags.put(tourTag.getTagId(), tourTag);

      } else {

         // remove tag

         _allModifiedTags.remove(tourTag.getTagId());
      }

      saveOrReopenTagMenu(isAddTag);
   }

   @Override
   public String toString() {
      return "ActionAddTourTag [getText()=" + getText() + ", hashCode()=" + hashCode() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
   }

}
