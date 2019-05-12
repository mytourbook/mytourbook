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

   /*
    * UI controls
    */
   private Button _chkShow_TrainingEffect;
   private Button _chkShow_TrainingEffect_Anaerobic;
   private Button _chkShow_TrainingPerformance;

   private String _prefKey_IsShowTrainingEffect;
   private String _prefKey_IsShowTrainingEffect_Anaerobic;
   private String _prefKey_IsShowTrainingPerformance;

   public ChartOptions_Training(final String prefKey_IsShowTrainingEffect,
                                final String prefKey_IsShowTrainingEffect_Anaerobic,
                                final String prefKey_IsShowTrainingPerformance) {

      _prefKey_IsShowTrainingEffect = prefKey_IsShowTrainingEffect;
      _prefKey_IsShowTrainingEffect_Anaerobic = prefKey_IsShowTrainingEffect_Anaerobic;
      _prefKey_IsShowTrainingPerformance = prefKey_IsShowTrainingPerformance;
   }

   @Override
   public void createUI(final Composite parent) {

      initUI(parent);

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Pref_Statistic_Group_Training);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .span(2, 1)
            .applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
      {
         createUI_10(group);
      }
   }

   private void createUI_10(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         {
            /*
             * Show training effect
             */
            _chkShow_TrainingEffect = new Button(container, SWT.CHECK);
            _chkShow_TrainingEffect.setText(Messages.Pref_Statistic_Checkbox_TrainingEffect);
            _chkShow_TrainingEffect.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show training effect anaerobic
             */
            _chkShow_TrainingEffect_Anaerobic = new Button(container, SWT.CHECK);
            _chkShow_TrainingEffect_Anaerobic.setText(Messages.Pref_Statistic_Checkbox_TrainingEffect_Anaerobic);
            _chkShow_TrainingEffect_Anaerobic.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show training performance
             */
            _chkShow_TrainingPerformance = new Button(container, SWT.CHECK);
            _chkShow_TrainingPerformance.setText(Messages.Pref_Statistic_Checkbox_TrainingPerformance);
            _chkShow_TrainingPerformance.addSelectionListener(_defaultSelectionListener);
         }
      }
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

      Display.getCurrent().asyncExec(new Runnable() {
         @Override
         public void run() {

            saveState();
         }
      });
   }

   @Override
   public void resetToDefaults() {

      _chkShow_TrainingEffect.setSelection(_prefStore.getDefaultBoolean(_prefKey_IsShowTrainingEffect));
      _chkShow_TrainingEffect_Anaerobic.setSelection(_prefStore.getDefaultBoolean(_prefKey_IsShowTrainingEffect_Anaerobic));
      _chkShow_TrainingPerformance.setSelection(_prefStore.getDefaultBoolean(_prefKey_IsShowTrainingPerformance));
   }

   @Override
   public void restoreState() {

      _chkShow_TrainingEffect.setSelection(_prefStore.getBoolean(_prefKey_IsShowTrainingEffect));
      _chkShow_TrainingEffect_Anaerobic.setSelection(_prefStore.getBoolean(_prefKey_IsShowTrainingEffect_Anaerobic));
      _chkShow_TrainingPerformance.setSelection(_prefStore.getBoolean(_prefKey_IsShowTrainingPerformance));
   }

   @Override
   public void saveState() {

      _prefStore.setValue(_prefKey_IsShowTrainingEffect, _chkShow_TrainingEffect.getSelection());
      _prefStore.setValue(_prefKey_IsShowTrainingEffect_Anaerobic, _chkShow_TrainingEffect_Anaerobic.getSelection());
      _prefStore.setValue(_prefKey_IsShowTrainingPerformance, _chkShow_TrainingPerformance.getSelection());
   }
}
