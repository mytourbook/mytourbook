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
package de.byteholder.geoclipse.map.event;

import de.byteholder.geoclipse.map.MapGrid_StartEnd_Data;

import net.tourbook.common.map.GeoPosition;

import org.eclipse.swt.graphics.Point;

public interface IMapGridListener {

   /**
    * Is called when a map grid is selected.
    *
    * @param Lat/lon
    *           top/left, multiplied with 100
    * @param Lat/lon
    *           bottom/right, multiplied with 100
    * @param mapZoomLevel
    * @param mapGeoCenter
    * @param isGridSelected
    *           Is <code>true</code> when grid is selected, otherwise mouse is just moved
    * @param gridBoxItem
    *           Map grid box item
    */
   public void onMapGrid(Point topLeftE2,
                         Point bottomRightE2,
                         int mapZoomLevel,
                         GeoPosition mapGeoCenter,
                         boolean isGridSelected,
                         MapGrid_StartEnd_Data gridBoxItem);

}
