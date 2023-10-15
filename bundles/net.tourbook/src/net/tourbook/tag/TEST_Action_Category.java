/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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

import java.util.Arrays;

import net.tourbook.common.UI;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

class TEST_Action_Category extends Action implements IMenuCreator {

   private TourTagCategory _tagCategory;

   private Menu            _categoryMenu;

   public TEST_Action_Category(final TourTagCategory tagCategory) {

      super(tagCategory.getCategoryName(), AS_DROP_DOWN_MENU);

      _tagCategory = tagCategory;

      System.out.println(UI.timeStamp() + " create Category: " + tagCategory.getCategoryName());

      setMenuCreator(this);
   }

   @Override
   public void dispose() {

      if (_categoryMenu != null) {

         _categoryMenu.dispose();
         _categoryMenu = null;
      }
   }

   private void fillMenu(final Menu menu) {

      final TagCollection tagCollection = TourDatabase.getTagEntries(_tagCategory.getCategoryId());

      for (final TourTag tourTag : tagCollection.tourTags) {

         final TEST_Action_Tag actionTourTag = new TEST_Action_Tag(tourTag);
         final ActionContributionItem actionItem = new ActionContributionItem(actionTourTag);

         actionItem.fill(menu, -1);
      }
   }

   @Override
   public Menu getMenu(final Control parent) {
      return null;
   }

   @Override
   public Menu getMenu(final Menu parent) {

      dispose();

      _categoryMenu = new Menu(parent);

      // add listener to repopulate the menu each time
      _categoryMenu.addMenuListener(MenuListener.menuShownAdapter(menuEvent -> {

         final Menu menu = (Menu) menuEvent.widget;

         System.out.println(UI.NEW_LINE + UI.timeStamp() + " menuShown:  " + menu);

         // dispose old menu items
         Arrays.stream(menu.getItems()).forEach(menuItem -> menuItem.dispose());

         fillMenu(_categoryMenu);
      }));

      return _categoryMenu;
   }
}
