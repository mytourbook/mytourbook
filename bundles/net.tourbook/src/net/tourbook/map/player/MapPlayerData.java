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
public class MapPlayerData {

   private static final String NL = UI.NEW_LINE;

   public boolean              isPlayerEnabled;

   public short[]              allVisible_PixelPositions;

   /**
    * Indices for {@link #allVisible_PixelPositions} into the tour track data
    */
   public int[]                allVisible_GeoLocationIndices;

   /**
    * Projected points 0...1 for all geo positions for all selected tours
    * <p>
    * Is projected from -180°...180° ==> 0...1 by using the {@link MercatorProjection}
    */
   public double[]             allProjectedPoints;

   /**
    * Contains indices into all geo positions for all selected tours which can be also outside of
    * the clipper (visible) area -2048...2048
    */
   public int[]                allNotClipped_GeoLocationIndices;

   /**
    * Map scale when binding buffer data
    */
   public double               mapScale;

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "MapPlayerData" + NL //                                                           //$NON-NLS-1$

            + "[" + NL //                                                                       //$NON-NLS-1$

            + "isPlayerEnabled                  = " + isPlayerEnabled + NL //                   //$NON-NLS-1$
            + "allProjectedPoints               = " + allProjectedPoints + NL //                //$NON-NLS-1$
            + "allVisible_PixelPositions        = " + allVisible_PixelPositions + NL //         //$NON-NLS-1$
            + "allVisible_GeoLocationIndices    = " + allVisible_GeoLocationIndices + NL //     //$NON-NLS-1$
            + "allNotClipped_GeoLocationIndices = " + allNotClipped_GeoLocationIndices + NL //  //$NON-NLS-1$
            + "mapScale                         = " + mapScale + NL //                          //$NON-NLS-1$

            + "]"; //$NON-NLS-1$
   }

}
