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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
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
import net.tourbook.common.util.IToolTipHideListener;
import net.tourbook.common.util.Util;
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
import net.tourbook.tour.SelectionTourId;
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

public class StatisticTour_Time extends TourbookStatistic implements IBarSelectionProvider, ITourProvider {

	private TourPerson					_activePerson;
	private TourTypeFilter				_activeTourTypeFiler;
	private int							_currentYear;
	private int							_numberOfYears;

	private final Calendar				_calendar					= GregorianCalendar.getInstance();
	private final DateFormat			_dateFormatter				= DateFormat.getDateInstance(DateFormat.FULL);

	private Chart						_chart;

	private StatisticContext			_statContext;

	private StatisticTourToolTip		_tourToolTip;
	private TourInfoIconToolTipProvider	_tourInfoToolTipProvider	= new TourInfoIconToolTipProvider();

	private TourData_Time				_tourTimeData;

	private final MinMaxKeeper_YData	_minMaxKeeper				= new MinMaxKeeper_YData();
	private boolean						_ifIsSynchScaleEnabled;

	private Long						_selectedTourId				= null;
	private int							_currentMonth;

	/**
	 * create segments for the chart
	 */
	private ChartStatisticSegments createChartSegments(final TourData_Time tourDataTime) {

		final double segmentStart[] = new double[_numberOfYears];
		final double segmentEnd[] = new double[_numberOfYears];
		final String[] segmentTitle = new String[_numberOfYears];

		final int[] allYearDays = tourDataTime.yearDays;
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

		chartSegments.years = tourDataTime.years;
		chartSegments.yearDays = tourDataTime.yearDays;
		chartSegments.allValues = tourDataTime.allDaysInAllYears;

		return chartSegments;
	}

	@Override
	public void createStatisticUI(final Composite parent, final IViewSite viewSite) {

		// chart widget page
		_chart = new Chart(parent, SWT.FLAT);
		_chart.setShowZoomActions(true);
		_chart.setDrawBarChartAtBottom(false);
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
			public void selectionChanged(final int serieIndex, int valueIndex) {

				final long[] tourIds = _tourTimeData.tourIds;

				if (tourIds != null && tourIds.length > 0) {

					if (valueIndex >= tourIds.length) {
						valueIndex = tourIds.length - 1;
					}

					_selectedTourId = tourIds[valueIndex];
					_tourInfoToolTipProvider.setTourId(_selectedTourId);

					DataProvider_Tour_Time.getInstance().setSelectedTourId(_selectedTourId);

					// don't fire an event when preferences are updated
					if (isInPreferencesUpdate() || _statContext.canFireEvents() == false) {
						return;
					}

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
				final long[] tourIds = _tourTimeData.tourIds;
				if (tourIds.length > 0) {

					_selectedTourId = tourIds[valueIndex];
					_tourInfoToolTipProvider.setTourId(_selectedTourId);

					DataProvider_Tour_Time.getInstance().setSelectedTourId(_selectedTourId);

					ActionEditQuick.doAction(StatisticTour_Time.this);
				}
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

							_selectedTourId = _tourTimeData.tourIds[barChartSelection.valueIndex];
							_tourInfoToolTipProvider.setTourId(_selectedTourId);

							ActionEditQuick.doAction(StatisticTour_Time.this);
						}
					}
				}
			}
		});

	}

	private ChartToolTipInfo createToolTipInfo(int valueIndex) {

		final int[] tourDOYValues = _tourTimeData.tourDOYValues;

		if (valueIndex >= tourDOYValues.length) {
			valueIndex -= tourDOYValues.length;
		}

		if (tourDOYValues == null || valueIndex >= tourDOYValues.length) {
			return null;
		}

		/*
		 * set calendar day/month/year
		 */
		final int oldestYear = _currentYear - _numberOfYears + 1;
		final int tourDOY = tourDOYValues[valueIndex];
		_calendar.set(oldestYear, 0, 1);
		_calendar.set(Calendar.DAY_OF_YEAR, tourDOY + 1);
		final String beginDate = _dateFormatter.format(_calendar.getTime());

		_currentMonth = _calendar.get(Calendar.MONTH) + 1;
		final long tooltipTourId = _tourTimeData.tourIds[valueIndex];

		final String tourTypeName = TourDatabase.getTourTypeName(_tourTimeData.typeIds[valueIndex]);
		final String tourTags = TourDatabase.getTagNames(_tourTimeData.tagIds.get(tooltipTourId));
		final String tourDescription = _tourTimeData.tourDescription.get(valueIndex).replace(
				net.tourbook.ui.UI.SYSTEM_NEW_LINE,
				UI.NEW_LINE1);

		final int[] startValue = _tourTimeData.tourTimeStartValues;
		final int[] endValue = _tourTimeData.tourTimeEndValues;

		final Integer recordingTime = _tourTimeData.tourRecordingTimeValues.get(valueIndex);
		final Integer drivingTime = _tourTimeData.tourDrivingTimeValues.get(valueIndex);
		final int breakTime = recordingTime - drivingTime;

		final float distance = _tourTimeData.tourDistanceValues[valueIndex];
		final float speed = drivingTime == 0 ? 0 : distance / (drivingTime / 3.6f);
		final int pace = (int) (distance == 0 ? 0 : (drivingTime * 1000 / distance));

		final StringBuilder toolTipFormat = new StringBuilder();
		toolTipFormat.append(Messages.TourTime_Info_DateDay); //			%s - CW %d
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

		final String toolTipLabel = String.format(toolTipFormat.toString(),
		//
				beginDate,
				_tourTimeData.weekValues[valueIndex],
				//
				distance / 1000,
				UI.UNIT_LABEL_DISTANCE,
				//
				_tourTimeData.tourAltitudeValues[valueIndex],
				UI.UNIT_LABEL_ALTITUDE,
				//
				// start time
				startValue[valueIndex] / 3600,
				(startValue[valueIndex] % 3600) / 60,
				//
				// end time
				endValue[valueIndex] / 3600 % 24,
				(endValue[valueIndex] % 3600) / 60,
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
				pace / 60,
				pace % 60,
				UI.UNIT_LABEL_PACE,
				//
				tourTypeName,
				tourTags,
				//
				tourDescription
//
				)
				.toString();

		/*
		 * create tool tip info
		 */
		String tourTitle = _tourTimeData.tourTitle.get(valueIndex);
		if (tourTitle == null || tourTitle.trim().length() == 0) {
			tourTitle = tourTypeName;
		}

		final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();

		toolTipInfo.setTitle(tourTitle);
		toolTipInfo.setLabel(toolTipLabel);

		return toolTipInfo;
	}

	@Override
	public int getEnabledGridOptions() {

		return ChartOptions_Grid.GRID_VERTICAL_DISTANCE
				| ChartOptions_Grid.GRID_IS_SHOW_HORIZONTAL_LINE
				| ChartOptions_Grid.GRID_IS_SHOW_VERTICAL_LINE;
	}

	@Override
	protected String getGridPrefPrefix() {
		return GRID_TOUR_TIME;
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

		if (_selectedTourId == null) {
			return null;
		}

		final ArrayList<TourData> selectedTours = new ArrayList<TourData>();

		selectedTours.add(TourManager.getInstance().getTourData(_selectedTourId));

		return selectedTours;
	}

	@Override
	public void preferencesHasChanged() {

		updateStatistic(new StatisticContext(_activePerson, _activeTourTypeFiler, _currentYear, _numberOfYears));
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
		if (_tourTimeData != null
				&& _tourTimeData.tourIds != null
				&& _tourTimeData.tourIds.length > 0
				&& selection instanceof SelectionBarChart) {

			final Long selectedTourId = _tourTimeData.tourIds[((SelectionBarChart) selection).valueIndex];

			viewState.put(STATE_SELECTED_TOUR_ID, Long.toString(selectedTourId));
		}
	}

	@Override
	public boolean selectTour(final Long tourId) {

		final long[] tourIds = _tourTimeData.tourIds;

		if (tourIds.length == 0) {
			_selectedTourId = null;
			_tourInfoToolTipProvider.setTourId(-1);

			return false;
		}

		final boolean selectedTours[] = new boolean[tourIds.length];

		boolean isSelected = false;

		// find the tour which has the same tourId as the selected tour
		for (int tourIndex = 0; tourIndex < tourIds.length; tourIndex++) {
			if ((tourIds[tourIndex] == tourId)) {
				selectedTours[tourIndex] = true;
				isSelected = true;

				_selectedTourId = tourId;
				_tourInfoToolTipProvider.setTourId(_selectedTourId);

				break;
			}
		}

		if (isSelected == false) {
			// select first tour
			selectedTours[0] = true;
			_selectedTourId = tourIds[0];
			_tourInfoToolTipProvider.setTourId(_selectedTourId);
		}

		_chart.setSelectedBars(selectedTours);

		return isSelected;
	}

	private void setChartProviders(final Chart chartWidget, final ChartDataModel chartModel) {

		final IChartInfoProvider chartInfoProvider = new IChartInfoProvider() {

			@Override
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
				return createToolTipInfo(valueIndex);
			}
		};

		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, chartInfoProvider);

		// set the menu context provider
		chartModel.setCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER, new TourChartContextProvider(_chart, this));
	}

	@Override
	public void setSynchScale(final boolean isSynchScaleEnabled) {

		if (!isSynchScaleEnabled) {

			// reset when it's disabled

			_minMaxKeeper.resetMinMax();
		}

		_ifIsSynchScaleEnabled = isSynchScaleEnabled;
	}

	private void updateChart(final long selectedTourId) {

		final ChartDataModel chartModel = new ChartDataModel(ChartType.BAR);

		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(Util.convertIntToDouble(_tourTimeData.tourDOYValues));
		xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_DAY);
		xData.setVisibleMaxValue(_currentYear);
		xData.setChartSegments(createChartSegments(_tourTimeData));
		chartModel.setXData(xData);

		// set the bar low/high data
		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartType.BAR,
				Util.convertIntToFloat(_tourTimeData.tourTimeStartValues),
				Util.convertIntToFloat(_tourTimeData.tourTimeEndValues));
		yData.setYTitle(Messages.LABEL_GRAPH_DAYTIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_HOUR_MINUTE_24H);
		yData.setYAxisDirection(false);
		yData.setShowYSlider(true);

		yData.setColorIndex(new int[][] { _tourTimeData.typeColorIndex });
		StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TIME, _activeTourTypeFiler);
		StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_TIME);

		chartModel.addYData(yData);

		/*
		 * set graph minimum width, this is the number of days in the year
		 */
		_calendar.set(_currentYear, 11, 31);
		final int yearDays = _calendar.get(Calendar.DAY_OF_YEAR);
		chartModel.setChartMinWidth(yearDays);

		setChartProviders(_chart, chartModel);

		if (_ifIsSynchScaleEnabled) {
			_minMaxKeeper.setMinMaxValues(chartModel);
		}

		StatisticServices.updateChartProperties(_chart, getGridPrefPrefix());

		// show the data in the chart
		_chart.updateChart(chartModel, false, true);

		// try to select the previous selected tour
		selectTour(selectedTourId);
	}

	@Override
	public void updateStatistic(final StatisticContext statContext) {

		_statContext = statContext;

		_activePerson = statContext.appPerson;
		_activeTourTypeFiler = statContext.appTourTypeFilter;
		_currentYear = statContext.statFirstYear;
		_numberOfYears = statContext.statNumberOfYears;

		/*
		 * get currently selected tour id
		 */
		long selectedTourId = -1;
		final ISelection selection = _chart.getSelection();
		if (selection instanceof SelectionBarChart) {
			final SelectionBarChart barChartSelection = (SelectionBarChart) selection;

			if (barChartSelection.serieIndex != -1 && _tourTimeData != null) {

				int selectedValueIndex = barChartSelection.valueIndex;
				final long[] tourIds = _tourTimeData.tourIds;

				if (tourIds.length > 0) {
					if (selectedValueIndex >= tourIds.length) {
						selectedValueIndex = tourIds.length - 1;
					}

					selectedTourId = tourIds[selectedValueIndex];
				}
			}
		}

		_tourTimeData = DataProvider_Tour_Time.getInstance().getTourTimeData(
				statContext.appPerson,
				statContext.appTourTypeFilter,
				statContext.statFirstYear,
				statContext.statNumberOfYears,
				isDataDirtyWithReset() || statContext.isRefreshData);

		// reset min/max values
		if (_ifIsSynchScaleEnabled == false && statContext.isRefreshData) {
			_minMaxKeeper.resetMinMax();
		}

		updateChart(selectedTourId);
	}

	@Override
	public void updateToolBar() {
		_chart.fillToolbar(true);
	}

}
