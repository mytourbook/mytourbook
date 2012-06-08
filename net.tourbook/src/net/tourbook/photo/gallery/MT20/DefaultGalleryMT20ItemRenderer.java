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
package net.tourbook.photo.gallery.MT20;

import org.eclipse.swt.graphics.GC;

public class DefaultGalleryMT20ItemRenderer extends AbstractGalleryMT20ItemRenderer {

	public DefaultGalleryMT20ItemRenderer() {}

	@Override
	public void dispose() {

	}

	@Override
	public void draw(	final GC gc,
						final GalleryMT20Item galleryItem,
						final int viewPortX,
						final int viewPortY,
						final int galleryItemWidth,
						final int galleryItemHeight,
						final boolean isSelected) {

	}

	@Override
	public PaintingResult drawFullSize(	final GC gc,
										final GalleryMT20Item galleryItem,
										final int monitorWidth,
										final int monitorHeight,
										final ZoomState zoomState,
										final double zoomFactor) {

		return null;
	}

	@Override
	public int getBorderSize() {
		return 0;
	}

	@Override
	public void resetPreviousImage() {}

	@Override
	public void setPrefSettings(final boolean isShowFullsizePreview,
								final boolean isShowLoadingMessage,
								final boolean isShowHQImage) {}

}
