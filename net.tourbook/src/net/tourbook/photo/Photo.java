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

	private File							_imageFile;
	private String							_fileName;
	private String							_filePathName;

	private DateTime						_dateTime;

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
	private int								_orientation	= 1;

	private int								_width			= Integer.MIN_VALUE;
	private int								_widthSmall;

	private int								_height			= Integer.MIN_VALUE;
	private int								_heightSmall;

	private double							_latitude		= Double.MIN_VALUE;
	private double							_longitude		= Double.MIN_VALUE;

	private GeoPosition						_geoPosition;
	private String							_gpsAreaInfo;
	private double							_imageDirection	= Double.MIN_VALUE;

	private double							_altitude		= Double.MIN_VALUE;

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

		this._imageFile = imageFile;

		_fileName = imageFile.getName();
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

	public double getAltitude() {
		return _altitude;
	}

	public DateTime getDateTime() {
		return _dateTime;
	}

	public String getFileName() {
		return _fileName;
	}

	/**
	 * @return Returns geo position or <code>null</code> when latitude/longitude is not available
	 */
	public GeoPosition getGeoPosition() {

		if (_geoPosition == null) {

			if (_latitude == Double.MIN_VALUE || _longitude == Double.MIN_VALUE) {
				return null;
			} else {
				_geoPosition = new GeoPosition(_latitude, _longitude);
			}
		}

		return _geoPosition;
	}

	public String getGpsAreaInfo() {
		return _gpsAreaInfo;
	}

	public int getHeight() {
		return _height;
	}

	public int getHeightSmall() {
		return _heightSmall;
	}

	public double getImageDirection() {
		return _imageDirection;
	}

	public File getImageFile() {
		return _imageFile;
	}

	public double getLatitude() {
		return _latitude;
	}

	public double getLongitude() {
		return _longitude;
	}

	public int getOrientation() {
		return _orientation;
	}

	public int getWidth() {
		return _width;
	}

	public int getWidthSmall() {
		return _widthSmall;
	}

	/**
	 * @param mapProvider
	 * @param projectionId
	 * @param zoomLevel
	 * @return Returns the world position for this photo
	 */
	public Point getWorldPosition(final MP mapProvider, final String projectionId, final int zoomLevel) {

		if (_latitude == Double.MIN_VALUE) {
			return null;
		}

		final Integer hashKey = projectionId.hashCode() + zoomLevel;

		Point worldPosition = _worldPosition.get(hashKey);

		if ((worldPosition == null)) {
			// convert lat/long into world pixels which depends on the map projection

			final GeoPosition photoGeoPosition = new GeoPosition(_latitude, _longitude);

			final Point geoToPixel = mapProvider.geoToPixel(photoGeoPosition, zoomLevel);

			worldPosition = _worldPosition.put(hashKey, geoToPixel);
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

	public void setAltitude(final double altitude) {
		this._altitude = altitude;
	}

	public void setDateTime(final DateTime dateTime) {
		this._dateTime = dateTime;
	}

	public void setGpsAreaInfo(final String gpsAreaInfo) {
		this._gpsAreaInfo = gpsAreaInfo;
	}

	public void setImageDirection(final double imageDirection) {
		this._imageDirection = imageDirection;
	}

	public void setLatitude(final double latitude) {
		this._latitude = latitude;
	}

	public void setLongitude(final double longitude) {
		this._longitude = longitude;
	}

	public void setOrientation(final int orientation) {
		this._orientation = orientation;
	}

	public void setSize(final int height, final int width) {

		this._width = width;
		this._height = height;

		final int SIZE_SMALL = 20;
		final float ratio = (float) width / height;

		_widthSmall = width > SIZE_SMALL ? SIZE_SMALL : width;
		_heightSmall = (int) (_widthSmall / ratio);
	}

}
