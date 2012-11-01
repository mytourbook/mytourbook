/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartComponentAxis;
import net.tourbook.chart.ChartComponents;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartLabel;
import net.tourbook.chart.ChartMarker;
import net.tourbook.chart.ChartMarkerLayer;
import net.tourbook.chart.ChartYDataMinMaxKeeper;
import net.tourbook.chart.IChartLayer;
import net.tourbook.chart.IFillPainter;
import net.tourbook.chart.IHoveredListener;
import net.tourbook.chart.ITooltipOwner;
import net.tourbook.colors.ColorDefinition;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.common.PointLong;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.IToolTipHideListener;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.photo.MergeTour;
import net.tourbook.photo.PhotoWrapper;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceTourChart;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourChartSelectionListener;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.TourInfoToolTipProvider;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.action.ActionCanAutoZoomToSlider;
import net.tourbook.ui.tourChart.action.ActionCanMoveSlidersWhenZoomed;
import net.tourbook.ui.tourChart.action.ActionChartOptions;
import net.tourbook.ui.tourChart.action.ActionGraph;
import net.tourbook.ui.tourChart.action.ActionHrZoneDropDownMenu;
import net.tourbook.ui.tourChart.action.ActionHrZoneGraphType;
import net.tourbook.ui.tourChart.action.ActionShowBreaktimeValues;
import net.tourbook.ui.tourChart.action.ActionShowSRTMData;
import net.tourbook.ui.tourChart.action.ActionShowStartTime;
import net.tourbook.ui.tourChart.action.ActionShowTourMarker;
import net.tourbook.ui.tourChart.action.ActionShowValuePointToolTip;
import net.tourbook.ui.tourChart.action.ActionTourPhotos;
import net.tourbook.ui.tourChart.action.ActionXAxisDistance;
import net.tourbook.ui.tourChart.action.ActionXAxisTime;
import net.tourbook.ui.tourChart.action.TCActionHandler;
import net.tourbook.ui.tourChart.action.TCActionHandlerManager;
import net.tourbook.ui.tourChart.action.TCActionProxy;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * The tour chart extends the chart with all the functionality for a tour chart
 */
public class TourChart extends Chart {

	private static final String				ID										= "net.tourbook.ui.tourChart";									//$NON-NLS-1$

	public static final String				COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER		= "net.tourbook.command.tourChart.canAutoZoomToSlider";		//$NON-NLS-1$
	public static final String				COMMAND_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED	= "net.tourbook.command.tourChart.canMoveSlidersWhenZoomed";	//$NON-NLS-1$
	public static final String				COMMAND_ID_EDIT_CHART_PREFERENCES		= "net.tourbook.command_EditChartPreferences";					//$NON-NLS-1$
	public static final String				COMMAND_ID_IS_SHOW_BREAKTIME_VALUES		= "net.tourbook.command_TourChart_IsShowBreaktimeValues";		//$NON-NLS-1$
	public static final String				COMMAND_ID_IS_SHOW_SRTM_DATA			= "net.tourbook.command.tourChart.isShowSRTMData";				//$NON-NLS-1$
	public static final String				COMMAND_ID_IS_SHOW_START_TIME			= "net.tourbook.command.tourChart.isShowStartTime";			//$NON-NLS-1$
	public static final String				COMMAND_ID_IS_SHOW_TOUR_MARKER			= "net.tourbook.command_TourChart_IsShowTourMarker";			//$NON-NLS-1$
	public static final String				COMMAND_ID_IS_SHOW_VALUEPOINT_TOOLTIP	= "net.tourbook.command_TourChart_IsShowValuePointToolTip";	//$NON-NLS-1$
	public static final String				COMMAND_ID_X_AXIS_DISTANCE				= "net.tourbook.command.tourChart.xAxisDistance";				//$NON-NLS-1$
	public static final String				COMMAND_ID_X_AXIS_TIME					= "net.tourbook.command.tourChart.xAxisTime";					//$NON-NLS-1$
	public static final String				COMMAND_ID_IS_SHOW_TOUR_PHOTOS			= "COMMAND_ID_TOUR_PHOTOS";									//$NON-NLS-1$

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

	public static final String				COMMAND_ID_HR_ZONE_DROPDOWN_MENU		= "net.tourbook.command_HrZone_DropDownMenu";					//$NON-NLS-1$
	public static final String				COMMAND_ID_HR_ZONE_STYLE_GRAPH_TOP		= "net.tourbook.command_HrZone_Style_GraphTop";				//$NON-NLS-1$
	public static final String				COMMAND_ID_HR_ZONE_STYLE_NO_GRADIENT	= "net.tourbook.command_HrZone_Style_NoGradient";				//$NON-NLS-1$
	public static final String				COMMAND_ID_HR_ZONE_STYLE_WHITE_TOP		= "net.tourbook.command_HrZone_Style_WhiteTop";				//$NON-NLS-1$
	public static final String				COMMAND_ID_HR_ZONE_STYLE_WHITE_BOTTOM	= "net.tourbook.command_HrZone_Style_WhiteBottom";				//$NON-NLS-1$

	private final IPreferenceStore			_prefStore								= TourbookPlugin.getDefault() //
																							.getPreferenceStore();

	private final IDialogSettings			_state									= TourbookPlugin.getDefault()//
																							.getDialogSettingsSection(
																									ID);
	private TourData						_tourData;

	private TourChartConfiguration			_tourChartConfig;
	private final boolean					_isShowActions;

	private Map<String, TCActionProxy>		_actionProxies;

	private final TCActionHandlerManager	_tcActionHandlerManager					= TCActionHandlerManager
																							.getInstance();
	/**
	 * datamodel listener is called when the chart data is created
	 */
	private IDataModelListener				_chartDataModelListener;

	private final ListenerList				_selectionListeners						= new ListenerList();
	private final ListenerList				_xAxisSelectionListener					= new ListenerList();
	private IPropertyChangeListener			_prefChangeListener;
	private boolean							_isSegmentLayerVisible					= false;

	private boolean							_is2ndAltiLayerVisible					= false;
	private boolean							_isMouseModeSet;

	/*
	 * UI controls
	 */
	private ChartMarkerLayer				_layerMarker;

	private ChartSegmentLayer				_layerSegment;
	private ChartSegmentValueLayer			_layerSegmentValue;
	private ChartLayer2ndAltiSerie			_layer2ndAltiSerie;
	private ChartLayerPhoto					_layerPhoto;
	private I2ndAltiLayer					_layer2ndAlti;
	private IFillPainter					_hrZonePainter;
	private ActionChartOptions				_actionOptions;

	private TourToolTip						_tourToolTip;

	private TourInfoToolTipProvider			_tourInfoToolTipProvider;
	private ValuePointToolTipUI				_valuePointToolTip;

	private ChartPhotoToolTip				_photoTooltip;
	private ControlListener					_ttControlListener						= new ControlListener();

	private ChartPhotoOverlay				_photoOverlay							= new ChartPhotoOverlay();
	private Color							_photoOverlayBGColor;

	/**
	 * This listener is added to ALL widgets within the tooltip shell.
	 */
	private class ControlListener implements Listener {

		TourChart	__tourChart	= TourChart.this;

		public void handleEvent(final Event event) {

			if (__tourChart.isDisposed()) {
				return;
			}

			if (event.widget instanceof Control) {

				switch (event.type) {
				case SWT.MouseEnter:

//					System.out.println(UI.timeStamp() + " TourChart\tEnter\t" + event.widget);
//					// TODO remove SYSTEM.OUT.PRINTLN

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

//					System.out.println(UI.timeStamp() + " TourChart\tExit\t" + hoveredControl);
//					// TODO remove SYSTEM.OUT.PRINTLN

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

	public class HoveredListener implements IHoveredListener {

		@Override
		public void hideTooltip() {

			if (_photoTooltip != null) {
				_photoTooltip.hide();
			}
		}

		@Override
		public void hoveredValue(	final int hoveredValueIndex,
									final PointLong devHoveredValue,
									final int devXMouseMove,
									final int devYMouseMove) {

			if (_tourData == null) {
				return;
			}

			if (_tourChartConfig.isShowTourPhotos == false) {
				return;
			}

			// check if photos are available
			final MergeTour mergeTour = _tourData.mergeTour;
			if (mergeTour == null) {
				return;
			}

			final ChartLayerPhoto photoLayer = getPhotoLayer();
			if (photoLayer == null) {
				return;
			}

			_photoTooltip.showChartPhotoToolTip(photoLayer, devHoveredValue, devXMouseMove, devYMouseMove);
		}
	}

	public TourChart(final Composite parent, final int style, final boolean isShowActions) {

		super(parent, style);

		_isShowActions = isShowActions;

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
				onDispose();
			}
		});

		addControlListener(this);

		addPrefListeners();

		final ColorDefinition colorDefinition = GraphColorProvider.getInstance()//
				.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_HISTORY);
		_photoOverlayBGColor = new Color(getDisplay(), colorDefinition.getLineColor());
		_photoOverlay.setBackgroundColor(_photoOverlayBGColor);

		/*
		 * set values from pref store
		 */
		graphTransparencyLine = _prefStore.getInt(ITourbookPreferences.GRAPH_TRANSPARENCY_LINE);
		graphTransparencyFilling = _prefStore.getInt(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING);
		graphAntialiasing = _prefStore.getBoolean(ITourbookPreferences.GRAPH_ANTIALIASING) ? SWT.ON : SWT.OFF;

		gridVerticalDistance = _prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE);
		gridHorizontalDistance = _prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE);

		isShowHorizontalGridLines = _prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES);
		isShowVerticalGridLines = _prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES);

		setShowMouseMode();

		/*
		 * setup tour info icon into the left axis
		 */
		_tourInfoToolTipProvider = new TourInfoToolTipProvider();
		_tourToolTip = new TourToolTip(getToolTipControl());
		_tourToolTip.addToolTipProvider(_tourInfoToolTipProvider);

		_tourToolTip.addHideListener(new IToolTipHideListener() {
			@Override
			public void afterHideToolTip(final Event event) {

				// hide hovered image
				getToolTipControl().afterHideToolTip(event);
			}
		});
		setTourToolTipProvider(_tourInfoToolTipProvider);

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
		setValuePointToolTipProvider(_valuePointToolTip = new ValuePointToolTipUI(vpToolTipOwner, _state));

		_photoTooltip = new ChartPhotoToolTip(this);

//		_photoOverlay.setPhotoToolTip(_photoTooltip);

		setHoveredListener(new HoveredListener());
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

	public void actionShowBreaktimeValues(final boolean isItemChecked) {

		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_BREAKTIME_VALUES_VISIBLE, isItemChecked);
		_tourChartConfig.isShowBreaktimeValues = isItemChecked;

		updateTourChart(true);

		setCommandChecked(COMMAND_ID_IS_SHOW_BREAKTIME_VALUES, isItemChecked);
	}

	/**
	 * Toggle HR zone background
	 */
	public void actionShowHrZones() {

		final boolean isHrZonevisible = !_tourChartConfig.isHrZoneDisplayed;

		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_HR_ZONE_BACKGROUND_VISIBLE, isHrZonevisible);
		_tourChartConfig.isHrZoneDisplayed = isHrZonevisible;

		updateTourChart(true);
	}

	/**
	 * @param isActionChecked
	 * @param selectedGraphType
	 */
	public void actionShowHrZoneStyle(final Boolean isActionChecked, final String selectedGraphType) {

		final String previousGraphType = _tourChartConfig.hrZoneStyle;

		// check if the same action was selected
		if (isActionChecked && selectedGraphType.equals(previousGraphType)) {
//			return;
		}

		_prefStore.setValue(ITourbookPreferences.GRAPH_HR_ZONE_STYLE, selectedGraphType);
		_tourChartConfig.hrZoneStyle = selectedGraphType;

		setCommandChecked(
				COMMAND_ID_HR_ZONE_STYLE_GRAPH_TOP,
				COMMAND_ID_HR_ZONE_STYLE_GRAPH_TOP.equals(selectedGraphType));
		setCommandChecked(
				COMMAND_ID_HR_ZONE_STYLE_NO_GRADIENT,
				COMMAND_ID_HR_ZONE_STYLE_NO_GRADIENT.equals(selectedGraphType));
		setCommandChecked(
				COMMAND_ID_HR_ZONE_STYLE_WHITE_TOP,
				COMMAND_ID_HR_ZONE_STYLE_WHITE_TOP.equals(selectedGraphType));
		setCommandChecked(
				COMMAND_ID_HR_ZONE_STYLE_WHITE_BOTTOM,
				COMMAND_ID_HR_ZONE_STYLE_WHITE_BOTTOM.equals(selectedGraphType));

		if (_tourChartConfig.isHrZoneDisplayed == false) {
			// HR zones are not yet displayed
			actionShowHrZones();
		} else {
			updateTourChart(true);
		}
	}

	public void actionShowSRTMData(final boolean isItemChecked) {

		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_SRTM_VISIBLE, isItemChecked);

		_tourChartConfig.isSRTMDataVisible = isItemChecked;
		updateTourChart(true);

		setCommandChecked(COMMAND_ID_IS_SHOW_SRTM_DATA, isItemChecked);
	}

	public void actionShowStartTime(final Boolean isItemChecked) {

		_tourChartConfig.isShowStartTime = isItemChecked;
		updateTourChart(true);

		setCommandChecked(COMMAND_ID_IS_SHOW_START_TIME, isItemChecked);
	}

	public void actionShowTourMarker(final Boolean isItemChecked) {

		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_MARKER_VISIBLE, isItemChecked);

		_tourChartConfig.isShowTourMarker = isItemChecked;

		updateLayerMarker(isItemChecked);

		setCommandChecked(COMMAND_ID_IS_SHOW_TOUR_MARKER, isItemChecked);
	}

	public void actionShowTourPhotos(final boolean isItemChecked) {

		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_TOUR_PHOTO_VISIBLE, isItemChecked);

		_tourChartConfig.isShowTourPhotos = isItemChecked;
		updateTourChart(true);

		setCommandChecked(COMMAND_ID_IS_SHOW_TOUR_PHOTOS, isItemChecked);
	}

	public void actionShowValuePointToolTip(final Boolean isItemChecked) {

		// set in pref store, tooltip is listening pref store modifications
		_prefStore.setValue(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE, isItemChecked);

//		_valuePointToolTip.setVisible(isItemChecked);

		setCommandChecked(COMMAND_ID_IS_SHOW_VALUEPOINT_TOOLTIP, isItemChecked);

		// chart needs not to be updated but update the actions
		enableTourActions();
	}

	/**
	 * Set the X-axis to distance
	 * 
	 * @param isChecked
	 */
	public void actionXAxisDistance(final boolean isChecked) {

		// check if the distance axis button was pressed
		if (isChecked && !_tourChartConfig.isShowTimeOnXAxis) {
			return;
		}

		if (isChecked) {

			// show distance on x axis

			_tourChartConfig.isShowTimeOnXAxis = !_tourChartConfig.isShowTimeOnXAxis;
			_tourChartConfig.isShowTimeOnXAxisBackup = _tourChartConfig.isShowTimeOnXAxis;

			switchSlidersTo2ndXData();
			updateTourChart(false);
		}

		// toggle time and distance buttons
		setCommandChecked(TourChart.COMMAND_ID_X_AXIS_TIME, !isChecked);
		setCommandChecked(TourChart.COMMAND_ID_X_AXIS_DISTANCE, isChecked);
	}

	/**
	 * @param isChecked
	 */
	public void actionXAxisTime(final boolean isChecked) {

		// check if the time axis button was already pressed
		if (isChecked && _tourChartConfig.isShowTimeOnXAxis) {

			// x-axis already shows time, toggle between tour start time and tour time

			final boolean isShowStartTime = !_tourChartConfig.isShowStartTime;

			_tourChartConfig.isShowStartTime = isShowStartTime;

			/**
			 * keepMinMaxValues must be set to false, that a deeply zoomed in chart can display
			 * x-axis units
			 */
			updateTourChart(false);

			setCommandChecked(COMMAND_ID_IS_SHOW_START_TIME, isShowStartTime);

			return;
		}

		if (isChecked) {

			// show time on x axis

			_tourChartConfig.isShowTimeOnXAxis = !_tourChartConfig.isShowTimeOnXAxis;
			_tourChartConfig.isShowTimeOnXAxisBackup = _tourChartConfig.isShowTimeOnXAxis;

			switchSlidersTo2ndXData();
			updateTourChart(false);
		}

		// toggle time and distance buttons
		setCommandChecked(TourChart.COMMAND_ID_X_AXIS_TIME, isChecked);
		setCommandChecked(TourChart.COMMAND_ID_X_AXIS_DISTANCE, !isChecked);

		fireXAxisSelection(_tourChartConfig.isShowTimeOnXAxis);
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
			_tcActionHandlerManager.updateTourActionHandlers(partSite, this);

			// update the action handlers in the chart
			updateChartActionHandlers();
		}
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
			public void propertyChange(final PropertyChangeEvent event) {

				if (_tourChartConfig == null) {
					return;
				}

				final String property = event.getProperty();
				boolean isChartModified = false;
				boolean keepMinMax = true;

				if (property.equals(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED)
						|| property.equals(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER)) {

					// zoom preferences has changed

					TourManager.updateZoomOptionsInChartConfig(_tourChartConfig, _prefStore);

					isChartModified = true;

				} else if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {

					/*
					 * when the chart is computed, the modified colors are read from the preferences
					 */
					isChartModified = true;

					// dispose old colors
					disposeColors();

				} else if (property.equals(ITourbookPreferences.GRAPH_TRANSPARENCY_LINE)
						|| property.equals(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING)
						|| property.equals(ITourbookPreferences.GRAPH_ANTIALIASING)) {

					graphTransparencyLine = _prefStore.getInt(ITourbookPreferences.GRAPH_TRANSPARENCY_LINE);
					graphTransparencyFilling = _prefStore.getInt(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING);
					graphAntialiasing = _prefStore.getBoolean(//
							ITourbookPreferences.GRAPH_ANTIALIASING) ? SWT.ON : SWT.OFF;

					isChartModified = true;

				} else if (property.equals(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE)) {

					final Boolean isVisible = (Boolean) event.getNewValue();

					setCommandChecked(COMMAND_ID_IS_SHOW_VALUEPOINT_TOOLTIP, isVisible);

					// chart needs not to be updated but update the actions
//					enableTourActions();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

//					UI.updateUnits();

					isChartModified = true;
					keepMinMax = false;

					_valuePointToolTip.reopen();

				} else if (property.equals(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES)) {

					gridVerticalDistance = _prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE);
					gridHorizontalDistance = _prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE);

					isShowHorizontalGridLines = _prefStore.getBoolean(//
							ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES);
					isShowVerticalGridLines = _prefStore.getBoolean(//
							ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES);

					isChartModified = true;
				}

				isChartModified |= setMinDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_PACE_MINMAX_IS_ENABLED,
						ITourbookPreferences.GRAPH_PACE_MIN_VALUE,
						TourManager.GRAPH_PACE,
						60);

				isChartModified |= setMaxDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_PACE_MINMAX_IS_ENABLED,
						ITourbookPreferences.GRAPH_PACE_MAX_VALUE,
						TourManager.GRAPH_PACE,
						60);

				isChartModified |= setMinDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_ALTIMETER_MIN_IS_ENABLED,
						ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE,
						TourManager.GRAPH_ALTIMETER,
						0);

				isChartModified |= setMinDefaultValue(
						property,
						isChartModified,
						ITourbookPreferences.GRAPH_GRADIENT_MIN_IS_ENABLED,
						ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE,
						TourManager.GRAPH_GRADIENT,
						0);

				if (isChartModified) {
					updateTourChart(keepMinMax);
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	public void addTourChartListener(final ITourChartSelectionListener listener) {
		_selectionListeners.add(listener);
	}

	public void addXAxisSelectionListener(final IXAxisSelectionListener listener) {
		_xAxisSelectionListener.add(listener);
	}

	/**
	 * Create the action proxies for all tour actions
	 */
	private void createAction10TourActionProxies() {

		// check if action proxies are created
		if (_actionProxies != null) {
			return;
		}

		_actionProxies = new HashMap<String, TCActionProxy>();

		// graph actions
		createAction20GraphActionProxies();

		String cmdId;
		final boolean useInternalActionBar = useInternalActionBar();

		cmdId = COMMAND_ID_IS_SHOW_TOUR_PHOTOS;
		_actionProxies.put(cmdId, //
				new TCActionProxy(cmdId, useInternalActionBar ? //
						new ActionTourPhotos(this)
						: null));

		cmdId = COMMAND_ID_HR_ZONE_DROPDOWN_MENU;
		_actionProxies.put(cmdId, //
				new TCActionProxy(cmdId, useInternalActionBar ? //
						new ActionHrZoneDropDownMenu(this)
						: null));

		cmdId = COMMAND_ID_HR_ZONE_STYLE_GRAPH_TOP;
		_actionProxies.put(cmdId, //
				new TCActionProxy(cmdId, useInternalActionBar ? //
						new ActionHrZoneGraphType(this, cmdId, Messages.Tour_Action_HrZoneGraphType_Default)
						: null));

		cmdId = COMMAND_ID_HR_ZONE_STYLE_NO_GRADIENT;
		_actionProxies.put(cmdId, //
				new TCActionProxy(cmdId, useInternalActionBar ? //
						new ActionHrZoneGraphType(this, cmdId, Messages.Tour_Action_HrZoneGraphType_NoGradient)
						: null));

		cmdId = COMMAND_ID_HR_ZONE_STYLE_WHITE_TOP;
		_actionProxies.put(cmdId, //
				new TCActionProxy(cmdId, useInternalActionBar ? //
						new ActionHrZoneGraphType(this, cmdId, Messages.Tour_Action_HrZoneGraphType_WhiteTop)
						: null));

		cmdId = COMMAND_ID_HR_ZONE_STYLE_WHITE_BOTTOM;
		_actionProxies.put(cmdId, //
				new TCActionProxy(cmdId, useInternalActionBar ? //
						new ActionHrZoneGraphType(this, cmdId, Messages.Tour_Action_HrZoneGraphType_WhiteBottom)
						: null));

		cmdId = COMMAND_ID_X_AXIS_TIME;
		_actionProxies.put(cmdId, //
				new TCActionProxy(cmdId, useInternalActionBar ? //
						new ActionXAxisTime(this)
						: null));

		cmdId = COMMAND_ID_X_AXIS_DISTANCE;
		_actionProxies.put(cmdId, //
				new TCActionProxy(cmdId, useInternalActionBar ? //
						new ActionXAxisDistance(this)
						: null));

		cmdId = COMMAND_ID_IS_SHOW_START_TIME;
		_actionProxies.put(cmdId, //
				new TCActionProxy(cmdId, useInternalActionBar ? //
						new ActionShowStartTime(this)
						: null));

		cmdId = COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER;
		_actionProxies.put(cmdId, //
				new TCActionProxy(cmdId, useInternalActionBar ? //
						new ActionCanAutoZoomToSlider(this)
						: null));

		cmdId = COMMAND_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED;
		_actionProxies.put(cmdId, //
				new TCActionProxy(cmdId, useInternalActionBar ? //
						new ActionCanMoveSlidersWhenZoomed(this)
						: null));

		cmdId = COMMAND_ID_IS_SHOW_SRTM_DATA;
		_actionProxies.put(cmdId, //
				new TCActionProxy(cmdId, useInternalActionBar ? //
						new ActionShowSRTMData(this)
						: null));

		cmdId = COMMAND_ID_IS_SHOW_TOUR_MARKER;
		_actionProxies.put(cmdId, //
				new TCActionProxy(cmdId, useInternalActionBar ? //
						new ActionShowTourMarker(this)
						: null));

		cmdId = COMMAND_ID_IS_SHOW_BREAKTIME_VALUES;
		_actionProxies.put(cmdId, //
				new TCActionProxy(cmdId, useInternalActionBar ? //
						new ActionShowBreaktimeValues(this)
						: null));

		cmdId = COMMAND_ID_IS_SHOW_VALUEPOINT_TOOLTIP;
		_actionProxies.put(cmdId, //
				new TCActionProxy(cmdId, useInternalActionBar ? //
						new ActionShowValuePointToolTip(this)
						: null));

		cmdId = COMMAND_ID_EDIT_CHART_PREFERENCES;
		_actionProxies.put(cmdId, //
				new TCActionProxy(cmdId, useInternalActionBar ? //
						new ActionOpenPrefDialog(
								Messages.Tour_Action_EditChartPreferences,
								PrefPageAppearanceTourChart.ID) : null));
	}

	/**
	 * Create action proxies for all chart graphs
	 */
	private void createAction20GraphActionProxies() {

		createAction30GraphActionProxy(
				TourManager.GRAPH_ALTITUDE,
				COMMAND_ID_GRAPH_ALTITUDE,
				Messages.Graph_Label_Altitude,
				Messages.Tour_Action_graph_altitude_tooltip,
				Messages.Image__graph_altitude,
				Messages.Image__graph_altitude_disabled);

		createAction30GraphActionProxy(
				TourManager.GRAPH_SPEED,
				COMMAND_ID_GRAPH_SPEED,
				Messages.Graph_Label_Speed,
				Messages.Tour_Action_graph_speed_tooltip,
				Messages.Image__graph_speed,
				Messages.Image__graph_speed_disabled);

		createAction30GraphActionProxy(
				TourManager.GRAPH_PACE,
				COMMAND_ID_GRAPH_PACE,
				Messages.Graph_Label_Pace,
				Messages.Tour_Action_graph_pace_tooltip,
				Messages.Image__graph_pace,
				Messages.Image__graph_pace_disabled);

		createAction30GraphActionProxy(
				TourManager.GRAPH_POWER,
				COMMAND_ID_GRAPH_POWER,
				Messages.Graph_Label_Power,
				Messages.Tour_Action_graph_power_tooltip,
				Messages.Image__graph_power,
				Messages.Image__graph_power_disabled);

		createAction30GraphActionProxy(
				TourManager.GRAPH_ALTIMETER,
				COMMAND_ID_GRAPH_ALTIMETER,
				Messages.Graph_Label_Altimeter,
				Messages.Tour_Action_graph_altimeter_tooltip,
				Messages.Image__graph_altimeter,
				Messages.Image__graph_altimeter_disabled);

		createAction30GraphActionProxy(
				TourManager.GRAPH_PULSE,
				COMMAND_ID_GRAPH_PULSE,
				Messages.Graph_Label_Heartbeat,
				Messages.Tour_Action_graph_heartbeat_tooltip,
				Messages.Image__graph_heartbeat,
				Messages.Image__graph_heartbeat_disabled);

		createAction30GraphActionProxy(
				TourManager.GRAPH_TEMPERATURE,
				COMMAND_ID_GRAPH_TEMPERATURE,
				Messages.Graph_Label_Temperature,
				Messages.Tour_Action_graph_temperature_tooltip,
				Messages.Image__graph_temperature,
				Messages.Image__graph_temperature_disabled);

		createAction30GraphActionProxy(
				TourManager.GRAPH_CADENCE,
				COMMAND_ID_GRAPH_CADENCE,
				Messages.Graph_Label_Cadence,
				Messages.Tour_Action_graph_cadence_tooltip,
				Messages.Image__graph_cadence,
				Messages.Image__graph_cadence_disabled);

		createAction30GraphActionProxy(
				TourManager.GRAPH_GRADIENT,
				COMMAND_ID_GRAPH_GRADIENT,
				Messages.Graph_Label_Gradient,
				Messages.Tour_Action_graph_gradient_tooltip,
				Messages.Image__graph_gradient,
				Messages.Image__graph_gradient_disabled);

		createAction30GraphActionProxy(
				TourManager.GRAPH_TOUR_COMPARE,
				COMMAND_ID_GRAPH_TOUR_COMPARE,
				Messages.Graph_Label_Tour_Compare,
				Messages.Tour_Action_graph_tour_compare_tooltip,
				Messages.Image__graph_tour_compare,
				Messages.Image__graph_tour_compare_disabled);
	}

	/**
	 * Create the action proxy for a graph action
	 * 
	 * @param graphId
	 * @param commandId
	 * @param label
	 * @param toolTip
	 * @param imageEnabled
	 * @param imageDisabled
	 */
	private void createAction30GraphActionProxy(final int graphId,
												final String commandId,
												final String label,
												final String toolTip,
												final String imageEnabled,
												final String imageDisabled) {

		Action action = null;

		if (useInternalActionBar()) {
			action = new ActionGraph(this, graphId, label, toolTip, imageEnabled, imageDisabled);
		}

		_actionProxies.put(getProxyId(graphId), new TCActionProxy(commandId, action));
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
	 * @param isMarkerVisibleEnforced
	 *            When <code>true</code> the marker must be drawn, otherwise
	 *            {@link TourChartConfiguration#isShowTourMarker} determines if the markers are
	 *            drawn or not.
	 */
	private void createLayer_Marker(final boolean isMarkerVisibleEnforced) {

		if (isMarkerVisibleEnforced == false && _tourChartConfig.isShowTourMarker == false) {
			return;
		}

		// set data serie for the x-axis
		final double[] xAxisSerie = _tourChartConfig.isShowTimeOnXAxis ? //
				_tourData.getTimeSerieDouble()
				: _tourData.getDistanceSerieDouble();

		_layerMarker = new ChartMarkerLayer();
		_layerMarker.setLineColor(new RGB(50, 100, 10));

		final Collection<TourMarker> tourMarkerList = _tourData.getTourMarkers();

		for (final TourMarker tourMarker : tourMarkerList) {

			if (tourMarker.isMarkerVisible() == false) {
				// skip marker
				continue;
			}

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

			_layerMarker.addLabel(chartLabel);
		}
	}

	private void createLayer_Photo() {

		// reset layer
		_photoOverlay.setPhotoLayer(null);

		if (_tourChartConfig.isShowTourPhotos == false) {
			return;
		}

		final MergeTour mergeTour = _tourData.mergeTour;

		if (mergeTour == null) {
			// no photos are available for this tour
			return;
		}

		final ArrayList<PhotoWrapper> tourPhotos = mergeTour.tourPhotos;
		final int numberOfPhotos = tourPhotos.size();
		if (numberOfPhotos == 0) {
			// no photos are available for this tour
			return;
		}

		final int[] timeSerie = _tourData.timeSerie;
		final long[] historySerie = _tourData.timeSerieHistory;

		final boolean isTimeSerie = timeSerie != null;
		final boolean isHistorySerie = historySerie != null;

		if (isTimeSerie == false && isHistorySerie == false) {
			// this is a manually created tour
			return;
		}

		if (isTimeSerie == false && isHistorySerie) {
			// hide x slider in history chart
			setShowSlider(false);
		} else {
			setShowSlider(true);
		}

		/*
		 * at least 1 photo is available
		 */

		final ArrayList<ChartPhoto> chartPhotos = new ArrayList<ChartPhoto>();

		_layerPhoto = new ChartLayerPhoto(chartPhotos, _photoOverlay, _photoOverlayBGColor);

		setCustomOverlay(_photoOverlay);

		_photoOverlay.setPhotoLayer(_layerPhoto);

		final long tourStart = _tourData.getTourStartTime().getMillis() / 1000;
		final int numberOfTimeSlices = isTimeSerie ? timeSerie.length : historySerie.length;

		/*
		 * set photos for tours which has max 1 value point
		 */
		if (numberOfTimeSlices <= 1) {
			for (final PhotoWrapper photoWrapper : tourPhotos) {
				chartPhotos.add(new ChartPhoto(photoWrapper, 0, 0));
			}
			return;
		}

		/*
		 * set photos for tours which has more than 1 value point
		 */

		// set value serie for the x-axis
		double[] xAxisSerie = null;
		xAxisSerie = _tourChartConfig.isShowTimeOnXAxis //
				? _tourData.getTimeSerieWithTimeZoneAdjusted()
				: _tourData.getDistanceSerieDouble();

		long timeSliceEnd;
		if (isTimeSerie) {
			timeSliceEnd = tourStart + (long) (timeSerie[1] / 2.0);
		} else {
			timeSliceEnd = tourStart + (long) (historySerie[1] / 2.0);
		}

		int photoIndex = 0;
		PhotoWrapper photoWrapper = tourPhotos.get(photoIndex);

		// tour time serie, fit photos into a tour

		int timeIndex = 0;

		while (true) {

			// check if a photo is in the current time slice
			while (true) {

				final long imageAdjustedTime = photoWrapper.adjustedTime;
				long imageTime = 0;

				if (imageAdjustedTime != Long.MIN_VALUE) {
					imageTime = imageAdjustedTime;
				} else {
					imageTime = photoWrapper.imageSortingTime;
				}

				final long photoTime = imageTime / 1000;

				if (photoTime <= timeSliceEnd) {

					// photo is available in the current time slice

					final double xValue = xAxisSerie[timeIndex];

					chartPhotos.add(new ChartPhoto(photoWrapper, xValue, timeIndex));

					photoIndex++;

				} else {

					// advance to the next time slice

					break;
				}

				if (photoIndex < numberOfPhotos) {
					photoWrapper = tourPhotos.get(photoIndex);
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
			timeIndex++;

			if (timeIndex >= numberOfTimeSlices - 1) {

				/*
				 * end of tour is reached but there are still photos available, set remaining photos
				 * at the end of the tour
				 */

				while (true) {

					final double xValue = xAxisSerie[numberOfTimeSlices - 1];
					chartPhotos.add(new ChartPhoto(photoWrapper, xValue, timeIndex));

					photoIndex++;

					if (photoIndex < numberOfPhotos) {
						photoWrapper = tourPhotos.get(photoIndex);
					} else {
						break;
					}
				}

			} else {

				long valuePointTime;
				long sliceDuration;
				if (isTimeSerie) {
					valuePointTime = timeSerie[timeIndex];
					sliceDuration = timeSerie[timeIndex + 1] - valuePointTime;
				} else {
					valuePointTime = historySerie[timeIndex];
					sliceDuration = historySerie[timeIndex + 1] - valuePointTime;
				}

				timeSliceEnd = tourStart + valuePointTime + (sliceDuration / 2);
			}
		}
	}

	/**
	 * Creates the layers from the segmented tour data
	 */
	private void createLayer_Segment() {

		if (_tourData == null) {
			return;
		}

		final int[] segmentSerie = _tourData.segmentSerieIndex;

		if ((segmentSerie == null) || (_isSegmentLayerVisible == false)) {
			// no segmented tour data available or segments are invisible
			return;
		}

		final double[] xDataSerie = _tourChartConfig.isShowTimeOnXAxis ? //
				_tourData.getTimeSerieDouble()
				: _tourData.getDistanceSerieDouble();

		/*
		 * create segment layer
		 */
		_layerSegment = new ChartSegmentLayer();
		_layerSegment.setLineColor(new RGB(0, 177, 219));

		for (final int serieIndex : segmentSerie) {

			final ChartMarker chartMarker = new ChartMarker();

			chartMarker.graphX = xDataSerie[serieIndex];
			chartMarker.serieIndex = serieIndex;

			_layerSegment.addMarker(chartMarker);
		}

		/*
		 * create segment value layer
		 */
		_layerSegmentValue = new ChartSegmentValueLayer();
		_layerSegmentValue.setLineColor(new RGB(231, 104, 38));
		_layerSegmentValue.setTourData(_tourData);
		_layerSegmentValue.setXDataSerie(xDataSerie);

		// draw the graph lighter that the segments are more visible
		setGraphAlpha(0.5);
	}

	private void createPainter_HrZone() {

		if (_tourChartConfig.isHrZoneDisplayed) {
			_hrZonePainter = new HrZonePainter();
		} else {
			_hrZonePainter = null;
		}
	}

	/**
	 * Creates the handlers for the tour chart actions
	 * 
	 * @param workbenchWindow
	 * @param tourChartConfig
	 */
	public void createTourEditorActionHandlers(final TourChartConfiguration tourChartConfig) {

		_tourChartConfig = tourChartConfig;

		_tcActionHandlerManager.createActionHandlers();
		createAction10TourActionProxies();
		createChartActionHandlers();
	}

	public void enableGraphAction(final int graphId, final boolean isEnabled) {

		if (_actionProxies == null) {
			return;
		}

		final TCActionProxy actionProxy = _actionProxies.get(getProxyId(graphId));
		if (actionProxy != null) {
			actionProxy.setEnabled(isEnabled);
		}

	}

	/**
	 * Enable/disable the graph action buttons, the visible state of a graph is defined in the tour
	 * chart config.
	 */
	public void enableTourActions() {

		/*
		 * all graph actions
		 */
		final int[] allGraphIds = TourManager.getAllGraphIDs();
		final ArrayList<Integer> visibleGraphIds = _tourChartConfig.getVisibleGraphs();
		final ArrayList<Integer> enabledGraphIds = new ArrayList<Integer>();

		// get all graph ids which can be displayed
		for (final ChartDataSerie xyDataIterator : getChartDataModel().getXyData()) {

			if (xyDataIterator instanceof ChartDataYSerie) {
				final ChartDataYSerie yData = (ChartDataYSerie) xyDataIterator;
				final Integer graphId = (Integer) yData.getCustomData(ChartDataYSerie.YDATA_INFO);
				enabledGraphIds.add(graphId);
			}
		}

		TCActionProxy proxy;

		for (final int graphId : allGraphIds) {

			proxy = _actionProxies.get(getProxyId(graphId));
			proxy.setChecked(visibleGraphIds.contains(graphId));
			proxy.setEnabled(enabledGraphIds.contains(graphId));
		}

		/*
		 * HR zones
		 */
		final boolean canShowHrZones = _tourChartConfig.canShowHrZones;
		final String currentHrZoneStyle = _tourChartConfig.hrZoneStyle;

		proxy = _actionProxies.get(COMMAND_ID_HR_ZONE_DROPDOWN_MENU);
		proxy.setEnabled(canShowHrZones);

		proxy = _actionProxies.get(COMMAND_ID_HR_ZONE_STYLE_GRAPH_TOP);
		proxy.setEnabled(true);
		proxy.setChecked(currentHrZoneStyle.equals(COMMAND_ID_HR_ZONE_STYLE_GRAPH_TOP));

		proxy = _actionProxies.get(COMMAND_ID_HR_ZONE_STYLE_NO_GRADIENT);
		proxy.setEnabled(true);
		proxy.setChecked(currentHrZoneStyle.equals(COMMAND_ID_HR_ZONE_STYLE_NO_GRADIENT));

		proxy = _actionProxies.get(COMMAND_ID_HR_ZONE_STYLE_WHITE_TOP);
		proxy.setEnabled(true);
		proxy.setChecked(currentHrZoneStyle.equals(COMMAND_ID_HR_ZONE_STYLE_WHITE_TOP));

		proxy = _actionProxies.get(COMMAND_ID_HR_ZONE_STYLE_WHITE_BOTTOM);
		proxy.setEnabled(true);
		proxy.setChecked(currentHrZoneStyle.equals(COMMAND_ID_HR_ZONE_STYLE_WHITE_BOTTOM));

		/*
		 * Tour marker
		 */
		proxy = _actionProxies.get(COMMAND_ID_IS_SHOW_TOUR_MARKER);
		proxy.setEnabled(true);
		proxy.setChecked(_tourChartConfig.isShowTourMarker);

		/*
		 * Tour photos
		 */
		proxy = _actionProxies.get(COMMAND_ID_IS_SHOW_TOUR_PHOTOS);
		proxy.setEnabled(true);
		proxy.setChecked(_tourChartConfig.isShowTourPhotos);

		/*
		 * Breaktime values
		 */
		proxy = _actionProxies.get(COMMAND_ID_IS_SHOW_BREAKTIME_VALUES);
		proxy.setEnabled(true);
		proxy.setChecked(_tourChartConfig.isShowBreaktimeValues);

		/*
		 * Value point tool tip
		 */
		proxy = _actionProxies.get(COMMAND_ID_IS_SHOW_VALUEPOINT_TOOLTIP);
		proxy.setEnabled(true);
		final boolean isVisible = _valuePointToolTip.isVisible();
		proxy.setChecked(isVisible);

		/*
		 * SRTM data
		 */
		proxy = _actionProxies.get(COMMAND_ID_IS_SHOW_SRTM_DATA);
		final boolean canShowSRTMData = _tourChartConfig.canShowSRTMData;
		proxy.setEnabled(canShowSRTMData);
		proxy.setChecked(canShowSRTMData ? _tourChartConfig.isSRTMDataVisible : false);

		/*
		 * x-axis time/distance
		 */
		proxy = _actionProxies.get(COMMAND_ID_IS_SHOW_START_TIME);
		proxy.setEnabled(_tourChartConfig.isShowTimeOnXAxis);
		proxy.setChecked(_tourChartConfig.isShowStartTime);

		proxy = _actionProxies.get(COMMAND_ID_X_AXIS_TIME);
		proxy.setEnabled(true); // time data are always available
		proxy.setChecked(_tourChartConfig.isShowTimeOnXAxis);

		proxy = _actionProxies.get(COMMAND_ID_X_AXIS_DISTANCE);
		proxy.setChecked(!_tourChartConfig.isShowTimeOnXAxis);
		proxy.setEnabled(!_tourChartConfig.isForceTimeOnXAxis);

		// get options check status from the configuration
		final boolean isMoveSlidersWhenZoomed = _tourChartConfig.moveSlidersWhenZoomed;
		final boolean isAutoZoomToSlider = _tourChartConfig.autoZoomToSlider;
		final boolean canAutoZoom = getMouseMode().equals(Chart.MOUSE_MODE_ZOOM);

		// update tour chart actions
		proxy = _actionProxies.get(COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER);
		proxy.setEnabled(true);
		proxy.setChecked(isAutoZoomToSlider);

		proxy = _actionProxies.get(COMMAND_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED);
		proxy.setEnabled(canAutoZoom);
		proxy.setChecked(isMoveSlidersWhenZoomed);

		// update the chart actions
		setCanAutoMoveSliders(isMoveSlidersWhenZoomed);
		setCanAutoZoomToSlider(isAutoZoomToSlider);

		proxy = _actionProxies.get(COMMAND_ID_EDIT_CHART_PREFERENCES);
		proxy.setEnabled(true);

		// update UI state for the action handlers
		if (useActionHandlers()) {
			_tcActionHandlerManager.updateUIState();
		}
	}

	private void enableZoomOptions() {

		if (_actionProxies == null) {
			return;
		}

		final boolean canAutoZoom = getMouseMode().equals(Chart.MOUSE_MODE_ZOOM);

		final TCActionProxy actionProxy = _actionProxies.get(COMMAND_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED);
		if (actionProxy != null) {
			actionProxy.setEnabled(canAutoZoom);
		}
	}

	/**
	 * create the tour specific action bar, they are defined in the chart configuration
	 */
	private void fillToolbar() {

		// check if toolbar is created
		if (_actionOptions != null) {
			return;
		}

		if (useInternalActionBar() == false) {
			return;
		}

		final IToolBarManager tbm = getToolBarManager();

		_actionOptions = new ActionChartOptions(this);

		/*
		 * add the actions to the toolbar
		 */
		if (_tourChartConfig.canShowTourCompareGraph) {
			final TCActionProxy actionProxy = _actionProxies.get(getProxyId(TourManager.GRAPH_TOUR_COMPARE));
			tbm.add(actionProxy.getAction());
		}

		tbm.add(new Separator());
		tbm.add(_actionProxies.get(getProxyId(TourManager.GRAPH_ALTITUDE)).getAction());
		tbm.add(_actionProxies.get(getProxyId(TourManager.GRAPH_PULSE)).getAction());
		tbm.add(_actionProxies.get(getProxyId(TourManager.GRAPH_SPEED)).getAction());
		tbm.add(_actionProxies.get(getProxyId(TourManager.GRAPH_PACE)).getAction());
		tbm.add(_actionProxies.get(getProxyId(TourManager.GRAPH_POWER)).getAction());
		tbm.add(_actionProxies.get(getProxyId(TourManager.GRAPH_TEMPERATURE)).getAction());
		tbm.add(_actionProxies.get(getProxyId(TourManager.GRAPH_GRADIENT)).getAction());
		tbm.add(_actionProxies.get(getProxyId(TourManager.GRAPH_ALTIMETER)).getAction());
		tbm.add(_actionProxies.get(getProxyId(TourManager.GRAPH_CADENCE)).getAction());

		tbm.add(_actionProxies.get(COMMAND_ID_HR_ZONE_DROPDOWN_MENU).getAction());
		tbm.add(_actionProxies.get(COMMAND_ID_IS_SHOW_TOUR_PHOTOS).getAction());

		tbm.add(new Separator());
		tbm.add(_actionProxies.get(COMMAND_ID_X_AXIS_TIME).getAction());
		tbm.add(_actionProxies.get(COMMAND_ID_X_AXIS_DISTANCE).getAction());

//		tbm.add(new Separator());
		tbm.add(_actionOptions);

		tbm.update(true);
	}

	/**
	 * fire a selection event for this tour chart
	 */
	private void fireTourChartSelection() {
		final Object[] listeners = _selectionListeners.getListeners();
		for (final Object listener2 : listeners) {
			final ITourChartSelectionListener listener = (ITourChartSelectionListener) listener2;
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					listener.selectedTourChart(new SelectionTourChart(TourChart.this));
				}
			});
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

	public Map<String, TCActionProxy> getActionProxies() {
		return _actionProxies;
	}

	ChartLayerPhoto getPhotoLayer() {
		return _layerPhoto;
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
		return _tourChartConfig;
	}

	public TourData getTourData() {
		return _tourData;
	}

	private void onDispose() {

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		_photoOverlayBGColor.dispose();

		_valuePointToolTip.hide();
	}

	public void partIsDeactivated() {

		// hide photo tooltip
		_photoTooltip.hide();
	}

	public void partIsHidden() {

		// hide value point tooltip
		_valuePointToolTip.setShellVisible(false);

		// hide photo tooltip
		_photoTooltip.hide();
	}

	public void partIsVisible() {

		// show tool tip again
		_valuePointToolTip.setShellVisible(true);
	}

	public void removeTourChartListener(final ITourChartSelectionListener listener) {
		_selectionListeners.remove(listener);
	}

	public void removeXAxisSelectionListener(final IXAxisSelectionListener listener) {
		_xAxisSelectionListener.remove(listener);
	}

	void restoreState() {

		_photoTooltip.restoreState(_state);
	}

	void saveState() {

		_valuePointToolTip.saveState();

		_photoTooltip.saveState(_state);
	}

	/**
	 * Set the check state for a command and update the UI
	 * 
	 * @param commandId
	 * @param isItemChecked
	 */
	public void setCommandChecked(final String commandId, final Boolean isItemChecked) {

		_actionProxies.get(commandId).setChecked(isItemChecked);
		_tcActionHandlerManager.updateUICheckState(commandId);
	}

	/**
	 * Set the enable state for a command and update the UI
	 */
	public void setCommandEnabled(final String commandId, final boolean isEnabled) {

		final TCActionProxy actionProxy = _actionProxies.get(commandId);

		if (actionProxy != null) {
			actionProxy.setEnabled(isEnabled);
			final TCActionHandler actionHandler = _tcActionHandlerManager.getActionHandler(commandId);
			if (actionHandler != null) {
				actionHandler.fireHandlerChanged();
			}
		}
	}

	/**
	 * Set the enable/check state for a command and update the UI
	 */
	public void setCommandState(final String commandId, final boolean isEnabled, final boolean isChecked) {

		final TCActionProxy actionProxy = _actionProxies.get(commandId);

		actionProxy.setEnabled(isEnabled);
		final TCActionHandler actionHandler = _tcActionHandlerManager.getActionHandler(commandId);
		if (actionHandler != null) {
			actionHandler.fireHandlerChanged();
		}

		actionProxy.setChecked(isChecked);
		_tcActionHandlerManager.updateUICheckState(commandId);
	}

	/**
	 * set custom data for all graphs
	 */
	private void setGraphData() {

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

		setGraphDataLayers(TourManager.CUSTOM_DATA_ALTITUDE, _tourData.segmentSerieAltitudeDiff, yDataWithLabels);
		setGraphDataLayers(TourManager.CUSTOM_DATA_PULSE, _tourData.segmentSeriePulse, yDataWithLabels);
		setGraphDataLayers(TourManager.CUSTOM_DATA_SPEED, _tourData.segmentSerieSpeed, yDataWithLabels);
		setGraphDataLayers(TourManager.CUSTOM_DATA_PACE, _tourData.segmentSeriePace, yDataWithLabels);
		setGraphDataLayers(TourManager.CUSTOM_DATA_POWER, _tourData.segmentSeriePower, yDataWithLabels);
		setGraphDataLayers(TourManager.CUSTOM_DATA_GRADIENT, _tourData.segmentSerieGradient, yDataWithLabels);
		setGraphDataLayers(TourManager.CUSTOM_DATA_ALTIMETER, _tourData.segmentSerieAltitudeUpH, yDataWithLabels);
		setGraphDataLayers(TourManager.CUSTOM_DATA_TEMPERATURE, null, yDataWithLabels);
		setGraphDataLayers(TourManager.CUSTOM_DATA_CADENCE, null, yDataWithLabels);

		setGraphDataLayers(TourManager.CUSTOM_DATA_HISTORY, null, null);
	}

	/**
	 * Set data for each graph
	 * 
	 * @param customDataKey
	 * @param segmentDataSerie
	 * @param yDataWithLabels
	 */
	private void setGraphDataLayers(final String customDataKey,
									final Object segmentDataSerie,
									final ChartDataYSerie yDataWithLabels) {

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
				&& _tourChartConfig.isShowTourPhotos == true
				&& (yData == yDataWithLabels || dataModel.getChartType() == ChartDataModel.CHART_TYPE_HISTORY)) {

			customFgLayers.add(_layerPhoto);
		}

		/*
		 * segment layer
		 */
		final ChartDataYSerie yDataAltitude = (ChartDataYSerie) dataModel
				.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE);
		if (yData == yDataAltitude) {
			if (_layerSegment != null) {
				customFgLayers.add(_layerSegment);
			}
		} else {
			if (_layerSegmentValue != null) {
				customFgLayers.add(_layerSegmentValue);
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
		if (segmentDataSerie != null) {
			yData.setCustomData(TourManager.CUSTOM_DATA_SEGMENT_VALUES, segmentDataSerie);
		}
	}

	private boolean setMaxDefaultValue(	final String property,
										boolean isChartModified,
										final String tagIsMaxEnabled,
										final String tabMaxValue,
										final int yDataInfoId,
										final int valueDivisor) {

		if (property.equals(tagIsMaxEnabled) || property.equals(tabMaxValue)) {

			final boolean isMaxEnabled = _prefStore.getBoolean(tagIsMaxEnabled);

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
					final int maxValue = _prefStore.getInt(tabMaxValue);
					yData.setVisibleMaxValue(valueDivisor == 0 ? maxValue : maxValue * valueDivisor);

				} else {
					// reset visible max value to the original min value
					yData.setVisibleMaxValue(yData.getOriginalMinValue());
				}

				isChartModified = true;
			}
		}

		return isChartModified;
	}

	private boolean setMinDefaultValue(	final String property,
										boolean isChartModified,
										final String tagIsMinEnabled,
										final String tabMinValue,
										final int yDataInfoId,
										final int valueDivisor) {

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
					final int minValue = _prefStore.getInt(tabMinValue);
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

		if (_tourInfoToolTipProvider != null) {
			_tourInfoToolTipProvider.setActionsEnabled(isEnabled);
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

		final Map<String, TCActionProxy> actionProxies = synchedChart._actionProxies;

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
			_tourChartConfig = null;

			_valuePointToolTip.setTourData(null);

			if (_actionProxies != null) {

				for (final TCActionProxy actionProxy : _actionProxies.values()) {
					actionProxy.setEnabled(false);
				}

				// update UI state for the action handlers
				if (useActionHandlers()) {
					_tcActionHandlerManager.updateUIState();
				}
			}
		}
	}

	public void updateLayer2ndAlti(final I2ndAltiLayer alti2ndLayerProvider, final boolean isLayerVisible) {

		_is2ndAltiLayerVisible = isLayerVisible;
		_layer2ndAlti = alti2ndLayerProvider;

		createLayer_2ndAlti();

		setGraphData();
		updateCustomLayers();
	}

	/**
	 * Updates the marker layer in the chart
	 * 
	 * @param isLayerVisible
	 */
	public void updateLayerMarker(final boolean isLayerVisible) {

		if (isLayerVisible) {
			createLayer_Marker(true);
		} else {
			_layerMarker = null;
		}

		setGraphData();
		updateCustomLayers();
	}

	/**
	 * Updates the segment layer
	 */
	public void updateLayerSegment(final boolean isLayerVisible) {

		if (_tourData == null) {
			return;
		}

		_isSegmentLayerVisible = isLayerVisible;

		if (isLayerVisible) {
			createLayer_Segment();
		} else {
			_layerSegment = null;
			_layerSegmentValue = null;
			resetGraphAlpha();
		}

		setGraphData();
		updateCustomLayers();

		/*
		 * the chart needs to be redrawn because the alpha for filling the chart was modified
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
		updateTourChartInternal(_tourData, _tourChartConfig, keepMinMaxValues, false);
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
		updateTourChartInternal(_tourData, _tourChartConfig, keepMinMaxValues, isPropertyChanged);
	}

	public void updateTourChart(final TourData tourData, final boolean keepMinMaxValues) {
		updateTourChartInternal(tourData, _tourChartConfig, keepMinMaxValues, false);

	}

	/**
	 * Set {@link TourData} and {@link TourChartConfiguration} to create a new chart data model
	 * 
	 * @param tourData
	 * @param tourChartSettings
	 * @param keepMinMaxValues
	 *            <code>true</code> keeps the min/max values from the previous chart
	 */
	public void updateTourChart(final TourData tourData,
								final TourChartConfiguration tourChartSettings,
								final boolean keepMinMaxValues) {

		updateTourChartInternal(tourData, tourChartSettings, keepMinMaxValues, false);
	}

	/**
	 * This method is synchronized because when SRTM data are retrieved and the import view is
	 * openened, the error occured that the chart config was deleted with {@link #updateChart(null)}
	 * 
	 * @param newTourData
	 * @param newChartConfig
	 * @param keepMinMaxValues
	 * @param isPropertyChanged
	 */
	private synchronized void updateTourChartInternal(	final TourData newTourData,
														final TourChartConfiguration newChartConfig,
														final boolean keepMinMaxValues,
														final boolean isPropertyChanged) {

		if ((newTourData == null) || (newChartConfig == null)) {

			_valuePointToolTip.setTourData(null);

			return;
		}

		// keep min/max values for the 'old' chart in the chart config
		if (_tourChartConfig != null) {
			final ChartYDataMinMaxKeeper oldMinMaxKeeper = _tourChartConfig.getMinMaxKeeper();
			if ((oldMinMaxKeeper != null) && keepMinMaxValues) {
				oldMinMaxKeeper.saveMinMaxValues(getChartDataModel());
			}
		}

		// set current tour data and chart config to new values
		_tourData = newTourData;
		_tourChartConfig = newChartConfig;

		final ChartDataModel newChartDataModel = TourManager.getInstance().createChartDataModel(
				_tourData,
				_tourChartConfig,
				isPropertyChanged);

		// set the model BEFORE actions are created/enabled/checked
		setDataModel(newChartDataModel);

		if (_isShowActions) {
			createAction10TourActionProxies();
			fillToolbar();
			enableTourActions();
		}

		// restore min/max values from the chart config
		final ChartYDataMinMaxKeeper newMinMaxKeeper = _tourChartConfig.getMinMaxKeeper();
		final boolean isMinMaxKeeper = (newMinMaxKeeper != null) && keepMinMaxValues;
		if (isMinMaxKeeper) {
			newMinMaxKeeper.setMinMaxValues(newChartDataModel);
		}

		if (_chartDataModelListener != null) {
			_chartDataModelListener.dataModelChanged(newChartDataModel);
		}

		createLayer_Segment();
		createLayer_Marker(false);
		createLayer_2ndAlti();
		createLayer_Photo();

		createPainter_HrZone();

		setGraphData();

		updateChart(newChartDataModel, !isMinMaxKeeper);

		/*
		 * this must be done after the chart is created because is sets an action, set it only once
		 * when the chart is displayed the first time otherwise it's annoying
		 */
		if (_isMouseModeSet == false) {

			_isMouseModeSet = true;

			setMouseMode(_prefStore.getString(ITourbookPreferences.GRAPH_MOUSE_MODE).equals(Chart.MOUSE_MODE_SLIDER));
		}

		_tourInfoToolTipProvider.setTourData(_tourData);

		_valuePointToolTip.setTourData(_tourData);
	}

	/**
	 * Update UI check state, the chart decides if the scroll/auto zoom options are available
	 */
	void updateZoomOptionActionHandlers() {

		setCommandChecked(TourChart.COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER, getCanAutoZoomToSlider());
		setCommandChecked(TourChart.COMMAND_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED, getCanAutoMoveSliders());
	}

	/**
	 * Enable/disable the zoom options in the tour chart
	 * 
	 * @param isEnabled
	 */
	private void updateZoomOptions(final boolean isEnabled) {
		_actionProxies.get(COMMAND_ID_CAN_AUTO_ZOOM_TO_SLIDER).setEnabled(isEnabled);
		_actionProxies.get(COMMAND_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED).setEnabled(isEnabled);
	}

}
