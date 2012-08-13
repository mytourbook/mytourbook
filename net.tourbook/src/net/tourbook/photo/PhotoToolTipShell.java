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

import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
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
public abstract class PhotoToolTipShell implements IExternalGalleryListener {

	/**
	 * how long each tick is when fading out (in ms)
	 */
	private static final int		FADE_TIME_INTERVAL					= 10;

	/**
	 * Number of steps when fading out
	 */
	private static final int		FADE_OUT_STEPS						= 40;
	/**
	 * Number of steps when fading out
	 */
	private static final int		FADE_OUT_DELAY_STEPS				= 30;

	/**
	 * Number of steps when fading in
	 */
	private static final int		FADE_IN_STEPS						= 20;

	private static final int		MOVE_STEPS							= 20;

	private static final int		ALPHA_OPAQUE						= 0xff;

	private static final int		RESIZE_BOX_SIZE						= 10;

	private static final String		STATE_PHOTO_HORIZ_TOOL_TIP_WIDTH	= "STATE_PHOTO_HORIZ_TOOL_TIP_WIDTH";	//$NON-NLS-1$
	private static final String		STATE_PHOTO_HORIZ_TOOL_TIP_HEIGHT	= "STATE_PHOTO_HORIZ_TOOL_TIP_HEIGHT";	//$NON-NLS-1$
	private static final String		STATE_PHOTO_VERT_TOOL_TIP_WIDTH		= "STATE_PHOTO_VERT_TOOL_TIP_WIDTH";	//$NON-NLS-1$
	private static final String		STATE_PHOTO_VERT_TOOL_TIP_HEIGHT	= "STATE_PHOTO_VERT_TOOL_TIP_HEIGHT";	//$NON-NLS-1$

	private static final int		MIN_SHELL_HORIZ_HEIGHT				= 60;
	private static final int		MIN_SHELL_HORIZ_WIDTH				= 100;
	private static final int		MIN_SHELL_VERT_HEIGHT				= 150;
	private static final int		MIN_SHELL_VERT_WIDTH				= 100;

	private Shell					_ttShell;

	private Control					_ownerControl;

	private OwnerControlListener	_ownerControlListener;
	private OwnerShellListener		_ownerShellListener;

	private ToolTipShellListener	_ttShellListener;
	private ToolTipControlListener	_ttControlListener;

	private boolean					_isShellToggled;
	private boolean					_isShellResized;
	private boolean					_isHitLeftResizeBox;
	private int						_resizeBoxPixel;

	private boolean					_isShellFadingOut;
	private boolean					_isShellFadingIn;
	private Point					_shellStartLocation;
	private Point					_shellEndLocation;
	private int						_animationStepCounter;
	private int						_fadeOutDelayCounter;
	private boolean					_isShellMovingEnabled;

	private int						_mouseDownX;
	private int						_mouseDownY;

	private int						_shellHorizWidth					= MIN_SHELL_HORIZ_WIDTH;
	private int						_shellHorizHeight					= MIN_SHELL_HORIZ_HEIGHT;
	private int						_shellVertWidth						= MIN_SHELL_VERT_WIDTH;
	private int						_shellVertHeight					= MIN_SHELL_VERT_HEIGHT;

	private Display					_display;

	private final AnimationTimer	_animationTimer;

	/*
	 * UI resources
	 */
	private Cursor					_cursor_NE_SW;
	private Cursor					_cursor_NW_SE;

	private ImageGallery			_imageGallery;

	private final class AnimationTimer implements Runnable {
		@Override
		public void run() {
			animation20_Runnable();
		}
	}

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
	public PhotoToolTipShell(final Control ownerControl) {

		_ownerControl = ownerControl;
		_display = _ownerControl.getDisplay();

		_ttControlListener = new ToolTipControlListener();
		_ttShellListener = new ToolTipShellListener();

		_ownerControlListener = new OwnerControlListener();
		_ownerShellListener = new OwnerShellListener();

		_animationTimer = new AnimationTimer();

		ownerControlAddListener();

		initUI(ownerControl);
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
		control.addListener(SWT.MouseEnter, _ttControlListener);

		if (control instanceof Composite) {
			final Control[] children = ((Composite) control).getChildren();
			for (final Control child : children) {
				addToolTipControlListener(child);
			}
		}
	}

	/**
	 * This is called after the shell and content area are created.
	 * 
	 * @param shell
	 */
	protected abstract void afterCreateShell(Shell shell);

	private void animation10_Start() {

		// fading out has no movement

		if (_isShellFadingIn) {

			// set fading in location

			final Point size = _ttShell.getSize();

			_shellEndLocation = fixupDisplayBounds(size, getLocation(size));

			if (_ttShell.isVisible()) {

				// shell is already visible, move from the current position to the target position

				_shellStartLocation = _ttShell.getLocation();

			} else {

				// shell is not visible, set position directly without moving animation, do only fading animation

				_shellStartLocation = _shellEndLocation;

				_ttShell.setLocation(_shellStartLocation);

				_ttShell.setVisible(true);
			}

			_animationStepCounter = 0;

		}
		if (_isShellFadingOut) {

			_fadeOutDelayCounter = 0;
		}

		_isShellMovingEnabled = true;

		_display.timerExec(FADE_TIME_INTERVAL, _animationTimer);
	}

	private void animation20_Runnable() {

		try {

			if (_ttShell == null || _ttShell.isDisposed() || _ttShell.isVisible() == false) {
				return;
			}

			final int currentAlpha = _ttShell.getAlpha();
			int newAlpha = ALPHA_OPAQUE;

			if (_isShellFadingIn) {

				final int shellStartX = _shellStartLocation.x;
				final int shellStartY = _shellStartLocation.y;
				final int shellEndX = _shellEndLocation.x;
				final int shellEndY = _shellEndLocation.y;

				final Point shellCurrentLocation = _ttShell.getLocation();

				final boolean isInTarget = shellCurrentLocation.x == shellEndX && shellCurrentLocation.y == shellEndY;

				final int diffAlpha = ALPHA_OPAQUE / FADE_IN_STEPS;

				newAlpha = currentAlpha + diffAlpha;
				if (newAlpha > ALPHA_OPAQUE) {
					newAlpha = ALPHA_OPAQUE;
				}

				if (isInTarget && newAlpha == ALPHA_OPAQUE) {

					// target is reached and fully visible, stop animation

					_ttShell.setAlpha(ALPHA_OPAQUE);

					_isShellFadingIn = false;

					return;

				} else {

					// move to target

					_animationStepCounter++;

					final int diffX = shellStartX - shellEndX;
					final int diffY = shellStartY - shellEndY;

					final double moveX = (double) diffX / MOVE_STEPS * _animationStepCounter;
					final double moveY = (double) diffY / MOVE_STEPS * _animationStepCounter;

					final int shellCurrentX = (int) (shellStartX - moveX);
					final int shellCurrentY = (int) (shellStartY - moveY);

					if (_isShellMovingEnabled) {

						// when mouse is over this tooltip the shell is not moved

						_ttShell.setLocation(new Point(shellCurrentX, shellCurrentY));
					}

				}

			} else if (_isShellFadingOut) {

				if (_fadeOutDelayCounter++ < FADE_OUT_DELAY_STEPS) {

					// delay fade out

					_display.timerExec(FADE_TIME_INTERVAL, _animationTimer);

					return;
				}

				final int alphaDiff = ALPHA_OPAQUE / FADE_OUT_STEPS;

				newAlpha = currentAlpha - alphaDiff;

				if (newAlpha <= 0) {

					// shell is not visible any more, hide it now

					_ttShell.setAlpha(0);
					_ttShell.setVisible(false);

					_isShellFadingOut = false;

					return;
				}
			}

			_ttShell.setAlpha(newAlpha);

			_display.timerExec(FADE_TIME_INTERVAL, _animationTimer);

		} catch (final Exception err) {
			StatusUtil.log(err);
		}
	}

	/**
	 * Create a shell but do not display it
	 * 
	 * @return Returns <code>true</code> when shell is created.
	 */
	private void createShell() {

		if (_ttShell != null && !_ttShell.isDisposed()) {
			return;
		}

		_ttShell = new Shell(_ownerControl.getShell(), //
				SWT.ON_TOP //
						| SWT.TOOL
						| SWT.NO_FOCUS
//						| SWT.RESIZE
		//
		);

		_ttShell.setLayout(new FillLayout());

		// close tooltip if user selects outside of the shell
		_ttShell.addListener(SWT.Deactivate, _ttShellListener);
		_ttShell.addListener(SWT.Dispose, _ttShellListener);

		ownerShellAddListener();

		_cursor_NE_SW = new Cursor(_display, SWT.CURSOR_SIZENESW);
		_cursor_NW_SE = new Cursor(_display, SWT.CURSOR_SIZENWSE);

		// initialize resize behaviour
		_isShellResized = false;

		createToolTipContentArea(_ttShell);

		addToolTipControlListener(_ttShell);

		_imageGallery.setExternalMouseListener(this);

		_ttShell.pack();

		afterCreateShell(_ttShell);
	}

	/**
	 * Creates the content area of the the tooltip.
	 * 
	 * @param shell
	 *            the parent of the content area
	 * @return the content area created
	 */
	protected abstract Composite createToolTipContentArea(Composite shell);

	private Point fixupDisplayBounds(final Point tipSize, final Point location) {

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

	protected abstract Point getLocation(Point size);

	Point getShellSize() {
		if (isVerticalGallery()) {
			return new Point(_shellVertWidth, _shellVertHeight);
		} else {
			return new Point(_shellHorizWidth, _shellHorizHeight);
		}
	}

	protected Shell getToolTipShell() {
		return _ttShell;
	}

	/**
	 * Hide the currently active tool tip
	 */
	public void hide() {
		ttHide(null);
	}

	private void initUI(final Control ownerControl) {

		final PixelConverter pc = new PixelConverter(ownerControl);

		_resizeBoxPixel = pc.convertWidthInCharsToPixels(RESIZE_BOX_SIZE);
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

		final int mouseX = mousePos.x;
		final int mouseY = mousePos.y;

		_isHitLeftResizeBox = false;

		// check if mouse is within the resize box
		if (mouseX < _resizeBoxPixel && mouseY < _resizeBoxPixel) {

			// mouse is in the top left resize box

			cursor = _cursor_NW_SE;
			_isHitLeftResizeBox = true;

		} else if (mouseX > (shellWidth - _resizeBoxPixel) && mouseY < _resizeBoxPixel) {

			// mouse is in the top right resize box

			cursor = _cursor_NE_SW;
		}

		final boolean isHitResizeBox = cursor != null;
		boolean isHandled;

		if (eventType == SWT.MouseMove && _isShellResized) {

			// mouse is moved and shell is still resizing

			isHandled = true;

			final int diffX = _mouseDownX - mouseX;
			final int diffY = _mouseDownY - mouseY;

//			System.out.println("diffX " + diffX + "\tdiffY " + diffY);
//			// TODO remove SYSTEM.OUT.PRINTLN

			final int newShellX = shellX - diffX;
			final int newShellY = shellY - diffY;
			int newShellWidth = shellWidth + diffX;
			int newShellHeight = shellHeight + diffY;

			Point newShellLocation = new Point(newShellX, newShellY);

			final Rectangle displayBounds = getDisplayBounds(newShellLocation);

			// ensure tooltip is not too large
			final double maxHeight = displayBounds.height * 0.8;
			final double maxWidth = displayBounds.width * 0.95;

			if (newShellHeight > maxHeight) {
				newShellHeight = (int) maxHeight;
			} else if (newShellHeight < MIN_SHELL_HORIZ_HEIGHT) {
				newShellHeight = MIN_SHELL_HORIZ_HEIGHT;
			}

			if (newShellWidth > maxWidth) {
				newShellWidth = (int) maxWidth;
			} else if (newShellWidth < MIN_SHELL_HORIZ_WIDTH) {
				newShellWidth = MIN_SHELL_HORIZ_WIDTH;
			}

			final Point size = new Point(newShellWidth, newShellHeight);

			newShellLocation = fixupDisplayBounds(size, newShellLocation);

			_ttShell.setBounds(newShellLocation.x, newShellLocation.y, newShellWidth, newShellHeight);

			if (isVerticalGallery()) {
				_shellVertWidth = newShellWidth;
				_shellVertHeight = newShellHeight;
			} else {
				_shellHorizWidth = newShellWidth;
				_shellHorizHeight = newShellHeight;
			}

		} else {

			if (isHitResizeBox == false) {

				// mouse is not in the resizebox, do not handle mouse event externally

				isHandled = false;

			} else {

				// mouse is within the resizebox, handle mouse event externally

				isHandled = true;

				switch (eventType) {
				case SWT.MouseDown:

					if (_isShellResized) {

						// disable shell resize

						_isShellResized = false;

					} else {

						// enable shell resize

						_isShellResized = true;

						_mouseDownX = mouseX;
						_mouseDownY = mouseY;
					}

					break;

				case SWT.MouseUp:
					break;
				}
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

	abstract boolean isVerticalGallery();

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

		if (_ttShell == null || _ttShell.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.Deactivate:

			_display.asyncExec(new Runnable() {

				public void run() {

					// hide tooltip when another shell is activated

					if (_display.getActiveShell() != _ttShell) {
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

	@Override
	public void onPaintAfter(final GC gc, final Rectangle clippingArea, final Rectangle clientArea) {

		if (_isShellResized == false) {
			// shell is not resized, do normal behaviour
			return;
		}

		/*
		 * shell is resized, paint marker
		 */

		if (_isHitLeftResizeBox) {

			gc.setBackground(_display.getSystemColor(SWT.COLOR_GREEN));
			gc.fillRectangle(0, 0, _resizeBoxPixel, _resizeBoxPixel);

		} else {

			gc.setBackground(_display.getSystemColor(SWT.COLOR_BLUE));
			gc.fillRectangle(clientArea.width - _resizeBoxPixel, 0, _resizeBoxPixel, _resizeBoxPixel);
		}

//		System.out.println("gc " + gc.getClipping() + "\tclip " + clippingArea + "\tclient " + clientArea);
//		// TODO remove SYSTEM.OUT.PRINTLN

	}

	protected abstract void onStartHide();

	private void onTTControlEvent(final Event event) {

		if (_ttShell == null || _ttShell.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.MouseEnter:

			// stop animation
			if (_isShellFadingIn || _isShellFadingOut) {
				_isShellMovingEnabled = false;
			}

			break;

		case SWT.MouseExit:

			if (_isShellResized) {

				// shell is currently being resized, continue resizing until resizing is finished

				return;
			}

			if (_isShellToggled) {

				// do this only once

				_isShellToggled = false;

				return;
			}

			boolean isHide = false;

			// get control which is hovered with the mouse after the exit, can be null
			final Control hoveredControl = _display.getCursorControl();

			if (hoveredControl == null) {

				isHide = true;

			} else {

				/*
				 * check if the hovered control is the owner control, if not, hide the tooltip
				 */
				Control hoveredParent = hoveredControl;
//				final Control hoveredToolTip = _imageGallery.getGalleryToolTipShell();

				// move up child-parent hierarchy until shell is reached
				while (true) {

					if (hoveredParent == _ttShell) {
						// mouse is hovering in this tooltip
						break;
					}

					if (hoveredParent == _ownerControl) {

						// mouse is over the owner control

						break;
					}

//					if (hoveredToolTip != null && hoveredParent == hoveredToolTip) {
//
//						// mouse is over the owner tooltip control
//
//						System.out.println(System.nanoTime() + " exit 2 " + hoveredParent);
//						// TODO remove SYSTEM.OUT.PRINTLN
//
//						break;
//					}

					hoveredParent = hoveredParent.getParent();

					if (hoveredParent == null) {

						// mouse has left the tooltip and the owner control

						isHide = true;
						break;
					}
				}

				if (isHide == false) {

					/*
					 * check tooltip area as it is done in the original code because the current
					 * tooltip shell check is not working always (often but sometimes) correctly
					 */

					/**
					 * !!! this adjustment do not work on Linux because the tooltip gets hidden when
					 * the mouse tries to mover over the tooltip <br>
					 * <br>
					 * it seems to work on windows and linux with margin 1, when set to 0 the
					 * tooltip do sometime not be poped up again and the i-icons is not deaktivated<br>
					 * wolfgang 2010-07-23
					 */

					final Rectangle ttShellRect = _ttShell.getBounds();
					final int margin = 1;

					ttShellRect.x += margin;
					ttShellRect.y += margin;
					ttShellRect.width -= 2 * margin;
					ttShellRect.height -= 2 * margin;

					final Point cursorLocation = _display.getCursorLocation();

					if (!ttShellRect.contains(cursorLocation)) {

						// mouse is not within the tooltip shell rectangle

						isHide = true;
					}
				}
			}

			if (isHide) {
				ttHide(event);
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

						ttHide(event);
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

	protected void restoreState(final IDialogSettings state) {

		/*
		 * get horizontal gallery values
		 */
		_shellHorizWidth = Util.getStateInt(state, STATE_PHOTO_HORIZ_TOOL_TIP_WIDTH, 300);
		_shellHorizHeight = Util.getStateInt(state, STATE_PHOTO_HORIZ_TOOL_TIP_HEIGHT, 150);

		// ensure min values
		if (_shellHorizWidth < MIN_SHELL_HORIZ_WIDTH) {
			_shellHorizWidth = MIN_SHELL_HORIZ_WIDTH;
		}

		if (_shellHorizHeight < MIN_SHELL_HORIZ_HEIGHT) {
			_shellHorizHeight = MIN_SHELL_HORIZ_HEIGHT;
		}

		/*
		 * get vertical gallery values
		 */
		_shellVertWidth = Util.getStateInt(state, STATE_PHOTO_VERT_TOOL_TIP_WIDTH, 400);
		_shellVertHeight = Util.getStateInt(state, STATE_PHOTO_VERT_TOOL_TIP_HEIGHT, 250);

		// ensure min values
		if (_shellVertWidth < MIN_SHELL_VERT_WIDTH) {
			_shellVertWidth = MIN_SHELL_VERT_WIDTH;
		}

		if (_shellVertHeight < MIN_SHELL_VERT_HEIGHT) {
			_shellVertHeight = MIN_SHELL_VERT_HEIGHT;
		}
	}

	protected void saveState(final IDialogSettings state) {

		state.put(STATE_PHOTO_HORIZ_TOOL_TIP_WIDTH, _shellHorizWidth);
		state.put(STATE_PHOTO_HORIZ_TOOL_TIP_HEIGHT, _shellHorizHeight);
		state.put(STATE_PHOTO_VERT_TOOL_TIP_WIDTH, _shellVertWidth);
		state.put(STATE_PHOTO_VERT_TOOL_TIP_HEIGHT, _shellVertHeight);
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

	protected void setIsShellToggle() {
		_isShellToggled = true;
	}

	protected void showAtDefaultLocation() {

		if (_ttShell == null || _ttShell.isDisposed()) {
			return;
		}

		final Point size = _ttShell.getSize();
		final Point fixedLocation = fixupDisplayBounds(size, getLocation(size));

		_ttShell.setLocation(fixedLocation);
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

		createShell();

		ttShow();

		return true;
	}

	private void showShellWhenVisible() {

		if (_ttShell == null || _ttShell.isDisposed() || _ttShell.isVisible() == false) {
			return;
		}

		ttShow();
	}

	private void ttDispose(final Event event) {

		if (_ttShell == null || _ttShell.isDisposed()) {
			return;
		}

		// hide tooltip definitively

		ownerShellRemoveListener();

//		_currentArea = null;

		passOnEvent(_ttShell, event);

		_ttShell.dispose();
		_ttShell = null;
	}

	private void ttHide(final Event event) {

		onStartHide();

		if (_ttShell == null || _ttShell.isDisposed() || _ttShell.isVisible() == false) {
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

		// shell is not yet fading in

		_isShellFadingIn = true;
		_isShellFadingOut = false;

		animation10_Start();
	}
}
