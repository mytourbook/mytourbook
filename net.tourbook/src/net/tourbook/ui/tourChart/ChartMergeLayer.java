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
import net.tourbook.ui.UI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
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

		final ChartDataYSerie yData = drawingData.getYData();
		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();
		float altiDiffScaling = scaleY;

		// get the horizontal offset for the graph
		final int graphValueOffset = (int) (Math.max(0, chart.getDevGraphImageXOffset()) / scaleX);

		final Display display = Display.getCurrent();

		final Path pathAltitude = new Path(display);
		final Path pathAltiDiff = new Path(display);
		final Path pathAdjustAlti = new Path(display);

		final RGB rgbFg = yData.getRgbLine()[0];
		final RGB rgbBg1 = yData.getRgbDark()[0];
		final RGB rgbBg2 = yData.getRgbBright()[0];

		final int graphYTop = drawingData.getGraphYTop();
		final int graphYBottom = drawingData.getGraphYBottom();

		final int devGraphHeight = drawingData.getDevGraphHeight();
		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = devYBottom - devGraphHeight;

		// get alti diff scaling
		if (fIsRelativeAltiDiffScaling) {
			altiDiffScaling = maxAltiDiff == 0 ? scaleY : (float) devGraphHeight / maxAltiDiff;
		}

		// position for the x-axis line in the graph
		final float devY0 = devYBottom + (scaleY * graphYBottom);

		final int startIndex = 0;
		final int endIndex = xValues.length;

		final Rectangle graphRect = new Rectangle(0, devYTop, gc.getClipping().width, devGraphHeight);

		/*
		 * create paths
		 */
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

			/*
			 * draw adjusted altitude graph
			 */
			if (yAdjustedAltitude != null) {

				final float devYAdjustedAlti = yAdjustedAltitude[xValueIndex] * scaleY;

				if (xValueIndex == startIndex) {

					// move to the first point
					pathAdjustAlti.moveTo(0, devYBottom);
					pathAdjustAlti.lineTo(devXValue, devY0 - devYAdjustedAlti);
				}

				// draw line to the next point
				pathAdjustAlti.lineTo(devXValue, devY0 - devYAdjustedAlti);

				if (xValueIndex == endIndex - 1) {

					/*
					 * this is the last point, draw the line to the x-axis and the start of the
					 * chart
					 */
					pathAdjustAlti.lineTo(devXValue, devYBottom);
//					pathAlti.moveTo(0, devYBottom);
				}
			}

			/*
			 * draw altitude and diff value graph
			 */
			if (xValueIndex == startIndex) {

				// move to the first point
				pathAltitude.moveTo(devXValue, devY0 - devYValue);
				pathAltiDiff.moveTo(devXValue, devYBottom - devAltitudeDiff);
			}

			// draw line to the next point
			pathAltitude.lineTo(devXValue, devY0 - devYValue);
			pathAltiDiff.lineTo(devXValue, devYBottom - devAltitudeDiff);

		}

		// draw the line of the graph
		gc.setAntialias(SWT.OFF);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(1);
		gc.setClipping(graphRect);

		/*
		 * draw data graph
		 */
//		final RGB rgbGraph = new RGB(0x0, 0x0, 0xFF);
		final RGB rgbGraph = new RGB(0xFF, 0x7C, 0x24);
		Color colorFg = new Color(display, rgbGraph);
		gc.setForeground(colorFg);
		gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		gc.drawPath(pathAltitude);
		colorFg.dispose();

		/*
		 * draw adjusted altitude graph
		 */
		if (yAdjustedAltitude != null) {


			colorFg = new Color(display, new RGB(0xFF, 0x7C, 0x24));
			final Color colorBg1 = new Color(display, rgbBg1);

			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
//			gc.setForeground(colorFg);
			gc.setBackground(colorBg1);
			
			gc.setAlpha(0x80);

			// fill background
			gc.setClipping(pathAdjustAlti);
			gc.fillGradientRectangle(0, devYTop, gc.getClipping().width, devGraphHeight, true);
			gc.setClipping(graphRect);

			// draw graph
			gc.drawPath(pathAdjustAlti);

			colorFg.dispose();
			colorBg1.dispose();

			gc.setAlpha(0xff);
		}

		/*
		 * draw altitude diff graph
		 */
//		colorFg = new Color(display, new RGB(0xFF, 0x24, 0x24));
//		colorFg = new Color(display, new RGB(0x00, 0xA2, 0x8B));
		colorFg = new Color(display, new RGB(0x00, 0xA2, 0x8B));
		gc.setForeground(colorFg);
		gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));

		gc.drawPath(pathAltiDiff);
		colorFg.dispose();

		// dispose resources
		pathAltitude.dispose();
		pathAltiDiff.dispose();
		pathAdjustAlti.dispose();
	}

	void setAltiDiffScaling(final boolean isRelativeAltiDiffScaling) {
		fIsRelativeAltiDiffScaling = isRelativeAltiDiffScaling;
	}
}
