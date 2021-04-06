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
package net.tourbook.map3.view;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.InputHandler;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.view.BasicView;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

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
import net.tourbook.common.color.ColorUtil;
import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.color.MapUnits;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.util.SWTPopupOverAWT;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.extension.upload.ActionUpload;
import net.tourbook.map.IMapSyncListener;
import net.tourbook.map.MapColorProvider;
import net.tourbook.map.MapManager;
import net.tourbook.map.bookmark.ActionMapBookmarks;
import net.tourbook.map.bookmark.IMapBookmarkListener;
import net.tourbook.map.bookmark.IMapBookmarks;
import net.tourbook.map.bookmark.MapBookmark;
import net.tourbook.map.bookmark.MapBookmarkManager;
import net.tourbook.map.bookmark.MapLocation;
import net.tourbook.map.bookmark.MapPosition_with_MarkerPosition;
import net.tourbook.map2.view.IDiscreteColorProvider;
import net.tourbook.map2.view.SelectionMapPosition;
import net.tourbook.map3.Messages;
import net.tourbook.map3.action.ActionMap3Color;
import net.tourbook.map3.action.ActionOpenMap3StatisticsView;
import net.tourbook.map3.action.ActionSetTrackSliderPositionLeft;
import net.tourbook.map3.action.ActionSetTrackSliderPositionRight;
import net.tourbook.map3.action.ActionShowDirectionArrows;
import net.tourbook.map3.action.ActionShowEntireTour;
import net.tourbook.map3.action.ActionShowLegend;
import net.tourbook.map3.action.ActionShowMap3Layer;
import net.tourbook.map3.action.ActionShowMarker;
import net.tourbook.map3.action.ActionShowTourInMap3;
import net.tourbook.map3.action.ActionShowTrackSlider;
import net.tourbook.map3.action.ActionSyncMap3WithOtherMap;
import net.tourbook.map3.action.ActionSyncMapWithChartSlider;
import net.tourbook.map3.action.ActionSyncMapWithTour;
import net.tourbook.map3.action.ActionTourColor;
import net.tourbook.map3.layer.MarkerLayer;
import net.tourbook.map3.layer.TourInfoLayer;
import net.tourbook.map3.layer.TrackPointAnnotation;
import net.tourbook.map3.layer.TrackSliderLayer;
import net.tourbook.map3.layer.tourtrack.ITrackPath;
import net.tourbook.map3.layer.tourtrack.TourMap3Position;
import net.tourbook.map3.layer.tourtrack.TourTrackConfig;
import net.tourbook.map3.layer.tourtrack.TourTrackConfigManager;
import net.tourbook.map3.layer.tourtrack.TourTrackLayer;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageMap3Color;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.printing.ActionPrint;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.oscim.core.MapPosition;

/**
 * Display 3-D map with tour tracks.
 */
public class Map3View extends ViewPart implements ITourProvider, IMapBookmarks, IMapBookmarkListener, IMapSyncListener {

   private static final String              GRAPH_LABEL_HEARTBEAT_UNIT             = net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit;

   private static final String              SLIDER_TEXT_ALTITUDE                   = "%.1f %s";                                              //$NON-NLS-1$
   private static final String              SLIDER_TEXT_GRADIENT                   = "%.1f %%";                                              //$NON-NLS-1$
   private static final String              SLIDER_TEXT_PACE                       = "%s %s";                                                //$NON-NLS-1$
   private static final String              SLIDER_TEXT_PULSE                      = "%.0f %s";                                              //$NON-NLS-1$
   private static final String              SLIDER_TEXT_SPEED                      = "%.1f %s";                                              //$NON-NLS-1$

   public static final String               ID                                     = "net.tourbook.map3.view.Map3ViewId";                    //$NON-NLS-1$

   private static final String              STATE_IS_LEGEND_VISIBLE                = "STATE_IS_LEGEND_VISIBLE";                              //$NON-NLS-1$
   private static final String              STATE_IS_MARKER_VISIBLE                = "STATE_IS_MARKER_VISIBLE";                              //$NON-NLS-1$
   private static final String              STATE_IS_SYNC_MAP_VIEW_WITH_TOUR       = "STATE_IS_SYNC_MAP_VIEW_WITH_TOUR";                     //$NON-NLS-1$
   private static final String              STATE_IS_SYNC_MAP_POSITION_WITH_SLIDER = "STATE_IS_SYNC_MAP_POSITION_WITH_SLIDER";               //$NON-NLS-1$
   private static final String              STATE_IS_SYNC_MAP3_WITH_OTHER_MAP      = "STATE_IS_SYNC_MAP3_WITH_OTHER_MAP";                    //$NON-NLS-1$
   private static final String              STATE_IS_TOUR_VISIBLE                  = "STATE_IS_TOUR_VISIBLE";                                //$NON-NLS-1$
   private static final String              STATE_IS_TRACK_SLIDER_VISIBLE          = "STATE_IS_TRACK_SLIDERVISIBLE";                         //$NON-NLS-1$
   private static final String              STATE_MAP3_VIEW                        = "STATE_MAP3_VIEW";                                      //$NON-NLS-1$
   private static final String              STATE_TOUR_COLOR_ID                    = "STATE_TOUR_COLOR_ID";                                  //$NON-NLS-1$

   private static final WorldWindowGLCanvas _wwCanvas                              = Map3Manager.getWWCanvas();

   private final IPreferenceStore           _prefStore                             = TourbookPlugin.getPrefStore();
   private final IPreferenceStore           _prefStore_Common                      = CommonActivator.getPrefStore();
   private final IDialogSettings            _state                                 = TourbookPlugin.getState(getClass().getCanonicalName());

   // SET_FORMATTING_ON

   private ActionMap3Color                   _actionMap3Color;
   private ActionOpenPrefDialog              _actionMap3Colors;
   private ActionMapBookmarks                _actionMapBookmarks;
//	private ActionOpenGLVersions					_actionOpenGLVersions;
   private ActionOpenMap3StatisticsView      _actionOpenMap3StatisticsView;
   private ActionSetTrackSliderPositionLeft  _actionSetTrackSliderLeft;
   private ActionSetTrackSliderPositionRight _actionSetTrackSliderRight;
   private ActionShowTrackSlider             _actionShowTrackSlider;
   private ActionShowDirectionArrows         _actionShowDirectionArrows;
   private ActionShowEntireTour              _actionShowEntireTour;
   private ActionShowLegend                  _actionShowLegendInMap;
   private ActionShowMap3Layer               _actionShowMap3Layer;
   private ActionShowMarker                  _actionShowMarker;
   private ActionShowTourInMap3              _actionShowTourInMap;
   private ActionSyncMapWithChartSlider      _actionSyncMap_WithChartSlider;
   private ActionSyncMap3WithOtherMap        _actionSyncMap_WithOtherMap;
   private ActionSyncMapWithTour             _actionSyncMap_WithTour;
   private ActionTourColor                   _actionTourColorAltitude;
   private ActionTourColor                   _actionTourColorGradient;
   private ActionTourColor                   _actionTourColorPulse;
   private ActionTourColor                   _actionTourColorSpeed;
   private ActionTourColor                   _actionTourColorPace;
   private ActionTourColor                   _actionTourColorHrZone;
   private ArrayList<ActionTourColor>        _allColorActions;
   //
   // context menu actions
   private ActionEditQuick                _actionEditQuick;
   private ActionEditTour                 _actionEditTour;
   private ActionExport                   _actionExportTour;
   private ActionOpenAdjustAltitudeDialog _actionOpenAdjustAltitudeDialog;
   private ActionOpenMarkerDialog         _actionOpenMarkerDialog;
   private ActionOpenTour                 _actionOpenTour;
   private ActionPrint                    _actionPrintTour;
   private ActionUpload                   _actionUploadTour;
   //
   private IPartListener2                 _partListener;
   private ISelectionListener             _postSelectionListener;
   private IPropertyChangeListener        _prefChangeListener;
   private IPropertyChangeListener        _prefChangeListener_Common;
   private ITourEventListener             _tourEventListener;
   //
   private MouseAdapter                   _wwMouseListener;
   private MouseAdapter                   _wwMouseMotionListener;
   private MouseAdapter                   _wwMouseWheelListener;
   //
   private boolean                        _isPartVisible;
   private boolean                        _isRestored;
   private boolean                        _isContextMenuVisible;
   //
   private ISelection                     _lastHiddenSelection;
   //
   private boolean                        _isMapSynched_WithChartSlider;
   private boolean                        _isMapSynched_WithOtherMap;
   private boolean                        _isMapSynched_WithTour;
   private long                           _lastFiredSyncEventTime;
   //
   /**
    * Contains all tours which are displayed in the map.
    */
   private ArrayList<TourData>            _allTours   = new ArrayList<>();
   //
   private int                            _allTourIdHash;
   private int                            _allTourDataHash;

   /**
    * Color id for the currently displayed tour tracks.
    */
   private MapGraphId                     _graphId;

   private OpenDialogManager              _openDlgMgr = new OpenDialogManager();

   /*
    * current position for the x-sliders (vertical slider)
    */
   private int        _currentLeftSliderValueIndex;
   private int        _currentRightSliderValueIndex;
   private Position   _currentTrackInfoSliderPosition;
   //
   private ITrackPath _currentHoveredTrack;
   private Integer    _currentHoveredTrackPosition;

   /*
    * UI controls
    */
   private Composite _parent;
   private Composite _mapContainer;
   private Frame     _awtFrame;
   private Menu      _swtContextMenu;

   private class Map3ContextMenu extends SWTPopupOverAWT {

      public Map3ContextMenu(final Display display, final Menu swtContextMenu) {
         super(display, swtContextMenu);
      }

   }

   public Map3View() {}

   /**
    * @param eyePosition
    * @return Returns altitude offset depending on the configuration settings.
    */
   public static double getAltitudeOffset(final Position eyePosition) {

      if (eyePosition == null) {
         return 0;
      }

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

      final boolean isAbsoluteAltitudeMode = config.altitudeMode == WorldWind.ABSOLUTE;
      final boolean isAltitudeOffset = config.isAltitudeOffset;
      final boolean isOffsetModeAbsolute =
            config.altitudeOffsetMode == TourTrackConfigManager.ALTITUDE_OFFSET_MODE_ABSOLUTE;
      final boolean isOffsetModeRelative =
            config.altitudeOffsetMode == TourTrackConfigManager.ALTITUDE_OFFSET_MODE_RELATIVE;

      final int relativeOffset = config.altitudeOffsetDistanceRelative;

      double altitudeOffset = 0;

      if (isAbsoluteAltitudeMode && isAltitudeOffset) {

         if (isOffsetModeAbsolute) {

            altitudeOffset = config.altitudeOffsetDistanceAbsolute;

         } else if (isOffsetModeRelative && relativeOffset > 0) {

            final double eyeElevation = eyePosition.getElevation();

            altitudeOffset = eyeElevation / 100.0 * relativeOffset;
         }

         if (config.isAltitudeOffsetRandom) {

            // this needs to be implemented, is not yet done
         }
      }

      return altitudeOffset;
   }

   public void actionOpenTrackColorDialog() {

      // set color before menu is filled, this sets the action image and color provider
      _actionMap3Color.setColorId(_graphId);

      _actionMap3Color.run();
   }

   public void actionSetMapColor(final MapGraphId graphId) {

      _graphId = graphId;

      setColorProvider(graphId);
   }

   public void actionSetTrackSlider(final boolean isLeftSlider) {

      if (_currentHoveredTrack == null || _currentHoveredTrackPosition == null) {
         return;
      }

      final TourData tourData = _currentHoveredTrack.getTourTrack().getTourData();

      TourChart tourChart = null;
      final TourChart activeTourChart = TourManager.getInstance().getActiveTourChart();

      if ((activeTourChart != null)
            && (activeTourChart.isDisposed() == false)
            && (activeTourChart.getTourData() == tourData)) {

         tourChart = activeTourChart;
      }

      final int serieIndex0 = SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;
      int serieIndexLeft = SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;
      int serieIndexRight = SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;

      if (isLeftSlider) {

         serieIndexLeft = _currentHoveredTrackPosition;
         _currentLeftSliderValueIndex = _currentHoveredTrackPosition;

      } else {

         serieIndexRight = _currentHoveredTrackPosition;
         _currentRightSliderValueIndex = _currentHoveredTrackPosition;
      }

      ISelection sliderSelection = null;

      if (tourChart == null) {

         // chart is not available, fire a map position

         final double[] serieLatitude = tourData.latitudeSerie;

         if ((serieLatitude != null) && (serieLatitude.length > 0)) {

            // map position is available

            sliderSelection = new SelectionMapPosition(tourData, serieIndexLeft, serieIndexRight, true);
         }

      } else {

         final SelectionChartXSliderPosition xSliderSelection = new SelectionChartXSliderPosition(
               tourChart,
               serieIndex0,
               serieIndexLeft);

         xSliderSelection.setCenterSliderPosition(true);

         sliderSelection = xSliderSelection;
      }

      if (sliderSelection != null) {

         updateTrackSlider_10_Position(//
               tourData,
               _currentLeftSliderValueIndex,
               _currentRightSliderValueIndex);

         TourManager.fireEventWithCustomData(//
               TourEventId.SLIDER_POSITION_CHANGED,
               sliderSelection,
               Map3View.this);

         setTrackSliderVisible(true);
      }
   }

   public void actionShowDirectionArrows(final boolean isVisible) {

      // update model
      TourTrackConfigManager.getActiveConfig().isShowDirectionArrows = isVisible;

      // update UI, invalidate cached data and force a redraw
      Map3Manager.getLayer_TourTrack().setExpired();
   }

   public void actionShowLegend(final boolean isLegendVisible) {

      Map3Manager.setLayerVisible_Legend(isLegendVisible);
   }

   public void actionShowMarker(final boolean isMarkerVisible) {

      Map3Manager.setLayerVisible_Marker(isMarkerVisible);

      updateUI_CreateMarker();

      Map3Manager.redrawMap();
   }

   public void actionShowTour(final boolean isTrackVisible) {

      Map3Manager.setLayerVisible_TourTrack(isTrackVisible);

      showAllTours_InternalTours();
   }

   public void actionShowTrackSlider(final boolean isVisible) {

      setTrackSliderVisible(isVisible);
   }

   public void actionSynch_WithChartSlider() {

      _isMapSynched_WithChartSlider = _actionSyncMap_WithChartSlider.isChecked();

      if (_isMapSynched_WithChartSlider) {

         deactivateMapSync();

         // ensure that the track sliders are displayed
         _actionShowTrackSlider.setChecked(true);
      }
   }

   public void actionSynch_WithOtherMap() {

      _isMapSynched_WithOtherMap = _actionSyncMap_WithOtherMap.isChecked();

      if (_isMapSynched_WithOtherMap) {

         deactivateTourSync();
         deactivateSliderSync();
      }
   }

   public void actionSynch_WithTour() {

      _isMapSynched_WithTour = _actionSyncMap_WithTour.isChecked();

      if (_isMapSynched_WithTour) {

         deactivateMapSync();

         showAllTours_InternalTours();
      }
   }

   public void actionZoomShowEntireTour() {

      showAllTours(true);
   }

   private void addMap3Listener() {

      _wwMouseListener = new MouseAdapter() {

         @Override
         public void mouseClicked(final MouseEvent e) {
            onAWTMouseClick(e);
         }
      };

      _wwMouseMotionListener = new MouseAdapter() {

         @Override
         public void mouseDragged(final MouseEvent e) {
            onAWTMouseDragged(e);
         }

      };
      _wwMouseWheelListener = new MouseAdapter() {

         @Override
         public void mouseWheelMoved(final MouseWheelEvent e) {
            onAWTMouseDragged(e);
         }

      };

      final InputHandler inputHandler = _wwCanvas.getInputHandler();

      inputHandler.addMouseListener(_wwMouseListener);
      inputHandler.addMouseMotionListener(_wwMouseMotionListener);
      inputHandler.addMouseWheelListener(_wwMouseWheelListener);

      /*
       * Statistics
       */
//		if (_isLogStatistics) {
//
//			_wwCanvas.setPerFrameStatisticsKeys(PerformanceStatistic.ALL_STATISTICS_SET);
//			_wwStatisticListener = new RenderingListener() {
//
//				@Override
//				public void stageChanged(final RenderingEvent event) {
//
//					final long now = System.currentTimeMillis();
//
//					final String stage = event.getStage();
//
//					if (stage.equals(RenderingEvent.AFTER_BUFFER_SWAP)
//							&& event.getSource() instanceof WorldWindow
//							&& now - _statisticLastUpdate > _statisticUpdateInterval) {
//
//						EventQueue.invokeLater(new Runnable() {
//							public void run() {
//								updateStatistics();
//							}
//						});
//
//						_statisticLastUpdate = now;
//					}
//				}
//			};
//
//			_wwCanvas.addRenderingListener(_wwStatisticListener);
//		}
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {

         private void onPartVisible(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == Map3View.this) {

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
            if (partRef.getPart(false) == Map3View.this) {
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

            if (property.equals(ITourbookPreferences.MAP3_COLOR_IS_MODIFIED)) {

               // update map colors

               setColorProvider(_graphId);
            }
         }
      };

      _prefChangeListener_Common = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

               _actionShowTourInMap.updateMeasurementSystem();
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   /**
    * listen for events when a tour is selected
    */
   private void addSelectionListener() {

      _postSelectionListener = new ISelectionListener() {
         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

            if (part == Map3View.this) {
               // ignore own selections
               return;
            }

            onSelectionChanged(selection);
         }
      };
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == Map3View.this) {
               return;
            }

            if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

               showAllTours_InternalTours();

            } else if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

               final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
               if ((modifiedTours != null) && (modifiedTours.size() > 0)) {
                  updateModifiedTours(modifiedTours);
               }

            } else if (eventId == TourEventId.UPDATE_UI || eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

               clearView();

            } else if (eventId == TourEventId.MARKER_SELECTION) {

               if (eventData instanceof SelectionTourMarker) {

                  final SelectionTourMarker selection = (SelectionTourMarker) eventData;

                  final TourData tourData = selection.getTourData();
                  final ArrayList<TourMarker> tourMarker = selection.getSelectedTourMarker();

                  syncMapWith_TourMarker(tourData, tourMarker);
               }

            } else if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);

            } else if (eventId == TourEventId.SLIDER_POSITION_CHANGED && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   /**
    * @param colorId
    * @param selectedToolItem
    */
   public void checkSelectedColorActions(final MapGraphId colorId, final ToolItem selectedToolItem) {

      final boolean isChecked = selectedToolItem.getSelection();

      if (colorId == _graphId) {

         // active color is selected

         if (isChecked == false) {

            // prevent unchecking selected color
            selectedToolItem.setSelection(true);
         }

         return;
      }

      // a new color is selected, uncheck previous color
      for (final ActionTourColor colorAction : _allColorActions) {

         final ToolItem actionToolItem = colorAction.getToolItem();

         // unckeck other colors
         if (actionToolItem != selectedToolItem) {
            actionToolItem.setSelection(false);
         }
      }
   }

   private void cleanupOldTours() {

      _currentTrackInfoSliderPosition = null;
      _currentHoveredTrack = null;
      _currentHoveredTrackPosition = null;

//		_postSelectionProvider.clearSelection();

      _allTours.clear();
   }

   private void clearView() {

      cleanupOldTours();

      final TrackSliderLayer trackSliderLayer = getLayerTrackSlider();
      if (trackSliderLayer != null) {
         trackSliderLayer.setSliderVisible(false);
      }

      showAllTours_InternalTours();
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

   /**
    * Compute a center position from an eye position and an orientation. If the view is looking at
    * the earth, the center position is the intersection point of the globe and a ray beginning at
    * the eye point, in the direction of the forward vector. If the view is looking at the horizon,
    * the center position is the eye position. Otherwise, the center position is null.
    *
    * @param eyePosition
    *           The eye position.
    * @param forward
    *           The forward vector.
    * @param pitch
    *           View pitch.
    * @param altitudeMode
    *           Altitude mode of {@code eyePosition}.
    * @return The center position of the view.
    */
   protected Position computeCenterPosition(final Position eyePosition,
                                            final Vec4 forward,
                                            final Angle pitch,
                                            final int altitudeMode) {
      double height;

      final Angle latitude = eyePosition.getLatitude();
      final Angle longitude = eyePosition.getLongitude();

      final Globe globe = _wwCanvas.getModel().getGlobe();

      if (altitudeMode == WorldWind.CLAMP_TO_GROUND) {
         height = globe.getElevation(latitude, longitude);
      } else if (altitudeMode == WorldWind.RELATIVE_TO_GROUND) {
         height = globe.getElevation(latitude, longitude) + eyePosition.getAltitude();
      } else {
         height = eyePosition.getAltitude();
      }

      final Vec4 eyePoint = globe.computePointFromPosition(new Position(latitude, longitude, height));

      // Find the intersection of the globe and the camera's forward vector. Looking at the horizon (tilt == 90)
      // is a special case because it is a valid view, but the view vector does not intersect the globe.
      Position lookAtPosition;
      final double tolerance = 0.001;
      if (Math.abs(pitch.degrees - 90.0) > tolerance) {
         lookAtPosition = globe.getIntersectionPosition(new Line(eyePoint, forward));
      } else {
         lookAtPosition = globe.computePositionFromPoint(eyePoint);
      }

      return lookAtPosition;
   }

   private void createActions(final Composite parent) {

//		_actionOpenGLVersions = new ActionOpenGLVersions();
      _actionOpenMap3StatisticsView = new ActionOpenMap3StatisticsView();

      _actionMap3Color = new ActionMap3Color();
      _actionMap3Colors = new ActionOpenPrefDialog(Messages.Map3_Action_TrackColors, PrefPageMap3Color.ID, _graphId);
      _actionMap3Colors.setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Options));

      _actionMapBookmarks = new ActionMapBookmarks(_parent, this);
      _actionSetTrackSliderLeft = new ActionSetTrackSliderPositionLeft(this);
      _actionSetTrackSliderRight = new ActionSetTrackSliderPositionRight(this);
      _actionShowTrackSlider = new ActionShowTrackSlider(this);
      _actionShowDirectionArrows = new ActionShowDirectionArrows(this);
      _actionShowEntireTour = new ActionShowEntireTour(this);
      _actionShowLegendInMap = new ActionShowLegend(this);
      _actionShowMap3Layer = new ActionShowMap3Layer(this, parent);
      _actionShowMarker = new ActionShowMarker(this);
      _actionShowTourInMap = new ActionShowTourInMap3(this, parent);
      _actionSyncMap_WithChartSlider = new ActionSyncMapWithChartSlider(this);
      _actionSyncMap_WithOtherMap = new ActionSyncMap3WithOtherMap(this);
      _actionSyncMap_WithTour = new ActionSyncMapWithTour(this);

      _actionTourColorAltitude = ActionTourColor.createAction(MapGraphId.Altitude, this, parent);
      _actionTourColorGradient = ActionTourColor.createAction(MapGraphId.Gradient, this, parent);
      _actionTourColorPace = ActionTourColor.createAction(MapGraphId.Pace, this, parent);
      _actionTourColorPulse = ActionTourColor.createAction(MapGraphId.Pulse, this, parent);
      _actionTourColorSpeed = ActionTourColor.createAction(MapGraphId.Speed, this, parent);
      _actionTourColorHrZone = ActionTourColor.createAction(MapGraphId.HrZone, this, parent);

      _allColorActions = new ArrayList<>();
      _allColorActions.add(_actionTourColorAltitude);
      _allColorActions.add(_actionTourColorGradient);
      _allColorActions.add(_actionTourColorPace);
      _allColorActions.add(_actionTourColorPulse);
      _allColorActions.add(_actionTourColorSpeed);
      _allColorActions.add(_actionTourColorHrZone);

      // context menu actions
      _actionEditQuick = new ActionEditQuick(this);
      _actionEditTour = new ActionEditTour(this);
      _actionExportTour = new ActionExport(this);
      _actionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(this);
      _actionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
      _actionOpenTour = new ActionOpenTour(this);
      _actionPrintTour = new ActionPrint(this);
      _actionUploadTour = new ActionUpload(this);
   }

   /**
    * Context menu with net.tourbook.common.util.SWTPopupOverAWT
    *
    * @param xPosScreen
    * @param yPosScreen
    */
   private void createContextMenu(final int xPosScreen, final int yPosScreen) {

      disposeContextMenu();

      _swtContextMenu = new Menu(_mapContainer);

      // Add listener to repopulate the menu each time
      _swtContextMenu.addMenuListener(new MenuAdapter() {

         boolean _isFilled;

         @Override
         public void menuHidden(final MenuEvent e) {

            _isContextMenuVisible = false;

            /*
             * run async that the context state and tour info reset is done after the context menu
             * actions has done they tasks
             */
            Display.getCurrent().asyncExec(new Runnable() {
               @Override
               public void run() {

                  hideTourInfo();
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

            _isContextMenuVisible = true;
         }
      });

      final Display display = _mapContainer.getDisplay();

      final Map3ContextMenu swt_awt_ContextMenu = new Map3ContextMenu(display, _swtContextMenu);

      display.asyncExec(new Runnable() {
         @Override
         public void run() {
//				System.out.println("SWT calling menu"); //$NON-NLS-1$
            swt_awt_ContextMenu.swtIndirectShowMenu(xPosScreen, yPosScreen);
         }
      });
   }

   @Override
   public void createPartControl(final Composite parent) {

      _parent = parent;

      createUI(parent);

      addPartListener();
      addPrefListener();
      addSelectionListener();
      addTourEventListener();
      addMap3Listener();
      MapBookmarkManager.addBookmarkListener(this);
      MapManager.addMapSyncListener(this);

      createActions(parent);
      fillActionBars();

      // set selection provider
//		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

//		getViewSite().registerContextMenu(menuManager, selectionProvider)

      Map3Manager.setMap3View(this);

      /*
       * !!! It requires 2x asyncExec that the a tour provider is providing tours !!!
       */
      Display.getCurrent().asyncExec(new Runnable() {
         @Override
         public void run() {

            restoreState();
            enableActions();

            _isRestored = true;

            if (_lastHiddenSelection != null) {

               onSelectionChanged(_lastHiddenSelection);

               _lastHiddenSelection = null;

            } else if (_allTours.isEmpty()) {

               // a tour is not displayed, find a tour provider which provides a tour
               showToursFromTourProvider();

            } else {

               showAllTours_InternalTours();
            }
         }
      });
   }

   private String createSliderText(final int positionIndex, final TourData tourData) {

      String graphValueText = null;

      switch (_graphId) {

      case Altitude:

         final float[] altitudeSerie = tourData.altitudeSerie;
         if (altitudeSerie != null) {

            final float altitudeMetric = altitudeSerie[positionIndex];
            final float altitude = altitudeMetric / UI.UNIT_VALUE_ELEVATION;

            graphValueText = String.format(SLIDER_TEXT_ALTITUDE, altitude, UI.UNIT_LABEL_ELEVATION);
         }
         break;

      case Gradient:

         final float[] gradientSerie = tourData.gradientSerie;
         if (gradientSerie != null) {
            graphValueText = String.format(SLIDER_TEXT_GRADIENT, gradientSerie[positionIndex]);
         }
         break;

      case Pace:

         final float[] paceSerie = tourData.getPaceSerieSeconds();
         if (paceSerie != null) {
            final float pace = paceSerie[positionIndex];
            graphValueText = String.format(//
                  SLIDER_TEXT_PACE,
                  UI.format_mm_ss((long) pace),
                  UI.UNIT_LABEL_PACE);
         }
         break;

      case HrZone:
      case Pulse:

         final float[] pulseSerie = tourData.pulseSerie;
         if (pulseSerie != null) {
            graphValueText = String
                  .format(SLIDER_TEXT_PULSE, pulseSerie[positionIndex], GRAPH_LABEL_HEARTBEAT_UNIT);
         }

         break;

      case Speed:

         final float[] speedSerie = tourData.getSpeedSerie();
         if (speedSerie != null) {
            graphValueText = String.format(SLIDER_TEXT_SPEED, speedSerie[positionIndex], UI.UNIT_LABEL_SPEED);
         }
         break;

      default:
         break;
      }

      if (graphValueText != null) {
         return graphValueText;
      } else {
         return UI.EMPTY_STRING;
      }
   }

   private void createUI(final Composite parent) {

      // build GUI: container(SWT) -> Frame(AWT) -> Panel(AWT) -> WorldWindowGLCanvas(AWT)
      _mapContainer = new Composite(parent, SWT.EMBEDDED);
      GridDataFactory.fillDefaults().applyTo(_mapContainer);
      {
         _awtFrame = SWT_AWT.new_Frame(_mapContainer);
         final java.awt.Panel awtPanel = new java.awt.Panel(new java.awt.BorderLayout());

         _awtFrame.add(awtPanel);
         awtPanel.add(_wwCanvas, BorderLayout.CENTER);

      }

      _awtFrame.addComponentListener(new ComponentAdapter() {

         @Override
         public void componentResized(final ComponentEvent e) {
            Map3Manager.getLayer_TourLegend().resizeLegendImage();
         }
      });

//		_mapContainer.addControlListener(new ControlAdapter() {
//
//			@Override
//			public void controlResized(final ControlEvent e) {
//				Map3Manager.getTourLegendLayer().resizeLegendImage();
//			}
//
//		});

      parent.layout();
   }

   private void deactivateMapSync() {

      // disable map sync

      _isMapSynched_WithOtherMap = false;
      _actionSyncMap_WithOtherMap.setChecked(false);
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

      Map3Manager.setMap3View(null);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      getViewSite().getPage().removePostSelectionListener(_postSelectionListener);
      getViewSite().getPage().removePartListener(_partListener);

      MapBookmarkManager.removeBookmarkListener(this);
      MapManager.removeMapSyncListener(this);
      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      final InputHandler inputHandler = _wwCanvas.getInputHandler();
      inputHandler.removeMouseListener(_wwMouseListener);
      inputHandler.removeMouseMotionListener(_wwMouseMotionListener);
      inputHandler.removeMouseMotionListener(_wwMouseWheelListener);

      /*
       * !!! THIS WILL BLOCK THE UI !!!
       */
//		_awtFrame.dispose();

      disposeContextMenu();

      super.dispose();
   }

   private void disposeContextMenu() {

      if (_swtContextMenu != null) {
         _swtContextMenu.dispose();
      }
   }

   void enableActions() {

      final boolean isLegendVisible = Map3Manager.getLayer_TourLegend().isEnabled();
      final boolean isMarkerVisible = Map3Manager.getLayer_Marker().isEnabled();
      final boolean isTrackSliderVisible = Map3Manager.getLayer_TrackSlider().isEnabled();
      final boolean isTrackVisible = Map3Manager.getLayer_TourTrack().isEnabled();

      final boolean isTourAvailable = _allTours.size() > 0;
      final boolean canTourBeDisplayed = isTrackVisible && isTourAvailable;

      final boolean isPulsePresent = _allTours.stream().anyMatch(t -> t.pulseSerie != null);
      final boolean canShowHrZones = _allTours.stream().anyMatch(t -> t.getNumberOfHrZones() > 0) && isPulsePresent;
      final boolean isGradientPresent = _allTours.stream().anyMatch(t -> t.getGradientSerie() != null);
      final boolean isSpeedPresent = _allTours.stream().anyMatch(t -> t.getSpeedSerie() != null);
      final boolean isPacePresent = _allTours.stream().anyMatch(t -> t.getPaceSerie() != null);

      _actionTourColorAltitude.setEnabled(canTourBeDisplayed);
      _actionTourColorGradient.setEnabled(canTourBeDisplayed && isGradientPresent);
      _actionTourColorPace.setEnabled(canTourBeDisplayed && isPacePresent);
      _actionTourColorPulse.setEnabled(canTourBeDisplayed && isPulsePresent);
      _actionTourColorSpeed.setEnabled(canTourBeDisplayed && isSpeedPresent);
      _actionTourColorHrZone.setEnabled(canTourBeDisplayed && canShowHrZones);

      _actionShowEntireTour.setEnabled(canTourBeDisplayed);

      _actionSyncMap_WithChartSlider.setEnabled(isTrackSliderVisible && isTourAvailable);
      _actionSyncMap_WithTour.setEnabled(canTourBeDisplayed);

      // set checked/enabled
      _actionShowLegendInMap.setChecked(isLegendVisible);

      _actionShowTourInMap.setSelection(isTrackVisible);
      _actionShowTourInMap.setEnabled(isTourAvailable);

      _actionShowMarker.setChecked(isMarkerVisible && isTourAvailable);
      _actionShowMarker.setEnabled(isTourAvailable);

      _actionShowTrackSlider.setChecked(isTrackSliderVisible);
      _actionShowTrackSlider.setEnabled(isTrackSliderVisible && canTourBeDisplayed);
   }

   private void enableContextMenuActions() {

      final ITrackPath selectedTrack = Map3Manager.getLayer_TourTrack().getSelectedTrack();

      final boolean isTourSelected = selectedTrack != null;
      final boolean isTourAvailable = _allTours.size() > 0;
      final boolean isTrackPositionHovered = _currentHoveredTrack != null && _currentHoveredTrackPosition != null;

      _actionSetTrackSliderLeft.setEnabled(isTrackPositionHovered);
      _actionSetTrackSliderRight.setEnabled(isTrackPositionHovered);
      _actionShowTrackSlider.setEnabled(isTourAvailable);
      _actionShowLegendInMap.setEnabled(isTourAvailable);

      _actionMap3Color.setEnabled(isTourAvailable);

      _actionEditQuick.setEnabled(isTourSelected);
      _actionEditTour.setEnabled(isTourSelected);
      _actionOpenMarkerDialog.setEnabled(isTourSelected);
      _actionOpenAdjustAltitudeDialog.setEnabled(isTourSelected);
      _actionOpenTour.setEnabled(isTourSelected);
      _actionExportTour.setEnabled(isTourSelected);
      _actionPrintTour.setEnabled(isTourSelected);
      _actionUploadTour.setEnabled(isTourSelected);
   }

   private void fillActionBars() {

      final IActionBars actionBars = getViewSite().getActionBars();

      /*
       * fill view toolbar
       */
      final IToolBarManager tbm = actionBars.getToolBarManager();

      tbm.add(_actionTourColorAltitude);
      tbm.add(_actionTourColorPulse);
      tbm.add(_actionTourColorSpeed);
      tbm.add(_actionTourColorPace);
      tbm.add(_actionTourColorGradient);
      tbm.add(_actionTourColorHrZone);
      tbm.add(new Separator());

      tbm.add(_actionMapBookmarks);
      tbm.add(new Separator());

      tbm.add(_actionShowTourInMap);
      tbm.add(_actionShowEntireTour);
      tbm.add(_actionSyncMap_WithTour);
      tbm.add(_actionSyncMap_WithChartSlider);
      tbm.add(_actionSyncMap_WithOtherMap);
      tbm.add(new Separator());

      tbm.add(_actionShowMarker);
      tbm.add(_actionShowMap3Layer);

      /*
       * fill view menu
       */
      final IMenuManager menuMgr = actionBars.getMenuManager();

      menuMgr.add(_actionOpenMap3StatisticsView);

// this is NOT working any more :-(((
//		menuMgr.add(_actionOpenGLVersions);
   }

   private void fillContextMenu(final Menu menu) {

      // set color before menu is filled, this sets the action image and color id
      _actionMap3Color.setColorId(_graphId);

      fillMenuItem(menu, _actionSetTrackSliderLeft);
      fillMenuItem(menu, _actionSetTrackSliderRight);

      (new Separator()).fill(menu, -1);
      fillMenuItem(menu, _actionShowDirectionArrows);
      fillMenuItem(menu, _actionShowMarker);
      fillMenuItem(menu, _actionShowTrackSlider);
      fillMenuItem(menu, _actionShowLegendInMap);

      (new Separator()).fill(menu, -1);
      MapBookmarkManager.fillContextMenu_RecentBookmarks(menu, this);

      if (_graphId != MapGraphId.HrZone) {

         // hr zone has a different color provider and is not yet supported

         (new Separator()).fill(menu, -1);
         fillMenuItem(menu, _actionMap3Color);
      }
      fillMenuItem(menu, _actionMap3Colors);

      (new Separator()).fill(menu, -1);
      fillMenuItem(menu, _actionEditQuick);
      fillMenuItem(menu, _actionEditTour);
      fillMenuItem(menu, _actionOpenMarkerDialog);
      fillMenuItem(menu, _actionOpenAdjustAltitudeDialog);
      fillMenuItem(menu, _actionOpenTour);

//		_tagMenuMgr.fillTagMenu(menuMgr);
//
//		// tour type actions
//		fillMenuItem(menu, new Separator());
//		fillMenuItem(menu, _actionSetTourType);
//		TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, this, true);

      (new Separator()).fill(menu, -1);
      fillMenuItem(menu, _actionUploadTour);
      fillMenuItem(menu, _actionExportTour);
      fillMenuItem(menu, _actionPrintTour);

      enableContextMenuActions();
   }

   private void fillMenuItem(final Menu menu, final Action action) {

      final ActionContributionItem item = new ActionContributionItem(action);
      item.fill(menu, -1);
   }

   public void fireTourSelection(final ISelection selection) {

      // add slider position to the selection
      if (selection instanceof SelectionTourData) {

         final SelectionTourData tourDataSelection = (SelectionTourData) selection;

         tourDataSelection.setSliderValueIndex(_currentLeftSliderValueIndex, _currentRightSliderValueIndex);
      }

      // run in SWT thread
      _mapContainer.getDisplay().asyncExec(new Runnable() {
         @Override
         public void run() {

            // activate this view

            TourManager.fireEventWithCustomData(TourEventId.TOUR_SELECTION, selection, Map3View.this);
         }
      });
   }

   public ArrayList<TourData> getAllTours() {
      return _allTours;
   }

   private float getDataSerieValue(final float[] dataSerie, final int positionIndex, final float legendMinValue) {

      final float legendValue = dataSerie == null ? legendMinValue : dataSerie[positionIndex];

      return legendValue;
   }

   /**
    * @return Returns {@link TrackSliderLayer} or <code>null</code> when layer is not displayed.
    */
   private TrackSliderLayer getLayerTrackSlider() {

      final TrackSliderLayer layer = Map3Manager.getLayer_TrackSlider();

      if (layer.isEnabled() == false) {
         // layer is not displayed
         return null;
      }

      return layer;
   }

   @Override
   public MapLocation getMapLocation() {

      final MapPosition_with_MarkerPosition mapPosition = getMapPosition();

      if (mapPosition == null) {
         return null;
      }

      return new MapLocation(mapPosition);
   }

   private MapPosition_with_MarkerPosition getMapPosition() {

      final View view = _wwCanvas.getView();

      if (!(view instanceof BasicView)) {
         return null;
      }

      final BasicView basicView = (BasicView) view;
      final Position centerPosition = basicView.getCenterPosition();
      final Position eyePosition = basicView.getEyePosition();

      final Angle latitudeAngle = centerPosition.latitude;
      final Angle longitudeAngle = centerPosition.longitude;

      final double latitude = latitudeAngle.degrees;
      final double longitude = longitudeAngle.degrees;

      final GeoPosition geoCenter = new GeoPosition(latitude, longitude);

      final double eyeElevation = eyePosition.elevation;
      final double groundElevation = _wwCanvas.getModel().getGlobe().getElevation(latitudeAngle, longitudeAngle);
      final double elevation = eyeElevation - groundElevation;

      final double zoomLevel = 20 - Math.log(elevation);

      final MapPosition_with_MarkerPosition mapPosition = new MapLocation(geoCenter, (int) zoomLevel + 2).getMapPosition();

      mapPosition.bearing = -(float) basicView.getHeading().getDegrees();
      mapPosition.tilt = (float) basicView.getPitch().getDegrees();
      return mapPosition;
   }

   public java.awt.Rectangle getMapSize() {
      return _wwCanvas.getBounds();
   }

   /**
    * @param allTours
    * @return Returns only tours which can be displayed in the map (which contains geo coordinates).
    */
   private ArrayList<TourData> getMapTours(final ArrayList<TourData> allTours) {

      final ArrayList<TourData> mapTours = new ArrayList<>(allTours.size());

      for (final TourData tourData : allTours) {

         if (tourData == null) {

            // this occurred, probably when there is no tour in the db
            continue;
         }

         final double[] latitudeSerie = tourData.latitudeSerie;

         if (latitudeSerie != null && latitudeSerie.length > 0) {
            mapTours.add(tourData);
         }
      }

      return mapTours;
   }

   /**
    * @param trackSliderLayer
    * @return Returns {@link TourData} of the selected tour track or <code>null</code> when a tour
    *         is not selected.
    */
   private TourData getSelectedTour(final TrackSliderLayer trackSliderLayer) {

      TourData tourData;
      final ITrackPath selectedTrack = Map3Manager.getLayer_TourTrack().getSelectedTrack();

      if (selectedTrack == null) {

         if (_allTours.isEmpty()) {

            return null;

         } else {

            // a track is not selected, get first tour

            tourData = _allTours.get(0);
         }

      } else {

         // get selected tour

         tourData = selectedTrack.getTourTrack().getTourData();
      }

      return tourData;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      final ITrackPath selectedTrack = Map3Manager.getLayer_TourTrack().getSelectedTrack();

      if (selectedTrack != null) {

         final ArrayList<TourData> selectedTours = new ArrayList<>();

         selectedTours.add(selectedTrack.getTourTrack().getTourData());

         return selectedTours;
      }

      return null;
   }

   public Shell getShell() {
      return _mapContainer.getShell();
   }

   private double getSliderYPosition(final float trackAltitude, final Position eyePosition) {

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

      double sliderYPosition = 0;

      switch (config.altitudeMode) {
      case WorldWind.ABSOLUTE:

         sliderYPosition = trackAltitude + getAltitudeOffset(eyePosition);
         break;

      case WorldWind.RELATIVE_TO_GROUND:

         sliderYPosition = trackAltitude;
         break;

      default:

         // WorldWind.CLAMP_TO_GROUND -> y position = 0

         break;
      }

      return sliderYPosition;
   }

   public MapGraphId getTrackColorId() {
      return _graphId;
   }

   private void hideTourInfo() {

      _currentHoveredTrack = null;
      _currentHoveredTrackPosition = null;

      Map3Manager.getLayer_TourInfo().setTrackPointVisible(false);
   }

   public boolean isContextMenuVisible() {
      return _isContextMenuVisible;
   }

   @Override
   public void moveToMapLocation(final MapBookmark mapBookmark) {

      moveToMapLocation(mapBookmark.getMapPosition(), 0);
   }

   private void moveToMapLocation(final MapPosition mapPosition, final int positionFlags) {

      final int zoomLevel = mapPosition.zoomLevel + 0;
      final double latitude = mapPosition.getLatitude();
      final double longitude = mapPosition.getLongitude();

      final double zoomElevation = Math.pow(2 * 1.5, 20 - zoomLevel);

      final LatLon latlon = LatLon.fromDegrees(latitude, longitude);

      final double groundElevation = _wwCanvas.getModel().getGlobe().getElevation(latlon.latitude, latlon.longitude);
      final double elevation = zoomElevation + groundElevation;

      final Position position = Position.fromDegrees(latitude, longitude, elevation);

      final View view = _wwCanvas.getView();

      try {

         final float bearingMapPos = mapPosition.bearing;
         final float tiltMapPos = mapPosition.tilt;

         final boolean isResetBearing = (positionFlags & IMapSyncListener.RESET_BEARING) != 0;
         final boolean isResetTilt = (positionFlags & IMapSyncListener.RESET_TILT) != 0;

         if (isResetBearing) {

            view.setHeading(Angle.ZERO);

         } else if (bearingMapPos != 0) {

            final Angle bearing = Angle.fromDegrees(-bearingMapPos);
            view.setHeading(bearing);
         }

         if (isResetTilt) {

            view.setPitch(Angle.ZERO);

         } else if (tiltMapPos != 0) {

            final Angle tilt = Angle.fromDegrees(tiltMapPos);

            view.setPitch(tilt);
         }

      } catch (final Exception e) {
         // ignore - this happened
      }

      view.goTo(position, elevation);
   }

   private void onAWTMouseClick(final MouseEvent mouseEvent) {

      if (mouseEvent == null || mouseEvent.isConsumed()) {
         return;
      }

//		System.out.println(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \tonAWTMouseClick");
//		// TODO remove SYSTEM.OUT.PRINTLN

      final boolean isRightClick = SwingUtilities.isRightMouseButton(mouseEvent);
      if (isRightClick) {

         // open context menu

         // set state here because opening the context menu is async
         _isContextMenuVisible = true;

         _mapContainer.getDisplay().asyncExec(new Runnable() {

            @Override
            public void run() {

               createContextMenu(mouseEvent.getXOnScreen(), mouseEvent.getYOnScreen());
            }
         });
         mouseEvent.consume();
      }
   }

   private void onAWTMouseDragged(final MouseEvent mouseEvent) {

      final MapPosition mapPosition = getMapPosition();

      if (mapPosition == null) {
         return;
      }

      _lastFiredSyncEventTime = System.currentTimeMillis();

      MapManager.fireSyncMapEvent(mapPosition, this, 0);
   }

   @Override
   public void onMapBookmarkActionPerformed(final MapBookmark mapBookmark, final MapBookmarkEventType mapBookmarkEventType) {

      if (mapBookmarkEventType == MapBookmarkEventType.MOVETO) {
         moveToMapLocation(mapBookmark);
      }
   }

   public void onModifyConfig() {

      // altitude mode can have been changed, do a slider repositioning
      updateTrackSlider();

      Map3Manager.getLayer_Marker().onModifyConfig(_allTours);
   }

   private void onSelectionChanged(final ISelection selection) {

      if (_isPartVisible == false || _isRestored == false) {

         if (selection instanceof SelectionTourData
               || selection instanceof SelectionTourId
               || selection instanceof SelectionTourIds) {

            // keep only selected tours
            _lastHiddenSelection = selection;
         }

         return;
      }

      final boolean isPOIVisible = Map3Manager.getLayer_Marker().isEnabled();
      final boolean isTrackSliderVisible = Map3Manager.getLayer_TrackSlider().isEnabled();
      final boolean isTourTrackVisible = Map3Manager.getLayer_TourTrack().isEnabled();

      // check if a tour or poi can be displayed
      if (!isTourTrackVisible && !isPOIVisible && !isTrackSliderVisible) {
         return;
      }

      if (selection instanceof SelectionTourData) {

         final SelectionTourData selectionTourData = (SelectionTourData) selection;
         final TourData tourData = selectionTourData.getTourData();

         showTour(tourData);

      } else if (selection instanceof SelectionTourId) {

         final Long tourId = ((SelectionTourId) selection).getTourId();
         final TourData tourData = TourManager.getInstance().getTourData(tourId);

         showTour(tourData);

      } else if (selection instanceof SelectionTourIds) {

         // paint all selected tours

         final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
         if (tourIds.isEmpty()) {

            clearView();

         } else if (tourIds.size() == 1) {

            // only 1 tour is displayed, synch with this tour !!!

            final TourData tourData = TourManager.getInstance().getTourData(tourIds.get(0));

            showTour(tourData);

         } else {

            // paint multiple tours

            showTours(tourIds);
         }

      } else if (selection instanceof SelectionTourMarker) {

         final SelectionTourMarker markerSelection = (SelectionTourMarker) selection;

         syncMapWith_TourMarker(markerSelection.getTourData(), markerSelection.getSelectedTourMarker());

      } else if (selection instanceof SelectionChartInfo) {

         if (isTrackSliderVisible == false) {
            return;
         }

         final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

         final ChartDataModel chartDataModel = chartInfo.chartDataModel;
         if (chartDataModel != null) {

            final Object tourData = chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);
            if (tourData instanceof TourData) {

               syncMapWith_ChartSlider(
                     (TourData) tourData,
                     chartInfo.leftSliderValuesIndex,
                     chartInfo.rightSliderValuesIndex,
                     chartInfo.selectedSliderValuesIndex);
            }
         }

      } else if (selection instanceof SelectionChartXSliderPosition) {

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

               syncMapWith_ChartSlider(//
                     tourData,
                     leftSliderValueIndex,
                     rightSliderValueIndex,
                     leftSliderValueIndex);

               enableActions();
            }
         }

      } else if (selection instanceof SelectionDeletedTours) {

         clearView();
      }
   }

   private void restoreState() {

      final boolean isTourAvailable = _allTours.size() > 0;

      // sync map with tour
      _isMapSynched_WithTour = Util.getStateBoolean(_state, STATE_IS_SYNC_MAP_VIEW_WITH_TOUR, true);
      _actionSyncMap_WithTour.setChecked(_isMapSynched_WithTour);

      // sync map position with slider
      _isMapSynched_WithChartSlider = Util.getStateBoolean(_state, STATE_IS_SYNC_MAP_POSITION_WITH_SLIDER, false);
      _actionSyncMap_WithChartSlider.setChecked(_isMapSynched_WithChartSlider);

      // synch map with another map
      _isMapSynched_WithOtherMap = Util.getStateBoolean(_state, STATE_IS_SYNC_MAP3_WITH_OTHER_MAP, false);
      _actionSyncMap_WithOtherMap.setChecked(_isMapSynched_WithOtherMap);

      /*
       * Tour
       */
      final boolean isTourVisible = Util.getStateBoolean(_state, STATE_IS_TOUR_VISIBLE, true);
      _actionShowTourInMap.setSelection(isTourVisible);
      _actionShowTourInMap.setEnabled(isTourAvailable);
      Map3Manager.setLayerVisible_TourTrack(isTourVisible);

      /*
       * Marker
       */
      final boolean isMarkerVisible = Util.getStateBoolean(_state, STATE_IS_MARKER_VISIBLE, true);
      Map3Manager.setLayerVisible_Marker(isMarkerVisible);

      /*
       * Legend
       */
      final boolean isLegendVisible = Util.getStateBoolean(_state, STATE_IS_LEGEND_VISIBLE, true);
      _actionShowLegendInMap.setChecked(isLegendVisible);
      Map3Manager.setLayerVisible_Legend(isLegendVisible);

      /*
       * Chart slider
       */
      final boolean isTrackSliderVisible = Util.getStateBoolean(_state, STATE_IS_TRACK_SLIDER_VISIBLE, true);
      _actionShowTrackSlider.setChecked(isTrackSliderVisible);
      Map3Manager.setLayerVisible_TrackSlider(isTrackSliderVisible);

      // tour color
      final String stateColorId = Util.getStateString(_state, STATE_TOUR_COLOR_ID, MapGraphId.Altitude.name());

      try {
         _graphId = MapGraphId.valueOf(stateColorId);
      } catch (final Exception e) {
         // set default
         _graphId = MapGraphId.Altitude;
      }

      // select/check active color
      switch (_graphId) {
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

      default:
         break;
      }

      setColorProvider(_graphId);

      _actionShowDirectionArrows.setChecked(TourTrackConfigManager.getActiveConfig().isShowDirectionArrows);

      // restore 3D view
      final String stateMap3View = Util.getStateString(_state, STATE_MAP3_VIEW, null);
      if (stateMap3View != null) {

         final View view = _wwCanvas.getView();
         view.restoreState(stateMap3View);

         view.firePropertyChange(AVKey.VIEW, null, view);
      }

   }

   @PersistState
   private void saveState() {

      /*
       * It can happen that this view is not yet restored with restoreState() but the saveState()
       * method is called which causes a NPE
       */
      if (_graphId == null) {
         return;
      }

      final boolean isLegendVisible = Map3Manager.getLayer_TourLegend().isEnabled();
      final boolean isMarkerVisible = Map3Manager.getLayer_Marker().isEnabled();
      final boolean isSliderVisible = Map3Manager.getLayer_TrackSlider().isEnabled();
      final boolean isTrackVisible = Map3Manager.getLayer_TourTrack().isEnabled();

      _state.put(STATE_IS_LEGEND_VISIBLE, isLegendVisible);
      _state.put(STATE_IS_MARKER_VISIBLE, isMarkerVisible);
      _state.put(STATE_IS_SYNC_MAP_POSITION_WITH_SLIDER, _isMapSynched_WithChartSlider);
      _state.put(STATE_IS_SYNC_MAP_VIEW_WITH_TOUR, _isMapSynched_WithTour);
      _state.put(STATE_IS_SYNC_MAP3_WITH_OTHER_MAP, _isMapSynched_WithOtherMap);
      _state.put(STATE_IS_TOUR_VISIBLE, isTrackVisible);
      _state.put(STATE_IS_TRACK_SLIDER_VISIBLE, isSliderVisible);

      _state.put(STATE_TOUR_COLOR_ID, _graphId.name());

      final View view = _wwCanvas.getView();

      try {
         _state.put(STATE_MAP3_VIEW, view.getRestorableState());
      } catch (final Exception e) {
         // this can occur
//			StatusUtil.log(e);
      }
   }

   private void setAnnotationColors(final TourData tourData,
                                    final int positionIndex,
                                    final GlobeAnnotation trackPoint) {

      final Color bgColor;
      final Color fgColor;

      final IMapColorProvider colorProvider = MapColorProvider.getActiveMap3ColorProvider(_graphId);

      Integer colorValue = null;

      if (colorProvider instanceof IGradientColorProvider) {

         final IGradientColorProvider gradientColorProvider = (IGradientColorProvider) colorProvider;

         final MapUnits mapUnits = gradientColorProvider.getMapUnits(ColorProviderConfig.MAP3_TOUR);
         final float legendMinValue = mapUnits.legendMinValue;

         float graphValue = legendMinValue;

         switch (_graphId) {

         case Altitude:

            graphValue = getDataSerieValue(tourData.altitudeSerie, positionIndex, legendMinValue);
            break;

         case Gradient:

            graphValue = getDataSerieValue(tourData.gradientSerie, positionIndex, legendMinValue);
            break;

         case Pace:

            graphValue = getDataSerieValue(tourData.getPaceSerie(), positionIndex, legendMinValue);
            break;

         case Pulse:

            graphValue = getDataSerieValue(tourData.pulseSerie, positionIndex, legendMinValue);
            break;

         case Speed:

            graphValue = getDataSerieValue(tourData.getSpeedSerie(), positionIndex, legendMinValue);
            break;

         default:
            break;
         }

         // get color according to the value
         colorValue = gradientColorProvider.getRGBValue(ColorProviderConfig.MAP3_TOUR, graphValue);

      } else if (colorProvider instanceof IDiscreteColorProvider) {

         final IDiscreteColorProvider discreteColorProvider = (IDiscreteColorProvider) colorProvider;

         colorValue = discreteColorProvider.getColorValue(tourData, positionIndex);
      }

      if (colorValue == null) {

         // set default color

         bgColor = null;
         fgColor = null;

      } else {

         final int r = (colorValue & 0xFF) >>> 0;
         final int g = (colorValue & 0xFF00) >>> 8;
         final int b = (colorValue & 0xFF0000) >>> 16;
//			final int o = (colorValue & 0xFF000000) >>> 24;

         bgColor = new Color(r, g, b, 0xff);
         fgColor = ColorUtil.getContrastColorAWT(r, g, b, 0xff);
      }

      final AnnotationAttributes attributes = trackPoint.getAttributes();

      attributes.setBackgroundColor(bgColor);
      attributes.setTextColor(fgColor);
   }

   private void setAnnotationPosition(final TrackPointAnnotation sliderAnnotation,
                                      final TourData tourData,
                                      final int positionIndex) {

      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      final double latitude = latitudeSerie[positionIndex];
      final double longitude = longitudeSerie[positionIndex];

      final float[] altitudeSerie = tourData.altitudeSerie;
      final float trackAltitude = altitudeSerie == null ? 0 : altitudeSerie[positionIndex];

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

      final LatLon latLon = new LatLon(//
            Angle.fromDegrees(latitude),
            Angle.fromDegrees(longitude));

      sliderAnnotation.latLon = latLon;
      sliderAnnotation.trackAltitude = trackAltitude;

      sliderAnnotation.setAltitudeMode(config.altitudeMode);
   }

   private void setColorProvider(final MapGraphId graphId) {

      _actionMap3Colors.setPrefData(graphId);

      final IMapColorProvider colorProvider = MapColorProvider.getActiveMap3ColorProvider(graphId);

      Map3Manager.getLayer_TourTrack().setColorProvider(colorProvider);
      Map3Manager.getLayer_TourLegend().setColorProvider(colorProvider);

      Map3Manager.getLayer_TourTrack().updateColors(_allTours);

      for (final ActionTourColor colorAction : _allColorActions) {
         colorAction.disposeColors();
      }

      showAllTours(false);
   }

   @Override
   public void setFocus() {

   }

   public void setTourInfo(final ITrackPath hoveredTrackPath, final Integer hoveredPositionIndex) {

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "]")
//				+ ("\thoveredPositionIndex: " + hoveredPositionIndex)
//				+ ("\t_currentHoveredTrackPosition: " + _currentHoveredTrackPosition)
//				+ ("\thoveredTrackPath: " + hoveredTrackPath)
//		//
//				);
//		// TODO remove SYSTEM.OUT.PRINTLN

      if (hoveredTrackPath == null) {

         // hide tour info

         if (_isContextMenuVisible) {

            // keep tour info visible when context menu is displayed

            return;
         }

         hideTourInfo();

      } else {

         // a tour is hovered

         if (hoveredPositionIndex == null) {

            // a new position is not hovered, keep tour info opened

         } else {

            // ckeck if a new position is hovered
            if (_currentHoveredTrack != null
                  && _currentHoveredTrack == hoveredTrackPath
                  && _currentHoveredTrackPosition != null
                  && _currentHoveredTrackPosition.intValue() == hoveredPositionIndex.intValue()) {

               return;
            }

            // keep hovered position
            _currentHoveredTrack = hoveredTrackPath;
            _currentHoveredTrackPosition = hoveredPositionIndex;

            final TourData tourData = hoveredTrackPath.getTourTrack().getTourData();

            final TourInfoLayer tourInfoLayer = Map3Manager.getLayer_TourInfo();
            final TrackPointAnnotation trackPoint = tourInfoLayer.getHoveredTrackPoint();

            trackPoint.setText(createSliderText(hoveredPositionIndex, tourData));

            setAnnotationPosition(trackPoint, tourData, hoveredPositionIndex);
            setAnnotationColors(tourData, hoveredPositionIndex, trackPoint);

//				final TrackPointLine trackPointLine = tourInfoLayer.getHoveredTrackPointLine();

            tourInfoLayer.setTrackPointVisible(true);
         }
      }
   }

   private void setTrackSliderVisible(final boolean isVisible) {

      // !!! set state in model before enable actions !!!
      Map3Manager.setLayerVisible_TrackSlider(isVisible);

      enableActions();
   }

   /**
    * Shows all tours in the map which are set in {@link #_allTours}.
    *
    * @param isSyncMapViewWithTour
    *           When <code>true</code> the map will zoomed and positioned to show all tours.
    */
   public void showAllTours(final boolean isSyncMapViewWithTour) {

      enableActions();

      final TourTrackLayer tourTrackLayer = Map3Manager.getLayer_TourTrack();

      if (tourTrackLayer.isEnabled()) {

         // track layer is displayed

         final ArrayList<TourMap3Position> allPositions = tourTrackLayer.createTrackPaths(_allTours);

         final boolean isTourAvailable = _allTours.size() > 0;

         Map3Manager.getLayer_TourLegend().updateLegendImage(isTourAvailable);

         showAllTours_Final(isSyncMapViewWithTour, allPositions);
      }

      updateUI_CreateMarker();

      Map3Manager.redrawMap();
   }

   //	public static final String	ALL						= "gov.nasa.worldwind.perfstat.All";
//
//	public static final String	FRAME_RATE				= "gov.nasa.worldwind.perfstat.FrameRate";
//	public static final String	FRAME_TIME				= "gov.nasa.worldwind.perfstat.FrameTime";
//	public static final String	PICK_TIME				= "gov.nasa.worldwind.perfstat.PickTime";
//
//	public static final String	TERRAIN_TILE_COUNT		= "gov.nasa.worldwind.perfstat.TerrainTileCount";
//	public static final String	IMAGE_TILE_COUNT		= "gov.nasa.worldwind.perfstat.ImageTileCount";
//
//	public static final String	AIRSPACE_GEOMETRY_COUNT	= "gov.nasa.worldwind.perfstat.AirspaceGeometryCount";
//	public static final String	AIRSPACE_VERTEX_COUNT	= "gov.nasa.worldwind.perfstat.AirspaceVertexCount";
//
//	public static final String	JVM_HEAP				= "gov.nasa.worldwind.perfstat.JvmHeap";
//	public static final String	JVM_HEAP_USED			= "gov.nasa.worldwind.perfstat.JvmHeapUsed";
//
//	public static final String	MEMORY_CACHE			= "gov.nasa.worldwind.perfstat.MemoryCache";
//	public static final String	TEXTURE_CACHE			= "gov.nasa.worldwind.perfstat.TextureCache";

//2013-10-06 10:12:12.317'955 [Map3View] 	         0  Frame Rate (fps)
//2013-10-06 10:12:12.317'982 [Map3View] 	       152  Frame Time (ms)
//2013-10-06 10:12:12.318'366 [Map3View] 	         8  Pick Time (ms)

//2013-10-06 10:12:12.318'392 [Map3View] 	        91  Terrain Tiles

//2013-10-06 10:12:12.318'169 [Map3View] 	      6252  Cache Size (Kb): Terrain
//2013-10-06 10:12:12.318'191 [Map3View] 	         0  Cache Size (Kb): Placename Tiles
//2013-10-06 10:12:12.318'214 [Map3View] 	      1860  Cache Size (Kb): Texture Tiles
//2013-10-06 10:12:12.318'289 [Map3View] 	      3870  Cache Size (Kb): Elevation Tiles
//2013-10-06 10:12:12.318'414 [Map3View] 	    422617  Texture Cache size (Kb)

//2013-10-06 10:12:12.318'007 [Map3View] 	         2  Blue Marble (WMS) 2004 Tiles
//2013-10-06 10:12:12.318'044 [Map3View] 	        35  i-cubed Landsat Tiles
//2013-10-06 10:12:12.318'069 [Map3View] 	        84  MS Virtual Earth Aerial Tiles
//2013-10-06 10:12:12.318'093 [Map3View] 	        84  Bing Imagery Tiles

//2013-10-06 10:12:12.318'118 [Map3View] 	    463659  JVM total memory (Kb)
//2013-10-06 10:12:12.318'141 [Map3View] 	    431273  JVM used memory (Kb)

   private void showAllTours_Final(final boolean isSyncMapViewWithTour,
                                   final ArrayList<TourMap3Position> allPositions) {

      if (isSyncMapViewWithTour) {

         final Map3ViewController viewController = Map3ViewController.create(_wwCanvas);

         viewController.goToDefaultView(allPositions);
      }

      updateTrackSlider();

      Map3Manager.redrawMap();

      enableActions();
   }

   private void showAllTours_InternalTours() {

      showAllTours(_isMapSynched_WithTour);
   }

   private void showAllTours_NewTours(final ArrayList<TourData> newTours) {

      // check if new tours are already displayed
      if (newTours.hashCode() == _allTours.hashCode()) {
         return;
      }

      cleanupOldTours();

      _allTours.addAll(getMapTours(newTours));

      showAllTours_InternalTours();
   }

   private void showTour(final TourData newTourData) {

      if (newTourData == null) {
         return;
      }

      // check if this tour is already displayed
      for (final TourData existingTour : _allTours) {

         if (newTourData.equals(existingTour)) {

            /*
             * the new tour is already displayed in the map, just select it but only when multiple
             * tours are displayed, otherwise one tour gets selected which is a very annoying
             */

            if (_allTours.size() > 1) {

               final TourTrackLayer tourTrackLayer = Map3Manager.getLayer_TourTrack();
               final ArrayList<TourMap3Position> trackPositions = tourTrackLayer.selectTrackPath(newTourData);

               if (trackPositions == null) {
                  // track is already selected
                  return;
               }

               showAllTours_Final(_isMapSynched_WithTour, trackPositions);
            }

            return;
         }
      }

      final ArrayList<TourData> allTours = new ArrayList<>();
      allTours.add(newTourData);

      showAllTours_NewTours(allTours);
   }

   private void showTours(final ArrayList<Long> allTourIds) {

      if (allTourIds.hashCode() != _allTourIdHash || _allTours.hashCode() != _allTourDataHash) {

         // tour data needs to be loaded

         final ArrayList<TourData> allTourData = new ArrayList<>();

         TourManager.loadTourData(allTourIds, allTourData, true);

         _allTourIdHash = allTourIds.hashCode();
         _allTourDataHash = allTourData.hashCode();

         showAllTours_NewTours(allTourData);

      } else {

         showAllTours_NewTours(_allTours);
      }
   }

   private void showToursFromTourProvider() {

      Display.getCurrent().asyncExec(new Runnable() {
         @Override
         public void run() {

            // validate widget
            if (_mapContainer.isDisposed()) {
               return;
            }

            // check if tour is set from a selection provider
            if (_allTours.size() > 0) {
               return;
            }

            final ArrayList<TourData> allTours = TourManager.getSelectedTours();
            if (allTours != null) {
               showAllTours_NewTours(allTours);
            }
         }
      });
   }

   private void syncMapWith_ChartSlider(final TourData tourData,
                                        final int leftSliderValuesIndex,
                                        final int rightSliderValuesIndex,
                                        final int selectedSliderIndex) {

      final TrackSliderLayer chartSliderLayer = getLayerTrackSlider();
      if (chartSliderLayer == null) {
         return;
      }

      if (tourData == null || tourData.latitudeSerie == null) {

         chartSliderLayer.setSliderVisible(false);

      } else {

         // sync map with chart slider

         syncMapWith_SliderPosition(tourData, chartSliderLayer, selectedSliderIndex);

         // update slider UI
         updateTrackSlider_10_Position(//
               tourData,
               leftSliderValuesIndex,
               rightSliderValuesIndex);

         enableActions();
      }
   }

   private void syncMapWith_SliderPosition(final TourData tourData,
                                           final TrackSliderLayer chartSliderLayer,
                                           int valuesIndex) {

      final double[] latitudeSerie = tourData.latitudeSerie;

      // check bounds
      if (valuesIndex >= latitudeSerie.length) {
         valuesIndex = latitudeSerie.length;
      }

      final double latitude = latitudeSerie[valuesIndex];
      final double longitude = tourData.longitudeSerie[valuesIndex];

      final float[] altitudeSerie = tourData.altitudeSerie;

      final View view = _wwCanvas.getView();
      if (view instanceof BasicOrbitView) {

         final BasicOrbitView orbitView = (BasicOrbitView) view;
         final Position eyePos = orbitView.getCurrentEyePosition();

         final float trackAltitude = altitudeSerie == null ? 0 : altitudeSerie[valuesIndex];
         final double elevation = getSliderYPosition(trackAltitude, eyePos);
         final LatLon sliderDegrees = LatLon.fromDegrees(latitude, longitude);

         /*
          * Prevent setting the same location because this will jitter the map slider and is
          * unnecessary.
          */
         if (_currentTrackInfoSliderPosition != null) {

            /*
             * Set a new position and sync map only, when lat/lon are different and elevation is
             * larger than 1mm.
             */

            final double eleDiff = _currentTrackInfoSliderPosition.elevation - elevation;

            if (_currentTrackInfoSliderPosition.getLatitude().equals(sliderDegrees.latitude)
                  && _currentTrackInfoSliderPosition.getLongitude().equals(sliderDegrees.longitude)
                  && eleDiff < 0.001) {

               return;
            }
         }

         chartSliderLayer.setSliderVisible(false);

         final Position mapSliderPosition = new Position(sliderDegrees, elevation);

         /*
          * This fragment is copied from
          * gov.nasa.worldwindx.applications.sar.AnalysisPanel.updateView(boolean)
          */

         // Send a message to stop all changes to the view's center position.
//				orbitView.stopMovementOnCenter();

         // Set the view to center on the track position, while keeping the eye altitude constant.
         try {

            // New eye lat/lon will follow the ground position.
            final LatLon newEyeLatLon = eyePos.add(mapSliderPosition.subtract(orbitView.getCenterPosition()));

            // Eye elevation will not change unless it is below the ground position elevation.
            final double newEyeElev = eyePos.getElevation() < mapSliderPosition.getElevation() //
                  ? mapSliderPosition.getElevation()
                  : eyePos.getElevation();

            final Position newEyePos = new Position(newEyeLatLon, newEyeElev);

            if (_isMapSynched_WithChartSlider) {
               orbitView.setOrientation(newEyePos, mapSliderPosition);
            }

            // keep current position
            _currentTrackInfoSliderPosition = mapSliderPosition;
         }

         // Fallback to setting center position.
         catch (final Exception e) {

            if (_isMapSynched_WithChartSlider) {

               orbitView.setCenterPosition(mapSliderPosition);
               // View/OrbitView will have logged the exception, no need to log it here.
            }
         }
      }
   }

   /**
    * A tourmarker is selected, sync it with the map.
    */
   private void syncMapWith_TourMarker(final TourData tourData, final ArrayList<TourMarker> allTourMarker) {

      final TrackSliderLayer chartSliderLayer = getLayerTrackSlider();
      if (chartSliderLayer == null) {
         return;
      }

      if (tourData == null || tourData.latitudeSerie == null) {
         chartSliderLayer.setSliderVisible(false);
         return;
      }

      // ensure tour is displayed
      updateUI_ShowTour(tourData);

      final int valuesIndex = allTourMarker.get(0).getSerieIndex();

      syncMapWith_SliderPosition(tourData, chartSliderLayer, valuesIndex);

      updateTrackSlider_10_Position(tourData, allTourMarker);
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

      final long timeDiff = System.currentTimeMillis() - _lastFiredSyncEventTime;

      if (timeDiff < 1000) {
         // ignore because it causes LOTS of problems when synchronizing moved map
         return;
      }

      moveToMapLocation(mapPosition, positionFlags);
   }

   private void updateModifiedTours(final ArrayList<TourData> modifiedTours) {

      // cleanup old tours, this method cannot be used: cleanupOldTours();
//		_postSelectionProvider.clearSelection();
      _currentTrackInfoSliderPosition = null;

      _allTours.removeAll(modifiedTours);
      _allTours.addAll(getMapTours(modifiedTours));

      showAllTours_InternalTours();
   }

   private void updateTrackSlider() {

      final TrackSliderLayer trackSliderLayer = getLayerTrackSlider();
      if (trackSliderLayer == null) {
         return;
      }

      final TourData tourData = getSelectedTour(trackSliderLayer);
      if (tourData == null) {
// ???		trackSliderLayer.setSliderVisible(false);
         return;
      }

      updateTrackSlider_10_Position(tourData, _currentLeftSliderValueIndex, _currentRightSliderValueIndex);
   }

   private void updateTrackSlider_10_Position(final TourData tourData, final ArrayList<TourMarker> allTourMarker) {

      final TrackSliderLayer trackSliderLayer = getLayerTrackSlider();
      if (trackSliderLayer == null) {
         return;
      }

      final int numberOfTourMarkers = allTourMarker.size();

      int leftSliderValueIndex = 0;
      int rightSliderValueIndex = 0;

      if (numberOfTourMarkers == 1) {

         leftSliderValueIndex = allTourMarker.get(0).getSerieIndex();
         rightSliderValueIndex = _currentRightSliderValueIndex;

      } else if (numberOfTourMarkers > 1) {

         leftSliderValueIndex = allTourMarker.get(0).getSerieIndex();
         rightSliderValueIndex = allTourMarker.get(numberOfTourMarkers - 1).getSerieIndex();
      }

      updateTrackSlider_10_Position(tourData, leftSliderValueIndex, rightSliderValueIndex);
   }

   private void updateTrackSlider_10_Position(final TourData tourData, int leftPosIndex, int rightPosIndex) {

      final TrackSliderLayer trackSliderLayer = getLayerTrackSlider();
      if (trackSliderLayer == null) {
         return;
      }

      final double[] latitudeSerie = tourData.latitudeSerie;
      final int lastIndex = latitudeSerie.length - 1;

      // check array bounds
      if (leftPosIndex < 0 || leftPosIndex > lastIndex) {
         leftPosIndex = 0;
      }
      if (rightPosIndex < 0 || rightPosIndex > lastIndex) {
         rightPosIndex = lastIndex;
      }

      _currentLeftSliderValueIndex = leftPosIndex;
      _currentRightSliderValueIndex = rightPosIndex;

      updateTrackSlider_20_Data(//
            trackSliderLayer.getLeftSlider(),
            _currentLeftSliderValueIndex,
            tourData);

      updateTrackSlider_20_Data(//
            trackSliderLayer.getRightSlider(),
            _currentRightSliderValueIndex,
            tourData);

      trackSliderLayer.setSliderVisible(true);

      Map3Manager.redrawMap();
   }

   private void updateTrackSlider_20_Data(final TrackPointAnnotation slider,
                                          final int positionIndex,
                                          final TourData tourData) {

      /*
       * set position and text
       */
      setAnnotationPosition(slider, tourData, positionIndex);
      setAnnotationColors(tourData, positionIndex, slider);

      slider.setText(createSliderText(positionIndex, tourData));
   }

   /**
    *
    */
   private void updateUI_CreateMarker() {

      final MarkerLayer markerLayer = Map3Manager.getLayer_Marker();
      if (markerLayer.isEnabled()) {

         // poi layer is visible

         markerLayer.createMarker(_allTours);
      }
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

      for (final TourData mapTourData : _allTours) {
         if (mapTourData.getTourId().longValue() == markerTourId) {
            isTourVisible = true;
            break;
         }
      }

      if (isTourVisible == false) {

         // show tour

         cleanupOldTours();

         _allTours.add(tourData);

         showAllTours_InternalTours();
      }
   }

}
