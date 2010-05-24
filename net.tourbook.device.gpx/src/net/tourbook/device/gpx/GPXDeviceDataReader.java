/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.device.gpx;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.SAXParserFactory;

import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.xml.sax.SAXParseException;

public class GPXDeviceDataReader extends TourbookDevice {

	private static final String	XML_START_ID	= "<?xml";	//$NON-NLS-1$

	// plugin constructor
	public GPXDeviceDataReader() {}

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

	public boolean processDeviceData(	final String importFilePath,
										final DeviceData deviceData,
										final HashMap<Long, TourData> tourDataMap) {

		/*
		 * check if the file is a xml file
		 */
		BufferedReader fileReader = null;
		try {
			fileReader = new BufferedReader(new FileReader(importFilePath));
			final String fileHeader = fileReader.readLine();
			if (fileHeader == null || fileHeader.contains(XML_START_ID) == false) {
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
				e.printStackTrace();
			}
		}

		final GPX_SAX_Handler handler = new GPX_SAX_Handler(this, importFilePath, deviceData, tourDataMap);

		try {

			SAXParserFactory.newInstance().newSAXParser().parse("file:" + importFilePath, handler);//$NON-NLS-1$

		} catch (final SAXParseException e) {

			final StringBuilder sb = new StringBuilder()//
					.append("XML error when parsing file:\n") //$NON-NLS-1$
					.append(UI.NEW_LINE)
					.append(importFilePath)
					.append(UI.NEW_LINE)
					.append(UI.NEW_LINE)
					.append(e.getLocalizedMessage())
					.append(UI.NEW_LINE)
					.append(UI.NEW_LINE)
					.append("Line: ") //$NON-NLS-1$
					.append(e.getLineNumber())
					.append("\tColumn: ") //$NON-NLS-1$
					.append(e.getColumnNumber())
			//
			;

			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", sb.toString()); //$NON-NLS-1$
				}
			});

			e.printStackTrace();
			return false;

		} catch (final Exception e) {
			System.err.println("Error parsing file: " + importFilePath); //$NON-NLS-1$
			e.printStackTrace();
			return false;
		}

		return handler.isImported();
	}

	public boolean validateRawData(final String fileName) {
		return true;
	}
}
