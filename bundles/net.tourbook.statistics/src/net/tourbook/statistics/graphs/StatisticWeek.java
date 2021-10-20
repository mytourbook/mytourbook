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
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartStatisticSegments;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.chart.MinMaxKeeper_YData;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourPerson;
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

public abstract class StatisticWeek extends TourbookStatistic {

   private static final String      SUB_TITLE_1_LINE       = "%s … %s";                   //$NON-NLS-1$
   private static final String      SUB_TITLE_2_LINES      = "%s …\n%s";                  //$NON-NLS-1$

   private TourStatisticData_Week   _statisticData_Week;
   private DataProvider_Tour_Week   _tourWeek_DataProvider = new DataProvider_Tour_Week();

   private Chart                    _chart;
   private String                   _chartType;
   private final MinMaxKeeper_YData _minMaxKeeper          = new MinMaxKeeper_YData();

   private TourPerson               _appPerson;
   private TourTypeFilter           _appTourTypeFilter;

   private int                      _statFirstYear;
   private int                      _statNumberOfYears;

   private boolean                  _isSynchScaleEnabled;

   private ChartDataYSerie          _yData_Duration;

   private IChartInfoProvider       _chartInfoProvider;

   private ChartDataYSerie          _athleteBodyWeight_YData;
   private ChartDataYSerie          _athleteBodyFat_YData;

   public boolean canTourBeVisible() {
      return false;
   }

   /**
    * create segments for each week
    */
   ChartStatisticSegments createChartSegments() {

      final double[] segmentStart = new double[_statNumberOfYears];
      final double[] segmentEnd = new double[_statNumberOfYears];
      final String[] segmentTitle = new String[_statNumberOfYears];

      final int oldestYear = _statFirstYear - _statNumberOfYears + 1;
      final int[] yearWeeks = _statisticData_Week.yearWeeks;

      int weekCounter = 0;
      int yearIndex = 0;

      // get start/end and title for each segment
      for (final int weeks : yearWeeks) {

         segmentStart[yearIndex] = weekCounter;
         segmentEnd[yearIndex] = weekCounter + weeks - 1;

         segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

         weekCounter += weeks;
         yearIndex++;
      }

      final ChartStatisticSegments weekSegments = new ChartStatisticSegments();
      weekSegments.segmentStartValue = segmentStart;
      weekSegments.segmentEndValue = segmentEnd;
      weekSegments.segmentTitle = segmentTitle;

      weekSegments.years = _statisticData_Week.years;
      weekSegments.yearWeeks = yearWeeks;
      weekSegments.yearDays = _statisticData_Week.yearDays;

      return weekSegments;
   }

   @Override
   public void createStatisticUI(final Composite parent, final IViewSite viewSite) {

      // create statistic chart
      _chart = new Chart(parent, SWT.FLAT);
      _chart.setShowZoomActions(true);
      _chart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);

      _chartInfoProvider = StatisticWeek.this::createToolTipUI;
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
                                final int colorIndex,
                                final int weekIndex) {

      /*
       * Get calendar week from year/weekindex
       */
      final int oldestYear = _statFirstYear - _statNumberOfYears + 1;

      final WeekFields calendarWeek = TimeTools.calendarWeek;
      final TemporalField weekOfWeekBasedYear = calendarWeek.weekOfWeekBasedYear();
      final TemporalField weekBasedYear = calendarWeek.weekBasedYear();
      final TemporalField dayOfWeek = calendarWeek.dayOfWeek();

      // first day in the statistic calendar
      final LocalDate jan_1_1 = LocalDate.of(oldestYear, 1, 1);
      final int jan_1_1_DayOfWeek = jan_1_1.get(dayOfWeek) - 1;
      final int jan_1_1_WeekOfYear = jan_1_1.get(weekOfWeekBasedYear);

      LocalDate firstStatisticDay;
      if (jan_1_1_WeekOfYear > 33) {

         // the week from 1.1.January is from the last year -> this is not displayed
         firstStatisticDay = jan_1_1.plusDays(7 - jan_1_1_DayOfWeek);

      } else {

         firstStatisticDay = jan_1_1.minusDays(jan_1_1_DayOfWeek);
      }

      final LocalDate valueStatisticDay = firstStatisticDay.plusWeeks(weekIndex);

      final int weekOfYear = valueStatisticDay.get(weekOfWeekBasedYear);
      final int weekYear = valueStatisticDay.get(weekBasedYear);

      final String weekBeginDate = TimeTools.Formatter_Date_F.format(valueStatisticDay);
      final String weekEndDate = TimeTools.Formatter_Date_F.format(valueStatisticDay.plusDays(6));

      final boolean isShowPercentageValues = _prefStore.getBoolean(ITourbookPreferences.STAT_WEEK_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES);
      final boolean isShowSummaryValues = _prefStore.getBoolean(ITourbookPreferences.STAT_WEEK_TOOLTIP_IS_SHOW_SUMMARY_VALUES);

      /*
       * Create tooltip title
       */
      final String toolTip_Title = String.format(Messages.Statistic_Week_Tooltip_Title, weekOfYear, weekYear);
      final String totalColumnHeaderTitel = String.format(Messages.Statistic_Week_Tooltip_ColumnHeaderTitle, weekOfYear);

      String toolTip_SubTitle;
      if (isShowSummaryValues && isShowPercentageValues) {
         // show sub title with 1 line
         toolTip_SubTitle = String.format(SUB_TITLE_1_LINE, weekBeginDate, weekEndDate);
      } else {
         // show sub title with 2 lines
         toolTip_SubTitle = String.format(SUB_TITLE_2_LINES, weekBeginDate, weekEndDate);
      }

      new StatisticTooltipUI_CategorizedData().createContentArea(
            parent,
            toolTipProvider,
            _statisticData_Week,
            colorIndex,
            weekIndex,
            toolTip_Title,
            toolTip_SubTitle,
            totalColumnHeaderTitel,
            isShowSummaryValues,
            isShowPercentageValues);
   }

   private double[] createWeekData() {

      final int weekCounter = _statisticData_Week.elevationUp_High[0].length;
      final double[] allWeeks = new double[weekCounter];

      for (int weekIndex = 0; weekIndex < weekCounter; weekIndex++) {
         allWeeks[weekIndex] = weekIndex;
      }

      return allWeeks;
   }

   void createXData_Week(final ChartDataModel chartDataModel) {

      // set the x-axis
      final ChartDataXSerie xData = new ChartDataXSerie(createWeekData());
      xData.setAxisUnit(ChartDataSerie.X_AXIS_UNIT_WEEK);
      xData.setChartSegments(createChartSegments());

      chartDataModel.setXData(xData);
   }

   void createYData_Altitude(final ChartDataModel chartDataModel) {

      // altitude
      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            getChartType(_chartType),
            _statisticData_Week.elevationUp_Low,
            _statisticData_Week.elevationUp_High);

      yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
      yData.setUnitLabel(UI.UNIT_LABEL_ELEVATION);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setVisibleMinValue(0);
      yData.setShowYSlider(true);

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE);
      StatisticServices.setTourTypeColorIndex(yData, _statisticData_Week.typeIds, _appTourTypeFilter);

      chartDataModel.addYData(yData);
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

         visibleMinValue = _prefStore.getDouble(ITourbookPreferences.STAT_BODYFAT_YAXIS_MIN_VISIBLE_VALUE);
         visibleMaxValue = _prefStore.getDouble(ITourbookPreferences.STAT_BODYFAT_YAXIS_MAX_VISIBLE_VALUE);
      }

      _athleteBodyFat_YData = new ChartDataYSerie(
            ChartType.LINE,
            _statisticData_Week.athleteBodyFat_Low,
            _statisticData_Week.athleteBodyFat_High);

      _athleteBodyFat_YData.setYTitle(Messages.LABEL_GRAPH_BODY_FAT);
      _athleteBodyFat_YData.setUnitLabel(UI.UNIT_PERCENT);
      _athleteBodyFat_YData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      _athleteBodyFat_YData.setVisibleMinValue(visibleMinValue);
      _athleteBodyFat_YData.setVisibleMaxValue(visibleMaxValue);
      _athleteBodyFat_YData.setShowYSlider(true);

      StatisticServices.setTourTypeColors(_athleteBodyFat_YData, GraphColorManager.PREF_GRAPH_BODYFAT);
      StatisticServices.setTourTypeColorIndex(_athleteBodyFat_YData, _statisticData_Week.typeIds_Resorted, _appTourTypeFilter);

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
            _statisticData_Week.athleteBodyWeight_Low,
            _statisticData_Week.athleteBodyWeight_High);

      _athleteBodyWeight_YData.setYTitle(Messages.LABEL_GRAPH_BODY_WEIGHT);
      _athleteBodyWeight_YData.setUnitLabel(UI.UNIT_LABEL_WEIGHT);
      _athleteBodyWeight_YData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      _athleteBodyWeight_YData.setVisibleMinValue(visibleMinValue);
      _athleteBodyWeight_YData.setVisibleMaxValue(visibleMaxValue);
      _athleteBodyWeight_YData.setShowYSlider(true);

      StatisticServices.setTourTypeColors(_athleteBodyWeight_YData, GraphColorManager.PREF_GRAPH_BODYWEIGHT);
      StatisticServices.setTourTypeColorIndex(_athleteBodyWeight_YData, _statisticData_Week.typeIds_Resorted, _appTourTypeFilter);

      chartDataModel.addYData(_athleteBodyWeight_YData);
   }

   void createYData_Distance(final ChartDataModel chartDataModel) {

      // distance
      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            getChartType(_chartType),
            _statisticData_Week.distance_Low,
            _statisticData_Week.distance_High);

      yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
      yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setValueDivisor(1000);
      yData.setVisibleMinValue(0);
      yData.setShowYSlider(true);

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE);
      StatisticServices.setTourTypeColorIndex(yData, _statisticData_Week.typeIds, _appTourTypeFilter);

      chartDataModel.addYData(yData);
   }

   void createYData_Duration(final ChartDataModel chartDataModel) {

      // duration
      _yData_Duration = new ChartDataYSerie(
            ChartType.BAR,
            getChartType(_chartType),
            _statisticData_Week.durationTime_Low,
            _statisticData_Week.durationTime_High);

      _yData_Duration.setYTitle(Messages.LABEL_GRAPH_TIME);
      _yData_Duration.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
      _yData_Duration.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
      _yData_Duration.setAllValueColors(0);
      _yData_Duration.setVisibleMinValue(0);
      _yData_Duration.setShowYSlider(true);

      StatisticServices.setTourTypeColors(_yData_Duration, GraphColorManager.PREF_GRAPH_TIME);
      StatisticServices.setTourTypeColorIndex(_yData_Duration, _statisticData_Week.typeIds, _appTourTypeFilter);

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
            _statisticData_Week.numTours_Low,
            _statisticData_Week.numTours_High);

      yData.setYTitle(Messages.LABEL_GRAPH_NUMBER_OF_TOURS);
      yData.setUnitLabel(Messages.NUMBERS_UNIT);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setShowYSlider(true);

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TOUR);
      StatisticServices.setTourTypeColorIndex(yData, _statisticData_Week.typeIds, _appTourTypeFilter);

      chartDataModel.addYData(yData);
   }

   abstract ChartDataModel getChartDataModel();

   @Override
   public int getEnabledGridOptions() {

      return ChartOptions_Grid.GRID_VERTICAL_DISTANCE
            | ChartOptions_Grid.GRID_IS_SHOW_HORIZONTAL_LINE
            | ChartOptions_Grid.GRID_IS_SHOW_VERTICAL_LINE;
   }

   private void getPreferences() {

      StatisticServices.updateChartProperties(_chart, getGridPrefPrefix());
   }

   @Override
   public String getRawStatisticValues(final boolean isShowSequenceNumbers) {
      return _tourWeek_DataProvider.getRawStatisticValues(isShowSequenceNumbers);
   }

   @Override
   public void preferencesHasChanged() {

      updateStatistic(new StatisticContext(_appPerson, _appTourTypeFilter, _statFirstYear, _statNumberOfYears));
   }

   /**
    * Reorder statistic bars according to the sequence start.
    * <p>
    * <b>
    * Weeks can currently not be reordered, but the reordered values are needed for the tooltip ->
    * do dummy reordering
    * </b>
    *
    * @param statContext
    */
   private void reorderStatisticData() {

      _statisticData_Week.reorderStatisticData(0, true);
   }

   @Override
   public void saveState(final IDialogSettings state) {

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
   public void setSynchScale(final boolean isSynchScaleEnabled) {

      if (!isSynchScaleEnabled) {

         // reset when it's disabled

         _minMaxKeeper.resetMinMax();
      }

      _isSynchScaleEnabled = isSynchScaleEnabled;
   }

   @Override
   public void updateStatistic(final StatisticContext statContext) {

      _chartType = _prefStore.getString(ITourbookPreferences.STAT_WEEK_CHART_TYPE);

      final DurationTime durationTime = (DurationTime) Util.getEnumValue(
            _prefStore.getString(ITourbookPreferences.STAT_WEEK_DURATION_TIME),
            DurationTime.MOVING);

      _appPerson = statContext.appPerson;
      _appTourTypeFilter = statContext.appTourTypeFilter;
      _statFirstYear = statContext.statSelectedYear;
      _statNumberOfYears = statContext.statNumberOfYears;

      _statisticData_Week = _tourWeek_DataProvider.getWeekData(
            _appPerson,
            _appTourTypeFilter,
            _statFirstYear,
            _statNumberOfYears,
            isDataDirtyWithReset() || statContext.isRefreshData || _isDuration_ReloadData,
            durationTime);

      _isDuration_ReloadData = false;

      reorderStatisticData();

      // reset min/max values
      if (_isSynchScaleEnabled == false && statContext.isRefreshData) {
         _minMaxKeeper.resetMinMax();
      }

      final ChartDataModel chartDataModel = getChartDataModel();

      if (_isSynchScaleEnabled) {
         _minMaxKeeper.setMinMaxValues(chartDataModel);
      }

      // show selected time duration
      if (_yData_Duration != null) {
         setGraphLabel_Duration(_yData_Duration, durationTime);
      }

      // set tool tip info
      chartDataModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, _chartInfoProvider);

      getPreferences();

      _chart.updateChart(chartDataModel, true);
   }

   @Override
   public void updateToolBar() {
      _chart.fillToolbar(true);
   }
}
