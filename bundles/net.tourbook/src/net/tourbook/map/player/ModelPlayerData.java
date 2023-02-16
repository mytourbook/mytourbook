/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.map.player;

import de.byteholder.geoclipse.map.UI;

import org.oscim.core.MercatorProjection;

/**
 * Data for the map animation player
 */
public class ModelPlayerData {

   private static final String NL = UI.NEW_LINE;

   /**
    * Indices for all visible positions into the tour track data
    */
   public int[]                allVisible_GeoLocationIndices;

   /**
    * Projected points 0...1 for all geo positions for all selected tours
    * <p>
    * Is projected from -180°...180° ==> 0...1 by using the {@link MercatorProjection}
    */
   public double[]             allProjectedPoints_NormalTrack;

   /**
    * Projected points 0...1 from the geo end point of the tour to the geo start point
    */
   public double[]             allProjectedPoints_ReturnTrack;

   /**
    * Contains indices into all geo positions for all selected tours. They are optimized for a
    * minimum distance, so they can be also outside of the clipper (visible) area -2048...2048
    */
   public int[]                allNotClipped_GeoLocationIndices;

   /**
    * Distance in pixel between the end and start point of the track for the current map scale
    */
   public double               trackEnd2StartPixelDistance;

   /**
    * Map scale when binding buffer data
    */
   public double               mapScale;

   public int[]                allTimeSeries;
   public float[]              allDistanceSeries;

   @Override
   public String toString() {

// SET_FORMATTING_OFF

      final int allProjectedPoints_NormalTrack_Length    = allProjectedPoints_NormalTrack    == null ? 0 : allProjectedPoints_NormalTrack.length;
      final int allProjectedPoints_ReturnTrack_Length    = allProjectedPoints_ReturnTrack    == null ? 0 : allProjectedPoints_ReturnTrack.length;
      final int allNotClipped_GeoLocationIndices_Length  = allNotClipped_GeoLocationIndices  == null ? 0 : allNotClipped_GeoLocationIndices.length;
      final int allVisible_GeoLocationIndices_Length     = allVisible_GeoLocationIndices     == null ? 0 : allVisible_GeoLocationIndices.length;

// SET_FORMATTING_ON

      return UI.EMPTY_STRING

            + "ModelPlayerData" + NL //                                                                 //$NON-NLS-1$

            + "[" + NL //                                                                             //$NON-NLS-1$

            + "allProjectedPoints_NormalTrack   = " + allProjectedPoints_NormalTrack_Length + NL //   //$NON-NLS-1$
            + "allProjectedPoints_ReturnTrack   = " + allProjectedPoints_ReturnTrack_Length + NL //   //$NON-NLS-1$
            + "allNotClipped_GeoLocationIndices = " + allNotClipped_GeoLocationIndices_Length + NL // //$NON-NLS-1$
            + "allVisible_GeoLocationIndices    = " + allVisible_GeoLocationIndices_Length + NL //    //$NON-NLS-1$
            + "mapScale                         = " + mapScale + NL //                                //$NON-NLS-1$

            + "]"; //$NON-NLS-1$
   }

}
