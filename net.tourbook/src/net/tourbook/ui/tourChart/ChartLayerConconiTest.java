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
import net.tourbook.chart.ChartDrawingData;
import net.tourbook.chart.IChartLayer;
import net.tourbook.math.LinearRegression;
import net.tourbook.tour.TourManager;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class ChartLayerConconiTest implements IChartLayer {

	public void draw(final GC gc, final ChartDrawingData drawingData, final Chart chart) {

		final ChartDataXSerie xData = drawingData.getXData();
		final ChartDataYSerie yData = drawingData.getYData();

		ConconiData conconiData;
		final Object customData = yData.getCustomData(TourManager.CUSTOM_DATA_CONCONI_TEST);
		if (customData instanceof ConconiData) {
			conconiData = (ConconiData) customData;
		} else {
			return;
		}

		// get the chart values
		final float scaleX = drawingData.getScaleX();
		final float scaleY = drawingData.getScaleY();
		final int graphYBottom = drawingData.getGraphYBottom();

		// get the horizontal offset for the graph
		final int devGraphImageXOffset = chart.getDevGraphImageXOffset();
		final int graphValueOffset = (int) (Math.max(0, devGraphImageXOffset) / scaleX);

		// get the top/bottom of the graph
		final int devYBottom = drawingData.getDevYBottom();
		final int devYTop = devYBottom - drawingData.devGraphHeight;

		/*
		 * draw regression lines
		 */
		final double[] maxXValues = conconiData.maxXValues.toArray();
		final double[] maxYValues = conconiData.maxYValues.toArray();
		final int deflexionIndex = conconiData.selectedDefletion;

		// check that at least 2  values are available
		if (maxXValues.length < 2) {
			return;
		}

		final int lastIndex = maxXValues.length - 1;

//		int deflexionIndexAdjusted = deflexionIndex == 0 ? 1 : deflexionIndex;
		int deflexionIndexAdjusted = deflexionIndex + 1;

		final Display display = Display.getCurrent();
		final Color color1 = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		final Color color2 = display.getSystemColor(SWT.COLOR_BLUE);

		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(3);
		gc.setAntialias(SWT.ON);
		gc.setAlpha(0xa0);
		gc.setClipping(0, devYTop, gc.getClipping().width, devYBottom - devYTop);

		/*
		 * draw left regression line
		 */
		double[] linRegXValues = Arrays.copyOfRange(maxXValues, 0, deflexionIndexAdjusted);
		double[] linRegYValues = Arrays.copyOfRange(maxYValues, 0, deflexionIndexAdjusted);
		draw10LineLinearRegression(
				gc,
				scaleX,
				scaleY,
				graphYBottom,
				graphValueOffset,
				devYBottom,
				linRegXValues,
				linRegYValues,
				xData,
				color2);

		draw20Point(
				gc,
				linRegXValues[0],
				linRegYValues[0],
				scaleX,
				scaleY,
				graphYBottom,
				graphValueOffset,
				devYBottom,
				color2);

		/*
		 * draw right regression line
		 */
		deflexionIndexAdjusted = deflexionIndex >= lastIndex ? lastIndex : deflexionIndex;

		linRegXValues = Arrays.copyOfRange(maxXValues, deflexionIndexAdjusted, lastIndex + 1);
		linRegYValues = Arrays.copyOfRange(maxYValues, deflexionIndexAdjusted, lastIndex + 1);

		draw10LineLinearRegression(
				gc,
				scaleX,
				scaleY,
				graphYBottom,
				graphValueOffset,
				devYBottom,
				linRegXValues,
				linRegYValues,
				xData,
				color1);

		draw20Point(
				gc,
				linRegXValues[linRegXValues.length - 1],
				linRegYValues[linRegXValues.length - 1],
				scaleX,
				scaleY,
				graphYBottom,
				graphValueOffset,
				devYBottom,
				color1);

		draw30DeflectionPoint(
				gc,
				linRegXValues[0],
				linRegYValues[0],
				scaleX,
				scaleY,
				graphYBottom,
				graphValueOffset,
				devYBottom,
				color1,
				color2);

		// reset clipping/antialias
		gc.setClipping((Rectangle) null);
		gc.setLineWidth(1);
		gc.setAntialias(SWT.OFF);
		gc.setAlpha(0xff);
	}

	private void draw10LineLinearRegression(final GC gc,
											final float scaleX,
											final float scaleY,
											final int graphYBottom,
											final int graphValueOffset,
											final int devYBottom,
											final double[] maxXValues,
											final double[] maxYValues,
											final ChartDataXSerie xData,
											final Color color) {

		if (maxXValues.length < 2) {
			return;
		}

		final LinearRegression linReg = new LinearRegression(maxXValues, maxYValues);

		final double graphXStart = maxXValues[0];
		final double graphXEnd = maxXValues[maxXValues.length - 1];

		final double graphYStart = linReg.calculateY(graphXStart);
		final double graphYEnd = linReg.calculateY(graphXEnd);

		final int devXStart = (int) ((graphXStart - graphValueOffset) * scaleX);
		final int devYStart = devYBottom - ((int) ((graphYStart - graphYBottom) * scaleY));

		final int devXEnd = (int) ((graphXEnd - graphValueOffset) * scaleX);
		final int devYEnd = devYBottom - ((int) ((graphYEnd - graphYBottom) * scaleY));

		gc.setForeground(color);
		gc.drawLine(devXStart, devYStart, devXEnd, devYEnd);
	}

	private void draw20Point(	final GC gc,
								final double graphX,
								final double graphY,
								final float scaleX,
								final float scaleY,
								final int graphYBottom,
								final int graphValueOffset,
								final int devYBottom,
								final Color color) {

		final int size = 9;
		final int size2 = size / 2;

		final int devX = (int) ((graphX - graphValueOffset) * scaleX);
		final int devY = devYBottom - ((int) ((graphY - graphYBottom) * scaleY));

		gc.setBackground(color);
		gc.fillOval(devX - size2, devY - size2, size, size);
//		gc.fillRectangle(devX - size2, devY - size2, size, size);
	}

	private void draw30DeflectionPoint(	final GC gc,
										final double graphX,
										final double graphY,
										final float scaleX,
										final float scaleY,
										final int graphYBottom,
										final int graphValueOffset,
										final int devYBottom,
										final Color color1,
										final Color color2) {

		final int size = 9;
		final int size2 = size / 2;

		final int devX = (int) ((graphX - graphValueOffset) * scaleX);
		final int devY = devYBottom - ((int) ((graphY - graphYBottom) * scaleY));

		gc.setBackground(color2);
//		gc.fillRectangle(devX - size2, devY - size2, size2, size);
		gc.fillArc(devX - size2, devY - size2, size, size, 90, 180);

		gc.setBackground(color1);
//		gc.fillRectangle(devX, devY - size2, size2, size);
		gc.fillArc(devX - size2, devY - size2, size, size, -90, 180);
	}

}
