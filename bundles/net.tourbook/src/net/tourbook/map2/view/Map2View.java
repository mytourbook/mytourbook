/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import de.byteholder.geoclipse.GeoclipseExtensions;
import de.byteholder.geoclipse.map.IMapContextProvider;
import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.MapGridData;
import de.byteholder.geoclipse.map.MapLegend;
import de.byteholder.geoclipse.map.event.IMapGridListener;
import de.byteholder.geoclipse.map.event.IMapInfoListener;
import de.byteholder.geoclipse.map.event.IMapPositionListener;
import de.byteholder.geoclipse.map.event.IPOIListener;
import de.byteholder.geoclipse.map.event.IPositionListener;
import de.byteholder.geoclipse.map.event.ITourSelectionListener;
import de.byteholder.geoclipse.map.event.MapPOIEvent;
import de.byteholder.geoclipse.map.event.MapPositionEvent;
import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;
import de.byteholder.gpx.PointOfInterest;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.color.ColorProviderConfig;
import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.ITourToolTipProvider;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourWayPoint;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.map.IMapSyncListener;
import net.tourbook.map.MapColorProvider;
import net.tourbook.map.MapInfoManager;
import net.tourbook.map.MapManager;
import net.tourbook.map.MapUtils;
import net.tourbook.map.bookmark.ActionMapBookmarks;
import net.tourbook.map.bookmark.IMapBookmarkListener;
import net.tourbook.map.bookmark.IMapBookmarks;
import net.tourbook.map.bookmark.MapBookmark;
import net.tourbook.map.bookmark.MapBookmarkManager;
import net.tourbook.map.bookmark.MapLocation;
import net.tourbook.map2.Messages;
import net.tourbook.map2.action.ActionDimMap;
import net.tourbook.map2.action.ActionManageMapProviders;
import net.tourbook.map2.action.ActionMap2Color;
import net.tourbook.map2.action.ActionPhotoProperties;
import net.tourbook.map2.action.ActionReloadFailedMapImages;
import net.tourbook.map2.action.ActionSaveDefaultPosition;
import net.tourbook.map2.action.ActionSelectMapProvider;
import net.tourbook.map2.action.ActionSetDefaultPosition;
import net.tourbook.map2.action.ActionShowAllFilteredPhotos;
import net.tourbook.map2.action.ActionShowLegendInMap;
import net.tourbook.map2.action.ActionShowPOI;
import net.tourbook.map2.action.ActionShowPhotos;
import net.tourbook.map2.action.ActionShowScaleInMap;
import net.tourbook.map2.action.ActionShowSliderInLegend;
import net.tourbook.map2.action.ActionShowSliderInMap;
import net.tourbook.map2.action.ActionShowStartEndInMap;
import net.tourbook.map2.action.ActionShowTourInfoInMap;
import net.tourbook.map2.action.ActionShowTourMarker;
import net.tourbook.map2.action.ActionShowWayPoints;
import net.tourbook.map2.action.ActionSyncMapWithOtherMap;
import net.tourbook.map2.action.ActionSyncMapWithPhoto;
import net.tourbook.map2.action.ActionSyncMapWithSlider;
import net.tourbook.map2.action.ActionSyncMapWithTour;
import net.tourbook.map2.action.ActionSyncZoomLevelAdjustment;
import net.tourbook.map2.action.ActionTourColor;
import net.tourbook.map2.action.ActionZoomCentered;
import net.tourbook.map2.action.ActionZoomIn;
import net.tourbook.map2.action.ActionZoomOut;
import net.tourbook.map2.action.ActionZoomShowEntireMap;
import net.tourbook.map2.action.ActionZoomShowEntireTour;
import net.tourbook.photo.IPhotoEventListener;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoEventId;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.PhotoSelection;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageMap2Appearance;
import net.tourbook.srtm.IPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourInfoIconToolTipProvider;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.filter.geo.GeoFilter_LoaderData;
import net.tourbook.tour.filter.geo.TourGeoFilter;
import net.tourbook.tour.filter.geo.TourGeoFilter_Loader;
import net.tourbook.tour.filter.geo.TourGeoFilter_Manager;
import net.tourbook.tour.photo.DialogPhotoProperties;
import net.tourbook.tour.photo.IPhotoPropertiesListener;
import net.tourbook.tour.photo.PhotoPropertiesEvent;
import net.tourbook.tour.photo.TourPhotoLink;
import net.tourbook.tour.photo.TourPhotoLinkSelection;
import net.tourbook.training.TrainingManager;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.geoCompare.GeoPartComparerItem;
import net.tourbook.ui.views.tourCatalog.ReferenceTourManager;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;
import net.tourbook.ui.views.tourSegmenter.SelectedTourSegmenterSegments;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.oscim.core.MapPosition;

/**
 * @author Wolfgang Schramm
 * @since 1.3.0
 */
public class Map2View extends ViewPart implements

      IMapContextProvider,
      IPhotoEventListener,
      IPhotoPropertiesListener,
      IMapBookmarks,
      IMapBookmarkListener,
      IMapPositionListener,
      IMapSyncListener,
      IMapInfoListener {

// SET_FORMATTING_OFF

   public static final String    ID                                                    = "net.tourbook.map2.view.Map2ViewId";                  //$NON-NLS-1$

   private static final String   IMAGE_GRAPH                                           = net.tourbook.Messages.Image__Graph;
   private static final String   IMAGE_GRAPH_DISABLED                                  = net.tourbook.Messages.Image__Graph_Disabled;
   private static final String   IMAGE_GRAPH_ALTITUDE                                  = net.tourbook.Messages.Image__graph_altitude;
   private static final String   IMAGE_GRAPH_ALTITUDE_DISABLED                         = net.tourbook.Messages.Image__graph_altitude_disabled;
   private static final String   IMAGE_GRAPH_GRADIENT                                  = net.tourbook.Messages.Image__graph_gradient;
   private static final String   IMAGE_GRAPH_GRADIENT_DISABLED                         = net.tourbook.Messages.Image__graph_gradient_disabled;
   private static final String   IMAGE_GRAPH_PACE                                      = net.tourbook.Messages.Image__graph_pace;
   private static final String   IMAGE_GRAPH_PACE_DISABLED                             = net.tourbook.Messages.Image__graph_pace_disabled;
   private static final String   IMAGE_GRAPH_PULSE                                     = net.tourbook.Messages.Image__graph_heartbeat;
   private static final String   IMAGE_GRAPH_PULSE_DISABLED                            = net.tourbook.Messages.Image__graph_heartbeat_disabled;
   private static final String   IMAGE_GRAPH_SPEED                                     = net.tourbook.Messages.Image__graph_speed;
   private static final String   IMAGE_GRAPH_SPEED_DISABLED                            = net.tourbook.Messages.Image__graph_speed_disabled;
   private static final String   IMAGE_GRAPH_RUN_DYN_STEP_LENGTH                       = net.tourbook.Messages.Image__Graph_RunDyn_StepLength;
   private static final String   IMAGE_GRAPH_RUN_DYN_STEP_LENGTH_DISABLED              = net.tourbook.Messages.Image__Graph_RunDyn_StepLength_Disabled;
   private static final String   IMAGE_SEARCH_TOURS_BY_LOCATION                        = net.tourbook.Messages.Image__SearchToursByLocation;

   private static final String   STATE_IS_SHOW_TOUR_IN_MAP                             = "STATE_IS_SHOW_TOUR_IN_MAP";                          //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_PHOTO_IN_MAP                            = "STATE_IS_SHOW_PHOTO_IN_MAP";                         //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_LEGEND_IN_MAP                           = "STATE_IS_SHOW_LEGEND_IN_MAP";                        //$NON-NLS-1$
   public static final String    STATE_IS_SHOW_HOVERED_SELECTED_TOUR                    = "STATE_IS_SHOW_HOVERED_SELECTED_TOUR";                 //$NON-NLS-1$
   public static final boolean   STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT            = true;
   private static final String   STATE_IS_SYNC_MAP2_WITH_OTHER_MAP                     = "STATE_IS_SYNC_MAP2_WITH_OTHER_MAP";                  //$NON-NLS-1$
   private static final String   STATE_IS_SYNC_WITH_PHOTO                              = "STATE_IS_SYNC_WITH_PHOTO";                           //$NON-NLS-1$
   private static final String   STATE_IS_SYNC_WITH_TOURCHART_SLIDER                   = "STATE_IS_SYNC_WITH_TOURCHART_SLIDER";                //$NON-NLS-1$
   private static final String   STATE_IS_SYNC_WITH_TOURCHART_SLIDER_IS_CENTERED       = "STATE_IS_SYNC_WITH_TOURCHART_SLIDER_IS_CENTERED";    //$NON-NLS-1$
   static final String           STATE_IS_ZOOM_WITH_MOUSE_POSITION                     = "STATE_IS_ZOOM_WITH_MOUSE_POSITION";                  //$NON-NLS-1$
   static final boolean          STATE_IS_ZOOM_WITH_MOUSE_POSITION_DEFAULT             = true;

   private static final String   MEMENTO_SHOW_START_END_IN_MAP                         = "action.show-start-end-in-map";                       //$NON-NLS-1$
   private static final String   MEMENTO_SHOW_TOUR_MARKER                              = "action.show-tour-marker";                            //$NON-NLS-1$
   static final String           MEMENTO_SHOW_SLIDER_IN_MAP                            = "action.show-slider-in-map";                          //$NON-NLS-1$
   static final boolean          MEMENTO_SHOW_SLIDER_IN_MAP_DEFAULT                    = true;
   private static final String   MEMENTO_SHOW_SLIDER_IN_LEGEND                         = "action.show-slider-in-legend";                       //$NON-NLS-1$
   private static final String   MEMENTO_SHOW_SCALE_IN_MAP                             = "action.show-scale-in-map";                           //$NON-NLS-1$
   private static final String   MEMENTO_SHOW_TOUR_INFO_IN_MAP                         = "action.show-tour-info-in-map";                       //$NON-NLS-1$
   private static final String   MEMENTO_SHOW_WAY_POINTS                               = "action.show-way-points-in-map";                      //$NON-NLS-1$
   private static final String   MEMENTO_SYNCH_WITH_SELECTED_TOUR                      = "action.synch-with-selected-tour";                    //$NON-NLS-1$
   private static final String   MEMENTO_ZOOM_CENTERED                                 = "action.zoom-centered";                               //$NON-NLS-1$
   private static final String   MEMENTO_MAP_DIM_LEVEL                                 = "action.map-dim-level";                               //$NON-NLS-1$

   private static final String   MEMENTO_SYNCH_TOUR_ZOOM_LEVEL                         = "synch-tour-zoom-level";                              //$NON-NLS-1$
   private static final String   MEMENTO_SELECTED_MAP_PROVIDER_ID                      = "selected.map-provider-id";                           //$NON-NLS-1$

   private static final String   MEMENTO_DEFAULT_POSITION_ZOOM                         = "default.position.zoom-level";                        //$NON-NLS-1$
   private static final String   MEMENTO_DEFAULT_POSITION_LATITUDE                     = "default.position.latitude";                          //$NON-NLS-1$
   private static final String   MEMENTO_DEFAULT_POSITION_LONGITUDE                    = "default.position.longitude";                         //$NON-NLS-1$

   private static final String   MEMENTO_TOUR_COLOR_ID                                 = "tour-color-id";                                      //$NON-NLS-1$

   static final String           PREF_DEBUG_MAP_DIM_LEVEL                              = "MapDebug.MapDimLevel";                               //$NON-NLS-1$
   static final String           PREF_DEBUG_MAP_SHOW_GEO_GRID                          = "PREF_DEBUG_MAP_SHOW_GEO_GRID";                       //$NON-NLS-1$
   static final String           PREF_SHOW_TILE_INFO                                   = "MapDebug.ShowTileInfo";                              //$NON-NLS-1$
   static final String           PREF_SHOW_TILE_BORDER                                 = "MapDebug.ShowTileBorder";                            //$NON-NLS-1$
   //
   static final String           STATE_IS_SHOW_IN_TOOLBAR_ALTITUDE                     = "STATE_IS_SHOW_IN_TOOLBAR_ALTITUDE";                  //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_GRADIENT                     = "STATE_IS_SHOW_IN_TOOLBAR_GRADIENT";                  //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_PACE                         = "STATE_IS_SHOW_IN_TOOLBAR_PACE";                      //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_PULSE                        = "STATE_IS_SHOW_IN_TOOLBAR_PULSE";                     //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_SPEED                        = "STATE_IS_SHOW_IN_TOOLBAR_SPEED";                     //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_HR_ZONE                      = "STATE_IS_SHOW_IN_TOOLBAR_HR_ZONE";                   //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_RUN_DYN_STEP_LENGTH          = "STATE_IS_SHOW_IN_TOOLBAR_RUN_DYN_STEP_LENGTH";       //$NON-NLS-1$
   //
   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_ALTITUDE_DEFAULT             = true;
   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_GRADIENT_DEFAULT             = false;
   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_PACE_DEFAULT                 = false;
   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_PULSE_DEFAULT                = true;
   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_SPEED_DEFAULT                = false;
   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_HR_ZONE_DEFAULT              = false;
   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_RUN_DYN_STEP_LENGTH_DEFAULT  = true;
   //
   static final String           STATE_IS_SHOW_SLIDER_PATH                             = "STATE_IS_SHOW_SLIDER_PATH";                          //$NON-NLS-1$
   static final String           STATE_SLIDER_PATH_LINE_WIDTH                          = "STATE_SLIDER_PATH_LINE_WIDTH";                       //$NON-NLS-1$
   static final String           STATE_SLIDER_PATH_OPACITY                             = "STATE_SLIDER_PATH_OPACITY";                          //$NON-NLS-1$
   static final String           STATE_SLIDER_PATH_SEGMENTS                            = "STATE_SLIDER_PATH_SEGMENTS";                         //$NON-NLS-1$
   static final String           STATE_SLIDER_PATH_COLOR                               = "STATE_SLIDER_PATH_COLOR";                            //$NON-NLS-1$
   //
   static final boolean          STATE_IS_SHOW_SLIDER_PATH_DEFAULT                     = true;
   static final int              STATE_SLIDER_PATH_LINE_WIDTH_DEFAULT                  = 30;
   static final int              STATE_SLIDER_PATH_OPACITY_DEFAULT                     = 60;
   static final int              STATE_SLIDER_PATH_SEGMENTS_DEFAULT                    = 200;
   static final RGB              STATE_SLIDER_PATH_COLOR_DEFAULT                       = new RGB(0xff, 0xff, 0x80);
   //
   private static final String   GRAPH_CONTRIBUTION_ID_SLIDEOUT                        = "GRAPH_CONTRIBUTION_ID_SLIDEOUT";                     //$NON-NLS-1$
   //
   private static final MapGraphId[] _allGraphContribId                                   = {

         MapGraphId.Altitude,
         MapGraphId.Gradient,
         MapGraphId.Pace,
         MapGraphId.Pulse,
         MapGraphId.Speed,

         MapGraphId.RunDyn_StepLength,

         MapGraphId.HrZone,
   };


   private final IPreferenceStore   _prefStore                             = TourbookPlugin.getPrefStore();
   private final IDialogSettings    _state                                 = TourbookPlugin.getState(ID);

   private final ImageDescriptor    _imageSyncWithSlider                   = TourbookPlugin.getImageDescriptor(Messages.image_action_synch_with_slider);
   private final ImageDescriptor    _imageSyncWithSlider_Disabled          = TourbookPlugin.getImageDescriptor(Messages.image_action_synch_with_slider_disabled);
   private final ImageDescriptor    _imageSyncWithSlider_Centered          = TourbookPlugin.getImageDescriptor(Messages.Image_Action_SynchWithSlider_Centered);
   private final ImageDescriptor    _imageSyncWithSlider_Centered_Disabled = TourbookPlugin.getImageDescriptor(Messages.Image_Action_SynchWithSlider_Centered_Disabled);

// SET_FORMATTING_ON

   private final TourInfoIconToolTipProvider _tourInfoToolTipProvider = new TourInfoIconToolTipProvider(2, 32);
   private final ITourToolTipProvider        _wayPointToolTipProvider = new WayPointToolTipProvider();

   private final DirectMappingPainter        _directMappingPainter    = new DirectMappingPainter();
   private final MapInfoManager              _mapInfoManager          = MapInfoManager.getInstance();

   private final TourPainterConfiguration    _tourPainterConfig       = TourPainterConfiguration.getInstance();

   private boolean                           _isPartVisible;

   /**
    * contains selection which was set when the part is hidden
    */
   private ISelection                        _selectionWhenHidden;
   private IPartListener2                    _partListener;
   private ISelectionListener                _postSelectionListener;
   private IPropertyChangeListener           _prefChangeListener;
   private ITourEventListener                _tourEventListener;

   /**
    * Contains all tours which are displayed in the map.
    */
   private final ArrayList<TourData>         _allTourData             = new ArrayList<>();
   private TourData                          _previousTourData;

   /**
    * contains photos which are displayed in the map
    */
   private final ArrayList<Photo>            _allPhotos               = new ArrayList<>();

   private final ArrayList<Photo>            _filteredPhotos          = new ArrayList<>();

   private boolean                           _isPhotoFilterActive;
   private int                               _photoFilterRatingStars;
   private int                               _photoFilterRatingStarOperator;

   private boolean                           _isShowTour;
   private boolean                           _isShowPhoto;
   private boolean                           _isShowLegend;

   private boolean                           _isMapSynched_WithOtherMap;
   private boolean                           _isMapSynched_WithPhoto;
   private boolean                           _isMapSynched_WithChartSlider;
   private boolean                           _isMapSynched_WithChartSlider_IsCentered;
   private boolean                           _isMapSynched_WithTour;
   private boolean                           _isPositionCentered;

   private boolean                           _isInSelectBookmark;
   private boolean                           _isInZoom;

   private int                               _defaultZoom;

   private GeoPosition                       _defaultPosition;

   /**
    * when <code>true</code> a tour is painted, <code>false</code> a point of interrest is painted
    */
   private boolean                           _isTourOrWayPoint;

   /*
    * tool tips
    */
   private TourToolTip _tourToolTip;
   private String      _poiName;
   private GeoPosition _poiPosition;
   private int         _poiZoomLevel;

   /*
    * current position for the x-sliders
    */
   private int                            _currentLeftSliderValueIndex;
   private int                            _currentRightSliderValueIndex;

   private int                            _currentSelectedSliderValueIndex;

   private MapLegend                      _mapLegend;

   private long                           _previousOverlayKey;
   private int                            _mapDimLevel         = -1;

   private RGB                            _mapDimColor;

   private int                            _selectedProfileKey  = 0;

   private MapGraphId                     _tourColorId;
   //
   private int                            _hash_AllTourIds;
   private int                            _hash_AllTourData;
   private long                           _hash_TourOverlayKey;
   private int                            _hash_AllPhotos;

   private final AtomicInteger            _asyncCounter        = new AtomicInteger();

   /**
    * Is <code>true</code> when a link photo is displayed, otherwise a tour photo (photo which is
    * save in a tour) is displayed.
    */
   private boolean                        _isLinkPhotoDisplayed;
   private SliderPathPaintingData         _sliderPathPaintingData;
   private OpenDialogManager              _openDlgMgr          = new OpenDialogManager();

   private boolean                        _isInMapSync;
   private long                           _lastFiredSyncEventTime;

   private HashMap<MapGraphId, Action>    _allTourColorActions = new HashMap<>();
   private ActionTourColor                _actionTourColorAltitude;
   private ActionTourColor                _actionTourColorGradient;
   private ActionTourColor                _actionTourColorPulse;
   private ActionTourColor                _actionTourColorSpeed;
   private ActionTourColor                _actionTourColorPace;
   private ActionTourColor                _actionTourColorHrZone;
   private ActionTourColor                _actionTourColor_RunDyn_StepLength;

   private ActionDimMap                   _actionDimMap;
   private ActionOpenPrefDialog           _actionEditMap2Preferences;
   private ActionMap2_Options             _actionMap2_Options;
   private ActionMapBookmarks             _actionMap2_Bookmarks;
   private ActionMap2Color                _actionMap2_Color;
   private ActionMap2_Graphs              _actionMap2_TourColors;
   private ActionManageMapProviders       _actionManageProvider;
   private ActionPhotoProperties          _actionPhotoFilter;
   private ActionReloadFailedMapImages    _actionReloadFailedMapImages;
   private ActionSaveDefaultPosition      _actionSaveDefaultPosition;
   private ActionSearchTourByLocation     _actionSearchTourByLocation;
   private ActionSelectMapProvider        _actionSelectMapProvider;
   private ActionSetDefaultPosition       _actionSetDefaultPosition;
   private ActionShowAllFilteredPhotos    _actionShowAllFilteredPhotos;
   private ActionShowLegendInMap          _actionShowLegendInMap;
   private ActionShowPhotos               _actionShowPhotos;
   private ActionShowPOI                  _actionShowPOI;
   private ActionShowScaleInMap           _actionShowScaleInMap;
   private ActionShowSliderInMap          _actionShowSliderInMap;
   private ActionShowSliderInLegend       _actionShowSliderInLegend;
   private ActionShowStartEndInMap        _actionShowStartEndInMap;
   private ActionShowTour                 _actionShowTour;
   private ActionShowTourInfoInMap        _actionShowTourInfoInMap;
   private ActionShowTourMarker           _actionShowTourMarker;
   private ActionShowWayPoints            _actionShowWayPoints;
   private ActionSyncZoomLevelAdjustment  _actionSyncZoomLevelAdjustment;
   private ActionSyncMapWithOtherMap      _actionSyncMap_WithOtherMap;
   private ActionSyncMapWithPhoto         _actionSyncMap_WithPhoto;
   private ActionSyncMapWithSlider        _actionSyncMap_WithChartSlider;
   private ActionSyncMapWithTour          _actionSyncMap_WithTour;

   private ActionZoomIn                   _actionZoom_In;
   private ActionZoomOut                  _actionZoom_Out;
   private ActionZoomCentered             _actionZoom_Centered;
   private ActionZoomShowEntireMap        _actionZoom_ShowEntireMap;

   private ActionZoomShowEntireTour       _actionZoom_ShowEntireTour;

   private org.eclipse.swt.graphics.Point _geoFilter_Loaded_TopLeft_E2;
   private org.eclipse.swt.graphics.Point _geoFilter_Loaded_BottomRight_E2;
   private GeoFilter_LoaderData           _geoFilter_PreviousGeoLoaderItem;
   private AtomicInteger                  _geoFilter_RunningId = new AtomicInteger();

   /*
    * UI controls
    */
   private Composite _parent;
   private Map       _map;

   private class ActionMap2_Graphs extends ActionToolbarSlideout {

      public ActionMap2_Graphs(final ImageDescriptor imageDescriptor,
                               final ImageDescriptor imageDescriptorDisabled) {

         super(imageDescriptor, imageDescriptorDisabled);

         setId(GRAPH_CONTRIBUTION_ID_SLIDEOUT);
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new Slideout_Map2_TourColors(_parent, toolbar, Map2View.this, _state);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }
   }

   private class ActionMap2_Options extends ActionToolbarSlideout {

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new Slideout_Map2_Options(_parent, toolbar, Map2View.this, _state);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }
   }

   public class ActionSearchTourByLocation extends Action {

      public ActionSearchTourByLocation() {

         setText(Messages.Map_Action_SearchTourByLocation);
         setImageDescriptor(TourbookPlugin.getImageDescriptor(IMAGE_SEARCH_TOURS_BY_LOCATION));
      }

      @Override
      public void runWithEvent(final Event event) {

         _map.actionSearchTourByLocation(event);
      }
   }

   private class ActionShowTour extends ActionToolbarSlideout {

      public ActionShowTour() {

         super(TourbookPlugin.getImageDescriptor(Messages.Image__Tour),
               TourbookPlugin.getImageDescriptor(Messages.Image__Tour_Disabled));

         isToggleAction = true;
         notSelectedTooltip = Messages.map_action_show_tour_in_map;
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {
         return new Slideout_Map2_TrackOptions(_parent, toolbar, Map2View.this, _state);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }

      @Override
      protected void onSelect() {

         super.onSelect();

         _isShowTour = getSelection();

         paintTours_10_All();
      }
   }

   public Map2View() {}

   public void action_SyncWith_ChartSlider() {

      if (_allTourData.size() == 0) {
         return;
      }

      /*
       * Change state
       */
      boolean isSync = _isMapSynched_WithChartSlider;
      boolean isCentered = _isMapSynched_WithChartSlider_IsCentered;

      if (isSync && isCentered) {

         isSync = true;
         isCentered = false;

      } else if (isSync) {

         isSync = false;
         isCentered = false;

      } else {

         isSync = true;
         isCentered = true;
      }

      _isMapSynched_WithChartSlider = isSync;
      _isMapSynched_WithChartSlider_IsCentered = isCentered;

      updateChartSliderAction();

      if (_isMapSynched_WithChartSlider) {

         deactivateMapSync();
         deactivatePhotoSync();

         _actionShowTour.setSelection(true);

         // map must be synched with selected tour
         _isMapSynched_WithTour = true;
         _actionSyncMap_WithTour.setChecked(true);

         _map.setShowOverlays(true);

         final TourData firstTourData = _allTourData.get(0);

         positionMapTo_0_TourSliders(
               firstTourData,
               _currentLeftSliderValueIndex,
               _currentRightSliderValueIndex,
               _currentSelectedSliderValueIndex,
               null);
      }
   }

   public void action_SyncWith_OtherMap(final boolean isSelected) {

      _isMapSynched_WithOtherMap = isSelected;

      if (_isMapSynched_WithOtherMap) {

         deactivatePhotoSync();
         deactivateTourSync();
         deactivateSliderSync();
      }
   }

   /**
    * Sync map with photo
    */
   public void action_SyncWith_Photo() {

      _isMapSynched_WithPhoto = _actionSyncMap_WithPhoto.isChecked();

      if (_isMapSynched_WithPhoto) {

         deactivateMapSync();
         deactivateTourSync();
         deactivateSliderSync();

         centerPhotos(_filteredPhotos, false);

         _map.paint();
      }

      enableActions(true);
   }

   public void action_SyncWith_Tour() {

      if (_allTourData.size() == 0) {
         return;
      }

      _isMapSynched_WithTour = _actionSyncMap_WithTour.isChecked();

      if (_isMapSynched_WithTour) {

         deactivateMapSync();
         deactivatePhotoSync();

         // force tour to be repainted, that it is synched immediately
         _previousTourData = null;

         _actionShowTour.setSelection(true);
         _map.setShowOverlays(true);

         paintTours_20_One(_allTourData.get(0), true);

      } else {

         deactivateSliderSync();
      }
   }

   public void actionDimMap(final int dimLevel) {

      // check if the dim level/color was changed
      if (_mapDimLevel != dimLevel) {

         _mapDimLevel = dimLevel;

         /*
          * dim color is stored in the pref store and not in the memento
          */
         final RGB dimColor = PreferenceConverter.getColor(
               _prefStore,
               ITourbookPreferences.MAP_LAYOUT_MAP_DIMM_COLOR);

         _map.dimMap(dimLevel, dimColor);
      }
   }

   private void actionDimMap(final RGB dimColor) {

      if (_mapDimColor != dimColor) {

         _mapDimColor = dimColor;

         _map.dimMap(_mapDimLevel, dimColor);
      }
   }

   public void actionOpenMapProviderDialog() {

      final DialogModifyMapProvider dialog = new DialogModifyMapProvider(Display.getCurrent().getActiveShell());

      if (dialog.open() == Window.OK) {
         _actionSelectMapProvider.updateMapProviders();
      }
   }

   public void actionPhotoProperties(final boolean isFilterActive) {

      _isPhotoFilterActive = isFilterActive;

      updateFilteredPhotos();
   }

   public void actionPOI() {

      final boolean isShowPOI = _actionShowPOI.isChecked();

      _map.setShowPOI(isShowPOI);

      if (isShowPOI) {
         _map.setPoi(_poiPosition, _map.getZoom(), _poiName);
      }
   }

   public void actionReloadFailedMapImages() {

      _map.deleteFailedImageFiles();
      _map.resetAll();
   }

   public void actionSaveDefaultPosition() {

      _defaultZoom = _map.getZoom();
      _defaultPosition = _map.getMapGeoCenter();
   }

   public void actionSetDefaultPosition() {

      if (_defaultPosition == null) {

         _map.setZoom(_map.getMapProvider().getMinimumZoomLevel());
         _map.setMapCenter(new GeoPosition(0, 0));

      } else {

         _map.setZoom(_defaultZoom);
         _map.setMapCenter(_defaultPosition);
      }

      _map.paint();
   }

   public void actionSetShowScaleInMap() {

      final boolean isScaleVisible = _actionShowScaleInMap.isChecked();

      _map.setShowScale(isScaleVisible);
      _map.paint();
   }

   public void actionSetShowStartEndInMap() {

      _tourPainterConfig.isShowStartEndInMap = _actionShowStartEndInMap.isChecked();

      _map.disposeOverlayImageCache();
      _map.paint();
   }

   public void actionSetShowTourInfoInMap() {

      final boolean isVisible = _actionShowTourInfoInMap.isChecked();

      if (isVisible) {
         _tourToolTip.addToolTipProvider(_tourInfoToolTipProvider);
      } else {
         _tourToolTip.removeToolTipProvider(_tourInfoToolTipProvider);
      }

      _map.paint();
   }

   public void actionSetShowTourMarkerInMap() {

      _tourPainterConfig.isShowTourMarker = _actionShowTourMarker.isChecked();

      _map.disposeOverlayImageCache();
      _map.paint();
   }

   public void actionSetShowWayPointsInMap() {

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

   public void actionSetTourColor(final MapGraphId colorId) {

      /*
       * Uncheck all other tour color actions, they could be checked in the slideout because each
       * action has it's own toolbar !!!
       */
      for (final MapGraphId graphId : _allGraphContribId) {
         if (graphId != colorId) {
            getTourColorAction(graphId).setChecked(false);
         }
      }

      setTourPainterColorProvider(colorId);

      _map.disposeOverlayImageCache();
      _map.paint();

      createLegendImage(getColorProvider(colorId));
   }

   public void actionSetZoomCentered() {

      _isPositionCentered = _actionZoom_Centered.isChecked();

      _isInZoom = true;
      {
         centerTour();
      }
      _isInZoom = false;
   }

   public void actionShowLegend() {

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

   public void actionShowPhotos() {

      _isShowPhoto = _actionShowPhotos.isChecked();

      enableActions();

      _tourPainterConfig.isPhotoVisible = _isShowPhoto;

      _map.setOverlayKey(Integer.toString(_filteredPhotos.hashCode()));
      _map.disposeOverlayImageCache();

      _map.paint();
   }

   public void actionShowSlider() {

      if ((_allTourData == null) || (_allTourData.size() == 0)) {
         return;
      }

      final boolean isShowSliderInMap = _actionShowSliderInMap.isChecked();

      // keep state for the slideout
      _state.put(Map2View.MEMENTO_SHOW_SLIDER_IN_MAP, isShowSliderInMap);

      // repaint map
      _directMappingPainter.setPaintContext(
            _map,
            _isShowTour,
            _allTourData.get(0),
            _currentLeftSliderValueIndex,
            _currentRightSliderValueIndex,
            isShowSliderInMap,
            _actionShowSliderInLegend.isChecked(),
            _sliderPathPaintingData);

      _map.redraw();
   }

   public void actionZoomIn() {

      _isInZoom = true;
      {
         _map.setZoom(_map.getZoom() + 1);
      }
      _isInZoom = false;

      centerTour();

      _map.paint();
   }

   public void actionZoomOut() {

      _isInZoom = true;
      {
         _map.setZoom(_map.getZoom() - 1);
      }
      _isInZoom = false;

      centerTour();

      _map.paint();
   }

   public void actionZoomShowAllPhotos() {

      centerPhotos(_filteredPhotos, true);
   }

   public void actionZoomShowEntireMap() {

      _map.setMapCenter(new GeoPosition(0.0, 0.0));
      _map.setZoom(_map.getMapProvider().getMinimumZoomLevel());

      _map.paint();
   }

   public void actionZoomShowEntireTour() {

      /*
       * reset position for all tours, it was really annoying that previous selected tours moved to
       * their previous positions, implemented in 11.8.2
       */
      TourManager.getInstance().resetMapPositions();

      _actionShowTour.setSelection(true);
      _map.setShowOverlays(true);

      paintEntireTour();
   }

   private void addMapListener() {

      _map.addMousePositionListener(new IPositionListener() {
         @Override
         public void setPosition(final MapPositionEvent event) {

            _mapInfoManager.setMapPosition(
                  event.mapGeoPosition.latitude,
                  event.mapGeoPosition.longitude,
                  event.mapZoomLevel);
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

      _map.addTourSelectionListener(new ITourSelectionListener() {

         @Override
         public void onSelection(final ISelection selection, final boolean isSelectAlsoInThisView) {

            if (isSelectAlsoInThisView) {

               _map.getDisplay().asyncExec(new Runnable() {
                  @Override
                  public void run() {

                     if (selection instanceof SelectionTourIds) {

                        // clone tour id's otherwise the original will be removed
                        final SelectionTourIds selectionTourIds = (SelectionTourIds) selection;

                        final ArrayList<Long> allTourIds = new ArrayList<>();
                        allTourIds.addAll(selectionTourIds.getTourIds());

                        onSelectionChanged(new SelectionTourIds(allTourIds), false);
                     }
                  }
               });
            }

            TourManager.fireEventWithCustomData(
                  TourEventId.TOUR_SELECTION,
                  selection,
                  Map2View.this);
         }

      });

      _map.addMapGridBoxListener(new IMapGridListener() {

         @Override
         public void onMapGrid(final int map_ZoomLevel,
                               final GeoPosition map_GeoCenter,
                               final boolean isGridSelected,
                               final MapGridData mapGridData) {

            if (isGridSelected) {

               TourGeoFilter_Manager.createAndSetGeoFilter(map_ZoomLevel, map_GeoCenter, mapGridData);

            } else {

               geoFilter_10_Loader(mapGridData, null);
            }
         }
      });

      _map.addMapInfoListener(this);
      _map.addMapPositionListener(this);

      _map.setMapContextProvider(this);
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {

         private void onPartVisible(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == Map2View.this) {

               if (_isPartVisible == false) {

                  _isPartVisible = true;

                  if (_selectionWhenHidden != null) {

                     onSelectionChanged(_selectionWhenHidden, true);

                     _selectionWhenHidden = null;
                  }
               }
            }
         }

         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {
            onPartVisible(partRef);
         }

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {
            onPartVisible(partRef);
         }

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == Map2View.this) {
               _mapInfoManager.resetInfo();
            }
         }

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {
            if (partRef.getPart(false) == Map2View.this) {
               _isPartVisible = false;
            }
         }

         @Override
         public void partInputChanged(final IWorkbenchPartReference partRef) {}

         @Override
         public void partOpened(final IWorkbenchPartReference partRef) {
            onPartVisible(partRef);
         }

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {
            onPartVisible(partRef);
         }
      };
      getViewSite().getPage().addPartListener(_partListener);
   }

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(PREF_SHOW_TILE_INFO)
                  || property.equals(PREF_SHOW_TILE_BORDER)
                  || property.equals(PREF_DEBUG_MAP_SHOW_GEO_GRID)) {

               // map properties has changed

               final boolean isShowGeoGrid = _prefStore.getBoolean(PREF_DEBUG_MAP_SHOW_GEO_GRID);
               final boolean isShowTileInfo = _prefStore.getBoolean(PREF_SHOW_TILE_INFO);
               final boolean isShowTileBorder = _prefStore.getBoolean(PREF_SHOW_TILE_BORDER);

               _map.setShowDebugInfo(isShowTileInfo, isShowTileBorder, isShowGeoGrid);
               _map.paint();

            } else if (property.equals(PREF_DEBUG_MAP_DIM_LEVEL)) {

               float prefDimLevel = _prefStore.getInt(Map2View.PREF_DEBUG_MAP_DIM_LEVEL);
               prefDimLevel *= 2.55;
               prefDimLevel -= 255;

               final int dimLevel = (int) Math.abs(prefDimLevel);
               _actionDimMap.setDimLevel(dimLevel);
               actionDimMap(dimLevel);

            } else if (property.equals(ITourbookPreferences.MAP_LAYOUT_MAP_DIMM_COLOR)) {

               actionDimMap(PreferenceConverter.getColor(_prefStore, ITourbookPreferences.MAP_LAYOUT_MAP_DIMM_COLOR));

            } else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

               // measurement system has changed

               net.tourbook.ui.UI.updateUnits();
               _map.setMeasurementSystem(net.tourbook.ui.UI.UNIT_VALUE_DISTANCE, UI.UNIT_LABEL_DISTANCE);

               createLegendImage(_tourPainterConfig.getMapColorProvider());

               _map.paint();

            } else if (property.equals(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD)) {

               _map.setTourPaintMethodEnhanced(event.getNewValue().equals(PrefPageMap2Appearance.TOUR_PAINT_METHOD_COMPLEX));

            } else if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)
                  || property.equals(ITourbookPreferences.MAP2_OPTIONS_IS_MODIFIED)) {

               // update tour and legend

               createLegendImage(_tourPainterConfig.getMapColorProvider());

               _map.updateGraphColors();

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

            } else if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

               // this can occure when tour geo filter color is modified

               _map.paint();
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
            onSelectionChanged(selection, true);
         }
      };

      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == Map2View.this) {
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

            } else if (eventId == TourEventId.MARKER_SELECTION) {

               if (eventData instanceof SelectionTourMarker) {

                  onSelectionChanged_TourMarker((SelectionTourMarker) eventData, false);
               }

            } else if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData, true);

            } else if (eventId == TourEventId.SLIDER_POSITION_CHANGED && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData, true);

            } else if (eventId == TourEventId.MAP_SHOW_GEO_GRID) {

               if (eventData instanceof TourGeoFilter) {

                  // show geo filter

                  final TourGeoFilter tourGeoFilter = (TourGeoFilter) eventData;

                  // show search rectangle
                  _map.showGeoGrid(tourGeoFilter);

                  // show tours in search rectangle
                  geoFilter_10_Loader(tourGeoFilter.mapGridData, tourGeoFilter);

               } else if (eventData == null) {

                  // hide geo grid

                  hideGeoGrid();
               }

            } else if (eventId == TourEventId.SEGMENT_LAYER_CHANGED) {

               resetMap();
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

      final Rectangle positionRect = _map.getWorldPixelFromGeoPositions(positionBounds, zoom);

      final Point center = new Point(//
            positionRect.x + positionRect.width / 2,
            positionRect.y + positionRect.height / 2);

      final GeoPosition geoPosition = _map.getMapProvider().pixelToGeo(center, zoom);

      _map.setMapCenter(geoPosition);

      if (isForceZooming) {
         positionMapTo_MapPosition(positionBounds, false);
      }
   }

   /**
    * Center the tour in the map when action is enabled
    */
   private void centerTour() {

      if (_isInZoom && _isPositionCentered) {

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
            positionBounds = new HashSet<>();
            positionBounds.add(_poiPosition);
         }

         final Rectangle positionRect = _map.getWorldPixelFromGeoPositions(positionBounds, zoom);

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

      _map.tourBreadcrumb().resetTours();

      showDefaultMap(false);

      enableActions();
   }

   @Override
   public void closeOpenedDialogs(final IOpeningDialog openingDialog) {
      _openDlgMgr.closeOpenedDialogs(openingDialog);
   }

   private void createActions(final Composite parent) {

      _actionMap2_TourColors = new ActionMap2_Graphs(
            TourbookPlugin.getImageDescriptor(IMAGE_GRAPH),
            TourbookPlugin.getImageDescriptor(IMAGE_GRAPH_DISABLED));

      _actionTourColorAltitude = new ActionTourColor(
            this,
            MapGraphId.Altitude,
            Messages.map_action_tour_color_altitude_tooltip,
            IMAGE_GRAPH_ALTITUDE,
            IMAGE_GRAPH_ALTITUDE_DISABLED);

      _actionTourColorGradient = new ActionTourColor(
            this,
            MapGraphId.Gradient,
            Messages.map_action_tour_color_gradient_tooltip,
            IMAGE_GRAPH_GRADIENT,
            IMAGE_GRAPH_GRADIENT_DISABLED);

      _actionTourColorPulse = new ActionTourColor(
            this,
            MapGraphId.Pulse,
            Messages.map_action_tour_color_pulse_tooltip,
            IMAGE_GRAPH_PULSE,
            IMAGE_GRAPH_PULSE_DISABLED);

      _actionTourColorSpeed = new ActionTourColor(
            this,
            MapGraphId.Speed,
            Messages.map_action_tour_color_speed_tooltip,
            IMAGE_GRAPH_SPEED,
            IMAGE_GRAPH_SPEED_DISABLED);

      _actionTourColorPace = new ActionTourColor(
            this,
            MapGraphId.Pace,
            Messages.map_action_tour_color_pase_tooltip,
            IMAGE_GRAPH_PACE,
            IMAGE_GRAPH_PACE_DISABLED);

      _actionTourColor_RunDyn_StepLength = new ActionTourColor(
            this,
            MapGraphId.RunDyn_StepLength,
            Messages.Tour_Action_RunDyn_StepLength_Tooltip,
            IMAGE_GRAPH_RUN_DYN_STEP_LENGTH,
            IMAGE_GRAPH_RUN_DYN_STEP_LENGTH_DISABLED);

      _actionTourColorHrZone = new ActionTourColor(
            this,
            MapGraphId.HrZone,
            Messages.Tour_Action_ShowHrZones_Tooltip,
            Messages.Image__PulseZones,
            Messages.Image__PulseZones_Disabled);

      _allTourColorActions.put(MapGraphId.Altitude, _actionTourColorAltitude);
      _allTourColorActions.put(MapGraphId.Gradient, _actionTourColorGradient);
      _allTourColorActions.put(MapGraphId.Pulse, _actionTourColorPulse);
      _allTourColorActions.put(MapGraphId.Speed, _actionTourColorSpeed);
      _allTourColorActions.put(MapGraphId.Pace, _actionTourColorPace);
      _allTourColorActions.put(MapGraphId.HrZone, _actionTourColorHrZone);
      _allTourColorActions.put(MapGraphId.RunDyn_StepLength, _actionTourColor_RunDyn_StepLength);

      _actionZoom_In = new ActionZoomIn(this);
      _actionZoom_Out = new ActionZoomOut(this);
      _actionZoom_Centered = new ActionZoomCentered(this);
      _actionZoom_ShowEntireMap = new ActionZoomShowEntireMap(this);
      _actionZoom_ShowEntireTour = new ActionZoomShowEntireTour(this);

      _actionSyncMap_WithOtherMap = new ActionSyncMapWithOtherMap(this);
      _actionSyncMap_WithPhoto = new ActionSyncMapWithPhoto(this);
      _actionSyncMap_WithTour = new ActionSyncMapWithTour(this);
      _actionSyncMap_WithChartSlider = new ActionSyncMapWithSlider(this);
      _actionSyncZoomLevelAdjustment = new ActionSyncZoomLevelAdjustment();

      _actionEditMap2Preferences = new ActionOpenPrefDialog(Messages.Map_Action_Edit2DMapPreferences, PrefPageMap2Appearance.ID);

      _actionMap2_Color = new ActionMap2Color();
      _actionMap2_Options = new ActionMap2_Options();
      _actionSearchTourByLocation = new ActionSearchTourByLocation();
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
      _actionShowTour = new ActionShowTour();
      _actionShowTourInfoInMap = new ActionShowTourInfoInMap(this);
      _actionShowTourMarker = new ActionShowTourMarker(this);
      _actionShowWayPoints = new ActionShowWayPoints(this);

      _actionReloadFailedMapImages = new ActionReloadFailedMapImages(this);
      _actionDimMap = new ActionDimMap(this);
      _actionManageProvider = new ActionManageMapProviders(this);

      _actionMap2_Bookmarks = new ActionMapBookmarks(this._parent, this);
   }

   /**
    * Creates a new legend image and disposes the old image.
    *
    * @param mapColorProvider
    */
   private void createLegendImage(final IMapColorProvider mapColorProvider) {

      Image legendImage = _mapLegend.getImage();

      // legend requires a tour with coordinates
      if (mapColorProvider == null /* || isPaintDataValid(fTourData) == false */) {
         showDefaultMap(_isShowPhoto);
         return;
      }

      // dispose old legend image
      if ((legendImage != null) && !legendImage.isDisposed()) {
         legendImage.dispose();
      }
      final int legendWidth = IMapColorProvider.DEFAULT_LEGEND_WIDTH;
      int legendHeight = IMapColorProvider.DEFAULT_LEGEND_HEIGHT;

      final Rectangle mapBounds = _map.getBounds();
      legendHeight = Math.max(1, Math.min(legendHeight, mapBounds.height - IMapColorProvider.LEGEND_TOP_MARGIN));

      final RGB rgbTransparent = new RGB(0xfe, 0xfe, 0xfe);

      final ImageData overlayImageData = new ImageData(//
            legendWidth,
            legendHeight,
            24,
            new PaletteData(0xff, 0xff00, 0xff0000));

      overlayImageData.transparentPixel = overlayImageData.palette.getPixel(rgbTransparent);

      final Display display = Display.getCurrent();
      legendImage = new Image(display, overlayImageData);
      final Rectangle imageBounds = legendImage.getBounds();
      final int legendHeightNoMargin = imageBounds.height - 2 * IMapColorProvider.LEGEND_MARGIN_TOP_BOTTOM;

      boolean isDataAvailable = false;
      if (mapColorProvider instanceof IGradientColorProvider) {

         isDataAvailable = MapUtils.configureColorProvider(
               _allTourData,
               (IGradientColorProvider) mapColorProvider,
               ColorProviderConfig.MAP2,
               legendHeightNoMargin);

      } else if (mapColorProvider instanceof IDiscreteColorProvider) {

         isDataAvailable = createLegendImage_20_SetProviderValues(
               (IDiscreteColorProvider) mapColorProvider,
               legendHeightNoMargin);
      }

      final Color transparentColor = new Color(display, rgbTransparent);
      final GC gc = new GC(legendImage);
      {
         gc.setBackground(transparentColor);
         gc.fillRectangle(imageBounds);

         if (isDataAvailable) {
            TourMapPainter.drawMap2Legend(gc, imageBounds, mapColorProvider, true);
         } else {
            // draws only a transparent image to hide the legend
         }
      }
      gc.dispose();
      transparentColor.dispose();

      _mapLegend.setImage(legendImage);
   }

   private boolean createLegendImage_20_SetProviderValues(final IDiscreteColorProvider legendProvider,
                                                          final int legendHeight) {

      if (_allTourData.size() == 0) {
         return false;
      }

      // tell the legend provider how to draw the legend
      switch (legendProvider.getGraphId()) {

      case HrZone:

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

      _parent = parent;

      _mapLegend = new MapLegend();

      _map = new Map(parent, SWT.NONE, _state);
      _map.setPainting(false);

      _map.setDirectPainter(_directMappingPainter);
//      _map.setLiveView(true);

      _map.setLegend(_mapLegend);
      _map.setShowLegend(true);
      _map.setMeasurementSystem(net.tourbook.ui.UI.UNIT_VALUE_DISTANCE, UI.UNIT_LABEL_DISTANCE);

      final String tourPaintMethod = _prefStore.getString(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD);
      _map.setTourPaintMethodEnhanced(PrefPageMap2Appearance.TOUR_PAINT_METHOD_COMPLEX.equals(tourPaintMethod));

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

            if ((mapBounds.height < IMapColorProvider.DEFAULT_LEGEND_HEIGHT + IMapColorProvider.LEGEND_TOP_MARGIN)
                  || ((mapBounds.height > IMapColorProvider.DEFAULT_LEGEND_HEIGHT
                        + IMapColorProvider.LEGEND_TOP_MARGIN) //
                        && (legendBounds.height < IMapColorProvider.DEFAULT_LEGEND_HEIGHT)) //
            ) {

               createLegendImage(_tourPainterConfig.getMapColorProvider());
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
      MapBookmarkManager.addBookmarkListener(this);
      PhotoManager.addPhotoEventListener(this);
      MapManager.addMapSyncListener(this);

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
             * enable map drawing, this is done very late to disable flickering which is caused by
             * setting up the map
             */
            _map.setPainting(true);

            if (_mapDimLevel < 30) {
               showDimWarning();
            }
         }
      });
   }

   private void deactivateMapSync() {

      // disable map sync

      _isMapSynched_WithOtherMap = false;
      _actionSyncMap_WithOtherMap.setChecked(false);
   }

   private void deactivatePhotoSync() {

      // disable photo sync

      _isMapSynched_WithPhoto = false;
      _actionSyncMap_WithPhoto.setChecked(false);
   }

   private void deactivateSliderSync() {

      // disable slider sync

      _isMapSynched_WithChartSlider = false;
      _actionSyncMap_WithChartSlider.setChecked(false);
   }

   private void deactivateTourSync() {

      // disable tour sync

      _isMapSynched_WithTour = false;
      _actionSyncMap_WithTour.setChecked(false);
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

      MapBookmarkManager.removeBookmarkListener(this);
      MapManager.removeMapSyncListener(this);
      PhotoManager.removePhotoEventListener(this);
      TourManager.getInstance().removeTourEventListener(_tourEventListener);

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
//      final boolean isPhotoSynced = canShowFilteredPhoto && _isMapSynchedWithPhoto;
//      final boolean canSyncTour = isPhotoSynced == false;

      /*
       * photo actions
       */
      _actionPhotoFilter.setEnabled(isAllPhotoAvailable && _isShowPhoto);
      _actionShowAllFilteredPhotos.setEnabled(canShowFilteredPhoto);
      _actionShowPhotos.setEnabled(isAllPhotoAvailable);
      _actionSyncMap_WithPhoto.setEnabled(canShowFilteredPhoto);

      /*
       * tour actions
       */
      final int numberOfTours = _allTourData.size();
      final boolean isTourAvailable = numberOfTours > 0;
      final boolean isMultipleTours = numberOfTours > 1 && _isShowTour;
      final boolean isOneTour = _isTourOrWayPoint && (isMultipleTours == false) && _isShowTour;

      _actionMap2_Color.setEnabled(isTourAvailable);
      _actionShowLegendInMap.setEnabled(_isTourOrWayPoint);
      _actionShowSliderInLegend.setEnabled(_isTourOrWayPoint && _isShowLegend);
      _actionShowSliderInMap.setEnabled(_isTourOrWayPoint);
      _actionShowStartEndInMap.setEnabled(isOneTour);
      _actionShowTourInfoInMap.setEnabled(isOneTour);
      _actionShowTour.setEnabled(_isTourOrWayPoint);
      _actionShowTourMarker.setEnabled(_isTourOrWayPoint);
      _actionShowWayPoints.setEnabled(_isTourOrWayPoint);
      _actionZoom_Centered.setEnabled(isTourAvailable);
      _actionZoom_ShowEntireTour.setEnabled(_isTourOrWayPoint && _isShowTour && isTourAvailable);
      _actionSyncZoomLevelAdjustment.setEnabled(isTourAvailable);
      _actionSyncMap_WithOtherMap.setEnabled(true);
      _actionSyncMap_WithChartSlider.setEnabled(isTourAvailable);
      _actionSyncMap_WithTour.setEnabled(isTourAvailable);

      if (numberOfTours == 0) {

         _actionTourColorAltitude.setEnabled(false);
         _actionTourColorGradient.setEnabled(false);
         _actionTourColorPulse.setEnabled(false);
         _actionTourColorSpeed.setEnabled(false);
         _actionTourColorPace.setEnabled(false);
         _actionTourColorHrZone.setEnabled(false);
         _actionTourColor_RunDyn_StepLength.setEnabled(false);

      } else if (isForceTourColor) {

         _actionTourColorAltitude.setEnabled(true);
         _actionTourColorGradient.setEnabled(true);
         _actionTourColorPulse.setEnabled(true);
         _actionTourColorSpeed.setEnabled(true);
         _actionTourColorPace.setEnabled(true);
         _actionTourColorHrZone.setEnabled(true);
         _actionTourColor_RunDyn_StepLength.setEnabled(true);

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
         _actionTourColor_RunDyn_StepLength.setEnabled(oneTourData.runDyn_StepLength != null);

      } else {

         _actionTourColorAltitude.setEnabled(false);
         _actionTourColorGradient.setEnabled(false);
         _actionTourColorPulse.setEnabled(false);
         _actionTourColorSpeed.setEnabled(false);
         _actionTourColorPace.setEnabled(false);
         _actionTourColorHrZone.setEnabled(false);
         _actionTourColor_RunDyn_StepLength.setEnabled(false);
      }
   }

   private void fillActionBars() {

      /*
       * fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionMap2_TourColors);

      // must be called AFTER the tour color slideout action is added !!
      fillToolbar_TourColors(tbm);

      tbm.add(new Separator());

      tbm.add(_actionPhotoFilter);
      tbm.add(_actionShowPhotos);
      tbm.add(_actionShowAllFilteredPhotos);
      tbm.add(_actionSyncMap_WithPhoto);

      tbm.add(new Separator());

      tbm.add(_actionMap2_Bookmarks);

      tbm.add(new Separator());

      tbm.add(_actionShowTour);
      tbm.add(_actionZoom_ShowEntireTour);
      tbm.add(_actionSyncMap_WithTour);
      tbm.add(_actionSyncMap_WithChartSlider);
      tbm.add(_actionSyncMap_WithOtherMap);

      tbm.add(new Separator());

      tbm.add(_actionZoom_In);
      tbm.add(_actionZoom_Out);
      tbm.add(_actionZoom_ShowEntireMap);
      tbm.add(_actionZoom_Centered);

      tbm.add(new Separator());

      tbm.add(_actionSelectMapProvider);
      tbm.add(_actionMap2_Options);

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

      menuMgr.add(_actionSearchTourByLocation);
      menuMgr.add(new Separator());

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

      MapBookmarkManager.fillContextMenu_RecentBookmarks(menuMgr, this);

      menuMgr.add(_actionSetDefaultPosition);
      menuMgr.add(_actionSaveDefaultPosition);

      menuMgr.add(new Separator());

      // it can happen that color id is not yet set
      if (_tourColorId != null) {

         // set color before menu is filled, this sets the action image and color id
         _actionMap2_Color.setColorId(_tourColorId);

         if (_tourColorId != MapGraphId.HrZone) {

            // hr zone has a different color provider and is not yet supported

            menuMgr.add(_actionMap2_Color);
         }
      }

      menuMgr.add(_actionEditMap2Preferences);
      menuMgr.add(_actionDimMap);
      menuMgr.add(_actionSyncZoomLevelAdjustment);

      menuMgr.add(new Separator());
      menuMgr.add(_actionManageProvider);
      menuMgr.add(_actionReloadFailedMapImages);
   }

   private void fillToolbar_TourColors(final IToolBarManager tbm) {

      /*
       * Remove all previous tour color actions
       */
      for (final MapGraphId contribId : _allGraphContribId) {
         tbm.remove(contribId.name());
      }

      /*
       * Add requested tour color actions
       */
      fillToolbar_TourColors_Color(
            tbm,
            MapGraphId.Altitude,
            STATE_IS_SHOW_IN_TOOLBAR_ALTITUDE,
            STATE_IS_SHOW_IN_TOOLBAR_ALTITUDE_DEFAULT);

      fillToolbar_TourColors_Color(
            tbm,
            MapGraphId.Pulse,
            STATE_IS_SHOW_IN_TOOLBAR_PULSE,
            STATE_IS_SHOW_IN_TOOLBAR_PULSE_DEFAULT);

      fillToolbar_TourColors_Color(
            tbm,
            MapGraphId.Speed,
            STATE_IS_SHOW_IN_TOOLBAR_SPEED,
            STATE_IS_SHOW_IN_TOOLBAR_SPEED_DEFAULT);

      fillToolbar_TourColors_Color(
            tbm,
            MapGraphId.Pace,
            STATE_IS_SHOW_IN_TOOLBAR_PACE,
            STATE_IS_SHOW_IN_TOOLBAR_PACE_DEFAULT);

      fillToolbar_TourColors_Color(
            tbm,
            MapGraphId.Gradient,
            STATE_IS_SHOW_IN_TOOLBAR_GRADIENT,
            STATE_IS_SHOW_IN_TOOLBAR_GRADIENT_DEFAULT);

      fillToolbar_TourColors_Color(
            tbm,
            MapGraphId.RunDyn_StepLength,
            STATE_IS_SHOW_IN_TOOLBAR_RUN_DYN_STEP_LENGTH,
            STATE_IS_SHOW_IN_TOOLBAR_RUN_DYN_STEP_LENGTH_DEFAULT);

      fillToolbar_TourColors_Color(
            tbm,
            MapGraphId.HrZone,
            STATE_IS_SHOW_IN_TOOLBAR_HR_ZONE,
            STATE_IS_SHOW_IN_TOOLBAR_HR_ZONE_DEFAULT);
   }

   private void fillToolbar_TourColors_Color(final IToolBarManager tbm,
                                             final MapGraphId graphId,
                                             final String stateKey,
                                             final boolean stateDefaultValue) {

      if (Util.getStateBoolean(_state, stateKey, stateDefaultValue)) {

         tbm.insertBefore(
               GRAPH_CONTRIBUTION_ID_SLIDEOUT,
               _allTourColorActions.get(graphId));
      }
   }

   private void geoFilter_10_Loader(final MapGridData mapGridData,
                                    final TourGeoFilter tourGeoFilter) {

      final org.eclipse.swt.graphics.Point geoParts_TopLeft_E2 = mapGridData.geoParts_TopLeft_E2;
      final org.eclipse.swt.graphics.Point geoParts_BottomRight_E2 = mapGridData.geoParts_BottomRight_E2;

      // check if this area is already loaded
      if (_geoFilter_Loaded_TopLeft_E2 != null
            && geoParts_TopLeft_E2.equals(_geoFilter_Loaded_TopLeft_E2)
            && geoParts_BottomRight_E2.equals(_geoFilter_Loaded_BottomRight_E2)) {

         // this is already loaded

//         return;
      }

      final int runnableRunningId = _geoFilter_RunningId.incrementAndGet();

      TourGeoFilter_Loader.stopLoading(_geoFilter_PreviousGeoLoaderItem);

      // delay geo part loader, moving the mouse can occure very often
      _parent.getDisplay().timerExec(50, new Runnable() {

         private int __runningId = runnableRunningId;

         @Override
         public void run() {

            if (_parent.isDisposed()) {
               return;
            }

            final int currentId = _geoFilter_RunningId.get();

            if (__runningId != currentId) {

               // a newer runnable is created

               return;
            }

            _geoFilter_Loaded_TopLeft_E2 = geoParts_TopLeft_E2;
            _geoFilter_Loaded_BottomRight_E2 = geoParts_BottomRight_E2;

            _geoFilter_PreviousGeoLoaderItem = TourGeoFilter_Loader.loadToursFromGeoParts(
                  geoParts_TopLeft_E2,
                  geoParts_BottomRight_E2,
                  _geoFilter_PreviousGeoLoaderItem,
                  mapGridData,
                  Map2View.this,
                  tourGeoFilter);
         }
      });

   }

   /**
    * @param loaderItem
    * @param tourGeoFilter
    *           Can be <code>null</code> then the geo grid will be hidden.
    */
   public void geoFilter_20_ShowLoadedTours(final GeoFilter_LoaderData loaderItem, final TourGeoFilter tourGeoFilter) {

      // update UI
      Display.getDefault().asyncExec(() -> {

         if (_parent.isDisposed()) {
            return;
         }

         _map.showGeoGrid(tourGeoFilter);

         if (_isShowTour) {

            // show tours even when 0 are displayed

            final ArrayList<Long> allLoadedTourIds = loaderItem.allLoadedTourIds;

            // update map with the updated number of tours in a grid box
            paintTours(allLoadedTourIds);
         }

         enableActions();
      });
   }

   private IMapColorProvider getColorProvider(final MapGraphId colorId) {

//      final ColorDefinition colorDefinition = GraphColorManager.getInstance().getColorDefinition(colorId);
//
//      final IMapColorProvider colorProvider = colorDefinition.getMap2Color_Active();
//
//      return colorProvider;

      return MapColorProvider.getActiveMap2ColorProvider(colorId);
   }

   public Map getMap() {
      return _map;
   }

   public int getMapDimLevel() {
      return _mapDimLevel;
   }

   @Override
   public MapLocation getMapLocation() {

      final GeoPosition mapPosition = _map.getMapGeoCenter();
      final int mapZoomLevel = _map.getZoom() - 1;

      return new MapLocation(mapPosition, mapZoomLevel);
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

      final Set<GeoPosition> mapPositions = new HashSet<>();
      mapPositions.add(new GeoPosition(minLatitude, minLongitude));
      mapPositions.add(new GeoPosition(maxLatitude, maxLongitude));

      return mapPositions;
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

         final Set<GeoPosition> mapPositions = new HashSet<>();

         mapPositions.add(new GeoPosition(allMinLatitude, allMinLongitude));
         mapPositions.add(new GeoPosition(allMaxLatitude, allMaxLongitude));

         return mapPositions;
      }
   }

   IAction getTourColorAction(final MapGraphId graphId) {

      return _allTourColorActions.get(graphId);
   }

   private Set<GeoPosition> getXSliderGeoPositions(final TourData tourData,
                                                   int valueIndex1,
                                                   int valueIndex2) {

      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      final int serieSize = latitudeSerie.length;

      // check bounds -> this problem occured several times
      if (valueIndex1 >= serieSize) {
         valueIndex1 = serieSize - 1;
      }
      if (valueIndex2 >= serieSize) {
         valueIndex2 = serieSize - 1;
      }

      final GeoPosition leftPosition = new GeoPosition(latitudeSerie[valueIndex1], longitudeSerie[valueIndex1]);
      final GeoPosition rightPosition = new GeoPosition(latitudeSerie[valueIndex2], longitudeSerie[valueIndex2]);

      final Set<GeoPosition> mapPositions = new HashSet<>();

      mapPositions.add(leftPosition);
      mapPositions.add(rightPosition);

      return mapPositions;
   }

   private void hideGeoGrid() {

      _map.showGeoGrid(null);
   }

   private void keepMapPosition(final TourData tourData) {

      final GeoPosition centerPosition = _map.getMapGeoCenter();

      tourData.mapZoomLevel = _map.getZoom();
      tourData.mapCenterPositionLatitude = centerPosition.latitude;
      tourData.mapCenterPositionLongitude = centerPosition.longitude;
   }

   @Override
   public void moveToMapLocation(final MapBookmark mapBookmark) {

      _lastFiredSyncEventTime = System.currentTimeMillis();

      MapBookmarkManager.setLastSelectedBookmark(mapBookmark);

      final MapPosition mapPosition = mapBookmark.getMapPosition();

      _map.setZoom(mapPosition.zoomLevel + 1);
      _map.setMapCenter(new GeoPosition(mapPosition.getLatitude(), mapPosition.getLongitude()));
   }

   @Override
   public void onMapInfo(final GeoPosition mapCenter, final int mapZoomLevel) {

      _mapInfoManager.setMapPosition(mapCenter.latitude, mapCenter.longitude, mapZoomLevel);
   }

   @Override
   public void onMapPosition(final GeoPosition geoCenter, final int zoomLevel, final boolean isCenterTour) {

      if (_isInSelectBookmark) {
         // prevent fire the sync event
         return;
      }

      _isInZoom = isCenterTour;
      {
         centerTour();
      }
      _isInZoom = false;

      if (_isInMapSync) {
         return;
      }

      _lastFiredSyncEventTime = System.currentTimeMillis();

      final MapPosition mapPosition = new MapLocation(geoCenter, zoomLevel - 1).getMapPosition();

      MapManager.fireSyncMapEvent(mapPosition, this, 0);
   }

   @Override
   public void onSelectBookmark(final MapBookmark mapBookmark) {

      _isInSelectBookmark = true;
      {
         moveToMapLocation(mapBookmark);
      }
      _isInSelectBookmark = false;
   }

   /**
    * @param selection
    * @param isExternalEvent
    *           Is <code>true</code> when the event is from another part, otherwise
    *           <code>false</code> when the event is internal
    */
   private void onSelectionChanged(final ISelection selection, final boolean isExternalEvent) {

      if (_isPartVisible == false) {

         if (selection instanceof SelectionTourData
               || selection instanceof SelectionTourId
               || selection instanceof SelectionTourIds) {

            // keep only selected tours
            _selectionWhenHidden = selection;
         }
         return;
      }

      if (isExternalEvent) {
         _map.tourBreadcrumb().resetTours();
      }

      if (selection instanceof SelectionTourData) {

         hideGeoGrid();

         final SelectionTourData selectionTourData = (SelectionTourData) selection;
         final TourData tourData = selectionTourData.getTourData();

         paintTours_20_One(tourData, false);
         paintPhotoSelection(selection);

         enableActions();

      } else if (selection instanceof SelectionTourId) {

         hideGeoGrid();

         final SelectionTourId tourIdSelection = (SelectionTourId) selection;
         final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

         paintTours_20_One(tourData, false);
         paintPhotoSelection(selection);

         enableActions();

      } else if (selection instanceof SelectionTourIds) {

         // paint all selected tours

         hideGeoGrid();

         final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
         if (tourIds.size() == 0) {

            // history tour (without tours) is displayed

            final ArrayList<Photo> allPhotos = paintPhotoSelection(selection);

            if (allPhotos.size() > 0) {

//               centerPhotos(allPhotos, false);
               showDefaultMap(true);

               enableActions();
            }

         } else if (tourIds.size() == 1) {

            // only 1 tour is displayed, synch with this tour !!!

            final TourData tourData = TourManager.getInstance().getTourData(tourIds.get(0));

            paintTours_20_One(tourData, false);
            paintPhotoSelection(selection);

            enableActions();

         } else {

            // paint multiple tours

            paintTours(tourIds);
            paintPhotoSelection(selection);

            enableActions(true);
         }

      } else if (selection instanceof SelectionChartInfo) {

         final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

         TourData tourData = null;

         final Chart chart = chartInfo.getChart();
         if (chart instanceof TourChart) {
            final TourChart tourChart = (TourChart) chart;
            tourData = tourChart.getTourData();
         }

         if (tourData != null && tourData.isMultipleTours()) {

            // multiple tours are selected

         } else {

            // use old behaviour

            final ChartDataModel chartDataModel = chartInfo.chartDataModel;
            if (chartDataModel != null) {

               final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
               if (tourId instanceof Long) {

                  tourData = TourManager.getInstance().getTourData((Long) tourId);
                  if (tourData == null) {

                     // tour is not in the database, try to get it from the raw data manager

                     final HashMap<Long, TourData> rawData = RawDataManager.getInstance().getImportedTours();
                     tourData = rawData.get(tourId);
                  }
               }
            }
         }

         if (tourData != null) {

            positionMapTo_0_TourSliders(
                  tourData,
                  chartInfo.leftSliderValuesIndex,
                  chartInfo.rightSliderValuesIndex,
                  chartInfo.selectedSliderValuesIndex,
                  null);

            enableActions();
         }

      } else if (selection instanceof SelectionChartXSliderPosition) {

         final SelectionChartXSliderPosition xSliderPos = (SelectionChartXSliderPosition) selection;

         final Object customData = xSliderPos.getCustomData();
         if (customData instanceof SelectedTourSegmenterSegments) {

            /*
             * This event is fired in the tour chart when a toursegmenter segment is selected
             */

            selectTourSegments((SelectedTourSegmenterSegments) customData);

         } else {

            final Chart chart = xSliderPos.getChart();
            if (chart == null) {
               return;
            }

            final ChartDataModel chartDataModel = chart.getChartDataModel();
            final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);

            if (tourId instanceof Long) {

               final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
               if (tourData != null) {

                  final int leftSliderValueIndex = xSliderPos.getLeftSliderValueIndex();
                  int rightSliderValueIndex = xSliderPos.getRightSliderValueIndex();

                  rightSliderValueIndex =
                        rightSliderValueIndex == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
                              ? leftSliderValueIndex
                              : rightSliderValueIndex;

                  positionMapTo_0_TourSliders(//
                        tourData,
                        leftSliderValueIndex,
                        rightSliderValueIndex,
                        leftSliderValueIndex,
                        null);

                  enableActions();
               }
            }
         }

      } else if (selection instanceof SelectionTourMarker) {

         final SelectionTourMarker markerSelection = (SelectionTourMarker) selection;

         onSelectionChanged_TourMarker(markerSelection, true);

      } else if (selection instanceof SelectionMapPosition) {

         final SelectionMapPosition mapPositionSelection = (SelectionMapPosition) selection;

         final int valueIndex1 = mapPositionSelection.getSlider1ValueIndex();
         int valueIndex2 = mapPositionSelection.getSlider2ValueIndex();

         valueIndex2 = valueIndex2 == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
               ? valueIndex1
               : valueIndex2;

         positionMapTo_0_TourSliders(//
               mapPositionSelection.getTourData(),
               valueIndex1,
               valueIndex2,
               valueIndex1,
               null);

         enableActions();

      } else if (selection instanceof PointOfInterest) {

         _isTourOrWayPoint = false;

         clearView();

         final PointOfInterest poi = (PointOfInterest) selection;

         _poiPosition = poi.getPosition();
         _poiName = poi.getName();

         final String boundingBox = poi.getBoundingBox();
         if (boundingBox == null) {
            _poiZoomLevel = _map.getZoom();
         } else {
            _poiZoomLevel = _map.getZoom(boundingBox);
         }

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

            paintTours_20_One(tourData, false);

         } else if (firstElement instanceof TVICompareResultComparedTour) {

            final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
            final TourData tourData = TourManager.getInstance()
                  .getTourData(
                        compareResultItem.getComparedTourData().getTourId());

            paintTours_20_One(tourData, false);

         } else if (firstElement instanceof GeoPartComparerItem) {

            final TourData refTourData = ReferenceTourManager.getGeoCompareReferenceTour().getTourData();

            final GeoPartComparerItem geoCompareItem = (GeoPartComparerItem) firstElement;
            final long comparedTourId = geoCompareItem.tourId;
            final TourData comparedTourData = TourManager.getInstance().getTourData(comparedTourId);

            _allTourData.clear();

            _allTourData.add(refTourData);
            _allTourData.add(comparedTourData);
            _hash_AllTourData = _allTourData.hashCode();

            paintTours_10_All();

            positionMapTo_0_TourSliders(//
                  comparedTourData,
                  geoCompareItem.tourFirstIndex,
                  geoCompareItem.tourLastIndex,
                  geoCompareItem.tourFirstIndex,
                  null);

         } else if (firstElement instanceof TourWayPoint) {

            final TourWayPoint wp = (TourWayPoint) firstElement;

            final TourData tourData = wp.getTourData();

            paintTours_20_One(tourData, false);

            // delay to show the poi otherwise the map is being painted OVER the poi !!!
            _map.getDisplay().timerExec(500, new Runnable() {
               @Override
               public void run() {

                  _map.setPOI(_wayPointToolTipProvider, wp);
               }
            });

            enableActions();
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

            paintTours_20_One(tourData, false);

            enableActions();
         }

      } else if (selection instanceof SelectionDeletedTours) {

         clearView();
      }
   }

   private void onSelectionChanged_TourMarker(final SelectionTourMarker markerSelection, final boolean isDrawSlider) {

      final TourData tourData = markerSelection.getTourData();

      updateUI_ShowTour(tourData);

      final ArrayList<TourMarker> allTourMarker = markerSelection.getSelectedTourMarker();
      final int numberOfTourMarkers = allTourMarker.size();

      int leftSliderValueIndex = 0;
      int rightSliderValueIndex = 0;

      if (tourData.isMultipleTours()) {

         if (numberOfTourMarkers == 1) {

            leftSliderValueIndex = allTourMarker.get(0).getMultiTourSerieIndex();
            rightSliderValueIndex = leftSliderValueIndex;

         } else if (numberOfTourMarkers > 1) {

            leftSliderValueIndex = allTourMarker.get(0).getMultiTourSerieIndex();
            rightSliderValueIndex = allTourMarker.get(numberOfTourMarkers - 1).getMultiTourSerieIndex();
         }

      } else {

         if (numberOfTourMarkers == 1) {

            leftSliderValueIndex = allTourMarker.get(0).getSerieIndex();
            rightSliderValueIndex = leftSliderValueIndex;

         } else if (numberOfTourMarkers > 1) {

            leftSliderValueIndex = allTourMarker.get(0).getSerieIndex();
            rightSliderValueIndex = allTourMarker.get(numberOfTourMarkers - 1).getSerieIndex();
         }
      }

      if (_isMapSynched_WithTour || _isMapSynched_WithChartSlider) {

         if (isDrawSlider) {

            positionMapTo_0_TourSliders(//
                  tourData,
                  leftSliderValueIndex,
                  rightSliderValueIndex,
                  leftSliderValueIndex,
                  null);

         } else {

            positionMapTo_ValueIndex(tourData, leftSliderValueIndex);
         }

         keepMapPosition(tourData);
      }
   }

   private void paintEntireTour() {

      // get overlay key for all tours which have valid tour data
      long newOverlayKey = -1;
      for (final TourData tourData : _allTourData) {

         if (TourManager.isLatLonAvailable(tourData)) {
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

//      final TourData firstTourData = _allTourData.get(0);
//
//      // set slider position
//      _directMappingPainter.setPaintContext(
//            _map,
//            _isShowTour,
//            firstTourData,
//            _currentLeftSliderValueIndex,
//            _currentRightSliderValueIndex,
//            _actionShowSliderInMap.isChecked(),
//            _actionShowSliderInLegend.isChecked());

      final Set<GeoPosition> tourBounds = getTourBounds(_allTourData);

      _tourPainterConfig.setTourBounds(tourBounds);

      _directMappingPainter.disablePaintContext();

      _map.tourBreadcrumb().setTours(_allTourData);
      _map.setShowOverlays(_isShowTour || _isShowPhoto);
      _map.setShowLegend(_isShowTour && _isShowLegend);

      if (_previousOverlayKey != newOverlayKey) {

         _previousOverlayKey = newOverlayKey;

         _map.setOverlayKey(Long.toString(newOverlayKey));
         _map.disposeOverlayImageCache();
      }

      positionMapTo_MapPosition(tourBounds, false);

      createLegendImage(_tourPainterConfig.getMapColorProvider());

      _map.paint();
   }

   private void paintPhotos(final ArrayList<Photo> allNewPhotos) {

      /*
       * TESTING if a map redraw can be avoided, 15.6.2015
       */
// DISABLED BECAUSE PHOTOS ARE NOT ALWAYS DISPLAYED
      final int allNewPhotoHash = allNewPhotos.hashCode();
      if (allNewPhotoHash == _hash_AllPhotos) {
         return;
      }

      _allPhotos.clear();
      _allPhotos.addAll(allNewPhotos);
      _hash_AllPhotos = _allPhotos.hashCode();

      runPhotoFilter();

      if (_isShowPhoto && _isMapSynched_WithPhoto) {
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

      final ArrayList<Photo> allPhotos = new ArrayList<>();

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

   private void paintTours(final ArrayList<Long> allTourIds) {

      /*
       * TESTING if a map redraw can be avoided, 15.6.2015
       */
      final int allTourIds_Hash = allTourIds.hashCode();
      final int allTourData_Hash = _allTourData.hashCode();
      if (allTourIds_Hash == _hash_AllTourIds && allTourData_Hash == _hash_AllTourData) {
         return;
      }

      _isTourOrWayPoint = true;

      // force single tour to be repainted
      _previousTourData = null;

      _directMappingPainter.disablePaintContext();

      _map.setShowOverlays(_isShowTour || _isShowPhoto);
      _map.setShowLegend(_isShowTour && _isShowLegend);

      long newOverlayKey = _hash_TourOverlayKey;

      if (allTourIds.hashCode() != _hash_AllTourIds || _allTourData.hashCode() != _hash_AllTourData) {

         // tour data needs to be loaded

         newOverlayKey = TourManager.loadTourData(allTourIds, _allTourData, true);

         _hash_AllTourIds = allTourIds.hashCode();
         _hash_AllTourData = _allTourData.hashCode();
         _hash_TourOverlayKey = newOverlayKey;
      }

      _tourPainterConfig.setTourData(_allTourData, _isShowTour);
      _tourPainterConfig.setPhotos(_filteredPhotos, _isShowPhoto, _isLinkPhotoDisplayed);

      _tourInfoToolTipProvider.setTourDataList(_allTourData);

      _map.tourBreadcrumb().setTours(_allTourData);

      if (_previousOverlayKey != newOverlayKey) {

         _previousOverlayKey = newOverlayKey;

         _map.setOverlayKey(Long.toString(newOverlayKey));
         _map.disposeOverlayImageCache();
      }

      if (_isMapSynched_WithTour && !_map.isSearchTourByLocation()) {

         // use default position for the tour

         final Set<GeoPosition> tourBounds = getTourBounds(_allTourData);

         positionMapTo_MapPosition(tourBounds, true);
      }

      createLegendImage(_tourPainterConfig.getMapColorProvider());

      _map.paint();
   }

   /**
    * Show tours which are set in {@link #_allTourData}.
    */
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
         paintTours_20_One(_allTourData.get(0), true);
         enableActions();
      }

      paintPhotoSelection(null);
   }

   /**
    * Paint the currently selected tour in the map
    *
    * @param tourData
    * @param forceRedraw
    * @param isSynchronized
    *           when <code>true</code>, map will be synchronized
    */
   private void paintTours_20_One(final TourData tourData, final boolean forceRedraw) {

      _isTourOrWayPoint = true;

      if (TourManager.isLatLonAvailable(tourData) == false) {
         showDefaultMap(false);
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
       * set tour into tour data list, this is currently used to draw the legend, it's also used to
       * figure out if multiple tours are selected
       */
      _allTourData.clear();
      _allTourData.add(tourData);
      _hash_AllTourData = _allTourData.hashCode();

      // reset also ALL tour id's, otherwiese a reselected multiple tour is not displayed
      // it took some time to debug this issue !!!
      _hash_AllTourIds = tourData.getTourId().hashCode();

      _tourInfoToolTipProvider.setTourDataList(_allTourData);

      // set the paint context (slider position) for the direct mapping painter
      _directMappingPainter.setPaintContext(
            _map,
            _isShowTour,
            tourData,
            _currentLeftSliderValueIndex,
            _currentRightSliderValueIndex,
            _actionShowSliderInMap.isChecked(),
            _actionShowSliderInLegend.isChecked(),
            _sliderPathPaintingData);

      // set the tour bounds
      final GeoPosition[] tourBounds = tourData.getGeoBounds();

      final HashSet<GeoPosition> tourBoundsSet = new HashSet<>();
      tourBoundsSet.add(tourBounds[0]);
      tourBoundsSet.add(tourBounds[1]);

      _tourPainterConfig.setTourBounds(tourBoundsSet);

      _map.tourBreadcrumb().setTours(tourData);
      _map.setShowOverlays(_isShowTour || _isShowPhoto);
      _map.setShowLegend(_isShowTour && _isShowLegend);

      /*
       * set position and zoom level for the tour
       */
      if (_isMapSynched_WithTour && !_map.isSearchTourByLocation()) {

         if (((forceRedraw == false) && (_previousTourData != null)) || (tourData == _previousTourData)) {

            /*
             * keep map area for the previous tour
             */
            keepMapPosition(_previousTourData);
         }

         if (tourData.mapCenterPositionLatitude == Double.MIN_VALUE) {

            // use default position for the tour
            positionMapTo_MapPosition(tourBoundsSet, true);

         } else {

            // position tour to the previous position
            _map.setZoom(tourData.mapZoomLevel);
            _map.setMapCenter(
                  new GeoPosition(
                        tourData.mapCenterPositionLatitude,
                        tourData.mapCenterPositionLongitude));
         }
      }

      // keep tour data
      _previousTourData = tourData;

      if (isNewTour || forceRedraw) {

         // adjust legend values for the new or changed tour
         createLegendImage(_tourPainterConfig.getMapColorProvider());

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

      _map.tourBreadcrumb().setTours(_allTourData);
      _map.setShowOverlays(_isShowTour || _isShowPhoto);
      _map.setShowLegend(_isShowTour && _isShowLegend);

      // get overlay key for all tours which have valid tour data
      long newOverlayKey = -1;
      for (final TourData tourData : _allTourData) {

         if (TourManager.isLatLonAvailable(tourData)) {
            newOverlayKey += tourData.getTourId();
         }
      }

      if (_previousOverlayKey != newOverlayKey) {

         _previousOverlayKey = newOverlayKey;

         _map.setOverlayKey(Long.toString(newOverlayKey));
         _map.disposeOverlayImageCache();
      }

      createLegendImage(_tourPainterConfig.getMapColorProvider());

      _map.paint();
   }

   @Override
   public void photoEvent(final IViewPart viewPart, final PhotoEventId photoEventId, final Object data) {

      if (photoEventId == PhotoEventId.PHOTO_SELECTION) {

         if (data instanceof TourPhotoLinkSelection) {

            onSelectionChanged((TourPhotoLinkSelection) data, true);

         } else if (data instanceof PhotoSelection) {

            onSelectionChanged((PhotoSelection) data, true);
         }

      } else if (photoEventId == PhotoEventId.PHOTO_ATTRIBUTES_ARE_MODIFIED) {

         if (data instanceof ArrayList<?>) {

            updateFilteredPhotos();
         }

      } else if (photoEventId == PhotoEventId.PHOTO_IMAGE_PATH_IS_MODIFIED) {

         // this is not working, manual refresh is necessary
//         _map.redraw();
      }
   }

   @Override
   public void photoPropertyEvent(final PhotoPropertiesEvent event) {

      _photoFilterRatingStars = event.filterRatingStars;
      _photoFilterRatingStarOperator = event.fiterRatingStarOperator;

      updateFilteredPhotos();
   }

   /**
    * @param tourData
    * @param leftSliderValuesIndex
    * @param rightSliderValuesIndex
    * @param selectedSliderIndex
    * @param geoPositions
    */
   private void positionMapTo_0_TourSliders(final TourData tourData,
                                            final int leftSliderValuesIndex,
                                            final int rightSliderValuesIndex,
                                            final int selectedSliderIndex,
                                            final Set<GeoPosition> geoPositions) {

      _isTourOrWayPoint = true;

      if (TourManager.isLatLonAvailable(tourData) == false) {
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
            _actionShowSliderInLegend.isChecked(),
            _sliderPathPaintingData);

      if (_isMapSynched_WithChartSlider) {

         if (geoPositions != null) {

            // center to the left AND right slider

            positionMapTo_MapPosition(geoPositions, true);

         } else {

            if (_isMapSynched_WithChartSlider_IsCentered) {

               // center to the left AND right slider

               final Set<GeoPosition> mapPositions = getXSliderGeoPositions(
                     tourData,
                     leftSliderValuesIndex,
                     rightSliderValuesIndex);

               positionMapTo_MapPosition(mapPositions, true);

            } else {

               positionMapTo_ValueIndex(tourData, _currentSelectedSliderValueIndex);
            }
         }

         _map.paint();

      } else {

         _map.redraw();
      }
   }

   /**
    * Calculates a zoom level so that all points in the specified set will be visible on screen.
    * This is useful if you have a bunch of points in an area like a city and you want to zoom out
    * so that the entire city and it's points are visible without panning.
    *
    * @param tourPositions
    *           A set of GeoPositions to calculate the new zoom from
    * @param adjustZoomLevel
    *           when <code>true</code> the zoom level will be adjusted to user settings
    */
   private void positionMapTo_MapPosition(final Set<GeoPosition> tourPositions, final boolean isAdjustZoomLevel) {

      if ((tourPositions == null) || (tourPositions.size() < 2)) {
         return;
      }

      _map.setMapPosition(tourPositions, isAdjustZoomLevel, _tourPainterConfig.getSynchTourZoomLevel());
   }

   /**
    * Calculate the bounds for the tour in latitude and longitude values
    *
    * @param tourData
    * @param valueIndex
    * @return
    */
   private void positionMapTo_ValueIndex(final TourData tourData, final int valueIndex) {

      if (tourData == null || tourData.latitudeSerie == null) {
         return;
      }

      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      final int sliderIndex = Math.max(0, Math.min(valueIndex, latitudeSerie.length - 1));

      _map.setMapCenter(new GeoPosition(latitudeSerie[sliderIndex], longitudeSerie[sliderIndex]));

   }

   public void redrawMap() {

      Display.getDefault().asyncExec(new Runnable() {
         @Override
         public void run() {

            if (_parent.isDisposed()) {
               return;
            }

            _map.redraw();
         }
      });
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
      _actionShowTour.setSelection(_isShowTour);

      // is show photo
      _isShowPhoto = Util.getStateBoolean(_state, STATE_IS_SHOW_PHOTO_IN_MAP, true);
      _actionShowPhotos.setChecked(_isShowPhoto);

      // is show legend
      _isShowLegend = Util.getStateBoolean(_state, STATE_IS_SHOW_LEGEND_IN_MAP, true);
      _actionShowLegendInMap.setChecked(_isShowLegend);

      // checkbox: is tour centered
      final boolean isTourCentered = _state.getBoolean(MEMENTO_ZOOM_CENTERED);
      _actionZoom_Centered.setChecked(isTourCentered);
      _isPositionCentered = isTourCentered;

      // sync map with photo
      _isMapSynched_WithPhoto = Util.getStateBoolean(_state, STATE_IS_SYNC_WITH_PHOTO, false);
      _actionSyncMap_WithPhoto.setChecked(_isMapSynched_WithPhoto);

      // synch map with tour
      final boolean isSynchTour = Util.getStateBoolean(_state, MEMENTO_SYNCH_WITH_SELECTED_TOUR, true);
      _actionSyncMap_WithTour.setChecked(isSynchTour);
      _isMapSynched_WithTour = isSynchTour;

      // synch map with chart slider
      _isMapSynched_WithChartSlider = Util.getStateBoolean(_state, STATE_IS_SYNC_WITH_TOURCHART_SLIDER, true);
      _isMapSynched_WithChartSlider_IsCentered = Util.getStateBoolean(
            _state,
            STATE_IS_SYNC_WITH_TOURCHART_SLIDER_IS_CENTERED,
            true);
      updateChartSliderAction();

      // synch map with another map
      _isMapSynched_WithOtherMap = Util.getStateBoolean(_state, STATE_IS_SYNC_MAP2_WITH_OTHER_MAP, false);
      _actionSyncMap_WithOtherMap.setChecked(_isMapSynched_WithOtherMap);

      //
      _actionSyncZoomLevelAdjustment.setZoomLevel(Util.getStateInt(_state, MEMENTO_SYNCH_TOUR_ZOOM_LEVEL, 0));
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
      _actionShowSliderInMap.setChecked(Util.getStateBoolean(_state,
            MEMENTO_SHOW_SLIDER_IN_MAP,
            MEMENTO_SHOW_SLIDER_IN_MAP_DEFAULT));

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

         final String stateColorId = Util.getStateString(_state, MEMENTO_TOUR_COLOR_ID, MapGraphId.Altitude.name());

         MapGraphId colorId;
         try {
            colorId = MapGraphId.valueOf(stateColorId);
         } catch (final Exception e) {
            // set default
            colorId = MapGraphId.Altitude;
         }

         switch (colorId) {
         case Altitude:
            _actionTourColorAltitude.setChecked(true);
            break;

         case Gradient:
            _actionTourColorGradient.setChecked(true);
            break;

         case Pace:
            _actionTourColorPace.setChecked(true);
            break;

         case Pulse:
            _actionTourColorPulse.setChecked(true);
            break;

         case Speed:
            _actionTourColorSpeed.setChecked(true);
            break;

         case HrZone:
            _actionTourColorHrZone.setChecked(true);
            break;

         case RunDyn_StepLength:
            _actionTourColor_RunDyn_StepLength.setChecked(true);
            break;

         default:
            _actionTourColorAltitude.setChecked(true);
            break;
         }

         setTourPainterColorProvider(colorId);

      } catch (final NumberFormatException e) {
         _actionTourColorAltitude.setChecked(true);
      }

      // draw tour with default color

      _map.setShowOverlays(_isShowTour || _isShowPhoto);
      _map.setShowLegend(_isShowTour);

      // check legend provider
      if (_tourPainterConfig.getMapColorProvider() == null) {

         // set default legend provider
         setTourPainterColorProvider(MapGraphId.Altitude);

         // hide legend
         _map.setShowLegend(false);
      }

      // debug info
      final boolean isShowGeoGrid = _prefStore.getBoolean(PREF_DEBUG_MAP_SHOW_GEO_GRID);
      final boolean isShowTileInfo = _prefStore.getBoolean(Map2View.PREF_SHOW_TILE_INFO);
      final boolean isShowTileBorder = _prefStore.getBoolean(PREF_SHOW_TILE_BORDER);

      _map.setShowDebugInfo(isShowTileInfo, isShowTileBorder, isShowGeoGrid);

      // set dim level/color after the map providers are set
      if (_mapDimLevel == -1) {
         _mapDimLevel = 0xff;
      }
      final RGB dimColor = PreferenceConverter.getColor(_prefStore, ITourbookPreferences.MAP_LAYOUT_MAP_DIMM_COLOR);
      _map.setDimLevel(_mapDimLevel, dimColor);
      _mapDimLevel = _actionDimMap.setDimLevel(_mapDimLevel);

      restoreState_Map2_TrackOptions(false);
      restoreState_Map2_Options();

      // display the map with the default position
      actionSetDefaultPosition();
   }

   void restoreState_Map2_Options() {

      _map.setIsZoomWithMousePosition(Util.getStateBoolean(_state,
            Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION,
            Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION_DEFAULT));

      _map.setShowHoveredTourTooltip(Util.getStateBoolean(_state,
            Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR,
            Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT));
   }

   void restoreState_Map2_TrackOptions(final boolean isUpdateMapUI) {

      _actionShowSliderInMap.setChecked(Util.getStateBoolean(_state,
            MEMENTO_SHOW_SLIDER_IN_MAP,
            Map2View.MEMENTO_SHOW_SLIDER_IN_MAP_DEFAULT));

      _sliderPathPaintingData = new SliderPathPaintingData();

      _sliderPathPaintingData.isShowSliderPath = Util.getStateBoolean(_state,
            Map2View.STATE_IS_SHOW_SLIDER_PATH,
            Map2View.STATE_IS_SHOW_SLIDER_PATH_DEFAULT);

      _sliderPathPaintingData.opacity = (Util.getStateInt(_state,
            Map2View.STATE_SLIDER_PATH_OPACITY,
            Map2View.STATE_SLIDER_PATH_OPACITY_DEFAULT));

      _sliderPathPaintingData.segments = (Util.getStateInt(_state,
            Map2View.STATE_SLIDER_PATH_SEGMENTS,
            Map2View.STATE_SLIDER_PATH_SEGMENTS_DEFAULT));

      _sliderPathPaintingData.lineWidth = (Util.getStateInt(_state,
            Map2View.STATE_SLIDER_PATH_LINE_WIDTH,
            Map2View.STATE_SLIDER_PATH_LINE_WIDTH_DEFAULT));

      _sliderPathPaintingData.color = (Util.getStateRGB(_state,
            Map2View.STATE_SLIDER_PATH_COLOR,
            Map2View.STATE_SLIDER_PATH_COLOR_DEFAULT));

      if (isUpdateMapUI) {

         // update map UI
         actionShowSlider();
      }
   }

   /**
    * Filter photos by rating stars.
    */
   private void runPhotoFilter() {

      _filteredPhotos.clear();

      if (_isPhotoFilterActive) {

         final boolean isNoStar = _photoFilterRatingStars == 0;
         final boolean isEqual = _photoFilterRatingStarOperator == DialogPhotoProperties.OPERATOR_IS_EQUAL;
         final boolean isMore = _photoFilterRatingStarOperator == DialogPhotoProperties.OPERATOR_IS_MORE_OR_EQUAL;
         final boolean isLess = _photoFilterRatingStarOperator == DialogPhotoProperties.OPERATOR_IS_LESS_OR_EQUAL;

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

      enableActions(true);

      PhotoManager.firePhotoEvent(
            this,
            PhotoEventId.PHOTO_FILTER,
            new MapFilterData(
                  _allPhotos.size(),
                  _filteredPhotos.size()));

   }

   @PersistState
   private void saveState() {

      // save checked actions
      _state.put(STATE_IS_SHOW_TOUR_IN_MAP, _isShowTour);
      _state.put(STATE_IS_SHOW_PHOTO_IN_MAP, _isShowPhoto);
      _state.put(STATE_IS_SHOW_LEGEND_IN_MAP, _isShowLegend);

      _state.put(STATE_IS_SYNC_MAP2_WITH_OTHER_MAP, _isMapSynched_WithOtherMap);
      _state.put(STATE_IS_SYNC_WITH_PHOTO, _isMapSynched_WithPhoto);
      _state.put(STATE_IS_SYNC_WITH_TOURCHART_SLIDER, _isMapSynched_WithChartSlider);
      _state.put(STATE_IS_SYNC_WITH_TOURCHART_SLIDER_IS_CENTERED, _isMapSynched_WithChartSlider_IsCentered);

      _state.put(MEMENTO_ZOOM_CENTERED, _actionZoom_Centered.isChecked());
      _state.put(MEMENTO_SYNCH_WITH_SELECTED_TOUR, _actionSyncMap_WithTour.isChecked());
      _state.put(MEMENTO_SYNCH_TOUR_ZOOM_LEVEL, _actionSyncZoomLevelAdjustment.getZoomLevel());

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
      MapGraphId colorId;

      if (_actionTourColorGradient.isChecked()) {
         colorId = MapGraphId.Gradient;

      } else if (_actionTourColorPulse.isChecked()) {
         colorId = MapGraphId.Pulse;

      } else if (_actionTourColorSpeed.isChecked()) {
         colorId = MapGraphId.Speed;

      } else if (_actionTourColorPace.isChecked()) {
         colorId = MapGraphId.Pace;

      } else if (_actionTourColorHrZone.isChecked()) {
         colorId = MapGraphId.HrZone;

      } else if (_actionTourColor_RunDyn_StepLength.isChecked()) {
         colorId = MapGraphId.RunDyn_StepLength;

      } else {
         // use altitude as default
         colorId = MapGraphId.Altitude;
      }
      _state.put(MEMENTO_TOUR_COLOR_ID, colorId.name());

      _actionPhotoFilter.saveState();
   }

   private void selectTourSegments(final SelectedTourSegmenterSegments selectedSegmenterConfig) {

      if (_allTourData.size() < 1) {
         return;
      }

      final int leftSliderValueIndex = selectedSegmenterConfig.xSliderSerieIndexLeft;
      final int rightSliderValueIndex = selectedSegmenterConfig.xSliderSerieIndexRight;

      final TourData segmenterTourData = selectedSegmenterConfig.tourData;
      final Set<GeoPosition> mapPositions = new HashSet<>();

      if (_allTourData.size() == 1) {

         final TourData mapTourData = _allTourData.get(0);

         // ensure it's the same tour
         if (segmenterTourData != mapTourData) {
            return;
         }

         final GeoPosition leftPosition = new GeoPosition(
               mapTourData.latitudeSerie[leftSliderValueIndex],
               mapTourData.longitudeSerie[leftSliderValueIndex]);

         final GeoPosition rightPosition = new GeoPosition(
               mapTourData.latitudeSerie[rightSliderValueIndex],
               mapTourData.longitudeSerie[rightSliderValueIndex]);

         mapPositions.add(leftPosition);
         mapPositions.add(rightPosition);

         positionMapTo_0_TourSliders(//
               mapTourData,
               leftSliderValueIndex,
               rightSliderValueIndex,
               leftSliderValueIndex,
               mapPositions);

         enableActions();

      } else {

         // multiple tourdata, I'm not sure if this still occures after merging multiple tours into one tourdata
      }
   }

   @Override
   public void setFocus() {
      _map.setFocus();
   }

   private void setTourPainterColorProvider(final MapGraphId colorId) {

      _tourColorId = colorId;

      final IMapColorProvider mapColorProvider = getColorProvider(colorId);

      _tourPainterConfig.setMapColorProvider(mapColorProvider);
   }

   private void showDefaultMap(final boolean isShowOverlays) {

      // disable tour actions in this view
      _isTourOrWayPoint = false;

      // disable tour data
      _allTourData.clear();
      _previousTourData = null;
      _tourPainterConfig.resetTourData();

      // update direct painter to draw nothing
      _directMappingPainter.setPaintContext(_map, false, null, 0, 0, false, false, _sliderPathPaintingData);

      _map.tourBreadcrumb().resetTours();

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

               final MessageDialogWithToggle dialog = MessageDialogWithToggle.openInformation(
                     Display.getCurrent().getActiveShell(),
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
               _hash_AllTourData = _allTourData.hashCode();

               paintTours_10_All();
            }
         }
      });
   }

   @Override
   public void syncMapWithOtherMap(final MapPosition mapPosition,
                                   final ViewPart viewPart,
                                   final int positionFlags) {

      if (!_isMapSynched_WithOtherMap) {

         // sync feature is disabled

         return;
      }

      if (viewPart == this || !_isPartVisible) {

         // event is fired from this map -> ignore

         return;
      }

      if (_isInMapSync) {
         return;
      }

      final long timeDiff = System.currentTimeMillis() - _lastFiredSyncEventTime;

      if (timeDiff < 1000) {
         // ignore because it causes LOTS of problems when synching moved map
         return;
      }

      final Runnable runnable = new Runnable() {

         final int __asynchRunnableCounter = _asyncCounter.incrementAndGet();

         @Override
         public void run() {

            // check if a newer runnable is available
            if (__asynchRunnableCounter != _asyncCounter.get()) {
               // a newer queryRedraw is available
               return;
            }

            _isInMapSync = true;
            {
               _map.setZoom(mapPosition.zoomLevel + 1);
               _map.setMapCenter(new GeoPosition(mapPosition.getLatitude(), mapPosition.getLongitude()));
            }
            _isInMapSync = false;
         }
      };

      // run in UI thread
      _parent.getDisplay().asyncExec(runnable);
   }

   private void updateChartSliderAction() {

      String toolTip;
      ImageDescriptor imageDescriptor;
      ImageDescriptor imageDescriptorDisabled;

      final boolean isSync = _isMapSynched_WithChartSlider;
      final boolean isCenter = _isMapSynched_WithChartSlider_IsCentered;

      if (isSync && isCenter) {

         toolTip = Messages.Map_Action_SynchWithSlider_Centered;

         imageDescriptor = _imageSyncWithSlider_Centered;
         imageDescriptorDisabled = _imageSyncWithSlider_Centered_Disabled;

      } else {

         toolTip = Messages.map_action_synch_with_slider;

         imageDescriptor = _imageSyncWithSlider;
         imageDescriptorDisabled = _imageSyncWithSlider_Disabled;
      }

      _actionSyncMap_WithChartSlider.setToolTipText(toolTip);
      _actionSyncMap_WithChartSlider.setImageDescriptor(imageDescriptor);
      _actionSyncMap_WithChartSlider.setDisabledImageDescriptor(imageDescriptorDisabled);
      _actionSyncMap_WithChartSlider.setChecked(isSync);
   }

   private void updateFilteredPhotos() {

      runPhotoFilter();

      _map.disposeOverlayImageCache();
      _map.paint();
   }

   void updateTourColorsInToolbar() {

      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      fillToolbar_TourColors(tbm);

      tbm.update(true);
   }

   /**
    * Show tour when it is not yet displayed.
    *
    * @param tourData
    */
   private void updateUI_ShowTour(final TourData tourData) {

      // check if the marker tour is displayed
      final long markerTourId = tourData.getTourId().longValue();
      boolean isTourVisible = false;

      for (final TourData mapTourData : _allTourData) {
         if (mapTourData.getTourId().longValue() == markerTourId) {
            isTourVisible = true;
            break;
         }
      }

      if (isTourVisible == false) {

         // tour is not yet visible, show it now

         _allTourData.clear();
         _allTourData.add(tourData);
         _hash_AllTourData = _allTourData.hashCode();
         _hash_AllTourIds = tourData.getTourId().hashCode();

         paintTours_10_All();
      }
   }
}
