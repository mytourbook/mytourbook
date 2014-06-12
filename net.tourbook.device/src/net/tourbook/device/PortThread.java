/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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

import net.tourbook.common.util.StatusUtil;
import net.tourbook.importdata.IDataListener;
import net.tourbook.importdata.SerialParameters;

public class PortThread implements Runnable, IDataListener {

	private SimpleSerialDevice	_importDevice;
	private String				_portName;

	private boolean				_isDataReceived;
	private int					_byteIndex;
	private PortListener		_portListener;

	public PortThread(final SimpleSerialDevice importDevice, final String portName) {

		_portName = portName;
		_importDevice = importDevice;
	}

	/*
	 * (non-Javadoc)
	 * @see net.tourbook.rxtx.IDataListener#dataArrived(java.lang.StringBuilder)
	 */
	public void dataArrived(final int newByte) {

		boolean appendData = true;

		if (_isDataReceived == false) {

			/*
			 * check if the start sequence is correct for this device
			 */
			if (_importDevice.getTourbookDevice().checkStartSequence(_byteIndex, newByte)) {
				_byteIndex++;

				/*
				 * when the start sequence is correct then the bytes will not be checked again
				 * because these data will change
				 */
				if (_byteIndex >= _importDevice.getTourbookDevice().getStartSequenceSize()) {
					_isDataReceived = true;
				}
			} else {
				// don't append wrong data
				appendData = false;
			}
		}

		if (appendData) {
			// forward the received data
			_importDevice.appendReceivedData(newByte);
		}
	}

	public void prepareInterrupt() {

		// cleanup resources
		if (_portListener != null) {
			_portListener.closeConnection();
			_portListener = null;
		}
	}

	public void run() {

		// open connection
		try {

			final SerialParameters portParameters = _importDevice.getTourbookDevice().getPortParameters(_portName);

			if (portParameters == null) {
				return;
			}

			_portListener = new PortListener(portParameters, this);

			_isDataReceived = false;
			_byteIndex = 0;

			/*
			 * open the port and wait until data are received, when new are available then the
			 * method dataArrived will be called
			 */
			_portListener.openConnection();

		} catch (final SerialConnectionException e) {

			StatusUtil.showStatus(e.getMessage(), e);
		}

		try {
			while (true) {

				if (_portListener == null) {
					break;
				}

				// send data when no data has been received yet
				if (_isDataReceived == false) {
					if (_portListener.sendData(0xD8) == false) {
						_importDevice.cancelImport();
						break;
					}
				}

				// sleep until this thread gets interrupted
				Thread.sleep(500);
			}

		} catch (final InterruptedException e2) {

			// e2.printStackTrace();

		}
	}
}
