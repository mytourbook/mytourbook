/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.device.polar.hrm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.device.gpx.GPXDeviceDataReader;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.TourTypeWrapper;
import net.tourbook.importdata.TourbookDevice;

/**
 * This device reader is importing data from Polar Person File
 */
public class Polar_PPD_Data {

   private static final String     DATA_DELIMITER         = "\t";              //$NON-NLS-1$

   private static final String     SECTION_PERSON_INFO    = "[PersonInfo]";    //$NON-NLS-1$
   private static final String     SECTION_SPORTS_INFO    = "[PersonSports]";  //$NON-NLS-1$
   private static final String     SECTION_PERSON_HRZONES = "[PersonHRZones]"; //$NON-NLS-1$

   private HashMap<Integer, Sport> _sports;
   private Person                  _person;

   private String                  _importFilePath;

   private Map<Long, TourData>     _alreadyImportedTours;
   private Map<Long, TourData>     _newlyImportedTours;

   private ImportState_File        _importState_File;
   private ImportState_Process     _importState_Process;

// private Polar_PPD_DataReader    _polar_PPD_DataReader;
   private Polar_PDD_DataReader _polar_PDD_DataReader_Silent;

   private DeviceData           _deviceData;

   @SuppressWarnings("unused")
   private class Person {

      private String firstName;
      private String lastName;
      private int    birthDate;
      private int    gender;   // 0: female; 1: male
      private int    size;
      private int    maxHr;
      private int    minHr;
      private int    vo2max;
      private String dataPath;
   }

   class SilentGPXDeviceDataReader extends GPXDeviceDataReader {

   }

   class SilentPolarHRMDateReader extends Polar_HRM_DataReader {

      @Override
      public void processDeviceData(final String importFilePath,
                                    final DeviceData deviceData,
                                    final Map<Long, TourData> alreadyImportedTours,
                                    final Map<Long, TourData> newlyImportedTours,
                                    final ImportState_File importState_File,
                                    final ImportState_Process importState_Process) {

         importState_Process.setIsSilentError(true);

         super.processDeviceData(
               importFilePath,
               deviceData,
               alreadyImportedTours,
               newlyImportedTours,

               importState_File,
               importState_Process);
      }
   }

   class SilentPolarPDDDataReader extends Polar_PDD_DataReader {

      @Override
      protected TourbookDevice getGPXDeviceDataReader() {
         return new SilentGPXDeviceDataReader();
      }

      @Override
      protected TourbookDevice getPolarHRMDataReader() {
         return new SilentPolarHRMDateReader();
      }
   }

   @SuppressWarnings("unused")
   private class Sport {

      private int    id;
      private String name;
      private int    color;

      public Sport(final int id, final String name, final int color) {

         this.id = id;
         this.name = name;
         this.color = color;
      }
   }

   // plugin constructor
   public Polar_PPD_Data(final String importFilePath,
                         final Map<Long, TourData> alreadyImportedTours,
                         final Map<Long, TourData> newlyImportedTours,

                         final ImportState_File importState_File,
                         final ImportState_Process importState_Process,

                         final Polar_PPD_DataReader polar_PPD_DataReader,
                         final DeviceData deviceData) {

      _importFilePath = importFilePath;
      _alreadyImportedTours = alreadyImportedTours;
      _newlyImportedTours = newlyImportedTours;

      _importState_File = importState_File;
      _importState_Process = importState_Process;

//      _polar_PPD_DataReader = polar_PPD_DataReader;
      _deviceData = deviceData;

      _polar_PDD_DataReader_Silent = new SilentPolarPDDDataReader();
   }

   private HashMap<Integer, TourType> addAllMissingSportTypes() {

      final HashMap<Integer, TourType> tourTypeForSport = new HashMap<>();

      for (final int sportId : _sports.keySet()) {

         final Sport sport = _sports.get(sportId);

         final TourTypeWrapper tourTypeWrapper = RawDataManager.setTourType(null, sport.name);

         if (tourTypeWrapper != null) {

            tourTypeForSport.put(sportId, tourTypeWrapper.tourType);

            if (tourTypeWrapper.isNewTourType) {
               _importState_Process.isCreated_NewTag().set(true);
            }
         }
      }

      return tourTypeForSport;
   }

   private int getTourSport(final Long tourId) {

      final HashMap<Long, Integer> tourSportMap = _importState_File.getTourSportMap();

      if (tourSportMap.containsKey(tourId)) {
         return tourSportMap.get(tourId);
      } else {
         return -1;
      }
   }

   public void parseSection() {

      // parse this person file
      if (!parseSection(_importFilePath)) {
         return;
      }

      // check all sports and create TourTypes if the are not there
      final HashMap<Integer, TourType> tourTypeForSport = addAllMissingSportTypes();

      // read all exercise file names for this person
      final File dataDir = new File(_person.dataPath);
      if (!dataDir.isDirectory()) {
         return;
      }

      // first find all years containing data
      final File[] directoriesToProcess = dataDir.listFiles(new FilenameFilter() {
         @Override
         public boolean accept(final File dir, final String name) {
            return name.matches("\\d\\d\\d\\d"); // 4-digit year //$NON-NLS-1$
         }
      });

      // than load all exercises for all ppd files for all year into tourDataMap
      for (final File dir : directoriesToProcess) {

         if (dir.isDirectory()) {

            final File[] pddFiles = dir.listFiles(new FilenameFilter() {
               @Override
               public boolean accept(final File dir, final String name) {
                  return name.matches(".*\\.pdd$"); // 4-digit year //$NON-NLS-1$
               }
            });

            // load all exercises from that pdd file
            for (final File pddFile : pddFiles) {

               _polar_PDD_DataReader_Silent.processDeviceData(

                     pddFile.getAbsolutePath(),
                     _deviceData,
                     _alreadyImportedTours,
                     _newlyImportedTours,

                     _importState_File,
                     _importState_Process);
            }

            for (final Long tourId : _newlyImportedTours.keySet()) {

               final int sportId = getTourSport(tourId);
               if (sportId > 0) {
                  final TourType type = tourTypeForSport.get(sportId);
                  if (null != type) {
                     _newlyImportedTours.get(tourId).setTourType(type);
                  }
               }
            }
         }
      }

      _importState_File.isFileImportedWithValidData = true;
   }

   private boolean parseSection(final String importFilePath) {

      boolean returnValue = false;

      // the default charset has not handled correctly the german umlaute in uppercase on Linux/OSX
      try (InputStreamReader inputStream = new InputStreamReader(new FileInputStream(importFilePath), UI.ISO_8859_1);
            BufferedReader fileReader = new BufferedReader(inputStream)) {

         String line;
         while ((line = fileReader.readLine()) != null) {

            boolean isValid = true;

            if (line.startsWith(SECTION_PERSON_INFO)) {

               isValid = parseSection10PersonInfo(fileReader);

            } else if (line.startsWith(SECTION_SPORTS_INFO)) {

               isValid = parseSection20SportsInfo(fileReader);

            } else if (line.startsWith(SECTION_PERSON_HRZONES)) {

               isValid = parseSection30PersonHrZones(fileReader);

            }

            if (isValid == false) {
               return false;
            }
         }

         returnValue = true;

      } catch (final Exception e) {
         StatusUtil.showStatus(e);
         return false;
      }

      return returnValue;
   }

   /**
    * <pre>
    *
    * [PersonInfo]
    * 100         1           6           6            4           256    // row0
    * 19650504   1 gender    168 size    1            0           3      // row1
    * 22         1           11          51           0           1      // row2
    * 34         20          30          0           -1           0      // row3
    * 185 maxHr  0           0           50 minHr     620 vo2max  0      // row4
    * 0          0           10          0            0           0      // row5
    * 0          0           0           0            0           0      // row6
    * Matthias                                                           // row7
    * Helmling                                                           // row8
    *                                                                    // row9
    * C:\Program Files\Polar\Polar ProTrainer\Matthias Helmling          // row10
    * </pre>
    *
    * @param fileReader
    * @return
    * @throws IOException
    */
   private boolean parseSection10PersonInfo(final BufferedReader fileReader) throws IOException {

      try {

         String line;
         StringTokenizer tokenLine;

         _person = new Person();

         if (null == fileReader.readLine()) {
            return false; // row0
         }

         if (null == (line = fileReader.readLine())) { // row1
            return false;
         }
         tokenLine = new StringTokenizer(line, DATA_DELIMITER);

         _person.birthDate = Integer.parseInt(tokenLine.nextToken());
         _person.gender = Integer.parseInt(tokenLine.nextToken());
         _person.size = Integer.parseInt(tokenLine.nextToken());

         if (null == fileReader.readLine()) { // row2
            return false;
         }
         if (null == fileReader.readLine()) { // row3
            return false;
         }

         if (null == (line = fileReader.readLine())) { // row4
            return false;
         }
         tokenLine = new StringTokenizer(line, DATA_DELIMITER);

         _person.maxHr = Integer.parseInt(tokenLine.nextToken());
         tokenLine.nextToken();
         tokenLine.nextToken();
         _person.minHr = Integer.parseInt(tokenLine.nextToken());
         _person.vo2max = Integer.parseInt(tokenLine.nextToken());

         if (null == fileReader.readLine()) { // row5
            return false;
         }
         if (null == fileReader.readLine()) { // row6
            return false;
         }

         if (null == (line = fileReader.readLine())) { // row7
            return false;
         }
         _person.firstName = line;

         if (null == (line = fileReader.readLine())) { // row8
            return false;
         }
         _person.lastName = line;

         if (null == fileReader.readLine()) { // row9
            return false;
         }

         if (null == (line = fileReader.readLine())) { // row10
            return false;
         }
         _person.dataPath = line;

      } catch (final Exception e) {
         StatusUtil.log(e);
         return false;
      }

      return true;
   }

   /**
    * <pre>
    *
    * [PersonSports]
    * 100   3   2   6   4   256              // row0
    * 12    0   0   0   0   0                // row1
    * 1     0   0   0   0   0                // row2
    * 1     0   0   100   1   0              // row3 sub1
    * 185   10354102   0   34   1   0        // row4 sub2
    * Running                                // row5 sub3
    * R                                      // row6 sub4
    *                                        // row7 sub5
    *                                        // row7 sub6
    * 5     0   0   100    0   0              //     sub1
    * 0     5876991   0   34   1   0          //     sub2
    * Intervall
    * INT
    * </pre>
    *
    * @param fileReader
    * @return
    * @throws IOException
    */
   private boolean parseSection20SportsInfo(final BufferedReader fileReader) {

      StringTokenizer tokenLine;
      String line;
      int numberOfSports;

      _sports = new HashMap<>();

      try {

// row 0
         if (null == fileReader.readLine()) {
            return false;
         }

// row 1
         if (null == (line = fileReader.readLine())) {
            return false;
         }
         tokenLine = new StringTokenizer(line, DATA_DELIMITER);

         numberOfSports = Integer.parseInt(tokenLine.nextToken());

// row 2
         if (null == fileReader.readLine()) {
            return false;
         }

// row 3 + n*6

         // parse all sports sub-sections
         while (null != (line = fileReader.readLine()) && numberOfSports > 0) {

            tokenLine = new StringTokenizer(line, DATA_DELIMITER);
            final int id = Integer.parseInt(tokenLine.nextToken());

// subrow 2
            if (null == (line = fileReader.readLine())) {
               return false;
            }
            tokenLine = new StringTokenizer(line, DATA_DELIMITER);
            tokenLine.nextToken();
            final int color = Integer.parseInt(tokenLine.nextToken());

// subrow 3
            if (null == (line = fileReader.readLine())) {
               return false;
            }
            final String name = line;

// subrow 4,5,6
            if (skipRows(fileReader, 3) == null) {
               return false;
            }

            _sports.put(id, new Sport(id, name, color));

            numberOfSports--;
         }

         return true;

      } catch (final Exception e) {
         StatusUtil.showStatus(e);
         return false;
      }
   }

   private boolean parseSection30PersonHrZones(final BufferedReader fileReader) {
      return true;
   }

   private String skipRows(final BufferedReader fileReader, final int numberOfRows) throws IOException {

      int rowCounter = 0;

      String line = null;
      while (rowCounter < numberOfRows) {

         line = fileReader.readLine();

         if (line == null) {
            return null;
         }

         rowCounter++;
      }

      return line;
   }

}
