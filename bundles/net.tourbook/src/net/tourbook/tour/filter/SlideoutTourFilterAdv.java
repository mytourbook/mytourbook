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
package net.tourbook.tour.filter;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.AdvancedToolTipShell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

public class SlideoutTourFilterAdv extends AdvancedToolTipShell {

	// initialize with default values which are (should) never be used
	private Rectangle			_toolTipItemBounds	= new Rectangle(0, 0, 50, 50);

	private final WaitTimer		_waitTimer			= new WaitTimer();

	private boolean				_isWaitTimerStarted;
	private boolean				_canOpenToolTip;

	private boolean				_isShellDragged;
	private int					_devXMousedown;
	private int					_devYMousedown;

	private ActionCloseToolTip	_actionCloseToolTip;
	private ActionPinToolTip	_actionPinToolTip;

	/*
	 * UI controls
	 */
	private ToolBar				_ttToolbarControlExit;

	private Label				_labelDragToolTip;

	private Cursor				_cursorResize;
	private Cursor				_cursorHand;

	private class ActionCloseToolTip extends Action {

		public ActionCloseToolTip() {

			super(null, Action.AS_PUSH_BUTTON);

			setToolTipText(Messages.App_Action_Close_ToolTip);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Close));
		}

		@Override
		public void run() {
			hideNow();
		}
	}

	private class ActionPinToolTip extends Action {

		public ActionPinToolTip() {

			super(null, Action.AS_CHECK_BOX);

			setToolTipText(Messages.Photo_Tooltip_Action_PinToolTip_ToolTip);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__Pin_Blue));
		}

		@Override
		public void run() {
			actionPinToolTip(_actionPinToolTip.isChecked());
		}
	}

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public SlideoutTourFilterAdv(final Control ownerControl, final ToolBar toolBar, final IDialogSettings state) {

		super(ownerControl, state);

		setShellFadeInSteps(1);
		setShellFadeOutSteps(10);
		setShellFadeOutDelaySteps(1);

		initUI(ownerControl);
		addListener(ownerControl, toolBar);

		createActions();
	}

	private void addListener(final Control ownerControl, final ToolBar toolBar) {

		toolBar.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(final MouseEvent e) {

				// prevent to open the tooltip
				_canOpenToolTip = false;
			}
		});
	}

	@Override
	protected void afterCreateShell(final Shell shell) {

	}

	@Override
	protected void beforeHideToolTip() {

	}

	@Override
	protected void closeInternalShells() {

	}

	private void createActions() {

		_actionPinToolTip = new ActionPinToolTip();
		_actionCloseToolTip = new ActionCloseToolTip();
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		final Composite container = createUI(parent);

		return container;
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(2, 2).applyTo(shellContainer);
//		shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_10_ActionBar(shellContainer);
			createUI_20_TourFilter(shellContainer);
		}

		fillActionBar();

		return shellContainer;
	}

	private void createUI_10_ActionBar(final Composite parent) {

		GridLayoutFactory.fillDefaults().applyTo(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * create toolbar for the exit button
			 */
			_ttToolbarControlExit = new ToolBar(container, SWT.FLAT);
			GridDataFactory
					.fillDefaults()//
					.applyTo(_ttToolbarControlExit);
//			_ttToolbarControlExit.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

			/*
			 * spacer
			 */
			_labelDragToolTip = new Label(container, SWT.NONE);
			GridDataFactory
					.fillDefaults()//
					.grab(true, false)
					.hint(50, SWT.DEFAULT)
					.applyTo(_labelDragToolTip);
			_labelDragToolTip.setText(UI.EMPTY_STRING);
			_labelDragToolTip.setToolTipText(Messages.Photo_Tooltip_Action_MoveToolTip_ToolTip);
//			_labelDragToolTip.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			_labelDragToolTip.addMouseTrackListener(new MouseTrackListener() {

				@Override
				public void mouseEnter(final MouseEvent e) {
					_labelDragToolTip.setCursor(_cursorHand);
				}

				@Override
				public void mouseExit(final MouseEvent e) {
					_labelDragToolTip.setCursor(null);
				}

				@Override
				public void mouseHover(final MouseEvent e) {

				}
			});

			_labelDragToolTip.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDown(final MouseEvent e) {
					onMouseDown(e);
				}

				@Override
				public void mouseUp(final MouseEvent e) {
					onMouseUp(e);
				}
			});
			_labelDragToolTip.addMouseMoveListener(new MouseMoveListener() {
				@Override
				public void mouseMove(final MouseEvent e) {
					onMouseMove(e);
				}
			});
		}
	}

	private void createUI_20_TourFilter(final Composite parent) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void enableControls() {

	}

	private void fillActionBar() {

		/*
		 * fill exit toolbar
		 */
		final ToolBarManager exitToolbarManager = new ToolBarManager(_ttToolbarControlExit);

		exitToolbarManager.add(_actionCloseToolTip);
		exitToolbarManager.add(_actionPinToolTip);

		exitToolbarManager.update(true);
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

		final int tipWidth = tipSize.x;
		final int tipHeight = tipSize.y;

		final int itemWidth = _toolTipItemBounds.width;
		final int itemHeight = _toolTipItemBounds.height;

		// center horizontally

		int devX = _toolTipItemBounds.x;
		devX += itemWidth / 2 - tipWidth / 2;

		int devY = _toolTipItemBounds.y + itemHeight + 0;

		final Rectangle displayBounds = Display.getCurrent().getBounds();

		if (devY + tipHeight > displayBounds.height) {

			// slideout is below bottom, show it above the action button

			devY = _toolTipItemBounds.y - tipHeight;
		}

		return new Point(devX, devY);
	}

	private void initUI(final Control ownerControl) {

		final Display display = ownerControl.getDisplay();
		_cursorResize = new Cursor(display, SWT.CURSOR_SIZEALL);
		_cursorHand = new Cursor(display, SWT.CURSOR_HAND);

		ownerControl.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	@Override
	protected boolean isToolTipDragged() {
		return false;
	}

	@Override
	protected boolean isVerticalLayout() {
		return false;
	}

	private void onDispose() {

		_cursorResize.dispose();
		_cursorHand.dispose();
	}

	private void onMouseDown(final MouseEvent e) {

		_isShellDragged = true;

		_devXMousedown = e.x;
		_devYMousedown = e.y;

		_labelDragToolTip.setCursor(_cursorResize);
	}

	private void onMouseMove(final MouseEvent e) {

		if (_isShellDragged) {

			// shell is dragged

			final int diffX = _devXMousedown - e.x;
			final int diffY = _devYMousedown - e.y;

			setShellLocation(diffX, diffY);
		}
	}

	private void onMouseUp(final MouseEvent e) {

		if (_isShellDragged) {

			// shell is dragged with the mouse, stop dragging

			_isShellDragged = false;

			_labelDragToolTip.setCursor(_cursorHand);

			setToolTipPinnedLocation();
		}
	}

	@Override
	protected void onReparentShell(final Shell reparentedShell) {

	}

	/**
	 * @param toolTipItemBounds
	 * @param isOpenDelayed
	 */
	public void open(final Rectangle toolTipItemBounds, final boolean isOpenDelayed) {

		if (isVisible()) {
			return;
		}

		if (isOpenDelayed == false) {

			if (toolTipItemBounds != null) {

				_toolTipItemBounds = toolTipItemBounds;

				showShell();
			}

		} else {

			if (toolTipItemBounds == null) {

				// item is not hovered any more

				_canOpenToolTip = false;

				return;
			}

			_toolTipItemBounds = toolTipItemBounds;
			_canOpenToolTip = true;

			if (_isWaitTimerStarted == false) {

				_isWaitTimerStarted = true;

				Display.getCurrent().timerExec(50, _waitTimer);
			}
		}
	}

	private void open_Runnable() {

		_isWaitTimerStarted = false;

		if (_canOpenToolTip) {
			showShell();
		}
	}

	@Override
	protected void setToolTipPinned(final boolean isToolTipPinned) {

	}

}
