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

package net.tourbook.device;

import net.tourbook.importdata.IDataListener;
import net.tourbook.importdata.SerialParameters;

public class PortThread implements Runnable, IDataListener {

	private SimpleSerialDevice	fImportDevice;
	private String				fPortName;

	private boolean				fIsDataReceived;
	private int					fByteIndex;
	private PortListener		fPortListener;

	public PortThread(SimpleSerialDevice importDevice, String portName) {
		fPortName = portName;
		fImportDevice = importDevice;
	}

	public void run() {

		// open connection
		try {

			SerialParameters portParameters = fImportDevice.getTourbookDevice().getPortParameters(fPortName);

			if (portParameters == null) {
				return;
			}

			fPortListener = new PortListener(portParameters, this);

			fIsDataReceived = false;
			fByteIndex = 0;

			/*
			 * open the port and wait until data are received, when new are available then the
			 * method dataArrived will be called
			 */
			fPortListener.openConnection();

		} catch (SerialConnectionException e) {
			e.printStackTrace();
		}

		try {
			while (true) {

				if (fPortListener == null) {
					break;
				}

				// send data when no data has been received yet
				if (fIsDataReceived == false) {
					if (fPortListener.sendData(0xD8) == false) {
						fImportDevice.cancelImport();
						break;
					}
				}

				// sleep until this thread gets interrupted
				Thread.sleep(500);
			}

		} catch (InterruptedException e2) {

			// e2.printStackTrace();

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.tourbook.rxtx.IDataListener#dataArrived(java.lang.StringBuilder)
	 */
	public void dataArrived(int newByte) {

		boolean appendData = true;

		if (fIsDataReceived == false) {

			/*
			 * check if the start sequence is correct for this device
			 */
			if (fImportDevice.getTourbookDevice().checkStartSequence(fByteIndex, newByte)) {
				fByteIndex++;

				/*
				 * when the start sequence is correct then the bytes will not be checked again
				 * because these data will change
				 */
				if (fByteIndex >= fImportDevice.getTourbookDevice().getStartSequenceSize()) {
					fIsDataReceived = true;
				}
			} else {
				// don't append wrong data
				appendData = false;
			}
		}

		if (appendData) {
			// forward the received data
			fImportDevice.appendReceivedData(newByte);
		}
	}

	public void prepareInterrupt() {
		// cleanup resources
		if (fPortListener != null) {
			fPortListener.closeConnection();
			fPortListener = null;
		}
	}
}
