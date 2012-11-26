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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.common.map.CommonMapProvider;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.photo.internal.gallery.MT20.RendererHelper;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingConstants;
import org.apache.commons.imaging.common.IImageMetadata;
import org.apache.commons.imaging.common.IImageMetadata.IImageMetadataItem;
import org.apache.commons.imaging.common.ImageMetadata.Item;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegPhotoshopMetadata;
import org.apache.commons.imaging.formats.jpeg.iptc.IptcTypes;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoShortOrLong;
import org.eclipse.osgi.util.NLS;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Photo {

	private PhotoWrapper					_photoWrapper;

	private String							_uniqueId;

	private PhotoImageMetadata				_photoImageMetadata;

	/**
	 * Last modified in GMT
	 */
	private DateTime						_imageFileDateTime;
	private DateTime						_exifDateTime;

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
	private int								_orientation			= 1;

	private int								_imageWidth				= Integer.MIN_VALUE;
	private int								_imageHeight			= Integer.MIN_VALUE;

	/**
	 * When <code>true</code>, EXIF geo is returned when available, otherwise tour geo is returned
	 * when available. When requested geo is not available, the other is returned.
	 */
	private static boolean					_isGetExifGeo			= false;

	private double							_exifLatitude			= Double.MIN_VALUE;
	private double							_exifLongitude			= Double.MIN_VALUE;
	private double							_tourLatitude			= Double.MIN_VALUE;
	private double							_tourLongitude			= Double.MIN_VALUE;

	private String							_gpsAreaInfo;

	private double							_imageDirection			= Double.MIN_VALUE;
	private double							_altitude				= Double.MIN_VALUE;

	private static final DateTimeFormatter	_dtParser				= DateTimeFormat//
																			.forPattern("yyyy:MM:dd HH:mm:ss") //$NON-NLS-1$
//																			.withZone(DateTimeZone.UTC)
																	;
//	private static final DateTimeFormatter	_dtFormatter			= DateTimeFormat.forStyle("SL");	//$NON-NLS-1$

	/**
	 * caches the world positions for the photo lat/long values for each zoom level
	 * <p>
	 * key: projection id + zoom level
	 */
	private final HashMap<Integer, Point>	_worldPosition			= new HashMap<Integer, Point>();

	/**
	 * Contains image keys for each image quality which can be used to get images from an image
	 * cache
	 */
	private String							_imageKeyThumb;
	private String							_imageKeyHQ;
	private String							_imageKeyOriginal;

	/**
	 * This array keeps track of the loading state for the photo images and for different qualities
	 */
	private PhotoLoadingState				_photoLoadingStateThumb;
	private PhotoLoadingState				_photoLoadingStateHQ;
	private PhotoLoadingState				_photoLoadingStateOriginal;

	/**
	 * Is <code>true</code> when loading the image causes an error.
	 */
	private boolean							_isLoadingError;

	/**
	 * Exif thumb state
	 * <p>
	 * 
	 * <pre>
	 * 1  exif thumb image is available
	 * 0  exif thumb image is not available
	 * -1 exif thumb has not yet been retrieved
	 * </pre>
	 */
	private int								_exifThumbImageState	= -1;

	/**
	 * Image size which is painted in the map
	 */
	private org.eclipse.swt.graphics.Point	_mapImageSize;

	/**
	 * @param galleryItemIndex
	 */
	public Photo(final PhotoWrapper photoWrapper) {

		_photoWrapper = photoWrapper;

		final String imageFilePathName = photoWrapper.imageFilePathName;

		_uniqueId = imageFilePathName;

		/*
		 * initialize image keys and loading states
		 */
		_imageKeyThumb = getImageKeyThumb(imageFilePathName);
		_imageKeyHQ = getImageKeyHQ(imageFilePathName);
		_imageKeyOriginal = Util.computeMD5(imageFilePathName + "_Original");//$NON-NLS-1$

		_photoLoadingStateThumb = PhotoLoadingState.UNDEFINED;
		_photoLoadingStateHQ = PhotoLoadingState.UNDEFINED;
		_photoLoadingStateOriginal = PhotoLoadingState.UNDEFINED;

		_isLoadingError = false;
	}

	public static String getImageKeyHQ(final String imageFilePathName) {
		return Util.computeMD5(imageFilePathName + "_HQ");//$NON-NLS-1$
	}

	public static String getImageKeyThumb(final String imageFilePathName) {
		return Util.computeMD5(imageFilePathName + "_Thumb");//$NON-NLS-1$
	}

//	/**
//	 * Update geo position in the cached exif metadata.
//	 *
//	 * @param updatedPhotos
//	 */
//	public static void updateExifGeoPosition(final ArrayList<PhotoWrapper> updatedPhotos) {
//
//		for (final PhotoWrapper photoWrapper : updatedPhotos) {
//
//			final PhotoImageMetadata imageMetadata = ExifCache.get(photoWrapper.imageFilePathName);
//			if (imageMetadata != null) {
//
//				imageMetadata.latitude = photoWrapper.photo.getLatitude();
//				imageMetadata.longitude = photoWrapper.photo.getLongitude();
//			}
//		}
//	}

	/**
	 * Creates metadata from image metadata
	 * 
	 * @param imageMetadata
	 *            Can be <code>null</code> when not available
	 * @return
	 */
	private PhotoImageMetadata createPhotoMetadata(final IImageMetadata imageMetadata) {

// this will log all available meta data
//		System.out.println(imageMetadata.toString());

		final PhotoImageMetadata photoMetadata = new PhotoImageMetadata();

		/*
		 * read meta data for this photo
		 */
		if (imageMetadata instanceof TiffImageMetadata) {

			final TiffImageMetadata tiffMetadata = (TiffImageMetadata) imageMetadata;

			photoMetadata.exifDateTime = getTiffValueDate(tiffMetadata);

			photoMetadata.orientation = 1;

			photoMetadata.imageWidth = getTiffValueInt(
					tiffMetadata,
					TiffTagConstants.TIFF_TAG_IMAGE_WIDTH,
					Integer.MIN_VALUE);

			photoMetadata.imageHeight = getTiffValueInt(
					tiffMetadata,
					TiffTagConstants.TIFF_TAG_IMAGE_LENGTH,
					Integer.MIN_VALUE);

			photoMetadata.model = getTiffValueString(tiffMetadata, TiffTagConstants.TIFF_TAG_MODEL);

		} else if (imageMetadata instanceof JpegImageMetadata) {

			final JpegImageMetadata jpegMetadata = (JpegImageMetadata) imageMetadata;

			photoMetadata.exifDateTime = getExifValueDate(jpegMetadata);

			photoMetadata.orientation = getExifValueInt(jpegMetadata, TiffTagConstants.TIFF_TAG_ORIENTATION, 1);

			photoMetadata.imageWidth = getExifValueInt(
					jpegMetadata,
					ExifTagConstants.EXIF_TAG_EXIF_IMAGE_WIDTH,
					Integer.MIN_VALUE);
			photoMetadata.imageHeight = getExifValueInt(
					jpegMetadata,
					ExifTagConstants.EXIF_TAG_EXIF_IMAGE_LENGTH,
					Integer.MIN_VALUE);

			photoMetadata.imageDirection = getExifValueDouble(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_IMG_DIRECTION);
			photoMetadata.altitude = getExifValueDouble(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_ALTITUDE);

			photoMetadata.model = getExifValueString(jpegMetadata, TiffTagConstants.TIFF_TAG_MODEL);

			/*
			 * GPS
			 */
			final TiffImageMetadata exifMetadata = jpegMetadata.getExif();
			if (exifMetadata != null) {

				try {
					final TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS();
					if (gpsInfo != null) {

						photoMetadata.latitude = gpsInfo.getLatitudeAsDegreesNorth();
						photoMetadata.longitude = gpsInfo.getLongitudeAsDegreesEast();
					}
				} catch (final Exception e) {
					// ignore
				}
			}
			photoMetadata.gpsAreaInfo = getExifValueGpsArea(jpegMetadata);

			/*
			 * photoshop metadata
			 */
			final JpegPhotoshopMetadata pshopMetadata = jpegMetadata.getPhotoshop();
			if (pshopMetadata != null) {

				final List<? extends IImageMetadataItem> pshopItems = pshopMetadata.getItems();

				for (final IImageMetadataItem pshopItem : pshopItems) {

					if (pshopItem instanceof Item) {

						final Item item = (Item) pshopItem;
						final String keyword = item.getKeyword();

						if (keyword.equals(IptcTypes.OBJECT_NAME.name)) {

							photoMetadata.objectName = item.getText();

						} else if (keyword.equals(IptcTypes.CAPTION_ABSTRACT.name)) {
							photoMetadata.captionAbstract = item.getText();
						}
					}
				}

			}
		}

		// set file date time
//		photoMetadata.fileDateTime = new DateTime(DateTimeZone.UTC).withMillis(_photoWrapper.imageFileLastModified);
		photoMetadata.fileDateTime = new DateTime(_photoWrapper.imageFileLastModified);

		return photoMetadata;
	}

	public String dumpLoadingState() {

		final StringBuilder sb = new StringBuilder();

		sb.append("Thumb:" + _photoLoadingStateThumb); //$NON-NLS-1$
		sb.append("\tHQ:" + _photoLoadingStateHQ); //$NON-NLS-1$
		sb.append("\tOriginal:" + _photoLoadingStateOriginal); //$NON-NLS-1$

		return sb.toString();
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
		if (_uniqueId == null) {
			if (other._uniqueId != null) {
				return false;
			}
		} else if (!_uniqueId.equals(other._uniqueId)) {
			return false;
		}
		return true;
	}

	public double getAltitude() {
		return _altitude;
	}

	public DateTime getExifDateTime() {
		return _exifDateTime;
	}

	/**
	 * @return Returns EXIF thumb image stage
	 * 
	 *         <pre>
	 * 1  exif thumb image is available
	 * 0  exif thumb image is not available
	 * -1 exif thumb has not yet been loaded
	 * </pre>
	 */
	public int getExifThumbImageState() {
		return _exifThumbImageState;
	}

	/**
	 * Date/Time
	 * 
	 * @param jpegMetadata
	 * @param file
	 * @return
	 */
	private DateTime getExifValueDate(final JpegImageMetadata jpegMetadata) {

//		/*
//		 * !!! time is not correct, maybe it is the time when the GPS signal was
//		 * received !!!
//		 */
//		printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_TIME_STAMP);

		try {

			final TiffField exifDate = jpegMetadata.findEXIFValueWithExactMatch(//
					ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);

			if (exifDate != null) {

				final DateTime parsedExifDateTime = _dtParser.parseDateTime(exifDate.getStringValue());

//				System.out.println((exifDate.getValueDescription())
//						+ ("\t" + parsedExifDateTime)
//						+ ("\t" + parsedExifDateTime.toDateTime(DateTimeZone.UTC))
//						+ ("\t" + _dtFormatter.print(parsedExifDateTime)));
//				// TODO remove SYSTEM.OUT.PRINTLN

				return parsedExifDateTime;
			}

			final TiffField tiffDate = jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_DATE_TIME);

			if (tiffDate != null) {
				return _dtParser.parseDateTime(tiffDate.getStringValue());
			}

		} catch (final Exception e) {
			// ignore
		}

		return null;
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

	/**
	 * GPS area info
	 */
	private String getExifValueGpsArea(final JpegImageMetadata jpegMetadata) {

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

							valueString = new String(valueBytes, UI.UTF_16);

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

	private int getExifValueInt(final JpegImageMetadata jpegMetadata, final TagInfo tiffTag, final int defaultValue) {

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

	private String getExifValueString(final JpegImageMetadata jpegMetadata, final TagInfo tagInfo) {

		try {
			final TiffField field = jpegMetadata.findEXIFValueWithExactMatch(tagInfo);
			if (field != null) {
				return field.getStringValue();
			}
		} catch (final Exception e) {
			// ignore
		}

		return null;
	}

	public String getGpsAreaInfo() {
		return _gpsAreaInfo;
	}

	/**
	 * @return Returns the cardinal direction (Himmelsrichtung) in degrees or
	 *         {@link Double#MIN_VALUE} when not set.
	 */
	public double getImageDirection() {
		return _imageDirection;
	}

	public DateTime getImageFileDateTime() {
		return _imageFileDateTime;
	}

	public int getImageHeight() {
		return _imageHeight;
	}

	/**
	 * @return Returns an image key which can be used to get images from an image cache. This key is
	 *         a MD5 hash from the full image file path and the image quality.
	 */
	public String getImageKey(final ImageQuality imageQuality) {

		if (imageQuality == ImageQuality.HQ) {
			return _imageKeyHQ;
		} else if (imageQuality == ImageQuality.ORIGINAL) {
			return _imageKeyOriginal;
		} else {
			return _imageKeyThumb;
		}
	}

	/**
	 * Updates metadata from image file.
	 * 
	 * @return Returns photo image metadata, metadata is loaded from the image file when it's not
	 *         yet loaded.
	 */
	public PhotoImageMetadata getImageMetaData() {

		if (_photoImageMetadata == null) {
			getImageMetaData(false);
		}

		return _photoImageMetadata;
	}

	/**
	 * Updated metadata from the image file
	 * 
	 * @param isReadThumbnail
	 * @return Returns image metadata <b>with</b> image thumbnail <b>only</b> when
	 *         <code>isReadThumbnail</code> is <code>true</code>, otherwise it checks if metadata
	 *         are already loaded.
	 */
	public IImageMetadata getImageMetaData(final Boolean isReadThumbnail) {

		if (_photoImageMetadata != null && isReadThumbnail == false) {

			// meta data are available but the exif thumnail is not requested

			return null;
		}

		if (PhotoLoadManager.isImageLoadingError(_photoWrapper.imageFilePathName)) {
			// image could not be loaded previously
			return null;
		}

		IImageMetadata imageFileMetadata = null;

		try {

			/*
			 * read metadata WITH thumbnail image info, this is the default when the pamameter is
			 * ommitted
			 */
			final HashMap<String, Object> params = new HashMap<String, Object>();
			params.put(ImagingConstants.PARAM_KEY_READ_THUMBNAILS, isReadThumbnail);

//			final long start = System.currentTimeMillis();

			imageFileMetadata = Imaging.getMetadata(_photoWrapper.imageFile, params);

//			System.out.println(UI.timeStamp()
//					+ Thread.currentThread().getName()
//					+ "read exif\t"
//					+ ((System.currentTimeMillis() - start) + " ms")
//					+ ("\tWithThumb: " + isReadThumbnail)
//					+ ("\t" + _photoWrapper.imageFilePathName)
//			//
//					);
//			// TODO remove SYSTEM.OUT.PRINTLN
//
//			System.out.println(imageFileMetadata);
//			// TODO remove SYSTEM.OUT.PRINTLN

		} catch (final Exception e) {

			StatusUtil.log(NLS.bind(//
					"Could not read metadata from image \"{0}\"", //$NON-NLS-1$
					_photoWrapper.imageFile));

			PhotoLoadManager.putPhotoInLoadingErrorMap(getPhotoWrapper().imageFilePathName);

		} finally {

			final PhotoImageMetadata photoImageMetadata = createPhotoMetadata(imageFileMetadata);

			updateImageMetadata(photoImageMetadata);
		}

		return imageFileMetadata;
	}

	/**
	 * @return Returns image meta data or <code>null</code> when not loaded or not available.
	 */
	public PhotoImageMetadata getImageMetaDataRaw() {
		return _photoImageMetadata;
	}

	/**
	 * @return Returns photo image width or {@link Integer#MIN_VALUE} when width is not set.
	 */
	public int getImageWidth() {
		return _imageWidth;
	}

	/**
	 * @return Returns latitude or {@link Double#MIN_VALUE} when not set
	 */
	public double getLatitude() {

		return _isGetExifGeo //
				? _exifLatitude != Double.MIN_VALUE //
						? _exifLatitude
						: _tourLatitude
				: _tourLatitude != Double.MIN_VALUE //
						? _tourLatitude
						: _exifLatitude;
	}

	/**
	 * @return Returns the loading state for the given photo quality
	 */
	public PhotoLoadingState getLoadingState(final ImageQuality imageQuality) {

		if (imageQuality == ImageQuality.HQ) {
			return _photoLoadingStateHQ;
		} else if (imageQuality == ImageQuality.ORIGINAL) {
			return _photoLoadingStateOriginal;
		} else {
			return _photoLoadingStateThumb;
		}
	}

	/**
	 * @return Returns longitude or {@link Double#MIN_VALUE} when not set
	 */
	public double getLongitude() {

		return _isGetExifGeo //
				? _exifLongitude != Double.MIN_VALUE //
						? _exifLongitude
						: _tourLongitude
				: _tourLongitude != Double.MIN_VALUE //
						? _tourLongitude
						: _exifLongitude;
	}

	/**
	 * @return Returns size when image is painted on the map or <code>null</code>, when not yet set.
	 */
	public org.eclipse.swt.graphics.Point getMapImageSize() {
		return _mapImageSize;
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

	/**
	 * @return Return date/time for the image. First EXIF date is returned, when not available,
	 *         image file date/time is returned.
	 */
	public DateTime getOriginalDateTime() {
		return _exifDateTime != null ? _exifDateTime : _imageFileDateTime;
	}

	public PhotoWrapper getPhotoWrapper() {
		return _photoWrapper;
	}

	private DateTime getTiffValueDate(final TiffImageMetadata tiffMetadata) {

		try {

			final TiffField exifDate = tiffMetadata.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, true);

			if (exifDate != null) {
				return _dtParser.parseDateTime(exifDate.getStringValue());
			}

			final TiffField date = tiffMetadata.findField(TiffTagConstants.TIFF_TAG_DATE_TIME, true);
			if (date != null) {
				return _dtParser.parseDateTime(date.getStringValue());
			}

		} catch (final Exception e) {
			// ignore
		}

		return null;
	}

	private int getTiffValueInt(final TiffImageMetadata tiffMetadata,
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

	private String getTiffValueString(final TiffImageMetadata tiffMetadata, final TagInfo tagInfo) {

		try {
			final TiffField field = tiffMetadata.findField(tagInfo, true);
			if (field != null) {
				return field.getStringValue();
			}
		} catch (final Exception e) {
			// ignore
		}

		return null;
	}

	/**
	 * @param mapProvider
	 * @param projectionId
	 * @param zoomLevel
	 * @return Returns the world position for this photo or <code>null</code> when geo position is
	 *         not set.
	 */
	public Point getWorldPosition(final CommonMapProvider mapProvider, final String projectionId, final int zoomLevel) {

		final double latitude = getLatitude();
		if (latitude == Double.MIN_VALUE) {
			return null;
		}

		final Integer hashKey = projectionId.hashCode() + zoomLevel;

		final Point worldPosition = _worldPosition.get(hashKey);

		if (worldPosition == null) {
			// convert lat/long into world pixels which depends on the map projection

			final GeoPosition photoGeoPosition = new GeoPosition(latitude, getLongitude());

			final Point geoToPixel = mapProvider.geoToPixel(photoGeoPosition, zoomLevel);

			_worldPosition.put(hashKey, geoToPixel);

			return geoToPixel;
		}

		return worldPosition;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_uniqueId == null) ? 0 : _uniqueId.hashCode());
		return result;
	}

	public boolean isLoadingError() {
		return _isLoadingError;
	}

	public void resetTourGeoPosition() {

		_tourLatitude = Double.MIN_VALUE;
		_tourLongitude = Double.MIN_VALUE;

		_photoWrapper.isPhotoWithGps = _photoWrapper.isGeoFromExif;
	}

	public void resetWorldPosition() {
		_worldPosition.clear();
	}

	public void setAltitude(final double altitude) {
		_altitude = altitude;
	}

	public void setDimension(final int width, final int height) {

		_imageWidth = width;
		_imageHeight = height;

		setMapImageSize();
	}

	public void setGpsAreaInfo(final String gpsAreaInfo) {
		_gpsAreaInfo = gpsAreaInfo;
	}

	public void setLoadingState(final PhotoLoadingState photoLoadingState, final ImageQuality imageQuality) {

		if (imageQuality == ImageQuality.HQ) {
			_photoLoadingStateHQ = photoLoadingState;
		} else if (imageQuality == ImageQuality.ORIGINAL) {
			_photoLoadingStateOriginal = photoLoadingState;
		} else {
			_photoLoadingStateThumb = photoLoadingState;
		}

		if (photoLoadingState == PhotoLoadingState.IMAGE_IS_INVALID) {
			_isLoadingError = true;
		}
//
//		System.out
//				.println("set state\t" + imageQuality + "\t" + photoLoadingState + "\t" + _photoWrapper.imageFileName);
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	private void setMapImageSize() {

		final int imageCanvasWidth = 80;
		final int imageCanvasHeight = imageCanvasWidth;

		_mapImageSize = RendererHelper.getBestSize(this, //
				_imageWidth,
				_imageHeight,
				imageCanvasWidth,
				imageCanvasHeight);
	}

	public void setStateExifThumb(final int exifThumbState) {
		_exifThumbImageState = exifThumbState;
	}

	public void setThumbSaveError() {
		PhotoLoadManager.putPhotoInThumbSaveErrorMap(_photoWrapper.imageFilePathName);
	}

	public void setTourGeoPosition(final double latitude, final double longitude) {

		_tourLatitude = latitude;
		_tourLongitude = longitude;

		_photoWrapper.isPhotoWithGps = true;
	}

	@Override
	public String toString() {

//		final String rotateDegree = _orientation == 8 ? "270" //
//				: _orientation == 3 ? "180" //
//						: _orientation == 6 ? "90" : "0";

		return "" //$NON-NLS-1$
//				+"Photo " //
				+ (_photoWrapper.imageFileName)
				+ ("\t_exifDateTime " + new DateTime(_exifDateTime).toString()) //$NON-NLS-1$
//				+ (_exifDateTime == null ? "-no date-" : "\t" + _exifDateTime)
//				+ ("\trotate:" + rotateDegree)
//				+ (_imageWidth == Integer.MIN_VALUE ? "-no size-" : "\t" + _imageWidth + "x" + _imageHeight)
//				+ (_latitude == Double.MIN_VALUE ? "\t-no GPS-" : "\t" + _latitude + " - " + _longitude)
				//
		;
	}

	public void updateImageMetadata(final PhotoImageMetadata photoImageMetadata) {

		_photoImageMetadata = photoImageMetadata;

		_exifDateTime = photoImageMetadata.exifDateTime;
		_imageFileDateTime = photoImageMetadata.fileDateTime;

		_imageWidth = photoImageMetadata.imageWidth;
		_imageHeight = photoImageMetadata.imageHeight;

		_orientation = photoImageMetadata.orientation;

		_imageDirection = photoImageMetadata.imageDirection;
		_altitude = photoImageMetadata.altitude;

		_exifLatitude = photoImageMetadata.latitude;
		_exifLongitude = photoImageMetadata.longitude;

		_gpsAreaInfo = photoImageMetadata.gpsAreaInfo;

		// rotate image, swap with and height
		if (_imageWidth != Integer.MIN_VALUE && _imageHeight != Integer.MIN_VALUE) {

			if (_orientation > 1) {

				// see here http://www.impulseadventure.com/photo/exif-orientation.html

				if (_orientation == 6 || _orientation == 8) {

					// camera is rotated to the left or right by 90 degree

					final int imageWidth = _imageWidth;

					_imageWidth = _imageHeight;
					_imageHeight = imageWidth;
				}
			}
		}

		setMapImageSize();

		_photoWrapper.isExifLoaded = true;

		/*
		 * set state if gps data are available, this state is used for filtering the photos and to
		 * indicate that exif data are loaded
		 */
		final boolean isExifGPS = _exifLatitude != Double.MIN_VALUE;
		final boolean isTourGPS = _tourLatitude != Double.MIN_VALUE;

		_photoWrapper.isGeoFromExif = isExifGPS;
		_photoWrapper.isPhotoWithGps = isTourGPS || isExifGPS;

		// sort by exif date when available
		if (_exifDateTime != null) {

			final long exifUTCMills = _exifDateTime.getMillis();

			_photoWrapper.imageExifTime = exifUTCMills;
//			_photoWrapper.imageUTCZoneOffset = DateTimeZone.getDefault().getOffset(exifUTCMills);
		}
	}
}
