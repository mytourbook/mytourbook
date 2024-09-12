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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.tourbook.common.UI;
import net.tourbook.common.util.NoAutoScalingImageDataProvider;
import net.tourbook.common.util.SWT2Dutil;
import net.tourbook.photo.internal.manager.ImageCacheWrapper;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * This cache is caching photo images and its metadata.
 */
public class PhotoImageCache {

   private static IPreferenceStore _prefStore                  = PhotoActivator.getPrefStore();

   private static int              _maxResizedImage_CacheSize  = _prefStore.getInt(IPhotoPreferences.PHOTO_THUMBNAIL_IMAGE_CACHE_SIZE);

   /**
    * This cache size should not be too large otherwise OS has no resources, loading images is
    * slowing down until all image caches must be disposed. This size is optimal for image size
    * 5184x3456 on win7 for smaller image it could be larger for bigger images it should be
    * smaller.
    */
   private static int              _maxOriginalImage_CacheSize = _prefStore.getInt(IPhotoPreferences.PHOTO_ORIGINAL_IMAGE_CACHE_SIZE);

// SET_FORMATTING_OFF

   private static final Cache<String, ImageCacheWrapper> _imageCache_ResizedImage;
   private static final Cache<String, ImageCacheWrapper> _imageCache_OriginalImage;

// SET_FORMATTING_ON

   static {

      final RemovalListener<String, ImageCacheWrapper> removalListener = new RemovalListener<>() {

         final ExecutorService executor = Executors.newSingleThreadExecutor();

         @Override
         public void onRemoval(final String fileName, final ImageCacheWrapper cacheWrapper, final RemovalCause removalCause) {

            executor.submit(new Callable<Void>() {
               @Override
               public Void call() throws IOException {

                  // dispose cached image
                  disposeImage(cacheWrapper);

                  return null;
               }
            });
         }
      };

      _imageCache_ResizedImage = Caffeine.newBuilder()
            .maximumSize(_maxResizedImage_CacheSize)
            .removalListener(removalListener)
            .build();

      _imageCache_OriginalImage = Caffeine.newBuilder()
            .maximumSize(_maxOriginalImage_CacheSize)
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

            disposeImage(cacheWrapper);
         }
      }

      if (isDisposeAll) {
         imageCache.invalidateAll();
      }
   }

   public static void disposeAll() {

      disposeThumbs(null);
      disposeOriginal(null);
   }

   private static void disposeImage(final ImageCacheWrapper cacheWrapper) {

      UI.disposeResource(cacheWrapper.swtImage);

      final BufferedImage awtImage = cacheWrapper.awtImage;
      if (awtImage != null) {
         awtImage.flush();
      }
   }

   /**
    * Dispose all original images in the cache
    *
    * @param folderPath
    */
   public static void disposeOriginal(final String folderPath) {

      dispose(_imageCache_OriginalImage, folderPath);
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

      dispose(_imageCache_ResizedImage, folderPath);
   }

   public static BufferedImage getImage_AWT(final Photo photo, final ImageQuality imageQuality) {

      final String imageKey = photo.getImageKey(imageQuality);

      return getImageFromCache_AWT(_imageCache_ResizedImage, photo, imageKey);
   }

   public static Image getImage_SWT(final Photo photo, final ImageQuality imageQuality) {

      final String imageKey = photo.getImageKey(imageQuality);

      return getImageFromCache_SWT(_imageCache_ResizedImage, photo, imageKey);
   }

   private static BufferedImage getImageFromCache_AWT(final Cache<String, ImageCacheWrapper> imageCache,
                                                      final Photo photo,
                                                      final String imageKey) {

      final ImageCacheWrapper cacheWrapper = imageCache.getIfPresent(imageKey);

      BufferedImage awtImage = null;

      if (cacheWrapper != null) {

         awtImage = cacheWrapper.awtImage;

         /*
          * ensure image and metadata are set in the photo
          */
         photo.getImageMetaData();

         // check if height is set
         if (photo.getPhotoImageWidth() == Integer.MIN_VALUE) {

            // image dimension is not yet set

            photo.setPhotoSize(awtImage.getWidth(), awtImage.getHeight());
         }
      }

      return awtImage;
   }

   private static Image getImageFromCache_SWT(final Cache<String, ImageCacheWrapper> imageCache,
                                              final Photo photo,
                                              final String imageKey) {

      final ImageCacheWrapper cacheWrapper = imageCache.getIfPresent(imageKey);

      Image swtImage = null;

      if (cacheWrapper != null) {

         swtImage = cacheWrapper.swtImage;

         if (swtImage == null && cacheWrapper.awtImage != null) {

            final ImageData swtImageData = SWT2Dutil.convertToSWT(cacheWrapper.awtImage, photo.imageFilePathName);

            if (swtImageData != null) {

               // image could be converted

               swtImage = new Image(Display.getDefault(), new NoAutoScalingImageDataProvider(swtImageData));
            }
         }

         /*
          * ensure image and metadata are set in the photo
          */
         photo.getImageMetaData();

         // check if height is set
         if (photo.getPhotoImageWidth() == Integer.MIN_VALUE) {

            // image dimension is not yet set

            final Rectangle imageSize = swtImage.getBounds();

            photo.setPhotoSize(imageSize.width, imageSize.height);
         }
      }
      return swtImage;
   }

   public static Image getImageOriginal(final Photo photo) {

      final String imageKey = photo.getImageKey(ImageQuality.ORIGINAL);

      return getImageFromCache_SWT(_imageCache_OriginalImage, photo, imageKey);

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
    * @param awtImage
    * @param imageMetadata
    * @param imageHeight
    * @param imageWidth
    * @param originalImagePathName
    */
   public static void putImage_AWT(final String imageKey,
                                   final BufferedImage awtImage,
                                   final String originalImagePathName) {

      putImageInCache_AWT(_imageCache_ResizedImage, imageKey, awtImage, originalImagePathName);
   }

   /**
    * Put a new image into the image cache. When an old image with the same image key already
    * exists, this image will be disposed.
    *
    * @param imageKey
    * @param swtImage
    * @param imageMetadata
    * @param imageHeight
    * @param imageWidth
    * @param originalImagePathName
    *
    * @return
    */
   public static ImageCacheWrapper putImage_SWT(final String imageKey,
                                                final Image swtImage,
                                                final String originalImagePathName) {

      return putImageInCache_SWT(_imageCache_ResizedImage, imageKey, swtImage, originalImagePathName);
   }

   private static void putImageInCache_AWT(final Cache<String, ImageCacheWrapper> imageCache,
                                           final String imageKey,
                                           final BufferedImage awtImage,
                                           final String originalImagePathName) {

      final ImageCacheWrapper imageCacheWrapper = new ImageCacheWrapper(awtImage, originalImagePathName, imageKey);

      final ImageCacheWrapper oldWrapper = imageCache.asMap().put(imageKey, imageCacheWrapper);

      if (oldWrapper != null) {
         disposeImage(oldWrapper);
      }
   }

   private static ImageCacheWrapper putImageInCache_SWT(final Cache<String, ImageCacheWrapper> imageCache,
                                                        final String imageKey,
                                                        final Image swtImage,
                                                        final String originalImagePathName) {

      final ImageCacheWrapper imageCacheWrapper = new ImageCacheWrapper(swtImage, originalImagePathName, imageKey);

      final ImageCacheWrapper oldWrapper = imageCache.asMap().put(imageKey, imageCacheWrapper);

      if (oldWrapper != null) {
         disposeImage(oldWrapper);
      }

      return imageCacheWrapper;
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

      if (_imageCache_OriginalImage.asMap().size() > 1) {}
      putImageInCache_SWT(_imageCache_OriginalImage, imageKey, image, originalImagePathName);
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

      if (isSet == false) {

         isSet = setImageSize_IntoCacheWrapper(photo, imageWidth, imageHeight, ImageQuality.THUMB, _imageCache_ResizedImage);
      }

      if (isSet == false) {

         isSet = setImageSize_IntoCacheWrapper(photo, imageWidth, imageHeight, ImageQuality.HQ, _imageCache_ResizedImage);
      }

      if (isSet == false) {

         isSet = setImageSize_IntoCacheWrapper(photo, imageWidth, imageHeight, ImageQuality.ORIGINAL, _imageCache_OriginalImage);
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

         photo.setPhotoSize(imageWidth, imageHeight);

         return true;
      }

      return false;
   }

   public static void setOriginalImageCacheSize(final int newCacheSize) {
      _imageCache_OriginalImage.policy().eviction().ifPresent(eviction -> {
         eviction.setMaximum(newCacheSize);
      });
   }

   public static void setThumbCacheSize(final int newCacheSize) {
      _imageCache_ResizedImage.policy().eviction().ifPresent(eviction -> {
         eviction.setMaximum(newCacheSize);
      });
   }
}
