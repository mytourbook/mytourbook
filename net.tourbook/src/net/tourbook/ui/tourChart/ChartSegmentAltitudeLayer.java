/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.chart.Chart;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.data.TourData;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * This layer displays the altitude values.
 */
public class ChartSegmentAltitudeLayer implements IChartLayer {

	private static final Color		SYSTEM_COLOR_0		= Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
	private static final Color		SYSTEM_COLOR_DOWN	= Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
	private static final Color		SYSTEM_COLOR_UP		= Display.getCurrent().getSystemColor(SWT.COLOR_RED);

	private TourData				_tourData;
	private ArrayList<ChartMarker>	_chartMarkers		= new ArrayList<ChartMarker>();

	private RGB						_lineColor			= new RGB(189, 0, 255);
	private boolean					_isShowSegmenterValues;

	private final NumberFormat		_nf1				= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

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
	@Override
	public void draw(	final GC gc,
						final GraphDrawingData drawingData,
						final Chart chart,
						final PixelConverter pixelConverter) {

		final Display display = Display.getCurrent();

		final int devYTop = drawingData.getDevYTop();
		final int devYBottom = drawingData.getDevYBottom();
		final long devGraphImageOffset = chart.getXXDevViewPortLeftBorder();
//		final int devGraphHeight = drawingData.devGraphHeight;

		final float graphYBottom = drawingData.getGraphYBottom();
		final float[] yValues = drawingData.getYData().getHighValuesFloat()[0];
		final double scaleX = drawingData.getScaleX();
		final double scaleY = drawingData.getScaleY();

		Rectangle prevUpTextRect = null;
		Rectangle prevDownTextRect = null;

		final LineAttributes defaultLineAttributes = gc.getLineAttributes();
		final LineAttributes vertLineLA = new LineAttributes(5);
		vertLineLA.dashOffset = 3;
		vertLineLA.style = SWT.LINE_CUSTOM;
		vertLineLA.dash = new float[] { 1f, 2f };
		vertLineLA.width = 1f;

		final Color colorLine = new Color(display, _lineColor);
		{
			Point previousPoint = null;

			final float[] segmentSerieAltitudeDiff = _tourData.segmentSerieAltitudeDiff;
			final float[] segmentSerieComputedAltitudeDiff = _tourData.segmentSerieComputedAltitudeDiff;

			for (int segmentIndex = 0; segmentIndex < _chartMarkers.size(); segmentIndex++) {

				final ChartMarker chartMarker = _chartMarkers.get(segmentIndex);
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

				float altiDiff = 0;
				if (segmentIndex > 0) {
					if (segmentSerieComputedAltitudeDiff != null) {
						altiDiff = segmentSerieComputedAltitudeDiff[segmentIndex];
					} else {
						altiDiff = segmentSerieAltitudeDiff[segmentIndex];
					}
				}
				final boolean isValueUp = altiDiff >= 0;

				final String valueText = _nf1.format(altiDiff);
				final Point textExtent = gc.textExtent(valueText);

				final int textWidth = textExtent.x;
				final int textHeight = textExtent.y;

				final Color altiDiffColor = getColor(altiDiff);

				/*
				 * Connect two segments with a line
				 */
				if (previousPoint == null) {

					previousPoint = new Point(devXOffset, devYSegment);

				} else {

					gc.setLineAttributes(defaultLineAttributes);
					gc.setForeground(altiDiffColor);
					gc.drawLine(//
							previousPoint.x,
							previousPoint.y,
							devXOffset,
							devYSegment);
				}

				/*
				 * Draw a line from the value marker to the top or the bottom
				 */
				int devYLine;
				if (isValueUp) {

					devYLine = devYSegment - 1 * textHeight;

					// don't draw over the graph borders
					if (devYLine < devYTop) {
						devYLine = devYTop;
					}

				} else {

					devYLine = devYSegment + 1 * textHeight;

					// don't draw over the graph borders
					if (devYLine > devYBottom) {
						devYLine = devYBottom - textHeight;
					}
				}

				gc.setForeground(altiDiffColor);
				gc.setLineAttributes(vertLineLA);
				gc.drawLine(//
						devXOffset,
						devYSegment,
						devXOffset,
						devYLine);

				if (segmentIndex > 0) {

					if (_isShowSegmenterValues) {

						/*
						 * Draw the diff value
						 */

						final int segmentWidth = devXOffset - previousPoint.x;
						final int devXValue = previousPoint.x + segmentWidth / 2 - textWidth / 2;

						int devYValue;

						final int yDiff = (devYSegment - previousPoint.y) / 2;

						if (isValueUp) {
							devYValue = devYSegment - yDiff - 2 * textHeight;

						} else {

							devYValue = devYSegment - yDiff + 2 * textHeight;
						}

						/*
						 * Ensure the value text do not overlap, if possible :-)
						 */
						Rectangle textRect = new Rectangle(devXValue, devYValue, textWidth, textHeight);

						if (isValueUp) {
							if (prevUpTextRect != null && prevUpTextRect.intersects(textRect)) {
								devYValue = prevUpTextRect.y - textHeight;
							}
						} else {
							if (prevDownTextRect != null && prevDownTextRect.intersects(textRect)) {
								devYValue = prevDownTextRect.y + textHeight;
							}
						}

						// don't draw over the graph borders
						if (devYValue < devYTop) {
							devYValue = devYTop;
						}
						if (devYValue + textHeight > devYBottom) {
							devYValue = devYBottom - textHeight;
						}

						// keep current up/down rectangle
						final int margin = 1;
						textRect = new Rectangle(//
								devXValue - margin,
								devYValue - margin,
								textWidth + 2 * margin,
								textHeight + 2 * margin);

						if (isValueUp) {
							prevUpTextRect = textRect;
						} else {
							prevDownTextRect = textRect;
						}

						gc.setForeground(altiDiffColor);
						gc.drawText(//
								valueText,
								devXValue,
								devYValue,
								true);
					}

					previousPoint.x = devXOffset;
					previousPoint.y = devYSegment;
				}
			}
		}
		colorLine.dispose();
	}

	private Color getColor(final float altiDiff) {

		if (altiDiff > 0) {
			return SYSTEM_COLOR_UP;
		} else if (altiDiff < 0) {
			return SYSTEM_COLOR_DOWN;
		} else {
			return SYSTEM_COLOR_0;
		}
	}

	void setIsShowSegmenterValues(final boolean isShowSegmenterValues) {
		_isShowSegmenterValues = isShowSegmenterValues;
	}

	void setupLayerData(final TourData tourData, final RGB lineColor) {

		_tourData = tourData;
		_lineColor = lineColor;
	}
}
