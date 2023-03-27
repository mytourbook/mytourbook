/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.device.crp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;

import net.tourbook.common.UI;
import net.tourbook.common.util.FileUtils;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.ui.tourChart.ChartLabelMarker;

public class CRPDataReader extends TourbookDevice {

   private int _fileVersion;

   // plugin constructor
   public CRPDataReader() {}

   @Override
   public String buildFileNameFromRawData(final String rawDataFileName) {
      return null;
   }

   @Override
   public boolean checkStartSequence(final int byteIndex, final int newByte) {
      return false;
   }

   /**
    * Set marker when a comment is set
    */
   private void createMarker(final TourData tourData,
                             final int tpIndex,
                             final int tourTime,
                             final int distance,
                             final int altitude,
                             final String comment,
                             final TimeData timeData) {

      if (tpIndex >= 0 && StringUtils.hasContent(comment)) {

         timeData.marker = 1;

         // create a new marker
         final TourMarker tourMarker = new TourMarker(tourData, ChartLabelMarker.MARKER_TYPE_DEVICE);
         tourMarker.setLabel(comment);
         tourMarker.setTime(tourTime, Long.MIN_VALUE);
         tourMarker.setDistance(distance);
         tourMarker.setSerieIndex(tpIndex);
         tourMarker.setAltitude(altitude);

         tourData.getTourMarkers().add(tourMarker);
      }
   }

   /**
    * Set TimeData
    */
   private TimeData createTimeData(final int pulse,
                                   final int distance,
                                   final int temperature,
                                   final int oldDistance,
                                   final String[] dataStrings,
                                   final short altitudeDiff,
                                   final long trackpointTimeDiff) {

      final TimeData timeData = new TimeData();

      timeData.altitude = altitudeDiff;
      if (_fileVersion > 9 && dataStrings.length > 9) {
         timeData.cadence = Integer.parseInt(dataStrings[9]);
      }
      timeData.distance = (distance - oldDistance) * 1f;
      timeData.pulse = pulse;
      timeData.temperature = temperature;
      timeData.time = (int) trackpointTimeDiff;

      return timeData;
   }

   /**
    * Disable data series when no data are available
    */
   private void disableDataSeries(final ArrayList<TimeData> timeDataList,
                                  final int sumAltitude,
                                  final int sumCadence,
                                  final int sumDistance,
                                  final int sumPulse,
                                  final int sumTemperature) {

      if (timeDataList.size() > 0) {

         final TimeData firstTimeData = timeDataList.get(0);

         if (sumAltitude == 0) {
            firstTimeData.altitude = Float.MIN_VALUE;
         }
         if (sumCadence == 0) {
            firstTimeData.cadence = Float.MIN_VALUE;
         }
         if (sumDistance == 0) {
            firstTimeData.distance = Float.MIN_VALUE;
         }
         if (sumPulse == 0) {
            firstTimeData.pulse = Float.MIN_VALUE;
         }
         if (sumTemperature == 0) {
            firstTimeData.temperature = Float.MIN_VALUE;
         }
      }
   }

   @Override
   public String getDeviceModeName(final int profileId) {

      // 0: Run
      // 1: Ski
      // 2: Bike
      // 3: Ski/Bike
      // 4: Altitude

      switch (profileId) {
      case 0:
         return Messages.CRP_Profile_run;

      case 1:
         return Messages.CRP_Profile_ski;

      case 2:
         return Messages.CRP_Profile_bike;

      case 3:
         return Messages.CRP_Profile_ski_bike;

      case 4:
         return Messages.CRP_Profile_altitude;

      default:
         break;
      }

      return Messages.CRP_Profile_unknown;
   }

   @Override
   public SerialParameters getPortParameters(final String portName) {
      return null;
   }

   /**
    * @return If valid, the file path of the raw CRP file
    */
   private String getRawFilePath(final String importFilePath) {

      String fileContent = FileUtils.readFileContentString(importFilePath);
      String createTemporaryFile = null;

      // Check that the .crp file starts with the correct header
      if (isFileHeaderValid(fileContent)) {

         createTemporaryFile = importFilePath;
      } else {

         //If it doesn't, it might be a compressed crp file and will need to be decompressed first

         createTemporaryFile = FileUtils.createTemporaryFile("temporaryCRP", "crp"); //$NON-NLS-1$ //$NON-NLS-2$
         ZLibCompressionHelper.decompress(new File(importFilePath), new File(createTemporaryFile));

         fileContent = FileUtils.readFileContentString(createTemporaryFile);
         return isFileHeaderValid(fileContent) ? createTemporaryFile : null;
      }

      return createTemporaryFile;
   }

   @Override
   public int getStartSequenceSize() {
      return -1;
   }

   @Override
   public int getTransferDataSize() {
      return -1;
   }

   private TourData importTour(final BufferedReader fileReader) throws IOException {

      final ArrayList<String> allTrackPoints = new ArrayList<>();

      // File header
      @SuppressWarnings("unused")
      final String fileHeader = fileReader.readLine();

      StringTokenizer tokenLine = new StringTokenizer(fileReader.readLine());
      _fileVersion = Integer.parseInt(tokenLine.nextToken());

      String line;
      // get all trackpoints
      while ((line = fileReader.readLine()) != null && !line.equals("***")) { //$NON-NLS-1$
         allTrackPoints.add(line);
      }

      // skip line
      // 4006  568   563   5040  1676  134   162   431   83 68 195   18,0  23,0  0  02:06:00 19,08
      fileReader.readLine();

      /*
       * Line: date/time
       */
      // 16.06.2004 -  18:53:00 - 02:23:24 - 3  - 2  - Tour 2
      tokenLine = new StringTokenizer(fileReader.readLine());

      // tour start date
      final String tourStartDate = tokenLine.nextToken();
      final int tourYear = Integer.parseInt(tourStartDate.substring(6));
      final int tourMonth = Integer.parseInt(tourStartDate.substring(3, 5));
      final int tourDay = Integer.parseInt(tourStartDate.substring(0, 2));

      // tour start time
      final String tourStartTime = tokenLine.nextToken();

      final int tourHour = Integer.parseInt(tourStartTime.substring(0, 2));
      int tourMinute;
      int tourSecond;

      final boolean isSecondsAvailable = tourStartTime.length() > 5;
      if (isSecondsAvailable) {
         tourMinute = Integer.parseInt(tourStartTime.substring(3, 5));
         tourSecond = Integer.parseInt(tourStartTime.substring(6));
      } else {
         tourMinute = Integer.parseInt(tourStartTime.substring(3));
         tourSecond = 0;
      }

      // elapsed time
      tokenLine.nextToken();

      // category
      tokenLine.nextToken();

      // difficulty
      tokenLine.nextToken();

      // tour name
      String tourName = UI.EMPTY_STRING;
      if (tokenLine.hasMoreTokens()) {
         tourName = tokenLine.nextToken(UI.TAB1);
      }

      // skip lines
      fileReader.readLine();
      fileReader.readLine();

      /*
       * Line: interval/mode
       */
      tokenLine = new StringTokenizer(fileReader.readLine());
      final int interval = Integer.parseInt(tokenLine.nextToken());
      final int tourMode = Integer.parseInt(tokenLine.nextToken());

      // skip empty lines
      fileReader.readLine();
      fileReader.readLine();

      // skip lines
      fileReader.readLine();
      fileReader.readLine();

      /*
       * lines: tour description
       */
      final StringBuilder tourDescription = new StringBuilder();
      while ((line = fileReader.readLine()) != null) {
         tourDescription.append(line + "\n"); //$NON-NLS-1$
      }

      final LocalDateTime dtTrackPoint_Previous = LocalDateTime.of(
            tourYear,
            tourMonth,
            tourDay,
            tourHour,
            tourMinute,
            tourSecond,
            0);

      /*
       * Set tour data
       */
      final TourData tourData = new TourData();

      tourData.setTourStartTime(tourYear, tourMonth, tourDay, tourHour, tourMinute, 0);

      tourData.setTourTitle(tourName);
      tourData.setTourDescription(tourDescription.toString().trim());

      tourData.setDeviceMode((short) (tourMode));
      tourData.setDeviceModeName(getDeviceModeName(tourMode));

      tourData.setDeviceTimeInterval((short) interval);

      // tourData.setStartDistance((int) DeviceReaderTools.get4ByteData(buffer, 8));
      // tourData.setStartAltitude((short) DeviceReaderTools.get2ByteData(buffer, 12));
      // tourData.setStartPulse((short) (buffer[14] & 0xff));

      setTimeSeriesFromTrackpoints(allTrackPoints, tourStartDate, interval, dtTrackPoint_Previous, tourData);

      // after all data are added, the tour id can be created
      final int tourDistance = (int) Math.abs(tourData.getStartDistance());
      final String uniqueId = createUniqueId_Legacy(tourData, tourDistance);
      tourData.createTourId(uniqueId);

      return tourData;
   }

   private boolean isFileHeaderValid(final String fileHeader) {

      return StringUtils.hasContent(fileHeader) && fileHeader.startsWith("HRMProfilDatas"); //$NON-NLS-1$
   }

   @Override
   public void processDeviceData(final String importFilePath,
                                 final DeviceData deviceData,
                                 final Map<Long, TourData> alreadyImportedTours,
                                 final Map<Long, TourData> newlyImportedTours,
                                 final ImportState_File importState_File,
                                 final ImportState_Process importState_Process) {

      final String rawFilePath = getRawFilePath(importFilePath);
      if (!StringUtils.hasContent(rawFilePath)) {
         return;
      }

      try (BufferedReader fileReader = new BufferedReader(new FileReader(rawFilePath))) {

         final TourData tourData = importTour(fileReader);

         tourData.setImportFilePath(importFilePath);
         final Long tourId = tourData.getTourId();

         // check if the tour is in the tour map
         if (alreadyImportedTours.containsKey(tourId) == false) {

            // add new tour to the map
            newlyImportedTours.put(tourId, tourData);

            // create additional data
            tourData.setTourDeviceTime_Recorded(tourData.getTourDeviceTime_Elapsed());
            tourData.computeTourMovingTime();
            tourData.computeComputedValues();

            tourData.completeTourMarkerWithRelativeTime();
         }

         importState_File.isFileImportedWithValidData = true;

      } catch (final Exception e) {

         e.printStackTrace();
      }
   }

   private void setTimeSeriesFromTrackpoints(final ArrayList<String> allTrackPoints,
                                             final String tourStartDate,
                                             final int interval,
                                             LocalDateTime dtTrackPoint_Previous,
                                             final TourData tourData) {

      int tpIndex = 0;
      int tourTime = 0;

      int pulse;
      int distance = 0;
      int altitude;
      int temperature;
      String trackpointTime;

      int oldDistance = 0;
      int oldAltitude = 0;
      int tourAltUp = 0;
      int tourAltDown = 0;

      int sumAltitude = 0;
      int sumCadence = 0;
      int sumDistance = 0;
      int sumPulse = 0;
      int sumTemperature = 0;

      final ArrayList<TimeData> timeDataList = new ArrayList<>();

      LocalDate tourDate = LocalDate.parse(tourStartDate, DateTimeFormatter.ofPattern("dd.MM.yyyy")); //$NON-NLS-1$

      for (final String trackPoint : allTrackPoints) {

         /*
          * Parse track point line
          */
         final String[] dataStrings = trackPoint.split(UI.TAB1);

         pulse = Integer.parseInt(dataStrings[0]);
         distance = Integer.parseInt(dataStrings[2]) * 10; //    [m]
         altitude = Integer.parseInt(dataStrings[3]); //         [m]
         temperature = Math.round(Float.parseFloat(dataStrings[6].replace(',', '.'))); // [C]
         trackpointTime = dataStrings[7];

         String comment = UI.EMPTY_STRING;
         if (dataStrings.length > 8) {
            comment = dataStrings[8];
         }

         /*
          * Prepare TimeData
          */
         final short altitudeDiff = (short) (altitude - oldAltitude);

         final int trackPoint_Hour = Integer.parseInt(trackpointTime.substring(0, 2));
         final int trackPoint_Minute = Integer.parseInt(trackpointTime.substring(3, 5));
         final int trackPoint_Second = Integer.parseInt(trackpointTime.substring(6));

         if (trackpointTime.equals("00:00:00")) { //$NON-NLS-1$
            tourDate = tourDate.plusDays(1);
         }

         final LocalDateTime dtTrackPoint = LocalDateTime.of(
               tourDate.getYear(),
               tourDate.getMonth(),
               tourDate.getDayOfMonth(),
               trackPoint_Hour,
               trackPoint_Minute,
               trackPoint_Second,
               0);

         final long trackpointTimeDiff = Duration.between(dtTrackPoint_Previous, dtTrackPoint).toSeconds();
         dtTrackPoint_Previous = dtTrackPoint;

         final TimeData timeData = createTimeData(pulse,
               distance,
               temperature,
               oldDistance,
               dataStrings,
               altitudeDiff,
               trackpointTimeDiff);
         timeDataList.add(timeData);

         createMarker(tourData, tpIndex, tourTime, distance, altitude, comment, timeData);

         // first altitude contains the start altitude and not the difference
         if (tpIndex != 0) {
            tourAltUp += ((altitudeDiff > 0) ? altitudeDiff : 0);
            tourAltDown -= ((timeData.altitude < 0) ? timeData.altitude : 0);
         }

         oldDistance = distance;
         oldAltitude = altitude;

         // prepare next interval
         tourTime += interval;
         tpIndex++;

         sumAltitude += Math.abs(altitude);
         sumCadence += timeData.cadence;
         sumDistance += timeData.distance;
         sumPulse += pulse;
         sumTemperature += Math.abs(temperature);
      }

      /*
       * set the start distance, this is not available in a .crp file but it's required to
       * create the tour-id
       */
      tourData.setStartDistance(distance);

      disableDataSeries(timeDataList, sumAltitude, sumCadence, sumDistance, sumPulse, sumTemperature);

      tourData.setDeviceId(deviceId);
      tourData.setDeviceName(visibleName);

      tourData.createTimeSeries(timeDataList, false);

      tourData.setTourAltUp(tourAltUp);
      tourData.setTourAltDown(tourAltDown);
   }

   /**
    * Check if the data file has a valid .crp data format
    *
    * @return true for a valid .crp data format
    */
   @Override
   public boolean validateRawData(final String fileName) {

      return StringUtils.hasContent(getRawFilePath(fileName));
   }

}
