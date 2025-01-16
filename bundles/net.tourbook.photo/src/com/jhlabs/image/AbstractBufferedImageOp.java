/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.jhlabs.image;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;

import pixelitor.utils.ProgressTracker;
import pixelitor.utils.StatusBarProgressTracker;

/**
 * A convenience class which implements those methods of BufferedImageOp which are rarely changed.
 */
public abstract class AbstractBufferedImageOp implements BufferedImageOp, Cloneable {

   //  ******* Start of Pixelitor-specific stuff *******

   /**
    * The filter name in Pixelitor.
    */
   protected final String    filterName;

   protected ProgressTracker pt;

   /**
    * Whether this filter is used as a helper filter for another
    * filter. Important for progress tracking.
    */
   private boolean           usedAsHelper = false;

   protected AbstractBufferedImageOp(final String filterName) {

      this.filterName = filterName;
      assert filterName != null;
   }

   /**
    * A convenience method for getting ARGB pixels from an image. This tries to avoid the
    * performance
    * penalty of BufferedImage.getRGB unmanaging the image.
    *
    * @param image
    *           a BufferedImage object
    * @param x
    *           the starting X coordinate
    * @param y
    *           the starting Y coordinate
    * @param width
    *           width of region
    * @param height
    *           height of region
    * @param pixels
    *           the array to hold the returned pixels. May be null.
    *
    * @return the pixels
    *
    * @see #setRGB
    */
   public static int[] getRGB(final BufferedImage image, final int x, final int y, final int width, final int height, final int[] pixels) {

      final int type = image.getType();
//		if ( type == TYPE_INT_ARGB || type == TYPE_INT_RGB )

      if ((type == TYPE_INT_ARGB) || (type == TYPE_INT_RGB) || (type == TYPE_INT_ARGB_PRE)) {
         return (int[]) image.getRaster().getDataElements(x, y, width, height, pixels);
      }

      return image.getRGB(x, y, width, height, pixels, 0, width);
   }

   //  ******* End of Pixelitor-specific stuff *******

   /**
    * A convenience method for setting ARGB pixels in an image. This tries to avoid the performance
    * penalty of BufferedImage.setRGB unmanaging the image.
    *
    * @param image
    *           a BufferedImage object
    * @param x
    *           the left edge of the pixel block
    * @param y
    *           the right edge of the pixel block
    * @param width
    *           the width of the pixel arry
    * @param height
    *           the height of the pixel arry
    * @param pixels
    *           the array of pixels to set
    *
    * @see #getRGB
    */
   public static void setRGB(final BufferedImage image, final int x, final int y, final int width, final int height, final int[] pixels) {
      final int type = image.getType();
//		if ( type == TYPE_INT_ARGB || type == TYPE_INT_RGB  )
      if ((type == TYPE_INT_ARGB) || (type == TYPE_INT_RGB) || (type == TYPE_INT_ARGB_PRE)) {
         image.getRaster().setDataElements(x, y, width, height, pixels);
      } else {
         image.setRGB(x, y, width, height, pixels, 0, width);
      }
   }

   @Override
   public Object clone() {
      try {
         return super.clone();
      } catch (final CloneNotSupportedException e) {
         return null;
      }
   }

   @Override
   public BufferedImage createCompatibleDestImage(final BufferedImage src, ColorModel dstCM) {
      if (dstCM == null) {
         dstCM = src.getColorModel();
      }
      return new BufferedImage(dstCM,
            dstCM.createCompatibleWritableRaster(src.getWidth(), src.getHeight()),
            dstCM.isAlphaPremultiplied(),
            null);
   }

   protected ProgressTracker createProgressTracker(final int workUnits) {

      if (!usedAsHelper) {
         pt = new StatusBarProgressTracker(filterName, workUnits);
      }

      return pt;
   }

   protected void finishProgressTracker() {
      // if it is used as a helper filter, then the
      // calling filter will finish it
      if (!usedAsHelper) {
         pt.finished();
      }
   }

   @Override
   public Rectangle2D getBounds2D(final BufferedImage src) {
      return new Rectangle(0, 0, src.getWidth(), src.getHeight());
   }

   @Override
   public Point2D getPoint2D(final Point2D srcPt, Point2D dstPt) {
      if (dstPt == null) {
         dstPt = new Point2D.Double();
      }
      dstPt.setLocation(srcPt.getX(), srcPt.getY());
      return dstPt;
   }

   public ProgressTracker getProgressTracker() {
      return pt;
   }

   @Override
   public RenderingHints getRenderingHints() {
      return null;
   }

   public void setProgressTracker(final ProgressTracker pt) {
      this.pt = pt;
      usedAsHelper = true;
   }
}
