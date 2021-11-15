/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import static org.eclipse.swt.events.ControlListener.controlResizedAdapter;

import de.byteholder.geoclipse.GeoclipseExtensions;
import de.byteholder.geoclipse.map.ActionManageOfflineImages;
import de.byteholder.geoclipse.map.IMapContextProvider;
import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.MapGridData;
import de.byteholder.geoclipse.map.MapLegend;
import de.byteholder.geoclipse.map.event.IMapInfoListener;
import de.byteholder.geoclipse.map.event.IMapPositionListener;
import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;
import de.byteholder.gpx.PointOfInterest;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.color.ColorProviderConfig;
import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.preferences.ICommonPreferences;
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
import net.tourbook.map2.action.ActionCreateTourMarkerFromMap;
import net.tourbook.map2.action.ActionManageMapProviders;
import net.tourbook.map2.action.ActionMap2Color;
import net.tourbook.map2.action.ActionMap2_MapProvider;
import net.tourbook.map2.action.ActionMap2_PhotoFilter;
import net.tourbook.map2.action.ActionReloadFailedMapImages;
import net.tourbook.map2.action.ActionSaveDefaultPosition;
import net.tourbook.map2.action.ActionSetDefaultPosition;
import net.tourbook.map2.action.ActionShowAllFilteredPhotos;
import net.tourbook.map2.action.ActionShowLegendInMap;
import net.tourbook.map2.action.ActionShowPOI;
import net.tourbook.map2.action.ActionShowScaleInMap;
import net.tourbook.map2.action.ActionShowSliderInLegend;
import net.tourbook.map2.action.ActionShowSliderInMap;
import net.tourbook.map2.action.ActionShowStartEndInMap;
import net.tourbook.map2.action.ActionShowTourInfoInMap;
import net.tourbook.map2.action.ActionShowTourMarker;
import net.tourbook.map2.action.ActionShowTourPauses;
import net.tourbook.map2.action.ActionShowValuePoint;
import net.tourbook.map2.action.ActionShowWayPoints;
import net.tourbook.map2.action.ActionSyncMapWith_OtherMap;
import net.tourbook.map2.action.ActionSyncMapWith_Photo;
import net.tourbook.map2.action.ActionSyncMapWith_Slider_Centered;
import net.tourbook.map2.action.ActionSyncMapWith_Slider_One;
import net.tourbook.map2.action.ActionSyncMapWith_Tour;
import net.tourbook.map2.action.ActionSyncMapWith_ValuePoint;
import net.tourbook.map2.action.ActionTourColor;
import net.tourbook.map2.action.ActionZoomCentered;
import net.tourbook.map2.action.ActionZoomIn;
import net.tourbook.map2.action.ActionZoomLevelAdjustment;
import net.tourbook.map2.action.ActionZoomOut;
import net.tourbook.map2.action.ActionZoomShowEntireMap;
import net.tourbook.map2.action.ActionZoomShowEntireTour;
import net.tourbook.map2.action.Action_ExportMap_SubMenu;
import net.tourbook.photo.IPhotoEventListener;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoEventId;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.PhotoRatingStarOperator;
import net.tourbook.photo.PhotoSelection;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPage_Map2_Appearance;
import net.tourbook.srtm.IPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.SelectionTourPause;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourInfoIconToolTipProvider;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.filter.geo.GeoFilter_LoaderData;
import net.tourbook.tour.filter.geo.TourGeoFilter;
import net.tourbook.tour.filter.geo.TourGeoFilter_Loader;
import net.tourbook.tour.filter.geo.TourGeoFilter_Manager;
import net.tourbook.tour.photo.IMapWithPhotos;
import net.tourbook.tour.photo.TourPhotoLink;
import net.tourbook.tour.photo.TourPhotoLinkSelection;
import net.tourbook.ui.tourChart.HoveredValueData;
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
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
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
      IMapBookmarks,
      IMapBookmarkListener,
      IMapPositionListener,
      IMapSyncListener,
      IMapInfoListener,
      IMapWithPhotos {

// SET_FORMATTING_OFF

   private static final String   TOUR_TOOLTIP_LABEL_NO_GEO_TOUR                        = net.tourbook.ui.Messages.Tour_Tooltip_Label_NoGeoTour;

   public static final String    ID                                                    = "net.tourbook.map2.view.Map2ViewId";                   //$NON-NLS-1$

   static final String           STATE_IS_TOGGLE_KEYBOARD_PANNING                      = "STATE_IS_TOGGLE_KEYBOARD_PANNING";                    //$NON-NLS-1$
   static final boolean          STATE_IS_TOGGLE_KEYBOARD_PANNING_DEFAULT              = true;
   private static final String   STATE_IS_SHOW_TOUR_IN_MAP                             = "STATE_IS_SHOW_TOUR_IN_MAP";                           //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_PHOTO_IN_MAP                            = "STATE_IS_SHOW_PHOTO_IN_MAP";                          //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_LEGEND_IN_MAP                           = "STATE_IS_SHOW_LEGEND_IN_MAP";                         //$NON-NLS-1$
   static final String           STATE_IS_ZOOM_WITH_MOUSE_POSITION                     = "STATE_IS_ZOOM_WITH_MOUSE_POSITION";                   //$NON-NLS-1$
   static final boolean          STATE_IS_ZOOM_WITH_MOUSE_POSITION_DEFAULT             = true;
   private static final String   STATE_IS_SHOW_VALUE_POINT                             = "STATE_IS_SHOW_VALUE_POINT";                           //$NON-NLS-1$
   private static final boolean  STATE_IS_SHOW_VALUE_POINT_DEFAULT                     = true;

   private static final String   STATE_IS_SHOW_SCALE_IN_MAP                            = "STATE_IS_SHOW_SCALE_IN_MAP";                          //$NON-NLS-1$
   static final String           STATE_IS_SHOW_SLIDER_IN_MAP                           = "STATE_IS_SHOW_SLIDER_IN_MAP";                         //$NON-NLS-1$
   static final boolean          STATE_IS_SHOW_SLIDER_IN_MAP_DEFAULT                   = true;
   private static final String   STATE_IS_SHOW_SLIDER_IN_LEGEND                        = "STATE_IS_SHOW_SLIDER_IN_LEGEND";                      //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_START_END_IN_MAP                        = "STATE_IS_SHOW_START_END_IN_MAP";                      //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_TOUR_MARKER                             = "STATE_IS_SHOW_TOUR_MARKER";                           //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_TOUR_PAUSES                             = "STATE_IS_SHOW_TOUR_PAUSES";                           //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_TOUR_INFO_IN_MAP                        = "STATE_IS_SHOW_TOUR_INFO_IN_MAP";                      //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_WAY_POINTS                              = "STATE_IS_SHOW_WAY_POINTS";                            //$NON-NLS-1$
   private static final String   STATE_IS_ZOOM_CENTERED                                = "STATE_IS_ZOOM_CENTERED";                              //$NON-NLS-1$

   public static final String    STATE_IS_SHOW_HOVERED_SELECTED_TOUR                            = "STATE_IS_SHOW_HOVERED_SELECTED_TOUR";                 //$NON-NLS-1$
   public static final boolean   STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT                    = true;
   static final String           STATE_HOVERED_SELECTED__HOVERED_OPACITY                        = "STATE_HOVERED_SELECTED__HOVERED_OPACITY";             //$NON-NLS-1$
   static final int              STATE_HOVERED_SELECTED__HOVERED_OPACITY_DEFAULT                = UI.MAX_OPACITY / 2;
   static final String           STATE_HOVERED_SELECTED__HOVERED_RGB                            = "STATE_HOVERED_SELECTED__HOVERED_RGB";                 //$NON-NLS-1$
   static final RGB              STATE_HOVERED_SELECTED__HOVERED_RGB_DEFAULT                    = new RGB(255, 255, 0);
   static final String           STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_OPACITY           = "STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_OPACITY";//$NON-NLS-1$
   static final int              STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_OPACITY_DEFAULT   = UI.MAX_OPACITY / 2;
   static final String           STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_RGB               = "STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_RGB";    //$NON-NLS-1$
   static final RGB              STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_RGB_DEFAULT       = new RGB(0, 255, 255);
   static final String           STATE_HOVERED_SELECTED__SELECTED_OPACITY                       = "STATE_HOVERED_SELECTED__SELECTED_OPACITY";            //$NON-NLS-1$
   static final int              STATE_HOVERED_SELECTED__SELECTED_OPACITY_DEFAULT               = UI.MAX_OPACITY / 2;
   static final String           STATE_HOVERED_SELECTED__SELECTED_RGB                           = "STATE_HOVERED_SELECTED__SELECTED_RGB";                //$NON-NLS-1$
   static final RGB              STATE_HOVERED_SELECTED__SELECTED_RGB_DEFAULT                   = new RGB(0, 255, 0);

   static final String           STATE_IS_SHOW_TOUR_DIRECTION                          = "STATE_IS_SHOW_TOUR_DIRECTION";                        //$NON-NLS-1$
   static final boolean          STATE_IS_SHOW_TOUR_DIRECTION_DEFAULT                  = true;
   static final String           STATE_TOUR_DIRECTION_LINE_WIDTH                       = "STATE_TOUR_DIRECTION_LINE_WIDTH";                     //$NON-NLS-1$
   static final int              STATE_TOUR_DIRECTION_LINE_WIDTH_DEFAULT               = 3;
   static final String           STATE_TOUR_DIRECTION_MARKER_GAP                       = "STATE_TOUR_DIRECTION_MARKER_GAP";                     //$NON-NLS-1$
   static final int              STATE_TOUR_DIRECTION_MARKER_GAP_DEFAULT               = 30;
   static final String           STATE_TOUR_DIRECTION_SYMBOL_SIZE                      = "STATE_TOUR_DIRECTION_SYMBOL_SIZE";                    //$NON-NLS-1$
   static final int              STATE_TOUR_DIRECTION_SYMBOL_SIZE_DEFAULT              = 10;
   static final String           STATE_TOUR_DIRECTION_RGB                              = "STATE_TOUR_DIRECTION_RGB";                            //$NON-NLS-1$
   static final RGB              STATE_TOUR_DIRECTION_RGB_DEFAULT                      = new RGB(55, 55, 55);

   private static final String   STATE_MAP_SYNC_MODE                                   = "STATE_MAP_SYNC_MODE";                                 //$NON-NLS-1$
   private static final String   STATE_MAP_SYNC_MODE_IS_ACTIVE                         = "STATE_MAP_SYNC_MODE_IS_ACTIVE";                       //$NON-NLS-1$

   private static final String   STATE_ZOOM_LEVEL_ADJUSTMENT                           = "STATE_ZOOM_LEVEL_ADJUSTMENT";                         //$NON-NLS-1$
   private static final String   STATE_SELECTED_MAP_PROVIDER_ID                        = "selected.map-provider-id";                            //$NON-NLS-1$

   private static final String   STATE_DEFAULT_POSITION_ZOOM                           = "STATE_DEFAULT_POSITION_ZOOM";                         //$NON-NLS-1$
   private static final String   STATE_DEFAULT_POSITION_LATITUDE                       = "STATE_DEFAULT_POSITION_LATITUDE";                     //$NON-NLS-1$
   private static final String   STATE_DEFAULT_POSITION_LONGITUDE                      = "STATE_DEFAULT_POSITION_LONGITUDE";                    //$NON-NLS-1$
   private static final String   STATE_TOUR_COLOR_ID                                   = "STATE_TOUR_COLOR_ID";                                 //$NON-NLS-1$

   static final String           PREF_DEBUG_MAP_SHOW_GEO_GRID                          = "PREF_DEBUG_MAP_SHOW_GEO_GRID";                        //$NON-NLS-1$
   static final String           PREF_SHOW_TILE_INFO                                   = "PREF_SHOW_TILE_INFO";                                 //$NON-NLS-1$
   static final String           PREF_SHOW_TILE_BORDER                                 = "PREF_SHOW_TILE_BORDER";                               //$NON-NLS-1$

   static final String           STATE_IS_SHOW_IN_TOOLBAR_ALTITUDE                     = "STATE_IS_SHOW_IN_TOOLBAR_ALTITUDE";                   //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_GRADIENT                     = "STATE_IS_SHOW_IN_TOOLBAR_GRADIENT";                   //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_PACE                         = "STATE_IS_SHOW_IN_TOOLBAR_PACE";                       //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_PULSE                        = "STATE_IS_SHOW_IN_TOOLBAR_PULSE";                      //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_SPEED                        = "STATE_IS_SHOW_IN_TOOLBAR_SPEED";                      //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_HR_ZONE                      = "STATE_IS_SHOW_IN_TOOLBAR_HR_ZONE";                    //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_RUN_DYN_STEP_LENGTH          = "STATE_IS_SHOW_IN_TOOLBAR_RUN_DYN_STEP_LENGTH";        //$NON-NLS-1$

   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_ALTITUDE_DEFAULT             = true;
   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_GRADIENT_DEFAULT             = false;
   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_PACE_DEFAULT                 = false;
   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_PULSE_DEFAULT                = true;
   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_SPEED_DEFAULT                = false;
   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_HR_ZONE_DEFAULT              = false;
   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_RUN_DYN_STEP_LENGTH_DEFAULT  = true;

   static final String           STATE_IS_SHOW_SLIDER_PATH                             = "STATE_IS_SHOW_SLIDER_PATH";                           //$NON-NLS-1$
   static final String           STATE_SLIDER_PATH_LINE_WIDTH                          = "STATE_SLIDER_PATH_LINE_WIDTH";                        //$NON-NLS-1$
   static final String           STATE_SLIDER_PATH_OPACITY                             = "STATE_SLIDER_PATH_OPACITY";                           //$NON-NLS-1$
   static final String           STATE_SLIDER_PATH_SEGMENTS                            = "STATE_SLIDER_PATH_SEGMENTS";                          //$NON-NLS-1$
   static final String           STATE_SLIDER_PATH_COLOR                               = "STATE_SLIDER_PATH_COLOR";                             //$NON-NLS-1$

   static final boolean          STATE_IS_SHOW_SLIDER_PATH_DEFAULT                     = true;
   static final int              STATE_SLIDER_PATH_LINE_WIDTH_DEFAULT                  = 30;
   static final int              STATE_SLIDER_PATH_OPACITY_DEFAULT                     = 60;
   static final int              STATE_SLIDER_PATH_SEGMENTS_DEFAULT                    = 200;
   static final RGB              STATE_SLIDER_PATH_COLOR_DEFAULT                       = new RGB(0xff, 0xff, 0x80);

   private static final String   STATE_IS_PHOTO_FILTER_ACTIVE                          = "STATE_IS_PHOTO_FILTER_ACTIVE";                        //$NON-NLS-1$
   private static final String   STATE_PHOTO_FILTER_RATING_STARS                       = "STATE_PHOTO_FILTER_RATING_STARS";                     //$NON-NLS-1$
   private static final String   STATE_PHOTO_FILTER_RATING_STAR_OPERATOR               = "STATE_PHOTO_FILTER_RATING_STAR_OPERATOR";             //$NON-NLS-1$

   public static final int       MAX_DIM_STEPS                                         = 10;
   public static final String    STATE_IS_MAP_DIMMED                                   = "STATE_IS_MAP_DIMMED";                                    //$NON-NLS-1$
   public static final boolean   STATE_IS_MAP_DIMMED_DEFAULT                           = false;
   public static final String    STATE_DIM_MAP_COLOR                                   = "STATE_DIM_MAP_COLOR";                                    //$NON-NLS-1$
   public static final RGB       STATE_DIM_MAP_COLOR_DEFAULT                           = new RGB(0x2f, 0x2f, 0x2f);
   public static final String    STATE_DIM_MAP_VALUE                                   = "STATE_DIM_MAP_VALUE";                                    //$NON-NLS-1$
   public static final int       STATE_DIM_MAP_VALUE_DEFAULT                           = MAX_DIM_STEPS / 2;


   private static final String   GRAPH_CONTRIBUTION_ID_SLIDEOUT                        = "GRAPH_CONTRIBUTION_ID_SLIDEOUT";                      //$NON-NLS-1$

   private static final MapGraphId[]         _allGraphContribId       = {

         MapGraphId.Altitude,
         MapGraphId.Gradient,
         MapGraphId.Pace,
         MapGraphId.Pulse,
         MapGraphId.Speed,

         MapGraphId.RunDyn_StepLength,

         MapGraphId.HrZone,
   };



   // SET_FORMATTING_ON
   //
   private static final IPreferenceStore _prefStore         = TourbookPlugin.getPrefStore();
   private static final IPreferenceStore _prefStore_Common  = CommonActivator.getPrefStore();
   private static final IDialogSettings  _state             = TourbookPlugin.getState(ID);
   private static final IDialogSettings  _state_MapProvider = TourbookPlugin.getState("net.tourbook.map2.view.Map2View.MapProvider"); //$NON-NLS-1$
   private static final IDialogSettings  _state_PhotoFilter = TourbookPlugin.getState("net.tourbook.map2.view.Map2View.PhotoFilter"); //$NON-NLS-1$
   //
   //
   private final TourInfoIconToolTipProvider _tourInfoToolTipProvider = new TourInfoIconToolTipProvider(3, 23);
   private final ITourToolTipProvider        _wayPointToolTipProvider = new WayPointToolTipProvider();
   private final DirectMappingPainter        _directMappingPainter    = new DirectMappingPainter();
   //
   private final MapInfoManager              _mapInfoManager          = MapInfoManager.getInstance();
   private final TourPainterConfiguration    _tourPainterConfig       = TourPainterConfiguration.getInstance();
   //
   private boolean                           _isPartVisible;
   //
   /**
    * Contains selection which was set when the part is hidden
    */
   private ISelection                        _selectionWhenHidden;
   private IPartListener2                    _partListener;
   private ISelectionListener                _postSelectionListener;
   private IPropertyChangeListener           _prefChangeListener;
   private IPropertyChangeListener           _prefChangeListener_Common;
   private ITourEventListener                _tourEventListener;
   //
   /**
    * Contains all tours which are displayed in the map.
    */
   private final ArrayList<TourData>         _allTourData             = new ArrayList<>();
   private TourData                          _previousTourData;
   //
   /**
    * contains photos which are displayed in the map
    */
   private final ArrayList<Photo>            _allPhotos               = new ArrayList<>();
   private final ArrayList<Photo>            _filteredPhotos          = new ArrayList<>();
   //
   private boolean                           _isPhotoFilterActive;
   private int                               _photoFilter_RatingStars;
   private Enum<PhotoRatingStarOperator>     _photoFilter_RatingStar_Operator;
   //
   private boolean                           _isShowTour;
   private boolean                           _isShowPhoto;
   private boolean                           _isShowLegend;
   private boolean                           _isPositionCentered;
   private boolean                           _isInSelectBookmark;
   private boolean                           _isInZoom;
   //
   private int                               _defaultZoom;
   private GeoPosition                       _defaultPosition;
   //
   /**
    * When <code>true</code> a tour is painted, <code>false</code> a point of interest is painted
    */
   private boolean                           _isTourOrWayPoint;
   //
   /*
    * Tool tips
    */
   private TourToolTip _tourToolTip;

   private String      _poiName;
   private GeoPosition _poiPosition;
   private int         _poiZoomLevel;
   //
   /*
    * Current position for the x-sliders and value point
    */
   private int                               _currentLeftSliderValueIndex;
   private int                               _currentRightSliderValueIndex;
   private int                               _currentSelectedSliderValueIndex;
   private int                               _currentValuePointIndex;
   //
   private MapLegend                         _mapLegend;
   //
   private long                              _previousOverlayKey;
   //
   private int                               _selectedProfileKey   = 0;
   //
   private MapGraphId                        _tourColorId;
   //
   private int                               _hash_AllTourIds;
   private int                               _hash_AllTourData;
   private long                              _hash_TourOverlayKey;
   private int                               _hash_AllPhotos;
   //
   private final AtomicInteger               _asyncCounter         = new AtomicInteger();

   /**
    * Is <code>true</code> when a link photo is displayed, otherwise a tour photo (photo which is
    * save in a tour) is displayed.
    */
   private boolean                           _isLinkPhotoDisplayed;
   //
   private SliderPathPaintingData            _sliderPathPaintingData;
   private OpenDialogManager                 _openDlgMgr           = new OpenDialogManager();
   //
   /**
    * Keep map sync mode when map sync action get's unchecked
    */
   private MapSyncMode                       _currentMapSyncMode   = MapSyncMode.IsSyncWith_NONE;
   private boolean                           _isInMapSync;
   private long                              _lastFiredSyncEventTime;
   //
   private boolean                           _isMapSyncWith_OtherMap;
   private boolean                           _isMapSyncWith_Photo;
   private boolean                           _isMapSyncWith_Slider_Centered;
   private boolean                           _isMapSyncWith_Slider_One;
   private boolean                           _isMapSyncWith_Tour;
   private boolean                           _isMapSyncWith_ValuePoint;
   //
   private HashMap<MapGraphId, Action>       _allTourColor_Actions = new HashMap<>();
   private ActionTourColor                   _actionTourColor_Elevation;
   private ActionTourColor                   _actionTourColor_Gradient;
   private ActionTourColor                   _actionTourColor_Pulse;
   private ActionTourColor                   _actionTourColor_Speed;
   private ActionTourColor                   _actionTourColor_Pace;
   private ActionTourColor                   _actionTourColor_HrZone;
   private ActionTourColor                   _actionTourColor_RunDyn_StepLength;
   //
   private ActionCreateTourMarkerFromMap     _actionCreateTourMarkerFromMap;
   private ActionOpenPrefDialog              _actionEditMap2Preferences;
   private Action_ExportMap_SubMenu          _actionExportMap_SubMenu;
   private ActionManageMapProviders          _actionManageMapProvider;
   private ActionMapBookmarks                _actionMap2_Bookmarks;
   private ActionMap2Color                   _actionMap2_Color;
   private ActionMap2_MapProvider            _actionMap2_MapProvider;
   private ActionMap2_Options                _actionMap2_Options;
   private ActionMap2_PhotoFilter            _actionMap2_PhotoFilter;
   private ActionMap2_Graphs                 _actionMap2_TourColors;
   private ActionReloadFailedMapImages       _actionReloadFailedMapImages;
   private ActionSaveDefaultPosition         _actionSaveDefaultPosition;
   private ActionSearchTourByLocation        _actionSearchTourByLocation;
   private ActionSetDefaultPosition          _actionSetDefaultPosition;
   private ActionShowAllFilteredPhotos       _actionShowAllFilteredPhotos;
   private ActionShowLegendInMap             _actionShowLegendInMap;
   private ActionShowPhotos                  _actionShowPhotos;
   private ActionShowPOI                     _actionShowPOI;
   private ActionShowScaleInMap              _actionShowScaleInMap;
   private ActionShowSliderInMap             _actionShowSliderInMap;
   private ActionShowSliderInLegend          _actionShowSliderInLegend;
   private ActionShowStartEndInMap           _actionShowStartEndInMap;
   private ActionShowTour                    _actionShowTour;
   private ActionShowTourInfoInMap           _actionShowTourInfoInMap;
   private ActionShowTourMarker              _actionShowTourMarker;
   private ActionShowTourPauses              _actionShowTourPauses;
   private ActionShowValuePoint              _actionShowValuePoint;
   private ActionShowWayPoints               _actionShowWayPoints;
   private ActionZoomLevelAdjustment         _actionZoomLevelAdjustment;
   //
   private ActionSyncMap                     _actionMap2_SyncMap;
   private HashMap<MapSyncId, Action>        _allSyncMap_Actions   = new HashMap<>();
   private ActionSyncMapWith_Photo           _actionSyncMapWith_Photo;
   private ActionSyncMapWith_Slider_One      _actionSyncMapWith_Slider_One;
   private ActionSyncMapWith_Slider_Centered _actionSyncMapWith_Slider_Centered;
   private ActionSyncMapWith_OtherMap        _actionSyncMapWith_OtherMap;
   private ActionSyncMapWith_Tour            _actionSyncMapWith_Tour;
   private ActionSyncMapWith_ValuePoint      _actionSyncMapWith_ValuePoint;
   //
   private ActionZoomIn                      _actionZoom_In;
   private ActionZoomOut                     _actionZoom_Out;
   private ActionZoomCentered                _actionZoom_Centered;
   private ActionZoomShowEntireMap           _actionZoom_ShowEntireMap;
   private ActionZoomShowEntireTour          _actionZoom_ShowEntireTour;
   //
   private org.eclipse.swt.graphics.Point    _geoFilter_Loaded_TopLeft_E2;
   private org.eclipse.swt.graphics.Point    _geoFilter_Loaded_BottomRight_E2;
   private GeoFilter_LoaderData              _geoFilter_PreviousGeoLoaderItem;
   private AtomicInteger                     _geoFilter_RunningId  = new AtomicInteger();
   //
   /*
    * UI controls
    */
   private Composite _parent;
   private Map       _map;

   private class ActionMap2_Graphs extends ActionToolbarSlideout {

      public ActionMap2_Graphs() {

         super(TourbookPlugin.getImageDescriptor(Images.Graph),
               TourbookPlugin.getImageDescriptor(Images.Graph_Disabled));

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

      public ActionMap2_Options() {

         super(TourbookPlugin.getThemedImageDescriptor(Images.MapOptions),
               TourbookPlugin.getImageDescriptor(Images.MapOptions_Disabled));
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new Slideout_Map2_Options(_parent, toolbar, Map2View.this, _state);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }
   }

   private class ActionSearchTourByLocation extends Action {

      public ActionSearchTourByLocation() {

         setText(Messages.Map_Action_SearchTourByLocation);
         setToolTipText(Messages.Map_Action_SearchTourByLocation_Tooltip);
         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.SearchTours_ByLocation));
      }

      @Override
      public void runWithEvent(final Event event) {

         _map.actionSearchTourByLocation(event);
      }
   }

   private class ActionShowPhotos extends ActionToolbarSlideout {

      public ActionShowPhotos() {

         super(TourbookPlugin.getThemedImageDescriptor(Images.ShowPhotos_InMap),
               TourbookPlugin.getThemedImageDescriptor(Images.ShowPhotos_InMap_Disabled));

         isToggleAction = true;
         notSelectedTooltip = Messages.Map_Action_ShowPhotos_Tooltip;
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {
         return new Slideout_Map2_PhotoOptions(_parent, toolbar, Map2View.this, _state);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }

      @Override
      protected void onSelect() {

         super.onSelect();

         actionShowPhotos(getSelection());
      }
   }

   private class ActionShowTour extends ActionToolbarSlideout {

      public ActionShowTour() {

         super(TourbookPlugin.getThemedImageDescriptor(Images.TourChart),
               TourbookPlugin.getThemedImageDescriptor(Images.TourChart_Disabled));

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

   private class ActionSyncMap extends ActionToolbarSlideout {

      public ActionSyncMap() {

         super(TourbookPlugin.getThemedImageDescriptor(Images.SyncMap),
               TourbookPlugin.getThemedImageDescriptor(Images.SyncMap_Disabled));

         isToggleAction = true;
         isShowSlideoutAlways = true;

         /*
          * Register other action images
          */

         // image 0: tour
         addOtherEnabledImage(TourbookPlugin.getThemedImageDescriptor(Images.SyncWith_Tour));

         // image 1: one slider
         addOtherEnabledImage(TourbookPlugin.getThemedImageDescriptor(Images.SyncWith_Slider));

         // image 2: centered sliders
         addOtherEnabledImage(TourbookPlugin.getThemedImageDescriptor(Images.SyncWith_Slider_Centered));

         // image 3: value point
         addOtherEnabledImage(TourbookPlugin.getThemedImageDescriptor(Images.SyncWith_ValuePoint));

         // image 4: other map
         addOtherEnabledImage(TourbookPlugin.getThemedImageDescriptor(Images.SyncWith_OtherMap));

         // image 5: photo
         addOtherEnabledImage(TourbookPlugin.getThemedImageDescriptor(Images.SyncWith_Photo));
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new Slideout_Map2_SyncMap(_parent, toolbar, Map2View.this);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }

      @Override
      protected void onSelect() {

         super.onSelect();

         syncMap_OnSelectSyncAction(getSelection());
      }
   }

   private enum MapSyncMode {

      /**
       * Image: 0
       */
      IsSyncWith_Tour,

      /**
       * Image: 1
       */
      IsSyncWith_Slider_One,

      /**
       * Image: 2
       */
      IsSyncWith_Slider_Center,

      /**
       * Image: 3
       */
      IsSyncWith_ValuePoint,

      /**
       * Image: 4
       */
      IsSyncWith_OtherMap,

      /**
       * Image: 5
       */
      IsSyncWith_Photo,

      IsSyncWith_NONE,
   }

   public Map2View() {}

   /**
    * @return Returns the map default state
    */
   public static IDialogSettings getState() {
      return _state;
   }

   public void action_SyncWith_ChartSlider(final boolean isActionCentered) {

      // uncheck the other action
      final boolean isChecked_Center = _actionSyncMapWith_Slider_Centered.isChecked();
      final boolean isChecked_One = _actionSyncMapWith_Slider_One.isChecked();

      if (isActionCentered) {

         // sync with both centered sliders

         _actionSyncMapWith_Slider_One.setChecked(false);
         _currentMapSyncMode = MapSyncMode.IsSyncWith_Slider_Center;

      } else {

         // sync with one slider

         _actionSyncMapWith_Slider_Centered.setChecked(false);
         _currentMapSyncMode = MapSyncMode.IsSyncWith_Slider_One;
      }

      /*
       * Change state
       */
      _isMapSyncWith_Slider_One = isChecked_One || isChecked_Center;
      _isMapSyncWith_Slider_Centered = isActionCentered;

      // ensure data are available
      if (_allTourData.isEmpty()) {
         return;
      }

      if (_isMapSyncWith_Slider_One) {

         deactivateSyncWith_OtherMap();
         deactivateSyncWith_Photo();
         deactivateSyncWith_Tour();
         deactivateSyncWith_ValuePoint();

         // map must also be synched with selected tour
         _isMapSyncWith_Tour = true;

         _map.setShowOverlays(true);

         final TourData firstTourData = _allTourData.get(0);

         positionMapTo_0_TourSliders(
               firstTourData,
               _currentLeftSliderValueIndex,
               _currentRightSliderValueIndex,
               _currentSelectedSliderValueIndex,
               null);
      }

      syncMap_ShowCurrentSyncModeImage(_isMapSyncWith_Slider_One);
   }

   public void action_SyncWith_OtherMap(final boolean isSelected) {

      _isMapSyncWith_OtherMap = isSelected;
      _currentMapSyncMode = MapSyncMode.IsSyncWith_OtherMap;

      if (_isMapSyncWith_OtherMap) {

         deactivateSyncWith_Photo();
         deactivateSyncWith_Slider();
         deactivateSyncWith_Tour();
         deactivateSyncWith_ValuePoint();
      }

      syncMap_ShowCurrentSyncModeImage(_isMapSyncWith_OtherMap);
   }

   /**
    * Sync map with photo
    */
   public void action_SyncWith_Photo() {

      _isMapSyncWith_Photo = _actionSyncMapWith_Photo.isChecked();
      _currentMapSyncMode = MapSyncMode.IsSyncWith_Photo;

      if (_isMapSyncWith_Photo) {

         deactivateSyncWith_OtherMap();
         deactivateSyncWith_Tour();
         deactivateSyncWith_Slider();
         deactivateSyncWith_ValuePoint();

         centerPhotos(_filteredPhotos, false);

         _map.paint();
      }

      syncMap_ShowCurrentSyncModeImage(_isMapSyncWith_Photo);

      enableActions(true);
   }

   public void action_SyncWith_Tour() {

      _isMapSyncWith_Tour = _actionSyncMapWith_Tour.isChecked();
      _currentMapSyncMode = MapSyncMode.IsSyncWith_Tour;

      if (_allTourData.isEmpty()) {
         return;
      }

      if (_isMapSyncWith_Tour) {

         deactivateSyncWith_OtherMap();
         deactivateSyncWith_Photo();
         deactivateSyncWith_Slider();
         deactivateSyncWith_ValuePoint();

         // force tour to be repainted, that it is synched immediately
         _previousTourData = null;

         _actionShowTour.setSelection(true);
         _map.setShowOverlays(true);

         paintTours_20_One(_allTourData.get(0), true);
      }

      syncMap_ShowCurrentSyncModeImage(_isMapSyncWith_Tour);
   }

   public void action_SyncWith_ValuePoint() {

      _isMapSyncWith_ValuePoint = _actionSyncMapWith_ValuePoint.isChecked();
      _currentMapSyncMode = MapSyncMode.IsSyncWith_ValuePoint;

      if (_allTourData.isEmpty()) {
         return;
      }

      if (_isMapSyncWith_ValuePoint) {

         deactivateSyncWith_OtherMap();
         deactivateSyncWith_Photo();
         deactivateSyncWith_Slider();
         deactivateSyncWith_Tour();

         // map must also be synched with selected tour
         _isMapSyncWith_Tour = true;

         _map.setShowOverlays(true);

         positionMapTo_0_TourSliders(
               _allTourData.get(0), // sync with first tour
               _currentLeftSliderValueIndex,
               _currentRightSliderValueIndex,
               _currentSelectedSliderValueIndex,
               null);
      }

      syncMap_ShowCurrentSyncModeImage(_isMapSyncWith_ValuePoint);
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

   public void actionSetShowTourPausesInMap() {

      _tourPainterConfig.isShowTourPauses = _actionShowTourPauses.isChecked();

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
            getAction_TourColor(graphId).setChecked(false);
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

   public void actionShowPhotos(final boolean isSelected) {

      _isShowPhoto = isSelected;

      enableActions();

      _tourPainterConfig.isPhotoVisible = _isShowPhoto;

      _map.setOverlayKey(Integer.toString(_filteredPhotos.hashCode()));
      _map.disposeOverlayImageCache();

      _map.paint();
   }

   public void actionShowSlider() {

      if (_allTourData.isEmpty()) {
         return;
      }

      final boolean isShowSliderInMap = _actionShowSliderInMap.isChecked();

      // keep state for the slideout
      _state.put(Map2View.STATE_IS_SHOW_SLIDER_IN_MAP, isShowSliderInMap);

      // repaint map
      _directMappingPainter.setPaintContext(

            _map,
            _isShowTour,
            _allTourData.get(0),

            _currentLeftSliderValueIndex,
            _currentRightSliderValueIndex,
            _currentValuePointIndex,

            isShowSliderInMap,
            _actionShowSliderInLegend.isChecked(),
            _actionShowValuePoint.isChecked(),

            _sliderPathPaintingData);

      _map.redraw();
   }

   public void actionShowValuePoint() {

      if (_allTourData.size() > 0) {

         updateUI_HoveredValuePoint();
      }
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

      _map.setShowOverlays(true);

      paintEntireTour();
   }

   private void addMapListener() {

      _map.addMousePositionListener(mapPositionEvent -> _mapInfoManager.setMapPosition(
            mapPositionEvent.mapGeoPosition.latitude,
            mapPositionEvent.mapGeoPosition.longitude,
            mapPositionEvent.mapZoomLevel));

      _map.addPOIListener(mapPoiEvent -> {

         _poiPosition = mapPoiEvent.mapGeoPosition;
         _poiZoomLevel = mapPoiEvent.mapZoomLevel;
         _poiName = mapPoiEvent.mapPOIText;

         _actionShowPOI.setEnabled(true);
         _actionShowPOI.setChecked(true);
      });

      _map.addHoveredTourListener(mapHoveredTourEvent -> {

         final long hoveredTourId = mapHoveredTourEvent.hoveredTourId;

         _actionCreateTourMarkerFromMap.setCurrentHoverTourId(hoveredTourId);
         _actionCreateTourMarkerFromMap.setEnabled(hoveredTourId != Integer.MIN_VALUE);
      });

      _map.addTourSelectionListener((selection, isSelectAlsoInThisView) -> {

         if (isSelectAlsoInThisView) {

            _map.getDisplay().asyncExec(() -> {

               if (selection instanceof SelectionTourIds) {

                  // clone tour id's otherwise the original could be removed
                  final SelectionTourIds selectionTourIds = (SelectionTourIds) selection;

                  final ArrayList<Long> allTourIds = new ArrayList<>();
                  allTourIds.addAll(selectionTourIds.getTourIds());

                  onSelectionChanged(new SelectionTourIds(allTourIds), false);
               }
            });
         }

         TourManager.fireEventWithCustomData(
               TourEventId.TOUR_SELECTION,
               selection,
               Map2View.this);
      });

      _map.addMapGridBoxListener((map_ZoomLevel, map_GeoCenter, isGridSelected, mapGridData) -> {

         if (isGridSelected) {

            TourGeoFilter_Manager.createAndSetGeoFilter(map_ZoomLevel, map_GeoCenter, mapGridData);

         } else {

            geoFilter_10_Loader(mapGridData, null);
         }
      });

      _map.addMapInfoListener(this);
      _map.addMapPositionListener(this);

      _map.setMapContextProvider(this);
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {

         private void onPartVisible(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == Map2View.this && _isPartVisible == false) {

               _isPartVisible = true;

               if (_selectionWhenHidden != null) {

                  onSelectionChanged(_selectionWhenHidden, true);

                  _selectionWhenHidden = null;
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

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(PREF_SHOW_TILE_INFO)
               || property.equals(PREF_SHOW_TILE_BORDER)
               || property.equals(PREF_DEBUG_MAP_SHOW_GEO_GRID)) {

            // map properties has changed

            final boolean isShowGeoGrid = _prefStore.getBoolean(PREF_DEBUG_MAP_SHOW_GEO_GRID);
            final boolean isShowTileInfo = _prefStore.getBoolean(PREF_SHOW_TILE_INFO);
            final boolean isShowTileBorder = _prefStore.getBoolean(PREF_SHOW_TILE_BORDER);

            _map.setShowDebugInfo(isShowTileInfo, isShowTileBorder, isShowGeoGrid);
            _map.paint();

         } else if (property.equals(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD)
               || property.equals(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD_WARNING)) {

            final String tourPaintMethod = _prefStore.getString(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD);
            final boolean isShowPaintingMethodWarning = _prefStore.getBoolean(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD_WARNING);

            _map.setTourPaintMethodEnhanced(PrefPage_Map2_Appearance.TOUR_PAINT_METHOD_COMPLEX.equals(tourPaintMethod), isShowPaintingMethodWarning);

         } else if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)
               || property.equals(ITourbookPreferences.MAP2_OPTIONS_IS_MODIFIED)) {

            // update tour and legend

            createLegendImage(_tourPainterConfig.getMapColorProvider());

            _map.updateGraphColors();

            _map.disposeOverlayImageCache();
            _map.paint();

         } else if (property.equals(IPreferences.SRTM_COLORS_SELECTED_PROFILE_KEY)) {

            final String newValue = propertyChangeEvent.getNewValue().toString();
            final Integer prefProfileKey = Integer.valueOf(newValue);

            if (prefProfileKey != _selectedProfileKey) {

               _selectedProfileKey = prefProfileKey;

               _map.disposeTiles();
               _map.paint();
            }

         } else if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

            // this can occur when tour geo filter color is modified

            _map.paint();
         }
      };

      _prefChangeListener_Common = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

            // measurement system has changed

            _map.setMeasurementSystem(UI.UNIT_VALUE_DISTANCE, UI.UNIT_LABEL_DISTANCE);

            createLegendImage(_tourPainterConfig.getMapColorProvider());

            _map.paint();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   /**
    * listen for events when a tour is selected
    */
   private void addSelectionListener() {

      _postSelectionListener = (part, selection) -> onSelectionChanged(selection, true);

      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = (part, eventId, eventData) -> {

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

         } else if (eventId == TourEventId.PAUSE_SELECTION && eventData instanceof SelectionTourPause) {

            onSelectionChanged_TourPause((SelectionTourPause) eventData, false);

         } else if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

            onSelectionChanged((ISelection) eventData, true);

         } else if (eventId == TourEventId.SLIDER_POSITION_CHANGED && eventData instanceof ISelection) {

            onSelectionChanged((ISelection) eventData, true);

         } else if (eventId == TourEventId.MAP_SHOW_GEO_GRID) {

            if (eventData instanceof TourGeoFilter) {

               // show geo filter

               _map.tourBreadcrumb().resetTours();

               final TourGeoFilter tourGeoFilter = (TourGeoFilter) eventData;

               // show search rectangle
               _map.showGeoGrid(tourGeoFilter);

               // show tours in search rectangle
               geoFilter_10_Loader(tourGeoFilter.mapGridData, tourGeoFilter);

            } else if (eventData == null) {

               // hide geo grid

               hideGeoGrid();
            }

         } else if (eventId == TourEventId.HOVERED_VALUE_POSITION && eventData instanceof HoveredValueData) {

            onSelection_HoveredValue((HoveredValueData) eventData);

         } else if (eventId == TourEventId.SEGMENT_LAYER_CHANGED) {

            resetMap();
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

      showDefaultMap(false);

      enableActions();
   }

   @Override
   public void closeOpenedDialogs(final IOpeningDialog openingDialog) {
      _openDlgMgr.closeOpenedDialogs(openingDialog);
   }

   private void createActions() {

      _actionTourColor_Elevation = new ActionTourColor(
            this,
            MapGraphId.Altitude,
            Messages.map_action_tour_color_altitude_tooltip,
            Images.Graph_Elevation,
            Images.Graph_Elevation_Disabled);

      _actionTourColor_Gradient = new ActionTourColor(
            this,
            MapGraphId.Gradient,
            Messages.map_action_tour_color_gradient_tooltip,
            Images.Graph_Gradient,
            Images.Graph_Gradient_Disabled);

      _actionTourColor_Pulse = new ActionTourColor(
            this,
            MapGraphId.Pulse,
            Messages.map_action_tour_color_pulse_tooltip,
            Images.Graph_Heartbeat,
            Images.Graph_Heartbeat_Disabled);

      _actionTourColor_Speed = new ActionTourColor(
            this,
            MapGraphId.Speed,
            Messages.map_action_tour_color_speed_tooltip,
            Images.Graph_Speed,
            Images.Graph_Speed_Disabled);

      _actionTourColor_Pace = new ActionTourColor(
            this,
            MapGraphId.Pace,
            Messages.map_action_tour_color_pase_tooltip,
            Images.Graph_Pace,
            Images.Graph_Pace_Disabled);

      _actionTourColor_RunDyn_StepLength = new ActionTourColor(
            this,
            MapGraphId.RunDyn_StepLength,
            Messages.Tour_Action_RunDyn_StepLength_Tooltip,
            Images.Graph_RunDyn_StepLength,
            Images.Graph_RunDyn_StepLength_Disabled);

      _actionTourColor_HrZone = new ActionTourColor(
            this,
            MapGraphId.HrZone,
            Messages.Tour_Action_ShowHrZones_Tooltip,
            Images.PulseZones,
            Images.PulseZones_Disabled);

// SET_FORMATTING_OFF

      _allTourColor_Actions.put(MapGraphId.Altitude,           _actionTourColor_Elevation);
      _allTourColor_Actions.put(MapGraphId.Gradient,           _actionTourColor_Gradient);
      _allTourColor_Actions.put(MapGraphId.Pulse,              _actionTourColor_Pulse);
      _allTourColor_Actions.put(MapGraphId.Speed,              _actionTourColor_Speed);
      _allTourColor_Actions.put(MapGraphId.Pace,               _actionTourColor_Pace);
      _allTourColor_Actions.put(MapGraphId.HrZone,             _actionTourColor_HrZone);
      _allTourColor_Actions.put(MapGraphId.RunDyn_StepLength,  _actionTourColor_RunDyn_StepLength);

      // map2 slideouts
      _actionMap2_Bookmarks               = new ActionMapBookmarks(this._parent, this);
      _actionMap2_Color                   = new ActionMap2Color();
      _actionMap2_MapProvider             = new ActionMap2_MapProvider(this, _state_MapProvider);
      _actionMap2_PhotoFilter             = new ActionMap2_PhotoFilter(this, _state_PhotoFilter);
      _actionMap2_Options                 = new ActionMap2_Options();
      _actionMap2_SyncMap                 = new ActionSyncMap();
      _actionMap2_TourColors              = new ActionMap2_Graphs();

      _actionZoom_In                      = new ActionZoomIn(this);
      _actionZoom_Out                     = new ActionZoomOut(this);
      _actionZoom_Centered                = new ActionZoomCentered(this);
      _actionZoom_ShowEntireMap           = new ActionZoomShowEntireMap(this);
      _actionZoom_ShowEntireTour          = new ActionZoomShowEntireTour(this);

      _actionCreateTourMarkerFromMap      = new ActionCreateTourMarkerFromMap(this);
      _actionEditMap2Preferences          = new ActionOpenPrefDialog(Messages.Map_Action_Edit2DMapPreferences, PrefPage_Map2_Appearance.ID);
      _actionManageMapProvider            = new ActionManageMapProviders(this);
      _actionReloadFailedMapImages        = new ActionReloadFailedMapImages(this);
      _actionSaveDefaultPosition          = new ActionSaveDefaultPosition(this);
      _actionExportMap_SubMenu            = new Action_ExportMap_SubMenu(this);
      _actionSearchTourByLocation         = new ActionSearchTourByLocation();
      _actionSetDefaultPosition           = new ActionSetDefaultPosition(this);
      _actionShowAllFilteredPhotos        = new ActionShowAllFilteredPhotos(this);
      _actionShowLegendInMap              = new ActionShowLegendInMap(this);
      _actionShowPhotos                   = new ActionShowPhotos();
      _actionShowScaleInMap               = new ActionShowScaleInMap(this);
      _actionShowSliderInMap              = new ActionShowSliderInMap(this);
      _actionShowSliderInLegend           = new ActionShowSliderInLegend(this);
      _actionShowStartEndInMap            = new ActionShowStartEndInMap(this);
      _actionShowValuePoint               = new ActionShowValuePoint(this);
      _actionShowPOI                      = new ActionShowPOI(this);
      _actionShowTour                     = new ActionShowTour();
      _actionShowTourInfoInMap            = new ActionShowTourInfoInMap(this);
      _actionShowTourMarker               = new ActionShowTourMarker(this);
      _actionShowTourPauses               = new ActionShowTourPauses(this);
      _actionShowWayPoints                = new ActionShowWayPoints(this);
      _actionZoomLevelAdjustment          = new ActionZoomLevelAdjustment();

      // map sync actions
      _actionSyncMapWith_OtherMap         = new ActionSyncMapWith_OtherMap(this);
      _actionSyncMapWith_Photo            = new ActionSyncMapWith_Photo(this);
      _actionSyncMapWith_Slider_Centered  = new ActionSyncMapWith_Slider_Centered(this);
      _actionSyncMapWith_Slider_One       = new ActionSyncMapWith_Slider_One(this);
      _actionSyncMapWith_Tour             = new ActionSyncMapWith_Tour(this);
      _actionSyncMapWith_ValuePoint       = new ActionSyncMapWith_ValuePoint(this);

      _allSyncMap_Actions.put(MapSyncId.SyncMapWith_OtherMap,           _actionSyncMapWith_OtherMap);
      _allSyncMap_Actions.put(MapSyncId.SyncMapWith_Photo,              _actionSyncMapWith_Photo);
      _allSyncMap_Actions.put(MapSyncId.SyncMapWith_Slider_One,         _actionSyncMapWith_Slider_One);
      _allSyncMap_Actions.put(MapSyncId.SyncMapWith_Slider_Centered,    _actionSyncMapWith_Slider_Centered);
      _allSyncMap_Actions.put(MapSyncId.SyncMapWith_Tour,               _actionSyncMapWith_Tour);
      _allSyncMap_Actions.put(MapSyncId.SyncMapWith_ValuePoint,         _actionSyncMapWith_ValuePoint);

// SET_FORMATTING_ON
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
         legendImage = null;
      }

      final int legendWidth = IMapColorProvider.DEFAULT_LEGEND_WIDTH;
      int legendHeight = IMapColorProvider.DEFAULT_LEGEND_HEIGHT;

      final Rectangle mapBounds = _map.getBounds();
      legendHeight = Math.max(1, Math.min(legendHeight, mapBounds.height - IMapColorProvider.LEGEND_TOP_MARGIN));

      boolean isDataAvailable = false;

      if (mapColorProvider instanceof IGradientColorProvider) {

         final int legendHeightNoMargin = legendHeight - 2 * IMapColorProvider.LEGEND_MARGIN_TOP_BOTTOM;

         isDataAvailable = MapUtils.configureColorProvider(
               _allTourData,
               (IGradientColorProvider) mapColorProvider,
               ColorProviderConfig.MAP2,
               legendHeightNoMargin);

         if (isDataAvailable) {

            legendImage = TourMapPainter.createMap2_LegendImage_AWT((IGradientColorProvider) mapColorProvider,
                  legendWidth,
                  legendHeight,
                  isBackgroundDark(),
                  true // draw unit shadow
            );

         } else {

            // return null image to hide the legend
         }

      } else if (mapColorProvider instanceof IDiscreteColorProvider) {

         // return null image to hide the legend -> there is currenly no legend provider for a IDiscreteColorProvider

//         isDataAvailable = createLegendImage_20_SetProviderValues((IDiscreteColorProvider) mapColorProvider);
      }

      _mapLegend.setImage(legendImage);
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
      _map.setMeasurementSystem(UI.UNIT_VALUE_DISTANCE, UI.UNIT_LABEL_DISTANCE);

      final String tourPaintMethod = _prefStore.getString(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD);
      final boolean isShowPaintingMethodWarning = _prefStore.getBoolean(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD_WARNING);
      _map.setTourPaintMethodEnhanced(PrefPage_Map2_Appearance.TOUR_PAINT_METHOD_COMPLEX.equals(tourPaintMethod), isShowPaintingMethodWarning);

      // setup tool tip's
      _map.setTourToolTip(_tourToolTip = new TourToolTip(_map));
      _tourInfoToolTipProvider.setActionsEnabled(true);
      _tourInfoToolTipProvider.setNoTourTooltip(TOUR_TOOLTIP_LABEL_NO_GEO_TOUR);

      _map.addControlListener(controlResizedAdapter(ControlEvent -> {

         /*
          * Check if the legend size must be adjusted
          */
         final Image legendImage = _mapLegend.getImage();
         if ((legendImage == null) || legendImage.isDisposed()) {
            return;
         }

         if ((_isTourOrWayPoint == false) || (_isShowTour == false) || (_isShowLegend == false)) {
            return;
         }

         /*
          * Check height
          */
         final Rectangle mapBounds = _map.getBounds();
         final Rectangle legendBounds = legendImage.getBounds();

         final int mapHeight = mapBounds.height;
         final int defaultLegendHeight = IMapColorProvider.DEFAULT_LEGEND_HEIGHT;
         final int legendTopMargin = IMapColorProvider.LEGEND_TOP_MARGIN;

         if ((mapHeight < defaultLegendHeight + legendTopMargin)
               || ((mapHeight > defaultLegendHeight + legendTopMargin) && (legendBounds.height < defaultLegendHeight))) {

            createLegendImage(_tourPainterConfig.getMapColorProvider());
         }
      }));

      createActions();

      fillActionBars();

      addPartListener();
      addPrefListener();
      addSelectionListener();
      addTourEventListener();
      addMapListener();

      MapBookmarkManager.addBookmarkListener(this);
      PhotoManager.addPhotoEventListener(this);
      MapManager.addMapSyncListener(this);

      MapProviderManager.setMap2View(this);

      // register overlays which draw the tour
      GeoclipseExtensions.registerOverlays(_map);

      // initialize map when part is created and the map size is > 0
      _map.getDisplay().asyncExec(() -> {

         restoreState();
         enableActions();

         if (_allTourData.isEmpty()) {
            // a tour is not displayed, find a tour provider which provides a tour
            showToursFromTourProvider();
         } else {
            _map.paint();
         }

         /*
          * Enable map drawing, this is done very late to disable flickering which is caused by
          * setting up the map
          */
         _map.setPainting(true);

         final boolean isMapDimmed = Util.getStateBoolean(_state, Map2View.STATE_IS_MAP_DIMMED, Map2View.STATE_IS_MAP_DIMMED_DEFAULT);
         if (isMapDimmed) {

            final float mapDimValue = Util.getStateInt(_state, Map2View.STATE_DIM_MAP_VALUE, Map2View.STATE_DIM_MAP_VALUE_DEFAULT);
            final float dimLevelPercent = mapDimValue / MAX_DIM_STEPS * 100;

            if (dimLevelPercent > 80) {
               showDimWarning();
            }
         }
      });
   }

   private void deactivateSyncWith_OtherMap() {

      // disable map sync

      _isMapSyncWith_OtherMap = false;
      _actionSyncMapWith_OtherMap.setChecked(false);
   }

   private void deactivateSyncWith_Photo() {

      // disable photo sync

      _isMapSyncWith_Photo = false;
      _actionSyncMapWith_Photo.setChecked(false);
   }

   private void deactivateSyncWith_Slider() {

      // disable slider sync

      _isMapSyncWith_Slider_One = false;

      _actionSyncMapWith_Slider_One.setChecked(false);
      _actionSyncMapWith_Slider_Centered.setChecked(false);
   }

   private void deactivateSyncWith_Tour() {

      // disable tour sync

      _isMapSyncWith_Tour = false;
      _actionSyncMapWith_Tour.setChecked(false);
   }

   private void deactivateSyncWith_ValuePoint() {

      _isMapSyncWith_ValuePoint = false;
      _actionSyncMapWith_ValuePoint.setChecked(false);
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

      MapProviderManager.setMap2View(null);

      MapBookmarkManager.removeBookmarkListener(this);
      MapManager.removeMapSyncListener(this);
      PhotoManager.removePhotoEventListener(this);
      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

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

      /*
       * Photo actions
       */
      final boolean isAllPhotoAvailable = _allPhotos.size() > 0;
      final boolean isFilteredPhotoAvailable = _filteredPhotos.size() > 0;
      final boolean canShowFilteredPhoto = isFilteredPhotoAvailable && _isShowPhoto;

      /*
       * sync photo has a higher priority than sync tour, both cannot be synced at the same time
       */
//      final boolean isPhotoSynced = canShowFilteredPhoto && _isMapSynchedWithPhoto;
//      final boolean canSyncTour = isPhotoSynced == false;

      _actionMap2_PhotoFilter.setEnabled(isAllPhotoAvailable && _isShowPhoto);
      _actionShowAllFilteredPhotos.setEnabled(canShowFilteredPhoto);
      _actionShowPhotos.setEnabled(isAllPhotoAvailable);
      _actionSyncMapWith_Photo.setEnabled(canShowFilteredPhoto);

      /*
       * Tour actions
       */
      final int numTours = _allTourData.size();
      final boolean isTourAvailable = numTours > 0;
      final boolean isMultipleTours = numTours > 1 && _isShowTour;
      final boolean isOneTour = _isTourOrWayPoint && isMultipleTours == false && _isShowTour;
      final boolean isOneTourHovered = _map.getHoveredTourId() != Integer.MIN_VALUE;

      _actionMap2_Color.setEnabled(isTourAvailable);

      _actionCreateTourMarkerFromMap.setEnabled(isTourAvailable && isOneTourHovered);
      _actionShowLegendInMap.setEnabled(_isTourOrWayPoint);
      _actionShowSliderInLegend.setEnabled(_isTourOrWayPoint && _isShowLegend);
      _actionShowSliderInMap.setEnabled(_isTourOrWayPoint);
      _actionShowStartEndInMap.setEnabled(isOneTour);
      _actionShowTourInfoInMap.setEnabled(isOneTour);
      _actionShowTour.setEnabled(_isTourOrWayPoint);
      _actionShowTourMarker.setEnabled(_isTourOrWayPoint);
      _actionShowTourPauses.setEnabled(_isTourOrWayPoint);
      _actionShowWayPoints.setEnabled(_isTourOrWayPoint);
      _actionZoom_Centered.setEnabled(isTourAvailable);
      _actionZoom_ShowEntireTour.setEnabled(_isTourOrWayPoint && _isShowTour && isTourAvailable);
      _actionZoomLevelAdjustment.setEnabled(isTourAvailable);

      _actionSyncMapWith_Slider_One.setEnabled(isTourAvailable);
      _actionSyncMapWith_Slider_Centered.setEnabled(isTourAvailable);
      _actionSyncMapWith_OtherMap.setEnabled(true);
      _actionSyncMapWith_Tour.setEnabled(isTourAvailable);
      _actionSyncMapWith_ValuePoint.setEnabled(isTourAvailable);

      syncMap_ShowCurrentSyncModeImage(isMapSynched());

      if (numTours == 0) {

         _actionTourColor_Elevation.setEnabled(false);
         _actionTourColor_Gradient.setEnabled(false);
         _actionTourColor_Pulse.setEnabled(false);
         _actionTourColor_Speed.setEnabled(false);
         _actionTourColor_Pace.setEnabled(false);
         _actionTourColor_HrZone.setEnabled(false);
         _actionTourColor_RunDyn_StepLength.setEnabled(false);

      } else if (isForceTourColor) {

         _actionTourColor_Elevation.setEnabled(true);
         _actionTourColor_Gradient.setEnabled(true);
         _actionTourColor_Pulse.setEnabled(true);
         _actionTourColor_Speed.setEnabled(true);
         _actionTourColor_Pace.setEnabled(true);
         _actionTourColor_HrZone.setEnabled(true);
         _actionTourColor_RunDyn_StepLength.setEnabled(true);

      } else if (isOneTour) {

         final TourData oneTourData = _allTourData.get(0);
         final boolean isPulse = oneTourData.pulseSerie != null;
         final boolean canShowHrZones = oneTourData.getNumberOfHrZones() > 0 && isPulse;

         _actionTourColor_Elevation.setEnabled(true);
         _actionTourColor_Gradient.setEnabled(oneTourData.getGradientSerie() != null);
         _actionTourColor_Pulse.setEnabled(isPulse);
         _actionTourColor_Speed.setEnabled(oneTourData.getSpeedSerie() != null);
         _actionTourColor_Pace.setEnabled(oneTourData.getPaceSerie() != null);
         _actionTourColor_HrZone.setEnabled(canShowHrZones);
         _actionTourColor_RunDyn_StepLength.setEnabled(oneTourData.runDyn_StepLength != null);

      } else {

         _actionTourColor_Elevation.setEnabled(false);
         _actionTourColor_Gradient.setEnabled(false);
         _actionTourColor_Pulse.setEnabled(false);
         _actionTourColor_Speed.setEnabled(false);
         _actionTourColor_Pace.setEnabled(false);
         _actionTourColor_HrZone.setEnabled(false);
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

      tbm.add(_actionShowPhotos);
      tbm.add(_actionMap2_PhotoFilter);
      tbm.add(_actionShowAllFilteredPhotos);

      tbm.add(new Separator());

      tbm.add(_actionMap2_Bookmarks);

      tbm.add(new Separator());

      tbm.add(_actionShowTour);
      tbm.add(_actionZoom_ShowEntireTour);
      tbm.add(_actionMap2_SyncMap);

      tbm.add(new Separator());

      tbm.add(_actionZoom_In);
      tbm.add(_actionZoom_Out);
      tbm.add(_actionZoom_ShowEntireMap);
      tbm.add(_actionZoom_Centered);

      tbm.add(new Separator());

      tbm.add(_actionMap2_MapProvider);
      tbm.add(_actionMap2_Options);
   }

   @Override
   public void fillContextMenu(final IMenuManager menuMgr, final ActionManageOfflineImages actionManageOfflineImages) {

      fillMapContextMenu(menuMgr, actionManageOfflineImages);
   }

   private void fillMapContextMenu(final IMenuManager menuMgr, final ActionManageOfflineImages actionManageOfflineImages) {

      menuMgr.add(_actionSearchTourByLocation);
      menuMgr.add(new Separator());

      menuMgr.add(_actionShowLegendInMap);
      menuMgr.add(_actionShowScaleInMap);
      menuMgr.add(_actionShowValuePoint);
      menuMgr.add(_actionShowSliderInMap);
      menuMgr.add(_actionShowSliderInLegend);

      menuMgr.add(new Separator());
      menuMgr.add(_actionCreateTourMarkerFromMap);
      menuMgr.add(_actionShowTourMarker);
      menuMgr.add(_actionShowTourPauses);
      menuMgr.add(_actionShowWayPoints);
      menuMgr.add(_actionShowPOI);
      menuMgr.add(_actionShowStartEndInMap);
      menuMgr.add(_actionShowTourInfoInMap);

      if (isShowTrackColor_InContextMenu()) {
         menuMgr.add(_actionMap2_Color);
      }

      menuMgr.add(new Separator());

      MapBookmarkManager.fillContextMenu_RecentBookmarks(menuMgr, this);

      menuMgr.add(_actionSetDefaultPosition);
      menuMgr.add(_actionSaveDefaultPosition);

      menuMgr.add(new Separator());

      menuMgr.add(_actionExportMap_SubMenu);
      menuMgr.add(_actionZoomLevelAdjustment);
      menuMgr.add(_actionEditMap2Preferences);

      menuMgr.add(new Separator());
      menuMgr.add(actionManageOfflineImages);
      menuMgr.add(_actionReloadFailedMapImages);
      menuMgr.add(_actionManageMapProvider);
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
               _allTourColor_Actions.get(graphId));
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

      // delay geo part loader, moving the mouse can occur very often
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

   IAction getAction_MapSync(final MapSyncId syncId) {

      return _allSyncMap_Actions.get(syncId);
   }

   IAction getAction_TourColor(final MapGraphId graphId) {

      return _allTourColor_Actions.get(graphId);
   }

   private IMapColorProvider getColorProvider(final MapGraphId colorId) {

//      final ColorDefinition colorDefinition = GraphColorManager.getInstance().getColorDefinition(colorId);
//
//      final IMapColorProvider colorProvider = colorDefinition.getMap2Color_Active();
//
//      return colorProvider;

      return MapColorProvider.getActiveMap2ColorProvider(colorId);
   }

   /**
    * @return Returns a list with all filtered photos
    */
   @Override
   public ArrayList<Photo> getFilteredPhotos() {
      return _filteredPhotos;
   }

   public Map getMap() {
      return _map;
   }

   @Override
   public MapLocation getMapLocation() {

      final GeoPosition mapPosition = _map.getMapGeoCenter();
      final int mapZoomLevel = _map.getZoom() - 1;

      return new MapLocation(mapPosition, mapZoomLevel);
   }

   public Image getMapViewImage() {

      final Image image = new Image(_parent.getDisplay(),
            _parent.getSize().x,
            _parent.getSize().y);

      final GC gc = new GC(image);
      _parent.print(gc);
      //This produces the same result
      //  final GC gc = new GC(_parent);
      //  gc.copyArea(image, 0, 0);
      gc.dispose();

      return image;
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

   /**
    * @return Returns a list with all available photos.
    */
   @Override
   public ArrayList<Photo> getPhotos() {
      return _allPhotos;
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

   private Set<GeoPosition> getXSliderGeoPositions(final TourData tourData,
                                                   int valueIndex1,
                                                   int valueIndex2) {

      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      final int serieSize = latitudeSerie.length;

      // check bounds -> this problem occurred several times
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

   /**
    * @return Returns <code>true</code> when the map is dimmed to a specific level
    */
   boolean isBackgroundDark() {

      final boolean isMapDimmed = Util.getStateBoolean(_state, Map2View.STATE_IS_MAP_DIMMED, Map2View.STATE_IS_MAP_DIMMED_DEFAULT);
      final float mapDimValue = Util.getStateInt(_state, Map2View.STATE_DIM_MAP_VALUE, Map2View.STATE_DIM_MAP_VALUE_DEFAULT);

      final float dimLevelPercent = mapDimValue / MAX_DIM_STEPS * 100;

      return isMapDimmed && dimLevelPercent >= 30;
   }

   private boolean isMapSynched() {

      return false

            || _isMapSyncWith_OtherMap
            || _isMapSyncWith_Photo
            || _isMapSyncWith_Slider_Centered
            || _isMapSyncWith_Slider_One
            || _isMapSyncWith_Tour
            || _isMapSyncWith_ValuePoint

      ;
   }

   private boolean isShowTrackColor_InContextMenu() {

      // it can happen that color id is not yet set
      if (_tourColorId != null) {

         // set color before menu is filled, this sets the action image and color id
         _actionMap2_Color.setColorId(_tourColorId);

         if (_tourColorId != MapGraphId.HrZone) {

            // hr zone has a different color provider and is not yet supported

            return true;
         }
      }

      return false;
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
   public void onMapBookmarkActionPerformed(final MapBookmark mapBookmark, final MapBookmarkEventType mapBookmarkEventType) {
      {
         if (mapBookmarkEventType == MapBookmarkEventType.MOVETO) {
            _isInSelectBookmark = true;
            moveToMapLocation(mapBookmark);
            _isInSelectBookmark = false;
         }
      }
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

   private void onSelection_HoveredValue(final HoveredValueData hoveredValueData) {

      _currentValuePointIndex = hoveredValueData.hoveredValuePointIndex;

      updateUI_HoveredValuePoint();
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
         if (tourIds.isEmpty()) {

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

            // use old behavior

            final ChartDataModel chartDataModel = chartInfo.chartDataModel;
            if (chartDataModel != null) {

               final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
               if (tourId instanceof Long) {

                  tourData = TourManager.getInstance().getTourData((Long) tourId);
                  if (tourData == null) {

                     // tour is not in the database, try to get it from the raw data manager

                     final java.util.Map<Long, TourData> rawData = RawDataManager.getInstance().getImportedTours();
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

                  positionMapTo_0_TourSliders(
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

         positionMapTo_0_TourSliders(
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
            final TourData tourData = TourManager.getInstance().getTourData(compareResultItem.getTourId());

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

            positionMapTo_0_TourSliders(
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
            _map.getDisplay().timerExec(500, () -> _map.setPOI(_wayPointToolTipProvider, wp));

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

      if (_isMapSyncWith_Tour || _isMapSyncWith_Slider_One) {

         if (isDrawSlider) {

            positionMapTo_0_TourSliders(
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

   private void onSelectionChanged_TourPause(final SelectionTourPause pauseSelection, final boolean isDrawSlider) {

      final TourData tourData = pauseSelection.getTourData();

      updateUI_ShowTour(tourData);

      final int leftSliderValueIndex = pauseSelection.getSerieIndex();

      if (_isMapSyncWith_Tour || _isMapSyncWith_Slider_One) {

         if (isDrawSlider) {

            positionMapTo_0_TourSliders(
                  tourData,
                  leftSliderValueIndex,
                  leftSliderValueIndex,
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

      final Set<GeoPosition> tourBounds = getTourBounds(_allTourData);

      _tourPainterConfig.setTourBounds(tourBounds);

      _directMappingPainter.disablePaintContext();

      _map.resetHoveredSelectedTours();
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

      /**
       * It is possible that sync photo action is disabled but map can be synched with photos. This
       * occure when show photos are deactivated but the photo sync is still selected.
       * <p>
       * To reactivate photo sync, first photos must be set visible.
       */
      if (_isMapSyncWith_Photo) {
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

         /*
          * Ensure the tour breadcrumb shows the correct values, the value is hidden when a geo tour
          * filter is reselected
          */
         _map.tourBreadcrumb().setTours(_allTourData);

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

         /*
          * Sort tours by date otherwise the chart value point, which is sorted by date, could show
          * the wrong tour -> complicated
          */
         Collections.sort(_allTourData);

         _hash_AllTourIds = allTourIds.hashCode();
         _hash_AllTourData = _allTourData.hashCode();
         _hash_TourOverlayKey = newOverlayKey;
      }

      _tourPainterConfig.setTourData(_allTourData, _isShowTour);
      _tourPainterConfig.setPhotos(_filteredPhotos, _isShowPhoto, _isLinkPhotoDisplayed);

      _tourInfoToolTipProvider.setTourDataList(_allTourData);

      _map.resetHoveredSelectedTours();
      _map.tourBreadcrumb().setTours(_allTourData);

      if (_previousOverlayKey != newOverlayKey) {

         _previousOverlayKey = newOverlayKey;

         _map.setOverlayKey(Long.toString(newOverlayKey));
         _map.disposeOverlayImageCache();
      }

      if (_isMapSyncWith_Tour && !_map.isSearchTourByLocation()) {

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

      if (_allTourData.isEmpty()) {
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
      if (forceRedraw == false && (_allTourData.size() == 1) && (_allTourData.get(0) == tourData)) {
         return;
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

      // reset also ALL tour id's, otherwise a reselected multiple tour is not displayed
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
            _currentValuePointIndex,

            _actionShowSliderInMap.isChecked(),
            _actionShowSliderInLegend.isChecked(),
            _actionShowValuePoint.isChecked(),

            _sliderPathPaintingData);

      // set the tour bounds
      final GeoPosition[] tourBounds = tourData.getGeoBounds();

      final HashSet<GeoPosition> tourBoundsSet = new HashSet<>();
      tourBoundsSet.add(tourBounds[0]);
      tourBoundsSet.add(tourBounds[1]);

      _tourPainterConfig.setTourBounds(tourBoundsSet);

      _map.resetHoveredSelectedTours();
      _map.tourBreadcrumb().setTours(tourData);
      _map.setShowOverlays(_isShowTour || _isShowPhoto);
      _map.setShowLegend(_isShowTour && _isShowLegend);

      /*
       * set position and zoom level for the tour
       */
      if (_isMapSyncWith_Tour && !_map.isSearchTourByLocation()) {

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

      _map.resetHoveredSelectedTours();
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

   public void photoFilter_UpdateFromAction(final boolean isFilterActive) {

      _isPhotoFilterActive = isFilterActive;

      updateFilteredPhotos();
   }

   private void photoFilter_UpdateFromSlideout(final int filterRatingStars, final PhotoRatingStarOperator ratingstaroperatorsvalues) {

      _photoFilter_RatingStars = filterRatingStars;
      _photoFilter_RatingStar_Operator = ratingstaroperatorsvalues;

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
            _currentValuePointIndex,

            _actionShowSliderInMap.isChecked(),
            _actionShowSliderInLegend.isChecked(),
            _actionShowValuePoint.isChecked(),

            _sliderPathPaintingData);

      if (_isMapSyncWith_Slider_One) {

         if (geoPositions != null) {

            // center to geo position

            positionMapTo_MapPosition(geoPositions, true);

         } else {

            if (_isMapSyncWith_Slider_Centered) {

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

      _map.setMapPosition(tourPositions, isAdjustZoomLevel, _tourPainterConfig.getZoomLevelAdjustment());
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

      Display.getDefault().asyncExec(() -> {

         if (_parent.isDisposed()) {
            return;
         }

         _map.redraw();
      });
   }

   private void resetMap() {

      if (_allTourData.isEmpty()) {
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

      // photo states
      _isShowPhoto = Util.getStateBoolean(_state, STATE_IS_SHOW_PHOTO_IN_MAP, true);
      _actionShowPhotos.setSelection(_isShowPhoto);

      _isPhotoFilterActive = Util.getStateBoolean(_state, STATE_IS_PHOTO_FILTER_ACTIVE, false);
      _actionMap2_PhotoFilter.setSelection(_isPhotoFilterActive);

      _photoFilter_RatingStars = Util.getStateInt(_state, STATE_PHOTO_FILTER_RATING_STARS, 0);
      _photoFilter_RatingStar_Operator = Util.getStateEnum(_state, STATE_PHOTO_FILTER_RATING_STAR_OPERATOR, PhotoRatingStarOperator.HAS_ANY);
      _actionMap2_PhotoFilter.getPhotoFilterSlideout().restoreState(_photoFilter_RatingStars, _photoFilter_RatingStar_Operator);

      // is show legend
      _isShowLegend = Util.getStateBoolean(_state, STATE_IS_SHOW_LEGEND_IN_MAP, true);
      _actionShowLegendInMap.setChecked(_isShowLegend);

      _actionShowValuePoint.setChecked(Util.getStateBoolean(_state, STATE_IS_SHOW_VALUE_POINT, STATE_IS_SHOW_VALUE_POINT_DEFAULT));

      // is tour centered
      final boolean isTourCentered = _state.getBoolean(STATE_IS_ZOOM_CENTERED);
      _actionZoom_Centered.setChecked(isTourCentered);
      _isPositionCentered = isTourCentered;

      // synch map with ...
      _currentMapSyncMode = (MapSyncMode) Util.getStateEnum(_state, STATE_MAP_SYNC_MODE, MapSyncMode.IsSyncWith_Tour);
      final boolean isSyncModeActive = _state.getBoolean(STATE_MAP_SYNC_MODE_IS_ACTIVE);
      syncMap_OnSelectSyncAction(isSyncModeActive);

      // zoom level adjustment
      _actionZoomLevelAdjustment.setZoomLevel(Util.getStateInt(_state, STATE_ZOOM_LEVEL_ADJUSTMENT, 0));

      // show start/end in map
      _actionShowStartEndInMap.setChecked(_state.getBoolean(STATE_IS_SHOW_START_END_IN_MAP));
      _tourPainterConfig.isShowStartEndInMap = _actionShowStartEndInMap.isChecked();

      // show tour marker
      final boolean isShowMarker = Util.getStateBoolean(_state, STATE_IS_SHOW_TOUR_MARKER, true);
      _actionShowTourMarker.setChecked(isShowMarker);
      _tourPainterConfig.isShowTourMarker = isShowMarker;

      // show tour pauses
      final boolean isShowPauses = Util.getStateBoolean(_state, STATE_IS_SHOW_TOUR_PAUSES, true);
      _actionShowTourPauses.setChecked(isShowPauses);
      _tourPainterConfig.isShowTourPauses = isShowPauses;

      // checkbox: show way points
      final boolean isShowWayPoints = Util.getStateBoolean(_state, STATE_IS_SHOW_WAY_POINTS, true);
      _actionShowWayPoints.setChecked(isShowWayPoints);
      _tourPainterConfig.isShowWayPoints = isShowWayPoints;
      if (isShowWayPoints) {
         _tourToolTip.addToolTipProvider(_wayPointToolTipProvider);
      }

      // show tour info in map
      final boolean isShowTourInfo = Util.getStateBoolean(_state, STATE_IS_SHOW_TOUR_INFO_IN_MAP, true);
      _actionShowTourInfoInMap.setChecked(isShowTourInfo);
      if (isShowTourInfo) {
         _tourToolTip.addToolTipProvider(_tourInfoToolTipProvider);
      }

      // show scale
      final boolean isScaleVisible = Util.getStateBoolean(_state, STATE_IS_SHOW_SCALE_IN_MAP, true);
      _actionShowScaleInMap.setChecked(isScaleVisible);
      _map.setShowScale(isScaleVisible);

      // show slider
      _actionShowSliderInMap.setChecked(Util.getStateBoolean(_state,
            STATE_IS_SHOW_SLIDER_IN_MAP,
            STATE_IS_SHOW_SLIDER_IN_MAP_DEFAULT));

      _actionShowSliderInLegend.setChecked(_state.getBoolean(STATE_IS_SHOW_SLIDER_IN_LEGEND));

      // restore map provider by selecting the last used map factory
      _actionMap2_MapProvider.selectMapProvider(_state.get(STATE_SELECTED_MAP_PROVIDER_ID));

      // default position
      _defaultZoom = Util.getStateInt(_state, STATE_DEFAULT_POSITION_ZOOM, 10);
      _defaultPosition = new GeoPosition(
            Util.getStateDouble(_state, STATE_DEFAULT_POSITION_LATITUDE, 46.303074),
            Util.getStateDouble(_state, STATE_DEFAULT_POSITION_LONGITUDE, 7.526386));

      // tour color
      try {

         final String stateColorId = Util.getStateString(_state, STATE_TOUR_COLOR_ID, MapGraphId.Altitude.name());

         MapGraphId colorId;
         try {
            colorId = MapGraphId.valueOf(stateColorId);
         } catch (final Exception e) {
            // set default
            colorId = MapGraphId.Altitude;
         }

         switch (colorId) {
         case Altitude:
            _actionTourColor_Elevation.setChecked(true);
            break;

         case Gradient:
            _actionTourColor_Gradient.setChecked(true);
            break;

         case Pace:
            _actionTourColor_Pace.setChecked(true);
            break;

         case Pulse:
            _actionTourColor_Pulse.setChecked(true);
            break;

         case Speed:
            _actionTourColor_Speed.setChecked(true);
            break;

         case HrZone:
            _actionTourColor_HrZone.setChecked(true);
            break;

         case RunDyn_StepLength:
            _actionTourColor_RunDyn_StepLength.setChecked(true);
            break;

         default:
            _actionTourColor_Elevation.setChecked(true);
            break;
         }

         setTourPainterColorProvider(colorId);

      } catch (final NumberFormatException e) {
         _actionTourColor_Elevation.setChecked(true);
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
      restoreState_Map2_TrackOptions(false);
      restoreState_Map2_Options();

      // display the map with the default position
      actionSetDefaultPosition();
   }

   void restoreState_Map2_Options() {

// SET_FORMATTING_OFF

      /*
       * Hovered/selected tour
       */
      final boolean isShowHoveredSelectedTour   = Util.getStateBoolean(_state,   Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR,                     Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT);

      final int hoveredOpacity                  = Util.getStateInt(_state,       Map2View.STATE_HOVERED_SELECTED__HOVERED_OPACITY,                 Map2View.STATE_HOVERED_SELECTED__HOVERED_OPACITY_DEFAULT);
      final int hoveredAndSelectedOpacity       = Util.getStateInt(_state,       Map2View.STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_OPACITY,    Map2View.STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_OPACITY_DEFAULT);
      final int selectedOpacity                 = Util.getStateInt(_state,       Map2View.STATE_HOVERED_SELECTED__SELECTED_OPACITY,                Map2View.STATE_HOVERED_SELECTED__SELECTED_OPACITY_DEFAULT);
      final RGB hoveredRGB                      = Util.getStateRGB(_state,       Map2View.STATE_HOVERED_SELECTED__HOVERED_RGB,                     Map2View.STATE_HOVERED_SELECTED__HOVERED_RGB_DEFAULT);
      final RGB hoveredAndSelectedRGB           = Util.getStateRGB(_state,       Map2View.STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_RGB,        Map2View.STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_RGB_DEFAULT);
      final RGB selectedRGB                     = Util.getStateRGB(_state,       Map2View.STATE_HOVERED_SELECTED__SELECTED_RGB,                    Map2View.STATE_HOVERED_SELECTED__SELECTED_RGB_DEFAULT);

      _map.setConfig_HoveredSelectedTour(
            isShowHoveredSelectedTour,
            hoveredRGB,
            hoveredOpacity,
            hoveredAndSelectedRGB,
            hoveredAndSelectedOpacity,
            selectedRGB,
            selectedOpacity
      );

      /*
       * Tour direction
       */
      final boolean isShowTourDirection      = Util.getStateBoolean(_state,      Map2View.STATE_IS_SHOW_TOUR_DIRECTION,       Map2View.STATE_IS_SHOW_TOUR_DIRECTION_DEFAULT);
      final int tourDirection_MarkerGap      = Util.getStateInt(_state,          Map2View.STATE_TOUR_DIRECTION_MARKER_GAP,    Map2View.STATE_TOUR_DIRECTION_MARKER_GAP_DEFAULT);
      final int tourDirection_LineWidth      = Util.getStateInt(_state,          Map2View.STATE_TOUR_DIRECTION_LINE_WIDTH,    Map2View.STATE_TOUR_DIRECTION_LINE_WIDTH_DEFAULT);
      final float tourDirection_SymbolSize   = Util.getStateInt(_state,          Map2View.STATE_TOUR_DIRECTION_SYMBOL_SIZE,   Map2View.STATE_TOUR_DIRECTION_SYMBOL_SIZE_DEFAULT);
      final RGB tourDirection_RGB            = Util.getStateRGB(_state,          Map2View.STATE_TOUR_DIRECTION_RGB,           Map2View.STATE_TOUR_DIRECTION_RGB_DEFAULT);

      _map.setConfig_TourDirection(
            isShowTourDirection,
            tourDirection_MarkerGap,
            tourDirection_LineWidth,
            tourDirection_SymbolSize,
            tourDirection_RGB);

      _map.setIsInInverseKeyboardPanning(Util.getStateBoolean(_state,   Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING,   Map2View.STATE_IS_TOGGLE_KEYBOARD_PANNING_DEFAULT));
      _map.setIsZoomWithMousePosition(Util.getStateBoolean(_state,      Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION,  Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION_DEFAULT));

      /*
       * Set dim level/color after the map providers are set
       */
      final boolean isMapDimmed  = Util.getStateBoolean( _state, Map2View.STATE_IS_MAP_DIMMED, Map2View.STATE_IS_MAP_DIMMED_DEFAULT);
      final int mapDimValue      = Util.getStateInt(     _state, Map2View.STATE_DIM_MAP_VALUE, Map2View.STATE_DIM_MAP_VALUE_DEFAULT);
      final RGB mapDimColor      = Util.getStateRGB(     _state, Map2View.STATE_DIM_MAP_COLOR, Map2View.STATE_DIM_MAP_COLOR_DEFAULT);

      final boolean isBackgroundDark = isBackgroundDark();

      _map.setDimLevel(isMapDimmed, mapDimValue, mapDimColor, isBackgroundDark);
      _tourPainterConfig.isBackgroundDark = isBackgroundDark;

// SET_FORMATTING_ON

      // create legend image after the dim level is modified
      createLegendImage(_tourPainterConfig.getMapColorProvider());

      _map.paint();
   }

   void restoreState_Map2_TrackOptions(final boolean isUpdateMapUI) {

      _actionShowSliderInMap.setChecked(Util.getStateBoolean(_state,
            STATE_IS_SHOW_SLIDER_IN_MAP,
            Map2View.STATE_IS_SHOW_SLIDER_IN_MAP_DEFAULT));

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

      final boolean hasAnyStars = _photoFilter_RatingStar_Operator == PhotoRatingStarOperator.HAS_ANY;

      if (_isPhotoFilterActive && !hasAnyStars) {

         final boolean isNoStar = _photoFilter_RatingStars == 0;
         final boolean isEqual = _photoFilter_RatingStar_Operator == PhotoRatingStarOperator.IS_EQUAL;
         final boolean isMore = _photoFilter_RatingStar_Operator == PhotoRatingStarOperator.IS_MORE_OR_EQUAL;
         final boolean isLess = _photoFilter_RatingStar_Operator == PhotoRatingStarOperator.IS_LESS_OR_EQUAL;

         for (final Photo photo : _allPhotos) {

            final int ratingStars = photo.ratingStars;

            if (isNoStar && ratingStars == 0) {

               // only photos without stars are displayed

               _filteredPhotos.add(photo);

            } else if (isEqual && ratingStars == _photoFilter_RatingStars) {

               _filteredPhotos.add(photo);

            } else if (isMore && ratingStars >= _photoFilter_RatingStars) {

               _filteredPhotos.add(photo);

            } else if (isLess && ratingStars <= _photoFilter_RatingStars) {

               _filteredPhotos.add(photo);
            }
         }

      } else {

         // photo filter is not active or any stars can be selected -> show all photos

         _filteredPhotos.addAll(_allPhotos);
      }

      _tourPainterConfig.setPhotos(_filteredPhotos, _isShowPhoto, _isLinkPhotoDisplayed);

      enableActions(true);

      // update UI: photo filter slideout
      _actionMap2_PhotoFilter.updateUI();
      _actionMap2_PhotoFilter.getPhotoFilterSlideout().updateUI_NumberOfPhotos();
   }

   @PersistState
   private void saveState() {

// SET_FORMATTING_OFF

      _state.put(STATE_IS_SHOW_TOUR_IN_MAP,                       _isShowTour);
      _state.put(STATE_IS_SHOW_PHOTO_IN_MAP,                      _isShowPhoto);
      _state.put(STATE_IS_SHOW_LEGEND_IN_MAP,                     _isShowLegend);

      _state.put(STATE_IS_SHOW_VALUE_POINT,                       _actionShowValuePoint.isChecked());
      _state.put(STATE_IS_SHOW_START_END_IN_MAP,                  _actionShowStartEndInMap.isChecked());
      _state.put(STATE_IS_SHOW_SCALE_IN_MAP,                      _actionShowScaleInMap.isChecked());
      _state.put(STATE_IS_SHOW_SLIDER_IN_MAP,                     _actionShowSliderInMap.isChecked());
      _state.put(STATE_IS_SHOW_SLIDER_IN_LEGEND,                  _actionShowSliderInLegend.isChecked());
      _state.put(STATE_IS_SHOW_TOUR_MARKER,                       _actionShowTourMarker.isChecked());
      _state.put(STATE_IS_SHOW_TOUR_PAUSES,                       _actionShowTourPauses.isChecked());
      _state.put(STATE_IS_SHOW_TOUR_INFO_IN_MAP,                  _actionShowTourInfoInMap.isChecked());
      _state.put(STATE_IS_SHOW_WAY_POINTS,                        _actionShowWayPoints.isChecked());

      _state.put(STATE_MAP_SYNC_MODE_IS_ACTIVE,                   isMapSynched());
      Util.setStateEnum(_state, STATE_MAP_SYNC_MODE,              _currentMapSyncMode);

      _state.put(STATE_IS_ZOOM_CENTERED,                          _actionZoom_Centered.isChecked());
      _state.put(STATE_ZOOM_LEVEL_ADJUSTMENT,                     _actionZoomLevelAdjustment.getZoomLevel());

      final MP selectedMapProvider = _actionMap2_MapProvider.getSelectedMapProvider();
      if (selectedMapProvider != null) {
         _state.put(STATE_SELECTED_MAP_PROVIDER_ID,               selectedMapProvider.getId());
      }

      if (_defaultPosition == null) {

         final MP mapProvider = _map.getMapProvider();
         final int defaultZoom = mapProvider == null ? _defaultZoom : mapProvider.getMinimumZoomLevel();

         _state.put(STATE_DEFAULT_POSITION_ZOOM,         defaultZoom);

         _state.put(STATE_DEFAULT_POSITION_LATITUDE,     0.0F);
         _state.put(STATE_DEFAULT_POSITION_LONGITUDE,    0.0F);

      } else {

         _state.put(STATE_DEFAULT_POSITION_ZOOM,         _defaultZoom);
         _state.put(STATE_DEFAULT_POSITION_LATITUDE,     (float) _defaultPosition.latitude);
         _state.put(STATE_DEFAULT_POSITION_LONGITUDE,    (float) _defaultPosition.longitude);
      }

// SET_FORMATTING_ON

      // photo filter
      _state.put(STATE_IS_PHOTO_FILTER_ACTIVE, _actionMap2_PhotoFilter.getSelection());
      _state.put(STATE_PHOTO_FILTER_RATING_STARS, _photoFilter_RatingStars);
      Util.setStateEnum(_state, STATE_PHOTO_FILTER_RATING_STAR_OPERATOR, _photoFilter_RatingStar_Operator);
      _actionMap2_PhotoFilter.getPhotoFilterSlideout().saveState();

      // tour color
      MapGraphId colorId;

      if (_actionTourColor_Gradient.isChecked()) {
         colorId = MapGraphId.Gradient;

      } else if (_actionTourColor_Pulse.isChecked()) {
         colorId = MapGraphId.Pulse;

      } else if (_actionTourColor_Speed.isChecked()) {
         colorId = MapGraphId.Speed;

      } else if (_actionTourColor_Pace.isChecked()) {
         colorId = MapGraphId.Pace;

      } else if (_actionTourColor_HrZone.isChecked()) {
         colorId = MapGraphId.HrZone;

      } else if (_actionTourColor_RunDyn_StepLength.isChecked()) {
         colorId = MapGraphId.RunDyn_StepLength;

      } else {
         // use altitude as default
         colorId = MapGraphId.Altitude;
      }
      _state.put(STATE_TOUR_COLOR_ID, colorId.name());
   }

   private void selectTourSegments(final SelectedTourSegmenterSegments selectedSegmenterConfig) {

      if (_allTourData.isEmpty()) {
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

         positionMapTo_0_TourSliders(
               mapTourData,
               leftSliderValueIndex,
               rightSliderValueIndex,
               leftSliderValueIndex,
               mapPositions);

         enableActions();

      } else {

         // multiple tourdata, I'm not sure if this still occurs after merging multiple tours into one tourdata
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

   /**
    * Show map by removing/resetting all previously displayed tours
    *
    * @param isShowOverlays
    */
   private void showDefaultMap(final boolean isShowOverlays) {

      _tourInfoToolTipProvider.setTourData(null);

      // disable tour actions in this view
      _isTourOrWayPoint = false;

      // disable tour data
      _allTourData.clear();
      _previousTourData = null;
      _tourPainterConfig.resetTourData();

      // update direct painter to draw nothing
      _directMappingPainter.setPaintContext(

            _map,
            false,
            null,

            0,
            0,
            0,

            false,
            false,
            false, // show value point

            _sliderPathPaintingData);

      _map.resetHoveredSelectedTours();

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

         _map.getDisplay().asyncExec(() -> {

            final MessageDialogWithToggle dialog = MessageDialogWithToggle.openInformation(
                  _map.getDisplay().getActiveShell(),
                  Messages.map_dlg_dim_warning_title, // title
                  Messages.map_dlg_dim_warning_message, // message
                  Messages.map_dlg_dim_warning_toggle_message, // toggle message
                  false, // toggle default state
                  null,
                  null);

            _prefStore.setValue(
                  ITourbookPreferences.MAP_VIEW_CONFIRMATION_SHOW_DIM_WARNING,
                  dialog.getToggleState());
         });
      }
   }

   public void showMapProvider(final MP mapProvider) {

      _map.setMapProvider(mapProvider);
   }

   private void showToursFromTourProvider() {

      _map.getDisplay().asyncExec(() -> {

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
      });
   }

   private void syncMap_OnSelectSyncAction(final boolean isActionSelected) {

      if (isActionSelected) {

         switch (_currentMapSyncMode) {

         case IsSyncWith_Slider_Center:
            _actionSyncMapWith_Slider_Centered.setChecked(true);
            action_SyncWith_ChartSlider(true);
            break;

         case IsSyncWith_Slider_One:
            _actionSyncMapWith_Slider_One.setChecked(true);
            action_SyncWith_ChartSlider(false);
            break;

         case IsSyncWith_OtherMap:
            _actionSyncMapWith_OtherMap.setChecked(true);
            action_SyncWith_OtherMap(true);
            break;

         case IsSyncWith_Tour:
            _actionSyncMapWith_Tour.setChecked(true);
            action_SyncWith_Tour();
            break;

         case IsSyncWith_ValuePoint:
            _actionSyncMapWith_ValuePoint.setChecked(true);
            action_SyncWith_ValuePoint();
            break;

         case IsSyncWith_Photo:
            _actionSyncMapWith_Photo.setChecked(true);
            action_SyncWith_Photo();
            break;

         case IsSyncWith_NONE:
         default:

            // sync action is selected but no sync subaction -> uncheck map sync
            _actionMap2_SyncMap.setSelection(false);
            break;
         }

      } else {

         // sync action is not selected

         deactivateSyncWith_OtherMap();
         deactivateSyncWith_Photo();
         deactivateSyncWith_Slider();
         deactivateSyncWith_Tour();
         deactivateSyncWith_ValuePoint();
      }
   }

   /**
    * Set sync map action selected when one of it's subactions are selected.
    *
    * @param isSelectSyncMap
    */
   private void syncMap_ShowCurrentSyncModeImage(final boolean isSelectSyncMap) {

      switch (_currentMapSyncMode) {

      case IsSyncWith_Tour:
         _actionMap2_SyncMap.showOtherEnabledImage(0);
         break;

      case IsSyncWith_Slider_One:
         _actionMap2_SyncMap.showOtherEnabledImage(1);
         break;

      case IsSyncWith_Slider_Center:
         _actionMap2_SyncMap.showOtherEnabledImage(2);
         break;

      case IsSyncWith_ValuePoint:
         _actionMap2_SyncMap.showOtherEnabledImage(3);
         break;

      case IsSyncWith_OtherMap:
         _actionMap2_SyncMap.showOtherEnabledImage(4);
         break;

      case IsSyncWith_Photo:
         _actionMap2_SyncMap.showOtherEnabledImage(5);
         break;

      case IsSyncWith_NONE:
      default:
         _actionMap2_SyncMap.showDefaultEnabledImage();
         break;
      }

      _actionMap2_SyncMap.setSelection(isSelectSyncMap);
   }

   @Override
   public void syncMapWithOtherMap(final MapPosition mapPosition,
                                   final ViewPart viewPart,
                                   final int positionFlags) {

      if (!_isMapSyncWith_OtherMap) {

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

   private void updateFilteredPhotos() {

      runPhotoFilter();

      _map.disposeOverlayImageCache();
      _map.paint();
   }

   @Override
   public void updatePhotoFilter(final int filterRatingStars, final PhotoRatingStarOperator ratingStarOperatorsValues) {

      photoFilter_UpdateFromSlideout(filterRatingStars, ratingStarOperatorsValues);
   }

   void updateTourColorsInToolbar() {

      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      fillToolbar_TourColors(tbm);

      tbm.update(true);
   }

   private void updateUI_HoveredValuePoint() {

      // find tour data for the index
      TourData hoveredTourData = null;
      int hoveredSerieIndex = 0;

      if (_allTourData.size() == 1) {

         // one simple tour or a multiple tour is displayed

         hoveredSerieIndex = _currentValuePointIndex;
         hoveredTourData = _allTourData.get(0);

      } else {

         /**
          * Discovered issue:
          * <p>
          * When multiple tours are selected in the tourbook view, _allTourData contains multiple
          * tours. However when tour chart is selected and it contains multiple tours, then one
          * TourData with isMultipleTour is displayed in the map ->complicated
          */

         int adjustedValuePointIndex = _currentValuePointIndex;

         for (final TourData tourData : _allTourData) {

            final double[] latitudeSerie = tourData.latitudeSerie;
            if (latitudeSerie != null) {

               final int serieLength = latitudeSerie.length;

               if (adjustedValuePointIndex < serieLength) {

                  hoveredTourData = tourData;
                  hoveredSerieIndex = adjustedValuePointIndex;

                  break;

               } else {

                  adjustedValuePointIndex -= serieLength;
               }
            }
         }
      }

      // set the paint context  for the direct mapping painter
      _directMappingPainter.setPaintContext(

            _map,
            _isShowTour,
            hoveredTourData,

            _currentLeftSliderValueIndex,
            _currentRightSliderValueIndex,
            hoveredSerieIndex,

            _actionShowSliderInMap.isChecked(),
            _actionShowSliderInLegend.isChecked(),
            _actionShowValuePoint.isChecked(),

            _sliderPathPaintingData);

      _map.paint();

      if (_isMapSyncWith_ValuePoint) {
         positionMapTo_ValueIndex(hoveredTourData, hoveredSerieIndex);
      }
   }

   public void updateUI_Photos() {

      _map.disposeOverlayImageCache();

      _map.paint();
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

         _map.tourBreadcrumb().resetTours();

         paintTours_10_All();
      }
   }

}
