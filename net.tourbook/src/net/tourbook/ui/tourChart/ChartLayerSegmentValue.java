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

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartMouseEvent;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;
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

	private TourChart			_tourChart;
	private TourData			_tourData;

	private double				_hiddenValueSize;
	private boolean				_isHideSmallValues;
	private boolean				_isShowDecimalPlaces;
	private boolean				_isShowSegmenterLine;
	private boolean				_isShowSegmenterValues;
	private int					_lineOpacity;
	private int					_stackedValues;
	private double[]			_xDataSerie;

	private final NumberFormat	_nf1	= NumberFormat.getNumberInstance();

	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	/**
	 * Area where the graph is painted.
	 */
	private Rectangle			_graphRect;

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

		final int[] segmentSerie = _tourData.segmentSerieIndex;

		if (segmentSerie == null) {
			return;
		}

		final int segmentSerieSize = segmentSerie.length;

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

		final Display display = Display.getCurrent();
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
		final double maxGraphValue = maxValue * _hiddenValueSize;

		final ValueOverlapChecker posChecker = new ValueOverlapChecker(_stackedValues);

		// setup font
		final Font fontBackup = gc.getFont();
		gc.setFont(_tourChart.getValueFont());

		gc.setLineStyle(SWT.LINE_SOLID);

		// do not draw over the graph area
		_graphRect = new Rectangle(0, devYTop, graphWidth, devYBottom - devYTop);
		gc.setClipping(_graphRect);

		final Color lineColor = new Color(display, segmentConfig.segmentLineColor);
		final Color textColor = new Color(display, segmentConfig.segmentLineColor);
		{
			int devXPrev = Integer.MIN_VALUE;
			int devYPrev = Integer.MIN_VALUE;

			int segmentIndex;
			for (segmentIndex = 0; segmentIndex < segmentSerieSize; segmentIndex++) {

				// get current value
				final int serieIndex = segmentSerie[segmentIndex];
				final int devXSegment = (int) (_xDataSerie[serieIndex] * scaleX - devGraphImageXOffset);
				final int segmentWidth = devXSegment - devXPrev;

				// optimize performance
				if (devXSegment < 0 || devXSegment > graphWidth) {

					// get next value
					if (segmentIndex < segmentSerieSize - 2) {

						final int serieIndexNext = segmentSerie[segmentIndex + 1];
						final int devXValueNext = (int) (_xDataSerie[serieIndexNext] * scaleX - devGraphImageXOffset);

						if (devXValueNext < 0) {

							// current and next value are outside of the visible area
							continue;
						}
					}

					// get previous value
					if (segmentIndex > 0) {

						final int serieIndexPrev = segmentSerie[segmentIndex - 1];
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

				boolean isShowValueText = true;
				if (_isHideSmallValues) {

					// check values if they are small enough

					if (graphYValue >= 0) {
						if (graphYValue < maxGraphValue) {
							isShowValueText = false;
						}
					} else {

						// value <0
						if (-graphYValue < maxGraphValue) {
							isShowValueText = false;
						}
					}
				}

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

				final Point textExtent = gc.textExtent(valueText);

				final int textWidth = textExtent.x;
				final int textHeight = textExtent.y;

				/*
				 * Connect two segments with a line
				 */
				if (devXPrev == Integer.MIN_VALUE) {

					// first visible segment

					devXPrev = devXSegment;
					devYPrev = devYSegment;

				} else {

					if (_isShowSegmenterLine && isShowValueText) {

						gc.setAlpha(_lineOpacity);
						gc.setForeground(lineColor);
						gc.drawLine(//
								devXPrev,
								devYSegment,
								devXSegment,
								devYSegment);
					}

//					chartLabel.paintedX1 = devXPrev;
//					chartLabel.paintedX2 = devXSegment;
//					chartLabel.paintedY1 = devYPrev;
//					chartLabel.paintedY2 = devYSegment;
//					chartLabel.paintedRGB = upDownColor.getRGB();
				}

				if (_isShowSegmenterValues && isShowValueText) {

					final int devXText = devXPrev + segmentWidth / 2 - textWidth / 2;

					final boolean isValueUp = graphYValue > 0;
					int devYText;
					boolean isDrawAbove;
					boolean isToggleAboveBelow = false;

					if (segmentConfig.canHaveNegativeValues) {

						isDrawAbove = isValueUp;

					} else {

						// toggle above/below segment line
						isDrawAbove = toggleAboveBelow = !toggleAboveBelow;
						isToggleAboveBelow = true;
					}

					if (isDrawAbove || isToggleAboveBelow) {

						// draw above segment line
						devYText = devYSegment - textHeight;

					} else {

						// draw below segment line
						devYText = devYSegment + 2;
					}

					/*
					 * Ensure the value text do not overlap, if possible :-)
					 */
					final Rectangle textRect = new Rectangle(devXText, devYText, textWidth, textHeight);
					final Rectangle validRect = posChecker.getValidRect(textRect, isValueUp, textHeight, valueText);

					// don't draw over the graph borders
					if (validRect != null && validRect.y > devYTop && validRect.y + textHeight < devYBottom) {

						// keep current valid rectangle
						posChecker.setupNext(validRect, isValueUp);

						gc.drawText(//
								valueText,
								devXText,
								validRect.y,
								true);

//						/*
//						 * Debugging
//						 */
//						gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
//						gc.setLineAttributes(defaultLineAttributes);
//						gc.drawRectangle(validRect);
					}
				}

				// advance to the next point
				devXPrev = devXSegment;
				devYPrev = devYSegment;
			}
		}
		lineColor.dispose();
		textColor.dispose();

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

		if (_graphRect == null) {

			// this happened, propably when not initialized
			return null;
		}

		ChartLabel hoveredLabel;

		if (_graphRect.contains(mouseEvent.devXMouse, mouseEvent.devYMouse)) {

			// mouse is hovering the graph area

			hoveredLabel = getHoveredLabel_10(mouseEvent.devXMouse, mouseEvent.devYMouse);

		} else {
			hoveredLabel = null;
		}

		return hoveredLabel;
	}

	private ChartLabel getHoveredLabel_10(final int devXMouse, final int devYMouse) {

//		for (final ChartLabel chartLabel : _chartLabels) {
//
//			final Rectangle hoveredLabel = chartLabel.hoveredLabel;
//
//			if (hoveredLabel != null && hoveredLabel.contains(devXMouse, devYMouse)) {
//
//				// label is hit
//				return chartLabel;
//			}
//		}

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

	void setSmallHiddenValuesProperties(final boolean isHideSmallValues, final int hiddenValueSize) {

		_isHideSmallValues = isHideSmallValues;
		_hiddenValueSize = hiddenValueSize / 10.0 / 100.0;
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
