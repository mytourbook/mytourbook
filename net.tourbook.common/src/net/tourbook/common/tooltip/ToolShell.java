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

import net.tourbook.common.Messages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

class ToolShell {

	private static final int	WINDOW_TITLE_HEIGHT	= 12;

	private boolean				_isTTDragged;
	private int					_devXTTMouseDown;
	private int					_devYTTMouseDown;

	private ToolTip3			_toolTip3;
	private IToolProvider		_toolProvider;

	/*
	 * UI controls
	 */
	private Button				_btnPin;
	private Button				_btnClose;

	private Shell				_shell;

	ToolShell(final ToolTip3 toolTip3, final IToolProvider toolProvider) {

		_toolTip3 = toolTip3;
		_toolProvider = toolProvider;
	}

	private void addHeaderListener(final Composite header) {

		header.addMouseTrackListener(new MouseTrackListener() {

			@Override
			public void mouseEnter(final MouseEvent e) {

				header.setCursor(_toolTip3.getCursorHand());
			}

			@Override
			public void mouseExit(final MouseEvent e) {

				_isTTDragged = false;

				header.setCursor(null);
			}

			@Override
			public void mouseHover(final MouseEvent e) {}
		});

		header.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(final MouseEvent event) {

//				System.out.println(UI.timeStampNano() + " mouseMove\t");
//				// TODO remove SYSTEM.OUT.PRINTLN

				if (_isTTDragged) {

					final int xDiff = event.x - _devXTTMouseDown;
					final int yDiff = event.y - _devYTTMouseDown;

					setDraggedLocation(header.getShell(), xDiff, yDiff);
				}
			}
		});

		header.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent event) {

//				System.out.println(UI.timeStampNano() + " mouseDown\t" + _cursorDragged); //$NON-NLS-1$
//				// TODO remove SYSTEM.OUT.PRINTLN

				_isTTDragged = true;

				_devXTTMouseDown = event.x;
				_devYTTMouseDown = event.y;

				header.setCursor(_toolTip3.getCursorDragged());
			}

			@Override
			public void mouseUp(final MouseEvent e) {

				if (_isTTDragged) {

					_isTTDragged = false;

//					_buttonPin.setEnabled(true);

					_toolTip3.toolTipIsMoved(_shell);
				}

				header.setCursor(_toolTip3.getCursorHand());
			}
		});
	}

	Composite createUI(final Shell shell) {

		_shell = shell;

		/*
		 * shell container is necessary because the margins of the inner container will hide the
		 * tooltip when the mouse is hovered, which is not as it should be.
		 */
		final Composite toolContainer = new Composite(shell, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(toolContainer);
//		shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			createUI_10_Custom(toolContainer);
		}

		return toolContainer;
	}

	private void createUI_10_Custom(final Composite parent) {

		createUI_20_ToolTipHeader(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults() //
				.extendedMargins(ToolTip3.SHELL_MARGIN, ToolTip3.SHELL_MARGIN, 0, ToolTip3.SHELL_MARGIN)
				.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			_toolProvider.createUI(container);
		}
	}

	private void createUI_20_ToolTipHeader(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, WINDOW_TITLE_HEIGHT).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		// show layer title in the hovered tooltip header
		container.setToolTipText(_toolProvider.getTitle());

		addHeaderListener(container);

		{
			/*
			 * button: pin
			 */
			_btnPin = new Button(container, SWT.NONE);

			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.grab(false, true)
					.hint(WINDOW_TITLE_HEIGHT, SWT.DEFAULT)
					.applyTo(_btnPin);

			_btnPin.setToolTipText(Messages.Map3_PropertyTooltip_Action_MoveToDefaultLocation_Tooltip);
			_btnPin.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));

			_btnPin.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectPin();
				}
			});

			_btnPin.setEnabled(false);

			/*
			 * button: close
			 */
			_btnClose = new Button(container, SWT.NONE);

			GridDataFactory.fillDefaults()//
					.align(SWT.END, SWT.CENTER)
					.grab(true, true)
					.hint(WINDOW_TITLE_HEIGHT, SWT.DEFAULT)
					.applyTo(_btnClose);

			_btnClose.setToolTipText(Messages.Map3_PropertyTooltip_Action_Close_Tooltip);
			_btnClose.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));

			_btnClose.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					// hide tooltip
					_btnClose.getShell().close();
				}
			});
		}
	}

	private void onSelectPin() {
		// TODO Auto-generated method stub

	}

	/**
	 * Set tooltip location when it was dragged with the mouse.
	 * 
	 * @param shell
	 * @param xDiff
	 *            Relative x location when dragging started
	 * @param yDiff
	 *            Relative y location when dragging started
	 */
	private void setDraggedLocation(final Shell shell, final int xDiff, final int yDiff) {

		if (shell == null || shell.isDisposed()) {
			return;
		}

		final Point movedLocation = shell.getLocation();
		movedLocation.x += xDiff;
		movedLocation.y += yDiff;

		final Point size = shell.getSize();
		final Point shellLocation = _toolTip3.fixupDisplayBounds(size, movedLocation);

		shell.setLocation(shellLocation.x, shellLocation.y);
	}

}
