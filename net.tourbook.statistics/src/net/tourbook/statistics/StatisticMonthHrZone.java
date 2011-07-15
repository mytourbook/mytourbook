/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.BarTooltipProvider;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartSegments;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.StatisticContext;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public class StatisticMonthHrZone extends YearStatistic {

	private TourPerson					_currentPerson;

	private int							_currentYear;

	private int							_numberOfYears;
	private Chart						_chart;

	private IChartInfoProvider			_tooltipProvider;

	private final BarChartMinMaxKeeper	_minMaxKeeper	= new BarChartMinMaxKeeper();
	private boolean						_isSynchScaleEnabled;

	private final Calendar				_calendar		= GregorianCalendar.getInstance();
//	private DateFormat					_dateFormatter	= DateFormat.getDateInstance(DateFormat.FULL);

	private TourDataMonthHrZones		_tourMonthData;

	public class BarTooltipProviderImpl implements BarTooltipProvider {

	}

	public StatisticMonthHrZone() {
		super();
	}

	@Override
	public void activateActions(final IWorkbenchPartSite partSite) {
		_chart.updateChartActionHandlers();
	}

	private ChartSegments createChartSegments(final TourDataMonthHrZones monthData) {

		/*
		 * create segments for each year
		 */
		final int monthCounter = monthData.hrZones[0].length;
		final int segmentStart[] = new int[_numberOfYears];
		final int segmentEnd[] = new int[_numberOfYears];
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

		this.createControl(parent);

		// create chart
		_chart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		_chart.setShowZoomActions(true);
		_chart.setCanScrollZoomedChart(true);
		_chart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);

		final BarTooltipProvider barTooltipProvider = new BarTooltipProviderImpl();
		_chart.setBarTooltipProvider(barTooltipProvider);

//		_tooltipProvider = new IChartInfoProvider() {
//			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
//				return createToolTipInfo(serieIndex, valueIndex);
//			}
//		};

	}

	private int[] createMonthData(final TourDataMonthHrZones monthData) {

		/*
		 * create segments for each year
		 */
		final int monthCounter = monthData.hrZones[0].length;
		final int allMonths[] = new int[monthCounter];

		// get start/end and title for each segment
		for (int monthIndex = 0; monthIndex < monthCounter; monthIndex++) {
			allMonths[monthIndex] = monthIndex;
		}

		return allMonths;
	}

//	private ChartToolTipInfo createToolTipInfo(final int serieIndex, final int valueIndex) {
//
//		final int oldestYear = fCurrentYear - fNumberOfYears + 1;
//
//		final Calendar calendar = GregorianCalendar.getInstance();
//
//		calendar.set(oldestYear, 0, 1);
//		calendar.add(Calendar.MONTH, valueIndex);
//
//		//
//		final StringBuffer monthStringBuffer = new StringBuffer();
//		final FieldPosition monthPosition = new FieldPosition(DateFormat.MONTH_FIELD);
//
//		final Date date = new Date();
//		date.setTime(calendar.getTimeInMillis());
//		fDateFormatter.format(date, monthStringBuffer, monthPosition);
//
//		final Integer recordingTime = fTourMonthData.fRecordingTime[serieIndex][valueIndex];
//		final Integer drivingTime = fTourMonthData.fDrivingTime[serieIndex][valueIndex];
//		final int breakTime = recordingTime - drivingTime;
//
//		/*
//		 * tool tip: title
//		 */
//		final StringBuilder titleString = new StringBuilder();
//
//		final String tourTypeName = getTourTypeName(serieIndex, fActiveTourTypeFilter);
//		if (tourTypeName != null && tourTypeName.length() > 0) {
//			titleString.append(tourTypeName);
//		}
//
//		final String toolTipTitle = new Formatter().format(Messages.tourtime_info_date_month, //
//				titleString.toString(),
//				monthStringBuffer.substring(monthPosition.getBeginIndex(), monthPosition.getEndIndex()),
//				calendar.get(Calendar.YEAR)
//		//
//		)
//				.toString();
//
//		/*
//		 * tool tip: label
//		 */
//		final StringBuilder toolTipFormat = new StringBuilder();
//		toolTipFormat.append(Messages.tourtime_info_distance_tour);
//		toolTipFormat.append(NEW_LINE);
//		toolTipFormat.append(Messages.tourtime_info_altitude);
//		toolTipFormat.append(NEW_LINE);
//		toolTipFormat.append(NEW_LINE);
//		toolTipFormat.append(Messages.tourtime_info_recording_time);
//		toolTipFormat.append(NEW_LINE);
//		toolTipFormat.append(Messages.tourtime_info_driving_time);
//		toolTipFormat.append(NEW_LINE);
//		toolTipFormat.append(Messages.tourtime_info_break_time);
//
//		final String toolTipLabel = new Formatter().format(toolTipFormat.toString(), //
//				//
//				(float) fTourMonthData.fDistanceHigh[serieIndex][valueIndex] / 1000,
//				UI.UNIT_LABEL_DISTANCE,
//				//
//				fTourMonthData.fAltitudeHigh[serieIndex][valueIndex],
//				UI.UNIT_LABEL_ALTITUDE,
//				//
//				recordingTime / 3600,
//				(recordingTime % 3600) / 60,
//				//
//				drivingTime / 3600,
//				(drivingTime % 3600) / 60,
//				//
//				breakTime / 3600,
//				(breakTime % 3600) / 60
//		//
//		)
//				.toString();
//
//		/*
//		 * create tool tip info
//		 */
//
//		final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
//		toolTipInfo.setTitle(toolTipTitle);
//		toolTipInfo.setLabel(toolTipLabel);
////		toolTipInfo.setLabel(toolTipFormat.toString());
//
//		return toolTipInfo;
//	}

	void createXDataMonths(final ChartDataModel chartDataModel) {

		// set x-axis

		final ChartDataXSerie xData = new ChartDataXSerie(createMonthData(_tourMonthData));
		xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_MONTH);
		xData.setChartSegments(createChartSegments(_tourMonthData));

		chartDataModel.setXData(xData);
	}

	private void createYDataHrZone(final ChartDataModel chartDataModel) {

		/*
		 * number of person hr zones decides how many hr zones are displayed
		 */
		final ArrayList<TourPersonHRZone> personHrZones = _currentPerson.getHrZonesSorted();
		final int zoneSize = personHrZones.size();

		final int[][] tourHrZones = _tourMonthData.hrZones;
		//		final int serieLength = hrZoneValues.length;
		final int serieValueLength = tourHrZones[0].length;

		final int[][] hrZones0 = new int[zoneSize][serieValueLength];
		final int[][] hrColorIndex = new int[zoneSize][serieValueLength];
		final int[][] hrZoneValues = new int[zoneSize][];

		final RGB[] rgbBright = new RGB[zoneSize];
		final RGB[] rgbDark = new RGB[zoneSize];
		final RGB[] rgbLine = new RGB[zoneSize];

		int zoneIndex = 0;
		for (final TourPersonHRZone hrZone : personHrZones) {

			rgbDark[zoneIndex] = hrZone.getColor();
			rgbBright[zoneIndex] = hrZone.getColorBright();
			rgbLine[zoneIndex] = hrZone.getColorDark();

			// set color index for HR zones
			Arrays.fill(hrColorIndex[zoneIndex], zoneIndex);

			// truncate values to the available hr zones in the person
			hrZoneValues[zoneIndex] = tourHrZones[zoneIndex];

			zoneIndex++;
		}

		final ChartDataYSerie yData = new ChartDataYSerie(//
				ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				hrZones0,
				hrZoneValues);

		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);

		yData.setColorIndex(hrColorIndex);
		yData.setRgbLine(rgbLine);
		yData.setRgbBright(rgbBright);
		yData.setRgbDark(rgbDark);
		yData.setDefaultRGB(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY).getRGB());

		chartDataModel.addYData(yData);
	}

	@Override
	public void deactivateActions(final IWorkbenchPartSite partSite) {}

	public void preferencesHasChanged() {
		updateStatistic(new StatisticContext(_currentPerson, null, _currentYear, _numberOfYears, false));
	}

	@Override
	public void updateStatistic(final StatisticContext statContext) {

		if (statContext.person == null) {
			return;
		}

		_currentPerson = statContext.person;
		_currentYear = statContext.currentYear;
		_numberOfYears = statContext.numberOfYears;

		_tourMonthData = DataProviderHrZoneMonth.getInstance().getMonthData(
				statContext.person,
				statContext.tourTypeFilter,
				statContext.currentYear,
				statContext.numberOfYears,
				isDataDirtyWithReset() || statContext.isRefreshData);

		// reset min/max values
		if (_isSynchScaleEnabled == false && statContext.isRefreshData) {
			_minMaxKeeper.resetMinMax();
		}

		final ChartDataModel chartDataModel = updateChart();

		// set tool tip info
		chartDataModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, _tooltipProvider);

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

	@Override
	public void setSynchScale(final boolean isSynchScaleEnabled) {
		_isSynchScaleEnabled = isSynchScaleEnabled;
	}

	private ChartDataModel updateChart() {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		createXDataMonths(chartDataModel);
		createYDataHrZone(chartDataModel);

		return chartDataModel;
	}

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		_chart.fillToolbar(refreshToolbar);
	}

}
