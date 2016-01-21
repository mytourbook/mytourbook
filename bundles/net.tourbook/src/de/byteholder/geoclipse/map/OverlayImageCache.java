/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.map;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.tourbook.common.util.StatusUtil;

import org.eclipse.swt.graphics.Image;

/**
 * cache for overlay images
 */
class OverlayImageCache {

	private static final int						MAX_CACHE_ENTRIES	= 200;

	private final ConcurrentHashMap<String, Image>	_imageCache			= new ConcurrentHashMap<String, Image>();
	private final ConcurrentLinkedQueue<String>		_imageCacheFifo		= new ConcurrentLinkedQueue<String>();

	public void add(final String tileKey, final Image overlayImage) {

		final int cacheSize = _imageCacheFifo.size();
		if (cacheSize > MAX_CACHE_ENTRIES) {

			// remove cache items
			for (int cacheIndex = MAX_CACHE_ENTRIES; cacheIndex < cacheSize; cacheIndex++) {

				// remove and dispose oldest image

				final String headTileKey = _imageCacheFifo.poll();
				final Image headImage = _imageCache.remove(headTileKey);

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
			final Image cachedImage = _imageCache.get(tileKey);
			if (cachedImage == null) {

				// this is a new image

				_imageCache.put(tileKey, overlayImage);
				_imageCacheFifo.add(tileKey);

				return;

			} else {

				// an image for the key already exists

				if (cachedImage == overlayImage) {

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
					_imageCache.put(tileKey, overlayImage);

					return;
				}

			}

		} catch (final Exception e) {
			StatusUtil.log(e.getMessage(), e);
		}
	}

	/**
	 * Dispose all overlay images in the cache
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
	 * @param tileKey
	 * @return Returns the overlay image or <code>null</code> when the image is not available or
	 *         disposed
	 */
	public Image get(final String tileKey) {

		final Image image = _imageCache.get(tileKey);
		if (image != null && !image.isDisposed()) {
			return image;
		}

		return null;
	}

}
