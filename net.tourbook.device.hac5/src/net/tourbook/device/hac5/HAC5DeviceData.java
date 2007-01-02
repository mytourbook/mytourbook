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
package net.tourbook.device.hac5;

import java.io.IOException;
import java.io.RandomAccessFile;

public class HAC5DeviceData {

	public void readFromFile(RandomAccessFile fileRawData) {

		try {

			// position file pointer to the device data
			fileRawData.seek(0x0 + HAC5DeviceDataReader.OFFSET_RAWDATA);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
