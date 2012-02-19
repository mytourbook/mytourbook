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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * <p>
 * Base class used to implement a custom gallery group renderer.
 * </p>
 * <p>
 * NOTE: THIS WIDGET AND ITS API ARE STILL UNDER DEVELOPMENT.
 * </p>
 * 
 * @author Nicolas Richeton (nicolas.richeton@gmail.com)
 */
public abstract class AbstractGalleryGroupRenderer {

	protected GalleryMT	gallery;

	protected boolean	expanded;

	public abstract void dispose();

	/**
	 * Group size informations can be retrieved from group. Clipping informations
	 * 
	 * @param gc
	 * @param group
	 * @param x
	 * @param y
	 */
	public abstract void draw(	GC gc,
								GalleryMTItem group,
								int x,
								int y,
								int clipX,
								int clipY,
								int clipWidth,
								int clipHeight);

	/**
	 * Returns item background color. This method is called by {@link GalleryMTItem#getBackground()}
	 * and should be overridden by any group renderer which use additional colors. Note that item
	 * renderer is automatically used for items.
	 * 
	 * @param item
	 *            a GalleryItem
	 * @return Color The current background color (never null)
	 */
	protected Color getBackground(final GalleryMTItem item) {
		if (item != null) {

			if (item.getParentItem() == null && gallery.getItemRenderer() != null) {
				// This is an item, let the renderer decide
				return gallery.getItemRenderer().getBackground(item);
			}

			// This is a group, or no item renderer. Use standard SWT behavior :

			// Use item color first
			if (item.background != null) {
				return item.background;
			}

			// Then parent color.
			return item.getGallery().getBackground();

		}
		return null;
	}

	/**
	 * Returns item font. This method is called by {@link GalleryMTItem#getFont()} and should be
	 * overridden by any group renderer which use additional fonts. Note that item renderer is
	 * automatically used for items.
	 * 
	 * @param item
	 *            a GalleryItem
	 * @return The current item Font (never null)
	 */
	protected Font getFont(final GalleryMTItem item) {
		if (item != null) {

			if (item.getParentItem() != null && gallery.getItemRenderer() != null) {
				// This is an item, let the renderer decide
				return gallery.getItemRenderer().getFont(item);
			}
			// This is a group, or no item renderer. Use standard SWT behavior :

			// Use item font first
			if (item.font != null) {
				return item.font;
			}

			// Then parent font.
			return item.getGallery().getFont();

		}
		return null;
	}

	/**
	 * Returns item foreground color. This method is called by {@link GalleryMTItem#getForeground()}
	 * and should be overridden by any group renderer which use additional colors. Note that item
	 * renderer is automatically used for items.
	 * 
	 * @param item
	 *            a GalleryItem
	 * @return The current foreground (never null)
	 */
	protected Color getForeground(final GalleryMTItem item) {
		if (item != null) {

			if (item.getParentItem() != null && gallery.getItemRenderer() != null) {
				// This is an item, let the renderer decide
				return gallery.getItemRenderer().getForeground(item);
			}
			// This is a group, or no item renderer. Use standard SWT behavior :

			// Use item color first
			if (item.foreground != null) {
				return item.foreground;
			}

			// Then parent color.
			return item.getGallery().getForeground();

		}
		return null;
	}

	public GalleryMT getGallery() {
		return this.gallery;
	}

	protected Point getGroupPosition(final GalleryMTItem item) {
		return new Point(item.x, item.y);
	}

	protected Point getGroupSize(final GalleryMTItem item) {
		return new Point(item.width, item.height);

	}

	/**
	 * Returns the item at coords relative to the parent group.
	 * 
	 * @param group
	 * @param coords
	 * @return
	 */
	public abstract GalleryMTItem getItem(GalleryMTItem group, Point coords);

	/**
	 * Returns the item that should be selected when the current item is 'item' and the 'key' is
	 * pressed
	 * 
	 * @param item
	 * @param key
	 * @return
	 */
	public abstract GalleryMTItem getNextItem(GalleryMTItem item, int key);

	/**
	 * Returns the preferred Scrollbar increment for the current gallery layout.
	 * 
	 * @return
	 */
	public int getScrollBarIncrement() {
		return 16;
	}

	/**
	 * Returns the size of a group.
	 * 
	 * @param item
	 * @return
	 */
	public abstract Rectangle getSize(GalleryMTItem item);

	/**
	 * Get the expand/collapse state of the current group
	 * 
	 * @return true is the current group is expanded
	 */
	public boolean isExpanded() {
		return this.expanded;
	}

	/**
	 * This method is called on each root item when the Gallery changes (resize, item addition or
	 * removal) in order to update the gallery size. The implementation must update the item
	 * internal size (px) using setGroupSize(item, size); before returning.
	 * 
	 * @param gc
	 * @param group
	 */
	public abstract void layout(GC gc, GalleryMTItem group);

	public abstract boolean mouseDown(GalleryMTItem group, MouseEvent e, Point coords);

	/**
	 * Notifies the Gallery that the control expanded/collapsed state has changed.
	 * 
	 * @param group
	 */
	protected void notifyTreeListeners(final GalleryMTItem group) {
		gallery.notifyTreeListeners(group, group.isExpanded());
	}

	/**
	 * This method is called after drawing the last item. It may be used to cleanup and release
	 * resources created in preDraw().
	 * 
	 * @param gc
	 */
	public void postDraw(final GC gc) {
		// Nothing required here. This method can be overridden when needed.
	}

	/**
	 * This method is called after the layout of the last item.
	 * 
	 * @param gc
	 */
	public void postLayout(final GC gc) {
		// Nothing required here. This method can be overridden when needed.
	}

	/**
	 * This method is called before drawing the first item. It can be used to calculate some values
	 * (like font metrics) that will be used for each item.
	 * 
	 * @param gc
	 */
	public void preDraw(final GC gc) {
		// Nothing required here. This method can be overridden when needed.
	}

	/**
	 * This method is called before the layout of the first item. It can be used to calculate some
	 * values (like font metrics) that will be used for each item.
	 * 
	 * @param gc
	 */
	public void preLayout(final GC gc) {
		// Nothing required here. This method can be overridden when needed.
	}

	/**
	 * @see AbstractGalleryGroupRenderer#isExpanded()
	 * @param selected
	 */
	public void setExpanded(final boolean selected) {
		this.expanded = selected;
	}

	public void setGallery(final GalleryMT gallery) {
		this.gallery = gallery;
	}

	protected void setGroupSize(final GalleryMTItem item, final Point size) {
		item.width = size.x;
		item.height = size.y;
	}

	protected void updateScrollBarsProperties() {
		gallery.updateScrollBarsProperties();
	}

	/**
	 * Forces an update of the gallery layout.
	 * 
	 * @param keeplocation
	 *            if true, the gallery will try to keep the current visible items in the client area
	 *            after the new layout has been calculated.
	 */
	protected void updateStructuralValues(final boolean keeplocation) {
		gallery.updateStructuralValues(null, keeplocation);
	}

}
