/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.ui.tourChart.SlideoutTourChartPauses;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

public class ActionTourChartPauses extends ActionToolbarSlideout {

   private TourChart _tourChart;
   private Control   _ownerControl;
   private IDialogSettings _state;

   public ActionTourChartPauses(final TourChart tourChart, final Control ownerControl, final IDialogSettings state) {

      super(TourbookPlugin.getThemedImageDescriptor(Images.TourPauses),
            TourbookPlugin.getThemedImageDescriptor(Images.TourPauses_Disabled));

      notSelectedTooltip = Messages.Tour_Action_ShowTourPauses_Tooltip;

      isToggleAction = true;

      _tourChart = tourChart;
      _ownerControl = ownerControl;
      _state = state;
   }

   @Override
   protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

      return new SlideoutTourChartPauses(_ownerControl, toolbar, _tourChart, _state);
   }

   @Override
   protected void onBeforeOpenSlideout() {

      _tourChart.closeOpenedDialogs(this);
   }

   @Override
   protected void onSelect() {

      super.onSelect();

      _tourChart.updateUI_Pauses();
   }
}
