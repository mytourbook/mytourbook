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
import java.util.Formatter;
import java.util.GregorianCalendar;

import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartSegments;
import net.tourbook.chart.ChartToolTipInfo;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourPerson;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public abstract class StatisticWeek extends YearStatistic {

	Chart					fChart;
	BarChartMinMaxKeeper	fMinMaxKeeper	= new BarChartMinMaxKeeper();

	private TourPerson		fActivePerson;
	TourTypeFilter			fActiveTourTypeFilter;

	int						fCurrentYear;
	int						fNumberOfYears;

	boolean					fIsSynchScaleEnabled;

	private final Calendar	fCalendar		= GregorianCalendar.getInstance();

	TourDataWeek			fTourWeekData;

	@Override
	public void activateActions(IWorkbenchPartSite partSite) {
		fChart.updateChartActionHandlers();
	}

	public boolean canTourBeVisible() {
		return false;
	}

	/**
	 * create segments for each week
	 */
	ChartSegments createChartSegments() {

		int yearWeeks = 53;
		int segmentStart[] = new int[fNumberOfYears];
		int segmentEnd[] = new int[fNumberOfYears];
		String[] segmentTitle = new String[fNumberOfYears];

		int weekCounter = fTourWeekData.fAltitudeHigh[0].length;

		int oldestYear = fCurrentYear - fNumberOfYears + 1;

		// get start/end and title for each segment
		for (int weekIndex = 0; weekIndex < weekCounter; weekIndex++) {

			int currentYearIndex = weekIndex / yearWeeks;

			if (weekIndex % yearWeeks == 0) {

				// first week in a year

				segmentStart[currentYearIndex] = weekIndex;
				segmentTitle[currentYearIndex] = Integer.toString(oldestYear + currentYearIndex);

			} else if (weekIndex % yearWeeks == yearWeeks - 1) {

				// last week in a year

				segmentEnd[currentYearIndex] = weekIndex;
			}
		}

		ChartSegments weekSegments = new ChartSegments();
		weekSegments.valueStart = segmentStart;
		weekSegments.valueEnd = segmentEnd;
		weekSegments.segmentTitle = segmentTitle;

		return weekSegments;
	}

	@Override
	public void createControl(Composite parent, IViewSite viewSite, final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		// create statistic chart
		fChart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		fChart.setShowZoomActions(true);
		fChart.setCanScrollZoomedChart(true);
		fChart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);
	}

	private ChartToolTipInfo createToolTipInfo(int serieIndex, int valueIndex) {

		int oldestYear = fCurrentYear - fNumberOfYears + 1;
		fCalendar.set(Calendar.YEAR, oldestYear);
		fCalendar.set(Calendar.WEEK_OF_YEAR, valueIndex + 1);

		/*
		 * tool tip: title
		 */
		StringBuilder titleString = new StringBuilder();
		titleString.append("%d / %d");

		final int calendarYear = fCalendar.get(Calendar.YEAR);

		final String toolTipTitle = new Formatter().format(titleString.toString(), //
				//
				fCalendar.get(Calendar.WEEK_OF_YEAR),
				calendarYear

		).toString();

		/*
		 * tool tip: label
		 */
		StringBuilder labelString = new StringBuilder();
		labelString.append("%d.%d.%d\n");
		labelString.append("Type:\t%s\n");

		final String toolTipLabel = new Formatter().format(labelString.toString(), //
				//
				fCalendar.get(Calendar.DAY_OF_MONTH),
				fCalendar.get(Calendar.MONTH) + 1,
				calendarYear,
				//
				getTourTypeName(serieIndex, fActiveTourTypeFilter)
		//
		)
				.toString();

		/*
		 * create tool tip info
		 */

		ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
		toolTipInfo.setTitle(toolTipTitle);
		toolTipInfo.setLabel(toolTipLabel);

		return toolTipInfo;
	}

	private int[] createWeekData() {

		int weekCounter = fTourWeekData.fAltitudeHigh[0].length;
		int allWeeks[] = new int[weekCounter];

		for (int weekIndex = 0; weekIndex < weekCounter; weekIndex++) {
			allWeeks[weekIndex] = weekIndex;
		}

		createDebug();

		return allWeeks;
	}

	private void createDebug() {

		Calendar calendar1 = GregorianCalendar.getInstance();
		Calendar calendar2 = GregorianCalendar.getInstance();
		Calendar calendar3 = GregorianCalendar.getInstance();

		final int firstYear = 1970;

		calendar2.set(firstYear, 0, 1);
		calendar3.set(firstYear, 11, 31);
//		calendar3.set(firstYear, 11, 26);

		for (int currentYear = firstYear; currentYear <= 2030; currentYear++) {

			calendar1.set(Calendar.YEAR, currentYear);
			calendar1.set(Calendar.WEEK_OF_YEAR, 1);

			calendar2.add(Calendar.MONTH, 12);
			calendar3.add(Calendar.MONTH, 12);

			printDayAndWeek(calendar1);
			System.out.print("\t");

			printDayAndWeek(calendar2);
			System.out.print("\t");

			printDayAndWeek(calendar3);

			System.out.println();

		}

		System.out.println();
	}

	private void printDayAndWeek(Calendar calendar2) {
		System.out.print(calendar2.get(Calendar.DAY_OF_MONTH)
				+ "."
				+ (calendar2.get(Calendar.MONTH) + 1)
				+ "."
				+ calendar2.get(Calendar.YEAR)
				+ " - "
				+ calendar2.get(Calendar.WEEK_OF_YEAR)
		//
		);
	}

	void createXDataWeek(ChartDataModel chartDataModel) {

		// set the x-axis
		ChartDataXSerie xData = new ChartDataXSerie(createWeekData());
		xData.setAxisUnit(ChartDataSerie.X_AXIS_UNIT_WEEK);
		xData.setChartSegments(createChartSegments());

		chartDataModel.setXData(xData);
	}

	void createYDataAltitude(ChartDataModel chartDataModel) {

		// altitude
		ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				fTourWeekData.fAltitudeLow,
				fTourWeekData.fAltitudeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
		yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setAllValueColors(0);
		yData.setVisibleMinValue(0);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE, fActiveTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, fTourWeekData.fTypeIds, fActiveTourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE);

		chartDataModel.addYData(yData);
	}

	void createYDataDistance(ChartDataModel chartDataModel) {

		// distance
		ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				fTourWeekData.fDistanceLow,
				fTourWeekData.fDistanceHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
		yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setAllValueColors(0);
		yData.setVisibleMinValue(0);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE, fActiveTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, fTourWeekData.fTypeIds, fActiveTourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE);

		chartDataModel.addYData(yData);
	}

	void createYDataDuration(ChartDataModel chartDataModel) {

		// duration
		ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				fTourWeekData.fTimeLow,
				fTourWeekData.fTimeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		yData.setAllValueColors(0);
		yData.setVisibleMinValue(0);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_TIME, fActiveTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, fTourWeekData.fTypeIds, fActiveTourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_TIME);

		chartDataModel.addYData(yData);
	}

	@Override
	public void deactivateActions(IWorkbenchPartSite partSite) {}

	public void prefColorChanged() {
		refreshStatistic(fActivePerson, fActiveTourTypeFilter, fCurrentYear, fNumberOfYears, false);
	}

	public void refreshStatistic(	TourPerson person,
									TourTypeFilter typeId,
									int currentYear,
									int numberOfYears,
									boolean refreshData) {

		fActivePerson = person;
		fActiveTourTypeFilter = typeId;
		fCurrentYear = currentYear;
		fNumberOfYears = numberOfYears;

		fTourWeekData = DataProviderTourWeek.getInstance().getWeekData(person,
				typeId,
				currentYear,
				numberOfYears,
				isDataDirtyWithReset() || refreshData);

		// reset min/max values
		if (fIsSynchScaleEnabled == false && refreshData) {
			fMinMaxKeeper.resetMinMax();
		}

		updateChart();
	}

	@Override
	public void resetSelection() {
		fChart.setSelectedBars(null);
	}

	@Override
	public boolean selectDay(Long date) {
		fCalendar.setTimeInMillis(date);
		int selectedWeek = fCalendar.get(Calendar.WEEK_OF_YEAR) - 0;

		boolean selectedItems[] = new boolean[53];
		selectedItems[selectedWeek] = true;

		fChart.setSelectedBars(selectedItems);

		return true;
	}

	@Override
	public boolean selectMonth(Long date) {

		fCalendar.setTimeInMillis(date);
		int selectedMonth = fCalendar.get(Calendar.MONTH);

		boolean selectedItems[] = new boolean[53];
		boolean isSelected = false;

		// select all weeks in the selected month
		for (int weekIndex = 0; weekIndex < selectedItems.length; weekIndex++) {
			fCalendar.set(Calendar.WEEK_OF_YEAR, weekIndex + 0);

			boolean isMonthSelected = fCalendar.get(Calendar.MONTH) == selectedMonth ? true : false;
			if (isMonthSelected) {
				isSelected = true;
			}
			selectedItems[weekIndex] = isMonthSelected;
		}

		if (isSelected) {
			fChart.setSelectedBars(selectedItems);
		}

		return isSelected;
	}

	void setChartProviders(final Chart chartWidget, final ChartDataModel chartModel) {

		// set tool tip info
		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, int valueIndex) {
				return createToolTipInfo(serieIndex, valueIndex);
			}
		});
	}

	@Override
	public void setSynchScale(boolean isSynchScaleEnabled) {
		fIsSynchScaleEnabled = isSynchScaleEnabled;
	}

	abstract void updateChart();

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		fChart.fillToolbar(refreshToolbar);
	}
}
