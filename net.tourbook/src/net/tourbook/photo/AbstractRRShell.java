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

import net.tourbook.common.UI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.PageBook;

/**
 * Reparented Resize Shell
 */
abstract class AbstractRRShell {

	private boolean		_isResizeable;

	private int			_shellTrimWidth;
	private int			_shellTrimHeight;

	/*
	 * UI resources
	 */
	private Display		_display;

	private Shell		_shell;

	private PageBook	_shellBook;
	private Composite	_pageShell;
	private Composite	_pageReparentableImage;

	private Image		_otherShellImage;

	public AbstractRRShell(final Shell parentShell, final int style, final String shellTitle, final boolean isResizeable) {

		_display = parentShell.getDisplay();

		_isResizeable = isResizeable;

		_shell = new Shell(parentShell, style);

		_shell.setText(shellTitle);
		_shell.setLayout(new FillLayout());
		_shell.setSize(getContentSize());

		setTrimSize();

//		_shell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));

		_shellBook = new PageBook(_shell, SWT.NONE);
		{
			_pageShell = createUI_10_ResizePageShell(_shellBook);
			_pageReparentableImage = createUI_20_ResizePageShellImage(_shellBook);
		}
	}

	private Composite createUI_10_ResizePageShell(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FillLayout());

//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		return container;
	}

	private Composite createUI_20_ResizePageShellImage(final Composite parent) {

		final Canvas resizeCanvas = new Canvas(//
				parent,
//				SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE//
				SWT.NONE //
		);

		resizeCanvas.setLayout(new FillLayout());

//		resizeCanvas.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));

		resizeCanvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(final PaintEvent e) {
				onPaintShellImage(e);
			}
		});

		return resizeCanvas;
	}

	private void delay() {
		try {
			Thread.sleep(500);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void dispose() {

		_shell.dispose();
		_shell = null;
	}

	protected abstract Point getContentSize();

	public Shell getShell() {
		return _shell;
	}

	private void onPaintShellImage(final PaintEvent event) {

		final GC gc = event.gc;

//		final Rectangle bounds = gc.getClipping();
//		gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
//		gc.fillRectangle(bounds);

		gc.drawImage(_otherShellImage, 0, 0);
	}

	public void reparentFromOtherShell(final AbstractRRShell otherRRShell, final Composite reparentContainer) {

		final Shell otherShell = otherRRShell.getShell();

		final Rectangle otherBounds = otherShell.getBounds();
		final Rectangle otherClientAreaBounds = otherShell.getClientArea();

		/*
		 * copy NoResize shell image into the resize shell, to prevent flickering
		 */
		_otherShellImage = new Image(_display, otherClientAreaBounds);

		final GC gc = new GC(otherShell);
		gc.copyArea(_otherShellImage, 0, 0);
		gc.dispose();

		_shellBook.showPage(_pageReparentableImage);

		final int shellOffsetX = _shellTrimWidth - otherRRShell._shellTrimWidth;
		final int shellOffsetY = _shellTrimHeight - otherRRShell._shellTrimHeight;

		_shell.setLocation(//
				otherBounds.x - shellOffsetX,
				otherBounds.y - shellOffsetY//
		);

		if (_isResizeable == false) {

			/*
			 * set size ONLY for the shell without resize, size for shell with resize is set by the
			 * user by resizing the window
			 */
			_shell.setSize(getContentSize());
		}

		_shell.setAlpha(0x0);

		/*
		 * this will paint the shell image before reparenting takes place
		 */
		_shell.setVisible(true);

		_shell.setAlpha(0xff);

		otherRRShell.setAlpha(0);

		// reparent UI container
		reparentContainer.setParent(_pageShell);

		_shellBook.showPage(_pageShell);

		_otherShellImage.dispose();
	}

	public void setAlpha(final int alpha) {
		_shell.setAlpha(alpha);
	}

	public void setSize(final int width, final int height) {

		_shell.setSize(width, height);

		System.out.println(UI.timeStampNano() + " setSize 1\t" + width + " x " + height);
		// TODO remove SYSTEM.OUT.PRINTLN
	}

	public void setSize(final Point size) {

		_shell.setSize(size);

		System.out.println(UI.timeStampNano() + " setSize 2\t" + size);
		// TODO remove SYSTEM.OUT.PRINTLN
	}

	private void setTrimSize() {

		final Point contentSize = getContentSize();

		final int contentWidth = contentSize.x;
		final int contentHeight = contentSize.y;

		final Rectangle contentWithTrim = _shell.computeTrim(0, 0, contentWidth, contentHeight);

		System.out.println(UI.timeStampNano() + " shell trim " + contentWithTrim);
		// TODO remove SYSTEM.OUT.PRINTLN

		final int shellTrimWidth = contentWithTrim.width - contentWidth;
		final int shellTrimHeight = contentWithTrim.height - contentHeight;

		_shellTrimWidth = shellTrimWidth / 2;
		_shellTrimHeight = shellTrimHeight / 2;
	}

}
