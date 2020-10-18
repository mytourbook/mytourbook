/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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

import java.time.LocalDate;
import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartStatisticSegments;
import net.tourbook.chart.ChartTitleSegmentConfig;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.chart.MinMaxKeeper_YData;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.DurationTime;
import net.tourbook.statistic.StatisticContext;
import net.tourbook.statistic.TourbookStatistic;
import net.tourbook.statistics.Messages;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.ui.ChartOptions_Grid;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

public abstract class StatisticYear extends TourbookStatistic {

   private final IPreferenceStore   _prefStore             = TourbookPlugin.getPrefStore();

   private TourStatisticData_Year   _statisticData_Year;
   private DataProvider_Tour_Year   _tourYear_DataProvider = new DataProvider_Tour_Year();

   private StatisticContext         _statContext;

   private TourPerson               _appPerson;
   private TourTypeFilter           _appTourTypeFilter;

   private int                      _statFirstYear;
   private int                      _statNumberOfYears;

   private Chart                    _chart;
   private String                   _chartType;

   private final MinMaxKeeper_YData _minMaxKeeper          = new MinMaxKeeper_YData();
   private ChartDataYSerie          _yData_Duration;

   private boolean                  _isSynchScaleEnabled;

   private int                      _barOrderStart;

   public boolean canTourBeVisible() {
      return false;
   }

   ChartStatisticSegments createChartSegments(final TourStatisticData_Year tourDataYear) {

      final int yearCounter = tourDataYear.elevationUp_High[0].length;

      final double segmentStart[] = new double[_statNumberOfYears];
      final double segmentEnd[] = new double[_statNumberOfYears];
      final String[] segmentTitle = new String[_statNumberOfYears];

      final int oldestYear = _statFirstYear - _statNumberOfYears + 1;

      for (int yearIndex = 0; yearIndex < yearCounter; yearIndex++) {

         segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

         segmentStart[yearIndex] = yearIndex;
         segmentEnd[yearIndex] = yearIndex;
      }

      final ChartStatisticSegments yearSegments = new ChartStatisticSegments();
      yearSegments.segmentStartValue = segmentStart;
      yearSegments.segmentEndValue = segmentEnd;
      yearSegments.segmentTitle = segmentTitle;
      yearSegments.years = tourDataYear.years;

      return yearSegments;
   }

   @Override
   public void createStatisticUI(final Composite parent, final IViewSite viewSite) {

      // create chart
      _chart = new Chart(parent, SWT.FLAT);
      _chart.setShowZoomActions(true);
      _chart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);
   }

   /**
    * @param toolTipProvider
    * @param parent
    * @param hoveredBar_VerticalIndex
    *           serieIndex
    * @param hoveredBar_HorizontalIndex
    *           valueIndex
    */
   private void createToolTipUI(final IToolTipProvider toolTipProvider,
                                final Composite parent,
                                final int hoveredBar_SerieIndex,
                                final int hoveredBar_ValueIndex) {

      /*
       * Create tooltip title
       */
      final int oldestYear = _statFirstYear - _statNumberOfYears + 1;
      final LocalDate yearDate = LocalDate.of(oldestYear, 1, 1).plusYears(hoveredBar_ValueIndex);

      final String toolTipTitle = Integer.toString(yearDate.getYear());
      final String totalColumnHeaderTitel = toolTipTitle;

      final boolean isShowPercentageValues = _prefStore.getBoolean(ITourbookPreferences.STAT_YEAR_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES);
      final boolean isShowSummaryValues = _prefStore.getBoolean(ITourbookPreferences.STAT_YEAR_TOOLTIP_IS_SHOW_SUMMARY_VALUES);

      new StatisticTooltipUI_CategorizedData().createContentArea(
            parent,
            toolTipProvider,
            _statisticData_Year,
            hoveredBar_SerieIndex,
            hoveredBar_ValueIndex,
            toolTipTitle,
            null,
            totalColumnHeaderTitel,
            isShowSummaryValues,
            isShowPercentageValues);
   }

   void createXData_Year(final ChartDataModel chartDataModel) {

      // set the x-axis
      final ChartDataXSerie xData = new ChartDataXSerie(createYearData(_statisticData_Year));
      xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_YEAR);
      xData.setChartSegments(createChartSegments(_statisticData_Year));
      chartDataModel.setXData(xData);
   }

   /**
    * Altitude
    *
    * @param chartDataModel
    */
   void createYData_Altitude(final ChartDataModel chartDataModel) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            getChartType(_chartType),
            _statisticData_Year.elevationUp_Low_Resorted,
            _statisticData_Year.elevationUp_High_Resorted);

      yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
      yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setShowYSlider(true);

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE, _appTourTypeFilter);
      StatisticServices.setTourTypeColorIndex(yData, _statisticData_Year.typeIds_Resorted, _appTourTypeFilter);

      chartDataModel.addYData(yData);
   }

   /**
    * Distance
    *
    * @param chartDataModel
    */
   void createYData_Distance(final ChartDataModel chartDataModel) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            getChartType(_chartType),
            _statisticData_Year.distance_Low_Resorted,
            _statisticData_Year.distance_High_Resorted);

      yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
      yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setValueDivisor(1000);
      yData.setShowYSlider(true);

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE, _appTourTypeFilter);
      StatisticServices.setTourTypeColorIndex(yData, _statisticData_Year.typeIds_Resorted, _appTourTypeFilter);

      chartDataModel.addYData(yData);
   }

   /**
    * Duration
    *
    * @param chartDataModel
    */
   void createYData_Duration(final ChartDataModel chartDataModel) {

      _yData_Duration = new ChartDataYSerie(
            ChartType.BAR,
            getChartType(_chartType),
            _statisticData_Year.durationTime_Low_Resorted,
            _statisticData_Year.durationTime_High_Resorted);

      _yData_Duration.setYTitle(Messages.LABEL_GRAPH_TIME);
      _yData_Duration.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
      _yData_Duration.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
      _yData_Duration.setShowYSlider(true);

      StatisticServices.setDefaultColors(_yData_Duration, GraphColorManager.PREF_GRAPH_TIME);
      StatisticServices.setTourTypeColors(_yData_Duration, GraphColorManager.PREF_GRAPH_TIME, _appTourTypeFilter);
      StatisticServices.setTourTypeColorIndex(_yData_Duration, _statisticData_Year.typeIds_Resorted, _appTourTypeFilter);

      chartDataModel.addYData(_yData_Duration);
   }

   /**
    * Number of tours
    *
    * @param chartDataModel
    */
   void createYData_NumTours(final ChartDataModel chartDataModel) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            getChartType(_chartType),
            _statisticData_Year.numTours_Low_Resorted,
            _statisticData_Year.numTours_High_Resorted);

      yData.setYTitle(Messages.LABEL_GRAPH_NUMBER_OF_TOURS);
      yData.setUnitLabel(Messages.NUMBERS_UNIT);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setShowYSlider(true);

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_TOUR);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TOUR, _appTourTypeFilter);
      StatisticServices.setTourTypeColorIndex(yData, _statisticData_Year.typeIds_Resorted, _appTourTypeFilter);

      chartDataModel.addYData(yData);
   }

   private double[] createYearData(final TourStatisticData_Year tourDataYear) {

      final int yearCounter = tourDataYear.elevationUp_High[0].length;
      final double allYears[] = new double[yearCounter];

      for (int yearIndex = 0; yearIndex < yearCounter; yearIndex++) {
         allYears[yearIndex] = yearIndex;
      }

      return allYears;
   }

   protected abstract String getBarOrderingStateKey();

   abstract ChartDataModel getChartDataModel();

   @Override
   public int getEnabledGridOptions() {

      return ChartOptions_Grid.GRID_VERTICAL_DISTANCE
            | ChartOptions_Grid.GRID_IS_SHOW_HORIZONTAL_LINE
            | ChartOptions_Grid.GRID_IS_SHOW_VERTICAL_LINE;
   }

   @Override
   public String getRawStatisticValues(final boolean isShowSequenceNumbers) {
      return _tourYear_DataProvider.getRawStatisticValues(isShowSequenceNumbers);
   }

   @Override
   public void preferencesHasChanged() {
      updateStatistic();
   }

   /**
    * Reorder statistic bars according to the sequence start
    *
    * @param statContext
    */
   private void reorderStatisticData() {

      _statisticData_Year.reorderStatisticData(_barOrderStart, _statContext.outBarNames != null);
   }

   @Override
   public void restoreStateEarly(final IDialogSettings state) {

      _barOrderStart = Util.getStateInt(state, getBarOrderingStateKey(), 0);
   }

   @Override
   public void saveState(final IDialogSettings state) {

      state.put(getBarOrderingStateKey(), _barOrderStart);
   }

   @Override
   public void setBarVerticalOrder(final int selectedIndex) {

      // selected index can be -1 when tour type combobox is empty
      _barOrderStart = selectedIndex < 0 ? 0 : selectedIndex;

      final ArrayList<TourType> tourTypes = TourDatabase.getActiveTourTypes();

      if (tourTypes == null || tourTypes.size() == 0) {
         return;
      }

      reorderStatisticData();

      updateStatistic();
   }

   private void setChartProviders(final ChartDataModel chartModel) {

      // set tool tip info
      chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {

         @Override
         public void createToolTipUI(final IToolTipProvider toolTipProvider, final Composite parent, final int serieIndex, final int valueIndex) {
            StatisticYear.this.createToolTipUI(toolTipProvider, parent, serieIndex, valueIndex);
         }
      });
   }

   @Override
   public void setSynchScale(final boolean isSynchScaleEnabled) {

      if (!isSynchScaleEnabled) {

         // reset when it's disabled

         _minMaxKeeper.resetMinMax();
      }

      _isSynchScaleEnabled = isSynchScaleEnabled;
   }

   private void updateStatistic() {
      updateStatistic(new StatisticContext(_appPerson, _appTourTypeFilter, _statFirstYear, _statNumberOfYears));
   }

   @Override
   public void updateStatistic(final StatisticContext statContext) {

      _chartType = _prefStore.getString(ITourbookPreferences.STAT_YEAR_CHART_TYPE);

      final DurationTime durationTime = (DurationTime) Util.getEnumValue(
            _prefStore.getString(ITourbookPreferences.STAT_YEAR_DURATION_TIME),
            DurationTime.MOVING);

      _statContext = statContext;

      // this statistic supports bar reordering
      statContext.outIsBarReorderingSupported = true;

      _appPerson = statContext.appPerson;
      _appTourTypeFilter = statContext.appTourTypeFilter;
      _statFirstYear = statContext.statSelectedYear;
      _statNumberOfYears = statContext.statNumberOfYears;

      _statisticData_Year = _tourYear_DataProvider.getYearData(
            statContext.appPerson,
            statContext.appTourTypeFilter,
            statContext.statSelectedYear,
            statContext.statNumberOfYears,
            isDataDirtyWithReset() || statContext.isRefreshData || _isDuration_ReloadData,
            durationTime);

      _isDuration_ReloadData = false;

      StatisticServices.setBarNames(statContext, _statisticData_Year.usedTourTypeIds, _barOrderStart);
      reorderStatisticData();

      // reset min/max values
      if (_isSynchScaleEnabled == false && statContext.isRefreshData) {
         _minMaxKeeper.resetMinMax();
      }

      final ChartDataModel chartDataModel = getChartDataModel();

      setChartProviders(chartDataModel);

      if (_isSynchScaleEnabled) {
         _minMaxKeeper.setMinMaxValues(chartDataModel);
      }

      // show selected time duration
      if (_yData_Duration != null) {
         setGraphLabel_Duration(_yData_Duration, durationTime);
      }

      StatisticServices.updateChartProperties(_chart, getGridPrefPrefix());

      // update title segment config AFTER defaults are set above
      final ChartTitleSegmentConfig ctsConfig = _chart.getChartTitleSegmentConfig();
      ctsConfig.isShowSegmentSeparator = _prefStore.getBoolean(ITourbookPreferences.STAT_YEAR_IS_SHOW_YEAR_SEPARATOR);

      // show the chart data model in the chart
      _chart.updateChart(chartDataModel, true);
   }

   @Override
   public void updateToolBar() {
      _chart.fillToolbar(true);
   }
}
