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
package net.tourbook.ui;

import net.tourbook.common.util.Util;
import net.tourbook.data.SplineData;
import net.tourbook.math.CubicSpline;

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

	private SplineData	_splineData;

	private int[]		_devYSplineValues;
	private boolean		_isComputeGraph;

	private Rectangle[]	_spPointRects;
	private int			_pointHitIndex;
	private boolean		_isPointMoved;

	private float		_movedScaleX;
	private float		_movedScaleY;
	private float		_movedXMin;
	private float		_movedXMax;
	private float		_movedYMin;
	private float		_movedYMax;
	private int			_movedDevX0;
	private int			_movedDevY0;

	private Cursor		_cursorDragged;

	private double		_graphStartX;

	private boolean		_isSynchMinMax;

	private float		_xMinComputed;
	private float		_xMaxComputed;
	private float		_yMinComputed;
	private float		_yMaxComputed;
	private boolean		_isMinMaxComputed;

	private int			_devXStartOffset;

	public SplineGraph(final Composite parent, final int style) {

		super(parent, style | SWT.DOUBLE_BUFFERED);

		_cursorDragged = new Cursor(getDisplay(), SWT.CURSOR_SIZEALL);

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
				_isComputeGraph = true;
				redraw();
			}
		});
	}

	private void onDispose(final DisposeEvent e) {

		_cursorDragged = (Cursor) Util.disposeResource(_cursorDragged);
	}

	private void onMouseDoubleClick(final MouseEvent e) {

	}

	private void onMouseDown(final MouseEvent e) {

		if (_spPointRects == null) {
			return;
		}

		if (_pointHitIndex != Integer.MIN_VALUE) {

			// mouse is over a point

			_isPointMoved = true;
		}

	}

	private void onMouseMove(final MouseEvent event) {

		if (_spPointRects == null) {
			return;
		}

		final int devXMouse = event.x;
		final int devYMouse = event.y;

		if (_isPointMoved) {

			final int devX = -_movedDevX0 + devXMouse;
			final int devY = _movedDevY0 - devYMouse;

			double graphPointX = (double) devX / _movedScaleX;
			final double graphPointY = (double) devY / _movedScaleY;

			/*
			 * limit horizontal movement to min/max when it's set
			 */
			final double graphXMin = _splineData.graphXMinValues[_pointHitIndex];
			final double graphXMax = _splineData.graphXMaxValues[_pointHitIndex];

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
			final double[] xPointValues = _splineData.graphXValues;
			final double[] yPointValues = _splineData.graphYValues;

			// update spline points
			xPointValues[_pointHitIndex] = graphPointX;
			yPointValues[_pointHitIndex] = graphPointY;

			_isComputeGraph = true;
			redraw();

		} else {

			final boolean[] isPointMovable = _splineData.isPointMovable;

			for (int valueIndex = 0; valueIndex < _spPointRects.length; valueIndex++) {

				if (isPointMovable[valueIndex]) {

					// keep point dev position
					final Rectangle pointRect = _spPointRects[valueIndex];

					if (pointRect.contains(devXMouse, devYMouse)) {
						setCursor(_cursorDragged);
						_pointHitIndex = valueIndex;
						return;
					}
				}
			}

			_pointHitIndex = Integer.MIN_VALUE;

			setCursor(null);
		}
	}

	private void onMouseUp(final MouseEvent e) {

		if (_isPointMoved) {

			_isPointMoved = false;

			if (_isSynchMinMax) {
				_isComputeGraph = true;
			}

			redraw();
		}
	}

	private void onPaint(final PaintEvent e) {

		if (_splineData == null) {
			return;
		}

		final double[] graphPointsX = _splineData.graphXValues;
		final double[] graphPointsY = _splineData.graphYValues;
		final boolean[] isPointMovable = _splineData.isPointMovable;


		// check if 2 points are available
		if (graphPointsX == null || graphPointsX.length < 2) {
			return;
		}

		final int pointLength = graphPointsX.length;

		float graphXMin = _xMinComputed;
		float graphXMax = _xMaxComputed;
		float graphYMin = _yMinComputed;
		float graphYMax = _yMaxComputed;

		if (_isSynchMinMax || _isMinMaxComputed == false) {

			_isMinMaxComputed = true;

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

			_xMinComputed = graphXMin;
			_xMaxComputed = graphXMax;
			_yMinComputed = graphYMin;
			_yMaxComputed = graphYMax;
		}

		if (_isPointMoved) {
			// use min/max from last none moved graph
			graphXMin = _movedXMin;
			graphXMax = _movedXMax;
			graphYMin = _movedYMin;
			graphYMax = _movedYMax;
		} else {

			// keep none moved min/max
			_movedXMin = graphXMin;
			_movedXMax = graphXMax;
			_movedYMin = graphYMin;
			_movedYMax = graphYMax;

			if (_devXStartOffset == 0) {
				_graphStartX = graphPointsX[0];
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

		_movedScaleX = scaleX;
		_movedScaleY = scaleY;
		_movedDevX0 = devX0;
		_movedDevY0 = devY0;

		// compute splines
		if (_isComputeGraph || _devYSplineValues == null) {

			_isComputeGraph = false;

			final double graphXStart = graphPointsX[0];
			final double graphXEnd = graphPointsX[pointLength - 1];
			final double graphWidth = graphXEnd - graphXStart;

			int devSplineWidth;

			if (_isPointMoved) {

				// get spline width
				devSplineWidth = (int) (graphWidth * _movedScaleX);

			} else {
				devSplineWidth = devGraphWidth;
			}

			_devYSplineValues = new int[devSplineWidth];

			final double scaleXGraph = graphWidth / devSplineWidth;

			final CubicSpline cubicSpline = new CubicSpline(graphPointsX, graphPointsY);

			try {

				for (int devIndex = 0; devIndex < devSplineWidth; devIndex++) {

					final double graphX = graphXStart + (devIndex * scaleXGraph);
					final double graphY = cubicSpline.interpolate(graphX);

					final int devY = (int) (graphY * scaleY);

					_devYSplineValues[devIndex] = devY;
				}

			} catch (final IllegalArgumentException e2) {
				// ignore
			}
		}

		// get fist point offset
		if (_isSynchMinMax) {

			if (_pointHitIndex == 0 && _isPointMoved) {
				_devXStartOffset = (int) ((graphPointsX[0] - _graphStartX) * _movedScaleX);
			} else {
				_devXStartOffset = 0;
			}

		} else {
			if (_pointHitIndex == 0 && _isPointMoved) {
				_devXStartOffset = (int) ((graphPointsX[0] - _graphStartX) * _movedScaleX);
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

		int devYPrev = _devYSplineValues[0];

		for (int devXIndex = 1; devXIndex < _devYSplineValues.length; devXIndex++) {

			final int devY = _devYSplineValues[devXIndex];

			gc.drawLine(_devXStartOffset + devXIndex - 1 + devMargin2,//
					devY0 - devYPrev,
					_devXStartOffset + devXIndex + devMargin2,
					devY0 - devY);

			devYPrev = devY;
		}

		/*
		 * paint spline points
		 */
		_spPointRects = new Rectangle[pointLength];

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
			_spPointRects[valueIndex] = new Rectangle(devX - hitSize2, devY - hitSize2, hitSize, hitSize);
		}

	}

	public void updateValues(final SplineData splineData, final boolean isSynchMinMax, final boolean keepSynchMinMax) {

		final boolean backupIsSynchMinMax = _isSynchMinMax;

		_splineData = splineData;
		_isSynchMinMax = isSynchMinMax;

		_isComputeGraph = true;

		// recompute min/max values
//		fIsMinMaxComputed = false;

		redraw();

		if (keepSynchMinMax == false) {
			// restore synch min/max state
			_isSynchMinMax = backupIsSynchMinMax;
		}
	}

}
