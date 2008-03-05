/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.Messages;
import net.tourbook.application.PerspectiveFactoryRawData;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

public class RawDataManager {

	public static final String			TEMP_RAW_DATA_FILE			= "temp-device-data.txt";						//$NON-NLS-1$

	protected static final String		RAW_DATA_LAST_SELECTED_PATH	= "raw-data-view.last-selected-import-path"; //$NON-NLS-1$

	private static RawDataManager		instance					= null;

	/**
	 * contains the device data imported from the device/file
	 */
	private DeviceData					fDeviceData					= new DeviceData();

	/**
	 * contains the tour data which were imported or received
	 */
	private HashMap<String, TourData>	fTourDataMap				= new HashMap<String, TourData>();

	/**
	 * contains the filenames for all imported files
	 */
	private HashSet<String>				fImportedFiles				= new HashSet<String>();

	private int							fImportYear					= -1;

	private boolean						fIsImported;

	private boolean						fIsMergeTracks;

	private RawDataManager() {}

	public static RawDataManager getInstance() {
		if (instance == null) {
			instance = new RawDataManager();
		}
		return instance;
	}

	/**
	 * @return Returns the file to the temp data file
	 */
	public static String getTempDataFileName() {

		return TourbookPlugin.getDefault().getStateLocation().append(TEMP_RAW_DATA_FILE).toFile().getAbsolutePath();
	}

	public void executeImportFromDevice() {

		new WizardImportDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
				new WizardImportData(),
				Messages.Import_Wizard_Dlg_title).open();

		showRawDataView();
	}

	public void executeImportFromDeviceDirect() {

		final IWorkbench workbench = PlatformUI.getWorkbench();
		final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

		final WizardImportData importWizard = new WizardImportData();

		final WizardDialog dialog = new WizardImportDialog(window.getShell(),
				importWizard,
				Messages.Import_Wizard_Dlg_title);

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

		final ArrayList<TourbookDevice> deviceList = DeviceManager.getDeviceList();

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
		String lastSelectedPath = prefStore.getString(RAW_DATA_LAST_SELECTED_PATH);

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
				String selectedPath = filePath.removeLastSegments(1).makeAbsolute().toString();
				prefStore.putValue(RAW_DATA_LAST_SELECTED_PATH, selectedPath);

				// loop: import all selected files
				for (String fileName : selectedFileNames) {

					// replace filename, keep the directory path
					fileName = filePath.removeLastSegments(1).append(fileName).makeAbsolute().toString();

					if (rawDataManager.importRawData(fileName)) {
						importCounter++;
					} else {
						notImportedFiles.add(fileName);
					}
				}

				if (importCounter > 0) {

					rawDataManager.updateTourDataFromDb();

					RawDataView view = showRawDataView();
					if (view != null) {
						view.updateViewer();
						view.selectFirstTour();
					}
				}

				if (notImportedFiles.size() > 0) {
					showMsgBoxInvalidFormat(notImportedFiles);
				}
			}
		});

	}

	private RawDataView showRawDataView() {

		final IWorkbench workbench = PlatformUI.getWorkbench();
		final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

		try {
			// show raw data perspective
			workbench.showPerspective(PerspectiveFactoryRawData.PERSPECTIVE_ID, window);

			// show raw data view
			return (RawDataView) window.getActivePage().showView(RawDataView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);

		} catch (WorkbenchException e) {
			e.printStackTrace();
		}
		return null;
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
		return fImportYear;
	}

	public HashMap<String, TourData> getTourDataMap() {
		return fTourDataMap;
	}

	/**
	 * Import the raw data from a file and save the imported data in the fields
	 * <code>fDeviceData</code> and <code>fTourData</code>
	 * 
	 * @param fileName
	 * @param isDeviceImport
	 * @return Returns <code>true</code> when the import was successfully
	 */
	public boolean importRawData(final String fileName) {

		File importFile = new File(fileName);

		// check if file exist
		if (importFile.exists() == false) {

			MessageBox msgBox = new MessageBox(Display.getDefault().getActiveShell(), SWT.ICON_ERROR | SWT.OK);

			msgBox.setText(Messages.DataImport_Error_file_does_not_exist_title);
			msgBox.setMessage(NLS.bind(Messages.DataImport_Error_file_does_not_exist_msg, fileName));

			msgBox.open();

			return false;
		}

		// find the file extension in the filename
		int dotPos = fileName.lastIndexOf("."); //$NON-NLS-1$
		if (dotPos == -1) {
			return false;
		}
		final String fileExtension = fileName.substring(dotPos + 1);

		final ArrayList<TourbookDevice> deviceList = DeviceManager.getDeviceList();
		fIsImported = false;

		BusyIndicator.showWhile(null, new Runnable() {

			public void run() {

				boolean isDataImported = false;

				/*
				 * try to import from all devices which have the same extension
				 */
				for (TourbookDevice device : deviceList) {

					if (device.fileExtension.equalsIgnoreCase(fileExtension)) {

						// device file extension was found in the filename extension

						if (importRawDataFromFile(device, fileName)) {
							isDataImported = true;
							fIsImported = true;
							break;
						}
					}
				}

				if (isDataImported == false) {

					/*
					 * when data has not imported yet, try all available devices without checking
					 * the file extension
					 */
					for (TourbookDevice device : deviceList) {
						if (importRawDataFromFile(device, fileName)) {
							isDataImported = true;
							fIsImported = true;
							break;
						}
					}
				}

				if (isDataImported) {
					fImportedFiles.add(fileName);
				}
			}
		});

		return fIsImported;
	}

	/**
	 * import the raw data for the device
	 * 
	 * @param device
	 * @param fileName
	 * @return
	 */
	private boolean importRawDataFromFile(TourbookDevice device, String fileName) {

		if (device.validateRawData(fileName)) {

			// file contains valid raw data for the raw data reader

			if (fImportYear != -1) {
				device.setImportYear(fImportYear);
			}

			device.setMergeTracks(fIsMergeTracks);

			if (device.processDeviceData(fileName, fDeviceData, fTourDataMap)) {
				return true;
			}
		}

		return false;
	}

	public void removeAllTours() {
		fTourDataMap.clear();
		fImportedFiles.clear();
	}

	public void setImportYear(int year) {
		fImportYear = year;
	}

	public static void showMsgBoxInvalidFormat(ArrayList<String> notImportedFiles) {

		MessageBox msgBox = new MessageBox(Display.getCurrent().getActiveShell(), SWT.ICON_ERROR | SWT.OK);

		StringBuilder fileText = new StringBuilder();
		for (String fileName : notImportedFiles) {
			fileText.append('\n');
			fileText.append(fileName);
		}

		msgBox.setMessage(NLS.bind(Messages.DataImport_Error_invalid_data_format, fileText.toString()));
		msgBox.open();
	}

	/**
	 * get the tourdata from the database when available
	 */
	public void updateTourDataFromDb_NOTWORKING() {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			@SuppressWarnings("unchecked") //$NON-NLS-1$
			public void run() {

				if (fTourDataMap.size() == 0) {
					// nothing to do
					return;
				}

				/*
				 * get tour id's in a list
				 */
				ArrayList<Long> tourIdList = new ArrayList<Long>();
				for (TourData tourData : fTourDataMap.values()) {
					tourIdList.add(tourData.getTourId());
				}

//				// Named parameter list
//				List names = new ArrayList();
//				names.add("Izi");
//				names.add("Fritz");
//				Query q = em.createQuery("select cat from DomesticCat cat where cat.name in (:namesList)");
//				q.setParameter("namesList", names);
//				List cats = q.list();

				EntityManager em = TourDatabase.getInstance().getEntityManager();

				final String sqlQuery = "SELECT TourData " //$NON-NLS-1$
						+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " tourdata ") //$NON-NLS-1$ //$NON-NLS-2$
						+ (" WHERE tourdata.tourId IN (:tourIdList)");//$NON-NLS-1$

				try {

					if (em != null) {

						Query query = em.createQuery(sqlQuery);

						query.setParameter("tourIdList", tourIdList); //$NON-NLS-1$

						List tourDataList = query.getResultList();

						for (TourData tourDataFromMap : fTourDataMap.values()) {

							final long tourIdFromMap = tourDataFromMap.getTourId().longValue();
							boolean isFound = false;

							for (Object dbObject : tourDataList) {

								TourData tourDataFromDb = (TourData) dbObject;

								if (tourDataFromDb.getTourId().longValue() == tourIdFromMap) {

									/*
									 * tour is available in the database, replace the imported tour
									 * with the tour from the database
									 */

									final TourData tourDataFromDB = (TourData) tourDataList.get(0);

									tourDataFromDB.importRawDataFile = tourDataFromMap.importRawDataFile;

									fTourDataMap.put(tourDataFromDB.getTourId().toString(), tourDataFromDB);

									isFound = true;
									break;
								}
							}

							if (isFound == false) {

								/*
								 * tour is not in the databse, therefore the tour was deleted and
								 * the person in the tour data must be removed
								 */

								tourDataFromMap.setTourPerson(null);
							}
						}
					}
				} catch (Exception e) {
					System.err.println(sqlQuery);
//					System.err.println("tourIdList=" + tourIdList.toString());
					e.printStackTrace();
				} finally {
					em.close();
				}
			}
		});
	}

	/**
	 * get the tourdata from the database when available
	 */
	public void updateTourDataFromDb() {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			@SuppressWarnings("unchecked") //$NON-NLS-1$
			public void run() {

				EntityManager em = TourDatabase.getInstance().getEntityManager();

				final String sqlQuery = "SELECT TourData " //$NON-NLS-1$
						+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " TourData ") //$NON-NLS-1$ //$NON-NLS-2$
						+ (" WHERE tourId = :tourId"); //$NON-NLS-1$

				long tourId = -1;

				try {

					if (em != null) {

						Query query = em.createQuery(sqlQuery);

						for (TourData tourDataFromMap : fTourDataMap.values()) {

							tourId = tourDataFromMap.getTourId();
							query.setParameter("tourId", tourId); //$NON-NLS-1$

							List peopleList = query.getResultList();

							if (peopleList.size() != 0) {

								// tour is in the database, replace the imported tour with the tour from the database

								final TourData tourDataFromDB = (TourData) peopleList.get(0);

								tourDataFromDB.importRawDataFile = tourDataFromMap.importRawDataFile;

								fTourDataMap.put(tourDataFromDB.getTourId().toString(), tourDataFromDB);

							} else {

								// when a tour was deleted the person in the tour data must be removed

								tourDataFromMap.setTourPerson(null);
							}
						}

					}
				} catch (Exception e) {
					System.err.println(sqlQuery);
					System.err.println("tourId=" + tourId); //$NON-NLS-1$
					e.printStackTrace();
				} finally {
					em.close();
				}
			}
		});
	}

	public void setMergeTracks(boolean checked) {
		fIsMergeTracks = checked;
	}
}
