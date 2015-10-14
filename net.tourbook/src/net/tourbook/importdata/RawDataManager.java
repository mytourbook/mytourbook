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
package net.tourbook.importdata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
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
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.rawData.RawDataView;
import net.tourbook.ui.views.tourBook.TVITourBookTour;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

public class RawDataManager {

	private static final String					ID									= "net.tourbook.importdata.RawDataManager";				//$NON-NLS-1$

	private static final String					RAW_DATA_LAST_SELECTED_PATH			= "raw-data-view.last-selected-import-path";				//$NON-NLS-1$
	private static final String					TEMP_IMPORTED_FILE					= "received-device-data.txt";								//$NON-NLS-1$

	public static final int						ADJUST_IMPORT_YEAR_IS_DISABLED		= -1;

	private static final String					STATE_AUTOMATED_IMPORT_CONFIG		= "STATE_AUTOMATED_IMPORT_CONFIG";							//$NON-NLS-1$
	private static final String					TAG_IMPORT_CONFIG					= "Config";
	private static final String					TAG_IMPORT_CONFIG_ROOT				= "AutomatedImportConfig";
	private static final String					TAG_SPEED_VERTICES					= "SpeedVertices";
	private static final String					TAG_SPEED_VERTEX					= "Speed";

	private static final String					ATTR_AVG_SPEED						= "avgSpeed";
	private static final String					ATTR_CONFIG_NAME					= "name";													//$NON-NLS-1$
	private static final String					ATTR_IS_SET_TOUR_TYPE				= "isSetTourType";											//$NON-NLS-1$
	private static final String					ATTR_TOUR_TYPE_ID					= "tourTypeId";

	static final int							CONFIG_SPEED_MIN					= 0;
	static final int							CONFIG_SPEED_MAX					= 3000;

	private static final int					CONFIG_SPEED_DEFAULT				= 0;

	private static RawDataManager				_instance							= null;

	private final IPreferenceStore				_prefStore							= TourbookPlugin.getPrefStore();
	private final IDialogSettings				_state								= TourbookPlugin.getState(ID);

	/**
	 * contains the device data imported from the device/file
	 */
	private final DeviceData					_deviceData							= new DeviceData();

	/**
	 * Contains tours which are imported or received and displayed in the import view.
	 */
	private final HashMap<Long, TourData>		_toursInImportView					= new HashMap<Long, TourData>();

	/**
	 * Contains tours which are imported from the last file name.
	 */
	private final HashMap<Long, TourData>		_newlyImportedTours					= new HashMap<Long, TourData>();

	private String								_lastImportedFileName;

	/**
	 * Contains the filenames for all imported files which are displayed in the import view
	 */
	private final HashSet<String>				_importedFileNames					= new HashSet<String>();

	/**
	 * Contains filenames which are not directly imported but is imported from other imported files
	 */
	private final HashSet<String>				_importedFileNamesChildren			= new HashSet<String>();

	private boolean								_isImported;

	private boolean								_isImportCanceled;
	//
	private int									_importState_ImportYear				= ADJUST_IMPORT_YEAR_IS_DISABLED;

	private boolean								_importState_IsConvertWayPoints		= RawDataView.STATE_IS_CONVERT_WAYPOINTS_DEFAULT;
	private boolean								_importState_IsCreateTourIdWithTime	= RawDataView.STATE_IS_CREATE_TOUR_ID_WITH_TIME_DEFAULT;
	private boolean								_importState_IsMergeTracks			= RawDataView.STATE_IS_MERGE_TRACKS_DEFAULT;
	private boolean								_importState_IsChecksumValidation	= RawDataView.STATE_IS_CHECKSUM_VALIDATION_DEFAULT;

	private List<TourbookDevice>				_devicesBySortPriority;

	private HashMap<String, TourbookDevice>		_devicesByExtension;

	private final ArrayList<TourType>			_tempTourTypes						= new ArrayList<TourType>();
	private final ArrayList<TourTag>			_tempTourTags						= new ArrayList<TourTag>();

	/**
	 * Filepath from the previous reimported tour
	 */
	private IPath								_previousSelectedReimportFolder;

	private ArrayList<AutomatedImportConfig>	_importConfigs;

	public static enum ReImport {

		AllTimeSlices, //
		OnlyAltitudeValues, //
		OnlyGearValues, //
		OnlyPowerAndSpeedValues, //
		OnlyTourMarker, //
		OnlyTemperatureValues, //
		Tour, //
	}

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
			@Override
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

	public void actionAutomatedImport() {

	}

	public void actionAutomatedImportConfiguration() {

		final Shell shell = Display.getDefault().getActiveShell();

		final DialogAutomatedImportConfig dialog = new DialogAutomatedImportConfig(shell, getImportConfigs());

		if (dialog.open() == Window.OK) {

			final ArrayList<AutomatedImportConfig> modifiedConfigs = dialog.getModifiedConfigs();

			_importConfigs.clear();
			_importConfigs.addAll(modifiedConfigs);

			saveImportConfig(modifiedConfigs);
		}
	}

	public void actionImportFromDevice() {

		final DataTransferWizardDialog dialog = new DataTransferWizardDialog(//
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
				new DataTransferWizard(),
				Messages.Import_Wizard_Dlg_title);

		if (dialog.open() == Window.OK) {
			showRawDataView();
		}
	}

	public void actionImportFromDeviceDirect() {

		final DataTransferWizard transferWizard = new DataTransferWizard();

		final WizardDialog dialog = new DataTransferWizardDialog(//
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
				transferWizard,
				Messages.Import_Wizard_Dlg_title);

		// create the dialog and shell which is required in setAutoDownload()
		dialog.create();

		transferWizard.setAutoDownload();

		if (dialog.open() == Window.OK) {
			showRawDataView();
		}
	}

	/**
	 * Import tours from files which are selected in a file selection dialog.
	 */
	public void actionImportFromFile() {

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

				final String file1Extension = path1.getFileExtension();
				final String file2Extension = path2.getFileExtension();

				if (file1Extension != null
						&& file1Extension.length() > 0
						&& file2Extension != null
						&& file2Extension.length() > 0) {

					final TourbookDevice file1Device = _devicesByExtension.get(file1Extension.toLowerCase());
					final TourbookDevice file2Device = _devicesByExtension.get(file2Extension.toLowerCase());

					if (file1Device != null && file2Device != null) {
						return file1Device.extensionSortPriority - file2Device.extensionSortPriority;
					}
				}

				// sort invalid files to the end
				return Integer.MAX_VALUE;
			}
		});

		setImportCanceled(false);

		final IRunnableWithProgress importRunnable = new IRunnableWithProgress() {

			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				int imported = 0;
				final int importSize = selectedFilePaths.size();

				monitor.beginTask(Messages.import_data_importTours_task, importSize);

				setImportId();

				int importCounter = 0;
				final ArrayList<String> notImportedFiles = new ArrayList<String>();

				// loop: import all selected files
				for (final IPath filePath : selectedFilePaths) {

					if (monitor.isCanceled()) {
						// stop importing but process imported tours
						break;
					}

					monitor.worked(1);
					monitor.subTask(NLS.bind(Messages.import_data_importTours_subTask, //
							new Object[] { ++imported, importSize, filePath }));

					final String osFilePath = filePath.toOSString();

					// ignore files which are imported as children from other imported files
					if (_importedFileNamesChildren.contains(osFilePath)) {
						continue;
					}

					if (importRawData(new File(osFilePath), null, false, null, true)) {
						importCounter++;
					} else {
						notImportedFiles.add(osFilePath);
					}
				}

				if (importCounter > 0) {

					updateTourDataFromDb(monitor);

					Display.getDefault().syncExec(new Runnable() {
						@Override
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
		};

		try {
			new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, true, importRunnable);
		} catch (final Exception e) {
			StatusUtil.log(e);
		}
	}

	/**
	 * @param reimportId
	 *            ID how a tour is reimported.
	 * @param tourViewer
	 *            Tour viewer where the selected tours should be reimported.
	 */
	public void actionReimportTour(final ReImport reimportId, final ITourViewer3 tourViewer) {

		// check if the tour editor contains a modified tour
		if (TourManager.isTourEditorModified()) {
			return;
		}

		if (actionReimportTour_10_Confirm(reimportId) == false) {
			return;
		}

		final boolean isReImported[] = { false };
		final File reimportedFile[] = { null };

		// prevent async error in the save tour method, cleanup environment
		tourViewer.getPostSelectionProvider().clearSelection();

		Util.clearSelection();

		TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, null);

		// get selected tours
		final IStructuredSelection selectedTours = ((IStructuredSelection) tourViewer.getViewer().getSelection());

		setImportId();
		setImportCanceled(false);

		final IRunnableWithProgress importRunnable = new IRunnableWithProgress() {

			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				int imported = 0;
				final int importSize = selectedTours.size();

				monitor.beginTask(Messages.Import_Data_Dialog_Reimport_Task, importSize);

				// loop: all selected tours in the viewer
				for (final Object element : selectedTours.toArray()) {

					if (monitor.isCanceled()) {
						// stop reimporting but process reimported tours
						break;
					}

					monitor.worked(1);
					monitor.subTask(NLS.bind(Messages.Import_Data_Dialog_Reimport_SubTask, //
							new Object[] { ++imported, importSize }));

					TourData oldTourData = null;

					if (element instanceof TVITourBookTour) {
						oldTourData = TourManager.getInstance().getTourData(((TVITourBookTour) element).getTourId());
					} else if (element instanceof TourData) {
						oldTourData = (TourData) element;
					}

					if (oldTourData == null) {
						continue;
					}

					boolean isTourReImportedFromSameFile = false;

					if (reimportedFile[0] != null && _newlyImportedTours.size() > 0) {

						// this case occures when a file contains multiple tours

						if (actionReimportTour_30(reimportId, reimportedFile[0], oldTourData)) {
							isReImported[0] = true;
							isTourReImportedFromSameFile = true;
						}
					}

					if (isTourReImportedFromSameFile == false) {

						reimportedFile[0] = actionReimportTour_20_GetImportFile(oldTourData);

						if (reimportedFile[0] == null) {
							// user canceled file dialog
							break;
						}

						// import file is available

						if (actionReimportTour_30(reimportId, reimportedFile[0], oldTourData)) {
							isReImported[0] = true;
						}
					}
				}

				if (isReImported[0]) {

					updateTourDataFromDb(null);

					// reselect tours, run in UI thread
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							tourViewer.reloadViewer();
							tourViewer.getViewer().setSelection(selectedTours, true);
						}
					});
				}
			}
		};

		try {
			new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, true, importRunnable);
		} catch (final Exception e) {
			StatusUtil.log(e);
		}
	}

	private boolean actionReimportTour_10_Confirm(final ReImport reimportTour) {

		if (reimportTour == ReImport.Tour) {

			if (actionReimportTour_12_ConfirmDialog(
					ITourbookPreferences.TOGGLE_STATE_REIMPORT_TOUR,
					Messages.Import_Data_Dialog_ConfirmReimport_Message)) {
				return true;
			}

		} else if (reimportTour == ReImport.AllTimeSlices) {

			if (actionReimportTour_12_ConfirmDialog(
					ITourbookPreferences.TOGGLE_STATE_REIMPORT_ALL_TIME_SLICES,
					Messages.Import_Data_Dialog_ConfirmReimportTimeSlices_Message)) {
				return true;
			}

		} else if (reimportTour == ReImport.OnlyAltitudeValues) {

			if (actionReimportTour_12_ConfirmDialog(
					ITourbookPreferences.TOGGLE_STATE_REIMPORT_ALTITUDE_VALUES,
					Messages.Import_Data_Dialog_ConfirmReimportAltitudeValues_Message)) {
				return true;
			}

		} else if (reimportTour == ReImport.OnlyGearValues) {

			if (actionReimportTour_12_ConfirmDialog(
					ITourbookPreferences.TOGGLE_STATE_REIMPORT_GEAR_VALUES,
					Messages.Import_Data_Dialog_ConfirmReimportGearValues_Message)) {
				return true;
			}

		} else if (reimportTour == ReImport.OnlyPowerAndSpeedValues) {

			if (actionReimportTour_12_ConfirmDialog(
					ITourbookPreferences.TOGGLE_STATE_REIMPORT_POWER_AND_SPEED_VALUES,
					Messages.Import_Data_Dialog_ConfirmReimportPowerAndSpeedValues_Message)) {
				return true;
			}

		} else if (reimportTour == ReImport.OnlyTourMarker) {

			if (actionReimportTour_12_ConfirmDialog(
					ITourbookPreferences.TOGGLE_STATE_REIMPORT_TOUR_MARKER,
					Messages.Import_Data_Dialog_ConfirmReimportTourMarker_Message)) {
				return true;
			}

		} else if (reimportTour == ReImport.OnlyTemperatureValues) {

			if (actionReimportTour_12_ConfirmDialog(
					ITourbookPreferences.TOGGLE_STATE_REIMPORT_TEMPERATURE_VALUES,
					Messages.Import_Data_Dialog_ConfirmReimportTemperatureValues_Message)) {
				return true;
			}
		}

		return false;
	}

	private boolean actionReimportTour_12_ConfirmDialog(final String toggleState, final String confirmMessage) {

		if (_prefStore.getBoolean(toggleState)) {

			return true;

		} else {

			final MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(//
					Display.getCurrent().getActiveShell(),//
					Messages.import_data_dlg_reimport_title, //
					confirmMessage, //
					Messages.App_ToggleState_DoNotShowAgain, //
					false, // toggle default state
					null,
					null);

			if (dialog.getReturnCode() == Window.OK) {
				_prefStore.setValue(toggleState, dialog.getToggleState());
				return true;
			}
		}

		return false;
	}

	/**
	 * @param tourData
	 * @return Returns <code>null</code> when the user has canceled the file dialog.
	 */
	private File actionReimportTour_20_GetImportFile(final TourData tourData) {

		final String[] reimportFilePathName = { null };

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {

				final Shell activeShell = Display.getDefault().getActiveShell();

				/*
				 * use path when tour is already imported
				 */
				final String importedFilePath = tourData.importRawDataFile;
				if (importedFilePath != null) {
					// check import file
					final File importFile = new File(importedFilePath);
					if (importFile.exists()) {
						reimportFilePathName[0] = importedFilePath;
					}
				}

				String oldImportFilePathName = null;

				if (reimportFilePathName[0] == null) {

					// get import file name
					oldImportFilePathName = tourData.getTourImportFilePathRaw();
					if (oldImportFilePathName == null) {

						final String tourDateTimeShort = TourManager.getTourDateTimeShort(tourData);
						MessageDialog.openInformation(
								activeShell,
								NLS.bind(Messages.Import_Data_Dialog_Reimport_Title, tourDateTimeShort),
								NLS.bind(Messages.Import_Data_Dialog_GetReimportedFilePath_Message,//
										tourDateTimeShort,
										tourDateTimeShort));

					} else {

						// check import file
						final File importFile = new File(oldImportFilePathName);
						if (importFile.exists()) {

							reimportFilePathName[0] = oldImportFilePathName;

						} else {

							if (_previousSelectedReimportFolder != null) {

								/*
								 * try to use the folder from the previously reimported tour
								 */

								final String oldImportFileName = new Path(oldImportFilePathName).lastSegment();
								final IPath newImportFilePath = _previousSelectedReimportFolder
										.append(oldImportFileName);

								final String newImportFilePathName = newImportFilePath.toOSString();
								final File newImportFile = new File(newImportFilePathName);
								if (newImportFile.exists()) {

									// reimport file exists in the same folder
									reimportFilePathName[0] = newImportFilePathName;
								}
							}

							if (reimportFilePathName[0] == null) {

								MessageDialog.openInformation(
								//
										activeShell,
										Messages.import_data_dlg_reimport_title,
										NLS.bind(
												Messages.Import_Data_Dialog_GetAlternativePath_Message,
												oldImportFilePathName));
							}
						}
					}
				}

				if (reimportFilePathName[0] == null) {

					final String tourDateTimeShort = TourManager.getTourDateTimeShort(tourData);

					final FileDialog dialog = new FileDialog(activeShell, SWT.OPEN);
					dialog.setText(NLS.bind(Messages.Import_Data_Dialog_Reimport_Title, tourDateTimeShort));

					if (oldImportFilePathName != null) {

						// select previous file location

						final IPath importFilePath = new Path(oldImportFilePathName);
						final String importFileName = importFilePath.lastSegment();

						dialog.setFileName(importFileName);
						dialog.setFilterPath(oldImportFilePathName);

					} else if (_previousSelectedReimportFolder != null) {

						dialog.setFilterPath(_previousSelectedReimportFolder.toOSString());
					}

					reimportFilePathName[0] = dialog.open();
				}
			}
		});

		if (reimportFilePathName[0] == null) {
			// user has canceled the file dialog
			return null;
		}

		/*
		 * Keep selected file path which is used to reimport following tours from the same folder
		 * that the user do not have to reselect again and again.
		 */
		_previousSelectedReimportFolder = new Path(reimportFilePathName[0]).removeLastSegments(1);

		return new File(reimportFilePathName[0]);
	}

	private boolean actionReimportTour_30(	final ReImport reimportId,
											final File reimportedFile,
											final TourData oldTourData) {

		boolean isTourReImported = false;
		final Long oldTourId = oldTourData.getTourId();

		/*
		 * tour must be removed otherwise it would be recognized as a duplicate and therefor not
		 * imported
		 */
		final TourData oldTourDataInImportView = _toursInImportView.remove(oldTourId);

		if (importRawData(reimportedFile, null, false, null, false)) {

			/*
			 * tour(s) could be reimported from the file, check if it contains a valid tour
			 */
			TourData newTourData = actionReimportTour_40(reimportId, reimportedFile, oldTourData);

			if (newTourData != null) {

				isTourReImported = true;

				// set reimport file path as new location
				newTourData.setTourImportFilePath(reimportedFile.getAbsolutePath());

				// check if tour is saved
				final TourPerson tourPerson = oldTourData.getTourPerson();
				if (tourPerson != null) {

					// resave tour when the reimported tour was already saved

					newTourData.setTourPerson(tourPerson);

					/*
					 * save tour but don't fire a change event because the tour editor would set the
					 * tour to dirty
					 */
					final TourData savedTourData = TourManager.saveModifiedTour(newTourData, false);

					newTourData = savedTourData;
				}

				// check if tour is displayed in the import view
				if (oldTourDataInImportView != null) {

					// replace tour data in the import view

					_toursInImportView.put(newTourData.getTourId(), newTourData);
				}
			}

		} else {

			if (oldTourDataInImportView != null) {

				// reattach removed tour

				_toursInImportView.put(oldTourId, oldTourDataInImportView);
			}
		}

		return isTourReImported;
	}

	/**
	 * @param reimportId
	 * @param reimportedFile
	 * @param oldTourData
	 * @return Returns {@link TourData} with the reimported time slices.
	 */
	private TourData actionReimportTour_40(	final ReImport reimportId,
											final File reimportedFile,
											final TourData oldTourData) {

		boolean isWrongLength = false;
		boolean isWrongTourId = false;

		for (final TourData reimportedTourData : _newlyImportedTours.values()) {

			if (oldTourData.timeSerie != null && reimportedTourData.timeSerie != null) {

				/*
				 * data series must have the same number of time slices, otherwise the markers can
				 * be off the array bounds, this problem could be solved but takes time to do it.
				 */
				if (oldTourData.timeSerie.length != reimportedTourData.timeSerie.length) {
					isWrongLength = true;
					continue;
				}
			}

			/*
			 * ensure that the reimported tour has the same tour id
			 */
			if (oldTourData.getTourId().longValue() != reimportedTourData.getTourId().longValue()) {
				isWrongTourId = true;
				continue;
			}

			TourData newTourData = null;

			if (reimportId == ReImport.Tour) {

				// replace complete tour

				TourManager.getInstance().removeTourFromCache(oldTourData.getTourId());

				newTourData = reimportedTourData;

			} else if (reimportId == ReImport.AllTimeSlices
					|| reimportId == ReImport.OnlyAltitudeValues
					|| reimportId == ReImport.OnlyGearValues
					|| reimportId == ReImport.OnlyPowerAndSpeedValues
					|| reimportId == ReImport.OnlyTemperatureValues
			//
			) {

				// replace part of the tour

				actionReimportTour_40_TimeSlices(reimportId, oldTourData, reimportedTourData);

				newTourData = oldTourData;

			} else if (reimportId == ReImport.OnlyTourMarker) {

				// reimport only tour markers
				oldTourData.setTourMarkers(reimportedTourData.getTourMarkers());

				newTourData = oldTourData;
			}

			if (newTourData != null) {

				/*
				 * compute computed values
				 */
				newTourData.clearComputedSeries();

				newTourData.computeAltitudeUpDown();
				newTourData.computeTourDrivingTime();
				newTourData.computeComputedValues();

				// maintain list, that another call of this method do not find this tour again
				_newlyImportedTours.remove(oldTourData.getTourId());

				return newTourData;
			}
		}

		/*
		 * reimport failed, display an error message
		 */
		final String oldTourDateTimeShort = TourManager.getTourDateTimeShort(oldTourData);

		if (_newlyImportedTours.size() == 1) {

			// file contains only one tour

			if (isWrongLength) {

				MessageDialog.openInformation(
						Display.getCurrent().getActiveShell(),
						Messages.import_data_dlg_reimport_title,
						NLS.bind(Messages.Import_Data_Dialog_ReimportIsInvalid_WrongSliceNumbers_Message, //
								oldTourDateTimeShort,
								reimportedFile.toString()));

			} else if (isWrongTourId) {
				MessageDialog.openInformation(
						Display.getCurrent().getActiveShell(),
						Messages.import_data_dlg_reimport_title,
						NLS.bind(Messages.Import_Data_Dialog_ReimportIsInvalid_DifferentTourId_Message, //
								oldTourDateTimeShort,
								reimportedFile.toString()));
			}

		} else {

			// file contains multiple tours

			// show common error message
			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.import_data_dlg_reimport_title,
					NLS.bind(Messages.Import_Data_Dialog_ReimportIsInvalid_CommonError_Message, //
							oldTourDateTimeShort,
							reimportedFile.toString()));
		}

		return null;
	}

	private void actionReimportTour_40_TimeSlices(	final ReImport reimportId,
													final TourData oldTourData,
													final TourData reimportedTourData) {

		if (reimportId == ReImport.AllTimeSlices || reimportId == ReImport.OnlyAltitudeValues) {

			// reimport altitude only
			oldTourData.altitudeSerie = reimportedTourData.altitudeSerie;
		}

		if (reimportId == ReImport.AllTimeSlices || reimportId == ReImport.OnlyGearValues) {

			// reimport gear only
			oldTourData.gearSerie = reimportedTourData.gearSerie;
			oldTourData.setFrontShiftCount(reimportedTourData.getFrontShiftCount());
			oldTourData.setRearShiftCount(reimportedTourData.getRearShiftCount());
		}

		if (reimportId == ReImport.AllTimeSlices || reimportId == ReImport.OnlyPowerAndSpeedValues) {

			// reimport power and speed only when it's from the device

			final boolean isDevicePower = reimportedTourData.isPowerSerieFromDevice();
			if (isDevicePower) {
				final float[] powerSerie = reimportedTourData.getPowerSerie();
				if (powerSerie != null) {
					oldTourData.setPowerSerie(powerSerie);
				}
			}

			final boolean isDeviceSpeed = reimportedTourData.isSpeedSerieFromDevice();
			if (isDeviceSpeed) {
				final float[] speedSerie = reimportedTourData.getSpeedSerieFromDevice();
				if (speedSerie != null) {
					oldTourData.setSpeedSerie(speedSerie);
				}
			}
		}

		if (reimportId == ReImport.AllTimeSlices || reimportId == ReImport.OnlyTemperatureValues) {

			// reimport temperature only
			oldTourData.temperatureSerie = reimportedTourData.temperatureSerie;
		}

		if (reimportId == ReImport.AllTimeSlices) {

			// reimport all other data series

			// update device data
			oldTourData.setDeviceFirmwareVersion(reimportedTourData.getDeviceFirmwareVersion());
			oldTourData.setDeviceId(reimportedTourData.getDeviceId());
			oldTourData.setDeviceName(reimportedTourData.getDeviceName());

			oldTourData.cadenceSerie = reimportedTourData.cadenceSerie;
			oldTourData.distanceSerie = reimportedTourData.distanceSerie;
			oldTourData.latitudeSerie = reimportedTourData.latitudeSerie;
			oldTourData.longitudeSerie = reimportedTourData.longitudeSerie;
			oldTourData.pulseSerie = reimportedTourData.pulseSerie;
			oldTourData.timeSerie = reimportedTourData.timeSerie;

			oldTourData.setCalories(reimportedTourData.getCalories());
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
				@Override
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

	private ArrayList<AutomatedImportConfig> getImportConfigs() {

		if (_importConfigs == null) {
			_importConfigs = loadImportConfig();
		}

		return _importConfigs;
	}

	public HashSet<String> getImportedFiles() {
		return _importedFileNames;
	}

	/**
	 * @return Returns all {@link TourData} which has been imported or received and are displayed in
	 *         the import view, tour id is the key.
	 */
	public HashMap<Long, TourData> getImportedTours() {
		return _toursInImportView;
	}

	/**
	 * @return Returns the import year or <code>-1</code> when the year was not set
	 */
	public int getImportYear() {
		return _importState_ImportYear;
	}

	public ArrayList<TourTag> getTempTourTags() {
		return _tempTourTags;
	}

	public ArrayList<TourType> getTempTourTypes() {
		return _tempTourTypes;
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
	 * @param isTourDisplayedInImportView
	 *            When <code>true</code>, the newly imported tours are displayed in the import view,
	 *            otherwise they are imported into {@link #_newlyImportedTours} but not displayed in
	 *            the import view.
	 * @return Returns <code>true</code> when the import was successfully
	 */
	public boolean importRawData(	final File importFile,
									final String destinationPath,
									final boolean buildNewFileNames,
									final FileCollisionBehavior fileCollision,
									final boolean isTourDisplayedInImportView) {

		final String importFilePathName = importFile.getAbsolutePath();
		final Display display = Display.getDefault();

		// check if importFile exist
		if (importFile.exists() == false) {

			display.syncExec(new Runnable() {
				@Override
				public void run() {

					final Shell activeShell = display.getActiveShell();

					// during initialisation there is no active shell
					if (activeShell != null) {

						MessageDialog.openError(
								activeShell,
								Messages.DataImport_Error_file_does_not_exist_title,
								NLS.bind(Messages.DataImport_Error_file_does_not_exist_msg, importFilePathName));
					}
				}
			});

			return false;
		}

		// find the file extension in the filename
		final int dotPos = importFilePathName.lastIndexOf("."); //$NON-NLS-1$
		if (dotPos == -1) {
			return false;
		}
		final String fileExtension = importFilePathName.substring(dotPos + 1);

		final List<TourbookDevice> deviceList = getDeviceListSortedByPriority();

		_isImported = false;

		BusyIndicator.showWhile(null, new Runnable() {

			@Override
			public void run() {

				boolean isDataImported = false;
				final ArrayList<String> additionalImportedFiles = new ArrayList<String>();

				/*
				 * try to import from all devices which have the defined extension
				 */
				for (final TourbookDevice device : deviceList) {

					final String deviceFileExtension = device.fileExtension;

					if (deviceFileExtension.equals("*") || deviceFileExtension.equalsIgnoreCase(fileExtension)) { //$NON-NLS-1$

						// Check if the file we want to import requires confirmation and if yes, ask user
						if (device.userConfirmationRequired()) {
							display.syncExec(new Runnable() {
								@Override
								public void run() {
									final Shell activeShell = display.getActiveShell();
									if (activeShell != null) {
										if (MessageDialog.openConfirm(
												Display.getCurrent().getActiveShell(),
												NLS.bind(Messages.DataImport_ConfirmImport_title, device.visibleName),
												device.userConfirmationMessage())) {
											_isImportCanceled = false;
										} else {
											_isImportCanceled = true;
										}
									}
								}
							});
						}

						if (_isImportCanceled) {
							_isImported = true; // don't display an error to the user
							return;
						}

						// device file extension was found in the filename extension
						if (importRawData10(
								device,
								importFilePathName,
								destinationPath,
								buildNewFileNames,
								fileCollision,
								isTourDisplayedInImportView)) {

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
						if (importRawData10(
								device,
								importFilePathName,
								destinationPath,
								buildNewFileNames,
								fileCollision,
								isTourDisplayedInImportView)) {

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

					_importedFileNames.add(_lastImportedFileName);

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
	 * @param isTourDisplayedInImportView
	 * @return Returns <code>true</code> when data has been imported.
	 */
	private boolean importRawData10(final TourbookDevice device,
									String sourceFileName,
									final String destinationPath,
									final boolean buildNewFileName,
									FileCollisionBehavior fileCollision,
									final boolean isTourDisplayedInImportView) {

		if (fileCollision == null) {
			fileCollision = new FileCollisionBehavior();
		}

		_newlyImportedTours.clear();

		device.setIsChecksumValidation(_importState_IsChecksumValidation);

		if (device.validateRawData(sourceFileName)) {

			// file contains valid raw data for the raw data reader

			if (_importState_ImportYear != -1) {
				device.setImportYear(_importState_ImportYear);
			}

			device.setMergeTracks(_importState_IsMergeTracks);
			device.setCreateTourIdWithTime(_importState_IsCreateTourIdWithTime);
			device.setConvertWayPoints(_importState_IsConvertWayPoints);

			// copy file to destinationPath
			if (destinationPath != null) {

				final String newFileName = importRawData20CopyFile(
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

			_lastImportedFileName = sourceFileName;

			boolean isImported = false;

			try {

				isImported = device.processDeviceData(
						sourceFileName,
						_deviceData,
						_toursInImportView,
						_newlyImportedTours);
			} catch (final Exception e) {
				StatusUtil.log(e);
			}

			if (isTourDisplayedInImportView) {
				_toursInImportView.putAll(_newlyImportedTours);
			}

			// keep tours in _newlyImportedTours because they are used when tours are reimported

			return isImported;
		}

		return false;

	}

	private String importRawData20CopyFile(	final TourbookDevice device,
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

					MessageDialog.openError(
							Display.getDefault().getActiveShell(),
							Messages.Import_Data_Error_CreatingFileName_Title,
							NLS.bind(Messages.Import_Data_Error_CreatingFileName_Message, //
									new Object[] {
											sourceFileName,
											new Path(destinationPath).addTrailingSeparator().toString(),
											TEMP_IMPORTED_FILE }));

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

	private ArrayList<AutomatedImportConfig> loadImportConfig() {

		final ArrayList<AutomatedImportConfig> importConfigs = new ArrayList<>();

		final String stateValue = Util.getStateString(_state, STATE_AUTOMATED_IMPORT_CONFIG, null);

		if ((stateValue != null) && (stateValue.length() > 0)) {

			try {

				final Reader reader = new StringReader(stateValue);

				loadImportConfig_Data(XMLMemento.createReadRoot(reader), importConfigs);

			} catch (final WorkbenchException e) {
				// ignore
			}
		}

		return importConfigs;
	}

	private void loadImportConfig_Data(final XMLMemento xmlMemento, final ArrayList<AutomatedImportConfig> importConfigs) {

		final ArrayList<TourType> allTourTypes = TourDatabase.getAllTourTypes();

		for (final IMemento xmlConfig : xmlMemento.getChildren()) {

			final AutomatedImportConfig importConfig = new AutomatedImportConfig();

			importConfig.name = Util.getXmlString(xmlConfig, ATTR_CONFIG_NAME, UI.EMPTY_STRING);
			importConfig.isSetTourType = Util.getXmlBoolean(xmlConfig, ATTR_IS_SET_TOUR_TYPE, true);

			final ArrayList<SpeedVertex> speedVertices = importConfig.speedVertices;

			for (final IMemento xmlSpeed : xmlConfig.getChildren()) {

				final long xmlTourTypeId = Util.getXmlLong(xmlSpeed, ATTR_TOUR_TYPE_ID, 0);

				/*
				 * Check if the loaded tour type id is valid
				 */
				boolean idIsValid = false;

				for (final TourType tourType : allTourTypes) {
					if (tourType.getTypeId() == xmlTourTypeId) {
						idIsValid = true;
						break;
					}
				}

				if (idIsValid) {

					final SpeedVertex speedVertex = new SpeedVertex();

					speedVertex.tourTypeId = xmlTourTypeId;
					speedVertex.avgSpeed = Util.getXmlInteger(
							xmlSpeed,
							ATTR_AVG_SPEED,
							CONFIG_SPEED_DEFAULT,
							CONFIG_SPEED_MIN,
							CONFIG_SPEED_MAX);

					speedVertices.add(speedVertex);
				}
			}
		}
	}

	public void removeAllTours() {

		_toursInImportView.clear();

		_importedFileNames.clear();
		_importedFileNamesChildren.clear();

		_tempTourTags.clear();
		_tempTourTypes.clear();
	}

	public void removeTours(final TourData[] removedTours) {

		final HashSet<?> oldFileNames = (HashSet<?>) _importedFileNames.clone();

		for (final Object item : removedTours) {

			final TourData tourData = (TourData) item;
			final Long key = tourData.getTourId();

			if (_toursInImportView.containsKey(key)) {
				_toursInImportView.remove(key);
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

				for (final TourData tourData : _toursInImportView.values()) {

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

	private void saveImportConfig(final ArrayList<AutomatedImportConfig> modifiedConfigs) {

		// Build the XML block for writing the bindings and active scheme.
		final XMLMemento xmlMemento = XMLMemento.createWriteRoot(TAG_IMPORT_CONFIG_ROOT);

		saveImportConfig_Data(xmlMemento, modifiedConfigs);

		// Write the XML block to the state store.
		final Writer writer = new StringWriter();
		try {

			xmlMemento.save(writer);
			_state.put(STATE_AUTOMATED_IMPORT_CONFIG, writer.toString());

		} catch (final IOException e) {

			StatusUtil.log(e);

		} finally {

			try {
				writer.close();
			} catch (final IOException e) {
				StatusUtil.log(e);
			}
		}
	}

	private void saveImportConfig_Data(	final XMLMemento xmlMemento,
										final ArrayList<AutomatedImportConfig> modifiedConfigs) {

		for (final AutomatedImportConfig importConfig : modifiedConfigs) {

			final IMemento xmlImportConfig = xmlMemento.createChild(TAG_IMPORT_CONFIG);

			xmlImportConfig.putString(ATTR_CONFIG_NAME, importConfig.name);
			xmlImportConfig.putBoolean(ATTR_IS_SET_TOUR_TYPE, importConfig.isSetTourType);

			final IMemento xmlSpeedVertices = xmlImportConfig.createChild(TAG_SPEED_VERTICES);

			for (final SpeedVertex speedVertex : importConfig.speedVertices) {

				final IMemento xmlSpeedVertex = xmlSpeedVertices.createChild(TAG_SPEED_VERTEX);

				Util.setXmlLong(xmlSpeedVertex, ATTR_TOUR_TYPE_ID, speedVertex.tourTypeId);
				xmlSpeedVertex.putInteger(ATTR_AVG_SPEED, speedVertex.avgSpeed);
			}
		}
	}

	public void setCreateTourIdWithTime(final boolean isActionChecked) {
		_importState_IsCreateTourIdWithTime = isActionChecked;
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
		_importState_ImportYear = year;
	}

	public void setIsChecksumValidation(final boolean checked) {
		_importState_IsChecksumValidation = checked;
	}

	public void setMergeTracks(final boolean checked) {
		_importState_IsMergeTracks = checked;
	}

	public void setState_ConvertWayPoints(final boolean isConvertWayPoints) {

		_importState_IsConvertWayPoints = isConvertWayPoints;
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
			return (RawDataView) Util.showView(RawDataView.ID, true);

		} catch (final WorkbenchException e) {
			StatusUtil.log(e);
		}
		return null;
	}

	/**
	 * Update {@link TourData} from the database for all imported tours which are displayed in the
	 * import view, a progress dialog is displayed.
	 * 
	 * @param monitor
	 */
	public void updateTourDataFromDb(final IProgressMonitor monitor) {

		if (_toursInImportView.size() < 5) {
			// don't show progress dialog
			updateTourDataFromDbRunnable(null);
		} else {

			if (monitor == null) {
				try {
					new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(
							true,
							false,
							new IRunnableWithProgress() {

								@Override
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
		final int workedAll = _toursInImportView.size();

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

		for (final TourData mapTourData : _toursInImportView.values()) {

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
					if (_toursInImportView.containsKey(dbTourId)) {

						/*
						 * check if the tour editor contains this tour, this should not be
						 * necessary, just make sure the correct tour is used !!!
						 */
						if (editorTourId == dbTourId) {
							_toursInImportView.put(dbTourId, tourDataEditor.getTourData());
						} else {
							_toursInImportView.put(dbTourId, dbTourData);
						}
					}
				}
			} catch (final Exception e) {
				StatusUtil.log(e);
			}
		}

		// prevent async error
		Display.getDefault().syncExec(new Runnable() {
			@Override
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
				if (_toursInImportView.containsKey(tourId)) {
					_toursInImportView.put(tourId, tourData);
				}
			}
		}
	}

}
