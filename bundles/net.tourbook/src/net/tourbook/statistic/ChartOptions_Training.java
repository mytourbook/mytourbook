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
package net.tourbook.statistic;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

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

public class ChartOptions_Training implements IStatisticOptions {

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private SelectionAdapter       _defaultSelectionListener;

   private String                 _prefKey_IsShow_Altitude;
   private String                 _prefKey_IsShow_Avg_Pace;
   private String                 _prefKey_IsShow_Avg_Speed;
   private String                 _prefKey_IsShow_Distance;
   private String                 _prefKey_IsShow_Duration;

   private String                 _prefKey_IsShow_TrainingEffect;
   private String                 _prefKey_IsShow_TrainingEffect_Anaerobic;
   private String                 _prefKey_IsShow_TrainingPerformance;
   private String                 _prefKey_IsShow_TrainingPerformance_AvgValue;

   /*
    * UI controls
    */
   private Button _chkShow_TrainingEffect;
   private Button _chkShow_TrainingEffect_Anaerobic;
   private Button _chkShow_TrainingPerformance;
   private Button _chkShow_TrainingPerformance_AvgValue;

   private Button _chkShowAltitude;
   private Button _chkShowDistance;
   private Button _chkShowDuration;
   private Button _chkShowAvgSpeed;
   private Button _chkShowAvgPace;

   public ChartOptions_Training(final String prefKey_IsShow_Altitude,
                                final String prefKey_IsShow_Avg_Pace,
                                final String prefKey_IsShow_Avg_Speed,
                                final String prefKey_IsShow_Distance,
                                final String prefKey_IsShow_Duration,
                                final String prefKey_IsShowTrainingEffect,
                                final String prefKey_IsShowTrainingEffect_Anaerobic,
                                final String prefKey_IsShowTrainingPerformance,
                                final String prefKey_IsShowTrainingPerformance_AvgValue) {

      _prefKey_IsShow_Altitude = prefKey_IsShow_Altitude;
      _prefKey_IsShow_Avg_Pace = prefKey_IsShow_Avg_Pace;
      _prefKey_IsShow_Avg_Speed = prefKey_IsShow_Avg_Speed;
      _prefKey_IsShow_Distance = prefKey_IsShow_Distance;
      _prefKey_IsShow_Duration = prefKey_IsShow_Duration;

      _prefKey_IsShow_TrainingEffect = prefKey_IsShowTrainingEffect;
      _prefKey_IsShow_TrainingEffect_Anaerobic = prefKey_IsShowTrainingEffect_Anaerobic;
      _prefKey_IsShow_TrainingPerformance = prefKey_IsShowTrainingPerformance;
      _prefKey_IsShow_TrainingPerformance_AvgValue = prefKey_IsShowTrainingPerformance_AvgValue;
   }

   @Override
   public void createUI(final Composite parent) {

      initUI(parent);

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
         createUI_50_DaySummary_Left(group);
         createUI_20_DaySummary_Right(group);
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

   private void createUI_20_DaySummary_Right(final Composite parent) {

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

   private void createUI_50_DaySummary_Left(final Composite parent) {

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
         }
      }
   }

   private void enableControls() {

      final boolean isShowTrainingPerformance = _chkShow_TrainingPerformance.getSelection();

      _chkShow_TrainingPerformance_AvgValue.setEnabled(isShowTrainingPerformance);
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

      _chkShowAltitude.setSelection(_prefStore.getDefaultBoolean(_prefKey_IsShow_Altitude));
      _chkShowAvgPace.setSelection(_prefStore.getDefaultBoolean(_prefKey_IsShow_Avg_Pace));
      _chkShowAvgSpeed.setSelection(_prefStore.getDefaultBoolean(_prefKey_IsShow_Avg_Speed));
      _chkShowDistance.setSelection(_prefStore.getDefaultBoolean(_prefKey_IsShow_Distance));
      _chkShowDuration.setSelection(_prefStore.getDefaultBoolean(_prefKey_IsShow_Duration));

      _chkShow_TrainingEffect.setSelection(_prefStore.getDefaultBoolean(_prefKey_IsShow_TrainingEffect));
      _chkShow_TrainingEffect_Anaerobic.setSelection(_prefStore.getDefaultBoolean(_prefKey_IsShow_TrainingEffect_Anaerobic));
      _chkShow_TrainingPerformance.setSelection(_prefStore.getDefaultBoolean(_prefKey_IsShow_TrainingPerformance));
      _chkShow_TrainingPerformance_AvgValue.setSelection(_prefStore.getDefaultBoolean(_prefKey_IsShow_TrainingPerformance_AvgValue));

      enableControls();
   }

   @Override
   public void restoreState() {

      _chkShowAltitude.setSelection(_prefStore.getBoolean(_prefKey_IsShow_Altitude));
      _chkShowAvgPace.setSelection(_prefStore.getBoolean(_prefKey_IsShow_Avg_Pace));
      _chkShowAvgSpeed.setSelection(_prefStore.getBoolean(_prefKey_IsShow_Avg_Speed));
      _chkShowDistance.setSelection(_prefStore.getBoolean(_prefKey_IsShow_Distance));
      _chkShowDuration.setSelection(_prefStore.getBoolean(_prefKey_IsShow_Duration));

      _chkShow_TrainingEffect.setSelection(_prefStore.getBoolean(_prefKey_IsShow_TrainingEffect));
      _chkShow_TrainingEffect_Anaerobic.setSelection(_prefStore.getBoolean(_prefKey_IsShow_TrainingEffect_Anaerobic));
      _chkShow_TrainingPerformance.setSelection(_prefStore.getBoolean(_prefKey_IsShow_TrainingPerformance));
      _chkShow_TrainingPerformance_AvgValue.setSelection(_prefStore.getBoolean(_prefKey_IsShow_TrainingPerformance_AvgValue));

      enableControls();
   }

   @Override
   public void saveState() {

      _prefStore.setValue(_prefKey_IsShow_Altitude, _chkShowAltitude.getSelection());
      _prefStore.setValue(_prefKey_IsShow_Avg_Pace, _chkShowAvgPace.getSelection());
      _prefStore.setValue(_prefKey_IsShow_Avg_Speed, _chkShowAvgSpeed.getSelection());
      _prefStore.setValue(_prefKey_IsShow_Distance, _chkShowDistance.getSelection());
      _prefStore.setValue(_prefKey_IsShow_Duration, _chkShowDuration.getSelection());

      _prefStore.setValue(_prefKey_IsShow_TrainingEffect, _chkShow_TrainingEffect.getSelection());
      _prefStore.setValue(_prefKey_IsShow_TrainingEffect_Anaerobic, _chkShow_TrainingEffect_Anaerobic.getSelection());
      _prefStore.setValue(_prefKey_IsShow_TrainingPerformance, _chkShow_TrainingPerformance.getSelection());
      _prefStore.setValue(_prefKey_IsShow_TrainingPerformance_AvgValue, _chkShow_TrainingPerformance_AvgValue.getSelection());
   }
}
