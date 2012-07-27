/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm, Markus Stipp
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
package net.tourbook.device.cm4xxm;

/*
 * author & copyright: Markus Stipp
 */

import gnu.io.SerialPort;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.data.DataUtil;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.ui.UI;

public class CM4XXMDeviceReader extends TourbookDevice {

	private static final int	RECORD_SIZE			= 40;
	private static final short	CM4XXM_TIMESLICE	= 20;

	private static final int	OFFSET_DEVICE_DATA	= 645;
	private static final int	OFFSET_DATA_START	= 765;
	private static final int	OFFSET_LAST_RECORD	= 81920;
	private static final int	OFFSET_CHECKSUM_POS	= 81925;
	private static final int	CM4XXM_DATA_SIZE	= 81930;

	private static final int	HARDWARE_ID_CM4XXM	= 0xb723;

	private class StartBlock {
		public int	month;
		public int	day;
		public int	hour;
		public int	minute;
	}

	/**
	 * constructor is used when the plugin is loaded
	 */
	public CM4XXMDeviceReader() {}

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {
		RandomAccessFile fileRawData = null;

		final CM4XXMDeviceData cm4xxDeviceData = new CM4XXMDeviceData();

		try {

			fileRawData = new RandomAccessFile(rawDataFileName, "r"); //$NON-NLS-1$

			// position file pointer to the device data
			fileRawData.seek(OFFSET_DEVICE_DATA);

			// read device data
			cm4xxDeviceData.readFromFile(fileRawData);

		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (fileRawData != null) {
				try {
					fileRawData.close();
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		return String.format(
				net.tourbook.Messages.Format_rawdata_file_yyyy_mm_dd + fileExtension,
				cm4xxDeviceData.transferYear,
				cm4xxDeviceData.transferMonth,
				cm4xxDeviceData.transferDay);
	}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {

		/*
		 * check if the first 4 bytes are set to AFRO
		 */
		if (byteIndex == 0 & newByte == 'A') {
			return true;
		}
		if (byteIndex == 1 & newByte == 'F') {
			return true;
		}
		if (byteIndex == 2 & newByte == 'R') {
			return true;
		}
		if (byteIndex == 3 & newByte == 'O') {
			return true;
		}

		return false;
	}

	public String getDeviceModeName(final int profileId) {

		// "2E" bike2 (CM414M)
		// "3E" bike1 (CM414M)

		switch (profileId) {
		case 46: // 0x2E
			return Messages.CM4XXM_profile_bike2;

		case 62: // 0x3E
			return Messages.CM4XXM_profile_bike1;

		}

		return Messages.CM4XXM_profile_unknown;
	}

	@Override
	public SerialParameters getPortParameters(final String portName) {

		return new SerialParameters(
				portName,
				9600,
				SerialPort.FLOWCONTROL_NONE,
				SerialPort.FLOWCONTROL_NONE,
				SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE);
	}

	@Override
	public int getStartSequenceSize() {
		return 4;
	}

	private TourType getTourType() {
		return null;
	}

	public int getTransferDataSize() {
		return CM4XXM_DATA_SIZE;
	}

	@Override
	public boolean processDeviceData(	final String importFilePath,
										final DeviceData deviceData,
										final HashMap<Long, TourData> alreadyImportedTours,
										final HashMap<Long, TourData> newlyImportedTours) {

		RandomAccessFile fileRawData = null;

		final byte[] buffer = new byte[5];
		String recordType = UI.EMPTY_STRING;

		final CM4XXMDeviceData cm4xxmDeviceData = new CM4XXMDeviceData();

		// reset tour data list
//		tourDataMap.clear();

		final TourType defaultTourType = getTourType();

		try {

			fileRawData = new RandomAccessFile(importFilePath, "r"); //$NON-NLS-1$

			// position file pointer to the device data
			fileRawData.seek(OFFSET_DEVICE_DATA);

			// read device data
			cm4xxmDeviceData.readFromFile(fileRawData);

			/*
			 * because the tour year is not available we calculate it from the transfer year, this
			 * might be not correct but there is no other way to get the year
			 */
			int tourYear = cm4xxmDeviceData.transferYear;
			int lastTourMonth = 0;

			/*
			 * move file pointer to the DD record of the last tour and read "offset AA record" of
			 * the last tour and position file pointer there
			 */
			fileRawData.seek(cm4xxmDeviceData.offsetDDRecord + 5);
			int offsetAARecord = DataUtil.readFileOffset(fileRawData, buffer);
			final int initialOffsetAARecord = offsetAARecord;
			int offsetDDRecord;

			boolean isLastTour = true;

			// loop: read all tours
			while (true) {

				/*
				 * read AA record
				 */

				// read encoded data
				fileRawData.seek(offsetAARecord);
				fileRawData.read(buffer);

				// decode record type
				recordType = new String(buffer, 2, 2);

				// make sure we read a AA record
				if (!recordType.equalsIgnoreCase("AA")) { //$NON-NLS-1$
					break;
				}

				/*
				 * read tour data
				 */

				fileRawData.seek(offsetAARecord);

				final TourData tourData = new TourData();

				tourData.setDeviceTimeInterval(CM4XXM_TIMESLICE);
				tourData.importRawDataFile = importFilePath;
				tourData.setTourImportFilePath(importFilePath);

				final StartBlock startBlock = readStartBlock(fileRawData, tourData);

				/*
				 * add device data to the tour, the last tour is the first which is read from the
				 * data file
				 */
				if (isLastTour) {
					isLastTour = false;

//					final int deviceTravelTimeHours = (cm4xxmDeviceData.totalTravelTimeHour1);
//
//					tourData.setDeviceTravelTime((deviceTravelTimeHours * 3600)
//							+ (cm4xxmDeviceData.totalTravelTimeMin1 * 60)
//							+ cm4xxmDeviceData.totalTravelTimeSec1);
//
//					// tourData.deviceDistance = ((deviceData.totalDistanceHigh
//					// * (2 ^ 16)) + deviceData.totalDistanceLow);
//					tourData.setDeviceWheel(cm4xxmDeviceData.wheelPerimeter1);
//					tourData.setDeviceWeight(cm4xxmDeviceData.personWeight);
				}

				/*
				 * calculate year of the tour
				 */

				// set initial tour month if not yet done
				lastTourMonth = (lastTourMonth == 0) ? startBlock.month : lastTourMonth;

				/*
				 * because we read the tours in decending order (last tour first), we check if the
				 * month of the current tour is higher than from the last tour, if this is the case,
				 * we assume to have data from the previous year
				 */
				if (startBlock.month > lastTourMonth) {
					tourYear--;
				}
				lastTourMonth = startBlock.month;

				tourData.setTourStartTime(
						tourYear,
						startBlock.month,
						startBlock.day,
						startBlock.hour,
						startBlock.minute,
						0);

				// create time list
				final ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();

				short temperature;
				short marker;
				short cadence;

				// short totalPulse = tourData.getStartPulse();
				short absoluteAltitude = tourData.getStartAltitude();

				int iDataMax; // number of time slices in a BB record
				boolean isFirstBBRecord = true;
				boolean isFirstTime = true;

				int sumDistance = 0;
				int sumAltitude = 0;
				int sumCadence = 0;

				/*
				 * read all records of current tour
				 */
				while (!recordType.equalsIgnoreCase("CC")) { //$NON-NLS-1$

					// if we reached EOF, position file pointer at the beginning
					if (fileRawData.getFilePointer() == OFFSET_CHECKSUM_POS) {
						fileRawData.seek(OFFSET_DATA_START);
					}

					// System.out.println(fileRawData.getFilePointer());

					// read encoded data
					fileRawData.read(buffer);

					// decode record type
					recordType = new String(buffer, 2, 2);

					// decode temperature
					temperature = Short.parseShort(new String(buffer, 0, 2), 16);

					// read encoded data
					fileRawData.read(buffer);

					// decode marker
					marker = Short.parseShort(new String(buffer, 0, 2), 16);

					// decode cadence
					cadence = Short.parseShort(new String(buffer, 2, 2), 16);

					/*
					 * marker in CC record contains the exact time when the tour ends, so we will
					 * read only those time slices which contains tour data
					 */
					if (recordType.equalsIgnoreCase("CC")) { //$NON-NLS-1$

						iDataMax = (marker / 20) + 1;

						if (marker == 0) {
							break;
						}

						// make sure not to exceed the maximum
						if (iDataMax > 6) {
							iDataMax = 6;
						}

					} else {
						iDataMax = 6;
					}

					// loop: all 6 data records
					for (int iData = 0; iData < iDataMax; iData++) {

						TimeData timeData;

						if (isFirstBBRecord) {

							/*
							 * before we read the first BB record we have to create the time slice
							 * for the start
							 */

							isFirstBBRecord = false;

							// create first time slice
							timeData = new TimeData();
							timeDataList.add(timeData);

							timeData.distance = 0;
							timeData.altitude = tourData.getStartAltitude();
							timeData.temperature = temperature;
							timeData.cadence = cadence;
						}

						// add new time slice
						timeData = new TimeData();
						timeDataList.add(timeData);

						timeData.temperature = temperature;
						timeData.cadence = cadence;

						// set time/marker
						if (recordType.equalsIgnoreCase("BB")) { //$NON-NLS-1$

							// BB record

							// a marker is set only in the first time slice in a
							// record
							if (iData == 0) {
								timeData.marker = marker;
							}

							timeData.time = CM4XXM_TIMESLICE;

						} else {

							// CC record

							if (iData + 1 == iDataMax) {
								// this is the last time slice
								timeData.time = marker % CM4XXM_TIMESLICE;
							} else {
								// this is a normal time slice
								timeData.time = CM4XXM_TIMESLICE;
							}
						}
						if (isFirstTime) {
							// first data point
							isFirstTime = false;
							timeData.time = 0;
						}

						// read data for this time slice
						readTimeSlice(fileRawData, timeData);

						// we have no pulse data in CM4xxM
//						timeData.pulse = 0;

						// adjust altitude from relative to absolute
						absoluteAltitude += timeData.altitude;

						tourData.setTourAltUp(tourData.getTourAltUp()
								+ ((timeData.altitude > 0) ? timeData.altitude : 0));
						tourData.setTourAltDown(tourData.getTourAltDown()
								+ ((timeData.altitude < 0) ? -timeData.altitude : 0));

						sumDistance += timeData.distance;
						sumAltitude += Math.abs(absoluteAltitude);
						sumCadence += cadence;
					}
				}

				// after all data are added, the tour id can be created
				final Long tourId = tourData.createTourId(//
						Integer.toString((int) Math.abs(tourData.getStartDistance())));

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
					if (sumCadence == 0) {
						firstTimeData.cadence = Float.MIN_VALUE;
					}

					tourData.createTimeSeries(timeDataList, true);
					tourData.setTourType(defaultTourType);
					tourData.computeTourDrivingTime();
					tourData.computeComputedValues();

					tourData.setDeviceId(deviceId);
					tourData.setDeviceName(visibleName);

					final Short profileId = Short.valueOf(tourData.getDeviceTourType(), 16);
					tourData.setDeviceMode(profileId);
					tourData.setDeviceModeName(getDeviceModeName(profileId));
				}

				/*
				 * calculate the start of the previous tour, we have to make sure that we get the
				 * complete tour with all the records
				 */

				if (offsetAARecord == OFFSET_DATA_START) {
					// set position after the end of the tour data
					offsetAARecord = OFFSET_CHECKSUM_POS;
				}

				/*
				 * calculate DD Record of previous tour by starting from the AA record of the
				 * current tour
				 */
				offsetDDRecord = offsetAARecord - RECORD_SIZE;

				// make sure we do not advance before the tour data start
				// position
				if (offsetDDRecord < OFFSET_DATA_START) {
					// set position at the end
					offsetDDRecord = OFFSET_LAST_RECORD;
				}

				// make sure we do not hit the free memory area
				if (offsetDDRecord == cm4xxmDeviceData.offsetNextMemory) {
					break;
				}

				// read DD record
				fileRawData.seek(offsetDDRecord);

				// read encoded data
				fileRawData.read(buffer);

				// decode record type
				recordType = new String(buffer, 2, 2);

				// make sure we read a DD record
				if (!recordType.equalsIgnoreCase("DD")) { //$NON-NLS-1$
					break;
				}

				offsetAARecord = DataUtil.readFileOffset(fileRawData, buffer);

				/*
				 * make sure to end not in an endless loop where the current AA offset is the same
				 * as the first AA offset (this seems to be unlikely but it happend already 2 Month
				 * after the first implementation)
				 */
				if (offsetAARecord == initialOffsetAARecord) {
					break;
				}
			}
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (fileRawData != null) {
				try {
					fileRawData.close();
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		deviceData.transferYear = cm4xxmDeviceData.transferYear;
		deviceData.transferMonth = cm4xxmDeviceData.transferMonth;
		deviceData.transferDay = cm4xxmDeviceData.transferDay;

		return true;
	}

	private StartBlock readStartBlock(final RandomAccessFile file, final TourData tourData) throws IOException {

		final StartBlock startBlock = new StartBlock();

		final byte[] buffer = new byte[5];

		file.read(buffer);
		tourData.setDeviceTourType(new String(buffer, 0, 2));

		tourData.offsetDDRecord = DataUtil.readFileOffset(file, buffer);

		file.read(buffer);
		startBlock.hour = Short.parseShort(new String(buffer, 0, 2));
		startBlock.minute = Short.parseShort(new String(buffer, 2, 2));

		file.read(buffer);
		startBlock.month = Short.parseShort(new String(buffer, 0, 2));
		startBlock.day = Short.parseShort(new String(buffer, 2, 2));

		file.read(buffer);
		tourData.setStartDistance(Integer.parseInt(new String(buffer, 0, 4), 16));

		file.read(buffer);
		// tourData.setDistance(Integer.parseInt(new String(buffer, 0, 4), 16));

		file.read(buffer);
		tourData.setStartAltitude((short) Integer.parseInt(new String(buffer, 0, 4), 16));

		file.read(buffer);
		tourData.setStartPulse((short) Integer.parseInt(new String(buffer, 0, 4), 16));

		return startBlock;
	}

	public final int readSummary(final byte[] buffer) throws IOException {
		final int ch0 = buffer[0];
		final int ch1 = buffer[1];
		final int ch2 = buffer[2];
		final int ch3 = buffer[3];
		if ((ch0 | ch1 | ch2 | ch3) < 0) {
			throw new EOFException();
		}
		return ((ch1 << 8) + (ch0 << 0)) + ((ch3 << 8) + (ch2 << 0));
	}

	/**
	 * @param timeData
	 * @param rawData
	 * @throws IOException
	 */
	public void readTimeSlice(final RandomAccessFile file, final TimeData timeData) throws IOException {

		// read encoded data
		final byte[] buffer = new byte[5];
		file.read(buffer);

		final int data = Integer.parseInt(new String(buffer, 0, 4), 16);

		// decode pulse (4 bits)
		if ((data & 0x8000) != 0) {
			timeData.pulse = (short) ((0xfff0 | ((data & 0xf000) >> 12)) * 2);
		} else {
			timeData.pulse = (short) (((data & 0xF000) >> 12) * 2);
		}

		// decode altitude (6 bits)
		if ((data & 0x0800) != 0) {
			timeData.altitude = (short) (0xFFC0 | ((data & 0x0FC0) >> 6));
			if (timeData.altitude < -16) {
				timeData.altitude = (-16 + ((timeData.altitude + 16) * 7));
			}
		} else {
			timeData.altitude = (short) ((data & 0x0FC0) >> 6);
			if (timeData.altitude > 16) {
				timeData.altitude = (16 + ((timeData.altitude - 16) * 7));
			}
		}

		// decode distance (6 bits)
		timeData.distance = (short) (data & 0x003F) * 10;

	}

	/**
	 * checks if the data file has a valid HAC4 data format
	 * 
	 * @return true for a valid HAC4 data format
	 */
	public boolean validateRawData(final String fileName) {

		boolean isValid = false;

		BufferedInputStream inStream = null;

		try {

			final byte[] buffer = new byte[5];

			final File dataFile = new File(fileName);
			inStream = new BufferedInputStream(new FileInputStream(dataFile));

			inStream.read(buffer);
			if (!"AFRO".equalsIgnoreCase(new String(buffer, 0, 4))) { //$NON-NLS-1$
				return false;
			}

			int checksum = 0;
			int lastValue = 0;
			int position = buffer.length;

			while (inStream.read(buffer) != -1) {
				checksum = (checksum + lastValue) & 0xFFFF;
				lastValue = Integer.parseInt(new String(buffer, 0, 4), 16);

				// check CM4xxM device id
				if (position == OFFSET_DEVICE_DATA) {
					if (lastValue != HARDWARE_ID_CM4XXM) {

						// file does not contain CM4xxM data, force the check
						// sum to be invalid
						checksum = -1;
						break;
					}
				}

				position += buffer.length;
			}

			if (checksum == lastValue) {
				isValid = true;
			}
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
