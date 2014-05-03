/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.sign;

import java.io.File;

import net.tourbook.data.TourSign;
import net.tourbook.photo.Photo;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIPrefSign extends TVIPrefSignItem {

	private TourSign	_tourSign;

	/**
	 * The sign image is wrapped into a {@link Photo} which allows loading of the photo (sign)
	 * image.
	 */
	private Photo		_signImagePhoto;

	public TVIPrefSign(final TreeViewer signViewer, final TourSign tourSign) {

		super(signViewer);

		_tourSign = tourSign;

		// setup sign image
		final File photoImageFile = new File(tourSign.getImageFilePathName());

		_signImagePhoto = new Photo(photoImageFile);
	}

	@Override
	protected void fetchChildren() {
		// a sign has no children
	}

	public Photo getSignImagePhoto() {
		return _signImagePhoto;
	}

	public TourSign getTourSign() {
		return _tourSign;
	}

	@Override
	public boolean hasChildren() {
		return false;
	}

	public void setTourSign(final TourSign savedSign) {
		_tourSign = savedSign;
	}

}
