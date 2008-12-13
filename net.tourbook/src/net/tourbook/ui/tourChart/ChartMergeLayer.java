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
//	private TourData	fMergeIntoTourData;
	private TourData	fMergeFromTourData;
	private int[]		fXDataSerie;
	private boolean		fIsRelativeAltiDiffScaling;

	public ChartMergeLayer(final TourData mergeIntoTourData, final TourData mergeFromTourData, final int[] xDataSerie) {

//		fMergeIntoTourData = mergeIntoTourData;
		fMergeFromTourData = mergeFromTourData;

		// x-data serie contains the time or distance distance data serie
		fXDataSerie = xDataSerie;

//		fAltitudeSerie = altitudeSerie;
	}

	public void draw(final GC gc, final ChartDrawingData drawingData, final Chart chart) {

		final int xValues[] = fXDataSerie;
		final int yAltitudeValues[] = fMergeFromTourData.mergeAltitudeSerie;
		final int yAltitudeDiffValues[] = fMergeFromTourData.mergeAltitudeDiff;
		final int[] yAdjustedAltitude = fMergeFromTourData.mergeAdjustedAltitudeSerie;

		if (xValues == null || xValues.length == 0 || yAltitudeValues == null || yAltitudeValues.length == 0) {
			return;
		}

		/*
		 * convert all altitude diff values into positive values
		 */
		int maxAltiDiff = 0;
		int altiIndex = 0;
		final int altiDiffValues[] = new int[yAltitudeDiffValues.length];
		for (int altiDiff : yAltitudeDiffValues) {
			altiDiffValues[altiIndex++] = altiDiff = (altiDiff < 0) ? -altiDiff : altiDiff;
			maxAltiDiff = (maxAltiDiff >= altiDiff) ? maxAltiDiff : altiDiff;
		}

		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();
		float altiDiffScaling = scaleY;

		// get the horizontal offset for the graph
		final int graphValueOffset = (int) (Math.max(0, chart.getDevGraphImageXOffset()) / scaleX);

		final Display display = Display.getCurrent();

		final Path pathData = new Path(display);
		final Path pathDiff = new Path(display);
		final Path pathAlti = new Path(display);

		final int graphYBottom = drawingData.getGraphYBottom();

		final int devGraphHeight = drawingData.getDevGraphHeight();
		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = devYBottom - devGraphHeight;

		// get alti diff scaling
		if (fIsRelativeAltiDiffScaling) {
			altiDiffScaling = maxAltiDiff == 0 ? scaleY : (float) devGraphHeight / maxAltiDiff;
		}

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

			final int graphXValue = xValues[xValueIndex] - graphValueOffset;
			final int graphYValue = (int) (yAltitudeValues[xValueIndex] / UI.UNIT_VALUE_ALTITUDE);
			final int graphAltitudeDiff = altiDiffValues[xValueIndex];

			final float devXValue = graphXValue * scaleX;
			final float devYValue = graphYValue * scaleY;
			final float devAltitudeDiff = graphAltitudeDiff * altiDiffScaling;

			if (yAdjustedAltitude != null) {
				final float devYAdjustedAlti = yAdjustedAltitude[xValueIndex] * scaleY;
				if (xValueIndex == startIndex) {

					// move to the first point
					pathAlti.moveTo(devXValue, devChartY0Line - devYAdjustedAlti);
				}

				// draw line to the next point
				pathAlti.lineTo(devXValue, devChartY0Line - devYAdjustedAlti);
			}

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
		RGB rgbGraph;
		if (yAdjustedAltitude == null) {
			rgbGraph = new RGB(0x0, 0x0, 0xFF);
		} else {
			// draw the graph lighter when the adjusted altitude is displayed
			rgbGraph = new RGB(0xbb, 0xbb, 0xbb);
		}
		Color colorFg = new Color(display, rgbGraph);
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

		/*
		 * draw adjusted altitude graph
		 */
		if (yAdjustedAltitude != null) {

//			colorFg = new Color(display, new RGB(0xFF, 0x67, 0x00));
			colorFg = new Color(display, new RGB(0xd8, 0x6a, 0x00));
//			colorFg = new Color(display, new RGB(0x2C, 0x95, 0x2C));
			gc.setForeground(colorFg);

			gc.drawPath(pathAlti);
			colorFg.dispose();
		}

		// dispose resources
		pathData.dispose();
		pathDiff.dispose();
		pathAlti.dispose();
	}

	void setAltiDiffScaling(final boolean isRelativeAltiDiffScaling) {
		fIsRelativeAltiDiffScaling = isRelativeAltiDiffScaling;
	}
}
