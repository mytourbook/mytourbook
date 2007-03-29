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

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.data.TourData;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ChartSegmentValueLayer implements IChartLayer {

	// private ArrayList<ChartMarker> fChartMarkers = new
	// ArrayList<ChartMarker>();

	private RGB			lineColor	= new RGB(255, 0, 0);

	private TourData	fTourData;

	private int[]		fXDataSerie;

	/**
	 * Adds a new marker to the internal marker list, the list can be retrieved
	 * with getMarkerList()
	 * 
	 * @param marker
	 * @param xCoord
	 *        Position of the marker on the x axis
	 * @param label
	 */
	// public void addMarker(ChartMarker marker) {
	// fChartMarkers.add(marker);
	// }
	public void setLineColor(final RGB lineColor) {
		this.lineColor = lineColor;
	}

	/**
	 * Draws the marker(s) for the current graph config
	 * 
	 * @param gc
	 * @param drawingData
	 * @param chartComponents
	 */
	public void draw(final GC gc, final ChartDrawingData drawingData, final Chart chart) {

		final Display display = Display.getCurrent();

		final int devYTop = drawingData.getDevYTop();
		final int devYBottom = drawingData.getDevYBottom();
		final int devGraphImageXOffset = chart.getDevGraphImageXOffset();

		final int graphYBottom = drawingData.getGraphYBottom();

		final ChartDataYSerie yData = drawingData.getYData();

		// get the segment values
		final Object segmentValuesObject = yData.getCustomData(TourChart.SEGMENT_VALUES);
		if ((segmentValuesObject instanceof float[]) == false) {
			return;
		}
		final float[] segmentValues = (float[]) segmentValuesObject;

		final int valueDivisor = yData.getValueDivisor();
		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();

		final Color colorLine = new Color(display, lineColor);
		gc.setForeground(colorLine);

		Point lastPoint = null;

		final int[] segmentSerie = fTourData.segmentSerieIndex;

		for (int segmentIndex = 0; segmentIndex < segmentSerie.length; segmentIndex++) {

			final int serieIndex = segmentSerie[segmentIndex];

			final int xDevOffset = (int) (fXDataSerie[serieIndex] * scaleX) - devGraphImageXOffset;

			final float graphYValue = segmentValues[segmentIndex] * valueDivisor;
			final int devYGraph = (int) ((float) (graphYValue - graphYBottom) * scaleY);
			int devYMarker = devYBottom - devYGraph;

			// don't draw over the graph borders
			if (devYMarker > devYBottom) {
				devYMarker = devYBottom;
			}
			if (devYMarker < devYTop) {
				devYMarker = devYTop;
			}

			if (lastPoint == null) {
				lastPoint = new Point(xDevOffset, devYMarker);
			} else {
				gc.setLineStyle(SWT.LINE_DOT);
				gc.drawLine(lastPoint.x, lastPoint.y, lastPoint.x, devYMarker);
				gc.setLineStyle(SWT.LINE_SOLID);
				gc.drawLine(lastPoint.x, devYMarker, xDevOffset, devYMarker);

				lastPoint.x = xDevOffset;
				lastPoint.y = devYMarker;
			}
		}

		colorLine.dispose();
	}

	public void setTourData(final TourData tourData) {
		fTourData = tourData;
	}

	public void setXDataSerie(final int[] dataSerie) {
		fXDataSerie = dataSerie;
	}
}
