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
import net.tourbook.chart.ChartToolTipInfo;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.chart.MinMaxKeeper_YData;
import net.tourbook.chart.Util;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.ChartOptions_TourFrequency;
import net.tourbook.statistic.DurationTime;
import net.tourbook.statistic.SlideoutStatisticOptions;
import net.tourbook.statistic.StatisticContext;
import net.tourbook.statistic.TourbookStatistic;
import net.tourbook.statistics.Messages;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewSite;

public class StatisticTour_Frequency extends TourbookStatistic {

   private static final char           NL                                  = UI.NEW_LINE;

   private final IPreferenceStore      _prefStore                          = TourbookPlugin.getPrefStore();

   private TourStatisticData_Day       _tourDay_Data;
   private DataProvider_Tour_Day       _tourDay_DataProvider               = new DataProvider_Tour_Day();

   private IPropertyChangeListener     _statTourFrequency_PrefChangeListener;

   private int                         _stat_SelectedYear;
   private int                         _stat_NumberOfYears;
   private TourPerson                  _stat_ActivePerson;
   private TourTypeFilter              _stat_ActiveTourTypeFilter;

   private boolean                     _isSynchScaleEnabled;

   private final MinMaxKeeper_YData    _minMaxKeeperStat_Elevation_Counter = new MinMaxKeeper_YData();
   private final MinMaxKeeper_YData    _minMaxKeeperStat_Elevation_Sum     = new MinMaxKeeper_YData();
   private final MinMaxKeeper_YData    _minMaxKeeperStat_Distance_Counter  = new MinMaxKeeper_YData();
   private final MinMaxKeeper_YData    _minMaxKeeperStat_Distance_Sum      = new MinMaxKeeper_YData();
   private final MinMaxKeeper_YData    _minMaxKeeperStat_Duration_Counter  = new MinMaxKeeper_YData();
   private final MinMaxKeeper_YData    _minMaxKeeperStat_Duration_Sum      = new MinMaxKeeper_YData();

   private TourStatisticData_Frequency _statisticData_Frequency            = new TourStatisticData_Frequency();

   private int[]                       _statDistance_GroupValues;
   private int[]                       _statDutationTime_GroupValues;
   private int[]                       _statElevation_GroupValues;

   private int[][]                     _statDistance_NumTours_Low;
   private int[][]                     _statDistance_NumTours_High;
   private int[][]                     _statDistance_NumTours_ColorIndex;
   private int[][]                     _statDistance_Sum_Low;
   private int[][]                     _statDistance_Sum_High;
   private int[][]                     _statDistance_Sum_ColorIndex;

   private int[][]                     _statDurationTime_NumTours_Low;
   private int[][]                     _statDurationTime_NumTours_High;
   private int[][]                     _statDurationTime_NumTours_ColorIndex;
   private int[][]                     _statDurationTime_Sum_Low;
   private int[][]                     _statDurationTime_Sum_High;
   private int[][]                     _statDurationTime_Sum_ColorIndex;

   private int[][]                     _statElevation_NumTours_Low;
   private int[][]                     _statElevation_NumTours_High;
   private int[][]                     _statElevation_NumTours_ColorIndex;
   private int[][]                     _statElevation_Sum_Low;
   private int[][]                     _statElevation_Sum_High;
   private int[][]                     _statElevation_Sum_ColorIndex;

   /*
    * UI controls
    */
   private Composite _statisticPage;

   private Chart     _chartDistance_NumTours;
   private Chart     _chartDistance_Values;

   private Chart     _chartDuration_NumTours;
   private Chart     _chartDuration_Values;

   private Chart     _chartElevation_NumTours;
   private Chart     _chartElevation_Values;

   public StatisticTour_Frequency() {}

   private void addPrefListener(final Composite container) {

      // create pref listener
      _statTourFrequency_PrefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {
            final String property = event.getProperty();

            // test if the color or statistic data have changed
            if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)
                  || property.equals(ITourbookPreferences.STAT_ALTITUDE_NUMBERS)
                  || property.equals(ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE)
                  || property.equals(ITourbookPreferences.STAT_ALTITUDE_INTERVAL)
                  || property.equals(ITourbookPreferences.STAT_DISTANCE_NUMBERS)
                  || property.equals(ITourbookPreferences.STAT_DISTANCE_LOW_VALUE)
                  || property.equals(ITourbookPreferences.STAT_DISTANCE_INTERVAL)
                  || property.equals(ITourbookPreferences.STAT_DURATION_NUMBERS)
                  || property.equals(ITourbookPreferences.STAT_DURATION_LOW_VALUE)
                  || property.equals(ITourbookPreferences.STAT_DURATION_INTERVAL)) {

               // get the changed preferences
               createGroupValues();

               /*
                * reset min/max keeper because they can be changed when the pref has changed
                */
               resetMinMaxKeeper(true);

               // update chart
               preferencesHasChanged();
            }
         }
      };

      // add pref listener
      _prefStore.addPropertyChangeListener(_statTourFrequency_PrefChangeListener);

      // remove pref listener
      container.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            _prefStore.removePropertyChangeListener(_statTourFrequency_PrefChangeListener);
         }
      });
   }

   public boolean canTourBeVisible() {
      return false;
   }

   private void createGroupValues() {

      _statisticData_Frequency.statDistance_GroupValues = _statDistance_GroupValues = createGroupValues(_prefStore,
            ITourbookPreferences.STAT_DISTANCE_NUMBERS,
            ITourbookPreferences.STAT_DISTANCE_LOW_VALUE,
            ITourbookPreferences.STAT_DISTANCE_INTERVAL,
            ChartDataSerie.AXIS_UNIT_NUMBER);

      _statisticData_Frequency.statDurationTime_GroupValues = _statDutationTime_GroupValues = createGroupValues(_prefStore,
            ITourbookPreferences.STAT_DURATION_NUMBERS,
            ITourbookPreferences.STAT_DURATION_LOW_VALUE,
            ITourbookPreferences.STAT_DURATION_INTERVAL,
            ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);

      _statisticData_Frequency.statElevation_GroupValues = _statElevation_GroupValues = createGroupValues(_prefStore,
            ITourbookPreferences.STAT_ALTITUDE_NUMBERS,
            ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE,
            ITourbookPreferences.STAT_ALTITUDE_INTERVAL,
            ChartDataSerie.AXIS_UNIT_NUMBER);
   }

   /**
    * create the units from the preference configuration
    *
    * @param store
    * @param prefInterval
    * @param prefLowValue
    * @param prefNumberOfBars
    * @param unitType
    * @return
    */
   private int[] createGroupValues(final IPreferenceStore store,
                                   final String prefNumberOfBars,
                                   final String prefLowValue,
                                   final String prefInterval,
                                   final int unitType) {

      final int lowValue = store.getInt(prefLowValue);
      final int interval = store.getInt(prefInterval);
      final int numberOfBars = store.getInt(prefNumberOfBars);

      final int[] allGroupValues = new int[numberOfBars];

      for (int number = 0; number < numberOfBars; number++) {

         if (unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE) {

            // adjust the values to minutes
            allGroupValues[number] = (lowValue * 60) + (interval * number * 60);

         } else {

            allGroupValues[number] = lowValue + (interval * number);
         }
      }

      return allGroupValues;
   }

   /**
    * calculate data for all statistics
    *
    * @param statData_Day
    */
   private void createStatisticData(final TourStatisticData_Day statData_Day) {

      int colorOffset = 0;
      if (_stat_ActiveTourTypeFilter.showUndefinedTourTypes()) {
         colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
      }

      final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
      final int numColors = colorOffset + tourTypeList.size();

      final TourStatisticData_Frequency statData = _statisticData_Frequency;

// SET_FORMATTING_OFF

      final int numDistanceGroups                    = _statDistance_GroupValues.length;
      final int numDurationTimeGroups                = _statDutationTime_GroupValues.length;
      final int numElevationGroups                   = _statElevation_GroupValues.length;

      statData.statDistance_NumTours_Low             = _statDistance_NumTours_Low            = new int[numColors][numDistanceGroups];
      statData.statDistance_NumTours_High            = _statDistance_NumTours_High           = new int[numColors][numDistanceGroups];
      statData.statDistance_NumTours_ColorIndex      = _statDistance_NumTours_ColorIndex     = new int[numColors][numDistanceGroups];
      statData.statDistance_SumValues_Low            = _statDistance_Sum_Low                 = new int[numColors][numDistanceGroups];
      statData.statDistance_SumValues_High           = _statDistance_Sum_High                = new int[numColors][numDistanceGroups];
      statData.statDistance_SumValues_ColorIndex     = _statDistance_Sum_ColorIndex          = new int[numColors][numDistanceGroups];

      statData.statDurationTime_NumTours_Low         = _statDurationTime_NumTours_Low        = new int[numColors][numDurationTimeGroups];
      statData.statDurationTime_NumTours_High        = _statDurationTime_NumTours_High       = new int[numColors][numDurationTimeGroups];
      statData.statDurationTime_NumTours_ColorIndex  = _statDurationTime_NumTours_ColorIndex = new int[numColors][numDurationTimeGroups];
      statData.statDurationTime_SumValues_Low        = _statDurationTime_Sum_Low             = new int[numColors][numDurationTimeGroups];
      statData.statDurationTime_SumValues_High       = _statDurationTime_Sum_High            = new int[numColors][numDurationTimeGroups];
      statData.statDurationTime_SumValues_ColorIndex = _statDurationTime_Sum_ColorIndex      = new int[numColors][numDurationTimeGroups];

      statData.statElevation_NumTours_Low            = _statElevation_NumTours_Low           = new int[numColors][numElevationGroups];
      statData.statElevation_NumTours_High           = _statElevation_NumTours_High          = new int[numColors][numElevationGroups];
      statData.statElevation_NumTours_ColorIndex     = _statElevation_NumTours_ColorIndex    = new int[numColors][numElevationGroups];
      statData.statElevation_SumValues_Low           = _statElevation_Sum_Low                = new int[numColors][numElevationGroups];
      statData.statElevation_SumValues_High          = _statElevation_Sum_High               = new int[numColors][numElevationGroups];
      statData.statElevation_SumValues_ColorIndex    = _statElevation_Sum_ColorIndex         = new int[numColors][numElevationGroups];

// SET_FORMATTING_ON

      // loop: all tours
      for (int tourIndex = 0; tourIndex < statData_Day.allDistance_High.length; tourIndex++) {

         int unitIndex;
         final int typeColorIndex = statData_Day.allTypeColorIndices[tourIndex];

         final int diffDistance = (int) ((statData_Day.allDistance_High[tourIndex] - statData_Day.allDistance_Low[tourIndex] + 500) / 1000);
         final int diffElevation = (int) (statData_Day.allElevation_High[tourIndex] - statData_Day.allElevation_Low[tourIndex]);
         final int diffDurationTime = (int) (statData_Day.getDurationHighFloat()[tourIndex] - statData_Day.getDurationLowFloat()[tourIndex]);

         unitIndex = createTourStatData(
               diffDistance,
               _statDistance_GroupValues,
               _statDistance_NumTours_High[typeColorIndex],
               _statDistance_Sum_High[typeColorIndex]);

         _statDistance_NumTours_ColorIndex[typeColorIndex][unitIndex] = typeColorIndex;
         _statDistance_Sum_ColorIndex[typeColorIndex][unitIndex] = typeColorIndex;

         unitIndex = createTourStatData(
               diffElevation,
               _statElevation_GroupValues,
               _statElevation_NumTours_High[typeColorIndex],
               _statElevation_Sum_High[typeColorIndex]);

         _statElevation_NumTours_ColorIndex[typeColorIndex][unitIndex] = typeColorIndex;
         _statElevation_Sum_ColorIndex[typeColorIndex][unitIndex] = typeColorIndex;

         unitIndex = createTourStatData(
               diffDurationTime,
               _statDutationTime_GroupValues,
               _statDurationTime_NumTours_High[typeColorIndex],
               _statDurationTime_Sum_High[typeColorIndex]);

         _statDurationTime_NumTours_ColorIndex[typeColorIndex][unitIndex] = typeColorIndex;
         _statDurationTime_Sum_ColorIndex[typeColorIndex][unitIndex] = typeColorIndex;
      }
   }

   @Override
   public void createStatisticUI(final Composite parent, final IViewSite viewSite) {

      // create statistic page
      _statisticPage = new Composite(parent, SWT.FLAT);

      // remove colored border
      _statisticPage.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
      _statisticPage.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));

      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .spacing(0, 0)
            .applyTo(_statisticPage);
      {
         _chartDistance_NumTours = new Chart(_statisticPage, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartDistance_NumTours);
         _chartDistance_Values = new Chart(_statisticPage, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartDistance_Values);

         _chartElevation_NumTours = new Chart(_statisticPage, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartElevation_NumTours);
         _chartElevation_Values = new Chart(_statisticPage, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartElevation_Values);

         _chartDuration_NumTours = new Chart(_statisticPage, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartDuration_NumTours);
         _chartDuration_Values = new Chart(_statisticPage, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartDuration_Values);
      }

      _chartDistance_NumTours.setToolBarManager(viewSite.getActionBars().getToolBarManager(), true);

      addPrefListener(parent);
      createGroupValues();
   }

   /**
    * create tool tip info
    */
   private ChartToolTipInfo createToolTipProvider(final int serieIndex, final String toolTipLabel) {

      final String tourTypeName = StatisticServices.getTourTypeName(serieIndex, _stat_ActiveTourTypeFilter);

      final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
      toolTipInfo.setTitle(tourTypeName);
      toolTipInfo.setLabel(toolTipLabel);

      return toolTipInfo;
   }

   private void createToolTipProvider_Distance(final ChartDataModel chartModel) {

      chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {

         @Override
         public void createToolTipUI(final IToolTipProvider toolTipProvider, final Composite parent, final int serieIndex, final int valueIndex) {

            StatisticTour_Frequency.this.createToolTipUI(
                  toolTipProvider,
                  parent,
                  serieIndex,
                  valueIndex,
                  FrequencyStatistic.DISTANCE);
         }

         @Override
         public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {

            String toolTipLabel;
            final StringBuilder sb = new StringBuilder();

            final int distance = _statDistance_Sum_High[serieIndex][valueIndex];
            final int counter = _statDistance_NumTours_High[serieIndex][valueIndex];

            if (valueIndex == 0) {

               sb.append(Messages.numbers_info_distance_down);
               sb.append(NL);
               sb.append(Messages.numbers_info_distance_total);

               toolTipLabel = String.format(
                     sb.toString(),
                     _statDistance_GroupValues[valueIndex],
                     UI.UNIT_LABEL_DISTANCE,
                     counter,
                     distance,
                     UI.UNIT_LABEL_DISTANCE).toString();

            } else if (valueIndex == _statDistance_GroupValues.length - 1) {

               sb.append(Messages.numbers_info_distance_up);
               sb.append(NL);
               sb.append(Messages.numbers_info_distance_total);

               toolTipLabel = String.format(
                     sb.toString(),
                     _statDistance_GroupValues[valueIndex - 1],
                     UI.UNIT_LABEL_DISTANCE,
                     counter,
                     distance,
                     UI.UNIT_LABEL_DISTANCE).toString();
            } else {

               sb.append(Messages.numbers_info_distance_between);
               sb.append(NL);
               sb.append(Messages.numbers_info_distance_total);

               toolTipLabel = String.format(
                     sb.toString(),
                     _statDistance_GroupValues[valueIndex - 1],
                     _statDistance_GroupValues[valueIndex],
                     UI.UNIT_LABEL_DISTANCE,
                     counter,
                     distance,
                     UI.UNIT_LABEL_DISTANCE).toString();
            }

            return createToolTipProvider(serieIndex, toolTipLabel);

         }

      });
   }

   private void createToolTipProvider_Elevation(final ChartDataModel chartModel) {

      chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {

         @Override
         public void createToolTipUI(final IToolTipProvider toolTipProvider, final Composite parent, final int serieIndex, final int valueIndex) {

            StatisticTour_Frequency.this.createToolTipUI(toolTipProvider,
                  parent,
                  serieIndex,
                  valueIndex,
                  FrequencyStatistic.ELEVATION);
         }

         @Override
         public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {

            String toolTipLabel;
            final StringBuilder infoText = new StringBuilder();

            final int summary = _statElevation_Sum_High[serieIndex][valueIndex];
            final int numTours = _statElevation_NumTours_High[serieIndex][valueIndex];
            final String unit = UI.UNIT_LABEL_ALTITUDE;

            if (valueIndex == 0) {

               infoText.append(Messages.numbers_info_altitude_down);
               infoText.append(NL);
               infoText.append(Messages.numbers_info_altitude_total);

               toolTipLabel = String.format(
                     infoText.toString(),
                     _statElevation_GroupValues[valueIndex],
                     unit,
                     numTours,
                     //
                     summary,
                     unit).toString();

            } else if (valueIndex == _statElevation_GroupValues.length - 1) {

               infoText.append(Messages.numbers_info_altitude_up);
               infoText.append(NL);
               infoText.append(Messages.numbers_info_altitude_total);

               toolTipLabel = String.format(
                     infoText.toString(),
                     _statElevation_GroupValues[valueIndex - 1],
                     unit,
                     numTours,
                     //
                     summary,
                     unit).toString();
            } else {

               infoText.append(Messages.numbers_info_altitude_between);
               infoText.append(NL);
               infoText.append(Messages.numbers_info_altitude_total);

               toolTipLabel = String.format(
                     infoText.toString(),
                     _statElevation_GroupValues[valueIndex - 1],
                     _statElevation_GroupValues[valueIndex],
                     unit,
                     numTours,
                     //
                     summary,
                     unit).toString();
            }

            return createToolTipProvider(serieIndex, toolTipLabel);
         }
      });
   }

   private void createToolTipProvider_TimeDuration(final ChartDataModel chartModel) {

      chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {

         @Override
         public void createToolTipUI(final IToolTipProvider toolTipProvider, final Composite parent, final int serieIndex, final int valueIndex) {

            StatisticTour_Frequency.this.createToolTipUI(
                  toolTipProvider,
                  parent,
                  serieIndex,
                  valueIndex,
                  FrequencyStatistic.DURATION_TIME);
         }

         @Override
         public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {

            String toolTipLabel;
            final StringBuilder toolTipFormat = new StringBuilder();

            if (valueIndex == 0) {

               // first bar

               toolTipFormat.append(Messages.numbers_info_time_down);
               toolTipFormat.append(NL);
               toolTipFormat.append(Messages.numbers_info_time_total);

               toolTipLabel = String.format(toolTipFormat.toString(),

                     Util.formatValue(_statDutationTime_GroupValues[valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),

                     _statDurationTime_NumTours_High[serieIndex][valueIndex],
                     Util.formatValue(_statDurationTime_Sum_High[serieIndex][valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE)).toString();

            } else if (valueIndex == _statDutationTime_GroupValues.length - 1) {

               // last bar

               toolTipFormat.append(Messages.numbers_info_time_up);
               toolTipFormat.append(NL);
               toolTipFormat.append(Messages.numbers_info_time_total);

               toolTipLabel = String.format(toolTipFormat.toString(),

                     Util.formatValue(_statDutationTime_GroupValues[valueIndex - 1], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
                     _statDurationTime_NumTours_High[serieIndex][valueIndex],
                     Util.formatValue(_statDurationTime_Sum_High[serieIndex][valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE)).toString();
            } else {

               // between bar

               toolTipFormat.append(Messages.numbers_info_time_between);
               toolTipFormat.append(NL);
               toolTipFormat.append(Messages.numbers_info_time_total);

               toolTipLabel = String.format(toolTipFormat.toString(),

                     Util.formatValue(_statDutationTime_GroupValues[valueIndex - 1], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
                     Util.formatValue(_statDutationTime_GroupValues[valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
                     _statDurationTime_NumTours_High[serieIndex][valueIndex],
                     Util.formatValue(_statDurationTime_Sum_High[serieIndex][valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE)).toString();
            }

            return createToolTipProvider(serieIndex, toolTipLabel);
         }
      });
   }

   /**
    * @param toolTipProvider
    * @param parent
    * @param serieIndex
    * @param valueIndex
    * @param frequencyStatistic
    */
   private void createToolTipUI(final IToolTipProvider toolTipProvider,
                                final Composite parent,
                                final int serieIndex,
                                final int valueIndex,
                                final FrequencyStatistic frequencyStatistic) {

      final long tourTypeId = StatisticServices.getTourTypeId(serieIndex, _stat_ActiveTourTypeFilter);

      // create sub title
      final int firstYear = _stat_SelectedYear - _stat_NumberOfYears + 1;
      String toolTip_SubTitle = null;
      if (_stat_NumberOfYears > 1) {
         toolTip_SubTitle = String.format("%d … %d", firstYear, _stat_SelectedYear);
      }

      final boolean isShowPercentageValues = _prefStore.getBoolean(ITourbookPreferences.STAT_FREQUENCY_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES);
      final boolean isShowSummaryValues = _prefStore.getBoolean(ITourbookPreferences.STAT_FREQUENCY_TOOLTIP_IS_SHOW_SUMMARY_VALUES);

      new StatisticTooltipUI_TourFrequency().createContentArea(parent,

            toolTipProvider,
            _statisticData_Frequency,
            frequencyStatistic,

            serieIndex,
            valueIndex,

            tourTypeId,

            toolTip_SubTitle,

            isShowSummaryValues,
            isShowPercentageValues);
   }

   /**
    * calculate the statistic for one tour
    *
    * @param tourValue
    * @param allGroupedValues
    * @param counter
    * @param sum
    * @return
    */
   private int createTourStatData(final int tourValue, final int[] allGroupedValues, final int[] counter, final int[] sum) {

      int lastGroup = -1;
      boolean isGroupFound = false;

      // loop: all units
      for (int groupIndex = 0; groupIndex < allGroupedValues.length; groupIndex++) {

         final int groupValue = allGroupedValues[groupIndex];

         if (lastGroup < 0) {

            // first group
            if (tourValue < groupValue) {
               isGroupFound = true;
            }

         } else {

            // second and continuous group
            if (tourValue >= lastGroup && tourValue < groupValue) {
               isGroupFound = true;
            }
         }

         if (isGroupFound) {

            counter[groupIndex]++;
            sum[groupIndex] += tourValue;

            return groupIndex;

         } else {

            lastGroup = groupValue;
         }
      }

      // if the value was not found, add it to the last group
      counter[allGroupedValues.length - 1]++;
      sum[allGroupedValues.length - 1] += tourValue;

      return allGroupedValues.length - 1;
   }

   @Override
   protected String getGridPrefPrefix() {
      return GRID_TOUR_FREQUENCY;
   }

   @Override
   public String getRawStatisticValues(final boolean isShowSequenceNumbers) {
      return _tourDay_DataProvider.getRawStatisticValues(isShowSequenceNumbers);
   }

   @Override
   public void preferencesHasChanged() {

      updateStatistic(new StatisticContext(_stat_ActivePerson, _stat_ActiveTourTypeFilter, _stat_SelectedYear, _stat_NumberOfYears));
   }

   private void resetMinMaxKeeper(final boolean isForceReset) {

      if (isForceReset || _isSynchScaleEnabled == false) {

         _minMaxKeeperStat_Elevation_Counter.resetMinMax();
         _minMaxKeeperStat_Elevation_Sum.resetMinMax();
         _minMaxKeeperStat_Distance_Counter.resetMinMax();
         _minMaxKeeperStat_Distance_Sum.resetMinMax();
         _minMaxKeeperStat_Duration_Counter.resetMinMax();
         _minMaxKeeperStat_Duration_Sum.resetMinMax();
      }
   }

   @Override
   public void setSynchScale(final boolean isSynchScaleEnabled) {

      if (!isSynchScaleEnabled) {
         resetMinMaxKeeper(true);
      }

      _isSynchScaleEnabled = isSynchScaleEnabled;
   }

   @Override
   protected void setupStatisticSlideout(final SlideoutStatisticOptions slideout) {

      slideout.setStatisticOptions(new ChartOptions_TourFrequency());
   }

   /**
    * @param statDistanceChart
    * @param statDistanceMinMaxKeeper
    * @param highValues
    * @param lowValues
    * @param statDistanceColorIndex
    * @param unit
    * @param title
    */
   private void updateChartDistance(final Chart statDistanceChart,
                                    final MinMaxKeeper_YData statDistanceMinMaxKeeper,
                                    final int[][] lowValues,
                                    final int[][] highValues,
                                    final int[][] colorIndex,
                                    final String unit,
                                    final String title) {

      final ChartDataModel chartDataModel = new ChartDataModel(ChartType.BAR);

      // set the x-axis
      final ChartDataXSerie xData = new ChartDataXSerie(net.tourbook.common.util.Util.convertIntToDouble(_statDistance_GroupValues));
      xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_NUMBER);
//      xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_NUMBER_CENTER);

      xData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
      chartDataModel.setXData(xData);

      // y-axis: distance
      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            ChartDataYSerie.BAR_LAYOUT_STACKED,
            net.tourbook.common.util.Util.convertIntToFloat(lowValues),
            net.tourbook.common.util.Util.convertIntToFloat(highValues));
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setUnitLabel(unit);
      yData.setAllValueColors(0);
      yData.setYTitle(title);
      yData.setVisibleMinValue(0);
      yData.setValueDivisor(1);
      yData.setShowYSlider(true);

      chartDataModel.addYData(yData);

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE, _stat_ActiveTourTypeFilter);
      yData.setColorIndex(colorIndex);

      createToolTipProvider_Distance(chartDataModel);

      if (_isSynchScaleEnabled) {
         statDistanceMinMaxKeeper.setMinMaxValues(chartDataModel);
      }

      StatisticServices.updateChartProperties(statDistanceChart, getGridPrefPrefix());

      // show the new data fDataModel in the chart
      statDistanceChart.updateChart(chartDataModel, true);
   }

   private void updateChartElevation(final Chart chart,
                                     final MinMaxKeeper_YData statElevationMinMaxKeeper,
                                     final int[][] lowValues,
                                     final int[][] highValues,
                                     final int[][] colorIndex,
                                     final String unit,
                                     final String title) {

      final ChartDataModel chartDataModel = new ChartDataModel(ChartType.BAR);

      // set the x-axis
      final ChartDataXSerie xData = new ChartDataXSerie(net.tourbook.common.util.Util.convertIntToDouble(_statElevation_GroupValues));
      xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_NUMBER);
//      xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_NUMBER_CENTER);

      xData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
      chartDataModel.setXData(xData);

      // y-axis: elevation
      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            ChartDataYSerie.BAR_LAYOUT_STACKED,
            net.tourbook.common.util.Util.convertIntToFloat(lowValues),
            net.tourbook.common.util.Util.convertIntToFloat(highValues));
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setUnitLabel(unit);
      yData.setAllValueColors(0);
      yData.setYTitle(title);
      yData.setVisibleMinValue(0);
      yData.setShowYSlider(true);

      chartDataModel.addYData(yData);

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE, _stat_ActiveTourTypeFilter);
      yData.setColorIndex(colorIndex);

      createToolTipProvider_Elevation(chartDataModel);

      if (_isSynchScaleEnabled) {
         statElevationMinMaxKeeper.setMinMaxValues(chartDataModel);
      }

      StatisticServices.updateChartProperties(chart, getGridPrefPrefix());

      // show the new data in the chart
      chart.updateChart(chartDataModel, true);
   }

   private void updateChartTime(final Chart statDurationChart,
                                final MinMaxKeeper_YData statDurationMinMaxKeeper,
                                final int[][] lowValues,
                                final int[][] highValues,
                                final int[][] colorIndex,
                                final int yUnit,
                                final String unit,
                                final String title) {

      final ChartDataModel chartDataModel = new ChartDataModel(ChartType.BAR);

      // set the x-axis
      final ChartDataXSerie xData = new ChartDataXSerie(net.tourbook.common.util.Util.convertIntToDouble(_statDutationTime_GroupValues));
      xData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
      xData.setUnitLabel(UI.UNIT_LABEL_TIME);
      chartDataModel.setXData(xData);

      // y-axis: elevation
      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            ChartDataYSerie.BAR_LAYOUT_STACKED,
            net.tourbook.common.util.Util.convertIntToFloat(lowValues),
            net.tourbook.common.util.Util.convertIntToFloat(highValues));
      yData.setAxisUnit(yUnit);
      yData.setUnitLabel(unit);
      yData.setAllValueColors(0);
      yData.setYTitle(title);
      yData.setVisibleMinValue(0);
      yData.setShowYSlider(true);

      chartDataModel.addYData(yData);

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_TIME);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TIME, _stat_ActiveTourTypeFilter);
      yData.setColorIndex(colorIndex);

      createToolTipProvider_TimeDuration(chartDataModel);

      if (_isSynchScaleEnabled) {
         statDurationMinMaxKeeper.setMinMaxValues(chartDataModel);
      }

      StatisticServices.updateChartProperties(statDurationChart, getGridPrefPrefix());

      // show the new data data model in the chart
      statDurationChart.updateChart(chartDataModel, true);
   }

   @Override
   public void updateStatistic(final StatisticContext statContext) {

      _stat_ActivePerson = statContext.appPerson;
      _stat_ActiveTourTypeFilter = statContext.appTourTypeFilter;
      _stat_SelectedYear = statContext.statSelectedYear;
      _stat_NumberOfYears = statContext.statNumberOfYears;

      // load statistic data from db
      _tourDay_Data = _tourDay_DataProvider.getDayData(

            statContext.appPerson,
            statContext.appTourTypeFilter,
            statContext.statSelectedYear,
            statContext.statNumberOfYears,

            isDataDirtyWithReset() || statContext.isRefreshData,

            // this may need to be customized as in the other statistics
            DurationTime.MOVING);

      // reset min/max values
      if (_isSynchScaleEnabled == false && statContext.isRefreshData) {
         resetMinMaxKeeper(false);
      }

      createStatisticData(_tourDay_Data);

      updateChartDistance(
            _chartDistance_NumTours,
            _minMaxKeeperStat_Distance_Counter,
            _statDistance_NumTours_Low,
            _statDistance_NumTours_High,
            _statDistance_NumTours_ColorIndex,
            Messages.NUMBERS_UNIT,
            Messages.LABEL_GRAPH_DISTANCE);

      updateChartDistance(
            _chartDistance_Values,
            _minMaxKeeperStat_Distance_Sum,
            _statDistance_Sum_Low,
            _statDistance_Sum_High,
            _statDistance_Sum_ColorIndex,
            UI.UNIT_LABEL_DISTANCE,
            Messages.LABEL_GRAPH_DISTANCE);

      updateChartElevation(
            _chartElevation_NumTours,
            _minMaxKeeperStat_Elevation_Counter,
            _statElevation_NumTours_Low,
            _statElevation_NumTours_High,
            _statElevation_NumTours_ColorIndex,
            Messages.NUMBERS_UNIT,
            Messages.LABEL_GRAPH_ALTITUDE);

      updateChartElevation(
            _chartElevation_Values,
            _minMaxKeeperStat_Elevation_Sum,
            _statElevation_Sum_Low,
            _statElevation_Sum_High,
            _statElevation_Sum_ColorIndex,
            UI.UNIT_LABEL_ALTITUDE,
            Messages.LABEL_GRAPH_ALTITUDE);

      updateChartTime(
            _chartDuration_NumTours,
            _minMaxKeeperStat_Duration_Counter,
            _statDurationTime_NumTours_Low,
            _statDurationTime_NumTours_High,
            _statDurationTime_NumTours_ColorIndex,
            ChartDataXSerie.AXIS_UNIT_NUMBER,
            Messages.NUMBERS_UNIT,
            Messages.LABEL_GRAPH_TIME);

      updateChartTime(
            _chartDuration_Values,
            _minMaxKeeperStat_Duration_Sum,
            _statDurationTime_Sum_Low,
            _statDurationTime_Sum_High,
            _statDurationTime_Sum_ColorIndex,
            ChartDataXSerie.AXIS_UNIT_HOUR_MINUTE,
            Messages.LABEL_GRAPH_TIME_UNIT,
            Messages.LABEL_GRAPH_TIME);
   }

   @Override
   public void updateToolBar() {}

}
