/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.layer.tourtrack;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;

public class TourMap3Position extends Position {

	/**
	 * Contains the value from the data serie which is painted in the map, this can be altitude,
	 * pulse, ...
	 */
	float	dataSerieValue;

	public TourMap3Position(final LatLon fromDegrees, final float alti, final float dataSerieValue) {

		super(fromDegrees, alti);

		this.dataSerieValue = dataSerieValue;
	}

}
