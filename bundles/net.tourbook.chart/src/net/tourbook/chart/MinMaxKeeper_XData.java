/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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

public class MinMaxKeeper_XData {

	/*
	 * min/max values for the y-axis data
	 */
	private double	_minValue	= Double.MIN_VALUE;
	private double	_maxValue	= Double.MIN_VALUE;

	/**
	 * This offset is used that the data have a padding to the chart border.
	 */
	private int		_minMaxPadding;

	public MinMaxKeeper_XData(final int minMaxPadding) {

		_minMaxPadding = minMaxPadding;
	}

	public void resetMinMax() {

		_minValue = Double.MIN_VALUE;
		_maxValue = Double.MIN_VALUE;
	}

	/**
	 * Save the min/max values from the chart data model
	 * 
	 * @param chartDataModel
	 */
	public void setMinMaxValues(final ChartDataModel chartDataModel) {

		// check required data
		if (chartDataModel == null) {
			return;
		}

		final ChartDataXSerie xData = chartDataModel.getXData();

		final double originalMinValue = xData.getOriginalMinValue();
		final double originalMaxValue = xData.getOriginalMaxValue();

		double keeperMinValue = _minValue;
		double keeperMaxValue = _maxValue;

		if (keeperMinValue == Double.MIN_VALUE) {

			// min/max values have not yet been kept

			keeperMinValue = originalMinValue;
			keeperMaxValue = originalMaxValue;

		} else {

			/*
			 * Get kept min/max values, but make sure min/max values are visible and not outside of
			 * the chart
			 */
			keeperMinValue = Math.min(keeperMinValue, originalMinValue);
			keeperMaxValue = Math.max(keeperMaxValue, originalMaxValue);
		}

		// keep new min/max values
		_minValue = keeperMinValue;
		_maxValue = keeperMaxValue;

		xData.forceXAxisMinValue(keeperMinValue - _minMaxPadding);
		xData.forceXAxisMaxValue(keeperMaxValue + _minMaxPadding);

		xData.setVisibleMinValue(keeperMinValue - _minMaxPadding);
		xData.setVisibleMaxValue(keeperMaxValue + _minMaxPadding);
	}

}
