/*******************************************************************************
 * Copyright (c) 2006-2007 Nicolas Richeton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors :
 *    Nicolas Richeton (nicolas.richeton@gmail.com) - initial API and implementation
 *******************************************************************************/
package net.tourbook.photo.gallery;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * <p>
 * Alternate group renderer for the Gallery widget. This group renderer does not draw group titles.
 * Only items are displayed. All groups are considered as expanded.
 * </p>
 * <p>
 * The visual aspect is the same as the first version of the gallery widget.
 * </p>
 * <p>
 * NOTE: THIS WIDGET AND ITS API ARE STILL UNDER DEVELOPMENT.
 * </p>
 * 
 * @author Nicolas Richeton (nicolas.richeton@gmail.com)
 */
public class NoGroupRenderer extends AbstractGridGroupRenderer {

	protected static int	OFFSET	= 0;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.nebula.widgets.gallery.AbstractGridGroupRenderer#draw(org
	 * .eclipse.swt.graphics.GC, org.eclipse.nebula.widgets.gallery.GalleryItem, int, int, int, int,
	 * int, int)
	 */
	@Override
	public void draw(	final GC gc,
						final GalleryMTItem group,
						final int x,
						final int y,
						final int clipX,
						final int clipY,
						final int clipWidth,
						final int clipHeight) {

		// Get items in the clipping area
		final int[] indexes = getVisibleItems(group, x, y, clipX, clipY, clipWidth, clipHeight, OFFSET);

		if (indexes != null && indexes.length > 0) {
			for (int i = indexes.length - 1; i >= 0; i--) {

				// Draw item
				final boolean selected = group.isSelected(group.getItem(indexes[i]));

				drawItem(gc, indexes[i], selected, group, OFFSET);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.nebula.widgets.gallery.AbstractGridGroupRenderer#getItem(
	 * org.eclipse.nebula.widgets.gallery.GalleryItem, org.eclipse.swt.graphics.Point)
	 */
	@Override
	public GalleryMTItem getItem(final GalleryMTItem group, final Point coords) {
		return super.getItem(group, coords, OFFSET);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.nebula.widgets.gallery.AbstractGridGroupRenderer#getSize(
	 * org.eclipse.nebula.widgets.gallery.GalleryItem)
	 */
	@Override
	public Rectangle getSize(final GalleryMTItem item) {
		return super.getSize(item, OFFSET);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.nebula.widgets.gallery.AbstractGridGroupRenderer#layout(org
	 * .eclipse.swt.graphics.GC, org.eclipse.nebula.widgets.gallery.GalleryItem)
	 */
	@Override
	public void layout(final GC gc, final GalleryMTItem group) {

		final int countLocal = group.getItemCount();

		if (gallery.isVertical()) {
			final int sizeX = group.width;
			group.height = OFFSET;

			final Point l = gridLayout(sizeX, countLocal, itemWidth);
			final int hCount = l.x;
			final int vCount = l.y;
			if (autoMargin) {
				// Calculate best margins
				margin = calculateMargins(sizeX, hCount, itemWidth);
			}

			final Point s = this.getSize(hCount, vCount, itemWidth, itemHeight, minMargin, margin);
			group.height += s.y;

			group.setData(H_COUNT, new Integer(hCount));
			group.setData(V_COUNT, new Integer(vCount));
		} else {
			final int sizeY = group.height;
			group.width = OFFSET;

			final Point l = gridLayout(sizeY, countLocal, itemHeight);
			final int vCount = l.x;
			final int hCount = l.y;
			if (autoMargin) {
				// Calculate best margins
				margin = calculateMargins(sizeY, vCount, itemHeight);
			}

			final Point s = this.getSize(hCount, vCount, itemWidth, itemHeight, minMargin, margin);
			group.width += s.x;

			group.setData(H_COUNT, new Integer(hCount));
			group.setData(V_COUNT, new Integer(vCount));
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.nebula.widgets.gallery.AbstractGridGroupRenderer#mouseDown
	 * (org.eclipse.nebula.widgets.gallery.GalleryItem, org.eclipse.swt.events.MouseEvent,
	 * org.eclipse.swt.graphics.Point)
	 */
	@Override
	public boolean mouseDown(final GalleryMTItem group, final MouseEvent e, final Point coords) {
		// Do nothing
		return true;
	}
}
