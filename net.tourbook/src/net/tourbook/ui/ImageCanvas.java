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
/**
 * @author Wolfgang Schramm
 * @author Alfred Barten
 */
package net.tourbook.ui;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class ImageCanvas extends Canvas {

	private Image	_image;

	private boolean	_isAdaptSize	= false;

	/**
	 * @param parent
	 * @param style
	 */
	public ImageCanvas(final Composite parent, final int style) {

		super(parent, style);

		addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent e) {

				if (_image == null || _image.isDisposed()) {
					return;
				}

				final Rectangle imageBounds = _image.getBounds();

				if (_isAdaptSize) {

					final Rectangle canvasBounds = getBounds();

					e.gc.drawImage(
							_image,
							0,
							0,
							imageBounds.width,
							imageBounds.height,
							0,
							0,
							canvasBounds.width,
							canvasBounds.height);
				} else {

					e.gc.drawImage(
							_image,
							0,
							0,
							imageBounds.width,
							imageBounds.height,
							0,
							0,
							imageBounds.width,
							imageBounds.height);
				}
			}
		});
	}

	@Override
	public void dispose() {
		super.dispose();
		_image.dispose();
	}

	/**
	 * Sets and draws the image, dispose previous image
	 * 
	 * @param image
	 */
	public void setImage(final Image image) {

		if (_image != null) {
			_image.dispose();
		}

		_image = image;
		redraw();
	}

	/**
	 * When <code>true</code> it adapts the size of the image to the available client area
	 * 
	 * @param isAdaptSize
	 */
	public void setIsAdaptSize(final boolean isAdaptSize) {
		_isAdaptSize = isAdaptSize;
	}
}
