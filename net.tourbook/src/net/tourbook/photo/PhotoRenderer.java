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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.photo.PicDirImages.LoadCallbackImage;
import net.tourbook.photo.PicDirImages.LoadCallbackOriginalImage;
import net.tourbook.photo.gallery.MT20.AbstractGalleryMT20ItemRenderer;
import net.tourbook.photo.gallery.MT20.DefaultGalleryMT20ItemRenderer;
import net.tourbook.photo.gallery.MT20.GalleryMT20;
import net.tourbook.photo.gallery.MT20.GalleryMT20Item;
import net.tourbook.photo.gallery.MT20.PaintingResult;
import net.tourbook.photo.gallery.MT20.RendererHelper;
import net.tourbook.photo.gallery.MT20.ZoomState;
import net.tourbook.photo.manager.ImageQuality;
import net.tourbook.photo.manager.Photo;
import net.tourbook.photo.manager.PhotoImageCache;
import net.tourbook.photo.manager.PhotoImageMetadata;
import net.tourbook.photo.manager.PhotoLoadManager;
import net.tourbook.photo.manager.PhotoLoadingState;
import net.tourbook.photo.manager.PhotoWrapper;
import net.tourbook.ui.UI;
import net.tourbook.util.StatusUtil;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

/**
 * Paint image in the gallery canvas.
 * <p>
 * Original: org.sharemedia.utils.gallery.ShareMediaIconRenderer2
 */
public class PhotoRenderer extends AbstractGalleryMT20ItemRenderer {

	private static final String		PHOTO_ANNOTATION_GPS			= "PHOTO_ANNOTATION_GPS";	//$NON-NLS-1$

	/**
	 * this value has been evaluated by some test
	 */
	private int						_textMinThumbSize				= 50;

	private int						_fontHeight						= -1;

	private Color					_fgColor;
	private Color					_bgColor;
	private Color					_selectionFgColor;

//	private final DateTimeFormatter	_dtFormatter					= DateTimeFormat.forStyle("SM");
	private final DateTimeFormatter	_dtFormatterDateTime			= new DateTimeFormatterBuilder()
																			.appendYear(4, 4)
																			.appendLiteral('-')
																			.appendMonthOfYear(2)
																			.appendLiteral('-')
																			.appendDayOfMonth(2)
																			.appendLiteral(' ')
																			.appendHourOfDay(2)
																			.appendLiteral(':')
																			.appendMinuteOfHour(2)
																			.appendLiteral(':')
																			.appendSecondOfMinute(2)
																			.toFormatter();

	private final DateTimeFormatter	_dtFormatterTime				= new DateTimeFormatterBuilder()
																			.appendHourOfDay(2)
																			.appendLiteral(':')
																			.appendMinuteOfHour(2)
																			.appendLiteral(':')
																			.appendSecondOfMinute(2)
																			.toFormatter();

	private final DateTimeFormatter	_dtFormatterDate				= new DateTimeFormatterBuilder()
																			.appendYear(4, 4)
																			.appendLiteral('-')
																			.appendMonthOfYear(2)
																			.appendLiteral('-')
																			.appendDayOfMonth(2)
																			.toFormatter();

	private boolean					_isShowPhotoName;
	private boolean					_isShowAnnotations;
	private boolean					_isShowDateInfo;
	private PhotoDateInfo			_photoDateInfo;

	private GalleryMT20				_gallery;
	private PicDirImages			_picDirImages;

	private int						_gridBorder						= 1;
	private int						_imageBorder					= 5;

	/**
	 * photo dimension without grid border but including image border
	 */
	private int						_photoX;
	private int						_photoY;
	private int						_photoWidth;
	private int						_photoHeight;

	/*
	 * position and size where the photo image is painted
	 */
	private int						_imageWidth;
	private int						_imageHeight;
	private int						_imagePaintedX;
	private int						_imagePaintedY;

	/**
	 * Width for the painted image or <code>-1</code> when not initialized.
	 */
	private int						_imagePaintedWidth				= -1;
	private int						_imagePaintedHeight;

	private boolean					_isShowFullsizeLoadingMessage	= true;

	private double					_imagePaintedZoomFactor;

	private Image					_prevImage;

	private static Image			_gpsImage;
	private static int				_gpsImageWidth;
	private static int				_gpsImageHeight;

	static {

		UI.IMAGE_REGISTRY.put(
				PHOTO_ANNOTATION_GPS,
				TourbookPlugin.getImageDescriptor(Messages.Image__PhotoAnnotationGPS));

		_gpsImage = UI.IMAGE_REGISTRY.get(PHOTO_ANNOTATION_GPS);

		final Rectangle bounds = _gpsImage.getBounds();
		_gpsImageWidth = bounds.width;
		_gpsImageHeight = bounds.height;
	}

	public PhotoRenderer(final GalleryMT20 galleryMT20, final PicDirImages picDirImages) {
		_gallery = galleryMT20;
		_picDirImages = picDirImages;
	}

	/**
	 * Compute image size for the canvas size.
	 * 
	 * @param photoWrapper
	 * @param imageCanvasWidth
	 * @param imageCanvasHeight
	 * @return
	 */
	private Point computeBestSize(final Photo photo, final int imageCanvasWidth, final int imageCanvasHeight) {

		final Point bestSize = RendererHelper.getBestSize(
				_imageWidth,
				_imageHeight,
				imageCanvasWidth,
				imageCanvasHeight);

		int imagePaintedWidth = bestSize.x;
		int imagePaintedHeight = bestSize.y;

		int photoWidthRotated = photo.getWidthRotated();
		int photoHeightRotated = photo.getHeightRotated();

		if (photoWidthRotated == Integer.MIN_VALUE) {
			photoWidthRotated = photo.getWidth();
			photoHeightRotated = photo.getHeight();
		}

		/*
		 * the photo image should not be displayed larger than the original photo even when the
		 * thumb image is larger, this can happen when image is resized
		 */
		if (photoWidthRotated != Integer.MIN_VALUE && photoHeightRotated != Integer.MIN_VALUE) {

			// photo is loaded

			if (imagePaintedWidth > photoWidthRotated || imagePaintedHeight > photoHeightRotated) {

				imagePaintedWidth = photoWidthRotated;
				imagePaintedHeight = photoHeightRotated;
			}
		} else if (imagePaintedWidth > _imageWidth || imagePaintedHeight > _imageHeight) {

			imagePaintedWidth = _imageWidth;
			imagePaintedHeight = _imageHeight;
		}

		return new Point(imagePaintedWidth, imagePaintedHeight);
	}

	@Override
	public void draw(	final GC gc,
						final GalleryMT20Item galleryItem,
						final int galleryItemViewPortX,
						final int galleryItemViewPortY,
						final int galleryItemWidth,
						final int galleryItemHeight,
						final boolean isSelected) {

		// init fontheight
		if (_fontHeight == -1) {
			_fontHeight = gc.getFontMetrics().getHeight() - 1;
		}

		boolean isDrawText = true;

		_photoX = galleryItemViewPortX + _gridBorder;
		_photoY = galleryItemViewPortY + _gridBorder;
		_photoWidth = galleryItemWidth - _gridBorder;
		_photoHeight = galleryItemHeight - _gridBorder;

		int itemImageWidth = _photoWidth;
		int itemImageHeight = _photoHeight;

		// ignore border for small images
		final boolean isBorder = itemImageWidth - _imageBorder >= _textMinThumbSize;
		final int border = _imageBorder;
		final int border2 = border / 2;

		final int imageX = _photoX + (isBorder ? border2 : 0);
		final int imageY = _photoY + (isBorder ? border2 : 0);

		itemImageWidth -= isBorder ? border : 0;
		itemImageHeight -= isBorder ? border : 0;
		_imagePaintedWidth = itemImageWidth;
		_imagePaintedHeight = itemImageHeight;

		final PhotoWrapper photoWrapper = (PhotoWrapper) galleryItem.customData;
		if (photoWrapper == null) {
			// this case should not happen but it did
			return;
		}

		final Photo photo = photoWrapper.photo;

		final ImageQuality requestedImageQuality = itemImageWidth <= PhotoLoadManager.IMAGE_SIZE_THUMBNAIL
				? ImageQuality.THUMB
				: ImageQuality.HQ;

		Image photoImage = null;
		boolean isRequestedQuality = false;

		// check if image has an loading error
		final PhotoLoadingState photoLoadingState = photo.getLoadingState(requestedImageQuality);

		if (photoLoadingState != PhotoLoadingState.IMAGE_HAS_A_LOADING_ERROR) {

			// image is not yet loaded

			// check if image is in the cache
			photoImage = PhotoImageCache.getImage(photo, requestedImageQuality);

			if ((photoImage == null || photoImage.isDisposed())
					&& photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

				// the requested image is not available in the image cache -> image must be loaded

				final LoadCallbackImage imageLoadCallback = _picDirImages.new LoadCallbackImage(galleryItem);

				PhotoLoadManager.putImageInLoadingQueueThumb(
						galleryItem,
						photo,
						requestedImageQuality,
						imageLoadCallback);
			}

			isRequestedQuality = true;

			if (photoImage == null || photoImage.isDisposed()) {

				// requested size is not available, try to get image with lower quality

				isRequestedQuality = false;

				final ImageQuality lowerImageQuality = galleryItemWidth > PhotoLoadManager.IMAGE_SIZE_THUMBNAIL
						? ImageQuality.THUMB
						: ImageQuality.HQ;

				photoImage = PhotoImageCache.getImage(photo, lowerImageQuality);
			}
		}

		gc.setForeground(_fgColor);
		gc.setBackground(_bgColor);

		if (photoImage != null && photoImage.isDisposed() == false) {

			/*
			 * draw photo image, when photo height is smaller than min photo height, only the
			 * picture but not the text is displayed
			 */

			/*
			 * an exception can occure because the image could be disposed before it is drawn
			 */
			try {

				final Rectangle imageBounds = photoImage.getBounds();
				_imageWidth = imageBounds.width;
				_imageHeight = imageBounds.height;
			} catch (final Exception e1) {
				StatusUtil.log(e1);
			}

			if (itemImageWidth < _textMinThumbSize) {
				isDrawText = false;
			}

			draw_Image(gc, photoWrapper, photoImage, galleryItem,//
					imageX,
					imageY,
					itemImageWidth,
					itemImageHeight,
					isRequestedQuality,
					isSelected);

			// debug box for the image area
//			gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
//			gc.drawRectangle(imageX, imageY, imageWidth - 2, imageHeight - 1);

		} else {

			// image is not available

			drawStatusText(gc, photoWrapper, //
					imageX,
					imageY,
					itemImageWidth,
					itemImageHeight,
					requestedImageQuality,
					isDrawText && _isShowPhotoName,
					isSelected,
					false);
		}

		// draw name & date & annotations
		if (isDrawText && (_isShowPhotoName || _isShowDateInfo)) {
			drawPhotoDateName(gc, photoWrapper, //
					imageX,
					imageY,
					itemImageWidth,
					itemImageHeight);
		}

		// annotations are drawn in the bottom right corner of the image
		if (_isShowAnnotations && photoWrapper.gpsState == 1) {
			gc.drawImage(_gpsImage, //
					_imagePaintedX + _imagePaintedWidth - _gpsImageWidth,
					_imagePaintedY + _imagePaintedHeight - _gpsImageHeight);
		}

//		// debug box for the whole gallery item area
//		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GREEN));
//		gc.drawRectangle(photoViewPortX - 1, photoViewPortY, galleryItemWidth - 1, galleryItemHeight - 1);
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
	 */
	private void draw_Image(final GC gc,
							final PhotoWrapper photoWrapper,
							final Image photoImage,
							final GalleryMT20Item galleryItem,
							final int photoPosX,
							final int photoPosY,
							final int imageCanvasWidth,
							final int imageCanvasHeight,
							final boolean isRequestedQuality,
							final boolean isSelected) {

		final Point bestSize = computeBestSize(photoWrapper.photo, imageCanvasWidth, imageCanvasHeight);

		_imagePaintedWidth = bestSize.x;
		_imagePaintedHeight = bestSize.y;

		int centerOffsetX = 0;
		int centerOffsetY = 0;

		// Draw image
		if (_imagePaintedWidth > 0 && _imagePaintedHeight > 0) {

			// center image
			centerOffsetX = (imageCanvasWidth - _imagePaintedWidth) / 2;
			centerOffsetY = (imageCanvasHeight - _imagePaintedHeight) / 2;

		}

		_imagePaintedX = photoPosX + centerOffsetX;
		_imagePaintedY = photoPosY + centerOffsetY;

		try {

			try {

				gc.drawImage(photoImage, //
						0,
						0,
						_imageWidth,
						_imageHeight,
						//
						_imagePaintedX,
						_imagePaintedY,
						_imagePaintedWidth,
						_imagePaintedHeight);

				galleryItem.imagePaintedWidth = _imagePaintedWidth;
				galleryItem.imagePaintedHeight = _imagePaintedHeight;

			} catch (final Exception e) {
				// this bug is covered here: https://bugs.eclipse.org/bugs/show_bug.cgi?id=375845
			}

			if (isSelected) {

				// draw marker line on the left side
				gc.setBackground(_selectionFgColor);
				gc.fillRectangle(_imagePaintedX, _imagePaintedY, 2, _imagePaintedHeight);
			}

			if (isRequestedQuality == false) {

				// draw an marker that the requested image quality is not yet painted

				final int markerSize = 9;

				gc.setBackground(_selectionFgColor);
				gc.fillRectangle(//
						_imagePaintedX + _imagePaintedWidth - markerSize,
						_imagePaintedY,
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
	}

	@Override
	public PaintingResult drawFullSize(	final GC gc,
										final GalleryMT20Item galleryItem,
										final int canvasWidth,
										final int canvasHeight,
										final ZoomState zoomState,
										final double zoomFactor) {

//		System.out.println("zoom " + zoomFactor);
//		// TODO remove SYSTEM.OUT.PRINTLN

		final PhotoWrapper photoWrapper = (PhotoWrapper) galleryItem.customData;
		if (photoWrapper == null) {
			return null;
		}

		final Photo photo = photoWrapper.photo;

		final ImageQuality requestedImageQuality = ImageQuality.ORIGINAL;

		final boolean isLoadingError = photo.getLoadingState(requestedImageQuality) == PhotoLoadingState.IMAGE_HAS_A_LOADING_ERROR;
		boolean isRequestedQuality = false;
		boolean isThumbImage = false;
		boolean isImageAvailable = false;

		Image photoImage = null;

		// check if image has an loading error
		final PhotoLoadingState photoLoadingState = photo.getLoadingState(requestedImageQuality);

		if (photoLoadingState != PhotoLoadingState.IMAGE_HAS_A_LOADING_ERROR) {

			// image is not yet loaded

			// check if image is in the cache
			photoImage = PhotoImageCache.getImageOriginal(photo);
			isRequestedQuality = true;

			if ((photoImage == null || photoImage.isDisposed())
					&& photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

				// the requested image is not available in the image cache -> image must be loaded

				isRequestedQuality = false;

				final LoadCallbackOriginalImage imageLoadCallback = _picDirImages.new LoadCallbackOriginalImage(
						galleryItem,
						photo);

				PhotoLoadManager.putImageInLoadingQueueOriginal(galleryItem, photo, imageLoadCallback);
			}

			if (photoImage == null || photoImage.isDisposed()) {

				// requested size is not available, try to get image with lower quality

				isRequestedQuality = false;
				isThumbImage = true;

				photoImage = PhotoImageCache.getImage(photo, ImageQuality.HQ);
			}

			if (photoImage == null || photoImage.isDisposed()) {

				// requested size is not available, try to get image with lower quality

				isRequestedQuality = false;
				isThumbImage = true;

				photoImage = PhotoImageCache.getImage(photo, ImageQuality.THUMB);
			}
		}

		gc.setForeground(_fgColor);

		isImageAvailable = photoImage != null && photoImage.isDisposed() == false;
		boolean isCorrectImageAvailable = isImageAvailable;

		/*
		 * Linux draws always a background, even when it should not, try to draw the previous image
		 */
		if (UI.IS_LINUX && isCorrectImageAvailable == false && _prevImage != null && _prevImage.isDisposed() == false) {
			photoImage = _prevImage;
			isImageAvailable = true;
		}

		if (isCorrectImageAvailable) {
			_prevImage = photoImage;
		}

		/*
		 * paint background only when an image is availabe or when image could not be loaded to show
		 * an error message without image
		 */
		if (isImageAvailable || isLoadingError) {
			gc.setBackground(_bgColor);
			gc.fillRectangle(0, 0, canvasWidth, canvasHeight);
		}

		/*
		 * paint image
		 */
		if (isImageAvailable) {

			/*
			 * an exception can occure because the image could be disposed before it is drawn
			 */
			try {

				final Rectangle imageBounds = photoImage.getBounds();
				_imageWidth = imageBounds.width;
				_imageHeight = imageBounds.height;
			} catch (final Exception e1) {
				StatusUtil.log(e1);
			}

			if (zoomState == ZoomState.FIT_WINDOW || zoomFactor == 0.0) {

				draw_Image(gc, photoWrapper, photoImage, galleryItem,//
						0,
						0,
						canvasWidth,
						canvasHeight,
						isRequestedQuality,
						false);

				setPaintedZoomFactor(_imageWidth, _imageHeight, _imagePaintedWidth, _imagePaintedHeight);

			} else {

				drawFullSize_Image(gc, photoWrapper, photoImage, galleryItem,//
						canvasWidth,
						canvasHeight,
						zoomState,
						zoomFactor,
						isRequestedQuality);
			}
		}

		/*
		 * draw status message
		 */
		if (isThumbImage || isCorrectImageAvailable == false) {

			// image is not available or thumb image is displayed

			final double textPosition = 0.95;

			drawStatusText(gc, photoWrapper, //
					0,
					(int) (canvasHeight * textPosition), // y pos
					canvasWidth,
					(int) (canvasHeight * (1 - textPosition)), // height
					requestedImageQuality,
					false,
					false,
					true);
		}

		final PaintingResult paintingResult = new PaintingResult();

		paintingResult.imagePaintedZoomFactor = _imagePaintedZoomFactor;

		return paintingResult;
	}

	private void drawFullSize_Image(final GC gc,
									final PhotoWrapper photoWrapper,
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
			final int srcWidth = _imageWidth;
			final int srcHeight = _imageHeight;

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

	private void drawPhotoDateName(	final GC gc,
									final PhotoWrapper photoWrapper,
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
			textFileName = photoWrapper.imageFileName;
			textFileNameWidth = gc.textExtent(textFileName).x;

			textFileNamePosCenterX = (photoWidth - (textFileNameWidth > photoWidth ? photoWidth : textFileNameWidth)) / 2;
		}

		if (_isShowDateInfo) {
			final DateTime dateTime = photoWrapper.photo.getOriginalDateTime();
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

	private void drawStatusText(final GC gc,
								final PhotoWrapper photoWrapper,
								final int photoPosX,
								final int photoPosY,
								final int imageCanvasWidth,
								final int imageCanvasHeight,
								final ImageQuality requestedImageQuality,
								final boolean isImageNameDisplayed,
								final boolean isSelected,
								final boolean isFullsizeImage) {

		final Photo photo = photoWrapper.photo;

		final boolean isError = photo.getLoadingState(requestedImageQuality) == PhotoLoadingState.IMAGE_HAS_A_LOADING_ERROR;

		if (isFullsizeImage && isError == false && _isShowFullsizeLoadingMessage == false) {
			return;
		}

		final String photoImageFileName = isImageNameDisplayed ? //
				// don't show file name a 2nd time
				UI.EMPTY_STRING
				: photoWrapper.imageFileName;

		String statusText;
		PhotoImageMetadata metaData = null;

		if (isError) {
			statusText = NLS.bind(Messages.Pic_Dir_StatusLabel_LoadingFailed, photoImageFileName);
		} else {

			final int exifThumbImageState = photo.getExifThumbImageState();
			metaData = photo.getImageMetaDataRaw();

			if (metaData == null || exifThumbImageState == -1) {
				statusText = NLS.bind(Messages.Pic_Dir_StatusLabel_LoadingThumbExif, photoImageFileName);
			} else {
				// meta data are already loaded
				statusText = NLS.bind(Messages.Pic_Dir_StatusLabel_LoadingFullsize, photoImageFileName);
			}
		}

		final int textWidth = gc.textExtent(statusText).x;

		// Center text
		final int textOffsetX = (imageCanvasWidth - (textWidth > imageCanvasWidth ? imageCanvasWidth : textWidth)) / 2;
		final int textOffsetY = (imageCanvasHeight - (_fontHeight > imageCanvasHeight ? imageCanvasHeight : _fontHeight)) / 2;

		final Device device = gc.getDevice();
		if (isError) {
			gc.setForeground(device.getSystemColor(SWT.COLOR_RED));
		} else {
			if (metaData != null) {
				gc.setForeground(_fgColor);
			} else {
				gc.setForeground(device.getSystemColor(SWT.COLOR_YELLOW));
			}
		}

		gc.setBackground(_bgColor);

		gc.drawString(statusText, photoPosX + textOffsetX, photoPosY + textOffsetY, false);

		if (isSelected) {

			// draw marker line on the left side
			gc.setBackground(_selectionFgColor);
			gc.fillRectangle(photoPosX, photoPosY, 2, imageCanvasHeight);
		}
	}

	@Override
	public int getBorderSize() {
		return _gridBorder + _imageBorder;
	}

	public void setColors(final Color fgColor, final Color bgColor, final Color selectionFgColor) {
		_fgColor = fgColor;
		_bgColor = bgColor;
		_selectionFgColor = selectionFgColor;
	}

	public void setFont(final Font font) {

		// force font update
		_fontHeight = -1;

		_gallery.setFont(font);
	}

	public void setImageBorderSize(final int imageBorderSize) {
		_imageBorder = imageBorderSize;
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
	public void setShowLabels(	final boolean isShowPhotoName,
								final PhotoDateInfo dateInfo,
								final boolean isShowAnnotations) {

		_photoDateInfo = dateInfo;

		_isShowDateInfo = _photoDateInfo != PhotoDateInfo.NoDateTime;
		_isShowPhotoName = isShowPhotoName;
		_isShowAnnotations = isShowAnnotations;

	}

	public void setTextMinThumbSize(final int textMinThumbSize) {
		_textMinThumbSize = textMinThumbSize;
	}
}
