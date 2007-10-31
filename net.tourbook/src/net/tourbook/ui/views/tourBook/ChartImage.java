/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class ChartImage extends Canvas {

	private Image	fChartImage;

	private int		fChartPosX;
	private int		fChartPosY;

//	private Text	fText;

	public ChartImage(Composite parent, int style) {
		super(parent, style | SWT.NO_BACKGROUND /* | SWT.BORDER */);

		addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent event) {
				drawChartImage(event.gc);
			}
		});
	}

	private void drawChartImage(GC gc) {

		if (fChartImage == null || fChartImage.isDisposed()) {
			return;
		}

		Rectangle clientRect = getClientArea();
		Rectangle chartRect = fChartImage.getBounds();

		final int clientWidth = clientRect.width;
		final int clientHeight = clientRect.height;

		final int srcLeft = fChartPosX - clientWidth / 2;
		final int srcTop = fChartPosY - clientHeight / 2;

		final int srcX = Math.max(0, srcLeft);
		final int srcY = Math.max(0, srcTop);

		int srcWidth = Math.min(clientWidth, chartRect.width - srcX);
		int srcHeight = Math.min(clientHeight, chartRect.height - srcY);

		int destX = srcLeft < 0 ? -srcLeft : 0;
		int destY = srcTop < 0 ? -srcTop : 0;

		gc.drawImage(fChartImage,
				srcX,
				srcY,
				srcWidth,
				srcHeight,
				destX,
				destY,
				srcWidth,
				srcHeight);

		if (srcLeft < 0) {
			gc.fillRectangle(0, 0, destX, clientHeight);
		}

		if (srcTop < 0) {
			gc.fillRectangle(0, 0, clientWidth, destY);
		}

		if (clientWidth > srcWidth) {
			if (srcLeft < 0) {
				gc.fillRectangle(-srcLeft + srcWidth, 0, clientWidth, clientHeight);
			} else {
				gc.fillRectangle(srcWidth, 0, clientWidth, clientHeight);
			}
		}
		if (clientHeight > srcHeight) {
			if (srcTop < 0) {
				gc.fillRectangle(0, -srcTop + srcHeight, clientWidth, clientHeight);
			} else {
				gc.fillRectangle(0, srcHeight, clientWidth, clientHeight);
			}
		}
	}

	public void draw(int xPos, int yPos, Text text) {

		fChartPosX = xPos;
		fChartPosY = yPos;
//		fText = text;

		redraw();
	}

	public void setImage(Image chartCoreImage) {
		fChartImage = chartCoreImage;
	}

}
