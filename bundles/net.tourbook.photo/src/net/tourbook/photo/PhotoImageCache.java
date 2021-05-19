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
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.tourbook.photo.internal.manager.ImageCacheWrapper;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

/**
 * This cache is caching photo images and its metadata.
 */
public class PhotoImageCache {

   private static IPreferenceStore _prefStore                 = PhotoActivator.getPrefStore();

   private static int              _maxThumbImageCacheSize    = _prefStore.getInt(IPhotoPreferences.PHOTO_THUMBNAIL_IMAGE_CACHE_SIZE);

   /**
    * This cache size should not be too large otherwise OS has no resources, loading images is
    * slowing down until all image caches must be disposed. This size is optimal for image size
    * 5184x3456 on win7 for smaller image it could be larger for bigger images it should be
    * smaller.
    */
   private static int              _maxOriginalImageCacheSize = _prefStore.getInt(IPhotoPreferences.PHOTO_ORIGINAL_IMAGE_CACHE_SIZE);

// SET_FORMATTING_OFF

   private static final Cache<String, ImageCacheWrapper> _imageCacheThumb;
   private static final Cache<String, ImageCacheWrapper> _imageCacheOriginal;

// SET_FORMATTING_ON

   static {

      final RemovalListener<String, ImageCacheWrapper> removalListener = new RemovalListener<String, ImageCacheWrapper>() {

         final ExecutorService executor = Executors.newSingleThreadExecutor();

         @Override
         public void onRemoval(final String fileName, final ImageCacheWrapper cacheWrapper, final RemovalCause removalCause) {

            executor.submit(new Callable<Void>() {
               @Override
               public Void call() throws IOException {

                  // dispose cached image
                  final Image image = cacheWrapper.image;
                  if (image != null) {
                     image.dispose();
                  }

                  return null;
               }
            });
         }
      };

      _imageCacheThumb = Caffeine.newBuilder()
            .maximumSize(_maxThumbImageCacheSize)
            .removalListener(removalListener)
            .build();

      _imageCacheOriginal = Caffeine.newBuilder()
            .maximumSize(_maxOriginalImageCacheSize)
            .removalListener(removalListener)
            .build();
   }

   /**
    * @param imageCache
    * @param folderPath
    *           When not <code>null</code> only the images in the folder path are disposed,
    *           otherwise all images are disposed.
    */
   private static synchronized void dispose(final Cache<String, ImageCacheWrapper> imageCache,
                                            final String folderPath) {

      if (imageCache == null) {
         return;
      }

      final boolean isDisposeAll = folderPath == null;

      // dispose cached images
      final Collection<ImageCacheWrapper> allWrappers = imageCache.asMap().values();
      for (final ImageCacheWrapper cacheWrapper : allWrappers) {

         if (cacheWrapper != null) {

            if (isDisposeAll == false) {

               // dispose images from a specific folder

               if (cacheWrapper.originalImagePathName.startsWith(folderPath) == false) {

                  // image is in another folder

                  continue;
               }

               imageCache.invalidate(cacheWrapper.imageKey);
            }

            final Image image = cacheWrapper.image;

            if (image != null) {

               // sometimes the device of the image is null which causes an exception

               try {
                  image.dispose();
               } catch (final Exception e) {
                  // ignore
               }
            }
         }
      }

      if (isDisposeAll) {
         imageCache.cleanUp();
      }
   }

   public static void disposeAll() {
      disposeThumbs(null);
      disposeOriginal(null);
   }

   /**
    * Dispose all original images in the cache
    *
    * @param folderPath
    */
   public static void disposeOriginal(final String folderPath) {
      dispose(_imageCacheOriginal, folderPath);
   }

   /**
    * Dispose all images in the folder path.
    *
    * @param folderPath
    */
   public static void disposePath(final String folderPath) {

      disposeThumbs(folderPath);
      disposeOriginal(folderPath);
   }

   /**
    * Dispose all images in the cache
    *
    * @param folderPath
    */
   public static void disposeThumbs(final String folderPath) {
      dispose(_imageCacheThumb, folderPath);
   }

   public static Image getImage(final Photo photo, final ImageQuality imageQuality) {

      final String imageKey = photo.getImageKey(imageQuality);

      return getImageFromCache(_imageCacheThumb, photo, imageKey);
   }

   private static Image getImageFromCache(final Cache<String, ImageCacheWrapper> imageCache,
                                          final Photo photo,
                                          final String imageKey) {

      final ImageCacheWrapper cacheWrapper = imageCache.getIfPresent(imageKey);

      Image photoImage = null;

      if (cacheWrapper != null) {

         photoImage = cacheWrapper.image;

         /*
          * ensure image and metadata are set in the photo
          */
         photo.getImageMetaData();

         // check if height is set
         if (photo.getPhotoImageWidth() == Integer.MIN_VALUE) {

            // image dimension is not yet set

            final Rectangle imageSize = photoImage.getBounds();

            photo.setPhotoDimension(imageSize.width, imageSize.height);
         }
      }
      return photoImage;
   }

   public static Image getImageOriginal(final Photo photo) {

      final String imageKey = photo.getImageKey(ImageQuality.ORIGINAL);

      return getImageFromCache(_imageCacheOriginal, photo, imageKey);

//		if (_imageCacheOriginal.size() > 1) {
//
//		}
//
//		return null;
   }

   /**
    * Put a new image into the image cache. When an old image with the same image key already
    * exists, this image will be disposed.
    *
    * @param imageKey
    * @param image
    * @param imageMetadata
    * @param imageHeight
    * @param imageWidth
    * @param originalImagePathName
    */
   public static void putImage(final String imageKey, final Image image, final String originalImagePathName) {

      putImageInCache(_imageCacheThumb, imageKey, image, originalImagePathName);
   }

   private static void putImageInCache(final Cache<String, ImageCacheWrapper> imageCache,
                                       final String imageKey,
                                       final Image image,
                                       final String originalImagePathName) {

      final ImageCacheWrapper imageCacheWrapper = new ImageCacheWrapper(image, originalImagePathName, imageKey);

      final ImageCacheWrapper oldWrapper = imageCache.asMap().put(imageKey, imageCacheWrapper);

      if (oldWrapper != null) {
         final Image oldImage = oldWrapper.image;
         if (oldImage != null) {
            oldImage.dispose();
         }
      }
   }

   /**
    * Put a new original image into the image cache. When an old image with the same image key
    * already exists, this image will be disposed.
    *
    * @param imageKey
    * @param image
    * @param imageMetadata
    * @param imageHeight
    * @param imageWidth
    * @param originalImagePathName
    */
   public static void putImageOriginal(final String imageKey, final Image image, final String originalImagePathName) {

      if (_imageCacheOriginal.asMap().size() > 1) {}
      putImageInCache(_imageCacheOriginal, imageKey, image, originalImagePathName);
   }

   /**
    * Set image size, when EXIF width/height is not available, the photo size is wrong and is
    * corrected here.
    *
    * @param photo
    * @param imageWidth
    * @param imageHeight
    */
   public static void setImageSize(final Photo photo, final int imageWidth, final int imageHeight) {

      boolean isSet = false;

      if (!isSet) {
         isSet = setImageSize_IntoCacheWrapper(photo,
               imageWidth,
               imageHeight, //
               ImageQuality.THUMB,
               _imageCacheThumb);
      }

      if (!isSet) {
         isSet = setImageSize_IntoCacheWrapper(photo,
               imageWidth,
               imageHeight, //
               ImageQuality.HQ,
               _imageCacheThumb);
      }

      if (!isSet) {
         isSet = setImageSize_IntoCacheWrapper(photo,
               imageWidth,
               imageHeight, //
               ImageQuality.ORIGINAL,
               _imageCacheOriginal);
      }
   }

   private static boolean setImageSize_IntoCacheWrapper(final Photo photo,
                                                        final int imageWidth,
                                                        final int imageHeight,
                                                        final ImageQuality imageQuality,
                                                        final Cache<String, ImageCacheWrapper> imageCache) {

      final String imageKey = photo.getImageKey(imageQuality);

      final ImageCacheWrapper cacheWrapper = imageCache.getIfPresent(imageKey);

      if (cacheWrapper != null) {

         photo.setPhotoDimension(imageWidth, imageHeight);

         return true;
      }

      return false;
   }

   public static void setOriginalImageCacheSize(final int newCacheSize) {
      _imageCacheOriginal.policy().eviction().ifPresent(eviction -> {
         eviction.setMaximum(newCacheSize);
      });
   }

   public static void setThumbCacheSize(final int newCacheSize) {
      _imageCacheThumb.policy().eviction().ifPresent(eviction -> {
         eviction.setMaximum(newCacheSize);
      });
   }
}
