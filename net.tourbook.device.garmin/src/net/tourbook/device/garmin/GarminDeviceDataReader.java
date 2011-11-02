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
import net.tourbook.ui.UI;
import net.tourbook.util.StatusUtil;

public class GarminDeviceDataReader extends TourbookDevice {

	private static final String	XML_START_ID	= "<?xml";						//$NON-NLS-1$
	private static final String	XML_GARMIN_TAG	= "<TrainingCenterDatabase";	//$NON-NLS-1$

	// plugin constructor
	public GarminDeviceDataReader() {}

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {
		// NEXT Auto-generated method stub
		return null;
	}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		return true;
	}

	public String getDeviceModeName(final int profileId) {
		return UI.EMPTY_STRING;
	}

	@Override
	public SerialParameters getPortParameters(final String portName) {
		return null;
	}

	@Override
	public int getStartSequenceSize() {
		return 0;
	}

	public int getTransferDataSize() {
		return -1;
	}

	/**
	 * check if the file is a xml file
	 */
	private boolean isXMLFile(final String importFilePath) {

		BufferedReader fileReader = null;
		try {
			fileReader = new BufferedReader(new FileReader(importFilePath));
			final String fileHeader = fileReader.readLine();
			if (fileHeader == null
					|| (fileHeader.startsWith(XML_START_ID) || fileHeader.startsWith(XML_GARMIN_TAG)) == false) {

				fileReader.close();
				return false;
			}

		} catch (final FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fileReader != null) {
					fileReader.close();
				}
			} catch (final IOException e) {
				StatusUtil.showStatus(e);
			}
		}

		return true;
	}

	@Override
	public boolean processDeviceData(	final String importFilePath,
										final DeviceData deviceData,
										final HashMap<Long, TourData> alreadyImportedTours,
										final HashMap<Long, TourData> newlyImportedTours) {

		if (isXMLFile(importFilePath) == false) {
			return false;
		}

		final GarminSAXHandler saxHandler = new GarminSAXHandler(
				this,
				importFilePath,
				deviceData,
				alreadyImportedTours,
				newlyImportedTours);

		try {

			final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

			parser.parse("file:" + importFilePath, saxHandler);//$NON-NLS-1$

		} catch (final Exception e) {
			StatusUtil.log("Error parsing file: " + importFilePath, e); //$NON-NLS-1$
			return false;
		} finally {
			saxHandler.dispose();
		}

		return saxHandler.isImported();
	}

	public boolean validateRawData(final String fileName) {
		return isXMLFile(fileName);
	}
}
