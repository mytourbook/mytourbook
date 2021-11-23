/*******************************************************************************
 * Copyright (C) 2020, 2021 Frédéric Bard
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
package device.suunto;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.device.suunto.Suunto9_DeviceDataReader;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;

class Suunto9_DeviceDataReaderTests {

   private static final String             IMPORT_FILE_PATH = FilesUtils.rootPath + "device/suunto/files/"; //$NON-NLS-1$

   private static final String             JSON_GZ          = ".json.gz";                                        //$NON-NLS-1$

   private static DeviceData               deviceData;
   private static HashMap<Long, TourData>  newlyImportedTours;
   private static HashMap<Long, TourData>  alreadyImportedTours;
   private static Suunto9_DeviceDataReader deviceDataReader;

   @BeforeAll
   static void initAll() {
      deviceData = new DeviceData();
      newlyImportedTours = new HashMap<>();
      alreadyImportedTours = new HashMap<>();
      deviceDataReader = new Suunto9_DeviceDataReader();
   }

   /**
    * Used only for unit tests, it retrieves the last processed activity.
    *
    * @return If any, the last processed activity.
    */
   private TourData getLastTourDataImported() {

      Field field = null;
      try {
         field = Suunto9_DeviceDataReader.class.getDeclaredField("_processedActivities"); //$NON-NLS-1$
         field.setAccessible(true);

         @SuppressWarnings("unchecked")
         final HashMap<TourData, ArrayList<TimeData>> processedActivities = (HashMap<TourData, ArrayList<TimeData>>) field
               .get(deviceDataReader);
         final Iterator<Entry<TourData, ArrayList<TimeData>>> it = processedActivities.entrySet().iterator();
         TourData lastTourData = null;
         while (it.hasNext()) {
            lastTourData = it.next().getKey();
         }

         return lastTourData;
      } catch (final NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
         e.printStackTrace();
      }

      return null;
   }

   @AfterEach
   void tearDown() {

      newlyImportedTours.clear();
      alreadyImportedTours.clear();
   }

   /**
    * City of Rocks, ID
    */
   @Test
   void testImportCityOfRocks() {

      final String filePath = IMPORT_FILE_PATH + "1537365846902_183010004848_post_timeline-1"; //$NON-NLS-1$

      final String testFilePath = FilesUtils.getAbsoluteFilePath(filePath + JSON_GZ);
      deviceDataReader.processDeviceData(testFilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process().setIsJUnitTest(true));

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, filePath);
   }

   /**
    * Maxwell, CO
    */
   @Test
   void testImportMaxwell1() {

      final String filePath = IMPORT_FILE_PATH + "Original-1536723722706_183010004848_post_timeline-1"; //$NON-NLS-1$

      final String testFilePath = FilesUtils.getAbsoluteFilePath(filePath + JSON_GZ);
      deviceDataReader.processDeviceData(testFilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process().setIsJUnitTest(true));

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, filePath);
   }

   /**
    * Reservoir Ridge with MoveSense HR belt (R-R data)
    */
   @Test
   void testImportRRData() {

      final String filePath = IMPORT_FILE_PATH + "1549250450458_183010004848_post_timeline-1"; //$NON-NLS-1$

      final String testFilePath = FilesUtils.getAbsoluteFilePath(filePath + JSON_GZ);
      deviceDataReader.processDeviceData(testFilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process().setIsJUnitTest(true));

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, filePath);
   }

   /**
    * Shoreline - with laps/markers
    */
   @Test
   void testImportShoreLineWithLaps() {

      final String filePath = IMPORT_FILE_PATH + "1555291925128_183010004848_post_timeline-1"; //$NON-NLS-1$

      final String testFilePath = FilesUtils.getAbsoluteFilePath(filePath + JSON_GZ);
      deviceDataReader.processDeviceData(testFilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process().setIsJUnitTest(true));

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, filePath);
   }

   /**
    * Unit tests for Suunto Spartan/9 split files. Because the files are split,
    * it causes small discrepancies in the data in between each file. Also,
    * because the import creates a marker at the end of the activity if markers
    * are present, it can create additional markers at the end of each file.
    * Note: We only test the case where the files come in the proper order
    * (1-2-3) because the function {@see RawDataManager#runImport} will sort
    * the files.
    *
    * @return
    */
   @Test
   void testImportSplitFiles() {

      // Maxwell, CO (Split manually)

      // File #1
      final String maxWell1FilePath = IMPORT_FILE_PATH + "1536723722706_183010004848_post_timeline-1.json.gz"; //$NON-NLS-1$

      // File #2
      final String maxWell2FilePath = IMPORT_FILE_PATH + "1536723722706_183010004848_post_timeline-2.json.gz"; //$NON-NLS-1$

      // File #3
      final String maxWell3FilePath = IMPORT_FILE_PATH + "1536723722706_183010004848_post_timeline-3.json.gz"; //$NON-NLS-1$

      // File control
      final String controlDocumentPath = IMPORT_FILE_PATH + "1536723722706_183010004848_post_timeline-1-SplitTests"; //$NON-NLS-1$

      // ORDER 1 - 2 - 3

      deviceDataReader.processDeviceData(maxWell1FilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process().setIsJUnitTest(true));

      deviceDataReader.processDeviceData(maxWell2FilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process().setIsJUnitTest(true));

      deviceDataReader.processDeviceData(maxWell3FilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process().setIsJUnitTest(true));

      final TourData entry = getLastTourDataImported();
      Comparison.compareTourDataAgainstControl(entry, controlDocumentPath);
   }

   /**
    * Start -> 100m -> LAP -> LAP -> 100m -> LAP -> LAP -> 100m -> LAP -> LAP
    * -> 100m -> Stop (courtesy of Z74)
    */
   @Test
   void testImportSwimming1() {

      final String filePath = IMPORT_FILE_PATH + "1547628896209_184710003036_post_timeline-1"; //$NON-NLS-1$

      final String testFilePath = FilesUtils.getAbsoluteFilePath(filePath + JSON_GZ);
      deviceDataReader.processDeviceData(testFilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process().setIsJUnitTest(true));

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, filePath);
   }

   /**
    * Start -> 100m -> Stop (courtesy of Z74)
    */
   @Test
   void testImportSwimming2() {

      final String filePath = IMPORT_FILE_PATH + "1547628897243_184710003036_post_timeline-1"; //$NON-NLS-1$

      final String testFilePath = FilesUtils.getAbsoluteFilePath(filePath + JSON_GZ);
      deviceDataReader.processDeviceData(testFilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process().setIsJUnitTest(true));

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, filePath);
   }

   /**
    * Pauses lasting less then 1000ms should not be imported
    */
   @Test
   void testImportTinyPause() {

      final String filePath = IMPORT_FILE_PATH + "1594598677631_183010004848_post_timeline-1"; //$NON-NLS-1$

      final String testFilePath = FilesUtils.getAbsoluteFilePath(filePath + JSON_GZ);
      deviceDataReader.processDeviceData(testFilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process().setIsJUnitTest(true));

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, filePath);
   }
}
