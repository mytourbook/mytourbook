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

import org.eclipse.swt.graphics.Image;

import com.mchange.v1.util.ArrayUtils;

/**
 * <p>
 * Original implementation: org.sharemedia.services.impl.imagemanager.WidgetImageCache
 * <p>
 */
public class PhotoImageCache_org_sharemedia {

	private static final int		EXPIRED					= 1000 * 20;
	public static final int			THUMBNAIL_SIZE			= 160;

	public int[]					IMAGE_WIDTH				= { THUMBNAIL_SIZE, 640, 800, 1024 };
	public int[]					IMAGE_HEIGHT			= { THUMBNAIL_SIZE, 480, 600, 768 };

	public static int				IMAGE_QUALITY_THUMB_160	= 0;									// 160x160 max
	public static int				IMAGE_QUALITY_LOW_640	= 1;									// 640x480 max
//	public static int				IMAGE_STD		= 2;											// 800x600 max
//	public static int				IMAGE_HIGH		= 3;											// 1024x768 max
	public static int				IMAGE_QUALITY_ORIGINAL	= 4;									// Full size
	/**
	 * This must be the max image quality which is also used as an index in an array
	 */
	public static int				MAX_IMAGE_QUALITY		= 4;

	private static PhotoImageCache_org_sharemedia	_instance;

	private Image[]					_images;
	private Photo[]					_photos;
	private int[]					_imageQuality;
	private long[]					_lastUsed;

	private int						_lastIndex				= 0;
	private int						_maxSize;

	private PhotoImageCache_org_sharemedia() {

		final int maxSize = 50;

		_maxSize = maxSize;
		_images = new Image[maxSize];
		_photos = new Photo[maxSize];
		_imageQuality = new int[maxSize];
		_lastUsed = new long[maxSize];

		for (int i = 0; i < maxSize; i++) {
			_images[i] = null;
			_photos[i] = null;
			_imageQuality[i] = -1;
			_lastUsed[i] = -1;
		}
	}

	public static PhotoImageCache_org_sharemedia getInstance() {

		if (_instance == null) {
			_instance = new PhotoImageCache_org_sharemedia();
		}

		return _instance;
	}

	public void clear(final Photo item) {

		final int pos = ArrayUtils.indexOf(_photos, item);
		if (pos >= 0) {
			free(pos);
		}
	}

	public void dispose() {
		for (int i = 0; i < _maxSize; i++) {
			if (_photos[i] != null) {
				free(i);
			}
		}
	}

	private synchronized int findSlot(final int from, final boolean onlyExpired) {

		long olderUse = System.currentTimeMillis();
		int olderPos = -1;

		for (int i = from; i < _maxSize; i++) {
			if (_photos[i] == null) {
				if (!onlyExpired) {
					return i;
				}
			} else {

				if (onlyExpired) {
					if (System.currentTimeMillis() - _lastUsed[i] > EXPIRED) {
						free(i);
						return i;
					}
				}

				if (olderUse > _lastUsed[i]) {
					olderPos = i;
					olderUse = _lastUsed[i];
				}
			}
		}
		if (onlyExpired) {
			return -1;
		}

		free(olderPos);
		return olderPos;
	}

	private synchronized void free(final int pos) {

		if (_photos[pos] != null) {

			final Image image = _images[pos];
			if (image != null) {
				image.dispose();
			}

			_lastUsed[pos] = -1;
			_photos[pos] = null;
			_images[pos] = null;
			_imageQuality[pos] = -1;
		}
	}

	public synchronized Image getImage(final Photo photo, final int imageQuality) {

		if (photo == null) {
			return null;
		}

		if (photo.equals(_photos[_lastIndex]) && imageQuality == _imageQuality[_lastIndex]) {
			_lastUsed[_lastIndex] = System.currentTimeMillis();

//			System.out.println("getImage\t" + photo);
//			// TODO remove SYSTEM.OUT.PRINTLN

			return _images[_lastIndex];
		}

		final int pos = indexOf(photo, imageQuality);

		if (pos >= 0) {
			_lastUsed[pos] = System.currentTimeMillis();
			_lastIndex = pos;

//			System.out.println("getImage\t" + photo);
//			// TODO remove SYSTEM.OUT.PRINTLN

			return _images[pos];
		}

		return null;
	}

	private int indexOf(final Photo m, final int definition) {

		for (int i = 0; i < _photos.length; i++) {
			if (m.equals(_photos[i]) && definition == _imageQuality[i]) {
				return i;
			}
		}

		return -1;
	}

	public synchronized void setImage(final Photo item, final int definition, final Image img) {

		int pos = 0;
		if (item.equals(_photos[_lastIndex])) {
			pos = _lastIndex;
			free(pos);
		} else {
			// Set usage
			pos = indexOf(item, definition);
			if (pos >= 0) {
				free(pos);
			} else {
				pos = findSlot(0, false);
			}
			_lastIndex = pos;
		}
		_photos[pos] = item;
		_images[pos] = img;
		_lastUsed[pos] = System.currentTimeMillis();
		_imageQuality[pos] = definition;
	}

}
