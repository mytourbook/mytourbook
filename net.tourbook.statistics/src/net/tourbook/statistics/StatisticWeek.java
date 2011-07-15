/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;

import net.tourbook.application.TourbookPlugin;
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
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.StatisticContext;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;
import net.tourbook.util.Util;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public abstract class StatisticWeek extends YearStatistic {

	private Chart						_chart;
	private final BarChartMinMaxKeeper	_minMaxKeeper	= new BarChartMinMaxKeeper();

	private TourPerson					_activePerson;
	private TourTypeFilter				_activeTourTypeFilter;

	private int							_currentYear;
	private int							_numberOfYears;

	private boolean						_isSynchScaleEnabled;

	private final DateFormat			_dateFormatter	= DateFormat.getDateInstance(DateFormat.FULL);

	private TourDataWeek				_tourWeekData;

	private Calendar					_calendar		= GregorianCalendar.getInstance();
	private int							_firstDayOfWeek;
	private int							_minimalDaysInFirstWeek;

	private static IPreferenceStore		_prefStore		= TourbookPlugin.getDefault().getPreferenceStore();

	@Override
	public void activateActions(final IWorkbenchPartSite partSite) {
		_chart.updateChartActionHandlers();
	}

	public boolean canTourBeVisible() {
		return false;
	}

	abstract ChartDataModel createChartDataModel();

	/**
	 * create segments for each week
	 */
	ChartSegments createChartSegments() {

		final int segmentStart[] = new int[_numberOfYears];
		final int segmentEnd[] = new int[_numberOfYears];
		final String[] segmentTitle = new String[_numberOfYears];

		final int oldestYear = _currentYear - _numberOfYears + 1;
		final int[] yearWeeks = _tourWeekData.yearWeeks;

		int weekCounter = 0;
		int yearIndex = 0;

		// get start/end and title for each segment
		for (final int weeks : yearWeeks) {

			segmentStart[yearIndex] = weekCounter;
			segmentEnd[yearIndex] = weekCounter + weeks - 1;

			segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

			weekCounter += weeks;
			yearIndex++;
		}

		final ChartSegments weekSegments = new ChartSegments();
		weekSegments.valueStart = segmentStart;
		weekSegments.valueEnd = segmentEnd;
		weekSegments.segmentTitle = segmentTitle;

		weekSegments.years = _tourWeekData.years;
		weekSegments.yearWeeks = yearWeeks;
		weekSegments.yearDays = _tourWeekData.yearDays;

		return weekSegments;
	}

	@Override
	public void createControl(	final Composite parent,
								final IViewSite viewSite,
								final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		// create statistic chart
		_chart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		_chart.setShowZoomActions(true);
		_chart.setCanScrollZoomedChart(true);
		_chart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);
	}

	private ChartToolTipInfo createToolTipInfo(final int serieIndex, final int valueIndex) {

		final int oldestYear = _currentYear - _numberOfYears + 1;

		_calendar.set(oldestYear, 0, 1);

		/*
		 * adjust calendar to the first day in the first week, it took a while to figure this out
		 */
		int weekOfYear = (short) _calendar.get(Calendar.WEEK_OF_YEAR);
		final int dayOfWeek = _calendar.get(Calendar.DAY_OF_WEEK);
		int dayOffset;
		if (weekOfYear == 1) {

			// week 1

			dayOffset = _firstDayOfWeek - dayOfWeek;

		} else {

			// week 52/53

			dayOffset = _firstDayOfWeek - dayOfWeek;
			if (dayOffset < 0) {
				dayOffset += 7;
			}
		}
		final int dayOffsetAll = (valueIndex * 7) + dayOffset;
		_calendar.add(Calendar.DAY_OF_YEAR, dayOffsetAll);

		// validate calendar week
		final int checkDayOfWeek = _calendar.get(Calendar.DAY_OF_WEEK);
		if (_firstDayOfWeek != checkDayOfWeek) {
			System.out.println("first day in first week is incorrect\t_firstDayOfWeek=" //$NON-NLS-1$
					+ _firstDayOfWeek
					+ " != " //$NON-NLS-1$
					+ checkDayOfWeek);
		}

		weekOfYear = (short) _calendar.get(Calendar.WEEK_OF_YEAR);
		final short weekYear = (short) Util.getYearForWeek(_calendar);

		final Date dateStart = _calendar.getTime();

		_calendar.add(Calendar.DAY_OF_YEAR, 6);
		final Date dateEnd = _calendar.getTime();

		final String beginDate = _dateFormatter.format(dateStart);
		final String endDate = _dateFormatter.format(dateEnd);

		final Integer recordingTime = _tourWeekData.recordingTime[serieIndex][valueIndex];
		final Integer drivingTime = _tourWeekData.drivingTime[serieIndex][valueIndex];
		final int breakTime = recordingTime - drivingTime;

		final String tourTypeName = getTourTypeName(serieIndex, _activeTourTypeFilter);

		/*
		 * tool tip: title
		 */
		final StringBuilder titleFormat = new StringBuilder();
		titleFormat.append(Messages.tourtime_info_week);
		titleFormat.append(UI.NEW_LINE);

		final String toolTipTitle = new Formatter().format(titleFormat.toString(),//
				tourTypeName,
				weekOfYear,
				weekYear
		//
				)
				.toString();

		/*
		 * // tool tip: label
		 */
		final StringBuilder toolTipFormat = new StringBuilder();
		toolTipFormat.append(Messages.tourtime_info_date_week);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_distance_tour);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_altitude);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_recording_time);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_driving_time);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_break_time);

		final String toolTipLabel = new Formatter().format(toolTipFormat.toString(), //
				//
				beginDate,
				endDate,
				//
				(float) _tourWeekData.distanceHigh[serieIndex][valueIndex] / 1000,
				UI.UNIT_LABEL_DISTANCE,
				//
				_tourWeekData.altitudeHigh[serieIndex][valueIndex],
				UI.UNIT_LABEL_ALTITUDE,
				//
				recordingTime / 3600,
				(recordingTime % 3600) / 60,
				//
				drivingTime / 3600,
				(drivingTime % 3600) / 60,
				//
				breakTime / 3600,
				(breakTime % 3600) / 60
		//
				)
				.toString();

		/*
		 * create tool tip info
		 */

		final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
		toolTipInfo.setTitle(toolTipTitle);
		toolTipInfo.setLabel(toolTipLabel);

		return toolTipInfo;
	}

//		System.out.println(UI.EMPTY_STRING//
//				+ ("_firstDayOfWeek=" + _firstDayOfWeek + "\t")//
//				+ ("dayOffset=" + dayOffset + "\t")//
//				+ ("dayOffsetAll=" + dayOffsetAll + "\t")//
//				+ ("DAY_OF_WEEK=" + _calendar.get(Calendar.DAY_OF_WEEK) + "\t")//
//				+ ("date="
//						+ _calendar.get(Calendar.DAY_OF_MONTH)
//						+ "."
//						+ (_calendar.get(Calendar.MONTH) + 1)
//						+ "."
//						+ _calendar.get(Calendar.YEAR) + "\t")//
//				+ ("DAY_OF_MONTH=" + _calendar.get(Calendar.DAY_OF_MONTH) + "\t")//
//				+ ("WEEK_OF_YEAR=" + _calendar.get(Calendar.WEEK_OF_YEAR) + "\t")//
//				+ ("WEEK_OF_MONTH=" + _calendar.get(Calendar.WEEK_OF_MONTH) + "\t")//
//				+ ("DAY_OF_WEEK_IN_MONTH=" + _calendar.get(Calendar.DAY_OF_WEEK_IN_MONTH) + "\t")//
//		//
//				);

//	private void debugWeekNumber() {
//
//		final int firstYear = 2000;
//
//		DateTime dt = (new DateTime()).withYear(firstYear)
//				.withWeekOfWeekyear(1)
//				.withDayOfWeek(DateTimeConstants.MONDAY);
//
//		Calendar calendar = GregorianCalendar.getInstance();
////		calendar.setFirstDayOfWeek(4);
//
//		for (int currentYear = firstYear; currentYear <= 2010; currentYear++) {
//
////			dt = dt.withYear(currentYear).withWeekOfWeekyear(1).withDayOfWeek(DateTimeConstants.MONDAY);
//			dt = dt.withYear(currentYear).withMonthOfYear(1).withDayOfYear(1);
//
//			calendar.set(currentYear, 0, 1);
//
//			printDayAndWeek(currentYear, dt, calendar);
//
//		}
//
//		System.out.println();
//	}
//
//	private void printDayAndWeek(int currentYear, DateTime dt, Calendar calendar) {
//
//		System.out.print(//
////				currentYear
////				+ ": "
//		+dt.getDayOfMonth() //
//				+ "."
//				+ dt.getMonthOfYear()
//				+ "."
//				+ dt.getYear()
//				+ "-"
//				+ dt.getWeekOfWeekyear()
//				+ "-"
//				+ dt.weekOfWeekyear().getMaximumValue()
//				+ "\t"
//		//
//		);
//
//		System.out.println(calendar.get(Calendar.DAY_OF_MONTH)
//				+ "."
//				+ (calendar.get(Calendar.MONTH) + 1)
//				+ "."
//				+ calendar.get(Calendar.YEAR)
//				+ " - "
//				+ calendar.get(Calendar.WEEK_OF_YEAR)
//				+ " - "
//				+ calendar.getActualMaximum(Calendar.WEEK_OF_YEAR)
//		//
//		);
//	}

	private int[] createWeekData() {

		final int weekCounter = _tourWeekData.altitudeHigh[0].length;
		final int allWeeks[] = new int[weekCounter];

		for (int weekIndex = 0; weekIndex < weekCounter; weekIndex++) {
			allWeeks[weekIndex] = weekIndex;
		}

//		debugWeekNumber();

		return allWeeks;
	}

	void createXDataWeek(final ChartDataModel chartDataModel) {

		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(createWeekData());
		xData.setAxisUnit(ChartDataSerie.X_AXIS_UNIT_WEEK);
		xData.setChartSegments(createChartSegments());

		chartDataModel.setXData(xData);
	}

	void createYDataAltitude(final ChartDataModel chartDataModel) {

		// altitude
		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				_tourWeekData.altitudeLow,
				_tourWeekData.altitudeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
		yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setAllValueColors(0);
		yData.setVisibleMinValue(0);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE, _activeTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, _tourWeekData.typeIds, _activeTourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE);

		chartDataModel.addYData(yData);
	}

	void createYDataDistance(final ChartDataModel chartDataModel) {

		// distance
		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				_tourWeekData.distanceLow,
				_tourWeekData.distanceHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
		yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setAllValueColors(0);
		yData.setValueDivisor(1000);
		yData.setVisibleMinValue(0);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE, _activeTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, _tourWeekData.typeIds, _activeTourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE);

		chartDataModel.addYData(yData);
	}

	void createYDataDuration(final ChartDataModel chartDataModel) {

		// duration
		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				_tourWeekData.timeLow,
				_tourWeekData.timeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		yData.setAllValueColors(0);
		yData.setVisibleMinValue(0);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_TIME, _activeTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, _tourWeekData.typeIds, _activeTourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_TIME);

		chartDataModel.addYData(yData);
	}

	@Override
	public void deactivateActions(final IWorkbenchPartSite partSite) {}

	public void preferencesHasChanged() {
		updateStatistic(new StatisticContext(_activePerson, _activeTourTypeFilter, _currentYear, _numberOfYears, false));
	}

	public void updateStatistic(final StatisticContext statContext) {

		_activePerson = statContext.person;
		_activeTourTypeFilter = statContext.tourTypeFilter;
		_currentYear = statContext.currentYear;
		_numberOfYears = statContext.numberOfYears;

		_tourWeekData = DataProviderTourWeek.getInstance().getWeekData(
				statContext.person,
				statContext.tourTypeFilter,
				statContext.currentYear,
				statContext.numberOfYears,
				isDataDirtyWithReset() || statContext.isRefreshData);

		// set week start values
		_firstDayOfWeek = _prefStore.getInt(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);
		_minimalDaysInFirstWeek = _prefStore.getInt(ITourbookPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK);

		_calendar.setFirstDayOfWeek(_firstDayOfWeek);
		_calendar.setMinimalDaysInFirstWeek(_minimalDaysInFirstWeek);

		// reset min/max values
		if (_isSynchScaleEnabled == false && statContext.isRefreshData) {
			_minMaxKeeper.resetMinMax();
		}

		final ChartDataModel chartDataModel = createChartDataModel();

		setChartProviders(chartDataModel);

		if (_isSynchScaleEnabled) {
			_minMaxKeeper.setMinMaxValues(chartDataModel);
		}

		// set grid size
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		_chart.setGrid(
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE),
				prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES),
				prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES));

		_chart.updateChart(chartDataModel, true);
	}

	@Override
	public void resetSelection() {
		_chart.setSelectedBars(null);
	}

	private void setChartProviders(final ChartDataModel chartModel) {

		// set tool tip info
		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
				return createToolTipInfo(serieIndex, valueIndex);
			}
		});
	}

	@Override
	public void setSynchScale(final boolean isSynchScaleEnabled) {
		_isSynchScaleEnabled = isSynchScaleEnabled;
	}

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		_chart.fillToolbar(refreshToolbar);
	}
}
