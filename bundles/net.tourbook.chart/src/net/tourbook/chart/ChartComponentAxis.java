/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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

import static org.eclipse.swt.events.ControlListener.controlResizedAdapter;

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.util.ITourToolTipProvider;
import net.tourbook.common.util.StringUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
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

public class ChartComponentAxis extends Canvas {

   private static final String         ZOOM_TEXT_0             = "0";                             //$NON-NLS-1$
   private static final String         ZOOM_TEXT_LARGER_THEN_0 = "> 0";                           //$NON-NLS-1$

   /**
    * Offset between the unit label and unit tick
    */
   private static final int            UNIT_HORIZONTAL_OFFSET  = 6;

   private final Chart                 _chart;

   private Image                       _axisImage;

   private ChartDrawingData            _chartDrawingData;
   private ArrayList<GraphDrawingData> _graphDrawingData;

   private boolean                     _isAxisModified;

   /**
    * is set to <code>true</code> when the axis is on the left side, <code>false</code> when on the
    * right side
    */
   private boolean                     _isLeft;

   private ITourToolTipProvider        _tourInfoIconToolTipProvider;

   /**
    * <pre>
    * -1  not initialized
    * 0   not hovered
    * 1   hovered
    * </pre>
    */
   private int                         _hoverState             = -1;

   /**
    * Client area of this axis canvas
    */
   private Rectangle                   _clientArea;

   private ChartComponentGraph         _componentGraph;

   private Display                     _display;
   private Color                       _moveMarkerColor;

   private final NumberFormat          _nf1                    = NumberFormat.getNumberInstance();
   {
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   ChartComponentAxis(final Chart chart, final Composite parent) {

      super(parent, SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED);

      _chart = chart;

      _moveMarkerColor = UI.IS_DARK_THEME
            ? new Color(70, 137, 201)
            : new Color(0x8B, 0xC6, 0xFF);

      addDisposeListener(disposeEvent -> onDispose());

      addPaintListener(paintEvent -> onPaint(paintEvent.gc));

      addMouseListener(MouseListener.mouseDoubleClickAdapter(mouseEvent -> _componentGraph.onMouseDoubleClick(mouseEvent)));
      addMouseListener(MouseListener.mouseDownAdapter(mouseEvent -> onMouseDown()));

      addMouseMoveListener(event -> onMouseMove(event));

      addMouseTrackListener(MouseTrackListener.mouseEnterAdapter(event -> onMouseEnter(event)));
      addMouseTrackListener(MouseTrackListener.mouseExitAdapter(event -> onMouseExit(event)));

      addControlListener(controlResizedAdapter(controlEvent -> _clientArea = getClientArea()));

      addListener(SWT.MouseWheel, event -> onMouseWheel(event));
   }

   public void afterHideToolTip() {

      // force redrawing of the axis and hide the hovered image

      _hoverState = 0;

      _isAxisModified = true;
      redraw();
   }

   private void checkHoveredArea(final int x, final int y) {

      if (_tourInfoIconToolTipProvider != null) {

         final int newHoverState = _tourInfoIconToolTipProvider.setHoveredLocation(x, y) ? 1 : 0;

         if (_hoverState != newHoverState) {
            // force redrawing of the axis
            _isAxisModified = true;
            redraw();
         }

         _hoverState = newHoverState;
      }
   }

   /**
    * draw the chart on the axisImage
    */
   private void draw_00_AxisImage() {

      final Rectangle axisRect = getClientArea();

      if (axisRect.width <= 0 || axisRect.height <= 0) {
         return;
      }

      if (_graphDrawingData == null) {
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

      _display = getDisplay();

      if (Util.canReuseImage(_axisImage, axisRect) == false) {
         _axisImage = Util.createImage(_display, _axisImage, axisRect);
      }

      final Color backgroundColor = UI.isDarkTheme()
            ? ThemeUtil.getDarkestBackgroundColor()
            : _chart.getBackgroundColor();

      if (backgroundColor == null) {

         // this happened during app startup with dark theme

      } else {

         // draw into the image

         final GC gc = new GC(_axisImage);
         {
            gc.setFont(UI.getUIDrawingFont());

            gc.setBackground(backgroundColor);
            gc.fillRectangle(_axisImage.getBounds());

            draw_10_ZoomMarker(gc, axisRect);
            draw_20_YUnits(gc, axisRect);

            if (_tourInfoIconToolTipProvider != null) {
               _tourInfoIconToolTipProvider.paint(gc, axisRect);
            }
         }
         gc.dispose();
      }

      _isAxisModified = false;
   }

   /**
    * The move markers in the tour chart axis shows, how far to the right or left a zoomed chart is
    * moved.
    *
    * @param gc
    * @param rect
    */
   private void draw_10_ZoomMarker(final GC gc, final Rectangle rect) {

      final double zoomRatio = _componentGraph.getZoomRatio();
      if (zoomRatio == 1.0) {

         // chart is not zoomed
         return;
      }

      final long devVirtualWidth = _componentGraph.getXXDevGraphWidth();
      final int devVisibleWidth = _chartDrawingData.devVisibleChartWidth;

      final long devWidthWithoutVisible = devVirtualWidth - devVisibleWidth;
      final long devLeftBorder = _componentGraph.getXXDevViewPortLeftBorder();
      final double moveRatio = (double) devLeftBorder / devWidthWithoutVisible;

      final int devAxisWidth = rect.width;
      final int devAxisHeight = rect.height;

      // this is the height between graph bottom and bottom of the x-axis unit label
      final int devMarkerHeight = 6;
      final int devYMarker = devAxisHeight - devMarkerHeight;

      gc.setBackground(_moveMarkerColor);

      if (_isLeft) {

         final int devZoomMarkerWidth = (int) (devAxisWidth * moveRatio);

         gc.fillRectangle(
               0,
               devYMarker,
               devZoomMarkerWidth,
               devMarkerHeight);

         /*
          * show zoom ratio
          */
         final String zoomText = getZoomText(moveRatio * 100);
         final int textHeight = gc.textExtent(zoomText).y;

         gc.setForeground(ThemeUtil.getDefaultForegroundColor_Shell());
         gc.drawText(zoomText,
               1,
               devYMarker - textHeight + 2,
               true);

      } else {

         final double moveValue = 1.0 - moveRatio;

         final int devZoomMarkerWidth = (int) (devAxisWidth * moveValue);

         gc.fillRectangle(
               devAxisWidth - devZoomMarkerWidth,
               devYMarker,
               devZoomMarkerWidth,
               devMarkerHeight);

         /*
          * show moved chart in%
          */
         final String zoomText = getZoomText(moveValue * 100);
         final Point textExtent = gc.textExtent(zoomText);

         final int textWidth = textExtent.x;
         final int textHeight = textExtent.y;

         gc.setForeground(ThemeUtil.getDefaultForegroundColor_Shell());
         gc.drawText(zoomText,
               devAxisWidth - textWidth - 0,
               devYMarker - textHeight + 2,
               true);
      }
   }

   /**
    * draws unit label and ticks onto the y-axis
    *
    * @param gc
    * @param graphRect
    */
   private void draw_20_YUnits(final GC gc, final Rectangle axisRect) {

      gc.setLineStyle(SWT.LINE_SOLID);

      int graphNo = 0;
      final int lastGraphNo = _graphDrawingData.size();

      final boolean isGraphOverlapped = _componentGraph._isChartOverlapped;
      final boolean canChartBeOverlapped = _componentGraph._canChartBeOverlapped;
      final boolean isStackedChart = !isGraphOverlapped;

      final int devX = _isLeft ? axisRect.width - 1 : 0;

      // loop: all graphs
      for (final GraphDrawingData graphDrawingData : _graphDrawingData) {

         graphNo++;

         final boolean isLastGraph = graphNo == lastGraphNo;

         /*
          * Draw units only for the last overlapped graph
          */

         final boolean isLastOverlappedGraph = isGraphOverlapped && isLastGraph;

         final boolean isDrawGridAndUnits =

               // chart is not overlapped
               canChartBeOverlapped == false

                     || (canChartBeOverlapped && (isStackedChart || isLastOverlappedGraph));

         if (isDrawGridAndUnits == false) {
            continue;
         }

         final ArrayList<ChartUnit> allYUnits = graphDrawingData.getYUnits();
         final int numberOfUnits = allYUnits.size();

         final double scaleY = graphDrawingData.getScaleY();
         final ChartDataYSerie yData = graphDrawingData.getYData();

         final String unitLabel = yData.getUnitLabel();
         final boolean isBottomUp = yData.isYAxis_Bottom2Top();

         final float graphYBottom = graphDrawingData.getGraphYBottom();
         final int devGraphHeight = graphDrawingData.devGraphHeight;

         final int devYBottom = graphDrawingData.getDevYBottom();
         final int devYTop = devYBottom - devGraphHeight;

         /*
          * Draw y units
          */
         gc.setForeground(Chart.FOREGROUND_COLOR_UNITS);

         int devY;

         for (final ChartUnit yUnit : allYUnits) {

            final double unitValue = yUnit.value;
            final double devYUnit = (((unitValue - graphYBottom) * scaleY) + .5);

            if (isBottomUp || numberOfUnits == 1) {
               devY = devYBottom - (int) devYUnit;
            } else {
               devY = devYTop + (int) devYUnit;
            }

            final String valueLabel = yUnit.valueLabel;

            // draw the unit tick, hide it when label is not set
            if (valueLabel.length() > 0) {

               if (_isLeft) {
                  gc.drawLine(devX - ChartComponents.UNIT_TICK_SIZE, devY, devX, devY);
               } else {
                  gc.drawLine(devX, devY, devX + ChartComponents.UNIT_TICK_SIZE, devY);
               }
            }

            final Point unitExtend = gc.textExtent(valueLabel);
            final int devYUnitLabel = devY - unitExtend.y / 2;

            // draw the unit label centered at the unit tick
            if (_isLeft) {
               gc.drawText(valueLabel, (devX - (unitExtend.x + UNIT_HORIZONTAL_OFFSET)), devYUnitLabel, true);
            } else {
               gc.drawText(valueLabel, (devX + UNIT_HORIZONTAL_OFFSET), devYUnitLabel, true);
            }
         }

         if (numberOfUnits > 0) {

            // draw unit line only when units are available

            gc.drawLine(devX, devYBottom, devX, devYTop);
         }

         /**
          * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
          *
          * VERY IMPORTANT
          * <p>
          *
          * The y-asix title MUST be painted after the unit because a "Transform" is modifying the
          * font and I did not find out how to fix this
          *
          * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
          *
          * Draw y-axis title which is the unit label
          */
         if (_isLeft && StringUtils.hasContent(unitLabel)) {

            final Point labelExtend = gc.textExtent(unitLabel);

            final int unitWidth = labelExtend.x;

            final int xPos = 3;
            final int yPos = devYTop + (devGraphHeight / 2) + (unitWidth / 2);

            gc.setForeground(new Color(yData.getRgbGraph_Text()));

            final Transform tr = new Transform(_display);
            {
               tr.translate(xPos, yPos);
               tr.rotate(-90f);

               gc.setTransform(tr);
               {
                  gc.drawText(unitLabel, 0, 0, true);
               }
               gc.setTransform(null);
            }
            tr.dispose();
         }
      }
   }

   public Rectangle getAxisClientArea() {
      return _clientArea;
   }

   private String getZoomText(final double percentValue) {

      String zoomText;

      if (percentValue == 100.00000000) {
         zoomText = Integer.toString((int) percentValue);
      } else if (percentValue > 0.05) {
         zoomText = _nf1.format(percentValue);
      } else if (percentValue > 0.0000000000001) {
         zoomText = ZOOM_TEXT_LARGER_THEN_0;
      } else {
         zoomText = ZOOM_TEXT_0;
      }
      return zoomText;
   }

   private void onDispose() {

      _axisImage = UI.disposeResource(_axisImage);
   }

   private void onMouseDown() {

      _componentGraph.setFocus();

      _componentGraph.onMouseDownAxis();
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

      if (_tourInfoIconToolTipProvider != null && _hoverState == 1) {
         _tourInfoIconToolTipProvider.show(new Point(event.x, event.y));
      }
   }

   private void onPaint(final GC gc) {

      draw_00_AxisImage();

      if (_axisImage == null) {

         // this case happened

         return;
      }

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
    * @param chartDrawingData
    * @param isLeft
    *           true if the axis is on the left side
    */
   protected void setDrawingData(final ChartDrawingData chartDrawingData, final boolean isLeft) {

      _chartDrawingData = chartDrawingData;
      _graphDrawingData = chartDrawingData.graphDrawingData;
      _isLeft = isLeft;

      onResize();
   }

   void setTourToolTipProvider(final ITourToolTipProvider tourInfoToolTipProvider) {
      _tourInfoIconToolTipProvider = tourInfoToolTipProvider;
   }
}
