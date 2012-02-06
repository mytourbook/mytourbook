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
import java.util.concurrent.ConcurrentLinkedQueue;

import net.tourbook.util.StatusUtil;

import org.eclipse.swt.graphics.Image;

/**
 * <p>
 * Original implementation: org.sharemedia.services.impl.imagemanager.WidgetImageCache
 * <p>
 */
public class PhotoImageCache {

	private static final int						MAX_CACHE_ENTRIES	= 1000;

	private static PhotoImageCache					_instance;

	private final ConcurrentHashMap<String, Image>	_imageCache			= new ConcurrentHashMap<String, Image>();
	private final ConcurrentLinkedQueue<String>		_imageCacheFifo		= new ConcurrentLinkedQueue<String>();

	public static PhotoImageCache getInstance() {

		if (_instance == null) {
			_instance = new PhotoImageCache();
		}

		return _instance;
	}

	/**
	 * Dispose all images in the cache
	 */
	public synchronized void dispose() {

		// dispose cached images
		final Collection<Image> images = _imageCache.values();
		for (final Image image : images) {
			if (image != null) {
				image.dispose();
			}
		}

		_imageCache.clear();
		_imageCacheFifo.clear();
	}

	/**
	 * @param imageKey
	 * @return Returns the image or <code>null</code> when the image is not available or disposed
	 */
	public Image getImage(final String imageKey) {

		final Image image = _imageCache.get(imageKey);
		if (image != null && !image.isDisposed()) {
			return image;
		}

		return null;
	}

	public void putImage(final String imageKey, final Image image) {

		final int cacheSize = _imageCacheFifo.size();
		if (cacheSize > MAX_CACHE_ENTRIES) {

			// remove cache items
			for (int cacheIndex = MAX_CACHE_ENTRIES; cacheIndex < cacheSize; cacheIndex++) {

				// remove and dispose oldest image

				final String headImageKey = _imageCacheFifo.poll();
				final Image headImage = _imageCache.remove(headImageKey);

				if (headImage != null) {
					try {
						headImage.dispose();
					} catch (final Exception e) {
						// it is possible that the image is already disposed by another thread
					}
				}
			}
		}

		try {

			/*
			 * check if the image is already in the cache
			 */
			final Image cachedImage = _imageCache.get(imageKey);
			if (cachedImage == null) {

				// this is a new image

				_imageCache.put(imageKey, image);
				_imageCacheFifo.add(imageKey);

				return;

			} else {

				// an image for the key already exists

				if (cachedImage == image) {

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
					_imageCache.put(imageKey, image);

					return;
				}

			}

		} catch (final Exception e) {
			StatusUtil.log(e.getMessage(), e);
		}
	}

}
