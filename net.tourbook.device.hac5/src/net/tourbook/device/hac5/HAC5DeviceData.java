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
package net.tourbook.device.hac5;

import java.io.IOException;
import java.io.RandomAccessFile;

import net.tourbook.device.DeviceReaderTools;

public class HAC5DeviceData {

	private static final int	OFFSET_DEVICE_DATA	= 0x380;

	private long				bike1DrivingTime;
	private long				bike1Distance;
	private long				bike1AltitudeUp;
	private long				bike1AltitudeDown;

	public void readFromFile(RandomAccessFile fileRawData) {

		byte[] recordBuffer = new byte[0x10];

		try {

			// position file pointer to the device data
			fileRawData.seek(HAC5DeviceDataReader.OFFSET_RAWDATA + OFFSET_DEVICE_DATA + 4);
			fileRawData.read(recordBuffer);

			bike1DrivingTime = DeviceReaderTools.get4ByteData(recordBuffer, 0);
			bike1Distance = DeviceReaderTools.get4ByteData(recordBuffer, 4);
			bike1AltitudeUp = DeviceReaderTools.get4ByteData(recordBuffer, 8);
			bike1AltitudeDown = DeviceReaderTools.get4ByteData(recordBuffer, 12);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
