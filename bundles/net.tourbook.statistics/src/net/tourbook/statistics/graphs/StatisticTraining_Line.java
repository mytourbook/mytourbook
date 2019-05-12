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

public class StatisticTraining_Line extends StatisticTraining {

   private final IPreferenceStore  _prefStore = TourbookPlugin.getPrefStore();
   private IPropertyChangeListener _prefChangeListener;

   private boolean                 _isShowTrainingEffect;
   private boolean                 _isShowTrainingEffect_Anaerobic;
   private boolean                 _isShowTrainingPerformance;

   private void addPrefListener(final Composite container) {

      // create pref listener
      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            // observe which data are displayed
            if (property.equals(ITourbookPreferences.STAT_TRAINING_LINE_IS_SHOW_TRAINING_EFFECT)
                  || property.equals(ITourbookPreferences.STAT_TRAINING_LINE_IS_SHOW_TRAINING_EFFECT_ANAEROBIC)
                  || property.equals(ITourbookPreferences.STAT_TRAINING_LINE_IS_SHOW_TRAINING_PERFORMANCE)) {

               // get the changed preferences
               getPreferences();

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

      final ChartDataModel chartDataModel = new ChartDataModel(ChartType.LINE);

      createXData_Day(chartDataModel);

      if (_isShowTrainingEffect) {
         createYData_TrainingEffect(chartDataModel, ChartType.LINE);
      }

      if (_isShowTrainingEffect_Anaerobic) {
         createYData_TrainingEffect_Anaerobic(chartDataModel, ChartType.LINE);
      }

      if (_isShowTrainingPerformance) {
         createYData_TrainingPerformance(chartDataModel, ChartType.LINE);
      }

      return chartDataModel;
   }

   @Override
   protected String getGridPrefPrefix() {
      return GRID_TRAINING_LINE;
   }

   private void getPreferences() {

      _isShowTrainingEffect = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_LINE_IS_SHOW_TRAINING_EFFECT);
      _isShowTrainingEffect_Anaerobic = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_LINE_IS_SHOW_TRAINING_EFFECT_ANAEROBIC);
      _isShowTrainingPerformance = _prefStore.getBoolean(ITourbookPreferences.STAT_TRAINING_LINE_IS_SHOW_TRAINING_PERFORMANCE);
   }

   @Override
   protected void setupStatisticSlideout(final SlideoutStatisticOptions slideout) {

      slideout.setStatisticOptions(new ChartOptions_Training(
            ITourbookPreferences.STAT_TRAINING_LINE_IS_SHOW_TRAINING_EFFECT,
            ITourbookPreferences.STAT_TRAINING_LINE_IS_SHOW_TRAINING_EFFECT_ANAEROBIC,
            ITourbookPreferences.STAT_TRAINING_LINE_IS_SHOW_TRAINING_PERFORMANCE));
   }
}
