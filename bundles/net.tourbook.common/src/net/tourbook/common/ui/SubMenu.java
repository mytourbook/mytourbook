/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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
package net.tourbook.common.ui;

import static org.eclipse.swt.events.MenuListener.menuShownAdapter;

import java.util.Arrays;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public abstract class SubMenu extends Action implements IMenuCreator {

   private Menu _menu;

   protected SubMenu(final String actionText, final int style) {

      super(actionText, style);

      setMenuCreator(this);
   }

   @Override
   public void dispose() {

      if (_menu == null) {
         return;
      }

      _menu.dispose();
      _menu = null;
   }

   public abstract void enableActions();

   public abstract void fillMenu(final Menu menu);

   @Override
   public Menu getMenu(final Control parent) {
      return null;
   }

   @Override
   public Menu getMenu(final Menu parent) {

      dispose();
      _menu = new Menu(parent);

      // Add listener to repopulate the menu each time
      _menu.addMenuListener(menuShownAdapter(menuEvent -> {

         // dispose old menu items
         Arrays.stream(((Menu) menuEvent.widget).getItems())
               .forEach(menuItem -> menuItem.dispose());

         fillMenu(_menu);

         enableActions();
      }));

      return _menu;
   }
}
