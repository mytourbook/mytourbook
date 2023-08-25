/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.chart.MinMaxKeeper_YData;
import net.tourbook.chart.SelectionBarChart;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.IToolTipHideListener;
import net.tourbook.common.util.IToolTipProvider;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;

public abstract class StatisticTraining extends TourbookStatistic implements IBarSelectionProvider, ITourProvider {

   private TourStatisticData_Day       _statisticData_Training;
   private DataProvider_Tour_Day       _tourDay_DataProvider    = new DataProvider_Tour_Day();

   private TourTypeFilter              _activeTourTypeFilter;
   private TourPerson                  _activePerson;

   private long                        _selectedTourId          = -1;

   private int                         _currentYear;
   private int                         _numberOfYears;
   private boolean                     _isForceReloadData;

   private Chart                       _chart;
   private StatisticContext            _statContext;

   private final MinMaxKeeper_YData    _minMaxKeeper            = new MinMaxKeeper_YData();

   private ChartDataYSerie             _yData_Duration;
   private ChartDataYSerie             _yData_TrainingPerformance;

   private boolean                     _isSynchScaleEnabled;

   private ITourEventListener          _tourPropertyListener;
   private StatisticTourToolTip        _tourToolTip;

   private TourInfoIconToolTipProvider _tourInfoToolTipProvider = new TourInfoIconToolTipProvider();

   private final TourInfoUI            _tourInfoUI              = new TourInfoUI();

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

                     final long[] tourIds = _statisticData_Training.allTourIds;
                     for (int tourIdIndex = 0; tourIdIndex < tourIds.length; tourIdIndex++) {

                        final long tourId = tourIds[tourIdIndex];

                        if (tourId == modifiedTourId) {

                           // set new tour title
                           _statisticData_Training.allTourTitles.set(tourIdIndex, modifiedTourData.getTourTitle());

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
   ChartStatisticSegments createChartSegments(final TourStatisticData_Day tourTimeData) {

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

// antialias looks ugly
//      _chart.graphAntialiasing = SWT.ON;

      // set tour info icon into the left axis
      _tourToolTip = new StatisticTourToolTip(_chart.getToolTipControl());
      _tourToolTip.addToolTipProvider(_tourInfoToolTipProvider);
      _tourToolTip.addHideListener(new IToolTipHideListener() {
         @Override
         public void afterHideToolTip(final Event event) {
            // hide hovered image
            _chart.getToolTipControl().afterHideToolTip();
         }
      });

      _chart.setTourInfoIconToolTipProvider(_tourInfoToolTipProvider);
      _tourInfoToolTipProvider.setActionsEnabled(true);

      _chart.addBarSelectionListener(new IBarSelectionListener() {
         @Override
         public void selectionChanged(final int serieIndex, final int valueIndex) {
            if (_statisticData_Training.allTypeIds.length > 0) {

               _selectedTourId = _statisticData_Training.allTourIds[valueIndex];
               _tourInfoToolTipProvider.setTourId(_selectedTourId);

               if (StatisticView.isInUpdateUI()) {

                  /*
                   * Do not fire an event when this is running already in an update event. This
                   * occurs when a tour is modified (marker) in the tourbook view and the stat view
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

            _selectedTourId = _statisticData_Training.allTourIds[valueIndex];
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

                     _selectedTourId = _statisticData_Training.allTourIds[barChartSelection.valueIndex];
                     _tourInfoToolTipProvider.setTourId(_selectedTourId);

                     ActionEditQuick.doAction(StatisticTraining.this);
                  }
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
                                final int serieIndex,
                                int valueIndex) {

      final int[] tourDOYValues = _statisticData_Training.getDoyValues();

      if (valueIndex >= tourDOYValues.length) {
         valueIndex -= tourDOYValues.length;
      }

      if (tourDOYValues == null || valueIndex >= tourDOYValues.length) {
         return;
      }

      final long tourId = _statisticData_Training.allTourIds[valueIndex];

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

      parent.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            _tourInfoUI.dispose();
         }
      });
   }

   /**
    * create data for the x-axis
    */
   void createXData_Day(final ChartDataModel chartModel) {

      final ChartDataXSerie xData = new ChartDataXSerie(_statisticData_Training.getDoyValuesDouble());
      xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_DAY);
//      xData.setVisibleMaxValue(fCurrentYear);
      xData.setChartSegments(createChartSegments(_statisticData_Training));

      chartModel.setXData(xData);
   }

   void createYData_AvgPace(final ChartDataModel chartDataModel, final ChartType chartType) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            chartType,
            _statisticData_Training.allAvgPace_Low,
            _statisticData_Training.allAvgPace_High);

      yData.setYTitle(Messages.LABEL_GRAPH_PACE);
      yData.setUnitLabel(UI.UNIT_LABEL_PACE);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setVisibleMinValue(0);
      yData.setColorIndex(new int[][] { _statisticData_Training.allTypeColorIndices });

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_PACE);

      chartDataModel.addYData(yData);
   }

   void createYData_AvgSpeed(final ChartDataModel chartDataModel, final ChartType chartType) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            chartType,
            _statisticData_Training.allAvgSpeed_Low,
            _statisticData_Training.allAvgSpeed_High);

      yData.setYTitle(Messages.LABEL_GRAPH_SPEED);
      yData.setUnitLabel(UI.UNIT_LABEL_SPEED);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setVisibleMinValue(0);
      yData.setColorIndex(new int[][] { _statisticData_Training.allTypeColorIndices });

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_SPEED);

      chartDataModel.addYData(yData);
   }

   /**
    * Distance
    */
   void createYData_Distance(final ChartDataModel chartModel, final ChartType chartType) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            chartType,
            _statisticData_Training.allDistance_Low,
            _statisticData_Training.allDistance_High);

      yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
      yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
      yData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setVisibleMinValue(0);
      yData.setValueDivisor(1000);
      yData.setColorIndex(new int[][] { _statisticData_Training.allTypeColorIndices });

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_DISTANCE);

      chartModel.addYData(yData);
   }

   /**
    * Duration - Time
    */
   void createYData_Duration(final ChartDataModel chartModel, final ChartType chartType) {

      _yData_Duration = new ChartDataYSerie(
            chartType,
            _statisticData_Training.getDurationLowFloat(),
            _statisticData_Training.getDurationHighFloat());

      _yData_Duration.setYTitle(Messages.LABEL_GRAPH_TIME);
      _yData_Duration.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
      _yData_Duration.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
      _yData_Duration.setAllValueColors(0);
      _yData_Duration.setShowYSlider(true);
      _yData_Duration.setVisibleMinValue(0);
      _yData_Duration.setColorIndex(new int[][] { _statisticData_Training.allTypeColorIndices });

      StatisticServices.setTourTypeColors(_yData_Duration, GraphColorManager.PREF_GRAPH_TIME);

      chartModel.addYData(_yData_Duration);
   }

   /**
    * Elevation loss
    */
   void createYData_ElevationDown(final ChartDataModel chartModel, final ChartType chartType) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            chartType,
            _statisticData_Training.allElevationDown_Low,
            _statisticData_Training.allElevationDown_High);

      yData.setYTitle(Messages.LABEL_GRAPH_ELEVATION_DOWN);
      yData.setUnitLabel(UI.UNIT_LABEL_ELEVATION);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setYAxisDirection(false);
      yData.setVisibleMinValue(0);
      yData.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_ZERO);

      yData.setColorIndex(new int[][] { _statisticData_Training.allTypeColorIndices });

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE);

      chartModel.addYData(yData);
   }

   /**
    * Elevation gain
    */
   void createYData_ElevationUp(final ChartDataModel chartModel, final ChartType chartType) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            chartType,
            _statisticData_Training.allElevationUp_Low,
            _statisticData_Training.allElevationUp_High);

      yData.setYTitle(Messages.LABEL_GRAPH_ELEVATION_UP);
      yData.setUnitLabel(UI.UNIT_LABEL_ELEVATION);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setVisibleMinValue(0);
      yData.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_ZERO);

      yData.setColorIndex(new int[][] { _statisticData_Training.allTypeColorIndices });

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_ALTITUDE);

      chartModel.addYData(yData);
   }

   /**
    * Training effect aerob
    */
   void createYData_TrainingEffect_Aerob(final ChartDataModel chartModel, final ChartType chartType) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            chartType,
            _statisticData_Training.allTraining_Effect_Aerob_Low,
            _statisticData_Training.allTraining_Effect_Aerob_High);

      yData.setYTitle(Messages.LABEL_GRAPH_TRAINING_EFFECT);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setVisibleMinValue(0);
      yData.setColorIndex(new int[][] { _statisticData_Training.allTypeColorIndices });

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TRAINING_EFFECT_AEROB);

      chartModel.addYData(yData);
   }

   /**
    * Training effect anaerobic
    */
   void createYData_TrainingEffect_Anaerob(final ChartDataModel chartModel, final ChartType chartType) {

      final ChartDataYSerie yData = new ChartDataYSerie(
            chartType,
            _statisticData_Training.allTraining_Effect_Anaerob_Low,
            _statisticData_Training.allTraining_Effect_Anaerob_High);

      yData.setYTitle(Messages.LABEL_GRAPH_TRAINING_EFFECT_ANAEROBIC);
      yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      yData.setAllValueColors(0);
      yData.setShowYSlider(true);
      yData.setVisibleMinValue(0);
      yData.setColorIndex(new int[][] { _statisticData_Training.allTypeColorIndices });

      StatisticServices.setTourTypeColors(yData, GraphColorManager.PREF_GRAPH_TRAINING_EFFECT_ANAEROB);

      chartModel.addYData(yData);
   }

   /**
    * Training performance
    */
   void createYData_TrainingPerformance(final ChartDataModel chartModel, final ChartType chartType) {

      _yData_TrainingPerformance = new ChartDataYSerie(
            chartType,
            _statisticData_Training.allTraining_Performance_Low,
            _statisticData_Training.allTraining_Performance_High);

      _yData_TrainingPerformance.setYTitle(Messages.LABEL_GRAPH_TRAINING_PERFORMANCE);
      _yData_TrainingPerformance.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
      _yData_TrainingPerformance.setAllValueColors(0);
      _yData_TrainingPerformance.setShowYSlider(true);
      _yData_TrainingPerformance.setVisibleMinValue(0);
      _yData_TrainingPerformance.setColorIndex(new int[][] { _statisticData_Training.allTypeColorIndices });

      StatisticServices.setTourTypeColors(_yData_TrainingPerformance, GraphColorManager.PREF_GRAPH_TRAINING_PERFORMANCE);

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
      if (_statisticData_Training != null && selection instanceof SelectionBarChart) {

         final int valueIndex = ((SelectionBarChart) selection).valueIndex;

         // check array bounds
         if (valueIndex < _statisticData_Training.allTourIds.length) {
            state.put(STATE_SELECTED_TOUR_ID, Long.toString(_statisticData_Training.allTourIds[valueIndex]));
         }
      }
   }

   @Override
   public boolean selectTour(final Long tourId) {

      final long[] tourIds = _statisticData_Training.allTourIds;
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

   private void setChartProviders(final Chart chartWidget, final ChartDataModel chartModel) {

      // set tool tip info
      chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {

         @Override
         public void createToolTipUI(final IToolTipProvider toolTipProvider, final Composite parent, final int serieIndex, final int valueIndex) {
            StatisticTraining.this.createToolTipUI(toolTipProvider, parent, serieIndex, valueIndex);
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
      _currentYear = statContext.statSelectedYear;
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
            final long[] tourIds = _statisticData_Training.allTourIds;

            if (tourIds.length > 0) {

               if (selectedValueIndex >= tourIds.length) {
                  selectedValueIndex = tourIds.length - 1;
               }

               selectedTourId = tourIds[selectedValueIndex];
            }
         }
      }

      boolean isAvgValue = false;

      DurationTime durationTime = DurationTime.MOVING;

      // set state if average values should be displayed or not, set it BEFORE retrieving data
      if (this instanceof StatisticTraining_Bar) {

         // ensure the data are computed with the correct graph context, otherwise it do not work depending what was previously selected
         _isForceReloadData = true;

         durationTime = (DurationTime) Util.getEnumValue(
               _prefStore.getString(ITourbookPreferences.STAT_TRAINING_BAR_DURATION_TIME),
               DurationTime.MOVING);

         isAvgValue = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE_AVG_VALUE);

         _tourDay_DataProvider.setGraphContext(isAvgValue, false);

      } else if (this instanceof StatisticTraining_Line) {

         // ensure the data are computed with the correct graph context, otherwise it do not work depending what was previously selected
         _isForceReloadData = true;

         durationTime = (DurationTime) Util.getEnumValue(
               _prefStore.getString(ITourbookPreferences.STAT_TRAINING_LINE_DURATION_TIME),
               DurationTime.MOVING);

         isAvgValue = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_LINE_IS_SHOW_TRAINING_PERFORMANCE_AVG_VALUE);

         _tourDay_DataProvider.setGraphContext(isAvgValue, true);
      }

      _statisticData_Training = _tourDay_DataProvider.getDayData(
            statContext.appPerson,
            statContext.appTourTypeFilter,
            statContext.statSelectedYear,
            statContext.statNumberOfYears,
            isDataDirtyWithReset() || statContext.isRefreshData || _isForceReloadData || _isDuration_ReloadData,
            durationTime);

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
