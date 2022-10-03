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
package device.hac4;

import net.tourbook.device.hac4.HAC4DeviceReader;

import org.junit.jupiter.api.Test;

import utils.DeviceDataReaderTester;
import utils.FilesUtils;

public class HAC4DeviceReaderTests extends DeviceDataReaderTester {

   public static final String FILES_PATH       = FilesUtils.rootPath + "device/hac4/files/"; //$NON-NLS-1$

   private HAC4DeviceReader   deviceDataReader = new HAC4DeviceReader();

   /**
    * Sample provided by ovahead
    * https://github.com/mytourbook/mytourbook/discussions/939#discussioncomment-3723897
    */
   @Test
   void testHAC4Import_Connect7() {

      testImportFile(deviceDataReader, FILES_PATH + "Connect7", ".dat"); //$NON-NLS-1$ //$NON-NLS-2$
   }
}
