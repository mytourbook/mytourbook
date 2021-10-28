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
package exportdata.gpx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.export.ExportTourGPX;
import net.tourbook.export.TourExporter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;
import utils.Initializer;

public class ExportGpxTests {

   private static final String IMPORT_PATH       = FilesUtils.rootPath + "exportdata/gpx/files/"; //$NON-NLS-1$
   private static final String _testTourFilePath = IMPORT_PATH + "GPXExport.gpx";                 //$NON-NLS-1$

   private static TourData     _tour;
   private TourExporter        _tourExporter;

   @BeforeAll
   static void initAll() {

      _tour = Initializer.importTour();
      final TourType tourType = new TourType();
      tourType.setName("Running"); //$NON-NLS-1$
      _tour.setTourType(tourType);
   }

   @AfterEach
   void afterEach() {

      if (!Files.exists(Paths.get(_testTourFilePath))) {
         return;
      }
      try {
         Files.delete(Paths.get(_testTourFilePath));
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   @BeforeEach
   void beforeEach() {

      _tourExporter = new TourExporter(ExportTourGPX.GPX_1_0_TEMPLATE).useTourData(_tour);
      _tourExporter.setActivityType(_tour.getTourType().getName());
   }

   private void executeTest(final String controlTourFileName) {

      _tourExporter.export(_testTourFilePath);

      final List<String> nodesToFilter = Arrays.asList("Cadence", "mt:tourType", "mt:tourDistance"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      final List<String> attributesToFilter = Arrays.asList("creator"); //$NON-NLS-1$
      Comparison.compareXmlAgainstControl(IMPORT_PATH + controlTourFileName,
            _testTourFilePath,
            nodesToFilter,
            attributesToFilter);
   }

   @Test
   void testGpxExportAllOptions() {

      final String controlTourFileName = "LongsPeak-AllOptions-RelativeDistance.gpx"; //$NON-NLS-1$

      _tourExporter.setUseDescription(true);
      _tourExporter.setIsExportAllTourData(true);
      _tourExporter.setIsExportSurfingWaves(true);
      _tourExporter.setIsExportWithBarometer(true);
      _tourExporter.setIsCamouflageSpeed(true);
      _tourExporter.setCamouflageSpeed(15 / 3.6f); // 15km/h
      _tour.setBodyWeight(77.7f); // 77.7kg
      _tour.setCalories(1282000); // 1282 kcal / 1282000 calories

      executeTest(controlTourFileName);
   }

   @Test
   void testGpxExportDescriptionAndActivity() {

      final String controlTourFileName = "LongsPeak-AbsoluteDistance.gpx"; //$NON-NLS-1$

      _tourExporter.setUseAbsoluteDistance(true);

      executeTest(controlTourFileName);
   }
}
