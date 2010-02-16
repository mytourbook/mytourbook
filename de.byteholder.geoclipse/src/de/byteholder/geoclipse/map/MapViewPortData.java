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
package de.byteholder.geoclipse.map;
 
public class MapViewPortData {

	public int	mapZoomLevel;

	public int	tilePosMinX;
	public int	tilePosMaxX;

	public int	tilePosMinY;
	public int	tilePosMaxY;

	public MapViewPortData(	final int mapZoomLevel,
						final int tilePosMinX,
						final int tilePosMaxX,
						final int tilePosMinY,
						final int tilePosMaxY) {

		this.mapZoomLevel = mapZoomLevel;

		this.tilePosMinX = tilePosMinX;
		this.tilePosMaxX = tilePosMaxX;

		this.tilePosMinY = tilePosMinY;
		this.tilePosMaxY = tilePosMaxY;
	}

}
