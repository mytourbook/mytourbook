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
package net.tourbook.photo.internal;

import java.text.NumberFormat;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.photo.ImageGallery;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;
import net.tourbook.photo.PhotoWrapper;
import net.tourbook.photo.internal.gallery.MT20.AbstractGalleryMT20ItemRenderer;
import net.tourbook.photo.internal.gallery.MT20.DefaultGalleryMT20ItemRenderer;
import net.tourbook.photo.internal.gallery.MT20.GalleryMT20;
import net.tourbook.photo.internal.gallery.MT20.GalleryMT20Item;
import net.tourbook.photo.internal.gallery.MT20.PaintingResult;
import net.tourbook.photo.internal.gallery.MT20.RendererHelper;
import net.tourbook.photo.internal.gallery.MT20.ZoomState;
import net.tourbook.photo.internal.manager.PhotoImageMetadata;

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
import org.joda.time.format.DateTimeFormatterBuilder;

/**
 * Paint image in the gallery canvas.
 * <p>
 * Original: org.sharemedia.utils.gallery.ShareMediaIconRenderer2
 */
public class PhotoRenderer extends AbstractGalleryMT20ItemRenderer {

	private static final String			PHOTO_ANNOTATION_GPS	= "PHOTO_ANNOTATION_GPS";								//$NON-NLS-1$

	/**
	 * this value has been evaluated by some test
	 */
	private int							_textMinThumbSize		= 50;

	private int							_fontHeight				= -1;

//	private final DateTimeFormatter	_dtFormatter					= DateTimeFormat.forStyle("SM");
	private final DateTimeFormatter		_dtFormatterDateTime	= new DateTimeFormatterBuilder()
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

	private final DateTimeFormatter		_dtFormatterTime		= new DateTimeFormatterBuilder()
																		.appendHourOfDay(2)
																		.appendLiteral(':')
																		.appendMinuteOfHour(2)
																		.appendLiteral(':')
																		.appendSecondOfMinute(2)
																		.toFormatter();

	private final DateTimeFormatter		_dtFormatterDate		= new DateTimeFormatterBuilder()
																		.appendYear(4, 4)
																		.appendLiteral('-')
																		.appendMonthOfYear(2)
																		.appendLiteral('-')
																		.appendDayOfMonth(2)
																		.toFormatter();

	private boolean						_isShowPhotoName;
	private boolean						_isShowAnnotations;
	private boolean						_isShowDateInfo;
	private PhotoDateInfo				_photoDateInfo;

	private ImageGallery				_imageGallery;
	private GalleryMT20					_galleryMT;

	private int							_gridBorder				= 1;
	private int							_imageBorder			= 5;

	/**
	 * photo dimension without grid border but including image border
	 */
	private int							_photoX;
	private int							_photoY;
	private int							_photoWidth;
	private int							_photoHeight;

	/*
	 * position and size where the photo image is painted
	 */
	private int							_photoImageWidth;
	private int							_photoImageHeight;
	private int							_paintedDestX;
	private int							_paintedDestY;

	/**
	 * Width for the painted image or <code>-1</code> when not initialized.
	 */
	private int							_paintedDestWidth		= -1;
	private int							_paintedDestHeight;

	private boolean						_isShowFullsizeHQImage;
	private boolean						_isShowFullsizePreview;
	private boolean						_isShowFullsizeLoadingMessage;

	private double						_imagePaintedZoomFactor;

	private int							_paintedImageWidth;
	private int							_paintedImageHeight;

	/*
	 * full size context fields
	 */
	private Image						_fullsizePaintedImage;
	private LoadCallbackOriginalImage	_fullsizeImageLoadCallback;
	private boolean						_isFullsizeImageAvailable;
	private boolean						_isFullsizeLoadingError;

	private final DateTimeFormatter		_dtFormatter			= DateTimeFormat.forStyle("ML");						//$NON-NLS-1$
	private final DateTimeFormatter		_dtWeekday				= DateTimeFormat.forPattern("E");						//$NON-NLS-1$
	private final NumberFormat			_nfMByte				= NumberFormat.getNumberInstance();
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
	private Color						_fullsizeBgColor		= Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);

	private Color						_fgColor				= Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
	private Color						_bgColor				= Display.getCurrent().getSystemColor(SWT.COLOR_RED);
	private Color						_selectionFgColor;
	private Color						_noFocusSelectionFgColor;

	private static Image				_gpsImage;
	private static int					_gpsImageWidth;
	private static int					_gpsImageHeight;

	static {

		UI.IMAGE_REGISTRY.put(PHOTO_ANNOTATION_GPS, Activator.getImageDescriptor(Messages.Image__PhotoAnnotationGPS));

		_gpsImage = UI.IMAGE_REGISTRY.get(PHOTO_ANNOTATION_GPS);

		final Rectangle bounds = _gpsImage.getBounds();
		_gpsImageWidth = bounds.width;
		_gpsImageHeight = bounds.height;
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

		_isFocusActive = isFocusActive;

		// init fontheight
		if (_fontHeight == -1) {
			_fontHeight = gc.getFontMetrics().getHeight();
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
		_paintedDestWidth = itemImageWidth;
		_paintedDestHeight = itemImageHeight;

		final PhotoWrapper photoWrapper = (PhotoWrapper) galleryItem.customData;
		if (photoWrapper == null) {
			// this case should not happen but it did
			return;
		}

		final Photo photo = photoWrapper.photo;

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

			if (itemImageWidth < _textMinThumbSize) {
				isDrawText = false;
			}

			final boolean isPainted = draw_Image(gc, photoWrapper, paintedImage, galleryItem,//
					imageX,
					imageY,
					itemImageWidth,
					itemImageHeight,
					isRequestedQuality,
					isSelected);

			if (isPainted == false) {
				// error occured painting the image, invalidate canvas
			}

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
					false,
					_bgColor);
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
					_paintedDestX + _paintedDestWidth - _gpsImageWidth,
					_paintedDestY + _paintedDestHeight - _gpsImageHeight);
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
	 * @param isFullsizeImage
	 * @return
	 */
	private boolean draw_Image(	final GC gc,
								final PhotoWrapper photoWrapper,
								final Image photoImage,
								final GalleryMT20Item galleryItem,
								final int photoPosX,
								final int photoPosY,
								final int imageCanvasWidth,
								final int imageCanvasHeight,
								final boolean isRequestedQuality,
								final boolean isSelected) {

		final Point bestSize = RendererHelper.getBestSize(
				photoWrapper.photo,
				_paintedImageWidth,
				_paintedImageHeight,
				imageCanvasWidth,
				imageCanvasHeight);

		_paintedDestWidth = bestSize.x;
		_paintedDestHeight = bestSize.y;

		// get center offset
		final int centerOffsetX = (imageCanvasWidth - _paintedDestWidth) / 2;
		final int centerOffsetY = (imageCanvasHeight - _paintedDestHeight) / 2;

		_paintedDestX = photoPosX + centerOffsetX;
		_paintedDestY = photoPosY + centerOffsetY;

		try {

			try {

				gc.drawImage(photoImage, //
						0,
						0,
						_paintedImageWidth,
						_paintedImageHeight,
						//
						_paintedDestX,
						_paintedDestY,
						_paintedDestWidth,
						_paintedDestHeight);

				galleryItem.imagePaintedWidth = _paintedDestWidth;
				galleryItem.imagePaintedHeight = _paintedDestHeight;

			} catch (final Exception e) {

				System.out.println("SWT exception occured when painting valid image " //$NON-NLS-1$
						+ photoWrapper.imageFilePathName
						+ " it's potentially this bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=375845"); //$NON-NLS-1$

				// ensure image is valid after reloading
				photoImage.dispose();

				return false;
			}

			/*
			 * draw selection
			 */
			if (isSelected) {

				// draw marker line on the left side
				gc.setBackground(_isFocusActive ? _selectionFgColor : _noFocusSelectionFgColor);
				gc.fillRectangle(_paintedDestX, _paintedDestY, 2, _paintedDestHeight);
			}

			/*
			 * draw HQ marker
			 */
			if (isRequestedQuality == false) {

				// draw an marker that the requested image quality is not yet painted

				final int markerSize = 9;

				gc.setBackground(_selectionFgColor);
				gc.fillRectangle(//
						_paintedDestX + _paintedDestWidth - markerSize,
						_paintedDestY,
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

	@Override
	public PaintingResult drawFullSize(	final GC gc,
										final GalleryMT20Item galleryItem,
										final int canvasWidth,
										final int canvasHeight,
										final ZoomState zoomState,
										final double zoomFactor) {

		final PhotoWrapper photoWrapper = (PhotoWrapper) galleryItem.customData;
		if (photoWrapper == null) {
			return null;
		}

		final Photo photo = photoWrapper.photo;

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

				isPainted = draw_Image(gc, photoWrapper, paintedImage, galleryItem,//
						0,
						0,
						canvasWidth,
						canvasHeight,
						_isFullsizeImageAvailable,
						false);

				setPaintedZoomFactor(_photoImageWidth, _photoImageHeight, _paintedDestWidth, _paintedDestHeight);

			} else {

				drawFullSize_Image(gc, photoWrapper, paintedImage, galleryItem,//
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

			drawStatusText(gc, photoWrapper, //
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

		final PhotoWrapper photoWrapper = (PhotoWrapper) galleryItem.customData;
		if (photoWrapper == null) {
			return null;
		}

		// show image file name in the shell
		shell.setText(NLS.bind(Messages.App__PhotoShell_Title, photoWrapper.imageFileName));

		final Photo photo = photoWrapper.photo;

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

	/**
	 * @param gc
	 * @param photoWrapper
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
								final PhotoWrapper photoWrapper,
								final int photoPosX,
								final int photoPosY,
								final int imageCanvasWidth,
								final int imageCanvasHeight,
								final ImageQuality requestedImageQuality,
								final boolean isImageNameDisplayed,
								final boolean isSelected,
								final boolean isFullsizeImage,
								final Color bgColor) {

		final Photo photo = photoWrapper.photo;

		final boolean isLoadingError = photo.getLoadingState(requestedImageQuality) == PhotoLoadingState.IMAGE_IS_INVALID;

		if (isFullsizeImage && isLoadingError == false && _isShowFullsizeLoadingMessage == false) {
			return;
		}

		final String photoImageFileName = isImageNameDisplayed ? //
				// don't show file name a 2nd time
				UI.EMPTY_STRING
				: photoWrapper.imageFileName;

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
					final String textSize = _nfMByte.format(photoWrapper.imageFileSize / 1024.0 / 1024.0)
							+ UI.SPACE
							+ UI.UNIT_MBYTES;

					/*
					 * date/time
					 */
					final DateTime dateTime = photoWrapper.photo.getOriginalDateTime();
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
		return _gridBorder + _imageBorder;
	}

	@Override
	public void resetPreviousImage() {

		_fullsizePaintedImage = null;
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

	@Override
	public void setPrefSettings(final boolean isShowFullsizePreview,
								final boolean isShowLoadingMessage,
								final boolean isShowHQImage) {

		_isShowFullsizePreview = isShowFullsizePreview;
		_isShowFullsizeLoadingMessage = isShowLoadingMessage;
		_isShowFullsizeHQImage = isShowHQImage;
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
