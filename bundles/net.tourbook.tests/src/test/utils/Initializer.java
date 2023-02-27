/*******************************************************************************
 * Copyright (C) 2020, 2023 Frédéric Bard
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

import java.time.ZonedDateTime;
import java.util.HashMap;

import javax.persistence.Persistence;

import net.tourbook.common.time.TimeTools;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.device.garmin.fit.FitDataReader;
import net.tourbook.device.gpx.GPXDeviceDataReader;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;

public class Initializer {

   public static TourData createManualTour() {

      final TourData manualTour = new TourData();
      final ZonedDateTime zonedDateTime = ZonedDateTime.of(
            2022,
            1,
            3,
            17,
            16,
            0,
            0,
            TimeTools.UTC);
      manualTour.setTourStartTime(zonedDateTime);
      manualTour.setTourDistance(10);
      manualTour.setTourDeviceTime_Elapsed(3600);
      manualTour.setTourTitle("Manual Tour"); //$NON-NLS-1$

      final TourType tourType = new TourType();
      tourType.setName("Running"); //$NON-NLS-1$
      manualTour.setTourType(tourType);

      return manualTour;
   }

   public static TourData importTour() {

      return importTour_GPX(FilesUtils.rootPath + "/utils/files/LongsPeak-Manual.gpx"); //$NON-NLS-1$
   }

   public static TourData importTour_FIT(final String importFilePath) {

      final HashMap<Long, TourData> newlyImportedTours = new HashMap<>();
      final HashMap<Long, TourData> alreadyImportedTours = new HashMap<>();
      final FitDataReader fitDataReader = new FitDataReader();

      fitDataReader.processDeviceData(importFilePath,
            null,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process());

      return Comparison.retrieveImportedTour(newlyImportedTours);
   }

   public static TourData importTour_GPX(final String importFilePath) {

      final HashMap<Long, TourData> newlyImportedTours = new HashMap<>();
      final HashMap<Long, TourData> alreadyImportedTours = new HashMap<>();
      final GPXDeviceDataReader deviceDataReader = new GPXDeviceDataReader();
      final String testFilePath = FilesUtils.getAbsoluteFilePath(importFilePath);

      deviceDataReader.processDeviceData(testFilePath,
            null,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process());

      return Comparison.retrieveImportedTour(newlyImportedTours);
   }

   public static void initializeDatabase() {

      Persistence.createEntityManagerFactory("tourdatabase").createEntityManager(); //$NON-NLS-1$
   }
}
