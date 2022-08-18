/*******************************************************************************
 * Copyright (C) 2020, 2022 Frédéric Bard
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
package device.sporttracks;

import java.io.IOException;

import net.tourbook.data.TourData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import utils.Comparison;
import utils.FilesUtils;

class FitLogEx_DeviceDataReaderTests extends FitLog_DeviceDataReaderTester {

   private static final String IMPORT_PATH = FilesUtils.rootPath + "device/sporttracks/fitlogex/files/"; //$NON-NLS-1$

   private void testImportFile(final String fileName) {

      final String filePathWithoutExtension = IMPORT_PATH + fileName;
      final String importFilePath = filePathWithoutExtension + ".fitlogEx"; //$NON-NLS-1$
      final String importFileAbsolutePath = FilesUtils.getAbsoluteFilePath(importFilePath);

      deviceDataReader.processDeviceData(importFileAbsolutePath,
            null,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process());

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      // set relative path that it works with different OS
      tour.setImportFilePath(importFilePath);

      Comparison.compareTourDataAgainstControl(tour, filePathWithoutExtension);
   }

   /**
    * This tests parses a file for which the time offset of -7 hours is wrong
    * <TimeZoneUtcOffset>-25200</TimeZoneUtcOffset> as it is located in the MST
    * zone (-6h or -21600). However, the start time is correct and needs to be
    * kept.
    *
    * @throws SAXException
    * @throws IOException
    */
   @Test
   void testImportParkCity() {

      testImportFile("ParkCity");
   }

   @Test
   void testImportTimothyLake() {

      testImportFile("TimothyLake");
   }
}
