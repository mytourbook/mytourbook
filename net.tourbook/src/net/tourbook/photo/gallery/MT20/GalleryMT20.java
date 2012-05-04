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

import java.util.HashMap;

import net.tourbook.util.UI;

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

	private boolean								_isVertical;
	private boolean								_isMultiSelection;

	private boolean								_isGalleryMoved;

	/**
	 * Current gallery top/left position (this is also the scroll bar position). Can be used by
	 * renderer during paint.
	 */
	private int									_galleryPosition		= 0;

	private int									_prevGalleryPosition;
	private Double								_newGalleryPositionRatio;

	/**
	 * When <code>true</code> images are painted in higher quality but must be larger than the high
	 * quality minimum size which is set in {@link #setImageQuality(boolean, int)}.
	 */
	private boolean								_isShowHighQuality;

	/**
	 * When images size (height) is larger than this values, the images are painted with high
	 * quality in a 2nd run.
	 */
	private int									_highQualityMinSize;

	/**
	 * Image quality : interpolation
	 */
	private int									_interpolation			= SWT.HIGH;

	/**
	 * Image quality : antialias
	 */
	private int									_antialias				= SWT.ON;

	/**
	 * Width for the whole gallery
	 */
	private int									_contentVirtualWidth	= 0;

	/**
	 * Height for the whole gallery
	 */
	private int									_contentVirtualHeight	= 0;

	private int									_prevViewportWidth;
	private int									_prevViewportHeight;
	private int									_prevContentHeight;
	private int									_prevContentWidth;

	private AbstractGalleryMT20ItemRenderer		_itemRenderer;

	/**
	 * Cached client area
	 */
	private Rectangle							_clientArea;

	private Composite							_parent;

	private ControlAdapter						_parentControlListener;
	private int									_higherQualityDelay;

	private RedrawTimer							_redrawTimer			= new RedrawTimer();

	/**
	 * Contains items which are displayed in the gallery. Initially the items are <code>null</code>
	 * because they are virtual until they are displayed.
	 */
	private GalleryMT20Item[]					_galleryItems;

	/**
	 * Contains items indices for the current client area. It can also contains indices for gallery
	 * item which are out of scope. Therefore it is necessary to check if the index is within the
	 * arraybounds of {@link #_visibleGalleryItems}.
	 * <p>
	 * This is used to stop loading images which are not displayed.
	 */
	private int[]								_clientAreaItemsIndices;

	/**
	 * Represents items which are filtered. It contains the indices of the gallery items in
	 * {@link #_galleryItems}.
	 */
	private int[]								_filteredItemsIndices;

	/**
	 * Contains gallery items which are currently be selected in the UI.
	 */
	private HashMap<String, GalleryMT20Item>	_selectedItems			= new HashMap<String, GalleryMT20Item>();

	/**
	 * Selection bit flags. Each 'int' contains flags for 32 items.
	 */
	private int[]								filterSelectionFlags	= null;

	/**
	 * Default image ratio between image width/height. It is the average between 4000x3000 (1.3333)
	 * and 5184x3456 (1.5)
	 */
	private double								_itemRatio				= 15.0 / 10;								//((4.0 / 3.0) + (15.0 / 10.0)) / 2;
	private int									_itemWidth				= 80;
	private int									_itemHeight				= (int) (_itemWidth / _itemRatio);

	/**
	 * @return Contains minimum gallery item width or <code>-1</code> when value is not set.
	 */
	private int									_minItemWidth			= -1;

	/**
	 * @return Contains maximum gallery item width or <code>-1</code> when value is not set.
	 */
	private int									_maxItemWidth			= -1;

	private int									_numberOfAllHorizItems;
	private int									_numberOfAllVertItems;

	/**
	 * Is <code>true</code> during zooming. OSX do fire a mouse wheel event always, Win do fire a
	 * mouse wheel event when scrollbars are not visible.
	 * <p>
	 * Terrible behaviour !!!
	 */
	private boolean								_isZoomed;

	/**
	 * Keeps track of the last selected item. This is necessary to support "Shift+Mouse button"
	 * where we have to select all items between the previous and the current item and keyboard
	 * navigation.
	 */
	private GalleryMT20Item						_lastSingleClick		= null;

	private Point								_mouseMovePosition;
	private Point								_mousePanStartPosition;
	private int									_lastZoomEventTime;
	private boolean								_mouseClickHandled;
	private boolean								_isGalleryPanned;

	/**
	 * Last result of _indexOf(GalleryItem). Used for optimisation.
	 */
	private int									_lastIndexOfItemFilter	= -1;

	/**
	 * Vertical/horizontal offset for centered gallery items
	 */
	private int									_itemCenterOffset;

	/**
	 * Gallery item which was selected at last
	 */
	private GalleryMT20Item						_lastSelectedItem;
	private int									_lastSelectedItemFilterIndex;

	private class RedrawTimer implements Runnable {
		public void run() {

			if (isDisposed()) {
				return;
			}

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

//		super(parent, style);
		super(parent, style | SWT.DOUBLE_BUFFERED);
//		super(parent, style | SWT.NO_BACKGROUND);
//		super(parent, style | SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE);
//		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE);
//		super(parent, style | SWT.NO_MERGE_PAINTS);

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

		updateGallery(false, null);
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
					zoomGallery(event.time, event.count > 0, isShift, isCtrl);
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

				updateGallery(true, null);
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

	private void _addSelection(final GalleryMT20Item item) {

		if (item == null) {
			return;
		}

		if (isSelected(item)) {
			return;
		}

		// Deselect all items if multi selection is disabled
		if (!_isMultiSelection) {
			_deselectAll(false);
		}

		final int itemFilterIndex = _indexOfFilter(item);

		// Divide position by 32 to get selection bloc for this item.
		final int n = itemFilterIndex >> 5;
		if (filterSelectionFlags == null) {
			// Create selectionFlag array
			// Add 31 before dividing by 32 to ensure at least one 'int' is
			// created if size < 32.
			filterSelectionFlags = new int[(_galleryItems.length + 31) >> 5];
		} else if (n >= filterSelectionFlags.length) {
			// Expand selectionArray
			final int[] oldFlags = filterSelectionFlags;
			filterSelectionFlags = new int[n + 1];
			System.arraycopy(oldFlags, 0, filterSelectionFlags, 0, oldFlags.length);
		}

		// Get flag position in the 32 bit block and ensure is selected.
		filterSelectionFlags[n] |= 1 << (itemFilterIndex & 0x1f);

		_lastSelectedItem = item;
		_lastSelectedItemFilterIndex = itemFilterIndex;

		_selectedItems.put(item.itemID, item);
	}

	private void _addSelection(final int itemFilterIndex, final GalleryMT20Item item) {

		if (isSelected(itemFilterIndex)) {
			return;
		}

		// Deselect all items if multi selection is disabled
		if (!_isMultiSelection) {
			_deselectAll(false);
		}

		// Divide position by 32 to get selection bloc for this item.
		final int n = itemFilterIndex >> 5;
		if (filterSelectionFlags == null) {
			// Create selectionFlag array
			// Add 31 before dividing by 32 to ensure at least one 'int' is
			// created if size < 32.
			filterSelectionFlags = new int[(_galleryItems.length + 31) >> 5];
		} else if (n >= filterSelectionFlags.length) {
			// Expand selectionArray
			final int[] oldFlags = filterSelectionFlags;
			filterSelectionFlags = new int[n + 1];
			System.arraycopy(oldFlags, 0, filterSelectionFlags, 0, oldFlags.length);
		}

		// Get flag position in the 32 bit block and ensure is selected.
		filterSelectionFlags[n] |= 1 << (itemFilterIndex & 0x1f);

		_lastSelectedItem = item;
		_lastSelectedItemFilterIndex = itemFilterIndex;

		_selectedItems.put(item.itemID, item);
	}

	/**
	 * Deselects all items and send selection event depending on parameter.
	 * 
	 * @param notifyListeners
	 *            If true, a selection event will be sent to all the current selection listeners.
	 */
	private void _deselectAll(final boolean notifyListeners) {

		_lastSelectedItem = null;
		_selectedItems.clear();

		// Deselect groups
		// We could set selectionFlags to null, but we rather set all values to
		// 0 to redure garbage collection. On each iteration, we deselect 32
		// items.
		if (filterSelectionFlags != null) {
			for (int i = 0; i < filterSelectionFlags.length; i++) {
				filterSelectionFlags[i] = 0;
			}
		}

		// Notify listeners if necessary.
		if (notifyListeners) {
			notifySelectionListeners(null, -1, false);
		}
	}

	/**
	 * @param galleryIndex
	 * @return Returns initialized gallery item from the given gallery position.
	 */
	private GalleryMT20Item _getItemFromGallery(final int galleryIndex) {

		GalleryMT20Item galleryItem = _galleryItems[galleryIndex];

		if (galleryItem == null) {

			galleryItem = new GalleryMT20Item(this);

			_galleryItems[galleryIndex] = galleryItem;

			initItem(galleryItem, galleryIndex);
		}

		return galleryItem;
	}

//	/**
//	 * Returns the index of a GalleryItem in all gallery items {@link #_galleryItems}.
//	 *
//	 * @param item
//	 * @return
//	 */
//	private int _indexOfGallery(final GalleryMT20Item item) {
//
//		final int itemCount = _galleryItems.length;
//
//		if (1 <= _lastIndexOf && _lastIndexOf < itemCount - 1) {
//			if (_galleryItems[_lastIndexOf] == item) {
//				return _lastIndexOf;
//			}
//			if (_galleryItems[_lastIndexOf + 1] == item) {
//				return ++_lastIndexOf;
//			}
//			if (_galleryItems[_lastIndexOf - 1] == item) {
//				return --_lastIndexOf;
//			}
//		}
//		if (_lastIndexOf < itemCount / 2) {
//
//			for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
//				if (_galleryItems[itemIndex] == item) {
//					return _lastIndexOf = itemIndex;
//				}
//			}
//		} else {
//
//			for (int itemIndex = itemCount - 1; itemIndex >= 0; --itemIndex) {
//				if (_galleryItems[itemIndex] == item) {
//					return _lastIndexOf = itemIndex;
//				}
//			}
//		}
//
//		return -1;
//	}

	/**
	 * Returns the index of a GalleryItem in the filtered gallery items
	 * {@link #_filteredItemsIndices}.
	 * 
	 * @param item
	 * @return
	 */
	private int _indexOfFilter(final GalleryMT20Item item) {

		final int filterItemCount = _filteredItemsIndices.length;

		if (1 <= _lastIndexOfItemFilter && _lastIndexOfItemFilter < filterItemCount - 1) {

			if (_galleryItems[_filteredItemsIndices[_lastIndexOfItemFilter]] == item) {
				return _lastIndexOfItemFilter;
			}

			if (_galleryItems[_filteredItemsIndices[_lastIndexOfItemFilter + 1]] == item) {
				return ++_lastIndexOfItemFilter;
			}

			if (_galleryItems[_filteredItemsIndices[_lastIndexOfItemFilter - 1]] == item) {
				return --_lastIndexOfItemFilter;
			}
		}

		if (_lastIndexOfItemFilter < filterItemCount / 2) {

			for (int itemIndex = 0; itemIndex < filterItemCount; itemIndex++) {
				if (_galleryItems[_filteredItemsIndices[itemIndex]] == item) {
					return _lastIndexOfItemFilter = itemIndex;
				}
			}
		} else {

			for (int itemFilterIndex = filterItemCount - 1; itemFilterIndex >= 0; --itemFilterIndex) {
				if (_galleryItems[_filteredItemsIndices[itemFilterIndex]] == item) {
					return _lastIndexOfItemFilter = itemFilterIndex;
				}
			}
		}

		return -1;
	}

	private void _removeSelection(final GalleryMT20Item item) {

		final int itemFilterIndex = _indexOfFilter(item);
		filterSelectionFlags[itemFilterIndex >> 5] &= ~(1 << (itemFilterIndex & 0x1f));

		_lastSelectedItem = null;
		_selectedItems.remove(item.itemID);
	}

	/**
	 * center selected item
	 */
	private Double centerSelectedItem() {

		if (_lastSelectedItem == null) {
			return null;
		}

		Double galleryPositionRatio = null;

		// get position in filter
//		double filterRatio = ((double) _lastSelectedItemFilterIndex) / (double) _filteredItemsIndices.length;

		final int row = _lastSelectedItemFilterIndex / _numberOfAllHorizItems;
		final int numberOfRows = _numberOfAllVertItems;//_filteredItemsIndices.length / _numberOfHorizItems;

		final double rowRatio = numberOfRows == 0 ? 0 : (double) row / numberOfRows;

		if (_isVertical) {

			final double verticalTopLeftPosition = _contentVirtualHeight * rowRatio;

			// center vertically
			final double verticalCenterPosition = verticalTopLeftPosition
					+ (0.5 * _clientArea.height)
					- (0.5 * _itemHeight)
			//
			;

			galleryPositionRatio = verticalCenterPosition / _contentVirtualHeight;

			System.out.println("\tcenterSelectedItem: " //
//					+ (" top-left pos:" + verticalTopLeftPosition)
					+ (" H:" + _numberOfAllHorizItems)
					+ (" V:" + _numberOfAllVertItems)
//					+ (" row:" + row)
//					+ (" nofRow:" + numberOfRows)
//					+ (" ratio:" + rowRatio)
//					+ (" centerHPos:" + verticalCenterPosition)
					//
					);
			// TODO remove SYSTEM.OUT.PRINTLN

		} else {

			// not yet implemented
		}

		return galleryPositionRatio;
	}

	/**
	 * Sets number of vertical and horizontal items for the whole gallery in
	 * {@link #_numberOfAllHorizItems} and {@link #_numberOfAllVertItems}
	 */
	private void computeNumberOfAllGalleryItems() {

		if (_isVertical) {

			final Point vhNumbers = computeNumberOfAllGalleryItems_10(_clientArea.width, _itemWidth);

			_numberOfAllHorizItems = vhNumbers.x;
			_numberOfAllVertItems = vhNumbers.y;

		} else {

			final Point vhNumbers = computeNumberOfAllGalleryItems_10(_clientArea.height, _itemHeight);

			_numberOfAllHorizItems = vhNumbers.y;
			_numberOfAllVertItems = vhNumbers.x;
		}
	}

	/**
	 * Calculate how many items are displayed horizontally or vertically.
	 * 
	 * @param visibleSize
	 * @param itemSize
	 * @return
	 */
	private Point computeNumberOfAllGalleryItems_10(final int visibleSize, final int itemSize) {

		if (_filteredItemsIndices == null || _filteredItemsIndices.length == 0) {
			return new Point(0, 0);
		}

		final int numberOfFilteredItems = _filteredItemsIndices.length;

		int x = visibleSize / itemSize;
		int y = 0;

		if (x > 0) {
			y = (int) Math.ceil((double) numberOfFilteredItems / (double) x);
		} else {
			// Show at least one item;
			y = numberOfFilteredItems;
			x = 1;
		}

//		System.out.println("\th:" + x + " v:" + y + " size:" + itemSize);
		// TODO remove SYSTEM.OUT.PRINTLN

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
	 * Original method: AbstractGridGroupRenderer.getVisibleItems()
	 * 
	 * @param clippingArea
	 * @return Returns indices for all gallery items contained in the clipping area. This can also
	 *         contain indices for which items are not available.
	 */
	private int[] getAreaItemsIndices(final Rectangle clippingArea) {

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

				firstItem = firstLine * _numberOfAllHorizItems;
				firstItem += horizontalItem;

				lastItem = firstItem + 1;

			} else {

				firstItem = firstLine * _numberOfAllHorizItems;
				lastItem = (lastLine + 1) * _numberOfAllHorizItems;
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

		} else {

			int firstLine = (clipX + _galleryPosition) / _itemWidth;
			if (firstLine < 0) {
				firstLine = 0;
			}

			final int firstItem = firstLine * _numberOfAllVertItems;

			int lastLine = (clipX + _galleryPosition + clipWidth) / _itemWidth;

			if (lastLine < firstLine) {
				lastLine = firstLine;
			}

			final int lastItem = (lastLine + 1) * _numberOfAllVertItems;

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
	 * @return Returns all gallery items or <code>null</code> when items are not set with
	 *         {@link #setupItems(int)}
	 */
	public GalleryMT20Item[] getGalleryItems() {
		return _galleryItems;
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

	/**
	 * Get item at pixel position
	 * 
	 * @param coords
	 * @return GalleryItem or null
	 */
	public GalleryMT20Item getItemFromPosition(final int viewPortX, final int viewPortY) {

		if (_isVertical) {

			final int contentPosX = viewPortX - _itemCenterOffset;
			final int contentPosY = _galleryPosition + viewPortY;

			final int indexX = contentPosX / _itemWidth;
			final int indexY = contentPosY / _itemHeight;

			// ckeck if mouse click is outside of the gallery horizontal items
			if (indexX >= _numberOfAllHorizItems) {
				return null;
			}

			// ckeck if mouse click is outside of the gallery vertical items
			if (indexY >= _numberOfAllVertItems) {
				return null;
			}

			final int itemIndex = indexY * _numberOfAllHorizItems + indexX;

			// ensure array bounds
			final int maxItems = _filteredItemsIndices.length;
			if (itemIndex >= maxItems) {
				return null;
			}

			return _galleryItems[_filteredItemsIndices[itemIndex]];

		} else {

			// not yet implemented
		}

		return null;
	}

	public int getItemWidth() {
		return _itemWidth;
	}

	public int getNumberOfHorizontalImages() {
		return _numberOfAllHorizItems;
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

	/**
	 * Initializes a gallery item which can be used to set data into the item. This method is called
	 * before a gallery item is painted.
	 * 
	 * @param galleryItem
	 * @param galleryIndex
	 *            Index within all gallery items
	 */
	public abstract void initItem(final GalleryMT20Item galleryItem, final int galleryIndex);

	/**
	 * @param checkedGalleryItem
	 * @return Returns <code>true</code> when the requested gallery item is currently visible in the
	 *         client area.
	 *         <p>
	 *         The gallery item location is updated.
	 */
	public boolean isItemVisible(final GalleryMT20Item checkedGalleryItem) {

		final GalleryMT20Item[] galleryItems = _galleryItems;

		if (_filteredItemsIndices == null
				|| _filteredItemsIndices.length == 0
				|| _clientAreaItemsIndices == null
				|| _clientAreaItemsIndices.length == 0) {
			return false;
		}

		final int numberOfAreaItems = _clientAreaItemsIndices.length;
		final int numberOfFilterItems = _filteredItemsIndices.length;

		for (int areaIndex = 0; areaIndex < numberOfAreaItems; areaIndex++) {

			final int filterIndex = _clientAreaItemsIndices[areaIndex];

			// ensure number of available items
			if (filterIndex >= numberOfFilterItems) {
				return false;
			}

			final int galleryIndex = _filteredItemsIndices[filterIndex];

			final GalleryMT20Item galleryItem = galleryItems[galleryIndex];

			if (galleryItem == checkedGalleryItem) {

				setItemPosition(galleryItem, filterIndex);

				return true;
			}
		}

		return false;
	}

	private boolean isSelected(final GalleryMT20Item item) {

		if (item == null) {
			return false;
		}

		if (filterSelectionFlags == null) {
			return false;
		}

		final int itemFilterIndex = _indexOfFilter(item);

		final int itemFlagIndex = itemFilterIndex >> 5;
		if (itemFlagIndex >= filterSelectionFlags.length) {
			return false;
		}

		final int flags = filterSelectionFlags[itemFlagIndex];

		return flags != 0 && (flags & 1 << (itemFilterIndex & 0x1f)) != 0;
	}

	private boolean isSelected(final int itemFilterIndex) {

		if (filterSelectionFlags == null) {
			return false;
		}

		final int itemFlagIndex = itemFilterIndex >> 5;
		if (itemFlagIndex >= filterSelectionFlags.length) {
			return false;
		}

		final int flags = filterSelectionFlags[itemFlagIndex];

		return flags != 0 && (flags & 1 << (itemFilterIndex & 0x1f)) != 0;
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

		final GalleryMT20Item item = getItemFromPosition(e.x, e.y);
		if (item != null) {
			notifySelectionListeners(item, 0, true);
		}
		_mouseClickHandled = true;
	}

	private void onMouseDown(final MouseEvent e) {

		_mouseClickHandled = false;

		final GalleryMT20Item item = getItemFromPosition(e.x, e.y);

		if (e.button == 1) {

			// left mouse button is pressed

			if (item == null) {
				_deselectAll(true);
				redraw();
				_mouseClickHandled = true;
				_lastSingleClick = null;
			} else {
				if ((e.stateMask & SWT.MOD1) > 0) {
					onMouseHandleLeftMod1(e, item, true, false);
				} else if ((e.stateMask & SWT.SHIFT) > 0) {
					onMouseHandleLeftShift(e, item);
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

				_lastSingleClick = item;
				redraw();
				_mouseClickHandled = true;
			}
		} else if (up) {
			if (item == null) {
				_deselectAll(true);
			} else {

				_deselectAll(false);
				setSelected(item, true, _lastSingleClick != item);
				_lastSingleClick = item;
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
				_lastSingleClick = item;
				redraw();
			}
			// }
		}
	}

	private void onMouseHandleLeftShift(final MouseEvent e, final GalleryMT20Item currentItem) {

		if (_lastSingleClick == null) {
			return;
		}

		_deselectAll(false);

		/*
		 * select items between current item and last click item
		 */

		final int filterIndexLast = _indexOfFilter(_lastSingleClick);
		final int filterIndexCurrent = _indexOfFilter(currentItem);

		int filterIndexFrom;
		int filterIndexTo;

		if (filterIndexLast < filterIndexCurrent) {
			filterIndexFrom = filterIndexLast;
			filterIndexTo = filterIndexCurrent;
		} else {
			filterIndexFrom = filterIndexCurrent;
			filterIndexTo = filterIndexLast;
		}

		for (int filterIndex = filterIndexFrom; filterIndex <= filterIndexTo; filterIndex++) {

			final GalleryMT20Item item = _getItemFromGallery(_filteredItemsIndices[filterIndex]);

			_addSelection(filterIndex, item);
		}

//	???	notifySelectionListeners(to, indexOf(to), false);

		redraw();
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

			final GalleryMT20Item item = getItemFromPosition(e.x, e.y);

			if (item == null) {
				return;
			}

			if ((e.stateMask & SWT.MOD1) > 0) {

				onMouseHandleLeftMod1(e, item, false, true);

			} else if ((e.stateMask & SWT.SHIFT) > 0) {

				onMouseHandleLeftShift(e, item);

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

			if (isLowQualityPainting) {
				gc.setAntialias(SWT.OFF);
				gc.setInterpolation(SWT.OFF);
			} else {
				gc.setAntialias(_antialias);
				gc.setInterpolation(_interpolation);
			}

			// get visible items in the clipping area
			final int[] areaItemsIndices = getAreaItemsIndices(clippingArea);
			if (areaItemsIndices != null) {

				final int numberOfAreaItems = areaItemsIndices.length;
				if (numberOfAreaItems > 0) {

					final int filterLength = _filteredItemsIndices.length;

					// loop: all gallery items in the clipping area
					for (int areaIndex = numberOfAreaItems - 1; areaIndex >= 0; areaIndex--) {

						final int filterIndex = areaItemsIndices[areaIndex];

						// ensure number of available items
						if (filterIndex >= filterLength) {
							continue;
						}

						final GalleryMT20Item galleryItem = _getItemFromGallery(_filteredItemsIndices[filterIndex]);
						final boolean isSelected = isSelected(galleryItem);

						setItemPosition(galleryItem, filterIndex);

						final int viewPortX = galleryItem.viewPortX;
						final int viewPortY = galleryItem.viewPortY;

						gc.setClipping(viewPortX, viewPortY, _itemWidth, _itemHeight);

						_itemRenderer.draw(gc, galleryItem, viewPortX, viewPortY, _itemWidth, _itemHeight, isSelected);
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

		_clientAreaItemsIndices = getAreaItemsIndices(_clientArea);
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

				zoomGallery(event.time, isZoomIn, isShift, isCtrl);

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

		_clientAreaItemsIndices = getAreaItemsIndices(_clientArea);
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
	 * Set items gallery position.
	 * 
	 * @param galleryItem
	 * @param filterIndex
	 */
	private void setItemPosition(final GalleryMT20Item galleryItem, final int filterIndex) {

		int viewPortX;
		int viewPortY;

		int numberOfItemsX;
		int numberOfItemsY;

		if (_isVertical) {
			numberOfItemsX = filterIndex % _numberOfAllHorizItems;
			numberOfItemsY = (filterIndex - numberOfItemsX) / _numberOfAllHorizItems;
		} else {
			numberOfItemsY = filterIndex % _numberOfAllVertItems;
			numberOfItemsX = (filterIndex - numberOfItemsY) / _numberOfAllVertItems;
		}

		final int galleryVirtualPosX = numberOfItemsX * _itemWidth;
		final int galleryVirtualPosY = numberOfItemsY * _itemHeight;

		if (_isVertical) {

			final int allItemsWidth = _numberOfAllHorizItems * _itemWidth;

			if (_contentVirtualWidth > allItemsWidth) {
				_itemCenterOffset = (_contentVirtualWidth - allItemsWidth) / 2;
			} else if (allItemsWidth > _contentVirtualWidth) {
				_itemCenterOffset = -(allItemsWidth - _contentVirtualWidth) / 2;
			} else {
				_itemCenterOffset = 0;
			}

			viewPortX = galleryVirtualPosX + _itemCenterOffset;
			viewPortY = galleryVirtualPosY - _galleryPosition;

		} else {

			viewPortX = galleryVirtualPosX - _galleryPosition;
			viewPortY = galleryVirtualPosY;
		}

		galleryItem.viewPortX = viewPortX;
		galleryItem.viewPortY = viewPortY;
		galleryItem.height = _itemHeight;
		galleryItem.width = _itemWidth;
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

	/**
	 * Sets the size (width) of the gallery item.
	 * 
	 * @param itemSize
	 * @return Returns the size which has been set. This value can differ from the requested item
	 *         size when a scrollbar needs to be displayed.
	 */
	public int setItemSize(final int itemSize) {

		_itemWidth = itemSize;
		_itemHeight = (int) (itemSize / _itemRatio);

		if (_isVertical) {

			// get number of items which SHOULD be displayed
			int requestedNumberOfHorizontalItems = _clientArea.width / itemSize;
			if (requestedNumberOfHorizontalItems < 1) {
				requestedNumberOfHorizontalItems = 1;
			}

			computeNumberOfAllGalleryItems();

			final int contentVirtualHeight = _numberOfAllVertItems * _itemHeight;

			if (contentVirtualHeight > _clientArea.height) {

				// vertical scrollbar must be displayed

				final ScrollBar bar = getVerticalBar();

				if (bar != null) {

					if (bar.isVisible()) {
						// virtual height is already correctly computed -> nothing more to do
					} else {
						/*
						 * vertical bar is not yet displayed but the content is larger than the
						 * visible area, vertical bar needs to be displayed and the content width
						 * and hight must be adjusted
						 */

						final int barWidth = bar.getSize().x;
						final int adjustedAreaWidth = _clientArea.width - barWidth;

						final int newItemWidth = adjustedAreaWidth / requestedNumberOfHorizontalItems;

						_itemWidth = newItemWidth;
						_itemHeight = (int) (newItemWidth / _itemRatio);
					}
				}
			}

		} else {

			// not yet implemented
		}

		// compute new content width/height
		updateStructuralValues(false);
		// ensure scrollbars are correctly displayed/hidden
		updateScrollBars();

		final Double galleryPositionRatio = centerSelectedItem();

		updateGallery(true, galleryPositionRatio);

		return _itemWidth;
	}

	/**
	 * Toggle item selection status
	 * 
	 * @param item
	 *            Item which state is to be changed.
	 * @param isSelected
	 *            true is the item is now selected, false if it is now unselected.
	 * @param notifyListeners
	 *            If true, a selection event will be sent to all the current selection listeners.
	 */
	private void setSelected(final GalleryMT20Item item, final boolean isSelected, final boolean notifyListeners) {

		if (isSelected) {
			if (!isSelected(item)) {
				_addSelection(item);
			}

		} else {
			if (isSelected(item)) {
				_removeSelection(item);
			}
		}

		// Notify listeners if necessary.
		if (notifyListeners) {

			GalleryMT20Item notifiedItem = null;

			if (item != null && isSelected) {
				notifiedItem = item;
			} else {

// this code is using the last selected item, have not yet discovery why this item is used ????
//				if (_selectedItems != null && _selectedItems.length > 0) {
//					notifiedItem = _selectedItems[_selectedItems.length - 1];
//				}
			}

			int index = -1;
			if (notifiedItem != null) {
				index = _indexOfFilter(notifiedItem);
			}

			notifySelectionListeners(notifiedItem, index, false);
		}
	}

	/**
	 * Set number of items in the gallery, this will also start a gallery update.
	 * 
	 * @param numberOfItems
	 * @param galleryPosition
	 *            Gallery position where the gallery should be used to display gallery items, or
	 *            <code>null</code> when position is not set.
	 */
	public void setupItems(final int numberOfItems, final Double galleryPosition) {

		// create empty (null) gallery items
		_galleryItems = new GalleryMT20Item[numberOfItems];

		_filteredItemsIndices = new int[numberOfItems];

		for (int index = 0; index < numberOfItems; index++) {
			_filteredItemsIndices[index] = index;
		}

		_deselectAll(false);

		updateGallery(false, galleryPosition);
	}

	/**
	 * @param isKeepLocation
	 *            Keeps gallery position when <code>true</code>, otherwise position will be reset.
	 * @param newPositionRatio
	 */
	public void updateGallery(final boolean isKeepLocation, final Double newPositionRatio) {

		// prevent setting it to NULL, especially when the UI is not initialized
		if (newPositionRatio != null) {
			_newGalleryPositionRatio = newPositionRatio;
		}

		if (_clientArea.width == 0 || _clientArea.height == 0) {

			// UI is not yet initialized

		} else {

			updateStructuralValues(isKeepLocation);
			updateScrollBars();

			_clientAreaItemsIndices = getAreaItemsIndices(_clientArea);
		}

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

			// show scrollbar

			bar.setEnabled(true);
			bar.setVisible(true);
			bar.setSelection(_galleryPosition);

			// Ensure that translate has a valid value.
			validateGalleryPosition();

		} else {

			// hide scrollbar

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
	 * @param newPosition
	 */
	private void updateStructuralValues(final boolean isKeepLocation) {

		computeNumberOfAllGalleryItems();

		final int clientAreaWidth = _clientArea.width;
		final int clientAreaHeight = _clientArea.height;

		double oldPosition = -1;

		if (_isVertical) {

			// vertical

			if (isKeepLocation && _contentVirtualHeight > 0) {
				oldPosition = (_galleryPosition + 0.5 * clientAreaHeight) / _contentVirtualHeight;
			}

			_contentVirtualWidth = clientAreaWidth;
			_contentVirtualHeight = _numberOfAllVertItems * _itemHeight;

			if (_newGalleryPositionRatio != null) {

				_galleryPosition = (int) (_contentVirtualHeight * _newGalleryPositionRatio);

				// set position only ONCE
				_newGalleryPositionRatio = null;

			} else {

				if (oldPosition != -1) {
					_galleryPosition = (int) (_contentVirtualHeight * oldPosition - 0.5 * clientAreaHeight);
				}
			}

		} else {

			// horizontal

			if (isKeepLocation && _contentVirtualWidth > 0) {
				oldPosition = (float) (_galleryPosition + 0.5 * clientAreaWidth) / _contentVirtualWidth;
			}

			_contentVirtualWidth = _numberOfAllHorizItems * _itemWidth;
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
	private void zoomGallery(	final int eventTime,
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

//		// get item from mouse position
//		final GalleryMT20Item currentItem = getItemFromPosition(_mouseMovePosition.x,_mouseMovePosition.y);
//
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

//			ZOOM_INCREMENT = (int) Math.pow(itemHeight / 4, 1.00);

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

		final int itemHeight = (int) (itemWidth / _itemRatio);

		_itemWidth = itemWidth;
		_itemHeight = itemHeight;

		// compute new content width/height
		updateStructuralValues(false);
		// ensure scrollbars are correctly displayed/hidden
		updateScrollBars();

		final Double galleryPositionRatio = centerSelectedItem();

		updateGallery(true, galleryPositionRatio);

		notifyZoomListener(itemWidth, itemHeight);
	}
}
