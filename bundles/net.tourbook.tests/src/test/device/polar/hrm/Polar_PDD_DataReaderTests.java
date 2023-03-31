/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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
package device.polar.hrm;

import net.tourbook.device.polar.hrm.Polar_PDD_DataReader;

import org.junit.jupiter.api.Test;

import utils.DeviceDataReaderTester;
import utils.FilesUtils;

public class Polar_PDD_DataReaderTests extends DeviceDataReaderTester {

   public static final String   FILES_PATH       = FilesUtils.rootPath + "device/polar/hrm/files/"; //$NON-NLS-1$

   private Polar_PDD_DataReader deviceDataReader = new Polar_PDD_DataReader();

   /**
    * Sample provided by Thompson-ongithub
    * https://github.com/mytourbook/mytourbook/issues/1044#issuecomment-1488725400
    */
   @Test
   void testPddImport_20230328() {

      testImportFile(deviceDataReader, FILES_PATH + "20230328", ".pdd"); //$NON-NLS-1$ //$NON-NLS-2$
   }
}
