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
package net.tourbook.common.measurement_system;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.common.Messages;
import net.tourbook.common.UI;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class DialogSelectMeasurementSystem extends Dialog {
   private int _selectedSystemProfileIndex;

   /*
    * UI controls
    */
   private Combo _comboSystem_Profile;
   private Combo _comboSystemOptiop_Distance;
   private Combo _comboSystemOptiop_Elevation;
   private Combo _comboSystemOptiop_Height_Body;
   private Combo _comboSystemOptiop_Length;
   private Combo _comboSystemOptiop_Length_Small;
   private Combo _comboSystemOptiop_Pace;
   private Combo _comboSystemOptiop_Pressure_Atmosphere;
   private Combo _comboSystemOptiop_Temperature;
   private Combo _comboSystemOptiop_Weight;

   /**
    * @param parentShell
    * @param isSetSelectedSystem
    *           When <code>true</code> then the system is selected in the
    *           {@link MeasurementSystem_Manager}.
    */
   public DialogSelectMeasurementSystem(final Shell parentShell) {
      super(parentShell);
   }

   @Override
   public boolean close() {

      // keep selected index
      _selectedSystemProfileIndex = _comboSystem_Profile.getSelectionIndex();

      MeasurementSystem_Manager.setActiveSystemProfileIndex(_selectedSystemProfileIndex, true);

      return super.close();
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.Measurement_System_Dialog_Title);
   }

   @Override
   protected void createButtonsForButtonBar(final Composite parent) {

      createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite ui = createUI(parent);

      enableControls();
      fillSystemControls();

      // select default system which is the metric (first) profile in the first startup
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

            final Label label = new Label(container, SWT.WRAP);
            label.setText(Messages.Measurement_System_Dialog_Label_SelectSystem);

            GridDataFactory.fillDefaults()
                  .hint(convertWidthInCharsToPixels(40), SWT.DEFAULT)
                  .applyTo(label);
         }

         createUI_10_MeasurementSystem_Data(container);
      }

      return container;
   }

   private void createUI_10_MeasurementSystem_Data(final Composite parent) {

      final GridDataFactory gridData_Combo = GridDataFactory.fillDefaults().grab(true, false);
      final GridDataFactory gridData_Label = GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER);

      final SelectionListener profileListener = widgetSelectedAdapter(selectionEvent -> onSystemProfile_Select());

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(false, false)
            .indent(0, 16)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
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

            // spacer
            new Label(container, SWT.NONE);
         }
         {
            /*
             * Info
             */

            // vertical spacer
            UI.createSpacer_Vertical(container, 5, 3);

            new Label(container, SWT.NONE);
            new Label(container, SWT.NONE);

            // label
            final Label labelInfo = new Label(container, SWT.NONE);
            labelInfo.setText(Messages.Pref_System_Label_UsingInfo);
            labelInfo.setToolTipText(Messages.Pref_System_Label_UsingInfo_Tooltip);
            gridData_Label.applyTo(labelInfo);
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
            _comboSystemOptiop_Distance = new Combo(container, SWT.READ_ONLY);
            gridData_Combo.applyTo(_comboSystemOptiop_Distance);

            // label: info
            final Label labelInfo = new Label(container, SWT.NONE);
            labelInfo.setText(Messages.Pref_System_Label_Distance_Info);
            gridData_Label.applyTo(labelInfo);
         }
         {
            /*
             * Length
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_System_Label_Length);
            gridData_Label.applyTo(label);

            // combo
            _comboSystemOptiop_Length = new Combo(container, SWT.READ_ONLY);
            gridData_Combo.applyTo(_comboSystemOptiop_Length);

            // label: info
            final Label labelInfo = new Label(container, SWT.NONE);
            labelInfo.setText(Messages.Pref_System_Label_Length_Info);
            gridData_Label.applyTo(labelInfo);
         }
         {
            /*
             * Small length
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_System_Label_Length_Small);
            gridData_Label.applyTo(label);

            // combo
            _comboSystemOptiop_Length_Small = new Combo(container, SWT.READ_ONLY);
            gridData_Combo.applyTo(_comboSystemOptiop_Length_Small);

            // label: info
            final Label labelInfo = new Label(container, SWT.NONE);
            labelInfo.setText(Messages.Pref_System_Label_Length_Small_Info);
            gridData_Label.applyTo(labelInfo);
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
            _comboSystemOptiop_Elevation = new Combo(container, SWT.READ_ONLY);
            gridData_Combo.applyTo(_comboSystemOptiop_Elevation);

            // label: info
            final Label labelInfo = new Label(container, SWT.NONE);
            labelInfo.setText(Messages.Pref_System_Label_Elevation_Info);
            gridData_Label.applyTo(labelInfo);
         }
         {
            /*
             * Height
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_System_Label_Height);
            gridData_Label.applyTo(label);

            // combo
            _comboSystemOptiop_Height_Body = new Combo(container, SWT.READ_ONLY);
            gridData_Combo.applyTo(_comboSystemOptiop_Height_Body);

            // label: info
            final Label labelInfo = new Label(container, SWT.NONE);
            labelInfo.setText(Messages.Pref_System_Label_Height_Info);
            gridData_Label.applyTo(labelInfo);
         }
         {
            /*
             * Pace
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_System_Label_Pace);
            gridData_Label.applyTo(label);

            // combo
            _comboSystemOptiop_Pace = new Combo(container, SWT.READ_ONLY);
            gridData_Combo.applyTo(_comboSystemOptiop_Pace);

            // label: info
            final Label labelInfo = new Label(container, SWT.NONE);
            labelInfo.setText(Messages.Pref_System_Label_Pace_Info);
            gridData_Label.applyTo(labelInfo);
         }
         {
            /*
             * Weight
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_System_Label_Weight);
            gridData_Label.applyTo(label);

            // combo
            _comboSystemOptiop_Weight = new Combo(container, SWT.READ_ONLY);
            gridData_Combo.applyTo(_comboSystemOptiop_Weight);

            // label: info
            final Label labelInfo = new Label(container, SWT.NONE);
            labelInfo.setText(Messages.Pref_System_Label_Weight_Info);
            gridData_Label.applyTo(labelInfo);
         }
         {
            /*
             * Atmospheric pressure
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_System_Label_Pressure_Atmosphere);
            gridData_Label.applyTo(label);

            // combo
            _comboSystemOptiop_Pressure_Atmosphere = new Combo(container, SWT.READ_ONLY);
            gridData_Combo.applyTo(_comboSystemOptiop_Pressure_Atmosphere);

            // label: info
            final Label labelInfo = new Label(container, SWT.NONE);
            labelInfo.setText(Messages.Pref_System_Label_Pressure_Atmosphere_Info);
            gridData_Label.applyTo(labelInfo);
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
            _comboSystemOptiop_Temperature = new Combo(container, SWT.READ_ONLY);
            gridData_Combo.applyTo(_comboSystemOptiop_Temperature);

            new Label(container, SWT.NONE);
         }
      }
   }

   private void enableControls() {

      _comboSystemOptiop_Distance.setEnabled(false);
      _comboSystemOptiop_Elevation.setEnabled(false);
      _comboSystemOptiop_Height_Body.setEnabled(false);
      _comboSystemOptiop_Length.setEnabled(false);
      _comboSystemOptiop_Length_Small.setEnabled(false);
      _comboSystemOptiop_Pace.setEnabled(false);
      _comboSystemOptiop_Pressure_Atmosphere.setEnabled(false);
      _comboSystemOptiop_Temperature.setEnabled(false);
      _comboSystemOptiop_Weight.setEnabled(false);
   }

   private void fillSystemControls() {

      for (final MeasurementSystem systemProfile : MeasurementSystem_Manager.getCurrentProfiles()) {
         _comboSystem_Profile.add(systemProfile.getName());
      }

      for (final System_Distance systemDistance : MeasurementSystem_Manager.getAllSystem_Distances()) {
         _comboSystemOptiop_Distance.add(systemDistance.getLabel());
      }

      for (final System_Elevation systemElevation : MeasurementSystem_Manager.getAllSystem_Elevations()) {
         _comboSystemOptiop_Elevation.add(systemElevation.getLabel());
      }

      for (final System_Height systemHeight : MeasurementSystem_Manager.getAllSystem_Heights()) {
         _comboSystemOptiop_Height_Body.add(systemHeight.getLabel());
      }

      for (final System_Length systemElevation : MeasurementSystem_Manager.getAllSystem_Length()) {
         _comboSystemOptiop_Length.add(systemElevation.getLabel());
      }

      for (final System_LengthSmall systemElevation : MeasurementSystem_Manager.getAllSystem_Length_Small()) {
         _comboSystemOptiop_Length_Small.add(systemElevation.getLabel());
      }

      for (final System_Pace system : MeasurementSystem_Manager.getAllSystem_Pace()) {
         _comboSystemOptiop_Pace.add(system.getLabel());
      }

      for (final System_Pressure_Atmosphere system : MeasurementSystem_Manager.getAllSystem_Pressures_Atmospheric()) {
         _comboSystemOptiop_Pressure_Atmosphere.add(system.getLabel());
      }

      for (final System_Temperature systemTemperature : MeasurementSystem_Manager.getAllSystem_Temperatures()) {
         _comboSystemOptiop_Temperature.add(systemTemperature.getLabel());
      }

      for (final System_Weight systemWeight : MeasurementSystem_Manager.getAllSystem_Weights()) {
         _comboSystemOptiop_Weight.add(systemWeight.getLabel());
      }

   }

   public int getSelectedSystem() {
      return _selectedSystemProfileIndex;
   }

   private void onSystemProfile_Select() {

      final int activeSystemProfileIndex = _comboSystem_Profile.getSelectionIndex();
      final MeasurementSystem selectedSystemProfile = MeasurementSystem_Manager.getCurrentProfiles().get(activeSystemProfileIndex);

// SET_FORMATTING_OFF

      _comboSystemOptiop_Distance            .select(MeasurementSystem_Manager.getSystemIndex_Distance(selectedSystemProfile));
      _comboSystemOptiop_Elevation           .select(MeasurementSystem_Manager.getSystemIndex_Elevation(selectedSystemProfile));
      _comboSystemOptiop_Height_Body         .select(MeasurementSystem_Manager.getSystemIndex_Height(selectedSystemProfile));
      _comboSystemOptiop_Length              .select(MeasurementSystem_Manager.getSystemIndex_Length(selectedSystemProfile));
      _comboSystemOptiop_Length_Small        .select(MeasurementSystem_Manager.getSystemIndex_Length_Small(selectedSystemProfile));
      _comboSystemOptiop_Pace                .select(MeasurementSystem_Manager.getSystemIndex_Pace(selectedSystemProfile));
      _comboSystemOptiop_Pressure_Atmosphere .select(MeasurementSystem_Manager.getSystemIndex_Pressure_Atmosphere(selectedSystemProfile));
      _comboSystemOptiop_Temperature         .select(MeasurementSystem_Manager.getSystemIndex_Temperature(selectedSystemProfile));
      _comboSystemOptiop_Weight              .select(MeasurementSystem_Manager.getSystemIndex_Weight(selectedSystemProfile));

// SET_FORMATTING_ON
   }
}
