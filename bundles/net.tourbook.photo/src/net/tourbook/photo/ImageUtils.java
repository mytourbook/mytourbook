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
package net.tourbook.photo;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileFilter;

import net.tourbook.common.UI;

import org.apache.commons.imaging.Imaging;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * Original code: org.sharemedia.utils.ImageUtils
 */
public class ImageUtils {

   public static FileFilter createImageFileFilter() {

      return pathname -> {

         if (pathname.isDirectory()) {
            return false;
         }

         if (pathname.isHidden()) {
            return false;
         }

         final String name = pathname.getName();
         if (name == null || name.length() == 0) {
            return false;
         }

         if (name.startsWith(UI.SYMBOL_DOT)) {
            return false;
         }

         if (Imaging.hasImageFileExtension(pathname)) {
            return true;
         }

         return false;
      };
   }

   /**
    * @param image
    * @param format
    *
    * @return Returns a formatted image, the format parameter can have one of the following values:
    *         <p>
    *         <dl>
    *         <dt><code>IMAGE_BMP</code></dt>
    *         <dd>Windows BMP file format, no compression</dd>
    *         <dt><code>IMAGE_BMP_RLE</code></dt>
    *         <dd>Windows BMP file format, RLE compression if appropriate</dd>
    *         <dt><code>IMAGE_GIF</code></dt>
    *         <dd>GIF file format</dd>
    *         <dt><code>IMAGE_ICO</code></dt>
    *         <dd>Windows ICO file format</dd>
    *         <dt><code>IMAGE_JPEG</code></dt>
    *         <dd>JPEG file format</dd>
    *         <dt><code>IMAGE_PNG</code></dt>
    *         <dd>PNG file format</dd>
    *         </dl>
    */
   public static byte[] formatImage(final Image image, final int format) {

      if (image == null) {
         return null;
      }

      final ImageLoader il = new ImageLoader();
      il.data = new ImageData[] { image.getImageData() };
      final ByteArrayOutputStream bas = new ByteArrayOutputStream();

      il.save(bas, format);

      return bas.toByteArray();
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
      // logger.debug("newWidth " + newWidth + " newHeight " + newHeight);

      return new Point(newWidth, newHeight);
   }

   @SuppressWarnings("unused")
   private static Point getBestSize(final Point original, final Point max) {
      return getBestSize(original.x, original.y, max.x, max.y);
   }

   public static boolean isResizeRequired(final Image image, final int width, final int height) {
      final Rectangle bounds = image.getBounds();
      return !(bounds.width == width && bounds.height == height);
   }

   public static boolean isResizeRequiredAWT(final BufferedImage img, final int width, final int height) {
      return !(img.getWidth() == width && img.getHeight() == height);
   }

   @SuppressWarnings("unused")
   private static ImageData resize(final Display display,
                                   final ImageData imageData,
                                   final int width,
                                   final int height,
                                   final boolean antiAliasing) {

      if (imageData == null) {
         return null;
      }

      if (imageData.width == width && imageData.height == height) {
         return imageData;
      }

      if (antiAliasing) {
         Image tmpImage = null;
         final Image fullImage = new Image(display, imageData);
         ImageData result = null;
         tmpImage = net.tourbook.common.util.ImageUtils.resize(display, fullImage, width, height);

         result = tmpImage.getImageData();
         tmpImage.dispose();
         fullImage.dispose();
         return result;
      }

      return imageData.scaledTo(width, height);
   }

   /**
    * Resize an image to the best fitting size. Old and new Image (result)must be disposed after
    * use.
    *
    * @param img
    * @param maxWidth
    * @param maxHeight
    *
    * @return
    */
   @SuppressWarnings("unused")
   private static Image resizeBestSize(final Display display, final Image img, final int maxWidth, final int maxHeight) {

      if (img == null) {
         return null;
      }

      final Rectangle imageBounds = img.getBounds();

      // Calculate best size
      final Point newSize = getBestSize(imageBounds.width, imageBounds.height, maxWidth, maxHeight);

      // Resize image
      return net.tourbook.common.util.ImageUtils.resize(display, img, newSize.x, newSize.y);
   }

   @SuppressWarnings("unused")
   private Image resize(final int w, final int h, final Image img) {

      final Image newImage = new Image(Display.getDefault(), w, h);
      final GC gc = new GC(newImage);
      {
         gc.setAntialias(SWT.ON);
         gc.setInterpolation(SWT.HIGH);
         gc.drawImage(img, 0, 0, img.getBounds().width, img.getBounds().height, 0, 0, w, h);
      }
      gc.dispose();
      img.dispose();

      return newImage;
   }
}
