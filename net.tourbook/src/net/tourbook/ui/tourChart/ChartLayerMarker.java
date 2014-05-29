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
import net.tourbook.data.TourMarker;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
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

	public ChartLayerMarker() {}

	private void adjustLabelPosition(	final ChartLabel chartMarker,
										final int devYTop,
										final int devYBottom,
										final int labelWidth,
										final int labelHeight) {

		final int labelHeight2 = labelHeight / 2;
		final int markerPointSize2 = MARKER_POINT_SIZE / 2;

		switch (chartMarker.visualPosition) {
		case TourMarker.LABEL_POS_VERTICAL_ABOVE_GRAPH:
			_isVertical = true;
			_devXMarker += labelHeight2;
			_devYMarker -= LABEL_OFFSET + markerPointSize2;
			break;

		case TourMarker.LABEL_POS_VERTICAL_BELOW_GRAPH:
			_isVertical = true;
			_devXMarker += labelHeight2;
			_devYMarker += labelWidth + LABEL_OFFSET + markerPointSize2;
			break;

		case TourMarker.LABEL_POS_VERTICAL_TOP_CHART:
			_isVertical = true;
			_devXMarker += labelHeight2;
			_devYMarker = devYTop + labelWidth;
			break;

		case TourMarker.LABEL_POS_VERTICAL_BOTTOM_CHART:
			_isVertical = true;
			_devXMarker += labelHeight2;
			_devYMarker = devYBottom - LABEL_OFFSET;
			break;

		case TourMarker.LABEL_POS_HORIZONTAL_ABOVE_GRAPH_LEFT:
			_isVertical = false;
			_devXMarker -= labelWidth + LABEL_OFFSET + markerPointSize2;
			_devYMarker -= labelHeight + LABEL_OFFSET + markerPointSize2;
			break;

		case TourMarker.LABEL_POS_HORIZONTAL_ABOVE_GRAPH_CENTERED:
			_isVertical = false;
			_devXMarker -= labelWidth / 2;
			_devYMarker -= labelHeight + LABEL_OFFSET + markerPointSize2;
			break;

		case TourMarker.LABEL_POS_HORIZONTAL_ABOVE_GRAPH_RIGHT:
			_isVertical = false;
			_devXMarker += LABEL_OFFSET + markerPointSize2;
			_devYMarker -= labelHeight + LABEL_OFFSET + markerPointSize2;
			break;

		case TourMarker.LABEL_POS_HORIZONTAL_BELOW_GRAPH_LEFT:
			_isVertical = false;
			_devXMarker -= labelWidth + LABEL_OFFSET + markerPointSize2;
			_devYMarker += LABEL_OFFSET + markerPointSize2;
			break;

		case TourMarker.LABEL_POS_HORIZONTAL_BELOW_GRAPH_CENTERED:
			_isVertical = false;
			_devXMarker -= labelWidth / 2;
			_devYMarker += LABEL_OFFSET + markerPointSize2;
			break;

		case TourMarker.LABEL_POS_HORIZONTAL_BELOW_GRAPH_RIGHT:
			_isVertical = false;
			_devXMarker += LABEL_OFFSET + markerPointSize2;
			_devYMarker += LABEL_OFFSET + markerPointSize2;
			break;

		case TourMarker.LABEL_POS_HORIZONTAL_GRAPH_LEFT:
			_isVertical = false;
			_devXMarker -= labelWidth + LABEL_OFFSET + markerPointSize2;
			_devYMarker -= labelHeight / 2;
			break;

		case TourMarker.LABEL_POS_HORIZONTAL_GRAPH_RIGHT:
			_isVertical = false;
			_devXMarker += LABEL_OFFSET + markerPointSize2;
			_devYMarker -= labelHeight / 2;
			break;

		default:
			break;
		}
	}

	/**
	 * Draws the marker(s) for the current graph config.
	 */
	public void draw(final GC gc, final GraphDrawingData drawingData, final Chart chart, final PixelConverter pc) {

		final Device display = gc.getDevice();

		final int markerPointSize = _chartMarkerConfig.markerPointSize;

		MARKER_POINT_SIZE = pc.convertVerticalDLUsToPixels(markerPointSize);
		MARKER_HOVER_OFFSET = pc.convertVerticalDLUsToPixels(4);
//		MARKER_HOVER_OFFSET = pc.convertVerticalDLUsToPixels(20);
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

			Color markerColor;
			if (chartMarker.visualType != ChartLabel.VISIBLE_TYPE_DEFAULT) {

				// marker is edited
				markerColor = colorSelected;

			} else {

				/*
				 * Set priority with which color a marker is painted.
				 */

				if (chartMarker.isVisible == false) {

					// marker is hidden
					markerColor = colorHidden;

				} else if (chartMarker.isDeviceMarker()) {

					// marker is created with the device
					markerColor = colorDevice;

				} else {

					// this is a default marker which is visible
					markerColor = colorDefault;
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
			final int markerPointSize2 = MARKER_POINT_SIZE / 2;

			final int devXMarker = _devXMarker - markerPointSize2 + 1;
			final int devYMarker = _devYMarker - markerPointSize2 - 0;

			chartMarker.devXMarker = devXMarker;
			chartMarker.devYMarker = devYMarker;

			if (MARKER_POINT_SIZE > 0) {

				// draw marker point
				gc.setBackground(markerColor);
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

					if (_chartMarkerConfig.isDrawLabelWithDefaultColor) {
						gc.setForeground(colorDefault);
					} else {
						gc.setForeground(markerColor);
					}
				}

				final int labelWidth = labelExtend.x;
				final int labelHeight = labelExtend.y;

				adjustLabelPosition(//
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

					final int vLabelWidth = labelHeight;
					final int vLabelHeight = labelWidth;

					// draw label to the left side of the marker
					_devXMarker -= vLabelWidth;

					// don't draw the marker to the right of the chart
					final long devXImageOffset = chart.getXXDevViewPortLeftBorder();
					if (devXImageOffset == 0 && _devXMarker < 0) {
						_devXMarker = 0;
					}

					// force label to be not below the bottom
					if (_devYMarker > devYBottom) {
						_devYMarker = devYBottom - LABEL_OFFSET;
					}

					// force label to be not above the top
					if (_devYMarker - vLabelHeight - LABEL_OFFSET < devYTop) {
						_devYMarker = devYTop + vLabelHeight + LABEL_OFFSET;
					}

					final int devXLabel = _devXMarker;
					int devYLabel = _devYMarker;

					switch (chartMarker.visualPosition) {
					case TourMarker.LABEL_POS_VERTICAL_ABOVE_GRAPH:

						devYLabel -= vLabelHeight;
						break;

					case TourMarker.LABEL_POS_VERTICAL_BELOW_GRAPH:

						devYLabel -= vLabelHeight;
						break;

					case TourMarker.LABEL_POS_VERTICAL_TOP_CHART:

						devYLabel = devYTop;
						break;

					case TourMarker.LABEL_POS_VERTICAL_BOTTOM_CHART:

						devYLabel = devYBottom - vLabelHeight;
						break;

					default:
						break;
					}

					// keep painted positions to identify and paint hovered positions
					chartMarker.devXLabel = devXLabel;
					chartMarker.devYLabel = devYLabel;
					chartMarker.devLabelWidth = vLabelWidth + markerPointSize2;
					chartMarker.devLabelHeight = vLabelHeight;

					// draw label vertical
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

					// keep painted positions to identify and paint hovered positions
					chartMarker.devXLabel = _devXMarker;
					chartMarker.devYLabel = _devYMarker;
					chartMarker.devLabelWidth = labelWidth;
					chartMarker.devLabelHeight = labelHeight;

					// draw label
					gc.drawText(//
							chartMarker.markerLabel,
							_devXMarker,
							_devYMarker,
							true);
				}

				// keep painted positions to identify and paint hovered positions
				chartMarker.isVertical = _isVertical;
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
	public void drawOverlay(final GC gc, final GraphDrawingData graphDrawingData) {

		if (_hoveredMarker == null) {
			return;
		}

		final int devYTop = graphDrawingData.getDevYTop();
		final int devGraphHeight = graphDrawingData.devGraphHeight;

		final Device device = gc.getDevice();

		gc.setClipping(0, devYTop, gc.getClipping().width, devGraphHeight);
		gc.setAlpha(0x30);

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

			final int devLabelX = _hoveredMarker.devXLabel - MARKER_HOVER_OFFSET;
			final int devLabelY = _hoveredMarker.devYLabel - MARKER_HOVER_OFFSET;
			final int devLabelWidth = _hoveredMarker.devLabelWidth + 2 * MARKER_HOVER_OFFSET;
			final int devLabelHeight = _hoveredMarker.devLabelHeight + 2 * MARKER_HOVER_OFFSET;

			final int devMarkerX = _hoveredMarker.devXMarker - MARKER_HOVER_OFFSET;
			final int devMarkerY = _hoveredMarker.devYMarker - MARKER_HOVER_OFFSET;
			final int devMarkerSize = MARKER_POINT_SIZE + 2 * MARKER_HOVER_OFFSET;

			final boolean isDrawOverlapped = true;

			if (isDrawOverlapped) {

				/*
				 * Rectangles can be merged into a union with regions, took me some time to find
				 * this solution :-)
				 */

				final Region region = new Region(device);
				{
					region.add(devLabelX, devLabelY, devLabelWidth, devLabelHeight);
					region.add(devMarkerX, devMarkerY, devMarkerSize, devMarkerSize);

					final Rectangle clientRect = gc.getClipping();

					gc.setClipping(region);
					{
						gc.fillRectangle(clientRect);
					}
					gc.setClipping((Region) null);
				}
				region.dispose();

			} else {

				// draw marker point area
				if (isPointVisible && isLabelVisible == false) {

					gc.fillRectangle(//
							devMarkerX,
							devMarkerY,
							devMarkerSize,
							devMarkerSize);
				}

				// draw marker text area
				if (isLabelVisible) {

					if (_hoveredMarker.isVertical) {

					} else {

						gc.fillRectangle(//
								devLabelX,
								devLabelY,
								devLabelWidth,
								devLabelHeight);
					}
				}
			}
		}
		colorDefault.dispose();
		colorDevice.dispose();
		colorHidden.dispose();

		gc.setAlpha(0xff);
		gc.setClipping((Rectangle) null);
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

			final int devXLabel = chartLabel.devXLabel;
			final int devYLabel = chartLabel.devYLabel;

			if (devXMouse > devXLabel - MARKER_HOVER_OFFSET
					&& devXMouse < devXLabel + chartLabel.devLabelWidth + MARKER_HOVER_OFFSET
					&& devYMouse > devYLabel - MARKER_HOVER_OFFSET
					&& devYMouse < devYLabel + chartLabel.devLabelHeight + MARKER_HOVER_OFFSET) {

				// horizontal label is hit
				return chartLabel;
			}

			// check tour marker point
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

		return null;
	}

	/**
	 * Set marker layer that nothing is hovered.
	 */
	void resetHoveredMarker() {

		_hoveredMarker = null;
	}

	public void setChartMarkerConfig(final ChartMarkerConfig chartMarkerConfig) {
		_chartMarkerConfig = chartMarkerConfig;
	}
}
