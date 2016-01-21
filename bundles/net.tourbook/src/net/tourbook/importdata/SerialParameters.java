/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import gnu.io.SerialPort;
import net.tourbook.ui.UI;

/**
 * A class that stores parameters for serial ports.
 */
public class SerialParameters {

	private String	_portName;

	private int		_baudRate;
	private int		_flowControlIn;
	private int		_flowControlOut;
	private int		_databits;
	private int		_stopbits;
	private int		_parity;

	/**
	 * Default constructer. Sets parameters to no port, 9600 baud, no flow control, 8 data bits, 1
	 * stop bit, no parity.
	 */
	public SerialParameters() {
		this(
				UI.EMPTY_STRING,
				9600,
				SerialPort.FLOWCONTROL_NONE,
				SerialPort.FLOWCONTROL_NONE,
				SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE);

	}

	/**
	 * Paramaterized constructer.
	 * 
	 * @param portName
	 *            The name of the port.
	 * @param baudRate
	 *            The baud rate.
	 * @param flowControlIn
	 *            Type of flow control for receiving.
	 * @param flowControlOut
	 *            Type of flow control for sending.
	 * @param databits
	 *            The number of data bits.
	 * @param stopbits
	 *            The number of stop bits.
	 * @param parity
	 *            The type of parity.
	 */
	public SerialParameters(final String portName,
							final int baudRate,
							final int flowControlIn,
							final int flowControlOut,
							final int databits,
							final int stopbits,
							final int parity) {

		_portName = portName;
		_baudRate = baudRate;
		_flowControlIn = flowControlIn;
		_flowControlOut = flowControlOut;
		_databits = databits;
		_stopbits = stopbits;
		_parity = parity;
	}

	/**
	 * Converts an <code>int</code> describing a flow control type to a <code>String</code>
	 * describing a flow control type.
	 * 
	 * @param flowControl
	 *            An <code>int</code> describing a flow control type.
	 * @return A <code>String</code> describing a flow control type.
	 */
	String flowToString(final int flowControl) {
		switch (flowControl) {
		case SerialPort.FLOWCONTROL_NONE:
			return "None"; //$NON-NLS-1$
		case SerialPort.FLOWCONTROL_XONXOFF_OUT:
			return "Xon/Xoff Out"; //$NON-NLS-1$
		case SerialPort.FLOWCONTROL_XONXOFF_IN:
			return "Xon/Xoff In"; //$NON-NLS-1$
		case SerialPort.FLOWCONTROL_RTSCTS_IN:
			return "RTS/CTS In"; //$NON-NLS-1$
		case SerialPort.FLOWCONTROL_RTSCTS_OUT:
			return "RTS/CTS Out"; //$NON-NLS-1$
		default:
			return "None"; //$NON-NLS-1$
		}
	}

	/**
	 * Gets baud rate as an <code>int</code>.
	 * 
	 * @return Current baud rate.
	 */
	public int getBaudRate() {
		return _baudRate;
	}

	/**
	 * Gets baud rate as a <code>String</code>.
	 * 
	 * @return Current baud rate.
	 */
	public String getBaudRateString() {
		return Integer.toString(_baudRate);
	}

	/**
	 * Gets data bits as an <code>int</code>.
	 * 
	 * @return Current data bits setting.
	 */
	public int getDatabits() {
		return _databits;
	}

	/**
	 * Gets data bits as a <code>String</code>.
	 * 
	 * @return Current data bits setting.
	 */
	public String getDatabitsString() {
		switch (_databits) {
		case SerialPort.DATABITS_5:
			return "5"; //$NON-NLS-1$
		case SerialPort.DATABITS_6:
			return "6"; //$NON-NLS-1$
		case SerialPort.DATABITS_7:
			return "7"; //$NON-NLS-1$
		case SerialPort.DATABITS_8:
			return "8"; //$NON-NLS-1$
		default:
			return "8"; //$NON-NLS-1$
		}
	}

	/**
	 * Gets flow control for reading as an <code>int</code>.
	 * 
	 * @return Current flow control type.
	 */
	public int getFlowControlIn() {
		return _flowControlIn;
	}

	/**
	 * Gets flow control for reading as a <code>String</code>.
	 * 
	 * @return Current flow control type.
	 */
	public String getFlowControlInString() {
		return flowToString(_flowControlIn);
	}

	/**
	 * Gets flow control for writing as an <code>int</code>.
	 * 
	 * @return Current flow control type.
	 */
	public int getFlowControlOut() {
		return _flowControlOut;
	}

	/**
	 * Gets flow control for writing as a <code>String</code>.
	 * 
	 * @return Current flow control type.
	 */
	public String getFlowControlOutString() {
		return flowToString(_flowControlOut);
	}

	/**
	 * Gets parity setting as an <code>int</code>.
	 * 
	 * @return Current parity setting.
	 */
	public int getParity() {
		return _parity;
	}

	/**
	 * Gets parity setting as a <code>String</code>.
	 * 
	 * @return Current parity setting.
	 */
	public String getParityString() {
		switch (_parity) {
		case SerialPort.PARITY_NONE:
			return "None"; //$NON-NLS-1$
		case SerialPort.PARITY_EVEN:
			return "Even"; //$NON-NLS-1$
		case SerialPort.PARITY_ODD:
			return "Odd"; //$NON-NLS-1$
		default:
			return "None"; //$NON-NLS-1$
		}
	}

	/**
	 * Gets port name.
	 * 
	 * @return Current port name.
	 */
	public String getPortName() {
		return _portName;
	}

	/**
	 * Gets stop bits setting as an <code>int</code>.
	 * 
	 * @return Current stop bits setting.
	 */
	public int getStopbits() {
		return _stopbits;
	}

	/**
	 * Gets stop bits setting as a <code>String</code>.
	 * 
	 * @return Current stop bits setting.
	 */
	public String getStopbitsString() {
		switch (_stopbits) {
		case SerialPort.STOPBITS_1:
			return "1"; //$NON-NLS-1$
		case SerialPort.STOPBITS_1_5:
			return "1.5"; //$NON-NLS-1$
		case SerialPort.STOPBITS_2:
			return "2"; //$NON-NLS-1$
		default:
			return "1"; //$NON-NLS-1$
		}
	}

	/**
	 * Sets baud rate.
	 * 
	 * @param baudRate
	 *            New baud rate.
	 */
	public void setBaudRate(final int baudRate) {
		this._baudRate = baudRate;
	}

	/**
	 * Sets baud rate.
	 * 
	 * @param baudRate
	 *            New baud rate.
	 */
	public void setBaudRate(final String baudRate) {
		this._baudRate = Integer.parseInt(baudRate);
	}

	/**
	 * Sets data bits.
	 * 
	 * @param databits
	 *            New data bits setting.
	 */
	public void setDatabits(final int databits) {
		this._databits = databits;
	}

	/**
	 * Sets data bits.
	 * 
	 * @param databits
	 *            New data bits setting.
	 */
	public void setDatabits(final String databits) {
		if (databits.equals("5")) { //$NON-NLS-1$
			this._databits = SerialPort.DATABITS_5;
		}
		if (databits.equals("6")) { //$NON-NLS-1$
			this._databits = SerialPort.DATABITS_6;
		}
		if (databits.equals("7")) { //$NON-NLS-1$
			this._databits = SerialPort.DATABITS_7;
		}
		if (databits.equals("8")) { //$NON-NLS-1$
			this._databits = SerialPort.DATABITS_8;
		}
	}

	/**
	 * Sets flow control for reading.
	 * 
	 * @param flowControlIn
	 *            New flow control for reading type.
	 */
	public void setFlowControlIn(final int flowControlIn) {
		this._flowControlIn = flowControlIn;
	}

	/**
	 * Sets flow control for reading.
	 * 
	 * @param flowControlIn
	 *            New flow control for reading type.
	 */
	public void setFlowControlIn(final String flowControlIn) {
		this._flowControlIn = stringToFlow(flowControlIn);
	}

	/**
	 * Sets flow control for writing.
	 * 
	 * @param flowControlIn
	 *            New flow control for writing type.
	 */
	public void setFlowControlOut(final int flowControlOut) {
		this._flowControlOut = flowControlOut;
	}

	/**
	 * Sets flow control for writing.
	 * 
	 * @param flowControlIn
	 *            New flow control for writing type.
	 */
	public void setFlowControlOut(final String flowControlOut) {
		this._flowControlOut = stringToFlow(flowControlOut);
	}

	/**
	 * Sets parity setting.
	 * 
	 * @param parity
	 *            New parity setting.
	 */
	public void setParity(final int parity) {
		this._parity = parity;
	}

	/**
	 * Sets parity setting.
	 * 
	 * @param parity
	 *            New parity setting.
	 */
	public void setParity(final String parity) {
		if (parity.equals("None")) { //$NON-NLS-1$
			this._parity = SerialPort.PARITY_NONE;
		}
		if (parity.equals("Even")) { //$NON-NLS-1$
			this._parity = SerialPort.PARITY_EVEN;
		}
		if (parity.equals("Odd")) { //$NON-NLS-1$
			this._parity = SerialPort.PARITY_ODD;
		}
	}

	/**
	 * Sets port name.
	 * 
	 * @param portName
	 *            New port name.
	 */
	public void setPortName(final String portName) {
		this._portName = portName;
	}

	/**
	 * Sets stop bits.
	 * 
	 * @param stopbits
	 *            New stop bits setting.
	 */
	public void setStopbits(final int stopbits) {
		this._stopbits = stopbits;
	}

	/**
	 * Sets stop bits.
	 * 
	 * @param stopbits
	 *            New stop bits setting.
	 */
	public void setStopbits(final String stopbits) {
		if (stopbits.equals("1")) { //$NON-NLS-1$
			this._stopbits = SerialPort.STOPBITS_1;
		}
		if (stopbits.equals("1.5")) { //$NON-NLS-1$
			this._stopbits = SerialPort.STOPBITS_1_5;
		}
		if (stopbits.equals("2")) { //$NON-NLS-1$
			this._stopbits = SerialPort.STOPBITS_2;
		}
	}

	/**
	 * Converts a <code>String</code> describing a flow control type to an <code>int</code> type
	 * defined in <code>SerialPort</code>.
	 * 
	 * @param flowControl
	 *            A <code>string</code> describing a flow control type.
	 * @return An <code>int</code> describing a flow control type.
	 */
	private int stringToFlow(final String flowControl) {
		if (flowControl.equals("None")) { //$NON-NLS-1$
			return SerialPort.FLOWCONTROL_NONE;
		}
		if (flowControl.equals("Xon/Xoff Out")) { //$NON-NLS-1$
			return SerialPort.FLOWCONTROL_XONXOFF_OUT;
		}
		if (flowControl.equals("Xon/Xoff In")) { //$NON-NLS-1$
			return SerialPort.FLOWCONTROL_XONXOFF_IN;
		}
		if (flowControl.equals("RTS/CTS In")) { //$NON-NLS-1$
			return SerialPort.FLOWCONTROL_RTSCTS_IN;
		}
		if (flowControl.equals("RTS/CTS Out")) { //$NON-NLS-1$
			return SerialPort.FLOWCONTROL_RTSCTS_OUT;
		}
		return SerialPort.FLOWCONTROL_NONE;
	}
}
