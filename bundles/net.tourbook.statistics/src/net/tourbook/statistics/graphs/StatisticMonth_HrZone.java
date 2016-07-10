/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.statistics.graphs;

import java.util.ArrayList;
import java.util.Arrays;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartStatisticSegments;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.chart.MinMaxKeeper_YData;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.statistic.StatisticContext;
import net.tourbook.statistic.TourbookStatistic;
import net.tourbook.statistics.Messages;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.ui.ChartOptions_Grid;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewSite;

public class StatisticMonth_HrZone extends TourbookStatistic {

	private TourPerson					_appPerson;
	private TourTypeFilter				_appTourTypeFilter;

	private int							_statYoungestYear;
	private int							_statNumberOfYears;

	private Chart						_chart;
	private final MinMaxKeeper_YData	_minMaxKeeper	= new MinMaxKeeper_YData();
	private IChartInfoProvider			_tooltipProvider;

	private boolean						_isSynchScaleEnabled;

	private TourData_MonthHrZones		_tourMonthData;

	private int							_barOrderStart;

	private TourPersonHRZone[]			_personHrZones;
	private TourPersonHRZone[]			_resortedPersonHrZones;
	private int[][]						_resortedHrZoneValues;

	public StatisticMonth_HrZone() {
		super();
	}

	private ChartStatisticSegments createChartSegments(final TourData_MonthHrZones monthData) {

		/*
		 * create segments for each year
		 */
		final int monthCounter = monthData.hrZoneValues[0].length;
		final double segmentStart[] = new double[_statNumberOfYears];
		final double segmentEnd[] = new double[_statNumberOfYears];
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

		final ChartStatisticSegments monthSegments = new ChartStatisticSegments();
		monthSegments.segmentStartValue = segmentStart;
		monthSegments.segmentEndValue = segmentEnd;
		monthSegments.segmentTitle = segmentTitle;

		return monthSegments;
	}

	private double[] createMonthData(final TourData_MonthHrZones monthData) {

		/*
		 * create segments for each year
		 */
		final int monthCounter = monthData.hrZoneValues[0].length;
		final double allMonths[] = new double[monthCounter];

		// get start/end and title for each segment
		for (int monthIndex = 0; monthIndex < monthCounter; monthIndex++) {
			allMonths[monthIndex] = monthIndex;
		}

		return allMonths;
	}

	@Override
	public void createStatisticUI(final Composite parent, final IViewSite viewSite) {

		// create chart
		_chart = new Chart(parent, SWT.FLAT);
		_chart.setShowZoomActions(true);
		_chart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);
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
				ChartType.BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				Util.convertIntToFloat(hrZones0),
				Util.convertIntToFloat(hrZoneValues));

		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		yData.setShowYSlider(true);

		yData.setColorIndex(hrColorIndex);
		yData.setRgbLine(rgbLine);
		yData.setRgbBright(rgbBright);
		yData.setRgbDark(rgbDark);
		yData.setDefaultRGB(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY).getRGB());

		chartDataModel.addYData(yData);
	}

	@Override
	public int getEnabledGridOptions() {

		return ChartOptions_Grid.GRID_VERTICAL_DISTANCE
				| ChartOptions_Grid.GRID_IS_SHOW_HORIZONTAL_LINE
				| ChartOptions_Grid.GRID_IS_SHOW_VERTICAL_LINE;
	}

	@Override
	protected String getGridPrefPrefix() {
		return GRID_MONTH_HR_ZONE;
	}

	@Override
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
	public void restoreStateEarly(final IDialogSettings state) {

		_barOrderStart = Util.getStateInt(state, STATE_BAR_ORDERING_HR_ZONE_START_FOR_MONTH, 0);
	}

	@Override
	public void saveState(final IDialogSettings state) {

		state.put(STATE_BAR_ORDERING_HR_ZONE_START_FOR_MONTH, _barOrderStart);
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

		if (!isSynchScaleEnabled) {

			// reset when it's disabled

			_minMaxKeeper.resetMinMax();
		}

		_isSynchScaleEnabled = isSynchScaleEnabled;
	}

	private void setupBars_10_HrZoneOrder(final boolean isNewPerson) {

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
	private void setupBars_20_BarNames(final StatisticContext statContext) {

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

		final ChartDataModel chartDataModel = new ChartDataModel(ChartType.BAR);

		createXDataMonths(chartDataModel);
		createYDataHrZone(chartDataModel);

		return chartDataModel;
	}

	private void updateStatistic() {

		updateStatistic(new StatisticContext(_appPerson, _appTourTypeFilter, _statYoungestYear, _statNumberOfYears));
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
		_statYoungestYear = statContext.statFirstYear;
		_statNumberOfYears = statContext.statNumberOfYears;

		_tourMonthData = DataProvider_HrZone_Month.getInstance().getMonthData(
				_appPerson,
				_appTourTypeFilter,
				_statYoungestYear,
				_statNumberOfYears,
				isDataDirtyWithReset() || statContext.isRefreshData);

		setupBars_10_HrZoneOrder(isNewPerson);
		setupBars_20_BarNames(statContext);

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

		StatisticServices.updateChartProperties(_chart, getGridPrefPrefix());

		// show the fDataModel in the chart
		_chart.updateChart(chartDataModel, true);
	}

	@Override
	public void updateToolBar() {
		_chart.fillToolbar(true);
	}

}
