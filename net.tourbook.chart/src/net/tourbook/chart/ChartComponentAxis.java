/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
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

public class ChartComponentAxis extends Canvas {

	private static final int			UNIT_OFFSET	= 7;

	private Chart						fChart;

	private Image						axisImage;

	private ArrayList<ChartDrawingData>	chartDrawingData;

	private boolean						isAxisDirty;

	/**
	 * is set to <code>true</code> when the axis is on the left side, <code>false</code> when on
	 * the right side
	 */
	private boolean						fIsLeft;

	ChartComponentAxis(Chart chart, Composite parent, int style) {

		super(parent, SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED);

		fChart = chart;

		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent event) {
				onPaint(event.gc);
			}
		});

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				axisImage = (Image) ChartUtil.disposeResource(axisImage);
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				fChart.fChartComponents.getChartComponentGraph().setFocus();
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				fChart.fChartComponents.getChartComponentGraph().onMouseDoubleClick(e);
			}

		});

		createContextMenu();
	}

	/**
	 * create the context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {
				fChart.fillContextMenu(menuMgr);
			}
		});

		setMenu(menuMgr.createContextMenu(this));
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
		// it is dirty
		if (!isAxisDirty && axisImage != null) {

			Rectangle oldBounds = axisImage.getBounds();

			if (oldBounds.width == axisRect.width && oldBounds.height == axisRect.height) {
				return;
			}
		}

		if (ChartUtil.canReuseImage(axisImage, axisRect) == false) {
			axisImage = ChartUtil.createImage(getDisplay(), axisImage, axisRect);
		}

		// draw into the image
		GC gc = new GC(axisImage);

		gc.setBackground(fChart.getBackgroundColor());
		gc.fillRectangle(axisImage.getBounds());

		drawYUnits(gc, axisRect);

		// font.dispose();
		gc.dispose();

		isAxisDirty = false;
	}

	/**
	 * draws unit label and ticks onto the y-axis
	 * 
	 * @param gc
	 * @param graphRect
	 */
	private void drawYUnits(GC gc, Rectangle axisRect) {

		if (chartDrawingData == null) {
			return;
		}

		Display display = getDisplay();

		int devX = fIsLeft ? axisRect.width - 1 : 0;

		// loop: all graphs
		for (ChartDrawingData drawingData : chartDrawingData) {

			ArrayList<ChartUnit> yUnits = drawingData.getYUnits();

			final float scaleY = drawingData.getScaleY();
			final ChartDataYSerie yData = drawingData.getYData();

			final boolean yAxisDirection = yData.isYAxisDirection();
			final int graphYBottom = drawingData.getGraphYBottom();
			final int devGraphHeight = drawingData.getDevGraphHeight();

			final int devYBottom = drawingData.getDevYBottom();
			final int devYTop = devYBottom - devGraphHeight;
			final String unitText = yData.getUnitLabel();

			String title = yData.getYTitle();

			if (fIsLeft && title != null) {

				Color colorLine = new Color(Display.getCurrent(), yData.getDefaultRGB());
				gc.setForeground(colorLine);

				String yTitle = title + (unitText.equals("") ? "" : " - " + unitText); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				Point labelExtend = gc.textExtent(yTitle);

				int devChartHeight = devYBottom - devYTop;

				// draw only the unit text and not the title when there is not
				// enough space
				if (labelExtend.x > devChartHeight) {
					yTitle = unitText;
					labelExtend = gc.textExtent(yTitle);
				}

				int xPos = labelExtend.y / 2;
				int yPos = devYTop + (devChartHeight / 2) + (labelExtend.x / 2);

				Transform tr = new Transform(display);
				tr.translate(xPos, yPos);
				tr.rotate(-90f);

				gc.setTransform(tr);

				gc.drawText(yTitle, 0, 0, true);

				gc.setTransform(null);

				tr.dispose();

				colorLine.dispose();
			}

			int devY;

			// loop: all units
			int unitCount = 0;
			for (ChartUnit yUnit : yUnits) {

				if (yAxisDirection) {
					devY = devYBottom - (int) ((float) (yUnit.value - graphYBottom) * scaleY);
				} else {
					devY = devYTop + (int) ((float) (yUnit.value - graphYBottom) * scaleY);
				}

				gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));

				final String valueLabel = yUnit.valueLabel;

				/*
				 * hide unit tick when label is not set
				 */
				if (valueLabel.length() > 0) {

					// draw the unit tick

					gc.setLineStyle(SWT.LINE_SOLID);
					if (fIsLeft) {
						gc.drawLine(devX - 5, devY, devX, devY);
					} else {
						gc.drawLine(devX, devY, devX + 5, devY);
					}
				}

				final Point unitExtend = gc.textExtent(valueLabel);
				int devYUnit = devY - unitExtend.y / 2;

				// draw the unit label centered at the unit tick
				if (fIsLeft) {
					gc.drawText(valueLabel, (devX - (unitExtend.x + UNIT_OFFSET)), devYUnit, true);
				} else {
					gc.drawText(valueLabel, (devX + UNIT_OFFSET), devYUnit, true);
				}

				unitCount++;
			}

			// draw the unit line
			gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
			gc.setLineStyle(SWT.LINE_SOLID);
			gc.drawLine(devX, devYBottom, devX, devYTop);
		}
	}

	private void onPaint(GC gc) {

		drawAxisImage();

		gc.drawImage(axisImage, 0, 0);
	}

	void onResize() {
		isAxisDirty = true;
		redraw();
	}

	/**
	 * set a new configuration for the axis, this causes a recreation of the axis
	 * 
	 * @param list
	 * @param isLeft
	 *        true if the axis is on the left side
	 */
	protected void setDrawingData(ArrayList<ChartDrawingData> list, boolean isLeft) {
		chartDrawingData = list;
		fIsLeft = isLeft;

		onResize();
	}
}
