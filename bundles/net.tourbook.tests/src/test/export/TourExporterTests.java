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
package export;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.export.ExportTourGPX;
import net.tourbook.export.ExportTourTCX;
import net.tourbook.export.TourExporter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;
import utils.Initializer;

public class TourExporterTests {

   private static final String FILES_PATH           = FilesUtils.rootPath + "export/files/"; //$NON-NLS-1$
   private static final String _testTourFilePathTcx = FILES_PATH + "TCXExport.tcx";          //$NON-NLS-1$
   private static final String _testTourFilePathGpx = FILES_PATH + "GPXExport.gpx";          //$NON-NLS-1$

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

      net.tourbook.common.util.FileUtils.deleteIfExists(Paths.get(_testTourFilePathTcx));
      net.tourbook.common.util.FileUtils.deleteIfExists(Paths.get(_testTourFilePathGpx));
   }

   private void executeGpxTest(final String controlTourFileName) {

      _tourExporter.export(_testTourFilePathGpx);

      final List<String> nodesToFilter = Arrays.asList("Cadence", "mt:tourType", "mt:tourDistance"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      final List<String> attributesToFilter = Arrays.asList("creator"); //$NON-NLS-1$
      Comparison.compareXmlAgainstControl(FILES_PATH + controlTourFileName,
            _testTourFilePathGpx,
            nodesToFilter,
            attributesToFilter);
   }

   private void executeTcxTest(final String controlTourFileName) {

      _tourExporter.export(_testTourFilePathTcx);

      final List<String> nodesToFilter = Arrays.asList("Cadence", "Author", "Creator"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      final List<String> attributesToFilter = new ArrayList<>();
      Comparison.compareXmlAgainstControl(FILES_PATH + controlTourFileName,
            _testTourFilePathTcx,
            nodesToFilter,
            attributesToFilter);
   }

   private void initializeTourExporterGpx() {

      _tourExporter = new TourExporter(ExportTourGPX.GPX_1_0_TEMPLATE).useTourData(_tour);
      _tourExporter.setActivityType(_tour.getTourType().getName());
   }

   private void initializeTourExporterTcx() {

      _tourExporter = new TourExporter(ExportTourTCX.TCX_2_0_TEMPLATE).useTourData(_tour);
      _tourExporter.setActivityType(_tour.getTourType().getName());
   }

   @Test
   void testGpxExportAllOptions() {

      initializeTourExporterGpx();

      final String controlTourFileName = "LongsPeak-AllOptions-RelativeDistance.gpx"; //$NON-NLS-1$

      _tourExporter.setUseDescription(true);
      _tourExporter.setIsExportAllTourData(true);
      _tourExporter.setIsExportSurfingWaves(true);
      _tourExporter.setIsExportWithBarometer(true);
      _tourExporter.setIsCamouflageSpeed(true);
      _tourExporter.setCamouflageSpeed(15 / 3.6f); // 15km/h
      _tour.setBodyWeight(77.7f); // 77.7kg
      _tour.setCalories(1282000); // 1282 kcal / 1282000 calories

      executeGpxTest(controlTourFileName);
   }

   @Test
   void testGpxExportDescriptionAndActivity() {

      initializeTourExporterGpx();

      final String controlTourFileName = "LongsPeak-AbsoluteDistance.gpx"; //$NON-NLS-1$

      _tourExporter.setUseAbsoluteDistance(true);

      executeGpxTest(controlTourFileName);
   }

   @Test
   void testTcxExportCamouflage15KmHBikingActivity() {

      initializeTourExporterTcx();

      _tourExporter.setUseActivityType(true);
      _tourExporter.setActivityType("Biking"); //$NON-NLS-1$
      _tourExporter.setIsCamouflageSpeed(true);
      _tourExporter.setCamouflageSpeed(15 / 3.6f);

      final String controlTourFileName = "LongsPeak-CamouflageSpeed-15kmh-BikingActivity.tcx"; //$NON-NLS-1$
      executeTcxTest(controlTourFileName);
   }

   @Test
   void testTcxExportCourse() {

      initializeTourExporterTcx();

      _tourExporter.setIsCourse(true);
      _tourExporter.setCourseName("Longs Peak"); //$NON-NLS-1$

      final String controlTourFileName = "LongsPeak-Course.tcx"; //$NON-NLS-1$
      executeTcxTest(controlTourFileName);
   }

   @Test
   void testTcxExportDescriptionAndActivity() {

      initializeTourExporterTcx();

      _tourExporter.setUseDescription(true);
      _tourExporter.setUseActivityType(true);
      _tourExporter.setIsExportAllTourData(true);

      final String controlTourFileName = "LongsPeak-Description-RunningActivity.tcx"; //$NON-NLS-1$
      executeTcxTest(controlTourFileName);
   }

   @Test
   void testTcxExportHikingActivity() {

      initializeTourExporterTcx();

      _tourExporter.setUseActivityType(true);
      _tourExporter.setActivityType("Hiking"); //$NON-NLS-1$

      final String controlTourFileName = "LongsPeak-HikingActivity.tcx"; //$NON-NLS-1$
      executeTcxTest(controlTourFileName);
   }
}
