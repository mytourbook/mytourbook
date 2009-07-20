/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class PrefPageGeneral extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private boolean				fShowMeasurementSystemInUI;
	private BooleanFieldEditor	fEditShowMeasurementInUI;

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		createUIMeasurementSystem(parent);
		createUIConfirmations(parent);

	}

	private void createUIConfirmations(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.pref_general_confirmation);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);

		// checkbox: confirm undo in tour editor
		addField(new BooleanFieldEditor(ITourbookPreferences.TOURDATA_EDITOR_CONFIRMATION_REVERT_TOUR,
				Messages.pref_general_hide_confirmation + Messages.tour_editor_dlg_revert_tour_message,
				group));

		// checkbox: confirm undo in tour editor
		addField(new BooleanFieldEditor(ITourbookPreferences.MAP_VIEW_CONFIRMATION_SHOW_DIM_WARNING,
				Messages.pref_general_hide_warning
				/*
				 * the externalize string wizard has problems when the messages are from 2 different
				 * packages, Eclipse 3.4
				 */
				+ net.tourbook.mapping.Messages.map_dlg_dim_warning_message//
				,
				group));

		// set margins after the editors are added
		final GridLayout groupLayout = (GridLayout) group.getLayout();
		groupLayout.marginWidth = 5;
		groupLayout.marginHeight = 5;
	}

	private void createUIMeasurementSystem(final Composite parent) {

		final Group measurementGroup = new Group(parent, SWT.NONE);
		measurementGroup.setText(Messages.Pref_general_system_measurement);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(measurementGroup);

		// radio: distance
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
				measurementGroup,
				true));

		// radio: altitude
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
				measurementGroup,
				true));

		// radio: temperature
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
				measurementGroup,
				true));

		// checkbox: show in UI
		addField(fEditShowMeasurementInUI = new BooleanFieldEditor(ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI,
				Messages.Pref_general_show_system_in_ui,
				measurementGroup));

		// set margins after the editors are added
		final GridLayout groupLayout = (GridLayout) measurementGroup.getLayout();
		groupLayout.marginWidth = 5;
		groupLayout.marginHeight = 5;
	}

	public void init(final IWorkbench workbench) {
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(prefStore);

		fShowMeasurementSystemInUI = prefStore.getBoolean(ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI);
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();
		if (isOK) {

			// fire one event for all modified measurement values
			getPreferenceStore().setValue(ITourbookPreferences.MEASUREMENT_SYSTEM, Math.random());

			if (fEditShowMeasurementInUI.getBooleanValue() != fShowMeasurementSystemInUI) {

				// field was modified, ask for restart

				if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
						Messages.pref_general_restart_app_title,
						Messages.pref_general_restart_app_message)) {

					Display.getCurrent().asyncExec(new Runnable() {
						public void run() {
							PlatformUI.getWorkbench().restart();
						}
					});
				}
			}

		}

		return isOK;
	}
}
