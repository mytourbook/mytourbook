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
package net.tourbook.chart;

import java.util.ArrayList;
import java.util.HashMap;

public class ChartYDataMinMaxKeeper {

	/**
	 * min/max values for the y-axis data
	 */
	private HashMap<Integer, Integer>	minValues	= null;
	private HashMap<Integer, Integer>	maxValues	= null;

	public boolean hasMinMaxChanged(){
		boolean isEqual = minValues.equals(maxValues);
		return !isEqual;
	}
	
	/**
	 * save the min/max values from the given chart data fDataModel
	 * 
	 * @param fChartDataModel
	 */
	public void saveMinMaxValues(ChartDataModel chartDataModel) {

		if (chartDataModel == null) {
			// not data fDataModel is available
			return;
		}

		ArrayList<ChartDataSerie> xyData = chartDataModel.getXyData();

		minValues = new HashMap<Integer, Integer>();
		maxValues = new HashMap<Integer, Integer>();

		// loop: save min/max values for all data series
		for (ChartDataSerie chartData : xyData) {
			if (chartData instanceof ChartDataYSerie) {
				ChartDataYSerie yData = (ChartDataYSerie) chartData;

				Integer yDataInfo = (Integer) yData
						.getCustomData(ChartDataYSerie.YDATA_INFO);

				if (yDataInfo != null) {

					minValues.put(yDataInfo, yData.getMinValue());
					maxValues.put(yDataInfo, yData.getMaxValue());
				}
			}
		}
	}

	public void restoreMinMaxValues(ChartDataModel chartDataModel) {

		if (minValues == null) {
			// min/max values have not yet been save, so nothing can be restored
			return;
		}
		ArrayList<ChartDataSerie> xyData = chartDataModel.getXyData();

		// loop: restore min/max values for all data series
		for (ChartDataSerie chartData : xyData) {
			if (chartData instanceof ChartDataYSerie) {
				ChartDataYSerie yData = (ChartDataYSerie) chartData;

				Integer yDataInfo = (Integer) yData
						.getCustomData(ChartDataYSerie.YDATA_INFO);

				if (yDataInfo != null) {

					Integer minValue = minValues.get(yDataInfo);
					if (minValue != null) {
						yData.setMinValue(minValue);
					}

					Integer maxValue = maxValues.get(yDataInfo);
					if (maxValue != null) {
						yData.setMaxValue(maxValue);
					}
				}
			}
		}
	}

	HashMap<Integer, Integer> getMaxValues() {
		return maxValues;
	}

	HashMap<Integer, Integer> getMinValues() {
		return minValues;
	}

}
