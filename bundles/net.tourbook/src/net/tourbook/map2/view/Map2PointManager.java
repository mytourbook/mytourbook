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

   private static int                    _numCommonLocations_Painted;
   private static int                    _numCommonLocations_All;

   private static int                    _numTourLocations_Painted;
   private static int                    _numTourLocations_All;

   private static int                    _numTourMarkers_Painted;
   private static int                    _numTourMarkers_All;

   private static int                    _numTourPauses_Painted;
   private static int                    _numTourPauses_All;

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

               _numCommonLocations_Painted,
               _numCommonLocations_All,

               _numTourLocations_Painted,
               _numTourLocations_All,

               _numTourMarkers_Painted,
               _numTourMarkers_All,

               _numTourPauses_Painted,
               _numTourPauses_All

         ));
      }

   }

   public static void updateStatistics(final int numCommonLocations_Painted,
                                       final int numCommonLocations_All,

                                       final int numTourLocations_Painted,
                                       final int numTourLocations_All,

                                       final int numTourMarkers_Painted,
                                       final int numTourMarkers_All,

                                       final int numTourPauses_Painted,
                                       final int numTourPauses_All) {

// SET_FORMATTING_OFF

      _numCommonLocations_Painted   = numCommonLocations_Painted;
      _numCommonLocations_All       = numCommonLocations_All;
      _numTourLocations_Painted     = numTourLocations_Painted;
      _numTourLocations_All         = numTourLocations_All;
      _numTourMarkers_Painted       = numTourMarkers_Painted;
      _numTourMarkers_All           = numTourMarkers_All;
      _numTourPauses_Painted        = numTourPauses_Painted;
      _numTourPauses_All            = numTourPauses_All;

// SET_FORMATTING_ON

      if (_mapPointSlideout != null) {

         PlatformUI.getWorkbench().getDisplay().asyncExec(() -> _mapPointSlideout.updateStatistics(

               numCommonLocations_Painted,
               numCommonLocations_All,

               numTourLocations_Painted,
               numTourLocations_All,

               numTourMarkers_Painted,
               numTourMarkers_All,

               numTourPauses_Painted,
               numTourPauses_All

         ));
      }

   }
}
