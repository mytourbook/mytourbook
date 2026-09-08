/*******************************************************************************
 * Copyright (C) 2026 Frédéric Bard and Contributors
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

import java.io.File;
import java.util.List;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.util.FileUtils;
import net.tourbook.data.TourData;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;

public class TourDataUpdate_062_to_063 implements ITourDataUpdate {

   @Override
   public int getDatabaseVersion() {

      return 62;
   }

   @Override
   public List<Long> getTourIDs() {

      final IPath stateLocation = Platform.getStateLocation(CommonActivator.getDefault().getBundle());
      final File invalidFiles = stateLocation.append("invalidfiles_to_ignore.txt").toFile(); //$NON-NLS-1$
      FileUtils.deleteIfExists(invalidFiles.toPath());

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
