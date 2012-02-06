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
package net.tourbook.photo;

import net.tourbook.photo.manager.Photo;
import net.tourbook.ui.UI;
import net.tourbook.util.Util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * Canvas where the photo is painted.
 */
public class PhotoCanvas extends Canvas {

	private int		_prevCanvasWidth;
	private int		_prevCanvasHeight;

	private Image	_photoImage;
	private Image	_scaledImage;
	private int		_prevScaledWidth;
	private int		_prevScaledHeight;

	/**
	 * @param parent
	 * @param style
	 */
	public PhotoCanvas(final Composite parent, final int style) {

		super(parent, style);

		addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent e) {

				if (_photoImage == null || _photoImage.isDisposed()) {
					return;
				}

				paintPhoto(e);
			}

		});

		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
//				_isCanvasResized = true;
			}
		});
	}

	@Override
	public void dispose() {

		super.dispose();

		Util.disposeResource(_photoImage);
		Util.disposeResource(_scaledImage);
	}

	private void paintPhoto(final PaintEvent paintEvent) {

		final GC gc = paintEvent.gc;
		gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));

		final Rectangle photoBounds = _photoImage.getBounds();
		final int photoWidth = photoBounds.width;
		final int photoHeight = photoBounds.height;

		final Rectangle canvasBounds = getBounds();
		final int canvasWidth = canvasBounds.width;
		final int canvasHeight = canvasBounds.height;

		if (photoWidth == canvasWidth && photoHeight == canvasHeight) {

			// photo and canvas has the exact size (this is very unlikely)

			gc.drawImage(_photoImage, 0, 0, photoWidth, photoHeight, 0, 0, canvasWidth, canvasHeight);

		} else if (photoWidth > canvasWidth || photoHeight > canvasHeight) {

			// photo is larger than the canvas

			int devX = 0;
			int devY = 0;
			int scaledWidth;
			int scaledHeight;

			final float photoRatio = (float) photoWidth / photoHeight;
			final float canvasRatio = (float) canvasWidth / canvasHeight;

			if (photoRatio >= canvasRatio) {

				// set photo width to canvas width

				scaledWidth = canvasWidth;
				scaledHeight = (int) (canvasWidth / photoRatio);

				devY = (canvasHeight - scaledHeight) / 2;

			} else {

				// set photo height to canvas height

				scaledWidth = (int) (canvasHeight * photoRatio);
				scaledHeight = canvasHeight;

				devX = (canvasWidth - scaledWidth) / 2;
			}

			final boolean hasWidthChanged = scaledWidth != _prevScaledWidth;
			final boolean hasHeightChanged = scaledHeight != _prevScaledHeight;

			// create scaled image when necessary
			if ((canvasWidth != _prevCanvasWidth && hasHeightChanged)
					|| (canvasHeight != _prevCanvasHeight && hasWidthChanged)
					|| _scaledImage == null
					|| _scaledImage.isDisposed()) {

				_scaledImage = resizeImage(_photoImage, scaledWidth, scaledHeight);
			}

			/*
			 * draw background for each segment, otherwise it is flickering
			 */
			if (devX > 0) {

				// draw left border
				gc.fillRectangle(0, 0, devX - 0, canvasHeight);

				// draw right border
				gc.fillRectangle(devX + scaledWidth + 0, 0, devX + 1, canvasHeight);

			} else {

				// draw top border
				gc.fillRectangle(0, 0, canvasWidth, devY - 0);

				// draw  bottom border
				gc.fillRectangle(0, devY + scaledHeight + 0, canvasWidth, devY + 1);
			}

			// draw image
			gc.drawImage(_scaledImage, devX, devY);

			_prevScaledWidth = scaledWidth;
			_prevScaledHeight = scaledHeight;

		} else {

			// photo is smaller than the canvas

			final int devX = (canvasWidth - photoWidth) / 2;
			final int devY = (canvasHeight - photoHeight) / 2;

			// draw left border
			gc.fillRectangle(0, 0, devX - 0, canvasHeight);

			// draw right border
			gc.fillRectangle(devX + photoWidth + 0, 0, devX + 1, canvasHeight);

			// draw top border
			gc.fillRectangle(0, 0, canvasWidth, devY - 0);

			// draw  bottom border
			gc.fillRectangle(0, devY + photoHeight + 0, canvasWidth, devY + 1);

			// draw image
			gc.drawImage(_photoImage, devX, devY);
		}

		_prevCanvasWidth = canvasWidth;
		_prevCanvasHeight = canvasHeight;
	}

	private Image resizeImage(final Image image, final int width, final int height) {

		final Image scaledImage = new Image(Display.getDefault(), width, height);
		final GC gc = new GC(scaledImage);
		{
			gc.setAntialias(SWT.ON);
			gc.setInterpolation(SWT.HIGH);

			gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, 0, 0, width, height);
		}
		gc.dispose();

		return scaledImage;
	}

	/**
	 * Sets and draws the image and disposes previous image.
	 * 
	 * @param image
	 * @param photo
	 */
	public void setImage(final Image image, final Photo photo) {

		Util.disposeResource(_photoImage);
		Util.disposeResource(_scaledImage);

		switch (photo.getOrientation()) {
		case 6:

			_photoImage = new Image(this.getDisplay(), UI.rotate(image.getImageData(), SWT.RIGHT));
			image.dispose();

			break;

		default:
			_photoImage = image;
			break;
		}

		redraw();
	}

}
