/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;

/**
 * This cache is photos.
 */
public class PhotoCache {

	private static final int								MAX_CACHE_SIZE	= 50000;

	private static ConcurrentLinkedHashMap<String, Photo>	_cache;

	static {

		final EvictionListener<String, Photo> evictionListener = new EvictionListener<String, Photo>() {

			final ExecutorService	executor	= Executors.newSingleThreadExecutor();

			@Override
			public void onEviction(final String fileName, final Photo photo) {

				executor.submit(new Callable<Void>() {
					@Override
					public Void call() throws IOException {

						return null;
					}
				});
			}
		};

		_cache = new ConcurrentLinkedHashMap.Builder<String, Photo>()
				.maximumWeightedCapacity(MAX_CACHE_SIZE)
				.listener(evictionListener)
				.build();
	}

	public static Photo getPhoto(final String imageFilePathName) {
		return _cache.get(imageFilePathName);
	}

	public static void setPhoto(final Photo photo) {
		_cache.put(photo.imageFilePathName, photo);
	}

}
