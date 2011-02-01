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
package net.tourbook.ui.tourChart;

import gnu.trove.list.array.TDoubleArrayList;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.IChartLayer;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourData;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.util.PixelConverter;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.DateTime;

import de.byteholder.geoclipse.ui.ViewerDetailForm;

/**
 * Show selected tours in a conconi test chart
 */
public class ConconiChartView extends ViewPart {

	public static final String		ID									= "net.tourbook.views.ConconiChartView";		//$NON-NLS-1$

	private static final String		STATE_CONCONIT_TOURS_VIEWER_WIDTH	= "STATE_CONCONIT_TOURS_VIEWER_WIDTH";			//$NON-NLS-1$

	private final IPreferenceStore	_prefStore							= TourbookPlugin.getDefault() //
																				.getPreferenceStore();

	private final IDialogSettings	_state								= TourbookPlugin.getDefault().//
																				getDialogSettingsSection(ID);

	private final Calendar			_calendar							= GregorianCalendar.getInstance();
	private final NumberFormat		_nf									= NumberFormat.getNumberInstance();
	private final DateFormat		_dateFormatter						= DateFormat.getDateInstance(DateFormat.SHORT);
	private final DateFormat		_timeFormatter						= DateFormat.getTimeInstance(DateFormat.SHORT);

	private ISelectionListener		_postSelectionListener;
	private IPartListener2			_partListener;
	private ITourEventListener		_tourEventListener;
	private IPropertyChangeListener	_prefChangeListener;

	private ArrayList<TourData>		_tourDataList;
	private ChartDataYSerie			_yDataPulse;
	private ConconiData				_conconiData;

//	private TourInfoToolTipProvider	_conconiTourInfoToolTipProvider;

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private PageBook				_pageBook;
	private Label					_pageNoChart;

	private Composite				_pageConconiTest;

	private Composite				_containerConconiTours;
	private ViewerDetailForm		_detailFormConconi;

	public ArrayList<TourData>		_conconiTours;

	private Chart					_chartConconiTest;
	private ChartLayerConconiTest	_conconiLayer;

	private Combo					_comboTours;
	private Scale					_scaleDeflection;
	private Label					_lblDeflactionPulse;
	private Label					_lblDeflactionPower;

	private class TourComparator extends ViewerComparator {

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {
			return ((TourData) e1).compareTo(e2);
		}
	}

	private void addPartListener() {
		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == ConconiChartView.this) {
//					saveTour();
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

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == ConconiChartView.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void clearView() {

		_tourDataList = null;

		if (_chartConconiTest != null) {
			_chartConconiTest.updateChart(null, false);
		}

		_pageBook.showPage(_pageNoChart);
	}

	private ChartDataModel createChartDataModelConconiTest(final ArrayList<TourData> tourDataList) {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_XY_SCATTER);

		final int serieLength = tourDataList.size();
		final TourData[] tourDataArray = tourDataList.toArray(new TourData[serieLength]);
		final int[][] powerSerie = new int[serieLength][];
		final int[][] pulseSerie = new int[serieLength][];

		boolean isDataAvailable = false;

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			final TourData tourData = tourDataArray[serieIndex];

			final int[] tdPowerSerie = tourData.getPowerSerie();
			final int[] tdPulseSerie = tourData.pulseSerie;

			if (tdPowerSerie != null && tdPowerSerie.length != 0 && tdPulseSerie != null && tdPulseSerie.length != 0) {

				isDataAvailable = true;

				powerSerie[serieIndex] = tdPowerSerie;
				pulseSerie[serieIndex] = tdPulseSerie;
			}
		}

		/*
		 * check if required data series are available
		 */
		if (isDataAvailable == false) {

			final StringBuilder sb = new StringBuilder();
//			sb.append();
//			Object[] tourTitle = TourManager.getTourTitle(_tourData);

			chartDataModel.setErrorMessage(NLS.bind(Messages.Tour_Chart_InvalidData, sb.toString()));

			return chartDataModel;
		}

		/*
		 * power
		 */
		final ChartDataXSerie xDataPower = new ChartDataXSerie(powerSerie);
		xDataPower.setLabel(Messages.Graph_Label_Power);
		xDataPower.setUnitLabel(Messages.Graph_Label_Power_unit);
		xDataPower.setDefaultRGB(new RGB(0, 0, 0));

		/*
		 * heartbeat
		 */
		_yDataPulse = new ChartDataYSerie(ChartDataModel.CHART_TYPE_XY_SCATTER, pulseSerie);
		_yDataPulse.setYTitle(Messages.Graph_Label_Heartbeat);
		_yDataPulse.setUnitLabel(Messages.Graph_Label_Heartbeat_unit);
		_yDataPulse.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		TourManager.setGraphColor(_prefStore, _yDataPulse, GraphColorProvider.PREF_GRAPH_HEARTBEAT);

		//adjust min/max values
		xDataPower.setVisibleMinValue(0, true);
		xDataPower.setVisibleMaxValue(xDataPower.getVisibleMaxValue() + 20, true);
		_yDataPulse.setVisibleMinValue(_yDataPulse.getVisibleMinValue() - 10, true);
		_yDataPulse.setVisibleMaxValue(_yDataPulse.getVisibleMaxValue() + 10, true);

		// setup chart data model
		chartDataModel.setXData(xDataPower);
		chartDataModel.addYData(_yDataPulse);
//		chartDataModel.setTitle(TourManager.getTourTitleDetailed(_tourData));

		_conconiData = createConconiData(xDataPower, _yDataPulse);

		updateUI10Conconi(_conconiData.maxXValues.size());

		/*
		 * updata layer for regression lines
		 */
		final ArrayList<IChartLayer> chartCustomLayers = new ArrayList<IChartLayer>();
		chartCustomLayers.add(_conconiLayer);

		_yDataPulse.setCustomLayers(chartCustomLayers);
		_yDataPulse.setCustomData(TourManager.CUSTOM_DATA_CONCONI_TEST, _conconiData);

		return chartDataModel;
	}

	private ConconiData createConconiData(final ChartDataSerie xData, final ChartDataSerie yData) {

		final int xValues[] = xData.getHighValues()[0];
		final int yHighValues[] = yData.getHighValues()[0];

		final TDoubleArrayList maxXValues = new TDoubleArrayList();
		final TDoubleArrayList maxYValues = new TDoubleArrayList();
		int lastMaxY = Integer.MIN_VALUE;
		int currentXValue = xValues[0];

		// loop: all values in the current serie
		for (int valueIndex = 0; valueIndex < xValues.length; valueIndex++) {

			// check array bounds
			if (valueIndex >= yHighValues.length) {
				break;
			}

			final int xValue = xValues[valueIndex];
			final int yValue = yHighValues[valueIndex];

			// ignore 0 values
//			if (xValue == 0) {
//				continue;
//			}

			if (xValue == currentXValue) {

				// get maximum y value for the same x value

				if (yValue > lastMaxY) {
					lastMaxY = yValue;
				}

			} else {

				// next x value is displayed, keep last max y

				maxXValues.add(currentXValue);
				maxYValues.add(lastMaxY);

				currentXValue = xValue;
				lastMaxY = yValue;
			}
		}

		// get last value
		maxXValues.add(currentXValue);
		maxYValues.add(lastMaxY);

		final ConconiData conconiData = new ConconiData();
		conconiData.maxXValues = maxXValues;
		conconiData.maxYValues = maxYValues;
		conconiData.selectedDeflection = _scaleDeflection.getSelection();

		return conconiData;
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		addSelectionListener();
		addPartListener();

		// show conconi chart from selection service
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		// check if tour chart is displayed
		if (_tourDataList == null) {
			showTourFromTourProvider();
		}
	}

	private void createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_pageBook = new PageBook(parent, SWT.NONE);

		_pageNoChart = new Label(_pageBook, SWT.NONE);
		_pageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		createUI10ConconiTest(_pageBook);
	}

	private void createUI10ConconiTest(final Composite parent) {

		_pageConconiTest = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_pageConconiTest);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_pageConconiTest);
		{
			createUI20ConconiChart(_pageConconiTest);
			createUI3OptionPanel(_pageConconiTest);
		}
	}

	/**
	 * chart: conconi test
	 */
	private void createUI20ConconiChart(final Composite parent) {

		_chartConconiTest = new Chart(parent, SWT.FLAT);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartConconiTest);

//		/*
//		 * set tour info icon into the left axis
//		 */
//		final ChartComponentAxis toolTipControl = _chartConconiTest.getToolTipControl();
//		_conconiTourInfoToolTipProvider = new TourInfoToolTipProvider();
//		final TourToolTip _tourToolTip = new TourToolTip(toolTipControl);
//
//		_tourToolTip.addToolTipProvider(_conconiTourInfoToolTipProvider);
//		_tourToolTip.addHideListener(new IToolTipHideListener() {
//			@Override
//			public void afterHideToolTip(final Event event) {
//
//				// hide hovered image
//				toolTipControl.afterHideToolTip(event);
//			}
//		});
//
//		_chartConconiTest.setTourToolTipProvider(_conconiTourInfoToolTipProvider);

		_conconiLayer = new ChartLayerConconiTest();
	}

	private void createUI3OptionPanel(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			createUI40TourCombo(container);
			createUI50DeflactionPoint(container);
		}
	}

	private void createUI40TourCombo(final Composite container) {

		_comboTours = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);

		_comboTours.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectTour();
			}
		});
	}

	private void createUI50DeflactionPoint(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().extendedMargins(5, 5, 0, 5).numColumns(3).applyTo(container);
		{
			// label: deflaction point
			final Label label = new Label(container, SWT.NONE);
			label.setText(Messages.Tour_Chart_DeflactionPoint);

			// scale: deflection point
			_scaleDeflection = new Scale(container, SWT.HORIZONTAL);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleDeflection);
			_scaleDeflection.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectDeflection();
//					setTourDirty();
				}
			});

			createUI52DeflPointValues(container);
		}
	}

	private void createUI52DeflPointValues(final Composite parent) {

		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			// label: heartbeat value
			_lblDeflactionPulse = new Label(container, SWT.TRAIL);
			GridDataFactory
					.fillDefaults()
					.align(SWT.FILL, SWT.CENTER)
					.hint(_pc.convertWidthInCharsToPixels(4), SWT.DEFAULT)
					.applyTo(_lblDeflactionPulse);

			// label: heartbeat unit
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(Messages.Graph_Label_Heartbeat_unit);

			// label: power value
			_lblDeflactionPower = new Label(container, SWT.TRAIL);
			GridDataFactory
					.fillDefaults()
					.align(SWT.FILL, SWT.CENTER)
					.hint(_pc.convertWidthInCharsToPixels(4), SWT.DEFAULT)
					.applyTo(_lblDeflactionPower);

			// label: power unit
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(UI.UNIT_LABEL_POWER);
		}
	}

	@Override
	public void dispose() {

		getViewSite().getPage().removePartListener(_partListener);
		getSite().getPage().removeSelectionListener(_postSelectionListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void onSelectDeflection() {

//		// update conconi data
//		_conconiData.selectedDeflection = _scaleDeflection.getSelection();
//		_yDataPulse.setCustomData(TourManager.CUSTOM_DATA_CONCONI_TEST, _conconiData);
//
//		updateUI20Conconi();
//
//		// update tolerance into the tour data
//		_tourData.setConconiDeflection(_scaleDeflection.getSelection());
	}

	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof SelectionTourData) {

			final TourData tourData = ((SelectionTourData) selection).getTourData();
			if (tourData != null) {

//				savePreviousTour(selectionTourData);

				updateChart20(tourData);
			}

		} else if (selection instanceof SelectionTourIds) {

			final SelectionTourIds selectionTourId = (SelectionTourIds) selection;
			final ArrayList<Long> tourIds = selectionTourId.getTourIds();
			if (tourIds != null && tourIds.size() > 0) {

//				savePreviousTour(tourId);

				updateChart12(tourIds);
			}

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId selectionTourId = (SelectionTourId) selection;
			final Long tourId = selectionTourId.getTourId();

			updateChart10(tourId);

		} else if (selection instanceof SelectionDeletedTours) {

			clearView();
		}
	}

	private void onSelectTour() {
		// TODO Auto-generated method stub

	}

	private void restoreStateConconiUI() {

		// restore width for the marker list when the width is available
		try {
			_detailFormConconi.setViewerWidth(_state.getInt(STATE_CONCONIT_TOURS_VIEWER_WIDTH));
		} catch (final NumberFormatException e) {
			// ignore
		}
	}

	private void saveState() {

		// check if UI is disposed
		if (_pageBook.isDisposed()) {
			return;
		}

		if (_containerConconiTours != null) {

			final int viewerWidth = _containerConconiTours.getSize().x;
			if (viewerWidth > 0) {
				_state.put(STATE_CONCONIT_TOURS_VIEWER_WIDTH, viewerWidth);
			}
		}
	}

	@Override
	public void setFocus() {

		if (_pageConconiTest != null && _pageConconiTest.isVisible()) {
			_chartConconiTest.setFocus();
		}
	}

	private void showTourFromTourProvider() {

		_pageBook.showPage(_pageNoChart);

		// a tour is not displayed, find a tour provider which provides a tour
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				// validate widget
				if (_pageBook.isDisposed()) {
					return;
				}

				/*
				 * check if tour was set from a selection provider
				 */
				if (_tourDataList != null) {
					return;
				}

				final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();
				if (selectedTours != null && selectedTours.size() > 0) {
//					updateChart10(selectedTours.get(0));
				}
			}
		});
	}

	private void updateChart10(final Long tourId) {

		final ArrayList<Long> tourIds = new ArrayList<Long>();
		tourIds.add(tourId);

		updateChart12(tourIds);
	}

	private void updateChart12(final ArrayList<Long> tourIds) {
		updateChart22(TourManager.getInstance().getTourData(tourIds));
	}

	private void updateChart20(final TourData tourData) {

		if (tourData == null) {
			return;
		}

		final ArrayList<TourData> tourDataList = new ArrayList<TourData>();
		tourDataList.add(tourData);

		updateChart22(tourDataList);
	}

	private void updateChart22(final ArrayList<TourData> tourDataList) {

		if (tourDataList == null || tourDataList.size() == 0) {
			// nothing to do
			clearView();
			return;
		}

//		_isDirtyDisabled = true;
		{

			_tourDataList = tourDataList;

			Collections.sort(tourDataList);

			final ChartDataModel conconiChartDataModel = createChartDataModelConconiTest(tourDataList);

//			_conconiTourInfoToolTipProvider.setTourData(_tourData);

			_chartConconiTest.updateChart(conconiChartDataModel, true, true);

			/*
			 * force the chart to be repainted because updating the conconi layer requires that the
			 * chart is already painted (it requires drawing data)
			 */
			_chartConconiTest.resizeChart();
			updateUI20Conconi();

			_pageBook.showPage(_pageConconiTest);

//			// keep original dp tolerance
//			_savedDpTolerance = _dpTolerance = _tourData.getDpTolerance();

		}
//		_isDirtyDisabled = false;

		// set application window title
//		setTitleToolTip(TourManager.getTourDateShort(_tourData));

		return;
	}

	private void updateUI10Conconi(final int maxDeflection) {

		/*
		 * update deflection scale
		 */
		_scaleDeflection.setMaximum(maxDeflection > 0 ? maxDeflection - 1 : 0);

		// ensure that too much scale ticks are displayed
		final int pageIncrement = maxDeflection < 20 ? 1 : maxDeflection < 100 ? 5 : maxDeflection < 1000 ? 50 : 100;

		_scaleDeflection.setPageIncrement(pageIncrement);

		/*
		 * tour combo box
		 */
		_comboTours.removeAll();
		
		
		for (TourData tourData : _tourDataList) {
			DateTime dt = tourData.getStartDateTime();
			
		}
	}

	private void updateUI20Conconi() {

		// deflation values
		final int scaleIndex = _scaleDeflection.getSelection();
		final int pulseValue = (int) _conconiData.maxYValues.get(scaleIndex);
		final int powerValue = (int) _conconiData.maxXValues.get(scaleIndex);

		_lblDeflactionPulse.setText(Integer.toString(pulseValue));
		_lblDeflactionPower.setText(Integer.toString(powerValue));

		// update conconi layer
		_chartConconiTest.updateCustomLayers();
	}

}
