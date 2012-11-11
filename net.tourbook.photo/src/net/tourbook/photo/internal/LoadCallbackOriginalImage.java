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
package net.tourbook.photo.internal;

import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.ImageGallery;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageMetadata;
import net.tourbook.photo.internal.gallery.MT20.FullScreenImageViewer;
import net.tourbook.photo.internal.gallery.MT20.GalleryMT20Item;
import net.tourbook.photo.internal.manager.ExifCache;

import org.eclipse.swt.widgets.Display;

class LoadCallbackOriginalImage implements ILoadCallBack {

	/**
	 * 
	 */
	private final ImageGallery	_imageGallery;
	private GalleryMT20Item		_galleryItem;
	private Photo				_photo;

	public LoadCallbackOriginalImage(	final ImageGallery imageGallery,
										final GalleryMT20Item galleryItem,
										final Photo photo) {

		_imageGallery = imageGallery;
		_galleryItem = galleryItem;
		_photo = photo;
	}

	@Override
	public void callBackImageIsLoaded(final boolean isUpdateUI) {

		// keep exif metadata
		final PhotoImageMetadata metadata = _photo.getImageMetaDataRaw();
		if (metadata != null) {
			ExifCache.put(_photo.getPhotoWrapper().imageFilePathName, metadata);
		}

		final FullScreenImageViewer fullSizeViewer = _imageGallery.getFullScreenImageViewer();

		if (fullSizeViewer.getCurrentItem() != _galleryItem) {
			// another gallery item is displayed
			return;
		}

		Display.getDefault().syncExec(new Runnable() {

			public void run() {

				if (_imageGallery.isDisposed()) {
					return;
				}

				// check again
				if (fullSizeViewer.getCurrentItem() != _galleryItem) {
					// another gallery item is displayed
					return;
				}

				fullSizeViewer.updateUI();
			}
		});
	}
}
