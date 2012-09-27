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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
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
		_shell.setLayout(new FillLayout());

		_shell.setText(shellTitle);

		setTrimSize();

		setContentSize(getContentSize());

//		_shell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		_shellBook = new PageBook(_shell, SWT.NONE);
		{
			_pageShell = createUI_10_PageShellContent(_shellBook);
			_pageReparentableImage = createUI_20_PageShellImage(_shellBook);
		}

		_shellBook.showPage(_pageShell);
	}

	private Composite createUI_10_PageShellContent(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FillLayout());

//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		return container;
	}

	private Composite createUI_20_PageShellImage(final Composite parent) {

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

	public Point getShellContentLocation() {

		final Point shellLocation = _shell.getLocation();

		final int shellX = shellLocation.x + _shellTrimWidth;
		final int shellY = shellLocation.y + _shellTrimHeight;

		return new Point(shellX, shellY);
	}

	public Point getShellLocation(final Point contentLocation) {

		final int shellX = contentLocation.x - _shellTrimWidth;
		final int shellY = contentLocation.y - _shellTrimHeight;

		return new Point(shellX, shellY);
	}

	public Composite getShellPage() {
		return _pageShell;
	}

	public Point getShellSize(final Point contentSize) {

		final int shellWidth = contentSize.x + _shellTrimWidth * 2;
		final int shellHeight = contentSize.y + _shellTrimHeight * 2;

		return new Point(shellWidth, shellHeight);
	}

	public int getShellTrimHeight() {
		return _shellTrimHeight;
	}

	public int getShellTrimWidth() {
		return _shellTrimWidth;
	}

	private void onPaintShellImage(final PaintEvent event) {

		if (_otherShellImage == null || _otherShellImage.isDisposed()) {
			return;
		}

		final GC gc = event.gc;

//		final Rectangle bounds = gc.getClipping();
//		gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
//		gc.fillRectangle(bounds);

		gc.drawImage(_otherShellImage, 0, 0);
	}

	public void reparentFromOtherShell(final AbstractRRShell otherRRShell, final Composite reparentContainer) {

		final Shell otherShell = otherRRShell.getShell();

		final Rectangle otherShellBounds = otherShell.getBounds();
		final Rectangle otherClientAreaBounds = otherShell.getClientArea();

		/*
		 * copy NoResize shell image into the resize shell, to prevent flickering
		 */
		_otherShellImage = new Image(_display, otherClientAreaBounds);

		final GC gc = new GC(otherShell);
		gc.copyArea(_otherShellImage, 0, 0);
		gc.dispose();

		_shellBook.showPage(_pageReparentableImage);

		/*
		 * set shell position with new trim size so that the shell content is not moving
		 */
		final int trimDiffX = _shellTrimWidth - otherRRShell._shellTrimWidth;
		final int trimDiffY = _shellTrimHeight - otherRRShell._shellTrimHeight;

		final int shellX = otherShellBounds.x - trimDiffX;
		final int shellY = otherShellBounds.y - trimDiffY;

		_shell.setLocation(shellX, shellY);

		if (_isResizeable == false) {

			/*
			 * set size ONLY for the shell without resize, size for shell with resize is set by the
			 * user by resizing the window, when size is not set, the shell is empty and default
			 * size is 2x2
			 */
			setContentSize(getContentSize());
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

	public void setContentSize(final int width, final int height) {

		final int shellWidth = width + _shellTrimWidth * 2;
		final int shellHeight = height + _shellTrimHeight * 2;

		_shell.setSize(shellWidth, shellHeight);
	}

	private void setContentSize(final Point size) {
		setContentSize(size.x, size.y);
	}

	public void setShellLocation(final int x, final int y, final int flag) {
		_shell.setLocation(x, y);
	}

	private void setTrimSize() {

		final Point contentSize = getContentSize();

		final int contentWidth = contentSize.x;
		final int contentHeight = contentSize.y;

		final Rectangle contentWithTrim = _shell.computeTrim(0, 0, contentWidth, contentHeight);

		final int shellTrimWidth = contentWithTrim.width - contentWidth;
		final int shellTrimHeight = contentWithTrim.height - contentHeight;

		_shellTrimWidth = shellTrimWidth / 2;
		_shellTrimHeight = shellTrimHeight / 2;
	}

	@Override
	public String toString() {
		return ("_isResizeable=" + _isResizeable)
				+ ("\tTrimWidth=" + _shellTrimWidth)
				+ ("\tTrimHeight=" + _shellTrimHeight)
				+ ("\t_shell=" + _shell.getText());
	}

	public void updateColors(final Color fgColor, final Color bgColor) {

		_shell.setBackground(bgColor);
	}

}
