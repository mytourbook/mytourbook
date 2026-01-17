/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import net.tourbook.common.UI;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Rotation;

/**
 * Original code: org.sharemedia.utils.ImageUtils
 */
public class ImageUtils {

   private static String _allImageExtensions;

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

   /**
    * Creates an image which must be disposed when not needed any more
    *
    * @param imageFilePath
    * @param imageSize
    *
    * @return
    *
    * @throws IOException
    */
   public static Image createImage(final String imageFilePath, final int imageSize) throws IOException {

      if (StringUtils.isNullOrEmpty(imageFilePath)
            || new File(imageFilePath).exists() == false) {

         return null;
      }

      /*
       * Load image
       */
      BufferedImage awtImage;

      try {

         awtImage = ImageIO.read(new File(imageFilePath));

      } catch (final IOException ioException) {

         StatusUtil.log("Image cannot be loaded: \"%s\"".formatted(imageFilePath), ioException); //$NON-NLS-1$

         throw ioException;
      }

      final int originalImageWidth = awtImage.getWidth();
      final int originalImageHeight = awtImage.getHeight();

      if (originalImageWidth >= imageSize || originalImageHeight >= imageSize) {

         // the original image is larger than the required image -> resize it

         final org.eclipse.swt.graphics.Point bestSize = ImageUtils.getBestSize(
               originalImageWidth,
               originalImageHeight,
               imageSize,
               imageSize);

         final int scaleWidth = bestSize.x;
         final int scaledHeight = bestSize.y;

         final int maxSize = Math.max(scaleWidth, scaledHeight);
         final BufferedImage scaledHQImage = Scalr.resize(awtImage, Method.QUALITY, maxSize);

         awtImage.flush();

         awtImage = scaledHQImage;
      }

      /*
       * Rotate image
       */
      final Rotation rotation = getImageRotation(imageFilePath);

      if (rotation != null) {

         // rotate image according to the EXIF flag

         final BufferedImage rotatedImage = Scalr.rotate(awtImage, rotation);

         awtImage.flush();

         awtImage = rotatedImage;
      }

      final Image swtImage = new Image(PlatformUI.getWorkbench().getDisplay(),
            new CustomScalingImageDataProvider(awtImage));

      awtImage.flush();

      return swtImage;
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
    * The strings are platform specific. For example, on some platforms, an extension filter string
    * is typically of the form "*.extension", where "*.*" matches all files. For filters with
    * multiple extensions, use semicolon as a separator, e.g. "*.jpg;*.png".
    *
    * @return Returns a string with file extensions for all supported image readers
    */
   public static String getImageExtensions() {

      if (_allImageExtensions != null) {
         return _allImageExtensions;
      }

      final String[] allImageExtensions = ImageIO.getReaderFormatNames();

      final StringBuilder sb = new StringBuilder();

      for (int formatIndex = 0; formatIndex < allImageExtensions.length; formatIndex++) {

         if (formatIndex > 0) {
            sb.append(';');
         }

         final String imageExtension = allImageExtensions[formatIndex];
         sb.append("*." + imageExtension);
      }

      _allImageExtensions = sb.toString();

      return _allImageExtensions;
   }

   public static Rotation getImageRotation(final String imageFilePath) {

      Rotation rotation = null;

      try {

         // load metadata
         final ImageMetadata imageMetadata = Imaging.getMetadata(new File(imageFilePath));
         if (imageMetadata instanceof JpegImageMetadata) {

            final JpegImageMetadata jpegMetadata = (JpegImageMetadata) imageMetadata;
            final TiffField field = jpegMetadata.findExifValueWithExactMatch(TiffTagConstants.TIFF_TAG_ORIENTATION);

            if (field != null) {

               final int orientation = field.getIntValue();

// SET_FORMATTING_OFF

               if (       orientation == 6) {   rotation = Rotation.CW_90;
               } else if (orientation == 3) {   rotation = Rotation.CW_180;
               } else if (orientation == 8) {   rotation = Rotation.CW_270;
               }

// SET_FORMATTING_ON
            }
         }

      } catch (final IOException e) {

         StatusUtil.log(e);
      }

      return rotation;
   }

   public static String imageToBase64(final Image image) {

      byte[] imageBytes = null;
      final BufferedImage bufferedImage = ImageConverter.convertIntoAWT(image);

      try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {

         ImageIO.write(bufferedImage, "png", output); //$NON-NLS-1$
         imageBytes = output.toByteArray();

      } catch (final IOException e) {
         StatusUtil.log(e);
      }

      final byte[] encoded = Base64.getEncoder().encode(imageBytes);
      return new String(encoded);
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
