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
package net.tourbook.chart;

import java.util.ArrayList;
import java.util.HashMap;

public class ChartYDataMinMaxKeeper {

	/**
	 * min/max values for the y-axis data
	 */
	private HashMap<Integer, Integer>	fMinValues	= null;
	private HashMap<Integer, Integer>	fMaxValues	= null;

	public boolean hasMinMaxChanged() {
		boolean isEqual = fMinValues.equals(fMaxValues);
		return !isEqual;
	}

	public ChartYDataMinMaxKeeper() {}

	/**
	 * keep the min/max values for all data series from the data model
	 * 
	 * @param chartDataModel
	 */
	public void saveMinMaxValues(ChartDataModel chartDataModel) {

		if (chartDataModel == null) {
			return;
		}

		ArrayList<ChartDataSerie> xyData = chartDataModel.getXyData();

		fMinValues = new HashMap<Integer, Integer>();
		fMaxValues = new HashMap<Integer, Integer>();

		// loop: save min/max values for all data series
		for (ChartDataSerie chartData : xyData) {
			if (chartData instanceof ChartDataYSerie) {
				ChartDataYSerie yData = (ChartDataYSerie) chartData;

				Integer yDataInfo = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_INFO);

				if (yDataInfo != null) {
					final int visibleMinValue = yData.getVisibleMinValue();
					int visibleMaxValue = yData.getVisibleMaxValue();

					// prevent setting to the same value,
					if (visibleMinValue == visibleMaxValue) {
						visibleMaxValue++;
					}

					fMinValues.put(yDataInfo, visibleMinValue);
					fMaxValues.put(yDataInfo, visibleMaxValue);
				}
			}
		}
	}

	/**
	 * Set min/max values from this min/max keeper into a data model
	 * 
	 * @param chartDataModelOut
	 *        data model which min/max data will be set from this min/max keeper
	 */
	public void setMinMaxValues(ChartDataModel chartDataModelOut) {

		if (fMinValues == null) {
			// min/max values have not yet been saved, so nothing can be restored
			return;
		}
//		System.out.println("restoreMinMaxValues" + chartDataModel);
		ArrayList<ChartDataSerie> xyData = chartDataModelOut.getXyData();

		// loop: restore min/max values for all data series
		for (ChartDataSerie chartData : xyData) {
			if (chartData instanceof ChartDataYSerie) {
				ChartDataYSerie yData = (ChartDataYSerie) chartData;

				Integer yDataInfo = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_INFO);

				if (yDataInfo != null) {

					Integer minValue = fMinValues.get(yDataInfo);
					if (minValue != null) {
						yData.setVisibleMinValue(minValue);
					}

					Integer maxValue = fMaxValues.get(yDataInfo);
					if (maxValue != null) {
						yData.setVisibleMaxValue(maxValue);
					}
				}
			}
		}
	}

	HashMap<Integer, Integer> getMaxValues() {
		return fMaxValues;
	}

	HashMap<Integer, Integer> getMinValues() {
		return fMinValues;
	}

}
