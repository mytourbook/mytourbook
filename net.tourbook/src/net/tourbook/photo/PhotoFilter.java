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
package net.tourbook.photo;

import net.tourbook.common.tooltip.AnimatedToolTipShell;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Photo filter dialog.
 */
public class PhotoFilter extends AnimatedToolTipShell {

	private static final int	SHELL_MARGIN	= 5;

	// initialize with default values which are (should) never be used
	private Rectangle			_itemBounds		= new Rectangle(0, 0, 50, 50);

	private final WaitTimer		_waitTimer		= new WaitTimer();

	private boolean				_canOpenToolTip;

	private boolean				_isWaitTimerStarted;

	private RatingStars			_ratingStars;

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public PhotoFilter(final Control ownerControl, final ToolBar toolBar) {

		super(ownerControl);

		toolBar.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(final MouseEvent e) {

				_canOpenToolTip = false;
			}
		});

		setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
		setBehaviourOnMouseOver(AnimatedToolTipShell.MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
	}

	@Override
	protected void beforeHideToolTip() {

	}

	@Override
	protected boolean canShowToolTip() {
		return true;
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		final Composite container = createUI(parent);

		return container;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(1)
				.margins(SHELL_MARGIN, SHELL_MARGIN)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText("PHOTO FILTER");

			_ratingStars = new RatingStars(container);
		}

		return container;
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

		final int tipWidth = tipSize.x;
		final int tipHeight = tipSize.y;

		final int itemWidth = _itemBounds.width;
		final int itemHeight = _itemBounds.height;

		final int itemWidth2 = itemWidth / 2;
		final int tipWidth2 = tipWidth / 2;

		final int devX = _itemBounds.x + itemWidth2 - tipWidth2;
		final int devY = _itemBounds.y + itemHeight + 0;

		return new Point(devX, devY);
	}

	@Override
	protected Rectangle noHideOnMouseMove() {
		return _itemBounds;
	}

	@Override
	protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {}

	/**
	 * @param itemBounds
	 * @param isOpenDelayed
	 */
	public void open(final Rectangle itemBounds, final boolean isOpenDelayed) {

		if (isToolTipVisible()) {
			return;
		}

		if (isOpenDelayed == false) {

			if (itemBounds != null) {

				_itemBounds = itemBounds;

				showToolTip();
			}

		} else {

			if (itemBounds == null) {

				// item is not hovered any more

				_canOpenToolTip = false;

				return;
			}

			_itemBounds = itemBounds;
			_canOpenToolTip = true;

//		System.out.println(UI.timeStampNano()
//				+ " open\t2\t_isWaitTimerStarted="
//				+ _isWaitTimerStarted
//				+ ("\t_canOpenToolTip=" + _canOpenToolTip)
//				+ ("\t__itemBounds=" + _itemBounds)
//		//
//				);
//		// TODO remove SYSTEM.OUT.PRINTLN

			if (_isWaitTimerStarted == false) {

				_isWaitTimerStarted = true;

				Display.getCurrent().timerExec(200, _waitTimer);
			}
		}
	}

	private void open_Runnable() {

		_isWaitTimerStarted = false;

		if (_canOpenToolTip) {
			showToolTip();
		}
	}

}
