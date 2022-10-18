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
package device.polartrainer;

import net.tourbook.device.polartrainer.PolarTrainerDataReader;

import org.junit.jupiter.api.Test;

import utils.DeviceDataReaderTester;
import utils.FilesUtils;

public class PolarTrainerDataReaderTests extends DeviceDataReaderTester {

   public static final String     FILES_PATH       = FilesUtils.rootPath + "device/polartrainer/files/"; //$NON-NLS-1$

   private PolarTrainerDataReader deviceDataReader = new PolarTrainerDataReader();

   @Test
   void testPolarTrainerImport_2020() {

      testImportFile(deviceDataReader, FILES_PATH + "user_25.11.2020_export", ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
   }
}
