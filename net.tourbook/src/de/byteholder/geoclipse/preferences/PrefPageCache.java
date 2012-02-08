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
package de.byteholder.geoclipse.preferences;

import java.io.File;
import java.text.NumberFormat;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.util.UI;

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

import de.byteholder.geoclipse.map.TileImageCache;

/**
 * Cache for offline map images
 */
public class PrefPageCache extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String			SIZE_MBYTE			= Messages.prefPage_cache_MByte;

	private final static NumberFormat	_nf					= NumberFormat.getNumberInstance();

	private final String				_defaultCachePath	= Platform.getInstanceLocation().getURL().getPath();

	private int							_fileCounter;
	private long						_fileSize;
	private File						_tileCacheDir;

	private Job							_offlineInfoJob;
	private int							_lastFileCounterUIUpdate;
	private boolean						_isOfflineInfoJobCanceled;

	/*
	 * UI controls
	 */
	private Group						_groupOffline;
	private BooleanFieldEditor			_boolEditorUseOffLineCache;
	private BooleanFieldEditor			_boolEditorUseDefaultLocation;
	private Composite					_containerPath;

	private DirectoryFieldEditor		_dirEditorCachePath;

	private Label						_lblInfoPath;
	private Label						_lblInfoPathValue;
	private Label						_lblInfoFiles;
	private Label						_lblInfoFilesValue;
	private Label						_lblInfoSize;
	private Label						_lblInfoSizeValue;
	private Label						_lblInfoWaiting;
	private Label						_lblInfoWaitingValue;
	private Button						_btnDeleteOfflineCache;

	@Override
	protected Control createContents(final Composite parent) {

		final Control ui = createUI(parent);

		enableControls();

		getOfflineInfo();

		return ui;
	}

	private Control createUI(final Composite parent) {

		final IPreferenceStore prefStore = getPreferenceStore();

		final Composite uiContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(uiContainer);
		GridLayoutFactory.fillDefaults().applyTo(uiContainer);
//		fPrefContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			// checkbox: is offline enabled
			_boolEditorUseOffLineCache = new BooleanFieldEditor(
					IMappingPreferences.OFFLINE_CACHE_USE_OFFLINE,
					Messages.pref_cache_use_offline,
					uiContainer);
			_boolEditorUseOffLineCache.setPreferenceStore(prefStore);
			_boolEditorUseOffLineCache.setPage(this);
			_boolEditorUseOffLineCache.load();
			_boolEditorUseOffLineCache.setPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					enableControls();
				}
			});

			/*
			 * offline cache settings
			 */
			final Composite offlineContainer = new Composite(uiContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).indent(15, 5).applyTo(offlineContainer);
			GridLayoutFactory.fillDefaults().applyTo(offlineContainer);

			createUI_10_CacheSettings(offlineContainer);
			createUI_20_CacheInfo(offlineContainer);
		}

		/*
		 * hide error messages, this happend when the cache path is invalid but the offline cache is
		 * disabled
		 */
		if (_boolEditorUseOffLineCache.getBooleanValue() == false) {
			setErrorMessage(null);
		}

		return uiContainer;
	}

	private void createUI_10_CacheSettings(final Composite parent) {

		final IPreferenceStore prefStore = getPreferenceStore();

		_groupOffline = new Group(parent, SWT.NONE);
		_groupOffline.setText(Messages.prefPage_cache_group_offlineDirectory);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_groupOffline);
//		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
		{
			// field: use default location
			_boolEditorUseDefaultLocation = new BooleanFieldEditor(
					IMappingPreferences.OFFLINE_CACHE_USE_DEFAULT_LOCATION,
					Messages.pref_cache_use_default_location,
					_groupOffline);
			_boolEditorUseDefaultLocation.setPreferenceStore(prefStore);
			_boolEditorUseDefaultLocation.setPage(this);
			_boolEditorUseDefaultLocation.load();
			_boolEditorUseDefaultLocation.setPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					enableControls();
				}
			});
			new Label(_groupOffline, SWT.NONE);

			_containerPath = new Composite(_groupOffline, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(_containerPath);
			{
				// field: path for the tile cache
				_dirEditorCachePath = new DirectoryFieldEditor(
						IMappingPreferences.OFFLINE_CACHE_PATH,
						Messages.pref_cache_location,
						_containerPath);
				_dirEditorCachePath.setPreferenceStore(prefStore);
				_dirEditorCachePath.setPage(this);
				_dirEditorCachePath.setEmptyStringAllowed(false);
				_dirEditorCachePath.load();
				_dirEditorCachePath.setPropertyChangeListener(new IPropertyChangeListener() {
					public void propertyChange(final PropertyChangeEvent event) {
						getOfflineInfo();
					}
				});
			}
		}

		// !!! set layout after the editor was created because the editor sets the parents layout
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(_groupOffline);
	}

	private void createUI_20_CacheInfo(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.prefPage_cache_group_offlineInfo);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		{
			_lblInfoPath = new Label(group, SWT.NONE);
			_lblInfoPath.setText(Messages.prefPage_cache_label_path);
			_lblInfoPathValue = new Label(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblInfoPathValue);

			_lblInfoFiles = new Label(group, SWT.NONE);
			_lblInfoFiles.setText(Messages.prefPage_cache_label_files);
			_lblInfoFilesValue = new Label(group, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(_lblInfoFilesValue);

			_lblInfoSize = new Label(group, SWT.NONE);
			_lblInfoSize.setText(Messages.prefPage_cache_label_size);
			_lblInfoSizeValue = new Label(group, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(_lblInfoSizeValue);

			_lblInfoWaiting = new Label(group, SWT.NONE);
			_lblInfoWaiting.setText(Messages.prefPage_cache_label_status);

			_lblInfoWaitingValue = new Label(group, SWT.NONE);
			_lblInfoWaitingValue.setText(Messages.prefPage_cache_status_retrieving);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblInfoWaitingValue);

			// button: delete offline files
			_btnDeleteOfflineCache = new Button(group, SWT.PUSH);
			_btnDeleteOfflineCache.setText(Messages.pref_cache_clear_cache);
			GridDataFactory.swtDefaults().span(2, 1).applyTo(_btnDeleteOfflineCache);
			_btnDeleteOfflineCache.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					stopOfflineInfoJob();

					deleteOfflineFiles();
				}
			});
		}
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

			for (final String element : children) {
				final boolean success = deleteDir(new File(directory, element));
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

		if (MessageDialog.openConfirm(
				display.getActiveShell(),
				Messages.prefPage_cache_dlg_confirmDelete_title,
				NLS.bind(Messages.prefPage_cache_dlg_confirmDelete_message, _tileCacheDir.getAbsolutePath()))) {

			BusyIndicator.showWhile(display, new Runnable() {

				public void run() {

					_lblInfoWaitingValue.setText(Messages.prefPage_cache_status_deletingFiles);
					_lblInfoWaitingValue.pack(true);

					deleteDir(_tileCacheDir);
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
		return TourbookPlugin.getDefault().getPreferenceStore();
	}

	private void enableControls() {

		final boolean useOffLineCache = _boolEditorUseOffLineCache.getBooleanValue();
		final boolean useDefaultLocation = _boolEditorUseDefaultLocation.getBooleanValue();

		_boolEditorUseDefaultLocation.setEnabled(useOffLineCache, _groupOffline);

		// enable cache path editor, set default path
		if (useOffLineCache) {
			if (useDefaultLocation) {
				_dirEditorCachePath.setEnabled(false, _containerPath);
				_dirEditorCachePath.setStringValue(_defaultCachePath);
			} else {
				_dirEditorCachePath.setEnabled(true, _containerPath);
			}
		} else {
			_dirEditorCachePath.setEnabled(false, _containerPath);
		}
	}

	/**
	 * !!!!! Recursive funktion to count files/size !!!!!
	 * 
	 * @param listOfFiles
	 */
	private void getFilesInfo(final File[] listOfFiles) {

		if (_isOfflineInfoJobCanceled) {
			return;
		}

		if (_fileCounter > _lastFileCounterUIUpdate + 1000) {
			_lastFileCounterUIUpdate = _fileCounter;
			updateUIOfflineInfo(false);
		}

		for (final File file : listOfFiles) {
			if (file.isFile()) {

				// file

				_fileCounter++;
				_fileSize += file.length();

			} else if (file.isDirectory()) {

				// directory

				getFilesInfo(file.listFiles());
				if (_isOfflineInfoJobCanceled) {
					return;
				}
			}
		}
	}

	private void getOfflineInfo() {

		stopOfflineInfoJob();

		final String workingDirectory = _dirEditorCachePath.getStringValue();

		// check if working directory is available
		if (new File(workingDirectory).exists() == false) {

			_lblInfoPathValue.setText(workingDirectory);
			_lblInfoPathValue.pack(true);

			_lblInfoWaitingValue.setText(Messages.prefPage_cache_status_directoryIsNotAvailable);
			_lblInfoWaitingValue.pack(true);

			updateUIInvalidOfflineCache();
			return;
		}

		final IPath tileCachePath = new Path(workingDirectory).append(TileImageCache.TILE_OFFLINE_CACHE_OS_PATH);

		_lblInfoPathValue.setText(tileCachePath.toOSString());
		_lblInfoPathValue.pack(true);

		_tileCacheDir = tileCachePath.toFile();
		if (_tileCacheDir.exists() == false) {

			_lblInfoWaitingValue.setText(Messages.prefPage_cache_status_directoryIsNotAvailable);
			_lblInfoWaitingValue.pack(true);

			updateUIInvalidOfflineCache();
			return;
		}

		_offlineInfoJob = new Job(Messages.prefPage_cache_jobNameReadOfflineInfo) {

			@Override
			protected IStatus run(final IProgressMonitor monitor) {

				_fileCounter = 0;
				_fileSize = 0;
				_lastFileCounterUIUpdate = 0;
				_isOfflineInfoJobCanceled = false;

				getFilesInfo(_tileCacheDir.listFiles());

				updateUIOfflineInfo(true);

				return Status.OK_STATUS;
			}
		};

		_offlineInfoJob.schedule();
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

		_boolEditorUseOffLineCache.loadDefault();
		_boolEditorUseDefaultLocation.loadDefault();

		enableControls();

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		stopOfflineInfoJob();

		if (validateData() == false) {
			return false;
		}

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		boolean isModified = false;

		// check if the cache settings have changed
		if (prefStore.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_OFFLINE) != _boolEditorUseOffLineCache
				.getBooleanValue()) {
			isModified = true;
		}
		if (prefStore.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_DEFAULT_LOCATION) != _boolEditorUseDefaultLocation
				.getBooleanValue()) {
			isModified = true;
		}
		if (prefStore.getString(IMappingPreferences.OFFLINE_CACHE_PATH).equals(_dirEditorCachePath.getStringValue()) == false) {
			isModified = true;
		}

		_boolEditorUseOffLineCache.store();
		_boolEditorUseDefaultLocation.store();
		_dirEditorCachePath.store();

		if (isModified) {

			if (MessageDialog.openQuestion(
					Display.getDefault().getActiveShell(),
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

		if (_offlineInfoJob == null) {
			return;
		}

		_offlineInfoJob.cancel();
		_isOfflineInfoJobCanceled = true;

		try {
			_offlineInfoJob.join();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Update offline cache info when the cache is invalid
	 */
	private void updateUIInvalidOfflineCache() {

		_lblInfoFilesValue.setText(UI.EMPTY_STRING);
		_lblInfoFilesValue.pack(true);

		_lblInfoSizeValue.setText(UI.EMPTY_STRING);
		_lblInfoSizeValue.pack(true);

		_btnDeleteOfflineCache.setEnabled(false);
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
				if (_lblInfoFilesValue.isDisposed()) {
					return;
				}

				if (_isOfflineInfoJobCanceled) {

					_lblInfoFilesValue.setText(Messages.prefPage_cache_status_noValue);
					_lblInfoSizeValue.setText(Messages.prefPage_cache_status_noValue);
					_lblInfoWaitingValue.setText(Messages.prefPage_cache_status_infoWasCanceled);

				} else {

					_nf.setMinimumIntegerDigits(0);
					_nf.setMaximumFractionDigits(0);
					_lblInfoFilesValue.setText(_nf.format(_fileCounter));

					_nf.setMinimumIntegerDigits(2);
					_nf.setMaximumFractionDigits(2);
					_lblInfoSizeValue.setText(_nf.format((float) _fileSize / 1024 / 1024) + SIZE_MBYTE);

					if (isJobFinished) {
						_lblInfoWaitingValue.setText(UI.EMPTY_STRING);
					} else {
						_lblInfoWaitingValue.setText(Messages.prefPage_cache_status_retrieving);
					}
				}

				_lblInfoFilesValue.pack(true);
				_lblInfoSizeValue.pack(true);
				_lblInfoWaitingValue.pack(true);

				_btnDeleteOfflineCache.setEnabled(true);
			}
		});
	}

	private boolean validateData() {

		boolean isValid = true;
		final boolean useOffLineCache = _boolEditorUseOffLineCache.getBooleanValue();

		if (useOffLineCache
				&& _boolEditorUseDefaultLocation.getBooleanValue() == false
				&& (!_dirEditorCachePath.isValid() || _dirEditorCachePath.getStringValue().trim().length() == 0)) {

			isValid = false;
			setErrorMessage(Messages.pref_error_invalid_path);
			_dirEditorCachePath.setFocus();
		}

		if (isValid) {
			setErrorMessage(null);
		}

		return isValid;
	}

}
