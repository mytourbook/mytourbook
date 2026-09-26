/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.MouseWheelMode;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.commands.ISaveAndRestorePart;
import net.tourbook.common.UI;
import net.tourbook.common.dialog.MessageDialog_WithRadioOptions;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourReference;
import net.tourbook.map2.view.SelectionMapSelection;
import net.tourbook.map25.Map25FPSManager;
import net.tourbook.photo.IPhotoEventListener;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoEventId;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.PhotoSelection;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.photo.TourPhotoLink;
import net.tourbook.tour.photo.TourPhotoLinkSelection;
import net.tourbook.ui.ITourChartViewer;
import net.tourbook.ui.views.geoCompare.GeoCompareEventId;
import net.tourbook.ui.views.geoCompare.GeoCompareManager;
import net.tourbook.ui.views.geoCompare.GeoComparedTour;
import net.tourbook.ui.views.geoCompare.IGeoCompareListener;
import net.tourbook.ui.views.referenceTour.ComparedTourChartView;
import net.tourbook.ui.views.referenceTour.ReferenceTourManager;
import net.tourbook.ui.views.referenceTour.SelectionReferenceTourView;
import net.tourbook.ui.views.referenceTour.TVIElevationCompareResult_ComparedTour;
import net.tourbook.ui.views.referenceTour.TVIRefTour_ComparedTour;
import net.tourbook.ui.views.referenceTour.TVIRefTour_RefTourItem;
import net.tourbook.ui.views.referenceTour.TourCompareConfig;
import net.tourbook.ui.views.tourSegmenter.TourSegmenterView;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.UndoContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

// author: Wolfgang Schramm
// create: 09.07.2007

/**
 * Shows the selected tour in a chart
 */
public class TourChartView extends ViewPart implements

      ISaveablePart,
      ISaveAndRestorePart,
      ITourChartViewer,
      IPhotoEventListener,
      IGeoCompareListener

{

   public static final String      ID                                  = "net.tourbook.views.TourChartView"; //$NON-NLS-1$

   static final String             TOOLBAR_GROUP_SAVE_AND_UNDO_ACTIONS = "group_SaveAndUndoActions";         //$NON-NLS-1$

   private final IDialogSettings   _state                              = TourbookPlugin.getState(ID);
   private final IPreferenceStore  _prefStore                          = TourbookPlugin.getPrefStore();

   private TourChartConfiguration  _tourChartConfig;
   private TourData                _tourData;
   private TourPhotoLink           _tourPhotoLink;

   /**
    * Chart update is forced, when previous selection was a photo link or current selection is a
    * photo link.
    */
   private boolean                 _isForceUpdate;

   private IPartListener2          _partListener;
   private PostSelectionProvider   _postSelectionProvider;
   private ISelectionListener      _postSelectionListener;
   private IPropertyChangeListener _prefChangeListener;
   private ITourEventListener      _tourEventListener;

   private boolean                 _isInSaving;
   private boolean                 _isInSelectionChanged;
   private boolean                 _isInSliderPositionFired;
   private boolean                 _isInTourModified;

   private FormToolkit             _tk;

   private IUndoContext            _undoContext;
   private UndoActionHandler       _undoActionHandler;
   private RedoActionHandler       _redoActionHandler;
   private int                     _undoCounter;

   /*
    * UI controls
    */
   private PageBook  _pageBook;
   private Composite _pageNoData;

   private TourChart _tourChart;

   private void addPartListener() {

      _partListener = new IPartListener2() {

         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == TourChartView.this) {

               Map25FPSManager.setBackgroundFPSToAnimationFPS(true);
            }
         }

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == TourChartView.this) {
               TourManager.setTourChartEditor(null);
            }
         }

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == TourChartView.this) {

               Map25FPSManager.setBackgroundFPSToAnimationFPS(false);
            }

            // ensure that at EACH part deactivation the photo tooltip gets hidden
            _tourChart.partIsDeactivated();
         }

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == TourChartView.this) {
               _tourChart.partIsHidden();
            }
         }

         @Override
         public void partInputChanged(final IWorkbenchPartReference partRef) {}

         @Override
         public void partOpened(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == TourChartView.this) {
               TourManager.setTourChartEditor(TourChartView.this);
            }
         }

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {
            if (partRef.getPart(false) == TourChartView.this) {
               _tourChart.partIsVisible();
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

            /*
             * create a new chart configuration when the preferences has changed
             */
            if (property.equals(ITourbookPreferences.GRAPH_VISIBLE)
                  || property.equals(ITourbookPreferences.GRAPH_X_AXIS)
                  || property.equals(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME)
            //
            //
            ) {
               _tourChartConfig = TourManager.createDefaultTourChartConfig(_state);

               if (_tourChart != null) {
                  _tourChart.updateTourChart(_tourData, _tourChartConfig, false);
               }

            } else if (
            //

            /*
             * HR zone colors can be modified and person hash code has changed by saving the person
             * entity -> tour chart must be recreated
             */
            property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)

                  // multiple tours can have the wrong person for hr zones
                  || property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

               clearView();
               showTour();

            } else if (property.equals(ITourbookPreferences.GRAPH_MOUSE_MODE)) {

               final Object newValue = event.getNewValue();
               final Enum<MouseWheelMode> enumValue = Util.getEnumValue((String) newValue, MouseWheelMode.Zoom);

               _tourChart.setMouseWheelMode((MouseWheelMode) enumValue);
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   /**
    * Listen for events when a tour is selected
    */
   private void addSelectionListener() {

      _postSelectionListener = (part, selection) -> {

         if (part == TourChartView.this) {
            return;
         }

         onSelectionChanged(selection);
      };

      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {

         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == TourChartView.this) {
               return;
            }

            if (eventId == TourEventId.SEGMENT_LAYER_CHANGED) {

               TourSegmenterView tourSegmenterView = null;
               if (part instanceof TourSegmenterView) {
                  tourSegmenterView = (TourSegmenterView) part;
               }

               if (tourSegmenterView != null && eventData instanceof TourData) {

                  final TourData eventTourData = (TourData) eventData;

                  if (eventTourData.equals(_tourData)) {

                     _tourChart.updateTourSegmenter();

                  } else {

                     /*
                      * This case happened that this event contains not the same tourdata as the
                      * tourchart, it occurred for multiple tours in tourdata.
                      */

                     onSelectionChanged(new SelectionTourData(eventTourData));

//                     StatusUtil.log(new Exception("Event contained wrong tourdata."));
                  }
               }

            } else if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

               _tourChart.updateTourChart(true, true);

            } else if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

               if (_tourData == null || _isInSaving) {
                  return;
               }

               // get modified tours
               final List<TourData> allModifiedTours = ((TourEvent) eventData).getModifiedTours();
               if (allModifiedTours != null) {

                  discardModifications(allModifiedTours);
               }

            } else if (eventId == TourEventId.TOUR_CHANGED) {

               if (_tourData == null) {
                  return;
               }

               if (_tourData.isMultipleTours()) {

                  clearView();
               }

            } else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED
                  || eventId == TourEventId.EQUIPMENT_STRUCTURE_CHANGED) {

               /*
                * A tag was saved, when this view was hidden and then unhidden, it can cause an
                * error in the tour editor to be out of sync
                */

               clearView();
               showTour();

            } else if ((eventId == TourEventId.TOUR_SELECTION
                  || eventId == TourEventId.SLIDER_POSITION_CHANGED)

                  && eventData instanceof ISelection) {

               if (part instanceof ComparedTourChartView) {

                  // ignore -> this would modify the geo compare tour

                  return;
               }

               onSelectionChanged((ISelection) eventData);

            } else if (eventId == TourEventId.MARKER_SELECTION && eventData instanceof SelectionTourMarker) {

               onSelection_TourMarker((SelectionTourMarker) eventData);

            } else if (eventId == TourEventId.HOVERED_VALUE_POSITION && eventData instanceof HoveredValueData) {

               onSelection_HoveredValue((HoveredValueData) eventData);

            } else if (eventId == TourEventId.MAP_SELECTION && eventData instanceof SelectionMapSelection) {

               onSelection_MapSelection((SelectionMapSelection) eventData);

            } else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

               clearView();

            } else if (eventId == TourEventId.UPDATE_UI) {

               // check if this tour chart contains a tour which must be updated

               if (_tourData == null) {
                  return;
               }

               final Long tourId = _tourData.getTourId();

               // update editor
               if (net.tourbook.ui.UI.containsTourId(eventData, tourId) != null) {

                  // reload tour data and update chart
                  updateChart(TourManager.getInstance().getTourData(tourId));
               }
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void chartListener_HoveredValue(final int hoveredValuePointIndex) {

      fireHoveredValue(hoveredValuePointIndex);
   }

   /**
    * Fire a slider move selection when a slider was moved in the tour chart
    */
   private void chartListener_SliderMoved(final SelectionChartInfo selectionChartInfo) {

      // don't refire when in an event
      if (_isInSelectionChanged || _isInTourModified) {
         return;
      }

      fireSliderPosition();
   }

   private void chartListener_TourIsModified(final TourData tourData) {

      _isInSaving = true;
      {
         final TourData savedTourData = TourManager.saveModifiedTour(tourData);

         updateChart(savedTourData);
      }
      _isInSaving = false;
   }

   private void clearView() {

      _tourData = null;
      _tourChart.updateChart(null, false);

      _pageBook.showPage(_pageNoData);

      // removed old tour data from the selection provider
      _postSelectionProvider.clearSelection();
   }

   @Override
   public void createPartControl(final Composite parent) {

      createUI(parent);

      restoreState();

      addSelectionListener();
      addPrefListener();
      addTourEventListener();
      addPartListener();
      PhotoManager.addPhotoEventListener(this);
      GeoCompareManager.addGeoCompareEventListener(this);

      // set this view part as selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      /*
       * Moving the save action to the right side do NOT work, this will partly scamble the actions
       * and some are hidden. It took me hours to finally find this problem
       */
      tbm.add(new Separator(TOOLBAR_GROUP_SAVE_AND_UNDO_ACTIONS));

// this is just as in info which is a remaining from the debugging
//    tbm.add(new Separator(TOOLBAR_GROUP_1_GRAPHS));
//    tbm.add(new Separator(TOOLBAR_GROUP_2));
//    tbm.add(new Separator(TOOLBAR_GROUP_3));

      setupUndoContext();

      showTour();
   }

   private void createUI(final Composite parent) {

      initUI(parent);

      _pageBook = new PageBook(parent, SWT.NONE);

      _pageNoData = UI.createPage(_tk, _pageBook, Messages.UI_Label_TourIsNotSelected);

      _tourChart = new TourChart(_pageBook, SWT.FLAT, this, _state);
      _tourChart.setCanShowTourSegments(true);
      _tourChart.setShowZoomActions(true);
      _tourChart.setShowSlider(true);
      _tourChart.setTourInfoActionsEnabled(true);
      _tourChart.setToolBarManager(getViewSite().getActionBars().getToolBarManager(), true);
      _tourChart.setContextProvider(new TourChartContextProvider(this), true);

      _tourChartConfig = TourManager.createDefaultTourChartConfig(_state);

      _tourChartConfig.canUseGeoCompareTool = true;

      _tourChart.addHoveredValueListener(hoveredValuePointIndex -> chartListener_HoveredValue(hoveredValuePointIndex));
      _tourChart.addSliderMoveListener(chartInfo -> chartListener_SliderMoved(chartInfo));
      _tourChart.addTourModifyListener(tourData -> chartListener_TourIsModified(tourData));
   }

   private void discardModifications(final List<TourData> allModifiedTours) {

      try {

         _isInTourModified = true;

         final long chartTourId = _tourData.getTourId();

         // update chart with the modified tour
         for (final TourData modifiedTourData : allModifiedTours) {

            if (modifiedTourData == null) {

               /*
                * tour is not set, this can be the case when a manual tour is discarded
                */

               clearView();

               return;
            }

            if (modifiedTourData.getTourId() == chartTourId) {

               updateChart(modifiedTourData);

               // removed old tour data from the selection provider
               _postSelectionProvider.clearSelection();

               return;
            }
         }

         // ensure that wrong data are not displayed
         clearView();

      } finally {

         _isInTourModified = false;
      }
   }

   @Override
   public void dispose() {

      saveState();

      if (_tk != null) {
         _tk.dispose();
      }

      getSite().getPage().removePostSelectionListener(_postSelectionListener);
      getViewSite().getPage().removePartListener(_partListener);

      TourManager.getInstance().removeTourEventListener(_tourEventListener);
      PhotoManager.removePhotoEventListener(this);
      GeoCompareManager.removeGeoCompareListener(this);

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      final IActionBars actionBars = getViewSite().getActionBars();
      if (actionBars != null) {
         actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), null);
         actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), null);
      }

      if (_undoActionHandler != null) {
         _undoActionHandler.dispose();
      }
      if (_redoActionHandler != null) {
         _redoActionHandler.dispose();
      }

      disposeUndo();

      super.dispose();
   }

   private void disposeUndo() {

      final IOperationHistory undoHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();

      undoHistory.dispose(_undoContext, true, true, true);

      _undoCounter = 0;
   }

   @Override
   public void doRestore() {

      disposeUndo();

      // removed old tour data from the selection provider
      _postSelectionProvider.clearSelection();

      final Long tourId = _tourData.getTourId();
      final TourData tourData = TourManager.getInstance().getTourDataFromDb(tourId);

      updateChart(tourData, true, false);
   }

   @Override
   public void doSave() {

      doSave(null);
   }

   @Override
   public void doSave(final IProgressMonitor monitor) {

      _tourChart.setTourDirty(false);

      TourManager.saveModifiedTour(_tourData);
   }

   @Override
   public void doSaveAs() {}

   private void fireHoveredValue(final int hoveredValuePointIndex) {

      if (_tourData == null) {
         return;
      }

      Long tourId = null;
      int hoveredTourSerieIndex = 0;

      if (_tourData.isMultipleTours()) {

         // get tour id and tour serie index

         final Long[] multipleTourIds = _tourData.multipleTourIds;
         final int[] multipleTourStartIndex = _tourData.multipleTourStartIndex;

         // set values for the first tour
         tourId = multipleTourIds[0];
         hoveredTourSerieIndex = hoveredValuePointIndex;

         int tourStartIndex = multipleTourStartIndex[0]; // this first value is always 0

         for (int tourIndex = 1; tourIndex < multipleTourStartIndex.length; tourIndex++) {

            if (hoveredValuePointIndex > tourStartIndex) {
               break;
            }

            tourStartIndex = multipleTourStartIndex[tourIndex];

            tourId = multipleTourIds[tourIndex];
            hoveredTourSerieIndex = hoveredValuePointIndex - tourStartIndex;
         }

      } else {

         tourId = _tourData.getTourId();
         hoveredTourSerieIndex = hoveredValuePointIndex;
      }

      final HoveredValueData hoveredValueData = new HoveredValueData(tourId, hoveredTourSerieIndex);

      TourManager.fireEventWithCustomData(
            TourEventId.HOVERED_VALUE_POSITION,
            hoveredValueData,
            TourChartView.this);
   }

   void firePropertyChange() {

      _tourChart.getDisplay().asyncExec(() -> {

         firePropertyChange(ISaveablePart.PROP_DIRTY);
      });
   }

   /**
    * Fire slider move event when the chart is drawn the first time or when the focus gets the
    * chart, this will move the sliders in the map to the correct position.
    */
   private void fireSliderPosition() {

      // don't fire an slider event when in selection change event
      if (_isInSelectionChanged) {
         return;
      }

      final SelectionChartInfo chartInfo = _tourChart.getChartInfo();
      if (chartInfo != null) {

         _isInSliderPositionFired = true;
         {
            if (_isInSaving) {

               final TourMarker hoveredMarker = _tourChart.getLastHoveredTourMarker();

               if (hoveredMarker != null) {

                  chartInfo.selectedSliderValuesIndex = hoveredMarker.getSerieIndex();
               }
            }

            TourManager.fireEventWithCustomData(
                  TourEventId.SLIDER_POSITION_CHANGED,
                  chartInfo,
                  TourChartView.this);
         }
         _isInSliderPositionFired = false;
      }
   }

   @Override
   public void geoCompareEvent(final IWorkbenchPart part, final GeoCompareEventId eventId, final Object eventData) {

      if (part == TourChartView.this) {
         return;
      }

      switch (eventId) {

      case TOUR_IS_GEO_COMPARED:

         break;

      case SET_COMPARING_ON:

         _tourChart.onGeoCompareOnOff(true);
         break;

      case SET_COMPARING_OFF:

         _tourChart.onGeoCompareOnOff(false);
         break;

      default:
         break;
      }
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      if (_tourData == null) {
         return null;
      }

      final ArrayList<TourData> tourList = new ArrayList<>();
      tourList.add(_tourData);

      return tourList;
   }

   @Override
   public TourChart getTourChart() {
      return _tourChart;
   }

   public IUndoContext getUndoContext() {

      return _undoContext;
   }

   private void initUI(final Composite parent) {

      _tk = new FormToolkit(parent.getDisplay());
   }

   @Override
   public boolean isDirty() {

      if (_tourData == null) {
         return false;
      }

      return _tourChart.isTourDirty();
   }

   @Override
   public boolean isSaveAsAllowed() {

      return false;
   }

   @Override
   public boolean isSaveOnCloseNeeded() {

      return isDirty();
   }

   private void onSelection_HoveredValue(final HoveredValueData eventData) {

      if (_tourData == null) {
         return;
      }

      final Long eventTourId = eventData.tourId;

      if (eventTourId == null) {
         return;
      }

      int hoveredTourSerieIndex = -1;

      if (_tourData.isMultipleTours()) {

         // adjust hovered value index

         hoveredTourSerieIndex = eventData.hoveredTourSerieIndex;

         final Long[] multipleTourIds = _tourData.multipleTourIds;
         final int[] multipleTourStartIndex = _tourData.multipleTourStartIndex;

         int valueIndexOffset = 0;

         for (int tourIndex = 0; tourIndex < multipleTourIds.length; tourIndex++) {
            final Long tourId = multipleTourIds[tourIndex];
            if (tourId.equals(eventTourId)) {
               valueIndexOffset = multipleTourStartIndex[tourIndex];
               break;
            }
         }

         hoveredTourSerieIndex += valueIndexOffset;

      } else if (_tourData.getTourId().equals(eventTourId)) {

         // the current tour is hovered

         hoveredTourSerieIndex = eventData.hoveredTourSerieIndex;

      }

      if (hoveredTourSerieIndex != -1) {

         _isInSelectionChanged = true;
         {
            _tourChart.setHovered_ValuePoint_Index(hoveredTourSerieIndex);
         }
         _isInSelectionChanged = false;
      }
   }

   private void onSelection_MapSelection(final SelectionMapSelection mapSelection) {

      final SelectionChartXSliderPosition xSliderPosition = new SelectionChartXSliderPosition(
            _tourChart,
            mapSelection.getValueIndex1(),
            mapSelection.getValueIndex2());

      xSliderPosition.setCenterSliderPosition(true);

      _isInSelectionChanged = true;
      {
         _tourChart.selectXSliders(xSliderPosition);
      }
      _isInSelectionChanged = false;
   }

   private void onSelection_TourMarker(final SelectionTourMarker markerSelection) {

      _isInSelectionChanged = true;
      {
         final TourData tourData = markerSelection.getTourData();
         final Long markerTourId = tourData.getTourId();

         /*
          * Check if the marker tour is displayed
          */
         if (_tourData == null || _tourData.getTourId().equals(markerTourId) == false) {

            // show tour

            updateChart(tourData);
         }

         /*
          * set slider position
          */
         final ArrayList<TourMarker> tourMarker = markerSelection.getSelectedTourMarker();
         final int numTourMarkers = tourMarker.size();
         if (numTourMarkers > 0) {

            final TourMarker firstTourMarker = tourMarker.get(0);

            int leftSliderValueIndex;
            if (tourData.isMultipleTours()) {
               leftSliderValueIndex = firstTourMarker.getMultiTourSerieIndex();
            } else {
               leftSliderValueIndex = firstTourMarker.getSerieIndex();
            }

            int rightSliderValueIndex = 0;

            if (numTourMarkers == 1) {

               rightSliderValueIndex = leftSliderValueIndex;

            } else if (numTourMarkers > 1) {

               final TourMarker lastTourMarker = tourMarker.get(numTourMarkers - 1);

               if (tourData.isMultipleTours()) {
                  rightSliderValueIndex = lastTourMarker.getMultiTourSerieIndex();
               } else {
                  rightSliderValueIndex = lastTourMarker.getSerieIndex();
               }
            }

            setSliderPositions(leftSliderValueIndex, rightSliderValueIndex, true);
         }
      }
      _isInSelectionChanged = false;
   }

   private void onSelectionChanged(final ISelection selection) {

      // prevent to listen to own events
      if (_isInSliderPositionFired) {
         return;
      }

      if (_isInSaving) {
         return;
      }

      _isInSelectionChanged = true;
      {
         _isForceUpdate = _tourPhotoLink != null;

         _tourPhotoLink = null;

         if (selection instanceof SelectionTourData) {

            final SelectionTourData tourDataSelection = (SelectionTourData) selection;

            final TourData selectionTourData = tourDataSelection.getTourData();
            if (selectionTourData != null) {

               // prevent loading the same tour
               if (_tourData != null && _tourData.equals(selectionTourData)) {

                  // do nothing

               } else {

                  updateChart(selectionTourData);

                  if (tourDataSelection.isSliderValueIndexAvailable()) {

                     // set slider positions

                     _tourChart.setXSliderPosition(new SelectionChartXSliderPosition(_tourChart,
                           tourDataSelection.getLeftSliderValueIndex(),
                           tourDataSelection.getRightSliderValueIndex()));
                  }
               }
            }

         } else if (selection instanceof SelectionTourId) {

            final SelectionTourId selectionTourId = (SelectionTourId) selection;
            final Long tourId = selectionTourId.getTourId();

            updateChart(tourId);

         } else if (selection instanceof SelectionTourIds) {

            // only 1 tour can be displayed in the tour chart

            final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();

            boolean isChartPainted = false;

            // TourPhotoLinkSelection extends SelectionTourIds
            if (selection instanceof TourPhotoLinkSelection) {

               final ArrayList<TourPhotoLink> tourPhotoLinks = ((TourPhotoLinkSelection) selection).tourPhotoLinks;

               if (tourPhotoLinks.size() > 0) {

                  _tourPhotoLink = tourPhotoLinks.get(0);

                  if (_tourPhotoLink.isHistoryTour()) {

                     // paint history tour

                     updateChart(_tourPhotoLink.getHistoryTourData());

                     isChartPainted = true;
                  }
               }
            }

            if (isChartPainted == false && tourIds != null && tourIds.size() > 0) {

               // paint regular tour

               // force update when photo link selection occurred
               _isForceUpdate = _tourPhotoLink != null;

               if (tourIds.size() > 1) {

                  // show multiple tours
                  updateChart(tourIds);

               } else {
                  updateChart(tourIds.get(0));
               }
            }

         } else if (selection instanceof SelectionChartInfo) {

            final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;
            final ChartDataModel chartDataModel = chartInfo.chartDataModel;

            if (chartDataModel != null) {

               final Object chartTourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
               if (chartTourId instanceof Long) {

                  updateChart(
                        (Long) chartTourId,
                        chartInfo.leftSliderValuesIndex,
                        chartInfo.rightSliderValuesIndex);
               }
            }

         } else if (selection instanceof SelectionChartXSliderPosition) {

            final SelectionChartXSliderPosition xSliderPosition = (SelectionChartXSliderPosition) selection;

            final Chart chart = xSliderPosition.getChart();
            if (chart != null && chart != _tourChart) {

               // it's not the same chart, check if it's the same tour

               final Object tourId = chart.getChartDataModel().getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
               if (tourId instanceof Long) {

                  final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
                  if (tourData != null) {

                     if (_tourData != null && _tourData.equals(tourData)) {

                        // it's the same tour, overwrite chart

                        xSliderPosition.setChart(_tourChart);
                     }
                  }
               }
            }

            _tourChart.selectXSliders(xSliderPosition);

         } else if (selection instanceof SelectionReferenceTourView) {

            final SelectionReferenceTourView tourCatalogSelection = (SelectionReferenceTourView) selection;

            final TVIRefTour_RefTourItem refTourItem = tourCatalogSelection.getRefItem();
            if (refTourItem != null) {

               final long refId = refTourItem.refId;

               final TourCompareConfig compareConfig = ReferenceTourManager.getTourCompareConfig(refId);
               if (compareConfig == null) {
                  return;
               }

               final TourReference refTour = compareConfig.getRefTour();
               if (refTour == null) {
                  return;
               }

               updateChart(
                     refTourItem.getTourId(),
                     refTour.getStartIndex(),
                     refTour.getEndIndex());
            }

         } else if (selection instanceof StructuredSelection) {

            final Object firstElement = ((StructuredSelection) selection).getFirstElement();

            if (firstElement instanceof TVIRefTour_ComparedTour) {

               final TVIRefTour_ComparedTour comparedTour = (TVIRefTour_ComparedTour) firstElement;

               final GeoComparedTour geoComparedTour = comparedTour.getGeoCompareTour();

               if (geoComparedTour != null) {

                  updateChart(
                        geoComparedTour.tourId,
                        geoComparedTour.tourFirstIndex,
                        geoComparedTour.tourLastIndex);

               } else {

                  updateChart(comparedTour.getTourId());
               }

            } else if (firstElement instanceof TVIElevationCompareResult_ComparedTour) {

               final TVIElevationCompareResult_ComparedTour compareResultItem = (TVIElevationCompareResult_ComparedTour) firstElement;
               final Long tourId = compareResultItem.getTourId();
               final TourData tourData = TourManager.getInstance().getTourData(tourId);

               updateChart(tourData);

            } else if (firstElement instanceof GeoComparedTour) {

               final GeoComparedTour geoComparedTour = (GeoComparedTour) firstElement;

               updateChart(
                     geoComparedTour.tourId,
                     geoComparedTour.tourFirstIndex,
                     geoComparedTour.tourLastIndex);
            }

         } else if (selection instanceof PhotoSelection) {

            final PhotoSelection photoSelection = (PhotoSelection) selection;

            final ArrayList<Photo> allGalleryPhotos = photoSelection.galleryPhotos;

            Long tourId = null;

            allPhotoLoop:

            // get first tour id
            for (final Photo photo : allGalleryPhotos) {

               for (final Long photoTourId : photo.getTourPhotoReferences().keySet()) {

                  tourId = photoTourId;

                  break allPhotoLoop;
               }
            }

            if (tourId != null) {
               updateChart(tourId);
            }

         } else if (selection instanceof SelectionDeletedTours) {

            clearView();
         }
      }
      _isInSelectionChanged = false;
   }

   @Override
   public void photoEvent(final IViewPart viewPart, final PhotoEventId photoEventId, final Object data) {

      if (photoEventId == PhotoEventId.PHOTO_SELECTION && data instanceof TourPhotoLinkSelection) {

         final TourPhotoLinkSelection linkSelection = (TourPhotoLinkSelection) data;

         onSelectionChanged(linkSelection);
      }
   }

   private void restoreState() {

      _tourChart.restoreState();
   }

   private void saveState() {

      if (_tourChart == null) {
         // this occurred when testing
         return;
      }

      _tourChart.saveState();
   }

   @Override
   public void setFocus() {

      _tourChart.setFocus();

      /*
       * Fire tour selection
       */
      if (_tourData == null) {

         _postSelectionProvider.clearSelection();

      } else {

         final SelectionTourData selection = new SelectionTourData(_tourChart, _tourData);

         _postSelectionProvider.setSelectionNoFireEvent(selection);

//         fireSliderPosition();
      }
   }

   private void setSliderPositions(final int leftSliderValuesIndex,
                                   final int rightSliderValuesIndex,
                                   final boolean isCenterSliderPosition) {

      final SelectionChartXSliderPosition xSliderPosition = new SelectionChartXSliderPosition(
            _tourChart,
            leftSliderValuesIndex,
            rightSliderValuesIndex);

      xSliderPosition.setCenterSliderPosition(isCenterSliderPosition);

      _tourChart.selectXSliders(xSliderPosition);
   }

   private void setupUndoContext() {

      final IViewSite viewSite = getViewSite();
      final IActionBars actionBars = viewSite.getActionBars();

      // 1. Get or create your unique Undo Context
      // Usually specific to your editor instance to avoid affecting other parts
      _undoContext = new UndoContext();

      _undoActionHandler = new UndoActionHandler(getViewSite(), _undoContext);
      _redoActionHandler = new RedoActionHandler(getViewSite(), _undoContext);

      // 3. Register it as the global Action Handler for the Undo command
      actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), _undoActionHandler);
      actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), _redoActionHandler);

      // 4. Update the action bars to apply changes
      actionBars.updateActionBars();
   }

   private void showTour() {

      final ISelection selection = getSite().getWorkbenchWindow().getSelectionService().getSelection();
      onSelectionChanged(selection);

      if (_tourData == null) {

         _pageBook.showPage(_pageNoData);

         // a tour is not displayed, find a tour provider which provides a tour
         _pageBook.getDisplay().asyncExec(() -> {

            // validate widget
            if (_pageBook.isDisposed()) {
               return;
            }

            /*
             * check if tour was set from a selection provider
             */
            if (_tourData != null) {
               return;
            }

            final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();
            if (selectedTours != null && selectedTours.size() > 0) {
               updateChart(selectedTours.get(0));
            }
         });
      }
   }

   /**
    * @param tourData
    *
    * @return Returns <code>false</code> when user has canceled the action
    */
   private boolean undoRedo_AskUser_ForModifiedTour() {

      final MessageDialog_WithRadioOptions dialog = new MessageDialog_WithRadioOptions(

            Display.getDefault().getActiveShell(),

            "Tour Chart",
            null,
//            "The current tour\n\n\"%s\"\n\nhas been modified but not saved. Selecting another tour will overwrite these modifications, select an option:"
//            "The current tour\n\n\"%s\"\n\nhas been modified but not saved. Another tour is selected, what should be done?"
//            "Your changes to the current tour \n\n\"%s\"\n\n have not been saved. Another tour was selected, what should be done?"
            "You selected a new tour.\n\nDo you want to save your changes to \"%s\" ?"
                  .formatted(TourManager.getTourTitleDetailed(_tourData)),

            MessageDialog.QUESTION,

            0, // default button index
            IDialogConstants.OK_LABEL);

      final String[] allOptions = new String[] {

            "&Cancel and keep changes", // 0
            "&Save changes", // 1
            "&Discard changes", // 2
      };

      dialog.setRadioOptions(allOptions, 0);

      if (dialog.open() == Window.OK) {

         switch (dialog.getSelectedOption()) {

         case 1:

            // save tour

            doSave();

            return true;

         case 2:

            // discard modifications

            doRestore();

            return true;

         case 0: // canceled
         default:

            // activate this view

// this produces a lot of tour change events
//          Util.showView(ID, true);
         }
      }

      return false;
   }

   /**
    * @param tourData_Cloned_Before
    * @param tourData_Cloned_WithRemovedTimeSliced
    * @param firstIndex
    * @param lastIndex
    */
   void undoRedo_DeleteTimeSlice(final TourData tourData_Cloned_Before,
                                 final TourData tourData_Cloned_WithRemovedTimeSliced,
                                 final int firstIndex,
                                 final int lastIndex) {

      // Inside an action listener or command handler
      final TourDataUndoOperation op = new TourDataUndoOperation(

            "%d: %s".formatted(
                  ++_undoCounter,
                  TimeTools.Formatter_DateTime_SM.format(TimeTools.now())),

            this,

            tourData_Cloned_Before, // old
            tourData_Cloned_WithRemovedTimeSliced, // new

            firstIndex,
            lastIndex);

      // Assign the context scope
      op.addContext(_undoContext);

      try {

         // Execute via the history framework
         final IOperationHistory opHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();

         // this will call undoRedo_Execute()
         opHistory.execute(op, null, null);

      } catch (final ExecutionException e) {

         StatusUtil.log(e);
      }
   }

   void undoRedo_Execute(final int firstSerieIndex, final int lastSerieIndex) {

      undoRedo_UpdateChart(_tourData, firstSerieIndex, lastSerieIndex, true, false);
   }

   void undoRedo_Redo(final TourData tourData_Cloned_WithRemovedTimeSliced,
                      final int firstSerieIndex,
                      final int lastSerieIndex) {

      _tourData.undoRedo_RevertTourData(tourData_Cloned_WithRemovedTimeSliced, firstSerieIndex, lastSerieIndex);

      undoRedo_UpdateChart(_tourData, firstSerieIndex, lastSerieIndex, true, false);
   }

   void undoRedo_Undo(final TourData tourData_Cloned_Before,
                      final int firstSerieIndex,
                      final int lastSerieIndex) {

      final IOperationHistory opHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
      final IUndoableOperation[] undoHistory = opHistory.getUndoHistory(_undoContext);

      boolean isTourDirty = true;

      if (undoHistory.length == 1) {

         /*
          * Because we are currently performing an undo, so when this undo is done, then the
          * undo history is empty -> all is undone -> tour is not dirty anymore
          */

         isTourDirty = false;
      }

      _tourData.undoRedo_RevertTourData(tourData_Cloned_Before, firstSerieIndex, lastSerieIndex);

      undoRedo_UpdateChart(_tourData, firstSerieIndex, lastSerieIndex, isTourDirty, true);
   }

   private void undoRedo_UpdateChart(final TourData tourData,
                                     final int firstSerieIndex,
                                     final int lastSerieIndex,
                                     final boolean isTourDirty,
                                     final boolean isUndo) {

      updateChart(tourData,

            true, // isKeepMinMaxValues
            isTourDirty);

      final int indexDiff = lastSerieIndex - firstSerieIndex;

      final int newLastIndex = isUndo
            ? lastSerieIndex + 1
            : lastSerieIndex - indexDiff;

      final int newFirstIndex = isUndo
            ? firstSerieIndex - 1
            : newLastIndex - 1;

      setSliderPositions(
            newFirstIndex,
            newLastIndex,
            false);

      // update undo/redo enablement
      getViewSite().getActionBars().updateActionBars();
   }

   /**
    * Create virtual tour which contains multiple tours.
    *
    * @param tourIds
    */
   private void updateChart(final ArrayList<Long> tourIds) {

      final TourData multipleTourData = TourManager.createJoinedTourData(tourIds);

      updateChart(multipleTourData);

      fireSliderPosition();
   }

   private void updateChart(final long tourId) {

      if (_tourData != null && _tourData.getTourId() == tourId && _isForceUpdate == false) {
         // optimize
         return;
      }

      final TourData tourData = TourManager.getInstance().getTourData(tourId);

      updateChart(tourData);

      fireSliderPosition();
   }

   private void updateChart(final long tourId,
                            final int leftSliderValuesIndex,
                            final int rightSliderValuesIndex) {

      final TourData tourData = TourManager.getInstance().getTourData(tourId);

      if (tourData == null) {
         return;
      }

      if (_tourData == null || _tourData.equals(tourData) == false) {
         updateChart(tourData);
      }

      setSliderPositions(leftSliderValuesIndex, rightSliderValuesIndex, true);
   }

   private void updateChart(final TourData tourData) {

      updateChart(tourData, false, false);
   }

   private void updateChart(final TourData tourData,
                            final boolean isKeepMinMaxValues,
                            final boolean isTourDirty) {

      if (tourData == null) {
         // nothing to do
         return;
      }

      boolean isOtherTourID = true;

      if (_tourData != null) {

         final Long oldTourID = _tourData.getTourId();
         final Long newTourID = tourData.getTourId();

         isOtherTourID = oldTourID.equals(newTourID) == false;
      }

      if (isOtherTourID) {

         // this is another tour

         if (_tourChart.isTourDirty()) {

            // tour is dirty -> ask user what to do

            if (undoRedo_AskUser_ForModifiedTour()) {

               // tour is saved or reverted

            } else {

               // dialog is canceled -> keep current state

               return;
            }
         }

         // dispose undo from the last tour
         disposeUndo();
      }

      _tourData = tourData;

      TourManager.getInstance().setActiveTourChart(_tourChart);

      _pageBook.showPage(_tourChart);

      // set or reset photo link
      _tourData.tourPhotoLink = _tourPhotoLink;

      _tourChart.updateTourChart(_tourData, _tourChartConfig, isKeepMinMaxValues, isTourDirty);

      // set application window title tool tip
      setTitleToolTip(TourManager.getTourDateShort(_tourData));
   }
}
