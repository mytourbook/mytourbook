/*******************************************************************************
 * Copyright (C) 2005, 2020  Wolfgang Schramm and Contributors
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
import net.tourbook.common.UI;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.action.Action;

public class ActionXAxisDistance extends Action {

   private TourChart fTourChart;

   public ActionXAxisDistance(final TourChart tourChart) {

      super(Messages.Tour_Action_show_distance_on_x_axis, AS_RADIO_BUTTON);

      this.fTourChart = tourChart;

      setToolTipText(Messages.Tour_Action_show_distance_on_x_axis_tooltip);

      setImages();

      setChecked(!tourChart.getTourChartConfig().isShowTimeOnXAxis);
   }

   @Override
   public void run() {
      fTourChart.actionXAxisDistance(isChecked());
   }

   public void setImages() {

      String imagePath = Messages.Image__show_distance_on_x_axis;
      String disabledImagePath = Messages.Image__show_distance_on_x_axis_disabled;

      if (!UI.UNIT_IS_METRIC) {
         imagePath = Messages.Image__show_distance_on_x_axis_imperial;
         disabledImagePath = Messages.Image__show_distance_on_x_axis_imperial_disabled;
      }

      setImageDescriptor(TourbookPlugin.getImageDescriptor(imagePath));
      setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(disabledImagePath));
   }

}
