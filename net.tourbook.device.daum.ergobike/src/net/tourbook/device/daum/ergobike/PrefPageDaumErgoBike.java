/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.device.daum.ergobike;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
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

public class PrefPageDaumErgoBike extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private Group					_groupDecimalFormat;
	private StringFieldEditor		_txtDecimalSep;
	private StringFieldEditor		_txtGroupSep;
	private BooleanFieldEditor		_chkUseCustomFormat;

	private Label					_lblExample;
	private Label					_lblExampleValue;

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		/*
		 * decimal format
		 */
		_groupDecimalFormat = new Group(parent, SWT.NONE);
		_groupDecimalFormat.setText(Messages.pref_regional_title);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_groupDecimalFormat);
		{
			// check: use custom format
			_chkUseCustomFormat = new BooleanFieldEditor(
					ITourbookPreferences.REGIONAL_USE_CUSTOM_DECIMAL_FORMAT,
					Messages.pref_regional_useCustomDecimalFormat,
					_groupDecimalFormat);
			_chkUseCustomFormat.fillIntoGrid(_groupDecimalFormat, 2);
			_chkUseCustomFormat.setPreferenceStore(_prefStore);
			_chkUseCustomFormat.load();
			_chkUseCustomFormat.setPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					enableControls();
				}
			});

			// text: group separator
			_txtGroupSep = new StringFieldEditor(
					ITourbookPreferences.REGIONAL_GROUP_SEPARATOR,
					Messages.pref_regional_groupSeparator,
					_groupDecimalFormat);
			GridDataFactory
					.swtDefaults()
					.hint(15, SWT.DEFAULT)
					.applyTo(_txtGroupSep.getTextControl(_groupDecimalFormat));
			_txtGroupSep.setTextLimit(1);
			_txtGroupSep.setPreferenceStore(_prefStore);
			_txtGroupSep.load();
			_txtGroupSep.setPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					setExampleValue();
				}
			});

			// text: decimal separator
			_txtDecimalSep = new StringFieldEditor(
					ITourbookPreferences.REGIONAL_DECIMAL_SEPARATOR,
					Messages.pref_regional_decimalSeparator,
					_groupDecimalFormat);
			GridDataFactory
					.swtDefaults()
					.hint(15, SWT.DEFAULT)
					.applyTo(_txtDecimalSep.getTextControl(_groupDecimalFormat));
			_txtDecimalSep.setTextLimit(1);
			_txtDecimalSep.setPreferenceStore(_prefStore);
			_txtDecimalSep.load();
			_txtDecimalSep.setPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					setExampleValue();
				}
			});

			// label: value example
			_lblExample = new Label(_groupDecimalFormat, SWT.NONE);
			_lblExample.setText(Messages.pref_regional_value_example);

			_lblExampleValue = new Label(_groupDecimalFormat, SWT.NONE);
			_lblExampleValue.setText(UI.EMPTY_STRING);
		}

		// add layout to the group
		final GridLayout regionalLayout = (GridLayout) _groupDecimalFormat.getLayout();
		regionalLayout.marginWidth = 5;
		regionalLayout.marginHeight = 5;

		enableControls();
		setExampleValue();
	}

	private void enableControls() {

		final boolean isUseCustomFormat = _chkUseCustomFormat.getBooleanValue();

		_txtDecimalSep.setEnabled(isUseCustomFormat, _groupDecimalFormat);
		_txtGroupSep.setEnabled(isUseCustomFormat, _groupDecimalFormat);

		_lblExample.setEnabled(isUseCustomFormat);
		_lblExampleValue.setEnabled(isUseCustomFormat);
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);

	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();

		if (isOK) {

			/*
			 * property change listener does not work when the checkbox is defined as a field in the
			 * pref page, therefor storing the value is done manually
			 */
			_chkUseCustomFormat.store();
			_txtDecimalSep.store();
			_txtGroupSep.store();

			// fire one event for all modified measurement values
			getPreferenceStore().setValue(ITourbookPreferences.MEASUREMENT_SYSTEM, Math.random());
		}

		return isOK;
	}

	private void setExampleValue() {

		final String groupSep = _txtGroupSep.getStringValue();

		final StringBuilder buffer = new StringBuilder();
		buffer.append("123"); //$NON-NLS-1$
		buffer.append(groupSep);
		buffer.append("456"); //$NON-NLS-1$
		buffer.append(groupSep);
		buffer.append("789"); //$NON-NLS-1$
		buffer.append(_txtDecimalSep.getStringValue());
		buffer.append("34"); //$NON-NLS-1$

		_lblExampleValue.setText(buffer.toString());
		_lblExampleValue.pack(true);
	}
}
