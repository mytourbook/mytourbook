/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.database;

import java.util.List;

import net.tourbook.data.TourData;

/**
 *
 */
public class TourDataUpdate_058_to_059 implements ITourDataUpdate {

   @Override
   public int getDatabaseVersion() {

      return 59;
   }

   @Override
   public List<Long> getTourIDs() {

      return null;
   }

   /**
    * The paused time in tours can be factor 1000 larger than the break time. There must be
    * sometimes a bug when the pause time was computed, I could find the reason, when it happened
    */
   @Override
   public boolean updateTourData(final TourData tourData) {

      final long pausedTime = tourData.getTourDeviceTime_Paused();
      final long breakTime = tourData.getTourDeviceTime_Elapsed() - tourData.getTourComputedTime_Moving();

      if (pausedTime > 0 && pausedTime == breakTime * 1000) {

         tourData.setTourDeviceTime_Paused(breakTime);

         return true;
      }

      return false;
   }

}
