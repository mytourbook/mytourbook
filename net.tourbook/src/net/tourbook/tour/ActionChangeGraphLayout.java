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

import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

class ActionChangeGraphLayout extends Action {

	private TourChart				fTourChart;

	private int						fGraphId;

	/**
	 * Creates an action for a toggle button
	 * 
	 * @param fTourChart
	 * @param fGraphId
	 * @param label
	 * @param toolTip
	 * @param image
	 * @param isChecked
	 */
	public ActionChangeGraphLayout(TourChart tourChart,
			int id, String label, String toolTip,
			String image) {

		super(label, AS_CHECK_BOX);

		fTourChart = tourChart;
		fGraphId = id;

		setToolTipText(toolTip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(image));

		setChecked(tourChart.fTourChartConfig.getVisibleGraphs().contains(id));
	}

	public void run() {

		TourChartConfiguration chartConfig = fTourChart.fTourChartConfig;
		
		boolean graphIsVisible = chartConfig.getVisibleGraphs().contains(fGraphId);

		if (!graphIsVisible) {
			// add the graph to the list
			chartConfig.addVisibleGraph(fGraphId);
		} else {
			// remove the graph from the list
			chartConfig.removeVisibleGraph(fGraphId);
		}

		fTourChart.enableActions();
		fTourChart.updateChart();
	}

}
