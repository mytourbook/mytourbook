/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartType;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.ChartOptions_Training;
import net.tourbook.statistic.SlideoutStatisticOptions;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

public class StatisticTraining_Bar extends StatisticTraining {

   private final IPreferenceStore  _prefStore = TourbookPlugin.getPrefStore();
   private IPropertyChangeListener _prefChangeListener;

   private boolean                 _isShow_Altitude;
   private boolean                 _isShow_Avg_Pace;
   private boolean                 _isShow_Avg_Speed;
   private boolean                 _isShow_Distance;
   private boolean                 _isShow_Duration;
   private boolean                 _isShow_TrainingEffect;
   private boolean                 _isShow_TrainingEffect_Anaerob;
   private boolean                 _isShow_TrainingPerformance;

   private void addPrefListener(final Composite container) {

      // create pref listener
      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            // observe which data are displayed
            if (property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT)
                  || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT_ANAEROBIC)
                  || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE)
                  || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE_AVG_VALUE)

                  || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_ALTITUDE)
                  || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_AVG_PACE)
                  || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_AVG_SPEED)
                  || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_DISTANCE)
                  || property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_DURATION)

            ) {

               // get the changed preferences
               getPreferences();

               // data needs to be recalculated
               if (property.equals(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE_AVG_VALUE)) {
                  setIsForceReloadData(true);
               }

               // update chart
               preferencesHasChanged();
            }
         }
      };

      // add pref listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);

      // remove pref listener
      container.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            _prefStore.removePropertyChangeListener(_prefChangeListener);
         }
      });
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

      if (_isShow_Altitude) {
         createYData_Altitude(chartDataModel, ChartType.BAR);
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

      _isShow_Altitude                       = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_ALTITUDE);
      _isShow_Avg_Pace                       = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_AVG_PACE);
      _isShow_Avg_Speed                      = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_AVG_SPEED);
      _isShow_Distance                       = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_DISTANCE);
      _isShow_Duration                       = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_DURATION);

      _isShow_TrainingEffect                 = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT);
      _isShow_TrainingEffect_Anaerob         = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT_ANAEROBIC);
      _isShow_TrainingPerformance            = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE);

// SET_FORMATTING_ON
   }

   @Override
   protected void setupStatisticSlideout(final SlideoutStatisticOptions slideout) {

      slideout.setStatisticOptions(new ChartOptions_Training(

            ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_ALTITUDE,
            ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_AVG_PACE,
            ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_AVG_SPEED,
            ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_DISTANCE,
            ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_DURATION,

            ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT,
            ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT_ANAEROBIC,
            ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE,
            ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE_AVG_VALUE

      ));
   }
}
