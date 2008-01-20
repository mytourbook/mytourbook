/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

package net.tourbook.preferences;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageGeneral extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public void init(IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();

		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Pref_general_system_measurement);

		addField(new RadioGroupFieldEditor(ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE,
				Messages.Pref_general_system_distance,
				2,
				new String[][] {
						new String[] {
								Messages.Pref_general_metric_unit_km,
								ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_KM },
						new String[] {
								Messages.Pref_general_imperial_unit_mi,
								ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_MI }, },
				parent,
				true));

		addField(new RadioGroupFieldEditor(ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE,
				Messages.Pref_general_system_altitude,
				2,
				new String[][] {
						new String[] {
								Messages.Pref_general_metric_unit_m,
								ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE_M },
						new String[] {
								Messages.Pref_general_imperial_unit_feet,
								ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE_FOOT }, },
				parent,
				true));

		addField(new RadioGroupFieldEditor(ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPERATURE,
				Messages.Pref_general_system_temperature,
				2,
				new String[][] {
						new String[] {
								Messages.Pref_general_metric_unit_celcius,
								ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPERATURE_C },
						new String[] {
								Messages.Pref_general_imperial_unit_fahrenheit,
								ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPTERATURE_F }, },
				parent,
				true));

		BooleanFieldEditor showInUI = new BooleanFieldEditor(ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI,
				Messages.Pref_general_show_system_in_ui,
				parent);
		addField(showInUI);
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();

		if (isOK) {
			// fire one event for all modified measurement values
			getPreferenceStore().setValue(ITourbookPreferences.MEASUREMENT_SYSTEM, Math.random());
		}

		return isOK;
	}

}
