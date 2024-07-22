/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import org.eclipse.ui.PlatformUI;

public class Map2PointManager {

   private static SlideoutMap2_MapPoints _mapPoint_Slideout;
   private static MapPointStatistics     _mapPoint_Statistics;

   /**
    * @param isOpenSlideout
    *           When <code>true</code> then the slideout is opened when it is available
    *
    * @return
    */
   public static SlideoutMap2_MapPoints getMapPointSlideout(final boolean isOpenSlideout) {

      if (_mapPoint_Slideout != null) {

         if (isOpenSlideout) {

            _mapPoint_Slideout.open(false);
         }
      }

      return _mapPoint_Slideout;
   }

   public static void setMapLocationSlideout(final SlideoutMap2_MapPoints mapLocationSlideout) {

      _mapPoint_Slideout = mapLocationSlideout;
   }

   /**
    * Update UI in the map point slideout
    */
   public static void updateMapPointSlideout() {

      if (_mapPoint_Slideout != null) {

         _mapPoint_Slideout.updateUI();
      }
   }

   public static void updateStatistics() {

      if (_mapPoint_Slideout != null && _mapPoint_Statistics != null) {

         PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {

            if (_mapPoint_Slideout != null) {

               _mapPoint_Slideout.updateStatistics(_mapPoint_Statistics);
            }
         });
      }

   }

   public static void updateStatistics(final MapPointStatistics mapPointStatistics) {

      _mapPoint_Statistics = mapPointStatistics;

      updateStatistics();
   }
}
