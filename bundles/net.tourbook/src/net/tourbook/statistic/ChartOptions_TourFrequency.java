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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

public class ChartOptions_TourFrequency implements IStatisticOptions {

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private SelectionListener      _defaultSelectionListener;
   private MouseWheelListener     _defaultMouseWheelListener;

   /*
    * UI controls
    */
   private Button  _chkTooltip_ShowPercentageValues;
   private Button  _chkTooltip_ShowSummaryValues;

   private Button  _rdoDuration_ElapsedTime;
   private Button  _rdoDuration_RecordedTime;
   private Button  _rdoDuration_PausedTime;
   private Button  _rdoDuration_MovingTime;
   private Button  _rdoDuration_BreakTime;

   private Spinner _spinnerAltitude_Interval;
   private Spinner _spinnerAltitude_Minimum;
   private Spinner _spinnerAltitude_NumOfBars;

   private Spinner _spinnerDistance_Interval;
   private Spinner _spinnerDistance_Minimum;
   private Spinner _spinnerDistance_NumOfBars;

   private Spinner _spinnerDuration_Interval;
   private Spinner _spinnerDuration_Minimum;
   private Spinner _spinnerDuration_NumOfBars;

   @Override
   public void createUI(final Composite parent) {

      initUI();

      createUI_10_DataGroups(parent);
      createUI_20_DurationTime(parent);
      createUI_30_StatisticTooltip(parent);
   }

   private void createUI_10_DataGroups(final Composite parent) {

      final int leftPadding = 8;

      final Group group = new Group(parent, SWT.NONE);
//		group.setText(Messages.Pref_Graphs_Group_Grid);
      group.setText(Messages.Slideout_StatisticOptions_Group_TourFrequency);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .span(2, 1)
            .applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(6).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            // spacer
            new Label(group, SWT.NONE);
         }
         {
            /*
             * Label: Minimum
             */
            final Label label = new Label(group, SWT.NONE);
            label.setText(Messages.Slideout_StatisticOptions_Label_Minimum);
            GridDataFactory.fillDefaults()//
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(leftPadding, 0)
                  .span(2, 1)
                  .applyTo(label);
         }
         {
            /*
             * Label: Interval
             */
            final Label label = new Label(group, SWT.NONE);
            label.setText(Messages.Slideout_StatisticOptions_Label_Interval);
            GridDataFactory.fillDefaults()//
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(leftPadding, 0)
                  .span(2, 1)
                  .applyTo(label);
         }
         {
            /*
             * Label: Number of bars
             */
            final Label label = new Label(group, SWT.NONE);
            label.setText(Messages.Slideout_StatisticOptions_Label_NumberOfBars);
            GridDataFactory.fillDefaults()//
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(leftPadding, 0)
                  .applyTo(label);
         }

         /*
          * Distance
          */
         {
            {
               /*
                * Label: Distance
                */
               final Label label = new Label(group, SWT.NONE);
               label.setText(Messages.Slideout_StatisticOptions_Label_Distance);
               GridDataFactory.fillDefaults()//
                     .align(SWT.FILL, SWT.CENTER)
                     .applyTo(label);
            }
            {
               /*
                * Minimum
                */

               // spinner
               _spinnerDistance_Minimum = new Spinner(group, SWT.BORDER);
               _spinnerDistance_Minimum.setMinimum(0);
               _spinnerDistance_Minimum.setMaximum(1000);
               _spinnerDistance_Minimum.setPageIncrement(5);
               _spinnerDistance_Minimum.addMouseWheelListener(_defaultMouseWheelListener);
               _spinnerDistance_Minimum.addSelectionListener(_defaultSelectionListener);
               GridDataFactory.fillDefaults() //
                     .align(SWT.BEGINNING, SWT.FILL)
                     .indent(leftPadding, 0)
                     .applyTo(_spinnerDistance_Minimum);

               // unit
               final Label label = new Label(group, SWT.NONE);
               label.setText(UI.UNIT_LABEL_DISTANCE);
               GridDataFactory.fillDefaults()//
                     .align(SWT.FILL, SWT.CENTER)
                     .applyTo(label);
            }
            {
               /*
                * Interval
                */

               // spinner
               _spinnerDistance_Interval = new Spinner(group, SWT.BORDER);
               _spinnerDistance_Interval.setMinimum(1);
               _spinnerDistance_Interval.setMaximum(1000);
               _spinnerDistance_Interval.setPageIncrement(5);
               _spinnerDistance_Interval.addMouseWheelListener(_defaultMouseWheelListener);
               _spinnerDistance_Interval.addSelectionListener(_defaultSelectionListener);
               GridDataFactory.fillDefaults() //
                     .align(SWT.BEGINNING, SWT.FILL)
                     .indent(leftPadding, 0)
                     .applyTo(_spinnerDistance_Interval);

               // unit
               final Label label = new Label(group, SWT.NONE);
               label.setText(UI.UNIT_LABEL_DISTANCE);
               GridDataFactory.fillDefaults()//
                     .align(SWT.FILL, SWT.CENTER)
                     .applyTo(label);
            }
            {
               /*
                * Number of bars
                */

               // spinner
               _spinnerDistance_NumOfBars = new Spinner(group, SWT.BORDER);
               _spinnerDistance_NumOfBars.setMinimum(1);
               _spinnerDistance_NumOfBars.setMaximum(1000);
               _spinnerDistance_NumOfBars.addMouseWheelListener(_defaultMouseWheelListener);
               _spinnerDistance_NumOfBars.addSelectionListener(_defaultSelectionListener);
               GridDataFactory.fillDefaults() //
                     .align(SWT.BEGINNING, SWT.FILL)
                     .indent(leftPadding, 0)
                     .applyTo(_spinnerDistance_NumOfBars);
            }
         }

         /*
          * Label: Altitude
          */
         {
            {
               /*
                * Label: Altitude
                */
               final Label label = new Label(group, SWT.NONE);
               label.setText(Messages.Slideout_StatisticOptions_Label_Altitude);
               GridDataFactory.fillDefaults()//
                     .align(SWT.FILL, SWT.CENTER)
                     .applyTo(label);
            }
            {
               /*
                * Minimum
                */

               // spinner
               _spinnerAltitude_Minimum = new Spinner(group, SWT.BORDER);
               _spinnerAltitude_Minimum.setMinimum(0);
               _spinnerAltitude_Minimum.setMaximum(9999);
               _spinnerAltitude_Minimum.setPageIncrement(50);
               _spinnerAltitude_Minimum.addMouseWheelListener(_defaultMouseWheelListener);
               _spinnerAltitude_Minimum.addSelectionListener(_defaultSelectionListener);
               GridDataFactory.fillDefaults() //
                     .align(SWT.BEGINNING, SWT.FILL)
                     .indent(leftPadding, 0)
                     .applyTo(_spinnerAltitude_Minimum);

               // unit
               final Label label = new Label(group, SWT.NONE);
               label.setText(UI.UNIT_LABEL_ELEVATION);
               GridDataFactory.fillDefaults()//
                     .align(SWT.FILL, SWT.CENTER)
                     .applyTo(label);
            }
            {
               /*
                * Interval
                */

               // spinner
               _spinnerAltitude_Interval = new Spinner(group, SWT.BORDER);
               _spinnerAltitude_Interval.setMinimum(1);
               _spinnerAltitude_Interval.setMaximum(9999);
               _spinnerAltitude_Interval.setPageIncrement(50);
               _spinnerAltitude_Interval.addMouseWheelListener(_defaultMouseWheelListener);
               _spinnerAltitude_Interval.addSelectionListener(_defaultSelectionListener);
               GridDataFactory.fillDefaults() //
                     .align(SWT.BEGINNING, SWT.FILL)
                     .indent(leftPadding, 0)
                     .applyTo(_spinnerAltitude_Interval);

               // unit
               final Label label = new Label(group, SWT.NONE);
               label.setText(UI.UNIT_LABEL_ELEVATION);
               GridDataFactory.fillDefaults()//
                     .align(SWT.FILL, SWT.CENTER)
                     .applyTo(label);
            }
            {
               /*
                * Number of bars
                */

               // spinner
               _spinnerAltitude_NumOfBars = new Spinner(group, SWT.BORDER);
               _spinnerAltitude_NumOfBars.setMinimum(1);
               _spinnerAltitude_NumOfBars.setMaximum(1000);
               _spinnerAltitude_NumOfBars.addMouseWheelListener(_defaultMouseWheelListener);
               _spinnerAltitude_NumOfBars.addSelectionListener(_defaultSelectionListener);
               GridDataFactory.fillDefaults() //
                     .align(SWT.BEGINNING, SWT.FILL)
                     .indent(leftPadding, 0)
                     .applyTo(_spinnerAltitude_NumOfBars);
            }
         }

         /*
          * Duration
          */
         {
            {
               /*
                * Label: Duration
                */
               final Label label = new Label(group, SWT.NONE);
               label.setText(Messages.Slideout_StatisticOptions_Label_Duration);
               GridDataFactory.fillDefaults()//
                     .align(SWT.FILL, SWT.CENTER)
                     .applyTo(label);
            }
            {
               /*
                * Minimum
                */

               // spinner
               _spinnerDuration_Minimum = new Spinner(group, SWT.BORDER);
               _spinnerDuration_Minimum.setMinimum(0);
               _spinnerDuration_Minimum.setMaximum(9999);
               _spinnerDuration_Minimum.setPageIncrement(30);
               _spinnerDuration_Minimum.addMouseWheelListener(_defaultMouseWheelListener);
               _spinnerDuration_Minimum.addSelectionListener(_defaultSelectionListener);
               GridDataFactory.fillDefaults() //
                     .align(SWT.BEGINNING, SWT.FILL)
                     .indent(leftPadding, 0)
                     .applyTo(_spinnerDuration_Minimum);

               // unit
               final Label label = new Label(group, SWT.NONE);
               label.setText(Messages.App_Unit_Minute_Small);
               GridDataFactory.fillDefaults()//
                     .align(SWT.FILL, SWT.CENTER)
                     .applyTo(label);
            }
            {
               /*
                * Interval
                */

               // spinner
               _spinnerDuration_Interval = new Spinner(group, SWT.BORDER);
               _spinnerDuration_Interval.setMinimum(1);
               _spinnerDuration_Interval.setMaximum(9999);
               _spinnerDuration_Interval.setPageIncrement(30);
               _spinnerDuration_Interval.addMouseWheelListener(_defaultMouseWheelListener);
               _spinnerDuration_Interval.addSelectionListener(_defaultSelectionListener);
               GridDataFactory.fillDefaults() //
                     .align(SWT.BEGINNING, SWT.FILL)
                     .indent(leftPadding, 0)
                     .applyTo(_spinnerDuration_Interval);

               // unit
               final Label label = new Label(group, SWT.NONE);
               label.setText(Messages.App_Unit_Minute_Small);
               GridDataFactory.fillDefaults()//
                     .align(SWT.FILL, SWT.CENTER)
                     .applyTo(label);
            }
            {
               /*
                * Number of bars
                */

               // spinner
               _spinnerDuration_NumOfBars = new Spinner(group, SWT.BORDER);
               _spinnerDuration_NumOfBars.setMinimum(1);
               _spinnerDuration_NumOfBars.setMaximum(1000);
               _spinnerDuration_NumOfBars.addMouseWheelListener(_defaultMouseWheelListener);
               _spinnerDuration_NumOfBars.addSelectionListener(_defaultSelectionListener);
               GridDataFactory.fillDefaults() //
                     .align(SWT.BEGINNING, SWT.FILL)
                     .indent(leftPadding, 0)
                     .applyTo(_spinnerDuration_NumOfBars);
            }
         }

      }
   }

   /**
    * Elapsed, recorded, paused, moving and break time
    */
   private void createUI_20_DurationTime(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Slideout_StatisticOptions_Group_DurationTime);
      GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(group);
      GridLayoutFactory.swtDefaults()
            .numColumns(2)
            .spacing(30, 5)
            .applyTo(group);
      {
         // row 1
         {
            /*
             * Elapsed time
             */
            _rdoDuration_ElapsedTime = new Button(group, SWT.RADIO);
            _rdoDuration_ElapsedTime.setText(Messages.Slideout_StatisticOptions_Radio_Duration_ElapsedTime);
            _rdoDuration_ElapsedTime.addSelectionListener(_defaultSelectionListener);
         }
         {
            // spacer
            new Label(group, SWT.NONE);
         }

         // row 2
         {
            /*
             * Recorded time
             */
            _rdoDuration_RecordedTime = new Button(group, SWT.RADIO);
            _rdoDuration_RecordedTime.setText(Messages.Slideout_StatisticOptions_Radio_Duration_RecordedTime);
            _rdoDuration_RecordedTime.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Moving time
             */
            _rdoDuration_MovingTime = new Button(group, SWT.RADIO);
            _rdoDuration_MovingTime.setText(Messages.Slideout_StatisticOptions_Radio_Duration_MovingTime);
            _rdoDuration_MovingTime.addSelectionListener(_defaultSelectionListener);
         }

         // row 3
         {
            /*
             * Paused time
             */
            _rdoDuration_PausedTime = new Button(group, SWT.RADIO);
            _rdoDuration_PausedTime.setText(Messages.Slideout_StatisticOptions_Radio_Duration_PausedTime);
            _rdoDuration_PausedTime.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Break time
             */
            _rdoDuration_BreakTime = new Button(group, SWT.RADIO);
            _rdoDuration_BreakTime.setText(Messages.Slideout_StatisticOptions_Radio_Duration_BreakTime);
            _rdoDuration_BreakTime.addSelectionListener(_defaultSelectionListener);
         }
      }

      // set tab order that device and computed times are grouped together
      final Control[] tabList = {

            _rdoDuration_ElapsedTime,
            _rdoDuration_RecordedTime,
            _rdoDuration_PausedTime,

            _rdoDuration_MovingTime,
            _rdoDuration_BreakTime,
      };
      group.setTabList(tabList);
   }

   private void createUI_30_StatisticTooltip(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Slideout_StatisticOptions_Group_StatisticTooltip);
      GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
      {
         {
            /*
             * Show total values
             */
            _chkTooltip_ShowSummaryValues = new Button(group, SWT.CHECK);
            _chkTooltip_ShowSummaryValues.setText(Messages.Slideout_StatisticOptions_Checkbox_ShowSummaryValues);
            _chkTooltip_ShowSummaryValues.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show % values
             */
            _chkTooltip_ShowPercentageValues = new Button(group, SWT.CHECK);
            _chkTooltip_ShowPercentageValues.setText(Messages.Slideout_StatisticOptions_Checkbox_ShowPercentageValues);
//          tooltip: Percentage of the bar value to the total value
            _chkTooltip_ShowPercentageValues.addSelectionListener(_defaultSelectionListener);
         }
      }
   }

   private void initUI() {

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener = mouseEvent -> {
         net.tourbook.common.UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI();
      };
   }

   private void onChangeUI() {

      // update chart async (which is done when a pref store value is modified) that the UI is updated immediately

      Display.getCurrent().asyncExec(this::saveState);
   }

   @Override
   public void resetToDefaults() {

      _chkTooltip_ShowPercentageValues.setSelection(_prefStore.getDefaultBoolean(
            ITourbookPreferences.STAT_FREQUENCY_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES));
      _chkTooltip_ShowSummaryValues.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.STAT_FREQUENCY_TOOLTIP_IS_SHOW_SUMMARY_VALUES));

      _spinnerAltitude_Interval.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_ALTITUDE_INTERVAL));
      _spinnerAltitude_Minimum.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE));
      _spinnerAltitude_NumOfBars.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_ALTITUDE_NUMBERS));

      _spinnerDistance_Interval.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_DISTANCE_INTERVAL));
      _spinnerDistance_Minimum.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_DISTANCE_LOW_VALUE));
      _spinnerDistance_NumOfBars.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_DISTANCE_NUMBERS));

      _spinnerDuration_Interval.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_DURATION_INTERVAL));
      _spinnerDuration_Minimum.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_DURATION_LOW_VALUE));
      _spinnerDuration_NumOfBars.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.STAT_DURATION_NUMBERS));

      final Enum<DurationTime> durationTime = Util.getEnumValue(
            _prefStore.getDefaultString(ITourbookPreferences.STAT_FREQUENCY_DURATION_TIME),
            DurationTime.MOVING);
      _rdoDuration_ElapsedTime.setSelection(durationTime.equals(DurationTime.ELAPSED));
      _rdoDuration_RecordedTime.setSelection(durationTime.equals(DurationTime.RECORDED));
      _rdoDuration_PausedTime.setSelection(durationTime.equals(DurationTime.PAUSED));
      _rdoDuration_MovingTime.setSelection(durationTime.equals(DurationTime.MOVING));
      _rdoDuration_BreakTime.setSelection(durationTime.equals(DurationTime.BREAK));

      onChangeUI();
   }

   @Override
   public void restoreState() {

      _chkTooltip_ShowPercentageValues.setSelection(_prefStore.getBoolean(ITourbookPreferences.STAT_FREQUENCY_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES));
      _chkTooltip_ShowSummaryValues.setSelection(_prefStore.getBoolean(ITourbookPreferences.STAT_FREQUENCY_TOOLTIP_IS_SHOW_SUMMARY_VALUES));

      _spinnerAltitude_Interval.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_ALTITUDE_INTERVAL));
      _spinnerAltitude_Minimum.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE));
      _spinnerAltitude_NumOfBars.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_ALTITUDE_NUMBERS));

      _spinnerDistance_Interval.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_DISTANCE_INTERVAL));
      _spinnerDistance_Minimum.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_DISTANCE_LOW_VALUE));
      _spinnerDistance_NumOfBars.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_DISTANCE_NUMBERS));

      _spinnerDuration_Interval.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_DURATION_INTERVAL));
      _spinnerDuration_Minimum.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_DURATION_LOW_VALUE));
      _spinnerDuration_NumOfBars.setSelection(_prefStore.getInt(ITourbookPreferences.STAT_DURATION_NUMBERS));

      final Enum<DurationTime> durationTime = Util.getEnumValue(
            _prefStore.getString(ITourbookPreferences.STAT_FREQUENCY_DURATION_TIME),
            DurationTime.MOVING);
      _rdoDuration_BreakTime.setSelection(durationTime.equals(DurationTime.BREAK));
      _rdoDuration_MovingTime.setSelection(durationTime.equals(DurationTime.MOVING));
      _rdoDuration_ElapsedTime.setSelection(durationTime.equals(DurationTime.ELAPSED));
      _rdoDuration_RecordedTime.setSelection(durationTime.equals(DurationTime.RECORDED));
      _rdoDuration_PausedTime.setSelection(durationTime.equals(DurationTime.PAUSED));
   }

   @Override
   public void saveState() {

      _prefStore.setValue(ITourbookPreferences.STAT_FREQUENCY_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES, _chkTooltip_ShowPercentageValues.getSelection());
      _prefStore.setValue(ITourbookPreferences.STAT_FREQUENCY_TOOLTIP_IS_SHOW_SUMMARY_VALUES, _chkTooltip_ShowSummaryValues.getSelection());

      _prefStore.setValue(ITourbookPreferences.STAT_ALTITUDE_INTERVAL, _spinnerAltitude_Interval.getSelection());
      _prefStore.setValue(ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE, _spinnerAltitude_Minimum.getSelection());
      _prefStore.setValue(ITourbookPreferences.STAT_ALTITUDE_NUMBERS, _spinnerAltitude_NumOfBars.getSelection());

      _prefStore.setValue(ITourbookPreferences.STAT_DISTANCE_INTERVAL, _spinnerDistance_Interval.getSelection());
      _prefStore.setValue(ITourbookPreferences.STAT_DISTANCE_LOW_VALUE, _spinnerDistance_Minimum.getSelection());
      _prefStore.setValue(ITourbookPreferences.STAT_DISTANCE_NUMBERS, _spinnerDistance_NumOfBars.getSelection());

      _prefStore.setValue(ITourbookPreferences.STAT_DURATION_INTERVAL, _spinnerDuration_Interval.getSelection());
      _prefStore.setValue(ITourbookPreferences.STAT_DURATION_LOW_VALUE, _spinnerDuration_Minimum.getSelection());
      _prefStore.setValue(ITourbookPreferences.STAT_DURATION_NUMBERS, _spinnerDuration_NumOfBars.getSelection());

      String selectedDurationTime = UI.EMPTY_STRING;
      if (_rdoDuration_BreakTime.getSelection()) {
         selectedDurationTime = DurationTime.BREAK.name();
      } else if (_rdoDuration_MovingTime.getSelection()) {
         selectedDurationTime = DurationTime.MOVING.name();
      } else if (_rdoDuration_RecordedTime.getSelection()) {
         selectedDurationTime = DurationTime.RECORDED.name();
      } else if (_rdoDuration_PausedTime.getSelection()) {
         selectedDurationTime = DurationTime.PAUSED.name();
      } else if (_rdoDuration_ElapsedTime.getSelection()) {
         selectedDurationTime = DurationTime.ELAPSED.name();
      }
      _prefStore.setValue(ITourbookPreferences.STAT_FREQUENCY_DURATION_TIME, selectedDurationTime);
   }
}
