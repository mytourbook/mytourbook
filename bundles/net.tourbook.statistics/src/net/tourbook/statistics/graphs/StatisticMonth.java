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
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Locale;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartStatisticSegments;
import net.tourbook.chart.ChartTitleSegmentConfig;
import net.tourbook.chart.ChartToolTipInfo;
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

public abstract class StatisticMonth extends TourbookStatistic {

   private static final char        NL                      = UI.NEW_LINE;

   private final IPreferenceStore   _prefStore              = TourbookPlugin.getPrefStore();

   private TourData_Month           _tourMonth_Data;
   private DataProvider_Tour_Month  _tourMonth_DataProvider = new DataProvider_Tour_Month();

   private TourPerson               _appPerson;
   private TourTypeFilter           _appTourTypeFilter;

   private int                      _statFirstYear;
   private int                      _statNumberOfYears;

   private Chart                    _chart;
   private String                   _chartType;
   private final MinMaxKeeper_YData _minMaxKeeper           = new MinMaxKeeper_YData();

   private boolean                  _isSynchScaleEnabled;

   private StatisticContext         _statContext;

   private ChartDataYSerie          _yData_Duration;

   private int                      _barOrderStart;

   private long[][]                 _resortedTypeIds;

   private float[][]                _resortedElevation_Low;
   private float[][]                _resortedElevation_High;
   private float[][]                _resortedDistance_Low;
   private float[][]                _resortedDistance_High;
   private float[][]                _resortedNumTours_Low;
   private float[][]                _resortedNumTours_High;
   private float[][]                _resortedTime_Low;
   private float[][]                _resortedTime_High;

   public boolean canTourBeVisible() {
      return false;
   }

   ChartStatisticSegments createChartSegments(final TourData_Month tourMonthData) {

      /*
       * create segments for each year
       */
      final int monthCounter = tourMonthData.elevationUp_High[0].length;
      final double segmentStart[] = new double[_statNumberOfYears];
      final double segmentEnd[] = new double[_statNumberOfYears];
      final String[] segmentTitle = new String[_statNumberOfYears];

      final int oldestYear = _statFirstYear - _statNumberOfYears + 1;

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

   private double[] createMonthData(final TourData_Month tourMonthData) {

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

   private ChartToolTipInfo createToolTipInfo(final int serieIndex, final int valueIndex) {

      final int oldestYear = _statFirstYear - _statNumberOfYears + 1;

      final LocalDate monthDate = LocalDate.of(oldestYear, 1, 1).plusMonths(valueIndex);

      final String monthText = Month
            .of(monthDate.getMonthValue())
            .getDisplayName(TextStyle.FULL, Locale.getDefault());

      final Integer elapsedTime = _tourMonth_Data.elapsedTime[serieIndex][valueIndex];
      final Integer recordedTime = _tourMonth_Data.recordedTime[serieIndex][valueIndex];
      final Integer pausedTime = _tourMonth_Data.pausedTime[serieIndex][valueIndex];
      final Integer movingTime = _tourMonth_Data.movingTime[serieIndex][valueIndex];
      final int breakTime = elapsedTime - movingTime;

      /*
       * Tool tip: title
       */
      final StringBuilder sbTitle = new StringBuilder();

      final String tourTypeName = StatisticServices.getTourTypeName(serieIndex, valueIndex, _resortedTypeIds, _appTourTypeFilter);

      if (tourTypeName != null && tourTypeName.length() > 0) {
         sbTitle.append(tourTypeName);
      }

      final String toolTipTitle = String.format(Messages.tourtime_info_date_month,
            sbTitle.toString(),
            monthText,
            monthDate.getYear());

      /*
       * Tool tip: label
       */
      final String toolTipFormat = UI.EMPTY_STRING

            + Messages.tourtime_info_distance_tour + NL
            + Messages.tourtime_info_altitude + NL
            + NL
            + Messages.tourtime_info_elapsed_time + NL
            + Messages.tourtime_info_recorded_time + NL
            + Messages.tourtime_info_paused_time + NL
            + Messages.tourtime_info_moving_time + NL
            + Messages.tourtime_info_break_time + NL
            + NL
            + Messages.TourTime_Info_NumberOfTours;

      final String toolTipLabel = String.format(toolTipFormat,

            _resortedDistance_High[serieIndex][valueIndex] / 1000,
            UI.UNIT_LABEL_DISTANCE,

            (int) _resortedElevation_High[serieIndex][valueIndex],
            UI.UNIT_LABEL_ALTITUDE,

            elapsedTime / 3600,
            (elapsedTime % 3600) / 60,

            recordedTime / 3600,
            (recordedTime % 3600) / 60,

            pausedTime / 3600,
            (pausedTime % 3600) / 60,

            movingTime / 3600,
            (movingTime % 3600) / 60,

            breakTime / 3600,
            (breakTime % 3600) / 60,

            (int) _resortedNumTours_High[serieIndex][valueIndex]

      ).toString();

      /*
       * create tool tip info
       */

      final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
      toolTipInfo.setTitle(toolTipTitle);
      toolTipInfo.setLabel(toolTipLabel);

      return toolTipInfo;
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

      final LocalDate monthDate = LocalDate.of(oldestYear, 1, 1).plusMonths(hoveredBar_ValueIndex);
      final String monthText = Month
            .of(monthDate.getMonthValue())
            .getDisplayName(TextStyle.FULL, Locale.getDefault());

      final long typeId = _resortedTypeIds[hoveredBar_SerieIndex][hoveredBar_ValueIndex];

      final String tourTypeName = StatisticServices.getTourTypeName(
            hoveredBar_SerieIndex,
            hoveredBar_ValueIndex,
            _resortedTypeIds,
            _appTourTypeFilter);

      final StringBuilder sbTitle = new StringBuilder();

      if (tourTypeName != null && tourTypeName.length() > 0) {
         sbTitle.append(tourTypeName);
      }

      final String toolTipTitle = String.format(TOOLTIP_TITLE_FORMAT,
            sbTitle,
            monthText,
            monthDate.getYear());

      final StatisticTooltipUI_Summary tooltipUI = new StatisticTooltipUI_Summary();

      tooltipUI.createContentArea(
            parent,
            toolTipProvider,
            _tourMonth_Data,
            toolTipTitle,
            hoveredBar_SerieIndex,
            hoveredBar_ValueIndex);
   }

   void createXData_Months(final ChartDataModel chartDataModel) {

      // set the x-axis
      final ChartDataXSerie xData = new ChartDataXSerie(createMonthData(_tourMonth_Data));
      xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_MONTH);
      xData.setChartSegments(createChartSegments(_tourMonth_Data));

      chartDataModel.setXData(xData);
   }

   void createYData_Altitude(final ChartDataModel chartDataModel) {

      // altitude

      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            getChartType(_chartType),
            _resortedElevation_Low,
            _resortedElevation_High);

      yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
      yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setShowYSlider(true);

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE, _appTourTypeFilter);
      StatisticServices.setTourTypeColorIndex(yData, _resortedTypeIds, _appTourTypeFilter);

      chartDataModel.addYData(yData);
   }

   void createYData_Distance(final ChartDataModel chartDataModel) {

      // distance

      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            getChartType(_chartType),
            _resortedDistance_Low,
            _resortedDistance_High);

      yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
      yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setValueDivisor(1000);
      yData.setShowYSlider(true);

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE, _appTourTypeFilter);
      StatisticServices.setTourTypeColorIndex(yData, _resortedTypeIds, _appTourTypeFilter);

      chartDataModel.addYData(yData);
   }

   void createYData_Duration(final ChartDataModel chartDataModel) {

      // duration

      _yData_Duration = new ChartDataYSerie(
            ChartType.BAR,
            getChartType(_chartType),
            _resortedTime_Low,
            _resortedTime_High);

      _yData_Duration.setYTitle(Messages.LABEL_GRAPH_TIME);
      _yData_Duration.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
      _yData_Duration.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
      _yData_Duration.setShowYSlider(true);

      StatisticServices.setDefaultColors(_yData_Duration, GraphColorManager.PREF_GRAPH_TIME);
      StatisticServices.setTourTypeColors(_yData_Duration, GraphColorManager.PREF_GRAPH_TIME, _appTourTypeFilter);
      StatisticServices.setTourTypeColorIndex(_yData_Duration, _resortedTypeIds, _appTourTypeFilter);

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
            _resortedNumTours_Low,
            _resortedNumTours_High);

      yData.setYTitle(Messages.LABEL_GRAPH_NUMBER_OF_TOURS);
      yData.setUnitLabel(Messages.NUMBERS_UNIT);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setShowYSlider(true);

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_TOUR);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TOUR, _appTourTypeFilter);
      StatisticServices.setTourTypeColorIndex(yData, _resortedTypeIds, _appTourTypeFilter);

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
      return _tourMonth_DataProvider.getRawStatisticValues(isShowSequenceNumbers);
   }

   @Override
   public void preferencesHasChanged() {

      updateStatistic();
   }

   /**
    * Resort statistic bars according to the sequence start
    *
    * @param statContext
    */
   private void reorderStatData() {

      final int barLength = _tourMonth_Data.elevationUp_High.length;

      _resortedTypeIds = new long[barLength][];

      _resortedElevation_Low = new float[barLength][];
      _resortedElevation_High = new float[barLength][];
      _resortedDistance_Low = new float[barLength][];
      _resortedDistance_High = new float[barLength][];
      _resortedNumTours_Low = new float[barLength][];
      _resortedNumTours_High = new float[barLength][];
      _resortedTime_Low = new float[barLength][];
      _resortedTime_High = new float[barLength][];

      if (_statContext.outBarNames == null) {

         // there are no data available, create dummy data that the UI do not fail

         _resortedTypeIds = new long[1][1];

         _resortedElevation_Low = new float[1][1];
         _resortedElevation_High = new float[1][1];
         _resortedDistance_Low = new float[1][1];
         _resortedDistance_High = new float[1][1];
         _resortedNumTours_Low = new float[1][1];
         _resortedNumTours_High = new float[1][1];
         _resortedTime_Low = new float[1][1];
         _resortedTime_High = new float[1][1];

         return;
      }

      int resortedIndex = 0;

      final long[][] typeIds = _tourMonth_Data.typeIds;

      final float[][] altitudeLowValues = _tourMonth_Data.elevationUp_Low;
      final float[][] altitudeHighValues = _tourMonth_Data.elevationUp_High;
      final float[][] distanceLowValues = _tourMonth_Data.distanceLow;
      final float[][] distanceHighValues = _tourMonth_Data.distanceHigh;
      final float[][] numToursLowValues = _tourMonth_Data.numToursLow;
      final float[][] numToursHighValues = _tourMonth_Data.numToursHigh;
      final float[][] timeLowValues = _tourMonth_Data.getDurationTimeLowFloat();
      final float[][] timeHighValues = _tourMonth_Data.getDurationTimeHighFloat();

      if (_barOrderStart >= barLength) {

         final int barOrderStart = _barOrderStart % barLength;

         // set types starting from the sequence start
         for (int serieIndex = barOrderStart; serieIndex >= 0; serieIndex--) {

            _resortedTypeIds[resortedIndex] = typeIds[serieIndex];

            _resortedElevation_Low[resortedIndex] = altitudeLowValues[serieIndex];
            _resortedElevation_High[resortedIndex] = altitudeHighValues[serieIndex];
            _resortedDistance_Low[resortedIndex] = distanceLowValues[serieIndex];
            _resortedDistance_High[resortedIndex] = distanceHighValues[serieIndex];
            _resortedNumTours_Low[resortedIndex] = numToursLowValues[serieIndex];
            _resortedNumTours_High[resortedIndex] = numToursHighValues[serieIndex];
            _resortedTime_Low[resortedIndex] = timeLowValues[serieIndex];
            _resortedTime_High[resortedIndex] = timeHighValues[serieIndex];

            resortedIndex++;
         }

         // set types starting from the last
         for (int serieIndex = barLength - 1; resortedIndex < barLength; serieIndex--) {

            _resortedTypeIds[resortedIndex] = typeIds[serieIndex];

            _resortedElevation_Low[resortedIndex] = altitudeLowValues[serieIndex];
            _resortedElevation_High[resortedIndex] = altitudeHighValues[serieIndex];
            _resortedDistance_Low[resortedIndex] = distanceLowValues[serieIndex];
            _resortedDistance_High[resortedIndex] = distanceHighValues[serieIndex];
            _resortedNumTours_Low[resortedIndex] = numToursLowValues[serieIndex];
            _resortedNumTours_High[resortedIndex] = numToursHighValues[serieIndex];
            _resortedTime_Low[resortedIndex] = timeLowValues[serieIndex];
            _resortedTime_High[resortedIndex] = timeHighValues[serieIndex];

            resortedIndex++;
         }

      } else {

         final int barOrderStart = _barOrderStart;

         // set types starting from the sequence start
         for (int serieIndex = barOrderStart; serieIndex < barLength; serieIndex++) {

            _resortedTypeIds[resortedIndex] = typeIds[serieIndex];

            _resortedElevation_Low[resortedIndex] = altitudeLowValues[serieIndex];
            _resortedElevation_High[resortedIndex] = altitudeHighValues[serieIndex];
            _resortedDistance_Low[resortedIndex] = distanceLowValues[serieIndex];
            _resortedDistance_High[resortedIndex] = distanceHighValues[serieIndex];
            _resortedNumTours_Low[resortedIndex] = numToursLowValues[serieIndex];
            _resortedNumTours_High[resortedIndex] = numToursHighValues[serieIndex];
            _resortedTime_Low[resortedIndex] = timeLowValues[serieIndex];
            _resortedTime_High[resortedIndex] = timeHighValues[serieIndex];

            resortedIndex++;
         }

         // set types starting from 0
         for (int serieIndex = 0; resortedIndex < barLength; serieIndex++) {

            _resortedTypeIds[resortedIndex] = typeIds[serieIndex];

            _resortedElevation_Low[resortedIndex] = altitudeLowValues[serieIndex];
            _resortedElevation_High[resortedIndex] = altitudeHighValues[serieIndex];
            _resortedDistance_Low[resortedIndex] = distanceLowValues[serieIndex];
            _resortedDistance_High[resortedIndex] = distanceHighValues[serieIndex];
            _resortedNumTours_Low[resortedIndex] = numToursLowValues[serieIndex];
            _resortedNumTours_High[resortedIndex] = numToursHighValues[serieIndex];
            _resortedTime_Low[resortedIndex] = timeLowValues[serieIndex];
            _resortedTime_High[resortedIndex] = timeHighValues[serieIndex];

            resortedIndex++;
         }
      }
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

      reorderStatData();

      updateStatistic();
   }

   private void setChartProviders(final ChartDataModel chartModel) {

      // set tool tip info
      chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {

         @Override
         public void createToolTipUI(final IToolTipProvider toolTipProvider,
                                     final Composite parent,
                                     final int hoveredBar_Serie_VerticalIndex,
                                     final int hoveredBar_Value_HorizontalIndex) {

            StatisticMonth.this.createToolTipUI(toolTipProvider, parent, hoveredBar_Serie_VerticalIndex, hoveredBar_Value_HorizontalIndex);
         }

         @Override
         public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
            return createToolTipInfo(serieIndex, valueIndex);
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

      _chartType = _prefStore.getString(ITourbookPreferences.STAT_MONTH_CHART_TYPE);

      final DurationTime durationTime = (DurationTime) Util.getEnumValue(
            _prefStore.getString(ITourbookPreferences.STAT_MONTH_DURATION_TIME),
            DurationTime.MOVING);

      _statContext = statContext;

      // this statistic supports bar reordering
      statContext.outIsBarReorderingSupported = true;

      _appPerson = statContext.appPerson;
      _appTourTypeFilter = statContext.appTourTypeFilter;
      _statFirstYear = statContext.statFirstYear;
      _statNumberOfYears = statContext.statNumberOfYears;

      _tourMonth_Data = _tourMonth_DataProvider.getMonthData(
            _appPerson,
            _appTourTypeFilter,
            _statFirstYear,
            _statNumberOfYears,
            isDataDirtyWithReset() || statContext.isRefreshData || _isDuration_ReloadData,
            durationTime);

      _isDuration_ReloadData = false;

      StatisticServices.setBarNames(statContext, _tourMonth_Data.usedTourTypeIds, _barOrderStart);
      reorderStatData();

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
      ctsConfig.isShowSegmentSeparator = _prefStore.getBoolean(//
            ITourbookPreferences.STAT_MONTH_IS_SHOW_YEAR_SEPARATOR);

      // show the fDataModel in the chart
      _chart.updateChart(chartDataModel, true);
   }

   @Override
   public void updateToolBar() {
      _chart.fillToolbar(true);
   }
}
