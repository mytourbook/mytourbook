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

package net.tourbook.importdata;


public abstract class TourbookDevice implements IRawDataReader {

	public TourbookDevice() {}

	public TourbookDevice(String deviceName) {
		visibleName = deviceName;
	}

	/**
	 * Unique id for each device reader
	 */
	public String	deviceId;

	/**
	 * Visible device name, e.g. HAC4, HAC5
	 */
	public String	visibleName;

	/**
	 * File extension used when tour data are imported from a file
	 */
	public String	fileExtension;

	/**
	 * <code>true</code> when this device reader can read from the device,
	 * <code>false</code> (default) when the device reader can only import
	 * from a file
	 */
	public boolean	canReadFromDevice	= false;

	/**
	 * @param portName
	 * @return returns the serial port parameters which are use to receive data
	 *         from the device
	 */
	public abstract SerialParameters getPortParameters(String portName);

	/**
	 * Check if the received data are correct for this device, Returns
	 * <code>true</code> when the received data are correct for this device
	 * 
	 * @param byteIndex
	 *        index in the byte stream, this will be incremented when the return
	 *        value is true
	 * @param newByte
	 *        received byte
	 * @return Return <code>true</code> when the receice data are correct for
	 *         this device
	 */
	public abstract boolean checkStartSequence(int byteIndex, int newByte);

	/**
	 * Returns the number of bytes which will be checked in the startsequence.
	 * For a HAC4/5 this can be set to 4 because the first 4 bytes of the input
	 * stream are always the characters AFRO
	 * 
	 * @return
	 */
	public abstract int getStartSequenceSize();

}
