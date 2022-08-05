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
package device.nmea;

import java.util.HashMap;

import net.tourbook.data.TourData;
import net.tourbook.device.nmea.NmeaDataReader;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;

public class NmeaDataReaderTests {

   private static final String            FILES_PATH = FilesUtils.rootPath + "device/nmea/files/"; //$NON-NLS-1$

   private static DeviceData              deviceData;
   private static HashMap<Long, TourData> newlyImportedTours;
   private static HashMap<Long, TourData> alreadyImportedTours;
   private static NmeaDataReader          nmeaDataReader;

   @BeforeAll
   static void initAll() {
      deviceData = new DeviceData();
      newlyImportedTours = new HashMap<>();
      alreadyImportedTours = new HashMap<>();
      nmeaDataReader = new NmeaDataReader();
   }

   @AfterEach
   void tearDown() {
      newlyImportedTours.clear();
      alreadyImportedTours.clear();
   }

   @Test
   void testNmeaImportBasic() {

      final String filePath = FILES_PATH + "NMEAExample"; //$NON-NLS-1$
      final String testFilePath = FilesUtils.getAbsoluteFilePath(filePath + ".txt");//$NON-NLS-1$

      nmeaDataReader.processDeviceData(testFilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process());

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, filePath);
   }
}
