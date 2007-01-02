/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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

public class ActionZoomFitGraph extends Action {

	private TourChart	tourChart;

	public ActionZoomFitGraph(TourChart tourChart) {

		super("ZoomFitGraph", AS_PUSH_BUTTON);

		this.tourChart = tourChart;

		setToolTipText("Fit chart to window");

		setImageDescriptor(TourbookPlugin.getImageDescriptor("zoom-fit-graph.gif"));
	}

	public void run() {

		tourChart.zoomOut(false);
		tourChart.updateChart(false);
	}
}
