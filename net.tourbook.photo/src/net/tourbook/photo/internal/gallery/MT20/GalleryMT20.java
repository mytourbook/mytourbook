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
package net.tourbook.photo.internal.gallery.MT20;

import java.util.Collection;
import java.util.HashMap;

import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.photo.IExternalGalleryListener;
import net.tourbook.photo.IPhotoProvider;
import net.tourbook.photo.internal.Messages;
import net.tourbook.photo.internal.preferences.PrefPagePhotoDirectory;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * This gallery has it's origin in http://www.eclipse.org/nebula/widgets/gallery/gallery.php but it
 * has been modified in many areas, grouping has been removed, filtering and sorting has been added.
 */
public abstract class GalleryMT20 extends Canvas {

	private static final int					GALLERY_ITEM_MIN_SIZE		= 10;

	private boolean								_isVertical;
	private boolean								_isHorizontal;

	private boolean								_isMultiSelection;
	private boolean								_isGalleryMoved;

	/**
	 * Current gallery top/left position (this is also the scroll bar position). Can be used by
	 * renderer during paint.
	 */
	private int									_galleryPosition			= 0;

	private int									_prevGalleryPosition;

	/**
	 * This is the saved and restored position. The gallery will be positioned here, until the user
	 * has scrolled or navigated the gallery.
	 */
	private Double								_forcedGalleryPosition;

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
	private int									_interpolation				= SWT.HIGH;

	/**
	 * Image quality : antialias
	 */
	private int									_antialias					= SWT.ON;

	/**
	 * Width for the whole gallery
	 */
	private int									_contentVirtualWidth		= 0;

	/**
	 * Height for the whole gallery
	 */
	private int									_contentVirtualHeight		= 0;

	private int									_prevViewportWidth;

	private int									_prevViewportHeight;
	private int									_prevContentHeight;
	private int									_prevContentWidth;
	private AbstractGalleryMT20ItemRenderer		_itemRenderer;

	private final ListenerList					_hoveredListeners			= new ListenerList(ListenerList.IDENTITY);

	/**
	 * Cached client area
	 */
	private Rectangle							_clientArea;

	private Composite							_parent;

	private ControlAdapter						_parentControlListener;

	private int									_higherQualityDelay;
	private RedrawTimer							_redrawTimer				= new RedrawTimer();

	/**
	 * Contains gallery items which has been created, not all gallery items must have been created,
	 * they are virtual.
	 */
	private HashMap<String, GalleryMT20Item>	_createdGalleryItems		= new HashMap<String, GalleryMT20Item>();

	private GalleryMT20Item[]					_virtualGalleryItems;

	/**
	 * Contains items indices for the current client area. It can also contains indices for gallery
	 * item which are out of scope. Therefore it is necessary to check if the index is within the
	 * arraybounds of {@link #_virtualGalleryItems}.
	 * <p>
	 * This is used to stop loading images which are not displayed.
	 */
	private int[]								_clientAreaItemsIndices;

	/**
	 * Contains gallery items which are currently be selected in the UI.
	 */
	private HashMap<String, GalleryMT20Item>	_selectedItems				= new HashMap<String, GalleryMT20Item>();
	private int[]								_initialSelectedItems;

	/**
	 * Selection bit flags. Each 'int' contains flags for 32 items. It contains selection flags for
	 * all virtual items.
	 */
	private int[]								_selectionFlags				= null;

	/**
	 * Default image ratio between image width/height. It is the average between 4000x3000 (1.3333)
	 * and 5184x3456 (1.5)
	 */
	private double								_itemRatio					= 15.0 / 10;								//((4.0 / 3.0) + (15.0 / 10.0)) / 2;

	private int									_itemWidth					= 80;

	private int									_itemHeight					= (int) (_itemWidth / _itemRatio);

	/**
	 * Item width when gallery orientation is vertical
	 */
	private int									_verticalItemWidth;

	/**
	 * @return Contains minimum gallery item width or <code>-1</code> when value is not set.
	 */
	private int									_minItemWidth				= -1;
	/**
	 * @return Contains maximum gallery item width or <code>-1</code> when value is not set.
	 */
	private int									_maxItemWidth				= -1;

	/**
	 * Number of horizontal items for all virtual gallery item.
	 */
	private int									_gridHorizItems;

	/**
	 * Number of vertical items for all virtual gallery item.
	 */
	private int									_gridVertItems;

	/**
	 * Is <code>true</code> during zooming. OSX do fire a mouse wheel event always, Win do fire a
	 * mouse wheel event when scrollbars are not visible.
	 * <p>
	 * Terrible behaviour !!!
	 */
	private boolean								_isZoomed;

	private Point								_mouseMovePosition;

	private Point								_mousePanStartPosition;

	private int									_lastZoomEventTime;

	private boolean								_isMouseClickHandled;
	private boolean								_isGalleryPanned;
	/**
	 * Vertical/horizontal offset for centered gallery items
	 */
	private int									_itemCenterOffsetX;

	/**
	 * Keeps track of the last selected item. This is necessary to support "Shift+Mouse button"
	 * where we have to select all items between the previous and the current item and keyboard
	 * navigation.
	 */
	private int									_lastSingleClick			= -1;

	/**
	 * Gallery item which was selected at last
	 */
	private GalleryMT20Item						_lastSelectedItem;

	private int									_lastSelectedItemIndex;

	/**
	 * Is <code>true</code> when the previous pressed key was also pressed with the shift key, this
	 * is used to identify multiple selections with the keyboard.
	 */
	private boolean								_prevKeyIsShift;

	private FullScreenImageViewer				_fullScreenImageViewer;

	private ActionOpenPrefDialog				_actionGalleryPrefPage;

	/**
	 * Is <code>true</code> when gallery has currently the focus.
	 */
	private boolean								_isFocusActive;

	private IGalleryContextMenuProvider			_customContextMenuProvider;

	/**
	 * Menu manager for the gallery context menu.
	 */
	private MenuManager							_contextMenuMgr;

	private IExternalGalleryListener			_externalGalleryListener;

	/**
	 * When <code>true</code> other shell actions are displayed.
	 */
	private boolean								_isShowOtherShellActions	= true;

	private IPhotoProvider						_photoProvider;

	private class RedrawTimer implements Runnable {
		public void run() {

			if (isDisposed()) {
				return;
			}

//			System.out.println(UI.timeStampNano() + " RedrawTimer\t");
//			// TODO remove SYSTEM.OUT.PRINTLN

			redrawGallery();
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
//		super(parent, style | SWT.DOUBLE_BUFFERED);
		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
//		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.NO_MERGE_PAINTS);
//		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND | SWT.NO_MERGE_PAINTS);
//		super(parent, style | SWT.NO_BACKGROUND);
//		super(parent, style | SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE);
//		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE);
//		super(parent, style | SWT.NO_MERGE_PAINTS);

		_isVertical = (style & SWT.V_SCROLL) > 0;
		_isHorizontal = !_isVertical;

		_isMultiSelection = (style & SWT.MULTI) > 0;

		_clientArea = getClientArea();

		setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));

		// Add listeners : redraws, mouse and keyboard
		addDisposeListeners();
		addResizeListeners();
		addPaintListeners();
		addScrollBarsListeners();
		addMouseListeners();
		addKeyListeners();
		addFocusListener();
		addTraverseListener();

		createActions();
		createContextMenu();

		// set item renderer
		_itemRenderer = new DefaultGalleryMT20ItemRenderer();

		// set fullsize viewer
		_fullScreenImageViewer = new FullScreenImageViewer(this, _itemRenderer);

		updateGallery(false);
	}

	/**
	 * Add internal dispose listeners to this gallery.
	 */
	private void addDisposeListeners() {
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	private void addFocusListener() {

		addFocusListener(new FocusListener() {

			@Override
			public void focusGained(final FocusEvent event) {
				onFocusGained(event);
			}

			@Override
			public void focusLost(final FocusEvent event) {
				onFocusLost(event);
			}
		});
	}

	public void addItemHoveredListener(final IItemHovereredListener hoveredListener) {
		_hoveredListeners.add(hoveredListener);
	}

	private void addKeyListeners() {
		addKeyListener(new KeyListener() {

			public void keyPressed(final KeyEvent e) {
				onKeyPressed(e);
			}

			public void keyReleased(final KeyEvent e) {
				// Nothing yet.
			}
		});
	}

	/**
	 * Add internal mouse listeners to this gallery.
	 */
	private void addMouseListeners() {

		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				onMouseWheel(event);
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

		addMenuDetectListener(new MenuDetectListener() {
			@Override
			public void menuDetected(final MenuDetectEvent menuEvent) {
				onMouseContextMenu(menuEvent);
			}
		});
	}

	/**
	 * Add internal paint listeners to this gallery.
	 */
	private void addPaintListeners() {
		addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent event) {
				onPaint(event.gc);
			}
		});
	}

	/**
	 * Add internal resize listeners to this gallery.
	 */
	private void addResizeListeners() {

		addControlListener(new ControlAdapter() {

			@Override
			public void controlResized(final ControlEvent event) {
				onResize();
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
	private void addScrollBarsListeners() {

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
					if (_isHorizontal) {
						onScrollHorizontal();
					}
				}
			});
		}

	}

	private void addTraverseListener() {
		addTraverseListener(new TraverseListener() {

			@Override
			public void keyTraversed(final TraverseEvent event) {

				/*
				 * traverse with the tab key to the next/previous control
				 */
				switch (event.detail) {
				case SWT.TRAVERSE_TAB_NEXT:
				case SWT.TRAVERSE_TAB_PREVIOUS:
					event.doit = true;
					break;
				}
			}
		});
	}

	/**
	 * Center selected item
	 * 
	 * @return Returns center position ratio for the last selected gallery item.
	 */
	private Double centerSelectedItem() {

		if (_lastSelectedItem == null) {
			return null;
		}

		Double galleryPositionRatio = null;

		final int row = _lastSelectedItemIndex / _gridHorizItems;
		final int numberOfRows = _gridVertItems;

		final double rowRatio = numberOfRows == 0 ? 0 : (double) row / numberOfRows;

		if (_isVertical) {

			final double topLeftRow = _contentVirtualHeight * rowRatio;

			// center vertically
			final double topLeftItem = (0.5 * _clientArea.height) - (0.5 * _itemHeight);

			final int verticalCenterPosition = (int) (topLeftRow - topLeftItem);

			galleryPositionRatio = (double) (verticalCenterPosition) / _contentVirtualHeight;

		} else {

			// not yet implemented
		}

		return galleryPositionRatio;
	}

	private void createActions() {

		_actionGalleryPrefPage = new ActionOpenPrefDialog(
				Messages.Action_Photo_OpenPrefPage_Gallery,
				PrefPagePhotoDirectory.ID);
	}

	/**
	 * Create context menu for the gallery
	 */
	private void createContextMenu() {

		_contextMenuMgr = new MenuManager();

		_contextMenuMgr.setRemoveAllWhenShown(true);

		_contextMenuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr) {
				fillContextMenu(menuMgr);
			}
		});

		setMenu(_contextMenuMgr.createContextMenu(this));
	}

	private void delay() {
		try {
			Thread.sleep(300);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Deselects all items.
	 */
	public void deselectAll() {

		deselectAll(false);

		redrawGallery();
	}

	/**
	 * Deselects all items and send selection event depending on parameter.
	 * 
	 * @param isNotifyListeners
	 *            If true, a selection event will be sent to all the current selection listeners.
	 */
	private void deselectAll(final boolean isNotifyListeners) {

		_lastSelectedItem = null;
		_selectedItems.clear();

		// Deselect groups
		// We could set selectionFlags to null, but we rather set all values to
		// 0 to redure garbage collection. On each iteration, we deselect 32
		// items.
		if (_selectionFlags != null) {
			for (int i = 0; i < _selectionFlags.length; i++) {
				_selectionFlags[i] = 0;
			}
		}

		// Notify listeners if necessary.
		if (isNotifyListeners) {
			notifySelectionListeners(null, -1, false);
		}
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		if (_customContextMenuProvider != null) {
			_customContextMenuProvider.fillContextMenu(menuMgr);
		}

		if (_isShowOtherShellActions) {

			menuMgr.add(new Separator());
			menuMgr.add(_actionGalleryPrefPage);
		}
	}

	private void fireItemHoverEvent(final int mouseX, final int mouseY) {

		GalleryMT20Item hoveredItem = null;

		if (_isGalleryPanned) {

			// tooltip is disable when items are panned, this behaviour needs more investigation, not now

		} else {

			final int itemIndex = getItemIndexFromPosition(mouseX, mouseY);

			hoveredItem = getInitializedItem(itemIndex);

			if (hoveredItem != null) {
				updateItemPosition(hoveredItem, itemIndex);
			}
		}

		// fire event to hover listener
		final Object[] listeners = _hoveredListeners.getListeners();
		for (final Object listener : listeners) {
			((IItemHovereredListener) listener).hoveredItem(hoveredItem);
		}
	}

	/**
	 * @return Returns all virtual gallery items. These items can be <code>null</code> when they
	 *         have not yet been displayed.
	 */
	public GalleryMT20Item[] getAllVirtualItems() {
		return _virtualGalleryItems;
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

// this is not working, some items are not displayed !!!
//			if (clipWidth == _itemWidth) {
//
//				// optimize when only 1 item is visible in the clipping area
//
//				final int horizontalItem = (clipX) / _itemWidth;
//
//				firstItem = firstLine * _gridHorizItems;
//				firstItem += horizontalItem;
//
//				lastItem = firstItem + 1;
//
//			} else {

			firstItem = firstLine * _gridHorizItems;
			lastItem = (lastLine + 1) * _gridHorizItems;
//			}

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

			final int firstItem = firstLine * _gridVertItems;

			int lastLine = (clipX + _galleryPosition + clipWidth) / _itemWidth;

			if (lastLine < firstLine) {
				lastLine = firstLine;
			}

			final int lastItem = (lastLine + 1) * _gridVertItems;

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

	public MenuManager getContextMenuManager() {
		return _contextMenuMgr;
	}

	public IGalleryContextMenuProvider getContextMenuProvider() {
		return _customContextMenuProvider;
	}

	public HashMap<String, GalleryMT20Item> getCreatedGalleryItems() {
		return _createdGalleryItems;
	}

	/**
	 * Initializes a gallery item which can be used to set data into the item. This method is called
	 * before a gallery item is painted.
	 * 
	 * @param itemIndex
	 *            Index within the gallery items, these are the gallery items which the gallery can
	 *            display.
	 * @return
	 */
	public abstract IGalleryCustomData getCustomData(final int itemIndex);

	public FullScreenImageViewer getFullsizeViewer() {
		return _fullScreenImageViewer;
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
	 * @param itemIndex
	 * @return Returns gallery item (which has been initialized with custom data) from the given
	 *         gallery position or <code>null</code> when the index is out of scope.
	 */
	private GalleryMT20Item getInitializedItem(final int itemIndex) {

		if (itemIndex < 0 || itemIndex >= _virtualGalleryItems.length) {
			// index is out of scope
			return null;
		}

		GalleryMT20Item galleryItem = _virtualGalleryItems[itemIndex];

		if (galleryItem == null) {

			galleryItem = new GalleryMT20Item(this);

			final IGalleryCustomData customData = getCustomData(itemIndex);

			if (customData != null) {

				galleryItem.customData = customData;
				galleryItem.uniqueItemID = customData.getUniqueId();

				_createdGalleryItems.put(galleryItem.uniqueItemID, galleryItem);

				_virtualGalleryItems[itemIndex] = galleryItem;
			}
		}

		return galleryItem;
	}

	/**
	 * Get item virtual index at pixel position
	 * 
	 * @param coords
	 * @return Returns gallery item index in {@link #_virtualGalleryItems} or <code> <0</code> when
	 *         item is not available.
	 */
	public int getItemIndexFromPosition(final int viewPortX, final int viewPortY) {

		if (_clientArea.contains(viewPortX, viewPortY) == false) {
			// mouse is outside of the gallery
			return -1;
		}

		int contentPosX;
		int contentPosY;

		if (_isVertical) {

			// vertical gallery

			contentPosX = viewPortX - _itemCenterOffsetX;
			contentPosY = _galleryPosition + viewPortY;

		} else {

			// horizontal gallery

			contentPosX = _galleryPosition + viewPortX;
			contentPosY = viewPortY;// - _itemCenterOffsetX;
		}

		final int indexX = contentPosX / _itemWidth;
		final int indexY = contentPosY / _itemHeight;

		// ckeck if mouse click is outside of the gallery horizontal items
		if (indexX >= _gridHorizItems) {
			return -1;
		}

		// ckeck if mouse click is outside of the gallery vertical items
		if (indexY >= _gridVertItems) {
			return -1;
		}

		final int itemIndex = indexY * _gridHorizItems + indexX;

		// ensure array bounds
		final int maxItems = _virtualGalleryItems.length;
		if (itemIndex >= maxItems) {
			return -1;
		}

		return itemIndex;
	}

	/**
	 * @return Returns ratio with: width / height
	 */
	public double getItemRatio() {
		return _itemRatio;
	}

	/**
	 * @return Returns width for one gallery item, height is computed with the {@link #_itemRatio}.
	 */
	public int getItemWidth() {
		return _itemWidth;
	}

	public int getNumberOfHorizontalImages() {
		return _gridHorizItems;
	}

	public IPhotoProvider getPhotoProvider() {
		return _photoProvider;
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
	 * @return Returns all selected items in an unorderd {@link Collection}.
	 */
	public Collection<GalleryMT20Item> getSelection() {
		return _selectedItems.values();
	}

	public int[] getSelectionIndex() {

		final int selectedItemsSize = _selectedItems.size();

		if (_selectionFlags == null || selectedItemsSize == 0) {
			return new int[0];
		}

		final int[] selectedIndex = new int[selectedItemsSize];
		int selectionIndex = 0;
		int itemIndex = 0;

		if (_selectionFlags != null) {
			for (final int flags : _selectionFlags) {

				for (int falgIndex = 0; falgIndex < 32; falgIndex++) {

					final boolean isSelected = flags != 0 && (flags & 1 << (itemIndex & 0x1f)) != 0;

					if (isSelected) {
						selectedIndex[selectionIndex++] = itemIndex;
					}

					itemIndex++;
				}
			}
		}

		return selectedIndex;
	}

	public int getVerticalItemWidth() {
		return _verticalItemWidth;
	}

	/**
	 * @param isZoomIn
	 * @param isShiftKey
	 * @param isCtrlKey
	 * @return Returns zoomed size starting with {@link #_itemWidth}.
	 *         <p>
	 *         The returned value is also checked against {@link #_minItemWidth} and
	 *         {@link #_maxItemWidth} values
	 */
	private int getZoomedSize(final boolean isZoomIn, final boolean isShiftKey, final boolean isCtrlKey) {

		int ZOOM_INCREMENT = 5;
		if (isShiftKey && isCtrlKey == false) {
			ZOOM_INCREMENT = 1;
		} else if (isCtrlKey && isShiftKey == false) {

			ZOOM_INCREMENT = 10;

		} else if (isCtrlKey && isShiftKey) {
			ZOOM_INCREMENT = 50;
		}

		int zoomedSize = _itemWidth;

		if (isZoomIn) {

			// zoom in

			// check if max zoom is reached
			if (zoomedSize >= _maxItemWidth) {
				// max is reached
				return _maxItemWidth;
			}

			zoomedSize += ZOOM_INCREMENT;

			if (zoomedSize > _maxItemWidth) {
				zoomedSize = _maxItemWidth;
			}

		} else {

			// zoom out

			if (_minItemWidth != -1 && zoomedSize <= _minItemWidth) {
				// min is reached
				return _minItemWidth;
			}

			zoomedSize -= ZOOM_INCREMENT;

			if (_maxItemWidth != -1 && zoomedSize < _minItemWidth) {
				zoomedSize = _minItemWidth;
			}
		}

		return zoomedSize;
	}

	private void hideTooltip() {

		if (_virtualGalleryItems == null) {

			// gallery is not yet fully initialized

			return;
		}

		// hide tooltip
		fireItemHoverEvent(-9999, -9999);
	}

	/**
	 * @param itemIndex
	 * @return Returns <code>true</code> when the item is selected
	 */
	private boolean isItemSelected(final int itemIndex) {

		if (_selectionFlags == null) {
			return false;
		}

		final int itemFlagIndex = itemIndex >> 5;
		if (itemFlagIndex >= _selectionFlags.length || itemFlagIndex < 0) {
			return false;
		}

		final int flags = _selectionFlags[itemFlagIndex];

		return flags != 0 && (flags & 1 << (itemIndex & 0x1f)) != 0;
	}

	/**
	 * @param checkedGalleryItem
	 * @return Returns <code>true</code> when the requested gallery item is currently visible in the
	 *         client area.
	 *         <p>
	 *         The gallery item location is updated.
	 */
	public boolean isItemVisible(final GalleryMT20Item checkedGalleryItem) {

		if (_virtualGalleryItems == null
				|| _virtualGalleryItems.length == 0
				|| _clientAreaItemsIndices == null
				|| _clientAreaItemsIndices.length == 0) {
			return false;
		}

		final int numberOfAreaItems = _clientAreaItemsIndices.length;
		final int numberOfVirtualItems = _virtualGalleryItems.length;

		for (int areaIndex = 0; areaIndex < numberOfAreaItems; areaIndex++) {

			final int virtualIndex = _clientAreaItemsIndices[areaIndex];

			// ensure number of available items
			if (virtualIndex >= numberOfVirtualItems) {
				return false;
			}

			final GalleryMT20Item galleryItem = _virtualGalleryItems[virtualIndex];

			if (galleryItem == checkedGalleryItem) {

				updateItemPosition(galleryItem, virtualIndex);

				return true;
			}
		}

		return false;
	}

	public boolean isVertical() {
		return _isVertical;
	}

	/**
	 * Navigate items for the full size viewer.
	 * 
	 * @param numberOfItems
	 */
	void navigateItem(final int numberOfItems) {

		final int virtualSize = _virtualGalleryItems.length;

		if (virtualSize < 2) {
			// there is nothing to navigate
			return;
		}

		boolean isNavigate = false;

		if (_lastSelectedItemIndex >= virtualSize) {

			// it's possible that the gallery content has changed

			_lastSelectedItemIndex = 0;

			isNavigate = true;
		}

		final int oldIndex = _lastSelectedItemIndex;

		if (numberOfItems < 0) {

			// previous item

			if (numberOfItems == Integer.MIN_VALUE) {

				// 1st item

				navigateItem_10(0);

			} else if (_lastSelectedItemIndex > 0) {

				// 2nd ... nth item

				navigateItem_10(_lastSelectedItemIndex - 1);

			} else if (_lastSelectedItemIndex == 0) {

				// 1st item

				navigateItem_10(_lastSelectedItemIndex);
			}

		} else {

			// next item

			if (numberOfItems == Integer.MAX_VALUE) {

				// last item

				navigateItem_10(virtualSize - 1);

			} else if (_lastSelectedItemIndex < virtualSize - 1) {

				navigateItem_10(_lastSelectedItemIndex + 1);

			} else if (_lastSelectedItemIndex == virtualSize - 1) {

				// last item

				navigateItem_10(_lastSelectedItemIndex);
			}
		}

		if (isNavigate || oldIndex != _lastSelectedItemIndex) {

			// index has changed

			showFullsizeImage(_lastSelectedItemIndex);
		}
	}

	private void navigateItem_10(final int itemIndex) {

		deselectAll(false);

		selectionAdd(itemIndex);
	}

	/**
	 * Send a selection event {@link SWT#Selection} or {@link SWT#DefaultSelection} for a gallery
	 * item.
	 * <p>
	 * {@link Event#data} contains the selected/deselected gallery item or <code>null</code> when
	 * nothing is selected
	 * 
	 * @param item
	 * @param index
	 * @param isDefault
	 */
	private void notifySelectionListeners(final GalleryMT20Item item, final int index, final boolean isDefault) {

		final Event e = new Event();
		e.widget = this;

		if (item != null) {
			e.data = item;
		}

		try {
			if (isDefault) {
				notifyListeners(SWT.DefaultSelection, e);
			} else {
				notifyListeners(SWT.Selection, e);
			}
		} catch (final RuntimeException ex) {
			StatusUtil.log(ex);
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
	 * Fullsize viewer is closed, show last selected item
	 */
	void onCloseFullsizeViewer() {

		showItem(_lastSelectedItemIndex);
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

		_fullScreenImageViewer.close();
	}

	private void onFocusGained(final FocusEvent event) {

		_isFocusActive = true;

//		if (_lastSelectedItem == null) {
//
//			// nothing is selected, select item in the center
//
//			int itemIndexFromPosition = getItemIndexFromPosition(_clientArea.width / 2, _clientArea.height / 2);
//
//			if (itemIndexFromPosition < 0) {
//				itemIndexFromPosition = getItemIndexFromPosition(0, 0);
//			}
//
//			if (itemIndexFromPosition >= 0) {
//				selectItemSingle(itemIndexFromPosition, true, true);
//			}
//		}

		redrawGallery();
	}

	private void onFocusLost(final FocusEvent event) {

		_isFocusActive = false;
		redrawGallery();
	}

	/**
	 * <pre>
	 * 
	 * Modifier Mask		Description
	 * 
	 * SWT.MOD1 			The first modifier was down (often SWT.CONTROL)
	 * SWT.MOD2 			The second modifier was down (often SWT.SHIFT)
	 * SWT.MOD3 			The third modifier was down (often SWT.ALT)
	 * SWT.MOD4 			The fourth modifier was down (often zero)
	 * SWT.MODIFIER_MASK 	Bitwise-OR of all valid modifiers
	 * 
	 * </pre>
	 * 
	 * @param keyEvent
	 */
	private void onKeyPressed(final KeyEvent keyEvent) {

		final int virtualSize = _virtualGalleryItems.length;

		// handle no items
		if (virtualSize == 0) {
			return;
		}

		final boolean isResetPosition = onKeyPressed_10(keyEvent, virtualSize);

		// reset position when position is modified manually
		if (isResetPosition) {
			_forcedGalleryPosition = null;
			hideTooltip();
		}
	}

	private boolean onKeyPressed_10(final KeyEvent keyEvent, final int virtualSize) {

		boolean isCtrlKey;
		boolean isShiftKey;

		if (UI.IS_OSX) {
			isCtrlKey = (keyEvent.stateMask & SWT.MOD1) > 0;
			isShiftKey = (keyEvent.stateMask & SWT.MOD3) > 0;
		} else {
			isCtrlKey = (keyEvent.stateMask & SWT.MOD1) > 0;
			isShiftKey = (keyEvent.stateMask & SWT.MOD2) > 0;
		}

		boolean isMultiSelection = false;
		int keyCode = keyEvent.keyCode;

		// check if multiple selection starts
		if (keyCode == SWT.ARROW_LEFT
				|| keyCode == SWT.ARROW_RIGHT
				|| keyCode == SWT.ARROW_UP
				|| keyCode == SWT.ARROW_DOWN
				|| keyCode == SWT.PAGE_UP
				|| keyCode == SWT.PAGE_DOWN
				|| keyCode == SWT.HOME
				|| keyCode == SWT.END) {

			if (UI.IS_OSX) {
				if (isCtrlKey) {
					if (keyCode == SWT.ARROW_UP) {
						keyCode = SWT.HOME;
					} else if (keyCode == SWT.ARROW_DOWN) {
						keyCode = SWT.END;
					}
				}
			}

			if (isShiftKey && _prevKeyIsShift == false //
					// item is selected
					&& _lastSelectedItem != null) {

				// start multiple selection
				_lastSingleClick = _lastSelectedItemIndex;

			} else if (isShiftKey && _prevKeyIsShift) {

				// continue multiple selection

			} else {

				// stop multiple selection

				_lastSingleClick = -1;
			}

			_prevKeyIsShift = isShiftKey;

			isMultiSelection = isShiftKey && _lastSingleClick != -1;
		}

		final int maxVisibleRows = Math.max((_clientArea.height / _itemHeight) - 1, 1);
		final int maxVisibleItems = maxVisibleRows * _gridHorizItems;

		switch (keyEvent.character) {
		case '+':

			// zoom IN

			zoomGallery(keyEvent.time, true, isShiftKey, isCtrlKey);

			return true;

		case '-':

			// zoom OUT

			zoomGallery(keyEvent.time, false, isShiftKey, isCtrlKey);

			return true;

		case ' ':

			showFullsizeImage(_lastSelectedItemIndex);

			return false;
		}

		switch (keyCode) {
		case 'a':
		case 'A':

			// select ALL with <Ctrl><A>

			if (isCtrlKey) {
				selectAll();
				return true;
			}

			return false;

		case SWT.ARROW_LEFT:

			return selectItem_Previous(isMultiSelection);

		case SWT.ARROW_RIGHT:

			return selectItem_Next(isMultiSelection);

		case SWT.ARROW_UP:

			if (_lastSelectedItemIndex < _gridHorizItems) {
				// selection is already in the first row
				return false;
			}

			if (isMultiSelection) {
				selectItemMultiple(_lastSelectedItemIndex - _gridHorizItems);
			} else {
				selectItemSingle(_lastSelectedItemIndex - _gridHorizItems, true, true);
			}

			return true;

		case SWT.ARROW_DOWN:

			final int nextRowIndex = _lastSelectedItemIndex + _gridHorizItems;

			if (nextRowIndex >= virtualSize) {
				// selection is already in the last row
				return false;
			}

			if (isMultiSelection) {
				selectItemMultiple(nextRowIndex);
			} else {
				selectItemSingle(nextRowIndex, true, true);
			}

			return true;

		case SWT.PAGE_UP:

			if (_lastSelectedItemIndex < _gridHorizItems) {
				// selection is already in the first row
				return false;
			}

			final int itemIndexPageUp = _lastSelectedItemIndex - maxVisibleItems;

			selectItemSingle(itemIndexPageUp < 0 ? 0 : itemIndexPageUp, true, false);

			return true;

		case SWT.PAGE_DOWN:

			int itemIndexPageDown = _lastSelectedItemIndex + _gridHorizItems;

			if (itemIndexPageDown >= virtualSize) {
				// selection is already in the last row
				return false;
			}

			itemIndexPageDown = _lastSelectedItemIndex + maxVisibleItems;

			selectItemSingle(itemIndexPageDown >= virtualSize ? virtualSize - 1 : itemIndexPageDown, true, false);

			return true;

		case SWT.HOME:

			if (_lastSelectedItemIndex == 0) {
				// selection is already at the first item
				return false;
			}

			selectItemSingle(0, true, false);

			return true;

		case SWT.END:

			if (_lastSelectedItemIndex == virtualSize - 1) {
				// selection is already at the last item
				return false;
			}

			selectItemSingle(virtualSize - 1, true, false);

			return true;

		case SWT.CR:
			showFullsizeImage(_lastSelectedItemIndex);

			return false;
		}

		return false;
	}

	private void onMouseContextMenu(final MenuDetectEvent menuEvent) {

	}

	private void onMouseDoubleClick(final MouseEvent e) {

		final int itemIndex = getItemIndexFromPosition(e.x, e.y);

		final GalleryMT20Item item = getInitializedItem(itemIndex);

		if (item != null) {

			showFullsizeImage(itemIndex);

//			notifySelectionListeners(item, itemIndex, true);
		}

		_isMouseClickHandled = true;
	}

	private void onMouseDown(final MouseEvent mouseEvent) {

		if (_externalGalleryListener != null) {
			if (_externalGalleryListener.isMouseEventHandledExternally(SWT.MouseDown, mouseEvent)) {
				redrawGallery();
				return;
			}
		}

		if (isFocusControl() == false) {
			super.setFocus();
		}

		_isMouseClickHandled = false;

		final int itemIndex = getItemIndexFromPosition(mouseEvent.x, mouseEvent.y);

		if (mouseEvent.button == 1) {

			// left mouse button is pressed

			if (itemIndex < 0) {

				// mouse is clicked beside an item

				deselectAll(true);
				redrawGallery();

				_isMouseClickHandled = true;
				_lastSingleClick = -1;

			} else {

				// item is selected

				final boolean isCtrlKey = (mouseEvent.stateMask & SWT.MOD1) > 0;
				final boolean isShiftKey = (mouseEvent.stateMask & SWT.MOD2) > 0;
				final boolean isAltKey = (mouseEvent.stateMask & SWT.MOD3) > 0;

				if (isAltKey) {

					// pan gallery, keep position
					_isGalleryPanned = true;
					_mousePanStartPosition = new Point(mouseEvent.x, mouseEvent.y);

				} else if (isCtrlKey) {
					onMouseHandleCtrlLeft(mouseEvent, itemIndex, true, false);
				} else if (isShiftKey) {
					onMouseHandleShiftLeft(mouseEvent, itemIndex);
				} else {
					onMouseHandleLeft(mouseEvent, itemIndex, true, false);
				}
			}
		}
	}

	private void onMouseHandleCtrlLeft(	final MouseEvent e,
										final int itemIndex,
										final boolean isMouseDownEvent,
										final boolean isMouseUpEvent) {
		if (isMouseUpEvent) {
			if (itemIndex != -1) {
				selectItemSingle(itemIndex, false, false);
			}
		}
	}

	private void onMouseHandleLeft(	final MouseEvent e,
									final int itemIndex,
									final boolean isMouseDownEvent,
									final boolean isMouseUpEvent) {

		if (isMouseDownEvent) {
			if (!isItemSelected(itemIndex)) {

				deselectAll(false);
				selectItemAndNotify(itemIndex, true, true);
				_lastSingleClick = itemIndex;

				redrawGallery();

				_isMouseClickHandled = true;
			}

		} else if (isMouseUpEvent) {

			if (itemIndex == -1) {
				deselectAll(true);
			} else {

				deselectAll(false);
				selectItemAndNotify(itemIndex, true, _lastSingleClick != itemIndex);
				_lastSingleClick = itemIndex;
			}
			redrawGallery();
		}
	}

	private void onMouseHandleShiftLeft(final MouseEvent e, final int itemIndex) {

		if (_lastSingleClick == -1) {
			return;
		}

		deselectAll(false);

		/*
		 * select items between current item and last click item
		 */

		final int virtualLast = _lastSingleClick;
		final int virtualCurrent = itemIndex;

		int virtualIndexFrom;
		int virtualIndexTo;

		if (virtualLast < virtualCurrent) {
			virtualIndexFrom = virtualLast;
			virtualIndexTo = virtualCurrent;
		} else {
			virtualIndexFrom = virtualCurrent;
			virtualIndexTo = virtualLast;
		}

		for (int virtualIndex = virtualIndexFrom; virtualIndex <= virtualIndexTo; virtualIndex++) {
			selectionAdd(virtualIndex);
		}

		notifySelectionListeners(getInitializedItem(virtualLast), virtualLast, false);

		redrawGallery();
	}

	private void onMouseMove(final MouseEvent mouseEvent) {

		if (_externalGalleryListener != null) {
			if (_externalGalleryListener.isMouseEventHandledExternally(SWT.MouseMove, mouseEvent)) {
				redrawGallery();
				return;
			}
		}

		final int mouseX = mouseEvent.x;
		final int mouseY = mouseEvent.y;

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

		} else {

			fireItemHoverEvent(mouseX, mouseY);
		}
	}

	private void onMouseUp(final MouseEvent mouseEvent) {

		if (_externalGalleryListener != null) {
			if (_externalGalleryListener.isMouseEventHandledExternally(SWT.MouseUp, mouseEvent)) {
				redrawGallery();
				return;
			}
		}

		if (_isGalleryPanned) {

			// panning has ended

			_isGalleryPanned = false;
			return;
		}

		if (_isMouseClickHandled) {
			// mouse click is already handled in the mouse down event
			return;
		}

		/*
		 * handle mouse click event
		 */

		if (mouseEvent.button == 1) {

			final int itemIndex = getItemIndexFromPosition(mouseEvent.x, mouseEvent.y);

			if (itemIndex < 0) {
				return;
			}

			if ((mouseEvent.stateMask & SWT.MOD1) > 0) {

				onMouseHandleCtrlLeft(mouseEvent, itemIndex, false, true);

			} else if ((mouseEvent.stateMask & SWT.MOD2) > 0) {

				onMouseHandleShiftLeft(mouseEvent, itemIndex);

			} else {

				onMouseHandleLeft(mouseEvent, itemIndex, false, true);
			}
		}
	}

	private void onMouseWheel(final MouseEvent event) {

		_isZoomed = false;

		/**
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
		 * <br>
		 * This event is fired ONLY when the scrollbars are not visible, on Win 7<br>
		 * <br>
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 */

		boolean isCtrlKey;
		boolean isShiftKey;

		if (UI.IS_OSX) {
			isCtrlKey = (event.stateMask & SWT.MOD1) > 0;
			isShiftKey = (event.stateMask & SWT.MOD3) > 0;
		} else {
			isCtrlKey = (event.stateMask & SWT.MOD1) > 0;
			isShiftKey = (event.stateMask & SWT.MOD2) > 0;
		}

		/*
		 * ensure <ctrl> or <shift> is pressed, otherwise it is zoomed when the scrollbar is hidden
		 */
		if (isCtrlKey || isShiftKey) {
			zoomGallery(event.time, event.count > 0, isShiftKey, isCtrlKey);
			_isZoomed = true;
		}

		// reset position when position is modified manually
		_forcedGalleryPosition = null;
	}

	private void onPaint(final GC gc) {

		final long start = System.nanoTime();

		/**
		 * After many hours I discovered, that the gallery background is not painted (with win7) in
		 * the background color after the shell is hidden and displayed again (in a tooltip),
		 * sometime it is painted, sometimes it isn't.
		 */

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

		Rectangle clippingArea = null;

		try {

			if (_initialSelectedItems != null) {
				for (final int itemIndex : _initialSelectedItems) {
					selectionAdd(itemIndex);
				}
				_initialSelectedItems = null;
			}

			clippingArea = gc.getClipping();
			gc.fillRectangle(clippingArea);

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

					final int virtualLength = _virtualGalleryItems.length;

					// loop: all gallery items in the clipping area
					for (int areaIndex = numberOfAreaItems - 1; areaIndex >= 0; areaIndex--) {

						final int virtualIndex = areaItemsIndices[areaIndex];

						// ensure number of available items
						if (virtualIndex >= virtualLength) {
							continue;
						}

						final GalleryMT20Item galleryItem = getInitializedItem(virtualIndex);

						if (galleryItem == null) {
							continue;
						}

						final boolean isSelected = isItemSelected(virtualIndex);

						updateItemPosition(galleryItem, virtualIndex);

						final int viewPortX = galleryItem.viewPortX;
						final int viewPortY = galleryItem.viewPortY;

						gc.setClipping(viewPortX, viewPortY, _itemWidth, _itemHeight);

						_itemRenderer.draw(
								gc,
								galleryItem,
								viewPortX,
								viewPortY,
								_itemWidth,
								_itemHeight,
								isSelected,
								_isFocusActive);
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

		if (_externalGalleryListener != null) {
			_externalGalleryListener.onPaintAfter(gc, clippingArea, _clientArea);
		}

		final float timeDiff = (float) (System.nanoTime() - start) / 1000000;
//		if (timeDiff > 500) {
		System.out.println(UI.timeStampNano() + " \tonPaint:\t" + timeDiff + " ms\t");
//		}
		// TODO remove SYSTEM.OUT.PRINTLN
	}

	private void onResize() {

		_clientArea = getClientArea();

		if (_clientArea.width == 0 || _clientArea.height == 0) {

			// UI is not yet initialized

		} else {

			/*
			 * set item height for horizontal galleries because it contains only 1 row with all
			 * images, these galleries cannot be zoomed so this is the only point where the size is
			 * set
			 */
			if (_isHorizontal) {

				_itemHeight = _clientArea.height;
				_itemWidth = (int) (_itemHeight * _itemRatio);
			}
		}

		updateGallery(true);
	}

	private void onScrollHorizontal() {

		// reset position when position is modified manually
		_forcedGalleryPosition = null;

		final int areaWidth = _clientArea.width;

		if (_contentVirtualWidth > areaWidth) {

			// content is larger than visible client area

			final ScrollBar hBar = getHorizontalBar();

			final int barSelection = hBar.getSelection();
			final int destX = _galleryPosition - barSelection;

// this is not working :-((
//
//			if (_isMouseWheel) {
//				/*
//				 * mouse wheel in the client area and NOT in the scrollbar has caused this scolling
//				 * event, accelerate scrolling
//				 */
////				destX *= 10;
//
////				hBar.setSelection(_galleryPosition - destX);
//
//				_isMouseWheel = false;
//			}

			scroll(destX, 0, 0, 0, areaWidth, _clientArea.height, false);

			_galleryPosition = barSelection;

		} else {
			_galleryPosition = 0;
		}

		_clientAreaItemsIndices = getAreaItemsIndices(_clientArea);

		hideTooltip();
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
				isCtrl = (event.stateMask & SWT.COMMAND) != 0;
//				isShift = (event.stateMask & SWT.ALT) != 0;
				isShift = (event.stateMask & SWT.MOD2) != 0;
			} else {
				isCtrl = (event.stateMask & SWT.MOD1) != 0;
				isShift = (event.stateMask & SWT.MOD2) != 0;
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

		// reset position when position is modified manually
		_forcedGalleryPosition = null;

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

		hideTooltip();
	}

	private void redrawGallery() {

//		System.out.println(UI.timeStampNano() + " redrawGallery\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

		redraw();
	}

	public void restoreState(final IDialogSettings state) {

		_fullScreenImageViewer.restoreState(state);
	}

	public void saveState(final IDialogSettings state) {

		_fullScreenImageViewer.saveState(state);
	}

	private void selectAll() {

		final int itemSize = _virtualGalleryItems.length;

		if (itemSize == 0) {
			return;
		}

		for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
			selectionAdd(itemIndex);
		}

		notifySelectionListeners(getInitializedItem(0), 0, false);

		redrawGallery();
	}

	/**
	 * Adds a gallery item to the selected items
	 * 
	 * @param itemIndex
	 */
	private void selectionAdd(final int itemIndex) {

		if (itemIndex == -1) {
			return;
		}

		if (isItemSelected(itemIndex)) {
			return;
		}

		// Deselect all items if multi selection is disabled
		if (!_isMultiSelection) {
			deselectAll(false);
		}

		// Divide position by 32 to get selection bloc for this item.
		final int flagIndex = itemIndex >> 5;
		if (_selectionFlags == null) {
			// Create selectionFlag array
			// Add 31 before dividing by 32 to ensure at least one 'int' is
			// created if size < 32.
			_selectionFlags = new int[(_virtualGalleryItems.length + 31) >> 5];

		} else if (flagIndex >= _selectionFlags.length) {

			// Expand selectionArray
			final int[] oldFlags = _selectionFlags;
			_selectionFlags = new int[flagIndex + 1];
			System.arraycopy(oldFlags, 0, _selectionFlags, 0, oldFlags.length);
		}

		final GalleryMT20Item galleryItem = getInitializedItem(itemIndex);

		if (galleryItem == null) {
			// happened during development but should not happen
			return;
		}

		// Get flag position in the 32 bit block and ensure is selected.
		_selectionFlags[flagIndex] |= 1 << (itemIndex & 0x1f);

		_lastSelectedItem = galleryItem;
		_lastSelectedItemIndex = itemIndex;

		_selectedItems.put(galleryItem.uniqueItemID, galleryItem);
	}

	/**
	 * Removes a gallery item ftom the selected items.
	 * 
	 * @param itemIndex
	 */
	private void selectionRemove(final int itemIndex) {

		_selectionFlags[itemIndex >> 5] &= ~(1 << (itemIndex & 0x1f));

		_lastSelectedItem = null;

		final GalleryMT20Item item = getInitializedItem(itemIndex);

		_selectedItems.remove(item.uniqueItemID);
	}

	private boolean selectItem_Next(final boolean isMultiSelection) {

		final int virtualSize = _virtualGalleryItems.length;
		boolean isResetPosition = false;

		if (_lastSelectedItemIndex < virtualSize - 1) {
			if (isMultiSelection) {
				selectItemMultiple(_lastSelectedItemIndex + 1);
			} else {
				selectItemSingle(_lastSelectedItemIndex + 1, true, true);
			}
			isResetPosition = true;

		} else if (_lastSelectedItemIndex == virtualSize - 1) {

			// last item

			if (isMultiSelection) {
				selectItemMultiple(_lastSelectedItemIndex);
			} else {
				selectItemSingle(_lastSelectedItemIndex, true, true);
			}
			isResetPosition = true;
		}

		return isResetPosition;
	}

	private boolean selectItem_Previous(final boolean isMultiSelection) {

		boolean isResetPosition = false;

		if (_lastSelectedItemIndex > 0) {

			// 2nd ... nth item

			if (isMultiSelection) {
				selectItemMultiple(_lastSelectedItemIndex - 1);
			} else {
				selectItemSingle(_lastSelectedItemIndex - 1, true, true);
			}
			isResetPosition = true;

		} else if (_lastSelectedItemIndex == 0) {

			// 1st item

			if (isMultiSelection) {
				selectItemMultiple(_lastSelectedItemIndex);
			} else {
				selectItemSingle(_lastSelectedItemIndex, true, true);
			}
			isResetPosition = true;
		}

		return isResetPosition;
	}

	/**
	 * Toggle item selection status
	 * 
	 * @param itemIndex
	 *            Item which state is to be changed.
	 * @param isSelected
	 *            true is the item is now selected, false if it is now unselected.
	 * @param isNotifyListeners
	 *            If true, a selection event will be sent to all the current selection listeners.
	 */
	private void selectItemAndNotify(final int itemIndex, final boolean isSelected, final boolean isNotifyListeners) {

		final boolean isItemSelected = isItemSelected(itemIndex);

		if (isSelected) {

			// select item

			if (!isItemSelected) {

				// select only when not yet selected
				selectionAdd(itemIndex);
			}

		} else {

			// deselect item

			if (isItemSelected) {

				// deselect only when selected
				selectionRemove(itemIndex);
			}
		}

		// Notify listeners if necessary.
		if (isNotifyListeners) {

			final int index = -1;
			GalleryMT20Item notifiedItem = null;

			if (itemIndex != -1 && isSelected) {
				notifiedItem = getInitializedItem(itemIndex);
			} else {

// this code is using the last selected item, have not yet discovery why this item is used ????
//				if (_selectedItems.length > 0) {
//					notifiedItem = _selectedItems[_selectedItems.length - 1];
//				}
			}
//
//			if (notifiedItem != null) {
//				index = getItemIndex(notifiedItem);
//			}

			notifySelectionListeners(notifiedItem, index, false);
		}
	}

	private void selectItemMultiple(final int itemEndIndex) {

		/*
		 * select items between current item and last click item
		 */

		final int virtualLast = _lastSingleClick;
		final int virtualCurrent = itemEndIndex;

		int virtualIndexFrom;
		int virtualIndexTo;

		if (virtualLast < virtualCurrent) {
			virtualIndexFrom = virtualLast;
			virtualIndexTo = virtualCurrent;
		} else {
			virtualIndexFrom = virtualCurrent;
			virtualIndexTo = virtualLast;
		}

		for (int itemIndex = virtualIndexFrom; itemIndex <= virtualIndexTo; itemIndex++) {
			selectionAdd(itemIndex);
		}

		_lastSingleClick = itemEndIndex;

		_lastSelectedItemIndex = itemEndIndex;
		_lastSelectedItem = getInitializedItem(_lastSelectedItemIndex);

		showItem(itemEndIndex);

		notifySelectionListeners(_lastSelectedItem, itemEndIndex, false);
	}

	/**
	 * Toggle selection of the given gallery item and notify listener.
	 * 
	 * @param itemIndex
	 * @param isDeselectAll
	 */
	private void selectItemSingle(final int itemIndex, final boolean isDeselectAll, final boolean isForceSelection) {

		final boolean isItemSelected = isItemSelected(itemIndex);

		if (isDeselectAll) {
			deselectAll(false);
		}

		selectItemAndNotify(itemIndex, isForceSelection ? true : !isItemSelected, true);

		_lastSingleClick = itemIndex;

		showItem(itemIndex);
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

	public void setColors(final Color fgColor, final Color bgColor) {

		setForeground(fgColor);
		setBackground(bgColor);

		_fullScreenImageViewer.setColors(fgColor, bgColor);
	}

	public void setContextMenuProvider(final IGalleryContextMenuProvider customContextMenuProvider) {
		_customContextMenuProvider = customContextMenuProvider;
	}

	public void setExternalMouseListener(final IExternalGalleryListener externalGalleryMouseListener) {
		_externalGalleryListener = externalGalleryMouseListener;
	}

	@Override
	public boolean setFocus() {
		return true;
	}

	@Override
	public void setFont(final Font font) {

		_fullScreenImageViewer.setFont(font);

		super.setFont(font);
	}

	/**
	 * Sets number of vertical and horizontal items for the whole gallery in
	 * {@link #_gridHorizItems} and {@link #_gridVertItems}
	 */
	private void setGridSize() {

		if (_isVertical) {

			final Point vhNumbers = setGridSize_10(_clientArea.width, _itemWidth);

			_gridHorizItems = vhNumbers.x;
			_gridVertItems = vhNumbers.y;

		} else {

			final Point vhNumbers = setGridSize_10(_clientArea.height, _itemHeight);

			_gridHorizItems = vhNumbers.y;
			_gridVertItems = vhNumbers.x;
		}
	}

	/**
	 * Calculate how many items are displayed horizontally or vertically.
	 * 
	 * @param visibleSize
	 * @param itemSize
	 * @return
	 */
	private Point setGridSize_10(final int visibleSize, final int itemSize) {

		int numberOfVirtualItems;
		if (_virtualGalleryItems == null || _virtualGalleryItems.length == 0) {

			// virtual items are not yet set, get number of images in 1 row

			final int oneRowItems = visibleSize / itemSize;

			numberOfVirtualItems = oneRowItems;

		} else {

			numberOfVirtualItems = _virtualGalleryItems.length;
		}

		int x = visibleSize / itemSize;
		int y = 0;

		if (x > 0) {
			y = (int) Math.ceil((double) numberOfVirtualItems / (double) x);
		} else {
			// Show at least one item;
			y = numberOfVirtualItems;
			x = 1;
		}

		return new Point(x, y);
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

		redrawGallery();
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

	public void setIsShowOtherShellActions(final boolean isShowOtherShellActions) {
		_isShowOtherShellActions = isShowOtherShellActions;
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

		_fullScreenImageViewer.setItemRenderer(itemRenderer);

		redrawGallery();
	}

	public void setPhotoProvider(final IPhotoProvider photoProvider) {
		_photoProvider = photoProvider;
	}

//	public void setSelection(final Collection<GalleryMT20Item> selection) {
//
//		// IS NOT YET IMPLEMENTED
//
//	}

	/**
	 * Set number of items in the gallery, the items itself are created when they are displayed.
	 * <p>
	 * This will also start a gallery update which runs the
	 * {@link #initializeGalleryItem(GalleryMT20Item, int)} method to initialize gallery items.
	 * 
	 * @param numberOfItems
	 * @param galleryPosition
	 *            Gallery position where the gallery should be used to display gallery items, or
	 *            <code>null</code> when position is not set.
	 * @param selectedItems
	 *            Items which should be selected or <code>null</code> when nothing should be
	 *            selected.
	 */
	public void setupItems(final int numberOfItems, final Double galleryPosition, final int[] selectedItems) {

		_initialSelectedItems = selectedItems;

		// create empty (null) gallery items
		_createdGalleryItems.clear();

		deselectAll(false);

		// initially all items can be displayed
		_virtualGalleryItems = new GalleryMT20Item[numberOfItems];

		updateGallery(false, galleryPosition);
	}

	public void setVertical(final boolean isVerticalGallery) {

		_isVertical = isVerticalGallery;
		_isHorizontal = !isVerticalGallery;

		if (isVerticalGallery) {

			// gallery is vertical

			final ScrollBar hBar = getHorizontalBar();
			if (hBar != null) {
				hBar.setVisible(false);
			}

			// reset to the last vertical width

			_itemWidth = _verticalItemWidth;
			_itemHeight = (int) (_itemWidth / _itemRatio);

		} else {

			// gallery is horizontal

			final ScrollBar vBar = getVerticalBar();
			if (vBar != null) {
				vBar.setVisible(false);
			}
		}

		// this will also set horizontal image height
		onResize();
	}

	/**
	 * Set gallery items, which can be retrieved with {@link #getAllVirtualItems()}. With this
	 * get/set mechanism, the gallery items can be sorted.
	 * 
	 * @param virtualGalleryItems
	 */
	public void setVirtualItems(GalleryMT20Item[] virtualGalleryItems, final Double galleryPosition) {

		// prevent NPE
		if (virtualGalleryItems == null) {
			virtualGalleryItems = new GalleryMT20Item[0];
		}

		_virtualGalleryItems = virtualGalleryItems;

		updateGallery(true, galleryPosition);
	}

	/**
	 * Show full size image
	 * 
	 * @param itemIndex
	 */
	private void showFullsizeImage(final int itemIndex) {

		final GalleryMT20Item initializedItem = getInitializedItem(itemIndex);

		if (initializedItem != null) {
			_fullScreenImageViewer.showImage(initializedItem, itemIndex, true);
		}
	}

	/**
	 * Show item in the client area.
	 * 
	 * @param itemIndex
	 */
	public void showItem(final int itemIndex) {

		if (itemIndex < 0 || itemIndex >= _virtualGalleryItems.length) {
			// index is out of scope
			return;
		}

		if (_isVertical) {

			final int visibleHeight = _clientArea.height;

			final int row = itemIndex / _gridHorizItems;
			final int itemPosY = row * _itemHeight;

			if (_gridHorizItems == 1) {

				// center image vertically

				if (_itemHeight > visibleHeight) {

					final int offset = (_itemHeight - visibleHeight) / 2;

					_galleryPosition = itemPosY + offset;

				} else {

					final int offset = (visibleHeight - _itemHeight) / 2;

					_galleryPosition = itemPosY - offset;
				}

			} else if (itemPosY < _galleryPosition) {

				_galleryPosition = itemPosY;

			} else if (_galleryPosition + visibleHeight < itemPosY + _itemHeight) {

				_galleryPosition = itemPosY + _itemHeight - visibleHeight;
			}

		} else {

			// horizontal gallery

			final int visibleWidth = _clientArea.width;

			final int column = itemIndex / _gridVertItems;
			final int virtualItemX = column * _itemWidth;

			if (virtualItemX < _galleryPosition) {

				_galleryPosition = virtualItemX - (visibleWidth / 2 - _itemWidth / 2);

			} else if (_galleryPosition + visibleWidth < virtualItemX + _itemWidth) {

				_galleryPosition = virtualItemX - (visibleWidth / 2 - _itemWidth / 2);
			}
		}

		_clientAreaItemsIndices = getAreaItemsIndices(_clientArea);

		updateScrollBars();

		redrawGallery();
	}

	/**
	 * @param isKeepLocation
	 *            Keeps gallery position when <code>true</code>, otherwise position will be reset.
	 */
	public void updateGallery(final boolean isKeepLocation) {
		updateGallery(isKeepLocation, null);
	}

	/**
	 * @param isKeepLocation
	 *            Keeps gallery position when <code>true</code>, otherwise position will be reset.
	 * @param forcedGalleryPosition
	 *            Ratio for the gallery position when not <code>null</code>
	 */
	public void updateGallery(final boolean isKeepLocation, final Double forcedGalleryPosition) {

		// !!! prevent setting it to NULL, especially when the UI is not initialized !!!
		if (forcedGalleryPosition != null) {
			_forcedGalleryPosition = forcedGalleryPosition;
		}

		if (_clientArea.width == 0 || _clientArea.height == 0) {

			// UI is not yet initialized

		} else {

			// compute new content width/height
			updateStructuralValues(isKeepLocation);

			// ensure scrollbars are correctly displayed/hidden
			updateScrollBars();

			_clientAreaItemsIndices = getAreaItemsIndices(_clientArea);
		}

		// start a paint event
		redrawGallery();
	}

	/**
	 * Set the items gallery position.
	 * <p>
	 * The item position can change when {@link #setVirtualItems(GalleryMT20Item[])} is called and
	 * the item is not yet displayed.
	 * 
	 * @param galleryItem
	 * @param virtualIndex
	 */
	private void updateItemPosition(final GalleryMT20Item galleryItem, final int virtualIndex) {

		int viewPortX;
		int viewPortY;

		int numberOfItemsX;
		int numberOfItemsY;

		if (_isVertical) {
			numberOfItemsX = virtualIndex % _gridHorizItems;
			numberOfItemsY = (virtualIndex - numberOfItemsX) / _gridHorizItems;
		} else {
			numberOfItemsY = virtualIndex % _gridVertItems;
			numberOfItemsX = (virtualIndex - numberOfItemsY) / _gridVertItems;
		}

		final int galleryVirtualPosX = numberOfItemsX * _itemWidth;
		final int galleryVirtualPosY = numberOfItemsY * _itemHeight;

		if (_isVertical) {

			final int allItemsWidth = _gridHorizItems * _itemWidth;

			if (_contentVirtualWidth > allItemsWidth) {
				_itemCenterOffsetX = (_contentVirtualWidth - allItemsWidth) / 2;
			} else if (allItemsWidth > _contentVirtualWidth) {
				_itemCenterOffsetX = -(allItemsWidth - _contentVirtualWidth) / 2;
			} else {
				_itemCenterOffsetX = 0;
			}

			viewPortX = galleryVirtualPosX + _itemCenterOffsetX;
			viewPortY = galleryVirtualPosY - _galleryPosition;

		} else {

			// horizontal gallery

			viewPortX = galleryVirtualPosX - _galleryPosition;
			viewPortY = galleryVirtualPosY;
		}

		galleryItem.viewPortX = viewPortX;
		galleryItem.viewPortY = viewPortY;
		galleryItem.height = _itemHeight;
		galleryItem.width = _itemWidth;

		galleryItem.imagePaintedWidth = -1;
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

		setGridSize();

		final int clientAreaWidth = _clientArea.width;
		final int clientAreaHeight = _clientArea.height;

		double oldPosition = -1;

		if (_isVertical) {

			// vertical

			if (isKeepLocation && _contentVirtualHeight > 0) {
				oldPosition = (_galleryPosition + 0.5 * clientAreaHeight) / _contentVirtualHeight;
			}

			_contentVirtualWidth = clientAreaWidth;
			_contentVirtualHeight = _gridVertItems * _itemHeight;

			if (_forcedGalleryPosition != null) {

				_galleryPosition = (int) (_contentVirtualHeight * _forcedGalleryPosition);

			} else {

				if (oldPosition != -1) {
					_galleryPosition = (int) (_contentVirtualHeight * oldPosition - 0.5 * clientAreaHeight);
				}
			}

		} else {

			// horizontal

			if (isKeepLocation && _contentVirtualWidth > 0) {
				oldPosition = (_galleryPosition + 0.5 * clientAreaWidth) / _contentVirtualWidth;
			}

			_contentVirtualWidth = _gridHorizItems * _itemWidth;
			_contentVirtualHeight = clientAreaHeight;

			if (_forcedGalleryPosition != null) {

				_galleryPosition = (int) (_contentVirtualWidth * _forcedGalleryPosition);

			} else {

				if (oldPosition != -1) {
					_galleryPosition = (int) (_contentVirtualWidth * oldPosition - 0.5 * clientAreaWidth);
				}
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
	 * @param newZoomedSize
	 * @param isInitializeGallery
	 *            When <code>true</code> the gallery is not yet initialized, grid and item width is
	 *            not yet set.
	 * @return Returns new item size or <code>-1</code> when gallery is not zoomed.
	 */
	public int zoomGallery(int newZoomedSize, final boolean isInitializeGallery) {

		if (_isHorizontal) {

			if (isInitializeGallery) {
				_verticalItemWidth = newZoomedSize;
			}

			return -1;
		}

		if (newZoomedSize < GALLERY_ITEM_MIN_SIZE) {
			newZoomedSize = GALLERY_ITEM_MIN_SIZE;
		}

		// ensure client area is set
		int clientAreaWidth = _clientArea.width;
		if (clientAreaWidth == 0) {

			_clientArea = getClientArea();

			clientAreaWidth = _clientArea.width;
		}

		int prevNumberOfImages;
		boolean isCheckWidth = false;

		if (isInitializeGallery) {

			prevNumberOfImages = clientAreaWidth / newZoomedSize;

			_itemWidth = newZoomedSize;

			isCheckWidth = true;

		} else {

			prevNumberOfImages = _gridHorizItems;

			// get default values when not yet set
			if (prevNumberOfImages < 1) {

				/*
				 * set default values for item width and number of images
				 */

				_itemWidth = newZoomedSize - 1;

				isCheckWidth = true;
			}
		}

		if (isCheckWidth) {

			/*
			 * ensure item width is not too small
			 */

			if (_itemWidth < 5) {
				_itemWidth = 5;
			}

			prevNumberOfImages = clientAreaWidth / _itemWidth;

			if (prevNumberOfImages < 1) {
				prevNumberOfImages = 5;
				_itemWidth = clientAreaWidth / prevNumberOfImages;
			}
		}

		// set state to use image width instead of number of images
		int stateNumberOfImages = -1;

		final boolean isZoomIn = newZoomedSize > _itemWidth;

		if (isInitializeGallery == false) {

			if (isZoomIn) {

				// zoom IN

				if (prevNumberOfImages <= 2) {

					// number of photos is already 1, only increase photo width

				} else {

					// less images in a row

					stateNumberOfImages = prevNumberOfImages - 1;
				}

			} else {

				// zoom OUT

				// more images in a row

				if (prevNumberOfImages == 1) {

					// number of photos is already 1, only increase photo width

				} else {

					// less images in a row

					stateNumberOfImages = prevNumberOfImages + 1;
				}
			}
		}

		/*
		 * update UI with new size
		 */
		hideTooltip();

		// update gallery
		final int newItemSize = zoomGallerySetItemSize(
				stateNumberOfImages,
				newZoomedSize,
				isZoomIn,
				isInitializeGallery);

		notifyZoomListener(_itemWidth, _itemHeight);

		return newItemSize;
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

		if (_isHorizontal) {
			return;
		}

		if (_mouseMovePosition == null) {
			return;
		}

		// check if this the same event which can be send multiple times
		if (_lastZoomEventTime == eventTime) {
			return;
		}
		_lastZoomEventTime = eventTime;

		if (_minItemWidth == -1 || _maxItemWidth == -1) {
			// min or max height is not set
			return;
		}

		final int newZoomedSize = getZoomedSize(isZoomIn, isShiftKey, isCtrlKey);

		if (newZoomedSize == _itemWidth) {
			// nothing has changed
			return;
		}

		zoomGallery(newZoomedSize, false);
	}

	/**
	 * Sets the size (width) of the gallery item, this contains the image width and the border.
	 * 
	 * @param requestedNumberOfImages
	 *            Number of requested horizontal/vertical images, or <code>-1</code> to use the
	 *            requested item size
	 * @param requestedItemSize
	 * @param isZoomIn
	 * @param isInitializeGallery
	 * @return Returns the size which has been set. This value can differ from the requested item
	 *         size when a scrollbar needs to be displayed.
	 */
	private int zoomGallerySetItemSize(	final int requestedNumberOfImages,
										final int requestedItemSize,
										final boolean isZoomIn,
										final boolean isInitializeGallery) {

		final boolean isForceNumberOfImages = requestedNumberOfImages != -1;

		int numberOfImages = requestedNumberOfImages;
		final int oldItemWidth = _itemWidth;

		if (isForceNumberOfImages) {

			int newItemWidth = _clientArea.width / requestedNumberOfImages;

			if (isInitializeGallery == false) {

				// initialize is not zooming !!!

				// ensure width is not the same
				if (newItemWidth == _itemWidth) {

					// size has not changed, this occures by small images

					newItemWidth = isZoomIn ? _itemWidth + 1 : _itemWidth - 1;
					numberOfImages = _clientArea.width / newItemWidth;
				}
			}

			_itemWidth = newItemWidth;
			_itemHeight = (int) (_itemWidth / _itemRatio);

		} else {

			// only 1 horizontal image is displayed

			_itemWidth = requestedItemSize;
			_itemHeight = (int) (_itemWidth / _itemRatio);

		}

		// ensure min size otherwise it can cause devide by zero
		if (_itemWidth < 10) {
			_itemWidth = 10;
			_itemHeight = (int) (_itemWidth / _itemRatio);
		}

		setGridSize();

		/*
		 * adjust to scrollbar visibility
		 */
		if (_isVertical) {

			if (numberOfImages > 1) {

				final int contentVirtualHeight = _gridVertItems * _itemHeight;

				// check scrollbar
				if (contentVirtualHeight > _clientArea.height) {

					// content is larger than visible area -> vertical scrollbar must be displayed

					final ScrollBar bar = getVerticalBar();

					if (bar != null) {

						if (bar.isVisible()) {

							// virtual height is already correctly computed -> nothing more to do

						} else {

							/*
							 * vertical bar is not yet displayed but the content is larger than the
							 * visible area, vertical bar needs to be displayed and the content
							 * width and hight must be adjusted
							 */

							final int barWidth = bar.getSize().x;

							// visible area width with scrollbar
							final int areaWidthWithScrollbar = _clientArea.width - barWidth;

							/*
							 * super hack, otherwise following computations are using client area
							 * without scrollbar until a resize is done, it took me many hours to
							 * solve zooming problems
							 */
//							_clientArea.width = areaWidthWithScrollbar;

							int newItemWidthWithScrollbar = _itemWidth;

							if (isForceNumberOfImages) {

								newItemWidthWithScrollbar = areaWidthWithScrollbar / requestedNumberOfImages;

								// ensure item width has changed otherwise image is not zoomed

								while (newItemWidthWithScrollbar == oldItemWidth) {

									// ensure that this is not an enless loop
									if (numberOfImages < 2 || numberOfImages > 1000) {
										break;
									}

									numberOfImages += isZoomIn ? -1 : 1;

									newItemWidthWithScrollbar = areaWidthWithScrollbar / numberOfImages;
								}
							}

							_itemWidth = newItemWidthWithScrollbar;
							_itemHeight = (int) (_itemWidth / _itemRatio);

							// this will fire a resize event
//							bar.setVisible(true);
						}
					}
				}
			}

		} else {

			// is not yet implemented
		}

		if (_isVertical) {
			_verticalItemWidth = _itemWidth;
		}

		updateGallery(true, centerSelectedItem());

		return _itemWidth;
	}
}
