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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.util.StatusUtil;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;

public class ThumbnailStore {

	private static final String			THUMBNAIL_STORE_OS_PATH	= "thumbnail-store";								//$NON-NLS-1$

	private static IPreferenceStore		_prefStore				= TourbookPlugin.getDefault().getPreferenceStore();

	private static IPath				_storePath				= getThumbnailStorePath();

	private static final ReentrantLock	SAVE_LOCK				= new ReentrantLock();

	private static IPath checkPath(final IPath storeImageFilePath) {

		final IPath imagePathWithoutExt = storeImageFilePath.removeFileExtension();

		// check store sub directory
		final File storeSubDir = imagePathWithoutExt.removeLastSegments(1).toFile();
		if (storeSubDir.exists() == false) {

			SAVE_LOCK.lock();

			try {
				// check again
				if (storeSubDir.exists() == false) {

					// create store sub directory

					if (storeSubDir.mkdirs() == false) {

						StatusUtil.log(NLS.bind(//
								"Thumbnail image path \"{0}\" cannot be created", //$NON-NLS-1$
								storeSubDir.getAbsolutePath()), new Exception());

						return null;
					}
				}

			} finally {
				SAVE_LOCK.unlock();
			}
		}
		return imagePathWithoutExt;
	}

	static synchronized IPath getStoreImagePath(final Photo photo, final int imageQuality) {

		final String imageKey = photo.getImageKey(imageQuality);

		final int imageQualitySize = PhotoManager.IMAGE_SIZE[imageQuality];
		final String imageKeyFolder = imageKey.substring(0, 2);

		final String imageFileName = imageKey + "_" + imageQualitySize + "_" + photo.getFileName();

		final IPath imageFilePath = _storePath//
				.append(imageKeyFolder)
				.append(imageFileName);

		return imageFilePath;
	}

	/**
	 * @return Returns the file path for the thumbnail store
	 */
	private static IPath getThumbnailStorePath() {

		final boolean useDefaultLocation = _prefStore.getBoolean(//
				ITourbookPreferences.PHOTO_USE_DEFAULT_THUMBNAIL_LOCATION);

		String tnFolderName;
		if (useDefaultLocation) {
			tnFolderName = Platform.getInstanceLocation().getURL().getPath();
		} else {
			tnFolderName = _prefStore.getString(ITourbookPreferences.PHOTO_CUSTOM_THUMBNAIL_LOCATION);
		}

		if (tnFolderName.trim().length() == 0) {
			tnFolderName = _prefStore.getString(ITourbookPreferences.PHOTO_CUSTOM_THUMBNAIL_LOCATION);
		}

		final File tnFolderFile = new File(tnFolderName);

		if (tnFolderFile.exists() == false || tnFolderFile.isDirectory() == false) {

			StatusUtil.logInfo(NLS.bind(
					"Thumbnail folder \"{0}\" is not available, it will be created now",
					tnFolderFile.getAbsolutePath()));

			// try to create thumbnail folder
			try {

				final boolean isCreated = tnFolderFile.mkdirs();
				if (isCreated) {
					StatusUtil.logInfo(NLS.bind("Thumbnail folder \"{0}\" created", tnFolderFile.getAbsolutePath()));
				} else {
					throw new Exception();
				}

			} catch (final Exception e) {
				throw new RuntimeException(NLS.bind(
						"Thumbnail folder \"{0}\" cannot be created",
						tnFolderFile.getAbsolutePath()), e);
			}
		}

		// append a unique folder so that deleting images is not being done in the wrong directory
		final IPath tnFolderPath = new Path(tnFolderName).append(THUMBNAIL_STORE_OS_PATH);
		final File tnFolderFileUnique = tnFolderPath.toFile();
		if (tnFolderFileUnique.exists() == false || tnFolderFileUnique.isDirectory() == false) {

			// try to create thumbnail unique folder
			try {
				final boolean isCreated = tnFolderFileUnique.mkdirs();
				if (isCreated) {
					StatusUtil.logInfo(NLS.bind(
							"Thumbnail folder \"{0}\" created",
							tnFolderFileUnique.getAbsolutePath()));
				} else {
					throw new Exception();
				}
			} catch (final Exception e) {
				throw new RuntimeException(NLS.bind(
						"Thumbnail folder \"{0}\" cannot be created",
						tnFolderFileUnique.getAbsolutePath()), e);
			}
		}

		return tnFolderPath.addTrailingSeparator();
	}

	static void saveImageAWT(final BufferedImage scaledImg, final IPath storeImageFilePath) {

		try {

			final IPath imagePathWithoutExt = checkPath(storeImageFilePath);
			if (imagePathWithoutExt == null) {
				return;
			}

			ImageIO.write(scaledImg, "jpg", new File(storeImageFilePath.toOSString()));

		} catch (final Exception e) {

			StatusUtil.log(NLS.bind(//
					"Cannot save thumbnail image: \"{0}\"", //$NON-NLS-1$
					storeImageFilePath.toOSString()), e);
		}
	}

	static void saveImageSWT(final Image thumbnailImage, final IPath storeImageFilePath) {

		try {

			final IPath imagePathWithoutExt = checkPath(storeImageFilePath);
			if (imagePathWithoutExt == null) {
				return;
			}

			final ImageLoader imageLoader = new ImageLoader();
			imageLoader.data = new ImageData[] { thumbnailImage.getImageData() };

			final IPath fullImageFilePath = imagePathWithoutExt.addFileExtension("jpg");

			imageLoader.save(fullImageFilePath.toOSString(), SWT.IMAGE_JPEG);

		} catch (final Exception e) {

			StatusUtil.log(NLS.bind(//
					"Cannot save thumbnail image: \"{0}\"", //$NON-NLS-1$
					storeImageFilePath.toOSString()), e);
		}
	}
}
