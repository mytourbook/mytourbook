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

import gnu.trove.list.array.TIntArrayList;

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
import net.tourbook.chart.ChartKeyEvent;
import net.tourbook.chart.ChartMouseEvent;
import net.tourbook.chart.ChartTitleSegment;
import net.tourbook.chart.ChartTitleSegmentConfig;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.ChartYDataMinMaxKeeper;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.chart.IFillPainter;
import net.tourbook.chart.IHoveredValueListener;
import net.tourbook.chart.IKeyListener;
import net.tourbook.chart.ILineSelectionPainter;
import net.tourbook.chart.IMouseListener;
import net.tourbook.chart.ITooltipOwner;
import net.tourbook.chart.MouseAdapter;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.PointLong;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.IToolTipHideListener;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourSegment;
import net.tourbook.photo.Photo;
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
import net.tourbook.tour.photo.TourPhotoLink;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.tourChart.action.ActionCanAutoZoomToSlider;
import net.tourbook.ui.tourChart.action.ActionCanMoveSlidersWhenZoomed;
import net.tourbook.ui.tourChart.action.ActionGraph;
import net.tourbook.ui.tourChart.action.ActionGraphOverlapped;
import net.tourbook.ui.tourChart.action.ActionHrZoneDropDownMenu;
import net.tourbook.ui.tourChart.action.ActionHrZoneGraphType;
import net.tourbook.ui.tourChart.action.ActionTourChartInfo;
import net.tourbook.ui.tourChart.action.ActionTourChartMarker;
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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;

/**
 * The tour chart extends the chart with all the functionality for a tour chart
 */
public class TourChart extends Chart implements ITourProvider, ITourMarkerUpdater, ILineSelectionPainter {

	private static final String				ID										= "net.tourbook.ui.tourChart";															//$NON-NLS-1$
	//
	private static final int				PAGE_NAVIGATION_SEGMENTS				= 10;
	//
	private static final String				GRAPH_LABEL_ALTIMETER					= net.tourbook.common.Messages.Graph_Label_Altimeter;
	private static final String				GRAPH_LABEL_ALTITUDE					= net.tourbook.common.Messages.Graph_Label_Altitude;
	private static final String				GRAPH_LABEL_CADENCE						= net.tourbook.common.Messages.Graph_Label_Cadence;
	private static final String				GRAPH_LABEL_GEARS						= net.tourbook.common.Messages.Graph_Label_Gears;
	private static final String				GRAPH_LABEL_GRADIENT					= net.tourbook.common.Messages.Graph_Label_Gradient;
	private static final String				GRAPH_LABEL_HEARTBEAT					= net.tourbook.common.Messages.Graph_Label_Heartbeat;
	private static final String				GRAPH_LABEL_PACE						= net.tourbook.common.Messages.Graph_Label_Pace;
	private static final String				GRAPH_LABEL_POWER						= net.tourbook.common.Messages.Graph_Label_Power;
	private static final String				GRAPH_LABEL_SPEED						= net.tourbook.common.Messages.Graph_Label_Speed;
	private static final String				GRAPH_LABEL_TEMPERATURE					= net.tourbook.common.Messages.Graph_Label_Temperature;
	private static final String				GRAPH_LABEL_TOUR_COMPARE				= net.tourbook.common.Messages.Graph_Label_Tour_Compare;
	//
	public static final String				ACTION_ID_CAN_AUTO_ZOOM_TO_SLIDER		= "ACTION_ID_CAN_AUTO_ZOOM_TO_SLIDER";													//$NON-NLS-1$
	public static final String				ACTION_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED	= "ACTION_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED";											//$NON-NLS-1$
	public static final String				ACTION_ID_EDIT_CHART_PREFERENCES		= "ACTION_ID_EDIT_CHART_PREFERENCES";													//$NON-NLS-1$
	private static final String				ACTION_ID_IS_GRAPH_OVERLAPPED			= "ACTION_ID_IS_GRAPH_OVERLAPPED";														//$NON-NLS-1$
	public static final String				ACTION_ID_IS_SHOW_TOUR_PHOTOS			= "ACTION_ID_IS_SHOW_TOUR_PHOTOS";														//$NON-NLS-1$
	public static final String				ACTION_ID_HR_ZONE_DROPDOWN_MENU			= "ACTION_ID_HR_ZONE_DROPDOWN_MENU";													//$NON-NLS-1$
	public static final String				ACTION_ID_HR_ZONE_STYLE_GRAPH_TOP		= "ACTION_ID_HR_ZONE_STYLE_GRAPH_TOP";													//$NON-NLS-1$
	public static final String				ACTION_ID_HR_ZONE_STYLE_NO_GRADIENT		= "ACTION_ID_HR_ZONE_STYLE_NO_GRADIENT";												//$NON-NLS-1$
	public static final String				ACTION_ID_HR_ZONE_STYLE_WHITE_BOTTOM	= "ACTION_ID_HR_ZONE_STYLE_WHITE_BOTTOM";												//$NON-NLS-1$
	public static final String				ACTION_ID_HR_ZONE_STYLE_WHITE_TOP		= "ACTION_ID_HR_ZONE_STYLE_WHITE_TOP";													//$NON-NLS-1$
	public static final String				ACTION_ID_X_AXIS_DISTANCE				= "ACTION_ID_X_AXIS_DISTANCE";															//$NON-NLS-1$
	public static final String				ACTION_ID_X_AXIS_TIME					= "ACTION_ID_X_AXIS_TIME";																//$NON-NLS-1$
	//
	private static final String				GRID_PREF_PREFIX						= "GRID_TOUR_CHART__";																	//$NON-NLS-1$
	//
	private static final String				GRID_IS_SHOW_VERTICAL_GRIDLINES			= (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES);
	private static final String				GRID_IS_SHOW_HORIZONTAL_GRIDLINES		= (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES);
	private static final String				GRID_VERTICAL_DISTANCE					= (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE);
	private static final String				GRID_HORIZONTAL_DISTANCE				= (GRID_PREF_PREFIX + ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE);
	//
	/**
	 * 1e-5 is too small for the min value, it do not correct the graph.
	 */
	public static final double				MIN_ADJUSTMENT							= 1e-3;
	public static final double				MAX_ADJUSTMENT							= 1e-5;
	//
	private final IPreferenceStore			_prefStore								= TourbookPlugin.getPrefStore();
	private final IDialogSettings			_state									= TourbookPlugin.getState(ID);
	private final IDialogSettings			_tourSegmenterState						= TourSegmenterView.getState();
	//
	/**
	 * Part in which the tour chart is created, can be <code>null</code> when created in a dialog.
	 */
	private IWorkbenchPart					_part;																															;
	//
	private TourData						_tourData;
	private TourChartConfiguration			_tcc;
	//
	private Map<String, Action>				_allTourChartActions;
	private ActionEditQuick					_actionEditQuick;
	private ActionGraphMinMax				_actionGraphMinMax;
	private ActionOpenMarkerDialog			_actionOpenMarkerDialog;
	private ActionTourChartOptions			_actionTourChartOptions;
	private ActionTourChartSmoothing		_actionTourChartSmoothing;
	private ActionTourChartInfo				_actionTourInfo;
	private ActionTourChartMarker			_actionTourMarker;
	//
	/**
	 * datamodel listener is called when the chart data is created
	 */
	private IDataModelListener				_chartDataModelListener;

	private IPropertyChangeListener			_prefChangeListener;
	private final ListenerList				_tourMarkerModifyListener				= new ListenerList();
	private final ListenerList				_tourMarkerSelectionListener			= new ListenerList();
	private final ListenerList				_tourModifyListener						= new ListenerList();
	private final ListenerList				_xAxisSelectionListener					= new ListenerList();
	//
	private boolean							_is2ndAltiLayerVisible;
	private boolean							_isDisplayedInDialog;
	private boolean							_isMouseModeSet;
	private boolean							_isTourChartToolbarCreated;
	private TourMarker						_firedTourMarker;

	/**
	 * The {@link TourMarker} selection state is <b>only</b> be displayed when the mouse is hovering
	 * it.
	 */
	private TourMarker						_selectedTourMarker;

	private ImageDescriptor					_imagePhoto								= TourbookPlugin
																							.getImageDescriptor(Messages.Image__PhotoPhotos);

	private ImageDescriptor					_imagePhotoTooltip						= TourbookPlugin
																							.getImageDescriptor(Messages.Image__PhotoImage);
	private IFillPainter					_hrZonePainter;

	private OpenDialogManager				_openDlgMgr								= new OpenDialogManager();

	private ChartPhotoToolTip				_photoTooltip;
	private TourToolTip						_tourInfoIconTooltip;
	private TourInfoIconToolTipProvider		_tourInfoIconTooltipProvider;
	private ChartMarkerToolTip				_tourMarkerTooltip;
	private TourSegmenterTooltip			_tourSegmenterTooltip;
	private ChartTitleToolTip				_tourTitleTooltip;
	private ValuePointToolTipUI				_valuePointTooltip;
	//
	private ControlListener					_ttControlListener						= new ControlListener();
	private IKeyListener					_chartKeyListener						= new ChartKeyListener();
	private IMouseListener					_mouseMarkerListener					= new MouseMarkerListener();
	private IMouseListener					_mousePhotoListener						= new MousePhotoListener();
	private IMouseListener					_mouseSegmentLabel_Listener				= new MouseListener_SegmenterSegment();
	private IMouseListener					_mouseSegmentLabel_MoveListener			= new MouseListener_SegmenterSegment_Move();
	private IMouseListener					_mouseSegmentTitle_Listener				= new MouseListener_SegmentTitle();
	private IMouseListener					_mouseSegmentTitle_MoveListener			= new MouseListener_SegmentTitle_Move();
	private long							_hoveredSegmentTitleEventTime;
	//
	private boolean							_isSegmenterSegmentHovered;
	private long							_hoveredSegmenterSegmentEventTime;
	private SegmenterSegment				_hoveredSegmenterSegment;
	private SegmenterSegment				_selectedSegmenterSegment_1;
	private SegmenterSegment				_selectedSegmenterSegment_2;
	private boolean							_isRecomputeLineSelection;
	private TIntArrayList					_selectedAltitudePoints;
	private ArrayList<RGB>					_selectedAltitudeRGB;
	private ArrayList<TIntArrayList>		_selectedOtherPoints;
	private ArrayList<RGB>					_selectedPathsRGB;
	//
	private boolean							_isSegmentTitleHovered;
	private ChartTitleSegment				_chartTitleSegment;
	private TourMarker						_lastHoveredTourMarker;

	/**
	 * Hide tour segments when tour chart is displayed in dialogs.
	 */
	private boolean							_canShowTourSegments;

	private boolean							_isTourSegmenterVisible;
	private boolean							_isShowSegmenterTooltip;
	private SelectedTourSegmenterSegments	_segmenterSelection;
	private Font							_segmenterValueFont;
	private int								_oldTourSegmentsHash;

	/*
	 * UI controls
	 */
	private Composite						_parent;
	//
	private I2ndAltiLayer					_layer2ndAlti;
	private ChartLayerMarker				_layerMarker;
	private ChartLayer2ndAltiSerie			_layer2ndAltiSerie;
	private ChartLayerPhoto					_layerPhoto;
	private ChartLayerSegmentAltitude		_layerTourSegmenterAltitude;
	private ChartLayerSegmentValue			_layerTourSegmenterOther;
	//
	private Color							_photoOverlayBGColorLink;
	private Color							_photoOverlayBGColorTour;

	private class ActionGraphMinMax extends ActionToolbarSlideout {

		public ActionGraphMinMax(final ImageDescriptor imageDescriptor, final ImageDescriptor imageDescriptorDisabled) {

			super(imageDescriptor, imageDescriptorDisabled);
		}

		@Override
		protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

			return new SlideoutGraphMinMax(_parent, toolbar);
		}

		@Override
		protected void onBeforeOpenSlideout() {
			closeOpenedDialogs(this);
		}
	}

	private class ActionTourChartOptions extends ActionToolbarSlideout {

		@Override
		protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

			return new SlideoutTourChartOptions(_parent, toolbar, TourChart.this, GRID_PREF_PREFIX);
		}

		@Override
		protected void onBeforeOpenSlideout() {
			closeOpenedDialogs(this);
		}
	}

	private class ActionTourChartSmoothing extends ActionToolbarSlideout {

		public ActionTourChartSmoothing(final ImageDescriptor imageDescriptor,
										final ImageDescriptor imageDescriptorDisabled) {

			super(imageDescriptor, imageDescriptorDisabled);
		}

		@Override
		protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

			return new SlideoutTourChartSmoothing(_parent, toolbar, TourChart.this);
		}

		@Override
		protected void onBeforeOpenSlideout() {
			closeOpenedDialogs(this);
		}
	}

	private class ChartKeyListener implements IKeyListener {

		@Override
		public void keyDown(final ChartKeyEvent keyEvent) {
			onChart_KeyDown(keyEvent);
		}
	}

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

	private class MouseListener_SegmenterSegment extends MouseAdapter {

		@Override
		public void chartResized() {
			onSegmenterSegment_Resize();
		}

		@Override
		public void mouseDown(final ChartMouseEvent event) {
			onSegmenterSegment_MouseDown(event);
		}

		@Override
		public void mouseExit() {
			onSegmenterSegment_MouseExit();
		}

		@Override
		public void mouseUp(final ChartMouseEvent event) {
			onSegmenterSegment_MouseUp(event);
		}
	}

	/**
	 * This mouse move listener is used to get mouse move events to show the tour tooltip when the
	 * y-slider is dragged.
	 */
	private class MouseListener_SegmenterSegment_Move extends MouseAdapter {

		@Override
		public void mouseMove(final ChartMouseEvent event) {
			onSegmenterSegment_MouseMove(event);
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

		// Setup tooltips
		_tourTitleTooltip = new ChartTitleToolTip(this);
		_tourSegmenterTooltip = new TourSegmenterTooltip(this);

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
		setLineSelectionPainter(this);
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
			onExecuteZoomOut(true, 1.0);
		}

		updateZoomOptionActionHandlers();
	}

	public void actionGraphOverlapped(final boolean isItemChecked) {

		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_GRAPH_OVERLAPPED, isItemChecked);

		_tcc.isGraphOverlapped = isItemChecked;
		updateTourChart();

		setActionChecked(ACTION_ID_IS_GRAPH_OVERLAPPED, isItemChecked);
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

						|| property.equals(GRID_IS_SHOW_HORIZONTAL_GRIDLINES)
						|| property.equals(GRID_IS_SHOW_VERTICAL_GRIDLINES)
						|| property.equals(GRID_HORIZONTAL_DISTANCE)
						|| property.equals(GRID_VERTICAL_DISTANCE)
				//
				) {

					setupChartConfig();

					isChartModified = true;

				} else if (property.equals(ITourbookPreferences.TOUR_SEGMENTER_CHART_VALUE_FONT)) {

					setupSegmenterValueFont();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					isChartModified = true;
					keepMinMax = false;

					_valuePointTooltip.reopen();
				}

				final boolean isMinMaxEnabled = _prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_MIN_MAX_ENABLED);

				/*
				 * Altitude
				 */
				isChartModified |= setMinDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_ALTITUDE_IS_MIN_ENABLED,
						ITourbookPreferences.GRAPH_ALTITUDE_MIN_VALUE,
						TourManager.GRAPH_ALTITUDE,
						0,
						MIN_ADJUSTMENT,
						isMinMaxEnabled);

				isChartModified |= setMaxDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_ALTITUDE_IS_MAX_ENABLED,
						ITourbookPreferences.GRAPH_ALTITUDE_MAX_VALUE,
						TourManager.GRAPH_ALTITUDE,
						0,
						1e-2,
						isMinMaxEnabled);

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
						MIN_ADJUSTMENT,
						isMinMaxEnabled);

				isChartModified |= setMaxDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_ALTIMETER_IS_MAX_ENABLED,
						ITourbookPreferences.GRAPH_ALTIMETER_MAX_VALUE,
						TourManager.GRAPH_ALTIMETER,
						0,
						1e-2,
						isMinMaxEnabled);

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
						MIN_ADJUSTMENT,
						isMinMaxEnabled);

				isChartModified |= setMaxDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_GRADIENT_IS_MAX_ENABLED,
						ITourbookPreferences.GRAPH_GRADIENT_MAX_VALUE,
						TourManager.GRAPH_GRADIENT,
						0,
						MAX_ADJUSTMENT,
						isMinMaxEnabled);

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
						MIN_ADJUSTMENT,
						isMinMaxEnabled);

				isChartModified |= setMaxDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_PULSE_IS_MAX_ENABLED,
						ITourbookPreferences.GRAPH_PULSE_MAX_VALUE,
						TourManager.GRAPH_PULSE,
						0,
						1e-3,
						isMinMaxEnabled);

				/*
				 * Speed
				 */
				isChartModified |= setMinDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_SPEED_IS_MIN_ENABLED,
						ITourbookPreferences.GRAPH_SPEED_MIN_VALUE,
						TourManager.GRAPH_SPEED,
						0,
						Double.MIN_VALUE,
						isMinMaxEnabled);

				isChartModified |= setMaxDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_SPEED_IS_MAX_ENABLED,
						ITourbookPreferences.GRAPH_SPEED_MAX_VALUE,
						TourManager.GRAPH_SPEED,
						0,
						Double.MIN_VALUE,
						isMinMaxEnabled);

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
						Double.MIN_VALUE,
						isMinMaxEnabled);

				isChartModified |= setMaxDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_PACE_IS_MAX_ENABLED,
						ITourbookPreferences.GRAPH_PACE_MAX_VALUE,
						TourManager.GRAPH_PACE,
						60,
						Double.MIN_VALUE,
						isMinMaxEnabled);

				/*
				 * Cadence
				 */
				isChartModified |= setMinDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_CADENCE_IS_MIN_ENABLED,
						ITourbookPreferences.GRAPH_CADENCE_MIN_VALUE,
						TourManager.GRAPH_CADENCE,
						0,
						MIN_ADJUSTMENT,
						isMinMaxEnabled);

				isChartModified |= setMaxDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_CADENCE_IS_MAX_ENABLED,
						ITourbookPreferences.GRAPH_CADENCE_MAX_VALUE,
						TourManager.GRAPH_CADENCE,
						0,
						1e-3,
						isMinMaxEnabled);

				/*
				 * Power
				 */
				isChartModified |= setMinDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_POWER_IS_MIN_ENABLED,
						ITourbookPreferences.GRAPH_POWER_MIN_VALUE,
						TourManager.GRAPH_POWER,
						0,
						MIN_ADJUSTMENT,
						isMinMaxEnabled);

				isChartModified |= setMaxDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_POWER_IS_MAX_ENABLED,
						ITourbookPreferences.GRAPH_POWER_MAX_VALUE,
						TourManager.GRAPH_POWER,
						0,
						1e-3,
						isMinMaxEnabled);

				/*
				 * Temperature
				 */
				isChartModified |= setMinDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_TEMPERATURE_IS_MIN_ENABLED,
						ITourbookPreferences.GRAPH_TEMPERATURE_MIN_VALUE,
						TourManager.GRAPH_TEMPERATURE,
						0,
						MIN_ADJUSTMENT,
						isMinMaxEnabled);

				isChartModified |= setMaxDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_TEMPERATURE_IS_MAX_ENABLED,
						ITourbookPreferences.GRAPH_TEMPERATURE_MAX_VALUE,
						TourManager.GRAPH_TEMPERATURE,
						0,
						1e-3,
						isMinMaxEnabled);

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
		_actionTourChartOptions = new ActionTourChartOptions();
		_actionTourChartSmoothing = new ActionTourChartSmoothing(
				TourbookPlugin.getImageDescriptor(Messages.Image__Smoothing),
				TourbookPlugin.getImageDescriptor(Messages.Image__Smoothing_Disabled));
		_actionGraphMinMax = new ActionGraphMinMax(
				TourbookPlugin.getImageDescriptor(Messages.Image__GraphMinMax),
				TourbookPlugin.getImageDescriptor(Messages.Image__GraphMinMax_Disabled));
		_actionTourInfo = new ActionTourChartInfo(this, _parent);
		_actionTourMarker = new ActionTourChartMarker(this, _parent);

		_allTourChartActions.put(ACTION_ID_CAN_AUTO_ZOOM_TO_SLIDER, new ActionCanAutoZoomToSlider(this));
		_allTourChartActions.put(ACTION_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED, new ActionCanMoveSlidersWhenZoomed(this));
		_allTourChartActions.put(ACTION_ID_IS_SHOW_TOUR_PHOTOS, new ActionTourPhotos(this));
		_allTourChartActions.put(ACTION_ID_IS_GRAPH_OVERLAPPED, new ActionGraphOverlapped(this));
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

		final boolean isMultipleTours = _tourData.isMultipleTours();
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

			addChartMouseListener(_mouseMarkerListener);
		}

		_layerMarker.setChartMarkerConfig(cmc);
		_tourMarkerTooltip.setChartMarkerConfig(cmc);

		// set data serie for the x-axis
		final double[] xAxisSerie = _tcc.isShowTimeOnXAxis ? //
				_tourData.getTimeSerieDouble()
				: _tourData.getDistanceSerieDouble();

		if (_tourData.isMultipleTours()) {

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

				addChartMouseListener(_mousePhotoListener);
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

			// set overlay painter to paint hovered segments
			addChartOverlay(_layerTourSegmenterAltitude);
		}

		_layerTourSegmenterAltitude.setTourData(_tourData);
		_layerTourSegmenterAltitude.setIsShowDecimalPlaces(isShowDecimalPlaces);
		_layerTourSegmenterAltitude.setIsShowSegmenterMarker(isShowSegmenterMarker);
		_layerTourSegmenterAltitude.setIsShowSegmenterValue(isShowSegmenterValue);
		_layerTourSegmenterAltitude.setLineProperties(isShowSegmenterLine, lineOpacity);
		_layerTourSegmenterAltitude.setSmallHiddenValuesProperties(isHideSmallValues, smallValueSize);
		_layerTourSegmenterAltitude.setStackedValues(stackedValues);
		_layerTourSegmenterAltitude.setXDataSerie(xDataSerie);

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

	private void createSelectedLines() {

		if (_selectedSegmenterSegment_1 == null || _layerTourSegmenterAltitude == null) {

			_selectedAltitudePoints = null;
			_selectedAltitudeRGB = null;

			_selectedOtherPoints = null;
			_selectedPathsRGB = null;

			return;
		}

		/*
		 * Get segment start/end indices
		 */
		int selectedSegmentIndexStart = _selectedSegmenterSegment_1.segmentIndex;
		int selectedSegmentIndexEnd;

		if (_selectedSegmenterSegment_2 == null) {
			selectedSegmentIndexEnd = selectedSegmentIndexStart;
		} else {
			selectedSegmentIndexEnd = _selectedSegmenterSegment_2.segmentIndex;
		}

		// depending how the segments are selected in the UI, start can be larger than the end
		if (selectedSegmentIndexStart > selectedSegmentIndexEnd) {

			// swap indices
			final int tempIndex = selectedSegmentIndexEnd;
			selectedSegmentIndexEnd = selectedSegmentIndexStart;
			selectedSegmentIndexStart = tempIndex;
		}

		/*
		 * Create poline for all selected segments
		 */
		TIntArrayList selectedAltitudePath = null;
		final ArrayList<SegmenterSegment> paintedSegments_Altitude = _layerTourSegmenterAltitude.getPaintedSegments();
		final ArrayList<RGB> selectedAltitudeRGB = new ArrayList<>();

		if (paintedSegments_Altitude.size() > 0) {

			selectedAltitudePath = createSelectedLines_Values(
					paintedSegments_Altitude,
					selectedSegmentIndexStart,
					selectedSegmentIndexEnd,
					selectedAltitudeRGB);
		}
		_selectedAltitudePoints = selectedAltitudePath;
		_selectedAltitudeRGB = selectedAltitudeRGB;

		/*
		 * 
		 */
		final ArrayList<TIntArrayList> selectedOtherPaths = new ArrayList<>();
		final ArrayList<RGB> selectedPathsRGB = new ArrayList<>();
		final ArrayList<ArrayList<SegmenterSegment>> paintedSegemntsOther = _layerTourSegmenterOther
				.getPaintedSegments();

		for (final ArrayList<SegmenterSegment> paintedSegments : paintedSegemntsOther) {

			final TIntArrayList selectedPath = createSelectedLines_Values(
					paintedSegments,
					selectedSegmentIndexStart,
					selectedSegmentIndexEnd,
					selectedPathsRGB);

			selectedOtherPaths.add(selectedPath);
		}
		_selectedOtherPoints = selectedOtherPaths;
		_selectedPathsRGB = selectedPathsRGB;
	}

	private TIntArrayList createSelectedLines_Values(	final ArrayList<SegmenterSegment> paintedSegments,
														final int selectedSegmentIndexStart,
														final int selectedSegmentIndexEnd,
														final ArrayList<RGB> allValueRGBs) {

		final TIntArrayList lineSegments = new TIntArrayList();

		// check bounds
		final int allLabelSize = paintedSegments.size();

		boolean isFirstPainted = false;

		for (int labelIndex = 0; labelIndex < allLabelSize; labelIndex++) {

			final SegmenterSegment paintedSegment = paintedSegments.get(labelIndex);

			final int labelSegmentIndex = paintedSegment.segmentIndex;

			if (labelSegmentIndex > selectedSegmentIndexEnd) {
				// last segment is painted
				break;
			}

			if (isFirstPainted) {

				// create following segments

				lineSegments.add(paintedSegment.paintedX2);
				lineSegments.add(paintedSegment.paintedY2);
				allValueRGBs.add(paintedSegment.paintedRGB);

			} else if (labelSegmentIndex >= selectedSegmentIndexStart
			//
			// check if this value is valid
					&& paintedSegment.paintedX1 != Integer.MIN_VALUE
			//
			) {

				// create first segment

				isFirstPainted = true;

				lineSegments.add(paintedSegment.paintedX1);
				lineSegments.add(paintedSegment.paintedY1);
				allValueRGBs.add(paintedSegment.paintedRGB);

				lineSegments.add(paintedSegment.paintedX2);
				lineSegments.add(paintedSegment.paintedY2);
				allValueRGBs.add(paintedSegment.paintedRGB);
			}
		}

		return lineSegments;
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

	@Override
	public void drawSelectedLines(	final GC gc,
									final ArrayList<GraphDrawingData> allGraphDrawingData,
									final boolean isFocusActive) {

		if (_isRecomputeLineSelection) {

			_isRecomputeLineSelection = false;

			createSelectedLines();
		}

		final Display display = getDisplay();

		gc.setLineWidth(9);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineJoin(SWT.JOIN_ROUND);
		gc.setAntialias(SWT.ON);

// SET CLIPPING FOR EACH GRAPH, this is a bit complex
// SET CLIPPING FOR EACH GRAPH
// SET CLIPPING FOR EACH GRAPH
// SET CLIPPING FOR EACH GRAPH
// SET CLIPPING FOR EACH GRAPH
// SET CLIPPING FOR EACH GRAPH
//		final int devYTop = graphDrawingData.getDevYTop();
//		final int devGraphHeight = graphDrawingData.devGraphHeight;
//
//		gc.setClipping(0, devYTop, gc.getClipping().width, devGraphHeight);
//		gc.setClipping((Rectangle) null);

		// paint altitude line
		if (_selectedAltitudePoints != null) {

			gc.setAlpha(isFocusActive ? 0xa0 : 0x60);

			final Path path1 = new Path(display);
			final Path path2 = new Path(display);
			{
				final int[] pathPoints = _selectedAltitudePoints.toArray();

				int graphX1;
				int graphY1;

				int graphXPrev = 200;
				int graphYPrev = 200;

				RGB rgb1 = null;
				RGB rgb2 = null;
				RGB prevRGB = null;

				for (int pointIndex = 0; pointIndex < pathPoints.length;) {

					/*
					 * get segment colors
					 */
					final RGB segmentRGB = _selectedAltitudeRGB.get(pointIndex / 2);
					if (rgb1 == null) {
						rgb1 = segmentRGB;
					}
					if (rgb2 == null && rgb1 != segmentRGB) {
						rgb2 = segmentRGB;
					}

					graphX1 = pathPoints[pointIndex++];
					graphY1 = pathPoints[pointIndex++];

					if (pointIndex == 4) {

						// draw start segment

						path1.moveTo(graphXPrev, graphYPrev);
						path1.lineTo(graphX1, graphY1);

					} else if (pointIndex > 4) {

						// draw following segments

						// optimize, draw only when changed
						if (graphX1 != graphXPrev || graphY1 != graphYPrev || prevRGB != segmentRGB) {

							if (segmentRGB == prevRGB) {

								// use the same color as the previous

								if (segmentRGB == rgb1) {
									path1.lineTo(graphX1, graphY1);

								} else {
									path2.lineTo(graphX1, graphY1);
								}

							} else {

								// color has changed, paint into other path

								if (segmentRGB == rgb1) {
									path1.moveTo(graphXPrev, graphYPrev);
									path1.lineTo(graphX1, graphY1);
								} else {
									path2.moveTo(graphXPrev, graphYPrev);
									path2.lineTo(graphX1, graphY1);
								}
							}
						}
					}

					graphXPrev = graphX1;
					graphYPrev = graphY1;

					prevRGB = segmentRGB;
				}

				gc.setLineCap(SWT.CAP_ROUND);
				gc.setClipping(_layerTourSegmenterAltitude.getGraphArea());

				if (rgb1 != null) {

					final Color color1 = new Color(display, rgb1);
					{
						gc.setForeground(color1);
						gc.drawPath(path1);
					}
					color1.dispose();
				}

				if (rgb2 != null) {

					final Color color2 = new Color(display, rgb2);
					{
						gc.setForeground(color2);
						gc.drawPath(path2);
					}
					color2.dispose();
				}
			}
			path1.dispose();
			path2.dispose();
		}

		// paint paths
		if (_selectedOtherPoints != null) {

			gc.setAlpha(isFocusActive ? 0x60 : 0x30);

			final ArrayList<Rectangle> allGraphAreas = _layerTourSegmenterOther.getAllGraphAreas();

			for (int pathIndex = 0; pathIndex < _selectedOtherPoints.size(); pathIndex++) {

				final TIntArrayList graphLine = _selectedOtherPoints.get(pathIndex);
				if (graphLine.size() == 0) {
					// can be empty when small values are hidden
					continue;
				}

				final Rectangle graphArea = allGraphAreas.get(pathIndex);
				final RGB pathRGB = _selectedPathsRGB.get(pathIndex * (graphLine.size() / 2));

				final Path path = new Path(display);
				final Color pathColor = new Color(display, pathRGB);
				{
					final int[] pathPoints = graphLine.toArray();

					int graphX1;
					int graphY1;

					int graphX2 = 0;

					for (int pointIndex = 0; pointIndex < pathPoints.length;) {

						graphX1 = pathPoints[pointIndex++];
						graphY1 = pathPoints[pointIndex++];

						if (pointIndex == 4) {

							path.moveTo(graphX2, graphY1);
							path.lineTo(graphX1, graphY1);

						} else if (pointIndex > 4) {

							path.moveTo(graphX2, graphY1);
							path.lineTo(graphX1, graphY1);
						}

						graphX2 = graphX1;
					}

					gc.setForeground(pathColor);
					gc.setClipping(graphArea);
					gc.drawPath(path);
				}
				pathColor.dispose();
				path.dispose();
			}
		}

		gc.setLineCap(SWT.CAP_FLAT);
		gc.setClipping((Rectangle) null);
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
		if (_isTourChartToolbarCreated) {
			return;
		}

		_isTourChartToolbarCreated = true;

		final IToolBarManager tbm = getToolBarManager();

		/*
		 * add actions to the toolbar
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
		tbm.add(_allTourChartActions.get(ACTION_ID_X_AXIS_TIME));
		tbm.add(_allTourChartActions.get(ACTION_ID_X_AXIS_DISTANCE));

		tbm.add(new Separator());
		tbm.add(_allTourChartActions.get(ACTION_ID_IS_SHOW_TOUR_PHOTOS));
		tbm.add(_actionTourMarker);
		tbm.add(_actionTourInfo);
		tbm.add(_actionTourChartSmoothing);
		tbm.add(_actionGraphMinMax);
		tbm.add(_actionTourChartOptions);

		tbm.update(true);
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

		// update selection locally (e.g. in a dialog)

		final ArrayList<TourMarker> allTourMarker = new ArrayList<TourMarker>();
		allTourMarker.add(tourMarker);

		final SelectionTourMarker tourMarkerSelection = new SelectionTourMarker(_tourData, allTourMarker);

		final Object[] listeners = _tourMarkerSelectionListener.getListeners();
		for (final Object listener2 : listeners) {
			final ITourMarkerSelectionListener listener = (ITourMarkerSelectionListener) listener2;
			listener.selectionChanged(tourMarkerSelection);
		}

		if (_isDisplayedInDialog) {
			return;
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

	SegmenterSegment getHoveredSegmenterSegment() {
		return _hoveredSegmenterSegment;
	}

	private SegmenterSegment getHoveredSegmenterSegment(final ChartMouseEvent mouseEvent) {

		if (_layerTourSegmenterAltitude == null) {
			// this occured, it is possible that an event is fired but this layer is not yet set
			return null;
		}

		SegmenterSegment hoveredSegmenterSegment = _layerTourSegmenterAltitude.getHoveredSegment(mouseEvent);

		if (hoveredSegmenterSegment == null) {
			hoveredSegmenterSegment = _layerTourSegmenterOther.getHoveredSegment(mouseEvent);
		}

		return hoveredSegmenterSegment;
	}

	/**
	 * @param mouseEvent
	 * @return Returns the hovered title or <code>null</code> when a title is not hovered.
	 */
	private ChartTitleSegment getHoveredTitleSegment(final ChartMouseEvent mouseEvent) {

		final int devXMouse = mouseEvent.devXMouse;
		final int devYMouse = mouseEvent.devYMouse;

		final ChartDrawingData chartDrawingData = getChartDrawingData();
		final ArrayList<ChartTitleSegment> chartTitleSegments = chartDrawingData.chartTitleSegments;

		for (final ChartTitleSegment chartTitleSegment : chartTitleSegments) {

			final int devXSegment = chartTitleSegment.devXSegment;
			final int devYLabel = chartTitleSegment.devYTitle;

			if (devXMouse > devXSegment
					&& devXMouse < devXSegment + chartTitleSegment.devSegmentWidth
					&& devYMouse > 0
					&& devYMouse < devYLabel + chartTitleSegment.titleHeight) {

				return chartTitleSegment;
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

		_lastHoveredTourMarker = tourMarker;

		return tourMarker;
	}

	public TourMarker getLastHoveredTourMarker() {

		return _lastHoveredTourMarker;
	}

	ChartLayerMarker getLayerTourMarker() {

		return _layerMarker;
	}

	TourMarker getSelectedTourMarker() {
		return _selectedTourMarker;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> tourChartTours = new ArrayList<TourData>();

		if (_tourData != null) {

			TourData tourData = null;

			if (_tourData.isMultipleTours()) {

				if (_selectedTourMarker != null) {

					// get tour from hovered marker

					tourData = _selectedTourMarker.getTourData();
				}

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

	public ArrayList<TourSegment> getTourSegments() {

		final IViewPart viewPart = Util.getView(TourSegmenterView.ID);

		if (viewPart instanceof TourSegmenterView) {
			return ((TourSegmenterView) viewPart).getTourSegments();
		}

		return null;
	}

	Font getValueFont() {

		if (_segmenterValueFont == null) {
			setupSegmenterValueFont();
		}

		return _segmenterValueFont;
	}

	private ChartDataYSerie getYData(final int yDataInfoId) {

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
		return yData;
	}

	/**
	 * Disable marker layer.
	 */
	private void hideMarkerLayer() {

		if (_layerMarker != null) {

			removeChartOverlay(_layerMarker);

			_layerMarker = null;
		}

		removeChartMouseListener(_mouseMarkerListener);
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

		removeChartMouseListener(_mousePhotoListener);
	}

	private void onChart_KeyDown(final ChartKeyEvent keyEvent) {

		if (_isTourSegmenterVisible) {
			selectSegmenterSegment(keyEvent);
		}
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

		// hide tooltip otherwise it has the wrong location, disable selection
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
	private void onSegmenterSegment_MouseDown(final ChartMouseEvent mouseEvent) {

		final SegmenterSegment hoveredSegment = getHoveredSegmenterSegment(mouseEvent);

		if (hoveredSegment == null) {

			_selectedSegmenterSegment_1 = null;
			_selectedSegmenterSegment_2 = null;

			// redraw chart
			setSelectedLines(false);

			return;
		}

		// notify the chart mouse listener that no other actions should be done
		mouseEvent.isWorked = true;
		mouseEvent.cursor = ChartCursor.Arrow;
		mouseEvent.isDisableSliderDragging = true;

		final boolean isShift = (mouseEvent.stateMask & SWT.SHIFT) != 0;

		if (isShift) {

			if (_selectedSegmenterSegment_1 == null) {

				// start new selection
				_selectedSegmenterSegment_1 = hoveredSegment;
				_selectedSegmenterSegment_2 = null;

			} else {

				// extend selection
				_selectedSegmenterSegment_2 = hoveredSegment;
			}

		} else {

			// start new selection
			_selectedSegmenterSegment_1 = hoveredSegment;
			_selectedSegmenterSegment_2 = null;
		}

		// redraw chart
		_isRecomputeLineSelection = true;
		setSelectedLines(true);

		final boolean isMoveChartToShowSlider = _selectedSegmenterSegment_2 == null;

		selectSegmenterSegments(//
				_selectedSegmenterSegment_1,
				_selectedSegmenterSegment_2,
				isMoveChartToShowSlider);
	}

	private void onSegmenterSegment_MouseExit() {

		_hoveredSegmenterSegment = null;

		_tourSegmenterTooltip.hide();

		setChartOverlayDirty();
	}

	private void onSegmenterSegment_MouseMove(final ChartMouseEvent mouseEvent) {

		// ignore events with the same time
		if (mouseEvent.eventTime == _hoveredSegmenterSegmentEventTime) {

			mouseEvent.isWorked = _isSegmenterSegmentHovered;
			mouseEvent.cursor = ChartCursor.Arrow;

			return;
		}

		_hoveredSegmenterSegmentEventTime = mouseEvent.eventTime;

		final SegmenterSegment hoveredSegment = getHoveredSegmenterSegment(mouseEvent);

		_isSegmenterSegmentHovered = hoveredSegment != null;

		if (_isSegmenterSegmentHovered) {

			// set worked that no other actions are done in this event
			mouseEvent.isWorked = _isSegmenterSegmentHovered;
			mouseEvent.cursor = ChartCursor.Arrow;
		}

		boolean isUpdateUI = false;

		final SegmenterSegment prevHoveredSegment = _hoveredSegmenterSegment;

		if (hoveredSegment != _hoveredSegmenterSegment) {

			// hovered label has changed

			_hoveredSegmenterSegment = hoveredSegment;

			isUpdateUI = true;

			if (_isShowSegmenterTooltip) {

				// show/hide tooltip
				if (hoveredSegment == null) {

					_tourSegmenterTooltip.hide();

				} else {

					// close other tooltips
					_openDlgMgr.closeOpenedDialogs(_tourSegmenterTooltip);

					_tourSegmenterTooltip.open(hoveredSegment);
				}
			}

		} else if (hoveredSegment == null && prevHoveredSegment != null) {

			// hide previous tooltip when not yet hidden

			_tourSegmenterTooltip.hide();
		}

		if (isUpdateUI) {
			setChartOverlayDirty();
		}
	}

	private void onSegmenterSegment_MouseUp(final ChartMouseEvent mouseEvent) {

		final SegmenterSegment segmentSegment = getHoveredSegmenterSegment(mouseEvent);

		if (segmentSegment == null) {
			return;
		}

		final boolean isHovered = segmentSegment != null;
		if (isHovered) {

			// set marker default cursor when the mouse is still hovering a marker
			mouseEvent.isWorked = true;
			mouseEvent.cursor = ChartCursor.Arrow;
		}
	}

	private void onSegmenterSegment_Resize() {

		/*
		 * Only visible segments are painted, during a resize the selection has not changed but the
		 * visible segments -> make the selected segements visible again.
		 */
		if (_segmenterSelection != null) {
			selectXSliders_Segments(_segmenterSelection);
		}

		_hoveredSegmenterSegment = null;
		_isRecomputeLineSelection = true;

		setChartOverlayDirty();
	}

	/**
	 * Open tour quick editor.
	 * 
	 * @param mouseEvent
	 */
	private void onSegmentTitle_MouseDoubleClick(final ChartMouseEvent mouseEvent) {

		final ChartTitleSegment chartTitleSegment = getHoveredTitleSegment(mouseEvent);

		_chartTitleSegment = chartTitleSegment;

		if (chartTitleSegment == null) {
			return;
		}

		// title is hovered and double clicked, quick edit the tour
		mouseEvent.isWorked = _isSegmentTitleHovered;

		_actionEditQuick.run();
	}

	private void onSegmentTitle_MouseDown(final ChartMouseEvent mouseEvent) {

		final ChartTitleSegment chartTitleSegment = getHoveredTitleSegment(mouseEvent);

		_chartTitleSegment = chartTitleSegment;

		if (chartTitleSegment == null) {
			return;
		}

		// title is hovered and clicked, select tour and fire tour selection
		mouseEvent.isWorked = selectTour(chartTitleSegment);
	}

	private void onSegmentTitle_MouseExit() {

		_chartTitleSegment = null;

		setHoveredTitleSegment(null);

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

		final ChartTitleSegment chartTitleSegment = getHoveredTitleSegment(mouseEvent);

		_isSegmentTitleHovered = chartTitleSegment != null;

		if (_isSegmentTitleHovered) {

			// set worked that no other actions are done in this event
			mouseEvent.isWorked = _isSegmentTitleHovered;
			mouseEvent.cursor = ChartCursor.Arrow;
		}

		final ChartTitleSegment prevHoveredPart = _chartTitleSegment;

		if (_chartTitleSegment != chartTitleSegment) {

			// hovered title has changed, show or hide tooltip

			// update internal state
			_chartTitleSegment = chartTitleSegment;

			// update state in the chart
			setHoveredTitleSegment(chartTitleSegment);

			if (_tcc.isShowInfoTooltip) {

				// show/hide tooltip
				if (chartTitleSegment == null) {

					_tourTitleTooltip.hide();

				} else {

					// close other tooltips
					_openDlgMgr.closeOpenedDialogs(_tourTitleTooltip);

					_tourTitleTooltip.open(chartTitleSegment);
				}
			}

		} else if (chartTitleSegment == null && prevHoveredPart != null) {

			// hide tooltip when not yet hidden

			_tourTitleTooltip.hide();
		}
	}

	private void onSegmentTitle_Resized() {

		_chartTitleSegment = null;

		setHoveredTitleSegment(null);

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

	/**
	 * Reset tour segmenter selection.
	 */
	private void resetSegmenterSelection() {

		final int[] tourSegments = _tourData.segmentSerieIndex;

		int tourSegmentHash = Integer.MIN_VALUE;
		if (tourSegments != null) {
			tourSegmentHash = tourSegments.hashCode();
		}

		if (tourSegments == null || tourSegmentHash != _oldTourSegmentsHash) {

			// reset selection when tour or segments have changed

			/*
			 * I'm not shure if this condition is sufficient to disable the segmenter selection but
			 * the selection should be kept as long as possible.
			 */

			_selectedSegmenterSegment_1 = null;
			_selectedSegmenterSegment_2 = null;
			_segmenterSelection = null;
		}

		_oldTourSegmentsHash = tourSegmentHash;

		setLineSelectionDirty();
	}

	void restoreState() {

		_photoTooltip.restoreState();
	}

	void saveState() {

		_photoTooltip.saveState();
		_valuePointTooltip.saveState();
	}

	private void selectSegmenterSegment(final ChartKeyEvent keyEvent) {

		final int[] segmentSerieIndex = _tourData.segmentSerieIndex;
		final int numSegments = segmentSerieIndex.length;

		int segmentIndex1 = -1;
		int segmentIndex2 = -1;

		final boolean isCtrlKey = (keyEvent.stateMask & SWT.MOD1) > 0;
		final boolean isShiftKey = (keyEvent.stateMask & SWT.MOD2) > 0;

		boolean isLeftKey = false;
		boolean isHomeKey = false;
		boolean isEndKey = false;

		int navigationSteps = 1;

		switch (keyEvent.keyCode) {
		case SWT.PAGE_DOWN:
			navigationSteps = PAGE_NAVIGATION_SEGMENTS;
			break;

		case SWT.ARROW_RIGHT:
			// support this key
			break;

		case SWT.PAGE_UP:
			navigationSteps = PAGE_NAVIGATION_SEGMENTS;
			isLeftKey = true;
			break;

		case SWT.ARROW_LEFT:
			isLeftKey = true;
			break;

		case SWT.HOME:
			isHomeKey = true;
			break;

		case SWT.END:
			isEndKey = true;
			break;

		default:
			// nothing can be navigated, do other actions
			return;
		}

		boolean isNavigated = false;
		final int lastSegmentIndex = numSegments - 1;

		if (_selectedSegmenterSegment_1 == null || isHomeKey || isEndKey) {

			// nothing is selected, start from the right or left border

			if (isLeftKey || isEndKey) {

				// navigate to the left, start navigation from the right border

				segmentIndex1 = lastSegmentIndex;

			} else {

				// navigate to the right, start navigation from the left border

				segmentIndex1 = 1;
			}

			isNavigated = true;

		} else {

			// at least one segment is selected

			int selectedSegmentIndex1 = _selectedSegmenterSegment_1.segmentIndex;
			int selectedSegmentIndex2 = -1;

			if (_selectedSegmenterSegment_2 != null) {
				selectedSegmentIndex2 = _selectedSegmenterSegment_2.segmentIndex;

				// swap index because with the mouse the left segment can be segment2 depending how they are selected

				if (selectedSegmentIndex2 < selectedSegmentIndex1) {

					final int tempIndex1 = selectedSegmentIndex1;
					selectedSegmentIndex1 = selectedSegmentIndex2;
					selectedSegmentIndex2 = tempIndex1;
				}
			}

			if (isShiftKey) {

				// select multiple segments

				if (_selectedSegmenterSegment_2 == null) {

					// start multiple selection

					if (isLeftKey) {

						// expand to the left

						if (selectedSegmentIndex1 > 1) {

							// expand to the next left segment

							segmentIndex1 = Math.max(1, selectedSegmentIndex1 - navigationSteps);
							segmentIndex2 = selectedSegmentIndex1;
						}

					} else {

						// expand to the right

						if (selectedSegmentIndex1 < lastSegmentIndex) {

							// expand to the next left segment

							segmentIndex1 = selectedSegmentIndex1;
							segmentIndex2 = Math.min(lastSegmentIndex, selectedSegmentIndex1 + navigationSteps);
						}
					}

				} else {

					// expand multiple selection

					if (isLeftKey) {

						if (isCtrlKey) {

							// reduce selection from the right border to the left

							if (selectedSegmentIndex2 - selectedSegmentIndex1 > 0) {

								segmentIndex1 = selectedSegmentIndex1;
								segmentIndex2 = selectedSegmentIndex2 - 1;
							}

						} else {

							// expand to the left

							if (selectedSegmentIndex1 > 1) {

								// expand to the next left segment

								segmentIndex1 = Math.max(1, selectedSegmentIndex1 - navigationSteps);
								segmentIndex2 = selectedSegmentIndex2;
							}
						}

					} else {

						if (isCtrlKey) {

							// reduce selection from the left border to the right

							if (selectedSegmentIndex2 - selectedSegmentIndex1 > 0) {

								segmentIndex1 = selectedSegmentIndex1 + 1;
								segmentIndex2 = selectedSegmentIndex2;
							}

						} else {

							// expand to the right

							if (selectedSegmentIndex2 < lastSegmentIndex) {

								// expand to the next right segment

								segmentIndex1 = selectedSegmentIndex1;
								segmentIndex2 = Math.min(lastSegmentIndex, selectedSegmentIndex2 + navigationSteps);
							}
						}
					}
				}

				if (segmentIndex2 != -1) {

					// 2nd index is set
					isNavigated = true;
				}

				if (segmentIndex1 == segmentIndex2) {
					// this case can occure when selection is reduced -> disable multiple selections
					segmentIndex2 = -1;
				}

			} else {

				// select a SINGLE segment

				if (isLeftKey) {

					// navigate to the left

					if (selectedSegmentIndex2 == -1) {

						// there is no multiple selection
						if (selectedSegmentIndex1 > 1) {

							// select previous segment
							segmentIndex1 = Math.max(1, selectedSegmentIndex1 - navigationSteps);

						} else {

							// start navigation from the right border
							segmentIndex1 = lastSegmentIndex;
						}

					} else {

						// there is multiple selection, navigate to the left segment

						segmentIndex1 = selectedSegmentIndex1;
					}

				} else {

					// navigate to the right

					if (selectedSegmentIndex2 == -1) {

						// there is no multiple selection

						if (selectedSegmentIndex1 < lastSegmentIndex) {

							// select following segment
							segmentIndex1 = Math.min(lastSegmentIndex, selectedSegmentIndex1 + navigationSteps);

						} else {

							// start navigation from the left border
							segmentIndex1 = 1;
						}

					} else {

						// there is multiple selection, navigate to the right segment

						segmentIndex1 = selectedSegmentIndex2;
					}
				}

				isNavigated = true;
			}
		}

		if (isNavigated) {

			_selectedSegmenterSegment_1 = new SegmenterSegment();

			_selectedSegmenterSegment_1.segmentIndex = segmentIndex1;
			_selectedSegmenterSegment_1.xSliderSerieIndexLeft = segmentSerieIndex[segmentIndex1 - 1];
			_selectedSegmenterSegment_1.xSliderSerieIndexRight = segmentSerieIndex[segmentIndex1];

			if (segmentIndex2 == -1) {

				// disable multiple selections
				_selectedSegmenterSegment_2 = null;

			} else {

				// setup multiple selection

				_selectedSegmenterSegment_2 = new SegmenterSegment();

				_selectedSegmenterSegment_2.segmentIndex = segmentIndex2;
				_selectedSegmenterSegment_2.xSliderSerieIndexLeft = segmentSerieIndex[segmentIndex2 - 1];
				_selectedSegmenterSegment_2.xSliderSerieIndexRight = segmentSerieIndex[segmentIndex2];
			}

			// redraw chart
			_isRecomputeLineSelection = true;
			setSelectedLines(true);

			final boolean isMoveChartToShowSlider = _selectedSegmenterSegment_2 == null;

			selectSegmenterSegments(//
					_selectedSegmenterSegment_1,
					_selectedSegmenterSegment_2,
					isMoveChartToShowSlider);
		}

		// prevent other actions in the chart even when a navigation cannot be done
		keyEvent.isWorked = true;

		/*
		 * Hide segmenter tooltip, I tried to open the tooltip but this needs some more work to get
		 * it running, THIS IS NOT A SIMPLE TASK.
		 */
		_tourSegmenterTooltip.hide();
	}

	/**
	 * Set sliders to the selected segments and fire these positions.
	 * 
	 * @param selectedSegment_1
	 * @param selectedSegment_2
	 *            Can be <code>null</code> when only 1 segment is selected.
	 * @param isMoveChartToShowSlider
	 */
	private void selectSegmenterSegments(	final SegmenterSegment selectedSegment_1,
											final SegmenterSegment selectedSegment_2,
											final boolean isMoveChartToShowSlider) {

		// prevent selection from previous selection event
		_segmenterSelection = null;

		// get start/end index depending which segments are selected
		SegmenterSegment startSegment = selectedSegment_1;
		SegmenterSegment endSegment;
		if (selectedSegment_2 == null) {
			endSegment = startSegment;
		} else {
			endSegment = selectedSegment_2;
		}

		// depending how the segments are selected, start can be larger than the end
		if (startSegment.segmentIndex > endSegment.segmentIndex) {

			// switch segments
			final SegmenterSegment tempSegment = endSegment;
			endSegment = startSegment;
			startSegment = tempSegment;
		}

		final int xSliderSerieIndexLeft = startSegment.xSliderSerieIndexLeft;
		final int xSliderSerieIndexRight = endSegment.xSliderSerieIndexRight;

		final SelectionChartXSliderPosition selectionSliderPosition = new SelectionChartXSliderPosition(
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
		selectionSliderPosition.setMoveChartToShowSlider(isMoveChartToShowSlider);
		selectionSliderPosition.setCenterZoomPositionWithKey(true);

		/*
		 * Set x slider position in the chart but do not fire an event because the event would be
		 * fired separately for each slider :-(
		 */
		setXSliderPosition(selectionSliderPosition, false);

		// fire event for both x sliders
		TourManager.fireEventWithCustomData(//
				TourEventId.SLIDER_POSITION_CHANGED,
				selectionSliderPosition,
				_part);
	}

	private boolean selectTour(final ChartTitleSegment selectedTitleSegment) {

		// exclude which is currently not yet supported
//		if (!_tourData.isMultipleTours || !_isTourSegmenterVisible) {
//			return false;
//		}
//
//		final long titleTourId = selectedTitleSegment.getTourId();
//
//		final int[] segmentSerieIndex = _tourData.segmentSerieIndex;
//
//		if (_layerTourSegmenterAltitude != null) {
//
//			final Long[] multipleTourIds = _tourData.multipleTourIds;
//
//			for (int tourIndex = 0; tourIndex < multipleTourIds.length; tourIndex++) {
//
//				final Long tourId = multipleTourIds[tourIndex];
//
//				if (titleTourId == tourId) {
//
//					final int tourStartIndex = _tourData.multipleTourStartIndex[tourIndex];
//				}
//			}
//
//		}
//
//		final int leftSerieIndex = selectedSegments.xSliderSerieIndexLeft;
//		final int rightSerieIndex = selectedSegments.xSliderSerieIndexRight;
//
//		_selectedSegmenterSegment_1 = null;
//		_selectedSegmenterSegment_2 = null;
//
//		for (final SegmenterSegment paintedLabel : paintedLabelsAltitude) {
//
//			if (_selectedSegmenterSegment_1 == null && paintedLabel.serieIndex > leftSerieIndex) {
//				_selectedSegmenterSegment_1 = paintedLabel;
//			}
//
//			if (_selectedSegmenterSegment_2 == null && paintedLabel.serieIndex >= rightSerieIndex) {
//				_selectedSegmenterSegment_2 = paintedLabel;
//			}
//
//			if (_selectedSegmenterSegment_1 != null && _selectedSegmenterSegment_2 != null) {
//				break;
//			}
//		}
//
//		// redraw chart
//		setSelectedLines(true);
//		_isRecomputeLineSelection = true;

		return true;
	}

	void selectXSliders(final SelectionChartXSliderPosition xSliderPosition) {

		// set position for the x-sliders
		setXSliderPosition(xSliderPosition);

		final Object customData = xSliderPosition.getCustomData();
		if (customData instanceof SelectedTourSegmenterSegments) {

			final SelectedTourSegmenterSegments selectedSegments = (SelectedTourSegmenterSegments) customData;

			// select segments

			if (_isTourSegmenterVisible) {
				selectXSliders_Segments(selectedSegments);
			}

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

	private void selectXSliders_Segments(final SelectedTourSegmenterSegments selectedSegments) {

		if (_layerTourSegmenterAltitude == null) {
			// is not fully initialized
			return;
		}

		// keep selection which is needed when the chart is resized and more/less/other segments are displayed
		_segmenterSelection = selectedSegments;

		_selectedSegmenterSegment_1 = null;
		_selectedSegmenterSegment_2 = null;

		ArrayList<SegmenterSegment> paintedSegmentLabels = _layerTourSegmenterAltitude.getPaintedSegments();
		if (paintedSegmentLabels.size() == 0) {

			for (final ArrayList<SegmenterSegment> paintedLabels : _layerTourSegmenterOther.getPaintedSegments()) {
				if (paintedLabels.size() > 0) {

					paintedSegmentLabels = paintedLabels;
					break;
				}
			}
		}

		if (paintedSegmentLabels.size() == 0) {
			// this case do propably not occure;
			return;
		}

		final int leftSerieIndex = selectedSegments.xSliderSerieIndexLeft;
		final int rightSerieIndex = selectedSegments.xSliderSerieIndexRight;

		for (final SegmenterSegment segmentSegment : paintedSegmentLabels) {

			if (_selectedSegmenterSegment_1 == null && segmentSegment.serieIndex > leftSerieIndex) {
				_selectedSegmenterSegment_1 = segmentSegment;
			}

			if (_selectedSegmenterSegment_2 == null && segmentSegment.serieIndex >= rightSerieIndex) {
				_selectedSegmenterSegment_2 = segmentSegment;
			}

			if (_selectedSegmenterSegment_1 != null && _selectedSegmenterSegment_2 != null) {

				/*
				 * Prevent that both segments have the same index, the 2nd segment is used for
				 * multiple selection, otherwise the selection with the mouse is a bit strange.
				 */
				if (_selectedSegmenterSegment_1.serieIndex == _selectedSegmenterSegment_2.serieIndex) {
					_selectedSegmenterSegment_2 = null;
//				} else {
				}

				break;
			}
		}

		// redraw chart
		_isRecomputeLineSelection = true;
		setSelectedLines(true);
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

	public void setCanShowTourSegments(final boolean canShowTourSegments) {
		_canShowTourSegments = canShowTourSegments;
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
	 * Set line selection dirty that it is recomputed the next time when painted.
	 */
	void setLineSelectionDirty() {
		_isRecomputeLineSelection = true;
	}

	/**
	 * @param property
	 * @param isChartModified
	 * @param prefName_IsMaxEnabled
	 * @param prefName_MaxValue
	 * @param yDataInfoId
	 * @param valueDivisor
	 * @param maxAdjustment
	 *            Is disabled when set to {@link Double#MIN_VALUE}.
	 * @param isMinMaxEnabled
	 * @return
	 */
	private boolean setMaxDefaultValue(	final String property,
										boolean isChartModified,
										final String prefName_IsMaxEnabled,
										final String prefName_MaxValue,
										final int yDataInfoId,
										final int valueDivisor,
										final double maxAdjustment,
										final boolean isMinMaxEnabled) {

		boolean isAllMinMaxModified = false;

		if (property.equals(ITourbookPreferences.GRAPH_IS_MIN_MAX_ENABLED)) {

			// global min/max value is set

			isAllMinMaxModified = true;
		}

		if (isMinMaxEnabled) {

			if (isAllMinMaxModified || property.equals(prefName_IsMaxEnabled) || property.equals(prefName_MaxValue)) {

				final ChartDataYSerie yData = getYData(yDataInfoId);

				if (yData != null) {

					final boolean isMaxEnabled = _prefStore.getBoolean(prefName_IsMaxEnabled);

					if (isMinMaxEnabled && isMaxEnabled) {

						// set visible max value from the preferences

						double maxValue = _prefStore.getInt(prefName_MaxValue);

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

		} else {

			// min/max is disabled, use default min value

			final ChartDataYSerie yData = getYData(yDataInfoId);
			if (yData != null) {

				// reset visible max value to the original max value
				yData.setVisibleMaxValue(yData.getOriginalMaxValue());

				isChartModified = true;
			}
		}

		return isChartModified;
	}

	/**
	 * @param property
	 * @param isChartModified
	 * @param prefName_IsMinEnabled
	 * @param prefName_MinValue
	 * @param yDataInfoId
	 * @param valueDivisor
	 * @param minAdjustment
	 *            Is disabled when set to {@link Double#MIN_VALUE}.
	 * @param isMinMaxEnabled
	 * @return
	 */
	private boolean setMinDefaultValue(	final String property,
										boolean isChartModified,
										final String prefName_IsMinEnabled,
										final String prefName_MinValue,
										final int yDataInfoId,
										final int valueDivisor,
										final double minAdjustment,
										final boolean isMinMaxEnabled) {

		boolean isAllMinMaxModified = false;

		if (property.equals(ITourbookPreferences.GRAPH_IS_MIN_MAX_ENABLED)) {

			// global min/max value is set

			isAllMinMaxModified = true;
		}

		if (isMinMaxEnabled) {

			if (isAllMinMaxModified || property.equals(prefName_IsMinEnabled) || property.equals(prefName_MinValue)) {

				final ChartDataYSerie yData = getYData(yDataInfoId);
				if (yData != null) {

					final boolean isMinEnabled = _prefStore.getBoolean(prefName_IsMinEnabled);

					if (isMinMaxEnabled && isMinEnabled) {

						// set visible min value from the preferences
						double minValue = _prefStore.getInt(prefName_MinValue);

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

		} else {

			// min/max is disabled, use default min value

			final ChartDataYSerie yData = getYData(yDataInfoId);
			if (yData != null) {

				// reset visible min value to the original min value

				yData.setVisibleMinValue(yData.getOriginalMinValue());

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

	void setupChartConfig() {

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

		isShowHorizontalGridLines = Util.getPrefixPrefBoolean(
				_prefStore,
				GRID_PREF_PREFIX,
				ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES);

		isShowVerticalGridLines = Util.getPrefixPrefBoolean(
				_prefStore,
				GRID_PREF_PREFIX,
				ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES);

		gridVerticalDistance = Util.getPrefixPrefInt(
				_prefStore,
				GRID_PREF_PREFIX,
				ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE);

		gridHorizontalDistance = Util.getPrefixPrefInt(
				_prefStore,
				GRID_PREF_PREFIX,
				ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE);
	}

	private void setupChartSegmentTitle() {

		final ChartTitleSegmentConfig ctsConfig = getChartTitleSegmentConfig();

		if (_tcc.isTourInfoVisible) {

			// show tour info
			addChartMouseListener(_mouseSegmentTitle_Listener);
			addChartMouseMoveListener(_mouseSegmentTitle_MoveListener);

			ctsConfig.isShowSegmentBackground = true;
			ctsConfig.isShowSegmentSeparator = _tcc.isShowInfoTourSeparator;
			ctsConfig.isShowSegmentTitle = _tcc.isShowInfoTitle;

		} else {

			// hide tour info
			removeChartMouseListener(_mouseSegmentTitle_Listener);
			removeChartMouseMoveListener(_mouseSegmentTitle_MoveListener);

			ctsConfig.isShowSegmentBackground = false;
			ctsConfig.isShowSegmentSeparator = false;
			ctsConfig.isShowSegmentTitle = false;
		}

		ctsConfig.isMultipleSegments = _tourData.isMultipleTours();

		_tourTitleTooltip.setFadeInDelayTime(_tcc.tourInfoTooltipDelay);
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

		ConfigGraphSegment cfgAltitude = null;
		ConfigGraphSegment cfgPulse = null;
		ConfigGraphSegment cfgSpeed = null;
		ConfigGraphSegment cfgPace = null;
		ConfigGraphSegment cfgPower = null;
		ConfigGraphSegment cfgGradient = null;
		ConfigGraphSegment cfgAltimeter = null;
		ConfigGraphSegment cfgCadence = null;

		/*
		 * Setup tour segmenter data
		 */
		if (_isTourSegmenterVisible) {

			final IValueLabelProvider labelProviderInt = TourManager.getLabelProviderInt();
			final IValueLabelProvider labelProviderMMSS = TourManager.getLabelProviderMMSS();

			cfgAltitude = new ConfigGraphSegment(GraphColorManager.PREF_GRAPH_ALTITUDE);
			cfgAltitude.segmentDataSerie = _tourData.segmentSerie_Altitude_Diff;
			cfgAltitude.labelProvider = labelProviderInt;
			cfgAltitude.canHaveNegativeValues = true;
			cfgAltitude.minValueAdjustment = 0.1;

			cfgPulse = new ConfigGraphSegment(GraphColorManager.PREF_GRAPH_HEARTBEAT);
			cfgPulse.segmentDataSerie = _tourData.segmentSerie_Pulse;
			cfgPulse.labelProvider = null;
			cfgPulse.canHaveNegativeValues = false;
			cfgPulse.minValueAdjustment = Double.MIN_VALUE;

			cfgSpeed = new ConfigGraphSegment(GraphColorManager.PREF_GRAPH_SPEED);
			cfgSpeed.segmentDataSerie = _tourData.segmentSerie_Speed;
			cfgSpeed.labelProvider = null;
			cfgSpeed.canHaveNegativeValues = false;
			cfgSpeed.minValueAdjustment = Double.MIN_VALUE;

			cfgPace = new ConfigGraphSegment(GraphColorManager.PREF_GRAPH_PACE);
			cfgPace.segmentDataSerie = _tourData.segmentSerie_Pace;
			cfgPace.labelProvider = labelProviderMMSS;
			cfgPace.canHaveNegativeValues = false;
			cfgPace.minValueAdjustment = Double.MIN_VALUE;

			cfgPower = new ConfigGraphSegment(GraphColorManager.PREF_GRAPH_POWER);
			cfgPower.segmentDataSerie = _tourData.segmentSerie_Power;
			cfgPower.labelProvider = labelProviderInt;
			cfgPower.canHaveNegativeValues = false;
			cfgPower.minValueAdjustment = 1.0;

			cfgGradient = new ConfigGraphSegment(GraphColorManager.PREF_GRAPH_GRADIENT);
			cfgGradient.segmentDataSerie = _tourData.segmentSerie_Gradient;
			cfgGradient.labelProvider = null;
			cfgGradient.canHaveNegativeValues = true;
			cfgGradient.minValueAdjustment = 1.6;

			cfgAltimeter = new ConfigGraphSegment(GraphColorManager.PREF_GRAPH_ALTIMETER);
			cfgAltimeter.segmentDataSerie = _tourData.segmentSerie_Altitude_UpDown_Hour;
			cfgAltimeter.labelProvider = labelProviderInt;
			cfgAltimeter.canHaveNegativeValues = true;
			cfgAltimeter.minValueAdjustment = 1.0;

			cfgCadence = new ConfigGraphSegment(GraphColorManager.PREF_GRAPH_CADENCE);
			cfgCadence.segmentDataSerie = _tourData.segmentSerie_Cadence;
			cfgCadence.labelProvider = null;
			cfgCadence.canHaveNegativeValues = false;
			cfgCadence.minValueAdjustment = Double.MIN_VALUE;
		}

		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_ALTIMETER, yDataWithLabels, cfgAltimeter);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_ALTITUDE, yDataWithLabels, cfgAltitude);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_CADENCE, yDataWithLabels, cfgCadence);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_GEAR_RATIO, yDataWithLabels, null);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_GRADIENT, yDataWithLabels, cfgGradient);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_HISTORY, null, null);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_PULSE, yDataWithLabels, cfgPulse);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_SPEED, yDataWithLabels, cfgSpeed);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_PACE, yDataWithLabels, cfgPace);
		setupGraphLayer_Layer(TourManager.CUSTOM_DATA_POWER, yDataWithLabels, cfgPower);
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

		_isShowSegmenterTooltip = Util.getStateBoolean(
				_tourSegmenterState,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_TOOLTIP,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_TOOLTIP_DEFAULT);

		_isTourSegmenterVisible = _canShowTourSegments && isSegmenterActive && isShowTourSegments;

		if (_isTourSegmenterVisible) {

			addChartMouseListener(_mouseSegmentLabel_Listener);
			addChartMouseMoveListener(_mouseSegmentLabel_MoveListener);
			addChartKeyListener(_chartKeyListener);

		} else {

			if (_layerTourSegmenterAltitude != null) {

				// disable segment layers
				removeChartOverlay(_layerTourSegmenterAltitude);

				_layerTourSegmenterAltitude = null;
				_layerTourSegmenterOther = null;
			}

			removeChartMouseListener(_mouseSegmentLabel_Listener);
			removeChartMouseMoveListener(_mouseSegmentLabel_MoveListener);
			removeChartKeyListener(_chartKeyListener);
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
		 * Overlapped graphs
		 */
		tourAction = _allTourChartActions.get(ACTION_ID_IS_GRAPH_OVERLAPPED);
		tourAction.setEnabled(true);
		tourAction.setChecked(_tcc.isGraphOverlapped);

		/*
		 * x-axis time/distance
		 */
		final boolean isShowTimeOnXAxis = _tcc.isShowTimeOnXAxis;

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

		/*
		 * Cleanup old data
		 */
		_selectedTourMarker = null;
		hidePhotoLayer();
		resetSegmenterSelection();

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
		setupChartSegmentTitle();

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
		_tourMarkerTooltip.setIsShowMarkerActions(_tourData.isMultipleTours() == false);
	}

	/**
	 * Toursegmenter is modified, update its layers.
	 * 
	 * @param tourSegmenterView
	 */
	public void updateTourSegmenter() {

		if (_tourData == null) {
			return;
		}

		resetSegmenterSelection();
		setupTourSegmenter();

		if (_isTourSegmenterVisible) {

			// tour segmenter is visible

			createLayer_TourSegmenter();

		} else {

			// tour segmenter is hidden

			setSelectedLines(false);
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

		// hide hovered segment
// this is NOT  working
// 		onSegmenterSegment_MouseExit();

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

		setupChartSegmentTitle();

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
