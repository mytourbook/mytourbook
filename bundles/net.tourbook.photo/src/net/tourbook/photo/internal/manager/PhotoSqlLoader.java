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
package net.tourbook.photo.internal.manager;

import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.IPhotoServiceProvider;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoSqlLoadingState;
import net.tourbook.photo.internal.LoadCallbackImage;

public class PhotoSqlLoader {

	private Photo					_photo;
	private IPhotoServiceProvider	_photoServiceProvider;

	private ILoadCallBack			_imageLoadCallback;
	private boolean					_isUpdateUI;

	public PhotoSqlLoader(	final Photo photo,
							final ILoadCallBack imageLoadCallback,
							final IPhotoServiceProvider photoServiceProvider,
							final boolean isUpdateUI) {

		_photo = photo;
		_imageLoadCallback = imageLoadCallback;
		_photoServiceProvider = photoServiceProvider;
		_isUpdateUI = isUpdateUI;
	}

	public void loadSql() {

		_photoServiceProvider.setTourReference(_photo);

		_photo.getSqlLoadingState().set(PhotoSqlLoadingState.IS_LOADED);

		// update UI in the original callback
		if (_isUpdateUI && _imageLoadCallback instanceof LoadCallbackImage) {
			((LoadCallbackImage) _imageLoadCallback).updateUI();
		}
	}

}
