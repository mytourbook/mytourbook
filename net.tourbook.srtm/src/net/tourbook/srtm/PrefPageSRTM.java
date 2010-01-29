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
package net.tourbook.srtm;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import net.tourbook.util.IExternalTourEvents;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;

import de.byteholder.geoclipse.map.UI;

public class PrefPageSRTM extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String		PROTOCOL_HTTP			= "http://";											//$NON-NLS-1$
	public static final String		PROTOCOL_FTP			= "ftp://";											//$NON-NLS-1$

	private static final String		PREF_PAGE_SRTM_COLORS	= "net.tourbook.ext.srtm.PrefPageSRTMColors";			//$NON-NLS-1$

//	http://dds.cr.usgs.gov/srtm/version2_1/SRTM3/Eurasia/N47E008.hgt.zip

	private IPreferenceStore		fPrefStore;

	private String					fDefaultSRTMFilePath	= Platform.getInstanceLocation().getURL().getPath();

	private Composite				fPrefContainer;
	private Composite				fPathContainer;

	private BooleanFieldEditor		fUseDefaultLocation;
	private DirectoryFieldEditor	fDataPathEditor;

	private Button					fRdoSRTM3FtpUrl;
	private Text					fTxtSRTM3FtpUrl;
	private Button					fRdoSRTM3HttpUrl;
	private Text					fTxtSRTM3HttpUrl;

	// original values when page is opened
	private boolean					fBackupIsFtp;
	private String					fBackupFtpUrl;
	private String					fBackupHttpUrl;

	@Override
	protected Control createContents(final Composite parent) {

		fPrefStore = Activator.getDefault().getPreferenceStore();

		createUI(parent);

		restoreState();

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

		createUICacheSettings(fPrefContainer);
		createUISrtm3Url(fPrefContainer);
		createUISrtmPageLink(fPrefContainer);
	}

	private void createUICacheSettings(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.prefPage_srtm_group_label_data_location);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		{
			// field: use default location
			fUseDefaultLocation = new BooleanFieldEditor(IPreferences.SRTM_USE_DEFAULT_DATA_FILEPATH,
					Messages.prefPage_srtm_chk_use_default_location,
					group);
			fUseDefaultLocation.setPage(this);
			fUseDefaultLocation.setPreferenceStore(fPrefStore);
			fUseDefaultLocation.setPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					enableControls();
				}
			});
			new Label(group, SWT.NONE);

			fPathContainer = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(fPathContainer);
			{
				// field: path for the srtm data
				fDataPathEditor = new DirectoryFieldEditor(IPreferences.SRTM_DATA_FILEPATH,
						Messages.prefPage_srtm_editor_data_filepath,
						fPathContainer);
				fDataPathEditor.setPage(this);
				fDataPathEditor.setPreferenceStore(fPrefStore);
				fDataPathEditor.setEmptyStringAllowed(false);
				fDataPathEditor.setPropertyChangeListener(new IPropertyChangeListener() {
					public void propertyChange(final PropertyChangeEvent event) {
						validateData();
					}
				});
			}
		}

		// !!! set layout after the editor was created because the editor sets the parents layout
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
	}

	private void createUISrtm3Url(final Composite parent) {

		final SelectionAdapter selectListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableControls();
				validateData();
			}
		};

		final ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				validateData();
			}
		};

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.prefPage_srtm_group_label_srtm3);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		{
			// radio: http url
			fRdoSRTM3HttpUrl = new Button(group, SWT.RADIO);
			fRdoSRTM3HttpUrl.setText(Messages.prefPage_srtm_radio_srtm3HttpUrl);
			fRdoSRTM3HttpUrl.addSelectionListener(selectListener);

			// text: http url
			fTxtSRTM3HttpUrl = new Text(group, SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fTxtSRTM3HttpUrl);
			fTxtSRTM3HttpUrl.addModifyListener(modifyListener);

			// radio: ftp url
			fRdoSRTM3FtpUrl = new Button(group, SWT.RADIO);
			fRdoSRTM3FtpUrl.setText(Messages.prefPage_srtm_radio_srtm3FtpUrl);
			fRdoSRTM3FtpUrl.addSelectionListener(selectListener);

			/*
			 * is disabled becuase the server is currently not available 2009-08-18 and
			 * the connection test feature cannot be tested
			 */
			fRdoSRTM3FtpUrl.setEnabled(false);

			// text: ftp url
			fTxtSRTM3FtpUrl = new Text(group, SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fTxtSRTM3FtpUrl);
			fTxtSRTM3FtpUrl.addModifyListener(modifyListener);

			// button: test connection
			final Button btnTestConnection = new Button(group, SWT.NONE);
			GridDataFactory.swtDefaults().indent(0, 10).span(2, 1).applyTo(btnTestConnection);
			btnTestConnection.setText(Messages.prefPage_srtm_button_testConnection);
			btnTestConnection.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onCheckConnection();
				}
			});
		}
	}

	private void createUISrtmPageLink(final Composite parent) {

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
		return fPrefStore;
	}

	private void enableControls() {

		final boolean useDefaultLocation = fUseDefaultLocation.getBooleanValue();

		if (useDefaultLocation) {
			fDataPathEditor.setEnabled(false, fPathContainer);
			fDataPathEditor.setStringValue(fDefaultSRTMFilePath);
		} else {
			fDataPathEditor.setEnabled(true, fPathContainer);
		}

		// SRTM3 server
		final boolean isFTP = fRdoSRTM3FtpUrl.getSelection();
		fTxtSRTM3FtpUrl.setEnabled(isFTP);
		fTxtSRTM3HttpUrl.setEnabled(!isFTP);
	}

	public void init(final IWorkbench workbench) {}

	@Override
	public boolean okToLeave() {

		if (validateData() == false) {
			return false;
		}

		return super.okToLeave();
	}

	private void onCheckConnection() {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				String baseUrl;
				if (fRdoSRTM3FtpUrl.getSelection()) {

					// check ftp connection

//					baseUrl = fTxtSRTM3FtpUrl.getText().trim();
//
//					final FTPClient ftp = new FTPClient();

				} else {

					// check http connection

					baseUrl = fTxtSRTM3HttpUrl.getText().trim();

					try {
						final URL url = new URL(baseUrl);
						final HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
						urlConn.connect();

						final int response = urlConn.getResponseCode();
						final String responseMessage = urlConn.getResponseMessage();

						final String message = response == HttpURLConnection.HTTP_OK//
								? NLS.bind(Messages.prefPage_srtm_checkHTTPConnectionOK_message, baseUrl)
								: NLS.bind(Messages.prefPage_srtm_checkHTTPConnectionFAILED_message,//
										new Object[] {
												baseUrl,
												Integer.toString(response),
												responseMessage == null ? UI.EMPTY_STRING : responseMessage });

						MessageDialog.openInformation(
								Display.getCurrent().getActiveShell(),
								Messages.prefPage_srtm_checkHTTPConnection_title,
								message);

					} catch (final IOException e) {

						MessageDialog.openInformation(
								Display.getCurrent().getActiveShell(),
								Messages.prefPage_srtm_checkHTTPConnection_title,
								NLS.bind(Messages.prefPage_srtm_checkHTTPConnection_message, baseUrl));

						e.printStackTrace();
					}
				}
			}
		});
	}

	@Override
	protected void performDefaults() {

		fUseDefaultLocation.loadDefault();

		fPrefStore.setToDefault(IPreferences.STATE_IS_SRTM3_FTP);
		fPrefStore.setToDefault(IPreferences.STATE_SRTM3_HTTP_URL);
		fPrefStore.setToDefault(IPreferences.STATE_SRTM3_FTP_URL);

		// update controls
		final boolean isFtp = fPrefStore.getDefaultBoolean(IPreferences.STATE_IS_SRTM3_FTP);
		fRdoSRTM3FtpUrl.setSelection(isFtp);
		fRdoSRTM3HttpUrl.setSelection(!isFtp);

		fTxtSRTM3FtpUrl.setText(fPrefStore.getDefaultString(IPreferences.STATE_SRTM3_FTP_URL));
		fTxtSRTM3HttpUrl.setText(fPrefStore.getDefaultString(IPreferences.STATE_SRTM3_HTTP_URL));

		enableControls();

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		if (validateData() == false) {
			return false;
		}

		saveState();

		/*
		 * when the srtm3 server has been modified, clear the file cache to reload the files from
		 * the new location
		 */
		if (fBackupIsFtp != fPrefStore.getBoolean(IPreferences.STATE_IS_SRTM3_FTP)
				|| fBackupFtpUrl.equalsIgnoreCase(fPrefStore.getString(IPreferences.STATE_SRTM3_FTP_URL)) == false
				|| fBackupHttpUrl.equalsIgnoreCase(fPrefStore.getString(IPreferences.STATE_SRTM3_HTTP_URL)) == false) {

			ElevationSRTM3.clearElevationFileCache();

			// fire event to clear the tour data cache which remove existing srtm data
			net.tourbook.util.Activator.getDefault().getPreferenceStore().setValue(
					IExternalTourEvents.CLEAR_TOURDATA_CACHE,
					Math.random());
		}

		return super.performOk();
	}

	private void restoreState() {

		fUseDefaultLocation.load();
		fDataPathEditor.load();

		fBackupIsFtp = fPrefStore.getBoolean(IPreferences.STATE_IS_SRTM3_FTP);
		fRdoSRTM3FtpUrl.setSelection(fBackupIsFtp);
		fRdoSRTM3HttpUrl.setSelection(!fBackupIsFtp);

		fBackupFtpUrl = fPrefStore.getString(IPreferences.STATE_SRTM3_FTP_URL);
		fBackupHttpUrl = fPrefStore.getString(IPreferences.STATE_SRTM3_HTTP_URL);
		fTxtSRTM3FtpUrl.setText(fBackupFtpUrl);
		fTxtSRTM3HttpUrl.setText(fBackupHttpUrl);
	}

	private void saveState() {

		fUseDefaultLocation.store();
		fDataPathEditor.store();

		fPrefStore.setValue(IPreferences.STATE_IS_SRTM3_FTP, fRdoSRTM3FtpUrl.getSelection());
		fPrefStore.setValue(IPreferences.STATE_SRTM3_HTTP_URL, fTxtSRTM3HttpUrl.getText().trim());
		fPrefStore.setValue(IPreferences.STATE_SRTM3_FTP_URL, fTxtSRTM3FtpUrl.getText().trim());
	}

	private boolean validateData() {

		boolean isValid = true;

		if (fUseDefaultLocation.getBooleanValue() == false
				&& (!fDataPathEditor.isValid() || fDataPathEditor.getStringValue().trim().length() == 0)) {

			isValid = false;

			setErrorMessage(Messages.prefPage_srtm_msg_invalid_data_path);
			fDataPathEditor.setFocus();

		} else if (fRdoSRTM3FtpUrl.getSelection()) {

			// check ftp url

			if (fTxtSRTM3FtpUrl.getText().trim().toLowerCase().startsWith(PROTOCOL_FTP) == false) {

				isValid = false;

				setErrorMessage(Messages.prefPage_srtm_msg_invalidSrtm3FtpUrl);
				fTxtSRTM3FtpUrl.setFocus();
			}

		} else {

			// check http url

			if (fTxtSRTM3HttpUrl.getText().trim().toLowerCase().startsWith(PROTOCOL_HTTP) == false) {

				isValid = false;

				setErrorMessage(Messages.prefPage_srtm_msg_invalidSrtm3HttpUrl);
				fTxtSRTM3HttpUrl.setFocus();
			}
		}

		if (isValid) {
			setErrorMessage(null);
		}

		setValid(isValid);

		return isValid;
	}

}
