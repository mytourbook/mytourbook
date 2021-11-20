/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.device.csv.tours;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourTypeWrapper;
import net.tourbook.importdata.TourbookDevice;

public class CSVTourDataReader extends TourbookDevice {

   //
   // csv import data samples
   //
   // Date (yyyy-mm-dd); Time (hh-mm); Duration (sec); Paused Time (sec), Distance (m); Title; Comment; Tour Type; Tags;
   // 2008-09-02;08-20;1200;300;8500;zur Arbeit;kein Kommentar, siehe nächste Tour;Rennvelo;Arbeitsfahrt am Abend, new tag
   // 2008-09-01;14-30;1500;20;6000;auf Fremersberg;;MTB;FB
   // 2008-08-28;18-00;780;120;12000;Feierabendrunde;;TestTourType;no tags

   private static final String TOUR_CSV_ID   =

         "Date (yyyy-mm-dd); Time (hh-mm); Duration (sec); Paused Time (sec), Distance (m); Title; Comment; Tour Type; Tags;"; //$NON-NLS-1$

   /**
    * This header is a modified header for {@link #TOUR_CSV_ID} with a semikolon instead of a komma
    * after
    * <p>
    * <i>Paused Time (sec);</i>
    */
   private static final String TOUR_CSV_ID_2 =

         "Date (yyyy-mm-dd); Time (hh-mm); Duration (sec); Paused Time (sec); Distance (m); Title; Comment; Tour Type; Tags;"; //$NON-NLS-1$
//       "Date (yyyy-mm-dd); Time (hh-mm); Duration (sec); Paused Time (sec), Distance (m); Title; Comment; Tour Type; Tags;"; //$NON-NLS-1$
//                                                                        -> <-
   /**
    *
    */
   private static final String TOUR_CSV_ID_3       = UI.EMPTY_STRING

         + "Date (yyyy-mm-dd); Time (hh-mm); Duration (sec); Paused Time (sec); Distance (m); Title; Comment; Tour Type; Tags;"              //$NON-NLS-1$
         + " Altitude Up (m); Altitude Down (m);";                                                                                           //$NON-NLS-1$

   private static final String CSV_TOKEN_SEPARATOR = ";";                                                                                    //$NON-NLS-1$
   private static final String CSV_TAG_SEPARATOR   = ",";                                                                                    //$NON-NLS-1$

   private class DateTimeData {
      public int year;
      public int month;
      public int day;
      public int hour;
      public int minute;
   }

   public CSVTourDataReader() {
      // plugin constructor
   }

   @Override
   public String buildFileNameFromRawData(final String rawDataFileName) {
      return null;
   }

   @Override
   public boolean checkStartSequence(final int byteIndex, final int newByte) {
      return false;
   }

   @Override
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

   @Override
   public int getTransferDataSize() {
      return -1;
   }

   private boolean isFileValid(final String fileHeader) {

      final boolean isId1 = fileHeader.startsWith(TOUR_CSV_ID);
      final boolean isId2 = fileHeader.startsWith(TOUR_CSV_ID_2);
      final boolean isId3 = fileHeader.startsWith(TOUR_CSV_ID_3);

      if (isId1 || isId2 || isId3) {
         return true;
      }

      return false;
   }

   private void parseDate(final DateTimeData dateTime, final String nextToken) {

      // Date (yyyy-mm-dd)
      dateTime.year = parseInteger(nextToken.substring(0, 4));
      dateTime.month = parseInteger(nextToken.substring(5, 7));
      dateTime.day = parseInteger(nextToken.substring(8, 10));
   }

   /**
    * @param string
    * @return Returns parsed integer value or 0 when the string argument do not contain a valid
    *         value.
    */
   private int parseInteger(final String string) {

      try {
         return Integer.parseInt(string);
      } catch (final Exception e) {
         return 0;
      }
   }

   /**
    * @param tourData
    * @param tagToken
    * @return <code>true</code> when a new tag is created
    */
   private boolean parseTags(final TourData tourData, final String tagToken) {

      final StringTokenizer tagTokenizer = new StringTokenizer(tagToken, CSV_TAG_SEPARATOR);
      final Set<String> allTourTagNames = new HashSet<>();
      try {

         String tagLabel;

         // loop: all tag labels
         while ((tagLabel = tagTokenizer.nextToken()) != null) {

            tagLabel = tagLabel.strip();

            allTourTagNames.add(tagLabel);
         }

      } catch (final NoSuchElementException e) {

         // no further tokens
      }

      if (allTourTagNames.size() == 0) {

         // nothing to do

         return false;
      }

      return RawDataManager.setTourTags(tourData, allTourTagNames);
   }

   private void parseTime(final DateTimeData dateTime, final String nextToken) {

      // Time (hh-mm)
      dateTime.hour = parseInteger(nextToken.substring(0, 2));
      dateTime.minute = parseInteger(nextToken.substring(3, 5));
   }

   /**
    * @param tourData
    * @param parsedTourTypeLabel
    * @return Returns <code>true</code> when a new {@link TourType} was created
    */
   private boolean parseTourType(final TourData tourData, final String parsedTourTypeLabel) {

      final TourTypeWrapper tourTypeWrapper = RawDataManager.setTourType(tourData, parsedTourTypeLabel);

      return tourTypeWrapper != null && tourTypeWrapper.isNewTourType;
   }

   @Override
   public void processDeviceData(final String importFilePath,
                                 final DeviceData deviceData,
                                 final Map<Long, TourData> alreadyImportedTours,
                                 final Map<Long, TourData> newlyImportedTours,
                                 final ImportState_File importState_File,
                                 final ImportState_Process importState_Process) {

      boolean isNewTag = false;
      boolean isNewTourType = false;

      try (FileReader fileReader = new FileReader(importFilePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader)) {

         // check file header
         final String fileHeader = bufferedReader.readLine();
         if (isFileValid(fileHeader) == false) {
            return;
         }
         final boolean isId3 = fileHeader.startsWith(TOUR_CSV_ID_3);

         String tokenLine;

         // read all tours, each line is one tour
         while ((tokenLine = bufferedReader.readLine()) != null) {

            int distance = 0;
            int duration = 0;
            int pausedTime = 0;

            final TourData tourData = new TourData();

            try {

               final DateTimeData dateTime = new DateTimeData();

               /*
                * The split method is used because the Tokenizer ignores empty tokens !!!
                */

               final String[] allToken = tokenLine.split(CSV_TOKEN_SEPARATOR, 20);

               parseDate(dateTime, allToken[0]); //                           1 Date (yyyy-mm-dd);
               parseTime(dateTime, allToken[1]); //                           2 Time (hh-mm);
               tourData.setTourStartTime(
                     dateTime.year,
                     dateTime.month,
                     dateTime.day,
                     dateTime.hour,
                     dateTime.minute,
                     0);

               duration = parseInteger(allToken[2]); //                       3 Duration (sec);
               tourData.setTourDeviceTime_Elapsed(duration);

               pausedTime = parseInteger(allToken[3]); //                     4 Paused Time (sec),
               tourData.setTourDeviceTime_Paused(pausedTime);

               final int recordedTime = Math.max(0, duration - pausedTime);
               tourData.setTourDeviceTime_Recorded(recordedTime);
               tourData.setTourComputedTime_Moving(recordedTime);

               distance = parseInteger(allToken[4]);//                        5 Distance (m);
               tourData.setTourDistance(distance);

               tourData.setTourTitle(allToken[5]);//                          6 Title;
               tourData.setTourDescription(allToken[6]);//                    7 Comment;

               isNewTourType |= parseTourType(tourData, allToken[7]);//       8 Tour Type;
               isNewTag |= parseTags(tourData, allToken[8]);//                9 Tags;

               if (isId3) {
                  tourData.setTourAltUp(parseInteger(allToken[9]));//         10 Altitude Up (m);
                  tourData.setTourAltDown(parseInteger(allToken[10]));//      11 Altitude Down (m);
               }

            } catch (final NoSuchElementException e) {

               // not all tokens are defined

            } finally {

               tourData.setImportFilePath(importFilePath);

               tourData.setDeviceId(deviceId);
               tourData.setDeviceName(visibleName);

               // after all data are added, the tour id can be created
               final String uniqueId = createUniqueId_Legacy(tourData, distance);
               final Long tourId = tourData.createTourId(uniqueId);

               // check if the tour is in the tour map
               if (alreadyImportedTours.containsKey(tourId) == false) {

                  // add new tour to the map
                  newlyImportedTours.put(tourId, tourData);

                  importState_File.isFileImportedWithValidData = true;
               }
            }
         }

      } catch (final Exception e) {

         StatusUtil.log(e);

         return;

      } finally {

         if (isNewTag) {
            importState_Process.isCreated_NewTag().set(true);
         }

         if (isNewTourType) {
            importState_Process.isCreated_NewTourType().set(true);
         }
      }
   }

   /**
    * checks if the data file has a valid .crp data format
    *
    * @return true for a valid .crp data format
    */
   @Override
   public boolean validateRawData(final String fileName) {

      try (FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader)) {

         final String fileHeader = bufferedReader.readLine();
         if (fileHeader == null || isFileValid(fileHeader) == false) {
            return false;
         }

      } catch (final IOException e) {
         e.printStackTrace();
      }

      return true;
   }

}
