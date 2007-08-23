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
/**
 * @author Wolfgang Schramm
 * 
 * created: 06.07.2005
 */

package net.tourbook.tour;

import java.util.ArrayList;

import net.tourbook.chart.ChartYDataMinMaxKeeper;

/**
 * Configuration how the tour chart is displayed
 */
public class TourChartConfiguration {

	/**
	 * true: show time on the x-axis
	 * <p>
	 * false: show distance on the x-axis
	 */
	public boolean					showTimeOnXAxis			= false;

	public boolean					showTimeOnXAxisBackup	= false;

	/**
	 * true: show the start time of the tour
	 * <p>
	 * false: show the tour time which starts at 0
	 */
	public boolean					isStartTime				= false;

	/**
	 * contains a list for all graphs which are displayed, the sequence of the list is the sequence
	 * in which the graphs will be displayed
	 */
	private ArrayList<Integer>		visibleGraphSequence	= new ArrayList<Integer>();

	/**
	 * contains the min/max keeper or <code>null</code> when min/max is not kept
	 */
	private ChartYDataMinMaxKeeper	fMinMaxKeeper;

	/**
	 * the graph can be scrolled when set to <code>true</code>
	 */
	public boolean					scrollZoomedGraph		= false;

	/**
	 * the graph is automatically zoomed to the slider position when the slider is moved
	 */
	public boolean					autoZoomToSlider		= false;

	/**
	 * @param keepMinMaxValues
	 *        set <code>true</code> to keep min/max values when tour data will change
	 */
	public TourChartConfiguration(boolean keepMinMaxValues) {
		if (keepMinMaxValues) {
			setMinMaxKeeper(true);
		}
	}

	public void addVisibleGraph(int visibleGraph) {
		visibleGraphSequence.add(visibleGraph);
	}

	public void removeVisibleGraph(int selectedGraphId) {

		int graphIndex = 0;

		for (Integer graphId : visibleGraphSequence) {
			if (graphId == selectedGraphId) {
				visibleGraphSequence.remove(graphIndex);
				break;
			}
			graphIndex++;
		}
	}

	public ArrayList<Integer> getVisibleGraphs() {
		return visibleGraphSequence;
	}

	/**
	 * <code>true</code> indicates to keep the min/max values in the chart configuration when the
	 * data model was changed, this has the higher priority than keeping the min/max values in the
	 * chart widget
	 * 
	 * @param keepMinMaxValues
	 *        the keepMinMaxValues to set
	 */
	public void setMinMaxKeeper(boolean keepMinMaxValues) {
		if (keepMinMaxValues) {
			fMinMaxKeeper = new ChartYDataMinMaxKeeper();
		} else {
			fMinMaxKeeper = null;
		}
	}

	/**
	 * @return Returns the min/max keeper of the chart configuration or <code>null</code> when a
	 *         min/max keeper is not set
	 */
	ChartYDataMinMaxKeeper getMinMaxKeeper() {
		return fMinMaxKeeper;
	}

}
