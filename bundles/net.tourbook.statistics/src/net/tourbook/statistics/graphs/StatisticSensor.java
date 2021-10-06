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
package net.tourbook.statistics.graphs;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartType;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourPerson;
import net.tourbook.statistic.StatisticContext;
import net.tourbook.statistic.TourbookStatistic;
import net.tourbook.statistics.Messages;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.ui.ChartOptions_Grid;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

public class StatisticSensor extends TourbookStatistic {

   private TourStatisticData_Sensor _sensorData;
   private DataProvider_Sensor      _sensorDataProvider = new DataProvider_Sensor();

   private TourPerson               _activePerson;
   private TourTypeFilter           _activeTourTypeFiler;
   private int                      _currentYear;
   private int                      _numberOfYears;

   private Chart                    _sensorChart;

   @Override
   public void createStatisticUI(final Composite parent, final IViewSite viewSite) {

      // chart widget page
      _sensorChart = new Chart(parent, SWT.FLAT);
      _sensorChart.setShowZoomActions(true);
      _sensorChart.setDrawBarChartAtBottom(false);
      _sensorChart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);
   }

   @Override
   public int getEnabledGridOptions() {

      return ChartOptions_Grid.GRID_VERTICAL_DISTANCE
            | ChartOptions_Grid.GRID_IS_SHOW_HORIZONTAL_LINE
            | ChartOptions_Grid.GRID_IS_SHOW_VERTICAL_LINE;
   }

   @Override
   protected String getGridPrefPrefix() {
      return GRID_SENSOR;
   }

   @Override
   public String getRawStatisticValues(final boolean isShowSequenceNumbers) {
      return _sensorDataProvider.getRawStatisticValues(isShowSequenceNumbers);
   }

   @Override
   public void preferencesHasChanged() {

      updateStatistic(new StatisticContext(_activePerson, _activeTourTypeFiler, _currentYear, _numberOfYears));
   }

   @Override
   public void setSynchScale(final boolean isEnabled) {

   }

   private void updateChart() {

      final ChartDataModel chartModel = new ChartDataModel(ChartType.BAR);

      // set the x-axis
      final ChartDataXSerie xData = new ChartDataXSerie(Util.convertIntToDouble(_sensorData.allXValues));
      xData.setVisibleMaxValue(_currentYear);
      chartModel.setXData(xData);

      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.LINE,
            _sensorData.allBatteryVoltage,
            true //
      );

      yData.setYTitle(Messages.LABEL_GRAPH_BATTERY);
      yData.setYTitle("Device Battery");
      yData.setUnitLabel("Volt");

      yData.setShowYSlider(true);
      yData.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_NO);

      chartModel.addYData(yData);

      /*
       * set graph minimum width, this is the number of days in the year
       */
      final int yearDays = TimeTools.getNumberOfDaysWithYear(_currentYear);
      chartModel.setChartMinWidth(yearDays);

      StatisticServices.updateChartProperties(_sensorChart, getGridPrefPrefix());

      // show the data in the chart
      _sensorChart.updateChart(chartModel, false, true);
   }

   @Override
   public void updateStatistic(final StatisticContext statContext) {

      _activePerson = statContext.appPerson;
      _activeTourTypeFiler = statContext.appTourTypeFilter;
      _currentYear = statContext.statSelectedYear;
      _numberOfYears = statContext.statNumberOfYears;

      final long sensorId = 0;

      _sensorData = _sensorDataProvider.getTourTimeData(
            statContext.appPerson,
            statContext.appTourTypeFilter,
            statContext.statSelectedYear,
            statContext.statNumberOfYears,
            isDataDirtyWithReset() || statContext.isRefreshData,
            sensorId);

      updateChart();
   }

   @Override
   public void updateToolBar() {
      _sensorChart.fillToolbar(true);
   }

}
