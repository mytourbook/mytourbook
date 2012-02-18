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

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Item;

/**
 * <p>
 * Instances of this class represent a selectable user interface object that represents an item in a
 * gallery.
 * </p>
 * <p>
 * NOTE: THIS WIDGET AND ITS API ARE STILL UNDER DEVELOPMENT.
 * </p>
 * 
 * @see GalleryMT
 * @author Nicolas Richeton (nicolas.richeton@gmail.com)
 * @contributor Peter Centgraf (bugs 212071, 212073)
 * @contributor Berthold Daum (bug 306144 - selection tuning)
 */

public class GalleryMTItem extends Item {

	private static final String	EMPTY_STRING	= "";											//$NON-NLS-1$

	private String[]			text			= { EMPTY_STRING, EMPTY_STRING, EMPTY_STRING };

	// This is managed by the Gallery
	/**
	 * Children of this item. Only used when groups are enabled.
	 */
	protected GalleryMTItem[]	items			= null;

	/**
	 * Bounds of this items in the current Gallery. X and Y values are used for vertical or
	 * horizontal offset depending on the Gallery settings. Only used when groups are enabled. Width
	 * and height
	 */
	// protected Rectangle bounds = new Rectangle(0, 0, 0, 0);
	protected int				x				= 0;

	protected int				y				= 0;

	/**
	 * Size of the group, including its title.
	 */
	protected int				width			= 0;

	protected int				height			= 0;

	protected int				marginBottom	= 0;

	protected int				hCount			= 0;

	protected int				vCount			= 0;

	/**
	 * Last result of indexOf( GalleryItem). Used for optimisation.
	 */
	protected int				lastIndexOf		= 0;

	/**
	 * True if the Gallery was created wih SWT.VIRTUAL
	 */
	private boolean				virtualGallery;

	private GalleryMT			parent;

	private GalleryMTItem		parentItem;

	/**
	 * Selection bit flags. Each 'int' contains flags for 32 items.
	 */
	protected int[]				selectionFlags	= null;

	protected Font				font;

	protected Color				foreground, background;

	private boolean				ultraLazyDummy	= false;

	/**
	 * 
	 */
	private boolean				expanded;

	public GalleryMTItem(final GalleryMT parent, final int style) {
		this(parent, style, -1, true);
	}

	public GalleryMTItem(final GalleryMT parent, final int style, final int index) {
		this(parent, style, index, true);
	}

	protected GalleryMTItem(final GalleryMT parent, final int style, final int index, final boolean create) {
		super(parent, style);
		this.parent = parent;
		this.parentItem = null;
		if ((parent.getStyle() & SWT.VIRTUAL) > 0) {
			virtualGallery = true;
		}

		if (create) {
			parent.addItem(this, index);
		}

	}

	public GalleryMTItem(final GalleryMTItem parent, final int style) {
		this(parent, style, -1, true);
	}

	public GalleryMTItem(final GalleryMTItem parent, final int style, final int index) {
		this(parent, style, index, true);
	}

	protected GalleryMTItem(final GalleryMTItem parent, final int style, final int index, final boolean create) {
		super(parent, style);

		this.parent = parent.parent;
		this.parentItem = parent;
		if ((parent.getStyle() & SWT.VIRTUAL) > 0) {
			virtualGallery = true;
		}

		if (create) {
			parent.addItem(this, index);
		}

	}

	private void _addItem(final GalleryMTItem item, final int position) {
		// TODO: ensure that there was no item at this position before using
		// this item in virtual mode

		// Insert item
		items = (GalleryMTItem[]) parent._arrayAddItem(items, item, position);

		// Update Gallery
		parent.updateStructuralValues(null, false);
		parent.updateScrollBarsProperties();
	}

	protected void _addSelection(final GalleryMTItem item) {
		// Deselect all items is multi selection is disabled
		if (!parent.multi) {
			_deselectAll();
		}

		if (item.getParentItem() == this) {

			final int index = indexOf(item);

			// Divide position by 32 to get selection bloc for this item.
			final int n = index >> 5;
			if (selectionFlags == null) {
				// Create selectionFlag array
				// Add 31 before dividing by 32 to ensure at least one 'int' is
				// created if size < 32.
				selectionFlags = new int[(items.length + 31) >> 5];
			} else if (n >= selectionFlags.length) {
				// Expand selectionArray
				final int[] oldFlags = selectionFlags;
				selectionFlags = new int[n + 1];
				System.arraycopy(oldFlags, 0, selectionFlags, 0, oldFlags.length);
			}

			// Get flag position in the 32 bit block and ensure is selected.
			selectionFlags[n] |= 1 << (index & 0x1f);

		}
	}

	protected void _deselectAll() {

		// Deselect groups
		// We could set selectionFlags to null, but we rather set all values to
		// 0 to redure garbage collection. On each iteration, we deselect 32
		// items.
		if (selectionFlags != null) {
			for (int i = 0; i < selectionFlags.length; i++) {
				selectionFlags[i] = 0;
			}
		}

		if (items == null) {
			return;
		}

		// Deselect group content.
		for (final GalleryMTItem item : items) {
			if (item != null) {
				item._deselectAll();
			}
		}
	}

	/**
	 * Disposes the gallery Item. This method is call directly by gallery and should not be used by
	 * a client
	 */
	protected void _dispose() {
		removeFromParent();
		_disposeChildren();
		super.dispose();
	}

	protected void _disposeChildren() {
		if (items != null) {
			while (items != null) {
				if (items[items.length - 1] != null) {
					items[items.length - 1]._dispose();
				} else {
					// This is an uninitialized item, just remove the slot
					parent._remove(this, items.length - 1);
				}
			}
		}
	}

	protected void _selectAll() {
		select(0, this.getItemCount() - 1);
	}

	public void _setExpanded(final boolean expanded, final boolean redraw) {
		this.expanded = expanded;
		parent.updateStructuralValues(this, false);
		parent.updateScrollBarsProperties();

		if (redraw) {
			parent.redraw();
		}
	}

	protected void addItem(final GalleryMTItem item, final int position) {
		if (position != -1 && (position < 0 || position > getItemCount())) {
			throw new IllegalArgumentException("ERROR_INVALID_RANGE"); //$NON-NLS-1$
		}
		_addItem(item, position);
	}

	/**
	 * Reset item values to defaults.
	 */
	public void clear() {
		checkWidget();
		// Clear all attributes
		text[0] = EMPTY_STRING;
		text[1] = EMPTY_STRING;
		text[2] = EMPTY_STRING;
		super.setImage(null);
		this.font = null;
		background = null;
		foreground = null;

		// Force redraw
		this.parent.redraw(this);
	}

	public void clearAll() {
		clearAll(false);
	}

	public void clearAll(final boolean all) {
		checkWidget();

		if (items == null) {
			return;
		}

		if (virtualGallery) {
			items = new GalleryMTItem[items.length];
		} else {
			for (final GalleryMTItem item : items) {
				if (item != null) {
					if (all) {
						item.clearAll(true);
					}
					item.clear();
				}
			}
		}
	}

	/**
	 * Deselect all children of this item
	 */
	public void deselectAll() {
		checkWidget();
		_deselectAll();
		parent.redraw();
	}

	@Override
	public void dispose() {
		checkWidget();

		removeFromParent();
		_disposeChildren();
		super.dispose();

		parent.updateStructuralValues(null, false);
		parent.updateScrollBarsProperties();
		parent.redraw();
	}

	/**
	 * Returns the receiver's background color.
	 * 
	 * @return The background color
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public Color getBackground() {
		return getBackground(false);
	}

	/**
	 * Returns the receiver's background color.
	 * 
	 * @param itemOnly
	 *            If TRUE, does not try to use renderer or parent widget to guess the real
	 *            background color. Note : FALSE is the default behavior.
	 * @return The background color
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public Color getBackground(final boolean itemOnly) {
		checkWidget();

		// If itemOnly, return this item's color attribute.
		if (itemOnly) {
			return this.background;
		}

		// Let the renderer decide the color.
		if (parent.getGroupRenderer() != null) {
			return parent.getGroupRenderer().getBackground(this);
		}

		// Default SWT behavior if no renderer.
		return background != null ? background : parent.getBackground();
	}

	/**
	 * Return the current bounds of the item. This method may return negative values if it is not
	 * visible.
	 * 
	 * @return
	 */
	public Rectangle getBounds() {
		// The y coords is relative to the client area because it may return
		// wrong values
		// on win32 when using the scroll bars. Instead, I use the absolute
		// position and make it relative using the current translation.

		if (parent.isVertical()) {
			return new Rectangle(x, y - parent.translate, width, height);
		}

		return new Rectangle(x - parent.translate, y, width, height);
	}

	/**
	 * @deprecated
	 * @return
	 */
	@Deprecated
	public String getDescription() {
		return getText(1);
	}

	public Font getFont() {
		return getFont(false);
	}

	public Font getFont(final boolean itemOnly) {
		checkWidget();

		if (itemOnly) {
			return font;
		}

		// Let the renderer decide the color.
		if (parent.getGroupRenderer() != null) {
			return parent.getGroupRenderer().getFont(this);
		}

		// Default SWT behavior if no renderer.
		return font != null ? font : parent.getFont();
	}

	/**
	 * Returns the receiver's foreground color.
	 * 
	 * @return The foreground color
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public Color getForeground() {
		return getForeground(false);
	}

	/**
	 * Returns the receiver's foreground color.
	 * 
	 * @param itemOnly
	 *            If TRUE, does not try to use renderer or parent widget to guess the real
	 *            foreground color. Note : FALSE is the default behavior.
	 * @return The foreground color
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public Color getForeground(final boolean itemOnly) {
		checkWidget();

		if (itemOnly) {
			return this.foreground;
		}

		// Let the renderer decide the color.
		if (parent.getGroupRenderer() != null) {
			return parent.getGroupRenderer().getForeground(this);
		}

		// Default SWT behavior if no renderer.
		return foreground != null ? foreground : parent.getForeground();
	}

	/**
	 * Searches the receiver's list starting at the first item (index 0) until an item is found that
	 * is equal to the argument, and returns the index of that item. <br/>
	 * If SWT.VIRTUAL is used and the item has not been used yet, the item is created and a
	 * SWT.SetData event is fired.
	 * 
	 * @param index
	 *            : index of the item.
	 * @return : the GalleryItem or null if index is out of bounds
	 */
	public GalleryMTItem getItem(final int index) {
		checkWidget();
		return parent._getItem(this, index);
	}

	/**
	 * Returns the number of items contained in the receiver that are direct item children of the
	 * receiver.
	 * 
	 * @return
	 */
	public int getItemCount() {

		if (items == null) {
			return 0;
		}

		return items.length;
	}

	public GalleryMTItem[] getItems() {
		checkWidget();
		if (items == null) {
			return new GalleryMTItem[0];
		}

		final GalleryMTItem[] itemsLocal = new GalleryMTItem[this.items.length];
		System.arraycopy(items, 0, itemsLocal, 0, this.items.length);

		return itemsLocal;
	}

	public GalleryMT getParent() {
		return parent;
	}

	public GalleryMTItem getParentItem() {
		return parentItem;
	}

	@Override
	public String getText() {
		return getText(0);
	}

	public String getText(final int index) {
		checkWidget();
		return text[index];
	}

	/**
	 * Returns the index of childItem within this item or -1 if childItem is not found. The search
	 * is only one level deep.
	 * 
	 * @param childItem
	 * @return
	 */
	public int indexOf(final GalleryMTItem childItem) {
		checkWidget();

		return parent._indexOf(this, childItem);
	}

	/**
	 * Returns true if the receiver is expanded, and false otherwise.
	 * 
	 * @return
	 */
	public boolean isExpanded() {
		return expanded;
	}

	public boolean isSelected(final GalleryMTItem item) {
		if (item == null) {
			return false;
		}

		if (item.getParentItem() == this) {
			if (selectionFlags == null) {
				return false;
			}

			final int index = indexOf(item);
			final int n = index >> 5;
			if (n >= selectionFlags.length) {
				return false;
			}
			final int flags = selectionFlags[n];
			return flags != 0 && (flags & 1 << (index & 0x1f)) != 0;
		}
		return false;
	}

	protected boolean isUltraLazyDummy() {
		return ultraLazyDummy;
	}

	public void remove(final GalleryMTItem item) {
		remove(indexOf(item));
	}

	public void remove(final int index) {
		checkWidget();
		parent._remove(this, index);

		parent.updateStructuralValues(null, false);
		parent.updateScrollBarsProperties();
		parent.redraw();
	}

	protected void removeFromParent() {
		if (parentItem != null) {
			final int index = parent._indexOf(parentItem, this);
			parent._remove(parentItem, index);
		} else {
			final int index = parent._indexOf(this);
			parent._remove(index);
		}
	}

	protected void select(final int from, final int to) {
		if (GalleryMT.DEBUG)
		 {
			System.out.println("GalleryItem.select(  " + from + "," + to + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		for (int i = from; i <= to; i++) {
			final GalleryMTItem item = getItem(i);
			parent._addSelection(item);
			item._selectAll();
		}
	}

	/**
	 * Selects all of the items in the receiver.
	 */
	public void selectAll() {
		checkWidget();
		_selectAll();
		parent.redraw();
	}

	/**
	 * Sets the receiver's background color to the color specified by the argument, or to the
	 * default system color for the item if the argument is null.
	 * 
	 * @param color
	 *            The new color (or null)
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_INVALID_ARGUMENT - if the argument has been disposed</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public void setBackground(final Color background) {
		checkWidget();
		if (background != null && background.isDisposed()) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.background = background;
		this.parent.redraw(this);
	}

	/**
	 * @param description
	 * @deprecated
	 */
	@Deprecated
	public void setDescription(final String description) {
		setText(1, description);
	}

	/**
	 * Sets the expanded state of the receiver.
	 * 
	 * @param expanded
	 */
	public void setExpanded(final boolean expanded) {
		checkWidget();
		_setExpanded(expanded, true);
	}

	public void setFont(final Font font) {
		checkWidget();
		if (font != null && font.isDisposed()) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.font = font;
		this.parent.redraw(this);
	}

	/**
	 * Sets the receiver's foreground color to the color specified by the argument, or to the
	 * default system color for the item if the argument is null.
	 * 
	 * @param color
	 *            The new color (or null)
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_INVALID_ARGUMENT - if the argument has been disposed</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public void setForeground(final Color foreground) {
		checkWidget();
		if (foreground != null && foreground.isDisposed()) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.foreground = foreground;
		this.parent.redraw(this);
	}

	@Override
	public void setImage(final Image image) {
		super.setImage(image);
		parent.redraw(this);
	}

	/**
	 * Only work when the table was created with SWT.VIRTUAL
	 * 
	 * @param itemCount
	 */
	public void setItemCount(final int count) {
		if (count == 0) {
			// No items
			items = null;
		} else {
			// At least one item, create a new array and copy data from the
			// old one.
			final GalleryMTItem[] newItems = new GalleryMTItem[count];
			if (items != null) {
				System.arraycopy(items, 0, newItems, 0, Math.min(count, items.length));
			}
			items = newItems;
		}
	}

	protected void setParent(final GalleryMT parent) {
		this.parent = parent;
	}

	protected void setParentItem(final GalleryMTItem parentItem) {
		this.parentItem = parentItem;
	}

	public void setText(final int index, final String string) {
		checkWidget();
		if (string == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		text[index] = string;
		parent.redraw(this);
	}

	@Override
	public void setText(final String string) {
		setText(0, string);
	}

	protected void setUltraLazyDummy(final boolean ultraLazyDummy) {
		this.ultraLazyDummy = ultraLazyDummy;
	}

}
