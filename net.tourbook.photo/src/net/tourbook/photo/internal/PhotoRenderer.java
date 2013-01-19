/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.photo.internal;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.photo.IPhotoServiceProvider;
import net.tourbook.photo.ImageGallery;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoImageMetadata;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;
import net.tourbook.photo.internal.gallery.MT20.AbstractGalleryMT20ItemRenderer;
import net.tourbook.photo.internal.gallery.MT20.DefaultGalleryMT20ItemRenderer;
import net.tourbook.photo.internal.gallery.MT20.GalleryMT20;
import net.tourbook.photo.internal.gallery.MT20.GalleryMT20Item;
import net.tourbook.photo.internal.gallery.MT20.PaintingResult;
import net.tourbook.photo.internal.gallery.MT20.RendererHelper;
import net.tourbook.photo.internal.gallery.MT20.ZoomState;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.imgscalr.Scalr.Rotation;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Paint image in the gallery canvas.
 * <p>
 * Original: org.sharemedia.utils.gallery.ShareMediaIconRenderer2
 */
public class PhotoRenderer extends AbstractGalleryMT20ItemRenderer {

	private static final String			PHOTO_ANNOTATION_GPS_EXIF				= "PHOTO_ANNOTATION_GPS_EXIF";				//$NON-NLS-1$
	private static final String			PHOTO_ANNOTATION_GPS_TOUR				= "PHOTO_ANNOTATION_GPS_TOUR";				//$NON-NLS-1$
	private static final String			PHOTO_ANNOTATION_SAVED_IN_TOUR			= "PHOTO_ANNOTATION_SAVED_IN_TOUR";		//$NON-NLS-1$
	private static final String			PHOTO_ANNOTATION_SAVED_IN_TOUR_HOVERED	= "PHOTO_ANNOTATION_SAVED_IN_TOUR_HOVERED"; //$NON-NLS-1$

	private static final String			PHOTO_RATING_STAR						= "PHOTO_RATING_STAR";						//$NON-NLS-1$
	private static final String			PHOTO_RATING_STAR_AND_HOVERED			= "PHOTO_RATING_STAR_AND_HOVERED";			//$NON-NLS-1$
	private static final String			PHOTO_RATING_STAR_DELETE				= "PHOTO_RATING_STAR_DELETE";				//$NON-NLS-1$
	private static final String			PHOTO_RATING_STAR_DISABLED				= "PHOTO_RATING_STAR_DISABLED";			//$NON-NLS-1$
	private static final String			PHOTO_RATING_STAR_HOVERED				= "PHOTO_RATING_STAR_HOVERED";				//$NON-NLS-1$
	private static final String			PHOTO_RATING_STAR_NOT_HOVERED			= "PHOTO_RATING_STAR_NOT_HOVERED";			//$NON-NLS-1$
	private static final String			PHOTO_RATING_STAR_NOT_HOVERED_BUT_SET	= "PHOTO_RATING_STAR_NOT_HOVERED_BUT_SET";	//$NON-NLS-1$

	/**
	 * Device width for all rating stars.
	 */
	private static int					MAX_RATING_STARS_WIDTH;

	private static final int			MAX_RATING_STARS						= 5;

	/**
	 * this value has been evaluated by some test
	 */
	private int							_textMinThumbSize						= 50;

	private int							_fontHeight								= -1;

	private final DateTimeFormatter		_dtFormatterDate						= DateTimeFormat.forStyle("M-");			//$NON-NLS-1$
	private final DateTimeFormatter		_dtFormatterTime						= DateTimeFormat.forStyle("-F");			//$NON-NLS-1$
	private final DateTimeFormatter		_dtFormatterDateTime					= DateTimeFormat.forStyle("MM");			//$NON-NLS-1$

//	private final DateTimeFormatter		_dtFormatterDateTime	= new DateTimeFormatterBuilder()
//																		.appendYear(4, 4)
//																		.appendLiteral('-')
//																		.appendMonthOfYear(2)
//																		.appendLiteral('-')
//																		.appendDayOfMonth(2)
//																		.appendLiteral(' ')
//																		.appendHourOfDay(2)
//																		.appendLiteral(':')
//																		.appendMinuteOfHour(2)
//																		.appendLiteral(':')
//																		.appendSecondOfMinute(2)
//																		.toFormatter();
//
//	private final DateTimeFormatter		_dtFormatterTime		= new DateTimeFormatterBuilder()
//																		.appendHourOfDay(2)
//																		.appendLiteral(':')
//																		.appendMinuteOfHour(2)
//																		.appendLiteral(':')
//																		.appendSecondOfMinute(2)
//																		.toFormatter();
//
//	private final DateTimeFormatter		_dtFormatterDate		= new DateTimeFormatterBuilder()
//																		.appendYear(4, 4)
//																		.appendLiteral('-')
//																		.appendMonthOfYear(2)
//																		.appendLiteral('-')
//																		.appendDayOfMonth(2)
//																		.toFormatter();

	private boolean						_isShowPhotoName;
	private boolean						_isShowAnnotations;
	private boolean						_isShowDateInfo;

	/**
	 * Attributes (date, filename, rating stars) are not painted when the photo image is too small.
	 */
	private boolean						_isAttributesPainted;

	private PhotoDateInfo				_photoDateInfo;
	private ImageGallery				_imageGallery;
	private GalleryMT20					_galleryMT;

	private int							_gridBorderSize							= 1;
	private int							_imageBorderSize						= 5;

	/**
	 * photo dimension without grid border but including image border
	 */
	private int							_photoWidth;
	private int							_photoHeight;

	/*
	 * position and size where the photo image is painted
	 */
	private int							_photoImageWidth;
	private int							_photoImageHeight;
	private int							_paintedDest_DevX;
	private int							_paintedDest_DevY;

	/**
	 * Width for the painted image or <code>-1</code> when not initialized.
	 */
	private int							_paintedDest_Width						= -1;
	private int							_paintedDest_Height;

	private boolean						_isShowFullsizeHQImage;
	private boolean						_isShowFullsizePreview;
	private boolean						_isShowFullsizeLoadingMessage;
	private boolean						_isShowPhotoRatingStars;
	private boolean						_isRatingStarsPainted;

	/**
	 * Is <code>true</code> when rating stars can be modified.
	 */
	private boolean						_isHandleHoveredRatingStars;

	private double						_imagePaintedZoomFactor;

	private int							_paintedImageWidth;
	private int							_paintedImageHeight;

	private static int					_annotationImageWidth;
	private static int					_annotationImageHeight;
	private static int					_ratingStarImageWidth;
	private static int					_ratingStarImageHeight;

	/**
	 * Right border for the rating stars, this value is relative to the gallery item.
	 */
	private int							_ratingStarsLeftBorder;

//	private int							_devYAnnotation;
//	private int							_devXAnnotationGps;
//	private int							_devXAnnotationTour;

	/*
	 * full size context fields
	 */
	private Image						_fullsizePaintedImage;
	private LoadCallbackOriginalImage	_fullsizeImageLoadCallback;
	private boolean						_isFullsizeImageAvailable;
	private boolean						_isFullsizeLoadingError;

	private final DateTimeFormatter		_dtFormatter							= DateTimeFormat.forStyle("ML");			//$NON-NLS-1$
	private final DateTimeFormatter		_dtWeekday								= DateTimeFormat.forPattern("E");			//$NON-NLS-1$
	private final NumberFormat			_nfMByte								= NumberFormat.getNumberInstance();
	{
		_nfMByte.setMinimumFractionDigits(3);
		_nfMByte.setMaximumFractionDigits(3);
		_nfMByte.setMinimumIntegerDigits(1);
	}

	/**
	 * 
	 */
	private boolean						_isFocusActive;

	/*
	 * UI resources
	 */
	private Color						_fullsizeBgColor						= Display.getCurrent()//
																						.getSystemColor(SWT.COLOR_BLACK);
	private Color						_fgColor								= Display.getCurrent()//
																						.getSystemColor(SWT.COLOR_WHITE);
	private Color						_bgColor								= Display.getCurrent()//
																						.getSystemColor(SWT.COLOR_RED);
	private Color						_selectionFgColor;
	private Color						_noFocusSelectionFgColor;

	private static Image				_imageAnnotationGpsExif;
	private static Image				_imageAnnotationGpsTour;
	private static Image				_imageAnnotationSavedInTour;
	private static Image				_imageAnnotationSavedInTour_Hovered;

	private static Image				_imageRatingStar;
	private static Image				_imageRatingStarAndHovered;
	private static Image				_imageRatingStarDisabled;
	private static Image				_imageRatingStarDelete;
	private static Image				_imageRatingStarHovered;
	private static Image				_imageRatingStarNotHovered;
	private static Image				_imageRatingStarNotHoveredButSet;

	static {

		UI.IMAGE_REGISTRY.put(PHOTO_ANNOTATION_GPS_EXIF, //
				Activator.getImageDescriptor(Messages.Image__PhotoAnnotationExifGPS));
		UI.IMAGE_REGISTRY.put(PHOTO_ANNOTATION_GPS_TOUR,//
				Activator.getImageDescriptor(Messages.Image__PhotoAnnotationTourGPS));

		UI.IMAGE_REGISTRY.put(PHOTO_ANNOTATION_SAVED_IN_TOUR,//
				Activator.getImageDescriptor(Messages.Image__PhotoAnnotationSavedInTour));
		UI.IMAGE_REGISTRY.put(PHOTO_ANNOTATION_SAVED_IN_TOUR_HOVERED,//
				Activator.getImageDescriptor(Messages.Image__PhotoAnnotationSavedInTourHovered));

		UI.IMAGE_REGISTRY.put(PHOTO_RATING_STAR,//
				Activator.getImageDescriptor(Messages.Image__PhotoRatingStar));
		UI.IMAGE_REGISTRY.put(PHOTO_RATING_STAR_AND_HOVERED,//
				Activator.getImageDescriptor(Messages.Image__PhotoRatingStarAndHovered));
		UI.IMAGE_REGISTRY.put(PHOTO_RATING_STAR_DISABLED,//
				Activator.getImageDescriptor(Messages.Image__PhotoRatingStarDisabled));
		UI.IMAGE_REGISTRY.put(PHOTO_RATING_STAR_DELETE,//
				Activator.getImageDescriptor(Messages.Image__PhotoRatingStarDelete));
		UI.IMAGE_REGISTRY.put(PHOTO_RATING_STAR_HOVERED,//
				Activator.getImageDescriptor(Messages.Image__PhotoRatingStarHovered));
		UI.IMAGE_REGISTRY.put(PHOTO_RATING_STAR_NOT_HOVERED,//
				Activator.getImageDescriptor(Messages.Image__PhotoRatingStarNotHovered));
		UI.IMAGE_REGISTRY.put(PHOTO_RATING_STAR_NOT_HOVERED_BUT_SET,//
				Activator.getImageDescriptor(Messages.Image__PhotoRatingStarNotHoveredButSet));

		_imageAnnotationGpsExif = UI.IMAGE_REGISTRY.get(PHOTO_ANNOTATION_GPS_EXIF);
		_imageAnnotationGpsTour = UI.IMAGE_REGISTRY.get(PHOTO_ANNOTATION_GPS_TOUR);
		_imageAnnotationSavedInTour = UI.IMAGE_REGISTRY.get(PHOTO_ANNOTATION_SAVED_IN_TOUR);
		_imageAnnotationSavedInTour_Hovered = UI.IMAGE_REGISTRY.get(PHOTO_ANNOTATION_SAVED_IN_TOUR_HOVERED);

		_imageRatingStar = UI.IMAGE_REGISTRY.get(PHOTO_RATING_STAR);
		_imageRatingStarAndHovered = UI.IMAGE_REGISTRY.get(PHOTO_RATING_STAR_AND_HOVERED);
		_imageRatingStarDelete = UI.IMAGE_REGISTRY.get(PHOTO_RATING_STAR_DELETE);
		_imageRatingStarDisabled = UI.IMAGE_REGISTRY.get(PHOTO_RATING_STAR_DISABLED);
		_imageRatingStarHovered = UI.IMAGE_REGISTRY.get(PHOTO_RATING_STAR_HOVERED);
		_imageRatingStarNotHovered = UI.IMAGE_REGISTRY.get(PHOTO_RATING_STAR_NOT_HOVERED);
		_imageRatingStarNotHoveredButSet = UI.IMAGE_REGISTRY.get(PHOTO_RATING_STAR_NOT_HOVERED_BUT_SET);

		final Rectangle bounds = _imageAnnotationGpsExif.getBounds();
		_annotationImageWidth = bounds.width;
		_annotationImageHeight = bounds.height;

		final Rectangle ratingStarBounds = _imageRatingStar.getBounds();
		_ratingStarImageWidth = ratingStarBounds.width;
		_ratingStarImageHeight = ratingStarBounds.height;

		MAX_RATING_STARS_WIDTH = _ratingStarImageWidth * MAX_RATING_STARS;
	}

	public PhotoRenderer(final GalleryMT20 galleryMT20, final ImageGallery imageGallery) {
		_galleryMT = galleryMT20;
		_imageGallery = imageGallery;
	}

	@Override
	public void draw(	final GC gc,
						final GalleryMT20Item galleryItem,
						final int galleryItemViewPortX,
						final int galleryItemViewPortY,
						final int galleryItemWidth,
						final int galleryItemHeight,
						final boolean isSelected,
						final boolean isFocusActive) {

		final Photo photo = galleryItem.photo;
		if (photo == null) {
			// this case should not happen but it did
			return;
		}

//		System.out.println(UI.timeStampNano()
//				+ (" \t" + photo.imageFileName)
//				+ ("\tratingStars=" + photo.ratingStars)
//				+ ("\t" + System.identityHashCode(photo)));
//		photo.dumpTourReferences();
//
////		PhotoCache.dumpAllPhotos();
//		// TODO remove SYSTEM.OUT.PRINTLN

//		if (photo.imageFileName.equals("P1000641.JPG")) { //$NON-NLS-1$
//
//			System.out.println(UI.timeStampNano() + " photo\t" + photo.imageFileName); //$NON-NLS-1$
//			// TODO remove SYSTEM.OUT.PRINTLN
//		}

		_isFocusActive = isFocusActive;

		// init fontheight
		if (_fontHeight == -1) {
			_fontHeight = gc.getFontMetrics().getHeight();
		}

		galleryItem.photoPaintedX = galleryItemViewPortX + _gridBorderSize;
		galleryItem.photoPaintedY = galleryItemViewPortY + _gridBorderSize;
		_photoWidth = galleryItemWidth - _gridBorderSize;
		_photoHeight = galleryItemHeight - _gridBorderSize;

		int itemImageWidth = _photoWidth;
		int itemImageHeight = _photoHeight;

		// center ratings stars in the middle of the image
		_ratingStarsLeftBorder = _photoWidth / 2 - MAX_RATING_STARS_WIDTH / 2;

		final int border = _imageBorderSize;
		final int border2 = border / 2;

		// ignore border for small images
		final boolean isBorder = itemImageWidth - border >= _textMinThumbSize;

		itemImageWidth -= isBorder ? border : 0;
		itemImageHeight -= isBorder ? border : 0;
		_paintedDest_Width = itemImageWidth;
		_paintedDest_Height = itemImageHeight;

		final int imageX = galleryItem.photoPaintedX + (isBorder ? border2 : 0);
		final int imageY = galleryItem.photoPaintedY + (isBorder ? border2 : 0);

		final ImageQuality requestedImageQuality = itemImageWidth <= PhotoLoadManager.IMAGE_SIZE_THUMBNAIL
				? ImageQuality.THUMB
				: ImageQuality.HQ;

		// painted image can have different sizes for 1 photo: original, HQ and thumb
		Image paintedImage = null;
		boolean isRequestedQuality = false;

		// check if image has an loading error
		final PhotoLoadingState photoLoadingState = photo.getLoadingState(requestedImageQuality);

		if (photoLoadingState != PhotoLoadingState.IMAGE_IS_INVALID) {

			// image is not yet loaded

			// check if image is in the cache
			paintedImage = PhotoImageCache.getImage(photo, requestedImageQuality);

			if ((paintedImage == null || paintedImage.isDisposed())
					&& photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

				// the requested image is not available in the image cache -> image must be loaded

				final LoadCallbackImage imageLoadCallback = new LoadCallbackImage(_imageGallery, galleryItem);

				PhotoLoadManager.putImageInLoadingQueueThumbGallery(
						galleryItem,
						photo,
						requestedImageQuality,
						imageLoadCallback);
			}

			isRequestedQuality = true;

			if (paintedImage == null || paintedImage.isDisposed()) {

				// requested size is not available, try to get image with lower quality

				isRequestedQuality = false;

				final ImageQuality lowerImageQuality = galleryItemWidth > PhotoLoadManager.IMAGE_SIZE_THUMBNAIL
						? ImageQuality.THUMB
						: ImageQuality.HQ;

				paintedImage = PhotoImageCache.getImage(photo, lowerImageQuality);
			}
		}

		gc.setForeground(_fgColor);
		gc.setBackground(_bgColor);

		if (paintedImage != null && paintedImage.isDisposed() == false) {

			/*
			 * draw photo image, when photo height is smaller than min photo height, only the
			 * picture but not the text is displayed
			 */

			/*
			 * an exception can occure because the image could be disposed before it is drawn
			 */
			try {

				final Rectangle imageBounds = paintedImage.getBounds();
				_paintedImageWidth = imageBounds.width;
				_paintedImageHeight = imageBounds.height;
			} catch (final Exception e1) {
				StatusUtil.log(e1);
			}

			final boolean isPainted = draw_Image(gc, photo, paintedImage, galleryItem,//
					imageX,
					imageY,
					itemImageWidth,
					itemImageHeight,
					isRequestedQuality,
					isSelected);

			if (isPainted == false) {
				// error occured painting the image, invalidate canvas
			}

//			// debug box for the image area
//			gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
//			gc.drawRectangle(imageX, imageY, imageWidth - 2, imageHeight - 1);

		} else {

			// image is not available

			drawStatusText(gc, photo, //
					imageX,
					imageY,
					itemImageWidth,
					itemImageHeight,
					requestedImageQuality,
					_isAttributesPainted && _isShowPhotoName,
					isSelected,
					false,
					_bgColor);
		}

		final boolean isDrawPhotoDateName = _isShowPhotoName || _isShowDateInfo;

		// draw name & date & annotations
		if (_isAttributesPainted && isDrawPhotoDateName) {
			drawAttributes(gc, photo, //
					imageX,
					imageY,
					itemImageWidth,
					itemImageHeight);
		}

		// annotations are drawn in the bottom right corner of the image
		if (_isShowAnnotations) {
			drawAnnotations(gc, galleryItem, photo);
		}

		if (_isAttributesPainted && _isShowPhotoRatingStars) {

			drawRatingStars(gc, galleryItem, photo.ratingStars);

			_isRatingStarsPainted = true;

		} else {

			_isRatingStarsPainted = false;
		}

//		// debug box for the whole gallery item area
//		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GREEN));
//		gc.drawRectangle(galleryItemViewPortX - 1, galleryItemViewPortY, galleryItemWidth - 1, galleryItemHeight - 1);
	}

	/**
	 * Draw photo image centered in the photo canvas.
	 * 
	 * @param gc
	 * @param photo
	 * @param photoImage
	 * @param galleryItem
	 * @param photoPosX
	 * @param photoPosY
	 * @param imageCanvasWidth
	 * @param imageCanvasHeight
	 * @param isRequestedQuality
	 * @param isSelected
	 * @param isFullsizeImage
	 * @return
	 */
	private boolean draw_Image(	final GC gc,
								final Photo photo,
								final Image photoImage,
								final GalleryMT20Item galleryItem,
								final int photoPosX,
								final int photoPosY,
								final int imageCanvasWidth,
								final int imageCanvasHeight,
								final boolean isRequestedQuality,
								final boolean isSelected) {

		final Point bestSize = RendererHelper.getBestSize(
				photo,
				_paintedImageWidth,
				_paintedImageHeight,
				imageCanvasWidth,
				imageCanvasHeight);

		_paintedDest_Width = bestSize.x;
		_paintedDest_Height = bestSize.y;

		// get center offset
		final int centerOffsetX = (imageCanvasWidth - _paintedDest_Width) / 2;
		final int centerOffsetY = (imageCanvasHeight - _paintedDest_Height) / 2;

		_paintedDest_DevX = photoPosX + centerOffsetX;
		_paintedDest_DevY = photoPosY + centerOffsetY;

		try {

			try {

				gc.drawImage(photoImage, //
						0,
						0,
						_paintedImageWidth,
						_paintedImageHeight,
						//
						_paintedDest_DevX,
						_paintedDest_DevY,
						_paintedDest_Width,
						_paintedDest_Height);

				galleryItem.imagePaintedWidth = _paintedDest_Width;
				galleryItem.imagePaintedHeight = _paintedDest_Height;

			} catch (final Exception e) {

				System.out.println("SWT exception occured when painting valid image " //$NON-NLS-1$
						+ photo.imageFilePathName
						+ " it's potentially this bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=375845"); //$NON-NLS-1$

				// ensure image is valid after reloading
//				photoImage.dispose();

				PhotoImageCache.disposeAll();

				return false;
			}

			/*
			 * draw selection
			 */
			if (isSelected) {

				// draw marker line on the left side
				gc.setBackground(_isFocusActive ? _selectionFgColor : _noFocusSelectionFgColor);
				gc.fillRectangle(_paintedDest_DevX, _paintedDest_DevY, 2, _paintedDest_Height);
			}

			/*
			 * draw HQ marker
			 */
			if (isRequestedQuality == false) {

				// draw an marker that the requested image quality is not yet painted

				final int markerSize = 9;

				gc.setBackground(_selectionFgColor);
				gc.fillRectangle(//
						_paintedDest_DevX + _paintedDest_Width - markerSize,
						_paintedDest_DevY,
						markerSize,
						markerSize);
			}

		} catch (final Exception e) {

			gc.drawString(e.getMessage(), photoPosX, photoPosY);

			// this case can happen very often when an image is drawn
//			final String message = ("srcWidth: " + imageWidth) //$NON-NLS-1$
//					+ ("  srcHeight:" + imageHeight) //$NON-NLS-1$
//					+ ("  destX:" + destX) //$NON-NLS-1$
//					+ ("  destY:" + destY) //$NON-NLS-1$
//					+ ("  destWidth: " + photoPaintedWidth) //$NON-NLS-1$
//					+ ("  destHeight :" + photoPaintedHeight) //$NON-NLS-1$
//					+ ("  " + photo); //$NON-NLS-1$
//
//			StatusUtil.log(message, e);
		}

		return true;
	}

	private void drawAnnotations(final GC gc, final GalleryMT20Item galleryItem, final Photo photo) {

		final boolean isItemHovered = galleryItem.isHovered;

		final int devXAnnotation = _paintedDest_DevX + _paintedDest_Width;
		final int devYAnnotation = _paintedDest_DevY + _paintedDest_Height - _annotationImageHeight;

		int devXAnnotationOffset = 0;
		Image annotationImage;

		if (photo.isPhotoWithGps) {

			devXAnnotationOffset = _annotationImageWidth;

			annotationImage = photo.isGeoFromExif ? _imageAnnotationGpsExif : _imageAnnotationGpsTour;

			gc.drawImage(//
					annotationImage,
					devXAnnotation - devXAnnotationOffset,
					devYAnnotation);

			galleryItem.itemX_AnnotationPainted_Gps = devXAnnotation - _paintedDest_DevX;

		} else {

			galleryItem.itemX_AnnotationPainted_Gps = Integer.MIN_VALUE;
		}

		if (photo.isSavedInTour) {

			devXAnnotationOffset += _annotationImageWidth;

			if (isItemHovered && galleryItem.isHoveredAnnotationTour) {
				annotationImage = _imageAnnotationSavedInTour_Hovered;
			} else {
				annotationImage = _imageAnnotationSavedInTour;
			}

			gc.drawImage(//
					annotationImage,
					devXAnnotation - devXAnnotationOffset,
					devYAnnotation);

			galleryItem.itemXAnnotationPaintedTour = devXAnnotation - galleryItem.photoPaintedX - devXAnnotationOffset;

//			galleryItem.photoPaintedX

		} else {

			galleryItem.itemXAnnotationPaintedTour = Integer.MIN_VALUE;
		}

		galleryItem.itemYAnnotationPainted = devYAnnotation - galleryItem.photoPaintedY;
	}

	private void drawAttributes(final GC gc,
								final Photo photo,
								final int photoPosX,
								final int photoPosY,
								final int photoWidth,
								final int photoHeight) {

		/*
		 * get text for date/filename
		 */
		int textFileNameWidth = -1;
		int textDateTimeWidth = -1;
		String textFileName = null;
		String textDateTime = null;

		int textFileNamePosCenterX = 0;
		int textDateTimePosCenterX = 0;

		if (_isShowPhotoName) {
			textFileName = photo.imageFileName;
			textFileNameWidth = gc.textExtent(textFileName).x;

			textFileNamePosCenterX = (photoWidth - (textFileNameWidth > photoWidth ? photoWidth : textFileNameWidth)) / 2;
		}

		if (_isShowDateInfo) {
			final DateTime dateTime = photo.getOriginalDateTime();
			if (dateTime != null) {

				if (_photoDateInfo == PhotoDateInfo.Date) {

					textDateTime = _dtFormatterDate.print(dateTime);

				} else if (_photoDateInfo == PhotoDateInfo.Time) {

					textDateTime = _dtFormatterTime.print(dateTime);

				} else {
					textDateTime = _dtFormatterDateTime.print(dateTime);
				}

				textDateTimeWidth = gc.textExtent(textDateTime).x;

				textDateTimePosCenterX = (photoWidth - (textDateTimeWidth > photoWidth ? photoWidth : textDateTimeWidth)) / 2;
			}
		}

		/*
		 * get text position
		 */
		final int defaultTextPosY = photoPosY + photoHeight - _fontHeight;

		int posXFilename = photoPosX;
		int posYFilename = defaultTextPosY;
		int posXDate = photoPosX;
		final int posYDate = defaultTextPosY;

		if (textFileNameWidth != -1 && textDateTimeWidth != -1) {

			// paint filename & date

			final int textSpacing = 10;
			final int textWidth = textFileNameWidth + textSpacing + textDateTimeWidth;

			if (textWidth > photoWidth) {

				// paint on top of each other, filename first

				posXFilename += textFileNamePosCenterX;
				posXDate += textDateTimePosCenterX;
				posYFilename -= _fontHeight;

			} else {

				// center text

				final int textX = (photoWidth - textWidth) / 2;
				posXFilename += textX;
				posXDate += textX + textFileNameWidth + textSpacing;
			}

		} else if (textFileNameWidth != -1) {

			// paint only filename
			posXFilename += textFileNamePosCenterX;

		} else if (textDateTimeWidth != -1) {

			// paint only date
			posXDate += textDateTimePosCenterX;
		}

		/*
		 * draw text
		 */
		gc.setForeground(_fgColor);
		gc.setBackground(_bgColor);

		// draw filename
		if (textFileNameWidth != -1) {
			gc.drawString(textFileName, posXFilename, posYFilename, false);
		}

		// draw date time
		if (textDateTimeWidth != -1) {
			gc.drawString(textDateTime, posXDate, posYDate, false);
		}
	}

	@Override
	public PaintingResult drawFullSize(	final GC gc,
										final GalleryMT20Item galleryItem,
										final int canvasWidth,
										final int canvasHeight,
										final ZoomState zoomState,
										final double zoomFactor) {

		final Photo photo = galleryItem.photo;
		if (photo == null) {
			return null;
		}

		final ImageQuality requestedImageQuality = ImageQuality.ORIGINAL;

		// painted image can have different sizes for 1 photo: original, HQ and thumb
		final Image paintedImage = _fullsizePaintedImage;

		final boolean isImageAvailable = paintedImage != null && paintedImage.isDisposed() == false;

		if (_isShowFullsizeHQImage && _isFullsizeImageAvailable) {
			gc.setAntialias(SWT.ON);
			gc.setInterpolation(SWT.LOW);
		} else {
			gc.setAntialias(SWT.OFF);
			gc.setInterpolation(SWT.OFF);
		}

		gc.setForeground(_fgColor);
		gc.setBackground(_fullsizeBgColor);

		boolean isPainted = true;

		/*
		 * paint image
		 */
		if (isImageAvailable) {

			//an exception can occure because the image could be disposed before it is drawn
			try {
				final Rectangle imageBounds = paintedImage.getBounds();
				_paintedImageWidth = imageBounds.width;
				_paintedImageHeight = imageBounds.height;
			} catch (final Exception e1) {
				StatusUtil.log(e1);
			}

			final int photoImageWidth = photo.getImageWidth();
			final int photoImageHeight = photo.getImageHeight();

			if (photoImageWidth == Integer.MIN_VALUE) {

				// get size from image
				_photoImageWidth = _paintedImageWidth;
				_photoImageHeight = _paintedImageHeight;

			} else {
				_photoImageWidth = photoImageWidth;
				_photoImageHeight = photoImageHeight;
			}

			if (zoomState == ZoomState.FIT_WINDOW || zoomFactor == 0.0) {

				isPainted = draw_Image(gc, photo, paintedImage, galleryItem,//
						0,
						0,
						canvasWidth,
						canvasHeight,
						_isFullsizeImageAvailable,
						false);

				setPaintedZoomFactor(_photoImageWidth, _photoImageHeight, _paintedDest_Width, _paintedDest_Height);

			} else {

				drawFullSize_Image(gc, photo, paintedImage, galleryItem,//
						canvasWidth,
						canvasHeight,
						zoomState,
						zoomFactor,
						_isFullsizeImageAvailable);
			}
		}

		/*
		 * draw status message
		 */
		if (_isShowFullsizeLoadingMessage && _isFullsizeImageAvailable == false || _isFullsizeLoadingError) {

			drawStatusText(gc, photo, //
					0, // 								x
					canvasHeight - _fontHeight - 1, //	y
					canvasWidth, // 					width
					_fontHeight + 1, // 				height
					requestedImageQuality,
					false,
					false,
					true,
					_fullsizeBgColor);
		}

		// load fullsize image delayed, after the UI is updated
		if (_fullsizeImageLoadCallback != null) {

			PhotoLoadManager.putImageInLoadingQueueOriginal(galleryItem, photo, _fullsizeImageLoadCallback);

			_fullsizeImageLoadCallback = null;
		}

		final PaintingResult paintingResult = new PaintingResult();

		paintingResult.imagePaintedZoomFactor = _imagePaintedZoomFactor;
		paintingResult.isOriginalImagePainted = _isFullsizeImageAvailable;
		paintingResult.isLoadingError = _isFullsizeLoadingError;
		paintingResult.isPainted = isPainted;

		return paintingResult;
	}

	private void drawFullSize_Image(final GC gc,
									final Photo photoWrapper,
									final Image photoImage,
									final GalleryMT20Item galleryItem,
									final int canvasWidth,
									final int canvasHeight,
									final ZoomState zoomState,
									final double zoomFactor,
									final boolean isRequestedQuality) {

//		final Point bestSize = computeBestSize(photoWrapper.photo, canvasWidth, canvasHeight);

		try {

			final int srcX = 0;
			final int srcY = 0;
			final int srcWidth = _photoImageWidth;
			final int srcHeight = _photoImageHeight;

			int destX = 0;
			int destY = 0;
			int destWidth = canvasWidth;
			int destHeight = canvasHeight;

			if (zoomState == ZoomState.ZOOMING) {

				final int zoomedImageWidth = (int) (srcWidth * zoomFactor);
				final int zoomedImageHeight = (int) (srcHeight * zoomFactor);

				if (zoomedImageWidth > canvasWidth || zoomedImageHeight > canvasHeight) {

					// image is larger than the monitor

				} else {

					// image is smaller than the monitor, center image

					final int offsetX = (canvasWidth - zoomedImageWidth) / 2;
					final int offsetY = (canvasHeight - zoomedImageHeight) / 2;

					destX = offsetX;
					destY = offsetY;
					destWidth = zoomedImageWidth;
					destHeight = zoomedImageHeight;
				}
			}

			try {
				gc.drawImage(photoImage, //
						srcX,
						srcY,
						srcWidth,
						srcHeight,
						//
						destX,
						destY,
						destWidth,
						destHeight);

			} catch (final Exception e) {
				// this bug is covered here: https://bugs.eclipse.org/bugs/show_bug.cgi?id=375845
			}

			if (isRequestedQuality == false) {

				// draw an marker that the requested image quality is not yet painted

				final int markerSize = 9;

				gc.setBackground(_selectionFgColor);
				gc.fillRectangle(//
						destX + destWidth - markerSize,
						destY,
						markerSize,
						markerSize);
			}

			// keep painted zoomfactor
			setPaintedZoomFactor(srcWidth, srcHeight, destWidth, destHeight);

		} catch (final Exception e) {

//			gc.drawString(e.getMessage(), photoPosX, photoPosY);

			// this case can happen very often when an image is drawn
//			final String message = ("srcWidth: " + imageWidth) //$NON-NLS-1$
//					+ ("  srcHeight:" + imageHeight) //$NON-NLS-1$
//					+ ("  destX:" + destX) //$NON-NLS-1$
//					+ ("  destY:" + destY) //$NON-NLS-1$
//					+ ("  destWidth: " + photoPaintedWidth) //$NON-NLS-1$
//					+ ("  destHeight :" + photoPaintedHeight) //$NON-NLS-1$
//					+ ("  " + photo); //$NON-NLS-1$
//
//			StatusUtil.log(message, e);
		}

	}

	@Override
	public Rectangle drawFullSizeSetContext(final Shell shell,
											final GalleryMT20Item galleryItem,
											final int monitorWidth,
											final int monitorHeight) {

		final Photo photo = galleryItem.photo;
		if (photo == null) {
			return null;
		}

		// show image file name in the shell
		shell.setText(NLS.bind(Messages.App__PhotoShell_Title, photo.imageFileName));

		final ImageQuality requestedImageQuality = ImageQuality.ORIGINAL;

		_isFullsizeImageAvailable = false;

		// painted image can have different sizes for 1 photo: original, HQ and thumb

		// check if image has an loading error
		final PhotoLoadingState photoLoadingState = photo.getLoadingState(requestedImageQuality);

		_fullsizePaintedImage = null;
		_fullsizeImageLoadCallback = null;

		_isFullsizeLoadingError = photoLoadingState == PhotoLoadingState.IMAGE_IS_INVALID;

		if (_isFullsizeLoadingError == false) {

			// image is not yet loaded

			// check if fullsize image is in the cache
			_fullsizePaintedImage = PhotoImageCache.getImageOriginal(photo);

			_isFullsizeImageAvailable = _fullsizePaintedImage != null && _fullsizePaintedImage.isDisposed() == false;

			if (_isFullsizeImageAvailable == false
					&& photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

				/*
				 * the requested image is not available in the image cache -> image must be loaded
				 * but is delayed that the status text is immediatedly be displayed, it
				 */
				_fullsizeImageLoadCallback = new LoadCallbackOriginalImage(_imageGallery, galleryItem, photo);

				/*
				 * get thumb image
				 */
				if (_isShowFullsizePreview) {

					// original size is not available, try to get image with lower quality

					_fullsizePaintedImage = PhotoImageCache.getImage(photo, ImageQuality.HQ);

					if (_fullsizePaintedImage == null || _fullsizePaintedImage.isDisposed()) {

						// requested size is not available, try to get image with lower quality

						_fullsizePaintedImage = PhotoImageCache.getImage(photo, ImageQuality.THUMB);
					}
				}
			}
		}

//		Rectangle clippingArea = null;
//		if (_isFullsizeImageAvailable
//				|| (_fullsizePaintedImage != null && _fullsizePaintedImage.isDisposed() == false)
//				|| _isFullsizeLoadingError) {
//
//			// paint image, original or thumb
//
//			clippingArea = new Rectangle(0, 0, monitorWidth, monitorHeight);
//
//		} else if (_isShowFullsizeLoadingMessage) {
//
//			// paint status text
//
//			final int statusHeight = _fontHeight + 1;
//
//			clippingArea = new Rectangle(0, monitorHeight - statusHeight, monitorWidth, statusHeight);
//		}
//
//		if (_fullsizeImageLoadCallback != null) {
//
//			/*
//			 * an image loading is done at the end of the paint event, but a redraw is not fired
//			 * when reaching this point -> load image now
//			 */
//
//			PhotoLoadManager.putImageInLoadingQueueOriginal(galleryItem, photo, _fullsizeImageLoadCallback);
//
//			_fullsizeImageLoadCallback = null;
//		}
//
//		// paint clipping area or nothing
//		return clippingArea;

// ORIGINAL
//
		if (_isFullsizeImageAvailable
				|| (_fullsizePaintedImage != null && _fullsizePaintedImage.isDisposed() == false)
				|| _isFullsizeLoadingError) {

			// paint image, original or thumb

			return new Rectangle(0, 0, monitorWidth, monitorHeight);

		} else if (_isShowFullsizeLoadingMessage) {

			// paint status text

			final int statusHeight = _fontHeight + 1;
			return new Rectangle(0, monitorHeight - statusHeight, monitorWidth, statusHeight);
		}

		if (_fullsizeImageLoadCallback != null) {

			/*
			 * an image loading is done at the end of the paint event, but a redraw is not fired
			 * when reaching this point -> load image now
			 */

			PhotoLoadManager.putImageInLoadingQueueOriginal(galleryItem, photo, _fullsizeImageLoadCallback);

			_fullsizeImageLoadCallback = null;
		}

		// paint nothing
		return null;

	}

	private void drawRatingStars(final GC gc, final GalleryMT20Item galleryItem, final int ratingStars) {

		final boolean isItemHovered = (galleryItem.isHovered || galleryItem.isInHoveredGroup)
				&& _isHandleHoveredRatingStars;

		final int hoveredStars = galleryItem.hoveredStars;
		final boolean isStarHovered = hoveredStars > 0;
		boolean canSetRating = false;

		final Photo photo = galleryItem.photo;
		if (photo != null) {
			canSetRating = photo.isSavedInTour;
		}

		// center ratings stars in the middle of the image
		final int ratingStarsLeftBorder = galleryItem.photoPaintedX + _photoWidth / 2 - MAX_RATING_STARS_WIDTH / 2;

		for (int starIndex = 0; starIndex < MAX_RATING_STARS; starIndex++) {

			Image starImage;

			if (canSetRating) {

				if (isItemHovered) {

					if (isStarHovered) {

						if (starIndex < hoveredStars) {

							if (starIndex < ratingStars) {

								if (starIndex == ratingStars - 1) {
									starImage = _imageRatingStarDelete;
								} else {
									starImage = _imageRatingStarAndHovered;
								}
							} else {
								starImage = _imageRatingStarHovered;
							}

						} else {
							if (starIndex < ratingStars) {
								starImage = _imageRatingStarNotHoveredButSet;
							} else {
								starImage = _imageRatingStarNotHovered;
							}
						}

					} else {

						if (starIndex < ratingStars) {
							starImage = _imageRatingStar;
						} else {
							starImage = _imageRatingStarNotHovered;
						}
					}

				} else {

					// item is not hovered

					if (starIndex < ratingStars) {
						starImage = _imageRatingStar;
					} else {
						return;
					}
				}
			} else {

				// rating stars cannot be set

				if (isItemHovered) {

					if (isStarHovered == false) {

						if (starIndex > 0) {

							// draw only one disabled star, thats enough

							return;
						}
					}
				} else {

					if (starIndex > 0) {

						// draw only one disabled star, thats enough

						return;
					}
				}

				starImage = _imageRatingStarDisabled;
			}

			// draw stars at the top

			gc.drawImage(starImage, //
					ratingStarsLeftBorder + (_ratingStarImageWidth * (starIndex)),
					galleryItem.photoPaintedY);
		}
	}

	/**
	 * @param gc
	 * @param photo
	 * @param photoPosX
	 * @param photoPosY
	 * @param imageCanvasWidth
	 * @param imageCanvasHeight
	 * @param requestedImageQuality
	 * @param isImageNameDisplayed
	 * @param isSelected
	 * @param isFullsizeImage
	 * @param bgColor
	 */
	private void drawStatusText(final GC gc,
								final Photo photo,
								final int photoPosX,
								final int photoPosY,
								final int imageCanvasWidth,
								final int imageCanvasHeight,
								final ImageQuality requestedImageQuality,
								final boolean isImageNameDisplayed,
								final boolean isSelected,
								final boolean isFullsizeImage,
								final Color bgColor) {

		final boolean isLoadingError = photo.getLoadingState(requestedImageQuality) == PhotoLoadingState.IMAGE_IS_INVALID;

		if (isFullsizeImage && isLoadingError == false && _isShowFullsizeLoadingMessage == false) {
			return;
		}

		final String photoImageFileName = isImageNameDisplayed ? //
				// don't show file name a 2nd time
				UI.EMPTY_STRING
				: photo.imageFileName;

		String statusText;
		PhotoImageMetadata metaData = null;

		if (isLoadingError) {

			// {0} loading failed
			statusText = NLS.bind(Messages.Pic_Dir_StatusLabel_LoadingFailed, photoImageFileName);

		} else {

			final int exifThumbImageState = photo.getExifThumbImageState();
			metaData = photo.getImageMetaDataRaw();

			if (isFullsizeImage) {

				if (metaData == null) {

					statusText = NLS.bind(Messages.Pic_Dir_StatusLabel_LoadingFullsizeNoMeta, photoImageFileName);

				} else {

					final int imageWidth = photo.getImageWidth();
					final boolean isImageLoaded = imageWidth != Integer.MIN_VALUE;

					/*
					 * dimension
					 */
					String textDimension = UI.EMPTY_STRING;
					if (isImageLoaded) {
						textDimension = imageWidth + " x " + photo.getImageHeight(); //$NON-NLS-1$
					}

					/*
					 * size
					 */
					final String textSize = _nfMByte.format(photo.imageFileSize / 1024.0 / 1024.0)
							+ UI.SPACE
							+ UI.UNIT_MBYTES;

					/*
					 * date/time
					 */
					final DateTime dateTime = photo.getOriginalDateTime();
					String textDateTime = UI.EMPTY_STRING;
					if (dateTime != null) {
						textDateTime = _dtWeekday.print(dateTime) + UI.SPACE2 + _dtFormatter.print(dateTime);
					}

					/*
					 * orientation
					 */
					String textOrientation = UI.EMPTY_STRING;
					final int orientation = photo.getOrientation();
					if (orientation > 1) {
						// see here http://www.impulseadventure.com/photo/exif-orientation.html

						if (orientation == 8) {
							textOrientation = Rotation.CW_270.toString();
						} else if (orientation == 3) {
							textOrientation = Rotation.CW_180.toString();
						} else if (orientation == 6) {
							textOrientation = Rotation.CW_90.toString();
						}
					}

					statusText = NLS.bind(Messages.Pic_Dir_StatusLabel_LoadingFullsizeMeta, new Object[] {
							photoImageFileName,
							textDimension,
							textSize,
							textDateTime,
							textOrientation });
				}

			} else {

				if (metaData == null || exifThumbImageState == -1) {

					// {0} loading thumb and exif...

					statusText = NLS.bind(Messages.Pic_Dir_StatusLabel_LoadingThumbExif, photoImageFileName);

				} else {

					// {0} loading fullsize...
					statusText = NLS.bind(Messages.Pic_Dir_StatusLabel_LoadingFullsize, photoImageFileName);
				}
			}
		}

		final int textWidth = gc.textExtent(statusText).x;

		// Center text
		final int textOffsetX = (imageCanvasWidth - (textWidth > imageCanvasWidth ? imageCanvasWidth : textWidth)) / 2;
		final int textOffsetY = (imageCanvasHeight - (_fontHeight > imageCanvasHeight ? imageCanvasHeight : _fontHeight)) / 2;

		final Device device = gc.getDevice();
		if (isLoadingError) {
			gc.setForeground(device.getSystemColor(SWT.COLOR_RED));
		} else {
			if (metaData != null) {
				gc.setForeground(_fgColor);
			} else {
				gc.setForeground(device.getSystemColor(SWT.COLOR_YELLOW));
			}
		}

		gc.setBackground(bgColor);

		gc.drawString(statusText, photoPosX + textOffsetX, photoPosY + textOffsetY, false);

		if (isSelected) {

			// draw marker line on the left side
			gc.setBackground(_selectionFgColor);
			gc.fillRectangle(photoPosX, photoPosY, 2, imageCanvasHeight);
		}
	}

	@Override
	public int getBorderSize() {
		return _gridBorderSize + _imageBorderSize;
	}

	public boolean isAttributesPainted(final int galleryItemSizeWithBorder) {

		final int photoWidth = galleryItemSizeWithBorder - _gridBorderSize;

		int itemImageWidth = photoWidth;

		// ignore border for small images
		final int border = _imageBorderSize;
		final boolean isBorder = itemImageWidth - border >= _textMinThumbSize;

		itemImageWidth -= isBorder ? border : 0;

		// disable drawing photo attributes when image is too small
		_isAttributesPainted = true;
		if (itemImageWidth < _textMinThumbSize) {
			_isAttributesPainted = false;
		}

		return _isAttributesPainted;
	}

	/**
	 * @param galleryItem
	 * @param itemMouseX
	 * @param itemMouseY
	 * @return Returns <code>true</code> when the mouse down event is handled and no further actions
	 *         should be done in the gallery (e.g. no select item).
	 */
	public boolean isMouseDownHandled(final GalleryMT20Item galleryItem, final int itemMouseX, final int itemMouseY) {

		if (_isRatingStarsPainted && _isHandleHoveredRatingStars && isRatingStarsHovered(itemMouseX, itemMouseY)) {
			return saveRatingStars(galleryItem);
		}

		if (_isShowAnnotations && galleryItem.isHoveredAnnotationTour) {

			Photo.getPhotoServiceProvider().openTour(galleryItem.photo.getTourPhotoReferences());

			return true;
		}

		return false;
	}

	/**
	 * @param itemMouseX
	 * @param itemMouseY
	 * @return Returns <code>true</code> when the rating star area in a gallery item is hovered.
	 */
	private boolean isRatingStarsHovered(final int itemMouseX, final int itemMouseY) {

		return itemMouseX >= _ratingStarsLeftBorder//
				//
				&& itemMouseX <= _ratingStarsLeftBorder + MAX_RATING_STARS_WIDTH
				&& itemMouseY <= _ratingStarImageHeight;
	}

	/**
	 * @param hoveredItem
	 * @param itemMouseX
	 * @param itemMouseY
	 * @return
	 */
	public boolean itemIsHovered(final GalleryMT20Item hoveredItem, final int itemMouseX, final int itemMouseY) {

		final boolean isStarsModified = itemIsHovered_Stars(hoveredItem, itemMouseX, itemMouseY);
		final boolean isAnnotationModified = itemIsHovered_Annotation(hoveredItem, itemMouseX, itemMouseY);

		return isStarsModified || isAnnotationModified;
	}

	/**
	 * @param hoveredItem
	 * @param itemMouseX
	 * @param itemMouseY
	 * @return Returns <code>true</code> when the hovered state has changed.
	 */
	private boolean itemIsHovered_Annotation(	final GalleryMT20Item hoveredItem,
												final int itemMouseX,
												final int itemMouseY) {

		final boolean isHoveredBackup = hoveredItem.isHoveredAnnotationTour;

		hoveredItem.isHoveredAnnotationTour = false;

		if (itemMouseY < hoveredItem.itemYAnnotationPainted) {
			return isHoveredBackup == true;
		}

		final int paintedX = hoveredItem.itemXAnnotationPaintedTour;

		if (paintedX != Integer.MIN_VALUE && itemMouseX >= paintedX && itemMouseX <= paintedX + _annotationImageWidth) {
			hoveredItem.isHoveredAnnotationTour = true;
		}

		return isHoveredBackup != hoveredItem.isHoveredAnnotationTour;
	}

	/**
	 * @param hoveredItem
	 * @param itemMouseX
	 * @param itemMouseY
	 * @return Returns <code>true</code> when the hovered state or the selection has changed.
	 */
	private boolean itemIsHovered_Stars(final GalleryMT20Item hoveredItem, final int itemMouseX, final int itemMouseY) {

		if (_isHandleHoveredRatingStars == false || _isAttributesPainted == false) {
			return false;
		}

		final int backupHoveredStars = hoveredItem.hoveredStars;
		final Collection<GalleryMT20Item> backupSelectedItems = hoveredItem.allSelectedGalleryItems;

		int hoveredStars;

		if (isRatingStarsHovered(itemMouseX, itemMouseY)) {

			final int hoveredPhotoX = itemMouseX + _gridBorderSize;

			hoveredStars = (hoveredPhotoX - _ratingStarsLeftBorder) / _ratingStarImageWidth + 1;

		} else {

			hoveredStars = 0;
		}

		boolean isSelectionModified = false;
		final HashMap<String, GalleryMT20Item> selectedItemsMap = _galleryMT.getSelectedItems();

		if (selectedItemsMap.containsKey(hoveredItem.uniqueItemID)) {

			/*
			 * A selected item is hit by the mouse, the star rating is set for all selected items.
			 */

			final Collection<GalleryMT20Item> selectedItems = selectedItemsMap.values();

			for (final GalleryMT20Item item : selectedItems) {
				item.hoveredStars = hoveredStars;
			}

			hoveredItem.allSelectedGalleryItems = selectedItems;

			isSelectionModified = (backupSelectedItems == null && hoveredItem.allSelectedGalleryItems != null)
			//
					|| (backupSelectedItems != null && selectedItems != null && backupSelectedItems.hashCode() != selectedItems
							.hashCode());

		} else {

			/*
			 * An unselected item is hit by the mouse, only for this item the star rating is set
			 */

			hoveredItem.hoveredStars = hoveredStars;
		}

		final boolean isHoveredStarsModified = backupHoveredStars != hoveredItem.hoveredStars;

		return isHoveredStarsModified || isSelectionModified;
	}

	@Override
	public void resetPreviousImage() {

		_fullsizePaintedImage = null;
	}

	/**
	 * Save star rating of the hovered/selected tours
	 */
	private boolean saveRatingStars(final GalleryMT20Item galleryItem) {

		final IPhotoServiceProvider photoServiceProvider = Photo.getPhotoServiceProvider();

		final HashMap<String, GalleryMT20Item> selectedItems = _galleryMT.getSelectedItems();
		final Collection<GalleryMT20Item> selectedItemValues = selectedItems.values();
		final boolean isMultipleSelection = selectedItems.containsKey(galleryItem.uniqueItemID);

		final Photo hoveredPhoto = galleryItem.photo;

		final int hoveredRatingStars = hoveredPhoto.ratingStars;
		int newRatingStars = galleryItem.hoveredStars;

		if (isMultipleSelection
				&& photoServiceProvider.canSaveStarRating(selectedItems.size(), newRatingStars) == false) {
			return false;
		}

		if (newRatingStars == hoveredRatingStars) {

			/**
			 * Feature to remove rating stars:
			 * <p>
			 * When a rating star is hit and this rating is already set in the photo, the rating
			 * stars are removed.
			 */

			newRatingStars = 0;
		}

		final ArrayList<Photo> photos = new ArrayList<Photo>();

		if (isMultipleSelection) {

			/*
			 * A selected item is hit by the mouse, the star rating is set for all selected items.
			 */

			for (final GalleryMT20Item item : selectedItemValues) {

				final Photo photo = item.photo;
				if (photo != null) {

					photo.ratingStars = newRatingStars;

					photos.add(photo);
				}
			}

		} else {

			/*
			 * An unselected item is hit by the mouse, only for this item the star rating is set
			 */

			hoveredPhoto.ratingStars = newRatingStars;

			photos.add(hoveredPhoto);
		}

		if (photos.size() > 0) {

			photoServiceProvider.saveStarRating(photos);

			// update UI with new star rating
			for (final GalleryMT20Item item : selectedItemValues) {

				_galleryMT.redraw(//
						item.viewPortX,
						item.viewPortY,
						item.width,
						item.height,
						false);
			}

			return true;
		}

		return false;
	}

	public void setColors(	final Color fgColor,
							final Color bgColor,
							final Color selectionFgColor,
							final Color noFocusSelectionFgColor) {
		_fgColor = fgColor;
		_bgColor = bgColor;
		_selectionFgColor = selectionFgColor;
		_noFocusSelectionFgColor = noFocusSelectionFgColor;
	}

	public void setFont(final Font font) {

		// force font update
		_fontHeight = -1;

		_galleryMT.setFont(font);
	}

	private void setPaintedZoomFactor(	final int imageWidth,
										final int imageHeight,
										final int canvasWidth,
										final int canvasHeight) {

		final boolean isWidthMax = imageWidth >= imageHeight;

		final int maxImageSize = isWidthMax ? imageWidth : imageHeight;
		final int maxCanvasSize = isWidthMax ? canvasWidth : canvasHeight;

		_imagePaintedZoomFactor = (double) maxCanvasSize / maxImageSize;
	}

	/**
	 * Enables / disables labels at the bottom of each item.
	 * 
	 * @param dateInfo
	 * @param isShowPhotoName
	 * @see DefaultGalleryMT20ItemRenderer#isShowLabels()
	 */
	public void setPhotoInfo(	final boolean isShowPhotoName,
								final PhotoDateInfo dateInfo,
								final boolean isShowAnnotations) {

		_photoDateInfo = dateInfo;

		_isShowDateInfo = _photoDateInfo != PhotoDateInfo.NoDateTime;
		_isShowPhotoName = isShowPhotoName;
		_isShowAnnotations = isShowAnnotations;

	}

	@Override
	public void setPrefSettings(final boolean isShowFullsizePreview,
								final boolean isShowLoadingMessage,
								final boolean isShowHQImage) {

		_isShowFullsizePreview = isShowFullsizePreview;
		_isShowFullsizeLoadingMessage = isShowLoadingMessage;
		_isShowFullsizeHQImage = isShowHQImage;
	}

	public void setShowRatingStars(final RatingStarBehaviour ratingStarBehaviour) {
		_isShowPhotoRatingStars = ratingStarBehaviour != RatingStarBehaviour.NO_STARS;
		_isHandleHoveredRatingStars = ratingStarBehaviour == RatingStarBehaviour.HOVERED_STARS;
	}

	public void setSizeImageBorder(final int imageBorderSize) {
		_imageBorderSize = imageBorderSize;
	}

	public void setSizeTextMinThumb(final int textMinThumbSize) {
		_textMinThumbSize = textMinThumbSize;
	}
}
