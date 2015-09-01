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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * This layer displays the altitude values.
 */
public class ChartLayerSegmentAltitude implements IChartLayer, IChartOverlay {

	private static final Color		SYSTEM_COLOR_0		= Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
	private static final Color		SYSTEM_COLOR_DOWN	= Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
	private static final Color		SYSTEM_COLOR_UP		= Display.getCurrent().getSystemColor(SWT.COLOR_RED);

	private TourChart				_tourChart;
	private TourData				_tourData;

	private ArrayList<ChartLabel>	_graphLabels		= new ArrayList<>();

	// hide small values
	private boolean					_isHideSmallValues;
	private double					_smallValue;

	// show lines
	private boolean					_isShowSegmenterLine;
	private int						_lineOpacity;

	private boolean					_isShowDecimalPlaces;
	private boolean					_isShowSegmenterMarker;
	private boolean					_isShowSegmenterValue;
	private int						_stackedValues;
	private double[]				_xDataSerie;

	/**
	 * Area where the graph is painted.
	 */
	private Rectangle				_graphArea;

	private final NumberFormat		_nf1				= NumberFormat.getNumberInstance();
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
		_graphLabels.clear();

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

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \t")
//				+ ("\t" + String.format("%9.2f  ", maxValue))
//				+ ("\t" + String.format("%9.2f  ", hideThreshold))
//				+ ("\t" + String.format("%3d  ", (int) (_smallValue * 100)))
//				+ ("\t" + minValueAdjustment)
//		//
//				);
//		// TODO remove SYSTEM.OUT.PRINTLN

		final ValueOverlapChecker posChecker = new ValueOverlapChecker(_stackedValues);

		int devXPrev = Integer.MIN_VALUE;
		int devYPrev = Integer.MIN_VALUE;

		final float[] segmentSerieAltitudeDiff = _tourData.segmentSerieAltitudeDiff;
		final float[] segmentSerieComputedAltitudeDiff = _tourData.segmentSerieComputedAltitudeDiff;

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

		{
			int segmentIndex;

			for (segmentIndex = 0; segmentIndex < segmentSerieSize; segmentIndex++) {

				// get current value
				final int serieIndex = segmentSerieIndex[segmentIndex];
				final int devXSegment = (int) (_xDataSerie[serieIndex] * scaleX - devGraphImageXOffset);
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

				final int yValueIndex = Math.min(yValues.length - 1, serieIndex);
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

				if (isShowValueText) {

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

					final Color upDownColor = getColor(altiDiff);

					final ChartLabel chartLabel = new ChartLabel();

					/*
					 * Connect two segments with a line
					 */
					if (devXPrev == Integer.MIN_VALUE) {

						// first visible segment

						devXPrev = devXSegment;
						devYPrev = devYSegment;

					} else {

						if (_isShowSegmenterLine) {

							gc.setAlpha(_lineOpacity);
							gc.setForeground(upDownColor);
							gc.setLineAttributes(defaultLineAttributes);

							gc.drawLine(//
									devXPrev,
									devYPrev,
									devXSegment,
									devYSegment);
						}

						chartLabel.paintedX1 = devXPrev;
						chartLabel.paintedY1 = devYPrev;

						chartLabel.paintedX2 = devXSegment;
						chartLabel.paintedY2 = devYSegment;

						int hoveredHeight = segmentHeight < 0 ? -segmentHeight : segmentHeight;
						int devYHovered = devYPrev < devYSegment ? devYPrev : devYSegment;
						if (hoveredHeight < ChartLabel.MIN_HOVER_LINE_HEIGHT) {
							hoveredHeight = ChartLabel.MIN_HOVER_LINE_HEIGHT;
							devYHovered -= ChartLabel.MIN_HOVER_LINE_HEIGHT / 2;
						}

						chartLabel.hoveredRect = new Rectangle(//
								devXPrev,
								devYHovered,
								segmentWidth,
								hoveredHeight);

						chartLabel.paintedRGB = upDownColor.getRGB();

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
							gc.setForeground(upDownColor);
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

							final Rectangle validRect = posChecker.getValidRect(
									textRect,
									isValueUp,
									textHeightWithBorder,
									valueText);

							// don't draw over the graph borders
							if (validRect != null && validRect.y > devYTop && validRect.y + textHeight < devYBottom) {

								// keep current valid rectangle
								posChecker.setupNext(validRect, isValueUp);

								gc.setAlpha(0xff);
								gc.setForeground(upDownColor);
								gc.drawText(//
										valueText,
										devXText - borderWidth,
										validRect.y + borderHeight,
										true);

								chartLabel.paintedLabel = validRect;

								// keep area to detect hovered segments, enlarge it with the hover border to easier hit the label
								final Rectangle hoveredRect = new Rectangle(
										(validRect.x + borderWidth - ChartLabel.MARKER_HOVER_SIZE),
										(validRect.y + borderHeight - ChartLabel.MARKER_HOVER_SIZE),
										(validRect.width - borderWidth2 + 2 * ChartLabel.MARKER_HOVER_SIZE),
										(validRect.height - borderHeight2 + 2 * ChartLabel.MARKER_HOVER_SIZE));

								chartLabel.hoveredLabel = hoveredRect;

//								/*
//								 * Debugging
//								 */
//								gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
//								gc.setLineAttributes(defaultLineAttributes);
//								gc.drawRectangle(validRect);
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

					chartLabel.xSliderSerieIndexLeft = leftIndex;
					chartLabel.xSliderSerieIndexRight = serieIndex;

					chartLabel.segmentIndex = segmentIndex;

					chartLabel.serieIndex = serieIndex;
					chartLabel.graphX = _xDataSerie[serieIndex];

					_graphLabels.add(chartLabel);
				}

				// advance to the next point
				devXPrev = devXSegment;
				devYPrev = devYSegment;
			}
		}

		// reset clipping
		gc.setClipping((Rectangle) null);

		// restore font
		gc.setFont(fontBackup);

		gc.setAlpha(0xff);

	}

	@Override
	public void drawOverlay(final GC gc, final GraphDrawingData graphDrawingData) {

		final ChartLabel hoveredLabel = _tourChart.getSegmentLabel_Hovered();
//		final ChartLabel selectedLabel_1 = _tourChart.getSegmentLabel_Selected_1();

//		final boolean isSelected = selectedLabel_1 != null;
		final boolean isHovered = hoveredLabel != null;

		if (!isHovered /* && !isSelected */) {
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

			final Rectangle paintedLabel = hoveredLabel.hoveredLabel;
			if (paintedLabel != null) {

				final int arc = 10;
//				gc.setAlpha(isSelected ? 0x60 : 0x30);
				gc.setAlpha(0x30);
				gc.setBackground(colorHovered);
				gc.fillRoundRectangle(//
						paintedLabel.x,//
						paintedLabel.y,
						paintedLabel.width,
						paintedLabel.height,
						arc,
						arc);
			}

			/*
			 * Draw line thicker
			 */
			final Color lineColor = new Color(device, hoveredLabel.paintedRGB);
			{
				final int x1 = hoveredLabel.paintedX1;
				final int y1 = hoveredLabel.paintedY1;
				final int x2 = hoveredLabel.paintedX2;
				final int y2 = hoveredLabel.paintedY2;

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

	private Color getColor(final float altiDiff) {

		if (altiDiff > 0) {
			return SYSTEM_COLOR_UP;
		} else if (altiDiff < 0) {
			return SYSTEM_COLOR_DOWN;
		} else {
			return SYSTEM_COLOR_0;
		}
	}

	/**
	 * @param mouseEvent
	 * @return Returns the hovered {@link ChartLabel} or <code>null</code> when a {@link ChartLabel}
	 *         is not hovered.
	 */
	ChartLabel getHoveredLabel(final ChartMouseEvent mouseEvent) {

		if (_graphArea == null) {

			// this happened, propably when not initialized
			return null;
		}

		ChartLabel hoveredLabel = null;

		if (_graphArea.contains(mouseEvent.devXMouse, mouseEvent.devYMouse)) {

			// mouse is hovering the graph area

			hoveredLabel = getHoveredLabel_10(mouseEvent.devXMouse, mouseEvent.devYMouse);
		}

		return hoveredLabel;
	}

	private ChartLabel getHoveredLabel_10(final int devXMouse, final int devYMouse) {

		for (final ChartLabel chartLabel : _graphLabels) {

			final Rectangle hoveredLabel = chartLabel.hoveredLabel;
			final Rectangle hoveredRect = chartLabel.hoveredRect;

			if ((hoveredLabel != null && hoveredLabel.contains(devXMouse, devYMouse))
					|| (hoveredRect != null && hoveredRect.contains(devXMouse, devYMouse))) {

				// segment is hit
				return chartLabel;
			}
		}

		return null;
	}

	ArrayList<ChartLabel> getPaintedLabels() {
		return _graphLabels;
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

		_graphLabels.clear();
	}

	void setXDataSerie(final double[] dataSerie) {
		_xDataSerie = dataSerie;
	}

}
