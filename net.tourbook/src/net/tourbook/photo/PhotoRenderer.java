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

import net.tourbook.photo.manager.Photo;
import net.tourbook.photo.manager.PhotoImageCache;
import net.tourbook.photo.manager.PhotoManager;

import org.eclipse.nebula.widgets.gallery.DefaultGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.nebula.widgets.gallery.RendererHelper;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Original: org.sharemedia.utils.gallery.ShareMediaIconRenderer2
 */
public class PhotoRenderer extends DefaultGalleryItemRenderer {

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public void draw(	final GC gc,
						final GalleryItem galleryItem,
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

		final int defaultImageQuality = galleryItemHeight > PhotoManager.THUMBNAIL_DEFAULT_SIZE
				? PhotoManager.IMAGE_QUALITY_600
				: PhotoManager.IMAGE_QUALITY_THUMB_160;

		Image photoImage = PhotoImageCache.getImage(photo.getImageKey(defaultImageQuality));
		if (photoImage == null) {

			final int lowerImageQuality = galleryItemHeight > PhotoManager.THUMBNAIL_DEFAULT_SIZE
					? PhotoManager.IMAGE_QUALITY_THUMB_160
					: PhotoManager.IMAGE_QUALITY_600;

			photoImage = PhotoImageCache.getImage(photo.getImageKey(lowerImageQuality));
		}

		final int imageBorderWidth = 2;
		final int roundingArc = 8;

		int imageWidth = 0;
		int imageHeight = 0;
		int useableHeight = galleryItemHeight;
		int fontHeight = 0;
		final boolean isText = galleryItem.getText() != null && isShowLabels();

		if (isText) {
			fontHeight = gc.getFontMetrics().getHeight();
			useableHeight -= fontHeight + 2;
		}

		if (selected) {
			// draw selection
			gc.setBackground(getSelectionBackgroundColor());
			gc.setForeground(getSelectionBackgroundColor());
		} else {
			gc.setBackground(getBackgroundColor());
		}

//		gc.fillRoundRectangle(//
//				devXGallery,
//				devYGallery,
//				galleryItemWidth,
//				useableHeight,
//				roundingArc,
//				roundingArc);
//
//		if (isText) {
//			gc.fillRoundRectangle(
//					devXGallery,
//					devYGallery + galleryItemHeight - fontHeight,
//					galleryItemWidth,
//					fontHeight,
//					roundingArc,
//					roundingArc);
//		}

		if (photoImage != null && photoImage.isDisposed() == false) {

			// draw image

			/*
			 * exception can occure because the image could be disposed before it is drawn
			 */
			try {

				final Rectangle itemImageBounds = photoImage.getBounds();
				imageWidth = itemImageBounds.width;
				imageHeight = itemImageBounds.height;

				final Point size = RendererHelper.getBestSize(//
						imageWidth,
						imageHeight,
						galleryItemWidth - imageBorderWidth,
						useableHeight - imageBorderWidth);

				final int xShiftSrc = galleryItemWidth - size.x;
				final int yShiftSrc = useableHeight - size.y;

				final int xShift = xShiftSrc >> 1;
				final int yShift = yShiftSrc >> 1;

				// Draw image
				if (size.x > 0 && size.y > 0) {

					gc.drawImage(photoImage, //
							0,
							0,
							imageWidth,
							imageHeight,
							//
							devXGallery + xShift,
							devYGallery + yShift,
							size.x,
							size.y);
				}
			} catch (final Exception e) {
				drawText(gc, devXGallery, devYGallery, photo, true);
			}
		} else {

			// image is not available

			drawText(gc, devXGallery, devYGallery, photo, false);
		}

		// Draw label
		if (galleryItem.getText() != null && isShowLabels()) {
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
			final String text = RendererHelper.createLabel(galleryItem.getText(), gc, galleryItemWidth - 10);

			// Center text
			final int textWidth = gc.textExtent(text).x;
			final int textxShift = (galleryItemWidth - (textWidth > galleryItemWidth ? galleryItemWidth : textWidth)) >> 1;

			// Draw
			gc.drawText(text, devXGallery + textxShift, devYGallery + galleryItemHeight - fontHeight, true);
		}

	}

	private void drawText(final GC gc, final int x, final int y, final Photo photo, final boolean isError) {

		final String photoData = (isError ? "!!! " : "") + photo.getFileName();
		gc.drawText(photoData, x, y);
	}
}
