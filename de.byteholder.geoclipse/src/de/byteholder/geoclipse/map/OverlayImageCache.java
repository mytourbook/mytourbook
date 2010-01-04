package de.byteholder.geoclipse.map;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.swt.graphics.Image;

import de.byteholder.geoclipse.logging.StatusUtil;

/**
 * cache for overlay images
 */
class OverlayImageCache {

	private static final int						MAX_CACHE_ENTRIES	= 150;

	private final ConcurrentHashMap<String, Image>	fImageCache			= new ConcurrentHashMap<String, Image>();
	private final ConcurrentLinkedQueue<String>		fImageCacheFifo		= new ConcurrentLinkedQueue<String>();

	public void add(final String tileKey, final Image overlayImage) {

		final int cacheSize = fImageCacheFifo.size();
		if (cacheSize > MAX_CACHE_ENTRIES) {

			// remove cache items 
			for (int cacheIndex = MAX_CACHE_ENTRIES; cacheIndex < cacheSize; cacheIndex++) {

				// remove and dispose oldest image

				final String headTileKey = fImageCacheFifo.poll();
				final Image headImage = fImageCache.remove(headTileKey);

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
			final Image cachedImage = fImageCache.get(tileKey);
			if (cachedImage == null) {

				// this is a new image

				fImageCache.put(tileKey, overlayImage);
				fImageCacheFifo.add(tileKey);

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
					fImageCache.put(tileKey, overlayImage);

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
		final Collection<Image> images = fImageCache.values();
		for (final Image image : images) {
			if (image != null) {
				image.dispose();
			}
		}

		fImageCache.clear();
		fImageCacheFifo.clear();
	}

	/**
	 * @param tileKey
	 * @return Returns the overlay image or <code>null</code> when the image is not available or
	 *         disposed
	 */
	public Image get(final String tileKey) {

		final Image image = fImageCache.get(tileKey);
		if (image != null && !image.isDisposed()) {
			return image;
		}

		return null;
	}

}
