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

import java.util.ArrayList;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartStatisticSegments;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.chart.MinMaxKeeper_YData;
import net.tourbook.chart.SelectionBarChart;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
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
import net.tourbook.tour.TourInfoUI;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ChartOptions_Grid;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.action.ActionEditQuick;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

public abstract class StatisticDay extends TourbookStatistic implements IBarSelectionProvider, ITourProvider {

   private TourStatisticData_Day       _statisticData_Day;
   private DataProvider_Tour_Day       _tourDay_DataProvider    = new DataProvider_Tour_Day();

   private TourTypeFilter              _activeTourTypeFilter;
   private TourPerson                  _activePerson;

   private long                        _selectedTourId          = -1;

   private int                         _statSelectedYear;
   private int                         _statNumberOfYears;

   private Chart                       _chart;
   private StatisticContext            _statContext;

   private final MinMaxKeeper_YData    _minMaxKeeper            = new MinMaxKeeper_YData();
   private ChartDataYSerie             _yData_Duration;

   private boolean                     _isSynchScaleEnabled;

   private ITourEventListener          _tourPropertyListener;

   private StatisticTourToolTip        _tourToolTip;
   private TourInfoIconToolTipProvider _tourInfoToolTipProvider = new TourInfoIconToolTipProvider();

   private final TourInfoUI            _tourInfoUI              = new TourInfoUI();

   private ChartDataYSerie             _athleteBodyWeight_YData;
   private ChartDataYSerie             _athleteBodyFat_YData;

   private void addTourPropertyListener() {

      _tourPropertyListener = (workbenchPart, tourEventId, propertyData) -> {

         if (tourEventId == TourEventId.TOUR_CHANGED && propertyData instanceof TourEvent) {

            // check if a tour was modified
            final ArrayList<TourData> modifiedTours = ((TourEvent) propertyData).getModifiedTours();
            if (modifiedTours != null) {

               for (final TourData modifiedTourData : modifiedTours) {

                  final long modifiedTourId = modifiedTourData.getTourId();

                  final long[] tourIds = _statisticData_Day.allTourIds;
                  for (int tourIdIndex = 0; tourIdIndex < tourIds.length; tourIdIndex++) {

                     final long tourId = tourIds[tourIdIndex];

                     if (tourId == modifiedTourId) {

                        // set new tour title
                        _statisticData_Day.allTourTitles.set(tourIdIndex, modifiedTourData.getTourTitle());

                        break;
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
   ChartStatisticSegments createChartSegments(final TourStatisticData_Day tourTimeData) {

      final double[] segmentStart = new double[_statNumberOfYears];
      final double[] segmentEnd = new double[_statNumberOfYears];
      final String[] segmentTitle = new String[_statNumberOfYears];

      final int[] allYearDays = tourTimeData.allYearDays;
      final int oldestYear = _statSelectedYear - _statNumberOfYears + 1;
      int yearDaysSum = 0;

      // create segments for each year
      for (int yearIndex = 0; yearIndex < allYearDays.length; yearIndex++) {

         final int yearDays = allYearDays[yearIndex];

         segmentStart[yearIndex] = yearDaysSum;
         segmentEnd[yearIndex] = yearDaysSum + yearDays - 1.0;
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
      _tourToolTip.addHideListener(event -> {
         // hide hovered image
         _chart.getToolTipControl().afterHideToolTip();
      });

      _chart.setTourInfoIconToolTipProvider(_tourInfoToolTipProvider);
      _tourInfoToolTipProvider.setActionsEnabled(true);

      _chart.addBarSelectionListener((serieIndex, valueIndex) -> {

         if (_statisticData_Day.allTypeIds.length > 0) {

            _selectedTourId = _statisticData_Day.allTourIds[valueIndex];
            _tourInfoToolTipProvider.setTourId(_selectedTourId);

            if (StatisticView.isInUpdateUI()) {

               /*
                * Do not fire an event when this is running already in an update event. This
                * occurs when a tour is modified (marker) in the toubook view and the stat view
                * is opened !!!
                */

               return;
            }

            // don't fire an event when preferences are updated
            if (isInPreferencesUpdate() || _statContext.canFireEvents() == false) {
               return;
            }

            final SelectionTourId selection = new SelectionTourId(_selectedTourId);

            // this view can be inactive -> selection is not fired with the SelectionProvider interface
            TourManager.fireEventWithCustomData(
                  TourEventId.TOUR_SELECTION,
                  selection,
                  viewSite.getPart());

            // set selection also into the view that when the view is activated, then a tour selection is fired
            final ISelectionProvider selectionProvider = viewSite.getSelectionProvider();
            if (selectionProvider instanceof PostSelectionProvider) {
               ((PostSelectionProvider) selectionProvider).setSelectionNoFireEvent(selection);
            }
         }
      });

      /*
       * open tour with double click on the tour bar
       */
      _chart.addDoubleClickListener((serieIndex, valueIndex) -> {

         _selectedTourId = _statisticData_Day.allTourIds[valueIndex];
         _tourInfoToolTipProvider.setTourId(_selectedTourId);

         ActionEditQuick.doAction(StatisticDay.this);
      });

      /*
       * open tour with Enter key
       */
      _chart.addTraverseListener(traverseEvent -> {

         if (traverseEvent.detail == SWT.TRAVERSE_RETURN) {

            final ISelection selection = _chart.getSelection();
            if (selection instanceof SelectionBarChart) {
               final SelectionBarChart barChartSelection = (SelectionBarChart) selection;

               if (barChartSelection.serieIndex != -1) {

                  _selectedTourId = _statisticData_Day.allTourIds[barChartSelection.valueIndex];
                  _tourInfoToolTipProvider.setTourId(_selectedTourId);

                  ActionEditQuick.doAction(StatisticDay.this);
               }
            }
         }
      });

      addTourPropertyListener();
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
                                int valueIndex) {

      final int[] tourDOYValues = _statisticData_Day.getDoyValues();

      if (valueIndex >= tourDOYValues.length) {
         valueIndex -= tourDOYValues.length;
      }

      if (valueIndex >= tourDOYValues.length) {
         return;
      }

      final long tourId = _statisticData_Day.allTourIds[valueIndex];

      TourData _tourData = null;
      if (tourId != -1) {

         // first get data from the tour id when it is set
         _tourData = TourManager.getInstance().getTourData(tourId);
      }

      if (_tourData == null) {

         // there are no data available

         _tourInfoUI.createUI_NoData(parent);

      } else {

         // tour data is available

         _tourInfoUI.createContentArea(parent, _tourData, toolTipProvider, this);

         _tourInfoUI.setActionsEnabled(true);
      }

      parent.addDisposeListener(disposeEvent -> _tourInfoUI.dispose());
   }

   /**
    * create data for the x-axis
    */
   void createXDataDay(final ChartDataModel chartModel) {

      final ChartDataXSerie xData = new ChartDataXSerie(_statisticData_Day.getDoyValuesDouble());
      xData.setAxisUnit(ChartDataSerie.X_AXIS_UNIT_DAY);
//      xData.setVisibleMaxValue(fCurrentYear);
      xData.setChartSegments(createChartSegments(_statisticData_Day));

      chartModel.setXData(xData);
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
            _statisticData_Day.allAthleteBodyFat_Low,
            _statisticData_Day.allAthleteBodyFat_High);

      _athleteBodyFat_YData.setYTitle(Messages.LABEL_GRAPH_BODY_FAT);
      _athleteBodyFat_YData.setUnitLabel(UI.UNIT_PERCENT);
      _athleteBodyFat_YData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      _athleteBodyFat_YData.setVisibleMinValue(visibleMinValue);
      _athleteBodyFat_YData.setVisibleMaxValue(visibleMaxValue);
      _athleteBodyFat_YData.setShowYSlider(true);

      StatisticServices.setTourTypeColors(_athleteBodyFat_YData, GraphColorManager.PREF_GRAPH_BODYFAT);

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
            _statisticData_Day.allAthleteBodyWeight_Low,
            _statisticData_Day.allAthleteBodyWeight_High);

      _athleteBodyWeight_YData.setYTitle(Messages.LABEL_GRAPH_BODY_WEIGHT);
      _athleteBodyWeight_YData.setUnitLabel(UI.UNIT_LABEL_WEIGHT);
      _athleteBodyWeight_YData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      _athleteBodyWeight_YData.setVisibleMinValue(visibleMinValue);
      _athleteBodyWeight_YData.setVisibleMaxValue(visibleMaxValue);
      _athleteBodyWeight_YData.setShowYSlider(true);

      StatisticServices.setTourTypeColors(_athleteBodyWeight_YData, GraphColorManager.PREF_GRAPH_BODYWEIGHT);

      chartDataModel.addYData(_athleteBodyWeight_YData);
   }

   /**
    * Altitude
    */
   void createYDataAltitude(final ChartDataModel chartModel) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            _statisticData_Day.allElevationUp_Low,
            _statisticData_Day.allElevationUp_High);

      yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
      yData.setUnitLabel(UI.UNIT_LABEL_ELEVATION);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setVisibleMinValue(0);
      yData.setColorIndex(new int[][] { _statisticData_Day.allTypeColorIndices });

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE);

      chartModel.addYData(yData);
   }

   void createYDataAvgPace(final ChartDataModel chartDataModel) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            _statisticData_Day.allAvgPace_Low,
            _statisticData_Day.allAvgPace_High);

      yData.setYTitle(Messages.LABEL_GRAPH_PACE);
      yData.setUnitLabel(UI.UNIT_LABEL_PACE);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setVisibleMinValue(0);
      yData.setColorIndex(new int[][] { _statisticData_Day.allTypeColorIndices });

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_PACE);

      chartDataModel.addYData(yData);
   }

   void createYDataAvgSpeed(final ChartDataModel chartDataModel) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            _statisticData_Day.allAvgSpeed_Low,
            _statisticData_Day.allAvgSpeed_High);

      yData.setYTitle(Messages.LABEL_GRAPH_SPEED);
      yData.setUnitLabel(UI.UNIT_LABEL_SPEED);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setVisibleMinValue(0);
      yData.setColorIndex(new int[][] { _statisticData_Day.allTypeColorIndices });

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_SPEED);

      chartDataModel.addYData(yData);
   }

   /**
    * Distance
    */
   void createYDataDistance(final ChartDataModel chartModel) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            ChartType.BAR,
            _statisticData_Day.allDistance_Low,
            _statisticData_Day.allDistance_High);

      yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
      yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setVisibleMinValue(0);
      yData.setValueDivisor(1000);
      yData.setColorIndex(new int[][] { _statisticData_Day.allTypeColorIndices });

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE);

      chartModel.addYData(yData);
   }

   /**
    * Time
    */
   void createYDataDuration(final ChartDataModel chartModel) {

      _yData_Duration = new ChartDataYSerie(
            ChartType.BAR,
            _statisticData_Day.getDurationLowFloat(),
            _statisticData_Day.getDurationHighFloat());

      _yData_Duration.setYTitle(Messages.LABEL_GRAPH_TIME);
      _yData_Duration.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
      _yData_Duration.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
      _yData_Duration.setAllValueColors(0);
      _yData_Duration.setShowYSlider(true);
      _yData_Duration.setVisibleMinValue(0);
      _yData_Duration.setColorIndex(new int[][] { _statisticData_Day.allTypeColorIndices });

      StatisticServices.setTourTypeColors(_yData_Duration, GraphColorManager.PREF_GRAPH_TIME);

      chartModel.addYData(_yData_Duration);
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
   public String getRawStatisticValues(final boolean isShowSequenceNumbers) {
      return _tourDay_DataProvider.getRawStatisticValues(isShowSequenceNumbers);
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

      updateStatistic(new StatisticContext(_activePerson, _activeTourTypeFilter, _statSelectedYear, _statNumberOfYears));
   }

   void resetMinMaxKeeper() {

      _minMaxKeeper.resetMinMax();
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
      if (_statisticData_Day != null && selection instanceof SelectionBarChart) {

         final int valueIndex = ((SelectionBarChart) selection).valueIndex;

         // check array bounds
         if (valueIndex < _statisticData_Day.allTourIds.length) {
            viewState.put(STATE_SELECTED_TOUR_ID, Long.toString(_statisticData_Day.allTourIds[valueIndex]));
         }
      }

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
   public boolean selectTour(final Long tourId) {

      final long[] tourIds = _statisticData_Day.allTourIds;
      final boolean[] selectedItems = new boolean[tourIds.length];
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

   private void setChartProviders(final ChartDataModel chartModel) {

      // set tool tip info
      chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER,
            (IChartInfoProvider) (toolTipProvider, parent, serieIndex, valueIndex) -> StatisticDay.this.createToolTipUI(toolTipProvider,
                  parent,
                  valueIndex));

      // set the menu context provider
      chartModel.setCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER, new TourChartContextProvider(_chart, this));
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

      final DurationTime durationTime = (DurationTime) Util.getEnumValue(
            _prefStore.getString(ITourbookPreferences.STAT_DAY_DURATION_TIME),
            DurationTime.MOVING);

      _statContext = statContext;

      _activePerson = statContext.appPerson;
      _activeTourTypeFilter = statContext.appTourTypeFilter;
      _statSelectedYear = statContext.statSelectedYear;
      _statNumberOfYears = statContext.statNumberOfYears;

      /*
       * get currently selected tour id
       */
      long selectedTourId = -1;
      final ISelection selection = _chart.getSelection();
      if (selection instanceof SelectionBarChart) {
         final SelectionBarChart barChartSelection = (SelectionBarChart) selection;

         if (barChartSelection.serieIndex != -1) {

            int selectedValueIndex = barChartSelection.valueIndex;
            final long[] tourIds = _statisticData_Day.allTourIds;

            if (tourIds.length > 0) {

               if (selectedValueIndex >= tourIds.length) {
                  selectedValueIndex = tourIds.length - 1;
               }

               selectedTourId = tourIds[selectedValueIndex];
            }
         }
      }

      _statisticData_Day = _tourDay_DataProvider.getDayData(
            statContext.appPerson,
            statContext.appTourTypeFilter,
            statContext.statSelectedYear,
            statContext.statNumberOfYears,
            isDataDirtyWithReset() || statContext.isRefreshData || _isDuration_ReloadData,
            durationTime);

      _isDuration_ReloadData = false;

      // reset min/max values
      if (_isSynchScaleEnabled == false && statContext.isRefreshData) {
         resetMinMaxKeeper();
      }

      final ChartDataModel chartModel = getChartDataModel();

      /*
       * set graph minimum width, this is the number of days in the year
       */
      final int yearDays = TimeTools.getNumberOfDaysWithYear(_statSelectedYear);
      chartModel.setChartMinWidth(yearDays);

      setChartProviders(chartModel);

      if (_isSynchScaleEnabled) {
         _minMaxKeeper.setMinMaxValues(chartModel);
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
