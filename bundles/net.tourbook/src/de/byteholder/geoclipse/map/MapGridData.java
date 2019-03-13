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

   Point         dev_Start;
   Point         dev_End;

   Point         world_Start;
   Point         world_End;

   /**
    * This geo position is set when the selecting of a geo grid has started.
    */
   GeoPosition   geo_Start;
   GeoPosition   geo_End;

   public String gridBox_Text;

   /*
    * Paint data
    */

   int          devGrid_X1;
   int          devGrid_Y1;

   int          devWidth;
   int          devHeight;

   public int   numWidth = -1;
   public int   numHeight;

   public Point geo_TopLeft_E2;
   public Point geo_BottomRight_E2;

   public MapGridData() {}

   @Override
   public String toString() {

// SET_FORMATTING_OFF

      return ""

            + "MapGridData\n"

            + "[\n"

            + " isSelectionStarted="    + isSelectionStarted + "\n"

            + " dev_Start="             + dev_Start + "\n"
            + " dev_End="               + dev_End + "\n"

            + " world_Start="           + world_Start + "\n"
            + " world_End="             + world_End + "\n"

            + " geo_Start="             + geo_Start + "\n"
            + " geo_End="               + geo_End + "\n"

            + " gridBox_Text="          + gridBox_Text + "\n"

            + "]";

// SET_FORMATTING_ON
   }

}
