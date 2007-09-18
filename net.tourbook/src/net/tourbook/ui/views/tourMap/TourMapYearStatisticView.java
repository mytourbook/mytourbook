package net.tourbook.ui.views.tourMap;

import java.sql.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.colors.GraphColors;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourManager;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class TourMapYearStatisticView extends ViewPart {

	public static final String		ID	= "net.tourbook.views.tourMap.yearStatisticView";	//$NON-NLS-1$

	private Chart					fYearChart;

	private ISelectionListener		fPostSelectionListener;
	private PostSelectionProvider	fPostSelectionProvider;

	/**
	 * contains all {@link TVTITourMapComparedTour} tour objects for the current year
	 */
	private Object[]				fYearMapTours;

	private TVITourMapYear			fYearItem;

	private PageBook				fPageBook;
	private Label					fPageNoChart;

	public TourMapYearStatisticView() {}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	@Override
	public void createPartControl(Composite parent) {

		fPageBook = new PageBook(parent, SWT.NONE);

		fPageNoChart = new Label(fPageBook, SWT.NONE);
		fPageNoChart.setText("A year is not selected");

		// year chart
		fYearChart = new Chart(fPageBook, SWT.NONE);

		fYearChart.addBarSelectionListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, final int valueIndex) {

				TVTITourMapComparedTour tourMapComparedTour = (TVTITourMapComparedTour) fYearMapTours[valueIndex];

				// select the tour in the tour viewer & show tour in compared tour char
				fPostSelectionProvider.setSelection(new StructuredSelection(tourMapComparedTour));
			}
		});

		addSelectionListener();

		// set selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fPageBook.showPage(fPageNoChart);
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);

		super.dispose();
	}

	private void onSelectionChanged(ISelection selection) {

		if (selection instanceof SelectionComparedTour) {

			final SelectionComparedTour selectionComparedTour = (SelectionComparedTour) selection;
			final TVITourMapYear yearItem = selectionComparedTour.getYearItem();
			if (yearItem != null) {

				fYearItem = yearItem;
				showYearBarChart();
			}

			final Long compTourId = selectionComparedTour.getCompTourId();
			if (compTourId != null) {
				selectTourInYearChart(compTourId);
			}

		} else if (selection instanceof SelectionRemovedComparedTours) {

			final SelectionRemovedComparedTours removedCompTours = (SelectionRemovedComparedTours) selection;

			if (removedCompTours.removedComparedTours.size() > 0) {
				showYearBarChart();
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

		final int tourLength = fYearMapTours.length;
		boolean[] selectedTours = new boolean[tourLength];

		for (int tourIndex = 0; tourIndex < tourLength; tourIndex++) {
			final TVTITourMapComparedTour comparedItem = (TVTITourMapComparedTour) fYearMapTours[tourIndex];
			if (comparedItem.getTourId() == selectedTourId) {
				selectedTours[tourIndex] = true;
			}
		}

		fYearChart.setSelectedBars(selectedTours);
	}

	@Override
	public void setFocus() {

	}

	private void showYearBarChart() {

		if (fYearItem == null) {
			return;
		}

		fPageBook.showPage(fYearChart);

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		fYearMapTours = fYearItem.getFetchedChildren();
		final int tourLength = fYearMapTours.length;

		final int[] tourDateValues = new int[tourLength];
		final int[] tourSpeed = new int[tourLength];
		final Calendar calendar = GregorianCalendar.getInstance();

		for (int tourIndex = 0; tourIndex < tourLength; tourIndex++) {
			final TVTITourMapComparedTour comparedItem = (TVTITourMapComparedTour) fYearMapTours[tourIndex];

			final Date tourDate = comparedItem.getTourDate();
			calendar.setTime(tourDate);

			tourDateValues[tourIndex] = calendar.get(Calendar.DAY_OF_YEAR) - 1;
			tourSpeed[tourIndex] = (int) (comparedItem.getTourSpeed() * 10);
		}

		final ChartDataModel chartModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		final ChartDataXSerie xData = new ChartDataXSerie(tourDateValues);
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_YEAR);
		chartModel.setXData(xData);

		// set the bar low/high data
		final ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR, tourSpeed);

		yData.setValueDivisor(10);
		TourManager.setGraphColor(prefStore, yData, GraphColors.PREF_GRAPH_SPEED);

		/*
		 * set/restore min/max values
		 */
		final TVTITourMapReferenceTour refItem = fYearItem.getRefItem();
		final int minValue = yData.getVisibleMinValue();
		final int maxValue = yData.getVisibleMaxValue();

		final int dataMinValue = minValue - (minValue / 10);
		final int dataMaxValue = maxValue;// + (maxValue / 30);

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

		yData.setYTitle(Messages.TourMap_Label_year_chart_title);
		yData.setUnitLabel(Messages.TourMap_Label_year_chart_unit);
		// yData.setMinValue(0);

		chartModel.addYData(yData);

		// set title
		chartModel.setTitle(NLS.bind(Messages.TourMap_Label_chart_title_year_map, fYearItem.year));

		// set graph minimum width, this is the number of days in the
		// fSelectedYear
		calendar.set(fYearItem.year, 11, 31);
		final int yearDays = calendar.get(Calendar.DAY_OF_YEAR);
		chartModel.setChartMinWidth(yearDays);

		// show the data in the chart
		fYearChart.updateChart(chartModel);
	}

}
