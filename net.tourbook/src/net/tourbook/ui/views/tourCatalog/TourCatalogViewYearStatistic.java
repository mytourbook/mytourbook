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
package net.tourbook.ui.views.tourCatalog;

import java.sql.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourCatalogViewYearStatistic extends ViewPart {

	public static final String		ID	= "net.tourbook.views.tourCatalog.yearStatisticView";	//$NON-NLS-1$

	private Chart					fYearChart;

	private ISelectionListener		fPostSelectionListener;
	private PostSelectionProvider	fPostSelectionProvider;

	/**
	 * contains all {@link TourCatalogItemComparedTour} tour objects for the current year
	 */
	private Object[]				fYearMapTours;

	/**
	 * year item for the visible statistics
	 */
	private TourCatalogItemYear		fCurrentYearItem;

	private PageBook				fPageBook;
	private Label					fPageNoChart;

	/**
	 * selection which is thrown by the this year statistic
	 */
	private StructuredSelection		fCurrentSelection;

	private ITourPropertyListener	fCompareTourPropertyListener;

	private IAction					fActionSynchChartScale;

//	private final BarChartMinMaxKeeper	fMinMaxKeeper	= new BarChartMinMaxKeeper();
	private boolean					fIsSynchMaxValue;

	private IPropertyChangeListener	fPrefChangeListener;

	public TourCatalogViewYearStatistic() {}

	public void actionSynchScale(boolean isSynchMaxValue) {
		fIsSynchMaxValue = isSynchMaxValue;
		updateYearBarChart();
	}

	private void addCompareTourPropertyListener() {

		fCompareTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(int propertyId, Object propertyData) {

				if (propertyId == TourManager.TOUR_PROPERTY_COMPARE_TOUR_CHANGED
						&& propertyData instanceof TourPropertyCompareTourChanged) {

					TourPropertyCompareTourChanged compareTourProperty = (TourPropertyCompareTourChanged) propertyData;

					if (compareTourProperty.isDataSaved) {

						// force the children to be reloaded
						if (fCurrentYearItem != null) {
//							fCurrentYearItem.setChildren(null);
						}

						updateYearBarChart();
					}
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fCompareTourPropertyListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				// prevent to listen to a selection which is originated by this year chart
				if (selection != fCurrentSelection) {
					onSelectionChanged(selection);
				}
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
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

					updateYearBarChart();
				}
			}
		};
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	private void createActions() {

		fActionSynchChartScale = new ActionSynchYearScale(this);

		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(fActionSynchChartScale);
	}

	@Override
	public void createPartControl(Composite parent) {

		fPageBook = new PageBook(parent, SWT.NONE);

		fPageNoChart = new Label(fPageBook, SWT.NONE);
		fPageNoChart.setText(Messages.tourCatalog_view_label_year_not_selected);

		createYearChart();

		addSelectionListener();
		addCompareTourPropertyListener();
		addPrefListener();

		createActions();

		// set selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fPageBook.showPage(fPageNoChart);
	}

	private void createYearChart() {

		// year chart
		fYearChart = new Chart(fPageBook, SWT.NONE);

		fYearChart.addBarSelectionListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, final int valueIndex) {

				TourCatalogItemComparedTour tourCatalogComparedTour = (TourCatalogItemComparedTour) fYearMapTours[valueIndex];

				// select the tour in the tour viewer & show tour in compared tour char
				fCurrentSelection = new StructuredSelection(tourCatalogComparedTour);
				fPostSelectionProvider.setSelection(fCurrentSelection);
			}
		});
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);
		TourManager.getInstance().removePropertyListener(fCompareTourPropertyListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	private void onSelectionChanged(ISelection selection) {

		if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView selectionComparedTour = (SelectionTourCatalogView) selection;

			// show year statistic
			final TourCatalogItemYear yearItem = selectionComparedTour.getYearItem();
			if (yearItem != null) {

				fCurrentYearItem = yearItem;
				updateYearBarChart();
			}

			// select tour in the year chart
			final Long compTourId = selectionComparedTour.getCompTourId();
			if (compTourId != null) {
				selectTourInYearChart(compTourId);
			}

			// hide chart when a different ref tour is selected
			if (fCurrentYearItem != null && selectionComparedTour.getRefId() != fCurrentYearItem.refId) {
				fPageBook.showPage(fPageNoChart);
				fCurrentYearItem = null;
			}

		} else if (selection instanceof StructuredSelection) {

			StructuredSelection structuredSelection = (StructuredSelection) selection;

			if (structuredSelection.size() > 0) {
				Object firstElement = structuredSelection.getFirstElement();
				if (firstElement instanceof TourCatalogItemComparedTour) {

					TourCatalogItemComparedTour compareItem = (TourCatalogItemComparedTour) firstElement;
					TourCatalogItemYear yearItem = (TourCatalogItemYear) compareItem.getParentItem();

					// show year statistic
					if (yearItem != fCurrentYearItem) {
						fCurrentYearItem = yearItem;
						updateYearBarChart();
					}

					// select tour in the year chart
					final Long compTourId = compareItem.getTourId();
					if (compTourId != null) {
						selectTourInYearChart(compTourId);
					}

					// hide chart when a different ref tour is selected
					if (fCurrentYearItem != null && compareItem.getRefId() != fCurrentYearItem.refId) {
						fPageBook.showPage(fPageNoChart);
						fCurrentYearItem = null;
					}
				}
			}

		} else if (selection instanceof SelectionRemovedComparedTours) {

			final SelectionRemovedComparedTours removedCompTours = (SelectionRemovedComparedTours) selection;

			if (removedCompTours.removedComparedTours.size() > 0) {
				updateYearBarChart();
			}
		}
	}

	/**
	 * select the tour in the year map chart
	 * 
	 * @param selectedTourId
	 *        tour id which should be selected
	 */
	private void selectTourInYearChart(long selectedTourId) {

		if (fYearMapTours == null) {
			return;
		}

		final int tourLength = fYearMapTours.length;
		boolean[] selectedTours = new boolean[tourLength];

		for (int tourIndex = 0; tourIndex < tourLength; tourIndex++) {
			final TourCatalogItemComparedTour comparedItem = (TourCatalogItemComparedTour) fYearMapTours[tourIndex];
			if (comparedItem.getTourId() == selectedTourId) {
				selectedTours[tourIndex] = true;
			}
		}

		fYearChart.setSelectedBars(selectedTours);
	}

	@Override
	public void setFocus() {
		fYearChart.setFocus();
	}

	private void updateYearBarChart() {

		if (fCurrentYearItem == null) {
			return;
		}

		fPageBook.showPage(fYearChart);

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		fYearMapTours = fCurrentYearItem.getFetchedChildren();
		final int tourLength = fYearMapTours.length;

		final int[] tourDateValues = new int[tourLength];
		final int[] tourSpeed = new int[tourLength];
		final Calendar calendar = GregorianCalendar.getInstance();

		for (int tourIndex = 0; tourIndex < tourLength; tourIndex++) {
			final TourCatalogItemComparedTour comparedItem = (TourCatalogItemComparedTour) fYearMapTours[tourIndex];

			final Date tourDate = comparedItem.getTourDate();
			calendar.setTime(tourDate);

			tourDateValues[tourIndex] = calendar.get(Calendar.DAY_OF_YEAR) - 1;
			tourSpeed[tourIndex] = (int) (comparedItem.getTourSpeed() * 10 / UI.UNIT_VALUE_DISTANCE);
		}

		final ChartDataModel chartModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		final ChartDataXSerie xData = new ChartDataXSerie(tourDateValues);
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_DAY);
		chartModel.setXData(xData);

		// set the bar low/high data
		final ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR, tourSpeed);
		yData.setValueDivisor(10);
		TourManager.setGraphColor(prefStore, yData, GraphColorProvider.PREF_GRAPH_SPEED);

		/*
		 * set/restore min/max values
		 */
		final TourCatalogItemReferenceTour refItem = fCurrentYearItem.getRefItem();
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

		// set title
		chartModel.setTitle(Integer.toString(fCurrentYearItem.year));

		// set graph minimum width, this is the number of days in the fSelectedYear
		calendar.set(fCurrentYearItem.year, 11, 31);
		final int yearDays = calendar.get(Calendar.DAY_OF_YEAR);
		chartModel.setChartMinWidth(yearDays);

		// show the data in the chart
		fYearChart.updateChart(chartModel);
	}

}
