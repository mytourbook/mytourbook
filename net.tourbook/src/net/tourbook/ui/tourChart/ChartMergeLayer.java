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
import net.tourbook.chart.ChartDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.data.TourData;
import net.tourbook.ui.UI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ChartMergeLayer implements IChartLayer {

	/**
	 * this is {@link TourData} which is displayed in the chart
	 */
	private TourData	fMergeIntoTourData;
	private TourData	fMergeFromTourData;

	public ChartMergeLayer(final TourData mergeIntoTourData, final TourData mergeFromTourData) {

		fMergeIntoTourData = mergeIntoTourData;
		fMergeFromTourData = mergeFromTourData;
	}

	public void draw(final GC gc, final ChartDrawingData drawingData, final Chart chart) {

		final int xValues[] = fMergeFromTourData.timeSerie;
		final int yValues[] = fMergeFromTourData.getAltitudeSerie();

		if (xValues == null || xValues.length == 0 || yValues == null || yValues.length == 0) {
			return;
		}

		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();

//		final ChartDataYSerie yData = drawingData.getYData();

		// get the horizontal offset for the graph
		final int graphValueOffset = (int) (Math.max(0, chart.getDevGraphImageXOffset()) / scaleX);

		final Display display = Display.getCurrent();
		final Path path = new Path(display);

		final int graphYBottom = drawingData.getGraphYBottom();
//		final int graphYTop = drawingData.getGraphYTop();

		final int xMergeOffset = fMergeIntoTourData.getMergedTourTimeOffset();
		final int yMergeOffset = (int) (fMergeIntoTourData.getMergedAltitudeOffset() * UI.UNIT_VALUE_ALTITUDE);

		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = devYBottom - drawingData.getDevGraphHeight();

		// virtual 0 line for the y-axis of the chart in dev units
		final float devChartY0Line = devYBottom + (scaleY * graphYBottom);

		final int startIndex = 0;
		final int endIndex = xValues.length;

		gc.setClipping(0, devYTop, gc.getClipping().width, devYBottom - devYTop);

		// draw the lines into the path
		for (int xValueIndex = startIndex; xValueIndex < endIndex; xValueIndex++) {

			// make sure the x-index is not higher than the yValues length
			if (xValueIndex >= yValues.length) {
				return;
			}

			final int xValue = xValues[xValueIndex] - graphValueOffset + xMergeOffset;
			final float devXValue = xValue * scaleX;

			// force the bottom and top value not to drawn over the border
			final int yValue = yValues[xValueIndex] + yMergeOffset;
// I don't like to draw a line at the top and bottom			
//			if (yValue < graphYBottom) {
//				yValue = graphYBottom;
//			}
//			if (yValue > graphYTop) {
//				yValue = graphYTop;
//			}

			/*
			 * set first point
			 */
			if (xValueIndex == startIndex) {

				// move to the first point

				path.moveTo(devXValue, devChartY0Line - (yValue * scaleY));
			}

			// draw line to the next point
			path.lineTo(devXValue, devChartY0Line - (yValue * scaleY));

			/*
			 * set last point
			 */
			if (xValueIndex == endIndex - 1) {
				path.lineTo(devXValue, devChartY0Line - (yValue * scaleY));
			}
		}

		final Color colorFg = new Color(display, new RGB(0x00, 0x00, 0xff));

		// draw the line of the graph
		gc.setAntialias(SWT.OFF);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(1);
		gc.setForeground(colorFg);

		gc.drawPath(path);

		// dispose resources
		colorFg.dispose();
		path.dispose();
	}
}
