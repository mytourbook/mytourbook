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
package device.cm4xxm;

import de.byteholder.geoclipse.map.UI;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.tourbook.device.cm4xxm.CM4XXMDeviceReader;

import org.junit.jupiter.api.Test;

import utils.DeviceDataReaderTester;
import utils.FilesUtils;

public class CM4XXMDeviceReaderTests extends DeviceDataReaderTester {

   public static final String FILES_PATH       = FilesUtils.rootPath + "device/cm4xxm/files/"; //$NON-NLS-1$

   private CM4XXMDeviceReader deviceDataReader = new CM4XXMDeviceReader();

   /**
    * Sample provided by cyc1ingsir
    * https://github.com/mytourbook/mytourbook/discussions/939#discussioncomment-3714641
    */
   @Test
   void testCM4XXMImport_2006() throws IOException {

      //Converting back the file from dos to unix as the Github Build action converts
      //it from the original in unix to dos and makes the unit test fail.

      final String importFileAbsolutePath = FilesUtils.getAbsoluteFilePath(FILES_PATH + "20060327-20060608_Touren.dat"); //$NON-NLS-1$

      String dosText = net.tourbook.common.util.FileUtils.readFileContentString(importFileAbsolutePath);
      dosText = dosText.replace("\r\n", UI.NEW_LINE);//$NON-NLS-1$
      Files.writeString(Paths.get(importFileAbsolutePath), dosText, Charset.defaultCharset());

      testImportFile(deviceDataReader, FILES_PATH + "20060327-20060608_Touren", ".dat"); //$NON-NLS-1$ //$NON-NLS-2$
   }
}
