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

import net.tourbook.chart.ChartDataModel;
import net.tourbook.data.TourData;

public interface IDataModelListener {

	/**
	 * Method is called after the chart data model was created from the {@link TourData} and
	 * configuration <b>but</b> before the chart is updated. This method can be used to set the
	 * title or markers
	 * 
	 * @param fChartDataModel
	 */
	abstract void dataModelChanged(ChartDataModel chartDataModel);

}
