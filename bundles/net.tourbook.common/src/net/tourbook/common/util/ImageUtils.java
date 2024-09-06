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

package net.tourbook.common.util;

import java.awt.image.BufferedImage;

import net.tourbook.common.UI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;
import org.imgscalr.Scalr.Rotation;

/**
 * Original code: org.sharemedia.utils.ImageUtils
 */
public class ImageUtils {

   /**
    * For images with a transparent layer, this will keep the existing
    * transparency.
    * Original code: https://stackoverflow.com/a/63703052
    *
    * @param sourceImageData
    * @param imgWidth
    * @param imgHeight
    *
    * @return
    */
   private static ImageData copyImageTransparencyData(final ImageData sourceImageData,
                                                      final int imgWidth,
                                                      final int imgHeight) {

      if (sourceImageData.alphaData == null) {
         return null;
      }

      final ImageData destData = new ImageData(imgWidth, imgHeight, sourceImageData.depth, sourceImageData.palette);

      final int destinationImageWidth = destData.width;
      final int sourceImageDataWidth = sourceImageData.width;
      destData.alphaData = new byte[destinationImageWidth * destData.height];

      for (int destRow = 0; destRow < destData.height; destRow++) {

         final int origRow = destRow * sourceImageData.height / destData.height;
         final int destination = destRow * destinationImageWidth;
         final int origin = origRow * sourceImageDataWidth;

         for (int destCol = 0; destCol < destinationImageWidth; destCol++) {

            final int origCol = destCol * sourceImageDataWidth / destinationImageWidth;
            destData.alphaData[destination + destCol] = sourceImageData.alphaData[origin + origCol];
         }
      }

      return destData;
   }

   /**
    * Create an AWT image from a SWT image, the SWT image is disposed
    *
    * @param swtImage
    *
    * @return
    */
   public static BufferedImage createAWTImage(final Image swtImage) {

      final BufferedImage awtImage = ImageConverter.convertIntoAWT(swtImage);

      UI.disposeResource(swtImage);

      return awtImage;
   }

   public static double getBestRatio(final int originalX, final int originalY, final int maxX, final int maxY) {

      final double widthRatio = (double) originalX / (double) maxX;
      final double heightRatio = (double) originalY / (double) maxY;

      final double bestRatio = widthRatio > heightRatio ? widthRatio : heightRatio;

      return bestRatio;
   }
   public static Point getBestSize(final int originalX, final int originalY, final int maxX, final int maxY) {

      final double bestRatio = getBestRatio(originalX, originalY, maxX, maxY);

      final int newWidth = (int) (originalX / bestRatio);
      final int newHeight = (int) (originalY / bestRatio);

      return new Point(newWidth, newHeight);
   }

   /**
    * Returns a new scaled image. new Image must be disposed after use.
    *
    * @param image
    * @param width
    * @param height
    *
    * @return
    */
   public static Image resize(final Display display, final Image image, final int width, final int height) {

      return resize(display, image, width, height, SWT.ON, SWT.HIGH, null, false);
   }

   public static/* synchronized */Image resize(final Display display,
                                               final Image srcImage,
                                               final int newWidth,
                                               final int newHeight,
                                               final int antialias,
                                               final int interpolation,
                                               final Rotation exifRotation,
                                               final boolean isRotateImageAutomatically) {

      if (srcImage == null) {
         return null;
      }

      final Rectangle originalImageBounds = srcImage.getBounds();
      final int originalWidth = originalImageBounds.width;
      final int originalHeight = originalImageBounds.height;

      final int srcWidth = originalWidth;
      final int srcHeight = originalHeight;
      final int destWidth = newWidth;
      final int destHeight = newHeight;

      int imgWidth = newWidth;
      int imgHeight = newHeight;

      // OSX is rotating the image automatically
      boolean isNoAutoRotate = UI.IS_OSX == false;

      isNoAutoRotate |= isRotateImageAutomatically == false;

      if (isNoAutoRotate) {

         if (exifRotation == Rotation.CW_90 || exifRotation == Rotation.CW_270) {
            // swap width/height
            imgWidth = newHeight;
            imgHeight = newWidth;
         }
      }

      Image scaledImage = new Image(display, imgWidth, imgHeight);

      final ImageData scaledImageWithTransparencyData =
            copyImageTransparencyData(srcImage.getImageData(), imgWidth, imgHeight);
      if (scaledImageWithTransparencyData != null) {

         UI.disposeResource(scaledImage);
         scaledImage = new Image(display, scaledImageWithTransparencyData);
      }

      //Resize the image
      final GC gc = new GC(scaledImage);
      Transform transformation = null;
      try {
         gc.setAdvanced(true);

         gc.setAntialias(antialias);
         gc.setInterpolation(interpolation);
//       gc.setAntialias(SWT.ON);
//       gc.setInterpolation(SWT.HIGH);

         int destX = 0;
         int destY = 0;

         if (exifRotation != null && isNoAutoRotate) {

            final int imgWidth2 = imgWidth / 2;
            final int imgHeight2 = imgHeight / 2;

            transformation = new Transform(display);
            transformation.translate(imgWidth2, imgHeight2);

            if (exifRotation == Rotation.CW_90) {

               transformation.rotate(90);

               destX = -imgHeight2;
               destY = -imgWidth2;

            } else if (exifRotation == Rotation.CW_180) {

               // this case is not yet tested

               transformation.rotate(180);

               destX = -imgWidth2;
               destY = -imgHeight2;

            } else if (exifRotation == Rotation.CW_270) {

               transformation.rotate(270);

               destX = -imgHeight2;
               destY = -imgWidth2;
            }

            gc.setTransform(transformation);
         }

         gc.drawImage(srcImage, //
               0,
               0,
               srcWidth,
               srcHeight,
               //
               destX,
               destY,
               destWidth,
               destHeight);
      } finally {

         // ensure resources are disposed when an error occurs

         gc.dispose();

         UI.disposeResource(transformation);
         UI.disposeResource(srcImage);
      }

      return scaledImage;
   }
}
