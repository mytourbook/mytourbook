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
public class ChartSegmentValueLayer implements IChartLayer {

	private RGB					lineColorRGB	= new RGB(255, 0, 0);
	private RGB					textColorRGB	= new RGB(0x20, 0x20, 0x20);

	private TourChart			_tourChart;
	private TourData			_tourData;
	private double[]			_xDataSerie;

	private boolean				_isShowSegmenterValues;

	private final NumberFormat	_nf1			= NumberFormat.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	public ChartSegmentValueLayer(final TourChart tourChart) {

		_tourChart = tourChart;
	}

	/**
	 * Draws the marker(s) for the current graph config
	 * 
	 * @param gc
	 * @param drawingData
	 * @param chartComponents
	 */
	@Override
	public void draw(	final GC gc,
						final GraphDrawingData drawingData,
						final Chart chart,
						final PixelConverter pixelConverter) {

		final int[] segmentSerie = _tourData.segmentSerieIndex;

		if (segmentSerie == null) {
			return;
		}

		final ChartDataYSerie yData = drawingData.getYData();

		final Object segmentConfigObject = yData.getCustomData(TourManager.CUSTOM_DATA_SEGMENT_VALUES);
		if (!(segmentConfigObject instanceof SegmentConfig)) {
			return;
		}

		final SegmentConfig segmentConfig = (SegmentConfig) segmentConfigObject;

		// check segment values
		if (segmentConfig.segmentDataSerie == null) {
			return;
		}

		final float[] segmentValues = segmentConfig.segmentDataSerie;
		final IValueLabelProvider segmentLabelProvider = segmentConfig.labelProvider;

		final Display display = Display.getCurrent();
		Rectangle prevUpTextRect = null;
		Rectangle prevDownTextRect = null;
		boolean toggleAboveBelow = false;

		final int devYTop = drawingData.getDevYTop();
		final int devYBottom = drawingData.getDevYBottom();
		final long devGraphImageXOffset = chart.getXXDevViewPortLeftBorder();

		final float graphYBottom = drawingData.getGraphYBottom();

		final int valueDivisor = yData.getValueDivisor();
		final double scaleX = drawingData.getScaleX();
		final double scaleY = drawingData.getScaleY();

		// setup font
		final Font fontBackup = gc.getFont();
		gc.setFont(_tourChart.getValueFont());

		gc.setLineStyle(SWT.LINE_SOLID);

		final Color lineColor = new Color(display, lineColorRGB);
		final Color textColor = new Color(display, textColorRGB);
		{
			Point previousValue = null;

			for (int segmentIndex = 0; segmentIndex < segmentSerie.length; segmentIndex++) {

				final int serieIndex = segmentSerie[segmentIndex];
				final int devXValue = (int) (_xDataSerie[serieIndex] * scaleX - devGraphImageXOffset);

				final float graphYValue = segmentValues[segmentIndex] * valueDivisor;
				final int devYGraph = (int) (scaleY * (graphYValue - graphYBottom));
				int devYValue = devYBottom - devYGraph;

				// don't draw over the graph borders
				if (devYValue > devYBottom) {
					devYValue = devYBottom;
				}
				if (devYValue < devYTop) {
					devYValue = devYTop;
				}

				/*
				 * Connect two segments with a line
				 */
				if (previousValue == null) {
					previousValue = new Point(devXValue, devYValue);
					continue;
				}

				String valueText;
				if (segmentLabelProvider != null) {
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

					if (segmentConfig.canHaveNegativeValues) {

						isDrawAbove = isValueUp;

					} else {

						// toggle above/below segment line
						isDrawAbove = toggleAboveBelow = !toggleAboveBelow;
					}

					if (isDrawAbove) {

						// draw above segment line
						devYText = (int) (devYValue - 1.5 * textHeight);

					} else {

						// draw below segment line
						devYText = (int) (devYValue + 0.5 * textHeight);
					}

					/*
					 * Ensure the value texts do not overlap, if possible :-)
					 */
					Rectangle textRect = new Rectangle(devXText, devYText, textWidth, textHeight);

					if (isDrawAbove) {
						if (prevUpTextRect != null && prevUpTextRect.intersects(textRect)) {
							devYText = prevUpTextRect.y - textHeight;
						}
					} else {
						if (prevDownTextRect != null && prevDownTextRect.intersects(textRect)) {
							devYText = prevDownTextRect.y + textHeight;
						}
					}

					// don't draw over the graph borders
					if (devYText < devYTop) {
						devYText = devYTop;
					}
					if (devYText + textHeight > devYBottom) {
						devYText = devYBottom - textHeight;
					}

					gc.setForeground(textColor);
					gc.drawText(//
							valueText,
							devXText,
							devYText,
							true);

					// keep current up/down rectangles
					final int margin = 1;
					textRect = new Rectangle(//
							devXText - margin,
							devYText - margin,
							textWidth + 2 * margin,
							textHeight + 2 * margin);

					if (isDrawAbove) {
						prevUpTextRect = textRect;
					} else {
						prevDownTextRect = textRect;
					}
				}

				// advance to the next point
				previousValue.x = devXValue;
				previousValue.y = devYValue;
			}
		}
		lineColor.dispose();
		textColor.dispose();

		// restore font
		gc.setFont(fontBackup);
	}

	void setIsShowSegmenterValues(final boolean isShowSegmenterValues) {
		_isShowSegmenterValues = isShowSegmenterValues;
	}

	public void setLineColor(final RGB lineColor) {
		this.lineColorRGB = lineColor;
	}

	public void setTourData(final TourData tourData) {
		_tourData = tourData;
	}

	public void setXDataSerie(final double[] dataSerie) {
		_xDataSerie = dataSerie;
	}
}
