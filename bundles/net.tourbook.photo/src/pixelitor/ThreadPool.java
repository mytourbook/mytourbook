/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor;

import com.jhlabs.image.AbstractBufferedImageOp;

import java.awt.image.BufferedImage;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import net.tourbook.common.util.StatusUtil;

import pixelitor.utils.ProgressTracker;

/**
 * A thread pool for parallel execution on multiple CPU cores
 */
public class ThreadPool {

   private static final int             NUM_CORES = Runtime.getRuntime().availableProcessors();

   private static final ExecutorService pool      =

         Executors.newFixedThreadPool(NUM_CORES, new ThreadFactory() {

                                                           private final AtomicInteger threadCount = new AtomicInteger(1);

                                                           @Override
                                                           public Thread newThread(final Runnable r) {

                                                              final Thread thread = new Thread(r, "ImageProcessor-" + threadCount.getAndIncrement()); //$NON-NLS-1$
                                                              thread.setDaemon(true);

                                                              return thread;
                                                           }
                                                        });

   private ThreadPool() {
      throw new AssertionError("utility class"); //$NON-NLS-1$
   }

   public static Executor getExecutor() {
      return pool;
   }

   /**
    * Submits a task that doesn't return anything.
    */
   public static Future<?> submit(final Runnable task) {
      return pool.submit(task);
   }

   /**
    * Submits a task that returns a value, such as
    * the calculated pixels in a line.
    */
   public static <T> Future<T> submit2(final Callable<T> task) {
      return pool.submit(task);
   }

   // same as the method above, but with an array argument
   public static void waitFor(final Future<?>[] futures, final ProgressTracker pt) {

      assert pt != null;

      for (final var future : futures) {
         try {

            future.get();
            pt.unitDone();

         } catch (InterruptedException | ExecutionException e) {
            StatusUtil.showStatus(e);
         }
      }
   }

   /**
    * Waits for all futures to complete while tracking progress.
    */
   public static void waitFor(final Iterable<Future<?>> futures, final ProgressTracker pt) {

      assert pt != null;

      for (final var future : futures) {
         try {

            future.get();

            // not completely accurate to count here, but good enough in practice
            pt.unitDone();

         } catch (InterruptedException | ExecutionException e) {
            StatusUtil.showStatus(e);
         }
      }
   }

   /**
    * Similar to waitFor, but also updates given the destination image.
    * Each future represents a processed line of pixels in the image.
    */
   public static void waitFor2(final Future<int[]>[] lineFutures,
                               final BufferedImage dst,
                               final int lineWidth,
                               final ProgressTracker pt) {
      assert pt != null;

      try {
         for (int y = 0; y < lineFutures.length; y++) {
            final int[] linePixels = lineFutures[y].get();
            AbstractBufferedImageOp.setRGB(dst, 0, y, lineWidth, 1, linePixels);
            pt.unitDone();
         }
      } catch (InterruptedException | ExecutionException e) {
         StatusUtil.showStatus(e);
      }
   }
}
