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
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageGeneral extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private StringFieldEditor		fTxtDecimalSep;
	private StringFieldEditor		fTxtGroupSep;
	private BooleanFieldEditor		fChkUseCustomFormat;
	private Group					fDecimalFormatContainer;

	private final IPreferenceStore	fPrefStore	= TourbookPlugin.getDefault().getPreferenceStore();
	private Label					fLblExample;
	private Label					fLblExampleValue;

	public void init(IWorkbench workbench) {
		setPreferenceStore(fPrefStore);
	}

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		Group measurementGroup = new Group(parent, SWT.NONE);
		measurementGroup.setText(Messages.Pref_general_system_measurement);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(measurementGroup);

		// checkbox: show in UI
		addField(new BooleanFieldEditor(ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI,
				Messages.Pref_general_show_system_in_ui,
				measurementGroup));

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

		// set margins after the editors are added
		GridLayout groupLayout = (GridLayout) measurementGroup.getLayout();
		groupLayout.marginWidth = 5;
		groupLayout.marginHeight = 5;

		/*
		 * decimal format
		 */
		fDecimalFormatContainer = new Group(parent, SWT.NONE);
		fDecimalFormatContainer.setText(Messages.pref_regional_title);
		GridDataFactory.fillDefaults().indent(0, 10).grab(true, false).applyTo(fDecimalFormatContainer);

		// label: description
		final Label label = new Label(fDecimalFormatContainer, SWT.WRAP);
		label.setText(Messages.pref_regional_description);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(label);

		// check: use custom format
		fChkUseCustomFormat = new BooleanFieldEditor(ITourbookPreferences.REGIONAL_USE_CUSTOM_DECIMAL_FORMAT,
				Messages.pref_regional_useCustomDecimalFormat,
				fDecimalFormatContainer);
		fChkUseCustomFormat.fillIntoGrid(fDecimalFormatContainer, 2);
		fChkUseCustomFormat.setPreferenceStore(fPrefStore);
		fChkUseCustomFormat.load();
		fChkUseCustomFormat.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				enableControls();
			}
		});

		// text: group separator
		fTxtGroupSep = new StringFieldEditor(ITourbookPreferences.REGIONAL_GROUP_SEPARATOR,
				Messages.pref_regional_groupSeparator,
				fDecimalFormatContainer);
		GridDataFactory.swtDefaults()
				.hint(15, SWT.DEFAULT)
				.applyTo(fTxtGroupSep.getTextControl(fDecimalFormatContainer));
		fTxtGroupSep.setTextLimit(1);
		fTxtGroupSep.setPreferenceStore(fPrefStore);
		fTxtGroupSep.load();
		fTxtGroupSep.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				setExampleValue();
			}
		});

		// text: decimal separator
		fTxtDecimalSep = new StringFieldEditor(ITourbookPreferences.REGIONAL_DECIMAL_SEPARATOR,
				Messages.pref_regional_decimalSeparator,
				fDecimalFormatContainer);
		GridDataFactory.swtDefaults()
				.hint(15, SWT.DEFAULT)
				.applyTo(fTxtDecimalSep.getTextControl(fDecimalFormatContainer));
		fTxtDecimalSep.setTextLimit(1);
		fTxtDecimalSep.setPreferenceStore(fPrefStore);
		fTxtDecimalSep.load();
		fTxtDecimalSep.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				setExampleValue();
			}
		});

		// label: value example
		fLblExample = new Label(fDecimalFormatContainer, SWT.NONE);
		fLblExample.setText(Messages.pref_regional_value_example);

		fLblExampleValue = new Label(fDecimalFormatContainer, SWT.NONE);
		fLblExampleValue.setText(UI.EMPTY_STRING);

		// add layout to the group
		GridLayout regionalLayout = (GridLayout) fDecimalFormatContainer.getLayout();
		regionalLayout.marginWidth = 5;
		regionalLayout.marginHeight = 5;

		enableControls();
		setExampleValue();
	}

	private void setExampleValue() {

		final String groupSep = fTxtGroupSep.getStringValue();

		StringBuilder buffer = new StringBuilder();
		buffer.append("123");
		buffer.append(groupSep);
		buffer.append("456");
		buffer.append(groupSep);
		buffer.append("789");
		buffer.append(fTxtDecimalSep.getStringValue());
		buffer.append("34");

		fLblExampleValue.setText(buffer.toString());
		fLblExampleValue.pack(true);
	}

	private void enableControls() {

		boolean isUseCustomFormat = fChkUseCustomFormat.getBooleanValue();

		fTxtDecimalSep.setEnabled(isUseCustomFormat, fDecimalFormatContainer);
		fTxtGroupSep.setEnabled(isUseCustomFormat, fDecimalFormatContainer);

		fLblExample.setEnabled(isUseCustomFormat);
		fLblExampleValue.setEnabled(isUseCustomFormat);
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();

		if (isOK) {

			/*
			 * property change listener does not work when the checkbox is defined as a field in the
			 * pref page, therefor storing the value is done manually
			 */
			fChkUseCustomFormat.store();
			fTxtDecimalSep.store();
			fTxtGroupSep.store();

			// fire one event for all modified measurement values
			getPreferenceStore().setValue(ITourbookPreferences.MEASUREMENT_SYSTEM, Math.random());
		}

		return isOK;
	}

}
