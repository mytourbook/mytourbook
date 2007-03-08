/* @(#)SerialConnection.java	1.6 98/07/17 SMI
 *
 * Copyright (c) 1998 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license
 * to use, modify and redistribute this software in source and binary
 * code form, provided that i) this copyright notice and license appear
 * on all copies of the software; and ii) Licensee does not utilize the
 * software in a manner which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind.
 * ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND
 * ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY
 * LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THE
 * SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS
 * BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES,
 * HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING
 * OUT OF THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control
 * of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.
 */

package net.tourbook.importdata;

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
	 * Attempts to open a serial connection and streams using the parameters in
	 * the SerialParameters object. If it is unsuccesfull at any step it returns
	 * the port to a closed state, throws a
	 * <code>SerialConnectionException</code>, and returns. Gives a timeout
	 * of 5 seconds on the portOpen to allow other applications to reliquish the
	 * port if have it open and no longer need it.
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
			fSerialPort = (SerialPort) portId.open("net.tourbook.data.serial", 5000);
		} catch (PortInUseException e) {
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
			throw new SerialConnectionException("Error opening i/o streams");
		}

		// Add this object as an event listener for the serial port.
		try {
			fSerialPort.addEventListener(this);
		} catch (TooManyListenersException e) {
			fSerialPort.close();
			throw new SerialConnectionException("too many selectionListeners added");
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
	 * Sets the connection parameters to the setting in the parameters object.
	 * If set fails return the parameters object to origional settings and throw
	 * exception.
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
			throw new SerialConnectionException("Unsupported parameter");
		}

		// Set flow control.
		try {
			fSerialPort.setFlowControlMode(fPortParams.getFlowControlIn()
					| fPortParams.getFlowControlOut());
		} catch (UnsupportedCommOperationException e) {
			throw new SerialConnectionException("Unsupported flow control");
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
						fDataListener.dataArrived(iData);
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
