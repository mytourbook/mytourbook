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

import net.tourbook.photo.gallery.DefaultGalleryItemRenderer;
import net.tourbook.photo.gallery.GalleryMTItem;
import net.tourbook.photo.gallery.RendererHelper;
import net.tourbook.photo.manager.Photo;
import net.tourbook.photo.manager.PhotoImageCache;
import net.tourbook.photo.manager.PhotoLoadingState;
import net.tourbook.photo.manager.PhotoManager;
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

	private static final int		MIN_PHOTO_IMAGE_HEIGHT	= 20;

	private int						_fontHeight				= -1;

//	private final DateTimeFormatter	_dtFormatter	= DateTimeFormat.forStyle("SM");
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
			_fontHeight = gc.getFontMetrics().getHeight();
		}

		final Object itemData = galleryItem.getData();

		Photo photo = null;
		if (itemData instanceof Photo) {
			photo = (Photo) itemData;
		} else {
			// this case should not happten
			gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
			gc.drawText("error: gallery item is not a photo", //
					galleryPosX + 3,
					galleryPosY + 3 + _fontHeight);
			return;
		}

		final int photoPosX = galleryPosX + 1;
		final int photoPosY = galleryPosY + 0;
		final int photoWidth = galleryItemWidth - 2;
		final int photoHeight = galleryItemHeight - 1;

		int imageCanvasHeight = photoHeight;

		boolean isDrawText = true;
		if (_isShowPhotoName) {
			imageCanvasHeight -= _fontHeight;
		}
		if (_isShowPhotoDate) {
			imageCanvasHeight -= _fontHeight;
		}

//		final int relativePhotoWidth = (int) (imageCanvasHeight * (float) 15 / 11);

//		System.out.println("relativePhotoWidth: " + relativePhotoWidth);
//		// TODO remove SYSTEM.OUT.PRINTLN

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

		gc.setForeground(getForegroundColor());
		gc.setBackground(getBackgroundColor());

		if (photoImage != null && photoImage.isDisposed() == false) {

			/*
			 * draw photo image, when photo height is smaller than min photo height, only the
			 * picture but not the text is displayed
			 */

			int imageHeight = imageCanvasHeight;
			if (imageHeight < MIN_PHOTO_IMAGE_HEIGHT) {
				imageHeight = photoHeight;
				isDrawText = false;
			}

			drawImage(gc, photo, photoImage, photoPosX, photoPosY, photoWidth, imageHeight, isRequestedQuality);

		} else {

			// image is not available

			drawStatusText(gc, photo, photoPosX, photoPosY, photoWidth, imageCanvasHeight, requestedImageQuality);
		}

		// draw name & date
		if (isDrawText && (_isShowPhotoName || _isShowPhotoDate)) {

			int textPosY = photoPosY + imageCanvasHeight;

			gc.setForeground(getForegroundColor());
			gc.setBackground(getBackgroundColor());

			/*
			 * draw 1st line
			 */
			if (_isShowPhotoDate) {

				final DateTime dateTime = photo.getFileDateTime();
				if (dateTime != null) {

					final String textDateTime = _dtFormatter.print(dateTime);

					// Center text
					final int textWidth = gc.textExtent(textDateTime).x;
					final int textOffset = (photoWidth - (textWidth > photoWidth ? photoWidth : textWidth)) / 2;

//					gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
					gc.drawString(textDateTime, photoPosX + textOffset, textPosY, true);

					// advance to next position
					textPosY = textPosY + _fontHeight;
				}
			}

			/*
			 * draw 2nd line
			 */
			if (_isShowPhotoName) {

				// Center text
				final String text = galleryItem.getText();
				final int textWidth = gc.textExtent(text).x;
				final int textOffset = (photoWidth - (textWidth > photoWidth ? photoWidth : textWidth)) / 2;

//				gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_BLUE));
				gc.drawString(text, photoPosX + textOffset, textPosY, true);
			}
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
				gc.fillRectangle(
						destX + photoPaintedWidth - markerSize,
						destY + photoPaintedHeight - markerSize,
						markerSize,
						markerSize);
			}

		} catch (final Exception e) {

			gc.drawString(e.getMessage(), photoPosX, photoPosY);

			final String message = ("srcWidth: " + imageWidth)
					+ ("  srcHeight:" + imageHeight)
					+ ("  destX:" + destX)
					+ ("  destY:" + destY)
					+ ("  destWidth: " + photoPaintedWidth)
					+ ("  destHeight :" + photoPaintedHeight)
					+ ("  " + photo);

			StatusUtil.log(message, e);
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
	public void setShowLabels(final boolean isShowPhotoName, final boolean isShowPhotoDate) {
		_isShowPhotoName = isShowPhotoName;
		_isShowPhotoDate = isShowPhotoDate;
	}
}
