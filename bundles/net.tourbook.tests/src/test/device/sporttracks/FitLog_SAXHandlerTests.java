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
package device.sporttracks;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;

import net.tourbook.data.TourData;
import net.tourbook.device.sporttracks.FitLogDeviceDataReader;
import net.tourbook.device.sporttracks.FitLog_SAXHandler;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import utils.Comparison;
import utils.FilesUtils;
import utils.Initializer;

public class FitLog_SAXHandlerTests {

   private static final String            IMPORT_PATH = "/device/sporttracks/fitlog/files/"; //$NON-NLS-1$

   private static SAXParser               parser;
   private static HashMap<Long, TourData> newlyImportedTours;
   private static HashMap<Long, TourData> alreadyImportedTours;
   private static FitLogDeviceDataReader  deviceDataReader;

   @BeforeAll
   static void initAll() {

      Initializer.initializeDatabase();
      parser = Initializer.initializeParser();
      newlyImportedTours = new HashMap<>();
      alreadyImportedTours = new HashMap<>();
      deviceDataReader = new FitLogDeviceDataReader();
   }

   @AfterEach
   void tearDown() {

      newlyImportedTours.clear();
      alreadyImportedTours.clear();
   }

   @Test
   void testImportTimothyLake() throws SAXException, IOException {

      final String filePathWithoutExtension = IMPORT_PATH + "TimothyLake"; //$NON-NLS-1$
      final String importFilePath = filePathWithoutExtension + ".fitlog"; //$NON-NLS-1$
      final InputStream fitLogFile = FitLog_SAXHandlerTests.class.getResourceAsStream(importFilePath);

      final FitLog_SAXHandler handler = new FitLog_SAXHandler(importFilePath,
            alreadyImportedTours,
            newlyImportedTours,
            false,
            new ImportState_File(),
            new ImportState_Process(),
            deviceDataReader);

      parser.parse(fitLogFile, handler);

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, FilesUtils.rootPath + filePathWithoutExtension);
   }
}
