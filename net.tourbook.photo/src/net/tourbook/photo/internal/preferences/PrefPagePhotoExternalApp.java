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
package net.tourbook.photo.internal.preferences;

import net.tourbook.common.UI;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.internal.Activator;
import net.tourbook.photo.internal.Messages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPagePhotoExternalApp extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String			ID			= "net.tourbook.preferences.PrefPagePhotoExternalAppID";	//$NON-NLS-1$

	private final IPreferenceStore		_prefStore	= Activator.getDefault().getPreferenceStore();

	/*
	 * UI controls
	 */
	private FileFieldEditorNoValidation	_editorExternalPhotoViewer1;
	private FileFieldEditorNoValidation	_editorExternalPhotoViewer2;
	private FileFieldEditorNoValidation	_editorExternalPhotoViewer3;

	public class FileFieldEditorNoValidation extends FileFieldEditor {

		public FileFieldEditorNoValidation(final String name, final String labelText, final Composite parent) {
			super(name, labelText, parent);
		}

		@Override
		protected boolean checkState() {
			return true;
		}
	}

	public PrefPagePhotoExternalApp() {
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
			createUI_30_ExternalPhotoViewer(parent);
		}
	}

	/**
	 * field: path to save raw tour data
	 */
	private void createUI_30_ExternalPhotoViewer(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(group);
		group.setText(Messages.PrefPage_Photo_ExtViewer_Group_ExternalApplication);
		{
			/*
			 * label: info
			 */
			Label label = new Label(group, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.indent(0, 5)
					.hint(UI.DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
					.applyTo(label);
			label.setText(Messages.PrefPage_Photo_ExtViewer_Label_Info);

			// spacer
			final Canvas spacer = new Canvas(group, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.indent(0, 10)
					.hint(1, 1)
					.applyTo(spacer);

			// App 1
			{
				/*
				 * editor: external file browser
				 */
				_editorExternalPhotoViewer1 = new FileFieldEditorNoValidation(
						IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_1,
						Messages.PrefPage_Photo_ExtViewer_Label_ExternalApplication1,
						group);
				_editorExternalPhotoViewer1.setEmptyStringAllowed(true);
				_editorExternalPhotoViewer1.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);

				label = _editorExternalPhotoViewer1.getLabelControl(group);
				label.setToolTipText(Messages.PrefPage_Photo_ExtViewer_Label_ExternalApplication_Tooltip);

				addField(_editorExternalPhotoViewer1);
			}

			// App 2
			{
				/*
				 * editor: external file browser
				 */
				_editorExternalPhotoViewer2 = new FileFieldEditorNoValidation(
						IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_2,
						Messages.PrefPage_Photo_ExtViewer_Label_ExternalApplication2,
						group);
				_editorExternalPhotoViewer2.setEmptyStringAllowed(true);
				_editorExternalPhotoViewer2.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);

				label = _editorExternalPhotoViewer2.getLabelControl(group);
				label.setToolTipText(Messages.PrefPage_Photo_ExtViewer_Label_ExternalApplication_Tooltip);

				addField(_editorExternalPhotoViewer2);
			}

			// App 3
			{
				/*
				 * editor: external file browser
				 */
				_editorExternalPhotoViewer3 = new FileFieldEditorNoValidation(
						IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_3,
						Messages.PrefPage_Photo_ExtViewer_Label_ExternalApplication3,
						group);
				_editorExternalPhotoViewer3.setEmptyStringAllowed(true);
				_editorExternalPhotoViewer3.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);

				label = _editorExternalPhotoViewer3.getLabelControl(group);
				label.setToolTipText(Messages.PrefPage_Photo_ExtViewer_Label_ExternalApplication_Tooltip);

				addField(_editorExternalPhotoViewer3);
			}
		}

		// set layout after the fields are created
		GridLayoutFactory.swtDefaults().numColumns(3).extendedMargins(0, 0, 0, 0).applyTo(group);

		/*
		 * set width for the text control that the pref dialog is not as wide as the full path
		 */
		final int pathWidth = 200;

		Text rawPathControl = _editorExternalPhotoViewer1.getTextControl(group);
		GridData gd = (GridData) rawPathControl.getLayoutData();
		gd.widthHint = pathWidth;

		rawPathControl = _editorExternalPhotoViewer2.getTextControl(group);
		gd = (GridData) rawPathControl.getLayoutData();
		gd.widthHint = pathWidth;

		rawPathControl = _editorExternalPhotoViewer3.getTextControl(group);
		gd = (GridData) rawPathControl.getLayoutData();
		gd.widthHint = pathWidth;
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}
}
