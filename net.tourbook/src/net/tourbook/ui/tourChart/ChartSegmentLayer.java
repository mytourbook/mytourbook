/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDrawingData;
import net.tourbook.chart.ChartMarker;
import net.tourbook.chart.IChartLayer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ChartSegmentLayer implements IChartLayer {

	private ArrayList<ChartMarker>	fChartMarkers	= new ArrayList<ChartMarker>();

	private RGB						lineColor		= new RGB(189, 0, 255);

	/**
	 * Adds a new marker to the internal marker list, the list can be retrieved with getMarkerList()
	 * 
	 * @param chartMarker
	 * @param xCoord
	 *            Position of the marker on the x axis
	 * @param label
	 */
	public void addMarker(final ChartMarker chartMarker) {
		fChartMarkers.add(chartMarker);
	}

	/**
	 * Draws the marker(s) for the current graph config
	 * 
	 * @param gc
	 * @param drawingData
	 * @param fChartComponents
	 */
	public void draw(final GC gc, final ChartDrawingData drawingData, final Chart chart) {

		final Display display = Display.getCurrent();

		final int devYTop = drawingData.getDevYTop();
		final int devYBottom = drawingData.getDevYBottom();
		final int devGraphImageOffset = chart.getDevGraphImageXOffset();
		final int devGraphHeight = drawingData.getDevGraphHeight();

		final int graphYBottom = drawingData.getGraphYBottom();
		final int[] yValues = drawingData.getYData().getHighValues()[0];
		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();

		final Color colorLine = new Color(display, getLineColor());
		Point lastPoint = null;

		for (final ChartMarker chartMarker : fChartMarkers) {

			final int devXOffset = (int) (chartMarker.graphX * scaleX) - devGraphImageOffset;

			final int yValueIndex = Math.min(yValues.length - 1, chartMarker.serieIndex);
			final int yValue = yValues[yValueIndex];

			final int devYGraph = (int) ((yValue - graphYBottom) * scaleY) - 0;
			int devYSegment = devYBottom - devYGraph;

			// don't draw over the graph borders
			if (devYSegment > devYBottom) {
				devYSegment = devYBottom;
			}
			if (devYSegment < devYTop) {
				devYSegment = devYTop;
			}

			gc.setForeground(colorLine);

			// connect the two segments with a line
			if (lastPoint == null) {
				lastPoint = new Point(devXOffset, devYSegment);
			} else {
				gc.setLineStyle(SWT.LINE_SOLID);
				gc.drawLine(lastPoint.x, lastPoint.y, devXOffset, devYSegment);

				lastPoint.x = devXOffset;
				lastPoint.y = devYSegment;
			}

			gc.setForeground(colorLine);

			// draw a line from the marker to the top of the graph
			gc.setForeground(display.getSystemColor(SWT.COLOR_GRAY));
			gc.setLineStyle(SWT.LINE_DOT);
			gc.drawLine(devXOffset, devYSegment, devXOffset, devYBottom - devGraphHeight);

			gc.setForeground(colorLine);

//			// draw marker label
//			final String markerLabel = chartMarker.markerLabel;
//			if (markerLabel.length() > 0) {
//
//				labelExtend = gc.textExtent(markerLabel);
//
//				final int xPos = devXOffset - labelExtend.y;
//				int yPos = yMarkerBar - labelExtend.y;
//
//				yPos = devYBottom - 5;
//
//				if (chart.getAdvancedGraphics()) {
//
//					final Transform tr = new Transform(display);
//					tr.translate(xPos, yPos);
//					tr.rotate(-90f);
//
//					gc.setTransform(tr);
//					// gc.setAntialias(SWT.ON);
//
//					gc.drawText(markerLabel, 0, 0, true);
//
//					gc.setTransform(null);
//					// gc.setAntialias(SWT.OFF);
//
//					tr.dispose();
//
//				} else {
//					gc.drawText(markerLabel, xPos, yPos, true);
//				}
//			}
		}

		colorLine.dispose();
	}

	public RGB getLineColor() {
		return lineColor;
	}

	public void setLineColor(final RGB lineColor) {
		this.lineColor = lineColor;
	}
}
