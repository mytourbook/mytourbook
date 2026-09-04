/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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

public class TourDataUpdate_062_to_063 implements ITourDataUpdate {

   @Override
   public int getDatabaseVersion() {

      return 62;
   }

   @Override
   public List<Long> getTourIDs() {

      return null;
   }

   @Override
   public boolean updateTourData(final TourData tourData) {

      if (tourData.getTourNutritionProducts().isEmpty()) {
         return false;
      }

      tourData.computeTourNutritionData();

      return true;
   }
}
