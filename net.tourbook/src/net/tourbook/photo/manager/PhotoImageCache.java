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

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Image;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;

public class PhotoImageCache {

	private static IPreferenceStore								_prefStore		= TourbookPlugin.getDefault() //
																						.getPreferenceStore();

	private static int											_maxCacheSize	= _prefStore.getInt(//
																						ITourbookPreferences.PHOTO_THUMBNAIL_IMAGE_CACHE_SIZE);

	private static final ConcurrentLinkedHashMap<String, Image>	_imageCache;

	static {

		final EvictionListener<String, Image> evictionListener = new EvictionListener<String, Image>() {

			final ExecutorService	executor	= Executors.newSingleThreadExecutor();

			@Override
			public void onEviction(final String fileName, final Image image) {

//				image.dispose();

				executor.submit(new Callable<Void>() {
					@Override
					public Void call() throws IOException {
						image.dispose();
						return null;
					}
				});
			}
		};

		_imageCache = new ConcurrentLinkedHashMap.Builder<String, Image>()
				.maximumWeightedCapacity(_maxCacheSize)
				.listener(evictionListener)
				.build();
	}

	/**
	 * Dispose all images in the cache
	 */
	public static synchronized void dispose() {

		if (_imageCache == null) {
			return;
		}

		// dispose cached images
		final Collection<Image> images = _imageCache.values();
		for (final Image image : images) {
			if (image != null) {
				image.dispose();
			}
		}

		_imageCache.clear();
	}

	public static Image getImage(final String imageKey) {
		return _imageCache.get(imageKey);
	}

	public static void putImage(final String imageKey, final Image image) {
		_imageCache.put(imageKey, image);
	}

	public static void setCacheSize(final int newCacheSize) {
		_imageCache.setCapacity(newCacheSize);
	}
}
