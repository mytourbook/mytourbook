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

import java.awt.Point;
import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

import net.tourbook.photo.PicDirGallery;
import net.tourbook.util.StatusUtil;

import org.eclipse.core.runtime.IPath;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class PhotoImageLoaderItem {

	private static final ReentrantLock	RESIZE_LOCK	= new ReentrantLock();

	Photo								photo;
	private GalleryItem					_galleryItem;
	int									galleryIndex;
	int									imageQuality;
	private String						_imageKey;

	Display								_display	= Display.getDefault();

	private ILoadCallBack				_loadCallBack;

	public PhotoImageLoaderItem(final GalleryItem galleryItem,
								final Photo photo,
								final int imageQuality,
								final ILoadCallBack loadCallBack) {

		_galleryItem = galleryItem;
		this.photo = photo;
		this.imageQuality = imageQuality;
		_loadCallBack = loadCallBack;

		galleryIndex = photo.getGalleryIndex();
		_imageKey = photo.getImageKey(imageQuality);
	}

//	private ImageData convertToSWT(final BufferedImage bufferedImage) {
//
//		if (bufferedImage.getColorModel() instanceof DirectColorModel) {
//
//			final DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
//
//			final PaletteData palette = new PaletteData(
//					colorModel.getRedMask(),
//					colorModel.getGreenMask(),
//					colorModel.getBlueMask());
//
//			final ImageData data = new ImageData(
//					bufferedImage.getWidth(),
//					bufferedImage.getHeight(),
//					colorModel.getPixelSize(),
//					palette);
//
//			final boolean hasAlpha = colorModel.hasAlpha();
//
//			for (int y = 0; y < data.height; y++) {
//				for (int x = 0; x < data.width; x++) {
//
//					final int rgb = bufferedImage.getRGB(x, y);
//					final int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
//
//					data.setPixel(x, y, pixel);
//
//					if (hasAlpha) {
//						data.setAlpha(x, y, (rgb >> 24) & 0xFF);
//					}
//				}
//			}
//			return data;
//
//		} else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
//
//			final IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
//
//			final int size = colorModel.getMapSize();
//			final byte[] reds = new byte[size];
//			final byte[] greens = new byte[size];
//			final byte[] blues = new byte[size];
//			colorModel.getReds(reds);
//			colorModel.getGreens(greens);
//			colorModel.getBlues(blues);
//			final RGB[] rgbs = new RGB[size];
//			for (int i = 0; i < rgbs.length; i++) {
//				rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
//			}
//			final PaletteData palette = new PaletteData(rgbs);
//
//			final ImageData data = new ImageData(
//					bufferedImage.getWidth(),
//					bufferedImage.getHeight(),
//					colorModel.getPixelSize(),
//					palette);
//
//			data.transparentPixel = colorModel.getTransparentPixel();
//			final WritableRaster raster = bufferedImage.getRaster();
//			final int[] pixelArray = new int[1];
//
//			for (int y = 0; y < data.height; y++) {
//				for (int x = 0; x < data.width; x++) {
//					raster.getPixel(x, y, pixelArray);
//					data.setPixel(x, y, pixelArray[0]);
//				}
//			}
//			return data;
//		}
//		return null;
//	}

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

	private Image getStoreImage(final Photo photo, final int requestedImageQuality) {

		IPath storeImageFilePath = null;

		storeImageFilePath = ThumbnailStore.getStoreImagePath(photo, requestedImageQuality);

		// check if image is available
		final File storeImageFile = new File(storeImageFilePath.toOSString());
		if (storeImageFile.isFile()) {

			// photo image is available in the thumbnail store

			return new Image(_display, storeImageFilePath.toOSString());
		}

		// load full size image
		final long start = System.currentTimeMillis();

		final String fullSizePathName = photo.getFilePathName();
		Image fullSizeImage = null;

		try {

			fullSizeImage = new Image(_display, fullSizePathName);

		} catch (final Exception e) {
			StatusUtil.log(NLS.bind("Fullsize image \"{0}\" cannot be loaded", fullSizePathName), e); //$NON-NLS-1$
		} finally {
			System.out.println("getStoreImage()\t"
					+ (Thread.currentThread().getName() + "\t")
					+ ((System.currentTimeMillis() - start) + " ms\t")
					+ photo.getFileName());
			// TODO remove SYSTEM.OUT.PRINTLN

			if (fullSizeImage == null) {
				StatusUtil.log(NLS.bind(//
						"Fullsize image \"{0}\" cannot be loaded",
						fullSizePathName), new Exception());
				return null;
			}
		}

		try {

			Image thumbnailImage = null;

			RESIZE_LOCK.lock();
			{
				final Rectangle fullSizeImageBounds = fullSizeImage.getBounds();
				final int thumbSize = PhotoManager.IMAGE_SIZE[imageQuality];

				final Point bestSize = ImageUtils.getBestSize(//
						new Point(fullSizeImageBounds.width, fullSizeImageBounds.height),
						new Point(thumbSize, thumbSize));

				try {
					final boolean isResizeRequired = ImageUtils.isResizeRequired(fullSizeImage, bestSize.x, bestSize.y);

					if (isResizeRequired) {

						thumbnailImage = ImageUtils.resize(fullSizeImage, bestSize.x, bestSize.y);

						fullSizeImage.dispose();

						ThumbnailStore.saveImage(thumbnailImage, storeImageFilePath);

					} else {
						thumbnailImage = fullSizeImage;
					}
				} catch (final Exception e) {
					StatusUtil.log(NLS.bind("Image \"{0}\" cannot be resized", fullSizePathName), e); //$NON-NLS-1$
					return null;

				} finally {
					RESIZE_LOCK.unlock();
				}
			}

			return thumbnailImage;

		} catch (final Exception e) {
			StatusUtil.log(NLS.bind("Store image \"{0}\" cannot be created", storeImageFilePath.toOSString()), e); //$NON-NLS-1$
		}

		return null;
	}

//			final BufferedImage img = ImageIO.read(photo.getImageFile());
//
//			final int thumbSize160 = PhotoManager.IMAGE_SIZE[PhotoManager.IMAGE_QUALITY_THUMB_160];
//
//			final Point newSize = ImageUtils.getBestSize(//
//					new Point(img.getWidth(), img.getHeight()),
//					new Point(thumbSize160, thumbSize160));
//
//			final BufferedImage scaledImg = Scalr.resize(img, newSize.x);
//
//			final Image thumbnailImage = new Image(_display, convertToSWT(scaledImg));
//
//			ThumbnailStore.saveImage(thumbnailImage, storeImageFilePath);

	/**
	 * This is called from the executor when the task is starting
	 */
	public void loadImage() {

		try {

			// check if the image is still visible

			final PicDirGallery gallery = (PicDirGallery) _galleryItem.getParent();
			final Rectangle[] galleryItemBoundses = { null };
			final Rectangle[] clientAreas = { null };

			_display.syncExec(new Runnable() {
				public void run() {
					if (_galleryItem.isDisposed()) {
						return;
					}
					galleryItemBoundses[0] = _galleryItem.getBounds();
					clientAreas[0] = gallery.getClientArea();
				}
			});

			final Rectangle galleryItemBounds = galleryItemBoundses[0];

			if (galleryItemBounds == null) {
				resetState();
				return;
			}

			final Rectangle clientArea = clientAreas[0];
			final int translate = gallery.getTranslate();

			final int itemTop = galleryItemBounds.y;
			final int itemBottom = itemTop + galleryItemBounds.height;
			final int visibleBottom = translate + clientArea.height;

			if (itemBottom < 0 || itemTop > visibleBottom) {

				// item is not visible

				resetState();
				return;
			}

//			final String imageKey = photo.getImageKey(imageQuality);
			if (imageQuality == PhotoManager.IMAGE_QUALITY_ORIGINAL) {

				// load original image

				final Image fullSizeImage = new Image(_display, photo.getFilePathName());

				PhotoImageCache.putImage(_imageKey, fullSizeImage);

			} else {

				/*
				 * check if photo is available in the thumbnail store
				 */
				final Image storeImage = getStoreImage(photo, imageQuality);

				if (storeImage == null) {
					throw new Exception();
				} else {
					PhotoImageCache.putImage(_imageKey, storeImage);
				}
			}

			resetState();

			// tell the call back that the image is loaded
			_loadCallBack.callBackImageIsLoaded(galleryItemBounds);

		} catch (final Exception e) {

			StatusUtil.log(NLS.bind("Image \"{0}\" cannot be loaded ({1})", photo.getFileName(), _imageKey), e); //$NON-NLS-1$

			// prevent loading it again
			photo.setLoadingState(PhotoLoadingState.IMAGE_HAS_A_LOADING_ERROR, imageQuality);
		}
	}

	private void resetState() {

		// reset state to undefined that it will be loaded again when image is visible again
		photo.setLoadingState(PhotoLoadingState.UNDEFINED, imageQuality);
	}

	@Override
	public String toString() {
		return "PhotoImageLoaderItem [" //$NON-NLS-1$
				+ ("_filePathName=" + _imageKey + "{)}, ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("galleryIndex=" + galleryIndex + "{)}, ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("imageQuality=" + imageQuality + "{)}, ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("photo=" + photo) //$NON-NLS-1$
				+ "]"; //$NON-NLS-1$
	}
}
