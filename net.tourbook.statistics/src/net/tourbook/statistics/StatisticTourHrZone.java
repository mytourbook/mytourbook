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
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartSegments;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.chart.SelectionBarChart;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourInfoToolTipProvider;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.util.IToolTipHideListener;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;

public class StatisticTourHrZone extends YearStatistic implements IBarSelectionProvider, ITourProvider {

	private TourPerson				_activePerson;
	private TourTypeFilter			_activeTourTypeFilter;

	private IPostSelectionProvider	_postSelectionProvider;

	private int						_currentYear;
	private int						_numberOfYears;

	private TourHrZoneData			_tourHrZoneData;
	private IViewSite				_viewSite;
	private Long					_selectedTourId				= null;

	private StatisticTourToolTip	_tourToolTip;
	private TourInfoToolTipProvider	_tourInfoToolTipProvider	= new TourInfoToolTipProvider();
	private final Calendar			_calendar					= GregorianCalendar.getInstance();

	/*
	 * UI controls
	 */
	private FormToolkit				_tk;

	private PageBook				_pageBook;
	private Composite				_pageChart;
	private Composite				_pageNoPerson;
	private Chart					_chart;

	@Override
	public void activateActions(final IWorkbenchPartSite partSite) {
		// TODO Auto-generated method stub

	}

	/**
	 * create segments for the chart
	 */
	private ChartSegments createChartSegments(final TourHrZoneData _tourHrZoneData) {

		final int segmentStart[] = new int[_numberOfYears];
		final int segmentEnd[] = new int[_numberOfYears];
		final String[] segmentTitle = new String[_numberOfYears];

		final int[] allYearDays = _tourHrZoneData.daysInEachYear;
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

		final ChartSegments chartSegments = new ChartSegments();
		chartSegments.valueStart = segmentStart;
		chartSegments.valueEnd = segmentEnd;
		chartSegments.segmentTitle = segmentTitle;

		chartSegments.years = _tourHrZoneData.years;
		chartSegments.yearDays = _tourHrZoneData.daysInEachYear;
		chartSegments.allValues = _tourHrZoneData.allDaysInAllYears;

		return chartSegments;
	}

	@Override
	public void createControl(	final Composite parent,
								final IViewSite viewSite,
								final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		_viewSite = viewSite;
		_postSelectionProvider = postSelectionProvider;

		createUI(parent);
	}

	private void createUI(final Composite parent) {

		_tk = new FormToolkit(parent.getDisplay());

		_pageBook = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);

		_pageNoPerson = createUI10NoPerson(_pageBook);
		_pageChart = createUI20Chart(_pageBook);
	}

	private Composite createUI10NoPerson(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
		{
			final Label label = new Label(container, SWT.WRAP);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
			label.setText(Messages.UI_Label_PersonIsNotSelected);
		}

		return container;
	}

	private Composite createUI20Chart(final Composite parent) {

		// chart widget page
		_chart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		_chart.setShowZoomActions(true);
		_chart.setCanScrollZoomedChart(true);
		_chart.setDrawBarChartAtBottom(false);
		_chart.setToolBarManager(_viewSite.getActionBars().getToolBarManager(), false);

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

		_chart.setTourToolTipProvider(_tourInfoToolTipProvider);
		_tourInfoToolTipProvider.setActionsEnabled(true);

		_chart.addBarSelectionListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, int valueIndex) {

				final long[] tourIds = _tourHrZoneData.tourIds;

				if (tourIds != null && tourIds.length > 0) {

					if (valueIndex >= tourIds.length) {
						valueIndex = tourIds.length - 1;
					}

					_selectedTourId = tourIds[valueIndex];
					_tourInfoToolTipProvider.setTourId(_selectedTourId);

					DataProviderTourTime.getInstance().setSelectedTourId(_selectedTourId);
					_postSelectionProvider.setSelection(new SelectionTourId(_selectedTourId));
				}
			}
		});

		/*
		 * open tour with double click on the tour bar
		 */
		_chart.addDoubleClickListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, final int valueIndex) {
				final long[] tourIds = _tourHrZoneData.tourIds;
				if (tourIds.length > 0) {

					_selectedTourId = tourIds[valueIndex];
					_tourInfoToolTipProvider.setTourId(_selectedTourId);

					DataProviderTourTime.getInstance().setSelectedTourId(_selectedTourId);

					ActionEditQuick.doAction(StatisticTourHrZone.this);
				}
			}
		});

		/*
		 * open tour with Enter key
		 */
		_chart.addTraverseListener(new TraverseListener() {
			public void keyTraversed(final TraverseEvent event) {

				if (event.detail == SWT.TRAVERSE_RETURN) {
					final ISelection selection = _chart.getSelection();
					if (selection instanceof SelectionBarChart) {
						final SelectionBarChart barChartSelection = (SelectionBarChart) selection;

						if (barChartSelection.serieIndex != -1) {

							_selectedTourId = _tourHrZoneData.tourIds[barChartSelection.valueIndex];
							_tourInfoToolTipProvider.setTourId(_selectedTourId);

							ActionEditQuick.doAction(StatisticTourHrZone.this);
						}
					}
				}
			}
		});

		return _chart;
	}

	/**
	 * create data for the x-axis
	 */
	void createXDataDay(final ChartDataModel chartModel) {

		final ChartDataXSerie xData = new ChartDataXSerie(_tourHrZoneData.tourDOY);
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_DAY);
//		xData.setVisibleMaxValue(fCurrentYear);
		xData.setChartSegments(createChartSegments(_tourHrZoneData));

		chartModel.setXData(xData);
	}


	void createYDataDuration(final ChartDataModel chartDataModel) {

		// duration
		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				_tourHrZoneData.timeLow,
				_tourHrZoneData.timeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		yData.setAllValueColors(0);
		yData.setVisibleMinValue(0);
//		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_TIME, _activeTourTypeFilter);
//		StatisticServices.setTourTypeColorIndex(yData, _tourHrZoneData.typeIds, _activeTourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_TIME);

		chartDataModel.addYData(yData);
	}

//	/**
//	 * Time
//	 */
//	void createYDataDuration(final ChartDataModel chartModel) {
//
//		final ChartDataYSerie yData = new ChartDataYSerie(
//				ChartDataModel.CHART_TYPE_BAR,
//				_tourHrZoneData.timeLow,
//				_tourHrZoneData.timeHigh);
//
//		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
//		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
//		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
//		yData.setAllValueColors(0);
//		yData.setVisibleMinValue(0);
//		yData.setCustomData(DURATION_DATA, 1);
//		yData.setColorIndex(new int[][] { _tourHrZoneData.typeColorIndex });
//
//		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_TIME);
//		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_TIME, _activeTourTypeFilter);
//
//		chartModel.addYData(yData);
//	}
	@Override
	public void deactivateActions(final IWorkbenchPartSite partSite) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {

		if (_tk != null) {
			_tk.dispose();
		}

		super.dispose();
	}

	@Override
	public Integer getSelectedMonth() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getSelectedTour() {
		return _selectedTourId;
	}

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
	public void prefColorChanged() {
		// TODO Auto-generated method stub

	}
	public void refreshStatistic(	final TourPerson person,
									final TourTypeFilter tourTypeFilter,
									final int year,
									final int numberOfYears,
									final boolean refreshData) {

		if (person == null) {

			// multiple persons are not supported

			_pageBook.showPage(_pageNoPerson);
			return;
		}

		_activePerson = person;
		_activeTourTypeFilter = tourTypeFilter;
		_currentYear = year;
		_numberOfYears = numberOfYears;

		/*
		 * get currently selected tour id
		 */
		long selectedTourId = -1;
		final ISelection selection = _chart.getSelection();
		if (selection instanceof SelectionBarChart) {
			final SelectionBarChart barChartSelection = (SelectionBarChart) selection;

			if (barChartSelection.serieIndex != -1) {

				int selectedValueIndex = barChartSelection.valueIndex;
				final long[] tourIds = _tourHrZoneData.tourIds;

				if (tourIds.length > 0) {

					if (selectedValueIndex >= tourIds.length) {
						selectedValueIndex = tourIds.length - 1;
					}

					selectedTourId = tourIds[selectedValueIndex];
				}
			}
		}

		_tourHrZoneData = DataProviderTourHrZones.getInstance().getTourHrZoneData(
				person,
				tourTypeFilter,
				year,
				numberOfYears,
				isDataDirtyWithReset() || refreshData);

		final ChartDataModel chartModel = updateChart();

		/*
		 * set graph minimum width, these is the number of days in the year
		 */
		_calendar.set(_currentYear, 11, 31);
		chartModel.setChartMinWidth(_calendar.get(Calendar.DAY_OF_YEAR));

		setChartProviders(_chart, chartModel);

		// set grid size
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		_chart.setGridDistance(
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE));

		// show the data in the chart
		_chart.updateChart(chartModel, false, true);

		// try to select the previous selected tour
		selectTour(selectedTourId);
	}
	@Override
	public void resetSelection() {
		// TODO Auto-generated method stub

	}

	void setChartProviders(final Chart chartWidget, final ChartDataModel chartModel) {

		// set tool tip info
//		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
//			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
//				return createToolTipInfo(valueIndex);
//			}
//		});

		// set the menu context provider
		chartModel.setCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER, new TourChartContextProvider(_chart, this));
	}
	@Override
	public void setSynchScale(final boolean isEnabled) {
		// scale is always 10%
	}

	ChartDataModel updateChart() {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		createXDataDay(chartDataModel);
//		createYDataDuration(chartDataModel);

		return chartDataModel;
	}

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		// TODO Auto-generated method stub

	}

}
