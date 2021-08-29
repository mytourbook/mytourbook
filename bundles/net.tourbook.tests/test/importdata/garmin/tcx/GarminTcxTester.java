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
package importdata.garmin.tcx;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;

import net.tourbook.data.TourData;
import net.tourbook.device.garmin.GarminTCX_DeviceDataReader;
import net.tourbook.device.garmin.GarminTCX_SAXHandler;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import utils.Comparison;
import utils.Initializer;

public class GarminTcxTester {

   private static SAXParser               parser;
   private static final String            IMPORT_PATH = "/importdata/garmin/tcx/files/"; //$NON-NLS-1$

   private static DeviceData              deviceData;
   private static HashMap<Long, TourData> newlyImportedTours;
   private static HashMap<Long, TourData> alreadyImportedTours;
   private static GarminTCX_DeviceDataReader  deviceDataReader;

   @BeforeAll
   static void initAll() {
      parser = Initializer.initializeParser();
      deviceData = new DeviceData();
      newlyImportedTours = new HashMap<>();
      alreadyImportedTours = new HashMap<>();
      deviceDataReader = new GarminTCX_DeviceDataReader();
   }

   @AfterEach
   void tearDown() {
      newlyImportedTours.clear();
      alreadyImportedTours.clear();
   }

   /**
    * Regression test
    *
    * @throws IOException
    * @throws SAXException
    */
   @Test
   void testTcxImportConeyLake() throws SAXException, IOException {

      final String filePathWithoutExtension = IMPORT_PATH +
            "Move_2020_05_23_08_55_42_Trail+running"; //$NON-NLS-1$
      final String importFilePath = filePathWithoutExtension + ".tcx"; //$NON-NLS-1$
      final InputStream tcxFile = GarminTcxTester.class.getResourceAsStream(importFilePath);

      final GarminTCX_SAXHandler handler = new GarminTCX_SAXHandler(
            deviceDataReader,
            importFilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File());

      parser.parse(tcxFile, handler);

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, "test" + filePathWithoutExtension); //$NON-NLS-1$
   }

   /**
    * TCX file with pauses
    */
   @Test
   void testTcxImportLyons() throws SAXException, IOException {

      final String filePathWithoutExtension = IMPORT_PATH + "2021-01-31"; //$NON-NLS-1$
      final String importFilePath = filePathWithoutExtension + ".tcx"; //$NON-NLS-1$
      final InputStream tcxFile = GarminTcxTester.class.getResourceAsStream(importFilePath);

      final GarminTCX_SAXHandler handler = new GarminTCX_SAXHandler(
            deviceDataReader,
            importFilePath,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File());

      parser.parse(tcxFile, handler);

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, "test" + filePathWithoutExtension); //$NON-NLS-1$
   }
}
