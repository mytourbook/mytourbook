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

package net.tourbook.mapping;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartUtil;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.ViewPart;

import de.byteholder.geoclipse.GeoclipseExtensions;
import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.geoclipse.map.TileFactoryInfo;
import de.byteholder.geoclipse.swt.Map;
import de.byteholder.geoclipse.swt.MapLegend;
import de.byteholder.gpx.GeoPosition;
import de.byteholder.gpx.ext.PointOfInterest;

/**
 * @author Wolfgang Schramm
 * @since 1.3.0
 */
public class MappingView extends ViewPart {

//	private static final String					EMPTY_STRING						= "";											//$NON-NLS-1$
	public static final int						TOUR_COLOR_DEFAULT					= 0;
	public static final int						TOUR_COLOR_ALTITUDE					= 10;
	public static final int						TOUR_COLOR_GRADIENT					= 20;
	public static final int						TOUR_COLOR_PULSE					= 30;
	public static final int						TOUR_COLOR_SPEED					= 40;
	public static final int						TOUR_COLOR_PACE						= 50;

	final public static String					ID									= "net.tourbook.mapping.mappingViewID";		//$NON-NLS-1$

	private static final String					MEMENTO_ZOOM_CENTERED				= "mapping.view.zoom-centered";				//$NON-NLS-1$
	private static final String					MEMENTO_SHOW_TOUR_IN_MAP			= "mapping.view.show-tour-in-map";				//$NON-NLS-1$
	private static final String					MEMENTO_SYNCH_WITH_SELECTED_TOUR	= "mapping.view.synch-with-selected-tour";		//$NON-NLS-1$
	private static final String					MEMENTO_SYNCH_TOUR_ZOOM_LEVEL		= "mapping.view.synch-tour-zoom-level";		//$NON-NLS-1$
	private static final String					MEMENTO_CURRENT_FACTORY_ID			= "mapping.view.current.factory-id";			//$NON-NLS-1$

	private static final String					MEMENTO_DEFAULT_POSITION_ZOOM		= "mapping.view.default.position.zoom-level";	//$NON-NLS-1$
	private static final String					MEMENTO_DEFAULT_POSITION_LATITUDE	= "mapping.view.default.position.latitude";	//$NON-NLS-1$
	private static final String					MEMENTO_DEFAULT_POSITION_LONGITUDE	= "mapping.view.default.position.longitude";	//$NON-NLS-1$

	private static final String					MEMENTO_TOUR_COLOR_ID				= "mapping.view.tour-color-id";				//$NON-NLS-1$

	final static String							SHOW_TILE_INFO						= "show.tile-info";							//$NON-NLS-1$

	public static final int						LEGEND_MARGIN						= 20;
	private static final int					LEGEND_UNIT_DISTANCE				= 60;

	private static IMemento						fSessionMemento;

	private Map									fMap;

	private ISelectionListener					fPostSelectionListener;
	private IPropertyChangeListener				fPrefChangeListener;
	private IPropertyChangeListener				fTourbookPrefChangeListener;
	private IPartListener2						fPartListener;
	private ITourPropertyListener				fTourPropertyListener;

	private TourData							fTourData;
	private TourData							fPreviousTourData;

	private ActionTourColor						fActionTourColorAltitude;
	private ActionTourColor						fActionTourColorGradient;
	private ActionTourColor						fActionTourColorPulse;
	private ActionTourColor						fActionTourColorSpeed;
	private ActionTourColor						fActionTourColorPace;
	private ActionZoomIn						fActionZoomIn;
	private ActionZoomOut						fActionZoomOut;
	private ActionZoomCentered					fActionZoomCentered;
	private ActionZoomShowAll					fActionZoomShowAll;
	private ActionZoomShowEntireTour			fActionZoomShowEntireTour;
	private ActionShowTourInMap					fActionShowTourInMap;
	private ActionSynchWithTour					fActionSynchWithTour;
	private ActionSynchTourZoomLevel			fActionSynchTourZoomLevel;
	private ActionChangeTileFactory				fActionChangeTileFactory;
	private ActionSaveDefaultPosition			fActionSaveDefaultPosition;
	private ActionSetDefaultPosition			fActionSetDefaultPosition;

	private boolean								fIsMapSynchedWithTour;
	private boolean								fIsPositionCentered;

	private List<TileFactory>					fTileFactories;

	private int									fDefaultZoom;
	private GeoPosition							fDefaultPosition					= null;

	private boolean								fIsTour;

	/**
	 * Position for the current point of interest
	 */
	private GeoPosition							fPOIPosition;

	private final DirectMappingPainter			fDirectMappingPainter				= new DirectMappingPainter();

	/**
	 * current position for the x-slider
	 */
	private int									fCurrentLeftSliderValueIndex;
	private int									fCurrentRightSliderValueIndex;

	private MapLegend							fMapLegend;
	private HashMap<Integer, ILegendProvider>	fLegendProviders					= new HashMap<Integer, ILegendProvider>();

	public MappingView() {}

	private static List<Integer> getLegendUnits(final Rectangle legendBounds, int graphMinValue, int graphMaxValue) {

		final int legendHeight = legendBounds.height - LEGEND_MARGIN;

		/*
		 * !!! value range does currently NOT provide negative altitudes
		 */
		final int graphRange = graphMaxValue - graphMinValue;

		final int unitCount = legendHeight / LEGEND_UNIT_DISTANCE;

		// get altitude range for one unit
		final int graphUnitValue = graphRange / Math.max(1, unitCount);

		// round the unit
		final float graphUnit = ChartUtil.roundDecimalValue(graphUnitValue);

		/*
		 * adjust min value
		 */
		float adjustMinValue = 0;
		if (((float) graphMinValue % graphUnit) != 0 && graphMinValue < 0) {
			adjustMinValue = graphUnit;
		}
		graphMinValue = (int) ((int) ((graphMinValue - adjustMinValue) / graphUnit) * graphUnit);

		/*
		 * adjust max value
		 */
		// increase the max value when it does not fit to unit borders
		float adjustMaxValue = 0;
		if (((float) graphMaxValue % graphUnit) != 0) {
			adjustMaxValue = graphUnit;
		}
		graphMaxValue = (int) ((int) ((graphMaxValue + adjustMaxValue) / graphUnit) * graphUnit);

		/*
		 * create a list with all units
		 */
		final ArrayList<Integer> unitList = new ArrayList<Integer>();

		int graphValue = graphMinValue;
		int unitCounter = 0;

		// loop: create unit label for all units
		while (graphValue <= graphMaxValue) {

			unitList.add(graphValue);

			// prevent endless loops 
			if (graphValue >= graphMaxValue || unitCounter > 100) {
				break;
			}

			graphValue += graphUnit;
			unitCounter++;
		}

		return unitList;
	}

	void actionSaveDefaultPosition() {
		fDefaultZoom = fMap.getZoom();
		fDefaultPosition = fMap.getCenterPosition();
	}

	void actionSetDefaultPosition() {
		if (fDefaultPosition == null) {
			fMap.setZoom(fMap.getTileFactory().getInfo().getMinimumZoomLevel());
			fMap.setCenterPosition(new GeoPosition(0, 0));
		} else {
			fMap.setZoom(fDefaultZoom);
			fMap.setCenterPosition(fDefaultPosition);
		}
		fMap.queueRedrawMap();
	}

	void actionSetShowTourInMap() {

		// show/hide legend
		fMap.setShowLegend(fActionShowTourInMap.isChecked());

		paintTour(fTourData, true, true);
	}

	void actionSetTourColor(final int colorId) {
		PaintManager.getInstance().setLegendProvider(getLegendProvider(colorId));
		fMap.resetOverlayImageCache();
		fMap.queueRedrawMap();
		createLegendImage(getLegendProvider(colorId));
	}

	void actionSetZoomCentered() {
		fIsPositionCentered = fActionZoomCentered.isChecked();
	}

	void actionSynchWithTour() {

		fIsMapSynchedWithTour = fActionSynchWithTour.isChecked();

		if (fIsMapSynchedWithTour) {

			fActionShowTourInMap.setChecked(true);
			fMap.setShowOverlays(true);

			paintTour(fTourData, true, false);
		}
	}

	void actionZoomIn() {
		fMap.setZoom(fMap.getZoom() + 1);
		centerTour();
		fMap.queueRedrawMap();
	}

	void actionZoomOut() {
		fMap.setZoom(fMap.getZoom() - 1);
		centerTour();
		fMap.queueRedrawMap();
	}

	void actionZoomShowEntireMap() {
		fMap.setZoom(fMap.getTileFactory().getInfo().getMinimumZoomLevel());
		fMap.queueRedrawMap();
	}

	void actionZoomShowEntireTour() {

		fActionShowTourInMap.setChecked(true);
		fMap.setShowOverlays(true);

		paintEntireTour();
	}

	private void addPartListener() {

		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(SHOW_TILE_INFO)) {

					// map properties has changed

					final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();
					final boolean isShowTileInfo = store.getBoolean(SHOW_TILE_INFO);

					fMap.setDrawTileBorders(isShowTileInfo);
					fMap.queueRedrawMap();
				}
			}
		};
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				onChangeSelection(selection);
			}
		};
		getViewSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourbookPrefListener() {

		fTourbookPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					createLegendImage(PaintManager.getInstance().getLegendProvider());

					fMap.queueRedrawMap();
				}
			}
		};

		// register the listener
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fTourbookPrefChangeListener);
	}

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(final int propertyId, final Object propertyData) {

				if (propertyId == TourManager.TOUR_PROPERTY_CHART_IS_MODIFIED) {
					fTourData.clearComputedSeries();
					paintTour(fTourData, true, true);
					fMap.resetOverlayImageCache();
					fMap.queueRedrawMap();
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	/**
	 * Center the tour in the map when action is enabled
	 */
	private void centerTour() {

		if (fIsPositionCentered) {

			final int zoom = fMap.getZoom();

			Set<GeoPosition> positionBounds = null;
			if (fIsTour) {
				positionBounds = PaintManager.getInstance().getTourBounds();
				if (positionBounds == null) {
					return;
				}
			} else {
				if (fPOIPosition == null) {
					return;
				}
				positionBounds = new HashSet<GeoPosition>();
				positionBounds.add(fPOIPosition);
			}

			final Rectangle2D positionRect = getPositionRect(positionBounds, zoom);
			final Point2D center = new Point2D.Double(positionRect.getX() + positionRect.getWidth() / 2,
					positionRect.getY() + positionRect.getHeight() / 2);
			final GeoPosition geoPosition = fMap.getTileFactory().pixelToGeo(center, zoom);

			fMap.setCenterPosition(geoPosition);
		}
	}

	private void createActions() {

		fActionTourColorAltitude = new ActionTourColor(this,
				TOUR_COLOR_ALTITUDE,
				Messages.map_action_tour_color_altitude_tooltip,
				Messages.image_action_tour_color_altitude,
				Messages.image_action_tour_color_altitude_disabled);

		fActionTourColorGradient = new ActionTourColor(this,
				TOUR_COLOR_GRADIENT,
				Messages.map_action_tour_color_gradient_tooltip,
				Messages.image_action_tour_color_gradient,
				Messages.image_action_tour_color_gradient_disabled);

		fActionTourColorPulse = new ActionTourColor(this,
				TOUR_COLOR_PULSE,
				Messages.map_action_tour_color_pulse_tooltip,
				Messages.image_action_tour_color_pulse,
				Messages.image_action_tour_color_pulse_disabled);

		fActionTourColorSpeed = new ActionTourColor(this,
				TOUR_COLOR_SPEED,
				Messages.map_action_tour_color_speed_tooltip,
				Messages.image_action_tour_color_speed,
				Messages.image_action_tour_color_speed_disabled);

		fActionTourColorPace = new ActionTourColor(this,
				TOUR_COLOR_PACE,
				Messages.map_action_tour_color_pase_tooltip,
				Messages.image_action_tour_color_pace,
				Messages.image_action_tour_color_pace_disabled);

		fActionZoomIn = new ActionZoomIn(this);
		fActionZoomOut = new ActionZoomOut(this);
		fActionZoomCentered = new ActionZoomCentered(this);
		fActionZoomShowAll = new ActionZoomShowAll(this);
		fActionZoomShowEntireTour = new ActionZoomShowEntireTour(this);
		fActionSynchWithTour = new ActionSynchWithTour(this);
		fActionShowTourInMap = new ActionShowTourInMap(this);
		fActionSynchTourZoomLevel = new ActionSynchTourZoomLevel(this);
		fActionChangeTileFactory = new ActionChangeTileFactory(this);
		fActionSetDefaultPosition = new ActionSetDefaultPosition(this);
		fActionSaveDefaultPosition = new ActionSaveDefaultPosition(this);

		/*
		 * fill view toolbar
		 */
		final IToolBarManager viewTbm = getViewSite().getActionBars().getToolBarManager();

		viewTbm.add(fActionTourColorAltitude);
		viewTbm.add(fActionTourColorPulse);
		viewTbm.add(fActionTourColorSpeed);
		viewTbm.add(fActionTourColorPace);
		viewTbm.add(fActionTourColorGradient);
		viewTbm.add(new Separator());
		viewTbm.add(fActionShowTourInMap);
		viewTbm.add(fActionZoomShowEntireTour);
		viewTbm.add(fActionSynchWithTour);
		viewTbm.add(new Separator());
		viewTbm.add(fActionZoomCentered);
		viewTbm.add(fActionZoomIn);
		viewTbm.add(fActionZoomOut);
		viewTbm.add(new Separator());
		viewTbm.add(fActionChangeTileFactory);
		viewTbm.add(fActionZoomShowAll);

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(fActionSetDefaultPosition);
		menuMgr.add(fActionSaveDefaultPosition);
		menuMgr.add(new Separator());
		menuMgr.add(fActionSynchTourZoomLevel);

	}

	private void createLegendProviders() {

		LegendConfig legendConfig;
		LegendColor legendColor;

		/*
		 * legend provider: heartbeat
		 */

		legendConfig = new LegendConfig();
		legendConfig.units = Arrays.asList(50, 75, 100, 125, 150, 175, 200);
		legendConfig.legendMinValue = 50;
		legendConfig.legendMaxValue = 200;
		legendConfig.unitText = Messages.graph_label_heartbeat_unit;

		legendColor = new LegendColor();
		legendColor.minValue = 50;
		legendColor.lowValue = 70;
		legendColor.midValue = 125;
		legendColor.highValue = 150;
		legendColor.maxValue = 200;

		legendColor.dimmFactor = 0.5F;

		legendColor.maxColor1 = 255;
		legendColor.maxColor2 = 220;
		legendColor.maxColor3 = 0;
		legendColor.color1 = LegendColor.COLOR_RED;
		legendColor.color2 = LegendColor.COLOR_GREEN;
		legendColor.color3 = LegendColor.COLOR_BLUE;

		fLegendProviders.put(MappingView.TOUR_COLOR_PULSE, //
				new LegendProvider(legendConfig, legendColor, MappingView.TOUR_COLOR_PULSE));

		/*
		 * legend provider: gradient
		 */

		legendConfig = new LegendConfig();
		legendConfig.units = Arrays.asList(-200, -100, 0, 100, 200);
		legendConfig.legendMinValue = -250;
		legendConfig.legendMaxValue = 250;
		legendConfig.unitFactor = 10;
		legendConfig.unitText = Messages.graph_label_gradiend_unit;

		legendColor = new LegendColor();
		legendColor.minValue = -300;
		legendColor.lowValue = -100;
		legendColor.midValue = 0;
		legendColor.highValue = 100;
		legendColor.maxValue = 300;

		legendColor.dimmFactor = 0.5F;

		legendColor.maxColor1 = 255;
		legendColor.maxColor2 = 255;
		legendColor.maxColor3 = 255;
		legendColor.color1 = LegendColor.COLOR_RED;
		legendColor.color2 = LegendColor.COLOR_GREEN;
		legendColor.color3 = LegendColor.COLOR_BLUE;

		fLegendProviders.put(MappingView.TOUR_COLOR_GRADIENT, //
				new LegendProvider(legendConfig, legendColor, MappingView.TOUR_COLOR_GRADIENT));

		/*
		 * legend provider: altitude
		 */
		legendConfig = new LegendConfig();

		// altitude legend color, min/max values will be set when a new tour is displayed
		legendColor = new LegendColor();

		legendColor.dimmFactor = 0.5F;

		legendColor.maxColor1 = 255;
		legendColor.maxColor2 = 200;
		legendColor.maxColor3 = 0;
		legendColor.color1 = LegendColor.COLOR_BLUE;
		legendColor.color2 = LegendColor.COLOR_GREEN;
		legendColor.color3 = LegendColor.COLOR_RED;

		fLegendProviders.put(MappingView.TOUR_COLOR_ALTITUDE, //
				new LegendProvider(legendConfig, legendColor, MappingView.TOUR_COLOR_ALTITUDE));

		/*
		 * legend provider: speed
		 */

		legendConfig = new LegendConfig();
		legendConfig.units = Arrays.asList(0, 10, 20, 30, 40, 50, 60, 70, 80);
		legendConfig.legendMinValue = 0;
		legendConfig.legendMaxValue = 60;
		legendConfig.unitText = UI.UNIT_LABEL_SPEED;

		legendColor = new LegendColor();

		fLegendProviders.put(MappingView.TOUR_COLOR_SPEED, //
				new LegendProvider(legendConfig, legendColor, MappingView.TOUR_COLOR_SPEED));

		/*
		 * legend provider: pace
		 */

		legendConfig = new LegendConfig();
		legendConfig.units = Arrays.asList(0, 5, 10, 15, 20);
		legendConfig.legendMinValue = 0;
		legendConfig.legendMaxValue = 20;
		legendConfig.unitText = UI.UNIT_LABEL_PACE;

		legendColor = new LegendColor();

		fLegendProviders.put(MappingView.TOUR_COLOR_PACE, //
				new LegendProvider(legendConfig, legendColor, MappingView.TOUR_COLOR_PACE));

	}

//	private LegendConfig createLegendConfig(final int colorId, final Rectangle legendBounds) {
//
//		LegendConfig legendConfig = new LegendConfig();
//		LegendColor legendColor;
//
//		switch (colorId) {
//
//		case MappingView.TOUR_COLOR_ALTITUDE:
//
////			if (fTourData == null) {
////				break;
////			}
////
////			final int[] altitudeSerie = fTourData.getAltitudeSerie();
////			if (altitudeSerie == null) {
////				break;
////			}
////
////			/*
////			 * get min/max altitude
////			 */
////			int minValue = 0;
////			int maxValue = 0;
////			for (int valueIndex = 0; valueIndex < altitudeSerie.length; valueIndex++) {
////				if (valueIndex == 0) {
////					minValue = altitudeSerie[valueIndex];
////					maxValue = altitudeSerie[valueIndex];
////				} else {
////					minValue = Math.min(minValue, altitudeSerie[valueIndex]);
////					maxValue = Math.max(maxValue, altitudeSerie[valueIndex]);
////				}
////			}
////
////			final List<Integer> legendUnits = getLegendUnits(legendBounds, minValue, maxValue);
////			final Integer legendMinValue = legendUnits.get(0);
////			final Integer legendMaxValue = legendUnits.get(legendUnits.size() - 1);
////
////			legendConfig.units = legendUnits;
////			legendConfig.legendMinValue = legendMinValue;
////			legendConfig.legendMaxValue = legendMaxValue;
////			legendConfig.unitText = UI.UNIT_LABEL_ALTITUDE;
////
////			/*
////			 * set color configuration, each tour has a different altitude config
////			 */
////			legendColor = TourPainter.getInstance().getAltitudeLegendColor();
////
////			final int midValueRelative = (legendMaxValue - legendMinValue) / 2;
////			final int midValueAbsolute = legendMinValue + midValueRelative;
////
////			legendColor.minValue = legendMinValue;
////			legendColor.lowValue = legendMinValue + midValueRelative / 3;
////			legendColor.midValue = midValueAbsolute;
////			legendColor.highValue = legendMaxValue - midValueRelative / 3;
////			legendColor.maxValue = legendMaxValue;
////
////			legendColor.dimmFactor = 0.5F;
////
////			legendColor.maxColor1 = 255;
////			legendColor.maxColor2 = 200;
////			legendColor.maxColor3 = 0;
////			legendColor.color1 = LegendColor.COLOR_BLUE;
////			legendColor.color2 = LegendColor.COLOR_GREEN;
////			legendColor.color3 = LegendColor.COLOR_RED;
//			break;
//
////		case MappingView.TOUR_COLOR_GRADIENT:
////			legendConfig = TourPainter.getInstance().getGradientLegendConfig();
////			break;
//
////		case MappingView.TOUR_COLOR_PULSE:
////
////			legendConfig.units = Arrays.asList(50, 75, 100, 125, 150, 175, 200);
////			legendConfig.legendMinValue = 50;
////			legendConfig.legendMaxValue = 200;
////			legendConfig.unitText = Messages.graph_label_heartbeat_unit;
////
////			legendColor = TourPainter.getInstance().getPulseLegendColor();
////			legendColor.dimmFactor = 0.5F;
////			legendColor.maxColor1 = 255;
////			legendColor.maxColor2 = 200;
////			legendColor.maxColor3 = 0;
////			legendColor.color1 = LegendColor.COLOR_RED;
////			legendColor.color2 = LegendColor.COLOR_GREEN;
////			legendColor.color3 = LegendColor.COLOR_BLUE;
////			break;
//
////		case MappingView.TOUR_COLOR_SPEED:
////
////			legendConfig.units = Arrays.asList(0, 10, 20, 30, 40, 50, 60, 70, 80);
////			legendConfig.legendMinValue = 0;
////			legendConfig.legendMaxValue = 60;
////			legendConfig.unitText = UI.UNIT_LABEL_SPEED;
////			break;
//
////		case MappingView.TOUR_COLOR_PACE:
////
////			legendConfig.units = Arrays.asList(0, 5, 10, 15, 20);
////			legendConfig.legendMinValue = 0;
////			legendConfig.legendMaxValue = 20;
////			legendConfig.unitText = UI.UNIT_LABEL_PACE;
////			break;
//
//		default:
//			break;
//		}
//
//		if (legendConfig.units == null || legendConfig.unitText == null) {
//			// set default values
//			legendConfig.units = Arrays.asList(0);
//			legendConfig.unitText = EMPTY_STRING;
//		}
//
//		return legendConfig;
//	}

	@Override
	public void createPartControl(final Composite parent) {

		fMap = new Map(parent);
		fMap.setDirectPainter(fDirectMappingPainter);

		fMapLegend = new MapLegend();
		fMap.setLegend(fMapLegend);
		fMap.setShowLegend(true);

		fTileFactories = GeoclipseExtensions.getInstance().readExtensions(fMap);

		createActions();
		createLegendProviders();

		addPartListener();
		addPrefListener();
		addSelectionListener();
		addTourPropertyListener();
		addTourbookPrefListener();

		restoreSettings();

		// show map from last selection
		onChangeSelection(getSite().getWorkbenchWindow().getSelectionService().getSelection());
	}

	@Override
	public void dispose() {

		// dispose tilefactory resources
		for (final TileFactory tileFactory : fTileFactories) {
			tileFactory.dispose();
		}

		getViewSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getViewSite().getPage().removePartListener(fPartListener);

		TourManager.getInstance().removePropertyListener(fTourPropertyListener);
		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);
		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fTourbookPrefChangeListener);

		super.dispose();
	}

	/**
	 * Creates a new map legend image and disposes the old image
	 * 
	 * @param legendColorProvider
	 * @param updateLegendValues
	 */
	private void createLegendImage(final ILegendProvider legendColorProvider) {

		// legend requires a tour with coordinates
		if (isPaintDataValid(fTourData) == false) {
			showDefaultMap();
			return;
		}

		Image legendImage = fMapLegend.getImage();

		// dispose old legend image
		if (legendImage != null && !legendImage.isDisposed()) {
			legendImage.dispose();
		}
		final int legendWidth = 150;
		final int legendHeight = 300;

		final RGB rgbTransparent = new RGB(0xfe, 0xfe, 0xfe);

		final ImageData overlayImageData = new ImageData(legendWidth, legendHeight, 24, //
				new PaletteData(0xff, 0xff00, 0xff00000));

		overlayImageData.transparentPixel = overlayImageData.palette.getPixel(rgbTransparent);

		final Display display = Display.getCurrent();
		legendImage = new Image(display, overlayImageData);
		final Rectangle imageBounds = legendImage.getBounds();

		updateLegendValues(legendColorProvider, imageBounds);

		final Color transparentColor = new Color(display, rgbTransparent);
		final GC gc = new GC(legendImage);
		{
			gc.setBackground(transparentColor);
			gc.fillRectangle(imageBounds);

			TourPainter.drawLegendColors(gc, imageBounds, legendColorProvider);
		}
		gc.dispose();
		transparentColor.dispose();

		fMapLegend.setImage(legendImage);
	}

	private void enableActions() {

		fActionZoomShowEntireTour.setEnabled(fIsTour);
		fActionSynchTourZoomLevel.setEnabled(fIsTour);
		fActionShowTourInMap.setEnabled(fIsTour);
		fActionSynchWithTour.setEnabled(fIsTour);

		if (fIsTour && fTourData != null) {
			fActionTourColorAltitude.setEnabled(true);
			fActionTourColorGradient.setEnabled(fTourData.getGradientSerie() != null);
			fActionTourColorPulse.setEnabled(fTourData.pulseSerie != null);
			fActionTourColorSpeed.setEnabled(fTourData.getSpeedSerie() != null);
			fActionTourColorPace.setEnabled(fTourData.getPaceSerie() != null);
		} else {
			fActionTourColorAltitude.setEnabled(false);
			fActionTourColorGradient.setEnabled(false);
			fActionTourColorPulse.setEnabled(false);
			fActionTourColorSpeed.setEnabled(false);
			fActionTourColorPace.setEnabled(false);
		}
	}

	public List<TileFactory> getFactories() {
		return fTileFactories;
	}

	private ILegendProvider getLegendProvider(int colorId) {
		return fLegendProviders.get(colorId);
	}

	public Map getMap() {
		return fMap;
	}

	private Rectangle2D getPositionRect(final Set<GeoPosition> positions, final int zoom) {

		final TileFactory tileFactory = fMap.getTileFactory();
		final Point2D point1 = tileFactory.geoToPixel(positions.iterator().next(), zoom);
		final Rectangle2D rect = new Rectangle2D.Double(point1.getX(), point1.getY(), 0, 0);

		for (final GeoPosition pos : positions) {
			final Point2D point = tileFactory.geoToPixel(pos, zoom);
			rect.add(point);
		}

		return rect;
	}

	private Set<GeoPosition> getTourBounds(final TourData tourData) {

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		/*
		 * get min/max longitude/latitude
		 */
		double minLatitude = latitudeSerie[0];
		double maxLatitude = latitudeSerie[0];
		double minLongitude = longitudeSerie[0];
		double maxLongitude = longitudeSerie[0];

		for (int serieIndex = 0; serieIndex < latitudeSerie.length; serieIndex++) {
			final double latitude = latitudeSerie[serieIndex];
			final double longitude = longitudeSerie[serieIndex];

			minLatitude = Math.min(minLatitude, latitude);
			maxLatitude = Math.max(maxLatitude, latitude);

			minLongitude = Math.min(minLongitude, longitude);
			maxLongitude = Math.max(maxLongitude, longitude);

			if (minLatitude == 0) {
				minLatitude = -180D;
			}
		}

		final Set<GeoPosition> mapPositions = new HashSet<GeoPosition>();
		mapPositions.add(new GeoPosition(minLatitude, minLongitude));
		mapPositions.add(new GeoPosition(maxLatitude, maxLongitude));

		return mapPositions;
	}

	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {

		super.init(site, memento);

		// set session memento
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	/**
	 * Checks if {@link TourData} can be painted
	 * 
	 * @param tourData
	 * @return <code>true</code> when the {@link TourData} can be painted
	 */
	private boolean isPaintDataValid(final TourData tourData) {

		if (tourData == null) {
			return false;
		}

		// check if coordinates are available
		final double[] longitudeSerie = tourData.longitudeSerie;
		final double[] latitudeSerie = tourData.latitudeSerie;
		if (longitudeSerie == null || longitudeSerie.length == 0 || latitudeSerie == null || latitudeSerie.length == 0) {
			return false;
		}

		return true;
	}

	private void onChangeSelection(final ISelection selection) {

		if (selection instanceof SelectionTourData) {

			final SelectionTourData selectionTourData = (SelectionTourData) selection;
			final TourData tourData = selectionTourData.getTourData();

			paintTour(tourData, false, false);

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId tourIdSelection = (SelectionTourId) selection;
			final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

			paintTour(tourData, false, false);

		} else if (selection instanceof SelectionActiveEditor) {

			final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();

			if (editor instanceof TourEditor) {

				final TourEditor fTourEditor = (TourEditor) editor;
				final TourChart fTourChart = fTourEditor.getTourChart();
				final TourData tourData = fTourChart.getTourData();

				paintTour(tourData, false, false);
			}

		} else if (selection instanceof SelectionChartInfo) {

			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
			final TourData tourData = (TourData) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);
			final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

			paintTourSliders(tourData, chartInfo.leftSliderValuesIndex, chartInfo.rightSliderValuesIndex);

		} else if (selection instanceof SelectionChartXSliderPosition) {

			final SelectionChartXSliderPosition xSliderPos = (SelectionChartXSliderPosition) selection;

			final ChartDataModel chartDataModel = xSliderPos.getChart().getChartDataModel();
			final TourData tourData = (TourData) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);

			final int leftSliderValueIndex = xSliderPos.getSlider1ValueIndex();
			int rightSliderValueIndex = xSliderPos.getSlider2ValueIndex();
			rightSliderValueIndex = rightSliderValueIndex == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
					? leftSliderValueIndex
					: rightSliderValueIndex;

			paintTourSliders(tourData, leftSliderValueIndex, rightSliderValueIndex);

		} else if (selection instanceof PointOfInterest) {

			fIsTour = false;

			// disable tour data
			fTourData = null;
			fPreviousTourData = null;
			PaintManager.getInstance().setTourData(null);

			final PointOfInterest poi = (PointOfInterest) selection;
			fPOIPosition = poi.getPosition();

			fMap.setZoom(poi.getRecommendedZoom());
			fMap.setCenterPosition(fPOIPosition);
			fMap.queueRedrawMap();
		}

		enableActions();
	}

	private void paintEntireTour() {

		if (isPaintDataValid(fTourData) == false) {
			showDefaultMap();
			return;
		}

		final PaintManager paintManager = PaintManager.getInstance();
		paintManager.setTourData(fTourData);

		// set slider position
		fDirectMappingPainter.setPaintContext(fMap,
				fActionShowTourInMap.isChecked(),
				fTourData,
				fCurrentLeftSliderValueIndex,
				fCurrentRightSliderValueIndex);

		final Set<GeoPosition> tourBounds = getTourBounds(fTourData);
		paintManager.setTourBounds(tourBounds);

		fMap.setShowOverlays(fActionShowTourInMap.isChecked());

		setTourZoomLevel(tourBounds, false);

		fMap.queueRedrawMap();
	}

	private void paintTour(final TourData tourData, final boolean forceRedraw, final boolean ignoreSynch) {

		if (isPaintDataValid(tourData) == false) {
			showDefaultMap();
			return;
		}

		fIsTour = true;
		final boolean isShowTour = fActionShowTourInMap.isChecked();

		// prevent loading the same tour
		if (forceRedraw == false && tourData == fTourData) {
			return;
		}

		// check if this is a new tour
		boolean isNewTour = true;
		if (fPreviousTourData != null && fPreviousTourData.getTourId().longValue() == tourData.getTourId().longValue()) {
			isNewTour = false;
		}

		final PaintManager paintManager = PaintManager.getInstance();
		paintManager.setTourData(tourData);
		fTourData = tourData;

		// set slider position
		fDirectMappingPainter.setPaintContext(fMap,
				isShowTour,
				tourData,
				fCurrentLeftSliderValueIndex,
				fCurrentRightSliderValueIndex);

		final Set<GeoPosition> tourBounds = getTourBounds(tourData);
		paintManager.setTourBounds(tourBounds);

		fMap.setShowOverlays(isShowTour);
		fMap.setShowLegend(isShowTour);

		if (fIsMapSynchedWithTour && ignoreSynch == false) {

			if (forceRedraw == false && fPreviousTourData != null) {

				/*
				 * keep map configuration for the previous tour
				 */
				fPreviousTourData.mapZoomLevel = fMap.getZoom();

				final GeoPosition centerPosition = fMap.getCenterPosition();
				fPreviousTourData.mapCenterPositionLatitude = centerPosition.getLatitude();
				fPreviousTourData.mapCenterPositionLongitude = centerPosition.getLongitude();
			}

			if (tourData.mapCenterPositionLatitude == Double.MIN_VALUE) {
				// use default position for the tour
				setTourZoomLevel(tourBounds, true);
			} else {
				// position tour to the previous or position
				fMap.setZoom(tourData.mapZoomLevel);
				fMap.setCenterPosition(new GeoPosition(tourData.mapCenterPositionLatitude,
						tourData.mapCenterPositionLongitude));
			}
		}

		// keep tour data
		fPreviousTourData = tourData;

		if (isNewTour) {

			// a new tour is selected

			final ILegendProvider legendProvider = PaintManager.getInstance().getLegendProvider();
			if (legendProvider.getTourColorId() == TOUR_COLOR_ALTITUDE) {

				// adjust legend according the tour altitude
				createLegendImage(legendProvider);
			}

			fMap.setOverlayKey(tourData.getTourId().toString());
			fMap.resetOverlays();

			enableActions();
		}

		fMap.queueRedrawMap();
	}

	private void paintTourSliders(	final TourData tourData,
									final int leftSliderValuesIndex,
									final int rightSliderValuesIndex) {

		if (isPaintDataValid(tourData) == false) {
			showDefaultMap();
			return;
		}

		fIsTour = true;
		fCurrentLeftSliderValueIndex = leftSliderValuesIndex;
		fCurrentRightSliderValueIndex = rightSliderValuesIndex;

		fDirectMappingPainter.setPaintContext(fMap,
				fActionShowTourInMap.isChecked(),
				tourData,
				leftSliderValuesIndex,
				rightSliderValuesIndex);

		fMap.redraw();
	}

	private void restoreSettings() {

		final IMemento memento = fSessionMemento;

		if (memento != null) {

			final Integer mementoZoomCentered = memento.getInteger(MEMENTO_ZOOM_CENTERED);
			if (mementoZoomCentered != null) {
				final boolean isTourCentered = mementoZoomCentered == 1 ? true : false;
				fActionZoomCentered.setChecked(isTourCentered);
				fIsPositionCentered = isTourCentered;
			}

			final Integer mementoSynchTour = memento.getInteger(MEMENTO_SYNCH_WITH_SELECTED_TOUR);
			if (mementoSynchTour != null) {

				final boolean isSynchTour = mementoSynchTour == 1 ? true : false;

				fActionSynchWithTour.setChecked(isSynchTour);
				fIsMapSynchedWithTour = isSynchTour;
			}

			final Integer mementoShowTour = memento.getInteger(MEMENTO_SHOW_TOUR_IN_MAP);
			if (mementoShowTour != null) {

				final boolean isShowTour = mementoShowTour == 1 ? true : false;

				fActionShowTourInMap.setChecked(isShowTour);
				fMap.setShowOverlays(isShowTour);
				fMap.setShowLegend(isShowTour);
			}

			final Integer zoomLevel = memento.getInteger(MEMENTO_SYNCH_TOUR_ZOOM_LEVEL);
			if (zoomLevel != null) {
				fActionSynchTourZoomLevel.setZoomLevel(zoomLevel);
			}

			// restore: factory ID
			fActionChangeTileFactory.setSelectedFactory(memento.getString(MEMENTO_CURRENT_FACTORY_ID));

			// restore: default position
			final Integer mementoZoom = memento.getInteger(MEMENTO_DEFAULT_POSITION_ZOOM);
			if (mementoZoom != null) {
				fDefaultZoom = mementoZoom;
			}
			final Float mementoLatitude = memento.getFloat(MEMENTO_DEFAULT_POSITION_LATITUDE);
			final Float mementoLongitude = memento.getFloat(MEMENTO_DEFAULT_POSITION_LONGITUDE);
			if (mementoLatitude != null && mementoLongitude != null) {
				fDefaultPosition = new GeoPosition(mementoLatitude, mementoLongitude);
			} else {
				fDefaultPosition = new GeoPosition(0, 0);
			}

			// tour color
			final Integer colorId = memento.getInteger(MEMENTO_TOUR_COLOR_ID);
			if (colorId != null) {

				PaintManager.getInstance().setLegendProvider(getLegendProvider(colorId));

				switch (colorId) {
				case TOUR_COLOR_ALTITUDE:
					fActionTourColorAltitude.setChecked(true);
					break;

				case TOUR_COLOR_GRADIENT:
					fActionTourColorGradient.setChecked(true);
					break;

				case TOUR_COLOR_PULSE:
					fActionTourColorPulse.setChecked(true);
					break;

				case TOUR_COLOR_SPEED:
					fActionTourColorSpeed.setChecked(true);
					break;

				case TOUR_COLOR_PACE:
					fActionTourColorPace.setChecked(true);
					break;

				default:
					fActionTourColorAltitude.setChecked(true);
					break;
				}

				createLegendImage(getLegendProvider(colorId));
			}

		} else {

			// memento is not available, set default values

			fActionChangeTileFactory.setSelectedFactory(null);

			// draw tour with default color
			fActionTourColorAltitude.setChecked(true);

			PaintManager.getInstance().setLegendProvider(getLegendProvider(TOUR_COLOR_ALTITUDE));

			// hide legend
			fMap.setShowLegend(false);
		}

		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		final boolean isShowTileInfo = store.getBoolean(MappingView.SHOW_TILE_INFO);

		fMap.setDrawTileBorders(isShowTileInfo);
		actionSetDefaultPosition();

	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("DeviceImportView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	@Override
	public void saveState(final IMemento memento) {

		// save checked actions
		memento.putInteger(MEMENTO_ZOOM_CENTERED, fActionZoomCentered.isChecked() ? 1 : 0);
		memento.putInteger(MEMENTO_SHOW_TOUR_IN_MAP, fActionShowTourInMap.isChecked() ? 1 : 0);
		memento.putInteger(MEMENTO_SYNCH_WITH_SELECTED_TOUR, fActionSynchWithTour.isChecked() ? 1 : 0);
		memento.putInteger(MEMENTO_SYNCH_TOUR_ZOOM_LEVEL, fActionSynchTourZoomLevel.getZoomLevel());

		memento.putString(MEMENTO_CURRENT_FACTORY_ID, fActionChangeTileFactory.getSelectedFactory()
				.getInfo()
				.getFactoryID());

		if (fDefaultPosition == null) {
			memento.putInteger(MEMENTO_DEFAULT_POSITION_ZOOM, fMap.getTileFactory().getInfo().getMinimumZoomLevel());
			memento.putFloat(MEMENTO_DEFAULT_POSITION_LATITUDE, 0.0F);
			memento.putFloat(MEMENTO_DEFAULT_POSITION_LONGITUDE, 0.0F);
		} else {
			memento.putInteger(MEMENTO_DEFAULT_POSITION_ZOOM, fDefaultZoom);
			memento.putFloat(MEMENTO_DEFAULT_POSITION_LATITUDE, (float) fDefaultPosition.getLatitude());
			memento.putFloat(MEMENTO_DEFAULT_POSITION_LONGITUDE, (float) fDefaultPosition.getLongitude());
		}

		// tour color
		int colorId;

		if (fActionTourColorGradient.isChecked()) {
			colorId = TOUR_COLOR_GRADIENT;
		} else if (fActionTourColorPulse.isChecked()) {
			colorId = TOUR_COLOR_PULSE;
		} else if (fActionTourColorSpeed.isChecked()) {
			colorId = TOUR_COLOR_SPEED;
		} else if (fActionTourColorPace.isChecked()) {
			colorId = TOUR_COLOR_PACE;
		} else {
			colorId = TOUR_COLOR_ALTITUDE;
		}
		memento.putInteger(MEMENTO_TOUR_COLOR_ID, colorId);
	}

	@Override
	public void setFocus() {}

	/**
	 * Calculates a zoom level so that all points in the specified set will be visible on screen.
	 * This is useful if you have a bunch of points in an area like a city and you want to zoom out
	 * so that the entire city and it's points are visible without panning.
	 * 
	 * @param positions
	 *        A set of GeoPositions to calculate the new zoom from
	 * @param adjustZoomLevel
	 *        when <code>true</code> the zoom level will be adjusted to user settings
	 */
	private void setTourZoomLevel(final Set<GeoPosition> positions, final boolean isAdjustZoomLevel) {

		if (positions.size() < 2) {
			return;
		}

		final TileFactory tileFactory = fMap.getTileFactory();
		final TileFactoryInfo tileInfo = tileFactory.getInfo();

		final int maximumZoomLevel = tileInfo.getMaximumZoomLevel();
		int zoom = tileInfo.getMinimumZoomLevel();

		Rectangle2D positionRect = getPositionRect(positions, zoom);
		java.awt.Rectangle viewport = fMap.getViewport();

		// zoom until the tour is visible in the map
		while (!viewport.contains(positionRect)) {

			// center position in the map 
			final Point2D center = new Point2D.Double(positionRect.getX() + positionRect.getWidth() / 2,
					positionRect.getY() + positionRect.getHeight() / 2);
			final GeoPosition px = tileFactory.pixelToGeo(center, zoom);
			fMap.setCenterPosition(px);

			// check zoom level
			if (++zoom >= maximumZoomLevel) {
				break;
			}
			fMap.setZoom(zoom);

			positionRect = getPositionRect(positions, zoom);
			viewport = fMap.getViewport();
		}

		// zoom in until the tour is larger than the viewport
		while (positionRect.getWidth() < viewport.width && positionRect.getHeight() < viewport.height) {

			// center position in the map 
			final Point2D center = new Point2D.Double(positionRect.getX() + positionRect.getWidth() / 2,
					positionRect.getY() + positionRect.getHeight() / 2);
			final GeoPosition px = tileFactory.pixelToGeo(center, zoom);
			fMap.setCenterPosition(px);

			// check zoom level
			if (++zoom >= maximumZoomLevel) {
				break;
			}
			fMap.setZoom(zoom);

			positionRect = getPositionRect(positions, zoom);
			viewport = fMap.getViewport();
		}

		// the algorithm generated a larger zoom level as necessary
		zoom--;

		int adjustedZoomLevel = 0;
		if (isAdjustZoomLevel) {
			adjustedZoomLevel = PaintManager.getInstance().getSynchTourZoomLevel();
		}

		fMap.setZoom(zoom + adjustedZoomLevel);
	}

	private void showDefaultMap() {

		// disable tour actions in this view
		fIsTour = false;

		// disable tour data 
		fTourData = null;
		fPreviousTourData = null;

		// update direct painter to draw nothing
		fDirectMappingPainter.setPaintContext(fMap, false, null, 0, 0);

		fMap.setShowOverlays(false);
		fMap.setShowLegend(false);
//		fMap.setZoom(1);

		fMap.queueRedrawMap();
	}

	private void updateAltitudeLegend(Rectangle legendBounds) {

		if (fTourData == null) {
			return;
		}

		final int[] altitudeSerie = fTourData.getAltitudeSerie();
		if (altitudeSerie == null) {
			return;
		}

		ILegendProvider legendProvider = getLegendProvider(TOUR_COLOR_ALTITUDE);
		LegendConfig legendConfig = legendProvider.getLegendConfig();

		/*
		 * get min/max altitude
		 */
		int minValue = 0;
		int maxValue = 0;
		for (int valueIndex = 0; valueIndex < altitudeSerie.length; valueIndex++) {
			if (valueIndex == 0) {
				minValue = altitudeSerie[valueIndex];
				maxValue = altitudeSerie[valueIndex];
			} else {
				minValue = Math.min(minValue, altitudeSerie[valueIndex]);
				maxValue = Math.max(maxValue, altitudeSerie[valueIndex]);
			}
		}

		final List<Integer> legendUnits = getLegendUnits(legendBounds, minValue, maxValue);
		final Integer legendMinValue = legendUnits.get(0);
		final Integer legendMaxValue = legendUnits.get(legendUnits.size() - 1);

		legendConfig.units = legendUnits;
		legendConfig.legendMinValue = legendMinValue;
		legendConfig.legendMaxValue = legendMaxValue;
		legendConfig.unitText = UI.UNIT_LABEL_ALTITUDE;

		/*
		 * set color configuration, each tour has a different altitude config
		 */
		LegendColor legendColor = legendProvider.getLegendColor();

		final int midValueRelative = (legendMaxValue - legendMinValue) / 2;
		final int midValueAbsolute = legendMinValue + midValueRelative;

		legendColor.minValue = legendMinValue;
		legendColor.lowValue = legendMinValue + midValueRelative / 3;
		legendColor.midValue = midValueAbsolute;
		legendColor.highValue = legendMaxValue - midValueRelative / 3;
		legendColor.maxValue = legendMaxValue;

	}

	private void updateLegendValues(ILegendProvider legendColorProvider, Rectangle legendBounds) {

		switch (legendColorProvider.getTourColorId()) {
		case TOUR_COLOR_ALTITUDE:
			updateAltitudeLegend(legendBounds);
			break;

		default:
			break;
		}

	}
}
