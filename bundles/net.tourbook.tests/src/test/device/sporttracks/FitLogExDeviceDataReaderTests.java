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
package device.sporttracks;

import net.tourbook.device.sporttracks.FitLogDeviceDataReader;

import org.junit.jupiter.api.Test;

import utils.DeviceDataReaderTester;
import utils.FilesUtils;

class FitLogExDeviceDataReaderTests extends DeviceDataReaderTester {

   private static final String      FILES_PATH       = FilesUtils.rootPath + "device/sporttracks/fitlogex/files/"; //$NON-NLS-1$

   protected FitLogDeviceDataReader deviceDataReader = new FitLogDeviceDataReader();

   /**
    * This test parses a file for which the time offset of -7 hours is wrong
    * <TimeZoneUtcOffset>-25200</TimeZoneUtcOffset> as it is located in the MST
    * zone (-6h or -21600). However, the start time is correct and needs to be
    * kept.
    */
   @Test
   void testImportParkCity() {

      testImportFile(deviceDataReader, FILES_PATH + "ParkCity", ".fitlogEx"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   @Test
   void testImportTimothyLake() {

      testImportFile(deviceDataReader, FILES_PATH + "TimothyLake", ".fitlogEx"); //$NON-NLS-1$ //$NON-NLS-2$
   }
}
