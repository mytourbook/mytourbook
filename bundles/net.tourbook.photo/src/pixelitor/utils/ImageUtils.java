/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.utils;

import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static java.awt.Transparency.TRANSLUCENT;
import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.awt.image.DataBuffer.TYPE_INT;
import static pixelitor.utils.Threads.onPool;

import com.jhlabs.image.ImageMath;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import pixelitor.utils.debug.Debug;

/*
 * The original code is from pixelitor.utils.ImageUtils
 */

/**
 * Static image-related utility methods
 */
public class ImageUtils {

   private static final GraphicsConfiguration graphicsConfig    = GraphicsEnvironment
         .getLocalGraphicsEnvironment()
         .getDefaultScreenDevice()
         .getDefaultConfiguration();

   private static final ColorModel            defaultColorModel = graphicsConfig.getColorModel();

   private ImageUtils() {}

   public static BufferedImage applyTransform(final BufferedImage src, final AffineTransform at, final int targetWidth, final int targetHeight) {

      assert targetWidth > 0 && targetHeight > 0 : "target = " + targetWidth + "x" + targetHeight; //$NON-NLS-1$ //$NON-NLS-2$
      final BufferedImage newImage = new BufferedImage(targetWidth, targetHeight, TYPE_INT_ARGB);
      final Graphics2D g = newImage.createGraphics();
      g.setTransform(at);
      if (targetWidth > src.getWidth() || targetHeight > src.getHeight()) {
         g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
      } else {
         g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
      }
      g.drawImage(src, 0, 0, null);
      g.dispose();
      return newImage;
   }

   /**
    * Returns the number of steps (progress tracking work units)
    * required for smooth enlargement.
    */
   public static int calcNumStepsForEnlargeSmooth(final double resizeFactor, final double step) {
      double progress = 1.0;
      final double lastStep = resizeFactor / step;
      int retVal = 1; // for the final step
      while (progress < lastStep) {
         progress = progress * step;
         retVal++;
      }
      return retVal;
   }

   /**
    * Calculates the target dimensions if an image needs to be resized
    * to fit into a box of a given size without distorting the aspect ratio.
    */
   public static Dimension calcThumbDimensions(final int srcWidth, final int srcHeight, final int boxSize, final boolean upscale) {
      int thumbWidth;
      int thumbHeight;
      if (srcWidth > srcHeight) { // landscape
         if (upscale || srcWidth > boxSize) {
            thumbWidth = boxSize;
            final double ratio = (double) srcWidth / srcHeight;
            thumbHeight = (int) (boxSize / ratio);
         } else {
            // the image already fits in the box and no up-scaling is needed
            thumbWidth = srcWidth;
            thumbHeight = srcHeight;
         }
      } else { // portrait
         if (upscale || srcHeight > boxSize) {
            thumbHeight = boxSize;
            final double ratio = (double) srcHeight / srcWidth;
            thumbWidth = (int) (boxSize / ratio);
         } else {
            // the image already fits in the box and no up-scaling is needed
            thumbWidth = srcWidth;
            thumbHeight = srcHeight;
         }
      }

      if (thumbWidth == 0) {
         thumbWidth = 1;
      }
      if (thumbHeight == 0) {
         thumbHeight = 1;
      }

      return new Dimension(thumbWidth, thumbHeight);
   }

   public static BufferedImage convertToARGB(final BufferedImage src, final boolean flushSrc) {
      assert src != null;

      final BufferedImage dest = copyTo(TYPE_INT_ARGB, src);

      if (flushSrc) {
         src.flush();
      }

      return dest;
   }

   public static BufferedImage convertToARGB_PRE(final BufferedImage src, final boolean flushSrc) {
      assert src != null;

      final BufferedImage dest = copyTo(TYPE_INT_ARGB_PRE, src);

      if (flushSrc) {
         src.flush();
      }

      return dest;
   }

   public static BufferedImage convertToGrayscaleImage(final BufferedImage src) {
      return copyTo(TYPE_BYTE_GRAY, src);
   }

   // From the Filthy Rich Clients book

   private static BufferedImage convertToInterleaved(final BufferedImage src, final boolean addAlpha) {
      final int numChannels = addAlpha ? 4 : 3;
      final WritableRaster wr = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
            src.getWidth(),
            src.getHeight(),
            numChannels,
            null);
      final ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
      final ComponentColorModel ccm = new ComponentColorModel(sRGB,
            addAlpha,
            false,
            Transparency.OPAQUE,
            DataBuffer.TYPE_BYTE);

      final BufferedImage dest = new BufferedImage(ccm, wr, false, null);
      final Graphics2D g = dest.createGraphics();
      g.drawImage(src, 0, 0, null);
      g.dispose();

      return dest;
   }

   public static BufferedImage convertToInterleavedRGB(final BufferedImage src) {
      return convertToInterleaved(src, false);
   }

   public static BufferedImage convertToInterleavedRGBA(final BufferedImage src) {
      return convertToInterleaved(src, true);
   }

   public static BufferedImage convertToRGB(final BufferedImage src) {
      return convertToRGB(src, false);
   }

   public static BufferedImage convertToRGB(final BufferedImage src, final boolean flushSrc) {
      assert src != null;

      final BufferedImage dest = copyTo(TYPE_INT_RGB, src);

      if (flushSrc) {
         src.flush();
      }

      return dest;
   }

   // There are two cases when this method can't be used to
   // copy an image: (1) for images with an IndexColorModel
   // this returns an image with a shared raster (jdk bug?)
   // (2) for an image created with BufferedImage.getSubimage
   // it throws an exception if the raster doesn't start at (0, 0).

   // Can copy an image that was created by BufferedImage.getSubimage
   public static BufferedImage copySubImage(final BufferedImage src) {
      return copyTo(src.getType(), src);
   }

   /**
    * Unlike BufferedImage.getSubimage, this method creates a copy of the data
    */
   public static BufferedImage copySubImage(final BufferedImage src, final Rectangle bounds) {
      assert src != null;
      assert bounds != null;

      final Rectangle intersection = SwingUtilities.computeIntersection(
            0,
            0,
            src.getWidth(),
            src.getHeight(), // image bounds
            bounds);

      if (intersection.width <= 0 || intersection.height <= 0) {
         throw new IllegalStateException("empty intersection: bounds = " + bounds //$NON-NLS-1$
               + ", src width = " + src.getWidth() //$NON-NLS-1$
               + ", src height = " + src.getHeight() //$NON-NLS-1$
               + ", intersection = " + intersection); //$NON-NLS-1$
      }

      final Raster copyRaster = src.getData(intersection); // a copy
      final Raster startingFrom00 = copyRaster.createChild(
            intersection.x,
            intersection.y,
            intersection.width,
            intersection.height,
            0,
            0,
            null);

      return new BufferedImage(src.getColorModel(),
            (WritableRaster) startingFrom00,
            src.isAlphaPremultiplied(),
            null);
   }

   public static BufferedImage copyTo(final int newType, final BufferedImage src) {
      final BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), newType);
      final Graphics2D g = dest.createGraphics();
      g.drawImage(src, 0, 0, null);
      g.dispose();
      return dest;
   }

   public static BufferedImage createGrayImageFromByteArray(final byte[] pixels, final int width, final int height) {
      assert pixels.length == width * height;

      final DataBuffer data = new DataBufferByte(pixels, 1);
      final WritableRaster raster = Raster.createInterleavedRaster(data,
            width,
            height,
            width,
            1,
            new int[] { 0 },
            new Point(0, 0));

      final ColorSpace cs = new ICC_ColorSpace(ICC_Profile.getInstance(ColorSpace.CS_GRAY));
      final ColorModel cm = new ComponentColorModel(cs,
            false,
            false,
            Transparency.OPAQUE,
            DataBuffer.TYPE_BYTE);

      return new BufferedImage(cm, raster, false, null);
   }

   /**
    * Creates a new {@link BufferedImage} with the same
    * {@link ColorModel} as the given source image.
    */
   public static BufferedImage createImageWithSameCM(final BufferedImage src) {
      final ColorModel cm = src.getColorModel();
      return new BufferedImage(cm,
            cm.createCompatibleWritableRaster(
                  src.getWidth(),
                  src.getHeight()),
            cm.isAlphaPremultiplied(),
            null);
   }

   /**
    * Creates a new {@link BufferedImage} with the same
    * {@link ColorModel} as the given source image and
    * with the given width and height.
    */
   public static BufferedImage createImageWithSameCM(final BufferedImage src,
                                                     final int width,
                                                     final int height) {
      final ColorModel cm = src.getColorModel();
      return new BufferedImage(cm,
            cm.createCompatibleWritableRaster(width, height),
            cm.isAlphaPremultiplied(),
            null);
   }

   public static BufferedImage createRandomPointsTemplateBrush(final int diameter, final float density) {
      if (density < 0.0 && density > 1.0) {
         throw new IllegalArgumentException("density is " + density); //$NON-NLS-1$
      }

      final BufferedImage brushImage = new BufferedImage(diameter, diameter, TYPE_INT_ARGB);

      final int radius = diameter / 2;
      final int radius2 = radius * radius;
      final Random random = new Random();

      final int[] pixels = getPixelArray(brushImage);
      for (int x = 0; x < diameter; x++) {
         for (int y = 0; y < diameter; y++) {
            final int dx = x - radius;
            final int dy = y - radius;
            final int centerDistance2 = dx * dx + dy * dy;
            if (centerDistance2 < radius2) {
               final float rn = random.nextFloat();
               if (density > rn) {
                  pixels[x + y * diameter] = random.nextInt();
               } else {
                  pixels[x + y * diameter] = 0xFF_FF_FF_FF; // white
               }
            } else {
               pixels[x + y * diameter] = 0xFF_FF_FF_FF; // white
            }
         }
      }

      return brushImage;
   }

   /**
    * Prepares a temporary Graphics2D for soft (anti-aliased) selection
    * clipping. It follows ideas from
    * http://web.archive.org/web/20120603053853/http://weblogs.java.net/blog/campbell/archive/2006/07/java_2d_tricker.html
    */
   public static Graphics2D createSoftSelectionMask(final Image image,
                                                    final Shape selShape,
                                                    final int selStartX,
                                                    final int selStartY) {
      final Graphics2D maskG = (Graphics2D) image.getGraphics();

      // fill the entire image with transparent pixels
      maskG.setComposite(AlphaComposite.Clear);
      maskG.fillRect(0, 0, image.getWidth(null), image.getHeight(null));

      // fill the transparent image with anti-aliased
      // selection-shaped white mask
      maskG.setComposite(AlphaComposite.Src);
      maskG.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
      maskG.setColor(WHITE);
      maskG.translate(-selStartX, -selStartY); // because the selection shape is relative to the canvas
      maskG.fill(selShape);

      // Prepare the Graphics2D for subsequent rendering.
      // It is important to use SrcIn, and not SrcAtop like in the
      // blog mentioned above, because the new content might also
      // contain transparent pixels, and we don't want to lose that information.
      maskG.setComposite(AlphaComposite.SrcIn);
      maskG.translate(selStartX, selStartY); // undo the previous translation

      return maskG;
   }

   public static BufferedImage createSysCompatibleImage(final Canvas canvas) {
      return createSysCompatibleImage(canvas.getWidth(), canvas.getHeight());
   }

   public static BufferedImage createSysCompatibleImage(final int width, final int height) {
      assert width > 0 && height > 0;

      return graphicsConfig.createCompatibleImage(width, height, TRANSLUCENT);
   }

   public static VolatileImage createSysCompatibleVolatileImage(final Canvas canvas) {
      return createSysCompatibleVolatileImage(canvas.getWidth(), canvas.getHeight());
   }

   public static VolatileImage createSysCompatibleVolatileImage(final int width, final int height) {
      assert width > 0 && height > 0;

      return graphicsConfig.createCompatibleVolatileImage(width, height, TRANSLUCENT);
   }

   public static BufferedImage crop(final BufferedImage input, final int x, final int y, final int width, final int height) {
      assert input != null;

      if (width <= 0) {
         throw new IllegalArgumentException("width = " + width); //$NON-NLS-1$
      }
      if (height <= 0) {
         throw new IllegalArgumentException("height = " + height); //$NON-NLS-1$
      }

      final BufferedImage output = createImageWithSameCM(input, width, height);
      final Graphics2D g = output.createGraphics();
      g.transform(AffineTransform.getTranslateInstance(-x, -y));
      g.drawImage(input, null, 0, 0);
      g.dispose();

      return output;
   }

   public static BufferedImage crop(final BufferedImage input, final Rectangle bounds) {
      return crop(input, bounds.x, bounds.y, bounds.width, bounds.height);
   }

   /**
    * Also an iterative approach, but using even smaller steps
    */
   public static BufferedImage enlargeSmooth(final BufferedImage src,
                                             final int targetWidth,
                                             final int targetHeight,
                                             final Object hint,
                                             final double step,
                                             final ProgressTracker pt) {
      final int srcWidth = src.getWidth();
      final int srcHeight = src.getHeight();
      final double factorX = targetWidth / (double) srcWidth;
      final double factorY = targetHeight / (double) srcHeight;

      // they should be the same, but rounding errors can cause small problems
      assert Math.abs(factorX - factorY) < 0.05;

      final double factor = (factorX + factorY) / 2.0;
      assert factor > 1.0; // this only makes sense for enlarging
      double progress = 1.0;
      final double lastStep = factor / step;
      BufferedImage last = src;
      final AffineTransform stepScale = AffineTransform.getScaleInstance(step, step);
      while (progress < lastStep) {
         progress = progress * step;
         final int newSrcWidth = (int) (srcWidth * progress);
         final int newSrcHeight = (int) (srcHeight * progress);
         final BufferedImage tmp = new BufferedImage(newSrcWidth, newSrcHeight, src.getType());
         final Graphics2D g = tmp.createGraphics();
         if (hint != null) {
            g.setRenderingHint(KEY_INTERPOLATION, hint);
         }

         g.drawImage(last, stepScale, null);
         g.dispose();

         final BufferedImage willBeForgotten = last;
         last = tmp;
         willBeForgotten.flush();
         pt.unitDone();
      }

      // do the last step: resize exactly to the target values
      final BufferedImage retVal = new BufferedImage(targetWidth, targetHeight, src.getType());
      final Graphics2D g = retVal.createGraphics();
      if (hint != null) {
         g.setRenderingHint(KEY_INTERPOLATION, hint);
      }

      g.drawImage(last, 0, 0, targetWidth, targetHeight, null);
      g.dispose();
      pt.unitDone();

      return retVal;
   }

   /**
    * Converts an image filename to a resource URL within the images directory.
    */
   public static URL findImageURL(final String fileName) {
      assert fileName != null;

      final String path = "/images/" + fileName; //$NON-NLS-1$
      final URL imgURL = ImageUtils.class.getResource(path);
      if (imgURL == null) {
         Messages.showError("Error", path + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
      }

      return imgURL;
   }

   /**
    * Convenience method that returns a scaled instance of the
    * provided BufferedImage.
    *
    * @param img
    *           the original image to be scaled
    * @param targetWidth
    *           the desired width of the scaled instance,
    *           in pixels
    * @param targetHeight
    *           the desired height of the scaled instance,
    *           in pixels
    * @param hint
    *           one of the rendering hints that corresponds to
    *           RenderingHints.KEY_INTERPOLATION (e.g.
    *           RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
    *           RenderingHints.VALUE_INTERPOLATION_BILINEAR,
    *           RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    * @param progressiveBilinear
    *           if true, this method will use a multi-step
    *           scaling technique that provides higher quality than the usual
    *           one-step technique (only useful in down-scaling cases, where
    *           targetWidth or targetHeight is
    *           smaller than the original dimensions)
    *
    * @return a scaled version of the original BufferedImage
    */
   public static BufferedImage getFasterScaledInstance(final BufferedImage img,
                                                       final int targetWidth,
                                                       final int targetHeight,
                                                       final Object hint,
                                                       boolean progressiveBilinear) {
      assert img != null;

      int prevW = img.getWidth();
      int prevH = img.getHeight();

      if (targetWidth >= prevW || targetHeight >= prevH) {
         progressiveBilinear = false;
      }

      final int type = img.getType();

      BufferedImage ret = img;
      int w, h;
      final boolean isTranslucent = img.getTransparency() != Transparency.OPAQUE;

      if (progressiveBilinear) {
         // Use multi-step technique: start with original size, then
         // scale down in multiple passes with drawImage()
         // until the target size is reached
         w = img.getWidth();
         h = img.getHeight();
      } else {
         // Use one-step technique: scale directly from original
         // size to target size with a single drawImage() call
         w = targetWidth;
         h = targetHeight;
      }

      BufferedImage scratchImage = null;
      Graphics2D g2 = null;
      do {
         if (progressiveBilinear && w > targetWidth) {
            w /= 2;
            if (w < targetWidth) {
               w = targetWidth;
            }
         }

         if (progressiveBilinear && h > targetHeight) {
            h /= 2;
            if (h < targetHeight) {
               h = targetHeight;
            }
         }

         if (scratchImage == null || isTranslucent) {
            // Use a single scratch buffer for all iterations
            // and then copy to the final, correctly-sized image
            // before returning
            scratchImage = new BufferedImage(w, h, type);
            g2 = scratchImage.createGraphics();
         }
         g2.setRenderingHint(KEY_INTERPOLATION, hint);
         g2.drawImage(ret, 0, 0, w, h, 0, 0, prevW, prevH, null);
         prevW = w;
         prevH = h;

         ret = scratchImage;
      }
      while (w != targetWidth || h != targetHeight);

      if (g2 != null) {
         g2.dispose();
      }

      // If we used a scratch buffer that is larger than our target size,
      // create an image of the right size and copy the results into it
      if (targetWidth != ret.getWidth() || targetHeight != ret.getHeight()) {
         scratchImage = new BufferedImage(targetWidth, targetHeight, type);
         g2 = scratchImage.createGraphics();
         g2.drawImage(ret, 0, 0, null);
         g2.dispose();
         ret = scratchImage;
      }

      return ret;
   }

   public static byte[] getGrayPixelByteArray(final BufferedImage img) {
      assert isGrayscale(img);

      final WritableRaster raster = img.getRaster();
      final DataBufferByte db = (DataBufferByte) raster.getDataBuffer();

      return db.getData();
   }

   // See https://graphicdesign.stackexchange.com/questions/89969/what-does-photoshops-high-pass-filter-actually-do-under-the-hood

   /**
    * Returns the minimum enclosing rectangle around the non-transparent region in the given image.
    */
   public static Rectangle getNonTransparentBounds(final BufferedImage image) {
      final WritableRaster alphaRaster = image.getAlphaRaster();
      final int width = alphaRaster.getWidth();
      final int height = alphaRaster.getHeight();

      // initial bounds
      int left = 0;
      int top = 0;
      int right = width - 1;
      int bottom = height - 1;

      // optimization helper variables
      int minRight = width - 1;
      int minBottom = height - 1;

      // iterates through the rows from the top and stops
      // when it finds the first non-transparent pixel
      topLabel: for (; top < bottom; top++) {
         for (int x = 0; x < width; x++) {
            if (alphaRaster.getSample(x, top, 0) != 0) {
               minRight = x;
               minBottom = top;
               break topLabel;
            }
         }
      }

      // iterates through the columns from the left
      leftLabel: for (; left < minRight; left++) {
         for (int y = height - 1; y > top; y--) {
            if (alphaRaster.getSample(left, y, 0) != 0) {
               minBottom = y;
               break leftLabel;
            }
         }
      }

      // iterates through the rows from the bottom
      bottomLabel: for (; bottom > minBottom; bottom--) {
         for (int x = width - 1; x >= left; x--) {
            if (alphaRaster.getSample(x, bottom, 0) != 0) {
               minRight = x;
               break bottomLabel;
            }
         }
      }

      // iterates through the columns from the right
      rightLabel: for (; right > minRight; right--) {
         for (int y = bottom; y >= top; y--) {
            if (alphaRaster.getSample(right, y, 0) != 0) {
               break rightLabel;
            }
         }
      }

      return new Rectangle(left, top, right - left + 1, bottom - top + 1);
   }

   /**
    * Returns the pixel array behind the given BufferedImage.
    * If the array data is modified, the image itself is modified.
    */
   public static int[] getPixelArray(final BufferedImage srcFinal) {

      final BufferedImage src = srcFinal;

      assert src != null;

//      final int type = src.getType();
//      if (type == BufferedImage.TYPE_3BYTE_BGR || type == BufferedImage.TYPE_CUSTOM) {
//
//         // create new image with right format
//         final int imageType = src.getTransparency() == Transparency.OPAQUE
//               ? BufferedImage.TYPE_INT_RGB
//               : BufferedImage.TYPE_INT_ARGB;
//
//         final BufferedImage tempImage = new BufferedImage(src.getWidth(), src.getHeight(), imageType);
//         final Graphics2D g = tempImage.createGraphics();
//         {
//            g.drawImage(src, 0, 0, null);
//            src = tempImage;
//         }
//         g.dispose();
//      }

      int[] pixels;

      if (hasPackedIntArray(src)) {

         assert src.getRaster().getTransferType() == TYPE_INT;
         assert src.getRaster().getNumDataElements() == 1;

         final DataBufferInt srcDataBuffer = (DataBufferInt) src.getRaster().getDataBuffer();
         pixels = srcDataBuffer.getData();

      } else {

         // If the image's pixels are not stored in an int array,
         // a correct int array could still be retrieved with
         // src.getRGB(0, 0, width, height, null, 0, width);
         // but modifying that array wouldn't have any effect on the image.
         throw new UnsupportedOperationException("type is " + Debug.bufferedImageTypeToString(src.getType())); //$NON-NLS-1$
      }

      return pixels;
   }

   public static boolean hasPackedIntArray(final BufferedImage image) {
      assert image != null;

      final int type = image.getType();
      return (type == TYPE_INT_ARGB_PRE || type == TYPE_INT_ARGB || type == TYPE_INT_RGB);
   }

   public static boolean isGrayscale(final BufferedImage img) {
      return img.getType() == TYPE_BYTE_GRAY;
   }

   public static Boolean isSubImage(final BufferedImage src) {
      final WritableRaster raster = src.getRaster();
      return raster.getSampleModelTranslateX() != 0
            || raster.getSampleModelTranslateY() != 0;
   }

   /**
    * Returns true if the coordinates (x, y) are within the image.
    */
   public static boolean isWithinBounds(final int x, final int y, final BufferedImage img) {
      return x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight();
   }

   public static int lerpAndPremultiply(final float t, final int[] color1, final int[] color2) {
      final int alpha = color1[0] + (int) (t * (color2[0] - color1[0]));
      int red;
      int green;
      int blue;
      if (alpha == 0) {
         red = 0;
         green = 0;
         blue = 0;
      } else {
         red = color1[1] + (int) (t * (color2[1] - color1[1]));
         green = color1[2] + (int) (t * (color2[2] - color1[2]));
         blue = color1[3] + (int) (t * (color2[3] - color1[3]));

         if (alpha != 255) { // premultiply
            final float f = alpha / 255.0f;
            red = (int) (red * f);
            green = (int) (green * f);
            blue = (int) (blue * f);
         }
      }

      return alpha << 24 | red << 16 | green << 8 | blue;
   }

   public static BufferedImage loadJarImageFromImagesFolder(final String fileName) {
      assert fileName != null;

      final URL imgURL = findImageURL(fileName);
      BufferedImage image = null;
      try {
         image = ImageIO.read(imgURL);
      } catch (final IOException e) {
         Messages.showException(e);
      }
      return image;
   }

   public static BufferedImage mask(final BufferedImage srcA, final BufferedImage srcB, final BufferedImage mask) {
      final BufferedImage dest = createImageWithSameCM(srcA);
      final int[] srcAPixels = getPixelArray(srcA);
      final int[] srcBPixels = getPixelArray(srcB);
      final int[] maskPixels = getPixelArray(mask);
      final int[] destPixels = getPixelArray(dest);

      for (int i = 0, numPixels = destPixels.length; i < numPixels; i++) {
         // take the blue channel, assuming that all channels are the same
         final float transparency = (maskPixels[i] & 0xFF) / 255.0f;
         destPixels[i] = ImageMath.mixColors(transparency, srcAPixels[i], srcBPixels[i]);
      }

      return dest;
   }

   public static void paintRedXOn(final BufferedImage thumb) {
      final int thumbWidth = thumb.getWidth();
      final int thumbHeight = thumb.getHeight();

      final Graphics2D g = thumb.createGraphics();

      g.setColor(new Color(200, 0, 0));
      g.setStroke(new BasicStroke(2.5f));
      g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
      g.drawLine(0, 0, thumbWidth, thumbHeight);
      g.drawLine(thumbWidth - 1, 0, 0, thumbHeight - 1);
      g.dispose();
   }

   public static void premultiply(final BufferedImage src) {
      final int[] pixels = getPixelArray(src);
      ImageMath.premultiply(pixels);
   }

   public static int premultiply(final int rgb) {
      final int a = (rgb >>> 24) & 0xFF;
      int r = (rgb >>> 16) & 0xFF;
      int g = (rgb >>> 8) & 0xFF;
      int b = rgb & 0xFF;

      final float f = a * (1.0f / 255.0f);
      r = (int) (r * f);
      g = (int) (g * f);
      b = (int) (b * f);

      return (a << 24) | (r << 16) | (g << 8) | b;
   }

   /**
    * A hack so that Fade can work with PartialImageEdit rasters.
    * It would be better if Fade could work with rasters directly.
    */
   public static BufferedImage rasterToImage(final Raster raster) {
      assert raster != null;

      final int minX = raster.getMinX();
      final int minY = raster.getMinY();
      final int width = raster.getWidth();
      final int height = raster.getHeight();
      final Raster startingFrom00 = raster.createChild(minX, minY, width, height, 0, 0, null);
      final BufferedImage image = new BufferedImage(width, height, TYPE_INT_ARGB_PRE);
      image.setData(startingFrom00);

      return image;
   }

   public static void renderBrickGrid(final Graphics2D g,
                                      final Color color,
                                      final int brickHeight,
                                      final int maxX,
                                      final int maxY) {
      if (brickHeight < 1) {
         throw new IllegalArgumentException("brickHeight = " + brickHeight); //$NON-NLS-1$
      }

      g.setColor(color);

      final int brickWidth = brickHeight * 2;
      int currentY = brickHeight;
      int rowCount = 0;

      while (currentY < maxY) {
         // vertical lines
         final int horOffset = ((rowCount % 2) == 1) ? brickHeight : 0;
         for (int x = horOffset; x < maxX; x += brickWidth) {
            g.drawLine(x, currentY, x, currentY - brickHeight);
         }

         // horizontal lines
         g.drawLine(0, currentY, maxX, currentY);
         currentY += brickHeight;
         rowCount++;
      }
   }

   public static void renderGrid(final Graphics2D g,
                                 final Color color,
                                 final int maxX,
                                 final int maxY,
                                 final int horLineThickness,
                                 final int horSpacing,
                                 final int verLineThickness,
                                 final int verSpacing) {
      if (horLineThickness < 0) {
         throw new IllegalArgumentException("horLineThickness = " + horLineThickness); //$NON-NLS-1$
      }
      if (verLineThickness < 0) {
         throw new IllegalArgumentException("verLineThickness = " + verLineThickness); //$NON-NLS-1$
      }
      if (horSpacing <= 0) {
         throw new IllegalArgumentException("horSpacing = " + horSpacing); //$NON-NLS-1$
      }
      if (verSpacing <= 0) {
         throw new IllegalArgumentException("verSpacing = " + verSpacing); //$NON-NLS-1$
      }

      g.setColor(color);

      // horizontal lines
      if (horLineThickness > 0) {
         final int halfLineThickness = verLineThickness / 2;
         for (int y = 0; y < maxY; y += verSpacing) {
            final int startY = y - halfLineThickness;
            //noinspection SuspiciousNameCombination
            g.fillRect(0, startY, maxX, verLineThickness);
         }
      }

      // vertical lines
      if (verLineThickness > 0) {
         final int halfLineThickness = horLineThickness / 2;
         for (int x = 0; x < maxX; x += horSpacing) {
            g.fillRect(x - halfLineThickness, 0, horLineThickness, maxY);
         }
      }
   }

   public static BufferedImage resize(final BufferedImage img, final int targetWidth, final int targetHeight) {
      final boolean progressiveBilinear = targetWidth < img.getWidth() / 2
            || targetHeight < img.getHeight() / 2;
      return getFasterScaledInstance(img, targetWidth, targetHeight, VALUE_INTERPOLATION_BICUBIC, progressiveBilinear);
   }

   public static CompletableFuture<BufferedImage> resizeAsync(final BufferedImage img,
                                                              final int targetWidth,
                                                              final int targetHeight) {
      return CompletableFuture.supplyAsync(() -> resize(img, targetWidth, targetHeight), onPool);
   }

   public static BufferedImage toSysCompatibleImage(final BufferedImage input) {
      assert input != null;

      if (input.getColorModel().equals(defaultColorModel)) {
         // RGB images have the right direct color model, but we need transparency
         if (input.getType() != TYPE_INT_RGB) {
            return input;
         }
      }

      final BufferedImage output = graphicsConfig.createCompatibleImage(
            input.getWidth(),
            input.getHeight(),
            TRANSLUCENT);
      final Graphics2D g = output.createGraphics();
      g.drawImage(input, 0, 0, null);
      g.dispose();

      return output;
   }

   public static void unpremultiply(final BufferedImage dest) {
      final int[] pixels = getPixelArray(dest);
      ImageMath.unpremultiply(pixels);
   }

   public static int unPremultiply(final int rgb) {
      final int a = (rgb >>> 24) & 0xFF;
      int r = (rgb >>> 16) & 0xFF;
      int g = (rgb >>> 8) & 0xFF;
      int b = rgb & 0xFF;

      if (a == 0 || a == 255) {
         return rgb;
      }

      final float f = 255.0f / a;
      r = (int) (r * f);
      g = (int) (g * f);
      b = (int) (b * f);
      if (r > 255) {
         r = 255;
      }
      if (g > 255) {
         g = 255;
      }
      if (b > 255) {
         b = 255;
      }

      return (a << 24) | (r << 16) | (g << 8) | b;
   }
}
