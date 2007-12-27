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
package net.tourbook.device.daum.ergobike;

import java.io.BufferedReader;
import java.io.File;
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

	private static final String	DAUM_ERGO_BIKE_CSV_ID	= "Elapsed Time (s);Distance (km);Phys. kJoule;Slope (%);NM;RPM;Speed (km/h);Watt;Gear;Device Active;Pulse;Pulse Type;"; //$NON-NLS-1$

	private static final String	CSV_STRING_TOKEN		= ";"; //$NON-NLS-1$

	private Calendar			fCalendar				= GregorianCalendar.getInstance();

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

	public int getTransferDataSize() {
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

	public boolean processDeviceData(String importFilePath, DeviceData deviceData, HashMap<String, TourData> tourDataMap) {

		boolean returnValue = false;

		BufferedReader fileReader = null;

		try {

			fileReader = new BufferedReader(new FileReader(importFilePath));

			/*
			 * check if the file is from a Daum Ergometer
			 */
			String fileHeader = fileReader.readLine();
			if (fileHeader.startsWith(DAUM_ERGO_BIKE_CSV_ID) == false) {
				return false;
			}

			StringTokenizer tokenizer;

			/*
			 * extract data from the file name
			 */
			String fileName = new File(importFilePath).getName();

			//           1         2         3         4         5         6         7
			// 01234567890123456789012345678901234567890123456789012345678901234567890
			//
			// 0026  12_12_2007 20_35_02    1min    0_3km  Manuelles Training (Watt).csv
			// 0031  19_12_2007 19_11_37   35min   13_5km  Coaching - 003 - 2_5.csv
			// 0032  19_12_2007 19_46_44    1min    0_3km  Manuelles Training (Watt).cvs

			// start date
			int tourDay = Integer.parseInt(fileName.substring(6, 8));
			int tourMonth = Integer.parseInt(fileName.substring(9, 11));
			int tourYear = Integer.parseInt(fileName.substring(12, 16));

			// start time
			int tourHour = Integer.parseInt(fileName.substring(17, 19));
			int tourMin = Integer.parseInt(fileName.substring(20, 22));

			String title = fileName.substring(44);
			title = title.substring(0, title.length() - 4);

			/*
			 * set tour data
			 */
			TourData tourData = new TourData();

			tourData.setTourTitle(title);
			tourData.setTourDescription(importFilePath);

			tourData.setDeviceMode((short) 0);
			tourData.setDeviceTimeInterval((short) -1);

			tourData.setStartMinute((short) tourMin);
			tourData.setStartHour((short) tourHour);
			tourData.setStartDay((short) tourDay);
			tourData.setStartMonth((short) tourMonth);
			tourData.setStartYear((short) tourYear);

			tourData.importRawDataFile = importFilePath;

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
			boolean isFirstTime = true;

			String tokenLine;

			// read all data points
			while ((tokenLine = fileReader.readLine()) != null) {

				tokenizer = new StringTokenizer(tokenLine, CSV_STRING_TOKEN);

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

				if (isFirstTime) {
					isFirstTime = false;
					timeData.time = 0;
				} else {
					timeData.time = time - previousTime;
				}
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
				/*
				 * data are valid but have no data points
				 */
				return true;
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
			}

			returnValue = true;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (fileReader != null) {
					fileReader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
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

			if (fileHeader.startsWith(DAUM_ERGO_BIKE_CSV_ID) == false) {
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
