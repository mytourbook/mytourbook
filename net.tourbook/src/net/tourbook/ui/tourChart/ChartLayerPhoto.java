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
 * @author Wolfgang Schramm Created: 23.7.2012
 */
package net.tourbook.ui.tourChart;

import java.util.ArrayList;

import net.tourbook.chart.Chart;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class ChartLayerPhoto implements IChartLayer {

	private ArrayList<ChartPhoto>	_chartPhotos;

	/**
	 * Device position where the photo is painted, is <code>null</code> when photo is not visible in
	 * the graph.
	 */
	private Point[]					_photoPositions;

	public ChartLayerPhoto(final ArrayList<ChartPhoto> chartPhotos) {

		_chartPhotos = chartPhotos;
	}

	/**
	 * Draw photos into the current graph.
	 */
	public void draw(final GC gc, final GraphDrawingData drawingData, final Chart chart) {

		if (_photoPositions == null) {
			// is not yet initialized
			_photoPositions = new Point[_chartPhotos.size()];
		}

		final Display display = Display.getCurrent();

		final int devYTop = drawingData.getDevYTop();
		final int devYBottom = drawingData.getDevYBottom();
		final int devGraphImageOffset = chart.getXXDevViewPortLeftBorder();
		final int devGraphHeight = drawingData.devGraphHeight;
		final int devGraphWidth = drawingData.devVirtualGraphWidth;

		final float graphYBottom = drawingData.getGraphYBottom();
		final float[] yValues = drawingData.getYData().getHighValues()[0];
		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();

		gc.setClipping(0, devYTop, devGraphWidth, devGraphHeight);

		gc.setBackground(display.getSystemColor(SWT.COLOR_DARK_GRAY));

		final int lineWidth = 4;
		gc.setLineWidth(lineWidth);
		gc.setAntialias(SWT.ON);

		int photoIndex = 0;

		for (final ChartPhoto chartPhoto : _chartPhotos) {

			final float yValue = yValues[chartPhoto.serieIndex];
			final int devYGraph = (int) ((yValue - graphYBottom) * scaleY) - 0;

			final int devXValue = (int) (chartPhoto.xValue * scaleX) - devGraphImageOffset;
			final int devYValue = devYBottom - devYGraph;

			final int photoIconWidthNoBorder = 10;
			final int photoIconWidth = photoIconWidthNoBorder + lineWidth;
			final int photoIconWidth2 = photoIconWidth / 2;

			final int devXPhoto = devXValue;
			int devYPhoto = devYValue - photoIconWidth - 20;

			// check if photo is visible
			if (devXPhoto + photoIconWidth2 < 0 || devXPhoto - photoIconWidth2 > devGraphWidth) {

				_photoPositions[photoIndex++] = null;

				continue;
			}

			gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));

			// don't draw the photo marker before the chart
			final int devXImageOffset = chart.getXXDevViewPortLeftBorder();
			if (devXImageOffset == 0 && devXPhoto < photoIconWidth) {
//				devXPhoto = photoIconWidth - lineWidth;
//				gc.setForeground(display.getSystemColor(SWT.COLOR_MAGENTA));
			}

			// don't draw the photo marker after the chart
			if (devXPhoto + photoIconWidth > devGraphWidth) {
//				devXPhoto = devGraphWidth - photoIconWidth + lineWidth;
//				gc.setForeground(display.getSystemColor(SWT.COLOR_MAGENTA));
			}

			// force photo marker to be not below the bottom
			if (devYPhoto + photoIconWidth > devYBottom) {
				devYPhoto = devYBottom - photoIconWidth;
//				gc.setForeground(display.getSystemColor(SWT.COLOR_MAGENTA));
			}

			// force photo marker to be not above the top
			if (devYPhoto < devYTop) {
				devYPhoto = devYTop + lineWidth;
//				gc.setForeground(display.getSystemColor(SWT.COLOR_MAGENTA));
			}

			// draw photo
			gc.drawOval(//
					devXPhoto - photoIconWidth2,
					devYPhoto,
					photoIconWidth,
					photoIconWidth);

			// keep photo position which is used when tooltip is displayed
			_photoPositions[photoIndex++] = new Point(devXPhoto, devYPhoto);
		}

		gc.setClipping((Rectangle) null);
		gc.setLineWidth(1);
	}

	public ArrayList<ChartPhoto> getChartPhotos() {
		return _chartPhotos;
	}

	/**
	 * @return Returns device position where the photos are painted, is <code>null</code> when photo
	 *         is not visible in the graph.
	 */
	Point[] getPhotoPositions() {
		return _photoPositions;
	}

}
