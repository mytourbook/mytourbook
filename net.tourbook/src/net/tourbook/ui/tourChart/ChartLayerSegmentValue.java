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
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * This layer displays the average values for a segment.
 */
public class ChartLayerSegmentValue implements IChartLayer {

	private TourChart							_tourChart;
	private TourData							_tourData;

	private double								_smallValue;
	private boolean								_isHideSmallValues;
	private boolean								_isShowDecimalPlaces;
	private boolean								_isShowSegmenterLine;
	private boolean								_isShowSegmenterValues;
	private int									_lineOpacity;
	private int									_stackedValues;
	private double[]							_xDataSerie;

	/**
	 * Area where the graph is painted.
	 */
	private ArrayList<Rectangle>				_allGraphArea	= new ArrayList<>();

	private ArrayList<ArrayList<ChartLabel>>	_allChartLabel	= new ArrayList<>();

	private int									_paintedGraphs;

	private final NumberFormat					_nf1			= NumberFormat.getNumberInstance();

	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	public ChartLayerSegmentValue(final TourChart tourChart) {

		_tourChart = tourChart;
	}

	/**
	 * Draws the marker(s) for the current graph config
	 * 
	 * @param gc
	 * @param graphDrawingData
	 * @param chartComponents
	 */
	@Override
	public void draw(	final GC gc,
						final GraphDrawingData graphDrawingData,
						final Chart chart,
						final PixelConverter pixelConverter) {

		final int[] segmentSerieIndex = _tourData.segmentSerieIndex;

		if (segmentSerieIndex == null) {
			return;
		}

		final int segmentSerieSize = segmentSerieIndex.length;

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

		final IValueLabelProvider segmentLabelProvider = segmentConfig.labelProvider;

		boolean toggleAboveBelow = false;

		final int graphWidth = graphDrawingData.getChartDrawingData().devVisibleChartWidth;
		final int devYTop = graphDrawingData.getDevYTop();
		final int devYBottom = graphDrawingData.getDevYBottom();
		final long devGraphImageXOffset = chart.getXXDevViewPortLeftBorder();

		final float graphYBottom = graphDrawingData.getGraphYBottom();

		final int valueDivisor = yData.getValueDivisor();
		final double scaleX = graphDrawingData.getScaleX();
		final double scaleY = graphDrawingData.getScaleY();

		final double maxValue = yData.getOriginalMaxValue();
		final double minValueAdjustment = segmentConfig.minValueAdjustment;
		final double hiddenThreshold = maxValue * _smallValue * minValueAdjustment;

		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \t")
				+ ("\t" + String.format("%9.2f  ", maxValue))
				+ ("\t" + String.format("%9.2f  ", hiddenThreshold))
				+ ("\t" + String.format("%3d  ", (int) (_smallValue * 100)))
				+ ("\t" + minValueAdjustment)
		//
				);
		// TODO remove SYSTEM.OUT.PRINTLN

		final ValueOverlapChecker posChecker = new ValueOverlapChecker(_stackedValues);

		final Display display = Display.getCurrent();

		// setup font
		final Font fontBackup = gc.getFont();
		gc.setFont(_tourChart.getValueFont());

		gc.setLineStyle(SWT.LINE_SOLID);

		/*
		 * Remove previous data, this is a bit tricky because the first graph which is painted in
		 * this layer can be 1 or 0.
		 */
		if (graphDrawingData.graphIndex <= _paintedGraphs) {
			_allGraphArea.clear();
			_allChartLabel.clear();
		}
		_paintedGraphs = graphDrawingData.graphIndex;

		final Rectangle graphArea = new Rectangle(0, devYTop, graphWidth, devYBottom - devYTop);
		_allGraphArea.add(graphArea);

		final ArrayList<ChartLabel> graphAreaLabels = new ArrayList<>();
		_allChartLabel.add(graphAreaLabels);

		// do not draw over the graph area
		gc.setClipping(graphArea);

		final Color lineColor = new Color(display, segmentConfig.segmentLineRGB);
		gc.setForeground(lineColor);
		{
			int devXPrev = Integer.MIN_VALUE;

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

				final float graphYValue = segmentValues[segmentIndex] * valueDivisor;
				final int devYGraph = (int) (scaleY * (graphYValue - graphYBottom));
				final int devYSegment = devYBottom - devYGraph;

				boolean isShowSegment = true;

				if (_isHideSmallValues) {

					// check values if they are small enough

					if (graphYValue >= 0) {
						if (graphYValue < hiddenThreshold) {
							isShowSegment = false;
						}
					} else {

						// value <0
						if (-graphYValue < hiddenThreshold) {
							isShowSegment = false;
						}
					}
				}

				if (isShowSegment) {

					/*
					 * Get value text
					 */
					String valueText;
					if (segmentLabelProvider != null) {

						// get value text from a label provider
						valueText = segmentLabelProvider.getLabel(graphYValue);

					} else {

						if (graphYValue < 0.0 || graphYValue > 0.0) {

							valueText = _isShowDecimalPlaces //
									? _nf1.format(graphYValue)
									: Integer.toString((int) (graphYValue > 0 //
											? (graphYValue + 0.5)
											: (graphYValue - 0.5)));

						} else {

							// hide digits
							valueText = UI.ZERO;
						}
					}

					final ChartLabel chartLabel = new ChartLabel();

					/*
					 * Connect two segments with a line
					 */
					if (devXPrev == Integer.MIN_VALUE) {

						// first visible segment

						devXPrev = devXSegment;

					} else {

						if (_isShowSegmenterLine) {

							gc.setAlpha(_lineOpacity);
							gc.drawLine(//
									devXPrev,
									devYSegment,
									devXSegment,
									devYSegment);
						}

						chartLabel.paintedX1 = devXPrev;
						chartLabel.paintedY1 = devYSegment;

						chartLabel.paintedX2 = devXSegment;
						chartLabel.paintedY2 = devYSegment;

						chartLabel.hoveredRect = new Rectangle(//
								devXPrev,
								devYSegment - ChartLabel.MIN_HOVER_LINE_HEIGHT / 2,
								segmentWidth,
								ChartLabel.MIN_HOVER_LINE_HEIGHT);

						chartLabel.paintedRGB = segmentConfig.segmentLineRGB;

						if (_isShowSegmenterValues) {

							final Point textExtent = gc.textExtent(valueText);

							final int textWidth = textExtent.x;
							final int textHeight = textExtent.y;

							final int devXText = devXPrev + segmentWidth / 2 - textWidth / 2;

							final boolean isValueUp = graphYValue > 0;
							int devYText;
							boolean isDrawAbove;

							if (segmentConfig.canHaveNegativeValues) {

								isDrawAbove = isValueUp;

							} else {

								// toggle above/below segment line
								isDrawAbove = toggleAboveBelow = !toggleAboveBelow;
							}

							if (isDrawAbove) {

								// draw above segment line
								devYText = devYSegment - textHeight;

							} else {

								// draw below segment line
								devYText = devYSegment + 2;
							}

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
								gc.drawText(//
										valueText,
										devXText,
										validRect.y,
										true);

								chartLabel.paintedLabel = validRect;

								// keep area to detect hovered segments, enlarge it with the hover border to easier hit the label
								final Rectangle hoveredRect = new Rectangle(
										(validRect.x + borderWidth),
										(validRect.y + borderHeight - ChartLabel.MARKER_HOVER_SIZE),
										(validRect.width - borderWidth2 + 2 * ChartLabel.MARKER_HOVER_SIZE),
										(validRect.height - borderHeight2 + 2 * ChartLabel.MARKER_HOVER_SIZE));

								chartLabel.hoveredLabel = hoveredRect;

//							/*
//							 * Debugging
//							 */
//							gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
//							gc.drawRectangle(hoveredRect);
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

					graphAreaLabels.add(chartLabel);
				}

				// advance to the next point
				devXPrev = devXSegment;
			}
		}
		lineColor.dispose();

		// reset clipping
		gc.setClipping((Rectangle) null);

		// restore font
		gc.setFont(fontBackup);
	}

	/**
	 * @param mouseEvent
	 * @return Returns the hovered {@link ChartLabel} or <code>null</code> when a {@link ChartLabel}
	 *         is not hovered.
	 */
	ChartLabel getHoveredLabel(final ChartMouseEvent mouseEvent) {

		ChartLabel hoveredLabel = null;

		for (int graphIndex = 0; graphIndex < _allGraphArea.size(); graphIndex++) {

			final Rectangle graphArea = _allGraphArea.get(graphIndex);

			if (graphArea.contains(mouseEvent.devXMouse, mouseEvent.devYMouse)) {

				// mouse is hovering the graph area

				hoveredLabel = getHoveredLabel_10(graphIndex, mouseEvent.devXMouse, mouseEvent.devYMouse);

				break;
			}
		}

		return hoveredLabel;
	}

	private ChartLabel getHoveredLabel_10(final int graphIndex, final int devXMouse, final int devYMouse) {

		final ArrayList<ChartLabel> chartLabels = _allChartLabel.get(graphIndex);

		for (final ChartLabel chartLabel : chartLabels) {

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

	void setIsShowDecimalPlaces(final boolean isShowDecimalPlaces) {
		_isShowDecimalPlaces = isShowDecimalPlaces;
	}

	void setIsShowSegmenterValues(final boolean isShowSegmenterValues) {
		_isShowSegmenterValues = isShowSegmenterValues;
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

	void setTourData(final TourData tourData) {
		_tourData = tourData;
	}

	void setXDataSerie(final double[] dataSerie) {
		_xDataSerie = dataSerie;
	}
}
