/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.device.hac4;

import gnu.io.SerialPort;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;

import net.tourbook.data.DataUtil;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

public class HAC4DeviceReader extends TourbookDevice {

	private static final int	HARDWARE_ID_HAC4_315	= 0xB735;
	private static final int	HARDWARE_ID_HAC4_IMP	= 0xB7b4;

	private static final int	RECORD_SIZE				= 40;
	private static final short	HAC4_TIMESLICE			= 20;

	private static final int	OFFSET_DEVICE_DATA		= 645;
	private static final int	OFFSET_DATA_START		= 765;
	private static final int	OFFSET_LAST_RECORD		= 81920;
	private static final int	OFFSET_CHECKSUM			= 81925;

	private static final int	HAC4_DATA_SIZE			= 81930;


	/**
	 * constructor is used when the plugin is loaded
	 */
	public HAC4DeviceReader() {
		canReadFromDevice = true;
	}

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {

		RandomAccessFile fileRawData = null;

		final HAC4DeviceData hac4DeviceData = new HAC4DeviceData();

		try {

			fileRawData = new RandomAccessFile(rawDataFileName, "r"); //$NON-NLS-1$

			// position file pointer to the device data
			fileRawData.seek(OFFSET_DEVICE_DATA);

			// read device data
			hac4DeviceData.readFromFile(fileRawData);

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

		return new Formatter().format(net.tourbook.Messages.Format_rawdata_file_yyyy_mm_dd + fileExtension,
				hac4DeviceData.transferYear,
				hac4DeviceData.transferMonth,
				hac4DeviceData.transferDay).toString();
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

		// "81" jogging 
		// "91" ski 
		// "A1" bike
		// "B1" ski-bike

		switch (profileId) {
		case 129: // 0x81
			return Messages.HAC4_Profile_jogging;

		case 145: // 0x91
			return Messages.HAC4_Profile_ski;

		case 161: // 0xA1
			return Messages.HAC4_Profile_bike;

		case 177: // 0xB1
			return Messages.HAC4_Profile_ski_bike;
		}

		return "HAC4: unknown profile"; //$NON-NLS-1$
	}

	@Override
	public SerialParameters getPortParameters(final String portName) {

		return new SerialParameters(portName,
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
		return HAC4_DATA_SIZE;
	}

	public boolean processDeviceData(	final String importFileName,
										final DeviceData deviceData,
										final HashMap<Long, TourData> tourDataMap) {

		RandomAccessFile fileRawData = null;

		final byte[] buffer = new byte[5];
		String recordType = ""; //$NON-NLS-1$

		final HAC4DeviceData hac4DeviceData = new HAC4DeviceData();
		final TourType defaultTourType = getTourType();

		try {

			fileRawData = new RandomAccessFile(importFileName, "r"); //$NON-NLS-1$

			// position file pointer to the device data
			fileRawData.seek(OFFSET_DEVICE_DATA);

			// read device data
			hac4DeviceData.readFromFile(fileRawData);

			/*
			 * because the tour year is not available we calculate it from the transfer year, this
			 * might be not correct but there is no other way to get the year
			 */
			short tourYear = hac4DeviceData.transferYear;
			if (importYear != -1) {
				tourYear = (short) importYear;
			}

			short lastTourMonth = 0;

			// move file pointer to the DD record of the last tour and
			// read "offset AA record" of the last tour and position file
			// pointer there
			fileRawData.seek(hac4DeviceData.offsetDDRecord + 5);
			int offsetAARecord = DataUtil.readFileOffset(fileRawData, buffer);
			final int initialOffsetAARecord = offsetAARecord;
			int offsetDDRecord;

			boolean isLastTour = true;
			/*
			 * read all tours
			 */
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

				tourData.setDeviceTimeInterval(HAC4_TIMESLICE);
				tourData.importRawDataFile = importFileName;
				tourData.setTourImportFilePath(importFileName);

				readStartBlock(fileRawData, tourData);

				/*
				 * add device data to the tour, the last tour is the first which is read from the
				 * data file
				 */
				if (isLastTour) {
					isLastTour = false;

					final int deviceTravelTimeHours = ((hac4DeviceData.totalTravelTimeHourHigh * 100) + hac4DeviceData.totalTravelTimeHourLow);

					tourData.setDeviceTravelTime((deviceTravelTimeHours * 3600)
							+ (hac4DeviceData.totalTravelTimeMin * 60)
							+ hac4DeviceData.totalTravelTimeSec);

					// tourData.deviceDistance = ((deviceData.totalDistanceHigh
					// * (2 ^ 16)) + deviceData.totalDistanceLow);
					tourData.setDeviceWheel(hac4DeviceData.wheelPerimeter);
					tourData.setDeviceWeight(hac4DeviceData.personWeight);
					tourData.setDeviceTotalUp(hac4DeviceData.totalAltitudeUp);
					tourData.setDeviceTotalDown(hac4DeviceData.totalAltitudeDown);
				}

				/*
				 * calculate year of the tour
				 */

				// set initial tour month if not yet done
				lastTourMonth = (lastTourMonth == 0) ? tourData.getStartMonth() : lastTourMonth;

				/*
				 * because we read the tours in decending order (last tour first), we check if the
				 * month of the current tour is higher than from the last tour, if this is the case,
				 * we assume to have data from the previous year
				 */
				if (tourData.getStartMonth() > lastTourMonth) {
					tourYear--;
				}
				lastTourMonth = tourData.getStartMonth();

				tourData.setStartYear(tourYear);

				// tourData.dumpData();
				// out.println("Offset AA Record: " + iOffsetAARecord);

				// create time list
				final ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();

				short temperature;
				short marker;
				short cadence;

				short absolutePulse = tourData.getStartPulse();
				short absoluteAltitude = tourData.getStartAltitude();

				int iDataMax; // number of time slices in a BB record
				boolean isFirstBBRecord = true;

				int sumDistance = 0;
				int sumPulse = 0;
				int sumAltitude = 0;
				int sumCadence = 0;

				/*
				 * read all records of current tour
				 */
				while (!recordType.equalsIgnoreCase("CC")) { //$NON-NLS-1$

					// if we reached EOF, position file pointer at the beginning
					if (fileRawData.getFilePointer() == OFFSET_CHECKSUM) {
						fileRawData.seek(OFFSET_DATA_START);
					}

					// System.out.println(fileRawData.getFilePointer());

					// read encoded data
					fileRawData.read(buffer);

					// decode record type
					recordType = new String(buffer, 2, 2);
					// out.print(recordType + " ");

					// decode temperature
					temperature = Short.parseShort(new String(buffer, 0, 2), 16);
					if (temperature > 127) {
						temperature = (short) (temperature - 255);
					}

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

							timeData.time = 0;
							timeData.distance = 0;
							timeData.pulse = tourData.getStartPulse();
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

							timeData.time = HAC4_TIMESLICE;

						} else {

							// CC record

							if (iData + 1 == iDataMax) {
								// this is the last time slice
								timeData.time = marker % HAC4_TIMESLICE;
							} else {
								// this is a normal time slice
								timeData.time = HAC4_TIMESLICE;
							}
						}

						// read data for this time slice
						readTimeSlice(fileRawData, timeData);

						// adjust pulse from relative to absolute
						absolutePulse += timeData.pulse;
						timeData.pulse = absolutePulse;

						// adjust altitude from relative to absolute
						absoluteAltitude += timeData.altitude;

						tourData.setTourAltUp(tourData.getTourAltUp()
								+ ((timeData.altitude > 0) ? timeData.altitude : 0));
						tourData.setTourAltDown(tourData.getTourAltDown()
								+ ((timeData.altitude < 0) ? -timeData.altitude : 0));

						sumDistance += timeData.distance;
						sumAltitude += Math.abs(absoluteAltitude);
						sumPulse += absolutePulse;
						sumCadence += cadence;
					}
				}

				// after all data are added, the tour id can be created
				final Long tourId = tourData.createTourId(Integer.toString(Math.abs(tourData.getStartDistance())));

				// check if the tour is in the tour map
				if (tourDataMap.containsKey(tourId) == false && timeDataList.size() > 0) {

					// add new tour to the map
					tourDataMap.put(tourId, tourData);

					/*
					 * disable data series when no data are available
					 */
					final TimeData firstTimeData = timeDataList.get(0);
					if (sumDistance == 0) {
						firstTimeData.distance = Integer.MIN_VALUE;
					}
					if (sumAltitude == 0) {
						firstTimeData.altitude = Integer.MIN_VALUE;
					}
					if (sumPulse == 0) {
						firstTimeData.pulse = Integer.MIN_VALUE;
					}
					if (sumCadence == 0) {
						firstTimeData.cadence = Integer.MIN_VALUE;
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

					tourData.setWeek(tourData.getStartYear(), tourData.getStartMonth(), tourData.getStartDay());
				}

				// tourData.dumpTourTotal();

				/*
				 * calculate the start of the previous tour, we have to make sure that we get the
				 * complete tour with all the records
				 */

				if (offsetAARecord == OFFSET_DATA_START) {
					// set position after the end of the tour data
					offsetAARecord = OFFSET_CHECKSUM;
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
				if (offsetDDRecord == hac4DeviceData.offsetNextMemory) {
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
			hac4DeviceData.dumpData();
		} finally {
			if (fileRawData != null) {
				try {
					fileRawData.close();
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		deviceData.transferYear = hac4DeviceData.transferYear;
		deviceData.transferMonth = hac4DeviceData.transferMonth;
		deviceData.transferDay = hac4DeviceData.transferDay;

		return true;
	}

	private void readStartBlock(final RandomAccessFile file, final TourData tourData) throws IOException {

		final byte[] buffer = new byte[5];

		file.read(buffer);
		tourData.setDeviceTourType(new String(buffer, 0, 2));

		tourData.offsetDDRecord = DataUtil.readFileOffset(file, buffer);

		file.read(buffer);
		tourData.setStartHour(Short.parseShort(new String(buffer, 0, 2)));
		tourData.setStartMinute(Short.parseShort(new String(buffer, 2, 2)));

		file.read(buffer);
		tourData.setStartMonth(Short.parseShort(new String(buffer, 0, 2)));
		tourData.setStartDay(Short.parseShort(new String(buffer, 2, 2)));

		file.read(buffer);
		tourData.setStartDistance(Integer.parseInt(new String(buffer, 0, 4), 16));

		file.read(buffer);
		// tourData.setDistance(Integer.parseInt(new String(buffer, 0, 4), 16));

		file.read(buffer);
		tourData.setStartAltitude((short) Integer.parseInt(new String(buffer, 0, 4), 16));

		file.read(buffer);
		tourData.setStartPulse((short) Integer.parseInt(new String(buffer, 0, 4), 16));
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

				// check HAC4 device id
				if (position == OFFSET_DEVICE_DATA) {
					if ((lastValue == HARDWARE_ID_HAC4_315 || lastValue == HARDWARE_ID_HAC4_IMP) == false) {

						// file does not contain HAC4 data, force the check sum to be invalid
						checksum = -1;
						break;
					}
				}
				position += buffer.length;

			}

			if (checksum == lastValue) {
				isValid = true;
			}

			if (isChecksumValidation == false && isValid == false) {

				System.out.println("Checksum validation failed for HAC4 file: " + fileName + ", validation is disabled");//$NON-NLS-1$ //$NON-NLS-2$

				/*
				 * ignore validation
				 */
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

	public boolean validateRawDataNEW(final String fileName) {

		boolean isValid = false;

		RandomAccessFile file = null;
		try {

			file = new RandomAccessFile(fileName, "r"); //$NON-NLS-1$

			final byte[] buffer = new byte[5];

			// check header
			file.read(buffer);
			if (!"AFRO".equalsIgnoreCase(new String(buffer, 0, 4))) { //$NON-NLS-1$
				return false;
			}

			int checksum = 0, lastValue = 0;

			while (file.read(buffer) != -1) {
				checksum = (checksum + lastValue) & 0xFFFF;

				lastValue = readSummary(buffer);

				// int lastValueOrig = Integer.parseInt(new String(buffer, 0,
				// 4), 16);
				// System.out.println(lastValueOrig + " " + lastValue);
			}

			if (checksum == lastValue) {
				isValid = true;
			}

		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final NumberFormatException e) {
			return false;
		} finally {
			if (file != null) {
				try {
					file.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}

		return isValid;
	}
}
