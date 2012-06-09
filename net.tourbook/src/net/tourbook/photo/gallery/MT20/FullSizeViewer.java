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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.photo.Messages;
import net.tourbook.photo.PrefPagePhotoFullsizeViewer;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.action.ActionOpenPrefDialog;
import net.tourbook.util.UI;
import net.tourbook.util.Util;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

public class FullSizeViewer {

	private static final int				TIME_BEFORE_CURSOR_GETS_HIDDEN			= 2000;
	private static final int				TIME_BEFORE_WAITING_CURSOR_IS_DISPLAYED	= 200;

	private static final String				STATE_FULL_SIZE_VIEWER_ZOOM_STATE		= "STATE_FULL_SIZE_VIEWER_ZOOM_STATE";	//$NON-NLS-1$

	private static double					MIN_ZOOM								= 1.0 / 50;
	private static double					MAX_ZOOM								= 50;

	private final IPreferenceStore			_prefStore								= TourbookPlugin
																							.getDefault()
																							.getPreferenceStore();

	private GalleryMT20						_gallery;
	private AbstractGalleryMT20ItemRenderer	_itemRenderer;

	private GalleryMT20Item					_galleryItem;

	private int								_monitorWidth;
	private int								_monitorHeight;

	/**
	 * State how images are zoomed when displayed.
	 */
	private ZoomState						_zoomState;

	private double							_zoomFactor;

	private TimerWaitCursor					_timerWaitingCursor						= new TimerWaitCursor();
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
	private Shell							_shell;
	private Canvas							_canvas;

	private Cursor							_cursorWait;
	private Cursor							_cursorHidden;
	private Cursor							_cursorSizeAll;
	private Font							_font;

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

	public FullSizeViewer() {}

	public FullSizeViewer(final GalleryMT20 gallery, final AbstractGalleryMT20ItemRenderer itemRenderer) {

		_gallery = gallery;

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
				ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_PREVIEW,
				isShowPreview);

		_prefStore.setValue(//
				ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_LOADING_MESSAGE,
				isShowLoadingMessage);

		_prefStore.setValue(//
				ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_HQ_IMAGE,
				isShowHQImage);

		setPrefSettings(isShowPreview, isShowLoadingMessage, isShowHQImage);

		updateUI();
	}

	void close() {

		// prevent that an old image is displayed
		_itemRenderer.resetPreviousImage();

		if (_shell != null) {

			_shell.dispose();
			_shell = null;

			_gallery.onCloseFullsizeViewer();
			_galleryItem = null;
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

	private void createUI() {

//		_shell = new Shell(SWT.NO_TRIM | SWT.ON_TOP);
		_shell = new Shell(SWT.NO_TRIM);

		_shell.setForeground(_fgColor);
//		_shell.setBackground(_bgColor);
		_shell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));

		updateBounds();

		_shell.setLayout(new FillLayout());

		/*
		 * SWT.NO_BACKGROUND can cause that other content which is drawn in another window can be
		 * displayed, e.g the gallery content was painted at the top of the canvas
		 */
//		_canvas = new Canvas(_shell, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
		_canvas = new Canvas(_shell, SWT.DOUBLE_BUFFERED);

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

		_canvas.setMenu(createUI_ContextMenu(_canvas));

		_cursorHidden = UI.createHiddenCursor();
		_cursorSizeAll = new Cursor(_shell.getDisplay(), SWT.CURSOR_SIZEALL);
		_cursorWait = new Cursor(_shell.getDisplay(), SWT.CURSOR_WAIT);

//		_shell.setFullScreen(true);
		_shell.setVisible(true);
		_shell.setActive();

		_shell.open();
	}

	private Menu createUI_ContextMenu(final Composite parent) {

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

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionShowHQImage);
		menuMgr.add(_actionShowLoadingMessage);
		menuMgr.add(_actionShowThumbPreview);

		menuMgr.add(new Separator());
		menuMgr.add(_actionOpenFullsizePrefPage);
	}

	public GalleryMT20Item getCurrentItem() {
		return _galleryItem;
	}

	private boolean isViewerInitialized() {

		if (_zoomFactor == 0.0) {
			_zoomState = ZoomState.FIT_WINDOW;
		}

		if (_monitorWidth == 0) {
			// ui is not initialized
			return false;
		}

		if (_galleryItem == null) {
			// can happen after close event (it did)
			return false;
		}

		return true;
	}

	private void onDispose() {

		_cursorHidden.dispose();
		_cursorSizeAll.dispose();
		_cursorWait.dispose();
	}

	private void onKeyPressed(final KeyEvent keyEvent) {

		switch (keyEvent.keyCode) {
		case SWT.ESC:
			close();
			break;
		}

		final boolean isShift = (keyEvent.stateMask & SWT.MOD2) != 0;

		switch (keyEvent.character) {
		case '1':

			// toggle full size and fit window

			_zoomState = _zoomState == ZoomState.FIT_WINDOW ? ZoomState.ZOOMING : ZoomState.FIT_WINDOW;
			_zoomFactor = 1.0;

			updateUI();

			break;

		case '+':

			// zoom IN

			zoomIn(isShift);
			break;

		case '-':

			// zoom OUT

			zoomOut(isShift);
			break;

		}
	}

	private void onMouseDown(final MouseEvent mouseEvent) {

	}

	private void onMouseMove(final MouseEvent mouseEvent) {

		_canvas.setCursor(_cursorSizeAll);

		sleepCursor();
	}

	private void onMouseWheel(final MouseEvent mouseEvent) {

		final boolean isDown = mouseEvent.count > 0;

		final boolean isCtrl = (mouseEvent.stateMask & SWT.MOD1) != 0;
		final boolean isShift = (mouseEvent.stateMask & SWT.MOD2) != 0;

		if (isCtrl) {

			// zoom image

			if (isDown) {
				zoomIn(isShift);
			} else {
				zoomOut(isShift);
			}

		} else {

			// select next/previous image

			_gallery.selectItem(isDown ? -1 : 1);
		}
	}

	private void onPaint(final GC gc) {

		if (isViewerInitialized() == false) {
			return;
		}

		final long start = System.currentTimeMillis();

		final PaintingResult paintingResult = _itemRenderer.drawFullSize(
				gc,
				_galleryItem,
				_monitorWidth,
				_monitorHeight,
				_zoomState,
				_zoomFactor);

		System.out.println(UI.timeStamp() + "draw fullsize image " + (System.currentTimeMillis() - start) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$

		if (paintingResult != null) {

			// get zoomfactor when image is painted to fill window

			_zoomFactor = paintingResult.imagePaintedZoomFactor;
		}

		final boolean isOriginalImagePainted = paintingResult.isOriginalImagePainted;

		// hide cursor when image is finally painted or when loading error
		if (isOriginalImagePainted || paintingResult.isLoadingError) {

			_isShowWaitCursor = false;
			_canvas.setCursor(_cursorHidden);

		} else if (_isShowLoadingMessage == false) {

			// do not show waiting cursor when loading message is displayed

			showWaitingCursor();
		}
	}

	void restoreState(final IDialogSettings state) {

		final ZoomState defaultZoom = ZoomState.FIT_WINDOW;
		final String stateValue = Util.getStateString(state, STATE_FULL_SIZE_VIEWER_ZOOM_STATE, defaultZoom.name());
		try {
			_zoomState = ZoomState.valueOf(stateValue);
		} catch (final Exception e) {
			_zoomState = defaultZoom;
		}

		_actionShowThumbPreview.setChecked(_prefStore.getBoolean(//
				ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_PREVIEW));

		_actionShowHQImage.setChecked(_prefStore.getBoolean(//
				ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_HQ_IMAGE));

		_actionShowLoadingMessage.setChecked(_prefStore.getBoolean(//
				ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_LOADING_MESSAGE));
	}

	void saveState(final IDialogSettings state) {

		state.put(STATE_FULL_SIZE_VIEWER_ZOOM_STATE, _zoomState.name());
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

		final boolean isShowPreview = _prefStore.getBoolean(ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_PREVIEW);

		final boolean isShowLoadingMessage = _prefStore.getBoolean(//
				ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_LOADING_MESSAGE);

		final boolean isShowHQImage = _prefStore.getBoolean(//
				ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_HQ_IMAGE);

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

	void showImage(final GalleryMT20Item galleryItem) {

		_galleryItem = galleryItem;

		if (updateUI() == false) {
			return;
		}

		_shell.setActive();
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

	private void updateBounds() {

		final Rectangle monitorBounds = Display.getDefault().getPrimaryMonitor().getBounds();

		final double partialFullsize = 1;
//		final double partialFullsize = 0.8;

		_monitorWidth = (int) (monitorBounds.width * partialFullsize);
		_monitorHeight = (int) (monitorBounds.height * partialFullsize);

		_shell.setBounds(monitorBounds);
//		_shell.setBounds(100, 10, _monitorWidth, _monitorHeight);
	}

	/**
	 * Update canvas by starting a redraw
	 * 
	 * @return
	 */
	public boolean updateUI() {

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
				_galleryItem,
				_monitorWidth,
				_monitorHeight);

		if (clippingArea != null) {

			// redraw canvas

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

	private void zoomIn(final boolean isShift) {

		if (_zoomFactor < MAX_ZOOM) {

			final int accelerator = isShift ? 5 : 1;

			_zoomState = ZoomState.ZOOMING;

			_zoomFactor = _zoomFactor < 1//
					? _zoomFactor + (0.02 * accelerator)
					: _zoomFactor + (1 * accelerator);

			// ensure max zoom
			_zoomFactor = Math.min(MAX_ZOOM, _zoomFactor);

			updateUI();
		}
	}

	private void zoomOut(final boolean isShift) {

		if (_zoomFactor > MIN_ZOOM) {

			final int accelerator = isShift ? 5 : 1;

			_zoomState = ZoomState.ZOOMING;

			if (_zoomFactor > 1) {

				final double zoomFactor = _zoomFactor - (1 * accelerator);

				// check if threshold 1 has changed
				if (zoomFactor > 1) {
					_zoomFactor = zoomFactor;
				} else {
					_zoomFactor = 1;
				}

			} else {
				_zoomFactor = _zoomFactor - (0.02 * accelerator);
			}

			// ensure min zoom
			_zoomFactor = Math.max(MIN_ZOOM, _zoomFactor);

			updateUI();
		}
	}

}
