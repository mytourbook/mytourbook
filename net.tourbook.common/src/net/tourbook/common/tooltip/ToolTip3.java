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

import net.tourbook.common.util.StatusUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

/**
 * <p>
 * Part of this tooltip is copied from org.eclipse.jface.window.ToolTip
 */
public abstract class ToolTip3 {

	private int							_popupDelay		= 0;
	private int							_hideDelay		= 0;

	private int							_defaultOffsetX	= 3;
	private int							_defaultOffsetY	= 0;

	private Control						_ownerControl;
	private Display						_display;

	private OwnerControlListener		_ownerControlListener;
	private OwnerShellListener			_ownerShellListener;
	private ToolTipShellListener		_ttShellListener;
	private ToolTipAllControlsListener	_ttAllControlsListener;
	private ToolTipDisplayListener		_ttDisplayListener;

	/**
	 * Keep track of added display listener that no more than <b>1</b> is set.
	 */
	private boolean						_isDisplayListenerSet;

	private final AnimationTimer		_animationTimer;

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
	private class ToolTipAllControlsListener implements Listener {
		public void handleEvent(final Event event) {
			onTTAllControlsEvent(event);
		}

	}

	private class ToolTipDisplayListener implements Listener {
		public void handleEvent(final Event event) {

			switch (event.type) {
			case SWT.MouseMove:
				onTTDisplayMouseMove(event);
				break;
			}
		}
	}

	private final class ToolTipShellListener implements Listener {
		public void handleEvent(final Event event) {
			onTTShellEvent(event);
		}
	}

	public ToolTip3(final Control ownerControl) {

		_ownerControl = ownerControl;
		_display = _ownerControl.getDisplay();

		_ttAllControlsListener = new ToolTipAllControlsListener();
		_ttShellListener = new ToolTipShellListener();
		_ttDisplayListener = new ToolTipDisplayListener();

		_ownerControlListener = new OwnerControlListener();
		_ownerShellListener = new OwnerShellListener();

		_animationTimer = new AnimationTimer();

		addOwnerControlListener();
		addOwnerShellListener();
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

	private void animation20_Runnable() {

//		final long start = System.nanoTime();

//		if (_shell == null || _shell.isDisposed() || _shell.isVisible() == false) {
//			return;
//		}

		try {

		} catch (final Exception err) {
			StatusUtil.log(err);
		} finally {

//			final float timeDiff = (float) (System.nanoTime() - start) / 1000000;
//			System.out.println(UI.timeStampNano() + " animation20_Runnable:\t" + timeDiff + " ms\t" + " ms");
//			// TODO remove SYSTEM.OUT.PRINTLN
		}
	}

	/**
	 * @param tipSize
	 * @param ttTopLeft
	 *            Top/left location for the hovered area relativ to the display
	 * @return
	 */
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
		return _ownerControl.toDisplay(event.x + _defaultOffsetX, event.y + _defaultOffsetY);
	}

	private void onDispose(final Event event) {

//		if (_shell == null || _shell.isDisposed()) {
//			return;
//		}
//
//		// hide tooltip definitively
//
//		removeOwnerShellListener();
//		removeTTDisplayListener();
//
////		passOnEvent(_shell, event);
//		_shell.dispose();
	}

	private void onOwnerControlEvent(final Event event) {

		if (_ownerControl == null || _ownerControl.isDisposed()) {
			return;
		}

//		switch (event.type) {
//		case SWT.Dispose:
//
//			onDispose(event);
//
//			removeOwnerControlsListener();
//
//			break;
//
//		case SWT.Resize:
//
//			showShellWhenVisible();
//
//			break;
//		}
	}

	private void onOwnerShellEvent(final Event event) {

//		if (_shell == null || _shell.isDisposed()) {
//			return;
//		}
//
//		switch (event.type) {
//		case SWT.Deactivate:
//
//			_display.asyncExec(new Runnable() {
//
//				public void run() {
//
//					// hide tooltip when another shell is activated
//
//					if (_display.getActiveShell() != _shell) {
//						ttHide(event);
//					}
//				}
//			});
//			break;
//
//		case SWT.Move:
//
//			showShellWhenVisible();
//
//			break;
//		}
	}

	private void onTTAllControlsEvent(final Event event) {

//		if (_shell == null || _shell.isDisposed()) {
//			return;
//		}
//
//		switch (event.type) {
//		case SWT.KeyDown:
//
//			if (event.keyCode == SWT.ESC) {
//				hide();
//			}
//
//			break;
//
//		case SWT.MouseEnter:
//
//			if (_isShellFadingIn || _isShellFadingOut) {
//
//				// stop animation
////				_isShellFadingIn = _isShellFadingOut = false;
//			}
//
//			break;
//
//		case SWT.MouseExit:
//
//			break;
//
//		case SWT.MouseMove:
//
//			if (_isReceiveOnMouseMove //
////					&& _isShellFadingIn == false
////					&& _isShellFadingOut == false//
//			) {
//
//				final Widget widget = event.widget;
//
//				if (widget instanceof Control) {
//
//					final Point ttDisplayLocation = ((Control) widget).toDisplay(event.x, event.y);
//					final Point ownerLocation = _ownerControl.toControl(ttDisplayLocation);
//
//					_display.asyncExec(new Runnable() {
//						public void run() {
//
//							final MouseEvent mouseEvent = new MouseEvent(event);
//							mouseEvent.x = ownerLocation.x;
//							mouseEvent.y = ownerLocation.y;
//
//							onMouseMoveInToolTip(mouseEvent);
//						}
//					});
//				}
//			}
//
//			break;
//		}
	}

	private void onTTDisplayMouseMove(final Event event) {

//		final long start = System.nanoTime();

//		if (_shell == null || _shell.isDisposed() || _shell.isVisible() == false) {
//			return;
//		}

//		System.out.println(UI.timeStampNano()
//				+ " onTTDisplayMouseMove"
//				+ ((float) (System.nanoTime() - start) / 1000000)
//				+ " ms");
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	private void onTTShellEvent(final Event event) {

		switch (event.type) {
		case SWT.Deactivate:

//			if (_shell != null && !_shell.isDisposed() && _ownerControl != null && !_ownerControl.isDisposed()) {
//
//				_display.asyncExec(new Runnable() {
//
//					public void run() {
//
//						// hide tooltip when another shell is activated
//
//						// check again
//						if (_shell == null
//								|| _shell.isDisposed()
//								|| _ownerControl == null
//								|| _ownerControl.isDisposed()) {
//							return;
//						}
//
//						if (_ownerControl.getShell() == _shell.getDisplay().getActiveShell()) {
//
//							// don't hide when main window is active
//							return;
//						}
//
//						ttHide(event);
//					}
//				});
//			}

			break;

		case SWT.Dispose:

			break;

		}

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

	private void removeTTDisplayListener() {

		if (_isDisplayListenerSet == false) {
			return;
		}

		_display.removeFilter(SWT.MouseMove, _ttDisplayListener);

		_isDisplayListenerSet = false;
	}
}
