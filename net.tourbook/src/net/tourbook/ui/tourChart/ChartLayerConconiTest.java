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
package net.tourbook.ui.tourChart;

import java.util.Arrays;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.GraphDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.math.LinearRegression;
import net.tourbook.tour.TourManager;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class ChartLayerConconiTest implements IChartLayer {

	private double	_scaleX;
	private double	_scaleY;
	private int		_devYBottom;
	private float	_graphYBottom;

	private double	_scalingFactor;
	private double	_scaleXextended;
	private boolean	_isExtendedScaling;

	public void draw(final GC gc, final GraphDrawingData drawingData, final Chart chart) {

		final ChartDataYSerie yData = drawingData.getYData();

		ConconiData conconiData;
		final Object customData = yData.getCustomData(TourManager.CUSTOM_DATA_CONCONI_TEST);
		if (customData instanceof ConconiData) {
			conconiData = (ConconiData) customData;
		} else {
			return;
		}

		// get the chart values
		final ChartDataXSerie xData = drawingData.getXData();
		_scaleX = drawingData.getScaleX();
		_scaleY = drawingData.getScaleY();

		final double devGraphWidth = drawingData.devVirtualGraphWidth;
		final double scalingMaxValue = xData.getScalingMaxValue();
		_scalingFactor = xData.getScalingFactor();
		_isExtendedScaling = _scalingFactor != 1.0;
		_scaleXextended = ((devGraphWidth - 1) / Math.pow(scalingMaxValue, _scalingFactor));

		// get the top/bottom of the graph
		_devYBottom = drawingData.getDevYBottom();
		_graphYBottom = drawingData.getGraphYBottom();
		final int devYTop = _devYBottom - drawingData.devGraphHeight;

		/*
		 * draw regression lines
		 */
		final double[] maxXValues = conconiData.maxXValues.toArray();
		final double[] maxYValues = conconiData.maxYValues.toArray();
		final int deflexionIndex = conconiData.selectedDeflection;

		// check that at least 2  values are available
		if (maxXValues.length < 2) {
			return;
		}

		final int lastIndex = maxXValues.length - 1;

		int deflexionIndexAdjusted = deflexionIndex + 1;

		final Display display = Display.getCurrent();
		final Color color1 = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		final Color color2 = display.getSystemColor(SWT.COLOR_BLUE);

		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(3);
		gc.setAntialias(SWT.ON);
		gc.setClipping(0, devYTop, gc.getClipping().width, _devYBottom - devYTop);

		/*
		 * draw left regression line
		 */
		double[] linRegXValues = Arrays.copyOfRange(maxXValues, 0, deflexionIndexAdjusted);
		double[] linRegYValues = Arrays.copyOfRange(maxYValues, 0, deflexionIndexAdjusted);

		draw10LineLinearRegression(gc, //
				linRegXValues,
				linRegYValues,
				color2);
		draw20Point(gc, //
				linRegXValues[0],
				linRegYValues[0],
				color2);

		/*
		 * draw right regression line
		 */
		deflexionIndexAdjusted = deflexionIndex >= lastIndex ? lastIndex : deflexionIndex;

		linRegXValues = Arrays.copyOfRange(maxXValues, deflexionIndexAdjusted, lastIndex + 1);
		linRegYValues = Arrays.copyOfRange(maxYValues, deflexionIndexAdjusted, lastIndex + 1);

		draw10LineLinearRegression(gc, //
				linRegXValues,
				linRegYValues,
				color1);
		draw20Point(gc, //
				linRegXValues[linRegXValues.length - 1],
				linRegYValues[linRegXValues.length - 1],
				color1);
		draw30DeflectionPoint(gc, //
				linRegXValues[0],
				linRegYValues[0],
				color1,
				color2);

		// reset clipping/antialias
		gc.setClipping((Rectangle) null);
		gc.setLineWidth(1);
		gc.setAntialias(SWT.OFF);
		gc.setAlpha(0xff);
	}

	private void draw10LineLinearRegression(final GC gc,
											final double[] maxXValues,
											final double[] maxYValues,
											final Color color) {

		if (maxXValues.length < 2) {
			return;
		}

		final LinearRegression linReg = new LinearRegression(maxXValues, maxYValues);

		final int extendedGraph = 0;

		final double graphXStart = maxXValues[0] - extendedGraph;
		final double graphXEnd = maxXValues[maxXValues.length - 1] + extendedGraph;

		final double graphYStart = linReg.calculateY(graphXStart);
		final double graphYEnd = linReg.calculateY(graphXEnd);

		int devXStart;
		int devXEnd;
		if (_isExtendedScaling) {
			devXStart = (int) ((Math.pow(graphXStart, _scalingFactor)) * _scaleXextended);
			devXEnd = (int) ((Math.pow(graphXEnd, _scalingFactor)) * _scaleXextended);
		} else {
			devXStart = (int) (graphXStart * _scaleX);
			devXEnd = (int) (graphXEnd * _scaleX);
		}

		final int devYStart = _devYBottom - (int) ((graphYStart - _graphYBottom) * _scaleY);
		final int devYEnd = _devYBottom - (int) ((graphYEnd - _graphYBottom) * _scaleY);

		gc.setAlpha(0xb0);

		gc.setForeground(color);
		gc.drawLine(devXStart, devYStart, devXEnd, devYEnd);
	}

	private void draw20Point(final GC gc, final double graphX, final double graphY, final Color color) {

		final int size = 20;//9;
		final int size2 = size / 2;

		int devX;
		if (_isExtendedScaling) {
			devX = (int) ((Math.pow(graphX, _scalingFactor)) * _scaleXextended);
		} else {
			devX = (int) (graphX * _scaleX);
		}

		final int devY = _devYBottom - (int) ((graphY - _graphYBottom) * _scaleY);

		gc.setAlpha(0x60);

		gc.setBackground(color);
		gc.fillOval(devX - size2, devY - size2, size, size);
//		gc.fillRectangle(devX - size2, devY - size2, size, size);
	}

	private void draw30DeflectionPoint(	final GC gc,
										final double graphX,
										final double graphY,
										final Color color1,
										final Color color2) {

		final int size = 20;//9;
		final int size2 = size / 2;

		int devX;
		if (_isExtendedScaling) {
			devX = (int) ((Math.pow(graphX, _scalingFactor)) * _scaleXextended);
		} else {
			devX = (int) (graphX * _scaleX);
		}
		final int devY = _devYBottom - (int) ((graphY - _graphYBottom) * _scaleY);

		gc.setAlpha(0x60);

		gc.setBackground(color2);
//		gc.fillRectangle(devX - size2, devY - size2, size2, size);
		gc.fillArc(devX - size2, devY - size2, size, size, 90, 180);

		gc.setBackground(color1);
//		gc.fillRectangle(devX, devY - size2, size2, size);
		gc.fillArc(devX - size2, devY - size2, size, size, -90, 180);
	}

}
