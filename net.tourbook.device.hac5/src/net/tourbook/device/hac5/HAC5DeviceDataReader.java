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
package net.tourbook.device.hac5;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.device.DeviceData;
import net.tourbook.device.TourbookDevice;

public class HAC5DeviceDataReader extends TourbookDevice {

	/**
	 * position in the file which skips the header (AFRO..) and the raw data
	 * begins
	 */
	public static final int		OFFSET_RAWDATA			= 6;

	private static final int	OFFSET_TOUR_DATA_START	= 0x0800;
	private static final int	OFFSET_TOUR_DATA_END	= 0x10000;

	private static final int	RECORD_LENGTH			= 0x10;

	private String				fImportFileName;

	private Calendar			fCalendar				= GregorianCalendar.getInstance();

	private GregorianCalendar	fFileDate;

	// plugin constructor
	public HAC5DeviceDataReader() {}

	/**
	 * Convert a byte[] array to readable string format. This makes the "hex"
	 * readable!
	 * 
	 * @return result String buffer in String format
	 * @param in
	 *        byte[] buffer to convert to string format
	 */

	public static String byteArrayToHexString(byte in[]) {

		byte ch = 0x00;

		int i = 0;

		if (in == null || in.length <= 0) {
			return null;
		}

		String pseudo[] = {
				"0", //$NON-NLS-1$
				"1", //$NON-NLS-1$
				"2", //$NON-NLS-1$
				"3", //$NON-NLS-1$
				"4", //$NON-NLS-1$
				"5", //$NON-NLS-1$
				"6", //$NON-NLS-1$
				"7", //$NON-NLS-1$
				"8", //$NON-NLS-1$
				"9", //$NON-NLS-1$
				"A", //$NON-NLS-1$
				"B", //$NON-NLS-1$
				"C", //$NON-NLS-1$
				"D", //$NON-NLS-1$
				"E", //$NON-NLS-1$
				"F" }; //$NON-NLS-1$

		StringBuffer out = new StringBuffer(in.length * 2 + in.length);

		while (i < in.length) {

			// Strip off high nibble
			ch = (byte) (in[i] & 0xF0);

			// shift the bits down
			ch = (byte) (ch >>> 4);

			// must do this is high order bit is on!
			ch = (byte) (ch & 0x0F);

			// convert the nibble to a String Character
			out.append(pseudo[(int) ch]);

			// Strip off low nibble
			ch = (byte) (in[i] & 0x0F);

			// convert the nibble to a String Character
			out.append(pseudo[(int) ch]);

			// add space between two bytes
			out.append(' ');

			i++;
		}

		String rslt = new String(out);

		return rslt;
	}

	/**
	 * get a 2 byte value (unsigned integer) from the buffer, by swapping the
	 * high and low byte
	 * 
	 * @param buffer
	 * @param offset
	 * @return Returns unsigned integer address
	 */
	private static int get2ByteData(byte[] buffer, int offset) {
		int byte1 = (buffer[offset] & 0xFF) << 0;
		int byte2 = (buffer[offset + 1] & 0xFF) << 8;
		return byte2 + byte1;
	}

	private static long get4ByteData(byte[] buffer, int offset) {
		long byte1 = (buffer[offset] & 0xFF) << 0;
		long byte2 = (buffer[offset + 1] & 0xFF) << 8;
		long byte3 = (buffer[offset + 2] & 0xFF) << 16;
		long byte4 = (buffer[offset + 3] & 0xFF) << 24;
		return byte4 + byte3 + byte2 + byte1;
	}

	/**
	 * Adjust the offset for the DD record so it's within the tour data area
	 * 
	 * @param offsetNextDDRecord
	 * @return
	 */
	private int adjustDDRecordOffset(int offsetNextDDRecord) {

		int offsetDDRecord;

		if (offsetNextDDRecord == OFFSET_TOUR_DATA_START) {
			offsetDDRecord = OFFSET_TOUR_DATA_END - RECORD_LENGTH;
		} else {
			offsetDDRecord = offsetNextDDRecord - RECORD_LENGTH;
		}

		return offsetDDRecord;
	}

	private void dumpBlock(RandomAccessFile file, byte[] recordBuffer) throws IOException {
		file.seek(file.getFilePointer() - RECORD_LENGTH);
		dumpBuffer(file, recordBuffer);
		file.read(recordBuffer);
	}

	private void dumpBuffer(RandomAccessFile file, byte[] recordBuffer) throws IOException {
		long pos = file.getFilePointer();
		file.read(recordBuffer);
		file.seek(pos);

		String address = "0000" + Integer.toHexString((int) pos); //$NON-NLS-1$
		address = address.substring(address.length() - 4);

		System.out.println((address + ": ") + byteArrayToHexString(recordBuffer)); //$NON-NLS-1$
	}

	public int getImportDataSize() {
		return 0;
	}

	public String getImportFileName() {
		return fImportFileName;
	}

	public boolean processDeviceData(	String fileName,
										DeviceData deviceData,
										ArrayList<TourData> tourDataList) {
		boolean returnValue = false;

		byte[] recordBuffer = new byte[RECORD_LENGTH];

		RandomAccessFile file = null;

		HAC5DeviceData hac5DeviceData = new HAC5DeviceData();

		// reset tour data list
		tourDataList.clear();

		try {
			File fileRaw = new File(fileName);
			file = new RandomAccessFile(fileRaw, "r"); //$NON-NLS-1$

			long lastModified = fileRaw.lastModified();

			// dump header
			// file.seek(OFFSET_RAWDATA + 0x0380);
			// for (int i = 0; i < 4000; i++) {
			// dumpBuffer(file, recordBuffer);
			// file.read(recordBuffer);
			// }

			/*
			 * get the year, because the year is not saved in the raw data file,
			 * the modified year of the file is used
			 */
			fFileDate = new GregorianCalendar();
			fFileDate.setTime(new Date(lastModified));

			short tourYear = (short) fFileDate.get(Calendar.YEAR);
			short lastTourMonth = -1;

			// read device data
			hac5DeviceData.readFromFile(file);

			/*
			 * get position for the next free tour and get the last dd-record
			 * from this position
			 */
			file.seek(OFFSET_RAWDATA + 0x0380 + 2);
			int offsetNextFreeTour = get2ByteData(file);

			int offsetDDRecord = adjustDDRecordOffset(offsetNextFreeTour);

			int initialOffsetDDRecord = offsetDDRecord;

			while (true) {

				// read DD record
				file.seek(OFFSET_RAWDATA + offsetDDRecord);
				file.read(recordBuffer);
				if ((recordBuffer[0] & 0xFF) != 0xDD) {
					returnValue = true;
					break;
				}

				// read AA record
				int offsetAARecordInDDRecord = get2ByteData(recordBuffer, 2);

				// dump AA block
				// file.seek(OFFSET_RAWDATA + offsetAARecordInDDRecord);
				// dumpBuffer(file, recordBuffer);

				file.seek(OFFSET_RAWDATA + offsetAARecordInDDRecord);
				file.read(recordBuffer);
				if ((recordBuffer[0] & 0xFF) != 0xAA) {
					returnValue = true;
					break;
				}

				/*
				 * check if the AA and the DD records point to each other
				 */
				int offsetDDRecordInAARecord = get2ByteData(recordBuffer, 2);
				if (offsetDDRecordInAARecord != offsetDDRecord) {
					returnValue = true;
					break;
				}

				TourData tourData = new TourData();
				tourDataList.add(tourData);

				/*
				 * save AA record data
				 */
				readAARecord(recordBuffer, tourData);

				/*
				 * calculate year of the tour
				 */
				if (lastTourMonth == -1) {
					// set initial tour month
					lastTourMonth = tourData.getStartMonth();
				}

				/*
				 * the tours are read in decending order (last tour first), if
				 * the month of the current tour is higher than from the last
				 * tour, we assume to have data from the previous year
				 */
				if (tourData.getStartMonth() > lastTourMonth) {
					tourYear--;
				}
				lastTourMonth = tourData.getStartMonth();

				tourData.setStartYear(tourYear);

				/*
				 * read/save BB records
				 */
				ArrayList<TimeData> timeDataList = new ArrayList<TimeData>();

				short timeInterval = tourData.getDeviceTimeInterval();

				short totalPulse = tourData.getStartPulse();
				short totalAltitude = tourData.getStartAltitude();

				short temperature;
				short marker;
				short cadence;

				boolean isFirstDataRecord = true;
				boolean isCCRecord = false;

				while (true) {

					// read BB or CC record
					file.read(recordBuffer);

					if ((recordBuffer[0] & 0xFF) == 0xCC) {
						isCCRecord = true;
					}

					// dump BB block
					// dumpBlock(file, recordBuffer);

					temperature = (short) (recordBuffer[1]);// & 0xFF);
					cadence = (short) (recordBuffer[2] & 0xFF);
					marker = (short) (recordBuffer[3] & 0xFF);

					/*
					 * the CC record does not contain the cadence, it contains
					 * the exact time when the tour ends, so we read only those
					 * time slices which contain the tour data
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
							 * create the START time slice, the current slice is
							 * the first slice which already contains data
							 */

							isFirstDataRecord = false;

							timeDataList.add(timeData = new TimeData());

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
							timeData.time = (short) (cadence % 20);
						} else {
							timeData.time = timeInterval;
						}

						// summarize the recording time
						tourData.setTourRecordingTime(tourData.getTourRecordingTime()
								+ timeData.time);

						// read data for the current time slice
						readTimeSlice(get2ByteData(recordBuffer, 4 + (2 * dataIndex)), timeData);

						// set distance
						tourData.setTourDistance(tourData.getTourDistance() + timeData.distance);

						// adjust pulse from relative to absolute
						timeData.pulse = totalPulse += timeData.pulse;

						if (timeData.pulse < 0) {
							timeData.pulse = 0;
						}

						// adjust altitude from relative to absolute
						totalAltitude += timeData.altitude;

						tourData.setTourAltUp(tourData.getTourAltUp()
								+ ((timeData.altitude > 0) ? timeData.altitude : 0));
						tourData.setTourAltDown(tourData.getTourAltDown()
								+ ((timeData.altitude < 0) ? -timeData.altitude : 0));
					}

					// check if the last record was read
					if (isCCRecord) {
						break;
					}

					/*
					 * when the end of the buffer is reached, read from the
					 * beginning of the ring buffer
					 */
					if (file.getFilePointer() > OFFSET_RAWDATA + OFFSET_TOUR_DATA_END) {
						file.seek(OFFSET_RAWDATA + OFFSET_TOUR_DATA_START);
					}
				}

				// read/save DD record
				offsetDDRecord = (int) file.getFilePointer();
				file.read(recordBuffer);
				if ((recordBuffer[0] & 0xFF) != 0xDD) {
					break;
				}

				// after all data are added, the tour id can be created
				tourData.createTourId();

				tourData.createTimeSeries(timeDataList);

				tourData.computeTourDrivingTime();
				
				// set week of year
				fCalendar.set(tourData.getStartYear(), tourData.getStartMonth() - 1, tourData
						.getStartDay());
				tourData.setStartWeek((short) fCalendar.get(Calendar.WEEK_OF_YEAR));

				// dump DD block
				// dumpBlock(file, recordBuffer);
				//
				// System.out.println("");
				// System.out.println("");

				/*
				 * make sure not to end in an endless loop where the current DD
				 * offset is the same as the first DD offset (this seems to be
				 * unlikely but it happend already 2 Month after the first
				 * implementation)
				 */
				if (offsetDDRecord == initialOffsetDDRecord) {
					returnValue = true;
					break;
				}

				offsetDDRecord = adjustDDRecordOffset(offsetAARecordInDDRecord);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			returnValue = false;
		} catch (IOException e) {
			e.printStackTrace();
			returnValue = false;
		} finally {
			if (file != null) {
				try {
					file.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		if (returnValue) {

			fImportFileName = fileName;

			deviceData.transferYear = (short) fFileDate.get(Calendar.YEAR);
			deviceData.transferMonth = (short) (fFileDate.get(Calendar.MONTH) + 1);
			deviceData.transferDay = (short) fFileDate.get(Calendar.DAY_OF_MONTH);
		}

		return returnValue;
	}
	/**
	 * @param timeData
	 * @param rawData
	 * @throws IOException
	 */
	public void readTimeSlice(int data, TimeData timeData) throws IOException {

		// pulse (4 bits)
		if ((data & 0x8000) != 0) {
			// -
			timeData.pulse = (short) ((0xFFF0 | ((data & 0xF000) >> 12)));
		} else {
			// +
			timeData.pulse = (short) (((data & 0xF000) >> 12));
		}

		// altitude (6 bits)
		if ((data & 0x0800) != 0) {
			// -
			timeData.altitude = (short) (0xFFC0 | ((data & 0x0FC0) >> 6));
			if (timeData.altitude < -16) {
				timeData.altitude = (short) (-16 + ((timeData.altitude + 16) * 7));
			}
		} else {
			// +
			timeData.altitude = (short) ((data & 0x0FC0) >> 6);
			if (timeData.altitude > 16) {
				timeData.altitude = (short) (16 + ((timeData.altitude - 16) * 7));
			}
		}

		// distance (6 bits)
		timeData.distance = (data & 0x003F) * 10;
	}

	/**
	 * @param buffer
	 * @param tourData
	 */
	private void readAARecord(byte[] buffer, TourData tourData) {

		// 00 1 0xAA
		//
		// 01 4: mode:
		// 0: bike1
		// 1: bike2
		// 2: rds
		// 3: alpine
		// 4: run
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
		// 08 4 ? total distance (m)
		//
		// 12 2 initial altitude
		// 14 1 initial pulse
		//
		// 15 1 ? 0xFF

		byte byteValue = buffer[1];

		int timeInterval = byteValue & 0x0F;
		int profile = (byteValue & 0xF0) >> 4;

		// set the timeinterval from the AA record
		timeInterval = timeInterval == 0 ? 2 : timeInterval == 1 ? 5 : timeInterval == 2 ? 10 : 20;

		tourData.setDeviceMode((short) (profile));
		tourData.setDeviceTimeInterval((short) timeInterval);

		tourData.setStartMinute(buffer[4]);
		tourData.setStartHour(buffer[5]);
		tourData.setStartDay(buffer[6]);
		tourData.setStartMonth(buffer[7]);

		tourData.setStartDistance((int) get4ByteData(buffer, 8));
		tourData.setStartAltitude((short) get2ByteData(buffer, 12));
		tourData.setStartPulse(buffer[14]);
	}

	private int get2ByteData(RandomAccessFile file) throws IOException {

		int ch1 = file.read();
		int ch2 = file.read();
		if ((ch1 | ch2) < 0)
			throw new EOFException();
		int offset = (ch2 << 8) + (ch1 << 0);
		return (offset);
	}

	// public boolean validateRawData(String fileName) {
	// return false;
	// }

	/**
	 * checks if the data file has a valid HAC4 data format
	 * 
	 * @return true for a valid HAC4 data format
	 */
	public boolean validateRawData(String fileName) {

		boolean isValid = false;

		BufferedInputStream inStream = null;

		try {

			byte[] bufferHeader = new byte[6];
			byte[] bufferData = new byte[2];

			File dataFile = new File(fileName);
			inStream = new BufferedInputStream(new FileInputStream(dataFile));

			inStream.read(bufferHeader);
			if (!"AFRO".equalsIgnoreCase(new String(bufferHeader, 0, 4))) { //$NON-NLS-1$
				return false;
			}

			int checksum = 0, lastValue = 0;

			while (inStream.read(bufferData) != -1) {

				checksum = (checksum + lastValue) & 0xFFFF;

				lastValue = ((bufferData[0] & 0xFF) << 0) + ((bufferData[1] & 0xFF) << 8);
			}

			if (checksum == lastValue) {
				isValid = true;
			}

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

	public String getDeviceModeName(int profileId) {

		// 0: bike1
		// 1: bike2
		// 2: rds
		// 3: alpine
		// 4: run

		switch (profileId) {
		case 0:
			return Messages.HAC5_profile_bike1;

		case 1:
			return Messages.HAC5_profile_bike2;

		case 2:
			return Messages.HAC5_profile_rds;

		case 3:
			return Messages.HAC5_profile_alpine;

		case 4:
			return Messages.HAC5_profile_run;

		default:
			break;
		}

		return Messages.HAC5_profile_none;
	}

}
