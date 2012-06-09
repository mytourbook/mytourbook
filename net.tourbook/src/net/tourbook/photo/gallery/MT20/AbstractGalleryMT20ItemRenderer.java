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
import org.eclipse.swt.graphics.Rectangle;

public abstract class AbstractGalleryMT20ItemRenderer {

	/**
	 * @param galleryItem
	 * @param monitorWidth
	 * @param monitorHeight
	 * @return Returns a rectangle which should be redrawn or <code>null</code> when nothing should
	 *         be redrawn.
	 */
	public abstract Rectangle drawFullSizeSetContext(GalleryMT20Item galleryItem, int monitorWidth, int monitorHeight);

	public void dispose() {}

	public abstract void draw(	GC gc,
								GalleryMT20Item galleryItem,
								int viewPortX,
								int viewPortY,
								int galleryItemWidth,
								int galleryItemHeight,
								boolean isSelected);

	/**
	 * @param gc
	 * @param galleryItem
	 * @param monitorWidth
	 * @param monitorHeight
	 * @param zoomState
	 *            Is <code>true</code> when image is displayed with fullsize or zoomed, is
	 *            <code>false</code> when image fits the window size.
	 * @param zoomFactor
	 * @return Returns painting parameters when image is painted or <code>null</code> when image
	 *         could not be painted.
	 */
	public abstract PaintingResult drawFullSize(GC gc,
												GalleryMT20Item galleryItem,
												int monitorWidth,
												int monitorHeight,
												ZoomState zoomState,
												double zoomFactor);

	public abstract int getBorderSize();

	public abstract void resetPreviousImage();

	public abstract void setPrefSettings(	boolean isShowFullsizePreview,
											boolean isShowLoadingMessage,
											boolean isShowHQImage);

}
