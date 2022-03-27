/*******************************************************************************
 * Copyright (C) 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.device.mt;

import java.util.Map;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

public class MTDeviceDataReader extends TourbookDevice {

   private static final String MT_XML_TAG = "<mt"; //$NON-NLS-1$

   /**
    * Plugin constructor
    */
   public MTDeviceDataReader() {}

   @Override
   public String buildFileNameFromRawData(final String rawDataFileName) {
      return null;
   }

   @Override
   public boolean checkStartSequence(final int byteIndex, final int newByte) {
      return true;
   }

   @Override
   public String getDeviceModeName(final int profileId) {
      return UI.EMPTY_STRING;
   }

   @Override
   public SerialParameters getPortParameters(final String portName) {
      return null;
   }

   @Override
   public int getStartSequenceSize() {
      return 0;
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

      if (isValidXMLFile(importFilePath, MT_XML_TAG) == false) {
         return;
      }

      MT_StAXHandler staxHandler = null;

      try {

         staxHandler = new MT_StAXHandler(
               this,
               importFilePath,
               alreadyImportedTours,
               newlyImportedTours,
               importState_File,
               importState_Process);

      } catch (final Exception e) {

         StatusUtil.log("Error parsing file: " + importFilePath, e); //$NON-NLS-1$

      } finally {

         // cleanup stax handler
         if (staxHandler != null) {
            staxHandler.dispose();
         }
      }
   }

   @Override
   public boolean validateRawData(final String fileName) {

      return isValidXMLFile(fileName, MT_XML_TAG);
   }
}
