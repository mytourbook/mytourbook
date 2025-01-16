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

import java.awt.image.BufferedImage;
import java.util.concurrent.Future;

import pixelitor.ThreadPool;
import pixelitor.utils.ImageUtils;

/**
 * An abstract superclass for point filters. The interface is the same as the old RGBImageFilter.
 */
public abstract class PointFilter extends AbstractBufferedImageOp {

   protected PointFilter(final String filterName) {
      super(filterName);
   }

   @Override
   public BufferedImage filter(final BufferedImage src, BufferedImage dst) {

      final int width = src.getWidth();
      final int height = src.getHeight();

      setDimensions(width, height);

      if (dst == null) {
         dst = createCompatibleDestImage(src, null);
      }

      final int[] inPixels = ImageUtils.getPixelArray(src);
      final int[] outPixels = ImageUtils.getPixelArray(dst);

      pt = createProgressTracker(height);

      final Future<?>[] rowFutures = new Future[height];

      for (int y = 0; y < height; y++) {

         final int finalY = y;

         final Runnable rowTask = () -> {

            for (int x = 0; x < width; x++) {

               final int index = finalY * width + x;
               outPixels[index] = filterRGB(x, finalY, inPixels[index]);
            }
         };

         rowFutures[y] = ThreadPool.submit(rowTask);
      }

      ThreadPool.waitFor(rowFutures, pt);
      finishProgressTracker();

      return dst;
   }

   public abstract int filterRGB(int x, int y, int rgb);

   public void setDimensions(final int width, final int height) {}
}
