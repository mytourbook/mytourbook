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
 ********************************************************************************
 *
 * @author Alfred Barten
 *
 ********************************************************************************

This class implements reading of Garmin FIT activity files.
Up to now (12/30/2009) the file format isn't publicly documented by Garmin.
The following structure of single trackpoints in this file format is derived by Edge 500 examples.
All other contents are ignored!

Version since Edge 500 V 1.54:

data field  type offset description
------------------------------------------------------------
type        char    0   const 0x05
timestamp   long    1   # sec. since 31.12.1989 0 h UTC 
latitude    long    5   latitude  = 180 * [value] / LONG_MAX
longitude   long    9   longitude = 180 * [value] / LONG_MAX
distance    long   13   in cm
unknown1    long   17   const 0xffffff7f
elevation   short  21   elevation = [value] / 5 - 500
speed       short  23   in mm/s
unknown2    long   25   const 0xffffff7f
heartrate   char   29   in 1/min
cadence     char   30   in 1/min or 0xFF for unused
unknown3    char   31   const 0xff
temperature char   32   in °C

All data fields are *little* endian!!!
One of the unknown data fields will be power/wattage.
LONG_MAX = 2^31-1

Example of record of type 0x05:
0  0  0  0  0  0  0  0  0  0  1  1  1  1  1  1  1  1  1  1  2  2  2  2  2  2  2  2  2  2  3  3  3
0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5  6  7  8  9  0  1  2

Version >= Edge 500 V.1.54
t.|timestamp  |latitude   |longitude  |distance   |unknown1   |elev.|speed|unknown2   |hr|cd|u3|t.|
05 36 d2 29 25 14 08 1a 24 3b ff 42 04 62 b9 03 00 ff ff ff 7f 96 0d 81 11 ff ff ff 7f 86 4b ff 0e
05 38 d2 29 25 ea 06 1a 24 a2 f9 42 04 b0 bc 03 00 ff ff ff 7f 99 0d cd 11 ff ff ff 7f 87 4b ff 0e
05 3e d2 29 25 8d 04 1a 24 85 e8 42 04 63 c7 03 00 ff ff ff 7f a1 0d 0b 11 ff ff ff 7f 88 49 ff 0e
05 3f d2 29 25 1a 04 1a 24 a8 e5 42 04 0f c9 03 00 ff ff ff 7f a2 0d 1d 11 ff ff ff 7f 88 49 ff 0e

Version <= 1.53
t.|timestamp  |latitude   |longitude  |distance   |unknown1   |elev.|speed|unk.2|hr|cd|u3|t.|
05 36 d2 29 25 14 08 1a 24 3b ff 42 04 62 b9 03 00 ff ff ff 7f 96 0d 81 11 ff ff 86 4b ff 0e
05 38 d2 29 25 ea 06 1a 24 a2 f9 42 04 b0 bc 03 00 ff ff ff 7f 99 0d cd 11 ff ff 87 4b ff 0e
05 3e d2 29 25 8d 04 1a 24 85 e8 42 04 63 c7 03 00 ff ff ff 7f a1 0d 0b 11 ff ff 88 49 ff 0e
05 3f d2 29 25 1a 04 1a 24 a8 e5 42 04 0f c9 03 00 ff ff ff 7f a2 0d 1d 11 ff ff 88 49 ff 0e 

Version <= 1.46
t.|timestamp  |latitude   |longitude  |distance   |unknown1   |elev.|speed|unk.2|hr|cd|u3|t.|
04 92 88 da 24 40 32 1a 24 34 e6 4d 04 f4 27 05 00 ff ff ff 7f ea 0d 6f 24 ff ff 85 ff ff 15 
04 96 88 da 24 29 33 1a 24 0c fd 4d 04 db 40 05 00 ff ff ff 7f e7 0d f1 22 ff ff 82 ff ff 15 
04 97 88 da 24 7d 33 1a 24 a0 02 4e 04 42 44 05 00 ff ff ff 7f e6 0d 5a 22 ff ff 81 ff ff 15 
04 9b 88 da 24 0f 32 1a 24 76 1d 4e 04 82 47 05 00 ff ff ff 7f e6 0d d6 23 ff ff 7e ff ff 15 

 *******************************************************************************/
 

package net.tourbook.device.garmin.fit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

//tourData.createTourId
//tourData.computeComputedValues
//tourData.computeTourDrivingTime
//tourData.createTimeSeries
//tourData.getTourMarkers
//tourData.importRawDataFile
//tourData.offsetDDRecord
//tourData.setDeviceId
//tourData.setDeviceMode
//tourData.setDeviceModeName
//tourData.setDeviceName
//tourData.setDeviceTimeInterval
//tourData.setDeviceTotalDown
//tourData.setDeviceTotalUp
//tourData.setDeviceTourType
//tourData.setDeviceTravelTime
//tourData.setDeviceWeight
//tourData.setDeviceWheel
//tourData.setStartAltitude
//tourData.setStartDay
//tourData.setStartDistance
//tourData.setStartHour
//tourData.setStartMinute
//tourData.setStartMonth
//tourData.setStartPulse
//tourData.setStartWeek
//tourData.setStartYear
//tourData.setTourAltDown
//tourData.setTourAltUp
//tourData.setTourDescription
//tourData.setTourDistance
//tourData.setTourDrivingTime
//tourData.setTourImportFilePath
//tourData.setTourRecordingTime
//tourData.setTourTags
//tourData.setTourTitle
//tourData.setTourType

public class FitDataReader extends TourbookDevice {
	
	private int distance = 0;
	private int oldDistance = 0;
	private int oldAltitude = 0;
	private int oldTime = 0;
	private int tourAltUp = 0;
	private int tourAltDown = 0;
	private boolean pulseExists = false;
	private boolean cadenceExists = false;
	private Calendar fCalendar = GregorianCalendar.getInstance();

	public FitDataReader() {
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
	
	private int getLong(char[]a, int i) {
		return 256 * (256 * (256 * (int) a[i+3] + (int) a[i+2]) + (int) a[i+1]) + (int) a[i]; 
	}

	private int getShort(char[]a, int i) {
		return 256 * (int) a[i+1] + (int) a[i]; 
	}

	private int getChar(char[]a, int i) {
		return (int) a[i]; 
	}

	private TimeData getTimeData(char[]a, int timeOff, int latOff, int lonOff, int distOff, int altitudeOff, int speedOff, int pulseOff, int cadenceOff, int temperatureOff) {
		      
		final int LONG_MAX = 2147483647;
		// int deltaTime = 7304 * 86400; // # sec 1/1/1970 (Unix Epoch) - 12/31/1989 (Garmin Epoch)

		int time = getLong(a, timeOff);
		// time += deltaTime;

		int lat = getLong(a, latOff);
		if (lat == LONG_MAX)
			return null;
		double latD = 180. * lat / LONG_MAX;
		if (latD > 90. || latD < -90.)
			return null;

		int lon = getLong(a, lonOff);
		if (lon == LONG_MAX)
			return null;

		double lonD = 180. * lon / LONG_MAX;

		distance = getLong(a, distOff);
		distance /= 100;

		int altitude = getShort(a, altitudeOff);
		double altitudeD = altitude;
		altitudeD = altitudeD / 5 - 500;
		altitude = (int) altitudeD;
		final short altitudeDiff = (short)(altitude - oldAltitude);

		int speed = getShort(a, speedOff);
		double speedD = 3.6 * speed / 100; // [0.1 km/h]

		int pulse = getChar(a, pulseOff);
		if (pulse == 0xFF)
			pulse = 0;
		else
			pulseExists = true;
		
		int cadence = getChar(a, cadenceOff);
		if (cadence == 0xFF) 
			cadence = 0;
		else
			cadenceExists = true;

		int temperature = getChar(a, temperatureOff);
		if (temperature > 128) 
			temperature -= 256;

		TimeData timeData = new TimeData();		      
		timeData.latitude = latD;
		timeData.longitude = lonD;
		timeData.altitude = altitudeDiff;
		timeData.cadence = cadence;
		timeData.distance = distance - oldDistance;
		timeData.pulse = pulse;
		timeData.temperature = temperature;
		timeData.speed = (int)speedD;
		
//		System.out.println("timeData.latitude " + timeData.latitude);
//		System.out.println("timeData.longitude " + timeData.longitude);
//		System.out.println("timeData.altitude " + timeData.altitude + " " + altitude);
//		System.out.println("timeData.cadence " + timeData.cadence);
//		System.out.println("timeData.distance " + timeData.distance);
//		System.out.println("timeData.temperature " + timeData.temperature);
//		System.out.println("timeData.pulse " + timeData.pulse);
//		System.out.println("timeData.speed " + timeData.speed);
		
		if (oldTime == 0) { // first trackpoint
			// int deltaTime = 7304 * 86400; // # sec 1/1/1970 (Unix Epoch) - 12/31/1989 (Garmin Epoch)
			timeData.time = 0;
			
			long calendarTime = time;
			// calendarTime += deltaTime; is right, but doesn't work!
			calendarTime *= 1000;			
			fCalendar.setTimeInMillis(calendarTime);
		}
		else
		{
			timeData.time = time - oldTime;
			// first altitude contains the start altitude and not the difference
			tourAltUp += ((altitudeDiff > 0) ? altitudeDiff : 0);
			tourAltDown += ((timeData.altitude < 0) ? -timeData.altitude : 0);
		}

		oldDistance = distance;
		oldAltitude = altitude;
		oldTime = time;
		
		return timeData;
	}

	public boolean processDeviceData(final String importFileName, final DeviceData deviceData, final HashMap<Long, TourData> tourDataMap) {

		boolean returnValue = false;

		BufferedReader bufferedReader = null;

		try {
			if (validateRawData(importFileName) == false)
				return false;
			
			final int BUFSIZE = 33;
		      char[] a = new char[BUFSIZE];
		      for (int i = 0; i < BUFSIZE; i++)
		         a[i] = 0;			
			
	        FileInputStream fileInputStream = new FileInputStream(importFileName);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "ISO-8859-1"); 
            bufferedReader = new BufferedReader(inputStreamReader);
			
			TimeData timeData = null;
			final ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();			
			
			do {
                int ir = bufferedReader.read();
                if (ir == -1) break;
	            
	            for (int j = 0; j < BUFSIZE-1; j++)
	               a[j] = a[j+1];
	            a[BUFSIZE-1] = (char) ir;
	            
	            timeData = null;
	            if (  a[ 0] == 0x05 
	               && a[17] == 0xFF 
	               && a[18] == 0xFF
	               && a[19] == 0xFF
	               && a[20] == 0x7F
	               && a[25] == 0xFF 
	               && a[26] == 0xFF
	               && a[27] == 0xFF
	               && a[28] == 0x7F
	               && a[31] == 0xFF                  
	               ) {
	               // Version >= 1.54
	               timeData = getTimeData(a, 1, 5, 9, 13, 21, 23, 29, 30, 32);
	            } else 
	            if (  a[ 0] == 0x05 
	               && a[17] == 0xFF 
	               && a[18] == 0xFF
	               && a[19] == 0xFF
	               && a[20] == 0x7F
	               && a[25] == 0xFF 
	               && a[26] == 0xFF
	               && a[29] == 0xFF                  
	               ) {
	               // Version <= 1.53
	               timeData = getTimeData(a, 1, 5, 9, 13, 21, 23, 27, 28, 30);
	            } else 
	            if (  a[ 0] == 0x04 
	               && a[17] == 0xFF 
	               && a[18] == 0xFF
	               && a[19] == 0xFF
	               && a[20] == 0x7F
	               && a[25] == 0xFF 
	               && a[26] == 0xFF
	               && a[29] == 0xFF                  
	               && a[30] != 0xFF // temperature                  
	               ) {
	               // Version <= 1.46
	               timeData = getTimeData(a, 1, 5, 9, 13, 21, 23, 27, 28, 30);
	            }
	               
	            if (timeData != null)
	            	timeDataList.add(timeData);
	         }
			while (true);
	         bufferedReader.close();

			// set tour data
			final TourData tourData = new TourData();

			String tourTitle = importFileName.substring(importFileName.lastIndexOf(File.separator)+1);
			tourData.setTourTitle(tourTitle);
			tourData.setTourDescription(tourTitle);

			// tourData.setDeviceMode((short)(tourMode));
			// tourData.setDeviceModeName(getDeviceModeName(tourMode));
			// tourData.setDeviceTimeInterval((short)interval);

			tourData.setStartMinute((short) fCalendar.get(Calendar.MINUTE));
			tourData.setStartHour((short) fCalendar.get(Calendar.HOUR_OF_DAY));
			tourData.setStartDay((short) fCalendar.get(Calendar.DAY_OF_MONTH));
			tourData.setStartMonth((short) (fCalendar.get(Calendar.MONTH)+1));
			tourData.setStartYear((short) fCalendar.get(Calendar.YEAR));

			tourData.importRawDataFile = importFileName;
			tourData.setTourImportFilePath(importFileName);
			
			// set the start distance, this is not available in a .fit file but it's required to create the tour-id
			tourData.setStartDistance(distance);

			// create unique tour id
			final Long tourId = tourData.createTourId(Integer.toString(Math.abs(tourData.getStartDistance())));

			// check if the tour is in the tour map
			if (tourDataMap.containsKey(tourId) == false) {

				// add new tour to the map
				tourDataMap.put(tourId, tourData);

				// disable data series when no data are available
				final TimeData firstTimeData = timeDataList.get(0);
				if (!pulseExists)
					firstTimeData.pulse = Integer.MIN_VALUE;
				if (!cadenceExists)
					firstTimeData.temperature = Integer.MIN_VALUE;

				// create additional data
				tourData.createTimeSeries(timeDataList, false);
				tourData.computeTourDrivingTime();
				tourData.computeComputedValues();

				tourData.setTourAltUp(tourAltUp);
				tourData.setTourAltDown(tourAltDown);

				tourData.setDeviceId(deviceId);
				tourData.setDeviceName(visibleName);

				// set week of year
				fCalendar.set(tourData.getStartYear(), tourData.getStartMonth() - 1, tourData.getStartDay());
				tourData.setStartWeek((short) fCalendar.get(Calendar.WEEK_OF_YEAR));
			}

			returnValue = true;

		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}

		return returnValue;
	}


	/**
	 * checks if the data file has a valid .fit data format
	 * 
	 * @return true for a valid .fit data format
	 */
	public boolean validateRawData(final String fileName) {
				
		BufferedReader fileReader = null;

		try {

			fileReader = new BufferedReader(new FileReader(fileName));
			char[] buf = new char[12];
			int nChar = fileReader.read(buf, 0, 12);
			if (nChar < 12)
				return false;
			
			String fileType = "" + buf[9] + buf[10] + buf[11];
			
			System.out.println("FitDataReader: fileType = " + fileType);				

			if (!fileType.equals("FIT")) {
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

