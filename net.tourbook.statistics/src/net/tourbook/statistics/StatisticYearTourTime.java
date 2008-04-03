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

import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.ui.TourTypeFilter;

public class StatisticYearTourTime extends StatisticYear {

	@Override
	void updateChart(final TourDataYear tourDataYear, TourTypeFilter tourTypeFilter) {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(createYearData(tourDataYear));
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_YEAR);
		xData.setChartSegments(createChartSegments(tourDataYear));
		chartDataModel.setXData(xData);

		// duration
		ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_BESIDE,
				tourDataYear.fTimeLow,
				tourDataYear.fTimeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		chartDataModel.addYData(yData);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_TIME, tourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, tourDataYear.fTypeIds, tourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_TIME);

		if (fIsSynchScaleEnabled) {
			fMinMaxKeeper.setMinMaxValues(chartDataModel);
		}

		// show the fDataModel in the chart
		fChart.updateChart(chartDataModel);
	}
}
