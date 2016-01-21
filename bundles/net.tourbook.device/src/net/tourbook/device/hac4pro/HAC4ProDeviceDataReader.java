/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.device.hac4pro;

/*
 * acknowledgement: implementing this device driver was supported by Hans-Joachim Willi from
 * http://www.bikexperience.de/
 */

import gnu.io.SerialPort;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.device.DeviceReaderTools;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

public class HAC4ProDeviceDataReader extends TourbookDevice {

	private static final byte	HAC4PRO_HARDWARE_ID		= 0x02;

	private static final int	RECORD_LENGTH			= 0x10;

	private static final int	OFFSET_NEXT_FREE_BLOCK	= 0x0130;

	static final int			OFFSET_RAWDATA			= 6;
	private static final int	OFFSET_TOUR_DATA_START	= 0x0140;
	private static final int	OFFSET_TOUR_DATA_END	= 0x10000;

	private GregorianCalendar	fFileDate;

	private class StartBlock {
		public int	month;
		public int	day;
		public int	hour;
		public int	minute;
	}

	// plugin constructor
	public HAC4ProDeviceDataReader() {}

	/**
	 * @param timeData
	 * @param rawData
	 * @throws IOException
	 */
	public static void readTimeSlice(final int data, final TimeData timeData) throws IOException {

		// pulse (4 bits)
		if ((data & 0x8000) != 0) {
			// -
			timeData.pulse = (short) ((0xFFF0 | ((data & 0xF000) >> 12)) * 2);
		} else {
			// +
			timeData.pulse = (short) (((data & 0xF000) >> 12) * 2);
		}

		// altitude (6 bits)
		if ((data & 0x0800) != 0) {
			// -
			timeData.altitude = (short) (0xFFC0 | ((data & 0x0FC0) >> 6));
			if (timeData.altitude < -16) {
				timeData.altitude = (-16 + ((timeData.altitude + 16) * 7));
			}
		} else {
			// +
			timeData.altitude = (short) ((data & 0x0FC0) >> 6);
			if (timeData.altitude > 16) {
				timeData.altitude = (16 + ((timeData.altitude - 16) * 7));
			}
		}

		// distance (6 bits)
		timeData.distance = (short) (data & 0x003F) * 10;
	}

	/**
	 * Adjust the offset for the DD record so it's within the tour data area
	 * 
	 * @param offsetNextDDRecord
	 * @return
	 */
	private int adjustDDRecordOffset(final int offsetNextDDRecord) {

		int offsetDDRecord;

		if (offsetNextDDRecord == OFFSET_TOUR_DATA_START) {
			offsetDDRecord = OFFSET_TOUR_DATA_END - RECORD_LENGTH;
		} else {
			offsetDDRecord = offsetNextDDRecord - RECORD_LENGTH;
		}

		return offsetDDRecord;
	}

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {

		final File fileRaw = new File(rawDataFileName);

		final long lastModified = fileRaw.lastModified();

		/*
		 * get the year, because the year is not saved in the raw data file, the modified year of
		 * the file is used
		 */
		final GregorianCalendar fileDate = new GregorianCalendar();
		fileDate.setTime(new Date(lastModified));

		return String.format(
				net.tourbook.Messages.Format_rawdata_file_yyyy_mm_dd + fileExtension,
				(short) fileDate.get(Calendar.YEAR),
				(short) fileDate.get(Calendar.MONTH) + 1,
				(short) fileDate.get(Calendar.DAY_OF_MONTH));
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

	public void computeTourAltitudeUpDown(final TourData tourData) {

		final float[] altitudeSerie = tourData.altitudeSerie;

		if (altitudeSerie.length < 2) {
			return;
		}

		float altUp = 0f;
		float altDown = 0f;

		float lastAltitude1 = altitudeSerie[0];
		float lastAltitude2 = altitudeSerie[1];

//		int logUp = 0;
//		int logDown = 0;

		for (final float altitude : altitudeSerie) {

			if (lastAltitude1 == lastAltitude2 + 1 & altitude == lastAltitude1) {
				// altUp += 0.5f;
//				logUp++;
			} else if (lastAltitude1 == lastAltitude2 - 1 & altitude == lastAltitude1) {
				// altDown += 0.5f;
//				logDown++;
			} else if (altitude > lastAltitude2) {
				altUp += altitude - lastAltitude2;
			} else if (altitude < lastAltitude2) {
				altDown += lastAltitude2 - altitude;
			}

			lastAltitude1 = lastAltitude2;
			lastAltitude2 = altitude;
		}

		tourData.setTourAltUp(altUp);
		tourData.setTourAltDown(altDown);

		// System.out.println("Up: " + logUp + " Down: " + logDown);
	}

	@Override
	public String getDeviceModeName(final int profileId) {

		// 1: run
		// 2: bike 2
		// 3: bike1
		// 4: ski

		switch (profileId) {
		case 1:
			return Messages.HAC4_Profile_run;

		case 2:
			return Messages.HAC4_Profile_bike2;

		case 3:
			return Messages.HAC4_Profile_bike1;

		case 4:
			return Messages.HAC4_Profile_ski;

		default:
			break;
		}

		return Messages.HAC4_Profile_unknown;
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

	@Override
	public int getTransferDataSize() {
		return 0x1009A;
	}

	@Override
	public boolean processDeviceData(	final String importFilePath,
										final DeviceData deviceData,
										final HashMap<Long, TourData> alreadyImportedTours,
										final HashMap<Long, TourData> newlyImportedTours) {

		boolean returnValue = false;

		final byte[] recordBuffer = new byte[RECORD_LENGTH];

		RandomAccessFile file = null;

		final HAC4ProDeviceData hac4ProDeviceData = new HAC4ProDeviceData();

		try {
			final File fileRaw = new File(importFilePath);
			file = new RandomAccessFile(fileRaw, "r"); //$NON-NLS-1$

			final long lastModified = fileRaw.lastModified();

			// dump header
			// file.seek(OFFSET_RAWDATA + 0x0380);
			// for (int i = 0; i < 4000; i++) {
			// dumpBuffer(file, recordBuffer);
			// file.read(recordBuffer);
			// }

			/*
			 * get the year, because the year is not saved in the raw data file, the modified year
			 * of the file is used
			 */
			fFileDate = new GregorianCalendar();
			fFileDate.setTime(new Date(lastModified));

			int tourYear = fFileDate.get(Calendar.YEAR);
			int lastTourMonth = -1;

			// read device data
			hac4ProDeviceData.readFromFile(file);

			/*
			 * get position for the next free tour and get the last dd-record from this position
			 */
			file.seek(OFFSET_NEXT_FREE_BLOCK);
			final int offsetNextFreeTour = DeviceReaderTools.get2ByteData(file);

			int offsetDDRecord = adjustDDRecordOffset(offsetNextFreeTour);
			final int initialOffsetDDRecord = offsetDDRecord;
			int bytes;
			int tourCounter = 0;

			while (true) {

				// read DD record
				file.seek(OFFSET_RAWDATA + offsetDDRecord);
				bytes = file.read(recordBuffer);
				if ((recordBuffer[0] & 0xFF) != 0xDD || bytes == -1) {
					returnValue = true;
					break;
				}

				// read AA record
				final int offsetAARecordInDDRecord = DeviceReaderTools.get2ByteData(recordBuffer, 2);

				// dump AA block
				// file.seek(OFFSET_RAWDATA + offsetAARecordInDDRecord);
				// dumpBuffer(file, recordBuffer);

				file.seek(OFFSET_RAWDATA + offsetAARecordInDDRecord);
				bytes = file.read(recordBuffer);
				if ((recordBuffer[0] & 0xFF) != 0xAA || bytes == -1) {
					returnValue = true;
					break;
				}

				/*
				 * check if the AA and the DD records point to each other
				 */
				final int offsetDDRecordInAARecord = DeviceReaderTools.get2ByteData(recordBuffer, 2);
				if (offsetDDRecordInAARecord != offsetDDRecord) {
					returnValue = true;
					break;
				}

				final TourData tourData = new TourData();

				tourData.setImportFilePath(importFilePath);

				/*
				 * save AA record data
				 */
				final StartBlock startBlock = readAARecord(recordBuffer, tourData);

				/*
				 * calculate year of the tour
				 */
				if (lastTourMonth == -1) {
					// set initial tour month
					lastTourMonth = startBlock.month;
				}

				/*
				 * the tours are read in decending order (last tour first), if the month of the
				 * current tour is higher than from the last tour, we assume to have data from the
				 * previous year
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

				/*
				 * read/save BB records
				 */
				final ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();

				final short timeInterval = tourData.getDeviceTimeInterval();

				short absolutePulse = tourData.getStartPulse();
				short absoluteAltitude = tourData.getStartAltitude();

				short temperature;
				short marker;
				short cadence;

				boolean isFirstDataRecord = true;
				boolean isCCRecord = false;

				int sumDistance = 0;
				int sumPulse = 0;
				int sumAltitude = 0;
				int sumCadence = 0;

				while (true) {

					// read BB or CC record
					bytes = file.read(recordBuffer);
					if (bytes == -1) {
						break;
					}

					if ((recordBuffer[0] & 0xFF) == 0xCC) {
						isCCRecord = true;
					}

					// dump BB block
					// dumpBlock(file, recordBuffer);

					temperature = (recordBuffer[1]);
					cadence = (short) (recordBuffer[2] & 0xFF);
					marker = (short) (recordBuffer[3] & 0xFF);

					/*
					 * the CC record does not contain the cadence, it contains the exact time when
					 * the tour ends, so we read only those time slices which contain the tour data
					 */
					int dataLength = 0;
					if (isCCRecord) {

						dataLength = (cadence / timeInterval) + 1;

						// make sure not to exceed the maximum
						if (dataLength > 6) {
							dataLength = 6;
						}
					} else {
						dataLength = 6;
					}

					// get all slices in the current record (BB or CC)
					for (int dataIndex = 0; dataIndex < dataLength; dataIndex++) {

						TimeData timeData;

						if (isFirstDataRecord) {

							/*
							 * create the START time slice, the current slice is the first slice
							 * which already contains data
							 */

							isFirstDataRecord = false;

							timeDataList.add(timeData = new TimeData());

							timeData.distance = 0;
							timeData.pulse = tourData.getStartPulse();
							timeData.altitude = tourData.getStartAltitude();
							timeData.temperature = temperature;
							timeData.cadence = cadence;
						}

						timeDataList.add(timeData = new TimeData());

						timeData.temperature = temperature;
						timeData.cadence = cadence;

						/*
						 * only one marker is in one record
						 */
						if (dataIndex == 0 && marker != 0xFF) {
							timeData.marker = marker;
						}

						// set time
						if (isCCRecord && (dataIndex + 1 == dataLength)) {
							/*
							 * this is the last time slice within the whole tour
							 */
							timeData.time = cadence % 20;
						} else {
							timeData.time = timeInterval;
						}

						// read data for the current time slice
						readTimeSlice(DeviceReaderTools.get2ByteData(recordBuffer, 4 + (2 * dataIndex)), timeData);

						// adjust pulse from relative to absolute value
						timeData.pulse = absolutePulse += timeData.pulse;

						// adjust altitude from relative to absolute
						absoluteAltitude += timeData.altitude;

						sumDistance += timeData.distance;
						sumAltitude += Math.abs(absoluteAltitude);
						sumPulse += absolutePulse;
						sumCadence += cadence;
					}

					// check if the last record was read
					if (isCCRecord) {
						break;
					}

					/*
					 * when the end of the buffer is reached, read from the beginning of the ring
					 * buffer
					 */
					if (file.getFilePointer() >= OFFSET_RAWDATA + OFFSET_TOUR_DATA_END) {
						file.seek(OFFSET_RAWDATA + OFFSET_TOUR_DATA_START);
					}
				}

				// read/save DD record
				offsetDDRecord = (int) file.getFilePointer();
				bytes = file.read(recordBuffer);
				if ((recordBuffer[0] & 0xFF) != 0xDD || bytes == -1) {
					break;
				}

				tourData.setDeviceId(deviceId);
				tourData.setDeviceName(visibleName);

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
					if (sumPulse == 0) {
						firstTimeData.pulse = Float.MIN_VALUE;
					}
					if (sumCadence == 0) {
						firstTimeData.cadence = Float.MIN_VALUE;
					}
				}

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
					tourData.computeTourDrivingTime();
					tourData.computeComputedValues();
					computeTourAltitudeUpDown(tourData);
				}

				// dump DD block
				// dumpBlock(file, recordBuffer);
				//
				// System.out.println(UI.EMPTY_STRING);
				// System.out.println(UI.EMPTY_STRING);

				offsetDDRecord = adjustDDRecordOffset(offsetAARecordInDDRecord);

				/*
				 * make sure not to end in an endless loop where the current DD offset is the same
				 * as the first DD offset (this seems to be unlikely but it happend already 2 Month
				 * after the first implementation)
				 */
				if (offsetDDRecord == initialOffsetDDRecord) {
					returnValue = true;
					break;
				}

				// check if something got wrong
				if (tourCounter++ == 1000) {
					returnValue = true;
					break;
				}
			}

		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			returnValue = false;
		} catch (final IOException e) {
			e.printStackTrace();
			returnValue = false;
		} catch (final NumberFormatException e) {
			e.printStackTrace();
			returnValue = false;
		} finally {
			if (file != null) {
				try {
					file.close();
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		if (returnValue) {

			// fImportFileName = fileName;

			deviceData.transferYear = (short) fFileDate.get(Calendar.YEAR);
			deviceData.transferMonth = (short) (fFileDate.get(Calendar.MONTH) + 1);
			deviceData.transferDay = (short) fFileDate.get(Calendar.DAY_OF_MONTH);
		}

		return returnValue;
	}

	/**
	 * @param buffer
	 * @param tourData
	 * @return
	 */
	private StartBlock readAARecord(final byte[] buffer, final TourData tourData) {

		// 00 1 0xAA
		//
		// 01 4: mode:
		// 1: run
		// 2: bike 2
		// 3: bike1
		// 4: ski
		//
		// 01 4: time interval
		// 0: 2 sec
		// 1: 5 sec
		// 2: 10 sec
		// 3: 20 sec
		//
		// 02 2 address of the DD record
		//
		// 04 1 minute
		// 05 1 hour
		// 06 1 day
		// 07 1 month
		//
		// 08 4 tourstart total distance (m)
		//
		// 12 2 tourstart initial altitude
		// 14 1 tourstart initial pulse
		//
		// 15 1 ? 0xFF

		final byte byteValue = buffer[1];

		int timeInterval = byteValue & 0x0F;
		final int profile = (byteValue & 0xF0) >> 4;

		// set the timeinterval from the AA record
		timeInterval = timeInterval == 0 ? 2 : timeInterval == 1 ? 5 : timeInterval == 2 ? 10 : 20;

		tourData.setDeviceMode((short) (profile));
		tourData.setDeviceModeName(getDeviceModeName(profile));

		tourData.setDeviceTimeInterval((short) timeInterval);

		final StartBlock startBlock = new StartBlock();
		startBlock.minute = DeviceReaderTools.convert1ByteBCD(buffer, 4);
		startBlock.hour = DeviceReaderTools.convert1ByteBCD(buffer, 5);
		startBlock.day = DeviceReaderTools.convert1ByteBCD(buffer, 6);
		startBlock.month = DeviceReaderTools.convert1ByteBCD(buffer, 7);

		tourData.setStartDistance((int) DeviceReaderTools.get4ByteData(buffer, 8));
		tourData.setStartAltitude((short) DeviceReaderTools.get2ByteData(buffer, 12));
		tourData.setStartPulse((short) (buffer[14] & 0xff));

		return startBlock;
	}

	/**
	 * checks if the data file has a valid HAC4Pro data format
	 * 
	 * @return true for a valid HAC4Pro data format
	 */
	@Override
	public boolean validateRawData(final String fileName) {

		boolean isValid = false;

		BufferedInputStream inStream = null;

		try {

			final byte[] bufferHeader = new byte[6];
			final byte[] bufferData = new byte[2];

			final File dataFile = new File(fileName);
			inStream = new BufferedInputStream(new FileInputStream(dataFile));

			inStream.read(bufferHeader);
			if (!"AFRO".equalsIgnoreCase(new String(bufferHeader, 0, 4))) { //$NON-NLS-1$
				return false;
			}

			// check hardware id
			if (bufferHeader[4] != HAC4PRO_HARDWARE_ID) {
				return false;
			}

			int checksum = 0, lastValue = 0;
			int streamPointer = 6;
			while (streamPointer < 0x10096 && inStream.read(bufferData) != -1) {

				checksum = (checksum + lastValue) & 0xFFFF;

				lastValue = ((bufferData[0] & 0xFF) << 0) + ((bufferData[1] & 0xFF) << 8);

				streamPointer += 2;
				// System.out.println(streamPointer);
			}

			inStream.read(bufferHeader);
			lastValue = Integer.parseInt(new String(bufferHeader, 0, 4), 16);

			// if (checksum == lastValue) {
			isValid = true;
			// }

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
