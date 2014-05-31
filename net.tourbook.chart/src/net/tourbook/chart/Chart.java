/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.chart;

import java.util.HashMap;

import net.tourbook.common.form.ViewForm;
import net.tourbook.common.util.ITourToolTipProvider;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Chart widget
 */
public class Chart extends ViewForm {

	private static final String		ACTION_ID_MOUSE_MODE				= "ACTION_ID_MOUSE_MODE";				//$NON-NLS-1$
	private static final String		ACTION_ID_MOVE_LEFT_SLIDER_HERE		= "ACTION_ID_MOVE_LEFT_SLIDER_HERE";	//$NON-NLS-1$
	private static final String		ACTION_ID_MOVE_RIGHT_SLIDER_HERE	= "ACTION_ID_MOVE_RIGHT_SLIDER_HERE";	//$NON-NLS-1$
	private static final String		ACTION_ID_MOVE_SLIDERS_TO_BORDER	= "ACTION_ID_MOVE_SLIDERS_TO_BORDER";	//$NON-NLS-1$
	private static final String		ACTION_ID_ZOOM_FIT_GRAPH			= "ACTION_ID_ZOOM_FIT_GRAPH";			//$NON-NLS-1$
	private static final String		ACTION_ID_ZOOM_IN					= "ACTION_ID_ZOOM_IN";					//$NON-NLS-1$
	private static final String		ACTION_ID_ZOOM_IN_TO_SLIDER			= "ACTION_ID_ZOOM_IN_TO_SLIDER";		//$NON-NLS-1$
	private static final String		ACTION_ID_ZOOM_OUT					= "ACTION_ID_ZOOM_OUT";				//$NON-NLS-1$

	static final int				NO_BAR_SELECTION					= -1;

	public static final int			SYNCH_MODE_NO						= 0;
	public static final int			SYNCH_MODE_BY_SCALE					= 1;
	public static final int			SYNCH_MODE_BY_SIZE					= 2;

	public static final String		MOUSE_MODE_SLIDER					= "slider";							//$NON-NLS-1$
	public static final String		MOUSE_MODE_ZOOM						= "zoom";								//$NON-NLS-1$

	private static final int		MouseMove							= 10;
	private static final int		MouseDownPre						= 20;
//	private static final int		MouseDownPost						= 21;
	private static final int		MouseUp								= 30;
	private static final int		MouseDoubleClick					= 40;
	private static final int		MouseExit							= 50;
	private static final int		ChartResized						= 999;

	private final ListenerList		_focusListeners						= new ListenerList();
	private final ListenerList		_barSelectionListeners				= new ListenerList();
	private final ListenerList		_barDoubleClickListeners			= new ListenerList();
	private final ListenerList		_sliderMoveListeners				= new ListenerList();
	private final ListenerList		_mouseChartListener					= new ListenerList();
	private final ListenerList		_chartOverlayListener				= new ListenerList();

	private ChartComponents			_chartComponents;

	private Chart					_synchedChart;

	private ChartDataModel			_chartDataModel;

	private IToolBarManager			_toolbarMgr;
	private IChartContextProvider	_chartContextProvider;
	private boolean					_isShowZoomActions					= false;

	private boolean					_isShowMouseMode					= false;
	private Color					_backgroundColor;

	/**
	 * listener which is called when the x-marker was dragged
	 */
	IChartListener					_draggingListenerXMarker;

	IHoveredValueListener			_hoveredListener;

	private HashMap<String, Action>	_allChartActions;
	private boolean					_isFillToolbar						= true;
	private boolean					_isToolbarCreated;

	private int						_barSelectionSerieIndex;
	private int						_barSelectionValueIndex;

	int								_synchMode;

	/**
	 * <code>true</code> to start the bar chart at the bottom of the chart
	 */
	private boolean					_isDrawBarChartAtBottom				= true;

	/**
	 * minimum width in pixel for one unit, this is only an approximate value because the pixel is
	 * rounded up or down to fit a rounded unit
	 */
	protected int					gridVerticalDistance				= 30;
	protected int					gridHorizontalDistance				= 70;

	protected boolean				isShowHorizontalGridLines			= false;
	protected boolean				isShowVerticalGridLines				= false;

	/**
	 * Transparency of the graph lines
	 */
	protected int					graphTransparencyLine				= 0xFF;

	/**
	 * Transparency of the graph fillings
	 */
	protected int					graphTransparencyFilling			= 0xE0;

	/**
	 * The graph transparency can be adjusted with this value. This value is multiplied with the
	 * {@link #graphTransparencyFilling} and {@link #graphTransparencyLine}.
	 */
	double							graphTransparencyAdjustment			= 1.0;

	/**
	 * Antialiasing for the graph, can be {@link SWT#ON} or {@link SWT#OFF}.
	 */
	protected int					graphAntialiasing					= SWT.OFF;

	/**
	 * mouse behaviour:<br>
	 * <br>
	 * {@link #MOUSE_MODE_SLIDER} or {@link #MOUSE_MODE_ZOOM}
	 */
	private String					_mouseMode							= MOUSE_MODE_SLIDER;

	private boolean					_isTopMenuPosition;

	/**
	 * Chart widget
	 */
	public Chart(final Composite parent, int style) {

		// remove border from the inner chart but set the border around the whole chart (with toolbar) when requested

		super(parent, removeBorder(style));

		if ((style & SWT.BORDER) != 0) {

			style = (style & ~SWT.BORDER);

			setBorderVisible(true);
		}

		//		setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		final GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.verticalSpacing = 0;
		setLayout(gl);

		// set the layout for the chart
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		_chartComponents = new ChartComponents(this, style);
		setContent(_chartComponents);

		// set the default background color
		_backgroundColor = getDisplay().getSystemColor(SWT.COLOR_WHITE);
	}

	private static int removeBorder(final int style) {

		if ((style & SWT.BORDER) != 0) {

			// remove border from style
			return (style & ~SWT.BORDER);
		}

		return style;
	}

	public void addBarSelectionListener(final IBarSelectionListener listener) {
		_barSelectionListeners.add(listener);
	}

	public void addChartOverlay(final IChartOverlay chartOverlay) {
		_chartOverlayListener.add(chartOverlay);
	}

	public void addDoubleClickListener(final IBarSelectionListener listener) {
		_barDoubleClickListeners.add(listener);
	}

	public void addFocusListener(final Listener listener) {
		_focusListeners.add(listener);
	}

	public void addMouseChartListener(final IMouseListener mouseListener) {
		_mouseChartListener.add(mouseListener);
	}

	/**
	 * Adds a listener when the vertical slider is moved
	 * 
	 * @param listener
	 */
	public void addSliderMoveListener(final ISliderMoveListener listener) {
		_sliderMoveListeners.add(listener);
	}

	public void addXMarkerDraggingListener(final IChartListener xMarkerDraggingListener) {
		_draggingListenerXMarker = xMarkerDraggingListener;
	}

	/**
	 * create zoom/navigation actions which are managed by the chart
	 */
	private void createActions() {

		createActions_10_ChartActions();

		if (_isFillToolbar && _isToolbarCreated == false) {
			_isToolbarCreated = true;
			fillToolbar(true);
		}
	}

	/**
	 * Creates all chart actions
	 */
	private void createActions_10_ChartActions() {

		// create actions only once
		if (_allChartActions != null) {
			return;
		}

		_allChartActions = new HashMap<String, Action>();

		_allChartActions.put(ACTION_ID_MOUSE_MODE, new ActionMouseMode(this));
		_allChartActions.put(ACTION_ID_MOVE_LEFT_SLIDER_HERE, new ActionMoveLeftSliderHere(this));
		_allChartActions.put(ACTION_ID_MOVE_RIGHT_SLIDER_HERE, new ActionMoveRightSliderHere(this));
		_allChartActions.put(ACTION_ID_MOVE_SLIDERS_TO_BORDER, new ActionMoveSlidersToBorder(this));
		_allChartActions.put(ACTION_ID_ZOOM_FIT_GRAPH, new ActionZoomFitGraph(this));
		_allChartActions.put(ACTION_ID_ZOOM_IN, new ActionZoomIn(this));
		_allChartActions.put(ACTION_ID_ZOOM_IN_TO_SLIDER, new ActionZoomToSlider(this));
		_allChartActions.put(ACTION_ID_ZOOM_OUT, new ActionZoomOut(this));

		enableActions();
	}

	/**
	 * Creates the chart actions
	 */
	public void createChartActions() {

		createActions_10_ChartActions();
	}

	/**
	 * @return
	 */
	private SelectionChartInfo createChartInfo() {

		if (_chartComponents == null) {
			return null;
		}

		final ChartComponentGraph componentGraph = _chartComponents.getChartComponentGraph();
		final int hoveredLineValueIndex = componentGraph.getHoveredValuePointIndex();
		if (hoveredLineValueIndex == -1) {
			// hovered line is not yet recognized
			return null;
		}

		final SelectionChartInfo chartInfo = new SelectionChartInfo(this);

		chartInfo.chartDataModel = _chartDataModel;
		chartInfo.chartDrawingData = _chartComponents.getChartDrawingData();

		chartInfo.leftSliderValuesIndex = componentGraph.getLeftSlider().getValuesIndex();
		chartInfo.rightSliderValuesIndex = componentGraph.getRightSlider().getValuesIndex();

		chartInfo.selectedSliderValuesIndex = hoveredLineValueIndex;

		return chartInfo;
	}

	/**
	 * disable all actions
	 */
	private void disableAllActions() {

		if (_allChartActions != null) {

			for (final Action action : _allChartActions.values()) {
				action.setEnabled(false);
			}
		}
	}

	/**
	 * Dispose colors which are used to paint the graphs.
	 */
	public void disposeColors() {

		_chartComponents.getChartComponentGraph().disposeColors();
	}

	void enableActions() {

		if (_allChartActions == null) {
			return;
		}

		final ChartComponentGraph chartComponentGraph = _chartComponents.getChartComponentGraph();

		final boolean canZoomOut = chartComponentGraph.getZoomRatio() > 1;
		final boolean canZoomIn = chartComponentGraph.getXXDevGraphWidth() < ChartComponents.CHART_MAX_WIDTH;

		_allChartActions.get(ACTION_ID_ZOOM_IN).setEnabled(canZoomIn);
		_allChartActions.get(ACTION_ID_ZOOM_OUT).setEnabled(canZoomOut);

		// zoom in to slider has no limits but when there are more than 10000 units, the units are not displayed
		_allChartActions.get(ACTION_ID_ZOOM_IN_TO_SLIDER).setEnabled(true);

		// fit to graph is always enabled because the y-slider can change the chart
		_allChartActions.get(ACTION_ID_ZOOM_FIT_GRAPH).setEnabled(true);

		_allChartActions.get(ACTION_ID_MOUSE_MODE).setEnabled(true);
		_allChartActions.get(ACTION_ID_MOVE_LEFT_SLIDER_HERE).setEnabled(true);
		_allChartActions.get(ACTION_ID_MOVE_RIGHT_SLIDER_HERE).setEnabled(true);
		_allChartActions.get(ACTION_ID_MOVE_SLIDERS_TO_BORDER).setEnabled(true);
	}

	void fillContextMenu(	final IMenuManager menuMgr,
							final ChartXSlider leftSlider,
							final ChartXSlider rightSlider,
							final int hoveredBarSerieIndex,
							final int hoveredBarValueIndex,
							final int mouseDownDevPositionX,
							final int mouseDownDevPositionY) {

		if (_allChartActions == null) {
			return;
		}

		// check if this is slider context
		final boolean isSliderContext = leftSlider != null || rightSlider != null;
		final boolean showOnlySliderContext = isSliderContext && _chartContextProvider.showOnlySliderContextMenu();

		if (_chartContextProvider != null && showOnlySliderContext == false && _isTopMenuPosition) {
			_chartContextProvider.fillContextMenu(menuMgr, mouseDownDevPositionX, mouseDownDevPositionY);
		}

		fillContextMenu_ChartDefault(menuMgr, leftSlider, rightSlider, hoveredBarSerieIndex, hoveredBarValueIndex);

		if (_chartContextProvider != null && showOnlySliderContext == false && _isTopMenuPosition == false) {
			menuMgr.add(new Separator());
			_chartContextProvider.fillContextMenu(menuMgr, mouseDownDevPositionX, mouseDownDevPositionY);
		}
	}

	private void fillContextMenu_ChartDefault(	final IMenuManager menuMgr,
												final ChartXSlider leftSlider,
												final ChartXSlider rightSlider,
												final int hoveredBarSerieIndex,
												final int hoveredBarValueIndex) {

		if (_chartDataModel.getChartType() == ChartType.BAR) {

			/*
			 * create menu for bar charts
			 */

			// get the context provider from the data model
			final IChartContextProvider barChartContextProvider = (IChartContextProvider) _chartDataModel
					.getCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER);

			if (barChartContextProvider != null) {
				barChartContextProvider.fillBarChartContextMenu(menuMgr, hoveredBarSerieIndex, hoveredBarValueIndex);
			}

		} else {

			/*
			 * create menu for line charts
			 */

			// set text for mouse wheel mode
			final Action actionMouseMode = _allChartActions.get(ACTION_ID_MOUSE_MODE);
			if (_mouseMode.equals(MOUSE_MODE_SLIDER)) {
				// mouse mode: slider
				actionMouseMode.setText(Messages.Action_mouse_mode_zoom);

			} else {
				// mouse mode: zoom
				actionMouseMode.setText(Messages.Action_mouse_mode_slider);
			}

			// fill slider context menu
			if (_chartContextProvider != null) {
				menuMgr.add(new Separator());
				_chartContextProvider.fillXSliderContextMenu(menuMgr, leftSlider, rightSlider);
			}

			if (_isShowZoomActions) {
				menuMgr.add(new Separator());
				menuMgr.add(actionMouseMode);
				menuMgr.add(_allChartActions.get(ACTION_ID_MOVE_LEFT_SLIDER_HERE));
				menuMgr.add(_allChartActions.get(ACTION_ID_MOVE_RIGHT_SLIDER_HERE));
				menuMgr.add(_allChartActions.get(ACTION_ID_MOVE_SLIDERS_TO_BORDER));
				menuMgr.add(_allChartActions.get(ACTION_ID_ZOOM_IN_TO_SLIDER));
			}
		}
	}

	/**
	 * put the actions into the internal toolbar
	 * 
	 * @param refreshToolbar
	 */
	public void fillToolbar(final boolean refreshToolbar) {

		if (_allChartActions == null) {
			return;
		}

		if (_isShowZoomActions || _isShowMouseMode) {

			// add the action to the toolbar
			final IToolBarManager tbm = getToolBarManager();

			if (_isShowZoomActions) {

				tbm.add(new Separator());

				if (_isShowMouseMode) {
					tbm.add(_allChartActions.get(ACTION_ID_MOUSE_MODE));
				}

				tbm.add(_allChartActions.get(ACTION_ID_ZOOM_IN));
				tbm.add(_allChartActions.get(ACTION_ID_ZOOM_OUT));

				if (_chartDataModel.getChartType() != ChartType.BAR) {
					tbm.add(_allChartActions.get(ACTION_ID_ZOOM_FIT_GRAPH));
				}
			}

			if (refreshToolbar) {
				tbm.update(true);
			}
		}
	}

	void fireBarSelectionEvent(final int serieIndex, final int valueIndex) {

		_barSelectionSerieIndex = serieIndex;
		_barSelectionValueIndex = valueIndex;

		final Object[] listeners = _barSelectionListeners.getListeners();
		for (final Object listener2 : listeners) {
			final IBarSelectionListener listener = (IBarSelectionListener) listener2;
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.selectionChanged(serieIndex, valueIndex);
				}
			});
		}
	}

	void fireChartDoubleClick(final int serieIndex, final int valueIndex) {

		_barSelectionSerieIndex = serieIndex;
		_barSelectionValueIndex = valueIndex;

		final Object[] listeners = _barDoubleClickListeners.getListeners();
		for (final Object listener2 : listeners) {
			final IBarSelectionListener listener = (IBarSelectionListener) listener2;
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.selectionChanged(serieIndex, valueIndex);
				}
			});
		}
	}

	private void fireChartMouseEvent(final ChartMouseEvent mouseEvent) {

		final Object[] listeners = _mouseChartListener.getListeners();
		for (final Object listener : listeners) {

			switch (mouseEvent.type) {
			case Chart.ChartResized:
				((IMouseListener) listener).chartResized();
				break;

			case Chart.MouseExit:
				((IMouseListener) listener).mouseExit();
				break;

			case Chart.MouseMove:
				((IMouseListener) listener).mouseMove(mouseEvent);
				break;

			case Chart.MouseDownPre:
				((IMouseListener) listener).mouseDown(mouseEvent);
				break;

			case Chart.MouseUp:
				((IMouseListener) listener).mouseUp(mouseEvent);
				break;

			case Chart.MouseDoubleClick:
				((IMouseListener) listener).mouseDoubleClick(mouseEvent);
				break;

			default:
				break;
			}
		}
	}

	void fireFocusEvent() {

		final Object[] listeners = _focusListeners.getListeners();
		for (final Object listener2 : listeners) {
			final Listener listener = (Listener) listener2;
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.handleEvent(new Event());
				}
			});
		}
	}

	public void fireSliderMoveEvent() {

		final SelectionChartInfo chartInfo = createChartInfo();

		final Object[] listeners = _sliderMoveListeners.getListeners();
		for (final Object listener2 : listeners) {
			final ISliderMoveListener listener = (ISliderMoveListener) listener2;
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.sliderMoved(chartInfo);
				}
			});
		}
	}

	public Color getBackgroundColor() {
		return _backgroundColor;
	}

	public Boolean getCanAutoMoveSliders() {
		return _chartComponents.getChartComponentGraph()._canAutoMoveSliders;
	}

	public boolean getCanAutoZoomToSlider() {
		return _chartComponents.getChartComponentGraph()._canAutoZoomToSlider;
	}

	public ChartComponents getChartComponents() {
		return _chartComponents;
	}

	/**
	 * @return Returns the data model for the chart
	 */
	public ChartDataModel getChartDataModel() {
		return _chartDataModel;
	}

	public ChartDrawingData getChartDrawingData() {
		return _chartComponents.getChartDrawingData();
	}

	/**
	 * Return information about the chart
	 * 
	 * @return
	 */
	public SelectionChartInfo getChartInfo() {
		return createChartInfo();
	}

	Object[] getChartOverlays() {
		return _chartOverlayListener.getListeners();
	}

	IHoveredValueListener getHoveredListener() {
		return _hoveredListener;
	}

	/**
	 * @return Returns the index in the data series which is hovered with the mouse or
	 *         <code>-1</code> when a value is not hovered.
	 */
	public int getHoveredValuePointIndex() {
		return _chartComponents.getChartComponentGraph().getHoveredValuePointIndex();
	}

	public int getLeftAxisWidth() {
		return _chartComponents.getYAxisWidthLeft();
	}

	/**
	 * @return Returns the left slider
	 */
	public ChartXSlider getLeftSlider() {
		return _chartComponents.getChartComponentGraph().getLeftSlider();
	}

	/**
	 * @return Returns margin between the upper most graph and the top. This can include the chart
	 *         title height and/or horizontal slider label height.
	 */
	public int getMarginTop() {
		return _chartComponents.getDevChartMarginTop();
	}

	public String getMouseMode() {
		return _mouseMode;
	}

	/**
	 * @return Return the right slider
	 */
	public ChartXSlider getRightSlider() {
		return _chartComponents.getChartComponentGraph().getRightSlider();
	}

	public ISelection getSelection() {

		if (_chartDataModel == null) {
			return null;
		}

		if (_chartDataModel.getChartType() == ChartType.BAR) {
			return new SelectionBarChart(_barSelectionSerieIndex, _barSelectionValueIndex);
		}

		return null;
	}

	/**
	 * @return Returns <code>true</code> to start the bars at the bottom of the chart
	 */
	boolean getStartAtChartBottom() {
		return _isDrawBarChartAtBottom;
	}

	/**
	 * Returns the toolbar for the chart, if no toolbar manager is set with setToolbarManager, the
	 * manager will be created and the toolbar is on top of the chart.
	 * <p>
	 * A border is painted between the chart and toolbar because {@link ViewForm} draws this line in
	 * the onPaint() method.
	 * 
	 * @return
	 */
	public IToolBarManager getToolBarManager() {

		if (_toolbarMgr == null) {

			// create the toolbar and put it on top of the chart
			final ToolBar toolBarControl = new ToolBar(this, SWT.FLAT/* | SWT.WRAP */);
			setTopRight(toolBarControl);

			// toolBarControl.addListener(SWT.Resize, new Listener() {
			// public void handleEvent(Event e) {
			//
// wrap the tool bar on resize
			// Rectangle rect = getClientArea();
			// Point size = toolBarControl.computeSize(rect.width, SWT.DEFAULT);
			// toolBarControl.setSize(size);
			//
			// }
			// });

			// create toolbar manager
			_toolbarMgr = new ToolBarManager(toolBarControl);
		}

		return _toolbarMgr;
	}

	/**
	 * @return
	 * @return Returns control for which the tool tip is created
	 */
	public ChartComponentAxis getToolTipControl() {
		return getChartComponents().getAxisLeft();
	}

	protected Control getValuePointControl() {
		return _chartComponents.getChartComponentGraph();
	}

	/**
	 * returns the value index for the x-sliders
	 */
	public SelectionChartXSliderPosition getXSliderPosition() {

		final ChartComponentGraph chartGraph = _chartComponents.getChartComponentGraph();

		return new SelectionChartXSliderPosition(this, chartGraph.getLeftSlider().getValuesIndex(), chartGraph
				.getRightSlider()
				.getValuesIndex());
	}

	public long getXXDevViewPortLeftBorder() {
		return _chartComponents.getChartComponentGraph().getXXDevViewPortLeftBorder();
	}

	protected void handleTooltipMouseEvent(final Event event, final Point mouseDisplayPosition) {
		_chartComponents.getChartComponentGraph().handleTooltipMouseEvent(event, mouseDisplayPosition);
	}

	/**
	 * @return Returns <code>true</code> when the x-sliders are visible
	 */
	public boolean isXSliderVisible() {
		return _chartComponents._devSliderBarHeight != 0;
	}

	void onExecuteMouseMode(final boolean isChecked) {
		setMouseMode(isChecked);
	}

	void onExecuteMoveLeftSliderHere() {
		_chartComponents.getChartComponentGraph().moveLeftSliderHere();
	}

	void onExecuteMoveRightSliderHere() {
		_chartComponents.getChartComponentGraph().moveRightSliderHere();
	}

	public void onExecuteMoveSlidersToBorder() {
		_chartComponents.getChartComponentGraph().moveSlidersToBorderWithoutCheck();
	}

	protected void onExecuteZoomFitGraph() {

		_chartDataModel.resetMinMaxValues();

		_chartComponents.getChartComponentGraph().zoomOutFitGraph();
	}

	void onExecuteZoomIn() {

		if (_chartComponents._devSliderBarHeight == 0) {
			_chartComponents.getChartComponentGraph().zoomInWithoutSlider();
			_chartComponents.onResize();
		} else {
			_chartComponents.getChartComponentGraph().zoomInWithMouse(Integer.MIN_VALUE);
		}
	}

	/**
	 * Zoom to the vertical sliders
	 */
	public void onExecuteZoomInWithSlider() {

		_chartComponents.getChartComponentGraph().zoomInWithSlider();
		_chartComponents.onResize();
	}

	public void onExecuteZoomOut(final boolean updateChart) {

		if (_chartDataModel == null) {
			return;
		}

		_chartComponents.getChartComponentGraph().zoomOutWithMouse(updateChart, Integer.MIN_VALUE);
	}

	void onExternalChartResize() {

		fireChartMouseEvent(new ChartMouseEvent(Chart.ChartResized, System.currentTimeMillis(), 0, 0));
	}

	ChartMouseEvent onExternalMouseDoubleClick(final long eventTime, final int devXMouse, final int devYMouse) {

		final ChartMouseEvent event = new ChartMouseEvent(Chart.MouseDoubleClick, eventTime, devXMouse, devYMouse);

		fireChartMouseEvent(event);

		return event;
	}

	ChartMouseEvent onExternalMouseDownPre(final long eventTime, final int devXMouse, final int devYMouse) {

		final ChartMouseEvent event = new ChartMouseEvent(Chart.MouseDownPre, eventTime, devXMouse, devYMouse);

		fireChartMouseEvent(event);

		return event;
	}

	void onExternalMouseExit(final long eventTime) {

		fireChartMouseEvent(new ChartMouseEvent(Chart.MouseExit, eventTime, 0, 0));
	}

	ChartMouseEvent onExternalMouseMove(final long eventTime, final int devXMouse, final int devYMouse) {

		final ChartMouseEvent event = new ChartMouseEvent(Chart.MouseMove, eventTime, devXMouse, devYMouse);

		fireChartMouseEvent(event);

		return event;
	}

	ChartMouseEvent onExternalMouseUp(final long eventTime, final int devXMouse, final int devYMouse) {

		final ChartMouseEvent event = new ChartMouseEvent(Chart.MouseUp, eventTime, devXMouse, devYMouse);

		fireChartMouseEvent(event);

		return event;
	}

	void onHideContextMenu(final MenuEvent e, final Control menuParentControl) {

		if (_chartContextProvider != null) {
			_chartContextProvider.onHideContextMenu(e, menuParentControl);
		}
	}

	void onShowContextMenu(final MenuEvent menuEvent, final Control menuParentControl) {

		if (_chartContextProvider != null) {
			_chartContextProvider.onShowContextMenu(menuEvent, menuParentControl);
		}
	}

	/**
	 * make the graph dirty and redraw it
	 */
	public void redrawChart() {
		_chartComponents.getChartComponentGraph().redrawChart();
	}

	public void removeChartOverlay(final IChartOverlay chartOverlay) {
		_chartOverlayListener.remove(chartOverlay);
	}

	public void removeDoubleClickListener(final IBarSelectionListener listener) {
		_barDoubleClickListeners.remove(listener);
	}

	public void removeFocusListener(final Listener listener) {
		_focusListeners.remove(listener);
	}

	public void removeMouseChartListener(final IMouseListener mouseListener) {
		_mouseChartListener.remove(mouseListener);
	}

	public void removeSelectionChangedListener(final IBarSelectionListener listener) {
		_barSelectionListeners.remove(listener);
	}

	public void resetGraphAlpha() {
		graphTransparencyAdjustment = 1;
	}

	/**
	 * Do a resize for all chart components which creates new drawing data
	 */
	public void resizeChart() {
		_chartComponents.onResize();
	}

	/**
	 * Set the background color for the chart, the default is SWT.COLOR_WHITE
	 * 
	 * @param backgroundColor
	 *            The backgroundColor to set.
	 */
	public void setBackgroundColor(final Color backgroundColor) {
		this._backgroundColor = backgroundColor;
	}

	/**
	 * Set the option to move the sliders to the border when the chart is zoomed
	 * 
	 * @param canMoveSlidersWhenZoomed
	 */
	public void setCanAutoMoveSliders(final boolean canMoveSlidersWhenZoomed) {
		_chartComponents.getChartComponentGraph().setCanAutoMoveSlidersWhenZoomed(canMoveSlidersWhenZoomed);
	}

	/**
	 * set the option to auto zoom the chart
	 * 
	 * @param canZoomToSliderOnMouseUp
	 */
	public void setCanAutoZoomToSlider(final boolean canZoomToSliderOnMouseUp) {
		_chartComponents.getChartComponentGraph().setCanAutoZoomToSlider(canZoomToSliderOnMouseUp);
	}

	public void setChartOverlayDirty() {
		_chartComponents.getChartComponentGraph().setChartOverlayDirty();
	}

	public void setContextProvider(final IChartContextProvider chartContextProvider) {
		_chartContextProvider = chartContextProvider;
	}

	/**
	 * @param chartContextProvider
	 * @param isTopMenuPosition
	 *            When <code>true</code> the context menu will be positioned before the chart menu
	 *            actions.
	 */
	public void setContextProvider(final IChartContextProvider chartContextProvider, final boolean isTopMenuPosition) {

		_chartContextProvider = chartContextProvider;
		_isTopMenuPosition = isTopMenuPosition;
	}

	protected void setDataModel(final ChartDataModel chartDataModel) {
		_chartDataModel = chartDataModel;
	}

	/**
	 * Set <code>false</code> to not draw the bars at the bottom of the chart
	 * 
	 * @param fDrawBarCharttAtBottom
	 */
	public void setDrawBarChartAtBottom(final boolean fDrawBarCharttAtBottom) {
		this._isDrawBarChartAtBottom = fDrawBarCharttAtBottom;
	}

	/**
	 * Display an error message instead of the chart.
	 * 
	 * @param errorMessage
	 */
	public void setErrorMessage(final String errorMessage) {

		final ChartDataModel emptyModel = new ChartDataModel(ChartType.LINE);

		_chartComponents.setErrorMessage(errorMessage);

		_chartDataModel = emptyModel;
		_chartComponents.setModel(emptyModel, false);

		disableAllActions();
	}

	@Override
	public boolean setFocus() {

		/*
		 * set focus to the graph component
		 */
		return _chartComponents.getChartComponentGraph().setFocus();
	}

	/**
	 * Adjust the alpha value for the filling operation, this value is multiplied with
	 * {@link #graphTransparencyFilling} and {@link #graphTransparencyLine} which is set in the tour
	 * chart preference page.
	 * 
	 * @param adjustment
	 */
	public void setGraphAlpha(final double adjustment) {
		graphTransparencyAdjustment = adjustment;
	}

	public void setGrid(final int horizontalGrid,
						final int verticalGrid,
						final boolean isHGridVisible,
						final boolean isVGridVisible) {

		gridHorizontalDistance = horizontalGrid;
		gridVerticalDistance = verticalGrid;

		isShowHorizontalGridLines = isHGridVisible;
		isShowVerticalGridLines = isVGridVisible;

		_chartComponents.onResize();
	}

	protected void setHoveredListener(final IHoveredValueListener hoveredValuePointListener) {
		_hoveredListener = hoveredValuePointListener;
	}

	/**
	 * Sets the mouse mode, when <code>true</code> the mode {@link #MOUSE_MODE_SLIDER} is active,
	 * this is the default
	 * 
	 * @param isChecked
	 */
	public void setMouseMode(final boolean isChecked) {

		_mouseMode = isChecked ? MOUSE_MODE_SLIDER : MOUSE_MODE_ZOOM;

		updateMouseModeUIState();

		final Point devMouse = this.toControl(getDisplay().getCursorLocation());
		_chartComponents.getChartComponentGraph().setCursorStyle(devMouse.y);

	}

	public void setMouseMode(final Object newMouseMode) {

		if (newMouseMode instanceof String) {

			_mouseMode = (String) newMouseMode;

			updateMouseModeUIState();
		}
	}

	/**
	 * Select (highlight) the bar in the bar chart
	 * 
	 * @param selectedItems
	 *            items in the x-data serie which should be selected, can be <code>null</code> to
	 *            deselect the bar
	 */
	public void setSelectedBars(final boolean[] selectedItems) {

		// set default value
		_barSelectionSerieIndex = 0;
		_barSelectionValueIndex = 0;

		if (selectedItems != null) {

			// get selected bar
			for (int itemIndex = 0; itemIndex < selectedItems.length; itemIndex++) {
				if (selectedItems[itemIndex]) {
					_barSelectionValueIndex = itemIndex;
					break;
				}
			}
		}

		_chartComponents.getChartComponentGraph().setSelectedBars(selectedItems);

		fireBarSelectionEvent(0, _barSelectionValueIndex);
	}

	/**
	 * Make the mouse mode button visible
	 */
	public void setShowMouseMode() {
		_isShowMouseMode = true;
	}

	/**
	 * @param isSliderVisible
	 *            <code>true</code> shows the sliders
	 */
	public void setShowSlider(final boolean isSliderVisible) {
		_chartComponents.setSliderVisible(isSliderVisible);
	}

	public void setShowZoomActions(final boolean isShowZoomActions) {
		_isShowZoomActions = isShowZoomActions;
	}

	/**
	 * set the synch configuration which is used when the chart is drawn/resized
	 * 
	 * @param synchConfigIn
	 *            set <code>null</code> to disable the synchronization
	 */
	public void setSynchConfig(final SynchConfiguration synchConfigIn) {
		_chartComponents.setSynchConfig(synchConfigIn);
	}

	/**
	 * Set's the {@link SynchConfiguration} listener, this is a {@link Chart} which will be notified
	 * when this chart is resized, <code>null</code> will disable the synchronisation
	 * 
	 * @param chartWidget
	 */
	public void setSynchedChart(final Chart chartWidget) {
		_synchedChart = chartWidget;
	}

	protected void setSynchMode(final int synchMode) {
		_synchMode = synchMode;
	}

	/**
	 * @param toolbarMgr
	 * @param isFillToolbar
	 *            set <code>false</code> when the toolbar will be filled with
	 *            {@link Chart#fillToolbar(boolean)} from externally, when <code>true</code> the
	 *            toolbar will be filled when the chart is updated
	 */
	public void setToolBarManager(final IToolBarManager toolbarMgr, final boolean isFillToolbar) {
		_toolbarMgr = toolbarMgr;
		_isFillToolbar = isFillToolbar;
	}

	public void setTourToolTipProvider(final ITourToolTipProvider tourToolTip) {

		// set tour info icon into the left axis
		getToolTipControl().setTourToolTipProvider(tourToolTip);
	}

	public void setValuePointToolTipProvider(final IValuePointToolTip valuePointToolTip) {
		_chartComponents.componentGraph.valuePointToolTip = valuePointToolTip;
	}

	/**
	 * sets the position of the x-sliders
	 * 
	 * @param sliderPosition
	 */
	public void setXSliderPosition(final SelectionChartXSliderPosition sliderPosition) {

		// check if the position is for this chart
		if (sliderPosition.getChart() == this) {
			_chartComponents.setXSliderPosition(sliderPosition);
		}
	}

	/**
	 * Enable/disable the zoom in/out action
	 * 
	 * @param isEnabled
	 */
	public void setZoomActionsEnabled(final boolean isEnabled) {

		_allChartActions.get(ACTION_ID_ZOOM_FIT_GRAPH).setEnabled(isEnabled);
		_allChartActions.get(ACTION_ID_ZOOM_IN).setEnabled(isEnabled);
		_allChartActions.get(ACTION_ID_ZOOM_OUT).setEnabled(isEnabled);
	}

	public void switchSlidersTo2ndXData() {
		_chartComponents.getChartComponentGraph().switchSlidersTo2ndXData();
	}

	/**
	 * synchronize the charts
	 */
	protected void synchronizeChart() {

		if (_synchedChart == null) {
			return;
		}

		getDisplay().asyncExec(new Runnable() {
			public void run() {
				_synchedChart.setSynchConfig(_chartComponents._synchConfigOut);
			}
		});
	}

	/**
	 * Sets a new data model for the chart and redraws it, NULL will hide the chart
	 * 
	 * @param chartDataModel
	 * @param isShowAllData
	 *            set <code>true</code> to show the entire data in the chart, otherwise the min max
	 *            values will be kept
	 */
	public void updateChart(final ChartDataModel chartDataModel, final boolean isShowAllData) {
		updateChart(chartDataModel, true, isShowAllData);
	}

	/**
	 * Set a new data model for the chart and redraws it, NULL will hide the chart.
	 * <p>
	 * This method sets the data for the chart and creates it.
	 * 
	 * @param chartDataModel
	 * @param isResetSelection
	 *            <code>true</code> to reset the last selection in the chart
	 * @param isShowAllData
	 *            set <code>true</code> to show the entire data in the chart, otherwise the min max
	 *            values will be kept
	 */
	public void updateChart(final ChartDataModel chartDataModel,
							final boolean isResetSelection,
							final boolean isShowAllData) {

		if (chartDataModel == null || //
				(chartDataModel != null //
						&& chartDataModel.getYData().isEmpty() //

				// history do not have Y values
				&& chartDataModel.getChartType() != ChartType.HISTORY) //
		) {

			final ChartDataModel emptyModel = new ChartDataModel(ChartType.LINE);

			if (chartDataModel != null) {
				String errorMessage = chartDataModel.getErrorMessage();
				if (errorMessage == null) {

					/*
					 * display error message that the user is not confuses when a graph is not
					 * displayed
					 */
					errorMessage = Messages.Error_Message_001_Default;
				}
				_chartComponents.setErrorMessage(errorMessage);
			}

			_chartDataModel = emptyModel;
			_chartComponents.setModel(emptyModel, false);

			disableAllActions();

			return;
		}

		// reset error
		_chartComponents.setErrorMessage(null);

		_chartDataModel = chartDataModel;

		createActions();
		_chartComponents.setModel(chartDataModel, isShowAllData);
		enableActions();

		// reset last selected x-data
		if (isResetSelection) {
			setSelectedBars(null);
		}

		// update chart info view
		fireSliderMoveEvent();
	}

	/**
	 * Updates only the custom layers which performance is much faster than a chart update.
	 */
	public void updateCustomLayers() {
		_chartComponents.updateCustomLayers();
	}

	private void updateMouseModeUIState() {

		if (_allChartActions != null) {
			_allChartActions.get(ACTION_ID_MOUSE_MODE).setChecked(_mouseMode.equals(MOUSE_MODE_SLIDER));
		}
	}

	public void zoomOut() {
		onExecuteZoomFitGraph();
	}

}
