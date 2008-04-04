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
import net.tourbook.chart.ChartSegments;
import net.tourbook.data.TourPerson;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public abstract class StatisticYear extends YearStatistic {

	private TourPerson			fActivePerson;
	private TourTypeFilter		fActiveTourType;

	int							fCurrentYear;
	int							fNumberOfYears;

	Chart						fChart;

	final BarChartMinMaxKeeper	fMinMaxKeeper	= new BarChartMinMaxKeeper();

	boolean						fIsSynchScaleEnabled;

	private final Calendar		fCalendar		= GregorianCalendar.getInstance();

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

	ChartSegments createChartSegments(TourDataYear tourDataYear) {

		final int yearCounter = tourDataYear.fAltitudeHigh[0].length;

		final int segmentStart[] = new int[fNumberOfYears];
		final int segmentEnd[] = new int[fNumberOfYears];
		final String[] segmentTitle = new String[fNumberOfYears];

		final int oldestYear = fCurrentYear - fNumberOfYears + 1;

		for (int yearIndex = 0; yearIndex < yearCounter; yearIndex++) {

			segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

			segmentStart[yearIndex] = yearIndex;
			segmentEnd[yearIndex] = yearIndex;
		}

		ChartSegments yearSegments = new ChartSegments();
		yearSegments.valueStart = segmentStart;
		yearSegments.valueEnd = segmentEnd;
		yearSegments.segmentTitle = segmentTitle;
		yearSegments.years = tourDataYear.years;

		return yearSegments;
	}

	int[] createYearData(TourDataYear tourDataYear) {

		final int yearCounter = tourDataYear.fAltitudeHigh[0].length;
		final int allYears[] = new int[yearCounter];

		for (int yearIndex = 0; yearIndex < yearCounter; yearIndex++) {
			allYears[yearIndex] = yearIndex;
		}

		return allYears;
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

		final TourDataYear tourDataYear = DataProviderTourYear.getInstance().getYearData(person,
				tourTypeFilter,
				currentYear,
				numberOfYears,
				isDataDirtyWithReset() || refreshData);

		// reset min/max values
		if (fIsSynchScaleEnabled == false && refreshData) {
			fMinMaxKeeper.resetMinMax();
		}

		updateChart(tourDataYear, tourTypeFilter);
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

	abstract void updateChart(final TourDataYear tourDataYear, TourTypeFilter tourTypeFilter);

//	private void updateChart(final TourDataYear tourDataYear, TourTypeFilter tourTypeFilter) {
//
//		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);
//
//		/*
//		 * create year data serie
//		 */
//		final int yearCounter = tourDataYear.fAltitudeHigh[0].length;
//
//		final int allYears[] = new int[yearCounter];
//		final int segmentStart[] = new int[fNumberOfYears];
//		final int segmentEnd[] = new int[fNumberOfYears];
//		final String[] segmentTitle = new String[fNumberOfYears];
//
//		final int oldestYear = fCurrentYear - fNumberOfYears + 1;
//
//		for (int yearIndex = 0; yearIndex < yearCounter; yearIndex++) {
//
//			allYears[yearIndex] = yearIndex;
//			segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);
//
//			segmentStart[yearIndex] = yearIndex;
//			segmentEnd[yearIndex] = yearIndex;
//		}
//
//		ChartSegments yearSegments = new ChartSegments();
//		yearSegments.valueStart = segmentStart;
//		yearSegments.valueEnd = segmentEnd;
//		yearSegments.segmentTitle = segmentTitle;
//		yearSegments.years = tourDataYear.years;
//
//		// set the x-axis
//		final ChartDataXSerie xData = new ChartDataXSerie(allYears);
//		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_YEAR);
//		xData.setChartSegments(yearSegments);
//		chartDataModel.setXData(xData);
//
//		// distance
//		ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
//				ChartDataYSerie.BAR_LAYOUT_BESIDE,
//				tourDataYear.fDistanceLow,
//				tourDataYear.fDistanceHigh);
//		yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
//		yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
//		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
//		chartDataModel.addYData(yData);
//		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE, tourTypeFilter);
//		StatisticServices.setTourTypeColorIndex(yData, tourDataYear.fTypeIds, tourTypeFilter);
//		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE);
//
//		// altitude
//		yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
//				ChartDataYSerie.BAR_LAYOUT_BESIDE,
//				tourDataYear.fAltitudeLow,
//				tourDataYear.fAltitudeHigh);
//		yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
//		yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
//		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
//		chartDataModel.addYData(yData);
//		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE, tourTypeFilter);
//		StatisticServices.setTourTypeColorIndex(yData, tourDataYear.fTypeIds, tourTypeFilter);
//		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE);
//
//		// duration
//		yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
//				ChartDataYSerie.BAR_LAYOUT_BESIDE,
//				tourDataYear.fTimeLow,
//				tourDataYear.fTimeHigh);
//		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
//		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
//		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
//		chartDataModel.addYData(yData);
//		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_TIME, tourTypeFilter);
//		StatisticServices.setTourTypeColorIndex(yData, tourDataYear.fTypeIds, tourTypeFilter);
//		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_TIME);
//
//		if (fIsSynchScaleEnabled) {
//			fMinMaxKeeper.setMinMaxValues(chartDataModel);
//		}
//
//		// show the fDataModel in the chart
//		fChart.updateChart(chartDataModel);
//	}

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		fChart.fillToolbar(refreshToolbar);
	}
}
