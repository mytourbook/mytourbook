/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import net.tourbook.common.map.GeoPosition;

public class MapPositionEvent {

	public GeoPosition	mapGeoPosition;
	public int			mapZoomLevel;

	public MapPositionEvent(final GeoPosition mousePosition, final int zoomLevel) {
		mapGeoPosition = mousePosition;
		mapZoomLevel = zoomLevel;
	}

}
