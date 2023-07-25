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
package net.tourbook.ui.views.referenceTour;

import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartSyncMode;
import net.tourbook.chart.XValueMarkerListener;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourChartViewer;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartContextProvider;
import net.tourbook.ui.tourChart.TourChartViewPart;
import net.tourbook.ui.views.geoCompare.GeoCompareData;
import net.tourbook.ui.views.geoCompare.GeoCompareManager;
import net.tourbook.ui.views.geoCompare.GeoComparedTour;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;

// author: Wolfgang Schramm
// create: 06.09.2007

/**
 * Mapping for old image names to names in tour.svg
 * <p>
 * These images names are not used because of heavy trouble with workbench.xmi.
 * <p>
 * Reset UI settings <a href=
 * "https://dbeaver.com/docs/wiki/Reset-UI-settings/21.3/">https://dbeaver.com/docs/wiki/Reset-UI-settings/21.3/<a/>
 *
 * <pre>
 *
 * net.tourbook.Images.GeoCompare_Tool          geo-parts.png                  ->    tour-compare-geo-compare-tool.png
 * net.tourbook.Images.ElevationCompare_Tool    tour-map-compare-result.png    ->    tour-compare-elevation-compare-tool.png
 *                                              tour-map-comparetour.png       ->    tour-compare-compared-tour.png
 * </pre>
 */
public class ComparedTourChartView extends TourChartViewPart implements ISynchedChart, ITourChartViewer {

   public static final String ID = "net.tourbook.views.tourCatalog.comparedTourView"; //$NON-NLS-1$

// SET_FORMATTING_OFF

   private static final ImageDescriptor imageDescriptor_Graph_GeoCompare               = TourbookPlugin.getThemedImageDescriptor(Images.Graph_TourCompare_ByGeo);
   private static final ImageDescriptor imageDescriptor_Graph_ElevationCompare         = TourbookPlugin.getThemedImageDescriptor(Images.Graph_TourCompare_ByElevation);

   private static final ImageDescriptor imageDescriptor_SyncByScale_ElevationCompare   = TourbookPlugin.getThemedImageDescriptor(Images.SyncGraph_ByScale);
   private static final ImageDescriptor imageDescriptor_SyncBySize_ElevationCompare    = TourbookPlugin.getThemedImageDescriptor(Images.SyncGraph_BySize);
   private static final ImageDescriptor imageDescriptor_SyncByScale_GeoCompare         = TourbookPlugin.getThemedImageDescriptor(Images.SyncGeoGraph_ByScale);
   private static final ImageDescriptor imageDescriptor_SyncBySize_GeoCompare          = TourbookPlugin.getThemedImageDescriptor(Images.SyncGeoGraph_BySize);

// SET_FORMATTING_ON

   private final IDialogSettings _state = TourbookPlugin.getState(ID);

   private boolean               _isInRefTourChanged;
   private boolean               _isInSelectionChanged;

   /*
    * Keep data from the reference tour view
    */
   private TourCompareConfig                 _refTour_CompareConfig;
   private TourChart                         _refTour_TourChart;
   private double                            _refTour_XValueDifference;

   private boolean                           _isGeoCompareTour;

   /**
    * Entity ID for the {@link TourCompared} instance or <code>-1</code> when it's not saved in the
    * database
    */
   private long                              _comparedTour_ComparedItemId = -1;

   /**
    * Tour Id for the displayed compared tour
    */
   private long                              _comparedTour_TourId         = -1;

   /**
    * Entity ID for the reference tour of the displayed compared tour
    */
   private long                              _comparedTour_RefId          = -1;

   /**
    * Reference tour chart for the displayed compared tour, chart is used for the synchronization
    */
   private TourChart                         _comparedTour_RefTourChart;

   private ITourEventListener                _tourEventListener;
   private XValueMarkerListener              _xValueMarker_DraggingListener;

   private ActionSynchChartHorizontalByScale _actionSynchChartsByScale;
   private ActionSynchChartHorizontalBySize  _actionSynchChartsBySize;

   private ActionNavigatePreviousTour        _actionNavigatePrevTour;
   private ActionNavigateNextTour            _actionNavigateNextTour;
   private ActionSaveComparedTour            _actionSaveComparedTour;
   private ActionSaveAndNextComparedTour     _actionSaveAndNext_ComparedTour;
   private ActionUndoChanges                 _actionUndoChanges;

   private boolean                           _isDataDirty;

   /*
    * 3 positons for the marker are available: computed, default(saved) and moved
    */
   private int    _computedStartIndex;
   private int    _computedEndIndex;

   private int    _defaultStartIndex;
   private int    _defaultEndIndex;

   private int    _movedStartIndex;
   private int    _movedEndIndex;

   /**
    * Object for the currently displayed compared tour
    */
   private Object _comparedTourItem;

   /*
    * UI controls
    */

   private PageBook  _pageBook;
   private Composite _pageNoData;

   private Image     _imageGeoCompare       = TourbookPlugin.getThemedImageDescriptor(Images.TourCompare_GeoCompare_Tour).createImage();
   private Image     _imageElevationCompare = TourbookPlugin.getThemedImageDescriptor(Images.TourCompare_ElevationCompare_Tour).createImage();

   private class ActionNavigateNextTour extends Action {

      public ActionNavigateNextTour() {

         super(null, AS_PUSH_BUTTON);

         setToolTipText(Messages.RefTour_Action_NavigateNextTour);

         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.Arrow_Right));
         setDisabledImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.Arrow_Right_Disabled));
      }

      @Override
      public void run() {
         navigateTour(true);
      }
   }

   private class ActionNavigatePreviousTour extends Action {

      public ActionNavigatePreviousTour() {

         super(null, AS_PUSH_BUTTON);

         setToolTipText(Messages.RefTour_Action_NavigatePrevTour);

         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.Arrow_Left));
         setDisabledImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.Arrow_Left_Disabled));
      }

      @Override
      public void run() {
         navigateTour(false);
      }
   }

   private class ActionSaveAndNextComparedTour extends Action {

      public ActionSaveAndNextComparedTour() {

         super(null, AS_PUSH_BUTTON);

         setToolTipText(Messages.RefTour_Action_SaveAndNext);

         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_SaveAndNext));
         setDisabledImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_SaveAndNext_Disabled));

         setEnabled(false);
      }

      @Override
      public void run() {

         saveComparedTour_10_Save();

         _pageBook.getDisplay().asyncExec(() -> navigateTour(true));
      }
   }

   private class ActionSaveComparedTour extends Action {

      public ActionSaveComparedTour() {

         super(null, AS_PUSH_BUTTON);

         setToolTipText(Messages.RefTour_Action_SaveMarker);

         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Save));
         setDisabledImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Save_Disabled));

         setEnabled(false);
      }

      @Override
      public void run() {

         saveComparedTour_10_Save();
      }
   }

   private class ActionSynchChartHorizontalByScale extends Action {

      public ActionSynchChartHorizontalByScale() {

         super(null, AS_CHECK_BOX);

         setToolTipText(Messages.RefTour_Action_SyncChartsByScale_Tooltip);

         setImageDescriptor(imageDescriptor_SyncByScale_ElevationCompare);
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.SyncGraph_ByScale_Disabled));
      }

      @Override
      public void run() {

         synchCharts(isChecked(), ChartSyncMode.BY_SCALE);
      }
   }

   private class ActionSynchChartHorizontalBySize extends Action {

      public ActionSynchChartHorizontalBySize() {

         super(null, AS_CHECK_BOX);

         setToolTipText(Messages.RefTour_Action_SyncChartsBySize_Tooltip);

         setImageDescriptor(imageDescriptor_SyncBySize_ElevationCompare);
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.SyncGraph_BySize_Disabled));
      }

      @Override
      public void run() {

         synchCharts(isChecked(), ChartSyncMode.BY_SIZE);
      }
   }

   private class ActionUndoChanges extends Action {

      public ActionUndoChanges() {

         super(null, AS_PUSH_BUTTON);

         setToolTipText(Messages.RefTour_Action_UndoMarkerPosition);

         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Undo));
         setDisabledImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Undo_Disabled));

         setEnabled(false);
      }

      @Override
      public void run() {
         undoChanges();
      }
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {

         @Override
         public void tourChanged(final IWorkbenchPart part,
                                 final TourEventId eventId,
                                 final Object eventData) {

            if (eventId == TourEventId.REFERENCE_TOUR_CHANGED
                  && eventData instanceof RefTourChartChanged) {

               /*
                * Reference tour changed
                */

               final RefTourChartChanged refTourChanged = (RefTourChartChanged) eventData;

               _refTour_CompareConfig = refTourChanged.compareConfig;
               _refTour_TourChart = refTourChanged.refTourChart;
               _refTour_XValueDifference = refTourChanged.xValueDifference;

               _isInRefTourChanged = true;
               {
                  if (updateTourChart(null) == false) {
                     enableSyncActions();
                  }
               }
               _isInRefTourChanged = false;

            } else if (eventId == TourEventId.UPDATE_UI) {

               // ref tour is removed -> hide tour chart

               _pageBook.showPage(_pageNoData);
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void createActions() {

      _actionSynchChartsBySize = new ActionSynchChartHorizontalBySize();
      _actionSynchChartsByScale = new ActionSynchChartHorizontalByScale();

      _actionNavigateNextTour = new ActionNavigateNextTour();
      _actionNavigatePrevTour = new ActionNavigatePreviousTour();
      _actionSaveComparedTour = new ActionSaveComparedTour();
      _actionSaveAndNext_ComparedTour = new ActionSaveAndNextComparedTour();
      _actionUndoChanges = new ActionUndoChanges();
   }

   @Override
   public void createPartControl(final Composite parent) {

      super.createPartControl(parent);

      initUI();

      _pageBook = new PageBook(parent, SWT.NONE);

      _pageNoData = UI.createUI_PageNoData(_pageBook, Messages.UI_Label_no_chart_is_selected);

      createTourChart();
      createActions();

      fillToolbar();

      addTourEventListener();

      _pageBook.showPage(_pageNoData);

      // show current selected tour
      final ISelection selection = getSite().getWorkbenchWindow().getSelectionService().getSelection();
      if (selection != null) {
         onSelectionChanged(selection);
      }

      enableSyncActions();
   }

   private float[] createRefTourDataSerie(final TourData comparedTourData,
                                          final int comparedTour_FirstIndex,
                                          final int comparedTour_LastIndex,

                                          final TourData refTourData,
                                          final int refTour_FirstIndex) {

      final float[] compTour_DistanceSerie = comparedTourData.distanceSerie;
      final float[] refTour_ElevationSerie = refTourData.altitudeSerie;
      final float[] refTour_DistanceSerie = refTourData.distanceSerie;

      final int numRefTourSlices = refTour_DistanceSerie.length;

      final int compTour_NumSlices = compTour_DistanceSerie.length;

      final float[] compTour_RefElevationSerie = new float[compTour_NumSlices];

      int compTour_SerieIndex = comparedTour_FirstIndex;
      int refTour_SerieIndex = refTour_FirstIndex;

      float compTour_Distance = compTour_DistanceSerie[compTour_SerieIndex];

      float refTour_Elevation = refTour_ElevationSerie[refTour_SerieIndex];
      float refTour_Distance = refTour_DistanceSerie[refTour_SerieIndex];

      final float compTour_Distance_Start = compTour_Distance;
      final float refTour_Distance_Start = refTour_Distance;

      float refTour_Distance_Diff = 0;

      for (; compTour_SerieIndex <= comparedTour_LastIndex; compTour_SerieIndex++) {

         compTour_Distance = compTour_DistanceSerie[compTour_SerieIndex];

         final float compTour_Distance_Diff = compTour_Distance - compTour_Distance_Start;

         while (refTour_Distance_Diff < compTour_Distance_Diff) {

            refTour_SerieIndex++;

            if (refTour_SerieIndex >= numRefTourSlices) {
               break;
            }

            refTour_Elevation = refTour_ElevationSerie[refTour_SerieIndex];
            refTour_Distance = refTour_DistanceSerie[refTour_SerieIndex];

            refTour_Distance_Diff = refTour_Distance - refTour_Distance_Start;
         }

         compTour_RefElevationSerie[compTour_SerieIndex] = refTour_Elevation;
      }

      return compTour_RefElevationSerie;
   }

   private void createTourChart() {

      _tourChart = new TourChart(_pageBook, SWT.FLAT, getSite().getPart(), _state);
      _tourChart.setShowZoomActions(true);
      _tourChart.setShowSlider(true);
      _tourChart.setToolBarManager(getViewSite().getActionBars().getToolBarManager(), true);
      _tourChart.setContextProvider(new TourChartContextProvider(this));
      _tourChart.setTourInfoActionsEnabled(true);

      // fire a slider move selection when a slider was moved in the tour chart
      _tourChart.addSliderMoveListener(chartInfoSelection -> {

         // prevent refireing selection
         if (_isInSelectionChanged || _isInRefTourChanged) {
            return;
         }

         TourManager.fireEventWithCustomData(
               TourEventId.SLIDER_POSITION_CHANGED,
               chartInfoSelection,
               ComparedTourChartView.this);
      });

      _tourChart.addDataModelListener(changedChartDataModel -> {

         if (_tourData == null) {
            return;
         }

         final ChartDataXSerie xData = changedChartDataModel.getXData();

         /*
          * Set synch marker position, this method is also called when a graph is
          * displayed/removed
          */
         xData.setXValueMarker_ValueIndices(_movedStartIndex, _movedEndIndex);

         setRangeMarkers(xData);

         // set chart title
         changedChartDataModel.setTitle(TourManager.getTourTitleDetailed(_tourData));
      });
   }

   @Override
   public void dispose() {

      UI.disposeResource(_imageGeoCompare);
      UI.disposeResource(_imageElevationCompare);

      saveComparedTour();

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      super.dispose();
   }

   private void enableActions() {

      final boolean isXValueMarkerMoved = _defaultStartIndex != _movedStartIndex || _defaultEndIndex != _movedEndIndex;
      final boolean isElevationCompareTour = _isGeoCompareTour == false
            && (isXValueMarkerMoved || _comparedTour_ComparedItemId == -1);

      // geo compared with ref tour cannot be saved !
      _actionSaveComparedTour.setEnabled(isElevationCompareTour);
      _actionSaveAndNext_ComparedTour.setEnabled(isElevationCompareTour);
   }

   private void enableSyncActions() {

      if (_refTour_CompareConfig == null) {

         // a NPE happened when this view was opened in another perspective

         return;
      }

      // check initial value
      if (_comparedTour_RefId == -1) {

         _actionSynchChartsByScale.setEnabled(false);
         _actionSynchChartsBySize.setEnabled(false);

      } else {

         boolean isSynchEnabled = false;

         if (_comparedTour_RefId == _refTour_CompareConfig.getRefTour_RefId()
               || _comparedTour_RefId == ReferenceTourManager.getGeoCompare_RefId()) {

            // reference tour for the compared chart is displayed

            if (_comparedTour_RefTourChart != _refTour_TourChart) {
               _comparedTour_RefTourChart = _refTour_TourChart;
            }

            isSynchEnabled = true;

         } else {

            // another ref tour is displayed, disable synchronization

            if (_comparedTour_RefTourChart != null) {
               _comparedTour_RefTourChart.synchChart(false, _tourChart, ChartSyncMode.NO);
            }

            _actionSynchChartsByScale.setChecked(false);
            _actionSynchChartsBySize.setChecked(false);
         }

         _actionSynchChartsByScale.setEnabled(isSynchEnabled);
         _actionSynchChartsBySize.setEnabled(isSynchEnabled);
      }
   }

   private void fillToolbar() {

      final IToolBarManager tbm = _tourChart.getToolBarManager();

      tbm.add(_actionNavigatePrevTour);
      tbm.add(_actionNavigateNextTour);
      tbm.add(_actionSaveAndNext_ComparedTour);
      tbm.add(_actionSaveComparedTour);
      tbm.add(_actionUndoChanges);

      tbm.add(new Separator());
      tbm.add(_actionSynchChartsByScale);
      tbm.add(_actionSynchChartsBySize);

      tbm.update(true);
   }

   /**
    * update tour map and compare result view
    */
   private void fireChangeEvent(final int startIndex, final int endIndex) {

      final int elapsedTime = TourManager.computeTourDeviceTime_Elapsed(_tourData, startIndex, endIndex);

      final float avgAltimeter = _tourData.computeAvg_FromValues(_tourData.getAltimeterSerie(), _movedStartIndex, _movedEndIndex);
      final float avgPulse = _tourData.computeAvg_PulseSegment(startIndex, endIndex);
      final float maxPulse = _tourData.computeMax_FromValues(_tourData.getPulse_SmoothedSerie(), startIndex, endIndex);

      final float speed = TourManager.computeTourSpeed(_tourData, startIndex, endIndex);
      final float pace = TourManager.computeTourPace(_tourData, startIndex, endIndex);

      fireChangeEvent(startIndex, endIndex, avgAltimeter, avgPulse, maxPulse, speed, pace, elapsedTime, false);
   }

   /**
    * update tour map and compare result view
    *
    * @param startIndex
    * @param endIndex
    * @param speed
    * @param isDataSaved
    */
   private void fireChangeEvent(final int startIndex,
                                final int endIndex,
                                final float avgAltimeter,
                                final float avgPulse,
                                final float maxPulse,
                                final float speed,
                                final float pace,
                                final int tourDeviceTime_Elapsed,
                                final boolean isDataSaved) {

      final TourPropertyCompareTourChanged customData = new TourPropertyCompareTourChanged(
            _comparedTour_ComparedItemId,
            _comparedTour_TourId,
            _comparedTour_RefId,
            startIndex,
            endIndex,
            isDataSaved,
            _comparedTourItem);

      customData.avgAltimeter = avgAltimeter;
      customData.avgPulse = avgPulse;
      customData.maxPulse = maxPulse;
      customData.tourDeviceTime_Elapsed = tourDeviceTime_Elapsed;

      customData.speed = speed;
      customData.pace = pace;

      TourManager.fireEventWithCustomData(TourEventId.COMPARE_TOUR_CHANGED, customData, this);
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {
      final ArrayList<TourData> selectedTours = new ArrayList<>();
      selectedTours.add(_tourData);
      return selectedTours;
   }

   @Override
   public TourChart getTourChart() {
      return _tourChart;
   }

   private void initUI() {

      _xValueMarker_DraggingListener = new XValueMarkerListener() {

         @Override
         public double getXValueDifference() {

            return _refTour_XValueDifference;
         }

         @Override
         public void xValueMarkerIsMoved(final int movedXMarkerStartValueIndex, final int movedXMarkerEndValueIndex) {

            onMoveXValueMarker(movedXMarkerStartValueIndex, movedXMarkerEndValueIndex);
         }
      };
   }

   private void navigateTour(final boolean isNextTour) {

      Object navigatedTour = null;
      boolean isNavigated = false;

      /*
       * Firstly check geo compared tours
       */
      if (_isGeoCompareTour) {

         navigatedTour = GeoCompareManager.navigateTour(isNextTour);

         if (navigatedTour instanceof GeoComparedTour) {

            isNavigated = true;

            final GeoComparedTour geoCompareTour = (GeoComparedTour) navigatedTour;

            updateTourChart_From_GeoComparedTour(geoCompareTour);
         }
      }

      /*
       * Secondly check the elevation compared tours
       */
      if (isNavigated == false) {

         navigatedTour = ElevationCompareManager.navigateTour(isNextTour);

         if (navigatedTour instanceof TVIRefTour_ComparedTour) {

            isNavigated = true;

            final TVIRefTour_ComparedTour navigatedComparedTour = (TVIRefTour_ComparedTour) navigatedTour;

            final GeoComparedTour geoCompareTour = navigatedComparedTour.getGeoCompareTour();

            if (geoCompareTour != null) {

               updateTourChart_From_GeoComparedTour(geoCompareTour);

            } else {

               updateTourChart_From_RefTourComparedTour(navigatedComparedTour);
            }

         } else if (navigatedTour instanceof TVIElevationCompareResult_ComparedTour) {

            isNavigated = true;

            updateTourChart_From_ElevationCompareResult((TVIElevationCompareResult_ComparedTour) navigatedTour);
         }
      }

      if (isNavigated) {

         // fire selection
         _postSelectionProvider.setSelection(new StructuredSelection(navigatedTour));
      }
   }

   private void onMoveXValueMarker(final int movedValueIndex, final int movedEndIndex) {

      // update the chart
      final ChartDataModel chartDataModel = _tourChart.getChartDataModel();
      final ChartDataXSerie xData = chartDataModel.getXData();

      xData.setXValueMarker_ValueIndices(movedValueIndex, movedEndIndex);
      setRangeMarkers(xData);

      _tourChart.updateChart(chartDataModel, true);

      // keep marker position for saving the tour
      _movedStartIndex = movedValueIndex;
      _movedEndIndex = movedEndIndex;

      // check if the data are dirty
      boolean isDataDirty;
      if (_defaultStartIndex == _movedStartIndex && _defaultEndIndex == _movedEndIndex) {
         isDataDirty = false;
      } else {
         isDataDirty = true;
      }
      setDataDirty(isDataDirty);

      fireChangeEvent(_movedStartIndex, _movedEndIndex);
   }

   private void onSelectionChanged(final ISelection selection) {

      if (selection instanceof StructuredSelection) {

         final Object firstElement = ((StructuredSelection) selection).getFirstElement();

         if (firstElement instanceof TVIRefTour_ComparedTour) {

            final TVIRefTour_ComparedTour comparedTour = (TVIRefTour_ComparedTour) firstElement;
            final GeoComparedTour geoCompareTour = comparedTour.getGeoCompareTour();

            if (geoCompareTour != null) {

               updateTourChart_From_GeoComparedTour(geoCompareTour);

            } else {

               updateTourChart_From_RefTourComparedTour(comparedTour);
            }

         } else if (firstElement instanceof TVIElevationCompareResult_ComparedTour) {

            updateTourChart_From_ElevationCompareResult((TVIElevationCompareResult_ComparedTour) firstElement);

         } else if (firstElement instanceof GeoComparedTour) {

            updateTourChart_From_GeoComparedTour((GeoComparedTour) firstElement);
         }
      }
   }

   @Override
   protected void onSelectionChanged(final IWorkbenchPart part, final ISelection selection) {

      if (part == ComparedTourChartView.this) {
         return;
      }

      _isInSelectionChanged = true;
      {
         onSelectionChanged(selection);
      }
      _isInSelectionChanged = false;
   }

   /**
    * @return Returns <code>false</code> when the save dialog was canceled
    */
   private boolean saveComparedTour() {

      if (_comparedTour_ComparedItemId == -1) {
         setDataDirty(false);
         return true;
      }

      if (_isDataDirty == false) {
         return true;
      }

      final MessageBox msgBox = new MessageBox(_pageBook.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);

      msgBox.setText(Messages.RefTour_Dialog_SaveComparedTour_Title);
      msgBox.setMessage(NLS.bind(
            Messages.RefTour_Dialog_SaveComparedTour_Message,
            TourManager.getTourTitleDetailed(_tourData)));

      final int answer = msgBox.open();

      if (answer == SWT.YES) {

         saveComparedTour_10_Save();

//		} else if (answer == SWT.CANCEL) {
// disabled, pops up for every selection when multiple selections are fired
//			return false;

      } else {

         fireChangeEvent(_computedStartIndex, _computedEndIndex);
      }

      setDataDirty(false);

      return true;
   }

   private void saveComparedTour_10_Save() {

      if (_comparedTour_ComparedItemId == -1) {

         // compared tour is not yet saved

         saveComparedTour_20_SaveInitial();
      }

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      final EntityTransaction ts = em.getTransaction();

      try {

         final TourCompared comparedTour = em.find(TourCompared.class, _comparedTour_ComparedItemId);

         if (comparedTour != null) {

            final ChartDataModel chartDataModel = _tourChart.getChartDataModel();

            final float avgAltimeter = _tourData.computeAvg_FromValues(_tourData.getAltimeterSerie(), _movedStartIndex, _movedEndIndex);
            final float avgPulse = _tourData.computeAvg_PulseSegment(_movedStartIndex, _movedEndIndex);
            final float maxPulse = _tourData.computeMax_FromValues(_tourData.getPulse_SmoothedSerie(), _movedStartIndex, _movedEndIndex);
            final int elapsedTime = TourManager.computeTourDeviceTime_Elapsed(_tourData, _movedStartIndex, _movedEndIndex);
            final float speed = TourManager.computeTourSpeed(_tourData, _movedStartIndex, _movedEndIndex);
            final float pace = TourManager.computeTourPace(_tourData, _movedStartIndex, _movedEndIndex);

            // set new data in entity
            comparedTour.setStartIndex(_movedStartIndex);
            comparedTour.setEndIndex(_movedEndIndex);
            comparedTour.setAvgAltimeter(avgAltimeter);
            comparedTour.setAvgPulse(avgPulse);
            comparedTour.setMaxPulse(maxPulse);
            comparedTour.setTourSpeed(speed);
            comparedTour.setTourPace(pace);

            // update entity
            ts.begin();
            em.merge(comparedTour);
            ts.commit();

            _comparedTour_ComparedItemId = comparedTour.getComparedId();
            _comparedTour_TourId = comparedTour.getTourId();

            setDataDirty(false);

            /*
             * Update chart and viewer with new marker position
             */
            _defaultStartIndex = _movedStartIndex;
            _defaultEndIndex = _movedEndIndex;

            final ChartDataXSerie xData = chartDataModel.getXData();
            xData.setXValueMarker_ValueIndices(_defaultStartIndex, _defaultEndIndex);
            setRangeMarkers(xData);

            _tourChart.updateChart(chartDataModel, true);
            enableActions();

            fireChangeEvent(_defaultStartIndex,
                  _defaultEndIndex,
                  avgAltimeter,
                  avgPulse,
                  maxPulse,
                  speed,
                  pace,
                  elapsedTime,
                  true);
         }
      } catch (final Exception e) {
         e.printStackTrace();
      } finally {
         if (ts.isActive()) {
            ts.rollback();
         }
         em.close();
      }
   }

   /**
    * Persist the compared tours
    */
   private void saveComparedTour_20_SaveInitial() {

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      final EntityTransaction ts = em.getTransaction();

      try {

         if (_comparedTourItem instanceof TVIElevationCompareResult_ComparedTour) {

            final TVIElevationCompareResult_ComparedTour comparedTourItem = (TVIElevationCompareResult_ComparedTour) _comparedTourItem;

            ElevationCompareManager.saveComparedTourItem(comparedTourItem, em, ts);

            _comparedTour_ComparedItemId = comparedTourItem.compareId;
            _comparedTour_TourId = comparedTourItem.tourId;

            // update comparison timeline view
            final SelectionPersistedCompareResults persistedCompareResults = new SelectionPersistedCompareResults();
            persistedCompareResults.persistedCompareResults.add(comparedTourItem);

            _postSelectionProvider.setSelection(persistedCompareResults);
         }

      } catch (final Exception e) {

         e.printStackTrace();

      } finally {
         if (ts.isActive()) {
            ts.rollback();
         }
         em.close();
      }
   }

   private void setDataDirty(final boolean isDirty) {

      _isDataDirty = isDirty;

      enableActions();

      _actionUndoChanges.setEnabled(isDirty);
   }

   @Override
   public void setFocus() {

      _tourChart.setFocus();

      _postSelectionProvider.setSelection(new SelectionTourChart(_tourChart));
   }

   private void setRangeMarkers(final ChartDataXSerie xData) {

      if (_comparedTourItem instanceof TVIRefTour_ComparedTour
            || _comparedTourItem instanceof GeoComparedTour) {

         xData.setRangeMarkers(
               new int[] { _defaultStartIndex },
               new int[] { _defaultEndIndex });

      } else if (_comparedTourItem instanceof TVIElevationCompareResult_ComparedTour) {

         xData.setRangeMarkers(
               new int[] { _defaultStartIndex, _computedStartIndex },
               new int[] { _defaultEndIndex, _computedEndIndex });
      }
   }

   @Override
   public void synchCharts(final boolean isSynched, final ChartSyncMode chartSyncMode) {

      if (_comparedTour_RefTourChart != null) {

         // uncheck the other synch mode
         switch (chartSyncMode) {
         case BY_SCALE:

            _actionSynchChartsBySize.setChecked(false);
            break;

         case BY_SIZE:

            _actionSynchChartsByScale.setChecked(false);
            break;

         default:
            break;
         }

         _comparedTour_RefTourChart.synchChart(isSynched, _tourChart, chartSyncMode);
      }
   }

   private void undoChanges() {

      // set synch marker to original position
      final ChartDataModel chartDataModel = _tourChart.getChartDataModel();
      final ChartDataXSerie xData = chartDataModel.getXData();

      _movedStartIndex = _defaultStartIndex;
      _movedEndIndex = _defaultEndIndex;

      xData.setXValueMarker_ValueIndices(_defaultStartIndex, _defaultEndIndex);

      _tourChart.updateChart(chartDataModel, true);

      setDataDirty(false);

      fireChangeEvent(_defaultStartIndex, _defaultEndIndex);
   }

   @Override
   protected void updateChart() {

      if (_tourData == null) {

         _refTour_CompareConfig = null;

         _comparedTour_TourId = -1;
         _comparedTour_RefId = -1;
         _comparedTour_ComparedItemId = -1;

         _pageBook.showPage(_pageNoData);

         return;
      }

      _tourChart.updateTourChart(_tourData, _tourChartConfig, false);

      _pageBook.showPage(_tourChart);

      // set part tooltip
      setTitleToolTip(TourManager.getTourDateShort(_tourData));
   }

   private void updateSyncActions() {

      if (_isGeoCompareTour) {

         setTitleImage(_imageGeoCompare);
         setPartName(Messages.Tour_Compare_ViewName_GeoComparedTour);

         _actionSynchChartsByScale.setImageDescriptor(imageDescriptor_SyncByScale_GeoCompare);
         _actionSynchChartsBySize.setImageDescriptor(imageDescriptor_SyncBySize_GeoCompare);

         _tourChart.setGraphActionImage(TourManager.GRAPH_TOUR_COMPARE, imageDescriptor_Graph_GeoCompare);

      } else {

         setTitleImage(_imageElevationCompare);
         setPartName(Messages.Tour_Compare_ViewName_ElevationComparedTour);

         _actionSynchChartsByScale.setImageDescriptor(imageDescriptor_SyncByScale_ElevationCompare);
         _actionSynchChartsBySize.setImageDescriptor(imageDescriptor_SyncBySize_ElevationCompare);

         _tourChart.setGraphActionImage(TourManager.GRAPH_TOUR_COMPARE, imageDescriptor_Graph_ElevationCompare);
      }
   }

   /**
    * @param isGeoComparedTourChecked
    * @return Returns <code>false</code> when the compared tour is not displayed
    */
   private boolean updateTourChart(final Boolean isGeoComparedTourChecked) {

      final TourCompareConfig tourCompareConfig = ReferenceTourManager.getTourCompareConfig(_comparedTour_RefId);

      if (tourCompareConfig == null) {
         return false;
      }

      final TourCompareType tourCompareType = tourCompareConfig.getTourCompareType();

      final boolean isGeoCompareTour = isGeoComparedTourChecked != null

            ? isGeoComparedTourChecked

            : (tourCompareType.equals(TourCompareType.GEO_COMPARE_ANY_TOUR)
                  || tourCompareType.equals(TourCompareType.GEO_COMPARE_REFERENCE_TOUR));

      _isGeoCompareTour = isGeoCompareTour;

      _tourChartConfig = tourCompareConfig.getCompareTourChartConfig();

      _tourChartConfig.setMinMaxKeeper();
      _tourChartConfig.canShowTourCompareGraph = true;
      _tourChartConfig.isGeoCompare = isGeoCompareTour;

      updateSyncActions();
      updateChart();
      enableSyncActions();
      enableActions();

      /*
       * Allow dragging of the compared tour only for elevation compared tour as they are saved
       * currently only into the db
       */
      _tourChart.setXValueMarker_DraggingListener(isGeoCompareTour
            ? null
            : _xValueMarker_DraggingListener);

      /*
       * fire change event to update tour markers
       */
      _postSelectionProvider.setSelection(new SelectionTourData(_tourChart, _tourChart.getTourData()));

      return true;
   }

   private void updateTourChart_From_ElevationCompareResult(final TVIElevationCompareResult_ComparedTour elevationComparedResultTour) {

      if (saveComparedTour() == false) {
         return;
      }

      final Long eleTourId = elevationComparedResultTour.getTourId();

      // check if the compared tour is already displayed
      if (_comparedTour_TourId == eleTourId && _comparedTourItem instanceof TVIElevationCompareResult_ComparedTour) {
         return;
      }

      // load tour data of the compared tour from the database
      final TourData compTourData = TourManager.getInstance().getTourData(eleTourId);
      if (compTourData == null) {
         return;
      }

      final RefTourItem refTourItem = elevationComparedResultTour.refTour;

      // keep data from the selected compared tour
      _comparedTour_TourId = eleTourId;
      _comparedTour_RefId = refTourItem.refId;
      _comparedTour_ComparedItemId = elevationComparedResultTour.compareId;

      _tourData = compTourData;

      if (_comparedTour_ComparedItemId == -1) {

         // compared tour is not saved

         _defaultStartIndex = _computedStartIndex = _movedStartIndex = elevationComparedResultTour.computedStartIndex;
         _defaultEndIndex = _computedEndIndex = _movedEndIndex = elevationComparedResultTour.computedEndIndex;

      } else {

         // compared tour is saved

         _defaultStartIndex = _movedStartIndex = elevationComparedResultTour.dbStartIndex;
         _defaultEndIndex = _movedEndIndex = elevationComparedResultTour.dbEndIndex;

         _computedStartIndex = elevationComparedResultTour.computedStartIndex;
         _computedEndIndex = elevationComparedResultTour.computedEndIndex;
      }

      _comparedTourItem = elevationComparedResultTour;

      final int compTour_FirstIndex = _defaultStartIndex;
      final int compTour_LastIndex = _defaultEndIndex;

      final TourData refTourData = TourManager.getInstance().getTourData(refTourItem.tourId);
      final int refTour_FirstIndex = refTourItem.startIndex;

      // set tour compare data, this will show the action button to see the graph for this data
      _tourData.tourCompare_DiffSerie = elevationComparedResultTour.altitudeDiffSerie;
      _tourData.tourCompare_ReferenceSerie = createRefTourDataSerie(

            compTourData,
            compTour_FirstIndex,
            compTour_LastIndex,

            refTourData,
            refTour_FirstIndex);

      updateTourChart(false);

      // enable action after the chart was created
      _tourChart.enableGraphAction(TourManager.GRAPH_TOUR_COMPARE, true);
      _tourChart.enableGraphAction(TourManager.GRAPH_TOUR_COMPARE_REF_TOUR, true);
      updateSyncActions();
   }

   private void updateTourChart_From_GeoComparedTour(final GeoComparedTour geoComparedTour) {

      if (saveComparedTour() == false) {
         return;
      }

      final long geoTourId = geoComparedTour.tourId;

      // check if the compared tour is already displayed
      if (_comparedTour_TourId == geoTourId && _comparedTourItem instanceof GeoComparedTour) {
         return;
      }

      // load tourdata of the compared tour from the database
      final TourData compTourData = TourManager.getInstance().getTourData(geoTourId);
      if (compTourData == null) {
         return;
      }

      final GeoCompareData geoCompareData = geoComparedTour.geoCompareData;

      // set data from the selection
      _comparedTour_TourId = geoTourId;
      _comparedTour_RefId = geoCompareData.refTour_RefId;
      _comparedTour_ComparedItemId = -1;

      _tourData = compTourData;

      final int compTour_FirstIndex = geoComparedTour.tourFirstIndex;
      final int compTour_LastIndex = geoComparedTour.tourLastIndex;

      final TourData refTourData = TourManager.getInstance().getTourData(geoCompareData.refTour_TourId);
      final int refTour_FirstIndex = geoCompareData.refTour_FirstIndex;

      // set tour compare data, this will enable the action button to see the graph for this data
      _tourData.tourCompare_DiffSerie = geoComparedTour.tourLatLonDiff;
      _tourData.tourCompare_ReferenceSerie = createRefTourDataSerie(

            compTourData,
            compTour_FirstIndex,
            compTour_LastIndex,

            refTourData,
            refTour_FirstIndex);

      _defaultStartIndex = _movedStartIndex = _computedStartIndex = geoComparedTour.tourFirstIndex;
      _defaultEndIndex = _movedEndIndex = _computedEndIndex = geoComparedTour.tourLastIndex;

      _comparedTourItem = geoComparedTour;

      updateTourChart(true);

      // enable action after the chart was created
      _tourChart.enableGraphAction(TourManager.GRAPH_TOUR_COMPARE, true);
      _tourChart.enableGraphAction(TourManager.GRAPH_TOUR_COMPARE_REF_TOUR, true);
      updateSyncActions();
   }

   /**
    * Shows the compared tour which was selected by the user in the {@link ReferenceTourView}
    *
    * @param selectionComparedTour
    */
   private void updateTourChart_From_RefTourComparedTour(final TVIRefTour_ComparedTour refTourComparedTour) {

      if (saveComparedTour() == false) {
         return;
      }

      final Long ctTourId = refTourComparedTour.getTourId();

      // check if the compared tour is already displayed
      if (_comparedTour_TourId == ctTourId.longValue() && _comparedTourItem instanceof TVIRefTour_ComparedTour) {
         return;
      }

      // load the tourdata of the compared tour from the database
      final TourData compTourData = TourManager.getInstance().getTourData(ctTourId);
      if (compTourData == null) {
         return;
      }

      // set data from the selection
      _comparedTour_TourId = ctTourId;
      _comparedTour_RefId = refTourComparedTour.getRefId();
      _comparedTour_ComparedItemId = refTourComparedTour.getCompareId();

      _tourData = compTourData;

      /*
       * Remove tour compare data (when there are any), but set dummy object to display the action
       * button
       */
      _tourData.tourCompare_DiffSerie = new float[0];

      _defaultStartIndex = _movedStartIndex = _computedStartIndex = refTourComparedTour.getStartIndex();
      _defaultEndIndex = _movedEndIndex = _computedEndIndex = refTourComparedTour.getEndIndex();

      _comparedTourItem = refTourComparedTour;

      /*
       * Load reference tour data serie
       */
      final int compTour_FirstIndex = _defaultStartIndex;
      final int compTour_LastIndex = _defaultEndIndex;

      final TourReference refTour = ReferenceTourManager.getReferenceTour(_comparedTour_RefId);
      final TourData refTourData = refTour.getTourData();
      final int refTour_FirstIndex = refTour.getStartIndex();

      // set tour compare data, this will show the action button to see the graph for this data
      _tourData.tourCompare_ReferenceSerie = createRefTourDataSerie(

            compTourData,
            compTour_FirstIndex,
            compTour_LastIndex,

            refTourData,
            refTour_FirstIndex);

      updateTourChart(false);

      // disable action after the chart was created
      _tourChart.enableGraphAction(TourManager.GRAPH_TOUR_COMPARE, false);
      _tourChart.enableGraphAction(TourManager.GRAPH_TOUR_COMPARE_REF_TOUR, true);
   }

}
