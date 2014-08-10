/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.device.suunto;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.device.InvalidDeviceSAXException;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

public class SuuntoDeviceDataReader extends TourbookDevice {

	public static final String	TAG_SUUNTO						= "suunto";

	private static final String	SUUNTO_TAG_WELL_FORMED_BEGIN	= "<" + TAG_SUUNTO + ">";	//$NON-NLS-1$
	private static final String	SUUNTO_TAG_WELL_FORMED_END		= "</suunto>";				//$NON-NLS-1$
	private static final String	SUUNTO_TAG_HEADER				= "<header>";				//$NON-NLS-1$
	private static final String	SUUNTO_TAG_SAMPLES				= "<samples>";				//$NON-NLS-1$

	// plugin constructor
	public SuuntoDeviceDataReader() {}

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {
		return null;
	}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		return false;
	}

	private InputStream convertIntoWellFormedXml(final String importFilePath) {

		BufferedReader fileReader = null;
		StringWriter xmlWriter = null;

		try {

			xmlWriter = new StringWriter();

			final FileInputStream inputStream = new FileInputStream(importFilePath);
			fileReader = new BufferedReader(new InputStreamReader(inputStream, UI.UTF_8));

			String line = fileReader.readLine();
			if (line.toLowerCase().startsWith(XML_START_ID)) {
				// write "<?xml"
				xmlWriter.write(line);
				xmlWriter.write(UI.NEW_LINE);
			}

			// <suunto>
			xmlWriter.write(SUUNTO_TAG_WELL_FORMED_BEGIN);
			xmlWriter.write(UI.NEW_LINE);
			{
				while ((line = fileReader.readLine()) != null) {
					xmlWriter.write(line);
					xmlWriter.write(UI.NEW_LINE);
				}
			}
			// </suunto>
			xmlWriter.write(SUUNTO_TAG_WELL_FORMED_END);
			xmlWriter.write(UI.NEW_LINE);

		} catch (final Exception e1) {

			StatusUtil.log(e1);

		} finally {
			Util.closeReader(fileReader);
			Util.closeWriter(xmlWriter);
		}

		final String xml = xmlWriter.toString();

		return new ByteArrayInputStream(xml.getBytes());
	}

	public String getDeviceModeName(final int profileId) {
		return null;
	}

	@Override
	public SerialParameters getPortParameters(final String portName) {
		return null;
	}

	@Override
	public int getStartSequenceSize() {
		return -1;
	}

	public int getTransferDataSize() {
		return -1;
	}

	/**
	 * Check if the file is a valid Suunto xml file by checking some tags.
	 * 
	 * @param importFilePath
	 * @return Returns <code>true</code> when the file contains garmin content.
	 */
	private boolean isSuuntoXMLFile(final String importFilePath) {

		BufferedReader fileReader = null;

		try {

			final FileInputStream inputStream = new FileInputStream(importFilePath);

			fileReader = new BufferedReader(new InputStreamReader(inputStream, UI.UTF_8));

			String line = fileReader.readLine();
			if (line == null || line.toLowerCase().startsWith(XML_START_ID) == false) {
				return false;
			}

			line = fileReader.readLine();
			if (line == null || line.toLowerCase().startsWith(SUUNTO_TAG_HEADER) == false) {
				return false;
			}

			while ((line = fileReader.readLine()) != null) {

				if (line.toLowerCase().startsWith(SUUNTO_TAG_SAMPLES)) {
					return true;
				}
			}

		} catch (final Exception e1) {

			StatusUtil.log(e1);

		} finally {
			Util.closeReader(fileReader);
		}

		return false;
	}

	@Override
	public boolean processDeviceData(	final String importFilePath,
										final DeviceData deviceData,
										final HashMap<Long, TourData> alreadyImportedTours,
										final HashMap<Long, TourData> newlyImportedTours) {

		if (isSuuntoXMLFile(importFilePath) == false) {
			return false;
		}

		final SuuntoSAXHandler saxHandler = new SuuntoSAXHandler(
				this,
				importFilePath,
				alreadyImportedTours,
				newlyImportedTours);

		try {

			final InputStream inputStream = convertIntoWellFormedXml(importFilePath);
			final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

			parser.parse(inputStream, saxHandler);

		} catch (final InvalidDeviceSAXException e) {
			StatusUtil.log(e);
			return false;
		} catch (final Exception e) {
			StatusUtil.log("Error parsing file: " + importFilePath, e); //$NON-NLS-1$
			return false;
		} finally {

			saxHandler.dispose();
		}

		return saxHandler.isImported();
	}

	public boolean validateRawData(final String fileName) {
		return isSuuntoXMLFile(fileName);
	}
}
