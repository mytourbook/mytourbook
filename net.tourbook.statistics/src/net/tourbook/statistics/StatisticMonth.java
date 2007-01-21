/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
import java.util.GregorianCalendar;

import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.colors.GraphColors;
import net.tourbook.data.TourPerson;
import net.tourbook.ui.ITourChartViewer;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class StatisticMonth extends YearStatistic {

	private TourPerson				fActivePerson;
	private int						fCurrentYear;

	private Chart					fChart;
	private BarChartMinMaxKeeper	fMinMaxKeeper	= new BarChartMinMaxKeeper();
	private long					fActiveTypeId;

	private Calendar				fCalendar		= GregorianCalendar.getInstance();
	private boolean					fIsSynchScaleEnabled;

	public boolean canTourBeVisible() {
		return false;
	}

	public void createControl(	Composite parent,
								ITourChartViewer tourChartViewer,
								ToolBarManager tbm) {

		super.createControl(parent);

		// create chart
		fChart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		fChart.setToolBarManager(tbm);
		fChart.setShowZoomActions(true);
		fChart.setCanScrollZoomedChart(true);
	}
	
	public void prefColorChanged() {
		refreshStatistic(fActivePerson, fActiveTypeId, fCurrentYear, false);
	}
	

	public void refreshStatistic(TourPerson person, long typeId, int year, boolean refreshData) {

		fActivePerson = person;
		fActiveTypeId = typeId;
		fCurrentYear = year;

		TourDataMonth tourMonthData = ProviderTourMonth.getInstance().getMonthData(
				person,
				typeId,
				year,
				isRefreshDataWithReset() || refreshData);

		// reset min/max values
		if (fIsSynchScaleEnabled == false && refreshData) {
			fMinMaxKeeper.resetMinMax();
		}

		updateChart(tourMonthData);
	}

	public void resetSelection() {
		fChart.setSelectedBars(null);
	}

	public boolean selectMonth(Long date) {

		fCalendar.setTimeInMillis(date);
		int selectedMonth = fCalendar.get(Calendar.MONTH);

		boolean selectedItems[] = new boolean[12];
		selectedItems[selectedMonth] = true;

		fChart.setSelectedBars(selectedItems);
		
		return true;
	}

	public void setSynchScale(boolean isSynchScaleEnabled) {
		fIsSynchScaleEnabled = isSynchScaleEnabled;
	}
	
	private void updateChart(TourDataMonth tourMonthData) {

		ChartDataModel chartModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		// set the x-axis
		ChartDataXSerie xData = new ChartDataXSerie(ProviderTourMonth.fAllMonths);
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_MONTH);
		chartModel.setXData(xData);

		// distance
		ChartDataYSerie yData = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				tourMonthData.fDistanceLow,
				tourMonthData.fDistanceHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
		yData.setUnitLabel(Messages.LABEL_GRAPH_DISTANCE_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		chartModel.addYData(yData);
		StatisticServices.setTourTypeColors(yData, GraphColors.PREF_GRAPH_DISTANCE);
		StatisticServices.setTourTypeColorIndex(yData, tourMonthData.fTypeIds);

		// altitude
		yData = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				tourMonthData.fAltitudeLow,
				tourMonthData.fAltitudeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
		yData.setUnitLabel(Messages.LABEL_GRAPH_ALTITUDE_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		chartModel.addYData(yData);
		StatisticServices.setTourTypeColors(yData, GraphColors.PREF_GRAPH_ALTITUDE);
		StatisticServices.setTourTypeColorIndex(yData, tourMonthData.fTypeIds);

		// duration
		yData = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				tourMonthData.fTimeLow,
				tourMonthData.fTimeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		chartModel.addYData(yData);
		StatisticServices.setTourTypeColors(yData, GraphColors.PREF_GRAPH_TIME);
		StatisticServices.setTourTypeColorIndex(yData, tourMonthData.fTypeIds);

		if (fIsSynchScaleEnabled) {
			fMinMaxKeeper.setMinMaxValues(chartModel);
		}

		// show the fDataModel in the chart
		fChart.setChartDataModel(chartModel);
	}

	public void updateToolBar(boolean refreshToolbar) {
		fChart.showActions(refreshToolbar);
	}
}
