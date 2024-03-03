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
import net.tourbook.common.ui.SubMenu;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;

public class ColumnAlignmentSubMenu extends SubMenu {

   private ColumnManager    _columnManager;
   private ColumnDefinition _colDef;

   private ActionAlignment  _actionAlignLeft;
   private ActionAlignment  _actionAlignCenter;
   private ActionAlignment  _actionAlignRight;

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

   public ColumnAlignmentSubMenu(final ColumnDefinition colDef, final ColumnManager columnManager) {

      super(Messages.Action_ColumnManager_SetColumnAlignment, AS_DROP_DOWN_MENU);

      _colDef = colDef;
      _columnManager = columnManager;

      _actionAlignLeft = new ActionAlignment(Messages.App_Alignment_Left, SWT.LEAD);
      _actionAlignCenter = new ActionAlignment(Messages.App_Alignment_Center, SWT.CENTER);
      _actionAlignRight = new ActionAlignment(Messages.App_Alignment_Right, SWT.TRAIL);
   }

   @Override
   public void enableActions() {

   }

   @Override
   public void fillMenu(final Menu menu) {

      addActionToMenu(_actionAlignLeft);
      addActionToMenu(_actionAlignCenter);
      addActionToMenu(_actionAlignRight);
   }

}
