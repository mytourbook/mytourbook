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
package device.crp;

import net.tourbook.device.crp.CRPDataReader;

import org.junit.jupiter.api.Test;

import utils.DeviceDataReaderTester;
import utils.FilesUtils;

/**
 * Samples provided by WRPSoft
 * https://github.com/mytourbook/mytourbook/discussions/924#discussioncomment-3585694
 */
public class CRPDataReaderTests extends DeviceDataReaderTester {

   public static final String FILES_PATH       = FilesUtils.rootPath + "device/crp/files/"; //$NON-NLS-1$

   private CRPDataReader      deviceDataReader = new CRPDataReader();

   /**
    * A .crp file with cadence values
    */
   @Test
   void testCrpImport_080510() {

      testImportFile(deviceDataReader, FILES_PATH + "080510", ".crp"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   /**
    * Testing a raw .crp file of version 9
    */
   @Test
   void testCrpImport_20040703() {

      testImportFile(deviceDataReader, FILES_PATH + "2004-07-03", ".crp"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   /**
    * Testing a raw .crp file
    */
   @Test
   void testCrpImport_20100923_Auf_die() {

      testImportFile(deviceDataReader, FILES_PATH + "20100923_Auf_die", ".crp"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   @Test
   void testCrpImport_Feldberg_260706() {

      testImportFile(deviceDataReader, FILES_PATH + "Feldberg_260706", ".crp"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   @Test
   void testCrpImport_Hac4() {

      testImportFile(deviceDataReader, FILES_PATH + "Hac4", ".crp"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   @Test
   void testCrpImport_Hac5() {

      testImportFile(deviceDataReader, FILES_PATH + "1st_TestTour_Hac5", ".crp"); //$NON-NLS-1$ //$NON-NLS-2$
   }
}
