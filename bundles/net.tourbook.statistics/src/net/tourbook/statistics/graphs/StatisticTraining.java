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

import java.time.ZonedDateTime;
import java.util.ArrayList;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartStatisticSegments;
import net.tourbook.chart.ChartToolTipInfo;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.chart.MinMaxKeeper_YData;
import net.tourbook.chart.SelectionBarChart;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.IToolTipHideListener;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.DurationTime;
import net.tourbook.statistic.StatisticContext;
import net.tourbook.statistic.StatisticView;
import net.tourbook.statistic.TourbookStatistic;
import net.tourbook.statistics.IBarSelectionProvider;
import net.tourbook.statistics.Messages;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.statistics.StatisticTourToolTip;
import net.tourbook.statistics.TourChartContextProvider;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourInfoIconToolTipProvider;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ChartOptions_Grid;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.action.ActionEditQuick;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;

public abstract class StatisticTraining extends TourbookStatistic implements IBarSelectionProvider, ITourProvider {

   private static final String         TOUR_TOOLTIP_FORMAT_DATE_WEEK_TIME = net.tourbook.ui.Messages.Tour_Tooltip_Format_DateWeekTime;

   private static final String         NL                                 = UI.NEW_LINE1;
   private static final String         NL2                                = NL + NL;

   private TourTypeFilter              _activeTourTypeFilter;
   private TourPerson                  _activePerson;

   private long                        _selectedTourId                    = -1;

   private int                         _currentYear;
   private int                         _numberOfYears;
   private boolean                     _isForceReloadData;

   private Chart                       _chart;
   private StatisticContext            _statContext;

   private final MinMaxKeeper_YData    _minMaxKeeper                      = new MinMaxKeeper_YData();
   private TourData_Day                _tourDayData;

   private ChartDataYSerie             _yData_Duration;
   private ChartDataYSerie             _yData_TrainingPerformance;

   private boolean                     _isSynchScaleEnabled;

   private ITourEventListener          _tourPropertyListener;
   private StatisticTourToolTip        _tourToolTip;

   private TourInfoIconToolTipProvider _tourInfoToolTipProvider           = new TourInfoIconToolTipProvider();

   private void addTourPropertyListener() {

      _tourPropertyListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part,
                                 final TourEventId propertyId,
                                 final Object propertyData) {

            if (propertyId == TourEventId.TOUR_CHANGED && propertyData instanceof TourEvent) {

               // check if a tour was modified
               final ArrayList<TourData> modifiedTours = ((TourEvent) propertyData).getModifiedTours();
               if (modifiedTours != null) {

                  for (final TourData modifiedTourData : modifiedTours) {

                     final long modifiedTourId = modifiedTourData.getTourId();

                     final long[] tourIds = _tourDayData.allTourIds;
                     for (int tourIdIndex = 0; tourIdIndex < tourIds.length; tourIdIndex++) {

                        final long tourId = tourIds[tourIdIndex];

                        if (tourId == modifiedTourId) {

                           // set new tour title
                           _tourDayData.allTourTitles.set(tourIdIndex, modifiedTourData.getTourTitle());

                           break;
                        }
                     }
                  }
               }
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourPropertyListener);
   }

   /**
    * create segments for the chart
    */
   ChartStatisticSegments createChartSegments(final TourData_Day tourTimeData) {

      final double[] segmentStart = new double[_numberOfYears];
      final double[] segmentEnd = new double[_numberOfYears];
      final String[] segmentTitle = new String[_numberOfYears];

      final int[] allYearDays = tourTimeData.allYearDays;
      final int oldestYear = _currentYear - _numberOfYears + 1;
      int yearDaysSum = 0;

      // create segments for each year
      for (int yearIndex = 0; yearIndex < allYearDays.length; yearIndex++) {

         final int yearDays = allYearDays[yearIndex];

         segmentStart[yearIndex] = yearDaysSum;
         segmentEnd[yearIndex] = yearDaysSum + yearDays - 1;
         segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

         yearDaysSum += yearDays;
      }

      final ChartStatisticSegments chartSegments = new ChartStatisticSegments();
      chartSegments.segmentStartValue = segmentStart;
      chartSegments.segmentEndValue = segmentEnd;
      chartSegments.segmentTitle = segmentTitle;

      chartSegments.years = tourTimeData.allYearNumbers;
      chartSegments.yearDays = tourTimeData.allYearDays;
      chartSegments.allValues = tourTimeData.allDaysInAllYears;

      return chartSegments;
   }

   @Override
   public void createStatisticUI(final Composite parent, final IViewSite viewSite) {

      // create statistic chart
      _chart = new Chart(parent, SWT.FLAT);
      _chart.setShowZoomActions(true);
      _chart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);

      // set tour info icon into the left axis
      _tourToolTip = new StatisticTourToolTip(_chart.getToolTipControl());
      _tourToolTip.addToolTipProvider(_tourInfoToolTipProvider);
      _tourToolTip.addHideListener(new IToolTipHideListener() {
         @Override
         public void afterHideToolTip(final Event event) {
            // hide hovered image
            _chart.getToolTipControl().afterHideToolTip(event);
         }
      });

      _chart.setTourInfoIconToolTipProvider(_tourInfoToolTipProvider);
      _tourInfoToolTipProvider.setActionsEnabled(true);

      _chart.addBarSelectionListener(new IBarSelectionListener() {
         @Override
         public void selectionChanged(final int serieIndex, final int valueIndex) {
            if (_tourDayData.allTypeIds.length > 0) {

               _selectedTourId = _tourDayData.allTourIds[valueIndex];
               _tourInfoToolTipProvider.setTourId(_selectedTourId);

               if (StatisticView.isInUpdateUI()) {

                  /*
                   * Do not fire an event when this is running already in an update event. This
                   * occures when a tour is modified (marker) in the toubook view and the stat view
                   * is opened !!!
                   */

                  return;
               }

               // don't fire an event when preferences are updated
               if (isInPreferencesUpdate() || _statContext.canFireEvents() == false) {
                  return;
               }

               // this view can be inactive -> selection is not fired with the SelectionProvider interface
               TourManager.fireEventWithCustomData(
                     TourEventId.TOUR_SELECTION,
                     new SelectionTourId(_selectedTourId),
                     viewSite.getPart());
            }
         }
      });

      /*
       * open tour with double click on the tour bar
       */
      _chart.addDoubleClickListener(new IBarSelectionListener() {
         @Override
         public void selectionChanged(final int serieIndex, final int valueIndex) {

            _selectedTourId = _tourDayData.allTourIds[valueIndex];
            _tourInfoToolTipProvider.setTourId(_selectedTourId);

            ActionEditQuick.doAction(StatisticTraining.this);
         }
      });

      /*
       * open tour with Enter key
       */
      _chart.addTraverseListener(new TraverseListener() {
         @Override
         public void keyTraversed(final TraverseEvent event) {

            if (event.detail == SWT.TRAVERSE_RETURN) {
               final ISelection selection = _chart.getSelection();
               if (selection instanceof SelectionBarChart) {
                  final SelectionBarChart barChartSelection = (SelectionBarChart) selection;

                  if (barChartSelection.serieIndex != -1) {

                     _selectedTourId = _tourDayData.allTourIds[barChartSelection.valueIndex];
                     _tourInfoToolTipProvider.setTourId(_selectedTourId);

                     ActionEditQuick.doAction(StatisticTraining.this);
                  }
               }
            }
         }
      });

      addTourPropertyListener();
   }

   private ChartToolTipInfo createToolTipData(int valueIndex) {

      final int[] tourDOYValues = _tourDayData.getDoyValues();

      if (valueIndex >= tourDOYValues.length) {
         valueIndex -= tourDOYValues.length;
      }

      if (tourDOYValues == null || valueIndex >= tourDOYValues.length) {
         return null;
      }

      final long tooltipTourId = _tourDayData.allTourIds[valueIndex];

      final String tourTypeName = TourDatabase.getTourTypeName(_tourDayData.allTypeIds[valueIndex]);
      final String tourTags = TourDatabase.getTagNames(_tourDayData.tagIds.get(tooltipTourId));
      final String tourDescription = _tourDayData.allTourDescriptions.get(valueIndex)
            .replace(
                  net.tourbook.ui.UI.SYSTEM_NEW_LINE,
                  UI.NEW_LINE1);

      final int[] startValue = _tourDayData.allStartTime;
      final int[] endValue = _tourDayData.allEndTime;

      final int elapsedTime = _tourDayData.allDeviceTime_Elapsed[valueIndex];
      final int movingTime = _tourDayData.allComputedTime_Moving[valueIndex];
      final int breakTime = elapsedTime - movingTime;
      final int recordedTime = _tourDayData.allDeviceTime_Recorded[valueIndex];
      final int pausedTime = _tourDayData.allDeviceTime_Paused[valueIndex];

      final ZonedDateTime zdtTourStart = _tourDayData.allStartDateTimes.get(valueIndex);
      final ZonedDateTime zdtTourEnd = zdtTourStart.plusSeconds(elapsedTime);

      final float distance = _tourDayData.allDistance[valueIndex];
      final boolean isPaceAndSpeedFromRecordedTime = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_PACEANDSPEED_FROM_RECORDED_TIME);
      final int time = isPaceAndSpeedFromRecordedTime ? recordedTime : movingTime;
      final float speed = time == 0 ? 0 : distance / (time / 3.6f);
      final float pace = distance == 0 ? 0 : time * 1000 / distance;

      final float training_Effect = _tourDayData.allTraining_Effect[valueIndex];
      final float training_Effect_Anaerobic = _tourDayData.allTraining_Effect_Anaerobic[valueIndex];
      final float training_Performance = _tourDayData.allTraining_Performance[valueIndex];

      final StringBuilder toolTipFormat = new StringBuilder();

      toolTipFormat.append(TOUR_TOOLTIP_FORMAT_DATE_WEEK_TIME + NL2); //      %s - %s - %s - CW %d

      toolTipFormat.append(Messages.tourtime_info_distance_tour + NL);
      toolTipFormat.append(Messages.tourtime_info_altitude + NL);
      toolTipFormat.append(Messages.tourtime_info_time + NL2);

      toolTipFormat.append(Messages.tourtime_info_elapsed_time_tour + NL);
      toolTipFormat.append(Messages.tourtime_info_recorded_time_tour + NL);
      toolTipFormat.append(Messages.tourtime_info_paused_time_tour + NL);
      toolTipFormat.append(Messages.tourtime_info_moving_time_tour + NL);
      toolTipFormat.append(Messages.tourtime_info_break_time_tour + NL2);

      toolTipFormat.append(Messages.tourtime_info_avg_speed + NL);
      toolTipFormat.append(Messages.tourtime_info_avg_pace + NL2);

      toolTipFormat.append(Messages.Tourtime_Info_Training_Effect_Aerob + NL);
      toolTipFormat.append(Messages.Tourtime_Info_Training_Effect_Anaerob + NL);
      toolTipFormat.append(Messages.Tourtime_Info_Training_Performance + NL2);

      toolTipFormat.append(Messages.tourtime_info_tour_type + NL);
      toolTipFormat.append(Messages.tourtime_info_tags);

      if (tourDescription.length() > 0) {

         toolTipFormat.append(NL2);
         toolTipFormat.append(Messages.tourtime_info_description + NL);
         toolTipFormat.append(Messages.tourtime_info_description_text);
      }

      final int tourStartTime = startValue[valueIndex];
      final int tourEndTime = endValue[valueIndex];

      final String toolTipLabel = String.format(

            toolTipFormat.toString(),

            // date/time
            zdtTourStart.format(TimeTools.Formatter_Date_F),
            zdtTourStart.format(TimeTools.Formatter_Time_M),
            zdtTourEnd.format(TimeTools.Formatter_Time_M),
            zdtTourStart.get(TimeTools.calendarWeek.weekOfWeekBasedYear()),

            // distance
            distance / 1000,
            UI.UNIT_LABEL_DISTANCE,

            // altitude
            (int) _tourDayData.allAltitude[valueIndex],
            UI.UNIT_LABEL_ALTITUDE,

            // start time
            tourStartTime / 3600,
            (tourStartTime % 3600) / 60,

            // end time
            (tourEndTime / 3600) % 24,
            (tourEndTime % 3600) / 60,

            // elapsed time
            elapsedTime / 3600,
            (elapsedTime % 3600) / 60,
            (elapsedTime % 3600) % 60,

            // recorded time
            recordedTime / 3600,
            (recordedTime % 3600) / 60,
            (recordedTime % 3600) % 60,

            // paused time
            pausedTime / 3600,
            (pausedTime % 3600) / 60,
            (pausedTime % 3600) % 60,

            // moving time
            movingTime / 3600,
            (movingTime % 3600) / 60,
            (movingTime % 3600) % 60,

            // break time
            breakTime / 3600,
            (breakTime % 3600) / 60,
            (breakTime % 3600) % 60,

            // speed
            speed,
            UI.UNIT_LABEL_SPEED,

            // pace
            (int) (pace / 60),
            (int) (pace % 60),
            UI.UNIT_LABEL_PACE,

            // training
            training_Effect,
            training_Effect_Anaerobic,
            training_Performance,

            // tour type / tags
            tourTypeName,
            tourTags,

            // description
            tourDescription

      );

      // set title
      String tourTitle = _tourDayData.allTourTitles.get(valueIndex);
      if (tourTitle == null || tourTitle.trim().length() == 0) {
         tourTitle = tourTypeName;
      }

      final ChartToolTipInfo tt1 = new ChartToolTipInfo();
      tt1.setTitle(tourTitle);
      tt1.setLabel(toolTipLabel);

      return tt1;
   }

   /**
    * create data for the x-axis
    */
   void createXData_Day(final ChartDataModel chartModel) {

      final ChartDataXSerie xData = new ChartDataXSerie(_tourDayData.getDoyValuesDouble());
      xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_DAY);
//      xData.setVisibleMaxValue(fCurrentYear);
      xData.setChartSegments(createChartSegments(_tourDayData));

      chartModel.setXData(xData);
   }

   /**
    * Altitude
    */
   void createYData_Altitude(final ChartDataModel chartModel, final ChartType chartType) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            chartType,
            _tourDayData.allElevation_Low,
            _tourDayData.allElevation_High);

      yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
      yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setVisibleMinValue(0);
      yData.setColorIndex(new int[][] { _tourDayData.allTypeColorIndices });

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE, _activeTourTypeFilter);

      chartModel.addYData(yData);
   }

   void createYData_AvgPace(final ChartDataModel chartDataModel, final ChartType chartType) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            chartType,
            _tourDayData.allAvgPace_Low,
            _tourDayData.allAvgPace_High);

      yData.setYTitle(Messages.LABEL_GRAPH_PACE);
      yData.setUnitLabel(UI.UNIT_LABEL_PACE);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setVisibleMinValue(0);
      yData.setColorIndex(new int[][] { _tourDayData.allTypeColorIndices });

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_PACE);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_PACE, _activeTourTypeFilter);

      chartDataModel.addYData(yData);
   }

   void createYData_AvgSpeed(final ChartDataModel chartDataModel, final ChartType chartType) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            chartType,
            _tourDayData.allAvgSpeed_Low,
            _tourDayData.allAvgSpeed_High);

      yData.setYTitle(Messages.LABEL_GRAPH_SPEED);
      yData.setUnitLabel(UI.UNIT_LABEL_SPEED);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setVisibleMinValue(0);
      yData.setColorIndex(new int[][] { _tourDayData.allTypeColorIndices });

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_SPEED);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_SPEED, _activeTourTypeFilter);

      chartDataModel.addYData(yData);
   }

   /**
    * Distance
    */
   void createYData_Distance(final ChartDataModel chartModel, final ChartType chartType) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            chartType,
            _tourDayData.allDistance_Low,
            _tourDayData.allDistance_High);

      yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
      yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
      yData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setVisibleMinValue(0);
      yData.setValueDivisor(1000);
      yData.setColorIndex(new int[][] { _tourDayData.allTypeColorIndices });

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE, _activeTourTypeFilter);

      chartModel.addYData(yData);
   }

   /**
    * Duration - Time
    */
   void createYData_Duration(final ChartDataModel chartModel, final ChartType chartType) {

      _yData_Duration = new ChartDataYSerie(
            chartType,
            _tourDayData.getDurationLowFloat(),
            _tourDayData.getDurationHighFloat());

      _yData_Duration.setYTitle(Messages.LABEL_GRAPH_TIME);
      _yData_Duration.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
      _yData_Duration.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
      _yData_Duration.setAllValueColors(0);
      _yData_Duration.setShowYSlider(true);
      _yData_Duration.setVisibleMinValue(0);
      _yData_Duration.setColorIndex(new int[][] { _tourDayData.allTypeColorIndices });

      StatisticServices.setDefaultColors(_yData_Duration, GraphColorManager.PREF_GRAPH_TIME);
      StatisticServices.setTourTypeColors(_yData_Duration, GraphColorManager.PREF_GRAPH_TIME, _activeTourTypeFilter);

      chartModel.addYData(_yData_Duration);
   }

   /**
    * Training effect aerob
    */
   void createYData_TrainingEffect_Aerob(final ChartDataModel chartModel, final ChartType chartType) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            chartType,
            _tourDayData.allTrainingEffect_Aerob_Low,
            _tourDayData.allTrainingEffect_Aerob_High);

      yData.setYTitle(Messages.LABEL_GRAPH_TRAINING_EFFECT);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setVisibleMinValue(0);
      yData.setColorIndex(new int[][] { _tourDayData.allTypeColorIndices });

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_TRAINING_EFFECT_AEROB);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TRAINING_EFFECT_AEROB, _activeTourTypeFilter);

      chartModel.addYData(yData);
   }

   /**
    * Training effect anaerobic
    */
   void createYData_TrainingEffect_Anaerob(final ChartDataModel chartModel, final ChartType chartType) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            chartType,
            _tourDayData.allTrainingEffect_Anaerob_Low,
            _tourDayData.allTrainingEffect_Anaerob_High);

      yData.setYTitle(Messages.LABEL_GRAPH_TRAINING_EFFECT_ANAEROBIC);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setVisibleMinValue(0);
      yData.setColorIndex(new int[][] { _tourDayData.allTypeColorIndices });

      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_TRAINING_EFFECT_ANAEROB);
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TRAINING_EFFECT_ANAEROB, _activeTourTypeFilter);

      chartModel.addYData(yData);
   }

   /**
    * Training performance
    */
   void createYData_TrainingPerformance(final ChartDataModel chartModel, final ChartType chartType) {

      _yData_TrainingPerformance = new ChartDataYSerie(
            chartType,
            _tourDayData.allTrainingPerformance_Low,
            _tourDayData.allTrainingPerformance_High);

      _yData_TrainingPerformance.setYTitle(Messages.LABEL_GRAPH_TRAINING_PERFORMANCE);
      _yData_TrainingPerformance.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      _yData_TrainingPerformance.setAllValueColors(0);
      _yData_TrainingPerformance.setShowYSlider(true);
      _yData_TrainingPerformance.setVisibleMinValue(0);
      _yData_TrainingPerformance.setColorIndex(new int[][] { _tourDayData.allTypeColorIndices });

      StatisticServices.setDefaultColors(_yData_TrainingPerformance, GraphColorManager.PREF_GRAPH_TRAINING_PERFORMANCE);
      StatisticServices.setTourTypeColors(_yData_TrainingPerformance, GraphColorManager.PREF_GRAPH_TRAINING_PERFORMANCE, _activeTourTypeFilter);

      chartModel.addYData(_yData_TrainingPerformance);
   }

   @Override
   public void dispose() {

      TourManager.getInstance().removeTourEventListener(_tourPropertyListener);

      super.dispose();
   }

   /**
    */
   abstract ChartDataModel getChartDataModel();

   @Override
   public int getEnabledGridOptions() {

      return ChartOptions_Grid.GRID_VERTICAL_DISTANCE
            | ChartOptions_Grid.GRID_IS_SHOW_HORIZONTAL_LINE
            | ChartOptions_Grid.GRID_IS_SHOW_VERTICAL_LINE;
   }

   @Override
   public Long getSelectedTour() {
      return _selectedTourId;
   }

   @Override
   public Long getSelectedTourId() {
      return _selectedTourId;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      if (_selectedTourId == -1) {
         return null;
      }

      final ArrayList<TourData> selectedTours = new ArrayList<>();

      selectedTours.add(TourManager.getInstance().getTourData(_selectedTourId));

      return selectedTours;
   }

   @Override
   public void preferencesHasChanged() {

      updateStatistic(new StatisticContext(_activePerson, _activeTourTypeFilter, _currentYear, _numberOfYears));
   }

   void resetMinMaxKeeper() {

      _minMaxKeeper.resetMinMax();
   }

   @Override
   public void restoreState(final IDialogSettings state) {

      final String stateTourId = state.get(STATE_SELECTED_TOUR_ID);
      if (stateTourId != null) {
         try {
            final long tourId = Long.parseLong(stateTourId);
            selectTour(tourId);
         } catch (final Exception e) {
            // ignore
         }
      }
   }

   @Override
   public void saveState(final IDialogSettings state) {

      if (_chart == null || _chart.isDisposed()) {
         return;
      }

      final ISelection selection = _chart.getSelection();
      if (_tourDayData != null && selection instanceof SelectionBarChart) {

         final int valueIndex = ((SelectionBarChart) selection).valueIndex;

         // check array bounds
         if (valueIndex < _tourDayData.allTourIds.length) {
            state.put(STATE_SELECTED_TOUR_ID, Long.toString(_tourDayData.allTourIds[valueIndex]));
         }
      }
   }

   @Override
   public boolean selectTour(final Long tourId) {

      final long[] tourIds = _tourDayData.allTourIds;
      final boolean selectedItems[] = new boolean[tourIds.length];
      boolean isSelected = false;

      // find the tour which has the same tourId as the selected tour
      for (int tourIndex = 0; tourIndex < tourIds.length; tourIndex++) {
         final boolean isTourSelected = tourIds[tourIndex] == tourId ? true : false;
         if (isTourSelected) {
            isSelected = true;
            _selectedTourId = tourId;
            _tourInfoToolTipProvider.setTourId(_selectedTourId);
         }
         selectedItems[tourIndex] = isTourSelected;
      }

      if (isSelected == false) {
         // select first tour
//         selectedItems[0] = true;
      }

      _chart.setSelectedBars(selectedItems);

      return isSelected;
   }

   private void setChartProviders(final Chart chartWidget, final ChartDataModel chartModel) {

      // set tool tip info
      chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
         @Override
         public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
            return createToolTipData(valueIndex);
         }
      });

      // set the menu context provider
      chartModel.setCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER, new TourChartContextProvider(_chart, this));
   }

   public void setIsForceReloadData(final boolean isForceReloadData) {
      _isForceReloadData = isForceReloadData;
   }

   @Override
   public void setSynchScale(final boolean isSynchScaleEnabled) {

      _isSynchScaleEnabled = isSynchScaleEnabled;

      if (!isSynchScaleEnabled) {
         // reset when sync is set to be disabled
         resetMinMaxKeeper();
      }
   }

   @Override
   public void updateStatistic(final StatisticContext statContext) {

      _statContext = statContext;

      _activePerson = statContext.appPerson;
      _activeTourTypeFilter = statContext.appTourTypeFilter;
      _currentYear = statContext.statFirstYear;
      _numberOfYears = statContext.statNumberOfYears;

      /*
       * get currently selected tour id
       */
      long selectedTourId = -1;
      final ISelection selection = _chart.getSelection();
      if (selection instanceof SelectionBarChart) {
         final SelectionBarChart barChartSelection = (SelectionBarChart) selection;

         if (barChartSelection.serieIndex != -1) {

            int selectedValueIndex = barChartSelection.valueIndex;
            final long[] tourIds = _tourDayData.allTourIds;

            if (tourIds.length > 0) {

               if (selectedValueIndex >= tourIds.length) {
                  selectedValueIndex = tourIds.length - 1;
               }

               selectedTourId = tourIds[selectedValueIndex];
            }
         }
      }

      final DataProvider_Tour_Day tourDayDataProvider = DataProvider_Tour_Day.getInstance();

      boolean isAvgValue = false;

      DurationTime durationTime = DurationTime.MOVING;

      // set state if average values should be displayed or not, set it BEFORE retrieving data
      if (this instanceof StatisticTraining_Bar) {

         // ensure the data are comuted with the correct graph context, otherwise it do not work depending what was previously selected
         _isForceReloadData = true;

         durationTime = (DurationTime) Util.getEnumValue(
               _prefStore.getString(ITourbookPreferences.STAT_TRAINING_BAR_DURATION_TIME),
               DurationTime.MOVING);

         isAvgValue = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE_AVG_VALUE);

         tourDayDataProvider.setGraphContext(isAvgValue, false);

      } else if (this instanceof StatisticTraining_Line) {

         // ensure the data are comuted with the correct graph context, otherwise it do not work depending what was previously selected
         _isForceReloadData = true;

         durationTime = (DurationTime) Util.getEnumValue(
               _prefStore.getString(ITourbookPreferences.STAT_TRAINING_LINE_DURATION_TIME),
               DurationTime.MOVING);

         isAvgValue = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_LINE_IS_SHOW_TRAINING_PERFORMANCE_AVG_VALUE);

         tourDayDataProvider.setGraphContext(isAvgValue, true);
      }

      _tourDayData = tourDayDataProvider.getDayData(
            statContext.appPerson,
            statContext.appTourTypeFilter,
            statContext.statFirstYear,
            statContext.statNumberOfYears,
            isDataDirtyWithReset() || statContext.isRefreshData || _isForceReloadData || _isDuration_ReloadData,
            durationTime);

      statContext.outStatisticValuesRaw = _tourDayData.statisticValuesRaw;

      _isDuration_ReloadData = false;

      // reset min/max values
      if (_isSynchScaleEnabled == false && (statContext.isRefreshData || _isForceReloadData)) {
         resetMinMaxKeeper();
      }

      // reset force loading state as it is done now
      _isForceReloadData = false;

      final ChartDataModel chartModel = getChartDataModel();

      /*
       * set graph minimum width, this is the number of days in the year
       */
      final int yearDays = TimeTools.getNumberOfDaysWithYear(_currentYear);
      chartModel.setChartMinWidth(yearDays);

      setChartProviders(_chart, chartModel);

      if (_isSynchScaleEnabled) {
         _minMaxKeeper.setMinMaxValues(chartModel);
      }

      // title MUST be set after the data model is created
      if (isAvgValue && _yData_TrainingPerformance != null) {

         // show avg sign in the title
         _yData_TrainingPerformance.setYTitle(Messages.LABEL_GRAPH_TRAINING_PERFORMANCE + UI.SPACE + UI.SYMBOL_AVERAGE);
      }

      // show selected time duration
      if (_yData_Duration != null) {
         setGraphLabel_Duration(_yData_Duration, durationTime);
      }

      StatisticServices.updateChartProperties(_chart, getGridPrefPrefix());

      // show the data in the chart
      _chart.updateChart(chartModel, false, true);

      // try to select the previous selected tour
      selectTour(selectedTourId);
   }

   @Override
   public void updateToolBar() {
      _chart.fillToolbar(true);
   }
}
