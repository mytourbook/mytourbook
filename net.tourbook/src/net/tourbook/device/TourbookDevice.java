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
package net.tourbook.device;

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
	 * <code>true</code> when this device reader can read from the
	 * device, <code>false</code> (default) when the device reader can only import from
	 * a file
	 */
	public boolean	canReadFromDevice	= false;

}
