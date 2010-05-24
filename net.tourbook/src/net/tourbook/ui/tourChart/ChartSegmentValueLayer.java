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
 * @author Wolfgang Schramm Created: 06.07.2005
 */

package net.tourbook.ui.tourChart;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ChartSegmentValueLayer implements IChartLayer {

	private RGB			lineColorRGB	= new RGB(255, 0, 0);

	private TourData	fTourData;

	private int[]		fXDataSerie;

	/**
	 * Draws the marker(s) for the current graph config
	 * 
	 * @param gc
	 * @param drawingData
	 * @param chartComponents
	 */
	public void draw(final GC gc, final ChartDrawingData drawingData, final Chart chart) {

		final int[] segmentSerie = fTourData.segmentSerieIndex;

		if (segmentSerie == null) {
			return;
		}

		final Display display = Display.getCurrent();

		final int devYTop = drawingData.getDevYTop();
		final int devYBottom = drawingData.getDevYBottom();
		final int devGraphImageXOffset = chart.getDevGraphImageXOffset();
//		final int devGraphHeight = drawingData.getDevGraphHeight();

		final int graphYBottom = drawingData.getGraphYBottom();

		final ChartDataYSerie yData = drawingData.getYData();

		// get the segment values
		final Object segmentValuesObject = yData.getCustomData(TourManager.CUSTOM_DATA_SEGMENT_VALUES);
		if ((segmentValuesObject instanceof float[]) == false) {
			return;
		}
		final float[] segmentValues = (float[]) segmentValuesObject;

		final int valueDivisor = yData.getValueDivisor();
		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();

		final Color lineColor = new Color(display, lineColorRGB);

		Point lastPoint = null;

		for (int segmentIndex = 0; segmentIndex < segmentSerie.length; segmentIndex++) {

			final int serieIndex = segmentSerie[segmentIndex];
			final int devXOffset = (int) (fXDataSerie[serieIndex] * scaleX) - devGraphImageXOffset;

			final float graphYValue = segmentValues[segmentIndex] * valueDivisor;
			final int devYGraph = (int) ((graphYValue - graphYBottom) * scaleY);
			int devYMarker = devYBottom - devYGraph;

			// don't draw over the graph borders
			if (devYMarker > devYBottom) {
				devYMarker = devYBottom;
			}
			if (devYMarker < devYTop) {
				devYMarker = devYTop;
			}

			if (lastPoint == null) {
				lastPoint = new Point(devXOffset, devYMarker);
			} else {

				gc.setForeground(lineColor);

				gc.setLineStyle(SWT.LINE_DOT);
				gc.drawLine(lastPoint.x, lastPoint.y, lastPoint.x, devYMarker);

				gc.setLineStyle(SWT.LINE_SOLID);
				gc.drawLine(lastPoint.x, devYMarker, devXOffset, devYMarker);

				lastPoint.x = devXOffset;
				lastPoint.y = devYMarker;
			}

			// draw a line from the marker to the top of the graph
			gc.setForeground(display.getSystemColor(SWT.COLOR_GRAY));
			gc.setLineStyle(SWT.LINE_DOT);
			gc.drawLine(devXOffset, devYMarker, devXOffset, devYTop);
		}

		lineColor.dispose();
	}

	public void setLineColor(final RGB lineColor) {
		this.lineColorRGB = lineColor;
	}

	public void setTourData(final TourData tourData) {
		fTourData = tourData;
	}

	public void setXDataSerie(final int[] dataSerie) {
		fXDataSerie = dataSerie;
	}
}
