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

package net.tourbook.importdata;

import java.util.HashMap;

import net.tourbook.data.TourData;

public interface IRawDataReader {

	/**
	 * @return Returns the complete data size which will be received on the COM port for this device
	 */
	public int getTransferDataSize();

	/**
	 * Read the data from the raw data file and create the device and tour data
	 * 
	 * @param fileName
	 * @param deviceData
	 * @param tourDataMap
	 * @return Returns <code>true</code> when the import was successfull, the parameters
	 *         <code>deviceData</code> and <code>tourData</code> are set from the imported file.
	 */
	public boolean processDeviceData(String fileName, DeviceData deviceData, HashMap<String, TourData> tourDataMap);

	/**
	 * Validate data format
	 * 
	 * @param fileName
	 *            file name for the file which is validated
	 * @return return <code>true</code> when the file has the format for this device reader
	 */
	public boolean validateRawData(String fileName);

	/**
	 * @return returns the profile name for the profile id
	 */
	public String getDeviceModeName(int modeId);

}
