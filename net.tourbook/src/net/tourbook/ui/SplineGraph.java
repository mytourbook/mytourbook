/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import net.tourbook.math.CubicSpline;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class SplineGraph extends Canvas {

	private double[][]	fSplinePoints;

	private boolean		fIsComputeGraph;

	private int[]		fDevYSplineValues;

	public SplineGraph(final Composite parent, final int style) {

		super(parent, style | SWT.DOUBLE_BUFFERED);

		addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent e) {
				onPaint(e);
			}
		});

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				onDispose(e);
			}
		});

		addControlListener(new ControlListener() {

			public void controlMoved(final ControlEvent e) {}

			public void controlResized(final ControlEvent e) {
				fIsComputeGraph = true;
				redraw();
			}
		});
	}

//	private void computeSplineValues() {
//
//		final CubicSpline cubicSpline = new CubicSpline(fSplinePoints[0], fSplinePoints[1]);
//		// TODO Auto-generated method stub
//
//		final Rectangle clientArea = getClientArea();
//
//	}

	private void onDispose(final DisposeEvent e) {

	}

	private void onPaint(final PaintEvent e) {

		// check if 2 points are available
		if (fSplinePoints == null || fSplinePoints.length == 0 || fSplinePoints[0].length < 2) {
			return;
		}

		final Display display = Display.getCurrent();
		final GC gc = e.gc;
		final Rectangle clientArea = getClientArea();

		final double[] graphXValues = fSplinePoints[0];
		final double[] graphYValues = fSplinePoints[1];

		int graphXMax;
		int graphXMin = graphXMax = (int) graphXValues[0];
		int graphYMax;
		int graphYMin = graphYMax = (int) graphYValues[0];

		// get x/y min/max values
		for (int valueIndex = 0; valueIndex < graphXValues.length; valueIndex++) {

			final int graphX = (int) graphXValues[valueIndex];
			final int graphY = (int) graphYValues[valueIndex];

			graphXMin = graphX < graphXMin ? graphX : graphXMin;
			graphXMax = graphX > graphXMax ? graphX : graphXMax;

			graphYMin = graphY < graphYMin ? graphY : graphYMin;
			graphYMax = graphY > graphYMax ? graphY : graphYMax;
		}

		// enforce minimum size
		if (graphXMin == graphXMax) {
			graphXMin--;
			graphXMax++;
		}
		if (graphYMin == graphYMax) {
			graphYMin--;
			graphYMax++;
		}

		final int devWidth = clientArea.width;
		final int devHeight = clientArea.height;

		final int devMargin = 2;
		final int devMargin2 = devMargin / 2;

		final int graphYMaxAbs = (graphYMax < 0) ? -graphYMax : graphYMax;
		final int graphYMinAbs = (graphYMin < 0) ? -graphYMin : graphYMin;
		final int graphYMaxAbsAbs = Math.max(graphYMinAbs, graphYMaxAbs);

		// create scale with a border of 5 pixel at each side
		final float scaleX = (float) (devWidth - devMargin) / (graphXMax - graphXMin);
		final float scaleY = (float) (devHeight - devMargin) / (2 * graphYMaxAbsAbs);

		final int devX0 = devMargin2 - (int) (graphXMin * scaleX);
		final int devY0 = (int) (graphYMaxAbsAbs * scaleY) + devMargin2;

		// compute splines
		if (fIsComputeGraph || fDevYSplineValues == null) {

			fDevYSplineValues = new int[devWidth];

			final double graphXStart = graphXValues[0];
			final double graphXEnd = graphXValues[graphXValues.length - 1];
			final double graphScale = (graphXEnd - graphXStart) / devWidth;

			final CubicSpline cubicSpline = new CubicSpline(graphXValues, graphYValues);

			for (int devIndex = 0; devIndex < devWidth; devIndex++) {

				final double graphX = graphXStart + (devIndex * graphScale);
				final double graphY = cubicSpline.interpolate(graphX);

				final int devY = (int) (graphY * scaleY);
				fDevYSplineValues[devIndex] = devY0 - devY;
			}

			fIsComputeGraph = false;
		}

		/*
		 * paint background
		 */
		gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		gc.fillRectangle(clientArea);

		gc.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		gc.drawRectangle(0, 0, clientArea.width - 1, clientArea.height - 1);

		/*
		 * paint axis
		 */
		gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));

		// x-axis
		gc.drawLine(devMargin2, devY0, devWidth - devMargin2, devY0);

		// y-axis
		gc.drawLine(devX0, devMargin2, devX0, devHeight - devMargin2);

		/*
		 * paint splines
		 */
		gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		
		int devYPrev = fDevYSplineValues[0];
		for (int devXIndex = 1; devXIndex < fDevYSplineValues.length; devXIndex++) {
			final int devY = fDevYSplineValues[devXIndex];
			
//			gc.drawPoint(devXIndex, devY);

			gc.drawLine(devXIndex - 1, devYPrev, devXIndex, devY);
			devYPrev = devY;
		}

		/*
		 * paint spline points
		 */
		gc.setBackground(display.getSystemColor(SWT.COLOR_RED));
		for (int valueIndex = 0; valueIndex < graphXValues.length; valueIndex++) {

			int devX = (int) (graphXValues[valueIndex] * scaleX);
			int devY = (int) (graphYValues[valueIndex] * scaleY);

			devX = devX0 + devX;
			devY = devY0 - devY;

			gc.fillRectangle(devX - 1, devY - 1, 3, 3);
		}

	}

	public void updateValues(final double[][] splinePoints) {

		fSplinePoints = splinePoints;

		fIsComputeGraph = true;

		redraw();
	}

}
