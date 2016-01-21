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
 * Used for bearing, heading, course. Units are decimal degrees, true (not magnetic).
 * 
 * @author Michael Kanis
 */
public class Degrees {

	private Double degrees;

	public Double get() {
		return degrees;
	}

	public void set(Double latitude) {
		
		if (latitude < -0 || latitude > 360) {
			throw new IllegalArgumentException("Latitude must be between -0 and 360."); //$NON-NLS-1$
		}
		
		this.degrees = latitude;
	}
}
