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
import net.tourbook.statistic.StatisticContext;
import net.tourbook.statistic.TourbookStatistic;
import net.tourbook.statistics.IBarSelectionProvider;
import net.tourbook.statistics.Messages;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.statistics.StatisticTourToolTip;
import net.tourbook.statistics.TourChartContextProvider;
import net.tourbook.tour.SelectionTourId;
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

public class StatisticTour_Time extends TourbookStatistic implements IBarSelectionProvider, ITourProvider {

   private static final char           NL                                 = UI.NEW_LINE;

   private static final String         TOUR_TOOLTIP_FORMAT_DATE_WEEK_TIME = net.tourbook.ui.Messages.Tour_Tooltip_Format_DateWeekTime;

   private TourData_Time               _tourTime_Data;
   private DataProvider_Tour_Time      _tourTime_DataProvider             = new DataProvider_Tour_Time();

   private TourPerson                  _activePerson;
   private TourTypeFilter              _activeTourTypeFiler;
   private int                         _currentYear;
   private int                         _numberOfYears;

   private Chart                       _chart;

   private StatisticContext            _statContext;

   private StatisticTourToolTip        _tourToolTip;
   private TourInfoIconToolTipProvider _tourInfoToolTipProvider           = new TourInfoIconToolTipProvider();

   private final MinMaxKeeper_YData    _minMaxKeeper                      = new MinMaxKeeper_YData();
   private boolean                     _ifIsSynchScaleEnabled;

   private Long                        _selectedTourId                    = null;

   /**
    * create segments for the chart
    */
   private ChartStatisticSegments createChartSegments(final TourData_Time tourDataTime) {

      final double segmentStart[] = new double[_numberOfYears];
      final double segmentEnd[] = new double[_numberOfYears];
      final String[] segmentTitle = new String[_numberOfYears];

      final int[] allYearDays = tourDataTime.allYear_NumDays;
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

      chartSegments.years = tourDataTime.allYear_Numbers;
      chartSegments.yearDays = tourDataTime.allYear_NumDays;
      chartSegments.allValues = tourDataTime.numDaysInAllYears;

      return chartSegments;
   }

   @Override
   public void createStatisticUI(final Composite parent, final IViewSite viewSite) {

      // chart widget page
      _chart = new Chart(parent, SWT.FLAT);
      _chart.setShowZoomActions(true);
      _chart.setDrawBarChartAtBottom(false);
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
         public void selectionChanged(final int serieIndex, int valueIndex) {

            final long[] tourIds = _tourTime_Data.allTourIds;

            if (tourIds != null && tourIds.length > 0) {

               if (valueIndex >= tourIds.length) {
                  valueIndex = tourIds.length - 1;
               }

               _selectedTourId = tourIds[valueIndex];
               _tourInfoToolTipProvider.setTourId(_selectedTourId);

               _tourTime_DataProvider.setSelectedTourId(_selectedTourId);

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
            final long[] tourIds = _tourTime_Data.allTourIds;
            if (tourIds.length > 0) {

               _selectedTourId = tourIds[valueIndex];
               _tourInfoToolTipProvider.setTourId(_selectedTourId);

               _tourTime_DataProvider.setSelectedTourId(_selectedTourId);

               ActionEditQuick.doAction(StatisticTour_Time.this);
            }
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

                     _selectedTourId = _tourTime_Data.allTourIds[barChartSelection.valueIndex];
                     _tourInfoToolTipProvider.setTourId(_selectedTourId);

                     ActionEditQuick.doAction(StatisticTour_Time.this);
                  }
               }
            }
         }
      });

   }

   private ChartToolTipInfo createToolTipInfo(int valueIndex) {

      final int[] tourDOYValues = _tourTime_Data.allTourDOYs;

      if (valueIndex >= tourDOYValues.length) {
         valueIndex -= tourDOYValues.length;
      }

      if (tourDOYValues == null || valueIndex >= tourDOYValues.length) {
         return null;
      }

      /*
       * set calendar day/month/year
       */
      final long tooltipTourId = _tourTime_Data.allTourIds[valueIndex];

      final String tourTypeName = TourDatabase.getTourTypeName(_tourTime_Data.allTypeIds[valueIndex]);
      final String tourTags = TourDatabase.getTagNames(_tourTime_Data.allTagIds.get(tooltipTourId));
      final String tourDescription = _tourTime_Data.allTourDescriptions.get(valueIndex).replace(
            net.tourbook.ui.UI.SYSTEM_NEW_LINE,
            UI.NEW_LINE1);

      final int[] startValue = _tourTime_Data.allTourTimeStart;
      final int[] endValue = _tourTime_Data.allTourTimeEnd;

      final int elapsedTime = _tourTime_Data.allTourDeviceTime_Elapsed[valueIndex];
      final int recordedTime = _tourTime_Data.allTourDeviceTime_Recorded[valueIndex];
      final int pausedTime = elapsedTime - recordedTime;
      final int movingTime = _tourTime_Data.allTourComputedTime_Moving[valueIndex];
      final int breakTime = elapsedTime - movingTime;

      final ZonedDateTime zdtTourStart = _tourTime_Data.allTourStartDateTimes.get(valueIndex);
      final ZonedDateTime zdtTourEnd = zdtTourStart.plusSeconds(elapsedTime);

      final float distance = _tourTime_Data.allTourDistances[valueIndex];
      final boolean isPaceAndSpeedFromRecordedTime = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_PACEANDSPEED_FROM_RECORDED_TIME);
      final int time = isPaceAndSpeedFromRecordedTime ? recordedTime : movingTime;
      final float speed = time == 0 ? 0 : distance / (time / 3.6f);
      final float pace = distance == 0 ? 0 : time * 1000 / distance;

      final String tourTimeZoneOffset = _tourTime_Data.allTourTimeZoneOffsets.get(valueIndex);

      final StringBuilder toolTipFormat = new StringBuilder();
      toolTipFormat.append(TOUR_TOOLTIP_FORMAT_DATE_WEEK_TIME); //		%s - %s - %s - CW %d
      toolTipFormat.append(NL);
      toolTipFormat.append(NL);
      toolTipFormat.append(Messages.tourtime_info_distance_tour);
      toolTipFormat.append(NL);
      toolTipFormat.append(Messages.tourtime_info_altitude);
      toolTipFormat.append(NL);
      toolTipFormat.append(Messages.tourtime_info_time);
      toolTipFormat.append(NL);
      toolTipFormat.append(NL);
      toolTipFormat.append(Messages.Tourtime_Info_TimeZone);
      toolTipFormat.append(NL);
      toolTipFormat.append(Messages.Tourtime_Info_TimeZoneDifference);
      toolTipFormat.append(NL);
      toolTipFormat.append(NL);
      toolTipFormat.append(Messages.tourtime_info_elapsed_time_tour);
      toolTipFormat.append(NL);
      toolTipFormat.append(Messages.tourtime_info_recorded_time_tour);
      toolTipFormat.append(NL);
      toolTipFormat.append(Messages.tourtime_info_paused_time_tour);
      toolTipFormat.append(NL);
      toolTipFormat.append(Messages.tourtime_info_moving_time_tour);
      toolTipFormat.append(NL);
      toolTipFormat.append(Messages.tourtime_info_break_time_tour);
      toolTipFormat.append(NL);
      toolTipFormat.append(NL);
      toolTipFormat.append(Messages.tourtime_info_avg_speed);
      toolTipFormat.append(NL);
      toolTipFormat.append(Messages.tourtime_info_avg_pace);
      toolTipFormat.append(NL);
      toolTipFormat.append(NL);
      toolTipFormat.append(Messages.tourtime_info_tour_type);
      toolTipFormat.append(NL);
      toolTipFormat.append(Messages.tourtime_info_tags);

      if (tourDescription.length() > 0) {
         toolTipFormat.append(NL);
         toolTipFormat.append(NL);
         toolTipFormat.append(Messages.tourtime_info_description);
         toolTipFormat.append(NL);
         toolTipFormat.append(Messages.tourtime_info_description_text);
      }

      final String toolTipLabel = String.format(toolTipFormat.toString(),
            //
            // date/time
            zdtTourStart.format(TimeTools.Formatter_Date_F),
            zdtTourStart.format(TimeTools.Formatter_Time_M),
            zdtTourEnd.format(TimeTools.Formatter_Time_M),
            zdtTourStart.get(TimeTools.calendarWeek.weekOfWeekBasedYear()),
            //
            distance / 1000,
            UI.UNIT_LABEL_DISTANCE,
            //
            (int) _tourTime_Data.allTourElevations[valueIndex],
            UI.UNIT_LABEL_ALTITUDE,
            //
            // start time
            startValue[valueIndex] / 3600,
            (startValue[valueIndex] % 3600) / 60,
            //
            // end time
            endValue[valueIndex] / 3600 % 24,
            (endValue[valueIndex] % 3600) / 60,
            //
            // time zone
            zdtTourStart.getZone().getId(),
            tourTimeZoneOffset,
            //
            elapsedTime / 3600,
            (elapsedTime % 3600) / 60,
            (elapsedTime % 3600) % 60,
            //
            recordedTime / 3600,
            (recordedTime % 3600) / 60,
            (recordedTime % 3600) % 60,
            //
            pausedTime / 3600,
            (pausedTime % 3600) / 60,
            (pausedTime % 3600) % 60,
            //
            movingTime / 3600,
            (movingTime % 3600) / 60,
            (movingTime % 3600) % 60,
            //
            breakTime / 3600,
            (breakTime % 3600) / 60,
            (breakTime % 3600) % 60,
            //
            speed,
            UI.UNIT_LABEL_SPEED,
            //
            (int) pace / 60,
            (int) pace % 60,
            UI.UNIT_LABEL_PACE,
            //
            tourTypeName,
            tourTags,
            //
            tourDescription
      //
      )
            .toString();

      /*
       * create tool tip info
       */
      String tourTitle = _tourTime_Data.allTourTitles.get(valueIndex);
      if (tourTitle == null || tourTitle.trim().length() == 0) {
         tourTitle = tourTypeName;
      }

      final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();

      toolTipInfo.setTitle(tourTitle);
      toolTipInfo.setLabel(toolTipLabel);

      return toolTipInfo;
   }

   @Override
   public int getEnabledGridOptions() {

      return ChartOptions_Grid.GRID_VERTICAL_DISTANCE
            | ChartOptions_Grid.GRID_IS_SHOW_HORIZONTAL_LINE
            | ChartOptions_Grid.GRID_IS_SHOW_VERTICAL_LINE;
   }

   @Override
   protected String getGridPrefPrefix() {
      return GRID_TOUR_TIME;
   }

   @Override
   public String getRawStatisticValues(final boolean isShowSequenceNumbers) {
      return _tourTime_DataProvider.getRawStatisticValues(isShowSequenceNumbers);
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

      if (_selectedTourId == null) {
         return null;
      }

      final ArrayList<TourData> selectedTours = new ArrayList<>();

      selectedTours.add(TourManager.getInstance().getTourData(_selectedTourId));

      return selectedTours;
   }

   @Override
   public void preferencesHasChanged() {

      updateStatistic(new StatisticContext(_activePerson, _activeTourTypeFiler, _currentYear, _numberOfYears));
   }

   @Override
   public void restoreState(final IDialogSettings viewState) {

      final String mementoTourId = viewState.get(STATE_SELECTED_TOUR_ID);
      if (mementoTourId != null) {
         try {
            final long tourId = Long.parseLong(mementoTourId);
            selectTour(tourId);
         } catch (final Exception e) {
            // ignore
         }
      }
   }

   @Override
   public void saveState(final IDialogSettings viewState) {

      if (_chart == null || _chart.isDisposed()) {
         return;
      }

      final ISelection selection = _chart.getSelection();
      if (_tourTime_Data != null
            && _tourTime_Data.allTourIds != null
            && _tourTime_Data.allTourIds.length > 0
            && selection instanceof SelectionBarChart) {

         final Long selectedTourId = _tourTime_Data.allTourIds[((SelectionBarChart) selection).valueIndex];

         viewState.put(STATE_SELECTED_TOUR_ID, Long.toString(selectedTourId));
      }
   }

   @Override
   public boolean selectTour(final Long tourId) {

      final long[] tourIds = _tourTime_Data.allTourIds;

      if (tourIds.length == 0) {
         _selectedTourId = null;
         _tourInfoToolTipProvider.setTourId(-1);

         return false;
      }

      final boolean selectedTours[] = new boolean[tourIds.length];

      boolean isSelected = false;

      // find the tour which has the same tourId as the selected tour
      for (int tourIndex = 0; tourIndex < tourIds.length; tourIndex++) {
         if ((tourIds[tourIndex] == tourId)) {
            selectedTours[tourIndex] = true;
            isSelected = true;

            _selectedTourId = tourId;
            _tourInfoToolTipProvider.setTourId(_selectedTourId);

            break;
         }
      }

      if (isSelected == false) {
         // select first tour
         selectedTours[0] = true;
         _selectedTourId = tourIds[0];
         _tourInfoToolTipProvider.setTourId(_selectedTourId);
      }

      _chart.setSelectedBars(selectedTours);

      return isSelected;
   }

   private void setChartProviders(final Chart chartWidget, final ChartDataModel chartModel) {

      final IChartInfoProvider chartInfoProvider = new IChartInfoProvider() {

         @Override
         public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
            return createToolTipInfo(valueIndex);
         }
      };

      chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, chartInfoProvider);

      // set the menu context provider
      chartModel.setCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER, new TourChartContextProvider(_chart, this));
   }

   @Override
   public void setSynchScale(final boolean isSynchScaleEnabled) {

      if (!isSynchScaleEnabled) {

         // reset when it's disabled

         _minMaxKeeper.resetMinMax();
      }

      _ifIsSynchScaleEnabled = isSynchScaleEnabled;
   }

   private void updateChart(final long selectedTourId) {

      final ChartDataModel chartModel = new ChartDataModel(ChartType.BAR);

      // set the x-axis
      final ChartDataXSerie xData = new ChartDataXSerie(Util.convertIntToDouble(_tourTime_Data.allTourDOYs));
      xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_DAY);
      xData.setVisibleMaxValue(_currentYear);
      xData.setChartSegments(createChartSegments(_tourTime_Data));
      chartModel.setXData(xData);

      // set the bar low/high data
      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            Util.convertIntToFloat(_tourTime_Data.allTourTimeStart),
            Util.convertIntToFloat(_tourTime_Data.allTourTimeEnd));
      yData.setYTitle(Messages.LABEL_GRAPH_DAYTIME);
      yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
      yData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_HOUR_MINUTE_24H);
      yData.setYAxisDirection(false);
      yData.setShowYSlider(true);

      yData.setColorIndex(new int[][] { _tourTime_Data.allTypeColorIndices });
      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TIME, _activeTourTypeFiler);
      StatisticServices.setDefaultColors(yData, GraphColorManager.PREF_GRAPH_TIME);

      chartModel.addYData(yData);

      /*
       * set graph minimum width, this is the number of days in the year
       */
      final int yearDays = TimeTools.getNumberOfDaysWithYear(_currentYear);
      chartModel.setChartMinWidth(yearDays);

      setChartProviders(_chart, chartModel);

      if (_ifIsSynchScaleEnabled) {
         _minMaxKeeper.setMinMaxValues(chartModel);
      }

      StatisticServices.updateChartProperties(_chart, getGridPrefPrefix());

      // show the data in the chart
      _chart.updateChart(chartModel, false, true);

      // try to select the previous selected tour
      selectTour(selectedTourId);
   }

   @Override
   public void updateStatistic(final StatisticContext statContext) {

      _statContext = statContext;

      _activePerson = statContext.appPerson;
      _activeTourTypeFiler = statContext.appTourTypeFilter;
      _currentYear = statContext.statFirstYear;
      _numberOfYears = statContext.statNumberOfYears;

      /*
       * get currently selected tour id
       */
      long selectedTourId = -1;
      final ISelection selection = _chart.getSelection();
      if (selection instanceof SelectionBarChart) {
         final SelectionBarChart barChartSelection = (SelectionBarChart) selection;

         if (barChartSelection.serieIndex != -1 && _tourTime_Data != null) {

            int selectedValueIndex = barChartSelection.valueIndex;
            final long[] tourIds = _tourTime_Data.allTourIds;

            if (tourIds.length > 0) {
               if (selectedValueIndex >= tourIds.length) {
                  selectedValueIndex = tourIds.length - 1;
               }

               selectedTourId = tourIds[selectedValueIndex];
            }
         }
      }

      _tourTime_Data = _tourTime_DataProvider.getTourTimeData(
            statContext.appPerson,
            statContext.appTourTypeFilter,
            statContext.statFirstYear,
            statContext.statNumberOfYears,
            isDataDirtyWithReset() || statContext.isRefreshData);

      // reset min/max values
      if (_ifIsSynchScaleEnabled == false && statContext.isRefreshData) {
         _minMaxKeeper.resetMinMax();
      }

      updateChart(selectedTourId);
   }

   @Override
   public void updateToolBar() {
      _chart.fillToolbar(true);
   }

}
