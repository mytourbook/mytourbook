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
package net.tourbook.device.polar.hrm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import net.tourbook.data.TourData;
import net.tourbook.device.gpx.GPXDeviceDataReader;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

/**
 * This device reader is importing data from Polar device files.
 */
public class Polar_PDD_DataReader extends TourbookDevice {

   private static final String SECTION_DAY_INFO = "[DayInfo]"; //$NON-NLS-1$

   private boolean             _isDebug         = false;

   // plugin constructor
   public Polar_PDD_DataReader() {}

   @Override
   public String buildFileNameFromRawData(final String rawDataFileName) {
      return null;
   }

   @Override
   public boolean checkStartSequence(final int byteIndex, final int newByte) {
      return false;
   }

   @Override
   public String getDeviceModeName(final int profileId) {
      return null;
   }

   protected TourbookDevice getGPXDeviceDataReader() {
      return new GPXDeviceDataReader();
   }

   protected TourbookDevice getPolarHRMDataReader() {
      return new Polar_HRM_DataReader();
   }

   @Override
   public SerialParameters getPortParameters(final String portName) {
      return null;
   }

   @Override
   public int getStartSequenceSize() {
      return -1;
   }

   @Override
   public int getTransferDataSize() {
      return -1;
   }

   @Override
   public void processDeviceData(final String importFilePath,
                                 final DeviceData deviceData,
                                 final Map<Long, TourData> alreadyImportedTours,
                                 final Map<Long, TourData> newlyImportedTours,
                                 final ImportState_File importState_File,
                                 final ImportState_Process importState_Process) {

      if (_isDebug) {
         System.out.println(importFilePath);
      }

      new Polar_PDD_Data(

            importFilePath,
            alreadyImportedTours,
            newlyImportedTours,

            importState_File,
            importState_Process,

            this

      ).parseSection();
   }

   /**
    * @return Return <code>true</code> when the file has a valid .hrm data format
    */
   @Override
   public boolean validateRawData(final String fileName) {

      try (FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader)) {

         final String firstLine = bufferedReader.readLine();
         if (firstLine == null || firstLine.startsWith(SECTION_DAY_INFO) == false) {
            return false;
         }

      } catch (final IOException e) {
         e.printStackTrace();
      }

      return true;
   }
}
