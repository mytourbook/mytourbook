/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views;

import net.tourbook.chart.ComputeChartValue;

/**
 * Contains the information which is needed to draw the average values in the tour chart analyzer.
 */
public class TourChartAnalyzerInfo {

	private ComputeChartValue	_computeValues	= null;

	private boolean				_isShowAvg;
	private boolean				_isShowAvgDecimals;

	private int					_avgDecimals	= 1;

	/**
	 * default constructor
	 */
	public TourChartAnalyzerInfo() {}

	/**
	 * @param isShowAvg
	 */
	public TourChartAnalyzerInfo(final boolean isShowAvg) {

		_isShowAvg = isShowAvg;
	}

	/**
	 * @param isShowAvg
	 * @param isShowAvgDecimals
	 */
	public TourChartAnalyzerInfo(final boolean isShowAvg, final boolean isShowAvgDecimals) {

		_isShowAvg = isShowAvg;
		_isShowAvgDecimals = isShowAvgDecimals;
	}

	/**
	 * @param isShowAvg
	 * @param isShowAvgDecimals
	 * @param computeValues
	 * @param avgDecimals
	 */
	public TourChartAnalyzerInfo(	final boolean isShowAvg,
									final boolean isShowAvgDecimals,
									final ComputeChartValue computeValues,
									final int avgDecimals) {

		_isShowAvg = isShowAvg;
		_isShowAvgDecimals = isShowAvgDecimals;
		_computeValues = computeValues;
		_avgDecimals = avgDecimals;
	}

	/**
	 * @param isShowAvg
	 * @param computeValues
	 */
	public TourChartAnalyzerInfo(final boolean isShowAvg, final ComputeChartValue computeValues) {

		_isShowAvg = isShowAvg;
		_computeValues = computeValues;
	}

	/**
	 * @return Returns the avgDecimals.
	 */
	public int getAvgDecimals() {
		return _avgDecimals;
	}

	/**
	 * @return Returns the computeChartValue.
	 */
	public ComputeChartValue getComputeChartValue() {
		return _computeValues;
	}

	/**
	 * @return Returns the showAvg.
	 */
	public boolean isShowAvg() {
		return _isShowAvg;
	}

	/**
	 * @return Returns the showAvgDecimals.
	 */
	public boolean isShowAvgDecimals() {
		return _isShowAvgDecimals;
	}

}
