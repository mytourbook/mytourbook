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

import java.util.ArrayList;

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

   private static final String      STRING_SEPARATOR       = " - ";                        //$NON-NLS-1$

   private final IPreferenceStore   _prefStore             = TourbookPlugin.getPrefStore();

   private TourData_Year            _tourYear_Data;
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

   private long[][]                 _resortedTypeIds;

   private float[][]                _resortedAltitudeLow;
   private float[][]                _resortedAltitudeHigh;
   private float[][]                _resortedDistanceLow;
   private float[][]                _resortedDistanceHigh;
   private float[][]                _resortedNumToursLow;
   private float[][]                _resortedNumToursHigh;
   private float[][]                _resortedTimeLow;
   private float[][]                _resortedTimeHigh;

   public boolean canTourBeVisible() {
      return false;
   }

   ChartStatisticSegments createChartSegments(final TourData_Year tourDataYear) {

      final int yearCounter = tourDataYear.altitudeHigh[0].length;

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

   private ChartToolTipInfo createToolTipInfo(final int serieIndex, final int valueIndex) {

      final int oldestYear = _statFirstYear - _statNumberOfYears + 1;

      final int elapsedTime = _tourYear_Data.elapsedTime[serieIndex][valueIndex];
      final int recordedTime = _tourYear_Data.recordedTime[serieIndex][valueIndex];
      final int pausedTime = _tourYear_Data.pausedTime[serieIndex][valueIndex];
      final int movingTime = _tourYear_Data.movingTime[serieIndex][valueIndex];
      final int breakTime = elapsedTime - movingTime;

      /*
       * tool tip: title
       */
      final StringBuilder titleString = new StringBuilder();

      final String tourTypeName = StatisticServices.getTourTypeName(//
            serieIndex,
            valueIndex,
            _resortedTypeIds,
            _appTourTypeFilter);

      if (tourTypeName != null && tourTypeName.length() > 0) {
         titleString.append(tourTypeName);
         titleString.append(STRING_SEPARATOR);
      }

      titleString.append(Messages.tourtime_info_date_year);
      titleString.append(UI.NEW_LINE);

      final String toolTipTitle = String.format(titleString.toString(), oldestYear + valueIndex).toString();

      /*
       * tool tip: label
       */
      final StringBuilder toolTipFormat = new StringBuilder();
      toolTipFormat.append(Messages.tourtime_info_distance);
      toolTipFormat.append(UI.NEW_LINE);
      toolTipFormat.append(Messages.tourtime_info_altitude);
      toolTipFormat.append(UI.NEW_LINE);
      toolTipFormat.append(UI.NEW_LINE);
      toolTipFormat.append(Messages.tourtime_info_elapsed_time);
      toolTipFormat.append(UI.NEW_LINE);
      toolTipFormat.append(Messages.tourtime_info_recorded_time);
      toolTipFormat.append(UI.NEW_LINE);
      toolTipFormat.append(Messages.tourtime_info_paused_time);
      toolTipFormat.append(UI.NEW_LINE);
      toolTipFormat.append(Messages.tourtime_info_moving_time);
      toolTipFormat.append(UI.NEW_LINE);
      toolTipFormat.append(Messages.tourtime_info_break_time);
      toolTipFormat.append(UI.NEW_LINE);
      toolTipFormat.append(UI.NEW_LINE);
      toolTipFormat.append(Messages.TourTime_Info_NumberOfTours);

      final String toolTipLabel = String.format(
            toolTipFormat.toString(), //
            //
            //
            (int) _resortedDistanceHigh[serieIndex][valueIndex],
            UI.UNIT_LABEL_DISTANCE,
            //
            (int) _resortedAltitudeHigh[serieIndex][valueIndex],
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
            (int) _resortedNumToursHigh[serieIndex][valueIndex]
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

   void createXData_Year(final ChartDataModel chartDataModel) {

      // set the x-axis
      final ChartDataXSerie xData = new ChartDataXSerie(createYearData(_tourYear_Data));
      xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_YEAR);
      xData.setChartSegments(createChartSegments(_tourYear_Data));
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
            _resortedAltitudeLow,
            _resortedAltitudeHigh);

      yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
      yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setShowYSlider(true);

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE, _appTourTypeFilter);
      StatisticServices.setTourTypeColorIndex(yData, _resortedTypeIds, _appTourTypeFilter);

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
            _resortedDistanceLow,
            _resortedDistanceHigh);

      yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
      yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setShowYSlider(true);

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE, _appTourTypeFilter);
      StatisticServices.setTourTypeColorIndex(yData, _resortedTypeIds, _appTourTypeFilter);

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
            _resortedTimeLow,
            _resortedTimeHigh);

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
            _resortedNumToursLow,
            _resortedNumToursHigh);

      yData.setYTitle(Messages.LABEL_GRAPH_NUMBER_OF_TOURS);
      yData.setUnitLabel(Messages.NUMBERS_UNIT);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setShowYSlider(true);

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_TOUR);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TOUR, _appTourTypeFilter);
      StatisticServices.setTourTypeColorIndex(yData, _resortedTypeIds, _appTourTypeFilter);

      chartDataModel.addYData(yData);
   }

   private double[] createYearData(final TourData_Year tourDataYear) {

      final int yearCounter = tourDataYear.altitudeHigh[0].length;
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
    * Resort statistic bars according to the sequence start
    *
    * @param statContext
    */
   private void reorderStatData() {

      final int barLength = _tourYear_Data.altitudeHigh.length;

      _resortedTypeIds = new long[barLength][];

      _resortedAltitudeLow = new float[barLength][];
      _resortedAltitudeHigh = new float[barLength][];
      _resortedDistanceLow = new float[barLength][];
      _resortedDistanceHigh = new float[barLength][];
      _resortedNumToursLow = new float[barLength][];
      _resortedNumToursHigh = new float[barLength][];
      _resortedTimeLow = new float[barLength][];
      _resortedTimeHigh = new float[barLength][];

      if (_statContext.outBarNames == null) {

         // there are no data available, create dummy data that the UI do not fail

         _resortedTypeIds = new long[1][1];

         _resortedAltitudeLow = new float[1][1];
         _resortedAltitudeHigh = new float[1][1];
         _resortedDistanceLow = new float[1][1];
         _resortedDistanceHigh = new float[1][1];
         _resortedNumToursLow = new float[1][1];
         _resortedNumToursHigh = new float[1][1];
         _resortedTimeLow = new float[1][1];
         _resortedTimeHigh = new float[1][1];

         return;
      }

      int resortedIndex = 0;

      final long[][] typeIds = _tourYear_Data.typeIds;

      final float[][] altitudeLowValues = _tourYear_Data.altitudeLow;
      final float[][] altitudeHighValues = _tourYear_Data.altitudeHigh;
      final float[][] distanceLowValues = _tourYear_Data.distanceLow;
      final float[][] distanceHighValues = _tourYear_Data.distanceHigh;
      final float[][] numToursLowValues = _tourYear_Data.numToursLow;
      final float[][] numToursHighValues = _tourYear_Data.numToursHigh;
      final float[][] timeLowValues = _tourYear_Data.getDurationTimeLowFloat();
      final float[][] timeHighValues = _tourYear_Data.getDurationTimeHighFloat();

      if (_barOrderStart >= barLength) {

         final int barOrderStart = _barOrderStart % barLength;

         // set types starting from the sequence start
         for (int serieIndex = barOrderStart; serieIndex >= 0; serieIndex--) {

            _resortedTypeIds[resortedIndex] = typeIds[serieIndex];

            _resortedAltitudeLow[resortedIndex] = altitudeLowValues[serieIndex];
            _resortedAltitudeHigh[resortedIndex] = altitudeHighValues[serieIndex];
            _resortedDistanceLow[resortedIndex] = distanceLowValues[serieIndex];
            _resortedDistanceHigh[resortedIndex] = distanceHighValues[serieIndex];
            _resortedNumToursLow[resortedIndex] = numToursLowValues[serieIndex];
            _resortedNumToursHigh[resortedIndex] = numToursHighValues[serieIndex];
            _resortedTimeLow[resortedIndex] = timeLowValues[serieIndex];
            _resortedTimeHigh[resortedIndex] = timeHighValues[serieIndex];

            resortedIndex++;
         }

         // set types starting from the last
         for (int serieIndex = barLength - 1; resortedIndex < barLength; serieIndex--) {

            _resortedTypeIds[resortedIndex] = typeIds[serieIndex];

            _resortedAltitudeLow[resortedIndex] = altitudeLowValues[serieIndex];
            _resortedAltitudeHigh[resortedIndex] = altitudeHighValues[serieIndex];
            _resortedDistanceLow[resortedIndex] = distanceLowValues[serieIndex];
            _resortedDistanceHigh[resortedIndex] = distanceHighValues[serieIndex];
            _resortedNumToursLow[resortedIndex] = numToursLowValues[serieIndex];
            _resortedNumToursHigh[resortedIndex] = numToursHighValues[serieIndex];
            _resortedTimeLow[resortedIndex] = timeLowValues[serieIndex];
            _resortedTimeHigh[resortedIndex] = timeHighValues[serieIndex];

            resortedIndex++;
         }

      } else {

         final int barOrderStart = _barOrderStart;

         // set types starting from the sequence start
         for (int serieIndex = barOrderStart; serieIndex < barLength; serieIndex++) {

            _resortedTypeIds[resortedIndex] = typeIds[serieIndex];

            _resortedAltitudeLow[resortedIndex] = altitudeLowValues[serieIndex];
            _resortedAltitudeHigh[resortedIndex] = altitudeHighValues[serieIndex];
            _resortedDistanceLow[resortedIndex] = distanceLowValues[serieIndex];
            _resortedDistanceHigh[resortedIndex] = distanceHighValues[serieIndex];
            _resortedNumToursLow[resortedIndex] = numToursLowValues[serieIndex];
            _resortedNumToursHigh[resortedIndex] = numToursHighValues[serieIndex];
            _resortedTimeLow[resortedIndex] = timeLowValues[serieIndex];
            _resortedTimeHigh[resortedIndex] = timeHighValues[serieIndex];

            resortedIndex++;
         }

         // set types starting from 0
         for (int serieIndex = 0; resortedIndex < barLength; serieIndex++) {

            _resortedTypeIds[resortedIndex] = typeIds[serieIndex];

            _resortedAltitudeLow[resortedIndex] = altitudeLowValues[serieIndex];
            _resortedAltitudeHigh[resortedIndex] = altitudeHighValues[serieIndex];
            _resortedDistanceLow[resortedIndex] = distanceLowValues[serieIndex];
            _resortedDistanceHigh[resortedIndex] = distanceHighValues[serieIndex];
            _resortedNumToursLow[resortedIndex] = numToursLowValues[serieIndex];
            _resortedNumToursHigh[resortedIndex] = numToursHighValues[serieIndex];
            _resortedTimeLow[resortedIndex] = timeLowValues[serieIndex];
            _resortedTimeHigh[resortedIndex] = timeHighValues[serieIndex];

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

      _chartType = _prefStore.getString(ITourbookPreferences.STAT_YEAR_CHART_TYPE);

      final DurationTime durationTime = (DurationTime) Util.getEnumValue(
            _prefStore.getString(ITourbookPreferences.STAT_YEAR_DURATION_TIME),
            DurationTime.MOVING);

      _statContext = statContext;

      // this statistic supports bar reordering
      statContext.outIsBarReorderingSupported = true;

      _appPerson = statContext.appPerson;
      _appTourTypeFilter = statContext.appTourTypeFilter;
      _statFirstYear = statContext.statFirstYear;
      _statNumberOfYears = statContext.statNumberOfYears;

      _tourYear_Data = _tourYear_DataProvider.getYearData(
            statContext.appPerson,
            statContext.appTourTypeFilter,
            statContext.statFirstYear,
            statContext.statNumberOfYears,
            isDataDirtyWithReset() || statContext.isRefreshData || _isDuration_ReloadData,
            durationTime);

      _isDuration_ReloadData = false;

      StatisticServices.setBarNames(statContext, _tourYear_Data.usedTourTypeIds, _barOrderStart);
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
      ctsConfig.isShowSegmentSeparator = _prefStore.getBoolean(ITourbookPreferences.STAT_YEAR_IS_SHOW_YEAR_SEPARATOR);

      // show the chart data model in the chart
      _chart.updateChart(chartDataModel, true);
   }

   @Override
   public void updateToolBar() {
      _chart.fillToolbar(true);
   }
}
