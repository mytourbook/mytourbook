/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import net.tourbook.common.UI;
import net.tourbook.common.map.CommonMapProvider;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Photo {

	private String										_uniqueId;

	public static final int								MAP_IMAGE_DEFAULT_WIDTH_HEIGHT	= 80;

	private static final org.eclipse.swt.graphics.Point	MAP_IMAGE_DEFAULT_SIZE			= new org.eclipse.swt.graphics.Point(
																								MAP_IMAGE_DEFAULT_WIDTH_HEIGHT,
																								MAP_IMAGE_DEFAULT_WIDTH_HEIGHT);

	/**
	 * Photo image file
	 */
	public File											imageFile;

	public String										imagePathName;
	public String										imageFileName;
	public String										imageFileExt;

	/**
	 * File path name is the unique key for a photo.
	 */
	public String										imageFilePathName;

	/**
	 * Last modified in GMT
	 */
	public long											imageFileLastModified;

	/**
	 * Exif time in milliseconds, when not available, the last modified time of the image file is
	 * used.
	 */
	public long											imageExifTime;

	/**
	 * Time in ms (or {@link Long#MIN_VALUE} when not set) when photo was taken + time adjustments,
	 * e.g. wrong time zone, wrong time is set in the camera.
	 */
	public long											adjustedTimeTour				= Long.MIN_VALUE;
	public long											adjustedTimeLink				= Long.MIN_VALUE;

	public long											imageFileSize;

	/**
	 * Camera which is used to take this photo, is <code>null</code> when not yet set.
	 */
	public Camera										camera;

	/**
	 * Is <code>true</code> when photo exif data are loaded.
	 */
	public boolean										isExifLoaded;

	/**
	 * Is <code>true</code> when this photo contains geo coordinates.
	 */
	public boolean										isLinkPhotoWithGps;
	public boolean										isTourPhotoWithGps;

	/**
	 * Is <code>true</code> when geo coordinates origin is in the photo EXIF data.
	 */
	public boolean										isGeoFromExif;

	/**
	 * Is <code>true</code> when a photo is saved in a tour.
	 * <p>
	 * This allows to set rating stars which requires that they can be saved in a tour.
	 */
	public boolean										isSavedInTour;

	/**
	 * Key is tourId
	 */
	private final HashMap<Long, TourPhotoReference>		_tourPhotoRef					= new HashMap<Long, TourPhotoReference>();

	/**
	 * When sql loading state is {@link PhotoSqlLoadingState#NOT_LOADED}, the photo is created from
	 * the file system and {@link #_tourPhotoRef} needs to be retrieved from the sql db.
	 */
	private AtomicReference<PhotoSqlLoadingState>		_photoSqlLoadingState			= new AtomicReference<PhotoSqlLoadingState>(
																								PhotoSqlLoadingState.NOT_LOADED);
	/**
	 * Rating stars are very complicated when a photo is saved in multiple tours. Currently
	 * (8.1.2013) ratings stars can be set only for ALL tours.
	 */
	public int											ratingStars;

	private PhotoImageMetadata							_photoImageMetadata;

	/**
	 * Last modified in GMT
	 */
	private DateTime									_imageFileDateTime;
	private DateTime									_exifDateTime;

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
	private int											_orientation					= 1;

	private int											_photoImageWidth				= Integer.MIN_VALUE;
	private int											_photoImageHeight				= Integer.MIN_VALUE;

	private int											_thumbImageWidth				= Integer.MIN_VALUE;
	private int											_thumbImageHeight				= Integer.MIN_VALUE;

	/**
	 * When <code>true</code>, EXIF geo is returned when available, otherwise tour geo is returned
	 * when available. When requested geo is not available, the other is returned.
	 */
//	private static boolean								_isGetExifGeo					= false;

	/**
	 * 
	 */
	private static IPhotoServiceProvider				_photoServiceProvider;

	/**
	 * Double.MIN_VALUE cannot be used, it cannot be saved in the database. 0 is the value when the
	 * value is not set !!!
	 */
	private double										_exifLatitude					= 0;
	private double										_exifLongitude					= 0;
	private double										_tourLatitude					= 0;
	private double										_tourLongitude					= 0;
	private double										_linkLatitude					= 0;
	private double										_linkLongitude					= 0;

	private String										_gpsAreaInfo;

	private double										_imageDirection					= Double.MIN_VALUE;
	private double										_altitude						= Double.MIN_VALUE;

	private static final DateTimeFormatter				_dtParser						= DateTimeFormat//
																								.forPattern("yyyy:MM:dd HH:mm:ss") //$NON-NLS-1$
//																			.withZone(DateTimeZone.UTC)
																						;
//	private static final DateTimeFormatter	_dtFormatter			= DateTimeFormat.forStyle("SL");	//$NON-NLS-1$

	/**
	 * caches the world positions for the photo lat/long values for each zoom level
	 * <p>
	 * key: projection id + zoom level
	 */
	private final HashMap<Integer, Point>				_tourWorldPosition				= new HashMap<Integer, Point>();
	private final HashMap<Integer, Point>				_linkWorldPosition				= new HashMap<Integer, Point>();

	/**
	 * Contains image keys for each image quality which can be used to get images from an image
	 * cache
	 */
	private String										_imageKeyThumb;
	private String										_imageKeyHQ;
	private String										_imageKeyOriginal;

	/**
	 * This array keeps track of the loading state for the photo images and for different qualities
	 */
	private PhotoLoadingState							_photoLoadingStateThumb;
	private PhotoLoadingState							_photoLoadingStateHQ;
	private PhotoLoadingState							_photoLoadingStateOriginal;

	/**
	 * Is <code>true</code> when loading the image causes an error.
	 */
	private boolean										_isLoadingError;

	/**
	 * Is <code>true</code> when the image file is available in the file system.
	 */
	private boolean										_isImageFileAvailable;

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
	private int											_exifThumbImageState			= -1;

	/**
	 * Image size which is painted in the map
	 */
	private org.eclipse.swt.graphics.Point				_mapImageSize					= MAP_IMAGE_DEFAULT_SIZE;

	/**
	 * This is the image size which the user has selected to paint a photo image.
	 */
	private static int									PAINTED_MAP_IMAGE_WIDTH			= MAP_IMAGE_DEFAULT_WIDTH_HEIGHT;

	private int											_paintedMapImageWidth;

	/**
	 */
	public Photo(final File photoImageFile) {

		setupPhoto(photoImageFile, new Path(photoImageFile.getPath()));
	}

	public Photo(final String imageFilePathName) {

		this(new File(imageFilePathName));
	}

	public static String getImageKeyHQ(final String imageFilePathName) {
		return Util.computeMD5(imageFilePathName + "_HQ");//$NON-NLS-1$
	}

	public static String getImageKeyThumb(final String imageFilePathName) {
		return Util.computeMD5(imageFilePathName + "_Thumb");//$NON-NLS-1$
	}

	public static IPhotoServiceProvider getPhotoServiceProvider() {

		final IPhotoServiceProvider photoServiceProvider = _photoServiceProvider;

		return photoServiceProvider;
	}

	public static void setPaintedMapImageWidth(final int paintedMapImageWidth) {
		PAINTED_MAP_IMAGE_WIDTH = paintedMapImageWidth;
	}

	public static void setPhotoServiceProvider(final IPhotoServiceProvider photoServiceProvider) {
		_photoServiceProvider = photoServiceProvider;
	}

	public void addTour(final Long tourId, final long photoId) {

		if (_tourPhotoRef.containsKey(tourId) == false) {

			_tourPhotoRef.put(tourId, new TourPhotoReference(tourId, photoId));
		}
	}

	/**
	 * Creates metadata from image metadata
	 * 
	 * @param imageMetadata
	 *            Can be <code>null</code> when not available
	 * @return
	 */
	private PhotoImageMetadata createPhotoMetadata(final IImageMetadata imageMetadata) {

		final PhotoImageMetadata photoMetadata = new PhotoImageMetadata();

		/*
		 * read meta data for this photo
		 */
		if (imageMetadata instanceof TiffImageMetadata) {

			photoMetadata.isExifFromImage = true;

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

			photoMetadata.isExifFromImage = true;

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
//		photoMetadata.fileDateTime = new DateTime(DateTimeZone.UTC).withMillis(imageFileLastModified);
		photoMetadata.fileDateTime = new DateTime(imageFileLastModified);

//// this will log all available meta data
//		System.out.println(UI.timeStampNano());
//		System.out.println(UI.timeStampNano() + " " + imageFileName);
//		System.out.println(UI.timeStampNano());
//		System.out.println(imageMetadata.toString());
//		System.out.println(UI.timeStampNano());
//		System.out.println(photoMetadata);
//		System.out.println(UI.timeStampNano());
//		// TODO remove SYSTEM.OUT.PRINTLN

		return photoMetadata;
	}

	public String dumpLoadingState() {

		final StringBuilder sb = new StringBuilder();

		sb.append("Thumb:" + _photoLoadingStateThumb); //$NON-NLS-1$
		sb.append("\tHQ:" + _photoLoadingStateHQ); //$NON-NLS-1$
		sb.append("\tOriginal:" + _photoLoadingStateOriginal); //$NON-NLS-1$

		return sb.toString();
	}

	public void dumpTourReferences() {

		for (final TourPhotoReference ref : _tourPhotoRef.values()) {
			System.out.println(UI.timeStampNano() + " \t\tphotoId=" + ref.photoId); //$NON-NLS-1$
			// TODO remove SYSTEM.OUT.PRINTLN
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

	/**
	 * @return Returns photo image height which is available, in this sequence:
	 *         <p>
	 *         <li>Original image height<br>
	 *         <li>Thumb image height<br>
	 *         <li>{@link Integer#MIN_VALUE} when image height is not yet set
	 */
	public int getAvailableImageHeight() {

		if (_photoImageHeight != Integer.MIN_VALUE) {
			return _photoImageHeight;
		}

		if (_thumbImageHeight != Integer.MIN_VALUE) {
			return _thumbImageHeight;
		}

		return Integer.MIN_VALUE;
	}

	/**
	 * @return Returns photo image width which is available, in this sequence:
	 *         <p>
	 *         <li>Original image width<br>
	 *         <li>Thumb image width<br>
	 *         <li>{@link Integer#MIN_VALUE} when image width is not yet set
	 */
	public int getAvailableImageWidth() {

		if (_photoImageWidth != Integer.MIN_VALUE) {
			return _photoImageWidth;
		}

		if (_thumbImageWidth != Integer.MIN_VALUE) {
			return _thumbImageWidth;
		}

		return Integer.MIN_VALUE;
	}

	/**
	 * @return Returns image size as visible text which is displayed in the UI.
	 */
	public String getDimensionText() {

		final StringBuilder sbDimenstion = new StringBuilder();

		if (isImageSizeAvailable()) {

			final boolean isThumbSize = isThumbImageSize();

			if (isThumbSize) {
				sbDimenstion.append(UI.SYMBOL_BRACKET_LEFT);
			}

			sbDimenstion.append(getAvailableImageWidth());
			sbDimenstion.append(UI.DIMENSION);
			sbDimenstion.append(getAvailableImageHeight());

			if (isThumbSize) {
				sbDimenstion.append(UI.SYMBOL_BRACKET_RIGHT);
			}
		}

		return sbDimenstion.toString();
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

		if (PhotoLoadManager.isImageLoadingError(imageFilePathName)) {
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

			imageFileMetadata = Imaging.getMetadata(imageFile, params);

//			System.out.println(UI.timeStamp()
//					+ Thread.currentThread().getName()
//					+ "read exif\t"
//					+ ((System.currentTimeMillis() - start) + " ms")
//					+ ("\tWithThumb: " + isReadThumbnail)
//					+ ("\t" + imageFilePathName)
//			//
//					);
//			// TODO remove SYSTEM.OUT.PRINTLN
//
//			System.out.println(imageFileMetadata);
//			// TODO remove SYSTEM.OUT.PRINTLN

		} catch (final Exception e) {

			StatusUtil.log(NLS.bind(//
					"Could not read metadata from image \"{0}\"", //$NON-NLS-1$
					imageFile));

			PhotoLoadManager.putPhotoInLoadingErrorMap(imageFilePathName);

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

	public double getLinkLatitude() {

		return _linkLatitude != 0 //
				? _linkLatitude
				: _exifLatitude;
	}

	public double getLinkLongitude() {
		return _linkLongitude != 0 //
				? _linkLongitude
				: _exifLongitude;
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
	 * @return Returns size when image is painted on the map or <code>null</code>, when not yet set.
	 */
	public org.eclipse.swt.graphics.Point getMapImageSize() {

		if (PAINTED_MAP_IMAGE_WIDTH != _paintedMapImageWidth) {

			setMapImageSize();

			_paintedMapImageWidth = PAINTED_MAP_IMAGE_WIDTH;
		}

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

	public int getPhotoImageHeight() {
		return _photoImageHeight;
	}

	/**
	 * @return Returns photo image width or {@link Integer#MIN_VALUE} when width is not set.
	 */
	public int getPhotoImageWidth() {
		return _photoImageWidth;
	}

	public AtomicReference<PhotoSqlLoadingState> getSqlLoadingState() {
		return _photoSqlLoadingState;
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
	 * @return Returns latitude.
	 *         <p>
	 *         <b> Double.MIN_VALUE cannot be used, it cannot be saved in the database.
	 *         <p>
	 *         Returns 0 when the value is not set !!! </b>
	 */
	public double getTourLatitude() {
		return _tourLatitude != 0 //
				? _tourLatitude
				: _exifLatitude;
	}

	public double getTourLongitude() {
		return _tourLongitude != 0 //
				? _tourLongitude
				: _exifLongitude;
	}

	public HashMap<Long, TourPhotoReference> getTourPhotoReferences() {
		return _tourPhotoRef;
	}

	public String getUniqueId() {
		return imageFilePathName;
	}

	/**
	 * @param mapProvider
	 * @param projectionId
	 * @param zoomLevel
	 * @param isLinkPhotoDisplayed
	 * @return Returns the world position for this photo or <code>null</code> when geo position is
	 *         not set.
	 */
	public Point getWorldPosition(	final CommonMapProvider mapProvider,
									final String projectionId,
									final int zoomLevel,
									final boolean isLinkPhotoDisplayed) {

		final double latitude = isLinkPhotoDisplayed //
				? getLinkLatitude()
				: getTourLatitude();

		if (latitude == 0) {
			return null;
		}

		final Integer hashKey = projectionId.hashCode() + zoomLevel;

		final Point worldPosition = isLinkPhotoDisplayed //
				? _linkWorldPosition.get(hashKey)
				: _tourWorldPosition.get(hashKey);

		if (worldPosition == null) {
			// convert lat/long into world pixels which depends on the map projection

			final GeoPosition photoGeoPosition = new GeoPosition(latitude, isLinkPhotoDisplayed
					? getLinkLongitude()
					: getTourLongitude());

			final Point geoToPixel = mapProvider.geoToPixel(photoGeoPosition, zoomLevel);

			if (isLinkPhotoDisplayed) {
				_linkWorldPosition.put(hashKey, geoToPixel);
			} else {
				_tourWorldPosition.put(hashKey, geoToPixel);
			}

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

	public boolean isImageFileAvailable() {
		return _isImageFileAvailable;
	}

	/**
	 * @return Return <code>true</code> when image size (thumb or original) is available.
	 */
	public boolean isImageSizeAvailable() {

		if (_photoImageWidth != Integer.MIN_VALUE || _thumbImageWidth != Integer.MIN_VALUE) {
			return true;
		}

		return false;
	}

	public boolean isLoadingError() {
		return _isLoadingError;
	}

	/**
	 * @return Returns <code>true</code> when the original image size is not available but the thumb
	 *         image size.
	 */
	private boolean isThumbImageSize() {

		if (_photoImageWidth != Integer.MIN_VALUE) {
			return false;
		}

		return _thumbImageWidth != Integer.MIN_VALUE;
	}

	public void removeTour(final Long tourId) {
		_tourPhotoRef.remove(tourId);
	}

	public void replaceImageFile(final IPath newImageFilePathName) {

		// force loading of metadata
		_photoImageMetadata = null;

		setupPhoto(newImageFilePathName.toFile(), newImageFilePathName);

//		PhotoLoadManager.putPhotoInLoadingErrorMap(imageFilePathName);
	}

	public void resetLinkGeoPositions() {

		_linkLatitude = 0;
		_linkLongitude = 0;

		isLinkPhotoWithGps = isGeoFromExif;
	}

	public void resetLinkWorldPosition() {
		_linkWorldPosition.clear();
	}

	public void resetTourExifState() {

		// photo is not saved any more in a tour

		_tourLatitude = 0;
		_tourLongitude = 0;

		isTourPhotoWithGps = isGeoFromExif;
	}

	public void setAltitude(final double altitude) {
		_altitude = altitude;
	}

	public void setGpsAreaInfo(final String gpsAreaInfo) {
		_gpsAreaInfo = gpsAreaInfo;
	}

	public void setLinkGeoPosition(final double linkLatitude, final double linkLongitude) {

		_linkLatitude = linkLatitude;
		_linkLongitude = linkLongitude;

		isLinkPhotoWithGps = true;
	}

	public void setLoadingState(final PhotoLoadingState photoLoadingState, final ImageQuality imageQuality) {

		if (imageQuality == ImageQuality.HQ) {
			_photoLoadingStateHQ = photoLoadingState;
		} else if (imageQuality == ImageQuality.ORIGINAL) {
			_photoLoadingStateOriginal = photoLoadingState;
		} else {
			_photoLoadingStateThumb = photoLoadingState;
		}

		// set overall loading error
		if (photoLoadingState == PhotoLoadingState.IMAGE_IS_INVALID) {
			_isLoadingError = true;
		}
//
//		System.out
//				.println("set state\t" + imageQuality + "\t" + photoLoadingState + "\t" + imageFileName);
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	private void setMapImageSize() {

		final int imageCanvasWidth = PAINTED_MAP_IMAGE_WIDTH;
		final int imageCanvasHeight = imageCanvasWidth;

		final int imageWidth = _photoImageWidth != Integer.MIN_VALUE ? _photoImageWidth : _thumbImageWidth;
		final int imageHeight = _photoImageHeight != Integer.MIN_VALUE ? _photoImageHeight : _thumbImageHeight;

		_mapImageSize = RendererHelper.getBestSize(this, //
				imageWidth,
				imageHeight,
				imageCanvasWidth,
				imageCanvasHeight);
	}

	public void setPhotoDimension(final int width, final int height) {

		_photoImageWidth = width;
		_photoImageHeight = height;

		setMapImageSize();
	}

	public void setStateExifThumb(final int exifThumbState) {
		_exifThumbImageState = exifThumbState;
	}

	public void setThumbDimension(final int width, final int height) {

		_thumbImageWidth = width;
		_thumbImageHeight = height;

		setMapImageSize();
	}

	public void setThumbSaveError() {
		PhotoLoadManager.putPhotoInThumbSaveErrorMap(imageFilePathName);
	}

	public void setTourGeoPosition(final double latitude, final double longitude) {

		_tourLatitude = latitude;
		_tourLongitude = longitude;

		isTourPhotoWithGps = true;
	}

	private void setupPhoto(final File photoImageFile, final IPath photoImagePath) {

		final String photoImageFilePathName = photoImageFile.getAbsolutePath();
		final long lastModified = photoImageFile.lastModified();

		imageFile = photoImageFile;

		imageFileName = photoImageFile.getName();
		imageFilePathName = photoImageFilePathName;

		imagePathName = photoImagePath.removeLastSegments(1).toOSString();
		imageFileExt = photoImagePath.getFileExtension();

		imageFileSize = photoImageFile.length();
		imageFileLastModified = lastModified;

		// initially sort by file date until exif data are loaded
		imageExifTime = lastModified;

		_uniqueId = photoImageFilePathName;

		/*
		 * initialize image keys and loading states
		 */
		_imageKeyThumb = getImageKeyThumb(photoImageFilePathName);
		_imageKeyHQ = getImageKeyHQ(photoImageFilePathName);
		_imageKeyOriginal = Util.computeMD5(photoImageFilePathName + "_Original");//$NON-NLS-1$

		_isImageFileAvailable = photoImageFile.exists();

		if (_isImageFileAvailable) {

			_photoLoadingStateThumb = PhotoLoadingState.UNDEFINED;
			_photoLoadingStateHQ = PhotoLoadingState.UNDEFINED;
			_photoLoadingStateOriginal = PhotoLoadingState.UNDEFINED;

			_isLoadingError = false;

		} else {

			_photoLoadingStateThumb = PhotoLoadingState.IMAGE_IS_INVALID;
			_photoLoadingStateHQ = PhotoLoadingState.IMAGE_IS_INVALID;
			_photoLoadingStateOriginal = PhotoLoadingState.IMAGE_IS_INVALID;

			_isLoadingError = true;
		}
	}

	@Override
	public String toString() {

//		final String rotateDegree = _orientation == 8 ? "270" //
//				: _orientation == 3 ? "180" //
//						: _orientation == 6 ? "90" : "0";

		return "" //$NON-NLS-1$
//				+"Photo " //
				+ (imageFileName)
				+ ("\t_exifDateTime " + new DateTime(_exifDateTime).toString()) //$NON-NLS-1$
//				+ (_exifDateTime == null ? "-no date-" : "\t" + _exifDateTime)
//				+ ("\trotate:" + rotateDegree)
//				+ (_imageWidth == Integer.MIN_VALUE ? "-no size-" : "\t" + _imageWidth + "x" + _imageHeight)
				+ ("\tEXIF GPS: " + _exifLatitude + " - " + _exifLongitude) //$NON-NLS-1$ //$NON-NLS-2$
				+ ("\tLink GPS: " + _linkLatitude + " - " + _linkLongitude) //$NON-NLS-1$ //$NON-NLS-2$
				+ ("\tTour GPS: " + _tourLatitude + " - " + _tourLongitude) //$NON-NLS-1$ //$NON-NLS-2$
		//
		;
	}

	public void updateImageMetadata(final PhotoImageMetadata photoImageMetadata) {

		_photoImageMetadata = photoImageMetadata;

		_imageFileDateTime = photoImageMetadata.fileDateTime;

		if (photoImageMetadata.isExifFromImage) {

			/*
			 * Set these data only when they are contained in the image file, this ensures that e.g.
			 * width/height is not overwritten with default values.
			 */

			_exifDateTime = photoImageMetadata.exifDateTime;

			_photoImageWidth = photoImageMetadata.imageWidth;
			_photoImageHeight = photoImageMetadata.imageHeight;

			_orientation = photoImageMetadata.orientation;

			_imageDirection = photoImageMetadata.imageDirection;
			_altitude = photoImageMetadata.altitude;

			_exifLatitude = photoImageMetadata.latitude;
			_exifLongitude = photoImageMetadata.longitude;

			_gpsAreaInfo = photoImageMetadata.gpsAreaInfo;
		}

		// rotate image, swap with and height
		if (_photoImageWidth != Integer.MIN_VALUE && _photoImageHeight != Integer.MIN_VALUE) {

			if (_orientation > 1) {

				// see here http://www.impulseadventure.com/photo/exif-orientation.html

				if (_orientation == 6 || _orientation == 8) {

					// camera is rotated to the left or right by 90 degree

					final int imageWidth = _photoImageWidth;

					_photoImageWidth = _photoImageHeight;
					_photoImageHeight = imageWidth;
				}
			}
		}

		setMapImageSize();

		isExifLoaded = true;

		/*
		 * set state if gps data are available, this state is used for filtering the photos and to
		 * indicate that exif data are loaded
		 */
		final boolean isExifGPS = _exifLatitude != 0;
		final boolean isTourGPS = _tourLatitude != 0;

		isGeoFromExif = isExifGPS;
		isTourPhotoWithGps = isTourGPS || isExifGPS;

		// sort by exif date when available
		if (_exifDateTime != null) {

			final long exifUTCMills = _exifDateTime.getMillis();

			imageExifTime = exifUTCMills;
//			imageUTCZoneOffset = DateTimeZone.getDefault().getOffset(exifUTCMills);
		}
	}
}
