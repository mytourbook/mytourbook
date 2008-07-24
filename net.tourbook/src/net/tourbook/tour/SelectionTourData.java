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
package net.tourbook.tour;

import net.tourbook.data.TourData;

import org.eclipse.jface.viewers.ISelection;

/**
 * selection is fired when a tour was selected
 */
public class SelectionTourData implements ISelection {

	private TourChart	fTourChart;
	private TourData	fTourData;

	public SelectionTourData(final TourChart tourChart, final TourData tourData) {
		fTourChart = tourChart;
		fTourData = tourData;
	}

	public boolean isEmpty() {
		return false;
	}

	public TourData getTourData() {
		return fTourData;
	}

	/**
	 * @return Returns the tour chart for the tour data or <code>null</code> when a tour chart is
	 *         not available
	 */
	public TourChart getTourChart() {
		return fTourChart;
	}

}
