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
 * Represents a differential GPS station.
 * 
 * @author Michael Kanis
 */
public class DgpsStation {
	
	private Short stationId;

	public Short get() {
		return stationId;
	}

	public void set(Short stationId) {
		
		if (stationId < 0 || stationId > 1023) {
			throw new IllegalArgumentException("DGPS Station ID must be between 0 and 1023."); //$NON-NLS-1$
		}
		
		this.stationId = stationId;
	}
}
