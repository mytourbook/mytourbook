/* *****************************************************************************
 *  Copyright (C) 2008 Michael Kanis and others
 *  
 *  This file is part of Geoclipse.
 *
 *  Geoclipse is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Geoclipse is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Geoclipse.  If not, see <http://www.gnu.org/licenses/>. 
 *******************************************************************************/

package de.byteholder.geoclipse.util;

import de.byteholder.gpx.GeoPosition;

/**
 * @author Michael Kanis
 */
public class GeoUtils {

	/**
	 * Earth radius is 6367 km.
	 */
	static final double			EARTH_RADIUS	= 6367d;

	// --- Mathematic constants ---
	private static final double	DEGRAD			= Math.PI / 180.0d;

	/**
	 * Calculates the distance between two points on earth, pos1 and pos2 in
	 * kilometres.
	 * 
	 * @param pos1
	 * @param pos2
	 * @return The distance between pos1 and pos2 in kilometers.
	 */
	public static double distance(final GeoPosition pos1, final GeoPosition pos2) {

		double lat1 = pos1.latitude;
		double lon1 = pos1.longitude;

		double lat2 = pos2.latitude;
		double lon2 = pos2.longitude;

		double a, c;

		// convert the degree values to radians before calculation
		lat1 = lat1 * DEGRAD;
		lon1 = lon1 * DEGRAD;
		lat2 = lat2 * DEGRAD;
		lon2 = lon2 * DEGRAD;

		final double dlon = lon2 - lon1;
		final double dlat = lat2 - lat1;

		a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);

		c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return (EARTH_RADIUS * c);
	}
}
