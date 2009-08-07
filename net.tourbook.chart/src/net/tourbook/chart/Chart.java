/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;
import java.util.HashMap;

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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
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

//	static final String					COMMAND_ID_PART_NEXT				= "net.tourbook.chart.command.partNext";			//$NON-NLS-1$
//	static final String					COMMAND_ID_PART_PREVIOUS			= "net.tourbook.chart.command.partPrevious";		//$NON-NLS-1$

	private static final String			COMMAND_ID_MOVE_LEFT_SLIDER_HERE	= "net.tourbook.chart.command.moveLeftSliderHere";	//$NON-NLS-1$
	private static final String			COMMAND_ID_MOVE_RIGHT_SLIDER_HERE	= "net.tourbook.chart.command.moveRightSliderHere"; //$NON-NLS-1$
	private static final String			COMMAND_ID_MOVE_SLIDERS_TO_BORDER	= "net.tourbook.chart.command.moveSlidersToBorder"; //$NON-NLS-1$

	static final int					NO_BAR_SELECTION					= -1;

	public static final int				SYNCH_MODE_NO						= 0;
	public static final int				SYNCH_MODE_BY_SCALE					= 1;
	public static final int				SYNCH_MODE_BY_SIZE					= 2;

	static final int					GRAPH_ALPHA							= 0xd0;

	public static final String			MOUSE_MODE_SLIDER					= "slider";										//$NON-NLS-1$
	public static final String			MOUSE_MODE_ZOOM						= "zoom";											//$NON-NLS-1$

	private static final int			MouseMove							= 10;
	private static final int			MouseDownPre						= 20;
//	private static final int			MouseDownPost						= 21;
	private static final int			MouseUp								= 30;
	private static final int			MouseDoubleClick					= 40;

	private final ListenerList			fFocusListeners						= new ListenerList();
	private final ListenerList			fBarSelectionListeners				= new ListenerList();
	private final ListenerList			fSliderMoveListeners				= new ListenerList();
	private final ListenerList			fBarDoubleClickListeners			= new ListenerList();
	private final ListenerList			fDoubleClickListeners				= new ListenerList();
	private final ListenerList			fMouseListener						= new ListenerList();

	ChartComponents						fChartComponents;
	private ChartDataModel				fChartDataModel;
	private IToolBarManager				fToolbarMgr;
	private IChartContextProvider		fChartContextProvider;

	private Chart						fSynchedChart;

	private boolean						fIsShowZoomActions					= false;
	private boolean						fIsShowMouseMode					= false;

	private Color						fBackgroundColor;

	/**
	 * listener which is called when the x-marker was dragged
	 */
	IChartListener						fXMarkerDraggingListener;

	/**
	 * when set to <code>true</code> the toolbar is within the chart control, otherwise the toolbar
	 * is outsite of the chart
	 */
	boolean								fUseInternalActionBar				= true;

	boolean								fUseActionHandlers					= false;

	private int							fBarSelectionSerieIndex;
	private int							fBarSelectionValueIndex;

	private final ActionHandlerManager	fActionHandlerManager				= ActionHandlerManager.getInstance();
	HashMap<String, ActionProxy>		fChartActionProxies;

	private boolean						fIsFillToolbar						= true;
	private boolean						fIsToolbarCreated;

	int									fSynchMode;

	/**
	 * <code>true</code> to start the bar chart at the bottom of the chart
	 */
	private boolean						fDrawBarChartAtBottom				= true;

	/**
	 * minimum width in pixel for one unit, this is only an approximate value because the pixel is
	 * rounded up or down to fit a rounded unit
	 */
	protected int						gridVerticalDistance				= 30;
	protected int						gridHorizontalDistance				= 70;

	/**
	 * mouse behaviour:<br>
	 * <br>{@link #MOUSE_MODE_SLIDER} or {@link #MOUSE_MODE_ZOOM}
	 */
	private String						fMouseMode							= MOUSE_MODE_SLIDER;
	private boolean						fIsFirstContextMenu;

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

		fChartComponents = new ChartComponents(this, style);
		setContent(fChartComponents);

		// set the default background color
		fBackgroundColor = getDisplay().getSystemColor(SWT.COLOR_WHITE);
	}

	public void addBarSelectionListener(final IBarSelectionListener listener) {
		fBarSelectionListeners.add(listener);
	}

	public void addDoubleClickListener(final IBarSelectionListener listener) {
		fBarDoubleClickListeners.add(listener);
	}

	public void addDoubleClickListener(final Listener listener) {
		fDoubleClickListeners.add(listener);
	}

	public void addFocusListener(final Listener listener) {
		fFocusListeners.add(listener);
	}

	public void addMouseListener(final IMouseListener mouseListener) {
		fMouseListener.add(mouseListener);
	}

	/**
	 * Adds a listener when the vertical slider is moved
	 * 
	 * @param listener
	 */
	public void addSliderMoveListener(final ISliderMoveListener listener) {
		fSliderMoveListeners.add(listener);
	}

	public void addXMarkerDraggingListener(final IChartListener xMarkerDraggingListener) {
		fXMarkerDraggingListener = xMarkerDraggingListener;
	}

	/**
	 * create zoom/navigation actions which are managed by the chart
	 */
	private void createActions() {

		createChartActionProxies();

		if (fIsFillToolbar && fIsToolbarCreated == false) {
			fIsToolbarCreated = true;
			fillToolbar(true);
		}
	}

	/**
	 * Creates the handlers for the chart actions
	 */
	public void createChartActionHandlers() {

		// use the commands defined in plugin.xml
		fUseActionHandlers = true;

		fActionHandlerManager.createActionHandlers();
		createChartActionProxies();
	}

	/**
	 * Creates the action proxys for all chart actions
	 */
	private void createChartActionProxies() {

		// create actions only once
		if (fChartActionProxies != null) {
			return;
		}

		fChartActionProxies = new HashMap<String, ActionProxy>();

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
		fChartActionProxies.put(COMMAND_ID_ZOOM_IN, actionProxy);

		/*
		 * Action: zoom in to slider
		 */
		if (useInternalActionBar) {
			action = new ActionZoomToSlider(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_ZOOM_IN_TO_SLIDER, action);
		fChartActionProxies.put(COMMAND_ID_ZOOM_IN_TO_SLIDER, actionProxy);

		/*
		 * Action: zoom out
		 */
		if (useInternalActionBar) {
			action = new ActionZoomOut(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_ZOOM_OUT, action);
		fChartActionProxies.put(COMMAND_ID_ZOOM_OUT, actionProxy);

		/*
		 * Action: fit graph to window
		 */
		if (useInternalActionBar) {
			action = new ActionFitGraph(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_ZOOM_FIT_GRAPH, action);
		fChartActionProxies.put(COMMAND_ID_ZOOM_FIT_GRAPH, actionProxy);

		/*
		 * Action: mouse moude
		 */
		if (useInternalActionBar) {
			action = new ActionMouseMode(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_MOUSE_MODE, action);
		fChartActionProxies.put(COMMAND_ID_MOUSE_MODE, actionProxy);

		/*
		 * Action: move sliders when
		 */
		if (useInternalActionBar) {
			action = new ActionMoveSlidersToBorder(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_MOVE_SLIDERS_TO_BORDER, action);
		fChartActionProxies.put(COMMAND_ID_MOVE_SLIDERS_TO_BORDER, actionProxy);

		/*
		 * Action: move left slider here
		 */
		if (useInternalActionBar) {
			action = new ActionMoveLeftSliderHere(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_MOVE_LEFT_SLIDER_HERE, action);
		fChartActionProxies.put(COMMAND_ID_MOVE_LEFT_SLIDER_HERE, actionProxy);

		/*
		 * Action: move right slider here
		 */
		if (useInternalActionBar) {
			action = new ActionMoveRightSliderHere(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_MOVE_RIGHT_SLIDER_HERE, action);
		fChartActionProxies.put(COMMAND_ID_MOVE_RIGHT_SLIDER_HERE, actionProxy);

		enableActions();
	}

	/**
	 * @return
	 */
	private SelectionChartInfo createChartInfo() {

		if (fChartComponents == null) {
			return null;
		}

		final SelectionChartInfo chartInfo = new SelectionChartInfo(this);

		chartInfo.chartDataModel = fChartDataModel;
		chartInfo.chartDrawingData = fChartComponents.getChartDrawingData();

		final ChartComponentGraph chartGraph = fChartComponents.getChartComponentGraph();
		chartInfo.leftSliderValuesIndex = chartGraph.getLeftSlider().getValuesIndex();
		chartInfo.rightSliderValuesIndex = chartGraph.getRightSlider().getValuesIndex();
		chartInfo.selectedSliderValuesIndex = chartGraph.getSelectedSlider().getValuesIndex();

		return chartInfo;
	}

	void enableActions() {

		if (fChartActionProxies == null) {
			return;
		}

		final ChartComponentGraph chartComponentGraph = fChartComponents.getChartComponentGraph();

		final boolean canZoomOut = chartComponentGraph.getGraphZoomRatio() > 1;
		final boolean canZoomIn = chartComponentGraph.getDevVirtualGraphImageWidth() < ChartComponents.CHART_MAX_WIDTH;

		fChartActionProxies.get(COMMAND_ID_ZOOM_IN).setEnabled(canZoomIn);
		fChartActionProxies.get(COMMAND_ID_ZOOM_OUT).setEnabled(canZoomOut);

		// zoom in to slider has no limits but when there are more than 10000 units, the units are not displayed
		fChartActionProxies.get(COMMAND_ID_ZOOM_IN_TO_SLIDER).setEnabled(true);

		// fit to graph is always enabled because the y-slider can change the chart
		fChartActionProxies.get(COMMAND_ID_ZOOM_FIT_GRAPH).setEnabled(true);

		if (fUseActionHandlers) {
			fActionHandlerManager.updateUIState();
		}

	}

	void fillContextMenu(	final IMenuManager menuMgr,
							final ChartXSlider leftSlider,
							final ChartXSlider rightSlider,
							final int hoveredBarSerieIndex,
							final int hoveredBarValueIndex,
							final int mouseDownDevPositionX,
							final int mouseDownDevPositionY) {

		if (fChartActionProxies == null) {
			return;
		}

		// check if this is slider context
		final boolean isSliderContext = leftSlider != null || rightSlider != null;
		final boolean showOnlySliderContext = isSliderContext && fChartContextProvider.showOnlySliderContextMenu();

		if (fChartContextProvider != null && showOnlySliderContext == false && fIsFirstContextMenu) {
			fChartContextProvider.fillContextMenu(menuMgr, mouseDownDevPositionX, mouseDownDevPositionY);
		}

		if (fChartDataModel.getChartType() == ChartDataModel.CHART_TYPE_BAR) {

			// create menu for bar charts

			// get the context provider from the data model
			final IChartContextProvider barChartContextProvider = (IChartContextProvider) fChartDataModel.getCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER);

			if (barChartContextProvider != null) {
				barChartContextProvider.fillBarChartContextMenu(menuMgr, hoveredBarSerieIndex, hoveredBarValueIndex);
			}

		} else {

			// create menu for line charts

			// set text for mouse wheel mode
			final Action actionMouseMode = fChartActionProxies.get(COMMAND_ID_MOUSE_MODE).getAction();
			if (fMouseMode.equals(MOUSE_MODE_SLIDER)) {
				// mouse mode: slider
				actionMouseMode.setText(Messages.Action_mouse_mode_zoom);

			} else {
				// mouse mode: zoom
				actionMouseMode.setText(Messages.Action_mouse_mode_slider);
			}

			// fill slider context menu
			if (fChartContextProvider != null) {
				menuMgr.add(new Separator());
				fChartContextProvider.fillXSliderContextMenu(menuMgr, leftSlider, rightSlider);
			}

			menuMgr.add(new Separator());
			menuMgr.add(actionMouseMode);
			menuMgr.add(fChartActionProxies.get(COMMAND_ID_MOVE_SLIDERS_TO_BORDER).getAction());
			menuMgr.add(fChartActionProxies.get(COMMAND_ID_MOVE_LEFT_SLIDER_HERE).getAction());
			menuMgr.add(fChartActionProxies.get(COMMAND_ID_MOVE_RIGHT_SLIDER_HERE).getAction());
			menuMgr.add(fChartActionProxies.get(COMMAND_ID_ZOOM_IN_TO_SLIDER).getAction());
		}

		if (fChartContextProvider != null && showOnlySliderContext == false && fIsFirstContextMenu == false) {
			menuMgr.add(new Separator());
			fChartContextProvider.fillContextMenu(menuMgr, mouseDownDevPositionX, mouseDownDevPositionY);
		}
	}

	/**
	 * put the actions into the internal toolbar
	 * 
	 * @param refreshToolbar
	 */
	public void fillToolbar(final boolean refreshToolbar) {

		if (fChartActionProxies == null) {
			return;
		}

		if (fUseInternalActionBar && (fIsShowZoomActions || fIsShowMouseMode)) {

			// add the action to the toolbar
			final IToolBarManager tbm = getToolBarManager();

			if (fIsShowZoomActions) {

				tbm.add(new Separator());

				if (fIsShowMouseMode) {
					tbm.add(fChartActionProxies.get(COMMAND_ID_MOUSE_MODE).getAction());
				}

				tbm.add(fChartActionProxies.get(COMMAND_ID_ZOOM_IN).getAction());
				tbm.add(fChartActionProxies.get(COMMAND_ID_ZOOM_OUT).getAction());

				if (fChartDataModel.getChartType() != ChartDataModel.CHART_TYPE_BAR) {
					tbm.add(fChartActionProxies.get(COMMAND_ID_ZOOM_FIT_GRAPH).getAction());
				}
			}

			if (refreshToolbar) {
				tbm.update(true);
			}
		}
	}

	void fireBarSelectionEvent(final int serieIndex, final int valueIndex) {

		fBarSelectionSerieIndex = serieIndex;
		fBarSelectionValueIndex = valueIndex;

		final Object[] listeners = fBarSelectionListeners.getListeners();
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

		fBarSelectionSerieIndex = serieIndex;
		fBarSelectionValueIndex = valueIndex;

		final Object[] listeners = fBarDoubleClickListeners.getListeners();
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

		final Object[] listeners = fMouseListener.getListeners();
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

		final Object[] listeners = fDoubleClickListeners.getListeners();
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

		final Object[] listeners = fFocusListeners.getListeners();
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

		final Object[] listeners = fSliderMoveListeners.getListeners();
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
		return fChartComponents.useAdvancedGraphics;
	}

	public Color getBackgroundColor() {
		return fBackgroundColor;
	}

	public Boolean getCanAutoMoveSliders() {
		return fChartComponents.getChartComponentGraph().canAutoMoveSliders;
	}

	public boolean getCanAutoZoomToSlider() {
		return fChartComponents.getChartComponentGraph().canAutoZoomToSlider;
	}

	public boolean getCanScrollZoomedChart() {
		return fChartComponents.getChartComponentGraph().canScrollZoomedChart;
	}

	/**
	 * @return Returns the data model for the chart
	 */
	public ChartDataModel getChartDataModel() {
		return fChartDataModel;
	}

	public ArrayList<ChartDrawingData> getChartDrawingData() {
		return fChartComponents.getChartDrawingData();
	}

	/**
	 * Return information about the chart
	 * 
	 * @return
	 */
	public SelectionChartInfo getChartInfo() {
		return createChartInfo();
	}

	public ChartProperties getChartProperties() {
		return fChartComponents.getChartProperties();
	}

	public int getDevGraphImageXOffset() {
		return fChartComponents.getChartComponentGraph().getDevGraphImageXOffset();
	}

	/**
	 * @return Returns the left slider
	 */
	public ChartXSlider getLeftSlider() {
		return fChartComponents.getChartComponentGraph().getLeftSlider();
	}

	public String getMouseMode() {
		return fMouseMode;
	}

	/**
	 * @return Return the right slider
	 */
	public ChartXSlider getRightSlider() {
		return fChartComponents.getChartComponentGraph().getRightSlider();
	}

	public ISelection getSelection() {

		if (fChartDataModel == null) {
			return null;
		}

		switch (fChartDataModel.getChartType()) {
		case ChartDataModel.CHART_TYPE_BAR:
			return new SelectionBarChart(fBarSelectionSerieIndex, fBarSelectionValueIndex);

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
		return fDrawBarChartAtBottom;
	}

	/**
	 * Returns the toolbar for the chart, if no toolbar manager is set with setToolbarManager, the
	 * manager will be created and the toolbar is on top of the chart
	 * 
	 * @return
	 */
	public IToolBarManager getToolBarManager() {

		if (fToolbarMgr == null) {

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
			fToolbarMgr = new ToolBarManager(toolBarControl);

			fUseInternalActionBar = true;
		}

		return fToolbarMgr;
	}

	/**
	 * returns the value index for the x-sliders
	 */
	public SelectionChartXSliderPosition getXSliderPosition() {

		final ChartComponentGraph chartGraph = fChartComponents.getChartComponentGraph();

		return new SelectionChartXSliderPosition(this,
				chartGraph.getLeftSlider().getValuesIndex(),
				chartGraph.getRightSlider().getValuesIndex());
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

	boolean isMouseDownExternalPre(final int devXMouse, final int devYMouse, final int devXGraph) {

		final ChartMouseEvent event = new ChartMouseEvent(Chart.MouseDownPre);

		event.devXMouse = devXMouse;
		event.devYMouse = devYMouse;
		event.devMouseXInGraph = devXGraph;

		fireChartMouseEvent(event);

		return event.isWorked;
	}

	boolean isMouseMoveExternal(final int devXMouse, final int devYMouse, final int devXGraph) {

		final ChartMouseEvent event = new ChartMouseEvent(Chart.MouseMove);

		event.devXMouse = devXMouse;
		event.devYMouse = devYMouse;
		event.devMouseXInGraph = devXGraph;

		fireChartMouseEvent(event);

		return event.isWorked;
	}

	boolean isMouseUpExternal(final int devXMouse, final int devYMouse, final int devXGraph) {

		final ChartMouseEvent event = new ChartMouseEvent(Chart.MouseUp);

		event.devXMouse = devXMouse;
		event.devYMouse = devYMouse;
		event.devMouseXInGraph = devXGraph;

		fireChartMouseEvent(event);

		return event.isWorked;
	}

	/**
	 * @return Returns <code>true</code> when the x-sliders are visible
	 */
	public boolean isXSliderVisible() {
		return fChartComponents.devSliderBarHeight != 0;
	}

	void onExecuteMouseMode(final boolean isChecked) {
		setMouseMode(isChecked);
	}

	void onExecuteMoveLeftSliderHere() {
		fChartComponents.getChartComponentGraph().moveLeftSliderHere();
	}

	void onExecuteMoveRightSliderHere() {
		fChartComponents.getChartComponentGraph().moveRightSliderHere();
	}

	public void onExecuteMoveSlidersToBorder() {
		fChartComponents.getChartComponentGraph().moveSlidersToBorderWithoutCheck();
	}

	protected void onExecuteZoomFitGraph() {

		fChartDataModel.resetMinMaxValues();

		fChartComponents.getChartComponentGraph().zoomOutFitGraph();
	}

	void onExecuteZoomIn() {

		if (fChartComponents.devSliderBarHeight == 0) {
			fChartComponents.getChartComponentGraph().zoomInWithoutSlider();
			fChartComponents.onResize();
		} else {
			fChartComponents.getChartComponentGraph().zoomInWithMouse();
		}
	}

	/**
	 * Zoom to the vertical sliders
	 */
	public void onExecuteZoomInWithSlider() {

		fChartComponents.getChartComponentGraph().zoomInWithSlider();
		fChartComponents.onResize();
	}

	public void onExecuteZoomOut(final boolean updateChart) {

		if (fChartDataModel == null) {
			return;
		}

		fChartComponents.getChartComponentGraph().zoomOutWithMouse(updateChart);
	}

	/**
	 * make the graph dirty and redraw it
	 */
	public void redrawChart() {
		fChartComponents.getChartComponentGraph().redrawChart();
	}

	public void removeDoubleClickListener(final IBarSelectionListener listener) {
		fBarDoubleClickListeners.remove(listener);
	}

	public void removeDoubleClickListener(final Listener listener) {
		fDoubleClickListeners.remove(listener);
	}

	public void removeFocusListener(final Listener listener) {
		fFocusListeners.remove(listener);
	}

	public void removeSelectionChangedListener(final IBarSelectionListener listener) {
		fBarSelectionListeners.remove(listener);
	}

	public void resetGraphAlpha() {
		fChartComponents.getChartComponentGraph().fGraphAlpha = GRAPH_ALPHA;
	}

	/**
	 * Set the background color for the chart, the default is SWT.COLOR_WHITE
	 * 
	 * @param backgroundColor
	 *            The backgroundColor to set.
	 */
	public void setBackgroundColor(final Color backgroundColor) {
		this.fBackgroundColor = backgroundColor;
	}

	/**
	 * Set the option to move the sliders to the border when the chart is zoomed
	 * 
	 * @param canMoveSlidersWhenZoomed
	 */
	public void setCanAutoMoveSliders(final boolean canMoveSlidersWhenZoomed) {
		fChartComponents.getChartComponentGraph().setCanAutoMoveSlidersWhenZoomed(canMoveSlidersWhenZoomed);
	}

	/**
	 * set the option to auto zoom the chart
	 * 
	 * @param canZoomToSliderOnMouseUp
	 */
	public void setCanAutoZoomToSlider(final boolean canZoomToSliderOnMouseUp) {
		fChartComponents.getChartComponentGraph().setCanAutoZoomToSlider(canZoomToSliderOnMouseUp);
	}

	/**
	 * set the option to scroll/not scroll the zoomed chart
	 * 
	 * @param canScrollabelZoomedGraph
	 */
	public void setCanScrollZoomedChart(final boolean canScrollabelZoomedGraph) {
		fChartComponents.getChartComponentGraph().setCanScrollZoomedChart(canScrollabelZoomedGraph);
	}

	/**
	 * Set the enable state for a command and update the UI
	 */
	public void setChartCommandEnabled(final String commandId, final boolean isEnabled) {

		fChartActionProxies.get(commandId).setEnabled(isEnabled);

		if (fUseActionHandlers) {
			fActionHandlerManager.getActionHandler(commandId).fireHandlerChanged();
		}
	}

//	/**
//	 * Set <code>true</code> when the internal action bar should be used, set <code>false</code>
//	 * when the workbench action should be used.
//	 * 
//	 * @param useInternalActionBar
//	 */
//	public void setUseInternalActionBar(boolean useInternalActionBar) {
//		fUseInternalActionBar = useInternalActionBar;
//	}

	public void setContextProvider(final IChartContextProvider chartContextProvider) {
		fChartContextProvider = chartContextProvider;
	}

	/**
	 * @param chartContextProvider
	 * @param isFirstContextMenu
	 *            when <code>true</code> the context menu will be positioned before the chart menu
	 *            items
	 */
	public void setContextProvider(final IChartContextProvider chartContextProvider, final boolean isFirstContextMenu) {

		fChartContextProvider = chartContextProvider;
		fIsFirstContextMenu = isFirstContextMenu;
	}

	protected void setDataModel(final ChartDataModel chartDataModel) {
		fChartDataModel = chartDataModel;
	}

	/**
	 * Set <code>false</code> to not draw the bars at the bottom of the chart
	 * 
	 * @param fDrawBarCharttAtBottom
	 */
	public void setDrawBarChartAtBottom(final boolean fDrawBarCharttAtBottom) {
		this.fDrawBarChartAtBottom = fDrawBarCharttAtBottom;
	}

	@Override
	public boolean setFocus() {

		/*
		 * set focus to the graph component
		 */
		return fChartComponents.getChartComponentGraph().setFocus();
	}

	/**
	 * Sets the alpha value for the filling operation
	 * 
	 * @param alphaValue
	 */
	public void setGraphAlpha(final int alphaValue) {
		fChartComponents.getChartComponentGraph().fGraphAlpha = alphaValue;
	}

	public void setGridDistance(final int horizontalGrid, final int verticalGrid) {

		gridVerticalDistance = verticalGrid;
		gridHorizontalDistance = horizontalGrid;

		fChartComponents.onResize();
	}

	/**
	 * Sets the mouse mode, when <code>true</code> the mode {@link #MOUSE_MODE_SLIDER} is active,
	 * this is the default
	 * 
	 * @param isChecked
	 */
	public void setMouseMode(final boolean isChecked) {

		fMouseMode = isChecked ? MOUSE_MODE_SLIDER : MOUSE_MODE_ZOOM;

		updateMouseModeUIState();

		fChartComponents.getChartComponentGraph().setDefaultCursor();

	}

//	public void setShowPartNavigation(final boolean showPartNavigation) {
//		fShowPartNavigation = showPartNavigation;
//	}

	public void setMouseMode(final Object newMouseMode) {

		if (newMouseMode instanceof String) {

			fMouseMode = (String) newMouseMode;

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
		fBarSelectionSerieIndex = 0;
		fBarSelectionValueIndex = 0;

		if (selectedItems != null) {

			// get selected bar
			for (int itemIndex = 0; itemIndex < selectedItems.length; itemIndex++) {
				if (selectedItems[itemIndex]) {
					fBarSelectionValueIndex = itemIndex;
					break;
				}
			}
		}

		fChartComponents.getChartComponentGraph().setSelectedBars(selectedItems);

		fireBarSelectionEvent(0, fBarSelectionValueIndex);
	}

	/**
	 * @param isMarkerVisible
	 *            <code>true</code> shows the marker area
	 */
	public void setShowMarker(final boolean isMarkerVisible) {
		fChartComponents.setMarkerVisible(isMarkerVisible);
	}

	/**
	 * Make the mouse mode button visible
	 */
	public void setShowMouseMode() {
		fIsShowMouseMode = true;
	}

	/**
	 * @param isSliderVisible
	 *            <code>true</code> shows the sliders
	 */
	public void setShowSlider(final boolean isSliderVisible) {
		fChartComponents.setSliderVisible(isSliderVisible);
	}

	public void setShowZoomActions(final boolean isShowZoomActions) {
		fIsShowZoomActions = isShowZoomActions;
	}

	/**
	 * set the synch configuration which is used when the chart is drawn/resized
	 * 
	 * @param synchConfigIn
	 *            set <code>null</code> to disable the synchronization
	 */
	public void setSynchConfig(final SynchConfiguration synchConfigIn) {
		fChartComponents.setSynchConfig(synchConfigIn);
	}

	/**
	 * Set's the {@link SynchConfiguration} listener, this is a {@link Chart} which will be notified
	 * when this chart is resized, <code>null</code> will disable the synchronisation
	 * 
	 * @param chartWidget
	 */
	public void setSynchedChart(final Chart chartWidget) {
		fSynchedChart = chartWidget;
	}

	protected void setSynchMode(final int synchMode) {
		fSynchMode = synchMode;
	}

	/**
	 * @param toolbarMgr
	 * @param isFillToolbar
	 *            set <code>false</code> when the toolbar will be filled with
	 *            {@link Chart#fillToolbar(boolean)} from externally, when <code>true</code> the
	 *            toolbar will be filled when the chart is updated
	 */
	public void setToolBarManager(final IToolBarManager toolbarMgr, final boolean isFillToolbar) {
		fToolbarMgr = toolbarMgr;
		fIsFillToolbar = isFillToolbar;
	}

	/**
	 * sets the position of the x-sliders
	 * 
	 * @param sliderPosition
	 */
	public void setXSliderPosition(final SelectionChartXSliderPosition sliderPosition) {

		// check if the position is for this chart
		if (sliderPosition.getChart() == this) {
			fChartComponents.setXSliderPosition(sliderPosition);
		}
	}

	/**
	 * Enable/disable the zoom in/out action
	 * 
	 * @param isEnabled
	 */
	public void setZoomActionsEnabled(final boolean isEnabled) {
		fChartActionProxies.get(COMMAND_ID_ZOOM_IN).setEnabled(isEnabled);
		fChartActionProxies.get(COMMAND_ID_ZOOM_OUT).setEnabled(isEnabled);
		fChartActionProxies.get(COMMAND_ID_ZOOM_FIT_GRAPH).setEnabled(isEnabled);
		fActionHandlerManager.updateUIState();
	}

	public void switchSlidersTo2ndXData() {
		fChartComponents.getChartComponentGraph().switchSlidersTo2ndXData();
	}

	/**
	 * synchronize the charts
	 */
	protected void synchronizeChart() {

		if (fSynchedChart == null) {
			return;
		}

		getDisplay().asyncExec(new Runnable() {
			public void run() {
				fSynchedChart.setSynchConfig(fChartComponents.fSynchConfigOut);
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

			fChartDataModel = emptyModel;
			fChartComponents.setModel(emptyModel, false);

			return;
		}

		fChartDataModel = chartDataModel;

		createActions();
		fChartComponents.setModel(chartDataModel, isShowAllData);

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
		fActionHandlerManager.updateActionHandlers(this);
	}

	public void updateChartLayers() {
		fChartComponents.updateChartLayers();
	}

	private void updateMouseModeUIState() {

		if (fChartActionProxies != null) {
			fChartActionProxies.get(COMMAND_ID_MOUSE_MODE).setChecked(fMouseMode.equals(MOUSE_MODE_SLIDER));
		}

		if (fUseActionHandlers) {
			fActionHandlerManager.updateUIState();
		}
	}

	/**
	 * @return Returns <code>true</code> when action handlers are used for this chart
	 */
	public boolean useActionHandlers() {
		return fUseActionHandlers;
	}

	/**
	 * @return Returns <code>true</code> when the internal action bar is used, returns
	 *         <code>false</code> when the global action handler are used
	 */
	public boolean useInternalActionBar() {
		return fUseInternalActionBar;
	}

}
