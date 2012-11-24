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
package net.tourbook.chart;

import java.util.ArrayList;

import net.tourbook.common.util.ITourToolTipProvider;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public class ChartComponentAxis extends Canvas {

	private static final int			UNIT_OFFSET	= 7;

	private final Chart					_chart;

	private Image						_axisImage;

	private ArrayList<GraphDrawingData>	_graphDrawingData;

	private boolean						_isAxisModified;

	/**
	 * is set to <code>true</code> when the axis is on the left side, <code>false</code> when on the
	 * right side
	 */
	private boolean						_isLeft;

	private ITourToolTipProvider		_tourToolTipProvider;

	/**
	 * <pre>
	 * -1  not initialized
	 * 0   not hovered
	 * 1   hovered
	 * </pre>
	 */
	private int							_hoverState	= -1;

	/**
	 * Client area of this axis canvas
	 */
	private Rectangle					_clientArea;

	private ChartComponentGraph			_componentGraph;

	ChartComponentAxis(final Chart chart, final Composite parent, final int style) {

		super(parent, SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED);

		_chart = chart;

		addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent event) {
				onPaint(event.gc);
			}
		});

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				_axisImage = Util.disposeResource(_axisImage);
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(final MouseEvent e) {
				_componentGraph.onMouseDoubleClick(e);
			}

			@Override
			public void mouseDown(final MouseEvent e) {
				onMouseDown(e);
			}

		});

		addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(final MouseEvent e) {
				onMouseMove(e);
			}
		});

		addMouseTrackListener(new MouseTrackListener() {

			public void mouseEnter(final MouseEvent e) {
				onMouseEnter(e);
			}

			public void mouseExit(final MouseEvent e) {
				onMouseExit(e);
			}

			public void mouseHover(final MouseEvent e) {}
		});

		addControlListener(new ControlListener() {

			@Override
			public void controlMoved(final ControlEvent e) {}

			@Override
			public void controlResized(final ControlEvent e) {
				_clientArea = getClientArea();
			}
		});

		addListener(SWT.MouseWheel, new Listener() {
			public void handleEvent(final Event event) {
				onMouseWheel(event);
			}
		});
	}

	public void afterHideToolTip(final Event event) {

		// force redrawing of the axis and hide the hovered image

		_hoverState = 0;

		_isAxisModified = true;
		redraw();
	}

	private void checkHoveredArea(final int x, final int y) {

		if (_tourToolTipProvider != null) {

			final int newHoverState = _tourToolTipProvider.setHoveredLocation(x, y) ? 1 : 0;

			if (_hoverState != newHoverState) {
				// force redrawing of the axis
				_isAxisModified = true;
				redraw();
			}

			_hoverState = newHoverState;
		}
	}

	/**
	 * draws unit label and ticks onto the y-axis
	 * 
	 * @param gc
	 * @param graphRect
	 */
	private void draw_10_YUnits(final GC gc, final Rectangle axisRect) {

		if (_graphDrawingData == null) {
			return;
		}

		gc.setLineStyle(SWT.LINE_SOLID);

		final Display display = getDisplay();

		final int devX = _isLeft ? axisRect.width - 1 : 0;

		// loop: all graphs
		for (final GraphDrawingData drawingData : _graphDrawingData) {

			final ArrayList<ChartUnit> yUnits = drawingData.getYUnits();
			final int numberOfUnits = yUnits.size();

			final double scaleY = drawingData.getScaleY();
			final ChartDataYSerie yData = drawingData.getYData();

			final String title = yData.getYTitle();
			final String unitLabel = yData.getUnitLabel();
			final boolean isBottomUp = yData.isYAxisDirection();

			final float graphYBottom = drawingData.getGraphYBottom();
			final int devGraphHeight = drawingData.devGraphHeight;

			final int devYBottom = drawingData.getDevYBottom();
			final int devYTop = devYBottom - devGraphHeight;

			/*
			 * draw axis title
			 */
			if (_isLeft && title != null) {

				// create title with unit label
				final StringBuilder sbTitle = new StringBuilder(title);
				if (unitLabel.length() > 0) {
					sbTitle.append(Util.DASH_WITH_SPACE);
					sbTitle.append(unitLabel);
				}

				String yTitle = sbTitle.toString();
				Point labelExtend = gc.textExtent(yTitle);

				final int devChartHeight = devYBottom - devYTop;

				// draw only the unit text and not the title when there is not
				// enough space
				if (labelExtend.x > devChartHeight) {
					yTitle = unitLabel;
					labelExtend = gc.textExtent(yTitle);
				}

				final int xPos = labelExtend.y / 2;
				final int yPos = devYTop + (devChartHeight / 2) + (labelExtend.x / 2);

				final Color fgColor = new Color(Display.getCurrent(), yData.getDefaultRGB());
				gc.setForeground(fgColor);

				final Transform tr = new Transform(display);
				{
					tr.translate(xPos, yPos);
					tr.rotate(-90f);

					gc.setTransform(tr);
					gc.drawText(yTitle, 0, 0, true);

					gc.setTransform(null);
				}
				tr.dispose();
				fgColor.dispose();
			}

			int devY;

			// loop: all units
			for (final ChartUnit yUnit : yUnits) {

				final double unitValue = yUnit.value;
				final double devYUnit = (((unitValue - graphYBottom) * scaleY) + .5);

				if (isBottomUp || numberOfUnits == 1) {
					devY = devYBottom - (int) devYUnit;
				} else {
					devY = devYTop + (int) devYUnit;
				}

				gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));

				final String valueLabel = yUnit.valueLabel;

				// draw the unit tick, hide it when label is not set
				if (valueLabel.length() > 0) {

					if (_isLeft) {
						gc.drawLine(devX - 5, devY, devX, devY);
					} else {
						gc.drawLine(devX, devY, devX + 5, devY);
					}
				}

				final Point unitExtend = gc.textExtent(valueLabel);
				final int devYUnitLabel = devY - unitExtend.y / 2;

				// draw the unit label centered at the unit tick
				if (_isLeft) {
					gc.drawText(valueLabel, (devX - (unitExtend.x + UNIT_OFFSET)), devYUnitLabel, true);
				} else {
					gc.drawText(valueLabel, (devX + UNIT_OFFSET), devYUnitLabel, true);
				}
			}

			if (numberOfUnits > 0) {

				// draw unit line only when units are available

				gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
				gc.drawLine(devX, devYBottom, devX, devYTop);
			}
		}
	}

	private void draw_20_ZoomMarker(final GC gc, final Rectangle rect) {

		final double zoomRatio = _componentGraph.getZoomRatio();
		final double leftBorderRatio = _componentGraph.getZoomRatioLeftBorder();

//		System.out.println(UI.timeStampNano() + " zoomRatio " + zoomRatio + "\tleftBorderRatio " + leftBorderRatio);
//		// TODO remove SYSTEM.OUT.PRINTLN

	}

	/**
	 * draw the chart on the axisImage
	 */
	private void drawAxisImage() {

		final Rectangle axisRect = getClientArea();

		if (axisRect.width <= 0 || axisRect.height <= 0) {
			return;
		}

		// when the image is the same size as the new we will redraw it only if
		// it is modified
		if (!_isAxisModified && _axisImage != null) {

			final Rectangle oldBounds = _axisImage.getBounds();

			if (oldBounds.width == axisRect.width && oldBounds.height == axisRect.height) {
				return;
			}
		}

		if (Util.canReuseImage(_axisImage, axisRect) == false) {
			_axisImage = Util.createImage(getDisplay(), _axisImage, axisRect);
		}

		// draw into the image
		final GC gc = new GC(_axisImage);

		gc.setBackground(_chart.getBackgroundColor());
//		gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		gc.fillRectangle(_axisImage.getBounds());

		draw_10_YUnits(gc, axisRect);
		draw_20_ZoomMarker(gc, axisRect);

		if (_tourToolTipProvider != null) {
			_tourToolTipProvider.paint(gc, axisRect);
		}

		gc.dispose();

		_isAxisModified = false;
	}

	public Rectangle getAxisClientArea() {
		return _clientArea;
	}

	private void onMouseDown(final MouseEvent event) {

		_componentGraph.setFocus();

		_componentGraph.onMouseDownAxis(event);
	}

	private void onMouseEnter(final MouseEvent event) {
		_componentGraph.onMouseEnterAxis(event);
	}

	private void onMouseExit(final MouseEvent event) {
		_componentGraph.onMouseExitAxis(event);
	}

	private void onMouseMove(final MouseEvent event) {

		if (_componentGraph.onMouseMoveAxis(event)) {
			return;
		}

		checkHoveredArea(event.x, event.y);
	}

	private void onMouseWheel(final Event event) {

		_componentGraph.onMouseWheel(event, true, _isLeft);

		/*
		 * display tour tool tip when mouse is hovered over the tour info icon in the statistics and
		 * the mouse wheel selects another tour
		 */
		checkHoveredArea(event.x, event.y);

		if (_tourToolTipProvider != null && _hoverState == 1) {
			_tourToolTipProvider.show(new Point(event.x, event.y));
		}
	}

	private void onPaint(final GC gc) {

		drawAxisImage();

		gc.drawImage(_axisImage, 0, 0);
	}

	void onResize() {
		_isAxisModified = true;
		redraw();
	}

	void setComponentGraph(final ChartComponentGraph componentGraph) {
		_componentGraph = componentGraph;
	}

	/**
	 * set a new configuration for the axis, this causes a recreation of the axis
	 * 
	 * @param _chartDrawingData
	 * @param isLeft
	 *            true if the axis is on the left side
	 */
	protected void setDrawingData(final ChartDrawingData _chartDrawingData, final boolean isLeft) {

		_graphDrawingData = _chartDrawingData.graphDrawingData;
		_isLeft = isLeft;

		onResize();
	}

	void setTourToolTipProvider(final ITourToolTipProvider tourInfoToolTipProvider) {
		_tourToolTipProvider = tourInfoToolTipProvider;
	}
}
