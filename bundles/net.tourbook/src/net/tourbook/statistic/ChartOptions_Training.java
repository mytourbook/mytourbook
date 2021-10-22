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

import de.byteholder.geoclipse.map.UI;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.Util;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;

public class ChartOptions_Training implements IStatisticOptions {

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private SelectionListener      _defaultSelectionListener;

   /*
    * UI controls
    */
   private Button           _chkShow_TrainingEffect;
   private Button           _chkShow_TrainingEffect_Anaerobic;
   private Button           _chkShow_TrainingPerformance;
   private Button           _chkShow_TrainingPerformance_AvgValue;

   private Button           _chkShowAltitude;
   private Button           _chkShowDistance;
   private Button           _chkShowDuration;
   private Button           _chkShowAvgSpeed;
   private Button           _chkShowAvgPace;

   private Button           _rdoDuration_ElapsedTime;
   private Button           _rdoDuration_RecordedTime;
   private Button           _rdoDuration_PausedTime;
   private Button           _rdoDuration_MovingTime;
   private Button           _rdoDuration_BreakTime;

   private TrainingPrefKeys _prefKeys;

   public ChartOptions_Training(final TrainingPrefKeys prefKeys) {

      _prefKeys = prefKeys;
   }

   @Override
   public void createUI(final Composite parent) {

      initUI();

      createUI_10_Training(parent);

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Pref_Statistic_Group_DaySummary);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .span(2, 1)
            .applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//    group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         createUI_20_DaySummary_Left(group);
         createUI_30_DaySummary_Right(group);
      }
   }

   private void createUI_10_Training(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Pref_Statistic_Group_Training);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .span(2, 1)
            .applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
      {
         {
            /*
             * Show training effect
             */
            _chkShow_TrainingEffect = new Button(group, SWT.CHECK);
            _chkShow_TrainingEffect.setText(Messages.Pref_Statistic_Checkbox_TrainingEffect_Aerob);
            _chkShow_TrainingEffect.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show training effect anaerobic
             */
            _chkShow_TrainingEffect_Anaerobic = new Button(group, SWT.CHECK);
            _chkShow_TrainingEffect_Anaerobic.setText(Messages.Pref_Statistic_Checkbox_TrainingEffect_Anaerob);
            _chkShow_TrainingEffect_Anaerobic.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show training performance
             */
            _chkShow_TrainingPerformance = new Button(group, SWT.CHECK);
            _chkShow_TrainingPerformance.setText(Messages.Pref_Statistic_Checkbox_TrainingPerformance);
            _chkShow_TrainingPerformance.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show training performance average value
             */
            _chkShow_TrainingPerformance_AvgValue = new Button(group, SWT.CHECK);
            _chkShow_TrainingPerformance_AvgValue.setText(Messages.Pref_Statistic_Checkbox_TrainingPerformance_AvgValue);
            _chkShow_TrainingPerformance_AvgValue.setToolTipText(Messages.Pref_Statistic_Checkbox_TrainingPerformance_AvgValue_Tooltip);
            _chkShow_TrainingPerformance_AvgValue.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().indent(16, 0).applyTo(_chkShow_TrainingPerformance_AvgValue);
         }
      }
   }

   private void createUI_20_DaySummary_Left(final Composite parent) {

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
      }
   }

   private void createUI_30_DaySummary_Right(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         {
            /*
             * Show avg speed
             */
            _chkShowAvgSpeed = new Button(container, SWT.CHECK);
            _chkShowAvgSpeed.setText(Messages.Pref_Statistic_Checkbox_AvgSpeed);
            _chkShowAvgSpeed.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show avg pace
             */
            _chkShowAvgPace = new Button(container, SWT.CHECK);
            _chkShowAvgPace.setText(Messages.Pref_Statistic_Checkbox_AvgPace);
            _chkShowAvgPace.addSelectionListener(_defaultSelectionListener);
         }
      }
   }

   private void enableControls() {

      final boolean isShowDuration = _chkShowDuration.getSelection();
      final boolean isShowTrainingPerformance = _chkShow_TrainingPerformance.getSelection();

      _chkShow_TrainingPerformance_AvgValue.setEnabled(isShowTrainingPerformance);

      _rdoDuration_MovingTime.setEnabled(isShowDuration);
      _rdoDuration_BreakTime.setEnabled(isShowDuration);
      _rdoDuration_ElapsedTime.setEnabled(isShowDuration);
      _rdoDuration_RecordedTime.setEnabled(isShowDuration);
      _rdoDuration_PausedTime.setEnabled(isShowDuration);
   }

   private void initUI() {

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());
   }

   private void onChangeUI() {

      // update chart async (which is done when a pref store value is modified) that the UI is updated immediately

      enableControls();

      Display.getCurrent().asyncExec(this::saveState);
   }

   @Override
   public void resetToDefaults() {

      _chkShowAltitude.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_Altitude));
      _chkShowAvgPace.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_Avg_Pace));
      _chkShowAvgSpeed.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_Avg_Speed));
      _chkShowDistance.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_Distance));
      _chkShowDuration.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_Duration));

      final Enum<DurationTime> durationTime = Util.getEnumValue(_prefStore.getDefaultString(_prefKeys.durationTime), DurationTime.MOVING);
      _rdoDuration_BreakTime.setSelection(durationTime.equals(DurationTime.BREAK));
      _rdoDuration_MovingTime.setSelection(durationTime.equals(DurationTime.MOVING));
      _rdoDuration_ElapsedTime.setSelection(durationTime.equals(DurationTime.ELAPSED));
      _rdoDuration_RecordedTime.setSelection(durationTime.equals(DurationTime.RECORDED));
      _rdoDuration_PausedTime.setSelection(durationTime.equals(DurationTime.PAUSED));

      _chkShow_TrainingEffect.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_TrainingEffect));
      _chkShow_TrainingEffect_Anaerobic.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_TrainingEffect_Anaerobic));
      _chkShow_TrainingPerformance.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_TrainingPerformance));
      _chkShow_TrainingPerformance_AvgValue.setSelection(_prefStore.getDefaultBoolean(_prefKeys.isShow_TrainingPerformance_AvgValue));

      enableControls();
   }

   @Override
   public void restoreState() {

      _chkShowAltitude.setSelection(_prefStore.getBoolean(_prefKeys.isShow_Altitude));
      _chkShowAvgPace.setSelection(_prefStore.getBoolean(_prefKeys.isShow_Avg_Pace));
      _chkShowAvgSpeed.setSelection(_prefStore.getBoolean(_prefKeys.isShow_Avg_Speed));
      _chkShowDistance.setSelection(_prefStore.getBoolean(_prefKeys.isShow_Distance));
      _chkShowDuration.setSelection(_prefStore.getBoolean(_prefKeys.isShow_Duration));

      final Enum<DurationTime> durationTime = Util.getEnumValue(_prefStore.getString(_prefKeys.durationTime), DurationTime.MOVING);
      _rdoDuration_BreakTime.setSelection(durationTime.equals(DurationTime.BREAK));
      _rdoDuration_MovingTime.setSelection(durationTime.equals(DurationTime.MOVING));
      _rdoDuration_ElapsedTime.setSelection(durationTime.equals(DurationTime.ELAPSED));
      _rdoDuration_RecordedTime.setSelection(durationTime.equals(DurationTime.RECORDED));
      _rdoDuration_PausedTime.setSelection(durationTime.equals(DurationTime.PAUSED));

      _chkShow_TrainingEffect.setSelection(_prefStore.getBoolean(_prefKeys.isShow_TrainingEffect));
      _chkShow_TrainingEffect_Anaerobic.setSelection(_prefStore.getBoolean(_prefKeys.isShow_TrainingEffect_Anaerobic));
      _chkShow_TrainingPerformance.setSelection(_prefStore.getBoolean(_prefKeys.isShow_TrainingPerformance));
      _chkShow_TrainingPerformance_AvgValue.setSelection(_prefStore.getBoolean(_prefKeys.isShow_TrainingPerformance_AvgValue));

      enableControls();
   }

   @Override
   public void saveState() {

      _prefStore.setValue(_prefKeys.isShow_Altitude, _chkShowAltitude.getSelection());
      _prefStore.setValue(_prefKeys.isShow_Avg_Pace, _chkShowAvgPace.getSelection());
      _prefStore.setValue(_prefKeys.isShow_Avg_Speed, _chkShowAvgSpeed.getSelection());
      _prefStore.setValue(_prefKeys.isShow_Distance, _chkShowDistance.getSelection());
      _prefStore.setValue(_prefKeys.isShow_Duration, _chkShowDuration.getSelection());

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

      _prefStore.setValue(_prefKeys.durationTime, selectedDurationType);

      _prefStore.setValue(_prefKeys.isShow_TrainingEffect, _chkShow_TrainingEffect.getSelection());
      _prefStore.setValue(_prefKeys.isShow_TrainingEffect_Anaerobic, _chkShow_TrainingEffect_Anaerobic.getSelection());
      _prefStore.setValue(_prefKeys.isShow_TrainingPerformance, _chkShow_TrainingPerformance.getSelection());
      _prefStore.setValue(_prefKeys.isShow_TrainingPerformance_AvgValue, _chkShow_TrainingPerformance_AvgValue.getSelection());
   }
}
