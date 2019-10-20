/*******************************************************************************
 * Copyright (C) 2019 Frédéric Bard
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
package net.tourbook.ui.views.performanceChart;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;

public class ActionShowAllStressScoreValues extends Action {

   private PerformanceChartView _performanceChartView;

   public ActionShowAllStressScoreValues(final PerformanceChartView performanceChartView) {

		super(UI.EMPTY_STRING, AS_CHECK_BOX);

		setToolTipText(Messages.Training_View_Action_ShowAllPulseValues);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ZoomFitGraph));

      _performanceChartView = performanceChartView;
	}

	@Override
	public void run() {
      _performanceChartView.actionShowAllStressScoreValues();
	}
}
