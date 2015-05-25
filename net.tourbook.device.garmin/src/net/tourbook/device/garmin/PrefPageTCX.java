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
import org.eclipse.jface.layout.PixelConverter;
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

	private SelectionAdapter		_defaultSelectionListener;

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private Composite				_containerCharacter;

	private Group					_groupNotesImport;

	private BooleanFieldEditor2		_editBool_IgnoreSpeedValues;
	private BooleanFieldEditor		_editBool_ImportIntoDescription;
	private BooleanFieldEditor2		_editBool_ImportIntoTitle;

	private IntegerFieldEditor		_editInt_TruncatedNotes;

	private Label					_lblIgnoreSpeed;

	private Button					_chkIgnoreSpeed;
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

		initUI(parent);

		createUI_10_Notes(parent);
		createUI_20_Other(parent);
	}

	private void createUI_10_Notes(final Composite parent) {

		_groupNotesImport = new Group(parent, SWT.NONE);
		_groupNotesImport.setText(Messages.prefPage_tcx_group_importNotes);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_groupNotesImport);
		{
			// label: description
			final Label label = new Label(_groupNotesImport, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(label);
			label.setText(Messages.prefPage_tcx_label_importNotes);

			// check: import into title field
			_editBool_ImportIntoTitle = new BooleanFieldEditor2(
					IPreferences.IS_IMPORT_INTO_TITLE_FIELD,
					Messages.prefPage_tcx_check_importIntoTitleField,
					_groupNotesImport);
			_editBool_ImportIntoTitle.fillIntoGrid(_groupNotesImport, 2);
			_editBool_ImportIntoTitle.setPreferenceStore(_prefStore);
			_editBool_ImportIntoTitle.load();
			addField(_editBool_ImportIntoTitle);

			/*
			 * setPropertyChangeListener() is occupied by the pref page when the field is added to
			 * the page with addField
			 */
			final Button chkImportIntoTitle = _editBool_ImportIntoTitle.getChangeControl(_groupNotesImport);
			chkImportIntoTitle.addSelectionListener(_defaultSelectionListener);

			// container: title
			final Composite containerTitle = new Composite(_groupNotesImport, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).grab(true, false).indent(15, 0).applyTo(containerTitle);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerTitle);
			{
				// radio: import all
				_rdoImportAll = new Button(containerTitle, SWT.RADIO);
				GridDataFactory.fillDefaults().span(2, 1).applyTo(_rdoImportAll);
				_rdoImportAll.setText(Messages.prefPage_tcx_radio_importIntoTitleAll);
				_rdoImportAll.addSelectionListener(_defaultSelectionListener);

				// radio: import truncated
				_rdoImportTruncated = new Button(containerTitle, SWT.RADIO);
				_rdoImportTruncated.setText(Messages.prefPage_tcx_radio_importIntoTitleTruncated);
				_rdoImportTruncated.addSelectionListener(_defaultSelectionListener);

				// editor: number of characters
				_containerCharacter = new Composite(containerTitle, SWT.NONE);
				{
					_editInt_TruncatedNotes = new IntegerFieldEditor(
							IPreferences.NUMBER_OF_TITLE_CHARACTERS,
							UI.EMPTY_STRING,
							_containerCharacter);
					_editInt_TruncatedNotes.setValidRange(10, 1000);
					_editInt_TruncatedNotes.fillIntoGrid(_containerCharacter, 2);
					_editInt_TruncatedNotes.setPreferenceStore(_prefStore);
					_editInt_TruncatedNotes.load();
					UI.setFieldWidth(_containerCharacter, _editInt_TruncatedNotes, UI.DEFAULT_FIELD_WIDTH);
					addField(_editInt_TruncatedNotes);
				}
			}

			// check: import into description field
			_editBool_ImportIntoDescription = new BooleanFieldEditor(
					IPreferences.IS_IMPORT_INTO_DESCRIPTION_FIELD,
					Messages.prefPage_tcx_check_importIntoDescriptionField,
					_groupNotesImport);
			_editBool_ImportIntoDescription.fillIntoGrid(_groupNotesImport, 2);
			_editBool_ImportIntoDescription.setPreferenceStore(_prefStore);
			_editBool_ImportIntoDescription.load();
			addField(_editBool_ImportIntoDescription);
		}

		// add layout to the group
		final GridLayout regionalLayout = (GridLayout) _groupNotesImport.getLayout();
		regionalLayout.marginWidth = 5;
		regionalLayout.marginHeight = 5;
	}

	private void createUI_20_Other(final Composite parent) {

		/*
		 * Check: Ignore speed values
		 */
		_editBool_IgnoreSpeedValues = new BooleanFieldEditor2(
				IPreferences.IS_IGNORE_SPEED_VALUES,
				Messages.PrefPage_TCX_Check_IgnoreSpeedValues,
				parent);
		_editBool_IgnoreSpeedValues.fillIntoGrid(parent, 1);
		_editBool_IgnoreSpeedValues.setPreferenceStore(_prefStore);
		_editBool_IgnoreSpeedValues.load();
		addField(_editBool_IgnoreSpeedValues);

		_chkIgnoreSpeed = _editBool_IgnoreSpeedValues.getChangeControl(parent);
		_chkIgnoreSpeed.addSelectionListener(_defaultSelectionListener);

		/*
		 * Label: Info
		 */
		_lblIgnoreSpeed = new Label(parent, SWT.WRAP);
		GridDataFactory.fillDefaults()//
				.indent(_pc.convertHorizontalDLUsToPixels(10), 0)
				.hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
				.applyTo(_lblIgnoreSpeed);
		_lblIgnoreSpeed.setText(Messages.PrefPage_TCX_Label_IgnoreSpeedValues);
	}

	private void enableFields() {

		final boolean isTitleImport = _editBool_ImportIntoTitle.getBooleanValue();
		final boolean isTruncateTitle = _rdoImportTruncated.getSelection();

		_lblIgnoreSpeed.setEnabled(_chkIgnoreSpeed.getSelection());
		
		_rdoImportAll.setEnabled(isTitleImport);
		_rdoImportTruncated.setEnabled(isTitleImport);

		_editInt_TruncatedNotes.setEnabled(isTitleImport && isTruncateTitle, _containerCharacter);
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableFields();
			}
		};
	}

	@Override
	protected void performDefaults() {

		_editBool_IgnoreSpeedValues.loadDefault();
		_editBool_ImportIntoDescription.loadDefault();
		_editBool_ImportIntoTitle.loadDefault();

		_editInt_TruncatedNotes.loadDefault();

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
