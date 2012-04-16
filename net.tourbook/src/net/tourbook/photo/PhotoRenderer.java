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
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Paint image in the gallery canvas.
 * <p>
 * Original: org.sharemedia.utils.gallery.ShareMediaIconRenderer2
 */
public class PhotoRenderer extends DefaultGalleryItemRenderer {

	private int						_fontHeight		= -1;

	private final DateTimeFormatter	_dtFormatter	= DateTimeFormat.mediumDateTime();

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

		final int photoPosX = galleryPosX + 2;
		final int photoPosY = galleryPosY + 2;
		final int photoWidth = galleryItemWidth - 4;
		final int photoHeight = galleryItemHeight - 4;

		final int requestedImageQuality = photoHeight > PhotoManager.IMAGE_SIZE_THUMBNAIL
				? PhotoManager.IMAGE_QUALITY_LARGE_IMAGE
				: PhotoManager.IMAGE_QUALITY_EXIF_THUMB;

		boolean isRequestedQuality = true;

		// get image with requested size
		Image photoImage = PhotoImageCache.getImage(photo, requestedImageQuality);

		if (photoImage == null) {

			// requested size is not available

			isRequestedQuality = false;

			final int lowerImageQuality = photoHeight > PhotoManager.IMAGE_SIZE_THUMBNAIL
					? PhotoManager.IMAGE_QUALITY_EXIF_THUMB
					: PhotoManager.IMAGE_QUALITY_LARGE_IMAGE;

			photoImage = PhotoImageCache.getImage(photo, lowerImageQuality);
		}

		int imageCanvasHeight = photoHeight;

		final boolean isShowText = isShowLabels();
		if (isShowText) {
			imageCanvasHeight -= 2 * _fontHeight + 0;
		}

		gc.setForeground(getForegroundColor());
		gc.setBackground(getBackgroundColor());

		if (photoImage != null && photoImage.isDisposed() == false) {

			// draw photo image

			drawImage(gc, photo, photoImage, photoPosX, photoPosY, photoWidth, imageCanvasHeight);

		} else {

			// image is not available

			drawStatusText(gc, photo, photoPosX, photoPosY, photoWidth, imageCanvasHeight, requestedImageQuality);
		}

		// Draw label
		if (isShowText) {

			final String text = galleryItem.getText();

			// Draw
			final int textPosY = photoPosY + imageCanvasHeight;

			gc.setBackground(getBackgroundColor());
			gc.setForeground(getForegroundColor());

			/*
			 * draw 1st line
			 */
			final DateTime dateTime = photo.getFileDateTime();
			if (dateTime != null) {

				final String textDateTime = _dtFormatter.print(dateTime);

				// Center text
				final int textWidth = gc.textExtent(textDateTime).x;
				final int textOffset = (photoWidth - (textWidth > photoWidth ? photoWidth : textWidth)) / 2;

				gc.drawString(textDateTime, photoPosX + textOffset, textPosY, true);
			}

			/*
			 * draw 2nd line
			 */
			// Center text
			final int textWidth = gc.textExtent(text).x;
			final int textOffset = (photoWidth - (textWidth > photoWidth ? photoWidth : textWidth)) / 2;

			gc.drawString(text, photoPosX + textOffset, textPosY + _fontHeight, true);
		}

//		if (isRequestedQuality == false) {
//
//			final int devY = photoPosY + photoHeight - _fontHeight + 0;
//
//			gc.setBackground(getBackgroundColor());
//			gc.fillRectangle(photoPosX, devY, 10, 10);
//
////			gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
////			gc.drawLine(devXGallery, devY, devXGallery + photoWidth, devY);
//		}

		// draw selection border
		if (selected) {
			gc.setForeground(getSelectionForegroundColor());
			gc.drawRoundRectangle(galleryPosX + 1, galleryPosY + 1, galleryItemWidth - 3, galleryItemHeight - 3, 6, 6);
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
	 */
	private void drawImage(	final GC gc,
							final Photo photo,
							final Image photoImage,
							final int photoPosX,
							final int photoPosY,
							final int photoWidth,
							final int photoHeight) {

		int photoPaintedWidth = 0;
		int photoPaintedHeight = 0;
		int offsetX = 0;
		int offsetY = 0;
		int imageWidth = 0;
		int imageHeight = 0;

//		gc.setAdvanced(false);

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

			if (photoWidthRotated != Integer.MIN_VALUE && photoHeightRotated != Integer.MIN_VALUE) {

				// photo is loaded

				if (photoPaintedWidth > photoWidthRotated || photoPaintedHeight > photoHeightRotated) {

					/*
					 * photo image should not be displayed larger than the original photo even when
					 * the thumb image is larger, this can happen when image is resized
					 */

					photoPaintedWidth = photoWidthRotated;
					photoPaintedHeight = photoHeightRotated;
				}
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
}
