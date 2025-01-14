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

import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import net.tourbook.common.UI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
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

   public static final int NUM_BINS       = 256;

   private int             _maxLuminance;
   private int[]           _allLuminances = new int[NUM_BINS];

   private ImageData       _imageData;

   private int             _threePoint_Dark;
   private int             _threePoint_Middle;
   private int             _threePoint_Bright;

   public Histogram(final Composite parent) {

      super(parent, SWT.DOUBLE_BUFFERED | SWT.NO_FOCUS);

      addListener();
   }

   private void addListener() {

      addPaintListener(this);

      addMouseListener(new MouseAdapter() {
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

      addMouseTrackListener(new MouseTrackAdapter() {
         @Override
         public void mouseExit(final MouseEvent e) {
            onMouseExit(e);
         }
      });
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

   private void onMouseDown(final MouseEvent e) {

   }

   private void onMouseExit(final MouseEvent e) {

   }

   private void onMouseMove(final MouseEvent e) {

   }

   @Override
   public void paintControl(final PaintEvent paintEvent) {

      final GC gc = paintEvent.gc;

      final Rectangle canvasBounds = getBounds();

      final int canvasWidth = canvasBounds.width;
      final int canvasHeight = canvasBounds.height;

      final int sliderbarHeight = 20;

      final int graphWidth = canvasWidth - 2;
      final int graphHeight = canvasHeight - sliderbarHeight;

      gc.setBackground(UI.SYS_COLOR_GRAY);
      gc.fillRectangle(0, 0, graphWidth - 0, graphHeight - 0);

      final int lineWidth = graphWidth / 256;

      gc.setForeground(UI.SYS_COLOR_DARK_GRAY);
      gc.setLineWidth(lineWidth + 1);

      for (int lumIndex = 0; lumIndex < _allLuminances.length; lumIndex++) {

         final int luminance = _allLuminances[lumIndex];

         final int maxLuminance = _maxLuminance == 0 ? 1 : _maxLuminance;

         final int devX = graphWidth * lumIndex / 256;
         final int devY = graphHeight * luminance / maxLuminance;

         gc.drawLine(

               devX + lineWidth,
               graphHeight,
               devX + lineWidth,
               graphHeight - devY);
      }
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

      _threePoint_Dark = photo.threePoint_Dark;
      _threePoint_Middle = photo.threePoint_Middle;
      _threePoint_Bright = photo.threePoint_Bright;

      redraw();
   }

   public void updateCropArea(final Rectangle2D.Float relCropArea) {

      if (_imageData == null || isDisposed()) {
         return;
      }

      computeLuminance(_imageData, relCropArea);

      redraw();
   }
}
