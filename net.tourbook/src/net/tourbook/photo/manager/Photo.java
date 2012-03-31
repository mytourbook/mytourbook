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
package net.tourbook.photo.manager;

import java.awt.Point;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

import net.tourbook.util.StatusUtil;
import net.tourbook.util.Util;

import org.apache.commons.sanselan.ImageReadException;
import org.apache.commons.sanselan.Sanselan;
import org.apache.commons.sanselan.common.IImageMetadata;
import org.apache.commons.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.commons.sanselan.formats.tiff.TiffField;
import org.apache.commons.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.commons.sanselan.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.sanselan.formats.tiff.constants.GpsTagConstants;
import org.apache.commons.sanselan.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.sanselan.formats.tiff.taginfos.TagInfo;
import org.apache.commons.sanselan.formats.tiff.taginfos.TagInfoShortOrLong;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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

	/**
	 * Contains photo image width after it is rotated with the EXIF orientation
	 */
	private int								_widthRotated	= _width;

	/**
	 * Contains photo image height after it is rotated with the EXIF orientation
	 */
	private int								_heightRotated	= _height;

	private int								_heightSmall;
	private double							_latitude		= Double.MIN_VALUE;

	private double							_longitude		= Double.MIN_VALUE;

	private GeoPosition						_geoPosition;
	private String							_gpsAreaInfo;

	private double							_imageDirection	= Double.MIN_VALUE;
	private double							_altitude		= Double.MIN_VALUE;
	private static final DateTimeFormatter	_dtParser		= DateTimeFormat.forPattern("yyyy:MM:dd HH:mm:ss")// //$NON-NLS-1$
																	.withZone(DateTimeZone.UTC);

	/**
	 * caches the world positions for the photo lat/long values for each zoom level
	 * <p>
	 * key: projection id + zoom level
	 */
	private final HashMap<Integer, Point>	_worldPosition	= new HashMap<Integer, Point>();

	/**
	 * Location index in the gallery widget
	 */
	private int								_galleryItemIndex;

	/**
	 * Contains image keys for each image quality which can be used to get images from an image
	 * cache
	 */
	private String[]						_imageKeys;

	/**
	 * This array keeps track of the loading state for the photo images and for different qualities
	 */
	private PhotoLoadingState[]				_photoLoadingState;
	private IImageMetadata					_metadata;

	/**
	 * @param galleryItemIndex
	 */
	public Photo(final File imageFile, final int galleryItemIndex) {

		_imageFile = imageFile;
		_galleryItemIndex = galleryItemIndex;

		_fileName = imageFile.getName();
		_filePathName = imageFile.getAbsolutePath();

		/*
		 * initialize image keys and loading states
		 */
		final int imageSizeLength = PhotoManager.IMAGE_SIZES.length;
		_imageKeys = new String[imageSizeLength];
		_photoLoadingState = new PhotoLoadingState[imageSizeLength];

		for (int qualityIndex = 0; qualityIndex < _photoLoadingState.length; qualityIndex++) {
			_imageKeys[qualityIndex] = Util.computeMD5(_filePathName + "_" + qualityIndex);
			_photoLoadingState[qualityIndex] = PhotoLoadingState.UNDEFINED;
		}
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

	/**
	 * Date/Time
	 * 
	 * @param jpegMetadata
	 * @param file
	 * @return
	 */
	private DateTime getExifDate(final JpegImageMetadata jpegMetadata) {

//		/*
//		 * !!! time is not correct, maybe it is the time when the GPS signal was
//		 * received !!!
//		 */
//		printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_TIME_STAMP);

		try {

			final TiffField date = jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_DATE_TIME);

			if (date != null) {
				return _dtParser.parseDateTime(date.getStringValue());
			}

		} catch (final Exception e) {
			// ignore
		}

		return null;
	}

	/**
	 * GPS area info
	 */
	private String getExifGpsArea(final JpegImageMetadata jpegMetadata) {

		try {
			final TiffField field = jpegMetadata
					.findEXIFValueWithExactMatch(GpsTagConstants.GPS_TAG_GPS_AREA_INFORMATION);
			if (field != null) {
				final Object fieldValue = field.getValue();
				if (fieldValue != null) {

					/**
					 * source: Exif 2.2 specification
					 * 
					 * <pre>
					 * 
					 * Table 6 Character Codes and their Designation
					 * 
					 * Character Code	Code Designation (8 Bytes) 						References
					 * ASCII  			41.H, 53.H, 43.H, 49.H, 49.H, 00.H, 00.H, 00.H  ITU-T T.50 IA5
					 * JIS				A.H, 49.H, 53.H, 00.H, 00.H, 00.H, 00.H, 00.H   JIS X208-1990
					 * Unicode			55.H, 4E.H, 49.H, 43.H, 4F.H, 44.H, 45.H, 00.H  Unicode Standard
					 * Undefined		00.H, 00.H, 00.H, 00.H, 00.H, 00.H, 00.H, 00.H  Undefined
					 * 
					 * </pre>
					 */
					final byte[] byteArrayValue = field.getByteArrayValue();
					final int fieldLength = byteArrayValue.length;

					if (fieldLength > 0) {

						/**
						 * <pre>
						 * 
						 * skipping 1 + 6 characters:
						 * 
						 * 1      character code
						 * 2...7  have no idea why these bytes are set to none valid characters
						 * 
						 * </pre>
						 */
						final byte[] valueBytes = Arrays.copyOfRange(byteArrayValue, 7, fieldLength);

						String valueString = null;

						final byte firstByte = byteArrayValue[0];
						if (firstByte == 0x55) {

							valueString = new String(valueBytes, "UTF-16");

						} else {

							valueString = new String(valueBytes);
						}

						return valueString;
					}
				}
			}
		} catch (final Exception e) {
			// ignore
		}

		return null;
	}

	private int getExifIntValue(final JpegImageMetadata jpegMetadata, final TagInfo tiffTag, final int defaultValue) {

		try {
			final TiffField field = jpegMetadata.findEXIFValueWithExactMatch(tiffTag);
			if (field != null) {
				return field.getIntValue();
			}
		} catch (final Exception e) {
			// ignore
		}

		return defaultValue;
	}

	/**
	 * Image direction
	 * 
	 * @param tagInfo
	 */
	private double getExifValueDouble(final JpegImageMetadata jpegMetadata, final TagInfo tagInfo) {
		try {
			final TiffField field = jpegMetadata.findEXIFValueWithExactMatch(tagInfo);
			if (field != null) {
				return field.getDoubleValue();
			}
		} catch (final Exception e) {
			// ignore
		}

		return Double.MIN_VALUE;
	}

	public String getFileName() {
		return _fileName;
	}

	/**
	 * @return Returns the absolute pathname for the fullsize image
	 */
	public String getFilePathName() {
		return _filePathName;
	}

	public int getGalleryIndex() {
		return _galleryItemIndex;
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

	/**
	 * @return Returns photo image height after it is rotated with the EXIF orientation
	 */
	public int getHeightRotated() {
		return _heightRotated;
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

	/**
	 * @param imageQuality
	 * @return Returns an image key which can be used to get images from an image cache. This key is
	 *         a MD5 hash from the full image file path and the image quality.
	 */
	public String getImageKey(final int imageQuality) {
		return _imageKeys[imageQuality];
	}

	public double getLatitude() {
		return _latitude;
	}

	/**
	 * @param imageQuality
	 * @return Returns the loading state for each photo quality
	 */
	public PhotoLoadingState getLoadingState(final int imageQuality) {
		return _photoLoadingState[imageQuality];
	}

	public double getLongitude() {
		return _longitude;
	}

	public IImageMetadata getMetaData() {

		if (_metadata != null) {
			return _metadata;
		}

		try {

			// SanselanConstants.PARAM_KEY_READ_THUMBNAILS

			// read metadata WITH thumbnail image info
			_metadata = Sanselan.getMetadata(_imageFile, new HashMap<Object, Object>());

			/*
			 * this will log all available meta data
			 */
//			System.out.println(metadata.toString());

			/*
			 * read meta data for this photo
			 */
			if (_metadata instanceof TiffImageMetadata) {

				final TiffImageMetadata tiffMetadata = (TiffImageMetadata) _metadata;

				_dateTime = getTiffDate(tiffMetadata);

				setSize(
						getTiffIntValue(tiffMetadata, TiffTagConstants.TIFF_TAG_IMAGE_WIDTH, Integer.MIN_VALUE),
						getTiffIntValue(tiffMetadata, TiffTagConstants.TIFF_TAG_IMAGE_LENGTH, Integer.MIN_VALUE),
						1);

			} else if (_metadata instanceof JpegImageMetadata) {

				final JpegImageMetadata jpegMetadata = (JpegImageMetadata) _metadata;

				_dateTime = getExifDate(jpegMetadata);

				_orientation = getExifIntValue(jpegMetadata, ExifTagConstants.EXIF_TAG_ORIENTATION, 1);

				setSize(
						getExifIntValue(jpegMetadata, ExifTagConstants.EXIF_TAG_EXIF_IMAGE_WIDTH, Integer.MIN_VALUE),
						getExifIntValue(jpegMetadata, ExifTagConstants.EXIF_TAG_EXIF_IMAGE_LENGTH, Integer.MIN_VALUE),
						_orientation);

				_imageDirection = getExifValueDouble(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_IMG_DIRECTION);
				_altitude = getExifValueDouble(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_ALTITUDE);

				setExifLatLon(jpegMetadata);
				_gpsAreaInfo = getExifGpsArea(jpegMetadata);
			}

			// ensure date is set
			if (_dateTime == null) {
				_dateTime = new DateTime(_imageFile.lastModified());
			}

		} catch (final Exception e) {
			StatusUtil.log(e);
		}

		return _metadata;
	}

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
	 * 
	 * @return
	 */
	public int getOrientation() {
		return _orientation;
	}

	private DateTime getTiffDate(final TiffImageMetadata tiffMetadata) {

		try {

			final TiffField date = tiffMetadata.findField(TiffTagConstants.TIFF_TAG_DATE_TIME, true);
			if (date != null) {
				return _dtParser.parseDateTime(date.getStringValue());
			}

		} catch (final Exception e) {
			// ignore
		}

		return null;
	}

	private int getTiffIntValue(final TiffImageMetadata tiffMetadata,
								final TagInfoShortOrLong tiffTag,
								final int defaultValue) {

		try {
			final TiffField field = tiffMetadata.findField(tiffTag, true);
			if (field != null) {
				return field.getIntValue();
			}
		} catch (final Exception e) {
			// ignore
		}

		return defaultValue;
	}

	public int getWidth() {
		return _width;
	}

	/**
	 * @return Return photo image width after it is rotated with the EXIF orientation
	 */
	public int getWidthRotated() {
		return _widthRotated;
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
		_altitude = altitude;
	}

	public void setDateTime(final DateTime dateTime) {
		_dateTime = dateTime;
	}

	/**
	 * Latitude + lLongitude
	 * 
	 * @param jpegMetadata
	 * @param file
	 * @throws ImageReadException
	 */
	private void setExifLatLon(final JpegImageMetadata jpegMetadata) {

		final TiffImageMetadata exifMetadata = jpegMetadata.getExif();
		if (exifMetadata != null) {

			try {
				final TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS();
				if (gpsInfo != null) {

					_latitude = gpsInfo.getLatitudeAsDegreesNorth();
					_longitude = gpsInfo.getLongitudeAsDegreesEast();
				}
			} catch (final Exception e) {
				// ignore
			}
		}
	}

	public void setGpsAreaInfo(final String gpsAreaInfo) {
		_gpsAreaInfo = gpsAreaInfo;
	}

	public void setLatitude(final double latitude) {
		_latitude = latitude;
	}

	public void setLoadingState(final PhotoLoadingState photoLoadingState, final int imageQuality) {
		_photoLoadingState[imageQuality] = photoLoadingState;
	}

	public void setLongitude(final double longitude) {
		_longitude = longitude;
	}

	public void setSize(final int width, final int height, final int orientation) {

		_width = width;
		_height = height;

		final int SIZE_SMALL = 20;
		final float ratio = (float) width / height;

		_widthSmall = width > SIZE_SMALL ? SIZE_SMALL : width;
		_heightSmall = (int) (_widthSmall / ratio);

		boolean isSwapWidthHeight = false;

		if (orientation > 1) {

			// see here http://www.impulseadventure.com/photo/exif-orientation.html

			if (orientation == 8) {
				isSwapWidthHeight = true;
			} else if (orientation == 6) {
				isSwapWidthHeight = true;
			}
		}

		if (isSwapWidthHeight) {
			_widthRotated = _height;
			_heightRotated = _width;
		} else {
			_widthRotated = _width;
			_heightRotated = _height;
		}
	}

	@Override
	public String toString() {
		return "Photo "
				+ (_fileName)
				+ (_dateTime == null ? "-no date-" : "\t" + _dateTime)
				+ (_width == Integer.MIN_VALUE ? "-no size-" : "\t" + _width + "x" + _height)
				+ (_latitude == Double.MIN_VALUE ? "-no GPS-" : "\t" + _latitude + "" + _longitude)
		//
		;
	}

}
