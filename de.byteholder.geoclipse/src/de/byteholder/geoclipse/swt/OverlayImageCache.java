package de.byteholder.geoclipse.swt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.swt.graphics.Image;

/**
 * cache for overlay images
 */
class OverlayImageCache {

	private static final int					MAX_CACHE_ENTRIES		= 128;

	private final Map<String, Image>			fOverlayImageCache		= new HashMap<String, Image>();
	private final ConcurrentLinkedQueue<String>	fOverlayImageCacheFifo	= new ConcurrentLinkedQueue<String>();

	public void add(final String tileKey, final Image image) {

		final int cacheSize = fOverlayImageCacheFifo.size();
		if (cacheSize > MAX_CACHE_ENTRIES) {

			// remove cache items 
			for (int cacheIndex = MAX_CACHE_ENTRIES; cacheIndex < cacheSize; cacheIndex++) {

				// remove and dispose oldest image
				final String head = fOverlayImageCacheFifo.poll();
				final Image headImage = fOverlayImageCache.remove(head);

				if (headImage != null && !headImage.isDisposed()) {
					try {
						headImage.dispose();
					} catch (final Exception e) {
						// it is possible that the image is already disposed by another thread
					}
				}
			}
		}

		fOverlayImageCache.put(tileKey, image);
		fOverlayImageCacheFifo.add(tileKey);
	}

	/**
	 * Dispose all overlay images in the cache
	 */
	public synchronized void dispose() {

		// dispose cached images
		final Collection<Image> images = fOverlayImageCache.values();
		for (final Image image : images) {
			if (image != null && !image.isDisposed()) {
				image.dispose();
			}
		}

		fOverlayImageCache.clear();
		fOverlayImageCacheFifo.clear();
	}

	/**
	 * @param tileKey
	 * @return Returns the overlay image or <code>null</code> when the image is not available or
	 *         disposed
	 */
	public Image get(final String tileKey) {

		final Image image = fOverlayImageCache.get(tileKey);
		if (image != null && !image.isDisposed()) {
			return image;
		}

		return null;
	}

}
