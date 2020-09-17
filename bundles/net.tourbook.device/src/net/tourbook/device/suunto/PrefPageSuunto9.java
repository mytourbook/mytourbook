/*******************************************************************************
 * Copyright (C) 2018, 2020 Frédéric Bard
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
package net.tourbook.device.suunto;

import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageSuunto9 extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   private final static String[]  AltitudeData = new String[] {
         Messages.pref_altitude_gps,
         Messages.pref_altitude_barometer
   };
   private final static String[]  DistanceData = new String[] {
         Messages.pref_distance_gps,
         Messages.pref_distance_providedvalues
   };

   private final IPreferenceStore _prefStore   = TourbookPlugin.getDefault().getPreferenceStore();

   /*
    * UI controls
    */
   private Group _groupData;

   private Combo _comboAltitudeDataSource;
   private Combo _comboDistanceDataSource;

   private Label _lblAltitudeDataSource;
   private Label _lblDistanceDataSource;

   @Override
   protected void createFieldEditors() {

      createUI();

      setupUI();

   }

   private void createUI() {

      final Composite parent = getFieldEditorParent();
      GridLayoutFactory.fillDefaults().applyTo(parent);

      /*
       * Data
       */
      _groupData = new Group(parent, SWT.NONE);
      _groupData.setText(Messages.pref_data_source);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_groupData);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_groupData);
      {
         // label: Altitude data source
         _lblAltitudeDataSource = new Label(_groupData, SWT.NONE);
         _lblAltitudeDataSource.setText(Messages.pref_altitude_source);

         /*
          * combo: Altitude source
          */
         _comboAltitudeDataSource = new Combo(_groupData, SWT.READ_ONLY | SWT.BORDER);
         _comboAltitudeDataSource.setVisibleItemCount(2);

         // label: Distance data source
         _lblDistanceDataSource = new Label(_groupData, SWT.NONE);
         _lblDistanceDataSource.setText(Messages.pref_distance_source);

         /*
          * combo: Distance source
          */
         _comboDistanceDataSource = new Combo(_groupData, SWT.READ_ONLY | SWT.BORDER);
         _comboDistanceDataSource.setVisibleItemCount(2);
      }
   }

   @Override
   public void init(final IWorkbench workbench) {}

   @Override
   protected void performDefaults() {

      _comboAltitudeDataSource.select(1);
      _comboDistanceDataSource.select(0);
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {
         _prefStore.setValue(IPreferences.ALTITUDE_DATA_SOURCE, _comboAltitudeDataSource.getSelectionIndex());
         _prefStore.setValue(IPreferences.DISTANCE_DATA_SOURCE, _comboDistanceDataSource.getSelectionIndex());
      }
      return isOK;
   }

   private void setupUI() {

      /*
       * Fill-up the altitude data choices
       */
      for (final String altitudeDataType : AltitudeData) {
         _comboAltitudeDataSource.add(altitudeDataType);
      }
      _comboAltitudeDataSource.select(_prefStore.getInt(IPreferences.ALTITUDE_DATA_SOURCE));

      /*
       * Fill-up the distance data choices
       */
      for (final String distanceDataType : DistanceData) {
         _comboDistanceDataSource.add(distanceDataType);
      }
      _comboDistanceDataSource.select(_prefStore.getInt(IPreferences.DISTANCE_DATA_SOURCE));
   }

}
