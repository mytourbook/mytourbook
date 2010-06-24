/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.ui.views.TourChartAnalyzerView;
import net.tourbook.util.Util;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class ActionGraphAnalyzer extends Action {

	private final Chart	fChart;

	public ActionGraphAnalyzer(final Chart chart) {

		super(null, AS_PUSH_BUTTON);

		setToolTipText(Messages.Tour_Action_graph_analyzer_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__chart_analyzer));

		fChart = chart;
	}

	@Override
	public void run() {

		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		if (window != null) {

			Util.showView(TourChartAnalyzerView.ID);

			// create a new selection to update the analyzer view
			fChart.fireSliderMoveEvent();
		}
	}
}
