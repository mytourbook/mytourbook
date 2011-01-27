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
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourInfoToolTipProvider;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourChartViewer;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;
import net.tourbook.util.IToolTipHideListener;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.TourToolTip;
import net.tourbook.util.Util;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

// author: Wolfgang Schramm
// create: 09.07.2007

/**
 * Shows the selected tour in a chart
 */
public class TourChartView extends ViewPart implements ITourChartViewer {

	public static final String				ID								= "net.tourbook.views.TourChartView";	//$NON-NLS-1$

	private static final String				STATE_SELECTED_TOUR_CHART_TYPE	= "STATE_SELECTED_TOUR_CHART_TYPE";	//$NON-NLS-1$

	private final IPreferenceStore			_prefStore						= TourbookPlugin.getDefault() //
																					.getPreferenceStore();

	private final IDialogSettings			_state							= TourbookPlugin.getDefault().//
																					getDialogSettingsSection(ID);

	private TourData						_tourData;

	private TourChartConfiguration			_tourChartConfig;
	private PostSelectionProvider			_postSelectionProvider;

	private ISelectionListener				_postSelectionListener;
	private IPropertyChangeListener			_prefChangeListener;
	private ITourEventListener				_tourEventListener;
	private IPartListener2					_partListener;
	private ActionTourChartTitle			_actionTourChartTitle;

	private ActionTourChartDefault			_actionTourChartDefault;
	private ActionTourChartDefaultToolbar	_actionTourChartDefaultToolbar;
	private ActionTourChartConconiPower		_actionTourChartConconiPower;
	private TourChartType					_requestedTourChartType			= TourChartType.TOUR_CHART;

	private TourChartType					_displayedTourChartType;
	private IToolBarManager					_tbmTourChartView;
	private IContributionItem[]				_tourChartContribItems;

	private IContributionItem[]				_conconiTestContribItems;
	private TourInfoToolTipProvider			_conconiTourInfoToolTipProvider;

	private ChartDataYSerie					_yDataPulse;
	private ConconiData						_conconiData;

	private boolean							_isTourDirty					= false;
	private boolean							_isDirtyDisabled				= true;
	private int								_savedDpTolerance;
	private int								_dpTolerance;

	/*
	 * UI controls
	 */
	private PageBook						_pageBook;

	private Label							_pageNoChart;
	private TourChart						_pageTourChart;
	private Composite						_pageConconiTest;
	private Chart							_chartConconiTest;

	private Scale							_scaleDeflection;
	private Label							_lblDeflactionPulse;
	private Label							_lblDeflactionPower;
	private ChartLayerConconiTest			_conconiLayer;

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
				if (partRef.getPart(false) == TourChartView.this) {
					saveTour();
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

				/*
				 * set a new chart configuration when the preferences has changed
				 */
				if (property.equals(ITourbookPreferences.GRAPH_VISIBLE)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME)) {

					_tourChartConfig = TourManager.createTourChartConfiguration();

					if (_pageTourChart != null) {
						_pageTourChart.updateTourChart(_tourData, _tourChartConfig, false);
					}

				} else if (property.equals(ITourbookPreferences.GRAPH_MOUSE_MODE)) {

					if (_pageTourChart != null) {
						_pageTourChart.setMouseMode(event.getNewValue());
					}
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

				if (part == TourChartView.this) {
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

				if (part == TourChartView.this) {
					return;
				}

				if (eventId == TourEventId.SEGMENT_LAYER_CHANGED) {

					if (_pageTourChart != null) {
						_pageTourChart.updateSegmentLayer((Boolean) eventData);
					}

				} else if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

					if (_pageTourChart != null) {
						_pageTourChart.updateTourChart(true, true);
					}

				} else if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

					if (_tourData == null || part == TourChartView.this) {
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

					// check if this tour data editor contains a tour which must be updated

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

	private void createActions() {

		/*
		 * create actions
		 */
		_actionTourChartTitle = new ActionTourChartTitle();
		_actionTourChartDefault = new ActionTourChartDefault(this);
		_actionTourChartDefaultToolbar = new ActionTourChartDefaultToolbar(this);
		_actionTourChartConconiPower = new ActionTourChartConconiPower(this);

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(_actionTourChartTitle);
		menuMgr.add(_actionTourChartDefault);
		menuMgr.add(_actionTourChartConconiPower);
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

		_tbmTourChartView = getViewSite().getActionBars().getToolBarManager();

		createActions();
		createUI(parent);

		addSelectionListener();
		addPrefListener();
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

	private void createUI10TourChart() {

		_pageTourChart = new TourChart(_pageBook, SWT.FLAT, true);
		_pageTourChart.setShowZoomActions(true);
		_pageTourChart.setShowSlider(true);
		_pageTourChart.setTourInfoActionsEnabled(true);
		_pageTourChart.setContextProvider(new TourChartContextProvicer(this));
		_pageTourChart.setToolBarManager(_tbmTourChartView, true);

		_pageTourChart.addDoubleClickListener(new Listener() {
			public void handleEvent(final Event event) {
				if (_tourData.getTourPerson() != null) {
					ActionEditQuick.doAction(TourChartView.this);
				}
			}
		});

		// set chart title
		_pageTourChart.addDataModelListener(new IDataModelListener() {
			public void dataModelChanged(final ChartDataModel chartDataModel) {
				chartDataModel.setTitle(TourManager.getTourTitleDetailed(_tourData));
			}
		});

		// fire a slider move selection when a slider was moved in the tour chart
		_pageTourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(final SelectionChartInfo chartInfoSelection) {
				_postSelectionProvider.setSelection(chartInfoSelection);
			}
		});

		_tourChartConfig = TourManager.createTourChartConfiguration();
	}

	private void createUI20ConconiTest() {

		_pageConconiTest = new Composite(_pageBook, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_pageConconiTest);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_pageConconiTest);
		{
			createUI21ConconiChart(_pageConconiTest);
			createUI22DeflactionPoint(_pageConconiTest);
		}
	}

	/**
	 * chart: conconi test
	 */
	private void createUI21ConconiChart(final Composite parent) {

		_chartConconiTest = new Chart(parent, SWT.FLAT);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartConconiTest);

		// fill toolbar
		_tbmTourChartView.add(_actionTourChartDefaultToolbar);
		_chartConconiTest.setToolBarManager(_tbmTourChartView, false);
		_tbmTourChartView.update(true);

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

	private void createUI22DeflactionPoint(final Composite parent) {

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

			createUI24DeflPointValues(container);
		}
	}

	private void createUI24DeflPointValues(final Composite parent) {

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

	private void enableActions() {

		final boolean isTour = _tourData != null;
		final boolean isTourChart = _displayedTourChartType == TourChartType.TOUR_CHART;
		final boolean isConconiTest = _displayedTourChartType == TourChartType.CONCONI_TEST_POWER;

		// disable title
		_actionTourChartTitle.setEnabled(false);

		_actionTourChartDefault.setEnabledEx(isTour && isTourChart == false);
		_actionTourChartDefaultToolbar.setEnabled(isTour && isTourChart == false);
		_actionTourChartConconiPower.setEnabled(isTour && isConconiTest == false);

		// check selected chart type
		_actionTourChartDefault.setChecked(isTourChart);
		_actionTourChartConconiPower.setChecked(isConconiTest);
	}

	/**
	 * fire slider move event when the chart is drawn the first time or when the focus gets the
	 * chart, this will move the sliders in the map to the correct position
	 */
	private void fireSliderPosition() {

		if (_pageTourChart == null) {
			return;
		}

		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				TourManager.fireEvent(
						TourEventId.SLIDER_POSITION_CHANGED,
						_pageTourChart.getChartInfo(),
						TourChartView.this);
			}
		});
	}

	public ArrayList<TourData> getSelectedTours() {

		if (_tourData == null) {
			return null;
		}

		final ArrayList<TourData> tourList = new ArrayList<TourData>();
		tourList.add(_tourData);

		return tourList;
	}

	public TourChart getTourChart() {
		return _pageTourChart;
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

				savePreviousTour(selectionTourData);

				updateChart10(selectionTourData);
			}

		} else if (selection instanceof SelectionTourIds) {

			final SelectionTourIds selectionTourId = (SelectionTourIds) selection;
			final ArrayList<Long> tourIds = selectionTourId.getTourIds();
			if (tourIds != null && tourIds.size() > 0) {

				final Long tourId = tourIds.get(0);

				savePreviousTour(tourId);
				updateChart(tourId);
			}

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId selectionTourId = (SelectionTourId) selection;
			final Long tourId = selectionTourId.getTourId();

			updateChart(tourId);

		} else if (selection instanceof SelectionChartInfo) {

			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
			if (chartDataModel != null) {

				final Object tourId = chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
				if (tourId instanceof Long) {

					final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
					if (tourData != null) {

						if (_tourData == null || _tourData.equals(tourData) == false) {
							updateChart10(tourData);
						}

						if (_pageTourChart != null) {

							// set slider position

							final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

							_pageTourChart.setXSliderPosition(new SelectionChartXSliderPosition(
									_pageTourChart,
									chartInfo.leftSliderValuesIndex,
									chartInfo.rightSliderValuesIndex));
						}
					}
				}
			}

		} else if (selection instanceof SelectionChartXSliderPosition) {

			if (_pageTourChart != null) {

				final SelectionChartXSliderPosition xSliderPosition = (SelectionChartXSliderPosition) selection;

				final Chart chart = xSliderPosition.getChart();
				if (chart != null && chart != _pageTourChart) {

					// it's not the same chart, check if it's the same tour

					final Object tourId = chart.getChartDataModel().getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
					if (tourId instanceof Long) {

						final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
						if (tourData != null) {

							if (_tourData.equals(tourData)) {

								// it's the same tour, overwrite chart

								xSliderPosition.setChart(_pageTourChart);
							}
						}
					}
				}

				_pageTourChart.setXSliderPosition(xSliderPosition);
			}

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();
			if (firstElement instanceof TVICatalogComparedTour) {

				updateChart(((TVICatalogComparedTour) firstElement).getTourId());

			} else if (firstElement instanceof TVICompareResultComparedTour) {

				final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
				final TourData tourData = TourManager.getInstance().getTourData(
						compareResultItem.getComparedTourData().getTourId());
				updateChart10(tourData);
			}

		} else if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {
				updateChart(refItem.getTourId());
			}

		} else if (selection instanceof SelectionActiveEditor) {

			final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();

			if (editor instanceof TourEditor) {

				final TourData editorTourData = ((TourEditor) editor).getTourChart().getTourData();

				// prevent loading the same tour when it is not modified
				if (editor.isDirty() == false //
						&& _tourData != null
						&& _tourData.equals(editorTourData)) {
					return;
				}

				updateChart10(editorTourData);
			}

		} else if (selection instanceof SelectionDeletedTours) {

			clearView();
		}
	}

	private void restoreState() {

		/*
		 * restore enum tour chart type
		 */
		final String stateChartTypeName = Util.getStateString(
				_state,
				STATE_SELECTED_TOUR_CHART_TYPE,
				TourChartType.TOUR_CHART.name());

		// set default
		TourChartType chartType = TourChartType.TOUR_CHART;

		try {
			chartType = TourChartType.valueOf(stateChartTypeName);
		} catch (final Exception e) {
			// use default
		}

		_requestedTourChartType = chartType;
	}

	private void savePreviousTour(final long tourId) {
		// TODO Auto-generated method stub

	}

	private void savePreviousTour(final TourData newTourData) {
		
		savedTour = TourDatabase.saveTour(tourData);
	}

	private void saveState() {

		// check if UI is disposed
		if (_pageBook.isDisposed()) {
			return;
		}

		_state.put(STATE_SELECTED_TOUR_CHART_TYPE, _displayedTourChartType.name());
	}

	@Override
	public void setFocus() {

		if (_pageTourChart != null) {

			_pageTourChart.setFocus();

			/*
			 * fire tour selection
			 */
			_postSelectionProvider.setSelection(new SelectionTourData(_pageTourChart, _tourData));

			fireSliderPosition();

		} else if (_pageConconiTest != null) {
			_chartConconiTest.setFocus();
		}
	}

	/**
	 * set toolbar items
	 */
	private void setToolbar() {

		final IContributionItem[] tbmItems = _tbmTourChartView.getItems();

		if (_displayedTourChartType != null) {

			// backup toolbar items for the displayed tour chart

			if (_displayedTourChartType == TourChartType.TOUR_CHART) {
				_tourChartContribItems = tbmItems;
			} else if (_displayedTourChartType == TourChartType.CONCONI_TEST_POWER) {
				_conconiTestContribItems = tbmItems;
			}
		}

		_displayedTourChartType = _requestedTourChartType;
		_requestedTourChartType = null;

		_tbmTourChartView.removeAll();

		/*
		 * fill toolbar when contribution actions are available
		 */
		if (_displayedTourChartType == TourChartType.TOUR_CHART && _tourChartContribItems != null) {

			for (final IContributionItem item : _tourChartContribItems) {
				_tbmTourChartView.add(item);
			}

		} else if (_displayedTourChartType == TourChartType.CONCONI_TEST_POWER && _conconiTestContribItems != null) {

			for (final IContributionItem item : _conconiTestContribItems) {
				_tbmTourChartView.add(item);
			}
		}

		_tbmTourChartView.update(true);
	}

	/**
	 * when dp tolerance was changed set the tour dirty
	 */
	private void setTourDirty() {

		if (_isDirtyDisabled) {
			return;
		}

		if (_tourData != null && _savedDpTolerance != _tourData.getConconiDeflection()) {
			_isTourDirty = true;
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

	private void updateChart(final long tourId) {

		// check if tour is already displayed
		if (_tourData != null && _tourData.getTourId() == tourId) {
			return;
		}

		updateChart10(TourManager.getInstance().getTourData(tourId));

		fireSliderPosition();
	}

	private void updateChart10(final TourData tourData) {

		if (tourData == null) {
			// nothing to do
			return;
		}

		_isDirtyDisabled = true;
		{

			_tourData = tourData;

			updateChart20();

			// keep original dp tolerance
			_savedDpTolerance = _dpTolerance = _tourData.getDpTolerance();

		}
		_isDirtyDisabled = false;

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
			 * force the chart to be repainted, updating conconi layer requires that the chart is
			 * already painted (it requires drawing data)
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
