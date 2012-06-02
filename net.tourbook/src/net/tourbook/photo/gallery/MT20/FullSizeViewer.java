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

import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class FullSizeViewer {

	private static final String				STATE_FULL_SIZE_VIEWER_ZOOM_STATE	= "STATE_FULL_SIZE_VIEWER_ZOOM_STATE";	//$NON-NLS-1$

	private static double					MIN_ZOOM							= 1.0 / 50;
	private static double					MAX_ZOOM							= 50;

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

	/*
	 * UI resources
	 */
	private Color							_fgColor;
	private Color							_bgColor;

	/*
	 * UI controls
	 */
	private Shell							_shell;
	private Canvas							_canvas;

	public FullSizeViewer() {

	}

	public FullSizeViewer(final GalleryMT20 gallery, final AbstractGalleryMT20ItemRenderer itemRenderer) {

		_gallery = gallery;

		_itemRenderer = itemRenderer;
	}

	void close() {

		if (_shell != null) {

			_shell.dispose();
			_shell = null;

			_gallery.onCloseFullsizeViewer();
			_galleryItem = null;
		}
	}

	private void createUI_Shell() {

		_shell = new Shell(SWT.NO_TRIM /* | SWT.ON_TOP */);

		_shell.setLayout(new FillLayout());

		_canvas = new Canvas(_shell, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);

		_canvas.setForeground(_fgColor);
//		_canvas.setBackground(_bgColor);
		_canvas.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));

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

		_shell.open();

		updateBounds();

		_shell.setVisible(true);
//		_shell.setFullScreen(true);
		_shell.setActive();
	}

	public GalleryMT20Item getCurrentItem() {
		return _galleryItem;
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
		// TODO Auto-generated method stub

	}

	private void onMouseMove(final MouseEvent mouseEvent) {
		// TODO Auto-generated method stub

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

		if (_monitorWidth == 0) {
			// ui is not initialized
			return;
		}

		if (_zoomFactor == 0.0) {
			_zoomState = ZoomState.FIT_WINDOW;
		}

		final PaintingResult paintingResult = _itemRenderer.drawFullSize(
				gc,
				_galleryItem,
				_monitorWidth,
				_monitorHeight,
				_zoomState,
				_zoomFactor);

		if (paintingResult != null) {

			// get zoomfactor when image is painted to fill window

			_zoomFactor = paintingResult.imagePaintedZoomFactor;
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

	}

	void saveState(final IDialogSettings state) {

		state.put(STATE_FULL_SIZE_VIEWER_ZOOM_STATE, _zoomState.name());
	}

	void setColors(final Color fgColor, final Color bgColor) {
		_fgColor = fgColor;
		_bgColor = bgColor;
	}

	void setItemRenderer(final AbstractGalleryMT20ItemRenderer itemRenderer) {
		_itemRenderer = itemRenderer;
	}

	void showImage(final GalleryMT20Item galleryItem) {

		_galleryItem = galleryItem;

		if (_shell == null || _shell.isDisposed()) {
			createUI_Shell();
		}

		_shell.setActive();

		_canvas.redraw();
	}

	private void updateBounds() {

		final Rectangle monitorBounds = Display.getDefault().getPrimaryMonitor().getBounds();

		_monitorWidth = (int) (monitorBounds.width * 0.7);
		_monitorHeight = (int) (monitorBounds.height * 0.7);

//		_shell.setBounds(monitorBounds);
		_shell.setBounds(100, 10, _monitorWidth, _monitorHeight);
	}

	/**
	 * Update canvas by starting a redraw
	 */
	public void updateUI() {

		if (_shell == null) {
			createUI_Shell();
		}

		_canvas.redraw();
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
