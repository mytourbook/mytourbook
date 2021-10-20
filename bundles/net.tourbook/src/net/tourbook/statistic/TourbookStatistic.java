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
package net.tourbook.statistic;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.ChartOptions_Grid;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

/**
 * Plugin interface for statistics in MyTourbook
 */
public abstract class TourbookStatistic {

   protected static final String STATE_SELECTED_TOUR_ID                     = "STATE_SELECTED_TOUR_ID";                     //$NON-NLS-1$

   protected static final String STATE_BAR_ORDERING_MONTH_ALTITUDE          = "STATE_BAR_ORDERING_MONTH_ALTITUDE";          //$NON-NLS-1$
   protected static final String STATE_BAR_ORDERING_MONTH_DISTANCE          = "STATE_BAR_ORDERING_MONTH_DISTANCE";          //$NON-NLS-1$
   protected static final String STATE_BAR_ORDERING_MONTH_SUMMARY           = "STATE_BAR_ORDERING_MONTH_SUMMARY";           //$NON-NLS-1$
   protected static final String STATE_BAR_ORDERING_MONTH_TIME              = "STATE_BAR_ORDERING_MONTH_TIME";              //$NON-NLS-1$
   protected static final String STATE_BAR_ORDERING_MONTH_ATHLETEDATA       = "STATE_BAR_ORDERING_MONTH_ATHLETEDATA";       //$NON-NLS-1$

   protected static final String STATE_BAR_ORDERING_YEAR_ALTITUDE           = "STATE_BAR_ORDERING_YEAR_ALTITUDE";           //$NON-NLS-1$
   protected static final String STATE_BAR_ORDERING_YEAR_DISTANCE           = "STATE_BAR_ORDERING_YEAR_DISTANCE";           //$NON-NLS-1$
   protected static final String STATE_BAR_ORDERING_YEAR_SUMMARY            = "STATE_BAR_ORDERING_YEAR_SUMMARY";            //$NON-NLS-1$
   protected static final String STATE_BAR_ORDERING_YEAR_TIME               = "STATE_BAR_ORDERING_YEAR_TIME";               //$NON-NLS-1$
   protected static final String STATE_BAR_ORDERING_YEAR_ATHLETEDATA        = "STATE_BAR_ORDERING_YEAR_ATHLETEDATA";        //$NON-NLS-1$

   protected static final String STATE_BAR_ORDERING_HR_ZONE_START_FOR_MONTH = "STATE_BAR_ORDERING_HR_ZONE_START_FOR_MONTH"; ////$NON-NLS-1$

   /*
    * Grid prefixes
    */
   protected static final String    GRID_BATTERY           = "GRID_BATTERY__";              //$NON-NLS-1$

   protected static final String    GRID_DAY_ALTITUDE      = "GRID_DAY_ALTITUDE__";         //$NON-NLS-1$
   protected static final String    GRID_DAY_DISTANCE      = "GRID_DAY_DISTANCE__";         //$NON-NLS-1$
   protected static final String    GRID_DAY_SUMMARY       = "GRID_DAY_SUMMARY__";          //$NON-NLS-1$
   protected static final String    GRID_DAY_TIME          = "GRID_DAY_TIME__";             //$NON-NLS-1$
   protected static final String    GRID_DAY_ATHLETEDATA   = "GRID_DAY_ATHLETEDATA__";      //$NON-NLS-1$

   protected static final String    GRID_WEEK_ALTITUDE     = "GRID_WEEK_ALTITUDE__";        //$NON-NLS-1$
   protected static final String    GRID_WEEK_DISTANCE     = "GRID_WEEK_DISTANCE__";        //$NON-NLS-1$
   protected static final String    GRID_WEEK_SUMMARY      = "GRID_WEEK_SUMMARY__";         //$NON-NLS-1$
   protected static final String    GRID_WEEK_TIME         = "GRID_WEEK_TIME__";            //$NON-NLS-1$
   protected static final String    GRID_WEEK_ATHLETEDATA  = "GRID_WEEK_ATHLETEDATA__";     //$NON-NLS-1$

   protected static final String    GRID_MONTH_ALTITUDE    = "GRID_MONTH_ALTITUDE__";       //$NON-NLS-1$
   protected static final String    GRID_MONTH_DISTANCE    = "GRID_MONTH_DISTANCE__";       //$NON-NLS-1$
   protected static final String    GRID_MONTH_SUMMARY     = "GRID_MONTH_SUMMARY__";        //$NON-NLS-1$
   protected static final String    GRID_MONTH_TIME        = "GRID_MONTH_TIME__";           //$NON-NLS-1$
   protected static final String    GRID_MONTH_ATHLETEDATA = "GRID_MONTH_ATHLETEDATA__";    //$NON-NLS-1$

   protected static final String    GRID_YEAR_ALTITUDE     = "GRID_YEAR_ALTITUDE__";        //$NON-NLS-1$
   protected static final String    GRID_YEAR_DISTANCE     = "GRID_YEAR_DISTANCE__";        //$NON-NLS-1$
   protected static final String    GRID_YEAR_SUMMARY      = "GRID_YEAR_SUMMARY__";         //$NON-NLS-1$
   protected static final String    GRID_YEAR_TIME         = "GRID_YEAR_TIME__";            //$NON-NLS-1$
   protected static final String    GRID_YEAR_ATHLETEDATA  = "GRID_YEAR_ATHLETEDATA__";     //$NON-NLS-1$

   protected static final String    GRID_WEEK_HR_ZONE      = "GRID_WEEK_HR_ZONE__";         //$NON-NLS-1$
   protected static final String    GRID_MONTH_HR_ZONE     = "GRID_MONTH_HR_ZONE__";        //$NON-NLS-1$

   protected static final String    GRID_TOUR_FREQUENCY    = "GRID_TOUR_FREQUENCY__";       //$NON-NLS-1$
   protected static final String    GRID_TOUR_TIME         = "GRID_TOUR_TIME__";            //$NON-NLS-1$

   protected static final String    GRID_TRAINING_BAR      = "GRID_TRAINING_BAR__";         //$NON-NLS-1$
   protected static final String    GRID_TRAINING_LINE     = "GRID_TRAINING_LINE__";        //$NON-NLS-1$

   /** ID from plugin.xml */
   public String                    plugin_StatisticId;

   /** Name from plugin.xml */
   public String                    plugin_VisibleName;

   /** Data category from plugin.xml */
   public String                    plugin_Category_Data;

   /** Time category from plugin.xml */
   public String                    plugin_Category_Time;

   private boolean                  _isDataDirty;

   protected final IPreferenceStore _prefStore             = TourbookPlugin.getPrefStore();
   protected final IPreferenceStore _prefStore_Common      = CommonActivator.getPrefStore();

   private IPropertyChangeListener  _prefChangeListener;

   private boolean                  _isInPrefUpdate;

   protected boolean                _isDuration_ReloadData;

   /*
    * UI controls
    */
   private Composite _container;

   /**
    * Add the pref listener which is called when the color was changed
    */
   private void addPrefListener() {

      final String gridPrefix = getGridPrefPrefix();

      final String gridHDistance = gridPrefix + ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE;
      final String gridVDistance = gridPrefix + ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE;
      final String gridIsHGridline = gridPrefix + ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES;
      final String gridIsVGridline = gridPrefix + ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES;

      // create pref listener
      _prefChangeListener = propertyChangeEvent -> {
         final String property = propertyChangeEvent.getProperty();

         // test if the color or statistic data have changed
         if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)
               //
               || property.equals(gridHDistance)
               || property.equals(gridVDistance)
               || property.equals(gridIsHGridline)
               || property.equals(gridIsVGridline)

               || property.equals(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR)
               || property.equals(ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR)
               || property.equals(ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR_DARK)
         //
         ) {

            _isInPrefUpdate = true;
            {
               // update chart
               preferencesHasChanged();
            }
            _isInPrefUpdate = false;
         }
      };

      // add pref listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   /**
    * Create the statistic UI component.
    *
    * @param parent
    * @param viewSite
    * @param postSelectionProvider
    */
   protected abstract void createStatisticUI(Composite parent, IViewSite viewSite);

   public void createUI(final Composite parent, final IViewSite viewSite) {

      _container = parent;

      createStatisticUI(parent, viewSite);

      addPrefListener();
   }

   /**
    * Disposes of the statistic
    */
   public void dispose() {

      if (_prefChangeListener != null) {

         _prefStore.removePropertyChangeListener(_prefChangeListener);
      }

      if (_container != null && _container.isDisposed() == false) {

         _container.dispose();
      }

      // !!! null is checked outside of this class !!!
      _container = null;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof TourbookStatistic)) {
         return false;
      }
      final TourbookStatistic other = (TourbookStatistic) obj;
      if (plugin_StatisticId == null) {
         if (other.plugin_StatisticId != null) {
            return false;
         }
      } else if (!plugin_StatisticId.equals(other.plugin_StatisticId)) {
         return false;
      }
      return true;
   }

   /**
    * Convert 'old' chart type format into 'new' format.
    *
    * @param _chartType
    * @return
    */
   protected int getChartType(final String _chartType) {

      switch (_chartType) {

      case ChartDataSerie.CHART_TYPE_BAR_ADJACENT:
         return ChartDataYSerie.BAR_LAYOUT_BESIDE;

      case ChartDataSerie.CHART_TYPE_BAR_STACKED:
         return ChartDataYSerie.BAR_LAYOUT_STACKED;
      }

      return ChartDataYSerie.BAR_LAYOUT_STACKED;
   }

   /**
    * @return Returns {@link ChartOptions_Grid#GRID_ALL} to enable all grid options, this can be
    *         overwritten to enable only a part of the grid options.
    */
   public int getEnabledGridOptions() {

      return ChartOptions_Grid.GRID_ALL;
   }

   /**
    * @return Returns a prefix which is used to access the grid preferences in the pref store.
    */
   protected abstract String getGridPrefPrefix();

   /**
    * @param isShowSequenceNumbers
    *           Show sequence numbers in the first column
    * @return Returns the statistic values, these values are created on demand because they can use
    *         some 100 ms, depending on the statistic.
    */
   public abstract String getRawStatisticValues(boolean isShowSequenceNumbers);

   protected RGB getRgbGraph_Text_4_Time() {

      final String prefColorName_Time = ICommonPreferences.GRAPH_COLORS + GraphColorManager.PREF_GRAPH_TIME + UI.SYMBOL_DOT;

      final String prefColorTextThemed = UI.IS_DARK_THEME
            ? GraphColorManager.PREF_COLOR_TEXT_DARK
            : GraphColorManager.PREF_COLOR_TEXT_LIGHT;

      return PreferenceConverter.getColor(_prefStore_Common, prefColorName_Time + prefColorTextThemed);
   }

   /**
    * @return When a tour can be selected in the statistic, this will return the tour Id of the
    *         selected tour or <code>null</code> otherwise.
    */
   public Long getSelectedTour() {
      return null;
   }

   public Composite getUIControl() {
      return _container;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((plugin_StatisticId == null) ? 0 : plugin_StatisticId.hashCode());
      return result;
   }

   /**
    * @return Returns the status if the tour data for the statistic is dirty and resets the status
    *         to <code>false</code>
    */
   protected boolean isDataDirtyWithReset() {

      final boolean isDataDirty = _isDataDirty;
      _isDataDirty = false;

      return isDataDirty;
   }

   public boolean isInPreferencesUpdate() {
      return _isInPrefUpdate;
   }

   /**
    * Preferences for the statistics has changed.
    */
   public abstract void preferencesHasChanged();

   /**
    * Restores the state from a memento (e.g. select previous selection), default does nothing
    *
    * @param state
    */
   public void restoreState(final IDialogSettings state) {
      // do nothing
   }

   /**
    * Restore state after the controls is created.
    *
    * @param state
    */
   public void restoreStateEarly(final IDialogSettings state) {
      // do nothing
   }

   /**
    * Saves the state of the statistic into a state, default does nothing
    *
    * @param viewState
    */
   public void saveState(final IDialogSettings viewState) {
      // do nothing
   }

   /**
    * Set the bar vertical order in the UI.
    *
    * @param selectedIndex
    *           Combobox selection index, can be <code>-1</code> when combobox is empty.
    */
   public void setBarVerticalOrder(final int selectedIndex) {
      // do nothing
   }

   /**
    * Set statistic data dirty that they must be reloaded when the chart is displayed the next
    * time.
    */
   public void setDataDirty() {
      _isDataDirty = true;
   }

   protected void setGraphLabel_Duration(final ChartDataYSerie yData_Duration, final DurationTime durationTime) {

      if (durationTime == DurationTime.BREAK) {

         yData_Duration.setYTitle(Messages.Graph_Label_Time_Break);

      } else if (durationTime == DurationTime.ELAPSED) {

         yData_Duration.setYTitle(Messages.Graph_Label_Time_Elapsed);

      } else if (durationTime == DurationTime.PAUSED) {

         yData_Duration.setYTitle(Messages.Graph_Label_Time_Paused);

      } else if (durationTime == DurationTime.RECORDED) {

         yData_Duration.setYTitle(Messages.Graph_Label_Time_Recorded);

      } else {

         // durationTime == DurationTime.MOVING, this is the default

         yData_Duration.setYTitle(Messages.Graph_Label_Time_Moving);
      }
   }

   /**
    * Set the state if the scale for the chart is synched for different data (e.g. years)
    *
    * @param isEnabled
    *           <code>true</code> when the synch is enabled, <code>false</code> when it's disabled
    */
   public abstract void setSynchScale(boolean isEnabled);

   /**
    * Set additional options in the slideout.
    *
    * @param slideout
    */
   protected void setupStatisticSlideout(final SlideoutStatisticOptions slideout) {

      // reset previous options

      slideout.setStatisticOptions(null);
   }

   @Override
   public String toString() {
      return "TourbookStatistic ["// //$NON-NLS-1$
            + ("statisticId=" + plugin_StatisticId + ", ")// //$NON-NLS-1$ //$NON-NLS-2$
            + ("visibleName=" + plugin_VisibleName) //$NON-NLS-1$
            + "]"; //$NON-NLS-1$
   }

   /**
    * Update statistic with the data in the context.
    *
    * @param statContext
    */
   public abstract void updateStatistic(StatisticContext statContext);

   /**
    * This method is called before the statistic control will be displayed. When the toolbar
    * manager is used, this method should put the actions into the toolbar manager
    */
   public abstract void updateToolBar();

}
