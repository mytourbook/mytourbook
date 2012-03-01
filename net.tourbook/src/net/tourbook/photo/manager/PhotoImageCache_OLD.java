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
package net.tourbook.photo.manager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.util.StatusUtil;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Image;

public class PhotoImageCache_OLD {

	private static IPreferenceStore							_prefStore			= TourbookPlugin.getDefault() //
																						.getPreferenceStore();

	private static int										_maxCacheSize		= _prefStore.getInt(//
																						ITourbookPreferences.PHOTO_THUMBNAIL_IMAGE_CACHE_SIZE);

	private static final ConcurrentHashMap<String, Image>	_imageCacheStore	= new ConcurrentHashMap<String, Image>();
	private static final LinkedBlockingDeque<String>		_imageCacheFifo		= new LinkedBlockingDeque<String>();

	private static int										_cacheThreshold		= _maxCacheSize / 10;

	private static void checkCacheSize() {

		final int imageCacheSize = _imageCacheFifo.size();
		if (imageCacheSize > _maxCacheSize) {

			// remove cache items
			for (int cacheIndex = _maxCacheSize - _cacheThreshold; cacheIndex < imageCacheSize; cacheIndex++) {

				// remove and dispose oldest image

				final String headImageKey = _imageCacheFifo.poll();
				final Image headImage = _imageCacheStore.remove(headImageKey);

				if (headImage != null) {
					try {
						headImage.dispose();
					} catch (final Exception e) {
						// it is possible that the image is already disposed by another thread
					}
				}
			}
		}
	}

	/**
	 * Dispose all images in the cache
	 */
	public static synchronized void dispose() {

		if (_imageCacheStore == null) {
			return;
		}

		// dispose cached images
		final Collection<Image> images = _imageCacheStore.values();
		for (final Image image : images) {
			if (image != null) {
				image.dispose();
			}
		}

		_imageCacheStore.clear();
		_imageCacheFifo.clear();
	}

	/**
	 * @param imageKey
	 * @return Returns the image or <code>null</code> when the image is not available or disposed
	 */
	public static Image getImage(final String imageKey) {

		final Image image = _imageCacheStore.get(imageKey);

		if (image != null && !image.isDisposed()) {
			return image;
		}

		return null;
	}

	public static void putImage(final String imageKey, final Image newImage) {

		checkCacheSize();

		try {

			/*
			 * check if the image is already in the cache
			 */
			final Image cachedImage = _imageCacheStore.get(imageKey);
			if (cachedImage == null) {

				// this is a new image

				_imageCacheStore.put(imageKey, newImage);
				_imageCacheFifo.add(imageKey);

				return;

			} else {

				// an image for the key already exists

				if (cachedImage == newImage) {

					// image is already in the cache

					return;

				} else {

					// dispose cached image which has the same key but is another image

					if (cachedImage != null) {
						try {
							cachedImage.dispose();
						} catch (final Exception e) {
							// it is possible that the image is already disposed by another thread
						}
					}

					// replace existing image, the image must be already in the fifo queue
					_imageCacheStore.put(imageKey, newImage);

					return;
				}

			}

		} catch (final Exception e) {
			StatusUtil.log(e.getMessage(), e);
		}
	}

	public static void setCacheSize(final int newCacheSize) {

		final boolean isSmaller = newCacheSize < _maxCacheSize;

		_maxCacheSize = newCacheSize;
		_cacheThreshold = newCacheSize / 10;

		if (isSmaller) {
			checkCacheSize();
		}
	}

}
