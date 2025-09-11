/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
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
/**
 * @author Alfred Barten
 */
package net.tourbook.common.color;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.util.CustomScalingImageDataProvider;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Contain the profile image and all rgb vertices.
 */
public class SRTMProfileImage extends ProfileImage implements Cloneable {

   private static int MAX_VERTICES_VALUE = 8850;

   @Override
   public Image createImage(int imageWidth, int imageHeight, final boolean isHorizontal) {

      // ensure min image size
      imageWidth = imageWidth < IMAGE_MIN_WIDTH ? IMAGE_MIN_WIDTH : imageWidth;
      imageHeight = imageHeight < IMAGE_MIN_HEIGHT ? IMAGE_MIN_HEIGHT : imageHeight;

      final int imageWidthScaled = (int) (imageWidth * UI.HIDPI_SCALING);
      final int imageHeightScaled = (int) (imageHeight * UI.HIDPI_SCALING);

      final BufferedImage awtImage = new BufferedImage(imageWidthScaled, imageHeightScaled, BufferedImage.TYPE_4BYTE_ABGR);
      final Graphics2D g2d = awtImage.createGraphics();

      try {

         /*
          * Draw colors
          */

         final ArrayList<RGBVertex> rgbVertices = getRgbVertices();

         final long maxValue = rgbVertices.isEmpty() //
               ? MAX_VERTICES_VALUE
               : rgbVertices.get(rgbVertices.size() - 1).getValue();

         final int horizontalSize = isHorizontal ? imageWidthScaled : imageHeightScaled + 1;
         final int verticalSize = isHorizontal ? imageHeightScaled : imageWidthScaled;

         for (int x = 0; x < horizontalSize; x++) {

            final long value = maxValue * x / horizontalSize;

            final int rgb = getRGB(value);

            final byte blue = (byte) ((rgb & 0xFF0000) >> 16);
            final byte green = (byte) ((rgb & 0xFF00) >> 8);
            final byte red = (byte) ((rgb & 0xFF) >> 0);

            g2d.setColor(new java.awt.Color(red & 0xFF, green & 0xFF, blue & 0xFF));

            if (isHorizontal) {

               // draw horizontal

               int x1 = horizontalSize - x - 1;
               int x2 = horizontalSize - x - 1;

               x1 = x2 = x;

               final int y1 = 0;
               final int y2 = verticalSize;

               g2d.drawLine(x1, y1, x2, y2);

            } else {

               // draw vertical

               final int x1 = 0;
               final int x2 = verticalSize;

               final int y1 = horizontalSize - x - 1;
               final int y2 = horizontalSize - x - 1;

               g2d.drawLine(x1, y1, x2, y2);
            }
         }

         /*
          * Draw text
          */

         final Font scaled4kFont = UI.getAWT4kScaledDefaultFont();

         g2d.setFont(scaled4kFont);
         g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

         final FontMetrics fontMetrics = g2d.getFontMetrics();
         final int fontHeight = fontMetrics.getHeight();

         final AffineTransform at = new AffineTransform();

         for (final RGBVertex vertex : rgbVertices) {

            final long elevation = vertex.getValue();

            if (elevation < 0) {
               continue;
            }

            final int rgb = getRGB(elevation);
            final byte blue = (byte) ((rgb & 0xFF0000) >> 16);
            final byte green = (byte) ((rgb & 0xFF00) >> 8);
            final byte red = (byte) ((rgb & 0xFF) >> 0);

            final int devPos = maxValue == 0
                  ? 0
                  : (int) (elevation * horizontalSize / maxValue);

//          @param m00 the X coordinate scaling element of the 3x3 matrix

//          @param m10 the Y coordinate shearing element of the 3x3 matrix
//          @param m01 the X coordinate shearing element of the 3x3 matrix

//          @param m11 the Y coordinate scaling element of the 3x3 matrix

//          @param m02 the X coordinate translation element of the 3x3 matrix
//          @param m12 the Y coordinate translation element of the 3x3 matrix

            if (isHorizontal) {

               int devX = (int) (devPos

                     - fontHeight * 0.2f)

               ;

               // ensure min/max value visibilty
               if (devX > horizontalSize - 20) {
                  devX = horizontalSize - 20;
               }
               if (devX < 5) {
                  devX = 5;
               }

               // rotate by -90 degrees

               at.setTransform(

                     0,

                     -1,
                     1,

                     0,

                     devX,
                     5);

               // rotates 90 degree clockwise
               at.quadrantRotate(2);

            } else {

               // vertical

               int devY = (int) (horizontalSize - devPos

                     + (fontHeight * 0.2f))

               ;

               // ensure min/max value visibilty
               if (devY < 20) {
                  devY = 20;
               }
               if (devY > horizontalSize - 10) {
                  devY = horizontalSize - 10;
               }

               at.setTransform(

                     1,

                     0,
                     0,

                     1,

                     5,
                     devY);
            }

            g2d.setColor(ColorUtil.getContrastColorAWT(red & 0xFF, green & 0xFF, blue & 0xFF, 0xff));

            g2d.setTransform(at);
            g2d.drawString(UI.EMPTY_STRING + elevation, 0, 0);
         }

      } finally {

         g2d.dispose();
      }

      final Image swtImage = new Image(Display.getCurrent(), new CustomScalingImageDataProvider(awtImage));

      return swtImage;
   }

   @Override
   public int getRGB(final long value) {

      final RGBVertex[] vertexArray = getRgbVerticesArray();

      final int vertexSize = vertexArray.length;

      if (vertexSize == 0) {
         return 0xFFFFFF;
      }

      if (vertexSize == 1) {

         final RGB rgb = vertexArray[0].getRGB();

         return (//
         (rgb.blue & 0xFF) << 16)
               + ((rgb.green & 0xFF) << 8)
               + (rgb.red & 0xFF);
      }

      for (int ix = vertexSize - 2; ix >= 0; ix--) {

         final RGBVertex vertex = vertexArray[ix];

         if (value > vertex.getValue()) {

            final RGBVertex vertex2 = vertexArray[ix + 1];

            final RGB rgb1 = vertex.getRGB();
            final RGB rgb2 = vertex2.getRGB();

            final long elev1 = vertex.getValue();
            final long elev2 = vertex2.getValue();

            final long dElevG = elev2 - elev1;
            final long dElev1 = value - elev1;
            final long dElev2 = elev2 - value;

            int red = (int) ((double) (rgb2.red * dElev1 + rgb1.red * dElev2) / dElevG);
            int green = (int) ((double) (rgb2.green * dElev1 + rgb1.green * dElev2) / dElevG);
            int blue = (int) ((double) (rgb2.blue * dElev1 + rgb1.blue * dElev2) / dElevG);

            if (red > 0xFF) {
               red = 0xFF;
            }
            if (green > 0xFF) {
               green = 0xFF;
            }
            if (blue > 0xFF) {
               blue = 0xFF;
            }

            if (red < 0) {
               red = 0;
            }
            if (green < 0) {
               green = 0;
            }
            if (blue < 0) {
               blue = 0;
            }

            return (//
            (blue & 0xFF) << 16)
                  + ((green & 0xFF) << 8)
                  + (red & 0xFF);
         }
      }

      return 0xFF005F;
   }

}
