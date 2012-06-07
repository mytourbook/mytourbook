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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPagePhotoFullsizeViewer extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String		ID			= "net.tourbook.preferences.PrefPagePhotoFullsizeViewerID"; //$NON-NLS-1$

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private boolean					_isModified;

	private SelectionAdapter		_defaultSelectionListener;

	/*
	 * UI controls
	 */
	private Button					_chkThumbPreview;
	private Button					_chkLoadingMessage;
	private Button					_chkHQImage;

	@Override
	protected void createFieldEditors() {

		initUI();
		createUI();
	}

	private void createUI() {

		final Composite parent = getFieldEditorParent();
		GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
		GridLayoutFactory.fillDefaults().applyTo(parent);
//		parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_10_ThumbPreview(parent);
		}
	}

	private void createUI_10_ThumbPreview(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(1)
//				.spacing(0, 0)
				.applyTo(container);
		{
			// checkbox: smooth image
			_chkHQImage = new Button(container, SWT.CHECK);
			_chkHQImage.setText(Messages.PrefPage_Photo_FullsizeViewer_Checkbox_HQImage);
			_chkHQImage.setToolTipText(//
					Messages.PrefPage_Photo_FullsizeViewer_Checkbox_HQImage_Tooltip);
			_chkHQImage.addSelectionListener(_defaultSelectionListener);

			// checkbox: show loading message
			_chkLoadingMessage = new Button(container, SWT.CHECK);
			_chkLoadingMessage.setText(Messages.PrefPage_Photo_FullsizeViewer_Checkbox_ShowLoadingMessage);
			_chkLoadingMessage.setToolTipText(//
					Messages.PrefPage_Photo_FullsizeViewer_Checkbox_ShowLoadingMessage_Tooltip);
			_chkLoadingMessage.addSelectionListener(_defaultSelectionListener);

			// checkbox: thumb preview
			_chkThumbPreview = new Button(container, SWT.CHECK);
			_chkThumbPreview.setText(Messages.PrefPage_Photo_FullsizeViewer_Checkbox_Preview);
			_chkThumbPreview.setToolTipText(Messages.PrefPage_Photo_FullsizeViewer_Checkbox_Preview_Tooltip);
			_chkThumbPreview.addSelectionListener(_defaultSelectionListener);
		}

	}

	private void enableControls() {

	}

	private void fireModifyEvent() {

		if (_isModified) {

			_isModified = false;

			getPreferenceStore().setValue(
					ITourbookPreferences.PHOTO_VIEWER_PREF_EVENT_FULLSIZE_VIEWER_IS_MODIFIED,
					Math.random());
		}
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	@Override
	protected void initialize() {

		super.initialize();

		restoreState();

		enableControls();
	}

	private void initUI() {

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				_isModified = true;
			}
		};
	}

	@Override
	public boolean okToLeave() {

		if (_isModified) {

			saveState();

			// save the colors in the pref store
			super.performOk();

			fireModifyEvent();
		}

		return super.okToLeave();
	}

	@Override
	protected void performDefaults() {

		_isModified = true;

		_chkThumbPreview.setSelection(_prefStore.getDefaultBoolean(//
				ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_PREVIEW));

		_chkLoadingMessage.setSelection(_prefStore.getDefaultBoolean(//
				ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_LOADING_MESSAGE));

		_chkHQImage.setSelection(_prefStore.getDefaultBoolean(//
				ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_HQ_IMAGE));

		// set editor defaults
		super.performDefaults();

		enableControls();
	}

	@Override
	public boolean performOk() {

		saveState();

		// store editor fields
		final boolean isOK = super.performOk();

		if (isOK) {
			fireModifyEvent();
		}

		return isOK;
	}

	private void restoreState() {

		_chkThumbPreview.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_PREVIEW));

		_chkLoadingMessage.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_LOADING_MESSAGE));

		_chkHQImage.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_HQ_IMAGE));
	}

	private void saveState() {

		_prefStore.setValue(//
				ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_PREVIEW,
				_chkThumbPreview.getSelection());

		_prefStore.setValue(//
				ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_LOADING_MESSAGE,
				_chkLoadingMessage.getSelection());

		_prefStore.setValue(//
				ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_HQ_IMAGE,
				_chkHQImage.getSelection());
	}

}
