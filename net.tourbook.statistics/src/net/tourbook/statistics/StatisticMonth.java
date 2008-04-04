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
import java.util.GregorianCalendar;

import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartSegments;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourPerson;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public abstract class StatisticMonth extends YearStatistic {

	private TourPerson			fActivePerson;
	TourTypeFilter				fActiveTourType;

	private int					fCurrentYear;
	private int					fNumberOfYears;

	Chart						fChart;
	final BarChartMinMaxKeeper	fMinMaxKeeper	= new BarChartMinMaxKeeper();

	boolean						fIsSynchScaleEnabled;
	private final Calendar		fCalendar		= GregorianCalendar.getInstance();

	@Override
	public void activateActions(final IWorkbenchPartSite partSite) {
		fChart.updateChartActionHandlers();
	}

	public boolean canTourBeVisible() {
		return false;
	}

	ChartSegments createChartSegments(TourDataMonth tourMonthData) {

		/*
		 * create segments for each year
		 */
		final int monthCounter = tourMonthData.fAltitudeHigh[0].length;
		final int segmentStart[] = new int[fNumberOfYears];
		final int segmentEnd[] = new int[fNumberOfYears];
		final String[] segmentTitle = new String[fNumberOfYears];

		final int oldestYear = fCurrentYear - fNumberOfYears + 1;

		// get start/end and title for each segment
		for (int monthIndex = 0; monthIndex < monthCounter; monthIndex++) {

			int yearIndex = monthIndex / 12;

			if (monthIndex % 12 == 0) {

				// first month in a year
				segmentStart[yearIndex] = monthIndex;
				segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

			} else if (monthIndex % 12 == 11) {

				// last month in a year
				segmentEnd[yearIndex] = monthIndex;
			}
		}

		ChartSegments monthSegments = new ChartSegments();
		monthSegments.valueStart = segmentStart;
		monthSegments.valueEnd = segmentEnd;
		monthSegments.segmentTitle = segmentTitle;

		return monthSegments;
	}

	@Override
	public void createControl(	final Composite parent,
								final IViewSite viewSite,
								final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		// create chart
		fChart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		fChart.setShowZoomActions(true);
		fChart.setCanScrollZoomedChart(true);
		fChart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);
	}

	int[] createMonthData(TourDataMonth tourMonthData) {

		/*
		 * create segments for each year
		 */
		final int monthCounter = tourMonthData.fAltitudeHigh[0].length;
		final int allMonths[] = new int[monthCounter];

		// get start/end and title for each segment
		for (int monthIndex = 0; monthIndex < monthCounter; monthIndex++) {
			allMonths[monthIndex] = monthIndex;
		}

		return allMonths;
	}

	void createXDataMonths(final TourDataMonth tourMonthData, final ChartDataModel chartDataModel) {
		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(createMonthData(tourMonthData));
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_MONTH);
		xData.setChartSegments(createChartSegments(tourMonthData));
		chartDataModel.setXData(xData);
	}

	void createYDataAltitude(final TourDataMonth tourMonthData, final ChartDataModel chartDataModel) {
		ChartDataYSerie yData;
		// altitude
		yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				tourMonthData.fAltitudeLow,
				tourMonthData.fAltitudeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
		yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		chartDataModel.addYData(yData);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE, fActiveTourType);
		StatisticServices.setTourTypeColorIndex(yData, tourMonthData.fTypeIds, fActiveTourType);
	}

	void createYDataDistance(final TourDataMonth tourMonthData, final ChartDataModel chartDataModel) {
		// distance
		ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				tourMonthData.fDistanceLow,
				tourMonthData.fDistanceHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
		yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		chartDataModel.addYData(yData);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE, fActiveTourType);
		StatisticServices.setTourTypeColorIndex(yData, tourMonthData.fTypeIds, fActiveTourType);
	}

	void createYDataTourTime(final TourDataMonth tourMonthData, final ChartDataModel chartDataModel) {
		ChartDataYSerie yData;
		// duration
		yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				tourMonthData.fTimeLow,
				tourMonthData.fTimeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		chartDataModel.addYData(yData);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_TIME);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_TIME, fActiveTourType);
		StatisticServices.setTourTypeColorIndex(yData, tourMonthData.fTypeIds, fActiveTourType);
	}

	@Override
	public void deactivateActions(final IWorkbenchPartSite partSite) {}

	public void prefColorChanged() {
		refreshStatistic(fActivePerson, fActiveTourType, fCurrentYear, fNumberOfYears, false);
	}

	public void refreshStatistic(	final TourPerson person,
									final TourTypeFilter tourTypeFilter,
									final int currentYear,
									final int numberOfYears,
									final boolean refreshData) {

		fActivePerson = person;
		fActiveTourType = tourTypeFilter;
		fCurrentYear = currentYear;
		fNumberOfYears = numberOfYears;

		final TourDataMonth tourMonthData = DataProviderTourMonth.getInstance().getMonthData(person,
				tourTypeFilter,
				currentYear,
				numberOfYears,
				isDataDirtyWithReset() || refreshData);

		// reset min/max values
		if (fIsSynchScaleEnabled == false && refreshData) {
			fMinMaxKeeper.resetMinMax();
		}

		updateChart(tourMonthData);
	}

	@Override
	public void resetSelection() {
		fChart.setSelectedBars(null);
	}

	@Override
	public boolean selectMonth(final Long date) {

		fCalendar.setTimeInMillis(date);
		final int selectedMonth = fCalendar.get(Calendar.MONTH);

		final boolean selectedItems[] = new boolean[12];
		selectedItems[selectedMonth] = true;

		fChart.setSelectedBars(selectedItems);

		return true;
	}

	@Override
	public void setSynchScale(final boolean isSynchScaleEnabled) {
		fIsSynchScaleEnabled = isSynchScaleEnabled;
	}

	abstract void updateChart(final TourDataMonth tourMonthData);

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		fChart.fillToolbar(refreshToolbar);
	}
}
