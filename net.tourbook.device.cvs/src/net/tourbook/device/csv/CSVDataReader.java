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
package net.tourbook.device.csv;

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

public class CSVDataReader extends TourbookDevice {

	private static final String	DAUM_ERGO_BIKE_ID	= "Elapsed Time (s);Distance (km);Phys. kJoule;Slope (%);NM;RPM;Speed (km/h);Watt;Gear;Device Active;Pulse;Pulse Type;";

	private static final String	CVS_STRING_TOKEN	= ";";

	private Calendar			fCalendar			= GregorianCalendar.getInstance();

	// plugin constructor
	public CSVDataReader() {
		canReadFromDevice = false;
		canSelectMultipleFilesInImportDialog = true;
	}

	@Override
	public boolean checkStartSequence(int byteIndex, int newByte) {
		return false;
	}

	public String getDeviceModeName(int profileId) {
		return null;
	}

	public int getImportDataSize() {
		return -1;
	}

	@Override
	public SerialParameters getPortParameters(String portName) {
		return null;
	}

	@Override
	public int getStartSequenceSize() {
		return -1;
	}

	public boolean processDeviceData(String importFileName, DeviceData deviceData, HashMap<String, TourData> tourDataMap) {

		boolean returnValue = false;

		BufferedReader fileReader = null;

		try {

			fileReader = new BufferedReader(new FileReader(importFileName));

			/*
			 * check if the file is from a Daum Ergometer
			 */
			String fileHeader = fileReader.readLine();
			if (fileHeader.startsWith(DAUM_ERGO_BIKE_ID) == false) {
				return false;
			}

			/*
			 * line: date/time
			 */

// !!!		new File(importRawDataFile).getName()
//
//			tokenLine = new StringTokenizer(fileReader.readLine());
//
//			// start date
//			String tourStartDate = tokenLine.nextToken();
//			int tourYear = Integer.parseInt(tourStartDate.substring(6));
//			int tourMonth = Integer.parseInt(tourStartDate.substring(3, 5));
//			int tourDay = Integer.parseInt(tourStartDate.substring(0, 2));
//
//			// start time
//			String tourStartTime = tokenLine.nextToken();
//			int tourHour = Integer.parseInt(tourStartTime.substring(0, 2));
//			int tourMin = tourStartTime.length() > 5
//					? Integer.parseInt(tourStartTime.substring(3, 5))
//					: Integer.parseInt(tourStartTime.substring(3));
//
//			// recording time
//			String tourRecTimeSt = tokenLine.nextToken();
//			int tourRecordingTime = Integer.parseInt(tourRecTimeSt.substring(0, 2))
//					* 3600
//					+ Integer.parseInt(tourRecTimeSt.substring(3, 5))
//					* 60
//					+ Integer.parseInt(tourRecTimeSt.substring(6));
			/*
			 * set tour data
			 */
			TourData tourData = new TourData();

			tourData.setTourTitle(importFileName);
			tourData.setTourDescription(importFileName);

			tourData.setDeviceMode((short) 0);
			tourData.setDeviceTimeInterval((short) 1);

//			tourData.setStartMinute((short) tourMin);
//			tourData.setStartHour((short) tourHour);
//			tourData.setStartDay((short) tourDay);
//			tourData.setStartMonth((short) tourMonth);
//			tourData.setStartYear((short) tourYear);

			tourData.importRawDataFile = importFileName;

			// tourData.setStartDistance((int) DeviceReaderTools.get4ByteData(buffer, 8));
			// tourData.setStartAltitude((short) DeviceReaderTools.get2ByteData(buffer, 12));
			// tourData.setStartPulse((short) (buffer[14] & 0xff));

			StringTokenizer tokenizer;

			/*
			 * set time serie from the imported trackpoints
			 */
			ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();
			TimeData timeData;

			int time;
			int previousTime = 0;

			int distance = 0;
			int previousDistance = 0;

			int cadence;
			int pulse;
			int power;
			int speed;

			String tokenLine;

			// read all data points
			while ((tokenLine = fileReader.readLine()) != null) {

				tokenizer = new StringTokenizer(tokenLine, CVS_STRING_TOKEN);

				time = (short) Integer.parseInt(tokenizer.nextToken()); // 				1  Elapsed Time (s)
				distance = (int) (Float.parseFloat(tokenizer.nextToken()) * 1000); // 	2  Distance (km)
				tokenizer.nextToken(); // 												3  Phys. kJoule
				tokenizer.nextToken(); // 												4  Slope (%)
				tokenizer.nextToken(); // 												5  NM
				cadence = (int) Float.parseFloat(tokenizer.nextToken()); // 			6  RPM
				speed = (int) Float.parseFloat(tokenizer.nextToken()); // 				7  Speed (km/h)
				power = Integer.parseInt(tokenizer.nextToken()); //						8  Watt
				tokenizer.nextToken(); // 												9  Gear
				tokenizer.nextToken(); // 												10 Device Active
				pulse = Integer.parseInt(tokenizer.nextToken()); // 					11 Pulse

				timeDataList.add(timeData = new TimeData());

				timeData.time = time - previousTime;
				timeData.distance = distance - previousDistance;
				timeData.cadence = cadence;
				timeData.pulse = pulse;
				timeData.power = power;
				timeData.speed = speed;

				// prepare next data point
				previousTime = time;
				previousDistance = distance;
			}

			fileReader.close();

			if (timeDataList.size() == 0) {
				return false;
			}
			/*
			 * set the start distance, this is not available in a .crp file but it's required to
			 * create the tour-id
			 */
			tourData.setStartDistance(distance);

			// create unique tour id
			tourData.createTourId(Integer.toString(Math.abs(tourData.getStartDistance())));

			// check if the tour is in the tour map
			final String tourId = tourData.getTourId().toString();
			if (tourDataMap.containsKey(tourId) == false) {

				// add new tour to the map
				tourDataMap.put(tourId, tourData);

				// create additional data
				tourData.createTimeSeries(timeDataList, false);
				tourData.computeTourDrivingTime();
				tourData.computeAvgFields();

				tourData.setDeviceId(deviceId);
				tourData.setDeviceName(visibleName);

				// set week of year
				fCalendar.set(tourData.getStartYear(), tourData.getStartMonth() - 1, tourData.getStartDay());
				tourData.setStartWeek((short) fCalendar.get(Calendar.WEEK_OF_YEAR));

				returnValue = true;
			}

		}
		catch (Exception e) {
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
			if (fileHeader == null) {
				return false;
			}

			if (fileHeader.startsWith(DAUM_ERGO_BIKE_ID) == false) {
				return false;
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (fileReader != null) {
				try {
					fileReader.close();
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		return true;
	}

}
