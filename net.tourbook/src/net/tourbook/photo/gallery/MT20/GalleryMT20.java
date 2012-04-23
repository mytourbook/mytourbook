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

import net.tourbook.photo.manager.Photo;
import net.tourbook.ui.UI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * This gallery has it's origin in http://www.eclipse.org/nebula/widgets/gallery/gallery.php but has
 * been modified in many areas, like grouping has been removed, filter has been added.
 */
public abstract class GalleryMT20 extends Canvas {

	private boolean							_isVertical;
	private boolean							_isMultiSelection;

	private boolean							_isGalleryMoved;

	/**
	 * Current gallery top/left position (this is also the scroll bar position). Can be used by
	 * renderer during paint.
	 */
	private int								_galleryPosition		= 0;

	private Double							_galleryPositionWhenUpdated;

	/**
	 * When <code>true</code> images are painted in higher quality but must be larger than the high
	 * quality minimum size which is set in {@link #setImageQuality(boolean, int)}.
	 */
	private boolean							_isShowHighQuality;

	/**
	 * When images size (height) is larger than this values, the images are painted with high
	 * quality in a 2nd run.
	 */
	private int								_highQualityMinSize;

	/**
	 * Image quality : interpolation
	 */
	private int								_interpolation			= SWT.HIGH;

	/**
	 * Image quality : antialias
	 */
	private int								_antialias				= SWT.ON;

	/*
	 * width and height for the whole gallery
	 */
	private int								_contentVirtualHeight	= 0;

	private int								_contentVirtualWidth	= 0;
	private int								_prevGalleryPosition;

	private int								_prevViewportWidth;
	private int								_prevViewportHeight;
	private int								_prevContentHeight;
	private int								_prevContentWidth;

	private AbstractGalleryMT20ItemRenderer	_itemRenderer;

	/**
	 * Cached client area
	 */
	private Rectangle						_clientArea;

	private Composite						_parent;

	private ControlAdapter					_parentControlListener;
	private int								_higherQualityDelay;

	private RedrawTimer						_redrawTimer			= new RedrawTimer();

	/**
	 * Contains items which are displayed in the gallery. Initially the items are not set because
	 * they are virtual until they are displayed.
	 */
	private GalleryMT20Item[]				_galleryItems;

	/**
	 * Contains items which are currently be visible in the viewport area. This is used to stop
	 * loading images which are not displayed.
	 */
	private GalleryMT20Item[]				_visibleGalleryItems;

	private GalleryMT20Item[]				_selectedItems			= null;

	/**
	 * Selection bit flags. Each 'int' contains flags for 32 items.
	 */
	private int[]							selectionFlags			= null;

	private int								_itemWidth				= 80;
	private int								_itemHeight				= (int) (_itemWidth * (float) 15 / 11);
	private double							_itemRatio				= (double) _itemWidth / _itemHeight;

	/**
	 * @return Returns minimum gallery item width or <code>-1</code> when value is not set.
	 */
	private int								_minItemWidth			= -1;

	/**
	 * @return Returns maximum gallery item width or <code>-1</code> when value is not set.
	 */
	private int								_maxItemWidth			= -1;

	private int								_numberOfHorizItems;
	private int								_numberOfVertItems;

	/**
	 * Is <code>true</code> during zooming. OSX do fire a mouse wheel event always, Win do fire a
	 * mouse wheel event when scrollbars are not visible.
	 * <p>
	 * Terrible behaviour !!!
	 */
	private boolean							_isZoomed;

	/**
	 * Keeps track of the last selected item. This is necessary to support "Shift+Mouse button"
	 * where we have to select all items between the previous and the current item and keyboard
	 * navigation.
	 */
	private GalleryMT20Item					lastSingleClick			= null;

	private Point							_mouseMovePosition;
	private Point							_mousePanStartPosition;
	private int								_lastZoomEventTime;
	private boolean							_mouseClickHandled;
	private boolean							_isGalleryPanned;

	private class RedrawTimer implements Runnable {
		public void run() {
			redraw();
		}
	}

	/**
	 * Create a Gallery
	 * 
	 * @param parent
	 * @param style
	 *            SWT.V_SCROLL add vertical slider and switches to vertical mode. <br/>
	 *            SWT.H_SCROLL add horizontal slider and switches to horizontal mode. <br/>
	 *            if both V_SCROLL and H_SCROLL are specified, the gallery is in vertical mode by
	 *            default. Mode can be changed afterward using setVertical<br/>
	 *            SWT.MULTI allows only several items to be selected at the same time.
	 */
	public GalleryMT20(final Composite parent, final int style) {

		super(parent, style | SWT.DOUBLE_BUFFERED);

		_isVertical = (style & SWT.V_SCROLL) > 0;
		_isMultiSelection = (style & SWT.MULTI) > 0;

		_clientArea = getClientArea();

		setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));

		// Add listeners : redraws, mouse and keyboard
		_addDisposeListeners();
		_addResizeListeners();
		_addPaintListeners();
		_addScrollBarsListeners();
		_addMouseListeners();
		_addKeyListeners();

		// set item renderer
		_itemRenderer = new DefaultGalleryMT20ItemRenderer();

		updateGallery(false);
	}

	/**
	 * Add internal dispose listeners to this gallery.
	 */
	private void _addDisposeListeners() {
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	private void _addKeyListeners() {
		addKeyListener(new KeyListener() {

			public void keyPressed(final KeyEvent e) {

				switch (e.keyCode) {
				case SWT.ARROW_LEFT:
				case SWT.ARROW_RIGHT:
				case SWT.ARROW_UP:
				case SWT.ARROW_DOWN:
				case SWT.PAGE_UP:
				case SWT.PAGE_DOWN:
				case SWT.HOME:
				case SWT.END:
//					final GalleryMT20Item newItem = groupRenderer.getNextItem(lastSingleClick, e.keyCode);
//
//					if (newItem != null) {
//						_deselectAll(false);
//						setSelected(newItem, true, true);
//						lastSingleClick = newItem;
//						_showItem(newItem);
//						redraw();
//					}

					break;

				case SWT.CR:

//					final GalleryMTFilterItem[] selection = getSelection();
//					GalleryMTFilterItem item = null;
//
//					if (selection != null && selection.length > 0) {
//						item = selection[0];
//					}
//
//					notifySelectionListeners(item, 0, true);
					break;
				}
			}

			public void keyReleased(final KeyEvent e) {
				// Nothing yet.
			}

		});
	}

	/**
	 * Add internal mouse listeners to this gallery.
	 */
	private void _addMouseListeners() {

		addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseScrolled(final MouseEvent event) {

				_isZoomed = false;

				/**
				 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
				 * <br>
				 * This event is fired ONLY when the scrollbars are not visible, on Win 7<br>
				 * <br>
				 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				 */

				boolean isShift;
				boolean isCtrl;
				if (UI.IS_OSX) {
					isShift = (event.stateMask & SWT.ALT) != 0;
					isCtrl = (event.stateMask & SWT.COMMAND) != 0;
				} else {
					isShift = (event.stateMask & SWT.SHIFT) != 0;
					isCtrl = (event.stateMask & SWT.CTRL) != 0;
				}

				/*
				 * ensure <ctrl> or <shift> is pressed, otherwise it is zoomed when the scrollbar is
				 * hidden
				 */
				if (isCtrl || isShift) {
					zoomImage(event.time, event.count > 0, isShift, isCtrl);
					_isZoomed = true;
				}
			}
		});

		addMouseListener(new MouseListener() {

			public void mouseDoubleClick(final MouseEvent e) {
				onMouseDoubleClick(e);
			}

			public void mouseDown(final MouseEvent e) {
				onMouseDown(e);
			}

			public void mouseUp(final MouseEvent e) {
				onMouseUp(e);
			}

		});

		addMouseMoveListener(new MouseMoveListener() {

			@Override
			public void mouseMove(final MouseEvent e) {
				onMouseMove(e);
			}
		});
	}

	/**
	 * Add internal paint listeners to this gallery.
	 */
	private void _addPaintListeners() {
		addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent event) {
				onPaint(event.gc);
			}
		});
	}

	/**
	 * Add internal resize listeners to this gallery.
	 */
	private void _addResizeListeners() {

		addControlListener(new ControlAdapter() {

			@Override
			public void controlResized(final ControlEvent event) {

				_clientArea = getClientArea();

				updateGallery(true);
			}
		});

		Composite parent = getParent();
		while (parent != null) {

			_parent = parent;
			parent = parent.getParent();
		}

		_parentControlListener = new ControlAdapter() {

			@Override
			public void controlMoved(final ControlEvent e) {
				// makes moving the shell not perfect but better
				_isGalleryMoved = true;
			}
		};
		_parent.addControlListener(_parentControlListener);

	}

	/**
	 * Add internal scrollbars listeners to this gallery.
	 */
	private void _addScrollBarsListeners() {

		// Vertical bar
		final ScrollBar verticalBar = getVerticalBar();
		if (verticalBar != null) {

			verticalBar.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(final SelectionEvent event) {
					onScrollVertical(event);
				}
			});
		}

		// Horizontal bar

		final ScrollBar horizontalBar = getHorizontalBar();
		if (horizontalBar != null) {
			horizontalBar.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(final SelectionEvent event) {
					if (!_isVertical) {
						onScrollHorizontal();
					}
				}
			});
		}

	}

	/**
	 * Deselects all items and send selection event depending on parameter.
	 * 
	 * @param notifyListeners
	 *            If true, a selection event will be sent to all the current selection listeners.
	 */
	protected void _deselectAll(final boolean notifyListeners) {

		_selectedItems = null;
		// Deselect groups
		// We could set selectionFlags to null, but we rather set all values to
		// 0 to redure garbage collection. On each iteration, we deselect 32
		// items.
		if (selectionFlags != null) {
			for (int i = 0; i < selectionFlags.length; i++) {
				selectionFlags[i] = 0;
			}
		}

//		if (_galleryRootItems == null) {
//			return;
//		}
//		for (final GalleryMT20Item item : _galleryRootItems) {
//			if (item != null) {
//				item._deselectAll();
//			}
//		}
//
//		// Notify listeners if necessary.
//		if (notifyListeners) {
//			notifySelectionListeners(null, -1, false);
//		}
	}

	/**
	 * Sets number of vertical and horizontal items for the whole gallery
	 */
	private void computeNumberOfVertHorizItems() {

		if (_isVertical) {

			final Point vhNumbers = computeNumberOfVertHorizItems(_clientArea.width, _itemWidth);

			_numberOfHorizItems = vhNumbers.x;
			_numberOfVertItems = vhNumbers.y;

		} else {

			final Point vhNumbers = computeNumberOfVertHorizItems(_clientArea.height, _itemHeight);
			_numberOfHorizItems = vhNumbers.y;
			_numberOfVertItems = vhNumbers.x;
		}
	}

	/**
	 * Calculate how many items are displayed horizontally and vertically.
	 * 
	 * @param visibleSize
	 * @param itemSize
	 * @return
	 */
	private Point computeNumberOfVertHorizItems(final int visibleSize, final int itemSize) {

		if (_galleryItems == null) {
			return new Point(0, 0);
		}

		final int numberOfItems = _galleryItems.length;
		if (numberOfItems == 0) {
			return new Point(0, 0);
		}

		int x = visibleSize / itemSize;
		int y = 0;

		if (x > 0) {
			y = (int) Math.ceil((double) numberOfItems / (double) x);
		} else {
			// Show at least one item;
			y = numberOfItems;
			x = 1;
		}

		return new Point(x, y);
	}

	/**
	 * Deselects all items.
	 */
	public void deselectAll() {

		_deselectAll(false);

		redraw();
	}

	/**
	 * @return Returns gallery relative position
	 */
	public double getGalleryPosition() {

		if (_isVertical) {

			final ScrollBar vBar = getVerticalBar();
			if (vBar == null) {
				return 1;
			}

			final int selection = vBar.getSelection();
			final int maximum = vBar.getMaximum();

			return (double) selection / maximum;

		} else {

			final ScrollBar hBar = getHorizontalBar();
			if (hBar == null) {
				return 1;
			}

			final int selection = hBar.getSelection();
			final int maximum = hBar.getMaximum();

			return (double) selection / maximum;
		}
	}

	private GalleryMT20Item getItem(final int itemIndex) {

		GalleryMT20Item galleryItem = _galleryItems[itemIndex];

		if (galleryItem == null) {

			galleryItem = new GalleryMT20Item(this);
			_galleryItems[itemIndex] = galleryItem;

			initItem(galleryItem, itemIndex);
		}

		System.out.println("\t" + ((Photo) galleryItem.data).getFileName());
		// TODO remove SYSTEM.OUT.PRINTLN

		return galleryItem;
	}

	/**
	 * Get item at pixel position
	 * 
	 * @param coords
	 * @return GalleryItem or null
	 */
	public GalleryMT20Item getItem(final Point coords) {

		final int pos = _isVertical ? (coords.y + _galleryPosition) : (coords.x + _galleryPosition);

//		final GalleryMT20Item group = _getGroup(coords);
//		if (group != null) {
//			return groupRenderer.getItem(group, new Point(_isVertical ? coords.x : pos, _isVertical ? pos : coords.y));
//		}

		return null;
	}

	public int getScrollBarIncrement() {

		if (_isVertical) {
			// Vertical fill
			return _clientArea.height;
		} else {

			// Horizontal fill
			return _clientArea.width;
		}

//		// Standard behavior
//		return 16;
	}

	public GalleryMT20Item[] getVisibleGalleryItems() {
		return _visibleGalleryItems;
	}

	/**
	 * Original method: AbstractGridGroupRenderer.getVisibleItems()
	 * 
	 * @param clippingArea
	 * @return Returns indices for all visible gallery items contained in the clipping area.
	 */
	private int[] getVisibleItems(final Rectangle clippingArea) {

		final int clipX = clippingArea.x;
		final int clipY = clippingArea.y;
		final int clipWidth = clippingArea.width;
		final int clipHeight = clippingArea.height;

		int[] indexes;

		if (_isVertical) {

			int firstLine = (clipY + _galleryPosition) / _itemHeight;
			if (firstLine < 0) {
				firstLine = 0;
			}

			int lastLine = (clipY + _galleryPosition + clipHeight) / _itemHeight;
			if (lastLine < firstLine) {
				lastLine = firstLine;
			}

			int firstItem;
			int lastItem;

			if (clipWidth == _itemWidth) {

				// optimize when only 1 item is visible in the clipping area

				final int horizontalItem = (clipX) / _itemWidth;

				firstItem = firstLine * _numberOfHorizItems;
				firstItem += horizontalItem;

				lastItem = firstItem + 1;

			} else {

				firstItem = firstLine * _numberOfHorizItems;
				lastItem = (lastLine + 1) * _numberOfHorizItems;
			}

			// ensure number of available items
			final int numberOfItems = _galleryItems.length;
			if (lastItem > numberOfItems) {
				lastItem = numberOfItems;
				if (firstItem > lastItem) {
					firstItem = lastItem;
				}
			}

			// exit if no item selected
			final int itemsCount = lastItem - firstItem;
			if (itemsCount == 0) {
				return null;
			}

			indexes = new int[itemsCount];
			for (int itemIndex = 0; itemIndex < itemsCount; itemIndex++) {
				indexes[itemIndex] = firstItem + itemIndex;
			}

			System.out.println("first:" + firstItem + "\tlast:" + lastItem);
			// TODO remove SYSTEM.OUT.PRINTLN

		} else {

			int firstLine = (clipX + _galleryPosition) / _itemWidth;
			if (firstLine < 0) {
				firstLine = 0;
			}

			int firstItem = firstLine * _numberOfVertItems;

			int lastLine = (clipX + _galleryPosition + clipWidth) / _itemWidth;

			if (lastLine < firstLine) {
				lastLine = firstLine;
			}

			int lastItem = (lastLine + 1) * _numberOfVertItems;

			// ensure number of available items
			final int numberOfItems = _galleryItems.length;
			if (lastItem > numberOfItems) {
				lastItem = numberOfItems;
				if (firstItem > lastItem) {
					firstItem = lastItem;
				}
			}

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
	 * Initializes a gallery item which can be used to set data into the item. This method is called
	 * before a gallery item is painted.
	 * 
	 * @param galleryItem
	 * @param itemIndex
	 */
	public abstract void initItem(final GalleryMT20Item galleryItem, final int itemIndex);

	private boolean isSelected(final GalleryMT20Item item) {

//		if (item == null) {
		return false;
//		}
//
//		if (item.getParentItem() != null) {
//			return item.getParentItem().isSelected(item);
//		}
//
//		if (selectionFlags == null) {
//			return false;
//		}
//
//		final int index = indexOf(item);
//		final int n = index >> 5;
//		if (n >= selectionFlags.length) {
//			return false;
//		}
//		final int flags = selectionFlags[n];
//		return flags != 0 && (flags & 1 << (index & 0x1f)) != 0;

	}

	public void keepGalleryPosition() {

		final double currentPos = getGalleryPosition();
		setGalleryPositionWhenUpdated(currentPos);
	}

	/**
	 * Send a selection event for a gallery item
	 * 
	 * @param item
	 */
	private void notifySelectionListeners(final GalleryMT20Item item, final int index, final boolean isDefault) {

//		final Event e = new Event();
//		e.widget = this;
//		e.item = item;
//		if (item != null) {
//			e.data = item.getData();
//		}
//
//		try {
//			if (isDefault) {
//				notifyListeners(SWT.DefaultSelection, e);
//			} else {
//				notifyListeners(SWT.Selection, e);
//			}
//		} catch (final RuntimeException ex) {
//			ex.printStackTrace();
//		}
	}

	/**
	 * Send a zoom in/out event with {@link SWT#Modify} (found no better SWT event)
	 * 
	 * @param itemWidth
	 * @param itemHeight
	 */
	private void notifyZoomListener(final int itemWidth, final int itemHeight) {

		final Event e = new Event();
		e.widget = this;
		e.width = itemWidth;
		e.height = itemHeight;

		try {
			notifyListeners(SWT.Modify, e);
		} catch (final RuntimeException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Clean up the Gallery and renderers on dispose.
	 */
	private void onDispose() {

		// dispose renderer
		if (_itemRenderer != null) {
			_itemRenderer.dispose();
		}

		if (_parent != null) {
			_parent.removeControlListener(_parentControlListener);
		}
	}

	private void onMouseDoubleClick(final MouseEvent e) {

		final GalleryMT20Item item = getItem(new Point(e.x, e.y));
		if (item != null) {
			notifySelectionListeners(item, 0, true);
		}
		_mouseClickHandled = true;
	}

	private void onMouseDown(final MouseEvent e) {

		_mouseClickHandled = false;

//		if (!_mouseDown(e)) {
//			mouseClickHandled = true;
//			return;
//		}

		final GalleryMT20Item item = getItem(new Point(e.x, e.y));

		if (e.button == 1) {

			// left mouse button is pressed

			if (item == null) {
				_deselectAll(true);
				redraw();
				_mouseClickHandled = true;
				lastSingleClick = null;
			} else {
				if ((e.stateMask & SWT.MOD1) > 0) {
					onMouseHandleLeftMod1(e, item, true, false);
				} else if ((e.stateMask & SWT.SHIFT) > 0) {
					onMouseHandleLeftShift(e, item, true, false);
				} else {
					onMouseHandleLeft(e, item, true, false);
				}
			}

			// keep position to pan the gallery
			_isGalleryPanned = true;
			_mousePanStartPosition = new Point(e.x, e.y);

		} else if (e.button == 3) {
			onMouseHandleRight(e, item, true, false);
		}
	}

	private void onMouseHandleLeft(final MouseEvent e, final GalleryMT20Item item, final boolean down, final boolean up) {
		if (down) {
			if (!isSelected(item)) {
				_deselectAll(false);

				setSelected(item, true, true);

				lastSingleClick = item;
				redraw();
				_mouseClickHandled = true;
			}
		} else if (up) {
			if (item == null) {
				_deselectAll(true);
			} else {

				_deselectAll(false);
				setSelected(item, true, lastSingleClick != item);
				lastSingleClick = item;
			}
			redraw();
		}
	}

	private void onMouseHandleLeftMod1(	final MouseEvent e,
										final GalleryMT20Item item,
										final boolean down,
										final boolean up) {
		if (up) {
			// if (lastSingleClick != null) {
			if (item != null) {
				setSelected(item, !isSelected(item), true);
				lastSingleClick = item;
				redraw();
			}
			// }
		}
	}

	private void onMouseHandleLeftShift(final MouseEvent e,
										final GalleryMT20Item item,
										final boolean down,
										final boolean up) {
		if (up) {
			if (lastSingleClick != null) {
				_deselectAll(false);

//				if (getOrder(item, lastSingleClick)) {
//					select(item, lastSingleClick);
//				} else {
//					select(lastSingleClick, item);
//				}
			}
		}
	}

	/**
	 * Handle right click.
	 * 
	 * @param e
	 * @param item
	 *            : The item which is under the cursor or null
	 * @param down
	 * @param up
	 */
	private void onMouseHandleRight(final MouseEvent e, final GalleryMT20Item item, final boolean down, final boolean up) {
		if (down) {

			if (item != null && !isSelected(item)) {
				_deselectAll(false);
				setSelected(item, true, true);
				redraw();
				_mouseClickHandled = true;
			}
		}
	}

	private void onMouseMove(final MouseEvent e) {

		final int mouseX = e.x;
		final int mouseY = e.y;
		_mouseMovePosition = new Point(mouseX, mouseY);

		if (_isGalleryPanned) {

			// gallery is panned

			if (_isVertical) {

				if (_contentVirtualHeight > _clientArea.height) {

					// image is higher than client area

					final int yDiff = mouseY - _mousePanStartPosition.y;

					final ScrollBar verticalBar = getVerticalBar();

					final int oldBarSelection = verticalBar.getSelection();
					final int newBarSelection = oldBarSelection - yDiff;

					verticalBar.setSelection(newBarSelection);

					onScrollVertical_10();
				}
			} else {

				// not yet implemented
			}

			// set down position to current mouse position
			_mousePanStartPosition = _mouseMovePosition;
		}
	}

	private void onMouseUp(final MouseEvent e) {

		_isGalleryPanned = false;

		if (_mouseClickHandled) {
			return;
		}

		if (e.button == 1) {

			final GalleryMT20Item item = getItem(new Point(e.x, e.y));
			if (item == null) {
				return;
			}

			if ((e.stateMask & SWT.MOD1) > 0) {
				onMouseHandleLeftMod1(e, item, false, true);
			} else if ((e.stateMask & SWT.SHIFT) > 0) {
				onMouseHandleLeftShift(e, item, false, true);
			} else {
				onMouseHandleLeft(e, item, false, true);
			}
		}
	}

	private void onPaint(final GC gc) {

//		final long start = System.nanoTime();

		// is true when image can not be painted with high quality
		final boolean isSmallerThanHQMinSize = _itemWidth < _highQualityMinSize;

		// check if the content is scrolled or window is resized
		final boolean isScrolled = _galleryPosition != _prevGalleryPosition;

		final boolean isContentResized = _prevContentHeight != _contentVirtualHeight
				|| _prevContentWidth != _contentVirtualWidth;

		final boolean isWindowResized = _prevViewportWidth != _clientArea.width
				|| _prevViewportHeight != _clientArea.height;

		final boolean isLowQualityPainting = isScrolled
				|| isWindowResized
				|| isContentResized
				|| _isGalleryMoved
				|| isSmallerThanHQMinSize;

		// reset state
		_isGalleryMoved = false;

		try {

			final Rectangle clippingArea = gc.getClipping();

			System.out.println("\t");
			System.out.println("clipping: " + clippingArea);
			// TODO remove SYSTEM.OUT.PRINTLN

			if (isLowQualityPainting) {
				gc.setAntialias(SWT.OFF);
				gc.setInterpolation(SWT.OFF);
			} else {
				gc.setAntialias(_antialias);
				gc.setInterpolation(_interpolation);
			}

			gc.setBackground(getBackground());

			// get visible items in the clipping area
			final int[] visibleIndexes = getVisibleItems(clippingArea);
			if (visibleIndexes != null) {

				final int numberOfVisibleItems = visibleIndexes.length;

				_visibleGalleryItems = new GalleryMT20Item[numberOfVisibleItems];

				if (numberOfVisibleItems > 0) {

					for (int itemIndex = numberOfVisibleItems - 1; itemIndex >= 0; itemIndex--) {

						final GalleryMT20Item galleryItem = getItem(visibleIndexes[itemIndex]);

						_visibleGalleryItems[itemIndex] = galleryItem;

//						final boolean isSelected = isSelected(galleryItem);
						final boolean isSelected = false;

						onPaint_10_DrawItem(gc, galleryItem, itemIndex, isSelected);
					}
				}
			}

		} catch (final Exception e) {
			// We can't let onPaint throw an exception because unexpected
			// results may occur in SWT.
			e.printStackTrace();
		}

		// When lowQualityOnUserAction is enabled, keep last state and wait
		// before updating with a higher quality
		if (_isShowHighQuality) {

			_prevGalleryPosition = _galleryPosition;

			_prevViewportWidth = _clientArea.width;
			_prevViewportHeight = _clientArea.height;

			_prevContentHeight = _contentVirtualHeight;
			_prevContentWidth = _contentVirtualWidth;

			if (isLowQualityPainting && isSmallerThanHQMinSize == false) {
				// Calling timerExec with the same object just delays the
				// execution (doesn't run twice)
				getDisplay().timerExec(_higherQualityDelay, _redrawTimer);
			}
		}

//		final float timeDiff = (float) (System.nanoTime() - start) / 1000000;
//		if (timeDiff > 10) {}
//		System.out.println("onPaint:\t" + timeDiff + " ms\t" + clipping);
		// TODO remove SYSTEM.OUT.PRINTLN
	}

	/**
	 * Original method: AbstractGridGroupRenderer.drawItem()
	 * 
	 * @param gc
	 * @param galleryItem
	 * @param itemIndex
	 * @param isSelected
	 */
	private void onPaint_10_DrawItem(	final GC gc,
										final GalleryMT20Item galleryItem,
										final int itemIndex,
										final boolean isSelected) {

		int numberOfItemsX;
		int numberOfItemsY;
		if (_isVertical) {
			numberOfItemsX = itemIndex % _numberOfHorizItems;
			numberOfItemsY = (itemIndex - numberOfItemsX) / _numberOfHorizItems;
		} else {
			numberOfItemsY = itemIndex % _numberOfVertItems;
			numberOfItemsX = (itemIndex - numberOfItemsY) / _numberOfVertItems;
		}

		int viewPortX;
		int viewPortY;

		final int virtualPosX = numberOfItemsX * _itemWidth;
		final int virtualPosY = numberOfItemsY * _itemHeight;

		if (_isVertical) {

			viewPortX = virtualPosX;
			viewPortY = virtualPosY - _galleryPosition;

		} else {

			viewPortX = virtualPosX - _galleryPosition;
			viewPortY = virtualPosY;
		}

		galleryItem.virtualPosX = virtualPosX;
		galleryItem.virtualPosY = virtualPosY;
		galleryItem.height = _itemHeight;
		galleryItem.width = _itemWidth;

		gc.setClipping(viewPortX, viewPortY, _itemWidth, _itemHeight);

		_itemRenderer.draw(gc, galleryItem, itemIndex, viewPortX, viewPortY, _itemWidth, _itemHeight, isSelected);

//		// Drawing area
//		final int galleryPosX = _isVertical ? groupItem.x : groupItem.x - _galleryPosition;
//		final int galleryPosY = _isVertical ? groupItem.y - _galleryPosition : groupItem.y;
//
//		final Rectangle clipping = gc.getClipping();
//		final Rectangle previousClipping = new Rectangle(clipping.x, clipping.y, clipping.width, clipping.height);
//
//		clipping.intersect(new Rectangle(galleryPosX, galleryPosY, groupItem.width, groupItem.height));
//		gc.setClipping(clipping);
//		{
//			// Draw group
//			groupRenderer.draw(
//					gc,
//					groupItem,
//					galleryPosX,
//					galleryPosY,
//					clipping.x,
//					clipping.y,
//					clipping.width,
//					clipping.height);
//		}
//		gc.setClipping(previousClipping);

	}

	/**
	 * Original method: AbstractGridGroupRenderer.drawItem()
	 * 
	 * @param gc
	 * @param galleryItem
	 * @param itemIndex
	 * @param isSelected
	 */
	private void onPaint_10_DrawItem_OLD(	final GC gc,
											final GalleryMT20Item galleryItem,
											final int itemIndex,
											final boolean isSelected) {

		int posX, posY;
		if (_isVertical) {
			posX = itemIndex % _numberOfHorizItems;
			posY = (itemIndex - posX) / _numberOfHorizItems;
		} else {
			posY = itemIndex % _numberOfVertItems;
			posX = (itemIndex - posY) / _numberOfVertItems;
		}

		final int scrollbarPosition = _galleryPosition;
		int itemViewPortX;
		int itemViewportY;

		if (_isVertical) {

			itemViewPortX = posX * (_itemWidth);
			itemViewportY = (posY * (_itemHeight) - scrollbarPosition);

			galleryItem.virtualPosX = itemViewPortX;
			galleryItem.virtualPosY = itemViewportY + scrollbarPosition;

		} else {

			itemViewPortX = (posX * (_itemWidth) - scrollbarPosition);
			itemViewportY = posY * (_itemHeight);

			galleryItem.virtualPosX = itemViewPortX + scrollbarPosition;
			galleryItem.virtualPosY = itemViewportY;
		}

		galleryItem.height = _itemHeight;
		galleryItem.width = _itemWidth;

		gc.setClipping(itemViewPortX, itemViewportY, _itemWidth, _itemHeight);

		_itemRenderer.draw(
				gc,
				galleryItem,
				itemIndex,
				itemViewPortX,
				itemViewportY,
				_itemWidth,
				_itemHeight,
				isSelected);
	}

	private void onScrollHorizontal() {

		final int areaWidth = _clientArea.width;
		if (_contentVirtualWidth > areaWidth) {

			// image is higher than client area

			final int barSelection = getHorizontalBar().getSelection();
			scroll(_galleryPosition - barSelection, 0, 0, 0, areaWidth, _clientArea.height, false);
			_galleryPosition = barSelection;

		} else {
			_galleryPosition = 0;
		}

	}

	private void onScrollVertical(final SelectionEvent event) {

		if (_isZoomed == false) {

			/*
			 * Zooming can happen in the mouse wheel event before the selection event is fired. When
			 * it is already zoomed, no other action should be done.
			 */

			boolean isShift;
			boolean isCtrl;
			if (UI.IS_OSX) {
				isShift = (event.stateMask & SWT.ALT) != 0;
				isCtrl = (event.stateMask & SWT.COMMAND) != 0;
			} else {
				isShift = (event.stateMask & SWT.SHIFT) != 0;
				isCtrl = (event.stateMask & SWT.CTRL) != 0;
			}

			/*
			 * ensure <ctrl> or <shift> is pressed, otherwise it is zoomed when the scrollbar is
			 * hidden
			 */
			if (isCtrl || isShift) {

				/*
				 * the scrollbar has already been move to a new position with the mouse wheel, first
				 * the scollbar position must be reverted to the current gallery position
				 */
				updateScrollBars();

				final boolean isZoomIn = event.detail == SWT.ARROW_UP;

				zoomImage(event.time, isZoomIn, isShift, isCtrl);

			} else {

				if (_isVertical) {

					onScrollVertical_10();
				}
			}
		}

		_isZoomed = false;
	}

	private void onScrollVertical_10() {

		final int areaHeight = _clientArea.height;

		if (_contentVirtualHeight > areaHeight) {

			// content is larger than visible client area

			final ScrollBar verticalBar = getVerticalBar();

			final int barSelection = verticalBar.getSelection();
			final int destY = _galleryPosition - barSelection;

			scroll(0, destY, 0, 0, _clientArea.width, areaHeight, false);

			_galleryPosition = barSelection;

		} else {
			_galleryPosition = 0;
		}
	}

	/**
	 * Sets the gallery's anti-aliasing value to the parameter, which must be one of
	 * <code>SWT.DEFAULT</code>, <code>SWT.OFF</code> or <code>SWT.ON</code>.
	 * 
	 * @param antialias
	 */
	public void setAntialias(final int antialias) {
		_antialias = antialias;
	}

	/**
	 * @param newPosition
	 *            Relative position
	 */
	public void setGalleryPositionWhenUpdated(final Double newPosition) {

//		System.out.println("gal pos: " + newPosition);
//		// TODO remove SYSTEM.OUT.PRINTLN

		_galleryPositionWhenUpdated = newPosition;
	}

	/**
	 * Set the delay after the last user action before the redraw at higher quality is triggered
	 * 
	 * @see #setLowQualityOnUserAction(boolean)
	 * @param higherQualityDelay
	 */
	public void setHigherQualityDelay(final int higherQualityDelay) {
		_higherQualityDelay = higherQualityDelay;
	}

	public void setImageQuality(final boolean isShowHighQuality, final int hqMinSize) {

		_isShowHighQuality = isShowHighQuality;
		_highQualityMinSize = hqMinSize;

		redraw();
	}

	/**
	 * Sets the gallery's interpolation setting to the parameter, which must be one of
	 * <code>SWT.DEFAULT</code>, <code>SWT.NONE</code>, <code>SWT.LOW</code> or
	 * <code>SWT.HIGH</code>.
	 * 
	 * @param interpolation
	 */
	public void setInterpolation(final int interpolation) {
		_interpolation = interpolation;
	}

	public void setItemMinMaxSize(final int minItemSize, final int maxItemSize) {
		_minItemWidth = minItemSize;
		_maxItemWidth = maxItemSize;
	}

	/**
	 * Set item receiver. Usually, this does not trigger gallery update. redraw must be called right
	 * after setGroupRenderer to reflect this change.
	 * 
	 * @param itemRenderer
	 */
	public void setItemRenderer(final AbstractGalleryMT20ItemRenderer itemRenderer) {

		_itemRenderer = itemRenderer;

		redraw();
	}

	public void setItemSize(final int itemWidth, final int itemHeight) {

		_itemWidth = itemWidth;
		_itemHeight = itemHeight;

		updateGallery(true);
	}

	/**
	 * Toggle item selection status
	 * 
	 * @param item
	 *            Item which state is to be changed.
	 * @param selected
	 *            true is the item is now selected, false if it is now unselected.
	 * @param notifyListeners
	 *            If true, a selection event will be sent to all the current selection listeners.
	 */
	private void setSelected(final GalleryMT20Item item, final boolean selected, final boolean notifyListeners) {

//		if (selected) {
//			if (!isSelected(item)) {
//				_addSelection(item);
//
//			}
//
//		} else {
//			if (isSelected(item)) {
//				_removeSelection(item);
//			}
//		}
//
//		// Notify listeners if necessary.
//		if (notifyListeners) {
//
//			GalleryMT20Item notifiedItem = null;
//
//			if (item != null && selected) {
//				notifiedItem = item;
//			} else {
//				if (_selectedItems != null && _selectedItems.length > 0) {
//					notifiedItem = _selectedItems[_selectedItems.length - 1];
//				}
//			}
//
//			int index = -1;
//			if (notifiedItem != null) {
//				index = indexOf(notifiedItem);
//			}
//
//			notifySelectionListeners(notifiedItem, index, false);
//		}

	}

	/**
	 * Set number of items in the gallery, this will also start a gallery update.
	 * 
	 * @param numberOfItems
	 */
	public void setupItems(final int numberOfItems) {

		_galleryItems = new GalleryMT20Item[numberOfItems];

//		// TODO: I'm clearing selection here
//		// but we have to check that Table has the same behavior
//		_deselectAll(false);

		updateGallery(false);
	}

	private void updateGallery(final boolean isKeepLocation) {

		updateStructuralValues(isKeepLocation);
		updateScrollBars();

		redraw();
	}

	/**
	 * Move the scrollbar to reflect the current visible items position. <br/>
	 * The bar which is moved depends of the current gallery scrolling : vertical or horizontal.
	 */
	private void updateScrollBars() {

		if (_isVertical) {
			updateScrollBarsProperties(getVerticalBar(), _clientArea.height, _contentVirtualHeight);
		} else {
			updateScrollBarsProperties(getHorizontalBar(), _clientArea.width, _contentVirtualWidth);
		}
	}

	/**
	 * Move the scrollbar to reflect the current visible items position.
	 * 
	 * @param bar
	 *            - the scroll bar to move
	 * @param clientAreaSize
	 *            - Client (visible) area size
	 * @param contentSize
	 *            - Total Size
	 */
	private void updateScrollBarsProperties(final ScrollBar bar, final int clientAreaSize, final int contentSize) {

		if (bar == null) {
			return;
		}

		bar.setMinimum(0);
		bar.setMaximum(contentSize);
		bar.setPageIncrement(clientAreaSize);
		bar.setThumb(clientAreaSize);

		bar.setIncrement(16);

		if (contentSize > clientAreaSize) {

			bar.setEnabled(true);
			bar.setVisible(true);
			bar.setSelection(_galleryPosition);

			// Ensure that translate has a valid value.
			validateGalleryPosition();

		} else {

			bar.setEnabled(false);
			bar.setVisible(false);
			bar.setSelection(0);
			_galleryPosition = 0;
		}
	}

	/**
	 * Recalculate structural values using the group renderer<br>
	 * Gallery and item size will be updated.
	 * 
	 * @param changedGroup
	 *            the group that was modified since the last layout. If the group renderer or more
	 *            that one group have changed, use null as parameter (full update)
	 * @param isKeepLocation
	 *            if true, the current scrollbars position ratio is saved and restored even if the
	 *            gallery size has changed. (Visible items stay visible)
	 */
	private void updateStructuralValues(final boolean isKeepLocation) {

		computeNumberOfVertHorizItems();

		final int clientAreaWidth = _clientArea.width;
		final int clientAreaHeight = _clientArea.height;

		float oldPosition = 0;

		if (_isVertical) {

			// vertical

			if (isKeepLocation && _contentVirtualHeight > 0) {
				oldPosition = (float) (_galleryPosition + 0.5 * clientAreaHeight) / _contentVirtualHeight;
			}

			_contentVirtualWidth = clientAreaWidth;
			_contentVirtualHeight = _numberOfVertItems * _itemHeight;

			if (isKeepLocation) {
				_galleryPosition = (int) (_contentVirtualHeight * oldPosition - 0.5 * clientAreaHeight);
			}

		} else {

			// horizontal

			if (isKeepLocation && _contentVirtualWidth > 0) {
				oldPosition = (float) (_galleryPosition + 0.5 * clientAreaWidth) / _contentVirtualWidth;
			}

			_contentVirtualWidth = _numberOfHorizItems * _itemWidth;
			_contentVirtualHeight = clientAreaHeight;

			if (isKeepLocation) {
				_galleryPosition = (int) (_contentVirtualWidth * oldPosition - 0.5 * clientAreaWidth);
			}
		}

		validateGalleryPosition();
	}

	/**
	 * Check the current translation value. Must be &gt; 0 and &lt; gallery size.<br/>
	 * Invalid values are fixed.
	 */
	private void validateGalleryPosition() {

		// Ensure that gallery position has a valid value.

		int contentSize = 0;
		int clientSize = 0;

		// Fix negative values
		if (_galleryPosition < 0) {
			_galleryPosition = 0;
		}

		// Get size depending on vertical setting.
		if (_isVertical) {
			contentSize = _contentVirtualHeight;
			clientSize = _clientArea.height;
		} else {
			contentSize = _contentVirtualWidth;
			clientSize = _clientArea.width;
		}

		if (contentSize > clientSize) {
			// Fix translate too big.
			if (_galleryPosition + clientSize > contentSize) {
				_galleryPosition = contentSize - clientSize;
			}
		} else {
			_galleryPosition = 0;
		}
	}

	/**
	 * @param eventTime
	 * @param isZoomIn
	 * @param isShiftKey
	 * @param isCtrlKey
	 */
	private void zoomImage(	final int eventTime,
							final boolean isZoomIn,
							final boolean isShiftKey,
							final boolean isCtrlKey) {

		if (_mouseMovePosition == null) {
			return;
		}

		// check if this the same event which can be send multiple times
		if (_lastZoomEventTime == eventTime) {
			return;
		}
		_lastZoomEventTime = eventTime;

		// get item from mouse position
		final GalleryMT20Item currentItem = getItem(_mouseMovePosition);

//		if (currentItem == null) {
//			return;
//		}

		if (_minItemWidth == -1 || _maxItemWidth == -1) {
			// min or max height is not set
			return;
		}

		int ZOOM_INCREMENT = 5;
		if (isShiftKey && isCtrlKey == false) {
			ZOOM_INCREMENT = 1;
		} else if (isCtrlKey && isShiftKey == false) {

			ZOOM_INCREMENT = 10;

//				ZOOM_INCREMENT = (int) Math.pow(itemHeight / 4, 1.00);

		} else if (isCtrlKey && isShiftKey) {
			ZOOM_INCREMENT = 50;
		}

		int itemWidth = _itemWidth;

		if (isZoomIn) {

			// zoom in

			// check if max zoom is reached
			if (itemWidth >= _maxItemWidth) {
				// max is reached
				return;
			}

			itemWidth += ZOOM_INCREMENT;
			if (itemWidth > _maxItemWidth) {
				itemWidth = _maxItemWidth;
			}

		} else {

			// zoom out

			if (itemWidth <= _minItemWidth) {
				// min is reached
				return;
			}

			itemWidth -= ZOOM_INCREMENT;
			if (itemWidth < _minItemWidth) {
				itemWidth = _minItemWidth;
			}
		}

//		final double itemRatio = groupRenderer.getItemRatio();

		final int itemHeight = (int) (itemWidth / _itemRatio);

		_itemWidth = itemWidth;
		_itemHeight = itemHeight;
		_itemRatio = (double) itemWidth / itemHeight;

		updateGallery(true);

		notifyZoomListener(itemWidth, itemHeight);
	}
}
