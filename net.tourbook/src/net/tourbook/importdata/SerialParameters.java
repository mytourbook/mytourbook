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

import gnu.io.SerialPort;

/**
 * A class that stores parameters for serial ports.
 */
public class SerialParameters {

	private String	portName;
	private int		baudRate;
	private int		flowControlIn;
	private int		flowControlOut;
	private int		databits;
	private int		stopbits;
	private int		parity;

	/**
	 * Default constructer. Sets parameters to no port, 9600 baud, no flow
	 * control, 8 data bits, 1 stop bit, no parity.
	 */
	public SerialParameters() {
		this(
				"", //$NON-NLS-1$
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
	 *        The name of the port.
	 * @param baudRate
	 *        The baud rate.
	 * @param flowControlIn
	 *        Type of flow control for receiving.
	 * @param flowControlOut
	 *        Type of flow control for sending.
	 * @param databits
	 *        The number of data bits.
	 * @param stopbits
	 *        The number of stop bits.
	 * @param parity
	 *        The type of parity.
	 */
	public SerialParameters(String portName, int baudRate, int flowControlIn, int flowControlOut,
			int databits, int stopbits, int parity) {

		this.portName = portName;
		this.baudRate = baudRate;
		this.flowControlIn = flowControlIn;
		this.flowControlOut = flowControlOut;
		this.databits = databits;
		this.stopbits = stopbits;
		this.parity = parity;
	}

	/**
	 * Sets port name.
	 * 
	 * @param portName
	 *        New port name.
	 */
	public void setPortName(String portName) {
		this.portName = portName;
	}

	/**
	 * Gets port name.
	 * 
	 * @return Current port name.
	 */
	public String getPortName() {
		return portName;
	}

	/**
	 * Sets baud rate.
	 * 
	 * @param baudRate
	 *        New baud rate.
	 */
	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
	}

	/**
	 * Sets baud rate.
	 * 
	 * @param baudRate
	 *        New baud rate.
	 */
	public void setBaudRate(String baudRate) {
		this.baudRate = Integer.parseInt(baudRate);
	}

	/**
	 * Gets baud rate as an <code>int</code>.
	 * 
	 * @return Current baud rate.
	 */
	public int getBaudRate() {
		return baudRate;
	}

	/**
	 * Gets baud rate as a <code>String</code>.
	 * 
	 * @return Current baud rate.
	 */
	public String getBaudRateString() {
		return Integer.toString(baudRate);
	}

	/**
	 * Sets flow control for reading.
	 * 
	 * @param flowControlIn
	 *        New flow control for reading type.
	 */
	public void setFlowControlIn(int flowControlIn) {
		this.flowControlIn = flowControlIn;
	}

	/**
	 * Sets flow control for reading.
	 * 
	 * @param flowControlIn
	 *        New flow control for reading type.
	 */
	public void setFlowControlIn(String flowControlIn) {
		this.flowControlIn = stringToFlow(flowControlIn);
	}

	/**
	 * Gets flow control for reading as an <code>int</code>.
	 * 
	 * @return Current flow control type.
	 */
	public int getFlowControlIn() {
		return flowControlIn;
	}

	/**
	 * Gets flow control for reading as a <code>String</code>.
	 * 
	 * @return Current flow control type.
	 */
	public String getFlowControlInString() {
		return flowToString(flowControlIn);
	}

	/**
	 * Sets flow control for writing.
	 * 
	 * @param flowControlIn
	 *        New flow control for writing type.
	 */
	public void setFlowControlOut(int flowControlOut) {
		this.flowControlOut = flowControlOut;
	}

	/**
	 * Sets flow control for writing.
	 * 
	 * @param flowControlIn
	 *        New flow control for writing type.
	 */
	public void setFlowControlOut(String flowControlOut) {
		this.flowControlOut = stringToFlow(flowControlOut);
	}

	/**
	 * Gets flow control for writing as an <code>int</code>.
	 * 
	 * @return Current flow control type.
	 */
	public int getFlowControlOut() {
		return flowControlOut;
	}

	/**
	 * Gets flow control for writing as a <code>String</code>.
	 * 
	 * @return Current flow control type.
	 */
	public String getFlowControlOutString() {
		return flowToString(flowControlOut);
	}

	/**
	 * Sets data bits.
	 * 
	 * @param databits
	 *        New data bits setting.
	 */
	public void setDatabits(int databits) {
		this.databits = databits;
	}

	/**
	 * Sets data bits.
	 * 
	 * @param databits
	 *        New data bits setting.
	 */
	public void setDatabits(String databits) {
		if (databits.equals("5")) { //$NON-NLS-1$
			this.databits = SerialPort.DATABITS_5;
		}
		if (databits.equals("6")) { //$NON-NLS-1$
			this.databits = SerialPort.DATABITS_6;
		}
		if (databits.equals("7")) { //$NON-NLS-1$
			this.databits = SerialPort.DATABITS_7;
		}
		if (databits.equals("8")) { //$NON-NLS-1$
			this.databits = SerialPort.DATABITS_8;
		}
	}

	/**
	 * Gets data bits as an <code>int</code>.
	 * 
	 * @return Current data bits setting.
	 */
	public int getDatabits() {
		return databits;
	}

	/**
	 * Gets data bits as a <code>String</code>.
	 * 
	 * @return Current data bits setting.
	 */
	public String getDatabitsString() {
		switch (databits) {
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
	 * Sets stop bits.
	 * 
	 * @param stopbits
	 *        New stop bits setting.
	 */
	public void setStopbits(int stopbits) {
		this.stopbits = stopbits;
	}

	/**
	 * Sets stop bits.
	 * 
	 * @param stopbits
	 *        New stop bits setting.
	 */
	public void setStopbits(String stopbits) {
		if (stopbits.equals("1")) { //$NON-NLS-1$
			this.stopbits = SerialPort.STOPBITS_1;
		}
		if (stopbits.equals("1.5")) { //$NON-NLS-1$
			this.stopbits = SerialPort.STOPBITS_1_5;
		}
		if (stopbits.equals("2")) { //$NON-NLS-1$
			this.stopbits = SerialPort.STOPBITS_2;
		}
	}

	/**
	 * Gets stop bits setting as an <code>int</code>.
	 * 
	 * @return Current stop bits setting.
	 */
	public int getStopbits() {
		return stopbits;
	}

	/**
	 * Gets stop bits setting as a <code>String</code>.
	 * 
	 * @return Current stop bits setting.
	 */
	public String getStopbitsString() {
		switch (stopbits) {
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
	 * Sets parity setting.
	 * 
	 * @param parity
	 *        New parity setting.
	 */
	public void setParity(int parity) {
		this.parity = parity;
	}

	/**
	 * Sets parity setting.
	 * 
	 * @param parity
	 *        New parity setting.
	 */
	public void setParity(String parity) {
		if (parity.equals("None")) { //$NON-NLS-1$
			this.parity = SerialPort.PARITY_NONE;
		}
		if (parity.equals("Even")) { //$NON-NLS-1$
			this.parity = SerialPort.PARITY_EVEN;
		}
		if (parity.equals("Odd")) { //$NON-NLS-1$
			this.parity = SerialPort.PARITY_ODD;
		}
	}

	/**
	 * Gets parity setting as an <code>int</code>.
	 * 
	 * @return Current parity setting.
	 */
	public int getParity() {
		return parity;
	}

	/**
	 * Gets parity setting as a <code>String</code>.
	 * 
	 * @return Current parity setting.
	 */
	public String getParityString() {
		switch (parity) {
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
	 * Converts a <code>String</code> describing a flow control type to an
	 * <code>int</code> type defined in <code>SerialPort</code>.
	 * 
	 * @param flowControl
	 *        A <code>string</code> describing a flow control type.
	 * @return An <code>int</code> describing a flow control type.
	 */
	private int stringToFlow(String flowControl) {
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

	/**
	 * Converts an <code>int</code> describing a flow control type to a
	 * <code>String</code> describing a flow control type.
	 * 
	 * @param flowControl
	 *        An <code>int</code> describing a flow control type.
	 * @return A <code>String</code> describing a flow control type.
	 */
	String flowToString(int flowControl) {
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
}
