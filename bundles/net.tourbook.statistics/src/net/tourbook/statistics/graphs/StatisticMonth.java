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
package net.tourbook.statistics.graphs;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Locale;

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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

public abstract class StatisticMonth extends TourbookStatistic {

   private static final String      TOOLTIP_TITLE_FORMAT         = "%s %d";                      //$NON-NLS-1$

   private TourStatisticData_Month  _statisticData_Month;
   private DataProvider_Tour_Month  _statisticMonth_DataProvider = new DataProvider_Tour_Month();

   private TourPerson               _appPerson;
   private TourTypeFilter           _appTourTypeFilter;

   private int                      _statSelectedYear;
   private int                      _statNumberOfYears;

   private Chart                    _chart;
   private String                   _chartType;
   private final MinMaxKeeper_YData _minMaxKeeper                = new MinMaxKeeper_YData();

   private boolean                  _isSynchScaleEnabled;

   private StatisticContext         _statContext;

   private ChartDataYSerie          _yData_DurationTime;

   private int                      _barOrderStart;

   private ChartDataYSerie          _athleteBodyWeight_YData;
   private ChartDataYSerie          _athleteBodyFat_YData;

   public boolean canTourBeVisible() {
      return false;
   }

   ChartStatisticSegments createChartSegments(final TourStatisticData_Month tourMonthData) {

      /*
       * create segments for each year
       */
      final int monthCounter = tourMonthData.elevationUp_High[0].length;
      final double[] segmentStart = new double[_statNumberOfYears];
      final double[] segmentEnd = new double[_statNumberOfYears];
      final String[] segmentTitle = new String[_statNumberOfYears];

      final int oldestYear = _statSelectedYear - _statNumberOfYears + 1;

      // get start/end and title for each segment
      for (int monthIndex = 0; monthIndex < monthCounter; monthIndex++) {

         final int yearIndex = monthIndex / 12;

         if (monthIndex % 12 == 0) {

            // first month in a year
            segmentStart[yearIndex] = monthIndex;
            segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

         } else if (monthIndex % 12 == 11) {

            // last month in a year
            segmentEnd[yearIndex] = monthIndex;
         }
      }

      final ChartStatisticSegments monthSegments = new ChartStatisticSegments();
      monthSegments.segmentStartValue = segmentStart;
      monthSegments.segmentEndValue = segmentEnd;
      monthSegments.segmentTitle = segmentTitle;

      return monthSegments;
   }

   private double[] createMonthData(final TourStatisticData_Month tourMonthData) {

      /*
       * create segments for each year
       */
      final int monthCounter = tourMonthData.elevationUp_High[0].length;
      final double[] allMonths = new double[monthCounter];

      // get start/end and title for each segment
      for (int monthIndex = 0; monthIndex < monthCounter; monthIndex++) {
         allMonths[monthIndex] = monthIndex;
      }

      return allMonths;
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
                                final int serieIndex,
                                final int valueIndex) {

      /*
       * Create tooltip title
       */
      final int firstYear = _statSelectedYear - _statNumberOfYears + 1;

      final LocalDate monthDate = LocalDate.of(firstYear, 1, 1).plusMonths(valueIndex);
      final String monthText = Month
            .of(monthDate.getMonthValue())
            .getDisplayName(TextStyle.FULL, Locale.getDefault());

      final String toolTip_Title = String.format(TOOLTIP_TITLE_FORMAT, monthText, monthDate.getYear());
      final String totalColumnHeaderTitel = monthText;

      final boolean isShowPercentageValues = _prefStore.getBoolean(ITourbookPreferences.STAT_MONTH_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES);
      final boolean isShowSummaryValues = _prefStore.getBoolean(ITourbookPreferences.STAT_MONTH_TOOLTIP_IS_SHOW_SUMMARY_VALUES);

      new StatisticTooltipUI_CategorizedData().createContentArea(
            parent,
            toolTipProvider,
            _statisticData_Month,
            serieIndex,
            valueIndex,
            toolTip_Title,
            null,
            totalColumnHeaderTitel,
            isShowSummaryValues,
            isShowPercentageValues);
   }

   void createXData_Months(final ChartDataModel chartDataModel) {

      // set the x-axis
      final ChartDataXSerie xData = new ChartDataXSerie(createMonthData(_statisticData_Month));
      xData.setAxisUnit(ChartDataSerie.X_AXIS_UNIT_MONTH);
      xData.setChartSegments(createChartSegments(_statisticData_Month));

      chartDataModel.setXData(xData);
   }

   /**
    * Athlete's body fat
    *
    * @param chartDataModel
    */
   void createYData_AthleteBodyFat(final ChartDataModel chartDataModel) {

      double visibleMinValue = 0;
      double visibleMaxValue = 0;

      // If the user has switched from a statistic to another, we need to retrieve
      // the last min/max values and not the ones from the preference store that can
      // be in this case outdated.
      if (_athleteBodyFat_YData != null) {

         visibleMinValue = _athleteBodyFat_YData.getVisibleMinValue();
         visibleMaxValue = _athleteBodyFat_YData.getVisibleMaxValue();
      } else {

         visibleMinValue = _prefStore.getDouble(ITourbookPreferences.STAT_BODYFAT_YAXIS_MIN_VISIBLE_VALUE) * UI.UNIT_VALUE_WEIGHT;
         visibleMaxValue = _prefStore.getDouble(ITourbookPreferences.STAT_BODYFAT_YAXIS_MAX_VISIBLE_VALUE);
      }

      _athleteBodyFat_YData = new ChartDataYSerie(
            ChartType.LINE,
            _statisticData_Month.athleteBodyFat_Low,
            _statisticData_Month.athleteBodyFat_High);

      _athleteBodyFat_YData.setYTitle(Messages.LABEL_GRAPH_BODY_FAT);
      _athleteBodyFat_YData.setUnitLabel(UI.UNIT_PERCENT);
      _athleteBodyFat_YData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      _athleteBodyFat_YData.setVisibleMinValue(visibleMinValue);
      _athleteBodyFat_YData.setVisibleMaxValue(visibleMaxValue);
      _athleteBodyFat_YData.setShowYSlider(true);

      StatisticServices.setTourTypeColors(_athleteBodyFat_YData, GraphColorManager.PREF_GRAPH_BODYFAT);
      StatisticServices.setTourTypeColorIndex(_athleteBodyFat_YData, _statisticData_Month.typeIds_Resorted, _appTourTypeFilter);

      chartDataModel.addYData(_athleteBodyFat_YData);
   }

   /**
    * Athlete's body weight
    *
    * @param chartDataModel
    */
   void createYData_AthleteBodyWeight(final ChartDataModel chartDataModel) {

      double visibleMinValue = 0;
      double visibleMaxValue = 0;

      // If the user has switched from a statistic to another, we need to retrieve
      // the last min/max values and not the ones from the preference store that can
      // be in this case outdated.
      if (_athleteBodyWeight_YData != null) {

         visibleMinValue = _athleteBodyWeight_YData.getVisibleMinValue();
         visibleMaxValue = _athleteBodyWeight_YData.getVisibleMaxValue();
      } else {

         visibleMinValue = _prefStore.getDouble(ITourbookPreferences.STAT_BODYWEIGHT_YAXIS_MIN_VISIBLE_VALUE) * UI.UNIT_VALUE_WEIGHT;
         visibleMaxValue = _prefStore.getDouble(ITourbookPreferences.STAT_BODYWEIGHT_YAXIS_MAX_VISIBLE_VALUE) * UI.UNIT_VALUE_WEIGHT;
      }

      _athleteBodyWeight_YData = new ChartDataYSerie(
            ChartType.LINE,
            _statisticData_Month.athleteBodyWeight_Low,
            _statisticData_Month.athleteBodyWeight_High);

      _athleteBodyWeight_YData.setYTitle(Messages.LABEL_GRAPH_BODY_WEIGHT);
      _athleteBodyWeight_YData.setUnitLabel(UI.UNIT_LABEL_WEIGHT);
      _athleteBodyWeight_YData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      _athleteBodyWeight_YData.setVisibleMinValue(visibleMinValue);
      _athleteBodyWeight_YData.setVisibleMaxValue(visibleMaxValue);
      _athleteBodyWeight_YData.setShowYSlider(true);

      StatisticServices.setTourTypeColors(_athleteBodyWeight_YData, GraphColorManager.PREF_GRAPH_BODYWEIGHT);
      StatisticServices.setTourTypeColorIndex(_athleteBodyWeight_YData, _statisticData_Month.typeIds_Resorted, _appTourTypeFilter);

      chartDataModel.addYData(_athleteBodyWeight_YData);
   }

   void createYData_Distance(final ChartDataModel chartDataModel) {

      // distance

      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            getChartType(_chartType),
            _statisticData_Month.distance_Low_Resorted,
            _statisticData_Month.distance_High_Resorted);

      yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
      yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setValueDivisor(1000);
      yData.setShowYSlider(true);

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE);
      StatisticServices.setTourTypeColorIndex(yData, _statisticData_Month.typeIds_Resorted, _appTourTypeFilter);

      chartDataModel.addYData(yData);
   }

   void createYData_Duration(final ChartDataModel chartDataModel) {

      // duration time

      _yData_DurationTime = new ChartDataYSerie(
            ChartType.BAR,
            getChartType(_chartType),
            _statisticData_Month.durationTime_Low_Resorted,
            _statisticData_Month.durationTime_High_Resorted);

      _yData_DurationTime.setYTitle(Messages.LABEL_GRAPH_TIME);
      _yData_DurationTime.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
      _yData_DurationTime.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
      _yData_DurationTime.setShowYSlider(true);

      StatisticServices.setTourTypeColors(_yData_DurationTime, GraphColorManager.PREF_GRAPH_TIME);
      StatisticServices.setTourTypeColorIndex(_yData_DurationTime, _statisticData_Month.typeIds_Resorted, _appTourTypeFilter);

      chartDataModel.addYData(_yData_DurationTime);
   }

   void createYData_Elevation(final ChartDataModel chartDataModel) {

      // elevation

      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            getChartType(_chartType),
            _statisticData_Month.elevationUp_Low_Resorted,
            _statisticData_Month.elevationUp_High_Resorted);

      yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
      yData.setUnitLabel(UI.UNIT_LABEL_ELEVATION);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setShowYSlider(true);

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE);
      StatisticServices.setTourTypeColorIndex(yData, _statisticData_Month.typeIds_Resorted, _appTourTypeFilter);

      chartDataModel.addYData(yData);
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
            _statisticData_Month.numTours_Low_Resorted,
            _statisticData_Month.numTours_High_Resorted);

      yData.setYTitle(Messages.LABEL_GRAPH_NUMBER_OF_TOURS);
      yData.setUnitLabel(Messages.NUMBERS_UNIT);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setShowYSlider(true);

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TOUR);
      StatisticServices.setTourTypeColorIndex(yData, _statisticData_Month.typeIds_Resorted, _appTourTypeFilter);

      chartDataModel.addYData(yData);
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
      return _statisticMonth_DataProvider.getRawStatisticValues(isShowSequenceNumbers);
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

      _statisticData_Month.reorderStatisticData(_barOrderStart, _statContext.outBarNames != null);
   }

   @Override
   public void restoreStateEarly(final IDialogSettings state) {

      _barOrderStart = Util.getStateInt(state, getBarOrderingStateKey(), 0);

   }

   @Override
   public void saveState(final IDialogSettings state) {

      state.put(getBarOrderingStateKey(), _barOrderStart);

      if (_athleteBodyWeight_YData != null) {

         _prefStore.setValue(ITourbookPreferences.STAT_BODYWEIGHT_YAXIS_MIN_VISIBLE_VALUE,
               _athleteBodyWeight_YData.getVisibleMinValue() / UI.UNIT_VALUE_WEIGHT);
         _prefStore.setValue(ITourbookPreferences.STAT_BODYWEIGHT_YAXIS_MAX_VISIBLE_VALUE,
               _athleteBodyWeight_YData.getVisibleMaxValue() / UI.UNIT_VALUE_WEIGHT);
      }

      if (_athleteBodyFat_YData != null) {

         _prefStore.setValue(ITourbookPreferences.STAT_BODYFAT_YAXIS_MIN_VISIBLE_VALUE, _athleteBodyFat_YData.getVisibleMinValue());
         _prefStore.setValue(ITourbookPreferences.STAT_BODYFAT_YAXIS_MAX_VISIBLE_VALUE, _athleteBodyFat_YData.getVisibleMaxValue());
      }
   }

   @Override
   public void setBarVerticalOrder(final int selectedIndex) {

      // selected index can be -1 when tour type combobox is empty
      _barOrderStart = selectedIndex < 0 ? 0 : selectedIndex;

      final ArrayList<TourType> tourTypes = TourDatabase.getActiveTourTypes();

      if (tourTypes == null || tourTypes.isEmpty()) {
         return;
      }

      reorderStatisticData();

      updateStatistic();
   }

   private void setChartProviders(final ChartDataModel chartModel) {

      // set tool tip info
      chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER,
            (IChartInfoProvider) StatisticMonth.this::createToolTipUI);
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

      updateStatistic(new StatisticContext(_appPerson, _appTourTypeFilter, _statSelectedYear, _statNumberOfYears));
   }

   @Override
   public void updateStatistic(final StatisticContext statContext) {

      _chartType = _prefStore.getString(ITourbookPreferences.STAT_MONTH_CHART_TYPE);

      final DurationTime durationTime = (DurationTime) Util.getEnumValue(
            _prefStore.getString(ITourbookPreferences.STAT_MONTH_DURATION_TIME),
            DurationTime.MOVING);

      _statContext = statContext;

      // this statistic supports bar reordering
      statContext.outIsBarReorderingSupported = true;

      _appPerson = statContext.appPerson;
      _appTourTypeFilter = statContext.appTourTypeFilter;
      _statSelectedYear = statContext.statSelectedYear;
      _statNumberOfYears = statContext.statNumberOfYears;

      _statisticData_Month = _statisticMonth_DataProvider.getMonthData(
            _appPerson,
            _appTourTypeFilter,
            _statSelectedYear,
            _statNumberOfYears,
            isDataDirtyWithReset() || statContext.isRefreshData || _isDuration_ReloadData,
            durationTime);

      _isDuration_ReloadData = false;

      StatisticServices.setBarNames(statContext, _statisticData_Month.usedTourTypeIds, _barOrderStart);
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
      if (_yData_DurationTime != null) {
         setGraphLabel_Duration(_yData_DurationTime, durationTime);
      }

      StatisticServices.updateChartProperties(_chart, getGridPrefPrefix());

      // update title segment config AFTER defaults are set above
      final ChartTitleSegmentConfig ctsConfig = _chart.getChartTitleSegmentConfig();
      ctsConfig.isShowSegmentSeparator = _prefStore.getBoolean(ITourbookPreferences.STAT_MONTH_IS_SHOW_YEAR_SEPARATOR);

      // show the fDataModel in the chart
      _chart.updateChart(chartDataModel, true);
   }

   @Override
   public void updateToolBar() {
      _chart.fillToolbar(true);
   }
}
