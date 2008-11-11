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
package net.tourbook.device;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.importdata.ExternalDevice;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;

public abstract class SimpleSerialDevice extends ExternalDevice {

	protected TourbookDevice			fTourbookDevice;
	protected boolean					fCancelImport;

	private final ByteArrayOutputStream	fRawDataBuffer	= new ByteArrayOutputStream();
	private List<File>					fReceivedFiles;

	public SimpleSerialDevice() {
		fTourbookDevice = getTourbookDevice();
	}

	/**
	 * Append newly received data to the internal data buffer
	 * 
	 * @param newData
	 *            data being written into the buffer
	 */
	public void appendReceivedData(final int newData) {
		fRawDataBuffer.write(newData);
	}

	public void cancelImport() {
		fCancelImport = true;
	}

	@Override
	public IRunnableWithProgress createImportRunnable(final String portName, final List<File> receivedFiles) {

		fReceivedFiles = receivedFiles;

		return new IRunnableWithProgress() {
			public void run(final IProgressMonitor monitor) {

				final SerialParameters portParameters = fTourbookDevice.getPortParameters(portName);

				if (portParameters == null) {
					return;
				}

				final String msg = NLS.bind(Messages.Import_Wizard_Monitor_task_msg, new Object[] {
						visibleName,
						portName,
						portParameters.getBaudRate() });

				monitor.beginTask(msg, fTourbookDevice.getTransferDataSize());

				readDeviceData(monitor, portName);
				saveReceivedData();
			}

		};
	}

	public abstract String getFileExtension();

	public abstract TourbookDevice getTourbookDevice();

	@Override
	public boolean isImportCanceled() {
		return fCancelImport;
	}

	/**
	 * Reads data from the device and save it in a buffer
	 * 
	 * @param monitor
	 */
	private void readDeviceData(final IProgressMonitor monitor, final String portName) {

		// truncate databuffer
		fRawDataBuffer.reset();

		int receiveTimeout = 0;
		int receiveTimer = 0;
		int receivedData = 0;
		boolean isReceivingStarted = false;

		fCancelImport = false;

		final int importDataSize = fTourbookDevice.getTransferDataSize();
		int timer = 0;

		// start the port thread which reads data from the com port
		final PortThread portThreadRunnable = new PortThread(this, portName);

		final Thread portThread = new Thread(portThreadRunnable, Messages.Import_Wizard_Thread_name_read_device_data);
		portThread.start();

		/*
		 * wait for the data thread, terminate after a certain time (600 x 100 milliseconds = 60
		 * seconds)
		 */

		while (receiveTimeout < RECEIVE_TIMEOUT) {

			try {
				Thread.sleep(100);
			} catch (final InterruptedException e2) {
				e2.printStackTrace();
			}

			if (isReceivingStarted == false) {
				monitor.subTask(NLS.bind(Messages.Import_Wizard_Monitor_wait_for_data,
						(RECEIVE_TIMEOUT / 10 - (receiveTimeout / 10))));
			}

			final int rawDataSize = fRawDataBuffer.size();

			/*
			 * if receiving data was started and no more data are coming in, stop receiving
			 * additional data
			 */
			if (isReceivingStarted && receiveTimer == 10 & rawDataSize == receivedData) {
				break;
			}

			// if user pressed the cancel button, exit the import
			if (monitor.isCanceled() || fCancelImport == true) {

				// close the dialog when the monitor was canceled
				fCancelImport = true;

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
				monitor.subTask(NLS.bind(Messages.Import_Wizard_Monitor_task_received_bytes, new Object[] {
						Integer.toString(receivedData * 100 / importDataSize),
						Integer.toString(timer / 10),
						Integer.toString(receivedData) }));
			}

			receiveTimeout++;
			receiveTimer++;

			receivedData = rawDataSize;
		}

		// tell the port listener thread to stop
		monitor.subTask(Messages.Import_Wizard_Monitor_stop_port);
		portThreadRunnable.prepareInterrupt();

		portThread.interrupt();

		// wait for the port thread to be terminated
		try {
			portThread.join();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * write received data into a temp file
	 */
	private void saveReceivedData() {

		try {
			
			final File tempFile = File.createTempFile("myTourbook", "." + getFileExtension(), //$NON-NLS-1$ //$NON-NLS-2$
					new File(RawDataManager.getTempDir()));

			final FileOutputStream fileStream = new FileOutputStream(tempFile);
			fileStream.write(fRawDataBuffer.toByteArray());
			fileStream.close();

			fReceivedFiles.add(tempFile);

		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

}
