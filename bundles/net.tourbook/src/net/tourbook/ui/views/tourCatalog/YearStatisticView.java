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
package net.tourbook.ui.views.tourCatalog;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartStatisticSegments;
import net.tourbook.chart.ChartToolTipInfo;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.util.ArrayListToArray;
import net.tourbook.common.util.IToolTipHideListener;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourInfoIconToolTipProvider;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

public class YearStatisticView extends ViewPart {

	public static final String					ID									= "net.tourbook.views.tourCatalog.yearStatisticView";									//$NON-NLS-1$

	private static final String					GRAPH_LABEL_HEARTBEAT				= net.tourbook.common.Messages.Graph_Label_Heartbeat;
	private static final String					GRAPH_LABEL_HEARTBEAT_UNIT			= net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit;

	private final boolean						_isOSX								= net.tourbook.common.UI.IS_OSX;
	private final boolean						_isLinux							= net.tourbook.common.UI.IS_LINUX;

	static final String							STATE_NUMBER_OF_YEARS				= "numberOfYearsToDisplay";															//$NON-NLS-1$

	private static final String					GRID_PREF_PREFIX					= "GRID_REF_TOUR_YEAR_STATISTIC__";													//$NON-NLS-1$

	private static final String					GRID_IS_SHOW_VERTICAL_GRIDLINES		= (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES);
	private static final String					GRID_IS_SHOW_HORIZONTAL_GRIDLINES	= (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES);
	private static final String					GRID_VERTICAL_DISTANCE				= (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE);
	private static final String					GRID_HORIZONTAL_DISTANCE			= (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE);

	private final IPreferenceStore				_prefStore							= TourbookPlugin.getPrefStore();
	private final IDialogSettings				_state								= TourbookPlugin
																							.getState("TourCatalogViewYearStatistic");										//$NON-NLS-1$

	private IPropertyChangeListener				_prefChangeListener;
	private IPartListener2						_partListener;
	private ISelectionListener					_postSelectionListener;
	private PostSelectionProvider				_postSelectionProvider;

	private final DateFormat					_dtFormatter						= DateFormat
																							.getDateInstance(DateFormat.FULL);
	private NumberFormat						_nf1								= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	private int[]								_displayedYears;

	private int[]								_numberOfDaysInYear;

	/**
	 * contains all {@link TVICatalogComparedTour} tour objects for all years
	 */
	private ArrayList<TVICatalogComparedTour>	_allTours							= new ArrayList<TVICatalogComparedTour>();

	/**
	 * Years which the user can select as start year in the combo box
	 */
	private ArrayList<Integer>					_comboYears							= new ArrayList<Integer>();

	/**
	 * Day of year values for all displayed years<br>
	 * DOY...Day Of Year
	 */
	private ArrayList<Integer>					_DOYValues							= new ArrayList<Integer>();

	/**
	 * Tour speed for all years
	 */
	private ArrayList<Float>					_tourSpeed							= new ArrayList<Float>();

	/**
	 * Average pulse for all years.
	 */
	private ArrayList<Float>					_avgPulse							= new ArrayList<Float>();

	/**
	 * this is the last year (on the right side) which is displayed in the statistics
	 */
	private int									_lastYear							= new DateTime().getYear();

	/**
	 * year item for the visible statistics
	 */
	private TVICatalogRefTourItem				_currentRefItem;

	/**
	 * selection which is thrown by the year statistic
	 */
	private StructuredSelection					_currentSelection;

	private ITourEventListener					_tourEventListener;

	private boolean								_isSynchMaxValue;

	private int									_numberOfYears;

	/**
	 * Contains the index in {@link #_allTours} for the currently selected tour.
	 */
	private int									_selectedTourIndex;

	private IAction								_actionSynchChartScale;
	private Action_RefTour_YearStatisticOptions	_actionYearStatOptions;

	private YearStatisticTourToolTip			_tourToolTip;
	private TourInfoIconToolTipProvider			_tourInfoToolTipProvider			= new TourInfoIconToolTipProvider();

	/*
	 * UI controls
	 */
	private PageBook							_pageBook;
	private Label								_pageNoChart;

	private Chart								_yearChart;

	private Composite							_toolbar;
	private Combo								_cboLastYear;
	private Combo								_cboNumberOfYears;

	private Composite							_pageChart;

	public YearStatisticView() {}

	void actionSynchScale(final boolean isSynchMaxValue) {
		_isSynchMaxValue = isSynchMaxValue;
		updateUI_YearChart(false);
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == YearStatisticView.this) {
					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					net.tourbook.ui.UI.updateUnits();

					// recreate the chart
					_yearChart.dispose();
					createUI_30_Chart(_pageChart);

					_pageChart.layout();

					updateUI_YearChart(false);

				} else if (property.equals(GRID_HORIZONTAL_DISTANCE)
						|| property.equals(GRID_VERTICAL_DISTANCE)
						|| property.equals(GRID_IS_SHOW_HORIZONTAL_GRIDLINES)
						|| property.equals(GRID_IS_SHOW_VERTICAL_GRIDLINES)
				//
				) {

					updateUI_YearChart(false);
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
			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				// prevent to listen to a selection which is originated by this year chart
				if (selection != _currentSelection) {
					onSelectionChanged(selection);
				}
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId propertyId, final Object propertyData) {

				if (propertyId == TourEventId.COMPARE_TOUR_CHANGED
						&& propertyData instanceof TourPropertyCompareTourChanged) {

					final TourPropertyCompareTourChanged compareTourProperty = (TourPropertyCompareTourChanged) propertyData;

					if (compareTourProperty.isDataSaved) {
						updateUI_YearChart(false);
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	/**
	 * Set/restore min/max values.
	 */
	private void computeMinMaxValues(final ChartDataYSerie yData) {

		final TVICatalogRefTourItem refItem = _currentRefItem;
		final float minValue = (float) yData.getVisibleMinValue();
		final float maxValue = (float) yData.getVisibleMaxValue();

		final float dataMinValue = minValue;// - (minValue / 100);
		final float dataMaxValue = maxValue;// + (maxValue / 100);

		if (_isSynchMaxValue) {

			if (refItem.yearMapMinValue == Float.MIN_VALUE) {

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
	}

	private void createActions() {

		_actionSynchChartScale = new ActionSynchYearScale(this);
		_actionYearStatOptions = new Action_RefTour_YearStatisticOptions(_pageBook, GRID_PREF_PREFIX);

		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(_actionSynchChartScale);
		tbm.add(_actionYearStatOptions);

		tbm.update(true);
	}

	/**
	 * create segments for the chart
	 */
	private ChartStatisticSegments createChartSegments() {

		final double segmentStart[] = new double[_numberOfYears];
		final double segmentEnd[] = new double[_numberOfYears];
		final String[] segmentTitle = new String[_numberOfYears];

		final int firstYear = getFirstYear();
		int yearDaysSum = 0;

		// create segments for each year
		for (int yearDayIndex = 0; yearDayIndex < _numberOfDaysInYear.length; yearDayIndex++) {

			final int yearDays = _numberOfDaysInYear[yearDayIndex];

			segmentStart[yearDayIndex] = yearDaysSum;
			segmentEnd[yearDayIndex] = yearDaysSum + yearDays - 1;
			segmentTitle[yearDayIndex] = Integer.toString(firstYear + yearDayIndex);

			yearDaysSum += yearDays;
		}

		final ChartStatisticSegments chartSegments = new ChartStatisticSegments();
		chartSegments.segmentStartValue = segmentStart;
		chartSegments.segmentEndValue = segmentEnd;
		chartSegments.segmentTitle = segmentTitle;

		chartSegments.years = _displayedYears;
		chartSegments.yearDays = _numberOfDaysInYear;
		chartSegments.allValues = yearDaysSum;

		return chartSegments;
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		addSelectionListener();
		addTourEventListener();
		addPrefListener();
		addPartListener();

		createActions();

		// set selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

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
		final int firstYear = getFirstYear();
		final int tourDOY = _DOYValues.get(valueIndex);
		final DateTime tourDate = new DateTime(firstYear, 1, 1, 0, 0, 0, 1).plusDays(tourDOY);

		final StringBuilder toolTipFormat = new StringBuilder();
		toolTipFormat.append(Messages.tourCatalog_view_tooltip_speed);
		toolTipFormat.append(UI.NEW_LINE);

		final String ttText = UI.EMPTY_STRING
				+ String.format(Messages.tourCatalog_view_tooltip_speed, _nf1.format(_tourSpeed.get(valueIndex)))
				+ UI.NEW_LINE
				+ String.format(Messages.Year_Statistic_Tooltip_Pulse, _avgPulse.get(valueIndex));

		final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();

		toolTipInfo.setTitle(_dtFormatter.format(tourDate.toDate()));
		toolTipInfo.setLabel(ttText);

		return toolTipInfo;
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);

		_pageNoChart = new Label(_pageBook, SWT.NONE);
		_pageNoChart.setText(Messages.tourCatalog_view_label_year_not_selected);

		createUI_10_PageYearChart();
	}

	private void createUI_10_PageYearChart() {

		_pageChart = new Composite(_pageBook, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageChart);
		GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(1).applyTo(_pageChart);
		{
			createUI_20_Toolbar(_pageChart);
			createUI_30_Chart(_pageChart);
		}
	}

	/**
	 * toolbar
	 */
	private void createUI_20_Toolbar(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);

		_toolbar = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_toolbar);
		GridLayoutFactory.fillDefaults()//
				.numColumns(3)
				.extendedMargins(0, 0, 0, 1)
				.spacing(0, 0)
				.applyTo(_toolbar);

		{
			/*
			 * combo: last year
			 */
			_cboLastYear = new Combo(_toolbar, SWT.DROP_DOWN | SWT.READ_ONLY);
			GridDataFactory.fillDefaults()//
					.hint(pc.convertWidthInCharsToPixels(_isOSX ? 12 : _isLinux ? 12 : 5), SWT.DEFAULT)
					.applyTo(_cboLastYear);
			_cboLastYear.setToolTipText(Messages.Year_Statistic_Combo_LastYears_Tooltip);
			_cboLastYear.setVisibleItemCount(50);
			_cboLastYear.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectYear();
				}
			});

			/*
			 * number of years
			 */
			// label
			final Label label = new Label(_toolbar, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(10, 0)
					.applyTo(label);
			label.setText(Messages.Year_Statistic_Label_NumberOfYears);

			// combo
			_cboNumberOfYears = new Combo(_toolbar, SWT.DROP_DOWN | SWT.READ_ONLY);
			GridDataFactory.fillDefaults()//
					.indent(2, 0)
					.hint(pc.convertWidthInCharsToPixels(_isOSX ? 8 : _isLinux ? 8 : 4), SWT.DEFAULT)
					.applyTo(_cboNumberOfYears);
			_cboNumberOfYears.setToolTipText(Messages.Year_Statistic_Combo_NumberOfYears_Tooltip);
			_cboNumberOfYears.setVisibleItemCount(50);
			_cboNumberOfYears.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectNumberOfYears(getSelectedYears());
				}
			});
		}

		// fill combo box
		for (int year = 1; year <= 50; year++) {
			_cboNumberOfYears.add(Integer.toString(year));
		}
	}

	/**
	 * year chart
	 */
	private void createUI_30_Chart(final Composite parent) {

		_yearChart = new Chart(parent, SWT.BORDER);

		_yearChart.addBarSelectionListener(new IBarSelectionListener() {
			@Override
			public void selectionChanged(final int serieIndex, final int valueIndex) {

				if (_allTours.size() == 0) {
					_tourInfoToolTipProvider.setTourId(-1);
					return;
				}

				// ensure list size
				_selectedTourIndex = Math.min(valueIndex, _allTours.size() - 1);

				// select tour in the tour viewer & show tour in compared tour char
				final TVICatalogComparedTour tourCatalogComparedTour = _allTours.get(_selectedTourIndex);
				_currentSelection = new StructuredSelection(tourCatalogComparedTour);
				_postSelectionProvider.setSelection(_currentSelection);

				_tourInfoToolTipProvider.setTourId(tourCatalogComparedTour.getTourId());
			}
		});

		// set tour info icon into the left axis
		_tourToolTip = new YearStatisticTourToolTip(_yearChart.getToolTipControl());
		_tourToolTip.addToolTipProvider(_tourInfoToolTipProvider);
		_tourToolTip.addHideListener(new IToolTipHideListener() {
			@Override
			public void afterHideToolTip(final Event event) {
				// hide hovered image
				_yearChart.getToolTipControl().afterHideToolTip(event);
			}
		});

		_yearChart.setTourInfoIconToolTipProvider(_tourInfoToolTipProvider);
		_tourInfoToolTipProvider.setActionsEnabled(true);

	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);
		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private int getFirstYear() {
		return _lastYear - _numberOfYears + 1;
	}

	private int getSelectedYears() {
		return _cboNumberOfYears.getSelectionIndex() + 1;
	}

	/**
	 * @param currentYear
	 * @param numberOfYears
	 * @return Returns the number of days between {@link #fLastYear} and currentYear
	 */
	int getYearDOYs(final int selectedYear) {

		int yearDOYs = 0;
		int yearIndex = 0;

		final int firstYear = getFirstYear();

		for (int currentYear = firstYear; currentYear < selectedYear; currentYear++) {

			if (currentYear == selectedYear) {
				return yearDOYs;
			}

			yearDOYs += _numberOfDaysInYear[yearIndex];

			yearIndex++;
		}

		return yearDOYs;
	}

	/**
	 * get numbers for each year <br>
	 * <br>
	 * all years into {@link #fYears} <br>
	 * number of day's into {@link #_numberOfDaysInYear} <br>
	 * number of week's into {@link #fYearWeeks}
	 */
	void initYearNumbers() {

	}

	TVICatalogComparedTour navigateTour(final boolean isNextTour) {

		final int numberOfTours = _allTours.size();

		if (numberOfTours < 2) {
			return null;
		}

		int navIndex;
		if (isNextTour) {

			// get nexttour

			if (_selectedTourIndex >= numberOfTours - 1) {

				navIndex = 0;

			} else {

				navIndex = _selectedTourIndex + 1;
			}

		} else {

			// get previous tour

			if (_selectedTourIndex <= 0) {

				navIndex = numberOfTours - 1;

			} else {

				navIndex = _selectedTourIndex - 1;
			}
		}

		return _allTours.get(navIndex);
	}

	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogItem = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogItem.getRefItem();
			if (refItem != null) {

				// reference tour is selected

				_currentRefItem = refItem;
				updateUI_YearChart(true);

			} else {

				// show statistic for a specific year

				final TVICatalogYearItem yearItem = tourCatalogItem.getYearItem();
				if (yearItem != null) {

					_currentRefItem = yearItem.getRefItem();

					// overwrite last year
					_lastYear = yearItem.year;

					// update year data
					setYearData();

					updateUI_YearChart(false);
				}
			}

			// select tour in the statistic
			final Long compTourId = tourCatalogItem.getCompTourId();
			if (compTourId != null) {

				selectTourInYearChart(compTourId);

			} else {

				// select first tour for the youngest year
				int yearIndex = 0;
				for (final TVICatalogComparedTour tourItem : _allTours) {

					if (new DateTime(tourItem.getTourDate()).getYear() == _lastYear) {
						break;
					}
					yearIndex++;
				}

				final int allTourSize = _allTours.size();

				if (allTourSize > 0 && yearIndex < allTourSize) {
					selectTourInYearChart(_allTours.get(yearIndex).getTourId());
				}
			}

		} else if (selection instanceof StructuredSelection) {

			final StructuredSelection structuredSelection = (StructuredSelection) selection;

			if (structuredSelection.size() > 0) {
				final Object firstElement = structuredSelection.getFirstElement();
				if (firstElement instanceof TVICatalogComparedTour) {

					final TVICatalogComparedTour compareItem = (TVICatalogComparedTour) firstElement;

					// select tour in the year chart
					final Long compTourId = compareItem.getTourId();
					if (compTourId != null) {
						selectTourInYearChart(compTourId);
					}
				}
			}

		} else if (selection instanceof SelectionRemovedComparedTours) {

			final SelectionRemovedComparedTours removedCompTours = (SelectionRemovedComparedTours) selection;

			if (removedCompTours.removedComparedTours.size() > 0) {
				updateUI_YearChart(false);
			}
		}
	}

	/**
	 * Update statistic by setting number of years
	 * 
	 * @param numberOfYears
	 */
	private void onSelectNumberOfYears(final int numberOfYears) {

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

		updateUI_YearChart(false);

		// reselect last selected tour
		selectTourInYearChart(selectedTourId);
	}

	private void onSelectYear() {

		// overwrite last year
		_lastYear = _comboYears.get(_cboLastYear.getSelectionIndex());

		// update year data
		setYearData();

		updateUI_YearChart(false);
	}

	private void restoreState() {

		// select previous value
		final int selectedYear = Util.getStateInt(_state, YearStatisticView.STATE_NUMBER_OF_YEARS, 3);
		_cboNumberOfYears.select(Math.min(selectedYear - 1, _cboNumberOfYears.getItemCount() - 1));

		_numberOfYears = getSelectedYears();

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
	private void selectTourInYearChart(final long selectedTourId) {

		if (_allTours.size() == 0) {
			_tourInfoToolTipProvider.setTourId(-1);
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

		_displayedYears = new int[_numberOfYears];
		_numberOfDaysInYear = new int[_numberOfYears];

		final int firstYear = getFirstYear();

		final DateTime dt = (new DateTime())
				.withYear(firstYear)
				.withWeekOfWeekyear(1)
				.withDayOfWeek(DateTimeConstants.MONDAY);

		int yearIndex = 0;
		for (int currentYear = firstYear; currentYear <= _lastYear; currentYear++) {

			_displayedYears[yearIndex] = currentYear;
			_numberOfDaysInYear[yearIndex] = dt.withYear(currentYear).dayOfYear().getMaximumValue();

			yearIndex++;
		}
	}

	/**
	 * show statistic for several years
	 * 
	 * @param isShowLatestYear
	 *            shows the latest year and the years before
	 */
	private void updateUI_YearChart(final boolean isShowLatestYear) {

		if (_currentRefItem == null) {
			return;
		}

		_pageBook.showPage(_pageChart);

		final Object[] yearItems = _currentRefItem.getFetchedChildrenAsArray();

		// get the last year when it's forced
		if (isShowLatestYear && yearItems != null && yearItems.length > 0) {

			final Object firstItem = yearItems[0];
			final Object lastItem = yearItems[yearItems.length - 1];

			if (lastItem instanceof TVICatalogYearItem) {

				final int newFirstYear = ((TVICatalogYearItem) firstItem).year;
				final int newLastYear = ((TVICatalogYearItem) lastItem).year;

				/*
				 * Use current years when the new items are in the current range, otherwise adjust
				 * the years
				 */
				if (newLastYear <= _lastYear && newFirstYear >= _lastYear - _numberOfYears) {

					// new years are within the current year range

				} else {

					_lastYear = newLastYear;
				}
			}
		}

		final int firstYear = getFirstYear();

		/**
		 * Create data for all years
		 */
		_allTours.clear();
		_DOYValues.clear();

		_avgPulse.clear();
		_tourSpeed.clear();

		for (final Object yearItemObj : yearItems) {
			if (yearItemObj instanceof TVICatalogYearItem) {

				final TVICatalogYearItem yearItem = (TVICatalogYearItem) yearItemObj;

				// check if the year can be displayed
				final int yearItemYear = yearItem.year;
				if (yearItemYear >= firstYear && yearItemYear <= _lastYear) {

					// loop: all tours
					final Object[] tourItems = yearItem.getFetchedChildrenAsArray();
					for (final Object tourItemObj : tourItems) {
						if (tourItemObj instanceof TVICatalogComparedTour) {

							final TVICatalogComparedTour tourItem = (TVICatalogComparedTour) tourItemObj;

							final DateTime dt = new DateTime(tourItem.getTourDate());

							_allTours.add(tourItem);
							_DOYValues.add(getYearDOYs(dt.getYear()) + dt.getDayOfYear() - 1);

							_avgPulse.add(tourItem.getAvgPulse());
							_tourSpeed.add(tourItem.getTourSpeed() / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE);
						}
					}
				}
			}
		}

		final ChartDataModel chartModel = new ChartDataModel(ChartType.BAR);

		final ChartDataXSerie xData = new ChartDataXSerie(ArrayListToArray.integerToDouble(_DOYValues));
		xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_DAY);
		xData.setChartSegments(createChartSegments());
		chartModel.setXData(xData);

		/**
		 * Speed
		 */
		// set the bar low/high data
		final ChartDataYSerie yDataSpeed = new ChartDataYSerie(
				ChartType.BAR,
				ArrayListToArray.toFloat(_tourSpeed),
				true);
		computeMinMaxValues(yDataSpeed);

		TourManager.setGraphColor(yDataSpeed, GraphColorManager.PREF_GRAPH_SPEED);

		yDataSpeed.setYTitle(Messages.tourCatalog_view_label_year_chart_title);
		yDataSpeed.setUnitLabel(UI.UNIT_LABEL_SPEED);

		/*
		 * ensure that painting of the bar is started at the bottom and not at the visible min which
		 * is above the bottom !!!
		 */
		yDataSpeed.setGraphFillMethod(ChartDataYSerie.BAR_DRAW_METHOD_BOTTOM);

		chartModel.addYData(yDataSpeed);

		/**
		 * Pulse
		 */
		// set the bar low/high data
		final ChartDataYSerie yDataPulse = new ChartDataYSerie(ChartType.BAR, ArrayListToArray.toFloat(_avgPulse), true);
		computeMinMaxValues(yDataPulse);

		TourManager.setGraphColor(yDataPulse, GraphColorManager.PREF_GRAPH_HEARTBEAT);

		yDataPulse.setYTitle(GRAPH_LABEL_HEARTBEAT);
		yDataPulse.setUnitLabel(GRAPH_LABEL_HEARTBEAT_UNIT);

		/*
		 * ensure that painting of the bar is started at the bottom and not at the visible min which
		 * is above the bottom !!!
		 */
		yDataPulse.setGraphFillMethod(ChartDataYSerie.BAR_DRAW_METHOD_BOTTOM);

		chartModel.addYData(yDataPulse);

		/**
		 * Setup UI
		 */
		// set tool tip info
		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
			@Override
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
				return createToolTipInfo(valueIndex);
			}
		});

		net.tourbook.ui.UI.updateChartProperties(_yearChart, GRID_PREF_PREFIX);

		// show the data in the chart
		_yearChart.updateChart(chartModel, false, true);

		/*
		 * update start year combo box
		 */
		_cboLastYear.removeAll();
		_comboYears.clear();

		for (int year = firstYear - 1; year <= _lastYear + _numberOfYears; year++) {
			_cboLastYear.add(Integer.toString(year));
			_comboYears.add(year);
		}

		_cboLastYear.select(_numberOfYears - 0);
	}
}
