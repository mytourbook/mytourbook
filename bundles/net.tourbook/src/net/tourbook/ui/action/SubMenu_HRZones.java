/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.action;

import net.tourbook.common.ui.SubMenu;
import net.tourbook.ui.ITourProviderByID;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.widgets.Menu;

public class SubMenu_HRZones extends SubMenu {

   private ActionSetHRZones_AllTours      _action_SetHRZones_AllTours;
   private ActionSetHRZones_SelectedTours _action_SetHRZones_SelectedTours;

   public SubMenu_HRZones(final ITourProviderByID tourProviderById) {

      super("HR Zones", AS_DROP_DOWN_MENU);

      _action_SetHRZones_AllTours = new ActionSetHRZones_AllTours();
      _action_SetHRZones_SelectedTours = new ActionSetHRZones_SelectedTours(tourProviderById);
   }

   @Override
   public void enableActions() {}

   @Override
   public void fillMenu(final Menu menu) {

      new ActionContributionItem(_action_SetHRZones_AllTours).fill(menu, -1);
      new ActionContributionItem(_action_SetHRZones_SelectedTours).fill(menu, -1);
   }
}
