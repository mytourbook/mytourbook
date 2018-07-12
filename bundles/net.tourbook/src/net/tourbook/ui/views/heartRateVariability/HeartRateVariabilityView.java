/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.heartRateVariability;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.MinMaxKeeper_XData;
import net.tourbook.chart.MinMaxKeeper_YData;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

/**
 * Show selected tours in a Heart rate variability (HRV) chart
 */
public class HeartRateVariabilityView extends ViewPart {

	public static final String			ID										= "net.tourbook.ui.views.heartRateVariability.HeartRateVariabilityView";																		//$NON-NLS-1$

	private static final String			GRAPH_LABEL_HEART_RATE_VARIABILITY		= net.tourbook.common.Messages.Graph_Label_HeartRateVariability;
	private static final String			GRAPH_LABEL_HEART_RATE_VARIABILITY_UNIT	= net.tourbook.common.Messages.Graph_Label_HeartRateVariability_Unit;

	private static final String			STATE_HRV_MIN_TIME						= "STATE_HRV_MIN_TIME";													//$NON-NLS-1$
	private static final String			STATE_HRV_MAX_TIME						= "STATE_HRV_MAX_TIME";													//$NON-NLS-1$
	private static final String			STATE_IS_SHOW_ALL_VALUES				= "STATE_IS_SHOW_ALL_VALUES";											//$NON-NLS-1$
	private static final String			STATE_IS_SYNC_CHART_SCALING				= "STATE_IS_SYNC_CHART_SCALING";										//$NON-NLS-1$

	private static final String			GRID_PREF_PREFIX						= "GRID_HEART_RATE_VARIABILITY__";										//$NON-NLS-1$

	private static final String			GRID_IS_SHOW_VERTICAL_GRIDLINES			= (GRID_PREF_PREFIX			+ ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES);
	private static final String			GRID_IS_SHOW_HORIZONTAL_GRIDLINES		= (GRID_PREF_PREFIX			+ ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES);
	private static final String			GRID_VERTICAL_DISTANCE					= (GRID_PREF_PREFIX			+ ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE);
	private static final String			GRID_HORIZONTAL_DISTANCE				= (GRID_PREF_PREFIX			+ ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE);

	private static final int			ADJUST_PULSE_VALUE						= 50;

	private static final int			HRV_TIME_MIN_BORDER						= 0;																	// ms
	private static final int			HRV_TIME_MAX_BORDER						= 9999;																	//ms

	private final IPreferenceStore		_prefStore								= TourbookPlugin.getPrefStore();
	private final IPreferenceStore		_commonPrefStore						= CommonActivator.getPrefStore();
	private final IDialogSettings		_state									= TourbookPlugin.getState(ID);

	private ModifyListener				_defaultSpinnerModifyListener;
	private MouseWheelListener			_defaultSpinnerMouseWheelListener;
	private SelectionAdapter			_defaultSpinnerSelectionListener;
	private IPartListener2				_partListener;
	private ISelectionListener			_postSelectionListener;
	private IPropertyChangeListener		_prefChangeListener;
	private ITourEventListener			_tourEventListener;

	private ArrayList<TourData>			_hrvTours;

	private ActionToolbarSlideout		_actionHrvOptions;
	private ActionSynchChartScale		_actionSynchChartScaling;
	private ActionShowAllValues			_actionShowAllValues;

	private boolean						_isUpdateUI;
	private boolean						_isSynchChartScaling;
	private boolean						_isShowAllValues;

	/**
	 * E4 calls partClosed() even when not created
	 */
	private boolean						_isPartCreated;

	private final MinMaxKeeper_XData	_xMinMaxKeeper							= new MinMaxKeeper_XData(ADJUST_PULSE_VALUE);
	private final MinMaxKeeper_YData	_yMinMaxKeeper							= new MinMaxKeeper_YData(ADJUST_PULSE_VALUE);

	private int							_fixed2xErrors_0;
	private int							_fixed2xErrors_1;

	private ToolBarManager				_toolbarManager;
	private FormToolkit					_tk;

	/*
	 * UI controls
	 */
	private PageBook					_pageBook;

	private Composite					_page_NoTour;
	private Composite					_page_Chart;
	private Composite					_page_InvalidData;

	private Chart						_chartHRV;

	private Label						_lblInvalidData;
	private Label						_lblMinTime;
	private Label						_lblMaxTime;

	private Spinner						_spinnerMinTime;
	private Spinner						_spinnerMaxTime;

	private class ActionHrvOptions extends ActionToolbarSlideout {

		@Override
		protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

			final SlideoutHRVOptions slideoutHRVOptions = new SlideoutHRVOptions(
					_pageBook,
					toolbar,
					GRID_PREF_PREFIX,
					HeartRateVariabilityView.this);

			return slideoutHRVOptions;
		}
	}

	private class ActionShowAllValues extends Action {

		public ActionShowAllValues() {

			super(UI.EMPTY_STRING, AS_CHECK_BOX);

			setToolTipText(Messages.HRV_View_Action_ShowAllValues);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ZoomFitGraph));
		}

		@Override
		public void run() {
			actionShowAllValues();
		}
	}

	private class ActionSynchChartScale extends Action {

		public ActionSynchChartScale() {

			super(UI.EMPTY_STRING, AS_CHECK_BOX);

			setToolTipText(Messages.HRV_View_Action_SynchChartScale);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__synch_statistics));
			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__synch_statistics_Disabled));
		}

		@Override
		public void run() {
			actionSynchChartScale();
		}
	}

	private void actionShowAllValues() {

		_isShowAllValues = _actionShowAllValues.isChecked();

		if (_isShowAllValues) {
			_actionSynchChartScaling.setChecked(false);
			_isSynchChartScaling = false;
		}

		updateChart_50_CurrentTours(!_isSynchChartScaling);
	}

	private void actionSynchChartScale() {

		_isSynchChartScaling = _actionSynchChartScaling.isChecked();

		if (_isSynchChartScaling == false) {

			_xMinMaxKeeper.resetMinMax();
			_yMinMaxKeeper.resetMinMax();
		}

		if (_isSynchChartScaling) {
			_actionShowAllValues.setChecked(false);
			_isShowAllValues = false;
		}

		updateChart_50_CurrentTours(!_isSynchChartScaling);
	}

	private void addPartListener() {

		getViewSite().getPage().addPartListener(_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == HeartRateVariabilityView.this && _isPartCreated) {
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
		});
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */
				if (property.equals(GRID_HORIZONTAL_DISTANCE)
						|| property.equals(GRID_VERTICAL_DISTANCE)
						|| property.equals(GRID_IS_SHOW_HORIZONTAL_GRIDLINES)
						|| property.equals(GRID_IS_SHOW_VERTICAL_GRIDLINES)
				//
				) {

					// grid has changed

					UI.updateChartProperties(_chartHRV, GRID_PREF_PREFIX);

					updateChart_50_CurrentTours(true);

				} else if (property.equals(ITourbookPreferences.HRV_OPTIONS_IS_FIX_2X_ERROR)
						| property.equals(ITourbookPreferences.HRV_OPTIONS_2X_ERROR_TOLERANCE)) {

					// hrv options has changed
					updateChart_50_CurrentTours(true);
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

				if (part == HeartRateVariabilityView.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {

			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == HeartRateVariabilityView.this) {
					return;
				}

				if (eventId == TourEventId.TOUR_SELECTION && eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void clearView() {

		_hrvTours = null;

		if (_chartHRV != null) {
			_chartHRV.updateChart(null, false);
		}

		showPage(_page_NoTour);

		enableControls();
	}

	private void createActions() {

		_actionHrvOptions = new ActionHrvOptions();
		_actionSynchChartScaling = new ActionSynchChartScale();
		_actionShowAllValues = new ActionShowAllValues();
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
		final ArrayList<TourData> validTourList = new ArrayList<>();

		/*
		 * get all tours which has valid data
		 */
		for (int serieIndex = 0; serieIndex < serieLengthRaw; serieIndex++) {

			final TourData tourData = toursArray[serieIndex];

			if (tourData == null) {
				continue;
			}

			final int[] tdPulseTimeSerie = tourData.pulseTimeSerie;

			// check if required data series are available
			if (tdPulseTimeSerie != null && tdPulseTimeSerie.length > 1) {
				validTourList.add(tourData);
			}
		}

		final int validDataLength = validTourList.size();

		// display error when required data are not available
		if (validDataLength == 0) {
			return null;
		}

		final boolean isFix2xErrors = _prefStore.getBoolean(ITourbookPreferences.HRV_OPTIONS_IS_FIX_2X_ERROR);
		final int error2xTolerance = _prefStore.getInt(ITourbookPreferences.HRV_OPTIONS_2X_ERROR_TOLERANCE);

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

		_fixed2xErrors_0 = 0;
		_fixed2xErrors_1 = 0;

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

				int rr0Value = pulseTimeSerie[valueIndex];
				int rr1Value = pulseTimeSerie[valueIndex + 1];

				if (isFix2xErrors) {

					final double rr0ValueFixed = rr0Value / 2.0;
					final double rr1ValueFixed = rr1Value / 2.0;

					if (rr1Value >= rr0ValueFixed - error2xTolerance && rr1Value <= rr0ValueFixed + error2xTolerance) {
						rr0Value = (int) rr0ValueFixed;
						_fixed2xErrors_0++;
					}

					if (rr0Value >= rr1ValueFixed - error2xTolerance && rr0Value <= rr1ValueFixed + error2xTolerance) {
						rr1Value = (int) rr1ValueFixed;
						_fixed2xErrors_1++;
					}
				}

				rr0Values[valueIndex] = rr0Value;
				rr1Values[valueIndex] = rr1Value;
			}

			rr0Series[tourIndex] = rr0Values;
			rr1Series[tourIndex] = rr1Values;

			rgbLine[tourIndex] = rgbPrefLine;
			rgbDark[tourIndex] = rgbPrefDark;
			rgbBright[tourIndex] = rgbPrefBright;
		}

		if (validDataLength == 1) {
			chartDataModel.setTitle(TourManager.getTourDateTimeShort(validTours[0]));
		}

		/*
		 * X axis: RR
		 */
		final ChartDataXSerie xDataRR0 = new ChartDataXSerie(rr0Series);
		xDataRR0.setLabel(GRAPH_LABEL_HEART_RATE_VARIABILITY);
		xDataRR0.setUnitLabel(GRAPH_LABEL_HEART_RATE_VARIABILITY_UNIT);

		xDataRR0.forceXAxisMinValue(xDataRR0.getOriginalMinValue() - ADJUST_PULSE_VALUE);
		xDataRR0.forceXAxisMaxValue(xDataRR0.getOriginalMaxValue() + ADJUST_PULSE_VALUE);

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

		yDataRR1.forceYAxisMinValue(yDataRR1.getOriginalMinValue() - ADJUST_PULSE_VALUE);
		yDataRR1.forceYAxisMaxValue(yDataRR1.getOriginalMaxValue() + ADJUST_PULSE_VALUE);

		chartDataModel.addYData(yDataRR1);

		if (_isSynchChartScaling && _isShowAllValues == false) {

			// sync scaling between different tours

			_xMinMaxKeeper.setMinMaxValues(chartDataModel);
			_yMinMaxKeeper.setMinMaxValues(chartDataModel);

		} else if (_isShowAllValues == false) {

			// use user min/max values

			final int timeMinValue = _spinnerMinTime.getSelection();
			final int timeMaxValue = _spinnerMaxTime.getSelection();

			xDataRR0.forceXAxisMinValue(timeMinValue);
			xDataRR0.forceXAxisMaxValue(timeMaxValue - 0);

			// + 1 = fix wrong units
			yDataRR1.forceYAxisMinValue(timeMinValue + 1);
			yDataRR1.forceYAxisMaxValue(timeMaxValue - 1);
		}

		return chartDataModel;
	}

	@Override
	public void createPartControl(final Composite parent) {

		initUI(parent);

		createUI(parent);

		createActions();
		fillToolbar();

		restoreState();

		addPartListener();
		addSelectionListener();
		addPrefListener();
		addTourEventListener();

		// show default page
		showPage(_page_NoTour);

		showTour();

		_isPartCreated = true;
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);

		_page_NoTour = UI.createPage(_tk, _pageBook, Messages.UI_Label_TourIsNotSelected);

		_page_InvalidData = _tk.createComposite(_pageBook);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_page_InvalidData);
		GridLayoutFactory.swtDefaults().applyTo(_page_InvalidData);
		{
			_lblInvalidData = _tk.createLabel(_page_InvalidData, Messages.UI_Label_TourIsNotSelected, SWT.WRAP);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblInvalidData);
		}

		_page_Chart = createUI_10_ChartPage(_pageBook);
	}

	private Composite createUI_10_ChartPage(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.spacing(0, 0)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			createUI_20_Toolbar(container);
			createUI_30_Chart(container);
		}

		return container;
	}

	private void createUI_20_Toolbar(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(5)
				.margins(3, 3)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * label: hr min
			 */
			_lblMinTime = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_lblMinTime);
			_lblMinTime.setText(Messages.HRV_View_Label_LeftChartBorder);
			_lblMinTime.setToolTipText(Messages.HRV_View_Label_LeftChartBorder_Tooltip);

			/*
			 * spinner: hr min
			 */
			_spinnerMinTime = new Spinner(container, SWT.BORDER);
			_spinnerMinTime.setMinimum(HRV_TIME_MIN_BORDER);
			_spinnerMinTime.setMaximum(HRV_TIME_MAX_BORDER);
			_spinnerMinTime.addModifyListener(_defaultSpinnerModifyListener);
			_spinnerMinTime.addSelectionListener(_defaultSpinnerSelectionListener);
			_spinnerMinTime.addMouseWheelListener(_defaultSpinnerMouseWheelListener);

			/*
			 * label: hr max
			 */
			_lblMaxTime = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_lblMaxTime);
			_lblMaxTime.setText(Messages.HRV_View_Label_RightChartBorder);
			_lblMaxTime.setToolTipText(Messages.HRV_View_Label_RightChartBorder_Tooltip);

			/*
			 * spinner: hr max
			 */
			_spinnerMaxTime = new Spinner(container, SWT.BORDER);
			_spinnerMaxTime.setMinimum(HRV_TIME_MIN_BORDER);
			_spinnerMaxTime.setMaximum(HRV_TIME_MAX_BORDER);
			_spinnerMaxTime.addModifyListener(_defaultSpinnerModifyListener);
			_spinnerMaxTime.addSelectionListener(_defaultSpinnerSelectionListener);
			_spinnerMaxTime.addMouseWheelListener(_defaultSpinnerMouseWheelListener);

			/*
			 * toolbar actions
			 */
			final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(toolbar);
			_toolbarManager = new ToolBarManager(toolbar);
		}

//		// label: horizontal separator
//		final Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
//		GridDataFactory.fillDefaults()//
//				.grab(true, false)
//				.hint(SWT.DEFAULT, 1)
//				.applyTo(label);
//		label.setText(UI.EMPTY_STRING);
	}

	private void createUI_30_Chart(final Composite parent) {

		_chartHRV = new Chart(parent, SWT.FLAT);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartHRV);

		UI.updateChartProperties(_chartHRV, GRID_PREF_PREFIX);

		// Show title
		_chartHRV.getChartTitleSegmentConfig().isShowSegmentTitle = true;
	}

	@Override
	public void dispose() {

		getSite().getPage().removeSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void enableControls() {

		final boolean isTourAvailable = _hrvTours != null;
		final boolean isCustomScaling = _isShowAllValues == false && _isSynchChartScaling == false;

		_spinnerMinTime.setEnabled(isCustomScaling);
		_spinnerMaxTime.setEnabled(isCustomScaling);
		_lblMinTime.setEnabled(isCustomScaling);
		_lblMaxTime.setEnabled(isCustomScaling);

		_actionSynchChartScaling.setEnabled(isTourAvailable);
		_actionShowAllValues.setEnabled(isTourAvailable);
	}

	/**
	 */
	private void fillToolbar() {

		/*
		 * View toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(_actionHrvOptions);

		/*
		 * Header toolbar
		 */
		_toolbarManager.add(_actionShowAllValues);
		_toolbarManager.add(_actionSynchChartScaling);

		// update toolbar to show added items
		_toolbarManager.update(true);
	}

	public int getFixed2xErrors_0() {
		return _fixed2xErrors_0;
	}

	public int getFixed2xErrors_1() {
		return _fixed2xErrors_1;
	}

	private void initUI(final Composite parent) {

		_tk = new FormToolkit(parent.getDisplay());

		_defaultSpinnerModifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				if (_isUpdateUI) {
					return;
				}
				onModifyMinMaxTime();
			}
		};

		_defaultSpinnerSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (_isUpdateUI) {
					return;
				}
				onModifyMinMaxTime();
			}
		};

		_defaultSpinnerMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				Util.adjustSpinnerValueOnMouseScroll(event);
				if (_isUpdateUI) {
					return;
				}
				onModifyMinMaxTime();
			}
		};
	}

	private void onModifyMinMaxTime() {

		int left = _spinnerMinTime.getSelection();
		int right = _spinnerMaxTime.getSelection();

		_isUpdateUI = true;
		{
			if (left == HRV_TIME_MIN_BORDER && right == HRV_TIME_MIN_BORDER) {

				right++;
				_spinnerMaxTime.setSelection(right);

			} else if (left == HRV_TIME_MAX_BORDER && right == HRV_TIME_MAX_BORDER) {

				left--;
				_spinnerMinTime.setSelection(left);

			} else if (left >= right) {

				left = right - 1;
				_spinnerMinTime.setSelection(left);
			}
		}
		_isUpdateUI = false;

		updateChart_50_CurrentTours(!_isSynchChartScaling);
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

		_isShowAllValues = Util.getStateBoolean(_state, STATE_IS_SHOW_ALL_VALUES, true);
		_actionShowAllValues.setChecked(_isShowAllValues);

		_isSynchChartScaling = Util.getStateBoolean(_state, STATE_IS_SYNC_CHART_SCALING, false);
		_actionSynchChartScaling.setChecked(_isSynchChartScaling);

		_isUpdateUI = true;
		{
			_spinnerMinTime.setSelection(Util.getStateInt(_state, STATE_HRV_MIN_TIME, 200));
			_spinnerMaxTime.setSelection(Util.getStateInt(_state, STATE_HRV_MAX_TIME, 1000));
		}
		_isUpdateUI = false;
	}

	private void saveState() {

		_state.put(STATE_HRV_MIN_TIME, _spinnerMinTime.getSelection());
		_state.put(STATE_HRV_MAX_TIME, _spinnerMaxTime.getSelection());

		_state.put(STATE_IS_SHOW_ALL_VALUES, _actionShowAllValues.isChecked());
		_state.put(STATE_IS_SYNC_CHART_SCALING, _actionSynchChartScaling.isChecked());
	}

	@Override
	public void setFocus() {

		if (_page_Chart != null && _page_Chart.isVisible()) {
			_chartHRV.setFocus();
		}
	}

	private void showPage(final Composite page) {

		_pageBook.showPage(page);

		final boolean isEnableOptions = page == _page_Chart;

		_actionHrvOptions.setEnabled(isEnableOptions);
	}

	private void showTour() {

		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (_hrvTours == null) {
			showTourFromTourProvider();
		}

		enableControls();
	}

	private void showTourFromTourProvider() {

		// a tour is not displayed, find a tour provider which provides a tour
		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {

				// validate widget
				if (_pageBook.isDisposed()) {
					return;
				}

				/*
				 * check if tour was set from a selection provider
				 */
				if (_hrvTours != null) {
					return;
				}

				final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();
				if (selectedTours != null && selectedTours.size() > 0) {
					updateChart_22(selectedTours);
				}
			}
		});
	}

	private void updateChart_10(final Long tourId) {

		final ArrayList<Long> tourIds = new ArrayList<>();
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

		final ArrayList<TourData> tourDataList = new ArrayList<>();
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

		updateChart_50_CurrentTours(true);

		return;
	}

	/**
	 * @param isShowAllData
	 */
	private void updateChart_50_CurrentTours(final boolean isShowAllData) {

		enableControls();

		if (_hrvTours == null) {

			showPage(_page_NoTour);
			return;
		}

		final ChartDataModel chartDataModel = createChartDataModel(_hrvTours);

		if (chartDataModel == null) {

			// currently only the first tour is used -> this can be improved when necessary
			final TourData tourData = _hrvTours.get(0);

			// update page info
			_lblInvalidData.setText(NLS.bind(
					Messages.HRV_View_Label_InvalidData,
					TourManager.getTourDateTimeShort(tourData)));

			showPage(_page_InvalidData);
			return;
		}

		_chartHRV.updateChart(chartDataModel, true, isShowAllData);

		showPage(_page_Chart);
	}

}
