/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
/**
 * @author Alfred Barten
 */
package net.tourbook.srtm;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;

import net.tourbook.common.UI;

public final class ElevationSRTM3 extends ElevationBase {

   private static final String ELEVATION_ID         = "SRTM3";    //$NON-NLS-1$

   /**
    * Example for a file name
    * <p>
    * https://e4ftl01.cr.usgs.gov/MEASURES/SRTMGL3.003/2000.02.11/N47E008.SRTMGL3.hgt.zip
    */
   private static final String SRTM3_FILE_NAME_PART = ".SRTMGL3"; //$NON-NLS-1$
   private static final String SRTM3_FILE_EXTENSION = ".hgt";     //$NON-NLS-1$

   // default initial 16 Files
   private static final HashMap<Integer, SRTM3ElevationFile> _srtmElevationFilesCache = new HashMap<>();

   private static SRTM3ElevationFile                         _srtm3ElevationFile;

   private class SRTM3ElevationFile {

      ElevationFile __elevationFile;

      private SRTM3ElevationFile(final GeoLat lat, final GeoLon lon) {

         final String srtm3DataPath = getElevationData_FilePath("srtm3"); //$NON-NLS-1$

         final String degreeNorthSouth = NumberForm.n2(lat.direction == GeoLat.DIRECTION_NORTH
               ? lat.degrees
               : lat.degrees + 1);

         final String degreeEastWest = NumberForm.n3(lon.direction == GeoLon.DIRECTION_EAST
               ? lon.degrees
               : lon.degrees + 1);

         final String localFilePath = new String(UI.EMPTY_STRING

               + srtm3DataPath
               + File.separator
               + lat.direction + degreeNorthSouth // e.g. N20
               + lon.direction + degreeEastWest //   e.g. W018
               + SRTM3_FILE_NAME_PART
               + SRTM3_FILE_EXTENSION

         );

         final String localFilePathUnzipped = new String(UI.EMPTY_STRING

               + srtm3DataPath
               + File.separator
               + lat.direction + degreeNorthSouth // e.g. N20
               + lon.direction + degreeEastWest //   e.g. W018
               + SRTM3_FILE_EXTENSION

         );

         try {
            __elevationFile = new ElevationFile(localFilePath, localFilePathUnzipped, ElevationType.SRTM3);
         } catch (final Exception e) {
            e.printStackTrace();
         }
      }

      private float getElevation(final GeoLat lat, final GeoLon lon) {

         final int srtmFileOffset = srtmFileOffset(lat, lon);

         return __elevationFile.get(srtmFileOffset);
      }

      // Offset in the SRTM3-File
      private int srtmFileOffset(final GeoLat lat, final GeoLon lon) {

         if (lat.direction == GeoLat.DIRECTION_SOUTH) {
            if (lon.direction == GeoLon.DIRECTION_EAST) {

               // SOUTH - EAST

               return 1201
                     * (lat.minutes * 20 + lat.seconds / 3)
                     + lon.minutes * 20
                     + lon.seconds / 3;
            } else {

               // SOUTH - WEST

               return 1201
                     * (lat.minutes * 20 + lat.seconds / 3)
                     + 1200
                     - lon.minutes * 20
                     - lon.seconds / 3;
            }
         } else {

            if (lon.direction == GeoLon.DIRECTION_EAST) {

               // NORTH -EAST

               return 1201
                     * (1200 - lat.minutes * 20 - lat.seconds / 3)
                     + lon.minutes * 20
                     + lon.seconds / 3;
            } else {

               // NORTH - WEST

               return 1201
                     * (1200 - lat.minutes * 20 - lat.seconds / 3)
                     + 1200
                     - lon.minutes * 20
                     - lon.seconds / 3;
            }
         }
      }
   }

   public ElevationSRTM3() {

      gridLat.setDegreesMinutesSecondsDirection(0, 0, 3, 'N');
      gridLon.setDegreesMinutesSecondsDirection(0, 0, 3, 'E');
   }

   /**
    * Clears the file cache by closing and removing all evaluation files
    */
   @SuppressWarnings("unused")
   private static synchronized void clearElevationFileCache() {

      // close all files
      for (final Entry<Integer, SRTM3ElevationFile> entry : _srtmElevationFilesCache.entrySet()) {
         entry.getValue().__elevationFile.close();
      }

      _srtmElevationFilesCache.clear();
   }

   @Override
   public float getElevation(final GeoLat lat, final GeoLon lon) {

      if (lat.tertias != 0) {
         return getElevationGrid(lat, lon);
      }
      if (lon.tertias != 0) {
         return getElevationGrid(lat, lon);
      }
      if (lat.seconds % 3 != 0) {
         return getElevationGrid(lat, lon);
      }
      if (lon.seconds % 3 != 0) {
         return getElevationGrid(lat, lon);
      }

      int i = lon.degrees;
      if (lon.direction == GeoLon.DIRECTION_WEST) {
         i += 256;
      }
      i *= 1024;
      i += lat.degrees;
      if (lat.direction == GeoLat.DIRECTION_SOUTH) {
         i += 256;
      }

      final Integer ii = Integer.valueOf(i);
      _srtm3ElevationFile = _srtmElevationFilesCache.get(ii);

      if (_srtm3ElevationFile == null) {

         // first time only
         _srtm3ElevationFile = new SRTM3ElevationFile(lat, lon);
         _srtmElevationFilesCache.put(ii, _srtm3ElevationFile);
      }

      return _srtm3ElevationFile.getElevation(lat, lon);

   }

   @Override
   public double getElevationDouble(final GeoLat lat, final GeoLon lon) {

      if (lat.decimal == 0 && lon.decimal == 0) {
         return 0.;
      }
      if (lat.tertias != 0) {
         return getElevationGridDouble(lat, lon);
      }
      if (lon.tertias != 0) {
         return getElevationGridDouble(lat, lon);
      }
      if (lat.seconds % 3 != 0) {
         return getElevationGridDouble(lat, lon);
      }
      if (lon.seconds % 3 != 0) {
         return getElevationGridDouble(lat, lon);
      }

      return getElevation(lat, lon);
   }

   @Override
   public String getName() {
      return ELEVATION_ID;
   }

   @Override
   public short getSecDiff() {
      // number of degrees seconds between two data points
      return 3;
   }
}
