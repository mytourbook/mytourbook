/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

/**
 * is the data model which is used by chart to draw it. All data which are required to draw the
 * chart must be contained in this data model.
 */
public class ChartDataModel {

	public static final int				CHART_TYPE_LINE				= 10;
	public static final int				CHART_TYPE_BAR				= 20;
	public static final int				CHART_TYPE_LINE_WITH_BARS	= 30;

	public static final String			BAR_TOOLTIP_INFO_PROVIDER	= "BarToolTipInfoProvider";		//$NON-NLS-1$
	public static final String			BAR_CONTEXT_PROVIDER		= "BarContextProvider";			//$NON-NLS-1$

	private int							fChartType					= CHART_TYPE_LINE;

	private ChartDataXSerie				xData						= null;
	private ChartDataXSerie				xData2nd					= null;

	/**
	 * title for the chart, will be positioned on top of the chart
	 */
	private String						fTitle;

	/**
	 * Contains data series for the y axis
	 */
	private ArrayList<ChartDataYSerie>	yData						= new ArrayList<ChartDataYSerie>();

	/**
	 * Contains all data series for the x and y axis and also hidden data which are not displayed in
	 * the chart
	 */
	private ArrayList<ChartDataSerie>	xyData						= new ArrayList<ChartDataSerie>();

	/**
	 * storage for custom data
	 */
	private HashMap<String, Object>		fCustomData					= new HashMap<String, Object>();

	/**
	 * true: the chart graphs are separate drawn vertically
	 * <p>
	 * false: the chart graphs are drawn on top of each other
	 */
	private boolean						isStackedChart				= true;

	/**
	 * minimum width for the chart, this can be overwritten for e.g. to show in a year chart for
	 * each day at least one pixel
	 */
	private int							chartMinWidth				= ChartComponents.CHART_MIN_WIDTH;

	public ChartDataModel(final int chartType) {
		fChartType = chartType;
	}

	public void addXyData(final ChartDataSerie data) {
		xyData.add(data);
	}

	/**
	 * @param data
	 */
	public void addYData(final ChartDataYSerie data) {
		yData.add(data);
	}

	public int getChartMinWidth() {
		return chartMinWidth;
	}

	/**
	 * @return returns the charttype, this can be CHART_TYPE_LINE, CHART_TYPE_BAR
	 */
	public int getChartType() {
		return fChartType;
	}

	/**
	 * Returns the application defined property of the receiver with the specified name, or null if
	 * it has not been set.
	 */
	public Object getCustomData(final String key) {
		if (fCustomData.containsKey(key)) {
			return fCustomData.get(key);
		} else {
			return null;
		}
	}

	public String getTitle() {
		return fTitle;
	}

	/**
	 * @return returns the dataseries which is used for the x axis
	 */
	public ChartDataXSerie getXData() {

		// create a fail save data series if none is set
		if (xData == null) {
			xData = new ChartDataXSerie(new int[0]);
		}
		return xData;
	}

	/**
	 * @return Returns the xData2nd.
	 */
	public ChartDataXSerie getXData2nd() {
		return xData2nd;
	}

	public ArrayList<ChartDataSerie> getXyData() {
		return xyData;
	}

	/**
	 * @return returns the y data list
	 */
	public ArrayList<ChartDataYSerie> getYData() {
		return yData;
	}

	public boolean isStackedChart() {
		return isStackedChart;
	}

	/**
	 * reset the min/max values of the chart to the min/max values from the original data
	 */
	public void resetMinMaxValues() {

		for (final ChartDataYSerie ySerie : yData) {
			ySerie.fVisibleMinValue = ySerie.getOriginalMinValue();
			ySerie.fVisibleMaxValue = ySerie.getOriginalMaxValue();
		}
	}

	public void setChartMinWidth(final int chartMinWidth) {
		this.chartMinWidth = chartMinWidth;
	}

	/**
	 * Sets the application defined property of the receiver with the specified name to the given
	 * value.
	 */
	public void setCustomData(final String key, final Object value) {
		fCustomData.put(key, value);
	}

	public void setStackedChart(final boolean isStackedChart) {
		this.isStackedChart = isStackedChart;
	}

	public void setTitle(final String title) {
		this.fTitle = title;
	}

	public void setXData(final ChartDataXSerie data) {
		xData = data;
	}

	/**
	 * @param data2nd
	 *            The xData2nd to set.
	 */
	public void setXData2nd(final ChartDataXSerie data2nd) {
		xData2nd = data2nd;
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();

		sb.append("[ChartDataModel]");//$NON-NLS-1$
		sb.append("\n\ttitle:<" + fTitle + ">");//$NON-NLS-1$//$NON-NLS-2$

		if (fCustomData != null) {

			sb.append("\n\tcustomData: ");

			for (final Object customData : fCustomData.values()) {
				sb.append("\t");//$NON-NLS-1$
				sb.append(customData);
			}
		}

		return sb.toString();
	}

}
