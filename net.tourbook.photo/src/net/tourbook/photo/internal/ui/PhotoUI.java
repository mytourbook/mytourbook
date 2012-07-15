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
package net.tourbook.photo.internal.ui;

import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.internal.Activator;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;

public class PhotoUI {

	public static Styler	PHOTO_FOLDER_STYLER;
	public static Styler	PHOTO_FILE_STYLER;

	static {

		setPhotoColorsFromPrefStore();

		/*
		 * set photo styler
		 */
		PHOTO_FOLDER_STYLER = StyledString.createColorRegistryStyler(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOLDER, null);
		PHOTO_FILE_STYLER = StyledString.createColorRegistryStyler(IPhotoPreferences.PHOTO_VIEWER_COLOR_FILE, null);
	}

	/**
	 * When this method is called, this class is loaded and initialized in the static initializer,
	 * which is setting the colors in the color registry
	 */
	public static void init() {}

	/**
	 * Set photo colors in the JFace color registry from the pref store
	 */
	public static void setPhotoColorsFromPrefStore() {

		// pref store var cannot be set from a static field because it can be null !!!
		final IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();

		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();

		colorRegistry.put(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND, //
				PreferenceConverter.getColor(prefStore, IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND));

		colorRegistry.put(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND, //
				PreferenceConverter.getColor(prefStore, IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND));

		colorRegistry.put(IPhotoPreferences.PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND, //
				PreferenceConverter.getColor(prefStore, IPhotoPreferences.PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND));

		colorRegistry.put(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOLDER, //
				PreferenceConverter.getColor(prefStore, IPhotoPreferences.PHOTO_VIEWER_COLOR_FOLDER));

		colorRegistry.put(IPhotoPreferences.PHOTO_VIEWER_COLOR_FILE, //
				PreferenceConverter.getColor(prefStore, IPhotoPreferences.PHOTO_VIEWER_COLOR_FILE));
	}

}
