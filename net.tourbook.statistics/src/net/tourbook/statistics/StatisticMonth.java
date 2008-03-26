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

public class StatisticMonth extends YearStatistic {

	private TourPerson					fActivePerson;
	private int							fCurrentYear;
	private int							fNumberOfYears;

	private Chart						fChart;
	private final BarChartMinMaxKeeper	fMinMaxKeeper	= new BarChartMinMaxKeeper();
	private TourTypeFilter				fActiveTourType;

	private final Calendar				fCalendar		= GregorianCalendar.getInstance();
	private boolean						fIsSynchScaleEnabled;

	@Override
	public void activateActions(final IWorkbenchPartSite partSite) {
		fChart.updateChartActionHandlers();
	}

	@Override
	public void deactivateActions(final IWorkbenchPartSite partSite) {}

	public boolean canTourBeVisible() {
		return false;
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

		final TourDataMonth tourMonthData = ProviderTourMonth.getInstance().getMonthData(person,
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

	private void updateChart(final TourDataMonth tourMonthData) {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		/*
		 * create month array
		 */
//		int monthCounter = tourMonthData.fAltitudeHigh[0].length;
//		int monthValues[] = new int[monthCounter];
//		for (int monthIndex = 0; monthIndex < monthCounter; monthIndex++) {
//			monthValues[monthIndex] = monthIndex;
//		}
		/*
		 * create segments for each year
		 */
		final int monthCounter = tourMonthData.fAltitudeHigh[0].length;
		final int allMonths[] = new int[monthCounter];
		final int segmentStart[] = new int[fNumberOfYears];
		final int segmentEnd[] = new int[fNumberOfYears];
		final String[] segmentTitle = new String[fNumberOfYears];

		final int oldestYear = fCurrentYear - fNumberOfYears + 1;

		// get start/end and title for each segment
		for (int monthIndex = 0; monthIndex < monthCounter; monthIndex++) {

			allMonths[monthIndex] = monthIndex;

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

		/*
		 * chart title
		 */
//		String title = "";
//		if (fNumberOfYears > 1) {
//			title = Integer.toString(fCurrentYear - fNumberOfYears + 1) + "-" + Integer.toString(fCurrentYear);
//		} else {
//			// one year
//			title = Integer.toString(fCurrentYear);
//		}
//		chartDataModel.setTitle(title);
		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(allMonths);
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_MONTH);
		xData.setSegmentMarker(monthSegments);
		chartDataModel.setXData(xData);

		// distance
		ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				tourMonthData.fDistanceLow,
				tourMonthData.fDistanceHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
		yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		chartDataModel.addYData(yData);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE);
		StatisticServices.setTourTypeColorIndex(yData, tourMonthData.fTypeIds);

		// altitude
		yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				tourMonthData.fAltitudeLow,
				tourMonthData.fAltitudeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
		yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		chartDataModel.addYData(yData);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE);
		StatisticServices.setTourTypeColorIndex(yData, tourMonthData.fTypeIds);

		// duration
		yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				tourMonthData.fTimeLow,
				tourMonthData.fTimeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		chartDataModel.addYData(yData);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_TIME);
		StatisticServices.setTourTypeColorIndex(yData, tourMonthData.fTypeIds);

		if (fIsSynchScaleEnabled) {
			fMinMaxKeeper.setMinMaxValues(chartDataModel);
		}

		// show the fDataModel in the chart
		fChart.updateChart(chartDataModel);
	}

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		fChart.fillToolbar(refreshToolbar);
	}
}
