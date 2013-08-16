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

import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartToolTipInfo;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.chart.Util;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.StatisticContext;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewSite;

public class StatisticTourNumbers extends YearStatistic {

	private final IPreferenceStore		_prefStore							= TourbookPlugin
																					.getDefault()
																					.getPreferenceStore();

	private IPropertyChangeListener		_prefChangeListener;

	private final BarChartMinMaxKeeper	_minMaxKeeperStatAltitudeCounter	= new BarChartMinMaxKeeper();
	private final BarChartMinMaxKeeper	_minMaxKeeperStatAltitudeSum		= new BarChartMinMaxKeeper();
	private final BarChartMinMaxKeeper	_minMaxKeeperStatDistanceCounter	= new BarChartMinMaxKeeper();
	private final BarChartMinMaxKeeper	_minMaxKeeperStatDistanceSum		= new BarChartMinMaxKeeper();
	private final BarChartMinMaxKeeper	_minMaxKeeperStatDurationCounter	= new BarChartMinMaxKeeper();
	private final BarChartMinMaxKeeper	_minMaxKeeperStatDurationSum		= new BarChartMinMaxKeeper();

	private int[]						_statDistanceUnits;
	private int[]						_statAltitudeUnits;
	private int[]						_statTimeUnits;

	private int[][]						_statDistanceCounterLow;
	private int[][]						_statDistanceCounterHigh;
	private int[][]						_statDistanceCounterColorIndex;

	private int[][]						_statDistanceSumLow;
	private int[][]						_statDistanceSumHigh;
	private int[][]						_statDistanceSumColorIndex;

	private int[][]						_statAltitudeCounterLow;
	private int[][]						_statAltitudeCounterHigh;
	private int[][]						_statAltitudeCounterColorIndex;

	private int[][]						_statAltitudeSumLow;
	private int[][]						_statAltitudeSumHigh;
	private int[][]						_statAltitudeSumColorIndex;

	private int[][]						_statTimeCounterLow;
	private int[][]						_statTimeCounterHigh;
	private int[][]						_statTimeCounterColorIndex;

	private int[][]						_statTimeSumLow;
	private int[][]						_statTimeSumHigh;
	private int[][]						_statTimeSumColorIndex;

	private int							_currentYear;
	private TourPerson					_activePerson;
	private TourTypeFilter				_activeTourTypeFilter;

	private boolean						_isSynchScaleEnabled;

	private TourDataDay					_tourDayData;

	private IViewSite					_viewSite;

	/*
	 * UI controls
	 */
	private Composite					_statisticPage;

	private Chart						_chartDistanceCounter;
	private Chart						_chartDistanceSum;
	private Chart						_chartDurationCounter;
	private Chart						_chartDurationSum;
	private Chart						_chartAltitudeCounter;
	private Chart						_chartAltitudeSum;

	public StatisticTourNumbers() {}

	void addPrefListener(final Composite container) {

		// create pref listener
		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				final String property = event.getProperty();

				// test if the color or statistic data have changed
				if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)
						|| property.equals(ITourbookPreferences.STAT_DISTANCE_NUMBERS)
						|| property.equals(ITourbookPreferences.STAT_DISTANCE_LOW_VALUE)
						|| property.equals(ITourbookPreferences.STAT_DISTANCE_INTERVAL)
						|| property.equals(ITourbookPreferences.STAT_ALTITUDE_NUMBERS)
						|| property.equals(ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE)
						|| property.equals(ITourbookPreferences.STAT_ALTITUDE_INTERVAL)
						|| property.equals(ITourbookPreferences.STAT_DURATION_NUMBERS)
						|| property.equals(ITourbookPreferences.STAT_DURATION_LOW_VALUE)
						|| property.equals(ITourbookPreferences.STAT_DURATION_INTERVAL)) {

					// get the changed preferences
					getPreferences();

					/*
					 * reset min/max keeper because they can be changed when the pref has changed
					 */
					resetMinMaxKeeper();

					// update chart
					updateStatistic(new StatisticContext(_activePerson, _activeTourTypeFilter, _currentYear, 1, false));
				}
			}
		};

		// add pref listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);

		// remove pref listener
		container.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				_prefStore.removePropertyChangeListener(_prefChangeListener);
			}
		});
	}

	public boolean canTourBeVisible() {
		return false;
	}

	@Override
	public void createControl(	final Composite parent,
								final IViewSite viewSite,
								final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		_viewSite = viewSite;

		// create statistic page
		_statisticPage = new Composite(parent, SWT.BORDER | SWT.FLAT);
		_statisticPage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// remove colored border
		_statisticPage.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		final GridLayout gl = new GridLayout(2, true);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		_statisticPage.setLayout(gl);

		_chartDistanceCounter = new Chart(_statisticPage, SWT.NONE);
		_chartDistanceSum = new Chart(_statisticPage, SWT.NONE);

		_chartAltitudeCounter = new Chart(_statisticPage, SWT.NONE);
		_chartAltitudeSum = new Chart(_statisticPage, SWT.NONE);

		_chartDurationCounter = new Chart(_statisticPage, SWT.NONE);
		_chartDurationSum = new Chart(_statisticPage, SWT.NONE);

		_chartDistanceCounter.setToolBarManager(viewSite.getActionBars().getToolBarManager(), true);

		addPrefListener(parent);
		getPreferences();
	}

	/**
	 * calculate data for all statistics
	 * 
	 * @param tourDayData
	 */
	private void createStatisticData(final TourDataDay tourDayData) {

		int colorOffset = 0;
		if (_activeTourTypeFilter.showUndefinedTourTypes()) {
			colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
		}

		final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
		final int colorLength = colorOffset + tourTypeList.size();

		final int distanceLength = _statDistanceUnits.length;
		final int altitudeLength = _statAltitudeUnits.length;
		final int timeLength = _statTimeUnits.length;

		_statDistanceCounterLow = new int[colorLength][distanceLength];
		_statDistanceCounterHigh = new int[colorLength][distanceLength];
		_statDistanceCounterColorIndex = new int[colorLength][distanceLength];

		_statDistanceSumLow = new int[colorLength][distanceLength];
		_statDistanceSumHigh = new int[colorLength][distanceLength];
		_statDistanceSumColorIndex = new int[colorLength][distanceLength];

		_statAltitudeCounterLow = new int[colorLength][altitudeLength];
		_statAltitudeCounterHigh = new int[colorLength][altitudeLength];
		_statAltitudeCounterColorIndex = new int[colorLength][altitudeLength];

		_statAltitudeSumLow = new int[colorLength][altitudeLength];
		_statAltitudeSumHigh = new int[colorLength][altitudeLength];
		_statAltitudeSumColorIndex = new int[colorLength][altitudeLength];

		_statTimeCounterLow = new int[colorLength][timeLength];
		_statTimeCounterHigh = new int[colorLength][timeLength];
		_statTimeCounterColorIndex = new int[colorLength][timeLength];

		_statTimeSumLow = new int[colorLength][timeLength];
		_statTimeSumHigh = new int[colorLength][timeLength];
		_statTimeSumColorIndex = new int[colorLength][timeLength];

		// loop: all tours
		for (int tourIndex = 0; tourIndex < tourDayData.distanceHigh.length; tourIndex++) {

			final int typeColorIndex = tourDayData.typeColorIndex[tourIndex];
			int unitIndex;

			final int diffDistance = (int) ((tourDayData.distanceHigh[tourIndex] - tourDayData.distanceLow[tourIndex] + 500) / 1000);
			final int diffAltitude = (int) (tourDayData.altitudeHigh[tourIndex] - tourDayData.altitudeLow[tourIndex]);
			final int diffTime = (int) (tourDayData.getTimeHighFloat()[tourIndex] - tourDayData.getTimeLowFloat()[tourIndex]);

			unitIndex = createTourStatData(
					diffDistance,
					_statDistanceUnits,
					_statDistanceCounterHigh[typeColorIndex],
					_statDistanceSumHigh[typeColorIndex]);

			_statDistanceCounterColorIndex[typeColorIndex][unitIndex] = typeColorIndex;
			_statDistanceSumColorIndex[typeColorIndex][unitIndex] = typeColorIndex;

			unitIndex = createTourStatData(
					diffAltitude,
					_statAltitudeUnits,
					_statAltitudeCounterHigh[typeColorIndex],
					_statAltitudeSumHigh[typeColorIndex]);

			_statAltitudeCounterColorIndex[typeColorIndex][unitIndex] = typeColorIndex;
			_statAltitudeSumColorIndex[typeColorIndex][unitIndex] = typeColorIndex;

			unitIndex = createTourStatData(
					diffTime,
					_statTimeUnits,
					_statTimeCounterHigh[typeColorIndex],
					_statTimeSumHigh[typeColorIndex]);

			_statTimeCounterColorIndex[typeColorIndex][unitIndex] = typeColorIndex;
			_statTimeSumColorIndex[typeColorIndex][unitIndex] = typeColorIndex;
		}

		updateLowHighValues(_statDistanceCounterLow, _statDistanceCounterHigh);
		updateLowHighValues(_statDistanceSumLow, _statDistanceSumHigh);
		updateLowHighValues(_statAltitudeCounterLow, _statAltitudeCounterHigh);
		updateLowHighValues(_statAltitudeSumLow, _statAltitudeSumHigh);
		updateLowHighValues(_statTimeCounterLow, _statTimeCounterHigh);
		updateLowHighValues(_statTimeSumLow, _statTimeSumHigh);
	}

	/**
	 * create tool tip info
	 */
	private ChartToolTipInfo createToolTipProvider(final int serieIndex, final String toolTipLabel) {

		final String tourTypeName = getTourTypeName(serieIndex, _activeTourTypeFilter);

		final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
		toolTipInfo.setTitle(tourTypeName);
		toolTipInfo.setLabel(toolTipLabel);

		return toolTipInfo;
	}

	private void createToolTipProviderAltitude(final ChartDataModel chartModel) {

		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
			@Override
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {

				String toolTipLabel;
				final StringBuilder infoText = new StringBuilder();

				if (valueIndex == 0) {

					infoText.append(Messages.numbers_info_altitude_down);
					infoText.append(UI.NEW_LINE);
					infoText.append(Messages.numbers_info_altitude_total);

					toolTipLabel = String.format(
							infoText.toString(),
							_statAltitudeUnits[valueIndex],
							UI.UNIT_LABEL_ALTITUDE,
							_statAltitudeCounterHigh[serieIndex][valueIndex],
							//
							_statAltitudeSumHigh[serieIndex][valueIndex],
							UI.UNIT_LABEL_ALTITUDE).toString();

				} else if (valueIndex == _statAltitudeUnits.length - 1) {

					infoText.append(Messages.numbers_info_altitude_up);
					infoText.append(UI.NEW_LINE);
					infoText.append(Messages.numbers_info_altitude_total);

					toolTipLabel = String.format(
							infoText.toString(),
							_statAltitudeUnits[valueIndex - 1],
							UI.UNIT_LABEL_ALTITUDE,
							_statAltitudeCounterHigh[serieIndex][valueIndex],
							//
							_statAltitudeSumHigh[serieIndex][valueIndex],
							UI.UNIT_LABEL_ALTITUDE).toString();
				} else {

					infoText.append(Messages.numbers_info_altitude_between);
					infoText.append(UI.NEW_LINE);
					infoText.append(Messages.numbers_info_altitude_total);

					toolTipLabel = String.format(
							infoText.toString(),
							_statAltitudeUnits[valueIndex - 1],
							_statAltitudeUnits[valueIndex],
							UI.UNIT_LABEL_ALTITUDE,
							_statAltitudeCounterHigh[serieIndex][valueIndex],
							//
							_statAltitudeSumHigh[serieIndex][valueIndex],
							UI.UNIT_LABEL_ALTITUDE).toString();
				}

				return createToolTipProvider(serieIndex, toolTipLabel);
			}
		});
	}

	private void createToolTipProviderDistance(final ChartDataModel chartModel) {

		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
			@Override
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {

				String toolTipLabel;
				final StringBuilder sb = new StringBuilder();

				final int distance = _statDistanceSumHigh[serieIndex][valueIndex];
				final int counter = _statDistanceCounterHigh[serieIndex][valueIndex];

				if (valueIndex == 0) {

					sb.append(Messages.numbers_info_distance_down);
					sb.append(UI.NEW_LINE);
					sb.append(Messages.numbers_info_distance_total);

					toolTipLabel = String.format(
							sb.toString(),
							_statDistanceUnits[valueIndex],
							UI.UNIT_LABEL_DISTANCE,
							counter,
							distance,
							UI.UNIT_LABEL_DISTANCE).toString();

				} else if (valueIndex == _statDistanceUnits.length - 1) {

					sb.append(Messages.numbers_info_distance_up);
					sb.append(UI.NEW_LINE);
					sb.append(Messages.numbers_info_distance_total);

					toolTipLabel = String.format(
							sb.toString(),
							_statDistanceUnits[valueIndex - 1],
							UI.UNIT_LABEL_DISTANCE,
							counter,
							distance,
							UI.UNIT_LABEL_DISTANCE).toString();
				} else {

					sb.append(Messages.numbers_info_distance_between);
					sb.append(UI.NEW_LINE);
					sb.append(Messages.numbers_info_distance_total);

					toolTipLabel = String.format(
							sb.toString(),
							_statDistanceUnits[valueIndex - 1],
							_statDistanceUnits[valueIndex],
							UI.UNIT_LABEL_DISTANCE,
							counter,
							distance,
							UI.UNIT_LABEL_DISTANCE).toString();
				}

				return createToolTipProvider(serieIndex, toolTipLabel);

			}

		});
	}

	private void createToolTipProviderDuration(final ChartDataModel chartModel) {

		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
			@Override
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {

				String toolTipLabel;
				final StringBuilder toolTipFormat = new StringBuilder();

				if (valueIndex == 0) {

					toolTipFormat.append(Messages.numbers_info_time_down);
					toolTipFormat.append(UI.NEW_LINE);
					toolTipFormat.append(Messages.numbers_info_time_total);

					toolTipLabel = String.format(
							toolTipFormat.toString(),
							Util.formatValue(_statTimeUnits[valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
							_statTimeCounterHigh[serieIndex][valueIndex],
							Util.formatValue(
									_statTimeSumHigh[serieIndex][valueIndex],
									ChartDataSerie.AXIS_UNIT_HOUR_MINUTE)).toString();

				} else if (valueIndex == _statTimeUnits.length - 1) {

					toolTipFormat.append(Messages.numbers_info_time_up);
					toolTipFormat.append(UI.NEW_LINE);
					toolTipFormat.append(Messages.numbers_info_time_total);

					toolTipLabel = String.format(
							toolTipFormat.toString(),
							Util.formatValue(_statTimeUnits[valueIndex - 1], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
							_statTimeCounterHigh[serieIndex][valueIndex],
							Util.formatValue(
									_statTimeSumHigh[serieIndex][valueIndex],
									ChartDataSerie.AXIS_UNIT_HOUR_MINUTE)).toString();
				} else {

					toolTipFormat.append(Messages.numbers_info_time_between);
					toolTipFormat.append(UI.NEW_LINE);
					toolTipFormat.append(Messages.numbers_info_time_total);

					toolTipLabel = String.format(
							toolTipFormat.toString(),
							Util.formatValue(_statTimeUnits[valueIndex - 1], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
							Util.formatValue(_statTimeUnits[valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
							_statTimeCounterHigh[serieIndex][valueIndex],
							Util.formatValue(
									_statTimeSumHigh[serieIndex][valueIndex],
									ChartDataSerie.AXIS_UNIT_HOUR_MINUTE)).toString();
				}

				return createToolTipProvider(serieIndex, toolTipLabel);
			}
		});
	}

	/**
	 * calculate the statistic for one tour
	 * 
	 * @param tourValue
	 * @param units
	 * @param counter
	 * @param sum
	 * @return
	 */
	private int createTourStatData(final int tourValue, final int[] units, final int[] counter, final int[] sum) {

		int lastUnit = -1;
		boolean isUnitFound = false;

		// loop: all units
		for (int unitIndex = 0; unitIndex < units.length; unitIndex++) {

			final int unit = units[unitIndex];

			if (lastUnit < 0) {
				// first unit
				if (tourValue < unit) {
					isUnitFound = true;
				}
			} else {
				// second and continuous units
				if (tourValue >= lastUnit && tourValue < unit) {
					isUnitFound = true;
				}
			}

			if (isUnitFound) {
				counter[unitIndex]++;
				sum[unitIndex] += tourValue;
				// colorIndex[unitIndex]=
				return unitIndex;
			} else {
				lastUnit = unit;
			}
		}

		// if the value was not found, add it to the last unit
		counter[units.length - 1]++;
		sum[units.length - 1] += tourValue;

		return units.length - 1;
	}

	private void getPreferences() {

		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		_statDistanceUnits = getPrefUnits(
				store,
				ITourbookPreferences.STAT_DISTANCE_NUMBERS,
				ITourbookPreferences.STAT_DISTANCE_LOW_VALUE,
				ITourbookPreferences.STAT_DISTANCE_INTERVAL,
				ChartDataSerie.AXIS_UNIT_NUMBER);

		_statAltitudeUnits = getPrefUnits(
				store,
				ITourbookPreferences.STAT_ALTITUDE_NUMBERS,
				ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE,
				ITourbookPreferences.STAT_ALTITUDE_INTERVAL,
				ChartDataSerie.AXIS_UNIT_NUMBER);

		_statTimeUnits = getPrefUnits(
				store,
				ITourbookPreferences.STAT_DURATION_NUMBERS,
				ITourbookPreferences.STAT_DURATION_LOW_VALUE,
				ITourbookPreferences.STAT_DURATION_INTERVAL,
				ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);

	}

	/**
	 * create the units from the preference configuration
	 * 
	 * @param store
	 * @param prefInterval
	 * @param prefLowValue
	 * @param prefNumbers
	 * @param unitType
	 * @return
	 */
	private int[] getPrefUnits(	final IPreferenceStore store,
								final String prefNumbers,
								final String prefLowValue,
								final String prefInterval,
								final int unitType) {

		final int lowValue = store.getInt(prefLowValue);
		final int interval = store.getInt(prefInterval);
		final int numbers = store.getInt(prefNumbers);

		final int[] units = new int[numbers];

		for (int number = 0; number < numbers; number++) {
			if (unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE) {
				// adjust the values to minutes
				units[number] = (lowValue * 60) + (interval * number * 60);
			} else {
				units[number] = lowValue + (interval * number);
			}
		}

		return units;
	}

	public void preferencesHasChanged() {
		updateStatistic(new StatisticContext(_activePerson, _activeTourTypeFilter, _currentYear, 1, false));
	}

	private void resetMinMaxKeeper() {
		if (_isSynchScaleEnabled == false) {
			_minMaxKeeperStatAltitudeCounter.resetMinMax();
			_minMaxKeeperStatAltitudeSum.resetMinMax();
			_minMaxKeeperStatDistanceCounter.resetMinMax();
			_minMaxKeeperStatDistanceSum.resetMinMax();
			_minMaxKeeperStatDurationCounter.resetMinMax();
			_minMaxKeeperStatDurationSum.resetMinMax();
		}
	}

	@Override
	public void resetSelection() {}

	@Override
	public void setSynchScale(final boolean isSynchScaleEnabled) {
		_isSynchScaleEnabled = isSynchScaleEnabled;
	}

	private void updateChartAltitude(	final Chart statAltitudeChart,
										final BarChartMinMaxKeeper statAltitudeMinMaxKeeper,
										final int[][] lowValues,
										final int[][] highValues,
										final int[][] colorIndex,
										final String unit,
										final String title) {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartType.BAR);

		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(
				net.tourbook.common.util.Util.convertIntToDouble(_statAltitudeUnits));
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_NUMBER);
		xData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
		chartDataModel.setXData(xData);

		// y-axis: altitude
		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartType.BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				net.tourbook.common.util.Util.convertIntToFloat(lowValues),
				net.tourbook.common.util.Util.convertIntToFloat(highValues));
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setUnitLabel(unit);
		yData.setAllValueColors(0);
		yData.setYTitle(title);
		yData.setVisibleMinValue(0);
		chartDataModel.addYData(yData);

		StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE);
		StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE, _activeTourTypeFilter);
		yData.setColorIndex(colorIndex);

		createToolTipProviderAltitude(chartDataModel);

		if (_isSynchScaleEnabled) {
			statAltitudeMinMaxKeeper.setMinMaxValues(chartDataModel);
		}

		// set grid size
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		statAltitudeChart.setGrid(
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE),
				prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES),
				prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES));

		// show the new data in the chart
		statAltitudeChart.updateChart(chartDataModel, true);
	}

	/**
	 * @param statDistanceChart
	 * @param statDistanceMinMaxKeeper
	 * @param highValues
	 * @param lowValues
	 * @param statDistanceColorIndex
	 * @param unit
	 * @param title
	 * @param valueDivisor
	 */
	private void updateChartDistance(	final Chart statDistanceChart,
										final BarChartMinMaxKeeper statDistanceMinMaxKeeper,
										final int[][] lowValues,
										final int[][] highValues,
										final int[][] colorIndex,
										final String unit,
										final String title,
										final int valueDivisor) {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartType.BAR);

		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(
				net.tourbook.common.util.Util.convertIntToDouble(_statDistanceUnits));
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_NUMBER);
		xData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
		chartDataModel.setXData(xData);

		// y-axis: distance
		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartType.BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				net.tourbook.common.util.Util.convertIntToFloat(lowValues),
				net.tourbook.common.util.Util.convertIntToFloat(highValues));
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setUnitLabel(unit);
		yData.setAllValueColors(0);
		yData.setYTitle(title);
		yData.setVisibleMinValue(0);
		yData.setValueDivisor(valueDivisor);
		chartDataModel.addYData(yData);
		StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE);
		StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE, _activeTourTypeFilter);
		yData.setColorIndex(colorIndex);

		createToolTipProviderDistance(chartDataModel);

		if (_isSynchScaleEnabled) {
			statDistanceMinMaxKeeper.setMinMaxValues(chartDataModel);
		}

		// set grid size
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		statDistanceChart.setGrid(
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE),
				prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES),
				prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES));

		// show the new data fDataModel in the chart
		statDistanceChart.updateChart(chartDataModel, true);
	}

	private void updateCharts() {

		updateChartDistance(
				_chartDistanceCounter,
				_minMaxKeeperStatDistanceCounter,
				_statDistanceCounterLow,
				_statDistanceCounterHigh,
				_statDistanceCounterColorIndex,
				Messages.NUMBERS_UNIT,
				Messages.LABEL_GRAPH_DISTANCE,
				1);

		updateChartDistance(
				_chartDistanceSum,
				_minMaxKeeperStatDistanceSum,
				_statDistanceSumLow,
				_statDistanceSumHigh,
				_statDistanceSumColorIndex,
				UI.UNIT_LABEL_DISTANCE,
				Messages.LABEL_GRAPH_DISTANCE,
				1);

		updateChartAltitude(
				_chartAltitudeCounter,
				_minMaxKeeperStatAltitudeCounter,
				_statAltitudeCounterLow,
				_statAltitudeCounterHigh,
				_statAltitudeCounterColorIndex,
				Messages.NUMBERS_UNIT,
				Messages.LABEL_GRAPH_ALTITUDE);

		updateChartAltitude(
				_chartAltitudeSum,
				_minMaxKeeperStatAltitudeSum,
				_statAltitudeSumLow,
				_statAltitudeSumHigh,
				_statAltitudeSumColorIndex,
				UI.UNIT_LABEL_ALTITUDE,
				Messages.LABEL_GRAPH_ALTITUDE);

		updateChartTime(
				_chartDurationCounter,
				_minMaxKeeperStatDurationCounter,
				_statTimeCounterLow,
				_statTimeCounterHigh,
				_statTimeCounterColorIndex,
				ChartDataXSerie.AXIS_UNIT_NUMBER,
				Messages.NUMBERS_UNIT,
				Messages.LABEL_GRAPH_TIME);

		updateChartTime(
				_chartDurationSum,
				_minMaxKeeperStatDurationSum,
				_statTimeSumLow,
				_statTimeSumHigh,
				_statTimeSumColorIndex,
				ChartDataXSerie.AXIS_UNIT_HOUR_MINUTE,
				Messages.LABEL_GRAPH_TIME_UNIT,
				Messages.LABEL_GRAPH_TIME);
	}

	private void updateChartTime(	final Chart statDurationChart,
									final BarChartMinMaxKeeper statDurationMinMaxKeeper,
									final int[][] lowValues,
									final int[][] highValues,
									final int[][] colorIndex,
									final int yUnit,
									final String unit,
									final String title) {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartType.BAR);

		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(
				net.tourbook.common.util.Util.convertIntToDouble(_statTimeUnits));
		xData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		xData.setUnitLabel(UI.UNIT_LABEL_TIME);
		chartDataModel.setXData(xData);

		// y-axis: altitude
		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartType.BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				net.tourbook.common.util.Util.convertIntToFloat(lowValues),
				net.tourbook.common.util.Util.convertIntToFloat(highValues));
		yData.setAxisUnit(yUnit);
		yData.setUnitLabel(unit);
		yData.setAllValueColors(0);
		yData.setYTitle(title);
		yData.setVisibleMinValue(0);
		chartDataModel.addYData(yData);

		StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_TIME);
		StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TIME, _activeTourTypeFilter);
		yData.setColorIndex(colorIndex);

		createToolTipProviderDuration(chartDataModel);

		if (_isSynchScaleEnabled) {
			statDurationMinMaxKeeper.setMinMaxValues(chartDataModel);
		}

		// set grid size
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		statDurationChart.setGrid(
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE),
				prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES),
				prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES));

		// show the new data data model in the chart
		statDurationChart.updateChart(chartDataModel, true);
	}

	/**
	 * update the low and high values so they are stacked on each other
	 * 
	 * @param lowValues
	 * @param highValues
	 */
	private void updateLowHighValues(final int[][] lowValues, final int[][] highValues) {

		for (int colorIndex = 0; colorIndex < highValues.length; colorIndex++) {
			if (colorIndex > 0) {
				for (int valueIndex = 0; valueIndex < highValues[0].length; valueIndex++) {

					if (highValues[colorIndex][valueIndex] > 0) {

						final int previousHighValue = highValues[colorIndex - 1][valueIndex];

						highValues[colorIndex][valueIndex] += previousHighValue;
					}
				}
			}
		}
	}

	//	public void refreshStatistic(	final TourPerson person,
//									final TourTypeFilter typeId,
//									final int year,
//									final int numberOfYears,
//									final boolean refreshData) {
	public void updateStatistic(final StatisticContext statContext) {

		_activePerson = statContext.appPerson;
		_activeTourTypeFilter = statContext.appTourTypeFilter;
		_currentYear = statContext.statYoungestYear;

		_tourDayData = DataProviderTourDay.getInstance().getDayData(
				statContext.appPerson,
				statContext.appTourTypeFilter,
				statContext.statYoungestYear,
				statContext.statNumberOfYears,
				isDataDirtyWithReset() || statContext.isRefreshData);

		// reset min/max values
		if (_isSynchScaleEnabled == false && statContext.isRefreshData) {
			resetMinMaxKeeper();
		}

		// hide actions from other statistics
		final IToolBarManager tbm = _viewSite.getActionBars().getToolBarManager();
		tbm.removeAll();
		tbm.update(true);

		createStatisticData(_tourDayData);
		updateCharts();

	}

	@Override
	public void updateToolBar(final boolean refreshToolbar) {}
}
