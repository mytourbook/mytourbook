/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ChartSegmentLayer implements IChartLayer {

	private ArrayList<ChartMarker>	_chartMarkers	= new ArrayList<ChartMarker>();

	private RGB						_lineColor		= new RGB(189, 0, 255);

	/**
	 * Adds a new marker to the internal marker list, the list can be retrieved with getMarkerList()
	 * 
	 * @param chartMarker
	 * @param xCoord
	 *            Position of the marker on the x axis
	 * @param label
	 */
	public void addMarker(final ChartMarker chartMarker) {
		_chartMarkers.add(chartMarker);
	}

	/**
	 * Draws the marker(s) for the current graph config
	 * 
	 * @param gc
	 * @param drawingData
	 * @param fChartComponents
	 */
	public void draw(	final GC gc,
						final GraphDrawingData drawingData,
						final Chart chart,
						final PixelConverter pixelConverter) {

		final Display display = Display.getCurrent();

		final int devYTop = drawingData.getDevYTop();
		final int devYBottom = drawingData.getDevYBottom();
		final long devGraphImageOffset = chart.getXXDevViewPortLeftBorder();
		final int devGraphHeight = drawingData.devGraphHeight;

		final float graphYBottom = drawingData.getGraphYBottom();
		final float[] yValues = drawingData.getYData().getHighValuesFloat()[0];
		final double scaleX = drawingData.getScaleX();
		final double scaleY = drawingData.getScaleY();

		final Color colorLine = new Color(display, getLineColor());
		Point lastPoint = null;

		for (final ChartMarker chartMarker : _chartMarkers) {

			final int devXOffset = (int) (chartMarker.graphX * scaleX - devGraphImageOffset);

			final int yValueIndex = Math.min(yValues.length - 1, chartMarker.serieIndex);
			final float yValue = yValues[yValueIndex];

			final int devYGraph = (int) ((yValue - graphYBottom) * scaleY);
			int devYSegment = devYBottom - devYGraph;

			// don't draw over the graph borders
			if (devYSegment > devYBottom) {
				devYSegment = devYBottom;
			}
			if (devYSegment < devYTop) {
				devYSegment = devYTop;
			}

			gc.setForeground(colorLine);

			// connect two segments with a line
			if (lastPoint == null) {
				lastPoint = new Point(devXOffset, devYSegment);
			} else {
				gc.setLineStyle(SWT.LINE_SOLID);
				gc.drawLine(lastPoint.x, lastPoint.y, devXOffset, devYSegment);

				lastPoint.x = devXOffset;
				lastPoint.y = devYSegment;
			}

			// draw a line from the marker to the top of the graph
			gc.setForeground(display.getSystemColor(SWT.COLOR_GRAY));
			gc.setLineStyle(SWT.LINE_DOT);
			gc.drawLine(devXOffset, devYSegment, devXOffset, devYBottom - devGraphHeight);
		}

		colorLine.dispose();
	}

	public RGB getLineColor() {
		return _lineColor;
	}

	public void setLineColor(final RGB lineColor) {
		this._lineColor = lineColor;
	}
}
