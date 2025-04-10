/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourData;
import net.tourbook.ui.ITourProvider2;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.widgets.Menu;

/**
 */
public class SubMenu_InterpolatedValues extends SubMenu {

   private ITourProvider2                    _tourProvider;

   private ActionInterpolatedValues_ReImport _action_InterpolatedValues_ReImport;
   private ActionInterpolatedValues_Remove   _action_InterpolatedValues_Remove;

   public SubMenu_InterpolatedValues(final ITourProvider2 tourProvider) {

      super(Messages.Tour_SubMenu_InterpolatedValues, AS_DROP_DOWN_MENU);

      _tourProvider = tourProvider;

      _action_InterpolatedValues_ReImport = new ActionInterpolatedValues_ReImport(tourProvider);
      _action_InterpolatedValues_Remove = new ActionInterpolatedValues_Remove(tourProvider);
   }

   @Override
   public void enableActions() {

      final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();
      final boolean isOneTour = selectedTours.size() == 1;

      _action_InterpolatedValues_ReImport.setEnabled(isOneTour);
      _action_InterpolatedValues_Remove.setEnabled(isOneTour);
   }

   @Override
   public void fillMenu(final Menu menu) {

      new ActionContributionItem(_action_InterpolatedValues_ReImport).fill(menu, -1);
      new ActionContributionItem(_action_InterpolatedValues_Remove).fill(menu, -1);
   }
}
