/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.dataImport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Formatter;

import net.tourbook.Messages;
import net.tourbook.device.DeviceData;
import net.tourbook.device.TourbookDevice;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.views.rawData.RawDataView;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class WizardImportData extends Wizard {

	public static final String			DIALOG_SETTINGS_SECTION	= "WizardImportData";			//$NON-NLS-1$

	private WizardPageImportSettings	fPageImportSettings;

	private ByteArrayOutputStream		fRawDataBuffer			= new ByteArrayOutputStream();

	/**
	 * contains the device which is used to read the data from it
	 */
	private TourbookDevice				fImportDevice;

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
	 * @param data
	 *        data being written into the buffer
	 */
	public void appendReceivedData(byte[] data) {
		try {
			fRawDataBuffer.write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void importData() {

		final String portName = fPageImportSettings.fComboPorts
				.getItem(fPageImportSettings.fComboPorts.getSelectionIndex());

		fImportDevice = fPageImportSettings.getSelectedDevice();

		if (fImportDevice == null) {
			return;
		}

		// create/run the import task with a monitor
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {

					// NLS.bind(Messages.key_two, "example usage");

					String msg = NLS.bind(
							Messages.ImportWizard_Monitor_task_msg,
							fImportDevice.visibleName,
							portName);

					monitor.beginTask(msg, fImportDevice.getImportDataSize());

					readDeviceData(monitor, portName);
				}
			});
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (fRawDataBuffer.size() == 0) {
			// data has not been received or the user canceled the import
			return;
		}

		// data has been received

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
			 * convert the data from the device data into the internal data
			 * structure
			 */
			if (fImportDevice.processDeviceData(
					tempDataFileName,
					rawDataManager.getDeviceData(),
					rawDataManager.getTourData())) {

				rawDataManager.setDevice(fImportDevice);
				rawDataManager.setImportFileName(tempDataFileName);
				rawDataManager.setIsDeviceImport();

				// auto save raw data
				if (fPageImportSettings.fCheckAutoSave.getSelection()) {
					autoSaveRawData();
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
						importView.setActionSaveEnabled(true);
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

			return;
		}
	}

	/**
	 * 
	 */
	public void autoSaveRawData() {

		// set filename to the transfer date
		DeviceData deviceData = RawDataManager.getInstance().getDeviceData();
		String fileName = new Formatter()
				.format(
						Messages.ImportWizard_Format_import_filename_yyyymmdd
								+ fImportDevice.fileExtension,
						deviceData.transferYear,
						deviceData.transferMonth,
						deviceData.transferDay)
				.toString();

		String importPathName = fPageImportSettings.fAutoSavePathEditor.getStringValue();

		File fileIn;
		File fileOut = new File(new Path(importPathName).addTrailingSeparator().toString()
				+ fileName);

		// check if file already exist, ask for overwriting the file
		if (fileOut.exists()) {

			MessageBox msgBox = new MessageBox(getShell(), SWT.ICON_WORKING | SWT.OK | SWT.CANCEL);

			msgBox.setMessage(NLS.bind(
					Messages.ImportWizard_Message_replace_existing_file,
					fileName));

			if (msgBox.open() != SWT.OK) {
				return;
			}
		}

		// get source file
		fileIn = new File(fImportDevice.getImportFileName());

		// copy source file into destination file
		FileReader inReader = null;
		FileWriter outReader = null;
		try {
			inReader = new FileReader(fileIn);
			outReader = new FileWriter(fileOut);
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
	}

	public boolean performFinish() {

		fPageImportSettings.persistDialogSettings();

		importData();

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

		int iTimeout = 0;
		int iReceivedData = 0;
		boolean isReceiving = false;
		int importDataSize = fImportDevice.getImportDataSize();

		// start thread which reads data from the com port
		Thread portThread = new Thread(
				new PortThread(this, portName),
				Messages.ImportWizard_Thread_name_read_device_data);
		portThread.start();

		/*
		 * wait for the data thread, terminate after a certain time (300 x 100
		 * milliseconds = 30 seconds)
		 */
		while (iTimeout < 300) {

			try {
				Thread.sleep(100);
			} catch (InterruptedException e2) {
				e2.printStackTrace();
			}

			// if receiving has started and no more data are coming in
			// stop receiving additional data
			if (isReceiving && fRawDataBuffer.size() == iReceivedData) {
				break;
			}

			// if user pressed the cancel button, exit the import
			if (monitor.isCanceled()) {

				// reset databuffer to prevent saving the content
				fRawDataBuffer.reset();
				break;
			}

			// check if receiving of data has been started
			if (fRawDataBuffer.size() > iReceivedData) {

				// set status that receiving of the data has been started
				isReceiving = true;

				// reset timeout if data are being received
				iTimeout = 0;

				// advance progress monitor
				monitor.worked(fRawDataBuffer.size() - iReceivedData);

				// display the bytes which have been received
				monitor.subTask(NLS.bind(
						Messages.ImportWizard_Monitor_task_received_bytes,
						(Integer.toString(iReceivedData * 100 / importDataSize)),
						(Integer.toString(iReceivedData))));
			}

			iTimeout++;

			iReceivedData = fRawDataBuffer.size();
		}

		// tell the thread to stop
		portThread.interrupt();

		// wait for the port thread to be terminated
		try {
			portThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
