/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
import net.tourbook.common.color.ColorProviderConfig;
import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.Map3GradientColorManager;
import net.tourbook.common.color.MapGraphId;
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
import net.tourbook.map.MapColorProvider;
import net.tourbook.map.MapInfoManager;
import net.tourbook.map.MapManager;
import net.tourbook.map.MapUtils;
import net.tourbook.map.bookmark.ActionMapBookmarks;
import net.tourbook.map.bookmark.IMapBookmarkListener;
import net.tourbook.map.bookmark.IMapBookmarks;
import net.tourbook.map.bookmark.MapBookmark;
import net.tourbook.map.bookmark.MapBookmarkManager;
import net.tourbook.map.player.ModelPlayerManager;
import net.tourbook.map.player.ModelPlayerView;
import net.tourbook.map2.view.IDiscreteColorProvider;
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
import net.tourbook.map25.layer.tourtrack.Map25TrackConfig.LineColorMode;
import net.tourbook.map25.layer.tourtrack.SliderLocation_Layer;
import net.tourbook.map25.layer.tourtrack.SliderPath_Layer;
import net.tourbook.map25.layer.tourtrack.TourTrack_Layer;
import net.tourbook.map25.ui.SlideoutMap25_MapLayer;
import net.tourbook.map25.ui.SlideoutMap25_MapOptions;
import net.tourbook.map25.ui.SlideoutMap25_MapProvider;
import net.tourbook.map25.ui.SlideoutMap25_PhotoOptions;
import net.tourbook.map25.ui.SlideoutMap25_TrackColors;
import net.tourbook.map25.ui.SlideoutMap25_TrackOptions;
import net.tourbook.photo.IPhotoEventListener;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoEventId;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.PhotoRatingStarOperator;
import net.tourbook.preferences.ITourbookPreferences;
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

import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
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

   private static final String MAP_ACTION_SHOW_TOUR_IN_MAP            = net.tourbook.map2.Messages.map_action_show_tour_in_map;
   private static final String MAP_ACTION_TOUR_COLOR_ALTITUDE_TOOLTIP = net.tourbook.map2.Messages.map_action_tour_color_altitude_tooltip;
   private static final String MAP_ACTION_TOUR_COLOR_GRADIENT_TOOLTIP = net.tourbook.map2.Messages.map_action_tour_color_gradient_tooltip;
   private static final String MAP_ACTION_TOUR_COLOR_PACE_TOOLTIP     = net.tourbook.map2.Messages.map_action_tour_color_pace_tooltip;
   private static final String MAP_ACTION_TOUR_COLOR_PULSE_TOOLTIP    = net.tourbook.map2.Messages.map_action_tour_color_pulse_tooltip;
   private static final String MAP_ACTION_TOUR_COLOR_SPEED_TOOLTIP    = net.tourbook.map2.Messages.map_action_tour_color_speed_tooltip;
   private static final String TOUR_ACTION_SHOW_HR_ZONES_TOOLTIP      = net.tourbook.map2.Messages.Tour_Action_ShowHrZones_Tooltip;

// SET_FORMATTING_OFF

   private static final String            MAP_ACTION_SYNCH_WITH_SLIDER                 = net.tourbook.map2.Messages.map_action_synch_with_slider;
   private static final String            MAP_ACTION_SYNCH_WITH_SLIDER_CENTERED        = net.tourbook.map2.Messages.Map_Action_SynchWithSlider_Centered;

   private static final ImageDescriptor   _imageSyncWithSlider                         = TourbookPlugin.getThemedImageDescriptor(Images.SyncWith_Slider);
   private static final ImageDescriptor   _imageSyncWithSlider_Disabled                = TourbookPlugin.getThemedImageDescriptor(Images.SyncWith_Slider_Disabled);
   private static final ImageDescriptor   _imageSyncWithSlider_Centered                = TourbookPlugin.getThemedImageDescriptor(Images.SyncWith_Slider_Centered);
   private static final ImageDescriptor   _imageSyncWithSlider_Centered_Disabled       = TourbookPlugin.getThemedImageDescriptor(Images.SyncWith_Slider_Centered_Disabled);

// SET_FORMATTING_ON

   private static final String STATE_IS_LAYER_BASE_MAP_VISIBLE     = "STATE_IS_LAYER_BASE_MAP_VISIBLE";     //$NON-NLS-1$
   private static final String STATE_IS_LAYER_BOOKMARK_VISIBLE     = "STATE_IS_LAYER_BOOKMARK_VISIBLE";     //$NON-NLS-1$
   private static final String STATE_IS_LAYER_HILLSHADING_VISIBLE  = "STATE_IS_LAYER_HILLSHADING_VISIBLE";  //$NON-NLS-1$
   private static final String STATE_IS_LAYER_LEGEND_VISIBLE       = "STATE_IS_LAYER_LEGEND_VISIBLE";       //$NON-NLS-1$
   private static final String STATE_IS_LAYER_MARKER_VISIBLE       = "STATE_IS_LAYER_MARKER_VISIBLE";       //$NON-NLS-1$
   private static final String STATE_IS_LAYER_COMPASS_ROSE_VISIBLE = "STATE_IS_LAYER_COMPASS_ROSE_VISIBLE"; //$NON-NLS-1$
   private static final String STATE_IS_LAYER_SATELLITE_VISIBLE    = "STATE_IS_LAYER_SATELLITE_VISIBLE";    //$NON-NLS-1$
   private static final String STATE_IS_LAYER_SCALE_BAR_VISIBLE    = "STATE_IS_LAYER_SCALE_BAR_VISIBLE";    //$NON-NLS-1$
   private static final String STATE_IS_LAYER_TILE_INFO_VISIBLE    = "STATE_IS_LAYER_TILE_INFO_VISIBLE";    //$NON-NLS-1$
   private static final String STATE_IS_LAYER_TOUR_VISIBLE         = "STATE_IS_LAYER_TOUR_VISIBLE";         //$NON-NLS-1$
// private static final String           STATE_IS_LAYER_OPEN_GL_TEST_VISIBLE     = "STATE_IS_LAYER_OPEN_GL_TEST_VISIBLE";        //$NON-NLS-1$
   //
   private static final String           STATE_LAYER_HILLSHADING_OPACITY         = "STATE_LAYER_HILLSHADING_OPACITY";            //$NON-NLS-1$
   private static final String           STATE_MAP_SYNCHED_WITH                  = "STATE_MAP_SYNCHED_WITH";                     //$NON-NLS-1$
   // photo layer
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
   private static final IPreferenceStore _prefStore                              = TourbookPlugin.getPrefStore();
   private static final IDialogSettings  _state                                  = TourbookPlugin.getState(ID);
   private static final IDialogSettings  _state_PhotoFilter                      = TourbookPlugin.getState(ID + ".PhotoFilter"); //$NON-NLS-1$
   //
   private static int[]                  _eventCounter                           = new int[1];
   //
   private Map25App                      _map25App;
   //
   private OpenDialogManager             _openDialogManager                      = new OpenDialogManager();
   private final MapInfoManager          _mapInfoManager                         = MapInfoManager.getInstance();
   //
   private boolean                       _isPartVisible;
   private boolean                       _isShowTour;
   //
   private IPartListener2                _partListener;
   private ISelectionListener            _postSelectionListener;
   private ITourEventListener            _tourEventListener;
   private IPropertyChangeListener       _prefChangeListener;
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
   private List<Object>                  _allTrackColorActions;
   private ActionTrackColor              _actionTrackColor_Altitude;
   private ActionTrackColor              _actionTrackColor_Gradient;
   private ActionTrackColor              _actionTrackColor_Pace;
   private ActionTrackColor              _actionTrackColor_Pulse;
   private ActionTrackColor              _actionTrackColor_Speed;
   private Action                        _actionTrackColor_HrZone;
   //
   private double                        _zoomFactor                             = 1.5;
   //
   /** Contains only geo tours */
   private ArrayList<TourData>           _allTourData                            = new ArrayList<>();
   private IntArrayList                  _allTourStarts                          = new IntArrayList();
   private GeoPoint[]                    _allGeoPoints;
   private BoundingBox                   _boundingBox;
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
   private int                           _hashTourId;
   private int                           _hashTourData;
   //
   private MapPosition                   _currentMapPosition                     = new MapPosition();
   private MapSync                       _mapSynchedWith                         = MapSync.NONE;
   //
   private long                          _lastFiredSyncEventTime;
   private long                          _lastReceivedSyncEventTime;
   //
   private IMapColorProvider             _mapColorProvider;
   //
   // context menu
//   private boolean _isContextMenuVisible;
   /*
    * UI controls
    */
   private Composite _swtContainer;
   private Composite _parent;
   //
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

         return new SlideoutMap25_MapOptions(_parent, toolbar, Map25View.this);
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

   private class ActionTrackColor extends ActionToolbarSlideout {

      public MapGraphId _graphId = null;

      ActionTrackColor(final MapGraphId graphId,
                       final String toolTipText) {

         super(net.tourbook.ui.UI.getGraphImage(graphId),
               net.tourbook.ui.UI.getGraphImage_Disabled(graphId));

         _graphId = graphId;

         isToggleAction = true;
         notSelectedTooltip = toolTipText;
      }

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {
         return new SlideoutMap25_TrackColors(_parent, toolbar, Map25View.this, _graphId, _state);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }

      @Override
      protected void onSelect() {

         super.onSelect();

         actionTrackColor(this);
      }
   }

   private class ActionTrackColor_HrZone extends Action {

      public MapGraphId _graphId = MapGraphId.HrZone;

      public ActionTrackColor_HrZone() {

         super(null, AS_CHECK_BOX);

         setToolTipText(TOUR_ACTION_SHOW_HR_ZONES_TOOLTIP);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.PulseZones));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.PulseZones_Disabled));
      }

      @Override
      public void run() {
         actionTrackColor(this);
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
      _map25App.setPhoto_IsVisible(isPhotoVisible);

      // update UI
      _map25App.getLayer_Photo().setEnabled(isPhotoVisible);
      _map25App.getMap().render();

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

      _map25App.getLayer_Tour().setEnabled(_isShowTour);
      _map25App.getLayer_SliderLocation().setEnabled(_isShowTour && isShowSliderLocation);
      _map25App.getLayer_SliderPath().setEnabled(_isShowTour && isShowSliderPath);

      _map25App.getMap().render();

      enableActions();
   }

   public void actionShowTourMarker(final boolean isMarkerVisible) {

      _map25App.getLayer_MapBookmark().setEnabled(isMarkerVisible);
      _map25App.getLayer_TourMarker().setEnabled(isMarkerVisible);

      _map25App.getMap().render();

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

   private void actionTrackColor(final Object selectedAction) {

      // get graph ID
      MapGraphId selectedActionGraphId = null;
      if (selectedAction instanceof ActionTrackColor) {

         selectedActionGraphId = ((ActionTrackColor) selectedAction)._graphId;

      } else if (selectedAction instanceof ActionTrackColor_HrZone) {

         selectedActionGraphId = ((ActionTrackColor_HrZone) selectedAction)._graphId;
      }

      final MapGraphId trackGraphId = Map25ConfigManager.getActiveTourTrackConfig().gradientColorGraphID;
      if (trackGraphId == selectedActionGraphId) {

         // active color is selected -> prevent unchecking selected color

         if (selectedAction instanceof ActionTrackColor) {

            final ActionTrackColor action = (ActionTrackColor) selectedAction;
            if (action.getSelection() == false) {

               action.setSelection(true);
            }

         } else if (selectedAction instanceof ActionTrackColor_HrZone) {

            final ActionTrackColor_HrZone action = (ActionTrackColor_HrZone) selectedAction;
            if (action.isChecked() == false) {

               action.setChecked(true);
            }
         }

         return;
      }

      // a new color is selected, uncheck previous colors

      for (final Object action : _allTrackColorActions) {

         // uncheck any other colors
         if (action != selectedAction) {

            if (action instanceof ActionTrackColor) {

               ((ActionTrackColor) action).setSelection(false);

            } else if (action instanceof ActionTrackColor_HrZone) {

               ((ActionTrackColor_HrZone) action).setChecked(false);
            }
         }
      }

      setColorProvider(selectedActionGraphId, true);
   }

   public void actionZoomIn() {

      final Map map25 = _map25App.getMap();

      map25.post(() -> {

         final Animator animator = map25.animator();

         animator.animateZoom(500, _zoomFactor, 0, 0);
         map25.updateMap();
      });

   }

   public void actionZoomOut() {
      final Map map25 = _map25App.getMap();

      map25.post(() -> {

         final Animator animator = map25.animator();

         animator.animateZoom(500, 1 / _zoomFactor, 0, 0);
         map25.updateMap();
      });

   }

   public void actionZoomShowEntireTour() {

      if (_boundingBox == null) {

         // a tour is not yet displayed

         showToursFromTourProvider();

         return;
      }

      final Map map25 = _map25App.getMap();

      map25.post(() -> {

         final Animator animator = map25.animator();

         animator.animateTo(
               2000,
               _boundingBox,
               Easing.Type.SINE_INOUT,
               Animator.ANIM_MOVE | Animator.ANIM_SCALE);

         map25.updateMap();
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

            setIsMap25Available(partRef, null);
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

            setIsMap25Available(partRef, Map25View.this);
         }

         private void setIsMap25Available(final IWorkbenchPartReference partRef, final Map25View map25View) {

            if (partRef.getPart(false) == Map25View.this) {

               ModelPlayerManager.setMap25View(map25View);
            }
         }
      };
      getViewSite().getPage().addPartListener(_partListener);
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.MAP3_COLOR_IS_MODIFIED)) {

            // update map colors

            final MapGraphId trackGraphId = Map25ConfigManager.getActiveTourTrackConfig().gradientColorGraphID;

            setColorProvider(trackGraphId, true);
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
      _openDialogManager.closeOpenedDialogs(openingDialog);
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

      _actionTrackColor_Altitude        = new ActionTrackColor(MapGraphId.Altitude,   MAP_ACTION_TOUR_COLOR_ALTITUDE_TOOLTIP);
      _actionTrackColor_Gradient        = new ActionTrackColor(MapGraphId.Gradient,   MAP_ACTION_TOUR_COLOR_GRADIENT_TOOLTIP);
      _actionTrackColor_Pace            = new ActionTrackColor(MapGraphId.Pace,       MAP_ACTION_TOUR_COLOR_PACE_TOOLTIP);
      _actionTrackColor_Pulse           = new ActionTrackColor(MapGraphId.Pulse,      MAP_ACTION_TOUR_COLOR_PULSE_TOOLTIP);
      _actionTrackColor_Speed           = new ActionTrackColor(MapGraphId.Speed,      MAP_ACTION_TOUR_COLOR_SPEED_TOOLTIP);
      _actionTrackColor_HrZone          = new ActionTrackColor_HrZone();

// SET_FORMATTING_ON

      _allTrackColorActions = new ArrayList<>();
      _allTrackColorActions.add(_actionTrackColor_Altitude);
      _allTrackColorActions.add(_actionTrackColor_Gradient);
      _allTrackColorActions.add(_actionTrackColor_Pace);
      _allTrackColorActions.add(_actionTrackColor_Pulse);
      _allTrackColorActions.add(_actionTrackColor_Speed);
      _allTrackColorActions.add(_actionTrackColor_HrZone);
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
      addPrefListener();
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
            final Map map = _map25App.getMap();

            // check if initialized
            if (map == null) {
               return;
            }

            map.render();
         }
      });

      _map25App = Map25App.createMap(this, _state, awtCanvas);
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

         _map25App.stop();
         _map25App.getMap().destroy();
      }

      _prefStore.removePropertyChangeListener(_prefChangeListener);

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

      final TourTrack_Layer tourLayer = _map25App.getLayer_Tour();
      final boolean isTourLayerVisible = tourLayer == null ? false : tourLayer.isEnabled();

      final boolean isTourAvailable = _allTourData.size() > 0;
      final boolean isPhotoAvailable = _allPhotos.size() > 0;
      final boolean isTourVisible = _actionShowTourOptions.getSelection();

      final boolean isPhotoDisplayed = _actionShowPhotoOptions.getSelection();
      final boolean isTourWithPhoto = isTourAvailable && isPhotoAvailable;

      final boolean canShowTour = isTourAvailable && isTourLayerVisible;

      final Map25TrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();

      final boolean isGradientColor = trackConfig.lineColorMode == LineColorMode.GRADIENT
            && canShowTour
            && isTourVisible;

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

      _actionTrackColor_Altitude       .setEnabled(isGradientColor);
      _actionTrackColor_Gradient       .setEnabled(isGradientColor);
      _actionTrackColor_HrZone         .setEnabled(isGradientColor);
      _actionTrackColor_Pace           .setEnabled(isGradientColor);
      _actionTrackColor_Pulse          .setEnabled(isGradientColor);
      _actionTrackColor_Speed          .setEnabled(isGradientColor);

// SET_FORMATTING_ON
   }

   private void enableContextMenuActions() {

   }

   private void fillActionBars() {

      /*
       * fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionTrackColor_Altitude);
      tbm.add(_actionTrackColor_Pulse);
      tbm.add(_actionTrackColor_Speed);
      tbm.add(_actionTrackColor_Pace);
      tbm.add(_actionTrackColor_Gradient);
      tbm.add(_actionTrackColor_HrZone);

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

   void fireSyncMapEvent(final MapPosition mapPosition, final SyncParameter syncParameter) {

      _lastFiredSyncEventTime = System.currentTimeMillis();

      MapManager.fireSyncMapEvent(mapPosition, this, syncParameter);

      updateUI_MapPosition(mapPosition.getLatitude(), mapPosition.getLongitude(), mapPosition.zoomLevel);
   }

   @Override
   public List<Photo> getFilteredPhotos() {
      return _filteredPhotos;
   }

   public long getLastReceivedSyncEventTime() {
      return _lastReceivedSyncEventTime;
   }

   public Map25App getMapApp() {
      return _map25App;
   }

   @Override
   public MapPosition getMapPosition() {

      return _map25App.getMap().getMapPosition();
   }

   @Override
   public ArrayList<Photo> getPhotos() {
      return _allPhotos;
   }

   public Shell getShell() {

      return _parent.getShell();
   }

   private float[] getValueSerie(final TourData tourData) {

      final MapGraphId trackGraphId = Map25ConfigManager.getActiveTourTrackConfig().gradientColorGraphID;

      switch (trackGraphId) {

      case Altitude:
         return tourData.getAltitudeSerie();

      case Cadence:
         return tourData.getCadenceSerie();

      case Gradient:
         return tourData.getGradientSerie();

      case Pulse:
         return tourData.pulseSerie;

      case Pace:
         return tourData.getPaceSerie();

      case Speed:
         return tourData.getSpeedSerie();

      case Altimeter:
      case HrZone:
      case Power:
      case RunDyn_StepLength:
      case Temperature:
      default:
         return null;
      }
   }

   @Override
   public void moveToMapLocation(final MapBookmark selectedBookmark) {

      MapBookmarkManager.setLastSelectedBookmark(selectedBookmark);

      final Map map = _map25App.getMap();
      final MapPosition mapPosition = selectedBookmark.getMapPosition();

      Map25LocationManager.setMapLocation(map, mapPosition);
   }

   @Override
   public void onMapBookmarkActionPerformed(final MapBookmark mapBookmark, final MapBookmarkEventType mapBookmarkEventType) {

      if (mapBookmarkEventType == MapBookmarkEventType.MOVETO) {
         //_mapApp.debugPrint("*** Map25View_onMapBookmarkActionPerformed moveto: " + mapBookmark.name);
         moveToMapLocation(mapBookmark);
      } else if (mapBookmarkEventType == MapBookmarkEventType.MODIFIED) {
         //_mapApp.debugPrint("*** Map25View_onMapBookmarkActionPerformed modify: " + mapBookmark.name);
         _map25App.updateLayer_MapBookmarks();
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
      final TourTrack_Layer tourLayer = _map25App.getLayer_Tour();
      if (tourLayer == null) {

         // tour layer is not yet created, this happened
         return;
      }

      // set colors according to the tour values
      if (_mapColorProvider instanceof IGradientColorProvider) {

         MapUtils.configureColorProvider(
               _allTourData,
               (IGradientColorProvider) _mapColorProvider,
               ColorProviderConfig.MAP3_TOUR,
               200 // set dummy for now
         );
      }

      int numAllTimeSlices = 0;

      for (final TourData tourData : _allTourData) {
         numAllTimeSlices += tourData.latitudeSerie.length;
      }

      // use an array to optimize performance when millions of points are created
      _allGeoPoints = new GeoPoint[numAllTimeSlices];
      _allTourStarts.clear();
      final int[] allGeoPointColors = new int[numAllTimeSlices];

      int tourIndex = 0;
      int geoIndex = 0;

      int[] allTimeSeries;
      float[] allDistanceSeries;

      if (_allTourData.size() == 1 && _allTourData.get(0).isMultipleTours()) {

         // one tourdata contains multiple tours

         final TourData tourData = _allTourData.get(0);

         allTimeSeries = tourData.timeSerie;
         allDistanceSeries = tourData.distanceSerie;

         _allTourStarts.addAll(tourData.multipleTourStartIndex);

         final double[] latitudeSerie = tourData.latitudeSerie;
         final double[] longitudeSerie = tourData.longitudeSerie;

         // create vtm E6 geo points
         for (int serieIndex = 0; serieIndex < latitudeSerie.length; serieIndex++, tourIndex++) {
            _allGeoPoints[geoIndex++] = (new GeoPoint(latitudeSerie[serieIndex], longitudeSerie[serieIndex]));
         }

      } else {

         allTimeSeries = new int[numAllTimeSlices];
         allDistanceSeries = new float[numAllTimeSlices];

         int prevTourTimes = 0;
         float prevTourDistances = 0;

         for (final TourData tourData : _allTourData) {

            _allTourStarts.add(tourIndex);

            /*
             * Create time/distance series
             */
            final int[] oneTourTimeSerie = tourData.timeSerie;
            final float[] oneTourDistanceSerie = tourData.distanceSerie;

            final boolean isTourDistanceAvailable = oneTourDistanceSerie != null && oneTourDistanceSerie.length > 0;
            final boolean isTourTimeAvailable = oneTourTimeSerie != null && oneTourTimeSerie.length > 0;

            /*
             * Create vtm geo points and colors
             */
            final double[] latitudeSerie = tourData.latitudeSerie;
            final double[] longitudeSerie = tourData.longitudeSerie;

            final float[] valueSerie = getValueSerie(tourData);

            for (int serieIndex = 0; serieIndex < latitudeSerie.length; serieIndex++, tourIndex++) {

               if (isTourTimeAvailable) {
                  allTimeSeries[geoIndex] = prevTourTimes + oneTourTimeSerie[serieIndex];
               }

               if (isTourDistanceAvailable) {
                  allDistanceSeries[geoIndex] = prevTourDistances + oneTourDistanceSerie[serieIndex];
               }

               _allGeoPoints[geoIndex] = (new GeoPoint(latitudeSerie[serieIndex], longitudeSerie[serieIndex]));

               int colorValue = 0xff_80_80_80;
               int abgr = 0;

               if (valueSerie != null && _mapColorProvider instanceof IGradientColorProvider) {

                  abgr = ((IGradientColorProvider) _mapColorProvider).getRGBValue(
                        ColorProviderConfig.MAP3_TOUR,
                        valueSerie[serieIndex]);

               } else if (_mapColorProvider instanceof IDiscreteColorProvider) {

                  // e.g. HR zone color provider

                  abgr = ((IDiscreteColorProvider) _mapColorProvider).getColorValue(tourData, serieIndex, true);
               }

// SET_FORMATTING_OFF

               final int alpha   = (abgr & 0xFF000000) >>> 24;
               final int blue    = (abgr & 0xFF0000)   >>> 16;
               final int green   = (abgr & 0xFF00)     >>> 8;
               final int red     = (abgr & 0xFF)       >>> 0;

               colorValue =
                       ((alpha   & 0xFF) << 24)
                     | ((red     & 0xFF) << 16)
                     | ((green   & 0xFF) << 8)
                     | ((blue    & 0xFF) << 0);

// SET_FORMATTING_ON

               allGeoPointColors[geoIndex] = colorValue;

               geoIndex++;
            }

            /*
             * Summarize tour times and distances
             */
            if (isTourTimeAvailable) {
               prevTourTimes += oneTourTimeSerie[oneTourTimeSerie.length - 1];
            }
            if (isTourDistanceAvailable) {
               prevTourDistances += oneTourDistanceSerie[oneTourDistanceSerie.length - 1];
            }
         }
      }

      tourLayer.setupTourPositions(_allGeoPoints, allGeoPointColors, _allTourStarts, allTimeSeries, allDistanceSeries);

      checkSliderIndices();

      /*
       * Chart slider + path
       */
      final Map25TrackConfig activeTourTrackConfig = Map25ConfigManager.getActiveTourTrackConfig();
      final boolean isShowSliderLocation = activeTourTrackConfig.isShowSliderLocation;
      final boolean isShowSliderPath = activeTourTrackConfig.isShowSliderPath;

      // show/hide layer
      final SliderLocation_Layer sliderLocation_Layer = _map25App.getLayer_SliderLocation();
      final SliderPath_Layer sliderPath_Layer = _map25App.getLayer_SliderPath();

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
      final MarkerLayerMT markerLayer = _map25App.getLayer_TourMarker();
      if (markerLayer.isEnabled()) {
         final List<MapMarker> allMarkers = createMapMarkers(_allTourData);
         markerLayer.replaceMarkers(allMarkers);
      }

      /*
       * Photos
       */
      if (_map25App.isPhoto_Visible()) {

         _map25App.updateLayer_Photos();
      }

      /*
       * Legend
       */
      _map25App.getLayer_Legend().updateLegend(_allTourData);

      /*
       * Update map
       */
      final Map map25 = _map25App.getMap();

      final boolean isSyncWithSlider = _mapSynchedWith == MapSync.WITH_SLIDER
            || _mapSynchedWith == MapSync.WITH_SLIDER_CENTERED;

      if (isSyncWithSlider == false) {

         map25.post(() -> {

            // create outside isSynch that data are available when map is zoomed to show the whole tour
            _boundingBox = createBoundingBox(_allGeoPoints);

            if (_mapSynchedWith == MapSync.WITH_TOUR) {

//					final int animationTime = Map25ConfigManager.getActiveTourTrackConfig().animationTime;
               final int animationTime = Map25ConfigManager.DEFAULT_ANIMATION_TIME;
               Map25LocationManager.setMapLocation(map25, _boundingBox, animationTime);
            }

            map25.updateMap();
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

// SET_FORMATTING_OFF

      final Map25TrackConfig tourTrackConfig = Map25ConfigManager.getActiveTourTrackConfig();

      /*
       * Layer
       */

      // tour layer
      _isShowTour       = Util.getStateBoolean(_state, STATE_IS_LAYER_TOUR_VISIBLE, true);
      _actionShowTourOptions.setSelection(_isShowTour);
      _map25App.getLayer_Tour().setEnabled(_isShowTour);

      // track color
      final MapGraphId trackGraphId = tourTrackConfig.gradientColorGraphID;
      restoreState_TrackColorActions(trackGraphId);
      setColorProvider(trackGraphId, false);

      // tour marker layer
      final boolean isMarkerVisible = Util.getStateBoolean(_state, STATE_IS_LAYER_MARKER_VISIBLE, true);
      _actionShowMarkerOptions.setSelected(isMarkerVisible);
      _map25App.getLayer_TourMarker().setEnabled(isMarkerVisible);

      // photo_layer
      final boolean isPhotoVisible = Util.getStateBoolean(_state, STATE_IS_LAYER_PHOTO_VISIBLE, true);
      _map25App.setPhoto_IsShowTitle  (Util.getStateBoolean(_state, STATE_IS_LAYER_PHOTO_TITLE_VISIBLE, true));
      _map25App.setPhoto_IsScaled     (Util.getStateBoolean(_state, STATE_IS_LAYER_PHOTO_SCALED, true));
      _map25App.setPhoto_IsVisible    (isPhotoVisible);
      _map25App.setPhoto_Size         (Util.getStateInt(_state, STATE_LAYER_PHOTO_SIZE, SlideoutMap25_PhotoOptions.IMAGE_SIZE_MINIMUM));

      _actionShowPhotoOptions.setSelection(isPhotoVisible);
      _map25App.getLayer_Photo().setEnabled(isPhotoVisible);

      _isPhotoFilterActive             = Util.getStateBoolean(_state, STATE_IS_PHOTO_FILTER_ACTIVE, false);
      _photoFilter_RatingStars         = Util.getStateInt(_state, STATE_PHOTO_FILTER_RATING_STARS, 0);
      _photoFilter_RatingStar_Operator = Util.getStateEnum(_state, STATE_PHOTO_FILTER_RATING_STAR_OPERATOR, PhotoRatingStarOperator.HAS_ANY);
      _actionMapPhotoFilter.setSelection(_isPhotoFilterActive);
      _actionMapPhotoFilter.getPhotoFilterSlideout().restoreState(_photoFilter_RatingStars, _photoFilter_RatingStar_Operator);

      // hillshading layer
      final int layerHillshadingOpacity         = Util.getStateInt(_state, STATE_LAYER_HILLSHADING_OPACITY, 255);
      final BitmapTileLayer layer_HillShading   = _map25App.getLayer_HillShading();
      layer_HillShading.setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_HILLSHADING_VISIBLE, true));
      layer_HillShading.setBitmapAlpha(layerHillshadingOpacity / 255.0f, true);
      _map25App.setLayer_HillShading_Options(layerHillshadingOpacity);

      // satellite maps
      _map25App.getLayer_Satellite()         .setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_SATELLITE_VISIBLE,    false));

      // cartography
      _map25App.getLayer_BaseMap()           .setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_BASE_MAP_VISIBLE,     true));

      // other layers
      _map25App.getLayer_CompassRose()      .setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_COMPASS_ROSE_VISIBLE, false));
      _map25App.getLayer_MapBookmark()      .setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_BOOKMARK_VISIBLE,      true));
      _map25App.getLayer_Legend()           .setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_LEGEND_VISIBLE,        true));
      _map25App.getLayer_ScaleBar()         .setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_SCALE_BAR_VISIBLE,     true));
      _map25App.getLayer_TileInfo()         .setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_TILE_INFO_VISIBLE,     false));

//      _map25App.getLayer_OpenGLTest()       .setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_OPEN_GL_TEST_VISIBLE,  false));

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
    * Select track color action
    *
    * @param trackGraphId
    */
   private void restoreState_TrackColorActions(final MapGraphId trackGraphId) {

      for (final Object action : _allTrackColorActions) {

         if (action instanceof ActionTrackColor) {

            final ActionTrackColor trackAction = (ActionTrackColor) action;

            trackAction.setSelection(trackGraphId == trackAction._graphId);

         } else if (action instanceof ActionTrackColor_HrZone) {

            final ActionTrackColor_HrZone trackAction = (ActionTrackColor_HrZone) action;

            trackAction.setChecked(trackGraphId == trackAction._graphId);
         }
      }
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

// SET_FORMATTING_OFF

      Util.setStateEnum(_state, STATE_MAP_SYNCHED_WITH,  _mapSynchedWith);

      // cartography
      _state.put(STATE_IS_LAYER_BASE_MAP_VISIBLE,     _map25App.getLayer_BaseMap().isEnabled());

      // other layers
      _state.put(STATE_IS_LAYER_BOOKMARK_VISIBLE,     _map25App.getLayer_MapBookmark().isEnabled());
      _state.put(STATE_IS_LAYER_COMPASS_ROSE_VISIBLE, _map25App.getLayer_CompassRose().isEnabled());
      _state.put(STATE_IS_LAYER_LEGEND_VISIBLE,       _map25App.getLayer_Legend().isEnabled());
      _state.put(STATE_IS_LAYER_MARKER_VISIBLE,       _map25App.getLayer_TourMarker().isEnabled());
      _state.put(STATE_IS_LAYER_SATELLITE_VISIBLE,    _map25App.getLayer_Satellite().isEnabled());
      _state.put(STATE_IS_LAYER_SCALE_BAR_VISIBLE,    _map25App.getLayer_ScaleBar().isEnabled());
      _state.put(STATE_IS_LAYER_TILE_INFO_VISIBLE,    _map25App.getLayer_TileInfo().isEnabled());
      _state.put(STATE_IS_LAYER_TOUR_VISIBLE,         _map25App.getLayer_Tour().isEnabled());

//      _state.put(STATE_IS_LAYER_OPEN_GL_TEST_VISIBLE, _map25App.getLayer_OpenGLTest().isEnabled());

      // photo layer
      _state.put(STATE_IS_LAYER_PHOTO_VISIBLE,        _map25App.isPhoto_Visible());
      _state.put(STATE_IS_LAYER_PHOTO_TITLE_VISIBLE,  _map25App.isPhoto_ShowTitle());
      _state.put(STATE_IS_LAYER_PHOTO_SCALED,         _map25App.isPhoto_Scaled());
      _state.put(STATE_LAYER_PHOTO_SIZE,              _map25App.getPhoto_Size());

      // hillshading layer
      _state.put(STATE_IS_LAYER_HILLSHADING_VISIBLE,  _map25App.getLayer_HillShading().isEnabled());
      _state.put(STATE_LAYER_HILLSHADING_OPACITY,     _map25App.getLayer_HillShading_Opacity());

      // photo filter
      _state.put(STATE_IS_PHOTO_FILTER_ACTIVE,        _actionMapPhotoFilter.getSelection());
      _state.put(STATE_PHOTO_FILTER_RATING_STARS,     _photoFilter_RatingStars);
      Util.setStateEnum(_state, STATE_PHOTO_FILTER_RATING_STAR_OPERATOR, _photoFilter_RatingStar_Operator);
      _actionMapPhotoFilter.getPhotoFilterSlideout().saveState();

// SET_FORMATTING_ON

      Map25ConfigManager.saveState();
      Map3GradientColorManager.saveColors();
   }

   /**
    * @param graphId
    */
   public void selectColorAction(final MapGraphId graphId) {

      for (final Object action : _allTrackColorActions) {

         if (action instanceof ActionTrackColor) {

            final ActionTrackColor colorAction = (ActionTrackColor) action;

            colorAction.setSelection(colorAction._graphId == graphId);

         } else if (action instanceof ActionTrackColor_HrZone) {

            final ActionTrackColor_HrZone colorAction = (ActionTrackColor_HrZone) action;

            colorAction.setChecked(colorAction._graphId == graphId);
         }
      }

      setColorProvider(graphId, true);
   }

   /**
    * @param trackGraphId
    * @param isPaintTours
    */
   private void setColorProvider(final MapGraphId trackGraphId, final boolean isPaintTours) {

      // update model
      Map25ConfigManager.getActiveTourTrackConfig().gradientColorGraphID = trackGraphId;

      _mapColorProvider = MapColorProvider.getActiveMap3ColorProvider(trackGraphId);

      _map25App.getLayer_Legend().setColorProvider(_mapColorProvider);

      if (isPaintTours) {
         paintTours();
      }
   }

   @Override
   public void setFocus() {

      // activate map
      _swtContainer.setFocus();
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

      if (tourData != null
            && tourData.latitudeSerie != null
            && tourData.latitudeSerie.length > 0) {

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

      final Map map25 = _map25App.getMap();
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

               Map25LocationManager.setMapLocation(map25, sliderBBox, 500);
            }
         });
      }
   }

   @Override
   public void syncMapWithOtherMap(final MapPosition syncMapPosition,
                                   final ViewPart viewPart,
                                   final IMapSyncListener.SyncParameter syncParameter) {

      if (_mapSynchedWith != MapSync.WITH_OTHER_MAP) {

         // sync feature is disabled

         return;
      }

      if (viewPart == this || !_isPartVisible) {

         // event is fired from this map -> ignore

         return;
      }

      final long currentTimeMillis = System.currentTimeMillis();

      _lastReceivedSyncEventTime = currentTimeMillis;

      final long timeDiffLastFiredSync = currentTimeMillis - _lastFiredSyncEventTime;
      if (timeDiffLastFiredSync < 1000

            // accept all sync events from the model player
            && (viewPart instanceof ModelPlayerView) == false) {

         // ignore because it causes LOTS of problems when synching moved map
         return;
      }

      final Map map = _map25App.getMap();
      map.getMapPosition(_currentMapPosition);

      if (syncParameter == SyncParameter.SHOW_MAP_POSITION_WITHOUT_ANIMATION) {

         // set map position without animation

         // update only map position x/y values and keep any other values from the current map position
         _currentMapPosition.x = syncMapPosition.x;
         _currentMapPosition.y = syncMapPosition.y;

         _map25App.getMap().setMapPosition(_currentMapPosition);

      } else {

         // sync map with animation

         /**
          * Set values which are not set
          */
         if (syncMapPosition.scale <= 1) {
            syncMapPosition.setScale(_currentMapPosition.scale);
         }

         if (syncMapPosition.bearing == 0) {
            syncMapPosition.bearing = _currentMapPosition.bearing;
         }

         if (syncMapPosition.tilt == 0) {
            syncMapPosition.tilt = _currentMapPosition.tilt;
         }

         Map25LocationManager.setMapLocation(map, syncMapPosition);
      }
   }

   private void updateFilteredPhotos() {

      runPhotoFilter();

      _map25App.updateLayer_Photos();
      _map25App.updateMap();

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
