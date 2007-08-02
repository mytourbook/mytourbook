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
import java.util.GregorianCalendar;

import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.colors.GraphColors;
import net.tourbook.data.TourPerson;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;

public class StatisticTourTime extends YearStatistic {

	private TourPerson					fActivePerson;
	private long						fActiveTypeId;
	private int							fCurrentYear;

	private final Calendar				fCalendar		= GregorianCalendar.getInstance();

	private Chart						fChart;
	private TourDataTime				fTourTimeData;

	private final BarChartMinMaxKeeper	fMinMaxKeeper	= new BarChartMinMaxKeeper();
	private boolean						fIsSynchScaleEnabled;

	private IPostSelectionProvider		fPostSelectionProvider;

	public boolean canTourBeVisible() {
		return true;
	}

	public void createControl(	final Composite parent,
								final IActionBars actionBars,
								final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		fPostSelectionProvider = postSelectionProvider;

		// chart widget page
		fChart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		fChart.setActionBars(actionBars);
		fChart.setShowPartNavigation(true);
		fChart.setShowZoomActions(true);
		fChart.setCanScrollZoomedChart(true);

		
		fChart.addBarSelectionListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, final int valueIndex) {
				long tourId = fTourTimeData.fTourIds[valueIndex];
				fPostSelectionProvider.setSelection(new SelectionTourId(tourId));
			}
		});

		fChart.addDoubleClickListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, final int valueIndex) {
				long tourId = fTourTimeData.fTourIds[valueIndex];
				TourManager.getInstance().openTourInEditor(tourId);
			}
		});
	}

	public void prefColorChanged() {
		refreshStatistic(fActivePerson, fActiveTypeId, fCurrentYear, false);
	}

	public boolean selectMonth(final Long date) {

		fCalendar.setTimeInMillis(date);
		final int tourMonth = fCalendar.get(Calendar.MONTH);
		final int[] tourMonths = fTourTimeData.fTourMonthValues;

		final boolean selectedItems[] = new boolean[tourMonths.length];
		boolean isSelected = false;

		// find the tours which have the same day as the selected day
		for (int tourIndex = 0; tourIndex < tourMonths.length; tourIndex++) {
			final boolean isMonthSelected = tourMonths[tourIndex] == tourMonth ? true : false;
			if (isMonthSelected) {
				isSelected = true;
			}
			selectedItems[tourIndex] = isMonthSelected;
		}

		if (isSelected) {
			fChart.setSelectedBars(selectedItems);
		}

		return isSelected;
	}

	public boolean selectTour(final long tourId) {

		final long[] tourIds = fTourTimeData.fTourIds;
		final boolean selectedTours[] = new boolean[tourIds.length];

		boolean isSelected = false;

		// find the tour which has the same tourId as the selected tour
		for (int tourIndex = 0; tourIndex < tourIds.length; tourIndex++) {
			if ((tourIds[tourIndex] == tourId)) {
				selectedTours[tourIndex] = true;
				isSelected = true;
			}
		}

		if (isSelected) {
			fChart.setSelectedBars(selectedTours);
		}

		return isSelected;
	}

	public void resetSelection() {
		fChart.setSelectedBars(null);
	}

	public void refreshStatistic(	final TourPerson person,
									final long type,
									final int year,
									final boolean refreshData) {

		// reset the selection in the chart when the data have changed
		final boolean isResetSelection = fActivePerson != person
				|| fActiveTypeId != type
				|| fCurrentYear != year;

		fActivePerson = person;
		fActiveTypeId = type;
		fCurrentYear = year;

		fTourTimeData = ProviderTourTime.getInstance().getTourTimeData(person,
				type,
				year,
				isRefreshDataWithReset() || refreshData);

		// reset min/max values
		if (fIsSynchScaleEnabled == false && refreshData) {
			fMinMaxKeeper.resetMinMax();
		}

		updateChart(isResetSelection);
	}

	private void updateChart(final boolean isResetSelection) {

		final ChartDataModel chartModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		final ChartDataXSerie xData = new ChartDataXSerie(fTourTimeData.fTourDOYValues);

		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_YEAR);
		xData.setVisibleMaxValue(fCurrentYear);
		chartModel.setXData(xData);

		// set the bar low/high data
		final ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				new int[][] { fTourTimeData.fTourTimeStartValues },
				new int[][] { fTourTimeData.fTourTimeEndValues });
		yData.setYTitle(Messages.LABEL_GRAPH_DAYTIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_HOUR_MINUTE_24H);
		yData.setYAxisDirection(false);

		yData.setColorIndex(new int[][] { fTourTimeData.fTypeColorIndex });
		StatisticServices.setTourTypeColors(yData, GraphColors.PREF_GRAPH_TIME);

		chartModel.addYData(yData);

		/*
		 * set graph minimum width, this is the number of days in the year
		 */
		fCalendar.set(fCurrentYear, 11, 31);
		final int yearDays = fCalendar.get(Calendar.DAY_OF_YEAR);
		chartModel.setChartMinWidth(yearDays);

		ProviderTourTime.getInstance().setChartProviders(fChart, chartModel);

		if (fIsSynchScaleEnabled) {
			fMinMaxKeeper.setMinMaxValues(chartModel);
		}

		// show the data in the chart
		fChart.setChartDataModel(chartModel, isResetSelection);
	}

	public void updateToolBar(final boolean refreshToolbar) {
		fChart.showActions(refreshToolbar);
	}

	public void setSynchScale(final boolean isSynchScaleEnabled) {
		fIsSynchScaleEnabled = isSynchScaleEnabled;
	}

}
