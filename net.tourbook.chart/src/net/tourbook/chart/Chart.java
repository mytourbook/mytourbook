/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
import org.eclipse.swt.graphics.Image;
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

	static final String					COMMAND_ID_ZOOM_IN			= "net.tourbook.chart.command.zoomIn";			//$NON-NLS-1$
	static final String					COMMAND_ID_ZOOM_OUT			= "net.tourbook.chart.command.zoomOut";		//$NON-NLS-1$
	static final String					COMMAND_ID_FIT_GRAPH		= "net.tourbook.chart.command.fitGraph";		//$NON-NLS-1$

	static final String					COMMAND_ID_PART_NEXT		= "net.tourbook.chart.command.partNext";		//$NON-NLS-1$
	static final String					COMMAND_ID_PART_PREVIOUS	= "net.tourbook.chart.command.partPrevious";	//$NON-NLS-1$

	static final int					NO_BAR_SELECTION			= -1;

	private final ListenerList			fFocusListeners				= new ListenerList();
	private final ListenerList			fBarSelectionListeners		= new ListenerList();
	private final ListenerList			fSliderMoveListeners		= new ListenerList();
	private final ListenerList			fBarDoubleClickListeners	= new ListenerList();
	private final ListenerList			fDoubleClickListeners		= new ListenerList();

	ChartComponents						fChartComponents;
	private ChartDataModel				fChartDataModel;
	private IToolBarManager				fToolbarMgr;
	private IChartContextProvider		fChartContextProvider;

	private Chart						fSynchedChart;

	private boolean						fShowZoomActions;
	private boolean						fShowPartNavigation;

	private Color						fBackgroundColor;

	/**
	 * listener which is called when the x-marker was dragged
	 */
	IChartListener						fXMarkerDraggingListener;

	/**
	 * when set to <code>true</code> the toolbar is within the chart control, otherwise the
	 * toolbar is outsite of the chart
	 */
	boolean								fUseInternalActionBar		= true;

	boolean								fUseActionHandlers			= false;

	private int							fBarSelectionSerieIndex;
	private int							fBarSelectionValueIndex;

	private final ActionHandlerManager	fActionHandlerManager		= ActionHandlerManager.getInstance();
	HashMap<String, ActionProxy>		fChartActionProxies;

	private boolean						fIsFillToolbar				= true;
	private boolean						fIsToolbarCreated;

	/**
	 * Chart widget
	 */
	public Chart(Composite parent, int style) {

		super(parent, style);
		setBorderVisible(false);

		// setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		GridLayout gl = new GridLayout(1, false);
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

	public void addBarSelectionListener(IBarSelectionListener listener) {
		fBarSelectionListeners.add(listener);
	}

	public void addDoubleClickListener(IBarSelectionListener listener) {
		fBarDoubleClickListeners.add(listener);
	}

	public void addDoubleClickListener(Listener listener) {
		fDoubleClickListeners.add(listener);
	}

	public void addFocusListener(Listener listener) {
		fFocusListeners.add(listener);
	}

	public void addSliderMoveListener(ISliderMoveListener listener) {
		fSliderMoveListeners.add(listener);
	}

	public void addXMarkerDraggingListener(IChartListener xMarkerDraggingListener) {
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
		actionProxy = new ActionProxy(COMMAND_ID_FIT_GRAPH, action);
		fChartActionProxies.put(COMMAND_ID_FIT_GRAPH, actionProxy);

		/*
		 * Action: previous part
		 */
		if (useInternalActionBar) {
			action = new ActionPartPrevious(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_PART_PREVIOUS, action);
		fChartActionProxies.put(COMMAND_ID_PART_PREVIOUS, actionProxy);

		/*
		 * Action: next part
		 */
		if (useInternalActionBar) {
			action = new ActionPartNext(this);
		} else {
			action = null;
		}
		actionProxy = new ActionProxy(COMMAND_ID_PART_NEXT, action);
		fChartActionProxies.put(COMMAND_ID_PART_NEXT, actionProxy);

		updateActionState();
	}

	/**
	 * @return
	 */
	private SelectionChartInfo createChartInfo() {

		if (fChartComponents == null) {
			return null;
		}

		SelectionChartInfo chartInfo = new SelectionChartInfo(this);

		chartInfo.chartDataModel = fChartDataModel;
		chartInfo.chartDrawingData = fChartComponents.getChartDrawingData();

		ChartComponentGraph chartGraph = fChartComponents.getChartComponentGraph();
		chartInfo.leftSliderValuesIndex = chartGraph.getLeftSlider().getValuesIndex();
		chartInfo.rightSliderValuesIndex = chartGraph.getRightSlider().getValuesIndex();

		return chartInfo;
	}

	void fillContextMenu(IMenuManager menuMgr) {
		if (fChartContextProvider != null) {
			fChartContextProvider.fillContextMenu(menuMgr);
		}
	}

	void fillContextMenu(	IMenuManager menuMgr,
							ChartXSlider leftSlider,
							ChartXSlider rightSlider,
							int hoveredBarSerieIndex,
							int hoveredBarValueIndex) {

		if (fChartContextProvider != null) {
			fChartContextProvider.fillContextMenu(menuMgr);
		}

		if (fChartDataModel.getChartType() == ChartDataModel.CHART_TYPE_BAR) {

			// get the context provider from the data model
			IChartContextProvider barChartContextProvider = (IChartContextProvider) fChartDataModel.getCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER);

			// create the menu for bar charts
			if (barChartContextProvider != null) {
				barChartContextProvider.fillBarChartContextMenu(menuMgr,
						hoveredBarSerieIndex,
						hoveredBarValueIndex);
			}

		} else {
			// create the menu for line charts
			if (fChartContextProvider != null) {
				fChartContextProvider.fillXSliderContextMenu(menuMgr, leftSlider, rightSlider);
			}
		}
	}

	/**
	 * put the actions into the internal toolbar
	 * 
	 * @param refreshToolbar
	 */
	public void fillToolbar(boolean refreshToolbar) {

		if (fChartActionProxies == null) {
			return;
		}

		if (fUseInternalActionBar && (fShowPartNavigation || fShowZoomActions)) {

			// add the action to the toolbar
			IToolBarManager tbm = getToolBarManager();

			if (fShowZoomActions) {
				tbm.add(new Separator());
				tbm.add(fChartActionProxies.get(COMMAND_ID_ZOOM_IN).getAction());
				tbm.add(fChartActionProxies.get(COMMAND_ID_ZOOM_OUT).getAction());
				tbm.add(fChartActionProxies.get(COMMAND_ID_FIT_GRAPH).getAction());
			}

			if (fShowPartNavigation) {
				tbm.add(new Separator());
				tbm.add(fChartActionProxies.get(COMMAND_ID_PART_PREVIOUS).getAction());
				tbm.add(fChartActionProxies.get(COMMAND_ID_PART_NEXT).getAction());
			}

			if (refreshToolbar) {
				tbm.update(true);
			}
		}
	}

	void fireBarSelectionEvent(final int serieIndex, final int valueIndex) {

		fBarSelectionSerieIndex = serieIndex;
		fBarSelectionValueIndex = valueIndex;

		Object[] listeners = fBarSelectionListeners.getListeners();
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

		Object[] listeners = fBarDoubleClickListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final IBarSelectionListener listener = (IBarSelectionListener) listeners[i];
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.selectionChanged(serieIndex, valueIndex);
				}
			});
		}
	}

	void fireDoubleClick() {

		Object[] listeners = fDoubleClickListeners.getListeners();
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

		Object[] listeners = fFocusListeners.getListeners();
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
//		System.out.println("fireSliderMove... " + this);
		Object[] listeners = fSliderMoveListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final ISliderMoveListener listener = (ISliderMoveListener) listeners[i];
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.sliderMoved(chartInfo);
				}
			});
		}
	}

	/**
	 * fire the current x-marker position which is in
	 * <code>chartComponents.xMarkerPositionOut</code>
	 */
	protected void fireSynchConfigListener() {

		if (fSynchedChart == null) {
			return;
		}

		getDisplay().asyncExec(new Runnable() {
			public void run() {
				fSynchedChart.setSynchConfigIn(fChartComponents.fSynchConfigOut);
			}
		});
	}

	public boolean getAdvancedGraphics() {
		return fChartComponents.useAdvancedGraphics;
	}

	public Color getBackgroundColor() {
		return fBackgroundColor;
	}

	public boolean getCanAutoZoomToSlider() {
		return fChartComponents.getChartComponentGraph().canAutoZoomToSlider;
	}

	public boolean getCanScrollZoomedChart() {
		return fChartComponents.getChartComponentGraph().canScrollZoomedChart;
	}

	public Image getChartCoreImage() {
		return fChartComponents.getChartComponentGraph().fGraphCoreImage;
	}

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

	/**
	 * @return Returns the data model for the chart
	 */
	public ChartDataModel getDataModel() {
		return fChartDataModel;
	}

	public int getDevGraphImageXOffset() {
		return fChartComponents.getChartComponentGraph().getDevGraphImageXOffset();
	}

	public ChartXSlider getLeftSlider() {
		return fChartComponents.getChartComponentGraph().getLeftSlider();
	}

	public ChartXSlider getRightSlider() {
		return fChartComponents.getChartComponentGraph().getRightSlider();
	}

	public ISelection getSelection() {

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
	 * Returns the toolbar for the chart, if no toolbar manager is set with setToolbarManager, the
	 * manager will be created and the toolbar is on top of the chart
	 * 
	 * @return
	 */
	public IToolBarManager getToolBarManager() {

		if (fToolbarMgr == null) {

			// create the toolbar and put it on top of the chart
			final ToolBar toolBarControl = new ToolBar(this, SWT.FLAT/* | SWT.WRAP */);
			setTopLeft(toolBarControl);

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

		ChartComponentGraph chartGraph = fChartComponents.getChartComponentGraph();

		return new SelectionChartXSliderPosition(this,
				chartGraph.getLeftSlider().getValuesIndex(),
				chartGraph.getRightSlider().getValuesIndex());
	}

	/**
	 * @return Returns <code>true</code> when the x-sliders are visible
	 */
	public boolean isXSliderVisible() {
		return fChartComponents.devSliderBarHeight != 0;
	}

	void onExecuteFitGraph() {
		fChartDataModel.resetMinMaxValues();
		zoomOut(true);
	}

	void onExecutePartNext() {
		fChartComponents.getChartComponentGraph().moveToNextPart();
	}

	void onExecutePartPrevious() {
		fChartComponents.getChartComponentGraph().moveToPrevPart();
	}

	void onExecuteZoomIn() {
		setCommandEnabled(COMMAND_ID_ZOOM_OUT, true);

		fChartComponents.zoomIn();
	}

	void onExecuteZoomOut() {

		fChartActionProxies.get(COMMAND_ID_ZOOM_IN).setEnabled(true);
		fChartActionProxies.get(COMMAND_ID_ZOOM_OUT).setEnabled(false);
		fChartActionProxies.get(COMMAND_ID_PART_PREVIOUS).setEnabled(false);
		fChartActionProxies.get(COMMAND_ID_PART_NEXT).setEnabled(false);

		if (fUseActionHandlers) {
			fActionHandlerManager.updateUIState();
		}

		fChartComponents.zoomOut(true);
	}

	/**
	 * make the graph dirty and redraw it
	 */
	public void redrawChart() {
		fChartComponents.getChartComponentGraph().redrawChart();
	}

	public void removeDoubleClickListener(IBarSelectionListener listener) {
		fBarDoubleClickListeners.remove(listener);
	}

	public void removeDoubleClickListener(Listener listener) {
		fDoubleClickListeners.remove(listener);
	}

	public void removeFocusListener(Listener listener) {
		fFocusListeners.remove(listener);
	}

	public void removeSelectionChangedListener(IBarSelectionListener listener) {
		fBarSelectionListeners.remove(listener);
	}

	/**
	 * Set the background color for the chart, the default is SWT.COLOR_WHITE
	 * 
	 * @param backgroundColor
	 *        The backgroundColor to set.
	 */
	public void setBackgroundColor(Color backgroundColor) {
		this.fBackgroundColor = backgroundColor;
	}

	/**
	 * set the option to auto zoom the chart
	 * 
	 * @param canZoomToSliderOnMouseUp
	 */
	public void setCanAutoZoomToSlider(boolean canZoomToSliderOnMouseUp) {

		fChartComponents.getChartComponentGraph().setCanAutoZoomToSlider(canZoomToSliderOnMouseUp);
	}

	/**
	 * set the option to scroll/not scroll the zoomed chart
	 * 
	 * @param canScrollabelZoomedGraph
	 */
	public void setCanScrollZoomedChart(boolean canScrollabelZoomedGraph) {

		fChartComponents.getChartComponentGraph().setCanScrollZoomedChart(canScrollabelZoomedGraph);
	}

	/**
	 * Set the enable state for a command and update the UI
	 */
	public void setCommandEnabled(String commandId, boolean isEnabled) {

		fChartActionProxies.get(commandId).setEnabled(isEnabled);

		if (fUseActionHandlers) {
			fActionHandlerManager.getActionHandler(commandId).fireHandlerChanged();
		}
	}

	public void setContextProvider(IChartContextProvider chartContextProvider) {
		fChartContextProvider = chartContextProvider;
	}

	public void setDataModel(ChartDataModel chartDataModel) {
		fChartDataModel = chartDataModel;
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
	public void setGraphAlpha(int alphaValue) {
		fChartComponents.getChartComponentGraph().fGraphAlpha = alphaValue;
	}

	/**
	 * Select (highlight) the bar in the bar chart
	 * 
	 * @param selectedItems
	 *        items in the x-data serie which should be selected, can be <code>null</code> to
	 *        deselect the bar
	 */
	public void setSelectedBars(boolean[] selectedItems) {
		fChartComponents.getChartComponentGraph().setSelectedBars(selectedItems);
	}

	/**
	 * @param isMarkerVisible
	 *        <code>true</code> shows the marker area
	 */
	public void setShowMarker(boolean isMarkerVisible) {
		fChartComponents.setMarkerVisible(isMarkerVisible);
	}

	public void setShowPartNavigation(boolean showPartNavigation) {
		fShowPartNavigation = showPartNavigation;
	}

	/**
	 * @param isSliderVisible
	 *        <code>true</code> shows the sliders
	 */
	public void setShowSlider(boolean isSliderVisible) {
		fChartComponents.setSliderVisible(isSliderVisible);
	}

	public void setShowZoomActions(boolean showZoomActions) {
		fShowZoomActions = showZoomActions;
	}

	/**
	 * @param toolbarMgr
	 * @param isFillToolbar
	 *        set <code>false</code> when the toolbar will be filled with
	 *        {@link Chart#fillToolbar(boolean)} from externally, when <code>true</code> the
	 *        toolbar will be filled when the chart is updated
	 */
	public void setToolBarManager(IToolBarManager toolbarMgr, boolean isFillToolbar) {
		fToolbarMgr = toolbarMgr;
		fIsFillToolbar = isFillToolbar;
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

	/**
	 * sets the position of the x-sliders
	 * 
	 * @param sliderPosition
	 */
	public void setXSliderPosition(SelectionChartXSliderPosition sliderPosition) {

		// check is the position is for this chart
		if (sliderPosition.chart == this) {
			fChartComponents.setXSliderPosition(sliderPosition);
		}
	}

	/**
	 * Enable/disable the zoom in/out action
	 * 
	 * @param isEnabled
	 */
	public void setZoomActionsEnabled(boolean isEnabled) {
		fChartActionProxies.get(COMMAND_ID_ZOOM_IN).setEnabled(isEnabled);
		fChartActionProxies.get(COMMAND_ID_ZOOM_OUT).setEnabled(isEnabled);
		fChartActionProxies.get(COMMAND_ID_FIT_GRAPH).setEnabled(isEnabled);
		fActionHandlerManager.updateUIState();
	}

	/**
	 * set the synch configuration which is used when the chart is drawn/resized
	 * 
	 * @param synchConfigIn
	 */
	public void setSynchConfigIn(SynchConfiguration synchConfigIn) {
		fChartComponents.setSynchConfigIn(synchConfigIn);
	}

	/**
	 * Set's the {@link SynchConfiguration} listener, this is a {@link Chart} which will be notified
	 * when this chart is resized, <code>null</code> will disable the synchronisation
	 * 
	 * @param chartWidget
	 */
	public void setSynchedChart(Chart chartWidget) {
		fSynchedChart = chartWidget;
	}

	public void switchSlidersTo2ndXData() {
		fChartComponents.getChartComponentGraph().switchSlidersTo2ndXData();
	}

	private void updateActionState() {

		fChartActionProxies.get(COMMAND_ID_PART_PREVIOUS).setEnabled(false);
		fChartActionProxies.get(COMMAND_ID_PART_NEXT).setEnabled(false);

		fChartActionProxies.get(COMMAND_ID_ZOOM_IN).setEnabled(true);
		fChartActionProxies.get(COMMAND_ID_ZOOM_OUT).setEnabled(false);

		fChartActionProxies.get(COMMAND_ID_FIT_GRAPH).setEnabled(true);

		// update UI state for the action handlers
		if (useInternalActionBar() == false) {
			fActionHandlerManager.updateUIState();
		}
	}

	/**
	 * Sets a new data model for the chart and redraws it, NULL will hide the chart
	 * 
	 * @param chartDataModel
	 */
	public void updateChart(ChartDataModel chartDataModel) {
		updateChart(chartDataModel, true);
	}

	/**
	 * Set a new data model for the chart and redraws it, NULL will hide the chart.
	 * <p>
	 * This method sets the data for the chart and creates it.
	 * 
	 * @param chartDataModel
	 * @param isResetSelection
	 *        <code>true</code> to reset the last selection in the chart
	 */
	public void updateChart(ChartDataModel chartDataModel, boolean isResetSelection) {

		if (chartDataModel == null
				|| (chartDataModel != null && chartDataModel.getYData().isEmpty())) {

			ChartDataModel emptyModel = new ChartDataModel(ChartDataModel.CHART_TYPE_LINE);

			fChartDataModel = emptyModel;
			fChartComponents.setModel(emptyModel);

			return;
		}

		fChartDataModel = chartDataModel;

		createActions();
		fChartComponents.setModel(chartDataModel);

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

	/**
	 * Zoom into the chart so that the sliders are positioned on the right and left border of the
	 * chart
	 */
	public void zoomIn() {
		fChartComponents.zoomIn();
		setCommandEnabled(COMMAND_ID_ZOOM_OUT, true);
	}

	/**
	 * Zoom into the chart and update the actions
	 */
	public void zoomInWithSlider() {

		fChartComponents.getChartComponentGraph().zoomInWithSlider();
		fChartComponents.onResize();

		setCommandEnabled(COMMAND_ID_ZOOM_OUT, true);
	}

	public void zoomOut(boolean updateChart) {

		if (fChartDataModel == null) {
			return;
		}

		fChartComponents.zoomOut(updateChart);

		setCommandEnabled(COMMAND_ID_ZOOM_IN, true);
		setCommandEnabled(COMMAND_ID_ZOOM_OUT, false);

		setCommandEnabled(COMMAND_ID_PART_PREVIOUS, false);
		setCommandEnabled(COMMAND_ID_PART_NEXT, false);
	}

	/**
	 * Zoom the graph, so that the left and right sliders are at the visible border
	 * 
	 * @param sliderPosition
	 */
//	public void zoomToXSlider(SelectionChartXSliderPosition sliderPosition) {
//		fChartComponents.zoomToXSlider(sliderPosition);
//	}
	/**
	 * zoom into the chart where the graph is divided into parts (months)
	 * 
	 * @param parts
	 *        number of parts into how many parts the chart is devided
	 * @param position
	 *        is based on 0
	 * @param scrollSmoothly
	 */
	public void zoomWithParts(int parts, int position, boolean scrollSmoothly) {

		fChartActionProxies.get(COMMAND_ID_ZOOM_IN).setEnabled(false);
		fChartActionProxies.get(COMMAND_ID_ZOOM_OUT).setEnabled(true);

		fChartActionProxies.get(COMMAND_ID_PART_PREVIOUS).setEnabled(true);
		fChartActionProxies.get(COMMAND_ID_PART_NEXT).setEnabled(true);

		if (fUseActionHandlers) {
			fActionHandlerManager.updateUIEnablementState();
		}

		fChartComponents.zoomWithParts(parts, position, scrollSmoothly);
	}

}
