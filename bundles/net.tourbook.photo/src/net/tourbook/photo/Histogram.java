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

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;

import net.tourbook.common.UI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * Displays a histogram of a photo
 * <p>
 * Parts are from pixelitor.gui.HistogramsPanel
 */
public class Histogram extends Canvas implements PaintListener {

   public static final int      NUM_BINS               = 256;

   private int                  _maxLuminance;
   private int[]                _allLuminances         = new int[NUM_BINS];

   private ImageData            _imageData;

   private boolean              _isSetTonality;
   private CurveType            _curveType;

   private int                  _threePoint_Dark;
   private int                  _threePoint_Bright;
   private float                _threePoint_MiddleX;
   private float                _threePoint_MiddleY;
   private CurvesFilter         _curvesFilter;

   private ArrayList<Rectangle> _allPaintedCurvePoints = new ArrayList<>();
   private Rectangle            _hoveredPoint;
   private boolean              _isPointDragged;

   public Histogram(final Composite parent) {

      super(parent, SWT.DOUBLE_BUFFERED | SWT.NO_FOCUS);

      addListener();
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

   private void onMouseDown(final MouseEvent mouseEvent) {

      if (_hoveredPoint != null) {

         _isPointDragged = true;
      }
   }

   private void onMouseExit(final MouseEvent mouseEvent) {

   }

   private void onMouseMove(final MouseEvent mouseEvent) {

      System.out.println(UI.timeStamp() + " : " + _hoveredPoint);
// TODO remove SYSTEM.OUT.PRINTLN

      if (_isPointDragged) {

      } else {

         _hoveredPoint = null;

         for (final Rectangle paintedCurvePoint : _allPaintedCurvePoints) {

            if (paintedCurvePoint.contains(mouseEvent.x, mouseEvent.y)) {

               _hoveredPoint = paintedCurvePoint;

               break;
            }
         }
      }
   }

   private void onMouseUp(final MouseEvent mouseEvent) {

      _isPointDragged = false;
   }

   private void paint_0(final PaintEvent paintEvent) {

      final GC gc = paintEvent.gc;

      final Rectangle canvasBounds = getBounds();

      final int canvasWidth = canvasBounds.width;
      final int canvasHeight = canvasBounds.height;

      final int sliderbarHeight = 20;

      final int graphWidth = canvasWidth;
      final int graphHeight = canvasHeight - sliderbarHeight;

      final float xUnitWidth = graphWidth / 255f;

      paint_10_Histogram(gc, graphWidth, graphHeight, xUnitWidth);

      if (_isSetTonality) {
         paint_20_ToneCurve(gc, graphWidth, graphHeight, xUnitWidth);
      }
   }

   private void paint_10_Histogram(final GC gc,
                                   final int graphWidth,
                                   final int graphHeight,
                                   final float xUnitWidth) {

      gc.setBackground(UI.SYS_COLOR_GRAY);
      gc.fillRectangle(0, 0, graphWidth - 0, graphHeight - 0);

      gc.setForeground(UI.SYS_COLOR_DARK_GRAY);
      gc.setLineWidth((int) (xUnitWidth + 1));

      for (int lumIndex = 0; lumIndex < _allLuminances.length; lumIndex++) {

         final int luminance = _allLuminances[lumIndex];

         final int maxLuminance = _maxLuminance == 0 ? 1 : _maxLuminance;

         final int devX = graphWidth * lumIndex / 256;
         final int devY = graphHeight * luminance / maxLuminance;

         gc.drawLine(

               (int) (devX + xUnitWidth),
               graphHeight,
               (int) (devX + xUnitWidth),
               graphHeight - devY);
      }
   }

   private void paint_20_ToneCurve(final GC gc,
                                   final int graphWidth,
                                   final int graphHeight,
                                   final float xUnitWidth) {

      final int devXDark = (int) (_threePoint_Dark * xUnitWidth);
      final int devXBright = (int) (_threePoint_Bright * xUnitWidth);
      final int devXMiddle = (int) (_threePoint_MiddleX * xUnitWidth);
//    final int devYMiddle = (int) (_threePoint_MiddleY * yUnitWidth);

      final int devYMarker = graphHeight + 2;

      paint_50_ToneMarker(gc, devXDark, devYMarker, UI.SYS_COLOR_BLACK);
      paint_50_ToneMarker(gc, devXMiddle, devYMarker, UI.SYS_COLOR_DARK_GRAY);
      paint_50_ToneMarker(gc, devXBright, devYMarker, UI.SYS_COLOR_WHITE);

      /*
       * Draw curve line
       */
      _allPaintedCurvePoints.clear();

      if (_curvesFilter != null) {

         final int[] allCurveValues = _curvesFilter.getRedTable();
         final int[] allCurvePointValues = new int[2 * 256];

         for (int valueIndex = 0; valueIndex < allCurveValues.length; valueIndex++) {

            final int curveIndex = valueIndex * 2;
            final int curveValueY = allCurveValues[valueIndex];

            final int devX = (int) (graphWidth * valueIndex / 255f);
            final int devY = graphHeight - (int) (graphHeight * curveValueY / 255f);

            allCurvePointValues[curveIndex] = devX;
            allCurvePointValues[curveIndex + 1] = devY;
         }

         gc.setAntialias(SWT.ON);
         gc.setLineWidth(1);

         // draw default line
         gc.setForeground(new Color(155, 155, 155));
         gc.drawLine(

               0,
               graphHeight,
               graphWidth,
               0);

         // draw curve graph
         gc.setForeground(new Color(99, 99, 99));
         gc.setForeground(UI.SYS_COLOR_YELLOW);
         gc.drawPolyline(allCurvePointValues);

         // draw vertical positions
         paint_30_TonePoint(gc, devXDark, allCurveValues[_threePoint_Dark], graphHeight);
         paint_30_TonePoint(gc, devXMiddle, allCurveValues[(int) _threePoint_MiddleX], graphHeight);
         paint_30_TonePoint(gc, devXBright, allCurveValues[_threePoint_Bright], graphHeight);
      }
   }

   private void paint_30_TonePoint(final GC gc,
                                   final int devXPoint,
                                   final int pointValue,
                                   final int graphHeight) {

      final int pointRadius = 12;
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

      paint_0(paintEvent);
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

      final int dark = photo.threePoint_Dark;
      final int bright = photo.threePoint_Bright;
      final float middleXPercent = photo.threePoint_MiddleX;
      final float middleYPercent = photo.threePoint_MiddleY;

      final float diffAbsolute = bright - dark;
      final float middleDiffAbsolute = diffAbsolute / 100 * middleXPercent;

      final float middleAbsolute = dark + middleDiffAbsolute;

      _threePoint_Dark = dark;
      _threePoint_Bright = bright;
      _threePoint_MiddleX = middleAbsolute;
      _threePoint_MiddleY = middleAbsolute;

      redraw();
   }

   public void updateCropArea(final Rectangle2D.Float relCropArea) {

      if (_imageData == null || isDisposed()) {
         return;
      }

      computeLuminance(_imageData, relCropArea);

      redraw();
   }

   public void updateCurvesFilter(final Photo photo) {

      _curvesFilter = photo.getToneCurvesFilter().getCurvesFilter();

      // update in UI thread
      getDisplay().asyncExec(() -> redraw());
   }
}
