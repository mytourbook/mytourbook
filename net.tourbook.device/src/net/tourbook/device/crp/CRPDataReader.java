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
package net.tourbook.device.crp;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import net.tourbook.chart.ChartLabel;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.ui.UI;

public class CRPDataReader extends TourbookDevice {

	// plugin constructor
	public CRPDataReader() {}

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {
		return null;
	}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		return false;
	}

	public String getDeviceModeName(final int profileId) {

		// 0: Run
		// 1: Ski
		// 2: Bike
		// 3: Ski/Bike
		// 4: Altitude

		switch (profileId) {
		case 0:
			return Messages.CRP_Profile_run;

		case 1:
			return Messages.CRP_Profile_ski;

		case 2:
			return Messages.CRP_Profile_bike;

		case 3:
			return Messages.CRP_Profile_ski_bike;

		case 4:
			return Messages.CRP_Profile_altitude;

		default:
			break;
		}

		return Messages.CRP_Profile_unknown;
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

		boolean returnValue = false;

		// reset tour data list
		// tourDataList.clear();

		BufferedReader fileReader = null;

		// int tourStartOdoMeter = 0;
		// int tourStartOdoSec = 0;
		// int tourStartOdoUp = 0;
		// int tourStartOdoDown = 0;
		//
		// double bikeMass;
		// double bikerWeight;
		// double bikerHeight;

		try {

			fileReader = new BufferedReader(new FileReader(importFilePath));

			final String fileHeader = fileReader.readLine();
			if (fileHeader.startsWith("HRMProfilDatas") == false) { //$NON-NLS-1$
				return false;
			}

			String line;
			StringTokenizer tokenLine;
			final ArrayList<String> trackPoints = new ArrayList<String>();

			tokenLine = new StringTokenizer(fileReader.readLine());
			@SuppressWarnings("unused")
			final String fileVersion = tokenLine.nextToken();

			// get all trackpoints
			while ((line = fileReader.readLine()) != null) {
				if (line.equals("***")) { //$NON-NLS-1$
					break;
				}
				trackPoints.add(new String(line.toString()));
			}

			// skip line
			fileReader.readLine();

			/*
			 * line: date/time
			 */
			tokenLine = new StringTokenizer(fileReader.readLine());

			// tour start date
			final String tourStartDate = tokenLine.nextToken();
			final int tourYear = Integer.parseInt(tourStartDate.substring(6));
			final int tourMonth = Integer.parseInt(tourStartDate.substring(3, 5));
			final int tourDay = Integer.parseInt(tourStartDate.substring(0, 2));

			// tour start time
			final String tourStartTime = tokenLine.nextToken();
			final int tourHour = Integer.parseInt(tourStartTime.substring(0, 2));
			final int tourMin = tourStartTime.length() > 5 //
					? Integer.parseInt(tourStartTime.substring(3, 5))
					: Integer.parseInt(tourStartTime.substring(3));

			// recording time
			final String tourRecTimeSt = tokenLine.nextToken();
			final int tourRecordingTime = Integer.parseInt(tourRecTimeSt.substring(0, 2))
					* 3600
					+ Integer.parseInt(tourRecTimeSt.substring(3, 5))
					* 60
					+ Integer.parseInt(tourRecTimeSt.substring(6));

			// category
			tokenLine.nextToken();

			// difficulty
			tokenLine.nextToken();

			// tour name
			String tourName = UI.EMPTY_STRING;
			if (tokenLine.hasMoreTokens()) {
				tourName = tokenLine.nextToken("\t"); //$NON-NLS-1$
			}

			// skip lines
			fileReader.readLine();
			fileReader.readLine();

			/*
			 * line: interval/mode
			 */
			tokenLine = new StringTokenizer(fileReader.readLine());
			final int interval = Integer.parseInt(tokenLine.nextToken());
			final int tourMode = Integer.parseInt(tokenLine.nextToken());

			// skip empty lines
			fileReader.readLine();
			fileReader.readLine();

			// skip lines
			fileReader.readLine();
			fileReader.readLine();

			/*
			 * lines: tour description
			 */
			String tourDesc = UI.EMPTY_STRING;
			while ((line = fileReader.readLine()) != null) {
				tourDesc += line + "\n"; //$NON-NLS-1$
			}
			tourDesc = tourDesc.trim();

			/*
			 * set tour data
			 */
			final TourData tourData = new TourData();

			tourData.setTourStartTime(tourYear, tourMonth, tourDay, tourHour, tourMin, 0);

			tourData.setTourTitle(tourName);
			tourData.setTourDescription(tourDesc);

			tourData.setDeviceMode((short) (tourMode));
			tourData.setDeviceModeName(getDeviceModeName(tourMode));

			tourData.setDeviceTimeInterval((short) interval);

			tourData.importRawDataFile = importFilePath;
			tourData.setTourImportFilePath(importFilePath);

			// tourData.setStartDistance((int) DeviceReaderTools.get4ByteData(buffer, 8));
			// tourData.setStartAltitude((short) DeviceReaderTools.get2ByteData(buffer, 12));
			// tourData.setStartPulse((short) (buffer[14] & 0xff));

			/*
			 * set time serie from the imported trackpoints
			 */
			final ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();
			TimeData timeData;

			int tpIndex = 0;
			int tourTime = 0;

			int pulse;
			int distance = 0;
			@SuppressWarnings("unused")
			int speed;
			int altitude;
			@SuppressWarnings("unused")
			int color;
			@SuppressWarnings("unused")
			int symbol;
			int temperature;
			@SuppressWarnings("unused")
			String trackpointTime;

			int oldDistance = 0;
			int oldAltitude = 0;
			int tourAltUp = 0;
			int tourAltDown = 0;

			int sumAltitude = 0;
			int sumDistance = 0;
			int sumPulse = 0;
			int sumTemperature = 0;

			for (final String trackPoint : trackPoints) {

				tokenLine = new StringTokenizer(trackPoint);

				pulse = Integer.parseInt(tokenLine.nextToken());
				speed = Integer.parseInt(tokenLine.nextToken()); // [0.1 km/h]
				distance = Integer.parseInt(tokenLine.nextToken()) * 10; // [m]
				altitude = Integer.parseInt(tokenLine.nextToken()); // [m]
				color = Integer.parseInt(tokenLine.nextToken()); // [0..4]
				symbol = Integer.parseInt(tokenLine.nextToken()); // [0..42]

				temperature = Math.round(//
						Float.parseFloat(tokenLine.nextToken().replace(',', '.'))); // [C]

				trackpointTime = tokenLine.nextToken();

				// get comment for current trackpoint
				String comment = UI.EMPTY_STRING;
				if (tokenLine.hasMoreTokens()) {
					comment = tokenLine.nextToken("\t"); //$NON-NLS-1$
				}

				timeDataList.add(timeData = new TimeData());

				final short altitudeDiff = (short) (altitude - oldAltitude);
				timeData.altitude = altitudeDiff;
				timeData.distance = distance - oldDistance;
				timeData.pulse = pulse;
				timeData.temperature = temperature;

				if (tpIndex == 0) {
					// first trackpoint
					timeData.time = 0;
				} else if (tpIndex == trackPoints.size() - 1) {
					// last track point
					timeData.time = tourRecordingTime - tourTime;
				} else {
					timeData.time = interval;
				}

				// set marker when a comment is set
				if (tpIndex > 0 && comment.length() > 0) {

					timeData.marker = 1;

					// create a new marker
					final TourMarker tourMarker = new TourMarker(tourData, ChartLabel.MARKER_TYPE_DEVICE);
					tourMarker.setLabel(comment);
					tourMarker.setVisualPosition(ChartLabel.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);
					tourMarker.setTime(timeData.time);
					tourMarker.setDistance(timeData.distance);
					tourMarker.setSerieIndex(tpIndex);

					tourData.getTourMarkers().add(tourMarker);
				}

				// first altitude contains the start altitude and not the difference
				if (tpIndex != 0) {
					tourAltUp += ((altitudeDiff > 0) ? altitudeDiff : 0);
					tourAltDown += ((timeData.altitude < 0) ? -timeData.altitude : 0);
				}

				oldDistance = distance;
				oldAltitude = altitude;

				// prepare next interval
				tourTime += interval;
				tpIndex++;

				sumDistance += timeData.distance;
				sumAltitude += Math.abs(altitude);
				sumPulse += pulse;
				sumTemperature += Math.abs(temperature);
			}

			fileReader.close();

			/*
			 * set the start distance, this is not available in a .crp file but it's required to
			 * create the tour-id
			 */
			tourData.setStartDistance(distance);

			// create unique tour id
			final Long tourId = tourData.createTourId(Integer.toString((int) Math.abs(tourData.getStartDistance())));

			// check if the tour is in the tour map
			if (alreadyImportedTours.containsKey(tourId) == false) {

				// add new tour to the map
				newlyImportedTours.put(tourId, tourData);

				/*
				 * disable data series when no data are available
				 */
				final TimeData firstTimeData = timeDataList.get(0);
				if (sumDistance == 0) {
					firstTimeData.distance = Float.MIN_VALUE;
				}
				if (sumAltitude == 0) {
					firstTimeData.altitude = Float.MIN_VALUE;
				}
				if (sumPulse == 0) {
					firstTimeData.pulse = Float.MIN_VALUE;
				}
				if (sumTemperature == 0) {
					firstTimeData.temperature = Float.MIN_VALUE;
				}

				// create additional data
				tourData.createTimeSeries(timeDataList, false);
				tourData.computeTourDrivingTime();
				tourData.computeComputedValues();

				tourData.setTourAltUp(tourAltUp);
				tourData.setTourAltDown(tourAltDown);

				tourData.setDeviceId(deviceId);
				tourData.setDeviceName(visibleName);
			}

			returnValue = true;

		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}

		return returnValue;
	}

	/**
	 * checks if the data file has a valid .crp data format
	 * 
	 * @return true for a valid .crp data format
	 */
	public boolean validateRawData(final String fileName) {

		BufferedReader fileReader = null;

		try {

			fileReader = new BufferedReader(new FileReader(fileName));

			final String fileHeader = fileReader.readLine();
			if (fileHeader == null) {
				return false;
			}

			if (fileHeader.startsWith("HRMProfilDatas") == false) { //$NON-NLS-1$
				return false;
			}

		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (fileReader != null) {
				try {
					fileReader.close();
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		return true;
	}

}
