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
package net.tourbook.ui.tourChart.action;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.action.Action;

/**
 * This delete action can be used to remove movements within a pause.
 * <p>
 * The time is kept but distances are removed and the tour start time is not adjusted
 */
public class ActionDeleteTimeSlices_KeepTime extends Action {

   private final TourChart _tourChart;

   public ActionDeleteTimeSlices_KeepTime(final TourChart tourChart) {

      super(Messages.Tour_Editor_Action_DeleteTimeSlices_KeepTime, AS_PUSH_BUTTON);

      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));

      _tourChart = tourChart;
   }

   @Override
   public void run() {

      _tourChart.actionDelete_TimeSlices(

            false, // isRemoveTime

            true, // isRemoveDistance

            false // isAdjustTourStartTime
      );
   }
}
