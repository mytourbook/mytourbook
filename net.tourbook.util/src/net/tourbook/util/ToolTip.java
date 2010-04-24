/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public abstract class ToolTip {

	private final Shell			_shell;

	private final Composite		_toolTipContent;

	/**
	 * maximum width in pixel for the width of the tooltip
	 */
	private static final int	MAX_TOOLTIP_WIDTH	= 500;

	public ToolTip(final Composite parent) {

		final Display display = parent.getDisplay();

		_shell = new Shell(display, SWT.ON_TOP | SWT.TOOL);

		_shell.addMouseListener(new MouseAdapter() {});

		_shell.addMouseMoveListener(new MouseMoveListener() {

			public void mouseMove(final MouseEvent e) {

			}
		});

		_shell.addMouseTrackListener(new MouseTrackListener() {

			public void mouseEnter(final MouseEvent e) {
				System.out.println("mouseEnter()");
				// TODO remove SYSTEM.OUT.PRINTLN

			}

			public void mouseExit(final MouseEvent e) {
				System.out.println("mouseExit");
				// TODO remove SYSTEM.OUT.PRINTLN

				_shell.setVisible(false);
			}

			public void mouseHover(final MouseEvent e) {
				System.out.println("mouseHover");
				// TODO remove SYSTEM.OUT.PRINTLN

			}
		});

		_shell.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.keyCode == SWT.ESC) {
					_shell.setVisible(false);
				}
			}

		});

//		_shell.setRegion(region)
		_shell.setLayout(new FillLayout());

		_toolTipContent = setContent(_shell);
	}

	public void dispose() {
		_shell.dispose();
	}

	public void hide() {
//		System.out.println("hide");
//		// TODO remove SYSTEM.OUT.PRINTLN

		_shell.setVisible(false);
	}

	public boolean isVisible() {
		return _shell.isVisible();
	}

//	/**
//	 * Position the tooltip and ensure that it is not located off the screen.
//	 */
//	private void setToolTipPosition() {
//
//		final Point cursorLocation = getDisplay().getCursorLocation();
//
//		// Assuming cursor is 21x21 because this is the size of
//		// the arrow cursor on Windows
//		final int cursorHeight = 21;
//
//		final Point tooltipSize = fToolTipShell.getSize();
//		final Rectangle monitorRect = getMonitor().getBounds();
//		final Point pt = new Point(cursorLocation.x, cursorLocation.y + cursorHeight + 2);
//
//		pt.x = Math.max(pt.x, monitorRect.x);
//		if (pt.x + tooltipSize.x > monitorRect.x + monitorRect.width) {
//			pt.x = monitorRect.x + monitorRect.width - tooltipSize.x;
//		}
//		if (pt.y + tooltipSize.y > monitorRect.y + monitorRect.height) {
//			pt.y = cursorLocation.y - 2 - tooltipSize.y;
//		}
//
//		fToolTipShell.setLocation(pt);
//	}

	protected abstract Composite setContent(Shell shell);

	/**
	 * Position the tooltip and ensure that it is not located off the screen.
	 * 
	 * @param shellArea
	 *            client area for the shell
	 * @param noCoverX
	 *            left position which should not be covered
	 * @param noCoverY
	 *            top position which should not be coverd
	 * @param noCoverWidth
	 *            width relative to left which should not be covered
	 * @param noCoverHeight
	 *            height relative to top which should not be covered
	 * @param noCoverYOffset
	 */
	private void setToolTipPosition(final Rectangle shellArea,
									final int noCoverX,
									final int noCoverY,
									final int noCoverWidth,
									final int noCoverHeight,
									final int noCoverYOffset) {

		final int devX = noCoverX - (shellArea.width / 2) + (noCoverWidth / 2);
		final int devY = noCoverY - shellArea.height - noCoverYOffset;

		_shell.setLocation(devX, devY);
	}

	/**
	 * Shows the tooltip
	 * 
	 * @param x
	 *            left position which should not be covered
	 * @param y
	 *            top position which should not be coverd
	 * @param width
	 *            width relative to left which should not be covered
	 * @param height
	 *            height relative to top which should not be covered
	 */
	public void show(	final int noCoverX,
						final int noCoverY,
						final int noCoverWidth,
						final int noCoverHeight,
						final int noCoverYOffset) {

		/*
		 * adjust width of the tooltip when it exeeds the maximum
		 */
		Point containerSize = _toolTipContent.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		if (containerSize.x > MAX_TOOLTIP_WIDTH) {

//			GridDataFactory.fillDefaults().hint(MAX_TOOLTIP_WIDTH, SWT.DEFAULT).applyTo(fToolTipLabel);
//			fToolTipLabel.pack(true);

			containerSize = _toolTipContent.computeSize(MAX_TOOLTIP_WIDTH, SWT.DEFAULT, true);
		}

		_toolTipContent.setSize(containerSize);
		_shell.pack(true);

		/*
		 * On some platforms, there is a minimum size for a shell which may be greater than the
		 * label size. To avoid having the background of the tip shell showing around the label,
		 * force the label to fill the entire client area.
		 */
		final Rectangle shellArea = _shell.getClientArea();
		_toolTipContent.setSize(shellArea.width, shellArea.height);

		setToolTipPosition(shellArea, noCoverX, noCoverY, noCoverWidth, noCoverHeight, noCoverYOffset);

//		System.out.println("show");
//		// TODO remove SYSTEM.OUT.PRINTLN

		_shell.setVisible(true);
	}


}
