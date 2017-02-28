/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
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
public abstract class AdvancedToolTipShell {

	/**
	 * how long each tick is when fading in/out (in ms)
	 */
	private static final int			FADE_TIME_INTERVAL					= UI.IS_OSX ? 10 : 10;
 
	/**
	 * Number of steps when fading in
	 */
	private static final int			SHELL_FADE_IN_STEPS					= 20;

	/**
	 * Number of steps when fading out
	 */
	private static final int			SHELL_FADE_OUT_STEPS				= 20;

	/**
	 * Number of steps before fading out
	 */
	private static final int			SHELL_FADE_OUT_DELAY_STEPS			= 20;

	private static final int			SHELL_MOVE_STEPS					= 30;

	private static final int			ALPHA_OPAQUE						= 0xff;

	private static final String			STATE_HORIZ_TOOL_TIP_WIDTH			= "STATE_HORIZ_TOOL_TIP_WIDTH";				//$NON-NLS-1$
	private static final String			STATE_HORIZ_TOOL_TIP_HEIGHT			= "STATE_HORIZ_TOOL_TIP_HEIGHT";			//$NON-NLS-1$
	private static final String			STATE_VERT_TOOL_TIP_WIDTH			= "STATE_VERT_TOOL_TIP_WIDTH";				//$NON-NLS-1$
	private static final String			STATE_VERT_TOOL_TIP_HEIGHT			= "STATE_VERT_TOOL_TIP_HEIGHT";				//$NON-NLS-1$

	private static final String			STATE_IS_TOOL_TIP_PINNED			= "STATE_IS_TOOL_TIP_PINNED";				//$NON-NLS-1$
	private static final String			STATE_HORIZ_TOOL_TIP_PIN_LOCATION_X	= "STATE_HORIZ_TOOL_TIP_PIN_LOCATION_X";	//$NON-NLS-1$
	private static final String			STATE_HORIZ_TOOL_TIP_PIN_LOCATION_Y	= "STATE_HORIZ_TOOL_TIP_PIN_LOCATION_Y";	//$NON-NLS-1$
	private static final String			STATE_VERT_TOOL_TIP_PIN_LOCATION_X	= "STATE_VERT_TOOL_TIP_PIN_LOCATION_X";		//$NON-NLS-1$
	private static final String			STATE_VERT_TOOL_TIP_PIN_LOCATION_Y	= "STATE_VERT_TOOL_TIP_PIN_LOCATION_Y";		//$NON-NLS-1$

	private static final int			MIN_SHELL_HORIZ_WIDTH				= 100;
	private static final int			MIN_SHELL_HORIZ_HEIGHT				= 60;
	private static final int			MIN_SHELL_VERT_WIDTH				= 100;
	private static final int			MIN_SHELL_VERT_HEIGHT				= 60;

	private int							_shellFadeInSteps					= SHELL_FADE_IN_STEPS;
	private int							_shellFadeOutSteps					= SHELL_FADE_OUT_STEPS;
	private int							_shellFadeOutDelaySteps				= SHELL_FADE_OUT_DELAY_STEPS;
	private int							_shellMoveSteps						= SHELL_MOVE_STEPS;

	private IDialogSettings				_state;

	private OwnerControlListener		_ownerControlListener;
	private OwnerShellListener			_ownerShellListener;
	private ToolTipShellListener		_ttShellListener;
	private ToolTipAllControlsListener	_ttAllControlsListener;
	private ToolTipDisplayListener		_ttDisplayListener;

	/**
	 * Keep track of added display listener that no more than <b>1</b> is set.
	 */
	private boolean						_isDisplayListenerSet;

	private boolean						_isShellToggled;

	/**
	 * Is <code>true</code> when shell is fading out, otherwise <code>false</code>.
	 */
	private boolean						_isShellFadingOut;

	/**
	 * Is <code>true</code> when shell is fading in, otherwise <code>false</code>.
	 */
	private boolean						_isShellFadingIn;

	private boolean						_isToolTipPinned;

	private Point						_shellStartLocation;
	private Point						_shellEndLocation					= new Point(0, 0);

	private int							_fadeOutDelayCounter;

	private int							_horizContentWidth					= MIN_SHELL_HORIZ_WIDTH;
	private int							_horizContentHeight					= MIN_SHELL_HORIZ_HEIGHT;
	private int							_vertContentWidth					= MIN_SHELL_VERT_WIDTH;
	private int							_vertContentHeight					= MIN_SHELL_VERT_HEIGHT;

	private int							_horizPinLocationX;
	private int							_horizPinLocationY;
	private int							_vertPinLocationX;
	private int							_vertPinLocationY;

	private final AnimationTimer		_animationTimer;
	private int							_animationMoveCounter;

	private boolean						_isInShellResize;
	private boolean						_isKeepToolTipOpen;
	private boolean						_isDoNotStopAnimation;

	private AbstractRRShell				_visibleRRShell;
	private AbstractRRShell				_rrShellWithResize;
	private AbstractRRShell				_rrShellNoResize;

	/*
	 * UI resources
	 */
	private Display						_display;

	private Composite					_ttContentArea;

	/**
	 * Tooltip shell which is currently be visible
	 */
	private Shell						_visibleShell;

	private Control						_ownerControl;

	private final class AnimationTimer implements Runnable {
		@Override
		public void run() {
			animation20_Runnable();
		}
	}

	private class OwnerControlListener implements Listener {
		@Override
		public void handleEvent(final Event event) {
			onOwnerControlEvent(event);
		}
	}

	private final class OwnerShellListener implements Listener {
		@Override
		public void handleEvent(final Event event) {
			onOwnerShellEvent(event);
		}
	}

	public class RRShell extends AbstractRRShell {

		public RRShell(final Shell parentShell, final int style, final String shellTitle, final boolean isResizeable) {
			super(parentShell, style, shellTitle, isResizeable);
		}

		@Override
		public Point getContentSize() {
			return AdvancedToolTipShell.this.getContentSize();
		}
	}

	/**
	 * This listener is added to ALL widgets within the tooltip shell.
	 */
	private class ToolTipAllControlsListener implements Listener {
		@Override
		public void handleEvent(final Event event) {
			onTTAllControlsEvent(event);
		}

	}

	private class ToolTipDisplayListener implements Listener {
		@Override
		public void handleEvent(final Event event) {

			switch (event.type) {
			case SWT.MouseMove:
				onTTDisplayMouseMove(event);
				break;
			}
		}
	}

	private final class ToolTipShellListener implements Listener {
		@Override
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
	public AdvancedToolTipShell(final Control ownerControl, final IDialogSettings state) {

		_ownerControl = ownerControl;
		_state = state;

		_display = _ownerControl.getDisplay();

		_ttAllControlsListener = new ToolTipAllControlsListener();
		_ttShellListener = new ToolTipShellListener();
		_ttDisplayListener = new ToolTipDisplayListener();

		_ownerControlListener = new OwnerControlListener();
		_ownerShellListener = new OwnerShellListener();

		_animationTimer = new AnimationTimer();

		ownerControlAddListener();
	}

	protected void actionPinToolTip(final boolean isPinned) {

		_isToolTipPinned = isPinned;

		if (isPinned) {
			setToolTipPinnedLocation();
		}

		enableControls();
	}

	/**
	 * This is called after the shell and content area are created.
	 * 
	 * @param shell
	 */
	protected abstract void afterCreateShell(Shell shell);

	private void animation10_Start() {

		final int a = 0;

		if (a == 1) {
			animation10_Start_Simple();
		} else {
			animation10_StartKomplex();
		}
	}

	private void animation10_Start_Simple() {

		if (_isShellFadingIn) {

			// show tool tip

			final Point size = getContentSize();

			final Point defaultLocation = getToolTipLocation(size);

			final Point shellLocation = fixupDisplayBounds(size, defaultLocation);

			_visibleRRShell.setShellLocation(shellLocation.x, shellLocation.y, 1);

			reparentShell(_rrShellNoResize);

			_rrShellNoResize.setAlpha(0xff);

			setShellVisible(true);

		} else {

			// hide tooltip

			setShellVisible(false);
		}
	}

	private void animation10_StartKomplex() {

//		System.out.println(UI.timeStampNano() + " in " + _isShellFadingIn + ("\tout " + _isShellFadingOut));
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (_isKeepToolTipOpen) {

			// hiding has started, stop it and keep tooltip open

			_isKeepToolTipOpen = false;

			final Point shellCurrentLocation = _visibleShell.getLocation();

			_shellStartLocation = _shellEndLocation = shellCurrentLocation;

			_isShellFadingIn = true;
			_isShellFadingOut = false;

		} else {

			if (_isShellFadingIn) {

				// set fading in location

				final Point contentSize = _visibleRRShell.getContentSize();
				final Point shellSize = _visibleRRShell.getShellSize(contentSize);

				Point contentLocation;
				if (_isToolTipPinned) {

					if (isVerticalLayout()) {
						contentLocation = new Point(_vertPinLocationX, _vertPinLocationY);
					} else {
						contentLocation = new Point(_horizPinLocationX, _horizPinLocationY);
					}

				} else {
					contentLocation = getToolTipLocation(contentSize);
				}

				Point shellEndLocation = _visibleRRShell.getShellLocation(contentLocation);
				shellEndLocation = fixupDisplayBounds(shellSize, shellEndLocation);

				final boolean isShellVisible = _visibleShell.isVisible();

				if (shellEndLocation.x == _shellEndLocation.x && isShellVisible) {

					// shell is already fading in with the correct location

//					System.out.println(UI.timeStampNano() + " is in END location\t");
//					// TODO remove SYSTEM.OUT.PRINTLN

					return;
				}

				// set new end location
				_shellEndLocation = shellEndLocation;

				if (_isToolTipPinned == false && isShellVisible) {

					// shell is already visible, move from the current position to the target position

					_shellStartLocation = _visibleShell.getLocation();

				} else {

					// shell is not visible, set position directly without moving animation, do only fading animation

					_shellStartLocation = _shellEndLocation;

					_visibleRRShell.setShellLocation(_shellStartLocation.x, _shellStartLocation.y, 2);

					reparentShell(_rrShellNoResize);

					setShellVisible(true);
				}

			} else if (_isShellFadingOut) {

				// fading out has no movement

				_fadeOutDelayCounter = 0;
			}
		}

//		System.out.println(UI.timeStampNano() + " animation10_StartKomplex\t" );
//		// TODO remove SYSTEM.OUT.PRINTLN

		// start animation now
		_animationMoveCounter = 0;
		animation20_Runnable();
	}

	private void animation20_Runnable() {

//		final long start = System.nanoTime();

		if (_visibleShell == null || _visibleShell.isDisposed() || _visibleShell.isVisible() == false) {
			return;
		}

//		System.out.println(UI.timeStampNano() + " animation20_Runnable " + _isShellFadingIn + "  " + _isShellFadingOut);
//		// TODO remove SYSTEM.OUT.PRINTLN

		try {
			/*
			 * endAlpha will be the final fadeIn/fadeOut value when the animation stops
			 */
			int finalFadeAlpha = -1;

			int currentAlpha = _visibleShell.getAlpha();
			boolean isLoopBreak = false;

			_animationMoveCounter++;

			while (true) {

				int newAlpha = -1;

				if (_isShellFadingIn) {

					final int shellStartX = _shellStartLocation.x;
					final int shellStartY = _shellStartLocation.y;
					final int shellEndX = _shellEndLocation.x;
					final int shellEndY = _shellEndLocation.y;

					final Point shellCurrentLocation = _visibleShell.getLocation();

					final boolean isInTarget = shellCurrentLocation.x == shellEndX
							&& shellCurrentLocation.y == shellEndY;

					final int diffAlpha = ALPHA_OPAQUE / _shellFadeInSteps;

					newAlpha = currentAlpha + diffAlpha;
					if (newAlpha > ALPHA_OPAQUE) {
						newAlpha = ALPHA_OPAQUE;
					}
					finalFadeAlpha = ALPHA_OPAQUE;

					if (isInTarget && currentAlpha == ALPHA_OPAQUE) {

						// target is reached and fully visible, stop animation

						_isShellFadingIn = false;

						return;

					} else {

						if (isInTarget == false) {

							// move to target

							final int diffX = shellStartX - shellEndX;
							final int diffY = shellStartY - shellEndY;

							final double moveX = (double) diffX / _shellMoveSteps * _animationMoveCounter;
							final double moveY = (double) diffY / _shellMoveSteps * _animationMoveCounter;

							final int shellCurrentX = (int) (shellStartX - moveX);
							final int shellCurrentY = (int) (shellStartY - moveY);

							_visibleRRShell.setShellLocation(shellCurrentX, shellCurrentY, 3);
						}
					}

				} else if (_isShellFadingOut) {

					if (currentAlpha == 0xff && _fadeOutDelayCounter++ < _shellFadeOutDelaySteps) {

						// delay fade out only when shell is fully visible

						_display.timerExec(FADE_TIME_INTERVAL, _animationTimer);

						return;
					}

					final int alphaDiff = ALPHA_OPAQUE / _shellFadeOutSteps;

					newAlpha = currentAlpha - alphaDiff;
					finalFadeAlpha = 0;

					if (newAlpha <= 0) {

						// shell is not visible any more, hide it now

						_visibleShell.setAlpha(0);

						// hide shell
						setShellVisible(false);

						_isShellFadingOut = false;

						return;
					}
				}

				if (newAlpha == -1) {

					return;

				} else {

					if (newAlpha != currentAlpha) {
						_visibleShell.setAlpha(newAlpha);
					}

					if (_visibleShell.getAlpha() != newAlpha) {

						// platform do not support shell alpha, this occured on Ubuntu 12.04

						if (isLoopBreak) {
							break;
						}

						// loop only once
						isLoopBreak = true;

						currentAlpha = finalFadeAlpha;

						continue;

					} else {

						_display.timerExec(FADE_TIME_INTERVAL, _animationTimer);

						break;
					}

				}
			}

		} catch (final Exception err) {
			StatusUtil.log(err);
		} finally {

//			final float timeDiff = (float) (System.nanoTime() - start) / 1000000;
//			System.out.println(UI.timeStampNano() + " animation20_Runnable:\t" + timeDiff + " ms\t" + " ms");
//			// TODO remove SYSTEM.OUT.PRINTLN
		}
	}

	protected abstract void beforeHideToolTip();

	/**
	 * <b>VERY IMPORTANT</b>
	 * <p>
	 * Do not hide the tooltip when other shells (dialogs) are open, otherwise the app is blocked
	 * and must be killed.
	 * 
	 * @return Returns <code>true</code> when shell can be closed, otherwise it CANNOT be closed.
	 */
	private boolean canCloseShell() {

		if (_visibleShell == null) {
			return true;
		}

		final Shell[] openedShells = _visibleShell.getShells();

		return openedShells.length == 0;
	}

	protected abstract void closeInternalShells();

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
	 * @return Returns <code>true</code> when shell is created.
	 */
	private void createUI() {

		if (_visibleShell != null && !_visibleShell.isDisposed()) {
			// shell is already created
			return;
		}

		/*
		 * resize shell
		 */
		_rrShellWithResize = new RRShell(
				_ownerControl.getShell(), //
				SWT.ON_TOP //
//						| SWT.TOOL
						| SWT.RESIZE | SWT.NO_FOCUS,
				getShellTitle_WithResize(),
				true);

		final Shell shellWithResize = _rrShellWithResize.getShell();
		shellWithResize.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onTTShellResize(e);
			}
		});

		ttShellAddListener(shellWithResize);

		/*
		 * no resize shell
		 */
		_rrShellNoResize = new RRShell(
				_ownerControl.getShell(), //
				SWT.ON_TOP //
//						| SWT.TOOL
						| SWT.NO_FOCUS,
				getShellTitle_NoResize(),
				false);

		ttShellAddListener(_rrShellNoResize.getShell());

		setCurrentVisibleShell(_rrShellNoResize);

		// set initial alpha
		_visibleShell.setAlpha(0x0);

		ownerShellAddListener();

		// create UI
		_ttContentArea = createToolTipContentArea(_rrShellNoResize.getShellPage());

		ttAllControlsAddListener(_visibleShell);

		updateUI_Colors();

		afterCreateShell(_visibleShell);
	}

	protected void doNotStopAnimation() {
		_isDoNotStopAnimation = true;
	}

	protected abstract void enableControls();

	private Point fixupDisplayBounds(final Point tipSize, final Point location) {

		final Rectangle displayBounds = getDisplayBounds(location);
		final Point rightBottomBounds = new Point(tipSize.x + location.x, tipSize.y + location.y);

		if (!(displayBounds.contains(location) && displayBounds.contains(rightBottomBounds))) {

			if (rightBottomBounds.x > displayBounds.x + displayBounds.width) {
				location.x -= rightBottomBounds.x - (displayBounds.x + displayBounds.width);
			}

			if (rightBottomBounds.y > displayBounds.y + displayBounds.height) {
				location.y -= rightBottomBounds.y - (displayBounds.y + displayBounds.height);
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

	/**
	 * @return Returns size of the tooltip content
	 */
	protected Point getContentSize() {
		if (isVerticalLayout()) {
			return new Point(_vertContentWidth, _vertContentHeight);
		} else {
			return new Point(_horizContentWidth, _horizContentHeight);
		}
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

	protected Color getShellColor_Background(final ColorRegistry colorRegistry) {
		return Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
	};

	protected Color getShellColor_Foreground(final ColorRegistry colorRegistry) {
		return Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
	};

	protected String getShellTitle_NoResize() {
		return UI.EMPTY_STRING;
	}

	protected String getShellTitle_WithResize() {
		return UI.EMPTY_STRING;
	}

	/**
	 * Get tooltip location.
	 * 
	 * @param size
	 *            Tooltip size
	 * @return Returns location relative to the device.
	 */
	protected abstract Point getToolTipLocation(Point size);

	/**
	 * @return Return tooltip shell which is currently be visible.
	 */
	protected Shell getToolTipShell() {
		return _visibleShell;
	}

	/**
	 * Hide the currently active tool tip
	 */
	public void hide() {
		ttHide(null);
	}

	/**
	 * @return Returns <code>true</code> to hide tooltip, <code>false</code> will not hide the
	 *         tooltip.
	 */
	protected abstract boolean isToolTipDragged();

	protected boolean isToolTipPinned() {
		return _isToolTipPinned;
	}

	/**
	 * A tooltip can have 2 layouts, vertical and horizontal, each of them has different screen
	 * locations which are preserved in a state.
	 * 
	 * @return Returns <code>true</code> when vertical, otherwise it is horizontal.
	 */
	protected abstract boolean isVerticalLayout();

	private void onOwnerControlEvent(final Event event) {

		if (_ownerControl == null || _ownerControl.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.Dispose:

			ttDispose(event);

			ownerControlsRemoveListener();

			break;

		case SWT.Resize:

			showShellWhenVisible();

			break;
		}
	}

	private void onOwnerShellEvent(final Event event) {

		if (_visibleShell == null || _visibleShell.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.Deactivate:

			_display.asyncExec(new Runnable() {

				@Override
				public void run() {

					// hide tooltip when another shell is activated

					if (_display.getActiveShell() != _visibleShell) {
						ttHide(event);
					}
				}
			});
			break;

		case SWT.Move:

			showShellWhenVisible();
			break;
		}
	}

	protected abstract void onReparentShell(Shell reparentedShell);

	private void onTTAllControlsEvent(final Event event) {

		if (_visibleShell == null || _visibleShell.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.KeyDown:

			if (event.keyCode == SWT.ESC) {
				hide();
			}

			break;

		case SWT.MouseEnter:

//			System.out.println(UI.timeStampNano() + " _isDoNotStopAnimation " + _isDoNotStopAnimation);
//			// TODO remove SYSTEM.OUT.PRINTLN

			if (_isDoNotStopAnimation == false) {

				if (_isShellFadingIn || _isShellFadingOut) {

					// stop animation

					_isShellFadingIn = false;
					_isShellFadingOut = false;
				}
			}

			reparentShell(_rrShellWithResize);

			break;

		case SWT.MouseExit:

			if (_isShellToggled) {

				// do this only once

				_isShellToggled = false;
			}

			break;
		}
	}

	private void onTTDisplayMouseMove(final Event event) {

//		final long start = System.nanoTime();

//		System.out.println(UI.timeStampNano() + " onTTDisplayMouseMove\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (_visibleShell == null || _visibleShell.isDisposed() || _visibleShell.isVisible() == false) {
			return;
		}

		if (isToolTipDragged()) {
			// is true when tooltip is dragged with the mouse
			return;
		}

		boolean isHide = false;
		boolean isKeepVisible = false;

		// get control which is hovered with the mouse after the exit, can be null
		final Control hoveredControl = _display.getCursorControl();

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

				if (hoveredParent == _visibleShell) {

					// mouse is hovering in this tooltip

					isKeepVisible = true;

//					System.out.println(UI.timeStampNano() + " exit 1 no hide");
//					// TODO remove SYSTEM.OUT.PRINTLN

					break;
				}

				if (hoveredParent == _ownerControl) {

					// mouse is hovering the owner control

					isKeepVisible = true;

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

		final Rectangle ttShellRect = _visibleShell.getBounds();
		final int margin = 10;

		ttShellRect.x -= margin;
		ttShellRect.y -= margin;
		ttShellRect.width += 2 * margin;
		ttShellRect.height += 2 * margin;

		final Point cursorLocation = _display.getCursorLocation();

		final boolean isInTooltip = ttShellRect.contains(cursorLocation);

		if (!isInTooltip) {

			// mouse is not within the tooltip shell rectangle, reparent to NoResize shell

			closeInternalShells();

			reparentShell(_rrShellNoResize);
		}

		if (isKeepVisible == false && isHide == false && isInTooltip == false) {
			isHide = true;
		}

		if (isInTooltip && _isShellFadingOut) {

			// don't hide when mouse is hovering hiding tooltip

			_isKeepToolTipOpen = true;

			ttShow();

		} else if (isHide) {

			// hide definitively

			if (canCloseShell() == false) {
				return;
			}

			ttHide(event);
		}

//		System.out.println("time\t" + ((float) (System.nanoTime() - start) / 1000000) + " ms");
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	private void onTTShellEvent(final Event event) {

		switch (event.type) {
		case SWT.Deactivate:

			if (_visibleShell != null
					&& !_visibleShell.isDisposed()
					&& _ownerControl != null
					&& !_ownerControl.isDisposed()) {

				_display.asyncExec(new Runnable() {

					@Override
					public void run() {

						// hide tooltip when another shell is activated

						// check again
						if (_visibleShell == null
								|| _visibleShell.isDisposed()
								|| _ownerControl == null
								|| _ownerControl.isDisposed()) {
							return;
						}

						if (_ownerControl.getShell() == _visibleShell.getDisplay().getActiveShell()) {

							// don't hide when main window is active
							return;
						}

						ttHide(event);
					}
				});
			}

			break;

		case SWT.Dispose:

			break;

		}

	}

	private void onTTShellResize(final ControlEvent event) {

		if (_isInShellResize) {
			return;
		}

		final Shell resizeShell = _rrShellWithResize.getShell();
		final Point shellLocation = resizeShell.getLocation();

		// ensure tooltip is not too large
		final Rectangle displayBounds = getDisplayBounds(shellLocation);
		final double maxHeight = displayBounds.height * 0.8;
		final double maxWidth = displayBounds.width * 0.95;

		boolean isResizeAdjusted = false;

		final Rectangle clientArea = resizeShell.getClientArea();
		int newContentWidth = clientArea.width;
		int newContentHeight = clientArea.height;

		if (newContentHeight > maxHeight) {
			newContentHeight = (int) maxHeight;
			isResizeAdjusted = true;
		} else if (newContentHeight < MIN_SHELL_HORIZ_HEIGHT) {
			newContentHeight = MIN_SHELL_HORIZ_HEIGHT;
			isResizeAdjusted = true;
		}

		if (newContentWidth > maxWidth) {
			newContentWidth = (int) maxWidth;
			isResizeAdjusted = true;
		} else if (newContentWidth < MIN_SHELL_HORIZ_WIDTH) {
			newContentWidth = MIN_SHELL_HORIZ_WIDTH;
			isResizeAdjusted = true;
		}

		if (isVerticalLayout()) {
			_vertContentWidth = newContentWidth;
			_vertContentHeight = newContentHeight;
		} else {
			_horizContentWidth = newContentWidth;
			_horizContentHeight = newContentHeight;
		}

		if (isResizeAdjusted) {
			_isInShellResize = true;
			{
				_rrShellWithResize.setContentSize(newContentWidth, newContentHeight);
			}
			_isInShellResize = false;
		}
	}

	/**
	 * Activate tooltip support for this control
	 */
	private void ownerControlAddListener() {

		ownerControlsRemoveListener();

		_ownerControl.addListener(SWT.Dispose, _ownerControlListener);
		_ownerControl.addListener(SWT.Resize, _ownerControlListener);
	}

	/**
	 * Deactivate tooltip support for the underlying control
	 */
	private void ownerControlsRemoveListener() {

		_ownerControl.removeListener(SWT.Dispose, _ownerControlListener);
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

	/**
	 * Reparent shell
	 * 
	 * @param newReparentedShell
	 *            Shell which should be used to display {@link #_ttContentArea}.
	 */
	private void reparentShell(final AbstractRRShell newReparentedShell) {

//		System.out.println(UI.timeStampNano() + " reparentShell\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (_visibleShell == newReparentedShell.getShell()) {
			// shell is already visible
			return;
		}

		final Shell prevShell = _visibleShell;
		Shell reparentedShell;

		ttAllControlsRemoveListener(prevShell);

		setCurrentVisibleShell(newReparentedShell);

		int trimDiffX = _rrShellWithResize.getShellTrimWidth() - _rrShellNoResize.getShellTrimWidth();
		int trimDiffY = _rrShellWithResize.getShellTrimHeight() - _rrShellNoResize.getShellTrimHeight();

		if (newReparentedShell == _rrShellWithResize) {

			// setup resize shell

			_rrShellWithResize.reparentFromOtherShell(_rrShellNoResize, _ttContentArea);

			reparentedShell = _rrShellWithResize.getShell();

			trimDiffX -= trimDiffX;
			trimDiffY -= trimDiffY;

		} else {

			// setup no resize shell

			_rrShellNoResize.reparentFromOtherShell(_rrShellWithResize, _ttContentArea);

			reparentedShell = _rrShellNoResize.getShell();
		}

		// fire reparent event
		onReparentShell(reparentedShell);

		// hide previous shell
		prevShell.setVisible(false);

		ttAllControlsAddListener(newReparentedShell.getShell());

		final boolean isShellMoving = _isShellFadingIn && _isKeepToolTipOpen == false;
		if (isShellMoving) {

			/*
			 * adjust shell positions because the reparent shell contains another trim size, it took
			 * me 2 whole days to finally fix this problem and find a solution but now the shell
			 * reparenting is smoothly
			 */

			final int shellStartX = _shellStartLocation.x + trimDiffX;
			final int shellStartY = _shellStartLocation.y + trimDiffY;

			final int shellEndX = _shellEndLocation.x + trimDiffX;
			final int shellEndY = _shellEndLocation.y + trimDiffY;

			_shellStartLocation = new Point(shellStartX, shellStartY);
			_shellEndLocation = new Point(shellEndX, shellEndY);
		}
	}

	protected void restoreState() {

		/*
		 * get horizontal gallery values
		 */
		_horizContentWidth = Util.getStateInt(_state, STATE_HORIZ_TOOL_TIP_WIDTH, 300);
		_horizContentHeight = Util.getStateInt(_state, STATE_HORIZ_TOOL_TIP_HEIGHT, 150);

		// ensure min values
		if (_horizContentWidth < MIN_SHELL_HORIZ_WIDTH) {
			_horizContentWidth = MIN_SHELL_HORIZ_WIDTH;
		}

		if (_horizContentHeight < MIN_SHELL_HORIZ_HEIGHT) {
			_horizContentHeight = MIN_SHELL_HORIZ_HEIGHT;
		}

		/*
		 * get vertical gallery values
		 */
		_vertContentWidth = Util.getStateInt(_state, STATE_VERT_TOOL_TIP_WIDTH, 400);
		_vertContentHeight = Util.getStateInt(_state, STATE_VERT_TOOL_TIP_HEIGHT, 250);

		// ensure min values
		if (_vertContentWidth < MIN_SHELL_VERT_WIDTH) {
			_vertContentWidth = MIN_SHELL_VERT_WIDTH;
		}

		if (_vertContentHeight < MIN_SHELL_VERT_HEIGHT) {
			_vertContentHeight = MIN_SHELL_VERT_HEIGHT;
		}

		/*
		 * pinned locations
		 */
		_isToolTipPinned = Util.getStateBoolean(_state, STATE_IS_TOOL_TIP_PINNED, false);
		setToolTipPinned(_isToolTipPinned);

		final int defaultPosition = 20;
		_horizPinLocationX = Util.getStateInt(_state, STATE_HORIZ_TOOL_TIP_PIN_LOCATION_X, defaultPosition);
		_horizPinLocationY = Util.getStateInt(_state, STATE_HORIZ_TOOL_TIP_PIN_LOCATION_Y, defaultPosition);
		_vertPinLocationX = Util.getStateInt(_state, STATE_VERT_TOOL_TIP_PIN_LOCATION_X, defaultPosition);
		_vertPinLocationY = Util.getStateInt(_state, STATE_VERT_TOOL_TIP_PIN_LOCATION_Y, defaultPosition);
	}

	protected void saveState() {

		_state.put(STATE_HORIZ_TOOL_TIP_WIDTH, _horizContentWidth);
		_state.put(STATE_HORIZ_TOOL_TIP_HEIGHT, _horizContentHeight);
		_state.put(STATE_VERT_TOOL_TIP_WIDTH, _vertContentWidth);
		_state.put(STATE_VERT_TOOL_TIP_HEIGHT, _vertContentHeight);

		_state.put(STATE_IS_TOOL_TIP_PINNED, _isToolTipPinned);
		_state.put(STATE_HORIZ_TOOL_TIP_PIN_LOCATION_X, _horizPinLocationX);
		_state.put(STATE_HORIZ_TOOL_TIP_PIN_LOCATION_Y, _horizPinLocationY);
		_state.put(STATE_VERT_TOOL_TIP_PIN_LOCATION_X, _vertPinLocationX);
		_state.put(STATE_VERT_TOOL_TIP_PIN_LOCATION_Y, _vertPinLocationY);
	}

	/**
	 * Set shell which is currently be visible.
	 * 
	 * @param rrShell
	 */
	private void setCurrentVisibleShell(final AbstractRRShell rrShell) {

		_visibleRRShell = rrShell;
		_visibleShell = rrShell.getShell();
	}

	protected void setIsShellToggle() {
		_isShellToggled = true;
	}

	protected void setShellFadeInSteps(final int shellFadeInSteps) {
		_shellFadeInSteps = shellFadeInSteps;
	}

	protected void setShellFadeOutDelaySteps(final int shellFadeOutDelaySteps) {
		_shellFadeOutDelaySteps = shellFadeOutDelaySteps;
	}

	protected void setShellFadeOutSteps(final int shellFadeOutSteps) {
		_shellFadeOutSteps = shellFadeOutSteps;
	}

	protected void setShellLocation(final int diffX, final int diffY) {

		final Rectangle shellBounds = _visibleShell.getBounds();

		final Point size = new Point(shellBounds.width, shellBounds.height);
		final Point newShellLocation = new Point(shellBounds.x - diffX, shellBounds.y - diffY);

		final Point fixedLocation = fixupDisplayBounds(size, newShellLocation);

		_visibleRRShell.setShellLocation(fixedLocation.x, fixedLocation.y, 4);
	}

	protected void setShellMoveSteps(final int shellMoveSteps) {
		_shellMoveSteps = shellMoveSteps;
	}

	private void setShellVisible(final boolean isVisible) {

//		final Rectangle shellBounds = _visibleShell.getBounds();

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tsetShellVisible: " + isVisible)+("\t"+shellBounds));
		// TODO remove SYSTEM.OUT.PRINTLN

		_visibleShell.setVisible(isVisible);

		if (isVisible) {

			ttDisplayAddListener();

		} else {

			ttDisplayRemoveListener();
		}
	}

	protected abstract void setToolTipPinned(boolean isToolTipPinned);

	/**
	 * Set location where the tooltip is pinned.
	 */
	protected void setToolTipPinnedLocation() {

		final Point contentLocation = _visibleRRShell.getShellContentLocation();

		if (isVerticalLayout()) {
			_vertPinLocationX = contentLocation.x;
			_vertPinLocationY = contentLocation.y;
		} else {
			_horizPinLocationX = contentLocation.x;
			_horizPinLocationY = contentLocation.y;
		}

		/*
		 * end location must be set that a new animation finds the correct position where it should
		 * be moved
		 */
		_shellEndLocation.x = contentLocation.x;
		_shellEndLocation.y = contentLocation.y;
	}

	protected void showAtDefaultLocation() {

		if (_visibleShell == null || _visibleShell.isDisposed()) {
			return;
		}

		final Point size = _visibleShell.getSize();
		final Point fixedLocation = fixupDisplayBounds(size, getToolTipLocation(size));

		_visibleRRShell.setShellLocation(fixedLocation.x, fixedLocation.y, 4);
	}

	protected boolean showShell() {

		/*
		 * show tooltip only when this is the active shell, this check is necessary that when a tour
		 * chart is opened in a dialog (e.g. adjust altitude) that a hidden tour chart tooltip in
		 * the tour chart view is also displayed
		 */
//		if (_display.getActiveShell() != _ownerControl.getShell() || _ownerControl.isVisible() == false) {
//			return false;
//		}

		createUI();

		ttShow();

		return true;
	}

	private void showShellWhenVisible() {

		if (_visibleShell == null || _visibleShell.isDisposed() || _visibleShell.isVisible() == false) {
			return;
		}

		ttShow();
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
	private void ttAllControlsAddListener(final Control control) {

		control.addListener(SWT.KeyDown, _ttAllControlsListener);

		control.addListener(SWT.MouseDown, _ttAllControlsListener);
		control.addListener(SWT.MouseUp, _ttAllControlsListener);
		control.addListener(SWT.MouseMove, _ttAllControlsListener);
		control.addListener(SWT.MouseExit, _ttAllControlsListener);
		control.addListener(SWT.MouseEnter, _ttAllControlsListener);

		if (control instanceof Composite) {
			final Control[] children = ((Composite) control).getChildren();
			for (final Control child : children) {
				ttAllControlsAddListener(child);
			}
		}
	}

	/**
	 * ########################### Recursive #########################################<br>
	 * <p>
	 * Removes listener from all controls within the tooltip
	 * <p>
	 * ########################### Recursive #########################################<br>
	 * 
	 * @param control
	 */
	private void ttAllControlsRemoveListener(final Control control) {

		control.removeListener(SWT.KeyDown, _ttAllControlsListener);

		control.removeListener(SWT.MouseDown, _ttAllControlsListener);
		control.removeListener(SWT.MouseUp, _ttAllControlsListener);
		control.removeListener(SWT.MouseMove, _ttAllControlsListener);
		control.removeListener(SWT.MouseExit, _ttAllControlsListener);
		control.removeListener(SWT.MouseEnter, _ttAllControlsListener);

		if (control instanceof Composite) {
			final Control[] children = ((Composite) control).getChildren();
			for (final Control child : children) {
				ttAllControlsRemoveListener(child);
			}
		}
	}

	private void ttDisplayAddListener() {

		if (_isDisplayListenerSet) {
			return;
		}

		_display.addFilter(SWT.MouseMove, _ttDisplayListener);

		_isDisplayListenerSet = true;
	}

	private void ttDisplayRemoveListener() {

		if (_isDisplayListenerSet == false) {
			return;
		}

		_display.removeFilter(SWT.MouseMove, _ttDisplayListener);

		_isDisplayListenerSet = false;
	}

	private void ttDispose(final Event event) {

		if (_visibleShell == null || _visibleShell.isDisposed()) {
			return;
		}

		// hide tooltip definitively

		ownerShellRemoveListener();
		ttDisplayRemoveListener();

		passOnEvent(_rrShellWithResize.getShell(), event);
		_rrShellWithResize.dispose();

		passOnEvent(_rrShellNoResize.getShell(), event);
		_rrShellNoResize.dispose();
	}

	/**
	 * Hide current shell immediatedly without animation.
	 */
	protected void ttHide() {

		closeInternalShells();

		if (canCloseShell() == false) {
			return;
		}

		_visibleShell.setAlpha(0);

		// hide shell
		setShellVisible(false);

		_isShellFadingIn = false;
		_isShellFadingOut = false;

		_isDoNotStopAnimation = false;
	}

	private void ttHide(final Event event) {

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\tttHide"));
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (_visibleShell == null || _visibleShell.isDisposed() || _visibleShell.isVisible() == false) {
			return;
		}

		closeInternalShells();

		if (canCloseShell() == false) {
			return;
		}

		beforeHideToolTip();

		_isDoNotStopAnimation = false;

		if (_isShellFadingOut) {

			// shell is already fading out
			return;
		}

		// shell is not yet fading out

		_isShellFadingIn = false;
		_isShellFadingOut = true;

		animation10_Start();
	}

	private void ttShellAddListener(final Shell shell) {
		// hide tooltip if user selects outside of the shell
		shell.addListener(SWT.Deactivate, _ttShellListener);
		shell.addListener(SWT.Dispose, _ttShellListener);
	}

	private void ttShow() {

		// shell is not yet fading in

		_isShellFadingIn = true;
		_isShellFadingOut = false;

		animation10_Start();
	}

	private void updateUI_Colors() {

		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		final Color fgColor = getShellColor_Foreground(colorRegistry);
		final Color bgColor = getShellColor_Background(colorRegistry);

		_rrShellNoResize.updateColors(fgColor, bgColor);
		_rrShellWithResize.updateColors(fgColor, bgColor);
	}
}
