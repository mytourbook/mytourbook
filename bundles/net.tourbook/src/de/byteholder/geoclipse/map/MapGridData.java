/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.map;

import net.tourbook.common.map.GeoPosition;
import net.tourbook.tour.filter.geo.TourGeoFilter;

import org.eclipse.swt.graphics.Point;

/**
 * Contains map device data for a {@link TourGeoFilter}
 */
public class MapGridData {

   boolean       isSelectionStarted;

   GeoPosition   geo_MouseMove;

   Point         world_Start;
   Point         world_End;

   /**
    * Geo start position is set when the selecting of a geo grid has started.
    */
   GeoPosition   geo_Start;
   GeoPosition   geo_End;

   public String gridBox_Text;

   /*
    * Paint data
    */

   int        devGrid_X1;
   int        devGrid_Y1;

   int        devWidth;
   int        devHeight;

   public int numWidth = -1;
   public int numHeight;

   /*
    * Geo location in the map
    */
   public Point geoLocation_TopLeft_E2;
   public Point geoLocation_BottomRight_E2;

   /*
    * Location for the geo parts
    */
   public Point geoParts_TopLeft_E2;
   public Point geoParts_BottomRight_E2;

   public MapGridData() {}

   @Override
   public String toString() {
      return "" //$NON-NLS-1$

            + "MapGridData\n" //$NON-NLS-1$

            + "[" + "\n" //$NON-NLS-1$ //$NON-NLS-2$

            + "devGrid_X1           =" + devGrid_X1 + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "devGrid_Y1           =" + devGrid_Y1 + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "devWidth             =" + devWidth + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "devHeight            =" + devHeight + "\n" //$NON-NLS-1$ //$NON-NLS-2$

            + "world_Start          =" + world_Start + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "world_End            =" + world_End + "\n" //$NON-NLS-1$ //$NON-NLS-2$

            + "geo_MouseMove        =" + geo_MouseMove + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "geo_Start            =" + geo_Start + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "geo_End              =" + geo_End + "\n" //$NON-NLS-1$ //$NON-NLS-2$

            + "geoLocation_TopLeft_E2        =" + geoLocation_TopLeft_E2 + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "geoLocation_BottomRight_E2    =" + geoLocation_BottomRight_E2 + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "geoParts_TopLeft_E2           =" + geoParts_TopLeft_E2 + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "geoParts_BottomRight_E2       =" + geoParts_BottomRight_E2 + "\n" //$NON-NLS-1$ //$NON-NLS-2$

            + "isSelectionStarted   =" + isSelectionStarted + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "numWidth             =" + numWidth + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "numHeight            =" + numHeight + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "gridBox_Text         =" + gridBox_Text + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "]"; //$NON-NLS-1$
   }

}
