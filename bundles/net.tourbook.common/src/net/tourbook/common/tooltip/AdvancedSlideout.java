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

import net.tourbook.common.CommonActivator;
import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;

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

/*
 * Resize control: org.eclipse.jface.text.AbstractInformationControl
 */
public abstract class AdvancedSlideout extends AdvancedSlideoutShell {

	private final WaitTimer			_waitTimer			= new WaitTimer();

	private boolean					_isWaitTimerStarted;
	private boolean					_canOpenToolTip;

	private boolean					_isShellDragged;
	private int						_devXMousedown;
	private int						_devYMousedown;

	private ActionCloseSlideout		_actionCloseSlideout;
	private ActionSlideoutKeepOpen	_actionKeepSlideoutOpen;
	private ActionPinSlideout		_actionPinSlideout;

	private String					_titleText			= UI.EMPTY_STRING;

	/*
	 * UI controls
	 */
	private ToolBar					_toolbarSlideoutActions;

	private Label					_labelDragSlideout;

	private Cursor					_cursorResize;
	private Cursor					_cursorHand;

	private SlideoutLocation		_slideoutLocation	= SlideoutLocation.BELOW_CENTER;

	private class ActionCloseSlideout extends Action {

		public ActionCloseSlideout() {

			super(null, Action.AS_PUSH_BUTTON);

			setToolTipText(Messages.App_Action_Close_Tooltip);
			setImageDescriptor(CommonActivator.getImageDescriptor(Messages.Image__App_Close_Themed));
		}

		@Override
		public void run() {
			close();
		}
	}

	private class ActionPinSlideout extends Action {

		public ActionPinSlideout() {

			super(null, Action.AS_CHECK_BOX);

			setToolTipText(Messages.Slideout_Dialog_Action_PinSlideoutLocation_Tooltip);
			setImageDescriptor(CommonActivator.getImageDescriptor(Messages.Image__Pin_Themed));
		}

		@Override
		public void run() {
			setIsSlideoutPinned(isChecked());
		}
	}

	private class ActionSlideoutKeepOpen extends Action {

		public ActionSlideoutKeepOpen() {

			super(null, Action.AS_CHECK_BOX);

			setToolTipText(Messages.Slideout_Dialog_Action_KeepSlideoutOpen_Tooltip);
			setImageDescriptor(CommonActivator.getImageDescriptor(Messages.Image__BookOpen_Themed));
		}

		@Override
		public void run() {
			setIsKeepSlideoutOpen(isChecked());
		}
	}

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	/**
	 * @param ownerControl
	 * @param state
	 * @param slideoutDefaultSize
	 *            Slideout default size or <code>null</code> when not available.
	 *            <p>
	 *            The default default size is
	 *            <p>
	 * 
	 *            <pre>
	 *            horizContentDefaultWidth = 300;
	 *            horizContentDefaultHeight = 150;
	 * 
	 *            vertContentDefaultWidth = 400;
	 *            vertContentDefaultHeight = 250;
	 * 
	 *            </pre>
	 */
	public AdvancedSlideout(final Control ownerControl, //
							final IDialogSettings state,
							final int[] slideoutDefaultSize) {

		super(ownerControl, state, slideoutDefaultSize);

		setShellFadeInSteps(0);
		setShellFadeOutSteps(30);
		setShellFadeOutDelaySteps(10);

		initUI(ownerControl);
		addListener(ownerControl);

		createActions();
	}

	private void addListener(final Control ownerControl) {

		ownerControl.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(final MouseEvent e) {

				// prevent to open the tooltip
				_canOpenToolTip = false;
			}
		});
	}

	@Override
	protected void afterCreateShell(final Shell shell) {}

	@Override
	protected void beforeHideToolTip() {}

	@Override
	protected void closeInternalShells() {}

	private void createActions() {

		_actionCloseSlideout = new ActionCloseSlideout();
		_actionKeepSlideoutOpen = new ActionSlideoutKeepOpen();
		_actionPinSlideout = new ActionPinSlideout();
	}

	/**
	 * Create the content for the slideout.
	 * 
	 * @param parent
	 */
	protected abstract void createSlideoutContent(Composite parent);

	@Override
	protected Composite createSlideoutContentArea(final Composite parent) {

		final Composite container = createUI(parent);

		return container;
	}

	protected void createTitleBarControls(final Composite parent) {

		// create default content
		final Label label = new Label(parent, SWT.NONE);
		label.setText(UI.EMPTY_STRING);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory
				.swtDefaults() //
				.spacing(0, 0)
				.applyTo(shellContainer);
//		shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			createUI_10_ActionBar(shellContainer);
			createSlideoutContent(shellContainer);
		}

		fillActionBar();

		return shellContainer;
	}

	private void createUI_10_ActionBar(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(3)
				.extendedMargins(0, 0, 0, 5)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_12_Header_Draggable(container);
			createTitleBarControls(container);
			createUI_14_Header_Toolbar(container);
		}
	}

	private void createUI_12_Header_Draggable(final Composite container) {

		_labelDragSlideout = new Label(container, SWT.NONE);
		_labelDragSlideout.setText(_titleText);
		_labelDragSlideout.setToolTipText(Messages.Slideout_Dialog_Action_DragSlideout_ToolTip);
		GridDataFactory
				.fillDefaults()//
				//				.grab(true, false)
				.align(SWT.FILL, SWT.CENTER)
				//				.indent(3, 0)
				.applyTo(_labelDragSlideout);
		MTFont.setBannerFont(_labelDragSlideout);

//		_labelDragSlideout.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));

		_labelDragSlideout.addMouseTrackListener(new MouseTrackListener() {

			@Override
			public void mouseEnter(final MouseEvent e) {
				_labelDragSlideout.setCursor(_cursorHand);
			}

			@Override
			public void mouseExit(final MouseEvent e) {
				_labelDragSlideout.setCursor(null);
			}

			@Override
			public void mouseHover(final MouseEvent e) {

			}
		});

		_labelDragSlideout.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				onMouseDown(e);
			}

			@Override
			public void mouseUp(final MouseEvent e) {
				onMouseUp(e);
			}
		});
		_labelDragSlideout.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(final MouseEvent e) {
				onMouseMove(e);
			}
		});
	}

	/**
	 * Create toolbar for the exit button
	 */
	private void createUI_14_Header_Toolbar(final Composite container) {

		_toolbarSlideoutActions = new ToolBar(container, SWT.FLAT);
		GridDataFactory
				.fillDefaults()//
				.indent(20, 0)
				.applyTo(_toolbarSlideoutActions);
	}

	private void fillActionBar() {

		/*
		 * fill exit toolbar
		 */
		final ToolBarManager exitToolbarManager = new ToolBarManager(_toolbarSlideoutActions);

		exitToolbarManager.add(_actionPinSlideout);
		exitToolbarManager.add(_actionKeepSlideoutOpen);
		exitToolbarManager.add(_actionCloseSlideout);

		exitToolbarManager.update(true);
	}

	protected abstract Rectangle getParentBounds();

	protected SlideoutLocation getSlideoutLocation() {
		return _slideoutLocation;
	}

	@Override
	public Point getToolTipLocation(final Point slideoutSize) {

		final int slideoutWidth = slideoutSize.x;
		final int slideoutHeight = slideoutSize.y;

		final Rectangle _slideoutParentBounds = getParentBounds();

		// toolitem top left position and size
		final int devXParent = _slideoutParentBounds.x;
		final int devYParent = _slideoutParentBounds.y;
		final int itemWidth = _slideoutParentBounds.width;
		final int itemHeight = _slideoutParentBounds.height;

		// center horizontally

		final int devXCenter = devXParent + itemWidth / 2 - slideoutWidth / 2;
		final int devXRight = devXParent;

		final int devYAbove = devYParent - slideoutHeight;
		final int devYBelow = devYParent + itemHeight;

		final Rectangle displayBounds = Display.getCurrent().getBounds();

		int devX;
		int devY;
		boolean isCheckBelow = false;
		boolean isCheckAbove = false;

		switch (_slideoutLocation) {
		case ABOVE_CENTER:

			devX = devXCenter;
			devY = devYAbove;
			isCheckAbove = true;

			break;

		case BELOW_RIGHT:

			devX = devXRight;
			devY = devYBelow;
			isCheckBelow = true;

			break;

		case BELOW_CENTER:
		default:

			devX = devXCenter;
			devY = devYBelow;
			isCheckBelow = true;

			break;
		}

		if (isCheckAbove && (devY < 0)) {
			devY = devYBelow;
		}
		if (isCheckBelow & (devY + slideoutHeight > displayBounds.height)) {
			devY = devYAbove;
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

	abstract protected void onFocus();

	private void onMouseDown(final MouseEvent e) {

		_isShellDragged = true;

		_devXMousedown = e.x;
		_devYMousedown = e.y;

		_labelDragSlideout.setCursor(_cursorResize);
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

			_labelDragSlideout.setCursor(_cursorHand);

			setSlideoutPinnedLocation();
		}
	}

	@Override
	protected void onReparentShell(final Shell reparentedShell) {

		reparentedShell.setFocus();

		onFocus();
	}

	/**
	 * @param slideoutParentBounds
	 * @param isOpenDelayed
	 */
	public void open(final boolean isOpenDelayed) {

		if (isVisible()) {
			return;
		}

		if (isOpenDelayed) {

			// open delayed

			_canOpenToolTip = true;

			if (_isWaitTimerStarted == false) {

				_isWaitTimerStarted = true;

				Display.getCurrent().timerExec(50, _waitTimer);
			}

		} else {

			// open now

			showShell();
		}
	}

	private void open_Runnable() {

		_isWaitTimerStarted = false;

		if (_canOpenToolTip) {
			showShell();
		}
	}

	@Override
	protected void restoreState_KeepSlideoutOpen(final boolean isKeepSlideoutOpen) {

		_actionKeepSlideoutOpen.setChecked(isKeepSlideoutOpen);
	}

	@Override
	protected void restoreState_SlideoutIsPinned(final boolean isToolTipPinned) {

		_actionPinSlideout.setChecked(isToolTipPinned);
	}

	public void setSlideoutLocation(final SlideoutLocation slideoutLocation) {
		_slideoutLocation = slideoutLocation;
	}

	protected void setTitleText(final String titleText) {
		_titleText = titleText;
	}

}
