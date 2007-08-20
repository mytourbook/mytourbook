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
package net.tourbook.device.garmin305;

import gnu.io.SerialPort;

import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

public class Garmin305DeviceDataReader extends TourbookDevice {

	// plugin constructor
	public Garmin305DeviceDataReader() {}

	@Override
	public boolean checkStartSequence(int byteIndex, int newByte) {
		return true;
	}

	public String getDeviceModeName(int profileId) {
		return "";
	}

	public int getImportDataSize() {
		return 0x10000;
	}

	@Override
	public SerialParameters getPortParameters(String portName) {

		SerialParameters portParameters = new SerialParameters(portName,
				4800,
				SerialPort.FLOWCONTROL_NONE,
				SerialPort.FLOWCONTROL_NONE,
				SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE);

		return portParameters;
	}

	@Override
	public int getStartSequenceSize() {
		return 0;
	}

	public boolean processDeviceData(	String importFileName,
										DeviceData deviceData,
										HashMap<String, TourData> tourDataMap) {

		// create data object for each tour
		TourData tourData = new TourData();

		tourData.importRawDataFile = importFileName;

		tourData.setStartMinute((short) 0);
		tourData.setStartHour((short) 18);
		tourData.setStartDay((short) 20);
		tourData.setStartMonth((short) 8);
		tourData.setStartYear((short) 2007);
		tourData.setStartWeek((short) 33);

		short timeInterval = 20;
		tourData.setDeviceTimeInterval(timeInterval);

		// create a list which contains all time slices 
		ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();

		TimeData timeData;

		timeData = new TimeData();
		timeData.pulse = 140;
		timeData.altitude = 440;
		timeData.distance = 0;
		timeData.temperature = 20;
		timeDataList.add(timeData);

		timeData = new TimeData();
		timeData.pulse = 150;
		timeData.altitude = -10;
		timeData.distance = 50;
		timeData.temperature = 20;
		timeDataList.add(timeData);

		timeData = new TimeData();
		timeData.pulse = 150;
		timeData.altitude = 0;
		timeData.distance = 30;
		timeData.temperature = 21;
		timeDataList.add(timeData);

		timeData = new TimeData();
		timeData.pulse = 160;
		timeData.altitude = 10;
		timeData.distance = 50;
		timeData.temperature = 22;
		timeDataList.add(timeData);

		timeData = new TimeData();
		timeData.pulse = 150;
		timeData.altitude = -20;
		timeData.distance = 80;
		timeData.temperature = 21;
		timeDataList.add(timeData);

		tourData.setTourAltUp(10);
		tourData.setTourAltDown(30);

		/*
		 * unique key is used to create the tour id, this key is combined with the tour start
		 * date/time to identify the tour in the database. The key can be the distance of the tour
		 * or other unique data
		 */
		String uniqueKey = "23221";

		// after all data are added, the tour id can be created
		tourData.createTourId(uniqueKey);

		// check if the tour is already imported
		final String tourId = tourData.getTourId().toString();
		if (tourDataMap.containsKey(tourId) == false) {

			// add new tour to other tours
			tourDataMap.put(tourId, tourData);

			tourData.createTimeSeries(timeDataList, true);
			tourData.computeTourDrivingTime();
			tourData.computeAvgFields();

			tourData.setDeviceId(deviceId);
			tourData.setDeviceName(visibleName);

		}

		// set the import date or transfer date
		deviceData.transferYear = 2008;
		deviceData.transferMonth = 8;
		deviceData.transferDay = 30;

		return true;
	}

	public boolean validateRawData(String fileName) {
		return true;
	}

}
