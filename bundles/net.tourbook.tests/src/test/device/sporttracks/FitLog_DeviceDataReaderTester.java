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
package device.sporttracks;

import java.util.HashMap;

import net.tourbook.data.TourData;
import net.tourbook.device.sporttracks.FitLogDeviceDataReader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import utils.Initializer;

public abstract class FitLog_DeviceDataReaderTester {

   protected static HashMap<Long, TourData> newlyImportedTours;
   protected static HashMap<Long, TourData> alreadyImportedTours;
   protected static FitLogDeviceDataReader deviceDataReader;

   @BeforeAll
   static void initAll() {

      Initializer.initializeDatabase();
      newlyImportedTours = new HashMap<>();
      alreadyImportedTours = new HashMap<>();
      deviceDataReader = new FitLogDeviceDataReader();
   }

   @AfterEach
   void tearDown() {

      newlyImportedTours.clear();
      alreadyImportedTours.clear();
   }
}
