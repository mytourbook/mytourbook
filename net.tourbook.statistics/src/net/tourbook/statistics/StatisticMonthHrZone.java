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
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartSegments;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.StatisticContext;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public class StatisticMonthHrZone extends YearStatistic {

	private static final String			STATE_HR_ZONE_START_FOR_MONTH_BAR_ORDERING	= "STATE_HR_ZONE_START_FOR_MONTH_BAR_ORDERING"; ////$NON-NLS-1$

	private final IPreferenceStore		_prefStore									= TourbookPlugin.getDefault() //
																							.getPreferenceStore();

	private TourPerson					_appPerson;
	private TourTypeFilter				_appTourTypeFilter;

	private int							_statYoungestYear;
	private int							_statNumberOfYears;

	private Chart						_chart;
	private final BarChartMinMaxKeeper	_minMaxKeeper								= new BarChartMinMaxKeeper();
	private IChartInfoProvider			_tooltipProvider;

	private boolean						_isSynchScaleEnabled;
	private final Calendar				_calendar									= GregorianCalendar.getInstance();

	private TourDataMonthHrZones		_tourMonthData;

	private int							_barOrderStart;

	private TourPersonHRZone[]			_personHrZones;
	private TourPersonHRZone[]			_resortedPersonHrZones;
	private int[][]						_resortedHrZoneValues;

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
		final int monthCounter = monthData.hrZoneValues[0].length;
		final long segmentStart[] = new long[_statNumberOfYears];
		final long segmentEnd[] = new long[_statNumberOfYears];
		final String[] segmentTitle = new String[_statNumberOfYears];

		final int oldestYear = _statYoungestYear - _statNumberOfYears + 1;

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
		_chart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);

//		_tooltipProvider = new IChartInfoProvider() {
//			@Override
//			public ChartToolTipContext getToolTipContext(final int serieIndex, final int valueIndex) {
//
//				_toolTipContextSerieIndex = serieIndex;
//				_toolTipContextValueIndex = valueIndex;
//
//				return _toolTipContext;
//			}
//		};
	}

	private float[] createMonthData(final TourDataMonthHrZones monthData) {

		/*
		 * create segments for each year
		 */
		final int monthCounter = monthData.hrZoneValues[0].length;
		final float allMonths[] = new float[monthCounter];

		// get start/end and title for each segment
		for (int monthIndex = 0; monthIndex < monthCounter; monthIndex++) {
			allMonths[monthIndex] = monthIndex;
		}

		return allMonths;
	}

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
		final int zoneSize = _resortedPersonHrZones.length;

		final int[][] monthHrZones = _resortedHrZoneValues;
		final int serieValueLength = monthHrZones[0].length;

		final int[][] hrZones0 = new int[zoneSize][serieValueLength];
		final int[][] hrColorIndex = new int[zoneSize][serieValueLength];
		final int[][] hrZoneValues = new int[zoneSize][];

		final RGB[] rgbBright = new RGB[zoneSize];
		final RGB[] rgbDark = new RGB[zoneSize];
		final RGB[] rgbLine = new RGB[zoneSize];

		int zoneIndex = 0;

		for (final TourPersonHRZone hrZone : _resortedPersonHrZones) {

			rgbDark[zoneIndex] = hrZone.getColor();
			rgbBright[zoneIndex] = hrZone.getColorBright();
			rgbLine[zoneIndex] = hrZone.getColorDark();

			// set color index for HR zones
			Arrays.fill(hrColorIndex[zoneIndex], zoneIndex);

			// truncate values to the available hr zones in the person
			hrZoneValues[zoneIndex] = monthHrZones[zoneIndex];

			zoneIndex++;
		}

		final ChartDataYSerie yData = new ChartDataYSerie(//
				ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				Util.convertIntToFloat(hrZones0),
				Util.convertIntToFloat(hrZoneValues));

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

	private void getPreferences() {

		_chart.setGrid(
				_prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
				_prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE),
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES),
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES));
	}

	public void preferencesHasChanged() {
		updateStatistic();
	}

	/**
	 * resort HR zones + values according to the sequence start
	 */
	private void reorderHrZones(final int maxBarLength) {

		_resortedHrZoneValues = new int[maxBarLength][];
		_resortedPersonHrZones = new TourPersonHRZone[maxBarLength];

		int resortedIndex = 0;
		final int[][] monthHrZoneValues = _tourMonthData.hrZoneValues;

		if (_barOrderStart >= maxBarLength) {

			final int barOrderStart = _barOrderStart % maxBarLength;

			// set HR zones starting from the sequence start
			for (int serieIndex = barOrderStart; serieIndex >= 0; serieIndex--) {

				_resortedHrZoneValues[resortedIndex] = monthHrZoneValues[serieIndex];
				_resortedPersonHrZones[resortedIndex] = _personHrZones[serieIndex];

				resortedIndex++;
			}

			// set HR zones starting from the last
			for (int serieIndex = maxBarLength - 1; resortedIndex < maxBarLength; serieIndex--) {

				_resortedHrZoneValues[resortedIndex] = monthHrZoneValues[serieIndex];
				_resortedPersonHrZones[resortedIndex] = _personHrZones[serieIndex];

				resortedIndex++;
			}

		} else {

			final int barOrderStart = _barOrderStart;

			// set HR zones starting from the sequence start
			for (int serieIndex = barOrderStart; serieIndex < maxBarLength; serieIndex++) {

				_resortedHrZoneValues[resortedIndex] = monthHrZoneValues[serieIndex];
				_resortedPersonHrZones[resortedIndex] = _personHrZones[serieIndex];

				resortedIndex++;
			}

			// set HR zones starting from 0
			for (int serieIndex = 0; resortedIndex < maxBarLength; serieIndex++) {

				_resortedHrZoneValues[resortedIndex] = monthHrZoneValues[serieIndex];
				_resortedPersonHrZones[resortedIndex] = _personHrZones[serieIndex];

				resortedIndex++;
			}
		}
	}

	@Override
	public void resetSelection() {
		_chart.setSelectedBars(null);
	}

//	private ChartToolTip1 createToolTipInfo(final int serieIndex, final int valueIndex) {
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
//		final ChartToolTip1 toolTipInfo = new ChartToolTip1();
//		toolTipInfo.setTitle(toolTipTitle);
//		toolTipInfo.setLabel(toolTipLabel);
////		toolTipInfo.setLabel(toolTipFormat.toString());
//
//		return toolTipInfo;
//	}

	@Override
	public void restoreStateEarly(final IDialogSettings state) {
		_barOrderStart = Util.getStateInt(state, STATE_HR_ZONE_START_FOR_MONTH_BAR_ORDERING, 0);
	}

	@Override
	public void saveState(final IDialogSettings state) {
		state.put(STATE_HR_ZONE_START_FOR_MONTH_BAR_ORDERING, _barOrderStart);
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
	public void setBarVerticalOrder(final int selectedIndex) {

		_barOrderStart = selectedIndex;

		final ArrayList<TourPersonHRZone> personHrZones = _appPerson.getHrZonesSorted();
		final int[][] weekHrZoneValues = _tourMonthData.hrZoneValues;

		/*
		 * ensure that only available person HR zones are displayed, _tourWeekData.hrZones contains
		 * all 10 zones
		 */
		final int maxBarLength = Math.min(personHrZones.size(), weekHrZoneValues.length);

		if (maxBarLength == 0) {
			return;
		}

		reorderHrZones(maxBarLength);

		updateStatistic();
	}

	@Override
	public void setSynchScale(final boolean isSynchScaleEnabled) {
		_isSynchScaleEnabled = isSynchScaleEnabled;
	}

	private void setupBars10HrZoneOrder(final boolean isNewPerson) {

		final ArrayList<TourPersonHRZone> originalPersonHrZones = _appPerson.getHrZonesSorted();
		final int[][] weekHrZoneValues = _tourMonthData.hrZoneValues;

		/*
		 * ensure that only available person HR zones are displayed, _tourWeekData.hrZones contains
		 * all 10 zones
		 */
		final int maxBarLength = Math.min(originalPersonHrZones.size(), weekHrZoneValues.length);
		if (maxBarLength == 0) {
			return;
		}

		if (isNewPerson) {

			// update HR zones

			_personHrZones = new TourPersonHRZone[maxBarLength];

			int zoneIndex = 0;
			for (final TourPersonHRZone tourPersonHRZone : originalPersonHrZones) {
				_personHrZones[zoneIndex++] = tourPersonHRZone;
			}
		}

		reorderHrZones(maxBarLength);
	}

	/**
	 * Set bar names into the statistic context. The names will be displayed in a combobox in the
	 * statistics toolbar.
	 * 
	 * @param statContext
	 */
	private void setupBars20BarNames(final StatisticContext statContext) {

		final ArrayList<TourPersonHRZone> personHrZones = _appPerson.getHrZonesSorted();
		final int maxSerieSize = Math.min(personHrZones.size(), _tourMonthData.hrZoneValues.length);

		if (personHrZones == null || maxSerieSize == 0) {
			statContext.outIsUpdateBarNames = true;
			statContext.outBarNames = null;
			return;
		}

		int hrZoneIndex = 0;

		// create bar names 2 times
		final String[] barNames = new String[maxSerieSize * 2];

		for (int inverseIndex = 0; inverseIndex < 2; inverseIndex++) {
			for (final TourPersonHRZone tourPersonHRZone : personHrZones) {

				String barName;

				if (inverseIndex == 0) {
					barName = tourPersonHRZone.getNameShort();
				} else {
					barName = tourPersonHRZone.getNameShort() + UI.SPACE + Messages.Statistic_Label_Invers;
				}

				barNames[hrZoneIndex++] = barName;
			}
		}

		// set state what the statistic container should do
		statContext.outIsUpdateBarNames = true;
		statContext.outBarNames = barNames;
		statContext.outVerticalBarIndex = _barOrderStart;
	}

	private ChartDataModel updateChart() {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		createXDataMonths(chartDataModel);
		createYDataHrZone(chartDataModel);

		return chartDataModel;
	}

	private void updateStatistic() {
		updateStatistic(new StatisticContext(
				_appPerson,
				_appTourTypeFilter,
				_statYoungestYear,
				_statNumberOfYears,
				false));
	}

	@Override
	public void updateStatistic(final StatisticContext statContext) {

		/*
		 * check if required data are available
		 */
		if (statContext.appPerson == null) {
			_chart.setErrorMessage(Messages.Statistic_HrZone_ErrorNoPerson);
			return;
		}

		if (statContext.appPerson.getHrZonesSorted().size() == 0) {
			_chart.setErrorMessage(NLS.bind(
					Messages.Statistic_HrZone_Error_NoHrZoneInPerson,
					statContext.appPerson.getName()));
			return;
		}

		final boolean isNewPerson = _appPerson == null || statContext.appPerson != _appPerson;

		// this statistic supports bar reordering
		statContext.outIsBarReorderingSupported = true;

		_appPerson = statContext.appPerson;
		_appTourTypeFilter = statContext.appTourTypeFilter;
		_statYoungestYear = statContext.statYoungestYear;
		_statNumberOfYears = statContext.statNumberOfYears;

		_tourMonthData = DataProviderHrZoneMonth.getInstance().getMonthData(
				_appPerson,
				_appTourTypeFilter,
				_statYoungestYear,
				_statNumberOfYears,
				isDataDirtyWithReset() || statContext.isRefreshData);

		setupBars10HrZoneOrder(isNewPerson);
		setupBars20BarNames(statContext);

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

		getPreferences();

		// show the fDataModel in the chart
		_chart.updateChart(chartDataModel, true);
	}

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		_chart.fillToolbar(refreshToolbar);
	}

}
