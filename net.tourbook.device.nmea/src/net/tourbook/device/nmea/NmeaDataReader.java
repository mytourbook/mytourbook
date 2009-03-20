/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.device.nmea;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.device.DeviceReaderTools;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

import org.opengts.util.Nmea0183;

public class NmeaDataReader extends TourbookDevice {

	private static final String		FILE_HEADER		= "$GP";							//$NON-NLS-1$

	private static final Calendar	fCalendar		= GregorianCalendar.getInstance();

	private ArrayList<TimeData>		fTimeDataList	= new ArrayList<TimeData>();
	private TimeData				fPrevTimeData;

	private float					fAbsoluteDistance;

	private String					fImportFilePath;
	private HashMap<Long, TourData>	fTourDataMap;

	public NmeaDataReader() {
		canReadFromDevice = false;
		canSelectMultipleFilesInImportDialog = true;
	}

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {
		return null;
	}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		return false;
	}

	public String getDeviceModeName(final int modeId) {
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

	private void parseNMEAStrings(final ArrayList<String> nmeaStrings) {

		final Nmea0183 nmea = new Nmea0183();

		// parse all nmea lines
		for (final String nmeaLine : nmeaStrings) {
			nmea.parse(nmeaLine);
		}

		// create new time item
		final TimeData timeData = new TimeData();
		fTimeDataList.add(timeData);

		// get attributes
		final double latitude = nmea.getLatitude();
		final double longitude = nmea.getLongitude();

		timeData.latitude = latitude == 90.0 ? Double.MIN_VALUE : latitude;
		timeData.longitude = longitude == 180.0 ? Double.MIN_VALUE : longitude;

		timeData.absoluteTime = nmea.getFixtime() * 1000;
		timeData.absoluteAltitude = (int) nmea.getAltitudeMeters();

		// calculate distance
		if (fPrevTimeData == null) {
			// first time data
			timeData.absoluteDistance = 0;
		} else {
			fAbsoluteDistance += DeviceReaderTools.computeDistance(fPrevTimeData.latitude,
					fPrevTimeData.longitude,
					latitude,
					longitude);

			timeData.absoluteDistance = fAbsoluteDistance;
		}

		// set virtual time if time is not available
		if (timeData.absoluteTime == Long.MIN_VALUE) {
			fCalendar.set(2000, 0, 1, 0, 0, 0);
			timeData.absoluteTime = fCalendar.getTimeInMillis();
		}

		fPrevTimeData = timeData;
	}

	public boolean processDeviceData(	final String fileName,
										final DeviceData deviceData,
										final HashMap<Long, TourData> tourDataMap) {

		// immediately bail out if the file format is not correct.
		if (!validateRawData(fileName)) {
			return false;
		}

		fImportFilePath = fileName;
		fTourDataMap = tourDataMap;

		// initialize new tour
		fAbsoluteDistance = 0;
		fPrevTimeData = null;
		fTimeDataList.clear();

		// if we are so far, we can assume that the file actually exists,
		// because the validateRawData call must check for it.
		final File file = new File(fileName);

		String nmeaLine;
		final ArrayList<String> nmeaStrings = new ArrayList<String>();

		long nmeaTypes = Nmea0183.TYPE_NONE;
		boolean startParsing = false;

		try {
			final BufferedReader reader = new BufferedReader(new FileReader(file));

			while ((nmeaLine = reader.readLine()) != null) {

				if (nmeaLine.startsWith("$GPRMC")) {//$NON-NLS-1$

					if ((nmeaTypes & Nmea0183.TYPE_GPRMC) != 0) {
						nmeaTypes = Nmea0183.TYPE_GPRMC;
						startParsing = true;
					} else {
						nmeaTypes |= Nmea0183.TYPE_GPRMC;
					}

				} else if (nmeaLine.startsWith("$GPGGA")) {//$NON-NLS-1$

					if ((nmeaTypes & Nmea0183.TYPE_GPGGA) != 0) {
						nmeaTypes = Nmea0183.TYPE_GPGGA;
						startParsing = true;
					} else {
						nmeaTypes |= Nmea0183.TYPE_GPGGA;
					}

				} else if (nmeaLine.startsWith("$GPVTG")) {//$NON-NLS-1$

					if ((nmeaTypes & Nmea0183.TYPE_GPVTG) != 0) {
						nmeaTypes = Nmea0183.TYPE_GPVTG;
						startParsing = true;
					} else {
						nmeaTypes |= Nmea0183.TYPE_GPVTG;
					}

				} else if (nmeaLine.startsWith("$GPZDA")) {//$NON-NLS-1$

					if ((nmeaTypes & Nmea0183.TYPE_GPZDA) != 0) {
						nmeaTypes = Nmea0183.TYPE_GPZDA;
						startParsing = true;
					} else {
						nmeaTypes |= Nmea0183.TYPE_GPZDA;
					}
				} else {

					// ignore invalid lines
					continue;
				}

				if (startParsing) {

					startParsing = false;

					parseNMEAStrings(nmeaStrings);

					// reset nmea strings and types
					nmeaStrings.clear();
				}

				nmeaStrings.add(nmeaLine);
			}

		} catch (final Exception e) {
			e.printStackTrace();
		}

		return setTourData();
	}

	private boolean setTourData() {

		if (fTimeDataList == null || fTimeDataList.size() == 0) {
			return false;
		}

		// create data object for each tour
		final TourData tourData = new TourData();

		/*
		 * set tour start date/time
		 */
		fCalendar.setTimeInMillis(fTimeDataList.get(0).absoluteTime);

		tourData.setStartHour((short) fCalendar.get(Calendar.HOUR_OF_DAY));
		tourData.setStartMinute((short) fCalendar.get(Calendar.MINUTE));
		tourData.setStartSecond((short) fCalendar.get(Calendar.SECOND));

		tourData.setStartYear((short) fCalendar.get(Calendar.YEAR));
		tourData.setStartMonth((short) (fCalendar.get(Calendar.MONTH) + 1));
		tourData.setStartDay((short) fCalendar.get(Calendar.DAY_OF_MONTH));

		tourData.setStartWeek((short) fCalendar.get(Calendar.WEEK_OF_YEAR));

		tourData.setDeviceTimeInterval((short) -1);
		tourData.importRawDataFile = fImportFilePath;
		tourData.setTourImportFilePath(fImportFilePath);

		tourData.createTimeSeries(fTimeDataList, true);
		tourData.computeAltitudeUpDown();

		// after all data are added, the tour id can be created
		final int[] distanceSerie = tourData.getMetricDistanceSerie();
		String uniqueKey;
		if (distanceSerie == null) {
			uniqueKey = "32481"; //$NON-NLS-1$
		} else {
			uniqueKey = Integer.toString(distanceSerie[distanceSerie.length - 1]);
		}
		final Long tourId = tourData.createTourId(uniqueKey);

		// check if the tour is already imported
		if (fTourDataMap.containsKey(tourId) == false) {

			tourData.computeTourDrivingTime();
			tourData.computeComputedValues();

			tourData.setDeviceId(deviceId);
			tourData.setDeviceName(visibleName);

			// add new tour to other tours
			fTourDataMap.put(tourId, tourData);
		}

		return true;
	}

	public boolean validateRawData(final String fileName) {

		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(fileName));
			String nmeaLine;

			while ((nmeaLine = reader.readLine()) != null) {
				if (nmeaLine.startsWith(FILE_HEADER)) {
					return true;
				}
			}

		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
		}

		return false;
	}

}
