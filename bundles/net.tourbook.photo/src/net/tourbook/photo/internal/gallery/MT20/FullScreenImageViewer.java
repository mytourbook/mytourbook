/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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

import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.Util;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.PhotoSelection;
import net.tourbook.photo.internal.Activator;
import net.tourbook.photo.internal.Messages;
import net.tourbook.photo.internal.preferences.PrefPagePhotoFullsizeViewer;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

public class FullScreenImageViewer {

	private static final int				TIME_BEFORE_CURSOR_GETS_HIDDEN			= 500;
	private static final int				TIME_BEFORE_SHOW_IMAGE					= UI.IS_OSX ? 100 : 500;
	private static final int				TIME_BEFORE_WAITING_CURSOR_IS_DISPLAYED	= 500;

	private static final String				STATE_FULL_SIZE_VIEWER_ZOOM_STATE		= "STATE_FULL_SIZE_VIEWER_ZOOM_STATE";	//$NON-NLS-1$

//	private static double					MIN_ZOOM								= 1.0 / 50;
//	private static double					MAX_ZOOM								= 50;

	private final IPreferenceStore			_prefStore								= Activator
																							.getDefault()
																							.getPreferenceStore();

	private GalleryMT20						_sourceGallery;
	private AbstractGalleryMT20ItemRenderer	_itemRenderer;
	private GalleryMT20Item					_displayedGalleryItem;
	private int								_displayedItemIndex;

	private FullScreenPhotoGallery			_fullScreenGallery;
	private PhotoSelection					_delayedPhotoSelection;

	private int								_monitorWidth;
	private int								_monitorHeight;

	/**
	 * State how images are zoomed when displayed.
	 */
	private ZoomState						_zoomState;

	private double							_zoomFactor;

	private TimerWaitCursor					_timerWaitingCursor						= new TimerWaitCursor();
	private TimerShowImageDelayed			_timerShowImageDelayed					= new TimerShowImageDelayed();
	private TimerSleepCursor				_timerSleepCursor						= new TimerSleepCursor();

	private boolean							_isShowWaitCursor;
	private boolean							_isShowLoadingMessage;

	private ActionOpenPrefDialog			_actionOpenFullsizePrefPage;
	private ActionShowThumbPreview			_actionShowThumbPreview;
	private ActionShowLoadingMessage		_actionShowLoadingMessage;
	private ActionShowHQImage				_actionShowHQImage;

	/*
	 * UI resources
	 */
	private Color							_fgColor;
//	private Color							_bgColor;

	/*
	 * UI controls
	 */
	private Image[]							_shellImage;
	private Shell							_shell;
	private Canvas							_canvas;

	private Cursor							_cursorWait;
	private Cursor							_cursorHidden;

	private Font							_font;
	private IDialogSettings					_state;

	private class TimerShowImageDelayed implements Runnable {
		public void run() {

			if (_shell == null || _shell.isDisposed()) {
				return;
			}

			showImage_Delayed();
		}
	}

	private class TimerSleepCursor implements Runnable {
		public void run() {

			if (_shell == null || _shell.isDisposed()) {
				return;
			}

			_canvas.setCursor(_cursorHidden);
		}
	}

	private class TimerWaitCursor implements Runnable {
		public void run() {

			if (_shell == null || _shell.isDisposed()) {
				return;
			}

			// check if the cursor still needs to display waiting state
			if (_isShowWaitCursor) {
				_canvas.setCursor(_cursorWait);
			}
		}
	}

//	public FullScreenImageViewer() {}

	public FullScreenImageViewer(	final GalleryMT20 gallery,
									final AbstractGalleryMT20ItemRenderer itemRenderer,
									final IDialogSettings state) {

		_state = state;

		_sourceGallery = gallery;

		_itemRenderer = itemRenderer;

		createActions();
	}

	void actionUpdatePrefStore() {

		// get state from actions
		final boolean isShowPreview = _actionShowThumbPreview.isChecked();
		final boolean isShowLoadingMessage = _actionShowLoadingMessage.isChecked();
		final boolean isShowHQImage = _actionShowHQImage.isChecked();

		// set state into pref store
		_prefStore.setValue(//
				IPhotoPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_PREVIEW,
				isShowPreview);

		_prefStore.setValue(//
				IPhotoPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_LOADING_MESSAGE,
				isShowLoadingMessage);

		_prefStore.setValue(//
				IPhotoPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_HQ_IMAGE,
				isShowHQImage);

		setPrefSettings(isShowPreview, isShowLoadingMessage, isShowHQImage);

		updateUI();
	}

	void activate() {
		_shell.setActive();
	}

	private void addShellListener() {

		_shell.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {}

			@Override
			public void focusLost(final FocusEvent e) {}
		});

		_shell.addShellListener(new ShellListener() {

			@Override
			public void shellActivated(final ShellEvent e) {}

			@Override
			public void shellClosed(final ShellEvent e) {}

			@Override
			public void shellDeactivated(final ShellEvent e) {}

			@Override
			public void shellDeiconified(final ShellEvent e) {}

			@Override
			public void shellIconified(final ShellEvent e) {}
		});
	}

	void close() {

		// prevent that an old image is displayed
		_itemRenderer.resetPreviousImage();

		if (_shell != null) {

			_shell.dispose();
			_shell = null;

			_sourceGallery.onCloseFullsizeViewer();
			_displayedGalleryItem = null;
		}
	}

	private void createActions() {

		_actionOpenFullsizePrefPage = new ActionOpenPrefDialog(
				Messages.Action_Photo_OpenPrefPage_FullsizeViewer,
				PrefPagePhotoFullsizeViewer.ID);

		_actionShowThumbPreview = new ActionShowThumbPreview(this);
		_actionShowLoadingMessage = new ActionShowLoadingMessage(this);
		_actionShowHQImage = new ActionShowHQImage(this);
	}

	private Menu createContextMenu(final Composite parent) {

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr) {
				fillContextMenu(menuMgr);
			}
		});

		return menuMgr.createContextMenu(parent);
	}

	private void createUI() {

//		_shell = new Shell(SWT.NO_TRIM | SWT.ON_TOP);
		_shell = new Shell(SWT.NO_TRIM);

		_shell.setForeground(_fgColor);
//		_shell.setBackground(_bgColor);
		_shell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));

		addShellListener();

		setBounds();

		_shell.setLayout(new FillLayout());

		_shellImage = new Image[] { Activator
				.getImageDescriptor(Messages.Image__PhotoFullsizeShellImage128)
				.createImage() };
		_shell.setImages(_shellImage);
		{
			createUI_10_Canvas(_shell);
			createUI_20_FullScreenGallery(_shell);
		}

		_cursorHidden = UI.createHiddenCursor();
		_cursorWait = new Cursor(_shell.getDisplay(), SWT.CURSOR_WAIT);

		_shell.setFullScreen(true);
		_shell.setVisible(true);
		_shell.setActive();

		_shell.open();
	}

	private void createUI_10_Canvas(final Shell parent) {

		/*
		 * SWT.NO_BACKGROUND can cause that other content which is drawn in another window can be
		 * displayed, e.g the gallery content was painted at the top of the canvas
		 */
//		_canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
		_canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED);

		_canvas.setForeground(_fgColor);
//		_canvas.setBackground(_bgColor);
		_canvas.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));

		_canvas.setFont(_font);

		_canvas.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				onKeyPressed(e);
			}
		});

		_canvas.addMouseListener(new MouseListener() {

			@Override
			public void mouseDoubleClick(final MouseEvent e) {}

			@Override
			public void mouseDown(final MouseEvent mouseEvent) {
				onMouseDown(mouseEvent);
			}

			@Override
			public void mouseUp(final MouseEvent e) {}
		});

		_canvas.addMouseMoveListener(new MouseMoveListener() {

			@Override
			public void mouseMove(final MouseEvent mouseEvent) {
				onMouseMove(mouseEvent);
			}
		});

		_canvas.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent e) {
				onMouseWheel(e);
			}
		});

		_canvas.addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent e) {
				onPaint(e.gc);
			}
		});

		_canvas.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		_canvas.setMenu(createContextMenu(_canvas));
	}

	private void createUI_20_FullScreenGallery(final Shell shell) {

		_fullScreenGallery = new FullScreenPhotoGallery(shell, _sourceGallery, this, _state);

		_fullScreenGallery.restoreState();
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionShowHQImage);
		menuMgr.add(_actionShowLoadingMessage);
		menuMgr.add(_actionShowThumbPreview);

		menuMgr.add(new Separator());
		menuMgr.add(_actionOpenFullsizePrefPage);
	}

	public GalleryMT20Item getCurrentItem() {
		return _displayedGalleryItem;
	}

	private boolean isViewerInitialized() {

		if (_zoomFactor == 0.0) {
			_zoomState = ZoomState.FIT_WINDOW;
		}

		if (_monitorWidth == 0) {
			// ui is not initialized
			return false;
		}

		if (_displayedGalleryItem == null) {
			// can happen after close event (it did)
			return false;
		}

		return true;
	}

	private void onDispose() {

		_cursorHidden.dispose();
		_cursorWait.dispose();

		for (final Image image : _shellImage) {
			image.dispose();
		}
	}

	private void onKeyPressed(final KeyEvent keyEvent) {

		final int keyCode = keyEvent.keyCode;
//		final boolean isShift = (keyEvent.stateMask & SWT.MOD2) != 0;

		switch (keyCode) {
		case SWT.ESC:
			close();
			break;

		case SWT.ARROW_LEFT:
		case SWT.ARROW_UP:

			_sourceGallery.navigateItem(-1);

			break;

		case SWT.ARROW_DOWN:
		case SWT.ARROW_RIGHT:

			_sourceGallery.navigateItem(1);

			break;

		case SWT.HOME:

			_sourceGallery.navigateItem(Integer.MIN_VALUE);
			break;

		case SWT.END:

			_sourceGallery.navigateItem(Integer.MAX_VALUE);
			break;
		}

		final char keyCharacterShowPhotoGallery = Messages.FullScreenImageViewer_KeyCharacter_ShowPhotoGallery
				.charAt(0);

		if (keyEvent.character == keyCharacterShowPhotoGallery) {

			showPhotoGallery(0);
		}

		switch (keyEvent.character) {
//		case '1':
//
//			// toggle full size and fit window
//
//			_zoomState = _zoomState == ZoomState.FIT_WINDOW ? ZoomState.ZOOMING : ZoomState.FIT_WINDOW;
//			_zoomFactor = 1.0;
//
//			updateUI();
//
//			break;
//
//		case '+':
//
//			// zoom IN
//
//			zoomIn(isShift);
//			break;
//
//		case '-':
//
//			// zoom OUT
//
//			zoomOut(isShift);
//			break;

		}

	}

	private void onMouseDown(final MouseEvent mouseEvent) {

	}

	private void onMouseMove(final MouseEvent mouseEvent) {

		final int mouseY = mouseEvent.y;

		if (showPhotoGallery(mouseY)) {

			// photo gallery is displayed

		} else {

			_canvas.setCursor(null);

			sleepCursor();
		}
	}

	private void onMouseWheel(final MouseEvent mouseEvent) {

		final boolean isDown = mouseEvent.count > 0;

		final boolean isCtrl = (mouseEvent.stateMask & SWT.MOD1) != 0;
//		final boolean isShift = (mouseEvent.stateMask & SWT.MOD2) != 0;

		if (isCtrl) {

			// zoom image

//			if (isDown) {
//				zoomIn(isShift);
//			} else {
//				zoomOut(isShift);
//			}

		} else {

			// select next/previous image

			_sourceGallery.navigateItem(isDown ? -1 : 1);
		}
	}

	private void onPaint(final GC gc) {

		if (isViewerInitialized() == false) {
			return;
		}

		final PaintingResult paintingResult = _itemRenderer.drawFullSize(
				gc,
				_displayedGalleryItem,
				_monitorWidth,
				_monitorHeight,
				_zoomState,
				_zoomFactor);

		if (paintingResult == null) {
			return;
		}

		if (paintingResult.isPainted == false) {
			// image is disposed, try to load and paint again
			updateUI();
		} else {

			// get zoomfactor when image is painted to fill window
			_zoomFactor = paintingResult.imagePaintedZoomFactor;

			// hide cursor when image is finally painted or when loading error
			if (paintingResult.isOriginalImagePainted || paintingResult.isLoadingError) {

				_isShowWaitCursor = false;
				_canvas.setCursor(_cursorHidden);

			} else if (_isShowLoadingMessage == false) {

				// do not show waiting cursor when loading message is displayed

				showWaitingCursor();
			}
		}
	}

	void restoreState() {

		final ZoomState defaultZoom = ZoomState.FIT_WINDOW;
		final String stateValue = Util.getStateString(_state, STATE_FULL_SIZE_VIEWER_ZOOM_STATE, defaultZoom.name());
		try {
			_zoomState = ZoomState.valueOf(stateValue);
		} catch (final Exception e) {
			_zoomState = defaultZoom;
		}

		_actionShowThumbPreview.setChecked(_prefStore.getBoolean(//
				IPhotoPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_PREVIEW));

		_actionShowHQImage.setChecked(_prefStore.getBoolean(//
				IPhotoPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_HQ_IMAGE));

		_actionShowLoadingMessage.setChecked(_prefStore.getBoolean(//
				IPhotoPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_LOADING_MESSAGE));
	}

	void saveState() {

		_state.put(STATE_FULL_SIZE_VIEWER_ZOOM_STATE, _zoomState.name());
	}

	private void setBounds() {

		final Rectangle monitorBounds = Display.getDefault().getPrimaryMonitor().getBounds();

		final double partialFullsize = 1;
//		final double partialFullsize = 0.9;
//		final double partialFullsize = 0.2;

		_monitorWidth = (monitorBounds.width);
//		_monitorWidth = (int) (monitorBounds.width * partialFullsize);
		_monitorHeight = (int) (monitorBounds.height * partialFullsize);

		_shell.setBounds(monitorBounds);
//		_shell.setBounds(0, 0, _monitorWidth, _monitorHeight);
//		_shell.setBounds(500, 100, _monitorWidth, _monitorHeight);
	}

	void setColors(final Color fgColor, final Color bgColor) {
		_fgColor = fgColor;
//		_bgColor = bgColor;
	}

	void setFont(final Font font) {

		_font = font;

		if (_canvas != null && _canvas.isDisposed() == false) {
			_canvas.setFont(font);
		}
	}

	void setItemRenderer(final AbstractGalleryMT20ItemRenderer itemRenderer) {
		_itemRenderer = itemRenderer;
	}

	public void setPrefSettings(final boolean isUpdateUI) {

		final boolean isShowPreview = _prefStore.getBoolean(IPhotoPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_PREVIEW);

		final boolean isShowLoadingMessage = _prefStore.getBoolean(//
				IPhotoPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_LOADING_MESSAGE);

		final boolean isShowHQImage = _prefStore.getBoolean(//
				IPhotoPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_HQ_IMAGE);

		_actionShowThumbPreview.setChecked(isShowPreview);
		_actionShowHQImage.setChecked(isShowHQImage);
		_actionShowLoadingMessage.setChecked(isShowLoadingMessage);

		setPrefSettings(isShowPreview, isShowLoadingMessage, isShowHQImage);

		if (isUpdateUI) {
			updateUI();
		}
	}

	private void setPrefSettings(	final boolean isShowPreview,
									final boolean isShowLoadingMessage,
									final boolean isShowHQImage) {

		_isShowLoadingMessage = isShowLoadingMessage;

		_itemRenderer.setPrefSettings(isShowPreview, isShowLoadingMessage, isShowHQImage);
	}

	void showImage(final GalleryMT20Item galleryItem, final int itemIndex, final boolean isActivateShell) {

		_displayedGalleryItem = galleryItem;
		_displayedItemIndex = itemIndex;

		if (updateUI() == false) {
			return;
		}

		if (isActivateShell) {
			_shell.setActive();
		}
	}

	void showImage(final PhotoSelection photoSelection) {

		_delayedPhotoSelection = photoSelection;

		/*
		 * delay fullsize image opening because it is very CPU intensive on win7+linux, however not
		 * on osx
		 */
		_shell.getDisplay().timerExec(TIME_BEFORE_SHOW_IMAGE, _timerShowImageDelayed);
	}

	private void showImage_Delayed() {

		// get first item
		final Collection<GalleryMT20Item> allItems = _delayedPhotoSelection.galleryItems;

		if (allItems.size() == 0) {
			return;
		}

		final GalleryMT20Item firstItem = allItems.iterator().next();
		final int itemIndex = _delayedPhotoSelection.selectionIndices[0];

		/*
		 * sync selected item index in the source gallery with the full screen photo gallery, this
		 * is necessary that keyboard navigation has the correct index
		 */
		_sourceGallery.setSelectedItemIndex(itemIndex);

		showImage(firstItem, itemIndex, false);
	}

	private boolean showPhotoGallery(final int mouseY) {
		return _fullScreenGallery.showImages(mouseY, _displayedItemIndex);
	}

	/**
	 * Schedule showing the waiting cursor, this is delayed because smaller images are displayed
	 * very fast and showing the waiting cursor for a short time is flickering the display.
	 */
	private void showWaitingCursor() {

		_isShowWaitCursor = true;

		// Calling timerExec with the same object just delays the
		// execution (doesn't run twice)
		_shell.getDisplay().timerExec(TIME_BEFORE_WAITING_CURSOR_IS_DISPLAYED, _timerWaitingCursor);
	}

	private void sleepCursor() {

		// Calling timerExec with the same object just delays the
		// execution (doesn't run twice)
		_shell.getDisplay().timerExec(TIME_BEFORE_CURSOR_GETS_HIDDEN, _timerSleepCursor);

	}

	/**
	 * Update canvas by starting a redraw
	 * 
	 * @return
	 */
	public boolean updateUI() {

		if (_displayedGalleryItem == null) {
			// there is nothing which can be displayed
			return false;
		}

		if (_shell == null) {
			createUI();
		}

		if (isViewerInitialized() == false) {
			return false;
		}

		/*
		 * painting is complicated because Linux is ignoring the SWT.NO_BACKGROUND style in the
		 * canvas, therefore when doing a redraw() the whole canvas is first painted with the
		 * background color
		 */
		final Rectangle clippingArea = _itemRenderer.drawFullSizeSetContext(//
				_shell,
				_displayedGalleryItem,
				_monitorWidth,
				_monitorHeight);

		if (clippingArea != null) {

			// redraw canvas, which fires an paint event

			_canvas.redraw(clippingArea.x, clippingArea.y, clippingArea.width, clippingArea.height, false);

		} else {

			// canvas is not painted

			if (_isShowLoadingMessage == false) {

				// do not show waiting cursor when loading message is displayed

				showWaitingCursor();
			}
		}

		return true;
	}

}
