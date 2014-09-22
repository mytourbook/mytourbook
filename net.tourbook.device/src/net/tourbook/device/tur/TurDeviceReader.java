/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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
import java.util.HashMap;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourType;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.ChartLabel;

/**
 * @author stm
 */
public class TurDeviceReader extends TourbookDevice {

	private final int	MAX_INT	= 0x10000;

	/**
	 * Plugin constructor
	 */
	public TurDeviceReader() {}

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {
		return null;
	}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
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

	public String getDeviceModeName(final int modeId) {
		return UI.EMPTY_STRING;
	}

	@Override
	public SerialParameters getPortParameters(final String portName) {
		// we don't have a device but a file
		return null;
	}

	@Override
	public int getStartSequenceSize() {
		return 16;
	}

	private TourType getTourType() {
		return null;
	}

	public int getTransferDataSize() {
		// We dont't have a com-port device so this is not neccessary
		return 0;
	}

	@Override
	public boolean processDeviceData(	final String importFilePath,
										final DeviceData deviceData,
										final HashMap<Long, TourData> alreadyImportedTours,
										final HashMap<Long, TourData> newlyImportedTours) {

		FileInputStream fileTurData = null;
		final TurDeviceData turDeviceData = new TurDeviceData();

		final TourType defaultTourType = getTourType();

		try {

			fileTurData = new FileInputStream(importFilePath);

			turDeviceData.readFromFile(fileTurData);

			final TourData tourData = new TourData();

			tourData.importRawDataFile = importFilePath;
			tourData.setTourImportFilePath(importFilePath);

			tourData.setDeviceMode(Short.parseShort(turDeviceData.deviceMode));
//			tourData.setDeviceTotalDown(Integer.parseInt(turDeviceData.deviceAltDown));
//			tourData.setDeviceTotalUp(Integer.parseInt(turDeviceData.deviceAltUp));
//			tourData.setDeviceTravelTime(Long.parseLong(turDeviceData.deviceTime));
//			tourData.setDeviceWeight(Integer.parseInt(turDeviceData.bikeWeight));

			final String tourDate = turDeviceData.tourStartDate;
			final String tourTime = turDeviceData.tourStartTime;

			tourData.setTourStartTime(
					Short.parseShort(tourDate.substring(6)),
					Short.parseShort(tourDate.substring(3, 5)),
					Short.parseShort(tourDate.substring(0, 2)),
					Short.parseShort(tourTime.substring(0, 2)),
					Short.parseShort(tourTime.substring(3, 5)),
					0);

			tourData.setStartDistance(Integer.parseInt(turDeviceData.deviceDistance));
			tourData.setTourDescription(turDeviceData.tourDescription);
			tourData.setTourTitle(turDeviceData.tourTitle);
			tourData.setTourStartPlace(turDeviceData.tourStartPlace);
			tourData.setTourEndPlace(turDeviceData.tourEndPlace);
//			tourData.setTourAltUp(Integer.parseInt(turDeviceData.tourAltUp));

			final int entryCount = Integer.parseInt(TurFileUtil.readText(fileTurData));

			int secStart1 = 0;
			int secStart2 = 0;
			int secStart3 = 0;
			int secStart4 = 0;

			int oldAltitude = 0;
			int oldDistance = 0;

			int sumDistance = 0;
			int sumAltitude = 0;
			int sumCadence = 0;
			int sumPulse = 0;
			int sumTemperature = 0;

			final ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();

			for (int dataIndex = 0; dataIndex < entryCount; dataIndex++) {

				if (Integer.parseInt(turDeviceData.fileVersion.substring(0, 1)) >= 3) {
					secStart1 = TurFileUtil.readByte(fileTurData); // Byte 1
					secStart2 = TurFileUtil.readByte(fileTurData); // Byte 2
					secStart3 = TurFileUtil.readByte(fileTurData); // Byte 3
					secStart4 = TurFileUtil.readByte(fileTurData); // Byte 4
				}

				final int sec1 = TurFileUtil.readByte(fileTurData); // Byte 5
				final int sec2 = TurFileUtil.readByte(fileTurData); // Byte 6
				final int sec3 = TurFileUtil.readByte(fileTurData); // Byte 7
				final int sec4 = TurFileUtil.readByte(fileTurData); // Byte 8

				final int dst1 = TurFileUtil.readByte(fileTurData); // Byte 9
				final int dst2 = TurFileUtil.readByte(fileTurData); // Byte 10
				final int dst3 = TurFileUtil.readByte(fileTurData); // Byte 11
				final int dst4 = TurFileUtil.readByte(fileTurData); // Byte 12

				final int hm1 = TurFileUtil.readByte(fileTurData); // Byte 13
				final int hm2 = TurFileUtil.readByte(fileTurData); // Byte 14

				final int pulse = TurFileUtil.readByte(fileTurData); // Byte 15
				final int cadence = TurFileUtil.readByte(fileTurData); // Byte 16

				final int temperature = TurFileUtil.readByte(fileTurData); // Byte 17

				// Read last 3 Byte of binary data (not used)
				TurFileUtil.readByte(fileTurData);
				TurFileUtil.readByte(fileTurData);
				TurFileUtil.readByte(fileTurData);

				// Calculate values
				@SuppressWarnings("unused")
				final int secStart = secStart1
						+ (256 * secStart2)
						+ (256 * 256 * secStart3)
						+ (256 * 256 * 256 * secStart4);
				final int seconds = sec1 + (256 * sec2) + (256 * 256 * sec3) + (256 * 256 * 256 * sec4);
				int distance = dst1 + (256 * dst2) + (256 * 256 * dst3) + (256 * 256 * 256 * dst4);
				distance *= 10; // distance in 10m
				int altitude = hm1 + (256 * hm2);

				if (altitude > 6000) { // negative
					altitude = altitude - MAX_INT;
				}

				if (dataIndex == 0) {
					tourData.setStartAltitude((short) altitude);
					tourData.setStartPulse((short) pulse);
				}

				if (dataIndex == 1) {
					tourData.setDeviceTimeInterval((short) seconds);
				}

				final TimeData timeData = new TimeData();

				timeData.altitude = altitude - oldAltitude;
				oldAltitude = altitude;

				timeData.cadence = cadence;
				timeData.pulse = pulse;
				timeData.temperature = temperature;

				if (dataIndex == 0) {
					timeData.distance = 0;
				} else {
					timeData.time = tourData.getDeviceTimeInterval();

					timeData.distance = distance - oldDistance;
					oldDistance = distance;

					tourData.setTourAltUp(tourData.getTourAltUp() + ((timeData.altitude > 0) ? timeData.altitude : 0));
					tourData.setTourAltDown(tourData.getTourAltDown()
							+ ((timeData.altitude < 0) ? -timeData.altitude : 0));
				}
				timeDataList.add(timeData);

				sumDistance += timeData.distance;
				sumAltitude += Math.abs(altitude);
				sumCadence += cadence;
				sumPulse += pulse;
				sumTemperature += Math.abs(temperature);

//				if (i == entryCount - 1) {
//					// summarize the recording time
//					tourData.setTourRecordingTime(seconds);
//
//					// set distance
//					tourData.setTourDistance(distance);
//				}
			}

			/*
			 * disable data series when no data are available
			 */
			if (timeDataList.size() > 0) {

				final TimeData firstTimeData = timeDataList.get(0);
				
				if (sumDistance == 0) {
					firstTimeData.distance = Float.MIN_VALUE;
				}
				if (sumAltitude == 0) {
					firstTimeData.altitude = Float.MIN_VALUE;
				}
				if (sumCadence == 0) {
					firstTimeData.cadence = Float.MIN_VALUE;
				}
				if (sumPulse == 0) {
					firstTimeData.pulse = Float.MIN_VALUE;
				}
				if (sumTemperature == 0) {
					firstTimeData.temperature = Float.MIN_VALUE;
				}
			}

			tourData.setTourType(defaultTourType);

			tourData.setDeviceId(deviceId);
			tourData.setDeviceName(visibleName);

			tourData.createTimeSeries(timeDataList, true);

			// after all data are added, the tour id can be created
			final int tourDistance = (int) Math.abs(tourData.getStartDistance());
			final String uniqueId = createUniqueId_Legacy(tourData, tourDistance);
			final Long tourId = tourData.createTourId(uniqueId);

			// check if the tour is in the tour map
			if (alreadyImportedTours.containsKey(tourId) == false) {

				// add new tour to the map
				newlyImportedTours.put(tourId, tourData);

				// create additional data
				tourData.computeComputedValues();
				tourData.computeTourDrivingTime();

				processDeviceData_10_CreateMarker(tourData, fileTurData);
				tourData.finalizeTourMarkerWithRelativeTime();
			}

		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (final IOException e) {
			e.printStackTrace();
			return false;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (fileTurData != null) {
				try {
					fileTurData.close();
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		return true;
	}

	private void processDeviceData_10_CreateMarker(final TourData tourData, final FileInputStream fileTurData)
			throws IOException {

		// Read last 0A from binary block
		TurFileUtil.readByte(fileTurData);
		// Read Marker
		final int markerCount = Integer.parseInt(TurFileUtil.readText(fileTurData));

		final float[] distanceSerie = tourData.getMetricDistanceSerie();

		// create new markers
		for (int i = 0; i < markerCount; i++) {

			final TourMarker tourMarker = new TourMarker(tourData, ChartLabel.MARKER_TYPE_DEVICE);

			// the correct absolute time will be set later
			tourMarker.setTime(Integer.parseInt(TurFileUtil.readText(fileTurData)), Long.MIN_VALUE);

			String label = TurFileUtil.readText(fileTurData);
			label = label.substring(0, label.indexOf(';'));
			final int index = label.indexOf(", Type:"); //$NON-NLS-1$
			if (index > 0) {
				label = label.substring(0, index);
			} else if (index == 0) {
				label = Messages.TourData_Tour_Marker_unnamed;
			}
			tourMarker.setLabel(label);

			final int[] timeSerie = tourData.timeSerie;
			if (timeSerie != null && timeSerie.length > 0) {

				for (int j = 0; j < timeSerie.length; j++) {
					if (timeSerie[j] > tourMarker.getTime()) {
						if (distanceSerie != null) {
							tourMarker.setDistance(distanceSerie[j - 1]);
						}
						tourMarker.setSerieIndex(j - 1);
						break;
					}
				}
			}

			tourData.getTourMarkers().add(tourMarker);
		}
	}

	public boolean validateRawData(final String fileName) {

		boolean isValid = false;

		BufferedInputStream inStream = null;

		try {

			final byte[] buffer = new byte[17];

			final File dataFile = new File(fileName);
			inStream = new BufferedInputStream(new FileInputStream(dataFile));

			inStream.read(buffer);
			if (!"HACtronic - Tour".equalsIgnoreCase(new String(buffer, 0, 16))) { //$NON-NLS-1$
				return false;
			}

			isValid = true;

		} catch (final NumberFormatException nfe) {
			return false;
		} catch (final FileNotFoundException e) {
			return false;
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		return isValid;
	}

}
