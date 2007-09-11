/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

import java.util.ArrayList;

import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

class ActionGraph extends Action {

	private final TourChart	fTourChart;

	private final int		fGraphId;

	/**
	 * Creates an action for a toggle button
	 * 
	 * @param fCompareTourChart
	 * @param fGraphId
	 * @param label
	 * @param toolTip
	 * @param image
	 * @param isChecked
	 */
	public ActionGraph(TourChart tourChart, int graphId, String label, String toolTip, String image) {

		super(label, AS_CHECK_BOX);

		fTourChart = tourChart;
		fGraphId = graphId;

		setToolTipText(toolTip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(image));

		setChecked(tourChart.fTourChartConfig.getVisibleGraphs().contains(graphId));
	}

	@Override
	public void run() {

		TourChartConfiguration chartConfig = fTourChart.fTourChartConfig;
		final ArrayList<Integer> visibleGraphs = chartConfig.getVisibleGraphs();

		final boolean isThisGraphVisible = visibleGraphs.contains(fGraphId);

		// check that at least one graph is visible
		if (isThisGraphVisible && visibleGraphs.size() == 1) {

			// this is a toggle button so the check status must be reset
			setChecked(true);

			return;
		}

		if (!isThisGraphVisible) {
			// add the graph to the list
			chartConfig.addVisibleGraph(fGraphId);
		} else {
			// remove the graph from the list
			chartConfig.removeVisibleGraph(fGraphId);
		}

		fTourChart.updateActionState();
		fTourChart.updateTourChart(true);
	}

}
