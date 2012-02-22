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
package net.tourbook.photo;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.BooleanFieldEditor2;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPagePhotoViewer extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private boolean					_isEditorModified;

	/*
	 * UI controls
	 */
	private Composite				_containerFileFolder;

	private BooleanFieldEditor2		_chkEditorIsShowFileFolder;
	private ColorFieldEditor		_colorEditorFolder;
	private ColorFieldEditor		_colorEditorFile;

	@Override
	protected void createFieldEditors() {

		createUI();
	}

	private void createUI() {

		final Composite parent = getFieldEditorParent();
		{
			GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
			GridLayoutFactory.fillDefaults().applyTo(parent);

			createUI_10_Colors(parent);
		}
	}

	private void createUI_10_Colors(final Composite parent) {

		final Group colorGroup = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(colorGroup);
		GridLayoutFactory.fillDefaults()//
				.margins(5, 5)
				.spacing(30, LayoutConstants.getSpacing().y)
				.numColumns(2)
				.applyTo(colorGroup);
		colorGroup.setText(Messages.PrefPage_Photo_Viewer_Group_Colors);
		{
			final Composite containerLeft = new Composite(colorGroup, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(containerLeft);
			{
				// color: foreground
				addField(new ColorFieldEditor(
						ITourbookPreferences.PHOTO_VIEWER_COLOR_FOREGROUND,
						Messages.PrefPage_Photo_Viewer_Label_ForgroundColor,
						containerLeft));

				// color: background
				addField(new ColorFieldEditor(ITourbookPreferences.PHOTO_VIEWER_COLOR_BACKGROUND, //
						Messages.PrefPage_Photo_Viewer_Label_BackgroundColor,
						containerLeft));
			}

			_containerFileFolder = new Composite(colorGroup, SWT.NONE);
			{
				/*
				 * checkbox: show file/folder number
				 */
				_chkEditorIsShowFileFolder = new BooleanFieldEditor2(
						ITourbookPreferences.PHOTO_VIEWER_IS_SHOW_FILE_FOLDER,
						Messages.PrefPage_Photo_Viewer_Checkbox_ShowNumbersInFolderView,
						_containerFileFolder);
				addField(_chkEditorIsShowFileFolder);

				final Button editorLinesControl = _chkEditorIsShowFileFolder.getChangeControl(_containerFileFolder);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(editorLinesControl);
				editorLinesControl
						.setToolTipText(Messages.PrefPage_Photo_Viewer_Checkbox_ShowNumbersInFolderView_Tooltip);

				/*
				 * color: folder
				 */
				_colorEditorFolder = new ColorFieldEditor(
						ITourbookPreferences.PHOTO_VIEWER_COLOR_FOLDER,
						Messages.PrefPage_Photo_Viewer_Label_FolderColor,
						_containerFileFolder);
				addField(_colorEditorFolder);

				/*
				 * color: file
				 */
				_colorEditorFile = new ColorFieldEditor(
						ITourbookPreferences.PHOTO_VIEWER_COLOR_FILE,
						Messages.PrefPage_Photo_Viewer_Label_FileColor,
						_containerFileFolder);
				addField(_colorEditorFile);
			}
		}
	}

	private void enableControls() {

		final Button chkIsShowFileFolder = _chkEditorIsShowFileFolder.getChangeControl(_containerFileFolder);
		final boolean isChecked = chkIsShowFileFolder.getSelection();

		_colorEditorFile.getColorSelector().setEnabled(isChecked);
		_colorEditorFile.getLabelControl(_containerFileFolder).setEnabled(isChecked);

		_colorEditorFolder.getColorSelector().setEnabled(isChecked);
		_colorEditorFolder.getLabelControl(_containerFileFolder).setEnabled(isChecked);
	}

	private void fireModifyEvent() {

		if (_isEditorModified) {

			_isEditorModified = false;

			UI.setPhotoColorsFromPrefStore();

			// fire one event for all modified colors
			getPreferenceStore().setValue(ITourbookPreferences.PHOTO_VIEWER_PREF_STORE_EVENT, Math.random());
		}

	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	@Override
	protected void initialize() {
		
		super.initialize();
		
		enableControls();
	}

	@Override
	public boolean okToLeave() {

		if (_isEditorModified) {

			// save the colors in the pref store
			super.performOk();

			fireModifyEvent();
		}

		return super.okToLeave();
	}

	@Override
	protected void performDefaults() {

		_isEditorModified = true;

		super.performDefaults();

		enableControls();
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();

		if (isOK) {
			fireModifyEvent();
		}

		return isOK;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event) {

		_isEditorModified = true;

		if (event.getSource() == _chkEditorIsShowFileFolder) {
			enableControls();
		}

		super.propertyChange(event);
	}
}
