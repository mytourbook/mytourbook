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
						final int devXGallery,
						final int devYGallery,
						final int galleryItemWidth,
						final int galleryItemHeight) {

		final Object itemData = galleryItem.getData();
		if ((itemData instanceof Photo) == false) {
			return;
		}

		final Photo photo = (Photo) itemData;

		if (_fontHeight == -1) {
			_fontHeight = gc.getFontMetrics().getHeight();
		}

		final int requestedImageQuality = galleryItemHeight > PhotoManager.IMAGE_SIZE_THUMBNAIL
				? PhotoManager.IMAGE_QUALITY_LARGE_IMAGE
				: PhotoManager.IMAGE_QUALITY_EXIF_THUMB;

		boolean isRequestedQuality = true;

		// get image with requested size
		Image photoImage = PhotoImageCache.getImage(photo, requestedImageQuality);

		if (photoImage == null) {

			// requested size is not available

			isRequestedQuality = false;

			final int lowerImageQuality = galleryItemHeight > PhotoManager.IMAGE_SIZE_THUMBNAIL
					? PhotoManager.IMAGE_QUALITY_EXIF_THUMB
					: PhotoManager.IMAGE_QUALITY_LARGE_IMAGE;

			photoImage = PhotoImageCache.getImage(photo, lowerImageQuality);
		}

		final int imageBorderWidth = 2;
		final int roundingArc = 8;

		int imageUseableHeight = galleryItemHeight;
		final boolean isText = isShowLabels();
//		boolean isText = galleryItem.getText() != null && isShowLabels();
//		if (galleryItemHeight <= 50) {
//			isText = false;
//		}
		if (isText) {
			imageUseableHeight -= 2 * _fontHeight + 2;
		}

		if (selected) {
			// draw selection
			gc.setForeground(getSelectionForegroundColor());
		} else {
			gc.setForeground(getForegroundColor());
		}
		gc.setBackground(getBackgroundColor());

		if (photoImage != null && photoImage.isDisposed() == false) {

			// draw image

			drawImage(
					photo,
					photoImage,
					gc,
					devXGallery,
					devYGallery,
					galleryItemWidth,
					imageBorderWidth,
					imageUseableHeight);

		} else {

			// image is not available

			drawText(gc, devXGallery, devYGallery, photo, null, true);
		}

		// Draw label
		if (isText) {

			// Set colors
			if (selected) {
				// Selected : use selection colors.
				gc.setForeground(getSelectionForegroundColor());
				gc.setBackground(getSelectionBackgroundColor());

			} else {
				// Not selected, use item values or defaults.

				// Background
//				if (itemBackgroundColor != null) {
//					gc.setBackground(itemBackgroundColor);
//				} else {
//					gc.setBackground(getBackgroundColor());
//				}
				gc.setBackground(getBackgroundColor());

				// Foreground
//				if (itemForegroundColor != null) {
//					gc.setForeground(itemForegroundColor);
//				} else {
//					gc.setForeground(getForegroundColor());
//				}
				gc.setForeground(getForegroundColor());
			}

			// Create label

			// RendererHelper IS A PERFORMANCE HOG when small images are displayed

//			final String text = RendererHelper.createLabel(galleryItem.getText(), gc, galleryItemWidth - 10);
			final String text = galleryItem.getText();

			// Center text
			final int textWidth = gc.textExtent(text).x;
			final int textxShift = (galleryItemWidth - (textWidth > galleryItemWidth ? galleryItemWidth : textWidth)) >> 1;

			// Draw
			final int devX = devXGallery + textxShift;
			final int devY = devYGallery + galleryItemHeight;

			gc.drawText(text, devX, devY - _fontHeight, true);

			final DateTime dateTime = photo.getFileDateTime();
			if (dateTime != null) {
				gc.drawText(_dtFormatter.print(dateTime), devXGallery + 10, devY - 2 * _fontHeight, true);
			}
		}

		if (isRequestedQuality == false) {

			final int devY = devYGallery + galleryItemHeight - _fontHeight + 0;

			gc.setBackground(getBackgroundColor());
			gc.fillRectangle(devXGallery, devY, 10, 10);

//			gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
//			gc.drawLine(devXGallery, devY, devXGallery + galleryItemWidth, devY);
		}

//		gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
//		gc.drawRectangle(devXGallery, devYGallery, galleryItemWidth - 1, galleryItemHeight - 1);
	}

	private void drawImage(	final Photo photo,
							final Image photoImage,
							final GC gc,
							final int devXGallery,
							final int devYGallery,
							final int galleryItemWidth,
							final int imageBorderWidth,
							final int imageUseableHeight) {

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

			final int imageCanvasWidth = galleryItemWidth - imageBorderWidth;
			final int imageCanvasHeight = imageUseableHeight - imageBorderWidth;

			final Point bestSize = RendererHelper.getBestSize(
					imageWidth,
					imageHeight,
					imageCanvasWidth,
					imageCanvasHeight);

			photoPaintedWidth = bestSize.x;
			photoPaintedHeight = bestSize.y;

			final int photoWidth = photo.getWidthRotated();
			final int photoHeight = photo.getHeightRotated();

			if (photoWidth != Integer.MIN_VALUE && photoHeight != Integer.MIN_VALUE) {

				// photo is loaded

				if (photoPaintedWidth > photoWidth || photoPaintedHeight > photoHeight) {

					/*
					 * photo image should not be displayed larger than the original photo even when
					 * the thumb image is larger, this can happen when image is resized
					 */

					photoPaintedWidth = photoWidth;
					photoPaintedHeight = photoHeight;
				}
			}

			// Draw image
			if (photoPaintedWidth > 0 && photoPaintedHeight > 0) {

				// center image
				offsetX = (galleryItemWidth - photoPaintedWidth) / 2;
				offsetY = (imageUseableHeight - photoPaintedHeight) / 2;

			}
		} catch (final Exception e) {
			StatusUtil.log(e);
		}

		int destX = 0;
		int destY = 0;
		try {

			destX = devXGallery + offsetX;
			destY = devYGallery + offsetY;

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

			drawText(gc, devXGallery, devYGallery, photo, e.getMessage(), false);

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

	private void drawText(	final GC gc,
							final int x,
							final int y,
							final Photo photo,
							final String errorMessage,
							final boolean isLoading) {

		String photoText = UI.EMPTY_STRING;
		final Device device = gc.getDevice();

		if (errorMessage != null) {
			photoText = "Error " + errorMessage + " in ";
			gc.setForeground(device.getSystemColor(SWT.COLOR_RED));
			gc.setBackground(device.getSystemColor(SWT.COLOR_WHITE));
		} else if (isLoading) {
			photoText = "Loading ";
			gc.setForeground(device.getSystemColor(SWT.COLOR_YELLOW));
			gc.setBackground(device.getSystemColor(SWT.COLOR_BLACK));
		}

		photoText += photo.getFileName();

		gc.drawText(photoText, x, y);
	}

	@Override
	public void setFont(final Font font) {

		// force font update
		_fontHeight = -1;

		super.setFont(font);
	}
}
