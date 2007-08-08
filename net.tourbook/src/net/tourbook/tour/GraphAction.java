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

class GraphAction extends Action {

	private TourChart	fTourChart;

	private int			fMapId;

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
	public GraphAction(TourChart tourChart, int mapId, String label, String toolTip, String image) {

		super(label, AS_CHECK_BOX);

		fTourChart = tourChart;
		fMapId = mapId;

		setToolTipText(toolTip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(image));

		setChecked(tourChart.fTourChartConfig.getVisibleGraphs().contains(mapId));
	}

	public void run() {

		TourChartConfiguration chartConfig = fTourChart.fTourChartConfig;

		boolean isGraphVisible = chartConfig.getVisibleGraphs().contains(fMapId);

		if (!isGraphVisible) {
			// add the graph to the list
			chartConfig.addVisibleGraph(fMapId);
		} else {
			// remove the graph from the list
			chartConfig.removeVisibleGraph(fMapId);
		}

		fTourChart.enableActions();
		fTourChart.updateChart(true);
	}

}
