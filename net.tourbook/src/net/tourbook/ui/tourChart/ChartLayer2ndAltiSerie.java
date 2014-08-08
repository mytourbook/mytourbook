/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.data.SplineData;
import net.tourbook.data.TourData;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class ChartLayer2ndAltiSerie implements IChartLayer {

	/**
	 * contains tour which is displayed in the chart
	 */
	private TourData				_tourData;
	private double[]				_xDataSerie;
	private TourChartConfiguration	_tourChartConfig;
	private SplineData				_splineData;

	private Rectangle[]				_spPointRects;

	private double					_graphXValueOffset;
	private long					_xxDevViewPortLeftBorder;
	private int						_devY0Spline;
	private double					_scaleX;
	private double					_scaleY;

	private final NumberFormat		_nf3	= NumberFormat.getNumberInstance();
	{
		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);
	}

	public ChartLayer2ndAltiSerie(	final TourData tourData,
									final double[] xDataSerie,
									final TourChartConfiguration tourChartConfig,
									final SplineData splineData) {

		_tourData = tourData;
		_tourChartConfig = tourChartConfig;
		_splineData = splineData;

		// x-data serie contains the time or distance distance data serie
		_xDataSerie = xDataSerie;
	}

	public void draw(	final GC gc,
						final GraphDrawingData drawingData,
						final Chart chart,
						final PixelConverter pixelConverter) {

		final double[] xValues = _xDataSerie;

		final float[] yValues2ndSerie = _tourData.dataSerie2ndAlti;
		final float[] yDiffTo2ndSerie = _tourData.dataSerieDiffTo2ndAlti;
		final float[] yAdjustedSerie = _tourData.dataSerieAdjustedAlti;

		final boolean is2ndYValues = yValues2ndSerie != null;
		final boolean isDiffValues = yDiffTo2ndSerie != null;
		final boolean isAdjustedValues = yAdjustedSerie != null;

		final boolean isPointInGraph = _splineData != null && _splineData.serieIndex != null;

		if (xValues == null || xValues.length == 0 /*
													 * || yValues2ndSerie == null ||
													 * yValues2ndSerie.length == 0
													 */) {
			return;
		}

		_scaleX = drawingData.getScaleX();
		_scaleY = drawingData.getScaleY();

		// get the horizontal offset for the graph
		_xxDevViewPortLeftBorder = chart.getXXDevViewPortLeftBorder();
		_graphXValueOffset = (Math.max(0, _xxDevViewPortLeftBorder) / _scaleX);

		final Display display = Display.getCurrent();

		final Path path2ndSerie = new Path(display);
		final Path pathValueDiff = new Path(display);
		final Path pathAdjustValue = new Path(display);

		final float graphYBottom = drawingData.getGraphYBottom();

		final int devGraphHeight = drawingData.devGraphHeight;
		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = devYBottom - devGraphHeight;

		// write spline into the middle of the chart
		_devY0Spline = devYBottom - devGraphHeight / 2;

		/*
		 * convert all diff values into positive values
		 */
		float diffValues[] = null;
		double scaleValueDiff = _scaleY;
		if (isDiffValues) {

			int valueIndex = 0;
			float maxValueDiff = 0;

			diffValues = new float[yDiffTo2ndSerie.length];
			for (float valueDiff : yDiffTo2ndSerie) {
				diffValues[valueIndex++] = valueDiff = (valueDiff < 0) ? -valueDiff : valueDiff;
				maxValueDiff = (maxValueDiff >= valueDiff) ? maxValueDiff : valueDiff;
			}

			// set value diff scaling
			if (_tourChartConfig.isRelativeValueDiffScaling) {
				scaleValueDiff = maxValueDiff == 0 ? _scaleY : (float) devGraphHeight / 2 / maxValueDiff;
			}
		}

		// position for the x-axis line in the graph
		final float devY0 = (float) (devYBottom + (_scaleY * graphYBottom));

		final int startIndex = 0;
		final int endIndex = xValues.length;

		final int devChartWidth = gc.getClipping().width;
		final Rectangle graphRect = new Rectangle(0, devYTop, devChartWidth, devGraphHeight);

		// get initial dev X
		double graphXValue = xValues[startIndex] - _graphXValueOffset;
		int devPrevXInt = (int) (_scaleX * graphXValue);

		float graphYValue2nd;
		if (is2ndYValues) {
			graphYValue2nd = yValues2ndSerie[startIndex];
		}

		/*
		 * create paths
		 */
		for (int xValueIndex = startIndex; xValueIndex < endIndex; xValueIndex++) {

			// make sure the x-index is not higher than the yValues length
//			if (xValueIndex >= yValues2ndSerie.length) {
//				return;
//			}

			graphXValue = xValues[xValueIndex] - _graphXValueOffset;
			final float devX = (float) (_scaleX * graphXValue);
			final int devXInt = (int) devX;

			/*
			 * draw adjusted value graph
			 */
			if (isAdjustedValues) {

				final float devYAdjustedValue = (float) (_scaleY * yAdjustedSerie[xValueIndex]);
				final float devYAdjusted = devY0 - devYAdjustedValue;

				if (xValueIndex == startIndex) {

					// move to the first point
					pathAdjustValue.moveTo(0, devYBottom);
					pathAdjustValue.lineTo(devX, devYAdjusted);
				}

				// draw line to the next point
				if (devXInt != devPrevXInt) {
					pathAdjustValue.lineTo(devX, devYAdjusted);
				}

				if (xValueIndex == endIndex - 1) {

					/*
					 * this is the last point, draw the line to the x-axis and the start of the
					 * chart
					 */
					pathAdjustValue.lineTo(devX, devYBottom);
				}
			}

			/*
			 * draw value graph
			 */
			if (is2ndYValues) {

				graphYValue2nd = yValues2ndSerie[xValueIndex];
				final float devYValue2nd = (float) (_scaleY * graphYValue2nd);
				final float devY2nd = devY0 - devYValue2nd;

				if (xValueIndex == startIndex) {

					// move to the first point
					path2ndSerie.moveTo(devX, devY2nd);
				}

				// draw line to the next point
				if (devXInt != devPrevXInt) {
					path2ndSerie.lineTo(devX, devY2nd);
				}
			}

			/*
			 * draw diff values
			 */
			if (isDiffValues) {

				final float graphValueDiff = (diffValues[xValueIndex]);
				final float devLayerValueDiff = (float) (graphValueDiff * scaleValueDiff);
				final float devYDiff = devYBottom - devLayerValueDiff;

				if (xValueIndex == startIndex) {

					// move to the first point
					pathValueDiff.moveTo(devX, devYDiff);
				}

				// draw line to the next point
				if (devXInt != devPrevXInt) {
					pathValueDiff.lineTo(devX, devYDiff);
				}
			}

			devPrevXInt = devXInt;
		}

		// draw the line of the graph
		gc.setAntialias(SWT.OFF);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(1);
		gc.setClipping(graphRect);

		/*
		 * paint and fill adjusted value graph
		 */
		if (isAdjustedValues) {

			final Color color1 = new Color(display, new RGB(0xFF, 0x3E, 0x00));
//			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
//			gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

			gc.setForeground(color1);
			gc.setBackground(color1);
			gc.setAlpha(0x80);

			// fill background
			gc.setClipping(pathAdjustValue);
			gc.fillGradientRectangle(0, devYTop, devChartWidth, devGraphHeight, true);
			gc.setClipping(graphRect);

			// draw graph
			gc.drawPath(pathAdjustValue);

			gc.setAlpha(0xff);
			color1.dispose();
		}

		/*
		 * paint value diff graph
		 */
		if (isDiffValues) {

			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
			gc.drawPath(pathValueDiff);
		}

		/*
		 * paint splines
		 */
		final Color splineColor = new Color(display, 0x00, 0xb4, 0xff);
		final float[] ySplineSerie = _tourData.dataSerieSpline;
		if (ySplineSerie != null) {

			gc.setForeground(splineColor);

			int devXPrev = (int) (_scaleX * (xValues[0] - _graphXValueOffset));
			int devYPrev = (int) (_scaleY * ySplineSerie[0]);

			for (int xIndex = 1; xIndex < xValues.length; xIndex++) {

				final double graphX = xValues[xIndex] - _graphXValueOffset;
				final float graphY = ySplineSerie[xIndex];

				final int devX = (int) (_scaleX * graphX);
				final int devY = (int) (_scaleY * graphY);

				if (!(devX == devXPrev && devY == devYPrev)) {
					gc.drawLine(devXPrev, _devY0Spline - devYPrev, devX, _devY0Spline - devY);
				}

				devXPrev = devX;
				devYPrev = devY;
			}
		}

		/*
		 * paint data graph
		 */
		if (is2ndYValues) {
			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			gc.drawPath(path2ndSerie);
		}

		/*
		 * paint spline points
		 */
		final SplineData splineData = _tourData.splineDataPoints;
		if (splineData != null) {

			final int[] graphSerieIndex = _splineData.serieIndex;

			final double[] graphXSplineValues = splineData.graphXValues;
			final double[] graphYSplineValues = splineData.graphYValues;
			final boolean[] isPointMovable = splineData.isPointMovable;

			final int splinePointLength = graphXSplineValues.length;

			_spPointRects = new Rectangle[splinePointLength];

			final int pointSize = 10;
			final int pointSize2 = pointSize / 2;
			final int hitSize = 10;
			final int hitSize2 = hitSize / 2;

			/*
			 * paint static points
			 */
			gc.setBackground(splineColor);
			for (int pointIndex = 0; pointIndex < splinePointLength; pointIndex++) {

				if (isPointMovable[pointIndex]) {
					continue;
				}

				final double graphX = graphXSplineValues[pointIndex] - _graphXValueOffset;
				final double graphY = graphYSplineValues[pointIndex];

				int devPointX = (int) (_scaleX * graphX);
				final int devPointY = (int) (graphY * scaleValueDiff);

				/*
				 * set the last point visible if it's hidden
				 */
				final boolean isPointHidden = devPointX > devChartWidth;
				final boolean isLastSPlinePoint = pointIndex == splinePointLength - 1;
				final boolean isLastSerieIndex = graphSerieIndex[pointIndex] == xValues.length - 1;
				final boolean isRightBorder = _xxDevViewPortLeftBorder + devChartWidth == drawingData.devVirtualGraphWidth;
				if (isPointHidden && isLastSPlinePoint && isLastSerieIndex && isRightBorder) {
					devPointX = devChartWidth;
				}

				final int devX = devPointX - pointSize2;
				final int devY = _devY0Spline - devPointY - pointSize2;

				gc.fillOval(devX, devY, pointSize, pointSize);

				// keep point position
				_spPointRects[pointIndex] = new Rectangle(
						devPointX - hitSize2,
						_devY0Spline - devPointY - hitSize2,
						hitSize,
						hitSize);
			}

			/*
			 * paint movable points
			 */
			gc.setBackground(display.getSystemColor(SWT.COLOR_RED));
			for (int pointIndex = 0; pointIndex < splinePointLength; pointIndex++) {

				if (isPointMovable[pointIndex] == false) {
					continue;
				}

				final double graphSpX = graphXSplineValues[pointIndex] - _graphXValueOffset;
				final double graphSpY = graphYSplineValues[pointIndex];

				final int devPointX = (int) (graphSpX * _scaleX);
				final int devPointY = (int) (graphSpY * scaleValueDiff);

				final int devX = devPointX - pointSize2;
				final int devY = _devY0Spline - devPointY - pointSize2;

				gc.fillOval(devX, devY, pointSize, pointSize);

				// keep point position
				_spPointRects[pointIndex] = new Rectangle(
						devPointX - hitSize2,
						_devY0Spline - devPointY - hitSize2,
						hitSize,
						hitSize);
			}
		}

		/*
		 * paint spline points in the graph
		 */
		if (isPointInGraph && isAdjustedValues) {

			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
			final int[] graphSerieIndex = _splineData.serieIndex;

			for (final int serieIndex : graphSerieIndex) {

				final double graphX = xValues[serieIndex] - _graphXValueOffset;
				final float graphY = yAdjustedSerie[serieIndex];

				final int devX = (int) (_scaleX * graphX);
				final int devY = (int) (devY0 - (_scaleY * graphY));

				gc.fillOval(devX - 2, devY - 2, 5, 5);

				/*
				 * draw altitude
				 */
				final String altiText = _nf3.format(graphY);
				final Point textExtent = gc.textExtent(altiText);
				final int textWidth = textExtent.x;

				int devXText = devX - 2 - textWidth / 2;
				final int devYText = devY - 5 - textExtent.y;

				// ensure the text is visible
				if (devXText < 0) {
					devXText = 2;
				} else if (devXText + textWidth > devChartWidth) {
					devXText = devChartWidth - textWidth - 2;
				}

				gc.drawText(altiText, devXText, devYText, true);
			}
		}

		// dispose resources
		splineColor.dispose();
		path2ndSerie.dispose();
		pathValueDiff.dispose();
		pathAdjustValue.dispose();
	}

	public SplineDrawingData getDrawingData() {

		// create drawing data
		final SplineDrawingData drawingData = new SplineDrawingData();

		drawingData.devY0Spline = _devY0Spline;
		drawingData.scaleX = _scaleX;
		drawingData.scaleY = _scaleY;
		drawingData.devGraphValueXOffset = _xxDevViewPortLeftBorder;

		return drawingData;
	}

	public Rectangle[] getPointHitRectangels() {
		return _spPointRects;
	}
}
