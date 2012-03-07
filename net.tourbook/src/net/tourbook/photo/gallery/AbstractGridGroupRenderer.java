/*******************************************************************************
 * Copyright (c) 2006-2007 Nicolas Richeton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors :
 *    Nicolas Richeton (nicolas.richeton@gmail.com) - initial API and implementation
 *    Richard Michalsky - bug 197959
 *******************************************************************************/
package net.tourbook.photo.gallery;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Item;

/**
 * <p>
 * Abstract class which provides low-level support for a grid-based group. renderer.
 * </p>
 * <p>
 * NOTE: THIS WIDGET AND ITS API ARE STILL UNDER DEVELOPMENT.
 * </p>
 * 
 * @author Nicolas Richeton (nicolas.richeton@gmail.com)
 * @contributor Richard Michalsky (bug 197959)
 * @contributor Robert Handschmann (bug 215817)
 */

public abstract class AbstractGridGroupRenderer extends AbstractGalleryGroupRenderer {

	static final int				DEFAULT_SIZE	= 96;

	protected int					minMargin;

	protected int					margin;

	protected boolean				autoMargin;

	protected int					itemWidth		= DEFAULT_SIZE;

	protected int					itemHeight		= DEFAULT_SIZE;

	public static final String		H_COUNT			= "g.h";		//$NON-NLS-1$

	public static final String		V_COUNT			= "g.v";		//$NON-NLS-1$

	protected static final String	EMPTY_STRING	= "";			//$NON-NLS-1$

	private static final int		END				= 0;

	private static final int		START			= 1;

	/**
	 * If true, groups are always expanded and toggle button is not displayed
	 */
	private boolean					alwaysExpanded	= false;

	protected int calculateMargins(final int size, final int count, final int itemSize) {
		int margin = this.minMargin;
		margin += Math.round((float) (size - this.minMargin - (count * (itemSize + this.minMargin))) / (count + 1));
		return margin;
	}

	@Override
	public void dispose() {
		// Nothing required here. This method can be overridden when needed.
	}

	@Override
	public void draw(	final GC gc,
						final GalleryMTItem group,
						final int x,
						final int y,
						final int clipX,
						final int clipY,
						final int clipWidth,
						final int clipHeight) {}

	/**
	 * Draw a child item. Only used when useGroup is true.
	 * 
	 * @param gc
	 * @param index
	 * @param isSelected
	 * @param parent
	 */
	protected void drawItem(final GC gc,
							final int index,
							final boolean isSelected,
							final GalleryMTItem parent,
							final int offsetY) {

		if (index < parent.getItemCount()) {

			final int hCount = ((Integer) parent.getData(H_COUNT)).intValue();
			final int vCount = ((Integer) parent.getData(V_COUNT)).intValue();

			final boolean isVertical = gallery.isVertical();
			int posX, posY;
			if (isVertical) {
				posX = index % hCount;
				posY = (index - posX) / hCount;
			} else {
				posY = index % vCount;
				posX = (index - posY) / vCount;
			}

			final Item item = parent.getItem(index);

			// No item ? return
			if (item == null) {
				return;
			}

			final GalleryMTItem gItem = (GalleryMTItem) item;

			int xPixelPos, yPixelPos;
			if (isVertical) {
				xPixelPos = posX * (itemWidth + margin) + margin;
				yPixelPos = posY * (itemHeight + minMargin) - gallery._galleryPosition
				/* + minMargin */
				+ ((parent == null) ? 0 : (parent.y) + offsetY);
				gItem.x = xPixelPos;
				gItem.y = yPixelPos + gallery._galleryPosition;
			} else {
				xPixelPos = posX * (itemWidth + minMargin) - gallery._galleryPosition
				/* + minMargin */
				+ ((parent == null) ? 0 : (parent.x) + offsetY);
				yPixelPos = posY * (itemHeight + margin) + margin;
				gItem.x = xPixelPos + gallery._galleryPosition;
				gItem.y = yPixelPos;
			}

			gItem.height = itemHeight;
			gItem.width = itemWidth;

			gallery.sendPaintItemEvent(item, index, gc, xPixelPos, yPixelPos, this.itemWidth, this.itemHeight);

			final AbstractGalleryItemRenderer itemRenderer = gallery.getItemRenderer();

			if (itemRenderer != null) {
				gc.setClipping(xPixelPos, yPixelPos, itemWidth, itemHeight);
				itemRenderer.setSelected(isSelected);

//				final Rectangle oldClipping = gc.getClipping();
//				gc.setClipping(oldClipping.intersection(new Rectangle(xPixelPos, yPixelPos, itemWidth, itemHeight)));
//				{
					itemRenderer.draw(gc, gItem, index, xPixelPos, yPixelPos, itemWidth, itemHeight);
//				}
//				gc.setClipping(oldClipping);
			}
		}
	}

	private GalleryMTItem getFirstItem(final GalleryMTItem group, final int from) {
		if (group == null) {
			return null;
		}

		switch (from) {
		case END:
			return group.getItem(group.getItemCount() - 1);

		case START:
		default:
			return group.getItem(0);
		}

	}

	@Override
	public GalleryMTItem getItem(final GalleryMTItem group, final Point coords) {
		return null;
	}

	/**
	 * Get item at pixel position
	 * 
	 * @param coords
	 * @return
	 */
	protected GalleryMTItem getItem(final GalleryMTItem group, final Point coords, final int offsetY) {

		int itemNb;
		if (gallery.isVertical()) {
			final Integer tmp = (Integer) group.getData(H_COUNT);
			if (tmp == null) {
				return null;
			}
			final int hCount = tmp.intValue();

			// Calculate where the item should be if it exists
			final int posX = (coords.x - margin) / (itemWidth + margin);

			// Check if the users clicked on the X margin.
			final int posOnItem = (coords.x - margin) % (itemWidth + margin);
			if (posOnItem > itemWidth || posOnItem < 0) {
				return null;
			}

			if (posX >= hCount) {
				return null;
			}

			if (coords.y - group.y < offsetY) {
				return null;
			}

			final int posY = (coords.y - group.y - offsetY) / (itemHeight + minMargin);

			// Check if the users clicked on the Y margin.
			if (((coords.y - group.y - offsetY) % (itemHeight + minMargin)) > itemHeight) {
				return null;
			}
			itemNb = posX + posY * hCount;
		} else {
			final Integer tmp = (Integer) group.getData(V_COUNT);
			if (tmp == null) {
				return null;
			}
			final int vCount = tmp.intValue();

			// Calculate where the item should be if it exists
			final int posY = (coords.y - margin) / (itemHeight + margin);

			// Check if the users clicked on the X margin.
			final int posOnItem = (coords.y - margin) % (itemHeight + margin);
			if (posOnItem > itemHeight || posOnItem < 0) {
				return null;
			}

			if (posY >= vCount) {
				return null;
			}

			if (coords.x - group.x < offsetY) {
				return null;
			}

			final int posX = (coords.x - group.x - offsetY) / (itemWidth + minMargin);

			// Check if the users clicked on the X margin.
			if (((coords.x - group.x - offsetY) % (itemWidth + minMargin)) > itemWidth) {
				return null;
			}
			itemNb = posY + posX * vCount;
		}

		if (itemNb < group.getItemCount()) {
			return group.getItem(itemNb);
		}

		return null;
	}

	/**
	 * Return the child item of group which is at column 'pos' starting from direction. If this item
	 * doesn't exists, returns the nearest item.
	 * 
	 * @param group
	 * @param pos
	 * @param from
	 *            START or END
	 * @return
	 */
	private GalleryMTItem getItemAt(final GalleryMTItem group, final int pos, final int from) {
		if (group == null) {
			return null;
		}

		final int hCount = ((Integer) group.getData(H_COUNT)).intValue();
		int offset = 0;
		switch (from) {
		case END:
			if (group.getItemCount() == 0) {
				return null;
			}

			// Last item column
			int endPos = group.getItemCount() % hCount;

			// If last item column is 0, the line is full
			if (endPos == 0) {
				endPos = hCount - 1;
				offset--;
			}

			// If there is an item at column 'pos'
			if (pos < endPos) {
				final int nbLines = (group.getItemCount() / hCount) + offset;
				return group.getItem(nbLines * hCount + pos);
			}

			// Get the last item.
			return group.getItem((group.getItemCount() / hCount + offset) * hCount + endPos - 1);

		case START:
		default:
			if (pos >= group.getItemCount()) {
				return group.getItem(group.getItemCount() - 1);
			}

			return group.getItem(pos);

		}

	}

	public int getItemHeight() {
		return itemHeight;
	}

	public int getItemWidth() {
		return itemWidth;
	}

	protected Point getLayoutData(final GalleryMTItem item) {
		final Integer hCount = ((Integer) item.getData(H_COUNT));
		final Integer vCount = ((Integer) item.getData(V_COUNT));

		if (hCount == null || vCount == null) {
			return null;
		}

		return new Point(hCount.intValue(), vCount.intValue());
	}

	/**
	 * Get maximum visible lines.
	 * 
	 * @return
	 */
	private int getMaxVisibleLines() {

		// TODO: support group titles (fewer lines are visible if one or more
		// group titles are displayed). This method should probably be
		// implemented in the group renderer and not in the abstract class.

		// Gallery is vertical
		if (gallery.isVertical()) {
			return gallery.getClientAreaCached().height / itemHeight;
		}

		// Gallery is horizontal
		return gallery.getClientAreaCached().width / itemWidth;
	}

	public int getMinMargin() {
		return minMargin;
	}

	private GalleryMTItem getNextGroup(final GalleryMTItem group) {
		int gPos = gallery.indexOf(group);
		while (gPos < gallery.getItemCount() - 1) {
			final GalleryMTItem newGroup = gallery.getItem(gPos + 1);
			if (isGroupExpanded(newGroup)) {
				return newGroup;
			}
			gPos++;
		}

		return null;
	}

	@Override
	public GalleryMTItem getNextItem(final GalleryMTItem item, final int key) {
		// Key navigation is useless with an empty gallery
		if (gallery.getItemCount() == 0) {
			return null;
		}

		// Check for current selection
		if (item == null) {
			// No current selection, select the first item
			if (gallery.getItemCount() > 0) {
				final GalleryMTItem firstGroup = gallery.getItem(0);
				if (firstGroup != null && firstGroup.getItemCount() > 0) {
					return firstGroup.getItem(0);
				}

			}
			return null;
		}

		// Check for groups
		if (item.getParentItem() == null) {
			// Key navigation is only available for child items ATM
			return null;
		}

		final GalleryMTItem group = item.getParentItem();

		// Handle HOME and END
		switch (key) {
		case SWT.HOME:
			gallery.getItem(0).setExpanded(true);
			return getFirstItem(gallery.getItem(0), START);

		case SWT.END:
			gallery.getItem(gallery.getItemCount() - 1).setExpanded(true);
			return getFirstItem(gallery.getItem(gallery.getItemCount() - 1), END);
		}

		final int pos = group.indexOf(item);
		GalleryMTItem next = null;

		// Handle arrows and page up / down
		if (gallery.isVertical()) {
			final int hCount = ((Integer) group.getData(H_COUNT)).intValue();
			final int maxVisibleRows = getMaxVisibleLines();
			switch (key) {
			case SWT.ARROW_LEFT:
				next = goLeft(group, pos);
				break;

			case SWT.ARROW_RIGHT:
				next = goRight(group, pos);
				break;

			case SWT.ARROW_UP:
				next = goUp(group, pos, hCount, 1);
				break;

			case SWT.ARROW_DOWN:
				next = goDown(group, pos, hCount, 1);
				break;

			case SWT.PAGE_UP:
				next = goUp(group, pos, hCount, Math.max(maxVisibleRows - 1, 1));
				break;

			case SWT.PAGE_DOWN:
				next = goDown(group, pos, hCount, Math.max(maxVisibleRows - 1, 1));
				break;
			}
		} else {
			final int vCount = ((Integer) group.getData(V_COUNT)).intValue();
			final int maxVisibleColumns = getMaxVisibleLines();
			switch (key) {
			case SWT.ARROW_LEFT:
				next = goUp(group, pos, vCount);
				break;

			case SWT.ARROW_RIGHT:
				next = goDown(group, pos, vCount);
				break;

			case SWT.ARROW_UP:
				next = goLeft(group, pos);
				break;

			case SWT.ARROW_DOWN:
				next = goRight(group, pos);
				break;

			case SWT.PAGE_UP:
				next = goUp(group, pos, vCount * Math.max(maxVisibleColumns - 1, 1));
				break;

			case SWT.PAGE_DOWN:
				next = goDown(group, pos, vCount * Math.max(maxVisibleColumns - 1, 1));
				break;

			}
		}

		return next;
	}

	private GalleryMTItem getPreviousGroup(final GalleryMTItem group) {
		int gPos = gallery.indexOf(group);
		while (gPos > 0) {
			final GalleryMTItem newGroup = gallery.getItem(gPos - 1);
			if (isGroupExpanded(newGroup)) {
				return newGroup;
			}
			gPos--;
		}

		return null;
	}

	@Override
	public Rectangle getSize(final GalleryMTItem item) {
		return null;
	}

	protected Rectangle getSize(final GalleryMTItem item, final int offsetY) {

		final GalleryMTItem parent = item.getParentItem();
		if (parent != null) {
			final int index = parent.indexOf(item);

			final Point layoutData = getLayoutData(parent);
			if (layoutData == null) {
				return null;
			}

			final int hCount = layoutData.x;
			final int vCount = layoutData.y;

			if (gallery.isVertical()) {
				final int posX = index % hCount;
				final int posY = (index - posX) / hCount;

				final int xPixelPos = posX * (itemWidth + margin) + margin;
				final int yPixelPos = posY * (itemHeight + minMargin) + ((parent == null) ? 0 : (parent.y) + offsetY);

				return new Rectangle(xPixelPos, yPixelPos, this.itemWidth, this.itemHeight);
			}

			// gallery is horizontal
			final int posY = index % vCount;
			final int posX = (index - posY) / vCount;

			final int yPixelPos = posY * (itemHeight + margin) + margin;
			final int xPixelPos = posX * (itemWidth + minMargin) + ((parent == null) ? 0 : (parent.x) + offsetY);

			return new Rectangle(xPixelPos, yPixelPos, this.itemWidth, this.itemHeight);
		}

		return null;
	}

	protected Point getSize(final int nbx,
							final int nby,
							final int itemSizeX,
							final int itemSizeY,
							final int minMargin,
							final int autoMargin) {
		int x = 0, y = 0;

		if (gallery.isVertical()) {
			x = nbx * itemSizeX + (nbx - 1) * margin + 2 * minMargin;
			y = nby * itemSizeY + nby * minMargin;
		} else {
			x = nbx * itemSizeX + nbx * minMargin;
			y = nby * itemSizeY + (nby - 1) * margin + 2 * minMargin;
		}
		return new Point(x, y);
	}

	protected int[] getVisibleItems(final GalleryMTItem group,
									final int x,
									final int y,
									final int clipX,
									final int clipY,
									final int clipWidth,
									final int clipHeight,
									final int offset) {
		int[] indexes;

		if (gallery.isVertical()) {
			final int count = ((Integer) group.getData(H_COUNT)).intValue();
			// TODO: Not used ATM
			// int vCount = ((Integer) group.getData(V_COUNT)).intValue();

			int firstLine = (clipY - y - offset - minMargin) / (itemHeight + minMargin);
			if (firstLine < 0) {
				firstLine = 0;
			}

			final int firstItem = firstLine * count;

			int lastLine = (clipY - y - offset + clipHeight - minMargin) / (itemHeight + minMargin);

			if (lastLine < firstLine) {
				lastLine = firstLine;
			}

			final int lastItem = (lastLine + 1) * count;

			// exit if no item selected
			if (lastItem - firstItem == 0) {
				return null;
			}

			indexes = new int[lastItem - firstItem];
			for (int i = 0; i < (lastItem - firstItem); i++) {
				indexes[i] = firstItem + i;
			}

		} else {
			final int count = ((Integer) group.getData(V_COUNT)).intValue();

			int firstLine = (clipX - x - offset - minMargin) / (itemWidth + minMargin);
			if (firstLine < 0) {
				firstLine = 0;
			}

			final int firstItem = firstLine * count;

			int lastLine = (clipX - x - offset + clipWidth - minMargin) / (itemWidth + minMargin);

			if (lastLine < firstLine) {
				lastLine = firstLine;
			}

			final int lastItem = (lastLine + 1) * count;

			// exit if no item selected
			if (lastItem - firstItem == 0) {
				return null;
			}

			indexes = new int[lastItem - firstItem];
			for (int i = 0; i < (lastItem - firstItem); i++) {
				indexes[i] = firstItem + i;
			}
		}

		return indexes;
	}

	/**
	 * Get the next item, when going down.
	 * 
	 * @param group
	 *            current group
	 * @param posParam
	 *            index of currently selected item
	 * @param hCount
	 *            size of a line
	 * @return
	 */
	private GalleryMTItem goDown(final GalleryMTItem group, final int posParam, final int hCount) {
		final int colPos = posParam % hCount;
		final int pos = posParam + hCount;

		if (pos >= group.getItemCount()) {
			// Look for next non-empty group and get the first item
			GalleryMTItem item = null;
			GalleryMTItem currentGroup = group;
			while (item == null && currentGroup != null) {
				currentGroup = this.getNextGroup(currentGroup);
				item = this.getItemAt(currentGroup, colPos, START);
			}
			return item;

		}

		// else
		return group.getItem(pos);
	}

	private GalleryMTItem goDown(final GalleryMTItem group, final int posParam, final int hCount, final int lineCount) {

		if (lineCount == 0) {
			return null;
		}

		// Optimization when only one group involved
		if (posParam + hCount * lineCount < group.getItemCount()) {
			return group.getItem(posParam + hCount * lineCount);
		}

		// Get next item.
		GalleryMTItem next = goDown(group, posParam, hCount);
		if (next == null) {
			return null;
		}

		GalleryMTItem newItem = null;
		for (int i = 1; i < lineCount; i++) {
			newItem = goDown(next.getParentItem(), next.getParentItem().indexOf(next), hCount);
			if (newItem == next || newItem == null) {
				break;
			}

			next = newItem;
		}

		return next;
	}

	private GalleryMTItem goLeft(final GalleryMTItem group, final int posParam) {
		final int pos = posParam - 1;

		if (pos < 0) {
			// Look for next non-empty group and get the last item
			GalleryMTItem item = null;
			GalleryMTItem currentGroup = group;
			while (item == null && currentGroup != null) {
				currentGroup = this.getPreviousGroup(currentGroup);
				item = this.getFirstItem(currentGroup, END);
			}
			return item;
		}

		// else
		return group.getItem(pos);
	}

	private GalleryMTItem goRight(final GalleryMTItem group, final int posParam) {
		final int pos = posParam + 1;

		if (pos >= group.getItemCount()) {
			// Look for next non-empty group and get the first item
			GalleryMTItem item = null;
			GalleryMTItem currentGroup = group;
			while (item == null && currentGroup != null) {
				currentGroup = this.getNextGroup(currentGroup);
				item = this.getFirstItem(currentGroup, START);
			}
			return item;
		}

		// else
		return group.getItem(pos);
	}

	/**
	 * Get the next item, when going up.
	 * 
	 * @param group
	 *            current group
	 * @param posParam
	 *            index of currently selected item
	 * @param hCount
	 *            size of a line
	 * @return
	 */
	private GalleryMTItem goUp(final GalleryMTItem group, final int posParam, final int hCount) {
		final int colPos = posParam % hCount;
		final int pos = posParam - hCount;

		if (pos < 0) {
			// Look for next non-empty group and get the last item
			GalleryMTItem item = null;
			GalleryMTItem currentGroup = group;
			while (item == null && currentGroup != null) {
				currentGroup = this.getPreviousGroup(currentGroup);
				item = this.getItemAt(currentGroup, colPos, END);
			}
			return item;
		}

		// else
		return group.getItem(pos);
	}

	private GalleryMTItem goUp(final GalleryMTItem group, final int posParam, final int hCount, final int lineCount) {

		if (lineCount == 0) {
			return null;
		}

		// Optimization when only one group involved
		if (posParam - hCount * lineCount >= 0) {
			return group.getItem(posParam - hCount * lineCount);
		}

		// Get next item.
		GalleryMTItem next = goUp(group, posParam, hCount);
		if (next == null) {
			return null;
		}

		GalleryMTItem newItem = null;
		for (int i = 1; i < lineCount; i++) {
			newItem = goUp(next.getParentItem(), next.getParentItem().indexOf(next), hCount);
			if (newItem == next || newItem == null) {
				break;
			}

			next = newItem;
		}

		return next;
	}

	/**
	 * Calculate how many items are displayed horizontally and vertically.
	 * 
	 * @param size
	 * @param nbItems
	 * @param itemSize
	 * @return
	 */
	protected Point gridLayout(final int size, final int nbItems, final int itemSize) {
		int x = 0, y = 0;

		if (nbItems == 0) {
			return new Point(x, y);
		}

		x = (size - minMargin) / (itemSize + minMargin);
		if (x > 0) {
			y = (int) Math.ceil((double) nbItems / (double) x);
		} else {
			// Show at least one item;
			y = nbItems;
			x = 1;
		}

		return new Point(x, y);
	}

	/**
	 * If true, groups are always expanded and toggle button is not displayed
	 * 
	 * @return true if groups are always expanded
	 */
	public boolean isAlwaysExpanded() {
		return alwaysExpanded;
	}

	public boolean isAutoMargin() {
		return autoMargin;
	}

	/**
	 * Return item expand state (item.isExpanded()) Returns always true is alwaysExpanded is set to
	 * true.
	 * 
	 * @param item
	 * @return
	 */
	protected boolean isGroupExpanded(final GalleryMTItem item) {
		if (alwaysExpanded) {
			return true;
		}

		if (item == null) {
			return false;
		}

		return item.isExpanded();
	}

	@Override
	public void layout(final GC gc, final GalleryMTItem group) {}

	@Override
	public boolean mouseDown(final GalleryMTItem group, final MouseEvent e, final Point coords) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.nebula.widgets.gallery.AbstractGalleryGroupRenderer#preLayout
	 * (org.eclipse.swt.graphics.GC)
	 */
	@Override
	public void preLayout(final GC gc) {
		// Reset margin to minimal value before "best fit" calculation
		this.margin = this.minMargin;
		super.preLayout(gc);
	}

	/**
	 * If true, groups are always expanded and toggle button is not displayed if false, expand
	 * status depends on each item.
	 * 
	 * @param alwaysExpanded
	 */
	public void setAlwaysExpanded(final boolean alwaysExpanded) {
		this.alwaysExpanded = alwaysExpanded;
	}

	public void setAutoMargin(final boolean autoMargin) {
		this.autoMargin = autoMargin;

		updateGallery();
	}

	public void setItemHeight(final int itemHeight) {
		this.itemHeight = itemHeight;

		updateGallery();
	}

	public void setItemSize(final int width, final int height) {

		this.itemHeight = height;
		this.itemWidth = width;

		updateGallery();
	}

	public void setItemWidth(final int itemWidth) {
		this.itemWidth = itemWidth;

		updateGallery();
	}

	public void setMinMargin(final int minMargin) {
		this.minMargin = minMargin;

		updateGallery();
	}

	private void updateGallery() {
		// Update gallery
		if (gallery != null) {
			gallery.updateStructuralValues(null, true);
			gallery.updateScrollBarsProperties();
			gallery.redraw();
		}
	}

}
