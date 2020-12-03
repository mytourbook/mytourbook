/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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

import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.device.DeviceReaderTools;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

import org.eclipse.osgi.util.NLS;

public class HAC5DeviceDataReader extends TourbookDevice {

   private static final int  HAC5_HARDWARE_ID       = 0x03;

   /**
    * position in the file which skips the header (AFRO..) and the raw data begins
    */
   public static final int   OFFSET_RAWDATA         = 6;

   private static final int  OFFSET_TOUR_DATA_START = 0x0800;
   private static final int  OFFSET_TOUR_DATA_END   = 0x10000;

   private static final int  RECORD_LENGTH          = 0x10;

   private GregorianCalendar _fileDate;

   private class StartBlock {
      public int month;
      public int day;
      public int hour;
      public int minute;
   }

   // plugin constructor
   public HAC5DeviceDataReader() {}

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

   @Override
   public String getDeviceModeName(final int profileId) {

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

   @Override
   public SerialParameters getPortParameters(final String portName) {

      final SerialParameters hac5PortParameters = new SerialParameters(
            portName,
            4800,
            SerialPort.FLOWCONTROL_NONE,
            SerialPort.FLOWCONTROL_NONE,
            SerialPort.DATABITS_8,
            SerialPort.STOPBITS_1,
            SerialPort.PARITY_NONE);

      return hac5PortParameters;
   }

   @Override
   public int getStartSequenceSize() {
      return 4;
   }

   @Override
   public int getTransferDataSize() {
      return 0x10007;
   }

   @Override
   public boolean processDeviceData(final String importFilePath,
                                    final DeviceData deviceData,
                                    final HashMap<Long, TourData> alreadyImportedTours,
                                    final HashMap<Long, TourData> newlyImportedTours) {

      boolean returnValue = false;

      final byte[] recordBuffer = new byte[RECORD_LENGTH];

      RandomAccessFile file = null;

      final HAC5DeviceData hac5DeviceData = new HAC5DeviceData();

      boolean isFirstTour = true;
      int firstTourDay = 1;
      int firstTourMonth = 1;

      try {
         final File fileRaw = new File(importFilePath);
         file = new RandomAccessFile(fileRaw, "r"); //$NON-NLS-1$

         final long lastModified = fileRaw.lastModified();

         /*
          * get the year, because the year is not saved in the raw data file, the modified year
          * of the file is used
          */
         _fileDate = new GregorianCalendar();
         _fileDate.setTime(new Date(lastModified));

         int tourYear = _fileDate.get(Calendar.YEAR);
         if (importYear != RawDataManager.ADJUST_IMPORT_YEAR_IS_DISABLED) {
            tourYear = importYear;
         }

         int lastTourMonth = -1;

         // read device data
         hac5DeviceData.readFromFile(file);

         /*
          * get position for the next free tour and get the last dd-record from this position
          */
         file.seek(OFFSET_RAWDATA + 0x0380 + 2);
         final int offsetNextFreeTour = DeviceReaderTools.get2ByteData(file);

         int offsetDDRecord = adjustDDRecordOffset(offsetNextFreeTour);

         final int initialOffsetDDRecord = offsetDDRecord;
         int checkedOffset1 = Integer.MAX_VALUE;
         int checkedOffset2 = Integer.MAX_VALUE;

         // loop: all tours in the file
         while (true) {

            // read DD record
            file.seek(OFFSET_RAWDATA + offsetDDRecord);
            file.read(recordBuffer);
            if ((recordBuffer[0] & 0xFF) != 0xDD) {
               returnValue = true;
               break;
            }

            // read AA record
            final int offsetAARecordInDDRecord = DeviceReaderTools.get2ByteData(recordBuffer, 2);

            // check if this is a AA record
            file.seek(OFFSET_RAWDATA + offsetAARecordInDDRecord);
            file.read(recordBuffer);
            if ((recordBuffer[0] & 0xFF) != 0xAA) {
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
             * get date from the last tour
             */
            if (isFirstTour) {
               isFirstTour = false;
               firstTourDay = startBlock.day;
               firstTourMonth = startBlock.month;
            }

            /*
             * read/save BB records
             */
            final ArrayList<TimeData> timeDataList = new ArrayList<>();

            final short timeInterval = tourData.getDeviceTimeInterval();

            short absolutePulse = tourData.getStartPulse();
            //short absoluteAltitude = tourData.getStartAltitude();

            int sumDistance = 0;
            int sumPulse = 0;
            int sumCadence = 0;

            short temperature;
            short marker;
            short cadence;

            boolean isFirstDataRecord = true;
            boolean isCCRecord = false;

            // loop: all records in current tour
            while (true) {

               // read BB or CC record
               file.read(recordBuffer);

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

               TimeData timeData;

               // get all slices in the current record (BB or CC)
               for (int dataIndex = 0; dataIndex < dataLength; dataIndex++) {

                  if (isFirstDataRecord) {

                     /*
                      * create the START time slice, the current slice is the first slice
                      * which already contains data
                      */

                     isFirstDataRecord = false;

                     timeDataList.add(timeData = new TimeData());

                     timeData.time = 0;
                     timeData.pulse = tourData.getStartPulse();
                     timeData.altitude = tourData.getStartAltitude();
                     timeData.temperature = temperature;
                     timeData.cadence = cadence;
                     timeData.distance = 0;
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
                     // this is the last time slice within the whole tour
                     timeData.time = cadence % 20;
                  } else {
                     timeData.time = timeInterval;
                  }

                  // read data for the current time slice
                  DeviceReaderTools.readTimeSlice(
                        DeviceReaderTools.get2ByteData(recordBuffer, 4 + (2 * dataIndex)),
                        timeData);

                  // adjust pulse from relative to absolute
                  timeData.pulse = absolutePulse += timeData.pulse;

                  // adjust altitude from relative to absolute
                  //absoluteAltitude += timeData.altitude;

                  sumDistance += timeData.distance;
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
            file.read(recordBuffer);
            if ((recordBuffer[0] & 0xFF) != 0xDD) {
               break;
            }

            /*
             * save DD record data
             */
            readDDRecord(recordBuffer, tourData);

            /*
             * disable data series when no data are available
             */
            if (timeDataList.size() > 0) {

               final TimeData firstTimeData = timeDataList.get(0);
               if (sumDistance == 0) {
                  firstTimeData.distance = Float.MIN_VALUE;
               }
               if (sumPulse == 0) {
                  firstTimeData.pulse = Float.MIN_VALUE;
               }
               if (sumCadence == 0) {
                  firstTimeData.cadence = Float.MIN_VALUE;
               }
            }

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
               tourData.setTourDeviceTime_Recorded(tourData.getTourDeviceTime_Elapsed());
               tourData.computeTourMovingTime();
               tourData.computeComputedValues();
            }

            // dump DD block
            // dumpBlock(file, recordBuffer);
            //
            // System.out.println(UI.EMPTY_STRING);
            // System.out.println(UI.EMPTY_STRING);

            /*
             * make sure not to end in an endless loop where the current DD offset is the same
             * as the first DD offset (this seems to be unlikely but it happend already 2 Month
             * after the first implementation)
             */
            if (offsetDDRecord == initialOffsetDDRecord) {
               returnValue = true;
               break;
            }

            /*
             * prevent endless loops by checking if the loop is run twice
             */
            if (checkedOffset1 > offsetDDRecord) {
               // first loop has not yet ended
               checkedOffset1 = offsetDDRecord;
            } else {
               if (checkedOffset2 > offsetDDRecord) {
                  // second loop has not yet ended
                  checkedOffset2 = offsetDDRecord;
               } else {
                  StatusUtil
                        .showStatus(new Exception(NLS.bind(Messages.Import_Error_EndlessLoop, importFilePath)));
                  returnValue = true;
                  break;
               }
            }

            offsetDDRecord = adjustDDRecordOffset(offsetAARecordInDDRecord);
         }

      } catch (final IOException e) {
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
         deviceData.transferYear = (short) _fileDate.get(Calendar.YEAR);
         deviceData.transferMonth = (short) firstTourMonth;
         deviceData.transferDay = (short) firstTourDay;
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
      startBlock.minute = buffer[4];
      startBlock.hour = buffer[5];
      startBlock.day = buffer[6];
      startBlock.month = buffer[7];

      tourData.setStartDistance((int) DeviceReaderTools.get4ByteData(buffer, 8));
      tourData.setStartAltitude((short) DeviceReaderTools.get2ByteData(buffer, 12));
      tourData.setStartPulse((short) (buffer[14] & 0xff));

      return startBlock;
   }

   private void readDDRecord(final byte[] buffer, final TourData tourData) {

      // 00 1 0xDD
      // 01 1 ?
      //
      // 02 2 address of the AA record
      //
      // 04 4 ?
      //
      // 08 2 tour distance
      // 10 2 tour altitude up
      // 12 2 tour altitude down
      //
      // 14 1 ?
      // 15 1 0xFF

      tourData.setTourAltUp(DeviceReaderTools.get2ByteData(buffer, 10));
      tourData.setTourAltDown(DeviceReaderTools.get2ByteData(buffer, 12));

// System.out.println("UP: "+DeviceReaderTools.get2ByteData(buffer, 10));
   }

   /**
    * checks if the data file has a valid HAC5 data format
    *
    * @return true for a valid HAC5 data format
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
         if (bufferHeader[4] != HAC5_HARDWARE_ID) {
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

      } catch (final NumberFormatException | FileNotFoundException e) {
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
