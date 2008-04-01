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

package net.tourbook.statistics;

import java.util.Calendar;

import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.ui.UI;

public class StatisticDayDistance extends StatisticDay {

	@Override
	void updateChart(TourDataTour tourTimeData, long lastSelectedTourId) {

		ChartDataModel chartModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		// set the x-axis
		ChartDataXSerie xData = new ChartDataXSerie(tourTimeData.fDOYValues);
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_YEAR);
		xData.setVisibleMaxValue(fCurrentYear);
		xData.setChartSegments(createChartSegments(tourTimeData));
		chartModel.setXData(xData);

		/*
		 * Distance
		 */
		ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				tourTimeData.fDistanceLow,
				tourTimeData.fDistanceHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
		yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
		yData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_NUMBER);
		yData.setAllValueColors(0);
		yData.setVisibleMinValue(0);
		yData.setCustomData(DISTANCE_DATA, 1);
		chartModel.addYData(yData);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE);
		yData.setColorIndex(new int[][] { tourTimeData.fTypeColorIndex });

		/*
		 * set graph minimum width, these is the number of days in the year
		 */
		fCalendar.set(fCurrentYear, 11, 31);
		int yearDays = fCalendar.get(Calendar.DAY_OF_YEAR);
		chartModel.setChartMinWidth(yearDays);

		setChartProviders(fChart, chartModel);

		if (fIsSynchScaleEnabled) {
			fMinMaxKeeper.setMinMaxValues(chartModel);
		}

		// show the data in the chart
		fChart.updateChart(chartModel, false);

		// try to select the previous selected tour
		selectTour(lastSelectedTourId);
	}
}
