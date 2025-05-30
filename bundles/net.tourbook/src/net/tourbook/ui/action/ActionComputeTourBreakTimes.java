/*******************************************************************************
 * Copyright (C) 2024, 2025 Wolfgang Schramm and Contributors
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

import java.util.List;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.database.ITourDataUpdate;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProviderByID;

import org.eclipse.jface.action.Action;

public class ActionComputeTourBreakTimes extends Action implements ITourDataUpdate {

   private final ITourProviderByID _tourProvider;

   public ActionComputeTourBreakTimes(final ITourProviderByID tourProvider) {

      super(null, AS_PUSH_BUTTON);

      _tourProvider = tourProvider;

      setText(Messages.Tour_Action_ComputeTourBreakTimes);
      setToolTipText(Messages.Tour_Action_ComputeTourBreakTimes_Tooltip);
   }

   @Override
   public int getDatabaseVersion() {

      // this is not needed

      return 0;
   }

   @Override
   public List<Long> getTourIDs() {
      return null;
   }

   @Override
   public void run() {

      // check if the tour editor contains a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final Set<Long> selectedTours = _tourProvider.getSelectedTourIDs();

      TourManager.updateTourData_Concurrent(selectedTours, this);
   }

   @Override
   public boolean updateTourData(final TourData tourData) {

      // force the break time to be recomputed with the current values which are already store in the pref store
      tourData.setBreakTimeSerie(null);

      // recompute break time
      tourData.computeTourMovingTime();

      return true;
   }
}
