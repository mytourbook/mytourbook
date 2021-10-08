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
package importdata.garmin.fit;

import java.nio.file.Paths;
import java.util.HashMap;

import net.tourbook.data.TourData;
import net.tourbook.device.garmin.fit.FitDataReader;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;

public class GarminFitTester {

   private static final String            IMPORT_FILE_PATH = "test/importdata/garmin/fit/files/"; //$NON-NLS-1$

   private static DeviceData              deviceData;
   private static HashMap<Long, TourData> newlyImportedTours;
   private static HashMap<Long, TourData> alreadyImportedTours;
   private static FitDataReader           fitDataReader;

   @BeforeAll
   static void initAll() {
      deviceData = new DeviceData();
      newlyImportedTours = new HashMap<>();
      alreadyImportedTours = new HashMap<>();
      fitDataReader = new FitDataReader();
   }

   @AfterEach
   void tearDown() {
      newlyImportedTours.clear();
      alreadyImportedTours.clear();
   }

   /**
    * Regression test. This test can be useful when updating the FIT SDK and ensuring that the FIT
    * import still works as expected.
    */
   @Test
   void testFitImportConeyLake() {

      final String filePath = IMPORT_FILE_PATH + "ConeyLakeMove_2020_05_23_08_55_42_Trail+running"; //$NON-NLS-1$

      final String testFilePath = Paths.get(filePath + ".fit").toAbsolutePath().toString(); //$NON-NLS-1$
      fitDataReader.processDeviceData(testFilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process());

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, filePath);
   }

   /**
    * Test to ensure that files without pauses have the recorded time equal to the elapsed time
    */
   @Test
   void testFitImportNoPauses() {

      final String filePath = IMPORT_FILE_PATH + "1-30-21 3-47 PM"; //$NON-NLS-1$

      final String testFilePath = Paths.get(filePath + ".fit").toAbsolutePath().toString(); //$NON-NLS-1$
      fitDataReader.processDeviceData(testFilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process());

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, filePath);
   }
}
