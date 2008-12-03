/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartLabel;
import net.tourbook.chart.ChartLabelLayer;
import net.tourbook.chart.ChartMarker;
import net.tourbook.chart.ChartYDataMinMaxKeeper;
import net.tourbook.chart.IChartLayer;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourChartSelectionListener;
import net.tourbook.tour.ITourModifyListener;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.action.ActionCanAutoZoomToSlider;
import net.tourbook.ui.tourChart.action.ActionCanMoveSlidersWhenZoomed;
import net.tourbook.ui.tourChart.action.ActionChartOptions;
import net.tourbook.ui.tourChart.action.ActionGraph;
import net.tourbook.ui.tourChart.action.ActionShowStartTime;
import net.tourbook.ui.tourChart.action.ActionXAxisDistance;
import net.tourbook.ui.tourChart.action.ActionXAxisTime;
import net.tourbook.ui.tourChart.action.TCActionHandler;
import net.tourbook.ui.tourChart.action.TCActionHandlerManager;
import net.tourbook.ui.tourChart.action.TCActionProxy;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
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

	public static final String				COMMAND_ID_CHART_OPTIONS				= "net.tourbook.command.tourChart.options";					//$NON-NLS-1$
	public static final String				COMMAND_ID_SHOW_START_TIME				= "net.tourbook.command.tourChart.showStartTime";				//$NON-NLS-1$
	public static final String				COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER		= "net.tourbook.command.tourChart.canAutoZoomToSlider";		//$NON-NLS-1$
	public static final String				COMMAND_ID_CAN_MOVE_SLIDERS_WHN_ZOOMED	= "net.tourbook.command.tourChart.canMoveSlidersWhenZoomed";	//$NON-NLS-1$

	public static final String				COMMAND_ID_X_AXIS_DISTANCE				= "net.tourbook.command.tourChart.xAxisDistance";				//$NON-NLS-1$
	public static final String				COMMAND_ID_X_AXIS_TIME					= "net.tourbook.command.tourChart.xAxisTime";					//$NON-NLS-1$

	public static final String				COMMAND_ID_GRAPH_ALTITUDE				= "net.tourbook.command.graph.altitude";						//$NON-NLS-1$
	public static final String				COMMAND_ID_GRAPH_SPEED					= "net.tourbook.command.graph.speed";							//$NON-NLS-1$
	public static final String				COMMAND_ID_GRAPH_PACE					= "net.tourbook.command.graph.pace";							//$NON-NLS-1$
	public static final String				COMMAND_ID_GRAPH_POWER					= "net.tourbook.command.graph.power";							//$NON-NLS-1$
	public static final String				COMMAND_ID_GRAPH_PULSE					= "net.tourbook.command.graph.pulse";							//$NON-NLS-1$
	public static final String				COMMAND_ID_GRAPH_TEMPERATURE			= "net.tourbook.command.graph.temperature";					//$NON-NLS-1$
	public static final String				COMMAND_ID_GRAPH_CADENCE				= "net.tourbook.command.graph.cadence";						//$NON-NLS-1$
	public static final String				COMMAND_ID_GRAPH_ALTIMETER				= "net.tourbook.command.graph.altimeter";						//$NON-NLS-1$
	public static final String				COMMAND_ID_GRAPH_GRADIENT				= "net.tourbook.command.graph.gradient";						//$NON-NLS-1$

	public static final String				COMMAND_ID_GRAPH_TOUR_COMPARE			= "net.tourbook.command.graph.tourCompare";					//$NON-NLS-1$

	static final String						SEGMENT_VALUES							= "segmentValues";												//$NON-NLS-1$

	private TourData						fTourData;
	private TourChartConfiguration			fTourChartConfig;

	private Map<String, TCActionProxy>		fActionProxies;

	private boolean							fShowActions;

	private final TCActionHandlerManager	fTCActionHandlerManager					= TCActionHandlerManager.getInstance();
	private ActionChartOptions				fActionOptions;

	/**
	 * datamodel listener is called when the chart data is created
	 */
	private IDataModelListener				fChartDataModelListener;

	private final ListenerList				fSelectionListeners						= new ListenerList();

	private ITourModifyListener				fTourModifyListener;
	private IPropertyChangeListener			fPrefChangeListener;

	private ChartLabelLayer					fLabelLayer;
	private ChartSegmentLayer				fSegmentLayer;
	private ChartSegmentValueLayer			fSegmentValueLayer;

	private boolean							fIsSegmentLayerVisible;

	private boolean							fIsTourDirty;

	public TourChart(final Composite parent, final int style, final boolean showActions) {

		super(parent, style);

		fShowActions = showActions;

		addPrefListeners();

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		gridVerticalDistance = prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE);
		gridHorizontalDistance = prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE);

		setShowMouseMode();

		/*
		 * when the focus is changed, fire a tour chart selection, this is neccesarry to update the
		 * tour markers when a tour chart got the focus
		 */
		addFocusListener(new Listener() {
			public void handleEvent(final Event event) {
				fireTourChartSelection();
			}
		});

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {

				TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);
			}
		});
	}

	/**
	 * Activate all tour chart action handlers, this must be done when the part with a tour chart is
	 * activated
	 * 
	 * @param workbenchPartSite
	 */
	public void activateActionHandlers(final IWorkbenchPartSite partSite) {

		if (useActionHandlers()) {

			// update tour action handlers
			fTCActionHandlerManager.updateTourActionHandlers(partSite, this);

			// update the action handlers in the chart
			updateChartActionHandlers();
		}
	}

	/**
	 * add a data model listener which is fired when the data model has changed
	 * 
	 * @param dataModelListener
	 */
	public void addDataModelListener(final IDataModelListener dataModelListener) {
		fChartDataModelListener = dataModelListener;
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

	private void addPrefListeners() {
		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {
				final String property = event.getProperty();

				if (fTourChartConfig == null) {
					return;
				}

				final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
				boolean isChartModified = false;
				boolean keepMinMax = true;

				if (property.equals(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED)
						|| property.equals(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER)) {

					// zoom preferences has changed

					TourManager.updateZoomOptionsInChartConfig(fTourChartConfig, prefStore);

					isChartModified = true;

				} else if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {

					/*
					 * when the chart is computed, the modified colors are read from the preferences
					 */

					isChartModified = true;

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					isChartModified = true;
					keepMinMax = false;

				} else if (property.equals(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE)) {

					gridVerticalDistance = prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE);
					gridHorizontalDistance = prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE);
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
					updateTourChart(keepMinMax);
				}
			}
		};

		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);

	}

	public void addTourChartListener(final ITourChartSelectionListener listener) {
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
				Messages.Image__graph_altitude,
				Messages.Image__graph_altitude_disabled);

		createGraphActionProxy(TourManager.GRAPH_SPEED,
				COMMAND_ID_GRAPH_SPEED,
				Messages.Graph_Label_Speed,
				Messages.Tour_Action_graph_speed_tooltip,
				Messages.Image__graph_speed,
				Messages.Image__graph_speed_disabled);

		createGraphActionProxy(TourManager.GRAPH_PACE,
				COMMAND_ID_GRAPH_PACE,
				Messages.Graph_Label_Pace,
				Messages.Tour_Action_graph_pace_tooltip,
				Messages.Image__graph_pace,
				Messages.Image__graph_pace_disabled);

		createGraphActionProxy(TourManager.GRAPH_POWER,
				COMMAND_ID_GRAPH_POWER,
				Messages.Graph_Label_Power,
				Messages.Tour_Action_graph_power_tooltip,
				Messages.Image__graph_power,
				Messages.Image__graph_power_disabled);

		createGraphActionProxy(TourManager.GRAPH_ALTIMETER,
				COMMAND_ID_GRAPH_ALTIMETER,
				Messages.Graph_Label_Altimeter,
				Messages.Tour_Action_graph_altimeter_tooltip,
				Messages.Image__graph_altimeter,
				Messages.Image__graph_altimeter_disabled);

		createGraphActionProxy(TourManager.GRAPH_PULSE,
				COMMAND_ID_GRAPH_PULSE,
				Messages.Graph_Label_Heartbeat,
				Messages.Tour_Action_graph_heartbeat_tooltip,
				Messages.Image__graph_heartbeat,
				Messages.Image__graph_heartbeat_disabled);

		createGraphActionProxy(TourManager.GRAPH_TEMPERATURE,
				COMMAND_ID_GRAPH_TEMPERATURE,
				Messages.Graph_Label_Temperature,
				Messages.Tour_Action_graph_temperature_tooltip,
				Messages.Image__graph_temperature,
				Messages.Image__graph_temperature_disabled);

		createGraphActionProxy(TourManager.GRAPH_CADENCE,
				COMMAND_ID_GRAPH_CADENCE,
				Messages.Graph_Label_Cadence,
				Messages.Tour_Action_graph_cadence_tooltip,
				Messages.Image__graph_cadence,
				Messages.Image__graph_cadence_disabled);

		createGraphActionProxy(TourManager.GRAPH_GRADIENT,
				COMMAND_ID_GRAPH_GRADIENT,
				Messages.Graph_Label_Gradient,
				Messages.Tour_Action_graph_gradient_tooltip,
				Messages.Image__graph_gradient,
				Messages.Image__graph_gradient_disabled);

		createGraphActionProxy(TourManager.GRAPH_TOUR_COMPARE,
				COMMAND_ID_GRAPH_TOUR_COMPARE,
				Messages.Graph_Label_Tour_Compare,
				Messages.Tour_Action_graph_tour_compare_tooltip,
				Messages.Image__graph_tour_compare,
				Messages.Image__graph_tour_compare_disabled);
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
										final String commandId,
										final String label,
										final String toolTip,
										final String imageEnabled,
										final String imageDisabled) {

		Action action = null;

		if (useInternalActionBar()) {
			action = new ActionGraph(this, graphId, label, toolTip, imageEnabled, imageDisabled);
		}

		final TCActionProxy actionProxy = new TCActionProxy(commandId, action);

		fActionProxies.put(getProxyId(graphId), actionProxy);
	}

	/**
	 * create the layer which displays the tour marker
	 */
	private void createLabelLayer() {

		// set data serie for the x-axis
		final int[] xAxisSerie = fTourChartConfig.showTimeOnXAxis ? fTourData.timeSerie : fTourData.getDistanceSerie();

		fLabelLayer = new ChartLabelLayer();
		fLabelLayer.setLineColor(new RGB(50, 100, 10));

		final Collection<TourMarker> tourMarkerList = fTourData.getTourMarkers();

		for (final TourMarker tourMarker : tourMarkerList) {

			final ChartLabel chartLabel = new ChartLabel();

			final int markerIndex = Math.min(tourMarker.getSerieIndex(), xAxisSerie.length - 1);

			chartLabel.graphX = xAxisSerie[markerIndex];
			chartLabel.serieIndex = markerIndex;

			chartLabel.markerLabel = tourMarker.getLabel();
			chartLabel.visualPosition = tourMarker.getVisualPosition();
			chartLabel.type = tourMarker.getType();
			chartLabel.visualType = tourMarker.getVisibleType();

			chartLabel.labelXOffset = tourMarker.getLabelXOffset();
			chartLabel.labelYOffset = tourMarker.getLabelYOffset();

			fLabelLayer.addLabel(chartLabel);
		}
	}

	/**
	 * Creates the layers from the segmented tour data
	 */
	private void createSegmentLayer() {

		if (fTourData == null) {
			return;
		}

		final int[] segmentSerie = fTourData.segmentSerieIndex;

		if (segmentSerie == null || fIsSegmentLayerVisible == false) {
			// no segmented tour data available or segments are invisible
			return;
		}

		final int[] xDataSerie = fTourChartConfig.showTimeOnXAxis ? fTourData.timeSerie : fTourData.getDistanceSerie();

		/*
		 * create segment layer
		 */
		fSegmentLayer = new ChartSegmentLayer();
		fSegmentLayer.setLineColor(new RGB(0, 177, 219));

		for (final int serieIndex : segmentSerie) {

			final ChartMarker chartMarker = new ChartMarker();

			chartMarker.graphX = xDataSerie[serieIndex];
			chartMarker.serieIndex = serieIndex;

			fSegmentLayer.addMarker(chartMarker);
		}

		/*
		 * create segment value layer
		 */
		fSegmentValueLayer = new ChartSegmentValueLayer();
		fSegmentValueLayer.setLineColor(new RGB(231, 104, 38));
		fSegmentValueLayer.setTourData(fTourData);
		fSegmentValueLayer.setXDataSerie(xDataSerie);

		// draw the graph lighter so the segments are more visible
		setGraphAlpha(0x60);
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
		fActionProxies.put(COMMAND_ID_SHOW_START_TIME, new TCActionProxy(COMMAND_ID_SHOW_START_TIME,
				useInternalActionBar ? new ActionShowStartTime(this) : null));

		/*
		 * Action: auto zoom to slider
		 */
		actionProxy = new TCActionProxy(COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER, //
				useInternalActionBar ? new ActionCanAutoZoomToSlider(this) : null);
		fActionProxies.put(COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER, actionProxy);

		/*
		 * Action: move sliders when zoomed
		 */
		actionProxy = new TCActionProxy(COMMAND_ID_CAN_MOVE_SLIDERS_WHN_ZOOMED, //
				useInternalActionBar ? new ActionCanMoveSlidersWhenZoomed(this) : null);
		fActionProxies.put(COMMAND_ID_CAN_MOVE_SLIDERS_WHN_ZOOMED, actionProxy);

	}

	/**
	 * Creates the handlers for the tour chart actions
	 * 
	 * @param workbenchWindow
	 * @param tourChartConfig
	 */
	public void createTourEditorActionHandlers(final TourChartConfiguration tourChartConfig) {

		fTourChartConfig = tourChartConfig;

		fTCActionHandlerManager.createActionHandlers();
		createTourActionProxies();
		createChartActionHandlers();
	}

	public void enableGraphAction(final int graphId, final boolean isEnabled) {

		if (fActionProxies == null) {
			return;
		}

		final TCActionProxy actionProxy = fActionProxies.get(getProxyId(graphId));
		if (actionProxy != null) {
			actionProxy.setEnabled(isEnabled);
		}

	}

	private void enableZoomOptions() {

		if (fActionProxies == null) {
			return;
		}

		final boolean canAutoZoom = getMouseMode().equals(Chart.MOUSE_MODE_ZOOM);

		final TCActionProxy actionProxy = fActionProxies.get(COMMAND_ID_CAN_MOVE_SLIDERS_WHN_ZOOMED);
		if (actionProxy != null) {
			actionProxy.setEnabled(canAutoZoom);
		}
	}

	/**
	 * create the tour specific action bar, they are defined in the chart configuration
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

		fActionOptions = new ActionChartOptions(this);

		/*
		 * add the actions to the toolbar
		 */
		if (fTourChartConfig.canShowTourCompareGraph) {
			final TCActionProxy actionProxy = fActionProxies.get(getProxyId(TourManager.GRAPH_TOUR_COMPARE));
			tbm.add(actionProxy.getAction());
		}

		tbm.add(new Separator());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_ALTITUDE)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_PULSE)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_SPEED)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_PACE)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_POWER)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_TEMPERATURE)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_GRADIENT)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_ALTIMETER)).getAction());
		tbm.add(fActionProxies.get(getProxyId(TourManager.GRAPH_CADENCE)).getAction());

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
		final Object[] listeners = fSelectionListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final ITourChartSelectionListener listener = (ITourChartSelectionListener) listeners[i];
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.selectedTourChart(new SelectionTourChart(TourChart.this));
				}
			});
		}
	}

	public Map<String, TCActionProxy> getActionProxies() {
		return fActionProxies;
	}

	/**
	 * Converts the graph Id into a proxy Id
	 * 
	 * @param graphId
	 * @return
	 */
	private String getProxyId(final int graphId) {
		return "graphId." + Integer.toString(graphId); //$NON-NLS-1$
	}

	public TourChartConfiguration getTourChartConfig() {
		return fTourChartConfig;
	}

	public TourData getTourData() {
		return fTourData;
	}

	/**
	 * Enable/disable the graph action buttons, the visible state of a graph is defined in the chart
	 * config
	 */
	public void initializeTourActions() {

		final int[] allGraphIds = TourManager.allGraphIDs;
		final ArrayList<Integer> checkedGraphIds = fTourChartConfig.getVisibleGraphs();
		final ArrayList<Integer> enabledGraphIds = new ArrayList<Integer>();

		// get all graph ids which can be displayed
		for (final ChartDataSerie xyDataIterator : getChartDataModel().getXyData()) {

			if (xyDataIterator instanceof ChartDataYSerie) {
				final ChartDataYSerie yData = (ChartDataYSerie) xyDataIterator;
				final Integer graphId = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_INFO);
				enabledGraphIds.add(graphId);
			}
		}

		for (final int graphId : allGraphIds) {

			final TCActionProxy actionProxy = fActionProxies.get(getProxyId(graphId));

			actionProxy.setChecked(checkedGraphIds.contains(graphId));
			actionProxy.setEnabled(enabledGraphIds.contains(graphId));
		}

		// update start time option
		fActionProxies.get(COMMAND_ID_SHOW_START_TIME).setEnabled(fTourChartConfig.showTimeOnXAxis);
		fActionProxies.get(COMMAND_ID_SHOW_START_TIME).setChecked(fTourChartConfig.isStartTime);

		fActionProxies.get(COMMAND_ID_X_AXIS_TIME).setChecked(fTourChartConfig.showTimeOnXAxis);
		fActionProxies.get(COMMAND_ID_X_AXIS_DISTANCE).setChecked(!fTourChartConfig.showTimeOnXAxis);
		fActionProxies.get(COMMAND_ID_X_AXIS_DISTANCE).setEnabled(!fTourChartConfig.isForceTimeOnXAxis);

		initializeZoomOptions();

		// update UI state for the action handlers
		if (useActionHandlers()) {
			fTCActionHandlerManager.updateUIState();
		}
	}

	/**
	 * enable/disable and check/uncheck the zoom options from the chart configuration
	 */
	private void initializeZoomOptions() {

		// get options check status from the configuration
		final boolean isMoveSlidersWhenZoomed = fTourChartConfig.moveSlidersWhenZoomed;
		final boolean isAutoZoomToSlider = fTourChartConfig.autoZoomToSlider;
		final boolean canAutoZoom = getMouseMode().equals(Chart.MOUSE_MODE_ZOOM);

		// update the chart
		setCanAutoMoveSliders(isMoveSlidersWhenZoomed);
		setCanAutoZoomToSlider(isAutoZoomToSlider);

		// update actions
		fActionProxies.get(COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER).setEnabled(true);
		fActionProxies.get(COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER).setChecked(isAutoZoomToSlider);

		fActionProxies.get(COMMAND_ID_CAN_MOVE_SLIDERS_WHN_ZOOMED).setEnabled(canAutoZoom);
		fActionProxies.get(COMMAND_ID_CAN_MOVE_SLIDERS_WHN_ZOOMED).setChecked(isMoveSlidersWhenZoomed);
	}

	public boolean isTourDirty() {
		return fIsTourDirty;
	}

	public void onExecuteCanAutoMoveSliders(final boolean isItemChecked) {

		setCanAutoMoveSliders(isItemChecked);

		// apply setting to the chart
		if (isItemChecked) {
			onExecuteMoveSlidersToBorder();
//			onExecuteZoomInWithSlider();
		}

		updateZoomOptionActionHandlers();
	}

	public void onExecuteCanAutoZoomToSlider(final Boolean isItemChecked) {

		setCanAutoZoomToSlider(isItemChecked);

		// apply setting to the chart
//		if (isItemChecked) {
//			zoomInWithSlider();
//		} else {
//			zoomOut(true);
//		}

		updateZoomOptionActionHandlers();
	}

	public void onExecuteCanScrollChart(final Boolean isItemChecked) {

		setCanScrollZoomedChart(isItemChecked);

		// apply setting to the chart
		if (isItemChecked) {
			onExecuteZoomInWithSlider();
		} else {
			onExecuteZoomOut(true);
		}

		updateZoomOptionActionHandlers();
	}

	public void onExecuteShowStartTime(final Boolean isItemChecked) {

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
	public boolean onExecuteXAxisDistance(final boolean isChecked) {

		// check if the distance axis button was pressed
		if (isChecked && !fTourChartConfig.showTimeOnXAxis) {
			return false;
		}

		if (isChecked) {

			// show distance on x axis

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
	public boolean onExecuteXAxisTime(final boolean isChecked) {

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

	public void removeTourChartListener(final ITourChartSelectionListener listener) {
		fSelectionListeners.remove(listener);
	}

	/**
	 * Set the check state for a command and update the UI
	 * 
	 * @param commandId
	 * @param isItemChecked
	 */
	public void setCommandChecked(final String commandId, final Boolean isItemChecked) {

		fActionProxies.get(commandId).setChecked(isItemChecked);
		fTCActionHandlerManager.updateUICheckState(commandId);
	}

	/**
	 * Set the enable state for a command and update the UI
	 */
	public void setCommandEnabled(final String commandId, final boolean isEnabled) {

		final TCActionProxy actionProxy = fActionProxies.get(commandId);

		if (actionProxy != null) {
			actionProxy.setEnabled(isEnabled);
			final TCActionHandler actionHandler = fTCActionHandlerManager.getActionHandler(commandId);
			if (actionHandler != null) {
				actionHandler.fireHandlerChanged();
			}
		}
	}

	/**
	 * Set the enable/check state for a command and update the UI
	 */
	public void setCommandState(final String commandId, final boolean isEnabled, final boolean isChecked) {

		final TCActionProxy actionProxy = fActionProxies.get(commandId);

		actionProxy.setEnabled(isEnabled);
		final TCActionHandler actionHandler = fTCActionHandlerManager.getActionHandler(commandId);
		if (actionHandler != null) {
			actionHandler.fireHandlerChanged();
		}

		actionProxy.setChecked(isChecked);
		fTCActionHandlerManager.updateUICheckState(commandId);
	}

	/**
	 * set the graph layers
	 */
	private void setGraphLayers() {

		final ChartDataModel dataModel = getChartDataModel();
		if (dataModel == null) {
			return;
		}

		ChartDataYSerie yDataWithLabels;

		// get y-data which is displayed, this graph will display the tour markers
		yDataWithLabels = (ChartDataYSerie) dataModel.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE);

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

		setYDataLayers(TourManager.CUSTOM_DATA_ALTITUDE, fTourData.segmentSerieAltitude, yDataWithLabels);
		setYDataLayers(TourManager.CUSTOM_DATA_PULSE, fTourData.segmentSeriePulse, yDataWithLabels);
		setYDataLayers(TourManager.CUSTOM_DATA_SPEED, fTourData.segmentSerieSpeed, yDataWithLabels);
		setYDataLayers(TourManager.CUSTOM_DATA_PACE, fTourData.segmentSeriePace, yDataWithLabels);
		setYDataLayers(TourManager.CUSTOM_DATA_POWER, fTourData.segmentSeriePower, yDataWithLabels);
		setYDataLayers(TourManager.CUSTOM_DATA_GRADIENT, fTourData.segmentSerieGradient, yDataWithLabels);
		setYDataLayers(TourManager.CUSTOM_DATA_ALTIMETER, fTourData.segmentSerieAltimeter, yDataWithLabels);
		setYDataLayers(TourManager.CUSTOM_DATA_TEMPERATURE, null, yDataWithLabels);
	}

	private boolean setMinDefaultValue(	final String property,
										boolean isChartModified,
										final String tagIsMinEnabled,
										final String tabMinValue,
										final int yDataInfoId,
										final int valueDivisor) {

		if (property.equals(tagIsMinEnabled) || property.equals(tabMinValue)) {

			final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

			final boolean isAltMinEnabled = prefStore.getBoolean(tagIsMinEnabled);

			final ArrayList<ChartDataYSerie> yDataList = getChartDataModel().getYData();

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
	 * set tour dirty to save the tour when the tour is closed
	 */
	public void setTourDirty(final boolean isDirty) {

		fIsTourDirty = isDirty;

		if (fTourModifyListener != null) {
			fTourModifyListener.tourIsModified();
		}
	}

	private void setYDataLayers(final String customDataKey,
								final Object segmentDataSerie,
								final ChartDataYSerie yDataWithLabels) {

		final ChartDataModel dataModel = getChartDataModel();
		final ChartDataYSerie yData = (ChartDataYSerie) dataModel.getCustomData(customDataKey);

		if (yData != null) {

			final ArrayList<IChartLayer> chartLayers = new ArrayList<IChartLayer>();

			// show label layer only at ONE visible graph
			if (fLabelLayer != null && yData == yDataWithLabels) {
				chartLayers.add(fLabelLayer);
			}

			final ChartDataYSerie yDataAltitude = (ChartDataYSerie) dataModel.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE);

			if (yData == yDataAltitude) {
				if (fSegmentLayer != null) {
					chartLayers.add(fSegmentLayer);
				}
			} else {
				if (fSegmentValueLayer != null) {
					chartLayers.add(fSegmentValueLayer);
				}
			}

			yData.setCustomLayers(chartLayers);

			if (segmentDataSerie != null) {
				yData.setCustomData(SEGMENT_VALUES, segmentDataSerie);
			}
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
//			actionProxies.get(COMMAND_ID_CAN_SCROLL_CHART).setChecked(synchedChart.getCanScrollZoomedChart());
			actionProxies.get(COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER).setChecked(synchedChart.getCanAutoZoomToSlider());

			synchedChart.setZoomActionsEnabled(true);
			synchedChart.updateZoomOptions(true);

			// restore the x-sliders
			synchedChart.setShowSlider(true);

			synchedChart.setSynchConfig(null);

			// show whole chart 
			synchedChart.getChartDataModel().resetMinMaxValues();
			synchedChart.onExecuteZoomOut(true);
		}
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();

		sb.append(this.getClass().getSimpleName());
		sb.append(UI.NEW_LINE);

		sb.append(fTourData);
		sb.append(UI.NEW_LINE);

		return sb.toString();
	}

	/**
	 * Updates the marker layer in the chart
	 * 
	 * @param showLayer
	 */
	public void updateMarkerLayer(final boolean showLayer) {

		if (showLayer) {
			createLabelLayer();
		} else {
			fLabelLayer = null;
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
			createSegmentLayer();
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
	 *            <code>true</code> keeps the min/max values from the previous chart
	 */
	public void updateTourChart(final boolean keepMinMaxValues) {
		updateTourChartInternal(fTourData, fTourChartConfig, keepMinMaxValues, false);
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
		updateTourChartInternal(fTourData, fTourChartConfig, keepMinMaxValues, isPropertyChanged);
	}

	public void updateTourChart(final TourData tourData, final boolean keepMinMaxValues) {
		updateTourChartInternal(tourData, fTourChartConfig, keepMinMaxValues, false);

	}

	/**
	 * Set {@link TourData} and {@link TourChartConfiguration} to create a new chart data model
	 * 
	 * @param tourData
	 * @param chartConfig
	 * @param keepMinMaxValues
	 *            <code>true</code> keeps the min/max values from the previous chart
	 */
	public void updateTourChart(final TourData tourData,
								final TourChartConfiguration chartConfig,
								final boolean keepMinMaxValues) {

		updateTourChartInternal(tourData, chartConfig, keepMinMaxValues, false);
	}

	private void updateTourChartInternal(	final TourData newTourData,
											final TourChartConfiguration newChartConfig,
											final boolean keepMinMaxValues,
											final boolean isPropertyChanged) {

		if (newTourData == null || newChartConfig == null) {
			return;
		}

		// keep min/max values for the 'old' chart in the chart config
		if (fTourChartConfig != null && fTourChartConfig.getMinMaxKeeper() != null && keepMinMaxValues) {
			fTourChartConfig.getMinMaxKeeper().saveMinMaxValues(getChartDataModel());
		}

		// set current tour data and chart config
		fTourData = newTourData;
		fTourChartConfig = newChartConfig;

		final ChartDataModel newChartDataModel = TourManager.getInstance().createChartDataModel(newTourData,
				newChartConfig,
				isPropertyChanged);

		// set the model BEFORE the actions are created/enabled/checked
		setDataModel(newChartDataModel);

		if (fShowActions) {
			createTourActionProxies();
			fillToolbar();
			initializeTourActions();
		}

		// restore min/max values from the chart config
		final ChartYDataMinMaxKeeper chartConfigMinMaxKeeper = newChartConfig.getMinMaxKeeper();
		if (chartConfigMinMaxKeeper != null && keepMinMaxValues) {
			chartConfigMinMaxKeeper.setMinMaxValues(newChartDataModel);
		}

		if (fChartDataModelListener != null) {
			fChartDataModelListener.dataModelChanged(newChartDataModel);
		}

		createSegmentLayer();
		createLabelLayer();

		setGraphLayers();

		updateChart(newChartDataModel);

		/*
		 * this must be done after the chart is created because is sets an action
		 */
		setMouseMode(TourbookPlugin.getDefault()
				.getPreferenceStore()
				.getString(ITourbookPreferences.GRAPH_MOUSE_MODE)
				.equals(Chart.MOUSE_MODE_SLIDER));
	}

	/**
	 * Update UI check state, the chart decides if the scroll/auto zoom options are available
	 */
	void updateZoomOptionActionHandlers() {

		setCommandChecked(TourChart.COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER, getCanAutoZoomToSlider());
		setCommandChecked(TourChart.COMMAND_ID_CAN_MOVE_SLIDERS_WHN_ZOOMED, getCanAutoMoveSliders());
	}

	/**
	 * Enable/disable the zoom options in the tour chart
	 * 
	 * @param isEnabled
	 */
	private void updateZoomOptions(final boolean isEnabled) {
		fActionProxies.get(COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER).setEnabled(isEnabled);
		fActionProxies.get(COMMAND_ID_CAN_MOVE_SLIDERS_WHN_ZOOMED).setEnabled(isEnabled);
	}

}
