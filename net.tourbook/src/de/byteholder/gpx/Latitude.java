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
 * The latitude of a point. Decimal degrees, WGS84 datum.
 */
public class Latitude {

	private double	latitude;

	public Latitude() {}

	public Latitude(double latitude) {

		if (latitude > 90 || latitude < -90) {

			latitude = latitude % 180;
			latitude = latitude > 90 ? //
					latitude - 180
					: latitude < -90 ? //
							latitude + 180
							: latitude;
		}

		this.latitude = latitude;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Latitude)) {
			return false;
		}
		final Latitude other = (Latitude) obj;
		if (Double.doubleToLongBits(latitude) != Double.doubleToLongBits(other.latitude)) {
			return false;
		}
		return true;
	}

	public double get() {
		return latitude;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(latitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	public void set(final double latitude) {

		if (latitude < -90 || latitude > 90) {
			throw new IllegalArgumentException("Latitude must be between -90 and 90."); //$NON-NLS-1$
		}

		this.latitude = latitude;
	}
}
