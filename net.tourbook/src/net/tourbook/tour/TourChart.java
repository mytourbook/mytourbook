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
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartMarker;
import net.tourbook.chart.ChartMarkerLayer;
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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public class TourChart extends Chart {

	static final String				SEGMENT_VALUES		= "segmentValues";		//$NON-NLS-1$

	TourData						fTourData;
	TourChartConfiguration			fTourChartConfig;

	protected Map<Integer, Action>	fGraphActions;
	protected ActionChartOptions	fActionOptions;

	private Action					fActionXAxesTime;
	private Action					fActionXAxesDistance;
	private Action					fActionZoomFitGraph;
	private Action					fActionAdjustAltitude;

	private ActionGraphAnalyzer		fActionGraphAnalyzer;
	private ActionTourSegmenter		fActionTourSegmenter;
	private ActionTourMarker		fActionMarkerEditor;

	private ListenerList			fSelectionListeners	= new ListenerList();

	/**
	 * datamodel listener is called when the chart data is created
	 */
	private IDataModelListener		fChartDataModelListener;

	private ITourModifyListener		fTourModifyListener;

	private ChartMarkerLayer		fMarkerLayer;
	private ChartSegmentLayer		fSegmentLayer;
	private ChartSegmentValueLayer	fSegmentValueLayer;

	private boolean					fIsXSliderVisible;

	private IPropertyChangeListener	fPrefChangeListener;
	private boolean					fIsTourDirty;
	private boolean					fIsSegmentLayerVisible;

	private boolean					fIsToolActions;

	public TourChart(final Composite parent, final int style, boolean showToolActions) {

		super(parent, style);

		fIsToolActions = showToolActions;

		setPrefListeners();

		/*
		 * when the focus is changed, fire a tour chart selection, this is neccesarry to update the
		 * tour markers when a tour chart got the focus
		 */
		addFocusListener(new Listener() {
			public void handleEvent(Event event) {
				fireTourChartSelection();
			}
		});

	}

	/**
	 * add a data model listener which is fired when the data model has changed
	 * 
	 * @param dataModelListener
	 */
	public void addDataModelListener(final IDataModelListener dataModelListener) {
		fChartDataModelListener = dataModelListener;
	}

	public void addTourChartListener(ITourChartSelectionListener listener) {
		fSelectionListeners.add(listener);
	}

	public void addTourModifyListener(final ITourModifyListener listener) {
		fTourModifyListener = listener;
	}

	/**
	 * @param id
	 * @param label
	 * @param toolTip
	 * @param imageName
	 * @param isChecked
	 * @return
	 */
	private ActionChangeGraphLayout createGraphAction(	final int id,
														final String label,
														final String toolTip,
														final String imageName) {

		final ActionChangeGraphLayout action = new ActionChangeGraphLayout(
				this,
				id,
				label,
				toolTip,
				imageName);

		fGraphActions.put(id, action);

		return action;
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

			chartMarker.graphX = xAxisSerie[tourMarker.getSerieIndex()];

			chartMarker.markerLabel = tourMarker.getLabel();
			chartMarker.graphLabel = Integer.toString(fTourData.altitudeSerie[tourMarker
					.getSerieIndex()]);

			chartMarker.serieIndex = tourMarker.getSerieIndex();
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
	private void createSegmentsLayer() {

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
		setGraphAlpha(0x40);
	}

	/**
	 * create the tour specific action, they are defined in the chart configuration
	 */
	private void createTourActions() {

		// create all actions only once
		if (fGraphActions != null) {
			return;
		}

		final IToolBarManager tbm = getToolbarManager();

		fGraphActions = new HashMap<Integer, Action>();

		fActionXAxesTime = new ActionXAxesTime(this);
		fActionXAxesDistance = new ActionXAxesDistance(this);
		fActionZoomFitGraph = new ActionZoomFitGraph(this);
		fActionAdjustAltitude = new ActionAdjustAltitude(this);

		createGraphAction(TourManager.GRAPH_ALTITUDE,
				Messages.Graph_Label_Altitude,
				Messages.Tour_Action_graph_altitude_tooltip,
				Messages.Image_graph_altitude);

		createGraphAction(TourManager.GRAPH_SPEED,
				Messages.Graph_Label_Speed,
				Messages.Tour_Action_graph_speed_tooltip,
				Messages.Image_graph_speed);

		createGraphAction(TourManager.GRAPH_ALTIMETER,
				Messages.Graph_Label_Altimeter,
				Messages.Tour_Action_graph_altimeter_tooltip,
				Messages.Image_graph_altimeter);

		createGraphAction(TourManager.GRAPH_PULSE,
				Messages.Graph_Label_Heartbeat,
				Messages.Tour_Action_graph_heartbeat_tooltip,
				Messages.Image_graph_heartbeat);

		createGraphAction(TourManager.GRAPH_TEMPERATURE,
				Messages.Graph_Label_Temperature,
				Messages.Tour_Action_graph_temperature_tooltip,
				Messages.Image_graph_temperature);

		createGraphAction(TourManager.GRAPH_CADENCE,
				Messages.Graph_Label_Cadence,
				Messages.Tour_Action_graph_cadence_tooltip,
				Messages.Image_graph_cadence);

		createGraphAction(TourManager.GRAPH_GRADIENT,
				Messages.Graph_Label_Gradiend,
				Messages.Tour_Action_graph_gradient_tooltip,
				Messages.Image_graph_gradient);

		fActionOptions = new ActionChartOptions(this, (ToolBarManager) tbm);

		/*
		 * add the actions to the toolbar
		 */

		tbm.add(new Separator());

		tbm.add(fGraphActions.get(TourManager.GRAPH_SPEED));
		tbm.add(fGraphActions.get(TourManager.GRAPH_ALTITUDE));
		tbm.add(fGraphActions.get(TourManager.GRAPH_PULSE));
		tbm.add(fGraphActions.get(TourManager.GRAPH_TEMPERATURE));
		tbm.add(fGraphActions.get(TourManager.GRAPH_CADENCE));
		tbm.add(fGraphActions.get(TourManager.GRAPH_ALTIMETER));
		tbm.add(fGraphActions.get(TourManager.GRAPH_GRADIENT));
		tbm.add(new Separator());

		tbm.add(fActionXAxesTime);
		tbm.add(fActionXAxesDistance);
		tbm.add(new Separator());

		tbm.add(fActionAdjustAltitude);
		tbm.add(fActionOptions);
		tbm.add(new Separator());

		tbm.add(fActionZoomFitGraph);
		tbm.add(new Separator());

		// ///////////////////////////////////////////////////////

		fActionGraphAnalyzer = new ActionGraphAnalyzer(this);

		tbm.add(fActionGraphAnalyzer);

		if (fIsToolActions) {

			fActionTourSegmenter = new ActionTourSegmenter(this);
			fActionMarkerEditor = new ActionTourMarker(this);

			tbm.add(fActionTourSegmenter);
			tbm.add(fActionMarkerEditor);
		}
	}

	public void dispose() {

		TourbookPlugin
				.getDefault()
				.getPluginPreferences()
				.removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	/**
	 * Enable/disable the graph buttons, this depends on the visible graphs which are defined in the
	 * chart config
	 */
	void enableActions() {

		final ArrayList<Integer> visibleGraphList = fTourChartConfig.getVisibleGraphs();

		// enable/uncheck all graph action
		for (final Action graphAction : fGraphActions.values()) {
			graphAction.setChecked(false);
			graphAction.setEnabled(true);
		}

		// check all actions which correspond to visible graphs
		if (visibleGraphList.size() == 1) {
			// disable the graph button when only one graph is displayed
			for (final int actionId : visibleGraphList) {
				fGraphActions.get(actionId).setChecked(true);
				fGraphActions.get(actionId).setEnabled(false);
			}
		} else {
			// enable all graph buttons
			for (final int actionId : visibleGraphList) {
				fGraphActions.get(actionId).setChecked(true);
				fGraphActions.get(actionId).setEnabled(true);
			}
		}

		fActionOptions.actionStartTimeOption.setEnabled(fTourChartConfig.showTimeOnXAxis);
		fActionOptions.actionStartTimeOption.setChecked(fTourChartConfig.isStartTime);

		fActionXAxesTime.setChecked(fTourChartConfig.showTimeOnXAxis);
		fActionXAxesDistance.setChecked(!fTourChartConfig.showTimeOnXAxis);

		enableZoomOptions();
	}

	/**
	 * enable/disable and check/uncheck the zoom options
	 */
	private void enableZoomOptions() {

		// get options check status from the configuration
		final boolean canScrollZoomedChart = fTourChartConfig.scrollZoomedGraph;
		final boolean canAutoZoomToSlider = fTourChartConfig.autoZoomToSlider;

		// update the chart
		setCanScrollZoomedChart(canScrollZoomedChart);
		setCanAutoZoomToSlider(canAutoZoomToSlider);

		// check the options
		fActionOptions.actionCanScrollZoomedChart.setChecked(canScrollZoomedChart);
		fActionOptions.actionCanAutoZoomToSlider.setChecked(canAutoZoomToSlider);

		// enable the options
		fActionOptions.actionCanScrollZoomedChart.setEnabled(true);
		fActionOptions.actionCanAutoZoomToSlider.setEnabled(true);
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
					listener.selectedTourChart(new TourChartSelection(TourChart.this));
				}
			});
		}
	}

	public TourData getTourData() {
		return fTourData;
	}

	public boolean isTourDirty() {
		return fIsTourDirty;
	}

	public void removeTourChartListener(ITourChartSelectionListener listener) {
		fSelectionListeners.remove(listener);
	}

	// public boolean setFocus() {
	// }

	/**
	 * Enable/disable the zoom options in the tour chart
	 * 
	 * @param isEnabled
	 */
	private void setEnabledZoomOptions(final boolean isEnabled) {

		fActionOptions.actionCanScrollZoomedChart.setEnabled(isEnabled);
		fActionOptions.actionCanAutoZoomToSlider.setEnabled(isEnabled);
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

		yData = (ChartDataYSerie) fChartDataModel.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE);
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
										final String tagIsAltiMinEnabled,
										final String minValue,
										final int yDataInfoId) {

		if (property.equals(tagIsAltiMinEnabled) || property.equals(minValue)) {

			final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

			final boolean isAltMinEnabled = prefStore.getBoolean(tagIsAltiMinEnabled);

			final ArrayList<ChartDataYSerie> yDataList = fChartDataModel.getYData();

			// get altimeter data from all y-data
			ChartDataYSerie yData = null;
			for (final ChartDataYSerie yDataIterator : yDataList) {
				final Integer yDataInfo = (Integer) yDataIterator
						.getCustomData(ChartDataYSerie.YDATA_INFO);
				if (yDataInfo == yDataInfoId) {
					yData = yDataIterator;
				}
			}

			if (yData != null) {

				if (isAltMinEnabled) {

					// set to pref store min value
					final int altMinValue = prefStore
							.getInt(ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE);

					yData.setVisibleMinValue(altMinValue);

				} else {
					// reset to the original min value
					yData.setVisibleMinValue(yData.getOriginalMinValue());
				}

				isChartModified = true;
			}
		}
		return isChartModified;
	}

	private void setPrefListeners() {
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

					final IPreferenceStore prefStore = TourbookPlugin
							.getDefault()
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
						TourManager.GRAPH_ALTIMETER);

				isChartModified = setMinDefaultValue(property,
						isChartModified,
						ITourbookPreferences.GRAPH_GRADIENT_MIN_ENABLED,
						ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE,
						TourManager.GRAPH_GRADIENT);

				if (isChartModified) {
					updateChart(true);
				}
			}
		};
		TourbookPlugin
				.getDefault()
				.getPluginPreferences()
				.addPropertyChangeListener(fPrefChangeListener);

	}

	private void setSegmentLayer(	final ArrayList<IChartLayer> segmentValueLayers,
									float[] segmentSerie,
									String customDataKey) {

		ChartDataYSerie yData = (ChartDataYSerie) fChartDataModel.getCustomData(customDataKey);

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
	 * set the x-marker position listener
	 * 
	 * @param isPositionListenerEnabled
	 *        <code>true</code> sets the position listener, <code>false</code> disables the
	 *        listener
	 * @param synchDirection
	 * @param tourChartListener
	 */
	public void setZoomMarkerPositionListener(	final boolean isPositionListenerEnabled,
												final TourChart tourChartListener) {

		// set/disable the position listener in the listener provider
		super.setZoomMarkerPositionListener(isPositionListenerEnabled ? tourChartListener : null);

		/*
		 * when the position listener is set, the zoom action will be deactivated
		 */
		if (isPositionListenerEnabled) {

			// disable zoom actions
			tourChartListener.setEnabledZoomActions(false);
			tourChartListener.setEnabledZoomOptions(false);

			// set the synched chart to auto-zoom
			tourChartListener.setCanScrollZoomedChart(false);
			tourChartListener.setCanAutoZoomToSlider(true);

			// hide the x-sliders
			fIsXSliderVisible = tourChartListener.isXSliderVisible();
			tourChartListener.setShowSlider(false);

			fireZoomMarkerPositionListener();

		} else {

			// enable zoom action
			tourChartListener.fActionOptions.actionCanScrollZoomedChart
					.setChecked(tourChartListener.getCanScrollZoomedChart());
			tourChartListener.fActionOptions.actionCanAutoZoomToSlider.setChecked(tourChartListener
					.getCanAutoZoomToSlider());

			tourChartListener.setEnabledZoomActions(true);
			tourChartListener.setEnabledZoomOptions(true);

			// restore the x-sliders
			tourChartListener.setShowSlider(fIsXSliderVisible);

			// reset the x-position in the listener
			tourChartListener.setZoomMarkerPositionIn(null);
			tourChartListener.zoomOut(true);
		}
	}

//	/**
//	 * update the chart by getting the tourdata from the db
//	 * 
//	 * @param tourId
//	 */
//	void updateChart(final long tourId) {
//
//		// load the tourdata from the database
//		TourData tourData = TourDatabase.getTourDataByTourId(tourId);
//
//		if (tourData != null) {
//			updateChart(tourData, fTourChartConfig, true);
//		}
//	}

	/**
	 * update the chart with the current tour data and chart configuration
	 * 
	 * @param keepMinMaxValues
	 *        <code>true</code> will keep the min/max values from the previous chart
	 */
	public void updateChart(final boolean keepMinMaxValues) {
		updateChart(fTourData, fTourChartConfig, keepMinMaxValues);
	}

	/**
	 * Update the chart by providing new tourdata and chart configuration which is used to create
	 * the chart data model
	 * 
	 * @param tourData
	 * @param chartConfig
	 * @param keepMinMaxValues
	 *        when set to <code>true</code> keep the min/max values from the previous chart
	 */
	public void updateChart(final TourData tourData,
							final TourChartConfiguration chartConfig,
							final boolean keepMinMaxValues) {

		if (tourData == null || chartConfig == null) {
			return;
		}

		// keep min/max values for the 'old' chart in the chart config
		if (fTourChartConfig != null
				&& fTourChartConfig.getMinMaxKeeper() != null
				&& keepMinMaxValues) {
			fTourChartConfig.getMinMaxKeeper().saveMinMaxValues(fChartDataModel);
		}

		// set current tour data and chart config
		fTourData = tourData;
		fTourChartConfig = chartConfig;

		fChartDataModel = TourManager.getInstance().createChartDataModel(tourData, chartConfig);

		if (fShowZoomActions) {
			createTourActions();
			enableActions();
		}

		// restore min/max values from the chart config
		if (chartConfig.getMinMaxKeeper() != null && keepMinMaxValues) {
			chartConfig.getMinMaxKeeper().restoreMinMaxValues(fChartDataModel);
		}

		if (fChartDataModelListener != null) {
			fChartDataModelListener.dataModelChanged(fChartDataModel);
		}

		createSegmentsLayer();
		createMarkerLayer();

		setGraphLayers();
		setChartDataModel(fChartDataModel);
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
			createSegmentsLayer();
		} else {
			fSegmentLayer = null;
			fSegmentValueLayer = null;
			setGraphAlpha(0xD0);
		}

		setGraphLayers();
		updateChartLayers();

		/*
		 * the chart needs to be redrawn because the alpha for filling the chart has changed
		 */
		redrawChart();
	}

}
