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

   private static SlideoutMap2_MapPoints _mapPointSlideout;

   private static int                    _numPaintedMarkers;
   private static int                    _numAllTourMarkers;

   private static int                    _numPaintedLocations;
   private static int                    _numAllLocations;

   private static int                    _numPaintedPauses;
   private static int                    _numAllTourPauses;

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

   public static void updateStatistics() {

      if (_mapPointSlideout != null) {

         PlatformUI.getWorkbench().getDisplay().asyncExec(() -> _mapPointSlideout.updateStatistics(

               _numPaintedMarkers,
               _numAllTourMarkers,

               _numPaintedLocations,
               _numAllLocations,

               _numPaintedPauses,
               _numAllTourPauses

         ));
      }

   }

   public static void updateStatistics(final int numPaintedMarkers,
                                       final int numAllTourMarkers,

                                       final int numPaintedLocations,
                                       final int numAllLocations,

                                       final int numPaintedPauses,
                                       final int numAllTourPauses) {

      _numPaintedMarkers = numPaintedMarkers;
      _numAllTourMarkers = numAllTourMarkers;
      _numPaintedLocations = numPaintedLocations;
      _numAllLocations = numAllLocations;
      _numPaintedPauses = numPaintedPauses;
      _numAllTourPauses = numAllTourPauses;

      if (_mapPointSlideout != null) {

         PlatformUI.getWorkbench().getDisplay().asyncExec(() -> _mapPointSlideout.updateStatistics(

               numPaintedMarkers,
               numAllTourMarkers,

               numPaintedLocations,
               numAllLocations,

               numPaintedPauses,
               numAllTourPauses

         ));
      }

   }
}
