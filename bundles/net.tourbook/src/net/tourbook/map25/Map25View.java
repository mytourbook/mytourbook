/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
 *
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
package net.tourbook.map25;

import gnu.trove.list.array.TIntArrayList;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ICloseOpenedDialogs;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.SWTPopupOverAWT;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.map.IMapSyncListener;
import net.tourbook.map.MapInfoManager;
import net.tourbook.map.MapManager;
import net.tourbook.map.bookmark.ActionMapBookmarks;
import net.tourbook.map.bookmark.IMapBookmarkListener;
import net.tourbook.map.bookmark.IMapBookmarks;
import net.tourbook.map.bookmark.MapBookmark;
import net.tourbook.map.bookmark.MapBookmarkManager;
import net.tourbook.map25.action.ActionMap25_PhotoFilter;
import net.tourbook.map25.action.ActionMap25_ShowMarker;
import net.tourbook.map25.action.ActionShowEntireTour;
import net.tourbook.map25.action.ActionSyncMap2WithOtherMap;
import net.tourbook.map25.action.ActionSynchMapWithChartSlider;
import net.tourbook.map25.action.ActionSynchMapWithTour;
import net.tourbook.map25.action.ActionZoomIn;
import net.tourbook.map25.action.ActionZoomOut;
import net.tourbook.map25.layer.marker.MapMarker;
import net.tourbook.map25.layer.marker.MarkerLayerMT;
import net.tourbook.map25.layer.tourtrack.Map25TrackConfig;
import net.tourbook.map25.layer.tourtrack.SliderLocation_Layer;
import net.tourbook.map25.layer.tourtrack.SliderPath_Layer;
import net.tourbook.map25.layer.tourtrack.TourLayer;
import net.tourbook.map25.ui.SlideoutMap25_MapLayer;
import net.tourbook.map25.ui.SlideoutMap25_MapOptions;
import net.tourbook.map25.ui.SlideoutMap25_MapProvider;
import net.tourbook.map25.ui.SlideoutMap25_PhotoOptions;
import net.tourbook.map25.ui.SlideoutMap25_TrackOptions;
import net.tourbook.photo.IPhotoEventListener;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoEventId;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.PhotoRatingStarOperator;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.photo.IMapWithPhotos;
import net.tourbook.tour.photo.TourPhotoLink;
import net.tourbook.tour.photo.TourPhotoLinkSelection;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.map.Animator;
import org.oscim.map.Map;
import org.oscim.utils.animation.Easing;

public class Map25View extends ViewPart implements
      IMapBookmarks,
      ICloseOpenedDialogs,
      IMapBookmarkListener,
      IMapSyncListener,
      IMapWithPhotos,
      IPhotoEventListener {

// SET_FORMATTING_OFF

   private static final String            MAP_ACTION_SHOW_TOUR_IN_MAP                  = net.tourbook.map2.Messages.map_action_show_tour_in_map;
   private static final String            MAP_ACTION_SYNCH_WITH_SLIDER                 = net.tourbook.map2.Messages.map_action_synch_with_slider;
   private static final String            MAP_ACTION_SYNCH_WITH_SLIDER_CENTERED        = net.tourbook.map2.Messages.Map_Action_SynchWithSlider_Centered;

   private static final ImageDescriptor   _imageSyncWithSlider                         = TourbookPlugin.getThemedImageDescriptor(Images.SyncWith_Slider);
   private static final ImageDescriptor   _imageSyncWithSlider_Disabled                = TourbookPlugin.getThemedImageDescriptor(Images.SyncWith_Slider_Disabled);
   private static final ImageDescriptor   _imageSyncWithSlider_Centered                = TourbookPlugin.getThemedImageDescriptor(Images.SyncWith_Slider_Centered);
   private static final ImageDescriptor   _imageSyncWithSlider_Centered_Disabled       = TourbookPlugin.getThemedImageDescriptor(Images.SyncWith_Slider_Centered_Disabled);

// SET_FORMATTING_ON

   private static final String           STATE_IS_LAYER_BASE_MAP_VISIBLE         = "STATE_IS_LAYER_BASE_MAP_VISIBLE";            //$NON-NLS-1$
   private static final String           STATE_IS_LAYER_BOOKMARK_VISIBLE         = "STATE_IS_LAYER_BOOKMARK_VISIBLE";            //$NON-NLS-1$
   private static final String           STATE_IS_LAYER_HILLSHADING_VISIBLE      = "STATE_IS_LAYER_HILLSHADING_VISIBLE";         //$NON-NLS-1$
   private static final String           STATE_IS_LAYER_MARKER_VISIBLE           = "STATE_IS_LAYER_MARKER_VISIBLE";              //$NON-NLS-1$
   private static final String           STATE_IS_LAYER_SATELLITE_VISIBLE        = "STATE_IS_LAYER_SATELLITE_VISIBLE";           //$NON-NLS-1$
   private static final String           STATE_IS_LAYER_SCALE_BAR_VISIBLE        = "STATE_IS_LAYER_SCALE_BAR_VISIBLE";           //$NON-NLS-1$
   private static final String           STATE_IS_LAYER_TILE_INFO_VISIBLE        = "STATE_IS_LAYER_TILE_INFO_VISIBLE";           //$NON-NLS-1$
   private static final String           STATE_IS_LAYER_TOUR_VISIBLE             = "STATE_IS_LAYER_TOUR_VISIBLE";                //$NON-NLS-1$

   private static final String           STATE_LAYER_HILLSHADING_OPACITY         = "STATE_LAYER_HILLSHADING_OPACITY";            //$NON-NLS-1$
   private static final String           STATE_MAP_SYNCHED_WITH                  = "STATE_MAP_SYNCHED_WITH";                     //$NON-NLS-1$

   private static final String           STATE_IS_LAYER_PHOTO_VISIBLE            = "STATE_IS_LAYER_PHOTO_VISIBLE";               //$NON-NLS-1$
   private static final String           STATE_IS_LAYER_PHOTO_SCALED             = "STATE_IS_LAYER_PHOTO_SCALED";                //$NON-NLS-1$
   private static final String           STATE_IS_LAYER_PHOTO_TITLE_VISIBLE      = "STATE_IS_LAYER_PHOTO_TITLE_VISIBLE";         //$NON-NLS-1$
   private static final String           STATE_IS_PHOTO_FILTER_ACTIVE            = "STATE_IS_PHOTO_FILTER_ACTIVE";               //$NON-NLS-1$
   private static final String           STATE_LAYER_PHOTO_SIZE                  = "STATE_LAYER_PHOTO_SIZE";                     //$NON-NLS-1$
   private static final String           STATE_PHOTO_FILTER_RATING_STARS         = "STATE_PHOTO_FILTER_RATING_STARS";            //$NON-NLS-1$
   private static final String           STATE_PHOTO_FILTER_RATING_STAR_OPERATOR = "STATE_PHOTO_FILTER_RATING_STAR_OPERATOR";    //$NON-NLS-1$
   //
   public static final String            ID                                      = "net.tourbook.map25.Map25View";               //$NON-NLS-1$
   //
   private static final IDialogSettings  _state                                  = TourbookPlugin.getState(ID);
   private static final IDialogSettings  _state_PhotoFilter                      = TourbookPlugin.getState(ID + ".PhotoFilter"); //$NON-NLS-1$
   //
   private static int[]                  _eventCounter                           = new int[1];
   //
   private Map25App                      _mapApp;
   //
   private OpenDialogManager             _openDlgMgr                             = new OpenDialogManager();
   private final MapInfoManager          _mapInfoManager                         = MapInfoManager.getInstance();
   //
   private boolean                       _isPartVisible;
   private boolean                       _isShowTour;
   //
   private IPartListener2                _partListener;
   private ISelectionListener            _postSelectionListener;
   private ITourEventListener            _tourEventListener;
   //
   private ISelection                    _lastHiddenSelection;
   private int                           _lastSelectionHash;
   //
   private ActionMapBookmarks            _actionMapBookmarks;
   private ActionMap25_MapProvider       _actionMapProvider;
   private ActionMap25_Layer             _actionMapLayer;
   private ActionMap25_Options           _actionMapOptions;
   private ActionMap25_PhotoFilter       _actionMapPhotoFilter;
   private ActionShowEntireTour          _actionShowEntireTour;
   private ActionMap25_ShowMarker        _actionShowMarkerOptions;
   private ActionShowPhotoOptions        _actionShowPhotoOptions;
   private ActionShowTour                _actionShowTourOptions;
   private ActionSynchMapWithChartSlider _actionSyncMap_WithChartSlider;
   private ActionSyncMap2WithOtherMap    _actionSyncMap_WithOtherMap;
   private ActionSynchMapWithTour        _actionSyncMap_WithTour;
   private ActionZoomIn                  _actionZoom_In;
   private ActionZoomOut                 _actionZoom_Out;
   //
   private double                        _zoomFactor                             = 1.5;

   /** Contains only geo tours */
   private ArrayList<TourData>           _allTourData                            = new ArrayList<>();
   private TIntArrayList                 _allTourStarts                          = new TIntArrayList();
   private GeoPoint[]                    _allGeoPoints;
   private BoundingBox                   _allBoundingBox;

   /**
    * Contains photos which are displayed in the map
    */
   private ArrayList<Photo>              _allPhotos                              = new ArrayList<>();
   private final ArrayList<Photo>        _filteredPhotos                         = new ArrayList<>();
   //
   private boolean                       _isPhotoFilterActive;
   private int                           _photoFilter_RatingStars;
   private Enum<PhotoRatingStarOperator> _photoFilter_RatingStar_Operator;
   //
   private int                           _leftSliderValueIndex;
   private int                           _rightSliderValueIndex;
   private int                           _selectedSliderValueIndex;
   //
//   private int     _hash_AllPhotos;
   private int     _hashTourId;
   private int     _hashTourData;
   //
   private MapSync _mapSynchedWith = MapSync.NONE;
   //
   private long    _lastFiredSyncEventTime;

   // context menu
//   private boolean _isContextMenuVisible;

   /*
    * UI controls
    */
   private Composite _swtContainer;
   private Composite _parent;

   private Menu      _swtContextMenu;

   private class ActionMap25_Layer extends ActionToolbarSlideout {

      public ActionMap25_Layer() {

         super(TourbookPlugin.getThemedImageDescriptor(Images.MapLayer),
               TourbookPlugin.getThemedImageDescriptor(Images.MapLayer));
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {
         return new SlideoutMap25_MapLayer(_parent, toolbar, Map25View.this);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }
   }

   private class ActionMap25_MapProvider extends ActionToolbarSlideout {

      private SlideoutMap25_MapProvider __slideoutMap25_MapProvider;

      public ActionMap25_MapProvider() {

         super(TourbookPlugin.getThemedImageDescriptor(Images.MapProvider),
               TourbookPlugin.getThemedImageDescriptor(Images.MapProvider));
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         __slideoutMap25_MapProvider = new SlideoutMap25_MapProvider(_parent, toolbar, Map25View.this);

         return __slideoutMap25_MapProvider;
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }
   }

   private class ActionMap25_Options extends ActionToolbarSlideout {

      public ActionMap25_Options() {

         super(TourbookPlugin.getThemedImageDescriptor(Images.MapOptions),
               TourbookPlugin.getThemedImageDescriptor(Images.MapOptions_Disabled));
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {
         return new SlideoutMap25_MapOptions(_parent, toolbar);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }
   }

   private class ActionShowPhotoOptions extends ActionToolbarSlideout {

      public ActionShowPhotoOptions() {

         super(TourbookPlugin.getThemedImageDescriptor(Images.ShowPhotos_InMap),
               TourbookPlugin.getThemedImageDescriptor(Images.ShowAllPhotos_InMap_Disabled));

         isToggleAction = true;
         notSelectedTooltip = Messages.Tour_Action_TourPhotos;
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {
         return new SlideoutMap25_PhotoOptions(_parent, toolbar, Map25View.this);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }

      @Override
      protected void onSelect() {

         super.onSelect();

         actionShowPhotos();
      }
   }

   private class ActionShowTour extends ActionToolbarSlideout {

      public ActionShowTour() {

         super(TourbookPlugin.getThemedImageDescriptor(Images.TourChart),
               TourbookPlugin.getThemedImageDescriptor(Images.TourChart_Disabled));

         isToggleAction = true;
         notSelectedTooltip = MAP_ACTION_SHOW_TOUR_IN_MAP;
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {
         return new SlideoutMap25_TrackOptions(_parent, toolbar, Map25View.this);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }

      @Override
      protected void onSelect() {

         super.onSelect();

         actionShowTour(getSelection());
      }
   }

   private class Map25ContextMenu extends SWTPopupOverAWT {

      public Map25ContextMenu(final Display display, final Menu swtContextMenu) {
         super(display, swtContextMenu);
      }
   }

   private enum MapSync {

      /** Map is not synced */
      NONE, //

      WITH_OTHER_MAP, //
      WITH_SLIDER, //
      WITH_SLIDER_CENTERED, //
      WITH_TOUR, //
   }

   void actionContextMenu(final int relativeX, final int relativeY) {

      // open context menu

      // set state here because opening the context menu is async
//      _isContextMenuVisible = true;

      _swtContainer.getDisplay().asyncExec(new Runnable() {

         @Override
         public void run() {

            final Point screenPoint = _swtContainer.toDisplay(relativeX, relativeY);

            createContextMenu(screenPoint.x, screenPoint.y);
         }
      });

   }

   private void actionShowPhotos() {

      final boolean isPhotoVisible = _actionShowPhotoOptions.getSelection();

      // update model
      _mapApp.setPhoto_IsVisible(isPhotoVisible);

      // update UI
      _mapApp.getLayer_Photo().setEnabled(isPhotoVisible);
      _mapApp.getMap().render();

      // hide photo filter when photos are hidden
      if (isPhotoVisible == false) {
         _actionMapPhotoFilter.getPhotoFilterSlideout().close();
      }

      enableActions();
   }

   /**
    * Show/hide tour tracks.
    *
    * @param isTrackVisible
    */
   public void actionShowTour(final boolean isTrackVisible) {

      _isShowTour = isTrackVisible;
      final Map25TrackConfig activeTourTrackConfig = Map25ConfigManager.getActiveTourTrackConfig();
      final boolean isShowSliderLocation = activeTourTrackConfig.isShowSliderLocation;
      final boolean isShowSliderPath = activeTourTrackConfig.isShowSliderPath;

      _mapApp.getLayer_Tour().setEnabled(_isShowTour);
      _mapApp.getLayer_SliderLocation().setEnabled(_isShowTour && isShowSliderLocation);
      _mapApp.getLayer_SliderPath().setEnabled(_isShowTour && isShowSliderPath);

      _mapApp.getMap().render();

      enableActions();
   }

   public void actionShowTourMarker(final boolean isMarkerVisible) {

      _mapApp.getLayer_MapBookmark().setEnabled(isMarkerVisible);
      _mapApp.getLayer_TourMarker().setEnabled(isMarkerVisible);

      _mapApp.getMap().render();

      enableActions();
   }

   public void actionSync_WithChartSlider() {

      if (_allTourData.isEmpty()) {
         return;
      }

      // change state
      switch (_mapSynchedWith) {

      case WITH_SLIDER_CENTERED:
         _mapSynchedWith = MapSync.WITH_SLIDER;
         break;

      case WITH_SLIDER:
         _mapSynchedWith = MapSync.NONE;
         break;

      default:
         _mapSynchedWith = MapSync.WITH_SLIDER_CENTERED;
         break;
      }

      updateUI_SyncSliderAction();

      deactivateOtherMapSync();

      if (_mapSynchedWith != MapSync.NONE) {

         _actionShowTourOptions.setSelection(true);

         final TourData firstTourData = _allTourData.get(0);

         syncMapWith_ChartSlider(firstTourData);
      }
   }

   public void actionSync_WithOtherMap(final boolean isSelected) {

      _mapSynchedWith = isSelected ? MapSync.WITH_OTHER_MAP : MapSync.NONE;

      deactivateOtherMapSync();
   }

   public void actionSync_WithTour(final boolean isSelected) {

      _mapSynchedWith = isSelected ? MapSync.WITH_TOUR : MapSync.NONE;

      deactivateOtherMapSync();

      if (_mapSynchedWith == MapSync.WITH_TOUR) {

         paintTours();
      }
   }

   public void actionZoomIn() {
      final Map map25 = _mapApp.getMap();

      map25.post(new Runnable() {

         @Override
         public void run() {

            final Animator animator = map25.animator();

            animator.cancel();
            animator.animateZoom(500, _zoomFactor, 0, 0);
            map25.updateMap();
         }
      });

   }

   public void actionZoomOut() {
      final Map map25 = _mapApp.getMap();

      map25.post(new Runnable() {

         @Override
         public void run() {

            final Animator animator = map25.animator();

            animator.cancel();
            animator.animateZoom(500, 1 / _zoomFactor, 0, 0);
            map25.updateMap();
         }
      });

   }

   public void actionZoomShowEntireTour() {

      if (_allBoundingBox == null) {

         // a tour is not yet displayed

         showToursFromTourProvider();

         return;
      }

      final Map map25 = _mapApp.getMap();

      map25.post(new Runnable() {

         @Override
         public void run() {

            final Animator animator = map25.animator();

            animator.cancel();
            animator.animateTo(//
                  2000,
                  _allBoundingBox,
                  Easing.Type.SINE_INOUT,
                  Animator.ANIM_MOVE | Animator.ANIM_SCALE);

            map25.updateMap();
         }
      });

   }

   private void addPartListener() {

      _partListener = new IPartListener2() {

         private void onPartVisible(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == Map25View.this) {

               if (_isPartVisible == false) {

                  _isPartVisible = true;

                  if (_lastHiddenSelection != null) {

                     onSelectionChanged(_lastHiddenSelection);

                     _lastHiddenSelection = null;
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
         public void partClosed(final IWorkbenchPartReference partRef) {}

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == Map25View.this) {
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

            if (part == Map25View.this) {
               return;
            }

            if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

//					resetMap();

            } else if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

//					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
//					if ((modifiedTours != null) && (modifiedTours.size() > 0)) {
//
//						_allTourData.clear();
//						_allTourData.addAll(modifiedTours);
//
//						resetMap();
//					}

            } else if (eventId == TourEventId.UPDATE_UI || eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

//					clearView();

            } else if (eventId == TourEventId.MARKER_SELECTION) {

//					if (eventData instanceof SelectionTourMarker) {
//
//						onSelectionChanged_TourMarker((SelectionTourMarker) eventData, false);
//					}

            } else if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);

            } else if (eventId == TourEventId.SLIDER_POSITION_CHANGED && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void checkSliderIndices() {

      if (_allTourData.isEmpty()) {
         return;
      }

      final TourData tourData = _allTourData.get(0);

      final TourChart tourChart = TourManager.getActiveTourChart(tourData);
      if (tourChart != null) {
         final SelectionChartInfo chartInfo = tourChart.getChartInfo();

         _leftSliderValueIndex = chartInfo.leftSliderValuesIndex;
         _rightSliderValueIndex = chartInfo.rightSliderValuesIndex;
         _selectedSliderValueIndex = chartInfo.selectedSliderValuesIndex;
      }

      final int maxSlices = tourData.latitudeSerie.length - 1;

      _leftSliderValueIndex = Math.max(0, Math.min(maxSlices, _leftSliderValueIndex));
      _rightSliderValueIndex = Math.max(0, Math.min(maxSlices, _rightSliderValueIndex));
      _selectedSliderValueIndex = Math.max(0, Math.min(maxSlices, _selectedSliderValueIndex));
   }

   private void clearView() {

   }

   /**
    * Close all opened dialogs except the opening dialog.
    *
    * @param openingDialog
    */
   @Override
   public void closeOpenedDialogs(final IOpeningDialog openingDialog) {
      _openDlgMgr.closeOpenedDialogs(openingDialog);
   }

   private void createActions() {

// SET_FORMATTING_OFF

      _actionMapBookmarks              = new ActionMapBookmarks(this._parent, this);
      _actionMapLayer                  = new ActionMap25_Layer();
      _actionMapOptions                = new ActionMap25_Options();
      _actionMapPhotoFilter            = new ActionMap25_PhotoFilter(this, _state_PhotoFilter);
      _actionMapProvider               = new ActionMap25_MapProvider();
      _actionShowEntireTour            = new ActionShowEntireTour(this);
      _actionShowMarkerOptions         = new ActionMap25_ShowMarker(this, _parent);
      _actionShowPhotoOptions          = new ActionShowPhotoOptions();
      _actionShowTourOptions           = new ActionShowTour();
      _actionSyncMap_WithChartSlider   = new ActionSynchMapWithChartSlider(this);
      _actionSyncMap_WithOtherMap      = new ActionSyncMap2WithOtherMap(this);
      _actionSyncMap_WithTour          = new ActionSynchMapWithTour(this);
      _actionZoom_In                   = new ActionZoomIn(this);
      _actionZoom_Out                  = new ActionZoomOut(this);

// SET_FORMATTING_ON
   }

   private BoundingBox createBoundingBox(final GeoPoint[] geoPoints) {

      // this is optimized for performance by using an array which BoundingBox do no support
      int minLat = Integer.MAX_VALUE;
      int minLon = Integer.MAX_VALUE;
      int maxLat = Integer.MIN_VALUE;
      int maxLon = Integer.MIN_VALUE;

      for (final GeoPoint geoPoint : geoPoints) {

         if (geoPoint != null) {

            minLat = Math.min(minLat, geoPoint.latitudeE6);
            minLon = Math.min(minLon, geoPoint.longitudeE6);
            maxLat = Math.max(maxLat, geoPoint.latitudeE6);
            maxLon = Math.max(maxLon, geoPoint.longitudeE6);
         }
      }

      return new BoundingBox(minLat, minLon, maxLat, maxLon);
   }

   /**
    * Context menu with net.tourbook.common.util.SWTPopupOverAWT
    *
    * @param xScreenPos
    * @param yScreenPos
    */
   private void createContextMenu(final int xScreenPos, final int yScreenPos) {

      disposeContextMenu();

      _swtContextMenu = new Menu(_swtContainer);

      // Add listener to repopulate the menu each time
      _swtContextMenu.addMenuListener(new MenuAdapter() {

         boolean _isFilled;

         @Override
         public void menuHidden(final MenuEvent e) {

//            _isContextMenuVisible = false;

            /*
             * run async that the context state and tour info reset is done after the context menu
             * actions has done they tasks
             */
            Display.getCurrent().asyncExec(new Runnable() {
               @Override
               public void run() {

//						hideTourInfo();
               }
            });
         }

         @Override
         public void menuShown(final MenuEvent e) {

            if (_isFilled == false) {

               // Ubuntu filled it twice

               _isFilled = true;

               fillContextMenu((Menu) e.widget);
            }

//            _isContextMenuVisible = true;
         }
      });

      final Display display = _swtContainer.getDisplay();

      final Map25ContextMenu swt_awt_ContextMenu = new Map25ContextMenu(display, _swtContextMenu);

      display.asyncExec(new Runnable() {
         @Override
         public void run() {
//				_mapApp.debugPrint("SWT calling menu"); //$NON-NLS-1$
            swt_awt_ContextMenu.swtIndirectShowMenu(xScreenPos, yScreenPos);
         }
      });
   }

   private List<MapMarker> createMapMarkers(final ArrayList<TourData> allTourData) {

      final List<MapMarker> allMarkerItems = new ArrayList<>();

      for (final TourData tourData : allTourData) {

         final Set<TourMarker> tourMarkerList = tourData.getTourMarkers();

         if (tourMarkerList.isEmpty()) {
            continue;
         }

         // check if geo position is available
         final double[] latitudeSerie = tourData.latitudeSerie;
         final double[] longitudeSerie = tourData.longitudeSerie;
         if (latitudeSerie == null || longitudeSerie == null) {
            continue;
         }

         for (final TourMarker tourMarker : tourMarkerList) {

            // skip marker when hidden or not set
            if (tourMarker.isMarkerVisible() == false || tourMarker.getLabel().length() == 0) {
               continue;
            }

            final int serieIndex = tourMarker.getSerieIndex();

            /*
             * check bounds because when a tour is split, it can happen that the marker serie index
             * is out of scope
             */
            if (serieIndex >= latitudeSerie.length) {
               continue;
            }

            /*
             * draw tour marker
             */

            final double latitude = latitudeSerie[serieIndex];
            final double longitude = longitudeSerie[serieIndex];

            final MapMarker item = new MapMarker(
                  tourMarker.getLabel(),
                  tourMarker.getDescription(),
                  new GeoPoint(latitude, longitude));

            allMarkerItems.add(item);
         }
      }

      return allMarkerItems;
   }

   @Override
   public void createPartControl(final Composite parent) {

      _parent = parent;

      createActions();
      fillActionBars();

      createUI(parent);

      addPartListener();
      addTourEventListener();
      addSelectionListener();

      MapBookmarkManager.addBookmarkListener(this);
      MapManager.addMapSyncListener(this);
      PhotoManager.addPhotoEventListener(this);
   }

   private void createUI(final Composite parent) {

      _swtContainer = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
      final Frame awtContainer = SWT_AWT.new_Frame(_swtContainer);

      final Canvas awtCanvas = new Canvas();
      awtContainer.setLayout(new BorderLayout());
      awtCanvas.setIgnoreRepaint(true);

      awtContainer.add(awtCanvas);
      awtCanvas.setFocusable(true);
      awtCanvas.requestFocus();

      awtContainer.addComponentListener(new ComponentAdapter() {

         @Override
         public void componentResized(final ComponentEvent e) {

            /*
             * Render map otherwise a black screen is displayed until the map is moved
             */
            final Map map = _mapApp.getMap();

            // check if initialized
            if (map == null) {
               return;
            }

            map.render();
         }
      });

      _mapApp = Map25App.createMap(this, _state, awtCanvas);
   }

   private void deactivateOtherMapSync() {

      switch (_mapSynchedWith) {

      case WITH_SLIDER:
      case WITH_SLIDER_CENTERED:

         _actionSyncMap_WithOtherMap.setChecked(false);
         _actionSyncMap_WithTour.setChecked(false);
         break;

      case WITH_OTHER_MAP:
         _actionSyncMap_WithChartSlider.setChecked(false);
         _actionSyncMap_WithTour.setChecked(false);
         break;

      case WITH_TOUR:
         _actionSyncMap_WithChartSlider.setChecked(false);
         _actionSyncMap_WithOtherMap.setChecked(false);
         break;

      case NONE:
      default:
         _actionSyncMap_WithChartSlider.setChecked(false);
         _actionSyncMap_WithOtherMap.setChecked(false);
         _actionSyncMap_WithTour.setChecked(false);
         break;
      }
   }

   @Override
   public void dispose() {

      if (_partListener != null) {

         getViewSite().getPage().removePartListener(_partListener);

         _mapApp.stop();
         _mapApp.getMap().destroy();
      }

      MapBookmarkManager.removeBookmarkListener(this);
      MapManager.removeMapSyncListener(this);
      PhotoManager.removePhotoEventListener(this);

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      disposeContextMenu();

      super.dispose();
   }

   private void disposeContextMenu() {

      if (_swtContextMenu != null) {
         _swtContextMenu.dispose();
      }
   }

   /**
    * Enable actions according to the available tours in {@link #_allTours}.
    */
   void enableActions() {

      final TourLayer tourLayer = _mapApp.getLayer_Tour();
      final boolean isTourLayerVisible = tourLayer == null ? false : tourLayer.isEnabled();

      final boolean isTourAvailable = _allTourData.size() > 0;
      final boolean isPhotoAvailable = _allPhotos.size() > 0;

      final boolean isPhotoDisplayed = _actionShowPhotoOptions.getSelection();
      final boolean isTourWithPhoto = isTourAvailable && isPhotoAvailable;

      final boolean canShowTour = isTourAvailable && isTourLayerVisible;

      _actionMapBookmarks.setEnabled(true);
      _actionMapProvider.setEnabled(true);
      _actionMapOptions.setEnabled(true);

// SET_FORMATTING_OFF

      _actionShowEntireTour            .setEnabled(canShowTour);
      _actionShowMarkerOptions         .setEnabled(isTourAvailable);
      _actionSyncMap_WithChartSlider   .setEnabled(canShowTour);
      _actionSyncMap_WithTour          .setEnabled(canShowTour);
      _actionShowTourOptions           .setEnabled(isTourAvailable);

      _actionMapPhotoFilter            .setEnabled(isTourWithPhoto && isPhotoDisplayed);
      _actionShowPhotoOptions          .setEnabled(isTourWithPhoto);

// SET_FORMATTING_ON
   }

   private void enableContextMenuActions() {

   }

   private void fillActionBars() {

      /*
       * fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(new Separator());
      tbm.add(_actionShowPhotoOptions);
      tbm.add(_actionMapPhotoFilter);
      tbm.add(_actionMapBookmarks); //should be moved to position like in Map2View

      tbm.add(new Separator());

      tbm.add(_actionShowTourOptions);
      tbm.add(_actionShowEntireTour);
      tbm.add(_actionSyncMap_WithTour);
      tbm.add(_actionSyncMap_WithChartSlider);
      tbm.add(_actionSyncMap_WithOtherMap);

      tbm.add(new Separator());

      tbm.add(_actionZoom_In);
      tbm.add(_actionZoom_Out);

      tbm.add(new Separator());

      tbm.add(_actionShowMarkerOptions);
      tbm.add(_actionMapLayer);
      tbm.add(_actionMapProvider);
      tbm.add(_actionMapOptions);

      /*
       * fill view menu
       */
//		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

//		fillMapContextMenu(menuMgr);
   }

   private void fillContextMenu(final Menu menu) {

      MapBookmarkManager.fillContextMenu_RecentBookmarks(menu, this);

      enableContextMenuActions();
   }

   void fireSyncMapEvent(final MapPosition mapPosition, final int positionFlags) {

      _lastFiredSyncEventTime = System.currentTimeMillis();

      MapManager.fireSyncMapEvent(mapPosition, this, positionFlags);

      updateUI_MapPosition(mapPosition.getLatitude(), mapPosition.getLongitude(), mapPosition.zoomLevel);
   }

   @Override
   public List<Photo> getFilteredPhotos() {
      return _filteredPhotos;
   }

   public Map25App getMapApp() {
      return _mapApp;
   }

   @Override
   public MapPosition getMapPosition() {

      return _mapApp.getMap().getMapPosition();
   }

   @Override
   public ArrayList<Photo> getPhotos() {
      return _allPhotos;
   }

   @Override
   public void moveToMapLocation(final MapBookmark selectedBookmark) {

      MapBookmarkManager.setLastSelectedBookmark(selectedBookmark);

      final Map map = _mapApp.getMap();
      final MapPosition mapPosition = selectedBookmark.getMapPosition();

      Map25ConfigManager.setMapLocation(map, mapPosition);
   }

   @Override
   public void onMapBookmarkActionPerformed(final MapBookmark mapBookmark, final MapBookmarkEventType mapBookmarkEventType) {

      if (mapBookmarkEventType == MapBookmarkEventType.MOVETO) {
         //_mapApp.debugPrint("*** Map25View_onMapBookmarkActionPerformed moveto: " + mapBookmark.name);
         moveToMapLocation(mapBookmark);
      } else if (mapBookmarkEventType == MapBookmarkEventType.MODIFIED) {
         //_mapApp.debugPrint("*** Map25View_onMapBookmarkActionPerformed modify: " + mapBookmark.name);
         _mapApp.updateLayer_MapBookmarks();
      }
   }

   void onMapPosition(final GeoPoint mapGeoPoint, final int zoomLevel) {
      updateUI_MapPosition(mapGeoPoint.getLatitude(), mapGeoPoint.getLongitude(), zoomLevel);
   }

   private void onSelectionChanged(final ISelection selection) {
      //_mapApp.debugPrint(" Map25View: * onSelectionChanged: tour selection changed");

      final int selectionHash = selection.hashCode();
      if (_lastSelectionHash == selectionHash) {

         /*
          * Last selection has not changed, this can occure when the app lost the focus and got the
          * focus again.
          */
         return;
      }

      _lastSelectionHash = selectionHash;

      if (_isPartVisible == false) {

         if (selection instanceof SelectionTourData
               || selection instanceof SelectionTourId
               || selection instanceof SelectionTourIds) {

            // keep only selected tours
//            _selectionWhenHidden = selection;
         }
         return;
      }

      final boolean isSyncWithSlider = _mapSynchedWith == MapSync.WITH_SLIDER
            || _mapSynchedWith == MapSync.WITH_SLIDER_CENTERED;

      if (selection instanceof SelectionTourData) {

         final SelectionTourData selectionTourData = (SelectionTourData) selection;
         final TourData tourData = selectionTourData.getTourData();

         setMapTour(tourData);
         setMapPhotos(selection);

         paintTours();

      } else if (selection instanceof SelectionTourId) {

         final SelectionTourId tourIdSelection = (SelectionTourId) selection;
         final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

         setMapTour(tourData);
         setMapPhotos(selection);

         paintTours();

      } else if (selection instanceof SelectionTourIds) {

         // paint all selected tours

         final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
         if (tourIds.isEmpty()) {

            // history tour (without tours) is displayed

         } else if (tourIds.size() == 1) {

            // only 1 tour is displayed, synch with this tour !!!

            final TourData tourData = TourManager.getInstance().getTourData(tourIds.get(0));

            setMapTour(tourData);
            setMapPhotos(selection);

            paintTours();

         } else {

            // paint multiple tours

            setMapTours_FromIds(tourIds);
            setMapPhotos(selection);

            paintTours();

         }

      } else if (selection instanceof SelectionChartInfo) {

         final Map25TrackConfig activeTourTrackConfig = Map25ConfigManager.getActiveTourTrackConfig();
         final boolean isShowSliderLocation = activeTourTrackConfig.isShowSliderLocation;
         final boolean isShowSliderPath = activeTourTrackConfig.isShowSliderPath;

         if (isSyncWithSlider == false
               && isShowSliderLocation == false
               && isShowSliderPath == false) {

            // nothing to display
            return;
         }

         TourData tourData = null;

         final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

         final Chart chart = chartInfo.getChart();
         if (chart instanceof TourChart) {

            final TourChart tourChart = (TourChart) chart;
            tourData = tourChart.getTourData();
         }

//			if (tourData != null && tourData.isMultipleTours()) {
//
//				// multiple tours are selected
//
//			} else {
//
//				// use old behaviour
//
//				final ChartDataModel chartDataModel = chartInfo.chartDataModel;
//				if (chartDataModel != null) {
//
//					final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
//					if (tourId instanceof Long) {
//
//						tourData = TourManager.getInstance().getTourData((Long) tourId);
//						if (tourData == null) {
//
//							// tour is not in the database, try to get it from the raw data manager
//
//							final HashMap<Long, TourData> rawData = RawDataManager.getInstance().getImportedTours();
//							tourData = rawData.get(tourId);
//						}
//					}
//				}
//			}

         _leftSliderValueIndex = chartInfo.leftSliderValuesIndex;
         _rightSliderValueIndex = chartInfo.rightSliderValuesIndex;
         _selectedSliderValueIndex = chartInfo.selectedSliderValuesIndex;

         if (tourData != null) {

            if (isSyncWithSlider) {

               syncMapWith_ChartSlider(tourData);

               enableActions();
            }

            if (isShowSliderPath || isShowSliderLocation) {

               setMapTour(tourData);
               setMapPhotos(null);

               paintTours();
            }
         }

      } else if (selection instanceof SelectionChartXSliderPosition) {

         if (isSyncWithSlider == false) {
            return;
         }

         final SelectionChartXSliderPosition xSliderPos = (SelectionChartXSliderPosition) selection;
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

               _leftSliderValueIndex = leftSliderValueIndex;
               _rightSliderValueIndex = rightSliderValueIndex;
               _selectedSliderValueIndex = leftSliderValueIndex;

               syncMapWith_ChartSlider(tourData);

               enableActions();
            }
         }

      } else if (selection instanceof SelectionDeletedTours) {

         clearView();
      }
   }

   /**
    * Paint tours into map
    * <p>
    * {@link #_allTourData} must contain all tours<br>
    * {@link #_allPhotos} must contain photos which should be displayed
    */
   private void paintTours() {

      enableActions();

      if (!_isShowTour) {
         return;
      }

      /*
       * Tours
       */
      final TourLayer tourLayer = _mapApp.getLayer_Tour();
      if (tourLayer == null) {

         // tour layer is not yet created, this happened
         return;
      }

      int geoSize = 0;
      for (final TourData tourData : _allTourData) {
         geoSize += tourData.latitudeSerie.length;
      }

      // use array to optimize performance when millions of points are created
      _allGeoPoints = new GeoPoint[geoSize];
      _allTourStarts.clear();

      int tourIndex = 0;
      int geoIndex = 0;

      if (_allTourData.size() == 1 && _allTourData.get(0).isMultipleTours()) {

         // tourdata contains multiple tours

         final TourData tourData = _allTourData.get(0);

         _allTourStarts.add(tourData.multipleTourStartIndex);

         final double[] latitudeSerie = tourData.latitudeSerie;
         final double[] longitudeSerie = tourData.longitudeSerie;

         // create vtm geo points
         for (int serieIndex = 0; serieIndex < latitudeSerie.length; serieIndex++, tourIndex++) {
            _allGeoPoints[geoIndex++] = (new GeoPoint(latitudeSerie[serieIndex], longitudeSerie[serieIndex]));
         }

      } else {

         for (final TourData tourData : _allTourData) {

            _allTourStarts.add(tourIndex);

            final double[] latitudeSerie = tourData.latitudeSerie;
            final double[] longitudeSerie = tourData.longitudeSerie;

            // create vtm geo points
            for (int serieIndex = 0; serieIndex < latitudeSerie.length; serieIndex++, tourIndex++) {
               _allGeoPoints[geoIndex++] = (new GeoPoint(latitudeSerie[serieIndex], longitudeSerie[serieIndex]));
            }
         }
      }

      tourLayer.setPoints(_allGeoPoints, _allTourStarts);

      checkSliderIndices();

      /*
       * Chart slider + path
       */
      final Map25TrackConfig activeTourTrackConfig = Map25ConfigManager.getActiveTourTrackConfig();
      final boolean isShowSliderLocation = activeTourTrackConfig.isShowSliderLocation;
      final boolean isShowSliderPath = activeTourTrackConfig.isShowSliderPath;

      // show/hide layer
      final SliderLocation_Layer sliderLocation_Layer = _mapApp.getLayer_SliderLocation();
      final SliderPath_Layer sliderPath_Layer = _mapApp.getLayer_SliderPath();

      sliderPath_Layer.setEnabled(isShowSliderPath);
      sliderLocation_Layer.setEnabled(isShowSliderLocation);

      final int numPoints = _allGeoPoints.length;

      if (numPoints > 0) {

         if (isShowSliderPath) {

            sliderPath_Layer.setPoints(_allGeoPoints,
                  _allTourStarts,
                  _leftSliderValueIndex,
                  _rightSliderValueIndex);
         }

         if (isShowSliderLocation) {

            final GeoPoint leftGeoPoint = _allGeoPoints[_leftSliderValueIndex];
            final GeoPoint rightGeoPoint = _allGeoPoints[_rightSliderValueIndex];

            sliderLocation_Layer.setPosition(leftGeoPoint, rightGeoPoint);
         }
      }

      /*
       * Markers
       */
      final MarkerLayerMT markerLayer = _mapApp.getLayer_TourMarker();
      if (markerLayer.isEnabled()) {
         final List<MapMarker> allMarkers = createMapMarkers(_allTourData);
         markerLayer.replaceMarkers(allMarkers);
      }

      /*
       * Photos
       */
      if (_mapApp.isPhoto_Visible()) {

         _mapApp.updateLayer_Photos();
      }

      /*
       * Update map
       */
      final Map map25 = _mapApp.getMap();

      final boolean isSyncWithSlider = _mapSynchedWith == MapSync.WITH_SLIDER
            || _mapSynchedWith == MapSync.WITH_SLIDER_CENTERED;

      if (isSyncWithSlider == false) {

         map25.post(new Runnable() {

            @Override
            public void run() {

               // create outside isSynch that data are available when map is zoomed to show the whole tour
               _allBoundingBox = createBoundingBox(_allGeoPoints);

               if (_mapSynchedWith == MapSync.WITH_TOUR) {

//						final int animationTime = Map25ConfigManager.getActiveTourTrackConfig().animationTime;
                  final int animationTime = Map25ConfigManager.DEFAULT_ANIMATION_TIME;
                  Map25ConfigManager.setMapLocation(map25, _allBoundingBox, animationTime);
               }

               map25.updateMap();
            }
         });

      } else {

         map25.updateMap();
      }
   }

   @Override
   public void photoEvent(final IViewPart viewPart, final PhotoEventId photoEventId, final Object data) {

      if (photoEventId == PhotoEventId.PHOTO_ATTRIBUTES_ARE_MODIFIED) {

         if (data instanceof ArrayList<?>) {

            updateFilteredPhotos();
         }
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

   void restoreState() {

      /*
       * Layer
       */
// SET_FORMATTING_OFF

      // tour layer
      _isShowTour = Util.getStateBoolean(_state, STATE_IS_LAYER_TOUR_VISIBLE, true);
      _actionShowTourOptions.setSelection(_isShowTour);
      _mapApp.getLayer_Tour().setEnabled(_isShowTour);

      // tour marker layer
      final boolean isMarkerVisible = Util.getStateBoolean(_state, STATE_IS_LAYER_MARKER_VISIBLE, true);
      _actionShowMarkerOptions.setSelected(isMarkerVisible);
      _mapApp.getLayer_TourMarker().setEnabled(isMarkerVisible);

      // photo_layer
      final boolean isPhotoVisible = Util.getStateBoolean(_state, STATE_IS_LAYER_PHOTO_VISIBLE, true);
      _mapApp.setPhoto_IsShowTitle  (Util.getStateBoolean(_state, STATE_IS_LAYER_PHOTO_TITLE_VISIBLE, true));
      _mapApp.setPhoto_IsScaled     (Util.getStateBoolean(_state, STATE_IS_LAYER_PHOTO_SCALED, true));
      _mapApp.setPhoto_IsVisible    (isPhotoVisible);
      _mapApp.setPhoto_Size         (Util.getStateInt(_state, STATE_LAYER_PHOTO_SIZE, SlideoutMap25_PhotoOptions.IMAGE_SIZE_MINIMUM));

      _actionShowPhotoOptions.setSelection(isPhotoVisible);
      _mapApp.getLayer_Photo().setEnabled(isPhotoVisible);

      _isPhotoFilterActive             = Util.getStateBoolean(_state, STATE_IS_PHOTO_FILTER_ACTIVE, false);
      _photoFilter_RatingStars         = Util.getStateInt(_state, STATE_PHOTO_FILTER_RATING_STARS, 0);
      _photoFilter_RatingStar_Operator = Util.getStateEnum(_state, STATE_PHOTO_FILTER_RATING_STAR_OPERATOR, PhotoRatingStarOperator.HAS_ANY);
      _actionMapPhotoFilter.setSelection(_isPhotoFilterActive);
      _actionMapPhotoFilter.getPhotoFilterSlideout().restoreState(_photoFilter_RatingStars, _photoFilter_RatingStar_Operator);

      // hillshading layer
      final int layerHillshadingOpacity         = Util.getStateInt(_state, STATE_LAYER_HILLSHADING_OPACITY, 255);
      final BitmapTileLayer layer_HillShading   = _mapApp.getLayer_HillShading();
      layer_HillShading.setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_HILLSHADING_VISIBLE, true));
      layer_HillShading.setBitmapAlpha(layerHillshadingOpacity / 255.0f, true);
      _mapApp.setLayer_HillShading_Options(layerHillshadingOpacity);

      // satellite maps
      _mapApp.getLayer_Satellite()        .setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_SATELLITE_VISIBLE, false));

      // other layers
      _mapApp.getLayer_BaseMap()          .setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_BASE_MAP_VISIBLE,  true));

      _mapApp.getLayer_MapBookmark()      .setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_BOOKMARK_VISIBLE,  true));
      _mapApp.getLayer_ScaleBar()         .setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_SCALE_BAR_VISIBLE, true));
      _mapApp.getLayer_TileInfo()         .setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_TILE_INFO_VISIBLE, false));

      // map is synced with
      _mapSynchedWith = (MapSync) Util.getStateEnum(_state, STATE_MAP_SYNCHED_WITH, MapSync.NONE);
      _actionSyncMap_WithOtherMap         .setChecked(_mapSynchedWith == MapSync.WITH_OTHER_MAP);
      _actionSyncMap_WithTour             .setChecked(_mapSynchedWith == MapSync.WITH_TOUR);
      updateUI_SyncSliderAction();

// SET_FORMATTING_ON

      // other layers are enabled/disabled in net.tourbook.map25.Map25App.restoreMapLayers()

      enableActions();

      showToursFromTourProvider();
   }

   /**
    * Filter photos by rating stars.
    */
   private void runPhotoFilter() {

      _filteredPhotos.clear();

      final boolean hasAnyStars = _photoFilter_RatingStar_Operator == PhotoRatingStarOperator.HAS_ANY;

      if (_isPhotoFilterActive && hasAnyStars == false) {

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

      enableActions();

      // update UI: photo filter slideout
      _actionMapPhotoFilter.updateUI();
      _actionMapPhotoFilter.getPhotoFilterSlideout().updateUI_NumberOfPhotos();
   }

   @PersistState
   private void saveState() {

      Util.setStateEnum(_state, STATE_MAP_SYNCHED_WITH, _mapSynchedWith);

// SET_FORMATTING_OFF

      // other layers
      _state.put(STATE_IS_LAYER_BASE_MAP_VISIBLE,     _mapApp.getLayer_BaseMap().isEnabled());
      _state.put(STATE_IS_LAYER_BOOKMARK_VISIBLE,     _mapApp.getLayer_MapBookmark().isEnabled());
      _state.put(STATE_IS_LAYER_MARKER_VISIBLE,       _mapApp.getLayer_TourMarker().isEnabled());
      _state.put(STATE_IS_LAYER_SATELLITE_VISIBLE,    _mapApp.getLayer_Satellite().isEnabled());
      _state.put(STATE_IS_LAYER_SCALE_BAR_VISIBLE,    _mapApp.getLayer_ScaleBar().isEnabled());
      _state.put(STATE_IS_LAYER_TILE_INFO_VISIBLE,    _mapApp.getLayer_TileInfo().isEnabled());
      _state.put(STATE_IS_LAYER_TOUR_VISIBLE,         _mapApp.getLayer_Tour().isEnabled());

      // photo layer
      _state.put(STATE_IS_LAYER_PHOTO_VISIBLE,        _mapApp.isPhoto_Visible());
      _state.put(STATE_IS_LAYER_PHOTO_TITLE_VISIBLE,  _mapApp.isPhoto_ShowTitle());
      _state.put(STATE_IS_LAYER_PHOTO_SCALED,         _mapApp.isPhoto_Scaled());
      _state.put(STATE_LAYER_PHOTO_SIZE,              _mapApp.getPhoto_Size());

      // hillshading layer
      _state.put(STATE_IS_LAYER_HILLSHADING_VISIBLE,  _mapApp.getLayer_HillShading().isEnabled());
      _state.put(STATE_LAYER_HILLSHADING_OPACITY,     _mapApp.getLayer_HillShading_Opacity());

      // photo filter
      _state.put(STATE_IS_PHOTO_FILTER_ACTIVE,        _actionMapPhotoFilter.getSelection());
      _state.put(STATE_PHOTO_FILTER_RATING_STARS,     _photoFilter_RatingStars);
      Util.setStateEnum(_state, STATE_PHOTO_FILTER_RATING_STAR_OPERATOR, _photoFilter_RatingStar_Operator);
      _actionMapPhotoFilter.getPhotoFilterSlideout().saveState();

// SET_FORMATTING_ON

      Map25ConfigManager.saveState();
   }

   @Override
   public void setFocus() {

//		_swtContainer.setFocus();
   }

   /**
    * Central point to set photos into {@link #_allPhotos} from selection or from
    * {@link #_allTourData}
    *
    * @param selection
    *           Selection which contains photo references or <code>null</code>, then the photos from
    *           {@link #_allTourData} are displayed
    */
   private void setMapPhotos(final ISelection selection) {

      final ArrayList<Photo> allPhotos = new ArrayList<>();

      if (selection instanceof TourPhotoLinkSelection) {

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

      _allPhotos = allPhotos;

      runPhotoFilter();
   }

   /**
    * Central point to set tour data into {@link #_allTourData}
    *
    * @param tourData
    */
   private void setMapTour(final TourData tourData) {

      _allTourData.clear();

      if (tourData != null && tourData.latitudeSerie != null && tourData.latitudeSerie.length > 0) {
         _allTourData.add(tourData);
      }
   }

   /**
    * Central point to set tour data into {@link #_allTourData}
    *
    * @param allTourData
    */
   private void setMapTours(final List<TourData> allTourData) {

      _allTourData.clear();
      _allTourData.addAll(allTourData);
   }

   /**
    * Load tours from tour ID's
    *
    * @param allTourIds
    * @return
    */
   private void setMapTours_FromIds(final List<Long> allTourIds) {

      _allTourData.clear();

      if (allTourIds.hashCode() != _hashTourId || _allTourData.hashCode() != _hashTourData) {

         // tour data needs to be loaded

         TourManager.loadTourData(allTourIds, _allTourData, true);

         _hashTourId = allTourIds.hashCode();
         _hashTourData = _allTourData.hashCode();
      }
   }

   private void showToursFromTourProvider() {

      if (!_isShowTour) {
         return;
      }

      Display.getCurrent().asyncExec(() -> {

         // validate widget
         if (_swtContainer.isDisposed()) {
            return;
         }

         final ArrayList<TourData> tourDataList = TourManager.getSelectedTours(true);
         if (tourDataList != null) {

            setMapTours(tourDataList);
            setMapPhotos(null);

            paintTours();
         }

         enableActions();
      });
   }

   private void syncMapWith_ChartSlider(final TourData tourData) {

      if (tourData == null || tourData.latitudeSerie == null) {

         return;
      }

      // sync map with chart slider

      syncMapWith_SliderPosition(tourData, _selectedSliderValueIndex);

      enableActions();
   }

   private void syncMapWith_SliderPosition(final TourData tourData, final int valueIndex) {

      final double[] latitudeSerie = tourData.latitudeSerie;

      // force bounds
      final int checkedValueIndex = Math.max(0, Math.min(valueIndex, latitudeSerie.length - 1));

      final double latitude = latitudeSerie[checkedValueIndex];
      final double longitude = tourData.longitudeSerie[checkedValueIndex];

      final Map map25 = _mapApp.getMap();
      final MapPosition currentMapPos = new MapPosition();

      if (_mapSynchedWith == MapSync.WITH_SLIDER) {

         // sync map with selected

         // get current position
         map25.viewport().getMapPosition(currentMapPos);

         // set new position
         currentMapPos.setPosition(latitude, longitude);

         // update map
         map25.setMapPosition(currentMapPos);
         map25.render();

      } else {

         // center sliders

         // create bounding box for the sliders
         final GeoPoint leftGeoPoint = _allGeoPoints[_leftSliderValueIndex];
         GeoPoint rightGeoPoint = _allGeoPoints[_rightSliderValueIndex];

         final int MIN_CENTER_SLIDER_DIFF_E6 = 100;

         // ensure that both locations are different otherwise it is like div by 0
         if (Math.abs(leftGeoPoint.latitudeE6 - rightGeoPoint.latitudeE6) < MIN_CENTER_SLIDER_DIFF_E6
               && Math.abs(leftGeoPoint.longitudeE6 - rightGeoPoint.longitudeE6) < MIN_CENTER_SLIDER_DIFF_E6) {

            rightGeoPoint = new GeoPoint(
                  rightGeoPoint.latitudeE6 + MIN_CENTER_SLIDER_DIFF_E6,
                  rightGeoPoint.longitudeE6 + MIN_CENTER_SLIDER_DIFF_E6);
         }

         final List<GeoPoint> sliderPoints = new ArrayList<>();
         sliderPoints.add(leftGeoPoint);
         sliderPoints.add(rightGeoPoint);

         final BoundingBox sliderBBox = new BoundingBox(sliderPoints);

         _eventCounter[0]++;

         map25.post(new Runnable() {

            final int __runnableCounter = _eventCounter[0];

            @Override
            public void run() {

               // skip all events which has not yet been executed
               if (__runnableCounter != _eventCounter[0]) {

                  // a new event occured
                  return;
               }

               Map25ConfigManager.setMapLocation(map25, sliderBBox, 500);
            }
         });
      }
   }

   @Override
   public void syncMapWithOtherMap(final MapPosition mapPosition,
                                   final ViewPart viewPart,
                                   final int positionFlags) {

      if (_mapSynchedWith != MapSync.WITH_OTHER_MAP) {

         // sync feature is disabled

         return;
      }

      if (viewPart == this || !_isPartVisible) {

         // event is fired from this map -> ignore

         return;
      }

      final long timeDiff = System.currentTimeMillis() - _lastFiredSyncEventTime;

      if (timeDiff < 1000) {
         // ignore because it causes LOTS of problems when synching moved map
         return;
      }

      final Map map = _mapApp.getMap();

      /**
       * Keep current tilt/bearing
       */
      final MapPosition currentMapPos = map.getMapPosition();
      if (mapPosition.bearing == 0) {
         mapPosition.bearing = currentMapPos.bearing;
      }

      if (mapPosition.tilt == 0) {
         mapPosition.tilt = currentMapPos.tilt;
      }

      Map25ConfigManager.setMapLocation(map, mapPosition);
   }

   private void updateFilteredPhotos() {

      runPhotoFilter();

      _mapApp.updateLayer_Photos();
      _mapApp.updateMap();

   }

   @Override
   public void updatePhotoFilter(final int filterRatingStars, final PhotoRatingStarOperator ratingStarOperatorsValues) {

      photoFilter_UpdateFromSlideout(filterRatingStars, ratingStarOperatorsValues);
   }

   private void updateUI_MapPosition(final double latitude, final double longitude, final int zoomLevel) {

      // validate widget
      if (_swtContainer.isDisposed()) {
         return;
      }

      _swtContainer.getDisplay().asyncExec(new Runnable() {
         @Override
         public void run() {

            // validate widget
            if (_swtContainer.isDisposed()) {
               return;
            }

            _mapInfoManager.setMapPosition(latitude, longitude, zoomLevel);
         }
      });
   }

   void updateUI_SelectedMapProvider(final Map25Provider selectedMapProvider) {

      _actionMapProvider.__slideoutMap25_MapProvider.selectMapProvider(selectedMapProvider);
   }

   private void updateUI_SyncSliderAction() {

      String toolTip;
      ImageDescriptor imageDescriptor;
      ImageDescriptor imageDescriptorDisabled;

      final boolean isSync = _mapSynchedWith == MapSync.WITH_SLIDER;
      final boolean isCenter = _mapSynchedWith == MapSync.WITH_SLIDER_CENTERED;

      if (isCenter) {

         toolTip = MAP_ACTION_SYNCH_WITH_SLIDER_CENTERED;

         imageDescriptor = _imageSyncWithSlider_Centered;
         imageDescriptorDisabled = _imageSyncWithSlider_Centered_Disabled;

      } else {

         toolTip = MAP_ACTION_SYNCH_WITH_SLIDER;

         imageDescriptor = _imageSyncWithSlider;
         imageDescriptorDisabled = _imageSyncWithSlider_Disabled;
      }

      _actionSyncMap_WithChartSlider.setToolTipText(toolTip);
      _actionSyncMap_WithChartSlider.setImageDescriptor(imageDescriptor);
      _actionSyncMap_WithChartSlider.setDisabledImageDescriptor(imageDescriptorDisabled);
      _actionSyncMap_WithChartSlider.setChecked(isSync || isCenter);
   }

}
