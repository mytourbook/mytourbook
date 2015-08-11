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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * This layer displays the average values for a segment.
 */
public class ChartLayerSegmentValue implements IChartLayer {

	private RGB					textColorRGB	= new RGB(0x20, 0x20, 0x20);

	private TourChart			_tourChart;
	private TourData			_tourData;
	private double[]			_xDataSerie;

	private int					_stackedValues;
	private boolean				_isShowSegmenterValues;

	private final NumberFormat	_nf1			= NumberFormat.getNumberInstance();
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

		// check segment values
		if (segmentConfig.segmentDataSerie == null) {
			return;
		}

		final ValueOverlapChecker valueCheckerUp = new ValueOverlapChecker(_stackedValues);
		final ValueOverlapChecker valueCheckerDown = new ValueOverlapChecker(_stackedValues);

		final float[] segmentValues = segmentConfig.segmentDataSerie;
		final IValueLabelProvider segmentLabelProvider = segmentConfig.labelProvider;

		Rectangle[] paintedValues = null;
		if (_isShowSegmenterValues) {
			paintedValues = segmentConfig.paintedValues = new Rectangle[segmentSerieSize];
		}

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

		// setup font
		final Font fontBackup = gc.getFont();
		gc.setFont(_tourChart.getValueFont());

		gc.setLineStyle(SWT.LINE_SOLID);

		// do not draw over the graph area
		gc.setClipping(0, devYTop, graphWidth, devYBottom - devYTop);

		final Color lineColor = new Color(display, segmentConfig.segmentLineColor);
//		final Color textColor = new Color(display, textColorRGB);
		final Color textColor = new Color(display, segmentConfig.segmentLineColor);
		{
			Point previousValue = null;

			for (int segmentIndex = 0; segmentIndex < segmentSerieSize; segmentIndex++) {

				// get current value
				final int serieIndex = segmentSerie[segmentIndex];
				final int devXValue = (int) (_xDataSerie[serieIndex] * scaleX - devGraphImageXOffset);

				// optimize performance
				if (devXValue < 0 || devXValue > graphWidth) {

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
				final int devYValue = devYBottom - devYGraph;

				/*
				 * Connect two segments with a line
				 */
				if (previousValue == null) {
					previousValue = new Point(devXValue, devYValue);
					continue;
				}

				String valueText;
				if (segmentLabelProvider != null) {

					// get value text from a label provider
					valueText = segmentLabelProvider.getLabel(graphYValue);

				} else {

					if (graphYValue < 0.0 || graphYValue > 0.0) {
						valueText = _nf1.format(graphYValue);
					} else {
						// hide digits
						valueText = UI.ZERO;
					}
				}

				final Point textExtent = gc.textExtent(valueText);

				final int textWidth = textExtent.x;
				final int textHeight = textExtent.y;

				final int devXPrev = previousValue.x;

				gc.setForeground(lineColor);
				gc.drawLine(devXPrev, devYValue, devXValue, devYValue);

				if (_isShowSegmenterValues) {

					final int segmentWidth = devXValue - devXPrev;
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
						devYText = devYValue - textHeight;

					} else {

						// draw below segment line
						devYText = devYValue + 2;
					}

					/*
					 * Ensure the value texts do not overlap, if possible :-)
					 */
					Rectangle textRect = new Rectangle(devXText, devYText, textWidth, textHeight);
					boolean isDrawValue = true;

					if (isDrawAbove || isToggleAboveBelow) {
						if (valueCheckerUp.intersectsWithValues(textRect)) {
							devYText = valueCheckerUp.getPreviousValue().y - textHeight;
						}
						if (valueCheckerUp.intersectsNoValues(textRect)) {
							isDrawValue = false;
						}

					} else {
						if (valueCheckerDown.intersectsWithValues(textRect)) {
							devYText = valueCheckerDown.getPreviousValue().y + textHeight;
						}
						if (valueCheckerDown.intersectsNoValues(textRect)) {
							isDrawValue = false;
						}
					}

					if (devYText < devYTop || devYText + textHeight > devYBottom) {
						isDrawValue = false;
					}

					if (isDrawValue) {

						gc.setForeground(textColor);
						gc.drawText(//
								valueText,
								devXText,
								devYText,
								true);

						// keep current up/down rectangles
						final int margin = 0;
						textRect = new Rectangle(//
								devXText - margin,
								devYText - margin,
								textWidth + 2 * margin,
								textHeight + 2 * margin);

						if (isDrawAbove || isToggleAboveBelow) {
							valueCheckerUp.setupNext(textRect);
						} else {
							valueCheckerDown.setupNext(textRect);
						}

						// keep painted position to detect hovered values
						paintedValues[segmentIndex] = textRect;
					}
				}

				// advance to the next point
				previousValue.x = devXValue;
				previousValue.y = devYValue;
			}
		}
		lineColor.dispose();
		textColor.dispose();

		// reset clipping
		gc.setClipping((Rectangle) null);

		// restore font
		gc.setFont(fontBackup);
	}

	void setIsShowSegmenterValues(final boolean isShowSegmenterValues) {
		_isShowSegmenterValues = isShowSegmenterValues;
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
