/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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

/**
 * @author Markus Stipp
 */

package net.tourbook.device.tur;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import net.tourbook.chart.ChartMarker;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourType;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

/**
 * @author stm
 */
public class TurDeviceReader extends TourbookDevice {

	private final int	MAX_INT		= 0x10000;

	private Calendar	fCalendar	= GregorianCalendar.getInstance();

	/**
	 * 
	 */
	public TurDeviceReader() {
		canReadFromDevice = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.tourbook.importdata.TourbookDevice#checkStartSequence(int, int)
	 */
	@Override
	public boolean checkStartSequence(int byteIndex, int newByte) {
		/*
		 * check if the first 4 bytes are set to AFRO
		 */
		if (byteIndex == 0 & newByte == 'H') {
			return true;
		}
		if (byteIndex == 1 & newByte == 'A') {
			return true;
		}
		if (byteIndex == 2 & newByte == 'C') {
			return true;
		}
		if (byteIndex == 3 & newByte == 't') {
			return true;
		}
		if (byteIndex == 4 & newByte == 'r') {
			return true;
		}
		if (byteIndex == 5 & newByte == 'o') {
			return true;
		}
		if (byteIndex == 6 & newByte == 'n') {
			return true;
		}
		if (byteIndex == 7 & newByte == 'i') {
			return true;
		}
		if (byteIndex == 8 & newByte == 'c') {
			return true;
		}
		if (byteIndex == 9 & newByte == ' ') {
			return true;
		}
		if (byteIndex == 10 & newByte == '-') {
			return true;
		}
		if (byteIndex == 11 & newByte == ' ') {
			return true;
		}
		if (byteIndex == 12 & newByte == 'T') {
			return true;
		}
		if (byteIndex == 13 & newByte == 'o') {
			return true;
		}
		if (byteIndex == 14 & newByte == 'u') {
			return true;
		}
		if (byteIndex == 15 & newByte == 'r') {
			return true;
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.tourbook.importdata.TourbookDevice#getPortParameters(java.lang.String)
	 */
	@Override
	public SerialParameters getPortParameters(String portName) {
		// we don't have a device but a file
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.tourbook.importdata.TourbookDevice#getStartSequenceSize()
	 */
	@Override
	public int getStartSequenceSize() {
		return 16;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.tourbook.importdata.IRawDataReader#getDeviceModeName(int)
	 */
	public String getDeviceModeName(int modeId) {
		return ""; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.tourbook.importdata.IRawDataReader#getImportDataSize()
	 */
	public int getImportDataSize() {
		// We dont't have a com-port device so this is not neccessary
		return 0;
	}

	private TourType getTourType() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.tourbook.importdata.IRawDataReader#processDeviceData(java.lang.String,
	 *      net.tourbook.importdata.DeviceData, java.util.ArrayList)
	 */
	public boolean processDeviceData(	String importFileName,
										DeviceData deviceData,
										HashMap<String, TourData> tourDataMap) {

		FileInputStream fileTurData = null;
		TurDeviceData turDeviceData = new TurDeviceData();

		TourType defaultTourType = getTourType();

		try {
			fileTurData = new FileInputStream(importFileName);

			turDeviceData.readFromFile(fileTurData);

			TourData tourData = new TourData();
			tourData.importRawDataFile = importFileName;

			tourData.setDeviceMode(Short.parseShort(turDeviceData.deviceMode));
			tourData.setDeviceTotalDown(Integer.parseInt(turDeviceData.deviceAltDown));
			tourData.setDeviceTotalUp(Integer.parseInt(turDeviceData.deviceAltUp));
			tourData.setDeviceTravelTime(Long.parseLong(turDeviceData.deviceTime));
			tourData.setDeviceWeight(Integer.parseInt(turDeviceData.bikeWeight));

			tourData.setStartHour(Short.parseShort(turDeviceData.tourStartTime.substring(0, 2)));
			tourData.setStartMinute(Short.parseShort(turDeviceData.tourStartTime.substring(3, 5)));

			tourData.setStartYear(Short.parseShort(turDeviceData.tourStartDate.substring(6)));
			tourData.setStartMonth(Short.parseShort(turDeviceData.tourStartDate.substring(3, 5)));
			tourData.setStartDay(Short.parseShort(turDeviceData.tourStartDate.substring(0, 2)));

			tourData.setStartDistance(Integer.parseInt(turDeviceData.deviceDistance));
			tourData.setTourDescription(turDeviceData.tourDescription);
			tourData.setTourTitle(turDeviceData.tourTitle);
			tourData.setTourStartPlace(turDeviceData.tourStartPlace);
			tourData.setTourEndPlace(turDeviceData.tourEndPlace);
//			tourData.setTourAltUp(Integer.parseInt(turDeviceData.tourAltUp));

			int entryCount = Integer.parseInt(TurFileUtil.readText(fileTurData));

//			int secStart1 = 0;
//			int secStart2 = 0;
//			int secStart3 = 0;
//			int secStart4 = 0;

			int oldAltimeter = 0;
			int oldDistance = 0;

			ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();

			for (int i = 0; i < entryCount; i++) {
				if (Integer.parseInt(turDeviceData.fileVersion.substring(0, 1)) >= 3) {
//					secStart1 = TurFileUtil.readByte(fileTurData); // Byte 1
//					secStart2 = TurFileUtil.readByte(fileTurData); // Byte 2
//					secStart3 = TurFileUtil.readByte(fileTurData); // Byte 3
//					secStart4 = TurFileUtil.readByte(fileTurData); // Byte 4
				}
				int sec1 = TurFileUtil.readByte(fileTurData); // Byte 5
				int sec2 = TurFileUtil.readByte(fileTurData); // Byte 6
				int sec3 = TurFileUtil.readByte(fileTurData); // Byte 7
				int sec4 = TurFileUtil.readByte(fileTurData); // Byte 8

				int dst1 = TurFileUtil.readByte(fileTurData); // Byte 9
				int dst2 = TurFileUtil.readByte(fileTurData); // Byte 10
				int dst3 = TurFileUtil.readByte(fileTurData); // Byte 11
				int dst4 = TurFileUtil.readByte(fileTurData); // Byte 12

				int hm1 = TurFileUtil.readByte(fileTurData); // Byte 13
				int hm2 = TurFileUtil.readByte(fileTurData); // Byte 14

				int puls = TurFileUtil.readByte(fileTurData); // Byte 15
				int cadence = TurFileUtil.readByte(fileTurData); // Byte 16

				int temp = TurFileUtil.readByte(fileTurData); // Byte 17

				// Read last 3 Byte of binary data (not used)
				TurFileUtil.readByte(fileTurData);
				TurFileUtil.readByte(fileTurData);
				TurFileUtil.readByte(fileTurData);

				// Calculate values
//				int secStart = secStart1
//						+ (256 * secStart2)
//						+ (256 * 256 * secStart3)
//						+ (256 * 256 * 256 * secStart4);
				int seconds = sec1 + (256 * sec2) + (256 * 256 * sec3) + (256 * 256 * 256 * sec4);
				int distance = dst1 + (256 * dst2) + (256 * 256 * dst3) + (256 * 256 * 256 * dst4);
				distance *= 10; // distance in 10m
				int altimeter = hm1 + (256 * hm2);

				if (altimeter > 6000) { // negative
					altimeter = altimeter - MAX_INT;
				}

				if (i == 0) {
					tourData.setStartAltitude((short) altimeter);
					tourData.setStartPulse((short) puls);
				}

				if (i == 1) {
					tourData.setDeviceTimeInterval((short) seconds);
				}

				TimeData timeData;
				timeData = new TimeData();

				timeData.altitude = (short) (altimeter - oldAltimeter);
				oldAltimeter = altimeter;
				timeData.cadence = (short) cadence;
				timeData.pulse = (short) puls;
				timeData.temperature = (short) temp;
				if (i != 0) {
					timeData.time = (short) tourData.getDeviceTimeInterval();
					timeData.distance = distance - oldDistance;
					oldDistance = distance;
					tourData.setTourAltUp(tourData.getTourAltUp()
							+ ((timeData.altitude > 0) ? timeData.altitude : 0));
					tourData.setTourAltDown(tourData.getTourAltDown()
							+ ((timeData.altitude < 0) ? -timeData.altitude : 0));
				}
				timeDataList.add(timeData);

//				if (i == entryCount - 1) {
//					// summarize the recording time
//					tourData.setTourRecordingTime(seconds);
//
//					// set distance
//					tourData.setTourDistance(distance);
//				}
			}

			// after all data are added, the tour id can be created
			tourData.createTourId(Integer.toString(Math.abs(tourData.getStartDistance())));

			// check if the tour is in the tour map
			final String tourId = tourData.getTourId().toString();
			if (tourDataMap.containsKey(tourId) == false) {

				// add new tour to the map
				tourDataMap.put(tourId, tourData);

				tourData.createTimeSeries(timeDataList, true);
				tourData.computeAvgFields();

				// Read last 0A from binary block
				TurFileUtil.readByte(fileTurData);
				// Read Marker
				int markerCount = Integer.parseInt(TurFileUtil.readText(fileTurData));

				// create new markers
				for (int i = 0; i < markerCount; i++) {
					TourMarker tourMarker = new TourMarker(tourData, ChartMarker.MARKER_TYPE_DEVICE);
					tourMarker.setTime(Integer.parseInt(TurFileUtil.readText(fileTurData)));
					String label = TurFileUtil.readText(fileTurData);
					label = label.substring(0, label.indexOf(';'));
					int index = label.indexOf(", Type:"); //$NON-NLS-1$
					if (index > 0) {
						label = label.substring(0, index);
					} else if (index == 0) {
						label = Messages.TourData_Tour_Marker_unnamed;
					}
					tourMarker.setLabel(label);
					tourMarker.setVisualPosition(ChartMarker.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);
					for (int j = 0; j < tourData.timeSerie.length; j++) {
						if (tourData.timeSerie[j] > tourMarker.getTime()) {
							tourMarker.setDistance(tourData.distanceSerie[j - 1]);
							tourMarker.setSerieIndex(j - 1);
							break;
						}
					}
					tourData.getTourMarkers().add(tourMarker);
				}
				tourData.setTourType(defaultTourType);
				tourData.computeTourDrivingTime();

				tourData.setDeviceId(deviceId);
				tourData.setDeviceName(visibleName);

				// set week of year
				fCalendar.set(tourData.getStartYear(),
						tourData.getStartMonth() - 1,
						tourData.getStartDay());
				tourData.setStartWeek((short) fCalendar.get(Calendar.WEEK_OF_YEAR));
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fileTurData != null) {
				try {
					fileTurData.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.tourbook.importdata.IRawDataReader#validateRawData(java.lang.String)
	 */
	public boolean validateRawData(String fileName) {

		boolean isValid = false;

		BufferedInputStream inStream = null;

		try {

			byte[] buffer = new byte[17];

			File dataFile = new File(fileName);
			inStream = new BufferedInputStream(new FileInputStream(dataFile));

			inStream.read(buffer);
			if (!"HACtronic - Tour".equalsIgnoreCase(new String(buffer, 0, 16))) { //$NON-NLS-1$
				return false;
			}

			isValid = true;

		} catch (NumberFormatException nfe) {
			return false;
		} catch (FileNotFoundException e) {
			return false;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		return isValid;
	}

}
