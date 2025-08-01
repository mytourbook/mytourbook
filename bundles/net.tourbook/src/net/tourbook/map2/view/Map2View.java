/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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

import com.jhlabs.image.CurveValues;

import de.byteholder.geoclipse.map.ActionManageOfflineImages;
import de.byteholder.geoclipse.map.CenterMapBy;
import de.byteholder.geoclipse.map.IMapContextMenuProvider;
import de.byteholder.geoclipse.map.Map2;
import de.byteholder.geoclipse.map.MapGridData;
import de.byteholder.geoclipse.map.MapLegend;
import de.byteholder.geoclipse.map.PaintedMapPoint;
import de.byteholder.geoclipse.map.event.MapGeoPositionEvent;
import de.byteholder.geoclipse.map.event.MapHoveredTourEvent;
import de.byteholder.geoclipse.map.event.MapPOIEvent;
import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;
import de.byteholder.gpx.PointOfInterest;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.tourbook.Images;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.HoveredValuePointData;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.PointLong;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorProviderConfig;
import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ActionToolbarSlideoutAdv;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.IPinned_Tooltip_Owner;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.tooltip.SlideoutLocation;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourLocation;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourPhoto;
import net.tourbook.data.TourReference;
import net.tourbook.data.TourWayPoint;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.map.Action_ExportMap_SubMenu;
import net.tourbook.map.IMapSyncListener;
import net.tourbook.map.IMapView;
import net.tourbook.map.MapColorProvider;
import net.tourbook.map.MapImageSize;
import net.tourbook.map.MapInfoManager;
import net.tourbook.map.MapManager;
import net.tourbook.map.MapUtils;
import net.tourbook.map.bookmark.ActionMapBookmarks;
import net.tourbook.map.bookmark.DialogGotoMapLocation;
import net.tourbook.map.bookmark.IMapBookmarkListener;
import net.tourbook.map.bookmark.IMapBookmarks;
import net.tourbook.map.bookmark.MapBookmark;
import net.tourbook.map.bookmark.MapBookmarkManager;
import net.tourbook.map.location.LocationType;
import net.tourbook.map.player.ModelPlayerManager;
import net.tourbook.map2.Messages;
import net.tourbook.map2.action.ActionCreateTourMarkerFromMap;
import net.tourbook.map2.action.ActionLookupCommonLocation;
import net.tourbook.map2.action.ActionManageMapProviders;
import net.tourbook.map2.action.ActionMap2Color;
import net.tourbook.map2.action.ActionMap2_MapProvider;
import net.tourbook.map2.action.ActionMap2_PhotoFilter;
import net.tourbook.map2.action.ActionReloadFailedMapImages;
import net.tourbook.map2.action.ActionSaveDefaultPosition;
import net.tourbook.map2.action.ActionSetDefaultPosition;
import net.tourbook.map2.action.ActionSetGeoPositionForGeoMarker;
import net.tourbook.map2.action.ActionSetGeoPositionForPhotoTours;
import net.tourbook.map2.action.ActionShowAllFilteredPhotos;
import net.tourbook.map2.action.ActionShowLegendInMap;
import net.tourbook.map2.action.ActionShowPOI;
import net.tourbook.map2.action.ActionShowScaleInMap;
import net.tourbook.map2.action.ActionShowSliderInLegend;
import net.tourbook.map2.action.ActionShowSliderInMap;
import net.tourbook.map2.action.ActionShowStartEndInMap;
import net.tourbook.map2.action.ActionShowTourInfoInMap;
import net.tourbook.map2.action.ActionShowTourWeatherInMap;
import net.tourbook.map2.action.ActionShowValuePoint;
import net.tourbook.map2.action.ActionSyncMapWith_OtherMap;
import net.tourbook.map2.action.ActionSyncMapWith_Photo;
import net.tourbook.map2.action.ActionSyncMapWith_Slider_Centered;
import net.tourbook.map2.action.ActionSyncMapWith_Slider_One;
import net.tourbook.map2.action.ActionSyncMapWith_Tour;
import net.tourbook.map2.action.ActionSyncMapWith_ValuePoint;
import net.tourbook.map2.action.ActionTourColor;
import net.tourbook.map2.action.ActionZoomCenterBy;
import net.tourbook.map2.action.ActionZoomIn;
import net.tourbook.map2.action.ActionZoomLevelAdjustment;
import net.tourbook.map2.action.ActionZoomOut;
import net.tourbook.map2.action.ActionZoomShowEntireMap;
import net.tourbook.map2.action.ActionZoomShowEntireTour;
import net.tourbook.map25.Map25FPSManager;
import net.tourbook.photo.IPhotoEventListener;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoActivator;
import net.tourbook.photo.PhotoAdjustments;
import net.tourbook.photo.PhotoEventId;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.PhotoRatingStarOperator;
import net.tourbook.photo.PhotoSelection;
import net.tourbook.photo.internal.preferences.PrefPagePhotoExternalApp;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.srtm.IPreferences;
import net.tourbook.tour.ActionOpenMarkerDialog;
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
import net.tourbook.tour.TourWeatherToolTipProvider;
import net.tourbook.tour.filter.geo.GeoFilter_LoaderData;
import net.tourbook.tour.filter.geo.TourGeoFilter;
import net.tourbook.tour.filter.geo.TourGeoFilter_Loader;
import net.tourbook.tour.filter.geo.TourGeoFilter_Manager;
import net.tourbook.tour.location.CommonLocationManager;
import net.tourbook.tour.location.TourLocationExtended;
import net.tourbook.tour.photo.IMapWithPhotos;
import net.tourbook.tour.photo.TourPhotoLink;
import net.tourbook.tour.photo.TourPhotoLinkSelection;
import net.tourbook.tour.photo.TourPhotoManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ValuePoint_ToolTip_UI;
import net.tourbook.ui.action.SubMenu_SetTourMarkerType;
import net.tourbook.ui.tourChart.HoveredValueData;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.geoCompare.GeoComparedTour;
import net.tourbook.ui.views.referenceTour.ReferenceTourManager;
import net.tourbook.ui.views.referenceTour.SelectionReferenceTourView;
import net.tourbook.ui.views.referenceTour.TVIElevationCompareResult_ComparedTour;
import net.tourbook.ui.views.referenceTour.TVIRefTour_ComparedTour;
import net.tourbook.ui.views.referenceTour.TVIRefTour_RefTourItem;
import net.tourbook.ui.views.tourSegmenter.SelectedTourSegmenterSegments;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.ViewPart;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;

/**
 * @author Wolfgang Schramm
 *
 * @since 1.3.0
 */
public class Map2View extends ViewPart implements

      IMapContextMenuProvider,
      IMapBookmarks,
      IMapBookmarkListener,
      IMapSyncListener,
      IMapWithPhotos,
      IMapView,
      IPhotoEventListener {

// SET_FORMATTING_OFF

   public static final String    ID                                                    = "net.tourbook.map2.view.Map2ViewId";                   //$NON-NLS-1$

   private static final String   EXTERNAL_APP_ACTION                                   = "&%d   %s";                                              //$NON-NLS-1$

   static final String           STATE_TRACK_OPTIONS_SELECTED_TAB                      = "STATE_TRACK_OPTIONS_SELECTED_TAB";                    //$NON-NLS-1$

   private static final String   STATE_IS_SHOW_LEGEND_IN_MAP                           = "STATE_IS_SHOW_LEGEND_IN_MAP";                         //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_MAP_POINTS                              = "STATE_IS_SHOW_MAP_POINTS";                            //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_PHOTO_IN_MAP                            = "STATE_IS_SHOW_PHOTO_IN_MAP";                          //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_TOUR_IN_MAP                             = "STATE_IS_SHOW_TOUR_IN_MAP";                           //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_SCALE_IN_MAP                            = "STATE_IS_SHOW_SCALE_IN_MAP";                          //$NON-NLS-1$
   static final String           STATE_IS_SHOW_SLIDER_IN_MAP                           = "STATE_IS_SHOW_SLIDER_IN_MAP";                         //$NON-NLS-1$
   static final boolean          STATE_IS_SHOW_SLIDER_IN_MAP_DEFAULT                   = true;
   private static final String   STATE_IS_SHOW_SLIDER_IN_LEGEND                        = "STATE_IS_SHOW_SLIDER_IN_LEGEND";                      //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_START_END_IN_MAP                        = "STATE_IS_SHOW_START_END_IN_MAP";                      //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_TOUR_INFO_IN_MAP                        = "STATE_IS_SHOW_TOUR_INFO_IN_MAP";                      //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_TOUR_WEATHER_IN_MAP                     = "STATE_IS_SHOW_TOUR_WEATHER_IN_MAP";                   //$NON-NLS-1$
   private static final String   STATE_IS_SHOW_VALUE_POINT                             = "STATE_IS_SHOW_VALUE_POINT";                           //$NON-NLS-1$
   private static final boolean  STATE_IS_SHOW_VALUE_POINT_DEFAULT                     = true;
   static final String           STATE_IS_TOGGLE_KEYBOARD_PANNING                      = "STATE_IS_TOGGLE_KEYBOARD_PANNING";                    //$NON-NLS-1$
   static final boolean          STATE_IS_TOGGLE_KEYBOARD_PANNING_DEFAULT              = true;

   // hovered/selected tour
   public static final String    STATE_IS_SHOW_HOVERED_SELECTED_TOUR                            = "STATE_IS_SHOW_HOVERED_SELECTED_TOUR";                 //$NON-NLS-1$
   public static final boolean   STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT                    = true;
   static final String           STATE_HOVERED_SELECTED__HOVERED_OPACITY                        = "STATE_HOVERED_SELECTED__HOVERED_OPACITY";             //$NON-NLS-1$
   static final int              STATE_HOVERED_SELECTED__HOVERED_OPACITY_DEFAULT                = 0x80;
   static final String           STATE_HOVERED_SELECTED__HOVERED_RGB                            = "STATE_HOVERED_SELECTED__HOVERED_RGB";                 //$NON-NLS-1$
   static final RGB              STATE_HOVERED_SELECTED__HOVERED_RGB_DEFAULT                    = new RGB(255, 255, 0);
   static final String           STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_OPACITY           = "STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_OPACITY";//$NON-NLS-1$
   static final int              STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_OPACITY_DEFAULT   = 0x80;
   static final String           STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_RGB               = "STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_RGB";    //$NON-NLS-1$
   static final RGB              STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_RGB_DEFAULT       = new RGB(0, 255, 255);
   static final String           STATE_HOVERED_SELECTED__SELECTED_OPACITY                       = "STATE_HOVERED_SELECTED__SELECTED_OPACITY";            //$NON-NLS-1$
   static final int              STATE_HOVERED_SELECTED__SELECTED_OPACITY_DEFAULT               = 0x80;
   static final String           STATE_HOVERED_SELECTED__SELECTED_RGB                           = "STATE_HOVERED_SELECTED__SELECTED_RGB";                //$NON-NLS-1$
   static final RGB              STATE_HOVERED_SELECTED__SELECTED_RGB_DEFAULT                   = new RGB(0, 255, 0);

   static final String           STATE_IS_SHOW_BREADCRUMBS                                      = "STATE_IS_SHOW_BREADCRUMBS";                  //$NON-NLS-1$
   public static final boolean   STATE_IS_SHOW_BREADCRUMBS_DEFAULT                              = true;
   static final String           STATE_VISIBLE_BREADCRUMBS                                      = "STATE_VISIBLE_BREADCRUMBS";                  //$NON-NLS-1$
   static final int              STATE_VISIBLE_BREADCRUMBS_DEFAULT                              = 5;

   static final String           STATE_IS_SHOW_TOUR_DIRECTION                          = "STATE_IS_SHOW_TOUR_DIRECTION";                        //$NON-NLS-1$
   static final boolean          STATE_IS_SHOW_TOUR_DIRECTION_DEFAULT                  = true;
   static final String           STATE_IS_SHOW_TOUR_DIRECTION_ALWAYS                   = "STATE_IS_SHOW_TOUR_DIRECTION_ALWAYS";                 //$NON-NLS-1$
   static final boolean          STATE_IS_SHOW_TOUR_DIRECTION_ALWAYS_DEFAULT           = false;
   static final String           STATE_TOUR_DIRECTION_LINE_WIDTH                       = "STATE_TOUR_DIRECTION_LINE_WIDTH";                     //$NON-NLS-1$
   static final int              STATE_TOUR_DIRECTION_LINE_WIDTH_DEFAULT               = 3;
   static final String           STATE_TOUR_DIRECTION_MARKER_GAP                       = "STATE_TOUR_DIRECTION_MARKER_GAP";                     //$NON-NLS-1$
   static final int              STATE_TOUR_DIRECTION_MARKER_GAP_DEFAULT               = 30;
   static final String           STATE_TOUR_DIRECTION_SYMBOL_SIZE                      = "STATE_TOUR_DIRECTION_SYMBOL_SIZE";                    //$NON-NLS-1$
   static final int              STATE_TOUR_DIRECTION_SYMBOL_SIZE_DEFAULT              = 10;
   static final String           STATE_TOUR_DIRECTION_RGB                              = "STATE_TOUR_DIRECTION_RGB";                            //$NON-NLS-1$
   static final RGB              STATE_TOUR_DIRECTION_RGB_DEFAULT                      = new RGB(55, 55, 55);

   static final String           PREF_DEBUG_MAP_SHOW_GEO_GRID                          = "PREF_DEBUG_MAP_SHOW_GEO_GRID";                        //$NON-NLS-1$
   static final String           PREF_SHOW_TILE_INFO                                   = "PREF_SHOW_TILE_INFO";                                 //$NON-NLS-1$
   static final String           PREF_SHOW_TILE_BORDER                                 = "PREF_SHOW_TILE_BORDER";                               //$NON-NLS-1$

   static final String           STATE_IS_SHOW_IN_TOOLBAR_ALTITUDE                     = "STATE_IS_SHOW_IN_TOOLBAR_ALTITUDE";                   //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_GRADIENT                     = "STATE_IS_SHOW_IN_TOOLBAR_GRADIENT";                   //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_PACE                         = "STATE_IS_SHOW_IN_TOOLBAR_PACE";                       //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_POWER                        = "STATE_IS_SHOW_IN_TOOLBAR_POWER";                      //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_PULSE                        = "STATE_IS_SHOW_IN_TOOLBAR_PULSE";                      //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_SPEED                        = "STATE_IS_SHOW_IN_TOOLBAR_SPEED";                      //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_HR_ZONE                      = "STATE_IS_SHOW_IN_TOOLBAR_HR_ZONE";                    //$NON-NLS-1$
   static final String           STATE_IS_SHOW_IN_TOOLBAR_RUN_DYN_STEP_LENGTH          = "STATE_IS_SHOW_IN_TOOLBAR_RUN_DYN_STEP_LENGTH";        //$NON-NLS-1$

   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_ALTITUDE_DEFAULT             = true;
   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_GRADIENT_DEFAULT             = false;
   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_PACE_DEFAULT                 = false;
   static final boolean          STATE_IS_SHOW_IN_TOOLBAR_POWER_DEFAULT                = false;
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
   static final int              STATE_SLIDER_PATH_OPACITY_DEFAULT                     = 150; // 60%;
   static final int              STATE_SLIDER_PATH_SEGMENTS_DEFAULT                    = 200;
   static final RGB              STATE_SLIDER_PATH_COLOR_DEFAULT                       = new RGB(0xff, 0xff, 0x80);

   private static final String   STATE_IS_PHOTO_FILTER_ACTIVE                          = "STATE_IS_PHOTO_FILTER_ACTIVE";                        //$NON-NLS-1$
   private static final String   STATE_PHOTO_FILTER_RATING_STARS                       = "STATE_PHOTO_FILTER_RATING_STARS";                     //$NON-NLS-1$
   private static final String   STATE_PHOTO_FILTER_RATING_STAR_OPERATOR               = "STATE_PHOTO_FILTER_RATING_STAR_OPERATOR";             //$NON-NLS-1$

   public static final int       MAX_DIM_STEPS                                         = 10;
   public static final String    STATE_IS_MAP_DIMMED                                   = "STATE_IS_MAP_DIMMED";                                 //$NON-NLS-1$
   public static final boolean   STATE_IS_MAP_DIMMED_DEFAULT                           = false;
   public static final String    STATE_DIM_MAP_COLOR                                   = "STATE_DIM_MAP_COLOR";                                 //$NON-NLS-1$
   public static final RGB       STATE_DIM_MAP_COLOR_DEFAULT                           = new RGB(0x2f, 0x2f, 0x2f);
   public static final String    STATE_DIM_MAP_VALUE                                   = "STATE_DIM_MAP_VALUE";                                 //$NON-NLS-1$
   public static final int       STATE_DIM_MAP_VALUE_DEFAULT                           = MAX_DIM_STEPS / 2;


   private static final String      GRAPH_CONTRIBUTION_ID_SLIDEOUT                     = "GRAPH_CONTRIBUTION_ID_SLIDEOUT";                      //$NON-NLS-1$

   private static final String      STATE_CENTER_MAP_BY                                = "STATE_CENTER_MAP_BY";                                 //$NON-NLS-1$
   private static final CenterMapBy STATE_CENTER_MAP_BY_DEFAULT                        = CenterMapBy.Mouse;
   private static final String      STATE_MAP_SYNC_MODE                                = "STATE_MAP_SYNC_MODE";                                 //$NON-NLS-1$
   private static final String      STATE_MAP_SYNC_MODE_IS_ACTIVE                      = "STATE_MAP_SYNC_MODE_IS_ACTIVE";                       //$NON-NLS-1$
   public static final String       STATE_MAP_TRANSPARENCY_COLOR                       = "STATE_MAP_TRANSPARENCY_COLOR";                                 //$NON-NLS-1$
   public static final RGB          STATE_MAP_TRANSPARENCY_COLOR_DEFAULT               = new RGB(0xfe, 0xfe, 0xfe);
   public static final String       STATE_MAP_TRANSPARENCY_USE_MAP_DIM_COLOR           = "STATE_MAP_TRANSPARENCY_USE_MAP_DIM_COLOR";                                 //$NON-NLS-1$
   public static final boolean      STATE_MAP_TRANSPARENCY_USE_MAP_DIM_COLOR_DEFAULT   = true;

   private static final String      STATE_ZOOM_LEVEL_ADJUSTMENT                        = "STATE_ZOOM_LEVEL_ADJUSTMENT";                         //$NON-NLS-1$
   private static final String      STATE_SELECTED_MAP_PROVIDER_ID                     = "selected.map-provider-id";                            //$NON-NLS-1$

   private static final String      STATE_DEFAULT_POSITION_ZOOM                        = "STATE_DEFAULT_POSITION_ZOOM";                         //$NON-NLS-1$
   private static final String      STATE_DEFAULT_POSITION_LATITUDE                    = "STATE_DEFAULT_POSITION_LATITUDE";                     //$NON-NLS-1$
   private static final String      STATE_DEFAULT_POSITION_LONGITUDE                   = "STATE_DEFAULT_POSITION_LONGITUDE";                    //$NON-NLS-1$
   private static final String      STATE_TOUR_COLOR_ID                                = "STATE_TOUR_COLOR_ID";                                 //$NON-NLS-1$

   private static final MapGraphId[]   _allGraphContribId = {

         MapGraphId.Altitude,
         MapGraphId.Gradient,
         MapGraphId.Pace,
         MapGraphId.Power,
         MapGraphId.Pulse,
         MapGraphId.Speed,

         MapGraphId.RunDyn_StepLength,

         MapGraphId.HrZone,
   };

// SET_FORMATTING_ON
   //
   private static final IPreferenceStore _prefStore             = TourbookPlugin.getPrefStore();
   private static final IPreferenceStore _prefStore_Common      = CommonActivator.getPrefStore();
   private static final IPreferenceStore _prefStore_Photo       = PhotoActivator.getPrefStore();
   private static final IDialogSettings  _state                 = TourbookPlugin.getState(ID);
   private static final IDialogSettings  _state_MapLocation     = TourbookPlugin.getState("net.tourbook.map2.view.Map2View.MapLocation");  //$NON-NLS-1$
   private static final IDialogSettings  _state_MapProvider     = TourbookPlugin.getState("net.tourbook.map2.view.Map2View.MapProvider");  //$NON-NLS-1$
   private static final IDialogSettings  _state_PhotoFilter     = TourbookPlugin.getState("net.tourbook.map2.view.Map2View.PhotoFilter");  //$NON-NLS-1$
   private static final IDialogSettings  _state_PhotoOptions    = TourbookPlugin.getState("net.tourbook.map2.view.Map2View.PhotoOptions"); //$NON-NLS-1$
   //
   public static final int               TOUR_INFO_TOOLTIP_X    = 3;
   public static final int               TOUR_INFO_TOOLTIP_Y    = 23;
   public static final int               TOUR_WEATHER_TOOLTIP_X = 28;
   public static final int               TOUR_WEATHER_TOOLTIP_Y = 23;
   //
   //
   private final TourInfoIconToolTipProvider _tourInfoToolTipProvider    = new TourInfoIconToolTipProvider(TOUR_INFO_TOOLTIP_X, TOUR_INFO_TOOLTIP_Y);
   private ValuePoint_ToolTip_UI             _valuePointTooltipUI;
   private final TourWeatherToolTipProvider  _tourWeatherToolTipProvider = new TourWeatherToolTipProvider(
         TOUR_WEATHER_TOOLTIP_X,
         TOUR_WEATHER_TOOLTIP_Y);
   //
   private DirectMappingPainter              _directMappingPainter;
   //
   private final MapInfoManager              _mapInfoManager             = MapInfoManager.getInstance();
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
   private final ArrayList<TourData>         _allTourData                = new ArrayList<>();
   private TourData                          _lastTourWithoutLatLon;
   private TourData                          _previousTourData;
   private Long                              _lastSelectedTourInsideMap;
   //
   /**
    * Contains photos which are displayed in the map
    */
   private final ArrayList<Photo>            _allPhotos                  = new ArrayList<>();
   private final ArrayList<Photo>            _filteredPhotos             = new ArrayList<>();
   //
   private boolean                           _isPhotoFilterActive;
   private int                               _photoFilter_RatingStars;
   private Enum<PhotoRatingStarOperator>     _photoFilter_RatingStar_Operator;
   //
   private SlideoutMap2_PhotoOptions         _slideoutPhotoOptions;
   private PhotoAdjustments                  _copyPaste_PhotoAdjustments;
   private CopyPasteTonalityTransfer         _copyPaste_Transfer         = new CopyPasteTonalityTransfer();
   //
   private boolean                           _isInSelectBookmark;
   private boolean                           _isShowLegend;
   private boolean                           _isShowMapPoints;
   private boolean                           _isShowPhoto;
   private boolean                           _isShowTour;
   //
   private int                               _defaultZoom;
   private GeoPosition                       _defaultPosition;
   //
   /**
    * When <code>true</code> then a tour is painted, <code>false</code> a point of interest is
    * painted
    */
   private boolean                           _isTourPainted;
   //
   /*
    * Tool tips
    */
   private TourToolTip _tourToolTip;
   //
   private String      _poiName;
   private GeoPosition _poiPosition;
   private int         _poiZoomLevel;
   //
   /*
    * Current position for the x-sliders and value point
    */
   private int                                     _currentSliderValueIndex_Left;
   private int                                     _currentSliderValueIndex_Right;
   private int                                     _currentSliderValueIndex_Selected;
   private int                                     _externalValuePointIndex;
   //
   private MapLegend                               _mapLegend;
   //
   private long                                    _previousOverlayKey;
   //
   private int                                     _selectedProfileKey   = 0;
   //
   private MapGraphId                              _tourColorId;
   //
   private int                                     _hash_AllTourIds;
   private int                                     _hash_AllTourData;
   private long                                    _hash_TourOverlayKey;
   private int                                     _hash_AllPhotos;
   //
   private final AtomicInteger                     _asyncCounter         = new AtomicInteger();
   //
   /**
    * Is <code>true</code> when a link photo is displayed, otherwise a tour photo (photo which is
    * save in a tour) is displayed.
    */
   private boolean                                 _isLinkPhotoDisplayed;
   //
   private SliderPathPaintingData                  _sliderPathPaintingData;
   private OpenDialogManager                       _openDlgMgr           = new OpenDialogManager();
   //
   /**
    * Keep map sync mode when map sync action get's unchecked
    */
   private MapSyncMode                             _currentMapSyncMode   = MapSyncMode.IsSyncWith_Tour;
   private boolean                                 _isMapSyncActive;
   private boolean                                 _isInMapSync;
   private long                                    _lastFiredMapSyncEventTime;
   //
   private boolean                                 _isMapSyncWith_MapLocation;
   private boolean                                 _isMapSyncWith_OtherMap;
   private boolean                                 _isMapSyncWith_Photo;
   private boolean                                 _isMapSyncWith_Slider_Centered;
   private boolean                                 _isMapSyncWith_Slider_One;
   private boolean                                 _isMapSyncWith_Tour;
   private boolean                                 _isMapSyncWith_ValuePoint;
   //
   private EnumMap<MapGraphId, Action>             _allTourColor_Actions = new EnumMap<>(MapGraphId.class);
   private ActionTourColor                         _actionTourColor_Elevation;
   private ActionTourColor                         _actionTourColor_Gradient;
   private ActionTourColor                         _actionTourColor_Power;
   private ActionTourColor                         _actionTourColor_Pulse;
   private ActionTourColor                         _actionTourColor_Speed;
   private ActionTourColor                         _actionTourColor_Pace;
   private ActionTourColor                         _actionTourColor_HrZone;
   private ActionTourColor                         _actionTourColor_RunDyn_StepLength;
   //
   private ActionCopyLocation                      _actionCopyLocation;
   private ActionCreateTourMarkerFromMap           _actionCreateTourMarkerFromMap;
   private Action_ExportMap_SubMenu                _actionExportMap_SubMenu;
   private ActionGotoLocation                      _actionGotoLocation;
   private ActionLookupCommonLocation              _actionLookupTourLocation;
   private ActionManageMapProviders                _actionManageMapProvider;
   private ActionMapBookmarks                      _actionMap2Slideout_Bookmarks;
   private ActionMap2Color                         _actionMap2Slideout_Color;
   private ActionMap2_MapPoints                    _actionMap2Slideout_MapPoints;
   private ActionMap2_MapProvider                  _actionMap2Slideout_MapProvider;
   private ActionMap2_Options                      _actionMap2Slideout_Options;
   private ActionMap2_PhotoFilter                  _actionMap2Slideout_PhotoFilter;
   private ActionMap2_PhotoOptions                 _actionMap2Slideout_PhotoOptions;
   private ActionMap2_Graphs                       _actionMap2Slideout_TourColors;
   private ActionMapPoint_CenterMap                _actionMapPoint_CenterMap;
   private ActionMapPoint_EditTourMarker           _actionMapPoint_EditTourMarker;
   private ActionMapPoint_Photo_AutoSelect         _actionMapPoint_Photo_AutoSelect;
   private ActionMapPoint_Photo_Deselect           _actionMapPoint_Photo_Deselect;
   private ActionMapPoint_Photo_EditLabel          _actionMapPoint_Photo_EditLabel;
   private ActionMapPoint_Photo_RemoveFromTour     _actionMapPoint_Photo_RemoveFromTour;
   private ActionMapPoint_Photo_ReplaceGeoPosition _actionMapPoint_Photo_ReplaceGeoPosition;
   private ActionMapPoint_Photo_ShowAnnotations    _actionMapPoint_Photo_ShowAnnotations;
   private ActionMapPoint_Photo_ShowHistogram      _actionMapPoint_Photo_ShowHistogram;
   private ActionMapPoint_Photo_ShowLabel          _actionMapPoint_Photo_ShowLabel;
   private ActionMapPoint_Photo_ShowRating         _actionMapPoint_Photo_ShowRating;
   private ActionMapPoint_Photo_ShowTooltip        _actionMapPoint_Photo_ShowTooltip;
   private ActionMapPoint_Photo_Tonality_Copy      _actionMapPoint_Photo_Tonality_Copy;
   private ActionMapPoint_Photo_Tonality_Paste     _actionMapPoint_Photo_Tonality_Paste;
   private ActionMapPoint_ShowOnlyThisTour         _actionMapPoint_ShowOnlyThisTour;
   private ActionMapPoint_ZoomIn                   _actionMapPoint_ZoomIn;
   private ActionReloadFailedMapImages             _actionReloadFailedMapImages;
   private ActionRunExternalApp                    _actionRunExternalApp1;
   private ActionRunExternalApp                    _actionRunExternalApp2;
   private ActionRunExternalApp                    _actionRunExternalApp3;
   private ActionRunExternalAppPrefPage            _actionRunExternalAppPrefPage;
   private ActionRunExternalAppTitle               _actionRunExternalAppTitle;
   private ActionSaveDefaultPosition               _actionSaveDefaultPosition;
   private ActionSearchTourByLocation              _actionSearchTourByLocation;
   private ActionSetDefaultPosition                _actionSetDefaultPosition;
   private ActionSetGeoPositionForGeoMarker        _actionSetGeoPositionForGeoMarker;
   private ActionSetGeoPositionForPhotoTours       _actionSetGeoPositionForPhotoTours;
   private ActionShowAllFilteredPhotos             _actionShowAllFilteredPhotos;
   private ActionShowLegendInMap                   _actionShowLegendInMap;
   private ActionShowPOI                           _actionShowPOI;
   private ActionShowScaleInMap                    _actionShowScaleInMap;
   private ActionShowSliderInMap                   _actionShowSliderInMap;
   private ActionShowSliderInLegend                _actionShowSliderInLegend;
   private ActionShowStartEndInMap                 _actionShowStartEndInMap;
   private ActionShowTour                          _actionShowTour;
   private ActionShowTourInfoInMap                 _actionShowTourInfoInMap;
   private ActionShowTourWeatherInMap              _actionShowTourWeatherInMap;
   private ActionShowValuePoint                    _actionShowValuePoint;
   private ActionZoomLevelAdjustment               _actionZoomLevelAdjustment;
   //
   private SubMenu_SetTourMarkerType               _actionSubMenu_SetTourMarkerType;
   //
   private ActionSyncMap                           _actionMap2Slideout_SyncMap;
   private ActionSyncMapWith_Photo                 _actionSyncMapWith_Photo;
   private ActionSyncMapWith_Slider_One            _actionSyncMapWith_Slider_One;
   private ActionSyncMapWith_Slider_Centered       _actionSyncMapWith_Slider_Centered;
   private ActionSyncMapWith_OtherMap              _actionSyncMapWith_OtherMap;
   private ActionSyncMapWith_Tour                  _actionSyncMapWith_Tour;
   private ActionSyncMapWith_TourLocation          _actionSyncMapWith_TourLocation;
   private ActionSyncMapWith_ValuePoint            _actionSyncMapWith_ValuePoint;
   private EnumMap<MapSyncId, Action>              _allSyncMapActions    = new EnumMap<>(MapSyncId.class);
   //
   private ActionZoomIn                            _actionZoom_In;
   private ActionZoomOut                           _actionZoom_Out;
   private ActionZoomCenterBy                      _actionZoom_CenterMapBy;
   private ActionZoomShowEntireMap                 _actionZoom_ShowEntireMap;
   private ActionZoomShowEntireTour                _actionZoom_ShowEntireTour;
   //
   private org.eclipse.swt.graphics.Point          _geoFilter_Loaded_TopLeft_E2;
   private org.eclipse.swt.graphics.Point          _geoFilter_Loaded_BottomRight_E2;
   private GeoFilter_LoaderData                    _geoFilter_PreviousGeoLoaderItem;
   private AtomicInteger                           _geoFilter_RunningId  = new AtomicInteger();
   //
   private PaintedMapPoint                         _contextMenu_HoveredMapPoint;
   //
   /*
    * UI controls
    */
   private Composite _parent;
   private Map2      _map;

   private class ActionCopyLocation extends Action {

      public ActionCopyLocation() {

         setText(Messages.Map_Action_CopyLocation);

         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Copy));
      }

      @Override
      public void run() {
         actionCopyLocationToClipboard();
      }
   }

   private class ActionGotoLocation extends Action {

      public ActionGotoLocation() {

         setText(Messages.Map_Action_GotoLocation);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Show_POI));
      }

      @Override
      public void run() {
         actionGotoLocation();
      }
   }

   private class ActionMap2_Graphs extends ActionToolbarSlideout {

      public ActionMap2_Graphs() {

         super(TourbookPlugin.getImageDescriptor(Images.Graph),
               null);

         setId(GRAPH_CONTRIBUTION_ID_SLIDEOUT);
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutMap2_TourColors(_parent, toolbar, Map2View.this, _state);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }
   }

   private class ActionMap2_MapPoints extends ActionToolbarSlideoutAdv {

      private static final ImageDescriptor _actionImageDescriptor = TourbookPlugin.getThemedImageDescriptor(Images.MapLocation_MapPoint);

      public ActionMap2_MapPoints() {

         super(_actionImageDescriptor, _actionImageDescriptor);

         isToggleAction = true;
         notSelectedTooltip = Messages.Map_Action_ShowMapPoints_Tooltip;
      }

      @Override
      protected AdvancedSlideout createSlideout(final ToolItem toolItem) {

         final SlideoutMap2_MapPoints slideoutMapPoint = new SlideoutMap2_MapPoints(toolItem, _state, _state_MapLocation, Map2View.this);
         slideoutMapPoint.setSlideoutLocation(SlideoutLocation.BELOW_RIGHT);

         return slideoutMapPoint;
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }

      @Override
      protected void onSelect(final SelectionEvent selectionEvent) {

         // show/hide slideout
         super.onSelect(selectionEvent);

         _isShowMapPoints = getSelection();

         _map.setShowMapPoint(_isShowMapPoints);
      }
   }

   private class ActionMap2_Options extends ActionToolbarSlideout {

      public ActionMap2_Options() {

         super(TourbookPlugin.getThemedImageDescriptor(Images.MapOptions), null);
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutMap2_Options(_parent, toolbar, Map2View.this, _state);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }
   }

   private class ActionMap2_PhotoOptions extends ActionToolbarSlideoutAdv {

      public ActionMap2_PhotoOptions() {

         super(TourbookPlugin.getThemedImageDescriptor(Images.ShowPhotos_InMap));

         isToggleAction = true;
         notSelectedTooltip = Messages.Map_Action_ShowPhotos_Tooltip;
      }

      @Override
      protected AdvancedSlideout createSlideout(final ToolItem toolItem) {

         _slideoutPhotoOptions = new SlideoutMap2_PhotoOptions(toolItem, _state, _state_PhotoOptions, Map2View.this);
         _slideoutPhotoOptions.setSlideoutLocation(SlideoutLocation.BELOW_RIGHT);

         return _slideoutPhotoOptions;
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }

      @Override
      protected void onSelect(final SelectionEvent selectionEvent) {

         // show/hide slideout
         super.onSelect(selectionEvent);

         actionShowPhotos(getSelection());
      }
   }

   private class ActionMapPoint_CenterMap extends Action {

      public ActionMapPoint_CenterMap() {

         setText(Messages.Map_Action_CenterMapToMapPointPosition);
      }

      @Override
      public void run() {

         actionMapMarker_CenterMap();
      }
   }

   private class ActionMapPoint_EditTourMarker extends Action {

      public ActionMapPoint_EditTourMarker() {

         setText(Messages.Map_Action_EditTourMarker);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_Edit));
      }

      @Override
      public void run() {

         actionMapMarker_Edit();
      }
   }

   private class ActionMapPoint_Photo_AutoSelect extends Action {

      public ActionMapPoint_Photo_AutoSelect() {

         super(Messages.Map_Action_AutoSelectPhoto, Action.AS_CHECK_BOX);
      }

      @Override
      public void run() {

         actionPhoto_AutoSelect();
      }
   }

   private class ActionMapPoint_Photo_Deselect extends Action {

      public ActionMapPoint_Photo_Deselect() {

         super(Messages.Map_Action_DeselectPhoto, Action.AS_PUSH_BUTTON);

         setToolTipText(Messages.Map_Action_DeselectPhoto_Tooltip);
      }

      @Override
      public void run() {

         _map.selectPhoto(null, null);
      }
   }

   private class ActionMapPoint_Photo_EditLabel extends Action {

      public ActionMapPoint_Photo_EditLabel() {

         super(Messages.Map_Action_EditPhotoLabel, Action.AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_Edit));
      }

      @Override
      public void run() {

         actionPhoto_EditPhotoLabel();
      }
   }

   private class ActionMapPoint_Photo_RemoveFromTour extends Action {

      public ActionMapPoint_Photo_RemoveFromTour() {

         super(OtherMessages.ACTION_PHOTOS_AND_TOURS_REMOVE_PHOTO, Action.AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
      }

      @Override
      public void run() {

         TourManager.tourPhoto_Remove(_contextMenu_HoveredMapPoint);
      }
   }

   private class ActionMapPoint_Photo_ReplaceGeoPosition extends Action {

      public ActionMapPoint_Photo_ReplaceGeoPosition() {

         super(Messages.Map_Action_ReplacePhotoGeoPosition, Action.AS_PUSH_BUTTON);

         setToolTipText(Messages.Map_Action_ReplacePhotoGeoPosition_Tooltip);
      }

      @Override
      public void run() {

         actionPhoto_ResetGeoPosition();
      }
   }

   private class ActionMapPoint_Photo_ShowAnnotations extends Action {

      public ActionMapPoint_Photo_ShowAnnotations() {

         super(Messages.Map_Action_ShowPhotoAnnotations, Action.AS_CHECK_BOX);
      }

      @Override
      public void run() {

         actionPhoto_ShowAnnotations();
      }
   }

   private class ActionMapPoint_Photo_ShowHistogram extends Action {

      public ActionMapPoint_Photo_ShowHistogram() {

         super(Messages.Map_Action_ShowPhotoHistogram, Action.AS_CHECK_BOX);
      }

      @Override
      public void run() {

         actionPhoto_ShowHistogram();
      }
   }

   private class ActionMapPoint_Photo_ShowLabel extends Action {

      public ActionMapPoint_Photo_ShowLabel() {

         super(Messages.Map_Action_ShowPhotoLabel, Action.AS_CHECK_BOX);
      }

      @Override
      public void run() {

         actionPhoto_ShowLabel();
      }
   }

   private class ActionMapPoint_Photo_ShowRating extends Action {

      public ActionMapPoint_Photo_ShowRating() {

         super(Messages.Map_Action_ShowPhotoRating, Action.AS_CHECK_BOX);
      }

      @Override
      public void run() {

         actionPhoto_ShowRating();
      }

   }

   private class ActionMapPoint_Photo_ShowTooltip extends Action {

      public ActionMapPoint_Photo_ShowTooltip() {

         super(Messages.Map_Action_ShowPhotoImage, Action.AS_CHECK_BOX);
      }

      @Override
      public void run() {

         actionPhoto_ShowTooltip();
      }
   }

   private class ActionMapPoint_Photo_Tonality_Copy extends Action {

      public ActionMapPoint_Photo_Tonality_Copy() {

         super(Messages.Map_Action_CopyTonality, Action.AS_PUSH_BUTTON);

         setToolTipText(Messages.Map_Action_CopyTonality_Tooltip);
         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Copy));
      }

      @Override
      public void run() {

         actionPhoto_Tonality_Copy();
      }
   }

   private class ActionMapPoint_Photo_Tonality_Paste extends Action {

      public ActionMapPoint_Photo_Tonality_Paste() {

         super(Messages.Map_Action_PasteTonality, Action.AS_PUSH_BUTTON);

         setToolTipText(Messages.Map_Action_PasteTonality_Tooltip);
         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Paste));
      }

      @Override
      public void run() {

         actionPhoto_Tonality_Paste();
      }
   }

   private class ActionMapPoint_ShowOnlyThisTour extends Action {

      public ActionMapPoint_ShowOnlyThisTour() {

         setText(Messages.Map_Action_ShowOnlyThisTour);
      }

      @Override
      public void run() {
         actionMapMarker_ShowOnlyThisTour();
      }
   }

   private class ActionMapPoint_ZoomIn extends Action {

      public ActionMapPoint_ZoomIn() {

         setText(Messages.Map_Action_ZoomInToTheMapPointPosition);
      }

      @Override
      public void run() {
         actionMapMarker_ZoomIn();
      }
   }

   public class ActionRunExternalApp extends Action {

      public ActionRunExternalApp() {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {

         actionExternalApp_Run(this, null);
      }
   }

   public class ActionRunExternalAppPrefPage extends Action {

      public ActionRunExternalAppPrefPage() {

         super(Messages.Map_Action_ExternalApp_Setup, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {

         actionExternalApp_PrefPage();
      }
   }

   private class ActionRunExternalAppTitle extends Action {

      public ActionRunExternalAppTitle() {

         super(Messages.Map_Action_ExternalApp_OpenPhotoImage, AS_PUSH_BUTTON);

         setEnabled(false);
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

   private class ActionShowTour extends ActionToolbarSlideout {

      public ActionShowTour() {

         super(TourbookPlugin.getThemedImageDescriptor(Images.TourChart), null);

         isToggleAction = true;
         notSelectedTooltip = Messages.map_action_show_tour_in_map;
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {
         return new SlideoutMap2_TrackOptions(_parent, toolbar, Map2View.this, _state);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }

      @Override
      protected void onSelect() {

         super.onSelect();

         _isShowTour = getSelection();
         _map.setIsShowTour(_isShowTour);

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
          * Register action images
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

         // image 6: tour location
         addOtherEnabledImage(TourbookPlugin.getThemedImageDescriptor(Images.SyncWith_TourLocation));
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutMap2_SyncMap(_parent, toolbar, Map2View.this);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }

      @Override
      protected void onSelect() {

         super.onSelect();

         _isMapSyncActive = getSelection();

         syncMap_OnSelectSyncAction();
      }
   }

   public class ActionSyncMapWith_TourLocation extends Action {

      public ActionSyncMapWith_TourLocation() {

         super(null, AS_CHECK_BOX);

         setToolTipText(Messages.Map_Action_SynchWith_TourLocations);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.SyncWith_TourLocation));
      }

      @Override
      public void run() {
         action_SyncWith_TourLocation();
      }
   }

   private class CopyPasteCurveValues {

      float[] allCurveValuesX;
      float[] allCurveValuesY;
   }

   private class CopyPasteTonalityTransfer extends ByteArrayTransfer {

      private static final String TYPE_NAME = "net.tourbook.map2.view.Map2View.CopyPasteTonalityTransfer"; //$NON-NLS-1$
      private final int           TYPE_ID   = registerType(TYPE_NAME);

      private CopyPasteTonalityTransfer() {}

      @Override
      protected int[] getTypeIds() {
         return new int[] { TYPE_ID };
      }

      @Override
      protected String[] getTypeNames() {
         return new String[] { TYPE_NAME };
      }

      @Override
      protected void javaToNative(final Object data, final TransferData transferData) {

         try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
               final DataOutputStream dataOut = new DataOutputStream(out)) {

            if (_copyPaste_PhotoAdjustments != null) {

               final float[] allCurveValuesX = _copyPaste_PhotoAdjustments.curveValuesX;
               final float[] allCurveValuesY = _copyPaste_PhotoAdjustments.curveValuesY;

               // write number of values
               dataOut.writeInt(allCurveValuesX.length);

               // write all values
               for (int valueIndex = 0; valueIndex < allCurveValuesX.length; valueIndex++) {

                  dataOut.writeFloat(allCurveValuesX[valueIndex]);
                  dataOut.writeFloat(allCurveValuesY[valueIndex]);
               }
            }

            super.javaToNative(out.toByteArray(), transferData);

         } catch (final IOException e) {

            StatusUtil.log(e);
         }
      }

      @Override
      protected Object nativeToJava(final TransferData transferData) {

         final byte[] bytes = (byte[]) super.nativeToJava(transferData);

         try (final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
               final DataInputStream dataIn = new DataInputStream(in)) {

            // read number of values
            final int numValues = dataIn.readInt();

            final float[] allCurveValuesX = new float[numValues];
            final float[] allCurveValuesY = new float[numValues];

            for (int valueIndex = 0; valueIndex < numValues; valueIndex++) {

               allCurveValuesX[valueIndex] = dataIn.readFloat();
               allCurveValuesY[valueIndex] = dataIn.readFloat();
            }

            /*
             * Return clipboard values
             */
            final CopyPasteCurveValues copyPasteCurveValues = new CopyPasteCurveValues();

            copyPasteCurveValues.allCurveValuesX = allCurveValuesX;
            copyPasteCurveValues.allCurveValuesY = allCurveValuesY;

            return copyPasteCurveValues;

         } catch (final IOException e) {

            StatusUtil.log(e);
         }

         return null;
      }
   }

   private static class DialogEditPhotoLabel extends InputDialog {

      public DialogEditPhotoLabel(final Shell parentShell,
                                  final String dialogTitle,
                                  final String dialogMessage,
                                  final String initialValue,
                                  final IInputValidator validator) {

         super(parentShell, dialogTitle, dialogMessage, initialValue, validator);
      }

      @Override
      protected void createButtonsForButtonBar(final Composite parent) {

         super.createButtonsForButtonBar(parent);

         // set text for the OK button
         final Button okButton = getButton(IDialogConstants.OK_ID);
         okButton.setText(OtherMessages.APP_ACTION_SAVE);
      }

      @Override
      protected org.eclipse.swt.graphics.Point getInitialLocation(final org.eclipse.swt.graphics.Point initialSize) {

         try {

            final org.eclipse.swt.graphics.Point cursorLocation = Display.getCurrent().getCursorLocation();

            // center below cursor location
            cursorLocation.x -= initialSize.x / 2;
            cursorLocation.y += 10;

            return cursorLocation;

         } catch (final NumberFormatException ex) {

            return super.getInitialLocation(initialSize);
         }
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

      /**
       * Image: 6
       */
      IsSyncWith_TourLocation,
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

         deactivateOtherSync(_actionSyncMapWith_Slider_One);

         // map must also be synched with selected tour
         _isMapSyncWith_Tour = true;

         _map.setShowOverlays(true);

         final TourData firstTourData = _allTourData.get(0);

         positionMapTo_0_TourSliders(
               firstTourData,
               _currentSliderValueIndex_Left,
               _currentSliderValueIndex_Right,
               _currentSliderValueIndex_Selected,
               null);
      }

      syncMap_UpdateSyncSlideoutAction(_isMapSyncWith_Slider_One);
   }

   public void action_SyncWith_OtherMap(final boolean isSelected) {

      _isMapSyncWith_OtherMap = isSelected;
      _currentMapSyncMode = MapSyncMode.IsSyncWith_OtherMap;

      if (_isMapSyncWith_OtherMap) {

         deactivateOtherSync(_actionSyncMapWith_OtherMap);
      }

      syncMap_UpdateSyncSlideoutAction(_isMapSyncWith_OtherMap);
   }

   /**
    * Sync map with photo
    */
   public void action_SyncWith_Photo() {

      _isMapSyncWith_Photo = _actionSyncMapWith_Photo.isChecked();
      _currentMapSyncMode = MapSyncMode.IsSyncWith_Photo;

      if (_isMapSyncWith_Photo) {

         deactivateOtherSync(_actionSyncMapWith_Photo);

         centerPhotos(_filteredPhotos, false);

         _map.paint();
      }

      syncMap_UpdateSyncSlideoutAction(_isMapSyncWith_Photo);

      enableActions(true);
   }

   public void action_SyncWith_Tour() {

      _isMapSyncWith_Tour = _actionSyncMapWith_Tour.isChecked();
      _currentMapSyncMode = MapSyncMode.IsSyncWith_Tour;

      if (_allTourData.isEmpty()) {
         return;
      }

      if (_isMapSyncWith_Tour) {

         deactivateOtherSync(_actionSyncMapWith_Tour);

         // force tour to be repainted, that it is synched immediately
         _previousTourData = null;

         _actionShowTour.setSelection(true);
         _map.setShowOverlays(true);

         paintTours_20_One(_allTourData.get(0), true);
      }

      syncMap_UpdateSyncSlideoutAction(_isMapSyncWith_Tour);
   }

   public void action_SyncWith_TourLocation() {

      _isMapSyncWith_MapLocation = _actionSyncMapWith_TourLocation.isChecked();
      _currentMapSyncMode = MapSyncMode.IsSyncWith_TourLocation;

      if (_isMapSyncWith_MapLocation) {

         deactivateOtherSync(_actionSyncMapWith_TourLocation);
      }

      syncMap_UpdateSyncSlideoutAction(_isMapSyncWith_MapLocation);
   }

   public void action_SyncWith_ValuePoint() {

      _isMapSyncWith_ValuePoint = _actionSyncMapWith_ValuePoint.isChecked();
      _currentMapSyncMode = MapSyncMode.IsSyncWith_ValuePoint;

      if (_allTourData.isEmpty()) {
         return;
      }

      if (_isMapSyncWith_ValuePoint) {

         deactivateOtherSync(_actionSyncMapWith_ValuePoint);

         // map must also be synched with selected tour
         _isMapSyncWith_Tour = true;

         _map.setShowOverlays(true);

         positionMapTo_0_TourSliders(
               _allTourData.get(0), // sync with first tour
               _currentSliderValueIndex_Left,
               _currentSliderValueIndex_Right,
               _currentSliderValueIndex_Selected,
               null);
      }

      syncMap_UpdateSyncSlideoutAction(_isMapSyncWith_ValuePoint);
   }

   private void actionCopyLocationToClipboard() {

      final GeoPosition mouseDown_GeoPosition = _map.getMouseDown_GeoPosition();

      final String geoPosition = String.format(Messages.Clipboard_Content_MapLocation,
            mouseDown_GeoPosition.latitude,
            mouseDown_GeoPosition.longitude);

      final String statusMessage = String.format(Messages.StatusLine_Message_CopiedLatitudeLongitude,
            mouseDown_GeoPosition.latitude,
            mouseDown_GeoPosition.longitude);

      UI.copyTextIntoClipboard(geoPosition, statusMessage);
   }

   private void actionExternalApp_PrefPage() {

      PreferencesUtil.createPreferenceDialogOn(
            Display.getCurrent().getActiveShell(),
            PrefPagePhotoExternalApp.ID,
            null,
            null)

            .open();
   }

   private void actionExternalApp_Run(final ActionRunExternalApp actionRunExternalApp, Photo photo) {

      /*
       * Hide all opened slideouts
       */
      _slideoutPhotoOptions.close();
      _map.photoHistogram_Close();
      _map.photoTooltip_Close();

      final SlideoutMap2_MapPoints mapPointSlideout = Map2PointManager.getMapPointSlideout(false);
      if (mapPointSlideout != null) {
         mapPointSlideout.close();
      }

      /*
       * Run external app
       */
      String extApp = null;

      if (actionRunExternalApp == _actionRunExternalApp1) {
         extApp = _prefStore_Photo.getString(IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_FILE_VIEWER_1).trim();
      } else if (actionRunExternalApp == _actionRunExternalApp2) {
         extApp = _prefStore_Photo.getString(IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_FILE_VIEWER_2).trim();
      } else if (actionRunExternalApp == _actionRunExternalApp3) {
         extApp = _prefStore_Photo.getString(IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_FILE_VIEWER_3).trim();
      }

      if (photo == null) {
         photo = _contextMenu_HoveredMapPoint.mapPoint.photo;
      }

      final String imageFilePath = photo.imageFilePathName;

      String commands[] = null;
      if (UI.IS_WIN) {

         final String[] commandsWin = {

               UI.SYMBOL_QUOTATION_MARK + extApp + UI.SYMBOL_QUOTATION_MARK,
               UI.SYMBOL_QUOTATION_MARK + imageFilePath + UI.SYMBOL_QUOTATION_MARK
         };

         commands = commandsWin;

      } else if (UI.IS_OSX) {

         final String[] commandsOSX = { "/usr/bin/open", "-a", // //$NON-NLS-1$ //$NON-NLS-2$
               extApp,
               imageFilePath };

         commands = commandsOSX;

      } else if (UI.IS_LINUX) {

         final String[] commandsLinux = { extApp, imageFilePath };

         commands = commandsLinux;
      }

      if (commands != null) {

         try {

            // log command
            final StringBuilder sb = new StringBuilder();
            for (final String cmd : commands) {
               sb.append(cmd + UI.SPACE1);
            }

            StatusUtil.logInfo(sb.toString());

            Runtime.getRuntime().exec(commands);

         } catch (final Exception e) {

            StatusUtil.showStatus(e);
         }
      }
   }

   private void actionGotoLocation() {

      final DialogGotoMapLocation dialogGotoMapLocation = new DialogGotoMapLocation(getMapPosition(), _map.getMouseDown_GeoPosition());

      dialogGotoMapLocation.open();

      if (dialogGotoMapLocation.getReturnCode() != Window.OK) {
         return;
      }

      final MapPosition mapPosition = dialogGotoMapLocation.getMapPosition();
      final boolean isCreateMapBookmark = dialogGotoMapLocation.isCreateMapBookmark();
      final String bookmarkName = dialogGotoMapLocation.getBookmarkName();

      /*
       * Show location as POI
       */
      final String latLonText = String.format(Messages.Map_POI_MapLocation,
            mapPosition.getLatitude(),
            mapPosition.getLongitude());

      final String poiText = isCreateMapBookmark
            ? bookmarkName + UI.NEW_LINE2 + latLonText
            : latLonText;

      _map.setPoi(new GeoPosition(mapPosition.getLatitude(), mapPosition.getLongitude()),
            mapPosition.getZoomLevel() + 1, // the correct zoom level is lost, adjust it to keep the same as the current
            poiText);

      /*
       * Create map bookmark
       */
      if (isCreateMapBookmark) {
         MapBookmarkManager.addBookmark(mapPosition, bookmarkName);
      }
   }

   private void actionMapMarker_CenterMap() {

      final PaintedMapPoint hoveredMapPoint = _contextMenu_HoveredMapPoint;

      final GeoPoint geoPoint = hoveredMapPoint.mapPoint.geoPoint;
      final GeoPosition geoPosition = new GeoPosition(geoPoint.getLatitude(), geoPoint.getLongitude());

      _map.setMapCenter(geoPosition);
   }

   private void actionMapMarker_Edit() {

      final PaintedMapPoint hoveredMapPoint = _contextMenu_HoveredMapPoint;

      final TourMarker tourMarker = hoveredMapPoint.mapPoint.tourMarker;

      final ITourProvider tourProvider = () -> {

         final ArrayList<TourData> allTours = new ArrayList<>();
         allTours.add(tourMarker.getTourData());

         return allTours;
      };

      ActionOpenMarkerDialog.doAction(tourProvider, true, tourMarker);

      // hide hovered marker
      _map.resetHoveredMapPoint();

      _map.paint();
   }

   private void actionMapMarker_ShowOnlyThisTour() {

      final PaintedMapPoint hoveredMapPoint = _contextMenu_HoveredMapPoint;

      final Map2Point mapPoint = hoveredMapPoint.mapPoint;
      final MapPointType pointType = mapPoint.pointType;

      TourData tourData = null;

      if (pointType.equals(MapPointType.TOUR_MARKER)) {

         tourData = mapPoint.tourMarker.getTourData();

      } else if (pointType.equals(MapPointType.TOUR_PAUSE)) {

         tourData = mapPoint.tourPause.tourData;
      }

      if (tourData != null) {

         paintTours_20_One(tourData, true);

         // force marker repaint otherwise the other markers are still be visible
         // this seems to be more complicated, a syncexec() to not work
         _map.getDisplay().timerExec(300, () -> {

            _map.resetHoveredMapPoint();

            _map.paint();
         });
      }
   }

   private void actionMapMarker_ZoomIn() {

      final PaintedMapPoint hoveredMapPoint = _contextMenu_HoveredMapPoint;

      final GeoPoint geoPoint = hoveredMapPoint.mapPoint.geoPoint;

      _map.setZoom(_map.getMapProvider().getMaximumZoomLevel());
      _map.setMapCenter(new GeoPosition(geoPoint.getLatitude(), geoPoint.getLongitude()));

      _map.redraw();

      // this need a delay otherwise the hovered map point is not hidden
      _map.getDisplay().timerExec(10, () -> {

         // hide hovered marker
         _map.resetHoveredMapPoint();
      });
   }

   private void actionPhoto_AutoSelect() {

      final boolean isAutoSelect = _actionMapPoint_Photo_AutoSelect.isChecked();

      // update model
      _state.put(SlideoutMap2_PhotoOptions.STATE_IS_PHOTO_AUTO_SELECT, isAutoSelect);

      Map2PainterConfig.isPhotoAutoSelect = isAutoSelect;
   }

   private void actionPhoto_EditPhotoLabel() {

      final Map2Point mapPoint = _contextMenu_HoveredMapPoint.mapPoint;
      final Photo hoveredPhoto = mapPoint.photo;

      final List<TourPhoto> allTourPhotos = TourPhotoManager.getTourPhotos(hoveredPhoto);

      if (allTourPhotos.size() < 1) {
         return;
      }

      final String oldLabel = allTourPhotos.get(0).getPhotoLabel();

      final DialogEditPhotoLabel labelDialog = new DialogEditPhotoLabel(

            Display.getDefault().getActiveShell(),

            Messages.Map_Action_EditPhotoLabel_Dialog_Title,
            Messages.Map_Action_EditPhotoLabel_Dialog_Message,

            oldLabel,
            null);

      labelDialog.open();

      if (labelDialog.getReturnCode() != Window.OK) {
         return;
      }

      final String newPhotoLabel = labelDialog.getValue();

      final Set<Long> allUpdatedTours = new HashSet<>();

      for (final TourPhoto tourPhoto : allTourPhotos) {

         tourPhoto.setPhotoLabel(newPhotoLabel);

         allUpdatedTours.add(tourPhoto.getTourId());
      }

      if (allUpdatedTours.size() > 0) {

         final List<TourData> allUpdatedTourData = new ArrayList<>();

         for (final Long tourID : allUpdatedTours) {
            allUpdatedTourData.add(TourManager.getTour(tourID));
         }

         TourManager.saveModifiedTours(allUpdatedTourData, getAllTourIDs());

         // update UI
         _map.paint();
      }
   }

   private void actionPhoto_ResetGeoPosition() {

      final Map2Point mapPoint = _contextMenu_HoveredMapPoint.mapPoint;
      final Photo hoveredPhoto = mapPoint.photo;

      final List<TourPhoto> allTourPhotos = TourPhotoManager.getTourPhotos(hoveredPhoto);

      if (allTourPhotos.size() < 1) {
         return;
      }

      final TourPhoto tourPhoto = allTourPhotos.get(0);

      if (tourPhoto != null) {

         final TourData tourData = tourPhoto.getTourData();

         if (tourData.isPhotoTour() == false) {
            return;
         }

         final Set<Long> allTourPhotosWithGeoPosition = tourData.getTourPhotosWithPositionedGeo();

         allTourPhotosWithGeoPosition.remove(tourPhoto.getPhotoId());

         // recompute geo positions
         tourData.computeGeo_Photos();

         TourManager.saveModifiedTour(tourData);
      }
   }

   private void actionPhoto_ShowAnnotations() {

      final boolean isShowAnnotations = _actionMapPoint_Photo_ShowAnnotations.isChecked();

      // update model
      _state.put(SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_ANNOTATIONS, isShowAnnotations);

      Map2PainterConfig.isShowPhotoAnnotations = isShowAnnotations;

      _slideoutPhotoOptions.updateUI_FromState();

      // update UI
      _map.paint();
   }

   private void actionPhoto_ShowHistogram() {

      final boolean isShowPhotoHistogram = _actionMapPoint_Photo_ShowHistogram.isChecked();

      // update model
      _state.put(SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_HISTOGRAM, isShowPhotoHistogram);

      Map2PainterConfig.isShowPhotoHistogram = isShowPhotoHistogram;

      // update UI
      if (isShowPhotoHistogram == false) {

         // hide photo histogram

         _map.photoHistogram_Close();
      }
   }

   private void actionPhoto_ShowLabel() {

      final boolean isShowPhotoLabel = _actionMapPoint_Photo_ShowLabel.isChecked();

      // update model
      _state.put(SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_LABEL, isShowPhotoLabel);

      Map2PainterConfig.isShowPhotoLabel = isShowPhotoLabel;

      // update UI
      _map.paint();
   }

   private void actionPhoto_ShowRating() {

      final boolean isShowPhotoRating = _actionMapPoint_Photo_ShowRating.isChecked();

      // update model
      _state.put(SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_RATING, isShowPhotoRating);

      Map2PainterConfig.isShowPhotoRating = isShowPhotoRating;

      _slideoutPhotoOptions.updateUI_FromState();

      // update UI
      _map.paint();
   }

   private void actionPhoto_ShowTooltip() {

      final boolean isShowPhotoTooltip = _actionMapPoint_Photo_ShowTooltip.isChecked();

      // update model
      _state.put(SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_TOOLTIP, isShowPhotoTooltip);

      Map2PainterConfig.isShowPhotoTooltip = isShowPhotoTooltip;

      // update UI
      if (isShowPhotoTooltip == false) {

         // hide photo tooltip

         _map.photoTooltip_Close();
      }
   }

   private void actionPhoto_Tonality_Copy() {

      final TourPhoto tourPhoto = getHoveredTourPhoto();

      if (tourPhoto == null) {
         return;
      }

      final PhotoAdjustments photoAdjustments = tourPhoto.getPhotoAdjustments(false);

      if (photoAdjustments.isSetTonality == false) {
         return;
      }

      _copyPaste_PhotoAdjustments = photoAdjustments;

      final Clipboard clipboard = new Clipboard(_map.getDisplay());
      {
         clipboard.setContents(

               new Object[] { new Object() },
               new Transfer[] { _copyPaste_Transfer });
      }
      clipboard.dispose();

      UI.showStatusLineMessage(Messages.Map_Action_CopyTonality_StatusLine);
   }

   private void actionPhoto_Tonality_Paste() {

      Object contents;

      final Clipboard clipboard = new Clipboard(_map.getDisplay());
      {
         contents = clipboard.getContents(_copyPaste_Transfer);
      }
      clipboard.dispose();

      if (contents instanceof final CopyPasteCurveValues copyPasteValues) {

         final Photo photo = _contextMenu_HoveredMapPoint.mapPoint.photo;
         photo.isSetTonality = true;

         // set flag that the map photo is recomputed
         photo.isAdjustmentModified = true;

         final int numValues = copyPasteValues.allCurveValuesX.length;

         final CurveValues photoCurveValues = photo.getToneCurvesFilter().getCurves().getActiveCurve().curveValues;

         photoCurveValues.allValuesX = Arrays.copyOf(copyPasteValues.allCurveValuesX, numValues);
         photoCurveValues.allValuesY = Arrays.copyOf(copyPasteValues.allCurveValuesY, numValues);

         TourPhotoManager.updatePhotoAdjustmentsInDB(photo);

         // select photo
         _map.selectPhoto(photo, _contextMenu_HoveredMapPoint);
      }
   }

   public void actionPOI() {

      final boolean isShowPOI = _actionShowPOI.isChecked();

      _map.setShowPOI(isShowPOI);

      if (isShowPOI) {
         _map.setPoi(_poiPosition, _map.getZoomLevel(), _poiName);
      }
   }

   public void actionReloadFailedMapImages() {

      _map.deleteFailedImageFiles();
      _map.resetAll();
   }

   public void actionSaveDefaultPosition() {

      _defaultZoom = _map.getZoomLevel();
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

      Map2PainterConfig.isShowTourStartEnd = _actionShowStartEndInMap.isChecked();

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

   public void actionSetShowTourWeatherInMap() {

      final boolean isVisible = _actionShowTourWeatherInMap.isChecked();

      if (isVisible) {
         _tourToolTip.addToolTipProvider(_tourWeatherToolTipProvider);
      } else {
         _tourToolTip.removeToolTipProvider(_tourWeatherToolTipProvider);
      }

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

   /**
    * Toggle sequence between
    * <ul>
    * <li>Center to map center</li>
    * <li>Center to mouse position</li>
    * <li>Center whole tour</li>
    * </ul>
    *
    * @param event
    */
   public void actionSetZoomCentered(final Event event) {

      final boolean isForwards = UI.isCtrlKey(event) == false;

      CenterMapBy newCenterMode;

      switch (_map.getCenterMapBy()) {
      case Tour:
         if (isForwards) {

            // tour -> map
            newCenterMode = CenterMapBy.Map;

         } else {

            // tour -> mouse
            newCenterMode = CenterMapBy.Mouse;
         }

         break;

      case Map:
         if (isForwards) {

            // map -> mouse
            newCenterMode = CenterMapBy.Mouse;

         } else {

            // map -> tour
            newCenterMode = CenterMapBy.Tour;
         }

         break;

      default:
      case Mouse:
         if (isForwards) {

            // mouse -> tour
            newCenterMode = CenterMapBy.Tour;

         } else {

            // mouse -> map
            newCenterMode = CenterMapBy.Map;
         }
         break;
      }

      _map.setCenterMapBy(newCenterMode);

      _actionZoom_CenterMapBy.setCenterMode(newCenterMode);
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

   private void actionShowPhotos(final boolean isPhotoVisible) {

      _isShowPhoto = isPhotoVisible;

      enableActions();

      Map2PainterConfig.isShowPhotos = _isShowPhoto;

      // update UI in the map point slideout
      Map2PointManager.enableControls();

      _map.setOverlayKey(Integer.toString(_filteredPhotos.hashCode()));
      _map.disposeOverlayImageCache();

      _map.paint();

      // hide all photo slideouts when photos are hidden
      if (isPhotoVisible == false) {

         _actionMap2Slideout_PhotoFilter.getPhotoFilterSlideout().close();
         _slideoutPhotoOptions.close();
         _map.photoTooltip_Close();
         _map.photoHistogram_Close();
      }
   }

   public void actionShowSlider() {

      if (_allTourData.isEmpty()) {
         return;
      }

      final boolean isShowSliderInMap = _actionShowSliderInMap.isChecked();

      // keep state for the slideout
      _state.put(Map2View.STATE_IS_SHOW_SLIDER_IN_MAP, isShowSliderInMap);

      // repaint map
      _directMappingPainter.setPaintingOptions(

            _isShowTour,
            _allTourData.get(0),

            _currentSliderValueIndex_Left,
            _currentSliderValueIndex_Right,
            _externalValuePointIndex,

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

      _map.setZoom(_map.getZoomLevel() + 1, _map.getCenterMapBy());
   }

   public void actionZoomOut() {

      _map.setZoom(_map.getZoomLevel() - 1, _map.getCenterMapBy());
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
       * Reset position for all tours, it was really annoying that previous selected tours moved to
       * their previous positions, implemented in 11.8.2
       */
      TourManager.getInstance().resetMapPositions();

      _map.setShowOverlays(true);

      paintEntireTour();
   }

   public void addCommonLocation(final TourLocation tourLocation) {

      // update model + slideout UI
      CommonLocationManager.addLocation(tourLocation);

      // update map UI
      _map.paint();
   }

   private void addMapListener() {

// SET_FORMATTING_OFF

      _map.addBreadcrumbListener    (()                           -> mapListener_Breadcrumb());
      _map.addHoveredTourListener   (mapHoveredTourEvent          -> mapListener_HoveredTour(mapHoveredTourEvent));
      _map.addMapInfoListener       ((mapCenter, mapZoomLevel)    -> mapListener_MapInfo(mapCenter, mapZoomLevel));
      _map.addMapSelectionListener  (selection                    -> mapListener_MapSelection(selection));
      _map.addMousePositionListener (mapGeoPositionEvent          -> mapListener_MousePosition(mapGeoPositionEvent));
      _map.addPOIListener           (mapPOIEvent                  -> mapListener_POI(mapPOIEvent));
      _map.addTourSelectionListener (selection                    -> mapListener_InsideMap(selection));
      _map.addExternalAppListener   ((numberOfExternalApp, photo) -> mapListener_RunExternalApp(numberOfExternalApp, photo));

      _map.addMapGridBoxListener    ((mapZoomLevel, mapGeoCenter, isGridSelected, mapGridData)  -> mapListener_MapGridBox(mapZoomLevel, mapGeoCenter, isGridSelected, mapGridData));
      _map.addMapPositionListener   ((mapCenter, mapZoomLevel, isZoomed)                        -> mapListener_MapPosition(mapCenter, mapZoomLevel, isZoomed));

      _map.addControlListener       (ControlListener.controlResizedAdapter(controlEvent -> mapListener_ControlResize(controlEvent)));

// SET_FORMATTING_ON

      _map.setMapContextProvider(this);
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {

         private void onPartClosed(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == Map2View.this) {

               _mapInfoManager.resetInfo();
            }
         }

         private void onPartHidden(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == Map2View.this) {

               _isPartVisible = false;

               // hide value point tooltip
               _valuePointTooltipUI.setShellVisible(false);
            }
         }

         private void onPartVisible(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == Map2View.this && _isPartVisible == false) {

               _isPartVisible = true;

               // show tool tip again
               _valuePointTooltipUI.setShellVisible(true);

               if (_selectionWhenHidden != null) {

                  onSelectionChanged(_selectionWhenHidden);

                  _selectionWhenHidden = null;
               }
            }
         }

         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {

            onPartVisible(partRef);

            if (partRef.getPart(false) == Map2View.this) {

               // ensure that map sync is working
               Map25FPSManager.setBackgroundFPSToAnimationFPS(true);
            }
         }

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {
            onPartVisible(partRef);
         }

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {
            onPartClosed(partRef);
         }

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == Map2View.this) {
               Map25FPSManager.setBackgroundFPSToAnimationFPS(false);
            }
         }

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {
            onPartHidden(partRef);
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

         } else if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)
               || property.equals(ITourbookPreferences.MAP2_OPTIONS_IS_MODIFIED)) {

            // update tour and legend
            createLegendImage(Map2PainterConfig.getMapColorProvider());

            _map.updateGraphColors();
            _map.updateMapOptions();

            // show/hide line gaps in pauses, this must be done AFTER updateMapOptions() !!!
            for (final TourData tourData : _allTourData) {
               setVisibleDataPoints(tourData);
            }

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

            createLegendImage(Map2PainterConfig.getMapColorProvider());

            _valuePointTooltipUI.reopen();

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

      _postSelectionListener = (part, selection) -> onSelectionChanged(selection);

      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = (part, eventId, eventData) -> {

         if (part == Map2View.this) {
            return;
         }

         if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

            resetMap();

         } else if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof final TourEvent tourEvent)) {

            final ArrayList<TourData> modifiedTours = tourEvent.getModifiedTours();
            if (CollectionUtils.isNotEmpty(modifiedTours)) {

               final List<Long> oldTourIDs = tourEvent.oldTourIDs;

               if (oldTourIDs != null) {

                  // tour is saved but use the old tours to update the UI

                  final List<TourData> allTourData = TourManager.getInstance().getTourData(oldTourIDs);

                  setTourData(allTourData);

               } else {

                  setTourData(modifiedTours);
               }

               resetMap();
            }

         } else if (eventId == TourEventId.UPDATE_UI || eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

            clearView();

         } else if (eventId == TourEventId.MARKER_SELECTION) {

            if (eventData instanceof final SelectionTourMarker selectionTourMarker) {

               onSelection_TourMarker(selectionTourMarker, false);
            }

         } else if (eventId == TourEventId.PAUSE_SELECTION && eventData instanceof final SelectionTourPause selectionTourPause) {

            onSelection_TourPause(selectionTourPause, false);

         } else if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof final ISelection selection) {

            onSelectionChanged(selection);

         } else if (eventId == TourEventId.SLIDER_POSITION_CHANGED && eventData instanceof final ISelection selection) {

            onSelectionChanged(selection);

         } else if (eventId == TourEventId.MAP_SHOW_GEO_GRID) {

            if (eventData instanceof final TourGeoFilter tourGeoFilter) {

               // show geo filter

               // show geo search rectangle
               _map.showGeoSearchGrid(tourGeoFilter);

               // show tours in search rectangle
               geoFilter_10_Loader(tourGeoFilter.mapGridData, tourGeoFilter);

            } else if (eventData == null) {

               // hide geo grid

               hideGeoGrid();
            }

         } else if (eventId == TourEventId.HOVERED_VALUE_POSITION &&
               eventData instanceof final HoveredValueData hoveredValueData) {

            onSelection_HoveredValue(hoveredValueData);

         } else if (eventId == TourEventId.COMMON_LOCATION_SELECTION) {

            moveToCommonLocation(eventData);

         } else if (eventId == TourEventId.TOUR_LOCATION_SELECTION) {

            moveToTourLocation(eventData);

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

      final int zoom = _map.getZoomLevel();

      final Rectangle positionRect = _map.getWorldPixelFromGeoPositions(positionBounds, zoom);

      final Point center = new Point(
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

      final int zoom = _map.getZoomLevel();

      Set<GeoPosition> positionBounds = null;

      if (_isTourPainted) {

         // tour or waypoint is painted

         positionBounds = Map2PainterConfig.getTourBounds();

         if (positionBounds == null) {
            return;
         }

      } else {

         // POI is painted

         if (_poiPosition == null) {
            return;
         }

         positionBounds = new HashSet<>();
         positionBounds.add(_poiPosition);
      }

      final Rectangle positionRect = _map.getWorldPixelFromGeoPositions(positionBounds, zoom);

      final Point positionCenter = new Point(
            positionRect.x + positionRect.width / 2,
            positionRect.y + positionRect.height / 2);

      final GeoPosition geoPosition = _map.getMapProvider().pixelToGeo(positionCenter, zoom);

      _map.setMapCenter(geoPosition);
   }

   private void clearView() {

      // disable tour data
      _allTourData.clear();
      _previousTourData = null;

      Map2PainterConfig.resetTourData();
      Map2PainterConfig.setPhotos(null, false, false);

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
            Images.Graph_Elevation);

      _actionTourColor_Gradient = new ActionTourColor(
            this,
            MapGraphId.Gradient,
            Messages.map_action_tour_color_gradient_tooltip,
            Images.Graph_Gradient);

      _actionTourColor_Power = new ActionTourColor(
            this,
            MapGraphId.Power,
            Messages.map_action_tour_color_power_tooltip,
            Images.Graph_Power);

      _actionTourColor_Pulse = new ActionTourColor(
            this,
            MapGraphId.Pulse,
            Messages.map_action_tour_color_pulse_tooltip,
            Images.Graph_Heartbeat);

      _actionTourColor_Speed = new ActionTourColor(
            this,
            MapGraphId.Speed,
            Messages.map_action_tour_color_speed_tooltip,
            Images.Graph_Speed);

      _actionTourColor_Pace = new ActionTourColor(
            this,
            MapGraphId.Pace,
            Messages.map_action_tour_color_pace_tooltip,
            Images.Graph_Pace);

      _actionTourColor_RunDyn_StepLength = new ActionTourColor(
            this,
            MapGraphId.RunDyn_StepLength,
            Messages.Tour_Action_RunDyn_StepLength_Tooltip,
            Images.Graph_RunDyn_StepLength);

      _actionTourColor_HrZone = new ActionTourColor(
            this,
            MapGraphId.HrZone,
            Messages.Tour_Action_ShowHrZones_Tooltip,
            Images.PulseZones);

// SET_FORMATTING_OFF

      _allTourColor_Actions.put(MapGraphId.Altitude,           _actionTourColor_Elevation);
      _allTourColor_Actions.put(MapGraphId.Gradient,           _actionTourColor_Gradient);
      _allTourColor_Actions.put(MapGraphId.Power,              _actionTourColor_Power);
      _allTourColor_Actions.put(MapGraphId.Pulse,              _actionTourColor_Pulse);
      _allTourColor_Actions.put(MapGraphId.Speed,              _actionTourColor_Speed);
      _allTourColor_Actions.put(MapGraphId.Pace,               _actionTourColor_Pace);
      _allTourColor_Actions.put(MapGraphId.HrZone,             _actionTourColor_HrZone);
      _allTourColor_Actions.put(MapGraphId.RunDyn_StepLength,  _actionTourColor_RunDyn_StepLength);

      // actions with slideouts
      _actionMap2Slideout_Bookmarks             = new ActionMapBookmarks(this._parent, this);
      _actionMap2Slideout_Color                 = new ActionMap2Color();
      _actionMap2Slideout_MapPoints             = new ActionMap2_MapPoints();
      _actionMap2Slideout_MapProvider           = new ActionMap2_MapProvider(this, _state_MapProvider);
      _actionMap2Slideout_PhotoFilter           = new ActionMap2_PhotoFilter(this, _state_PhotoFilter);
      _actionMap2Slideout_Options               = new ActionMap2_Options();
      _actionMap2Slideout_SyncMap               = new ActionSyncMap();
      _actionMap2Slideout_TourColors            = new ActionMap2_Graphs();

      _actionZoom_CenterMapBy                   = new ActionZoomCenterBy(this);
      _actionZoom_In                            = new ActionZoomIn(this);
      _actionZoom_Out                           = new ActionZoomOut(this);
      _actionZoom_ShowEntireMap                 = new ActionZoomShowEntireMap(this);
      _actionZoom_ShowEntireTour                = new ActionZoomShowEntireTour(this);

      _actionRunExternalAppTitle                = new ActionRunExternalAppTitle();
      _actionRunExternalAppPrefPage             = new ActionRunExternalAppPrefPage();
      _actionRunExternalApp1                    = new ActionRunExternalApp();
      _actionRunExternalApp2                    = new ActionRunExternalApp();
      _actionRunExternalApp3                    = new ActionRunExternalApp();

      _actionCopyLocation                       = new ActionCopyLocation();
      _actionCreateTourMarkerFromMap            = new ActionCreateTourMarkerFromMap(this);
      _actionGotoLocation                       = new ActionGotoLocation();
      _actionLookupTourLocation                 = new ActionLookupCommonLocation(this);
      _actionManageMapProvider                  = new ActionManageMapProviders(this);
      _actionMapPoint_CenterMap                 = new ActionMapPoint_CenterMap();
      _actionMapPoint_EditTourMarker            = new ActionMapPoint_EditTourMarker();
      _actionMapPoint_Photo_AutoSelect          = new ActionMapPoint_Photo_AutoSelect();
      _actionMapPoint_Photo_Deselect            = new ActionMapPoint_Photo_Deselect();
      _actionMapPoint_Photo_EditLabel           = new ActionMapPoint_Photo_EditLabel();
      _actionMapPoint_Photo_RemoveFromTour      = new ActionMapPoint_Photo_RemoveFromTour();
      _actionMapPoint_Photo_ReplaceGeoPosition  = new ActionMapPoint_Photo_ReplaceGeoPosition();
      _actionMapPoint_Photo_ShowAnnotations     = new ActionMapPoint_Photo_ShowAnnotations();
      _actionMapPoint_Photo_ShowHistogram       = new ActionMapPoint_Photo_ShowHistogram();
      _actionMapPoint_Photo_ShowLabel           = new ActionMapPoint_Photo_ShowLabel();
      _actionMapPoint_Photo_ShowRating          = new ActionMapPoint_Photo_ShowRating();
      _actionMapPoint_Photo_ShowTooltip         = new ActionMapPoint_Photo_ShowTooltip();
      _actionMapPoint_Photo_Tonality_Copy       = new ActionMapPoint_Photo_Tonality_Copy();
      _actionMapPoint_Photo_Tonality_Paste      = new ActionMapPoint_Photo_Tonality_Paste();
      _actionMapPoint_ShowOnlyThisTour          = new ActionMapPoint_ShowOnlyThisTour();
      _actionMapPoint_ZoomIn                    = new ActionMapPoint_ZoomIn();
      _actionReloadFailedMapImages              = new ActionReloadFailedMapImages(this);
      _actionSaveDefaultPosition                = new ActionSaveDefaultPosition(this);
      _actionExportMap_SubMenu                  = new Action_ExportMap_SubMenu(this);
      _actionSearchTourByLocation               = new ActionSearchTourByLocation();
      _actionSetDefaultPosition                 = new ActionSetDefaultPosition(this);
      _actionShowAllFilteredPhotos              = new ActionShowAllFilteredPhotos(this);
      _actionShowLegendInMap                    = new ActionShowLegendInMap(this);
      _actionMap2Slideout_PhotoOptions          = new ActionMap2_PhotoOptions();
      _actionSetGeoPositionForGeoMarker         = new ActionSetGeoPositionForGeoMarker();
      _actionSetGeoPositionForPhotoTours        = new ActionSetGeoPositionForPhotoTours(this);
      _actionShowScaleInMap                     = new ActionShowScaleInMap(this);
      _actionShowSliderInMap                    = new ActionShowSliderInMap(this);
      _actionShowSliderInLegend                 = new ActionShowSliderInLegend(this);
      _actionShowStartEndInMap                  = new ActionShowStartEndInMap(this);
      _actionShowValuePoint                     = new ActionShowValuePoint(this);
      _actionShowPOI                            = new ActionShowPOI(this);
      _actionShowTour                           = new ActionShowTour();
      _actionShowTourInfoInMap                  = new ActionShowTourInfoInMap(this);
      _actionShowTourWeatherInMap               = new ActionShowTourWeatherInMap(this);
      _actionSubMenu_SetTourMarkerType          = new SubMenu_SetTourMarkerType();
      _actionZoomLevelAdjustment                = new ActionZoomLevelAdjustment();

      // map sync actions
      _actionSyncMapWith_OtherMap               = new ActionSyncMapWith_OtherMap(this);
      _actionSyncMapWith_Photo                  = new ActionSyncMapWith_Photo(this);
      _actionSyncMapWith_Slider_Centered        = new ActionSyncMapWith_Slider_Centered(this);
      _actionSyncMapWith_Slider_One             = new ActionSyncMapWith_Slider_One(this);
      _actionSyncMapWith_Tour                   = new ActionSyncMapWith_Tour(this);
      _actionSyncMapWith_TourLocation           = new ActionSyncMapWith_TourLocation();
      _actionSyncMapWith_ValuePoint             = new ActionSyncMapWith_ValuePoint(this);

      _allSyncMapActions.put(MapSyncId.SyncMapWith_OtherMap,            _actionSyncMapWith_OtherMap);
      _allSyncMapActions.put(MapSyncId.SyncMapWith_Photo,               _actionSyncMapWith_Photo);
      _allSyncMapActions.put(MapSyncId.SyncMapWith_Slider_One,          _actionSyncMapWith_Slider_One);
      _allSyncMapActions.put(MapSyncId.SyncMapWith_Slider_Centered,     _actionSyncMapWith_Slider_Centered);
      _allSyncMapActions.put(MapSyncId.SyncMapWith_Tour,                _actionSyncMapWith_Tour);
      _allSyncMapActions.put(MapSyncId.SyncMapWith_TourLocation,        _actionSyncMapWith_TourLocation);
      _allSyncMapActions.put(MapSyncId.SyncMapWith_ValuePoint,          _actionSyncMapWith_ValuePoint);

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

      if (mapColorProvider instanceof final IGradientColorProvider gradientColorProvider) {

         final int legendHeightNoMargin = legendHeight - 2 * IMapColorProvider.LEGEND_MARGIN_TOP_BOTTOM;

         isDataAvailable = MapUtils.configureColorProvider(
               _allTourData,
               gradientColorProvider,
               ColorProviderConfig.MAP2,
               legendHeightNoMargin);

         if (isDataAvailable) {

            legendImage = TourMapPainter.createMap2_LegendImage_AWT(gradientColorProvider,
                  legendWidth,
                  legendHeight,
                  isBackgroundDark(),
                  true // draw unit shadow
            );

         } else {

            // return null image to hide the legend
         }

      } else if (mapColorProvider instanceof IDiscreteColorProvider) {

         // return null image to hide the legend -> there is currently no legend provider for a IDiscreteColorProvider

//         isDataAvailable = createLegendImage_20_SetProviderValues((IDiscreteColorProvider) mapColorProvider);
      }

      _mapLegend.setImage(legendImage);
   }

   @Override
   public void createPartControl(final Composite parent) {

      _parent = parent;

      _mapLegend = new MapLegend();

      _map = new Map2(parent, SWT.NONE, _state);
      _map.setPainting(false);

      _directMappingPainter = new DirectMappingPainter(_map);
      _map.setDirectPainter(_directMappingPainter);

//    _map.setLiveView(true);

      _map.setLegend(_mapLegend);
      _map.setShowLegend(true);
      _map.setMeasurementSystem(UI.UNIT_VALUE_DISTANCE, UI.UNIT_LABEL_DISTANCE);

      // setup tool tip's
      _map.setTourToolTip(_tourToolTip = new TourToolTip(_map));
      _tourInfoToolTipProvider.setActionsEnabled(true);
      _tourInfoToolTipProvider.setNoTourTooltip(OtherMessages.TOUR_TOOLTIP_LABEL_NO_GEO_TOUR);

      /*
       * Setup value point tooltip
       */
      final IPinned_Tooltip_Owner valuePoint_ToolTipOwner = new IPinned_Tooltip_Owner() {

         @Override
         public Control getControl() {
            return _map;
         }

         @Override
         public void handleMouseEvent(final Event event, final org.eclipse.swt.graphics.Point mouseDisplayPosition) {

            // is not yet used
         }
      };
      _valuePointTooltipUI = new ValuePoint_ToolTip_UI(
            valuePoint_ToolTipOwner,
            Messages.Map_Label_ValuePoint_Title,
            _state,
            ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE_MAP2,

            false // do not show the chart zoom factor
      );

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

      setMapImageSize();

      // initialize map when part is created and the map size is > 0
      _map.getDisplay().asyncExec(() -> {

         if (_map.getDisplay().isDisposed()) {

            // fixing https://github.com/mytourbook/mytourbook/issues/1233
            return;
         }

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

   /**
    * @param activeAction
    *           Action which should <b>NOT</b> be deactivated
    */
   private void deactivateOtherSync(final Action activeAction) {

      if (activeAction != _actionSyncMapWith_OtherMap) {

         _isMapSyncWith_OtherMap = false;
         _actionSyncMapWith_OtherMap.setChecked(false);
      }

      if (activeAction != _actionSyncMapWith_Photo) {

         _isMapSyncWith_Photo = false;
         _actionSyncMapWith_Photo.setChecked(false);
      }

      if (activeAction != _actionSyncMapWith_Slider_One) {

         _isMapSyncWith_Slider_One = false;

         _actionSyncMapWith_Slider_One.setChecked(false);
         _actionSyncMapWith_Slider_Centered.setChecked(false);
      }

      if (activeAction != _actionSyncMapWith_Tour) {

         _isMapSyncWith_Tour = false;
         _actionSyncMapWith_Tour.setChecked(false);
      }

      if (activeAction != _actionSyncMapWith_TourLocation) {

         _isMapSyncWith_MapLocation = false;
         _actionSyncMapWith_TourLocation.setChecked(false);
      }

      if (activeAction != _actionSyncMapWith_ValuePoint) {

         _isMapSyncWith_ValuePoint = false;
         _actionSyncMapWith_ValuePoint.setChecked(false);
      }
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

      _valuePointTooltipUI.hide();

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

      // update legend action
      if (_isTourPainted) {

         _map.setShowLegend(_isShowLegend);

         if (_isShowLegend == false) {
            _actionShowSliderInLegend.setChecked(false);
         }
      }

      final Long hoveredTourId = _map.getHoveredTourId();

      // update action because after closing the context menu, the hovered values are reset in the map paint event
      _actionCreateTourMarkerFromMap.setCurrentHoveredTourId(hoveredTourId);
      _actionLookupTourLocation.setCurrentHoveredTourId(hoveredTourId);

      /*
       * Set geo positions
       */
      final GeoPosition currentMouseGeoPosition = _map.getMouseMove_GeoPosition();

      String actionGeoPositionLabel = Messages.Map_Action_GeoPositions_Set;
      boolean canCreateGeoPositions = false;

      final TourData tourData = getTourDataWhereGeoPositionsCanBeSet();

      if (tourData != null) {

         canCreateGeoPositions = true;
         actionGeoPositionLabel = (Messages.Map_Action_GeoPositions_SetInto.formatted(TourManager.getTourTitle(tourData)));

         _actionSetGeoPositionForPhotoTours.setData(currentMouseGeoPosition);
      }

      _actionSetGeoPositionForPhotoTours.setEnabled(canCreateGeoPositions);
      _actionSetGeoPositionForPhotoTours.setText(actionGeoPositionLabel);

      /*
       * Set geo positions into tour geo marker
       */
      final List<TourMarker> allGeoMarker = getTourMarkersWhereGeoPositionsCanBeSet();

      _actionSetGeoPositionForGeoMarker.setContextData(allGeoMarker, currentMouseGeoPosition);
      _actionSetGeoPositionForGeoMarker.setEnabled(allGeoMarker != null && allGeoMarker.size() > 0);

// SET_FORMATTING_OFF

      /*
       * Photo actions
       */
      final boolean isAllPhotoAvailable      = _allPhotos.size() > 0;
      final boolean isFilteredPhotoAvailable = _filteredPhotos.size() > 0;
      final boolean canShowFilteredPhoto     = isFilteredPhotoAvailable && _isShowPhoto;

      /*
       * Sync photo has a higher priority than sync tour, both cannot be synced at the same time
       */
//      final boolean isPhotoSynced = canShowFilteredPhoto && _isMapSynchedWithPhoto;
//      final boolean canSyncTour = isPhotoSynced == false;

      _actionMap2Slideout_PhotoFilter     .setEnabled(isAllPhotoAvailable && _isShowPhoto);
      _actionMap2Slideout_PhotoOptions    .setEnabled(isAllPhotoAvailable);
      _actionShowAllFilteredPhotos        .setEnabled(canShowFilteredPhoto);
      _actionSyncMapWith_Photo            .setEnabled(canShowFilteredPhoto);

      /*
       * Tour actions
       */
      final int numTours                  = _allTourData.size();
      final boolean isTourAvailable       = numTours > 0;
      final boolean isMultipleTours       = numTours > 1 && _isShowTour;
      final boolean isOneTourDisplayed    = _isTourPainted && isMultipleTours == false && _isShowTour;
      final boolean isOneTourHovered      = hoveredTourId != null;

      _actionCreateTourMarkerFromMap      .setEnabled(isTourAvailable && isOneTourHovered);
      _actionLookupTourLocation           .setEnabled(true);
      _actionMap2Slideout_Color           .setEnabled(isTourAvailable);
      _actionShowLegendInMap              .setEnabled(_isTourPainted);
      _actionShowPOI                      .setEnabled(_poiPosition != null);
      _actionShowSliderInLegend           .setEnabled(_isTourPainted && _isShowLegend);
      _actionShowSliderInMap              .setEnabled(_isTourPainted);
      _actionShowStartEndInMap            .setEnabled(isOneTourDisplayed);
      _actionShowTourInfoInMap            .setEnabled(isOneTourDisplayed);
      _actionShowTour                     .setEnabled(_isTourPainted);
      _actionShowTourWeatherInMap         .setEnabled(isTourAvailable);
      _actionZoom_CenterMapBy             .setEnabled(true);
      _actionZoom_ShowEntireTour          .setEnabled(_isTourPainted && isTourAvailable);
      _actionZoomLevelAdjustment          .setEnabled(isTourAvailable);

      _actionSyncMapWith_Slider_One       .setEnabled(isTourAvailable);
      _actionSyncMapWith_Slider_Centered  .setEnabled(isTourAvailable);
      _actionSyncMapWith_OtherMap         .setEnabled(true);
      _actionSyncMapWith_Tour             .setEnabled(isTourAvailable);
      _actionSyncMapWith_ValuePoint       .setEnabled(isTourAvailable);

      syncMap_UpdateSyncSlideoutAction(_isMapSyncActive);

      if (numTours == 0) {

         _actionTourColor_Elevation          .setEnabled(false);
         _actionTourColor_Gradient           .setEnabled(false);
         _actionTourColor_Power              .setEnabled(false);
         _actionTourColor_Pulse              .setEnabled(false);
         _actionTourColor_Speed              .setEnabled(false);
         _actionTourColor_Pace               .setEnabled(false);
         _actionTourColor_HrZone             .setEnabled(false);
         _actionTourColor_RunDyn_StepLength  .setEnabled(false);

      } else if (isForceTourColor) {

         _actionTourColor_Elevation          .setEnabled(true);
         _actionTourColor_Gradient           .setEnabled(true);
         _actionTourColor_Power              .setEnabled(true);
         _actionTourColor_Pulse              .setEnabled(true);
         _actionTourColor_Speed              .setEnabled(true);
         _actionTourColor_Pace               .setEnabled(true);
         _actionTourColor_HrZone             .setEnabled(true);
         _actionTourColor_RunDyn_StepLength  .setEnabled(true);

      } else if (isOneTourDisplayed) {

         final TourData oneTourData          = _allTourData.get(0);
         final boolean isPulse               = oneTourData.pulseSerie != null;
         final boolean canShowHrZones        = oneTourData.getNumberOfHrZones() > 0 && isPulse;

         _actionTourColor_Elevation          .setEnabled(true);
         _actionTourColor_Gradient           .setEnabled(oneTourData.getGradientSerie() != null);
         _actionTourColor_Power              .setEnabled(oneTourData.getPowerSerie() != null);
         _actionTourColor_Pulse              .setEnabled(isPulse);
         _actionTourColor_Speed              .setEnabled(oneTourData.getSpeedSerie() != null);
         _actionTourColor_Pace               .setEnabled(oneTourData.getPaceSerie() != null);
         _actionTourColor_HrZone             .setEnabled(canShowHrZones);
         _actionTourColor_RunDyn_StepLength  .setEnabled(oneTourData.runDyn_StepLength != null);

      } else {

         _actionTourColor_Elevation          .setEnabled(false);
         _actionTourColor_Gradient           .setEnabled(false);
         _actionTourColor_Power              .setEnabled(false);
         _actionTourColor_Pulse              .setEnabled(false);
         _actionTourColor_Speed              .setEnabled(false);
         _actionTourColor_Pace               .setEnabled(false);
         _actionTourColor_HrZone             .setEnabled(false);
         _actionTourColor_RunDyn_StepLength  .setEnabled(false);
      }

// SET_FORMATTING_ON
   }

   private void enableActions_MapPoint(final PaintedMapPoint hoveredMapPoint) {

      boolean isGeoPositionSet = false;
      boolean isPhotoTour = false;
      boolean isPhotoAdjustTonality = false;

      final Map2Point mapPoint = hoveredMapPoint.mapPoint;
      final MapPointType pointType = mapPoint.pointType;
      final Photo hoveredPhoto = mapPoint.photo;

      if (hoveredPhoto != null) {

         isPhotoAdjustTonality = hoveredPhoto.isSetTonality;

         final List<TourPhoto> allTourPhotos = TourPhotoManager.getTourPhotos(hoveredPhoto);

         if (allTourPhotos.size() > 0) {

            final TourPhoto tourPhoto = allTourPhotos.get(0);
            if (tourPhoto != null) {

               final TourData tourData = tourPhoto.getTourData();

               isPhotoTour = tourData.isPhotoTour();

               if (isPhotoTour) {

                  final Set<Long> allTourPhotosWithGeoPosition = tourData.getTourPhotosWithPositionedGeo();

                  isGeoPositionSet = allTourPhotosWithGeoPosition.contains(tourPhoto.getPhotoId());
               }
            }
         }
      }

      boolean canPasteTonality = false;

      final Clipboard clipboard = new Clipboard(_map.getDisplay());
      {
         final String[] allTypeNames = clipboard.getAvailableTypeNames();

         for (final String typeName : allTypeNames) {

            if (typeName.equals(CopyPasteTonalityTransfer.TYPE_NAME)) {

               canPasteTonality = true;

               break;
            }
         }
      }
      clipboard.dispose();

// SET_FORMATTING_OFF


      final int      numTours          = _allTourData.size();

      final boolean  isMultipleTours   = numTours > 1;
      final boolean  isOneTour         = numTours == 1;
      final boolean  isTourMarker      = pointType.equals(MapPointType.TOUR_MARKER);
      final boolean  isTourAvailable   = isTourMarker || pointType.equals(MapPointType.TOUR_PAUSE);

      _actionMapPoint_EditTourMarker            .setEnabled(isTourMarker);
      _actionMapPoint_ShowOnlyThisTour          .setEnabled(isMultipleTours && isTourAvailable);
      _actionMapPoint_Photo_ReplaceGeoPosition  .setEnabled(isGeoPositionSet);

      _actionMapPoint_Photo_Tonality_Copy       .setEnabled(isOneTour && isPhotoAdjustTonality);
      _actionMapPoint_Photo_Tonality_Paste      .setEnabled(canPasteTonality);

      // currently it is not supported to remove photos from a photo tour
      _actionMapPoint_Photo_RemoveFromTour      .setEnabled(isPhotoTour == false);

// SET_FORMATTING_ON

      /*
       * Restore state
       */
      final boolean isShowPhotoAnnotations = Util.getStateBoolean(_state,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_ANNOTATIONS,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_ANNOTATIONS_DEFAULT);

      final boolean isShowPhotoHistogram = Util.getStateBoolean(_state,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_HISTOGRAM,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_HISTOGRAM_DEFAULT);

      final boolean isShowPhotoLabel = Util.getStateBoolean(_state,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_LABEL,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_LABEL_DEFAULT);

      final boolean isShowPhotoRating = Util.getStateBoolean(_state,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_RATING,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_RATING_DEFAULT);

      final boolean isShowPhotoTooltip = Util.getStateBoolean(_state,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_TOOLTIP,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_TOOLTIP_DEFAULT);

      final boolean isShowHQPhotoImages = Util.getStateBoolean(_state,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_THUMB_HQ_IMAGES,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_THUMB_HQ_IMAGES_DEFAULT);

      final boolean isShowPhotoAdjustments = Util.getStateBoolean(_state,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_ADJUSTMENTS,
            SlideoutMap2_PhotoOptions.STATE_IS_SHOW_PHOTO_ADJUSTMENTS_DEFAULT);

// SET_FORMATTING_OFF

      _actionMapPoint_Photo_ShowAnnotations  .setChecked(isShowPhotoAnnotations);
      _actionMapPoint_Photo_ShowHistogram    .setChecked(isShowPhotoHistogram);
      _actionMapPoint_Photo_ShowLabel        .setChecked(isShowPhotoLabel);
      _actionMapPoint_Photo_ShowRating       .setChecked(isShowPhotoRating);
      _actionMapPoint_Photo_ShowTooltip      .setChecked(isShowPhotoTooltip);

      _actionMapPoint_Photo_ShowAnnotations  .setEnabled(isShowHQPhotoImages && isShowPhotoAdjustments);

// SET_FORMATTING_ON
   }

   private void fillActionBars() {

      /*
       * fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionMap2Slideout_TourColors);

      // must be called AFTER the tour color slideout action is added !!
      fillToolbar_TourColors(tbm);

      tbm.add(new Separator());

      tbm.add(_actionMap2Slideout_PhotoOptions);
      tbm.add(_actionMap2Slideout_PhotoFilter);
      tbm.add(_actionShowAllFilteredPhotos);

      tbm.add(new Separator());

      tbm.add(_actionMap2Slideout_MapPoints);
      tbm.add(_actionMap2Slideout_Bookmarks);

      tbm.add(new Separator());

      tbm.add(_actionShowTour);
      tbm.add(_actionZoom_ShowEntireTour);
      tbm.add(_actionMap2Slideout_SyncMap);

      tbm.add(new Separator());

      tbm.add(_actionZoom_In);
      tbm.add(_actionZoom_Out);
      tbm.add(_actionZoom_CenterMapBy);

      tbm.add(new Separator());

      tbm.add(_actionMap2Slideout_MapProvider);
      tbm.add(_actionMap2Slideout_Options);
   }

   @Override
   public void fillContextMenu(final IMenuManager menuMgr,
                               final ActionManageOfflineImages actionManageOfflineImages) {

      _contextMenu_HoveredMapPoint = _map.getHoveredMapPoint();

      if (_contextMenu_HoveredMapPoint != null) {

         // open context menu for a map marker

         final Map2Point mapPoint = _contextMenu_HoveredMapPoint.mapPoint;
         final boolean isPhotoAvailable = mapPoint.photo != null;

         enableActions_MapPoint(_contextMenu_HoveredMapPoint);

         _actionSubMenu_SetTourMarkerType.setTourMarker(mapPoint.tourMarker);
         _actionSubMenu_SetTourMarkerType.setOldTourIDs(getAllTourIDs());

         if (isPhotoAvailable) {

            menuMgr.add(_actionMapPoint_Photo_ShowRating);
            menuMgr.add(_actionMapPoint_Photo_AutoSelect);
            menuMgr.add(_actionMapPoint_Photo_Deselect);
            menuMgr.add(_actionMapPoint_Photo_ShowTooltip);
            menuMgr.add(_actionMapPoint_Photo_ShowHistogram);
            menuMgr.add(_actionMapPoint_Photo_ShowAnnotations);

            menuMgr.add(new Separator());

            menuMgr.add(_actionMapPoint_Photo_ShowLabel);
            menuMgr.add(_actionMapPoint_Photo_EditLabel);
            menuMgr.add(_actionSubMenu_SetTourMarkerType);
            menuMgr.add(_actionMapPoint_Photo_Tonality_Copy);
            menuMgr.add(_actionMapPoint_Photo_Tonality_Paste);
            menuMgr.add(_actionMapPoint_Photo_ReplaceGeoPosition);
            menuMgr.add(_actionMapPoint_Photo_RemoveFromTour);

            menuMgr.add(new Separator());

            fillExternalApp(menuMgr);
         }

         menuMgr.add(new Separator());

         menuMgr.add(_actionMapPoint_ZoomIn);
         menuMgr.add(_actionMapPoint_CenterMap);

         menuMgr.add(new Separator());

         menuMgr.add(_actionMapPoint_ShowOnlyThisTour);
         menuMgr.add(_actionMapPoint_EditTourMarker);

      } else {

         // open default context menu

         enableActions();

         menuMgr.add(_actionSearchTourByLocation);
         menuMgr.add(_actionCreateTourMarkerFromMap);
         menuMgr.add(_actionLookupTourLocation);
         menuMgr.add(_actionSetGeoPositionForPhotoTours);
         menuMgr.add(_actionSetGeoPositionForGeoMarker);

         /*
          * Show tour features
          */
         menuMgr.add(new Separator());
         menuMgr.add(_actionShowPOI);
         menuMgr.add(_actionShowStartEndInMap);
         if (isShowTrackColor_InContextMenu()) {
            menuMgr.add(_actionMap2Slideout_Color);
         }

         /*
          * Show map features
          */
         menuMgr.add(new Separator());
         menuMgr.add(_actionShowTourInfoInMap);
         menuMgr.add(_actionShowTourWeatherInMap);
         menuMgr.add(_actionShowLegendInMap);
         menuMgr.add(_actionShowScaleInMap);
         menuMgr.add(_actionShowValuePoint);
         menuMgr.add(_actionShowSliderInMap);
         menuMgr.add(_actionShowSliderInLegend);
         menuMgr.add(_actionZoom_ShowEntireMap);

         menuMgr.add(new Separator());

         MapBookmarkManager.fillContextMenu_RecentBookmarks(menuMgr, this);

         menuMgr.add(_actionGotoLocation);
         menuMgr.add(_actionCopyLocation);
         menuMgr.add(_actionSetDefaultPosition);
         menuMgr.add(_actionSaveDefaultPosition);

         menuMgr.add(new Separator());

         menuMgr.add(_actionExportMap_SubMenu);
         menuMgr.add(_actionZoomLevelAdjustment);

         menuMgr.add(new Separator());
         menuMgr.add(actionManageOfflineImages);
         menuMgr.add(_actionReloadFailedMapImages);
         menuMgr.add(_actionManageMapProvider);
      }
   }

   private void fillExternalApp(final IMenuManager menuMgr) {

      final Photo photo = _contextMenu_HoveredMapPoint.mapPoint.photo;

      // set image file name into the external app title/tooltip
      _actionRunExternalAppTitle.setText(Messages.Map_Action_ExternalApp_OpenPhotoImage.formatted(photo.imageFileName));
      _actionRunExternalAppTitle.setToolTipText(photo.imageFilePathName);

      menuMgr.add(_actionRunExternalAppTitle);

      fillExternalApp_One(1, _actionRunExternalApp1, menuMgr, IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_FILE_VIEWER_1);
      fillExternalApp_One(2, _actionRunExternalApp2, menuMgr, IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_FILE_VIEWER_2);
      fillExternalApp_One(3, _actionRunExternalApp3, menuMgr, IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_FILE_VIEWER_3);

      menuMgr.add(_actionRunExternalAppPrefPage);
   }

   private void fillExternalApp_One(final int appIndex,
                                    final ActionRunExternalApp appAction,
                                    final IMenuManager menuMgr,
                                    final String prefStoreName) {

      final String extAppFilePath = _prefStore_Photo.getString(prefStoreName).trim();

      if (extAppFilePath.length() > 0) {

         appAction.setText(EXTERNAL_APP_ACTION.formatted(appIndex, new Path(extAppFilePath).lastSegment()));

         // set tooltip text
         if (appIndex == 1) {

            appAction.setToolTipText(Messages.Map_Action_ExternalApp_DoubleClickStart.formatted(extAppFilePath));

         } else {

            appAction.setToolTipText(extAppFilePath);
         }

         menuMgr.add(appAction);
      }
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
            MapGraphId.Power,
            STATE_IS_SHOW_IN_TOOLBAR_POWER,
            STATE_IS_SHOW_IN_TOOLBAR_POWER_DEFAULT);

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

      if (mapGridData == null) {
         return;
      }

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

         _map.showGeoSearchGrid(tourGeoFilter);

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

      return _allSyncMapActions.get(syncId);
   }

   IAction getAction_TourColor(final MapGraphId graphId) {

      return _allTourColor_Actions.get(graphId);
   }

   /**
    * @return Returns a list with all tour ID's which are currently displayed
    */
   private List<Long> getAllTourIDs() {

      final List<Long> allTourIDs = new ArrayList<>();

      for (final TourData tourData : _allTourData) {
         allTourIDs.add(tourData.getTourId());
      }

      return allTourIDs;
   }

   /**
    * @param tourData
    *
    * @return Returns a list with all multiple tour ID's
    */
   private List<Long> getAllTourIDsFromMultipleTours(final TourData tourData) {

      List<Long> tourIds = new ArrayList<>();
      tourIds = Arrays.asList(tourData.multipleTourIds);

      return tourIds;
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

   public Long getHoveredTourId() {

      return _map.getHoveredTourId();
   }

   private TourPhoto getHoveredTourPhoto() {

      if (_contextMenu_HoveredMapPoint == null) {
         return null;
      }

      final Map2Point mapPoint = _contextMenu_HoveredMapPoint.mapPoint;

      if (mapPoint == null) {
         return null;
      }

      final Photo photo = mapPoint.photo;

      final List<TourPhoto> allTourPhotos = TourPhotoManager.getTourPhotos(photo);

      if (allTourPhotos.size() == 0) {
         return null;
      }

      return allTourPhotos.get(0);
   }

   public Map2 getMap() {
      return _map;
   }

   @Override
   public MapPosition getMapPosition() {

      final GeoPosition mapPosition = _map.getMapGeoCenter();
      final int mapZoomLevel = _map.getZoomLevel() - 1;

      return new MapPosition(
            mapPosition.latitude,
            mapPosition.longitude,
            Math.pow(2, mapZoomLevel));
   }

   @Override
   public Image getMapViewImage() {

      return MapUtils.getMapViewImage(_parent);
   }

   /**
    * Calculate lat/lon bounds for all photos.
    *
    * @param allPhotos
    *
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

   private Set<GeoPosition> getRefTourBounds(final ArrayList<TourData> allTourData) {

      final TourReference refTour = ReferenceTourManager.getGeoCompare_RefTour();

      if (refTour == null) {
         return null;
      }

      final TourData refTour_TourData = refTour.getTourData();
      if (refTour_TourData == null) {
         return null;
      }

      // find ref tour in provided tours
      final Long refTour_TourId = refTour_TourData.getTourId();
      TourData refTourInAllTourData = null;

      for (final TourData tourData : allTourData) {

         if (tourData.getTourId().equals(refTour_TourId)) {

            refTourInAllTourData = tourData;
            break;
         }
      }

      if (refTourInAllTourData == null) {
         return null;
      }

      // show ref/compare tour only when both are available
      if (allTourData.size() <= 1) {
         return null;
      }

      return refTourInAllTourData.computeGeo_Bounds(

            refTour.getStartValueIndex(),
            refTour.getEndValueIndex());
   }

   private Set<GeoPosition> getTourBounds(final ArrayList<TourData> allTourData) {

      /*
       * Get min/max for longitude/latitude
       */
      double allMinLatitude = Double.MIN_VALUE;
      double allMaxLatitude = 0;

      double allMinLongitude = 0;
      double allMaxLongitude = 0;

      for (final TourData tourData : allTourData) {

         final double[] latitudeSerie = tourData.latitudeSerie;
         final double[] longitudeSerie = tourData.longitudeSerie;

         if ((latitudeSerie == null) || (longitudeSerie == null)) {
            continue;
         }

         final GeoPosition[] geoBounds = tourData.getGeoBounds();

         if (geoBounds == null) {
            continue;
         }

         final double tourMinLatitude = geoBounds[0].latitude;
         final double tourMinLongitude = geoBounds[0].longitude;

         final double tourMaxLatitude = geoBounds[1].latitude;
         final double tourMaxLongitude = geoBounds[1].longitude;

         if (tourMinLatitude == 0
               && tourMaxLatitude == 0
               && tourMinLongitude == 0
               && tourMaxLongitude == 0) {

            continue;
         }

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

   /**
    * @return Returns {@link TourData} where the geo position can be set, otherwise
    *         <code>null</code>
    */
   public TourData getTourDataWhereGeoPositionsCanBeSet() {

      if (_lastTourWithoutLatLon != null) {

         // tour do not have geo positions

         return _lastTourWithoutLatLon;
      }

      if (_allTourData.size() == 1) {

         final TourData tourData = _allTourData.get(0);

         if (tourData.isPhotoTour()) {

            return tourData;
         }
      }

      return null;
   }

   /**
    *
    * @param allTourLocations
    *
    * @return Returns the center of the provided tour locations
    */
   private GeoPosition getTourLocationCenter(final List<TourLocation> allTourLocations) {

      double latitudeMin = 0;
      double latitudeMax = 0;
      double longitudeMin = 0;
      double longitudeMax = 0;

      boolean isFirst = true;

      // center map to the center of all location bounding boxes
      for (final TourLocation tourLocation : allTourLocations) {

         if (isFirst) {

            isFirst = false;

            latitudeMin = tourLocation.latitudeMin_Resized;
            latitudeMax = tourLocation.latitudeMax_Resized;

            longitudeMin = tourLocation.longitudeMin_Resized;
            longitudeMax = tourLocation.longitudeMax_Resized;
         }

         latitudeMin = Math.min(latitudeMin, tourLocation.latitudeMin_Resized);
         latitudeMax = Math.max(latitudeMax, tourLocation.latitudeMax_Resized);

         longitudeMin = Math.min(longitudeMin, tourLocation.longitudeMin_Resized);
         longitudeMax = Math.max(longitudeMax, tourLocation.longitudeMax_Resized);
      }

      final double latitudeCenter = latitudeMin + (latitudeMax - latitudeMin) / 2;
      final double longitudeCenter = longitudeMin + (longitudeMax - longitudeMin) / 2;

      return new GeoPosition(latitudeCenter, longitudeCenter);
   }

   /**
    * @return Returns {@link TourData} where the geo position can be set, otherwise
    *         <code>null</code>
    */
   private List<TourMarker> getTourMarkersWhereGeoPositionsCanBeSet() {

      if (_allTourData.size() != 1) {
         return null;
      }

      final TourData tourData = _allTourData.get(0);

      final double[] latitudeSerie = tourData.latitudeSerie;

      if (latitudeSerie == null || latitudeSerie.length == 0) {
         return null;
      }

      final String geoMarkerPrefix = ActionSetGeoPositionForGeoMarker.GEO_MARKER_PREFIX.toLowerCase();

      final List<TourMarker> allGeoMarker = new ArrayList<>();
      final List<TourMarker> allSortedTourMarkers = tourData.getTourMarkersSorted();

      for (final TourMarker tourMarker : allSortedTourMarkers) {

         final String label = tourMarker.getLabel();

         if (label.trim().toLowerCase().startsWith(geoMarkerPrefix)) {

            allGeoMarker.add(tourMarker);
         }
      }

      return allGeoMarker;
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

      _map.showGeoSearchGrid(null);
   }

   /**
    * @return Returns <code>true</code> when the map is dimmed to a specific level
    */
   boolean isBackgroundDark() {

      final boolean isMapDimmed = Util.getStateBoolean(_state, Map2View.STATE_IS_MAP_DIMMED, Map2View.STATE_IS_MAP_DIMMED_DEFAULT);
      final float mapDimValue = Util.getStateInt(_state, Map2View.STATE_DIM_MAP_VALUE, Map2View.STATE_DIM_MAP_VALUE_DEFAULT);

      final float dimLevelPercent = mapDimValue / MAX_DIM_STEPS * 100;

      // background is dark when dim level > 20 %
      final boolean isDark = isMapDimmed && dimLevelPercent > 20;

      return isDark;
   }

   private boolean isShowTrackColor_InContextMenu() {

      // it can happen that color id is not yet set
      if (_tourColorId != null) {

         // set color before menu is filled, this sets the action image and color id
         _actionMap2Slideout_Color.setColorId(_tourColorId);

         if (_tourColorId != MapGraphId.HrZone) {

            // hr zone has a different color provider and is not yet supported

            return true;
         }
      }

      return false;
   }

   private void keepMapPosition(final TourData tourData) {

      final GeoPosition centerPosition = _map.getMapGeoCenter();

      tourData.mapZoomLevel = _map.getZoomLevel();
      tourData.mapCenterPositionLatitude = centerPosition.latitude;
      tourData.mapCenterPositionLongitude = centerPosition.longitude;
   }

   private void mapListener_Breadcrumb() {

      // update the tour info icon depending if breadcrumbs are visible

      setIconPosition_TourInfo();
      setIconPosition_TourWeather();
   }

   private void mapListener_ControlResize(final ControlEvent event) {

      /*
       * Check if the legend size must be adjusted
       */
      final Image legendImage = _mapLegend.getImage();
      if ((legendImage == null) || legendImage.isDisposed()) {
         return;
      }

      if (_isTourPainted == false
            || _isShowTour == false
            || _isShowLegend == false) {

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

         createLegendImage(Map2PainterConfig.getMapColorProvider());
      }
   }

   private void mapListener_HoveredTour(final MapHoveredTourEvent mapHoveredTourEvent) {

      if (_actionShowValuePoint.isChecked()) {

         /*
          * Hide external value point, it can be irritating when the hovered value and the external
          * value point are displayed at the same time
          */
         _externalValuePointIndex = -1;

         // repaint map
         _directMappingPainter.setPaintingOptions(

               _isShowTour,
               _allTourData.get(0),

               _currentSliderValueIndex_Left,
               _currentSliderValueIndex_Right,
               _externalValuePointIndex,

               _actionShowSliderInMap.isChecked(),
               _actionShowSliderInLegend.isChecked(),
               _actionShowValuePoint.isChecked(),

               _sliderPathPaintingData);

         _map.redraw();
      }

      final Long hoveredTourId = mapHoveredTourEvent.hoveredTourId;
      final int hoveredValuePointIndex = mapHoveredTourEvent.hoveredValuePointIndex;

      // update value point tooltip
      if (hoveredValuePointIndex != -1) {

         final int mousePositionX = mapHoveredTourEvent.mousePositionX;
         final int mousePositionY = mapHoveredTourEvent.mousePositionY;

         final PointLong hoveredLinePosition = new PointLong(mousePositionX, mousePositionY);

         // when multiple tours are displayed then the tour data into the value point must be set
         final TourData tourData = TourManager.getTour(hoveredTourId);
         _valuePointTooltipUI.setTourData(tourData);

         _valuePointTooltipUI.setHoveredData(
               mousePositionX,
               mousePositionY,
               new HoveredValuePointData(hoveredValuePointIndex, hoveredLinePosition, -1));
      }

      final HoveredValueData hoveredValueData = new HoveredValueData(
            hoveredTourId,
            hoveredValuePointIndex);

      TourManager.fireEventWithCustomData(
            TourEventId.HOVERED_VALUE_POSITION,
            hoveredValueData,
            Map2View.this);
   }

   private void mapListener_InsideMap(final ISelection selection) {

      if (selection instanceof SelectionTourIds) {

         _lastSelectedTourInsideMap = null;

      } else if (selection instanceof final SelectionTourId selectionTourId) {

         /*
          * Update tour info tooltip
          */
         final Long tourId = selectionTourId.getTourId();
         final TourData tourData = TourManager.getInstance().getTourData(tourId);

         _tourInfoToolTipProvider.setTourData(tourData);
         _tourWeatherToolTipProvider.setTourData(tourData);

         /*
          * Show single tour only when it's selected the 2nd time
          */
         final boolean isSetBreadcrumbOnly =

               _lastSelectedTourInsideMap == null
                     || tourId.equals(_lastSelectedTourInsideMap) == false;

         selectionTourId.setIsSetBreadcrumbOnly(isSetBreadcrumbOnly);

         _lastSelectedTourInsideMap = tourId;
      }

      _map.getDisplay().asyncExec(() -> {

         onSelectionChanged(selection);
      });

      TourManager.fireEventWithCustomData(
            TourEventId.TOUR_SELECTION,
            selection,
            Map2View.this);
   }

   private void mapListener_MapGridBox(final int map_ZoomLevel,
                                       final GeoPosition map_GeoCenter,
                                       final boolean isGridSelected,
                                       final MapGridData mapGridData) {

      if (isGridSelected) {

         TourGeoFilter_Manager.createAndSetGeoFilter(map_ZoomLevel, map_GeoCenter, mapGridData);

      } else {

         geoFilter_10_Loader(mapGridData, null);
      }
   }

   private void mapListener_MapInfo(final GeoPosition mapCenter, final int mapZoomLevel) {

      _mapInfoManager.setMapPosition(mapCenter.latitude, mapCenter.longitude, mapZoomLevel);
   }

   private void mapListener_MapPosition(final GeoPosition geoCenter, final int zoomLevel, final boolean isZoomed) {

      if (_isInSelectBookmark) {

         // prevent fire the sync event
         return;
      }

      // fixed NPE when _map.getCenterMapBy() == null
      if (isZoomed && CenterMapBy.Tour.equals(_map.getCenterMapBy())) {
         centerTour();
      }

      if (_isInMapSync) {
         return;
      }

      _lastFiredMapSyncEventTime = System.currentTimeMillis();

      final MapPosition mapPosition = new MapPosition(
            geoCenter.latitude,
            geoCenter.longitude,
            Math.pow(2, zoomLevel - 1));

      MapManager.fireSyncMapEvent(mapPosition, this, null);
   }

   private void mapListener_MapSelection(final ISelection selection) {

      if (selection instanceof final SelectionMapSelection mapSelection) {

         final boolean isShowSliderInMap = _actionShowSliderInMap.isChecked();
         final boolean isShowValuePointInMap = _actionShowValuePoint.isChecked();

         if (isShowSliderInMap || isShowValuePointInMap) {

            /*
             * Set left/right slider position to the selected value points and/or hide external
             * value point
             */

            int valueIndex1 = mapSelection.getValueIndex1();
            int valueIndex2 = mapSelection.getValueIndex2();

            // ensure that the first index is set for the left slider
            if (valueIndex1 > valueIndex2) {

               final int valueIndex1Backup = valueIndex1;

               valueIndex1 = valueIndex2;
               valueIndex2 = valueIndex1Backup;
            }

            _currentSliderValueIndex_Left = valueIndex1;
            _currentSliderValueIndex_Right = valueIndex2;

            // hide the external value point
            _externalValuePointIndex = -1;

            // repaint map
            _directMappingPainter.setPaintingOptions(

                  _isShowTour,
                  _allTourData.get(0),

                  _currentSliderValueIndex_Left,
                  _currentSliderValueIndex_Right,
                  _externalValuePointIndex,

                  isShowSliderInMap,
                  _actionShowSliderInLegend.isChecked(),
                  _actionShowValuePoint.isChecked(),

                  _sliderPathPaintingData);

            _map.redraw();
         }

         TourManager.fireEventWithCustomData(
               TourEventId.MAP_SELECTION,
               selection,
               Map2View.this);
      }

   }

   private void mapListener_MousePosition(final MapGeoPositionEvent mapPositionEvent) {

      _mapInfoManager.setMapPosition(
            mapPositionEvent.mapGeoPosition.latitude,
            mapPositionEvent.mapGeoPosition.longitude,
            mapPositionEvent.mapZoomLevel);
   }

   private void mapListener_POI(final MapPOIEvent mapPoiEvent) {

      _poiPosition = mapPoiEvent.mapGeoPosition;
      _poiZoomLevel = mapPoiEvent.mapZoomLevel;
      _poiName = mapPoiEvent.mapPOIText;

      _actionShowPOI.setEnabled(true);
      _actionShowPOI.setChecked(true);
   }

   private void mapListener_RunExternalApp(final int numberOfExternalApp, final Photo photo) {

// SET_FORMATTING_OFF

      switch (numberOfExternalApp) {
      case 1:  actionExternalApp_Run(_actionRunExternalApp1, photo);  break;
      case 2:  actionExternalApp_Run(_actionRunExternalApp2, photo);  break;
      case 3:  actionExternalApp_Run(_actionRunExternalApp3, photo);  break;
      default: break;
      }

// SET_FORMATTING_ON

   }

   @SuppressWarnings("unchecked")
   private void moveToCommonLocation(final Object eventData) {

      List<TourLocation> allTourLocations = null;

      if (eventData instanceof final List allTourLocationsFromEvent) {
         allTourLocations = allTourLocationsFromEvent;

      }

      if (eventData == null || CollectionUtils.isEmpty(allTourLocations)) {

         // hide tour locations

         _map.setLocations_Common(null);

         return;
      }

      // repaint map
      final List<TourLocationExtended> allCommonLocations = new ArrayList<>();
      for (final TourLocation tourLocation : allTourLocations) {
         allCommonLocations.add(new TourLocationExtended(tourLocation, LocationType.Common));
      }

      _map.setLocations_Common(allTourLocations);

      if (_isMapSyncActive && _isMapSyncWith_MapLocation) {

         final GeoPosition geoPosition = getTourLocationCenter(allTourLocations);

         _map.setMapCenter(geoPosition);
      }
   }

   @Override
   public void moveToMapLocation(final MapBookmark mapBookmark) {

      _lastFiredMapSyncEventTime = System.currentTimeMillis();

      MapBookmarkManager.setLastSelectedBookmark(mapBookmark);

      final MapPosition mapPosition = mapBookmark.getMapPosition();

      _map.setZoom(mapPosition.zoomLevel + 1);
      _map.setMapCenter(new GeoPosition(mapPosition.getLatitude(), mapPosition.getLongitude()));
   }

   @SuppressWarnings("unchecked")
   private void moveToTourLocation(final Object eventData) {

      List<TourLocation> allTourLocations = null;

      if (eventData instanceof final List allTourLocationsFromEvent) {
         allTourLocations = allTourLocationsFromEvent;
      }

      if (eventData == null || CollectionUtils.isEmpty(allTourLocations)) {

         // hide tour locations

         _map.setLocations_Tours(null);

      } else {

         _map.setLocations_Tours(allTourLocations);

         if (_isMapSyncActive && _isMapSyncWith_MapLocation) {

            final GeoPosition geoPosition = getTourLocationCenter(allTourLocations);

            _map.setMapCenter(geoPosition);
         }
      }
   }

   @Override
   public void onMapBookmarkActionPerformed(final MapBookmark mapBookmark, final MapBookmarkEventType mapBookmarkEventType) {
      {
         if (mapBookmarkEventType == MapBookmarkEventType.MOVETO) {

            _isInSelectBookmark = true;
            {
               moveToMapLocation(mapBookmark);
            }
            _isInSelectBookmark = false;
         }
      }
   }

   private void onSelection_GeoComparedTour(final GeoComparedTour geoCompareTour) {

      final TourData refTourData = ReferenceTourManager.getGeoCompare_RefTour().getTourData();

      final long comparedTourId = geoCompareTour.tourId;
      final TourData comparedTourData = TourManager.getInstance().getTourData(comparedTourId);

      _allTourData.clear();

      _allTourData.add(refTourData);
      _allTourData.add(comparedTourData);
      _hash_AllTourData = _allTourData.hashCode();

      paintTours_10_All();

      positionMapTo_0_TourSliders(
            comparedTourData,
            geoCompareTour.tourFirstIndex,
            geoCompareTour.tourLastIndex,
            geoCompareTour.tourFirstIndex,
            null);
   }

   private void onSelection_HoveredValue(final HoveredValueData hoveredValueData) {

      _externalValuePointIndex = hoveredValueData.hoveredTourSerieIndex;

      updateUI_HoveredValuePoint();
   }

   private void onSelection_TourMarker(final SelectionTourMarker markerSelection, final boolean isDrawSlider) {

      final TourData tourData = markerSelection.getTourData();
      final ArrayList<TourMarker> allSelectedTourMarkers = markerSelection.getSelectedTourMarker();

      updateUI_ShowTour(tourData, allSelectedTourMarkers);

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

      if (_isMapSyncActive && _isMapSyncWith_Slider_One) {

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

   private void onSelection_TourPause(final SelectionTourPause pauseSelection, final boolean isDrawSlider) {

      final TourData tourData = pauseSelection.getTourData();

      updateUI_ShowTour(tourData, null);

      final int leftSliderValueIndex = pauseSelection.getSerieIndex();

      if (_isMapSyncActive && _isMapSyncWith_Slider_One) {

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

   private void onSelectionChanged(final ISelection selection) {

      if (_isPartVisible == false) {

         if (selection instanceof SelectionTourData
               || selection instanceof SelectionTourId
               || selection instanceof SelectionTourIds) {

            // keep only selected tours
            _selectionWhenHidden = selection;
         }

         return;
      }

      _lastTourWithoutLatLon = null;

      if (selection instanceof final SelectionTourData selectionTourData) {

         hideGeoGrid();

         final TourData tourData = selectionTourData.getTourData();

         paintToursAndPhotos(tourData, selection);

      } else if (selection instanceof final SelectionTourId tourIdSelection) {

         hideGeoGrid();

         if (tourIdSelection.isSetBreadcrumbOnly()) {

            // special case :-)

            _map.tourBreadcrumb().addBreadcrumTour(tourIdSelection.getTourId());
            setIconPosition_TourInfo();
            setIconPosition_TourWeather();

         } else {

            final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

            paintToursAndPhotos(tourData, selection);

            // recenter map AFTER it was centered in the paint... method
            if (_isMapSyncActive && _isMapSyncWith_MapLocation) {

               final GeoPosition hoveredTourLocation = tourIdSelection.getHoveredTourLocation();

               if (hoveredTourLocation != null) {
                  _map.setMapCenter(hoveredTourLocation);
               }
            }
         }

      } else if (selection instanceof final SelectionTourIds selectionTourIds) {

         // paint all selected tours

         hideGeoGrid();

         final ArrayList<Long> tourIds = selectionTourIds.getTourIds();
         if (tourIds.isEmpty()) {

            // history tour (without tours) is displayed

            // hide tours
            paintTours(tourIds);

            final ArrayList<Photo> allPhotos = paintPhotoSelection(selection);

            if (allPhotos.size() > 0) {

               showDefaultMap(true);

               enableActions();
            }

         } else if (tourIds.size() == 1) {

            // only 1 tour is displayed, synch with this tour !!!

            final TourData tourData = TourManager.getInstance().getTourData(tourIds.get(0));

            paintToursAndPhotos(tourData, selection);

         } else {

            // paint multiple tours

            paintTours(tourIds);
            paintPhotoSelection(selection);

            enableActions(true);
         }

      } else if (selection instanceof final SelectionChartInfo chartInfo) {

         TourData tourData = null;

         final Chart chart = chartInfo.getChart();
         if (chart instanceof final TourChart tourChart) {
            tourData = tourChart.getTourData();
         }

         if (tourData != null && tourData.isMultipleTours()) {

            // multiple tours are selected

         } else {

            // use old behavior

            final ChartDataModel chartDataModel = chartInfo.chartDataModel;
            if (chartDataModel != null) {

               final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
               if (tourId instanceof final Long tourIdLong) {

                  tourData = TourManager.getInstance().getTourData(tourIdLong);
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

      } else if (selection instanceof final SelectionChartXSliderPosition xSliderPos) {

         final Object customData = xSliderPos.getCustomData();
         if (customData instanceof final SelectedTourSegmenterSegments selectedTourSegmenterSegments) {

            /*
             * This event is fired in the tour chart when a toursegmenter segment is selected
             */

            selectTourSegments(selectedTourSegmenterSegments);

         } else {

            final Chart chart = xSliderPos.getChart();
            if (chart == null) {
               return;
            }

            final ChartDataModel chartDataModel = chart.getChartDataModel();
            final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);

            if (tourId instanceof final Long tourIdLong) {

               final TourData tourData = TourManager.getInstance().getTourData(tourIdLong);
               if (tourData != null) {

                  final int beforeLeftSliderIndex = xSliderPos.getBeforeLeftSliderIndex();
                  int leftSliderValueIndex = xSliderPos.getLeftSliderValueIndex();
                  int rightSliderValueIndex = xSliderPos.getRightSliderValueIndex();

                  /*
                   * These values are tested with the selection from the tour editor
                   */
                  if (beforeLeftSliderIndex != SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION) {

                     // one slice is selected

                     leftSliderValueIndex = beforeLeftSliderIndex;
                     rightSliderValueIndex = leftSliderValueIndex;
                  }

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

      } else if (selection instanceof final SelectionTourMarker markerSelection) {

         onSelection_TourMarker(markerSelection, true);

      } else if (selection instanceof final SelectionMapPosition mapPositionSelection) {

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

      } else if (selection instanceof final PointOfInterest poi) {

         _isTourPainted = false;

         clearView();

         _poiPosition = poi.getPosition();
         _poiName = poi.getName();

         final String boundingBox = poi.getBoundingBox();
         if (boundingBox == null) {
            _poiZoomLevel = _map.getZoomLevel();
         } else {
            _poiZoomLevel = _map.setZoomToBoundingBox(boundingBox);
         }

         if (_poiZoomLevel == -1) {
            _poiZoomLevel = _map.getZoomLevel();
         }

         _map.setPoi(_poiPosition, _poiZoomLevel, _poiName);

         _actionShowPOI.setChecked(true);

         enableActions();

      } else if (selection instanceof final StructuredSelection structuredSelection) {

         final Object firstElement = structuredSelection.getFirstElement();

         if (firstElement instanceof final TVIRefTour_ComparedTour comparedTour) {

            final GeoComparedTour geoCompareTour = comparedTour.getGeoCompareTour();

            if (geoCompareTour != null) {

               onSelection_GeoComparedTour(geoCompareTour);

            } else {

               final long tourId = comparedTour.getTourId();
               final TourData tourData = TourManager.getInstance().getTourData(tourId);

               paintTours_20_One(tourData, false);
            }

         } else if (firstElement instanceof final TVIElevationCompareResult_ComparedTour compareResultItem) {

            final TourData tourData = TourManager.getInstance().getTourData(compareResultItem.getTourId());

            paintTours_20_One(tourData, false);

         } else if (firstElement instanceof final GeoComparedTour geoCompareTour) {

            onSelection_GeoComparedTour(geoCompareTour);

         } else if (firstElement instanceof final TourWayPoint wp) {

            final TourData tourData = wp.getTourData();

            paintTours_20_One(tourData, false);

            // display wp in the center of the map which makes it also visible
            _map.setMapCenter(wp.getPosition());

            enableActions();
         }

         enableActions();

      } else if (selection instanceof final PhotoSelection photoSelection) {

         final ArrayList<Photo> allGalleryPhotos = photoSelection.galleryPhotos;

         TourData tourData = null;

         allPhotoLoop:

         // get first tour id
         for (final Photo photo : allGalleryPhotos) {

            for (final Long photoTourId : photo.getTourPhotoReferences().keySet()) {

               tourData = TourManager.getInstance().getTourData(photoTourId);

               break allPhotoLoop;
            }
         }

         if (tourData != null) {

            paintToursAndPhotos(tourData, selection);

         } else {

            paintPhotos(((PhotoSelection) selection).galleryPhotos);
         }

         enableActions();

      } else if (selection instanceof final SelectionReferenceTourView tourCatalogSelection) {

         // show reference tour

         final TVIRefTour_RefTourItem refItem = tourCatalogSelection.getRefItem();
         if (refItem != null) {

            final TourData tourData = TourManager.getInstance().getTourData(refItem.getTourId());

            paintTours_20_One(tourData, false);

            enableActions();
         }

      } else if (selection instanceof SelectionDeletedTours) {

         clearView();
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

      Map2PainterConfig.setTourData(_allTourData, _isShowTour);
      Map2PainterConfig.setPhotos(_filteredPhotos, _isShowPhoto, _isLinkPhotoDisplayed);

      _tourInfoToolTipProvider.setTourDataList(_allTourData);
      _tourWeatherToolTipProvider.setTourDataList(_allTourData);

      Set<GeoPosition> refTourBounds = getRefTourBounds(_allTourData);
      if (refTourBounds == null) {

         // use normal tour bounds
         refTourBounds = getTourBounds(_allTourData);
      }

      Map2PainterConfig.setTourBounds(refTourBounds);

      _directMappingPainter.disablePaintContext();

      _map.resetTours_HoveredData();
      _map.setShowOverlays(_isShowTour || _isShowPhoto);
      _map.setShowLegend(_isShowTour && _isShowLegend);

      if (_previousOverlayKey != newOverlayKey) {

         _previousOverlayKey = newOverlayKey;

         _map.setOverlayKey(Long.toString(newOverlayKey));
         _map.disposeOverlayImageCache();
      }

      positionMapTo_MapPosition(refTourBounds, false);

      createLegendImage(Map2PainterConfig.getMapColorProvider());

      _map.paint();
   }

   private void paintPhotos(final ArrayList<Photo> allNewPhotos) {

      /*
       * TESTING if a map redraw can be avoided, 15.6.2015
       */
// DISABLED BECAUSE PHOTOS ARE NOT ALWAYS DISPLAYED
      final int allNewPhotoHash = allNewPhotos.hashCode();
      if (allNewPhotoHash == _hash_AllPhotos) {
//         return;
      }

      _allPhotos.clear();
      _allPhotos.addAll(allNewPhotos);
      _hash_AllPhotos = _allPhotos.hashCode();

      runPhotoFilter();

      /**
       * It is possible that sync photo action is disabled but map can be synched with photos. This
       * occur when show photos are deactivated but the photo sync is still selected.
       * <p>
       * To reactivate photo sync, first photos must be set visible.
       */
      if (_isMapSyncActive && _isMapSyncWith_Photo) {
         centerPhotos(_filteredPhotos, false);
      }

      _map.setShowOverlays(_isShowTour || _isShowPhoto);
      _map.setOverlayKey(Integer.toString(_filteredPhotos.hashCode()));

      _map.disposeOverlayImageCache();

      _map.paint();
   }

   /**
    * @param selection
    *
    * @return Returns a list which contains all photos.
    */
   private ArrayList<Photo> paintPhotoSelection(final ISelection selection) {

      _isLinkPhotoDisplayed = false;

      final ArrayList<Photo> allPhotos = new ArrayList<>();

      if (selection instanceof final TourPhotoLinkSelection linkSelection) {

         _isLinkPhotoDisplayed = true;

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

   private void paintTours(final List<Long> allTourIds) {

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
         _map.tourBreadcrumb().setBreadcrumbTours(_allTourData);

         setIconPosition_TourInfo();
         setIconPosition_TourWeather();

         return;
      }

      _isTourPainted = true;

      // force single tour to be repainted
      _previousTourData = null;

      _directMappingPainter.disablePaintContext();

      _map.setShowOverlays(_isShowTour || _isShowPhoto);
      _map.setShowLegend(_isShowTour && _isShowLegend);
      _map.setIsMultipleTours(allTourIds.size() > 1);

      long newOverlayKey = _hash_TourOverlayKey;

      if (allTourIds.hashCode() != _hash_AllTourIds || _allTourData.hashCode() != _hash_AllTourData) {

         // tour data needs to be loaded

         final ArrayList<TourData> allLoadedTourData = new ArrayList<>();

         newOverlayKey = TourManager.loadTourData(allTourIds, allLoadedTourData, true);

         setTourData(allLoadedTourData);

         /*
          * Sort tours by date otherwise the chart value point, which is sorted by date, could show
          * the wrong tour -> complicated
          */
         Collections.sort(_allTourData);

         _hash_AllTourIds = allTourIds.hashCode();
         _hash_AllTourData = _allTourData.hashCode();
         _hash_TourOverlayKey = newOverlayKey;
      }

      Map2PainterConfig.setTourData(_allTourData, _isShowTour);
      Map2PainterConfig.setPhotos(_filteredPhotos, _isShowPhoto, _isLinkPhotoDisplayed);

      _tourInfoToolTipProvider.setTourDataList(_allTourData);
      _tourWeatherToolTipProvider.setTourDataList(_allTourData);

      _map.resetTours_HoveredData();
      _map.resetTours_SelectedData();

      if (_previousOverlayKey != newOverlayKey) {

         _previousOverlayKey = newOverlayKey;

         _map.setOverlayKey(Long.toString(newOverlayKey));
         _map.disposeOverlayImageCache();
      }

      if (_isMapSyncActive && _isMapSyncWith_Tour && _map.isSearchTourByLocation() == false) {

         // use default position for the tour

         final Set<GeoPosition> tourBounds = getTourBounds(_allTourData);

         positionMapTo_MapPosition(tourBounds, true);
      }

      createLegendImage(Map2PainterConfig.getMapColorProvider());

      _map.paint();
   }

   /**
    * Paint tours which are set in {@link #_allTourData}.
    */
   private void paintTours_10_All() {

      if (_allTourData.isEmpty()) {

         _tourInfoToolTipProvider.setTourData(null);
         _tourWeatherToolTipProvider.setTourData(null);

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
    */
   private void paintTours_20_One(final TourData tourData, final boolean forceRedraw) {

      _isTourPainted = true;

      // this can return the wrong tourdata when it is not reset !!!
      _lastTourWithoutLatLon = null;

      if (TourManager.isLatLonAvailable(tourData) == false) {

         _lastTourWithoutLatLon = tourData;

         showDefaultMap(false);

         return;
      }

      // prevent loading the same tour
      if (forceRedraw == false && (_allTourData.size() == 1) && (_allTourData.get(0) == tourData)) {

         /**
          * DISABLED
          * <p>
          * A reselected tour would not be visible
          */

//         return;
      }

      // force multiple tours to be repainted
      _previousOverlayKey = -1;

      // check if this is a new tour
      boolean isNewTour = true;
      if (_previousTourData != null
            && _previousTourData.getTourId().longValue() == tourData.getTourId().longValue()) {

         isNewTour = false;
      }

      Map2PainterConfig.setTourData(tourData, _isShowTour);

      /*
       * set tour into tour data list, this is currently used to draw the legend, it's also used to
       * figure out if multiple tours are selected
       */
      setTourData(tourData);
      _hash_AllTourData = _allTourData.hashCode();

      // reset also ALL tour id's, otherwise a reselected multiple tour is not displayed
      // it took some time to debug this issue !!!
      _hash_AllTourIds = tourData.getTourId().hashCode();

      _tourInfoToolTipProvider.setTourDataList(_allTourData);
      _tourWeatherToolTipProvider.setTourDataList(_allTourData);

      // set the paint context (slider position) for the direct mapping painter
      _directMappingPainter.setPaintingOptions(

            _isShowTour,
            tourData,

            _currentSliderValueIndex_Left,
            _currentSliderValueIndex_Right,
            _externalValuePointIndex,

            _actionShowSliderInMap.isChecked(),
            _actionShowSliderInLegend.isChecked(),
            _actionShowValuePoint.isChecked(),

            _sliderPathPaintingData);

      // set the tour bounds
      final GeoPosition[] tourBounds = tourData.getGeoBounds();

      final HashSet<GeoPosition> tourBoundsSet = new HashSet<>();
      tourBoundsSet.add(tourBounds[0]);
      tourBoundsSet.add(tourBounds[1]);

      Map2PainterConfig.setTourBounds(tourBoundsSet);

      _map.resetTours_HoveredData();
      _map.resetTours_SelectedData();
      _map.resetTours_Photos();

      _map.setShowOverlays(_isShowTour || _isShowPhoto);
      _map.setShowLegend(_isShowTour && _isShowLegend);
      _map.setIsMultipleTours(false);

      /*
       * Set position and zoom level for the tour
       */
      if (_isMapSyncActive) {

         if (_isMapSyncWith_Tour && _map.isSearchTourByLocation() == false) {

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
               _map.setMapCenter(new GeoPosition(
                     tourData.mapCenterPositionLatitude,
                     tourData.mapCenterPositionLongitude));
            }

         } else if (_isMapSyncWith_MapLocation) {

            // center map to tour locations

            final List<TourLocation> allTourLocations = new ArrayList<>();

            final TourLocation tourLocationStart = tourData.getTourLocationStart();
            final TourLocation tourLocationEnd = tourData.getTourLocationEnd();

            if (tourLocationStart != null) {
               allTourLocations.add(tourLocationStart);
            }

            if (tourLocationEnd != null) {
               allTourLocations.add(tourLocationEnd);
            }

            if (allTourLocations.size() > 0) {

               final GeoPosition geoPosition = getTourLocationCenter(allTourLocations);

               _map.setMapCenter(geoPosition);
            }

         } else if (isNewTour) {

            /**
             * !! Disabled !!
             * <p>
             * Because map is moved when any sync is disabled but there should be a
             * possibility that the map is NOT moved when a new tour is selected
             */

            // ensure that a new tour is visible
            // positionMapTo_MapPosition(tourBoundsSet, true);
         }
      }

      // keep tour data
      _previousTourData = tourData;

      if (isNewTour || forceRedraw) {

         // adjust legend values for the new or changed tour
         createLegendImage(Map2PainterConfig.getMapColorProvider());

         _map.setOverlayKey(tourData.getTourId().toString());
         _map.disposeOverlayImageCache();

      }

      _map.paint();
   }

   /**
    * paints the tours which are set in {@link #_allTourData}
    */
   private void paintTours_30_Multiple() {

      _isTourPainted = true;

      // force single tour to be repainted
      _previousTourData = null;

      Map2PainterConfig.setTourData(_allTourData, _isShowTour);
      Map2PainterConfig.setPhotos(_filteredPhotos, _isShowPhoto, _isLinkPhotoDisplayed);

      _tourInfoToolTipProvider.setTourDataList(_allTourData);
      _tourWeatherToolTipProvider.setTourDataList(_allTourData);

      _directMappingPainter.disablePaintContext();

      _map.resetTours_HoveredData();
      _map.setShowOverlays(_isShowTour || _isShowPhoto);
      _map.setShowLegend(_isShowTour && _isShowLegend);
      _map.setIsMultipleTours(_allTourData.size() > 1);

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

      createLegendImage(Map2PainterConfig.getMapColorProvider());

      _map.paint();
   }

   private void paintToursAndPhotos(final TourData tourData, final ISelection selection) {

      if (tourData == null) {
         return;
      }

      if (tourData.isMultipleTours()) {

         /*
          * Convert one multiple tour with it's sub-tours into many tours, this makes some
          * processings much easier
          */
         final List<Long> manyTourIDs = getAllTourIDsFromMultipleTours(tourData);

         paintTours(manyTourIDs);

      } else {

         paintTours_20_One(tourData, false);
      }

      paintPhotoSelection(selection);

      enableActions();
   }

   @Override
   public void photoEvent(final IViewPart viewPart, final PhotoEventId photoEventId, final Object data) {

      if (photoEventId == PhotoEventId.PHOTO_SELECTION) {

         if (data instanceof final TourPhotoLinkSelection tourPhotoLinkSelection) {

            onSelectionChanged(tourPhotoLinkSelection);

         } else if (data instanceof final PhotoSelection photoSelection) {

            onSelectionChanged(photoSelection);
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

      _isTourPainted = true;

      if (TourManager.isLatLonAvailable(tourData) == false) {
         showDefaultMap(_isShowPhoto);
         return;
      }

      _currentSliderValueIndex_Left = leftSliderValuesIndex;
      _currentSliderValueIndex_Right = rightSliderValuesIndex;
      _currentSliderValueIndex_Selected = selectedSliderIndex;

      _directMappingPainter.setPaintingOptions(

            _isShowTour,
            tourData,

            leftSliderValuesIndex,
            rightSliderValuesIndex,
            _externalValuePointIndex,

            _actionShowSliderInMap.isChecked(),
            _actionShowSliderInLegend.isChecked(),
            _actionShowValuePoint.isChecked(),

            _sliderPathPaintingData);

      if (_isMapSyncActive && _isMapSyncWith_Slider_One) {

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

               positionMapTo_ValueIndex(tourData, _currentSliderValueIndex_Selected);
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

      _map.setMapPosition(tourPositions, isAdjustZoomLevel, Map2PainterConfig.getZoomLevelAdjustment());
   }

   /**
    * Calculate the bounds for the tour in latitude and longitude values
    *
    * @param tourData
    * @param valueIndex
    *
    * @return
    */
   private void positionMapTo_ValueIndex(final TourData tourData, final int valueIndex) {

      if (tourData == null || tourData.latitudeSerie == null) {
         return;
      }

      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      final int sliderIndex = Math.max(0, Math.min(valueIndex, latitudeSerie.length - 1));

      final double latitude = latitudeSerie[sliderIndex];
      final double longitude = longitudeSerie[sliderIndex];

      // ignore lat/lon == 0, this occur when there are no geo data
      if (latitude != 0 && longitude != 0) {
         _map.setMapCenter(new GeoPosition(latitude, longitude));
      }
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
      _map.setIsShowTour(_isShowTour);

      // map points
      _isShowMapPoints = Util.getStateBoolean(_state, STATE_IS_SHOW_MAP_POINTS, true);
      _actionMap2Slideout_MapPoints.setSelection(_isShowMapPoints);
      _map.setShowMapPoint(_isShowMapPoints);

      // photo options
      _isShowPhoto = Util.getStateBoolean(_state, STATE_IS_SHOW_PHOTO_IN_MAP, true);
      _actionMap2Slideout_PhotoOptions.setSelection(_isShowPhoto);

      _isPhotoFilterActive = Util.getStateBoolean(_state, STATE_IS_PHOTO_FILTER_ACTIVE, false);
      _actionMap2Slideout_PhotoFilter.setSelection(_isPhotoFilterActive);

      _photoFilter_RatingStars = Util.getStateInt(_state, STATE_PHOTO_FILTER_RATING_STARS, 0);
      _photoFilter_RatingStar_Operator = Util.getStateEnum(_state, STATE_PHOTO_FILTER_RATING_STAR_OPERATOR, PhotoRatingStarOperator.HAS_ANY);
      _actionMap2Slideout_PhotoFilter.getPhotoFilterSlideout().restoreState(_photoFilter_RatingStars, _photoFilter_RatingStar_Operator);

      // is show legend
      _isShowLegend = Util.getStateBoolean(_state, STATE_IS_SHOW_LEGEND_IN_MAP, true);
      _actionShowLegendInMap.setChecked(_isShowLegend);

      _actionShowValuePoint.setChecked(Util.getStateBoolean(_state, STATE_IS_SHOW_VALUE_POINT, STATE_IS_SHOW_VALUE_POINT_DEFAULT));

      // center map by ...
      final CenterMapBy centerMapBy = (CenterMapBy) Util.getStateEnum(_state, STATE_CENTER_MAP_BY, STATE_CENTER_MAP_BY_DEFAULT);
      _map.setCenterMapBy(centerMapBy);
      _actionZoom_CenterMapBy.setCenterMode(centerMapBy);

      // synch map with ...
      _currentMapSyncMode = (MapSyncMode) Util.getStateEnum(_state, STATE_MAP_SYNC_MODE, MapSyncMode.IsSyncWith_Tour);
      _isMapSyncActive = _state.getBoolean(STATE_MAP_SYNC_MODE_IS_ACTIVE);
      _actionMap2Slideout_SyncMap.setSelection(_isMapSyncActive);
      syncMap_OnSelectSyncAction();
      if (_isMapSyncWith_Slider_One) {
         // enable sync tour also, sync tour is not reset when sync one slider is selected
         _isMapSyncWith_Tour = true;
      }

      // zoom level adjustment
      _actionZoomLevelAdjustment.setZoomLevel(Util.getStateInt(_state, STATE_ZOOM_LEVEL_ADJUSTMENT, 0));

      // show start/end in map
      _actionShowStartEndInMap.setChecked(_state.getBoolean(STATE_IS_SHOW_START_END_IN_MAP));
      Map2PainterConfig.isShowTourStartEnd = _actionShowStartEndInMap.isChecked();

      // show tour info in map
      final boolean isShowTourInfo = Util.getStateBoolean(_state, STATE_IS_SHOW_TOUR_INFO_IN_MAP, true);
      _actionShowTourInfoInMap.setChecked(isShowTourInfo);
      if (isShowTourInfo) {
         _tourToolTip.addToolTipProvider(_tourInfoToolTipProvider);
      }

      // show tour weather in map
      final boolean isShowTourWeather = Util.getStateBoolean(_state, STATE_IS_SHOW_TOUR_WEATHER_IN_MAP, true);
      _actionShowTourWeatherInMap.setChecked(isShowTourWeather);
      if (isShowTourWeather) {
         _tourToolTip.addToolTipProvider(_tourWeatherToolTipProvider);
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
      _actionMap2Slideout_MapProvider.selectMapProvider(_state.get(STATE_SELECTED_MAP_PROVIDER_ID));

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

         case Power:
            _actionTourColor_Power.setChecked(true);
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
      if (Map2PainterConfig.getMapColorProvider() == null) {

         // set default legend provider
         setTourPainterColorProvider(MapGraphId.Altitude);

         // hide legend
         _map.setShowLegend(false);
      }

      // debug info
      final boolean isShowGeoGrid = _prefStore.getBoolean(PREF_DEBUG_MAP_SHOW_GEO_GRID);
      final boolean isShowTileInfo = _prefStore.getBoolean(PREF_SHOW_TILE_INFO);
      final boolean isShowTileBorder = _prefStore.getBoolean(PREF_SHOW_TILE_BORDER);

      _map.setShowDebugInfo(isShowTileInfo, isShowTileBorder, isShowGeoGrid);
      updateState_Map2_TrackOptions(false);
      updateState_Map2_Options();

      // display the map with the default position
      actionSetDefaultPosition();
   }

   /**
    * Filter photos by rating stars.
    */
   private void runPhotoFilter() {

      _filteredPhotos.clear();

      final boolean hasAnyStars = _photoFilter_RatingStar_Operator == PhotoRatingStarOperator.HAS_ANY;

      if (_isPhotoFilterActive && hasAnyStars == false) {

// SET_FORMATTING_OFF

         final boolean isNoStar        = _photoFilter_RatingStars == 0;
         final boolean isEqual         = _photoFilter_RatingStar_Operator == PhotoRatingStarOperator.IS_EQUAL;
         final boolean isMore          = _photoFilter_RatingStar_Operator == PhotoRatingStarOperator.IS_MORE;
         final boolean isMoreOrEqual   = _photoFilter_RatingStar_Operator == PhotoRatingStarOperator.IS_MORE_OR_EQUAL;
         final boolean isMoreOrNone    = _photoFilter_RatingStar_Operator == PhotoRatingStarOperator.IS_MORE_OR_EQUAL_OR_NONE;
         final boolean isLess          = _photoFilter_RatingStar_Operator == PhotoRatingStarOperator.IS_LESS;
         final boolean isLessOrEqual   = _photoFilter_RatingStar_Operator == PhotoRatingStarOperator.IS_LESS_OR_EQUAL;

// SET_FORMATTING_ON

         for (final Photo photo : _allPhotos) {

            final int ratingStars = photo.ratingStars;

            if (isNoStar && ratingStars == 0) {

               // only photos without stars are displayed

               _filteredPhotos.add(photo);

            } else if (isEqual && ratingStars == _photoFilter_RatingStars) {

               _filteredPhotos.add(photo);

            } else if (isMore && ratingStars > _photoFilter_RatingStars) {

               _filteredPhotos.add(photo);

            } else if (isMoreOrEqual && ratingStars >= _photoFilter_RatingStars) {

               _filteredPhotos.add(photo);

            } else if (isMoreOrNone && (ratingStars >= _photoFilter_RatingStars || ratingStars == 0)) {

               _filteredPhotos.add(photo);

            } else if (isLess && ratingStars < _photoFilter_RatingStars) {

               _filteredPhotos.add(photo);

            } else if (isLessOrEqual && ratingStars <= _photoFilter_RatingStars) {

               _filteredPhotos.add(photo);
            }
         }

      } else {

         // photo filter is not active or any stars can be selected -> show all photos

         _filteredPhotos.addAll(_allPhotos);
      }

      Map2PainterConfig.setPhotos(_filteredPhotos, _isShowPhoto, _isLinkPhotoDisplayed);

      enableActions(true);

      // update UI: photo filter slideout
      _actionMap2Slideout_PhotoFilter.updateUI();
      _actionMap2Slideout_PhotoFilter.getPhotoFilterSlideout().updateUI_NumberOfPhotos();
   }

   @PersistState
   private void saveState() {

// SET_FORMATTING_OFF

      _state.put(STATE_IS_SHOW_TOUR_IN_MAP,                       _isShowTour);
      _state.put(STATE_IS_SHOW_MAP_POINTS,                        _isShowMapPoints);
      _state.put(STATE_IS_SHOW_PHOTO_IN_MAP,                      _isShowPhoto);
      _state.put(STATE_IS_SHOW_LEGEND_IN_MAP,                     _isShowLegend);

      _state.put(STATE_IS_SHOW_VALUE_POINT,                       _actionShowValuePoint.isChecked());
      _state.put(STATE_IS_SHOW_START_END_IN_MAP,                  _actionShowStartEndInMap.isChecked());
      _state.put(STATE_IS_SHOW_SCALE_IN_MAP,                      _actionShowScaleInMap.isChecked());
      _state.put(STATE_IS_SHOW_SLIDER_IN_MAP,                     _actionShowSliderInMap.isChecked());
      _state.put(STATE_IS_SHOW_SLIDER_IN_LEGEND,                  _actionShowSliderInLegend.isChecked());
      _state.put(STATE_IS_SHOW_TOUR_INFO_IN_MAP,                  _actionShowTourInfoInMap.isChecked());
      _state.put(STATE_IS_SHOW_TOUR_WEATHER_IN_MAP,               _actionShowTourWeatherInMap.isChecked());

      Util.setStateEnum(_state, STATE_CENTER_MAP_BY,              _map.getCenterMapBy());

      _state.put(STATE_MAP_SYNC_MODE_IS_ACTIVE,                   _isMapSyncActive);
      Util.setStateEnum(_state, STATE_MAP_SYNC_MODE,              _currentMapSyncMode);

      _state.put(STATE_ZOOM_LEVEL_ADJUSTMENT,                     _actionZoomLevelAdjustment.getZoomLevel());

      final MP selectedMapProvider = _actionMap2Slideout_MapProvider.getSelectedMapProvider();
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
      _state.put(STATE_IS_PHOTO_FILTER_ACTIVE, _actionMap2Slideout_PhotoFilter.getSelection());
      _state.put(STATE_PHOTO_FILTER_RATING_STARS, _photoFilter_RatingStars);
      Util.setStateEnum(_state, STATE_PHOTO_FILTER_RATING_STAR_OPERATOR, _photoFilter_RatingStar_Operator);
      _actionMap2Slideout_PhotoFilter.getPhotoFilterSlideout().saveState();

      /*
       * Tour color
       */
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

      // value point tooltip
      _valuePointTooltipUI.saveState();

      Map2ConfigManager.saveState();
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

   /**
    * Adjust tour info tooltip according to the breadcrumb toolbar visibility
    */
   private void setIconPosition_TourInfo() {

      final int devXTooltip = TOUR_INFO_TOOLTIP_X;
      final int devYTooltip =

            Map2PainterConfig.isShowBreadcrumbs && _map.tourBreadcrumb().getUsedCrumbs() > 0

                  // show tooltip icon below the crumbs
                  ? TOUR_INFO_TOOLTIP_Y

                  // breadcrumb is not visible -> "center" icon in the top left corner
                  : TOUR_INFO_TOOLTIP_X;

      _tourInfoToolTipProvider.setIconPosition(devXTooltip, devYTooltip);
   }

   /**
    * Adjust tour weather icon according to the breadcrumb toolbar visibility
    */
   private void setIconPosition_TourWeather() {

      final int devXTooltip = TOUR_WEATHER_TOOLTIP_X;
      final int devYTooltip =

            Map2PainterConfig.isShowBreadcrumbs && _map.tourBreadcrumb().getUsedCrumbs() > 0

                  // show tooltip icon below the crumbs
                  ? TOUR_WEATHER_TOOLTIP_Y

                  // breadcrumb is not visible -> "center" icon in the top left corner
                  : TOUR_INFO_TOOLTIP_X;

      _tourWeatherToolTipProvider.setIconPosition(devXTooltip, devYTooltip);
   }

   private void setMapImageSize() {

      final Enum<MapImageSize> imageSize = Util.getStateEnum(_state,
            SlideoutMap2_PhotoOptions.STATE_PHOTO_IMAGE_SIZE,
            MapImageSize.MEDIUM);

      int mapImageSize;

      if (imageSize.equals(MapImageSize.LARGE)) {

         mapImageSize = Util.getStateInt(_state,
               SlideoutMap2_PhotoOptions.STATE_PHOTO_IMAGE_SIZE_LARGE,
               _map.MAP_IMAGE_DEFAULT_SIZE_LARGE);

      } else if (imageSize.equals(MapImageSize.MEDIUM)) {

         mapImageSize = Util.getStateInt(_state,
               SlideoutMap2_PhotoOptions.STATE_PHOTO_IMAGE_SIZE_MEDIUM,
               _map.MAP_IMAGE_DEFAULT_SIZE_MEDIUM);

      } else if (imageSize.equals(MapImageSize.SMALL)) {

         mapImageSize = Util.getStateInt(_state,
               SlideoutMap2_PhotoOptions.STATE_PHOTO_IMAGE_SIZE_SMALL,
               _map.MAP_IMAGE_DEFAULT_SIZE_SMALL);

      } else {

         mapImageSize = Util.getStateInt(_state,
               SlideoutMap2_PhotoOptions.STATE_PHOTO_IMAGE_SIZE_TINY,
               _map.MAP_IMAGE_DEFAULT_SIZE_TINY);
      }

      Photo.setMap2ImageRequestedSize(mapImageSize);
   }

   void setShowPhotos(final boolean isShowPhotos) {

      _actionMap2Slideout_PhotoOptions.setSelection(isShowPhotos);

      actionShowPhotos(isShowPhotos);
   }

   /**
    * Set tour data for the map, this is THE central point to set new tours into the map.
    *
    * @param allTourData
    */
   private void setTourData(final List<TourData> allTourData) {

      _allTourData.clear();
      _allTourData.addAll(allTourData);

      for (final TourData tourData : allTourData) {
         setVisibleDataPoints(tourData);
      }

      _map.tourBreadcrumb().addBreadcrumTours(allTourData);

      // the value point tooltip do not support multiple tours
      _valuePointTooltipUI.setTourData(null);

      // update tour id's in the map
      final List<Long> allTourIds = new ArrayList<>();
      for (final TourData tourData : allTourData) {

         if (tourData == null) {
            continue;
         }

         allTourIds.add(tourData.getTourId());
      }
      _map.setTourIds(allTourIds);

      setTourData_Common();
   }

   /**
    * Set tour data for the map, this is THE central point to set new tours into the map.
    *
    * @param tourData
    */
   private void setTourData(final TourData tourData) {

      _allTourData.clear();
      _allTourData.add(tourData);

      setVisibleDataPoints(tourData);

      final Long tourId = tourData.getTourId();

      _map.tourBreadcrumb().addBreadcrumTour(tourId);

      _valuePointTooltipUI.setTourData(tourData);

      // update tour id's in the map
      _map.setTourIds(Arrays.asList(tourId));

      setTourData_Common();
   }

   private void setTourData_Common() {

      // with new tours these values are invalid
//      _currentSliderValueIndex_Left = -1;
//      _currentSliderValueIndex_Right = -1;
//      _currentSliderValueIndex_Selected = -1;

      // this must be set AFTER the breadcrumbs are set
      setIconPosition_TourInfo();
      setIconPosition_TourWeather();
   }

   private void setTourPainterColorProvider(final MapGraphId colorId) {

      _tourColorId = colorId;

      final IMapColorProvider mapColorProvider = getColorProvider(colorId);

      Map2PainterConfig.setMapColorProvider(mapColorProvider);
   }

   void setupMapDimLevel() {

      _map.setTransparencyColor(Util.getStateRGB(_state, STATE_MAP_TRANSPARENCY_COLOR, STATE_MAP_TRANSPARENCY_COLOR_DEFAULT));

// SET_FORMATTING_OFF

      final boolean  isMapDimmed       = Util.getStateBoolean( _state, STATE_IS_MAP_DIMMED,                       STATE_IS_MAP_DIMMED_DEFAULT);
      final boolean  isUseMapDimColor  = Util.getStateBoolean( _state, STATE_MAP_TRANSPARENCY_USE_MAP_DIM_COLOR,  STATE_MAP_TRANSPARENCY_USE_MAP_DIM_COLOR_DEFAULT);
      final int      mapDimValue       = Util.getStateInt(     _state, STATE_DIM_MAP_VALUE,                       STATE_DIM_MAP_VALUE_DEFAULT);
      final RGB      mapDimColor       = Util.getStateRGB(     _state, STATE_DIM_MAP_COLOR,                       STATE_DIM_MAP_COLOR_DEFAULT);

// SET_FORMATTING_ON

      final boolean isBackgroundDark = isBackgroundDark();

      _map.setDimLevel(isMapDimmed, mapDimValue, mapDimColor, isUseMapDimColor, isBackgroundDark);

      // update legend image after the dim level is modified
      createLegendImage(Map2PainterConfig.getMapColorProvider());
   }

   private void setVisibleDataPoints(final TourData tourData) {

      if (tourData == null) {
         return;
      }

      if (_map.isCutOffLinesInPauses() == false) {

         // all lines are visible -> reset visible points

         tourData.visibleDataPointSerie = null;

         return;
      }

      final int[] timeSerie = tourData.timeSerie;
      if (timeSerie == null) {
         return;
      }

      /*
       * Cut off lines within a pause -> set visible and hidden points
       */
      final boolean[] breakTimeSerie = tourData.getBreakTimeSerie();
      final boolean isBreakTimeAvailable = breakTimeSerie != null;

      final int numSlices = timeSerie.length;

      tourData.visibleDataPointSerie = isBreakTimeAvailable
            ? new boolean[numSlices]
            : null;

      if (isBreakTimeAvailable) {

         final boolean[] visibleDataPointSerie = tourData.visibleDataPointSerie;

         for (int timeIndex = 0; timeIndex < breakTimeSerie.length; timeIndex++) {

            final boolean isBreakTime = breakTimeSerie[timeIndex];

            if (isBreakTime == false) {
               visibleDataPointSerie[timeIndex] = true;
            }
         }
      }
   }

   /**
    * Show map by removing/resetting all previously displayed tours
    *
    * @param isShowOverlays
    */
   private void showDefaultMap(final boolean isShowOverlays) {

      _tourInfoToolTipProvider.setTourData(null);
      _tourWeatherToolTipProvider.setTourData(null);

      _valuePointTooltipUI.setTourData(null);

      // disable tour actions in this view
      _isTourPainted = false;

      // disable tour data
      _allTourData.clear();
      _previousTourData = null;
      Map2PainterConfig.resetTourData();

      // update direct painter to draw nothing
      _directMappingPainter.setPaintingOptions(

            false,
            null,

            0,
            0,
            0,

            false,
            false,
            false, // show value point

            _sliderPathPaintingData);

      _map.resetTours_HoveredData();

      _map.tourBreadcrumb().removeAllCrumbs();
      setIconPosition_TourInfo();
      setIconPosition_TourWeather();

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

         final ArrayList<TourData> allSelectedTours = TourManager.getSelectedTours();
         if (allSelectedTours != null) {

            setTourData(allSelectedTours);

            _hash_AllTourData = _allTourData.hashCode();

            paintTours_10_All();
         }
      });
   }

   private void syncMap_OnSelectSyncAction() {

      if (_isMapSyncActive) {

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

         case IsSyncWith_TourLocation:
            _actionSyncMapWith_TourLocation.setChecked(true);
            action_SyncWith_TourLocation();
            break;

         case IsSyncWith_ValuePoint:
            _actionSyncMapWith_ValuePoint.setChecked(true);
            action_SyncWith_ValuePoint();
            break;

         case IsSyncWith_Photo:
            _actionSyncMapWith_Photo.setChecked(true);
            action_SyncWith_Photo();
            break;

         default:
            break;
         }

      } else {

         // sync action is not selected

         deactivateOtherSync(null);
      }
   }

   /**
    * Set sync map action selected when one of it's subactions are selected.
    *
    * @param isSelected
    *
    * @param isSelectSyncMap
    */
   private void syncMap_UpdateSyncSlideoutAction(final boolean isSelected) {

      _isMapSyncActive = isSelected;
      _actionMap2Slideout_SyncMap.setSelection(isSelected);

// SET_FORMATTING_OFF

      switch (_currentMapSyncMode) {

      case IsSyncWith_Tour:            _actionMap2Slideout_SyncMap.showOtherEnabledImage(0);    break;
      case IsSyncWith_Slider_One:      _actionMap2Slideout_SyncMap.showOtherEnabledImage(1);    break;
      case IsSyncWith_Slider_Center:   _actionMap2Slideout_SyncMap.showOtherEnabledImage(2);    break;
      case IsSyncWith_ValuePoint:      _actionMap2Slideout_SyncMap.showOtherEnabledImage(3);    break;
      case IsSyncWith_OtherMap:        _actionMap2Slideout_SyncMap.showOtherEnabledImage(4);    break;
      case IsSyncWith_Photo:           _actionMap2Slideout_SyncMap.showOtherEnabledImage(5);    break;
      case IsSyncWith_TourLocation:    _actionMap2Slideout_SyncMap.showOtherEnabledImage(6);    break;

      default:
         break;
      }

// SET_FORMATTING_ON
   }

   @Override
   public void syncMapWithOtherMap(final MapPosition mapPosition,
                                   final ViewPart viewPart,
                                   final IMapSyncListener.SyncParameter syncParameter) {

      if (_isMapSyncActive || _isMapSyncWith_OtherMap == false) {

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

      final long timeDiff = System.currentTimeMillis() - _lastFiredMapSyncEventTime;

      if (timeDiff < 1000) {
         // ignore because it causes LOTS of problems when synching moved map
         return;
      }

      final Runnable runnable = new Runnable() {

         final int __asynchRunnableCounter = _asyncCounter.incrementAndGet();

         @Override
         public void run() {

            if (_map.isDisposed()) {
               return;
            }

            // check if a newer runnable is available
            if (__asynchRunnableCounter != _asyncCounter.get()) {

               // a newer runnable is available
               return;
            }

            _isInMapSync = true;
            {
               final int zoomLevel = mapPosition.zoomLevel;
               final int mapZoomLevel = zoomLevel == ModelPlayerManager.MAP_ZOOM_LEVEL_IS_NOT_AVAILABLE

                     // use current zoom
                     ? _map.getZoomLevel()

                     // use provided zoom
                     : zoomLevel + 1;

               _map.setZoom(mapZoomLevel);
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

   public void updateState_Map2_Options() {

// SET_FORMATTING_OFF

      /*
       * Hovered/selected tour
       */
      final boolean isShowHoveredOrSelectedTour = Util.getStateBoolean(_state,   STATE_IS_SHOW_HOVERED_SELECTED_TOUR,                     STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT);
      final boolean isShowBreadcrumbs           = Util.getStateBoolean(_state,   STATE_IS_SHOW_BREADCRUMBS,                               STATE_IS_SHOW_BREADCRUMBS_DEFAULT);

      final int numVisibleBreadcrumbs           = Util.getStateInt(_state,       STATE_VISIBLE_BREADCRUMBS,                               STATE_VISIBLE_BREADCRUMBS_DEFAULT);
      final int hoveredOpacity                  = Util.getStateInt(_state,       STATE_HOVERED_SELECTED__HOVERED_OPACITY,                 STATE_HOVERED_SELECTED__HOVERED_OPACITY_DEFAULT);
      final int hoveredAndSelectedOpacity       = Util.getStateInt(_state,       STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_OPACITY,    STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_OPACITY_DEFAULT);
      final int selectedOpacity                 = Util.getStateInt(_state,       STATE_HOVERED_SELECTED__SELECTED_OPACITY,                STATE_HOVERED_SELECTED__SELECTED_OPACITY_DEFAULT);
      final RGB hoveredRGB                      = Util.getStateRGB(_state,       STATE_HOVERED_SELECTED__HOVERED_RGB,                     STATE_HOVERED_SELECTED__HOVERED_RGB_DEFAULT);
      final RGB hoveredAndSelectedRGB           = Util.getStateRGB(_state,       STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_RGB,        STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_RGB_DEFAULT);
      final RGB selectedRGB                     = Util.getStateRGB(_state,       STATE_HOVERED_SELECTED__SELECTED_RGB,                    STATE_HOVERED_SELECTED__SELECTED_RGB_DEFAULT);

      _map.setConfig_HoveredSelectedTour(

            isShowHoveredOrSelectedTour,
            isShowBreadcrumbs,

            numVisibleBreadcrumbs,
            hoveredRGB,
            hoveredOpacity,
            hoveredAndSelectedRGB,
            hoveredAndSelectedOpacity,
            selectedRGB,
            selectedOpacity
      );


      Map2PainterConfig.isShowBreadcrumbs   = isShowBreadcrumbs;

      /*
       * Tour direction
       */
      final boolean isShowTourDirection         = Util.getStateBoolean(_state,      STATE_IS_SHOW_TOUR_DIRECTION,          STATE_IS_SHOW_TOUR_DIRECTION_DEFAULT);
      final boolean isShowTourDirection_Always  = Util.getStateBoolean(_state,      STATE_IS_SHOW_TOUR_DIRECTION_ALWAYS,   STATE_IS_SHOW_TOUR_DIRECTION_ALWAYS_DEFAULT);
      final int tourDirection_MarkerGap         = Util.getStateInt(_state,          STATE_TOUR_DIRECTION_MARKER_GAP,       STATE_TOUR_DIRECTION_MARKER_GAP_DEFAULT);
      final int tourDirection_LineWidth         = Util.getStateInt(_state,          STATE_TOUR_DIRECTION_LINE_WIDTH,       STATE_TOUR_DIRECTION_LINE_WIDTH_DEFAULT);
      final float tourDirection_SymbolSize      = Util.getStateInt(_state,          STATE_TOUR_DIRECTION_SYMBOL_SIZE,      STATE_TOUR_DIRECTION_SYMBOL_SIZE_DEFAULT);
      final RGB tourDirection_RGB               = Util.getStateRGB(_state,          STATE_TOUR_DIRECTION_RGB,              STATE_TOUR_DIRECTION_RGB_DEFAULT);

// SET_FORMATTING_ON

      _map.setConfig_TourDirection(
            isShowTourDirection,
            isShowTourDirection_Always,
            tourDirection_MarkerGap,
            tourDirection_LineWidth,
            tourDirection_SymbolSize,
            tourDirection_RGB);

      _map.setIsInInverseKeyboardPanning(Util.getStateBoolean(_state, STATE_IS_TOGGLE_KEYBOARD_PANNING, STATE_IS_TOGGLE_KEYBOARD_PANNING_DEFAULT));

      /*
       * Set dim level/color after the map providers are set
       */
      setupMapDimLevel();

      /*
       * Painting
       */
      final boolean isBackgroundDark = isBackgroundDark();
      Map2PainterConfig.isBackgroundDark = isBackgroundDark;

      // enable/disable cluster/marker tooltip
      final Map2Config mapConfig = Map2ConfigManager.getActiveConfig();

      final boolean isShowTooltip = false
            || mapConfig.isShowCommonLocation
            || mapConfig.isShowTourLocation
            || mapConfig.isShowTourMarker
            || mapConfig.isShowTourPauses;

      if (isShowTooltip) {
         _map.getMapPointTooltip().activate();
      } else {
         _map.getMapPointTooltip().deactivate();
      }

      setIconPosition_TourInfo();
      setIconPosition_TourWeather();

      _map.resetMapPoints();

      _map.paint();
   }

   void updateState_Map2_TrackOptions(final boolean isUpdateMapUI) {

// SET_FORMATTING_OFF

      _actionShowSliderInMap.setChecked(          Util.getStateBoolean(_state,   STATE_IS_SHOW_SLIDER_IN_MAP,  STATE_IS_SHOW_SLIDER_IN_MAP_DEFAULT));

      _sliderPathPaintingData = new SliderPathPaintingData();

      _sliderPathPaintingData.isShowSliderPath  = Util.getStateBoolean(_state,   STATE_IS_SHOW_SLIDER_PATH,    STATE_IS_SHOW_SLIDER_PATH_DEFAULT);
      _sliderPathPaintingData.opacity           = Util.getStateInt(_state,       STATE_SLIDER_PATH_OPACITY,    STATE_SLIDER_PATH_OPACITY_DEFAULT);
      _sliderPathPaintingData.segments          = Util.getStateInt(_state,       STATE_SLIDER_PATH_SEGMENTS,   STATE_SLIDER_PATH_SEGMENTS_DEFAULT);
      _sliderPathPaintingData.lineWidth         = Util.getStateInt(_state,       STATE_SLIDER_PATH_LINE_WIDTH, STATE_SLIDER_PATH_LINE_WIDTH_DEFAULT);
      _sliderPathPaintingData.color             = Util.getStateRGB(_state,       STATE_SLIDER_PATH_COLOR,      STATE_SLIDER_PATH_COLOR_DEFAULT);

// SET_FORMATTING_ON

      if (isUpdateMapUI) {

         // update map UI
         actionShowSlider();
      }
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

         hoveredSerieIndex = _externalValuePointIndex;
         hoveredTourData = _allTourData.get(0);

      } else {

         /**
          * Discovered issue:
          * <p>
          * When multiple tours are selected in the tourbook view, _allTourData contains multiple
          * tours. However when tour chart is selected and it contains multiple tours, then one
          * TourData with isMultipleTour is displayed in the map -> complicated
          */

         int adjustedValuePointIndex = _externalValuePointIndex;

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
      _directMappingPainter.setPaintingOptions(

            _isShowTour,
            hoveredTourData,

            _currentSliderValueIndex_Left,
            _currentSliderValueIndex_Right,
            hoveredSerieIndex,

            _actionShowSliderInMap.isChecked(),
            _actionShowSliderInLegend.isChecked(),
            _actionShowValuePoint.isChecked(),

            _sliderPathPaintingData);

      _map.paint();

      if (_isMapSyncActive && _isMapSyncWith_ValuePoint) {
         positionMapTo_ValueIndex(hoveredTourData, hoveredSerieIndex);
      }
   }

   /**
    * Show tour when it is not yet displayed
    *
    * @param tourData
    * @param allSelectedTourMarkers
    */
   private void updateUI_ShowTour(final TourData tourData, final ArrayList<TourMarker> allSelectedTourMarkers) {

      boolean isTourVisible = false;

      // check if the marker's tour is displayed

      if (tourData.isMultipleTours()
            && allSelectedTourMarkers != null
            && allSelectedTourMarkers.size() == 1) {

         final long selectedMarkerTourId = allSelectedTourMarkers.get(0).getTourData().getTourId().longValue();

         final List<Long> allMultipleTourIDs = getAllTourIDsFromMultipleTours(tourData);

         for (final Long tourID : allMultipleTourIDs) {

            if (tourID.longValue() == selectedMarkerTourId) {

               isTourVisible = true;
               break;
            }
         }

      } else {

         // single tour

         final long markerTourId = tourData.getTourId().longValue();

         for (final TourData mapTourData : _allTourData) {
            if (mapTourData.getTourId().longValue() == markerTourId) {

               isTourVisible = true;
               break;
            }
         }
      }

      if (isTourVisible == false) {

         // tour is not yet visible, show it now

         setTourData(tourData);

         _hash_AllTourData = _allTourData.hashCode();
         _hash_AllTourIds = tourData.getTourId().hashCode();

         paintTours_10_All();
      }
   }

}
