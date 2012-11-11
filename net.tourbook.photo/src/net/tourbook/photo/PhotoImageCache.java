/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.tourbook.photo.internal.Activator;
import net.tourbook.photo.internal.manager.ImageCacheWrapper;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Image;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;

/**
 * This cache is caching photo images and it's metadata.
 */
public class PhotoImageCache {

	private static IPreferenceStore											_prefStore					= Activator
																												.getDefault()
																												.getPreferenceStore();

	private static int														_maxThumbImageCacheSize		= _prefStore
																												.getInt(IPhotoPreferences.PHOTO_THUMBNAIL_IMAGE_CACHE_SIZE);

	/**
	 * This cache size should not be too large otherwise OS has no resources, loading images is
	 * slowing down until all image caches must be disposed. This size is optimal for image size
	 * 5184x3456 on win7 for smaller image it could be larger for bigger images it should be
	 * smaller.
	 */
	private static int														_maxOriginalImageCacheSize	= _prefStore
																												.getInt(IPhotoPreferences.PHOTO_ORIGINAL_IMAGE_CACHE_SIZE);

	private static final ConcurrentLinkedHashMap<String, ImageCacheWrapper>	_imageCache;
	private static final ConcurrentLinkedHashMap<String, ImageCacheWrapper>	_imageCacheOriginal;

	static {

		final EvictionListener<String, ImageCacheWrapper> evictionListener = new EvictionListener<String, ImageCacheWrapper>() {

			final ExecutorService	executor	= Executors.newSingleThreadExecutor();

			@Override
			public void onEviction(final String fileName, final ImageCacheWrapper cacheWrapper) {

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

		_imageCache = new ConcurrentLinkedHashMap.Builder<String, ImageCacheWrapper>()
				.maximumWeightedCapacity(_maxThumbImageCacheSize)
				.listener(evictionListener)
				.build();

		_imageCacheOriginal = new ConcurrentLinkedHashMap.Builder<String, ImageCacheWrapper>()
				.maximumWeightedCapacity(_maxOriginalImageCacheSize)
				.listener(evictionListener)
				.build();
	}

	/**
	 * @param imageCache
	 * @param folderPath
	 *            When not <code>null</code> only the images in the folder path are disposed,
	 *            otherwise all images are disposed.
	 */
	private static synchronized void dispose(	final ConcurrentLinkedHashMap<String, ImageCacheWrapper> imageCache,
												final String folderPath) {

		if (imageCache == null) {
			return;
		}

		final boolean isDisposeAll = folderPath == null;

		// dispose cached images
		final Collection<ImageCacheWrapper> allWrappers = imageCache.values();
		for (final ImageCacheWrapper cacheWrapper : allWrappers) {

			if (cacheWrapper != null) {

				if (isDisposeAll == false) {

					// dispose images from a specific folder

					if (cacheWrapper.originalImagePathName.startsWith(folderPath) == false) {

						// image is in another folder

						continue;
					}

					imageCache.remove(cacheWrapper.imageKey);
				}

				final Image image = cacheWrapper.image;

				if (image != null) {
					image.dispose();
				}
			}
		}

		if (isDisposeAll) {
			imageCache.clear();
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
		dispose(_imageCache, folderPath);
	}

	public static Image getImage(final Photo photo, final ImageQuality imageQuality) {

		final String imageKey = photo.getImageKey(imageQuality);

		return getImageFromCache(_imageCache, photo, imageKey);
	}

	private static Image getImageFromCache(	final ConcurrentLinkedHashMap<String, ImageCacheWrapper> imageCache,
											final Photo photo,
											final String imageKey) {

		final ImageCacheWrapper cacheWrapper = imageCache.get(imageKey);

		Image photoImage = null;

		if (cacheWrapper != null) {

			photoImage = cacheWrapper.image;

			/*
			 * ensure image and metadata are set in the photo
			 */
			photo.getImageMetaData();

			// check if height is set
			if (photo.getImageWidth() == Integer.MIN_VALUE) {

				// image dimension is not yet set

				if (cacheWrapper.imageWidth != Integer.MIN_VALUE) {

					// image dimension is available

					photo.setDimension(cacheWrapper.imageWidth, cacheWrapper.imageHeight);
				}
			}
		}
		return photoImage;
	}

	public static Image getImageOriginal(final Photo photo) {

		final String imageKey = photo.getImageKey(ImageQuality.ORIGINAL);

		return getImageFromCache(_imageCacheOriginal, photo, imageKey);
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
	public static void putImage(final String imageKey,
								final Image image,
								final int imageWidth,
								final int imageHeight,
								final String originalImagePathName) {

		putImageInCache(_imageCache, imageKey, image, imageWidth, imageHeight, originalImagePathName);
	}

	private static void putImageInCache(final ConcurrentLinkedHashMap<String, ImageCacheWrapper> imageCache,
										final String imageKey,
										final Image image,
										final int imageWidth,
										final int imageHeight,
										final String originalImagePathName) {

		final ImageCacheWrapper imageCacheWrapper = new ImageCacheWrapper(
				image,
				imageWidth,
				imageHeight,
				originalImagePathName,
				imageKey);

		final ImageCacheWrapper oldWrapper = imageCache.put(imageKey, imageCacheWrapper);

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
	public static void putImageOriginal(final String imageKey,
										final Image image,
										final int imageWidth,
										final int imageHeight,
										final String originalImagePathName) {

		putImageInCache(_imageCacheOriginal, imageKey, image, imageWidth, imageHeight, originalImagePathName);
	}

	public static void setOriginalImageCacheSize(final int newCacheSize) {
		_imageCacheOriginal.setCapacity(newCacheSize);
	}

	public static void setThumbCacheSize(final int newCacheSize) {
		_imageCache.setCapacity(newCacheSize);
	}
}
