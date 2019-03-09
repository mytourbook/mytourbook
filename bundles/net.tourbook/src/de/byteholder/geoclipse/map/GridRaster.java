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

public class GridRaster {

   int        devGrid_X1;
   int        devGrid_Y1;
   int        devGrid_X2;
   int        devGrid_Y2;

   int        devWidth;
   int        devHeight;

   int        geoGrid_Lon1_E2;
   int        geoGrid_Lat1_E2;
   int        geoGrid_Lon2_E2;
   int        geoGrid_Lat2_E2;

   public int numWidth;
   public int numHeight;
}
