/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.device.ciclotour.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.StringTokenizer;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

public class CiclotourTextDataReader extends TourbookDevice {

	private static final String	FILE_HEADER_EN	= "Time:	Distance:	Alt.:	Speed:	HR:	Temperature:	Gradient:	Cadence:";	//$NON-NLS-1$
	private static final String	FILE_HEADER_DE	= "Zeit:	Strecke:	Höhe:	Geschw:	Puls:	Temperatur:	Prozent:	Cadence:";	//$NON-NLS-1$

	public CiclotourTextDataReader() {}

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {
		return null;
	}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		return false;
	}

	/**
	 * Derives a date from a filename, if possible. The name of the file must start with
	 * <code>ddmmyy</code>, with <code>dd</code> being the day of year, <code>mm</code> the month,
	 * and <code>yy</code> the year.
	 * <p>
	 * As a default, todays date is returned.
	 * 
	 * @param file
	 *            The file from which the name should be derived.
	 * @return A Date object that has its calendar information set correctly, but not its time
	 *         information.
	 */
	private ZonedDateTime deriveDateFromFile(final File file) {

		ZonedDateTime fileDate = null;

		// only get the last part of the filename
		final String filename = file.getName();

		// standard format is ddmmyy.txt, but the user can actually modify it,
		// so be
		// careful.
		try {
			if (filename.length() > 6) {

				final String dayDenom = filename.substring(0, 2);
				final String monthDenom = filename.substring(2, 4);
				final String yearDenom = filename.substring(4, 6);

				final int day = Integer.parseInt(dayDenom);
				final int month = Integer.parseInt(monthDenom);
				int year = Integer.parseInt(yearDenom);

				// we assume that a two-digit date smaller 90 is in the 21st century,
				// and dates larger 90 in the 20st century, covering a timespan of
				// 1900 to 2089, which should be ample.
				if (year < 90) {
					year += 2000;
				} else {
					year += 1900;
				}

				fileDate = ZonedDateTime.of(year, month, day, 0, 0, 0, 0, TimeTools.getDefaultTimeZone());
			}

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
		}

		return fileDate;
	}

	@Override
	public String getDeviceModeName(final int modeId) {
		return null;
	}

	@Override
	public SerialParameters getPortParameters(final String portName) {
		return null;
	}

	/**
	 * Computes the equivalent of the time section of the parameter in seconds. The date section is
	 * ignored.
	 * 
	 * @param cal
	 *            A Calendar object
	 * @return The equivalent of the time in seconds
	 */
	private int getSeconds(final Calendar cal) {
		return (cal.get(Calendar.HOUR_OF_DAY) * 3600) + (cal.get(Calendar.MINUTE) * 60) + (cal.get(Calendar.SECOND));

	}

	@Override
	public int getStartSequenceSize() {
		return -1;
	}

	@Override
	public int getTransferDataSize() {
		return -1;
	}

	@Override
	public boolean processDeviceData(	final String importFilePath,
										final DeviceData deviceData,
										final HashMap<Long, TourData> alreadyImportedTours,
										final HashMap<Long, TourData> newlyImportedTours) {

		// immediately bail out if the file format is not correct.
		if (!validateRawData(importFilePath)) {
			return false;
		}

		final TourData tourData = new TourData();

		// if we are this far, we can assume that the file actually exists,
		// because
		// the validateRawData call must check for it.
		final File file = new File(importFilePath);

		// The text file export does not record any time or date information,
		// but if we are really lucky, the user did not change the filename
		// format,
		// and we at least get the correct date. Time info is lost.
		final ZonedDateTime fileDate = deriveDateFromFile(file);

		tourData.setTourStartTime(fileDate);

		StringTokenizer tokenizer = null;
		String tokenLine;

		final ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();
		TimeData timeData;

		int previousTime = 0;
		int time = 0;
		int timeDelta = 0;

		float distance = 0;
		float previousDistance = 0;
		float distanceDelta = 0;

		int alt = 0;
		int previousAlt = 0;
		int altDelta = 0;

		BufferedReader reader = null;

		try {

			reader = new BufferedReader(new FileReader(file));

			// skip the header line
			reader.readLine();

			while ((tokenLine = reader.readLine()) != null) {

				if (tokenLine.length() == 0) {
					continue;
				}

				// file format is Tabbed Seperated Values
				tokenizer = new StringTokenizer(tokenLine, "\t"); //$NON-NLS-1$

				final String recTime = tokenizer.nextToken();
				distance = Float.parseFloat(tokenizer.nextToken());
				alt = Integer.parseInt(tokenizer.nextToken());

				// not recorded, but read for the fun of it.
				@SuppressWarnings("unused")
				final float speed = Float.parseFloat(tokenizer.nextToken());

				final int heartrate = Integer.parseInt(tokenizer.nextToken());
				final float temperature = Float.parseFloat(tokenizer.nextToken());

				// same as with speed ...
				@SuppressWarnings("unused")
				final float gradient = Float.parseFloat(tokenizer.nextToken());

				final int cadence = Integer.parseInt(tokenizer.nextToken());

				final DateFormat df = DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.GERMAN);

				final Calendar dataCal = Calendar.getInstance();

				dataCal.setTime(df.parse(recTime));
				time = getSeconds(dataCal);

				// values are recorded absolutely, but we need to get the difference
				// between the current and the previous data point. Let's compute
				// the delta
				distanceDelta = distance - previousDistance;
				timeDelta = time - previousTime;
				altDelta = alt - previousAlt;

				timeDataList.add(timeData = new TimeData());

				timeData.altitude = altDelta;
				timeData.cadence = cadence;
				timeData.pulse = heartrate;
				timeData.temperature = temperature;
				timeData.time = timeDelta;

				// distance is stored in kilometers, but we need meters.
				timeData.distance = Math.round(distanceDelta * 1000);

				previousTime = time;
				previousDistance = distance;
				previousAlt = alt;
			}

			tourData.setStartDistance(Math.round(distance));

			tourData.setImportFilePath(importFilePath);

			tourData.setDeviceId(deviceId);
			tourData.setDeviceName(visibleName);

			tourData.createTimeSeries(timeDataList, false);

			// after all data are added, the tour id can be created
			final int tourIdSuffix = (int) Math.abs(tourData.getStartDistance());
			final String uniqueId = createUniqueId_Legacy(tourData, tourIdSuffix);
			final Long tourId = tourData.createTourId(uniqueId);

			// check if the tour is in the tour map
			if (alreadyImportedTours.containsKey(tourId) == false) {

				// add new tour to the map
				newlyImportedTours.put(tourId, tourData);

				// create additional data
				tourData.computeTourDrivingTime();
				tourData.computeComputedValues();
			}

		} catch (final Exception e) {

			StatusUtil.showStatus(e);

		} finally {

			Util.closeReader(reader);
		}

		return true;
	}

	/**
	 * Checks the presence of the header information written by the CicloTour text export function.
	 * 
	 * @see net.tourbook.importdata.IRawDataReader#validateRawData(java.lang.String)
	 * @return <code>true</code> if the file appears to be a valid CicloTour Text file, otherwise
	 *         <code>false</code>.
	 */
	@Override
	public boolean validateRawData(final String fileName) {

		BufferedReader reader = null;

		try {

			reader = new BufferedReader(new FileReader(fileName));

			final String header = reader.readLine();

			if (header == null) {
				return false;
			}

			if (header.startsWith(FILE_HEADER_EN)) {
				return true;
			}

			if (header.startsWith(FILE_HEADER_DE)) {
				return true;
			}

			return false;

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					StatusUtil.showStatus(e);
				}
			}
		}

		return false;
	}

}
