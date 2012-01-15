/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import java.awt.Point;
import java.io.File;
import java.util.HashMap;

import org.joda.time.DateTime;

import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.gpx.GeoPosition;

public class Photo {

	File									imageFile;
	String									fileName;
	private String							_filePathName;

	DateTime								dateTime;

	/**
	 * <pre>
	 * Orientation
	 * 
	 * The image orientation viewed in terms of rows and columns.
	 * Type		=      SHORT
	 * Default  =      1
	 * 
	 * 1  =     The 0th row is at the visual top of the image, and the 0th column is the visual left-hand side.
	 * 2  =     The 0th row is at the visual top of the image, and the 0th column is the visual right-hand side.
	 * 3  =     The 0th row is at the visual bottom of the image, and the 0th column is the visual right-hand side.
	 * 4  =     The 0th row is at the visual bottom of the image, and the 0th column is the visual left-hand side.
	 * 5  =     The 0th row is the visual left-hand side of the image, and the 0th column is the visual top.
	 * 6  =     The 0th row is the visual right-hand side of the image, and the 0th column is the visual top.
	 * 7  =     The 0th row is the visual right-hand side of the image, and the 0th column is the visual bottom.
	 * 8  =     The 0th row is the visual left-hand side of the image, and the 0th column is the visual bottom.
	 * Other        =     reserved
	 * </pre>
	 */
	int										orientation		= 1;

	int										width			= Integer.MIN_VALUE;

	int										height			= Integer.MIN_VALUE;

	double									latitude		= Double.MIN_VALUE;
	double									longitude		= Double.MIN_VALUE;

	private GeoPosition						_geoPosition;
	String									gpsAreaInfo;
	double									imageDirection	= Double.MIN_VALUE;

	double									altitude		= Double.MIN_VALUE;

	/**
	 * caches the world positions for the photo lat/long values for each zoom level
	 * <p>
	 * key: projection id + zoom level
	 */
	private final HashMap<Integer, Point>	_worldPosition	= new HashMap<Integer, Point>();

	/**
	 * 
	 */
	public Photo(final File imageFile) {

		this.imageFile = imageFile;
		fileName = imageFile.getName();
		_filePathName = imageFile.getAbsolutePath();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Photo)) {
			return false;
		}
		final Photo other = (Photo) obj;
		if (_filePathName == null) {
			if (other._filePathName != null) {
				return false;
			}
		} else if (!_filePathName.equals(other._filePathName)) {
			return false;
		}
		return true;
	}

	/**
	 * @return Returns geo position or <code>null</code> when latitude/longitude is not available
	 */
	public GeoPosition getGeoPosition() {

		if (_geoPosition == null) {

			if (latitude == Double.MIN_VALUE || longitude == Double.MIN_VALUE) {
				return null;
			} else {
				_geoPosition = new GeoPosition(latitude, longitude);
			}
		}

		return _geoPosition;
	}

	/**
	 * @param mapProvider
	 * @param projectionId
	 * @param zoomLevel
	 * @return Returns the world position for this photo
	 */
	public Point getWorldPosition(final MP mapProvider, final String projectionId, final int zoomLevel) {

		if (latitude == Double.MIN_VALUE) {
			return null;
		}

		final int hashKey = projectionId.hashCode() + zoomLevel;

		Point worldPosition = _worldPosition.get(hashKey);

		if ((worldPosition == null)) {
			// convert lat/long into world pixels which depends on the map projection

			final GeoPosition geoPosition = new GeoPosition(longitude, latitude);

			worldPosition = _worldPosition.put(hashKey, mapProvider.geoToPixel(geoPosition, zoomLevel));
		}

		return worldPosition;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_filePathName == null) ? 0 : _filePathName.hashCode());
		return result;
	}

}
