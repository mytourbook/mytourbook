/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;

import org.eclipse.jface.action.Action;

public class ActionGraph extends Action {

	private final TourChart	_tourChart;

	private final int		_graphId;

	/**
	 * Creates an action for a toggle button
	 * 
	 * @param fCompareTourChart
	 * @param fGraphId
	 * @param label
	 * @param toolTip
	 * @param imageEnabled
	 * @param imageDisabled
	 * @param isChecked
	 */
	public ActionGraph(	final TourChart tourChart,
						final int graphId,
						final String label,
						final String toolTip,
						final String imageEnabled,
						final String imageDisabled) {

		super(label, AS_CHECK_BOX);

		_tourChart = tourChart;
		_graphId = graphId;

		setToolTipText(toolTip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(imageEnabled));

		if (imageDisabled != null) {
			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(imageDisabled));
		}

		setChecked(tourChart.getTourChartConfig().getVisibleGraphs().contains(graphId));
	}

	@Override
	public void run() {

		final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

		if (tcc == null) {
			return;
		}

		final ArrayList<Integer> visibleGraphs = tcc.getVisibleGraphs();

		final boolean isThisGraphVisible = visibleGraphs.contains(_graphId);

		// check that at least one graph is visible
		if (isThisGraphVisible && visibleGraphs.size() == 1) {

			// this is a toggle button so the check status must be reset
			setChecked(true);

			return;
		}

		if (!isThisGraphVisible) {
			// add the graph to the list
			tcc.addVisibleGraph(_graphId);
		} else {
			// remove the graph from the list
			tcc.removeVisibleGraph(_graphId);
		}

		_tourChart.updateTourActions();
		_tourChart.updateTourChart();
	}

}
