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
package device.suunto;

import net.tourbook.device.suunto.Suunto3_DeviceDataReader;

import org.junit.jupiter.api.Test;

import utils.DeviceDataReaderTester;
import utils.FilesUtils;

class Suunto3_DeviceDataReaderTests extends DeviceDataReaderTester {

   private static final String      FILES_PATH       = FilesUtils.rootPath + "device/suunto/files/"; //$NON-NLS-1$

   private Suunto3_DeviceDataReader deviceDataReader = new Suunto3_DeviceDataReader();

   /**
    * Forest Park, OR with laps
    */
   @Test
   void testImportForestParkLaps() {

      testImportFile(deviceDataReader, FILES_PATH + "597F0A5112001700-2016-08-27T15_45_41-0", ".sml"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   /**
    * Timothy Lake, OR
    */
   @Test
   void testImportTimothyLake() {

      testImportFile(deviceDataReader, FILES_PATH + "F783095113000500-2015-05-31T09_51_13-0", ".sml"); //$NON-NLS-1$ //$NON-NLS-2$
   }
}
