/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

package net.tourbook.ui.views.tourCatalog;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Formatter;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartSegments;
import net.tourbook.chart.ChartToolTipInfo;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.ActionSelectYears;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.util.ArrayListToArray;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.Util;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

public class TourCatalogViewYearStatistic extends ViewPart {

	public static final String					ID						= "net.tourbook.views.tourCatalog.yearStatisticView";	//$NON-NLS-1$

	private static final String					STATE_NUMBER_OF_YEARS	= "numberOfYearsToDisplay";							//$NON-NLS-1$

	private final IPreferenceStore				_prefStore				= TourbookPlugin
																				.getDefault()
																				.getPreferenceStore();
	private final IDialogSettings				_state					= TourbookPlugin
																				.getDefault()
																				.getDialogSettingsSection(
																						"TourCatalogViewYearStatistic");		//$NON-NLS-1$

	private IPropertyChangeListener				_prefChangeListener;
	private IPartListener2						_partListener;
	private ISelectionListener					_postSelectionListener;
	private PostSelectionProvider				_postSelectionProvider;

	private final DateFormat					_dtFormatter			= DateFormat.getDateInstance(DateFormat.FULL);
	private NumberFormat						_nf						= NumberFormat.getNumberInstance();
	{
		_nf.setMinimumFractionDigits(1);
		_nf.setMaximumFractionDigits(1);
	}

	/**
	 * contains all {@link TVICatalogComparedTour} tour objects for all years
	 */
	private ArrayList<TVICatalogComparedTour>	_allTours;

	private int[]								_allYears;
	private int									_youngesYear			= new DateTime().getYear();

	/**
	 * year item for the visible statistics
	 */
	private TVICatalogRefTourItem				_currentRefItem;

	/**
	 * selection which is thrown by the year statistic
	 */
	private StructuredSelection					_currentSelection;

	private ITourEventListener					_compareTourPropertyListener;

	private boolean								_isSynchMaxValue;

	private int									_numberOfYears;

	private int[]								_yearDays;

	/**
	 * Day of year values for all years
	 */
	private ArrayList<Integer>					_DOYValues;

	/**
	 * Tour speed for all years
	 */
	private ArrayList<Integer>					_tourSpeed;

	private int									_selectedTourIndex;

	/*
	 * UI controls
	 */
	private PageBook							_pageBook;
	private Label								_pageNoChart;
	private Chart								_yearChart;

	private ActionSelectYears					_actionSelectYears;
	private IAction								_actionSynchChartScale;

	public TourCatalogViewYearStatistic() {}

	public void actionSynchScale(final boolean isSynchMaxValue) {
		_isSynchMaxValue = isSynchMaxValue;
		updateYearBarChart(false);
	}

	private void addCompareTourPropertyListener() {

		_compareTourPropertyListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId propertyId, final Object propertyData) {

				if (propertyId == TourEventId.COMPARE_TOUR_CHANGED
						&& propertyData instanceof TourPropertyCompareTourChanged) {

					final TourPropertyCompareTourChanged compareTourProperty = (TourPropertyCompareTourChanged) propertyData;

					if (compareTourProperty.isDataSaved) {
						updateYearBarChart(false);
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_compareTourPropertyListener);
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourCatalogViewYearStatistic.this) {
					saveState();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					// recreate the chart
					_yearChart.dispose();
					createYearChart();

					updateYearBarChart(false);
				}
			}
		};
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				// prevent to listen to a selection which is originated by this year chart
				if (selection != _currentSelection) {
					onSelectionChanged(selection);
				}
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void createActions() {

		_actionSynchChartScale = new ActionSynchYearScale(this);
		_actionSelectYears = new ActionSelectYears(this);

		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(_actionSynchChartScale);

		final IMenuManager mm = getViewSite().getActionBars().getMenuManager();
		mm.add(_actionSelectYears);
	}

	/**
	 * create segments for the chart
	 */
	ChartSegments createChartSegments() {

		final int segmentStart[] = new int[_numberOfYears];
		final int segmentEnd[] = new int[_numberOfYears];
		final String[] segmentTitle = new String[_numberOfYears];

		final int oldestYear = _youngesYear - _numberOfYears + 1;
		int yearDaysSum = 0;

		// create segments for each year
		for (int yearDayIndex = 0; yearDayIndex < _yearDays.length; yearDayIndex++) {

			final int yearDays = _yearDays[yearDayIndex];

			segmentStart[yearDayIndex] = yearDaysSum;
			segmentEnd[yearDayIndex] = yearDaysSum + yearDays - 1;
			segmentTitle[yearDayIndex] = Integer.toString(oldestYear + yearDayIndex);

			yearDaysSum += yearDays;
		}

		final ChartSegments chartSegments = new ChartSegments();
		chartSegments.valueStart = segmentStart;
		chartSegments.valueEnd = segmentEnd;
		chartSegments.segmentTitle = segmentTitle;

		chartSegments.years = _allYears;
		chartSegments.yearDays = _yearDays;
		chartSegments.allValues = yearDaysSum;

		return chartSegments;
	}

	@Override
	public void createPartControl(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);

		_pageNoChart = new Label(_pageBook, SWT.NONE);
		_pageNoChart.setText(Messages.tourCatalog_view_label_year_not_selected);

		createYearChart();

		addSelectionListener();
		addCompareTourPropertyListener();
		addPrefListener();
		addPartListener();

		createActions();

		// set selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

		_pageBook.showPage(_pageNoChart);

		restoreState();

		// restore selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

	}

	private ChartToolTipInfo createToolTipInfo(int valueIndex) {

		if (valueIndex >= _DOYValues.size()) {
			valueIndex -= _DOYValues.size();
		}

		if (_DOYValues == null || valueIndex >= _DOYValues.size()) {
			return null;
		}

		/*
		 * set calendar day/month/year
		 */
		final int oldestYear = _youngesYear - _numberOfYears + 1;
		final int tourDOY = _DOYValues.get(valueIndex);
		final DateTime tourDate = new DateTime(oldestYear, 1, 1, 0, 0, 0, 1).plusDays(tourDOY);

		final StringBuilder toolTipFormat = new StringBuilder();
		toolTipFormat.append(Messages.tourCatalog_view_tooltip_speed);
		toolTipFormat.append(UI.NEW_LINE);

		final String toolTipLabel = new Formatter().format(
				toolTipFormat.toString(),
				_nf.format((float) _tourSpeed.get(valueIndex) / 10)).toString();

		final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
		toolTipInfo.setTitle(_dtFormatter.format(tourDate.toDate()));
		toolTipInfo.setLabel(toolTipLabel);

		return toolTipInfo;
	}

	private void createYearChart() {

		// year chart
		_yearChart = new Chart(_pageBook, SWT.NONE);

		_yearChart.addBarSelectionListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, final int valueIndex) {

				if (_allTours.size() == 0) {
					return;
				}

				// ensure list size
				_selectedTourIndex = Math.min(valueIndex, _allTours.size() - 1);

				// select the tour in the tour viewer & show tour in compared tour char
				final TVICatalogComparedTour tourCatalogComparedTour = _allTours.get(_selectedTourIndex);
				_currentSelection = new StructuredSelection(tourCatalogComparedTour);
				_postSelectionProvider.setSelection(_currentSelection);
			}
		});
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);
		TourManager.getInstance().removeTourEventListener(_compareTourPropertyListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	/**
	 * @param selectedYear
	 * @param numberOfYears
	 * @return Returns the number of days between {@link #fLastYear} and selectedYear
	 */
	int getYearDOYs(final int selectedYear) {

		int yearDOYs = 0;
		int yearIndex = 0;

		final int firstYear = _youngesYear - _numberOfYears + 1;

		for (int currentYear = firstYear; currentYear < selectedYear; currentYear++) {

			if (currentYear == selectedYear) {
				return yearDOYs;
			}

			yearDOYs += _yearDays[yearIndex];

			yearIndex++;
		}

		return yearDOYs;
	}

	/**
	 * get numbers for each year <br>
	 * <br>
	 * all years into {@link #fYears} <br>
	 * number of day's into {@link #_yearDays} <br>
	 * number of week's into {@link #fYearWeeks}
	 */
	void initYearNumbers() {

	}

	/**
	 * Update statistic by setting the number of years
	 * 
	 * @param numberOfYears
	 */
	public void onExecuteSelectNumberOfYears(final int numberOfYears) {

		// get selected tour
		long selectedTourId = 0;
		if (_allTours.size() == 0) {
			selectedTourId = -1;
		} else {
			final int selectedTourIndex = Math.min(_selectedTourIndex, _allTours.size() - 1);
			selectedTourId = _allTours.get(selectedTourIndex).getTourId();
		}

		_numberOfYears = numberOfYears;
		setYearData();

		updateYearBarChart(false);

		// reselect last selected tour
		selectTourInYearStatistic(selectedTourId);
	}

	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogItem = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogItem.getRefItem();
			if (refItem != null) {

				// reference tour is selected

				_currentRefItem = refItem;
				updateYearBarChart(true);

			} else {

				// show year statistic

				final TVICatalogYearItem yearItem = tourCatalogItem.getYearItem();
				if (yearItem != null) {

					_currentRefItem = yearItem.getRefItem();
					_youngesYear = yearItem.year;

					setYearData();
					updateYearBarChart(false);
				}
			}

			// select tour in the statistic
			final Long compTourId = tourCatalogItem.getCompTourId();
			if (compTourId != null) {

				selectTourInYearStatistic(compTourId);

			} else if (_allTours != null) {

				// select first tour for the youngest year
				int yearIndex = 0;
				for (final TVICatalogComparedTour tourItem : _allTours) {

					if (new DateTime(tourItem.getTourDate()).getYear() == _youngesYear) {
						break;
					}
					yearIndex++;
				}

				if (_allTours.size() > 0 && _allTours.size() >= yearIndex) {
					selectTourInYearStatistic(_allTours.get(yearIndex).getTourId());
				}
			}

//			// hide chart when a different ref tour is selected
//			if (fCurrentYearItem != null && tourCatalogItem.getRefId() != fCurrentYearItem.refId) {
//				fPageBook.showPage(fPageNoChart);
//				fCurrentYearItem = null;
//			}

		} else if (selection instanceof StructuredSelection) {

			final StructuredSelection structuredSelection = (StructuredSelection) selection;

			if (structuredSelection.size() > 0) {
				final Object firstElement = structuredSelection.getFirstElement();
				if (firstElement instanceof TVICatalogComparedTour) {

					final TVICatalogComparedTour compareItem = (TVICatalogComparedTour) firstElement;
//					final TVICatalogYearItem yearItem = (TVICatalogYearItem) compareItem.getParentItem();
//
//					// show year statistic
//					if (yearItem != fCurrentYearItem) {
//						fCurrentYearItem = yearItem;
//						updateYearBarChart();
//					}

					// select tour in the year chart
					final Long compTourId = compareItem.getTourId();
					if (compTourId != null) {
						selectTourInYearStatistic(compTourId);
					}

//					// hide chart when a different ref tour is selected
//					if (fCurrentYearItem != null && compareItem.getRefId() != fCurrentYearItem.refId) {
//						fPageBook.showPage(fPageNoChart);
//						fCurrentYearItem = null;
//					}
				}
			}

		} else if (selection instanceof SelectionRemovedComparedTours) {

			final SelectionRemovedComparedTours removedCompTours = (SelectionRemovedComparedTours) selection;

			if (removedCompTours.removedComparedTours.size() > 0) {
				updateYearBarChart(false);
			}
		}
	}

	private void restoreState() {

		_actionSelectYears.setNumberOfYears(Util.getStateInt(_state, STATE_NUMBER_OF_YEARS, 3));

		_numberOfYears = _actionSelectYears.getSelectedYear();

		/*
		 * reselect again because there is somewhere a bug because the first time setting the
		 * checkmark for the year does not work
		 */
		_actionSelectYears.setNumberOfYears(_numberOfYears);

		setYearData();
	}

	private void saveState() {

		// save number of years which are displayed
		_state.put(STATE_NUMBER_OF_YEARS, _numberOfYears);
	}

	/**
	 * select the tour in the year map chart
	 * 
	 * @param selectedTourId
	 *            tour id which should be selected
	 */
	private void selectTourInYearStatistic(final long selectedTourId) {

		if (_allTours == null || _allTours.size() == 0) {
			return;
		}

		final int tourLength = _allTours.size();
		final boolean[] selectedTours = new boolean[tourLength];
		boolean isTourSelected = false;

		for (int tourIndex = 0; tourIndex < tourLength; tourIndex++) {
			final TVICatalogComparedTour comparedItem = _allTours.get(tourIndex);
			if (comparedItem.getTourId() == selectedTourId) {
				selectedTours[tourIndex] = true;
				isTourSelected = true;
			}
		}

		if (isTourSelected == false && selectedTours.length > 0) {
			// a tour is not selected, select first tour
			selectedTours[0] = true;
		}

		_yearChart.setSelectedBars(selectedTours);
	}

	@Override
	public void setFocus() {
		_yearChart.setFocus();
	}

	/**
	 * get data for each displayed year
	 */
	private void setYearData() {

		_yearDays = new int[_numberOfYears];
		_allYears = new int[_numberOfYears];

		final int firstYear = _youngesYear - _numberOfYears + 1;

		final DateTime dt = (new DateTime())
				.withYear(firstYear)
				.withWeekOfWeekyear(1)
				.withDayOfWeek(DateTimeConstants.MONDAY);

		int yearIndex = 0;
		for (int currentYear = firstYear; currentYear <= _youngesYear; currentYear++) {

			_allYears[yearIndex] = currentYear;
			_yearDays[yearIndex] = dt.withYear(currentYear).dayOfYear().getMaximumValue();
			yearIndex++;
		}
	}

	/**
	 * show statistic for several years
	 * 
	 * @param showYoungestYear
	 *            shows the youngest year and the years before
	 */
	private void updateYearBarChart(final boolean showYoungestYear) {

		if (_currentRefItem == null) {
			return;
		}

		_pageBook.showPage(_yearChart);

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		if (_allTours != null) {
			_allTours.clear();
		}
		if (_DOYValues != null) {
			_DOYValues.clear();
		}
		if (_tourSpeed != null) {
			_tourSpeed.clear();
		}

		_DOYValues = new ArrayList<Integer>(); // DOY...Day Of Year
		_tourSpeed = new ArrayList<Integer>();
		_allTours = new ArrayList<TVICatalogComparedTour>();

		final Object[] yearItems = _currentRefItem.getFetchedChildrenAsArray();

		// get youngest year if this is forced
		if (yearItems != null && yearItems.length > 0 && showYoungestYear) {
			final Object item = yearItems[yearItems.length - 1];
			if (item instanceof TVICatalogYearItem) {
				final TVICatalogYearItem youngestYearItem = (TVICatalogYearItem) item;
				_youngesYear = youngestYearItem.year;
			}
		}

		final int firstYear = _youngesYear - _numberOfYears + 1;

		// loop: all years
		for (final Object yearItemObj : yearItems) {
			if (yearItemObj instanceof TVICatalogYearItem) {

				final TVICatalogYearItem yearItem = (TVICatalogYearItem) yearItemObj;

				// check if the year can be displayed
				final int yearItemYear = yearItem.year;
				if (yearItemYear >= firstYear && yearItemYear <= _youngesYear) {

					// loop: all tours
					final Object[] tourItems = yearItem.getFetchedChildrenAsArray();
					for (final Object tourItemObj : tourItems) {
						if (tourItemObj instanceof TVICatalogComparedTour) {

							final TVICatalogComparedTour tourItem = (TVICatalogComparedTour) tourItemObj;

							final DateTime dt = new DateTime(tourItem.getTourDate());

							_DOYValues.add(getYearDOYs(dt.getYear()) + dt.getDayOfYear() - 1);
							_tourSpeed.add((int) (tourItem.getTourSpeed() * 10 / UI.UNIT_VALUE_DISTANCE));
							_allTours.add(tourItem);
						}
					}
				}
			}
		}

		final ChartDataModel chartModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		final ChartDataXSerie xData = new ChartDataXSerie(ArrayListToArray.toInt(_DOYValues));
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_DAY);
		xData.setChartSegments(createChartSegments());
		chartModel.setXData(xData);

		// set the bar low/high data
		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_BAR,
				ArrayListToArray.toInt(_tourSpeed));
		yData.setValueDivisor(10);
		TourManager.setGraphColor(prefStore, yData, GraphColorProvider.PREF_GRAPH_SPEED);

		/*
		 * set/restore min/max values
		 */
		final TVICatalogRefTourItem refItem = _currentRefItem;
		final int minValue = yData.getVisibleMinValue();
		final int maxValue = yData.getVisibleMaxValue();

		final int dataMinValue = minValue - (minValue / 10);
		final int dataMaxValue = maxValue + (maxValue / 20);

		if (_isSynchMaxValue) {

			if (refItem.yearMapMinValue == Integer.MIN_VALUE) {

				// min/max values have not yet been saved

				/*
				 * set the min value 10% below the computed so that the lowest value is not at the
				 * bottom
				 */
				yData.setVisibleMinValue(dataMinValue);
				yData.setVisibleMaxValue(dataMaxValue);

				refItem.yearMapMinValue = dataMinValue;
				refItem.yearMapMaxValue = dataMaxValue;

			} else {

				/*
				 * restore min/max values, but make sure min/max values for the current graph are
				 * visible and not outside of the chart
				 */

				refItem.yearMapMinValue = Math.min(refItem.yearMapMinValue, dataMinValue);
				refItem.yearMapMaxValue = Math.max(refItem.yearMapMaxValue, dataMaxValue);

				yData.setVisibleMinValue(refItem.yearMapMinValue);
				yData.setVisibleMaxValue(refItem.yearMapMaxValue);
			}

		} else {
			yData.setVisibleMinValue(dataMinValue);
			yData.setVisibleMaxValue(dataMaxValue);
		}

		yData.setYTitle(Messages.tourCatalog_view_label_year_chart_title);
		yData.setUnitLabel(UI.UNIT_LABEL_SPEED);

		chartModel.addYData(yData);

		// set tool tip info
		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
				return createToolTipInfo(valueIndex);
			}
		});

		// set grid size
		_yearChart.setGridDistance(
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE));

		// show the data in the chart
		_yearChart.updateChart(chartModel, false, true);
	}
}
