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

import java.time.ZonedDateTime;
import java.util.ArrayList;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartStatisticSegments;
import net.tourbook.chart.ChartToolTipInfo;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.chart.MinMaxKeeper_YData;
import net.tourbook.chart.SelectionBarChart;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.IToolTipHideListener;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistic.StatisticContext;
import net.tourbook.statistic.TourbookStatistic;
import net.tourbook.statistics.IBarSelectionProvider;
import net.tourbook.statistics.Messages;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.statistics.StatisticTourToolTip;
import net.tourbook.statistics.TourChartContextProvider;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourInfoIconToolTipProvider;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ChartOptions_Grid;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.action.ActionEditQuick;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;

public abstract class StatisticDay extends TourbookStatistic implements IBarSelectionProvider, ITourProvider {

	private static final String			TOUR_TOOLTIP_FORMAT_DATE_WEEK_TIME	= net.tourbook.ui.Messages.Tour_Tooltip_Format_DateWeekTime;

	private TourTypeFilter				_activeTourTypeFilter;
	private TourPerson					_activePerson;

	private long						_selectedTourId						= -1;

	private int							_currentYear;
	private int							_numberOfYears;

	private Chart						_chart;

	private final MinMaxKeeper_YData	_minMaxKeeper						= new MinMaxKeeper_YData();
	private TourData_Day				_tourDayData;

	private boolean						_isSynchScaleEnabled;

	private ITourEventListener			_tourPropertyListener;
	private StatisticTourToolTip		_tourToolTip;

	private TourInfoIconToolTipProvider	_tourInfoToolTipProvider			= new TourInfoIconToolTipProvider();

	private void addTourPropertyListener() {

		_tourPropertyListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId propertyId, final Object propertyData) {

				if (propertyId == TourEventId.TOUR_CHANGED && propertyData instanceof TourEvent) {

					// check if a tour was modified
					final ArrayList<TourData> modifiedTours = ((TourEvent) propertyData).getModifiedTours();
					if (modifiedTours != null) {

						for (final TourData modifiedTourData : modifiedTours) {

							final long modifiedTourId = modifiedTourData.getTourId();

							final long[] tourIds = _tourDayData.tourIds;
							for (int tourIdIndex = 0; tourIdIndex < tourIds.length; tourIdIndex++) {

								final long tourId = tourIds[tourIdIndex];

								if (tourId == modifiedTourId) {

									// set new tour title
									_tourDayData.tourTitle.set(tourIdIndex, modifiedTourData.getTourTitle());

									break;
								}
							}
						}
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourPropertyListener);
	}

	/**
	 * create segments for the chart
	 */
	ChartStatisticSegments createChartSegments(final TourData_Day tourTimeData) {

		final double[] segmentStart = new double[_numberOfYears];
		final double[] segmentEnd = new double[_numberOfYears];
		final String[] segmentTitle = new String[_numberOfYears];

		final int[] allYearDays = tourTimeData.yearDays;
		final int oldestYear = _currentYear - _numberOfYears + 1;
		int yearDaysSum = 0;

		// create segments for each year
		for (int yearIndex = 0; yearIndex < allYearDays.length; yearIndex++) {

			final int yearDays = allYearDays[yearIndex];

			segmentStart[yearIndex] = yearDaysSum;
			segmentEnd[yearIndex] = yearDaysSum + yearDays - 1;
			segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

			yearDaysSum += yearDays;
		}

		final ChartStatisticSegments chartSegments = new ChartStatisticSegments();
		chartSegments.segmentStartValue = segmentStart;
		chartSegments.segmentEndValue = segmentEnd;
		chartSegments.segmentTitle = segmentTitle;

		chartSegments.years = tourTimeData.years;
		chartSegments.yearDays = tourTimeData.yearDays;
		chartSegments.allValues = tourTimeData.allDaysInAllYears;

		return chartSegments;
	}

	@Override
	public void createStatisticUI(final Composite parent, final IViewSite viewSite) {

		// create statistic chart
		_chart = new Chart(parent, SWT.FLAT);
		_chart.setShowZoomActions(true);
		_chart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);

		// set tour info icon into the left axis
		_tourToolTip = new StatisticTourToolTip(_chart.getToolTipControl());
		_tourToolTip.addToolTipProvider(_tourInfoToolTipProvider);
		_tourToolTip.addHideListener(new IToolTipHideListener() {
			@Override
			public void afterHideToolTip(final Event event) {
				// hide hovered image
				_chart.getToolTipControl().afterHideToolTip(event);
			}
		});

		_chart.setTourInfoIconToolTipProvider(_tourInfoToolTipProvider);
		_tourInfoToolTipProvider.setActionsEnabled(true);

		_chart.addBarSelectionListener(new IBarSelectionListener() {
			@Override
			public void selectionChanged(final int serieIndex, final int valueIndex) {
				if (_tourDayData.typeIds.length > 0) {

					_selectedTourId = _tourDayData.tourIds[valueIndex];
					_tourInfoToolTipProvider.setTourId(_selectedTourId);

					// this view can be inactive -> selection is not fired with the SelectionProvider interface
					TourManager.fireEventWithCustomData(
							TourEventId.TOUR_SELECTION,
							new SelectionTourId(_selectedTourId),
							viewSite.getPart());
				}
			}
		});

		/*
		 * open tour with double click on the tour bar
		 */
		_chart.addDoubleClickListener(new IBarSelectionListener() {
			@Override
			public void selectionChanged(final int serieIndex, final int valueIndex) {

				_selectedTourId = _tourDayData.tourIds[valueIndex];
				_tourInfoToolTipProvider.setTourId(_selectedTourId);

				ActionEditQuick.doAction(StatisticDay.this);
			}
		});

		/*
		 * open tour with Enter key
		 */
		_chart.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(final TraverseEvent event) {

				if (event.detail == SWT.TRAVERSE_RETURN) {
					final ISelection selection = _chart.getSelection();
					if (selection instanceof SelectionBarChart) {
						final SelectionBarChart barChartSelection = (SelectionBarChart) selection;

						if (barChartSelection.serieIndex != -1) {

							_selectedTourId = _tourDayData.tourIds[barChartSelection.valueIndex];
							_tourInfoToolTipProvider.setTourId(_selectedTourId);

							ActionEditQuick.doAction(StatisticDay.this);
						}
					}
				}
			}
		});

		addTourPropertyListener();
	}

	private ChartToolTipInfo createToolTipData(int valueIndex) {

		final int[] tourDOYValues = _tourDayData.getDoyValues();

		if (valueIndex >= tourDOYValues.length) {
			valueIndex -= tourDOYValues.length;
		}

		if (tourDOYValues == null || valueIndex >= tourDOYValues.length) {
			return null;
		}

		final long tooltipTourId = _tourDayData.tourIds[valueIndex];

		final String tourTypeName = TourDatabase.getTourTypeName(_tourDayData.typeIds[valueIndex]);
		final String tourTags = TourDatabase.getTagNames(_tourDayData.tagIds.get(tooltipTourId));
		final String tourDescription = _tourDayData.tourDescription.get(valueIndex).replace(
				net.tourbook.ui.UI.SYSTEM_NEW_LINE,
				UI.NEW_LINE1);

		final int[] startValue = _tourDayData.tourStartValues;
		final int[] endValue = _tourDayData.tourEndValues;

		final int recordingTime = _tourDayData.recordingTime[valueIndex];
		final int drivingTime = _tourDayData.drivingTime[valueIndex];
		final int breakTime = recordingTime - drivingTime;

		final ZonedDateTime zdtTourStart = _tourDayData.tourStartDateTimes.get(valueIndex);
		final ZonedDateTime zdtTourEnd = zdtTourStart.plusSeconds(recordingTime);

		final float distance = _tourDayData.tourDistanceValues[valueIndex];
		final float speed = drivingTime == 0 ? 0 : distance / (drivingTime / 3.6f);
		final float pace = distance == 0 ? 0 : drivingTime * 1000 / distance;

		final StringBuilder toolTipFormat = new StringBuilder();
		toolTipFormat.append(TOUR_TOOLTIP_FORMAT_DATE_WEEK_TIME); //		%s - %s - %s - CW %d

		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_distance_tour);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_altitude);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_time);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_recording_time_tour);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_driving_time_tour);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_break_time_tour);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_avg_speed);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_avg_pace);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_tour_type);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_tags);

		if (tourDescription.length() > 0) {
			toolTipFormat.append(UI.NEW_LINE);
			toolTipFormat.append(UI.NEW_LINE);
			toolTipFormat.append(Messages.tourtime_info_description);
			toolTipFormat.append(UI.NEW_LINE);
			toolTipFormat.append(Messages.tourtime_info_description_text);
		}

		final int tourStartTime = startValue[valueIndex];
		final int tourEndTime = endValue[valueIndex];

		final String toolTipLabel = String.format(toolTipFormat.toString(),
		//
		// date/time
				zdtTourStart.format(TimeTools.Formatter_Date_F),
				zdtTourStart.format(TimeTools.Formatter_Time_M),
				zdtTourEnd.format(TimeTools.Formatter_Time_M),
				zdtTourStart.get(TimeTools.calendarWeek.weekOfWeekBasedYear()),
				//
				// distance
				distance / 1000,
				UI.UNIT_LABEL_DISTANCE,
				//
				// altitude
				(int) _tourDayData.tourAltitudeValues[valueIndex],
				UI.UNIT_LABEL_ALTITUDE,
				//
				// start time
				tourStartTime / 3600,
				(tourStartTime % 3600) / 60,
				//
				// end time
				(tourEndTime / 3600) % 24,
				(tourEndTime % 3600) / 60,
				//
				recordingTime / 3600,
				(recordingTime % 3600) / 60,
				(recordingTime % 3600) % 60,
				//
				drivingTime / 3600,
				(drivingTime % 3600) / 60,
				(drivingTime % 3600) % 60,
				//
				breakTime / 3600,
				(breakTime % 3600) / 60,
				(breakTime % 3600) % 60,
				//
				speed,
				UI.UNIT_LABEL_SPEED,
				//
				(int) (pace / 60),
				(int) (pace % 60),
				UI.UNIT_LABEL_PACE,
				//
				tourTypeName,
				tourTags,
				//
				tourDescription
		//
				);

		// set title
		String tourTitle = _tourDayData.tourTitle.get(valueIndex);
		if (tourTitle == null || tourTitle.trim().length() == 0) {
			tourTitle = tourTypeName;
		}

		final ChartToolTipInfo tt1 = new ChartToolTipInfo();
		tt1.setTitle(tourTitle);
		tt1.setLabel(toolTipLabel);

		return tt1;
	}

	/**
	 * create data for the x-axis
	 */
	void createXDataDay(final ChartDataModel chartModel) {

		final ChartDataXSerie xData = new ChartDataXSerie(_tourDayData.getDoyValuesDouble());
		xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_DAY);
//		xData.setVisibleMaxValue(fCurrentYear);
		xData.setChartSegments(createChartSegments(_tourDayData));

		chartModel.setXData(xData);
	}

	/**
	 * Altitude
	 */
	void createYDataAltitude(final ChartDataModel chartModel) {

		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartType.BAR,
				_tourDayData.altitudeLow,
				_tourDayData.altitudeHigh);

		yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
		yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setAllValueColors(0);
		yData.setShowYSlider(true);
		yData.setVisibleMinValue(0);
		yData.setColorIndex(new int[][] { _tourDayData.typeColorIndex });

		StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE);
		StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE, _activeTourTypeFilter);

		chartModel.addYData(yData);
	}

	void createYDataAvgPace(final ChartDataModel chartDataModel) {

		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartType.BAR,
				_tourDayData.avgPaceLow,
				_tourDayData.avgPaceHigh);

		yData.setYTitle(Messages.LABEL_GRAPH_PACE);
		yData.setUnitLabel(UI.UNIT_LABEL_PACE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setAllValueColors(0);
		yData.setShowYSlider(true);
		yData.setVisibleMinValue(0);
		yData.setColorIndex(new int[][] { _tourDayData.typeColorIndex });

		StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_PACE);
		StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_PACE, _activeTourTypeFilter);

		chartDataModel.addYData(yData);
	}

	void createYDataAvgSpeed(final ChartDataModel chartDataModel) {

		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartType.BAR,
				_tourDayData.avgSpeedLow,
				_tourDayData.avgSpeedHigh);

		yData.setYTitle(Messages.LABEL_GRAPH_SPEED);
		yData.setUnitLabel(UI.UNIT_LABEL_SPEED);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setAllValueColors(0);
		yData.setShowYSlider(true);
		yData.setVisibleMinValue(0);
		yData.setColorIndex(new int[][] { _tourDayData.typeColorIndex });

		StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_SPEED);
		StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_SPEED, _activeTourTypeFilter);

		chartDataModel.addYData(yData);
	}

	/**
	 * Distance
	 */
	void createYDataDistance(final ChartDataModel chartModel) {

		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartType.BAR,
				_tourDayData.distanceLow,
				_tourDayData.distanceHigh);

		yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
		yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
		yData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_NUMBER);
		yData.setAllValueColors(0);
		yData.setShowYSlider(true);
		yData.setVisibleMinValue(0);
		yData.setValueDivisor(1000);
		yData.setColorIndex(new int[][] { _tourDayData.typeColorIndex });

		StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE);
		StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE, _activeTourTypeFilter);

		chartModel.addYData(yData);
	}

	/**
	 * Time
	 */
	void createYDataDuration(final ChartDataModel chartModel) {

		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartType.BAR,
				_tourDayData.getDurationLowFloat(),
				_tourDayData.getDurationHighFloat());

		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		yData.setAllValueColors(0);
		yData.setShowYSlider(true);
		yData.setVisibleMinValue(0);
		yData.setColorIndex(new int[][] { _tourDayData.typeColorIndex });

		StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_TIME);
		StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TIME, _activeTourTypeFilter);

		chartModel.addYData(yData);
	}

	@Override
	public void dispose() {

		TourManager.getInstance().removeTourEventListener(_tourPropertyListener);

		super.dispose();
	}

	/**
	 */
	abstract ChartDataModel getChartDataModel();

	@Override
	public int getEnabledGridOptions() {

		return ChartOptions_Grid.GRID_VERTICAL_DISTANCE
				| ChartOptions_Grid.GRID_IS_SHOW_HORIZONTAL_LINE
				| ChartOptions_Grid.GRID_IS_SHOW_VERTICAL_LINE;
	}

	@Override
	public Long getSelectedTour() {
		return _selectedTourId;
	}

	@Override
	public Long getSelectedTourId() {
		return _selectedTourId;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		if (_selectedTourId == -1) {
			return null;
		}

		final ArrayList<TourData> selectedTours = new ArrayList<TourData>();

		selectedTours.add(TourManager.getInstance().getTourData(_selectedTourId));

		return selectedTours;
	}

	@Override
	public void preferencesHasChanged() {

		updateStatistic(new StatisticContext(_activePerson, _activeTourTypeFilter, _currentYear, _numberOfYears));
	}

	void resetMinMaxKeeper() {

		_minMaxKeeper.resetMinMax();
	}

	@Override
	public void restoreState(final IDialogSettings viewState) {

		final String mementoTourId = viewState.get(STATE_SELECTED_TOUR_ID);
		if (mementoTourId != null) {
			try {
				final long tourId = Long.parseLong(mementoTourId);
				selectTour(tourId);
			} catch (final Exception e) {
				// ignore
			}
		}
	}

	@Override
	public void saveState(final IDialogSettings viewState) {

		if (_chart == null || _chart.isDisposed()) {
			return;
		}

		final ISelection selection = _chart.getSelection();
		if (_tourDayData != null && selection instanceof SelectionBarChart) {

			final int valueIndex = ((SelectionBarChart) selection).valueIndex;

			// check array bounds
			if (valueIndex < _tourDayData.tourIds.length) {
				viewState.put(STATE_SELECTED_TOUR_ID, Long.toString(_tourDayData.tourIds[valueIndex]));
			}
		}
	}

	@Override
	public boolean selectTour(final Long tourId) {

		final long[] tourIds = _tourDayData.tourIds;
		final boolean selectedItems[] = new boolean[tourIds.length];
		boolean isSelected = false;

		// find the tour which has the same tourId as the selected tour
		for (int tourIndex = 0; tourIndex < tourIds.length; tourIndex++) {
			final boolean isTourSelected = tourIds[tourIndex] == tourId ? true : false;
			if (isTourSelected) {
				isSelected = true;
				_selectedTourId = tourId;
				_tourInfoToolTipProvider.setTourId(_selectedTourId);
			}
			selectedItems[tourIndex] = isTourSelected;
		}

		if (isSelected == false) {
			// select first tour
//			selectedItems[0] = true;
		}

		_chart.setSelectedBars(selectedItems);

		return isSelected;
	}

	private void setChartProviders(final Chart chartWidget, final ChartDataModel chartModel) {

		// set tool tip info
		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
			@Override
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
				return createToolTipData(valueIndex);
			}
		});

		// set the menu context provider
		chartModel.setCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER, new TourChartContextProvider(_chart, this));
	}

	@Override
	public void setSynchScale(final boolean isSynchScaleEnabled) {

		_isSynchScaleEnabled = isSynchScaleEnabled;

		if (!isSynchScaleEnabled) {
			// reset when sync is set to be disabled
			resetMinMaxKeeper();
		}
	}

	@Override
	public void updateStatistic(final StatisticContext statContext) {

		_activePerson = statContext.appPerson;
		_activeTourTypeFilter = statContext.appTourTypeFilter;
		_currentYear = statContext.statFirstYear;
		_numberOfYears = statContext.statNumberOfYears;

		/*
		 * get currently selected tour id
		 */
		long selectedTourId = -1;
		final ISelection selection = _chart.getSelection();
		if (selection instanceof SelectionBarChart) {
			final SelectionBarChart barChartSelection = (SelectionBarChart) selection;

			if (barChartSelection.serieIndex != -1) {

				int selectedValueIndex = barChartSelection.valueIndex;
				final long[] tourIds = _tourDayData.tourIds;

				if (tourIds.length > 0) {

					if (selectedValueIndex >= tourIds.length) {
						selectedValueIndex = tourIds.length - 1;
					}

					selectedTourId = tourIds[selectedValueIndex];
				}
			}
		}

		_tourDayData = DataProvider_Tour_Day.getInstance().getDayData(
				statContext.appPerson,
				statContext.appTourTypeFilter,
				statContext.statFirstYear,
				statContext.statNumberOfYears,
				isDataDirtyWithReset() || statContext.isRefreshData);

		// reset min/max values
		if (_isSynchScaleEnabled == false && statContext.isRefreshData) {
			resetMinMaxKeeper();
		}

		final ChartDataModel chartModel = getChartDataModel();

		/*
		 * set graph minimum width, this is the number of days in the year
		 */
		final int yearDays = TimeTools.getNumberOfDaysWithYear(_currentYear);
		chartModel.setChartMinWidth(yearDays);

		setChartProviders(_chart, chartModel);

		if (_isSynchScaleEnabled) {
			_minMaxKeeper.setMinMaxValues(chartModel);
		}

		StatisticServices.updateChartProperties(_chart, getGridPrefPrefix());

		// show the data in the chart
		_chart.updateChart(chartModel, false, true);

		// try to select the previous selected tour
		selectTour(selectedTourId);
	}

	@Override
	public void updateToolBar() {
		_chart.fillToolbar(true);
	}
}
