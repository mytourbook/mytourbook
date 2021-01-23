/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
package net.tourbook.map2.action;

import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class Action_ExportMap_SubMenu extends Action implements IMenuCreator {

   private Menu                         _menu;

   private ActionExportMapViewImage     _actionExportMapViewImage;
   private ActionExportMapViewClipboard _actionExportMapViewClipboard;

   public Action_ExportMap_SubMenu(final Map2View map2View) {

      super(Messages.Map_Action_Export_Map_View, AS_DROP_DOWN_MENU);

      _actionExportMapViewImage = new ActionExportMapViewImage(map2View);
      _actionExportMapViewClipboard = new ActionExportMapViewClipboard(map2View);

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

   @Override
   public Menu getMenu(final Control parent) {
      return null;
   }

   @Override
   public Menu getMenu(final Menu parent) {

      dispose();

      _menu = new Menu(parent);

      // Add listener to re-populate the menu each time
      _menu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuShown(final MenuEvent e) {

            // dispose old menu items
            for (final MenuItem menuItem : ((Menu) e.widget).getItems()) {
               menuItem.dispose();
            }

            // add actions
            new ActionContributionItem(_actionExportMapViewImage).fill(_menu, -1);
            new ActionContributionItem(_actionExportMapViewClipboard).fill(_menu, -1);
         }
      });

      return _menu;
   }

}
