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
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartStatisticSegments;
import net.tourbook.chart.ChartToolTipInfo;
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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IViewSite;

public abstract class StatisticWeek extends TourbookStatistic {

   private static final char        NL                     = UI.NEW_LINE;

   private TourData_Week            _tourWeek_Data;
   private DataProvider_Tour_Week   _tourWeek_DataProvider = new DataProvider_Tour_Week();

   private Chart                    _chart;
   private String                   _chartType;
   private final MinMaxKeeper_YData _minMaxKeeper          = new MinMaxKeeper_YData();

   private TourPerson               _appPerson;
   private TourTypeFilter           _appTourTypeFilter;

   private int                      _statYoungestYear;
   private int                      _statNumberOfYears;

   private boolean                  _isSynchScaleEnabled;

   private ChartDataYSerie          _yData_Duration;

   private IChartInfoProvider       _chartInfoProvider;

   public boolean canTourBeVisible() {
      return false;
   }

   /**
    * create segments for each week
    */
   ChartStatisticSegments createChartSegments() {

      final double segmentStart[] = new double[_statNumberOfYears];
      final double segmentEnd[] = new double[_statNumberOfYears];
      final String[] segmentTitle = new String[_statNumberOfYears];

      final int oldestYear = _statYoungestYear - _statNumberOfYears + 1;
      final int[] yearWeeks = _tourWeek_Data.yearWeeks;

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

      weekSegments.years = _tourWeek_Data.years;
      weekSegments.yearWeeks = yearWeeks;
      weekSegments.yearDays = _tourWeek_Data.yearDays;

      return weekSegments;
   }

   @Override
   public void createStatisticUI(final Composite parent, final IViewSite viewSite) {

      // create statistic chart
      _chart = new Chart(parent, SWT.FLAT);
      _chart.setShowZoomActions(true);
      _chart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);

      _chartInfoProvider = new IChartInfoProvider() {

         @Override
         public void createToolTipUI(final IToolTipProvider toolTipProvider, final Composite parent, final int serieIndex, final int valueIndex) {
            StatisticWeek.this.createToolTipUI(toolTipProvider, parent, serieIndex, valueIndex);
         }

         @Override
         public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
            return createToolTipInfo(serieIndex, valueIndex);
         }
      };
   }
   private ChartToolTipInfo createToolTipInfo(final int colorIndex, final int weekIndex) {

      final int oldestYear = _statYoungestYear - _statNumberOfYears + 1;

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

      final String beginDate = TimeTools.Formatter_Date_F.format(valueStatisticDay);
      final String endDate = TimeTools.Formatter_Date_F.format(valueStatisticDay.plusDays(6));

      final Integer elapsedTime = _tourWeek_Data.elapsedTime[colorIndex][weekIndex];
      final Integer recordedTime = _tourWeek_Data.recordedTime[colorIndex][weekIndex];
      final Integer pausedTime = _tourWeek_Data.pausedTime[colorIndex][weekIndex];
      final Integer movingTime = _tourWeek_Data.movingTime[colorIndex][weekIndex];
      final int breakTime = elapsedTime - movingTime;

      final String tourTypeName = StatisticServices.getTourTypeName(colorIndex, _appTourTypeFilter);

      /*
       * Tool tip: title
       */
      final StringBuilder sbTitleFormat = new StringBuilder();
      sbTitleFormat.append(Messages.tourtime_info_week);
      sbTitleFormat.append(NL);

      final String toolTipTitle = String.format(sbTitleFormat.toString(),
            tourTypeName,
            weekOfYear,
            weekYear

      ).toString();

      /*
       * Tool tip: label
       */
      final StringBuilder sbToolTipFormat = new StringBuilder();
      sbToolTipFormat.append(Messages.tourtime_info_date_week);
      sbToolTipFormat.append(NL);
      sbToolTipFormat.append(NL);
      sbToolTipFormat.append(Messages.tourtime_info_distance_tour);
      sbToolTipFormat.append(NL);
      sbToolTipFormat.append(Messages.tourtime_info_altitude);
      sbToolTipFormat.append(NL);
      sbToolTipFormat.append(NL);
      sbToolTipFormat.append(Messages.tourtime_info_elapsed_time);
      sbToolTipFormat.append(NL);
      sbToolTipFormat.append(Messages.tourtime_info_recorded_time);
      sbToolTipFormat.append(NL);
      sbToolTipFormat.append(Messages.tourtime_info_paused_time);
      sbToolTipFormat.append(NL);
      sbToolTipFormat.append(Messages.tourtime_info_moving_time);
      sbToolTipFormat.append(NL);
      sbToolTipFormat.append(Messages.tourtime_info_break_time);
      sbToolTipFormat.append(NL);
      sbToolTipFormat.append(NL);
      sbToolTipFormat.append(Messages.TourTime_Info_NumberOfTours);

      final String toolTipLabel = String.format(
            sbToolTipFormat.toString(), //
            //
            beginDate,
            endDate,
            //
            _tourWeek_Data.distanceHigh[colorIndex][weekIndex] / 1000,
            UI.UNIT_LABEL_DISTANCE,
            //
            (int) _tourWeek_Data.elevationUp_High[colorIndex][weekIndex],
            UI.UNIT_LABEL_ALTITUDE,
            //
            elapsedTime / 3600,
            (elapsedTime % 3600) / 60,
            //
            recordedTime / 3600,
            (recordedTime % 3600) / 60,
            //
            pausedTime / 3600,
            (pausedTime % 3600) / 60,
            //
            movingTime / 3600,
            (movingTime % 3600) / 60,
            //
            breakTime / 3600,
            (breakTime % 3600) / 60,
            //
            (int) _tourWeek_Data.numToursHigh[colorIndex][weekIndex]
      //
      ).toString();

      /*
       * create tool tip info
       */

      final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
      toolTipInfo.setTitle(toolTipTitle);
      toolTipInfo.setLabel(toolTipLabel);

      return toolTipInfo;
   }

   private void createToolTipUI(final IToolTipProvider toolTipProvider,
                                final Composite parent,
                                final int _hoveredBar_VerticalIndex,
                                final int _hoveredBar_HorizontalIndex) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         final Label label = new Label(container, SWT.NONE);
         GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
         label.setText("Bar Tooltip\n\n"
               + "serieIndex:" + _hoveredBar_VerticalIndex + "\n"
               + "valueIndex:" + _hoveredBar_HorizontalIndex + "\n");

      }
   }

   private double[] createWeekData() {

      final int weekCounter = _tourWeek_Data.elevationUp_High[0].length;
      final double allWeeks[] = new double[weekCounter];

      for (int weekIndex = 0; weekIndex < weekCounter; weekIndex++) {
         allWeeks[weekIndex] = weekIndex;
      }

//      debugWeekNumber();

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
            _tourWeek_Data.elevationUp_Low,
            _tourWeek_Data.elevationUp_High);

      yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
      yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setVisibleMinValue(0);
      yData.setShowYSlider(true);

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE, _appTourTypeFilter);
      StatisticServices.setTourTypeColorIndex(yData, _tourWeek_Data.typeIds, _appTourTypeFilter);
      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE);

      chartDataModel.addYData(yData);
   }

   void createYData_Distance(final ChartDataModel chartDataModel) {

      // distance
      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            getChartType(_chartType),
            _tourWeek_Data.distanceLow,
            _tourWeek_Data.distanceHigh);

      yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
      yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setValueDivisor(1000);
      yData.setVisibleMinValue(0);
      yData.setShowYSlider(true);

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE, _appTourTypeFilter);
      StatisticServices.setTourTypeColorIndex(yData, _tourWeek_Data.typeIds, _appTourTypeFilter);
      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE);

      chartDataModel.addYData(yData);
   }

   void createYData_Duration(final ChartDataModel chartDataModel) {

      // duration
      _yData_Duration = new ChartDataYSerie(
            ChartType.BAR,
            getChartType(_chartType),
            _tourWeek_Data.getDurationTimeLowFloat(),
            _tourWeek_Data.getDurationTimeHighFloat());

      _yData_Duration.setYTitle(Messages.LABEL_GRAPH_TIME);
      _yData_Duration.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
      _yData_Duration.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
      _yData_Duration.setAllValueColors(0);
      _yData_Duration.setVisibleMinValue(0);
      _yData_Duration.setShowYSlider(true);

      StatisticServices.setTourTypeColors(_yData_Duration, GraphColorManager.PREF_GRAPH_TIME, _appTourTypeFilter);
      StatisticServices.setTourTypeColorIndex(_yData_Duration, _tourWeek_Data.typeIds, _appTourTypeFilter);
      StatisticServices.setDefaultColors(_yData_Duration, GraphColorManager.PREF_GRAPH_TIME);

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
            _tourWeek_Data.numToursLow,
            _tourWeek_Data.numToursHigh);

      yData.setYTitle(Messages.LABEL_GRAPH_NUMBER_OF_TOURS);
      yData.setUnitLabel(Messages.NUMBERS_UNIT);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setShowYSlider(true);

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_TOUR);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TOUR, _appTourTypeFilter);
      StatisticServices.setTourTypeColorIndex(yData, _tourWeek_Data.typeIds, _appTourTypeFilter);

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

      updateStatistic(new StatisticContext(_appPerson, _appTourTypeFilter, _statYoungestYear, _statNumberOfYears));
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
      _statYoungestYear = statContext.statFirstYear;
      _statNumberOfYears = statContext.statNumberOfYears;

      _tourWeek_Data = _tourWeek_DataProvider.getWeekData(
            _appPerson,
            _appTourTypeFilter,
            _statYoungestYear,
            _statNumberOfYears,
            isDataDirtyWithReset() || statContext.isRefreshData || _isDuration_ReloadData,
            durationTime);

      _isDuration_ReloadData = false;

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
