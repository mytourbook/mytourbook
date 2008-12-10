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
//	private TourData	fMergeIntoTourData;
	private TourData	fMergeFromTourData;
	private int[]		fXDataSerie;

	public ChartMergeLayer(final TourData mergeIntoTourData, final TourData mergeFromTourData, final int[] xDataSerie) {

//		fMergeIntoTourData = mergeIntoTourData;
		fMergeFromTourData = mergeFromTourData;

		// x-data serie contains the time or distance distance data serie
		fXDataSerie = xDataSerie;
	}

	public void draw(final GC gc, final ChartDrawingData drawingData, final Chart chart) {

		final int xValues[] = fXDataSerie;
		final int yAltitudeValues[] = fMergeFromTourData.mergeAltitudeSerie;
		final int yAltitudeDiffValues[] = fMergeFromTourData.mergeAltitudeDiff;

		if (xValues == null || xValues.length == 0 || yAltitudeValues == null || yAltitudeValues.length == 0) {
			return;
		}

		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();

		// get the horizontal offset for the graph
		final int graphValueOffset = (int) (Math.max(0, chart.getDevGraphImageXOffset()) / scaleX);

		final Display display = Display.getCurrent();
		final Path pathData = new Path(display);
		final Path pathDiff = new Path(display);

		final int graphYBottom = drawingData.getGraphYBottom();

		final int devGraphHeight = drawingData.getDevGraphHeight();
		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = devYBottom - devGraphHeight;

		// virtual 0 line for the y-axis of the chart in dev units
		final float devChartY0Line = devYBottom + (scaleY * graphYBottom);

		final int startIndex = 0;
		final int endIndex = xValues.length;

		gc.setClipping(0, devYTop, gc.getClipping().width, devGraphHeight);

		// draw the lines into the path
		for (int xValueIndex = startIndex; xValueIndex < endIndex; xValueIndex++) {

			// make sure the x-index is not higher than the yValues length
			if (xValueIndex >= yAltitudeValues.length) {
				return;
			}

			final int xValue = xValues[xValueIndex] - graphValueOffset;// + xMergeOffset;
			final int yValue = yAltitudeValues[xValueIndex];// + yMergeOffset;

			final float devXValue = xValue * scaleX;
			final float devYValue = yValue * scaleY;

			final int graphAltitudeDiff = yAltitudeDiffValues[xValueIndex];
			final float devAltitudeDiff = ((graphAltitudeDiff < 0) ? -graphAltitudeDiff : graphAltitudeDiff) * scaleY;

			/*
			 * set first point
			 */
			if (xValueIndex == startIndex) {

				// move to the first point

				pathData.moveTo(devXValue, devChartY0Line - devYValue);
				pathDiff.moveTo(devXValue, devYBottom - devAltitudeDiff);
			}

			// draw line to the next point
			pathData.lineTo(devXValue, devChartY0Line - devYValue);
			pathDiff.lineTo(devXValue, devYBottom - devAltitudeDiff);
		}

		// draw the line of the graph
		gc.setAntialias(SWT.OFF);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(1);

		/*
		 * draw data graph
		 */
		Color colorFg = new Color(display, new RGB(0x00, 0x00, 0xff));
		gc.setForeground(colorFg);

		gc.drawPath(pathData);
		colorFg.dispose();

		/*
		 * draw altitude diff graph
		 */
		colorFg = new Color(display, new RGB(0xff, 0x00, 0x77));
		gc.setForeground(colorFg);

		gc.drawPath(pathDiff);
		colorFg.dispose();

		// dispose resources
		pathData.dispose();
		pathDiff.dispose();
	}
}
