/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

package net.tourbook.ext.srtm;

import java.text.NumberFormat;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class PrefPageSRTM extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String		PREF_PAGE_SRTM_COLORS	= "net.tourbook.ext.srtm.PrefPageSRTMColors";			//$NON-NLS-1$

	final static NumberFormat		nf						= NumberFormat.getNumberInstance();

	final String					fDefaultSRTMFilePath	= Platform.getInstanceLocation().getURL().getPath();

	private Composite				fPrefContainer;
	private Group					fLocationContainer;
	private Composite				fPathContainer;

	private BooleanFieldEditor		fUseDefaultLocation;
	private DirectoryFieldEditor	fDataPathEditor;

	@Override
	protected Control createContents(final Composite parent) {

		createUI(parent);

		enableControls();

		/*
		 * hide error messages, it appears when the srtm data path is invalid
		 */
		if (fUseDefaultLocation.getBooleanValue() == false) {
			setErrorMessage(null);
		}
		return fPrefContainer;
	}

	private void createUI(final Composite parent) {

		fPrefContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(fPrefContainer);
		GridLayoutFactory.fillDefaults().applyTo(fPrefContainer);
		GridDataFactory.swtDefaults().applyTo(fPrefContainer);
//		fPrefContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		createUICacheSettings(fPrefContainer);
		createUISRTMPageLink(fPrefContainer);
	}

	private void createUICacheSettings(final Composite parent) {

		final IPreferenceStore prefStore = getPreferenceStore();

		fLocationContainer = new Group(parent, SWT.NONE);
		fLocationContainer.setText(Messages.prefPage_srtm_group_label_data_location);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fLocationContainer);
//		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);

		// field: use default location
		fUseDefaultLocation = new BooleanFieldEditor(IPreferences.SRTM_USE_DEFAULT_DATA_FILEPATH,
				Messages.prefPage_srtm_chk_use_default_location,
				fLocationContainer);
		fUseDefaultLocation.setPage(this);
		fUseDefaultLocation.setPreferenceStore(prefStore);
		fUseDefaultLocation.load();
		fUseDefaultLocation.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				enableControls();
			}
		});
		new Label(fLocationContainer, SWT.NONE);

		fPathContainer = new Composite(fLocationContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(fPathContainer);
		{
			// field: path for the srtm data
			fDataPathEditor = new DirectoryFieldEditor(IPreferences.SRTM_DATA_FILEPATH,
					Messages.prefPage_srtm_editor_data_filepath,
					fPathContainer);
			fDataPathEditor.setPage(this);
			fDataPathEditor.setPreferenceStore(prefStore);
			fDataPathEditor.setEmptyStringAllowed(false);
			fDataPathEditor.load();
//			fDataPathEditor.setPropertyChangeListener(new IPropertyChangeListener() {
//				public void propertyChange(final PropertyChangeEvent event) {
//					updateDataInfo();
//				}
//			});
		}

		// !!! set layout after the editor was created because the editor sets the parents layout
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(fLocationContainer);
	}

	private void createUISRTMPageLink(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().indent(0, 20).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{

			final Link link = new Link(container, SWT.NONE);
			link.setText(Messages.prefPage_srtm_link_srtmProfiles);
			link.setEnabled(true);
			link.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					PreferencesUtil.createPreferenceDialogOn(getShell(), PREF_PAGE_SRTM_COLORS, null, null);
				}
			});
		}
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	private void enableControls() {

		final boolean useDefaultLocation = fUseDefaultLocation.getBooleanValue();

		if (useDefaultLocation) {
			fDataPathEditor.setEnabled(false, fPathContainer);
			fDataPathEditor.setStringValue(fDefaultSRTMFilePath);
		} else {
			fDataPathEditor.setEnabled(true, fPathContainer);
		}
	}

	public void init(final IWorkbench workbench) {}

	@Override
	public boolean okToLeave() {

		if (validateData() == false) {
			return false;
		}

		return super.okToLeave();
	}

	@Override
	protected void performDefaults() {

		fUseDefaultLocation.loadDefault();

		enableControls();

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		if (validateData() == false) {
			return false;
		}

		fUseDefaultLocation.store();
		fDataPathEditor.store();

		return super.performOk();
	}


	private boolean validateData() {

		boolean isValid = true;

		if (fUseDefaultLocation.getBooleanValue() == false
				&& (!fDataPathEditor.isValid() || fDataPathEditor.getStringValue().trim().length() == 0)) {

			isValid = false;
			setErrorMessage(net.tourbook.ext.srtm.Messages.prefPage_srtm_msg_invalid_data_path);
			fDataPathEditor.setFocus();
		}

		if (isValid) {
			setErrorMessage(null);
		}

		return isValid;
	}

}
