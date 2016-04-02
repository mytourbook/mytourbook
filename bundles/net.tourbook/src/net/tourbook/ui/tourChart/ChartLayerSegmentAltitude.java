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
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartMouseEvent;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.chart.IChartOverlay;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.graphics.Line2D;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * This layer displays the altitude values.
 */
public class ChartLayerSegmentAltitude implements IChartLayer, IChartOverlay {

	private TourChart					_tourChart;
	private TourData					_tourData;

	/**
	 * Contains only chart labels which are painted (visible) all hidden labels are NOT in this
	 * list.
	 */
	private ArrayList<SegmenterSegment>	_paintedSegments	= new ArrayList<>();

	// hide small values
	private boolean						_isHideSmallValues;
	private double						_smallValue;

	// show lines
	private boolean						_isShowSegmenterLine;
	private int							_lineOpacity;

	private boolean						_isShowDecimalPlaces;
	private boolean						_isShowSegmenterMarker;
	private boolean						_isShowSegmenterValue;
	private int							_stackedValues;
	private double[]					_xDataSerie;

	/**
	 * Area where the graph is painted.
	 */
	private Rectangle					_graphArea;

	private final NumberFormat			_nf1				= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	ChartLayerSegmentAltitude(final TourChart tourChart) {

		_tourChart = tourChart;
	}

	@Override
	public void draw(	final GC gc,
						final GraphDrawingData graphDrawingData,
						final Chart chart,
						final PixelConverter pixelConverter) {

		/**
		 * Cleanup hovering data.
		 * <p>
		 * This layer is painted only once.
		 */
		_paintedSegments.clear();

		final ChartDataYSerie yData = graphDrawingData.getYData();

		final Object segmentConfigObject = yData.getCustomData(TourManager.CUSTOM_DATA_SEGMENT_VALUES);
		if ((segmentConfigObject instanceof ConfigGraphSegment) == false) {
			return;
		}

		final ConfigGraphSegment segmentConfig = (ConfigGraphSegment) segmentConfigObject;
		final float[] segmentValues = segmentConfig.segmentDataSerie;

		// check segment values
		if (segmentValues == null) {
			return;
		}

		_tourChart.setLineSelectionDirty();

		final int graphWidth = graphDrawingData.getChartDrawingData().devVisibleChartWidth;
		final int devYTop = graphDrawingData.getDevYTop();
		final int devYBottom = graphDrawingData.getDevYBottom();
		final long devGraphImageXOffset = chart.getXXDevViewPortLeftBorder();

		final float graphYBottom = graphDrawingData.getGraphYBottom();
		final float[] yValues = yData.getHighValuesFloat()[0];

		final double scaleX = graphDrawingData.getScaleX();
		final double scaleY = graphDrawingData.getScaleY();

		final double minValueAdjustment = segmentConfig.minValueAdjustment;
		final double maxValue = yData.getOriginalMaxValue();
		final double hideThreshold = maxValue * _smallValue * minValueAdjustment;

		final int[] segmentSerieIndex = _tourData.segmentSerieIndex;
		final int segmentSerieSize = segmentSerieIndex.length;

		final Long[] multipleTourIds = _tourData.multipleTourIds;
		final int[] multipleTourStartIndex = _tourData.multipleTourStartIndex;
		int tourIndex = 0;
		int nextTourStartIndex = 0;

		final ValueOverlapChecker overlapChecker = new ValueOverlapChecker(_stackedValues);

		int devXPrev = Integer.MIN_VALUE;
		int devYPrev = Integer.MIN_VALUE;

		final float[] segmentSerieAltitudeDiff = _tourData.segmentSerie_Altitude_Diff;
		final float[] segmentSerieComputedAltitudeDiff = _tourData.segmentSerie_Altitude_Diff_Computed;

		final LineAttributes defaultLineAttributes = gc.getLineAttributes();
		final LineAttributes markerLineAttribute = new LineAttributes(5);
		markerLineAttribute.dashOffset = 3;
		markerLineAttribute.style = SWT.LINE_CUSTOM;
		markerLineAttribute.dash = new float[] { 1f, 2f };
		markerLineAttribute.width = 1f;

		// setup font for values
		final Font fontBackup = gc.getFont();
		gc.setFont(_tourChart.getValueFont());

		// do not draw over the graph area
		_graphArea = new Rectangle(0, devYTop, graphWidth, devYBottom - devYTop);
		gc.setClipping(_graphArea);

		gc.setAntialias(chart.graphAntialiasing);
		gc.setTextAntialias(chart.graphAntialiasing);

		final RGB segmentRGB = segmentConfig.segmentLineRGB;
		final Color segmentColor = new Color(gc.getDevice(), segmentRGB);
		final RGB rgbUp = new RGB(0xff, 0x5e, 0x62);
		final Color colorUp = new Color(gc.getDevice(), rgbUp);
		{
			int segmentIndex;

			for (segmentIndex = 0; segmentIndex < segmentSerieSize; segmentIndex++) {

				// get current value
				final int dataSerieIndex = segmentSerieIndex[segmentIndex];
				final int devXSegment = (int) (_xDataSerie[dataSerieIndex] * scaleX - devGraphImageXOffset);
				final int segmentWidth = devXSegment - devXPrev;

				// optimize performance
				if (devXSegment < 0 || devXSegment > graphWidth) {

					// get next value
					if (segmentIndex < segmentSerieSize - 2) {

						final int serieIndexNext = segmentSerieIndex[segmentIndex + 1];
						final int devXValueNext = (int) (_xDataSerie[serieIndexNext] * scaleX - devGraphImageXOffset);

						if (devXValueNext < 0) {

							// current and next value are outside of the visible area
							continue;
						}
					}

					// get previous value
					if (segmentIndex > 0) {

						final int serieIndexPrev = segmentSerieIndex[segmentIndex - 1];
						final int devXValuePrev = (int) (_xDataSerie[serieIndexPrev] * scaleX - devGraphImageXOffset);

						if (devXValuePrev > graphWidth) {

							// current and previous value are outside of the visible area
							break;
						}
					}
				}

				final int yValueIndex = Math.min(yValues.length - 1, dataSerieIndex);
				final float yValue = yValues[yValueIndex];

				final int devYGraph = (int) ((yValue - graphYBottom) * scaleY);
				final int devYSegment = devYBottom - devYGraph;
				final int segmentHeight = devYSegment - devYPrev;

				/*
				 * Get up/down value
				 */
				float altiDiff = 0;
				if (segmentIndex > 0) {
					if (segmentSerieComputedAltitudeDiff != null) {
						altiDiff = segmentSerieComputedAltitudeDiff[segmentIndex];
					} else {
						altiDiff = segmentSerieAltitudeDiff[segmentIndex];
					}
				}
				final boolean isValueUp = altiDiff >= 0;

				boolean isShowValueText = true;

				if (_isHideSmallValues) {

					// check values if they are small enough

					if (altiDiff >= 0) {
						if (altiDiff < hideThreshold) {
							isShowValueText = false;
						}
					} else {

						// diff <0
						if (-altiDiff < hideThreshold) {
							isShowValueText = false;
						}
					}
				}

				/*
				 * Get value text
				 */
				final String valueText = _isShowDecimalPlaces //
						? _nf1.format(altiDiff)
						: Integer.toString((int) (altiDiff > 0 //
								? (altiDiff + 0.5)
								: (altiDiff - 0.5)));

				final Point textExtent = gc.textExtent(valueText);

				final int textWidth = textExtent.x;
				final int textHeight = textExtent.y;

				Color paintedColor;
				RGB paintedRGB;
				if (altiDiff < 0) {
					paintedColor = segmentColor;
					paintedRGB = segmentRGB;
				} else {
					paintedColor = colorUp;
					paintedRGB = rgbUp;
				}

				final SegmenterSegment segmenterSegment = new SegmenterSegment();

				/*
				 * Connect two segments with a line
				 */
				if (devXPrev == Integer.MIN_VALUE) {

					// first visible segment

					devXPrev = devXSegment;
					devYPrev = devYSegment;

				} else {

					if (_isShowSegmenterLine /* && isShowValueText */) {

						gc.setAlpha(_lineOpacity);
						gc.setForeground(paintedColor);
						gc.setLineAttributes(defaultLineAttributes);

						gc.drawLine(//
								devXPrev,
								devYPrev,
								devXSegment,
								devYSegment);
					}

					segmenterSegment.paintedX1 = devXPrev;
					segmenterSegment.paintedY1 = devYPrev;

					segmenterSegment.paintedX2 = devXSegment;
					segmenterSegment.paintedY2 = devYSegment;

					segmenterSegment.hoveredLineShape = new Line2D(//
							devXPrev,
							devYPrev,
							devXSegment,
							devYSegment);

					// use ALLWAYS the same instance
					segmenterSegment.paintedRGB = paintedRGB;

					/*
					 * Draw a line from the value marker to the top or the bottom
					 */
					if (_isShowSegmenterMarker) {

						int devYLine;
						if (isValueUp) {
							devYLine = devYSegment - 1 * textHeight;
						} else {
							devYLine = devYSegment + 1 * textHeight;
						}

						gc.setAlpha(0xff);
						gc.setForeground(paintedColor);
						gc.setLineAttributes(markerLineAttribute);

						gc.drawLine(//
								devXSegment,
								devYSegment,
								devXSegment,
								devYLine);
					}

					if (_isShowSegmenterValue) {

						// show segment value

						/*
						 * get default y position
						 */
						final float yDiff2 = segmentHeight / 2;
						final int devYText = (int) (devYSegment - yDiff2 + (isValueUp ? -textHeight : 0));

						/*
						 * Get default x position
						 */
						final float segmentWidth2 = segmentWidth / 2;
						final int devXText = (int) (devXPrev + segmentWidth2 - textWidth);

						final int borderWidth = 5;
						final int borderWidth2 = 2 * borderWidth;
						final int borderHeight = 0;
						final int borderHeight2 = 2 * borderHeight;
						final int textHeightWithBorder = textHeight + borderHeight2;

						/*
						 * Ensure the value text do not overlap, if possible :-)
						 */
						final Rectangle textRect = new Rectangle(//
								devXText - borderWidth2,
								devYText - borderHeight,
								textWidth + borderWidth2,
								textHeightWithBorder);

						final Rectangle validRect = overlapChecker.getValidRect(
								textRect,
								isValueUp,
								textHeightWithBorder,
								valueText);

						// don't draw over the graph borders
						if (validRect != null && validRect.y > devYTop && validRect.y + textHeight < devYBottom) {

							if (isShowValueText) {

								// keep current valid rectangle
								overlapChecker.setupNext(validRect, isValueUp);

								gc.setAlpha(0xff);
								gc.setForeground(paintedColor);
								gc.drawText(//
										valueText,
										devXText - borderWidth,
										validRect.y + borderHeight,
										true);

								// ensure that only visible labels can be hovered
								segmenterSegment.isValueVisible = true;
							}

							segmenterSegment.paintedLabel = validRect;

							// keep area to detect hovered segments, enlarge it with the hover border to easier hit the label
							final Rectangle hoveredRect = new Rectangle(
									(validRect.x + borderWidth - SegmenterSegment.EXPANDED_HOVER_SIZE2),
									(validRect.y + borderHeight - SegmenterSegment.EXPANDED_HOVER_SIZE2),
									(validRect.width - borderWidth2 + SegmenterSegment.EXPANDED_HOVER_SIZE),
									(validRect.height - borderHeight2 + SegmenterSegment.EXPANDED_HOVER_SIZE));

							segmenterSegment.hoveredLabelRect = hoveredRect;

//							/*
//							 * Debugging
//							 */
//							gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
//							gc.setLineAttributes(defaultLineAttributes);
//							gc.drawRectangle(validRect);
						}
					}
				}

				/*
				 * Set slider positions
				 */
				final int prevSegmentIndex = segmentIndex - 1;
				final int leftIndex = prevSegmentIndex < 0
						? SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
						: segmentSerieIndex[prevSegmentIndex];
				segmenterSegment.xSliderSerieIndexLeft = leftIndex;
				segmenterSegment.xSliderSerieIndexRight = dataSerieIndex;

				segmenterSegment.segmentIndex = segmentIndex;

				segmenterSegment.serieIndex = dataSerieIndex;
				segmenterSegment.graphX = _xDataSerie[dataSerieIndex];

				segmenterSegment.devGraphWidth = graphWidth;
				segmenterSegment.devYGraphTop = devYTop;

				/*
				 * Get tour id for each segment
				 */
				if (_tourData.isMultipleTours) {

					// multiple tours are displayed

					/*
					 * This algorithm is highly complicated because of the complex start values but
					 * now it seams to work.
					 */
					for (; tourIndex < multipleTourStartIndex.length;) {

						if (nextTourStartIndex == 0) {

							// 1st segment

							nextTourStartIndex = multipleTourStartIndex[1];

							segmenterSegment.tourId = multipleTourIds[0];
							break;

						} else {

							// 2nd...n segments

							if (dataSerieIndex <= nextTourStartIndex) {

								segmenterSegment.tourId = multipleTourIds[tourIndex];
								break;

							} else {

								tourIndex++;

								if (tourIndex >= multipleTourStartIndex.length - 1) {
									nextTourStartIndex = Integer.MAX_VALUE;
								} else {
									nextTourStartIndex = multipleTourStartIndex[tourIndex + 1];
								}
							}
						}
					}

				} else {

					// a single tour is displayed

					segmenterSegment.tourId = _tourData.getTourId();
				}

				_paintedSegments.add(segmenterSegment);

				// advance to the next point
				devXPrev = devXSegment;
				devYPrev = devYSegment;
			}
		}
		segmentColor.dispose();
		colorUp.dispose();

		// reset clipping
		gc.setClipping((Rectangle) null);

		// restore font
		gc.setFont(fontBackup);

		gc.setAlpha(0xff);

	}

	@Override
	public void drawOverlay(final GC gc, final GraphDrawingData graphDrawingData) {

		final SegmenterSegment hoveredSegment = _tourChart.getHoveredSegmenterSegment();

		final boolean isHovered = hoveredSegment != null;

		if (!isHovered) {
			return;
		}

		final int devYTop = graphDrawingData.getDevYTop();
		final int devGraphHeight = graphDrawingData.devGraphHeight;

		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setClipping(0, devYTop, gc.getClipping().width, devGraphHeight);

		final Device device = gc.getDevice();
		final Color colorHovered = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);

		/*
		 * Draw label background
		 */
		if (isHovered) {

			final Rectangle hoveredRect = hoveredSegment.hoveredLabelRect;
			if (hoveredRect != null && hoveredSegment.isValueVisible) {

				final int arc = 10;
//				gc.setAlpha(isSelected ? 0x60 : 0x30);
				gc.setAlpha(0x30);
				gc.setBackground(colorHovered);
				gc.fillRoundRectangle(//
						hoveredRect.x,//
						hoveredRect.y,
						hoveredRect.width,
						hoveredRect.height,
						arc,
						arc);
			}

			/*
			 * Draw line thicker
			 */
			final Color lineColor = new Color(device, hoveredSegment.paintedRGB);
			{
				final int x1 = hoveredSegment.paintedX1;
				final int y1 = hoveredSegment.paintedY1;
				final int x2 = hoveredSegment.paintedX2;
				final int y2 = hoveredSegment.paintedY2;

				gc.setAntialias(SWT.ON);
				gc.setForeground(lineColor);
				gc.setLineCap(SWT.CAP_ROUND);

				// draw hovered segment
				gc.setAlpha(0xff);
				gc.setLineWidth(3);
				gc.drawLine(x1, y1, x2, y2);
			}
			lineColor.dispose();
		}

		gc.setAlpha(0xff);
		gc.setClipping((Rectangle) null);
	}

	Rectangle getGraphArea() {
		return _graphArea;
	}

	/**
	 * @param mouseEvent
	 * @return Returns the hovered {@link ChartLabel} or <code>null</code> when a {@link ChartLabel}
	 *         is not hovered.
	 */
	SegmenterSegment getHoveredSegment(final ChartMouseEvent mouseEvent) {

		if (_graphArea == null) {

			// this happened, propably when not initialized
			return null;
		}

		SegmenterSegment hoveredLabel = null;

		if (_graphArea.contains(mouseEvent.devXMouse, mouseEvent.devYMouse)) {

			// mouse is hovering the graph area

			hoveredLabel = getHoveredSegment_10(mouseEvent.devXMouse, mouseEvent.devYMouse);
		}

		return hoveredLabel;
	}

	private SegmenterSegment getHoveredSegment_10(final int devXMouse, final int devYMouse) {

		for (final SegmenterSegment paintedSegment : _paintedSegments) {

			final Rectangle hoveredLabelRect = paintedSegment.hoveredLabelRect;
			final Line2D hoveredLineShape = paintedSegment.hoveredLineShape;

			if (//
				// the label must be visible that it is checked
			(paintedSegment.isValueVisible && hoveredLabelRect != null && hoveredLabelRect.contains(
					devXMouse,
					devYMouse))
					|| (hoveredLineShape != null && hoveredLineShape.intersects(
							devXMouse - SegmenterSegment.EXPANDED_HOVER_SIZE2,
							devYMouse - SegmenterSegment.EXPANDED_HOVER_SIZE2,
							SegmenterSegment.EXPANDED_HOVER_SIZE,
							SegmenterSegment.EXPANDED_HOVER_SIZE))) {

				// segment is hit
				return paintedSegment;
			}
		}

		return null;
	}

	/**
	 * @return Returns <b>ONLY</b> labels which are visible. When zoomed in then some labels are not
	 *         contained in this list.
	 */
	ArrayList<SegmenterSegment> getPaintedSegments() {
		return _paintedSegments;
	}

	void setIsShowDecimalPlaces(final boolean isShowDecimalPlaces) {
		_isShowDecimalPlaces = isShowDecimalPlaces;
	}

	void setIsShowSegmenterMarker(final boolean isShowSegmenterMarker) {
		_isShowSegmenterMarker = isShowSegmenterMarker;
	}

	void setIsShowSegmenterValue(final boolean isShowSegmenterValue) {
		_isShowSegmenterValue = isShowSegmenterValue;
	}

	void setLineProperties(final boolean isShowSegmenterLine, final int lineOpacity) {

		_isShowSegmenterLine = isShowSegmenterLine;
		_lineOpacity = (int) (lineOpacity / 100.0 * 255);
	}

	void setSmallHiddenValuesProperties(final boolean isHideSmallValues, final int smallValue) {

		_isHideSmallValues = isHideSmallValues;
		_smallValue = smallValue / 100.0;
	}

	void setStackedValues(final int stackedValues) {
		_stackedValues = stackedValues;
	}

	/**
	 * Setup new tour for this layer.
	 * 
	 * @param tourData
	 */
	void setTourData(final TourData tourData) {

		_tourData = tourData;

		_paintedSegments.clear();
	}

	void setXDataSerie(final double[] dataSerie) {
		_xDataSerie = dataSerie;
	}

}
