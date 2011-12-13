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

	private static final int		VALUE_POINT_OFFSET					= 20;
	private static final int		TOOL_TIP_SHELL_LOCATION_OFFSET		= 0;

	private static final String		STATE_VALUE_POINT_TOOLTIP_X			= "ValuePoint_ToolTip_DiffPositionX";				//$NON-NLS-1$
	private static final String		STATE_VALUE_POINT_TOOLTIP_Y			= "ValuePoint_ToolTip_DiffPositionY";				//$NON-NLS-1$
	private static final String		STATE_VALUE_POINT_PIN_LOCATION		= "ValuePoint_ToolTip_PinnedLocation";				//$NON-NLS-1$
	private static final String		STATE_MOUSE_X_POSITION_RELATIVE		= "ValuePoint_ToolTip_MouseXPositionRelative";		//$NON-NLS-1$
	private static final String		STATE_IS_TOOLTIP_ABOVE_VALUE_POINT	= "ValuePoint_ToolTip_IsToolTipAboveValuePoint";	//$NON-NLS-1$

	IDialogSettings					state;

	private ITooltipOwner			_tooltipOwner;

	// Ensure that only one tooltip is active in time
//	private static Shell			_ttShell;
	private Shell					_ttShell;

	private Object					_currentArea;
	private Control					_ownerControl;
	private OwnerShellListener		_ownerShellListener;

	private OwnerControlListener	_ownerControlListener;
	private TooltipListener			_ttListener							= new TooltipListener();
	private TooltipShellListener	_ttShellListener					= new TooltipShellListener();

	private boolean					_isTTDragged;

	private int						_devXTTMouseDown;
	private int						_devYTTMouseDown;
	private int						_devXOwnerMouseMove;
	private int						_devYOwnerMouseMove;

	/**
	 * Relative y position for the pinned location
	 * {@link ValuePointToolTipPinLocation#MouseXPosition}
	 */
	private int						_devPinnedMouseXPositionRelative;

	/**
	 * Is <code>true</code> when the tool tip location is above the value point in the chart.
	 */
	private boolean					_isTTAboveValuePoint				= true;

	/**
	 * Position where the hovered value is painted in the chart, the position is relative to the
	 * client.
	 */
	private Point					_ownerValueDevPosition				= new Point(0, 0);

	int								chartMarginTop;
	int								chartMarginBottom;

	/**
	 * Relative location for the tooltip shell to the pinned location when it's moved with the
	 * mouse.
	 */
	private Point					_ttShellDiff						= new Point(0, 0);

	private Point					_screenNotFixedTTShellLocation;
	private Point					_fixedTTShellLocation				= new Point(0, 0);

	ValuePointToolTipPinLocation	pinnedLocation;

	/*
	 * UI resources
	 */
	private Cursor					_cursorDragged;

	private Cursor					_cursorHand;
	private boolean					_isSetDefaultLocation				= true;
	private int						_edgeCoverOffset;
	private Display					_display;
	private Runnable				_ttShellPositioningRunnable;
	private int						_animationCounter;
	private int						_repeatTime;

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

					boolean isForwardEvent = false;

					switch (event.type) {
					case SWT.MouseMove:

						if (_isTTDragged) {
							onMoveTT(event);
						} else {

							/*
							 * move value point in the chart when tooltip is hovered and the mouse
							 * position is within the chart
							 */
							isForwardEvent = true;

							cursor = _cursorHand;
						}

						break;

					case SWT.MouseDown:

						_isTTDragged = true;

						_devXTTMouseDown = event.x;
						_devYTTMouseDown = event.y;

						cursor = _cursorDragged;

						break;

					case SWT.MouseUp:

						if (_isTTDragged) {

							_isTTDragged = false;

							onMouseUpTT(event);
						}

						cursor = _cursorHand;

						break;

					case SWT.MouseVerticalWheel:

						// pass to tt owner for zooming in/out
						isForwardEvent = true;

						break;

					case SWT.MouseEnter:

						isForwardEvent = true;

						break;

					case SWT.MouseExit:

						isForwardEvent = true;
						_isTTDragged = false;

						break;
					}

					if (isForwardEvent) {
						_tooltipOwner.handleMouseEvent(event, control.toDisplay(event.x, event.y));
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
		_display = _ownerControl.getDisplay();

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

		_ownerControl.getDisplay().timerExec(100, new Runnable() {
			public void run() {

			}
		});

		_ttShellPositioningRunnable = new Runnable() {
			public void run() {
				setTTShellLocation20Run();
			}
		};

		restoreState();
	}

	/**
	 * Pin the tooltip to a corder which is defined in PIN_LOCATION_...
	 * 
	 * @param locationId
	 */
	void actionPinLocation(final ValuePointToolTipPinLocation locationId) {

		// set new location
		pinnedLocation = locationId;

		if (_ttShell == null || _ttShell.isDisposed()) {
			return;
		}

		if (pinnedLocation == ValuePointToolTipPinLocation.Screen) {
			return;
		}

		if (pinnedLocation == ValuePointToolTipPinLocation.MouseXPosition) {
			// force default location
			_isSetDefaultLocation = true;
		}

		// display at pinned location without offset
		_ttShellDiff = new Point(0, 0);

		setTTShellLocation(false, true);
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
		control.addListener(SWT.MouseEnter, _ttListener);
		control.addListener(SWT.MouseExit, _ttListener);
		control.addListener(SWT.MouseVerticalWheel, _ttListener);

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

	/**
	 * @param tipSize
	 * @param originalLocation
	 * @return
	 */
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
		return pinnedLocation;
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

	Shell getToolTipShell() {
		return _ttShell;
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

	private void onMouseUpTT(final Event event) {

		/*
		 * get the tt vertical position
		 */
		// value point position
		final int devYValuePoint = _ownerControl.toDisplay(0, _ownerValueDevPosition.y).y;

		final Rectangle scrTTBounds = _ttShell.getBounds();
		final int srcTTTop = scrTTBounds.y;
		final int scrTTBottom = srcTTTop + scrTTBounds.height;

		_isTTAboveValuePoint = devYValuePoint > scrTTBottom;

		/*
		 * get tt relative position
		 */
		final Point scrOwnerControlLocation = _ownerControl.toDisplay(0, 0);
		final Point ownerSize = _ownerControl.getSize();
		final int scrOwnerTop = scrOwnerControlLocation.y;
		final int ownerHeight = ownerSize.y;
		final int srcOwnerBottom = scrOwnerTop + ownerHeight;

		if (_isTTAboveValuePoint) {
			_devPinnedMouseXPositionRelative = scrOwnerTop - scrTTBottom;
		} else {
			_devPinnedMouseXPositionRelative = srcOwnerBottom - srcTTTop;
		}

		setTTShellLocation(false, true);
	}

	/**
	 * The owner shell has been moved, adjust tooltip shell that it moves also with the owner
	 * control but preserves the display border.
	 */
	private void onMoveOwner(final Event event) {

		if (_ttShell == null || _ttShell.isDisposed()) {
			return;
		}

		setTTShellLocation(false, false);
	}

	/**
	 * Tooltip location has been moved with the mouse.
	 * 
	 * @param event
	 */
	private void onMoveTT(final Event event) {

		final int xDiff = event.x - _devXTTMouseDown;
		final int yDiff = event.y - _devYTTMouseDown;

		if (pinnedLocation == ValuePointToolTipPinLocation.Screen) {

			_ttShellDiff.x = xDiff;
			_ttShellDiff.y = yDiff;

		} else if (pinnedLocation == ValuePointToolTipPinLocation.MouseXPosition) {

			_ttShellDiff.x = 0;
			_ttShellDiff.y = yDiff;

		} else {

			_ttShellDiff.x += xDiff;
			_ttShellDiff.y += yDiff;
		}

		setTTShellLocation(true, false);
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

		setTTShellLocation(false, false);
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

	private void restoreState() {

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

		pinnedLocation = ValuePointToolTipPinLocation.valueOf(statePinnedLocation);

		_devPinnedMouseXPositionRelative = Util.getStateInt(state, STATE_MOUSE_X_POSITION_RELATIVE, 0);
		_isTTAboveValuePoint = Util.getStateBoolean(state, STATE_IS_TOOLTIP_ABOVE_VALUE_POINT, true);
	}

	void saveState() {

		state.put(STATE_VALUE_POINT_TOOLTIP_X, _ttShellDiff.x);
		state.put(STATE_VALUE_POINT_TOOLTIP_Y, _ttShellDiff.y);

		state.put(STATE_VALUE_POINT_PIN_LOCATION, pinnedLocation.name());
		state.put(STATE_MOUSE_X_POSITION_RELATIVE, _devPinnedMouseXPositionRelative);
		state.put(STATE_IS_TOOLTIP_ABOVE_VALUE_POINT, _isTTAboveValuePoint);
	}

	void setShellVisible(final boolean isVisible) {

		if (_ttShell == null || _ttShell.isDisposed()) {
			return;
		}

		_ttShell.setVisible(isVisible);
	}

	/**
	 * Set tooltip location according to the pinned location.
	 * 
	 * @param isTTDragged
	 *            is <code>true</code> when the tooltip is dragged
	 * @param isAnimation
	 */
	private void setTTShellLocation(final boolean isTTDragged, final boolean isAnimation) {

		final Point screenOwnerControlLocation = _ownerControl.toDisplay(0, 0);
		final Point ownerSize = _ownerControl.getSize();
		final int ownerWidth = ownerSize.x;
		final int ownerHeight = ownerSize.y;
		final int screenOwnerLeft = screenOwnerControlLocation.x;
		final int screenOwnerTop = screenOwnerControlLocation.y;
		final int screenOwnerBotton = screenOwnerTop + ownerHeight;

		final Point screenTTLocation = _fixedTTShellLocation;//_ttShell.getBounds();
		final Point ttSize = _ttShell.getSize();
		final int ttWidth = ttSize.x;
		final int ttHeight = ttSize.y;

		final int screenEdgeLeft = screenOwnerLeft + TOOL_TIP_SHELL_LOCATION_OFFSET;
		final int screenEdgeRight = screenOwnerLeft + ownerWidth - ttWidth - TOOL_TIP_SHELL_LOCATION_OFFSET;
		final int screenEdgeTop = screenOwnerTop + TOOL_TIP_SHELL_LOCATION_OFFSET + chartMarginTop;
		final int screenEdgeBottom = screenOwnerBotton - ttHeight - TOOL_TIP_SHELL_LOCATION_OFFSET - chartMarginBottom;

		final Point screenValuePoint = _ownerControl.toDisplay(_ownerValueDevPosition.x, _ownerValueDevPosition.y);
		final int screenValuePointTop = screenValuePoint.y;

		boolean isCheckCover = false;
		Point screenTTShellLocation = new Point(0, 0);

		switch (pinnedLocation) {
		case Screen:

			// tooltip is not pinned and must not be repositioned

			if (isTTDragged == false && _ttShell.isVisible()) {
				// tooltip is not moved and already visible -> nothing to do
				return;
			}

			// use default location when location was not yet set, center the tooltip in the center of the owner
			if (_isSetDefaultLocation) {

				_isSetDefaultLocation = false;

				screenTTShellLocation.x = screenOwnerLeft + (ownerWidth / 2) - (ttWidth / 2);
				screenTTShellLocation.y = screenOwnerTop + (ownerHeight / 2) - (ttHeight / 2);

			} else {
				screenTTShellLocation = _screenNotFixedTTShellLocation;
			}

			screenTTShellLocation.x = screenTTShellLocation.x + _ttShellDiff.x;
			screenTTShellLocation.y = screenTTShellLocation.y + _ttShellDiff.y;

			break;

		case MouseXPosition:

			if (isTTDragged) {

				// tooltip is currently dragged

				screenTTShellLocation.y = _screenNotFixedTTShellLocation.y + _ttShellDiff.y;

			} else {

				int screenTTDefaultY;

				if (_isTTAboveValuePoint) {
					screenTTDefaultY = screenOwnerTop - _devPinnedMouseXPositionRelative - ttHeight;
				} else {
					screenTTDefaultY = screenOwnerBotton - _devPinnedMouseXPositionRelative;
				}

				int devY = screenTTDefaultY;

				if (_isTTAboveValuePoint) {

					// tt must be above value point

					if (screenValuePointTop < (screenTTDefaultY + ttHeight + VALUE_POINT_OFFSET)) {
						// set above value point
						devY = screenValuePointTop - VALUE_POINT_OFFSET - ttHeight;
					}

				} else {

					// tt must be below value point

					if (screenValuePointTop > (screenTTDefaultY - VALUE_POINT_OFFSET)) {
						// set below value point
						devY = screenValuePointTop + VALUE_POINT_OFFSET;
					}
				}

				screenTTShellLocation.y = devY;
			}

			screenTTShellLocation.x = screenOwnerLeft - (ttWidth / 2) + _devXOwnerMouseMove;

			break;

		case TopLeft:
			screenTTShellLocation.x = screenEdgeLeft + _ttShellDiff.x;
			screenTTShellLocation.y = screenEdgeTop + _ttShellDiff.y;
			isCheckCover = true;
			break;

		case BottomLeft:
			screenTTShellLocation.x = screenEdgeLeft + _ttShellDiff.x;
			screenTTShellLocation.y = screenEdgeBottom + _ttShellDiff.y;
			isCheckCover = true;
			break;

		case BottomRight:
			screenTTShellLocation.x = screenEdgeRight + _ttShellDiff.x;
			screenTTShellLocation.y = screenEdgeBottom + _ttShellDiff.y;
			isCheckCover = true;
			break;

		case TopRight:
		default:
			screenTTShellLocation.x = screenEdgeRight + _ttShellDiff.x;
			screenTTShellLocation.y = screenEdgeTop + _ttShellDiff.y;
			isCheckCover = true;
			break;
		}

		if (isTTDragged == false && isCheckCover) {

			// check if the new location is covered by the tooltip

			// increase tooltip size
			final Rectangle scrTTNotMovedLocation = new Rectangle(//
					screenTTLocation.x - VALUE_POINT_OFFSET,
					screenTTLocation.y - VALUE_POINT_OFFSET + _edgeCoverOffset,
					ttSize.x + 2 * VALUE_POINT_OFFSET,
					ttSize.y + 2 * VALUE_POINT_OFFSET);

			// check if value point is hidden by the tooltip
			if (scrTTNotMovedLocation.contains(screenValuePoint)) {

				int screenUncoveredPos;
				switch (pinnedLocation) {
				case BottomLeft:
				case BottomRight:

					// show tooltip below the value point
					screenUncoveredPos = screenValuePointTop + VALUE_POINT_OFFSET;
					break;

				case TopLeft:
				case TopRight:
				default:
					// show tooltip above the value point
					screenUncoveredPos = screenValuePointTop - VALUE_POINT_OFFSET - ttHeight;
					break;
				}

				_edgeCoverOffset = screenTTShellLocation.y - screenUncoveredPos;

				screenTTShellLocation.y = screenUncoveredPos;

			} else {

				// reset, very important !!!
				_edgeCoverOffset = 0;
			}
		}

		_screenNotFixedTTShellLocation = screenTTShellLocation;

		final Point newFixedTTShellLocation = fixupDisplayBounds(ttSize, screenTTShellLocation);

		if (isTTDragged || isAnimation == false) {
			// no animation
			_ttShell.setLocation(newFixedTTShellLocation);
		} else {
			setTTShellLocation10Start(newFixedTTShellLocation);
		}
	}

	/**
	 * Move tooltip according to the mouse position.
	 * 
	 * @param devXMouseMove
	 * @param devYMouseMove
	 * @param valueDevPosition
	 */
	void setTTShellLocation(final int devXMouseMove, final int devYMouseMove, final Point valueDevPosition) {

		_devXOwnerMouseMove = devXMouseMove;
		_devYOwnerMouseMove = devYMouseMove;

		if (valueDevPosition == null) {
			_ownerValueDevPosition = new Point(0, 0);
		} else {
			_ownerValueDevPosition = valueDevPosition;
		}

		setTTShellLocation(false, true);
	}

	private synchronized void setTTShellLocation10Start(final Point newLocation) {

		final int oldCounter = _animationCounter;

		_animationCounter = 8;
		_repeatTime = 30;

		_fixedTTShellLocation = newLocation;

		// check if animation is already running
		if (oldCounter == 0) {


			// animation is not running, start a new animantion
			_display.syncExec(_ttShellPositioningRunnable);

		} else {

			// do the first movement
			setTTShellLocation20Run();
		}
	}

	private void setTTShellLocation20Run() {

		if (_animationCounter == 0 || _ownerControl.isDisposed() || _ttShell == null || _ttShell.isDisposed()) {
			return;
		}

		Point nextLocation = null;
		final Point currentLocation = _ttShell.getLocation();

		if (_animationCounter == 1) {

			// this is the last movement, move to the desired location

			nextLocation = _fixedTTShellLocation;

		} else {

			// animate movement

			final Point newLocation = _fixedTTShellLocation;

			final float diffX = currentLocation.x - newLocation.x;
			final float diffY = currentLocation.y - newLocation.y;

			final float stepX = diffX / _animationCounter;
			final float stepY = diffY / _animationCounter;

			final float devXRemainder = stepX * (_animationCounter - 1);
			final float devYRemainder = stepY * (_animationCounter - 1);

			final float devX = newLocation.x + devXRemainder;
			final float devY = newLocation.y + devYRemainder;

			nextLocation = new Point((int) devX, (int) devY);
		}

		_ttShell.setLocation(nextLocation);

		_animationCounter--;

		if (_animationCounter > 0) {
			// start new animation
			_display.timerExec(_repeatTime, _ttShellPositioningRunnable);
		}
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
							| SWT.NO_TRIM
			//
			);

			shell.setLayout(new FillLayout());

			toolTipOpen(shell, event);
		}
	}

	private void toolTipHide(final Shell ttShell, final Event event) {

		// initialize next animation, otherwise tooltip would never be displayed again
		_animationCounter = 0;

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
	};

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

			setTTShellLocation(false, false);

//			// prevent to open tool tips when the owner is not the active shell
//			final boolean isFocusControl = _ownerControl.getDisplay().getActiveShell() == _ownerControl.getShell();
//			System.out.println("isFocusControl\t" + isFocusControl);
//			// TODO remove SYSTEM.OUT.PRINTLN
//
//			if (isFocusControl) {
			_ttShell.setVisible(true);
//			}
		}
	}
}
