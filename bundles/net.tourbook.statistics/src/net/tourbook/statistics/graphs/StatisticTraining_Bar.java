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

import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartType;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.ChartOptions_Training;
import net.tourbook.statistic.SlideoutStatisticOptions;
import net.tourbook.statistic.TrainingPrefKeys;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

public class StatisticTraining_Bar extends StatisticTraining {

   private IPropertyChangeListener _prefChangeListener;

   private boolean                 _isShow_Avg_Pace;
   private boolean                 _isShow_Avg_Speed;
   private boolean                 _isShow_Distance;
   private boolean                 _isShow_Duration;
   private boolean                 _isShow_ElevationUp;
   private boolean                 _isShow_ElevationDown;
   private boolean                 _isShow_TrainingEffect;
   private boolean                 _isShow_TrainingEffect_Anaerob;
   private boolean                 _isShow_TrainingPerformance;

   private void addPrefListener(final Composite container) {

      // create pref listener
      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         // observe which data are displayed
         if (property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT)
               || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT_ANAEROBIC)
               || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE)
               || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE_AVG_VALUE)

               || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_AVG_PACE)
               || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_AVG_SPEED)
               || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_DISTANCE)
               || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_DURATION)
               || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_ELEVATION_UP)
               || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_ELEVATION_DOWN)

               || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_DURATION_TIME)

         ) {

            if (property.equals(ITourbookPreferences.STAT_TRAINING_BAR_DURATION_TIME)) {
               _isDuration_ReloadData = true;
            }

            // get the changed preferences
            getPreferences();

            // data needs to be recalculated
            if (property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE_AVG_VALUE)) {
               setIsForceReloadData(true);
            }

            // update chart
            preferencesHasChanged();
         }
      };

      // add pref listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);

      // remove pref listener
      container.addDisposeListener(disposeEvent -> _prefStore.removePropertyChangeListener(_prefChangeListener));
   }

   @Override
   public void createStatisticUI(final Composite parent, final IViewSite viewSite) {

      super.createStatisticUI(parent, viewSite);

      addPrefListener(parent);
      getPreferences();
   }

   @Override
   ChartDataModel getChartDataModel() {

      final ChartDataModel chartDataModel = new ChartDataModel(ChartType.BAR);

      createXData_Day(chartDataModel);

      if (_isShow_TrainingEffect) {
         createYData_TrainingEffect_Aerob(chartDataModel, ChartType.BAR);
      }

      if (_isShow_TrainingEffect_Anaerob) {
         createYData_TrainingEffect_Anaerob(chartDataModel, ChartType.BAR);
      }

      if (_isShow_TrainingPerformance) {
         createYData_TrainingPerformance(chartDataModel, ChartType.BAR);
      }

      if (_isShow_Distance) {
         createYData_Distance(chartDataModel, ChartType.BAR);
      }

      if (_isShow_ElevationUp) {
         createYData_ElevationUp(chartDataModel, ChartType.BAR);
      }

      if (_isShow_ElevationDown) {
         createYData_ElevationDown(chartDataModel, ChartType.BAR);
      }

      if (_isShow_Duration) {
         createYData_Duration(chartDataModel, ChartType.BAR);
      }

      if (_isShow_Avg_Pace) {
         createYData_AvgPace(chartDataModel, ChartType.BAR);
      }

      if (_isShow_Avg_Speed) {
         createYData_AvgSpeed(chartDataModel, ChartType.BAR);
      }

      return chartDataModel;
   }

   @Override
   protected String getGridPrefPrefix() {
      return GRID_TRAINING_BAR;
   }

   private void getPreferences() {

// SET_FORMATTING_OFF

      _isShow_Avg_Pace                 = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_AVG_PACE);
      _isShow_Avg_Speed                = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_AVG_SPEED);
      _isShow_Distance                 = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_DISTANCE);
      _isShow_Duration                 = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_DURATION);
      _isShow_ElevationUp              = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_ELEVATION_UP);
      _isShow_ElevationDown            = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_ELEVATION_DOWN);

      _isShow_TrainingEffect           = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT);
      _isShow_TrainingEffect_Anaerob   = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT_ANAEROBIC);
      _isShow_TrainingPerformance      = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE);
   }

   @Override
   protected void setupStatisticSlideout(final SlideoutStatisticOptions slideout) {

      final TrainingPrefKeys prefKeys = new TrainingPrefKeys();

      prefKeys.isShow_Avg_Pace      = ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_AVG_PACE;
      prefKeys.isShow_Avg_Speed     = ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_AVG_SPEED;
      prefKeys.isShow_Distance      = ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_DISTANCE;
      prefKeys.isShow_Duration      = ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_DURATION;
      prefKeys.isShow_ElevationUp   = ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_ELEVATION_UP;
      prefKeys.isShow_ElevationDown = ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_ELEVATION_DOWN;

      prefKeys.isShow_TrainingEffect               = ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT;
      prefKeys.isShow_TrainingEffect_Anaerobic     = ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT_ANAEROBIC;
      prefKeys.isShow_TrainingPerformance          = ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE;
      prefKeys.isShow_TrainingPerformance_AvgValue = ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE_AVG_VALUE;

      prefKeys.durationTime         = ITourbookPreferences.STAT_TRAINING_BAR_DURATION_TIME;

// SET_FORMATTING_ON

      slideout.setStatisticOptions(new ChartOptions_Training(prefKeys));
   }
}
