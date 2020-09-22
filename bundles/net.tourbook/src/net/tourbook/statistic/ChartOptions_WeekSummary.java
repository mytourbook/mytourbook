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
package net.tourbook.statistic;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;

public class ChartOptions_WeekSummary implements IStatisticOptions {

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private SelectionAdapter       _defaultSelectionListener;

   /*
    * UI controls
    */
   private Button _chkShowAltitude;
   private Button _chkShowDistance;
   private Button _chkShowDuration;
   private Button _chkShowNumberOfTours;

   private Button _rdoChartType_BarAdjacent;
   private Button _rdoChartType_BarStacked;
   private Button _rdoDuration_ElapsedTime;
   private Button _rdoDuration_RecordedTime;
   private Button _rdoDuration_PausedTime;
   private Button _rdoDuration_MovingTime;
   private Button _rdoDuration_BreakTime;

   @Override
   public void createUI(final Composite parent) {

      initUI(parent);

      createUI_100_Graphs(parent);
      createUI_200_ChartType(parent);
   }

   private void createUI_100_Graphs(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
//      group.setText(Messages.Pref_Graphs_Group_Grid);
      group.setText(Messages.Pref_Statistic_Group_WeekSummary);
      GridDataFactory
            .fillDefaults()//
            .grab(true, false)
            .span(2, 1)
            .applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
//      group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         createUI_110_Left(group);
      }
   }

   private void createUI_110_Left(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         {
            /*
             * Show distance
             */
            _chkShowDistance = new Button(container, SWT.CHECK);
            _chkShowDistance.setText(Messages.Pref_Statistic_Checkbox_Distance);
            _chkShowDistance.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show altitude
             */
            _chkShowAltitude = new Button(container, SWT.CHECK);
            _chkShowAltitude.setText(Messages.Pref_Statistic_Checkbox_Altitude);
            _chkShowAltitude.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show time
             */
            _chkShowDuration = new Button(container, SWT.CHECK);
            _chkShowDuration.setText(Messages.Pref_Statistic_Checkbox_Duration);
            _chkShowDuration.addSelectionListener(_defaultSelectionListener);

            /*
             * Moving, elapsed + break time
             */
            final Composite timeContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .indent(16, 0)
                  .applyTo(timeContainer);
            GridLayoutFactory.fillDefaults().numColumns(1).applyTo(timeContainer);
            {
               {
                  /*
                   * Elapsed time
                   */
                  _rdoDuration_ElapsedTime = new Button(timeContainer, SWT.RADIO);
                  _rdoDuration_ElapsedTime.setText(Messages.Pref_Statistic_Radio_Duration_ElapsedTime);
                  _rdoDuration_ElapsedTime.addSelectionListener(_defaultSelectionListener);
               }
               {
                  /*
                   * Recorded time
                   */
                  _rdoDuration_RecordedTime = new Button(timeContainer, SWT.RADIO);
                  _rdoDuration_RecordedTime.setText(Messages.Pref_Statistic_Radio_Duration_RecordedTime);
                  _rdoDuration_RecordedTime.addSelectionListener(_defaultSelectionListener);
               }
               {
                  /*
                   * Paused time
                   */
                  _rdoDuration_PausedTime = new Button(timeContainer, SWT.RADIO);
                  _rdoDuration_PausedTime.setText(Messages.Pref_Statistic_Radio_Duration_PausedTime);
                  _rdoDuration_PausedTime.addSelectionListener(_defaultSelectionListener);
               }
               {
                  /*
                   * Moving time
                   */
                  _rdoDuration_MovingTime = new Button(timeContainer, SWT.RADIO);
                  _rdoDuration_MovingTime.setText(Messages.Pref_Statistic_Radio_Duration_MovingTime);
                  _rdoDuration_MovingTime.addSelectionListener(_defaultSelectionListener);
               }
               {
                  /*
                   * Break time
                   */
                  _rdoDuration_BreakTime = new Button(timeContainer, SWT.RADIO);
                  _rdoDuration_BreakTime.setText(Messages.Pref_Statistic_Radio_Duration_BreakTime);
                  _rdoDuration_BreakTime.addSelectionListener(_defaultSelectionListener);
               }
            }
         }
         {
            /*
             * Show number of tours
             */
            _chkShowNumberOfTours = new Button(container, SWT.CHECK);
            _chkShowNumberOfTours.setText(Messages.Pref_Statistic_Checkbox_NumberOfTours);
            _chkShowNumberOfTours.addSelectionListener(_defaultSelectionListener);
         }
      }
   }

   private void createUI_200_ChartType(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
//      group.setText(Messages.Pref_Graphs_Group_Grid);
      group.setText(Messages.Pref_Statistic_Group_ChartType);
      GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//      group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * Bar adjacent
             */
            _rdoChartType_BarAdjacent = new Button(group, SWT.RADIO);
            _rdoChartType_BarAdjacent.setText(Messages.Pref_Statistic_Radio_BarAdjacent);
            _rdoChartType_BarAdjacent.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Bar adjacent
             */
            _rdoChartType_BarStacked = new Button(group, SWT.RADIO);
            _rdoChartType_BarStacked.setText(Messages.Pref_Statistic_Radio_BarStacked);
            _rdoChartType_BarStacked.addSelectionListener(_defaultSelectionListener);
         }
      }
   }

   private void enableControls() {

      final boolean isShowDuration = _chkShowDuration.getSelection();

      _rdoDuration_MovingTime.setEnabled(isShowDuration);
      _rdoDuration_BreakTime.setEnabled(isShowDuration);
      _rdoDuration_ElapsedTime.setEnabled(isShowDuration);
      _rdoDuration_RecordedTime.setEnabled(isShowDuration);
      _rdoDuration_PausedTime.setEnabled(isShowDuration);
   }

   private void initUI(final Composite parent) {

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
      };
   }

   private void onChangeUI() {

      // update chart async (which is done when a pref store value is modified) that the UI is updated immediately

      enableControls();

      Display.getCurrent().asyncExec(new Runnable() {
         @Override
         public void run() {

            saveState();
         }
      });
   }

   @Override
   public void resetToDefaults() {

      _chkShowAltitude.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.STAT_WEEK_IS_SHOW_ALTITUDE));
      _chkShowDistance.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.STAT_WEEK_IS_SHOW_DISTANCE));
      _chkShowDuration.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.STAT_WEEK_IS_SHOW_DURATION));
      _chkShowNumberOfTours.setSelection(//
            _prefStore.getDefaultBoolean(ITourbookPreferences.STAT_WEEK_IS_SHOW_NUMBER_OF_TOURS));

      final String chartType = _prefStore.getDefaultString(ITourbookPreferences.STAT_WEEK_CHART_TYPE);
      _rdoChartType_BarAdjacent.setSelection(chartType.equals(ChartDataSerie.CHART_TYPE_BAR_ADJACENT));
      _rdoChartType_BarStacked.setSelection(chartType.equals(ChartDataSerie.CHART_TYPE_BAR_STACKED));

      final Enum<DurationTime> durationTime = Util.getEnumValue(
            _prefStore.getDefaultString(ITourbookPreferences.STAT_WEEK_DURATION_TIME),
            DurationTime.MOVING);
      _rdoDuration_BreakTime.setSelection(durationTime.equals(DurationTime.BREAK));
      _rdoDuration_MovingTime.setSelection(durationTime.equals(DurationTime.MOVING));
      _rdoDuration_ElapsedTime.setSelection(durationTime.equals(DurationTime.ELAPSED));
      _rdoDuration_RecordedTime.setSelection(durationTime.equals(DurationTime.RECORDED));
      _rdoDuration_PausedTime.setSelection(durationTime.equals(DurationTime.PAUSED));

      enableControls();
   }

   @Override
   public void restoreState() {

      _chkShowAltitude.setSelection(_prefStore.getBoolean(ITourbookPreferences.STAT_WEEK_IS_SHOW_ALTITUDE));
      _chkShowDistance.setSelection(_prefStore.getBoolean(ITourbookPreferences.STAT_WEEK_IS_SHOW_DISTANCE));
      _chkShowDuration.setSelection(_prefStore.getBoolean(ITourbookPreferences.STAT_WEEK_IS_SHOW_DURATION));
      _chkShowNumberOfTours.setSelection(//
            _prefStore.getBoolean(ITourbookPreferences.STAT_WEEK_IS_SHOW_NUMBER_OF_TOURS));

      final String chartType = _prefStore.getString(ITourbookPreferences.STAT_WEEK_CHART_TYPE);
      _rdoChartType_BarAdjacent.setSelection(chartType.equals(ChartDataSerie.CHART_TYPE_BAR_ADJACENT));
      _rdoChartType_BarStacked.setSelection(chartType.equals(ChartDataSerie.CHART_TYPE_BAR_STACKED));

      final Enum<DurationTime> durationTime = Util.getEnumValue(
            _prefStore.getString(ITourbookPreferences.STAT_WEEK_DURATION_TIME),
            DurationTime.MOVING);
      _rdoDuration_BreakTime.setSelection(durationTime.equals(DurationTime.BREAK));
      _rdoDuration_MovingTime.setSelection(durationTime.equals(DurationTime.MOVING));
      _rdoDuration_ElapsedTime.setSelection(durationTime.equals(DurationTime.ELAPSED));
      _rdoDuration_RecordedTime.setSelection(durationTime.equals(DurationTime.RECORDED));
      _rdoDuration_PausedTime.setSelection(durationTime.equals(DurationTime.PAUSED));

      enableControls();
   }

   @Override
   public void saveState() {

      _prefStore.setValue(ITourbookPreferences.STAT_WEEK_IS_SHOW_ALTITUDE, _chkShowAltitude.getSelection());
      _prefStore.setValue(ITourbookPreferences.STAT_WEEK_IS_SHOW_DISTANCE, _chkShowDistance.getSelection());
      _prefStore.setValue(ITourbookPreferences.STAT_WEEK_IS_SHOW_DURATION, _chkShowDuration.getSelection());
      _prefStore.setValue(//
            ITourbookPreferences.STAT_WEEK_IS_SHOW_NUMBER_OF_TOURS,
            _chkShowNumberOfTours.getSelection());

      _prefStore.setValue(
            ITourbookPreferences.STAT_WEEK_CHART_TYPE,
            _rdoChartType_BarAdjacent.getSelection()
                  ? ChartDataSerie.CHART_TYPE_BAR_ADJACENT
                  : ChartDataSerie.CHART_TYPE_BAR_STACKED);

      // duration time
      String selectedDurationType = UI.EMPTY_STRING;

      if (_rdoDuration_BreakTime.getSelection()) {
         selectedDurationType = DurationTime.BREAK.name();
      } else if (_rdoDuration_MovingTime.getSelection()) {
         selectedDurationType = DurationTime.MOVING.name();
      } else if (_rdoDuration_RecordedTime.getSelection()) {
         selectedDurationType = DurationTime.RECORDED.name();
      } else if (_rdoDuration_PausedTime.getSelection()) {
         selectedDurationType = DurationTime.PAUSED.name();
      } else if (_rdoDuration_ElapsedTime.getSelection()) {
         selectedDurationType = DurationTime.ELAPSED.name();
      }

      _prefStore.setValue(ITourbookPreferences.STAT_WEEK_DURATION_TIME, selectedDurationType);
   }
}
