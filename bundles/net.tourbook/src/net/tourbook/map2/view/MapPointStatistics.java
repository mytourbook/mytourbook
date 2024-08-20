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

public class MapPointStatistics {

   public int     numCommonLocations_Painted;
   public int     numCommonLocations_All;

   public int     numTourLocations_Painted;
   public int     numTourLocations_All;

   public int     numTourMarkers_Painted;
   public int     numTourMarkers_All;
   public boolean numTourMarkers_All_IsTruncated;

   public int     numTourPauses_Painted;
   public int     numTourPauses_All;
   public boolean numTourPauses_All_IsTruncated;

   public int     numTourPhotos_Painted;
   public int     numTourPhotos_All;
   public boolean numTourPhotos_All_IsTruncated;

   public int     numTourWayPoints_Painted;
   public int     numTourWayPoints_All;
   public boolean numTourWayPoints_All_IsTruncated;

   public MapPointStatistics(final int numCommonLocations_Painted,
                             final int numCommonLocations_All,

                             final int numTourLocations_Painted,
                             final int numTourLocations_All,

                             final int numTourMarkers_Painted,
                             final int numTourMarkers_All,
                             final boolean numTourMarkers_All_IsTruncated,

                             final int numTourPauses_Painted,
                             final int numTourPauses_All,
                             final boolean numTourPauses_All_IsTruncated,

                             final int numTourPhotos_Painted,
                             final int numTourPhotos_All,
                             final boolean numTourPhotos_All_IsTruncated,

                             final int numTourWayPoints_Painted,
                             final int numTourWayPoints_All,
                             final boolean numTourWayPoints_All_IsTruncated) {

   // SET_FORMATTING_OFF

         this.numCommonLocations_Painted        = numCommonLocations_Painted;
         this.numCommonLocations_All            = numCommonLocations_All;

         this.numTourLocations_Painted          = numTourLocations_Painted;
         this.numTourLocations_All              = numTourLocations_All;

         this.numTourMarkers_Painted            = numTourMarkers_Painted;
         this.numTourMarkers_All                = numTourMarkers_All;
         this.numTourMarkers_All_IsTruncated    = numTourMarkers_All_IsTruncated;

         this.numTourPauses_Painted             = numTourPauses_Painted;
         this.numTourPauses_All                 = numTourPauses_All;
         this.numTourPauses_All_IsTruncated     = numTourPauses_All_IsTruncated;

         this.numTourPhotos_Painted             = numTourPhotos_Painted;
         this.numTourPhotos_All                 = numTourPhotos_All;
         this.numTourPhotos_All_IsTruncated     = numTourPhotos_All_IsTruncated;

         this.numTourWayPoints_Painted          = numTourWayPoints_Painted;
         this.numTourWayPoints_All              = numTourWayPoints_All;
         this.numTourWayPoints_All_IsTruncated  = numTourWayPoints_All_IsTruncated;

   // SET_FORMATTING_ON

   }

}
