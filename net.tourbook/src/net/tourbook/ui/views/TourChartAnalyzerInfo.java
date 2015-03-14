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
package net.tourbook.ui.views;

import net.tourbook.chart.ComputeChartValue;

/**
 * Contains the information which is needed to draw the average values in the tour chart analyzer.
 */
public class TourChartAnalyzerInfo {

	private ComputeChartValue	_computeValues		= null;

	private boolean				_showAvg			= false;
	private boolean				_showAvgDecimals	= false;
	private int					_avgDecimals		= 1;

	/**
	 * default constructor
	 */
	public TourChartAnalyzerInfo() {}

	/**
	 * @param showAvg
	 */
	public TourChartAnalyzerInfo(final boolean showAvg) {
		this._showAvg = showAvg;
	}

	/**
	 * @param showAvg
	 * @param showAvgDecimals
	 */
	public TourChartAnalyzerInfo(final boolean showAvg, final boolean showAvgDecimals) {
		this._showAvg = showAvg;
		this._showAvgDecimals = showAvgDecimals;
	}

	/**
	 * @param showAvg
	 * @param showAvgDecimals
	 * @param computeValues
	 * @param avgDecimals
	 */
	public TourChartAnalyzerInfo(	final boolean showAvg,
									final boolean showAvgDecimals,
									final ComputeChartValue computeValues,
									final int avgDecimals) {

		_showAvg = showAvg;
		_showAvgDecimals = showAvgDecimals;
		_computeValues = computeValues;
		_avgDecimals = avgDecimals;
	}

	/**
	 * @param showAvg
	 * @param computeValues
	 */
	public TourChartAnalyzerInfo(final boolean showAvg, final ComputeChartValue computeValues) {
		_showAvg = showAvg;
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
		return _showAvg;
	}

	/**
	 * @return Returns the showAvgDecimals.
	 */
	public boolean isShowAvgDecimals() {
		return _showAvgDecimals;
	}

}
