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
package net.tourbook.photo;

public interface IPhotoPreferences {

	/*
	 * photo
	 */
	public static final String	PHOTO_THUMBNAIL_STORE_IS_DEFAULT_LOCATION			= "PHOTO_THUMBNAIL_STORE_IS_DEFAULT_LOCATION";				//$NON-NLS-1$
	public static final String	PHOTO_THUMBNAIL_STORE_CUSTOM_LOCATION				= "PHOTO_THUMBNAIL_STORE_CUSTOM_LOCATION";					//$NON-NLS-1$
	public static final String	PHOTO_THUMBNAIL_STORE_IS_CLEANUP					= "PHOTO_THUMBNAIL_STORE_IS_CLEANUP";						//$NON-NLS-1$
	public static final String	PHOTO_THUMBNAIL_STORE_CLEANUP_PERIOD				= "PHOTO_THUMBNAIL_STORE_CLEANUP_PERIOD";					//$NON-NLS-1$
	public static final String	PHOTO_THUMBNAIL_STORE_NUMBER_OF_DAYS_TO_KEEP_IMAGES	= "PHOTO_THUMBNAIL_STORE_NUMBER_OF_DAYS_TO_KEEP_IMAGES";	//$NON-NLS-1$
	public static final String	PHOTO_THUMBNAIL_STORE_LAST_CLEANUP_DATE_TIME		= "PHOTO_THUMBNAIL_STORE_LAST_CLEANUP_DATE_TIME";			//$NON-NLS-1$

	public static final String	PHOTO_THUMBNAIL_IMAGE_CACHE_SIZE					= "PHOTO_THUMBNAIL_IMAGE_CACHE_SIZE";						//$NON-NLS-1$

	public static final String	PHOTO_EXTERNAL_PHOTO_VIEWER_1						= "PHOTO_EXTERNAL_PHOTO_VIEWER_1";							//$NON-NLS-1$
	public static final String	PHOTO_EXTERNAL_PHOTO_VIEWER_2						= "PHOTO_EXTERNAL_PHOTO_VIEWER_2";							//$NON-NLS-1$
	public static final String	PHOTO_EXTERNAL_PHOTO_VIEWER_3						= "PHOTO_EXTERNAL_PHOTO_VIEWER_3";							//$NON-NLS-1$

	public static final String	PHOTO_VIEWER_PREF_EVENT_IMAGE_QUALITY_IS_MODIFIED	= "PHOTO_VIEWER_PREF_EVENT_IMAGE_QUALITY_IS_MODIFIED";		//$NON-NLS-1$
	public static final String	PHOTO_VIEWER_PREF_EVENT_IMAGE_VIEWER_UI_IS_MODIFIED	= "PHOTO_VIEWER_PREF_EVENT_IMAGE_VIEWER_UI_IS_MODIFIED";	//$NON-NLS-1$
	public static final String	PHOTO_VIEWER_PREF_EVENT_FULLSIZE_VIEWER_IS_MODIFIED	= "PHOTO_VIEWER_PREF_EVENT_FULLSIZE_VIEWER_IS_MODIFIED";	//$NON-NLS-1$
	public static final String	PHOTO_VIEWER_FONT									= "PHOTO_VIEWER_FONT";										//$NON-NLS-1$
	public static final String	PHOTO_VIEWER_COLOR_FOREGROUND						= "PHOTO_VIEWER_COLOR_FOREGROUND";							//$NON-NLS-1$
	public static final String	PHOTO_VIEWER_COLOR_BACKGROUND						= "PHOTO_VIEWER_COLOR_BACKGROUND";							//$NON-NLS-1$
	public static final String	PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND				= "PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND";				//$NON-NLS-1$
	public static final String	PHOTO_VIEWER_IS_SHOW_FILE_FOLDER					= "PHOTO_VIEWER_IS_SHOW_FILE_FOLDER";						//$NON-NLS-1$
	public static final String	PHOTO_VIEWER_COLOR_FOLDER							= "PHOTO_VIEWER_COLOR_FOLDER";								//$NON-NLS-1$
	public static final String	PHOTO_VIEWER_COLOR_FILE								= "PHOTO_VIEWER_COLOR_FILE";								//$NON-NLS-1$
	public static final String	PHOTO_VIEWER_IS_SHOW_IMAGE_WITH_HIGH_QUALITY		= "PHOTO_VIEWER_IS_SHOW_IMAGE_WITH_HIGH_QUALITY";			//$NON-NLS-1$
	public static final String	PHOTO_VIEWER_HIGH_QUALITY_IMAGE_MIN_SIZE			= "PHOTO_VIEWER_HIGH_QUALITY_IMAGE_MIN_SIZE";				//$NON-NLS-1$
	public static final String	PHOTO_VIEWER_HQ_IMAGE_SIZE							= "PHOTO_VIEWER_HQ_IMAGE_SIZE";							//$NON-NLS-1$
	public static final String	PHOTO_VIEWER_TEXT_MIN_THUMB_SIZE					= "PHOTO_VIEWER_TEXT_MIN_THUMB_SIZE";						//$NON-NLS-1$
	public static final String	PHOTO_VIEWER_IMAGE_BORDER_SIZE						= "PHOTO_VIEWER_IMAGE_BORDER_SIZE";						//$NON-NLS-1$
	public static final String	PHOTO_VIEWER_IMAGE_FRAMEWORK						= "PHOTO_VIEWER_IMAGE_FRAMEWORK";							//$NON-NLS-1$

	public static final String	PHOTO_FULLSIZE_VIEWER_IS_SHOW_PREVIEW				= "PHOTO_FULLSIZE_VIEWER_IS_SHOW_PREVIEW";					//$NON-NLS-1$
	public static final String	PHOTO_FULLSIZE_VIEWER_IS_SHOW_LOADING_MESSAGE		= "PHOTO_FULLSIZE_VIEWER_IS_SHOW_LOADING_MESSAGE";			//$NON-NLS-1$
	public static final String	PHOTO_FULLSIZE_VIEWER_IS_SHOW_HQ_IMAGE				= "PHOTO_FULLSIZE_VIEWER_IS_SHOW_HQ_IMAGE";				//$NON-NLS-1$

	public static final String	PHOTO_ORIGINAL_IMAGE_CACHE_SIZE						= "PHOTO_ORIGINAL_IMAGE_CACHE_SIZE";						//$NON-NLS-1$

	public static final String	PHOTO_SYSTEM_IS_ROTATE_IMAGE_AUTOMATICALLY			= "PHOTO_SYSTEM_IS_ROTATE_IMAGE_AUTOMATICALLY";			//$NON-NLS-1$
}
