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
package net.tourbook.photo.internal.manager;

import net.tourbook.photo.PhotoImageMetadata;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

/**
 * Cache for exif meta data.
 */
public class ExifCache {

	/**
	 * Cache for exif meta data, key is file path
	 */
	private static final ConcurrentLinkedHashMap<String, PhotoImageMetadata>	_exifCache	= new ConcurrentLinkedHashMap//
																							.Builder<String, PhotoImageMetadata>()
																									.maximumWeightedCapacity(
																											20000)
																									.build();

	public static void clear() {
		_exifCache.clear();
	}

	public static PhotoImageMetadata get(final String imageFilePathName) {
		return _exifCache.get(imageFilePathName);
	}

	public static void put(final String imageFilePathName, final PhotoImageMetadata metadata) {
		_exifCache.put(imageFilePathName, metadata);
	}

	/**
	 * Remove all cached metadata which starts with the folder path.
	 * 
	 * @param folderPath
	 */
	public static void remove(final String folderPath) {

		// remove cached exif data
		for (final String cachedPath : _exifCache.keySet()) {
			if (cachedPath.startsWith(folderPath)) {
				_exifCache.remove(cachedPath);
			}
		}
	}

//	/**
//	 * Update geo position in the cached exif data.
//	 *
//	 * @param updatedPhotos
//	 */
//	public static void updateGPSPosition(final ArrayList<PhotoWrapper> updatedPhotos) {
//
//		for (final PhotoWrapper photoWrapper : updatedPhotos) {
//
//			final PhotoImageMetadata imageMetadata = _exifCache.get(photoWrapper.imageFilePathName);
//			if (imageMetadata != null) {
//
//				imageMetadata.latitude = photoWrapper.photo.getLatitude();
//				imageMetadata.longitude = photoWrapper.photo.getLongitude();
//			}
//		}
//	}
}
