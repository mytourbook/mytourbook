/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
	 * @return returns the profile name for the profile id
	 */
	public String getDeviceModeName(int modeId);

	/**
	 * @return Returns the complete data size which will be received on the COM port for this device
	 */
	public int getTransferDataSize();

	/**
	 * Read the data from the raw data file and create the device and tour data
	 * 
	 * @param importFilePath
	 * @param deviceData
	 * @param alreadyImportedTours
	 *            Contains all tours which are already imported and displayed in the import view.
	 *            Tour id is the hash map key. Newly imported tour should not be added to this map,
	 *            they are added after this method is returned, they must be put into the parameter
	 *            <i>importedTours</i> map.
	 *            <p>
	 *            This map can be used to check if a tour is already imported and displayed in the
	 *            import view.
	 * @param newlyImportedTours
	 *            Contains all tours which are imported in this method.
	 * @return Returns <code>true</code> when the import was successfull, the parameters
	 *         <code>deviceData</code> and <code>tourData</code> are set from the imported file.
	 */
	public boolean processDeviceData(	String importFilePath,
										DeviceData deviceData,
										HashMap<Long, TourData> alreadyImportedTours,
										HashMap<Long, TourData> newlyImportedTours);

	/**
	 * Validate data format
	 * 
	 * @param importFilePath
	 *            file name for the file which is validated
	 * @return return <code>true</code> when the file has the format for this device reader
	 */
	public boolean validateRawData(String importFilePath);

}
