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

import net.tourbook.data.TourSign;
import net.tourbook.photo.Photo;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIPrefSign extends TVIPrefSignItem {

	private TourSign	_tourSign;

	public TVIPrefSign(final TVIPrefSignCategory_D parentItem, final TreeViewer signViewer, final TourSign tourSign) {

		super(signViewer);

		setParentItem(parentItem);
		_tourSign = tourSign;
	}

	@Override
	protected void fetchChildren() {
		// a sign has no children
	}

	public Photo getSignImagePhoto() {
		return _tourSign.getSignImagePhoto();
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
