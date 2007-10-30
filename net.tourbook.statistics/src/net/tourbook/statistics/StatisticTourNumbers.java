/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
import java.util.Formatter;

import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartUtil;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.colors.GraphColors;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public class StatisticTourNumbers extends YearStatistic {

	private Composite					fStatisticPage;

	private Chart						fChartDistanceCounter;
	private Chart						fChartDistanceSum;
	private Chart						fChartDurationCounter;
	private Chart						fChartDurationSum;
	private Chart						fChartAltitudeCounter;
	private Chart						fChartAltitudeSum;

	private final BarChartMinMaxKeeper	fMinMaxKeeperStatAltitudeCounter	= new BarChartMinMaxKeeper();
	private final BarChartMinMaxKeeper	fMinMaxKeeperStatAltitudeSum		= new BarChartMinMaxKeeper();
	private final BarChartMinMaxKeeper	fMinMaxKeeperStatDistanceCounter	= new BarChartMinMaxKeeper();
	private final BarChartMinMaxKeeper	fMinMaxKeeperStatDistanceSum		= new BarChartMinMaxKeeper();
	private final BarChartMinMaxKeeper	fMinMaxKeeperStatDurationCounter	= new BarChartMinMaxKeeper();
	private final BarChartMinMaxKeeper	fMinMaxKeeperStatDurationSum		= new BarChartMinMaxKeeper();

	private int[]						fStatDistanceUnits;
	private int[]						fStatAltitudeUnits;
	private int[]						fStatTimeUnits;

	private int[][]						fStatDistanceCounterLow;
	private int[][]						fStatDistanceCounterHigh;
	private int[][]						fStatDistanceCounterColorIndex;

	private int[][]						fStatDistanceSumLow;
	private int[][]						fStatDistanceSumHigh;
	private int[][]						fStatDistanceSumColorIndex;

	private int[][]						fStatAltitudeCounterLow;
	private int[][]						fStatAltitudeCounterHigh;
	private int[][]						fStatAltitudeCounterColorIndex;

	private int[][]						fStatAltitudeSumLow;
	private int[][]						fStatAltitudeSumHigh;
	private int[][]						fStatAltitudeSumColorIndex;

	private int[][]						fStatTimeCounterLow;
	private int[][]						fStatTimeCounterHigh;
	private int[][]						fStatTimeCounterColorIndex;

	private int[][]						fStatTimeSumLow;
	private int[][]						fStatTimeSumHigh;
	private int[][]						fStatTimeSumColorIndex;

	private IPropertyChangeListener		fPrefChangeListener;

	private int							fCurrentYear;
	private TourPerson					fActivePerson;
	protected TourTypeFilter			fActiveTourTypeFilter;

	private boolean						fIsSynchScaleEnabled;

	private TourDataTour				fTourDataTour;

	private IViewSite					fViewSite;

	public StatisticTourNumbers() {}

	void addPrefListener(Composite container) {

		// create pref listener
		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(Preferences.PropertyChangeEvent event) {
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
					refreshStatistic(fActivePerson, fActiveTourTypeFilter, fCurrentYear, false);
				}
			}
		};

		// add pref listener
		TourbookPlugin.getDefault()
				.getPluginPreferences()
				.addPropertyChangeListener(fPrefChangeListener);

		// remove pref listener
		container.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				TourbookPlugin.getDefault()
						.getPluginPreferences()
						.removePropertyChangeListener(fPrefChangeListener);
			}
		});
	}

	@Override
	public void activateActions(IWorkbenchPartSite partSite) {}

	@Override
	public void deactivateActions(IWorkbenchPartSite partSite) {}

	public boolean canTourBeVisible() {
		return false;
	}

	@Override
	public void resetSelection() {}

	private void createAltitudeStatisticProvider(ChartDataModel chartModel) {

		chartModel.setCustomData(ChartDataModel.BAR_INFO_PROVIDER, new IChartInfoProvider() {
			public String getInfo(int serieIndex, int valueIndex) {

				String barInfo;

				if (valueIndex == 0) {
					barInfo = new Formatter().format(Messages.NUMBERS_ALTITUDE_DOWN
							+ Messages.NUMBERS_ALTITUDE_UNIT,
							fStatAltitudeUnits[valueIndex],
							fStatAltitudeCounterHigh[serieIndex][valueIndex],
							fStatAltitudeSumHigh[serieIndex][valueIndex]).toString();

				} else if (valueIndex == fStatAltitudeUnits.length - 1) {

					barInfo = new Formatter().format(Messages.NUMBERS_ALTITUDE_UP
							+ Messages.NUMBERS_ALTITUDE_UNIT,
							fStatAltitudeUnits[valueIndex - 1],
							fStatAltitudeCounterHigh[serieIndex][valueIndex],
							fStatAltitudeSumHigh[serieIndex][valueIndex]).toString();
				} else {

					barInfo = new Formatter().format(Messages.NUMBERS_ALTITUDE_BETWEEN
							+ Messages.NUMBERS_ALTITUDE_UNIT,
							fStatAltitudeUnits[valueIndex - 1],
							fStatAltitudeUnits[valueIndex],
							fStatAltitudeCounterHigh[serieIndex][valueIndex],
							fStatAltitudeSumHigh[serieIndex][valueIndex]).toString();
				}

				return barInfo;
			}
		});
	}

	@Override
	public void createControl(	Composite parent,
								IViewSite viewSite,
								final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		fViewSite = viewSite;

		// create statistic page
		fStatisticPage = new Composite(parent, SWT.BORDER | SWT.FLAT);
		fStatisticPage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// remove colored border
		fStatisticPage.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		GridLayout gl = new GridLayout(2, true);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		fStatisticPage.setLayout(gl);

		fChartDistanceCounter = new Chart(fStatisticPage, SWT.NONE);
		fChartDistanceSum = new Chart(fStatisticPage, SWT.NONE);

		fChartAltitudeCounter = new Chart(fStatisticPage, SWT.NONE);
		fChartAltitudeSum = new Chart(fStatisticPage, SWT.NONE);

		fChartDurationCounter = new Chart(fStatisticPage, SWT.NONE);
		fChartDurationSum = new Chart(fStatisticPage, SWT.NONE);

		fChartDistanceCounter.setToolBarManager(viewSite.getActionBars().getToolBarManager(), true);

		addPrefListener(parent);
		getPreferences();
	}

	private void createDistanceStatisticProvider(ChartDataModel chartModel) {

		chartModel.setCustomData(ChartDataModel.BAR_INFO_PROVIDER, new IChartInfoProvider() {

			public String getInfo(int serieIndex, int valueIndex) {

				String barInfo;

				if (valueIndex == 0) {
					barInfo = new Formatter().format(Messages.NUMBERS_DISTANCE_DOWN
							+ Messages.NUMBERS_DISTANCE_UNIT,
							fStatDistanceUnits[valueIndex],
							fStatDistanceCounterHigh[serieIndex][valueIndex],
							fStatDistanceSumHigh[serieIndex][valueIndex]).toString();

				} else if (valueIndex == fStatDistanceUnits.length - 1) {

					barInfo = new Formatter().format(Messages.NUMBERS_DISTANCE_UP
							+ Messages.NUMBERS_DISTANCE_UNIT,
							fStatDistanceUnits[valueIndex - 1],
							fStatDistanceCounterHigh[serieIndex][valueIndex],
							fStatDistanceSumHigh[serieIndex][valueIndex]).toString();
				} else {

					barInfo = new Formatter().format(Messages.NUMBERS_DISTANCE_BETWEEN
							+ Messages.NUMBERS_DISTANCE_UNIT,
							fStatDistanceUnits[valueIndex - 1],
							fStatDistanceUnits[valueIndex],
							fStatDistanceCounterHigh[serieIndex][valueIndex],
							fStatDistanceSumHigh[serieIndex][valueIndex]).toString();
				}

				return barInfo;
			}
		});
	}

	private void createDurationStatisticProvider(ChartDataModel chartModel) {

		chartModel.setCustomData(ChartDataModel.BAR_INFO_PROVIDER, new IChartInfoProvider() {
			public String getInfo(int serieIndex, int valueIndex) {

				String barInfo;

				if (valueIndex == 0) {
					barInfo = new Formatter().format(Messages.NUMBERS_TIME_DOWN
							+ Messages.NUMBERS_TIME_UNIT,
							ChartUtil.formatValue(fStatTimeUnits[valueIndex],
									ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
							fStatTimeCounterHigh[serieIndex][valueIndex],
							ChartUtil.formatValue(fStatTimeSumHigh[serieIndex][valueIndex],
									ChartDataSerie.AXIS_UNIT_HOUR_MINUTE)).toString();

				} else if (valueIndex == fStatTimeUnits.length - 1) {
					barInfo = new Formatter().format(Messages.NUMBERS_TIME_UP
							+ Messages.NUMBERS_TIME_UNIT,
							ChartUtil.formatValue(fStatTimeUnits[valueIndex - 1],
									ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),

							fStatTimeCounterHigh[serieIndex][valueIndex],

							ChartUtil.formatValue(fStatTimeSumHigh[serieIndex][valueIndex],
									ChartDataSerie.AXIS_UNIT_HOUR_MINUTE)).toString();
				} else {
					barInfo = new Formatter().format(Messages.NUMBERS_TIME_BETWEEN
							+ Messages.NUMBERS_TIME_UNIT,
							ChartUtil.formatValue(fStatTimeUnits[valueIndex - 1],
									ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
							ChartUtil.formatValue(fStatTimeUnits[valueIndex],
									ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
							fStatTimeCounterHigh[serieIndex][valueIndex],
							ChartUtil.formatValue(fStatTimeSumHigh[serieIndex][valueIndex],
									ChartDataSerie.AXIS_UNIT_HOUR_MINUTE)).toString();
				}

				return barInfo;
			}
		});
	}

	/**
	 * calculate data for all statistics
	 * 
	 * @param tourDataTour
	 */
	private void createStatisticData(TourDataTour tourDataTour) {

		ArrayList<TourType> tourTypeList = TourDatabase.getTourTypes();
		int colorLength = tourTypeList.size();

		int distanceLength = fStatDistanceUnits.length;
		int altitudeLength = fStatAltitudeUnits.length;
		int timeLength = fStatTimeUnits.length;

		fStatDistanceCounterLow = new int[colorLength][distanceLength];
		fStatDistanceCounterHigh = new int[colorLength][distanceLength];
		fStatDistanceCounterColorIndex = new int[colorLength][distanceLength];

		fStatDistanceSumLow = new int[colorLength][distanceLength];
		fStatDistanceSumHigh = new int[colorLength][distanceLength];
		fStatDistanceSumColorIndex = new int[colorLength][distanceLength];

		fStatAltitudeCounterLow = new int[colorLength][altitudeLength];
		fStatAltitudeCounterHigh = new int[colorLength][altitudeLength];
		fStatAltitudeCounterColorIndex = new int[colorLength][altitudeLength];

		fStatAltitudeSumLow = new int[colorLength][altitudeLength];
		fStatAltitudeSumHigh = new int[colorLength][altitudeLength];
		fStatAltitudeSumColorIndex = new int[colorLength][altitudeLength];

		fStatTimeCounterLow = new int[colorLength][timeLength];
		fStatTimeCounterHigh = new int[colorLength][timeLength];
		fStatTimeCounterColorIndex = new int[colorLength][timeLength];

		fStatTimeSumLow = new int[colorLength][timeLength];
		fStatTimeSumHigh = new int[colorLength][timeLength];
		fStatTimeSumColorIndex = new int[colorLength][timeLength];

		// loop: all tours
		for (int tourIndex = 0; tourIndex < tourDataTour.fDistanceHigh.length; tourIndex++) {

			int tourColorIndex = tourDataTour.fTypeColorIndex[tourIndex];
			int unitIndex;

			unitIndex = createTourStatData(tourDataTour.fDistanceHigh[tourIndex]
					- tourDataTour.fDistanceLow[tourIndex],
					fStatDistanceUnits,
					fStatDistanceCounterHigh[tourColorIndex],
					fStatDistanceSumHigh[tourColorIndex]);

			fStatDistanceCounterColorIndex[tourColorIndex][unitIndex] = tourColorIndex;
			fStatDistanceSumColorIndex[tourColorIndex][unitIndex] = tourColorIndex;

			unitIndex = createTourStatData(tourDataTour.fAltitudeHigh[tourIndex]
					- tourDataTour.fAltitudeLow[tourIndex],
					fStatAltitudeUnits,
					fStatAltitudeCounterHigh[tourColorIndex],
					fStatAltitudeSumHigh[tourColorIndex]);

			fStatAltitudeCounterColorIndex[tourColorIndex][unitIndex] = tourColorIndex;
			fStatAltitudeSumColorIndex[tourColorIndex][unitIndex] = tourColorIndex;

			unitIndex = createTourStatData(tourDataTour.fTimeHigh[tourIndex]
					- tourDataTour.fTimeLow[tourIndex],
					fStatTimeUnits,
					fStatTimeCounterHigh[tourColorIndex],
					fStatTimeSumHigh[tourColorIndex]);

			fStatTimeCounterColorIndex[tourColorIndex][unitIndex] = tourColorIndex;
			fStatTimeSumColorIndex[tourColorIndex][unitIndex] = tourColorIndex;
		}

		updateLowHighValues(fStatDistanceCounterLow, fStatDistanceCounterHigh);
		updateLowHighValues(fStatDistanceSumLow, fStatDistanceSumHigh);
		updateLowHighValues(fStatAltitudeCounterLow, fStatAltitudeCounterHigh);
		updateLowHighValues(fStatAltitudeSumLow, fStatAltitudeSumHigh);
		updateLowHighValues(fStatTimeCounterLow, fStatTimeCounterHigh);
		updateLowHighValues(fStatTimeSumLow, fStatTimeSumHigh);
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
	private int createTourStatData(int tourValue, int[] units, int[] counter, int[] sum) {

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

		fStatDistanceUnits = getPrefUnits(store,
				ITourbookPreferences.STAT_DISTANCE_NUMBERS,
				ITourbookPreferences.STAT_DISTANCE_LOW_VALUE,
				ITourbookPreferences.STAT_DISTANCE_INTERVAL,
				ChartDataSerie.AXIS_UNIT_NUMBER);

		fStatAltitudeUnits = getPrefUnits(store,
				ITourbookPreferences.STAT_ALTITUDE_NUMBERS,
				ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE,
				ITourbookPreferences.STAT_ALTITUDE_INTERVAL,
				ChartDataSerie.AXIS_UNIT_NUMBER);

		fStatTimeUnits = getPrefUnits(store,
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
	private int[] getPrefUnits(	IPreferenceStore store,
								String prefNumbers,
								String prefLowValue,
								String prefInterval,
								int unitType) {

		int lowValue = store.getInt(prefLowValue);
		int interval = store.getInt(prefInterval);
		int numbers = store.getInt(prefNumbers);

		int[] units = new int[numbers];

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

	public void prefColorChanged() {
		refreshStatistic(fActivePerson, fActiveTourTypeFilter, fCurrentYear, false);
	}

	public void refreshStatistic(	TourPerson person,
									TourTypeFilter typeId,
									int year,
									boolean refreshData) {

		fActivePerson = person;
		fActiveTourTypeFilter = typeId;
		fCurrentYear = year;

		fTourDataTour = ProviderTourDay.getInstance().getDayData(person,
				typeId,
				year,
				isDataDirtyWithReset() || refreshData);

		// reset min/max values
		if (fIsSynchScaleEnabled == false && refreshData) {
			resetMinMaxKeeper();
		}

		// hide actions from other statistics
		final IToolBarManager tbm = fViewSite.getActionBars().getToolBarManager();
		tbm.removeAll();
		tbm.update(true);

		createStatisticData(fTourDataTour);
		updateCharts();

	}

	private void resetMinMaxKeeper() {
		if (fIsSynchScaleEnabled == false) {
			fMinMaxKeeperStatAltitudeCounter.resetMinMax();
			fMinMaxKeeperStatAltitudeSum.resetMinMax();
			fMinMaxKeeperStatDistanceCounter.resetMinMax();
			fMinMaxKeeperStatDistanceSum.resetMinMax();
			fMinMaxKeeperStatDurationCounter.resetMinMax();
			fMinMaxKeeperStatDurationSum.resetMinMax();
		}
	}

	@Override
	public void setSynchScale(boolean isSynchScaleEnabled) {
		fIsSynchScaleEnabled = isSynchScaleEnabled;
	}

	private void updateChartAltitude(	Chart statAltitudeChart,
										BarChartMinMaxKeeper statAltitudeMinMaxKeeper,
										int[][] lowValues,
										int[][] highValues,
										int[][] colorIndex,
										String unit,
										String title) {

		ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		// set the x-axis
		ChartDataXSerie xData = new ChartDataXSerie(fStatAltitudeUnits);
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_NUMBER);
		chartDataModel.setXData(xData);

		// y-axis: altitude
		ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				lowValues,
				highValues);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setAllValueColors(0);
		yData.setUnitLabel(unit);
		yData.setYTitle(title);
		yData.setVisibleMinValue(0);
		chartDataModel.addYData(yData);

		StatisticServices.setTourTypeColors(yData, GraphColors.PREF_GRAPH_ALTITUDE);
		yData.setColorIndex(colorIndex);

		createAltitudeStatisticProvider(chartDataModel);

		if (fIsSynchScaleEnabled) {
			statAltitudeMinMaxKeeper.setMinMaxValues(chartDataModel);
		}

		// show the new data in the chart
		statAltitudeChart.updateChart(chartDataModel);
	}

	/**
	 * @param statDistanceChart
	 * @param statDistanceMinMaxKeeper
	 * @param highValues
	 * @param lowValues
	 * @param statDistanceColorIndex
	 * @param unit
	 * @param title
	 */
	private void updateChartDistance(	Chart statDistanceChart,
										BarChartMinMaxKeeper statDistanceMinMaxKeeper,
										int[][] lowValues,
										int[][] highValues,
										int[][] colorIndex,
										String unit,
										String title) {

		ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		// set the x-axis
		ChartDataXSerie xData = new ChartDataXSerie(fStatDistanceUnits);
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_NUMBER);
		chartDataModel.setXData(xData);

		// y-axis: distance
		ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				lowValues,
				highValues);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setAllValueColors(0);
		yData.setUnitLabel(unit);
		yData.setYTitle(title);
		yData.setVisibleMinValue(0);
		chartDataModel.addYData(yData);

		StatisticServices.setTourTypeColors(yData, GraphColors.PREF_GRAPH_DISTANCE);
		yData.setColorIndex(colorIndex);

		createDistanceStatisticProvider(chartDataModel);

		if (fIsSynchScaleEnabled) {
			statDistanceMinMaxKeeper.setMinMaxValues(chartDataModel);
		}

		// show the new data fDataModel in the chart
		statDistanceChart.updateChart(chartDataModel);
	}

	private void updateCharts() {

		updateChartDistance(fChartDistanceCounter,
				fMinMaxKeeperStatDistanceCounter,
				fStatDistanceCounterLow,
				fStatDistanceCounterHigh,
				fStatDistanceCounterColorIndex,
				Messages.NUMBERS_UNIT,
				Messages.LABEL_GRAPH_DISTANCE);

		updateChartDistance(fChartDistanceSum,
				fMinMaxKeeperStatDistanceSum,
				fStatDistanceSumLow,
				fStatDistanceSumHigh,
				fStatDistanceSumColorIndex,
				Messages.LABEL_GRAPH_DISTANCE_UNIT,
				Messages.LABEL_GRAPH_DISTANCE);

		updateChartAltitude(fChartAltitudeCounter,
				fMinMaxKeeperStatAltitudeCounter,
				fStatAltitudeCounterLow,
				fStatAltitudeCounterHigh,
				fStatAltitudeCounterColorIndex,
				Messages.NUMBERS_UNIT,
				Messages.LABEL_GRAPH_ALTITUDE);

		updateChartAltitude(fChartAltitudeSum,
				fMinMaxKeeperStatAltitudeSum,
				fStatAltitudeSumLow,
				fStatAltitudeSumHigh,
				fStatAltitudeSumColorIndex,
				Messages.LABEL_GRAPH_ALTITUDE_UNIT,
				Messages.LABEL_GRAPH_ALTITUDE);

		updateChartTime(fChartDurationCounter,
				fMinMaxKeeperStatDurationCounter,
				fStatTimeCounterLow,
				fStatTimeCounterHigh,
				fStatTimeCounterColorIndex,
				ChartDataXSerie.AXIS_UNIT_NUMBER,
				Messages.NUMBERS_UNIT,
				Messages.LABEL_GRAPH_TIME);

		updateChartTime(fChartDurationSum,
				fMinMaxKeeperStatDurationSum,
				fStatTimeSumLow,
				fStatTimeSumHigh,
				fStatTimeSumColorIndex,
				ChartDataXSerie.AXIS_UNIT_HOUR_MINUTE,
				Messages.LABEL_GRAPH_TIME_UNIT,
				Messages.LABEL_GRAPH_TIME);
	}

	private void updateChartTime(	Chart statDurationChart,
									BarChartMinMaxKeeper statDurationMinMaxKeeper,
									int[][] lowValues,
									int[][] highValues,
									int[][] colorIndex,
									int yUnit,
									String unit,
									String title) {

		ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		// set the x-axis
		ChartDataXSerie xData = new ChartDataXSerie(fStatTimeUnits);
		xData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		chartDataModel.setXData(xData);

		// y-axis: altitude
		ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				lowValues,
				highValues);
		yData.setAxisUnit(yUnit);
		yData.setAllValueColors(0);
		yData.setUnitLabel(unit);
		yData.setYTitle(title);
		yData.setVisibleMinValue(0);
		chartDataModel.addYData(yData);

		StatisticServices.setTourTypeColors(yData, GraphColors.PREF_GRAPH_TIME);
		yData.setColorIndex(colorIndex);

		createDurationStatisticProvider(chartDataModel);

		if (fIsSynchScaleEnabled) {
			statDurationMinMaxKeeper.setMinMaxValues(chartDataModel);
		}

		// show the new data fDataModel in the chart
		statDurationChart.updateChart(chartDataModel);
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

						int previousHighValue = highValues[colorIndex - 1][valueIndex];

						highValues[colorIndex][valueIndex] += previousHighValue;
					}
				}
			}
		}
	}

	@Override
	public void updateToolBar(final boolean refreshToolbar) {}
}
