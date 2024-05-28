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

public class Map2PointManager {

   private static SlideoutMap2_MapPoints _mapPointSlideout;

   /**
    * @param isOpenSlideout
    *           When <code>true</code> then the slideout is opened when it is available
    *
    * @return
    */
   public static SlideoutMap2_MapPoints getMapPointSlideout(final boolean isOpenSlideout) {

      if (_mapPointSlideout != null) {

         if (isOpenSlideout) {

            _mapPointSlideout.open(false);
         }
      }

      return _mapPointSlideout;
   }

   public static void setMapLocationSlideout(final SlideoutMap2_MapPoints mapLocationSlideout) {

      _mapPointSlideout = mapLocationSlideout;
   }

   /**
    * Update UI in the location + marker slideout
    */
   public static void updateMapLocationAndMarkerSlideout() {

      if (_mapPointSlideout != null) {

         _mapPointSlideout.updateUI();
      }
   }
}
