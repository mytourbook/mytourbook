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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import net.tourbook.photo.gallery.GalleryMTItem;
import net.tourbook.util.StatusUtil;

import org.apache.commons.sanselan.ImageReadException;
import org.apache.commons.sanselan.common.IImageMetadata;
import org.apache.commons.sanselan.formats.jpeg.JpegImageMetadata;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class PhotoImageLoader {

	Photo					photo;
	private GalleryMTItem	_galleryItem;
	int						galleryIndex;
	int						imageQuality;
	private String			_imageKey;

	private ILoadCallBack	_loadCallBack;

	Display					_display;		;

	public PhotoImageLoader(final Display display,
							final GalleryMTItem galleryItem,
							final Photo photo,
							final int imageQuality,
							final ILoadCallBack loadCallBack) {

		_display = display;
		_galleryItem = galleryItem;
		this.photo = photo;
		this.imageQuality = imageQuality;
		_loadCallBack = loadCallBack;

		galleryIndex = photo.getGalleryIndex();
		_imageKey = photo.getImageKey(imageQuality);
	}

//	private Image getThumbImageAWT(final Photo photo, final int requestedImageQuality) {
//
//		IPath storeImageFilePath = ThumbnailStore.getStoreImagePath(photo, requestedImageQuality);
//
//		// check if image is available
//		final File storeImageFile = new File(storeImageFilePath.toOSString());
//		if (storeImageFile.isFile()) {
//
//			// photo image is available in the thumbnail store
//
//			return new Image(_display, storeImageFilePath.toOSString());
//		}
//
//		try {
//
//			// load full size image
//			final long startLoadAWT = System.currentTimeMillis();
//
//			final BufferedImage img = ImageIO.read(photo.getImageFile());
//			if (img == null) {
//				StatusUtil.log(NLS.bind("Image \"{0}\" cannot be loaded", photo.getFilePathName())); //$NON-NLS-1$
//				return null;
//			}
//			final long endLoadAWT = System.currentTimeMillis();
//
////			RESIZE_LOCK.lock();
//			long startSaveAWT = 0;
//			long endSaveAWT = 0;
//			long endResize = 0;
//			long startResize = 0;
//			{
//				final int thumbSize = PhotoManager.IMAGE_SIZE[imageQuality];
//
//				final Point bestSize = ImageUtils.getBestSize(//
//						new Point(img.getWidth(), img.getHeight()),
//						new Point(thumbSize, thumbSize));
//
//				try {
//					final boolean isResizeRequired = ImageUtils.isResizeRequiredAWT(img, bestSize.x, bestSize.y);
//
//					if (isResizeRequired) {
//
//						startResize = System.currentTimeMillis();
//
//						final BufferedImage scaledImg = Scalr.resize(img, Math.max(bestSize.x, bestSize.y));
//
//						endResize = System.currentTimeMillis();
//						startSaveAWT = System.currentTimeMillis();
//
//						ThumbnailStore.saveImageAWT(scaledImg, storeImageFilePath);
//
//						endSaveAWT = System.currentTimeMillis();
//
//					}
//				} catch (final Exception e) {
//					StatusUtil.log(NLS.bind("Image \"{0}\" cannot be resized", photo.getFilePathName()), e); //$NON-NLS-1$
//					return null;
//
//				} finally {
////					RESIZE_LOCK.unlock();
//				}
//			}
//
//			final long startLoadSWT = System.currentTimeMillis();
//
//			final Image thumbnailImage = new Image(_display, storeImageFilePath.toOSString());
//
//			final long endLoadSWT = System.currentTimeMillis();
//
////			System.out.println((Thread.currentThread().getName() + "\t")
////					+ photo.getFileName()
////					+ "\tload: "
////					+ ((endLoadAWT - startLoadAWT) + "")
////					+ "\tresize: "
////					+ ((endResize - startResize) + "")
////					+ "\tsave AWT: "
////					+ ((endSaveAWT - startSaveAWT) + "")
////					+ "\tload SWT: "
////					+ ((endLoadSWT - startLoadSWT) + "")
////					+ "\ttotal: "
////					+ ((endLoadSWT - startLoadAWT) + "")
////			//
////					);
////			// TODO remove SYSTEM.OUT.PRINTLN
//
//			return thumbnailImage;
//
//		} catch (final Exception e) {
//			StatusUtil.log(NLS.bind("Store image \"{0}\" cannot be created", storeImageFilePath.toOSString()), e); //$NON-NLS-1$
//		}
//
//		return null;
//	}

	/**
	 * @param photo
	 * @param storeImageFilePath
	 *            Path to store image in the thumbnail store
	 * @return
	 */
	private Image getImageFromEXIFThumbnail(final Photo photo, final IPath storeImageFilePath) {

		try {

			final IImageMetadata metadata = photo.getMetaData();

			if (metadata == null) {
				return null;
			}

//			System.out.println();
//			System.out.println(metadata);
//			System.out.println("\t");
//			// TODO remove SYSTEM.OUT.PRINTLN

			if (metadata instanceof JpegImageMetadata) {

				final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;

				final BufferedImage bufferedImage = jpegMetadata.getEXIFThumbnail();
				if (bufferedImage == null) {
					System.out.println(photo.getFileName() + "\tNO EXIF THUMB");
					// TODO remove SYSTEM.OUT.PRINTLN

					return null;
				}

				System.out.println(photo.getFileName()
						+ "\tWITH EXIF THUMB\t"
						+ bufferedImage.getWidth()
						+ "x"
						+ bufferedImage.getHeight());
				// TODO remove SYSTEM.OUT.PRINTLN

				//#########################################################

				try {

					// get SWT image from AWT image

					try {
						ThumbnailStore.saveImageAWT(bufferedImage, storeImageFilePath);
					} catch (final Exception e) {
						StatusUtil.log(NLS.bind(//
								"Image \"{0}\" cannot be resized", //$NON-NLS-1$
								photo.getFilePathName()), e);
						return null;
					}

					final Image thumbnailImage = new Image(_display, storeImageFilePath.toOSString());

					return thumbnailImage;

				} catch (final Exception e) {
					StatusUtil.log(NLS.bind(//
							"Store image \"{0}\" cannot be created", //$NON-NLS-1$
							storeImageFilePath.toOSString()), e);
				}

				//#########################################################

			}
		} catch (final ImageReadException e) {
			StatusUtil.log(e);
		} catch (final IOException e) {
			StatusUtil.log(e);
		}

		return null;
	}

	/**
	 * Get image from thumb store with the requested image quality.
	 * 
	 * @param photo
	 * @param requestedImageQuality
	 * @return
	 */
	private Image getImageFromStore(final Photo photo, final int requestedImageQuality) {

		final IPath requestedStoreImageFilePath = ThumbnailStore.getStoreImagePath(photo, requestedImageQuality);

		/*
		 * check if image is available in the thumbstore
		 */
		final File storeImageFile = new File(requestedStoreImageFilePath.toOSString());
		if (storeImageFile.isFile()) {

			// photo image is available in the thumbnail store

			/*
			 * touch store file when it is not yet done today, this is done to track last access
			 * time so that a store cleanup can check the date
			 */
			final LocalDate dtModified = new LocalDate(storeImageFile.lastModified());
			if (dtModified.equals(new LocalDate()) == false) {
				storeImageFile.setLastModified(new DateTime().getMillis());
			}

			return new Image(_display, requestedStoreImageFilePath.toOSString());
		}

		return null;
	}

	/**
	 * check if the image is still visible
	 * 
	 * @return
	 */
	private boolean isImageVisible() {

		final GalleryMTItem group = _galleryItem.getParentItem();
		if (group == null) {
			return true;
		}

		final GalleryMTItem[] visibleItems = group.getVisibleItems();
		if (visibleItems == null) {
			return true;
		}

		for (final GalleryMTItem visibleItem : visibleItems) {

			if (visibleItem == null) {
				continue;
			}

			// !!! visibleItem.equals() is a performance hog when many items are displayed !!!
			if (visibleItem.x == _galleryItem.x && visibleItem.y == _galleryItem.y) {
				return true;
			}
		}

		// item is not visible

		return false;
	}

	/**
	 * This is called from the executor when the loading task is starting
	 * 
	 * <pre>
	 * 
	 * 2 Threads
	 * 
	 * SWT
	 * Photo-Image-Loader-1	IMG_1219_10.JPG	load:	1165	resize:	645	save:	110	total:	1920
	 * Photo-Image-Loader-0	IMG_1219_9.JPG	load:	1165	resize:	650	save:	110	total:	1925
	 * Photo-Image-Loader-1	IMG_1219.JPG	load:	566		resize:	875	save:	60	total:	1501
	 * Photo-Image-Loader-0	IMG_1219_2.JPG	load:	835		resize:	326	save:	55	total:	1216
	 * Photo-Image-Loader-1	IMG_1219_3.JPG	load:	1150	resize:	625	save:	55	total:	1830
	 * Photo-Image-Loader-0	IMG_1219_4.JPG	load:	565		resize:	630	save:	60	total:	1255
	 * Photo-Image-Loader-1	IMG_1219_5.JPG	load:	566		resize:	880	save:	60	total:	1506
	 * Photo-Image-Loader-0	IMG_1219_6.JPG	load:	845		resize:	341	save:	65	total:	1251
	 * Photo-Image-Loader-1	IMG_1219_7.JPG	load:	575		resize:	875	save:	50	total:	1500
	 * Photo-Image-Loader-0	IMG_1219_8.JPG	load:	845		resize:	356	save:	45	total:	1246
	 * 												8277			6203		670			15150
	 * 
	 * 
	 * AWT
	 * Photo-Image-Loader-1	IMG_1219_9.JPG	load:	1005	resize:	770		save AWT:	25	load SWT:	10	total:	1810
	 * Photo-Image-Loader-0	IMG_1219_10.JPG	load:	1015	resize:	1311	save AWT:	145	load SWT:	5	total:	2476
	 * Photo-Image-Loader-1	IMG_1219.JPG	load:	931		resize:	755		save AWT:	65	load SWT:	5	total:	1756
	 * Photo-Image-Loader-0	IMG_1219_2.JPG	load:	960		resize:	737		save AWT:	30	load SWT:	5	total:	1732
	 * Photo-Image-Loader-1	IMG_1219_3.JPG	load:	1340	resize:	700		save AWT:	25	load SWT:	10	total:	2075
	 * Photo-Image-Loader-0	IMG_1219_4.JPG	load:	935		resize:	751		save AWT:	25	load SWT:	10	total:	1721
	 * Photo-Image-Loader-1	IMG_1219_5.JPG	load:	981		resize:	810		save AWT:	25	load SWT:	5	total:	1821
	 * Photo-Image-Loader-0	IMG_1219_6.JPG	load:	970		resize:	821		save AWT:	30	load SWT:	5	total:	1826
	 * Photo-Image-Loader-1	IMG_1219_7.JPG	load:	950		resize:	710		save AWT:	25	load SWT:	5	total:	1690
	 * Photo-Image-Loader-0	IMG_1219_8.JPG	load:	950		resize:	706		save AWT:	30	load SWT:	5	total:	1691
	 * 												10037			8071				425				65			18598
	 * 
	 * 1 Thread
	 * 
	 * SWT
	 * Photo-Image-Loader-0	IMG_1219_10.JPG	load:	595	resize:	330	save:	70	total:	995
	 * Photo-Image-Loader-0	IMG_1219.JPG	load:	561	resize:	325	save:	80	total:	966
	 * Photo-Image-Loader-0	IMG_1219_2.JPG	load:	560	resize:	330	save:	50	total:	940
	 * Photo-Image-Loader-0	IMG_1219_3.JPG	load:	561	resize:	325	save:	45	total:	931
	 * Photo-Image-Loader-0	IMG_1219_4.JPG	load:	570	resize:	325	save:	50	total:	945
	 * Photo-Image-Loader-0	IMG_1219_5.JPG	load:	570	resize:	340	save:	50	total:	960
	 * Photo-Image-Loader-0	IMG_1219_6.JPG	load:	575	resize:	330	save:	45	total:	950
	 * Photo-Image-Loader-0	IMG_1219_7.JPG	load:	560	resize:	335	save:	50	total:	945
	 * Photo-Image-Loader-0	IMG_1219_8.JPG	load:	565	resize:	330	save:	45	total:	940
	 * Photo-Image-Loader-0	IMG_1219_9.JPG	load:	565	resize:	330	save:	45	total:	940
	 * 												5682		3300		530			9512
	 * 
	 * AWT
	 * Photo-Image-Loader-0	IMG_1219.JPG	load:	1115	resize:	790	save AWT:	45	load SWT:	5	total:	1955
	 * Photo-Image-Loader-0	IMG_1219_2.JPG	load:	1070	resize:	695	save AWT:	30	load SWT:	5	total:	1800
	 * Photo-Image-Loader-0	IMG_1219_3.JPG	load:	1035	resize:	695	save AWT:	25	load SWT:	5	total:	1760
	 * Photo-Image-Loader-0	IMG_1219_4.JPG	load:	1040	resize:	695	save AWT:	25	load SWT:	5	total:	1765
	 * Photo-Image-Loader-0	IMG_1219_5.JPG	load:	1040	resize:	695	save AWT:	25	load SWT:	110	total:	1870
	 * Photo-Image-Loader-0	IMG_1219_6.JPG	load:	1050	resize:	690	save AWT:	25	load SWT:	5	total:	1770
	 * Photo-Image-Loader-0	IMG_1219_7.JPG	load:	1035	resize:	690	save AWT:	145	load SWT:	5	total:	1875
	 * Photo-Image-Loader-0	IMG_1219_8.JPG	load:	1032	resize:	700	save AWT:	20	load SWT:	10	total:	1762
	 * Photo-Image-Loader-0	IMG_1219_9.JPG	load:	1030	resize:	700	save AWT:	25	load SWT:	5	total:	1760
	 * Photo-Image-Loader-0	IMG_1219_10.JPG	load:	1032	resize:	700	save AWT:	25	load SWT:	5	total:	1762
	 * 												10479			7050			390				160			18079
	 * 
	 * </pre>
	 */
	public void loadImage() {

		if (isImageVisible() == false) {
			setStateUndefined();
			return;
		}

		boolean isImageLoadedInRequestedQuality = false;
		Image loadedImage = null;
		String imageKey = null;
		boolean isLoadingError = false;

		try {

			// 1. get image from image store
			final Image storeImage = getImageFromStore(photo, imageQuality);
			if (storeImage != null) {

				isImageLoadedInRequestedQuality = true;
				imageKey = _imageKey;
				loadedImage = storeImage;

			} else {

				final int defaultThumbQuality = PhotoManager.IMAGE_QUALITY_THUMB_160;

				// 2. get image from thumbnail image in the EXIF data
				final IPath storeThumbImageFilePath = ThumbnailStore.getStoreImagePath(photo, defaultThumbQuality);
				final Image exifThumbnail = getImageFromEXIFThumbnail(photo, storeThumbImageFilePath);

				if (exifThumbnail != null) {
					isImageLoadedInRequestedQuality = imageQuality == defaultThumbQuality;
					imageKey = photo.getImageKey(defaultThumbQuality);
					loadedImage = exifThumbnail;
				} else {

					/*
					 * image could not be loaded the fast way, it must be loaded the slow way
					 */

					if (imageQuality == PhotoManager.IMAGE_QUALITY_ORIGINAL) {

						// load original image

						imageKey = _imageKey;
						loadedImage = new Image(_display, photo.getFilePathName());
					}
				}
			}

		} catch (final Exception e) {

			setStateLoadingError();

			isLoadingError = true;

		} finally {

			final boolean isImageLoaded = loadedImage != null;
			final boolean isImageVisible = isImageVisible();

			if (isImageLoaded) {
				// keep image in cache
				PhotoImageCache.putImage(imageKey, loadedImage);
			}

			if (isImageVisible == false) {

				// image is NOT visible
				setStateUndefined();

			} else if (isImageLoadedInRequestedQuality) {

				// image is loaded with requested quality
				setStateUndefined();

			} else {

				// load image with requested quality

				PhotoManager.putImageInHQLoadingQueue(_galleryItem, photo, imageQuality, _loadCallBack);
			}

			// display image in the loading callback
			_loadCallBack.callBackImageIsLoaded(isImageVisible, isImageLoaded || isLoadingError);
		}
	}

	/**
	 * Image could not be loaded with {@link #loadImage()}, try to load high quality image.
	 */
	public void loadImageHQ() {

		final long start = System.currentTimeMillis();

		if (isImageVisible() == false) {
			setStateUndefined();
			return;
		}

		boolean isLoadingError = false;
		Image hqImage = null;

		try {

			// load original image and create thumbs
			hqImage = loadImageHQ_10(photo, imageQuality);

		} catch (final Exception e) {

			setStateLoadingError();

			isLoadingError = true;

		} finally {

			final boolean isImageLoaded = hqImage != null;
			final boolean isImageVisible = isImageVisible();

			if (isImageLoaded) {

				setStateUndefined();

			} else {

				setStateLoadingError();

				isLoadingError = true;
			}

			// display image in the loading callback
			_loadCallBack.callBackImageIsLoaded(isImageVisible, isImageLoaded || isLoadingError);

			System.out.println("loadImageHQ() time: " + (System.currentTimeMillis() - start) + " ms  " + photo);
			// TODO remove SYSTEM.OUT.PRINTLN
		}
	}

	private Image loadImageHQ_10(final Photo photo, final int requestedImageQuality) {

		// load full size image
		final String fullSizePathName = photo.getFilePathName();
		Image loadedHQImage = null;

		int loadedImageWidth = 0;
		int loadedImageHeight = 0;

		try {

			// !!! this is not working on win7 with images 3500x5000 !!!
			loadedHQImage = new Image(_display, fullSizePathName);

			final Rectangle imageSize = loadedHQImage.getBounds();
			loadedImageWidth = imageSize.width;
			loadedImageHeight = imageSize.height;

		} catch (final Exception e) {

			// #######################################
			// this must be handled without logging
			// #######################################
			StatusUtil.log(NLS.bind("Fullsize image \"{0}\" cannot be loaded", fullSizePathName), e); //$NON-NLS-1$

		} finally {

			if (loadedHQImage == null) {

				/**
				 * sometimes (when images are loaded concurrently) larger images could not be loaded
				 * with SWT methods on Win7 (Eclipse 3.8 M6), try to load image with AWT, this
				 * <code>https://bugs.eclipse.org/bugs/show_bug.cgi?id=350783</code> bug fix has not
				 * solved this problem
				 */

				loadedHQImage = loadImageHQ_20_AWT();

				if (loadedHQImage == null) {
					StatusUtil.log(NLS.bind(//
							"Fullsize image \"{0}\" cannot be loaded",
							fullSizePathName), new Exception());
					return null;
				}
			}
		}

		Image requestedImage = null;

		final int[] thumbSizes = PhotoManager.IMAGE_SIZES;

		// the original size will not be stored in the thumb store
		for (int imageQuality = thumbSizes.length - 2; imageQuality >= 0; imageQuality--) {

			final int thumbSize = thumbSizes[imageQuality];

			Image resizedImage = null;
			IPath storeImagePath = null;

			try {

				if (loadedImageWidth > thumbSize || loadedImageHeight > thumbSize) {

					final Point bestSize = ImageUtils.getBestSize(
							loadedImageWidth,
							loadedImageHeight,
							thumbSize,
							thumbSize);

					resizedImage = ImageUtils.resize(_display, loadedHQImage, bestSize.x, bestSize.y, SWT.ON, SWT.HIGH);

				} else {

					resizedImage = loadedHQImage;
				}

				storeImagePath = ThumbnailStore.getStoreImagePath(photo, imageQuality);
				ThumbnailStore.saveImageSWT(//
						resizedImage,
						storeImagePath);

			} catch (final Exception e) {
				StatusUtil.log(NLS.bind("Store image \"{0}\" couldn't be created", storeImagePath.toOSString()), e); //$NON-NLS-1$
			}

			// requested image will be cached
			if (imageQuality == requestedImageQuality) {

				// keep requested image in cache
				PhotoImageCache.putImage(_imageKey, resizedImage);

				requestedImage = resizedImage;

			} else {

				// dispose resized image

				if (resizedImage != loadedHQImage) {
					resizedImage.dispose();
				}
			}
		}

		return requestedImage;
	}

	private Image loadImageHQ_20_AWT() {

		return null;
	}

	private void setStateLoadingError() {

		System.out.println("setStateLoadingError\t" + photo);
		// TODO remove SYSTEM.OUT.PRINTLN

		// prevent loading the image again
		photo.setLoadingState(PhotoLoadingState.IMAGE_HAS_A_LOADING_ERROR, imageQuality);
	}

	private void setStateUndefined() {

		// set state to undefined that it will be loaded again when image is visible
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
