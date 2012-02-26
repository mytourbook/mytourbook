/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageGeneralExternalPrograms extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String	ID	= "net.tourbook.preferences.PrefPageGeneralExternalProgramsID"; //$NON-NLS-1$

	private IPreferenceStore	_prefStore;

	private FileFieldEditor		_editorExternalFileBrowser;

	/*
	 * UI controls
	 */

	@Override
	protected void createFieldEditors() {

		createUI();

		restoreState();
		enableControls();
	}

	private void createUI() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);
		{
			createUI_100_ExternalExplorer(parent);
		}
	}

	/**
	 * field: path to save raw tour data
	 */
	private void createUI_100_ExternalExplorer(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(group);
		{
			/*
			 * label: info
			 */
			final Label label = new Label(group, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
//					.indent(0, 15)
					.hint(UI.DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
					.applyTo(label);
			label.setText(Messages.Pref_ExtPrograms_Label_Info);

			/*
			 * editor: external file browser
			 */
			_editorExternalFileBrowser = new FileFieldEditor(
					ITourbookPreferences.DUMMY_FIELD,
					Messages.Pref_ExtPrograms_Label_ExplorerPath,
					group);
			_editorExternalFileBrowser.setEmptyStringAllowed(true);
			_editorExternalFileBrowser.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);

			final Label lblPath = _editorExternalFileBrowser.getLabelControl(group);
			lblPath.setToolTipText(Messages.Pref_People_Label_DefaultDataTransferFilePath_Tooltip);
		}

		// set layout after the fields are created
		GridLayoutFactory.swtDefaults().numColumns(3).extendedMargins(0, 0, 7, 0).applyTo(group);

		/*
		 * set width for the text control that the pref dialog is not as wide as the full path
		 */
		final Text rawPathControl = _editorExternalFileBrowser.getTextControl(group);
		final GridData gd = (GridData) rawPathControl.getLayoutData();
		gd.widthHint = 200;
	}

	private void enableControls() {

	}

	public void init(final IWorkbench workbench) {

		_prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		setPreferenceStore(_prefStore);
	}

	@Override
	public boolean okToLeave() {

		return super.okToLeave();
	}

	@Override
	protected void performDefaults() {

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();
		if (isOK) {

			saveState();
		}

		return isOK;
	}

	private void restoreState() {

	}

	private void saveState() {

	}
}
