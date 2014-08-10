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
package net.tourbook.device.sporttracks;

import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.device.InvalidDeviceSAXException;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;

public class FitLogDeviceDataReader extends TourbookDevice {

	private static final String				XML_FIT_LOG_TAG	= "<FitnessWorkbook ";								//$NON-NLS-1$

	private static final IPreferenceStore	_prefStore		= TourbookPlugin.getDefault().getPreferenceStore();

	// plugin constructor
	public FitLogDeviceDataReader() {}

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {
		return null;
	}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		return false;
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

	@Override
	public boolean processDeviceData(	final String importFilePath,
										final DeviceData deviceData,
										final HashMap<Long, TourData> alreadyImportedTours,
										final HashMap<Long, TourData> newlyImportedTours) {

		if (isValidXMLFile(importFilePath, XML_FIT_LOG_TAG, true) == false) {
			return false;
		}

		final FitLogSAXHandler saxHandler = new FitLogSAXHandler(
				this,
				importFilePath,
				alreadyImportedTours,
				newlyImportedTours);

		try {

			final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

			parser.parse("file:" + importFilePath, saxHandler);//$NON-NLS-1$

		} catch (final InvalidDeviceSAXException e) {
			StatusUtil.log(e);
			return false;
		} catch (final Exception e) {
			StatusUtil.log("Error parsing file: " + importFilePath, e); //$NON-NLS-1$
			return false;
		} finally {

			final Display display = Display.getDefault();

			if (saxHandler.isNewTag()) {
				display.syncExec(new Runnable() {
					public void run() {
						TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED);
					}
				});
			}

			if (saxHandler.isNewTourType()) {

				TourDatabase.clearTourTypes();
				TourManager.getInstance().clearTourDataCache();

				display.syncExec(new Runnable() {
					public void run() {
						// fire modify event
						_prefStore.setValue(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED, Math.random());
					}
				});

			}
		}

		return saxHandler.isImported();
	}

	public boolean validateRawData(final String fileName) {

		/*
		 * .fitlog files contain BOM's (Byte Order Mark)
		 */
		return isValidXMLFile(fileName, XML_FIT_LOG_TAG, true);
	}
}
