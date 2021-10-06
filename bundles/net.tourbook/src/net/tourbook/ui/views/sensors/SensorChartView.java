/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.sensors;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartType;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.Util;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

/**
 * Shows the selected sensor in a chart
 */
public class SensorChartView extends ViewPart {

   public static final String     ID                  = "net.tourbook.ui.views.sensors.SensorChartView.ID"; //$NON-NLS-1$

   private final IPreferenceStore _prefStore          = TourbookPlugin.getPrefStore();
   private final IDialogSettings  _state              = TourbookPlugin.getState(ID);

   private IPartListener2         _partListener;
   private PostSelectionProvider  _postSelectionProvider;
   private ISelectionListener     _postSelectionListener;

   private FormToolkit            _tk;

   private SensorDataProvider     _sensorDataProvider = new SensorDataProvider();

   /*
    * UI controls
    */
   private PageBook  _pageBook;
   private Composite _pageNoData;

   private Chart     _sensorChart;

   private void addPartListener() {

      _partListener = new IPartListener2() {

         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {}

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {}

         @Override
         public void partInputChanged(final IWorkbenchPartReference partRef) {}

         @Override
         public void partOpened(final IWorkbenchPartReference partRef) {}

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {}
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

            if (part == SensorChartView.this) {
               return;
            }

            onSelectionChanged(selection);
         }
      };
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   @Override
   public void createPartControl(final Composite parent) {

      createUI(parent);

      restoreState();

      addSelectionListener();
      addPartListener();

      // set this view part as selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));
   }

   private void createUI(final Composite parent) {

      initUI(parent);

      _pageBook = new PageBook(parent, SWT.NONE);

      _pageNoData = UI.createPage(_tk, _pageBook, Messages.UI_Label_TourIsNotSelected);

      _sensorChart = new Chart(_pageBook, SWT.FLAT);
      _sensorChart.setShowZoomActions(true);
      _sensorChart.setDrawBarChartAtBottom(false);
      _sensorChart.setToolBarManager(getViewSite().getActionBars().getToolBarManager(), false);

      _pageBook.showPage(_pageNoData);
   }

   @Override
   public void dispose() {

      saveState();

      if (_tk != null) {
         _tk.dispose();
      }

      getSite().getPage().removePostSelectionListener(_postSelectionListener);
      getViewSite().getPage().removePartListener(_partListener);

      super.dispose();
   }

   private void initUI(final Composite parent) {

      _tk = new FormToolkit(parent.getDisplay());
   }

   private void onSelectionChanged(final ISelection selection) {

      if (selection instanceof StructuredSelection) {

         final Object firstElement = ((StructuredSelection) selection).getFirstElement();

         if (firstElement instanceof SensorView.SensorItem) {

            // show selected sensor

            _pageBook.showPage(_sensorChart);

            final SensorView.SensorItem sensorItem = (SensorView.SensorItem) firstElement;

            final long sensorId = sensorItem.sensorId;

            final SensorData sensorData = _sensorDataProvider.getTourTimeData(sensorId);

            updateChart(sensorData);
         }
      }
   }

   private void restoreState() {

   }

   private void saveState() {

   }

   @Override
   public void setFocus() {

      _sensorChart.setFocus();
   }

   private void updateChart(final SensorData sensorData) {

      final ChartDataModel chartModel = new ChartDataModel(ChartType.LINE);

      // set the x-axis
      final ChartDataXSerie xData = new ChartDataXSerie(Util.convertIntToDouble(sensorData.allXValues));
      chartModel.setXData(xData);

      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.LINE,
            sensorData.allBatteryVoltage,
            true //
      );

      yData.setYTitle("Sensor Battery");
      yData.setUnitLabel("Volt");

      yData.setShowYSlider(true);
      yData.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_NO);

      chartModel.addYData(yData);

      // show the data in the chart
      _sensorChart.updateChart(chartModel, false, true);
   }

}
