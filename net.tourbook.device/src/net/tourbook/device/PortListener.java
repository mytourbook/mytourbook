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

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

import net.tourbook.importdata.IDataListener;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.ui.UI;

public class PortListener implements SerialPortEventListener {

	private SerialParameters	fPortParams;
	private SerialPort			fSerialPort;

	private InputStream			fInStream;
	private OutputStream		fOutStream;

	private boolean				fIsPortOpen;

	private IDataListener		fDataListener;

	/**
	 * Class Constructor
	 *
	 * @param portParams
	 * @param dataNotification
	 */
	public PortListener(SerialParameters portParams, IDataListener dataListener) {

		fPortParams = portParams;
		fDataListener = dataListener;

		fIsPortOpen = false;
	}

	/**
	 * Attempts to open a serial connection and streams using the parameters in the SerialParameters
	 * object. If it is unsuccesfull at any step it returns the port to a closed state, throws a
	 * <code>SerialConnectionException</code>, and returns. Gives a timeout of 5 seconds on the
	 * portOpen to allow other applications to reliquish the port if have it open and no longer need
	 * it.
	 *
	 * @throws SerialConnectionException
	 */
	public void openConnection() throws SerialConnectionException {

		// Enumeration ports = CommPortIdentifier.getPortIdentifiers();

		// dump available ports
		// while (ports.hasMoreElements()) {
		// CommPortIdentifier port = (CommPortIdentifier) ports.nextElement();
		// System.out.println(port.getPortType() +" - "+port.getName());
		// }

		CommPortIdentifier portId;

		// Obtain a CommPortIdentifier object for the port you want to open.
		try {
			portId = CommPortIdentifier.getPortIdentifier(fPortParams.getPortName());
		} catch (NoSuchPortException e) {
			throw new SerialConnectionException(e.getMessage());
		}

		// Open the port represented by the CommPortIdentifier object. Give
		// the open call a relatively long timeout of 30 seconds to allow
		// a different application to reliquish the port if the user
		// wants to.
		try {
			fSerialPort = (SerialPort) portId.open("net.tourbook.data.serial", 5000); //$NON-NLS-1$
		} catch (PortInUseException e) {

			UI.showMessageInfo(Messages.Port_Listener_Error_ntd001, Messages.Port_Listener_Error_ntd002);

			throw new SerialConnectionException(e.getMessage());
		}

		// Set the parameters of the connection. If they won't set, close the
		// port before throwing an exception.
		try {
			setConnectionParameters();
		} catch (SerialConnectionException e) {
			fSerialPort.close();
			throw e;
		}

		// Open the input and output streams for the connection. If they won't
		// open, close the port before throwing an exception.
		try {
			fInStream = fSerialPort.getInputStream();
			fOutStream = fSerialPort.getOutputStream();
			// fOutStreamIsOpen=true;
		} catch (IOException e) {
			fSerialPort.close();
			throw new SerialConnectionException("Error opening i/o streams"); //$NON-NLS-1$
		}

		// Add this object as an event listener for the serial port.
		try {
			fSerialPort.addEventListener(this);
		} catch (TooManyListenersException e) {
			fSerialPort.close();
			throw new SerialConnectionException("too many selectionListeners added"); //$NON-NLS-1$
		}

		// Set notifyOnDataAvailable to true to allow event driven input.
		fSerialPort.notifyOnDataAvailable(true);

		// Set receive timeout to allow breaking out of polling loop during
		// input handling.
		try {
			fSerialPort.enableReceiveTimeout(10);
		} catch (UnsupportedCommOperationException e) {}

		fIsPortOpen = true;
	}

	/**
	 * Close the port and clean up associated elements.
	 */
	public void closeConnection() {

		// If port is alread closed just return.
		if (fIsPortOpen == false) {
			return;
		}

		// Check to make sure sPort has reference to avoid a NPE.
		if (fSerialPort != null) {

			// System.out.println("close port");

			// cleanup the port
			fSerialPort.notifyOnDataAvailable(false);
			fSerialPort.removeEventListener();
			// fSerialPort.disableReceiveTimeout();
			// fSerialPort.

			try {
				// close the i/o streams
				fInStream.close();
				fOutStream.close();
			} catch (IOException e) {
				System.err.println(e);
			}

			// Close the port.
			fSerialPort.close();
		}

		fSerialPort = null;
		fIsPortOpen = false;
	}

	/**
	 * Sets the connection parameters to the setting in the parameters object. If set fails return
	 * the parameters object to origional settings and throw exception.
	 */
	public void setConnectionParameters() throws SerialConnectionException {

		// Save state of parameters before trying a set.
		int oldBaudRate = fSerialPort.getBaudRate();
		int oldDatabits = fSerialPort.getDataBits();
		int oldStopbits = fSerialPort.getStopBits();
		int oldParity = fSerialPort.getParity();
		// Set connection parameters, if set fails return parameters object
		// to original state.
		try {
			fSerialPort.setSerialPortParams(
					fPortParams.getBaudRate(),
					fPortParams.getDatabits(),
					fPortParams.getStopbits(),
					fPortParams.getParity());
		} catch (UnsupportedCommOperationException e) {
			fPortParams.setBaudRate(oldBaudRate);
			fPortParams.setDatabits(oldDatabits);
			fPortParams.setStopbits(oldStopbits);
			fPortParams.setParity(oldParity);
			throw new SerialConnectionException("Unsupported parameter"); //$NON-NLS-1$
		}

		// Set flow control.
		try {
			fSerialPort.setFlowControlMode(fPortParams.getFlowControlIn() | fPortParams.getFlowControlOut());
		} catch (UnsupportedCommOperationException e) {
			throw new SerialConnectionException("Unsupported flow control"); //$NON-NLS-1$
		}
	}

	/*
	 * @see javax.comm.SerialPortEventListener#serialEvent(javax.comm.SerialPortEvent)
	 */
	public void serialEvent(SerialPortEvent portEvent) {

		// Determine type of event
		switch (portEvent.getEventType()) {

		case SerialPortEvent.DATA_AVAILABLE:

			int iData = 0;

			// Read data until -1 is returned
			while (iData != -1) {
				try {

					iData = fInStream.read();

					if (iData != -1) {
						fDataListener.dataArrived(iData & 0xff);
					}

				} catch (IOException ex) {
					System.out.println(ex);
					return;
				}
			}

			break;
		}
	}

	/**
	 * send data from the port
	 *
	 * @param data
	 * @return Returns <code>false</code> when an error occured
	 */
	public boolean sendData(int data) {
		try {
			if (fOutStream == null) {
				return false;
			} else {
				fOutStream.write(data);
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
