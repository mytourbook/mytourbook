/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.photo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import net.tourbook.common.util.StatusUtil;

public class Map25PhotoImageManager {

   private static final AtomicLong                          _executerId   = new AtomicLong();
   private static final LinkedBlockingDeque<PhotoImageData> _waitingQueue = new LinkedBlockingDeque<>();
   private static ExecutorService                           _executor;

   static {

      final ThreadFactory threadFactory = runnable -> {

         final Thread thread = new Thread(runnable, "Map25 : Creating map photo images");//$NON-NLS-1$

         thread.setPriority(Thread.MIN_PRIORITY);
         thread.setDaemon(true);

         return thread;
      };

      _executor = Executors.newSingleThreadExecutor(threadFactory);
   }

   public static PhotoImageData createPhotoImages(final PhotoImageData previousImageData) {

      stopPreviousTask(previousImageData);

      // invalidate old requests
      final long executerId = _executerId.incrementAndGet();

      final PhotoImageData photoImageData = new PhotoImageData(executerId);

      _waitingQueue.add(photoImageData);

      final Runnable executorTask = () -> {

         try {

            // get last added loader item
            final PhotoImageData geoLoaderData = _waitingQueue.pollFirst();

            if (geoLoaderData == null) {
               return;
            }

         } catch (final Exception e) {
            StatusUtil.log(e);
         }
      };

      _executor.submit(executorTask);

      return photoImageData;
   }

   /**
    * Stop creating images in the waiting queue.
    *
    * @param photoImageData
    */
   public static void stopPreviousTask(final PhotoImageData photoImageData) {

      if (photoImageData != null) {
         photoImageData.isCanceled = true;
      }
   }

}
