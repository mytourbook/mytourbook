/*******************************************************************************
 * Copyright (c) 2006-2009 Nicolas Richeton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors :
 *    Nicolas Richeton (nicolas.richeton@gmail.com) - initial API and implementation
 *    Tom Schindl      (tom.schindl@bestsolution.at) - fix for bug 174933
 *******************************************************************************/

package net.tourbook.photo.gallery;

import java.lang.reflect.Array;
import java.util.ArrayList;

import net.tourbook.ui.UI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.TypedListener;

/*
 * this is an adjusted version of the gallery widget from Feb. 2012
 */

/**
 * <p>
 * SWT Widget that displays an image gallery<br/>
 * see http://www.eclipse.org/nebula/widgets/gallery/gallery.php<br/>
 * This widget requires jdk-1.4+
 * </p>
 * <p>
 * Style <code>VIRTUAL</code> is used to create a <code>Gallery</code> whose
 * <code>GalleryItem</code>s are to be populated by the client on an on-demand basis instead of
 * up-front. This can provide significant performance improvements for galleries that are very large
 * or for which <code>GalleryItem</code> population is expensive (for example, retrieving values
 * from an external source).
 * </p>
 * <p>
 * Here is an example of using a <code>Gallery</code> with style <code>VIRTUAL</code>: <code><pre>
 * final Gallery gallery = new Gallery(parent, SWT.VIRTUAL | V_SCROLL | SWT.BORDER);
 * gallery.setGroupRenderer(new DefaultGalleryGroupRenderer());
 * gallery.setItemRenderer(new DefaultGalleryItemRenderer());
 * gallery.setItemCount(1000000);
 * gallery.addListener(SWT.SetData, new Listener() {
 * 	public void handleEvent(Event event) {
 * 		GalleryItem item = (GalleryItem) event.item;
 * 		int index = gallery.indexOf(item);
 * 		item.setText(&quot;Item &quot; + index);
 * 		System.out.println(item.getText());
 * 	}
 * });
 * </pre></code>
 * </p>
 * <p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>SINGLE, MULTI, VIRTUAL, V_SCROLL, H_SCROLL</dd>
 * </dl>
 * </p>
 * <p>
 * Note: Only one of the styles SINGLE and MULTI may be specified.
 * </p>
 * <p>
 * Note: Only one of the styles V_SCROLL and H_SCROLL may be specified.
 * </p>
 * <p>
 * <dl>
 * <dt><b>Events:</b></dt>
 * <dd>Selection, DefaultSelection, SetData, PaintItem</dd>
 * </dl>
 * </p>
 * <p>
 * NOTE: THIS WIDGET AND ITS API ARE STILL UNDER DEVELOPMENT.
 * </p>
 * 
 * @author Nicolas Richeton (nicolas.richeton@gmail.com)
 * @contributor Peter Centgraf (bugs 212071, 212073)
 * @contributor Robert Handschmann (bug 215817)
 * @contributor Berthold Daum (bug 306144 - selection tuning)
 */

public class GalleryMT extends Canvas {

//	private static final int		ZOOM_INCREMENT						= 10;

	/**
	 * Gallery root items
	 */
	GalleryMTItem[]					_galleryRootItems					= null;

	private GalleryMTItem[]			_selectedItems						= null;

	/**
	 * Selection bit flags. Each 'int' contains flags for 32 items.
	 */
	protected int[]					selectionFlags						= null;

	/**
	 * Virtual mode flag.
	 */
	boolean							_isVirtual							= false;

	/**
	 * Ultra virtual : non visible groups are not initialized.
	 */
	boolean							_isVirtualGroups					= false;
	boolean							_isVirtualGroupsCompatibilityMode	= false;
	int								_virtualGroupDefaultItemCount		= 10;

	/**
	 * Scrolling direction flag. True : V_SCROLL, false : H_SCROLL.
	 */
	boolean							_isVertical							= true;

	/**
	 * Multi-selection flag
	 */
	boolean							_isMultiSelection					= false;

	/**
	 * Image quality : interpolation
	 */
	int								interpolation						= SWT.HIGH;

	/**
	 * Image quality : antialias
	 */
	int								antialias							= SWT.ON;

	/*
	 * width and height for the whole gallery
	 */
	private int						_contentVirtualHeight				= 0;
	private int						_contentVirtualWidth				= 0;

	int								lastIndexOf							= 0;

	/**
	 * Keeps track of the last selected item. This is necessary to support "Shift+Mouse button"
	 * where we have to select all items between the previous and the current item and keyboard
	 * navigation.
	 */
	protected GalleryMTItem			lastSingleClick						= null;

	/**
	 * Current gallery top/left position (this is also the scroll bar position). Can be used by
	 * renderer during paint.
	 */
	protected int					_galleryPosition					= 0;

	private Double					_galleryPositionWhenUpdated;

	protected int					higherQualityDelay					= 500;

	protected int					_prevGalleryPosition				= 0;
	protected int					_prevViewportWidth					= 0;
	protected int					_prevViewportHeight					= 0;
	protected int					_prevContentHeight					= 0;
	protected int					_prevContentWidth					= 0;

	/**
	 * Keep track of processing the current mouse event.
	 */
	private boolean					mouseClickHandled					= false;

	/**
	 * Background color, if Control#getBackground() is not used.
	 * 
	 * @see GalleryMT#useControlColors
	 */
	private Color					backgroundColor;

	/**
	 * Foreground color, if Control#getForeground() is not used.
	 * 
	 * @see GalleryMT#useControlColors
	 */
	private Color					foregroundColor;

	/**
	 * If set to true, the gallery will get colors from parent Control. This may generate more
	 * objects and slightly slow down the application. See Bug 279822 :
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=279822
	 */
	private boolean					useControlColors					= false;

	AbstractGalleryGroupRenderer	groupRenderer;
	AbstractGalleryItemRenderer		itemRenderer;

	RedrawTimer						redrawTimer							= new RedrawTimer();

	/**
	 * Cached client area
	 */
	private Rectangle				_clientArea;

	private Point					_mouseMovePosition;
	private Point					_mousePanStartPosition;
	private int						_lastZoomEventTime;

	private boolean					_isGalleryPanned;

//	/**
//	 * Is <code>true</code> when the paint event should be interrupted. This will <b>only</b> be set
//	 * when in the paint event the eventloop {@link Display#readAndDispatch()} is run.
//	 */
//	private boolean					_isPaintingInterrupted;
//
//	/**
//	 * Is <code>true</code> when painting is currently done with low image quality
//	 */
//	private boolean					_isLowQualityPainting;

	private boolean					_isGalleryMoved;

	private Composite				_shell;
	private ControlAdapter			_shellControlListener;

	/**
	 * When <code>true</code> images are painted in higher quality but must be larger than the high
	 * quality minimum size which is set in {@link #setImageQuality(boolean, int)}.
	 */
	private boolean					_isShowHighQuality;

	/**
	 * When images size (height) is larger than this values, the images are painted with high
	 * quality in a 2nd run.
	 */
	private int						_highQualityMinSize;

	/**
	 * Is <code>true</code> during zooming. OSX do fire a mouse wheel event always, Win do fire a
	 * mouse wheel event when scrollbars are not visible.
	 * <p>
	 * Terrible behaviour !!!
	 */
	private boolean					_isZoomed;

	protected class RedrawTimer implements Runnable {
		public void run() {
			redraw();
		}
	}

	/**
	 * Create a Gallery
	 * 
	 * @param parent
	 * @param style
	 *            - SWT.VIRTUAL switches in virtual mode. <br/>
	 *            SWT.V_SCROLL add vertical slider and switches to vertical mode. <br/>
	 *            SWT.H_SCROLL add horizontal slider and switches to horizontal mode. <br/>
	 *            if both V_SCROLL and H_SCROLL are specified, the gallery is in vertical mode by
	 *            default. Mode can be changed afterward using setVertical<br/>
	 *            SWT.MULTI allows only several items to be selected at the same time.
	 */
	public GalleryMT(final Composite parent, final int style) {

		super(parent, style | SWT.DOUBLE_BUFFERED);
//		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
//		super(parent, style | SWT.NO_BACKGROUND);
//		super(parent, style);

		_isVirtual = (style & SWT.VIRTUAL) > 0;
		_isVertical = (style & SWT.V_SCROLL) > 0;
		_isMultiSelection = (style & SWT.MULTI) > 0;
		setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));

		_clientArea = getClientArea();

		// Add listeners : redraws, mouse and keyboard
		_addDisposeListeners();
		_addResizeListeners();
		_addPaintListeners();
		_addScrollBarsListeners();
		_addMouseListeners();
		_addKeyListeners();

		// Set defaults
		_setDefaultRenderers();

		// Layout
		updateStructuralValues(null, false);
		updateScrollBarsProperties();
		redraw();
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

	private void _addItem(final GalleryMTItem item, final int position) {
		// Insert item
		_galleryRootItems = (GalleryMTItem[]) _arrayAddItem(_galleryRootItems, item, position);

		// Update Gallery
		updateStructuralValues(null, false);
		updateScrollBarsProperties();
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
					final GalleryMTItem newItem = groupRenderer.getNextItem(lastSingleClick, e.keyCode);

					if (newItem != null) {
						_deselectAll(false);
						setSelected(newItem, true, true);
						lastSingleClick = newItem;
						_showItem(newItem);
						redraw();
					}

					break;

				case SWT.CR:

					final GalleryMTItem[] selection = getSelection();
					GalleryMTItem item = null;

					if (selection != null && selection.length > 0) {
						item = selection[0];
					}

					notifySelectionListeners(item, 0, true);
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

				updateStructuralValues(null, true);
				updateScrollBarsProperties();

				redraw();
			}
		});

		Composite parent = getParent();
		while (parent != null) {

			_shell = parent;
			parent = parent.getParent();
		}

		_shellControlListener = new ControlAdapter() {

			@Override
			public void controlMoved(final ControlEvent e) {
				// makes moving the shell not perfect but better
				_isGalleryMoved = true;
			}
		};
		_shell.addControlListener(_shellControlListener);

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

					if (_isZoomed == false) {

						/*
						 * Zooming can happen in the mouse wheel event before the selection event is
						 * fired. When it is already zoomed, no other action should be done.
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
						 * ensure <ctrl> or <shift> is pressed, otherwise it is zoomed when the
						 * scrollbar is hidden
						 */
						if (isCtrl || isShift) {

							/*
							 * the scrollbar has already been move to a new position with the mouse
							 * wheel, first the scollbar position must be reverted to the current
							 * gallery position
							 */
							updateScrollBarsProperties();

							final boolean isZoomIn = event.detail == SWT.ARROW_UP;

							zoomImage(event.time, isZoomIn, isShift, isCtrl);

						} else {

							if (_isVertical) {
								scrollVertical();
							}
						}
					}

					_isZoomed = false;
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
						scrollHorizontal();
					}
				}
			});
		}

	}

	protected void _addSelection(final GalleryMTItem item) {

		if (item == null) {
			return;
		}

		if (isSelected(item)) {
			return;
		}

		// Deselect all items is multi selection is disabled
		if (!_isMultiSelection) {
			_deselectAll(false);
		}

		if (item.getParentItem() != null) {
			item.getParentItem()._addSelection(item);
		} else {
			final int index = indexOf(item);

			// Divide position by 32 to get selection bloc for this item.
			final int n = index >> 5;
			if (selectionFlags == null) {
				// Create selectionFlag array
				// Add 31 before dividing by 32 to ensure at least one 'int' is
				// created if size < 32.
				selectionFlags = new int[(_galleryRootItems.length + 31) >> 5];
			} else if (n >= selectionFlags.length) {
				// Expand selectionArray
				final int[] oldFlags = selectionFlags;
				selectionFlags = new int[n + 1];
				System.arraycopy(oldFlags, 0, selectionFlags, 0, oldFlags.length);
			}

			// Get flag position in the 32 bit block and ensure is selected.
			selectionFlags[n] |= 1 << (index & 0x1f);

		}

		if (_selectedItems == null) {
			_selectedItems = new GalleryMTItem[1];
		} else {
			final GalleryMTItem[] oldSelection = _selectedItems;
			_selectedItems = new GalleryMTItem[oldSelection.length + 1];
			System.arraycopy(oldSelection, 0, _selectedItems, 0, oldSelection.length);
		}
		_selectedItems[_selectedItems.length - 1] = item;

	}

	/**
	 * Adds an item to an array.
	 * 
	 * @param array
	 * @param object
	 * @param index
	 *            : if index == -1, item is added at the end of the array.
	 * @return
	 */
	protected Object[] _arrayAddItem(final Object[] array, final Object object, final int index) {

		// Get current array length
		int length = 0;
		if (array != null) {
			length = array.length;
		}

		// Create new array
		final Object[] newArray = (Object[]) Array.newInstance(object.getClass(), length + 1);

		if (array != null) {
			System.arraycopy(array, 0, newArray, 0, length);
		}

		if (index != -1) {
			// Move all items
			for (int i = newArray.length - 2; i >= index; i--) {
				if (i >= 0) {
					newArray[i + 1] = newArray[i];
				}
			}

			// Insert item at index
			newArray[index] = object;

		} else {
			// Insert item at the end
			newArray[newArray.length - 1] = object;
		}

		return newArray;
	}

	protected int _arrayIndexOf(final int[] array, final int value) {
		if (array == null) {
			return -1;
		}

		for (int i = array.length - 1; i >= 0; --i) {
			if (array[i] == value) {
				return i;

			}
		}
		return -1;
	}

	protected int _arrayIndexOf(final Object[] array, final Object value) {
		if (array == null) {
			return -1;
		}

		for (int i = array.length - 1; i >= 0; --i) {
			if (array[i] == value) {
				return i;

			}
		}
		return -1;
	}

	protected int[] _arrayRemoveItem(final int[] array, final int index) {

		if (array == null) {
			return null;
		}

		if (array.length == 1 && index == 0) {
			return null;
		}

		final int[] newArray = new int[array.length - 1];

		if (index > 0) {
			System.arraycopy(array, 0, newArray, 0, index);
		}

		if (index + 1 < array.length) {
			System.arraycopy(array, index + 1, newArray, index, newArray.length - index);
		}

		return newArray;
	}

	protected Object[] _arrayRemoveItem(final Object[] array, final int index) {

		if (array == null) {
			return null;
		}

		if (array.length == 1 && index == 0) {
			return null;
		}

		final Object[] newArray = (Object[]) Array.newInstance(array.getClass().getComponentType(), array.length - 1);

		if (index > 0) {
			System.arraycopy(array, 0, newArray, 0, index);
		}

		if (index + 1 < array.length) {
			System.arraycopy(array, index + 1, newArray, index, newArray.length - index);
		}

		return newArray;
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

		if (_galleryRootItems == null) {
			return;
		}
		for (final GalleryMTItem item : _galleryRootItems) {
			if (item != null) {
				item._deselectAll();
			}
		}

		// Notify listeners if necessary.
		if (notifyListeners) {
			notifySelectionListeners(null, -1, false);
		}
	}

	/**
	 * Handle the drawing of root items
	 * 
	 * @param gc
	 * @param index
	 */
	private void _drawGroup(final GC gc, final int index) {

		GalleryMTItem groupItem = null;

		if (_isVirtualGroups) {

			// Ultra virtual : when a group is about to be drawn, initialize it
			// and update gallery layout according to the new size

			groupItem = _getItem(index, false);

			if (groupItem.isUltraLazyDummy()) {

				final boolean updateLocation = (_isVertical && groupItem.y < _galleryPosition)
						|| (!_isVertical && groupItem.x < _galleryPosition);

				final int oldSize = groupItem.height;

				groupItem = _getItem(index, true);

				// Compatibility mode : ensure all previous items are already
				// initialized
				if (_isVirtualGroupsCompatibilityMode) {
					for (int i = 0; i < index; i++) {
						_getItem(i);
					}
				}

				updateStructuralValues(groupItem, false);

				if (_galleryPositionWhenUpdated != null) {

					final double newPosition = _galleryPositionWhenUpdated.doubleValue();

					// set only once
					_galleryPositionWhenUpdated = null;

					_galleryPosition = (int) (_contentVirtualHeight * newPosition);

				} else if (updateLocation) {
					_galleryPosition += (groupItem.height - oldSize);
				}

				updateScrollBarsProperties();
				redraw();
			}

		} else {
			// Default behavior : get the item with no special handling.
			groupItem = getItem(index);
		}

		if (groupItem == null) {
			return;
		}

		// update item attributes
		groupRenderer.setExpanded(groupItem.isExpanded());

		// Drawing area
		final int galleryPosX = _isVertical ? groupItem.x : groupItem.x - _galleryPosition;
		final int galleryPosY = _isVertical ? groupItem.y - _galleryPosition : groupItem.y;

		final Rectangle clipping = gc.getClipping();
		final Rectangle previousClipping = new Rectangle(clipping.x, clipping.y, clipping.width, clipping.height);

		clipping.intersect(new Rectangle(galleryPosX, galleryPosY, groupItem.width, groupItem.height));
		gc.setClipping(clipping);
		{
			// Draw group
			groupRenderer.draw(
					gc,
					groupItem,
					galleryPosX,
					galleryPosY,
					clipping.x,
					clipping.y,
					clipping.width,
					clipping.height);
		}
		gc.setClipping(previousClipping);

//		System.out.println("_drawGroup: "
//				+ clipping
//				+ "\t"
//				+ previousClipping
//				+ (_isLowQualityPainting ? "\tlow" : "\thigh"));
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	/**
	 * Get group at pixel position
	 * 
	 * @param coords
	 * @return GalleryItem or null
	 */
	private GalleryMTItem _getGroup(final Point coords) {
		// If there is no item in the gallery, return asap
		if (_galleryRootItems == null) {
			return null;
		}

		final int pos = _isVertical ? (coords.y + _galleryPosition) : (coords.x + _galleryPosition);

		int index = 0;
		GalleryMTItem item = null;
		while (index < _galleryRootItems.length) {
			item = getItem(index);

			if ((_isVertical ? item.y : item.x) > pos) {
				break;
			}

			if ((_isVertical ? (item.y + item.height) : (item.x + item.width)) >= pos) {
				return item;
			}

			index++;
		}

		return null;
	}

	/**
	 * This method is used by items to implement getItem( index )
	 * 
	 * @param parentItem
	 * @param itemIndex
	 * @param parentItemCount
	 * @return
	 */
	protected GalleryMTItem _getItem(final GalleryMTItem parentItem, final int itemIndex, final int parentItemCount) {

		if (itemIndex < parentItemCount) {

			// Refresh item if it is not set yet

			/*
			 * this is an optimization for updateItem()
			 */
			if (_isVirtual) {

				GalleryMTItem galleryItem;

				if (parentItem == null) {

					// Parent is the Gallery widget
					updateItem(parentItem, itemIndex, true);

				} else {

					// Parent is another GalleryItem

					galleryItem = parentItem.items[itemIndex];
					if (galleryItem == null) {

						galleryItem = new GalleryMTItem(parentItem, SWT.NONE, itemIndex, false);
						parentItem.items[itemIndex] = galleryItem;
						setData(galleryItem, itemIndex);
					}
				}
			}

			if (parentItem.items == null) {
				return null;
			} else {
				return parentItem.items[itemIndex];
			}
		}

		return null;
	}

	/**
	 * Get the item at index.<br/>
	 * If SWT.VIRTUAL is used and the item has not been used yet, the item is created and a
	 * SWT.SetData is fired.<br/>
	 * This is the internal implementation of this method : checkWidget() is not used.
	 * 
	 * @param index
	 * @return The item at 'index' (not null)
	 */
	protected GalleryMTItem _getItem(final int index) {
		return _getItem(index, true);
	}

	/**
	 * Get the item at 'index'.<br/>
	 * If SWT.VIRTUAL is used, 'create' is true and the item has not been used yet, the item is
	 * created and a SWT.SetData is fired.<br/>
	 * 
	 * @param index
	 * @param create
	 * @return The item at 'index' or null if there was no item and 'create' was false.
	 */
	protected GalleryMTItem _getItem(final int index, final boolean create) {

		final int galleryItemsCount = _galleryRootItems == null ? 0 : _galleryRootItems.length;

		if (index < galleryItemsCount) {

			updateItem(null, index, create);

			return _galleryRootItems[index];
		}

		return null;
	}

	/**
	 * Returns the index of a GalleryItem when it is a root Item
	 * 
	 * @param parentItem
	 * @param item
	 * @return
	 */
	protected int _indexOf(final GalleryMTItem item) {
		final int itemCount = getItemCount();
		if (item == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (1 <= lastIndexOf && lastIndexOf < itemCount - 1) {
			if (_galleryRootItems[lastIndexOf] == item) {
				return lastIndexOf;
			}
			if (_galleryRootItems[lastIndexOf + 1] == item) {
				return ++lastIndexOf;
			}
			if (_galleryRootItems[lastIndexOf - 1] == item) {
				return --lastIndexOf;
			}
		}
		if (lastIndexOf < itemCount / 2) {
			for (int i = 0; i < itemCount; i++) {
				if (_galleryRootItems[i] == item) {
					return lastIndexOf = i;
				}
			}
		} else {
			for (int i = itemCount - 1; i >= 0; --i) {
				if (_galleryRootItems[i] == item) {
					return lastIndexOf = i;
				}
			}
		}
		return -1;
	}

	/**
	 * Returns the index of a GalleryItem when it is not a root Item
	 * 
	 * @param parentItem
	 * @param item
	 * @return
	 */
	protected int _indexOf(final GalleryMTItem parentItem, final GalleryMTItem item) {
		final int itemCount = parentItem.getItemCount();
		if (item == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (1 <= parentItem.lastIndexOf && parentItem.lastIndexOf < itemCount - 1) {
			if (parentItem.items[parentItem.lastIndexOf] == item) {
				return parentItem.lastIndexOf;
			}
			if (parentItem.items[parentItem.lastIndexOf + 1] == item) {
				return ++parentItem.lastIndexOf;
			}
			if (parentItem.items[parentItem.lastIndexOf - 1] == item) {
				return --parentItem.lastIndexOf;
			}
		}
		if (parentItem.lastIndexOf < itemCount / 2) {
			for (int i = 0; i < itemCount; i++) {
				if (parentItem.items[i] == item) {
					return parentItem.lastIndexOf = i;
				}
			}
		} else {
			for (int i = itemCount - 1; i >= 0; --i) {
				if (parentItem.items[i] == item) {
					return parentItem.lastIndexOf = i;
				}
			}
		}
		return -1;
	}

	/**
	 * Forward the mouseDown event to the corresponding group according to the mouse position.
	 * 
	 * @param e
	 * @return
	 */
	protected boolean _mouseDown(final MouseEvent e) {

		final GalleryMTItem group = _getGroup(new Point(e.x, e.y));
		if (group != null) {
			final int pos = _isVertical ? (e.y + _galleryPosition) : (e.x + _galleryPosition);
			return groupRenderer.mouseDown(group, e, new Point(_isVertical ? e.x : pos, _isVertical ? pos : e.y));
		}

		return true;
	}

	protected void _remove(final GalleryMTItem parent, final int index) {
		if (isSelected(parent.items[index])) {
			setSelected(parent.items[index], false, false);
		}

		parent.items = (GalleryMTItem[]) _arrayRemoveItem(parent.items, index);
	}

	protected void _remove(final int index) {
		// if (!virtual) {
		if (isSelected(_galleryRootItems[index])) {
			setSelected(_galleryRootItems[index], false, false);
		}

		_galleryRootItems = (GalleryMTItem[]) _arrayRemoveItem(_galleryRootItems, index);

		// if( virtual)
		// itemCount--;
		// }
	}

	private void _removeSelection(final GalleryMTItem item) {

		if (item.getParentItem() == null) {
			final int index = _indexOf(item);
			selectionFlags[index >> 5] &= ~(1 << (index & 0x1f));
		} else {
			_removeSelection(item.getParentItem(), item);
		}

		final int index = _arrayIndexOf(_selectedItems, item);
		if (index == -1) {
			return;
		}

		_selectedItems = (GalleryMTItem[]) _arrayRemoveItem(_selectedItems, index);

	}

	protected void _removeSelection(final GalleryMTItem parent, final GalleryMTItem item) {
		final int index = _indexOf(parent, item);
		parent.selectionFlags[index >> 5] &= ~(1 << (index & 0x1f));
	}

	protected void _selectAll() {
		select(0, getItemCount() - 1);
	}

	private void _setDefaultRenderers() {
		// Group renderer
		final DefaultGalleryGroupRenderer gr = new DefaultGalleryGroupRenderer();
		gr.setMinMargin(2);
		gr.setItemHeight(56);
		gr.setItemWidth(72);
		gr.setAutoMargin(true);
		gr.setGallery(this);
		groupRenderer = gr;

		// Item renderer
		itemRenderer = new DefaultGalleryItemRenderer();
		itemRenderer.setGallery(this);
	}

	public void _setGalleryItems(final GalleryMTItem[] items) {
		this._galleryRootItems = items;
	}

	void _showItem(final GalleryMTItem item) {
		int y;
		int height;
		final Rectangle rect = groupRenderer.getSize(item);
		if (rect == null) {
			return;
		}

		if (_isVertical) {
			y = rect.y;
			height = rect.height;
			if (y < _galleryPosition) {
				_galleryPosition = y;
			} else if (_galleryPosition + _clientArea.height < y + height) {
				_galleryPosition = y + height - _clientArea.height;
			}

		} else {
			y = rect.x;
			height = rect.width;

			if (y < _galleryPosition) {
				_galleryPosition = y;
			} else if (_galleryPosition + _clientArea.width < y + height) {
				_galleryPosition = y + height - _clientArea.width;
			}
		}

		updateScrollBarsProperties();
		redraw();

	}

	protected void addItem(final GalleryMTItem item, final int position) {
		if (position != -1 && (position < 0 || position > getItemCount())) {
			throw new IllegalArgumentException("ERROR_INVALID_RANGE "); //$NON-NLS-1$
		}
		_addItem(item, position);
	}

	/**
	 * Adds the listener to the collection of listeners who will be notified when the receiver's
	 * selection changes, by sending it one of the messages defined in the
	 * <code>SelectionListener</code> interface.
	 * <p>
	 * When <code>widgetSelected</code> is called, the item field of the event object is valid.
	 * </p>
	 * 
	 * @param listener
	 *            the listener which should be notified
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 * @see SelectionListener
	 * @see #removeSelectionListener
	 * @see SelectionEvent
	 */
	public void addSelectionListener(final SelectionListener listener) {

		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		final TypedListener typedListener = new TypedListener(listener);
		addListener(SWT.Selection, typedListener);
		addListener(SWT.DefaultSelection, typedListener);
	}

	/**
	 * Adds the listener to the collection of listeners who will be notified when an item in the
	 * receiver is expanded or collapsed by sending it one of the messages defined in the
	 * TreeListener interface.
	 * 
	 * @param listener
	 */
	public void addTreeListener(final TreeListener listener) {

		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		addListener(SWT.Expand, new TypedListener(listener));
	}

	/**
	 * Clear one item.<br/>
	 * 
	 * @param index
	 */
	public void clear(final int index) {
		clear(index, false);
	}

	/**
	 * Clear one item and all its children if 'all' is true
	 * 
	 * @param index
	 * @param all
	 */
	public void clear(final int index, final boolean all) {

		// Item is already cleared, return immediately.
		if (_galleryRootItems[index] == null) {
			return;
		}

		if (_isVirtual) {
			// Clear item
			_galleryRootItems[index] = null;

			// In virtual mode, clearing an item can change its content, so
			// force content update
			updateStructuralValues(null, false);
			updateScrollBarsProperties();
		} else {
			// Reset item
			_galleryRootItems[index].clear();
			if (all) {
				_galleryRootItems[index].clearAll(true);
			}
		}

		redraw();
	}

	/**
	 * Clear all GalleryGroups
	 */
	public void clearAll() {
		clearAll(false);
	}

	/**
	 * Clear all Gallery items.<br/>
	 * If the Gallery is virtual, the item count is not reseted and all items will be created again
	 * at their first use.<br/>
	 * 
	 * @param all
	 *            If true, all children will be cleared. Only groups are cleared otherwise.
	 */
	public void clearAll(final boolean all) {

		if (_galleryRootItems == null) {
			return;
		}

		if (_isVirtual) {
			_galleryRootItems = new GalleryMTItem[_galleryRootItems.length];
		} else {
			for (final GalleryMTItem item : _galleryRootItems) {
				if (item != null) {
					if (all) {
						item.clearAll(true);
					} else {
						item.clear();
					}
				}
			}
		}

		// TODO: I'm clearing selection here
		// but we have to check that Table has the same behavior
		_deselectAll(false);

		updateStructuralValues(null, false);
		updateScrollBarsProperties();

		redraw();
	}

	/**
	 * Calculate full height (or width) of the Gallery. The group renderer is used to calculate the
	 * size of each group.
	 * 
	 * @return
	 */
	private int computeContentSize(final GalleryMTItem onlyUpdateGroup) {

		if (groupRenderer == null) {
			return 0;
		}

		groupRenderer.preLayout(null);

		int currentHeight = 0;

		final int mainItemCount = getItemCount();

		for (int i = 0; i < mainItemCount; i++) {

			GalleryMTItem item = null;
			if (_isVirtualGroups) {
				item = _getItem(i, false);
			} else {
				item = _getItem(i);
			}

			if (onlyUpdateGroup != null && !onlyUpdateGroup.equals(item)) {

				// Item has not changed : no layout.

				if (_isVertical) {
					item.y = currentHeight;
					item.x = _clientArea.x;
					currentHeight += item.height;
				} else {
					item.y = _clientArea.y;
					item.x = currentHeight;
					currentHeight += item.width;
				}

			} else {

				groupRenderer.setExpanded(item.isExpanded());

				// TODO: Not used ATM
				// int groupItemCount = item.getItemCount();
				if (_isVertical) {
					item.y = currentHeight;
					item.x = _clientArea.x;
					item.width = _clientArea.width;
					item.height = -1;
					groupRenderer.layout(null, item);
					currentHeight += item.height;

				} else {
					item.y = _clientArea.y;
					item.x = currentHeight;
					item.width = -1;
					item.height = _clientArea.height;
					groupRenderer.layout(null, item);
					currentHeight += item.width;
				}
				// Unused ?
				// Point s = getSize(item.hCount, item.vCount, itemSizeX,
				// itemSizeY, userMargin, realMargin);

				// item.height = s.y;
			}

		}

		groupRenderer.postLayout(null);

		return currentHeight;
	}

	/**
	 * Deselects all items.
	 */
	public void deselectAll() {

		_deselectAll(false);

		redraw();
	}

	/**
	 * @see #setAntialias(int)
	 * @return
	 */
	public int getAntialias() {
		return antialias;
	}

	@Override
	public Color getBackground() {
		return getBackground(false);
	}

	/**
	 * Returns the receiver's background color.
	 * 
	 * @param galleryOnly
	 *            If TRUE, does not try to parent widget or Display defaults to guess the real
	 *            background color. Note : FALSE is the default behavior.
	 * @return The background color or null if galleryOnly was used and the gallery has not
	 *         foreground color set.
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public Color getBackground(final boolean galleryOnly) {
		if (galleryOnly) {
			return backgroundColor;
		}

		if (useControlColors) {
			return super.getBackground();
		}

		return backgroundColor != null ? backgroundColor : getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
	}

	public Rectangle getClientAreaCached() {
		return _clientArea;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Control#getForeground()
	 */
	@Override
	public Color getForeground() {
		return getForeground(false);
	}

	/**
	 * Returns the receiver's foreground color.
	 * 
	 * @param galleryOnly
	 *            If TRUE, does not try to parent widget or Display defaults to guess the real
	 *            foreground color. Note : FALSE is the default behavior.
	 * @return The foreground color or null if galleryOnly was used and the gallery has not
	 *         foreground color set.
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 */
	public Color getForeground(final boolean galleryOnly) {

		if (galleryOnly) {
			return foregroundColor;
		}

		if (useControlColors) {
			return super.getForeground();
		}

		return foregroundColor != null ? foregroundColor : getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
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
	 * <p>
	 * Get group at pixel position (relative to client area).
	 * </p>
	 * <p>
	 * This is an experimental API which is exposing an internal method, it may become deprecated at
	 * some point.
	 * </p>
	 * 
	 * @param coords
	 * @return
	 */
	public GalleryMTItem getGroup(final Point coords) {
		return _getGroup(coords);
	}

	public AbstractGalleryGroupRenderer getGroupRenderer() {
		return groupRenderer;
	}

	/**
	 * @see #setHigherQualityDelay(int)
	 * @return
	 */
	public int getHigherQualityDelay() {
		return higherQualityDelay;
	}

	/**
	 * @see #setInterpolation(int)
	 * @return
	 */
	public int getInterpolation() {
		return interpolation;
	}

	/**
	 * Get the item at index.<br/>
	 * If SWT.VIRTUAL is used and the item has not been used yet, the item is created and a
	 * SWT.SetData event is fired.
	 * 
	 * @param index
	 *            index of the item.
	 * @return the GalleryItem or null if index is out of bounds
	 */
	public GalleryMTItem getItem(final int index) {
		return _getItem(index);
	}

	/**
	 * Get item at pixel position
	 * 
	 * @param coords
	 * @return GalleryItem or null
	 */
	public GalleryMTItem getItem(final Point coords) {

		final int pos = _isVertical ? (coords.y + _galleryPosition) : (coords.x + _galleryPosition);

		final GalleryMTItem group = _getGroup(coords);
		if (group != null) {
			return groupRenderer.getItem(group, new Point(_isVertical ? coords.x : pos, _isVertical ? pos : coords.y));
		}

		return null;
	}

	/**
	 * Return the number of root-level items in the receiver. Does not include children.
	 * 
	 * @return
	 */
	public int getItemCount() {

		if (_galleryRootItems == null) {
			return 0;
		}

		return _galleryRootItems.length;
	}

	/**
	 * Get current item renderer
	 * 
	 * @return
	 */
	public AbstractGalleryItemRenderer getItemRenderer() {
		return itemRenderer;
	}

	public GalleryMTItem[] getItems() {

		if (_galleryRootItems == null) {
			return new GalleryMTItem[0];
		}

		final GalleryMTItem[] itemsLocal = new GalleryMTItem[_galleryRootItems.length];
		System.arraycopy(_galleryRootItems, 0, itemsLocal, 0, _galleryRootItems.length);

		return itemsLocal;
	}

	private boolean getOrder(final GalleryMTItem before, final GalleryMTItem after) {

		if (before == null || after == null) {
			return true;
		}

		final GalleryMTItem newParent = before.getParentItem();
		final GalleryMTItem oldParent = after.getParentItem();

		final int beforeParentIndex = indexOf(newParent);
		final int afterParentIndex = indexOf(oldParent);

		if (newParent == oldParent) {
			int newParentIndex;
			int oldParentIndex;
			if (newParent == null) {
				newParentIndex = indexOf(before);
				oldParentIndex = indexOf(after);

			} else {
				newParentIndex = newParent.indexOf(before);
				oldParentIndex = newParent.indexOf(after);
			}
			return (newParentIndex < oldParentIndex);
		}

		return beforeParentIndex < afterParentIndex;
	}

	public GalleryMTItem[] getSelection() {
		if (_selectedItems == null) {
			return new GalleryMTItem[0];
		}

		return _selectedItems;
	}

	public int getSelectionCount() {
		if (_selectedItems == null) {
			return 0;
		}

		return _selectedItems.length;
	}

	public int getTranslate() {
		return _galleryPosition;
	}

	/**
	 * @see #setVirtualGroupDefaultItemCount(int)
	 * @return
	 */
	public int getVirtualGroupDefaultItemCount() {
		return _virtualGroupDefaultItemCount;
	}

	private int[] getVisibleItems(final Rectangle clipping) {

		if (_galleryRootItems == null) {
			return null;
		}

		final int start = _isVertical //
				? (clipping.y + _galleryPosition)
				: (clipping.x + _galleryPosition);

		final int end = _isVertical
				? (clipping.y + clipping.height + _galleryPosition)
				: (clipping.x + clipping.width + _galleryPosition);

		final ArrayList<Integer> al = new ArrayList<Integer>();
		int index = 0;
		GalleryMTItem item = null;
		while (index < _galleryRootItems.length) {

			if (_isVirtualGroups) {
				item = _getItem(index, false);
			} else {
				item = _getItem(index);
			}

			if ((_isVertical ? item.y : item.x) > end) {
				break;
			}

			if ((_isVertical ? (item.y + item.height) : (item.x + item.width)) >= start) {
				al.add(new Integer(index));
			}

			index++;
		}

		final int[] result = new int[al.size()];

		for (int i = 0; i < al.size(); i++) {
			result[i] = al.get(i).intValue();
		}

		return result;
	}

	/**
	 * Returns the index of a GalleryItem.
	 * 
	 * @param parentItem
	 * @param item
	 * @return
	 */
	public int indexOf(final GalleryMTItem item) {

		if (item == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
			// SWT.error throws an exception
		}

		if (item.getParentItem() == null) {
			return _indexOf(item);
		}

		return _indexOf(item.getParentItem(), item);
	}

	protected boolean isSelected(final GalleryMTItem item) {

		if (item == null) {
			return false;
		}

		if (item.getParentItem() != null) {
			return item.getParentItem().isSelected(item);
		}

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

	/**
	 * @see GalleryMT#setUseControlColors(boolean)
	 * @return true if Gallery uses parent colors.
	 */
	public boolean isUseControlColors() {
		return useControlColors;
	}

	/**
	 * Checks if the Gallery was created with SWT.V_SCROLL (ie has a vertical scroll bar).
	 * 
	 * @return true if the gallery has the SWT.V_SCROLL style.
	 */
	public boolean isVertical() {
		return _isVertical;
	}

	/**
	 * @see #setVirtualGroups(boolean)
	 * @return
	 */
	public boolean isVirtualGroups() {
		return _isVirtualGroups;
	}

	/**
	 * @see #setVirtualGroupsCompatibilityMode(boolean)
	 * @return
	 */
	public boolean isVirtualGroupsCompatibilityMode() {
		return _isVirtualGroupsCompatibilityMode;
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
	protected void notifySelectionListeners(final GalleryMTItem item, final int index, final boolean isDefault) {

		final Event e = new Event();
		e.widget = this;
		e.item = item;
		if (item != null) {
			e.data = item.getData();
		}
		// TODO: report index
		// e.index = index;
		try {
			if (isDefault) {
				notifyListeners(SWT.DefaultSelection, e);
			} else {
				notifyListeners(SWT.Selection, e);
			}
		} catch (final RuntimeException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Send an Expand event for a GalleryItem
	 * 
	 * @param item
	 * @param index
	 */
	protected void notifyTreeListeners(final GalleryMTItem item, final boolean state) {

		final Event e = new Event();
		e.widget = this;
		e.item = item;
		if (item != null) {
			e.data = item.getData();
		}
		// TODO: report index
		// e.index = index;
		try {
			notifyListeners(SWT.Expand, e);
		} catch (final RuntimeException ex) {
			ex.printStackTrace();
		}
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
	void onDispose() {
		// Remove items if not Virtual.
		if (!_isVirtual) {
			removeAll();
		}

		// Dispose renderers
		if (itemRenderer != null) {
			itemRenderer.dispose();
		}

		if (groupRenderer != null) {
			groupRenderer.dispose();
		}

		if (_shell != null) {
			_shell.removeControlListener(_shellControlListener);
		}
	}

	// TODO: Not used ATM
	// private void clear() {
	// checkWidget();
	// if (virtual) {
	// setItemCount(0);
	// } else {
	// items = null;
	// }
	//
	// updateStructuralValues(true);
	// updateScrollBarsProperties();
	// }

	void onMouseDoubleClick(final MouseEvent e) {

		final GalleryMTItem item = getItem(new Point(e.x, e.y));
		if (item != null) {
			notifySelectionListeners(item, 0, true);
		}
		mouseClickHandled = true;
	}

	void onMouseDown(final MouseEvent e) {

		mouseClickHandled = false;

		if (!_mouseDown(e)) {
			mouseClickHandled = true;
			return;
		}

		final GalleryMTItem item = getItem(new Point(e.x, e.y));

		if (e.button == 1) {

			// left mouse button is pressed

			if (item == null) {
				_deselectAll(true);
				redraw();
				mouseClickHandled = true;
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

	void onMouseHandleLeft(final MouseEvent e, final GalleryMTItem item, final boolean down, final boolean up) {
		if (down) {
			if (!isSelected(item)) {
				_deselectAll(false);

				setSelected(item, true, true);

				lastSingleClick = item;
				redraw();
				mouseClickHandled = true;
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
										final GalleryMTItem item,
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
										final GalleryMTItem item,
										final boolean down,
										final boolean up) {
		if (up) {
			if (lastSingleClick != null) {
				_deselectAll(false);

				if (getOrder(item, lastSingleClick)) {
					select(item, lastSingleClick);
				} else {
					select(lastSingleClick, item);
				}
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
	void onMouseHandleRight(final MouseEvent e, final GalleryMTItem item, final boolean down, final boolean up) {
		if (down) {

			if (item != null && !isSelected(item)) {
				_deselectAll(false);
				setSelected(item, true, true);
				redraw();
				mouseClickHandled = true;
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

					scrollVertical();
				}
			} else {

				// not yet implemented
			}

			// set down position to current mouse position
			_mousePanStartPosition = _mouseMovePosition;
		}
	}

	void onMouseUp(final MouseEvent e) {

		_isGalleryPanned = false;

		if (mouseClickHandled) {
			return;
		}

		if (e.button == 1) {

			final GalleryMTItem item = getItem(new Point(e.x, e.y));
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

	void onPaint(final GC gc) {

		final long start = System.nanoTime();

		int itemHeight = -1;
		if (groupRenderer instanceof AbstractGridGroupRenderer) {
			final AbstractGridGroupRenderer gridRenderer = (AbstractGridGroupRenderer) groupRenderer;
			itemHeight = gridRenderer.getItemHeight();
		}
		// is true when image can not be painted with high quality
		final boolean isSmallerThanHQMinSize = itemHeight == -1 || itemHeight < _highQualityMinSize;

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

		final Rectangle clipping = gc.getClipping();

		try {

			if (isLowQualityPainting) {
				gc.setAntialias(SWT.OFF);
				gc.setInterpolation(SWT.OFF);
			} else {
				gc.setAntialias(antialias);
				gc.setInterpolation(interpolation);
			}

			gc.setBackground(getBackground());
//			drawBackground(gc, clipping.x, clipping.y, clipping.width, clipping.height);

			final int[] visibleRootItems = getVisibleItems(clipping);

			if (visibleRootItems != null && visibleRootItems.length > 0) {

				// Call preDraw for optimization
				if (groupRenderer != null) {
					groupRenderer.preDraw(gc);
				}
				if (itemRenderer != null) {
					itemRenderer.preDraw(gc);
				}

				for (int i = visibleRootItems.length - 1; i >= 0; i--) {
					_drawGroup(gc, visibleRootItems[i]);
				}

				// Call postDraw for optimization / cleanup
				if (groupRenderer != null) {
					groupRenderer.postDraw(gc);
				}
				if (itemRenderer != null) {
					itemRenderer.postDraw(gc);
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
				getDisplay().timerExec(higherQualityDelay, redrawTimer);
			}
		}

		final float timeDiff = (float) (System.nanoTime() - start) / 1000000;
		if (timeDiff > 10) {}
//		System.out.println("onPaint:\t" + timeDiff + " ms\t" + clipping);
		// TODO remove SYSTEM.OUT.PRINTLN
	}

	/**
	 * Redraw the item given as parameter.
	 * 
	 * @param item
	 */
	public void redraw(final GalleryMTItem item) {

//		// Redraw only the item's bounds
//		final Rectangle bounds = item.getBounds();
//		redraw(bounds.x, bounds.y, bounds.width, bounds.height, true);
//
//		System.out.println("redraw\t" + bounds);
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	/**
	 * Refresh item by firering SWT.SetData.
	 * <p>
	 * Currently not implemented.
	 * </p>
	 * 
	 * @param index
	 */
	public void refresh(final int index) {
		if (index < getItemCount()) {
			// TODO: refresh
		}
	}

	public void remove(final GalleryMTItem item) {
		if (item.getParentItem() == null) {
			remove(indexOf(item));
		} else {
			item.getParentItem().remove(item);
		}
	}

	public void remove(final int index) {

		_remove(index);

		updateStructuralValues(null, false);
		updateScrollBarsProperties();
		redraw();
	}

	public void removeAll() {

		if (_galleryRootItems != null) {
			// Clear items

			final GalleryMTItem[] tmpArray = new GalleryMTItem[_galleryRootItems.length];
			System.arraycopy(_galleryRootItems, 0, tmpArray, 0, _galleryRootItems.length);

			for (final GalleryMTItem element : tmpArray) {

				// Dispose items if not virtual
				// if (!virtual) {
				if (element != null) {
					element._dispose();
				}
				// }
			}
		}
	}

	/**
	 * Removes the listener from the collection of listeners who will be notified when the
	 * receiver's selection changes.
	 * 
	 * @param listener
	 *            the listener which should no longer be notified
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 * @see SelectionListener
	 * @see #addSelectionListener(SelectionListener)
	 */
	public void removeSelectionListener(final SelectionListener listener) {

		removeListener(SWT.Selection, listener);
		removeListener(SWT.DefaultSelection, listener);
	}

	/**
	 * Removes the listener from the collection of listeners who will be notified when items in the
	 * receiver are expanded or collapsed.
	 * 
	 * @param listener
	 */
	public void removeTreeListener(final SelectionListener listener) {
		removeListener(SWT.Expand, listener);
	}

	protected void scrollHorizontal() {

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

	protected void scrollVertical() {

		final int areaHeight = _clientArea.height;

		if (_contentVirtualHeight > areaHeight) {

			// content is larger than visible client area

			final int barSelection = getVerticalBar().getSelection();

			scroll(0, _galleryPosition - barSelection, 0, 0, _clientArea.width, areaHeight, false);

			_galleryPosition = barSelection;

		} else {
			_galleryPosition = 0;
		}
	}

	private void select(final GalleryMTItem from, final GalleryMTItem to) {
		final GalleryMTItem fromParent = from.getParentItem();
		final GalleryMTItem toParent = to.getParentItem();

		if (fromParent == toParent) {

			if (fromParent == null) {
				final int fromIndex = indexOf(from);
				final int toIndex = indexOf(to);
				select(fromIndex, toIndex);
			} else {
				final int fromIndex = fromParent.indexOf(from);
				final int toIndex = toParent.indexOf(to);
				fromParent.select(fromIndex, toIndex);
			}
		} else {
			final int fromParentIndex = indexOf(fromParent);
			final int toParentIndex = indexOf(toParent);
			final int fromIndex = fromParent.indexOf(from);
			final int toIndex = toParent.indexOf(to);

			fromParent.select(fromIndex, fromParent.getItemCount() - 1);
			for (int i = fromParentIndex + 1; i < toParentIndex; i++) {
				getItem(i)._selectAll();
			}
			toParent.select(0, toIndex);

		}
		notifySelectionListeners(to, indexOf(to), false);
		redraw();
	}

	private void select(final int from, final int to) {
		for (int i = from; i <= to; i++) {
			final GalleryMTItem item = getItem(i);
			_addSelection(item);
			item._selectAll();

		}
	}

	/**
	 * Selects all of the items in the receiver.
	 */
	public void selectAll() {

		_selectAll();
		redraw();
	}

	/**
	 * Send SWT.PaintItem for one item.
	 * 
	 * @param item
	 * @param index
	 * @param gc
	 * @param x
	 * @param y
	 */
	protected void sendPaintItemEvent(	final Item item,
										final int index,
										final GC gc,
										final int x,
										final int y,
										final int width,
										final int height) {

		final Event e = new Event();
		e.item = item;
		e.type = SWT.PaintItem;
		e.index = index;
		// TODO: Does clipping need to be set ?
		// gc.setClipping(x, y, width, height);
		e.gc = gc;
		e.x = x;
		e.y = y;
		e.width = width;
		e.height = height;
		notifyListeners(SWT.PaintItem, e);
	}

	/**
	 * Sets the gallery's anti-aliasing value to the parameter, which must be one of
	 * <code>SWT.DEFAULT</code>, <code>SWT.OFF</code> or <code>SWT.ON</code>.
	 * 
	 * @param antialias
	 */
	public void setAntialias(final int antialias) {
		this.antialias = antialias;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Control#setBackground(org.eclipse.swt.graphics .Color)
	 */
	@Override
	public void setBackground(final Color color) {
		// Cache color locally
		if (!useControlColors) {
			backgroundColor = color;
		}

		// Always call Control#setBackground(); Additionally, this will trigger
		// a redraw.
		super.setBackground(color);
	}

	/**
	 * Sends SWT.SetData event. Used if SWT.VIRTUAL
	 * 
	 * @param galleryItem
	 * @param index
	 */
	protected void setData(final GalleryMTItem galleryItem, final int index) {
		final Item item = galleryItem;
		final Event e = new Event();
		e.item = item;
		e.type = SWT.SetData;
		e.index = index;
		notifyListeners(SWT.SetData, e);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Control#setForeground(org.eclipse.swt.graphics .Color)
	 */
	@Override
	public void setForeground(final Color color) {
		// Cache color locally
		if (!useControlColors) {
			foregroundColor = color;
		}

		// Always call Control#setForeground(); Additionally, this will trigger
		// a redraw.
		super.setForeground(color);
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

	public void setGroupRenderer(final AbstractGalleryGroupRenderer groupRenderer) {
		this.groupRenderer = groupRenderer;

		if (groupRenderer != null) {
			groupRenderer.setGallery(this);
		}

		updateStructuralValues(null, true);
		updateScrollBarsProperties();
		redraw();
	}

	/**
	 * Set the delay after the last user action before the redraw at higher quality is triggered
	 * 
	 * @see #setLowQualityOnUserAction(boolean)
	 * @param higherQualityDelay
	 */
	public void setHigherQualityDelay(final int higherQualityDelay) {
		this.higherQualityDelay = higherQualityDelay;
	}

	public void setImageQuality(final boolean isShowHighQuality, final int hqMinSize) {

		_isShowHighQuality = isShowHighQuality;
		_highQualityMinSize = hqMinSize;
	}

	/**
	 * Sets the gallery's interpolation setting to the parameter, which must be one of
	 * <code>SWT.DEFAULT</code>, <code>SWT.NONE</code>, <code>SWT.LOW</code> or
	 * <code>SWT.HIGH</code>.
	 * 
	 * @param interpolation
	 */
	public void setInterpolation(final int interpolation) {
		this.interpolation = interpolation;
	}

	/**
	 * Sets the number of root-level items contained in the receiver. Only work in VIRTUAL mode.
	 * 
	 * @return
	 */
	public void setItemCount(final int count) {

		if (count == 0) {
			// No items
			_galleryRootItems = null;
		} else {
			// At least one item, create a new array and copy data from the
			// old one.
			final GalleryMTItem[] newItems = new GalleryMTItem[count];
			if (_galleryRootItems != null) {
				System.arraycopy(_galleryRootItems, 0, newItems, 0, Math.min(count, _galleryRootItems.length));
			}
			_galleryRootItems = newItems;
		}

		updateStructuralValues(null, false);
		updateScrollBarsProperties();
		redraw();
	}

	/**
	 * Set item receiver. Usually, this does not trigger gallery update. redraw must be called right
	 * after setGroupRenderer to reflect this change.
	 * 
	 * @param itemRenderer
	 */
	public void setItemRenderer(final AbstractGalleryItemRenderer itemRenderer) {

		this.itemRenderer = itemRenderer;

		if (itemRenderer != null) {
			itemRenderer.setGallery(this);
		}

		redraw();
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
	protected void setSelected(final GalleryMTItem item, final boolean selected, final boolean notifyListeners) {
		if (selected) {
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

			GalleryMTItem notifiedItem = null;

			if (item != null && selected) {
				notifiedItem = item;
			} else {
				if (_selectedItems != null && _selectedItems.length > 0) {
					notifiedItem = _selectedItems[_selectedItems.length - 1];
				}
			}

			int index = -1;
			if (notifiedItem != null) {
				index = indexOf(notifiedItem);
			}

			notifySelectionListeners(notifiedItem, index, false);
		}

	}

	public void setSelection(final GalleryMTItem[] items) {

		_deselectAll(false);

		for (final GalleryMTItem item : items) {
			setSelected(item, true, false);

			// Ensure item is visible
			_showItem(item);

			// Simulate mouse click to enable keyboard navigation
			lastSingleClick = item;
		}
		redraw();
	}

	/**
	 * Set gallery items in the first root item
	 * 
	 * @param sortedGalleryItems
	 */
	public void setSortedItems(final GalleryMTItem[] sortedGalleryItems) {

		if (_galleryRootItems == null || _galleryRootItems.length == 0) {
			return;
		}

		_galleryRootItems[0].items = sortedGalleryItems;

		updateStructuralValues(null, true);
		updateScrollBarsProperties();
		redraw();
	}

	/**
	 * Set useControlColors to true in order to get colors from parent Control (SWT default). This
	 * may generate more objects on painting and slightly slow down the application. See Bug 279822
	 * : https://bugs.eclipse.org/bugs/show_bug.cgi?id=279822 If enabled, you'll get new Color
	 * objects each time you call getXXXColor() on Gallery or GalleryItem. Default is false : colors
	 * are stored locally in Gallery, and you'll get the same object each time you call
	 * getXXXColor() on Gallery orGalleryItem. The Gallery may not catch color changes on parent
	 * control.
	 * 
	 * @param useControlColors
	 */
	public void setUseControlColors(final boolean useControlColors) {
		this.useControlColors = useControlColors;
	}

	/**
	 * @deprecated
	 * @param vertical
	 */
	@Deprecated
	public void setVertical(final boolean vertical) {

		this._isVertical = vertical;
		updateStructuralValues(null, true);
		redraw();
	}

	/**
	 * Set the item count used when a group is not yet initialized (with virtual groups). Since the
	 * virtual groups make the size of the gallery change while scrolling, a fine tuned item count
	 * can improve the accuracy of the slider.
	 * 
	 * @see #setVirtualGroups(boolean)
	 * @param defaultItemCount
	 */
	public void setVirtualGroupDefaultItemCount(final int defaultItemCount) {
		_virtualGroupDefaultItemCount = defaultItemCount;
	}

	/**
	 * Enable virtual groups
	 * <p>
	 * When a gallery has the SWT.VIRTUAL flag, only items are initialized on display. All groups
	 * need to be initialized from the beginning to calculate the total size of the content.
	 * </p>
	 * <p>
	 * Virtual groups enable creating groups AND items lazily at the cost of a poor approximation of
	 * the total size of the content.
	 * </p>
	 * <p>
	 * While a group isn't initialized, the item count defined as default item count is used.
	 * </p>
	 * <p>
	 * When a group comes into view, it is initialized using the setData event, and the size of the
	 * gallery content is updated to match the real value.
	 * </p>
	 * <p>
	 * From the developer point of view, virtual groups uses exactly the same code as the standard
	 * virtual mode of SWT.
	 * </p>
	 * <p>
	 * This mode can create visual glitches with code that automatically scrolls the widget such as
	 * SAT Smooth Scrolling. In that case, you can enable the compatibility mode which is little
	 * less lazy that the default virtual groups, but still better than the standard virtual mode
	 * </p>
	 * 
	 * @see #setVirtualGroupDefaultItemCount(int)
	 * @see #setVirtualGroupsCompatibilityMode(boolean)
	 * @param virtualGroups
	 */
	public void setVirtualGroups(final boolean virtualGroups) {
		this._isVirtualGroups = virtualGroups;
	}

	/**
	 * Enable the compatibility workaround for problems with the ultra virtual mode.
	 * 
	 * @see #setVirtualGroups(boolean)
	 * @param compatibilityMode
	 */
	public void setVirtualGroupsCompatibilityMode(final boolean compatibilityMode) {
		_isVirtualGroupsCompatibilityMode = compatibilityMode;
	}

	/**
	 * Scroll the Gallery in order to make 'item' visible.
	 * 
	 * @param item
	 *            Item to show
	 */
	public void showItem(final GalleryMTItem item) {
		_showItem(item);
	}

	/**
	 * If table is virtual and item at pos i has not been set, call the callback listener to set its
	 * value.
	 * 
	 * @return
	 */
	private void updateItem(final GalleryMTItem parentItem, final int i, final boolean create) {

		if (_isVirtual) {

			GalleryMTItem galleryItem;

			if (parentItem == null) {

				// Parent is the Gallery widget
				galleryItem = _galleryRootItems[i];

				if (galleryItem == null || (_isVirtualGroups && galleryItem.isUltraLazyDummy() && create)) {

					galleryItem = new GalleryMTItem(this, SWT.NONE, i, false);
					_galleryRootItems[i] = galleryItem;

					if (_isVirtualGroups && !create) {
						galleryItem.setItemCount(_virtualGroupDefaultItemCount);
						galleryItem.setUltraLazyDummy(true);
						galleryItem.setExpanded(true);
					} else {
						setData(galleryItem, i);
					}
				}
			} else {

				// Parent is another GalleryItem

				galleryItem = parentItem.items[i];
				if (galleryItem == null) {

					galleryItem = new GalleryMTItem(parentItem, SWT.NONE, i, false);
					parentItem.items[i] = galleryItem;
					setData(galleryItem, i);
				}
			}
		}

	}

	/**
	 * Move the scrollbar to reflect the current visible items position.
	 * 
	 * @param bar
	 *            - the scroll bar to move
	 * @param clientArea
	 *            - Client (visible) area size
	 * @param contentSize
	 *            - Total Size
	 */
	private void updateScrollBarProperties(final ScrollBar bar, final int clientArea, final int contentSize) {

		if (bar == null) {
			return;
		}

		bar.setMinimum(0);
		bar.setMaximum(contentSize);
		bar.setPageIncrement(clientArea);
		bar.setThumb(clientArea);

		// Let the group renderer use a custom increment value.
		if (groupRenderer != null) {
			bar.setIncrement(groupRenderer.getScrollBarIncrement());
		}

		if (contentSize > clientArea) {

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
	 * Move the scrollbar to reflect the current visible items position. <br/>
	 * The bar which is moved depends of the current gallery scrolling : vertical or horizontal.
	 */
	protected void updateScrollBarsProperties() {

		if (_isVertical) {
			updateScrollBarProperties(getVerticalBar(), _clientArea.height, _contentVirtualHeight);
		} else {
			updateScrollBarProperties(getHorizontalBar(), _clientArea.width, _contentVirtualWidth);
		}

	}

	/**
	 * Recalculate structural values using the group renderer<br>
	 * Gallery and item size will be updated.
	 * 
	 * @param keepLocation
	 *            if true, the current scrollbars position ratio is saved and restored even if the
	 *            gallery size has changed. (Visible items stay visible)
	 * @deprecated Use {@link #updateStructuralValues(GalleryMTItem,boolean)} instead
	 */
	@Deprecated
	protected void updateStructuralValues(final boolean keepLocation) {
		updateStructuralValues(null, keepLocation);
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
	protected void updateStructuralValues(final GalleryMTItem changedGroup, final boolean isKeepLocation) {

		float pos = 0;

		if (_isVertical) {

			// vertical

			if (_contentVirtualHeight > 0 && isKeepLocation) {
				pos = (float) (_galleryPosition + 0.5 * _clientArea.height) / _contentVirtualHeight;
			}

			_contentVirtualWidth = _clientArea.width;
			_contentVirtualHeight = computeContentSize(changedGroup);

			if (isKeepLocation) {
				_galleryPosition = (int) (_contentVirtualHeight * pos - 0.5 * _clientArea.height);
			}

		} else {

			// horizontal

			if (_contentVirtualWidth > 0 && isKeepLocation) {
				pos = (float) (_galleryPosition + 0.5 * _clientArea.width) / _contentVirtualWidth;
			}

			_contentVirtualWidth = computeContentSize(changedGroup);
			_contentVirtualHeight = _clientArea.height;

			if (isKeepLocation) {
				_galleryPosition = (int) (_contentVirtualWidth * pos - 0.5 * _clientArea.width);
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
//		final GalleryMTItem currentItem = getItem(_mouseMovePosition);
//
//		if (currentItem == null) {
//			return;
//		}

		final AbstractGalleryGroupRenderer renderer = getGroupRenderer();
		if (renderer instanceof AbstractGridGroupRenderer) {
			final AbstractGridGroupRenderer groupRenderer = (AbstractGridGroupRenderer) renderer;

			final int minHeight = groupRenderer.getItemMinHeight();
			final int maxHeight = groupRenderer.getItemMaxHeight();
			if (minHeight == -1 || maxHeight == -1) {
				// min or max height is not set
				return;
			}

			int itemWidth = groupRenderer.getItemWidth();

			int ZOOM_INCREMENT = 5;
			if (isShiftKey && isCtrlKey == false) {
				ZOOM_INCREMENT = 1;
			} else if (isCtrlKey && isShiftKey == false) {

				ZOOM_INCREMENT = 10;

//				ZOOM_INCREMENT = (int) Math.pow(itemHeight / 4, 1.00);

			} else if (isCtrlKey && isShiftKey) {
				ZOOM_INCREMENT = 50;
			}

			if (isZoomIn) {

				// zoom in

				// check if max zoom is reached
				if (itemWidth >= maxHeight) {
					// max is reached
					return;
				}

				itemWidth += ZOOM_INCREMENT;
				if (itemWidth > maxHeight) {
					itemWidth = maxHeight;
				}

			} else {

				// zoom out

				if (itemWidth <= minHeight) {
					// min is reached
					return;
				}

				itemWidth -= ZOOM_INCREMENT;
				if (itemWidth < minHeight) {
					itemWidth = minHeight;
				}
			}

			final double itemRatio = groupRenderer.getItemRatio();

			final int itemHeight = (int) (itemWidth / itemRatio);

			groupRenderer.setItemSize(itemWidth, itemHeight);

			notifyZoomListener(itemWidth, itemHeight);
		}
	}

}
