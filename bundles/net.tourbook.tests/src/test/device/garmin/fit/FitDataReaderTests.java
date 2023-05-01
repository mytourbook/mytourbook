/*******************************************************************************
 * Copyright (C) 2020, 2023 Frédéric Bard
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
package device.garmin.fit;

import net.tourbook.device.garmin.fit.FitDataReader;

import org.junit.jupiter.api.Test;

import utils.DeviceDataReaderTester;
import utils.FilesUtils;

public class FitDataReaderTests extends DeviceDataReaderTester {

   private static final String FILES_PATH    = FilesUtils.rootPath + "device/garmin/fit/files/"; //$NON-NLS-1$

   private FitDataReader       fitDataReader = new FitDataReader();

   /**
    * Test with a file containing swimming data
    * Sample provided by Doriano
    * https://github.com/mytourbook/mytourbook/discussions/939#discussioncomment-5728661
    */
   @Test
   void testFitImport_SwimmingData() {

      testImportFile(fitDataReader, FILES_PATH + "8ADG5025", ".FIT"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   /**
    * Regression test. This test can be useful when updating the FIT SDK and
    * ensuring that the FIT import still works as expected.
    */
   @Test
   void testFitImportConeyLake() {

      testImportFile(fitDataReader, FILES_PATH + "ConeyLakeMove_2020_05_23_08_55_42_Trail+running", ".fit"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   /**
    * Test to ensure that files without pauses have the recorded time equal to
    * the elapsed time
    */
   @Test
   void testFitImportNoPauses() {

      testImportFile(fitDataReader, FILES_PATH + "1-30-21 3-47 PM", ".fit"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   /**
    * Test with a file containing pauses triggered automatically by the device
    */
   @Test
   void testFitImportPauses_Auto() {

      testImportFile(fitDataReader, FILES_PATH + "Hardrock_100_Start_Finish", ".fit"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   /**
    * Test with a file containing pauses triggered by the user
    */
   @Test
   void testFitImportPauses_User() {

      testImportFile(fitDataReader, FILES_PATH + "Bye_bye_Silverton", ".fit"); //$NON-NLS-1$ //$NON-NLS-2$
   }
}
