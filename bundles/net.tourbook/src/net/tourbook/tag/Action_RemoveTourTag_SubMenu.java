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

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Removes a tag from the selected tours
 */
class Action_RemoveTourTag_SubMenu extends Action implements IMenuCreator {

   private TagMenuManager      _tagMenuMgr;
   private Menu                _menu;

   private ArrayList<TourData> _selectedTours;

   /**
    * contains the tags for all selected tours in the viewer
    */
   private Set<TourTag>        _selectedTags;

   private class ActionTourTag extends Action {

      private final TourTag _tourTag;

      public ActionTourTag(final TourTag tourTag) {

         super(tourTag.getTagName(), AS_CHECK_BOX);

         final Image tagImage = TagManager.getTagImage(tourTag);

         if (tagImage != null) {
            setImageDescriptor(ImageDescriptor.createFromImage(tagImage));
         }

         _tourTag = tourTag;
      }

      @Override
      public void run() {
         _tagMenuMgr.saveTourTags(_tourTag, false);
      }
   }

   Action_RemoveTourTag_SubMenu(final TagMenuManager tagMenuManager) {

      super(Messages.action_tag_remove, AS_DROP_DOWN_MENU);

      _tagMenuMgr = tagMenuManager;

      setMenuCreator(this);
   }

   private void addActionToMenu(final Menu menu, final Action action) {

      final ActionContributionItem item = new ActionContributionItem(action);
      item.fill(menu, -1);
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

         boolean isTagChecked = false;
         final boolean isOneTour = _selectedTours != null && _selectedTours.size() == 1;
         if (_selectedTags != null && isOneTour) {

            /*
             * only when one tour is selected check the tag otherwise it's confusing, a
             * three-state check could solve this problem but is not available
             */

            final long tagId = menuTourTag.getTagId();

            for (final TourTag checkTourTag : _selectedTags) {
               if (checkTourTag.getTagId() == tagId) {
                  isTagChecked = true;
                  break;
               }
            }

         } else {
            isTagChecked = true;
         }

         actionTourTag.setChecked(isTagChecked);
         actionTourTag.setEnabled(isTagChecked);

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

   @Override
   public Menu getMenu(final Control parent) {

      dispose();

      _menu = new Menu(parent);

      // Add listener to repopulate the menu each time
      _menu.addMenuListener(menuShownAdapter(menuEvent -> onFillMenu((Menu) menuEvent.widget)));

      return _menu;
   }

   @Override
   public Menu getMenu(final Menu parent) {

      dispose();

      _menu = new Menu(parent);

      // Add listener to repopulate the menu each time
      _menu.addMenuListener(menuShownAdapter(menuEvent -> onFillMenu((Menu) menuEvent.widget)));

      return _menu;
   }

   /**
    * Fill the context menu and check/disable tags for the selected tours
    *
    * @param menu
    */
   private void onFillMenu(final Menu menu) {

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
      _selectedTags = new HashSet<>();
      for (final TourData tourData : _selectedTours) {
         final Set<TourTag> tags = tourData.getTourTags();
         if (tags != null) {
            _selectedTags.addAll(tags);
         }
      }

      // remove tags, create actions for all tags of all selected tours

      final ArrayList<TourTag> sortedTags = new ArrayList<>(_selectedTags);
      Collections.sort(sortedTags);

      createTagActions(new TagCollection(sortedTags), _menu);
   }

   @Override
   public void setEnabled(final boolean enabled) {

      // ensure tags are available
      final HashMap<Long, TourTag> allTags = TourDatabase.getAllTourTags();

      super.setEnabled(enabled && allTags.size() > 0);
   }

}
