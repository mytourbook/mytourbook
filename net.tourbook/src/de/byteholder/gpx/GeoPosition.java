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
 * An immutable coordinate in the real (geographic) world, composed of a latitude and a longitude.
 * 
 * @author rbair
 */
public class GeoPosition {

//	private Latitude	latitude;
//	private Longitude	longitude;

	public double	latitude;
	public double	longitude;

	/**
	 * Creates a new instance of GeoPosition from the specified latitude and longitude. These are
	 * double values in decimal degrees, not degrees, minutes, and seconds. Use the other
	 * constructor for those.
	 * 
	 * @param lat
	 *            a latitude value in decmial degrees
	 * @param lon
	 *            a longitude value in decimal degrees
	 */
	public GeoPosition(double lat, double lon) {

		if (lat > 90 || lat < -90) {

			lat = lat % 180;
			lat = lat > 90 ? //
					lat - 180
					: lat < -90 ? //
							lat + 180
							: lat;
		}
		latitude = lat;

		if (lon > 180 || lon < -180) {

			lon = lon % 360;
			lon = lon > 180 ? //
					lon - 360
					: lon < -180 ? //
							lon + 360
							: lon;
		}
		longitude = lon;
	}

//	/**
//	 * Creates a new instance of GeoPosition from the specified latitude and longitude. Each are
//	 * specified as degrees, minutes, and seconds; not as decimal degrees. Use the other constructor
//	 * for those.
//	 * 
//	 * @param latDegrees
//	 *            the degrees part of the current latitude
//	 * @param latMinutes
//	 *            the minutes part of the current latitude
//	 * @param latSeconds
//	 *            the seconds part of the current latitude
//	 * @param lonDegrees
//	 *            the degrees part of the current longitude
//	 * @param lonMinutes
//	 *            the minutes part of the current longitude
//	 * @param lonSeconds
//	 *            the seconds part of the current longitude
//	 */
//	public GeoPosition(	final double latDegrees,
//						final double latMinutes,
//						final double latSeconds,
//						final double lonDegrees,
//						final double lonMinutes,
//						final double lonSeconds) {
//		this(latDegrees + (latMinutes + latSeconds / 60.0) / 60.0, lonDegrees + (lonMinutes + lonSeconds / 60.0) / 60.0);
//	}

	/**
	 * Clone geo position
	 * 
	 * @param geoPosition
	 */
	public GeoPosition(final GeoPosition geoPosition) {
		latitude = geoPosition.latitude;
		longitude = geoPosition.longitude;
	}

//	/**
//	 * Returns true the specified GeoPosition and this GeoPosition represent the exact same latitude
//	 * and longitude coordinates.
//	 * 
//	 * @param obj
//	 *            a GeoPosition to compare this GeoPosition to
//	 * @return returns true if the specified GeoPosition is equal to this one
//	 */
//	@Override
//	public boolean equals(final Object obj) {
//		if (this == obj) {
//			return true;
//		}
//		if (obj == null) {
//			return false;
//		}
//		if (!(obj instanceof GeoPosition)) {
//			return false;
//		}
//		final GeoPosition other = (GeoPosition) obj;
//		if (latitude == null) {
//			if (other.latitude != null) {
//				return false;
//			}
//		} else if (!latitude.equals(other.latitude)) {
//			return false;
//		}
//		if (longitude == null) {
//			if (other.longitude != null) {
//				return false;
//			}
//		} else if (!longitude.equals(other.longitude)) {
//			return false;
//		}
//		return true;
//	}
//
//	@Override
//	public boolean equalsOLD(final Object obj) {
//		if (obj != null && obj instanceof GeoPosition) {
//			final GeoPosition coord = (GeoPosition) obj;
//			return latitude == coord.latitude && longitude == coord.longitude;
//		}
//		return false;
//	}
//
//	/**
//	 * Get the latitude as decimal degrees
//	 * 
//	 * @return the latitude as decimal degrees
//	 */
//	public double getLatitude() {
//		return latitude.get();
//	}
//
//	/**
//	 * Get the longitude as decimal degrees
//	 * 
//	 * @return the longitude as decimal degrees
//	 */
//	public double getLongitude() {
//		return longitude.get();
//	}

//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + ((latitude == null) ? 0 : latitude.hashCode());
//		result = prime * result + ((longitude == null) ? 0 : longitude.hashCode());
//		return result;
//	}

//	public void setLatitude(final double newlatitude) {
//		latitude.set(newlatitude);
//	}

	@Override
	public String toString() {
		return "lat:" + latitude + ", long:" + longitude; //$NON-NLS-1$ //$NON-NLS-2$ 
	}
}
