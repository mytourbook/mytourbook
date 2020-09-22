/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.action.Action;


public class ActionTourChartPauses extends Action {

   private TourChart _tourChart;

   public ActionTourChartPauses(final TourChart tourChart) {

      super(Messages.Tour_Action_Show_Tour_Pauses, AS_CHECK_BOX);

      _tourChart = tourChart;

      setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TourPauses));
      setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TourPauses_disabled));
   }

   @Override
   public void run() {
      _tourChart.actionShowTourPauses(isChecked());
   }
}
