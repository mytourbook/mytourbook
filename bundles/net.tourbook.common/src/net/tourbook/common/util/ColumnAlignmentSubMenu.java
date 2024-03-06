/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.common.util;

import net.tourbook.common.Messages;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class ColumnAlignmentSubMenu extends Action implements IMenuCreator {

   private Menu             _contextMenu;

   private ColumnDefinition _colDef;
   private ColumnManager    _columnManager;

   private ActionAlignment  _actionAlignmentLeft;
   private ActionAlignment  _actionAlignmentCenter;
   private ActionAlignment  _actionAlignmentRight;

   private class ActionAlignment extends Action {

      private int _style;

      public ActionAlignment(final String text, final int style) {

         super(text, AS_CHECK_BOX);

         _style = style;
      }

      @Override
      public void run() {

         _columnManager.action_SetColumnAlignment(_colDef, _style);
      }
   }

   private class ActionAlignments_Submenu extends Action implements IMenuCreator {

      private Menu __alignmentMenu;

      public ActionAlignments_Submenu() {

         super(Messages.Action_ColumnManager_SetColumnAlignment, AS_DROP_DOWN_MENU);

         setMenuCreator(this);
      }

      @Override
      public void dispose() {

         if (__alignmentMenu != null) {
            __alignmentMenu.dispose();
            __alignmentMenu = null;
         }
      }

      @Override
      public Menu getMenu(final Control parent) {
         return null;
      }

      @Override
      public Menu getMenu(final Menu parent) {

         dispose();

         __alignmentMenu = new Menu(parent);

         // Add listener to repopulate the menu each time
         __alignmentMenu.addMenuListener(new MenuAdapter() {
            @Override
            public void menuShown(final MenuEvent e) {

               final Menu menu = (Menu) e.widget;

               // dispose old items
               final MenuItem[] items = menu.getItems();
               for (final MenuItem item : items) {
                  item.dispose();
               }

               // add actions
               addActionToMenu(menu, _actionAlignmentLeft);
               addActionToMenu(menu, _actionAlignmentCenter);
               addActionToMenu(menu, _actionAlignmentRight);

               final int columnStyle = _colDef.getColumnStyle();

               // select actions
               _actionAlignmentLeft.setChecked(columnStyle == SWT.LEAD);
               _actionAlignmentCenter.setChecked(columnStyle == SWT.CENTER);
               _actionAlignmentRight.setChecked(columnStyle == SWT.TRAIL);

               // enable actions
               _actionAlignmentLeft.setEnabled(columnStyle != SWT.LEAD);
               _actionAlignmentCenter.setEnabled(columnStyle != SWT.CENTER);
               _actionAlignmentRight.setEnabled(columnStyle != SWT.TRAIL);
            }
         });

         return __alignmentMenu;
      }
   }

   public ColumnAlignmentSubMenu(final Menu contextMenu, final ColumnDefinition colDef, final ColumnManager columnManager) {

      _contextMenu = contextMenu;
      _columnManager = columnManager;

      _colDef = colDef;

      _actionAlignmentLeft = new ActionAlignment(Messages.App_Alignment_Left, SWT.LEAD);
      _actionAlignmentCenter = new ActionAlignment(Messages.App_Alignment_Center, SWT.CENTER);
      _actionAlignmentRight = new ActionAlignment(Messages.App_Alignment_Right, SWT.TRAIL);

      addActionToMenu(_contextMenu, new ActionAlignments_Submenu());
   }

   private void addActionToMenu(final Menu menu, final Action action) {

      final ActionContributionItem item = new ActionContributionItem(action);
      item.fill(menu, -1);
   }

   @Override
   public void dispose() {

      if (_contextMenu != null) {
         _contextMenu.dispose();
         _contextMenu = null;
      }
   }

   @Override
   public Menu getMenu(final Control parent) {
      return null;
   }

   @Override
   public Menu getMenu(final Menu parent) {
      return null;
   }

}
