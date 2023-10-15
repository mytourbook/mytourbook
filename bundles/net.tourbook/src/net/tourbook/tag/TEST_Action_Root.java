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

import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

class TEST_Action_Root extends Action implements IMenuCreator {

   private Menu _rootMenu;

   /**
    */
   TEST_Action_Root() {

      super("Add Tag TEST", AS_DROP_DOWN_MENU);

      setMenuCreator(this);
   }

   @Override
   public void dispose() {

      if (_rootMenu != null) {

         _rootMenu.dispose();
         _rootMenu = null;
      }
   }

   private void fillMenu(final Menu menu) {

      final TagCollection rootItems = TourDatabase.getRootTags();

      for (final TourTagCategory tagCategory : rootItems.tourTagCategories) {

         final TEST_Action_Category action = new TEST_Action_Category(tagCategory);
         final ActionContributionItem actionItem = new ActionContributionItem(action);

         actionItem.fill(menu, -1);
      }
   }

   @Override
   public Menu getMenu(final Control parentControl) {
      return null;
   }

   @Override
   public Menu getMenu(final Menu parentMenu) {

      dispose();

      _rootMenu = new Menu(parentMenu);

      // add listener to repopulate the menu each time
      _rootMenu.addMenuListener(MenuListener.menuShownAdapter(menuEvent -> {

         // dispose old menu items
         Arrays.stream(_rootMenu.getItems()).forEach(menuItem -> menuItem.dispose());

         fillMenu((Menu) menuEvent.widget);
      }));

      return _rootMenu;
   }
}
