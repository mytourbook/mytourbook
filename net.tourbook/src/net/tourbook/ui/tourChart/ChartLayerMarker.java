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

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartMouseEvent;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.chart.IChartOverlay;
import net.tourbook.data.TourMarker;
import net.tourbook.photo.ILoadCallBack;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;

public class ChartLayerMarker implements IChartLayer, IChartOverlay {

	private int					LABEL_OFFSET;
	private int					MARKER_HOVER_SIZE;
	private int					MARKER_POINT_SIZE;
//	private int					SIGN_IMAGE_MAX_SIZE;

	private TourChart			_tourChart;
	private ChartMarkerConfig	_cmc;

	private boolean				_isVertical;

	private int					_devXMarker;
	private int					_devYMarker;
	private long				_hoveredEventTime;

	private ChartLabel			_hoveredLabel;
	private ChartLabel			_tooltipLabel;

	public class LoadImageCallback implements ILoadCallBack {

		@Override
		public void callBackImageIsLoaded(final boolean isImageLoaded) {

			if (isImageLoaded == false) {
				return;
			}

			// run in UI thread
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {

					// ensure chart is still displayed
					if (_tourChart.getShell().isDisposed()) {
						return;
					}

					// paint image
					_tourChart.redrawLayer();
				}
			});
		}
	}

	public ChartLayerMarker(final TourChart tourChart) {

		_tourChart = tourChart;
	}

	private void adjustLabelPosition(	final ChartLabel chartLabel,
										final int devYTop,
										final int devYBottom,
										final int labelWidth,
										final int labelHeight) {

		final int labelHeight2 = labelHeight / 2;
		final int markerPointSize2 = MARKER_POINT_SIZE / 2 + 0;

		final int visualPosition = _cmc.isShowLabelTempPos ? //
				_cmc.markerLabelTempPos
				: chartLabel.visualPosition;

		switch (visualPosition) {
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
			_devXMarker += LABEL_OFFSET + markerPointSize2 + 0;
			_devYMarker -= labelHeight / 2;
			break;

		default:
			break;
		}
	}

	/**
	 * This paints the marker(s) for the current graph config.
	 */
	@Override
	public void draw(final GC gc, final GraphDrawingData drawingData, final Chart chart, final PixelConverter pc) {

		final Device display = gc.getDevice();

		final int markerPointSize = _cmc.markerPointSize;
//		final int markerSignImageSize = _cmc.markerSignImageSize;
		final int labelOffset = _cmc.markerLabelOffset;
		final int hoverSize = _cmc.markerHoverSize;

		MARKER_POINT_SIZE = pc.convertVerticalDLUsToPixels(markerPointSize);
		MARKER_HOVER_SIZE = pc.convertVerticalDLUsToPixels(hoverSize);
		LABEL_OFFSET = pc.convertVerticalDLUsToPixels(labelOffset);
//		SIGN_IMAGE_MAX_SIZE = pc.convertVerticalDLUsToPixels(markerSignImageSize);

		/*
		 * Set marker point size even that the label positioning has the correct distance otherwise
		 * the right alignment looks ugly when the size is not even.
		 */
		if (MARKER_POINT_SIZE % 2 == 1) {
			MARKER_POINT_SIZE++;
		}
		final int markerPointSize2 = MARKER_POINT_SIZE / 2;

		final int devYTop = drawingData.getDevYTop();
		final int devYBottom = drawingData.getDevYBottom();
		final long devVirtualGraphImageOffset = chart.getXXDevViewPortLeftBorder();
		final int devGraphHeight = drawingData.devGraphHeight;
		final long devVirtualGraphWidth = drawingData.devVirtualGraphWidth;
//		final int devVisibleChartWidth = drawingData.getChartDrawingData().devVisibleChartWidth;
//		final boolean isZoomed = devVirtualGraphWidth != devVisibleChartWidth;

		final float graphYBottom = drawingData.getGraphYBottom();
		final float[] yValues = drawingData.getYData().getHighValuesFloat()[0];
		final double scaleX = drawingData.getScaleX();
		final double scaleY = drawingData.getScaleY();

		final Color editColorFg = display.getSystemColor(SWT.COLOR_WHITE);
		final Color editColorBg = display.getSystemColor(SWT.COLOR_RED);
		final Color colorDefault = new Color(display, _cmc.markerColorDefault);
		final Color colorDevice = new Color(display, _cmc.markerColorDevice);
		final Color colorHidden = new Color(display, _cmc.markerColorHidden);

		gc.setClipping(0, devYTop, gc.getClipping().width, devGraphHeight);

		/*
		 * Draw marker point and label
		 */
		for (final ChartLabel chartLabel : _cmc.chartLabels) {

			// check if a marker should be displayed
			if (chartLabel.isVisible == false) {

				// check if hidden markers should be displayed
				if (_cmc.isShowHiddenMarker == false) {
					continue;
				}
			}

			final boolean isEditColor = chartLabel.visualType != ChartLabel.VISIBLE_TYPE_DEFAULT;
			final boolean isTextBgTransparent = isEditColor == false;
			Color markerColor;

			if (isEditColor) {

				// marker is edited
				markerColor = editColorBg;

			} else {

				/*
				 * Set priority with which color a marker is painted.
				 */

				if (chartLabel.isVisible == false) {

					// marker is hidden
					markerColor = colorHidden;

				} else if (chartLabel.isDeviceMarker()) {

					// marker is created with the device
					markerColor = colorDevice;

				} else {

					// this is a default marker which is visible
					markerColor = colorDefault;
				}
			}

			final float yValue = yValues[chartLabel.serieIndex];
			final int devYGraph = (int) ((yValue - graphYBottom) * scaleY) - 0;

			final double virtualXPos = chartLabel.graphX * scaleX;
			_devXMarker = (int) (virtualXPos - devVirtualGraphImageOffset);
			_devYMarker = devYBottom - devYGraph;

			final Point labelExtend = gc.textExtent(chartLabel.markerLabel);

			/*
			 * Get marker point top/left position
			 */
			final int devXMarkerTopLeft = _devXMarker - markerPointSize2;
			final int devYMarkerTopLeft = _devYMarker - markerPointSize2;

			chartLabel.devXMarker = devXMarkerTopLeft;
			chartLabel.devYMarker = devYMarkerTopLeft;

			/*
			 * Draw marker point
			 */
			if (_cmc.isShowMarkerPoint && MARKER_POINT_SIZE > 0) {

				if (_cmc.isDrawMarkerWithDefaultColor) {
					gc.setBackground(colorDefault);
				} else {
					gc.setBackground(markerColor);
				}

				// draw marker point
				gc.fillRectangle(//
						devXMarkerTopLeft,
						devYMarkerTopLeft,
						MARKER_POINT_SIZE,
						MARKER_POINT_SIZE);
			}

			/*
			 * Draw marker label
			 */
			if (_cmc.isShowMarkerLabel) {

				if (isEditColor) {
					gc.setForeground(editColorFg);
					gc.setBackground(editColorBg);
				} else {

					if (_cmc.isDrawMarkerWithDefaultColor) {
						gc.setForeground(colorDefault);
					} else {
						gc.setForeground(markerColor);
					}
				}

				final int labelWidth = labelExtend.x;
				final int labelHeight = labelExtend.y;

				adjustLabelPosition(//
						chartLabel,
						devYTop,
						devYBottom,
						labelWidth,
						labelHeight);

				// add additional offset
				_devXMarker += chartLabel.labelXOffset;
				_devYMarker -= chartLabel.labelYOffset;

				if (_isVertical) {

					/*
					 * label is vertical
					 */

					final int vLabelWidth = labelHeight;
					final int vLabelHeight = labelWidth;

					// draw label to the left side of the marker
					_devXMarker -= vLabelWidth;

					// don't draw the marker to the right of the chart
					if (devVirtualGraphImageOffset == 0 && _devXMarker < 0) {
						_devXMarker = 0;
					}

					// don't draw the marker to the right of the chart
					final double devVirtualMarkerRightPos = virtualXPos + vLabelWidth;
					if (devVirtualMarkerRightPos > devVirtualGraphWidth) {
						_devXMarker = (int) (devVirtualGraphWidth - vLabelWidth - devVirtualGraphImageOffset);
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

					final int visualPosition = _cmc.isShowLabelTempPos ? //
							_cmc.markerLabelTempPos
							: chartLabel.visualPosition;

					switch (visualPosition) {
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

					// keep painted positions to identify and paint the hovered positions
					chartLabel.paintedLabel = new Rectangle(devXLabel, devYLabel, vLabelWidth, vLabelHeight);

					// draw label vertical
					final Transform tr = new Transform(display);
					{
						tr.translate(_devXMarker, _devYMarker);
						tr.rotate(-90f);

						gc.setTransform(tr);

						gc.setAntialias(SWT.ON);
						gc.drawText(chartLabel.markerLabel, 0, 0, isTextBgTransparent);
						gc.setAntialias(SWT.OFF);

						gc.setTransform(null);
					}
					tr.dispose();

				} else {

					/*
					 * label is horizontal
					 */

					// don't draw the marker to the left of the chart
					if (devVirtualGraphImageOffset == 0 && _devXMarker < 0) {
						_devXMarker = 0;
					}

					// don't draw the marker to the right of the chart
					final double devVirtualMarkerRightPos = virtualXPos + labelWidth;
					if (devVirtualMarkerRightPos > devVirtualGraphWidth) {
						_devXMarker = (int) (devVirtualGraphWidth - labelWidth - devVirtualGraphImageOffset - 2);
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
					chartLabel.paintedLabel = new Rectangle(_devXMarker, _devYMarker, labelWidth, labelHeight);

					// draw label
					gc.drawText(//
							chartLabel.markerLabel,
							_devXMarker,
							_devYMarker,
							isTextBgTransparent);
				}

			}

			// keep painted positions to identify and paint hovered positions
			chartLabel.devIsVertical = _isVertical;
			chartLabel.devMarkerPointSize = MARKER_POINT_SIZE;
			chartLabel.devHoverSize = MARKER_HOVER_SIZE;
			chartLabel.devYBottom = devYBottom;
			chartLabel.devYTop = devYTop;
			chartLabel.devGraphWidth = drawingData.getChartDrawingData().devVisibleChartWidth;
		}

		/*
		 * Draw marker image
		 */
//		for (final ChartLabel chartLabel : _cmc.chartLabels) {
//
//			// check if a marker should be displayed
//			if (chartLabel.isVisible == false) {
//
//				// check if hidden markers should be displayed
//				if (_cmc.isShowHiddenMarker == false) {
//					continue;
//				}
//			}
//
//			if (_cmc.isShowSignImage) {
//
//				final Photo signPhoto = chartLabel.markerSignPhoto;
//				if (signPhoto != null) {
//
//					// draw the sign image
//
//					final ILoadCallBack imageLoadCallback = new LoadImageCallback();
//					final Image signImage = SignManager.getSignImage(signPhoto, imageLoadCallback);
//
//					if (signImage != null && signImage.isDisposed() == false) {
//
//						// position photo on top, above the tour marker point and centered
//						final int photoPosX = chartLabel.devXMarker - SIGN_IMAGE_MAX_SIZE / 2 + MARKER_POINT_SIZE / 2;
//						final int photoPosY = devYTop;
//
//						final Rectangle noHideArea = chartLabel.paintedLabel;
//
//						final Rectangle rectPainted = PhotoUI.paintPhotoImage(
//								gc,
//								signPhoto,
//								signImage,
//								photoPosX,
//								photoPosY,
//								SIGN_IMAGE_MAX_SIZE,
//								SIGN_IMAGE_MAX_SIZE,
//								SWT.TOP,
//								noHideArea);
//
//						chartLabel.devMarkerSignImageBounds = rectPainted;
//					}
//				}
//			}
//
//			// keep painted positions to identify and paint hovered positions
//			chartLabel.devIsVertical = _isVertical;
//			chartLabel.devMarkerPointSize = MARKER_POINT_SIZE;
//			chartLabel.devHoverSize = MARKER_HOVER_SIZE;
//		}
		colorDefault.dispose();
		colorDevice.dispose();
		colorHidden.dispose();

		gc.setClipping((Rectangle) null);
	}

	/**
	 * This paints the hovered marker.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public void drawOverlay(final GC gc, final GraphDrawingData graphDrawingData) {

		final TourMarker selectedTourMarker = _tourChart.getSelectedTourMarker();

		ChartLabel selectedLabel = null;
		if (selectedTourMarker != null) {
			selectedLabel = getChartLabel(selectedTourMarker);
		}

		final boolean isHovered = _hoveredLabel != null || _tooltipLabel != null;
		final boolean isSelected = selectedTourMarker != null;

		if (isHovered == false && isSelected == false) {
			return;
		}

		ChartLabel hoveredLabel = _hoveredLabel;

		if (hoveredLabel == null) {
			hoveredLabel = _tooltipLabel;
		}

		final int devYTop = graphDrawingData.getDevYTop();
		final int devGraphHeight = graphDrawingData.devGraphHeight;

		final Device device = gc.getDevice();

		gc.setClipping(0, devYTop, gc.getClipping().width, devGraphHeight);

		final Color colorDefault = new Color(device, _cmc.markerColorDefault);
		final Color colorDevice = new Color(device, _cmc.markerColorDevice);
		final Color colorHidden = new Color(device, _cmc.markerColorHidden);
		{
			if (isHovered && isSelected && hoveredLabel == selectedLabel) {

				// same label is hovered and selected

				drawOverlay_Label(hoveredLabel, gc, colorDefault, colorDevice, colorHidden, true);

			} else if (isHovered && isSelected) {

				// one label is hovered another label is selected

				drawOverlay_Label(hoveredLabel, gc, colorDefault, colorDevice, colorHidden, false);
				drawOverlay_Label(selectedLabel, gc, colorDefault, colorDevice, colorHidden, true);

			} else if (isHovered) {

				// the label is hovered

				drawOverlay_Label(hoveredLabel, gc, colorDefault, colorDevice, colorHidden, false);

			} else if (isSelected) {

				// a marker is selected

				drawOverlay_Label(selectedLabel, gc, colorDefault, colorDevice, colorHidden, true);
			}

		}
		colorDefault.dispose();
		colorDevice.dispose();
		colorHidden.dispose();

		gc.setAlpha(0xff);
		gc.setClipping((Rectangle) null);
	}

	private void drawOverlay_Label(	final ChartLabel chartLabel,
									final GC gc,
									final Color colorDefault,
									final Color colorDevice,
									final Color colorHidden,
									final boolean isSelected) {

		if (chartLabel == null) {
			return;
		}

		if (isSelected) {
			gc.setAlpha(0x60);
		} else {
			gc.setAlpha(0x30);
		}

		if (isSelected) {

			final Color selectedColorBg = gc.getDevice().getSystemColor(SWT.COLOR_DARK_GRAY);
			gc.setBackground(selectedColorBg);

		} else if (chartLabel.isDeviceMarker()) {
			gc.setBackground(colorDevice);
		} else if (chartLabel.isVisible) {
			gc.setBackground(colorDefault);
		} else {
			gc.setBackground(colorHidden);
		}

		/*
		 * Rectangles can be merged into a union with regions, took me some time to find this
		 * solution :-)
		 */
		final Region region = new Region(gc.getDevice());

		final Rectangle paintedLabel = chartLabel.paintedLabel;
		if (paintedLabel != null) {

			final int devLabelX = paintedLabel.x - MARKER_HOVER_SIZE;
			final int devLabelY = paintedLabel.y - MARKER_HOVER_SIZE;
			final int devLabelWidth = paintedLabel.width + 2 * MARKER_HOVER_SIZE;
			final int devLabelHeight = paintedLabel.height + 2 * MARKER_HOVER_SIZE;

			region.add(devLabelX, devLabelY, devLabelWidth, devLabelHeight);
		}

		final int devMarkerX = chartLabel.devXMarker - MARKER_HOVER_SIZE;
		final int devMarkerY = chartLabel.devYMarker - MARKER_HOVER_SIZE;
		final int devMarkerSize = MARKER_POINT_SIZE + 2 * MARKER_HOVER_SIZE;

		region.add(devMarkerX, devMarkerY, devMarkerSize, devMarkerSize);

		// get whole chart rect
		final Rectangle clientRect = gc.getClipping();

		gc.setClipping(region);
		{
			gc.fillRectangle(clientRect);
		}
		region.dispose();
		gc.setClipping((Region) null);
	}

	private ChartLabel getChartLabel(final TourMarker tourMarker) {

		for (final ChartLabel chartLabel : _cmc.chartLabels) {

			final Object chartLabelData = chartLabel.data;

			if (chartLabelData instanceof TourMarker) {
				final TourMarker chartTourMarker = (TourMarker) chartLabelData;

				if (chartTourMarker == tourMarker) {

					// marker is found

					return chartLabel;
				}
			}
		}

		return null;
	}

	public ChartLabel getHoveredLabel() {
		return _hoveredLabel;
	}

	/**
	 * Set state in marker layer that nothing is hovered.
	 */
	void resetHoveredState() {

		_hoveredLabel = null;
		_tooltipLabel = null;
	}

	/**
	 * @param mouseEvent
	 * @return Returns the hovered {@link ChartLabel} or <code>null</code> when a {@link ChartLabel}
	 *         is not hovered.
	 */
	ChartLabel retrieveHoveredLabel(final ChartMouseEvent mouseEvent) {

		if (mouseEvent.eventTime == _hoveredEventTime) {
			return _hoveredLabel;
		}

		_hoveredEventTime = mouseEvent.eventTime;

		// marker is dirty -> retrieve again
		_hoveredLabel = retrieveHoveredLabel_10(mouseEvent.devXMouse, mouseEvent.devYMouse);

		return _hoveredLabel;
	}

	private ChartLabel retrieveHoveredLabel_10(final int devXMouse, final int devYMouse) {

		/*
		 * Check sign images first, they have a higher priority
		 */
		for (final ChartLabel chartLabel : _cmc.chartLabels) {

			final Rectangle imageBounds = chartLabel.devMarkerSignImageBounds;
			if (imageBounds != null) {

				final int devXImage = imageBounds.x;
				final int devYImage = imageBounds.y;
				final int imageWidth = imageBounds.width;
				final int imageHeight = imageBounds.height;

				if (devXMouse > devXImage
						&& devXMouse < devXImage + imageWidth
						&& devYMouse > devYImage
						&& devYMouse < devYImage + imageHeight) {

					// marker sign image is hit
					return chartLabel;
				}
			}
		}

		for (final ChartLabel chartLabel : _cmc.chartLabels) {

			/*
			 * Check sign label
			 */
			final Rectangle paintedLabel = chartLabel.paintedLabel;
			if (paintedLabel != null) {

				final int devXLabel = paintedLabel.x;
				final int devYLabel = paintedLabel.y;

				if (devXMouse > devXLabel - MARKER_HOVER_SIZE
						&& devXMouse < devXLabel + paintedLabel.width + MARKER_HOVER_SIZE
						&& devYMouse > devYLabel - MARKER_HOVER_SIZE
						&& devYMouse < devYLabel + paintedLabel.height + MARKER_HOVER_SIZE) {

					// horizontal label is hit
					return chartLabel;
				}
			}

			/*
			 * Check marker point
			 */
			final int devXMarker = chartLabel.devXMarker;
			final int devYMarker = chartLabel.devYMarker;

			if (devXMouse > devXMarker - MARKER_HOVER_SIZE
					&& devXMouse < devXMarker + MARKER_POINT_SIZE + MARKER_HOVER_SIZE
					&& devYMouse > devYMarker - MARKER_HOVER_SIZE
					&& devYMouse < devYMarker + MARKER_POINT_SIZE + MARKER_HOVER_SIZE) {

				// marker point is hit
				return chartLabel;
			}
		}

		return null;
	}

	public void setChartMarkerConfig(final ChartMarkerConfig chartMarkerConfig) {

		_cmc = chartMarkerConfig;
	}

	public void setTooltipLabel(final ChartLabel tooltipLabel) {

		if (tooltipLabel == _tooltipLabel) {
			return;
		}

		_tooltipLabel = tooltipLabel;

		_tourChart.setChartOverlayDirty();
	}

}
