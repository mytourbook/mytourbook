/*******************************************************************************
 * Copyright (C) 2005, 2010 Wolfgang Schramm and Contributors
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.mapping;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.colors.ColorDefinition;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourWayPoint;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceMap;
import net.tourbook.srtm.Activator;
import net.tourbook.srtm.IPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.MTRectangle;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;
import net.tourbook.util.Util;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
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
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

import de.byteholder.geoclipse.GeoclipseExtensions;
import de.byteholder.geoclipse.map.IMapContextProvider;
import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.MapLegend;
import de.byteholder.geoclipse.map.event.IPOIListener;
import de.byteholder.geoclipse.map.event.IPositionListener;
import de.byteholder.geoclipse.map.event.IZoomListener;
import de.byteholder.geoclipse.map.event.MapPOIEvent;
import de.byteholder.geoclipse.map.event.MapPositionEvent;
import de.byteholder.geoclipse.map.event.ZoomEvent;
import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;
import de.byteholder.gpx.GeoPosition;
import de.byteholder.gpx.PointOfInterest;

/**
 * @author Wolfgang Schramm
 * @since 1.3.0
 */
public class TourMapView extends ViewPart implements IMapContextProvider {

	private static final int						DEFAULT_LEGEND_WIDTH				= 150;
	private static final int						DEFAULT_LEGEND_HEIGHT				= 300;

	public static final int							LEGEND_MARGIN_TOP_BOTTOM			= 10;
	public static final int							LEGEND_UNIT_DISTANCE				= 60;

	public static final String						ID									= "net.tourbook.mapping.mappingViewID";	//$NON-NLS-1$

	public static final int							TOUR_COLOR_DEFAULT					= 0;
	public static final int							TOUR_COLOR_ALTITUDE					= 10;
	public static final int							TOUR_COLOR_GRADIENT					= 20;
	public static final int							TOUR_COLOR_PULSE					= 30;
	public static final int							TOUR_COLOR_SPEED					= 40;
	public static final int							TOUR_COLOR_PACE						= 50;

	private static final String						MEMENTO_SHOW_START_END_IN_MAP		= "action.show-start-end-in-map";			//$NON-NLS-1$
	private static final String						MEMENTO_SHOW_TOUR_MARKER			= "action.show-tour-marker";				//$NON-NLS-1$
	private static final String						MEMENTO_SHOW_SLIDER_IN_MAP			= "action.show-slider-in-map";				//$NON-NLS-1$
	private static final String						MEMENTO_SHOW_SLIDER_IN_LEGEND		= "action.show-slider-in-legend";			//$NON-NLS-1$
	private static final String						MEMENTO_SHOW_LEGEND_IN_MAP			= "action.show-legend-in-map";				//$NON-NLS-1$
	private static final String						MEMENTO_SHOW_SCALE_IN_MAP			= "action.show-scale-in-map";				//$NON-NLS-1$
	private static final String						MEMENTO_SHOW_TOUR_IN_MAP			= "action.show-tour-in-map";				//$NON-NLS-1$
	private static final String						MEMENTO_SYNCH_WITH_SELECTED_TOUR	= "action.synch-with-selected-tour";		//$NON-NLS-1$
	private static final String						MEMENTO_SYNCH_WITH_TOURCHART_SLIDER	= "action.synch-with-tourchart-slider";	//$NON-NLS-1$
	private static final String						MEMENTO_ZOOM_CENTERED				= "action.zoom-centered";					//$NON-NLS-1$
	private static final String						MEMENTO_MAP_DIM_LEVEL				= "action.map-dim-level";					//$NON-NLS-1$

	private static final String						MEMENTO_SYNCH_TOUR_ZOOM_LEVEL		= "synch-tour-zoom-level";					//$NON-NLS-1$
	private static final String						MEMENTO_SELECTED_MAP_PROVIDER_ID	= "selected.map-provider-id";				//$NON-NLS-1$

	private static final String						MEMENTO_DEFAULT_POSITION_ZOOM		= "default.position.zoom-level";			//$NON-NLS-1$
	private static final String						MEMENTO_DEFAULT_POSITION_LATITUDE	= "default.position.latitude";				//$NON-NLS-1$
	private static final String						MEMENTO_DEFAULT_POSITION_LONGITUDE	= "default.position.longitude";			//$NON-NLS-1$

	private static final String						MEMENTO_TOUR_COLOR_ID				= "tour-color-id";							//$NON-NLS-1$

	final static String								PREF_SHOW_TILE_INFO					= "map.debug.show.tile-info";				//$NON-NLS-1$
	final static String								PREF_DEBUG_MAP_DIM_LEVEL			= "map.debug.dim-map";						//$NON-NLS-1$

	private final IPreferenceStore					_prefStore							= TourbookPlugin
																								.getDefault()
																								.getPreferenceStore();

	private Map										_map;

	private ISelectionListener						_postSelectionListener;
	private IPropertyChangeListener					_prefChangeListener;
	private IPropertyChangeListener					_tourbookPrefChangeListener;
	private IPropertyChangeListener					_mapPrefChangeListener;
	private IPartListener2							_partListener;
	private ITourEventListener						_tourEventListener;

	/**
	 * contains the tours which are displayed in the map
	 */
	private final ArrayList<TourData>				_tourDataList						= new ArrayList<TourData>();
	private TourData								_previousTourData;

	private ActionDimMap							_actionDimMap;
	private ActionManageMapProviders				_actionManageProvider;
	private ActionReloadFailedMapImages				_actionReloadFailedMapImages;
	private ActionSelectMapProvider					_actionSelectMapProvider;
	private ActionSaveDefaultPosition				_actionSaveDefaultPosition;
	private ActionSetDefaultPosition				_actionSetDefaultPosition;
	private ActionShowPOI							_actionShowPOI;
	private ActionShowLegendInMap					_actionShowLegendInMap;
	private ActionShowScaleInMap					_actionShowScaleInMap;
	private ActionShowSliderInMap					_actionShowSliderInMap;
	private ActionShowSliderInLegend				_actionShowSliderInLegend;
	private ActionShowStartEndInMap					_actionShowStartEndInMap;
	private ActionShowTourInMap						_actionShowTourInMap;
	private ActionShowTourMarker					_actionShowTourMarker;
	private ActionSynchWithTour						_actionSynchWithTour;
	private ActionSynchWithSlider					_actionSynchWithSlider;
	private ActionSynchTourZoomLevel				_actionSynchTourZoomLevel;
	private ActionTourColor							_actionTourColorAltitude;
	private ActionTourColor							_actionTourColorGradient;
	private ActionTourColor							_actionTourColorPulse;
	private ActionTourColor							_actionTourColorSpeed;
	private ActionTourColor							_actionTourColorPace;
	private ActionZoomIn							_actionZoomIn;
	private ActionZoomOut							_actionZoomOut;
	private ActionZoomCentered						_actionZoomCentered;
	private ActionZoomShowAll						_actionZoomShowAll;
	private ActionZoomShowEntireTour				_actionZoomShowEntireTour;

	private boolean									_isMapSynchedWithTour;
	private boolean									_isMapSynchedWithSlider;
	private boolean									_isPositionCentered;

	private int										_defaultZoom;
	private GeoPosition								_defaultPosition					= null;

	/**
	 * when <code>true</code> a tour is painted, <code>false</code> a point of interrest is painted
	 */
	private boolean									_isTour;

	/*
	 * POI
	 */
	private String									_poiName;
	private GeoPosition								_poiPosition;
	private int										_poiZoomLevel;

	private final DirectMappingPainter				_directMappingPainter				= new DirectMappingPainter();

	/*
	 * current position for the x-sliders
	 */
	private int										_currentLeftSliderValueIndex;
	private int										_currentRightSliderValueIndex;
	private int										_currentSelectedSliderValueIndex;

	private MapLegend								_mapLegend;
	private final HashMap<Integer, ILegendProvider>	_legendProviders					= new HashMap<Integer, ILegendProvider>();

	private long									_previousOverlayKey;

	private int										_mapDimLevel						= -1;
	private RGB										_mapDimColor;

	private int										_selectedProfileKey					= 0;

	private final MapInfoManager					_mapInfoManager						= MapInfoManager.getInstance();

	public TourMapView() {}

	void actionDimMap(final int dimLevel) {

		// check if the dim level/color was changed
		if (_mapDimLevel != dimLevel) {

			_mapDimLevel = dimLevel;

			/*
			 * dim color is stored in the pref store and not in the memento
			 */
			final RGB dimColor = PreferenceConverter.getColor(_prefStore, ITourbookPreferences.MAP_LAYOUT_DIM_COLOR);

			_map.dimMap(dimLevel, dimColor);
		}
	}

	private void actionDimMap(final RGB dimColor) {

		if (_mapDimColor != dimColor) {

			_mapDimColor = dimColor;

			_map.dimMap(_mapDimLevel, dimColor);
		}
	}

	void actionOpenMapProviderDialog() {

		final ModifyMapProviderDialog dialog = new ModifyMapProviderDialog(Display.getCurrent().getActiveShell());

		if (dialog.open() == Window.OK) {
			_actionSelectMapProvider.updateMapProviders();
		}
	}

	void actionPOI() {

		final boolean isShowPOI = _actionShowPOI.isChecked();

		_map.setShowPOI(isShowPOI);

		if (isShowPOI) {
			_map.setPOI(_poiPosition, _map.getZoom(), _poiName);
		}
	}

	void actionReloadFailedMapImages() {
		_map.resetAll();
	}

	void actionSaveDefaultPosition() {
		_defaultZoom = _map.getZoom();
		_defaultPosition = _map.getGeoCenter();
	}

	void actionSetDefaultPosition() {
		if (_defaultPosition == null) {
			_map.setZoom(_map.getMapProvider().getMinimumZoomLevel());
			_map.setGeoCenterPosition(new GeoPosition(0, 0));
		} else {
			_map.setZoom(_defaultZoom);
			_map.setGeoCenterPosition(_defaultPosition);
		}
		_map.queueMapRedraw();
	}

	void actionSetShowLegendInMap() {

		final boolean isLegendVisible = _actionShowLegendInMap.isChecked();

		_map.setShowLegend(isLegendVisible);

		_actionShowSliderInLegend.setEnabled(isLegendVisible);
		if (isLegendVisible == false) {
			_actionShowSliderInLegend.setChecked(false);
		}

		// update legend
		actionShowSlider();

		_map.queueMapRedraw();
	}

	void actionSetShowScaleInMap() {

		final boolean isScaleVisible = _actionShowScaleInMap.isChecked();

		_map.setShowScale(isScaleVisible);
		_map.queueMapRedraw();
	}

	void actionSetShowStartEndInMap() {

		PaintManager.getInstance().setShowStartEnd(_actionShowStartEndInMap.isChecked());

		_map.disposeOverlayImageCache();
		_map.queueMapRedraw();
	}

	void actionSetShowTourInMap() {
		paintAllTours();
	}

	void actionSetShowTourMarkerInMap() {

		PaintManager.getInstance().setShowTourMarker(_actionShowTourMarker.isChecked());

		_map.disposeOverlayImageCache();
		_map.queueMapRedraw();
	}

	void actionSetTourColor(final int colorId) {

		final ILegendProvider legendProvider = getLegendProvider(colorId);

		PaintManager.getInstance().setLegendProvider(legendProvider);

		_map.disposeOverlayImageCache();
		_map.queueMapRedraw();

		createLegendImage(legendProvider);
	}

	void actionSetZoomCentered() {
		_isPositionCentered = _actionZoomCentered.isChecked();
	}

	void actionShowSlider() {

		if ((_tourDataList == null) || (_tourDataList.size() == 0)) {
			return;
		}

		// repaint map
		_directMappingPainter.setPaintContext(
				_map,
				_actionShowTourInMap.isChecked(),
				_tourDataList.get(0),
				_currentLeftSliderValueIndex,
				_currentRightSliderValueIndex,
				_actionShowSliderInMap.isChecked(),
				_actionShowSliderInLegend.isChecked());

		_map.redraw();
	}

	void actionSynchWithSlider() {

		if (_tourDataList.size() == 0) {
			return;
		}

		_isMapSynchedWithSlider = _actionSynchWithSlider.isChecked();

		if (_isMapSynchedWithSlider) {

			_actionShowTourInMap.setChecked(true);

			// map must be synched with selected tour
			_actionSynchWithTour.setChecked(true);
			_isMapSynchedWithTour = true;

			_map.setShowOverlays(true);

			final TourData firstTourData = _tourDataList.get(0);

			paintOneTour(firstTourData, false, true);
			setMapToSliderBounds(firstTourData);
		}
	}

	void actionSynchWithTour() {

		if (_tourDataList.size() == 0) {
			return;
		}

		_isMapSynchedWithTour = _actionSynchWithTour.isChecked();

		if (_isMapSynchedWithTour) {

			_actionShowTourInMap.setChecked(true);
			_map.setShowOverlays(true);

			paintOneTour(_tourDataList.get(0), true, true);

		} else {

			// disable synch with slider
			_isMapSynchedWithSlider = false;
			_actionSynchWithSlider.setChecked(false);
		}
	}

	void actionZoomIn() {
		_map.setZoom(_map.getZoom() + 1);
		centerTour();
		_map.queueMapRedraw();
	}

	void actionZoomOut() {
		_map.setZoom(_map.getZoom() - 1);
		centerTour();
		_map.queueMapRedraw();
	}

	void actionZoomShowEntireMap() {
		_map.setZoom(_map.getMapProvider().getMinimumZoomLevel());
		_map.queueMapRedraw();
	}

	void actionZoomShowEntireTour() {

		_actionShowTourInMap.setChecked(true);
		_map.setShowOverlays(true);

		paintEntireTour();
	}

	private void addMapListener() {

		/*
		 * observe map preferences
		 */
		_mapPrefChangeListener = new Preferences.IPropertyChangeListener() {
			@Override
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();
				if (property.equals(IPreferences.SRTM_COLORS_SELECTED_PROFILE_KEY)) {

					final String newValue = event.getNewValue().toString();
					final Integer prefProfileKey = Integer.parseInt(newValue);

					if (prefProfileKey != _selectedProfileKey) {

						_selectedProfileKey = prefProfileKey;

						_map.disposeTiles();
						_map.queueMapRedraw();
					}
				}
			}
		};
		// !!! SRTM pref store !!!
		Activator.getDefault().getPluginPreferences().addPropertyChangeListener(_mapPrefChangeListener);

		_map.addZoomListener(new IZoomListener() {
			@Override
			public void zoomChanged(final ZoomEvent event) {
				_mapInfoManager.setZoom(event.getZoom());
				centerTour();
			}
		});

		_map.addMousePositionListener(new IPositionListener() {
			@Override
			public void setPosition(final MapPositionEvent event) {
				_mapInfoManager.setMousePosition(event.mapGeoPosition);
			}
		});

		_map.addPOIListener(new IPOIListener() {
			@Override
			public void setPOI(final MapPOIEvent poiEvent) {

				_poiPosition = poiEvent.mapGeoPosition;
				_poiZoomLevel = poiEvent.mapZoomLevel;
				_poiName = poiEvent.mapPOIText;

				_actionShowPOI.setEnabled(true);
				_actionShowPOI.setChecked(true);
			}
		});

		_map.setMapContextProvider(this);
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourMapView.this) {
					saveState();
					_mapInfoManager.resetInfo();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new Preferences.IPropertyChangeListener() {
			@Override
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(PREF_SHOW_TILE_INFO)) {

					// map properties has changed

					final boolean isShowTileInfo = _prefStore.getBoolean(PREF_SHOW_TILE_INFO);

					_map.setShowDebugInfo(isShowTileInfo);
					_map.queueMapRedraw();

				} else if (property.equals(PREF_DEBUG_MAP_DIM_LEVEL)) {

					float prefDimLevel = _prefStore.getInt(TourMapView.PREF_DEBUG_MAP_DIM_LEVEL);
					prefDimLevel *= 2.55;
					prefDimLevel -= 255;

					final int dimLevel = (int) Math.abs(prefDimLevel);
					_actionDimMap.setDimLevel(dimLevel);
					actionDimMap(dimLevel);

				} else if (property.equals(ITourbookPreferences.MAP_LAYOUT_DIM_COLOR)) {

					actionDimMap(PreferenceConverter.getColor(_prefStore, ITourbookPreferences.MAP_LAYOUT_DIM_COLOR));

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					UI.updateUnits();
					_map.setMeasurementSystem(UI.UNIT_VALUE_DISTANCE, UI.UNIT_LABEL_DISTANCE);
					_map.queueMapRedraw();

				} else if (property.equals(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD)) {

					_map.setTourPaintMethodEnhanced(//
							event.getNewValue().equals(PrefPageAppearanceMap.TOUR_PAINT_METHOD_COMPLEX));

				} else if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {

					// update tour and legend

					createLegendImage(PaintManager.getInstance().getLegendProvider());

					_map.disposeOverlayImageCache();
					_map.queueMapRedraw();
				}
			}
		};

		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourbookPrefListener() {

		_tourbookPrefChangeListener = new Preferences.IPropertyChangeListener() {
			@Override
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					createLegendImage(PaintManager.getInstance().getLegendProvider());

					_map.queueMapRedraw();
				}
			}
		};

		// register the listener
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(_tourbookPrefChangeListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == TourMapView.this) {
					return;
				}

				if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

					resetMap();

				} else if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if ((modifiedTours != null) && (modifiedTours.size() > 0)) {

						_tourDataList.clear();
						_tourDataList.addAll(modifiedTours);

						resetMap();
					}

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();

				} else if (eventId == TourEventId.SLIDER_POSITION_CHANGED) {
					onSelectionChanged((ISelection) eventData);
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	/**
	 * Center the tour in the map when action is enabled
	 */
	private void centerTour() {

		if (_isPositionCentered) {

			final int zoom = _map.getZoom();

			Set<GeoPosition> positionBounds = null;
			if (_isTour) {
				positionBounds = PaintManager.getInstance().getTourBounds();
				if (positionBounds == null) {
					return;
				}
			} else {
				if (_poiPosition == null) {
					return;
				}
				positionBounds = new HashSet<GeoPosition>();
				positionBounds.add(_poiPosition);
			}

			final Rectangle positionRect = getPositionRect(positionBounds, zoom);

			final Point center = new Point(//
					positionRect.x + positionRect.width / 2,
					positionRect.y + positionRect.height / 2);

			final GeoPosition geoPosition = _map.getMapProvider().pixelToGeo(center, zoom);

			_map.setGeoCenterPosition(geoPosition);
		}
	}

	private void clearView() {

		// disable tour data
		_tourDataList.clear();
		_previousTourData = null;

		PaintManager.getInstance().setTourData(new ArrayList<TourData>());

		showDefaultMap();
	}

	private void createActions() {

		_actionTourColorAltitude = new ActionTourColor(
				this,
				TOUR_COLOR_ALTITUDE,
				Messages.map_action_tour_color_altitude_tooltip,
				Messages.image_action_tour_color_altitude,
				Messages.image_action_tour_color_altitude_disabled);

		_actionTourColorGradient = new ActionTourColor(
				this,
				TOUR_COLOR_GRADIENT,
				Messages.map_action_tour_color_gradient_tooltip,
				Messages.image_action_tour_color_gradient,
				Messages.image_action_tour_color_gradient_disabled);

		_actionTourColorPulse = new ActionTourColor(
				this,
				TOUR_COLOR_PULSE,
				Messages.map_action_tour_color_pulse_tooltip,
				Messages.image_action_tour_color_pulse,
				Messages.image_action_tour_color_pulse_disabled);

		_actionTourColorSpeed = new ActionTourColor(
				this,
				TOUR_COLOR_SPEED,
				Messages.map_action_tour_color_speed_tooltip,
				Messages.image_action_tour_color_speed,
				Messages.image_action_tour_color_speed_disabled);

		_actionTourColorPace = new ActionTourColor(
				this,
				TOUR_COLOR_PACE,
				Messages.map_action_tour_color_pase_tooltip,
				Messages.image_action_tour_color_pace,
				Messages.image_action_tour_color_pace_disabled);

//		_actionTourColorTourType = new ActionTourColor(this,
//				TOUR_COLOR_TOURTYPE,
//				Messages.map_action_tour_color_tourType_tooltip,
//				Messages.image_action_tour_color_tourType,
//				Messages.image_action_tour_color_tourType_disabled);

		_actionZoomIn = new ActionZoomIn(this);
		_actionZoomOut = new ActionZoomOut(this);
		_actionZoomCentered = new ActionZoomCentered(this);
		_actionZoomShowAll = new ActionZoomShowAll(this);
		_actionZoomShowEntireTour = new ActionZoomShowEntireTour(this);
		_actionSynchWithTour = new ActionSynchWithTour(this);
		_actionSynchWithSlider = new ActionSynchWithSlider(this);
		_actionShowPOI = new ActionShowPOI(this);
		_actionShowTourInMap = new ActionShowTourInMap(this);
		_actionSynchTourZoomLevel = new ActionSynchTourZoomLevel(this);
		_actionSelectMapProvider = new ActionSelectMapProvider(this);
		_actionSetDefaultPosition = new ActionSetDefaultPosition(this);
		_actionSaveDefaultPosition = new ActionSaveDefaultPosition(this);
		_actionShowSliderInMap = new ActionShowSliderInMap(this);
		_actionShowSliderInLegend = new ActionShowSliderInLegend(this);
		_actionShowLegendInMap = new ActionShowLegendInMap(this);
		_actionShowScaleInMap = new ActionShowScaleInMap(this);
		_actionShowStartEndInMap = new ActionShowStartEndInMap(this);
		_actionShowTourMarker = new ActionShowTourMarker(this);
		_actionReloadFailedMapImages = new ActionReloadFailedMapImages(this);
		_actionDimMap = new ActionDimMap(this);
		_actionManageProvider = new ActionManageMapProviders(this);

		/*
		 * fill view toolbar
		 */
		final IToolBarManager viewTbm = getViewSite().getActionBars().getToolBarManager();

		viewTbm.add(_actionTourColorAltitude);
		viewTbm.add(_actionTourColorPulse);
		viewTbm.add(_actionTourColorSpeed);
		viewTbm.add(_actionTourColorPace);
		viewTbm.add(_actionTourColorGradient);
		viewTbm.add(new Separator());

		viewTbm.add(_actionShowTourInMap);
		viewTbm.add(_actionZoomShowEntireTour);
		viewTbm.add(_actionSynchWithTour);
		viewTbm.add(_actionSynchWithSlider);
		viewTbm.add(new Separator());

		viewTbm.add(_actionSelectMapProvider);
		viewTbm.add(new Separator());

		viewTbm.add(_actionZoomCentered);
		viewTbm.add(_actionZoomIn);
		viewTbm.add(_actionZoomOut);
		viewTbm.add(_actionZoomShowAll);
		viewTbm.add(_actionShowPOI);

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		fillMapMenu(menuMgr);
	}

	/**
	 * Creates a new legend image and disposes the old image
	 * 
	 * @param legendProvider
	 */
	private void createLegendImage(final ILegendProvider legendProvider) {

		Image legendImage = _mapLegend.getImage();

		// legend requires a tour with coordinates
		if (legendProvider == null /* || isPaintDataValid(fTourData) == false */) {
			showDefaultMap();
			return;
		}

		// dispose old legend image
		if ((legendImage != null) && !legendImage.isDisposed()) {
			legendImage.dispose();
		}
		final int legendWidth = DEFAULT_LEGEND_WIDTH;
		int legendHeight = DEFAULT_LEGEND_HEIGHT;

		final Rectangle mapBounds = _map.getBounds();
		legendHeight = Math.max(1, Math.min(legendHeight, mapBounds.height));

		final RGB rgbTransparent = new RGB(0xfe, 0xfe, 0xfe);

		final ImageData overlayImageData = new ImageData(legendWidth, legendHeight, 24, //
				new PaletteData(0xff, 0xff00, 0xff00000));

		overlayImageData.transparentPixel = overlayImageData.palette.getPixel(rgbTransparent);

		final Display display = Display.getCurrent();
		legendImage = new Image(display, overlayImageData);
		final Rectangle legendImageBounds = legendImage.getBounds();

		final boolean isDataAvailable = updateLegendValues(legendProvider, legendImageBounds);

		final Color transparentColor = new Color(display, rgbTransparent);
		final GC gc = new GC(legendImage);
		{
			gc.setBackground(transparentColor);
			gc.fillRectangle(legendImageBounds);

			if (isDataAvailable) {
				TourPainter.drawLegendColors(gc, legendImageBounds, legendProvider, true);
			}
		}
		gc.dispose();
		transparentColor.dispose();

		_mapLegend.setImage(legendImage);
	}

	private void createLegendProviders() {

		_legendProviders.put(//
				TourMapView.TOUR_COLOR_PULSE, //
				new LegendProvider(new LegendConfig(), new LegendColor(), TourMapView.TOUR_COLOR_PULSE));

		_legendProviders.put(//
				TourMapView.TOUR_COLOR_ALTITUDE, //
				new LegendProvider(new LegendConfig(), new LegendColor(), TourMapView.TOUR_COLOR_ALTITUDE));

		_legendProviders.put(//
				TourMapView.TOUR_COLOR_SPEED, //
				new LegendProvider(new LegendConfig(), new LegendColor(), TourMapView.TOUR_COLOR_SPEED));

		_legendProviders.put(//
				TourMapView.TOUR_COLOR_PACE, //
				new LegendProvider(new LegendConfig(), new LegendColor(), TourMapView.TOUR_COLOR_PACE));

		_legendProviders.put(//
				TourMapView.TOUR_COLOR_GRADIENT, //
				new LegendProvider(new LegendConfig(), new LegendColor(), TourMapView.TOUR_COLOR_GRADIENT));

//		fLegendProviders.put(//
//				TourMapView.TOUR_COLOR_TOURTYPE, //
//				new LegendProvider(new LegendConfig(), new LegendColor(), TourMapView.TOUR_COLOR_TOURTYPE));
	}

	@Override
	public void createPartControl(final Composite parent) {

		_map = new Map(parent, SWT.NONE);
		_map.setDirectPainter(_directMappingPainter);

		_mapLegend = new MapLegend();
		_map.setLegend(_mapLegend);
		_map.setShowLegend(true);
		_map.setMeasurementSystem(UI.UNIT_VALUE_DISTANCE, UI.UNIT_LABEL_DISTANCE);

		final String tourPaintMethod = _prefStore.getString(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD);
		_map.setTourPaintMethodEnhanced(tourPaintMethod.equals(PrefPageAppearanceMap.TOUR_PAINT_METHOD_COMPLEX));

		_map.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {

				/*
				 * check if the legend size must be adjusted
				 */
				final Image legendImage = _mapLegend.getImage();
				if ((legendImage == null) || legendImage.isDisposed()) {
					return;
				}

				final boolean showTour = _actionShowTourInMap.isChecked();
				final boolean showLegend = _actionShowLegendInMap.isChecked();
				if ((_isTour == false) || (showTour == false) || (showLegend == false)) {
					return;
				}

				/*
				 * check height
				 */
				final Rectangle mapBounds = _map.getBounds();
				final Rectangle legendBounds = legendImage.getBounds();

				if ((mapBounds.height < DEFAULT_LEGEND_HEIGHT //
						)
						|| ((mapBounds.height > DEFAULT_LEGEND_HEIGHT //
						) && (legendBounds.height < DEFAULT_LEGEND_HEIGHT))) {

					createLegendImage(PaintManager.getInstance().getLegendProvider());
				}
			}
		});

		createActions();
		createLegendProviders();

		addPartListener();
		addPrefListener();
		addSelectionListener();
		addTourEventListener();
		addTourbookPrefListener();
		addMapListener();

		// register overlays which draw the tour
		GeoclipseExtensions.registerOverlays(_map);

		// initialize map when part is created and the map size is > 0
		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {

				restoreState();

				if (_tourDataList.size() == 0) {
					// a tour is not displayed, find a tour provider which provides a tour
					showToursFromTourProvider();
				} else {
					_map.queueMapRedraw();
				}

				if (_mapDimLevel < 30) {
					showDimWarning();
				}
			}
		});
	}

	@Override
	public void dispose() {

		_tourDataList.clear();

		// dispose tilefactory resources

		final ArrayList<MP> allMapProviders = MapProviderManager.getInstance().getAllMapProviders(true);
		for (final MP mp : allMapProviders) {
			mp.disposeAllImages();
		}

		_map.disposeOverlayImageCache();

		getViewSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		final Preferences pluginPreferences = TourbookPlugin.getDefault().getPluginPreferences();
		pluginPreferences.removePropertyChangeListener(_prefChangeListener);
		pluginPreferences.removePropertyChangeListener(_tourbookPrefChangeListener);

		super.dispose();
	}

	private void enableActions() {
		enableActions(false);
	}

	private void enableActions(final boolean isForceTourColor) {

		_actionShowPOI.setEnabled(_poiPosition != null);

		// update legend action
		if (_isTour) {

			final boolean isLegendVisible = _actionShowLegendInMap.isChecked();

			_map.setShowLegend(isLegendVisible);

			_actionShowSliderInLegend.setEnabled(isLegendVisible);
			if (isLegendVisible == false) {
				_actionShowSliderInLegend.setChecked(false);
			}
		}

		final boolean isMultipleTours = _tourDataList.size() > 1;
		final boolean isOneTour = _isTour && (isMultipleTours == false);

		/*
		 * enable/disable tour actions
		 */
		_actionZoomShowEntireTour.setEnabled(isOneTour);
		_actionSynchTourZoomLevel.setEnabled(isOneTour);
		_actionShowTourInMap.setEnabled(_isTour);
		_actionSynchWithTour.setEnabled(isOneTour);
		_actionSynchWithSlider.setEnabled(isOneTour);

		_actionShowStartEndInMap.setEnabled(isOneTour);
		_actionShowTourMarker.setEnabled(_isTour);
		_actionShowLegendInMap.setEnabled(_isTour);
		_actionShowSliderInMap.setEnabled(_isTour);
		_actionShowSliderInLegend.setEnabled(isOneTour);

		if (_tourDataList.size() == 0) {

			_actionTourColorAltitude.setEnabled(false);
			_actionTourColorGradient.setEnabled(false);
			_actionTourColorPulse.setEnabled(false);
			_actionTourColorSpeed.setEnabled(false);
			_actionTourColorPace.setEnabled(false);
//			_actionTourColorTourType.setEnabled(false);

		} else if (isForceTourColor) {

			_actionTourColorAltitude.setEnabled(true);
			_actionTourColorGradient.setEnabled(true);
			_actionTourColorPulse.setEnabled(true);
			_actionTourColorSpeed.setEnabled(true);
			_actionTourColorPace.setEnabled(true);
//			_actionTourColorTourType.setEnabled(true);

		} else if (isOneTour) {

			final TourData oneTourData = _tourDataList.get(0);
			_actionTourColorAltitude.setEnabled(true);
			_actionTourColorGradient.setEnabled(oneTourData.getGradientSerie() != null);
			_actionTourColorPulse.setEnabled(oneTourData.pulseSerie != null);
			_actionTourColorSpeed.setEnabled(oneTourData.getSpeedSerie() != null);
			_actionTourColorPace.setEnabled(oneTourData.getPaceSerie() != null);
//			_actionTourColorTourType.setEnabled(true);

		} else {

			_actionTourColorAltitude.setEnabled(false);
			_actionTourColorGradient.setEnabled(false);
			_actionTourColorPulse.setEnabled(false);
			_actionTourColorSpeed.setEnabled(false);
			_actionTourColorPace.setEnabled(false);
//			_actionTourColorTourType.setEnabled(false);
		}
	}

	@Override
	public void fillContextMenu(final IMenuManager menuMgr) {
		fillMapMenu(menuMgr);
	}

	private void fillMapMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionShowStartEndInMap);
		menuMgr.add(_actionShowTourMarker);
		menuMgr.add(_actionShowLegendInMap);
		menuMgr.add(_actionShowScaleInMap);
		menuMgr.add(_actionShowSliderInMap);
		menuMgr.add(_actionShowSliderInLegend);
		menuMgr.add(_actionShowPOI);
		menuMgr.add(new Separator());

		menuMgr.add(_actionSetDefaultPosition);
		menuMgr.add(_actionSaveDefaultPosition);
		menuMgr.add(new Separator());

		menuMgr.add(_actionDimMap);
		menuMgr.add(_actionSynchTourZoomLevel);
		menuMgr.add(new Separator());

		menuMgr.add(_actionManageProvider);
		menuMgr.add(_actionReloadFailedMapImages);
	}

	private ILegendProvider getLegendProvider(final int colorId) {
		return _legendProviders.get(colorId);
	}

	public Map getMap() {
		return _map;
	}

	public int getMapDimLevel() {
		return _mapDimLevel;
	}

	private Rectangle getPositionRect(final Set<GeoPosition> positions, final int zoom) {

		final MP mp = _map.getMapProvider();
		final Point point1 = mp.geoToPixel(positions.iterator().next(), zoom);
		final MTRectangle mtRect = new MTRectangle(point1.x, point1.y, 0, 0);

		for (final GeoPosition pos : positions) {
			final Point point = mp.geoToPixel(pos, zoom);
			mtRect.add(point.x, point.y);
		}

		return new Rectangle(mtRect.x, mtRect.y, mtRect.width, mtRect.height);
	}

	/**
	 * Calculate the bounds for the tour in latitude and longitude values
	 * 
	 * @param tourData
	 * @return
	 */
	private Set<GeoPosition> getTourBounds(final TourData tourData) {

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		if ((latitudeSerie == null) || (longitudeSerie == null)) {
			return null;
		}

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

//			minLatitude = Math.min(minLatitude, latitude);
//			maxLatitude = Math.max(maxLatitude, latitude);
//
//			minLongitude = Math.min(minLongitude, longitude);
//			maxLongitude = Math.max(maxLongitude, longitude);

			minLatitude = latitude < minLatitude ? latitude : minLatitude;
			maxLatitude = latitude > maxLatitude ? latitude : maxLatitude;

			minLongitude = longitude < minLongitude ? longitude : minLongitude;
			maxLongitude = longitude > maxLongitude ? longitude : maxLongitude;

			if (minLatitude == 0) {
				minLatitude = -180D;
			}
		}

		final Set<GeoPosition> mapPositions = new HashSet<GeoPosition>();
		mapPositions.add(new GeoPosition(minLatitude, minLongitude));
		mapPositions.add(new GeoPosition(maxLatitude, maxLongitude));

		return mapPositions;
	}

	/**
	 * Checks if {@link TourData} can be painted
	 * 
	 * @param tourData
	 * @return <code>true</code> when {@link TourData} contains a tour which can be painted in the
	 *         map
	 */
	private boolean isPaintDataValid(final TourData tourData) {

		if (tourData == null) {
			return false;
		}

		// check if coordinates are available

		final double[] longitudeSerie = tourData.longitudeSerie;
		final double[] latitudeSerie = tourData.latitudeSerie;

		if ((longitudeSerie == null)
				|| (longitudeSerie.length == 0)
				|| (latitudeSerie == null)
				|| (latitudeSerie.length == 0)) {
			return false;
		}

		return true;
	}

	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof SelectionTourData) {

			final SelectionTourData selectionTourData = (SelectionTourData) selection;
			final TourData tourData = selectionTourData.getTourData();

			paintOneTour(tourData, selectionTourData.isForceRedraw(), true);

			enableActions();

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId tourIdSelection = (SelectionTourId) selection;
			final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

			paintOneTour(tourData, false, true);

			enableActions();

		} else if (selection instanceof SelectionTourIds) {

			// paint all selected tours

			final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
			if (tourIds.size() == 0) {
				return;
			}

			paintTours(tourIds);

			enableActions(true);

		} else if (selection instanceof SelectionActiveEditor) {

			final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();
			if (editor instanceof TourEditor) {

				final TourEditor fTourEditor = (TourEditor) editor;
				final TourChart fTourChart = fTourEditor.getTourChart();
				final TourData tourData = fTourChart.getTourData();

				paintOneTour(tourData, false, true);

				final SelectionChartInfo chartInfo = fTourChart.getChartInfo();
				paintTourSliders(
						tourData,
						chartInfo.leftSliderValuesIndex,
						chartInfo.rightSliderValuesIndex,
						chartInfo.selectedSliderValuesIndex);

				enableActions();
			}

		} else if (selection instanceof SelectionChartInfo) {

			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
			if (chartDataModel != null) {

				final Object tourId = chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
				if (tourId instanceof Long) {

					TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
					if (tourData == null) {

						// tour is not in the database, try to get it from the raw data manager

						final HashMap<Long, TourData> rawData = RawDataManager.getInstance().getImportedTours();
						tourData = rawData.get(tourId);
					}

					if (tourData != null) {

						final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

						paintTourSliders(
								tourData,
								chartInfo.leftSliderValuesIndex,
								chartInfo.rightSliderValuesIndex,
								chartInfo.selectedSliderValuesIndex);

						enableActions();
					}
				}
			}

		} else if (selection instanceof SelectionChartXSliderPosition) {

			final SelectionChartXSliderPosition xSliderPos = (SelectionChartXSliderPosition) selection;
			final Chart chart = xSliderPos.getChart();
			if (chart == null) {
				return;
			}

			final ChartDataModel chartDataModel = chart.getChartDataModel();

			final Object tourId = chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
			if (tourId instanceof Long) {

				final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
				if (tourData != null) {

					final int leftSliderValueIndex = xSliderPos.getLeftSliderValueIndex();
					int rightSliderValueIndex = xSliderPos.getRightSliderValueIndex();

					rightSliderValueIndex = rightSliderValueIndex == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
							? leftSliderValueIndex
							: rightSliderValueIndex;

					paintTourSliders(tourData, leftSliderValueIndex, rightSliderValueIndex, leftSliderValueIndex);

					enableActions();
				}
			}

		} else if (selection instanceof SelectionMapPosition) {

			final SelectionMapPosition mapPositionSelection = (SelectionMapPosition) selection;

			final int valueIndex1 = mapPositionSelection.getSlider1ValueIndex();
			int valueIndex2 = mapPositionSelection.getSlider2ValueIndex();

			valueIndex2 = valueIndex2 == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
					? valueIndex1
					: valueIndex2;

			paintTourSliders(mapPositionSelection.getTourData(), valueIndex1, valueIndex2, valueIndex1);

			enableActions();

		} else if (selection instanceof PointOfInterest) {

			_isTour = false;

			clearView();

			final PointOfInterest poi = (PointOfInterest) selection;

			_poiPosition = poi.getPosition();
			_poiName = poi.getName();

			_poiZoomLevel = poi.getRecommendedZoom();
			if (_poiZoomLevel == -1) {
				_poiZoomLevel = _map.getZoom();
			}

			_map.setPOI(_poiPosition, _poiZoomLevel, _poiName);

			_actionShowPOI.setChecked(true);

			enableActions();

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();

			if (firstElement instanceof TVICatalogComparedTour) {

				final TVICatalogComparedTour comparedTour = (TVICatalogComparedTour) firstElement;
				final long tourId = comparedTour.getTourId();

				final TourData tourData = TourManager.getInstance().getTourData(tourId);
				paintOneTour(tourData, false, true);

			} else if (firstElement instanceof TVICompareResultComparedTour) {

				final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
				final TourData tourData = TourManager.getInstance().getTourData(
						compareResultItem.getComparedTourData().getTourId());
				paintOneTour(tourData, false, true);

			} else if (firstElement instanceof TourWayPoint) {

				final TourWayPoint wp = (TourWayPoint) firstElement;

				_poiPosition = wp.getPosition();
				_poiName = wp.getName();
				_poiZoomLevel = _map.getZoom();

				_map.setPOI(_poiPosition, _poiZoomLevel, _poiName);

				_actionShowPOI.setChecked(true);
			}

			enableActions();

		} else if (selection instanceof SelectionTourCatalogView) {

			// show reference tour

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {

				final TourData tourData = TourManager.getInstance().getTourData(refItem.getTourId());
				paintOneTour(tourData, false, true);

				enableActions();
			}
		}

	}

	private void paintAllTours() {

		if (_tourDataList.size() == 0) {
			return;
		}

		// show/hide legend
		_map.setShowLegend(_actionShowTourInMap.isChecked());

		if (_tourDataList.size() > 1) {

			// multiple tours are displayed

			paintTours();
			enableActions(true);

		} else {
			paintOneTour(_tourDataList.get(0), true, false);
			enableActions();
		}
	}

	private void paintEntireTour() {

		if ((_tourDataList.size() == 0) || (isPaintDataValid(_tourDataList.get(0)) == false)) {
			showDefaultMap();
			return;
		}

		final PaintManager paintManager = PaintManager.getInstance();

		paintManager.setTourData(_tourDataList);

		final TourData firstTourData = _tourDataList.get(0);

		// set slider position
		_directMappingPainter.setPaintContext(
				_map,
				_actionShowTourInMap.isChecked(),
				firstTourData,
				_currentLeftSliderValueIndex,
				_currentRightSliderValueIndex,
				_actionShowSliderInMap.isChecked(),
				_actionShowSliderInLegend.isChecked());

		final Set<GeoPosition> tourBounds = getTourBounds(firstTourData);
		paintManager.setTourBounds(tourBounds);

		_map.setShowOverlays(_actionShowTourInMap.isChecked());

		setTourZoomLevel(tourBounds, false);

		_map.queueMapRedraw();
	}

	/**
	 * Paint the currently selected tour in the map
	 * 
	 * @param tourData
	 * @param forceRedraw
	 * @param isSynchronized
	 *            when <code>true</code>, map will be synchronized
	 */
	private void paintOneTour(final TourData tourData, final boolean forceRedraw, final boolean isSynchronized) {

		if (isPaintDataValid(tourData) == false) {
			showDefaultMap();
			return;
		}

		_isTour = true;
		final boolean isShowTour = _actionShowTourInMap.isChecked();

		// prevent loading the same tour
		if (forceRedraw == false) {

			if ((_tourDataList.size() == 1) && (_tourDataList.get(0) == tourData)) {
				return;
			}
		}

		// force multiple tours to be repainted
		_previousOverlayKey = -1;

		// check if this is a new tour
		boolean isNewTour = true;
		if ((_previousTourData != null)
				&& (_previousTourData.getTourId().longValue() == tourData.getTourId().longValue())) {
			isNewTour = false;
		}

		final PaintManager paintManager = PaintManager.getInstance();

		paintManager.setTourData(tourData);

		/*
		 * set tour into tour data list, this is currently used to draw the legend, it's also used
		 * to figure out if multiple tours are selected
		 */
		_tourDataList.clear();
		_tourDataList.add(tourData);

		// set the paint context (slider position) for the direct mapping painter
		_directMappingPainter.setPaintContext(
				_map,
				isShowTour,
				tourData,
				_currentLeftSliderValueIndex,
				_currentRightSliderValueIndex,
				_actionShowSliderInMap.isChecked(),
				_actionShowSliderInLegend.isChecked());

		// set the tour bounds
		final Set<GeoPosition> tourBounds = getTourBounds(tourData);
		paintManager.setTourBounds(tourBounds);

		_map.setShowOverlays(isShowTour);
		_map.setShowLegend(isShowTour && _actionShowLegendInMap.isChecked());

		// set position and zoom level for the tour
		if (_isMapSynchedWithTour && isSynchronized) {

			if (((forceRedraw == false) && (_previousTourData != null)) || (tourData == _previousTourData)) {

				/*
				 * keep map configuration for the previous tour
				 */
				_previousTourData.mapZoomLevel = _map.getZoom();

				final GeoPosition centerPosition = _map.getGeoCenter();
				_previousTourData.mapCenterPositionLatitude = centerPosition.latitude;
				_previousTourData.mapCenterPositionLongitude = centerPosition.longitude;
			}

			if (tourData.mapCenterPositionLatitude == Double.MIN_VALUE) {

				// use default position for the tour
				setTourZoomLevel(tourBounds, true);

			} else {

				// position tour to the previous position
				_map.setZoom(tourData.mapZoomLevel);
				_map.setGeoCenterPosition(new GeoPosition(
						tourData.mapCenterPositionLatitude,
						tourData.mapCenterPositionLongitude));
			}
		}

		// keep tour data
		_previousTourData = tourData;

		if (isNewTour || forceRedraw) {

			// adjust legend values for the new or changed tour
			createLegendImage(PaintManager.getInstance().getLegendProvider());

			_map.setOverlayKey(tourData.getTourId().toString());
			_map.disposeOverlayImageCache();

		}

		_map.queueMapRedraw();
	}

	/**
	 * paints the tours which are set in {@link #_tourDataList}
	 */
	private void paintTours() {

		_isTour = true;

		// force single tour to be repainted
		_previousTourData = null;

		PaintManager.getInstance().setTourData(_tourDataList);

		_directMappingPainter.disablePaintContext();

		final boolean isShowTour = _actionShowTourInMap.isChecked();
		_map.setShowOverlays(isShowTour);
		_map.setShowLegend(isShowTour && _actionShowLegendInMap.isChecked());

		// get overlay key for all tours which have valid tour data
		long newOverlayKey = -1;
		for (final TourData tourData : _tourDataList) {

			if (isPaintDataValid(tourData)) {
				newOverlayKey += tourData.getTourId();
			}
		}

		if (_previousOverlayKey != newOverlayKey) {

			_previousOverlayKey = newOverlayKey;

			_map.setOverlayKey(Long.toString(newOverlayKey));
			_map.disposeOverlayImageCache();
		}

		createLegendImage(PaintManager.getInstance().getLegendProvider());
		_map.queueMapRedraw();
	}

	private void paintTours(final ArrayList<Long> tourIdList) {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			@Override
			public void run() {

				_isTour = true;

				// force single tour to be repainted
				_previousTourData = null;

				_directMappingPainter.disablePaintContext();

				final boolean isShowTour = _actionShowTourInMap.isChecked();
				_map.setShowOverlays(isShowTour);
				_map.setShowLegend(isShowTour && _actionShowLegendInMap.isChecked());

				/*
				 * create a unique overlay key for the selected tours
				 */
				long newOverlayKey = 0;
				_tourDataList.clear();
				for (final Long tourId : tourIdList) {

					final TourData tourData = TourManager.getInstance().getTourData(tourId);
					if (isPaintDataValid(tourData)) {
						// keep tour data for each tour id
						_tourDataList.add(tourData);
						newOverlayKey += tourData.getTourId();
					}
				}
				PaintManager.getInstance().setTourData(_tourDataList);

				if (_previousOverlayKey != newOverlayKey) {

					_previousOverlayKey = newOverlayKey;

					_map.setOverlayKey(Long.toString(newOverlayKey));
					_map.disposeOverlayImageCache();
				}

				createLegendImage(PaintManager.getInstance().getLegendProvider());
				_map.queueMapRedraw();
			}
		});
	}

	private void paintTourSliders(	final TourData tourData,
									final int leftSliderValuesIndex,
									final int rightSliderValuesIndex,
									final int selectedSliderIndex) {

		if (isPaintDataValid(tourData) == false) {
			showDefaultMap();
			return;
		}

		_isTour = true;
		_currentLeftSliderValueIndex = leftSliderValuesIndex;
		_currentRightSliderValueIndex = rightSliderValuesIndex;
		_currentSelectedSliderValueIndex = selectedSliderIndex;

		_directMappingPainter.setPaintContext(
				_map,
				_actionShowTourInMap.isChecked(),
				tourData,
				leftSliderValuesIndex,
				rightSliderValuesIndex,
				_actionShowSliderInMap.isChecked(),
				_actionShowSliderInLegend.isChecked());

		if (_isMapSynchedWithSlider) {

			setMapToSliderBounds(tourData);

			_map.queueMapRedraw();

		} else {

			_map.redraw();
		}
	}

	private void resetMap() {

		if (_tourDataList.size() == 0) {
			return;
		}

		_map.disposeOverlayImageCache();

		paintAllTours();

		_map.queueMapRedraw();
	}

	private void restoreState() {

		final PaintManager paintManager = PaintManager.getInstance();
		final IDialogSettings settings = TourbookPlugin.getDefault().getDialogSettingsSection(ID);
		String state = null;

		// checkbox: is tour centered
		final boolean isTourCentered = settings.getBoolean(MEMENTO_ZOOM_CENTERED);
		_actionZoomCentered.setChecked(isTourCentered);
		_isPositionCentered = isTourCentered;

		// checkbox: synch map with tour
		final boolean isSynchTour = Util.getStateBoolean(settings, MEMENTO_SYNCH_WITH_SELECTED_TOUR, true);
		_actionSynchWithTour.setChecked(isSynchTour);
		_isMapSynchedWithTour = isSynchTour;

		// ckeckbox: synch with tour chart slider
		final boolean isSynchSlider = settings.getBoolean(MEMENTO_SYNCH_WITH_TOURCHART_SLIDER);
		_actionSynchWithSlider.setChecked(isSynchSlider);
		_isMapSynchedWithSlider = isSynchSlider;

		// checkbox: show tour in map
		final boolean isShowTour = Util.getStateBoolean(settings, MEMENTO_SHOW_TOUR_IN_MAP, true);
		_actionShowTourInMap.setChecked(isShowTour);
		_map.setShowOverlays(isShowTour);
		_map.setShowLegend(isShowTour);

		//
		_actionSynchTourZoomLevel.setZoomLevel(Util.getStateInt(settings, MEMENTO_SYNCH_TOUR_ZOOM_LEVEL, 0));
		_mapDimLevel = Util.getStateInt(settings, MEMENTO_MAP_DIM_LEVEL, -1);

		// checkbox: show start/end in map
		_actionShowStartEndInMap.setChecked(settings.getBoolean(MEMENTO_SHOW_START_END_IN_MAP));
		paintManager.setShowStartEnd(_actionShowStartEndInMap.isChecked());

		// checkbox: show tour marker
		state = settings.get(MEMENTO_SHOW_TOUR_MARKER);
		_actionShowTourMarker.setChecked(state == null ? true : settings.getBoolean(MEMENTO_SHOW_TOUR_MARKER));
		paintManager.setShowTourMarker(_actionShowTourMarker.isChecked());

		// checkbox: show legend in map
		_actionShowLegendInMap.setChecked(Util.getStateBoolean(settings, MEMENTO_SHOW_LEGEND_IN_MAP, true));

		// checkbox: show scale
		final boolean isScaleVisible = Util.getStateBoolean(settings, MEMENTO_SHOW_SCALE_IN_MAP, true);
		_actionShowScaleInMap.setChecked(isScaleVisible);
		_map.setShowScale(isScaleVisible);

		// other actions
		state = settings.get(MEMENTO_SHOW_SLIDER_IN_MAP);
		_actionShowSliderInMap.setChecked(state == null ? true : settings.getBoolean(MEMENTO_SHOW_SLIDER_IN_MAP));

		_actionShowSliderInLegend.setChecked(settings.getBoolean(MEMENTO_SHOW_SLIDER_IN_LEGEND));

		// restore map factory by selecting the last used map factory
		_actionSelectMapProvider.selectMapProvider(settings.get(MEMENTO_SELECTED_MAP_PROVIDER_ID));

		// default position
		_defaultZoom = Util.getStateInt(settings, MEMENTO_DEFAULT_POSITION_ZOOM, 10);
		_defaultPosition = new GeoPosition(//
				Util.getStateDouble(settings, MEMENTO_DEFAULT_POSITION_LATITUDE, 46.303074),
				Util.getStateDouble(settings, MEMENTO_DEFAULT_POSITION_LONGITUDE, 7.526386));

		// tour color
		try {
			final Integer colorId = settings.getInt(MEMENTO_TOUR_COLOR_ID);

			switch (colorId) {
			case TOUR_COLOR_ALTITUDE:
				_actionTourColorAltitude.setChecked(true);
				break;

			case TOUR_COLOR_GRADIENT:
				_actionTourColorGradient.setChecked(true);
				break;

			case TOUR_COLOR_PULSE:
				_actionTourColorPulse.setChecked(true);
				break;

			case TOUR_COLOR_SPEED:
				_actionTourColorSpeed.setChecked(true);
				break;

			case TOUR_COLOR_PACE:
				_actionTourColorPace.setChecked(true);
				break;

//			case TOUR_COLOR_TOURTYPE:
//				_actionTourColorTourType.setChecked(true);
//				break;

			default:
				_actionTourColorAltitude.setChecked(true);
				break;
			}

			paintManager.setLegendProvider(getLegendProvider(colorId));

		} catch (final NumberFormatException e) {
			_actionTourColorAltitude.setChecked(true);
		}

		// draw tour with default color

		// check legend provider
		final ILegendProvider legendProvider = paintManager.getLegendProvider();
		if (legendProvider == null) {

			// set default legend provider
			paintManager.setLegendProvider(getLegendProvider(TOUR_COLOR_ALTITUDE));

			// hide legend
			_map.setShowLegend(false);
		}

		// debug info
		final boolean isShowTileInfo = _prefStore.getBoolean(TourMapView.PREF_SHOW_TILE_INFO);
		_map.setShowDebugInfo(isShowTileInfo);

		// set dim level/color after the map providers are set
		if (_mapDimLevel == -1) {
			_mapDimLevel = 0xff;
		}
		final RGB dimColor = PreferenceConverter.getColor(_prefStore, ITourbookPreferences.MAP_LAYOUT_DIM_COLOR);
		_map.setDimLevel(_mapDimLevel, dimColor);
		_mapDimLevel = _actionDimMap.setDimLevel(_mapDimLevel);

		// display the map with the default position
		actionSetDefaultPosition();
	}

	private void saveState() {

		final IDialogSettings settings = TourbookPlugin.getDefault().getDialogSettingsSection(ID);

		// save checked actions
		settings.put(MEMENTO_ZOOM_CENTERED, _actionZoomCentered.isChecked());
		settings.put(MEMENTO_SHOW_TOUR_IN_MAP, _actionShowTourInMap.isChecked());
		settings.put(MEMENTO_SYNCH_WITH_SELECTED_TOUR, _actionSynchWithTour.isChecked());
		settings.put(MEMENTO_SYNCH_WITH_TOURCHART_SLIDER, _actionSynchWithSlider.isChecked());
		settings.put(MEMENTO_SYNCH_TOUR_ZOOM_LEVEL, _actionSynchTourZoomLevel.getZoomLevel());

		settings.put(MEMENTO_MAP_DIM_LEVEL, _mapDimLevel);

		settings.put(MEMENTO_SHOW_START_END_IN_MAP, _actionShowStartEndInMap.isChecked());
		settings.put(MEMENTO_SHOW_TOUR_MARKER, _actionShowTourMarker.isChecked());
		settings.put(MEMENTO_SHOW_LEGEND_IN_MAP, _actionShowLegendInMap.isChecked());
		settings.put(MEMENTO_SHOW_SCALE_IN_MAP, _actionShowScaleInMap.isChecked());
		settings.put(MEMENTO_SHOW_SLIDER_IN_MAP, _actionShowSliderInMap.isChecked());
		settings.put(MEMENTO_SHOW_SLIDER_IN_LEGEND, _actionShowSliderInLegend.isChecked());

		settings.put(MEMENTO_SELECTED_MAP_PROVIDER_ID, _actionSelectMapProvider.getSelectedMapProvider().getId());

		if (_defaultPosition == null) {
			settings.put(MEMENTO_DEFAULT_POSITION_ZOOM, _map.getMapProvider().getMinimumZoomLevel());
			settings.put(MEMENTO_DEFAULT_POSITION_LATITUDE, 0.0F);
			settings.put(MEMENTO_DEFAULT_POSITION_LONGITUDE, 0.0F);
		} else {
			settings.put(MEMENTO_DEFAULT_POSITION_ZOOM, _defaultZoom);
			settings.put(MEMENTO_DEFAULT_POSITION_LATITUDE, (float) _defaultPosition.latitude);
			settings.put(MEMENTO_DEFAULT_POSITION_LONGITUDE, (float) _defaultPosition.longitude);
		}

		// tour color
		int colorId;

		if (_actionTourColorGradient.isChecked()) {
			colorId = TOUR_COLOR_GRADIENT;
		} else if (_actionTourColorPulse.isChecked()) {
			colorId = TOUR_COLOR_PULSE;
		} else if (_actionTourColorSpeed.isChecked()) {
			colorId = TOUR_COLOR_SPEED;
		} else if (_actionTourColorPace.isChecked()) {
			colorId = TOUR_COLOR_PACE;
//		} else if (_actionTourColorTourType.isChecked()) {
//			colorId = TOUR_COLOR_TOURTYPE;
		} else {
			colorId = TOUR_COLOR_ALTITUDE;
		}
		settings.put(MEMENTO_TOUR_COLOR_ID, colorId);
	}

	@Override
	public void setFocus() {
		_map.setFocus();
	}

	/**
	 * Calculate the bounds for the tour in latitude and longitude values
	 * 
	 * @param tourData
	 * @return
	 */
	private void setMapToSliderBounds(final TourData tourData) {

		if (tourData == null) {
			return;
		}

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

//		final double leftSliderLat = latitudeSerie[fCurrentLeftSliderValueIndex];
//		final double leftSliderLong = longitudeSerie[fCurrentLeftSliderValueIndex];
//
//		final double rightSliderLat = latitudeSerie[fCurrentRightSliderValueIndex];
//		final double rightSliderLong = longitudeSerie[fCurrentRightSliderValueIndex];
//
//		final double minLatitude = Math.min(leftSliderLat + 0, rightSliderLat + 0);
//		final double minLongitude = Math.min(leftSliderLong + 0, rightSliderLong + 0);
//
//		final double maxLatitude = Math.max(leftSliderLat + 0, rightSliderLat + 0);
//		final double maxLongitude = Math.max(leftSliderLong + 0, rightSliderLong + 0);
//
//		final double latDiff2 = (maxLatitude - minLatitude) / 2;
//		final double longDiff2 = (maxLongitude - minLongitude) / 2;
//
//		final double sliderLat = minLatitude + latDiff2 - 0;
//		final double sliderLong = minLongitude + longDiff2 - 0;

//		_map.setCenterPosition(new GeoPosition(sliderLat, sliderLong));
//		_map.setCenterPosition(new GeoPosition(sliderLat, leftSliderLong));
//		_map.setCenterPosition(new GeoPosition(leftSliderLat, leftSliderLong));

		final int sliderIndex = Math.max(0, Math.min(_currentSelectedSliderValueIndex, latitudeSerie.length - 1));

		_map.setGeoCenterPosition(new GeoPosition(latitudeSerie[sliderIndex], longitudeSerie[sliderIndex]));

	}

	/**
	 * Calculates a zoom level so that all points in the specified set will be visible on screen.
	 * This is useful if you have a bunch of points in an area like a city and you want to zoom out
	 * so that the entire city and it's points are visible without panning.
	 * 
	 * @param positions
	 *            A set of GeoPositions to calculate the new zoom from
	 * @param adjustZoomLevel
	 *            when <code>true</code> the zoom level will be adjusted to user settings
	 */
	private void setTourZoomLevel(final Set<GeoPosition> positions, final boolean isAdjustZoomLevel) {

		if ((positions == null) || (positions.size() < 2)) {
			return;
		}

		final MP mp = _map.getMapProvider();

		final int maximumZoomLevel = mp.getMaximumZoomLevel();
		int zoom = mp.getMinimumZoomLevel();

		Rectangle positionRect = getPositionRect(positions, zoom);
		java.awt.Rectangle viewport = _map.getMapPixelViewport();

//		// zoom until the tour is visible in the map
//		while (!viewport.contains(positionRect)) {
//
//			// center position in the map
//			final Point center = new Point(//
//					positionRect.x + positionRect.width / 2,
//					positionRect.y + positionRect.height / 2);
//
//			_map.setGeoCenterPosition(mp.pixelToGeo(center, zoom));
//
//			zoom++;
//
//			// check zoom level
//			if (zoom >= maximumZoomLevel) {
//				break;
//			}
//			_map.setZoom(zoom);
//
//			positionRect = getPositionRect(positions, zoom);
//			viewport = _map.getMapPixelViewport();
//		}

		// zoom in until the tour is larger than the viewport
		while ((positionRect.width < viewport.width) && (positionRect.height < viewport.height)) {

			// center position in the map
			final Point center = new Point(//
					positionRect.x + positionRect.width / 2,
					positionRect.y + positionRect.height / 2);

			_map.setGeoCenterPosition(mp.pixelToGeo(center, zoom));

			zoom++;

			// check zoom level
			if (zoom >= maximumZoomLevel) {
				break;
			}
			_map.setZoom(zoom);

			positionRect = getPositionRect(positions, zoom);
			viewport = _map.getMapPixelViewport();
		}

		// the algorithm generated a larger zoom level as necessary
		zoom--;

		int adjustedZoomLevel = 0;
		if (isAdjustZoomLevel) {
			adjustedZoomLevel = PaintManager.getInstance().getSynchTourZoomLevel();
		}

		_map.setZoom(zoom + adjustedZoomLevel);
	}

	private void showDefaultMap() {

		// disable tour actions in this view
		_isTour = false;

		// disable tour data
		_tourDataList.clear();
		_previousTourData = null;

		// update direct painter to draw nothing
		_directMappingPainter.setPaintContext(_map, false, null, 0, 0, false, false);

		_map.setShowOverlays(false);
		_map.setShowLegend(false);

		_map.queueMapRedraw();
	}

	/**
	 * show warning that map is dimmed and can be invisible
	 */
	private void showDimWarning() {

		if (_prefStore.getBoolean(ITourbookPreferences.MAP_VIEW_CONFIRMATION_SHOW_DIM_WARNING) == false) {

			Display.getCurrent().asyncExec(new Runnable() {
				@Override
				public void run() {

					final MessageDialogWithToggle dialog = MessageDialogWithToggle.openInformation(Display
							.getCurrent()
							.getActiveShell(),//
							Messages.map_dlg_dim_warning_title, // title
							Messages.map_dlg_dim_warning_message, // message
							Messages.map_dlg_dim_warning_toggle_message, // toggle message
							false, // toggle default state
							null,
							null);

					_prefStore.setValue(ITourbookPreferences.MAP_VIEW_CONFIRMATION_SHOW_DIM_WARNING, dialog
							.getToggleState());
				}
			});
		}
	}

	private void showToursFromTourProvider() {

		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {

				// validate widget
				if (_map.isDisposed()) {
					return;
				}

				/*
				 * check if tour is set from a selection provider
				 */
				if (_tourDataList.size() > 0) {
					return;
				}

				final ArrayList<TourData> tourDataList = TourManager.getSelectedTours();
				if (tourDataList != null) {

//					final TourData tourData = TourManager.getInstance().getTourData(tourDataList);
//					if (tourData != null) {

					_tourDataList.clear();
					_tourDataList.addAll(tourDataList);

					paintAllTours();

//						/*
//						 * set position and zoomlevel to show the entire tour
//						 */
//						final PaintManager paintManager = PaintManager.getInstance();
//						final Set<GeoPosition> tourBounds = getTourBounds(tourData);
//
//						paintManager.setTourBounds(tourBounds);
//						setTourZoomLevel(tourBounds, true);
//
//						paintOneTour(tourData, true, false);
//
//						enableActions();
//					}
				}
			}
		});
	}

	/**
	 * Update the min/max values in the {@link ILegendProvider} for the currently displayed legend
	 * 
	 * @param legendProvider
	 * @param legendBounds
	 * @return Return <code>true</code> when the legend value could be updated, <code>false</code>
	 *         when data are not available
	 */
	private boolean updateLegendValues(final ILegendProvider legendProvider, final Rectangle legendBounds) {

		if (_tourDataList.size() == 0) {
			return false;
		}

		final GraphColorProvider colorProvider = GraphColorProvider.getInstance();

		ColorDefinition colorDefinition = null;
		final LegendConfig legendConfig = legendProvider.getLegendConfig();

		// tell the legend provider how to draw the legend
		switch (legendProvider.getTourColorId()) {

		case TOUR_COLOR_ALTITUDE:

			int minValue = Integer.MIN_VALUE;
			int maxValue = Integer.MAX_VALUE;
			boolean setInitialValue = true;

			for (final TourData tourData : _tourDataList) {

				final int[] dataSerie = tourData.getAltitudeSerie();
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final int dataValue : dataSerie) {

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataValue;
					}

					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if ((minValue == Integer.MIN_VALUE) || (maxValue == Integer.MAX_VALUE)) {
				return false;
			}

			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_ALTITUDE);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(legendBounds, minValue, maxValue, UI.UNIT_LABEL_ALTITUDE);

			break;

		case TOUR_COLOR_PULSE:

			minValue = Integer.MIN_VALUE;
			maxValue = Integer.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : _tourDataList) {

				final int[] dataSerie = tourData.pulseSerie;
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final int dataValue : dataSerie) {

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataValue;
					}

					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if ((minValue == Integer.MIN_VALUE) || (maxValue == Integer.MAX_VALUE)) {
				return false;
			}

			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_HEARTBEAT);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(legendBounds, minValue, maxValue, Messages.graph_label_heartbeat_unit);

			break;

		case TOUR_COLOR_SPEED:

			minValue = Integer.MIN_VALUE;
			maxValue = Integer.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : _tourDataList) {

				final int[] dataSerie = tourData.getSpeedSerie();
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final int dataValue : dataSerie) {

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataValue;
					}

					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if ((minValue == Integer.MIN_VALUE) || (maxValue == Integer.MAX_VALUE)) {
				return false;
			}

			legendConfig.unitFactor = 10;
			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_SPEED);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(legendBounds, minValue, maxValue, UI.UNIT_LABEL_SPEED);

			break;

		case TOUR_COLOR_PACE:

			minValue = Integer.MIN_VALUE;
			maxValue = Integer.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : _tourDataList) {

				final int[] dataSerie = tourData.getPaceSerie();
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final int dataValue : dataSerie) {

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataValue;
					}

					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if ((minValue == Integer.MIN_VALUE) || (maxValue == Integer.MAX_VALUE)) {
				return false;
			}

			legendConfig.unitFactor = 10;
			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_PACE);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(legendBounds, minValue, maxValue, UI.UNIT_LABEL_PACE);

			break;

		case TOUR_COLOR_GRADIENT:

			minValue = Integer.MIN_VALUE;
			maxValue = Integer.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : _tourDataList) {

				final int[] dataSerie = tourData.getGradientSerie();
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final int dataValue : dataSerie) {

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataValue;
					}

					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if ((minValue == Integer.MIN_VALUE) || (maxValue == Integer.MAX_VALUE)) {
				return false;
			}

			legendConfig.unitFactor = 10;
			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_GRADIENT);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(legendBounds, minValue, maxValue, Messages.graph_label_gradient_unit);

			break;

//		case TOUR_COLOR_TOURTYPE:
//			return false;

		default:
			break;
		}

		return true;
	}
}
