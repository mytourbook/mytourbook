/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 *                                                 bugfix in: 195137, 198089, 225190
 *******************************************************************************/
package net.tourbook.common.util;

import java.util.HashMap;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ViewerCell;
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
 * This class gives implementors to provide customized tooltips for any control.
 * <p>
 * <b>This class is a copy from the ToolTip in org.eclipse.jface.window</b>, <br>
 * the method {@link TooltipHideListener} is adjusted
 * 
 * @since 3.3
 */
public abstract class ToolTip {

	/**
	 * Recreate the tooltip on every mouse move
	 */
	public static final int				RECREATE				= 1;

	/**
	 * Don't recreate the tooltip as long the mouse doesn't leave the area triggering the tooltip
	 * creation
	 */
	public static final int				NO_RECREATE				= 1 << 1;

	private Control						control;

	private int							xShift					= 3;
	private int							yShift					= 0;

	private int							popupDelay				= 0;
	private int							hideDelay				= 0;

	private ToolTipOwnerControlListener	listener;

	private HashMap<String, Object>		data;

	// Ensure that only one tooltip is active in time
//	private static Shell				CURRENT_TOOLTIP;
	private Shell						CURRENT_TOOLTIP;

	private TooltipHideListener			hideListener			= new TooltipHideListener();

	private Listener					shellListener;

	private boolean						isHideOnMouseDown		= true;
	private boolean						isRespectDisplayBounds	= true;
	private boolean						isRespectMonitorBounds	= true;

	private int							style;

	private Object						currentArea;

	private boolean						_isHideOnMouseMove;

	private class TooltipHideListener implements Listener {

		@Override
		public void handleEvent(final Event event) {

			if (event.widget instanceof Control) {

				final Control c = (Control) event.widget;
				final Shell shell = c.getShell();

				switch (event.type) {
				case SWT.MouseMove:

					if (_isHideOnMouseMove) {
						hide();
					}
					break;

				case SWT.MouseDown:
					if (isHideOnMouseDown()) {
						toolTipHide(shell, event);
					}
					break;

				case SWT.MouseExit:

					/*
					 * Give some insets to ensure we get exit informations from a wider area ;-)
					 */

					/**
					 * !!! this adjustment do not work on Linux because the tooltip gets hidden when
					 * the mouse tries to mover over the tooltip <br>
					 * <br>
					 * it seems to work on windows and linux with margin 1, when set to 0 the
					 * tooltip do sometime not be poped up again and the i-icons is not
					 * deaktivated<br>
					 * wolfgang 2010-07-23
					 */
					final Rectangle rect = shell.getBounds();
//					rect.x += 5;
//					rect.y += 5;
//					rect.width -= 10;
//					rect.height -= 10;
					final int margin = 1;
					rect.x += margin;
					rect.y += margin;
					rect.width -= 2 * margin;
					rect.height -= 2 * margin;

					final Point cursorLocation = c.getDisplay().getCursorLocation();
					if (!rect.contains(cursorLocation)) {
						toolTipHide(shell, event);
					}

					break;
				}
			}
		}
	}

	private class ToolTipOwnerControlListener implements Listener {

		@Override
		public void handleEvent(final Event event) {

			switch (event.type) {
			case SWT.Dispose:
			case SWT.KeyDown:
			case SWT.MouseDown:
			case SWT.MouseWheel:
				toolTipHide(getShell(), event);
				break;

			case SWT.MouseMove:
				toolTipCreate(event);
				break;

			case SWT.MouseExit:
				/*
				 * Check if the mouse exit happened because we move over the tooltip
				 */
				if (getShell() != null && !getShell().isDisposed()) {
					if (getShell().getBounds().contains(control.toDisplay(event.x, event.y))) {
						break;
					}
				}

				toolTipHide(getShell(), event);
				break;
			}
		}
	}

	/**
	 * Create new instance which add TooltipSupport to the widget
	 * 
	 * @param control
	 *            the control on whose action the tooltip is shown
	 */
	public ToolTip(final Control control) {
		this(control, RECREATE, false);
	}

	/**
	 * @param control
	 *            the control to which the tooltip is bound
	 * @param style
	 *            style passed to control tooltip behavior
	 * @param manualActivation
	 *            <code>true</code> if the activation is done manually using {@link #show(Point)}
	 * @see #RECREATE
	 * @see #NO_RECREATE
	 */
	public ToolTip(final Control control, final int style, final boolean manualActivation) {
		this.control = control;
		this.style = style;
		this.control.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent e) {
				data = null;
				deactivate();
			}

		});

		this.listener = new ToolTipOwnerControlListener();
		this.shellListener = new Listener() {
			@Override
			public void handleEvent(final Event event) {

				final Control ttControl = ToolTip.this.control;

				if (ttControl == null || ttControl.isDisposed()) {
					return;
				}

				ttControl.getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {

						// check again, NPE has occured during debugging

						if (ttControl == null || ttControl.isDisposed()) {
							return;
						}

						// Check if the new active shell is the tooltip itself
						if (ttControl.getDisplay().getActiveShell() != CURRENT_TOOLTIP) {
							toolTipHide(CURRENT_TOOLTIP, event);
						}
					}

				});
			}
		};

		if (!manualActivation) {
			activate();
		}
	}

	/**
	 * Activate tooltip support for this control
	 */
	public void activate() {

		deactivate();

		control.addListener(SWT.Dispose, listener);
		control.addListener(SWT.MouseMove, listener);
		control.addListener(SWT.MouseExit, listener);
		control.addListener(SWT.MouseDown, listener);
		control.addListener(SWT.MouseWheel, listener);
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

	/**
	 * Deactivate tooltip support for the underlying control
	 */
	public void deactivate() {

		control.removeListener(SWT.Dispose, listener);
		control.removeListener(SWT.MouseMove, listener);
		control.removeListener(SWT.MouseExit, listener);
		control.removeListener(SWT.MouseDown, listener);
		control.removeListener(SWT.MouseWheel, listener);
	}

	private Point fixupDisplayBounds(final Point tipSize, final Point location) {
		if (isRespectDisplayBounds || isRespectMonitorBounds) {
			Rectangle bounds;
			final Point rightBounds = new Point(tipSize.x + location.x, tipSize.y + location.y);

			final Monitor[] ms = control.getDisplay().getMonitors();

			if (isRespectMonitorBounds && ms.length > 1) {
				// By default present in the monitor of the control
				bounds = control.getMonitor().getBounds();
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
				bounds = control.getDisplay().getBounds();
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
	 * @param tipSize
	 * @param ttTopLeft
	 *            Top/left location for the hovered area relativ to the display
	 * @return
	 */
	protected Point fixupDisplayBoundsWithMonitor(final Point tipSize, final Point ttTopLeft) {

		Rectangle displayBounds;
		final Monitor[] allMonitors = control.getDisplay().getMonitors();

		if (allMonitors.length > 1) {

			// By default present in the monitor of the control
			displayBounds = control.getMonitor().getBounds();
			final Point topLeft2 = new Point(ttTopLeft.x, ttTopLeft.y);

			// Search on which monitor the event occurred
			Rectangle monitorBounds;
			for (final Monitor monitor : allMonitors) {
				monitorBounds = monitor.getBounds();
				if (monitorBounds.contains(topLeft2)) {
					displayBounds = monitorBounds;
					break;
				}
			}

		} else {
			displayBounds = control.getDisplay().getBounds();
		}

		final Point bottomRight = new Point(//
				ttTopLeft.x + tipSize.x,
				ttTopLeft.y + tipSize.y);

		if (!(displayBounds.contains(ttTopLeft) && displayBounds.contains(bottomRight))) {

			if (bottomRight.x > displayBounds.x + displayBounds.width) {
				ttTopLeft.x -= bottomRight.x - (displayBounds.x + displayBounds.width);
			}

			if (bottomRight.y > displayBounds.y + displayBounds.height) {
				ttTopLeft.y -= bottomRight.y - (displayBounds.y + displayBounds.height);
			}

			if (ttTopLeft.x < displayBounds.x) {
				ttTopLeft.x += displayBounds.x;
			}

			if (ttTopLeft.y < displayBounds.y) {
				ttTopLeft.y = displayBounds.y;
			}
		}

		return ttTopLeft;
	}

	/**
	 * Get the data restored under the key
	 * 
	 * @param key
	 *            the key
	 * @return data or <code>null</code> if no entry is restored under the key
	 */
	public Object getData(final String key) {
		if (data != null) {
			return data.get(key);
		}
		return null;
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
		return control.toDisplay(event.x + xShift, event.y + yShift);
	}

	/**
	 * @return Returns shell for this tooltip or <code>null</code> when tooltip is hidden.
	 */
	protected Shell getShell() {

		if (CURRENT_TOOLTIP == null || CURRENT_TOOLTIP.isDisposed()) {
			return null;
		}

		return CURRENT_TOOLTIP;
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
		return control;
	}

	/**
	 * Hide the currently active tool tip
	 */
	public void hide() {
		toolTipHide(CURRENT_TOOLTIP, null);
	}

	/**
	 * Return if hiding on mouse down is set.
	 * 
	 * @return <code>true</code> if hiding on mouse down in the tool tip is on
	 */
	public boolean isHideOnMouseDown() {
		return isHideOnMouseDown;
	}

	public boolean isHideOnMouseMove() {
		return _isHideOnMouseMove;
	}

	/**
	 * Return whether the tooltip respects bounds of the display.
	 * 
	 * @return <code>true</code> if the tooltip respects bounds of the display
	 */
	public boolean isRespectDisplayBounds() {
		return isRespectDisplayBounds;
	}

	/**
	 * Return whether the tooltip respects bounds of the monitor.
	 * 
	 * @return <code>true</code> if tooltip respects the bounds of the monitor
	 */
	public boolean isRespectMonitorBounds() {
		return isRespectMonitorBounds;
	}

	private void passOnEvent(final Shell tip, final Event event) {
		if (control != null
				&& !control.isDisposed()
				&& event != null
				&& event.widget != control
				&& event.type == SWT.MouseDown) {
			// the following was left in order to fix bug 298770 with minimal change. In 3.7, the complete method should be removed.
			tip.close();
		}
	}

	/**
	 * Restore arbitrary data under the given key
	 * 
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 */
	public void setData(final String key, final Object value) {
		if (data == null) {
			data = new HashMap();
		}
		data.put(key, value);
	}

	/**
	 * Set the hide delay.
	 * 
	 * @param hideDelay
	 *            the delay before the tooltip is hidden. If <code>0</code> the tooltip is shown
	 *            until user moves to other item
	 */
	public void setHideDelay(final int hideDelay) {
		this.hideDelay = hideDelay;
	}

	/**
	 * If you don't want the tool tip to be hidden when the user clicks inside the tool tip set this
	 * to <code>false</code>. You maybe also need to hide the tool tip yourself depending on what
	 * you do after clicking in the tooltip (e.g. if you open a new {@link Shell})
	 * 
	 * @param hideOnMouseDown
	 *            flag to indicate of tooltip is hidden automatically on mouse down inside the tool
	 *            tip
	 */
	public void setHideOnMouseDown(final boolean hideOnMouseDown) {
		// Only needed if there's currently a tooltip active
		if (CURRENT_TOOLTIP != null && !CURRENT_TOOLTIP.isDisposed()) {
			// Only change if value really changed
			if (hideOnMouseDown != this.isHideOnMouseDown) {
				control.getDisplay().syncExec(new Runnable() {

					@Override
					public void run() {
						if (CURRENT_TOOLTIP != null && CURRENT_TOOLTIP.isDisposed()) {
							toolTipHookByTypeRecursively(CURRENT_TOOLTIP, hideOnMouseDown, SWT.MouseDown);
						}
					}

				});
			}
		}

		this.isHideOnMouseDown = hideOnMouseDown;
	}

	public void setHideOnMouseMove(final boolean isHideOnMouseMove) {
		_isHideOnMouseMove = isHideOnMouseMove;
	}

	/**
	 * Set the popup delay.
	 * 
	 * @param popupDelay
	 *            the delay before the tooltip is shown to the user. If <code>0</code> the tooltip
	 *            is shown immediately
	 */
	public void setPopupDelay(final int popupDelay) {
		this.popupDelay = popupDelay;
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
		this.isRespectDisplayBounds = respectDisplayBounds;
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
		this.isRespectMonitorBounds = respectMonitorBounds;
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
		xShift = p.x;
		yShift = p.y;
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

		final boolean isNoReCreate = (style & NO_RECREATE) != 0;

		if (isNoReCreate) {

			final Object tmp = getToolTipArea(event);

			// No new area close the current tooltip
			if (tmp == null) {

				hide();

				return false;
			}

			final boolean isCurrentArea = tmp.equals(currentArea);

			return !isCurrentArea;
		}

		return true;
	}

	/**
	 * This method is called before the tooltip is hidden
	 * 
	 * @param event
	 *            the event trying to hide the tooltip
	 * @return <code>true</code> if the tooltip should be hidden
	 */
	private boolean shouldHideToolTip(final Event event) {

		final boolean isMouseMoveEvent = event != null && event.type == SWT.MouseMove;
		final boolean isNoReCreate = (style & NO_RECREATE) != 0;

		if (isMouseMoveEvent && isNoReCreate) {

			final Object tmp = getToolTipArea(event);

			// No new area close the current tooltip
			if (tmp == null) {

				hide();

				return false;
			}

			final boolean isInCurrentArea = tmp.equals(currentArea);

			return !isInCurrentArea;
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
		event.widget = control;
		toolTipCreate(event);
	}

	private Shell toolTipCreate(final Event event) {

		if (shouldCreateToolTip(event)) {

			final Shell shell = new Shell(control.getShell(), SWT.ON_TOP | SWT.TOOL | SWT.NO_FOCUS);
			shell.setLayout(new FillLayout());

			toolTipOpen(shell, event);

			return shell;
		}

		return null;
	}

	private void toolTipHide(final Shell tip, final Event event) {

		if (control != null && !control.isDisposed() && tip != null && !tip.isDisposed() && shouldHideToolTip(event)) {

			control.getShell().removeListener(SWT.Deactivate, shellListener);

			currentArea = null;
			passOnEvent(tip, event);
			tip.dispose();
			CURRENT_TOOLTIP = null;
			afterHideToolTip(event);
		}
	}

	private void toolTipHookBothRecursively(final Control c) {

		c.addListener(SWT.MouseMove, hideListener);
		c.addListener(SWT.MouseDown, hideListener);
		c.addListener(SWT.MouseExit, hideListener);

		if (c instanceof Composite) {
			final Control[] children = ((Composite) c).getChildren();
			for (final Control element : children) {
				toolTipHookBothRecursively(element);
			}
		}
	}

	private void toolTipHookByTypeRecursively(final Control c, final boolean add, final int type) {

		if (add) {
			c.addListener(type, hideListener);
		} else {
			c.removeListener(type, hideListener);
		}

		if (c instanceof Composite) {
			final Control[] children = ((Composite) c).getChildren();
			for (final Control element : children) {
				toolTipHookByTypeRecursively(element, add, type);
			}
		}
	}

	private void toolTipOpen(final Shell shell, final Event event) {
		// Ensure that only one Tooltip is shown in time
		if (CURRENT_TOOLTIP != null) {
			toolTipHide(CURRENT_TOOLTIP, null);
		}

		CURRENT_TOOLTIP = shell;

		control.getShell().addListener(SWT.Deactivate, shellListener);

		if (popupDelay > 0) {
			control.getDisplay().timerExec(popupDelay, new Runnable() {
				@Override
				public void run() {
					toolTipShow(shell, event);
				}
			});
		} else {
			toolTipShow(CURRENT_TOOLTIP, event);
		}

		if (hideDelay > 0) {
			control.getDisplay().timerExec(popupDelay + hideDelay, new Runnable() {

				@Override
				public void run() {
					toolTipHide(shell, null);
				}
			});
		}
	}

	private void toolTipShow(final Shell tip, final Event event) {
		if (!tip.isDisposed()) {
			currentArea = getToolTipArea(event);
			final Composite contentArea = createToolTipContentArea(event, tip);
			if (contentArea != null) {

				// don't show the tooltip when content area is not created

				if (isHideOnMouseDown()) {
					toolTipHookBothRecursively(tip);
				} else {
					toolTipHookByTypeRecursively(tip, true, SWT.MouseExit);
				}

				tip.pack();
				final Point size = tip.getSize();
				final Point location = fixupDisplayBounds(size, getLocation(size, event));

				// Need to adjust a bit more if the mouse cursor.y == tip.y and
				// the cursor.x is inside the tip
				final Point cursorLocation = tip.getDisplay().getCursorLocation();

				if (cursorLocation.y == location.y
						&& location.x < cursorLocation.x
						&& location.x + size.x > cursorLocation.x) {
					location.y -= 2;
				}

				tip.setLocation(location);
				tip.setVisible(true);
			}
		}
	}
}
