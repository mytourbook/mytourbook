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
package net.tourbook.statistics;

import java.util.Calendar;

import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.colors.GraphColors;

public class StatisticDayAltitude extends StatisticDay {

	void updateChart(final TourDataTour tourTimeData) {

		final ChartDataModel chartModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(tourTimeData.fDOYValues);
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_YEAR);
		xData.setVisibleMaxValue(fCurrentYear);
		chartModel.setXData(xData);

		/*
		 * Altitude
		 */
		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_BAR,
				tourTimeData.fAltitudeLow,
				tourTimeData.fAltitudeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
		yData.setUnitLabel(Messages.LABEL_GRAPH_ALTITUDE_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setVisibleMinValue(0);
		yData.setCustomData(ALTITUDE_DATA, 1);
		StatisticServices.setTourTypeColors(yData, GraphColors.PREF_GRAPH_ALTITUDE);
		yData.setColorIndex(new int[][] { tourTimeData.fTypeColorIndex });
		chartModel.addYData(yData);

		/*
		 * set graph minimum width, these is the number of days in the year
		 */
		fCalendar.set(fCurrentYear, 11, 31);
		final int yearDays = fCalendar.get(Calendar.DAY_OF_YEAR);
		chartModel.setChartMinWidth(yearDays);

		ProviderTourDay.getInstance().setChartProviders(
				fChart,
				chartModel
//				,
//				fTourChartViewer
				);

		if (fIsSynchScaleEnabled) {
			fMinMaxKeeper.setMinMaxValues(chartModel);
		}

		// show the data in the chart
		fChart.setChartDataModel(chartModel);
	}
}
