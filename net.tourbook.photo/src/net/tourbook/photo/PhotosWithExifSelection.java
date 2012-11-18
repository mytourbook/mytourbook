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

import java.util.ArrayList;

import org.eclipse.jface.viewers.ISelection;

/**
 * This selection contains photos which EXIF data are loaded.
 */
public class PhotosWithExifSelection implements ISelection {

	/**
	 * Contains photos sorted by date/time, EXIF data are loaded for the photos.
	 */
	public ArrayList<PhotoWrapper>	photos;

	public PhotosWithExifSelection(final ArrayList<PhotoWrapper> selectedPhotos) {
		this.photos = selectedPhotos;
	}

	@Override
	public boolean isEmpty() {
		return photos.size() == 0;
	}

}
