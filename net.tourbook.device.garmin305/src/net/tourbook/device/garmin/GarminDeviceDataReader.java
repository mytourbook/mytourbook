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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

public class GarminDeviceDataReader extends TourbookDevice {

	private static final String	XML_START_ID	= "<?xml"; //$NON-NLS-1$

	// plugin constructor
	public GarminDeviceDataReader() {}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		return true;
	}

	public String getDeviceModeName(final int profileId) {
		return ""; //$NON-NLS-1$
	}

	public int getTransferDataSize() {
		return -1;
	}

	@Override
	public SerialParameters getPortParameters(final String portName) {
		return null;
	}

	@Override
	public int getStartSequenceSize() {
		return 0;
	}

	public boolean processDeviceData(	final String importFilePath,
										final DeviceData deviceData,
										final HashMap<String, TourData> tourDataMap) {

		/*
		 * check if the file is a xml file
		 */
		BufferedReader fileReader = null;
		try {
			fileReader = new BufferedReader(new FileReader(importFilePath));
			String fileHeader = fileReader.readLine();
			if (fileHeader == null || fileHeader.startsWith(XML_START_ID) == false) {
				fileReader.close();
				return false;
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fileReader != null) {
					fileReader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		GarminSAXHandler handler = new GarminSAXHandler(this, importFilePath, deviceData, tourDataMap);

		try {

			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

			parser.parse("file:" + importFilePath, handler);//$NON-NLS-1$

		} catch (Exception e) {
			System.err.println("Error parsing file: " + importFilePath); //$NON-NLS-1$ 
//			System.err.println(e);
			e.printStackTrace();
			return false;
		}

		return handler.isImported();
	}

	public boolean validateRawData(final String fileName) {
		return true;
	}
}
