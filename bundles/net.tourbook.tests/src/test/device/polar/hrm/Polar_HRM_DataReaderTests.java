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
package device.polar.hrm;

import net.tourbook.device.polar.hrm.Polar_HRM_DataReader;

import org.junit.jupiter.api.Test;

import utils.DeviceDataReaderTester;
import utils.FilesUtils;

public class Polar_HRM_DataReaderTests extends DeviceDataReaderTester {

   public static final String   FILES_PATH       = FilesUtils.rootPath + "device/polar/hrm/files/"; //$NON-NLS-1$

   private Polar_HRM_DataReader deviceDataReader = new Polar_HRM_DataReader();

   /**
    * Sample provided by
    * https://sourceforge.net/projects/hrmedit/files/hrmedit/Sample%20HRM%20files/
    */
   @Test
   void testHrmImport_080510() {

      testImportFile(deviceDataReader, FILES_PATH + "SampleHRM_WithErrorsAtTheEnd", ".hrm"); //$NON-NLS-1$ //$NON-NLS-2$
   }
}
