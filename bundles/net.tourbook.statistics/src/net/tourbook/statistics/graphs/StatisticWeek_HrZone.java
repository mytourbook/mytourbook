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

public class StatisticWeek_HrZone extends TourbookStatistic {

	private static final String			STATE_HR_ZONE_WEEK_BAR_ORDERING_START	= "STATE_HR_ZONE_WEEK_BAR_ORDERING_START";	////$NON-NLS-1$

	private TourPerson					_appPerson;
	private TourTypeFilter				_appTourTypeFilter;
	private int							_statYoungestYear;
	private int							_statNumberOfYears;

	private Chart						_chart;

	private IChartInfoProvider			_tooltipProvider;

	private final MinMaxKeeper_YData	_minMaxKeeper							= new MinMaxKeeper_YData();
	private boolean						_isSynchScaleEnabled;

	private TourData_WeekHrZones		_tourWeekData;

	private int							_barOrderStart;

	private TourPersonHRZone[]			_personHrZones;
	private TourPersonHRZone[]			_resortedPersonHrZones;

	private int[][]						_resortedHrZoneValues;

	private String[]					_barNames;

	public StatisticWeek_HrZone() {
		super();
	}

	/**
	 * create segments for each week
	 */
	ChartStatisticSegments createChartSegments() {

		final double segmentStart[] = new double[_statNumberOfYears];
		final double segmentEnd[] = new double[_statNumberOfYears];
		final String[] segmentTitle = new String[_statNumberOfYears];

		final int oldestYear = _statYoungestYear - _statNumberOfYears + 1;
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

		final ChartStatisticSegments weekSegments = new ChartStatisticSegments();
		weekSegments.segmentStartValue = segmentStart;
		weekSegments.segmentEndValue = segmentEnd;
		weekSegments.segmentTitle = segmentTitle;

		weekSegments.years = _tourWeekData.years;
		weekSegments.yearWeeks = yearWeeks;
		weekSegments.yearDays = _tourWeekData.yearDays;

		return weekSegments;
	}

	@Override
	public void createStatisticUI(final Composite parent, final IViewSite viewSite) {

		// create chart
		_chart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		_chart.setShowZoomActions(true);
		_chart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);
	}

	private double[] createWeekData() {

		final int weekCounter = _resortedHrZoneValues[0].length;
		final double allWeeks[] = new double[weekCounter];

		for (int weekIndex = 0; weekIndex < weekCounter; weekIndex++) {
			allWeeks[weekIndex] = weekIndex;
		}

		return allWeeks;
	}

	void createXDataWeek(final ChartDataModel chartDataModel) {

		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(createWeekData());
		xData.setAxisUnit(ChartDataSerie.X_AXIS_UNIT_WEEK);
		xData.setChartSegments(createChartSegments());

		chartDataModel.setXData(xData);
	}

	private void createYDataHrZone(final ChartDataModel chartDataModel) {

		/*
		 * number of person hr zones decides how many hr zones are displayed
		 */
		final int zoneSize = _resortedPersonHrZones.length;

		final int[][] weekHrZones = _resortedHrZoneValues;
		final int serieValueLength = weekHrZones[0].length;

		final float[][] hrZones0 = new float[zoneSize][serieValueLength];
		final int[][] hrColorIndex = new int[zoneSize][serieValueLength];
		final float[][] hrZoneValues = new float[zoneSize][];

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

			/*
			 * truncate values to the available hr zones in the person and convert from in to float
			 */
			final int[] weekHrZone = weekHrZones[zoneIndex];
			final float[] hrZoneValue = hrZoneValues[zoneIndex] = new float[weekHrZone.length];

			for (int valueIndex = 0; valueIndex < weekHrZone.length; valueIndex++) {
				hrZoneValue[valueIndex] = weekHrZone[valueIndex];
			}

			zoneIndex++;
		}

		final ChartDataYSerie yData = new ChartDataYSerie(//
				ChartType.BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				hrZones0,
				hrZoneValues);

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
		return GRID_WEEK_HR_ZONE;
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
		final int[][] weekHrZoneValues = _tourWeekData.hrZoneValues;

		if (_barOrderStart >= maxBarLength) {

			final int barOrderStart = _barOrderStart % maxBarLength;

			// set HR zones starting from the sequence start
			for (int serieIndex = barOrderStart; serieIndex >= 0; serieIndex--) {

				_resortedHrZoneValues[resortedIndex] = weekHrZoneValues[serieIndex];
				_resortedPersonHrZones[resortedIndex] = _personHrZones[serieIndex];

				resortedIndex++;
			}

			// set HR zones starting from the last
			for (int serieIndex = maxBarLength - 1; resortedIndex < maxBarLength; serieIndex--) {

				_resortedHrZoneValues[resortedIndex] = weekHrZoneValues[serieIndex];
				_resortedPersonHrZones[resortedIndex] = _personHrZones[serieIndex];

				resortedIndex++;
			}

		} else {

			final int barOrderStart = _barOrderStart;

			// set HR zones starting from the sequence start
			for (int serieIndex = barOrderStart; serieIndex < maxBarLength; serieIndex++) {

				_resortedHrZoneValues[resortedIndex] = weekHrZoneValues[serieIndex];
				_resortedPersonHrZones[resortedIndex] = _personHrZones[serieIndex];

				resortedIndex++;
			}

			// set HR zones starting from 0
			for (int serieIndex = 0; resortedIndex < maxBarLength; serieIndex++) {

				_resortedHrZoneValues[resortedIndex] = weekHrZoneValues[serieIndex];
				_resortedPersonHrZones[resortedIndex] = _personHrZones[serieIndex];

				resortedIndex++;
			}
		}
	}

	@Override
	public void restoreStateEarly(final IDialogSettings state) {
		_barOrderStart = Util.getStateInt(state, STATE_HR_ZONE_WEEK_BAR_ORDERING_START, 0);
	}

	@Override
	public void saveState(final IDialogSettings state) {
		state.put(STATE_HR_ZONE_WEEK_BAR_ORDERING_START, _barOrderStart);
	}

	@Override
	public void setBarVerticalOrder(final int selectedIndex) {

		_barOrderStart = selectedIndex;

		final ArrayList<TourPersonHRZone> personHrZones = _appPerson.getHrZonesSorted();
		final int[][] weekHrZoneValues = _tourWeekData.hrZoneValues;

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
		final int[][] weekHrZoneValues = _tourWeekData.hrZoneValues;

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
		final int maxSerieSize = Math.min(personHrZones.size(), _tourWeekData.hrZoneValues.length);

		if (personHrZones == null || maxSerieSize == 0) {
			statContext.outIsUpdateBarNames = true;
			statContext.outBarNames = _barNames = null;
			return;
		}

		int hrZoneIndex = 0;

		// create bar names 2 times
		_barNames = new String[maxSerieSize * 2];

		for (int inverseIndex = 0; inverseIndex < 2; inverseIndex++) {
			for (final TourPersonHRZone tourPersonHRZone : personHrZones) {

				String barName;

				if (inverseIndex == 0) {
					barName = tourPersonHRZone.getNameShort();
				} else {
					barName = tourPersonHRZone.getNameShort() + UI.SPACE + Messages.Statistic_Label_Invers;
				}

				_barNames[hrZoneIndex++] = barName;
			}
		}

		// set state what the statistic container should do
		statContext.outIsUpdateBarNames = true;
		statContext.outBarNames = _barNames;
		statContext.outVerticalBarIndex = _barOrderStart;
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

		// this statistic supports bar reordering
		statContext.outIsBarReorderingSupported = true;

		final boolean isNewPerson = _appPerson == null || statContext.appPerson != _appPerson;

		_appPerson = statContext.appPerson;
		_appTourTypeFilter = statContext.appTourTypeFilter;
		_statYoungestYear = statContext.statFirstYear;
		_statNumberOfYears = statContext.statNumberOfYears;

		_tourWeekData = DataProvider_HrZone_Week.getInstance().getWeekData(
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

		// create data model
		final ChartDataModel chartDataModel = new ChartDataModel(ChartType.BAR);

		createXDataWeek(chartDataModel);
		createYDataHrZone(chartDataModel);

		// set tool tip info
		chartDataModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, _tooltipProvider);

		if (_isSynchScaleEnabled) {
			_minMaxKeeper.setMinMaxValues(chartDataModel);
		}

		StatisticServices.updateChartProperties(_chart, getGridPrefPrefix());

		// show the data model in the chart
		_chart.updateChart(chartDataModel, true);
	}

	@Override
	public void updateToolBar() {
		_chart.fillToolbar(true);
	}

}
