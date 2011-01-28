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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartComponentAxis;
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
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourInfoToolTipProvider;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.util.IToolTipHideListener;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.TourToolTip;
import net.tourbook.util.Util;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import de.byteholder.geoclipse.ui.ViewerDetailForm;

/**
 * Shows the selected tours in a conconi test chart
 */
public class ConconiChartViewOLD extends ViewPart {

	public static final String		ID									= "net.tourbook.views.TourChartView";	//$NON-NLS-1$

	private static final String		STATE_CONCONIT_TOURS_VIEWER_WIDTH	= "STATE_CONCONIT_TOURS_VIEWER_WIDTH";	//$NON-NLS-1$

	private final IPreferenceStore	_prefStore							= TourbookPlugin.getDefault() //
																				.getPreferenceStore();
	private final IDialogSettings	_state								= TourbookPlugin.getDefault().//
																				getDialogSettingsSection(ID);

	private TourData				_tourData;

	private PostSelectionProvider	_postSelectionProvider;
	private ISelectionListener		_postSelectionListener;

	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener		_tourEventListener;
	private IPartListener2			_partListener;

	private TourInfoToolTipProvider	_conconiTourInfoToolTipProvider;
	private ChartDataYSerie			_yDataPulse;

	private ConconiData				_conconiData;
	/*
	 * UI controls
	 */
	private PageBook				_pageBook;

//	private boolean							_isTourDirty					= false;
//	private boolean							_isDirtyDisabled				= true;
//	private int								_savedDpTolerance;
//	private int								_dpTolerance;

	private Label					_pageNoChart;

	private Composite				_pageConconiTest;
	private Chart					_chartConconiTest;
	private Scale					_scaleDeflection;

	private Label					_lblDeflactionPulse;
	private Label					_lblDeflactionPower;
	private ChartLayerConconiTest	_conconiLayer;

	private ViewerDetailForm		_detailFormConconi;

	private TableViewer				_viewerConconiTours;

	public class ConconiTourViewerContentProvicer implements IContentProvider {

		@Override
		public void dispose() {
			// TODO Auto-generated method stub

		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
			// TODO Auto-generated method stub

		}

	}

	/**
	 * Tour chart type is selected
	 * 
	 * @param tourChartType
	 */
	void actionTourChartType(final TourChartType tourChartType) {

		_requestedTourChartType = tourChartType;

		updateChart10(_tourData);
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

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {

			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == ConconiChartView.this) {
					return;
				}

				if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

					if (_tourData == null || part == ConconiChartView.this) {
						return;
					}

					// get modified tours
					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						final long chartTourId = _tourData.getTourId();

						// update chart with the modified tour
						for (final TourData tourData : modifiedTours) {

							if (tourData == null) {

								/*
								 * tour is not set, this can be the case when a manual tour is
								 * discarded
								 */

								clearView();

								return;
							}

							if (tourData.getTourId() == chartTourId) {

								updateChart10(tourData);

								// removed old tour data from the selection provider
								_postSelectionProvider.clearSelection();

								return;
							}
						}
					}

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();

				} else if (eventId == TourEventId.UPDATE_UI) {

					// check if tour data editor contains a tour which must be updated

					if (_tourData == null) {
						return;
					}

					final Long tourId = _tourData.getTourId();

					// update editor
					if (UI.containsTourId(eventData, tourId) != null) {

						// reload tour data and update chart
						updateChart10(TourManager.getInstance().getTourData(tourId));
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void clearView() {

		_tourData = null;

		if (_pageTourChart != null) {
			_pageTourChart.updateChart(null, false);
		}

		_pageBook.showPage(_pageNoChart);

		// removed old tour data from the selection provider
		_postSelectionProvider.clearSelection();
	}

	private ChartDataModel createChartDataModelConconiTest() {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_XY_SCATTER);

		final int[] powerSerie = _tourData.getPowerSerie();
		final int[] pulseSerie = _tourData.pulseSerie;

		/*
		 * check if required value series are available
		 */
		if (powerSerie == null || powerSerie.length == 0 || pulseSerie == null || pulseSerie.length == 0) {

			chartDataModel.setErrorMessage(NLS.bind(
					Messages.Tour_Chart_InvalidData,
					TourManager.getTourTitle(_tourData)));

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
		chartDataModel.setTitle(TourManager.getTourTitleDetailed(_tourData));

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
		addTourEventListener();
		addPartListener();

		enableActions();

		// set this view part as selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

		restoreState();

		// show tour chart from selection service
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		// check if tour chart is displayed
		if (_tourData == null) {
			showTourFromTourProvider();
		}
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);

		_pageNoChart = new Label(_pageBook, SWT.NONE);
		_pageNoChart.setText(Messages.UI_Label_no_chart_is_selected);

		// charts are created lazily
	}

	private void createUI20ConconiTest() {

		_pageConconiTest = new Composite(_pageBook, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_pageConconiTest);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_pageConconiTest);
		{

			final Composite container = new Composite(_pageConconiTest, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
			container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			{
				// left part: conconi tours
				_containerConconiTours = new Composite(container, SWT.NONE);
				GridLayoutFactory.fillDefaults().extendedMargins(0, 5, 0, 0).applyTo(_containerConconiTours);
				createUI21ConconiTours(_containerConconiTours);

				// sash
				final Sash sash = new Sash(container, SWT.VERTICAL);
				net.tourbook.util.UI.addSashColorHandler(sash);

				// right part: conconi chart
				final Composite chartContainer = new Composite(container, SWT.NONE);
				GridLayoutFactory.fillDefaults().extendedMargins(5, 0, 0, 0).spacing(0, 0).applyTo(chartContainer);
				createUI22ConconiChart(chartContainer);

				_detailFormConconi = new ViewerDetailForm(container, _containerConconiTours, sash, chartContainer, 30);
			}

			createUI40DeflactionPoint(_pageConconiTest);
		}
	}

	private void createUI21ConconiTours(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);

		final TableColumnLayout tableLayout = new TableColumnLayout();
		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);

		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(SWT.DEFAULT, pc.convertHeightInCharsToPixels(13))
				.applyTo(layoutContainer);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION | SWT.BORDER);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		_viewerConconiTours = new TableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;
		TableColumn tvcColumn;

		// column: map provider
		tvc = new TableViewerColumn(_viewerConconiTours, SWT.LEAD);
		tvcColumn = tvc.getColumn();
		tvcColumn.setText(Messages.Dialog_OfflineArea_Column_MapProvider);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final PartMP partMp = (PartMP) cell.getElement();
				cell.setText(partMp.partMp.getName());
			}
		});
		tableLayout.setColumnData(tvcColumn, new ColumnWeightData(100));

		/*
		 * create table viewer
		 */

		_viewerConconiTours.setContentProvider(new ConconiTourViewerContentProvicer());

		_viewerConconiTours.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {

				}
			}
		});
	}

	/**
	 * chart: conconi test
	 */
	private void createUI22ConconiChart(final Composite parent) {

		_chartConconiTest = new Chart(parent, SWT.FLAT);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartConconiTest);

		/*
		 * set tour info icon into the left axis
		 */
		final ChartComponentAxis toolTipControl = _chartConconiTest.getToolTipControl();
		_conconiTourInfoToolTipProvider = new TourInfoToolTipProvider();
		final TourToolTip _tourToolTip = new TourToolTip(toolTipControl);

		_tourToolTip.addToolTipProvider(_conconiTourInfoToolTipProvider);
		_tourToolTip.addHideListener(new IToolTipHideListener() {
			@Override
			public void afterHideToolTip(final Event event) {

				// hide hovered image
				toolTipControl.afterHideToolTip(event);
			}
		});

		_chartConconiTest.setTourToolTipProvider(_conconiTourInfoToolTipProvider);

		_conconiLayer = new ChartLayerConconiTest();
	}

	private void createUI40DeflactionPoint(final Composite parent) {

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
					setTourDirty();
				}
			});

			createUI42DeflPointValues(container);
		}
	}

	private void createUI42DeflPointValues(final Composite parent) {

		Label label;
		final PixelConverter pc = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			// label: heartbeat value
			_lblDeflactionPulse = new Label(container, SWT.TRAIL);
			GridDataFactory
					.fillDefaults()
					.align(SWT.FILL, SWT.CENTER)
					.hint(pc.convertWidthInCharsToPixels(4), SWT.DEFAULT)
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
					.hint(pc.convertWidthInCharsToPixels(4), SWT.DEFAULT)
					.applyTo(_lblDeflactionPower);

			// label: power unit
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(UI.UNIT_LABEL_POWER);
		}
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void onSelectDeflection() {

		// update conconi data
		_conconiData.selectedDeflection = _scaleDeflection.getSelection();
		_yDataPulse.setCustomData(TourManager.CUSTOM_DATA_CONCONI_TEST, _conconiData);

		updateUI20Conconi();

		// update tolerance into the tour data
		_tourData.setConconiDeflection(_scaleDeflection.getSelection());
	}

	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof SelectionTourData) {

			final TourData selectionTourData = ((SelectionTourData) selection).getTourData();
			if (selectionTourData != null) {

				// prevent loading the same tour
				if (_tourData != null && _tourData.equals(selectionTourData)) {
					return;
				}

//				savePreviousTour(selectionTourData);

				updateChart10(selectionTourData);
			}

		} else if (selection instanceof SelectionTourIds) {

			final SelectionTourIds selectionTourId = (SelectionTourIds) selection;
			final ArrayList<Long> tourIds = selectionTourId.getTourIds();
			if (tourIds != null && tourIds.size() > 0) {

				final Long tourId = tourIds.get(0);

//				savePreviousTour(tourId);

				updateChart(tourIds);
			}

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId selectionTourId = (SelectionTourId) selection;
			final Long tourId = selectionTourId.getTourId();

			updateChart(tourId);

		} else if (selection instanceof SelectionDeletedTours) {

			clearView();
		}
	}

	private void restoreState() {

	}

	private void restoreStateConconiUI() {

		// restore width for the marker list when the width is available
		try {
			_detailFormConconi.setViewerWidth(_state.getInt(STATE_CONCONIT_TOURS_VIEWER_WIDTH));
		} catch (final NumberFormatException e) {
			// ignore
		}
	}

//	private void savePreviousTour(final long tourId) {
//
//	}

//	private void savePreviousTour(final TourData newTourData) {
//
////		savedTour = TourDatabase.saveTour(tourData);
//	}

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

		if (_pageConconiTest != null&&_pageConconiTest.isVisible()) {

			_chartConconiTest.setFocus();
		}
	}


	/**
	 * when dp tolerance was changed set the tour dirty
	 */
	private void setTourDirty() {

//		if (_isDirtyDisabled) {
//			return;
//		}
//
//		if (_tourData != null && _savedDpTolerance != _tourData.getConconiDeflection()) {
//			_isTourDirty = true;
//		}
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
				if (_tourData != null) {
					return;
				}

				final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();
				if (selectedTours != null && selectedTours.size() > 0) {
					updateChart10(selectedTours.get(0));
				}
			}
		});
	}

	private void updateChart(final ArrayList<Long> tourIds) {

		// check if tour is already displayed
		if (_tourData != null && _tourData.getTourId() == tourIds) {
			return;
		}

		updateChart10(TourManager.getInstance().getTourData(tourIds));

		fireSliderPosition();
	}

	private void updateChart10(final TourData tourData) {

		if (tourData == null) {
			// nothing to do
			return;
		}

//		_isDirtyDisabled = true;
		{

			_tourData = tourData;

			updateChart20();

//			// keep original dp tolerance
//			_savedDpTolerance = _dpTolerance = _tourData.getDpTolerance();

		}
//		_isDirtyDisabled = false;

		// set application window title
		setTitleToolTip(TourManager.getTourDateShort(_tourData));
	}

	private void updateChart20() {

		if (_requestedTourChartType != null) {
			// a new tour chart type is selected
			setToolbar();
		}

		if (_displayedTourChartType == TourChartType.TOUR_CHART) {

			/*
			 * display default tour chart
			 */

			if (_pageTourChart == null) {
				createUI10TourChart();
			}

			TourManager.getInstance().setActiveTourChart(_pageTourChart);

			_pageTourChart.updateTourChart(_tourData, _tourChartConfig, false);

			_pageBook.showPage(_pageTourChart);

		} else if (_displayedTourChartType == TourChartType.CONCONI_TEST_POWER) {

			/*
			 * display conconi test
			 */
			if (_pageConconiTest == null) {
				createUI20ConconiTest();
			}

			TourManager.getInstance().setActiveTourChart(null);

			final ChartDataModel conconiChartDataModel = createChartDataModelConconiTest();

			_conconiTourInfoToolTipProvider.setTourData(_tourData);

			_chartConconiTest.updateChart(conconiChartDataModel, true, true);

			/*
			 * force the chart to be repainted because updating the conconi layer requires that the
			 * chart is already painted (it requires drawing data)
			 */
//			Display.getDefault().asyncExec(new Runnable() {
//				public void run() {
			_chartConconiTest.resizeChart();
			updateUI20Conconi();
//				}
//			});

			_pageBook.showPage(_pageConconiTest);

		} else {

			TourManager.getInstance().setActiveTourChart(null);

			_pageBook.showPage(_pageNoChart);
		}

		enableActions();
	}

	private void updateUI10Conconi(final int maxDeflection) {

		/*
		 * update deflection scale
		 */
		_scaleDeflection.setMaximum(maxDeflection > 0 ? maxDeflection - 1 : 0);

		// ensure that too much scale ticks are displayed
		final int pageIncrement = maxDeflection < 20 ? 1 : maxDeflection < 100 ? 5 : maxDeflection < 1000 ? 50 : 100;

		_scaleDeflection.setPageIncrement(pageIncrement);
	}

	private void updateUI20Conconi() {

		final int scaleIndex = _scaleDeflection.getSelection();
		final int pulseValue = (int) _conconiData.maxYValues.get(scaleIndex);
		final int powerValue = (int) _conconiData.maxXValues.get(scaleIndex);

		_lblDeflactionPulse.setText(_tourData.pulseSerie == null ? //
				Messages.App_Label_NA
				: Integer.toString(pulseValue));

		_lblDeflactionPower.setText(_tourData.pulseSerie == null ? //
				Messages.App_Label_NA
				: Integer.toString(powerValue));

		// update conconi layer
		_chartConconiTest.updateCustomLayers();
	}

}
