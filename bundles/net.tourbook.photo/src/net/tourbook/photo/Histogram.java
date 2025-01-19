/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import com.jhlabs.image.CurvesFilter;
import com.jhlabs.image.ImageMath;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;

import net.tourbook.common.UI;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * Displays a histogram of a photo
 * <p>
 * Parts are from pixelitor.gui.HistogramsPanel
 */
public class Histogram extends Canvas implements PaintListener {

   private static final int                       POINT_RADIUS           = 12;

   public static final int                        NUM_BINS               = 256;

   private int                                    _maxLuminance;
   private int[]                                  _allLuminances         = new int[NUM_BINS];

   private ImageData                              _imageData;

   private boolean                                _isSetTonality;
   private CurveType                              _curveType;

   private int                                    _threePoint_DevDark;
   private int                                    _threePoint_DevBright;
   private float                                  _threePoint_DevMiddleX;
   private CurvesFilter                           _curvesFilter;

   private ArrayList<Rectangle>                   _allPaintedCurvePoints = new ArrayList<>();
   private int                                    _hoveredPointIndex     = -1;
   private boolean                                _isPointDragged;

   private int                                    _graphWidth;
   private int                                    _graphHeight;
   private float                                  _xUnitWidth;

   private final ListenerList<IHistogramListener> _allHistogramListener  = new ListenerList<>(ListenerList.IDENTITY);

   /*
    * UI controls
    */
   private Cursor _currentCursor;
   private Cursor _cursor_Size_All;

   public Histogram(final Composite parent) {

      super(parent, SWT.DOUBLE_BUFFERED | SWT.NO_FOCUS);

      addListener();

      initUI();
   }

   public void addHistogramListener(final IHistogramListener listener) {

      _allHistogramListener.add(listener);
   }

   private void addListener() {

      addPaintListener(this);

      addMouseListener(MouseListener.mouseDownAdapter(mouseEvent -> onMouseDown(mouseEvent)));
      addMouseListener(MouseListener.mouseUpAdapter(mouseEvent -> onMouseUp(mouseEvent)));
      addMouseMoveListener(event -> onMouseMove(event));
      addMouseTrackListener(MouseTrackListener.mouseExitAdapter(mouseEvent -> onMouseExit(mouseEvent)));
   }

   /**
    * @param imageData
    * @param relCropArea
    *           x = X1<br>
    *           y = Y1<br>
    *           width = X2<br>
    *           height = Y2
    */
   private void computeLuminance(final ImageData imageData, final Rectangle2D.Float relCropArea) {

      Arrays.fill(_allLuminances, 0);

      int alpha = 1;
      int blue;
      int green;
      int red;

      final byte[] dstData = imageData.data;
      final int dstWidth = imageData.width;
      final int dstHeight = imageData.height;
      final int dstBytesPerLine = imageData.bytesPerLine;
      final int dstPixelBytes = imageData.depth == 32 ? 4 : 3;

      int xStart = 0;
      int xEnd = dstWidth;
      int yStart = 0;
      int yEnd = dstHeight;

      if (relCropArea != null) {

         final float relCropX1 = relCropArea.x;
         final float relCropY1 = relCropArea.y;

         final float relCropX2 = relCropArea.width;
         final float relCropY2 = relCropArea.height;

         final int devCropX1 = (int) (relCropX1 * dstWidth);
         final int devCropY1 = (int) (relCropY1 * dstHeight);
         final int devCropX2 = (int) (relCropX2 * dstWidth);
         final int devCropY2 = (int) (relCropY2 * dstHeight);

         xStart = devCropX1;
         xEnd = devCropX2;

         yStart = devCropY1;
         yEnd = devCropY2;
      }

      for (int dstY = yStart; dstY < yEnd; dstY++) {

         final int dstYBytesPerLine = dstY * dstBytesPerLine;

         for (int dstX = xStart; dstX < xEnd; dstX++) {

            final int dataIndex = dstYBytesPerLine + (dstX * dstPixelBytes);

            if (dstPixelBytes == 4) {

               alpha = dstData[dataIndex + 0] & 0xFF;
               red = dstData[dataIndex + 1] & 0xFF;
               green = dstData[dataIndex + 2] & 0xFF;
               blue = dstData[dataIndex + 3] & 0xFF;

            } else {

               red = dstData[dataIndex + 0] & 0xFF;
               green = dstData[dataIndex + 1] & 0xFF;
               blue = dstData[dataIndex + 2] & 0xFF;
            }

            if (alpha > 0) {

               final int lumimance = (0

                     + 299 * red
                     + 587 * green
                     + 114 * blue

               ) / 1000;

               _allLuminances[lumimance]++;
            }
         }
      }

      int maxLuminance = 0;

      for (final int luminance : _allLuminances) {

         if (luminance > maxLuminance) {
            maxLuminance = luminance;
         }
      }

      _maxLuminance = maxLuminance;
   }

   /**
    * @param yValueRel
    * @param xValueRel
    * @param yValueRel2
    * @param isZoomed
    *           Is <code>true</code> when the event is fired by zooming
    */
   private void fireEvent_OnPointMove(final int pointIndex, final float xValueRel, final float yValueRel) {

      // ensure 0...1
      final float xRel = ImageMath.clamp01(xValueRel);
      final float yRel = ImageMath.clamp01(yValueRel);

      final Object[] listeners = _allHistogramListener.getListeners();

      for (final Object listener : listeners) {
         ((IHistogramListener) listener).onPointMove(pointIndex, xRel, yRel);
      }
   }

   private int getHoveredPointIndex(final MouseEvent mouseEvent) {

      for (int pointIndex = 0; pointIndex < _allPaintedCurvePoints.size(); pointIndex++) {

         final Rectangle paintedCurvePoint = _allPaintedCurvePoints.get(pointIndex);

         if (paintedCurvePoint.contains(mouseEvent.x, mouseEvent.y)) {

            return pointIndex;
         }
      }

      return -1;
   }

   private void initUI() {

      _cursor_Size_All = getDisplay().getSystemCursor(SWT.CURSOR_SIZEALL);
   }

   private void onMouseDown(final MouseEvent mouseEvent) {

      if (_hoveredPointIndex != -1) {

         _isPointDragged = true;

         redraw();
      }
   }

   private void onMouseExit(final MouseEvent mouseEvent) {

   }

   private void onMouseMove(final MouseEvent mouseEvent) {

      final int oldHoveredPointIndex = _hoveredPointIndex;

      if (_isPointDragged) {

         if (_hoveredPointIndex != -1) {

            // point is dragged

            final float mouseX = mouseEvent.x;
            final float mouseY = mouseEvent.y;

            final float xValueRel = mouseX / _graphWidth;
            final float YValueRel = 1 - (mouseY / _graphHeight);

            fireEvent_OnPointMove(_hoveredPointIndex, xValueRel, YValueRel);
         }

      } else {

         _hoveredPointIndex = getHoveredPointIndex(mouseEvent);
      }

      updateCursor(_hoveredPointIndex == -1 ? null : _cursor_Size_All);

      // check if a redraw is necessary
      if (oldHoveredPointIndex != _hoveredPointIndex) {
         redraw();
      }
   }

   private void onMouseUp(final MouseEvent mouseEvent) {

      _isPointDragged = false;

      // update hovered point, the mouse could be outside of the hovered point
      _hoveredPointIndex = getHoveredPointIndex(mouseEvent);

      updateCursor(_hoveredPointIndex == -1 ? null : _cursor_Size_All);

      redraw();
   }

   private void paint(final PaintEvent paintEvent) {

      final GC gc = paintEvent.gc;

      final Rectangle canvasBounds = getBounds();

      final int canvasWidth = canvasBounds.width;
      final int canvasHeight = canvasBounds.height;

      final int sliderbarHeight = 20;

      _graphWidth = canvasWidth;
      _graphHeight = canvasHeight - sliderbarHeight;
      _xUnitWidth = _graphWidth / 255f;

      paint_10_Histogram(gc);

      if (_isSetTonality) {
         paint_20_ToneCurve(gc);
      }
   }

   private void paint_10_Histogram(final GC gc) {

      gc.setBackground(UI.SYS_COLOR_GRAY);
      gc.fillRectangle(0, 0, _graphWidth - 0, _graphHeight - 0);

      gc.setForeground(UI.SYS_COLOR_DARK_GRAY);
      gc.setLineWidth((int) (_xUnitWidth + 1));

      for (int lumIndex = 0; lumIndex < _allLuminances.length; lumIndex++) {

         final int luminance = _allLuminances[lumIndex];

         final int maxLuminance = _maxLuminance == 0 ? 1 : _maxLuminance;

         final int devX = _graphWidth * lumIndex / 256;
         final int devY = _graphHeight * luminance / maxLuminance;

         gc.drawLine(

               (int) (devX + _xUnitWidth),
               _graphHeight,
               (int) (devX + _xUnitWidth),
               _graphHeight - devY);
      }
   }

   private void paint_20_ToneCurve(final GC gc) {

      final int devXDark = (int) (_threePoint_DevDark * _xUnitWidth);
      final int devXBright = (int) (_threePoint_DevBright * _xUnitWidth);
      final int devXMiddle = (int) (_threePoint_DevMiddleX * _xUnitWidth);

      final int devYMarker = _graphHeight + 2;

      paint_50_ToneMarker(gc, devXDark, devYMarker, UI.SYS_COLOR_BLACK);
      paint_50_ToneMarker(gc, devXMiddle, devYMarker, UI.SYS_COLOR_DARK_GRAY);
      paint_50_ToneMarker(gc, devXBright, devYMarker, UI.SYS_COLOR_WHITE);

      /*
       * Draw curve line
       */
      _allPaintedCurvePoints.clear();

      if (_curvesFilter != null) {

         final int[] allCurveValues = _curvesFilter.getRedTable();

         final Path curvePath = new Path(getDisplay());

         for (int valueIndex = 0; valueIndex < allCurveValues.length; valueIndex++) {

            final int curveValueY = allCurveValues[valueIndex];

            final float devX = _graphWidth * valueIndex / 255f;
            final float devY = _graphHeight - (_graphHeight * curveValueY / 255f);

            if (valueIndex == 0) {

               curvePath.moveTo(devX, devY);

            } else {

               curvePath.lineTo(devX, devY);
            }
         }

         gc.setAntialias(SWT.ON);
         gc.setLineWidth(1);

         // draw default line
         gc.setForeground(new Color(155, 155, 155));
         gc.drawLine(

               devXDark,
               _graphHeight,
               devXBright,
               0);

         // draw curve graph
         gc.setForeground(UI.SYS_COLOR_YELLOW);
         gc.drawPath(curvePath);

         curvePath.dispose();

         // draw vertical positions
         paint_30_TonePoint(gc, devXDark, allCurveValues[_threePoint_DevDark], _graphHeight);
         paint_30_TonePoint(gc, devXMiddle, allCurveValues[(int) _threePoint_DevMiddleX], _graphHeight);
         paint_30_TonePoint(gc, devXBright, allCurveValues[_threePoint_DevBright], _graphHeight);

         if (_hoveredPointIndex != -1) {

            final Rectangle hoveredRectangle = _allPaintedCurvePoints.get(_hoveredPointIndex);

            if (_isPointDragged) {

               // paint dragged point

               gc.setBackground(UI.SYS_COLOR_YELLOW);
               gc.fillArc(

                     hoveredRectangle.x,
                     hoveredRectangle.y,

                     hoveredRectangle.width,
                     hoveredRectangle.height,

                     0,
                     360);

            } else {

               // paint hovered point

               gc.setForeground(UI.SYS_COLOR_MAGENTA);
               gc.drawArc(

                     hoveredRectangle.x,
                     hoveredRectangle.y,

                     hoveredRectangle.width,
                     hoveredRectangle.height,

                     0,
                     360);
            }
         }
      }
   }

   private void paint_30_TonePoint(final GC gc,
                                   final int devXPoint,
                                   final int pointValue,
                                   final int graphHeight) {

      final int pointRadius = POINT_RADIUS;
      final int pointRadius2 = pointRadius / 2;

      final int devYPoint = (int) (graphHeight - (graphHeight * pointValue / 255f));

      final int devX = devXPoint - pointRadius2;
      final int devY = devYPoint - pointRadius2;

      gc.setForeground(UI.SYS_COLOR_BLUE);
      gc.drawArc(

            devX,
            devY,

            pointRadius,
            pointRadius,

            0,
            360);

      // keep painted positions
      _allPaintedCurvePoints.add(new Rectangle(

            devX,
            devY,
            pointRadius,
            pointRadius));
   }

   private void paint_50_ToneMarker(final GC gc,
                                    final int devX,
                                    final int devYBottom,
                                    final Color bgColor) {

      final int markerWidth = 16;
      final int markerWidth2 = markerWidth / 2;

      final int devMarkerXPos = devX - markerWidth2;

      final int[] marker = new int[] {
            devMarkerXPos,
            devYBottom + 1 + markerWidth2,
            devMarkerXPos + markerWidth2,
            devYBottom + 1,
            devMarkerXPos + markerWidth,
            devYBottom + 1 + markerWidth2 };

      // draw background
      gc.setBackground(bgColor);
      gc.fillPolygon(marker);
   }

   @Override
   public void paintControl(final PaintEvent paintEvent) {

      paint(paintEvent);
   }

   public void setImage(final Image image, final Rectangle2D.Float relCropArea) {

      if (image == null || image.isDisposed()) {

         // reset image
         _imageData = null;

         return;
      }

      _imageData = image.getImageData();

      computeLuminance(_imageData, relCropArea);

      redraw();
   }

   public void update3Points(final Photo photo) {

      _isSetTonality = photo.isSetTonality;
      _curveType = photo.curveType;

      final int devDark = photo.threePoint_Dark;
      final int devBright = photo.threePoint_Bright;
      final float middleX100 = photo.threePoint_MiddleX;

      final float devDiff = devBright - devDark;
      final float devMiddleDiff = devDiff / 100 * middleX100;

      final float devMiddleX = devDark + devMiddleDiff;

      _threePoint_DevDark = devDark;
      _threePoint_DevBright = devBright;
      _threePoint_DevMiddleX = devMiddleX;

      redraw();
   }

   public void updateCropArea(final Rectangle2D.Float relCropArea) {

      if (_imageData == null || isDisposed()) {
         return;
      }

      computeLuminance(_imageData, relCropArea);

      redraw();
   }

   /**
    * Update cursor only when it was modified
    *
    * @param cursor
    */
   private void updateCursor(final Cursor cursor) {

      if (_currentCursor != cursor) {

         _currentCursor = cursor;

         setCursor(cursor);
      }
   }

   public void updateCurvesFilter(final Photo photo) {

      _curvesFilter = photo.getToneCurvesFilter().getCurvesFilter();

      // update in UI thread
      getDisplay().asyncExec(() -> redraw());
   }
}
