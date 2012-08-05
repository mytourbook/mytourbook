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
package net.tourbook.photo;

import net.tourbook.common.util.Util;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

/**
 * Part of this tooltip is copied from {@link ToolTip}.
 */
public abstract class PhotoToolTipShell implements IExternalGalleryMouseListener {

	private Shell					_ttShell;

	private Object					_currentArea;
	private Control					_ownerControl;

	private OwnerControlListener	_ownerControlListener;
	private OwnerShellListener		_ownerShellListener;

	private TooltipShellListener	_ttShellListener;
	private ToolTipControlListener	_ttControlListener;

	private boolean					_isShellResized;

	private int						_mouseDownX;
	private int						_mouseDownY;

	private Display					_display;

	/*
	 * UI resources
	 */
	private Cursor					_cursor_NE_SW;
	private Cursor					_cursor_NW_SE;

	private ImageGallery			_imageGallery;

	private class OwnerControlListener implements Listener {
		public void handleEvent(final Event event) {
			onOwnerControlEvent(event);
		}
	}

	private final class OwnerShellListener implements Listener {
		public void handleEvent(final Event event) {
			onOwnerShellEvent(event);
		}
	}

	/**
	 * This listener is added to ALL widgets within the tooltip shell.
	 */
	private class ToolTipControlListener implements Listener {
		public void handleEvent(final Event event) {
			onTTControlEvent(event);
		}

	}

	private final class TooltipShellListener implements Listener {
		public void handleEvent(final Event event) {
			onTTShellEvent(event);
		}
	}

	/**
	 * Create new instance which add TooltipSupport to the widget
	 * 
	 * @param state
	 * @param ownerControl
	 *            the control on whose action the tooltip is shown
	 */
	public PhotoToolTipShell(final Control ownerControl) {

		_ownerControl = ownerControl;
		_display = _ownerControl.getDisplay();

		_ttControlListener = new ToolTipControlListener();
		_ttShellListener = new TooltipShellListener();

		_ownerControlListener = new OwnerControlListener();
		_ownerShellListener = new OwnerShellListener();

		ownerControlAddListener();
	}

	/**
	 * ########################### Recursive #########################################<br>
	 * <p>
	 * Add listener to all controls within the tooltip
	 * <p>
	 * ########################### Recursive #########################################<br>
	 * 
	 * @param control
	 */
	private void addToolTipControlListener(final Control control) {

		control.addListener(SWT.MouseDown, _ttControlListener);
		control.addListener(SWT.MouseUp, _ttControlListener);
		control.addListener(SWT.MouseMove, _ttControlListener);
		control.addListener(SWT.MouseExit, _ttControlListener);

		if (control instanceof Composite) {
			final Control[] children = ((Composite) control).getChildren();
			for (final Control child : children) {
				addToolTipControlListener(child);
			}
		}
	}

	/**
	 * Creates the content area of the the tooltip.
	 * 
	 * @param event
	 *            the event that triggered the activation of the tooltip
	 * @param shell
	 *            the parent of the content area
	 * @return the content area created
	 */
	protected abstract Composite createToolTipContentArea(Event event, Composite shell);

	private Point fixupDisplayBounds(final Point tipSize, final Point location) {

		Rectangle bounds;
		final Point rightBounds = new Point(tipSize.x + location.x, tipSize.y + location.y);

		final Monitor[] ms = _ownerControl.getDisplay().getMonitors();

		if (ms.length > 1) {
			// By default present in the monitor of the control
			bounds = _ownerControl.getMonitor().getBounds();
			final Point p = new Point(location.x, location.y);

			// Search on which monitor the event occurred
			Rectangle tmp;
			for (final Monitor element : ms) {
				tmp = element.getBounds();
				if (tmp.contains(p)) {
					bounds = tmp;
					break;
				}
			}

		} else {
			bounds = _ownerControl.getDisplay().getBounds();
		}

		if (!(bounds.contains(location) && bounds.contains(rightBounds))) {
			if (rightBounds.x > bounds.x + bounds.width) {
				location.x -= rightBounds.x - (bounds.x + bounds.width);
			}

			if (rightBounds.y > bounds.y + bounds.height) {
				location.y -= rightBounds.y - (bounds.y + bounds.height);
			}

			if (location.x < bounds.x) {
				location.x = bounds.x;
			}

			if (location.y < bounds.y) {
				location.y = bounds.y;
			}
		}

		return location;
	}

	protected abstract Point getLocation(Point size, Event event);

	/**
	 * This method is called to check for which area the tooltip is created/hidden for. In case of
	 * {@link #NO_RECREATE} this is used to decide if the tooltip is hidden recreated.
	 * <code>By the default it is the widget the tooltip is created for but could be any object. To decide if
	 * the area changed the {@link Object#equals(Object)} method is used.</code>
	 * 
	 * @param event
	 *            the event
	 * @return the area responsible for the tooltip creation or <code>null</code> this could be any
	 *         object describing the area (e.g. the {@link Control} onto which the tooltip is bound
	 *         to, a part of this area e.g. for {@link ColumnViewer} this could be a
	 *         {@link ViewerCell})
	 */
	protected Object getToolTipArea(final Event event) {
		return _ownerControl;
	}

	protected Shell getToolTipShell() {
		return _ttShell;
	}

	/**
	 * Hide the currently active tool tip
	 */
	public void hide() {
		toolTipHide(_ttShell, null);
	}

	@Override
	public boolean isMouseEventHandledExternally(final int eventType, final MouseEvent mouseEvent) {

		if (_ttShell == null || _ttShell.isDisposed()) {
			return false;
		}

		final Rectangle shellBounds = _ttShell.getBounds();
		final int shellX = shellBounds.x;
		final int shellY = shellBounds.y;
		final int shellWidth = shellBounds.width;
		final int shellHeight = shellBounds.height;

		final Point mousePos = _ttShell.toControl(_display.getCursorLocation());

		Cursor cursor = null;
		final int resizeBox = 30;

		final int mouseX = mousePos.x;
		final int mouseY = mousePos.y;

		boolean isLeft = false;

		// check if mouse is within the resize box
		if (mouseX < resizeBox && mouseY < resizeBox) {

			// mouse is in the top left resize box

			cursor = _cursor_NW_SE;
			isLeft = true;

		} else if (mouseX > (shellWidth - resizeBox) && mouseY < resizeBox) {

			// mouse is in the top right resize box

			cursor = _cursor_NE_SW;
		}

		boolean isHandled;

		if (cursor == null) {
			isHandled = false;
		} else {
			isHandled = true;

			switch (eventType) {
			case SWT.MouseDown:

				_isShellResized = true;

				_mouseDownX = mouseX;
				_mouseDownY = mouseY;

				System.out.println("tt mouse down\t");
				// TODO remove SYSTEM.OUT.PRINTLN

				break;

			case SWT.MouseUp:

				if (_isShellResized) {

					_isShellResized = false;
				}

				break;

			case SWT.MouseMove:

				if (_isShellResized) {

					if (isLeft) {

						final int diffX = _mouseDownX - mouseX;
						final int diffY = _mouseDownY - mouseY;

						final int newShellX = shellX - diffX;
						final int newShellY = shellY - diffY;
						final int newShellWidth = shellWidth + diffX;
						final int newShellHeight = shellHeight + diffY;

						_ttShell.setLocation(newShellX, newShellY);
						_ttShell.setSize(newShellWidth, newShellHeight);
					}
				}

				break;
			}
		}

		/*
		 * shell could be hidden
		 */
		if (_ttShell != null && !_ttShell.isDisposed()) {
			_ttShell.setCursor(cursor);
		}

		return isHandled;
	}

	private void onOwnerControlEvent(final Event event) {

		if (_ownerControl == null || _ownerControl.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.Dispose:

			toolTipHide(_ttShell, event);
			ownerControlsRemoveListener();

			break;

		case SWT.Resize:
			setTTShellLocation();
			break;
		}
	}

	private void onOwnerShellEvent(final Event event) {

		if (_ttShell == null || _ttShell.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.Deactivate:

			_display.asyncExec(new Runnable() {

				public void run() {

					// hide tooltip when another shell is activated

					if (_display.getActiveShell() != _ttShell) {
						toolTipHide(_ttShell, event);
					}
				}
			});
			break;

		case SWT.Move:
			setTTShellLocation();
			break;
		}
	}

	private void onTTControlEvent(final Event event) {

		if (_ttShell == null || _ttShell.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.MouseExit:

			boolean isHide = false;

			// control which is hovered with the mouse after the exit, can be null
			final Control hoveredExitControl = _display.getCursorControl();

			if (hoveredExitControl == null) {

				isHide = true;

			} else {

				/*
				 * check if the hovered control is the owner control, if not, hide the tooltip
				 */
				Control hoveredExitParent = hoveredExitControl;
				final Control hoveredToolTip = _imageGallery.getGalleryToolTipShell();

				// move up child-parent hierarchy until shell is reached
				while (true) {

					if (hoveredExitParent == _ownerControl) {
						// mouse is over the owner control
						break;
					}

					if (hoveredToolTip != null && hoveredExitParent == hoveredToolTip) {
						// mouse is over the owner tooltip control
						break;
					}

					hoveredExitParent = hoveredExitParent.getParent();

					if (hoveredExitParent == null) {
						// mouse has left the tooltip and the owner control
						isHide = true;
						break;
					}
				}

			}

			if (isHide) {
				toolTipHide(_ttShell, event);
			}

			break;
		}
	}

	private void onTTShellEvent(final Event event) {

		switch (event.type) {
		case SWT.Deactivate:

			if (_ttShell != null && !_ttShell.isDisposed() && _ownerControl != null && !_ownerControl.isDisposed()) {

				_display.asyncExec(new Runnable() {

					public void run() {

						// hide tooltip when another shell is activated

						// check again
						if (_ttShell == null
								|| _ttShell.isDisposed()
								|| _ownerControl == null
								|| _ownerControl.isDisposed()) {
							return;
						}

						if (_ownerControl.getShell() == _ttShell.getDisplay().getActiveShell()) {

							// don't hide when main window is active
							return;
						}

						toolTipHide(_ttShell, null);
					}
				});
			}

			break;

		case SWT.Dispose:

			_cursor_NE_SW = (Cursor) Util.disposeResource(_cursor_NE_SW);
			_cursor_NW_SE = (Cursor) Util.disposeResource(_cursor_NW_SE);

			break;

		}

	}

	/**
	 * Activate tooltip support for this control
	 */
	private void ownerControlAddListener() {

		ownerControlsRemoveListener();

		_ownerControl.addListener(SWT.Dispose, _ownerControlListener);
//		_ownerControl.addListener(SWT.MouseEnter, _ownerControlListener);
		_ownerControl.addListener(SWT.Resize, _ownerControlListener);
	}

	/**
	 * Deactivate tooltip support for the underlying control
	 */
	private void ownerControlsRemoveListener() {

		_ownerControl.removeListener(SWT.Dispose, _ownerControlListener);
//		_ownerControl.removeListener(SWT.MouseEnter, _ownerControlListener);
		_ownerControl.removeListener(SWT.Resize, _ownerControlListener);
	}

	private void ownerShellAddListener() {

		final Shell ownerShell = _ownerControl.getShell();

		ownerShell.addListener(SWT.Deactivate, _ownerShellListener);
		ownerShell.addListener(SWT.Move, _ownerShellListener);
	}

	private void ownerShellRemoveListener() {

		final Shell ownerShell = _ownerControl.getShell();

		ownerShell.removeListener(SWT.Deactivate, _ownerShellListener);
		ownerShell.removeListener(SWT.Move, _ownerShellListener);
	}

	private void passOnEvent(final Shell tip, final Event event) {
		if (_ownerControl != null
				&& !_ownerControl.isDisposed()
				&& event != null
				&& event.widget != _ownerControl
				&& event.type == SWT.MouseDown) {
			// the following was left in order to fix bug 298770 with minimal change. In 3.7, the complete method should be removed.
			tip.close();
		}
	}

	/**
	 * Set image gallery, this is done, after {@link #createToolTipContentArea(Event, Composite)} is
	 * executed.
	 * 
	 * @param imageGallery
	 */
	protected void setImageGallery(final ImageGallery imageGallery) {
		_imageGallery = imageGallery;
	}

	protected void setTTShellLocation() {

		if (_ttShell == null || _ttShell.isDisposed()) {
			return;
		}

		final Point size = _ttShell.getSize();
		final Point location = fixupDisplayBounds(size, getLocation(size, null));

		_ttShell.setLocation(location);
		_ttShell.setVisible(true);
	}

	/**
	 * Should the tooltip displayed because of the given event.
	 * <p>
	 * <b>Subclasses may overwrite this to get custom behavior</b>
	 * </p>
	 * 
	 * @param event
	 *            the event
	 * @return <code>true</code> if tooltip should be displayed
	 */
	protected boolean shouldCreateToolTip(final Event event) {

		final Object ttArea = getToolTipArea(event);

		// No new area close the current tooltip
		if (ttArea == null) {
			hide();
			return false;
		}

		final boolean rv = !ttArea.equals(_currentArea);
		return rv;
	}

	/**
	 * This method is called before the tooltip is hidden
	 * 
	 * @param event
	 *            the event trying to hide the tooltip
	 * @return <code>true</code> if the tooltip should be hidden
	 */
	private boolean shouldHideToolTip(final Event event) {

		if (event != null && event.type == SWT.MouseMove) {

			final Object ttArea = getToolTipArea(event);

			// No new area close the current tooltip
			if (ttArea == null) {
				hide();
				return false;
			}

			final boolean rv = !ttArea.equals(_currentArea);
			return rv;
		}

		return true;
	}

	/**
	 * Start up the tooltip programmatically
	 * 
	 * @param location
	 *            the location relative to the control the tooltip is shown
	 * @return
	 */
	public boolean show(final Point location) {

		/*
		 * show tooltip only when this is the active shell, this check is necessary that when a tour
		 * chart is opened in a dialog (e.g. adjust altitude) that a hidden tour chart tooltip in
		 * the tour chart view is also displayed
		 */
		if (_display.getActiveShell() != _ownerControl.getShell() || _ownerControl.isVisible() == false) {
			return false;
		}

		final Event event = new Event();
		event.x = location.x;
		event.y = location.y;
		event.widget = _ownerControl;

		toolTipCreate(event);

		return true;
	}

	private void toolTipCreate(final Event event) {

		if (shouldCreateToolTip(event)) {

			final Shell shell = new Shell(_ownerControl.getShell(), //
					SWT.ON_TOP //
//							| SWT.TOOL
							| SWT.NO_FOCUS
							| SWT.NO_TRIM
			//
			);

			shell.setLayout(new FillLayout());

			toolTipOpen(shell, event);
		}
	}

	private void toolTipHide(final Shell ttShell, final Event event) {

		if (ttShell == null || ttShell.isDisposed()) {
			return;
		}

		if (shouldHideToolTip(event)) {

			// hide tooltip definitively

			ownerShellRemoveListener();

			_currentArea = null;

			passOnEvent(ttShell, event);

			ttShell.dispose();
			_ttShell = null;

			return;
		}
	}

	private void toolTipOpen(final Shell shell, final Event event) {

		// Ensure that only one Tooltip is shown in time
		if (_ttShell != null) {
			toolTipHide(_ttShell, null);
		}

		_ttShell = shell;

		// close tooltip if user selects outside of the shell
		_ttShell.addListener(SWT.Deactivate, _ttShellListener);
		_ttShell.addListener(SWT.Dispose, _ttShellListener);

		ownerShellAddListener();

		_cursor_NE_SW = new Cursor(_display, SWT.CURSOR_SIZENESW);
		_cursor_NW_SE = new Cursor(_display, SWT.CURSOR_SIZENWSE);

		_isShellResized = false;

		toolTipShow(event);
	}

	private void toolTipShow(final Event event) {

		if (!_ttShell.isDisposed()) {

			_currentArea = getToolTipArea(event);

			createToolTipContentArea(event, _ttShell);

			addToolTipControlListener(_ttShell);

			_imageGallery.setExternalMouseListener(this);

			_ttShell.pack();

			final Point size = _ttShell.getSize();
			final Point location = fixupDisplayBounds(size, getLocation(size, event));
			_ttShell.setLocation(location);

			_ttShell.setVisible(true);
		}
	}
}
