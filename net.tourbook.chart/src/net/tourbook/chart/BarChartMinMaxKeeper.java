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

import java.util.ArrayList;
import java.util.HashMap;

public class BarChartMinMaxKeeper {

	/**
	 * min/max values for the y-axis data
	 */
	private HashMap<Integer, Object>	fMinValues	= new HashMap<Integer, Object>();
	private HashMap<Integer, Object>	fMaxValues	= new HashMap<Integer, Object>();

	/**
	 * save the min/max values from the chart data model
	 * 
	 * @param chartDataModel
	 */
	public void setMinMaxValues(ChartDataModel chartDataModel) {

		if (chartDataModel == null) {
			// data fDataModel is not available
			return;
		}

		ArrayList<ChartDataYSerie> yDataSerie = chartDataModel.getYData();

		// loop: save min/max values for all data series
		Integer yDataId = 0;
		for (ChartDataSerie yData : yDataSerie) {
			if (yData instanceof ChartDataYSerie) {
				setYDataMinMaxValues((ChartDataYSerie) yData, yDataId++);
			}
		}
	}

	private void setYDataMinMaxValues(ChartDataYSerie yData, Integer yDataId) {

		/*
		 * set/restore min/max values
		 */
		int minValue = yData.getOriginalMinValue();
		int maxValue = yData.getOriginalMaxValue();

		Integer keeperMinValue = (Integer) fMinValues.get(yDataId);
		Integer keeperMaxValue = (Integer) fMaxValues.get(yDataId);

		if (keeperMinValue == null) {

			// min/max values have not yet been saved

			/*
			 * set the min value 10% below the computed so that the lowest value is not at the
			 * bottom
			 */
			keeperMinValue = minValue;
			keeperMaxValue = maxValue;

		} else {

			/*
			 * restore min/max values, but make sure min/max values for the current graph are
			 * visible and not outside of the chart
			 */
			keeperMinValue = Math.min(keeperMinValue, minValue);
			keeperMaxValue = Math.max(keeperMaxValue, maxValue);
		}

		// keep new min/max values
		fMinValues.put(yDataId, keeperMinValue);
		fMaxValues.put(yDataId, keeperMaxValue);

		yData.setVisibleMinValue(keeperMinValue);
		yData.setVisibleMaxValue(keeperMaxValue);
	}

	public void resetMinMax() {
		fMinValues = new HashMap<Integer, Object>();
		fMaxValues = new HashMap<Integer, Object>();

	}

}
