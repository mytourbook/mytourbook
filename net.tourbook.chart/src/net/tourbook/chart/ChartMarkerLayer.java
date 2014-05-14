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
package net.tourbook.chart;

import java.util.ArrayList;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;

public class ChartMarkerLayer implements IChartLayer {

	private int							LABEL_OFFSET;
	private int							MARKER_POINT_SIZE;

	private final ArrayList<ChartLabel>	_chartLabels	= new ArrayList<ChartLabel>();

	private RGB							_lineColor		= new RGB(189, 0, 255);

	private boolean						_isVertical;
	private int							_devXMarker;
	private int							_devYMarker;

	private int							_markerPointSize;

	/**
	 * Adds a new marker to the internal marker list, the list can be retrieved with getMarkerList()
	 * 
	 * @param label
	 * @param xCoord
	 *            Position of the marker on the x axis
	 * @param label
	 */
	public void addLabel(final ChartLabel label) {
		_chartLabels.add(label);
	}

	/**
	 * Draws the marker(s) for the current graph config.
	 */
	public void draw(final GC gc, final GraphDrawingData drawingData, final Chart chart, final PixelConverter pc) {

		final Device display = gc.getDevice();

		MARKER_POINT_SIZE = pc.convertVerticalDLUsToPixels(_markerPointSize);
		LABEL_OFFSET = pc.convertVerticalDLUsToPixels(1);

		final int devYTop = drawingData.getDevYTop();
		final int devYBottom = drawingData.getDevYBottom();
		final long devGraphImageOffset = chart.getXXDevViewPortLeftBorder();
		final int devGraphHeight = drawingData.devGraphHeight;
		final long devGraphWidth = drawingData.devVirtualGraphWidth;

		final float graphYBottom = drawingData.getGraphYBottom();
		final float[] yValues = drawingData.getYData().getHighValuesFloat()[0];
		final double scaleX = drawingData.getScaleX();
		final double scaleY = drawingData.getScaleY();

		final Color colorLine = new Color(display, _lineColor);

		gc.setClipping(0, devYTop, gc.getClipping().width, devGraphHeight);

		for (final ChartLabel chartMarker : _chartLabels) {

			final float yValue = yValues[chartMarker.serieIndex];
			final int devYGraph = (int) ((yValue - graphYBottom) * scaleY) - 0;

			_devXMarker = (int) (chartMarker.graphX * scaleX - devGraphImageOffset);
			_devYMarker = devYBottom - devYGraph;

			final Point labelExtend = gc.textExtent(chartMarker.markerLabel);
			final int markerType = chartMarker.type;

			if (markerType == ChartLabel.MARKER_TYPE_DEVICE) {
				gc.setBackground(display.getSystemColor(SWT.COLOR_RED));
			} else {
				gc.setBackground(colorLine);
			}
			gc.setForeground(colorLine);

			final int markerPointSize2 = MARKER_POINT_SIZE / 2;

			// draw marker point
			gc.fillRectangle(//
					_devXMarker - markerPointSize2 + 1,
					_devYMarker - markerPointSize2 - 0,
					MARKER_POINT_SIZE,
					MARKER_POINT_SIZE);

			if (chartMarker.visualType != ChartLabel.VISIBLE_TYPE_DEFAULT) {
				gc.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
			}

			_isVertical = true;

			final int labelWidth = labelExtend.x;
			final int labelHeight = labelExtend.y;

			setMarkerPosition(//
					chartMarker,
					devYTop,
					devYBottom,
					labelWidth,
					labelHeight);

			// add additional offset
			_devXMarker += chartMarker.labelXOffset;
			_devYMarker -= chartMarker.labelYOffset;

			// draw marker label
			if (chart.getAdvancedGraphics()) {

				if (_isVertical) {

					/*
					 * label is vertical
					 */

					// draw label to the left side of the marker
					_devXMarker -= labelHeight;

					// don't draw the marker before the chart
					final long devXImageOffset = chart.getXXDevViewPortLeftBorder();
					if (devXImageOffset == 0 && _devXMarker < 0) {
						_devXMarker = 0;
					}

					// force label to be not below the bottom
					if (_devYMarker > devYBottom) {
						_devYMarker = devYBottom;
					}

					// force label to be not above the top
					if (_devYMarker - labelWidth < devYTop) {
						_devYMarker = devYTop + labelWidth;
					}

					final Transform tr = new Transform(display);
					{
						tr.translate(_devXMarker, _devYMarker);
						tr.rotate(-90f);

						gc.setTransform(tr);

						gc.setAntialias(SWT.ON);
						gc.drawText(chartMarker.markerLabel, 0, 0, true);
						gc.setAntialias(SWT.OFF);

						gc.setTransform(null);
					}
					tr.dispose();

				} else {

					/*
					 * label is horizontal
					 */

					// don't draw the marker left of the chart
					final long devXImageOffset = chart.getXXDevViewPortLeftBorder();
					if (devXImageOffset == 0 && _devXMarker < 0) {
						_devXMarker = 0;
					}

					// don't draw the marker right of the chart
					if (_devXMarker + labelWidth > devGraphWidth) {
						_devXMarker = (int) (devGraphWidth - labelWidth);
					}

					// force label to be not below the bottom
					if (_devYMarker + labelHeight > devYBottom) {
						_devYMarker = devYBottom - labelHeight;
					}

					// force label to be not above the top
					if (_devYMarker /*- labelHeight*/< devYTop) {
						_devYMarker = devYTop;// + labelHeight;
					}

					gc.drawText(chartMarker.markerLabel, _devXMarker, _devYMarker, true);
				}

			} else {

				// use simple graphics, this is propably never used !!!

				_devYMarker = devYBottom - devYGraph - labelHeight;
				if (_devXMarker + labelWidth > devGraphWidth) {
					_devXMarker -= labelWidth;
				}
				gc.drawText(chartMarker.markerLabel, _devXMarker, _devYMarker, true);
			}
		}

		gc.setClipping((Rectangle) null);

		colorLine.dispose();
	}

	public void setLineColor(final RGB lineColor) {
		_lineColor = lineColor;
	}

	public void setMarkerPointSize(final int markerPointSize) {
		_markerPointSize = markerPointSize;
	}

	private void setMarkerPosition(	final ChartLabel chartMarker,
									final int devYTop,
									final int devYBottom,
									final int labelWidth,
									final int labelHeight) {

		final int labelHeight2 = labelHeight / 2;
		final int markerPointSize2 = MARKER_POINT_SIZE / 2;

		switch (chartMarker.visualPosition) {
		case ChartLabel.VISUAL_VERTICAL_ABOVE_GRAPH:
			_devXMarker += labelHeight2;
			_devYMarker -= LABEL_OFFSET + markerPointSize2;
			break;

		case ChartLabel.VISUAL_VERTICAL_BELOW_GRAPH:
			_devXMarker += labelHeight2;
			_devYMarker += labelWidth + LABEL_OFFSET + markerPointSize2;
			break;

		case ChartLabel.VISUAL_VERTICAL_TOP_CHART:
			_devXMarker += labelHeight2;
			_devYMarker = devYTop + labelWidth;
			break;

		case ChartLabel.VISUAL_VERTICAL_BOTTOM_CHART:
			_devXMarker += labelHeight2;
			_devYMarker = devYBottom - LABEL_OFFSET;
			break;

		case ChartLabel.VISUAL_HORIZONTAL_ABOVE_GRAPH_LEFT:
			_devXMarker -= labelWidth + LABEL_OFFSET + markerPointSize2;
			_devYMarker -= labelHeight + LABEL_OFFSET + markerPointSize2;
			_isVertical = false;
			break;

		case ChartLabel.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED:
			_devXMarker -= labelWidth / 2;
			_devYMarker -= labelHeight + LABEL_OFFSET + markerPointSize2;
			_isVertical = false;
			break;

		case ChartLabel.VISUAL_HORIZONTAL_ABOVE_GRAPH_RIGHT:
			_devXMarker += LABEL_OFFSET + markerPointSize2;
			_devYMarker -= labelHeight + LABEL_OFFSET + markerPointSize2;
			_isVertical = false;
			break;

		case ChartLabel.VISUAL_HORIZONTAL_BELOW_GRAPH_LEFT:
			_devXMarker -= labelWidth + LABEL_OFFSET + markerPointSize2;
			_devYMarker += LABEL_OFFSET + markerPointSize2;
			_isVertical = false;
			break;

		case ChartLabel.VISUAL_HORIZONTAL_BELOW_GRAPH_CENTERED:
			_devXMarker -= labelWidth / 2;
			_devYMarker += LABEL_OFFSET + markerPointSize2;
			_isVertical = false;
			break;

		case ChartLabel.VISUAL_HORIZONTAL_BELOW_GRAPH_RIGHT:
			_devXMarker += LABEL_OFFSET + markerPointSize2;
			_devYMarker += LABEL_OFFSET + markerPointSize2;
			_isVertical = false;
			break;

		case ChartLabel.VISUAL_HORIZONTAL_GRAPH_LEFT:
			_devXMarker -= labelWidth + LABEL_OFFSET + markerPointSize2;
			_devYMarker -= labelHeight / 2;
			_isVertical = false;
			break;

		case ChartLabel.VISUAL_HORIZONTAL_GRAPH_RIGHT:
			_devXMarker += LABEL_OFFSET + markerPointSize2;
			_devYMarker -= labelHeight / 2;
			_isVertical = false;
			break;

		default:
			break;
		}
	}
}
