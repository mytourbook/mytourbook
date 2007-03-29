/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

package net.tourbook.tour;

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
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;

public class ChartSegmentLayer implements IChartLayer {

	private ArrayList<ChartMarker>	fChartMarkers	= new ArrayList<ChartMarker>();

	private RGB						lineColor		= new RGB(189, 0, 255);

	/**
	 * Adds a new marker to the internal marker list, the list can be retrieved
	 * with getMarkerList()
	 * 
	 * @param marker
	 * @param xCoord
	 *        Position of the marker on the x axis
	 * @param label
	 */
	public void addMarker(ChartMarker marker) {
		fChartMarkers.add(marker);
	}

	public RGB getLineColor() {
		return lineColor;
	}

	public void setLineColor(RGB lineColor) {
		this.lineColor = lineColor;
	}

	/**
	 * Draws the marker(s) for the current graph config
	 * 
	 * @param gc
	 * @param drawingData
	 * @param fChartComponents
	 */
	public void draw(GC gc, ChartDrawingData drawingData, Chart chart) {

		Display display = Display.getCurrent();

		final int devYTop = drawingData.getDevYTop();
		final int devYBottom = drawingData.getDevYBottom();
		final int devGraphImageOffset = chart.getDevGraphImageXOffset();
		final int devGraphHeight = drawingData.getDevGraphHeight();

		final int graphYBottom = drawingData.getGraphYBottom();

		final int yMarkerBar = drawingData.getDevMarginTop()
				+ drawingData.getDevXTitelBarHeight()
				+ drawingData.getDevMarkerBarHeight();

		// yMarkerBar=
		final int[] yValues = drawingData.getYData().getHighValues()[0];
		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();

		Color colorLine = new Color(display, getLineColor());
		Point labelExtend;
		Point lastPoint = null;

		for (ChartMarker chartMarker : fChartMarkers) {

			int devXOffset = (int) (chartMarker.graphX * scaleX) - devGraphImageOffset;
			
			int yValueIndex = Math.min(yValues.length - 1, chartMarker.serieIndex);
			int yValue = yValues[yValueIndex];
			
			int devYGraph = (int) ((float) (yValue - graphYBottom) * scaleY) - 0;
			int devYMarker = devYBottom - devYGraph;

			// don't draw over the graph borders
			if (devYMarker > devYBottom) {
				devYMarker = devYBottom;
			}
			if (devYMarker < devYTop) {
				devYMarker = devYTop;
			}

			gc.setForeground(colorLine);

			// connect the two marker with a line
			if (lastPoint == null) {
				lastPoint = new Point(devXOffset, devYMarker);
			} else {
				gc.setLineStyle(SWT.LINE_SOLID);
				gc.drawLine(lastPoint.x, lastPoint.y, devXOffset, devYMarker);

				lastPoint.x = devXOffset;
				lastPoint.y = devYMarker;
			}

			gc.setForeground(colorLine);

			// draw a line from the marker to the top of the graph
			gc.setForeground(display.getSystemColor(SWT.COLOR_GRAY));
			gc.setLineStyle(SWT.LINE_DOT);
			gc.drawLine(devXOffset, devYMarker, devXOffset, devYBottom - devGraphHeight);

			gc.setForeground(colorLine);

			// draw marker label
			if (!chartMarker.markerLabel.equalsIgnoreCase("")) { //$NON-NLS-1$

				labelExtend = gc.textExtent(chartMarker.markerLabel);

				int xPos = devXOffset - labelExtend.y;
				int yPos = yMarkerBar - labelExtend.y;

				yPos = devYBottom - 5;

				if (chart.getAdvancedGraphics()) {
					Transform tr = new Transform(display);
					tr.translate(xPos, yPos);
					tr.rotate(-90f);

					gc.setTransform(tr);
					// gc.setAntialias(SWT.ON);

					gc.drawText(chartMarker.markerLabel, 0, 0, true);

					gc.setTransform(null);
					// gc.setAntialias(SWT.OFF);

					tr.dispose();
				} else {
					gc.drawText(chartMarker.markerLabel, xPos, yPos, true);
				}
			}
		}

		colorLine.dispose();
	}
}
