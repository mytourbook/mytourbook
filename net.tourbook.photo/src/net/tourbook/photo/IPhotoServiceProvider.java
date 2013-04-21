/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
import java.util.ArrayList;
import java.util.HashMap;

public interface IPhotoServiceProvider {

	/**
	 * @param selectedPhotos
	 * @return Returns <code>true</code> when rating stars can be saved, otherwise
	 *         <code>false</code>.
	 */
	boolean canSaveStarRating(int selectedPhotos, int ratingStars);

	/**
	 * Key is tourId.
	 * 
	 * @param tourPhotoReferences
	 */
	void openTour(HashMap<Long, TourPhotoReference> tourPhotoReferences);

	/**
	 * Replace file image path in this and other photos which are in the same path.
	 * 
	 * @param sourcePhoto
	 *            Photo from which the image should be replaced
	 * @return Returns a list of all images which file image path has been modified.
	 */
	ArrayList<File> replaceImageFilePath(Photo sourcePhoto);

	/**
	 * A mouse down event occured at a hovered gallery item within the star rating area.
	 * 
	 * @param photos
	 *            Photos which star rating should be saved.
	 */
	void saveStarRating(ArrayList<Photo> photos);

	/**
	 * Set's into the photo for which tours this photo is saved. Multiple tours are valid, e.g. a
	 * photo is saved for 2 different people.
	 * 
	 * @param photo
	 */
	void setTourReference(Photo photo);

}
