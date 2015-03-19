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
package net.tourbook.web.preferences;

import net.tourbook.common.UI;
import net.tourbook.web.Activator;
import net.tourbook.web.Messages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageWebBrowser extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String			ID			= "net.tourbook.web.preferences.PrefPageWebBrowser";	//$NON-NLS-1$

	private final IPreferenceStore		_prefStore	= Activator.getDefault().getPreferenceStore();

	/*
	 * UI controls
	 */
	private FileFieldEditorNoValidation	_editorWebBrowser;

	public class FileFieldEditorNoValidation extends FileFieldEditor {

		/**
		 * {@inheritDoc}
		 */
		public FileFieldEditorNoValidation(final String name, final String labelText, final Composite parent) {
			super(name, labelText, parent);
		}

		@Override
		protected boolean checkState() {
			return true;
		}
	}

	public PrefPageWebBrowser() {
//		noDefaultAndApplyButton();
	}

	@Override
	protected void createFieldEditors() {
		createUI();
	}

	private void createUI() {

		final Composite parent = getFieldEditorParent();

		GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
		GridLayoutFactory.fillDefaults().applyTo(parent);
//		parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_10_WebBrowser(parent);
		}
	}

	/**
	 * field: path to save raw tour data
	 */
	private void createUI_10_WebBrowser(final Composite parent) {

		{
			/*
			 * label: info
			 */
			final Label label = new Label(parent, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.hint(UI.DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
					.applyTo(label);
			label.setText(Messages.PrefPage_Web_Label_ExternalWebBrowser_Info);
			{
				/*
				 * editor: external web browser
				 */
				_editorWebBrowser = new FileFieldEditorNoValidation(
						IWebPreferences.EXTERNAL_WEB_BROWSER,
						Messages.PrefPage_Web_Label_ExternalWebBrowser,
						parent);
				_editorWebBrowser.setEmptyStringAllowed(true);
				_editorWebBrowser.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);

				addField(_editorWebBrowser);
			}
		}

		// set layout after the fields are created
		GridLayoutFactory.fillDefaults()//
				.numColumns(3)
				.applyTo(parent);

		/*
		 * set width for the text control that the pref dialog is not as wide as the full path
		 */
		final int pathWidth = 200;

		final Text rawPathControl = _editorWebBrowser.getTextControl(parent);
		final GridData gd = (GridData) rawPathControl.getLayoutData();
		gd.widthHint = pathWidth;

//		parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}
}
