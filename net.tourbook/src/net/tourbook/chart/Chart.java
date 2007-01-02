/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
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

	static final int					NO_BAR_SELECTION			= -1;

	private static final int			ACTION_MOVETO_PREVIOUS_PART	= 1000;
	private static final int			ACTION_MOVETO_NEXT_PART		= 1001;
	protected static final int			ACTION_ZOOM_IN				= 1002;
	protected static final int			ACTION_ZOOM_OUT				= 1003;
	private static final int			ACTION_ZOOM_FIT_GRAPH		= 1004;

	private ListenerList				fFocusListeners				= new ListenerList();
	private ListenerList				fBarSelectionListeners		= new ListenerList();
	private ListenerList				fDoubleClickListeners		= new ListenerList();
	private ListenerList				fSliderMoveListeners		= new ListenerList();

	protected ChartComponents			fChartComponents;
	protected ChartDataModel			fChartDataModel;
	private IToolBarManager				fToolbarMgr;
	private ChartContextProvider		fChartContextProvider;
	protected HashMap<Integer, Action>	actions;

	protected boolean					fShowZoomActions;
	private boolean						showPartNavigation;
	private boolean						showZoomFitGraph;

	private Color						backgroundColor;

	private Chart						zoomMarkerPositionListener;

	/**
	 * listener which is called when the x-marker was dragged
	 */
	IChartListener						fXMarkerDraggingListener;

	/**
	 * This is the ui control which displays the chart
	 */

	public Chart(Composite parent, int style) {

		super(parent, style);

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
		backgroundColor = getDisplay().getSystemColor(SWT.COLOR_WHITE);
	}

	public void addDoubleClickListener(IBarSelectionListener listener) {
		fDoubleClickListeners.add(listener);
	}

	public void addSelectionChangedListener(IBarSelectionListener listener) {
		fBarSelectionListeners.add(listener);
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
	 * @param actionId
	 * @param text
	 * @param toolTip
	 * @param image
	 * @return
	 */
	private IAction createAction(int actionId, String text, String toolTip, String[] image) {

		ChartAction action = new ChartAction(this, actionId, text, toolTip, image);
		action.setEnabled(false);
		actions.put(actionId, action);

		return action;
	}

	/**
	 * create zoom/navigation actions which are managed from the chart
	 */
	private void createActions() {

		if (actions != null) {
			return;
		}

		actions = new HashMap<Integer, Action>();

		IAction action = createAction(
				ACTION_MOVETO_PREVIOUS_PART,
				"Previous Month",
				"Scroll to the previous month",
				new String[] { "arrow-left.gif", "arrow-left-disabled.gif" });

		action = createAction(
				ACTION_MOVETO_NEXT_PART,
				"Next Month",
				"Scroll to the next month",
				new String[] { "arrow-right.gif", "arrow-right-disabled.gif" });

		action = createAction(ACTION_ZOOM_IN, "Zoom in", "Zoom into the graphic", new String[] {
				"zoom-in.gif",
				"zoom-in-disabled.gif" });
		action.setEnabled(true);

		action = createAction(ACTION_ZOOM_OUT, "Zoom out", "Zoom out of the graphic", new String[] {
				"zoom-out.gif",
				"zoom-out-disabled.gif" });

		action = createAction(
				ACTION_ZOOM_FIT_GRAPH,
				"Fit to Graph",
				"Fit graph to window",
				new String[] { "zoom-fit-graph.gif", null });
		action.setEnabled(true);

		showActions(true);
	}

	/**
	 * @return
	 */
	private SelectionChartInfo createChartInfo() {

		if (fChartComponents == null) {
			return null;
		}

		SelectionChartInfo chartInfo = new SelectionChartInfo();

		chartInfo.chartDataModel = fChartDataModel;
		chartInfo.chartDrawingData = fChartComponents.getChartDrawingData();

		ChartComponentGraph chartGraph = fChartComponents.getChartComponentGraph();
		chartInfo.leftSlider = chartGraph.getLeftSlider();
		chartInfo.rightSlider = chartGraph.getRightSlider();

		return chartInfo;
	}

	public void dispose() {
		fChartComponents.dispose();
		super.dispose();
	}

	void fillMenu(IMenuManager menuMgr, ChartXSlider leftSlider, ChartXSlider rightSlider) {

		if (fChartDataModel.getChartType() == ChartDataModel.CHART_TYPE_BAR) {

			// get the method which computes the bar info
			ChartContextProvider barChartContextProvider = (ChartContextProvider) fChartDataModel
					.getCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER);

			// create the menu for bar charts
			if (barChartContextProvider != null) {
				barChartContextProvider.fillBarChartContextMenu(menuMgr);
			}

		} else {
			// create the menu for line charts
			if (fChartContextProvider != null) {
				fChartContextProvider.fillXSliderContextMenu(menuMgr, leftSlider, rightSlider);
			}
		}
	}

	void fireChartDoubleClick(final int serieIndex, final int valueIndex) {
		Object[] listeners = fDoubleClickListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final IBarSelectionListener listener = (IBarSelectionListener) listeners[i];
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.selectionChanged(serieIndex, valueIndex);
				}
			});
		}
	}

	void fireBarSelectionEvent(final int serieIndex, final int valueIndex) {

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
	 * 
	 * @param newMarkerPositionOut
	 */
	protected void fireZoomMarkerPositionListener() {

		if (zoomMarkerPositionListener == null) {
			return;
		}

		getDisplay().asyncExec(new Runnable() {
			public void run() {
				zoomMarkerPositionListener
						.setZoomMarkerPositionIn(fChartComponents.zoomMarkerPositionOut);
			}
		});
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public boolean getCanAutoZoomToSlider() {
		return fChartComponents.getChartComponentGraph().canAutoZoomToSlider;
	}

	public boolean getCanScrollZoomedChart() {
		return fChartComponents.getChartComponentGraph().canScrollZoomedChart;
	}

	public ChartDataModel getChartDataModel() {
		return fChartDataModel;
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

	public ISelection getSelection() {
		return null;
	}

	/**
	 * Returns the toolbar for the chart, if no toolbar manager is set with
	 * setToolbarManager, the manager will be created and the toolbar is on top
	 * of the chart
	 * 
	 * @return
	 */
	public IToolBarManager getToolbarManager() {

		if (fToolbarMgr == null) {

			// create the toolbar and put it on top of the chart
			final ToolBar toolBarControl = new ToolBar(this, SWT.FLAT | SWT.WRAP);
			setTopLeft(toolBarControl);

			// toolBarControl.addListener(SWT.Resize, new Listener() {
			// public void handleEvent(Event e) {
			//
			// // wrap the tool bar on resize
			// Rectangle rect = getClientArea();
			// Point size = toolBarControl.computeSize(rect.width, SWT.DEFAULT);
			// toolBarControl.setSize(size);
			//					
			// }
			// });

			// create toolbar manager
			fToolbarMgr = new ToolBarManager(toolBarControl);
		}

		return fToolbarMgr;
	}

	/**
	 * returns the value index for the x-sliders
	 */
	public SelectionChartXSliderPosition getXSliderPosition() {

		ChartComponentGraph chartGraph = fChartComponents.getChartComponentGraph();

		return new SelectionChartXSliderPosition(
				this,
				chartGraph.getLeftSlider().getValuesIndex(),
				chartGraph.getRightSlider().getValuesIndex());
	}

	/**
	 * @return Returns <code>true</code> when the x-sliders are visible
	 */
	public boolean isXSliderVisible() {
		return fChartComponents.devSliderBarHeight != 0;
	}

	void performZoomAction(int actionId) {

		switch (actionId) {
		case ACTION_MOVETO_PREVIOUS_PART:
			fChartComponents.getChartComponentGraph().moveToPrevPart();
			break;

		case ACTION_MOVETO_NEXT_PART:
			fChartComponents.getChartComponentGraph().moveToNextPart();
			break;

		case ACTION_ZOOM_IN:
			actions.get(ACTION_ZOOM_OUT).setEnabled(true);
			fChartComponents.zoomIn();
			break;

		case ACTION_ZOOM_OUT:
			actions.get(ACTION_ZOOM_IN).setEnabled(true);
			actions.get(ACTION_ZOOM_OUT).setEnabled(false);
			actions.get(ACTION_MOVETO_PREVIOUS_PART).setEnabled(false);
			actions.get(ACTION_MOVETO_NEXT_PART).setEnabled(false);
			fChartComponents.zoomOut(true);
			break;

		case ACTION_ZOOM_FIT_GRAPH:
			fChartDataModel.resetMinMaxValues();
			zoomOut(true);
			break;

		default:
			break;
		}
	}

	/**
	 * make the graph dirty and redraw it
	 */
	public void redrawChart() {
		fChartComponents.getChartComponentGraph().redrawChart();
	}

	public void removeDoubleClickListener(IBarSelectionListener listener) {
		fDoubleClickListeners.remove(listener);
	}

	public void removeSelectionChangedListener(IBarSelectionListener listener) {
		fBarSelectionListeners.remove(listener);
	}

	public void removeFocusListener(Listener listener) {
		fFocusListeners.remove(listener);
	}

	/**
	 * Set the background color for the chart, the default is SWT.COLOR_WHITE
	 * 
	 * @param backgroundColor
	 *        The backgroundColor to set.
	 */
	public void setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
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
	 * Sets a new data model for the chart and redraws it, NULL will hide the
	 * chart
	 * 
	 * @param chartDataModel
	 */
	public void setChartDataModel(ChartDataModel chartDataModel) {
		setChartDataModel(chartDataModel, true);
	}

	/**
	 * Sets a new data model for the chart and redraws it, NULL will hide the
	 * chart
	 * 
	 * @param chartDataModel
	 * @param isResetSelection
	 *        <code>true</code> to reset the last selection in the chart
	 */
	public void setChartDataModel(ChartDataModel chartDataModel, boolean isResetSelection) {

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

	public void setContextProvider(ChartContextProvider chartContextProvider) {
		fChartContextProvider = chartContextProvider;
	}

	/**
	 * Enable/disable the zoom in/out action
	 * 
	 * @param isEnabled
	 */
	public void setEnabledZoomActions(boolean isEnabled) {
		actions.get(ACTION_ZOOM_IN).setEnabled(isEnabled);
		actions.get(ACTION_ZOOM_OUT).setEnabled(isEnabled);
	}

	// public boolean setFocus() {
	// return fChartComponents.getChartComponentGraph().setChartFocus();
	// }

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
	 *        items in the x-data serie which should be selected, can be also
	 *        <code>null</code> to deselect the bar
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
		this.showPartNavigation = showPartNavigation;
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

	public void setToolBarManager(IToolBarManager toolbarMgr) {
		fToolbarMgr = toolbarMgr;
	}

	/**
	 * sets the position of the x-sliders
	 * 
	 * @param position
	 */
	public void setXSliderPosition(SelectionChartXSliderPosition position) {
		fChartComponents.setXSliderPosition(position);
	}

	/**
	 * set the zoom-marker position, this position is used when the chart is
	 * drawn/resized
	 * 
	 * @param zoomMarkerPositionIn
	 */
	public void setZoomMarkerPositionIn(ZoomMarkerPosition zoomMarkerPositionIn) {
		fChartComponents.setZoomMarkerPositionIn(zoomMarkerPositionIn);
	}

	/**
	 * Set's the zoom-marker position listener, this is a chartwidget which will
	 * be notified to synchronize the marker position when this chart is
	 * resized, when set to <code>null</code> this will disable the
	 * synchronisation
	 * 
	 * @param chartWidget
	 */
	public void setZoomMarkerPositionListener(Chart chartWidget) {
		zoomMarkerPositionListener = chartWidget;
	}

	/**
	 * put the actions into the toolbar
	 * 
	 * @param refreshToolbar
	 */
	public void showActions(boolean refreshToolbar) {

		if (actions != null && (showPartNavigation || fShowZoomActions)) {

			// add the action to the toolbar
			IToolBarManager tbm = getToolbarManager();

			if (showPartNavigation) {
				tbm.add(new Separator());
				tbm.add(actions.get(ACTION_MOVETO_PREVIOUS_PART));
				tbm.add(actions.get(ACTION_MOVETO_NEXT_PART));
			}

			if (fShowZoomActions) {
				tbm.add(new Separator());
				tbm.add(actions.get(ACTION_ZOOM_IN));
				tbm.add(actions.get(ACTION_ZOOM_OUT));
			}

			if (showZoomFitGraph) {
				tbm.add(actions.get(ACTION_ZOOM_FIT_GRAPH));
			}

			if (refreshToolbar) {
				tbm.update(true);
			}
		}
	}

	public void switchSlidersTo2ndXData() {
		fChartComponents.getChartComponentGraph().switchSlidersTo2ndXData();
	}

	public void updateChartLayers() {
		fChartComponents.updateChartLayers();
	}

	/**
	 * Zoom into the chart so that the sliders are positioned on the right and
	 * left border of the chart
	 */
	public void zoomIn() {
		fChartComponents.zoomIn();
		actions.get(ACTION_ZOOM_OUT).setEnabled(true);
	}

	/**
	 * Zoom into the chart and update the actions
	 */
	public void zoomInWithSlider() {

		fChartComponents.getChartComponentGraph().zoomInWithSlider();

		fChartComponents.onResize();

		actions.get(ACTION_ZOOM_OUT).setEnabled(true);
	}

	public void zoomOut(boolean updateChart) {

		if (fChartDataModel == null) {
			return;
		}

		fChartComponents.zoomOut(updateChart);
		actions.get(ACTION_ZOOM_OUT).setEnabled(false);
	}

	/**
	 * Zoom the graph, so that the left and right sliders are at the visible
	 * border
	 * 
	 * @param sliderPosition
	 */
	public void zoomToXSlider(SelectionChartXSliderPosition sliderPosition) {
		fChartComponents.zoomToXSlider(sliderPosition);
	}

	/**
	 * zoom into the chart where the graph is divided into parts (months)
	 * 
	 * @param parts
	 *        number of parts into how many parts the chart is devided
	 * @param position
	 *        is based on 0
	 */
	public void zoomWithParts(int parts, int position) {

		actions.get(ACTION_ZOOM_IN).setEnabled(false);
		actions.get(ACTION_ZOOM_OUT).setEnabled(true);
		actions.get(ACTION_MOVETO_PREVIOUS_PART).setEnabled(true);
		actions.get(ACTION_MOVETO_NEXT_PART).setEnabled(true);

		fChartComponents.zoomWithParts(parts, position);
	}

	public int getDevGraphImageXOffset() {
		return fChartComponents.getChartComponentGraph().getDevGraphImageXOffset();
	}

	public boolean getAdvancedGraphics() {
		return fChartComponents.useAdvancedGraphics;
	}

}
