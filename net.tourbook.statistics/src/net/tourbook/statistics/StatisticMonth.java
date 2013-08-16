/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
import java.text.FieldPosition;
import java.util.Calendar;
import java.util.Date;
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
import net.tourbook.chart.ChartType;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.data.TourPerson;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.StatisticContext;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

public abstract class StatisticMonth extends YearStatistic {

	private TourPerson					_activePerson;
	private TourTypeFilter				_activeTourTypeFilter;

	private int							_currentYear;
	private int							_numberOfYears;

	private Chart						_chart;
	private final BarChartMinMaxKeeper	_minMaxKeeper	= new BarChartMinMaxKeeper();

	private boolean						_isSynchScaleEnabled;

	private final Calendar				_calendar		= GregorianCalendar.getInstance();
	private DateFormat					_dateFormatter	= DateFormat.getDateInstance(DateFormat.FULL);

	private TourDataMonth				_tourMonthData;

	public boolean canTourBeVisible() {
		return false;
	}

	ChartSegments createChartSegments(final TourDataMonth tourMonthData) {

		/*
		 * create segments for each year
		 */
		final int monthCounter = tourMonthData.altitudeHigh[0].length;
		final long segmentStart[] = new long[_numberOfYears];
		final long segmentEnd[] = new long[_numberOfYears];
		final String[] segmentTitle = new String[_numberOfYears];

		final int oldestYear = _currentYear - _numberOfYears + 1;

		// get start/end and title for each segment
		for (int monthIndex = 0; monthIndex < monthCounter; monthIndex++) {

			final int yearIndex = monthIndex / 12;

			if (monthIndex % 12 == 0) {

				// first month in a year
				segmentStart[yearIndex] = monthIndex;
				segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

			} else if (monthIndex % 12 == 11) {

				// last month in a year
				segmentEnd[yearIndex] = monthIndex;
			}
		}

		final ChartSegments monthSegments = new ChartSegments();
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
		_chart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		_chart.setShowZoomActions(true);
		_chart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);
	}

	double[] createMonthData(final TourDataMonth tourMonthData) {

		/*
		 * create segments for each year
		 */
		final int monthCounter = tourMonthData.altitudeHigh[0].length;
		final double[] allMonths = new double[monthCounter];

		// get start/end and title for each segment
		for (int monthIndex = 0; monthIndex < monthCounter; monthIndex++) {
			allMonths[monthIndex] = monthIndex;
		}

		return allMonths;
	}

	private ChartToolTipInfo createToolTipInfo(final int serieIndex, final int valueIndex) {

		final int oldestYear = _currentYear - _numberOfYears + 1;

		final Calendar calendar = GregorianCalendar.getInstance();

		calendar.set(oldestYear, 0, 1);
		calendar.add(Calendar.MONTH, valueIndex);

		//
		final StringBuffer monthStringBuffer = new StringBuffer();
		final FieldPosition monthPosition = new FieldPosition(DateFormat.MONTH_FIELD);

		final Date date = new Date();
		date.setTime(calendar.getTimeInMillis());
		_dateFormatter.format(date, monthStringBuffer, monthPosition);

		final Integer recordingTime = _tourMonthData.recordingTime[serieIndex][valueIndex];
		final Integer drivingTime = _tourMonthData.drivingTime[serieIndex][valueIndex];
		final int breakTime = recordingTime - drivingTime;

		/*
		 * tool tip: title
		 */
		final StringBuilder titleString = new StringBuilder();

		final String tourTypeName = getTourTypeName(serieIndex, _activeTourTypeFilter);
		if (tourTypeName != null && tourTypeName.length() > 0) {
			titleString.append(tourTypeName);
		}

		final String toolTipTitle = String.format(Messages.tourtime_info_date_month, //
				titleString.toString(),
				monthStringBuffer.substring(monthPosition.getBeginIndex(), monthPosition.getEndIndex()),
				calendar.get(Calendar.YEAR)
		//
				)
				.toString();

		/*
		 * tool tip: label
		 */
		final StringBuilder toolTipFormat = new StringBuilder();
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

		final String toolTipLabel = String.format(toolTipFormat.toString(), //
				//
				_tourMonthData.distanceHigh[serieIndex][valueIndex] / 1000,
				UI.UNIT_LABEL_DISTANCE,
				//
				(int) _tourMonthData.altitudeHigh[serieIndex][valueIndex],
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
//		toolTipInfo.setLabel(toolTipFormat.toString());

		return toolTipInfo;
	}

	void createXDataMonths(final ChartDataModel chartDataModel) {

		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(createMonthData(_tourMonthData));
		xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_MONTH);
		xData.setChartSegments(createChartSegments(_tourMonthData));

		chartDataModel.setXData(xData);
	}

	void createYDataAltitude(final ChartDataModel chartDataModel) {
		// altitude
		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartType.BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				_tourMonthData.altitudeLow,
				_tourMonthData.altitudeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
		yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE);
		StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE, _activeTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, _tourMonthData.typeIds, _activeTourTypeFilter);

		chartDataModel.addYData(yData);
	}

	void createYDataDistance(final ChartDataModel chartDataModel) {
		// distance
		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartType.BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				_tourMonthData.distanceLow,
				_tourMonthData.distanceHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
		yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setValueDivisor(1000);
		StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE);
		StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE, _activeTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, _tourMonthData.typeIds, _activeTourTypeFilter);

		chartDataModel.addYData(yData);
	}

	void createYDataTourTime(final ChartDataModel chartDataModel) {
		// duration
		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartType.BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				_tourMonthData.getTimeLowFloat(),
				_tourMonthData.getTimeHighFloat());
		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_TIME);
		StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TIME, _activeTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, _tourMonthData.typeIds, _activeTourTypeFilter);

		chartDataModel.addYData(yData);
	}

	public void preferencesHasChanged() {
		updateStatistic(new StatisticContext(_activePerson, _activeTourTypeFilter, _currentYear, _numberOfYears, false));
	}

	@Override
	public void resetSelection() {
		_chart.setSelectedBars(null);
	}

	@Override
	public boolean selectMonth(final Long date) {

		_calendar.setTimeInMillis(date);
		final int selectedMonth = _calendar.get(Calendar.MONTH);

		final boolean selectedItems[] = new boolean[12];
		selectedItems[selectedMonth] = true;

		_chart.setSelectedBars(selectedItems);

		return true;
	}

	private void setChartProviders(final ChartDataModel chartModel) {

		// set tool tip info
		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
			@Override
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
				return createToolTipInfo(serieIndex, valueIndex);
			}
		});
	}

	@Override
	public void setSynchScale(final boolean isSynchScaleEnabled) {
		_isSynchScaleEnabled = isSynchScaleEnabled;
	}

	abstract ChartDataModel updateChart();

	public void updateStatistic(final StatisticContext statContext) {

		_activePerson = statContext.appPerson;
		_activeTourTypeFilter = statContext.appTourTypeFilter;
		_currentYear = statContext.statYoungestYear;
		_numberOfYears = statContext.statNumberOfYears;

		_tourMonthData = DataProviderTourMonth.getInstance().getMonthData(
				statContext.appPerson,
				statContext.appTourTypeFilter,
				statContext.statYoungestYear,
				statContext.statNumberOfYears,
				isDataDirtyWithReset() || statContext.isRefreshData);

		// reset min/max values
		if (_isSynchScaleEnabled == false && statContext.isRefreshData) {
			_minMaxKeeper.resetMinMax();
		}

		final ChartDataModel chartDataModel = updateChart();

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

		// show the fDataModel in the chart
		_chart.updateChart(chartDataModel, true);
	}

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		_chart.fillToolbar(refreshToolbar);
	}
}
