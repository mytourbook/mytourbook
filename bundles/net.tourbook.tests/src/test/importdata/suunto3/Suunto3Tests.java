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
package importdata.suunto3;

import java.nio.file.Paths;
import java.util.HashMap;

import net.tourbook.data.TourData;
import net.tourbook.device.suunto.Suunto3_DeviceDataReader;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;

class Suunto3Tests {

	private static final String IMPORT_FILE_PATH = FilesUtils.rootPath + "importdata/suunto3/files/"; //$NON-NLS-1$

   private static DeviceData               deviceData;
   private static HashMap<Long, TourData>  newlyImportedTours;
   private static HashMap<Long, TourData>  alreadyImportedTours;
   private static Suunto3_DeviceDataReader deviceDataReader;

   @BeforeAll
   static void initAll() {
      deviceData = new DeviceData();
      newlyImportedTours = new HashMap<>();
      alreadyImportedTours = new HashMap<>();
      deviceDataReader = new Suunto3_DeviceDataReader();
   }

   @AfterEach
   void tearDown() {
      newlyImportedTours.clear();
      alreadyImportedTours.clear();
   }

   /**
    * Forest Park, OR with laps
    */
   @Test
   void testImportForestParkLaps() {

      final String filePath = IMPORT_FILE_PATH + "597F0A5112001700-2016-08-27T15_45_41-0"; //$NON-NLS-1$

      final String testFilePath = Paths.get(filePath + ".sml").toAbsolutePath().toString(); //$NON-NLS-1$

      deviceDataReader.processDeviceData(testFilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process());

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, filePath);
   }

   /**
    * Timothy Lake, OR
    */
   @Test
   void testImportTimothyLake() {

      final String filePath = IMPORT_FILE_PATH + "F783095113000500-2015-05-31T09_51_13-0"; //$NON-NLS-1$

      final String testFilePath = Paths.get(filePath + ".sml").toAbsolutePath().toString(); //$NON-NLS-1$

      deviceDataReader.processDeviceData(testFilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process());

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, filePath);
   }
}
