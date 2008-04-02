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
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.ui.UI;

public class StatisticDayAltitude extends StatisticDay {

	@Override
	void updateChart(final TourDataTour tourDataTour, long lastSelectedTourId) {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(tourDataTour.fDOYValues);
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_DAY);
		xData.setVisibleMaxValue(fCurrentYear);
		xData.setChartSegments(createChartSegments(tourDataTour));
		chartDataModel.setXData(xData);

		/*
		 * Altitude
		 */
		final ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				tourDataTour.fAltitudeLow,
				tourDataTour.fAltitudeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
		yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setVisibleMinValue(0);
		yData.setCustomData(ALTITUDE_DATA, 1);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE);
		yData.setColorIndex(new int[][] { tourDataTour.fTypeColorIndex });
		chartDataModel.addYData(yData);

		/*
		 * set graph minimum width, these is the number of days in the year
		 */
		fCalendar.set(fCurrentYear, 11, 31);
		final int yearDays = fCalendar.get(Calendar.DAY_OF_YEAR);
		chartDataModel.setChartMinWidth(yearDays);

		setChartProviders(fChart, chartDataModel);

		if (fIsSynchScaleEnabled) {
			fMinMaxKeeper.setMinMaxValues(chartDataModel);
		}

		// show the data in the chart
		fChart.updateChart(chartDataModel, false);

		// try to select the previous selected tour
		selectTour(lastSelectedTourId);
	}
}
