/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Formatter;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class WizardImportData extends Wizard {

	// timeout in milliseconds * 100
	private static final int			RECEIVE_TIMEOUT			= 600;

	public static final String			DIALOG_SETTINGS_SECTION	= "WizardImportData";			//$NON-NLS-1$

	private WizardPageImportSettings	fPageImportSettings;
	private ByteArrayOutputStream		fRawDataBuffer			= new ByteArrayOutputStream();

	/**
	 * contains the device which is used to read the data from it
	 */
	private TourbookDevice				fImportDevice;

	private boolean						fCloseDialog;

	private IRunnableWithProgress		fRunnableReceiveData;

	WizardImportData() {

		setDialogSettings();
		setNeedsProgressMonitor(true);
	}

	public void addPages() {

		fPageImportSettings = new WizardPageImportSettings("import-settings"); //$NON-NLS-1$
		addPage(fPageImportSettings);

	}

	/**
	 * Append newly received data to the internal data buffer
	 * 
	 * @param newData
	 *        data being written into the buffer
	 */
	public void appendReceivedData(int newData) {
		fRawDataBuffer.write(newData);
	}

	/**
	 * @param sourceFileName
	 * @return Returns the path of the saved file or null when it was not saved
	 */
	private String autoSaveRawData(String sourceFileName) {

		// set filename to the transfer date
		DeviceData deviceData = RawDataManager.getInstance().getDeviceData();
		String fileName = new Formatter().format(
				Messages.Format_rawdata_file_yyyy_mm_dd + fImportDevice.fileExtension,
				deviceData.transferYear,
				deviceData.transferMonth,
				deviceData.transferDay).toString();

		String importPathName = fPageImportSettings.fAutoSavePathEditor.getStringValue();

		String fileOutPath = new Path(importPathName).addTrailingSeparator().toString() + fileName;
		File fileOut = new File(fileOutPath);

		// check if file already exist, ask for overwriting the file
		if (fileOut.exists()) {

			MessageBox msgBox = new MessageBox(getShell(), SWT.ICON_WORKING | SWT.OK | SWT.CANCEL);

			msgBox.setMessage(NLS.bind(
					Messages.ImportWizard_Message_replace_existing_file,
					fileName));

			if (msgBox.open() != SWT.OK) {
				return null;
			}
		}

		// get source file
		File fileIn = new File(sourceFileName);

		// copy source file into destination file
		FileInputStream inReader = null;
		FileOutputStream outReader = null;
		try {
			inReader = new FileInputStream(fileIn);
			outReader = new FileOutputStream(fileOut);
			int c;

			while ((c = inReader.read()) != -1)
				outReader.write(c);

			inReader.close();
			outReader.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// close the files
			if (inReader != null) {
				try {
					inReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (outReader != null) {
				try {
					outReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return fileOutPath;
	}

	public boolean getCloseStatus() {
		return fCloseDialog;
	}

	/**
	 * @return Returns <code>true</code> when the import was successful
	 */
	private boolean receiveData() {

		Combo comboPorts = fPageImportSettings.fComboPorts;

		if (comboPorts.isDisposed()) {
			return false;
		}

		/*
		 * get port name
		 */
		int selectedComPort = comboPorts.getSelectionIndex();
		if (selectedComPort == -1) {
			return false;
		}

		final String portName = comboPorts.getItem(selectedComPort);

		/*
		 * when the Cancel button is pressed multiple times, the app calls this function each time
		 */
		if (fRunnableReceiveData != null) {
			return false;
		}
		/*
		 * set the device which is used to read the data
		 */
		fImportDevice = fPageImportSettings.getSelectedDevice();

		if (fImportDevice == null) {
			return false;
		}

		/*
		 * receive data from the device
		 */
		try {
			fRunnableReceiveData = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {

					String msg = NLS.bind(Messages.ImportWizard_Monitor_task_msg, new Object[] {
							fImportDevice.visibleName,
							portName,
							fImportDevice.getPortParameters(portName).getBaudRate() });

					monitor.beginTask(msg, fImportDevice.getImportDataSize());

					readDeviceData(monitor, portName);
				}
			};

			getContainer().run(true, true, fRunnableReceiveData);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (fCloseDialog) {
			return true;
		}

		if (fRawDataBuffer.size() == 0) {
			// data has not been received or the user canceled the import
			return true;
		}

		/*
		 * data has been received, sav the data in a temp file
		 */

		final String tempDataFileName = RawDataManager.getTempDataFileName();

		// write received data into a temp file
		FileOutputStream fileStream;
		try {
			fileStream = new FileOutputStream(tempDataFileName);
			fileStream.write(fRawDataBuffer.toByteArray());
			fileStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (fImportDevice.validateRawData(tempDataFileName)) {

			// imported data format is valid

			RawDataManager rawDataManager = RawDataManager.getInstance();

			/*
			 * convert the data from the device data into the internal data structure
			 */
			if (fImportDevice.processDeviceData(
					tempDataFileName,
					rawDataManager.getDeviceData(),
					rawDataManager.getTourData())) {

				String autoSavedFileName = null;

				// auto save raw data
				if (fPageImportSettings.fCheckAutoSave.getSelection()) {
					autoSavedFileName = autoSaveRawData(tempDataFileName);
				}

				rawDataManager.setDevice(fImportDevice);

				if (autoSavedFileName == null) {
					// it was not auto saved or the auto save was canceled
					rawDataManager.setReceiveDataFileName(tempDataFileName);
					rawDataManager.setIsDeviceImport(true);
				} else {
					/*
					 * tell the raw data manager that the data are not received they are now from a
					 * file
					 */
					rawDataManager.setReceiveDataFileName(autoSavedFileName);
					rawDataManager.setIsDeviceImport(false);
				}

				rawDataManager.updatePersonInRawData();

				// show imported data in the raw data view
				try {
					RawDataView importView = (RawDataView) PlatformUI
							.getWorkbench()
							.getActiveWorkbenchWindow()
							.getActivePage()
							.showView(RawDataView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);

					if (importView != null) {
						importView.updateViewer();
						importView.setActionSaveEnabled(autoSavedFileName == null);
					}
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}

		} else {

			// data format is invalid

			MessageBox msgBox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);

			msgBox.setMessage(NLS.bind(
					Messages.ImportWizard_Error_invalid_data_format,
					fImportDevice.visibleName));
			msgBox.open();

			return true;
		}

		return true;
	}

	public boolean performFinish() {

		receiveData();

		fPageImportSettings.persistDialogSettings();

		return true;
	}

	/**
	 * Reads data from the device and save it in a buffer
	 * 
	 * @param monitor
	 */
	private void readDeviceData(IProgressMonitor monitor, String portName) {

		// truncate databuffer
		fRawDataBuffer.reset();

		int receiveTimeout = 0;
		int receiveTimer = 0;
		int receivedData = 0;
		boolean isReceivingStarted = false;

		fCloseDialog = false;

		int importDataSize = fImportDevice.getImportDataSize();
		int timer = 0;

		// start the port thread which reads data from the com port
		PortThread portThreadRunnable = new PortThread(this, fImportDevice, portName);

		Thread portThread = new Thread(
				portThreadRunnable,
				Messages.ImportWizard_Thread_name_read_device_data);
		portThread.start();

		/*
		 * wait for the data thread, terminate after a certain time (600 x 100 milliseconds = 60
		 * seconds)
		 */

		while (receiveTimeout < RECEIVE_TIMEOUT) {

			try {
				Thread.sleep(100);
			} catch (InterruptedException e2) {
				e2.printStackTrace();
			}

			if (isReceivingStarted == false) {
				monitor.subTask(NLS.bind(
						Messages.ImportWizard_Monitor_wait_for_data,
						(RECEIVE_TIMEOUT / 10 - (receiveTimeout / 10))));
			}

			int rawDataSize = fRawDataBuffer.size();

			/*
			 * if receiving data was started and no more data are coming in, stop receiving
			 * additional data
			 */
			if (isReceivingStarted && receiveTimer == 10 & rawDataSize == receivedData) {
				break;
			}

			// if user pressed the cancel button, exit the import
			if (monitor.isCanceled() || fCloseDialog == true) {

				// close the dialog when the monitor was canceled
				fCloseDialog = true;

				// reset databuffer to prevent saving the content
				fRawDataBuffer.reset();
				break;
			}

			// check if new data are arrived
			if (rawDataSize > receivedData) {

				// set status that receiving of the data has been started
				isReceivingStarted = true;
				timer++;

				// reset timeout if data are being received
				receiveTimeout = 0;
				receiveTimer = 0;

				// advance progress monitor
				monitor.worked(rawDataSize - receivedData);

				// display the bytes which have been received
				monitor.subTask(NLS.bind(
						Messages.ImportWizard_Monitor_task_received_bytes,
						new Object[] {
								Integer.toString(receivedData * 100 / importDataSize),
								Integer.toString(timer / 10),
								Integer.toString(receivedData) }));
			}

			receiveTimeout++;
			receiveTimer++;

			receivedData = rawDataSize;
		}

		// tell the port listener thread to stop
		monitor.subTask(Messages.ImportWizard_Monitor_stop_port);
		portThreadRunnable.prepareInterrupt();

		portThread.interrupt();

		// wait for the port thread to be terminated
		try {
			portThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void setAutoDownload() {

		getContainer().getShell().addShellListener(new ShellAdapter() {
			public void shellActivated(ShellEvent e) {

				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {

						// start downloading
						boolean importResult = receiveData();

						fPageImportSettings.persistDialogSettings();

						if (importResult) {
							getContainer().getShell().close();
						}
					}
				});
			}
		});

	}

	public void setCloseDialog() {
		fCloseDialog = true;
	}

	public void setDialogSettings() {

		IDialogSettings pluginSettings = TourbookPlugin.getDefault().getDialogSettings();
		IDialogSettings wizardSettings = pluginSettings.getSection(DIALOG_SETTINGS_SECTION);

		if (wizardSettings == null) {
			wizardSettings = pluginSettings.addNewSection(DIALOG_SETTINGS_SECTION);
		}

		super.setDialogSettings(wizardSettings);
	}

}
