/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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

	private final IPreferenceStore	fPrefStore					= Activator.getDefault().getPreferenceStore();

	private Group					fGroupNotesImport;
	private BooleanFieldEditor		fChkImportIntoDescription;
	private BooleanFieldEditor2		fChkImportIntoTitle;
	private Button					fRdoImportAll;
	private Button					fRdoImportTruncated;
	private IntegerFieldEditor		fEditorTruncatedNotes;
	private Composite				fContainerCharacter;

	@Override
	protected void createFieldEditors() {

		createUI();

		restoreState();
		enableFields();
	}

	private void createUI() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		fGroupNotesImport = new Group(parent, SWT.NONE);
		fGroupNotesImport.setText(Messages.prefPage_tcx_group_importNotes);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fGroupNotesImport);
		{
			// label: description
			final Label label = new Label(fGroupNotesImport, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(label);
			label.setText(Messages.prefPage_tcx_label_importNotes);

			// check: import into title field
			fChkImportIntoTitle = new BooleanFieldEditor2(IPreferences.IS_IMPORT_INTO_TITLE_FIELD,
					Messages.prefPage_tcx_check_importIntoTitleField,
					fGroupNotesImport);
			fChkImportIntoTitle.fillIntoGrid(fGroupNotesImport, 2);
			fChkImportIntoTitle.setPreferenceStore(fPrefStore);
			fChkImportIntoTitle.load();
			addField(fChkImportIntoTitle);

			/*
			 * setPropertyChangeListener() is occupied by the pref page when the field is added to
			 * the page with addField
			 */
			final Button chkImportIntoTitle = fChkImportIntoTitle.getChangeControl(fGroupNotesImport);
			chkImportIntoTitle.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					enableFields();
				}
			});

			// container: title
			final Composite containerTitle = new Composite(fGroupNotesImport, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).grab(true, false).indent(15, 0).applyTo(containerTitle);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerTitle);
			{
				// radio: import all
				fRdoImportAll = new Button(containerTitle, SWT.RADIO);
				GridDataFactory.fillDefaults().span(2, 1).applyTo(fRdoImportAll);
				fRdoImportAll.setText(Messages.prefPage_tcx_radio_importIntoTitleAll);
				fRdoImportAll.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent event) {
						enableFields();
					}
				});

				// radio: import truncated
				fRdoImportTruncated = new Button(containerTitle, SWT.RADIO);
				fRdoImportTruncated.setText(Messages.prefPage_tcx_radio_importIntoTitleTruncated);
				fRdoImportTruncated.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent event) {
						enableFields();
					}
				});

				// editor: number of characters
				fContainerCharacter = new Composite(containerTitle, SWT.NONE);
				{
					fEditorTruncatedNotes = new IntegerFieldEditor(IPreferences.NUMBER_OF_TITLE_CHARACTERS,
							UI.EMPTY_STRING,
							fContainerCharacter);
					fEditorTruncatedNotes.setValidRange(10, 1000);
					fEditorTruncatedNotes.fillIntoGrid(fContainerCharacter, 2);
					fEditorTruncatedNotes.setPreferenceStore(fPrefStore);
					fEditorTruncatedNotes.load();
					UI.setFieldWidth(fContainerCharacter, fEditorTruncatedNotes, UI.DEFAULT_FIELD_WIDTH);
					addField(fEditorTruncatedNotes);
				}
			}

			// check: import into description field
			fChkImportIntoDescription = new BooleanFieldEditor(IPreferences.IS_IMPORT_INTO_DESCRIPTION_FIELD,
					Messages.prefPage_tcx_check_importIntoDescriptionField,
					fGroupNotesImport);
			fChkImportIntoDescription.fillIntoGrid(fGroupNotesImport, 2);
			fChkImportIntoDescription.setPreferenceStore(fPrefStore);
			fChkImportIntoDescription.load();
			addField(fChkImportIntoDescription);
		}

		// add layout to the group
		final GridLayout regionalLayout = (GridLayout) fGroupNotesImport.getLayout();
		regionalLayout.marginWidth = 5;
		regionalLayout.marginHeight = 5;
	}

	private void enableFields() {

		final boolean isTitleImport = fChkImportIntoTitle.getBooleanValue();
		final boolean isTruncateTitle = fRdoImportTruncated.getSelection();

		fRdoImportAll.setEnabled(isTitleImport);
		fRdoImportTruncated.setEnabled(isTitleImport);

		fEditorTruncatedNotes.setEnabled(isTitleImport && isTruncateTitle, fContainerCharacter);
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(fPrefStore);
	}

	@Override
	protected void performDefaults() {

		fChkImportIntoDescription.loadDefault();
		fChkImportIntoTitle.loadDefault();
		fEditorTruncatedNotes.loadDefault();

		if (fPrefStore.getDefaultBoolean(IPreferences.IS_TITLE_IMPORT_ALL)) {
			fRdoImportAll.setSelection(true);
			fRdoImportTruncated.setSelection(false);
		} else {
			fRdoImportAll.setSelection(false);
			fRdoImportTruncated.setSelection(true);
		}

		enableFields();
	}

	@Override
	public boolean performOk() {

		fPrefStore.setValue(IPreferences.IS_TITLE_IMPORT_ALL, fRdoImportAll.getSelection());

		return super.performOk();
	}

	private void restoreState() {

		if (fPrefStore.getBoolean(IPreferences.IS_TITLE_IMPORT_ALL)) {
			fRdoImportAll.setSelection(true);
			fRdoImportTruncated.setSelection(false);
		} else {
			fRdoImportAll.setSelection(false);
			fRdoImportTruncated.setSelection(true);
		}
	}

}
