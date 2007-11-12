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

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageGeneral extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private RadioGroupFieldEditor	fEditorSystemOfMeasurement;

//	private Button	fRadioMetric;
//	private Button	fRadioImperial;

	@Override
	protected Control createContents(Composite parent) {

		Control container = createUI(parent);

		return container;
	}

	private Control createUI(Composite parent) {

		Control container = super.createContents(parent);

//		Point margins = LayoutConstants.getMargins();
//
//		Composite container = new Composite(parent, SWT.NONE);
//		GridLayoutFactory.fillDefaults().applyTo(container);
//
//		Group group = new Group(container, SWT.NONE);
//		group.setText("System of Measurement");
//		GridLayoutFactory.fillDefaults().margins(margins).applyTo(group);
//
//		fRadioMetric = new Button(group, SWT.RADIO);
//		fRadioMetric.setText("Metric Units");
//		fRadioMetric.setToolTipText("m, km");
//
//		fRadioImperial = new Button(group, SWT.RADIO);
//		fRadioImperial.setText("Imperial Units");
//		fRadioImperial.setToolTipText("feet, miles");

		return container;
	}

	public void init(IWorkbench workbench) {
		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {

		fEditorSystemOfMeasurement = new RadioGroupFieldEditor(ITourbookPreferences.MEASUREMENT_SYSTEM,
				Messages.Pref_general_system_of_measurement,
				1,
				new String[][] {
						new String[] { Messages.Pref_general_metric_units,//"Metric Units (m, km)",
								ITourbookPreferences.MEASUREMENT_SYSTEM_METRIC },
						new String[] { Messages.Pref_general_metric_imperial,//"Imperial Units (feet, mile)",
								ITourbookPreferences.MEASUREMENT_SYSTEM_IMPERIAL }, },
				getFieldEditorParent(),
				true);

		addField(fEditorSystemOfMeasurement);

//		fEditorSystemOfMeasurement.setPropertyChangeListener(new IPropertyChangeListener() {
//			public void propertyChange(PropertyChangeEvent event) {
//				Object newValue = event.getNewValue();
//				if (newValue == null) {
//
//				}
//			}
//		});
	}

//	@Override
//	public boolean performOk() {
//
//		final boolean isOK = super.performOk();
//
//		if (isOK) {
//			UI.setUnits();
//		}
//
//		return isOK;
//	}

}
