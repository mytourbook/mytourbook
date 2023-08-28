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
package net.tourbook.ui.tourChart.action;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Event;

public class ActionXAxisDistance extends Action {

   private TourChart _tourChart;

   public ActionXAxisDistance(final TourChart tourChart) {

      super(UI.SPACE1, AS_RADIO_BUTTON);

      _tourChart = tourChart;

      setToolTipText(Messages.Tour_Action_ShowDistanceOnXAxis_Tooltip);

      setImages();

      setChecked(!tourChart.getTourChartConfig().isShowTimeOnXAxis);
   }

   @Override
   public void runWithEvent(final Event event) {

      _tourChart.actionXAxisDistance(event, isChecked());
   }

   public void setImages() {

      String imageName;
      String disabledImageName;

      if (UI.UNIT_IS_DISTANCE_MILE) {

         imageName = Images.XAxis_ShowDistance_Imperial;
         disabledImageName = Images.XAxis_ShowDistance_Imperial_Disabled;

      } else if (UI.UNIT_IS_DISTANCE_NAUTICAL_MILE) {

         imageName = Images.XAxis_ShowDistance_NauticMile;
         disabledImageName = Images.XAxis_ShowDistance_NauticMile_Disabled;

      } else {

         imageName = Images.XAxis_ShowDistance;
         disabledImageName = Images.XAxis_ShowDistance_Disabled;
      }

      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(imageName));
      setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(disabledImageName));
   }
}
