/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
	private HashMap<Integer, Double>	_minValues	= null;
	private HashMap<Integer, Double>	_maxValues	= null;

	public ChartYDataMinMaxKeeper() {}

	HashMap<Integer, Double> getMaxValues() {
		return _maxValues;
	}

	HashMap<Integer, Double> getMinValues() {
		return _minValues;
	}

	public boolean hasMinMaxChanged() {
		final boolean isEqual = _minValues.equals(_maxValues);
		return !isEqual;
	}

	/**
	 * keep the min/max values for all data series from the data model
	 * 
	 * @param chartDataModel
	 */
	public void saveMinMaxValues(final ChartDataModel chartDataModel) {

		if (chartDataModel == null) {
			return;
		}

		final ArrayList<ChartDataSerie> xyData = chartDataModel.getXyData();

		_minValues = new HashMap<Integer, Double>();
		_maxValues = new HashMap<Integer, Double>();

		// loop: save min/max values for all data series
		for (final ChartDataSerie chartData : xyData) {

			if (chartData instanceof ChartDataYSerie) {

				final ChartDataYSerie yData = (ChartDataYSerie) chartData;

				final Integer yDataInfo = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_INFO);

				if (yDataInfo != null) {

					final double visibleMinValue = yData.getVisibleMinValue();
					double visibleMaxValue = yData.getVisibleMaxValue();

					// prevent setting to the same value,
					if (visibleMinValue == visibleMaxValue) {
						visibleMaxValue++;
					}

					_minValues.put(yDataInfo, visibleMinValue);
					_maxValues.put(yDataInfo, visibleMaxValue);
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
	public void setMinMaxValues(final ChartDataModel chartDataModelOut) {

		if (_minValues == null) {
			// min/max values have not yet been saved, so nothing can be restored
			return;
		}
//		System.out.println("restoreMinMaxValues" + chartDataModel);
		final ArrayList<ChartDataSerie> xyData = chartDataModelOut.getXyData();

		// loop: restore min/max values for all data series
		for (final ChartDataSerie chartData : xyData) {
			if (chartData instanceof ChartDataYSerie) {

				final ChartDataYSerie yData = (ChartDataYSerie) chartData;

				final Integer yDataInfo = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_INFO);

				if (yDataInfo != null) {

					final Double minValue = _minValues.get(yDataInfo);
					if (minValue != null) {
						yData.setVisibleMinValue(minValue);
					}

					final Double maxValue = _maxValues.get(yDataInfo);
					if (maxValue != null) {
						yData.setVisibleMaxValue(maxValue);
					}
				}
			}
		}
	}

}
