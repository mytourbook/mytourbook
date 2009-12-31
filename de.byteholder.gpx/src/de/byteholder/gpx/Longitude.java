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
 * The longitude of the point. Decimal degrees, WGS84 datum.
 */
public class Longitude {

	private Double	longitude;

	public Longitude() {}

	public Longitude(double longitude) {

		if (longitude > 180 || longitude < -180) {

			longitude = longitude % 360;
			longitude = longitude > 180 ? //
					longitude - 360
					: longitude < -180 ? //
							longitude + 360
							: longitude;
		}

		this.longitude = longitude;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Longitude)) {
			return false;
		}
		final Longitude other = (Longitude) obj;
		if (longitude == null) {
			if (other.longitude != null) {
				return false;
			}
		} else if (!longitude.equals(other.longitude)) {
			return false;
		}
		return true;
	}

	public Double get() {
		return longitude;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((longitude == null) ? 0 : longitude.hashCode());
		return result;
	}

	public void set(final Double longitude) {

		if (longitude < -180 || longitude > 180) {
			throw new IllegalArgumentException("Longitude must be between -180 and 180."); //$NON-NLS-1$
		}

		this.longitude = longitude;
	}
}
