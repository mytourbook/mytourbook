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
import net.tourbook.common.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * A flexiable tool can be moved with the mouse.
 */
class FlexTool {

//	private static final int	WINDOW_TITLE_HEIGHT	= UI.IS_OSX ? SWT.DEFAULT : 16;

//	private boolean			_isTTDragged;
//	private int				_devXTTMouseDown;
//	private int				_devYTTMouseDown;

	private ToolTip3		_toolTip3;
	private ToolTip3Tool	_tooltipTool;

//	private boolean			_isToolMoved;

	/*
	 * UI controls
	 */
//	private Shell				_shell;
	private Composite		_headerContainer;
	private Button			_btnDefaultLocation;
	private Button			_btnClose;

	FlexTool(final ToolTip3 toolTip3, final ToolTip3Tool tooltipTool) {

		_toolTip3 = toolTip3;
		_tooltipTool = tooltipTool;
	}

	private void addHeaderListener(final Composite header) {

		header.addMouseTrackListener(new MouseTrackListener() {

			@Override
			public void mouseEnter(final MouseEvent e) {
				onMouseEnter(header);
			}

			@Override
			public void mouseExit(final MouseEvent e) {
				onMouseExit(header);
			}

			@Override
			public void mouseHover(final MouseEvent e) {}
		});

		header.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(final MouseEvent event) {
				onMouseMove(header, event);
			}
		});

		header.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent event) {
				onMouseDown(header, event);
			}

			@Override
			public void mouseUp(final MouseEvent e) {
				onMouseUp(header);
			}
		});
	}

	void createUI(final Shell shell) {

//		_shell = shell;

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

		setHeaderColor();
	}

	private void createUI_10_Custom(final Composite parent) {

		createUI_20_ToolTipHeader(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults() //
				.extendedMargins(ToolTip3.SHELL_MARGIN, ToolTip3.SHELL_MARGIN, 0, ToolTip3.SHELL_MARGIN)
				.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			_tooltipTool.getToolProvider().createToolUI(container);
		}
	}

	private void createUI_20_ToolTipHeader(final Composite parent) {

		final int WINDOW_TITLE_HEIGHT = UI.IS_OSX ? SWT.DEFAULT : 16;
		final String btnCloseText = UI.IS_OSX ? "x" : UI.SPACE;
		final String btnDefaultText = UI.IS_OSX ? "M" : UI.SPACE;

		_headerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()
				.grab(true, false)
				.hint(SWT.DEFAULT, WINDOW_TITLE_HEIGHT)
				.applyTo(_headerContainer);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.spacing(0, 0)
				.applyTo(_headerContainer);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		// show layer title in the hovered tooltip header
		_headerContainer.setToolTipText(_tooltipTool.getToolProvider().getToolTitle());

		addHeaderListener(_headerContainer);

		{
			/*
			 * button: close
			 */
			_btnClose = new Button(_headerContainer, SWT.NONE);

			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.grab(true, true)
//					.indent(0, 4)
//					.hint(WINDOW_TITLE_HEIGHT, SWT.DEFAULT)
					.applyTo(_btnClose);

//			_btnClose.setText(Messages.Map3_PropertyTooltip_Action_Close);
			_btnClose.setText(btnCloseText);
			_btnClose.setToolTipText(Messages.Map3_PropertyTooltip_Action_Close_Tooltip);

			_btnClose.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					// hide tooltip
					_btnClose.getShell().close();
				}
			});

			/*
			 * button: default location
			 */
			_btnDefaultLocation = new Button(_headerContainer, SWT.NONE);

			GridDataFactory.fillDefaults()//
					.align(SWT.END, SWT.CENTER)
					.grab(false, true)
//					.indent(0, 2)
//					.hint(WINDOW_TITLE_HEIGHT, SWT.DEFAULT)
					.applyTo(_btnDefaultLocation);

			_btnDefaultLocation.setText(btnDefaultText);
//			_btnDefaultLocation.setText("«");
//			_btnDefaultLocation.setText("•");
//			_btnDefaultLocation.setText(Messages.Map3_PropertyTooltip_Action_MoveToDefaultLocation);
			_btnDefaultLocation.setToolTipText(Messages.Map3_PropertyTooltip_Action_MoveToDefaultLocation_Tooltip);

			_btnDefaultLocation.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectDefaultLocation();
				}
			});

			_btnDefaultLocation.setEnabled(false);
		}
	}

	boolean isMoved() {
		return false;
//		return _isToolMoved;
	}

	private void onMouseDown(final Composite header, final MouseEvent event) {

//		_isTTDragged = true;
//
//		_devXTTMouseDown = event.x;
//		_devYTTMouseDown = event.y;
//
//		header.setCursor(_toolTip3.getCursorDragged());
	}

	private void onMouseEnter(final Composite header) {

//		header.setCursor(_toolTip3.getCursorHand());
	}

	private void onMouseExit(final Composite header) {

//		_isTTDragged = false;
//
//		header.setCursor(null);
	}

	private void onMouseMove(final Composite header, final MouseEvent event) {

//		if (_isTTDragged) {
//
//			final int xDiff = event.x - _devXTTMouseDown;
//			final int yDiff = event.y - _devYTTMouseDown;
//
//			setDraggedLocation(header.getShell(), xDiff, yDiff);
//		}
	}

	private void onMouseUp(final Composite header) {

//		if (_isTTDragged) {
//
//			_isTTDragged = false;
//
//			updateUI_MoveLocation(true);
//
//			_toolTip3.disableDisplayListener();
//		}
//
//		header.setCursor(_toolTip3.getCursorHand());
	}

	private void onSelectDefaultLocation() {

		updateUI_MoveLocation(false);

		_toolTip3.moveToDefaultLocation(_tooltipTool);
	}

//	/**
//	 * Set tooltip location when it was dragged with the mouse.
//	 *
//	 * @param shell
//	 * @param xDiff
//	 *            Relative x location when dragging started
//	 * @param yDiff
//	 *            Relative y location when dragging started
//	 */
//	private void setDraggedLocation(final Shell shell, final int xDiff, final int yDiff) {
//
//		if (shell == null || shell.isDisposed()) {
//			return;
//		}
//
//		final Point movedLocation = shell.getLocation();
//		movedLocation.x += xDiff;
//		movedLocation.y += yDiff;
//
//		final Point size = shell.getSize();
//		final Point shellLocation = _toolTip3.fixupDisplayBounds(size, movedLocation);
//
//		shell.setLocation(shellLocation.x, shellLocation.y);
//	}

	private void setHeaderColor() {

//		final Color headerBg = _isToolMoved //
//				? Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW)
//				: Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);

		final Color headerBg = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);

		_headerContainer.setBackground(headerBg);
		_btnDefaultLocation.setBackground(headerBg);
		_btnClose.setBackground(headerBg);
	}

	void setMoved() {
		updateUI_MoveLocation(true);
	}

	private void updateUI_MoveLocation(final boolean isMoved) {

//		// set move state
//		_isToolMoved = isMoved;

		// update UI
		_btnDefaultLocation.setEnabled(isMoved);

		setHeaderColor();
	}

}
