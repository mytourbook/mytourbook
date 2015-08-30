/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
import java.util.HashMap;
import java.util.Map;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartComponentAxis;
import net.tourbook.chart.ChartComponents;
import net.tourbook.chart.ChartCursor;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartDrawingData;
import net.tourbook.chart.ChartMouseEvent;
import net.tourbook.chart.ChartSegment;
import net.tourbook.chart.ChartSegmentConfig;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.ChartYDataMinMaxKeeper;
import net.tourbook.chart.IChartLayer;
import net.tourbook.chart.IFillPainter;
import net.tourbook.chart.IHoveredValueListener;
import net.tourbook.chart.IMouseListener;
import net.tourbook.chart.ITooltipOwner;
import net.tourbook.chart.MouseAdapter;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.PointLong;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.util.IToolTipHideListener;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.photo.Photo;
import net.tourbook.photo.TourPhotoLink;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceTourChart;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourMarkerModifyListener;
import net.tourbook.tour.ITourModifyListener;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourInfoIconToolTipProvider;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.tourChart.action.ActionCanAutoZoomToSlider;
import net.tourbook.ui.tourChart.action.ActionCanMoveSlidersWhenZoomed;
import net.tourbook.ui.tourChart.action.ActionGraph;
import net.tourbook.ui.tourChart.action.ActionGraphOverlapped;
import net.tourbook.ui.tourChart.action.ActionHrZoneDropDownMenu;
import net.tourbook.ui.tourChart.action.ActionHrZoneGraphType;
import net.tourbook.ui.tourChart.action.ActionShowBreaktimeValues;
import net.tourbook.ui.tourChart.action.ActionShowSRTMData;
import net.tourbook.ui.tourChart.action.ActionShowStartTime;
import net.tourbook.ui.tourChart.action.ActionShowValuePointToolTip;
import net.tourbook.ui.tourChart.action.ActionTourChartInfo;
import net.tourbook.ui.tourChart.action.ActionTourChartMarker;
import net.tourbook.ui.tourChart.action.ActionTourChartOptions;
import net.tourbook.ui.tourChart.action.ActionTourPhotos;
import net.tourbook.ui.tourChart.action.ActionXAxisDistance;
import net.tourbook.ui.tourChart.action.ActionXAxisTime;
import net.tourbook.ui.views.tourSegmenter.SelectedTourSegmenterSegments;
import net.tourbook.ui.views.tourSegmenter.TourSegmenterView;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;

/**
 * The tour chart extends the chart with all the functionality for a tour chart
 */
public class TourChart extends Chart implements ITourProvider, ITourMarkerUpdater {

	private static final String			ID										= "net.tourbook.ui.tourChart";								//$NON-NLS-1$

	private static final String			GRAPH_LABEL_ALTIMETER					= net.tourbook.common.Messages.Graph_Label_Altimeter;
	private static final String			GRAPH_LABEL_ALTITUDE					= net.tourbook.common.Messages.Graph_Label_Altitude;
	private static final String			GRAPH_LABEL_CADENCE						= net.tourbook.common.Messages.Graph_Label_Cadence;
	private static final String			GRAPH_LABEL_GEARS						= net.tourbook.common.Messages.Graph_Label_Gears;
	private static final String			GRAPH_LABEL_GRADIENT					= net.tourbook.common.Messages.Graph_Label_Gradient;
	private static final String			GRAPH_LABEL_HEARTBEAT					= net.tourbook.common.Messages.Graph_Label_Heartbeat;
	private static final String			GRAPH_LABEL_PACE						= net.tourbook.common.Messages.Graph_Label_Pace;
	private static final String			GRAPH_LABEL_POWER						= net.tourbook.common.Messages.Graph_Label_Power;
	private static final String			GRAPH_LABEL_SPEED						= net.tourbook.common.Messages.Graph_Label_Speed;
	private static final String			GRAPH_LABEL_TEMPERATURE					= net.tourbook.common.Messages.Graph_Label_Temperature;
	private static final String			GRAPH_LABEL_TOUR_COMPARE				= net.tourbook.common.Messages.Graph_Label_Tour_Compare;

	public static final String			ACTION_ID_CAN_AUTO_ZOOM_TO_SLIDER		= "ACTION_ID_CAN_AUTO_ZOOM_TO_SLIDER";						//$NON-NLS-1$
	public static final String			ACTION_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED	= "ACTION_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED";				//$NON-NLS-1$
	public static final String			ACTION_ID_EDIT_CHART_PREFERENCES		= "ACTION_ID_EDIT_CHART_PREFERENCES";						//$NON-NLS-1$
	private static final String			ACTION_ID_IS_GRAPH_OVERLAPPED			= "ACTION_ID_IS_GRAPH_OVERLAPPED";							//$NON-NLS-1$
	public static final String			ACTION_ID_IS_SHOW_BREAKTIME_VALUES		= "ACTION_ID_IS_SHOW_BREAKTIME_VALUES";					//$NON-NLS-1$
	public static final String			ACTION_ID_IS_SHOW_SRTM_DATA				= "ACTION_ID_IS_SHOW_SRTM_DATA";							//$NON-NLS-1$
	public static final String			ACTION_ID_IS_SHOW_START_TIME			= "ACTION_ID_IS_SHOW_START_TIME";							//$NON-NLS-1$
	public static final String			ACTION_ID_IS_SHOW_TOUR_PHOTOS			= "ACTION_ID_IS_SHOW_TOUR_PHOTOS";							//$NON-NLS-1$
	public static final String			ACTION_ID_IS_SHOW_VALUEPOINT_TOOLTIP	= "ACTION_ID_IS_SHOW_VALUEPOINT_TOOLTIP";					//$NON-NLS-1$
	public static final String			ACTION_ID_HR_ZONE_DROPDOWN_MENU			= "ACTION_ID_HR_ZONE_DROPDOWN_MENU";						//$NON-NLS-1$
	public static final String			ACTION_ID_HR_ZONE_STYLE_GRAPH_TOP		= "ACTION_ID_HR_ZONE_STYLE_GRAPH_TOP";						//$NON-NLS-1$
	public static final String			ACTION_ID_HR_ZONE_STYLE_NO_GRADIENT		= "ACTION_ID_HR_ZONE_STYLE_NO_GRADIENT";					//$NON-NLS-1$
	public static final String			ACTION_ID_HR_ZONE_STYLE_WHITE_BOTTOM	= "ACTION_ID_HR_ZONE_STYLE_WHITE_BOTTOM";					//$NON-NLS-1$
	public static final String			ACTION_ID_HR_ZONE_STYLE_WHITE_TOP		= "ACTION_ID_HR_ZONE_STYLE_WHITE_TOP";						//$NON-NLS-1$
	public static final String			ACTION_ID_X_AXIS_DISTANCE				= "ACTION_ID_X_AXIS_DISTANCE";								//$NON-NLS-1$
	public static final String			ACTION_ID_X_AXIS_TIME					= "ACTION_ID_X_AXIS_TIME";									//$NON-NLS-1$

	/**
	 * 1e-5 is too small for the min value, it do not correct the graph.
	 */
	public static final double			MIN_ADJUSTMENT							= 1e-3;
	public static final double			MAX_ADJUSTMENT							= 1e-5;

	private final IPreferenceStore		_prefStore								= TourbookPlugin.getPrefStore();
	private final IDialogSettings		_state									= TourbookPlugin.getState(ID);
	private final IDialogSettings		_tourSegmenterState						= TourSegmenterView.getState();								;

	/**
	 * Part in which the tour chart is created, can be <code>null</code> when created in a dialog.
	 */
	private IWorkbenchPart				_part;

	private TourData					_tourData;
	private TourChartConfiguration		_tcc;

	private Map<String, Action>			_allTourChartActions;
	private ActionOpenMarkerDialog		_actionOpenMarkerDialog;
	private ActionTourChartInfo			_actionTourInfo;
	private ActionTourChartMarker		_actionTourMarker;
	private ActionTourChartOptions		_actionTourOptions;

	/**
	 * datamodel listener is called when the chart data is created
	 */
	private IDataModelListener			_chartDataModelListener;

	private IPropertyChangeListener		_prefChangeListener;
	private final ListenerList			_tourMarkerModifyListener				= new ListenerList();
	private final ListenerList			_tourMarkerSelectionListener			= new ListenerList();
	private final ListenerList			_tourModifyListener						= new ListenerList();
	private final ListenerList			_xAxisSelectionListener					= new ListenerList();
	//
	private boolean						_is2ndAltiLayerVisible;
	private boolean						_isMouseModeSet;
	private boolean						_isDisplayedInDialog;

	private TourMarker					_firedTourMarker;

	/**
	 * The {@link TourMarker} selection state is <b>only</b> be displayed when the mouse is hovering
	 * it.
	 */
	private TourMarker					_selectedTourMarker;

	private ImageDescriptor				_imagePhoto								= TourbookPlugin
																						.getImageDescriptor(Messages.Image__PhotoPhotos);
	private ImageDescriptor				_imagePhotoTooltip						= TourbookPlugin
																						.getImageDescriptor(Messages.Image__PhotoImage);
	private IFillPainter				_hrZonePainter;

	private OpenDialogManager			_openDlgMgr								= new OpenDialogManager();

	private TourToolTip					_tourInfoIconTooltip;
	private TourInfoIconToolTipProvider	_tourInfoIconTooltipProvider;
	private ChartPhotoToolTip			_photoTooltip;

	private ChartMarkerToolTip			_tourMarkerTooltip;
	private ChartTitleToolTip			_tourTitleTooltip;
	private ValuePointToolTipUI			_valuePointTooltip;
	private ControlListener				_ttControlListener						= new ControlListener();

	private IMouseListener				_mouseMarkerListener					= new MouseMarkerListener();
	private IMouseListener				_mousePhotoListener						= new MousePhotoListener();
	private IMouseListener				_mouseSegmentLabel_Listener				= new MouseListener_SegmentLabel();
	private IMouseListener				_mouseSegmentLabel_MoveListener			= new MouseListener_SegmentLabel_Move();
	private IMouseListener				_mouseSegmentTitle_Listener				= new MouseListener_SegmentTitle();
	private IMouseListener				_mouseSegmentTitle_MoveListener			= new MouseListener_SegmentTitle_Move();

	private long						_hoveredSegmentTitleEventTime;

	private boolean						_isSegmentLabelHovered;
	private long						_hoveredSegmentLabelEventTime;
	private ChartLabel					_hoveredSegmentLabel;

	private ChartLabel					_selectedSegmentLabel_1;
	private ChartLabel					_selectedSegmentLabel_2;

	private boolean						_isSegmentTitleHovered;
	private ChartSegment				_hoveredSegmentTitle;

	private boolean						_isTourSegmenterVisible;
	private Font						_segmenterValueFont;

	private ActionEditQuick				_actionEditQuick;

	/*
	 * UI controls
	 */
	private Composite					_parent;

	private I2ndAltiLayer				_layer2ndAlti;

	private ChartLayerMarker			_layerMarker;

	private ChartLayer2ndAltiSerie		_layer2ndAltiSerie;
	private ChartLayerPhoto				_layerPhoto;
	private ChartLayerSegmentAltitude	_layerTourSegmenterAltitude;
	private ChartLayerSegmentValue		_layerTourSegmenterOther;

	private Color						_photoOverlayBGColorLink;
	private Color						_photoOverlayBGColorTour;

	/**
	 * This listener is added to ALL widgets within the tooltip shell.
	 */
	private class ControlListener implements Listener {

		TourChart	__tourChart	= TourChart.this;

		@Override
		public void handleEvent(final Event event) {

			if (__tourChart.isDisposed()) {
				return;
			}

			if (event.widget instanceof Control) {

				switch (event.type) {
				case SWT.MouseEnter:

					break;

				case SWT.MouseExit:

					// check if photo tooltip is displayed
					final Shell photoTTShell = _photoTooltip.getShell();
					if (photoTTShell == null || photoTTShell.isDisposed()) {
						return;
					}

					boolean isHide = false;

					// check what is hovered with the mouse after the MouseExit event is fired, can be null
					final Control hoveredControl = __tourChart.getDisplay().getCursorControl();

					if (hoveredControl == null) {

						isHide = true;

					} else {

						/*
						 * check if the hovered control is the owner control, if not, hide the
						 * tooltip
						 */

						final ChartComponents chartComponents = getChartComponents();
						final ChartComponentAxis axisLeft = chartComponents.getAxisLeft();
						final ChartComponentAxis axisRight = chartComponents.getAxisRight();

						Control parent = hoveredControl;

						while (true) {

							if (parent == photoTTShell) {
								// mouse is over the photo tooltip
								break;
							}

							if (parent == axisLeft || parent == axisRight) {
								// mouse is hovering the y axis
								break;
							}

							parent = parent.getParent();

							if (parent == null) {
								// mouse has left the tourchart and the photo tooltip
								isHide = true;
								break;
							}
						}

					}

					if (isHide) {
						_photoTooltip.hide();
					}

					break;
				}
			}
		}
	}

	private class HoveredValueListener implements IHoveredValueListener {

		@Override
		public void hideTooltip() {

			if (_photoTooltip != null) {
				_photoTooltip.hide();
			}
		}

		@Override
		public void hoveredValue(	final long eventTime,
									final int devXMouseMove,
									final int devYMouseMove,
									final int hoveredValueIndex,
									final PointLong devHoveredValue) {

			if (_tourData == null) {
				return;
			}

			if (_tcc.isShowTourPhotos == false) {
				return;
			}

			// check if photos are available
			if (_tourData.tourPhotoLink == null && _tourData.getTourPhotos().size() == 0) {
				return;
			}

			if (_layerPhoto == null) {
				return;
			}

			if (_tcc.isShowTourPhotoTooltip) {

				_photoTooltip.showChartPhotoToolTip(
						_layerPhoto,
						eventTime,
						devHoveredValue,
						devXMouseMove,
						devYMouseMove);
			}
		}

	}

	private class MouseListener_SegmentLabel extends MouseAdapter {

		@Override
		public void chartResized() {
			onSegmentLabel_Reset();
		}

		@Override
		public void mouseDown(final ChartMouseEvent event) {
			onSegmentLabel_MouseDown(event);
		}

		@Override
		public void mouseExit() {
			onSegmentLabel_Reset();
		}

		@Override
		public void mouseUp(final ChartMouseEvent event) {
			onSegmentLabel_MouseUp(event);
		}
	}

	/**
	 * This mouse move listener is used to get mouse move events to show the tour tooltip when the
	 * y-slider is dragged.
	 */
	private class MouseListener_SegmentLabel_Move extends MouseAdapter {

		@Override
		public void mouseMove(final ChartMouseEvent event) {
			onSegmentLabel_MouseMove(event);
		}
	}

	private class MouseListener_SegmentTitle extends MouseAdapter {

		@Override
		public void chartResized() {
			onSegmentTitle_Resized();
		}

		@Override
		public void mouseDoubleClick(final ChartMouseEvent event) {
			onSegmentTitle_MouseDoubleClick(event);
		}

		@Override
		public void mouseDown(final ChartMouseEvent event) {
			onSegmentTitle_MouseDown(event);
		}

		@Override
		public void mouseExit() {
			onSegmentTitle_MouseExit();
		}
	}

	/**
	 * This mouse move listener is used to get mouse move events to show the tour tooltip when the
	 * y-slider is dragged.
	 */
	private class MouseListener_SegmentTitle_Move extends MouseAdapter {

		@Override
		public void mouseMove(final ChartMouseEvent event) {
			onSegmentTitle_MouseMove(event);
		}
	}

	private class MouseMarkerListener extends MouseAdapter {

		@Override
		public void chartResized() {
			onMarker_ChartResized();
		}

		@Override
		public void mouseDoubleClick(final ChartMouseEvent event) {
			onMarker_MouseDoubleClick(event);
		}

		@Override
		public void mouseDown(final ChartMouseEvent event) {
			onMarker_MouseDown(event);
		}

		@Override
		public void mouseExit() {
			onMarker_MouseExit();
		}

		@Override
		public void mouseMove(final ChartMouseEvent event) {
			onMarker_MouseMove(event);
		}

		@Override
		public void mouseUp(final ChartMouseEvent event) {
			onMarker_MouseUp(event);
		}
	}

	private class MousePhotoListener extends MouseAdapter {

		@Override
		public void chartResized() {
			onPhoto_ChartResized();
		}

		@Override
		public void mouseExit() {
			onPhoto_MouseExit();
		}

		@Override
		public void mouseMove(final ChartMouseEvent event) {
			onPhoto_MouseMove(event);
		}
	}

	/**
	 * @param parent
	 * @param style
	 * @param part
	 *            Part in which the tour chart is created, can be <code>null</code> when created in
	 *            a dialog.
	 */
	public TourChart(final Composite parent, final int style, final IWorkbenchPart part) {

		super(parent, style);

		_parent = parent;
		_part = part;

//		/*
//		 * when the focus is changed, fire a tour chart selection, this is neccesarry to update the
//		 * tour markers when a tour chart got the focus
//		 */
//		addFocusListener(new Listener() {
//			public void handleEvent(final Event event) {
////				fireTourChartSelection();
//			}
//		});

		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		addControlListener(this);
		addPrefListeners();

		final GraphColorManager colorProvider = GraphColorManager.getInstance();

		_photoOverlayBGColorLink = new Color(getDisplay(), //
				colorProvider.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_HISTORY).getLineColor_Active());
		_photoOverlayBGColorTour = new Color(getDisplay(), //
				colorProvider.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_TOUR).getLineColor_Active());

		setupChartConfig();

		setShowMouseMode();

		/*
		 * setup tour info icon into the left axis
		 */
		_tourInfoIconTooltipProvider = new TourInfoIconToolTipProvider();
		_tourInfoIconTooltip = new TourToolTip(getToolTipControl());
		_tourInfoIconTooltip.addToolTipProvider(_tourInfoIconTooltipProvider);

		_tourInfoIconTooltip.addHideListener(new IToolTipHideListener() {
			@Override
			public void afterHideToolTip(final Event event) {

				// hide hovered image
				getToolTipControl().afterHideToolTip(event);
			}
		});
		setTourInfoIconToolTipProvider(_tourInfoIconTooltipProvider);

		/*
		 * Setup tour title tooltip
		 */
		_tourTitleTooltip = new ChartTitleToolTip(this);

		/*
		 * setup value point tooltip
		 */
		final ITooltipOwner vpToolTipOwner = new ITooltipOwner() {

			@Override
			public Control getControl() {
				return getValuePointControl();
			}

			@Override
			public void handleMouseEvent(final Event event, final Point mouseDisplayPosition) {
				handleTooltipMouseEvent(event, mouseDisplayPosition);
			}
		};
		_valuePointTooltip = new ValuePointToolTipUI(vpToolTipOwner, _state);
		setValuePointToolTipProvider(_valuePointTooltip);

		_photoTooltip = new ChartPhotoToolTip(this, _state);
		_tourMarkerTooltip = new ChartMarkerToolTip(this);

		// show delayed that it is not flickering when moving the mouse fast
		_tourMarkerTooltip.setFadeInDelayTime(50);

		setHoveredListener(new HoveredValueListener());
	}

	public void actionCanAutoMoveSliders(final boolean isItemChecked) {

		setCanAutoMoveSliders(isItemChecked);

		// apply setting to the chart
		if (isItemChecked) {
			onExecuteMoveSlidersToBorder();
//			onExecuteZoomInWithSlider();
		}

		updateZoomOptionActionHandlers();
	}

	public void actionCanAutoZoomToSlider(final Boolean isItemChecked) {

		setCanAutoZoomToSlider(isItemChecked);

		// apply setting to the chart
//		if (isItemChecked) {
//			zoomInWithSlider();
//		} else {
//			zoomOut(true);
//		}

		updateZoomOptionActionHandlers();
	}

	public void actionCanScrollChart(final Boolean isItemChecked) {

		// apply setting to the chart
		if (isItemChecked) {
			onExecuteZoomInWithSlider();
		} else {
			onExecuteZoomOut(true);
		}

		updateZoomOptionActionHandlers();
	}

	public void actionGraphOverlapped(final boolean isItemChecked) {

		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_GRAPH_OVERLAPPED, isItemChecked);

		_tcc.isGraphOverlapped = isItemChecked;
		updateTourChart();

		setActionChecked(ACTION_ID_IS_GRAPH_OVERLAPPED, isItemChecked);
	}

	public void actionShowBreaktimeValues(final boolean isItemChecked) {

		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_BREAKTIME_VALUES_VISIBLE, isItemChecked);
		_tcc.isShowBreaktimeValues = isItemChecked;

		updateTourChart();

		setActionChecked(ACTION_ID_IS_SHOW_BREAKTIME_VALUES, isItemChecked);
	}

	/**
	 * Toggle HR zone background
	 */
	public void actionShowHrZones() {

		final boolean isHrZonevisible = !_tcc.isHrZoneDisplayed;

		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_HR_ZONE_BACKGROUND_VISIBLE, isHrZonevisible);
		_tcc.isHrZoneDisplayed = isHrZonevisible;

		updateTourChart();
	}

	/**
	 * @param isActionChecked
	 * @param selectedGraphType
	 */
	public void actionShowHrZoneStyle(final Boolean isActionChecked, final String selectedGraphType) {

		final String previousGraphType = _tcc.hrZoneStyle;

		// check if the same action was selected
		if (isActionChecked && selectedGraphType.equals(previousGraphType)) {
//			return;
		}

		_prefStore.setValue(ITourbookPreferences.GRAPH_HR_ZONE_STYLE, selectedGraphType);
		_tcc.hrZoneStyle = selectedGraphType;

		setActionChecked(ACTION_ID_HR_ZONE_STYLE_GRAPH_TOP, //
				ACTION_ID_HR_ZONE_STYLE_GRAPH_TOP.equals(selectedGraphType));
		setActionChecked(ACTION_ID_HR_ZONE_STYLE_NO_GRADIENT, //
				ACTION_ID_HR_ZONE_STYLE_NO_GRADIENT.equals(selectedGraphType));
		setActionChecked(ACTION_ID_HR_ZONE_STYLE_WHITE_TOP, //
				ACTION_ID_HR_ZONE_STYLE_WHITE_TOP.equals(selectedGraphType));
		setActionChecked(ACTION_ID_HR_ZONE_STYLE_WHITE_BOTTOM,//
				ACTION_ID_HR_ZONE_STYLE_WHITE_BOTTOM.equals(selectedGraphType));

		if (_tcc.isHrZoneDisplayed == false) {
			// HR zones are not yet displayed
			actionShowHrZones();
		} else {
			updateTourChart();
		}
	}

	public void actionShowSRTMData(final boolean isItemChecked) {

		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_SRTM_VISIBLE, isItemChecked);

		_tcc.isSRTMDataVisible = isItemChecked;
		updateTourChart();

		setActionChecked(ACTION_ID_IS_SHOW_SRTM_DATA, isItemChecked);
	}

	public void actionShowStartTime(final Boolean isItemChecked) {

		_tcc.xAxisTime = isItemChecked ? X_AXIS_START_TIME.TOUR_START_TIME : X_AXIS_START_TIME.START_WITH_0;
		updateTourChart();

		setActionChecked(ACTION_ID_IS_SHOW_START_TIME, isItemChecked);
	}

	public void actionShowTourInfo(final boolean isTourInfoVisible) {

		_prefStore.setValue(ITourbookPreferences.GRAPH_TOUR_INFO_IS_VISIBLE, isTourInfoVisible);

		_tcc.isTourInfoVisible = isTourInfoVisible;

		updateUI_TourTitleInfo();
	}

	public void actionShowTourMarker(final Boolean isMarkerVisible) {

		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_MARKER_VISIBLE, isMarkerVisible);

		updateUI_Marker(isMarkerVisible);
	}

	public void actionShowTourPhotos() {

		boolean isShowPhotos = _tcc.isShowTourPhotos;
		boolean isShowTooltip = _tcc.isShowTourPhotoTooltip;

		if (isShowPhotos && isShowTooltip) {

			isShowPhotos = true;
			isShowTooltip = false;

		} else if (isShowPhotos) {

			isShowPhotos = false;
			isShowTooltip = false;

		} else {

			isShowPhotos = true;
			isShowTooltip = true;
		}

		_tcc.isShowTourPhotos = isShowPhotos;
		_tcc.isShowTourPhotoTooltip = isShowTooltip;

		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_TOUR_PHOTO_VISIBLE, isShowPhotos);
		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_TOUR_PHOTO_TOOLTIP_VISIBLE, isShowTooltip);

		updatePhotoAction();

		updateTourChart();
	}

	public void actionShowValuePointToolTip(final Boolean isItemChecked) {

		// set in pref store, tooltip is listening pref store modifications
		_prefStore.setValue(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE, isItemChecked);

//		_valuePointToolTip.setVisible(isItemChecked);

		setActionChecked(ACTION_ID_IS_SHOW_VALUEPOINT_TOOLTIP, isItemChecked);

		// chart needs not to be updated but update the actions
		updateTourActions();
	}

	/**
	 * Set the X-axis to distance
	 * 
	 * @param isChecked
	 */
	public void actionXAxisDistance(final boolean isChecked) {

		// check if the distance axis button was pressed
		if (isChecked && !_tcc.isShowTimeOnXAxis) {
			return;
		}

		if (isChecked) {

			// show distance on x axis

			_tcc.isShowTimeOnXAxis = !_tcc.isShowTimeOnXAxis;
			_tcc.isShowTimeOnXAxisBackup = _tcc.isShowTimeOnXAxis;

			switchSlidersTo2ndXData();
			updateTourChart(false);
		}

		// toggle time and distance buttons
		setActionChecked(ACTION_ID_X_AXIS_TIME, !isChecked);
		setActionChecked(ACTION_ID_X_AXIS_DISTANCE, isChecked);
	}

	/**
	 * @param isChecked
	 */
	public void actionXAxisTime(final boolean isChecked) {

		// check if the time axis button was already pressed
		if (isChecked && _tcc.isShowTimeOnXAxis) {

			// x-axis already shows time, toggle between tour start time and tour time

			final X_AXIS_START_TIME configXAxisTime = _tcc.xAxisTime;

			if (_tourData.getPhotoTimeAdjustment() > 0) {

				if (configXAxisTime == X_AXIS_START_TIME.START_WITH_0) {

					_tcc.xAxisTime = X_AXIS_START_TIME.TOUR_START_TIME;

				} else if (configXAxisTime == X_AXIS_START_TIME.TOUR_START_TIME) {

					_tcc.xAxisTime = X_AXIS_START_TIME.PHOTO_TIME;

				} else {
					_tcc.xAxisTime = X_AXIS_START_TIME.START_WITH_0;
				}

			} else {

				/*
				 * there is no photo time adjustment, toggle between relative and absolute time
				 */

				_tcc.xAxisTime = configXAxisTime == X_AXIS_START_TIME.START_WITH_0
						? X_AXIS_START_TIME.TOUR_START_TIME
						: X_AXIS_START_TIME.START_WITH_0;
			}

			/**
			 * keepMinMaxValues must be set to false, that a deeply zoomed in chart can display
			 * x-axis units
			 */
			updateTourChart(false);

			setActionChecked(ACTION_ID_IS_SHOW_START_TIME, _tcc.xAxisTime != X_AXIS_START_TIME.START_WITH_0);

			return;
		}

		if (isChecked) {

			// show time on x axis

			_tcc.isShowTimeOnXAxis = !_tcc.isShowTimeOnXAxis;
			_tcc.isShowTimeOnXAxisBackup = _tcc.isShowTimeOnXAxis;

			switchSlidersTo2ndXData();
			updateTourChart(false);
		}

		// toggle time and distance buttons
		setActionChecked(ACTION_ID_X_AXIS_TIME, isChecked);
		setActionChecked(ACTION_ID_X_AXIS_DISTANCE, !isChecked);

		fireXAxisSelection(_tcc.isShowTimeOnXAxis);
	}

	/**
	 * ########################### Recursive #########################################<br>
	 * <p>
	 * Add listener to all controls within the tour chart
	 * <p>
	 * ########################### Recursive #########################################<br>
	 * 
	 * @param control
	 */
	private void addControlListener(final Control control) {

		control.addListener(SWT.MouseExit, _ttControlListener);
		control.addListener(SWT.MouseEnter, _ttControlListener);

		if (control instanceof Composite) {
			final Control[] children = ((Composite) control).getChildren();
			for (final Control child : children) {
				addControlListener(child);
			}
		}
	}

	/**
	 * add a data model listener which is fired when the data model has changed
	 * 
	 * @param dataModelListener
	 */
	public void addDataModelListener(final IDataModelListener dataModelListener) {
		_chartDataModelListener = dataModelListener;
	}

	private void addPrefListeners() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				if (_tcc == null) {
					return;
				}

				final String property = event.getProperty();

				boolean isChartModified = false;
				boolean keepMinMax = true;

				if (property.equals(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED)
						|| property.equals(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER)) {

					// zoom preferences has changed

					_tcc.updateZoomOptions();

					isChartModified = true;

				} else if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {

					/*
					 * when the chart is computed, the modified colors are read from the preferences
					 */
					isChartModified = true;

					// dispose old colors
					disposeColors();

				} else if (property.equals(ITourbookPreferences.GRAPH_ANTIALIASING)
						|| property.equals(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR)
						|| property.equals(ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR)
						|| property.equals(ITourbookPreferences.GRAPH_TRANSPARENCY_LINE)
						|| property.equals(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES)
				//
				) {

					setupChartConfig();

					isChartModified = true;

				} else if (property.equals(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE)) {

					final Boolean isVisible = (Boolean) event.getNewValue();

					setActionChecked(ACTION_ID_IS_SHOW_VALUEPOINT_TOOLTIP, isVisible);

					// chart needs not to be updated but update the actions
//					enableTourActions();

				} else if (property.equals(ITourbookPreferences.TOUR_SEGMENTER_CHART_VALUE_FONT)) {

					setupSegmenterValueFont();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

//					UI.updateUnits();

					isChartModified = true;
					keepMinMax = false;

					_valuePointTooltip.reopen();

				}

				/*
				 * Altimeter
				 */
				isChartModified |= setMinDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_ALTIMETER_IS_MIN_ENABLED,
						ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE,
						TourManager.GRAPH_ALTIMETER,
						0,
						MIN_ADJUSTMENT);

				isChartModified |= setMaxDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_ALTIMETER_IS_MAX_ENABLED,
						ITourbookPreferences.GRAPH_ALTIMETER_MAX_VALUE,
						TourManager.GRAPH_ALTIMETER,
						0,
						1e-2);

				/*
				 * Gradient
				 */
				isChartModified |= setMinDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_GRADIENT_IS_MIN_ENABLED,
						ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE,
						TourManager.GRAPH_GRADIENT,
						0,
						MIN_ADJUSTMENT);

				isChartModified |= setMaxDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_GRADIENT_IS_MAX_ENABLED,
						ITourbookPreferences.GRAPH_GRADIENT_MAX_VALUE,
						TourManager.GRAPH_GRADIENT,
						0,
						MAX_ADJUSTMENT);

				/*
				 * Pace
				 */
				isChartModified |= setMinDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_PACE_IS_MIN_ENABLED,
						ITourbookPreferences.GRAPH_PACE_MIN_VALUE,
						TourManager.GRAPH_PACE,
						60,
						Double.MIN_VALUE);

				isChartModified |= setMaxDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_PACE_IS_MAX_ENABLED,
						ITourbookPreferences.GRAPH_PACE_MAX_VALUE,
						TourManager.GRAPH_PACE,
						60,
						Double.MIN_VALUE);

				/*
				 * Pulse
				 */
				isChartModified |= setMinDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_PULSE_IS_MIN_ENABLED,
						ITourbookPreferences.GRAPH_PULSE_MIN_VALUE,
						TourManager.GRAPH_PULSE,
						0,
						MIN_ADJUSTMENT);

				isChartModified |= setMaxDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_PULSE_IS_MAX_ENABLED,
						ITourbookPreferences.GRAPH_PULSE_MAX_VALUE,
						TourManager.GRAPH_PULSE,
						0,
						1e-3);

				if (isChartModified) {
					updateTourChart(keepMinMax);
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	public void addTourMarkerModifyListener(final ITourMarkerModifyListener listener) {
		_tourMarkerModifyListener.add(listener);
	}

	public void addTourMarkerSelectionListener(final ITourMarkerSelectionListener listener) {
		_tourMarkerSelectionListener.add(listener);
	}

	public void addTourModifyListener(final ITourModifyListener listener) {
		_tourModifyListener.add(listener);
	}

	public void addXAxisSelectionListener(final IXAxisSelectionListener listener) {
		_xAxisSelectionListener.add(listener);
	}

	/**
	 * Close all opened dialogs except the opening dialog.
	 * 
	 * @param openingDialog
	 */
	public void closeOpenedDialogs(final IOpeningDialog openingDialog) {
		_openDlgMgr.closeOpenedDialogs(openingDialog);
	}

	/**
	 * Create all tour chart actions.
	 */
	private void createActions() {

		// create actions only once
		if (_allTourChartActions != null) {
			return;
		}

		_allTourChartActions = new HashMap<String, Action>();

		/*
		 * graph actions
		 */
		createActions_10_GraphActions();

		/*
		 * other actions
		 */
		_actionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
		_actionTourInfo = new ActionTourChartInfo(this, _parent);
		_actionTourMarker = new ActionTourChartMarker(this, _parent);

		_allTourChartActions.put(ACTION_ID_CAN_AUTO_ZOOM_TO_SLIDER, new ActionCanAutoZoomToSlider(this));
		_allTourChartActions.put(ACTION_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED, new ActionCanMoveSlidersWhenZoomed(this));
		_allTourChartActions.put(ACTION_ID_IS_SHOW_BREAKTIME_VALUES, new ActionShowBreaktimeValues(this));
		_allTourChartActions.put(ACTION_ID_IS_SHOW_SRTM_DATA, new ActionShowSRTMData(this));
		_allTourChartActions.put(ACTION_ID_IS_SHOW_START_TIME, new ActionShowStartTime(this));
		_allTourChartActions.put(ACTION_ID_IS_SHOW_TOUR_PHOTOS, new ActionTourPhotos(this));
		_allTourChartActions.put(ACTION_ID_IS_GRAPH_OVERLAPPED, new ActionGraphOverlapped(this));
		_allTourChartActions.put(ACTION_ID_IS_SHOW_VALUEPOINT_TOOLTIP, new ActionShowValuePointToolTip(this));
		_allTourChartActions.put(ACTION_ID_X_AXIS_DISTANCE, new ActionXAxisDistance(this));
		_allTourChartActions.put(ACTION_ID_X_AXIS_TIME, new ActionXAxisTime(this));

		_allTourChartActions.put(ACTION_ID_EDIT_CHART_PREFERENCES, new ActionOpenPrefDialog(
				Messages.Tour_Action_EditChartPreferences,
				PrefPageAppearanceTourChart.ID));

		/*
		 * hr zone actions
		 */
		_allTourChartActions.put(ACTION_ID_HR_ZONE_DROPDOWN_MENU, new ActionHrZoneDropDownMenu(this));

		_allTourChartActions.put(ACTION_ID_HR_ZONE_STYLE_GRAPH_TOP, new ActionHrZoneGraphType(
				this,
				ACTION_ID_HR_ZONE_STYLE_GRAPH_TOP,
				Messages.Tour_Action_HrZoneGraphType_Default));

		_allTourChartActions.put(ACTION_ID_HR_ZONE_STYLE_NO_GRADIENT, new ActionHrZoneGraphType(
				this,
				ACTION_ID_HR_ZONE_STYLE_NO_GRADIENT,
				Messages.Tour_Action_HrZoneGraphType_NoGradient));

		_allTourChartActions.put(ACTION_ID_HR_ZONE_STYLE_WHITE_TOP, new ActionHrZoneGraphType(
				this,
				ACTION_ID_HR_ZONE_STYLE_WHITE_TOP,
				Messages.Tour_Action_HrZoneGraphType_WhiteTop));

		_allTourChartActions.put(ACTION_ID_HR_ZONE_STYLE_WHITE_BOTTOM, new ActionHrZoneGraphType(
				this,
				ACTION_ID_HR_ZONE_STYLE_WHITE_BOTTOM,
				Messages.Tour_Action_HrZoneGraphType_WhiteBottom));

		/*
		 * This is a special case because the quick edit action in the tour info tooltip can not yet
		 * been initialized and creates a NPE when this action is run, therfore a hidden action is
		 * used.
		 */
		_actionEditQuick = new ActionEditQuick(this);
	}

	/**
	 * Create all graph actions
	 */
	private void createActions_10_GraphActions() {

		createActions_12_GraphAction(
				TourManager.GRAPH_ALTITUDE,
				GRAPH_LABEL_ALTITUDE,
				Messages.Tour_Action_graph_altitude_tooltip,
				Messages.Image__graph_altitude,
				Messages.Image__graph_altitude_disabled);

		createActions_12_GraphAction(
				TourManager.GRAPH_SPEED,
				GRAPH_LABEL_SPEED,
				Messages.Tour_Action_graph_speed_tooltip,
				Messages.Image__graph_speed,
				Messages.Image__graph_speed_disabled);

		createActions_12_GraphAction(
				TourManager.GRAPH_PACE,
				GRAPH_LABEL_PACE,
				Messages.Tour_Action_graph_pace_tooltip,
				Messages.Image__graph_pace,
				Messages.Image__graph_pace_disabled);

		createActions_12_GraphAction(
				TourManager.GRAPH_POWER,
				GRAPH_LABEL_POWER,
				Messages.Tour_Action_graph_power_tooltip,
				Messages.Image__graph_power,
				Messages.Image__graph_power_disabled);

		createActions_12_GraphAction(
				TourManager.GRAPH_ALTIMETER,
				GRAPH_LABEL_ALTIMETER,
				Messages.Tour_Action_graph_altimeter_tooltip,
				Messages.Image__graph_altimeter,
				Messages.Image__graph_altimeter_disabled);

		createActions_12_GraphAction(
				TourManager.GRAPH_PULSE,
				GRAPH_LABEL_HEARTBEAT,
				Messages.Tour_Action_graph_heartbeat_tooltip,
				Messages.Image__graph_heartbeat,
				Messages.Image__graph_heartbeat_disabled);

		createActions_12_GraphAction(
				TourManager.GRAPH_TEMPERATURE,
				GRAPH_LABEL_TEMPERATURE,
				Messages.Tour_Action_graph_temperature_tooltip,
				Messages.Image__graph_temperature,
				Messages.Image__graph_temperature_disabled);

		createActions_12_GraphAction(
				TourManager.GRAPH_CADENCE,
				GRAPH_LABEL_CADENCE,
				Messages.Tour_Action_graph_cadence_tooltip,
				Messages.Image__graph_cadence,
				Messages.Image__graph_cadence_disabled);

		createActions_12_GraphAction(
				TourManager.GRAPH_GEARS,
				GRAPH_LABEL_GEARS,
				Messages.Tour_Action_GraphGears,
				Messages.Image__Graph_Gears,
				Messages.Image__Graph_Gears_disabled);

		createActions_12_GraphAction(
				TourManager.GRAPH_GRADIENT,
				GRAPH_LABEL_GRADIENT,
				Messages.Tour_Action_graph_gradient_tooltip,
				Messages.Image__graph_gradient,
				Messages.Image__graph_gradient_disabled);

		createActions_12_GraphAction(
				TourManager.GRAPH_TOUR_COMPARE,
				GRAPH_LABEL_TOUR_COMPARE,
				Messages.Tour_Action_graph_tour_compare_tooltip,
				Messages.Image__graph_tour_compare,
				Messages.Image__graph_tour_compare_disabled);
	}

	/**
	 * Create a graph action
	 * 
	 * @param graphId
	 * @param label
	 * @param toolTip
	 * @param imageEnabled
	 * @param imageDisabled
	 */
	private void createActions_12_GraphAction(	final int graphId,
												final String label,
												final String toolTip,
												final String imageEnabled,
												final String imageDisabled) {

		final ActionGraph graphAction = new ActionGraph(this, graphId, label, toolTip, imageEnabled, imageDisabled);

		_allTourChartActions.put(getGraphActionId(graphId), graphAction);
	}

	/**
	 * Creates the handlers for the tour chart actions
	 * 
	 * @param workbenchWindow
	 * @param tourChartConfig
	 */
	public void createActions_TourEditor(final TourChartConfiguration tourChartConfig) {

		_tcc = tourChartConfig;

		createActions();
		createChartActions();
	}

	/**
	 * Create chart photos, these are photos which contain the time slice position.
	 * 
	 * @param srcPhotos
	 * @param chartPhotos
	 * @param isPhotoSavedInTour
	 */
	private void createChartPhotos(	final ArrayList<Photo> srcPhotos,
									final ArrayList<ChartPhoto> chartPhotos,
									final boolean isPhotoSavedInTour) {

		final int[] timeSerie = _tourData.timeSerie;
		final long[] historySerie = _tourData.timeSerieHistory;

		final boolean isTimeSerie = timeSerie != null;

		final boolean isMultipleTours = _tourData.isMultipleTours;
		final int[] multipleStartTimeIndex = _tourData.multipleTourStartIndex;
		final long[] multipleStartTime = _tourData.multipleTourStartTime;

		final long tourStart = _tourData.getTourStartTime().getMillis() / 1000;
		final int numberOfTimeSlices = isTimeSerie ? timeSerie.length : historySerie.length;

		/*
		 * set photos for tours which has max 1 value point
		 */
		if (numberOfTimeSlices <= 1) {
			for (final Photo photo : srcPhotos) {
				chartPhotos.add(new ChartPhoto(photo, 0, 0));
			}
			return;
		}

		/*
		 * set photos for tours which has more than 1 value point
		 */
		final int numberOfPhotos = srcPhotos.size();

		// set value serie for the x-axis, it can be time or distance
		double[] xAxisSerie = null;
		xAxisSerie = _tcc.isShowTimeOnXAxis //
				? _tourData.getTimeSerieWithTimeZoneAdjusted()
				: _tourData.getDistanceSerieDouble();

		long timeSliceEnd;
		if (isTimeSerie) {
			timeSliceEnd = tourStart + (long) (timeSerie[1] / 2.0);
		} else {
			// is history
			timeSliceEnd = tourStart + (long) (historySerie[1] / 2.0);
		}

		int photoIndex = 0;
		Photo photo = srcPhotos.get(photoIndex);

		// tour time serie, fit photos into a tour

		int serieIndex = 0;

		int nextTourIndex = 1;
		int nextTourSerieIndex = 0;
		long firstTourStartTime = 0;
		long tourStartTime = 0;
		long tourRecordingTime = 0;

		// setup first multiple tour
		if (isMultipleTours) {

			firstTourStartTime = multipleStartTime[0];
			// this is the first tour

			tourStartTime = firstTourStartTime;
			nextTourSerieIndex = multipleStartTimeIndex[nextTourIndex];
		}

		// loop serieIndex
		while (true) {

			// check if a photo is in the current time slice
			while (true) {

				final long imageAdjustedTime = isPhotoSavedInTour //
						? photo.adjustedTimeTour
						: photo.adjustedTimeLink;

				long imageTime = 0;

				if (imageAdjustedTime != Long.MIN_VALUE) {
					imageTime = imageAdjustedTime;
				} else {
					imageTime = photo.imageExifTime;
				}

				if (isMultipleTours) {

					// adjust image time because multiple tours do not have a gap between tours
					// it took me several hours to find this algorithm, but now it works :-)

					if (serieIndex >= nextTourSerieIndex) {

						// setup next tour

						final int tourDuration = timeSerie[nextTourSerieIndex - 1];
						tourRecordingTime = tourDuration;

						tourStartTime = multipleStartTime[nextTourIndex];

						if (nextTourIndex < multipleStartTimeIndex.length - 1) {

							nextTourIndex++;
							nextTourSerieIndex = multipleStartTimeIndex[nextTourIndex];
						}
					}

					final long tourTimeOffset = tourStartTime - firstTourStartTime;
					final long xAxisOffset = tourRecordingTime * 1000;

					final long timeOffset = tourTimeOffset - xAxisOffset;

					imageTime -= timeOffset;
				}

				final long photoTime = imageTime / 1000;

				if (photoTime <= timeSliceEnd) {

					// photo is available in the current time slice

					final double xValue = xAxisSerie[serieIndex];

					chartPhotos.add(new ChartPhoto(photo, xValue, serieIndex));

					photoIndex++;

				} else {

					// advance to the next time slice

					break;
				}

				if (photoIndex < numberOfPhotos) {
					photo = srcPhotos.get(photoIndex);
				} else {
					break;
				}
			}

			if (photoIndex >= numberOfPhotos) {
				// no more photos
				break;
			}

			/*
			 * photos are still available
			 */

			// advance to the next time slice on the x-axis
			serieIndex++;

			if (serieIndex >= numberOfTimeSlices - 1) {

				/*
				 * end of tour is reached but there are still photos available, set remaining photos
				 * at the end of the tour
				 */

				while (true) {

					final double xValue = xAxisSerie[numberOfTimeSlices - 1];
					chartPhotos.add(new ChartPhoto(photo, xValue, serieIndex));

					photoIndex++;

					if (photoIndex < numberOfPhotos) {
						photo = srcPhotos.get(photoIndex);
					} else {
						break;
					}
				}

			} else {

				// calculate next time slice

				long valuePointTime;
				long sliceDuration;

				if (isTimeSerie) {
					valuePointTime = timeSerie[serieIndex];
					sliceDuration = timeSerie[serieIndex + 1] - valuePointTime;
				} else {
					// is history
					valuePointTime = historySerie[serieIndex];
					sliceDuration = historySerie[serieIndex + 1] - valuePointTime;
				}

				timeSliceEnd = tourStart + valuePointTime + (sliceDuration / 2);
			}
		}
	}

	private void createLayer_2ndAlti() {

		if (_is2ndAltiLayerVisible && (_layer2ndAlti != null)) {
			_layer2ndAltiSerie = _layer2ndAlti.create2ndAltiLayer();
		} else {
			_layer2ndAltiSerie = null;
		}
	}

	/**
	 * create the layer which displays the tour marker
	 * 
	 * @param isForcedMarker
	 *            When <code>true</code> the marker must be drawn, otherwise
	 *            {@link TourChartConfiguration#isShowTourMarker} determines if the markers are
	 *            drawn or not.
	 *            <p>
	 *            Marker must be drawn in the marker dialog.
	 */
	private void createLayer_Marker(final boolean isForcedMarker) {

		if (isForcedMarker == false && _tcc.isShowTourMarker == false) {

			// marker layer is not displayed

			hideMarkerLayer();

			return;
		}

		// marker layer is visible

		final ChartMarkerConfig cmc = new ChartMarkerConfig();

		cmc.isDrawMarkerWithDefaultColor = _tcc.isDrawMarkerWithDefaultColor;
		cmc.isShowAbsoluteValues = _tcc.isShowAbsoluteValues;
		cmc.isShowHiddenMarker = _tcc.isShowHiddenMarker;
		cmc.isShowMarkerLabel = _tcc.isShowMarkerLabel;
		cmc.isShowMarkerTooltip = _tcc.isShowMarkerTooltip;
		cmc.isShowMarkerPoint = _tcc.isShowMarkerPoint;
		cmc.isShowOnlyWithDescription = _tcc.isShowOnlyWithDescription;
		cmc.isShowSignImage = _tcc.isShowSignImage;
		cmc.isShowLabelTempPos = _tcc.isShowLabelTempPos;

		cmc.markerLabelTempPos = _tcc.markerLabelTempPos;
		cmc.markerTooltipPosition = _tcc.markerTooltipPosition;

		cmc.markerHoverSize = _tcc.markerHoverSize;
		cmc.markerLabelOffset = _tcc.markerLabelOffset;
		cmc.markerPointSize = _tcc.markerPointSize;
		cmc.markerSignImageSize = _tcc.markerSignImageSize;

		cmc.markerColorDefault = _tcc.markerColorDefault;
		cmc.markerColorDevice = _tcc.markerColorDevice;
		cmc.markerColorHidden = _tcc.markerColorHidden;

		if (_layerMarker == null) {

			// setup marker layer, a layer is created only once

			_layerMarker = new ChartLayerMarker(this);

			// set overlay painter
			addChartOverlay(_layerMarker);

			addMouseChartListener(_mouseMarkerListener);
		}

		_layerMarker.setChartMarkerConfig(cmc);
		_tourMarkerTooltip.setChartMarkerConfig(cmc);

		// set data serie for the x-axis
		final double[] xAxisSerie = _tcc.isShowTimeOnXAxis ? //
				_tourData.getTimeSerieDouble()
				: _tourData.getDistanceSerieDouble();

		if (_tourData.isMultipleTours) {

			final int[] multipleStartTimeIndex = _tourData.multipleTourStartIndex;
			final int[] multipleNumberOfMarkers = _tourData.multipleNumberOfMarkers;

			int tourIndex = 0;
			int numberOfMultiMarkers = 0;
			int tourSerieIndex = 0;

			// setup first multiple tour
			tourSerieIndex = multipleStartTimeIndex[tourIndex];
			numberOfMultiMarkers = multipleNumberOfMarkers[tourIndex];

			final ArrayList<TourMarker> allTourMarkers = _tourData.multiTourMarkers;

			for (int markerIndex = 0; markerIndex < allTourMarkers.size(); markerIndex++) {

				while (markerIndex >= numberOfMultiMarkers) {

					// setup next tour

					tourIndex++;

					if (tourIndex <= multipleStartTimeIndex.length - 1) {

						tourSerieIndex = multipleStartTimeIndex[tourIndex];
						numberOfMultiMarkers += multipleNumberOfMarkers[tourIndex];
					}
				}

				final TourMarker tourMarker = allTourMarkers.get(markerIndex);
				final int xAxisSerieIndex = tourSerieIndex + tourMarker.getSerieIndex();

				tourMarker.setMultiTourSerieIndex(xAxisSerieIndex);

				final ChartLabel chartLabel = createLayer_Marker_ChartLabel(//
						tourMarker,
						xAxisSerie,
						xAxisSerieIndex,
						tourMarker.getLabelPosition());

				cmc.chartLabels.add(chartLabel);
			}

		} else {

			for (final TourMarker tourMarker : _tourData.getTourMarkers()) {

				final ChartLabel chartLabel = createLayer_Marker_ChartLabel(
						tourMarker,
						xAxisSerie,
						tourMarker.getSerieIndex(),
						tourMarker.getLabelPosition());

				cmc.chartLabels.add(chartLabel);
			}
		}
	}

	/**
	 * @param tourMarker
	 * @param xAxisSerie
	 * @param xAxisSerieIndex
	 * @param labelPosition
	 * @return
	 */
	private ChartLabel createLayer_Marker_ChartLabel(	final TourMarker tourMarker,
														final double[] xAxisSerie,
														final int xAxisSerieIndex,
														final int labelPosition) {

		final ChartLabel chartLabel = new ChartLabel();

		chartLabel.data = tourMarker;

		// create marker label
		String markerLabel = tourMarker.getLabel();
		final boolean isDescription = tourMarker.getDescription().length() > 0;
		final boolean isUrlAddress = tourMarker.getUrlAddress().length() > 0;
		final boolean isUrlText = tourMarker.getUrlText().length() > 0;
		if (isDescription | isUrlAddress | isUrlText) {
			markerLabel += UI.SPACE2 + UI.SYMBOL_FOOT_NOTE;
		}

		chartLabel.graphX = xAxisSerie[xAxisSerieIndex];
		chartLabel.serieIndex = xAxisSerieIndex;

		chartLabel.markerLabel = markerLabel;
		chartLabel.isDescription = isDescription;
		chartLabel.visualPosition = labelPosition;
		chartLabel.type = tourMarker.getType();
		chartLabel.visualType = tourMarker.getVisibleType();

		chartLabel.labelXOffset = tourMarker.getLabelXOffset();
		chartLabel.labelYOffset = tourMarker.getLabelYOffset();

		chartLabel.isVisible = tourMarker.isMarkerVisible();

//		final TourSign tourSign = tourMarker.getTourSign();
//		if (tourSign != null) {
//			chartLabel.markerSignPhoto = tourSign.getSignImagePhoto();
//		}

		return chartLabel;
	}

	private void createLayer_Photo() {

		while (true) {

			if (_tcc.isShowTourPhotos == false) {
				break;
			}

			final int[] timeSerie = _tourData.timeSerie;
			final long[] historySerie = _tourData.timeSerieHistory;

			final boolean isTimeSerie = timeSerie != null;
			final boolean isHistorySerie = historySerie != null;

			if (isTimeSerie == false && isHistorySerie == false) {
				// this is a manually created tour
				break;
			}

			final ArrayList<PhotoCategory> chartPhotoGroups = new ArrayList<PhotoCategory>();

			/*
			 * get saved photos
			 */
			final ArrayList<Photo> srcTourPhotos = _tourData.getGalleryPhotos();
			if (srcTourPhotos != null && srcTourPhotos.size() > 0) {

				final ArrayList<ChartPhoto> chartPhotos = new ArrayList<ChartPhoto>();
				createChartPhotos(srcTourPhotos, chartPhotos, true);

				final PhotoCategory chartPhotoGroup = new PhotoCategory(chartPhotos, ChartPhotoType.TOUR);
				chartPhotoGroups.add(chartPhotoGroup);
			}

			/*
			 * get link photos, they are painted below saved photos that the mouse hit area is
			 * larger
			 */
			final TourPhotoLink tourPhotoLink = _tourData.tourPhotoLink;
			if (tourPhotoLink != null) {

				final ArrayList<Photo> srcLinkPhotos = tourPhotoLink.linkPhotos;

				if (srcLinkPhotos.size() > 0) {

					final ArrayList<ChartPhoto> chartPhotos = new ArrayList<ChartPhoto>();
					createChartPhotos(srcLinkPhotos, chartPhotos, false);

					final PhotoCategory chartPhotoGroup = new PhotoCategory(chartPhotos, ChartPhotoType.LINK);
					chartPhotoGroups.add(chartPhotoGroup);
				}
			}

			if (chartPhotoGroups.size() == 0) {
				// there are no photos
				break;
			}

			/*
			 * at least 1 photo is available
			 */

			/*
			 * The old photo layer can still be available. This happens because the action button is
			 * a three state button.
			 */
			if (_layerPhoto == null) {

				_layerPhoto = new ChartLayerPhoto(chartPhotoGroups);
				_layerPhoto.setBackgroundColor(_photoOverlayBGColorLink, _photoOverlayBGColorTour);

				// set overlay painter
				addChartOverlay(_layerPhoto);

				addMouseChartListener(_mousePhotoListener);
			}

			if (isTimeSerie == false && isHistorySerie) {
				// hide x slider in history chart
				setShowSlider(false);
			} else {
				// ensure sliders are displayed for real tours
				setShowSlider(true);
			}

			return;
		}

		hidePhotoLayer();
	}

	/**
	 * Creates the layers from the segmented tour data
	 */
	private void createLayer_TourSegmenter() {

		setupTourSegmenter();

		if (_isTourSegmenterVisible == false) {
			return;
		}

		if (_tourData == null) {
			return;
		}

		final int[] segmentSerieIndex = _tourData.segmentSerieIndex;

		if (segmentSerieIndex == null) {
			// no segmented tour data available or segments are invisible
			return;
		}

		// show hidden values
		final boolean isHideSmallValues = Util.getStateBoolean(
				_tourSegmenterState,
				TourSegmenterView.STATE_IS_HIDE_SMALL_VALUES,
				TourSegmenterView.STATE_IS_HIDE_SMALL_VALUES_DEFAULT);
		final int smallValueSize = Util.getStateInt(
				_tourSegmenterState,
				TourSegmenterView.STATE_SMALL_VALUE_SIZE,
				TourSegmenterView.STATE_SMALL_VALUE_SIZE_DEFAULT);

		// show segment lines
		final boolean isShowSegmenterLine = Util.getStateBoolean(
				_tourSegmenterState,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_LINE,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_LINE_DEFAULT);
		final int lineOpacity = Util.getStateInt(_tourSegmenterState,//
				TourSegmenterView.STATE_LINE_OPACITY,
				TourSegmenterView.STATE_LINE_OPACITY_DEFAULT);

		final boolean isShowSegmenterMarker = Util.getStateBoolean(
				_tourSegmenterState,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_MARKER,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_MARKER_DEFAULT);

		final boolean isShowSegmenterValue = Util.getStateBoolean(
				_tourSegmenterState,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUE,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUE_DEFAULT);

		final boolean isShowDecimalPlaces = Util.getStateBoolean(
				_tourSegmenterState,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_DECIMAL_PLACES,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_DECIMAL_PLACES_DEFAULT);

		final int stackedValues = Util.getStateInt(
				_tourSegmenterState,
				TourSegmenterView.STATE_STACKED_VISIBLE_VALUES,
				TourSegmenterView.STATE_STACKED_VISIBLE_VALUES_DEFAULT);

		final int graphOpacity = Util.getStateInt(_tourSegmenterState,//
				TourSegmenterView.STATE_GRAPH_OPACITY,
				TourSegmenterView.STATE_GRAPH_OPACITY_DEFAULT);

		final double[] xDataSerie = _tcc.isShowTimeOnXAxis ? //
				_tourData.getTimeSerieDouble()
				: _tourData.getDistanceSerieDouble();

		/*
		 * Create/update altitude layer
		 */
		if (_layerTourSegmenterAltitude == null) {

			_layerTourSegmenterAltitude = new ChartLayerSegmentAltitude(this);

			// set overlay painter
			addChartOverlay(_layerTourSegmenterAltitude);
		}

		_layerTourSegmenterAltitude.setTourData(_tourData);
		_layerTourSegmenterAltitude.setIsShowDecimalPlaces(isShowDecimalPlaces);
		_layerTourSegmenterAltitude.setIsShowSegmenterMarker(isShowSegmenterMarker);
		_layerTourSegmenterAltitude.setIsShowSegmenterValue(isShowSegmenterValue);
		_layerTourSegmenterAltitude.setLineProperties(isShowSegmenterLine, lineOpacity);
		_layerTourSegmenterAltitude.setSmallHiddenValuesProperties(isHideSmallValues, smallValueSize);
		_layerTourSegmenterAltitude.setStackedValues(stackedValues);

		final int segmentIndexSize = segmentSerieIndex.length;

		for (int segmentIndex = 0; segmentIndex < segmentIndexSize; segmentIndex++) {

			final int serieIndex = segmentSerieIndex[segmentIndex];
			final ChartLabel chartLabel = new ChartLabel();

			chartLabel.graphX = xDataSerie[serieIndex];
			chartLabel.serieIndex = serieIndex;
			chartLabel.segmentIndex = segmentIndex;

			/*
			 * Set slider positions
			 */
			final int prevSegmentIndex = segmentIndex - 1;
			final int leftIndex = prevSegmentIndex < 0
					? SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
					: segmentSerieIndex[prevSegmentIndex];

			chartLabel.xSliderSerieIndexLeft = leftIndex;
			chartLabel.xSliderSerieIndexRight = serieIndex;

			_layerTourSegmenterAltitude.addMarker(chartLabel);
		}

		/*
		 * Create/update value layer
		 */
		if (_layerTourSegmenterOther == null) {

			_layerTourSegmenterOther = new ChartLayerSegmentValue(this);
		}

		_layerTourSegmenterOther.setTourData(_tourData);
		_layerTourSegmenterOther.setIsShowDecimalPlaces(isShowDecimalPlaces);
		_layerTourSegmenterOther.setIsShowSegmenterValues(isShowSegmenterValue);
		_layerTourSegmenterOther.setLineProperties(isShowSegmenterLine, lineOpacity);
		_layerTourSegmenterOther.setSmallHiddenValuesProperties(isHideSmallValues, smallValueSize);
		_layerTourSegmenterOther.setStackedValues(stackedValues);
		_layerTourSegmenterOther.setXDataSerie(xDataSerie);

		// draw the graph lighter that the segments are more visible
		setGraphAlpha(graphOpacity / 100.0);
	}

	private void createPainter_HrZone() {

		if (_tcc.isHrZoneDisplayed) {
			_hrZonePainter = new HrZonePainter();
		} else {
			_hrZonePainter = null;
		}
	}

	@Override
	public void deleteTourMarker(final TourMarker tourMarker) {

		if (_isDisplayedInDialog) {

			/*
			 * Do not confirm deletion when tour chart is displayed in a dialog, because the dialog
			 * and the removal can be canceled.
			 */

			// this will update the chart
			fireTourMarkerModifyEvent(tourMarker, true);

		} else {

			// confirm to delete the marker
			if (MessageDialog.openQuestion(
					getShell(),
					Messages.Dlg_TourMarker_MsgBox_delete_marker_title,
					NLS.bind(Messages.Dlg_TourMarker_MsgBox_delete_marker_message, (tourMarker).getLabel()))) {

				// remove tourmarker from the model
				final boolean isRemoved = _tourData.getTourMarkers().remove(tourMarker);
				Assert.isTrue(isRemoved);

				// tour will be saved and the chart will also be updated
				fireTourModifyEvent_Globally();
			}
		}
	}

	public void enableGraphAction(final int graphId, final boolean isEnabled) {

		if (_allTourChartActions == null) {
			return;
		}

		final Action action = _allTourChartActions.get(getGraphActionId(graphId));
		if (action != null) {
			action.setEnabled(isEnabled);
		}

	}

	private void enableZoomOptions() {

		if (_allTourChartActions == null) {
			return;
		}

		final boolean canAutoZoom = getMouseMode().equals(Chart.MOUSE_MODE_ZOOM);

		final Action action = _allTourChartActions.get(ACTION_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED);
		if (action != null) {
			action.setEnabled(canAutoZoom);
		}
	}

	/**
	 * create the tour specific action bar, they are defined in the chart configuration
	 */
	private void fillToolbar() {

		// check if toolbar is created
		if (_actionTourOptions != null) {
			return;
		}

		final IToolBarManager tbm = getToolBarManager();

		_actionTourOptions = new ActionTourChartOptions(this);

		/*
		 * add the actions to the toolbar
		 */
		if (_tcc.canShowTourCompareGraph) {
			tbm.add(_allTourChartActions.get(getGraphActionId(TourManager.GRAPH_TOUR_COMPARE)));
		}

		tbm.add(new Separator());
		tbm.add(_allTourChartActions.get(getGraphActionId(TourManager.GRAPH_ALTITUDE)));
		tbm.add(_allTourChartActions.get(getGraphActionId(TourManager.GRAPH_PULSE)));
		tbm.add(_allTourChartActions.get(getGraphActionId(TourManager.GRAPH_SPEED)));
		tbm.add(_allTourChartActions.get(getGraphActionId(TourManager.GRAPH_PACE)));
		tbm.add(_allTourChartActions.get(getGraphActionId(TourManager.GRAPH_POWER)));
		tbm.add(_allTourChartActions.get(getGraphActionId(TourManager.GRAPH_TEMPERATURE)));
		tbm.add(_allTourChartActions.get(getGraphActionId(TourManager.GRAPH_GRADIENT)));
		tbm.add(_allTourChartActions.get(getGraphActionId(TourManager.GRAPH_ALTIMETER)));
		tbm.add(_allTourChartActions.get(getGraphActionId(TourManager.GRAPH_CADENCE)));
		tbm.add(_allTourChartActions.get(getGraphActionId(TourManager.GRAPH_GEARS)));

		tbm.add(new Separator());
		tbm.add(_allTourChartActions.get(ACTION_ID_IS_GRAPH_OVERLAPPED));
		tbm.add(_allTourChartActions.get(ACTION_ID_HR_ZONE_DROPDOWN_MENU));
		tbm.add(_allTourChartActions.get(ACTION_ID_IS_SHOW_TOUR_PHOTOS));
		tbm.add(_actionTourMarker);
		tbm.add(_actionTourInfo);

		tbm.add(new Separator());
		tbm.add(_allTourChartActions.get(ACTION_ID_X_AXIS_TIME));
		tbm.add(_allTourChartActions.get(ACTION_ID_X_AXIS_DISTANCE));

		tbm.add(_actionTourOptions);

		tbm.update(true);
	}

	/**
	 * Set sliders to the selected segment and fire this position.
	 * 
	 * @param selectedLabel1
	 * @param selectedLabel2
	 */
	private void fireSegmentLabelSelection(final ChartLabel selectedLabel1, final ChartLabel selectedLabel2) {

		// get start/end index depending which segments are selected
		ChartLabel startSegment = selectedLabel1;
		ChartLabel endSegment;
		if (selectedLabel2 == null) {
			endSegment = startSegment;
		} else {
			endSegment = selectedLabel2;
		}

		// depending how the segments are selected, start can be larger than the end
		if (startSegment.segmentIndex > endSegment.segmentIndex) {

			// switch segments
			final ChartLabel tempSegment = endSegment;
			endSegment = startSegment;
			startSegment = tempSegment;
		}

		final int xSliderSerieIndexLeft = startSegment.xSliderSerieIndexLeft;
		final int xSliderSerieIndexRight = endSegment.xSliderSerieIndexRight;

		final SelectionChartXSliderPosition selectionSliderPosition = new SelectionChartXSliderPosition(//
				this,
				xSliderSerieIndexLeft,
				xSliderSerieIndexRight);

		/*
		 * Extend default selection with the sement positions
		 */
		final SelectedTourSegmenterSegments selectedSegments = new SelectedTourSegmenterSegments();
		selectedSegments.tourData = _tourData;
		selectedSegments.xSliderSerieIndexLeft = xSliderSerieIndexLeft;
		selectedSegments.xSliderSerieIndexRight = xSliderSerieIndexRight;

		selectionSliderPosition.setCustomData(selectedSegments);

		/*
		 * Set x slider position in the chart but do not fire an event because the event is fired
		 * separately for each slider :-(
		 */
		setXSliderPosition(selectionSliderPosition, false);

		// fire event for both x sliders
		TourManager.fireEventWithCustomData(//
				TourEventId.SLIDER_POSITION_CHANGED,
				selectionSliderPosition,
				_part);
	}

	/**
	 * Fires an event when the a tour marker is modified.
	 * 
	 * @param tourMarker
	 * @param isTourMarkerDeleted
	 */
	private void fireTourMarkerModifyEvent(final TourMarker tourMarker, final boolean isTourMarkerDeleted) {

		final Object[] listeners = _tourMarkerModifyListener.getListeners();
		for (final Object listener2 : listeners) {

			final ITourMarkerModifyListener listener = (ITourMarkerModifyListener) listener2;
			listener.tourMarkerIsModified(tourMarker, isTourMarkerDeleted);
		}
	}

	private void fireTourMarkerSelection(final TourMarker tourMarker) {

		if (_isDisplayedInDialog) {
			return;
		}

		// update selection locally (e.g. in a dialog)

		final ArrayList<TourMarker> allTourMarker = new ArrayList<TourMarker>();
		allTourMarker.add(tourMarker);

		final SelectionTourMarker tourMarkerSelection = new SelectionTourMarker(_tourData, allTourMarker);

		final Object[] listeners = _tourMarkerSelectionListener.getListeners();
		for (final Object listener2 : listeners) {
			final ITourMarkerSelectionListener listener = (ITourMarkerSelectionListener) listener2;
			listener.selectionChanged(tourMarkerSelection);
		}

		TourManager.fireEventWithCustomData(//
				TourEventId.MARKER_SELECTION,
				tourMarkerSelection,
				_part);
	}

	/**
	 * Fires an event when a tour is modified.
	 */
	private void fireTourModifyEvent_Globally() {

		final Object[] listeners = _tourModifyListener.getListeners();
		for (final Object listener2 : listeners) {

			final ITourModifyListener listener = (ITourModifyListener) listener2;
			listener.tourIsModified(_tourData);
		}
	}

	/**
	 * Fires an event when the x-axis values were changed by the user
	 * 
	 * @param isShowTimeOnXAxis
	 */
	private void fireXAxisSelection(final boolean showTimeOnXAxis) {

		final Object[] listeners = _xAxisSelectionListener.getListeners();
		for (final Object listener2 : listeners) {
			final IXAxisSelectionListener listener = (IXAxisSelectionListener) listener2;
			listener.selectionChanged(showTimeOnXAxis);
		}
	}

	/**
	 * Converts the graph Id into an action Id
	 * 
	 * @param graphId
	 * @return
	 */
	private String getGraphActionId(final int graphId) {
		return "graphId." + Integer.toString(graphId); //$NON-NLS-1$
	}

	/**
	 * @return Returns the hovered marker or <code>null</code> when a marker is not hovered.
	 */
	private ChartLabel getHoveredMarkerLabel() {

		if (_layerMarker == null) {
			return null;
		}

		return _layerMarker.getHoveredLabel();
	}

	private ChartLabel getHoveredSegmentLabel(final ChartMouseEvent mouseEvent) {

		ChartLabel hoveredLabel = _layerTourSegmenterAltitude.getHoveredLabel(mouseEvent);

		if (hoveredLabel == null) {
			hoveredLabel = _layerTourSegmenterOther.getHoveredLabel(mouseEvent);
		}

		return hoveredLabel;
	}

	/**
	 * @param mouseEvent
	 * @return Returns the hovered title or <code>null</code> when a title is not hovered.
	 */
	private ChartSegment getHoveredSegmentTitle(final ChartMouseEvent mouseEvent) {

		final int devXMouse = mouseEvent.devXMouse;
		final int devYMouse = mouseEvent.devYMouse;

		final ChartDrawingData chartDrawingData = getChartDrawingData();
		final ArrayList<ChartSegment> tourSegments = chartDrawingData.tourSegments;

		for (final ChartSegment tourSegment : tourSegments) {

			final int devXSegment = tourSegment.devXSegment;
			final int devYLabel = tourSegment.devYTitle;

			if (devXMouse > devXSegment
					&& devXMouse < devXSegment + tourSegment.devSegmentWidth
					&& devYMouse > 0
					&& devYMouse < devYLabel + tourSegment.titleHeight) {

				return tourSegment;
			}
		}

		return null;
	}

	/**
	 * @return Returns a {@link TourMarker} when a {@link ChartLabel} (marker) is hovered or
	 *         <code>null</code> when a {@link ChartLabel} is not hovered.
	 */
	public TourMarker getHoveredTourMarker() {

		TourMarker tourMarker = null;

		final ChartLabel hoveredMarkerLabel = getHoveredMarkerLabel();

		if (hoveredMarkerLabel != null) {
			if (hoveredMarkerLabel.data instanceof TourMarker) {
				tourMarker = (TourMarker) hoveredMarkerLabel.data;
			}
		}
		return tourMarker;
	}

	ChartLayerMarker getLayerTourMarker() {

		return _layerMarker;
	}

	ChartLabel getSegmentLabel_Hovered() {
		return _hoveredSegmentLabel;
	}

	ChartLabel getSegmentLabel_Selected_1() {
		return _selectedSegmentLabel_1;
	}

	ChartLabel getSegmentLabel_Selected_2() {
		return _selectedSegmentLabel_2;
	}

	TourMarker getSelectedTourMarker() {
		return _selectedTourMarker;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> tourChartTours = new ArrayList<TourData>();

		if (_tourData != null) {

			TourData tourData;

			if (_tourData.isMultipleTours) {
				tourData = TourManager.getTour(_hoveredSegmentTitle.getTourId());
			} else {
				tourData = _tourData;
			}

			if (tourData != null) {
				tourChartTours.add(tourData);
			}
		}

		return tourChartTours;
	}

	public Map<String, Action> getTourChartActions() {
		return _allTourChartActions;
	}

	public TourChartConfiguration getTourChartConfig() {
		return _tcc;
	}

	public TourData getTourData() {
		return _tourData;
	}

	Font getValueFont() {

		if (_segmenterValueFont == null) {
			setupSegmenterValueFont();
		}

		return _segmenterValueFont;
	}

	/**
	 * Disable marker layer.
	 */
	private void hideMarkerLayer() {

		if (_layerMarker != null) {

			removeChartOverlay(_layerMarker);

			_layerMarker = null;
		}

		removeMouseChartListener(_mouseMarkerListener);
	}

	void hideMarkerTooltip() {

		// disable selection
		_selectedTourMarker = null;

		_tourMarkerTooltip.hideNow();
	}

	private void hidePhotoLayer() {

		if (_layerPhoto != null) {

			removeChartOverlay(_layerPhoto);

			_layerPhoto = null;
		}

		removeMouseChartListener(_mousePhotoListener);
	}

	private void onDispose() {

		if (_segmenterValueFont != null) {
			_segmenterValueFont.dispose();
		}

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		_photoOverlayBGColorLink.dispose();
		_photoOverlayBGColorTour.dispose();

		_valuePointTooltip.hide();
	}

	private void onMarker_ChartResized() {

		// hide tooltip otherwise it has the wrong location
		// disable selection
		_selectedTourMarker = null;

		// ensure that a marker do not keeps hovered state when chart is zoomed
		_layerMarker.resetHoveredState();

		_tourMarkerTooltip.hideNow();
	}

	private void onMarker_MouseDoubleClick(final ChartMouseEvent event) {

		final TourMarker tourMarker = getHoveredTourMarker();
		if (tourMarker != null) {

			// notify the chart mouse listener that no other actions should be done
			event.isWorked = true;

			// prevent that this action will open another marker dialog
			if (_isDisplayedInDialog == false) {

				// open marker dialog with the hovered/selected tour marker
				_actionOpenMarkerDialog.setTourMarker(tourMarker);
				_actionOpenMarkerDialog.run();
			}
		}
	}

	private void onMarker_MouseDown(final ChartMouseEvent mouseEvent) {

		final TourMarker tourMarker = getHoveredTourMarker();

		if (tourMarker != null) {

			// notify the chart mouse listener that no other actions should be done
			mouseEvent.isWorked = true;

			_selectedTourMarker = tourMarker;

			fireTourMarkerSelection(tourMarker);

			_firedTourMarker = tourMarker;

			// redraw chart
			setChartOverlayDirty();
		}
	}

	private void onMarker_MouseExit() {

		// mouse has exited the chart, reset hovered label

		if (_layerMarker == null) {
			return;
		}

		// disable selection
		_selectedTourMarker = null;

		_layerMarker.resetHoveredState();

		// redraw chart
		setChartOverlayDirty();
	}

	private void onMarker_MouseMove(final ChartMouseEvent mouseEvent) {

		if (_layerMarker == null) {
			return;
		}

		final ChartLabel hoveredLabel = _layerMarker.retrieveHoveredLabel(mouseEvent);

		final boolean isLabelHovered = hoveredLabel != null;
		if (isLabelHovered) {

			// set worked that no other actions are done in this event
			mouseEvent.isWorked = isLabelHovered;
			mouseEvent.cursor = ChartCursor.Arrow;
		}

		// check if the selected marker is hovered
		final TourMarker hoveredMarker = getHoveredTourMarker();
		if (_selectedTourMarker != null && hoveredLabel == null || (hoveredMarker != _selectedTourMarker)) {

			_selectedTourMarker = null;

			// redraw chart
			setChartOverlayDirty();
		}

		// ensure that a selected tour marker is drawn in the overlay
		if (_selectedTourMarker != null) {

			// redraw chart
			setChartOverlayDirty();
		}

		if (_tcc.isShowMarkerTooltip) {

			// marker tooltip is displayed

			_tourMarkerTooltip.open(hoveredLabel);
		}
	}

	private void onMarker_MouseUp(final ChartMouseEvent mouseEvent) {

		if (_layerMarker == null) {
			return;
		}

		final ChartLabel hoveredLabel = _layerMarker.retrieveHoveredLabel(mouseEvent);

		final boolean isLabelHovered = hoveredLabel != null;
		if (isLabelHovered) {

			// set marker default cursor when the mouse is still hovering a marker
			mouseEvent.isWorked = isLabelHovered;
			mouseEvent.cursor = ChartCursor.Arrow;

			/*
			 * Fire tourmarker selection
			 */
			final TourMarker hoveredTourMarker = getHoveredTourMarker();

			// ensure that the tour marker selection is fired only once
			if (_firedTourMarker == null || _firedTourMarker != hoveredTourMarker) {

				// select the tour marker when the context menu is opened
				fireTourMarkerSelection(hoveredTourMarker);
			}
			_firedTourMarker = null;
		}
	}

	private void onPhoto_ChartResized() {

		// reset hovered state otherwise hovered photo marker are still displayed when chart is zoomed in
		onPhoto_MouseExit();
	}

	private void onPhoto_MouseExit() {

		// mouse has exited the chart, reset hovered marker

		if (_layerPhoto == null) {
			return;
		}

		_layerPhoto.setHoveredData(null, null);

		// redraw chart overlay
		setChartOverlayDirty();
	}

	private void onPhoto_MouseMove(final ChartMouseEvent mouseEvent) {

		if (_layerPhoto == null) {
			return;
		}

		// check if photos are hovered
		final PhotoCategory hoveredPhotoCategory = _layerPhoto.getHoveredPhotoCategory(
				mouseEvent.eventTime,
				mouseEvent.devXMouse,
				mouseEvent.devYMouse);

		final PhotoPaintGroup hoveredPhotoGroup = _layerPhoto.getHoveredPaintGroup();

		_layerPhoto.setHoveredData(hoveredPhotoCategory, hoveredPhotoGroup);

		final boolean isHovered = hoveredPhotoGroup != null;
		if (isHovered) {
			mouseEvent.isWorked = isHovered;
		}
	}

	/**
	 * Fire selection for the selected segment label.
	 * 
	 * @param mouseEvent
	 */
	private void onSegmentLabel_MouseDown(final ChartMouseEvent mouseEvent) {

		final ChartLabel hoveredLabel = getHoveredSegmentLabel(mouseEvent);

		if (hoveredLabel == null) {

			_selectedSegmentLabel_1 = null;
			_selectedSegmentLabel_2 = null;

			// redraw chart
			setChartOverlayDirty();

			return;
		}

		// notify the chart mouse listener that no other actions should be done
		mouseEvent.isWorked = true;
		mouseEvent.cursor = ChartCursor.Arrow;

		final boolean isShift = (mouseEvent.stateMask & SWT.SHIFT) != 0;

		if (isShift) {

			if (_selectedSegmentLabel_1 == null) {

				// start new selection
				_selectedSegmentLabel_1 = hoveredLabel;
				_selectedSegmentLabel_2 = null;

			} else {

				// extend selection
				_selectedSegmentLabel_2 = hoveredLabel;
			}

		} else {

			// start new selection
			_selectedSegmentLabel_1 = hoveredLabel;
			_selectedSegmentLabel_2 = null;
		}

		fireSegmentLabelSelection(_selectedSegmentLabel_1, _selectedSegmentLabel_2);

		// redraw chart
		setChartOverlayDirty();
	}

	private void onSegmentLabel_MouseMove(final ChartMouseEvent mouseEvent) {

		// ignore events with the same time
		if (mouseEvent.eventTime == _hoveredSegmentLabelEventTime) {

			mouseEvent.isWorked = _isSegmentLabelHovered;
			mouseEvent.cursor = ChartCursor.Arrow;

			// must be painted otherwise it is flickering when selected
			setChartOverlayDirty();

			return;
		}

		_hoveredSegmentLabelEventTime = mouseEvent.eventTime;

		final ChartLabel hoveredLabel = getHoveredSegmentLabel(mouseEvent);

		_isSegmentLabelHovered = hoveredLabel != null;

		if (_isSegmentLabelHovered) {

			// set worked that no other actions are done in this event
			mouseEvent.isWorked = _isSegmentLabelHovered;
			mouseEvent.cursor = ChartCursor.Arrow;
		}

		boolean isUpdateUI = false;

		if (hoveredLabel != _hoveredSegmentLabel) {

			// hovered label has changed

			_hoveredSegmentLabel = hoveredLabel;

			isUpdateUI = true;
		}

		if (_selectedSegmentLabel_1 != null) {
			isUpdateUI = true;
		}

		if (isUpdateUI) {
			setChartOverlayDirty();
		}
	}

	private void onSegmentLabel_MouseUp(final ChartMouseEvent mouseEvent) {

		final ChartLabel segmentLabel = getHoveredSegmentLabel(mouseEvent);

		if (segmentLabel == null) {
			return;
		}

		final boolean isHovered = segmentLabel != null;
		if (isHovered) {

			// set marker default cursor when the mouse is still hovering a marker
			mouseEvent.isWorked = true;
			mouseEvent.cursor = ChartCursor.Arrow;
		}
	}

	private void onSegmentLabel_Reset() {

		_hoveredSegmentLabel = null;

		setChartOverlayDirty();
	}

	/**
	 * Open tour quick editor.
	 * 
	 * @param mouseEvent
	 */
	private void onSegmentTitle_MouseDoubleClick(final ChartMouseEvent mouseEvent) {

		final ChartSegment hoveredSegment = getHoveredSegmentTitle(mouseEvent);

		_hoveredSegmentTitle = hoveredSegment;

		if (hoveredSegment == null) {
			return;
		}

		// title is hovered and double clicked, quick edit the tour
		mouseEvent.isWorked = _isSegmentTitleHovered;

		_actionEditQuick.run();
	}

	private void onSegmentTitle_MouseDown(final ChartMouseEvent mouseEvent) {

		final ChartSegment tourSegment = getHoveredSegmentTitle(mouseEvent);

		if (tourSegment == null) {
			return;
		}

		// title is hovered and clicked, fire tour selection
//		mouseEvent.isWorked = _isSegmentHovered;
	}

	private void onSegmentTitle_MouseExit() {

		_hoveredSegmentTitle = null;

		setHoveredSegment(null);

		_tourTitleTooltip.hide();
	}

	private void onSegmentTitle_MouseMove(final ChartMouseEvent mouseEvent) {

		// ignore events with the same time
		if (mouseEvent.eventTime == _hoveredSegmentTitleEventTime) {

			mouseEvent.isWorked = _isSegmentTitleHovered;
			mouseEvent.cursor = ChartCursor.Arrow;

			return;
		}

		_hoveredSegmentTitleEventTime = mouseEvent.eventTime;

		final ChartSegment hoveredSegment = getHoveredSegmentTitle(mouseEvent);

		_isSegmentTitleHovered = hoveredSegment != null;

		if (_isSegmentTitleHovered) {

			// set worked that no other actions are done in this event
			mouseEvent.isWorked = _isSegmentTitleHovered;
			mouseEvent.cursor = ChartCursor.Arrow;
		}

		final ChartSegment prevHoveredTour = _hoveredSegmentTitle;

		if (_hoveredSegmentTitle != hoveredSegment) {

			// hovered title has changed, show or hide tooltip

			// update internal state
			_hoveredSegmentTitle = hoveredSegment;

			// update state in the chart
			setHoveredSegment(hoveredSegment);

			if (_tcc.isShowInfoTooltip) {

				// show/hide tooltip
				if (hoveredSegment == null) {

					_tourTitleTooltip.hide();

				} else {

					_openDlgMgr.closeOpenedDialogs(_tourTitleTooltip);
					_tourTitleTooltip.open(hoveredSegment);
				}
			}

		} else if (hoveredSegment == null && prevHoveredTour != null) {

			// hide tooltip when not yet hidden

			_tourTitleTooltip.hide();
		}
	}

	private void onSegmentTitle_Resized() {

		_hoveredSegmentTitle = null;

		setHoveredSegment(null);

		_tourTitleTooltip.hide();
	}

	public void partIsDeactivated() {

		// hide photo tooltip
		_photoTooltip.hide();
	}

	public void partIsHidden() {

		// hide value point tooltip
		_valuePointTooltip.setShellVisible(false);

		// hide photo tooltip
		_photoTooltip.hide();
	}

	public void partIsVisible() {

		// show tool tip again
		_valuePointTooltip.setShellVisible(true);
	}

	public void removeTourMarkerSelectionListener(final ITourMarkerSelectionListener listener) {
		_tourMarkerSelectionListener.remove(listener);
	}

	public void removeTourModifySelectionListener(final ITourModifyListener listener) {
		_tourModifyListener.remove(listener);
	}

	public void removeXAxisSelectionListener(final IXAxisSelectionListener listener) {
		_xAxisSelectionListener.remove(listener);
	}

	void restoreState() {

		_photoTooltip.restoreState();
	}

	void saveState() {

		_photoTooltip.saveState();
		_valuePointTooltip.saveState();
	}

	void selectMarker(final SelectionChartXSliderPosition xSliderPosition) {

		// set position for the x-sliders
		setXSliderPosition(xSliderPosition);

		final Object customData = xSliderPosition.getCustomData();
		if (customData instanceof SelectedTourSegmenterSegments) {

			final SelectedTourSegmenterSegments selectedSegments = (SelectedTourSegmenterSegments) customData;

			// select segments

		} else {

			/*
			 * Select tour marker
			 */
			if (_layerMarker != null) {

				final int leftSliderValueIndex = xSliderPosition.getLeftSliderValueIndex();
				if (leftSliderValueIndex != SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {

					// check if a marker is selected
					for (final TourMarker tourMarker : _tourData.getTourMarkers()) {

						if (tourMarker.getSerieIndex() == leftSliderValueIndex) {

							// marker is selected, get marker from chart label

							_selectedTourMarker = tourMarker;

							// redraw chart
							setChartOverlayDirty();

							break;
						}
					}
				}
			}
		}
	}

	/**
	 * Set the check state for a command and update the UI
	 * 
	 * @param commandId
	 * @param isChecked
	 */
	public void setActionChecked(final String commandId, final Boolean isChecked) {

		_allTourChartActions.get(commandId).setChecked(isChecked);
	}

	/**
	 * Set the enable state for a command and update the UI
	 */
	public void setActionEnabled(final String commandId, final boolean isEnabled) {

		final Action action = _allTourChartActions.get(commandId);

		if (action != null) {
			action.setEnabled(isEnabled);
		}
	}

	/**
	 * Set the enable/check state for a command and update the UI
	 */
	public void setActionState(final String commandId, final boolean isEnabled, final boolean isChecked) {

		final Action action = _allTourChartActions.get(commandId);

		if (action != null) {
			action.setEnabled(isEnabled);
			action.setChecked(isChecked);
		}
	}

	/**
	 * When a tour chart is opened in a dialog, some actions should not be done.
	 * 
	 * @param isDisplayedInDialog
	 */
	public void setIsDisplayedInDialog(final boolean isDisplayedInDialog) {

		_isDisplayedInDialog = isDisplayedInDialog;
	}

	/**
	 * @param property
	 * @param isChartModified
	 * @param prefIsMaxEnabled
	 * @param prefMaxValue
	 * @param yDataInfoId
	 * @param valueDivisor
	 * @param maxAdjustment
	 *            Is disabled when set to {@link Double#MIN_VALUE}.
	 * @return
	 */
	private boolean setMaxDefaultValue(	final String property,
										boolean isChartModified,
										final String prefIsMaxEnabled,
										final String prefMaxValue,
										final int yDataInfoId,
										final int valueDivisor,
										final double maxAdjustment) {

		if (property.equals(prefIsMaxEnabled) || property.equals(prefMaxValue)) {

			final boolean isMaxEnabled = _prefStore.getBoolean(prefIsMaxEnabled);

			final ArrayList<ChartDataYSerie> yDataList = getChartDataModel().getYData();

			// get y-data serie from custom data
			ChartDataYSerie yData = null;
			for (final ChartDataYSerie yDataIterator : yDataList) {
				final Integer yDataInfo = (Integer) yDataIterator.getCustomData(ChartDataYSerie.YDATA_INFO);
				if (yDataInfo == yDataInfoId) {
					yData = yDataIterator;
					break;
				}
			}

			if (yData != null) {

				if (isMaxEnabled) {

					// set visible max value from the preferences

					double maxValue = _prefStore.getInt(prefMaxValue);

					if (maxAdjustment != Double.MIN_VALUE) {
						maxValue = maxValue > 0 //
								? maxValue - maxAdjustment
								: maxValue + maxAdjustment;
					}

					yData.setVisibleMaxValue(valueDivisor == 0 ? maxValue : maxValue * valueDivisor);

				} else {

					// reset visible max value to the original max value
					yData.setVisibleMaxValue(yData.getOriginalMaxValue());
				}

				isChartModified = true;
			}
		}

		return isChartModified;
	}

	/**
	 * @param property
	 * @param isChartModified
	 * @param tagIsMinEnabled
	 * @param tabMinValue
	 * @param yDataInfoId
	 * @param valueDivisor
	 * @param minAdjustment
	 *            Is disabled when set to {@link Double#MIN_VALUE}.
	 * @return
	 */
	private boolean setMinDefaultValue(	final String property,
										boolean isChartModified,
										final String tagIsMinEnabled,
										final String tabMinValue,
										final int yDataInfoId,
										final int valueDivisor,
										final double minAdjustment) {

		if (property.equals(tagIsMinEnabled) || property.equals(tabMinValue)) {

			final boolean isMinEnabled = _prefStore.getBoolean(tagIsMinEnabled);

			final ArrayList<ChartDataYSerie> yDataList = getChartDataModel().getYData();

			// get y-data serie from custom data
			ChartDataYSerie yData = null;
			for (final ChartDataYSerie yDataIterator : yDataList) {
				final Integer yDataInfo = (Integer) yDataIterator.getCustomData(ChartDataYSerie.YDATA_INFO);
				if (yDataInfo == yDataInfoId) {
					yData = yDataIterator;
					break;
				}
			}

			if (yData != null) {

				if (isMinEnabled) {

					// set visible min value from the preferences
					double minValue = _prefStore.getInt(tabMinValue);

					if (minAdjustment != Double.MIN_VALUE) {
						minValue += minAdjustment;
					}

					yData.setVisibleMinValue(valueDivisor == 0 ? minValue : minValue * valueDivisor);

				} else {

					// reset visible min value to the original min value

					yData.setVisibleMinValue(yData.getOriginalMinValue());
				}

				isChartModified = true;
			}
		}

		return isChartModified;
	}

	@Override
	public void setMouseMode(final boolean isChecked) {
		super.setMouseMode(isChecked);
		enableZoomOptions();
	}

	@Override
	public void setMouseMode(final Object newMouseMode) {
		super.setMouseMode(newMouseMode);
		enableZoomOptions();
	}

	/**
	 * Enable or disable the edit actions in the tour info tooltip, by default the edit actions are
	 * disabled.
	 * 
	 * @param isEnabled
	 */
	public void setTourInfoActionsEnabled(final boolean isEnabled) {

		if (_tourInfoIconTooltipProvider != null) {
			_tourInfoIconTooltipProvider.setActionsEnabled(isEnabled);
		}
	}

	private void setupChartConfig() {

		graphAntialiasing = _prefStore.getBoolean(//
				ITourbookPreferences.GRAPH_ANTIALIASING) ? SWT.ON : SWT.OFF;

		isShowSegmentAlternateColor = _prefStore.getBoolean(//
				ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR);
		segmentAlternateColor = PreferenceConverter.getColor(_prefStore, //
				ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR);

		graphTransparencyLine = _prefStore.getInt(//
				ITourbookPreferences.GRAPH_TRANSPARENCY_LINE);
		graphTransparencyFilling = _prefStore.getInt(//
				ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING);

		gridVerticalDistance = _prefStore.getInt(//
				ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE);
		gridHorizontalDistance = _prefStore.getInt(//
				ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE);

		isShowHorizontalGridLines = _prefStore.getBoolean(//
				ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES);
		isShowVerticalGridLines = _prefStore.getBoolean(//
				ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES);
	}

	/**
	 * set custom data for all graphs
	 */
	private void setupGraphLayer() {

		final ChartDataModel dataModel = getChartDataModel();
		if (dataModel == null) {
			return;
		}

		/*
		 * the tour markers are displayed in the altitude graph, when this graph is not available,
		 * the markers are painted in one of the other graphs
		 */
		ChartDataYSerie yDataWithLabels = (ChartDataYSerie) dataModel.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE);

		if (yDataWithLabels == null) {
			yDataWithLabels = (ChartDataYSerie) dataModel.getCustomData(TourManager.CUSTOM_DATA_PULSE);
		}
		if (yDataWithLabels == null) {
			yDataWithLabels = (ChartDataYSerie) dataModel.getCustomData(TourManager.CUSTOM_DATA_SPEED);
		}
		if (yDataWithLabels == null) {
			yDataWithLabels = (ChartDataYSerie) dataModel.getCustomData(TourManager.CUSTOM_DATA_PACE);
		}
		if (yDataWithLabels == null) {
			yDataWithLabels = (ChartDataYSerie) dataModel.getCustomData(TourManager.CUSTOM_DATA_POWER);
		}
		if (yDataWithLabels == null) {
			yDataWithLabels = (ChartDataYSerie) dataModel.getCustomData(TourManager.CUSTOM_DATA_GRADIENT);
		}
		if (yDataWithLabels == null) {
			yDataWithLabels = (ChartDataYSerie) dataModel.getCustomData(TourManager.CUSTOM_DATA_ALTIMETER);
		}
		if (yDataWithLabels == null) {
			yDataWithLabels = (ChartDataYSerie) dataModel.getCustomData(TourManager.CUSTOM_DATA_TEMPERATURE);
		}
		if (yDataWithLabels == null) {
			yDataWithLabels = (ChartDataYSerie) dataModel.getCustomData(TourManager.CUSTOM_DATA_CADENCE);
		}
		if (yDataWithLabels == null) {
			yDataWithLabels = (ChartDataYSerie) dataModel.getCustomData(TourManager.CUSTOM_DATA_GEAR_RATIO);
		}

		ConfigGraphSegment segmentConfigAltitude = null;
		ConfigGraphSegment segmentConfigPulse = null;
		ConfigGraphSegment segmentConfigSpeed = null;
		ConfigGraphSegment segmentConfigPace = null;
		ConfigGraphSegment segmentConfigPower = null;
		ConfigGraphSegment segmentConfigGradient = null;
		ConfigGraphSegment segmentConfigAltimeter = null;
		ConfigGraphSegment segmentConfigCadence = null;

		/*
		 * Setup tour segmenter data
		 */
		if (_isTourSegmenterVisible) {

			final IValueLabelProvider labelProviderInt = TourManager.getLabelProviderInt();
			final IValueLabelProvider labelProviderMMSS = TourManager.getLabelProviderMMSS();

			segmentConfigAltitude = new ConfigGraphSegment(GraphColorManager.PREF_GRAPH_ALTITUDE);
			segmentConfigAltitude.segmentDataSerie = _tourData.segmentSerieAltitudeDiff;
			segmentConfigAltitude.labelProvider = labelProviderInt;
			segmentConfigAltitude.canHaveNegativeValues = true;
			segmentConfigAltitude.minValueAdjustment = 0.1;

			segmentConfigPulse = new ConfigGraphSegment(GraphColorManager.PREF_GRAPH_HEARTBEAT);
			segmentConfigPulse.segmentDataSerie = _tourData.segmentSeriePulse;
			segmentConfigPulse.labelProvider = null;
			segmentConfigPulse.canHaveNegativeValues = false;
			segmentConfigPulse.minValueAdjustment = Double.MIN_VALUE;

			segmentConfigSpeed = new ConfigGraphSegment(GraphColorManager.PREF_GRAPH_SPEED);
			segmentConfigSpeed.segmentDataSerie = _tourData.segmentSerieSpeed;
			segmentConfigSpeed.labelProvider = null;
			segmentConfigSpeed.canHaveNegativeValues = false;
			segmentConfigSpeed.minValueAdjustment = Double.MIN_VALUE;

			segmentConfigPace = new ConfigGraphSegment(GraphColorManager.PREF_GRAPH_PACE);
			segmentConfigPace.segmentDataSerie = _tourData.segmentSeriePace;
			segmentConfigPace.labelProvider = labelProviderMMSS;
			segmentConfigPace.canHaveNegativeValues = false;
			segmentConfigPace.minValueAdjustment = Double.MIN_VALUE;

			segmentConfigPower = new ConfigGraphSegment(GraphColorManager.PREF_GRAPH_POWER);
			segmentConfigPower.segmentDataSerie = _tourData.segmentSeriePower;
			segmentConfigPower.labelProvider = labelProviderInt;
			segmentConfigPower.canHaveNegativeValues = false;
			segmentConfigPower.minValueAdjustment = 0.5;

			segmentConfigGradient = new ConfigGraphSegment(GraphColorManager.PREF_GRAPH_GRADIENT);
			segmentConfigGradient.segmentDataSerie = _tourData.segmentSerieGradient;
			segmentConfigGradient.labelProvider = null;
			segmentConfigGradient.canHaveNegativeValues = true;
			segmentConfigGradient.minValueAdjustment = 0.5;

			segmentConfigAltimeter = new ConfigGraphSegment(GraphColorManager.PREF_GRAPH_ALTIMETER);
			segmentConfigAltimeter.segmentDataSerie = _tourData.segmentSerieAltitudeUpH;
			segmentConfigAltimeter.labelProvider = labelProviderInt;
			segmentConfigAltimeter.canHaveNegativeValues = true;
			segmentConfigAltimeter.minValueAdjustment = 0.5;

			segmentConfigCadence = new ConfigGraphSegment(GraphColorManager.PREF_GRAPH_CADENCE);
			segmentConfigCadence.segmentDataSerie = _tourData.segmentSerieCadence;
			segmentConfigCadence.labelProvider = null;
			segmentConfigCadence.canHaveNegativeValues = false;
			segmentConfigCadence.minValueAdjustment = Double.MIN_VALUE;
		}

		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_ALTIMETER, yDataWithLabels, segmentConfigAltimeter);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_ALTITUDE, yDataWithLabels, segmentConfigAltitude);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_CADENCE, yDataWithLabels, segmentConfigCadence);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_GEAR_RATIO, yDataWithLabels, null);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_GRADIENT, yDataWithLabels, segmentConfigGradient);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_HISTORY, null, null);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_PULSE, yDataWithLabels, segmentConfigPulse);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_SPEED, yDataWithLabels, segmentConfigSpeed);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_PACE, yDataWithLabels, segmentConfigPace);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_POWER, yDataWithLabels, segmentConfigPower);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_TEMPERATURE, yDataWithLabels, null);
	}

	/**
	 * Set data for each graph
	 * 
	 * @param customDataKey
	 * @param segmentDataSerie
	 * @param yDataWithLabels
	 */
	private void setupGraphLayer_Layer(	final String customDataKey,
										final ChartDataYSerie yDataWithLabels,
										final ConfigGraphSegment segmentConfig) {

		final ChartDataModel dataModel = getChartDataModel();
		final ChartDataYSerie yData = (ChartDataYSerie) dataModel.getCustomData(customDataKey);

		if (yData == null) {
			return;
		}

		final ArrayList<IChartLayer> customFgLayers = new ArrayList<IChartLayer>();

		/**
		 * Sequence of the graph layer is the z-order, last added layer is on the top
		 */

		/*
		 * marker layer
		 */
		// show label layer only for ONE visible graph
		if (_layerMarker != null && yData == yDataWithLabels) {
			customFgLayers.add(_layerMarker);
		}

		/*
		 * photo layer
		 */
		// show photo layer only for ONE visible graph
		if (_layerPhoto != null
				&& _tcc.isShowTourPhotos == true
				&& (yData == yDataWithLabels || dataModel.getChartType() == ChartType.HISTORY)) {

			customFgLayers.add(_layerPhoto);
		}

		/*
		 * Tour segmenter layer
		 */
		final ChartDataYSerie yDataAltitude = (ChartDataYSerie) dataModel
				.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE);
		if (yData == yDataAltitude) {
			if (_layerTourSegmenterAltitude != null) {
				customFgLayers.add(_layerTourSegmenterAltitude);
			}
		} else {
			if (_layerTourSegmenterOther != null) {
				customFgLayers.add(_layerTourSegmenterOther);
			}
		}

		/*
		 * display merge layer only together with the altitude graph
		 */
		if ((_layer2ndAltiSerie != null) && customDataKey.equals(TourManager.CUSTOM_DATA_ALTITUDE)) {
			customFgLayers.add(_layer2ndAltiSerie);
		}

		/*
		 * HR zone painter
		 */
		if (_hrZonePainter != null) {
			yData.setCustomFillPainter(_hrZonePainter);
		}

		// set custom layers, no layers are set when layer list is empty
		yData.setCustomForegroundLayers(customFgLayers);

		// set segment data series
		if (segmentConfig != null) {
			yData.setCustomData(TourManager.CUSTOM_DATA_SEGMENT_VALUES, segmentConfig);
		}
	}

	private void setupSegmenterValueFont() {

		if (_segmenterValueFont != null) {
			_segmenterValueFont.dispose();
		}

		final FontData[] valueFontData = PreferenceConverter.getFontDataArray(
				_prefStore,
				ITourbookPreferences.TOUR_SEGMENTER_CHART_VALUE_FONT);

		_segmenterValueFont = new Font(getDisplay(), valueFontData);
	}

	private void setupSegments_Title() {

		final ChartSegmentConfig csConfig = getChartSegmentConfig();

		if (_tcc.isTourInfoVisible) {

			// show tour info
			addMouseChartListener(_mouseSegmentTitle_Listener);
			addMouseChartMoveListener(_mouseSegmentTitle_MoveListener);

			csConfig.isShowSegmentBackground = true;
			csConfig.isShowSegmentSeparator = _tcc.isShowInfoTourSeparator;
			csConfig.isShowSegmentTitle = _tcc.isShowInfoTitle;

		} else {

			// hide tour info
			removeMouseChartListener(_mouseSegmentTitle_Listener);
			removeMouseMoveChartListener(_mouseSegmentTitle_MoveListener);

			csConfig.isShowSegmentBackground = false;
			csConfig.isShowSegmentSeparator = false;
			csConfig.isShowSegmentTitle = false;
		}

		csConfig.isMultipleSegments = _tourData.isMultipleTours;

		_tourTitleTooltip.setFadeInDelayTime(_tcc.tourInfoTooltipDelay);
	}

	/**
	 * Add/removes the toursegmenter mouse listeners and sets's the state
	 * {@link #_isTourSegmenterVisible} if tour segmenter is visible or not.
	 */
	private void setupTourSegmenter() {

		final boolean isSegmenterActive = Util.getStateBoolean(
				_tourSegmenterState,
				TourSegmenterView.STATE_IS_SEGMENTER_ACTIVE,
				false);

		final boolean isShowTourSegments = Util.getStateBoolean(
				_tourSegmenterState,
				TourSegmenterView.STATE_IS_SHOW_TOUR_SEGMENTS,
				TourSegmenterView.STATE_IS_SHOW_TOUR_SEGMENTS_DEFAULT);

		_isTourSegmenterVisible = isSegmenterActive && isShowTourSegments;

		if (_isTourSegmenterVisible) {

			addMouseChartListener(_mouseSegmentLabel_Listener);
			addMouseChartMoveListener(_mouseSegmentLabel_MoveListener);

		} else {

			if (_layerTourSegmenterAltitude != null) {

				// disable segment layers
				removeChartOverlay(_layerTourSegmenterAltitude);

				_layerTourSegmenterAltitude = null;
				_layerTourSegmenterOther = null;
			}

			removeMouseChartListener(_mouseSegmentLabel_Listener);
			removeMouseMoveChartListener(_mouseSegmentLabel_MoveListener);
		}
	}

	/**
	 * set's the chart which is synched with this chart
	 * 
	 * @param isSynchEnabled
	 *            <code>true</code> to enable synch, <code>false</code> to disable synch
	 * @param synchedChart
	 *            contains the {@link Chart} which is synched with this chart
	 * @param synchMode
	 */
	public void synchChart(final boolean isSynchEnabled, final TourChart synchedChart, final int synchMode) {

		// enable/disable synched chart
		super.setSynchedChart(isSynchEnabled ? synchedChart : null);

		final Map<String, Action> synchChartActions = synchedChart._allTourChartActions;

		if (synchChartActions == null) {
			return;
		}

		synchedChart.setSynchMode(synchMode);

		/*
		 * when the position listener is set, the zoom actions will be deactivated
		 */
		if (isSynchEnabled) {

			// synchronize this chart with the synchedChart

			// disable zoom actions
			synchedChart.setZoomActionsEnabled(false);
			synchedChart.updateZoomOptions(false);

			// set the synched chart to auto-zoom
			synchedChart.setCanAutoZoomToSlider(true);

			// hide the x-sliders
//			fBackupIsXSliderVisible = synchedChart.isXSliderVisible();
			synchedChart.setShowSlider(false);

			synchronizeChart();

		} else {

			// disable chart synchronization

			// enable zoom action
//			actionProxies.get(COMMAND_ID_CAN_SCROLL_CHART).setChecked(synchedChart.getCanScrollZoomedChart());
			synchChartActions.get(ACTION_ID_CAN_AUTO_ZOOM_TO_SLIDER).setChecked(synchedChart.getCanAutoZoomToSlider());

			synchedChart.setZoomActionsEnabled(true);
			synchedChart.updateZoomOptions(true);

			// restore the x-sliders
			synchedChart.setShowSlider(true);

			synchedChart.setSynchConfig(null);

			// show whole chart
			synchedChart.getChartDataModel().resetMinMaxValues();
//			synchedChart.onExecuteZoomOut(true);
			synchedChart.onExecuteZoomFitGraph();
		}
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();

		sb.append(this.getClass().getSimpleName());
		sb.append(UI.NEW_LINE);

		sb.append(_tourData);
		sb.append(UI.NEW_LINE);

		return sb.toString();
	}

	@Override
	public void updateChart(final ChartDataModel chartDataModel, final boolean isShowAllData) {

		super.updateChart(chartDataModel, isShowAllData);

		if (chartDataModel == null) {

			_tourData = null;
			_tcc = null;

			_valuePointTooltip.setTourData(null);

			// disable all actions
			if (_allTourChartActions != null) {

				for (final Action action : _allTourChartActions.values()) {
					action.setEnabled(false);
				}
			}

			if (_actionTourInfo != null) {
				_actionTourInfo.setEnabled(false);
			}

			if (_actionTourMarker != null) {
				_actionTourMarker.setEnabled(false);
			}
		}
	}

	public void updateLayer2ndAlti(final I2ndAltiLayer alti2ndLayerProvider, final boolean isLayerVisible) {

		_is2ndAltiLayerVisible = isLayerVisible;
		_layer2ndAlti = alti2ndLayerProvider;

		createLayer_2ndAlti();

		setupGraphLayer();
		updateCustomLayers();
	}

	@Override
	public void updateModifiedTourMarker(final TourMarker tourMarker) {

		if (_isDisplayedInDialog) {

			// this will update the chart
			fireTourMarkerModifyEvent(tourMarker, false);

		} else {

			// tour will be saved and the chart is also updated
			fireTourModifyEvent_Globally();
		}
	}

	private void updatePhotoAction() {

		String toolTip;
		ImageDescriptor imageDescriptor;

		final boolean isShowPhotos = _tcc.isShowTourPhotos;
		final boolean isShowTooltip = _tcc.isShowTourPhotoTooltip;

		if (isShowPhotos && isShowTooltip) {

			toolTip = Messages.Tour_Action_TourPhotosWithTooltip_Tooltip;
			imageDescriptor = _imagePhotoTooltip;

		} else if (!isShowPhotos && !isShowTooltip) {

			toolTip = Messages.Tour_Action_TourPhotos;
			imageDescriptor = _imagePhoto;

		} else {

			toolTip = Messages.Tour_Action_TourPhotosWithoutTooltip_Tooltip;
			imageDescriptor = _imagePhoto;
		}

		final Action action = _allTourChartActions.get(ACTION_ID_IS_SHOW_TOUR_PHOTOS);

		action.setToolTipText(toolTip);
		action.setImageDescriptor(imageDescriptor);
		action.setChecked(isShowPhotos);
	}

	/**
	 * Check and enable the graph actions, the visible state of a graph is defined in the tour chart
	 * config.
	 */
	public void updateTourActions() {

		/*
		 * all graph actions
		 */
		final int[] allGraphIds = TourManager.getAllGraphIDs();
		final ArrayList<Integer> visibleGraphIds = _tcc.getVisibleGraphs();
		final ArrayList<Integer> enabledGraphIds = new ArrayList<Integer>();

		// get all graph ids which can be displayed
		for (final ChartDataSerie xyDataIterator : getChartDataModel().getXyData()) {

			if (xyDataIterator instanceof ChartDataYSerie) {
				final ChartDataYSerie yData = (ChartDataYSerie) xyDataIterator;
				final Integer graphId = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_INFO);
				enabledGraphIds.add(graphId);
			}
		}

		Action tourAction;

		for (final int graphId : allGraphIds) {

			tourAction = _allTourChartActions.get(getGraphActionId(graphId));
			tourAction.setChecked(visibleGraphIds.contains(graphId));
			tourAction.setEnabled(enabledGraphIds.contains(graphId));
		}

		/*
		 * HR zones
		 */
		final boolean canShowHrZones = _tcc.canShowHrZones;
		final String currentHrZoneStyle = _tcc.hrZoneStyle;

		tourAction = _allTourChartActions.get(ACTION_ID_HR_ZONE_DROPDOWN_MENU);
		tourAction.setEnabled(canShowHrZones);

		tourAction = _allTourChartActions.get(ACTION_ID_HR_ZONE_STYLE_GRAPH_TOP);
		tourAction.setEnabled(true);
		tourAction.setChecked(currentHrZoneStyle.equals(ACTION_ID_HR_ZONE_STYLE_GRAPH_TOP));

		tourAction = _allTourChartActions.get(ACTION_ID_HR_ZONE_STYLE_NO_GRADIENT);
		tourAction.setEnabled(true);
		tourAction.setChecked(currentHrZoneStyle.equals(ACTION_ID_HR_ZONE_STYLE_NO_GRADIENT));

		tourAction = _allTourChartActions.get(ACTION_ID_HR_ZONE_STYLE_WHITE_TOP);
		tourAction.setEnabled(true);
		tourAction.setChecked(currentHrZoneStyle.equals(ACTION_ID_HR_ZONE_STYLE_WHITE_TOP));

		tourAction = _allTourChartActions.get(ACTION_ID_HR_ZONE_STYLE_WHITE_BOTTOM);
		tourAction.setEnabled(true);
		tourAction.setChecked(currentHrZoneStyle.equals(ACTION_ID_HR_ZONE_STYLE_WHITE_BOTTOM));

		/*
		 * Tour infos
		 */
		_actionTourInfo.setSelected(_tcc.isTourInfoVisible);
		_actionTourInfo.setEnabled(true);

		/*
		 * Tour marker
		 */
		_actionTourMarker.setSelected(_tcc.isShowTourMarker);
		_actionTourMarker.setEnabled(true);

		/*
		 * Tour photos
		 */
		tourAction = _allTourChartActions.get(ACTION_ID_IS_SHOW_TOUR_PHOTOS);
		tourAction.setEnabled(true);
		updatePhotoAction();

		/*
		 * Breaktime values
		 */
		tourAction = _allTourChartActions.get(ACTION_ID_IS_SHOW_BREAKTIME_VALUES);
		tourAction.setEnabled(true);
		tourAction.setChecked(_tcc.isShowBreaktimeValues);

		/*
		 * Value point tool tip
		 */
		tourAction = _allTourChartActions.get(ACTION_ID_IS_SHOW_VALUEPOINT_TOOLTIP);
		tourAction.setEnabled(true);
		final boolean isVisible = _valuePointTooltip.isVisible();
		tourAction.setChecked(isVisible);

		/*
		 * SRTM data
		 */
		tourAction = _allTourChartActions.get(ACTION_ID_IS_SHOW_SRTM_DATA);
		final boolean canShowSRTMData = _tcc.canShowSRTMData;
		tourAction.setEnabled(canShowSRTMData);
		tourAction.setChecked(canShowSRTMData ? _tcc.isSRTMDataVisible : false);

		/*
		 * Overlapped graphs
		 */
		tourAction = _allTourChartActions.get(ACTION_ID_IS_GRAPH_OVERLAPPED);
		tourAction.setEnabled(true);
		tourAction.setChecked(_tcc.isGraphOverlapped);

		/*
		 * x-axis time/distance
		 */
		final boolean isShowTimeOnXAxis = _tcc.isShowTimeOnXAxis;

		tourAction = _allTourChartActions.get(ACTION_ID_IS_SHOW_START_TIME);
		tourAction.setEnabled(isShowTimeOnXAxis);
		tourAction.setChecked(_tcc.xAxisTime != X_AXIS_START_TIME.START_WITH_0);

		tourAction = _allTourChartActions.get(ACTION_ID_X_AXIS_TIME);
		tourAction.setEnabled(true); // time data are always available
		tourAction.setChecked(isShowTimeOnXAxis);

		tourAction = _allTourChartActions.get(ACTION_ID_X_AXIS_DISTANCE);
		tourAction.setChecked(!isShowTimeOnXAxis);
		tourAction.setEnabled(!_tcc.isForceTimeOnXAxis);

		// get options check status from the configuration
		final boolean isMoveSlidersWhenZoomed = _tcc.moveSlidersWhenZoomed;
		final boolean isAutoZoomToSlider = _tcc.autoZoomToSlider;
		final boolean canAutoZoom = getMouseMode().equals(Chart.MOUSE_MODE_ZOOM);

		// update tour chart actions
		tourAction = _allTourChartActions.get(ACTION_ID_CAN_AUTO_ZOOM_TO_SLIDER);
		tourAction.setEnabled(true);
		tourAction.setChecked(isAutoZoomToSlider);

		tourAction = _allTourChartActions.get(ACTION_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED);
		tourAction.setEnabled(canAutoZoom);
		tourAction.setChecked(isMoveSlidersWhenZoomed);

		// update the chart actions
		setCanAutoMoveSliders(isMoveSlidersWhenZoomed);
		setCanAutoZoomToSlider(isAutoZoomToSlider);

		tourAction = _allTourChartActions.get(ACTION_ID_EDIT_CHART_PREFERENCES);
		tourAction.setEnabled(true);
	}

	/**
	 * Update the tour chart with the previous data, configuration and min/max values.
	 */
	public void updateTourChart() {
		updateTourChartInternal(_tourData, _tcc, true, false);
	}

	/**
	 * Update the tour chart with the previous data and configuration.
	 * 
	 * @param keepMinMaxValues
	 *            <code>true</code> keeps the min/max values from the previous chart
	 */
	public void updateTourChart(final boolean keepMinMaxValues) {
		updateTourChartInternal(_tourData, _tcc, keepMinMaxValues, false);
	}

	/**
	 * Update the tour chart with the previous data and configuration
	 * 
	 * @param keepMinMaxValues
	 *            <code>true</code> keeps the min/max values from the previous chart
	 * @param isPropertyChanged
	 *            when <code>true</code> the properties for the tour chart have changed
	 */
	public void updateTourChart(final boolean keepMinMaxValues, final boolean isPropertyChanged) {
		updateTourChartInternal(_tourData, _tcc, keepMinMaxValues, isPropertyChanged);
	}

	public void updateTourChart(final TourData tourData, final boolean keepMinMaxValues) {
		updateTourChartInternal(tourData, _tcc, keepMinMaxValues, false);

	}

	/**
	 * Set {@link TourData} and {@link TourChartConfiguration} to create a new chart data model
	 * 
	 * @param tourData
	 * @param tourChartConfig
	 * @param keepMinMaxValues
	 *            <code>true</code> keeps the min/max values from the previous chart
	 */
	public void updateTourChart(final TourData tourData,
								final TourChartConfiguration tourChartConfig,
								final boolean keepMinMaxValues) {

		updateTourChartInternal(tourData, tourChartConfig, keepMinMaxValues, false);
	}

	/**
	 * This is the entry point for new tours.
	 * <p>
	 * This method is synchronized because when SRTM data are retrieved and the import view is
	 * openened, the error occured that the chart config was deleted with {@link #updateChart(null)}
	 * 
	 * @param newTourData
	 * @param newTCC
	 * @param keepMinMaxValues
	 * @param isPropertyChanged
	 */
	private synchronized void updateTourChartInternal(	final TourData newTourData,
														final TourChartConfiguration newTCC,
														final boolean keepMinMaxValues,
														final boolean isPropertyChanged) {

		if (newTourData == null || newTCC == null) {

			// there are no new tour data

			_valuePointTooltip.setTourData(null);
			return;
		}

		// keep min/max values for the 'old' chart in the chart config
		if (_tcc != null && keepMinMaxValues) {

			final ChartYDataMinMaxKeeper oldMinMaxKeeper = _tcc.getMinMaxKeeper();

			if (oldMinMaxKeeper != null) {
				oldMinMaxKeeper.saveMinMaxValues(getChartDataModel());
			}
		}

		// set current tour data and chart config to new values
		_tourData = newTourData;
		_tcc = newTCC;

		// cleanup old data
		_selectedTourMarker = null;
		hidePhotoLayer();

		final ChartDataModel newChartDataModel = TourManager.getInstance().createChartDataModel(
				_tourData,
				_tcc,
				isPropertyChanged);

		// set the model BEFORE actions are created/enabled/checked
		setDataModel(newChartDataModel);

		// create actions
		createActions();
		fillToolbar();
		updateTourActions();

		// restore min/max values from the chart config
		final ChartYDataMinMaxKeeper newMinMaxKeeper = _tcc.getMinMaxKeeper();
		final boolean isMinMaxKeeper = (newMinMaxKeeper != null) && keepMinMaxValues;
		if (isMinMaxKeeper) {
			newMinMaxKeeper.setMinMaxValues(newChartDataModel);
		}

		if (_chartDataModelListener != null) {
			_chartDataModelListener.dataModelChanged(newChartDataModel);
		}

		createLayer_TourSegmenter();
		createLayer_Marker(false);
		createLayer_2ndAlti();
		createLayer_Photo();

		createPainter_HrZone();

		setupGraphLayer();
		setupSegments_Title();

		updateChart(newChartDataModel, !isMinMaxKeeper);

		/*
		 * this must be done after the chart is created because is sets an action, set it only once
		 * when the chart is displayed the first time otherwise it's annoying
		 */
		if (_isMouseModeSet == false) {
			_isMouseModeSet = true;
			setMouseMode(_prefStore.getString(ITourbookPreferences.GRAPH_MOUSE_MODE).equals(Chart.MOUSE_MODE_SLIDER));
		}

		_tourInfoIconTooltipProvider.setTourData(_tourData);
		_valuePointTooltip.setTourData(_tourData);
		_tourMarkerTooltip.setIsShowMarkerActions(_tourData.isMultipleTours == false);
	}

	/**
	 * Toursegmenter is modified, update its layers.
	 */
	public void updateTourSegmenter() {

		if (_tourData == null) {
			return;
		}

		// reset selection because some config settings can reduce the number of segments
		_selectedSegmentLabel_1 = null;
		_selectedSegmentLabel_2 = null;

		setupTourSegmenter();

		if (_isTourSegmenterVisible) {

			// tour segmenter is visible

			createLayer_TourSegmenter();

		} else {

			// tour segmenter is hidden

			resetGraphAlpha();
		}

		setupGraphLayer();
		updateCustomLayers();

		redrawChart();
	}

	private void updateUI_Marker(final Boolean isMarkerVisible) {

		_tcc.isShowTourMarker = isMarkerVisible;

		updateUI_MarkerLayer(isMarkerVisible);

		// update actions
		_actionTourMarker.setSelected(isMarkerVisible);
	}

	void updateUI_MarkerLayer() {
		updateUI_MarkerLayer(true);
	}

	/**
	 * Updates the marker layer in the chart
	 * 
	 * @param isMarkerVisible
	 */
	public void updateUI_MarkerLayer(final boolean isMarkerVisible) {

		// create/hide marker layer
		if (isMarkerVisible) {
			createLayer_Marker(true);
		} else {
			hideMarkerLayer();
		}

		setupGraphLayer();

		// update marker layer
		updateCustomLayers();
	}

	void updateUI_TourTitleInfo() {

		setupSegments_Title();

		updateTourChart();
	}

	/**
	 * Update UI check state, the chart decides if the scroll/auto zoom options are available
	 */
	void updateZoomOptionActionHandlers() {

		setActionChecked(ACTION_ID_CAN_AUTO_ZOOM_TO_SLIDER, getCanAutoZoomToSlider());
		setActionChecked(ACTION_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED, getCanAutoMoveSliders());
	}

	/**
	 * Enable/disable the zoom options in the tour chart
	 * 
	 * @param isEnabled
	 */
	private void updateZoomOptions(final boolean isEnabled) {

		setActionEnabled(ACTION_ID_CAN_AUTO_ZOOM_TO_SLIDER, isEnabled);
		setActionEnabled(ACTION_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED, isEnabled);
	}

}
