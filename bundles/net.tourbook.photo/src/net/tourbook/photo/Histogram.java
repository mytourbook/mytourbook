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

import com.jhlabs.image.CurveValues;
import com.jhlabs.image.ImageMath;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.awt.image.BufferedImage;
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

import pixelitor.filters.curves.ToneCurvesFilter;

/**
 * Displays a histogram of a photo
 * <p>
 * Parts are from pixelitor.gui.HistogramsPanel
 */
public class Histogram extends Canvas implements PaintListener {

   private static final int                       POINT_RADIUS           = 12;

   public static final int                        NUM_BINS               = 256;

   private Color                                  _adjustedColor         = new Color(0x0, 0xff, 0xff);

   private int                                    _maxLuminance_Default;
   private int                                    _maxLuminanceAdjusted;
   private int[]                                  _luminancesDefault     = new int[NUM_BINS];
   private int[]                                  _luminancesAdjusted    = new int[NUM_BINS];

   private ImageData                              _defaultImageData;

   private boolean                                _isEnabled;
   private boolean                                _isSetTonality;

   private ToneCurvesFilter                       _toneCurvesFilter;

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

   private Float  _relCropArea;

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
      addMouseListener(MouseListener.mouseDoubleClickAdapter(mouseEvent -> onMouseDoubleClick(mouseEvent)));
      addMouseListener(MouseListener.mouseUpAdapter(mouseEvent -> onMouseUp(mouseEvent)));
      addMouseMoveListener(event -> onMouseMove(event));
      addMouseTrackListener(MouseTrackListener.mouseExitAdapter(mouseEvent -> onMouseExit(mouseEvent)));
   }

   private int computeLuminance_Adjusted(final BufferedImage image,
                                         final int[] allLuminancValues) {

      Arrays.fill(allLuminancValues, 0);

      if (image == null) {
         return 0;
      }

      final int imageWidth = image.getWidth();
      final int imageHeight = image.getHeight();

      final int[] allPixelValues = pixelitor.utils.ImageUtils.getPixelArray(image);
      final int imageType = image.getType();

      int red = 0;
      int green = 0;
      int blue = 0;

      for (int imageY = 0; imageY < imageHeight; imageY++) {

         final int intPerLine = imageY * imageWidth;

         for (int imageX = 0; imageX < imageWidth; imageX++) {

            final int pixelIndex = intPerLine + imageX;

            final int rgb = allPixelValues[pixelIndex];

            if (imageType == BufferedImage.TYPE_INT_RGB) {

               red = (rgb >> 16) & 0xFF;
               green = (rgb >> 8) & 0xFF;
               blue = (rgb) & 0xFF;
            }

            final int lumimance = (0

                  + 299 * red
                  + 587 * green
                  + 114 * blue

            ) / 1000;

            allLuminancValues[lumimance]++;
         }
      }

      // get max value
      int maxLuminance = 0;
      for (final int luminance : allLuminancValues) {
         if (luminance > maxLuminance) {
            maxLuminance = luminance;
         }
      }

      return maxLuminance;
   }

   /**
    * @param imageData
    * @param relCropArea
    *           x = X1<br>
    *           y = Y1<br>
    *           width = X2<br>
    *           height = Y2
    * @param imageLuminances
    *
    * @return
    */
   private int computeLuminance_Default(final ImageData imageData,
                                        final int[] imageLuminances) {

      Arrays.fill(imageLuminances, 0);

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

      if (_relCropArea != null) {

         final float relCropX1 = _relCropArea.x;
         final float relCropY1 = _relCropArea.y;

         final float relCropX2 = _relCropArea.width;
         final float relCropY2 = _relCropArea.height;

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

               imageLuminances[lumimance]++;
            }
         }
      }

      int maxLuminance = 0;

      for (final int luminance : imageLuminances) {

         if (luminance > maxLuminance) {
            maxLuminance = luminance;
         }
      }

      return maxLuminance;
   }

   /**
    * @param yValueRel
    * @param xValueRel
    * @param yValueRel2
    * @param isZoomed
    *           Is <code>true</code> when the event is fired by zooming
    */
   private void fireEvent_PointIsModified() {

      final Object[] listeners = _allHistogramListener.getListeners();

      for (final Object listener : listeners) {
         ((IHistogramListener) listener).pointIsModified();
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

   private void onMouseDoubleClick(final MouseEvent mouseEvent) {

      if (_isEnabled == false) {
         return;
      }

      if (_hoveredPointIndex != -1) {

         final CurveValues curveValues = _toneCurvesFilter.getCurves().getActiveCurve().curveValues;

         final float[] allValuesX = curveValues.allValuesX;

         final int numValues = allValuesX.length;
         final int lastPointIndex = numValues - 1;

         // remove point

         if (UI.isCtrlKey(mouseEvent)) {

            // remove all inner points

//            curveValues.removeKnot(_hoveredPointIndex);
//
//            fireEvent_PointIsModified();

         } else {

            if (_hoveredPointIndex != 0 && _hoveredPointIndex != lastPointIndex) {

               curveValues.removeKnot(_hoveredPointIndex);

               fireEvent_PointIsModified();
            }
         }
      }
   }

   private void onMouseDown(final MouseEvent mouseEvent) {

      if (_isEnabled == false) {
         return;
      }

      if (_hoveredPointIndex != -1) {

         _isPointDragged = true;

         redraw();

      } else {

         // add new point

         final float mouseX = mouseEvent.x;
         final float mouseY = mouseEvent.y;

         final float xValueRel = mouseX / _graphWidth;
         final float yValueRel = 1 - (mouseY / _graphHeight);

         // ensure 0...1
         final float xRel = ImageMath.clamp01(xValueRel);
         final float yRel = ImageMath.clamp01(yValueRel);

         final CurveValues curveValues = _toneCurvesFilter.getCurves().getActiveCurve().curveValues;

         _hoveredPointIndex = curveValues.addKnot(xRel, yRel);
         _isPointDragged = true;

         fireEvent_PointIsModified();

         redraw();
      }
   }

   private void onMouseExit(final MouseEvent mouseEvent) {

   }

   private void onMouseMove(final MouseEvent mouseEvent) {

      if (_isEnabled == false) {
         return;
      }

      final int oldHoveredPointIndex = _hoveredPointIndex;

      if (_isPointDragged) {

         if (_hoveredPointIndex != -1) {

            // point is dragged

            final float mouseX = mouseEvent.x;
            final float mouseY = mouseEvent.y;

            final float xValueRel = mouseX / _graphWidth;
            final float yValueRel = 1 - (mouseY / _graphHeight);

            // ensure 0...1
            final float xRel = ImageMath.clamp01(xValueRel);
            final float yRel = ImageMath.clamp01(yValueRel);

            final CurveValues curveValues = _toneCurvesFilter.getCurves().getActiveCurve().curveValues;

            curveValues.allValuesX[_hoveredPointIndex] = xRel;
            curveValues.allValuesY[_hoveredPointIndex] = yRel;

            fireEvent_PointIsModified();
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

      if (_isEnabled == false) {
         return;
      }

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

      _graphWidth = canvasWidth;
      _graphHeight = canvasHeight;
      _xUnitWidth = _graphWidth / 256f;

      _allPaintedCurvePoints.clear();

      _adjustedColor = new Color(0xff, 0xff, 0x0);

      paint_10_Histogram(gc);

      if (_isSetTonality && _isEnabled) {
         paint_20_ToneCurve(gc);
      }
   }

   private void paint_10_Histogram(final GC gc) {

      gc.setBackground(UI.SYS_COLOR_GRAY);
      gc.fillRectangle(0, 0, _graphWidth, _graphHeight);

      if (_isSetTonality && _isEnabled) {
         gc.setAlpha(0x60);
      }

      float devX0 = 0;
      float devX1 = _xUnitWidth;
      int barWidth;

      final int darkGrey = 0x66;
      gc.setBackground(new Color(darkGrey, darkGrey, darkGrey));

      int maxLuminance = _maxLuminance_Default == 0 ? 1 : _maxLuminance_Default;

      for (final int luminance : _luminancesDefault) {

         final int barHeight = _graphHeight * luminance / maxLuminance;

         final float diffX = devX1 - devX0;
         barWidth = (int) (diffX + 0.5);

         final int devX = (int) (devX0);
         final int devY = _graphHeight - barHeight;

         gc.fillRectangle(devX, devY, barWidth, barHeight);

         devX0 = devX + barWidth;
         devX1 += _xUnitWidth;
      }

      if (_isSetTonality && _isEnabled) {

         gc.setAlpha(0x80);
         gc.setBackground(_adjustedColor);

         devX0 = 0;
         devX1 = _xUnitWidth;

         maxLuminance = _maxLuminanceAdjusted == 0 ? 1 : _maxLuminanceAdjusted;

         for (final int luminance : _luminancesAdjusted) {

            final int barHeight = _graphHeight * luminance / maxLuminance;
            final float diffX = devX1 - devX0;
            barWidth = (int) (diffX);

            final int devX = (int) (devX0);
            final int devY = _graphHeight - barHeight;

            gc.fillRectangle(

                  devX,
                  devY,

                  barWidth,
                  barHeight);

            devX0 = devX + barWidth;
            devX1 += _xUnitWidth;
         }
      }
   }

   private void paint_20_ToneCurve(final GC gc) {

      if (_toneCurvesFilter == null
            || _toneCurvesFilter.getCurvesFilter() == null
            || _toneCurvesFilter.getCurvesFilter().getRedTable() == null) {

         return;
      }

      final CurveValues curveValues = _toneCurvesFilter.getCurves().getActiveCurve().curveValues;
      final int[] allCurveValues = _toneCurvesFilter.getCurvesFilter().getRedTable();

      final float[] allPointValuesX = curveValues.allValuesX;
      final float[] allPointValuesY = curveValues.allValuesY;

      final int numValues = allPointValuesX.length;

      final int[] allDevX = new int[numValues];
      final int[] allDevY = new int[numValues];

      for (int valueIndex = 0; valueIndex < numValues; valueIndex++) {

         final float valueX = allPointValuesX[valueIndex];
         final float valueY = allPointValuesY[valueIndex];

         final int devXPoint = (int) (_graphWidth * valueX);
         final int devYPoint = (int) (_graphHeight - (_graphHeight * valueY));

         allDevX[valueIndex] = devXPoint;
         allDevY[valueIndex] = devYPoint;
      }

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

      // draw default line
      final int darkGrey = 0x40;
      gc.setLineWidth(1);
      gc.setForeground(new Color(darkGrey, darkGrey, darkGrey));
      gc.drawLine(

            allDevX[0],
            _graphHeight,
            allDevX[numValues - 1],
            0);

      // draw curve graph
      gc.setLineWidth(2);
      gc.setForeground(_adjustedColor);
      gc.drawPath(curvePath);

      curvePath.dispose();

      // draw point values
      for (int valueIndex = 0; valueIndex < numValues; valueIndex++) {
         paint_30_TonePoint(gc, allDevX[valueIndex], allDevY[valueIndex]);
      }

      if (_hoveredPointIndex != -1) {

         gc.setLineWidth(2);
         final Rectangle hoveredRectangle = _allPaintedCurvePoints.get(_hoveredPointIndex);

         if (_isPointDragged) {

            // paint dragged point

            gc.setBackground(_adjustedColor);
            gc.fillArc(

                  hoveredRectangle.x,
                  hoveredRectangle.y,

                  hoveredRectangle.width,
                  hoveredRectangle.height,

                  0,
                  360);

         } else {

            // paint hovered point

            gc.setForeground(new Color(0x0, 0xff, 0x0));
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

   private void paint_30_TonePoint(final GC gc,
                                   final int devXPoint,
                                   final int devYPoint) {

      final int pointRadius = POINT_RADIUS;
      final int pointRadius2 = pointRadius / 2;

      final int devX = devXPoint - pointRadius2;
      final int devY = devYPoint - pointRadius2;

      gc.setForeground(new Color(0x0, 0x0, 0x0));
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

   @Override
   public void paintControl(final PaintEvent paintEvent) {

      paint(paintEvent);
   }

   public void setAdjustedImage(final BufferedImage adjustedImage) {

      _maxLuminanceAdjusted = computeLuminance_Adjusted(adjustedImage, _luminancesAdjusted);
   }

   @Override
   public void setEnabled(final boolean isEnabled) {

      if (isDisposed()) {
         return;
      }

      _isEnabled = isEnabled;

      redraw();
   }

   public void setImage(final Image image, final Rectangle2D.Float relCropArea) {

      _hoveredPointIndex = -1;

      if (image == null || image.isDisposed()) {

         // reset image
         _defaultImageData = null;

         return;
      }

      _defaultImageData = image.getImageData();
      _relCropArea = relCropArea;

      _maxLuminance_Default = computeLuminance_Default(_defaultImageData, _luminancesDefault);

      redraw();
   }

   public void updateCropArea(final Rectangle2D.Float relCropArea) {

      if (_defaultImageData == null || isDisposed()) {
         return;
      }

      _relCropArea = relCropArea;

      _maxLuminance_Default = computeLuminance_Default(_defaultImageData, _luminancesDefault);
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

      if (isDisposed()) {
         return;
      }

      _isSetTonality = photo.isSetTonality;
      _toneCurvesFilter = photo.getToneCurvesFilter();

      // update in UI thread
      getDisplay().asyncExec(() -> {

         redraw();
      });
   }
}
