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

import net.tourbook.photo.gallery.GalleryMTItem;
import net.tourbook.photo.gallery.NoGroupRenderer;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

public class NoGroupRendererMT extends NoGroupRenderer {

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public void draw(	final GC gc,
						final GalleryMTItem group,
						final int galleryPosX,
						final int galleryPosY,
						final int clippingX,
						final int clippingY,
						final int clippingWidth,
						final int clippingHeight) {

		// get visible items in the clipping area
		final int[] visibleIndexes = getVisibleItems(
				group,
				galleryPosX,
				galleryPosY,
				clippingX,
				clippingY,
				clippingWidth,
				clippingHeight,
				OFFSET);

		if (visibleIndexes != null && visibleIndexes.length > 0) {

			for (int itemIndex = visibleIndexes.length - 1; itemIndex >= 0; itemIndex--) {

				final GalleryMTItem galleryItem = group.getItem(visibleIndexes[itemIndex]);
				final boolean isSelected = group.isSelected(galleryItem);

				drawItem(gc, visibleIndexes[itemIndex], isSelected, group, OFFSET);
			}

			setAllVisibleItems(group, galleryPosX, galleryPosY);
		}
	}

	/**
	 * Get visible state for all items, this is used to stop loading images which are not displayed.
	 * 
	 * @param groupItem
	 * @param x
	 * @param y
	 */
	private void setAllVisibleItems(final GalleryMTItem groupItem, final int x, final int y) {

		final Rectangle clientArea = groupItem.getGallery().getClientAreaCached();

		final int[] visibleIndexes = getVisibleItems(groupItem, x, y, 0, 0, clientArea.width, clientArea.height, OFFSET);

		if (visibleIndexes != null && visibleIndexes.length > 0) {

			final GalleryMTItem[] visibleItems = new GalleryMTItem[visibleIndexes.length];

			for (int itemIndex = visibleIndexes.length - 1; itemIndex >= 0; itemIndex--) {
				visibleItems[itemIndex] = groupItem.getItem(visibleIndexes[itemIndex]);
			}

			groupItem.setVisibleItems(visibleItems);
		}
	}

}
