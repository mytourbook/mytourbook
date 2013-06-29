/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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

import java.awt.Point;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorProvider;
import net.tourbook.common.color.ILegendProvider;
import net.tourbook.common.color.LegendConfig;
import net.tourbook.common.color.LegendUnitFormat;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.util.ITourToolTipProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourWayPoint;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.photo.IPhotoEventListener;
import net.tourbook.photo.IPhotoPropertiesListener;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoEventId;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.PhotoProperties;
import net.tourbook.photo.PhotoPropertiesEvent;
import net.tourbook.photo.PhotoSelection;
import net.tourbook.photo.TourPhotoLink;
import net.tourbook.photo.TourPhotoLinkSelection;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceMap;
import net.tourbook.srtm.IPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourInfoToolTipProvider;
import net.tourbook.tour.TourManager;
import net.tourbook.training.TrainingManager;
import net.tourbook.ui.MTRectangle;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
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
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
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
import de.byteholder.gpx.PointOfInterest;

/**
 * @author Wolfgang Schramm
 * @since 1.3.0
 */
public class TourMapView extends ViewPart implements IMapContextProvider, IPhotoEventListener, IPhotoPropertiesListener {

	public static final String				ID									= "net.tourbook.mapping.mappingViewID"; //$NON-NLS-1$

	private static final int				DEFAULT_LEGEND_WIDTH				= 150;
	private static final int				DEFAULT_LEGEND_HEIGHT				= 300;
	private static final int				LEGEND_TOP_MARGIN					= 20;

	public static final int					LEGEND_MARGIN_TOP_BOTTOM			= 10;
	public static final int					LEGEND_UNIT_DISTANCE				= 60;

	private static final String				STATE_IS_SHOW_TOUR_IN_MAP			= "STATE_IS_SHOW_TOUR_IN_MAP";			//$NON-NLS-1$
	private static final String				STATE_IS_SHOW_PHOTO_IN_MAP			= "STATE_IS_SHOW_PHOTO_IN_MAP";		//$NON-NLS-1$
	private static final String				STATE_IS_SHOW_LEGEND_IN_MAP			= "STATE_IS_SHOW_LEGEND_IN_MAP";		//$NON-NLS-1$
	private static final String				STATE_SYNC_WITH_PHOTO				= "STATE_SYNC_WITH_PHOTO";				//$NON-NLS-1$
	private static final String				MEMENTO_SHOW_START_END_IN_MAP		= "action.show-start-end-in-map";		//$NON-NLS-1$
	private static final String				MEMENTO_SHOW_TOUR_MARKER			= "action.show-tour-marker";			//$NON-NLS-1$
	private static final String				MEMENTO_SHOW_SLIDER_IN_MAP			= "action.show-slider-in-map";			//$NON-NLS-1$
	private static final String				MEMENTO_SHOW_SLIDER_IN_LEGEND		= "action.show-slider-in-legend";		//$NON-NLS-1$
	private static final String				MEMENTO_SHOW_SCALE_IN_MAP			= "action.show-scale-in-map";			//$NON-NLS-1$
	private static final String				MEMENTO_SHOW_TOUR_INFO_IN_MAP		= "action.show-tour-info-in-map";		//$NON-NLS-1$
	private static final String				MEMENTO_SHOW_WAY_POINTS				= "action.show-way-points-in-map";		//$NON-NLS-1$
	private static final String				MEMENTO_SYNCH_WITH_SELECTED_TOUR	= "action.synch-with-selected-tour";	//$NON-NLS-1$
	private static final String				MEMENTO_SYNCH_WITH_TOURCHART_SLIDER	= "action.synch-with-tourchart-slider"; //$NON-NLS-1$
	private static final String				MEMENTO_ZOOM_CENTERED				= "action.zoom-centered";				//$NON-NLS-1$
	private static final String				MEMENTO_MAP_DIM_LEVEL				= "action.map-dim-level";				//$NON-NLS-1$

	private static final String				MEMENTO_SYNCH_TOUR_ZOOM_LEVEL		= "synch-tour-zoom-level";				//$NON-NLS-1$
	private static final String				MEMENTO_SELECTED_MAP_PROVIDER_ID	= "selected.map-provider-id";			//$NON-NLS-1$

	private static final String				MEMENTO_DEFAULT_POSITION_ZOOM		= "default.position.zoom-level";		//$NON-NLS-1$
	private static final String				MEMENTO_DEFAULT_POSITION_LATITUDE	= "default.position.latitude";			//$NON-NLS-1$
	private static final String				MEMENTO_DEFAULT_POSITION_LONGITUDE	= "default.position.longitude";		//$NON-NLS-1$

	private static final String				MEMENTO_TOUR_COLOR_ID				= "tour-color-id";						//$NON-NLS-1$

	static final String						PREF_SHOW_TILE_INFO					= "MapDebug.ShowTileInfo";				//$NON-NLS-1$
	static final String						PREF_SHOW_TILE_BORDER				= "MapDebug.ShowTileBorder";			//$NON-NLS-1$
	static final String						PREF_DEBUG_MAP_DIM_LEVEL			= "MapDebug.MapDimLevel";				//$NON-NLS-1$

	private final IPreferenceStore			_prefStore							= TourbookPlugin
																						.getDefault()
																						.getPreferenceStore();
	private final IDialogSettings			_state								= TourbookPlugin
																						.getDefault()
																						.getDialogSettingsSection(ID);

	private boolean							_isPartVisible;

	/**
	 * contains selection which was set when the part is hidden
	 */
	private ISelection						_selectionWhenHidden;

	private IPartListener2					_partListener;
	private ISelectionListener				_postSelectionListener;
	private IPropertyChangeListener			_prefChangeListener;
	private ITourEventListener				_tourEventListener;

	/**
	 * Contains all tours which are displayed in the map.
	 */
	private final ArrayList<TourData>		_allTourData						= new ArrayList<TourData>();
	private TourData						_previousTourData;

	/**
	 * contains photos which are displayed in the map
	 */
	private final ArrayList<Photo>			_allPhotos							= new ArrayList<Photo>();
	private final ArrayList<Photo>			_filteredPhotos						= new ArrayList<Photo>();

	private boolean							_isPhotoFilterActive;

	private int								_photoFilterRatingStars;
	private int								_photoFilterRatingStarOperator;

	private boolean							_isShowTour;
	private boolean							_isShowPhoto;
	private boolean							_isShowLegend;

	private boolean							_isMapSynchedWithPhoto;
	private boolean							_isMapSynchedWithTour;
	private boolean							_isMapSynchedWithSlider;
	private boolean							_isPositionCentered;

	private int								_defaultZoom;
	private GeoPosition						_defaultPosition					= null;

	/**
	 * when <code>true</code> a tour is painted, <code>false</code> a point of interrest is painted
	 */
	private boolean							_isTourOrWayPoint;

	/*
	 * tool tips
	 */
	private TourToolTip						_tourToolTip;

	private TourInfoToolTipProvider			_tourInfoToolTipProvider			= new TourInfoToolTipProvider();
	private ITourToolTipProvider			_wayPointToolTipProvider			= new WayPointToolTipProvider();

	private String							_poiName;
	private GeoPosition						_poiPosition;
	private int								_poiZoomLevel;

	private final DirectMappingPainter		_directMappingPainter				= new DirectMappingPainter();

	/*
	 * current position for the x-sliders
	 */
	private int								_currentLeftSliderValueIndex;
	private int								_currentRightSliderValueIndex;
	private int								_currentSelectedSliderValueIndex;

	private MapLegend						_mapLegend;

	private long							_previousOverlayKey;

	private int								_mapDimLevel						= -1;
	private RGB								_mapDimColor;

	private int								_selectedProfileKey					= 0;

	private final MapInfoManager			_mapInfoManager						= MapInfoManager.getInstance();
	private final TourPainterConfiguration	_tourPainterConfig					= TourPainterConfiguration
																						.getInstance();
	private int								_tourIdHash;
	private int								_tourDataHash;
	private long							_tourHashOverlayKey;

	/**
	 * Is <code>true</code> when a link photo is displayed, otherwise a tour photo (photo which is
	 * save in a tour) is displayed.
	 */
	private boolean							_isLinkPhotoDisplayed;

	/*
	 * UI controls
	 */
	private Map								_map;

	private ActionTourColor					_actionTourColorAltitude;
	private ActionTourColor					_actionTourColorGradient;
	private ActionTourColor					_actionTourColorPulse;
	private ActionTourColor					_actionTourColorSpeed;
	private ActionTourColor					_actionTourColorPace;
	private ActionTourColor					_actionTourColorHrZone;

	private ActionDimMap					_actionDimMap;
	private ActionManageMapProviders		_actionManageProvider;
	private ActionPhotoProperties			_actionPhotoFilter;
	private ActionReloadFailedMapImages		_actionReloadFailedMapImages;
	private ActionSaveDefaultPosition		_actionSaveDefaultPosition;
	private ActionSelectMapProvider			_actionSelectMapProvider;
	private ActionSetDefaultPosition		_actionSetDefaultPosition;
	private ActionShowAllFilteredPhotos		_actionShowAllFilteredPhotos;
	private ActionShowLegendInMap			_actionShowLegendInMap;
	private ActionShowPhotos				_actionShowPhotos;
	private ActionShowPOI					_actionShowPOI;
	private ActionShowScaleInMap			_actionShowScaleInMap;
	private ActionShowSliderInMap			_actionShowSliderInMap;
	private ActionShowSliderInLegend		_actionShowSliderInLegend;
	private ActionShowStartEndInMap			_actionShowStartEndInMap;
	private ActionShowTourInMap				_actionShowTourInMap;
	private ActionShowTourInfoInMap			_actionShowTourInfoInMap;
	private ActionShowTourMarker			_actionShowTourMarker;
	private ActionShowWayPoints				_actionShowWayPoints;
	private ActionSynchWithPhoto			_actionSynchWithPhoto;
	private ActionSynchWithSlider			_actionSynchWithSlider;
	private ActionSynchWithTour				_actionSynchWithTour;
	private ActionSynchTourZoomLevel		_actionSynchTourZoomLevel;

	private ActionZoomIn					_actionZoomIn;
	private ActionZoomOut					_actionZoomOut;
	private ActionZoomCentered				_actionZoomCentered;
	private ActionZoomShowEntireEarth		_actionZoomShowAll;
	private ActionZoomShowEntireTour		_actionShowEntireTour;

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

		final DialogModifyMapProvider dialog = new DialogModifyMapProvider(Display.getCurrent().getActiveShell());

		if (dialog.open() == Window.OK) {
			_actionSelectMapProvider.updateMapProviders();
		}
	}

	void actionPhotoProperties(final boolean isFilterActive) {

		_isPhotoFilterActive = isFilterActive;

		updateFilteredPhotos();
	}

	void actionPOI() {

		final boolean isShowPOI = _actionShowPOI.isChecked();

		_map.setShowPOI(isShowPOI);

		if (isShowPOI) {
			_map.setPoi(_poiPosition, _map.getZoom(), _poiName);
		}
	}

	void actionReloadFailedMapImages() {
		_map.deleteFailedImageFiles();
		_map.resetAll();
	}

	void actionSaveDefaultPosition() {
		_defaultZoom = _map.getZoom();
		_defaultPosition = _map.getGeoCenter();
	}

	void actionSetDefaultPosition() {
		if (_defaultPosition == null) {
			_map.setZoom(_map.getMapProvider().getMinimumZoomLevel());
			_map.setMapCenter(new GeoPosition(0, 0));
		} else {
			_map.setZoom(_defaultZoom);
			_map.setMapCenter(_defaultPosition);
		}
		_map.paint();
	}

	void actionSetShowScaleInMap() {

		final boolean isScaleVisible = _actionShowScaleInMap.isChecked();

		_map.setShowScale(isScaleVisible);
		_map.paint();
	}

	void actionSetShowStartEndInMap() {

		_tourPainterConfig.isShowStartEndInMap = _actionShowStartEndInMap.isChecked();

		_map.disposeOverlayImageCache();
		_map.paint();
	}

	void actionSetShowTourInfoInMap() {

		final boolean isVisible = _actionShowTourInfoInMap.isChecked();

		if (isVisible) {
			_tourToolTip.addToolTipProvider(_tourInfoToolTipProvider);
		} else {
			_tourToolTip.removeToolTipProvider(_tourInfoToolTipProvider);
		}

		_map.paint();
	}

	void actionSetShowTourMarkerInMap() {

		_tourPainterConfig.isShowTourMarker = _actionShowTourMarker.isChecked();

		_map.disposeOverlayImageCache();
		_map.paint();
	}

	void actionSetShowWayPointsInMap() {

		final boolean isShowWayPoints = _actionShowWayPoints.isChecked();
		if (isShowWayPoints) {
			_tourToolTip.addToolTipProvider(_wayPointToolTipProvider);
		} else {
			_tourToolTip.removeToolTipProvider(_wayPointToolTipProvider);
		}

		_tourPainterConfig.isShowWayPoints = isShowWayPoints;

		_map.disposeOverlayImageCache();
		_map.paint();
	}

	void actionSetTourColor(final int colorId) {

		final ILegendProvider legendProvider = getLegendProvider(colorId);

		_tourPainterConfig.setLegendProvider(legendProvider);

		_map.disposeOverlayImageCache();
		_map.paint();

		createLegendImage(legendProvider);
	}

	void actionSetZoomCentered() {
		_isPositionCentered = _actionZoomCentered.isChecked();
	}

	void actionShowLegend() {

		_isShowLegend = _actionShowLegendInMap.isChecked();

		_map.setShowLegend(_isShowLegend);

		_actionShowSliderInLegend.setEnabled(_isShowLegend);
		if (_isShowLegend == false) {
			_actionShowSliderInLegend.setChecked(false);
		}

		// update legend
		actionShowSlider();

		_map.paint();
	}

	void actionShowPhotos() {

		_isShowPhoto = _actionShowPhotos.isChecked();

		enableActions();

		_tourPainterConfig.isPhotoVisible = _isShowPhoto;

		_map.setOverlayKey(Integer.toString(_filteredPhotos.hashCode()));
		_map.disposeOverlayImageCache();

		_map.paint();
	}

	void actionShowSlider() {

		if ((_allTourData == null) || (_allTourData.size() == 0)) {
			return;
		}

		// repaint map
		_directMappingPainter.setPaintContext(
				_map,
				_isShowTour,
				_allTourData.get(0),
				_currentLeftSliderValueIndex,
				_currentRightSliderValueIndex,
				_actionShowSliderInMap.isChecked(),
				_actionShowSliderInLegend.isChecked());

		_map.redraw();
	}

	void actionShowTour() {

		_isShowTour = _actionShowTourInMap.isChecked();

		paintTours_10_All();
	}

	/**
	 * Sync map with photo
	 */
	void actionSynchWithPhoto() {

		_isMapSynchedWithPhoto = _actionSynchWithPhoto.isChecked();

		if (_isMapSynchedWithPhoto) {

			centerPhotos(_filteredPhotos, false);

			_map.paint();
		}

		enableActions();
	}

	void actionSynchWithSlider() {

		if (_allTourData.size() == 0) {
			return;
		}

		_isMapSynchedWithSlider = _actionSynchWithSlider.isChecked();

		if (_isMapSynchedWithSlider) {

			_actionShowTourInMap.setChecked(true);

			// map must be synched with selected tour
			_actionSynchWithTour.setChecked(true);
			_isMapSynchedWithTour = true;

			_map.setShowOverlays(true);

			final TourData firstTourData = _allTourData.get(0);

			paintTours_20_One(firstTourData, false, true);
			setMapToSliderBounds(firstTourData);
		}
	}

	void actionSynchWithTour() {

		if (_allTourData.size() == 0) {
			return;
		}

		_isMapSynchedWithTour = _actionSynchWithTour.isChecked();

		if (_isMapSynchedWithTour) {

//			// remove prevous positions
//			TourManager.getInstance().resetMapPositions();

			_actionShowTourInMap.setChecked(true);
			_map.setShowOverlays(true);

			paintTours_20_One(_allTourData.get(0), true, true);

		} else {

			// disable synch with slider
			_isMapSynchedWithSlider = false;
			_actionSynchWithSlider.setChecked(false);
		}
	}

	void actionZoomIn() {
		_map.setZoom(_map.getZoom() + 1);
		centerTour();
		_map.paint();
	}

	void actionZoomOut() {
		_map.setZoom(_map.getZoom() - 1);
		centerTour();
		_map.paint();
	}

	void actionZoomShowAllPhotos() {

		centerPhotos(_filteredPhotos, true);
	}

	void actionZoomShowEntireMap() {

		_map.setMapCenter(new GeoPosition(0.0, 0.0));
		_map.setZoom(_map.getMapProvider().getMinimumZoomLevel());

		_map.paint();
	}

	void actionZoomShowEntireTour() {

		/*
		 * reset position for all tours, it was really annoying that previous selected tours moved
		 * to their previous positions, implemented in 11.8.2
		 */
		TourManager.getInstance().resetMapPositions();

		_actionShowTourInMap.setChecked(true);
		_map.setShowOverlays(true);

		paintEntireTour();
	}

	private void addMapListener() {

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
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourMapView.this) {
					_isPartVisible = false;
				}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourMapView.this) {

					_isPartVisible = true;

					if (_selectionWhenHidden != null) {

						onSelectionChanged(_selectionWhenHidden);

						_selectionWhenHidden = null;
					}
				}
			}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(PREF_SHOW_TILE_INFO) || property.equals(PREF_SHOW_TILE_BORDER)) {

					// map properties has changed

					final boolean isShowTileInfo = _prefStore.getBoolean(PREF_SHOW_TILE_INFO);
					final boolean isShowTileBorder = _prefStore.getBoolean(PREF_SHOW_TILE_BORDER);

					_map.setShowDebugInfo(isShowTileInfo, isShowTileBorder);
					_map.paint();

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

					// measurement system has changed

					UI.updateUnits();
					_map.setMeasurementSystem(UI.UNIT_VALUE_DISTANCE, UI.UNIT_LABEL_DISTANCE);

					createLegendImage(_tourPainterConfig.getLegendProvider());

					_map.paint();

				} else if (property.equals(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD)) {

					_map.setTourPaintMethodEnhanced(//
							event.getNewValue().equals(PrefPageAppearanceMap.TOUR_PAINT_METHOD_COMPLEX));

				} else if (property.equals(ICommonPreferences.GRAPH_COLORS_HAS_CHANGED)) {

					// update tour and legend

					createLegendImage(_tourPainterConfig.getLegendProvider());

					_map.disposeOverlayImageCache();
					_map.paint();

				} else if (property.equals(IPreferences.SRTM_COLORS_SELECTED_PROFILE_KEY)) {

					final String newValue = event.getNewValue().toString();
					final Integer prefProfileKey = Integer.valueOf(newValue);

					if (prefProfileKey != _selectedProfileKey) {

						_selectedProfileKey = prefProfileKey;

						_map.disposeTiles();
						_map.paint();
					}

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
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

						_allTourData.clear();
						_allTourData.addAll(modifiedTours);

						resetMap();
					}

				} else if (eventId == TourEventId.UPDATE_UI || eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();

				} else if (eventId == TourEventId.SLIDER_POSITION_CHANGED) {
					onSelectionChanged((ISelection) eventData);
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	/**
	 * Center photo in the map
	 * 
	 * @param allPhotos
	 * @param isForceZooming
	 */
	private void centerPhotos(final ArrayList<Photo> allPhotos, final boolean isForceZooming) {

		final Set<GeoPosition> positionBounds = getPhotoBounds(allPhotos);
		if (positionBounds == null) {
			return;
		}

		final int zoom = _map.getZoom();

		final Rectangle positionRect = getPositionRect(positionBounds, zoom);

		final Point center = new Point(//
				positionRect.x + positionRect.width / 2,
				positionRect.y + positionRect.height / 2);

		final GeoPosition geoPosition = _map.getMapProvider().pixelToGeo(center, zoom);

		_map.setMapCenter(geoPosition);

		if (isForceZooming) {
			setBoundsZoomLevel(positionBounds, false);
		}
	}

	/**
	 * Center the tour in the map when action is enabled
	 */
	private void centerTour() {

		if (_isPositionCentered) {

			final int zoom = _map.getZoom();

			Set<GeoPosition> positionBounds = null;
			if (_isTourOrWayPoint) {
				positionBounds = _tourPainterConfig.getTourBounds();
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

			_map.setMapCenter(geoPosition);
		}
	}

	private void clearView() {

		// disable tour data
		_allTourData.clear();
		_previousTourData = null;

		_tourPainterConfig.resetTourData();
		_tourPainterConfig.setPhotos(null, false, false);

		_tourInfoToolTipProvider.setTourData(null);

		showDefaultMap(false);
	}

	private void createActions(final Composite parent) {

		_actionTourColorAltitude = new ActionTourColor(
				this,
				TourMapColors.TOUR_COLOR_ALTITUDE,
				Messages.map_action_tour_color_altitude_tooltip,
				Messages.image_action_tour_color_altitude,
				Messages.image_action_tour_color_altitude_disabled);

		_actionTourColorGradient = new ActionTourColor(
				this,
				TourMapColors.TOUR_COLOR_GRADIENT,
				Messages.map_action_tour_color_gradient_tooltip,
				Messages.image_action_tour_color_gradient,
				Messages.image_action_tour_color_gradient_disabled);

		_actionTourColorPulse = new ActionTourColor(
				this,
				TourMapColors.TOUR_COLOR_PULSE,
				Messages.map_action_tour_color_pulse_tooltip,
				Messages.image_action_tour_color_pulse,
				Messages.image_action_tour_color_pulse_disabled);

		_actionTourColorSpeed = new ActionTourColor(
				this,
				TourMapColors.TOUR_COLOR_SPEED,
				Messages.map_action_tour_color_speed_tooltip,
				Messages.image_action_tour_color_speed,
				Messages.image_action_tour_color_speed_disabled);

		_actionTourColorPace = new ActionTourColor(
				this,
				TourMapColors.TOUR_COLOR_PACE,
				Messages.map_action_tour_color_pase_tooltip,
				Messages.image_action_tour_color_pace,
				Messages.image_action_tour_color_pace_disabled);

		_actionTourColorHrZone = new ActionTourColor(
				this,
				TourMapColors.TOUR_COLOR_HR_ZONE,
				Messages.Tour_Action_ShowHrZones_Tooltip,
				Messages.Image__PulseZones,
				Messages.Image__PulseZones_Disabled);

		_actionZoomIn = new ActionZoomIn(this);
		_actionZoomOut = new ActionZoomOut(this);
		_actionZoomCentered = new ActionZoomCentered(this);
		_actionZoomShowAll = new ActionZoomShowEntireEarth(this);
		_actionShowEntireTour = new ActionZoomShowEntireTour(this);

		_actionSynchWithPhoto = new ActionSynchWithPhoto(this);
		_actionSynchWithTour = new ActionSynchWithTour(this);
		_actionSynchWithSlider = new ActionSynchWithSlider(this);
		_actionSynchTourZoomLevel = new ActionSynchTourZoomLevel(this);

		_actionSelectMapProvider = new ActionSelectMapProvider(this);
		_actionSetDefaultPosition = new ActionSetDefaultPosition(this);
		_actionSaveDefaultPosition = new ActionSaveDefaultPosition(this);

		_actionPhotoFilter = new ActionPhotoProperties(this, parent, _state);
		_actionShowPhotos = new ActionShowPhotos(this);
		_actionShowAllFilteredPhotos = new ActionShowAllFilteredPhotos(this);
		_actionShowSliderInMap = new ActionShowSliderInMap(this);
		_actionShowSliderInLegend = new ActionShowSliderInLegend(this);
		_actionShowLegendInMap = new ActionShowLegendInMap(this);
		_actionShowScaleInMap = new ActionShowScaleInMap(this);
		_actionShowStartEndInMap = new ActionShowStartEndInMap(this);

		_actionShowPOI = new ActionShowPOI(this);
		_actionShowTourInMap = new ActionShowTourInMap(this);
		_actionShowTourInfoInMap = new ActionShowTourInfoInMap(this);
		_actionShowTourMarker = new ActionShowTourMarker(this);
		_actionShowWayPoints = new ActionShowWayPoints(this);

		_actionReloadFailedMapImages = new ActionReloadFailedMapImages(this);
		_actionDimMap = new ActionDimMap(this);
		_actionManageProvider = new ActionManageMapProviders(this);
	}

	/**
	 * Creates a new legend image and disposes the old image.
	 * 
	 * @param legendProvider
	 */
	private void createLegendImage(final ILegendProvider legendProvider) {

		Image legendImage = _mapLegend.getImage();

		// legend requires a tour with coordinates
		if (legendProvider == null /* || isPaintDataValid(fTourData) == false */) {
			showDefaultMap(_isShowPhoto);
			return;
		}

		// dispose old legend image
		if ((legendImage != null) && !legendImage.isDisposed()) {
			legendImage.dispose();
		}
		final int legendWidth = DEFAULT_LEGEND_WIDTH;
		int legendHeight = DEFAULT_LEGEND_HEIGHT;

		final Rectangle mapBounds = _map.getBounds();
		legendHeight = Math.max(1, Math.min(legendHeight, mapBounds.height - LEGEND_TOP_MARGIN));

		final RGB rgbTransparent = new RGB(0xfe, 0xfe, 0xfe);

		final ImageData overlayImageData = new ImageData(//
				legendWidth,
				legendHeight,
				24,
				new PaletteData(0xff, 0xff00, 0xff0000));

		overlayImageData.transparentPixel = overlayImageData.palette.getPixel(rgbTransparent);

		final Display display = Display.getCurrent();
		legendImage = new Image(display, overlayImageData);
		final Rectangle legendImageBounds = legendImage.getBounds();

		boolean isDataAvailable = false;
		if (legendProvider instanceof ILegendProviderGradientColors) {

			isDataAvailable = createLegendImage10SetProviderValues(//
					(ILegendProviderGradientColors) legendProvider,
					legendImageBounds);

		} else if (legendProvider instanceof ILegendProviderDiscreteColors) {

			isDataAvailable = createLegendImage20SetProviderValues(
					(ILegendProviderDiscreteColors) legendProvider,
					legendImageBounds);
		}

		final Color transparentColor = new Color(display, rgbTransparent);
		final GC gc = new GC(legendImage);
		{
			gc.setBackground(transparentColor);
			gc.fillRectangle(legendImageBounds);

			if (isDataAvailable) {
				TourMapPainter.drawLegend(gc, legendImageBounds, legendProvider, true);
			}
		}
		gc.dispose();
		transparentColor.dispose();

		_mapLegend.setImage(legendImage);
	}

	/**
	 * Update the min/max values in the {@link ILegendProviderGradientColors} for the currently
	 * displayed legend
	 * 
	 * @param legendProvider
	 * @param legendBounds
	 * @return Return <code>true</code> when the legend value could be updated, <code>false</code>
	 *         when data are not available
	 */
	private boolean createLegendImage10SetProviderValues(	final ILegendProviderGradientColors legendProvider,
															final Rectangle legendBounds) {

		if (_allTourData.size() == 0) {
			return false;
		}

		final GraphColorProvider colorProvider = GraphColorProvider.getInstance();

		ColorDefinition colorDefinition = null;
		final LegendConfig legendConfig = legendProvider.getLegendConfig();

		// tell the legend provider how to draw the legend
		switch (legendProvider.getTourColorId()) {

		case TourMapColors.TOUR_COLOR_ALTITUDE:

			float minValue = Float.MIN_VALUE;
			float maxValue = Float.MAX_VALUE;
			boolean setInitialValue = true;

			for (final TourData tourData : _allTourData) {

				final float[] dataSerie = tourData.getAltitudeSerie();
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final float dataValue : dataSerie) {

					if (dataValue == Float.MIN_VALUE) {
						// skip invalid values
						continue;
					}

					if (setInitialValue) {

						setInitialValue = false;
						minValue = maxValue = dataValue;
					}

					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if ((minValue == Float.MIN_VALUE) || (maxValue == Float.MAX_VALUE)) {
				return false;
			}

			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_ALTITUDE);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(
					legendBounds,
					minValue,
					maxValue,
					UI.UNIT_LABEL_ALTITUDE,
					LegendUnitFormat.Number);

			break;

		case TourMapColors.TOUR_COLOR_PULSE:

			minValue = Float.MIN_VALUE;
			maxValue = Float.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : _allTourData) {

				final float[] dataSerie = tourData.pulseSerie;
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final float dataValue : dataSerie) {

					// patch from Kenny Moens / 2011-08-04
					if (dataValue == 0) {
						continue;
					}

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataValue;
					}

					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if ((minValue == Float.MIN_VALUE) || (maxValue == Float.MAX_VALUE)) {
				return false;
			}

			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_HEARTBEAT);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(
					legendBounds,
					minValue,
					maxValue,
					Messages.graph_label_heartbeat_unit,
					LegendUnitFormat.Number);

			break;

		case TourMapColors.TOUR_COLOR_SPEED:

			minValue = Float.MIN_VALUE;
			maxValue = Float.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : _allTourData) {

				final float[] dataSerie = tourData.getSpeedSerie();
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final float dataValue : dataSerie) {

					if (dataValue == Float.MIN_VALUE) {
						// skip invalid values
						continue;
					}

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataValue;
					}

					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if ((minValue == Float.MIN_VALUE) || (maxValue == Float.MAX_VALUE)) {
				return false;
			}

			legendConfig.numberFormatDigits = 1;
			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_SPEED);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(
					legendBounds,
					minValue,
					maxValue,
					UI.UNIT_LABEL_SPEED,
					LegendUnitFormat.Number);

			break;

		case TourMapColors.TOUR_COLOR_PACE:

			minValue = Float.MIN_VALUE;
			maxValue = Float.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : _allTourData) {

				final float[] dataSerie = tourData.getPaceSerieSeconds();
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final float dataValue : dataSerie) {

					if (dataValue == Float.MIN_VALUE) {
						// skip invalid values
						continue;
					}

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataValue;
					}

					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if ((minValue == Float.MIN_VALUE) || (maxValue == Float.MAX_VALUE)) {
				return false;
			}

			legendConfig.unitFormat = LegendUnitFormat.Pace;
			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_PACE);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(
					legendBounds,
					minValue,
					maxValue,
					UI.UNIT_LABEL_PACE,
					LegendUnitFormat.Pace);

			break;

		case TourMapColors.TOUR_COLOR_GRADIENT:

			minValue = Float.MIN_VALUE;
			maxValue = Float.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : _allTourData) {

				final float[] dataSerie = tourData.getGradientSerie();
				if ((dataSerie == null) || (dataSerie.length == 0)) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (final float dataValue : dataSerie) {

					if (dataValue == Float.MIN_VALUE) {
						// skip invalid values
						continue;
					}

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataValue;
					}

					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if ((minValue == Float.MIN_VALUE) || (maxValue == Float.MAX_VALUE)) {
				return false;
			}

			legendConfig.numberFormatDigits = 1;
			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_GRADIENT);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(
					legendBounds,
					minValue,
					maxValue,
					Messages.graph_label_gradient_unit,
					LegendUnitFormat.Number);

			break;

		default:
			break;
		}

		return true;
	}

	private boolean createLegendImage20SetProviderValues(	final ILegendProviderDiscreteColors legendProvider,
															final Rectangle legendImageBounds) {

		if (_allTourData.size() == 0) {
			return false;
		}

		// tell the legend provider how to draw the legend
		switch (legendProvider.getTourColorId()) {

		case TourMapColors.TOUR_COLOR_HR_ZONE:

			boolean isValidData = false;

			for (final TourData tourData : _allTourData) {

				if (TrainingManager.isRequiredHrZoneDataAvailable(tourData) == false) {
					continue;
				}

				isValidData = true;
			}

			return isValidData;

		default:
			break;
		}

		return false;
	}

	@Override
	public void createPartControl(final Composite parent) {

		_mapLegend = new MapLegend();

		_map = new Map(parent, SWT.NONE);
		_map.setPainting(false);

		_map.setDirectPainter(_directMappingPainter);
//		_map.setLiveView(true);

		_map.setLegend(_mapLegend);
		_map.setShowLegend(true);
		_map.setMeasurementSystem(UI.UNIT_VALUE_DISTANCE, UI.UNIT_LABEL_DISTANCE);

		final String tourPaintMethod = _prefStore.getString(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD);
		_map.setTourPaintMethodEnhanced(tourPaintMethod.equals(PrefPageAppearanceMap.TOUR_PAINT_METHOD_COMPLEX));

		// setup tool tip's
		_map.setTourToolTip(_tourToolTip = new TourToolTip(_map));
		_tourInfoToolTipProvider.setActionsEnabled(true);

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

				if ((_isTourOrWayPoint == false) || (_isShowTour == false) || (_isShowLegend == false)) {
					return;
				}

				/*
				 * check height
				 */
				final Rectangle mapBounds = _map.getBounds();
				final Rectangle legendBounds = legendImage.getBounds();

				if ((mapBounds.height < DEFAULT_LEGEND_HEIGHT + LEGEND_TOP_MARGIN)
						|| ((mapBounds.height > DEFAULT_LEGEND_HEIGHT + LEGEND_TOP_MARGIN) //
						&& (legendBounds.height < DEFAULT_LEGEND_HEIGHT)) //
				) {

					createLegendImage(_tourPainterConfig.getLegendProvider());
				}
			}
		});

		createActions(parent);

		fillActionBars();

		addPartListener();
		addPrefListener();
		addSelectionListener();
		addTourEventListener();
		addMapListener();
		PhotoManager.addPhotoEventListener(this);

		// register overlays which draw the tour
		GeoclipseExtensions.registerOverlays(_map);

		// initialize map when part is created and the map size is > 0
		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {

				restoreState();
				enableActions();

				if (_allTourData.size() == 0) {
					// a tour is not displayed, find a tour provider which provides a tour
					showToursFromTourProvider();
				} else {
					_map.paint();
				}

				/*
				 * enable map drawing, this is done very late to disable flickering which is caused
				 * by setting up the map
				 */
				_map.setPainting(true);

				if (_mapDimLevel < 30) {
					showDimWarning();
				}
			}
		});
	}

	@Override
	public void dispose() {

		_allTourData.clear();

		_filteredPhotos.clear();
		_allPhotos.clear();

		// dispose tilefactory resources

		final ArrayList<MP> allMapProviders = MapProviderManager.getInstance().getAllMapProviders(true);
		for (final MP mp : allMapProviders) {
			mp.disposeAllImages();
		}

		_map.disposeOverlayImageCache();

		getViewSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);
		PhotoManager.removePhotoEventListener(this);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void enableActions() {
		enableActions(false);
	}

	private void enableActions(final boolean isForceTourColor) {

		_actionShowPOI.setEnabled(_poiPosition != null);

		// update legend action
		if (_isTourOrWayPoint) {

			_map.setShowLegend(_isShowLegend);

			if (_isShowLegend == false) {
				_actionShowSliderInLegend.setChecked(false);
			}
		}

		final boolean isAllPhotoAvailable = _allPhotos.size() > 0;
		final boolean isFilteredPhotoAvailable = _filteredPhotos.size() > 0;
		final boolean canShowFilteredPhoto = isFilteredPhotoAvailable && _isShowPhoto;

		/*
		 * sync photo has a higher priority than sync tour, both cannot be synced at the same time
		 */
		final boolean isPhotoSynced = canShowFilteredPhoto && _isMapSynchedWithPhoto;
		final boolean canSyncTour = isPhotoSynced == false;

		/*
		 * photo actions
		 */
		_actionPhotoFilter.setEnabled(isAllPhotoAvailable && _isShowPhoto);
		_actionShowAllFilteredPhotos.setEnabled(canShowFilteredPhoto);
		_actionShowPhotos.setEnabled(isAllPhotoAvailable);
		_actionSynchWithPhoto.setEnabled(canShowFilteredPhoto);

		/*
		 * tour actions
		 */
		final int numberOfTours = _allTourData.size();
		final boolean isMultipleTours = numberOfTours > 1 && _isShowTour;
		final boolean isOneTour = _isTourOrWayPoint && (isMultipleTours == false) && _isShowTour;

		_actionShowEntireTour.setEnabled(_isTourOrWayPoint && _isShowTour && numberOfTours > 0);
		_actionShowLegendInMap.setEnabled(_isTourOrWayPoint);
		_actionShowSliderInLegend.setEnabled(_isTourOrWayPoint && _isShowLegend);
		_actionShowSliderInMap.setEnabled(_isTourOrWayPoint);
		_actionShowStartEndInMap.setEnabled(isOneTour);
		_actionShowTourInfoInMap.setEnabled(isOneTour);
		_actionShowTourInMap.setEnabled(_isTourOrWayPoint);
		_actionShowTourMarker.setEnabled(_isTourOrWayPoint);
		_actionShowWayPoints.setEnabled(_isTourOrWayPoint);

		_actionSynchTourZoomLevel.setEnabled(isOneTour);
		_actionSynchWithSlider.setEnabled(isOneTour);
		_actionSynchWithTour.setEnabled(isOneTour && canSyncTour);

		if (numberOfTours == 0) {

			_actionTourColorAltitude.setEnabled(false);
			_actionTourColorGradient.setEnabled(false);
			_actionTourColorPulse.setEnabled(false);
			_actionTourColorSpeed.setEnabled(false);
			_actionTourColorPace.setEnabled(false);
			_actionTourColorHrZone.setEnabled(false);

		} else if (isForceTourColor) {

			_actionTourColorAltitude.setEnabled(true);
			_actionTourColorGradient.setEnabled(true);
			_actionTourColorPulse.setEnabled(true);
			_actionTourColorSpeed.setEnabled(true);
			_actionTourColorPace.setEnabled(true);
			_actionTourColorHrZone.setEnabled(true);

		} else if (isOneTour) {

			final TourData oneTourData = _allTourData.get(0);
			final boolean isPulse = oneTourData.pulseSerie != null;
			final boolean canShowHrZones = oneTourData.getNumberOfHrZones() > 0 && isPulse;

			_actionTourColorAltitude.setEnabled(true);
			_actionTourColorGradient.setEnabled(oneTourData.getGradientSerie() != null);
			_actionTourColorPulse.setEnabled(isPulse);
			_actionTourColorSpeed.setEnabled(oneTourData.getSpeedSerie() != null);
			_actionTourColorPace.setEnabled(oneTourData.getPaceSerie() != null);
			_actionTourColorHrZone.setEnabled(canShowHrZones);

		} else {

			_actionTourColorAltitude.setEnabled(false);
			_actionTourColorGradient.setEnabled(false);
			_actionTourColorPulse.setEnabled(false);
			_actionTourColorSpeed.setEnabled(false);
			_actionTourColorPace.setEnabled(false);
			_actionTourColorHrZone.setEnabled(false);
		}
	}

	private void fillActionBars() {

		/*
		 * fill view toolbar
		 */
		final IToolBarManager viewTbm = getViewSite().getActionBars().getToolBarManager();

		viewTbm.add(_actionTourColorAltitude);
		viewTbm.add(_actionTourColorPulse);
		viewTbm.add(_actionTourColorSpeed);
		viewTbm.add(_actionTourColorPace);
		viewTbm.add(_actionTourColorGradient);
		viewTbm.add(_actionTourColorHrZone);
		viewTbm.add(new Separator());

		viewTbm.add(_actionPhotoFilter);
		viewTbm.add(_actionShowPhotos);
		viewTbm.add(_actionShowAllFilteredPhotos);
		viewTbm.add(_actionSynchWithPhoto);
		viewTbm.add(new Separator());

		viewTbm.add(_actionShowTourInMap);
		viewTbm.add(_actionShowEntireTour);
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

		fillMapContextMenu(menuMgr);
	}

	@Override
	public void fillContextMenu(final IMenuManager menuMgr) {
		fillMapContextMenu(menuMgr);
	}

	private void fillMapContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionShowLegendInMap);
		menuMgr.add(_actionShowScaleInMap);
		menuMgr.add(_actionShowSliderInMap);
		menuMgr.add(_actionShowSliderInLegend);
		menuMgr.add(new Separator());

		menuMgr.add(_actionShowTourMarker);
		menuMgr.add(_actionShowWayPoints);
		menuMgr.add(_actionShowPOI);
		menuMgr.add(_actionShowStartEndInMap);
		menuMgr.add(_actionShowTourInfoInMap);
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
		return TourMapColors.getColorProvider(colorId);
	}

	public Map getMap() {
		return _map;
	}

	public int getMapDimLevel() {
		return _mapDimLevel;
	}

	/**
	 * Calculate lat/lon bounds for all photos.
	 * 
	 * @param allPhotos
	 * @return
	 */
	private Set<GeoPosition> getPhotoBounds(final ArrayList<Photo> allPhotos) {

		/*
		 * get min/max longitude/latitude
		 */
		double minLatitude = 0;
		double maxLatitude = 0;
		double minLongitude = 0;
		double maxLongitude = 0;

		boolean isFirst = true;

		for (final Photo photo : allPhotos) {

			final boolean isPhotoWithGps = _isLinkPhotoDisplayed ? photo.isLinkPhotoWithGps : photo.isTourPhotoWithGps;

			if (isPhotoWithGps) {

				double latitude;
				double longitude;

				if (_isLinkPhotoDisplayed) {
					latitude = photo.getLinkLatitude();
					longitude = photo.getLinkLongitude();
				} else {
					latitude = photo.getTourLatitude();
					longitude = photo.getTourLongitude();
				}

				// exclude invalid positions
				if (latitude == 0) {
					continue;
				}

//				System.out.println(net.tourbook.common.UI.timeStampNano() + " " + photo);
//				System.out.println(net.tourbook.common.UI.timeStampNano() + " \t" + latitude + "\t" + longitude);
//				// TODO remove SYSTEM.OUT.PRINTLN

				if (isFirst) {

					isFirst = false;

					minLatitude = maxLatitude = latitude;
					minLongitude = maxLongitude = longitude;

				} else {

					minLatitude = latitude < minLatitude ? latitude : minLatitude;
					maxLatitude = latitude > maxLatitude ? latitude : maxLatitude;

					minLongitude = longitude < minLongitude ? longitude : minLongitude;
					maxLongitude = longitude > maxLongitude ? longitude : maxLongitude;

					if (minLatitude == 0) {
						minLatitude = -180.0;
					}
				}
			}
		}

		if (isFirst) {
			// there are no photos with geo
			return null;
		}

		final Set<GeoPosition> mapPositions = new HashSet<GeoPosition>();
		mapPositions.add(new GeoPosition(minLatitude, minLongitude));
		mapPositions.add(new GeoPosition(maxLatitude, maxLongitude));

		return mapPositions;
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

	private Set<GeoPosition> getTourBounds(final ArrayList<TourData> tourDataList) {

		/*
		 * get min/max longitude/latitude
		 */
		double allMinLatitude = Double.MIN_VALUE;
		double allMaxLatitude = 0;
		double allMinLongitude = 0;
		double allMaxLongitude = 0;

		for (final TourData tourData : tourDataList) {

			final double[] latitudeSerie = tourData.latitudeSerie;
			final double[] longitudeSerie = tourData.longitudeSerie;

			if ((latitudeSerie == null) || (longitudeSerie == null)) {
				continue;
			}

			final GeoPosition[] geoPosition = tourData.getGeoBounds();

			if (geoPosition == null) {
				continue;
			}

			final double tourMinLatitude = geoPosition[0].latitude;
			final double tourMinLongitude = geoPosition[0].longitude;

			final double tourMaxLatitude = geoPosition[1].latitude;
			final double tourMaxLongitude = geoPosition[1].longitude;

			if (allMinLatitude == Double.MIN_VALUE) {

				// initialize first data point

				allMinLatitude = tourMinLatitude;
				allMinLongitude = tourMinLongitude;

				allMaxLatitude = tourMaxLatitude;
				allMaxLongitude = tourMaxLongitude;

			} else {

				allMinLatitude = tourMinLatitude < allMinLatitude ? tourMinLatitude : allMinLatitude;
				allMaxLatitude = tourMaxLatitude > allMaxLatitude ? tourMaxLatitude : allMaxLatitude;

				allMinLongitude = tourMinLongitude < allMinLongitude ? tourMinLongitude : allMinLongitude;
				allMaxLongitude = tourMaxLongitude > allMaxLongitude ? tourMaxLongitude : allMaxLongitude;
			}
		}

		if (allMinLatitude == Double.MIN_VALUE) {

			return null;

		} else {

			final Set<GeoPosition> mapPositions = new HashSet<GeoPosition>();

			mapPositions.add(new GeoPosition(allMinLatitude, allMinLongitude));
			mapPositions.add(new GeoPosition(allMaxLatitude, allMaxLongitude));

			return mapPositions;
		}
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

//		System.out.println(net.tourbook.common.UI.timeStampNano() + " Map::onSelectionChanged\t" + selection);
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (_isPartVisible == false) {

			if (selection instanceof SelectionTourData
					|| selection instanceof SelectionTourId
					|| selection instanceof SelectionTourIds) {

				// keep only selected tours
				_selectionWhenHidden = selection;
			}
			return;
		}

		if (selection instanceof SelectionTourData) {

			final SelectionTourData selectionTourData = (SelectionTourData) selection;
			final TourData tourData = selectionTourData.getTourData();

			paintTours_20_One(tourData, selectionTourData.isForceRedraw(), true);
			paintPhotoSelection(selection);

			enableActions();

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId tourIdSelection = (SelectionTourId) selection;
			final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

			paintTours_20_One(tourData, false, true);
			paintPhotoSelection(selection);

			enableActions();

		} else if (selection instanceof SelectionTourIds) {

			// paint all selected tours

			final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
			if (tourIds.size() == 0) {

				// history tour (without tours) is displayed

				final ArrayList<Photo> allPhotos = paintPhotoSelection(selection);

				if (allPhotos.size() > 0) {

//					centerPhotos(allPhotos, false);
					showDefaultMap(true);

					enableActions();
				}

			} else if (tourIds.size() == 1) {

				// only 1 tour is displayed, synch with this tour !!!

				final TourData tourData = TourManager.getInstance().getTourData(tourIds.get(0));

				paintTours_20_One(tourData, false, true);
				paintPhotoSelection(selection);

				enableActions();

			} else {

				// paint multiple tours

				paintTours(tourIds);
				paintPhotoSelection(selection);

				enableActions(true);
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

			_isTourOrWayPoint = false;

			clearView();

			final PointOfInterest poi = (PointOfInterest) selection;

			_poiPosition = poi.getPosition();
			_poiName = poi.getName();

			_poiZoomLevel = poi.getRecommendedZoom();
			if (_poiZoomLevel == -1) {
				_poiZoomLevel = _map.getZoom();
			}

			_map.setPoi(_poiPosition, _poiZoomLevel, _poiName);

			_actionShowPOI.setChecked(true);

			enableActions();

		} else if (selection instanceof StructuredSelection) {

			final StructuredSelection structuredSelection = (StructuredSelection) selection;
			final Object firstElement = structuredSelection.getFirstElement();

			if (firstElement instanceof TVICatalogComparedTour) {

				final TVICatalogComparedTour comparedTour = (TVICatalogComparedTour) firstElement;
				final long tourId = comparedTour.getTourId();

				final TourData tourData = TourManager.getInstance().getTourData(tourId);
				paintTours_20_One(tourData, false, true);

			} else if (firstElement instanceof TVICompareResultComparedTour) {

				final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
				final TourData tourData = TourManager.getInstance().getTourData(
						compareResultItem.getComparedTourData().getTourId());
				paintTours_20_One(tourData, false, true);

			} else if (firstElement instanceof TourWayPoint) {

				final TourWayPoint wp = (TourWayPoint) firstElement;

				_map.setPOI(_wayPointToolTipProvider, wp);
			}

			enableActions();

		} else if (selection instanceof PhotoSelection) {

			paintPhotos(((PhotoSelection) selection).galleryPhotos);

			enableActions();

		} else if (selection instanceof SelectionTourCatalogView) {

			// show reference tour

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {

				final TourData tourData = TourManager.getInstance().getTourData(refItem.getTourId());

				paintTours_20_One(tourData, false, true);

				enableActions();
			}
		}
	}

	private void paintEntireTour() {

		// get overlay key for all tours which have valid tour data
		long newOverlayKey = -1;
		for (final TourData tourData : _allTourData) {

			if (isPaintDataValid(tourData)) {
				newOverlayKey += tourData.getTourId();
			}
		}

		// check if a valid tour is available
		if (newOverlayKey == -1) {
			showDefaultMap(_isShowPhoto);
			return;
		}

		// force single tour to be repainted
		_previousTourData = null;

		_tourPainterConfig.setTourData(_allTourData, _isShowTour);
		_tourPainterConfig.setPhotos(_filteredPhotos, _isShowPhoto, _isLinkPhotoDisplayed);

		_tourInfoToolTipProvider.setTourDataList(_allTourData);

//		final TourData firstTourData = _allTourData.get(0);
//
//		// set slider position
//		_directMappingPainter.setPaintContext(
//				_map,
//				_isShowTour,
//				firstTourData,
//				_currentLeftSliderValueIndex,
//				_currentRightSliderValueIndex,
//				_actionShowSliderInMap.isChecked(),
//				_actionShowSliderInLegend.isChecked());

		final Set<GeoPosition> tourBounds = getTourBounds(_allTourData);

		_tourPainterConfig.setTourBounds(tourBounds);

		_directMappingPainter.disablePaintContext();

		_map.setShowOverlays(_isShowTour || _isShowPhoto);
		_map.setShowLegend(_isShowTour && _isShowLegend);

		if (_previousOverlayKey != newOverlayKey) {

			_previousOverlayKey = newOverlayKey;

			_map.setOverlayKey(Long.toString(newOverlayKey));
			_map.disposeOverlayImageCache();
		}

		setBoundsZoomLevel(tourBounds, false);

		createLegendImage(_tourPainterConfig.getLegendProvider());

		_map.paint();
	}

	private void paintPhotos(final ArrayList<Photo> allPhotos) {

		_allPhotos.clear();
		_allPhotos.addAll(allPhotos);

		runPhotoFilter();

//		// dump tour photos
//		System.out.println(net.tourbook.common.UI.timeStampNano() + " paintPhotos\t");
//		// TODO remove SYSTEM.OUT.PRINTLN
//
//		for (final Photo photo : allPhotos) {
//			System.out.println(net.tourbook.common.UI.timeStampNano()
//					+ " "
//					+ photo.imageFileName
//					+ ("\t" + new DateTime(photo.adjustedTime)));
//			// TODO remove SYSTEM.OUT.PRINTLN
//		}

		if (_isShowPhoto && _isMapSynchedWithPhoto) {
			centerPhotos(_filteredPhotos, false);
		}

		_map.setShowOverlays(_isShowTour || _isShowPhoto);
		_map.setOverlayKey(Integer.toString(_filteredPhotos.hashCode()));

		_map.disposeOverlayImageCache();

		_map.paint();
	}

	/**
	 * @param selection
	 * @return Returns a list which contains all photos.
	 */
	private ArrayList<Photo> paintPhotoSelection(final ISelection selection) {

		_isLinkPhotoDisplayed = false;

		final ArrayList<Photo> allPhotos = new ArrayList<Photo>();

		if (selection instanceof TourPhotoLinkSelection) {

			_isLinkPhotoDisplayed = true;

			final TourPhotoLinkSelection linkSelection = (TourPhotoLinkSelection) selection;

			final ArrayList<TourPhotoLink> tourPhotoLinks = linkSelection.tourPhotoLinks;

			for (final TourPhotoLink tourPhotoLink : tourPhotoLinks) {
				allPhotos.addAll(tourPhotoLink.linkPhotos);
			}

		} else {

			for (final TourData tourData : _allTourData) {

				final ArrayList<Photo> galleryPhotos = tourData.getGalleryPhotos();

				if (galleryPhotos != null) {
					allPhotos.addAll(galleryPhotos);
				}
			}
		}

		paintPhotos(allPhotos);

		return allPhotos;
	}

	private void paintTours(final ArrayList<Long> tourIdList) {

		_isTourOrWayPoint = true;

		// force single tour to be repainted
		_previousTourData = null;

		_directMappingPainter.disablePaintContext();

		_map.setShowOverlays(_isShowTour || _isShowPhoto);
		_map.setShowLegend(_isShowTour && _isShowLegend);

		long newOverlayKey = _tourHashOverlayKey;

		if (tourIdList.hashCode() != _tourIdHash || _allTourData.hashCode() != _tourDataHash) {

			// tour data needs to be loaded

			_allTourData.clear();

			newOverlayKey = paintTours_05_GetTourData(tourIdList);

			_tourIdHash = tourIdList.hashCode();
			_tourDataHash = _allTourData.hashCode();
			_tourHashOverlayKey = newOverlayKey;
		}

		_tourPainterConfig.setTourData(_allTourData, _isShowTour);
		_tourPainterConfig.setPhotos(_filteredPhotos, _isShowPhoto, _isLinkPhotoDisplayed);

		_tourInfoToolTipProvider.setTourDataList(_allTourData);

		if (_previousOverlayKey != newOverlayKey) {

			_previousOverlayKey = newOverlayKey;

			_map.setOverlayKey(Long.toString(newOverlayKey));
			_map.disposeOverlayImageCache();
		}

		createLegendImage(_tourPainterConfig.getLegendProvider());

		_map.paint();
	}

	private long paintTours_05_GetTourData(final ArrayList<Long> tourIdList) {

		// create a unique overlay key for the selected tours
		final long newOverlayKey[] = { 0 };

		if (tourIdList.size() > 200) {

			try {

				final IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
					@Override
					public void run(final IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {

						int loadCounter = 0;
						final int idSize = tourIdList.size();

						monitor.beginTask(Messages.Tour_Data_LoadTourData_Monitor, idSize);

						for (final Long tourId : tourIdList) {

							monitor.subTask(NLS.bind(
									Messages.Tour_Data_LoadTourData_Monitor_SubTask,
									++loadCounter,
									idSize));

							if (monitor.isCanceled()) {
								break;
							}

							final TourData tourData = TourManager.getInstance().getTourData(tourId);
							if (isPaintDataValid(tourData)) {

								// keep tour data for each tour id
								_allTourData.add(tourData);
								newOverlayKey[0] += tourData.getTourId();
							}

							monitor.worked(1);
						}
					}
				};

				new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, true, saveRunnable);

			} catch (final InvocationTargetException e) {
				StatusUtil.showStatus(e);
			} catch (final InterruptedException e) {
				StatusUtil.showStatus(e);
			}

		} else {

			for (final Long tourId : tourIdList) {

				final TourData tourData = TourManager.getInstance().getTourData(tourId);
				if (isPaintDataValid(tourData)) {

					// keep tour data for each tour id
					_allTourData.add(tourData);
					newOverlayKey[0] += tourData.getTourId();
				}
			}
		}

		return newOverlayKey[0];
	}

	private void paintTours_10_All() {

		if (_allTourData.size() == 0) {
			_tourInfoToolTipProvider.setTourData(null);
			return;
		}

		// show/hide legend
		_map.setShowLegend(_isShowTour);

		if (_allTourData.size() > 1) {

			// multiple tours are displayed

			paintTours_30_Multiple();
			enableActions(true);

		} else {
			paintTours_20_One(_allTourData.get(0), true, false);
			enableActions();
		}
	}

	/**
	 * Paint the currently selected tour in the map
	 * 
	 * @param tourData
	 * @param forceRedraw
	 * @param isSynchronized
	 *            when <code>true</code>, map will be synchronized
	 */
	private void paintTours_20_One(final TourData tourData, final boolean forceRedraw, final boolean isSynchronized) {

		_isTourOrWayPoint = true;

		if (isPaintDataValid(tourData) == false) {
			showDefaultMap(_isShowPhoto);
			return;
		}

		// prevent loading the same tour
		if (forceRedraw == false) {

			if ((_allTourData.size() == 1) && (_allTourData.get(0) == tourData)) {
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

		_tourPainterConfig.setTourData(tourData, _isShowTour);

		/*
		 * set tour into tour data list, this is currently used to draw the legend, it's also used
		 * to figure out if multiple tours are selected
		 */
		_allTourData.clear();
		_allTourData.add(tourData);

		_tourInfoToolTipProvider.setTourDataList(_allTourData);

		// set the paint context (slider position) for the direct mapping painter
		_directMappingPainter.setPaintContext(
				_map,
				_isShowTour,
				tourData,
				_currentLeftSliderValueIndex,
				_currentRightSliderValueIndex,
				_actionShowSliderInMap.isChecked(),
				_actionShowSliderInLegend.isChecked());

		// set the tour bounds
		final Set<GeoPosition> tourBounds = getTourBounds(tourData);
		_tourPainterConfig.setTourBounds(tourBounds);

		_map.setShowOverlays(_isShowTour || _isShowPhoto);
		_map.setShowLegend(_isShowTour && _isShowLegend);

		/*
		 * set position and zoom level for the tour
		 */
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
				setBoundsZoomLevel(tourBounds, true);

			} else {

				// position tour to the previous position
				_map.setZoom(tourData.mapZoomLevel);
				_map.setMapCenter(new GeoPosition(
						tourData.mapCenterPositionLatitude,
						tourData.mapCenterPositionLongitude));
			}
		}

		// keep tour data
		_previousTourData = tourData;

		if (isNewTour || forceRedraw) {

			// adjust legend values for the new or changed tour
			createLegendImage(_tourPainterConfig.getLegendProvider());

			_map.setOverlayKey(tourData.getTourId().toString());
			_map.disposeOverlayImageCache();

		}

		_map.paint();
	}

	/**
	 * paints the tours which are set in {@link #_allTourData}
	 */
	private void paintTours_30_Multiple() {

		_isTourOrWayPoint = true;

		// force single tour to be repainted
		_previousTourData = null;

		_tourPainterConfig.setTourData(_allTourData, _isShowTour);
		_tourPainterConfig.setPhotos(_filteredPhotos, _isShowPhoto, _isLinkPhotoDisplayed);

		_tourInfoToolTipProvider.setTourDataList(_allTourData);

		_directMappingPainter.disablePaintContext();

		_map.setShowOverlays(_isShowTour || _isShowPhoto);
		_map.setShowLegend(_isShowTour && _isShowLegend);

		// get overlay key for all tours which have valid tour data
		long newOverlayKey = -1;
		for (final TourData tourData : _allTourData) {

			if (isPaintDataValid(tourData)) {
				newOverlayKey += tourData.getTourId();
			}
		}

		if (_previousOverlayKey != newOverlayKey) {

			_previousOverlayKey = newOverlayKey;

			_map.setOverlayKey(Long.toString(newOverlayKey));
			_map.disposeOverlayImageCache();
		}

		createLegendImage(_tourPainterConfig.getLegendProvider());

		_map.paint();
	}

	private void paintTourSliders(	final TourData tourData,
									final int leftSliderValuesIndex,
									final int rightSliderValuesIndex,
									final int selectedSliderIndex) {

		_isTourOrWayPoint = true;

		if (isPaintDataValid(tourData) == false) {
			showDefaultMap(_isShowPhoto);
			return;
		}

		_currentLeftSliderValueIndex = leftSliderValuesIndex;
		_currentRightSliderValueIndex = rightSliderValuesIndex;
		_currentSelectedSliderValueIndex = selectedSliderIndex;

		_directMappingPainter.setPaintContext(
				_map,
				_isShowTour,
				tourData,
				leftSliderValuesIndex,
				rightSliderValuesIndex,
				_actionShowSliderInMap.isChecked(),
				_actionShowSliderInLegend.isChecked());

		if (_isMapSynchedWithSlider) {

			setMapToSliderBounds(tourData);

			_map.paint();

		} else {

			_map.redraw();
		}
	}

	@Override
	public void photoEvent(final IViewPart viewPart, final PhotoEventId photoEventId, final Object data) {

		if (photoEventId == PhotoEventId.PHOTO_SELECTION) {

			if (data instanceof TourPhotoLinkSelection) {

				onSelectionChanged((TourPhotoLinkSelection) data);

			} else if (data instanceof PhotoSelection) {

				onSelectionChanged((PhotoSelection) data);
			}

		} else if (photoEventId == PhotoEventId.PHOTO_ATTRIBUTES_ARE_MODIFIED) {

			if (data instanceof ArrayList<?>) {

				updateFilteredPhotos();
			}

		} else if (photoEventId == PhotoEventId.PHOTO_IMAGE_PATH_IS_MODIFIED) {

			// this is not working, manual refresh is necessary
//			_map.redraw();
		}
	}

	@Override
	public void photoPropertyEvent(final PhotoPropertiesEvent event) {

		_photoFilterRatingStars = event.filterRatingStars;
		_photoFilterRatingStarOperator = event.fiterRatingStarOperator;

		updateFilteredPhotos();
	}

	private void resetMap() {

		if (_allTourData.size() == 0) {
			return;
		}

		_map.disposeOverlayImageCache();

		paintTours_10_All();

		_map.paint();
	}

	private void restoreState() {

		// is show tour
		_isShowTour = Util.getStateBoolean(_state, STATE_IS_SHOW_TOUR_IN_MAP, true);
		_actionShowTourInMap.setChecked(_isShowTour);

		// is show photo
		_isShowPhoto = Util.getStateBoolean(_state, STATE_IS_SHOW_PHOTO_IN_MAP, true);
		_actionShowPhotos.setChecked(_isShowPhoto);

		// is show legend
		_isShowLegend = Util.getStateBoolean(_state, STATE_IS_SHOW_LEGEND_IN_MAP, true);
		_actionShowLegendInMap.setChecked(_isShowLegend);

		// is sync with photo
		_isMapSynchedWithPhoto = Util.getStateBoolean(_state, STATE_SYNC_WITH_PHOTO, true);
		_actionSynchWithPhoto.setChecked(_isMapSynchedWithPhoto);

		// checkbox: is tour centered
		final boolean isTourCentered = _state.getBoolean(MEMENTO_ZOOM_CENTERED);
		_actionZoomCentered.setChecked(isTourCentered);
		_isPositionCentered = isTourCentered;

		// checkbox: synch map with tour
		final boolean isSynchTour = Util.getStateBoolean(_state, MEMENTO_SYNCH_WITH_SELECTED_TOUR, true);
		_actionSynchWithTour.setChecked(isSynchTour);
		_isMapSynchedWithTour = isSynchTour;

		// ckeckbox: synch with tour chart slider
		final boolean isSynchSlider = _state.getBoolean(MEMENTO_SYNCH_WITH_TOURCHART_SLIDER);
		_actionSynchWithSlider.setChecked(isSynchSlider);
		_isMapSynchedWithSlider = isSynchSlider;

		//
		_actionSynchTourZoomLevel.setZoomLevel(Util.getStateInt(_state, MEMENTO_SYNCH_TOUR_ZOOM_LEVEL, 0));
		_mapDimLevel = Util.getStateInt(_state, MEMENTO_MAP_DIM_LEVEL, -1);

		// checkbox: show start/end in map
		_actionShowStartEndInMap.setChecked(_state.getBoolean(MEMENTO_SHOW_START_END_IN_MAP));
		_tourPainterConfig.isShowStartEndInMap = _actionShowStartEndInMap.isChecked();

		// show tour marker
		final boolean isShowMarker = Util.getStateBoolean(_state, MEMENTO_SHOW_TOUR_MARKER, true);
		_actionShowTourMarker.setChecked(isShowMarker);
		_tourPainterConfig.isShowTourMarker = isShowMarker;

		// checkbox: show way points
		final boolean isShowWayPoints = Util.getStateBoolean(_state, MEMENTO_SHOW_WAY_POINTS, true);
		_actionShowWayPoints.setChecked(isShowWayPoints);
		_tourPainterConfig.isShowWayPoints = isShowWayPoints;
		if (isShowWayPoints) {
			_tourToolTip.addToolTipProvider(_wayPointToolTipProvider);
		}

		// checkbox: show tour info in map
		final boolean isShowTourInfo = Util.getStateBoolean(_state, MEMENTO_SHOW_TOUR_INFO_IN_MAP, true);
		_actionShowTourInfoInMap.setChecked(isShowTourInfo);
		if (isShowTourInfo) {
			_tourToolTip.addToolTipProvider(_tourInfoToolTipProvider);
		}

		// checkbox: show scale
		final boolean isScaleVisible = Util.getStateBoolean(_state, MEMENTO_SHOW_SCALE_IN_MAP, true);
		_actionShowScaleInMap.setChecked(isScaleVisible);
		_map.setShowScale(isScaleVisible);

		// show slider
		_actionShowSliderInMap.setChecked(Util.getStateBoolean(_state, MEMENTO_SHOW_SLIDER_IN_MAP, true));

		_actionShowSliderInLegend.setChecked(_state.getBoolean(MEMENTO_SHOW_SLIDER_IN_LEGEND));

		// restore map provider by selecting the last used map factory
		_actionSelectMapProvider.selectMapProvider(_state.get(MEMENTO_SELECTED_MAP_PROVIDER_ID));

		_actionPhotoFilter.restoreState();

		// default position
		_defaultZoom = Util.getStateInt(_state, MEMENTO_DEFAULT_POSITION_ZOOM, 10);
		_defaultPosition = new GeoPosition(//
				Util.getStateDouble(_state, MEMENTO_DEFAULT_POSITION_LATITUDE, 46.303074),
				Util.getStateDouble(_state, MEMENTO_DEFAULT_POSITION_LONGITUDE, 7.526386));

		// tour color
		try {
			final Integer colorId = _state.getInt(MEMENTO_TOUR_COLOR_ID);

			switch (colorId) {
			case TourMapColors.TOUR_COLOR_ALTITUDE:
				_actionTourColorAltitude.setChecked(true);
				break;

			case TourMapColors.TOUR_COLOR_GRADIENT:
				_actionTourColorGradient.setChecked(true);
				break;

			case TourMapColors.TOUR_COLOR_PULSE:
				_actionTourColorPulse.setChecked(true);
				break;

			case TourMapColors.TOUR_COLOR_SPEED:
				_actionTourColorSpeed.setChecked(true);
				break;

			case TourMapColors.TOUR_COLOR_PACE:
				_actionTourColorPace.setChecked(true);
				break;

			case TourMapColors.TOUR_COLOR_HR_ZONE:
				_actionTourColorHrZone.setChecked(true);
				break;

			default:
				_actionTourColorAltitude.setChecked(true);
				break;
			}

			_tourPainterConfig.setLegendProvider(getLegendProvider(colorId));

		} catch (final NumberFormatException e) {
			_actionTourColorAltitude.setChecked(true);
		}

		// draw tour with default color

		_map.setShowOverlays(_isShowTour || _isShowPhoto);
		_map.setShowLegend(_isShowTour);

		// check legend provider
		if (_tourPainterConfig.getLegendProvider() == null) {

			// set default legend provider
			_tourPainterConfig.setLegendProvider(getLegendProvider(TourMapColors.TOUR_COLOR_ALTITUDE));

			// hide legend
			_map.setShowLegend(false);
		}

		// debug info
		final boolean isShowTileInfo = _prefStore.getBoolean(TourMapView.PREF_SHOW_TILE_INFO);
		final boolean isShowTileBorder = _prefStore.getBoolean(PREF_SHOW_TILE_BORDER);

		_map.setShowDebugInfo(isShowTileInfo, isShowTileBorder);

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

	private void runPhotoFilter() {

		_filteredPhotos.clear();

		if (_isPhotoFilterActive) {

			final boolean isNoStar = _photoFilterRatingStars == 0;
			final boolean isEqual = _photoFilterRatingStarOperator == PhotoProperties.OPERATOR_IS_EQUAL;
			final boolean isMore = _photoFilterRatingStarOperator == PhotoProperties.OPERATOR_IS_MORE_OR_EQUAL;
			final boolean isLess = _photoFilterRatingStarOperator == PhotoProperties.OPERATOR_IS_LESS_OR_EQUAL;

			for (final Photo photo : _allPhotos) {

				final int ratingStars = photo.ratingStars;

				if (isNoStar && ratingStars == 0) {

					// only photos without stars are displayed

					_filteredPhotos.add(photo);

				} else if (isEqual && ratingStars == _photoFilterRatingStars) {

					_filteredPhotos.add(photo);

				} else if (isMore && ratingStars >= _photoFilterRatingStars) {

					_filteredPhotos.add(photo);

				} else if (isLess && ratingStars <= _photoFilterRatingStars) {

					_filteredPhotos.add(photo);
				}
			}

		} else {

			// photo filter is not active

			_filteredPhotos.addAll(_allPhotos);
		}

		_tourPainterConfig.setPhotos(_filteredPhotos, _isShowPhoto, _isLinkPhotoDisplayed);

		enableActions();

		PhotoManager.firePhotoEvent(this, PhotoEventId.PHOTO_FILTER, new MapFilterData(
				_allPhotos.size(),
				_filteredPhotos.size()));

//		System.out.println(net.tourbook.common.UI.timeStampNano()
//				+ " runPhotoFilter"
//				+ ("\tall=" + _allPhotos.size())
//				+ ("\tfilter=" + _filteredPhotos.size()));
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	private void saveState() {

		// save checked actions
		_state.put(STATE_IS_SHOW_TOUR_IN_MAP, _isShowTour);
		_state.put(STATE_IS_SHOW_PHOTO_IN_MAP, _isShowPhoto);
		_state.put(STATE_IS_SHOW_LEGEND_IN_MAP, _isShowLegend);

		_state.put(STATE_SYNC_WITH_PHOTO, _isMapSynchedWithPhoto);

		_state.put(MEMENTO_ZOOM_CENTERED, _actionZoomCentered.isChecked());
		_state.put(MEMENTO_SYNCH_WITH_SELECTED_TOUR, _actionSynchWithTour.isChecked());
		_state.put(MEMENTO_SYNCH_WITH_TOURCHART_SLIDER, _actionSynchWithSlider.isChecked());
		_state.put(MEMENTO_SYNCH_TOUR_ZOOM_LEVEL, _actionSynchTourZoomLevel.getZoomLevel());

		_state.put(MEMENTO_MAP_DIM_LEVEL, _mapDimLevel);

		_state.put(MEMENTO_SHOW_START_END_IN_MAP, _actionShowStartEndInMap.isChecked());
		_state.put(MEMENTO_SHOW_SCALE_IN_MAP, _actionShowScaleInMap.isChecked());
		_state.put(MEMENTO_SHOW_SLIDER_IN_MAP, _actionShowSliderInMap.isChecked());
		_state.put(MEMENTO_SHOW_SLIDER_IN_LEGEND, _actionShowSliderInLegend.isChecked());
		_state.put(MEMENTO_SHOW_TOUR_MARKER, _actionShowTourMarker.isChecked());
		_state.put(MEMENTO_SHOW_TOUR_INFO_IN_MAP, _actionShowTourInfoInMap.isChecked());
		_state.put(MEMENTO_SHOW_WAY_POINTS, _actionShowWayPoints.isChecked());

		_state.put(MEMENTO_SELECTED_MAP_PROVIDER_ID, _actionSelectMapProvider.getSelectedMapProvider().getId());

		if (_defaultPosition == null) {

			final MP mapProvider = _map.getMapProvider();

			_state.put(MEMENTO_DEFAULT_POSITION_ZOOM, mapProvider == null ? //
					_defaultZoom
					: mapProvider.getMinimumZoomLevel());

			_state.put(MEMENTO_DEFAULT_POSITION_LATITUDE, 0.0F);
			_state.put(MEMENTO_DEFAULT_POSITION_LONGITUDE, 0.0F);
		} else {
			_state.put(MEMENTO_DEFAULT_POSITION_ZOOM, _defaultZoom);
			_state.put(MEMENTO_DEFAULT_POSITION_LATITUDE, (float) _defaultPosition.latitude);
			_state.put(MEMENTO_DEFAULT_POSITION_LONGITUDE, (float) _defaultPosition.longitude);
		}

		// tour color
		int colorId;

		if (_actionTourColorGradient.isChecked()) {
			colorId = TourMapColors.TOUR_COLOR_GRADIENT;

		} else if (_actionTourColorPulse.isChecked()) {
			colorId = TourMapColors.TOUR_COLOR_PULSE;

		} else if (_actionTourColorSpeed.isChecked()) {
			colorId = TourMapColors.TOUR_COLOR_SPEED;

		} else if (_actionTourColorPace.isChecked()) {
			colorId = TourMapColors.TOUR_COLOR_PACE;

		} else if (_actionTourColorHrZone.isChecked()) {
			colorId = TourMapColors.TOUR_COLOR_HR_ZONE;
		} else {
			colorId = TourMapColors.TOUR_COLOR_ALTITUDE;
		}
		_state.put(MEMENTO_TOUR_COLOR_ID, colorId);

		_actionPhotoFilter.saveState();
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
	private void setBoundsZoomLevel(final Set<GeoPosition> positions, final boolean isAdjustZoomLevel) {

		if ((positions == null) || (positions.size() < 2)) {
			return;
		}

		final MP mp = _map.getMapProvider();

		final int maximumZoomLevel = mp.getMaximumZoomLevel();
		int zoom = mp.getMinimumZoomLevel();

		Rectangle positionRect = getPositionRect(positions, zoom);
		Rectangle viewport = _map.getWorldPixelViewport();

		// zoom in until the tour is larger than the viewport
		while ((positionRect.width < viewport.width) && (positionRect.height < viewport.height)) {

			// center position in the map
			final Point center = new Point(//
					positionRect.x + positionRect.width / 2,
					positionRect.y + positionRect.height / 2);

			_map.setMapCenter(mp.pixelToGeo(center, zoom));

			zoom++;

			// check zoom level
			if (zoom >= maximumZoomLevel) {
				break;
			}
			_map.setZoom(zoom);

			positionRect = getPositionRect(positions, zoom);
			viewport = _map.getWorldPixelViewport();
		}

		// the algorithm generated a larger zoom level as necessary
		zoom--;

		int adjustedZoomLevel = 0;
		if (isAdjustZoomLevel) {
			adjustedZoomLevel = _tourPainterConfig.getSynchTourZoomLevel();
		}

		_map.setZoom(zoom + adjustedZoomLevel);
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

		_map.setMapCenter(new GeoPosition(latitudeSerie[sliderIndex], longitudeSerie[sliderIndex]));

	}

	private void showDefaultMap(final boolean isShowOverlays) {

		// disable tour actions in this view
		_isTourOrWayPoint = false;

		// disable tour data
		_allTourData.clear();
		_previousTourData = null;
		_tourPainterConfig.resetTourData();

		// update direct painter to draw nothing
		_directMappingPainter.setPaintContext(_map, false, null, 0, 0, false, false);

		_map.setShowOverlays(isShowOverlays);
		_map.setShowLegend(false);

		_map.paint();
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

					_prefStore.setValue(
							ITourbookPreferences.MAP_VIEW_CONFIRMATION_SHOW_DIM_WARNING,
							dialog.getToggleState());
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
				if (_allTourData.size() > 0) {
					return;
				}

				final ArrayList<TourData> tourDataList = TourManager.getSelectedTours();
				if (tourDataList != null) {

					_allTourData.clear();
					_allTourData.addAll(tourDataList);

					paintTours_10_All();
				}
			}
		});
	}

	private void updateFilteredPhotos() {

		runPhotoFilter();

		_map.disposeOverlayImageCache();
		_map.paint();
	}
}
