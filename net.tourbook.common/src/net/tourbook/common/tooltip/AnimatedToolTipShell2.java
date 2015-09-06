/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

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
 * Fade in/out a tooltip, location is not animated.
 */
public abstract class AnimatedToolTipShell2 {

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

	private static final int			AUTO_CLOSE_INTERVAL						= 700;

	private static final int			ALPHA_OPAQUE							= 0xff;

	private OwnerControlListener		_ownerControlListener;

	private OwnerShellListener			_ownerShellListener;
	private ToolTipShellListener		_ttShellListener;
	private ToolTipAllControlsListener	_ttAllControlsListener;
	private ToolTipDisplayFilter		_ttDisplayFilter;

	/**
	 * Keep track of added display listener that no more than <b>1</b> is set.
	 */
	private boolean						_isDisplayFilterActive;

	/**
	 * Is <code>true</code> when shell is fading out, otherwise <code>false</code>.
	 */
	private boolean						_isShellFadingOut;

	/**
	 * Is <code>true</code> when shell is fading in, otherwise <code>false</code>.
	 */
	private boolean						_isShellFadingIn;

	/**
	 * When <code>true</code> (default) the shell trim style is used to create the tooltip shell
	 * which creates a border. Set <code>false</code> to hide the border.
	 */
	private boolean						_isShowShellTrimStyle					= true;

	private int							_fadeOutDelayCounter;

	private final AnimationTimer		_animationTimer;

	private ToolTipAutoCloseTimer		_ttAutoCloseTimer;

	private boolean						_isReceiveOnMouseMove;

	private int							_fadeInSteps							= FADE_IN_STEPS;
	private int							_fadeInDelayCounter;

	/**
	 * Number of {@link #FADE_IN_STEPS} until the tooltip starts to fade in.
	 */
	private int							_fadeInDelaySteps;

	private int							_fadeOutSteps							= FADE_OUT_STEPS;
	private int							_fadeOutDelaySteps						= FADE_OUT_DELAY_STEPS;
	private int							_mouseOverBehaviour						= MOUSE_OVER_BEHAVIOUR_NO_IGNORE;

	private boolean						_isKeepToolTipOpenWhenResizedOrMoved	= true;

	/*
	 * UI resources
	 */
	private Display						_display;

	/**
	 * Tooltip shell which is currently visible
	 */
	private Shell						_currentShell;

	private ArrayList<ShellWrapper>		_oldShells								= new ArrayList<ShellWrapper>();
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

	private class ShellWrapper {

		Shell	__shell;
		int		__fadeOutDelayCounter;

		public ShellWrapper(final Shell shell) {
			__shell = shell;
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

	/**
	 * This checks if a mouse is above a tooltip. When mouse is above the 3D map or outside of this
	 * application, mouse events are not captured and a tooltip keeps opened until other actions are
	 * done.
	 */
	private final class ToolTipAutoCloseTimer implements Runnable {
		@Override
		public void run() {
			onTTAutoCloseTimer();
		}
	}

	private class ToolTipDisplayFilter implements Listener {
		@Override
		public void handleEvent(final Event event) {

			switch (event.type) {
			case SWT.MouseMove:
				onDisplayFilterMouseMove();
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
	public AnimatedToolTipShell2(final Control ownerControl) {

		_ownerControl = ownerControl;
		_display = _ownerControl.getDisplay();

		_ttAllControlsListener = new ToolTipAllControlsListener();
		_ttShellListener = new ToolTipShellListener();
		_ttDisplayFilter = new ToolTipDisplayFilter();

		_ownerControlListener = new OwnerControlListener();
		_ownerShellListener = new OwnerShellListener();

		_animationTimer = new AnimationTimer();
		_ttAutoCloseTimer = new ToolTipAutoCloseTimer();

		addOwnerControlListener();
		addOwnerShellListener();
	}

	private void addDisplayFilterListener() {

		if (_isDisplayFilterActive) {
			return;
		}

		_display.addFilter(SWT.MouseMove, _ttDisplayFilter);

		_isDisplayFilterActive = true;
	}

	/**
	 * Activate tooltip support for this control
	 */
	private void addOwnerControlListener() {

		removeOwnerControlsListener();

		_ownerControl.addListener(SWT.Dispose, _ownerControlListener);
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

		int a = 0;
		a++;
		a++;

		if (a == 1) {
			animation10_Start_Simple();
		} else {
			animation10_StartKomplex();
		}
	}

	private void animation10_Start_Simple() {

		if (_isShellFadingIn) {

			// show tool tip

			final Point size = _currentShell.getSize();
			final Point defaultLocation = getToolTipLocation(size);
			final Point shellLocation = fixupDisplayBounds(size, defaultLocation);
			_currentShell.setLocation(shellLocation.x, shellLocation.y);

			_currentShell.setAlpha(0xff);

			setShellVisible(true);

		} else {

			// hide tooltip

			setShellVisible(false);
		}

		closeOldShells(false);
	}

	private void animation10_StartKomplex() {

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tanimation10_StartKomplex"));
//		// TODO remove SYSTEM.OUT.PRINTLN

//		final long start = System.nanoTime();

		if (_isShellFadingIn) {

			final boolean isShellVisible = _currentShell.isVisible();

			if (isShellVisible) {

				// shell is already fading in

				return;

			} else {

				// shell is not visible, do fading animation

				_fadeInDelayCounter = 0;

				// set location
				final Point size = _currentShell.getSize();
				final Point defaultLocation = getToolTipLocation(size);
				final Point shellLocation = fixupDisplayBounds(size, defaultLocation);

				_currentShell.setAlpha(0);
				_currentShell.setLocation(shellLocation.x, shellLocation.y);

				setShellVisible(true);
			}

		} else if (_isShellFadingOut) {

			// fading out has no movement

			_fadeOutDelayCounter = 0;
		}

		// start animation now
		animation20_Runnable();
	}

	private void animation20_Runnable() {

//		final long start = System.nanoTime();

		if (isShellHidden()) {
			return;
		}

		try {
			/*
			 * endAlpha will be the final fadeIn/fadeOut value when the animation stops
			 */
			int finalFadeAlpha = -1;

			int currentAlpha = _currentShell.getAlpha();
			boolean isLoopBreak = false;

			while (true) {

				int newAlpha = -1;

				if (_isShellFadingIn) {

					if (_fadeInDelayCounter++ < _fadeInDelaySteps) {

						// delay fade in

						_display.timerExec(FADE_TIME_INTERVAL, _animationTimer);

						return;
					}

					if (_fadeInSteps <= 0) {
						newAlpha = ALPHA_OPAQUE;
					} else {

						final int diffAlpha = ALPHA_OPAQUE / _fadeInSteps;

						newAlpha = currentAlpha + diffAlpha;
						if (newAlpha > ALPHA_OPAQUE) {
							newAlpha = ALPHA_OPAQUE;
						}
					}

					finalFadeAlpha = ALPHA_OPAQUE;

					if (currentAlpha == ALPHA_OPAQUE) {

						// target is reached and fully visible, stop animation

						_isShellFadingIn = false;

						return;
					}

				} else if (_isShellFadingOut) {

					if (_fadeOutDelayCounter++ < _fadeOutDelaySteps) {

						// delay fade out

						_display.timerExec(FADE_TIME_INTERVAL, _animationTimer);

						return;
					}

					if (_fadeInSteps <= 0 || _fadeOutSteps <= 0) {

						newAlpha = 0;

					} else {

						final int alphaDiff = ALPHA_OPAQUE / _fadeOutSteps;

						newAlpha = currentAlpha - alphaDiff;
						finalFadeAlpha = 0;
					}

					if (newAlpha <= 0) {

						// shell is not visible any more, hide it now

						_currentShell.setAlpha(0);

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
						_currentShell.setAlpha(newAlpha);
					}

					if (_currentShell.getAlpha() != newAlpha) {

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

			closeOldShells(true);
		}
	}

	/**
	 * Is called before the tooltip shell is set hidden.
	 */
	protected abstract void beforeHideToolTip();

	/**
	 * Is called before the tooltip is displayed.
	 * 
	 * @return Returns <code>true</code> when the tooltip should be opened, otherwise
	 *         <code>false</code>.
	 */
	protected abstract boolean canShowToolTip();

	/**
	 * @return Returns <code>true</code> when the tooltip should not be closed.
	 */
	private boolean checkShowOrHide() {

		//		final long start = System.nanoTime();

		int hideFlag = 0;
		int showFlag = 0;

		try {

			final boolean isShellHidden = isShellHidden();
			if (isShellHidden) {

				hideFlag |= 0b1; // 1

				return false;
			}

			if (isDoNotClose()) {

				showFlag |= 0b1; // 1

				return true;
			}

			// get control which is hovered with the mouse after the exit, can be null
			final Control hoveredControl = _display.getCursorControl();

			//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
			//				+ ("\thoveredControl: " + hoveredControl));
			//		// TODO remove SYSTEM.OUT.PRINTLN

			if (hoveredControl == null) {

				hideFlag |= 0b10; // 2

			} else {

				/*
				 * check if the hovered control is the owner control, if not, hide the tooltip
				 */
				Control hoveredParent = hoveredControl;

				// move up child-parent hierarchy until shell is reached
				while (true) {

					//					System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
					//							+ ("\t\thoveredParent: " + hoveredParent));
					//					// TODO remove SYSTEM.OUT.PRINTLN

					if (hoveredParent == _currentShell) {

						// mouse is hovering in this tooltip

						showFlag |= 0b100; // 3

						break;
					}

					if (hoveredParent == _ownerControl) {

						// mouse is hovering the owner control

						if (_mouseOverBehaviour == MOUSE_OVER_BEHAVIOUR_NO_IGNORE) {

							/*
							 * owner is not ignored, which means when the mouse is hovered the
							 * owner, the tooltip keeps opened, this is the default
							 */

							showFlag |= 0b1000; // 4

						} else {

							// MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER

							hideFlag |= 0b1000; // 4
						}

						break;
					}

					hoveredParent = hoveredParent.getParent();

					if (hoveredParent == null) {

						// mouse has left the tooltip and the owner control

						hideFlag = 0b10000; // 5

						break;
					}
				}
			}

			/**
			 * !!! this adjustment do not work on Linux because the tooltip gets hidden when the
			 * mouse tries to hover over the tooltip <br>
			 * <br>
			 * it seems to work on windows and linux with margin 1, when set to 0 the tooltip do
			 * sometime not be poped up again and the i-icons is not deaktivated<br>
			 * wolfgang 2010-07-23
			 */

			final Rectangle ttShellRect = _currentShell.getBounds();
			final int margin = 10;

			ttShellRect.x -= margin;
			ttShellRect.y -= margin;
			ttShellRect.width += 2 * margin;
			ttShellRect.height += 2 * margin;

			final Point displayCursorLocation = _display.getCursorLocation();

			final boolean isMouseInTooltipRect = ttShellRect.contains(displayCursorLocation);
			if (isMouseInTooltipRect) {
				showFlag |= 0b10_0000; // 6
			} else {
				hideFlag |= 0b10_0000; // 6
			}

			if (isMouseInTooltipRect && _isShellFadingOut) {

				//			System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
				//					+ ("\tisInTooltip && _isShellFadingOut"));
				//			// TODO remove SYSTEM.OUT.PRINTLN

				// don't hide when mouse is hovering hiding tooltip

				showFlag |= 0b100_0000; // 7

				ttShow();

				//			} else if (isMouseInTooltip && _isShellFadingIn && _fadeInDelayCounter < _fadeInDelaySteps) {
				//
				//				/*
				//				 * Mouse is in the tooltip, fading in but still in the delay steps ->
				//				 */
				//
				//
				//				ttHide();
				//
			} else if (hideFlag > 0) {

				// check no hide area

				if (isInNoHideArea(displayCursorLocation)) {

					showFlag |= 0b1000_0000; // 8

				} else {

					// hide definitively

					ttHide();

					hideFlag |= 0b100_0000; // 7
				}
			}

		} finally {

//			System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//					+ ("\thideFlag: " + Integer.toBinaryString(hideFlag))
//					+ ("\tshowFlag: " + Integer.toBinaryString(showFlag))
//					+ "\n"
//			//
//					);
//			// TODO remove SYSTEM.OUT.PRINTLN

		}

		return showFlag > 0;
	}

	/**
	 * Close tooltip immediatedly without animation.
	 */
	public void close() {

		if (_currentShell != null && _currentShell.isDisposed() == false) {

			_currentShell.close();
			_currentShell = null;
		}

		for (final ShellWrapper oldShellWrapper : _oldShells) {

			final Shell oldShell = oldShellWrapper.__shell;

			if (oldShell != null && oldShell.isDisposed() == false) {
				oldShell.close();
			}
		}

		_oldShells.clear();
	}

	/**
	 * @param isWithFadeOut
	 *            When <code>true</code>, first the shell is faded out and then it's disposed.
	 */
	private void closeOldShells(final boolean isWithFadeOut) {

		if (_oldShells.size() == 0) {
			// nothing to do
			return;
		}

		if (isWithFadeOut) {

			// fade out and then dispose

			final ArrayList<ShellWrapper> disposedShells = new ArrayList<ShellWrapper>();

			for (final ShellWrapper oldShellWrapper : _oldShells) {

				final Shell oldShell = oldShellWrapper.__shell;

				try {

					int newAlpha;
					final int currentAlpha = oldShell.getAlpha();

					if (_fadeOutSteps <= 0) {

						newAlpha = 0;

					} else if (oldShellWrapper.__fadeOutDelayCounter++ < _fadeOutDelaySteps) {

						continue;

					} else {

						final int alphaDiff = ALPHA_OPAQUE / _fadeOutSteps;

						newAlpha = currentAlpha - alphaDiff;
					}

					if (newAlpha <= 0) {

						// shell is not visible any more

						oldShell.dispose();
						disposedShells.add(oldShellWrapper);

						continue;
					}

					oldShell.setAlpha(newAlpha);

					if (oldShell.getAlpha() != newAlpha) {

						// platform do not support shell alpha, this occured on Ubuntu 12.04

						oldShell.dispose();
						disposedShells.add(oldShellWrapper);

						continue;
					}

				} catch (final Exception err) {
					StatusUtil.log(err);
				}
			}

			_oldShells.removeAll(disposedShells);

			if (_oldShells.size() > 0) {
				// there are still visible shells
				_display.timerExec(FADE_TIME_INTERVAL, _animationTimer);
			}

		} else {

			// dispose all

			for (final ShellWrapper oldShellWrapper : _oldShells) {
				oldShellWrapper.__shell.dispose();
			}
			_oldShells.clear();
		}
	}

	/**
	 * Creates the content area of the the tooltip.
	 * 
	 * @param shell
	 *            the parent of the content area
	 * @return the content area created
	 */
	protected abstract Composite createToolTipContentArea(Composite shell);

	/**
	 * Create a shell but do not display it
	 * 
	 * @return Returns <code>true</code> when shell is created.
	 */
	private void createUI() {

		if (_currentShell != null && _currentShell.isDisposed() == false) {

			// hide old shell
			_oldShells.add(new ShellWrapper(_currentShell));
		}

		final int trimStyle = _isShowShellTrimStyle ? 0 : SWT.NO_TRIM;

		/*
		 * create new shell
		 */
		final int shellStyle = SWT.ON_TOP //
				/*
				 * SWT.TOOL must be disabled that NO_FOCUS is working !!!
				 */
//				| SWT.TOOL
//				| SWT.RESIZE
				| SWT.NO_FOCUS
				| trimStyle;

		_currentShell = new Shell(_ownerControl.getShell(), shellStyle);

		_currentShell.setLayout(new FillLayout());

		addTTShellListener(_currentShell);

		// create ui
		createToolTipContentArea(_currentShell);

		_currentShell.layout();
		_currentShell.pack(true);

		addTTAllControlsListener(_currentShell);
	}

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
	 * Get tooltip location.
	 * 
	 * @param size
	 *            Tooltip size
	 * @return Returns location relative to the device.
	 */
	protected abstract Point getToolTipLocation(Point size);

	/**
	 * Hide the currently active tool tip
	 */
	public void hide() {
		ttHide();
	}

	/**
	 * Hide the currently active tool tip without delay.
	 */
	public void hideNow() {

		if (isShellHidden()) {
			return;
		}

		_currentShell.setAlpha(0);

		// hide shell
		setShellVisible(false);

		closeOldShells(false);
	}

	/**
	 * @return When returning <code>true</code> then the tooltip will <b>not</b> be closed. This can
	 *         be used when the tooltip opens another dialog which prevents to close this tooltip,
	 *         default is <code>true</code>.
	 */
	protected boolean isDoNotClose() {
		return false;
	}

	/**
	 * @param displayCursorLocation
	 * @return When the returned rectangle (which has display locations) is hit by the mouse, the
	 *         tooltip should not be hidden. When <code>null</code> this check is ignored.
	 */
	protected boolean isInNoHideArea(final Point displayCursorLocation) {
		return false;
	}

	/**
	 * @return Return <code>true</code> when the tooltip shell is <code>null</code>, disposed or not
	 *         visible.
	 */
	private boolean isShellHidden() {

		final boolean isShellHidden = _currentShell == null
				|| _currentShell.isDisposed()
				|| _currentShell.isVisible() == false;

		return isShellHidden;
	}

	/**
	 * @return Returns <code>true</code> when the tooltip is opened but is started to close it.
	 */
	public boolean isTooltipClosing() {

		return _isShellFadingOut && isToolTipVisible();
	}

	public boolean isToolTipVisible() {

		if (_currentShell == null || _currentShell.isDisposed()) {
			return false;
		}

		final boolean isShellVisible = _currentShell.isVisible();

		return isShellVisible;
	}

	/**
	 */
	private void onDisplayFilterMouseMove() {

		checkShowOrHide();
	}

	private void onDispose(final Event event) {

		if (_currentShell == null || _currentShell.isDisposed()) {
			return;
		}

		// hide tooltip definitively

		removeOwnerShellListener();
		removeDisplayFilter();

		// deactivate auto close timer
		_display.timerExec(-1, _ttAutoCloseTimer);

		_currentShell.dispose();

		closeOldShells(false);
	}

	protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {}

	private void onOwnerControlEvent(final Event event) {

		if (_ownerControl == null || _ownerControl.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.Dispose:

			onDispose(event);

			removeOwnerControlsListener();

			break;

		case SWT.Resize:

			showShellWhenVisible();

			break;
		}
	}

	private void onOwnerShellEvent(final Event event) {

		if (_currentShell == null || _currentShell.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.Deactivate:

			_display.asyncExec(new Runnable() {

				@Override
				public void run() {

					// hide tooltip when another shell is activated

					if (_display.getActiveShell() != _currentShell) {
						ttHide();
					}
				}
			});
			break;

		case SWT.Move:

			showShellWhenVisible();

			break;
		}
	}

	private void onTTAllControlsEvent(final Event event) {

		if (_currentShell == null || _currentShell.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.KeyDown:

			if (event.keyCode == SWT.ESC) {
				ttHide();
			}

			break;

		case SWT.MouseEnter:

			if (_isShellFadingIn || _isShellFadingOut) {

				// stop animation
//				_isShellFadingIn = _isShellFadingOut = false;
			}

			break;

		case SWT.MouseExit:

			break;

		case SWT.MouseMove:

			if (_isReceiveOnMouseMove) {

				final Widget widget = event.widget;

				if (widget instanceof Control) {

					final Point ttDisplayLocation = ((Control) widget).toDisplay(event.x, event.y);
					final Point ownerLocation = _ownerControl.toControl(ttDisplayLocation);

					_display.asyncExec(new Runnable() {
						@Override
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

	private void onTTAutoCloseTimer() {

		final boolean isKeepOpened = checkShowOrHide();

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tonTTAutoCloseTimer - isKeepOpened: " + isKeepOpened));
//// TODO remove SYSTEM.OUT.PRINTLN

		if (isKeepOpened) {

			// start again to check again
			_display.timerExec(AUTO_CLOSE_INTERVAL, _ttAutoCloseTimer);
		}
	}

	private void onTTShellEvent(final Event event) {

		switch (event.type) {
		case SWT.Deactivate:

			if (_currentShell != null
					&& !_currentShell.isDisposed()
					&& _ownerControl != null
					&& !_ownerControl.isDisposed()) {

				_display.asyncExec(new Runnable() {

					@Override
					public void run() {

						// hide tooltip when another shell is activated

						// check again
						if (_currentShell == null
								|| _currentShell.isDisposed()
								|| _ownerControl == null
								|| _ownerControl.isDisposed()) {
							return;
						}

						if (// don't hide when ...

						// ... main window is active
						_ownerControl.getShell() == _currentShell.getDisplay().getActiveShell()

						// ... a sub shell is opened
								|| isDoNotClose()) {

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

	private void removeDisplayFilter() {

		if (_isDisplayFilterActive == false) {
			return;
		}

		_display.removeFilter(SWT.MouseMove, _ttDisplayFilter);

		_isDisplayFilterActive = false;
	}

	/**
	 * Deactivate tooltip support for the underlying control
	 */
	private void removeOwnerControlsListener() {

		_ownerControl.removeListener(SWT.Dispose, _ownerControlListener);
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

	public void setFadeInDelayTime(final int delayTimeInMS) {
		_fadeInDelaySteps = delayTimeInMS / FADE_TIME_INTERVAL;
	}

	public void setFadeInSteps(final int fadeInSteps) {
		_fadeInSteps = fadeInSteps;
	}

	public void setFadeOutDelaySteps(final int fadeOutDelaySteps) {
		_fadeOutDelaySteps = fadeOutDelaySteps;
	}

	public void setFadeOutSteps(final int fadeOutSteps) {
		_fadeOutSteps = fadeOutSteps;
	}

	public void setIsKeepShellOpenWhenMoved(final boolean isKeepShellOpenWhenMoved) {
		_isKeepToolTipOpenWhenResizedOrMoved = isKeepShellOpenWhenMoved;
	}

	/**
	 * When <code>true</code> (default) the shell trim style is used to create the tooltip shell
	 * which creates a border. Set <code>false</code> to hide the border.
	 */
	public void setIsShowShellTrimStyle(final boolean isShowShellTrimStyle) {
		_isShowShellTrimStyle = isShowShellTrimStyle;
	}

	public void setReceiveMouseMoveEvent(final boolean isReceive) {
		_isReceiveOnMouseMove = isReceive;
	}

	private void setShellVisible(final boolean isVisible) {

		if (isVisible) {

			// show tooltip

			_currentShell.setVisible(true);

			addDisplayFilterListener();

		} else {

			// hide tooltip

			beforeHideToolTip();

			_currentShell.setVisible(false);

			removeDisplayFilter();

			close();
		}
	}

	private void showShellWhenVisible() {

		if (_isKeepToolTipOpenWhenResizedOrMoved == false) {
			return;
		}

		if (isShellHidden()) {
			return;
		}

		ttShow();
	}

	protected void showToolTip() {

		createUI();

		ttShow();
	}

	private void ttHide() {

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\tttHide"));
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (isShellHidden()) {
			return;
		}

		if (_fadeInDelayCounter < _fadeInDelaySteps) {

			// fade in have not yet started -> hide shell
			setShellVisible(false);

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

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\tttShow"));
//		// TODO remove SYSTEM.OUT.PRINTLN

		// shell is not yet fading in

		if (canShowToolTip() == false) {

			return;
		}

		// ensure tooltip is closed when mouse is not hovering the tooltip or application
		_display.timerExec(AUTO_CLOSE_INTERVAL, _ttAutoCloseTimer);

		_isShellFadingIn = true;
		_isShellFadingOut = false;

		animation10_Start();
	}

}
