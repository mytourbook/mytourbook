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

import java.util.concurrent.LinkedBlockingDeque;

public class PhotoExifLoader {

	private Photo			_photo;
	private ILoadCallBack	_loadCallBack;

	public PhotoExifLoader(final Photo photo, final ILoadCallBack imageLoadCallback) {

		_photo = photo;
		_loadCallBack = imageLoadCallback;
	}

	public void loadExif(	final LinkedBlockingDeque<PhotoImageLoader> waitingQueueThumb,
							final LinkedBlockingDeque<PhotoImageLoader> waitingQueueOriginal) {

		/*
		 * wait until thumb images and original images are loaded
		 */
		try {
			while (waitingQueueThumb.size() > 0 || waitingQueueOriginal.size() > 0) {
				Thread.sleep(50);
			}
		} catch (final InterruptedException e) {
			// should not happen, I hope so
		}

		// load metadata
		_photo.getImageMetaData();

		_loadCallBack.callBackImageIsLoaded(false);
	}

}
