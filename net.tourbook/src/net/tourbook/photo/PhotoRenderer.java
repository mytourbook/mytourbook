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

	private static final PhotoImageCache	_imageCache	= PhotoImageCache.getInstance();

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public void draw(	final GC gc,
						final GalleryItem galleryItem,
						final int index,
						final int x,
						final int y,
						final int width,
						final int height) {

		final Object itemData = galleryItem.getData();
		if ((itemData instanceof Photo) == false) {
			return;
		}

		final Photo photo = (Photo) itemData;

		final int defaultImageQuality = height > PhotoManager.THUMBNAIL_SIZE
				? PhotoManager.IMAGE_QUALITY_LOW_640
				: PhotoManager.IMAGE_QUALITY_THUMB_160;

		Image photoImage = _imageCache.getImage(photo.getImageKey(defaultImageQuality));
		if (photoImage == null) {

			final int lowerImageQuality = height > PhotoManager.THUMBNAIL_SIZE
					? PhotoManager.IMAGE_QUALITY_THUMB_160
					: PhotoManager.IMAGE_QUALITY_LOW_640;

			photoImage = _imageCache.getImage(photo.getImageKey(lowerImageQuality));
		}

		if (photoImage == null || photoImage.isDisposed()) {

			drawText(gc, x, y, photo, false);
			return;
		}

		int imageWidth = 0;
		int imageHeight = 0;
		int xShift = 0;
		int yShift = 0;
		final int dropShadowsSize = this.getDropShadowsSize();
		final int useableHeight = height;

		// draw background
//		gc.fillRoundRectangle(x, y, width, useableHeight, 15, 15);
//		if (item.getText() != null && isShowLabels()) {
//			gc.fillRoundRectangle(x, y + height - fontHeight, width, fontHeight, 15, 15);
//		}

		final Rectangle itemImageBounds = photoImage.getBounds();
		imageWidth = itemImageBounds.width;
		imageHeight = itemImageBounds.height;

		final Point size = RendererHelper.getBestSize(//
				imageWidth,
				imageHeight,
				width - 8 - 2 * dropShadowsSize,
				useableHeight - 8 - 2 * dropShadowsSize);

		xShift = (width - size.x) >> 1;
		yShift = (useableHeight - size.y) >> 1;

		// Draw image
		if (size.x > 0 && size.y > 0) {

			/*
			 * exception can occure when image is disposed, this happened several times during
			 * development
			 */
			try {
				gc.drawImage(photoImage, //
						0,
						0,
						imageWidth,
						imageHeight,
						//
						x + xShift,
						y + yShift,
						size.x,
						size.y);
			} catch (final Exception e) {
				drawText(gc, x, y, photo, true);
			}

		}
	}

	private void drawText(final GC gc, final int x, final int y, final Photo photo, final boolean isError) {

		final String photoData = (isError ? "!!! " : "") + photo.getFileName();
		gc.drawText(photoData, x, y);
	}
}
