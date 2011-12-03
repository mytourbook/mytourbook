/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart;

import net.tourbook.chart.ITooltipOwner;
import net.tourbook.chart.Util;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import org.eclipse.swt.widgets.ToolBar;

/**
 * Part of this tooltip is copied from {@link ToolTip}.
 */
public abstract class ToolTipValuePoint {

	private ITooltipOwner			_tooltipOwner;

	// Ensure that only one tooltip is active in time
	private static Shell			_ttShell;
	private Object					_currentArea;
	private Control					_ownerControl;

	private int						_xShift					= 3;
	private int						_yShift					= 0;

	private OwnerShellListener		_ownerShellListener;
	private OwnerControlListener	_ownerControlListener;
	private TooltipListener			_ttListener				= new TooltipListener();
	private TooltipShellListener	_ttShellListener		= new TooltipShellListener();

	private boolean					_isRespectDisplayBounds	= true;
	private boolean					_isRespectMonitorBounds	= true;

	private boolean					_isTTMouseDown;
	private int						_devXMouseDown;
	private int						_devYMouseDown;

	/*
	 * UI resources
	 */
	private Cursor					_cursorDragged;
	private Cursor					_cursorHand;

	/**
	 * Shell location before it is hidden.
	 */
	private Point					_ttShellHideLocation;

	private class OwnerControlListener implements Listener {
		public void handleEvent(final Event event) {

			switch (event.type) {
//			case SWT.MouseMove:
//			case SWT.KeyDown:
//			case SWT.MouseDown:
//			case SWT.MouseWheel:

			case SWT.Dispose:
				toolTipHide(_ttShell, event);
				break;

			case SWT.MouseHover:
				toolTipCreate(event);
				break;

			case SWT.MouseExit:

				/*
				 * Check if the mouse exit happened because we move over the tooltip
				 */
				if (_ttShell != null && !_ttShell.isDisposed()) {
					if (_ttShell.getBounds().contains(_ownerControl.toDisplay(event.x, event.y))) {
						break;
					}
				}

//				toolTipHide(_ttShell, event);
				break;
			}
		}
	}

	private final class OwnerShellListener implements Listener {
		public void handleEvent(final Event event) {

			if (_ownerControl != null && !_ownerControl.isDisposed()) {

				_ownerControl.getDisplay().asyncExec(new Runnable() {

					public void run() {

						// hide tooltip when another shell is activated

						if (_ownerControl.getDisplay().getActiveShell() != _ttShell) {
							toolTipHide(_ttShell, event);
						}
					}
				});
			}
		}
	}

	/**
	 * This listener is added to ALL widgets within the tooltip shell.
	 */
	private class TooltipListener implements Listener {
		public void handleEvent(final Event event) {

			if (_ttShell == null || _ttShell.isDisposed()) {
				return;
			}

			if (event.widget instanceof Control) {

				final Control control = (Control) event.widget;
				boolean isToolbar = false;

//				System.out.println("control\t" + control.toString());
//				// TODO remove SYSTEM.OUT.PRINTLN

				Cursor cursor = null;

				if (control instanceof ToolBar) {

					// disable other features when toolbar actions are hovered
					isToolbar = true;

				} else {

					final int devXMouse = event.x;
					final int devYMouse = event.y;

					switch (event.type) {
					case SWT.MouseMove:

						if (_isTTMouseDown) {
							setTTLocationHasMoved(event);
						} else {

							/*
							 * move value point in the chart when tooltip is hovered and the mouse
							 * position is within the chart
							 */
							_tooltipOwner.handleEventMouseMove(control.toDisplay(event.x, event.y));

							cursor = _cursorHand;
						}

						break;

					case SWT.MouseDown:

						_isTTMouseDown = true;

						_devXMouseDown = devXMouse;
						_devYMouseDown = devYMouse;

						cursor = _cursorDragged;

						break;

					case SWT.MouseUp:

						_isTTMouseDown = false;
						cursor = _cursorHand;

						break;

					case SWT.MouseExit:

						_isTTMouseDown = false;

						break;
					}
				}

				/*
				 * shell could be hidden (it was during testing)
				 */
				if (_ttShell != null && !_ttShell.isDisposed()) {

					if (cursor != null) {

						_ttShell.setCursor(cursor);

					} else if (isToolbar) {

						// display normal cursor when toolbar actions are hovered
						_ttShell.setCursor(null);
					}
				}
			}
		}
	}

	private final class TooltipShellListener implements Listener {
		public void handleEvent(final Event e) {

//			System.out.println("TooltipShellListener()\t");
			// TODO remove SYSTEM.OUT.PRINTLN

			if (_ttShell != null && !_ttShell.isDisposed() && _ownerControl != null && !_ownerControl.isDisposed()) {

				_ownerControl.getDisplay().asyncExec(new Runnable() {

					public void run() {

						// hide tooltip when another shell is activated

						// check again
						if (_ttShell == null
								|| _ttShell.isDisposed()
								|| _ownerControl == null
								|| _ownerControl.isDisposed()) {
							return;
						}

						final Shell ownerControlShell = _ownerControl.getShell();
						final Display ttShellDisplay = _ttShell.getDisplay();

						if (ownerControlShell == ttShellDisplay.getActiveShell()) {

							// don't hide when main window is active
							return;
						}

						toolTipHide(_ttShell, null);
					}
				});
			}
		}
	}

	/**
	 * Create new instance which add TooltipSupport to the widget
	 * 
	 * @param control
	 *            the control on whose action the tooltip is shown
	 */
	public ToolTipValuePoint(final ITooltipOwner tooltipOwner) {

		_tooltipOwner = tooltipOwner;
		_ownerControl = tooltipOwner.getControl();
		_ownerControl.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		_ownerControlListener = new OwnerControlListener();
		_ownerShellListener = new OwnerShellListener();

		addControlListener();

		_cursorDragged = new Cursor(_ownerControl.getDisplay(), SWT.CURSOR_SIZEALL);
		_cursorHand = new Cursor(_ownerControl.getDisplay(), SWT.CURSOR_HAND);
	}

	void actionCloseToolTip(final Event event) {
		// TODO Auto-generated method stub

	}

	/**
	 * Activate tooltip support for this control
	 */
	public void addControlListener() {

		removeControlListener();

		_ownerControl.addListener(SWT.Dispose, _ownerControlListener);
		_ownerControl.addListener(SWT.MouseHover, _ownerControlListener);
		_ownerControl.addListener(SWT.MouseMove, _ownerControlListener);
		_ownerControl.addListener(SWT.MouseExit, _ownerControlListener);
		_ownerControl.addListener(SWT.MouseDown, _ownerControlListener);
		_ownerControl.addListener(SWT.MouseWheel, _ownerControlListener);
	}

	/**
	 * Add listener to all controls within the tooltip
	 * <p>
	 * ########################### Recursive #########################################<br>
	 * 
	 * @param control
	 */
	private void addTooltipListener(final Control control) {

		control.addListener(SWT.MouseMove, _ttListener);
		control.addListener(SWT.MouseDown, _ttListener);
		control.addListener(SWT.MouseUp, _ttListener);
		control.addListener(SWT.MouseExit, _ttListener);

		if (control instanceof Composite) {
			final Control[] children = ((Composite) control).getChildren();
			for (final Control child : children) {
				addTooltipListener(child);
			}
		}
	}

	/**
	 * This method is called after a tooltip is hidden.
	 * <p>
	 * <b>Subclasses may override to clean up requested system resources</b>
	 * </p>
	 * 
	 * @param event
	 *            event triggered the hiding action (may be <code>null</code> if event wasn't
	 *            triggered by user actions directly)
	 */
	protected void afterHideToolTip(final Event event) {

	}

	/**
	 * Creates the content area of the the tooltip.
	 * 
	 * @param event
	 *            the event that triggered the activation of the tooltip
	 * @param parent
	 *            the parent of the content area
	 * @return the content area created
	 */
	protected abstract Composite createToolTipContentArea(Event event, Composite parent);

	private Point fixupDisplayBounds(final Point tipSize, final Point location) {

		if (_isRespectDisplayBounds || _isRespectMonitorBounds) {

			Rectangle bounds;
			final Point rightBounds = new Point(tipSize.x + location.x, tipSize.y + location.y);

			final Monitor[] monitors = _ownerControl.getDisplay().getMonitors();

			if (_isRespectMonitorBounds && monitors.length > 1) {
				// By default present in the monitor of the control
				bounds = _ownerControl.getMonitor().getBounds();
				final Point p = new Point(location.x, location.y);

				// Search on which monitor the event occurred
				Rectangle tmp;
				for (final Monitor monitor : monitors) {
					tmp = monitor.getBounds();
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
		}

		return location;
	}

	/**
	 * Get the display relative location where the tooltip is displayed. Subclasses may overwrite to
	 * implement custom positioning.
	 * 
	 * @param tipSize
	 *            the size of the tooltip to be shown
	 * @param event
	 *            the event triggered showing the tooltip
	 * @return the absolute position on the display
	 */
	private Point getLocation(final Point tipSize, final Event event) {
		return _ownerControl.toDisplay(event.x + _xShift, event.y + _yShift);
	}

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

	/**
	 * Hide the currently active tool tip
	 */
	public void hide() {
		toolTipHide(_ttShell, null);
	}

	/**
	 * Return whether the tooltip respects bounds of the display.
	 * 
	 * @return <code>true</code> if the tooltip respects bounds of the display
	 */
	public boolean isRespectDisplayBounds() {
		return _isRespectDisplayBounds;
	}

	/**
	 * Return whether the tooltip respects bounds of the monitor.
	 * 
	 * @return <code>true</code> if tooltip respects the bounds of the monitor
	 */
	public boolean isRespectMonitorBounds() {
		return _isRespectMonitorBounds;
	}

	private void onDispose() {

		_cursorDragged = Util.disposeResource(_cursorDragged);
		_cursorHand = Util.disposeResource(_cursorHand);

		removeControlListener();
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
	 * Deactivate tooltip support for the underlying control
	 */
	private void removeControlListener() {
		_ownerControl.removeListener(SWT.Dispose, _ownerControlListener);
		_ownerControl.removeListener(SWT.MouseHover, _ownerControlListener);
		_ownerControl.removeListener(SWT.MouseMove, _ownerControlListener);
		_ownerControl.removeListener(SWT.MouseExit, _ownerControlListener);
		_ownerControl.removeListener(SWT.MouseDown, _ownerControlListener);
		_ownerControl.removeListener(SWT.MouseWheel, _ownerControlListener);
	}

	private void setLocation(final Event event) {

		final Point size = _ttShell.getSize();
		Point beforeFixupLocation;

		if (_ttShellHideLocation != null) {
			// position tooltip at the same position before it was hidden
			beforeFixupLocation = _ttShellHideLocation;
		} else {
			final Point defaultLocation = getLocation(size, event);
			beforeFixupLocation = defaultLocation;
		}

		final Point fixupLocation = fixupDisplayBounds(size, beforeFixupLocation);

		// Need to adjust a bit more if the mouse cursor.y == tip.y and
		// the cursor.x is inside the tip
		final Point cursorLocation = _ttShell.getDisplay().getCursorLocation();

		if (cursorLocation.y == fixupLocation.y //
				&& fixupLocation.x < cursorLocation.x
				&& fixupLocation.x + size.x > cursorLocation.x) {
			fixupLocation.y -= 2;
		}

		_ttShell.setLocation(fixupLocation);
	}

	/**
	 * Set to <code>false</code> if display bounds should not be respected or to <code>true</code>
	 * if the tooltip is should repositioned to not overlap the display bounds.
	 * <p>
	 * Default is <code>true</code>
	 * </p>
	 * 
	 * @param respectDisplayBounds
	 */
	public void setRespectDisplayBounds(final boolean respectDisplayBounds) {
		_isRespectDisplayBounds = respectDisplayBounds;
	}

	/**
	 * Set to <code>false</code> if monitor bounds should not be respected or to <code>true</code>
	 * if the tooltip is should repositioned to not overlap the monitors bounds. The monitor the
	 * tooltip belongs to is the same is control's monitor the tooltip is shown for.
	 * <p>
	 * Default is <code>true</code>
	 * </p>
	 * 
	 * @param respectMonitorBounds
	 */
	public void setRespectMonitorBounds(final boolean respectMonitorBounds) {
		_isRespectMonitorBounds = respectMonitorBounds;
	}

	/**
	 * Set the shift (from the mouse position triggered the event) used to display the tooltip.
	 * <p>
	 * By default the tooltip is shifted 3 pixels to the right.
	 * </p>
	 * 
	 * @param p
	 *            the new shift
	 */
	public void setShift(final Point p) {
		_xShift = p.x;
		_yShift = p.y;
	}

	private void setTTLocationHasMoved(final Event event) {

		final Point size = _ttShell.getSize();

		final Point shellLocation = _ttShell.getLocation();

		final int xDiff = event.x - _devXMouseDown;
		final int yDiff = event.y - _devYMouseDown;

		final Point movedShellLocation = new Point(shellLocation.x + xDiff, shellLocation.y + yDiff);

		final Point fixedLocation = fixupDisplayBounds(size, movedShellLocation);

		_ttShell.setLocation(fixedLocation);

//		System.out.println("x:" + event.x + "\ty:" + event.y + "\t" + _devXMouseDown + "\t" + _devYMouseDown);
		// TODO remove SYSTEM.OUT.PRINTLN

//		final Point ownerLocation = _ownerControl.toDisplay(event.x, event.y);
//
//		final Point location = fixupDisplayBounds(size, ownerLocation);
//
//		System.out.println("x:" + event.x + "\ty:" + event.y + "\t" + ownerLocation + "\t" + location);
//		// TODO remove SYSTEM.OUT.PRINTLN
//
//
//		_ttShell.setLocation(location);
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
	 */
	public void show(final Point location) {

		final Event event = new Event();
		event.x = location.x;
		event.y = location.y;
		event.widget = _ownerControl;

		toolTipCreate(event);
	}

	private void toolTipCreate(final Event event) {

		if (shouldCreateToolTip(event)) {

			final Shell shell = new Shell(_ownerControl.getShell(), SWT.ON_TOP | SWT.TOOL | SWT.NO_FOCUS);

			shell.setLayout(new FillLayout());
//			shell.setLayout(new GridLayout());
//			shell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

			toolTipOpen(shell, event);
		}
	}

	private void toolTipHide(final Shell ttShell, final Event event) {

		if (ttShell != null && !ttShell.isDisposed()) {

			if (shouldHideToolTip(event)) {

//				System.out.println("toolTipHide:hide\t");
// TODO remove SYSTEM.OUT.PRINTLN

				_ownerControl.getShell().removeListener(SWT.Deactivate, _ownerShellListener);

				_currentArea = null;

				// keep current position
				_ttShellHideLocation = _ttShell.getLocation();

				passOnEvent(ttShell, event);

				ttShell.dispose();
				_ttShell = null;

				afterHideToolTip(event);
				return;
			}
		}

//		System.out.println("toolTipHide:NOhide\t");
// TODO remove SYSTEM.OUT.PRINTLN
	}

	private void toolTipOpen(final Shell shell, final Event event) {

		// Ensure that only one Tooltip is shown in time
		if (_ttShell != null) {
			toolTipHide(_ttShell, null);
		}

		_ttShell = shell;

		// close tooltip if user selects outside of the shell
		_ttShell.addListener(SWT.Deactivate, _ttShellListener);

		_ownerControl.getShell().addListener(SWT.Deactivate, _ownerShellListener);

		toolTipShow(event);
	}

	private void toolTipShow(final Event event) {

		if (!_ttShell.isDisposed()) {

			_currentArea = getToolTipArea(event);

			createToolTipContentArea(event, _ttShell);

			addTooltipListener(_ttShell);

			_ttShell.pack();

			setLocation(event);

			_ttShell.setVisible(true);
		}
	}
}
