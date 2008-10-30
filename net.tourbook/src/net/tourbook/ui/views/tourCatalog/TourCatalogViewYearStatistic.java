/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourProperty;
import net.tourbook.ui.UI;
import net.tourbook.util.ArrayListToArray;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

public class TourCatalogViewYearStatistic extends ViewPart {

	public static final String					ID						= "net.tourbook.views.tourCatalog.yearStatisticView";	//$NON-NLS-1$

	private static final String					MEMENTO_NUMBER_OF_YEARS	= "numberOfYearsToDisplay";							//$NON-NLS-1$

	private Chart								fYearChart;

	private ISelectionListener					fPostSelectionListener;
	private PostSelectionProvider				fPostSelectionProvider;

	private final DateFormat					fDateFormatter			= DateFormat.getDateInstance(DateFormat.FULL);
	private NumberFormat						fNumberFormatter		= NumberFormat.getNumberInstance();
	/**
	 * contains all {@link TVICatalogComparedTour} tour objects for all years
	 */
	private ArrayList<TVICatalogComparedTour>	fAllTours;

	/**
	 * year item for the visible statistics
	 */
	private TVICatalogReferenceTour				fCurrentRefItem;

	private PageBook							fPageBook;

	private Label								fPageNoChart;
	/**
	 * selection which is thrown by the year statistic
	 */
	private StructuredSelection					fCurrentSelection;

	private ITourPropertyListener				fCompareTourPropertyListener;

	private IPropertyChangeListener				fPrefChangeListener;
	private IPartListener2						fPartListener;
	private IAction								fActionSynchChartScale;

	private ActionSelectYears					fActionSelectYears;
	private boolean								fIsSynchMaxValue;

	private IMemento							fSessionMemento;

	private int									fNumberOfYears;

	private int									fYoungesYear			= new DateTime().getYear();
	private int[]								fAllYears;

	private int[]								fYearDays;
	/**
	 * Day of year values for all years
	 */
	private ArrayList<Integer>					fDOYValues;

	/**
	 * Tour speed for all years
	 */
	private ArrayList<Integer>					fTourSpeed;

	protected int								fSelectedTourIndex;

	{
		fNumberFormatter.setMinimumFractionDigits(1);
		fNumberFormatter.setMaximumFractionDigits(1);
	}

	public TourCatalogViewYearStatistic() {}

	public void actionSynchScale(final boolean isSynchMaxValue) {
		fIsSynchMaxValue = isSynchMaxValue;
		updateYearBarChart(false);
	}

	private void addCompareTourPropertyListener() {

		fCompareTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(final IWorkbenchPart part,
										final TourProperty propertyId,
										final Object propertyData) {

				if (propertyId == TourProperty.TOUR_PROPERTY_COMPARE_TOUR_CHANGED
						&& propertyData instanceof TourPropertyCompareTourChanged) {

					final TourPropertyCompareTourChanged compareTourProperty = (TourPropertyCompareTourChanged) propertyData;

					if (compareTourProperty.isDataSaved) {
						updateYearBarChart(false);
					}
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fCompareTourPropertyListener);
	}

	private void addPartListener() {

		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourCatalogViewYearStatistic.this) {
					saveSession();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					// recreate the chart
					fYearChart.dispose();
					createYearChart();

					updateYearBarChart(false);
				}
			}
		};
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				// prevent to listen to a selection which is originated by this year chart
				if (selection != fCurrentSelection) {
					onSelectionChanged(selection);
				}
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void createActions() {

		fActionSynchChartScale = new ActionSynchYearScale(this);
		fActionSelectYears = new ActionSelectYears(this);

		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(fActionSynchChartScale);

		final IMenuManager mm = getViewSite().getActionBars().getMenuManager();
		mm.add(fActionSelectYears);
	}

	/**
	 * create segments for the chart
	 */
	ChartSegments createChartSegments() {

		final int segmentStart[] = new int[fNumberOfYears];
		final int segmentEnd[] = new int[fNumberOfYears];
		final String[] segmentTitle = new String[fNumberOfYears];

		final int oldestYear = fYoungesYear - fNumberOfYears + 1;
		int yearDaysSum = 0;

		// create segments for each year
		for (int yearDayIndex = 0; yearDayIndex < fYearDays.length; yearDayIndex++) {

			final int yearDays = fYearDays[yearDayIndex];

			segmentStart[yearDayIndex] = yearDaysSum;
			segmentEnd[yearDayIndex] = yearDaysSum + yearDays - 1;
			segmentTitle[yearDayIndex] = Integer.toString(oldestYear + yearDayIndex);

			yearDaysSum += yearDays;
		}

		final ChartSegments chartSegments = new ChartSegments();
		chartSegments.valueStart = segmentStart;
		chartSegments.valueEnd = segmentEnd;
		chartSegments.segmentTitle = segmentTitle;

		chartSegments.years = fAllYears;
		chartSegments.yearDays = fYearDays;
		chartSegments.allValues = yearDaysSum;

		return chartSegments;
	}

	@Override
	public void createPartControl(final Composite parent) {

		fPageBook = new PageBook(parent, SWT.NONE);

		fPageNoChart = new Label(fPageBook, SWT.NONE);
		fPageNoChart.setText(Messages.tourCatalog_view_label_year_not_selected);

		createYearChart();

		addSelectionListener();
		addCompareTourPropertyListener();
		addPrefListener();
		addPartListener();

		createActions();

		// set selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fPageBook.showPage(fPageNoChart);

		restoreState(fSessionMemento);

		// restore selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

	}

	private ChartToolTipInfo createToolTipInfo(int valueIndex) {

		if (valueIndex >= fDOYValues.size()) {
			valueIndex -= fDOYValues.size();
		}

		if (fDOYValues == null || valueIndex >= fDOYValues.size()) {
			return null;
		}

		/*
		 * set calendar day/month/year
		 */
		final int oldestYear = fYoungesYear - fNumberOfYears + 1;
		final int tourDOY = fDOYValues.get(valueIndex);
		final DateTime tourDate = new DateTime(oldestYear, 1, 1, 0, 0, 0, 1).plusDays(tourDOY);

		final StringBuilder toolTipFormat = new StringBuilder();
		toolTipFormat.append(Messages.tourCatalog_view_tooltip_speed);
		toolTipFormat.append(UI.NEW_LINE);

		final String toolTipLabel = new Formatter().format(toolTipFormat.toString(),
				fNumberFormatter.format((float) fTourSpeed.get(valueIndex) / 10)).toString();

		final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
		toolTipInfo.setTitle(fDateFormatter.format(tourDate.toDate()));
		toolTipInfo.setLabel(toolTipLabel);

		return toolTipInfo;
	}

	private void createYearChart() {

		// year chart
		fYearChart = new Chart(fPageBook, SWT.NONE);

		fYearChart.addBarSelectionListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, final int valueIndex) {

				if (fAllTours.size() == 0) {
					return;
				}

				// ensure list size
				fSelectedTourIndex = Math.min(valueIndex, fAllTours.size() - 1);

				// select the tour in the tour viewer & show tour in compared tour char
				final TVICatalogComparedTour tourCatalogComparedTour = fAllTours.get(fSelectedTourIndex);
				fCurrentSelection = new StructuredSelection(tourCatalogComparedTour);
				fPostSelectionProvider.setSelection(fCurrentSelection);
			}
		});
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getViewSite().getPage().removePartListener(fPartListener);
		TourManager.getInstance().removePropertyListener(fCompareTourPropertyListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

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

		final int firstYear = fYoungesYear - fNumberOfYears + 1;

		for (int currentYear = firstYear; currentYear < selectedYear; currentYear++) {

			if (currentYear == selectedYear) {
				return yearDOYs;
			}

			yearDOYs += fYearDays[yearIndex];

			yearIndex++;
		}

		return yearDOYs;
	}

	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {

		super.init(site, memento);

		// set the session memento
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	/**
	 * get numbers for each year <br>
	 * <br>
	 * all years into {@link #fYears} <br>
	 * number of day's into {@link #fYearDays} <br>
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
		if (fAllTours.size() == 0) {
			selectedTourId = -1;
		} else {
			final int selectedTourIndex = Math.min(fSelectedTourIndex, fAllTours.size() - 1);
			selectedTourId = fAllTours.get(selectedTourIndex).getTourId();
		}

		fNumberOfYears = numberOfYears;
		setYearData();

		updateYearBarChart(false);

		// reselect last selected tour
		selectTourInYearStatistic(selectedTourId);
	}

	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogItem = (SelectionTourCatalogView) selection;

			final TVICatalogReferenceTour refItem = tourCatalogItem.getRefItem();
			if (refItem != null) {

				// reference tour is selected

				fCurrentRefItem = refItem;
				updateYearBarChart(true);

			} else {

				// show year statistic

				final TVICatalogYearItem yearItem = tourCatalogItem.getYearItem();
				if (yearItem != null) {

					fCurrentRefItem = yearItem.getRefItem();
					fYoungesYear = yearItem.year;

					setYearData();
					updateYearBarChart(false);
				}
			}

			// select tour in the statistic
			final Long compTourId = tourCatalogItem.getCompTourId();
			if (compTourId != null) {

				selectTourInYearStatistic(compTourId);

			} else if (fAllTours != null) {

				// select first tour for the youngest year
				int yearIndex = 0;
				for (final TVICatalogComparedTour tourItem : fAllTours) {

					if (new DateTime(tourItem.getTourDate()).getYear() == fYoungesYear) {
						break;
					}
					yearIndex++;
				}

				if (fAllTours.size() > 0 && fAllTours.size() >= yearIndex) {
					selectTourInYearStatistic(fAllTours.get(yearIndex).getTourId());
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

	private void restoreState(final IMemento memento) {

		if (memento != null) {

			final Integer mementoNumberOfYears = memento.getInteger(MEMENTO_NUMBER_OF_YEARS);

			if (mementoNumberOfYears == null) {
				fActionSelectYears.setNumberOfYears(3);
			} else {
				fActionSelectYears.setNumberOfYears(mementoNumberOfYears);
			}

		} else {

			fActionSelectYears.setNumberOfYears(3);
		}

		fNumberOfYears = fActionSelectYears.getSelectedYear();

		/*
		 * reselect again because there is somewhere a bug because the first time setting the
		 * checkmark for the year does not work
		 */
		fActionSelectYears.setNumberOfYears(fNumberOfYears);

		setYearData();
	}

	private void saveSession() {
		fSessionMemento = XMLMemento.createWriteRoot("TourCatalogViewYearStatistic"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	@Override
	public void saveState(final IMemento memento) {

		// save number of years which are displayed
		memento.putInteger(MEMENTO_NUMBER_OF_YEARS, fNumberOfYears);

	}

	/**
	 * select the tour in the year map chart
	 * 
	 * @param selectedTourId
	 *            tour id which should be selected
	 */
	private void selectTourInYearStatistic(final long selectedTourId) {

		if (fAllTours == null || fAllTours.size() == 0) {
			return;
		}

		final int tourLength = fAllTours.size();
		final boolean[] selectedTours = new boolean[tourLength];
		boolean isTourSelected = false;

		for (int tourIndex = 0; tourIndex < tourLength; tourIndex++) {
			final TVICatalogComparedTour comparedItem = fAllTours.get(tourIndex);
			if (comparedItem.getTourId() == selectedTourId) {
				selectedTours[tourIndex] = true;
				isTourSelected = true;
			}
		}

		if (isTourSelected == false && selectedTours.length > 0) {
			// a tour is not selected, select first tour
			selectedTours[0] = true;
		}

		fYearChart.setSelectedBars(selectedTours);
	}

	@Override
	public void setFocus() {
		fYearChart.setFocus();
	}

	/**
	 * get data for each displayed year
	 */
	private void setYearData() {

		fYearDays = new int[fNumberOfYears];
		fAllYears = new int[fNumberOfYears];

		final int firstYear = fYoungesYear - fNumberOfYears + 1;

		final DateTime dt = (new DateTime()).withYear(firstYear)
				.withWeekOfWeekyear(1)
				.withDayOfWeek(DateTimeConstants.MONDAY);

		int yearIndex = 0;
		for (int currentYear = firstYear; currentYear <= fYoungesYear; currentYear++) {

			fAllYears[yearIndex] = currentYear;
			fYearDays[yearIndex] = dt.withYear(currentYear).dayOfYear().getMaximumValue();
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

		if (fCurrentRefItem == null) {
			return;
		}

		fPageBook.showPage(fYearChart);

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		fDOYValues = new ArrayList<Integer>(); // DOY...Day Of Year
		fTourSpeed = new ArrayList<Integer>();
		fAllTours = new ArrayList<TVICatalogComparedTour>();

		final Object[] yearItems = fCurrentRefItem.getFetchedChildrenAsArray();

		// get youngest year if this is forced
		if (yearItems != null && yearItems.length > 0 && showYoungestYear) {
			final Object item = yearItems[yearItems.length - 1];
			if (item instanceof TVICatalogYearItem) {
				final TVICatalogYearItem youngestYearItem = (TVICatalogYearItem) item;
				fYoungesYear = youngestYearItem.year;
			}
		}

		final int firstYear = fYoungesYear - fNumberOfYears + 1;

		// loop: all years
		for (final Object yearItemObj : yearItems) {
			if (yearItemObj instanceof TVICatalogYearItem) {

				final TVICatalogYearItem yearItem = (TVICatalogYearItem) yearItemObj;

				// check if the year can be displayed
				final int yearItemYear = yearItem.year;
				if (yearItemYear >= firstYear && yearItemYear <= fYoungesYear) {

					// loop: all tours
					final Object[] tourItems = yearItem.getFetchedChildrenAsArray();
					for (final Object tourItemObj : tourItems) {
						if (tourItemObj instanceof TVICatalogComparedTour) {

							final TVICatalogComparedTour tourItem = (TVICatalogComparedTour) tourItemObj;

							final DateTime dt = new DateTime(tourItem.getTourDate());

							fDOYValues.add(getYearDOYs(dt.getYear()) + dt.getDayOfYear() - 1);
							fTourSpeed.add((int) (tourItem.getTourSpeed() * 10 / UI.UNIT_VALUE_DISTANCE));
							fAllTours.add(tourItem);
						}
					}
				}
			}
		}

		final ChartDataModel chartModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		final ChartDataXSerie xData = new ChartDataXSerie(ArrayListToArray.toInt(fDOYValues));
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_DAY);
		xData.setChartSegments(createChartSegments());
		chartModel.setXData(xData);

		// set the bar low/high data
		final ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ArrayListToArray.toInt(fTourSpeed));
		yData.setValueDivisor(10);
		TourManager.setGraphColor(prefStore, yData, GraphColorProvider.PREF_GRAPH_SPEED);

		/*
		 * set/restore min/max values
		 */
		final TVICatalogReferenceTour refItem = fCurrentRefItem;
		final int minValue = yData.getVisibleMinValue();
		final int maxValue = yData.getVisibleMaxValue();

		final int dataMinValue = minValue - (minValue / 10);
		final int dataMaxValue = maxValue + (maxValue / 20);

		if (fIsSynchMaxValue) {

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
		fYearChart.setGridDistance(prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE));

		// show the data in the chart
		fYearChart.updateChart(chartModel, false);
	}
}
