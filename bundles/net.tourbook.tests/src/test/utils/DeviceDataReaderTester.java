/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;

import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.TourbookDevice;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

public abstract class DeviceDataReaderTester {

   private static HashMap<Long, TourData> newlyImportedTours;

   @BeforeAll
   static void initAll() {

      Initializer.initializeDatabase();
      newlyImportedTours = new HashMap<>();
   }

   @AfterEach
   void tearDown() {

      newlyImportedTours.clear();
   }

   protected void testImportFile(final TourbookDevice tourbookDevice, final String filePathWithoutExtension, final String extension) {

      final String importFilePath = filePathWithoutExtension + extension;
      final String importFileAbsolutePath = FilesUtils.getAbsoluteFilePath(importFilePath);

      assertTrue(tourbookDevice.validateRawData(importFileAbsolutePath));

      tourbookDevice.processDeviceData(importFileAbsolutePath,
            new DeviceData(),
            new HashMap<>(),
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process());

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, filePathWithoutExtension);
   }
}
