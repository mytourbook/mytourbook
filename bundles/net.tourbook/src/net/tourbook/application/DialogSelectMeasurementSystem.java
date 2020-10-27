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
package net.tourbook.application;

import net.tourbook.Messages;
import net.tourbook.measurement_system.MeasurementSystem;
import net.tourbook.measurement_system.MeasurementSystem_Manager;
import net.tourbook.measurement_system.SystemAtmosphericPressure;
import net.tourbook.measurement_system.SystemDistance;
import net.tourbook.measurement_system.SystemElevation;
import net.tourbook.measurement_system.SystemTemperature;
import net.tourbook.measurement_system.SystemWeight;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

class DialogSelectMeasurementSystem extends Dialog {

   private Combo _comboSystem_AtmosphericPressure;
   private Combo _comboSystem_Distance;
   private Combo _comboSystem_Elevation;
   private Combo _comboSystem_Profile;
   private Combo _comboSystem_Temperature;
   private Combo _comboSystem_Weight;

   protected DialogSelectMeasurementSystem(final Shell parentShell) {
      super(parentShell);
   }

   @Override
   public boolean close() {

//		MeasurementSystemContributionItem.saveMeasurementSystem_OLD(systemIndex);

      return super.close();
   }

   @Override
   protected void configureShell(final Shell shell) {
      super.configureShell(shell);
      shell.setText(Messages.App_Dialog_FirstStartupSystem_Title);
   }

   @Override
   protected void createButtonsForButtonBar(final Composite parent) {
      createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite ui = createUI(parent);

      enableControls();
      fillControls();

      // select metric system which is the first profile in the first startup
      _comboSystem_Profile.select(0);
      onSystemProfile_Select();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.swtDefaults().margins(10, 10).numColumns(1).applyTo(container);
      {
         {
            // label: measurement system

            final Label label = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().applyTo(label);
            label.setText(Messages.App_Dialog_FirstStartupSystem_Label_System);
         }

         createUI_10_MeasurementSystem_Data(container);

         {
            // label: info

            final Label label = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().indent(0, 15).applyTo(label);
            label.setText(Messages.App_Dialog_FirstStartupSystem_Label_Info);
         }
      }

      return container;
   }

   private void createUI_10_MeasurementSystem_Data(final Composite parent) {

      final GridDataFactory gridData_Combo = GridDataFactory.fillDefaults().grab(true, false);
      final GridDataFactory gridData_Label = GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER);

      final SelectionAdapter profileListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onSystemProfile_Select();
         }
      };

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Measurement system
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_System_Label_System);
            gridData_Label.applyTo(label);

            // combo
            _comboSystem_Profile = new Combo(container, SWT.READ_ONLY);
            _comboSystem_Profile.addSelectionListener(profileListener);
            gridData_Combo.applyTo(_comboSystem_Profile);
         }
         {
            /*
             * Distance
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_System_Label_Distance);
            gridData_Label.applyTo(label);

            // combo
            _comboSystem_Distance = new Combo(container, SWT.READ_ONLY);
            gridData_Combo.applyTo(_comboSystem_Distance);
         }
         {
            /*
             * Elevation
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_System_Label_Elevation);
            gridData_Label.applyTo(label);

            // combo
            _comboSystem_Elevation = new Combo(container, SWT.READ_ONLY);
            gridData_Combo.applyTo(_comboSystem_Elevation);
         }
         {
            /*
             * Temperature
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_System_Label_Temperature);
            gridData_Label.applyTo(label);

            // combo
            _comboSystem_Temperature = new Combo(container, SWT.READ_ONLY);
            gridData_Combo.applyTo(_comboSystem_Temperature);
         }
         {
            /*
             * Body weight
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_System_Label_Weight);
            gridData_Label.applyTo(label);

            // combo
            _comboSystem_Weight = new Combo(container, SWT.READ_ONLY);
            gridData_Combo.applyTo(_comboSystem_Weight);
         }
         {
            /*
             * Atmospheric pressure
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_System_Label_AtmosphericPressure);
            gridData_Label.applyTo(label);

            // combo
            _comboSystem_AtmosphericPressure = new Combo(container, SWT.READ_ONLY);
            gridData_Combo.applyTo(_comboSystem_AtmosphericPressure);
         }
      }
   }

   private void enableControls() {

      _comboSystem_AtmosphericPressure.setEnabled(false);
      _comboSystem_Distance.setEnabled(false);
      _comboSystem_Elevation.setEnabled(false);
      _comboSystem_Temperature.setEnabled(false);
      _comboSystem_Weight.setEnabled(false);
   }

   private void fillControls() {

      for (final MeasurementSystem systemProfile : MeasurementSystem_Manager.getCurrentProfiles()) {
         _comboSystem_Profile.add(systemProfile.getName());
      }

      for (final SystemAtmosphericPressure system : MeasurementSystem_Manager.getAllSystem_AtmosphericPressures()) {
         _comboSystem_AtmosphericPressure.add(system.getLabel());
      }

      for (final SystemDistance systemDistance : MeasurementSystem_Manager.getAllSystem_Distances()) {
         _comboSystem_Distance.add(systemDistance.getLabel());
      }

      for (final SystemElevation systemElevation : MeasurementSystem_Manager.getAllSystem_Elevations()) {
         _comboSystem_Elevation.add(systemElevation.getLabel());
      }

      for (final SystemTemperature systemTemperature : MeasurementSystem_Manager.getAllSystem_Temperatures()) {
         _comboSystem_Temperature.add(systemTemperature.getLabel());
      }

      for (final SystemWeight systemWeight : MeasurementSystem_Manager.getAllSystem_Weights()) {
         _comboSystem_Weight.add(systemWeight.getLabel());
      }
   }

   private void onSystemProfile_Select() {

      final int activeSystemProfileIndex = _comboSystem_Profile.getSelectionIndex();
      final MeasurementSystem selectedSystemProfile = MeasurementSystem_Manager.getCurrentProfiles().get(activeSystemProfileIndex);

      _comboSystem_AtmosphericPressure.select(MeasurementSystem_Manager.getSystemIndex_AtmosphericPressure(selectedSystemProfile));
      _comboSystem_Distance.select(MeasurementSystem_Manager.getSystemIndex_Distance(selectedSystemProfile));
      _comboSystem_Elevation.select(MeasurementSystem_Manager.getSystemIndex_Elevation(selectedSystemProfile));
      _comboSystem_Temperature.select(MeasurementSystem_Manager.getSystemIndex_Temperature(selectedSystemProfile));
      _comboSystem_Weight.select(MeasurementSystem_Manager.getSystemIndex_Weight(selectedSystemProfile));
   }
}
