/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.chart;

/**
 * The chart manager keeps track of the active chart
 */
public class ChartManager {

	private static ChartManager	instance;
	private Chart				fActiveChart;

	private ChartManager() {}

	public static ChartManager getInstance() {

		if (instance == null) {
			instance = new ChartManager();
		}

		return instance;
	}

	/**
	 * Return the chart which has the focus
	 */
	public Chart getActiveChart() {
		return fActiveChart;
	}

	/**
	 * Set the chart which currently has the focus
	 * 
	 * @param activeChart
	 */
	public void setActiveChart(Chart activeChart) {
		fActiveChart = activeChart;
	}
}
