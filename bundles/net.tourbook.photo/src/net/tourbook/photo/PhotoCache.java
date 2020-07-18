/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.tourbook.common.UI;

import org.eclipse.core.runtime.ListenerList;

/**
 * This cache is caching ALL photos.
 */
public class PhotoCache {

   private static final int                                  MAX_CACHE_SIZE     = 50000;

   private static Cache<String, Photo>                       _cache;

   private static final ListenerList<IPhotoEvictionListener> _evictionListeners = new ListenerList<>(ListenerList.IDENTITY);

   static {

      final RemovalListener<String, Photo> removalListener = new RemovalListener<String, Photo>() {

         final ExecutorService executor = Executors.newSingleThreadExecutor();

         @Override
         public void onRemoval(final String fileName, final Photo photo, final RemovalCause removalCause) {

            executor.submit(new Callable<Void>() {
               @Override
               public Void call() throws IOException {

                  final Object[] allListeners = _evictionListeners.getListeners();
                  for (final Object listener : allListeners) {
                     ((IPhotoEvictionListener) listener).evictedPhoto(photo);
                  }

                  return null;
               }
            });
         }
      };

      _cache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .removalListener(removalListener)
            .build();
   }

   public static void addEvictionListener(final IPhotoEvictionListener listener) {
      _evictionListeners.add(listener);
   }

   public static void dumpAllPhotos() {

      System.out.println(UI.timeStampNano() + " PhotoCache\t"); //$NON-NLS-1$

      for (final Photo photo : _cache.asMap().values()) {

         System.out.println(UI.timeStampNano() + " \t" + photo.imageFilePathName + ("\t")); //$NON-NLS-1$ //$NON-NLS-2$

         photo.dumpTourReferences();
      }
   }

   public static Photo getPhoto(final String imageFilePathName) {
      return _cache.getIfPresent(imageFilePathName);
   }

   public static void removeEvictionListener(final IPhotoEvictionListener listener) {
      if (listener != null) {
         _evictionListeners.remove(listener);
      }
   }

   public static synchronized void removePhotosFromFolder(final String folderName) {

      for (final Photo photo : _cache.asMap().values()) {

         if (photo.imagePathName.equals(folderName)) {
            _cache.invalidate(photo.imageFilePathName);
         }
      }
   }

   public static synchronized void replaceImageFile(final ArrayList<ImagePathReplacement> replacedImages) {

      for (final ImagePathReplacement replacedImage : replacedImages) {

         final Photo cachedPhoto = _cache.asMap().remove(replacedImage.oldImageFilePathName);

         if (cachedPhoto != null) {

            // update image file path

            // reset loading state to force a reload of the image in the gallery
            cachedPhoto.replaceImageFile(replacedImage.newImageFilePathName);

            // set photo with new file path name
            setPhoto(cachedPhoto);
         }
      }
   }

   /**
    * Keep photo in the photo cache.
    *
    * @param photo
    */
   public static void setPhoto(final Photo photo) {
      _cache.put(photo.imageFilePathName, photo);
   }

}
