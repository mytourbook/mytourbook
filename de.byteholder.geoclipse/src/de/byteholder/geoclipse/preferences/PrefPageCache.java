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

package de.byteholder.geoclipse.preferences;
 
import java.io.File;
import java.text.NumberFormat;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import de.byteholder.geoclipse.Activator;
import de.byteholder.geoclipse.map.TileImageCache;

public class PrefPageCache extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String		EMPTY_STRING		= "";													//$NON-NLS-1$
	private static final String		SIZE_MBYTE			= Messages.prefPage_cache_MByte;

	final static NumberFormat		nf					= NumberFormat.getNumberInstance();

	final String					fDefaultCachePath	= Platform.getInstanceLocation().getURL().getPath();

	private Composite				fPrefContainer;
	private Group					fOfflineContainer;
	private BooleanFieldEditor		fUseOffLineCache;
	private BooleanFieldEditor		fUseDefaultLocation;
	private Composite				fPathContainer;

	private DirectoryFieldEditor	fCachePathEditor;

	private Label					fLblInfoPath;
	private Label					fLblInfoPathValue;
	private Label					fLblInfoFiles;
	private Label					fLblInfoFilesValue;
	private Label					fLblInfoSize;
	private Label					fLblInfoSizeValue;
	private Label					fLblInfoWaiting;
	private Label					fLblInfoWaitingValue;
	private Button					fBtnDeleteOfflineCache;

	private int						fFileCounter;
	private long					fFileSize;
	private File					fTileCacheDir;

	private Job						fOfflineInfoJob;
	private int						fLastFileCounterUIUpdate;
	private boolean					fIsOfflineInfoJobCanceled;

	@Override
	protected Control createContents(final Composite parent) {

		createUI(parent);

		enableControls();

		getOfflineInfo();

		return fPrefContainer;
	}

	private void createUI(final Composite parent) {

		final IPreferenceStore prefStore = getPreferenceStore();

		fPrefContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(fPrefContainer);
		GridLayoutFactory.fillDefaults().applyTo(fPrefContainer);
//		fPrefContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		// checkbox: is offline enabled
		fUseOffLineCache = new BooleanFieldEditor(IMappingPreferences.OFFLINE_CACHE_USE_OFFLINE,
				Messages.pref_cache_use_offline,
				fPrefContainer);
		fUseOffLineCache.setPreferenceStore(prefStore);
		fUseOffLineCache.setPage(this);
		fUseOffLineCache.load();
		fUseOffLineCache.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				enableControls();
			}
		});

		/*
		 * offline cache settings
		 */
		final Composite offlineContainer = new Composite(fPrefContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(15, 5).applyTo(offlineContainer);
		GridLayoutFactory.fillDefaults().applyTo(offlineContainer);

		createUICacheSettings(offlineContainer);
		createUICacheInfo(offlineContainer);

		/*
		 * hide error messages, this happend when the cache path is invalid but the offline cache is
		 * disabled
		 */
		if (fUseOffLineCache.getBooleanValue() == false) {
			setErrorMessage(null);
		}
	}

	private void createUICacheInfo(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.prefPage_cache_group_offlineInfo);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);

		fLblInfoPath = new Label(group, SWT.NONE);
		fLblInfoPath.setText(Messages.prefPage_cache_label_path);
		fLblInfoPathValue = new Label(group, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fLblInfoPathValue);

		fLblInfoFiles = new Label(group, SWT.NONE);
		fLblInfoFiles.setText(Messages.prefPage_cache_label_files);
		fLblInfoFilesValue = new Label(group, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(fLblInfoFilesValue);

		fLblInfoSize = new Label(group, SWT.NONE);
		fLblInfoSize.setText(Messages.prefPage_cache_label_size);
		fLblInfoSizeValue = new Label(group, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(fLblInfoSizeValue);

		fLblInfoWaiting = new Label(group, SWT.NONE);
		fLblInfoWaiting.setText(Messages.prefPage_cache_label_status);

		fLblInfoWaitingValue = new Label(group, SWT.NONE);
		fLblInfoWaitingValue.setText(Messages.prefPage_cache_status_retrieving);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fLblInfoWaitingValue);

		// button: delete offline files
		fBtnDeleteOfflineCache = new Button(group, SWT.PUSH);
		fBtnDeleteOfflineCache.setText(Messages.pref_cache_clear_cache);
		GridDataFactory.swtDefaults().span(2, 1).applyTo(fBtnDeleteOfflineCache);
		fBtnDeleteOfflineCache.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				stopOfflineInfoJob();

				deleteOfflineFiles();
			}
		});
	}

	private void createUICacheSettings(final Composite parent) {

		final IPreferenceStore prefStore = getPreferenceStore();

		fOfflineContainer = new Group(parent, SWT.NONE);
		fOfflineContainer.setText(Messages.prefPage_cache_group_offlineDirectory);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fOfflineContainer);
//		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);

		// field: use default location
		fUseDefaultLocation = new BooleanFieldEditor(IMappingPreferences.OFFLINE_CACHE_USE_DEFAULT_LOCATION,
				Messages.pref_cache_use_default_location,
				fOfflineContainer);
		fUseDefaultLocation.setPreferenceStore(prefStore);
		fUseDefaultLocation.setPage(this);
		fUseDefaultLocation.load();
		fUseDefaultLocation.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				enableControls();
			}
		});
		new Label(fOfflineContainer, SWT.NONE);

		fPathContainer = new Composite(fOfflineContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(fPathContainer);
		{
			// field: path for the tile cache
			fCachePathEditor = new DirectoryFieldEditor(IMappingPreferences.OFFLINE_CACHE_PATH,
					Messages.pref_cache_location,
					fPathContainer);
			fCachePathEditor.setPreferenceStore(prefStore);
			fCachePathEditor.setPage(this);
			fCachePathEditor.setEmptyStringAllowed(false);
			fCachePathEditor.load();
			fCachePathEditor.setPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					getOfflineInfo();
				}
			});
		}

		// !!! set layout after the editor was created because the editor sets the parents layout
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(fOfflineContainer);
	}

	/**
	 * Deletes all files and subdirectories. If a deletion fails, the method stops attempting to
	 * delete and returns false.
	 * 
	 * @param directory
	 * @return Returns <code>true</code> if all deletions were successful
	 */
	private boolean deleteDir(final File directory) {

		if (directory.isDirectory()) {

			final String[] children = directory.list();

			for (int i = 0; i < children.length; i++) {
				final boolean success = deleteDir(new File(directory, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		final boolean isDeleted = directory.delete();

		return isDeleted;
	}

	private void deleteOfflineFiles() {

		final Display display = Display.getCurrent();

		if (MessageDialog.openConfirm(display.getActiveShell(),
				Messages.prefPage_cache_dlg_confirmDelete_title,
				NLS.bind(Messages.prefPage_cache_dlg_confirmDelete_message, fTileCacheDir.getAbsolutePath()))) {

			BusyIndicator.showWhile(display, new Runnable() {

				public void run() {

					fLblInfoWaitingValue.setText(Messages.prefPage_cache_status_deletingFiles);
					fLblInfoWaitingValue.pack(true);

					deleteDir(fTileCacheDir);
					getOfflineInfo();
				}
			});
		}
	}

	/**
	 * Returns preference store that belongs to this plugin.
	 * 
	 * @return IPreferenceStore the preference store for this plugin
	 */
	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	private void enableControls() {

		final boolean useOffLineCache = fUseOffLineCache.getBooleanValue();
		final boolean useDefaultLocation = fUseDefaultLocation.getBooleanValue();

		fUseDefaultLocation.setEnabled(useOffLineCache, fOfflineContainer);

		// enable cache path editor, set default path
		if (useOffLineCache) {
			if (useDefaultLocation) {
				fCachePathEditor.setEnabled(false, fPathContainer);
				fCachePathEditor.setStringValue(fDefaultCachePath);
			} else {
				fCachePathEditor.setEnabled(true, fPathContainer);
			}
		} else {
			fCachePathEditor.setEnabled(false, fPathContainer);
		}
	}

	/**
	 * !!!!! Recursive funktion to count files/size !!!!!
	 * 
	 * @param listOfFiles
	 */
	private void getFilesInfo(final File[] listOfFiles) {

		if (fIsOfflineInfoJobCanceled) {
			return;
		}

		if (fFileCounter > fLastFileCounterUIUpdate + 1000) {
			fLastFileCounterUIUpdate = fFileCounter;
			updateUIOfflineInfo(false);
		}

		for (int fileIndex = 0; fileIndex < listOfFiles.length; fileIndex++) {
			final File file = listOfFiles[fileIndex];
			if (file.isFile()) {

				// file

				fFileCounter++;
				fFileSize += file.length();

			} else if (file.isDirectory()) {

				// directory

				getFilesInfo(file.listFiles());
				if (fIsOfflineInfoJobCanceled) {
					return;
				}
			}
		}
	}

	private void getOfflineInfo() {

		stopOfflineInfoJob();

		final String workingDirectory = fCachePathEditor.getStringValue();

		// check if working directory is available
		if (new File(workingDirectory).exists() == false) {

			fLblInfoPathValue.setText(workingDirectory);
			fLblInfoPathValue.pack(true);

			fLblInfoWaitingValue.setText(Messages.prefPage_cache_status_directoryIsNotAvailable);
			fLblInfoWaitingValue.pack(true);

			updateUIInvalidOfflineCache();
			return;
		}

		final IPath tileCachePath = new Path(workingDirectory).append(TileImageCache.TILE_OFFLINE_CACHE_OS_PATH);

		fLblInfoPathValue.setText(tileCachePath.toOSString());
		fLblInfoPathValue.pack(true);

		fTileCacheDir = tileCachePath.toFile();
		if (fTileCacheDir.exists() == false) {

			fLblInfoWaitingValue.setText(Messages.prefPage_cache_status_directoryIsNotAvailable);
			fLblInfoWaitingValue.pack(true);

			updateUIInvalidOfflineCache();
			return;
		}

		fOfflineInfoJob = new Job(Messages.prefPage_cache_jobNameReadOfflineInfo) {

			@Override
			protected IStatus run(final IProgressMonitor monitor) {

				fFileCounter = 0;
				fFileSize = 0;
				fLastFileCounterUIUpdate = 0;
				fIsOfflineInfoJobCanceled = false;

				getFilesInfo(fTileCacheDir.listFiles());

				updateUIOfflineInfo(true);

				return Status.OK_STATUS;
			}
		};

		fOfflineInfoJob.schedule();
	}

	public void init(final IWorkbench workbench) {}

	@Override
	public boolean okToLeave() {

		stopOfflineInfoJob();

		if (validateData() == false) {
			return false;
		}

		return super.okToLeave();
	}

	@Override
	public boolean performCancel() {

		stopOfflineInfoJob();

		return super.performCancel();
	}

	@Override
	protected void performDefaults() {

		stopOfflineInfoJob();

		fUseOffLineCache.loadDefault();
		fUseDefaultLocation.loadDefault();

		enableControls();

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		stopOfflineInfoJob();

		if (validateData() == false) {
			return false;
		}

		final IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
		boolean isModified = false;

		// check if the cache settings have changed
		if (prefStore.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_OFFLINE) != fUseOffLineCache.getBooleanValue()) {
			isModified = true;
		}
		if (prefStore.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_DEFAULT_LOCATION) != fUseDefaultLocation.getBooleanValue()) {
			isModified = true;
		}
		if (prefStore.getString(IMappingPreferences.OFFLINE_CACHE_PATH).equals(fCachePathEditor.getStringValue()) == false) {
			isModified = true;
		}

		fUseOffLineCache.store();
		fUseDefaultLocation.store();
		fCachePathEditor.store();

		if (isModified) {

			if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
					Messages.pref_cache_message_box_title,
					Messages.pref_cache_message_box_text)) {

				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {
						PlatformUI.getWorkbench().restart();
					}
				});
			}
		}

		return super.performOk();
	}

	private void stopOfflineInfoJob() {

		if (fOfflineInfoJob == null) {
			return;
		}

		fOfflineInfoJob.cancel();
		fIsOfflineInfoJobCanceled = true;

		try {
			fOfflineInfoJob.join();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Update offline cache info when the cache is invalid
	 */
	private void updateUIInvalidOfflineCache() {

		fLblInfoFilesValue.setText(EMPTY_STRING);
		fLblInfoFilesValue.pack(true);

		fLblInfoSizeValue.setText(EMPTY_STRING);
		fLblInfoSizeValue.pack(true);

		fBtnDeleteOfflineCache.setEnabled(false);
	}

	/**
	 * Update offline info in a UI thread
	 * 
	 * @param isJobFinished
	 */
	private void updateUIOfflineInfo(final boolean isJobFinished) {

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {

				// check if the controls are available
				if (fLblInfoFilesValue.isDisposed()) {
					return;
				}
				
				if (fIsOfflineInfoJobCanceled) {

					fLblInfoFilesValue.setText(Messages.prefPage_cache_status_noValue);
					fLblInfoSizeValue.setText(Messages.prefPage_cache_status_noValue);
					fLblInfoWaitingValue.setText(Messages.prefPage_cache_status_infoWasCanceled);

				} else {

					nf.setMinimumIntegerDigits(0);
					nf.setMaximumFractionDigits(0);
					fLblInfoFilesValue.setText(nf.format(fFileCounter));

					nf.setMinimumIntegerDigits(2);
					nf.setMaximumFractionDigits(2);
					fLblInfoSizeValue.setText(nf.format((float) fFileSize / 1024 / 1024) + SIZE_MBYTE);

					if (isJobFinished) {
						fLblInfoWaitingValue.setText(EMPTY_STRING);
					} else {
						fLblInfoWaitingValue.setText(Messages.prefPage_cache_status_retrieving);
					}
				}

				fLblInfoFilesValue.pack(true);
				fLblInfoSizeValue.pack(true);
				fLblInfoWaitingValue.pack(true);

				fBtnDeleteOfflineCache.setEnabled(true);
			}
		});
	}

	private boolean validateData() {

		boolean isValid = true;
		final boolean useOffLineCache = fUseOffLineCache.getBooleanValue();

		if (useOffLineCache
				&& fUseDefaultLocation.getBooleanValue() == false
				&& (!fCachePathEditor.isValid() || fCachePathEditor.getStringValue().trim().length() == 0)) {

			isValid = false;
			setErrorMessage(Messages.pref_error_invalid_path);
			fCachePathEditor.setFocus();
		}

		if (isValid) {
			setErrorMessage(null);
		}

		return isValid;
	}

}
