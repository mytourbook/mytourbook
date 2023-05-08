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
public class SubMenu_Cadence extends SubMenu {

   private SubMenu_SetCadence             _subMenu_SetCadence;

   private ITourProvider2                 _tourProvider;

   private ActionComputeCadenceZonesTimes _action_ComputeCadenceZonesTimes;

   public SubMenu_Cadence(final ITourProvider2 tourProvider) {

      super(Messages.Tour_SubMenu_Cadence, AS_DROP_DOWN_MENU);

      _tourProvider = tourProvider;

      _subMenu_SetCadence = new SubMenu_SetCadence(tourProvider);

      _action_ComputeCadenceZonesTimes = new ActionComputeCadenceZonesTimes(tourProvider);
   }

   @Override
   public void enableActions() {}

   public boolean enableSubMenu() {

      final boolean enableActions = false;

      final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();
      for (final TourData selectedTour : selectedTours) {

         final float[] cadenceSerie = selectedTour.getCadenceSerie();
         if (cadenceSerie != null) {
            return true;
         }
      }

      return enableActions;
   }

   @Override
   public void fillMenu(final Menu menu) {

      new ActionContributionItem(_action_ComputeCadenceZonesTimes).fill(menu, -1);
      new ActionContributionItem(_subMenu_SetCadence).fill(menu, -1);
   }
}
