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
package net.tourbook.photo.internal;

import java.util.concurrent.atomic.AtomicReference;

import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.IPhotoServiceProvider;
import net.tourbook.photo.ImageGallery;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoSqlLoadingState;
import net.tourbook.photo.internal.gallery.MT20.GalleryMT20Item;

import org.eclipse.swt.widgets.Display;

public class LoadCallbackImage implements ILoadCallBack {

	private final ImageGallery	_imageGallery;
	private GalleryMT20Item		_galleryItem;

	/**
	 * @param galleryItem
	 * @param imageGallery
	 */
	public LoadCallbackImage(final ImageGallery imageGallery, final GalleryMT20Item galleryItem) {

		_imageGallery = imageGallery;
		_galleryItem = galleryItem;
	}

	@Override
	public void callBackImageIsLoaded(final boolean isUpdateUI) {

		final Photo photo = _galleryItem.photo;

		final AtomicReference<PhotoSqlLoadingState> sqlLoadingState = photo.getSqlLoadingState();

		final boolean isInLoadingQueue = sqlLoadingState.get() == PhotoSqlLoadingState.IS_IN_LOADING_QUEUE;

		final boolean isSqlLoaded = sqlLoadingState.compareAndSet(
				PhotoSqlLoadingState.IS_LOADED,
				PhotoSqlLoadingState.IS_IN_LOADING_QUEUE);

		if (isInLoadingQueue || isSqlLoaded) {

			if (isUpdateUI) {
				updateUI();
			}

		} else {

			final IPhotoServiceProvider photoServiceProvider = _galleryItem.gallery.getPhotoServiceProvider();
			if (photoServiceProvider == null) {

				// set to dummy loaded, this should not happen but it can happen when it's not fully setup
				photo.getSqlLoadingState().set(PhotoSqlLoadingState.IS_LOADED);

			} else {

				PhotoLoadManager.putPhotoInLoadingQueueSql(photo, this, photoServiceProvider);
			}
		}
	}

	public void updateUI() {

		// mark image area as needed to be redrawn
		Display.getDefault().syncExec(new Runnable() {

			public void run() {

				if (_imageGallery.isDisposed()) {
					return;
				}

				/*
				 * Visibility check must be done in the UI thread because scrolling the gallery can
				 * reposition the gallery item. This can be a BIG problem because the redraw()
				 * method is painting the background color at the specified rectangle, it cost me a
				 * lot of time to figure this out.
				 */
				final boolean isItemVisible = _galleryItem.gallery.isItemVisible(_galleryItem);

				if (isItemVisible) {

					// redraw gallery item WITH background
					_imageGallery.redrawItem(_galleryItem);
				}
			}
		});

		_imageGallery.jobUILoading_20_Schedule();
	}
}
