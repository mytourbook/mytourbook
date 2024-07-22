/*******************************************************************************
 * Copyright (C) 2024 Frédéric Bard
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

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourData;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.ITourProviderByID;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.widgets.Menu;

public class SubMenu_Pauses extends SubMenu {

   private SubMenu_SetPausesType   _subMenu_SetPausesType;

   private ITourProvider2          _tourProvider;
   private ITourProviderByID       _tourProviderById;

   private ActionSetTourPauseTimes _action_SetTourPauseTimes;

   public SubMenu_Pauses(final ITourProvider2 tourProvider, final ITourProviderByID tourProviderById) {

      super(Messages.Tour_Action_Pauses, AS_DROP_DOWN_MENU);
      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourPauses));

      _tourProvider = tourProvider;
      _tourProviderById = tourProviderById;

      _subMenu_SetPausesType = new SubMenu_SetPausesType(_tourProvider, true);
      _action_SetTourPauseTimes = new ActionSetTourPauseTimes(_tourProviderById);
   }

   @Override
   public void enableActions() {

      boolean isTourWithPauseTime = false;

      final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();
      for (final TourData selectedTour : selectedTours) {

         final long[] pausedTime_Start = selectedTour.getPausedTime_Start();
         if (pausedTime_Start != null) {
            isTourWithPauseTime = true;
            break;
         }
      }

      _subMenu_SetPausesType.setEnabled(isTourWithPauseTime);
   }

   public boolean enableSubMenu() {

      return true;
   }

   @Override
   public void fillMenu(final Menu menu) {

      new ActionContributionItem(_action_SetTourPauseTimes).fill(menu, -1);
      new ActionContributionItem(_subMenu_SetPausesType).fill(menu, -1);
   }
}
