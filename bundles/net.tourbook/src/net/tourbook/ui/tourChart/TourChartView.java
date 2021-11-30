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
package net.tourbook.ui.tourChart;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.IHoveredValueListener;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.MouseWheelMode;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.photo.IPhotoEventListener;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoEventId;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.PhotoSelection;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.ITourModifyListener;
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
import net.tourbook.ui.UI;
import net.tourbook.ui.views.geoCompare.GeoCompareEventId;
import net.tourbook.ui.views.geoCompare.GeoCompareManager;
import net.tourbook.ui.views.geoCompare.GeoPartComparerItem;
import net.tourbook.ui.views.geoCompare.IGeoCompareListener;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;
import net.tourbook.ui.views.tourCatalog.TourCatalogView_ComparedTour;
import net.tourbook.ui.views.tourSegmenter.TourSegmenterView;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

// author: Wolfgang Schramm
// create: 09.07.2007

/**
 * Shows the selected tour in a chart
 */
public class TourChartView extends ViewPart implements ITourChartViewer, IPhotoEventListener, ITourModifyListener,
      IGeoCompareListener {

   public static final String      ID         = "net.tourbook.views.TourChartView"; //$NON-NLS-1$

   private final IDialogSettings   _state     = TourbookPlugin.getState(ID);
   private final IPreferenceStore  _prefStore = TourbookPlugin.getPrefStore();

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

   private FormToolkit             _tk;

//   @Inject
//   private IThemeManager           manager;
//
//   @Inject
//   private IThemeEngine            engine;
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
//               _isPartActive = true;
            }
         }

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {}

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == TourChartView.this) {
//               _isPartActive = false;
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
         public void partOpened(final IWorkbenchPartReference partRef) {}

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
               _tourChartConfig = TourManager.createDefaultTourChartConfig();

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
    * listen for events when a tour is selected
    */
   private void addSelectionListener() {

      _postSelectionListener = new ISelectionListener() {
         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

            if (part == TourChartView.this) {
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
               final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
               if (modifiedTours != null) {

                  final long chartTourId = _tourData.getTourId();

                  // update chart with the modified tour
                  for (final TourData tourData : modifiedTours) {

                     if (tourData == null) {

                        /*
                         * tour is not set, this can be the case when a manual tour is discarded
                         */

                        clearView();

                        return;
                     }

                     if (tourData.getTourId() == chartTourId) {

                        updateChart(tourData);

                        // removed old tour data from the selection provider
                        _postSelectionProvider.clearSelection();

                        return;
                     }
                  }

                  // ensure that wrong data are not displayed
                  clearView();
               }

            } else if (eventId == TourEventId.TOUR_CHANGED) {

               if (_tourData == null) {
                  return;
               }

               if (_tourData.isMultipleTours()) {

                  clearView();
               }

            } else if ((eventId == TourEventId.TOUR_SELECTION //
                  || eventId == TourEventId.SLIDER_POSITION_CHANGED)

                  && eventData instanceof ISelection) {

               if (part instanceof TourCatalogView_ComparedTour) {

                  // ignore -> this would modify the geo compare tour

                  return;
               }

               onSelectionChanged((ISelection) eventData);

            } else if (eventId == TourEventId.MARKER_SELECTION && eventData instanceof SelectionTourMarker) {

               onSelectionChanged_TourMarker((SelectionTourMarker) eventData);

            } else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

               clearView();

            } else if (eventId == TourEventId.UPDATE_UI) {

               // check if this tour chart contains a tour which must be updated

               if (_tourData == null) {
                  return;
               }

               final Long tourId = _tourData.getTourId();

               // update editor
               if (UI.containsTourId(eventData, tourId) != null) {

                  // reload tour data and update chart
                  updateChart(TourManager.getInstance().getTourData(tourId));
               }
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
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

      _tourChartConfig = TourManager.createDefaultTourChartConfig();

      _tourChartConfig.canUseGeoCompareTool = true;

      // set chart title
      _tourChart.addDataModelListener(new IDataModelListener() {
         @Override
         public void dataModelChanged(final ChartDataModel chartDataModel) {
//            chartDataModel.setTitle(TourManager.getTourTitleDetailed(_tourData));
         }
      });

      _tourChart.addTourModifyListener(this);

      // fire a slider move selection when a slider was moved in the tour chart
      _tourChart.addSliderMoveListener(new ISliderMoveListener() {
         @Override
         public void sliderMoved(final SelectionChartInfo chartInfoSelection) {
            fireSliderPosition();
         }
      });

      _tourChart.addHoveredValueListener(new IHoveredValueListener() {

         @Override
         public void hoveredValue(final int hoveredValuePointIndex) {
            fireHoveredValue(hoveredValuePointIndex);
         }
      });
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

      super.dispose();
   }

   private void fireHoveredValue(final int hoveredValuePointIndex) {

      final HoveredValueData hoveredValueData = new HoveredValueData(hoveredValuePointIndex);

      TourManager.fireEventWithCustomData(
            TourEventId.HOVERED_VALUE_POSITION,
            hoveredValueData,
            TourChartView.this);
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
      // TODO Auto-generated method stub

      if (part == TourChartView.this) {
         return;
      }

      switch (eventId) {

      case SET_COMPARING_ON:

         break;

      case SET_COMPARING_OFF:

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

   private void initUI(final Composite parent) {

      _tk = new FormToolkit(parent.getDisplay());
   }

   private void onSelectionChanged(final ISelection selection) {

      // prevent to listen to own events
      if (_isInSliderPositionFired) {
         return;
      }

      if (_isInSaving) {
         return;
      }

//      System.out.println((net.tourbook.common.UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//            + ("\t\t\tonSelectionChanged:\t" + selection));
//      // TODO remove SYSTEM.OUT.PRINTLN

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

         } else if (selection instanceof SelectionTourCatalogView) {

            final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

            final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
            if (refItem != null) {
               updateChart(refItem.getTourId());
            }

         } else if (selection instanceof StructuredSelection) {

            final Object firstElement = ((StructuredSelection) selection).getFirstElement();
            if (firstElement instanceof TVICatalogComparedTour) {

               updateChart(((TVICatalogComparedTour) firstElement).getTourId());

            } else if (firstElement instanceof TVICompareResultComparedTour) {

               final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
               final TourData tourData = TourManager.getInstance().getTourData(compareResultItem.getTourId());
               updateChart(tourData);

            } else if (firstElement instanceof GeoPartComparerItem) {

               final GeoPartComparerItem geoCompareItem = (GeoPartComparerItem) firstElement;

               updateChart(
                     geoCompareItem.tourId,
                     geoCompareItem.tourFirstIndex,
                     geoCompareItem.tourLastIndex);

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

   private void onSelectionChanged_TourMarker(final SelectionTourMarker markerSelection) {

      _isInSelectionChanged = true;
      {
         final TourData tourData = markerSelection.getTourData();
         final Long markerTourId = tourData.getTourId();

         /*
          * check if the marker tour is displayed
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

            final SelectionChartXSliderPosition xSliderPosition = new SelectionChartXSliderPosition(
                  _tourChart,
                  SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION,
                  leftSliderValueIndex,
                  rightSliderValueIndex);

            xSliderPosition.setCenterSliderPosition(true);

            _tourChart.selectXSliders(xSliderPosition);
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
       * fire tour selection
       */
      if (_tourData == null) {

         _postSelectionProvider.clearSelection();

      } else {

         final SelectionTourData selection = new SelectionTourData(_tourChart, _tourData);

         _postSelectionProvider.setSelectionNoFireEvent(selection);

         fireSliderPosition();
      }
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

   @Override
   public void tourIsModified(final TourData tourData) {

      _isInSaving = true;

      final TourData savedTourData = TourManager.saveModifiedTour(tourData);

      updateChart(savedTourData);

      _isInSaving = false;
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

   private void updateChart(final long tourId, final int leftSliderValuesIndex, final int rightSliderValuesIndex) {

      final TourData tourData = TourManager.getInstance().getTourData(tourId);
      if (tourData != null) {

         if (_tourData == null || _tourData.equals(tourData) == false) {
            updateChart(tourData);
         }

         // set slider position
         final SelectionChartXSliderPosition xSliderPosition = new SelectionChartXSliderPosition(
               _tourChart,
               leftSliderValuesIndex,
               rightSliderValuesIndex);

         xSliderPosition.setCenterSliderPosition(true);

         _tourChart.selectXSliders(xSliderPosition);
      }
   }

   private void updateChart(final TourData tourData) {

      if (tourData == null) {
         // nothing to do
         return;
      }

      _tourData = tourData;

      TourManager.getInstance().setActiveTourChart(_tourChart);

      _pageBook.showPage(_tourChart);

      // set or reset photo link
      _tourData.tourPhotoLink = _tourPhotoLink;

      _tourChart.updateTourChart(_tourData, _tourChartConfig, false);

      // set application window title tool tip
      setTitleToolTip(TourManager.getTourDateShort(_tourData));
   }

}
