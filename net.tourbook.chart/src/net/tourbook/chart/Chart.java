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
package net.tourbook.chart;

import java.util.HashMap;

import net.tourbook.util.ITourToolTipProvider;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Color;
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

	static final String					COMMAND_ID_ZOOM_IN					= "net.tourbook.chart.command.zoomIn";				//$NON-NLS-1$
	static final String					COMMAND_ID_ZOOM_IN_TO_SLIDER		= "net.tourbook.chart.command.zoomInToSlider";		//$NON-NLS-1$
	static final String					COMMAND_ID_ZOOM_OUT					= "net.tourbook.chart.command.zoomOut";			//$NON-NLS-1$
	static final String					COMMAND_ID_ZOOM_FIT_GRAPH			= "net.tourbook.chart.command.fitGraph";			//$NON-NLS-1$

	static final String					COMMAND_ID_MOUSE_MODE				= "net.tourbook.chart.command.mouseMode";			//$NON-NLS-1$

	private static final String			COMMAND_ID_MOVE_LEFT_SLIDER_HERE	= "net.tourbook.chart.command.moveLeftSliderHere";	//$NON-NLS-1$
	private static final String			COMMAND_ID_MOVE_RIGHT_SLIDER_HERE	= "net.tourbook.chart.command.moveRightSliderHere"; //$NON-NLS-1$
	private static final String			COMMAND_ID_MOVE_SLIDERS_TO_BORDER	= "net.tourbook.chart.command.moveSlidersToBorder"; //$NON-NLS-1$

	static final int					NO_BAR_SELECTION					= -1;

	public static final int				SYNCH_MODE_NO						= 0;
	public static final int				SYNCH_MODE_BY_SCALE					= 1;
	public static final int				SYNCH_MODE_BY_SIZE					= 2;

	public static final String			MOUSE_MODE_SLIDER					= "slider";										//$NON-NLS-1$
	public static final String			MOUSE_MODE_ZOOM						= "zoom";											//$NON-NLS-1$

	private static final int			MouseMove							= 10;
	private static final int			MouseDownPre						= 20;
//	private static final int			MouseDownPost						= 21;
	private static final int			MouseUp								= 30;
	private static final int			MouseDoubleClick					= 40;

	private final ListenerList			_focusListeners						= new ListenerList();
	private final ListenerList			_barSelectionListeners				= new ListenerList();
	private final ListenerList			_barDoubleClickListeners			= new ListenerList();
	private final ListenerList			_sliderMoveListeners				= new ListenerList();
	private final ListenerList			_doubleClickListeners				= new ListenerList();
	private final ListenerList			_mouseListener						= new ListenerList();

	ChartComponents						_chartComponents;

	private Chart						_synchedChart;

	private ChartDataModel				_chartDataModel;

	private IToolBarManager				_toolbarMgr;
	private IChartContextProvider		_chartContextProvider;
	private boolean						_isShowZoomActions					= false;

	private boolean						_isShowMouseMode					= false;
	private Color						_backgroundColor;

	/**
	 * listener which is called when the x-marker was dragged
	 */
	IChartListener						_draggingListenerXMarker;

	/**
	 * when set to <code>true</code> the toolbar is within the chart control, otherwise the toolbar
	 * is outsite of the chart
	 */
	boolean								_useInternalActionBar				= true;
	boolean								_useActionHandlers					= false;

	private final ActionHandlerManager	_actionHandlerManager				= ActionHandlerManager.getInstance();
	HashMap<String, ActionProxy>		_chartActionProxies;
	private boolean						_isFillToolbar						= true;
	private boolean						_isToolbarCreated;

	private int							_barSelectionSerieIndex;
	private int							_barSelectionValueIndex;

	int									_synchMode;

	/**
	 * <code>true</code> to start the bar chart at the bottom of the chart
	 */
	private boolean						_isDrawBarChartAtBottom				= true;

	/**
	 * minimum width in pixel for one unit, this is only an approximate value because the pixel is
	 * rounded up or down to fit a rounded unit
	 */
	protected int						gridVerticalDistance				= 30;

	protected int						gridHorizontalDistance				= 70;

	protected boolean					isShowHorizontalGridLines			= false;
	protected boolean					isShowVerticalGridLines				= false;

	/**
	 * Transparency of the graph lines
	 */
	protected int						graphTransparencyLine				= 0xFF;

	/**
	 * Transparency of the graph fillings
	 */
	protected int						graphTransparencyFilling			= 0xE0;

	/**
	 * The graph transparency can be adjusted with this value. This value is multiplied with the
	 * {@link #graphTransparencyFilling} and {@link #graphTransparencyLine}.
	 */
	double								graphTransparencyAdjustment			= 1.0;

	/**
	 * Antialiasing for the graph, can be {@link SWT#ON} or {@link SWT#OFF}.
	 */
	protected int						graphAntialiasing					= SWT.OFF;

	/**
	 * mouse behaviour:<br>
	 * <br>
	 * {@link #MOUSE_MODE_SLIDER} or {@link #MOUSE_MODE_ZOOM}
	 */
	private String						_mouseMode							= MOUSE_MODE_SLIDER;

	private boolean						_isFirstContextMenu;

	/**
	 * Chart widget
	 */
	public Chart(final Composite parent, final int style) {

		super(parent, style);
		setBorderVisible(false);

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

	public void addBarSelectionListener(final IBarSelectionListener listener) {
		_barSelectionListeners.add(listener);
	}

	public void addDoubleClickListener(final IBarSelectionListener listener) {
		_barDoubleClickListeners.add(listener);
	}

	public void addDoubleClickListener(final Listener listener) {
		_doubleClickListeners.add(listener);
	}

	public void addFocusListener(final Listener listener) {
		_focusListeners.add(listener);
	}

	public void addMouseListener(final IMouseListener mouseListener) {
		_mouseListener.add(mouseListener);
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

		createChartActionProxies();

		if (_isFillToolbar && _isToolbarCreated == false) {
			_isToolbarCreated = true;
			fillToolbar(true);
		}
	}

	/**
	 * Creates the handlers for the chart actions
	 */
	public void createChartActionHandlers() {

		// use the commands defined in plugin.xml
		_useActionHandlers = true;

		_actionHandlerManager.createActionHandlers();
		createChartActionProxies();
	}

	/**
	 * Creates action proxys for all chart actions
	 */
	private void createChartActionProxies() {

		// create actions only once
		if (_chartActionProxies != null) {
			return;
		}

		_chartActionProxies = new HashMap<String, ActionProxy>();

		final boolean useInternalActionBar = useInternalActionBar();
		Action action;
		ActionProxy actionProxy;

		/*
		 * Action: zoom in
		 */
		if (useInternalActionBar) {
			action = new ActionZoomIn(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_ZOOM_IN, action);
		_chartActionProxies.put(COMMAND_ID_ZOOM_IN, actionProxy);

		/*
		 * Action: zoom in to slider
		 */
		if (useInternalActionBar) {
			action = new ActionZoomToSlider(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_ZOOM_IN_TO_SLIDER, action);
		_chartActionProxies.put(COMMAND_ID_ZOOM_IN_TO_SLIDER, actionProxy);

		/*
		 * Action: zoom out
		 */
		if (useInternalActionBar) {
			action = new ActionZoomOut(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_ZOOM_OUT, action);
		_chartActionProxies.put(COMMAND_ID_ZOOM_OUT, actionProxy);

		/*
		 * Action: fit graph to window
		 */
		if (useInternalActionBar) {
			action = new ActionFitGraph(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_ZOOM_FIT_GRAPH, action);
		_chartActionProxies.put(COMMAND_ID_ZOOM_FIT_GRAPH, actionProxy);

		/*
		 * Action: mouse moude
		 */
		if (useInternalActionBar) {
			action = new ActionMouseMode(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_MOUSE_MODE, action);
		_chartActionProxies.put(COMMAND_ID_MOUSE_MODE, actionProxy);

		/*
		 * Action: move sliders when
		 */
		if (useInternalActionBar) {
			action = new ActionMoveSlidersToBorder(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_MOVE_SLIDERS_TO_BORDER, action);
		_chartActionProxies.put(COMMAND_ID_MOVE_SLIDERS_TO_BORDER, actionProxy);

		/*
		 * Action: move left slider here
		 */
		if (useInternalActionBar) {
			action = new ActionMoveLeftSliderHere(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_MOVE_LEFT_SLIDER_HERE, action);
		_chartActionProxies.put(COMMAND_ID_MOVE_LEFT_SLIDER_HERE, actionProxy);

		/*
		 * Action: move right slider here
		 */
		if (useInternalActionBar) {
			action = new ActionMoveRightSliderHere(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_MOVE_RIGHT_SLIDER_HERE, action);
		_chartActionProxies.put(COMMAND_ID_MOVE_RIGHT_SLIDER_HERE, actionProxy);

		enableActions();
	}

	/**
	 * @return
	 */
	private SelectionChartInfo createChartInfo() {

		if (_chartComponents == null) {
			return null;
		}

		final SelectionChartInfo chartInfo = new SelectionChartInfo(this);

		chartInfo.chartDataModel = _chartDataModel;
		chartInfo.chartDrawingData = _chartComponents.getChartDrawingData();

		final ChartComponentGraph chartGraph = _chartComponents.getChartComponentGraph();
		chartInfo.leftSliderValuesIndex = chartGraph.getLeftSlider().getValuesIndex();
		chartInfo.rightSliderValuesIndex = chartGraph.getRightSlider().getValuesIndex();
		chartInfo.selectedSliderValuesIndex = chartGraph.getSelectedSlider().getValuesIndex();

		return chartInfo;
	}

	/**
	 * disable all actions
	 */
	private void disableAllActions() {

		if (_chartActionProxies != null) {

			for (final ActionProxy actionProxy : _chartActionProxies.values()) {
				actionProxy.setEnabled(false);
			}
			_actionHandlerManager.updateUIState();
		}
	}

	void enableActions() {

		if (_chartActionProxies == null) {
			return;
		}

		final ChartComponentGraph chartComponentGraph = _chartComponents.getChartComponentGraph();

		final boolean canZoomOut = chartComponentGraph.getGraphZoomRatio() > 1;
		final boolean canZoomIn = chartComponentGraph.getXXDevGraphWidth() < ChartComponents.CHART_MAX_WIDTH;

		_chartActionProxies.get(COMMAND_ID_ZOOM_IN).setEnabled(canZoomIn);
		_chartActionProxies.get(COMMAND_ID_ZOOM_OUT).setEnabled(canZoomOut);

		// zoom in to slider has no limits but when there are more than 10000 units, the units are not displayed
		_chartActionProxies.get(COMMAND_ID_ZOOM_IN_TO_SLIDER).setEnabled(true);

		// fit to graph is always enabled because the y-slider can change the chart
		_chartActionProxies.get(COMMAND_ID_ZOOM_FIT_GRAPH).setEnabled(true);

		_chartActionProxies.get(COMMAND_ID_MOUSE_MODE).setEnabled(true);
		_chartActionProxies.get(COMMAND_ID_MOVE_LEFT_SLIDER_HERE).setEnabled(true);
		_chartActionProxies.get(COMMAND_ID_MOVE_RIGHT_SLIDER_HERE).setEnabled(true);
		_chartActionProxies.get(COMMAND_ID_MOVE_SLIDERS_TO_BORDER).setEnabled(true);

		if (_useActionHandlers) {
			_actionHandlerManager.updateUIState();
		}
	}

	void fillContextMenu(	final IMenuManager menuMgr,
							final ChartXSlider leftSlider,
							final ChartXSlider rightSlider,
							final int hoveredBarSerieIndex,
							final int hoveredBarValueIndex,
							final int mouseDownDevPositionX,
							final int mouseDownDevPositionY) {

		if (_chartActionProxies == null) {
			return;
		}

		// check if this is slider context
		final boolean isSliderContext = leftSlider != null || rightSlider != null;
		final boolean showOnlySliderContext = isSliderContext && _chartContextProvider.showOnlySliderContextMenu();

		if (_chartContextProvider != null && showOnlySliderContext == false && _isFirstContextMenu) {
			_chartContextProvider.fillContextMenu(menuMgr, mouseDownDevPositionX, mouseDownDevPositionY);
		}

		if (_chartDataModel.getChartType() == ChartDataModel.CHART_TYPE_BAR) {

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
			final Action actionMouseMode = _chartActionProxies.get(COMMAND_ID_MOUSE_MODE).getAction();
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
				menuMgr.add(_chartActionProxies.get(COMMAND_ID_MOVE_LEFT_SLIDER_HERE).getAction());
				menuMgr.add(_chartActionProxies.get(COMMAND_ID_MOVE_RIGHT_SLIDER_HERE).getAction());
				menuMgr.add(_chartActionProxies.get(COMMAND_ID_MOVE_SLIDERS_TO_BORDER).getAction());
				menuMgr.add(_chartActionProxies.get(COMMAND_ID_ZOOM_IN_TO_SLIDER).getAction());
			}
		}

		if (_chartContextProvider != null && showOnlySliderContext == false && _isFirstContextMenu == false) {
			menuMgr.add(new Separator());
			_chartContextProvider.fillContextMenu(menuMgr, mouseDownDevPositionX, mouseDownDevPositionY);
		}
	}

	/**
	 * put the actions into the internal toolbar
	 * 
	 * @param refreshToolbar
	 */
	public void fillToolbar(final boolean refreshToolbar) {

		if (_chartActionProxies == null) {
			return;
		}

		if (_useInternalActionBar && (_isShowZoomActions || _isShowMouseMode)) {

			// add the action to the toolbar
			final IToolBarManager tbm = getToolBarManager();

			if (_isShowZoomActions) {

				tbm.add(new Separator());

				if (_isShowMouseMode) {
					tbm.add(_chartActionProxies.get(COMMAND_ID_MOUSE_MODE).getAction());
				}

				tbm.add(_chartActionProxies.get(COMMAND_ID_ZOOM_IN).getAction());
				tbm.add(_chartActionProxies.get(COMMAND_ID_ZOOM_OUT).getAction());

				if (_chartDataModel.getChartType() != ChartDataModel.CHART_TYPE_BAR) {
					tbm.add(_chartActionProxies.get(COMMAND_ID_ZOOM_FIT_GRAPH).getAction());
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
		for (int i = 0; i < listeners.length; ++i) {
			final IBarSelectionListener listener = (IBarSelectionListener) listeners[i];
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
		for (int i = 0; i < listeners.length; ++i) {
			final IBarSelectionListener listener = (IBarSelectionListener) listeners[i];
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.selectionChanged(serieIndex, valueIndex);
				}
			});
		}
	}

	private void fireChartMouseEvent(final ChartMouseEvent mouseEvent) {

		final Object[] listeners = _mouseListener.getListeners();
		for (int i = 0; i < listeners.length; ++i) {

			final Object listener = listeners[i];

			switch (mouseEvent.type) {
			case Chart.MouseMove:
				((IMouseListener) listener).mouseMove(mouseEvent);
				break;

			case Chart.MouseDownPre:
				((IMouseListener) listener).mouseDown(mouseEvent);
				break;

//			case Chart.MouseDownPost:
//				((IMouseListener) listener).mouseDownPost(mouseEvent);
//				break;

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

	void fireDoubleClick() {

		final Object[] listeners = _doubleClickListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final Listener listener = (Listener) listeners[i];
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.handleEvent(null);
				}
			});
		}
	}

	void fireFocusEvent() {

		final Object[] listeners = _focusListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final Listener listener = (Listener) listeners[i];
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
		for (int i = 0; i < listeners.length; ++i) {
			final ISliderMoveListener listener = (ISliderMoveListener) listeners[i];
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.sliderMoved(chartInfo);
				}
			});
		}
	}

	public boolean getAdvancedGraphics() {
		return _chartComponents._useAdvancedGraphics;
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

	protected ChartComponents getChartComponents() {
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

	public int getDevGraphImageXOffset() {
		return _chartComponents.getChartComponentGraph().getXXDevViewPortOffset();
	}

	/**
	 * @return Returns the left slider
	 */
	public ChartXSlider getLeftSlider() {
		return _chartComponents.getChartComponentGraph().getLeftSlider();
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

		switch (_chartDataModel.getChartType()) {
		case ChartDataModel.CHART_TYPE_BAR:
			return new SelectionBarChart(_barSelectionSerieIndex, _barSelectionValueIndex);

		case ChartDataModel.CHART_TYPE_LINE:
			break;

		default:
			break;
		}

		return null;
	}

	/**
	 * @return Returns <code>true</code> to start the bars at the bottom of the chart
	 */
	boolean getStartAtChartBottom() {
		return _isDrawBarChartAtBottom;
	}

//	boolean isMouseDownExternalPost(final int devXMouse, final int devYMouse, final int devXGraph) {
//
//		final ChartMouseEvent event = new ChartMouseEvent(Chart.MouseDownPost);
//
//		event.devXMouse = devXMouse;
//		event.devYMouse = devYMouse;
//		event.devMouseXInGraph = devXGraph;
//
//		fireChartMouseEvent(event);
//
//		return event.isWorked;
//	}

	/**
	 * Returns the toolbar for the chart, if no toolbar manager is set with setToolbarManager, the
	 * manager will be created and the toolbar is on top of the chart
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

			_useInternalActionBar = true;
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

	/**
	 * returns the value index for the x-sliders
	 */
	public SelectionChartXSliderPosition getXSliderPosition() {

		final ChartComponentGraph chartGraph = _chartComponents.getChartComponentGraph();

		return new SelectionChartXSliderPosition(this, chartGraph.getLeftSlider().getValuesIndex(), chartGraph
				.getRightSlider()
				.getValuesIndex());
	}

	boolean isMouseDownExternalPre(final int devXMouse, final int devYMouse) {

		final ChartMouseEvent event = new ChartMouseEvent(Chart.MouseDownPre);

		event.devXMouse = devXMouse;
		event.devYMouse = devYMouse;

		fireChartMouseEvent(event);

		return event.isWorked;
	}

	boolean isMouseMoveExternal(final int devXMouse, final int devYMouse) {

		final ChartMouseEvent event = new ChartMouseEvent(Chart.MouseMove);

		event.devXMouse = devXMouse;
		event.devYMouse = devYMouse;

		fireChartMouseEvent(event);

		return event.isWorked;
	}

	boolean isMouseUpExternal(final int devXMouse, final int devYMouse) {

		final ChartMouseEvent event = new ChartMouseEvent(Chart.MouseUp);

		event.devXMouse = devXMouse;
		event.devYMouse = devYMouse;

		fireChartMouseEvent(event);

		return event.isWorked;
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

	public void removeDoubleClickListener(final IBarSelectionListener listener) {
		_barDoubleClickListeners.remove(listener);
	}

	public void removeDoubleClickListener(final Listener listener) {
		_doubleClickListeners.remove(listener);
	}

	public void removeFocusListener(final Listener listener) {
		_focusListeners.remove(listener);
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

	/**
	 * Set the enable state for a command and update the UI
	 */
	public void setChartCommandEnabled(final String commandId, final boolean isEnabled) {

		_chartActionProxies.get(commandId).setEnabled(isEnabled);

		if (_useActionHandlers) {
			_actionHandlerManager.getActionHandler(commandId).fireHandlerChanged();
		}
	}

	public void setContextProvider(final IChartContextProvider chartContextProvider) {
		_chartContextProvider = chartContextProvider;
	}

	/**
	 * @param chartContextProvider
	 * @param isFirstContextMenu
	 *            when <code>true</code> the context menu will be positioned before the chart menu
	 *            items
	 */
	public void setContextProvider(final IChartContextProvider chartContextProvider, final boolean isFirstContextMenu) {

		_chartContextProvider = chartContextProvider;
		_isFirstContextMenu = isFirstContextMenu;
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

		final ChartDataModel emptyModel = new ChartDataModel(ChartDataModel.CHART_TYPE_LINE);

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

	/**
	 * Sets the mouse mode, when <code>true</code> the mode {@link #MOUSE_MODE_SLIDER} is active,
	 * this is the default
	 * 
	 * @param isChecked
	 */
	public void setMouseMode(final boolean isChecked) {

		_mouseMode = isChecked ? MOUSE_MODE_SLIDER : MOUSE_MODE_ZOOM;

		updateMouseModeUIState();

		_chartComponents.getChartComponentGraph().setCursorStyle();

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
		_chartActionProxies.get(COMMAND_ID_ZOOM_IN).setEnabled(isEnabled);
		_chartActionProxies.get(COMMAND_ID_ZOOM_OUT).setEnabled(isEnabled);
		_chartActionProxies.get(COMMAND_ID_ZOOM_FIT_GRAPH).setEnabled(isEnabled);
		_actionHandlerManager.updateUIState();
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

		if (chartDataModel == null || (chartDataModel != null && chartDataModel.getYData().isEmpty())) {

			final ChartDataModel emptyModel = new ChartDataModel(ChartDataModel.CHART_TYPE_LINE);

			if (chartDataModel != null) {
				String errorMessage = chartDataModel.getErrorMessage();
				if (errorMessage == null) {
					/*
					 * error message is disabled because it confuses the user because it is
					 * displayed when a graph is not displayed but another graph could be selected
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
	 * Update all action handlers from their action proxy and update the UI state
	 */
	public void updateChartActionHandlers() {
		_actionHandlerManager.updateActionHandlers(this);
	}

	/**
	 * Updates only the custom layers which performce much faster than a chart update.
	 */
	public void updateCustomLayers() {
		_chartComponents.updateCustomLayers();
	}

	private void updateMouseModeUIState() {

		if (_chartActionProxies != null) {
			_chartActionProxies.get(COMMAND_ID_MOUSE_MODE).setChecked(_mouseMode.equals(MOUSE_MODE_SLIDER));
		}

		if (_useActionHandlers) {
			_actionHandlerManager.updateUIState();
		}
	}

	/**
	 * @return Returns <code>true</code> when action handlers are used for this chart
	 */
	public boolean useActionHandlers() {
		return _useActionHandlers;
	}

	/**
	 * @return Returns <code>true</code> when the internal action bar is used, returns
	 *         <code>false</code> when the global action handler are used
	 */
	public boolean useInternalActionBar() {
		return _useInternalActionBar;
	}

}
