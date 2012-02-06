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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import net.tourbook.util.StatusUtil;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class PhotoImageLoaderItem implements Runnable {

	private static final PhotoImageCache								_imageCache		= PhotoImageCache.getInstance();

	private static final ConcurrentLinkedQueue<PhotoImageLoaderItem>	_waitingQueue	= PhotoManager
																								.getWaitingQueue();

	Photo																photo;
	private String														_filePathName;
	int																	galleryIndex;
	int																	imageQuality;

	private ILoadCallBack												_loadCallBack;

	public Future<?>													future;

	PhotoImageLoaderItem(final Photo photo, final int imageQuality, final ILoadCallBack loadCallBack) {

		this.photo = photo;
		this.imageQuality = imageQuality;
		_loadCallBack = loadCallBack;

		_filePathName = photo.getFilePathName();
		galleryIndex = photo.getGalleryIndex();
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (!(obj instanceof PhotoImageLoaderItem)) {
			return false;
		}

		final PhotoImageLoaderItem other = (PhotoImageLoaderItem) obj;
		if (_filePathName == null) {
			if (other._filePathName != null) {
				return false;
			}
		} else if (!_filePathName.equals(other._filePathName)) {
			return false;
		}
		if (imageQuality != other.imageQuality) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_filePathName == null) ? 0 : _filePathName.hashCode());
		result = prime * result + imageQuality;
		return result;
	}

	@Override
	public void run() {

		try {

			final Image image = new Image(Display.getDefault(), photo.getFilePathName());

//			_imageCache.setImage(photo, PhotoImageCache.IMAGE_QUALITY_ORIGINAL, image);
//			AsyncScalr.

			_imageCache.putImage(photo.getImageKey(imageQuality), image);

			// reset state to be undefined that it will be loaded again when it is disposed
			photo.setLoadingState(PhotoLoadingState.UNDEFINED, imageQuality);

			_waitingQueue.remove(this);

			_loadCallBack.imageIsLoaded();

		} catch (final Exception e) {

			StatusUtil.log(NLS.bind("Image '{0}' cannot be loaded", _filePathName), e);

			// prevent loading it again
			photo.setLoadingState(PhotoLoadingState.IMAGE_HAS_A_LOADING_ERROR, imageQuality);
		}
	}

	@Override
	public String toString() {
		return "PhotoImageLoaderItem ["
				+ ("_filePathName=" + _filePathName + "{)}, ")
				+ ("galleryIndex=" + galleryIndex + "{)}, ")
				+ ("imageQuality=" + imageQuality + "{)}, ")
				+ ("photo=" + photo)
				+ "]";
	}
}
