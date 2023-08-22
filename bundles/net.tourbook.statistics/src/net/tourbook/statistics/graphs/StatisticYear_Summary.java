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

import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartType;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.ChartOptions_YearSummary;
import net.tourbook.statistic.SlideoutStatisticOptions;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

public class StatisticYear_Summary extends StatisticYear {

   private IPropertyChangeListener _statYear_PrefChangeListener;

   private boolean                 _isShowDistance;
   private boolean                 _isShowDuration;
   private boolean                 _isShowElevationUp;
   private boolean                 _isShowElevationDown;
   private boolean                 _isShowNumTours;

   private void addPrefListener(final Composite container) {

      // create pref listener
      _statYear_PrefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         // observe changes in stat options
         if (property.equals(ITourbookPreferences.STAT_YEAR_CHART_TYPE)

               || property.equals(ITourbookPreferences.STAT_YEAR_DURATION_TIME)

               || property.equals(ITourbookPreferences.STAT_YEAR_IS_SHOW_DISTANCE)
               || property.equals(ITourbookPreferences.STAT_YEAR_IS_SHOW_DURATION)
               || property.equals(ITourbookPreferences.STAT_YEAR_IS_SHOW_ELEVATION_UP)
               || property.equals(ITourbookPreferences.STAT_YEAR_IS_SHOW_ELEVATION_DOWN)
               || property.equals(ITourbookPreferences.STAT_YEAR_IS_SHOW_NUMBER_OF_TOURS)
               || property.equals(ITourbookPreferences.STAT_YEAR_IS_SHOW_YEAR_SEPARATOR)
         //
         ) {

            if (property.equals(ITourbookPreferences.STAT_YEAR_DURATION_TIME)) {
               _isDuration_ReloadData = true;
            }

            // get the changed preferences
            getPreferences();

            // update chart
            preferencesHasChanged();
         }
      };

      // add pref listener
      _prefStore.addPropertyChangeListener(_statYear_PrefChangeListener);

      // remove pref listener
      container.addDisposeListener(disposeEvent -> _prefStore.removePropertyChangeListener(_statYear_PrefChangeListener));
   }

   @Override
   public void createStatisticUI(final Composite parent, final IViewSite viewSite) {

      super.createStatisticUI(parent, viewSite);

      addPrefListener(parent);
      getPreferences();
   }

   @Override
   protected String getBarOrderingStateKey() {
      return STATE_BAR_ORDERING_YEAR_SUMMARY;
   }

   @Override
   ChartDataModel getChartDataModel() {

      final ChartDataModel chartDataModel = new ChartDataModel(ChartType.BAR);

      createXData_Year(chartDataModel);

      if (_isShowDistance) {
         createYData_Distance(chartDataModel);
      }

      if (_isShowElevationUp) {
         createYData_ElevationUp(chartDataModel);
      }

      if (_isShowElevationDown) {
         createYData_ElevationDown(chartDataModel);
      }

      if (_isShowDuration) {
         createYData_Duration(chartDataModel);
      }

      if (_isShowNumTours) {
         createYData_NumTours(chartDataModel);
      }

      return chartDataModel;
   }

   @Override
   protected String getGridPrefPrefix() {
      return GRID_YEAR_SUMMARY;
   }

   private void getPreferences() {

      _isShowDistance = _prefStore.getBoolean(ITourbookPreferences.STAT_YEAR_IS_SHOW_DISTANCE);
      _isShowDuration = _prefStore.getBoolean(ITourbookPreferences.STAT_YEAR_IS_SHOW_DURATION);
      _isShowElevationUp = _prefStore.getBoolean(ITourbookPreferences.STAT_YEAR_IS_SHOW_ELEVATION_UP);
      _isShowElevationDown = _prefStore.getBoolean(ITourbookPreferences.STAT_YEAR_IS_SHOW_ELEVATION_DOWN);
      _isShowNumTours = _prefStore.getBoolean(ITourbookPreferences.STAT_YEAR_IS_SHOW_NUMBER_OF_TOURS);
   }

   @Override
   protected void setupStatisticSlideout(final SlideoutStatisticOptions slideout) {

      slideout.setStatisticOptions(new ChartOptions_YearSummary());
   }
}
