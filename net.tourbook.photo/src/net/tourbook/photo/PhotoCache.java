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

import net.tourbook.common.UI;

import org.eclipse.core.runtime.ListenerList;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;

/**
 * This cache is photos.
 */
public class PhotoCache {

	private static final int								MAX_CACHE_SIZE		= 50000;

	private static ConcurrentLinkedHashMap<String, Photo>	_cache;

	private static final ListenerList						_evictionListeners	= new ListenerList(
																						ListenerList.IDENTITY);

	static {

		final EvictionListener<String, Photo> evictionListener = new EvictionListener<String, Photo>() {

			final ExecutorService	executor	= Executors.newSingleThreadExecutor();

			@Override
			public void onEviction(final String fileName, final Photo photo) {

				executor.submit(new Callable<Void>() {
					@Override
					public Void call() throws IOException {

						final Object[] allListeners = _evictionListeners.getListeners();
						for (final Object listener : allListeners) {
							((IPhotoEvictionListener) listener).evictedPhoto(photo);
						}

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

	public static void addEvictionListener(final IPhotoEvictionListener listener) {
		_evictionListeners.add(listener);
	}

	public static void dumpAllPhotos() {

		System.out.println(UI.timeStampNano() + " PhotoCache\t");

		for (final Photo photo : _cache.values()) {

			System.out.println(UI.timeStampNano() + " \t" + photo.imageFilePathName + ("\t"));

			photo.dumpTourReferences();
		}
	}

	public static Photo getPhoto(final String imageFilePathName) {
		return _cache.get(imageFilePathName);
	}

	public static void removeEvictionListener(final IPhotoEvictionListener listener) {
		if (listener != null) {
			_evictionListeners.remove(listener);
		}
	}

//	public static synchronized void removePhotos(final Collection<Photo> photos) {
//
//		for (final Photo photo : photos) {
//			_cache.remove(photo.imageFilePathName);
//		}
//	}
//
//	public static synchronized void removePhotosFromFolder(final String folderName) {
//
//		for (final Photo photo : _cache.values()) {
//
//			if (photo.imagePathName.equals(folderName)) {
//
//				_cache.remove(photo.imageFilePathName);
//
////				System.out.println(UI.timeStampNano() + " \tremoved:" + photo.imageFilePathName);
////				// TODO remove SYSTEM.OUT.PRINTLN
//			}
//		}
//	}

	public static void setPhoto(final Photo photo) {
		_cache.put(photo.imageFilePathName, photo);
	}

}
