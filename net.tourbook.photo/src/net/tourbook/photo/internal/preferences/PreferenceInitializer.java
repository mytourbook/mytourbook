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
package net.tourbook.photo.internal.preferences;

import net.tourbook.common.UI;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.internal.Activator;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {

		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		/*
		 * photo
		 */
		store.setDefault(IPhotoPreferences.PHOTO_THUMBNAIL_STORE_IS_DEFAULT_LOCATION, true);
		store.setDefault(IPhotoPreferences.PHOTO_THUMBNAIL_STORE_CUSTOM_LOCATION, UI.EMPTY_STRING);

		store.setDefault(IPhotoPreferences.PHOTO_THUMBNAIL_STORE_IS_CLEANUP, false);
		store.setDefault(IPhotoPreferences.PHOTO_THUMBNAIL_STORE_NUMBER_OF_DAYS_TO_KEEP_IMAGES, 90);
		store.setDefault(IPhotoPreferences.PHOTO_THUMBNAIL_STORE_CLEANUP_PERIOD, 30);

		store.setDefault(IPhotoPreferences.PHOTO_THUMBNAIL_IMAGE_CACHE_SIZE, 2000);
		store.setDefault(IPhotoPreferences.PHOTO_ORIGINAL_IMAGE_CACHE_SIZE, 3);

		store.setDefault(IPhotoPreferences.PHOTO_VIEWER_IS_SHOW_FILE_FOLDER, true);

		store.setDefault(IPhotoPreferences.PHOTO_VIEWER_IS_SHOW_IMAGE_WITH_HIGH_QUALITY, true);
		store.setDefault(IPhotoPreferences.PHOTO_VIEWER_HIGH_QUALITY_IMAGE_MIN_SIZE, 50);
		store.setDefault(IPhotoPreferences.PHOTO_VIEWER_HQ_IMAGE_SIZE, PhotoLoadManager.IMAGE_SIZE_LARGE_DEFAULT);

		PreferenceConverter.setDefault(store, IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND, //
				new RGB(0xf3, 0xf3, 0xf3));

		PreferenceConverter.setDefault(store, IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND, //
				new RGB(0x33, 0x33, 0x33));

		PreferenceConverter.setDefault(store, IPhotoPreferences.PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND, //
				new RGB(0xFF, 0x80, 0x33));

		PreferenceConverter.setDefault(store, IPhotoPreferences.PHOTO_VIEWER_COLOR_FOLDER, //
				new RGB(0xFF, 0x6A, 0x11));

		PreferenceConverter.setDefault(store, IPhotoPreferences.PHOTO_VIEWER_COLOR_FILE, //
				new RGB(0x55, 0xC8, 0xFF));

		store.setDefault(IPhotoPreferences.PHOTO_VIEWER_TEXT_MIN_THUMB_SIZE, 70);
		store.setDefault(IPhotoPreferences.PHOTO_VIEWER_IMAGE_BORDER_SIZE, 4);

		store.setDefault(IPhotoPreferences.PHOTO_VIEWER_FONT, //
				UI.IS_OSX /*
						 * this small font for OSX cannot be selected in the UI, but is smaller than
						 * the fonts which can be selected
						 */
				? "1|sans-serif|9|0|"//$NON-NLS-1$
						: "1|sans-serif|7|0|");//$NON-NLS-1$
/////////////////////	  1|DejaVu Sans|6.75|0|WINDOWS|1|-9|0|0|0|400|0|0|0|0|3|2|1|34|DejaVu Sans

		store.setDefault(IPhotoPreferences.PHOTO_VIEWER_IMAGE_FRAMEWORK, //
				UI.IS_OSX //
						? PhotoLoadManager.IMAGE_FRAMEWORK_SWT
						//
						// SWT is terrible when scolling large images on win & linux, osx is smoothly
						//
						: PhotoLoadManager.IMAGE_FRAMEWORK_AWT);

		store.setDefault(IPhotoPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_PREVIEW, false);
		store.setDefault(IPhotoPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_LOADING_MESSAGE, true);
		store.setDefault(IPhotoPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_HQ_IMAGE, false);

		store.setDefault(IPhotoPreferences.PHOTO_SYSTEM_IS_ROTATE_IMAGE_AUTOMATICALLY, false);

		/*
		 * external photo viewer
		 */
		if (UI.IS_WIN) {

			store.setDefault(IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_1, "explorer.exe"); //$NON-NLS-1$

		} else if (UI.IS_OSX) {

			store.setDefault(IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_1, "Preview.app"); //$NON-NLS-1$
			store.setDefault(IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_2, "Finder.app"); //$NON-NLS-1$

		} else if (UI.IS_LINUX) {
			store.setDefault(IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_1, "nautilus"); //$NON-NLS-1$
		}

	}
}
