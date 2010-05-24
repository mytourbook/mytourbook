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

import net.tourbook.data.SplineData;
import net.tourbook.math.CubicSpline;
import net.tourbook.util.Util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class SplineGraph extends Canvas {

	private SplineData	fSplineData;

	private int[]		fDevYSplineValues;
	private boolean		fIsComputeGraph;

	private Rectangle[]	fSpPointRects;
	private int			fPointHitIndex;
	private boolean		fIsPointMoved;

	private float		fMovedScaleX;
	private float		fMovedScaleY;
	private float		fMovedXMin;
	private float		fMovedXMax;
	private float		fMovedYMin;
	private float		fMovedYMax;
	private int			fMovedDevX0;
	private int			fMovedDevY0;

	private Cursor		fCursorDragged;

	private double		fGraphStartX;

	private boolean		fIsSynchMinMax;

	private float		fXMinComputed;
	private float		fXMaxComputed;
	private float		fYMinComputed;
	private float		fYMaxComputed;
	private boolean		fIsMinMaxComputed;

	private int			fDevXStartOffset;

	public SplineGraph(final Composite parent, final int style) {

		super(parent, style | SWT.DOUBLE_BUFFERED);

		fCursorDragged = new Cursor(getDisplay(), SWT.CURSOR_SIZEALL);

		addListener();
	}

	private void addListener() {
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

		addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(final MouseEvent e) {
				onMouseMove(e);
			}
		});

		addMouseListener(new MouseListener() {
			public void mouseDoubleClick(final MouseEvent e) {
				onMouseDoubleClick(e);
			}

			public void mouseDown(final MouseEvent e) {
				onMouseDown(e);
			}

			public void mouseUp(final MouseEvent e) {
				onMouseUp(e);
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

	private void onDispose(final DisposeEvent e) {

		fCursorDragged = (Cursor) Util.disposeResource(fCursorDragged);
	}

	private void onMouseDoubleClick(final MouseEvent e) {

	}

	private void onMouseDown(final MouseEvent e) {

		if (fSpPointRects == null) {
			return;
		}

		if (fPointHitIndex != Integer.MIN_VALUE) {

			// mouse is over a point

			fIsPointMoved = true;
		}

	}

	private void onMouseMove(final MouseEvent event) {

		if (fSpPointRects == null) {
			return;
		}

		final int devXMouse = event.x;
		final int devYMouse = event.y;

		if (fIsPointMoved) {

			final int devX = -fMovedDevX0 + devXMouse;
			final int devY = fMovedDevY0 - devYMouse;

			double graphPointX = (double) devX / fMovedScaleX;
			final double graphPointY = (double) devY / fMovedScaleY;

			/*
			 * limit horizontal movement to min/max when it's set
			 */
			final double graphXMin = fSplineData.graphXMinValues[fPointHitIndex];
			final double graphXMax = fSplineData.graphXMaxValues[fPointHitIndex];

			if (Double.isNaN(graphXMin) == false) {
				if (graphPointX < graphXMin) {
					graphPointX = graphXMin;
				}
			}
			if (Double.isNaN(graphXMax) == false) {
				if (graphPointX > graphXMax) {
					graphPointX = graphXMax;
				}
			}
			final double[] xPointValues = fSplineData.graphXValues;
			final double[] yPointValues = fSplineData.graphYValues;

			// update spline points
			xPointValues[fPointHitIndex] = graphPointX;
			yPointValues[fPointHitIndex] = graphPointY;

			fIsComputeGraph = true;
			redraw();

		} else {

			final boolean[] isPointMovable = fSplineData.isPointMovable;

			for (int valueIndex = 0; valueIndex < fSpPointRects.length; valueIndex++) {

				if (isPointMovable[valueIndex]) {

					// keep point dev position
					final Rectangle pointRect = fSpPointRects[valueIndex];

					if (pointRect.contains(devXMouse, devYMouse)) {
						setCursor(fCursorDragged);
						fPointHitIndex = valueIndex;
						return;
					}
				}
			}

			fPointHitIndex = Integer.MIN_VALUE;

			setCursor(null);
		}
	}

	private void onMouseUp(final MouseEvent e) {

		if (fIsPointMoved) {

			fIsPointMoved = false;

			if (fIsSynchMinMax) {
				fIsComputeGraph = true;
			}

			redraw();
		}
	}

	private void onPaint(final PaintEvent e) {

		if (fSplineData == null) {
			return;
		}

		final double[] graphPointsX = fSplineData.graphXValues;
		final double[] graphPointsY = fSplineData.graphYValues;
		final boolean[] isPointMovable = fSplineData.isPointMovable;

		final int pointLength = graphPointsX.length;

		// check if 2 points are available
		if (graphPointsX == null || pointLength < 2) {
			return;
		}

		float graphXMin = fXMinComputed;
		float graphXMax = fXMaxComputed;
		float graphYMin = fYMinComputed;
		float graphYMax = fYMaxComputed;

		if (fIsSynchMinMax || fIsMinMaxComputed == false) {

			fIsMinMaxComputed = true;

			graphXMin = graphXMax = (float) graphPointsX[0];
			graphYMin = graphYMax = (float) graphPointsY[0];

			// get x/y min/max values
			for (int valueIndex = 0; valueIndex < pointLength; valueIndex++) {

				final float graphX = (float) graphPointsX[valueIndex];
				final float graphY = (float) graphPointsY[valueIndex];

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

			fXMinComputed = graphXMin;
			fXMaxComputed = graphXMax;
			fYMinComputed = graphYMin;
			fYMaxComputed = graphYMax;
		}

		if (fIsPointMoved) {
			// use min/max from last none moved graph
			graphXMin = fMovedXMin;
			graphXMax = fMovedXMax;
			graphYMin = fMovedYMin;
			graphYMax = fMovedYMax;
		} else {

			// keep none moved min/max
			fMovedXMin = graphXMin;
			fMovedXMax = graphXMax;
			fMovedYMin = graphYMin;
			fMovedYMax = graphYMax;

			if (fDevXStartOffset == 0) {
				fGraphStartX = graphPointsX[0];
			}
		}

		final Display display = Display.getCurrent();
		final GC gc = e.gc;
		final Rectangle clientArea = getClientArea();

		final int devWidth = clientArea.width;
		final int devHeight = clientArea.height;

		final int devMargin = 10;
		final int devMargin2 = devMargin / 2;

		final int devGraphWidth = devWidth - devMargin;
		final int devGraphHeight = devHeight - devMargin;

		final float graphYMaxAbs = (graphYMax < 0) ? -graphYMax : graphYMax;
		final float graphYMinAbs = (graphYMin < 0) ? -graphYMin : graphYMin;
		final float graphYMaxAbsAbs = Math.max(graphYMinAbs, graphYMaxAbs);

		float scaleX, scaleY;

		scaleX = devGraphWidth / (graphXMax - graphXMin);
		scaleY = devGraphHeight / (2 * graphYMaxAbsAbs);

		final int devX0 = devMargin2 - (int) (graphXMin * scaleX);
		final int devY0 = (int) (graphYMaxAbsAbs * scaleY) + devMargin2;

		fMovedScaleX = scaleX;
		fMovedScaleY = scaleY;
		fMovedDevX0 = devX0;
		fMovedDevY0 = devY0;

		// compute splines
		if (fIsComputeGraph || fDevYSplineValues == null) {

			fIsComputeGraph = false;

			final double graphXStart = graphPointsX[0];
			final double graphXEnd = graphPointsX[pointLength - 1];
			final double graphWidth = graphXEnd - graphXStart;

			int devSplineWidth;

			if (fIsPointMoved) {

				// get spline width
				devSplineWidth = (int) (graphWidth * fMovedScaleX);

			} else {
				devSplineWidth = devGraphWidth;
			}

			fDevYSplineValues = new int[devSplineWidth];

			final double scaleXGraph = graphWidth / devSplineWidth;

			final CubicSpline cubicSpline = new CubicSpline(graphPointsX, graphPointsY);

			try {

				for (int devIndex = 0; devIndex < devSplineWidth; devIndex++) {

					final double graphX = graphXStart + (devIndex * scaleXGraph);
					final double graphY = cubicSpline.interpolate(graphX);

					final int devY = (int) (graphY * scaleY);

					fDevYSplineValues[devIndex] = devY;
				}

			} catch (final IllegalArgumentException e2) {
				// ignore
			}
		}

		// get fist point offset
		if (fIsSynchMinMax) {

			if (fPointHitIndex == 0 && fIsPointMoved) {
				fDevXStartOffset = (int) ((graphPointsX[0] - fGraphStartX) * fMovedScaleX);
			} else {
				fDevXStartOffset = 0;
			}

		} else {
			if (fPointHitIndex == 0 && fIsPointMoved) {
				fDevXStartOffset = (int) ((graphPointsX[0] - fGraphStartX) * fMovedScaleX);
			}
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

		gc.drawLine(0, devY0, devWidth, devY0); //	x-axis
		gc.drawLine(devX0, 0, devX0, devHeight); // y-axis

		/*
		 * paint splines
		 */
		gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));

		int devYPrev = fDevYSplineValues[0];

		for (int devXIndex = 1; devXIndex < fDevYSplineValues.length; devXIndex++) {

			final int devY = fDevYSplineValues[devXIndex];

			gc.drawLine(fDevXStartOffset + devXIndex - 1 + devMargin2,//
					devY0 - devYPrev,
					fDevXStartOffset + devXIndex + devMargin2,
					devY0 - devY);

			devYPrev = devY;
		}

		/*
		 * paint spline points
		 */
		fSpPointRects = new Rectangle[pointLength];

		final int hitSize = 10;
		final int hitSize2 = hitSize / 2;
		final int pointSize = 10;
		final int pointSize2 = pointSize / 2;

		for (int valueIndex = 0; valueIndex < pointLength; valueIndex++) {

			int devX = (int) (graphPointsX[valueIndex] * scaleX);
			int devY = (int) (graphPointsY[valueIndex] * scaleY);

			devX = devX0 + devX;
			devY = devY0 - devY;

			// draw movable points with different colors
			if (isPointMovable[valueIndex]) {
				gc.setBackground(display.getSystemColor(SWT.COLOR_BLUE));
			} else {
				gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
			}

//			gc.fillRectangle(devX - pointSize2, devY - pointSize2, pointSize, pointSize);
			gc.fillOval(devX - pointSize2, devY - pointSize2, pointSize, pointSize);

			// keep point position
			fSpPointRects[valueIndex] = new Rectangle(devX - hitSize2, devY - hitSize2, hitSize, hitSize);
		}

	}

	public void updateValues(final SplineData splineData, final boolean isSynchMinMax, final boolean keepSynchMinMax) {

		final boolean backupIsSynchMinMax = fIsSynchMinMax;

		fSplineData = splineData;
		fIsSynchMinMax = isSynchMinMax;

		fIsComputeGraph = true;

		// recompute min/max values
//		fIsMinMaxComputed = false;

		redraw();

		if (keepSynchMinMax == false) {
			// restore synch min/max state
			fIsSynchMinMax = backupIsSynchMinMax;
		}
	}

}
