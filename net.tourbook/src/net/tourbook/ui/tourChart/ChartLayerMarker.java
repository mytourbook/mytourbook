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

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartMouseEvent;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.chart.IChartOverlay;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;

public class ChartLayerMarker implements IChartLayer, IChartOverlay {

	private int					LABEL_OFFSET;
	private int					MARKER_HOVER_OFFSET;
	private int					MARKER_POINT_SIZE;

	private ChartMarkerConfig	_chartMarkerConfig;

	private boolean				_isVertical;
	private int					_devXMarker;
	private int					_devYMarker;

	private long				_hoveredEventTime;
	private ChartLabel			_hoveredMarker;

	public ChartLayerMarker(final ChartMarkerConfig chartMarkerConfig) {

		_chartMarkerConfig = chartMarkerConfig;
	}

	/**
	 * Draws the marker(s) for the current graph config.
	 */
	public void draw(final GC gc, final GraphDrawingData drawingData, final Chart chart, final PixelConverter pc) {

		final Device display = gc.getDevice();

		MARKER_POINT_SIZE = pc.convertVerticalDLUsToPixels(_chartMarkerConfig.markerPointSize);
		MARKER_HOVER_OFFSET = pc.convertVerticalDLUsToPixels(4);
		LABEL_OFFSET = pc.convertVerticalDLUsToPixels(2);

		final int devYTop = drawingData.getDevYTop();
		final int devYBottom = drawingData.getDevYBottom();
		final long devGraphImageOffset = chart.getXXDevViewPortLeftBorder();
		final int devGraphHeight = drawingData.devGraphHeight;
		final long devGraphWidth = drawingData.devVirtualGraphWidth;

		final float graphYBottom = drawingData.getGraphYBottom();
		final float[] yValues = drawingData.getYData().getHighValuesFloat()[0];
		final double scaleX = drawingData.getScaleX();
		final double scaleY = drawingData.getScaleY();

		final Color colorSelected = display.getSystemColor(SWT.COLOR_BLUE);
		final Color colorDefault = new Color(display, _chartMarkerConfig.markerColorDefault);
		final Color colorDevice = new Color(display, _chartMarkerConfig.markerColorDevice);
		final Color colorHidden = new Color(display, _chartMarkerConfig.markerColorHidden);

		gc.setClipping(0, devYTop, gc.getClipping().width, devGraphHeight);

		for (final ChartLabel chartMarker : _chartMarkerConfig.chartLabels) {

			// check if a marker should be displayed
			if (chartMarker.isVisible == false) {

				// check if hidden markers should be displayed
				if (_chartMarkerConfig.isShowHiddenMarker == false) {
					continue;
				}
			}

			final float yValue = yValues[chartMarker.serieIndex];
			final int devYGraph = (int) ((yValue - graphYBottom) * scaleY) - 0;

			_devXMarker = (int) (chartMarker.graphX * scaleX - devGraphImageOffset);
			_devYMarker = devYBottom - devYGraph;

			final Point labelExtend = gc.textExtent(chartMarker.markerLabel);

			/*
			 * Draw marker point
			 */
			if (MARKER_POINT_SIZE > 0) {

				if (chartMarker.visualType != ChartLabel.VISIBLE_TYPE_DEFAULT) {

					gc.setBackground(colorSelected);

				} else {

					if (chartMarker.isDeviceMarker()) {
						gc.setBackground(colorDevice);
					} else if (chartMarker.isVisible) {
						gc.setBackground(colorDefault);
					} else {
						gc.setBackground(colorHidden);
					}
				}

				final int markerPointSize2 = MARKER_POINT_SIZE / 2;

				final int devXMarker = _devXMarker - markerPointSize2 + 1;
				final int devYMarker = _devYMarker - markerPointSize2 - 0;

				chartMarker.devXMarker = devXMarker;
				chartMarker.devYMarker = devYMarker;

				// draw marker point
				gc.fillRectangle(//
						devXMarker,
						devYMarker,
						MARKER_POINT_SIZE,
						MARKER_POINT_SIZE);
			}

			/*
			 * Draw marker label
			 */
			if (_chartMarkerConfig.isShowMarkerLabel) {

				if (chartMarker.visualType != ChartLabel.VISIBLE_TYPE_DEFAULT) {
					gc.setForeground(colorSelected);
				} else {
					gc.setForeground(colorDefault);
				}

				final int labelWidth = labelExtend.x;
				final int labelHeight = labelExtend.y;
				_isVertical = true;

				setLabelPosition(//
						chartMarker,
						devYTop,
						devYBottom,
						labelWidth,
						labelHeight);

				// add additional offset
				_devXMarker += chartMarker.labelXOffset;
				_devYMarker -= chartMarker.labelYOffset;

				if (_isVertical) {

					/*
					 * label is vertical
					 */

					// draw label to the left side of the marker
					_devXMarker -= labelHeight;

					// don't draw the marker to the right of the chart
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

					// don't draw the marker to the left of the chart
					final long devXImageOffset = chart.getXXDevViewPortLeftBorder();
					if (devXImageOffset == 0 && _devXMarker < 0) {
						_devXMarker = 0;
					}

					// don't draw the marker to the right of the chart
					if (_devXMarker + labelWidth > devGraphWidth) {
						_devXMarker = (int) (devGraphWidth - labelWidth);
					}

					// force label to be not below the bottom
					if (_devYMarker + labelHeight > devYBottom) {
						_devYMarker = devYBottom - labelHeight;
					}

					// force label to be not above the top
					if (_devYMarker < devYTop) {
						_devYMarker = devYTop;
					}

					gc.drawText(chartMarker.markerLabel, _devXMarker, _devYMarker, true);
				}

				// keep painted positions to identify and paint hovered positions
				chartMarker.isVertical = _isVertical;
				chartMarker.devXLabel = _devXMarker;
				chartMarker.devYLabel = _devYMarker;
				chartMarker.devLabelWidth = labelWidth;
				chartMarker.devLabelHeight = labelHeight;
			}
		}
		colorDefault.dispose();
		colorDevice.dispose();
		colorHidden.dispose();

		gc.setClipping((Rectangle) null);

	}

	/**
	 * Draw hovered marker.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public void drawOverlay(final GC gc) {

		if (_hoveredMarker == null) {
			return;
		}

		gc.setAlpha(0x30);

		final Device device = gc.getDevice();

		final Color colorDefault = new Color(device, _chartMarkerConfig.markerColorDefault);
		final Color colorDevice = new Color(device, _chartMarkerConfig.markerColorDevice);
		final Color colorHidden = new Color(device, _chartMarkerConfig.markerColorHidden);
		{
			gc.setForeground(colorDefault);

			if (_hoveredMarker.isDeviceMarker()) {
				gc.setBackground(colorDevice);
			} else if (_hoveredMarker.isVisible) {
				gc.setBackground(colorDefault);
			} else {
				gc.setBackground(colorHidden);
			}

			final boolean isLabelVisible = _chartMarkerConfig.isShowMarkerLabel;
			final boolean isPointVisible = MARKER_POINT_SIZE > 0;

			if (isPointVisible && isLabelVisible == false) {

				// draw marker point area
				gc.fillRectangle(//
						_hoveredMarker.devXMarker - MARKER_HOVER_OFFSET,
						_hoveredMarker.devYMarker - MARKER_HOVER_OFFSET,
						MARKER_POINT_SIZE + 2 * MARKER_HOVER_OFFSET,
						MARKER_POINT_SIZE + 2 * MARKER_HOVER_OFFSET);
			}

			if (isLabelVisible) {

				if (_hoveredMarker.isVertical) {

				} else {

					// draw marker text area
					gc.fillRectangle(//
							_hoveredMarker.devXLabel - MARKER_HOVER_OFFSET,
							_hoveredMarker.devYLabel - MARKER_HOVER_OFFSET,
							_hoveredMarker.devLabelWidth + 2 * MARKER_HOVER_OFFSET,
							_hoveredMarker.devLabelHeight + 2 * MARKER_HOVER_OFFSET);
				}
			}
		}
		colorDefault.dispose();
		colorDevice.dispose();
		colorHidden.dispose();

		gc.setAlpha(0xff);
	}

	public ChartLabel getHoveredMarker() {
		return _hoveredMarker;
	}

	ChartLabel getHoveredMarker(final ChartMouseEvent mouseEvent) {

		if (mouseEvent.eventTime == _hoveredEventTime) {
			return _hoveredMarker;
		}

		_hoveredEventTime = mouseEvent.eventTime;

		// marker is dirty -> retrieve again
		_hoveredMarker = getHoveredMarker_10(mouseEvent.devXMouse, mouseEvent.devYMouse);

		return _hoveredMarker;
	}

	private ChartLabel getHoveredMarker_10(final int devXMouse, final int devYMouse) {

		for (final ChartLabel chartLabel : _chartMarkerConfig.chartLabels) {

			if (chartLabel.isVertical) {

			} else {

				final int devXLabel = chartLabel.devXLabel;
				final int devYLabel = chartLabel.devYLabel;

				if (devXMouse > devXLabel - MARKER_HOVER_OFFSET
						&& devXMouse < devXLabel + chartLabel.devLabelWidth + MARKER_HOVER_OFFSET
						&& devYMouse > devYLabel - MARKER_HOVER_OFFSET
						&& devYMouse < devYLabel + chartLabel.devLabelHeight + MARKER_HOVER_OFFSET) {

					// horizontal label is hit
					return chartLabel;
				}

				final int devXMarker = chartLabel.devXMarker;
				final int devYMarker = chartLabel.devYMarker;

				if (devXMouse > devXMarker - MARKER_HOVER_OFFSET
						&& devXMouse < devXMarker + MARKER_POINT_SIZE + MARKER_HOVER_OFFSET
						&& devYMouse > devYMarker - MARKER_HOVER_OFFSET
						&& devYMouse < devYMarker + MARKER_POINT_SIZE + MARKER_HOVER_OFFSET) {

					// marker point is hit
					return chartLabel;
				}
			}
		}

		return null;
	}

	/**
	 * Set marker layer that nothing is hovered.
	 */
	void resetHoveredMarker() {

		_hoveredMarker = null;
	}

	private void setLabelPosition(	final ChartLabel chartMarker,
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
