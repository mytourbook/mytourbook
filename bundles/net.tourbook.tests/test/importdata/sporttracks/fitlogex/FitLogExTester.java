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
package importdata.sporttracks.fitlogex;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;

import net.tourbook.data.TourData;
import net.tourbook.device.sporttracks.FitLogDeviceDataReader;
import net.tourbook.device.sporttracks.FitLogSAXHandler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import utils.Comparison;
import utils.Initializer;

class FitLogExTester {

   private static final String            IMPORT_PATH = "/importdata/sporttracks/fitlogex/files/"; //$NON-NLS-1$

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
   void testImportParkCity() throws SAXException, IOException {
      final String filePathWithoutExtension = IMPORT_PATH +
				"ParkCity"; //$NON-NLS-1$
      final String importFilePath = filePathWithoutExtension + ".fitlogEx"; //$NON-NLS-1$
      final InputStream fitLogExFile = FitLogExTester.class.getResourceAsStream(importFilePath);

      final FitLogSAXHandler handler = new FitLogSAXHandler(
            deviceDataReader,
            importFilePath,
            alreadyImportedTours,
            newlyImportedTours,
            true);

      parser.parse(fitLogExFile, handler);

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, "test/" + filePathWithoutExtension); //$NON-NLS-1$
   }

	@Test
   void testImportTimothyLake() throws SAXException, IOException {
      final String filePathWithoutExtension = IMPORT_PATH +
            "TimothyLake"; //$NON-NLS-1$
      final String importFilePath = filePathWithoutExtension + ".fitlogEx"; //$NON-NLS-1$
      final InputStream fitLogExFile = FitLogExTester.class.getResourceAsStream(importFilePath);

      final FitLogSAXHandler handler = new FitLogSAXHandler(
            deviceDataReader,
            importFilePath,
            alreadyImportedTours,
            newlyImportedTours,
            true);

      parser.parse(fitLogExFile, handler);

      final TourData tour = Comparison.retrieveImportedTour(newlyImportedTours);

      Comparison.compareTourDataAgainstControl(tour, "test/" + filePathWithoutExtension); //$NON-NLS-1$
   }
}
