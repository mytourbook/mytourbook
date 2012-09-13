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
class RRShell {

	private Display		_display;
	private Shell		_shell;

	private PageBook	_resizeShellBook;

	private Composite	_resizeShellPageShell;
	private Composite	_resizeShellPageTempImage;
	private Image		_shellReparentableImage;

	public void createUI(final Shell shell) {

		_shell = shell;
		_display = shell.getDisplay();

		_resizeShellBook = new PageBook(shell, SWT.NONE);
		_resizeShellPageShell = createUI_10_ResizePageShell(_resizeShellBook);
		_resizeShellPageTempImage = createUI_20_ResizePageShellImage(_resizeShellBook);
	}

	private Composite createUI_10_ResizePageShell(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FillLayout());

		return container;
	}

	private Composite createUI_20_ResizePageShellImage(final Composite parent) {

		final Canvas resizeCanvas = new Canvas(//
				parent,
//				SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE//
				SWT.NONE //
		);

		resizeCanvas.setLayout(new FillLayout());

		resizeCanvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(final PaintEvent e) {
				onPaintShellImage(e);
			}
		});

		return resizeCanvas;
	}

	public void dispose() {

		_shell.dispose();
		_shell = null;
	}

	public Shell getShell() {
		return _shell;
	}

	private void onPaintShellImage(final PaintEvent event) {

		final GC gc = event.gc;

//		final Rectangle bounds = gc.getClipping();
//		gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
//		gc.fillRectangle(bounds);

		gc.drawImage(_shellReparentableImage, 0, 0);
	}

	public void reparentFromShell(final RRShell fromRRShell, final Composite reparentContainer) {

		final Rectangle fromShellBounds = fromRRShell.getShell().getBounds();

		/*
		 * copy NoResize shell image into the resize shell, to prevent flickering
		 */
		final Image _shellTempImage = new Image(_display, fromShellBounds);

		final GC gc = new GC(fromRRShell.getShell());
		gc.copyArea(_shellTempImage, 0, 0);
		gc.dispose();

		_resizeShellBook.showPage(_resizeShellPageTempImage);

		_rrShellWithResize.setLocation(//
				fromShellBounds.x - _shellTrimWidth,
				fromShellBounds.y - _shellTrimHeight//
		);

		_rrShellWithResize.setAlpha(0x0);

		/*
		 * this will paint the shell image before reparenting takes place
		 */
		_rrShellWithResize.setVisible(true);

		_rrShellWithResize.setAlpha(0xff);

		prevShell.setAlpha(0);

		// reparent UI container
		_ttContentArea.setParent(_resizeShellPageShell);

		_resizeShellBook.showPage(_resizeShellPageShell);

		_shellTempImage.dispose();

	}

	public void setAlpha(final int alpha) {
		_shell.setAlpha(alpha);
	}

	public void setShellImage(final Image shellImage) {
		_shellReparentableImage = shellImage;
	}

	public void setSize(final int width, final int height) {
		_shell.setSize(width, height);
	}

	public void setSize(final Point size) {
		_shell.setSize(size);
	}

	public void setVisible(final boolean isVisible) {
		_shell.setVisible(isVisible);
	}

}
