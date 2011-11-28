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

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

/**
 * Part of this tooltip is copied from {@link ToolTip}.
 */
public abstract class ToolTipVP {

	// Ensure that only one tooltip is active in time
	private static Shell	_ttShell;
	private Object			_currentArea;
	private Control			_control;

	private int				_xShift					= 3;
	private int				_yShift					= 0;

	private Listener		_shellListener;
	private TooltipListener	_ttListener				= new TooltipListener();
	private ControlListener	_controlListener;

	private boolean			_isRespectDisplayBounds	= true;
	private boolean			_isRespectMonitorBounds	= true;

	private class ControlListener implements Listener {
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
					if (_ttShell.getBounds().contains(_control.toDisplay(event.x, event.y))) {
						break;
					}
				}

//				toolTipHide(_ttShell, event);
				break;
			}
		}
	}

	private class TooltipListener implements Listener {

		public void handleEvent(final Event event) {

			if (event.widget instanceof Control) {

				final Control c = (Control) event.widget;
				final Shell shell = c.getShell();

				switch (event.type) {
				case SWT.MouseExit:
					/*
					 * Give some insets to ensure we get exit informations from a wider area ;-)
					 */
					/**
					 * !!! this adjustment do not work on Linux because the tooltip gets hidden when
					 * the mouse tries to mover over the tooltip <br>
					 * <br>
					 * it seems to work on windows and linux with margin 1, when set to 0 the
					 * tooltip do sometime not be poped up again and the i-icons is not deaktivated<br>
					 * wolfgang 2010-07-23
					 */
					final Rectangle shellRect = shell.getBounds();
//					rect.x += 5;
//					rect.y += 5;
//					rect.width -= 10;
//					rect.height -= 10;
					final int margin = 1;
					shellRect.x += margin;
					shellRect.y += margin;
					shellRect.width -= 2 * margin;
					shellRect.height -= 2 * margin;

					final Point cursorLocation = c.getDisplay().getCursorLocation();
					if (!shellRect.contains(cursorLocation)) {
						toolTipHide(shell, event);
					}

					break;
				}
			}
		}
	}

	/**
	 * Create new instance which add TooltipSupport to the widget
	 * 
	 * @param control
	 *            the control on whose action the tooltip is shown
	 */
	public ToolTipVP(final Control control) {

		_control = control;
		_control.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(final DisposeEvent e) {
				removeControlListener();
			}
		});

		_controlListener = new ControlListener();

		_shellListener = new Listener() {
			public void handleEvent(final Event event) {

				final Control thisControl = ToolTipVP.this._control;

				if (thisControl != null && !thisControl.isDisposed()) {

					thisControl.getDisplay().asyncExec(new Runnable() {

						public void run() {

							// hide tooltip when another shell is activated

							if (thisControl.getDisplay().getActiveShell() != _ttShell) {
								toolTipHide(_ttShell, event);
							}
						}
					});
				}
			}
		};

		addControlListener();
	}

	/**
	 * Activate tooltip support for this control
	 */
	public void addControlListener() {

		removeControlListener();

		_control.addListener(SWT.Dispose, _controlListener);
		_control.addListener(SWT.MouseHover, _controlListener);
		_control.addListener(SWT.MouseMove, _controlListener);
		_control.addListener(SWT.MouseExit, _controlListener);
		_control.addListener(SWT.MouseDown, _controlListener);
		_control.addListener(SWT.MouseWheel, _controlListener);
	}

	private void addShellListenerRecursively(final Control control, final boolean add, final int type) {

		if (add) {
			control.addListener(type, _ttListener);
		} else {
			control.removeListener(type, _ttListener);
		}

		if (control instanceof Composite) {
			final Control[] children = ((Composite) control).getChildren();
			for (final Control child : children) {
				addShellListenerRecursively(child, add, type);
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

			final Monitor[] ms = _control.getDisplay().getMonitors();

			if (_isRespectMonitorBounds && ms.length > 1) {
				// By default present in the monitor of the control
				bounds = _control.getMonitor().getBounds();
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
				bounds = _control.getDisplay().getBounds();
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
	public Point getLocation(final Point tipSize, final Event event) {
		return _control.toDisplay(event.x + _xShift, event.y + _yShift);
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
		return _control;
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

	private void passOnEvent(final Shell tip, final Event event) {
		if (_control != null
				&& !_control.isDisposed()
				&& event != null
				&& event.widget != _control
				&& event.type == SWT.MouseDown) {
			// the following was left in order to fix bug 298770 with minimal change. In 3.7, the complete method should be removed.
			tip.close();
		}
	}

	/**
	 * Deactivate tooltip support for the underlying control
	 */
	private void removeControlListener() {
		_control.removeListener(SWT.Dispose, _controlListener);
		_control.removeListener(SWT.MouseHover, _controlListener);
		_control.removeListener(SWT.MouseMove, _controlListener);
		_control.removeListener(SWT.MouseExit, _controlListener);
		_control.removeListener(SWT.MouseDown, _controlListener);
		_control.removeListener(SWT.MouseWheel, _controlListener);
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
		event.widget = _control;

		toolTipCreate(event);
	}

	private Shell toolTipCreate(final Event event) {

		if (shouldCreateToolTip(event)) {

			final Shell shell = new Shell(_control.getShell(), SWT.ON_TOP | SWT.TOOL | SWT.NO_FOCUS);

			shell.setLayout(new FillLayout());

			toolTipOpen(shell, event);

			return shell;
		}

		return null;
	}

	private void toolTipHide(final Shell ttShell, final Event event) {

		if (ttShell != null && !ttShell.isDisposed() && shouldHideToolTip(event)) {

			_control.getShell().removeListener(SWT.Deactivate, _shellListener);

			_currentArea = null;
			passOnEvent(ttShell, event);
			ttShell.dispose();
			_ttShell = null;

			afterHideToolTip(event);
		}
	}

	private void toolTipOpen(final Shell shell, final Event event) {

		// Ensure that only one Tooltip is shown in time
		if (_ttShell != null) {
			toolTipHide(_ttShell, null);
		}

		_ttShell = shell;

		_control.getShell().addListener(SWT.Deactivate, _shellListener);

		toolTipShow(_ttShell, event);
	}

	private void toolTipShow(final Shell ttShell, final Event event) {

		if (!ttShell.isDisposed()) {

			_currentArea = getToolTipArea(event);

			createToolTipContentArea(event, ttShell);

			addShellListenerRecursively(ttShell, true, SWT.MouseExit);

			ttShell.pack();
			final Point size = ttShell.getSize();
			final Point location = fixupDisplayBounds(size, getLocation(size, event));

			// Need to adjust a bit more if the mouse cursor.y == tip.y and
			// the cursor.x is inside the tip
			final Point cursorLocation = ttShell.getDisplay().getCursorLocation();

			if (cursorLocation.y == location.y
					&& location.x < cursorLocation.x
					&& location.x + size.x > cursorLocation.x) {
				location.y -= 2;
			}

			ttShell.setLocation(location);
			ttShell.setVisible(true);
		}
	}
}
