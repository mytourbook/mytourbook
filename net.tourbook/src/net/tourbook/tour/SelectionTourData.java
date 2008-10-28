/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import net.tourbook.data.TourData;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.viewers.ISelection;

/**
 * selection is fired when a tour was selected
 */
public class SelectionTourData implements ISelection {

	private TourChart	fTourChart;

	private TourData	fTourData;
	
	private boolean		fForceRedraw	= false;

	public SelectionTourData(final TourChart tourChart, final TourData tourData) {
		fTourChart = tourChart;
		fTourData = tourData;
	}

	/**
	 * @param tourChart
	 * @param tourData
	 * @param forceRedraw
	 *            when <code>true</code> the displayed tour should be redrawn because the
	 *            {@link TourData} has been changed
	 */
	public SelectionTourData(final TourChart tourChart, final TourData tourData, final boolean forceRedraw) {
		fTourChart = tourChart;
		fTourData = tourData;
		fForceRedraw = forceRedraw;
	}

	/**
	 * @return Returns the tour chart for the tour data or <code>null</code> when a tour chart is
	 *         not available
	 */
	public TourChart getTourChart() {
		return fTourChart;
	}

	public TourData getTourData() {
		return fTourData;
	}

	public boolean isEmpty() {
		return false;
	}

	public boolean isForceRedraw() {
		return fForceRedraw;
	}

	public void setForceRedraw(final boolean fForceRedraw) {
		this.fForceRedraw = fForceRedraw;
	}

	@Override
	public String toString() {
		return "SelectionTourData\ttourData:" + fTourData.toString();
	}

}
