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
import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
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
public abstract class ValuePointToolTipShell {

	private static final int				TOOL_TIP_SHELL_LOCATION_OFFSET	= 0;									//10;

	private static final String				STATE_VALUE_POINT_TOOLTIP_X		= "ValuePoint_ToolTip_DiffPositionX";	//$NON-NLS-1$
	private static final String				STATE_VALUE_POINT_TOOLTIP_Y		= "ValuePoint_ToolTip_DiffPositionY";	//$NON-NLS-1$
	private static final String				STATE_VALUE_POINT_PIN_LOCATION	= "ValuePoint_ToolTip_PinnedLocation";	//$NON-NLS-1$

	IDialogSettings							state;

	private ITooltipOwner					_tooltipOwner;

	// Ensure that only one tooltip is active in time
	private static Shell					_ttShell;
	private Object							_currentArea;
	private Control							_ownerControl;

	private OwnerShellListener				_ownerShellListener;
	private OwnerControlListener			_ownerControlListener;
	private TooltipListener					_ttListener						= new TooltipListener();
	private TooltipShellListener			_ttShellListener				= new TooltipShellListener();

	private boolean							_isTTMouseDown;

	private int								_devXMouseDown;
	private int								_devYMouseDown;

	int										marginTop;
	int										marginBottom;

	/**
	 * Relative location for the tooltip shell to the pinned location when it's moved with the
	 * mouse.
	 */
	private Point							_ttShellDiff					= new Point(0, 0);

	private Point							_ttShellLocation;
	private ValuePointToolTipPinLocation	_pinnedLocation;

	/*
	 * UI resources
	 */
	private Cursor							_cursorDragged;
	private Cursor							_cursorHand;
	private boolean							_isDefaultLocationSet;

	private class OwnerControlListener implements Listener {
		public void handleEvent(final Event event) {

			switch (event.type) {
			case SWT.Dispose:
				toolTipHide(_ttShell, event);
				break;

			case SWT.Resize:
				onResizeOwner(event);
			}
		}
	}

	private final class OwnerShellListener implements Listener {
		public void handleEvent(final Event event) {

			if (_ownerControl == null || _ownerControl.isDisposed()) {
				return;
			}

			switch (event.type) {
			case SWT.Deactivate:

				_ownerControl.getDisplay().asyncExec(new Runnable() {

					public void run() {

						// hide tooltip when another shell is activated

						if (_ownerControl.getDisplay().getActiveShell() != _ttShell) {
							toolTipHide(_ttShell, event);
						}
					}
				});
				break;

			case SWT.Move:
				onMoveOwner(event);
				break;
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
							onMoveTT(event);
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
	 * @param state
	 * @param control
	 *            the control on whose action the tooltip is shown
	 */
	public ValuePointToolTipShell(final ITooltipOwner tooltipOwner, final IDialogSettings state) {

		_tooltipOwner = tooltipOwner;
		_ownerControl = tooltipOwner.getControl();

		this.state = state;

		_ownerControl.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		_ownerControlListener = new OwnerControlListener();
		_ownerShellListener = new OwnerShellListener();

		addOwnerControlListener();

		_cursorDragged = new Cursor(_ownerControl.getDisplay(), SWT.CURSOR_SIZEALL);
		_cursorHand = new Cursor(_ownerControl.getDisplay(), SWT.CURSOR_HAND);
	}

	/**
	 * Pin the tooltip to a corder which is defined in PIN_LOCATION_...
	 * 
	 * @param locationId
	 */
	void actionPinLocation(final ValuePointToolTipPinLocation locationId) {

		// set new location
		_pinnedLocation = locationId;

		if (_ttShell == null || _ttShell.isDisposed()) {
			return;
		}

		if (_pinnedLocation == ValuePointToolTipPinLocation.Disabled) {
			return;
		}

		// display at pinned location without offset
		_ttShellDiff = new Point(0, 0);

		setTTShellLocation(false);
	}

	/**
	 * Activate tooltip support for this control
	 */
	private void addOwnerControlListener() {

		removeOwnerControlListener();

		_ownerControl.addListener(SWT.Dispose, _ownerControlListener);
		_ownerControl.addListener(SWT.Resize, _ownerControlListener);
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
	 * Creates the content area of the the tooltip.
	 * 
	 * @param event
	 *            the event that triggered the activation of the tooltip
	 * @param parent
	 *            the parent of the content area
	 * @return the content area created
	 */
	protected abstract Composite createToolTipContentArea(Event event, Composite parent);

	private Point fixupDisplayBounds(final Point tipSize, final Point originalLocation) {

		// create a copy that the original value is not modified
		final Point location = new Point(originalLocation.x, originalLocation.y);

		Rectangle bounds;
		final Point rightBounds = new Point(tipSize.x + location.x, tipSize.y + location.y);

		final Monitor[] monitors = _ownerControl.getDisplay().getMonitors();

		if (monitors.length > 1) {
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

		return location;
	}

	ValuePointToolTipPinLocation getPinnedLocation() {
		return _pinnedLocation;
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

	void onDispose() {

		_cursorDragged = (Cursor) Util.disposeResource(_cursorDragged);
		_cursorHand = (Cursor) Util.disposeResource(_cursorHand);

		removeOwnerControlListener();
	}

	/**
	 * The wwner shell has been moved, adjust tooltip shell that it moves also with the owner
	 * control but preserves the display border.
	 */
	private void onMoveOwner(final Event event) {

		if (_ttShell == null || _ttShell.isDisposed()) {
			return;
		}

		setTTShellLocation(false);
	}

	/**
	 * Tooltip location has been moved with the mouse.
	 * 
	 * @param event
	 */
	private void onMoveTT(final Event event) {

		final int xDiff = event.x - _devXMouseDown;
		final int yDiff = event.y - _devYMouseDown;

		if (_pinnedLocation == ValuePointToolTipPinLocation.Disabled) {
			_ttShellDiff.x = xDiff;
			_ttShellDiff.y = yDiff;
		} else {

			_ttShellDiff.x += xDiff;
			_ttShellDiff.y += yDiff;
		}

		setTTShellLocation(true);
	}

	/**
	 * Owner control is resized.
	 * 
	 * @param event
	 */
	private void onResizeOwner(final Event event) {

		if (_ttShell == null || _ttShell.isDisposed()) {
			return;
		}

		setTTShellLocation(false);
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
	private void removeOwnerControlListener() {

		_ownerControl.removeListener(SWT.Dispose, _ownerControlListener);
		_ownerControl.removeListener(SWT.Resize, _ownerControlListener);
	}

	void restoreState() {

		/*
		 * restore value point tooltip location, when location is not set, don't set it to 0,0
		 * instead use tooltip default location which is positioning the tooltip into the center of
		 * the chart
		 */
		if (state.get(STATE_VALUE_POINT_TOOLTIP_X) != null) {

			final Point ttShellDiff = new Point(0, 0);

			ttShellDiff.x = Util.getStateInt(state, STATE_VALUE_POINT_TOOLTIP_X, 0);
			ttShellDiff.y = Util.getStateInt(state, STATE_VALUE_POINT_TOOLTIP_Y, 0);

			_ttShellDiff = ttShellDiff;
		}

		// tooltip orientation
		final String statePinnedLocation = Util.getStateString(
				state,
				STATE_VALUE_POINT_PIN_LOCATION,
				ValuePointToolTipPinLocation.TopRight.name());

		_pinnedLocation = ValuePointToolTipPinLocation.valueOf(statePinnedLocation);

	}

	void saveState() {

		// keep value point tooltip location
		state.put(STATE_VALUE_POINT_TOOLTIP_X, _ttShellDiff.x);
		state.put(STATE_VALUE_POINT_TOOLTIP_Y, _ttShellDiff.y);

		state.put(STATE_VALUE_POINT_PIN_LOCATION, _pinnedLocation.name());
	}

	void setShellVisible(final boolean isVisible) {

		if (_ttShell == null || _ttShell.isDisposed()) {
			return;
		}

		_ttShell.setVisible(isVisible);
	}

	/**
	 * set tooltip location into the requested corner
	 * 
	 * @param isTTMoved
	 */
	private void setTTShellLocation(final boolean isTTMoved) {

		final Point ownerControlLocation = _ownerControl.toDisplay(0, 0);
		final Point ownerSize = _ownerControl.getSize();

		final Rectangle ttBounds = _ttShell.getBounds();
		final Point ttSize = new Point(ttBounds.width, ttBounds.height);

		final int devXLeft = ownerControlLocation.x + TOOL_TIP_SHELL_LOCATION_OFFSET;
		final int devXRight = ownerControlLocation.x + ownerSize.x - ttSize.x - TOOL_TIP_SHELL_LOCATION_OFFSET;
		final int devYTop = ownerControlLocation.y + TOOL_TIP_SHELL_LOCATION_OFFSET + marginTop;
		final int devYBottom = ownerControlLocation.y
				+ ownerSize.y
				- ttSize.y
				- TOOL_TIP_SHELL_LOCATION_OFFSET
				- marginBottom;

		boolean isAdjustShell = true;
		Point ttShellLocation = new Point(0, 0);

		switch (_pinnedLocation) {
		case Disabled:

			// tooltip is not pinned and must not be repositioned

			if (isTTMoved == false && _ttShell.isVisible()) {
				// tooltip is not moved and already visible -> nothing to do
				return;
			}

			// use default location when location was not yet set, center the tooltip in the center of the owner
			if (_isDefaultLocationSet == false) {

				_isDefaultLocationSet = true;

				ttShellLocation.x = ownerControlLocation.x + ownerSize.x / 2 - ttSize.x / 2;
				ttShellLocation.y = ownerControlLocation.y + ownerSize.y / 2 - ttSize.y / 2;

			} else {
				ttShellLocation = _ttShellLocation;
			}

			isAdjustShell = false;

			break;

		case TopLeft:
			ttShellLocation.x = devXLeft;
			ttShellLocation.y = devYTop;
			break;

		case BottomLeft:
			ttShellLocation.x = devXLeft;
			ttShellLocation.y = devYBottom;
			break;

		case BottomRight:
			ttShellLocation.x = devXRight;
			ttShellLocation.y = devYBottom;
			break;

		case TopRight:
		default:
			ttShellLocation.x = devXRight;
			ttShellLocation.y = devYTop;
			break;
		}

		if (isAdjustShell || isTTMoved) {

			// adjust to manually moved location
			ttShellLocation.x += _ttShellDiff.x;
			ttShellLocation.y += _ttShellDiff.y;
		}

		_ttShellLocation = ttShellLocation;

		_ttShell.setLocation(fixupDisplayBounds(ttSize, ttShellLocation));
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

			final Shell shell = new Shell(_ownerControl.getShell(), //
					SWT.ON_TOP //
//							| SWT.TOOL
							| SWT.NO_FOCUS
//							| SWT.NO_TRIM
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

			final Shell ownerShell = _ownerControl.getShell();
			ownerShell.removeListener(SWT.Deactivate, _ownerShellListener);
			ownerShell.removeListener(SWT.Move, _ownerShellListener);

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

		final Shell ownerShell = _ownerControl.getShell();
		ownerShell.addListener(SWT.Deactivate, _ownerShellListener);
		ownerShell.addListener(SWT.Move, _ownerShellListener);

		toolTipShow(event);
	}

	private void toolTipShow(final Event event) {

		if (!_ttShell.isDisposed()) {

			_currentArea = getToolTipArea(event);

			createToolTipContentArea(event, _ttShell);

			addTooltipListener(_ttShell);

			_ttShell.pack();

			setTTShellLocation(false);

			_ttShell.setVisible(true);
		}
	}
}
