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

import java.io.File;

import net.tourbook.common.UI;
import net.tourbook.photo.internal.gallery.MT20.IGalleryCustomData;

/**
 * Wrapper for a photo image file, it's used to sort and filter {@link Photo}s.
 */
public class PhotoWrapper implements IGalleryCustomData {

	public Photo	photo;

	/**
	 * Photo image file
	 */
	public File		imageFile;

	public String	imageFileName;
	public String	imageFileExt;
	public String	imageFilePathName;

	/**
	 * Last modified in GMT
	 */
	public long		imageFileLastModified;

	/**
	 * Time in milliseconds
	 */
	public long		imageUTCTime;

	/**
	 * Offset in milliseconds from {@link #imageUTCTime} which is in UTC and the local time
	 */
//	public int		imageUTCZoneOffset;

	/**
	 * Time in ms (or {@link Long#MIN_VALUE} when not set) when photo was taken + time adjustments,
	 * e.g. wrong time zone, wrong time is set in the camera.
	 */
	public long		adjustedTime	= Long.MIN_VALUE;

	public long		imageFileSize;

	/**
	 * Camera which is used to take this photo, is <code>null</code> when not yet set.
	 */
	public Camera	camera;

	/**
	 * GPS has three states:
	 * 
	 * <pre>
	 * -1 state is not yet set, EXIF data are not yet loaded
	 *  0 photo do not contain GPS data
	 *  1 photo contains GPS data
	 * </pre>
	 */
	public int		gpsState		= -1;


	public PhotoWrapper(final File file) {

		imageFile = file;

		imageFileName = imageFile.getName();
		imageFilePathName = imageFile.getPath();
		imageFileLastModified = imageFile.lastModified();

		imageFileSize = imageFile.length();

		final int dotPos = imageFileName.lastIndexOf(UI.SYMBOL_DOT);
		imageFileExt = dotPos > 0 ? imageFileName.substring(dotPos + 1).toLowerCase() : UI.EMPTY_STRING;

		// initially sort by file date until exif data are loaded
		imageUTCTime = imageFileLastModified;
	}

	@Override
	public String getUniqueId() {
		return imageFilePathName;
	}

	@Override
	public String toString() {

		return (imageFileLastModified + "  ") + imageFileName; //$NON-NLS-1$

//		return photo.toString();
	}

}
