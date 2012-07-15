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
package net.tourbook.photo.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class ImageSizeIndicator extends Canvas {

	private boolean	_isHqImage;

	public ImageSizeIndicator(final Composite parent, final int style) {

		super(parent, style);

		addPaintListener(new PaintListener() {

			@Override
			public void paintControl(final PaintEvent e) {
				onPaint(e);
			}
		});
	}

	private void onPaint(final PaintEvent paintEvent) {

		final GC gc = paintEvent.gc;

		final Rectangle bounds = getBounds();

		final int margin = 0;
		final int margin2 = 2 * margin;
		final int width = bounds.width;
		final int height = bounds.height;

		// debug box
		gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		gc.fillRectangle(0, 0, bounds.width, bounds.height);

		if (_isHqImage) {
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));
		} else {
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_GREEN));
		}
		gc.fillRectangle(margin, margin, width - margin2, height - margin2);
	}

	void setIndicator(final boolean isHqImage) {
		_isHqImage = isHqImage;
		redraw();
	}

}
