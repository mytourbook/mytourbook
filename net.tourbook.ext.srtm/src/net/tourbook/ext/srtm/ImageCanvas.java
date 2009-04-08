/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

package net.tourbook.ext.srtm;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class ImageCanvas extends Canvas {

	private Image	fImage;

	/**
	 * @param parent
	 * @param style
	 */
	public ImageCanvas(final Composite parent, final int style) {

		super(parent, style);

		addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent e) {
				if (fImage != null) {

					final Rectangle canvasBounds = getBounds();
					final Rectangle imageBounds = fImage.getBounds();

					e.gc.drawImage(fImage,
							0,
							0,
							imageBounds.width,
							imageBounds.height,
							0,
							0,
							canvasBounds.width,
							canvasBounds.height);
				}
			}
		});
	}

	@Override
	public void dispose() {
		super.dispose();
		fImage.dispose();
	}

	public void paintImage(final Image image) {
		fImage = image;
		redraw();
	}
}
