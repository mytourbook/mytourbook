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
import java.util.Map.Entry;

/**
 * This is the data model for a chart to draw it. All data which are required to draw the chart must
 * be contained in this data model.
 */
public class ChartDataModel {

	public static final String			BAR_TOOLTIP_INFO_PROVIDER	= "BarToolTipInfoProvider";		//$NON-NLS-1$
	public static final String			BAR_CONTEXT_PROVIDER		= "BarContextProvider";			//$NON-NLS-1$

	private ChartType					_chartType;

	private ChartDataXSerie				_xData						= null;
	private ChartDataXSerie				_xData2nd					= null;

	/**
	 * title for the chart, will be positioned on top of the chart
	 */
	private String						_title;

	/**
	 * Contains data series for the y axis
	 */
	private ArrayList<ChartDataYSerie>	_yData						= new ArrayList<ChartDataYSerie>();

	/**
	 * Contains all data series for the x and y axis and also hidden data which are not displayed in
	 * the chart
	 */
	private ArrayList<ChartDataSerie>	_xyData						= new ArrayList<ChartDataSerie>();

	/**
	 * storage for custom data
	 */
	private HashMap<String, Object>		_customData					= new HashMap<String, Object>();

	/**
	 * Is <code>true</code> when the graphs are all painted at the same position to overlap each
	 * other. <br>
	 * Is <code>false</code> when the graphs are painted vertically positioned above each other
	 * (which is the default since the first 10 years and also the reason why MyTourbook was
	 * created).
	 */
	private boolean						_isGraphOverlapped;

	/**
	 * minimum width for the chart, this can be overwritten for e.g. to show in a year chart for
	 * each day at least one pixel
	 */
	private int							_chartMinWidth				= ChartComponents.CHART_MIN_WIDTH;

	/**
	 * this error message is displayed when data for the chart are not available
	 */
	private String						_errorMessage;

	/**
	 * Is <code>true</code> when skipped values are displayed as dots.
	 */
	private boolean						_isNoLinesValuesDisplayed;

	public ChartDataModel(final ChartType chartType) {
		_chartType = chartType;
	}

	public void addXyData(final ChartDataSerie data) {
		_xyData.add(data);
	}

	/**
	 * @param data
	 */
	public void addYData(final ChartDataYSerie data) {
		_yData.add(data);
	}

	public int getChartMinWidth() {
		return _chartMinWidth;
	}

	/**
	 * @return returns the charttype, this can be CHART_TYPE_LINE, CHART_TYPE_BAR
	 */
	public ChartType getChartType() {
		return _chartType;
	}

	/**
	 * Returns the application defined property of the receiver with the specified name, or null if
	 * it has not been set.
	 */
	public Object getCustomData(final String key) {
		if (_customData.containsKey(key)) {
			return _customData.get(key);
		} else {
			return null;
		}
	}

	public String getErrorMessage() {
		return _errorMessage;
	}

	public String getTitle() {
		return _title;
	}

	/**
	 * @return returns the dataseries which is used for the x axis
	 */
	public ChartDataXSerie getXData() {

		// create a fail save data series if none is set
		if (_xData == null) {
			_xData = new ChartDataXSerie(new double[0]);
		}
		return _xData;
	}

	/**
	 * @return Returns the xData2nd.
	 */
	public ChartDataXSerie getXData2nd() {
		return _xData2nd;
	}

	public ArrayList<ChartDataSerie> getXyData() {
		return _xyData;
	}

	/**
	 * @return returns the y data list
	 */
	public ArrayList<ChartDataYSerie> getYData() {
		return _yData;
	}

	/**
	 * @return Returns <code>true</code> when all graphs are painted at the same position to overlap
	 *         each other.<br>
	 *         Returns <code>false</code> when the graphs are painted vertically positioned above
	 *         each other (which is the default since the first 10 years and also the reason why
	 *         MyTourbook was created).
	 */
	public boolean isGraphOverlapped() {
		return _isGraphOverlapped;
	}

	public boolean isNoLinesValuesDisplayed() {
		return _isNoLinesValuesDisplayed;
	}

	/**
	 * reset the min/max values of the chart to the min/max values from the original data
	 */
	public void resetMinMaxValues() {

		_xData._visibleMinValue = _xData._originalMinValue;
		_xData._visibleMaxValue = _xData._originalMaxValue;

		for (final ChartDataYSerie ySerie : _yData) {
			ySerie._visibleMinValue = ySerie.getOriginalMinValue();
			ySerie._visibleMaxValue = ySerie.getOriginalMaxValue();
		}
	}

	public void setChartMinWidth(final int chartMinWidth) {
		_chartMinWidth = chartMinWidth;
	}

	public void setChartType(final ChartType chartType) {
		_chartType = chartType;
	}

	/**
	 * Sets the application defined property of the receiver with the specified name to the given
	 * value.
	 */
	public void setCustomData(final String key, final Object value) {
		_customData.put(key, value);
	}

	public void setErrorMessage(final String errorMessage) {
		_errorMessage = errorMessage;
	}

	public void setIsGraphOverlapped(final boolean isOverlapped) {
		_isGraphOverlapped = isOverlapped;
	}

	public void setShowNoLineValues(final boolean isNoLinesValuesDisplayed) {
		_isNoLinesValuesDisplayed = isNoLinesValuesDisplayed;
	}

	public void setTitle(final String title) {
		_title = title;
	}

	public void setXData(final ChartDataXSerie data) {
		_xData = data;
	}

	/**
	 * @param data2nd
	 *            The xData2nd to set.
	 */
	public void setXData2nd(final ChartDataXSerie data2nd) {
		_xData2nd = data2nd;
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();

		sb.append("[ChartDataModel]");//$NON-NLS-1$
		sb.append("\n\ttitle:<" + _title + ">");//$NON-NLS-1$//$NON-NLS-2$

		if (_customData != null) {

			sb.append("\n\tcustomData: ");//$NON-NLS-1$

			for (final Entry<String, Object> entry : _customData.entrySet()) {

				sb.append("\t"); //$NON-NLS-1$
				sb.append(entry.getKey());
				sb.append(":");//$NON-NLS-1$
				sb.append(entry.getValue());
			}
		}

		return sb.toString();
	}

}
