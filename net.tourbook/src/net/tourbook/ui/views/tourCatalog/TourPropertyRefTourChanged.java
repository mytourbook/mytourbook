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
package net.tourbook.ui.views.tourCatalog;

import net.tourbook.ui.tourChart.TourChart;

public class TourPropertyRefTourChanged {

	long		refId;
	int			xMarkerValue;
	TourChart	refTourChart;

	/**
	 * @param refTourChart
	 *            reference tour chart
	 * @param refId
	 *            reference id
	 * @param refTourXMarkerValue
	 *            value difference in the reference tour
	 */
	public TourPropertyRefTourChanged(final TourChart refTourChart, final long refId, final int refTourXMarkerValue) {
		this.refTourChart = refTourChart;
		this.refId = refId;
		this.xMarkerValue = refTourXMarkerValue;
	}

}
