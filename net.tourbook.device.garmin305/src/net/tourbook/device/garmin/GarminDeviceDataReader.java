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
package net.tourbook.device.garmin;

import gnu.io.SerialPort;

import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

public class GarminDeviceDataReader extends TourbookDevice {

	// plugin constructor
	public GarminDeviceDataReader() {}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		return true;
	}

	public String getDeviceModeName(final int profileId) {
		return ""; //$NON-NLS-1$
	}

	public int getImportDataSize() {
		return 0x10000;
	}

	@Override
	public SerialParameters getPortParameters(final String portName) {

		final SerialParameters portParameters = new SerialParameters(portName,
				4800,
				SerialPort.FLOWCONTROL_NONE,
				SerialPort.FLOWCONTROL_NONE,
				SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE);

		return portParameters;
	}

	@Override
	public int getStartSequenceSize() {
		return 0;
	}

	public boolean processDeviceData(	final String importFileName,
										final DeviceData deviceData,
										final HashMap<String, TourData> tourDataMap) {

		GarminSAXHandler handler = new GarminSAXHandler(this,
				importFileName,
				deviceData,
				tourDataMap);

		try {

			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

			parser.parse("file:" + importFileName, handler);//$NON-NLS-1$

		} catch (Exception e) {
			System.err.println("Error parsing " + importFileName + ": " + e); //$NON-NLS-1$ //$NON-NLS-2$
			e.printStackTrace();
			return false;
		}

		return handler.isImported();
	}

	public boolean validateRawData(final String fileName) {
		return true;
	}
}
