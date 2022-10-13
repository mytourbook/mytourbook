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

import net.tourbook.device.suunto.Suunto2_DeviceDataReader;

import org.junit.jupiter.api.Test;

import utils.DeviceDataReaderTester;
import utils.FilesUtils;

class Suunto2_DeviceDataReaderTests extends DeviceDataReaderTester {

   private static final String      FILES_PATH       = FilesUtils.rootPath + "device/suunto/files/"; //$NON-NLS-1$

   private Suunto2_DeviceDataReader deviceDataReader = new Suunto2_DeviceDataReader();

   /**
    * Forest park, Portland, OR
    */
   @Test
   void testImportForestPark() {

      testImportFile(deviceDataReader, FILES_PATH + "log-F783095113000500-2013-05-18T11_00_38-0", ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
   }
}
