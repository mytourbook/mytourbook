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

import java.util.HashSet;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.database.ITourDataUpdate_OnlyUpdate;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;

public class ActionSetHRZones_AllTours extends Action implements ITourDataUpdate_OnlyUpdate {

   public ActionSetHRZones_AllTours() {

      super(null, AS_PUSH_BUTTON);

      setText(Messages.Tour_Action_HRZones_InAllTours);
      setToolTipText(Messages.Tour_Action_HRZones_InAllTours_Tooltip);
   }

   @Override
   public void run() {

      // check if the tour editor contains a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final List<Long> allTourIDs = TourDatabase.getAllTourIds();

      TourManager.updateTourData_Concurrent(new HashSet<>(allTourIDs), this);
   }

   @Override
   public boolean updateTourData(final TourData tourData) {

      // set HR zones
      final int[] allHrZones = tourData.getHrZones();

      if (allHrZones == null) {
         return false;
      }

      // save tour
      return true;
   }
}
