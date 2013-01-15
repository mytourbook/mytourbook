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

import java.util.ArrayList;
import java.util.HashMap;

public interface IPhotoServiceProvider {

	/**
	 * Key is tourId.
	 * 
	 * @param tourPhotoReferences
	 */
	void openTour(HashMap<Long, TourPhotoReference> tourPhotoReferences);

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
