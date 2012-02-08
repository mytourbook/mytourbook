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

import net.tourbook.util.StatusUtil;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class PhotoImageLoaderItem {

	Photo					photo;
	int						galleryIndex;
	int						imageQuality;
	private String			_imageKey;

	private ILoadCallBack	_loadCallBack;

	PhotoImageLoaderItem(final Photo photo, final int imageQuality, final ILoadCallBack loadCallBack) {

		this.photo = photo;
		this.imageQuality = imageQuality;
		_loadCallBack = loadCallBack;

		galleryIndex = photo.getGalleryIndex();
		_imageKey = photo.getImageKey(imageQuality);
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
		if (imageQuality != other.imageQuality) {
			return false;
		}

		if (_imageKey == null) {
			if (other._imageKey != null) {
				return false;
			}
		} else if (!_imageKey.equals(other._imageKey)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + _imageKey.hashCode();
		return result;
	}

	/**
	 * This is called from the executor when the task is starting
	 */
	public void loadImage() {

		try {

//			System.out.println("loading: " + photo.getFileName());
//			// TODO remove SYSTEM.OUT.PRINTLN

//			// check if the image is still visible
//			boolean isVisible = false;
//			final int[] visibleGalleryItems = PhotoManager.getVisibleGalleryItems();
//			for (final int visibleItemIndex : visibleGalleryItems) {
//				if (visibleItemIndex == galleryIndex) {
//					isVisible = true;
//					break;
//				}
//			}
//
//			if (isVisible == false) {
//
//				// reset state to undefined that it will be loaded again when image is displayed again
//				photo.setLoadingState(PhotoLoadingState.UNDEFINED, imageQuality);
//				return;
//			}

			final Image fullSizeImage = new Image(Display.getDefault(), photo.getFilePathName());

//			final BufferedImage img = ImageIO.read(photo.getImageFile());
//			final BufferedImage scaledImg = Scalr.resize(img, PhotoManager.IMAGE_QUALITY_ORIGINAL);
//
//			final Point newSize = ImageUtils.getBestSize(
//					new Point(fullSizeImage.getBounds().width, fullSizeImage.getBounds().height),
//					new Point(PhotoManager.IMAGE_QUALITY_THUMB_160, PhotoManager.IMAGE_QUALITY_THUMB_160));
//
//			if (ImageUtils.isResizeRequired(fullSizeImage, newSize.x, newSize.y)) {
//
//				thumbnailImage = ImageUtils.resize(fullSizeImage, newSize.x, newSize.y);
//
//				this.imageService.release(fullSizeImage);
//				thumbnailImage = this.imageService.acquire(thumbnailImage);
//
//			} else {
//				thumbnailImage = fullSizeImage;
//			}

			PhotoImageCache.putImage(photo.getImageKey(imageQuality), fullSizeImage);

			// reset state to undefined that it will be loaded again when image is disposed
			photo.setLoadingState(PhotoLoadingState.UNDEFINED, imageQuality);

			// tell the call back that the image is loaded
			_loadCallBack.imageIsLoaded();

		} catch (final Exception e) {

			StatusUtil.log(NLS.bind("Image '{0}' cannot be loaded", _imageKey), e);

			// prevent loading it again
			photo.setLoadingState(PhotoLoadingState.IMAGE_HAS_A_LOADING_ERROR, imageQuality);
		}
	}

	@Override
	public String toString() {
		return "PhotoImageLoaderItem ["
				+ ("_filePathName=" + _imageKey + "{)}, ")
				+ ("galleryIndex=" + galleryIndex + "{)}, ")
				+ ("imageQuality=" + imageQuality + "{)}, ")
				+ ("photo=" + photo)
				+ "]";
	}
}
