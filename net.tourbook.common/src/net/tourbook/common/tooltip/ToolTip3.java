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
package net.tourbook.common.tooltip;

import java.util.HashMap;

import net.tourbook.common.UI;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
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
import org.eclipse.swt.widgets.Widget;

/**
 * <p>
 * Part of this tooltip is copied from org.eclipse.jface.window.ToolTip
 */
public abstract class ToolTip3 {

	public static final int				TOOLTIP_STYLE_RECREATE_CONTENT			= 0;
	public static final int				TOOLTIP_STYLE_KEEP_CONTENT				= 1;

	public static final int				MOUSE_OVER_BEHAVIOUR_NO_IGNORE			= 0;
	public static final int				MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER		= 1;

	/**
	 * how long each tick is when fading in/out (in ms)
	 */
	private static final int			FADE_TIME_INTERVAL						= UI.IS_OSX ? 10 : 10;

	/**
	 * Number of steps when fading in
	 */
	private static final int			FADE_IN_STEPS							= 20;

	/**
	 * Number of steps when fading out
	 */
	private static final int			FADE_OUT_STEPS							= 10;

	/**
	 * Number of steps before fading out
	 */
	private static final int			FADE_OUT_DELAY_STEPS					= 20;

	private static final int			MOVE_STEPS								= 20;

	private static final int			ALPHA_OPAQUE							= 0xff;

	private static final int			MOUSE_HOVER_DELAY						= 1;
	private static final int			AUTO_CLOSE_INTERVAL						= 700;

	private OwnerControlListener		_ownerControlListener;
	private OwnerShellListener			_ownerShellListener;
	private ToolTipShellListener		_ttShellListener;
	private ToolTipAllControlsListener	_ttAllControlsListener;
	private DisplayFilterListener		_displayFilterListener;

	/**
	 * Keep track of added display listener that no more than <b>1</b> is set.
	 */
	private boolean						_isDisplayListenerSet;

	/**
	 * Is <code>true</code> when shell is fading out, otherwise <code>false</code>.
	 */
	private boolean						_isShellFadingOut;

	/**
	 * Is <code>true</code> when shell is fading in, otherwise <code>false</code>.
	 */
	private boolean						_isShellFadingIn;

	private Point						_shellStartLocation;
	private Point						_shellEndLocation						= new Point(0, 0);

	private int							_fadeOutDelayCounter;

	private final AnimationTimer		_animationTimer;
	private int							_animationMoveCounter;

	private boolean						_isReceiveOnMouseMove;

	private int							_fadeInSteps							= FADE_IN_STEPS;

	private int							_mouseOverBehaviour						= MOUSE_OVER_BEHAVIOUR_NO_IGNORE;
	private boolean						_isKeepToolTipOpenWhenResizedOrMoved	= true;

	/*
	 * UI resources
	 */
	private Display						_display;

	/**
	 * Tooltip shell which is currently be visible
	 */
//	private Shell						_shell;

	private Control						_ownerControl;

	private OwnerHoverTimer				_ownerHoverTimer;
	private Point						_ownerHoverPosition;
	private ToolTipAutoCloseTimer		_ttAutoCloseTimer;

	/**
	 * Is <code>true</code> when the owner control is hovered with the mouse.
	 */
	private boolean						_isOwnerHovered;

	private HashMap<Object, Shell>		_allHoveredShells						= new HashMap<Object, Shell>();

	/**
	 * Tooltip area for the {@link #_activeShell}, is be <code>null</code> when a tooltip is not
	 * visible.
	 */
	private Object						_activeToolTipArea;
	private Shell						_activeShell;

	private final class AnimationTimer implements Runnable {
		@Override
		public void run() {
			animation20_Runnable();
		}
	}

	private class DisplayFilterListener implements Listener {
		public void handleEvent(final Event event) {

			switch (event.type) {
			case SWT.MouseMove:
				onDisplayMouseMove();
				break;
			}
		}
	}

	private class OwnerControlListener implements Listener {
		public void handleEvent(final Event event) {
			onOwnerControlEvent(event);
		}
	}

	/**
	 * This works like the {@link SWT#MouseHover} event but the time until a tooltip is opened is
	 * tooooo slow in the original implementation. In this implementation the open delay can be
	 * customized.
	 */
	private final class OwnerHoverTimer implements Runnable {
		@Override
		public void run() {
			onOwnerHovered();
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
	private class ToolTipAllControlsListener implements Listener {
		public void handleEvent(final Event event) {
			onTTControlEvent(event);
		}

	}

	/**
	 * This checks if a mouse is above a tooltip. When mouse is above the 3D map or outside of this
	 * applicate, mouse events are not captured and a tooltip keeps opend until other actions are
	 * done.
	 */
	private final class ToolTipAutoCloseTimer implements Runnable {
		@Override
		public void run() {
			onTTAutoCloseTimer();
		}
	}

	private final class ToolTipShellListener implements Listener {
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
	public ToolTip3(final Control ownerControl) {

		_ownerControl = ownerControl;
		_display = _ownerControl.getDisplay();

		_ttAllControlsListener = new ToolTipAllControlsListener();
		_ttShellListener = new ToolTipShellListener();
		_displayFilterListener = new DisplayFilterListener();

		_ownerControlListener = new OwnerControlListener();
		_ownerShellListener = new OwnerShellListener();

		_animationTimer = new AnimationTimer();
		_ownerHoverTimer = new OwnerHoverTimer();
		_ttAutoCloseTimer = new ToolTipAutoCloseTimer();

		addOwnerControlListener();
		addOwnerShellListener();
	}

	private void addDisplayFilterListener() {

		if (_isDisplayListenerSet == false) {

			_display.addFilter(SWT.MouseMove, _displayFilterListener);

			_isDisplayListenerSet = true;
		}
	}

	/**
	 * Activate tooltip support for this control
	 */
	private void addOwnerControlListener() {

		removeOwnerControlsListener();

		_ownerControl.addListener(SWT.Dispose, _ownerControlListener);
		_ownerControl.addListener(SWT.MouseExit, _ownerControlListener);
		_ownerControl.addListener(SWT.MouseMove, _ownerControlListener);
		_ownerControl.addListener(SWT.Resize, _ownerControlListener);
	}

	private void addOwnerShellListener() {

		final Shell ownerShell = _ownerControl.getShell();

		ownerShell.addListener(SWT.Deactivate, _ownerShellListener);
		ownerShell.addListener(SWT.Move, _ownerShellListener);
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
	private void addTTAllControlsListener(final Control control) {

		control.addListener(SWT.KeyDown, _ttAllControlsListener);

		control.addListener(SWT.MouseDown, _ttAllControlsListener);
		control.addListener(SWT.MouseUp, _ttAllControlsListener);
		control.addListener(SWT.MouseMove, _ttAllControlsListener);
		control.addListener(SWT.MouseExit, _ttAllControlsListener);
		control.addListener(SWT.MouseEnter, _ttAllControlsListener);

		if (control instanceof Composite) {
			final Control[] children = ((Composite) control).getChildren();
			for (final Control child : children) {
				addTTAllControlsListener(child);
			}
		}
	}

	private void addTTShellListener(final Shell shell) {

		// hide tooltip if user selects outside of the shell

		shell.addListener(SWT.Deactivate, _ttShellListener);
		shell.addListener(SWT.Dispose, _ttShellListener);
	}

	private void animation10_Start() {

		final int a = 1;

//		if (a == 1) {
		animation10_Start_Simple();
//		} else {
//			animation10_StartKomplex();
//		}
	}

	private void animation10_Start_Simple() {

		if (_isShellFadingIn) {

			// show tool tip

			final Point size = _activeShell.getSize();
			final Point defaultLocation = getToolTipLocation(size, _ownerHoverPosition);

			final Point shellLocation = fixupDisplayBounds(size, defaultLocation);

			_activeShell.setLocation(shellLocation.x, shellLocation.y);
			_activeShell.setAlpha(0xff);

			setShellVisible(true);

		} else {

			// hide tooltip

			setShellVisible(false);
		}

		// close previous shells
		close(false);

		// keep current shell
		_allHoveredShells.put(_activeToolTipArea, _activeShell);

	}

	private void animation20_Runnable() {

//		final long start = System.nanoTime();

//		if (_shell == null || _shell.isDisposed() || _shell.isVisible() == false) {
//			return;
//		}
//
//		try {
//			/*
//			 * endAlpha will be the final fadeIn/fadeOut value when the animation stops
//			 */
//			int finalFadeAlpha = -1;
//
//			int currentAlpha = _shell.getAlpha();
//			boolean isLoopBreak = false;
//
//			_animationMoveCounter++;
//
//			while (true) {
//
//				int newAlpha = -1;
//
//				if (_isShellFadingIn) {
//
//					final int shellStartX = _shellStartLocation.x;
//					final int shellStartY = _shellStartLocation.y;
//					final int shellEndX = _shellEndLocation.x;
//					final int shellEndY = _shellEndLocation.y;
//
//					final Point shellCurrentLocation = _shell.getLocation();
//
//					final boolean isInTarget = shellCurrentLocation.x == shellEndX
//							&& shellCurrentLocation.y == shellEndY;
//
//					final int diffAlpha = ALPHA_OPAQUE / _fadeInSteps;
//
//					newAlpha = currentAlpha + diffAlpha;
//					if (newAlpha > ALPHA_OPAQUE) {
//						newAlpha = ALPHA_OPAQUE;
//					}
//					finalFadeAlpha = ALPHA_OPAQUE;
//
//					if (isInTarget && currentAlpha == ALPHA_OPAQUE) {
//
//						// target is reached and fully visible, stop animation
//
//						_isShellFadingIn = false;
//
//						return;
//
//					} else {
//
//						if (isInTarget == false) {
//
//							// move to target
//
//							final int diffX = shellStartX - shellEndX;
//							final int diffY = shellStartY - shellEndY;
//
//							final double moveX = (double) diffX / MOVE_STEPS * _animationMoveCounter;
//							final double moveY = (double) diffY / MOVE_STEPS * _animationMoveCounter;
//
//							final int shellCurrentX = (int) (shellStartX - moveX);
//							final int shellCurrentY = (int) (shellStartY - moveY);
//
//							_shell.setLocation(shellCurrentX, shellCurrentY);
//						}
//					}
//
//				} else if (_isShellFadingOut) {
//
//					if (_fadeOutDelayCounter++ < FADE_OUT_DELAY_STEPS) {
//
//						// delay fade out
//
//						_display.timerExec(FADE_TIME_INTERVAL, _animationTimer);
//
//						return;
//					}
//
//					final int alphaDiff = ALPHA_OPAQUE / FADE_OUT_STEPS;
//
//					newAlpha = currentAlpha - alphaDiff;
//					finalFadeAlpha = 0;
//
//					if (newAlpha <= 0) {
//
//						// shell is not visible any more, hide it now
//
//						_shell.setAlpha(0);
//
//						// hide shell
//						setShellVisible(false);
//
//						_isShellFadingOut = false;
//
//						return;
//					}
//				}
//
//				if (newAlpha == -1) {
//
//					return;
//
//				} else {
//
//					if (newAlpha != currentAlpha) {
//						_shell.setAlpha(newAlpha);
//					}
//
//					if (_shell.getAlpha() != newAlpha) {
//
//						// platform do not support shell alpha, this occured on Ubuntu 12.04
//
//						if (isLoopBreak) {
//							break;
//						}
//
//						// loop only once
//						isLoopBreak = true;
//
//						currentAlpha = finalFadeAlpha;
//
//						continue;
//
//					} else {
//
//						_display.timerExec(FADE_TIME_INTERVAL, _animationTimer);
//
//						break;
//					}
//
//				}
//			}
//
//		} catch (final Exception err) {
//			StatusUtil.log(err);
//		} finally {
//
////			final float timeDiff = (float) (System.nanoTime() - start) / 1000000;
////			System.out.println(UI.timeStampNano() + " animation20_Runnable:\t" + timeDiff + " ms\t" + " ms");
////			// TODO remove SYSTEM.OUT.PRINTLN
//		}
	}

//
//	private void animation10_StartKomplex() {
//
////		final long start = System.nanoTime();
//
//		if (_isShellFadingIn) {
//
//			// set fading in location
//
//			final Point shellSize = _shell.getSize();
//			Point shellEndLocation = getToolTipLocation(shellSize);
//			shellEndLocation = fixupDisplayBounds(shellSize, shellEndLocation);
//
//			final boolean isShellVisible = _shell.isVisible();
//
//			if (shellEndLocation.x == _shellEndLocation.x
//					&& shellEndLocation.y == _shellEndLocation.y
//					&& isShellVisible) {
//
//				// shell is already fading in with the correct location
//
//				return;
//			}
//
//			// set new end location
//			_shellEndLocation = shellEndLocation;
//
//			if (isShellVisible) {
//
//				// shell is already visible, move from the current position to the target position
//
//				_shellStartLocation = _shell.getLocation();
//
//			} else {
//
//				// shell is not visible, set position directly without moving animation, do only fading animation
//
//				_shellStartLocation = _shellEndLocation;
//
//				_shell.setLocation(_shellStartLocation.x, _shellStartLocation.y);
//
//				_shell.setAlpha(0);
//
//				setShellVisible(true);
//			}
//
//		} else if (_isShellFadingOut) {
//
//			// fading out has no movement
//
//			_fadeOutDelayCounter = 0;
//		}
//
//		// start animation now
//		_animationMoveCounter = 0;
//		animation20_Runnable();
//
////		System.out.println(UI.timeStampNano()
////				+ " animation10_StartKomplex\t"
////				+ ((float) (System.nanoTime() - start) / 1000000)
////				+ " ms");
////		// TODO remove SYSTEM.OUT.PRINTLN
//	}

	/**
	 * Is called before the tooltip shell is set to hidden.
	 */
	protected void beforeHideToolTip() {}

	/**
	 * Close tooltip immediatedly without animation.
	 */
	private void close(final boolean isCloseFinal) {

//		System.out.println(UI.timeStampNano() + " close\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (isCloseFinal) {

			// deactivate auto close timer
			_display.timerExec(-1, _ttAutoCloseTimer);
		}

		removeDisplayFilterListener();

		for (final Shell shell : _allHoveredShells.values()) {

			if (shell != null && !shell.isDisposed()) {
				shell.close();
			}
		}

		_allHoveredShells.clear();
	}

	/**
	 * Creates the content area of the the tooltip.
	 * 
	 * @param parent
	 *            the parent of the content area
	 * @return the content area created
	 */
	protected abstract Composite createToolTipContentArea(Composite parent);

	/**
	 * Create a shell but do not display it
	 * 
	 * @param toolTipArea
	 * @return Returns <code>true</code> when shell is created.
	 */
	private void createUI(final Object toolTipArea) {

		// create shell
		_activeShell = new Shell(_ownerControl.getShell(), //
				SWT.ON_TOP //
						/*
						 * SWT.TOOL must be disabled that NO_FOCUS is working !!!
						 */
//						| SWT.TOOL
						| SWT.NO_FOCUS);

		_activeShell.setLayout(new FillLayout());

		// create content
		createToolTipContentArea(_activeShell);

		_activeShell.pack(true);

		_activeShell.setRedraw(true);

		addTTShellListener(_activeShell);
		addTTAllControlsListener(_activeShell);

		_activeToolTipArea = toolTipArea;
	}

	protected Point fixupDisplayBounds(final Point tipSize, final Point location) {

		final Rectangle displayBounds = getDisplayBounds(location);
		final Point rightBottomBounds = new Point(tipSize.x + location.x, tipSize.y + location.y);

		if (!(displayBounds.contains(location) && displayBounds.contains(rightBottomBounds))) {

			if (rightBottomBounds.x > displayBounds.x + displayBounds.width) {
				location.x -= rightBottomBounds.x - (displayBounds.x + displayBounds.width);
			}

			if (rightBottomBounds.y > displayBounds.y + displayBounds.height) {
// ignore when tt is below the bottom, force the user to resize the tt
//				location.y -= rightBottomBounds.y - (displayBounds.y + displayBounds.height);
			}

			if (location.x < displayBounds.x) {
				location.x = displayBounds.x;
			}

			if (location.y < displayBounds.y) {
				location.y = displayBounds.y;
			}
		}

		return location;
	}

	private Rectangle getDisplayBounds(final Point location) {

		Rectangle displayBounds;
		final Monitor[] allMonitors = _ownerControl.getDisplay().getMonitors();

		if (allMonitors.length > 1) {
			// By default present in the monitor of the control
			displayBounds = _ownerControl.getMonitor().getBounds();
			final Point p = new Point(location.x, location.y);

			// Search on which monitor the event occurred
			Rectangle tmp;
			for (final Monitor element : allMonitors) {
				tmp = element.getBounds();
				if (tmp.contains(p)) {
					displayBounds = tmp;
					break;
				}
			}

		} else {
			displayBounds = _ownerControl.getDisplay().getBounds();
		}

		return displayBounds;
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
	protected Object getToolTipArea(final Point ownerHoverPosition) {
		return _ownerControl;
	}

	/**
	 * Get tooltip location.
	 * 
	 * @param size
	 *            Tooltip size
	 * @return Returns location relative to the device.
	 */
	protected abstract Point getToolTipLocation(Point size, final Point mouseOwnerPosition);

	/**
	 * Hide the currently active tool tip
	 */
	public void hide() {
		ttHide();
	}

	private void moveShellWhenVisible() {

		if (_isKeepToolTipOpenWhenResizedOrMoved == false) {
			return;
		}

		if (_activeShell == null || _activeShell.isDisposed() || _activeShell.isVisible() == false) {
			return;
		}

		ttShow();
	}

//	protected boolean isToolTipVisible() {
//
//		if (shell == null || shell.isDisposed()) {
//			return false;
//		}
//
//		final boolean isShellVisible = shell.isVisible();
//
////		System.out.println(UI.timeStampNano() + " isShellVisible=" + isShellVisible);
////		// TODO remove SYSTEM.OUT.PRINTLN
//
//		return isShellVisible;
//	}

	/**
	 * @return When the returned rectangle (which has display locations) is hit by the mouse, the
	 *         tooltip should not be hidden. When <code>null</code> this check is ignored.
	 */
	protected Rectangle noHideOnMouseMove() {
		return null;
	}

	/**
	 * @return Returns <code>true</code> when tooltip should be kept opened.
	 */
	private boolean onDisplayMouseMove() {

//		System.out.println(UI.timeStampNano() + " onDisplayMouseMove\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

//		final long start = System.nanoTime();

		if (_activeShell == null || _activeShell.isDisposed() || _activeShell.isVisible() == false) {
			return false;
		}

		boolean isHide = false;
		boolean isKeepVisible = false;

		// get control which is hovered with the mouse after the exit, can be null
		final Control hoveredControl = _display.getCursorControl();

//		System.out.println(UI.timeStampNano() + " onTTDisplayMouseMove - hoveredControl " + hoveredControl);
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (hoveredControl == null) {

//			System.out.println(UI.timeStampNano() + " exit 0 hide");
//			// TODO remove SYSTEM.OUT.PRINTLN

			isHide = true;

		} else {

			/*
			 * check if the hovered control is the owner control, if not, hide the tooltip
			 */
			Control hoveredParent = hoveredControl;

			// move up child-parent hierarchy until shell is reached
			while (true) {

				if (hoveredParent == _activeShell) {

					// mouse is hovering in this tooltip

					isKeepVisible = true;

//					System.out.println(UI.timeStampNano() + " exit 1 no hide");
//					// TODO remove SYSTEM.OUT.PRINTLN

					break;
				}

				if (hoveredParent == _ownerControl) {

					// mouse is hovering the owner control

					if (_mouseOverBehaviour == MOUSE_OVER_BEHAVIOUR_NO_IGNORE) {

						/*
						 * owner is not ignored, which means the when the mouse is hovered the
						 * owner, the tooltip keeps opened, this is the default
						 */

						isKeepVisible = true;
					}

//					System.out.println(UI.timeStampNano() + " exit 2 no hide");
//					// TODO remove SYSTEM.OUT.PRINTLN

					break;
				}

				hoveredParent = hoveredParent.getParent();

				if (hoveredParent == null) {

					// mouse has left the tooltip and the owner control

//					System.out.println(UI.timeStampNano() + " exit 3 hide");
//					// TODO remove SYSTEM.OUT.PRINTLN

					isHide = true;

					break;
				}
			}
		}

		/**
		 * !!! this adjustment do not work on Linux because the tooltip gets hidden when the mouse
		 * tries to mover over the tooltip <br>
		 * <br>
		 * it seems to work on windows and linux with margin 1, when set to 0 the tooltip do
		 * sometime not be poped up again and the i-icons is not deaktivated<br>
		 * wolfgang 2010-07-23
		 */

		final Rectangle ttShellRect = _activeShell.getBounds();
		final int margin = 10;

		ttShellRect.x -= margin;
		ttShellRect.y -= margin;
		ttShellRect.width += 2 * margin;
		ttShellRect.height += 2 * margin;

		final Point displayCursorLocation = _display.getCursorLocation();

		final boolean isInTooltip = ttShellRect.contains(displayCursorLocation);

		if (isKeepVisible == false && isHide == false && isInTooltip == false) {
			isHide = true;
		}

		boolean isKeepOpened = true;

		if (isInTooltip && _isShellFadingOut) {

			// don't hide when mouse is hovering hiding tooltip

			ttShow();

		} else if (isHide) {

			final Rectangle noHideArea = noHideOnMouseMove();

			if (noHideArea == null || noHideArea.contains(displayCursorLocation) == false) {

				// hide definitively

				ttHide();

				isKeepOpened = false;
			}
		}

//		System.out.println(UI.timeStampNano()
//				+ " onDisplayMouseMove\t"
//				+ ((float) (System.nanoTime() - start) / 1000000)
//				+ " ms");
//		// TODO remove SYSTEM.OUT.PRINTLN

		return isKeepOpened;
	}

	protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {}

	private void onOwnerControlEvent(final Event event) {

		if (_ownerControl == null || _ownerControl.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.Dispose:

			onOwnerDispose(event);

			removeOwnerControlsListener();

			break;

		case SWT.MouseExit:

			_isOwnerHovered = false;

			// suspend hover time
			_display.timerExec(-1, _ownerHoverTimer);

			break;

		case SWT.MouseMove:

			_isOwnerHovered = true;

			// keep mouse position
			_ownerHoverPosition = new Point(event.x, event.y);

			// start hover time
			_display.timerExec(MOUSE_HOVER_DELAY, _ownerHoverTimer);

			break;

		case SWT.Resize:

			moveShellWhenVisible();

			break;
		}
	}

	private void onOwnerDispose(final Event event) {

		// hide tooltip definitively

		removeOwnerShellListener();
		removeDisplayFilterListener();

//		passOnEvent(shell, event);

		close(true);
	}

	private void onOwnerHovered() {

		final Object toolTipArea = getToolTipArea(_ownerHoverPosition);

		if (toolTipArea == null) {

			// nothing is hovered, hide tooltip

			close(true);

		} else {

			// tooltip area is hovered, create/open tooltip

			final Shell ttShell = _allHoveredShells.get(toolTipArea);

			if (ttShell != null && ttShell.isDisposed() == false) {

				// shell exists, put this shell on top

				ttShell.moveAbove(null);

			} else {

				// create a new shell

				showToolTip(toolTipArea);
			}
		}
	}

	private void onOwnerShellEvent(final Event event) {

		if (_activeShell == null || _activeShell.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.Deactivate:

			_display.asyncExec(new Runnable() {

				public void run() {

					// hide tooltip when another shell is activated

					if (_display.getActiveShell() != _activeShell) {
						ttHide();
					}
				}
			});
			break;

		case SWT.Move:

			moveShellWhenVisible();

			break;
		}
	}

	private void onTTAutoCloseTimer() {

//		System.out.println(UI.timeStampNano() + " onTTAutoCloseTimer\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

		final boolean isKeepOpened = onDisplayMouseMove();

		if (isKeepOpened) {

			// start again to check again
			_display.timerExec(AUTO_CLOSE_INTERVAL, _ttAutoCloseTimer);
		}
	}

	private void onTTControlEvent(final Event event) {

		if (_activeShell == null || _activeShell.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.KeyDown:

			if (event.keyCode == SWT.ESC) {
				hide();
			}

			break;

		case SWT.MouseEnter:

			if (_isShellFadingIn || _isShellFadingOut) {

				// stop animation
//				_isShellFadingIn = _isShellFadingOut = false;
			}

			break;

		case SWT.MouseExit:

//			System.out.println(UI.timeStampNano() + " mouse exit\t" + event.widget);
//			// TODO remove SYSTEM.OUT.PRINTLN

			break;

		case SWT.MouseMove:

//			System.out.println(UI.timeStampNano() + " mouse move\t" + event.widget);
//			// TODO remove SYSTEM.OUT.PRINTLN

			if (_isReceiveOnMouseMove //
//					&& _isShellFadingIn == false
//					&& _isShellFadingOut == false//
			) {

				final Widget widget = event.widget;

				if (widget instanceof Control) {

					final Point ttDisplayLocation = ((Control) widget).toDisplay(event.x, event.y);
					final Point ownerLocation = _ownerControl.toControl(ttDisplayLocation);

					_display.asyncExec(new Runnable() {
						public void run() {

							final MouseEvent mouseEvent = new MouseEvent(event);
							mouseEvent.x = ownerLocation.x;
							mouseEvent.y = ownerLocation.y;

							onMouseMoveInToolTip(mouseEvent);
						}
					});
				}
			}

			break;
		}
	}

	private void onTTShellEvent(final Event event) {

		switch (event.type) {
		case SWT.Deactivate:

			if (_activeShell != null
					&& !_activeShell.isDisposed()
					&& _ownerControl != null
					&& !_ownerControl.isDisposed()) {

				_display.asyncExec(new Runnable() {

					public void run() {

						// hide tooltip when another shell is activated

						// check again
						if (_activeShell == null
								|| _activeShell.isDisposed()
								|| _ownerControl == null
								|| _ownerControl.isDisposed()) {
							return;
						}

						if (_ownerControl.getShell() == _activeShell.getDisplay().getActiveShell()) {

							// don't hide when main window is active
							return;
						}

						ttHide();
					}
				});
			}

			break;

		case SWT.Dispose:

			break;

		}

	}

	private void passOnEvent(final Shell shell, final Event event) {

		if (_ownerControl != null
				&& !_ownerControl.isDisposed()
				&& event != null
				&& event.widget != _ownerControl
				&& event.type == SWT.MouseDown) {

			// the following was left in order to fix bug 298770 with minimal change. In 3.7, the complete method should be removed.
			shell.close();
		}
	}

	private void removeDisplayFilterListener() {

		if (_isDisplayListenerSet) {

			_display.removeFilter(SWT.MouseMove, _displayFilterListener);

			_isDisplayListenerSet = false;
		}
	}

	/**
	 * Deactivate tooltip support for the underlying control
	 */
	private void removeOwnerControlsListener() {

		_ownerControl.removeListener(SWT.Dispose, _ownerControlListener);
		_ownerControl.removeListener(SWT.MouseExit, _ownerControlListener);
		_ownerControl.removeListener(SWT.MouseMove, _ownerControlListener);
		_ownerControl.removeListener(SWT.Resize, _ownerControlListener);
	}

	private void removeOwnerShellListener() {

		final Shell ownerShell = _ownerControl.getShell();

		ownerShell.removeListener(SWT.Deactivate, _ownerShellListener);
		ownerShell.removeListener(SWT.Move, _ownerShellListener);
	}

	protected void setBehaviourOnMouseOver(final int mouseOverBehaviour) {
		_mouseOverBehaviour = mouseOverBehaviour;
	}

	public void setFadeIsSteps(final int fadeInSteps) {
		_fadeInSteps = fadeInSteps;
	}

	public void setIsKeepShellOpenWhenMoved(final boolean isKeepShellOpenWhenMoved) {
		_isKeepToolTipOpenWhenResizedOrMoved = isKeepShellOpenWhenMoved;
	}

	public void setReceiveMouseMoveEvent(final boolean isReceive) {
		_isReceiveOnMouseMove = isReceive;
	}

	private void setShellVisible(final boolean isVisible) {

		if (isVisible) {

			// show tooltip

			_activeShell.setVisible(true);

			addDisplayFilterListener();

		} else {

			// hide tooltip

			beforeHideToolTip();

			_activeShell.setVisible(false);

			removeDisplayFilterListener();
		}
	}

	private void showToolTip(final Object toolTipArea) {

		/*
		 * show tooltip only when this is the active shell, this check is necessary that when a tour
		 * chart is opened in a dialog (e.g. adjust altitude) that a hidden tour chart tooltip in
		 * the tour chart view is also displayed
		 */
//		if (_display.getActiveShell() != _ownerControl.getShell() || _ownerControl.isVisible() == false) {
//			return false;
//		}

		createUI(toolTipArea);

		ttShow();
	}

	private void ttHide() {

//		System.out.println(UI.timeStampNano() + " ttHide\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (_activeShell == null || _activeShell.isDisposed() || _activeShell.isVisible() == false) {
			return;
		}

		if (_isShellFadingOut) {

			// shell is already fading out
			return;
		}

		// shell is not yet fading out

		_isShellFadingIn = false;
		_isShellFadingOut = true;

		animation10_Start();
	}

	private void ttShow() {

//		System.out.println(UI.timeStampNano() + " ttShow\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

		// ensure tooltip is closed when mouse is not hovering the tooltip or application
		_display.timerExec(AUTO_CLOSE_INTERVAL, _ttAutoCloseTimer);

		// shell is not yet fading in

		_isShellFadingIn = true;
		_isShellFadingOut = false;

		animation10_Start();
	}
}
