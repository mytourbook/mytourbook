/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.importdata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.PerspectiveFactoryRawData;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.rawData.RawDataView;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;
import net.tourbook.util.StatusUtil;
import net.tourbook.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

public class RawDataManager {

	private static final String				RAW_DATA_LAST_SELECTED_PATH			= "raw-data-view.last-selected-import-path";	//$NON-NLS-1$
	private static final String				TEMP_IMPORTED_FILE					= "received-device-data.txt";					//$NON-NLS-1$

	private static RawDataManager			_instance							= null;

	/**
	 * contains the device data imported from the device/file
	 */
	private final DeviceData				_deviceData							= new DeviceData();

	/**
	 * contains tours which were imported or received
	 */
	private final HashMap<Long, TourData>	_importedTourData					= new HashMap<Long, TourData>();

	/**
	 * contains the filenames for all imported files
	 */
	private final HashSet<String>			_importedFileNames					= new HashSet<String>();

	/**
	 * Contains filenames which are not directly imported but is imported from other imported files
	 */
	private final HashSet<String>			_importedFileNamesChildren			= new HashSet<String>();

	private boolean							_isImported;
	private boolean							_isImportCanceled;

	private int								_importSettingsImportYear			= -1;
	private boolean							_importSettingsIsMergeTracks;
	private boolean							_importSettingsIsChecksumValidation	= true;
	private boolean							_importSettingsCreateTourIdWithTime	= false;

	private String							_lastImportedFile;

	private List<TourbookDevice>			_devicesBySortPriority;
	private HashMap<String, TourbookDevice>	_devicesByExtension;

	private RawDataManager() {}

	public static RawDataManager getInstance() {

		if (_instance == null) {
			_instance = new RawDataManager();
		}

		return _instance;
	}

	/**
	 * @return temp directory where received data are stored temporarily
	 */
	public static String getTempDir() {
		return TourbookPlugin.getDefault().getStateLocation().toFile().getAbsolutePath();
	}

	public static void showMsgBoxInvalidFormat(final ArrayList<String> notImportedFiles) {

		Display.getDefault().syncExec(new Runnable() {
			public void run() {

				final StringBuilder fileText = new StringBuilder();
				for (final String fileName : notImportedFiles) {
					fileText.append(UI.NEW_LINE);
					fileText.append(fileName);
				}

				final String errorMessage = NLS.bind(Messages.DataImport_Error_invalid_data_format, fileText.toString());
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.app_error_title, errorMessage);

				System.out.println(errorMessage);
			}
		});
	}

	void actionImportFromDevice() {

		final WizardImportDialog dialog = new WizardImportDialog(PlatformUI
				.getWorkbench()
				.getActiveWorkbenchWindow()
				.getShell(), new WizardImportData(), Messages.Import_Wizard_Dlg_title);

		if (dialog.open() == Window.OK) {
			showRawDataView();
		}
	}

	void actionImportFromDeviceDirect() {

		final WizardImportData importWizard = new WizardImportData();

		final WizardDialog dialog = new WizardImportDialog(PlatformUI
				.getWorkbench()
				.getActiveWorkbenchWindow()
				.getShell(), importWizard, Messages.Import_Wizard_Dlg_title);

		// create the dialog and shell which is required in setAutoDownload()
		dialog.create();

		importWizard.setAutoDownload();

		if (dialog.open() == Window.OK) {
			showRawDataView();
		}
	}

	/**
	 * Import tours from files which are selected in a file selection dialog.
	 */
	void actionImportFromFile() {

		final List<TourbookDevice> deviceList = DeviceManager.getDeviceList();

		// create file filter list
		final int deviceLength = deviceList.size() + 1;
		final String[] filterExtensions = new String[deviceLength];
		final String[] filterNames = new String[deviceLength];

		int deviceIndex = 0;

		// add option to show all files
		filterExtensions[deviceIndex] = "*.*"; //$NON-NLS-1$
		filterNames[deviceIndex] = "*.*"; //$NON-NLS-1$

		deviceIndex++;

		// add option for every file extension
		for (final TourbookDevice device : deviceList) {
			filterExtensions[deviceIndex] = "*." + device.fileExtension; //$NON-NLS-1$
			filterNames[deviceIndex] = device.visibleName + (" (*." + device.fileExtension + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			deviceIndex++;
		}

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		final String lastSelectedPath = prefStore.getString(RAW_DATA_LAST_SELECTED_PATH);

		// setup open dialog
		final FileDialog fileDialog = new FileDialog(Display.getDefault().getActiveShell(), (SWT.OPEN | SWT.MULTI));
		fileDialog.setFilterExtensions(filterExtensions);
		fileDialog.setFilterNames(filterNames);
		fileDialog.setFilterPath(lastSelectedPath);

		// open file dialog
		final String firstFilePathName = fileDialog.open();

		// check if user canceled the dialog
		if (firstFilePathName == null) {
			return;
		}

		final IPath filePath = new Path(firstFilePathName).removeLastSegments(1);
		final String[] selectedFileNames = fileDialog.getFileNames();

		// keep last selected path
		final String selectedPath = filePath.makeAbsolute().toString();
		prefStore.putValue(RAW_DATA_LAST_SELECTED_PATH, selectedPath);

		// create path for each file
		final ArrayList<IPath> selectedFilePaths = new ArrayList<IPath>();
		for (final String fileName : selectedFileNames) {

			// replace filename, keep the directory path
			final IPath filePathWithName = filePath.append(fileName);
			final IPath absolutePath = filePathWithName.makeAbsolute();
			final String filePathName = absolutePath.toString();

			selectedFilePaths.add(new Path(filePathName));
		}

		// check if devices are loaded
		if (_devicesByExtension == null) {
			getDeviceListSortedByPriority();
		}

		// resort files by extension priority
		Collections.sort(selectedFilePaths, new Comparator<IPath>() {

			@Override
			public int compare(final IPath path1, final IPath path2) {

				final String file1Extension = path1.getFileExtension().toLowerCase();
				final String file2Extension = path2.getFileExtension().toLowerCase();

				if (file1Extension != null
						&& file1Extension.length() > 0
						&& file2Extension != null
						&& file2Extension.length() > 0) {

					final TourbookDevice file1Device = _devicesByExtension.get(file1Extension);
					final TourbookDevice file2Device = _devicesByExtension.get(file2Extension);

					if (file1Device != null && file2Device != null) {
						return file1Device.extensionSortPriority - file2Device.extensionSortPriority;
					}
				}

				// sort invalid files to the end
				return Integer.MAX_VALUE;
			}
		});

		setImportCanceled(false);

		try {
			new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(
					true,
					false,
					new IRunnableWithProgress() {

						public void run(final IProgressMonitor monitor) throws InvocationTargetException,
								InterruptedException {

							int workedDone = 0;
							final int workedAll = selectedFilePaths.size();

							monitor.beginTask(Messages.import_data_importTours_task, workedAll);

							setImportId();

							int importCounter = 0;
							final ArrayList<String> notImportedFiles = new ArrayList<String>();

							// loop: import all selected files
							for (final IPath filePath : selectedFilePaths) {

								monitor.worked(1);
								monitor.subTask(NLS.bind(Messages.import_data_importTours_subTask, //
										new Object[] { workedDone++, workedAll, filePath }));

								final String osFilePath = filePath.toOSString();

								// ignore files which are imported as children from other imported files
								if (_importedFileNamesChildren.contains(osFilePath)) {
									continue;
								}

								if (importRawData(new File(osFilePath), null, false, null)) {
									importCounter++;
								} else {
									notImportedFiles.add(osFilePath);
								}
							}

							if (importCounter > 0) {

								updateTourDataFromDb(monitor);

								Display.getDefault().syncExec(new Runnable() {
									public void run() {
										final RawDataView view = showRawDataView();
										if (view != null) {
											view.reloadViewer();
											view.selectFirstTour();
										}
									}
								});
							}

							if (notImportedFiles.size() > 0) {
								showMsgBoxInvalidFormat(notImportedFiles);
							}
						}
					});

		} catch (final InvocationTargetException e) {
			StatusUtil.log(e);
		} catch (final InterruptedException e) {
			StatusUtil.log(e);
		}
	}

	public DeviceData getDeviceData() {
		return _deviceData;
	}

	private List<TourbookDevice> getDeviceListSortedByPriority() {

		if (_devicesBySortPriority == null) {

			_devicesBySortPriority = new ArrayList<TourbookDevice>(DeviceManager.getDeviceList());

			// sort device list by sorting priority
			Collections.sort(_devicesBySortPriority, new Comparator<TourbookDevice>() {
				public int compare(final TourbookDevice o1, final TourbookDevice o2) {

					// 1. sort by prio
					final int sortByPrio = o1.extensionSortPriority - o2.extensionSortPriority;

					// 2. sort by name
					if (sortByPrio == 0) {
						return o1.deviceId.compareTo(o2.deviceId);
					}

					return sortByPrio;
				}
			});

			_devicesByExtension = new HashMap<String, TourbookDevice>();

			for (final TourbookDevice device : _devicesBySortPriority) {
				_devicesByExtension.put(device.fileExtension.toLowerCase(), device);
			}
		}

		return _devicesBySortPriority;
	}

	public HashSet<String> getImportedFiles() {
		return _importedFileNames;
	}

	/**
	 * @return Returns all {@link TourData} which has been imported or received, tour id is the key
	 */
	public HashMap<Long, TourData> getImportedTours() {
		return _importedTourData;
	}

	/**
	 * @return Returns the import year or <code>-1</code> when the year was not set
	 */
	public int getImportYear() {
		return _importSettingsImportYear;
	}

	/**
	 * Import a tour from a file, all imported tours can be retrieved with
	 * {@link #getImportedTours()}
	 * 
	 * @param importFile
	 *            the file to be imported
	 * @param destinationPath
	 *            if not null copy the file to this path
	 * @param buildNewFileNames
	 *            if <code>true</code> create a new filename depending on the content of the file,
	 *            keep old name if false
	 * @param fileCollision
	 *            behavior if destination file exists (ask if null)
	 * @return Returns <code>true</code> when the import was successfully
	 */
	public boolean importRawData(	final File importFile,
									final String destinationPath,
									final boolean buildNewFileNames,
									final FileCollisionBehavior fileCollision) {

		final String filePathName = importFile.getAbsolutePath();

		// check if importFile exist
		if (importFile.exists() == false) {

			final Shell activeShell = Display.getDefault().getActiveShell();

			// during initialisation there is no active shell
			if (activeShell != null) {
				final MessageBox msgBox = new MessageBox(activeShell, SWT.ICON_ERROR | SWT.OK);

				msgBox.setText(Messages.DataImport_Error_file_does_not_exist_title);
				msgBox.setMessage(NLS.bind(Messages.DataImport_Error_file_does_not_exist_msg, filePathName));

				msgBox.open();
			}

			return false;
		}

		// find the file extension in the filename
		final int dotPos = filePathName.lastIndexOf("."); //$NON-NLS-1$
		if (dotPos == -1) {
			return false;
		}
		final String fileExtension = filePathName.substring(dotPos + 1);

		final List<TourbookDevice> deviceList = getDeviceListSortedByPriority();

		_isImported = false;

		BusyIndicator.showWhile(null, new Runnable() {

			public void run() {

				boolean isDataImported = false;
				final ArrayList<String> additionalImportedFiles = new ArrayList<String>();

				/*
				 * try to import from all devices which have the defined extension
				 */
				for (final TourbookDevice device : deviceList) {

					final String deviceFileExtension = device.fileExtension;

					if (deviceFileExtension.equals("*") || deviceFileExtension.equalsIgnoreCase(fileExtension)) { //$NON-NLS-1$

						// device file extension was found in the filename extension

						if (importWithDevice(device, filePathName, destinationPath, buildNewFileNames, fileCollision)) {

							isDataImported = true;
							_isImported = true;

							final ArrayList<String> deviceImportedFiles = device.getAdditionalImportedFiles();
							if (deviceImportedFiles != null) {
								additionalImportedFiles.addAll(deviceImportedFiles);
							}

							break;
						}
						if (_isImportCanceled) {
							break;
						}
					}
				}

				if (isDataImported == false && !_isImportCanceled) {

					/*
					 * when data has not imported yet, try all available devices without checking
					 * the file extension
					 */
					for (final TourbookDevice device : deviceList) {
						if (importWithDevice(device, filePathName, destinationPath, buildNewFileNames, fileCollision)) {

							isDataImported = true;
							_isImported = true;

							final ArrayList<String> otherImportedFiles = device.getAdditionalImportedFiles();
							if (otherImportedFiles != null) {
								additionalImportedFiles.addAll(otherImportedFiles);
							}

							break;
						}
					}
				}

				if (isDataImported) {

					_importedFileNames.add(_lastImportedFile);

					if (additionalImportedFiles.size() > 0) {
						_importedFileNamesChildren.addAll(additionalImportedFiles);
					}
				}

				// cleanup
				additionalImportedFiles.clear();
			}
		});

		return _isImported;
	}

	/**
	 * import the raw data of the given file
	 * 
	 * @param device
	 *            the device which is able to process the data of the file
	 * @param sourceFileName
	 *            the file to be imported
	 * @param destinationPath
	 *            if not null copy the file to this path
	 * @param buildNewFileName
	 *            if true create a new filename depending on the content of the file, keep old name
	 *            if false
	 * @param fileCollision
	 *            behavior if destination file exists (ask if null)
	 * @return Return <code>true</code> when the data have been imported
	 */
	private boolean importWithDevice(	final TourbookDevice device,
										String sourceFileName,
										final String destinationPath,
										final boolean buildNewFileName,
										FileCollisionBehavior fileCollision) {

		if (fileCollision == null) {
			fileCollision = new FileCollisionBehavior();
		}

		device.setIsChecksumValidation(_importSettingsIsChecksumValidation);

		if (device.validateRawData(sourceFileName)) {

			// file contains valid raw data for the raw data reader

			if (_importSettingsImportYear != -1) {
				device.setImportYear(_importSettingsImportYear);
			}

			device.setMergeTracks(_importSettingsIsMergeTracks);
			device.setCreateTourIdWithTime(_importSettingsCreateTourIdWithTime);

			// copy file to destinationPath
			if (destinationPath != null) {

				final String newFileName = importWithDeviceCopyFile(
						device,
						sourceFileName,
						destinationPath,
						buildNewFileName,
						fileCollision);

				if (newFileName == null) {
					return false;
				}

				sourceFileName = newFileName;
			}

			_lastImportedFile = sourceFileName;

			return device.processDeviceData(sourceFileName, _deviceData, _importedTourData);
		}

		return false;

	}

	private String importWithDeviceCopyFile(final TourbookDevice device,
											final String sourceFileName,
											final String destinationPath,
											final boolean buildNewFileName,
											final FileCollisionBehavior fileCollision) {

		String destFileName = new File(sourceFileName).getName();

		if (buildNewFileName) {

			destFileName = null;

			try {
				destFileName = device.buildFileNameFromRawData(sourceFileName);
			} catch (final Exception e) {
				StatusUtil.log(e);
			} finally {

				if (destFileName == null) {

					MessageDialog.openError(Display.getDefault().getActiveShell(), "Error Creating Filename", //$NON-NLS-1$
							"The filename for the received data" //$NON-NLS-1$
									+ " could not be created from the file '" //$NON-NLS-1$
									+ sourceFileName
									+ "'\n\n" //$NON-NLS-1$
									+ "The received data will be saved in the temp file '" //$NON-NLS-1$
									+ new Path(destinationPath).addTrailingSeparator().toString()
									+ TEMP_IMPORTED_FILE
									+ "'\n\n" //$NON-NLS-1$
									+ "The possible reason could be that the transfered data are corrupted." //$NON-NLS-1$
									+ "\n\n" //$NON-NLS-1$
									+ "When you think the received data are correct, " //$NON-NLS-1$
									+ "you can send the received data file to the author of MyTourbook to analyze it."); //$NON-NLS-1$

					destFileName = TEMP_IMPORTED_FILE;
				}
			}
		}
		final File newFile = new File((new Path(destinationPath).addTrailingSeparator().toString() + destFileName));

		// get source file
		final File fileIn = new File(sourceFileName);

		// check if file already exist
		if (newFile.exists()) {
			// TODO allow user to rename the file

			boolean keepFile = false; // for MessageDialog result
			if (fileCollision.value == FileCollisionBehavior.ASK) {

				final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				final MessageDialog messageDialog = new MessageDialog(
						shell,
						Messages.Import_Wizard_Message_Title,
						null,
						NLS.bind(Messages.Import_Wizard_Message_replace_existing_file, newFile),
						MessageDialog.QUESTION,
						new String[] {
								IDialogConstants.YES_LABEL,
								IDialogConstants.YES_TO_ALL_LABEL,
								IDialogConstants.NO_LABEL,
								IDialogConstants.NO_TO_ALL_LABEL },
						0);
				messageDialog.open();
				final int returnCode = messageDialog.getReturnCode();
				switch (returnCode) {

				case 1: // YES_TO_ALL
					fileCollision.value = FileCollisionBehavior.REPLACE;
					break;

				case 3: // NO_TO_ALL
					fileCollision.value = FileCollisionBehavior.KEEP;
				case 2: // NO
					keepFile = true;
					break;

				default:
					break;
				}
			}

			if (fileCollision.value == FileCollisionBehavior.KEEP || keepFile) {
				_isImportCanceled = true;
				fileIn.delete();
				return null;
			}
		}

		// copy source file into destination file
		FileInputStream inReader = null;
		FileOutputStream outReader = null;
		try {
			inReader = new FileInputStream(fileIn);
			outReader = new FileOutputStream(newFile);
			int c;

			while ((c = inReader.read()) != -1) {
				outReader.write(c);
			}

			inReader.close();
			outReader.close();

		} catch (final FileNotFoundException e) {
			StatusUtil.log(e);
			return null;
		} catch (final IOException e) {
			StatusUtil.log(e);
			return null;
		} finally {
			// close the files
			if (inReader != null) {
				try {
					inReader.close();
				} catch (final IOException e) {
					StatusUtil.log(e);
					return null;
				}
			}
			if (outReader != null) {
				try {
					outReader.close();
				} catch (final IOException e) {
					StatusUtil.log(e);
					return null;
				}
			}
		}

		// delete source file
		fileIn.delete();

		return newFile.getAbsolutePath();
	}

	public void removeAllTours() {
		_importedTourData.clear();
		_importedFileNames.clear();
		_importedFileNamesChildren.clear();
	}

	public void removeTours(final TourData[] removedTours) {

		final HashSet<?> oldFileNames = (HashSet<?>) _importedFileNames.clone();

		for (final Object item : removedTours) {

			final TourData tourData = (TourData) item;
			final Long key = tourData.getTourId();

			if (_importedTourData.containsKey(key)) {
				_importedTourData.remove(key);
			}
		}

		/*
		 * Check if all tours from a file are removed, when yes, remove file path that the file will
		 * not reimported. When at least one tour is still used, all tours will be reimported
		 * because it's not yet saved which tours are removed from a file and which are not.
		 */
		for (final Object item : oldFileNames) {
			if (item instanceof String) {

				final String oldFilePath = (String) item;
				boolean isNeeded = false;

				for (final TourData tourData : _importedTourData.values()) {

					final String tourFilePath = tourData.getTourImportFilePathRaw();

					if (tourFilePath != null && tourFilePath.equals(oldFilePath)) {
						isNeeded = true;
						break;
					}
				}

				if (isNeeded == false) {
					// file path is not needed any more
					_importedFileNames.remove(oldFilePath);
				}
			}
		}
	}

	public void setCreateTourIdWithTime(final boolean isActionChecked) {
		_importSettingsCreateTourIdWithTime = isActionChecked;
	}

	public void setImportCanceled(final boolean importCanceled) {
		_isImportCanceled = importCanceled;
	}

	/**
	 * Sets a unique id into the device data so that each import can be identified.
	 */
	public void setImportId() {
		_deviceData.importId = System.currentTimeMillis();
	}

	public void setImportYear(final int year) {
		_importSettingsImportYear = year;
	}

	public void setIsChecksumValidation(final boolean checked) {
		_importSettingsIsChecksumValidation = checked;
	}

	public void setMergeTracks(final boolean checked) {
		_importSettingsIsMergeTracks = checked;
	}

	private RawDataView showRawDataView() {

		final IWorkbench workbench = PlatformUI.getWorkbench();
		final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

		try {

			final IViewPart rawDataView = window.getActivePage().findView(RawDataView.ID);

			if (rawDataView == null) {

				// show raw data perspective when raw data view is not visible
				workbench.showPerspective(PerspectiveFactoryRawData.PERSPECTIVE_ID, window);
			}

			// show raw data view
			return (RawDataView) Util.showView(RawDataView.ID);

		} catch (final WorkbenchException e) {
			StatusUtil.log(e);
		}
		return null;
	}

	/**
	 * update {@link TourData} from the database for all imported tours, displays a progress dialog
	 * 
	 * @param monitor
	 */
	public void updateTourDataFromDb(final IProgressMonitor monitor) {

		if (_importedTourData.size() < 5) {
			updateTourDataFromDbRunnable(null);
		} else {

			if (monitor == null) {
				try {
					new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(
							true,
							false,
							new IRunnableWithProgress() {

								public void run(final IProgressMonitor monitor) throws InvocationTargetException,
										InterruptedException {

									updateTourDataFromDbRunnable(monitor);
								}
							});

				} catch (final InvocationTargetException e) {
					StatusUtil.log(e);
				} catch (final InterruptedException e) {
					StatusUtil.log(e);
				}
			} else {
				updateTourDataFromDbRunnable(monitor);
			}
		}
	}

	private void updateTourDataFromDbRunnable(final IProgressMonitor monitor) {

		int workedDone = 0;
		final int workedAll = _importedTourData.size();

		if (monitor != null) {
			monitor.beginTask(Messages.import_data_updateDataFromDatabase_task, workedAll);
		}

		long editorTourId = -1;

		final TourDataEditorView tourDataEditor = TourManager.getTourDataEditor();
		if (tourDataEditor != null) {
			final TourData tourData = tourDataEditor.getTourData();
			if (tourData != null) {
				editorTourId = tourData.getTourId();
			}
		}

		for (final TourData mapTourData : _importedTourData.values()) {

			if (monitor != null) {
				monitor.worked(1);
				monitor.subTask(NLS.bind(Messages.import_data_updateDataFromDatabase_subTask, workedDone++, workedAll));
			}

			if (mapTourData.isTourDeleted) {
				continue;
			}

			final Long tourId = mapTourData.getTourId();

			try {

				final TourData dbTourData = TourManager.getInstance().getTourDataFromDb(tourId);
				if (dbTourData != null) {

					/*
					 * tour is saved in the database, set rawdata file name to display the filepath
					 */
					dbTourData.importRawDataFile = mapTourData.importRawDataFile;

					final Long dbTourId = dbTourData.getTourId();

					// replace existing tours but do not add new tours
					if (_importedTourData.containsKey(dbTourId)) {

						/*
						 * check if the tour editor contains this tour, this should not be
						 * necessary, just make sure the correct tour is used !!!
						 */
						if (editorTourId == dbTourId) {
							_importedTourData.put(dbTourId, tourDataEditor.getTourData());
						} else {
							_importedTourData.put(dbTourId, dbTourData);
						}
					}
				}
			} catch (final Exception e) {
				StatusUtil.log(e);
			}
		}

		// prevent async error
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, null);
			}
		});
	}

	/**
	 * Updates the model with modified tours
	 * 
	 * @param modifiedTours
	 */
	public void updateTourDataModel(final ArrayList<TourData> modifiedTours) {

		for (final TourData tourData : modifiedTours) {
			if (tourData != null) {

				final Long tourId = tourData.getTourId();

				// replace existing tour do not add new tours
				if (_importedTourData.containsKey(tourId)) {
					_importedTourData.put(tourId, tourData);
				}
			}
		}
	}
}
