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
package net.tourbook.ui.views.tourCatalog;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionTourChart;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourChartViewer;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartContextProvider;
import net.tourbook.ui.tourChart.TourChartViewPart;
import net.tourbook.ui.views.geoCompare.GeoPartComparerItem;
import net.tourbook.ui.views.geoCompare.GeoPartItem;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;

// author: Wolfgang Schramm
// create: 09.07.2007

public class TourCatalogView_ReferenceTour extends TourChartViewPart implements ITourChartViewer {

   public static final String    ID           = "net.tourbook.views.tourCatalog.referenceTourView"; //$NON-NLS-1$

   private final IDialogSettings _state       = TourbookPlugin.getState(ID);

   private long                  _activeRefId = -1;

   private boolean               _isInSelectionChanged;

   private ITourEventListener    _tourEventListener;

   /*
    * UI controls
    */
   private PageBook  _pageBook;
   private Composite _pageNoData;

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {

         @Override
         public void tourChanged(final IWorkbenchPart part,
                                 final TourEventId eventId,
                                 final Object eventData) {

            if (eventId == TourEventId.UPDATE_UI) {

               // ref tour is removed -> hide tour chart and wait until another tour is selected

               _pageBook.showPage(_pageNoData);
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   @Override
   public void createPartControl(final Composite parent) {

      super.createPartControl(parent);

      createUI(parent);

      addTourEventListener();

      _pageBook.showPage(_pageNoData);

      // show current selected tour
      final ISelection selection = getSite().getWorkbenchWindow().getSelectionService().getSelection();
      if (selection != null) {
         onSelectionChanged(selection);
      }
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);

      _pageNoData = UI.createUI_PageNoData(_pageBook, Messages.UI_Label_no_chart_is_selected);

      _tourChart = new TourChart(_pageBook, SWT.FLAT, getSite().getPart(), _state);
      _tourChart.setShowZoomActions(true);
      _tourChart.setShowSlider(true);
      _tourChart.setToolBarManager(getViewSite().getActionBars().getToolBarManager(), true);
      _tourChart.setContextProvider(new TourChartContextProvider(this));
      _tourChart.setTourInfoActionsEnabled(true);

      // set chart title
      _tourChart.addDataModelListener(new IDataModelListener() {
         @Override
         public void dataModelChanged(final ChartDataModel chartDataModel) {

            if (_tourData == null) {
               return;
            }

            chartDataModel.setTitle(TourManager.getTourTitleDetailed(_tourData));
         }
      });

      // fire a slider move selection when a slider was moved in the tour chart
      _tourChart.addSliderMoveListener(new ISliderMoveListener() {
         @Override
         public void sliderMoved(final SelectionChartInfo chartInfo) {

            // prevent refireing selection
            if (_isInSelectionChanged) {
               return;
            }

            TourManager.fireEventWithCustomData(
                  TourEventId.SLIDER_POSITION_CHANGED,
                  chartInfo,
                  TourCatalogView_ReferenceTour.this);
         }
      });
   }

   @Override
   public void dispose() {

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      super.dispose();
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      final ArrayList<TourData> selectedTour = new ArrayList<>();

      if (_tourData != null) {

         selectedTour.add(_tourData);
      }

      return selectedTour;
   }

   @Override
   public TourChart getTourChart() {
      return _tourChart;
   }

   private void onSelectionChanged(final ISelection selection) {

      if (selection instanceof SelectionTourCatalogView) {

         showRefTour(((SelectionTourCatalogView) selection).getRefId());

      } else if (selection instanceof StructuredSelection) {

         final Object firstElement = ((StructuredSelection) selection).getFirstElement();

         if (firstElement instanceof TVICatalogComparedTour) {

            showRefTour(((TVICatalogComparedTour) firstElement).getRefId());

         } else if (firstElement instanceof TVICompareResultComparedTour) {

            showRefTour(((TVICompareResultComparedTour) firstElement).refTour.refId);

         } else if (firstElement instanceof GeoPartComparerItem) {

            final GeoPartItem geoPartItem = ((GeoPartComparerItem) firstElement).geoPartItem;

            showRefTour(geoPartItem.refId);
         }
      }
   }

   @Override
   protected void onSelectionChanged(final IWorkbenchPart part, final ISelection selection) {

      if (part == TourCatalogView_ReferenceTour.this) {
         return;
      }

      _isInSelectionChanged = true;
      {
         onSelectionChanged(selection);
      }
      _isInSelectionChanged = false;
   }

   @Override
   public void setFocus() {

      _tourChart.setFocus();

      _postSelectionProvider.setSelection(new SelectionTourChart(_tourChart));
   }

   /**
    * set the configuration for a reference tour
    *
    * @param compareConfig
    * @return Returns <code>true</code> then the ref tour changed
    */
   private void setTourCompareConfig(final TourCompareConfig compareConfig) {

      // save the chart slider positions for the old ref tour
      final TourCompareConfig oldRefTourConfig = ReferenceTourManager.getTourCompareConfig(_activeRefId);

      if (oldRefTourConfig != null) {

         final SelectionChartXSliderPosition oldXSliderPosition = _tourChart.getXSliderPosition();

         oldRefTourConfig.setXSliderPosition(
               new SelectionChartXSliderPosition(
                     _tourChart,
                     oldXSliderPosition.getLeftSliderValueIndex(),
                     oldXSliderPosition.getRightSliderValueIndex()));
      }

      _tourChart.addDataModelListener(new IDataModelListener() {

         @Override
         public void dataModelChanged(final ChartDataModel changedChartDataModel) {

            if (_tourData == null) {
               return;
            }

            final ChartDataXSerie xData = changedChartDataModel.getXData();
            final TourReference refTour = compareConfig.getRefTour();

            final int refTour_EndValueIndex = refTour.getEndValueIndex();
            final double[] xValues = xData.getHighValuesDouble()[0];
            if (refTour_EndValueIndex >= xValues.length) {

               // an ArrayIndexOutOfBoundsException occured but cannot be reproduced
               return;
            }

            // set marker positions
            xData.setSynchMarkerValueIndex(refTour.getStartValueIndex(), refTour_EndValueIndex);

            // set the value difference of the synch marker
            final double refTourXMarkerValue = xValues[refTour_EndValueIndex] - xValues[refTour.getStartValueIndex()];

            TourManager.fireEventWithCustomData(
                  TourEventId.REFERENCE_TOUR_CHANGED,
                  new TourPropertyRefTourChanged(_tourChart, refTour.getRefId(), refTourXMarkerValue),
                  TourCatalogView_ReferenceTour.this);

            // set title
            changedChartDataModel.setTitle(NLS.bind(
                  Messages.tourCatalog_view_label_chart_title_reference_tour,
                  refTour.getLabel(),
                  TourManager.getTourTitleDetailed(_tourData)));

         }
      });
   }

   private void showRefTour(final long refId) {

      // check if the ref tour is already displayed
      if (refId == _activeRefId) {
         return;
      }

      final TourCompareConfig tourCompareConfig = ReferenceTourManager.getTourCompareConfig(refId);
      if (tourCompareConfig == null) {
         return;
      }

      /*
       * show new ref tour
       */

      _tourData = tourCompareConfig.getRefTourData();
      _tourChartConfig = tourCompareConfig.getRefTourChartConfig();

      setTourCompareConfig(tourCompareConfig);

      // set active ref id after the configuration is set
      _activeRefId = refId;

      // ???
      _tourChart.onExecuteZoomOut(false, 1.0);

      updateChart();

   }

   @Override
   public void updateChart() {

      if (_tourData == null) {
         
         _activeRefId = -1;
         _pageBook.showPage(_pageNoData);
         
         return;
      }

      _tourChart.updateTourChart(_tourData, _tourChartConfig, false);

      _pageBook.showPage(_tourChart);

      // set application window title
      setTitleToolTip(TourManager.getTourDateShort(_tourData));
   }

}
