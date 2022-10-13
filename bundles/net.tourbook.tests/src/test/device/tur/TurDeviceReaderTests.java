/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package device.tur;

import net.tourbook.device.tur.TurDeviceReader;

import org.junit.jupiter.api.Test;

import utils.DeviceDataReaderTester;
import utils.FilesUtils;

public class TurDeviceReaderTests extends DeviceDataReaderTester {

   public static final String FILES_PATH       = FilesUtils.rootPath + "device/tur/files/"; //$NON-NLS-1$

   private TurDeviceReader    deviceDataReader = new TurDeviceReader();

   /**
    * Sample provided by ovahead
    * https://github.com/mytourbook/mytourbook/discussions/939#discussioncomment-3723897
    */
   @Test
   void testTurImport_20200915() {

      testImportFile(deviceDataReader, FILES_PATH + "15.09.2020", ".tur"); //$NON-NLS-1$ //$NON-NLS-2$
   }
}
