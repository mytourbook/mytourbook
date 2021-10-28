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
package exportdata.garmin.tcx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.export.ExportTourTCX;
import net.tourbook.export.TourExporter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;
import utils.Initializer;

public class ExportTcxTests {

   private static final String IMPORT_PATH       = FilesUtils.rootPath + "exportdata/garmin/tcx/files/"; //$NON-NLS-1$
   private static final String _testTourFilePath = IMPORT_PATH + "TCXExport.tcx";                        //$NON-NLS-1$

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

      _tourExporter = new TourExporter(ExportTourTCX.TCX_2_0_TEMPLATE).useTourData(_tour);
      _tourExporter.setActivityType(_tour.getTourType().getName());
   }

   private void executeTest(final String controlTourFileName) {

      _tourExporter.export(_testTourFilePath);

      final List<String> nodesToFilter = Arrays.asList("Cadence", "Author", "Creator"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      final List<String> attributesToFilter = new ArrayList<>();
      Comparison.compareXmlAgainstControl(IMPORT_PATH + controlTourFileName,
            _testTourFilePath,
            nodesToFilter,
            attributesToFilter);
   }

   @Test
   void testTcxExportCamouflage15KmHBikingActivity() {

      final String controlTourFileName = "LongsPeak-CamouflageSpeed-15kmh-BikingActivity.tcx"; //$NON-NLS-1$

      _tourExporter.setUseActivityType(true);
      _tourExporter.setActivityType("Biking"); //$NON-NLS-1$
      _tourExporter.setIsCamouflageSpeed(true);
      _tourExporter.setCamouflageSpeed(15 / 3.6f);

      executeTest(controlTourFileName);
   }

   @Test
   void testTcxExportCourse() {

      final String controlTourFileName = "LongsPeak-Course.tcx"; //$NON-NLS-1$

      _tourExporter.setIsCourse(true);
      _tourExporter.setCourseName("Longs Peak"); //$NON-NLS-1$

      executeTest(controlTourFileName);
   }

   @Test
   void testTcxExportDescriptionAndActivity() {

      final String controlTourFileName = "LongsPeak-Description-RunningActivity.tcx"; //$NON-NLS-1$

      _tourExporter.setUseDescription(true);
      _tourExporter.setUseActivityType(true);
      _tourExporter.setIsExportAllTourData(true);

      executeTest(controlTourFileName);
   }

   @Test
   void testTcxExportHikingActivity() {

      final String controlTourFileName = "LongsPeak-HikingActivity.tcx"; //$NON-NLS-1$

      _tourExporter.setUseActivityType(true);
      _tourExporter.setActivityType("Hiking"); //$NON-NLS-1$

      executeTest(controlTourFileName);
   }
}
