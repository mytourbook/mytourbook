/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.io.File;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import net.tourbook.common.UI;
import net.tourbook.common.map.CommonMapProvider;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingConstants;
import org.apache.commons.imaging.common.GenericImageMetadata.GenericImageMetadataItem;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.ImageMetadata.ImageMetadataItem;
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
import org.eclipse.swt.graphics.Rectangle;

import pixelitor.filters.curves.ToneCurvesFilter;

public class Photo implements Serializable {

   private static final long                           serialVersionUID               = 1L;

   public static final int                             MAP_IMAGE_DEFAULT_WIDTH_HEIGHT = 80;

   private static final org.eclipse.swt.graphics.Point MAP_IMAGE_DEFAULT_SIZE         = new org.eclipse.swt.graphics.Point(
         MAP_IMAGE_DEFAULT_WIDTH_HEIGHT,
         MAP_IMAGE_DEFAULT_WIDTH_HEIGHT);

   /**
    *
    */
   private static IPhotoServiceProvider                _photoServiceProvider;

   private static DateTimeFormatter                    _dtParser;

   static {

      setupTimeZone();
   }

   /**
    * This is the image size which the user has selected to paint a photo image.
    */
   private static int                              _map2ImageRequestedSize  = MAP_IMAGE_DEFAULT_WIDTH_HEIGHT;
   private static int                              _map25ImageRequestedSize = MAP_IMAGE_DEFAULT_WIDTH_HEIGHT;

   private int                                     _map25ImageRequestedAndCheckedSize;

   /**
    * Image size which is painted in the map
    */
   private org.eclipse.swt.graphics.Point          _map25ImageRenderSize    = MAP_IMAGE_DEFAULT_SIZE;

   private String                                  _uniqueId;

   /**
    * Photo image file
    */
   public File                                     imageFile;

   public String                                   imagePathName;
   public String                                   imageFileName;
   public String                                   imageFileExt;

   /**
    * File path name is the unique key for a photo.
    */
   public String                                   imageFilePathName;
   /**
    * Last modified date/time of the image file which is provided by the file system with the
    * system time zone.
    */
   private LocalDateTime                           _imageFileLastModified;

   /**
    * Exif date/time, it has no time zone but UTC with 0 time offset is used
    */
   private LocalDateTime                           _exifDateTime;

   /**
    * Exif time in milliseconds, when not available, the last modified time of the image file is
    * used.
    */
   public long                                     imageExifTime;

   /**
    * Time in ms (or {@link Long#MIN_VALUE} when not set) when photo was taken + time adjustments,
    * e.g. wrong time zone, wrong time is set in the camera. This time is saved in the tour photo.
    */
   public long                                     adjustedTime_Tour        = Long.MIN_VALUE;

   public ZonedDateTime                            adjustedTime_Tour_WithZone;

   /**
    * Time in ms which is set in the link view with the adjusted camera time
    */
   public long                                     adjustedTime_Camera      = Long.MIN_VALUE;

   public long                                     imageFileSize;

   /**
    * Camera which is used to take this photo, is <code>null</code> when not yet set.
    */
   public Camera                                   camera;

   /**
    * Is <code>true</code> when photo exif data are loaded.
    */
   public boolean                                  isExifLoaded;

   /**
    * Is <code>true</code> when this photo contains geo coordinates.
    */
   public boolean                                  isLinkPhotoWithGps;
   public boolean                                  isTourPhotoWithGps;

   /**
    * Is <code>true</code> when geo coordinates origin is in the photo EXIF data.
    */
   public boolean                                  isGeoFromExif;

   /**
    * Is <code>true</code> when a photo is saved in a tour.
    * <p>
    * This allows to set rating stars which requires that they can be saved in a tour.
    */
   public boolean                                  isSavedInTour;

   /**
    * A photo can be linked with different tours, key is tourId
    */
   private final HashMap<Long, TourPhotoReference> _tourPhotoRef            = new HashMap<>();

   /**
    * When sql loading state is {@link PhotoSqlLoadingState#NOT_LOADED}, the photo is created from
    * the file system and {@link #_tourPhotoRef} needs to be retrieved from the sql db.
    */
   private AtomicReference<PhotoSqlLoadingState>   _photoSqlLoadingState    = new AtomicReference<>(PhotoSqlLoadingState.NOT_LOADED);

   /**
    * Rating stars are very complicated when a photo is saved in multiple tours. Currently
    * (8.1.2013) ratings stars can be set only for ALL tours.
    */
   public int                                      ratingStars;

   /**
    * Is <code>true</code> when small rating stars are painted in the map. Small rating stars cannot
    * be modified.
    */
   public boolean                                  isSmallRatingStars;

   /**
    * Rectangle in device coordinates where the photo is painted
    */
   public Rectangle                                paintedPhoto;

   /**
    * Rectangle for the painted rating stars
    */
   public Rectangle                                paintedRatingStars;

   /**
    * Number of hovered rating stars
    */
   public int                                      hoveredStars;

   private PhotoImageMetadata                      _photoImageMetadata;

   /**
    * <pre>
    * Orientation
    *
    * The image orientation viewed in terms of rows and columns.
    * Type      =      SHORT
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
   private int                                     _orientation             = 1;

   /**
    * When <code>true</code>, EXIF geo is returned when available, otherwise tour geo is returned
    * when available. When requested geo is not available, the other is returned.
    */
//   private static boolean                        _isGetExifGeo               = false;

   /**
    * Photo image width, it is {@link Integer#MIN_VALUE} when not yet set
    */
   private int                           _photoImageWidth     = Integer.MIN_VALUE;
   private int                           _photoImageHeight    = Integer.MIN_VALUE;

   private int                           _thumbImageWidth     = Integer.MIN_VALUE;
   private int                           _thumbImageHeight    = Integer.MIN_VALUE;

   /**
    * Double.MIN_VALUE cannot be used, it cannot be saved in the database. 0 is the value when the
    * value is not set !!!
    */
   private double                        _exifLatitude;
   private double                        _exifLongitude;

   private double                        _tourLatitude;
   private double                        _tourLongitude;

   private double                        _linkLatitude;
   private double                        _linkLongitude;

   private String                        _gpsAreaInfo;

   private double                        _imageDirection      = Double.MIN_VALUE;

   private double                        _altitude            = Double.MIN_VALUE;

   /**
    * Caches the world positions for the photo lat/long values for each zoom level
    * <p>
    * key: projection id + zoom level
    */
   private final HashMap<Integer, Point> _tourWorldPosition   = new HashMap<>();
   private final HashMap<Integer, Point> _linkWorldPosition   = new HashMap<>();

   /**
    * Contains image keys for each image quality which can be used to get images from an image
    * cache
    */
   private String                        _imageKey_Thumb;
   private String                        _imageKey_ThumbHQ;
   private String                        _imageKey_ThumbHQ_Adjusted;
   private String                        _imageKey_HQ;
   private String                        _imageKey_Original;

   /**
    * This array keeps track of the loading state for the photo images and for different qualities
    */
   private PhotoLoadingState             _photoLoadingStateThumb;
   private PhotoLoadingState             _photoLoadingStateThumbHQ;
   private PhotoLoadingState             _photoLoadingStateHQ;
   private PhotoLoadingState             _photoLoadingStateOriginal;

   /**
    * Is <code>true</code> when loading the image causes an error.
    */
   private boolean                       _isLoadingError;

   /**
    * Is <code>true</code> when the image file is available in the file system.
    */
   private boolean                       _isImageFileAvailable;

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
   private int                           _exifThumbImageState = -1;

   /**
    * Temporarily tour id from a {@link TourPhotoLink}
    */
   private long                          _photoLinkTourId;

   /**
    * When <code>true</code> then the photo adjustments are computed. This is the MAIN flag to
    * recomputed photo adjustments.
    */
   public boolean                        isAdjustmentModified;

   public boolean                        isCropped;

   /** Relative position for the top left x position of the cropping area */
   public float                          cropAreaX1;
   public float                          cropAreaY1;
   public float                          cropAreaX2;
   public float                          cropAreaY2;

   public boolean                        isSetTonality;
   private ToneCurvesFilter              _toneCurvesFilter;

   /**
    */
   public Photo(final File photoImageFile) {

      setupPhoto(photoImageFile, new Path(photoImageFile.getPath()));
   }

   public static String getImageKey_HQ(final String imageFilePathName) {

      return Util.computeMD5(imageFilePathName) + "_KeyHQ";//$NON-NLS-1$
   }

   public static String getImageKey_Thumb(final String imageFilePathName) {

      return Util.computeMD5(imageFilePathName) + "_KeyThumb";//$NON-NLS-1$
   }

   public static String getImageKey_ThumbHQ(final String imageFilePathName) {

      return Util.computeMD5(imageFilePathName) + "_KeyThumbHQ";//$NON-NLS-1$
   }

   public static String getImageKey_ThumbHQ_Adjusted(final String imageFilePathName) {

      return Util.computeMD5(imageFilePathName) + "_KeyThumbHQ_Adjusted";//$NON-NLS-1$
   }

   public static int getMap25ImageRequestedSize() {

      return _map25ImageRequestedSize;
   }

   public static int getMap2ImageRequestedSize() {

      return _map2ImageRequestedSize;
   }

   public static IPhotoServiceProvider getPhotoServiceProvider() {

      final IPhotoServiceProvider photoServiceProvider = _photoServiceProvider;

      return photoServiceProvider;
   }

   public static void setMap25ImageRequestedSize(final int mapImageSize) {

      _map25ImageRequestedSize = mapImageSize;
   }

   public static void setMap2ImageRequestedSize(final int mapImageSize) {

      _map2ImageRequestedSize = mapImageSize;
   }

   public static void setPhotoServiceProvider(final IPhotoServiceProvider photoServiceProvider) {

      _photoServiceProvider = photoServiceProvider;
   }

   static void setupTimeZone() {

      _dtParser = DateTimeFormatter
            .ofPattern("yyyy:MM:dd HH:mm:ss") //$NON-NLS-1$
            .withZone(TimeTools.getDefaultTimeZone());
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
    *           Can be <code>null</code> when not available
    *
    * @return
    */
   private PhotoImageMetadata createPhotoMetadata(final ImageMetadata imageMetadata) {

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

            final List<? extends ImageMetadataItem> pshopItems = pshopMetadata.getItems();

            for (final ImageMetadataItem pshopItem : pshopItems) {

               if (pshopItem instanceof GenericImageMetadataItem) {

                  final GenericImageMetadataItem item = (GenericImageMetadataItem) pshopItem;
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
      photoMetadata.fileDateTime = _imageFileLastModified;

//// this will log all available meta data
//      System.out.println(UI.timeStampNano());
//      System.out.println(UI.timeStampNano() + " " + imageFileName);
//      System.out.println(UI.timeStampNano());
//      System.out.println(imageMetadata.toString());
//      System.out.println(UI.timeStampNano());
//      System.out.println(photoMetadata);
//      System.out.println(UI.timeStampNano());
//      // TODO remove SYSTEM.OUT.PRINTLN

      return photoMetadata;
   }

   public String dumpLoadingState() {

      final StringBuilder sb = new StringBuilder();

      sb.append("Thumb: " + _photoLoadingStateThumb); //$NON-NLS-1$
      sb.append("  ThumbHQ: " + _photoLoadingStateThumbHQ); //$NON-NLS-1$
      sb.append("  HQ: " + _photoLoadingStateHQ); //$NON-NLS-1$
      sb.append("  Original: " + _photoLoadingStateOriginal); //$NON-NLS-1$

      return sb.toString();
   }

   void dumpTourReferences() {

      for (final TourPhotoReference ref : _tourPhotoRef.values()) {

         System.out.println(UI.timeStampNano() + "   photoId=" + ref.photoId); //$NON-NLS-1$
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

   public LocalDateTime getExifDateTime() {
      return _exifDateTime;
   }

   /**
    * @return Returns EXIF thumb image stage
    *
    *         <pre>
    * 1  exif thumb image is available
    * 0  exif thumb image is not available
    * -1 exif thumb has not yet been loaded
    *         </pre>
    */
   public int getExifThumbImageState() {
      return _exifThumbImageState;
   }

   /**
    * Date/Time
    *
    * @param jpegMetadata
    * @param file
    *
    * @return
    */
   private LocalDateTime getExifValueDate(final JpegImageMetadata jpegMetadata) {

//      /*
//       * !!! time is not correct, maybe it is the time when the GPS signal was
//       * received !!!
//       */
//      printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_TIME_STAMP);

      try {

         final TiffField exifDate = jpegMetadata.findEXIFValueWithExactMatch(//
               ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);

         if (exifDate != null) {
            return LocalDateTime.parse(exifDate.getStringValue(), _dtParser);
         }

         final TiffField tiffDate = jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_DATE_TIME);

         if (tiffDate != null) {
            return LocalDateTime.parse(tiffDate.getStringValue(), _dtParser);
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
                * Character Code   Code Designation (8 Bytes)                   References
                * ASCII           41.H, 53.H, 43.H, 49.H, 49.H, 00.H, 00.H, 00.H  ITU-T T.50 IA5
                * JIS            A.H, 49.H, 53.H, 00.H, 00.H, 00.H, 00.H, 00.H   JIS X208-1990
                * Unicode         55.H, 4E.H, 49.H, 43.H, 4F.H, 44.H, 45.H, 00.H  Unicode Standard
                * Undefined      00.H, 00.H, 00.H, 00.H, 00.H, 00.H, 00.H, 00.H  Undefined
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

   public LocalDateTime getImageFileDateTime() {
      return _imageFileLastModified;
   }

   /**
    * @return Returns an image key which can be used to get images from an image cache. This key is
    *         a MD5 hash from the full image file path and the image quality.
    */
   public String getImageKey(final ImageQuality imageQuality) {

      if (imageQuality == ImageQuality.HQ) {

         return _imageKey_HQ;

      } else if (imageQuality == ImageQuality.THUMB_HQ) {

         return _imageKey_ThumbHQ;

      } else if (imageQuality == ImageQuality.THUMB_HQ_ADJUSTED) {

         return _imageKey_ThumbHQ_Adjusted;

      } else if (imageQuality == ImageQuality.ORIGINAL) {

         return _imageKey_Original;

      } else {

         return _imageKey_Thumb;
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
    *
    * @return Returns image metadata <b>with</b> image thumbnail <b>only</b> when
    *         <code>isReadThumbnail</code> is <code>true</code>, otherwise it checks if metadata
    *         are already loaded.
    */
   public ImageMetadata getImageMetaData(final Boolean isReadThumbnail) {

      if (_photoImageMetadata != null && isReadThumbnail == false) {

         // meta data are available but the exif thumnail is not requested

         return null;
      }

      if (PhotoLoadManager.isImageLoadingError(imageFilePathName)) {
         // image could not be loaded previously
         return null;
      }

      ImageMetadata imageFileMetadata = null;

      try {

         /*
          * read metadata WITH thumbnail image info, this is the default when the parameter is
          * omitted
          */
         final HashMap<String, Object> params = new HashMap<>();
         params.put(ImagingConstants.PARAM_KEY_READ_THUMBNAILS, isReadThumbnail);

//         final long start = System.currentTimeMillis();

         imageFileMetadata = Imaging.getMetadata(imageFile, params);

//         System.out.println(UI.timeStamp()
//               + Thread.currentThread().getName()
//               + "read exif\t"
//               + ((System.currentTimeMillis() - start) + " ms")
//               + ("\tWithThumb: " + isReadThumbnail)
//               + ("\t" + imageFilePathName)
//         //
//               );
//         // TODO remove SYSTEM.OUT.PRINTLN
//
//         System.out.println(imageFileMetadata);
//         // TODO remove SYSTEM.OUT.PRINTLN

      } catch (final Exception e) {

         StatusUtil.logError(NLS.bind(
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

      return _linkLatitude != 0
            ? _linkLatitude
            : _exifLatitude;
   }

   public double getLinkLongitude() {

      return _linkLongitude != 0
            ? _linkLongitude
            : _exifLongitude;
   }

   public long getLinkTourId() {
      return _photoLinkTourId;
   }

   /**
    * @return Returns the loading state for the given photo quality
    */
   public PhotoLoadingState getLoadingState(final ImageQuality imageQuality) {

      if (imageQuality == ImageQuality.HQ) {

         return _photoLoadingStateHQ;

      } else if (imageQuality == ImageQuality.THUMB_HQ
            || imageQuality == ImageQuality.THUMB_HQ_ADJUSTED) {

         return _photoLoadingStateThumbHQ;

      } else if (imageQuality == ImageQuality.ORIGINAL) {

         return _photoLoadingStateOriginal;

      } else {

         return _photoLoadingStateThumb;
      }
   }

   /**
    * @return Returns size when image is painted on the map
    */
   public org.eclipse.swt.graphics.Point getMap25ImageSize() {

      if (_map25ImageRequestedSize != _map25ImageRequestedAndCheckedSize) {

         setMap25ImageRenderSize(_map25ImageRequestedSize);

         _map25ImageRequestedAndCheckedSize = _map25ImageRequestedSize;
      }

      return _map25ImageRenderSize;
   }

   /**
    * @param isShowHQPhotoImages
    *           When <code>false</code> then only the thumb images are displayed
    * @param isShowPhotoAdjustments
    *           Is <code>true</code> when e.g. the photo is cropped
    * @param isEnlargeSmallImages
    *
    * @return Returns size when image is painted on the map
    */
   public org.eclipse.swt.graphics.Point getMap2ImageSize(final boolean isShowHQPhotoImages,
                                                          final boolean isShowPhotoAdjustments,
                                                          final boolean isEnlargeSmallImages) {

      int imageWidth;
      int imageHeight;

      if (isShowHQPhotoImages) {

         imageWidth = _photoImageWidth != Integer.MIN_VALUE ? _photoImageWidth : _thumbImageWidth;
         imageHeight = _photoImageHeight != Integer.MIN_VALUE ? _photoImageHeight : _thumbImageHeight;

      } else {

         // a thumb image is displayed

         imageWidth = _thumbImageWidth;
         imageHeight = _thumbImageHeight;
      }

      imageWidth = imageWidth == Integer.MIN_VALUE ? MAP_IMAGE_DEFAULT_WIDTH_HEIGHT : imageWidth;
      imageHeight = imageHeight == Integer.MIN_VALUE ? MAP_IMAGE_DEFAULT_WIDTH_HEIGHT : imageHeight;

      if (isShowHQPhotoImages && isShowPhotoAdjustments && isCropped && imageWidth != Integer.MIN_VALUE) {

         // adjust to cropping area

         imageWidth = (int) ((cropAreaX2 - cropAreaX1) * imageWidth);
         imageHeight = (int) ((cropAreaY2 - cropAreaY1) * imageHeight);

         // fix 0, this happenes when the mouse is clicked but not moved
         if (imageWidth == 0) {
            imageWidth = 20;
         }

         if (imageHeight == 0) {
            imageHeight = 20;
         }
      }

      final int imageCanvasWidth = _map2ImageRequestedSize;
      final int imageCanvasHeight = _map2ImageRequestedSize;

      org.eclipse.swt.graphics.Point renderSize;

      if (isEnlargeSmallImages == false
            && imageWidth < imageCanvasWidth && imageHeight < imageCanvasHeight) {

         renderSize = new org.eclipse.swt.graphics.Point(imageWidth, imageHeight);

      } else {

         renderSize = RendererHelper.getBestSize(this,

               imageWidth,
               imageHeight,

               imageCanvasWidth,
               imageCanvasHeight);
      }

      return renderSize;
   }

   /**
    * <pre>
    * Orientation
    *
    * The image orientation viewed in terms of rows and columns.
    * Type      =      SHORT
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
   public LocalDateTime getOriginalDateTime() {
      return _exifDateTime != null ? _exifDateTime : _imageFileLastModified;
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

   public long getPhotoTime() {

      if (adjustedTime_Tour != Long.MIN_VALUE) {

         return adjustedTime_Tour;

      } else {

         return imageExifTime;
      }
   }

   public AtomicReference<PhotoSqlLoadingState> getSqlLoadingState() {
      return _photoSqlLoadingState;
   }

   private LocalDateTime getTiffValueDate(final TiffImageMetadata tiffMetadata) {

      try {

         final TiffField exifDate = tiffMetadata.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, true);

         if (exifDate != null) {
            return LocalDateTime.parse(exifDate.getStringValue(), _dtParser);
         }

         final TiffField date = tiffMetadata.findField(TiffTagConstants.TIFF_TAG_DATE_TIME, true);
         if (date != null) {
            return LocalDateTime.parse(date.getStringValue(), _dtParser);
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

   public ToneCurvesFilter getToneCurvesFilter() {

      if (_toneCurvesFilter == null) {
         _toneCurvesFilter = new ToneCurvesFilter();
      }

      return _toneCurvesFilter;
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

   /**
    * Key is tourId
    *
    * @return
    */
   public HashMap<Long, TourPhotoReference> getTourPhotoReferences() {
      return _tourPhotoRef;
   }

   public String getUniqueId() {
      return imageFilePathName;
   }

   /**
    * @return Returns a validate relative crop area
    */
   public Float getValidCropArea() {

      if (cropAreaX1 == 0 && cropAreaX2 == 0
            || cropAreaY1 == 0 && cropAreaY2 == 0) {

         // set initial and valid crop areas

         final float defaultCrop = 0.35f;
         final float defaultCrop2 = 1 - defaultCrop;

         cropAreaX1 = defaultCrop;
         cropAreaY1 = defaultCrop;

         cropAreaX2 = defaultCrop2;
         cropAreaY2 = defaultCrop2;
      }

      return new Rectangle2D.Float(

            cropAreaX1,
            cropAreaY1,
            cropAreaX2,
            cropAreaY2);
   }

   /**
    * @param mapProvider
    * @param projectionHash
    * @param zoomLevel
    * @param isLinkPhotoDisplayed
    *
    * @return Returns the world position for this photo or <code>null</code> when geo position is
    *         not set.
    */
   public Point getWorldPosition(final CommonMapProvider mapProvider,
                                 final int projectionHash,
                                 final int zoomLevel,
                                 final boolean isLinkPhotoDisplayed) {

      final double latitude = isLinkPhotoDisplayed
            ? getLinkLatitude()
            : getTourLatitude();

      if (latitude == 0) {
         return null;
      }

      final Integer hashKey = projectionHash + zoomLevel;

      final Point worldPosition = isLinkPhotoDisplayed
            ? _linkWorldPosition.get(hashKey)
            : _tourWorldPosition.get(hashKey);

      if (worldPosition == null) {

         // convert lat/long into world pixels which depends on the map projection

         final GeoPosition photoGeoPosition = new GeoPosition(latitude,
               isLinkPhotoDisplayed
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

      // cleanup photo
      if (_tourPhotoRef.isEmpty()) {

         isSavedInTour = false;
         ratingStars = 0;

         resetTourExifState();
      }
   }

   void replaceImageFile(final IPath newImageFilePathName) {

      // force loading of metadata
      _photoImageMetadata = null;

      setupPhoto(newImageFilePathName.toFile(), newImageFilePathName);

//      PhotoLoadManager.putPhotoInLoadingErrorMap(imageFilePathName);
   }

   public void resetLinkGeoPositions() {

      _linkLatitude = 0;
      _linkLongitude = 0;

      isLinkPhotoWithGps = isGeoFromExif;
   }

   public void resetLinkWorldPosition() {
      _linkWorldPosition.clear();
   }

   private void resetTourExifState() {

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

   public void setLinkTourId(final long photoLinkTourId) {

      _photoLinkTourId = photoLinkTourId;
   }

   public void setLoadingState(final PhotoLoadingState photoLoadingState, final ImageQuality imageQuality) {

      if (imageQuality == ImageQuality.HQ) {

         _photoLoadingStateHQ = photoLoadingState;

      } else if (imageQuality == ImageQuality.THUMB_HQ
            || imageQuality == ImageQuality.THUMB_HQ_ADJUSTED) {

         _photoLoadingStateThumbHQ = photoLoadingState;

      } else if (imageQuality == ImageQuality.ORIGINAL) {

         _photoLoadingStateOriginal = photoLoadingState;

      } else {

         _photoLoadingStateThumb = photoLoadingState;
      }

      // set overall loading error
      if (photoLoadingState == PhotoLoadingState.IMAGE_IS_INVALID) {

         _isLoadingError = true;
      }
   }

   private void setMap25ImageRenderSize(final int mapImageRequestedSize) {

      final int imageCanvasWidth = mapImageRequestedSize;
      final int imageCanvasHeight = mapImageRequestedSize;

      final int imageWidth = _photoImageWidth != Integer.MIN_VALUE ? _photoImageWidth : _thumbImageWidth;
      final int imageHeight = _photoImageHeight != Integer.MIN_VALUE ? _photoImageHeight : _thumbImageHeight;

      final org.eclipse.swt.graphics.Point renderSize = RendererHelper.getBestSize(this,

            imageWidth,
            imageHeight,

            imageCanvasWidth,
            imageCanvasHeight);

      _map25ImageRenderSize = renderSize;
   }

   public void setPhotoSize(final int width, final int height) {

      if (width == _photoImageHeight && height == _photoImageWidth) {

         /*
          * There is somewhere a bug which do not recognize the photo orientation
          */

      } else {

         _photoImageWidth = width;
         _photoImageHeight = height;
      }

      updateMapImageRenderSize();
   }

   public void setStateExifThumb(final int exifThumbState) {

      _exifThumbImageState = exifThumbState;
   }

   public void setThumbSize(final int width, final int height) {

      _thumbImageWidth = width;
      _thumbImageHeight = height;

      updateMapImageRenderSize();
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
      _imageFileLastModified = LocalDateTime.ofInstant(//
            Instant.ofEpochMilli(lastModified),
//            ZoneOffset.UTC
            ZoneId.systemDefault()
      //
      );

      // initially sort by file date until exif data are loaded
      imageExifTime = lastModified;

      _uniqueId = photoImageFilePathName;

// SET_FORMATTING_OFF

      /*
       * Initialize image keys and loading states
       */
      _imageKey_Thumb            = getImageKey_Thumb              (photoImageFilePathName);
      _imageKey_ThumbHQ          = getImageKey_ThumbHQ            (photoImageFilePathName);
      _imageKey_ThumbHQ_Adjusted = getImageKey_ThumbHQ_Adjusted   (photoImageFilePathName);
      _imageKey_HQ               = getImageKey_HQ                 (photoImageFilePathName);
      _imageKey_Original         = Util.computeMD5                (photoImageFilePathName) + "_KeyOriginal"; //$NON-NLS-1$

      _isImageFileAvailable = photoImageFile.exists();

      if (_isImageFileAvailable) {

         _photoLoadingStateThumb    = PhotoLoadingState.UNDEFINED;
         _photoLoadingStateThumbHQ  = PhotoLoadingState.UNDEFINED;
         _photoLoadingStateHQ       = PhotoLoadingState.UNDEFINED;
         _photoLoadingStateOriginal = PhotoLoadingState.UNDEFINED;

         _isLoadingError = false;

      } else {

         _photoLoadingStateThumb    = PhotoLoadingState.IMAGE_IS_INVALID;
         _photoLoadingStateThumbHQ  = PhotoLoadingState.IMAGE_IS_INVALID;
         _photoLoadingStateHQ       = PhotoLoadingState.IMAGE_IS_INVALID;
         _photoLoadingStateOriginal = PhotoLoadingState.IMAGE_IS_INVALID;

         _isLoadingError = true;
      }
// SET_FORMATTING_ON
   }

   @Override
   public String toString() {

//      final String rotateDegree = _orientation == 8 ? "270" //
//            : _orientation == 3 ? "180" //
//                  : _orientation == 6 ? "90" : "0";

      return UI.EMPTY_STRING

            + "Photo" //
            + " " + imageFileName
            + " adjustedTime_Tour " + adjustedTime_Tour //$NON-NLS-1$
            + " _exifDateTime " + _exifDateTime //$NON-NLS-1$

//            + (_exifDateTime == null ? "-no date-" : "\t" + _exifDateTime)
//            + ("\trotate:" + rotateDegree)
//            + (_imageWidth == Integer.MIN_VALUE ? "-no size-" : "\t" + _imageWidth + "x" + _imageHeight)

//            + ("\tEXIF GPS: " + _exifLatitude + " - " + _exifLongitude) //$NON-NLS-1$ //$NON-NLS-2$
//            + ("\tLink GPS: " + _linkLatitude + " - " + _linkLongitude) //$NON-NLS-1$ //$NON-NLS-2$
//            + ("\tTour GPS: " + _tourLatitude + " - " + _tourLongitude) //$NON-NLS-1$ //$NON-NLS-2$
      //

//            + UI.NEW_LINE

      ;
   }

   void updateImageMetadata(final PhotoImageMetadata photoImageMetadata) {

      _photoImageMetadata = photoImageMetadata;

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

      updateMapImageRenderSize();

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

         final long exifUTCMills = TimeTools.toEpochMilli(_exifDateTime);

         imageExifTime = exifUTCMills;
      }
   }

   public void updateMapImageRenderSize() {

      setMap25ImageRenderSize(_map25ImageRequestedSize);
   }

}
