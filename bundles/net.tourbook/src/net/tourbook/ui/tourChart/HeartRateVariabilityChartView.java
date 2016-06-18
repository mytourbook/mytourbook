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
package net.tourbook.ui.tourChart;

import java.util.ArrayList;
import java.util.Collections;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartType;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.ActionChartOptions;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

/**
 * Show selected tours in a Heart rate variability (HRV) chart
 */
public class HeartRateVariabilityChartView extends ViewPart {

	public static final String		ID										= "net.tourbook.ui.tourChart.HeartRateVariabilityChartView";			//$NON-NLS-1$

	private static final String		GRAPH_LABEL_HEART_RATE_VARIABILITY		= net.tourbook.common.Messages.Graph_Label_HeartRateVariability;
	private static final String		GRAPH_LABEL_HEART_RATE_VARIABILITY_UNIT	= net.tourbook.common.Messages.Graph_Label_HeartRateVariability_Unit;

	private static final int		ADJUST_PULSE_VALUE						= 10;

	private final IPreferenceStore	_prefStore								= TourbookPlugin.getPrefStore();
	private final IPreferenceStore	_commonPrefStore						= CommonActivator.getPrefStore();

	private IPropertyChangeListener	_prefChangeListener;
	private ISelectionListener		_postSelectionListener;
	private ITourEventListener		_tourEventListener;

	private ArrayList<TourData>		_hrvTours;

	private ActionChartOptions		_actionChartOptions;
//	private ActionSynchChartScale	_actionSynchChartScale;
	/*
	 * UI controls
	 */
	private PageBook				_pageBook;
	private Label					_pageNoChart;
	private Composite				_pageHrvChart;
	private Chart					_chartHRV;

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */
				if (property.equals(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES)
						|| property.equals(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR)
						|| property.equals(ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR)
				//
				) {

					UI.updateChartProperties(_chartHRV);

					// grid has changed, update chart
					updateChart_30_CurrentTours();
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

				if (part == HeartRateVariabilityChartView.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void clearView() {

		_hrvTours = null;

		if (_chartHRV != null) {
			_chartHRV.updateChart(null, false);
		}

		_pageBook.showPage(_pageNoChart);
	}

	private void createActions() {

		_actionChartOptions = new ActionChartOptions(_pageBook);
//		_actionSynchChartScale = new ActionSynchChartScale(this);
	}

	/**
	 * @param hrvTours
	 *            contains all tours which are displayed in the chart, they can be valid or invalid
	 * @return
	 */
	private ChartDataModel createChartDataModel(final ArrayList<TourData> hrvTours) {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartType.XY_SCATTER);

		final int serieLengthRaw = hrvTours.size();

		final TourData[] toursArray = hrvTours.toArray(new TourData[serieLengthRaw]);
		final ArrayList<TourData> validTourList = new ArrayList<TourData>();

		/*
		 * get all tours which has valid data
		 */
		for (int serieIndex = 0; serieIndex < serieLengthRaw; serieIndex++) {

			final TourData tourData = toursArray[serieIndex];

			final int[] tdPulseTimeSerie = tourData.pulseTimeSerie;

			// check if required data series are available
			if (tdPulseTimeSerie != null && tdPulseTimeSerie.length > 1) {
				validTourList.add(tourData);
			}
		}

		final int validDataLength = validTourList.size();

		// display error when required data are not available
		if (validDataLength == 0) {

			chartDataModel.setErrorMessage(Messages.HRV_Chart_InvalidData);

			return chartDataModel;
		}

		final String prefGraphName = ICommonPreferences.GRAPH_COLORS + GraphColorManager.PREF_GRAPH_HEARTBEAT + "."; //$NON-NLS-1$

		final RGB rgbPrefLine = PreferenceConverter.getColor(//
				_commonPrefStore,
				prefGraphName + GraphColorManager.PREF_COLOR_LINE);

		final RGB rgbPrefDark = PreferenceConverter.getColor(//
				_commonPrefStore,
				prefGraphName + GraphColorManager.PREF_COLOR_DARK);

		final RGB rgbPrefBright = PreferenceConverter.getColor(//
				_commonPrefStore,
				prefGraphName + GraphColorManager.PREF_COLOR_BRIGHT);

		final double[][] rr0Series = new double[validDataLength][];
		final float[][] rr1Series = new float[validDataLength][];

		final RGB[] rgbLine = new RGB[validDataLength];
		final RGB[] rgbDark = new RGB[validDataLength];
		final RGB[] rgbBright = new RGB[validDataLength];

		final TourData[] validTours = validTourList.toArray(new TourData[validTourList.size()]);

		/*
		 * create data series which contain valid data, reduce data that the highes value for an x
		 * value is displayed
		 */
		for (int tourIndex = 0; tourIndex < validDataLength; tourIndex++) {

			final TourData tourData = validTours[tourIndex];

			final int[] pulseTimeSerie = tourData.pulseTimeSerie;
			final int numPulseTimes = pulseTimeSerie.length - 1;

			final double[] rr0Values = new double[numPulseTimes];
			final float[] rr1Values = new float[numPulseTimes];

			// loop: all values in the current serie
			for (int valueIndex = 0; valueIndex < numPulseTimes; valueIndex++) {

				rr0Values[valueIndex] = pulseTimeSerie[valueIndex];
				rr1Values[valueIndex] = pulseTimeSerie[valueIndex + 1];
			}

			rr0Series[tourIndex] = rr0Values;
			rr1Series[tourIndex] = rr1Values;

			rgbLine[tourIndex] = rgbPrefLine;
			rgbDark[tourIndex] = rgbPrefDark;
			rgbBright[tourIndex] = rgbPrefBright;
		}

		/*
		 * X axis: RR
		 */
		final ChartDataXSerie xDataRR0 = new ChartDataXSerie(rr0Series);
		xDataRR0.setLabel(GRAPH_LABEL_HEART_RATE_VARIABILITY);
		xDataRR0.setUnitLabel(GRAPH_LABEL_HEART_RATE_VARIABILITY_UNIT);
		xDataRR0.setXAxisStartValue(xDataRR0.getVisibleMinValue() - ADJUST_PULSE_VALUE);

		chartDataModel.setXData(xDataRR0);

		/*
		 * Y axis: RR +1
		 */
		final ChartDataYSerie yDataRR1 = new ChartDataYSerie(ChartType.XY_SCATTER, rr1Series);
		yDataRR1.setYTitle(GRAPH_LABEL_HEART_RATE_VARIABILITY);
		yDataRR1.setUnitLabel(GRAPH_LABEL_HEART_RATE_VARIABILITY_UNIT);
		yDataRR1.setDefaultRGB(rgbPrefLine);
		yDataRR1.setRgbLine(rgbLine);
		yDataRR1.setRgbDark(rgbDark);
		yDataRR1.setRgbBright(rgbBright);

		yDataRR1.setVisibleMinValueForced(yDataRR1.getVisibleMinValue() - ADJUST_PULSE_VALUE);
		yDataRR1.setVisibleMaxValueForced(yDataRR1.getVisibleMaxValue() + ADJUST_PULSE_VALUE);

		chartDataModel.addYData(yDataRR1);

		return chartDataModel;
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		createActions();
		fillToolbar();

		restoreState();

		addSelectionListener();
		addPrefListener();

		// show hrv chart from selection service
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);

		_pageNoChart = new Label(_pageBook, SWT.NONE);
		_pageNoChart.setText(Messages.UI_Label_TourIsNotSelected);

		createUI_10_HrvChart(_pageBook);
	}

	private void createUI_10_HrvChart(final Composite parent) {

		_pageHrvChart = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_pageHrvChart);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(_pageHrvChart);
		{
			_chartHRV = new Chart(_pageHrvChart, SWT.FLAT);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartHRV);

			UI.updateChartProperties(_chartHRV);
		}
	}

	@Override
	public void dispose() {

		getSite().getPage().removeSelectionListener(_postSelectionListener);
		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		super.dispose();
	}

	/**
	 * Each statistic has it's own toolbar
	 */
	private void fillToolbar() {

		// update view toolbar
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.removeAll();

//		tbm.add(_actionSynchChartScale);
		tbm.add(_actionChartOptions);

		// update toolbar to show added items
		tbm.update(true);
	}

	private void onSelectionChanged(final ISelection selection) {

		if (_pageBook != null && _pageBook.isDisposed()) {
			return;
		}

		if (selection instanceof SelectionTourData) {

			final TourData tourData = ((SelectionTourData) selection).getTourData();
			if (tourData != null) {
				updateChart_20(tourData);
			}

		} else if (selection instanceof SelectionTourIds) {

			final SelectionTourIds selectionTourId = (SelectionTourIds) selection;
			final ArrayList<Long> tourIds = selectionTourId.getTourIds();
			if (tourIds != null && tourIds.size() > 0) {
				updateChart_12(tourIds);
			}

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId selectionTourId = (SelectionTourId) selection;
			final Long tourId = selectionTourId.getTourId();

			updateChart_10(tourId);

		} else if (selection instanceof SelectionDeletedTours) {

			clearView();
		}
	}

	private void restoreState() {

	}

	@Override
	public void setFocus() {

		if (_pageHrvChart != null && _pageHrvChart.isVisible()) {
			_chartHRV.setFocus();
		}
	}

	private void updateChart_10(final Long tourId) {

		final ArrayList<Long> tourIds = new ArrayList<Long>();
		tourIds.add(tourId);

		updateChart_12(tourIds);
	}

	private void updateChart_12(final ArrayList<Long> tourIds) {

		updateChart_22(TourManager.getInstance().getTourData(tourIds));
	}

	private void updateChart_20(final TourData tourData) {

		if (tourData == null) {
			return;
		}

		final ArrayList<TourData> tourDataList = new ArrayList<TourData>();
		tourDataList.add(tourData);

		updateChart_22(tourDataList);
	}

	private void updateChart_22(final ArrayList<TourData> tourDataList) {

		/*
		 * tour editor is not opened because it can cause a recursive attempt to active a part in
		 * the middle of activating a part
		 */
		if (tourDataList == null || tourDataList.size() == 0 || TourManager.isTourEditorModified(false)) {
			// nothing to do
			clearView();
			return;
		}

		// sort tours by date/time
		Collections.sort(tourDataList);

		_hrvTours = tourDataList;

		updateChart_30_CurrentTours();

		_pageBook.showPage(_pageHrvChart);

		return;
	}

	/**
	 */
	private void updateChart_30_CurrentTours() {

		if (_hrvTours == null) {
			_pageBook.showPage(_pageNoChart);
			return;
		}

		final ChartDataModel chartDataModel = createChartDataModel(_hrvTours);

		_chartHRV.updateChart(chartDataModel, true, true);
	}

}
