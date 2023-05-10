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
package net.tourbook.ui.views.tourBook;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.ui.SubMenu;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.widgets.Menu;

/**
 * The action to delete a tour is displayed in a sub menu that it is not accidentally be run
 */
public class ActionDeleteTourMenu extends SubMenu {

   private ActionDeleteTourMenu2 _actionDeleteTourMenu2;

   public ActionDeleteTourMenu(final TourBookView tourBookView) {

      super(Messages.Tour_Book_Action_delete_selected_tours_menu, AS_DROP_DOWN_MENU);
      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.State_Delete));

      _actionDeleteTourMenu2 = new ActionDeleteTourMenu2(tourBookView);
   }

   @Override
   public void enableActions() {}

   @Override
   public void fillMenu(final Menu menu) {

      new ActionContributionItem(_actionDeleteTourMenu2).fill(menu, -1);
   }
}
