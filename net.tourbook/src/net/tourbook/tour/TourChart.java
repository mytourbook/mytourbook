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

package net.tourbook.tour;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartMarker;
import net.tourbook.chart.ChartMarkerLayer;
import net.tourbook.chart.ChartYDataMinMaxKeeper;
import net.tourbook.chart.IChartLayer;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * The tour chart extends the chart with all the functionality for a tour chart
 */
public class TourChart extends Chart {

	static final String						COMMAND_ID_CHART_OPTIONS			= "net.tourbook.command.tourChart.options";			//$NON-NLS-1$
	static final String						COMMAND_ID_SHOW_START_TIME			= "net.tourbook.command.tourChart.showStartTime";		//$NON-NLS-1$
	static final String						COMMAND_ID_CAN_SCROLL_CHART			= "net.tourbook.command.tourChart.canScrollChart";		//$NON-NLS-1$
	static final String						COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER	= "net.tourbook.command.tourChart.canAutoZoomToSlider"; //$NON-NLS-1$

	static final String						COMMAND_ID_X_AXIS_DISTANCE			= "net.tourbook.command.tourChart.xAxisDistance";		//$NON-NLS-1$
	static final String						COMMAND_ID_X_AXIS_TIME				= "net.tourbook.command.tourChart.xAxisTime";			//$NON-NLS-1$

	static final String						COMMAND_ID_GRAPH_ALTITUDE			= "net.tourbook.command.graph.altitude";				//$NON-NLS-1$
	static final String						COMMAND_ID_GRAPH_SPEED				= "net.tourbook.command.graph.speed";					//$NON-NLS-1$
	static final String						COMMAND_ID_GRAPH_PULSE				= "net.tourbook.command.graph.pulse";					//$NON-NLS-1$
	static final String						COMMAND_ID_GRAPH_TEMPERATURE		= "net.tourbook.command.graph.temperature";			//$NON-NLS-1$
	static final String						COMMAND_ID_GRAPH_CADENCE			= "net.tourbook.command.graph.cadence";				//$NON-NLS-1$
	static final String						COMMAND_ID_GRAPH_ALTIMETER			= "net.tourbook.command.graph.altimeter";				//$NON-NLS-1$
	static final String						COMMAND_ID_GRAPH_GRADIENT			= "net.tourbook.command.graph.gradient";				//$NON-NLS-1$

	public static final String				COMMAND_ID_GRAPH_TOUR_COMPARE		= "net.tourbook.command.graph.tourCompare";			//$NON-NLS-1$

	static final String						SEGMENT_VALUES						= "segmentValues";										//$NON-NLS-1$

	TourData								fTourData;
	TourChartConfiguration					fTourChartConfig;

	Map<String, TCActionProxy>				fActionProxies;

	private boolean							fShowActions;
	private final TCActionHandlerManager	fTCActionHandlerManager				= TCActionHandlerManager.getInstance();

	private ActionChartOptions				fActionOptions;

	/**
	 * datamodel listener is called when the chart data is created
	 */
	private IDataModelListener				fChartDataModelListener;

	private final ListenerList				fSelectionListeners					= new ListenerList();
	private ITourModifyListener				fTourModifyListener;
	private IPropertyChangeListener			fPrefChangeListener;

	private ChartMarkerLayer				fMarkerLayer;
	private ChartSegmentLayer				fSegmentLayer;
	private ChartSegmentValueLayer			fSegmentValueLayer;

	private boolean							fIsTourDirty;
	private boolean							fIsSegmentLayerVisible;

//	private boolean							fBackupIsXSliderVisible;

	public TourChart(final Composite parent, final int style, boolean showActions) {

		super(parent, style);

		fShowActions = showActions;

		addPrefListeners();

		/*
		 * when the focus is changed, fire a tour chart selection, this is neccesarry to update the
		 * tour markers when a tour chart got the focus
		 */
		addFocusListener(new Listener() {
			public void handleEvent(Event event) {
				fireTourChartSelection();
			}
		});

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {

				TourbookPlugin.getDefault()
						.getPluginPreferences()
						.removePropertyChangeListener(fPrefChangeListener);
			}
		});
	}

	/**
	 * Activate all tour chart action handlers, this must be done when the part with a tour chart is
	 * activated
	 * 
	 * @param workbenchPartSite
	 */
	public void activateActionHandlers(IWorkbenchPartSite partSite) {

		if (useActionHandlers()) {

			// update tour action handlers
			fTCActionHandlerManager.updateTourActionHandlers(partSite, this);

			// update the action handlers in the chart
			updateChartActionHandlers();
		}
	}

//	@Override
//	public void activateActions(IWorkbenchPartSite partSite) {
//
////		IContextService contextService = (IContextService) partSite.getService(IContextService.class);
////		fContextBarChart = contextService.activateContext(Chart.CONTEXT_ID_BAR_CHART);
////		net.tourbook.chart.context.isTourChart
////		fChart.updateChartActionHandlers();
//	}
//
//	@Override
//	public void deactivateActions(IWorkbenchPartSite partSite) {
//
////		IContextService contextService = (IContextService) partSite.getService(IContextService.class);
////		contextService.deactivateContext(fContextBarChart);
//	}

	/**
	 * add a data model listener which is fired when the data model has changed
	 * 
	 * @param dataModelListener
	 */
	public void addDataModelListener(final IDataModelListener dataModelListener) {
		fChartDataModelListener = dataModelListener;
	}

	private void addPrefListeners() {
		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {
				final String property = event.getProperty();

				if (fTourChartConfig == null) {
					return;
				}

				boolean isChartModified = false;

				// test if the zoom preferences has changed
				if (property.equals(ITourbookPreferences.GRAPH_ZOOM_SCROLL_ZOOMED_GRAPH)
						|| property.equals(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER)) {

					final IPreferenceStore prefStore = TourbookPlugin.getDefault()
							.getPreferenceStore();

					TourManager.updateZoomOptionsInChartConfig(fTourChartConfig, prefStore);

					isChartModified = true;
				}

				if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {

					/*
					 * when the chart is computed, the changed colors are read from the preferences
					 */

					isChartModified = true;
				}

				isChartModified = setMinDefaultValue(property,
						isChartModified,
						ITourbookPreferences.GRAPH_ALTIMETER_MIN_ENABLED,
						ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE,
						TourManager.GRAPH_ALTIMETER,
						0);

				isChartModified = setMinDefaultValue(property,
						isChartModified,
						ITourbookPreferences.GRAPH_GRADIENT_MIN_ENABLED,
						ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE,
						TourManager.GRAPH_GRADIENT,
						TourManager.GRADIENT_DIVISOR);

				if (isChartModified) {
					updateTourChart(true);
				}
			}
		};
		TourbookPlugin.getDefault()
				.getPluginPreferences()
				.addPropertyChangeListener(fPrefChangeListener);

	}

	public void addTourChartListener(ITourChartSelectionListener listener) {
		fSelectionListeners.add(listener);
	}

	public void addTourModifyListener(final ITourModifyListener listener) {
		fTourModifyListener = listener;
	}

	/**
	 * Create action proxies for all chart graphs
	 */
	private void createGraphActionProxies() {

		createGraphActionProxy(TourManager.GRAPH_ALTITUDE,
				COMMAND_ID_GRAPH_ALTITUDE,
				Messages.Graph_Label_Altitude,
				Messages.Tour_Action_graph_altitude_tooltip,
				Messages.Image_graph_altitude,
				null);

		createGraphActionProxy(TourManager.GRAPH_SPEED,
				COMMAND_ID_GRAPH_SPEED,
				Messages.Graph_Label_Speed,
				Messages.Tour_Action_graph_speed_tooltip,
				Messages.Image_graph_speed,
				null);

		createGraphActionProxy(TourManager.GRAPH_ALTIMETER,
				COMMAND_ID_GRAPH_ALTIMETER,
				Messages.Graph_Label_Altimeter,
				Messages.Tour_Action_graph_altimeter_tooltip,
				Messages.Image_graph_altimeter,
				null);

		createGraphActionProxy(TourManager.GRAPH_PULSE,
				COMMAND_ID_GRAPH_PULSE,
				Messages.Graph_Label_Heartbeat,
				Messages.Tour_Action_graph_heartbeat_tooltip,
				Messages.Image_graph_heartbeat,
				null);

		createGraphActionProxy(TourManager.GRAPH_TEMPERATURE,
				COMMAND_ID_GRAPH_TEMPERATURE,
				Messages.Graph_Label_Temperature,
				Messages.Tour_Action_graph_temperature_tooltip,
				Messages.Image_graph_temperature,
				null);

		createGraphActionProxy(TourManager.GRAPH_CADENCE,
				COMMAND_ID_GRAPH_CADENCE,
				Messages.Graph_Label_Cadence,
				Messages.Tour_Action_graph_cadence_tooltip,
				Messages.Image_graph_cadence,
				null);

		createGraphActionProxy(TourManager.GRAPH_GRADIENT,
				COMMAND_ID_GRAPH_GRADIENT,
				Messages.Graph_Label_Gradiend,
				Messages.Tour_Action_graph_gradient_tooltip,
				Messages.Image_graph_gradient,
				null);

		createGraphActionProxy(TourManager.GRAPH_TOUR_COMPARE,
				COMMAND_ID_GRAPH_TOUR_COMPARE,
				Messages.Graph_Label_Tour_Compare,
				Messages.Tour_Action_graph_tour_compare_tooltip,
				Messages.Image_graph_tour_compare,
				Messages.Image_graph_tour_compare_disabled);
	}

	/**
	 * Create the action proxy for a graph action
	 * 
	 * @param label
	 * @param toolTip
	 * @param imageEnabled
	 * @param imageDisabled
	 * @param id
	 * @param definitionId
	 * @param isChecked
	 * @return
	 */
	private void createGraphActionProxy(final int graphId,
										String commandId,
										final String label,
										final String toolTip,
										final String imageEnabled,
										String imageDisabled) {

		Action action = null;

		if (useInternalActionBar()) {
			action = new ActionGraph(this, graphId, label, toolTip, imageEnabled, imageDisabled);
		}

		final TCActionProxy actionProxy = new TCActionProxy(commandId, action);

		actionProxy.setIsGraphAction();

		fActionProxies.put(getProxyId(graphId), actionProxy);
	}

	/**
	 * create the layer which displays the tour marker
	 */
	private void createMarkerLayer() {

		final int[] xAxisSerie = fTourChartConfig.showTimeOnXAxis
				? fTourData.timeSerie
				: fTourData.distanceSerie;

		fMarkerLayer = new ChartMarkerLayer();
		fMarkerLayer.setLineColor(new RGB(50, 100, 10));

		final Collection<TourMarker> tourMarkerList = fTourData.getTourMarkers();

		for (final TourMarker tourMarker : tourMarkerList) {

			final ChartMarker chartMarker = new ChartMarker();

			final int markerIndex = Math.min(tourMarker.getSerieIndex(), xAxisSerie.length - 1);
			chartMarker.graphX = xAxisSerie[markerIndex];

			chartMarker.markerLabel = tourMarker.getLabel();
			chartMarker.graphLabel = Integer.toString(fTourData.altitudeSerie[markerIndex]);

			chartMarker.serieIndex = markerIndex;
			chartMarker.visualPosition = tourMarker.getVisualPosition();
			chartMarker.type = tourMarker.getType();
			chartMarker.visualType = tourMarker.getVisibleType();

			chartMarker.labelXOffset = tourMarker.getLabelXOffset();
			chartMarker.labelYOffset = tourMarker.getLabelYOffset();

			fMarkerLayer.addMarker(chartMarker);
		}
	}

	/**
	 * Creates the layers from the segmented tour data
	 */
	private void createSegmentLayers() {

		if (fTourData == null) {
			return;
		}

		final int[] segmentSerie = fTourData.segmentSerieIndex;

		if (segmentSerie == null || fIsSegmentLayerVisible == false) {
			// no segmented tour data available or segments are invisible
			return;
		}

		final int[] xDataSerie = fTourChartConfig.showTimeOnXAxis
				? fTourData.timeSerie
				: fTourData.distanceSerie;

		fSegmentLayer = new ChartSegmentLayer();
		fSegmentLayer.setLineColor(new RGB(0, 177, 219));

		for (int segmentIndex = 0; segmentIndex < segmentSerie.length; segmentIndex++) {

			final int serieIndex = segmentSerie[segmentIndex];

			final ChartMarker chartMarker = new ChartMarker();

			chartMarker.graphX = xDataSerie[serieIndex];
			chartMarker.serieIndex = serieIndex;

			fSegmentLayer.addMarker(chartMarker);
		}

		fSegmentValueLayer = new ChartSegmentValueLayer();
		fSegmentValueLayer.setLineColor(new RGB(231, 104, 38));
		fSegmentValueLayer.setTourData(fTourData);
		fSegmentValueLayer.setXDataSerie(xDataSerie);

		// draw the graph lighter so the segments are more visible
		setGraphAlpha(0x60);
	}

	/**
	 * Creates the handlers for the tour chart actions
	 * 
	 * @param workbenchWindow
	 * @param tourChartConfig
	 */
	public void createTourActionHandlers(TourChartConfiguration tourChartConfig) {

		fTourChartConfig = tourChartConfig;

		fTCActionHandlerManager.createActionHandlers();
		createTourActionProxies();
		createChartActionHandlers();
	}

	/**
	 * Create the action proxies for all tour actions
	 */
	private void createTourActionProxies() {

		// check if action proxies are created
		if (fActionProxies != null) {
			return;
		}

		fActionProxies = new HashMap<String, TCActionProxy>();

		/*
		 * Action: chart graphs
		 */
		createGraphActionProxies();

		Action action;
		TCActionProxy actionProxy;
		final boolean useInternalActionBar = useInternalActionBar();

		/*
		 * Action: x-axis time
		 */
		action = useInternalActionBar ? new ActionXAxisTime(this) : null;
		actionProxy = new TCActionProxy(COMMAND_ID_X_AXIS_TIME, action);
		actionProxy.setChecked(fTourChartConfig.showTimeOnXAxis);
		fActionProxies.put(COMMAND_ID_X_AXIS_TIME, actionProxy);

		/*
		 * Action: x-axis distance
		 */
		action = useInternalActionBar ? new ActionXAxisDistance(this) : null;
		actionProxy = new TCActionProxy(COMMAND_ID_X_AXIS_DISTANCE, action);
		actionProxy.setChecked(!fTourChartConfig.showTimeOnXAxis);
		fActionProxies.put(COMMAND_ID_X_AXIS_DISTANCE, actionProxy);

		/*
		 * Action: chart options
		 */
		actionProxy = new TCActionProxy(COMMAND_ID_CHART_OPTIONS, null);
		fActionProxies.put(COMMAND_ID_CHART_OPTIONS, actionProxy);

		/*
		 * Action: show start time
		 */
		fActionProxies.put(COMMAND_ID_SHOW_START_TIME,
				new TCActionProxy(COMMAND_ID_SHOW_START_TIME, useInternalActionBar
						? new ActionShowStartTime(this)
						: null));

		/*
		 * Action: can scroll zoomed chart
		 */
		actionProxy = new TCActionProxy(COMMAND_ID_CAN_SCROLL_CHART, useInternalActionBar
				? new ActionCanScrollZoomedChart(this)
				: null);
		fActionProxies.put(COMMAND_ID_CAN_SCROLL_CHART, actionProxy);

		/*
		 * Action: auto zoom to slider
		 */
		actionProxy = new TCActionProxy(COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER, useInternalActionBar
				? new ActionCanAutoZoomToSlider(this)
				: null);
		fActionProxies.put(COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER, actionProxy);

	}

	public void enableGraphAction(int graphId, boolean isEnabled) {

		if (fActionProxies == null) {
			return;
		}

		final TCActionProxy actionProxy = fActionProxies.get(getProxyId(graphId));
		if (actionProxy != null) {
			actionProxy.setEnabled(isEnabled);
		}

	}

	/**
	 * create the tour specific action, they are defined in the chart configuration
	 */
	private void fillToolbar() {

		// check if toolbar is created
		if (fActionOptions != null) {
			return;
		}

		if (useInternalActionBar() == false) {
			return;
		}

		final IToolBarManager tbm = getToolBarManager();

		fActionOptions = new ActionChartOptions(this, (ToolBarManager) tbm);

		/*
		 * add the actions to the toolbar
		 */
		if (fTourChartConfig.canShowTourCompareGraph) {
			final TCActionProxy actionProxy = fActionProxies.get(getProxyId(TourManager.GRAPH_TOUR_COMPARE));
			tbm.add(actionProxy.getAction());
		}

		tbm.add(new Separator());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_ALTITUDE)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_SPEED)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_PULSE)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_TEMPERATURE)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_CADENCE)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_ALTIMETER)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_GRADIENT)).getAction());

		tbm.add(new Separator());
		tbm.add(fActionProxies.get(COMMAND_ID_X_AXIS_TIME).getAction());
		tbm.add(fActionProxies.get(COMMAND_ID_X_AXIS_DISTANCE).getAction());

		tbm.add(new Separator());
		tbm.add(fActionOptions);

		tbm.update(true);
	}

	/**
	 * fire a selection event for this tour chart
	 */
	public void fireTourChartSelection() {
		Object[] listeners = fSelectionListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final ITourChartSelectionListener listener = (ITourChartSelectionListener) listeners[i];
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.selectedTourChart(new SelectionTourChart(TourChart.this));
				}
			});
		}
	}

	/**
	 * Converts the graph Id into a proxy Id
	 * 
	 * @param graphId
	 * @return
	 */
	private String getProxyId(int graphId) {
		return "graphId." + Integer.toString(graphId); //$NON-NLS-1$
	}

	public TourData getTourData() {
		return fTourData;
	}

	public boolean isTourDirty() {
		return fIsTourDirty;
	}

	void onExecuteCanAutoZoomToSlider(Boolean isItemChecked) {

		setCanAutoZoomToSlider(isItemChecked);

		// apply setting to the chart
		if (isItemChecked) {
			zoomInWithSlider();
		} else {
			zoomOut(true);
		}

		updateZoomOptionActionHandlers();
	}

	void onExecuteCanScrollChart(Boolean isItemChecked) {

		setCanScrollZoomedChart(isItemChecked);

		// apply setting to the chart
		if (isItemChecked) {
			zoomInWithSlider();
		} else {
			zoomOut(true);
		}

		updateZoomOptionActionHandlers();
	}

	void onExecuteShowStartTime(Boolean isItemChecked) {

		fTourChartConfig.isStartTime = isItemChecked;
		updateTourChart(true);

		setCommandChecked(COMMAND_ID_SHOW_START_TIME, isItemChecked);
	}

	/**
	 * Set the X-axis to distance
	 * 
	 * @param isChecked
	 * @return Returns <code>true</code> when the x-axis was set to the distance
	 */
	boolean onExecuteXAxisDistance(boolean isChecked) {

		// check if the distance axis button was pressed
		if (isChecked && !fTourChartConfig.showTimeOnXAxis) {
			return false;
		}

		if (isChecked) {

			// show distance on x axes

			fTourChartConfig.showTimeOnXAxis = !fTourChartConfig.showTimeOnXAxis;
			fTourChartConfig.showTimeOnXAxisBackup = fTourChartConfig.showTimeOnXAxis;

			switchSlidersTo2ndXData();
			updateTourChart(true);
		}

		// toggle time and distance buttons
		setCommandChecked(TourChart.COMMAND_ID_X_AXIS_TIME, !isChecked);
		setCommandChecked(TourChart.COMMAND_ID_X_AXIS_DISTANCE, isChecked);

		return true;
	}

	/**
	 * @param isChecked
	 * @return Returns <code>true</code> when the check state was changed
	 */
	boolean onExecuteXAxisTime(boolean isChecked) {

		// check if the time axis button was pressed
		if (isChecked && fTourChartConfig.showTimeOnXAxis) {
			return false;
		}

		if (isChecked) {

			// show time on x axis

			fTourChartConfig.showTimeOnXAxis = !fTourChartConfig.showTimeOnXAxis;
			fTourChartConfig.showTimeOnXAxisBackup = fTourChartConfig.showTimeOnXAxis;

			switchSlidersTo2ndXData();
			updateTourChart(true);
		}

		// toggle time and distance buttons
		setCommandChecked(TourChart.COMMAND_ID_X_AXIS_TIME, isChecked);
		setCommandChecked(TourChart.COMMAND_ID_X_AXIS_DISTANCE, !isChecked);

		return true;
	}

	public void removeTourChartListener(ITourChartSelectionListener listener) {
		fSelectionListeners.remove(listener);
	}

	/**
	 * Set the check state for a command and update the UI
	 * 
	 * @param commandId
	 * @param isItemChecked
	 */
	public void setCommandChecked(String commandId, Boolean isItemChecked) {

		fActionProxies.get(commandId).setChecked(isItemChecked);
		fTCActionHandlerManager.updateUICheckState(commandId);
	}

	/**
	 * Set the enable/check state for a command and update the UI
	 */
	public void setCommandState(String commandId, boolean isEnabled, boolean isChecked) {

		final TCActionProxy actionProxy = fActionProxies.get(commandId);

		actionProxy.setEnabled(isEnabled);
		fTCActionHandlerManager.getActionHandler(commandId).fireHandlerChanged();

		actionProxy.setChecked(isChecked);
		fTCActionHandlerManager.updateUICheckState(commandId);
	}

	/**
	 * set the graph layers
	 */
	private void setGraphLayers() {

		final ArrayList<IChartLayer> customLayers = new ArrayList<IChartLayer>();
		final ArrayList<IChartLayer> segmentValueLayers = new ArrayList<IChartLayer>();

		if (fSegmentLayer != null) {
			customLayers.add(fSegmentLayer);
		}

		if (fSegmentValueLayer != null) {
			segmentValueLayers.add(fSegmentValueLayer);
		}

		if (fMarkerLayer != null) {
			customLayers.add(fMarkerLayer);
		}

		ChartDataYSerie yData;

		final ChartDataModel dataModel = getDataModel();

		if (dataModel == null) {
			return;
		}

		yData = (ChartDataYSerie) dataModel.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE);
		if (yData != null) {
			yData.setCustomLayers(customLayers);
		}

		setSegmentLayer(segmentValueLayers,
				fTourData.segmentSerieSpeed,
				TourManager.CUSTOM_DATA_SPEED);

		setSegmentLayer(segmentValueLayers,
				fTourData.segmentSerieGradient,
				TourManager.CUSTOM_DATA_GRADIENT);

		setSegmentLayer(segmentValueLayers,
				fTourData.segmentSerieAltimeter,
				TourManager.CUSTOM_DATA_ALTIMETER);

		setSegmentLayer(segmentValueLayers,
				fTourData.segmentSeriePulse,
				TourManager.CUSTOM_DATA_PULSE);
	}

	private boolean setMinDefaultValue(	final String property,
										boolean isChartModified,
										final String tagIsMinEnabled,
										final String tabMinValue,
										final int yDataInfoId,
										int valueDivisor) {

		if (property.equals(tagIsMinEnabled) || property.equals(tabMinValue)) {

			final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

			final boolean isAltMinEnabled = prefStore.getBoolean(tagIsMinEnabled);

			final ArrayList<ChartDataYSerie> yDataList = getDataModel().getYData();

			// get altimeter data from all y-data
			ChartDataYSerie yData = null;
			for (final ChartDataYSerie yDataIterator : yDataList) {
				final Integer yDataInfo = (Integer) yDataIterator.getCustomData(ChartDataYSerie.YDATA_INFO);
				if (yDataInfo == yDataInfoId) {
					yData = yDataIterator;
				}
			}

			if (yData != null) {

				if (isAltMinEnabled) {
					// set visible min value from the preferences
					yData.setVisibleMinValue(prefStore.getInt(tabMinValue) * valueDivisor);

				} else {
					// reset visible min value to the original min value
					yData.setVisibleMinValue(yData.getOriginalMinValue());
				}

				isChartModified = true;
			}
		}
		return isChartModified;
	}

	private void setSegmentLayer(	final ArrayList<IChartLayer> segmentValueLayers,
									float[] segmentSerie,
									String customDataKey) {

		ChartDataYSerie yData = (ChartDataYSerie) getDataModel().getCustomData(customDataKey);

		if (yData != null) {
			yData.setCustomLayers(segmentValueLayers);
			yData.setCustomData(SEGMENT_VALUES, segmentSerie);
		}
	}

	/**
	 * set the tour dirty that the tour is saved when the tour is closed
	 */
	public void setTourDirty(boolean isDirty) {

		fIsTourDirty = isDirty;

		if (fTourModifyListener != null) {
			fTourModifyListener.tourIsModified();
		}
	}

	/**
	 * set's the chart which is synched with this chart
	 * 
	 * @param isSynchEnabled
	 *        <code>true</code> to enable synch, <code>false</code> to disable synch
	 * @param synchedChart
	 *        contains the {@link Chart} which is synched with this chart
	 * @param synchMode
	 */
	public void synchChart(final boolean isSynchEnabled, final TourChart synchedChart, int synchMode) {

		// enable/disable synched chart
		super.setSynchedChart(isSynchEnabled ? synchedChart : null);

		final Map<String, TCActionProxy> actionProxies = synchedChart.fActionProxies;

		if (actionProxies == null) {
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
			synchedChart.setCanScrollZoomedChart(false);
			synchedChart.setCanAutoZoomToSlider(true);

			// hide the x-sliders
//			fBackupIsXSliderVisible = synchedChart.isXSliderVisible();
			synchedChart.setShowSlider(false);

			synchronizeChart();

		} else {

			// disable chart synchronization

			// enable zoom action
			actionProxies.get(COMMAND_ID_CAN_SCROLL_CHART)
					.setChecked(synchedChart.getCanScrollZoomedChart());
			actionProxies.get(COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER)
					.setChecked(synchedChart.getCanAutoZoomToSlider());

			synchedChart.setZoomActionsEnabled(true);
			synchedChart.updateZoomOptions(true);

			// restore the x-sliders
//			synchedChart.setShowSlider(fBackupIsXSliderVisible);
			synchedChart.setShowSlider(true);

			synchedChart.setSynchConfig(null);

			// show whole chart 
			synchedChart.getChartDataModel().resetMinMaxValues();
			synchedChart.zoomOut(true);
		}
	}

	/**
	 * Enable/disable the graph action buttons, the visible state of a graph is defined in the chart
	 * config
	 */
	void updateActionState() {

		final ArrayList<Integer> visibleGraphs = fTourChartConfig.getVisibleGraphs();

		// enable/uncheck all GRAPH action
		for (final TCActionProxy actionProxy : fActionProxies.values()) {
			if (actionProxy.isGraphAction()) {
				actionProxy.setChecked(false);
				actionProxy.setEnabled(true);
			}
		}

		// check visible graph buttons
		for (final int graphId : visibleGraphs) {
			String proxyId = getProxyId(graphId);
			fActionProxies.get(proxyId).setChecked(true);
			fActionProxies.get(proxyId).setEnabled(true);
		}

		// update start time option
		fActionProxies.get(COMMAND_ID_SHOW_START_TIME).setEnabled(fTourChartConfig.showTimeOnXAxis);
		fActionProxies.get(COMMAND_ID_SHOW_START_TIME).setChecked(fTourChartConfig.isStartTime);

		fActionProxies.get(COMMAND_ID_X_AXIS_TIME).setChecked(fTourChartConfig.showTimeOnXAxis);
		fActionProxies.get(COMMAND_ID_X_AXIS_DISTANCE)
				.setChecked(!fTourChartConfig.showTimeOnXAxis);

		updateZoomOptions();

		// update UI state for the action handlers
		if (useActionHandlers()) {
			fTCActionHandlerManager.updateUIState();
		}
	}

	/**
	 * Updates the marker layer in the chart
	 * 
	 * @param showLayer
	 */
	public void updateMarkerLayer(final boolean showLayer) {

		if (showLayer) {
			createMarkerLayer();
		} else {
			fMarkerLayer = null;
		}

		setGraphLayers();
		updateChartLayers();
	}

	/**
	 * 
	 */
	public void updateSegmentLayer(final boolean showLayer) {

		fIsSegmentLayerVisible = showLayer;

		if (fIsSegmentLayerVisible) {
			createSegmentLayers();
		} else {
			fSegmentLayer = null;
			fSegmentValueLayer = null;
			resetGraphAlpha();
		}

		setGraphLayers();
		updateChartLayers();

		/*
		 * the chart needs to be redrawn because the alpha for filling the chart has changed
		 */
		redrawChart();
	}

	/**
	 * Update the tour chart with the previous data and configuration
	 * 
	 * @param keepMinMaxValues
	 *        <code>true</code> keeps the min/max values from the previous chart
	 */
	public void updateTourChart(final boolean keepMinMaxValues) {
		updateTourChartInternal(fTourData, fTourChartConfig, keepMinMaxValues, false);
	}

	/**
	 * Update the tour chart with the previous data and configuration
	 * 
	 * @param keepMinMaxValues
	 *        <code>true</code> keeps the min/max values from the previous chart
	 * @param isPropertyChanged
	 *        when <code>true</code> the properties for the tour chart have changed
	 */
	public void updateTourChart(boolean keepMinMaxValues, boolean isPropertyChanged) {
		updateTourChartInternal(fTourData, fTourChartConfig, keepMinMaxValues, isPropertyChanged);
	}

	/**
	 * Set {@link TourData} and {@link TourChartConfiguration} to create a new chart data model
	 * 
	 * @param tourData
	 * @param chartConfig
	 * @param keepMinMaxValues
	 *        <code>true</code> keeps the min/max values from the previous chart
	 */
	public void updateTourChart(final TourData tourData,
								final TourChartConfiguration chartConfig,
								final boolean keepMinMaxValues) {

		updateTourChartInternal(tourData, chartConfig, keepMinMaxValues, false);
	}

	private void updateTourChartInternal(	final TourData newTourData,
											final TourChartConfiguration newChartConfig,
											final boolean keepMinMaxValues,
											boolean isPropertyChanged) {

		if (newTourData == null || newChartConfig == null) {
			return;
		}

		// keep min/max values for the 'old' chart in the chart config
		if (fTourChartConfig != null
				&& fTourChartConfig.getMinMaxKeeper() != null
				&& keepMinMaxValues) {
			fTourChartConfig.getMinMaxKeeper().saveMinMaxValues(getDataModel());
		}

		// set current tour data and chart config
		fTourData = newTourData;
		fTourChartConfig = newChartConfig;

		final ChartDataModel newDataModel = TourManager.getInstance()
				.createChartDataModel(newTourData, newChartConfig, isPropertyChanged);

		// set the model before the actions are created
		setDataModel(newDataModel);

		if (fShowActions) {
			createTourActionProxies();
			fillToolbar();
			updateActionState();
		}

		// restore min/max values from the chart config
		final ChartYDataMinMaxKeeper chartConfigMinMaxKeeper = newChartConfig.getMinMaxKeeper();
		if (chartConfigMinMaxKeeper != null && keepMinMaxValues) {
			chartConfigMinMaxKeeper.setMinMaxValues(newDataModel);
		}

		if (fChartDataModelListener != null) {
			fChartDataModelListener.dataModelChanged(newDataModel);
		}

		createSegmentLayers();
		createMarkerLayer();
		setGraphLayers();

		updateChart(newDataModel);
	}

	/**
	 * Update UI check state, the chart decides if the scroll/auto zoom options are available
	 */
	void updateZoomOptionActionHandlers() {

		setCommandChecked(TourChart.COMMAND_ID_CAN_SCROLL_CHART, getCanScrollZoomedChart());
		setCommandChecked(TourChart.COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER, getCanAutoZoomToSlider());
	}

	/**
	 * enable/disable and check/uncheck the zoom options from the chart configuration
	 */
	private void updateZoomOptions() {

		// get options check status from the configuration
		final boolean canScrollZoomedChart = fTourChartConfig.scrollZoomedGraph;
		final boolean canAutoZoomToSlider = fTourChartConfig.autoZoomToSlider;

		// update the chart
		setCanScrollZoomedChart(canScrollZoomedChart);
		setCanAutoZoomToSlider(canAutoZoomToSlider);

		fActionProxies.get(COMMAND_ID_CAN_SCROLL_CHART).setEnabled(true);
		fActionProxies.get(COMMAND_ID_CAN_SCROLL_CHART).setChecked(canScrollZoomedChart);

		fActionProxies.get(COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER).setEnabled(true);
		fActionProxies.get(COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER).setChecked(canAutoZoomToSlider);
	}

	/**
	 * Enable/disable the zoom options in the tour chart
	 * 
	 * @param isEnabled
	 */
	private void updateZoomOptions(final boolean isEnabled) {

		fActionProxies.get(COMMAND_ID_CAN_SCROLL_CHART).setEnabled(isEnabled);
		fActionProxies.get(COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER).setEnabled(isEnabled);
	}

}
