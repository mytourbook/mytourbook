/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.device.garmin;

import net.tourbook.common.UI;
import net.tourbook.common.preferences.BooleanFieldEditor2;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageTCX extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String		IMPORT_INTO_TITLE_TRUNCATED	= "truncated";									//$NON-NLS-1$
	public static final String		IMPORT_INTO_TITLE_ALL		= "all";										//$NON-NLS-1$

	private final IPreferenceStore	_prefStore					= Activator.getDefault().getPreferenceStore();

	private Composite				_containerCharacter;

	private Group					_groupNotesImport;

	private BooleanFieldEditor		_boolean_ImportIntoDescription;
	private BooleanFieldEditor2		_boolean_ImportIntoTitle;

	private IntegerFieldEditor		_integer_TruncatedNotes;

	private Button					_rdoImportAll;
	private Button					_rdoImportTruncated;

	@Override
	protected void createFieldEditors() {

		createUI();

		restoreState();
		enableFields();
	}

	private void createUI() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		_groupNotesImport = new Group(parent, SWT.NONE);
		_groupNotesImport.setText(Messages.prefPage_tcx_group_importNotes);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_groupNotesImport);
		{
			// label: description
			final Label label = new Label(_groupNotesImport, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(label);
			label.setText(Messages.prefPage_tcx_label_importNotes);

			// check: import into title field
			_boolean_ImportIntoTitle = new BooleanFieldEditor2(
					IPreferences.IS_IMPORT_INTO_TITLE_FIELD,
					Messages.prefPage_tcx_check_importIntoTitleField,
					_groupNotesImport);
			_boolean_ImportIntoTitle.fillIntoGrid(_groupNotesImport, 2);
			_boolean_ImportIntoTitle.setPreferenceStore(_prefStore);
			_boolean_ImportIntoTitle.load();
			addField(_boolean_ImportIntoTitle);

			/*
			 * setPropertyChangeListener() is occupied by the pref page when the field is added to
			 * the page with addField
			 */
			final Button chkImportIntoTitle = _boolean_ImportIntoTitle.getChangeControl(_groupNotesImport);
			chkImportIntoTitle.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					enableFields();
				}
			});

			// container: title
			final Composite containerTitle = new Composite(_groupNotesImport, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).grab(true, false).indent(15, 0).applyTo(containerTitle);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerTitle);
			{
				// radio: import all
				_rdoImportAll = new Button(containerTitle, SWT.RADIO);
				GridDataFactory.fillDefaults().span(2, 1).applyTo(_rdoImportAll);
				_rdoImportAll.setText(Messages.prefPage_tcx_radio_importIntoTitleAll);
				_rdoImportAll.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent event) {
						enableFields();
					}
				});

				// radio: import truncated
				_rdoImportTruncated = new Button(containerTitle, SWT.RADIO);
				_rdoImportTruncated.setText(Messages.prefPage_tcx_radio_importIntoTitleTruncated);
				_rdoImportTruncated.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent event) {
						enableFields();
					}
				});

				// editor: number of characters
				_containerCharacter = new Composite(containerTitle, SWT.NONE);
				{
					_integer_TruncatedNotes = new IntegerFieldEditor(
							IPreferences.NUMBER_OF_TITLE_CHARACTERS,
							UI.EMPTY_STRING,
							_containerCharacter);
					_integer_TruncatedNotes.setValidRange(10, 1000);
					_integer_TruncatedNotes.fillIntoGrid(_containerCharacter, 2);
					_integer_TruncatedNotes.setPreferenceStore(_prefStore);
					_integer_TruncatedNotes.load();
					UI.setFieldWidth(_containerCharacter, _integer_TruncatedNotes, UI.DEFAULT_FIELD_WIDTH);
					addField(_integer_TruncatedNotes);
				}
			}

			// check: import into description field
			_boolean_ImportIntoDescription = new BooleanFieldEditor(
					IPreferences.IS_IMPORT_INTO_DESCRIPTION_FIELD,
					Messages.prefPage_tcx_check_importIntoDescriptionField,
					_groupNotesImport);
			_boolean_ImportIntoDescription.fillIntoGrid(_groupNotesImport, 2);
			_boolean_ImportIntoDescription.setPreferenceStore(_prefStore);
			_boolean_ImportIntoDescription.load();
			addField(_boolean_ImportIntoDescription);
		}

		// add layout to the group
		final GridLayout regionalLayout = (GridLayout) _groupNotesImport.getLayout();
		regionalLayout.marginWidth = 5;
		regionalLayout.marginHeight = 5;
	}

	private void enableFields() {

		final boolean isTitleImport = _boolean_ImportIntoTitle.getBooleanValue();
		final boolean isTruncateTitle = _rdoImportTruncated.getSelection();

		_rdoImportAll.setEnabled(isTitleImport);
		_rdoImportTruncated.setEnabled(isTitleImport);

		_integer_TruncatedNotes.setEnabled(isTitleImport && isTruncateTitle, _containerCharacter);
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	@Override
	protected void performDefaults() {

		_boolean_ImportIntoDescription.loadDefault();
		_boolean_ImportIntoTitle.loadDefault();
		_integer_TruncatedNotes.loadDefault();

		if (_prefStore.getDefaultBoolean(IPreferences.IS_TITLE_IMPORT_ALL)) {
			_rdoImportAll.setSelection(true);
			_rdoImportTruncated.setSelection(false);
		} else {
			_rdoImportAll.setSelection(false);
			_rdoImportTruncated.setSelection(true);
		}

		enableFields();
	}

	@Override
	public boolean performOk() {

		_prefStore.setValue(IPreferences.IS_TITLE_IMPORT_ALL, _rdoImportAll.getSelection());

		return super.performOk();
	}

	private void restoreState() {

		if (_prefStore.getBoolean(IPreferences.IS_TITLE_IMPORT_ALL)) {
			_rdoImportAll.setSelection(true);
			_rdoImportTruncated.setSelection(false);
		} else {
			_rdoImportAll.setSelection(false);
			_rdoImportTruncated.setSelection(true);
		}
	}

}
