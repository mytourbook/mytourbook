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
package net.tourbook.device.crp;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.StringTokenizer;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

public class CRPDataReader extends TourbookDevice {

	private Calendar	fCalendar	= GregorianCalendar.getInstance();

	// plugin constructor
	public CRPDataReader() {
		canReadFromDevice = false;
		canSelectMultipleFilesInImportDialog = true;
	}

	public boolean checkStartSequence(int byteIndex, int newByte) {
		return false;
	}

	public String getDeviceModeName(int profileId) {
		return "";
	}

	public int getImportDataSize() {
		return -1;
	}

	public SerialParameters getPortParameters(String portName) {
		return null;
	}

	public int getStartSequenceSize() {
		return -1;
	}

	public boolean processDeviceData(	String importFileName,
										DeviceData deviceData,
										HashMap<String, TourData> tourDataMap) {
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

			fileReader = new BufferedReader(new FileReader(importFileName));

			String fileHeader = fileReader.readLine();
			if (fileHeader.startsWith("HRMProfilDatas") == false) {
				return false;
			}

			String line;
			StringTokenizer tokenLine;
			ArrayList<String> trackPoints = new ArrayList<String>();

			tokenLine = new StringTokenizer(fileReader.readLine());
			String fileVersion = tokenLine.nextToken();

			// get all trackpoints
			while ((line = fileReader.readLine()) != null) {
				if (line.equals("***")) {
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

			// start date
			String tourStartDate = tokenLine.nextToken();
			int tourYear = Integer.parseInt(tourStartDate.substring(6));
			int tourMonth = Integer.parseInt(tourStartDate.substring(3, 5));
			int tourDay = Integer.parseInt(tourStartDate.substring(0, 2));

			// start time
			String tourStartTime = tokenLine.nextToken();
			int tourHour = Integer.parseInt(tourStartTime.substring(0, 2));
			int tourMin = tourStartTime.length() > 5 ? Integer.parseInt(tourStartTime.substring(
					3,
					5)) : Integer.parseInt(tourStartTime.substring(3));

			// recording time
			String tourRecTimeSt = tokenLine.nextToken();
			int tourRecordingTime = Integer.parseInt(tourRecTimeSt.substring(0, 2)) * 3600
					+ Integer.parseInt(tourRecTimeSt.substring(3, 5))
					* 60
					+ Integer.parseInt(tourRecTimeSt.substring(6));

			// category
			tokenLine.nextToken();

			// difficulty
			tokenLine.nextToken();

			// tour name
			String tourName = "";
			if (tokenLine.hasMoreTokens()) {
				tourName = tokenLine.nextToken("\t");
			}

			// skip lines
			fileReader.readLine();
			fileReader.readLine();

			/*
			 * line: interval/mode
			 */
			tokenLine = new StringTokenizer(fileReader.readLine());
			int interval = Integer.parseInt(tokenLine.nextToken());
			int tourMode = Integer.parseInt(tokenLine.nextToken());

			// skip empty lines
			fileReader.readLine();
			fileReader.readLine();

			// skip lines
			fileReader.readLine();
			fileReader.readLine();

			/*
			 * lines: tour description
			 */
			String tourDesc = "";
			while ((line = fileReader.readLine()) != null) {
				tourDesc += line + "\n";
			}
			tourDesc = tourDesc.trim();

			/*
			 * set tour data
			 */
			TourData tourData = new TourData();

			tourData.setDeviceMode((short) (tourMode));
			tourData.setDeviceTimeInterval((short) interval);

			tourData.setStartMinute((short) tourMin);
			tourData.setStartHour((short) tourHour);
			tourData.setStartDay((short) tourDay);
			tourData.setStartMonth((short) tourMonth);
			tourData.setStartYear((short) tourYear);

			tourData.importRawDataFile = importFileName;

			// tourData.setStartDistance((int) DeviceReaderTools.get4ByteData(buffer, 8));
			// tourData.setStartAltitude((short) DeviceReaderTools.get2ByteData(buffer, 12));
			// tourData.setStartPulse((short) (buffer[14] & 0xff));

			/*
			 * set time serie from the imported trackpoints
			 */
			ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();
			TimeData timeData;

			int tpIndex = 0;
			int tourTime = 0;

			int pulse;
			int distance = 0;
			int speed;
			int altitude;
			int color;
			int symbol;
			int temperature;
			String trackpointTime;

			int oldDistance = 0;
			int oldAltitude = 0;
			int tourAltUp = 0;
			int tourAltDown = 0;

			for (String trackPoint : trackPoints) {

				tokenLine = new StringTokenizer(trackPoint);

				pulse = Integer.parseInt(tokenLine.nextToken());
				speed = Integer.parseInt(tokenLine.nextToken()); // [0.1 km/h]
				distance = Integer.parseInt(tokenLine.nextToken()) * 10; // [m]
				altitude = Integer.parseInt(tokenLine.nextToken()); // [m]
				color = Integer.parseInt(tokenLine.nextToken()); // [0..4]
				symbol = Integer.parseInt(tokenLine.nextToken()); // [0..42]
				temperature = Math.round(Float.parseFloat(tokenLine.nextToken().replace(',', '.'))); // [°C]
				trackpointTime = tokenLine.nextToken();

				// get comment for current trackpoint
				String comment = "";
				if (tokenLine.hasMoreTokens()) {
					comment = tokenLine.nextToken("\t");
				}

				timeDataList.add(timeData = new TimeData());

				final short altitudeDiff = (short) (altitude - oldAltitude);
				timeData.altitude = altitudeDiff;
				timeData.cadence = 0;
				timeData.distance = distance - oldDistance;
				timeData.pulse = (short) pulse;
				timeData.temperature = (short) temperature;

				if (tpIndex == trackPoints.size() - 1) {
					// last track point
					timeData.time = (short) (tourRecordingTime - tourTime);
				} else {
					timeData.time = (short) interval;
				}

				// set marker when a comment is set
				if (tpIndex > 0 && comment.length() > 0) {
					timeData.marker = 1;
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
			}

			fileReader.close();

			/*
			 * set the start distance, this is not available in a .crp file but it's required to
			 * create the tour-id
			 */
			tourData.setStartDistance(distance);

			// create unique tour id
			tourData.createTourId();

			// check if the tour is in the tour map
			final String tourId = tourData.getTourId().toString();
			if (tourDataMap.containsKey(tourId) == false) {

				// add new tour to the map
				tourDataMap.put(tourId, tourData);

				// create additional data
				tourData.setTourDistance(distance);
				tourData.createTimeSeries(timeDataList);
				tourData.computeTourDrivingTime();
				tourData.setTourRecordingTime(tourRecordingTime);

				tourData.setTourAltUp(tourAltUp);
				tourData.setTourAltDown(tourAltDown);

				tourData.setDeviceId(visibleName);

				// set week of year
				fCalendar.set(tourData.getStartYear(), tourData.getStartMonth() - 1, tourData
						.getStartDay());
				tourData.setStartWeek((short) fCalendar.get(Calendar.WEEK_OF_YEAR));

				returnValue = true;
			}

		} catch (Exception e) {
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
	public boolean validateRawData(String fileName) {

		BufferedReader fileReader = null;

		try {

			fileReader = new BufferedReader(new FileReader(fileName));

			String fileHeader = fileReader.readLine();
			if (fileHeader.startsWith("HRMProfilDatas") == false) {
				return false;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fileReader != null) {
				try {
					fileReader.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		return true;
	}

}
