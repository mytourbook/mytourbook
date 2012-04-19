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
import net.tourbook.photo.gallery.DefaultGalleryItemRenderer;
import net.tourbook.photo.gallery.GalleryMTItem;
import net.tourbook.photo.gallery.RendererHelper;
import net.tourbook.photo.manager.Photo;
import net.tourbook.photo.manager.PhotoImageCache;
import net.tourbook.photo.manager.PhotoLoadingState;
import net.tourbook.photo.manager.PhotoManager;
import net.tourbook.ui.UI;
import net.tourbook.util.StatusUtil;

import org.eclipse.swt.SWT;
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
public class PhotoRenderer extends DefaultGalleryItemRenderer {

	/**
	 * this value has been evaluated by some test
	 */
	private static final int		MIN_PHOTO_IMAGE_HEIGHT	= 50;

	private static final String		PHOTO_ANNOTATION_GPS	= "PHOTO_ANNOTATION_GPS";	//$NON-NLS-1$

	private int						_fontHeight				= -1;

//	private final DateTimeFormatter	_dtFormatter			= DateTimeFormat.forStyle("SM");
	private final DateTimeFormatter	_dtFormatter			= new DateTimeFormatterBuilder()
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

	private boolean					_isShowPhotoDate;
	private boolean					_isShowPhotoName;
	private boolean					_isShowAnnotations;

	private static Image			_annotationGPS;
	private static int				_gpsWidth;
	private static int				_gpsHeight;

//	photo-annotation-gps

	static {
		UI.IMAGE_REGISTRY.put(
				PHOTO_ANNOTATION_GPS,
				TourbookPlugin.getImageDescriptor(Messages.Image__PhotoAnnotationGPS));

		_annotationGPS = UI.IMAGE_REGISTRY.get(PHOTO_ANNOTATION_GPS);

		final Rectangle bounds = _annotationGPS.getBounds();
		_gpsWidth = bounds.width;
		_gpsHeight = bounds.height;
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public void draw(	final GC gc,
						final GalleryMTItem galleryItem,
						final int index,
						final int galleryPosX,
						final int galleryPosY,
						final int galleryItemWidth,
						final int galleryItemHeight) {

		if (_fontHeight == -1) {
			_fontHeight = gc.getFontMetrics().getHeight() - 1;
		}

		final Object itemData = galleryItem.getData();

		Photo photo = null;
		if (itemData instanceof Photo) {
			photo = (Photo) itemData;
		} else {
			// this case should not happten
			gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
			gc.drawText("error: gallery item is not a photo", // //$NON-NLS-1$
					galleryPosX + 3,
					galleryPosY + 3 + _fontHeight);
			return;
		}

		final int photoPosX = galleryPosX + 1;
		final int photoPosY = galleryPosY + 0;
		final int photoWidth = galleryItemWidth - 2;
		final int photoHeight = galleryItemHeight - 1;

		final int requestedImageQuality = galleryItemWidth > PhotoManager.IMAGE_SIZE_THUMBNAIL
				? PhotoManager.IMAGE_QUALITY_LARGE_IMAGE
				: PhotoManager.IMAGE_QUALITY_EXIF_THUMB;

		boolean isRequestedQuality = true;

		// get image with requested size
		Image photoImage = PhotoImageCache.getImage(photo, requestedImageQuality);

		if (photoImage == null) {

			// requested size is not available

			isRequestedQuality = false;

			final int lowerImageQuality = galleryItemWidth > PhotoManager.IMAGE_SIZE_THUMBNAIL
					? PhotoManager.IMAGE_QUALITY_EXIF_THUMB
					: PhotoManager.IMAGE_QUALITY_LARGE_IMAGE;

			photoImage = PhotoImageCache.getImage(photo, lowerImageQuality);
		}

		boolean isDrawText = true;

		gc.setForeground(getForegroundColor());
		gc.setBackground(getBackgroundColor());

		if (photoImage != null && photoImage.isDisposed() == false) {

			/*
			 * draw photo image, when photo height is smaller than min photo height, only the
			 * picture but not the text is displayed
			 */

			if (photoHeight < MIN_PHOTO_IMAGE_HEIGHT) {
				isDrawText = false;
			}

			drawImage(gc, photo, photoImage, photoPosX, photoPosY, photoWidth, photoHeight, isRequestedQuality);

		} else {

			// image is not available

			drawStatusText(gc, photo, photoPosX, photoPosY, photoWidth, photoHeight, requestedImageQuality);
		}

		// draw name & date & annotations
		if (isDrawText && (_isShowPhotoName || _isShowPhotoDate)) {
			drawInfoText(gc, photo, photoPosX, photoPosY, photoWidth, photoHeight);
		}

		// annotations are drawn on top of the info lines
//		if (isDrawText && _isShowAnnotations && photo.getGeoPosition() != null) {
		if (_isShowAnnotations && photo.getGeoPosition() != null) {
			gc.drawImage(_annotationGPS, //
					photoPosX + photoWidth - _gpsWidth,
					photoPosY + photoHeight - _gpsHeight);
		}

		// debug box for the whole gallery item area
//		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GREEN));
//		gc.drawRectangle(galleryPosX, galleryPosY, galleryItemWidth - 1, galleryItemHeight - 1);
	}

	/**
	 * Draw photo image centered in the photo canvas.
	 * 
	 * @param gc
	 * @param photo
	 * @param photoImage
	 * @param photoPosX
	 * @param photoPosY
	 * @param photoWidth
	 * @param photoHeight
	 * @param isRequestedQuality
	 */
	private void drawImage(	final GC gc,
							final Photo photo,
							final Image photoImage,
							final int photoPosX,
							final int photoPosY,
							final int photoWidth,
							final int photoHeight,
							final boolean isRequestedQuality) {

		int photoPaintedWidth = 0;
		int photoPaintedHeight = 0;
		int offsetX = 0;
		int offsetY = 0;
		int imageWidth = 0;
		int imageHeight = 0;

		/*
		 * exception can occure because the image could be disposed before it is drawn
		 */
		try {

			final Rectangle imageBounds = photoImage.getBounds();
			imageWidth = imageBounds.width;
			imageHeight = imageBounds.height;

			final int imageBorderWidth = 0;

			final int imageCanvasWidth = photoWidth - imageBorderWidth;
			final int imageCanvasHeight = photoHeight - imageBorderWidth;

			final Point bestSize = RendererHelper.getBestSize(
					imageWidth,
					imageHeight,
					imageCanvasWidth,
					imageCanvasHeight);

			photoPaintedWidth = bestSize.x;
			photoPaintedHeight = bestSize.y;

			final int photoWidthRotated = photo.getWidthRotated();
			final int photoHeightRotated = photo.getHeightRotated();

			/*
			 * photo image should not be displayed larger than the original photo even when the
			 * thumb image is larger, this can happen when image is resized
			 */
			if (photoWidthRotated != Integer.MIN_VALUE && photoHeightRotated != Integer.MIN_VALUE) {

				// photo is loaded

				if (photoPaintedWidth > photoWidthRotated || photoPaintedHeight > photoHeightRotated) {

					photoPaintedWidth = photoWidthRotated;
					photoPaintedHeight = photoHeightRotated;
				}
			} else if (photoPaintedWidth > imageWidth || photoPaintedHeight > imageHeight) {

				photoPaintedWidth = imageWidth;
				photoPaintedHeight = imageHeight;
			}

			// Draw image
			if (photoPaintedWidth > 0 && photoPaintedHeight > 0) {

				// center image
				offsetX = (photoWidth - photoPaintedWidth) / 2;
				offsetY = (photoHeight - photoPaintedHeight) / 2;

			}
		} catch (final Exception e) {
			StatusUtil.log(e);
		}

		int destX = 0;
		int destY = 0;
		try {

			destX = photoPosX + offsetX;
			destY = photoPosY + offsetY;

			gc.drawImage(photoImage, //
					0,
					0,
					imageWidth,
					imageHeight,
					//
					destX,
					destY,
					photoPaintedWidth,
					photoPaintedHeight);

			if (selected) {

				// draw marker line on the left side
				gc.setBackground(getSelectionForegroundColor());
				gc.fillRectangle(destX, destY, 2, photoPaintedHeight);
			}

			if (isRequestedQuality == false) {

				// draw an marker that the requested image quality is not yet painted

				final int markerSize = 9;

				gc.setBackground(getSelectionForegroundColor());
				gc.fillRectangle(//
						destX + photoPaintedWidth - markerSize,
						destY,// + photoPaintedHeight - markerSize,
						markerSize,
						markerSize);
			}

		} catch (final Exception e) {

			gc.drawString(e.getMessage(), photoPosX, photoPosY);

			final String message = ("srcWidth: " + imageWidth) //$NON-NLS-1$
					+ ("  srcHeight:" + imageHeight) //$NON-NLS-1$
					+ ("  destX:" + destX) //$NON-NLS-1$
					+ ("  destY:" + destY) //$NON-NLS-1$
					+ ("  destWidth: " + photoPaintedWidth) //$NON-NLS-1$
					+ ("  destHeight :" + photoPaintedHeight) //$NON-NLS-1$
					+ ("  " + photo); //$NON-NLS-1$

			StatusUtil.log(message, e);
		}
	}

	private void drawInfoText(	final GC gc,
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
			textFileName = photo.getFileName();
			textFileNameWidth = gc.textExtent(textFileName).x;

			textFileNamePosCenterX = (photoWidth - (textFileNameWidth > photoWidth ? photoWidth : textFileNameWidth)) / 2;
		}
		if (_isShowPhotoDate) {
			final DateTime dateTime = photo.getFileDateTime();
			if (dateTime != null) {
				textDateTime = _dtFormatter.print(dateTime);
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
		gc.setForeground(getForegroundColor());
		gc.setBackground(getBackgroundColor());

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
								final Photo photo,
								final int photoPosX,
								final int photoPosY,
								final int photoWidth,
								final int imageCanvasHeight,
								final int requestedImageQuality) {

		final PhotoLoadingState photoLoadingState = photo.getLoadingState(requestedImageQuality);
		final boolean isError = photoLoadingState == PhotoLoadingState.IMAGE_HAS_A_LOADING_ERROR;
		final String statusText = isError //
				? photo.getFileName() + " cannot be loaded"
				: photo.getFileName() + " is being loaded";
		;

		// Center text
		final int textWidth = gc.textExtent(statusText).x;
		final int textOffsetX = (photoWidth - (textWidth > photoWidth ? photoWidth : textWidth)) / 2;
		final int textOffsetY = (imageCanvasHeight - (_fontHeight > imageCanvasHeight ? imageCanvasHeight : _fontHeight)) / 2;

		final Device device = gc.getDevice();
		if (isError) {
			gc.setForeground(device.getSystemColor(SWT.COLOR_RED));
		} else {
			gc.setForeground(device.getSystemColor(SWT.COLOR_YELLOW));
		}

		gc.drawString(statusText, photoPosX + textOffsetX, photoPosY + textOffsetY, true);
	}

	@Override
	public void setFont(final Font font) {

		// force font update
		_fontHeight = -1;

		super.setFont(font);
	}

	/**
	 * Enables / disables labels at the bottom of each item.
	 * 
	 * @param isShowPhotoDate
	 * @param isShowPhotoName
	 * @see DefaultGalleryItemRenderer#isShowLabels()
	 */
	public void setShowLabels(	final boolean isShowPhotoName,
								final boolean isShowPhotoDate,
								final boolean isShowAnnotations) {
		_isShowPhotoName = isShowPhotoName;
		_isShowPhotoDate = isShowPhotoDate;
		_isShowAnnotations = isShowAnnotations;
	}
}
