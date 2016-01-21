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
package net.tourbook.common.action;

import net.tourbook.common.util.StatusUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TypedListener;

/**
 * Simple image action button.
 */
public class SmallImageButton extends Canvas {

//	private boolean	_isEnabled;

	private int		_imageWidth		= 16;
	private int		_imageHeight	= 16;

	private Image	_imageEnabled;
	private Image	_imageDisabled;
	private Image	_imageHovered;

	private boolean	_isHovered;

	public SmallImageButton(final Composite parent,
							final Image imageEnabled,
							final Image imageDisabled,
							final Image imageHovered) {

		super(parent, SWT.DOUBLE_BUFFERED | SWT.NO_FOCUS);

		if (imageEnabled == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		_imageEnabled = imageEnabled;
		_imageDisabled = imageDisabled;
		_imageHovered = imageHovered;

		final Rectangle imageSize = imageEnabled.getBounds();
		_imageWidth = imageSize.width;
		_imageHeight = imageSize.height;

		addListener();
	}

	private void addListener() {

		addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent e) {
				onPaint(e);
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				onMouseDown(e);
			}
		});

		addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseEnter(final MouseEvent e) {
				onMouseEnter(e);
			}

			@Override
			public void mouseExit(final MouseEvent e) {
				onMouseExit(e);
			}
		});
	}

	/**
	 * Adds the listener to the collection of listeners who will be notified when ratings stars has
	 * been modified by the user, by sending it one of the messages defined in the
	 * <code>SelectionListener</code> interface.
	 * <p>
	 * <code>widgetSelected</code> is called when the control is selected by the user.
	 * <code>widgetDefaultSelected</code> is not called.
	 * </p>
	 * <p>
	 * During <code>widgetSelected</code> the application can use <code>getSelection()</code> to
	 * determine the current selected state of the receiver.
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

		checkWidget();

		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		final TypedListener typedListener = new TypedListener(listener);
		addListener(SWT.Selection, typedListener);
	}

	@Override
	public Point computeSize(final int wHint, final int hHint, final boolean changed) {

		checkWidget();

		final Point size = new Point(_imageWidth, _imageHeight);

		return size;
	}

	/**
	 * Send a selection event {@link SWT#Selection} or {@link SWT#DefaultSelection} for a gallery
	 * item.
	 * <p>
	 * {@link Event#data} contains the selected rating stars.
	 */
	private void notifySelectionListeners() {

		final Event e = new Event();

		e.widget = this;

		try {
			notifyListeners(SWT.Selection, e);
		} catch (final RuntimeException ex) {
			StatusUtil.log(ex);
		}
	}

	private void onMouseDown(final MouseEvent mouseEvent) {

		if (isEnabled()) {
			notifySelectionListeners();
		}
	}

	private void onMouseEnter(final MouseEvent e) {

		_isHovered = true;

		redraw();
	}

	private void onMouseExit(final MouseEvent e) {

		_isHovered = false;

		redraw();
	}

	private void onPaint(final PaintEvent paintEvent) {

		final GC gc = paintEvent.gc;

		Image actionImage = isEnabled() ? _isHovered ? _imageHovered : _imageEnabled : _imageDisabled;

		if (actionImage == null) {
			actionImage = _imageEnabled;
		}

		gc.drawImage(actionImage, 0, 0);
	}

	/**
	 * Removes the listener from the collection of listeners who will be notified when a rating star
	 * is selected by the user.
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
	 * @see #addSelectionListener
	 */
	public void removeSelectionListener(final SelectionListener listener) {

		checkWidget();

		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		removeListener(SWT.Selection, listener);
	}

}
