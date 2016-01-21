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

package de.byteholder.gpx;


/**
 * Two lat/lon pairs defining the extent of an element.
 * 
 * @author Michael Kanis
 */
public class Bounds {

	/**
	 * The minimum latitude.
	 */
	private Latitude minLat;
	
	/**
	 * The maximum latitude.
	 */
	private Latitude maxLat;
	
	/**
	 * The minimum longitude.
	 */
	private Longitude minLon;
	
	/**
	 * The maximum longitude.
	 */
	private Longitude maxLon;

	public Latitude getMaxLat() {
		return maxLat;
	}

	public void setMaxLat(Latitude maxLat) {
		this.maxLat = maxLat;
	}

	public Longitude getMaxLon() {
		return maxLon;
	}

	public void setMaxLon(Longitude maxLon) {
		this.maxLon = maxLon;
	}

	public Latitude getMinLat() {
		return minLat;
	}

	public void setMinLat(Latitude minLat) {
		this.minLat = minLat;
	}

	public Longitude getMinLon() {
		return minLon;
	}

	public void setMinLon(Longitude minLon) {
		this.minLon = minLon;
	}
}
