/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.PerspectiveFactoryRawData;
import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

public class RawDataManager {

//	public static final String			TEMP_RAW_DATA_FILE					= "temp-device-data.txt";						//$NON-NLS-1$

	private static final String			RAW_DATA_LAST_SELECTED_PATH			= "raw-data-view.last-selected-import-path";	//$NON-NLS-1$

	private static final String			TEMP_IMPORTED_FILE					= "received-device-data.txt";

	private static RawDataManager		instance							= null;

	/**
	 * contains the device data imported from the device/file
	 */
	private DeviceData					fDeviceData							= new DeviceData();

	/**
	 * contains the tour data which were imported or received
	 */
	private HashMap<String, TourData>	fTourDataMap						= new HashMap<String, TourData>();

	/**
	 * contains the filenames for all imported files
	 */
	private HashSet<String>				fImportedFiles						= new HashSet<String>();

	private boolean						fIsImported;

	private boolean						fImportCanceled;

	private int							fImportSettingsImportYear			= -1;
	private boolean						fImportSettingsIsMergeTracks;
	private boolean						fImportSettingsIsChecksumValidation	= true;

	private String						fLastImportedFile;

	public static RawDataManager getInstance() {
		if (instance == null) {
			instance = new RawDataManager();
		}
		return instance;
	}

//	/**
//	 * @return Returns the file to the temp data file
//	 */
//	public static String getTempDataFileName() {
//
//		return TourbookPlugin.getDefault().getStateLocation().append(TEMP_RAW_DATA_FILE).toFile().getAbsolutePath();
//	}

	/**
	 * @return Return the temp directory where received data are stored temporarily
	 */
	public static String getTempDir() {
		return TourbookPlugin.getDefault().getStateLocation().toFile().getAbsolutePath();
	}

	public static void showMsgBoxInvalidFormat(final ArrayList<String> notImportedFiles) {

		final MessageBox msgBox = new MessageBox(Display.getCurrent().getActiveShell(), SWT.ICON_ERROR | SWT.OK);

		final StringBuilder fileText = new StringBuilder();
		for (final String fileName : notImportedFiles) {
			fileText.append('\n');
			fileText.append(fileName);
		}

		msgBox.setMessage(NLS.bind(Messages.DataImport_Error_invalid_data_format, fileText.toString()));
		msgBox.open();
	}

	private RawDataManager() {}

	public void executeImportFromDevice() {

		new WizardImportDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
				new WizardImportData(),
				Messages.Import_Wizard_Dlg_title).open();

		showRawDataView();
	}

	public void executeImportFromDeviceDirect() {

		final WizardImportData importWizard = new WizardImportData();

		final WizardDialog dialog = new WizardImportDialog(PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow()
				.getShell(), importWizard, Messages.Import_Wizard_Dlg_title);

		// create the dialog and shell which is required in setAutoDownload()
		dialog.create();

		importWizard.setAutoDownload();

		dialog.open();

		showRawDataView();
	}

	/**
	 * import tour data from a file
	 */
	public void executeImportFromFile() {

		final List<TourbookDevice> deviceList = DeviceManager.getDeviceList();

		// sort device list alphabetically
		Collections.sort(deviceList, new Comparator<TourbookDevice>() {
			public int compare(final TourbookDevice o1, final TourbookDevice o2) {
				return o1.visibleName.compareTo(o2.visibleName);
			}
		});

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
		final FileDialog fileDialog = new FileDialog(Display.getCurrent().getActiveShell(), (SWT.OPEN | SWT.MULTI));
		fileDialog.setFilterExtensions(filterExtensions);
		fileDialog.setFilterNames(filterNames);
		fileDialog.setFilterPath(lastSelectedPath);

		// open file dialog
		final String firstFileName = fileDialog.open();

		// check if user canceled the dialog
		if (firstFileName == null) {
			return;
		}

		final String[] selectedFileNames = fileDialog.getFileNames();

		Display.getDefault().asyncExec(new Runnable() {

			public void run() {

				final RawDataManager rawDataManager = RawDataManager.getInstance();
				final ArrayList<String> notImportedFiles = new ArrayList<String>();

				int importCounter = 0;

				final Path filePath = new Path(firstFileName);

				// keep last selected path
				final String selectedPath = filePath.removeLastSegments(1).makeAbsolute().toString();
				prefStore.putValue(RAW_DATA_LAST_SELECTED_PATH, selectedPath);

				// loop: import all selected files
				for (String fileName : selectedFileNames) {

					// replace filename, keep the directory path
					fileName = filePath.removeLastSegments(1).append(fileName).makeAbsolute().toString();

					if (rawDataManager.importRawData(fileName, null, false, null)) {
						importCounter++;
					} else {
						notImportedFiles.add(fileName);
					}
				}

				if (importCounter > 0) {

					rawDataManager.updateTourDataFromDb();

					final RawDataView view = showRawDataView();
					if (view != null) {
						view.reloadViewer();
						view.selectFirstTour();
					}
				}

				if (notImportedFiles.size() > 0) {
					showMsgBoxInvalidFormat(notImportedFiles);
				}
			}
		});

	}

	public DeviceData getDeviceData() {
		return fDeviceData;
	}

	public HashSet<String> getImportedFiles() {
		return fImportedFiles;
	}

	/**
	 * @return Returns the import year or <code>-1</code> when the year was not set
	 */
	public int getImportYear() {
		return fImportSettingsImportYear;
	}

	/**
	 * @return Returns the tour data which were imported or received, key is tour ID
	 */
	public HashMap<String, TourData> getTourDataMap() {
		return fTourDataMap;
	}

	/**
	 * Import the raw data from a file and save the imported data in the fields
	 * <code>fDeviceData</code> and <code>fTourData</code>
	 * 
	 * @param importFile
	 *            the file to be imported
	 * @param destinationPath
	 *            if not null copy the file to this path
	 * @param buildNewFileNames
	 *            if true create a new filename depending on the content of the file, keep old name
	 *            if false
	 * @param fileCollision
	 *            behavior if destination file exists (ask if null)
	 * @return Returns <code>true</code> when the import was successfully
	 */
	public boolean importRawData(	final File importFile,
									final String destinationPath,
									final boolean buildNewFileNames,
									final FileCollisionBehavior fileCollision) {

		final String fileName = importFile.getAbsolutePath();

		// check if importFile exist
		if (importFile.exists() == false) {

			final Shell activeShell = Display.getDefault().getActiveShell();

			// during initialisation there is no active shell
			if (activeShell != null) {
				final MessageBox msgBox = new MessageBox(activeShell, SWT.ICON_ERROR | SWT.OK);

				msgBox.setText(Messages.DataImport_Error_file_does_not_exist_title);
				msgBox.setMessage(NLS.bind(Messages.DataImport_Error_file_does_not_exist_msg, fileName));

				msgBox.open();
			}

			return false;
		}

		// find the file extension in the filename
		final int dotPos = fileName.lastIndexOf("."); //$NON-NLS-1$
		if (dotPos == -1) {
			return false;
		}
		final String fileExtension = fileName.substring(dotPos + 1);

		final List<TourbookDevice> deviceList = DeviceManager.getDeviceList();
		fIsImported = false;

		BusyIndicator.showWhile(null, new Runnable() {

			public void run() {

				boolean isDataImported = false;

				/*
				 * try to import from all devices which have the same extension
				 */
				for (final TourbookDevice device : deviceList) {

					if (device.fileExtension.equalsIgnoreCase(fileExtension)) {

						// device file extension was found in the filename extension

						if (importRawDataFromFile(device, fileName, destinationPath, buildNewFileNames, fileCollision)) {
							isDataImported = true;
							fIsImported = true;
							break;
						}
						if (fImportCanceled) {
							break;
						}
					}
				}

				if (isDataImported == false && !fImportCanceled) {

					/*
					 * when data has not imported yet, try all available devices without checking
					 * the file extension
					 */
					for (final TourbookDevice device : deviceList) {
						if (importRawDataFromFile(device, fileName, destinationPath, buildNewFileNames, fileCollision)) {
							isDataImported = true;
							fIsImported = true;
							break;
						}
					}
				}

				if (isDataImported) {
					fImportedFiles.add(fLastImportedFile);
				}
			}
		});

		return fIsImported;
	}

	/**
	 * Import the raw data from a file and save the imported data in the fields
	 * <code>fDeviceData</code> and <code>fTourData</code>
	 * 
	 * @param importFileName
	 *            the file to be imported
	 * @param destinationPath
	 *            if not null copy the file to this path
	 * @param buildNewFileNames
	 *            if true create a new filename depending on the content of the file, keep old name
	 *            if false
	 * @param fileCollision
	 *            behavior if destination file exists (ask if null)
	 * @return Returns <code>true</code> when the import was successfully
	 */
	public boolean importRawData(	final String importFileName,
									final String destinationPath,
									final boolean buildNewFileNames,
									final FileCollisionBehavior fileCollision) {
		final File importFile = new File(importFileName);
		return importRawData(importFile, destinationPath, buildNewFileNames, fileCollision);
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
	private boolean importRawDataFromFile(	final TourbookDevice device,
											String sourceFileName,
											final String destinationPath,
											final boolean buildNewFileName,
											FileCollisionBehavior fileCollision) {

		if (fileCollision == null) {
			fileCollision = new FileCollisionBehavior();
		}

		device.setIsChecksumValidation(fImportSettingsIsChecksumValidation);

		if (device.validateRawData(sourceFileName)) {

			// file contains valid raw data for the raw data reader

			if (fImportSettingsImportYear != -1) {
				device.setImportYear(fImportSettingsImportYear);
			}

			device.setMergeTracks(fImportSettingsIsMergeTracks);

			// copy file to destinationPath
			if (destinationPath != null) {

				String destFileName = new File(sourceFileName).getName();
				if (buildNewFileName) {

					destFileName = null;

					try {
						destFileName = device.buildFileNameFromRawData(sourceFileName);
					} catch (final Exception e) {
						e.printStackTrace();
					} finally {

						if (destFileName == null) {
							MessageDialog.openError(Display.getCurrent().getActiveShell(),
									"Error Creating Filename",
									"The filename for the received data"
											+ " could not be created from the file '"
											+ sourceFileName
											+ "'\n\n"
											+ "The received data will be saved in the temp file '"
											+ new Path(destinationPath).addTrailingSeparator().toString()
											+ TEMP_IMPORTED_FILE
											+ "'\n\n"
											+ "The possible reason could be that the transfered data are corrupted."
											+ "\n\n"
											+ "When you think the received data are correct, "
											+ "you can send the received data file to the author of MyTourbook so he can analyze it.");

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
						final MessageDialog messageDialog = new MessageDialog(shell,
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
						fImportCanceled = true;
						fileIn.delete();
						return false;
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
					e.printStackTrace();
					return false;
				} catch (final IOException e) {
					e.printStackTrace();
					return false;
				} finally {
					// close the files
					if (inReader != null) {
						try {
							inReader.close();
						} catch (final IOException e) {
							e.printStackTrace();
							return false;
						}
					}
					if (outReader != null) {
						try {
							outReader.close();
						} catch (final IOException e) {
							e.printStackTrace();
							return false;
						}
					}
				}

				// delete source file
				fileIn.delete();
				sourceFileName = newFile.getAbsolutePath();

			}

			fLastImportedFile = sourceFileName;

			return device.processDeviceData(sourceFileName, fDeviceData, fTourDataMap);
		}

		return false;

	}

	public void removeAllTours() {
		fTourDataMap.clear();
		fImportedFiles.clear();
	}

	public void setImportCanceled(final boolean importCanceled) {
		fImportCanceled = importCanceled;
	}

	public void setImportYear(final int year) {
		fImportSettingsImportYear = year;
	}

	public void setIsChecksumValidation(final boolean checked) {
		fImportSettingsIsChecksumValidation = checked;
	}

	public void setMergeTracks(final boolean checked) {
		fImportSettingsIsMergeTracks = checked;
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
			return (RawDataView) window.getActivePage().showView(RawDataView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);

		} catch (final WorkbenchException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * update {@link TourData} from the database for all saved tours
	 */
	public void updateTourDataFromDb() {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				for (final TourData mapTourData : fTourDataMap.values()) {

					if (mapTourData.isTourDeleted == false) {

						final Long tourId = mapTourData.getTourId();

						try {

							final TourData dbTourData = TourManager.getInstance().getTourData(tourId);
							if (dbTourData != null) {

								// tour is in the database, replace the imported tour with the tour from the database

								dbTourData.importRawDataFile = mapTourData.importRawDataFile;

								fTourDataMap.put(dbTourData.getTourId().toString(), dbTourData);
							}
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
				}

			}
		});
	}
}
